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

/**
 * FFM facade for {@code wkj_timer_fire}, the one entry point {@link Timer} needs from the
 * {@code jfxwebkit} C ABI.
 * <p>
 * The call runs {@code MainThreadSharedTimer::fired()}, that is the whole WebKit timer queue, and
 * can therefore execute arbitrary script. It is main thread only, which {@link Timer} enforces the
 * way it always has, by firing under {@code WebPage.lockPage()} from the event thread.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class TimerNative {

    private static final MethodHandle FIRE = WebKitNative.downcall(
            "wkj_timer_fire",
            FunctionDescriptor.ofVoid());

    private TimerNative() {
    }

    static void fire() {
        try {
            FIRE.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
