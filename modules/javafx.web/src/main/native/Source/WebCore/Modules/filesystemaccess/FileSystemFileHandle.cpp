/*
 * Copyright (C) 2021 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "FileSystemFileHandle.h"

#include "ContextDestructionObserverInlines.h"
#include "File.h"
#include "FileSystemHandleCloseScope.h"
#include "FileSystemStorageConnection.h"
#include "FileSystemSyncAccessHandle.h"
#include "FileSystemWritableFileStream.h"
#include "FileSystemWritableFileStreamSink.h"
#include "JSDOMPromiseDeferred.h"
#include "JSFile.h"
#include "JSFileSystemSyncAccessHandle.h"
#include "JSFileSystemWritableFileStream.h"
#include "WorkerFileSystemStorageConnection.h"
#include <wtf/TZoneMallocInlines.h>

namespace WebCore {

WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(FileSystemFileHandle);

Ref<FileSystemFileHandle> FileSystemFileHandle::create(ScriptExecutionContext& context, String&& name, FileSystemHandleIdentifier identifier, Ref<FileSystemStorageConnection>&& connection)
{
    auto result = adoptRef(*new FileSystemFileHandle(context, WTFMove(name), identifier, WTFMove(connection)));
    result->suspendIfNeeded();
    return result;
}

FileSystemFileHandle::FileSystemFileHandle(ScriptExecutionContext& context, String&& name, FileSystemHandleIdentifier identifier, Ref<FileSystemStorageConnection>&& connection)
    : FileSystemHandle(context, FileSystemHandle::Kind::File, WTFMove(name), identifier, WTFMove(connection))
{
}

void FileSystemFileHandle::getFile(DOMPromiseDeferred<IDLInterface<File>>&& promise)
{
    if (isClosed())
        return promise.reject(Exception { ExceptionCode::InvalidStateError, "Handle is closed"_s });

    connection().getFile(identifier(), [protectedThis = Ref { *this }, promise = WTFMove(promise)](auto result) mutable {
        if (result.hasException())
            return promise.reject(result.releaseException());

        RefPtr context = protectedThis->scriptExecutionContext();
        if (!context)
            return promise.reject(Exception { ExceptionCode::InvalidStateError, "Context has stopped"_s });

        promise.resolve(File::create(context.get(), result.returnValue(), { }, protectedThis->name()));
    });
}

void FileSystemFileHandle::createSyncAccessHandle(DOMPromiseDeferred<IDLInterface<FileSystemSyncAccessHandle>>&& promise)
{
    if (isClosed())
        return promise.reject(Exception { ExceptionCode::InvalidStateError, "Handle is closed"_s });

    connection().createSyncAccessHandle(identifier(), [protectedThis = Ref { *this }, promise = WTFMove(promise)](auto result) mutable {
        if (result.hasException())
            return promise.reject(result.releaseException());

        auto info = result.releaseReturnValue();
        if (!info.file)
            return promise.reject(Exception { ExceptionCode::UnknownError, "Invalid platform file handle"_s });

        RefPtr context = protectedThis->scriptExecutionContext();
        if (!context) {
            protectedThis->closeSyncAccessHandle(info.identifier);
            return promise.reject(Exception { ExceptionCode::InvalidStateError, "Context has stopped"_s });
        }

        promise.resolve(FileSystemSyncAccessHandle::create(*context, protectedThis.get(), info.identifier, WTFMove(info.file), info.capacity));
    });
}

void FileSystemFileHandle::closeSyncAccessHandle(FileSystemSyncAccessHandleIdentifier accessHandleIdentifier)
{
    if (isClosed())
        return;

    downcast<WorkerFileSystemStorageConnection>(connection()).closeSyncAccessHandle(identifier(), accessHandleIdentifier);
}

std::optional<uint64_t> FileSystemFileHandle::requestNewCapacityForSyncAccessHandle(FileSystemSyncAccessHandleIdentifier accessHandleIdentifier, uint64_t newCapacity)
{
    if (isClosed())
        return std::nullopt;

    return downcast<WorkerFileSystemStorageConnection>(connection()).requestNewCapacityForSyncAccessHandle(identifier(), accessHandleIdentifier, newCapacity);
}

void FileSystemFileHandle::registerSyncAccessHandle(FileSystemSyncAccessHandleIdentifier identifier, FileSystemSyncAccessHandle& handle)
{
    if (isClosed())
        return;

    downcast<WorkerFileSystemStorageConnection>(connection()).registerSyncAccessHandle(identifier, handle);
}

void FileSystemFileHandle::unregisterSyncAccessHandle(FileSystemSyncAccessHandleIdentifier identifier)
{
    if (isClosed())
        return;

    connection().unregisterSyncAccessHandle(identifier);
}

// https://fs.spec.whatwg.org/#api-filesystemfilehandle-createwritable
void FileSystemFileHandle::createWritable(const CreateWritableOptions& options, DOMPromiseDeferred<IDLInterface<FileSystemWritableFileStream>>&& promise)
{
    if (isClosed())
        return promise.reject(Exception { ExceptionCode::InvalidStateError, "Handle is closed"_s });

    connection().createWritable(scriptExecutionContext()->identifier(), identifier(), options.keepExistingData, [this, protectedThis = Ref { *this }, promise = WTFMove(promise)](auto result) mutable {
        if (result.hasException())
            return promise.reject(result.releaseException());

        auto streamIdentifier = result.returnValue();
        RefPtr context = protectedThis->scriptExecutionContext();
        if (!context) {
            closeWritable(streamIdentifier, FileSystemWriteCloseReason::Aborted);
            return promise.reject(Exception { ExceptionCode::InvalidStateError, "Context has stopped"_s });
        }

        auto* globalObject = JSC::jsCast<JSDOMGlobalObject*>(context->globalObject());
        if (!globalObject) {
            closeWritable(streamIdentifier, FileSystemWriteCloseReason::Aborted);
            return promise.reject(Exception { ExceptionCode::InvalidStateError, "Global object is invalid"_s });
        }

        auto sink = FileSystemWritableFileStreamSink::create(streamIdentifier, *this);
        if (sink.hasException()) {
            closeWritable(streamIdentifier, FileSystemWriteCloseReason::Aborted);
            return promise.reject(sink.releaseException());
        }

        ExceptionOr<Ref<FileSystemWritableFileStream>> stream { Exception { ExceptionCode::UnknownError } };
        {
            // FIXME: Make WritableStream function acquire lock as needed and remove this.
            Locker<JSC::JSLock> locker(globalObject->vm().apiLock());
            stream = FileSystemWritableFileStream::create(*globalObject, sink.releaseReturnValue());
        }
        if (!stream.hasException())
            connection().registerFileSystemWritable(streamIdentifier, stream.returnValue());

        promise.settle(WTFMove(stream));
    });
}

void FileSystemFileHandle::closeWritable(FileSystemWritableFileStreamIdentifier streamIdentifier, FileSystemWriteCloseReason reason)
{
    connection().unregisterFileSystemWritable(streamIdentifier);
    if (!isClosed())
        connection().closeWritable(identifier(), streamIdentifier, reason, [](auto) { });
}

void FileSystemFileHandle::executeCommandForWritable(FileSystemWritableFileStreamIdentifier streamIdentifier, FileSystemWriteCommandType type, std::optional<uint64_t> position, std::optional<uint64_t> size, std::span<const uint8_t> dataBytes, bool hasDataError, DOMPromiseDeferred<void>&& promise)
{
    if (isClosed())
        return promise.reject(Exception { ExceptionCode::InvalidStateError, "Handle is closed"_s });

    connection().executeCommandForWritable(identifier(), streamIdentifier, type, position, size, dataBytes, hasDataError, [promise = WTFMove(promise)](auto result) mutable {
        // Writable should be closed when stream is closed or errored, and stream will be errored after a failed write.
        promise.settle(WTFMove(result));
    });
}

} // namespace WebCore

