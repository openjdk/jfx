/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "JavaEnv.h"

JavaVM* jvm = 0;

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
    return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnLoad(JavaVM* vm, void* reserved)
{
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
