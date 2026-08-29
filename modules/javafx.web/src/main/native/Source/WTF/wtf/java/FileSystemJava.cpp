/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "config.h"
#include "FileHandle.h"
#include "FileSystem.h"
#include "MappedFileData.h"
#include "FileMetadata.h"
#include <optional>
#include <wtf/java/WKJHandle.h>
#include <wtf/java/WKJRuntime.h>
#include <wtf/text/CString.h>
#include <wtf/text/MakeString.h>

#if OS(WINDOWS)
    #include <windows.h>
#else
    #include <sys/types.h>
    #include <sys/stat.h>
    #include <unistd.h>
#endif

namespace WTF {

namespace FileSystemImpl {

static inline bool isHandleValid(PlatformFileHandle handle)
{
    return handle != invalidPlatformFileHandle;
}

#if HAVE(MMAP)
MappedFileData::MappedFileData(MmapSpan<uint8_t>&& fileData)
    : m_fileData(WTF::move(fileData))
{ }

MappedFileData::~MappedFileData() = default;

#elif OS(WINDOWS)
MappedFileData::MappedFileData(std::span<uint8_t> fileData, Win32Handle&& fileMapping)
    : m_fileData(fileData)
    , m_fileMapping(WTF::move(fileMapping))
{
}

MappedFileData::~MappedFileData()
{

}
#endif

// -----------------------------------------------------------------------
//  Below methods use Java calls to implement the intended functionality.
//
//  They reach com.sun.webkit.FileSystem through WKJHostFileSystem
//  (webkit_java_api_theme.h), which replaced the ten cached static method ids and the
//  eagerly resolved jclass that JNI_OnLoad had to take at load time. Every slot may be
//  NULL, so each call tests the table and the pointer and falls back to the value the
//  failed JNI call produced - 0/false for the predicates, -1 for the sizes, the empty
//  String for the paths.
//
//  THREAD: any. WebKit reaches these from AsyncFileStream and WorkerThread as well as
//  from the main thread, which is why the JNI code attached the calling thread. An FFM
//  upcall stub attaches on its own, so nothing here does.
//
//  wkjCheckAndClearException() is called wherever WTF::CheckAndClearException(env) was,
//  including where the result was discarded; seekFile is the one place that branches on
//  it, and it still does.
// -----------------------------------------------------------------------
bool fileExists(const String& path)
{
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->file_exists)
        return false;

    WKJStringArg jpath(path);
    int32_t result = cb->file_exists(jpath.data(), jpath.length());
    wkjCheckAndClearException();

    return result != 0;
}


bool getFileSize(const String& path, long long& result)
{
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->get_file_size)
        return false;

    WKJStringArg jpath(path);
    // A negative size means "no size available" and is what the caller tests; it is not an
    // error code to be normalised.
    int64_t size = cb->get_file_size(jpath.data(), jpath.length());
    wkjCheckAndClearException();

    if (size >= 0) {
        result = size;
        return true;
    } else {
        return false;
    }
}

std::optional<uint64_t> fileSize(const String& path)
{
    long long size = 0;
    getFileSize(path, size);
    return size;
}

std::optional<FileMetadata> fileMetadata(const String& path)
{
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->get_file_metadata)
        return {};

    // The long[3] that crossed the boundary is now a caller-owned array, so the allocation
    // that NewLongArray could fail at is gone along with GetLongArrayElements. The three
    // slots keep their order and their meanings.
    WKJStringArg jpath(path);
    int64_t metadataResults[3] = { 0, 0, 0 };
    int32_t result = cb->get_file_metadata(jpath.data(), jpath.length(), metadataResults);
    wkjCheckAndClearException();

    if (result) {
        FileMetadata metadata {};
        metadata.modificationTime = WallTime::fromRawSeconds(metadataResults[0] / 1000.0);
        metadata.length = metadataResults[1];
        metadata.type = static_cast<FileMetadata::Type>(metadataResults[2]);
        return metadata;
    }
    return {};
}

std::optional<WallTime> getFileModificationTime(const String& path)
{
    std::optional<FileMetadata> metadata = fileMetadata(path);
    if (metadata) {
        return { metadata->modificationTime };
    } else {
        return { };
    }
}

std::optional<WallTime> fileModificationTime(const String& path)
{
    return getFileModificationTime(path);
}

String pathByAppendingComponents(StringView path, std::span<const StringView> components)
{
    String result = path.toString();
    // FIXME-java: Use nio.file.Paths.get(...)
    for (const auto& component : components) {
        result = pathByAppendingComponent(result, component.toString());
    }
    return result;
}

