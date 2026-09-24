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

package com.sun.glass.ui.win;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Test access to {@link WinTimer}, the Glass {@code Timer} peer on Windows, which is both
 * package-private and final.
 * <p>
 * The five {@code _start}/{@code _stop}/{@code _pause}/{@code _resume} entry points are called
 * directly rather than through {@code com.sun.glass.ui.Timer.start(int)}, because that one needs a
 * running {@code Application} for its range check ({@code Timer.java:88}) and these tests are headless
 * and toolkit-free. Everything else about the peer - the cached period bounds, the vsync refusal, the
 * empty pause and resume - is exercised as it stands.
 * <p>
 * This is deliberately the same harness before and after the JNI to FFM flip: the test code that
 * drives it does not name either implementation, so the timing numbers it produces compare like with
 * like.
 * <p>
 * Line numbers into {@code Timer.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/Timer.cpp}).
 */
public final class WinTimerShim {

    private final WinTimer timer;
    private long id;

    /** A peer for {@code runnable}; nothing is started until {@link #start}. */
    public WinTimerShim(Runnable runnable) {
        this.timer = new WinTimer(runnable);
    }

    /**
     * {@code WinTimer._start(runnable, period)}.
     *
     * @return the id the platform returned, or 0 if the timer did not start
     */
    public long start(Runnable runnable, int period) {
        id = timer._start(runnable, period);
        return id;
    }

    /** {@code WinTimer._start(runnable)}: the vsync timer, which Windows does not have. */
    public long startVsync(Runnable runnable) {
        id = timer._start(runnable);
        return id;
    }

    /** {@code WinTimer._stop(id)}; a no-op when the timer never started or is already stopped. */
    public void stop() {
        if (id != 0L) {
            timer._stop(id);
            id = 0L;
        }
    }

    /** {@code WinTimer._pause(id)}: empty on Windows, and still empty after the migration. */
    public void pause() {
        timer._pause(id);
    }

    /** {@code WinTimer._resume(id)}: empty on Windows, and still empty after the migration. */
    public void resume() {
        timer._resume(id);
    }

    /** The id {@link #start} returned, or 0. */
    public long id() {
        return id;
    }

    /** {@code WinTimer.getMinPeriod_impl()}, the value cached in its static initializer. */
    public static int minPeriod() {
        return WinTimer.getMinPeriod_impl();
    }

    /** {@code WinTimer.getMaxPeriod_impl()}, the value cached in its static initializer. */
    public static int maxPeriod() {
        return WinTimer.getMaxPeriod_impl();
    }

    /**
     * Runs {@code WinApplication}'s static initializer. When the timer was JNI that initializer ended with
     * the {@code initIDs} native, which cached {@code javaIDs.Runnable.run} ({@code GlassApplication.cpp}).
     * <p>
     * The JNI timer callback dereferenced that method id ({@code Timer.cpp:62}), so a JNI timer
     * started in a JVM that never touched {@code WinApplication} called {@code CallVoidMethod} with a
     * null {@code jmethodID} and killed the VM with an access violation - which is what a headless
     * measurement of the old implementation ran into. The FFM timer needs none of it: its upcall
     * target is an ordinary Java method. Only the A/B measurement calls this, and only when
     * {@code WinTimer} still declares a native, which no build since the timer flip does; neither
     * {@code initIDs} nor that method id exists any more.
     */
    public static void initGlassApplicationIds() {
        try {
            Class.forName("com.sun.glass.ui.win.WinApplication", true, WinTimerShim.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("com.sun.glass.ui.win.WinApplication is not on this path", e);
        }
    }

    /**
     * The names of every method {@link WinTimer} still declares {@code native}. The migration is only
     * done when this is empty, so the test asserts it rather than trusting a grep.
     */
    public static List<String> nativeMethodsOfWinTimer() {
        return Arrays.stream(WinTimer.class.getDeclaredMethods())
                .filter(method -> Modifier.isNative(method.getModifiers()))
                .map(Method::getName)
                .sorted()
                .toList();
    }
}
