/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "NotImplemented.h"
#include "FileSystem.h"
#include "FileMetadata.h"
#include <wtf/java/JavaEnv.h>
#include <wtf/text/CString.h>


namespace WebCore {

namespace FileSystem {

static jclass GetFileSystemClass(JNIEnv* env)
{
    static JGClass clazz(env->FindClass("com/sun/webkit/FileSystem"));
    ASSERT(clazz);
    return clazz;
}

bool fileExists(const String& path)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkFileExists",
            "(Ljava/lang/String;)Z");
    ASSERT(mid);

    jboolean result = env->CallStaticBooleanMethod(
            GetFileSystemClass(env),
            mid,
            (jstring)path.toJavaString(env));
    CheckAndClearException(env);

    return jbool_to_bool(result);
}

bool deleteFile(const String&)
{
    notImplemented();
    return false;
}

bool deleteEmptyDirectory(String const &)
{
    notImplemented();
    return false;
}

bool getFileSize(const String& path, long long& result)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkGetFileSize",
            "(Ljava/lang/String;)J");
    ASSERT(mid);

    jlong size = env->CallStaticLongMethod(
            GetFileSystemClass(env),
            mid,
            (jstring) path.toJavaString(env));
    CheckAndClearException(env);

    if (size >= 0) {
        result = size;
        return true;
    } else {
        return false;
    }
}

bool getFileModificationTime(const String& path, time_t& result)
{
  std::optional<FileMetadata> metadata = fileMetadata(path);
    if (metadata) {
        result = metadata->modificationTime;
        return true;
    } else {
        return false;
    }
}

bool getFileCreationTime(const String&, time_t&)
{
    notImplemented(); // todo tav
    return false;
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
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkPathByAppendingComponent",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            GetFileSystemClass(env),
            mid,
            (jstring)path.toJavaString(env),
            (jstring)component.toJavaString(env)));
    CheckAndClearException(env);

    return String(env, result);
}

bool makeAllDirectories(const String& path)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkMakeAllDirectories",
            "(Ljava/lang/String;)Z");
    ASSERT(mid);

    jboolean result = env->CallStaticBooleanMethod(
            GetFileSystemClass(env),
            mid,
            (jstring)path.toJavaString(env));
    CheckAndClearException(env);

    return jbool_to_bool(result);
}

String homeDirectoryPath()
{
    notImplemented();
    return "";
}

String directoryName(String const &)
{
    notImplemented();
    return String();
}

std::optional<FileMetadata> fileMetadata(const String& path)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkGetFileMetadata",
            "(Ljava/lang/String;[J)Z");
    ASSERT(mid);

    JLocalRef<jlongArray> lArray(env->NewLongArray(3));

    jboolean result = env->CallStaticBooleanMethod(
            GetFileSystemClass(env),
            mid,
            (jstring)path.toJavaString(env), (jlongArray)lArray);
    CheckAndClearException(env);

    if (result) {
        jlong* metadataResults = env->GetLongArrayElements(lArray, 0);
        FileMetadata metadata {};
        metadata.modificationTime = metadataResults[0] / 1000.0;
        metadata.length = metadataResults[1];
        metadata.type = static_cast<FileMetadata::Type>(metadataResults[2]);
        env->ReleaseLongArrayElements(lArray, metadataResults, 0);
        return metadata;
    }
    return {};
}

std::optional<FileMetadata> fileMetadataFollowingSymlinks(const String& path)
{
    // TODO-java: Use nio Files to avoid sym link traversal
    return fileMetadata(path);
}

Vector<String> listDirectory(const String&, const String&)
{
    Vector<String> entities;
    notImplemented();
    return entities;
}

CString fileSystemRepresentation(const String& s)
{
    notImplemented();
    return CString(s.latin1().data());
}

String openTemporaryFile(const String&, PlatformFileHandle& handle)
{
    notImplemented();
    handle = invalidPlatformFileHandle;
    return String();
}

PlatformFileHandle openFile(const String& path, FileOpenMode mode)
{
    if (mode != FileOpenMode::Read) {
        return invalidPlatformFileHandle;
    }
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkOpenFile",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/io/RandomAccessFile;");
    ASSERT(mid);

    PlatformFileHandle result = env->CallStaticObjectMethod(
            GetFileSystemClass(env),
            mid,
            (jstring)path.toJavaString(env), (jstring)(env->NewStringUTF("r")));

    CheckAndClearException(env);
    return result ? result : invalidPlatformFileHandle;
}

void closeFile(PlatformFileHandle& handle)
{
    if (isHandleValid(handle)) {
        JNIEnv* env = WebCore_GetJavaEnv();
        static jmethodID mid = env->GetStaticMethodID(
                GetFileSystemClass(env),
                "fwkCloseFile",
                "(Ljava/io/RandomAccessFile;)V");
        ASSERT(mid);

        env->CallStaticVoidMethod(
                GetFileSystemClass(env),
                mid, (jobject)handle);
        CheckAndClearException(env);
        handle = invalidPlatformFileHandle;
    }
}

int readFromFile(PlatformFileHandle handle, char* data, int length)
{
    if (length < 0 || !isHandleValid(handle) || data == nullptr) {
        return -1;
    }
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkReadFromFile",
            "(Ljava/io/RandomAccessFile;Ljava/nio/ByteBuffer;)I");
    ASSERT(mid);

    int result = env->CallStaticIntMethod(
            GetFileSystemClass(env),
            mid,
            (jobject)handle,
            (jobject)(env->NewDirectByteBuffer(data, length)));
    CheckAndClearException(env);

    if (result < 0) {
        return -1;
    }
    return result;
}

int writeToFile(PlatformFileHandle, const char*, int)
{
    notImplemented();
    return -1;
}

String pathGetFileName(const String& path)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkPathGetFileName",
            "(Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            GetFileSystemClass(env),
            mid,
            (jstring) path.toJavaString(env)));
    CheckAndClearException(env);

    return String(env, result);
}

long long seekFile(PlatformFileHandle handle, long long offset, FileSeekOrigin)
{
    // we always get positive value for offset from webkit.
    // Below check for offset < 0 might be redundant?
    if (offset < 0 || !isHandleValid(handle)) {
        return -1;
    }
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetStaticMethodID(
            GetFileSystemClass(env),
            "fwkSeekFile",
            "(Ljava/io/RandomAccessFile;J)V");
    ASSERT(mid);

    env->CallStaticVoidMethod(
            GetFileSystemClass(env),
            mid,
            (jobject)handle, (jlong)offset);
    if (CheckAndClearException(env)) {
        offset = -1;
    }
    return offset;
}

std::optional<int32_t> getFileDeviceId(const CString&)
{
    notImplemented();
    return {};
}

} // namespace FileSystem

} // namespace WebCore
