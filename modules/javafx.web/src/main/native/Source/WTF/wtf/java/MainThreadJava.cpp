/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/MainThread.h>
#include <wtf/RunLoop.h>
#include <wtf/java/WKJRuntime.h>

#if OS(UNIX)
#include <pthread.h>
#endif

namespace WTF {

#if OS(UNIX)
static pthread_t s_mainThread;
#elif OS(WINDOWS)
static ThreadIdentifier s_mainThread { 0 };
#endif

void scheduleDispatchFunctionsOnMainThread()
{
    /*
     * SHUTDOWN GATE. The JNI version opened with AttachThreadAsNonDaemonToJavaEnv and then
     * "if (env)", and that environment was null in exactly one case: g_ShuttingDown was set,
     * because AttachThreadToJavaEnv refused to attach once it was (JavaEnv.h:87-99). So the
     * call was silently skipped during teardown. There is no environment to be null now, so
     * the test has to be explicit or dispatch requests would start reaching a Java side that
     * is going away. See THE SHUTDOWN GATE in WKJRuntime.h.
     */
    if (wkjIsShuttingDown())
        return;

    const WKJHostWTF* cb = wkjWTF();
    if (!cb || !cb->main_thread_schedule_dispatch)
        return;

    cb->main_thread_schedule_dispatch();
    wkjCheckAndClearException();
}

void initializeMainThreadPlatform()
{
    /*
     * Nothing but recording which thread is the main one.
     *
     * The JNI version also resolved com.sun.webkit.MainThread and cached
     * fwkScheduleDispatchFunctions here, and the comment that used to sit in this function
     * explained why it had to happen HERE rather than lazily: the call arrives from Java
     * through WebPage.twkCreatePage, so FindClass would use the class loader that loaded
     * WebPage, whereas a lookup from a WebKit-spawned thread would use the system loader and
     * fail when the JavaFX modules are not in the boot layer.
     *
     * That reasoning was entirely about JNI class lookup. The dispatch hook is now a function
     * pointer in WKJHostWTF that Java installs at wkj_init, so there is no class to resolve,
     * no method id to cache, and no constraint on when this function runs. The guard in
     * WTF::initializeMainThread that made it run once is unchanged and still does its job.
     */
#if OS(UNIX)
    s_mainThread = pthread_self();
#elif OS(WINDOWS)
    s_mainThread = Thread::currentID();
#endif
}

#if OS(UNIX)
bool isMainThread()
{
    return pthread_equal(pthread_self(), s_mainThread);
}
#elif OS(WINDOWS)
bool isMainThread()
{
    return s_mainThread == Thread::currentID();
}
#endif

} // namespace WTF

extern "C" {

/* MainThread.twkScheduleDispatchFunctions(); was Java_com_sun_webkit_MainThread_twkScheduleDispatchFunctions. */
void wkj_main_thread_dispatch_functions(void)
{
    WTF::RunLoop::mainSingleton().dispatchFunctionsFromMainThread();
}

/*
 * MainThread.twkSetShutdown(boolean); was Java_com_sun_webkit_MainThread_twkSetShutdown.
 *
 * The flag it sets is the one every shutdown gate in the library reads, so this is the single
 * point at which Java tells the library to stop calling back. It is a downcall, not a
 * callback: the JNI version was a native method too.
 */
void wkj_set_shutdown(int32_t shutting_down)
{
    g_ShuttingDown = shutting_down != 0;
}

} // extern "C"
