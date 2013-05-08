/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "NotImplemented.h"
#include "FileSystem.h"
#include "FileMetadata.h"
#include "JavaEnv.h"

static JGClass fileSystemClass;
static jmethodID pathGetFileNameMID;

static void initRefs(JNIEnv* env)
{
    if (!fileSystemClass) {
        fileSystemClass = JLClass(env->FindClass(
                "com/sun/webkit/FileSystem"));
        ASSERT(fileSystemClass);

        pathGetFileNameMID = env->GetStaticMethodID(
                fileSystemClass,
                "fwkPathGetFileName",
                "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(pathGetFileNameMID);
    }
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

bool getFileSize(String const&, long long&)
{
    notImplemented();
    return false;
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

void closeFile(PlatformFileHandle&)
{
    notImplemented();
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
    initRefs(env);
    
    JLString result = static_cast<jstring>(env->CallStaticObjectMethod(
            fileSystemClass,
            pathGetFileNameMID,
            (jstring)path.toJavaString(env)));
    CheckAndClearException(env);

    return String(env, result);    
}

} // namespace WebCore
