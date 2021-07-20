/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

bool deleteFile(const String&)
{
    return false;
}

bool deleteEmptyDirectory(String const &)
{
    return false;
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

Optional<WallTime> getFileModificationTime(const String& path)
{
    Optional<FileMetadata> metadata = fileMetadata(path);
    if (metadata) {
        return { metadata->modificationTime };
    } else {
        return { };
    }
}

Optional<WallTime> getFileCreationTime(const String&) // Not all platforms store file creation time.
{
    return { };
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

String homeDirectoryPath()
{
    return "";
}

String directoryName(String const &)
{
    return String();
}

Optional<FileMetadata> fileMetadata(const String& path)
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

Optional<FileMetadata> fileMetadataFollowingSymlinks(const String& path)
{
    // TODO-java: Use nio Files to avoid sym link traversal
    return fileMetadata(path);
}

Vector<String> listDirectory(const String&, const String&)
{
    Vector<String> entities;
    return entities;
}

CString fileSystemRepresentation(const String& s)
{
    return CString(s.latin1().data());
}

String openTemporaryFile(const String&, PlatformFileHandle& handle, const String&)
{
    handle = invalidPlatformFileHandle;
    return String();
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

int readFromFile(PlatformFileHandle handle, char* data, int length)
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

int writeToFile(PlatformFileHandle, const char*, int)
{
    return -1;
}

bool truncateFile(PlatformFileHandle, long long offset)
{
    // FIXME: openjfx2.26 implement truncateFile
    UNUSED_PARAM(offset);
    fprintf(stderr, "FileSystemJava::truncateFile notImplemented\n");
    return false;
}

String pathGetFileName(const String& path)
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

Optional<int32_t> getFileDeviceId(const CString&)
{
    return {};
}

bool MappedFileData::mapFileHandle(PlatformFileHandle, FileOpenMode, MappedFileMode)
{
    fprintf(stderr, "MappedFileData::mapFileHandle(PlatformFileHandle handle, MappedFileMode) notImplemented()\n");
    return false;
}

bool unmapViewOfFile(void* , size_t)
{
    fprintf(stderr, "unmapViewOfFile(void* , size_t) notImplemented()\n");
    return false;
}

} // namespace FileSystemImpl

} // namespace WTF
