/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "NotImplemented.h"
#include "FileSystem.h"
#include "FileMetadata.h"
#include "JavaEnv.h"
#include <text/CString.h>

static jclass GetFileSystemClass(JNIEnv* env)
{
    static JGClass clazz(env->FindClass("com/sun/webkit/FileSystem"));
    ASSERT(clazz);
    return clazz;
}

namespace WebCore {

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

bool getFileModificationTime(const String&, time_t& result)
{
    notImplemented();
    return false;
}

bool getFileCreationTime(const String&, time_t& result)
{
    notImplemented(); // todo tav
    return false;
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