/*
 * The JNI body ended in "return String(env, result)", and that constructor mapped a null
 * jstring to the EMPTY String (StringJava.cpp:36-38). wkjFetchString reports Java null as
 * the null String instead, because the outbound null/empty distinction is load-bearing
 * elsewhere in the ABI, so the collapse has to be written out here to keep this function
 * behaving as it always has.
 */
static String collapseNullToEmpty(String&& value)
{
    if (value.isNull())
        return emptyString();
    return WTF::move(value);
}

String pathByAppendingComponent(const String& path, const String& component)
{
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->path_by_appending_component)
        return emptyString();

    WKJStringArg jpath(path);
    WKJStringArg jcomponent(component);
    String result = wkjFetchString([&](uint16_t* buf, int32_t cap, int32_t* length) {
        return cb->path_by_appending_component(jpath.data(), jpath.length(),
                                               jcomponent.data(), jcomponent.length(),
                                               buf, cap, length);
    });
    wkjCheckAndClearException();

    return collapseNullToEmpty(WTF::move(result));
}

String pathByAppendingComponent(StringView path, StringView component)
{
    return pathByAppendingComponent(path.toString(), component.toString());
}

bool makeAllDirectories(const String& path)
{
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->make_all_directories)
        return false;

    WKJStringArg jpath(path);
    int32_t result = cb->make_all_directories(jpath.data(), jpath.length());
    wkjCheckAndClearException();

    return result != 0;
}


CString fileSystemRepresentation(const String& s)
{
    return CString(s.latin1().data());
}

FileHandle openFile(const String& path, FileOpenMode mode, FileAccessPermission, OptionSet<FileLockMode> , bool failIfFileExists)
{
    if (mode != FileOpenMode::Read) {
        return FileHandle::adopt(invalidPlatformFileHandle);
    }
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->open_file)
        return FileHandle::adopt(invalidPlatformFileHandle);

    // Only "r" is ever passed, exactly as before; the slot takes the mode so that the Java
    // side stays a plain forwarder.
    static const uint16_t readMode[] = { 'r' };
    WKJStringArg jpath(path);

    /*
     * OWNERSHIP. open_file returns a NEW id that this library owns and must release exactly
     * once - the registry counts references, and nothing reclaims a leaked id the way JNI
     * reclaimed a leaked local reference when a native method returned. The id is adopted
     * here (WKJHandle takes ownership without adding a reference, which is what the
     * consuming JGlobalRef(T raw) constructor did with the local ref) and then handed to the
     * FileHandle, which is its named owner from this point until FileHandle::close.
     * A zero id is invalidPlatformFileHandle, so the failure path needs no special case.
     */
    PlatformFileHandle handle { cb->open_file(jpath.data(), jpath.length(), readMode, 1) };
    wkjCheckAndClearException();

    return FileHandle::adopt(handle);
}


void closeFile(PlatformFileHandle& handle)
{
    if (isHandleValid(handle)) {
        const WKJHostFileSystem* cb = wkjFileSystem();
        if (cb && cb->close_file) {
            cb->close_file(handle.get());
            wkjCheckAndClearException();
        }
        // Drops the reference the FileHandle owned, whether or not the Java close ran. The
        // JNI version did the same by assigning the null global reference over it.
        handle = invalidPlatformFileHandle;
    }
}

int readFromFile(PlatformFileHandle handle, void* data, int length)
{
    if (length < 0 || data == nullptr) {
        return -1;
    }
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->read_from_file)
        return -1;

    /*
     * "data" is the caller's buffer, wrapped by Java without copying for the duration of the
     * call - what NewDirectByteBuffer did. That direct ByteBuffer was a JNI LOCAL reference
     * created outside any native method, so it was reclaimed only when the calling thread
     * detached; on a long-lived WebKit thread doing many reads it accumulated. The wrapper
     * is now scoped to the call and that accumulation is gone. Behaviour is otherwise
     * identical, including turning any negative result into -1.
     */
    int result = cb->read_from_file(handle.get(), data, length);
    wkjCheckAndClearException();

    if (result < 0) {
        return -1;
    }
    return result;
}

std::optional<PlatformFileID> FileHandle::id()
{
    return std::nullopt;
}

std::optional<MappedFileData> FileHandle::map(MappedFileMode mapMode, FileOpenMode openMode)
{
   return std::nullopt;
}

std::optional<uint64_t> FileHandle::read(std::span<uint8_t> data)
{
    if (!m_handle || data.empty())
        return std::nullopt;

    int result = readFromFile(platformHandle(), data.data(), data.size());
    if (result < 0)
        return std::nullopt;

    return static_cast<uint64_t>(result);
}

std::optional<uint64_t> FileHandle::write(std::span<const uint8_t> data)
{
    return { };
}

bool FileHandle::truncate(int64_t offset)
{
    return false;
}

