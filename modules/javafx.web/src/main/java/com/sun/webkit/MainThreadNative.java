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
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM facade for the two {@code WTF/wtf/java/MainThreadJava.cpp} entry points of the
 * {@code jfxwebkit} C ABI, used by {@link MainThread}.
 * <p>
 * The pair is one round trip: the library asks Java to schedule work through
 * {@code WKJHostWTF::main_thread_schedule_dispatch}, and Java calls
 * {@code wkj_main_thread_dispatch_functions} back on the JavaFX application thread, which is the
 * WebKit main thread. Only the second half is a downcall and therefore only the second half is
 * here; the callback slot lives in {@code WKJHostWTF}, a group of the process wide host table that
 * {@link WebKitNative} still installs as a placeholder.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class MainThreadNative {

    private static final MethodHandle DISPATCH_FUNCTIONS = WebKitNative.downcall(
            "wkj_main_thread_dispatch_functions",
            FunctionDescriptor.ofVoid());
    private static final MethodHandle SET_SHUTDOWN = WebKitNative.downcall(
            "wkj_set_shutdown",
            FunctionDescriptor.ofVoid(JAVA_INT));

    private MainThreadNative() {
    }

    /**
     * Runs the functions queued for the WebKit main thread. Must be called on the JavaFX
     * application thread, which {@link MainThread} guarantees by posting through the invoker.
     */
    static void dispatchFunctions() {
        try {
            DISPATCH_FUNCTIONS.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Sets the library's shutting down flag, after which it stops making the upcalls that would
     * otherwise reach a Java side that is tearing down. Safe from any thread and idempotent.
     *
     * @param shutdown whether the toolkit is shutting down
     */
    static void setShutdown(boolean shutdown) {
        try {
            SET_SHUTDOWN.invokeExact(shutdown ? 1 : 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
