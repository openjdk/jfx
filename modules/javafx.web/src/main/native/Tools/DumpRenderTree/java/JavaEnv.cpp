/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaEnv.h"

JavaVM* jvm = 0;


jclass dumpRenderTreeClass;
static jmethodID waitUntilDoneMID;
static jmethodID notifyDoneMID;
static jmethodID overridePreferenceMID;
static jmethodID getBackForwardItemCountMID;
static jmethodID clearBackForwardListMID;
static jmethodID resolveURLMID;
static jmethodID loadURLMID;
static jmethodID goBackForwardMID;

jclass getDumpRenderTreeClass() { return dumpRenderTreeClass; }
jmethodID getWaitUntillDoneMethodId() { return waitUntilDoneMID; }
jmethodID getNotifyDoneMID() { return notifyDoneMID; }
jmethodID getOverridePreferenceMID() { return overridePreferenceMID; }
jmethodID getGetBackForwardItemCountMID() { return getBackForwardItemCountMID; }
jmethodID getClearBackForwardListMID() { return clearBackForwardListMID; }
jmethodID getResolveURLMID() { return resolveURLMID; }
jmethodID getLoadURLMID() { return loadURLMID; }
jmethodID getGoBackForward() { return goBackForwardMID; }

static void initRefs(JNIEnv* env) {
    if (!dumpRenderTreeClass) {
        jclass cls =  env->FindClass("com/sun/javafx/webkit/drt/DumpRenderTree");
        dumpRenderTreeClass = (jclass)env->NewGlobalRef(cls);
        if (JNI_TRUE == env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                return;
            }
        ASSERT(dumpRenderTreeClass);
        waitUntilDoneMID = env->GetStaticMethodID(dumpRenderTreeClass, "waitUntilDone", "()V");
        ASSERT(waitUntilDoneMID);
        notifyDoneMID = env->GetStaticMethodID(dumpRenderTreeClass, "notifyDone", "()V");
        ASSERT(notifyDoneMID);
        overridePreferenceMID = env->GetStaticMethodID(dumpRenderTreeClass, "overridePreference", "(Ljava/lang/String;Ljava/lang/String;)V");
        ASSERT(overridePreferenceMID);
        getBackForwardItemCountMID = env->GetStaticMethodID(dumpRenderTreeClass, "getBackForwardItemCount", "()I");
        ASSERT(getBackForwardItemCountMID);
        clearBackForwardListMID = env->GetStaticMethodID(dumpRenderTreeClass, "clearBackForwardList", "()V");
        ASSERT(clearBackForwardListMID);
        resolveURLMID = env->GetStaticMethodID(dumpRenderTreeClass, "resolveURL", "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(resolveURLMID);
        loadURLMID = env->GetStaticMethodID(dumpRenderTreeClass, "loadURL", "(Ljava/lang/String;)V");
        ASSERT(loadURLMID);
        goBackForwardMID = env->GetStaticMethodID(dumpRenderTreeClass, "goBackForward", "(I)V");
        ASSERT(goBackForwardMID);
    }
}


JNIEnv* JNICALL DumpRenderTree_GetJavaEnv()
{
    void* env;
    jvm->GetEnv(&env, JNI_VERSION_1_2);
    return (JNIEnv*)env;
}

bool CheckAndClearException(JNIEnv* env)
{
    if (JNI_TRUE == env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    jvm = vm;
    JNIEnv *env;
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_2)) {
        fprintf(stderr, "DumpRenderTree::JNI_OnLoad() failed \n");
             return JNI_ERR; /* JNI version not supported */
         }
     initRefs(env);
    return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv *env = DumpRenderTree_GetJavaEnv();
    env->DeleteGlobalRef(dumpRenderTreeClass);
    jvm = 0;
}

#if OS(WINDOWS)
#include <Windows.h>
#include <math.h>

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved)
{
    if (fdwReason == DLL_PROCESS_ATTACH) {
#if defined(_MSC_VER) && _MSC_VER >= 1800 && _MSC_VER < 1900 && defined(_M_X64) || defined(__x86_64__)
        // The VS2013 runtime has a bug where it mis-detects AVX-capable processors
        // if the feature has been disabled in firmware. This causes us to crash
        // in some of the math functions. For now, we disable those optimizations
        // because Microsoft is not going to fix the problem in VS2013.
        // FIXME: Remove this workaround when we switch to VS2015+.
        _set_FMA3_enable(0);
#endif
    }

    return TRUE;
}
#endif

#ifdef __cplusplus
}
#endif
