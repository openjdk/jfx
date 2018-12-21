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
#include <wtf/java/JavaEnv.h>

#include <wtf/Assertions.h>


JavaVM* jvm = 0;

bool CheckAndClearException(JNIEnv* env)
{
    if (JNI_TRUE == env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

namespace WebCore {

jclass PG_GetGraphicsManagerClass(JNIEnv* env)
{
    static JGClass graphicsManagerCls(
        env->FindClass("com/sun/webkit/graphics/WCGraphicsManager"));
    ASSERT(graphicsManagerCls);
    return graphicsManagerCls;
}

jclass PG_GetGraphicsContextClass(JNIEnv* env)
{
    static JGClass graphicsContextCls(
        env->FindClass("com/sun/webkit/graphics/WCGraphicsContext"));
    ASSERT(graphicsContextCls);
    return graphicsContextCls;
}

jclass PG_GetPathClass(JNIEnv* env)
{
    static JGClass pathCls(
        env->FindClass("com/sun/webkit/graphics/WCPath"));
    ASSERT(pathCls);
    return pathCls;
}

jclass PG_GetPathIteratorClass(JNIEnv* env)
{
    static JGClass pathIteratorCls(
        env->FindClass("com/sun/webkit/graphics/WCPathIterator"));
    ASSERT(pathIteratorCls);
    return pathIteratorCls;
}


jclass PG_GetImageClass(JNIEnv* env)
{
    static JGClass imageCls(
        env->FindClass("com/sun/webkit/graphics/WCImage"));
    ASSERT(imageCls);
    return imageCls;
}

jclass PG_GetImageFrameClass(JNIEnv* env)
{
    static JGClass imageFrameCls(
        env->FindClass("com/sun/webkit/graphics/WCImageFrame"));
    ASSERT(imageFrameCls);
    return imageFrameCls;
}

jclass PG_GetRectangleClass(JNIEnv* env)
{
    static JGClass rectangleCls(
        env->FindClass("com/sun/webkit/graphics/WCRectangle"));
    ASSERT(rectangleCls);
    return rectangleCls;
}

jclass PG_GetFontClass(JNIEnv* env)
{
    static JGClass fontCls(
        env->FindClass("com/sun/webkit/graphics/WCFont"));
    ASSERT(fontCls);
    return fontCls;
}

jclass PG_GetFontCustomPlatformDataClass(JNIEnv* env)
{
    static JGClass fontCustomPlatformDataCls(env->FindClass(
            "com/sun/webkit/graphics/WCFontCustomPlatformData"));
    ASSERT(fontCustomPlatformDataCls);
    return fontCustomPlatformDataCls;
}

JLObject PL_GetGraphicsManager(JNIEnv* env)
{
    static jmethodID getGraphicsManagerMID = env->GetStaticMethodID(PG_GetGraphicsManagerClass(env),
            "getGraphicsManager",
            "()Lcom/sun/webkit/graphics/WCGraphicsManager;");
    ASSERT(getGraphicsManagerMID);

    JLObject mgr(env->CallStaticObjectMethod(
        PG_GetGraphicsManagerClass(env), getGraphicsManagerMID));
    ASSERT(mgr);
    CheckAndClearException(env);

    return mgr;
}

jclass PG_GetGraphicsImageDecoderClass(JNIEnv* env)
{
    static JGClass graphicsImageDecoderCls(
        env->FindClass("com/sun/webkit/graphics/WCImageDecoder"));
    ASSERT(graphicsImageDecoderCls);
    return graphicsImageDecoderCls;
}

jclass PG_GetRefClass(JNIEnv* env)
{
    static JGClass refCls(
        env->FindClass("com/sun/webkit/graphics/Ref"));
    ASSERT(refCls);
    return refCls;
}

jclass PG_GetRenderQueueClass(JNIEnv* env)
{
    static JGClass rqCls(
        env->FindClass("com/sun/webkit/graphics/WCRenderQueue"));
    ASSERT(rqCls);
    return rqCls;
}

jclass PG_GetMediaPlayerClass(JNIEnv* env)
{
    static JGClass mediaPlayerCls(
        env->FindClass("com/sun/webkit/graphics/WCMediaPlayer"));
    ASSERT(mediaPlayerCls);
    return mediaPlayerCls;
}

jclass PG_GetTransformClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/graphics/WCTransform"));
    ASSERT(cls);
    return cls;
}

jclass PG_GetWebPageClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/WebPage"));
    ASSERT(cls);
    return cls;
}

jclass PG_GetColorChooserClass(JNIEnv* env)
{
    static JGClass cls(
        env->FindClass("com/sun/webkit/ColorChooser"));
    return cls;
}

jclass getTimerClass(JNIEnv* env)
{
    static JGClass timerCls(
        env->FindClass("com/sun/webkit/Timer"));
    return timerCls;
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

} // namespace WebCore

extern "C" {

#if PLATFORM(JAVA_WIN) && !defined(NDEBUG)
#include <crtdbg.h>
#endif
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*)
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
