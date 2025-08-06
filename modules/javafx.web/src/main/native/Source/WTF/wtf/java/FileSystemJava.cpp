/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
#include "FileSystem.h"
#include "FileMetadata.h"
#include <wtf/java/JavaEnv.h>
#include <wtf/text/CString.h>

namespace WTF {

namespace FileSystemImpl {


// -----------------------------------------------------------------------
//  Below methods use Java calls to implement the intended functionality.
// -----------------------------------------------------------------------
bool fileExists(const String& path)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkFileExists",
            "(Ljava/lang/String;)Z");
    ASSERT(mid);

    jboolean result = env->CallStaticBooleanMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring)path.toJavaString(env));
    WTF::CheckAndClearException(env);

    return jbool_to_bool(result);
}


bool getFileSize(const String& path, long long& result)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkGetFileSize",
            "(Ljava/lang/String;)J");
    ASSERT(mid);

    jlong size = env->CallStaticLongMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring) path.toJavaString(env));
    WTF::CheckAndClearException(env);

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
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkGetFileMetadata",
            "(Ljava/lang/String;[J)Z");
    ASSERT(mid);

    JLocalRef<jlongArray> lArray(env->NewLongArray(3));

    jboolean result = env->CallStaticBooleanMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring)path.toJavaString(env), (jlongArray)lArray);
    WTF::CheckAndClearException(env);

    if (result) {
        jlong* metadataResults = env->GetLongArrayElements(lArray, 0);
        FileMetadata metadata {};
        metadata.modificationTime = WallTime::fromRawSeconds(metadataResults[0] / 1000.0);
        metadata.length = metadataResults[1];
        metadata.type = static_cast<FileMetadata::Type>(metadataResults[2]);
        env->ReleaseLongArrayElements(lArray, metadataResults, 0);
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

String pathByAppendingComponents(StringView path, const Vector<StringView>& components)
{
    String result = path.toString();
    // FIXME-java: Use nio.file.Paths.get(...)
    for (const auto& component : components) {
        result = pathByAppendingComponent(result, component.toString());
    }

    return result;
}

String pathByAppendingComponent(const String& path, const String& component)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkPathByAppendingComponent",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring)path.toJavaString(env),
            (jstring)component.toJavaString(env)));
    WTF::CheckAndClearException(env);

    return String(env, result);
}

String pathByAppendingComponent(StringView path, StringView component)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkPathByAppendingComponent",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring)path.toString().toJavaString(env),
            (jstring)component.toString().toJavaString(env)));
    WTF::CheckAndClearException(env);

    return String(env, result);
}

bool makeAllDirectories(const String& path)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkMakeAllDirectories",
            "(Ljava/lang/String;)Z");
    ASSERT(mid);

    jboolean result = env->CallStaticBooleanMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring)path.toJavaString(env));
    WTF::CheckAndClearException(env);

    return jbool_to_bool(result);
}


CString fileSystemRepresentation(const String& s)
{
    return CString(s.latin1().data());
}

PlatformFileHandle openFile(const String& path, FileOpenMode mode, FileAccessPermission, bool)
{
    if (mode != FileOpenMode::Read) {
        return invalidPlatformFileHandle;
    }
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkOpenFile",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/io/RandomAccessFile;");
    ASSERT(mid);

    PlatformFileHandle result = env->CallStaticObjectMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring)path.toJavaString(env), (jstring)(env->NewStringUTF("r")));

    WTF::CheckAndClearException(env);
    return result ? result : invalidPlatformFileHandle;
}

void closeFile(PlatformFileHandle& handle)
{
    if (isHandleValid(handle)) {
        JNIEnv* env = WTF::GetJavaEnv();
        static jmethodID mid = env->GetStaticMethodID(
                comSunWebkitFileSystem,
                "fwkCloseFile",
                "(Ljava/io/RandomAccessFile;)V");
        ASSERT(mid);

        env->CallStaticVoidMethod(
                comSunWebkitFileSystem,
                mid, (jobject)handle);
        WTF::CheckAndClearException(env);
        handle = invalidPlatformFileHandle;
    }
}

int readFromFile(PlatformFileHandle handle, void* data, int length)
{
    if (length < 0 || !isHandleValid(handle) || data == nullptr) {
        return -1;
    }
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkReadFromFile",
            "(Ljava/io/RandomAccessFile;Ljava/nio/ByteBuffer;)I");
    ASSERT(mid);

    int result = env->CallStaticIntMethod(
            comSunWebkitFileSystem,
            mid,
            (jobject)handle,
            (jobject)(env->NewDirectByteBuffer(data, length)));
    WTF::CheckAndClearException(env);

    if (result < 0) {
        return -1;
    }
    return result;
}

String pathFileName(const String& path)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkPathGetFileName",
            "(Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            comSunWebkitFileSystem,
            mid,
            (jstring) path.toJavaString(env)));
    WTF::CheckAndClearException(env);

    return String(env, result);
}

long long seekFile(PlatformFileHandle handle, long long offset, FileSeekOrigin)
{
    // we always get positive value for offset from webkit.
    // Below check for offset < 0 might be redundant?
    if (offset < 0 || !isHandleValid(handle)) {
        return -1;
    }
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetStaticMethodID(
            comSunWebkitFileSystem,
            "fwkSeekFile",
            "(Ljava/io/RandomAccessFile;J)V");
    ASSERT(mid);

    env->CallStaticVoidMethod(
            comSunWebkitFileSystem,
            mid,
            (jobject)handle, (jlong)offset);
    if (WTF::CheckAndClearException(env)) {
        offset = -1;
    }
    return offset;
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

bool MappedFileData::mapFileHandle(PlatformFileHandle, FileOpenMode, MappedFileMode)
{
    fprintf(stderr, "MappedFileData::mapFileHandle(PlatformFileHandle handle, MappedFileMode) NOT IMPLEMENTED\n");
    return false;
}

bool unmapViewOfFile(void* , size_t)
{
    fprintf(stderr, "unmapViewOfFile(void* , size_t) NOT IMPLEMENTED()\n");
    return false;
}

MappedFileData::~MappedFileData()
{
    if (!m_fileData)
        return;
    unmapViewOfFile(m_fileData, m_fileSize);
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

String openTemporaryFile(StringView prefix, PlatformFileHandle& handle, StringView suffix)
{
    fprintf(stderr, "openTemporaryFile(const String&, PlatformFileHandle& handle, const String&) NOT IMPLEMENTED\n");
    handle = invalidPlatformFileHandle;
        UNUSED_PARAM(prefix);
        UNUSED_PARAM(suffix);
    return String();
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

int overwriteEntireFile(const String& path, std::span<const uint8_t>)
{
    fprintf(stderr, "overwriteEntireFile(const String& path, std::span<const uint8_t>) NOT IMPLEMENTED\n");
    return 0;
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

std::pair<String, PlatformFileHandle> openTemporaryFile(StringView prefix, StringView suffix)
{
     fprintf(stderr, "openTemporaryFile(StringView prefix, StringView suffix) return { String(), nullptr}\n");
     return { String(), nullptr};
}

} // namespace FileSystemImpl

} // namespace WTF
