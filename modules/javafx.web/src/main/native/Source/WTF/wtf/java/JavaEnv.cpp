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

#include <wtf/Assertions.h>
#include <wtf/java/JavaEnv.h>

JavaVM* jvm = 0;

namespace WTF {

bool CheckAndClearException(JNIEnv* env)
{
    if (JNI_TRUE == env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}


jclass PL_GetClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/perf/PerfLogger"));
    return cls;
}

JLObject PL_GetLogger(JNIEnv* env, const char* name)
{
    static jmethodID mid =
        env->GetStaticMethodID(PL_GetClass(env),
            "getLogger",
            "(Ljava/lang/String;)Lcom/sun/webkit/perf/PerfLogger;");
    ASSERT(mid);

    JLObject jlogger(env->CallStaticObjectMethod(PL_GetClass(env), mid,
        (jstring)JLString(env->NewStringUTF(name))));
    CheckAndClearException(env);

    return jlogger;
}

void PL_ResumeCount(JNIEnv* env, jobject perfLogger, const char* probe)
{
    static jmethodID mid =
        env->GetMethodID(PL_GetClass(env),
            "resumeCount",
            "(Ljava/lang/String;)V");
    ASSERT(mid);

    env->CallVoidMethod(perfLogger, mid,
        (jstring)JLString(env->NewStringUTF(probe)));
    CheckAndClearException(env);
}

void PL_SuspendCount(JNIEnv* env, jobject perfLogger, const char* probe)
{
    static jmethodID mid =
        env->GetMethodID(PL_GetClass(env),
            "suspendCount",
            "(Ljava/lang/String;)V");
    ASSERT(mid);

    env->CallVoidMethod(perfLogger, mid,
        (jstring)JLString(env->NewStringUTF(probe)));
    CheckAndClearException(env);
}

bool PL_IsEnabled(JNIEnv* env, jobject perfLogger)
{
    static jmethodID mid =
        env->GetMethodID(PL_GetClass(env),
            "isEnabled",
            "()Z");
    ASSERT(mid);

    static jboolean isEnabled = env->CallBooleanMethod(perfLogger, mid);
    CheckAndClearException(env);

    return isEnabled;
}

} // namespace WTF

extern "C" {

#if PLATFORM(JAVA_WIN) && !defined(NDEBUG)
#include <crtdbg.h>
#endif
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL JNI_OnLoad_jfxwebkit(JavaVM* vm, void*)
#else
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*)
#endif
{
#if PLATFORM(JAVA_WIN) && !defined(NDEBUG)
    _CrtSetReportMode( _CRT_ERROR, _CRTDBG_MODE_FILE );
    _CrtSetReportFile( _CRT_ERROR, _CRTDBG_FILE_STDERR );

    // Get current flag
    int tmpFlag = _CrtSetDbgFlag( _CRTDBG_REPORT_FLAG );

    // Turn on leak-checking bit
    tmpFlag |= _CRTDBG_CHECK_CRT_DF | _CRTDBG_LEAK_CHECK_DF; //| _CRTDBG_CHECK_EVERY_1024_DF;

    // Set flag to the new value
    _CrtSetDbgFlag( tmpFlag );
#endif
    jvm = vm;
    return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* , void*)
{
#if PLATFORM(JAVA_WIN) && !defined(NDEBUG)
    _CrtDumpMemoryLeaks();
#endif
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

}
