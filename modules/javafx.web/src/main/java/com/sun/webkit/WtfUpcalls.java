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

package com.sun.webkit;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;

/**
 * The {@code WKJHostWTF} group: the one upcall {@code Source/WTF/wtf/java} makes that is not a file
 * system call, {@code MainThread.fwkScheduleDispatchFunctions()}.
 * <p>
 * It is not optional. {@code WTF::scheduleDispatchFunctionsOnMainThread} is how work queued with
 * {@code WTF::callOnMainThread} - from JavaScript worker threads, from {@code WorkQueue} threads and
 * from the network threads - reaches the JavaFX application thread. Without this slot the schedule
 * half of the round trip is dead while the run half, {@code wkj_main_thread_dispatch_functions},
 * stays bound: nothing would ever ask Java to post the runnable that calls it, and every
 * {@code callOnMainThread} in the library would simply never run. Shutdown goes through the same
 * path.
 * <p>
 * The upcall is deliberately asynchronous: it schedules, it does not run. Any thread may make it.
 * The JNI version attached the calling thread as a non-daemon thread first; an FFM upcall stub
 * attaches and detaches on its own, so that distinction is gone, and nothing depended on it - it
 * only governed whether the JVM would wait for a WebKit-owned thread at exit.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation: the stub is created by
 * {@link WebKitNative#installHostSlot}.
 *
 * @see com.sun.webkit.MainThread
 */
final class WtfUpcalls {

    private WtfUpcalls() {
    }

    /**
     * Fills the {@code wtf} group of a {@code WKJHost} table under construction.
     *
     * @param host the table
     */
    static void install(MemorySegment host) {
        WebKitNative.installHostSlot(host, "wtf.main_thread_schedule_dispatch",
                MethodHandles.lookup(), "mainThreadScheduleDispatch", FunctionDescriptor.ofVoid());
    }

    /*
     * Default when NULL: no-op, which is exactly what the null JNI environment produced once
     * g_ShuttingDown was set. The shutdown gate itself stays in C++, where the caller tests
     * WTF::wkjIsShuttingDown() before reaching this slot, so there is nothing to re-check here.
     */
    private static void mainThreadScheduleDispatch() {
        try {
            MainThread.fwkScheduleDispatchFunctions();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("wtf.main_thread_schedule_dispatch", t);
        }
    }
}