bool FileHandle::flush()
{
    return false;
}


void FileHandle::close()
{
   closeFile(m_handle.unsafeValue());
}

std::optional<uint64_t> FileHandle::size()
{
   return {};
}


String pathFileName(const String& path)
{
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->path_get_file_name)
        return emptyString();

    WKJStringArg jpath(path);
    String result = wkjFetchString([&](uint16_t* buf, int32_t cap, int32_t* length) {
        return cb->path_get_file_name(jpath.data(), jpath.length(), buf, cap, length);
    });
    wkjCheckAndClearException();

    // As above: String(env, jstring) collapsed a null result to the empty String.
    return collapseNullToEmpty(WTF::move(result));
}

long long seekFile(PlatformFileHandle handle, long long offset, FileSeekOrigin)
{
    // we always get positive value for offset from webkit.
    // Below check for offset < 0 might be redundant?
    if (offset < 0 || !isHandleValid(handle)) {
        return -1;
    }
    const WKJHostFileSystem* cb = wkjFileSystem();
    if (!cb || !cb->seek_file)
        return -1;

    cb->seek_file(handle.get(), offset);

    /*
     * One of the roughly twelve sites in the tree that BRANCH on the exception check rather
     * than merely clearing. The slot returns nothing, so a failed seek is reported exactly
     * where it was before: an exception out of the upcall becomes -1. The check is narrower
     * than the JNI one - it can no longer be tripped by a failed member-id lookup, because
     * there is no lookup - but for this call site the question was always "did the Java seek
     * throw", and the answer is unchanged.
     */
    if (wkjCheckAndClearException()) {
        offset = -1;
    }
    return offset;
}

std::optional<uint64_t> FileHandle::seek(int64_t offset, FileSeekOrigin origin)
{
    long long pos = seekFile(m_handle.unsafeValue(), offset, origin);

    if (pos < 0)
        return std::nullopt;

    return static_cast<uint64_t>(pos);
}

// -----------------------------------------------------------------------
// Below methods are stubs as of now.
// TODO: Implement the functionality in future using Java calls as and
// when needed.
// -----------------------------------------------------------------------
std::optional<WallTime> fileCreationTime(const String&) // Not all platforms store file creation time.
{
    fprintf(stderr, "fileCreationTime(const String&) NOT IMPLEMENTED\n");
    return { };
}

String homeDirectoryPath()
{
    fprintf(stderr, "homeDirectoryPath() NOT IMPLEMENTED\n");
    return String();
}

String directoryName(String const &)
{
    fprintf(stderr, "directoryName(String const &) NOT IMPLEMENTED\n");
    return String();
}

Vector<String> listDirectory(const String&, const String&)
{
    fprintf(stderr, "listDirectory(const String&, const String&) NOT IMPLEMENTED\n");
    Vector<String> entities;
    return entities;
}

Vector<String> listDirectory(const String&)
{
    fprintf(stderr, "listDirectory(const String&) NOT IMPLEMENTED\n");
    Vector<String> entities;
    return entities;
}

int writeToFile(PlatformFileHandle, const void* data, int length)
{
    fprintf(stderr, "writeToFile(PlatformFileHandle, const void* data, int length) NOT IMPLEMENTED\n");
    UNUSED_PARAM(data);
    UNUSED_PARAM(length);

    return -1;
}

bool truncateFile(PlatformFileHandle, long long offset)
{
    fprintf(stderr, "truncateFile(PlatformFileHandle, long long offset) NOT IMPLEMENTED\n");

    // FIXME: openjfx2.26 implement truncateFile
    UNUSED_PARAM(offset);
    return false;
}

std::optional<int32_t> getFileDeviceId(const String&)
{
    fprintf(stderr, "getFileDeviceId(const String&) NOT IMPLEMENTED\n");
    return {};
}


bool unmapViewOfFile(void* , size_t)
{
    fprintf(stderr, "unmapViewOfFile(void* , size_t) NOT IMPLEMENTED()\n");
    return false;
}


bool deleteFile(const String&)
{
    fprintf(stderr, "deleteFile(const String&) NOT IMPLEMENTED\n");
    return false;
}

bool deleteEmptyDirectory(String const &)
{
    fprintf(stderr, "deleteEmptyDirectory(String const &) NOT IMPLEMENTED\n");
    return false;
}

std::pair<String, FileHandle> openTemporaryFile(StringView prefix, StringView suffix, const String& temporaryDirectory)
{
    fprintf(stderr, "openTemporaryFile(const String&, PlatformFileHandle& handle, const String&) NOT IMPLEMENTED\n");
    UNUSED_PARAM(prefix);
    UNUSED_PARAM(suffix);
    UNUSED_PARAM(temporaryDirectory);
    return { String(), FileHandle() };
}

