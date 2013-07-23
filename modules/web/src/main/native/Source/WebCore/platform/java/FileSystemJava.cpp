/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "NotImplemented.h"
#include "FileSystem.h"
#include "FileMetadata.h"
#include "JavaEnv.h"

static jclass GetFileSystemClass(JNIEnv* env)
{
    static JGClass clazz(env->FindClass("com/sun/webkit/FileSystem"));
    ASSERT(clazz);
    return clazz;
}

namespace WebCore {

bool fileExists(const String&)
{
    notImplemented();
    return false;
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

bool getFileModificationTime(const String&, time_t& result)
{
    notImplemented();
    return false;
}

String pathByAppendingComponent(const String& path, const String& component)
{
    notImplemented();
    return path + "/" + component;
}

bool makeAllDirectories(const String& path)
{
    notImplemented();
    return false;
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

bool getFileMetadata(const String& path, FileMetadata& metadata)
{
    notImplemented();
    return false;
}

Vector<String> listDirectory(const String& path, const String& filter)
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
    notImplemented();
    return invalidPlatformFileHandle;
}

void closeFile(PlatformFileHandle&)
{
    notImplemented();
}

int readFromFile(PlatformFileHandle handle, char* data, int length)
{
    notImplemented();
    return -1;
}

int writeToFile(PlatformFileHandle, const char* data, int length)
{
    notImplemented();
    return -1;
}

bool unloadModule(PlatformModule)
{
    notImplemented();
    return false;
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

long long seekFile(PlatformFileHandle handle, long long offset, FileSeekOrigin origin)
{
    notImplemented();
    return (long long)(-1);
}

} // namespace WebCore
