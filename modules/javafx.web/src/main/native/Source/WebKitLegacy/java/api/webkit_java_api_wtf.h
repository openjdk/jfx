/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * webkit_java_api_wtf.h - the WTF slice of the ABI: Source/WTF/wtf/java.
 *
 * WHAT IS HERE
 *
 *   1. WKJHostWTF, a one-slot callback table for com.sun.webkit.MainThread. It is the only
 *      upcall the WTF layer makes that is not a file-system call; the ten file-system
 *      upcalls of wtf/java/FileSystemJava.cpp use WKJHostFileSystem, which is defined in
 *      webkit_java_api_theme.h and must not be duplicated here.
 *   2. The two exported entry points that replace the two JNIEXPORT functions of
 *      wtf/java/MainThreadJava.cpp.
 *
 * WHY THE WTF SLICE NEEDS A GROUP OF ITS OWN
 *
 * Contract section 4 fixes the WKJHost group names, and none of them fits
 * com.sun.webkit.MainThread: it is neither a page, a client, a platform service nor a theme.
 * The alternative - hanging one function pointer off WKJHostTheme, whose header another
 * slice owns - would make two slices edit one file for no gain. So the WTF layer gets the
 * group "wtf", defined here, owned here, and appended to WKJHost after "theme".
 *
 * WKJHostCore is deliberately NOT extended for this. Core is the seven-slot object and
 * exception substrate that every layer uses; a main-thread dispatch hook is a client
 * callback that happens to have WTF as its client, and putting it in core would start the
 * slide back towards "JavaEnv, but with function pointers".
 *
 * WHAT USED TO BE HERE AND IS NOT
 *
 * wtf/java/JavaEnv.h also declared four perf-logger hooks (PL_GetLogger, PL_ResumeCount,
 * PL_SuspendCount, PL_IsEnabled) around com.sun.webkit.perf.PerfLogger. They get no slots:
 * the LOG_PERF_RECORD macro that used them had a single call site, in a file that was in no
 * build list and has since been deleted, so the whole path is dead code. See the note at
 * the end of WKJHostCore in webkit_java_api.h.
 *
 * INTEGRATION - the edit this header requires in webkit_java_api.h
 *
 *     #include "webkit_java_api_wtf.h"      beside the _platform and _theme includes
 *     WKJHostWTF wtf;                       last member of WKJHost
 *
 * The include direction matches webkit_java_api_platform.h and webkit_java_api_theme.h: the
 * master includes this header, and this header does not include the master. WKJHost needs
 * WKJHostWTF as a complete type in the middle of its own body, which a mutual include
 * cannot deliver, and everything used below - WKJ_EXPORT, the <stdint.h> types - is already
 * declared above the include point.
 *
 * CONVENTIONS - inherited from webkit_java_api.h; only the additions are restated.
 * Booleans are int32_t carrying 0 or 1. Every callback slot may be NULL: the library tests
 * the pointer before every call and falls back to the default documented on the slot.
 */

#ifndef WEBKIT_JAVA_API_WTF_H
#define WEBKIT_JAVA_API_WTF_H

/*
 * Included by webkit_java_api.h, not the other way round - see INTEGRATION above. Naming
 * this header directly is a mistake worth catching, because WKJ_EXPORT would not exist yet.
 */
#ifndef WEBKIT_JAVA_API_H
#error "include webkit_java_api.h; it includes this header at the right point"
#endif

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ======================================================================================== */
/* WKJHostWTF - com.sun.webkit.MainThread                                                   */
/* ======================================================================================== */

typedef struct WKJHostWTF {

    /*
     * MainThread.fwkScheduleDispatchFunctions() - asks the Java side to post a runnable onto
     * the JavaFX application thread, which then calls wkj_main_thread_dispatch_functions()
     * below. It is how WTF::callOnMainThread work reaches the main thread, and it is the one
     * upcall in this ABI that is deliberately asynchronous: it schedules, it does not run.
     *
     * THREAD: any. WTF::scheduleDispatchFunctionsOnMainThread is called from whichever
     * thread queued the function - JSC worker threads, WorkQueue threads and the network
     * threads all reach it. The Java target is a plain static that posts to the FX thread,
     * and the JNI version reached it after attaching the calling thread as a NON-daemon
     * thread; an FFM upcall stub attaches and detaches on its own, so the attach disappears
     * and the daemon versus non-daemon distinction with it. That distinction only ever
     * governed whether the JVM would wait for the thread at exit, and these threads are
     * WebKit-owned, not JVM-owned - nothing in the tree depended on it.
     *
     * SHUTDOWN: the JNI version could not fire during teardown, because
     * AttachThreadAsNonDaemonToJavaEnv returned a null environment once g_ShuttingDown was
     * set and the call was then skipped. That gate is preserved in C++ rather than here: the
     * caller tests WTF::wkjIsShuttingDown() before reaching this slot. See the "Phase B
     * hazards" note in api/README.md.
     *
     * Default when NULL: no-op, which is exactly what the null environment produced.
     */
    void (*main_thread_schedule_dispatch)(void);

} WKJHostWTF;

/* ======================================================================================== */
/* Entry points - the two JNIEXPORT functions of wtf/java/MainThreadJava.cpp                */
/* ======================================================================================== */

/*
 * MainThread.twkScheduleDispatchFunctions(). Runs the functions queued for the main thread,
 * i.e. RunLoop::mainSingleton().dispatchFunctionsFromMainThread().
 *
 * THREAD: the JavaFX application thread, which is the WebKit main thread. This is the second
 * half of the round trip that main_thread_schedule_dispatch starts.
 */
WKJ_EXPORT void wkj_main_thread_dispatch_functions(void);

/*
 * MainThread.twkSetShutdown(boolean). Sets the library shutting-down flag, after which the
 * library stops making the upcalls that would otherwise reach a Java side that is tearing
 * down, and ThreadTimers stops rescheduling the shared timer.
 *
 * This replaces the write to the g_ShuttingDown global that
 * Java_com_sun_webkit_MainThread_twkSetShutdown performed. The flag is still a plain C
 * global inside the library (WTF::wkjIsShuttingDown reads it); only the way Java sets it
 * changes. It is a downcall, not a callback: Java tells the library, the library does not
 * ask.
 *
 * `shutting_down` is 0 or 1. Safe to call from any thread and idempotent.
 */
WKJ_EXPORT void wkj_set_shutdown(int32_t shutting_down);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_WTF_H */