String parentPath(const String& path)
{
    fprintf(stderr, "parentPath(const String& path) NOT IMPLEMENTED\n");
    UNUSED_PARAM(path);
    return String();
}

bool moveFile(const String& oldPath, const String& newPath)
{
    fprintf(stderr, "moveFile(const String& oldPath, const String& newPath) NOT IMPLEMENTED\n");
    UNUSED_PARAM(oldPath);
    UNUSED_PARAM(newPath);

    return false;
}

bool isHiddenFile(const String& path)
{
    fprintf(stderr, "isHiddenFile(const String& path) NOT IMPLEMENTED\n");
    UNUSED_PARAM(path);
    return false;
}

bool hardLinkOrCopyFile(const String& targetPath, const String& linkPath)
{
    fprintf(stderr, "hardLinkOrCopyFile(const String& targetPath, const String& linkPath) NOT IMPLEMENTED\n");
    UNUSED_PARAM(targetPath);
    UNUSED_PARAM(linkPath);

    return false;
}

std::optional<FileType> fileTypeFollowingSymlinks(const String& path)
{
    fprintf(stderr, "fileTypeFollowingSymlinks(const String& path) NOT IMPLEMENTED\n");
    UNUSED_PARAM(path);
    return {};
}

std::optional<FileType> fileType(const String& path)
{
    fprintf(stderr, "fileType(const String& path) NOT IMPLEMENTED\n");
    UNUSED_PARAM(path);
    return {};
}

void deleteAllFilesModifiedSince(const String& path, WallTime t)
{
    fprintf(stderr, "deleteAllFilesModifiedSince(const String&, WallTime) NOT IMPLEMENTED\n");
    UNUSED_PARAM(path);
    UNUSED_PARAM(t);
}

bool flushFile(PlatformFileHandle handle)
{
     fprintf(stderr, "flushFile(PlatformFileHandle) NOT IMPLEMENTED\n");
     UNUSED_PARAM(handle);
     return false;
}

std::optional<Vector<uint8_t>> readEntireFile(PlatformFileHandle handle)
{
    fprintf(stderr, "readEntireFile(PlatformFileHandle handle) NOT IMPLEMENTED\n");
    UNUSED_PARAM(handle);
    Vector<uint8_t> vec;
    return vec;
}
std::optional<Vector<uint8_t>> readEntireFile(const String& path)
{
    fprintf(stderr, "readEntireFile(const String& path) NOT IMPLEMENTED\n");
    UNUSED_PARAM(path);
    Vector<uint8_t> vec;
    return vec;
}

bool deleteNonEmptyDirectory(String const &)
{
    fprintf(stderr, "deleteNonEmptyDirectory(String const &) NOT IMPLEMENTED\n");
    return false;
}

std::optional<uint64_t> fileSize(PlatformFileHandle handle)
{
    long long size = 0;
    fprintf(stderr, "readEntireFile(PlatformFileHandle) NOT IMPLEMENTED\n");
    UNUSED_PARAM(handle);
    return size;
}

std::optional<PlatformFileID> fileID(PlatformFileHandle fileHandle)
{
    UNUSED_PARAM(fileHandle);
    return std::nullopt;
}

bool fileIDsAreEqual(std::optional<PlatformFileID> a, std::optional<PlatformFileID> b)
{
    fprintf(stderr, "fileIDsAreEqual(std::optional<PlatformFileID> a, std::optional<PlatformFileID> b) NOT IMPLEMENTED\n");
    UNUSED_PARAM(a);
    UNUSED_PARAM(b);
    return true;
}

std::optional<uint64_t> overwriteEntireFile(const String& path, std::span<const uint8_t>)
{
    fprintf(stderr, "overwriteEntireFile(const String& path, std::span<const uint8_t>) NOT IMPLEMENTED\n");
    return {};
}

int64_t writeToFile(PlatformFileHandle, std::span<const uint8_t> data)
{
     fprintf(stderr, "writeToFile(PlatformFileHandle, std::span<const uint8_t> data) NOT IMPLEMENTED\n");
     return 0;
}

int64_t readFromFile(PlatformFileHandle, std::span<uint8_t> data)
{
      fprintf(stderr, "readFromFile(PlatformFileHandle, std::span<uint8_t> data) NOT IMPLEMENTED\n");
      return 0;
}

FileHandle createDumpFile(StringView filename, StringView extension, StringView path)
{
    if (path.isEmpty()) {
        auto [p, handle] = openTemporaryFile(filename, extension);
        return WTF::move(handle);
    }
    return openFile(makeString(path, pathSeparator, filename, extension), FileOpenMode::Truncate);
}

} // namespace FileSystemImpl

} // namespace WTF
