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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Timer;

/**
 * MS Windows platform implementation class for Timer.
 * <p>
 * Every one of the four natives this class used to declare has become a call into
 * {@link WinGlassNative}, which binds {@code winmm!timeGetDevCaps}, {@code timeBeginPeriod},
 * {@code timeEndPeriod}, {@code timeSetEvent} and {@code timeKillEvent} directly; there is no
 * {@code Timer.cpp} behind it any more. Behaviour is unchanged, including the two period bounds being
 * queried once and cached here for the life of the JVM.
 */
final class WinTimer extends Timer {

    static {
        minPeriod = WinGlassNative.timerMinPeriod();
        maxPeriod = WinGlassNative.timerMaxPeriod();
    }

    private static final int minPeriod, maxPeriod;

    protected WinTimer(Runnable runnable) {
        super(runnable);
    }

    static int getMinPeriod_impl() {
        return minPeriod;
    }

    static int getMaxPeriod_impl() {
        return maxPeriod;
    }

    @Override protected long _start(Runnable runnable) {
        throw new RuntimeException("vsync timer not supported");
    }
    @Override protected long _start(Runnable runnable, int period) {
        return WinGlassNative.timerStart(runnable, period);
    }
    @Override protected void _stop(long timer) {
        WinGlassNative.timerStop(timer);
    }
    @Override protected void _pause(long timer) {}
    @Override protected void _resume(long timer) {}
}

