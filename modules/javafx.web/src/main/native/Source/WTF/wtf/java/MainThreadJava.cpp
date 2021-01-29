/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <wtf/java/JavaRef.h>
#include <wtf/MainThread.h>
#include <wtf/RunLoop.h>

namespace WTF {
static JGClass jMainThreadCls;
static jmethodID fwkIsMainThread;
static jmethodID fwkScheduleDispatchFunctions;

void scheduleDispatchFunctionsOnMainThread()
{
    AttachThreadAsNonDaemonToJavaEnv autoAttach;
    JNIEnv* env = autoAttach.env();
    env->CallStaticVoidMethod(jMainThreadCls, fwkScheduleDispatchFunctions);
    WTF::CheckAndClearException(env);
}

void initializeMainThreadPlatform()
{
    // Initialize the class reference and methodids for the MainThread. The
    // initialization has to be done from a context where the class
    // com.sun.webkit.MainThread is accessible. When
    // scheduleDispatchFunctionsOnMainThread is invoked, the system class loader
    // would be used to locate the class, which fails if the JavaFX modules are
    // not loaded from the boot module layer.
    //
    // initializeMainThreadPlatform is called through the chain:
    // - com.sun.webkit.WebPage.WebPage
    // - com.sun.webkit.WebPage.twkCreatePage
    // - WTF::initializeMainThread
    // - WTF::initializeMainThreadPlatform
    //
    // As we are invoked through JNI from java, the class loader, that loaded
    // WebPage will be used by FindClass.
    //
    // WTF::initializeMainThread has a guard, so that initialization is only run
    // once

    AttachThreadAsNonDaemonToJavaEnv autoAttach;
    JNIEnv* env = autoAttach.env();

    static JGClass jMainThreadRef(env->FindClass("com/sun/webkit/MainThread"));
    jMainThreadCls = jMainThreadRef;

    fwkIsMainThread = env->GetStaticMethodID(
            jMainThreadCls,
            "fwkIsMainThread",
            "()Z");

    ASSERT(fwkIsMainThread);

    fwkScheduleDispatchFunctions = env->GetStaticMethodID(
            jMainThreadCls,
            "fwkScheduleDispatchFunctions",
            "()V");

    ASSERT(fwkScheduleDispatchFunctions);

#if OS(WINDOWS)
    RunLoop::registerRunLoopMessageWindowClass();
#endif
}

bool isMainThreadIfInitialized()
{
    return isMainThread();
}

bool isMainThread()
{
    AttachThreadAsNonDaemonToJavaEnv autoAttach;
    JNIEnv* env = autoAttach.env();
    jboolean isMainThread = env->CallStaticBooleanMethod(jMainThreadCls, fwkIsMainThread);
    WTF::CheckAndClearException(env);
    return isMainThread == JNI_TRUE;
}

extern "C" {

/*
 * Class:     com_sun_webkit_MainThread
 * Method:    twkScheduleDispatchFunctions
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_webkit_MainThread_twkScheduleDispatchFunctions
  (JNIEnv*, jobject)
{
    RunLoop::main().dispatchFunctionsFromMainThread();
}
}

} // namespace WTF
