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

package com.sun.webkit.graphics;

import com.sun.webkit.WebKitNative;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_media_notify_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@link WCMediaPlayer} to report player state back to {@code MediaPlayerPrivate}.
 * <p>
 * The player handle is the {@code WebCore::MediaPlayerPrivate*} that
 * {@code WKJHostMedia::create_player} was given, which {@link WCMediaPlayer} holds as {@code nPtr}
 * and reads only on the event thread. Every one of these can run script - {@code MediaPlayer}
 * forwards to the media element's event loop - so none is bound with
 * {@code Linker.Option.critical(true)}, and the array in {@link #notifyBufferChanged} is copied
 * through a confined arena rather than pinned.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class WCMediaPlayerNative {

    private static final MethodHandle NETWORK_STATE = WebKitNative.downcall(
            "wkj_media_notify_network_state",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle READY_STATE = WebKitNative.downcall(
            "wkj_media_notify_ready_state",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAUSED = WebKitNative.downcall(
            "wkj_media_notify_paused",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle SEEKING = WebKitNative.downcall(
            "wkj_media_notify_seeking",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT));
    private static final MethodHandle FINISHED = WebKitNative.downcall(
            "wkj_media_notify_finished",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle READY = WebKitNative.downcall(
            "wkj_media_notify_ready",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_FLOAT));
    private static final MethodHandle DURATION_CHANGED = WebKitNative.downcall(
            "wkj_media_notify_duration_changed",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT));
    private static final MethodHandle SIZE_CHANGED = WebKitNative.downcall(
            "wkj_media_notify_size_changed",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT));
    private static final MethodHandle NEW_FRAME = WebKitNative.downcall(
            "wkj_media_notify_new_frame",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle BUFFER_CHANGED = WebKitNative.downcall(
            "wkj_media_notify_buffer_changed",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));

    private WCMediaPlayerNative() {
    }

    static void notifyNetworkStateChanged(long player, int networkState) {
        try {
            NETWORK_STATE.invokeExact(player, networkState);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifyReadyStateChanged(long player, int readyState) {
        try {
            READY_STATE.invokeExact(player, readyState);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifyPaused(long player, boolean paused) {
        try {
            PAUSED.invokeExact(player, paused ? 1 : 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Reports a seek. The library accepts and ignores {@code readyState}, exactly as the JNI
     * function did - it named the parameter in a comment and never read it - so dropping it would be
     * a Java side change rather than an ABI simplification.
     *
     * @param player the player handle
     * @param seeking whether a seek is in progress
     * @param readyState the ready state, accepted and ignored
     */
    static void notifySeeking(long player, boolean seeking, int readyState) {
        try {
            SEEKING.invokeExact(player, seeking ? 1 : 0, readyState);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifyFinished(long player) {
        try {
            FINISHED.invokeExact(player);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifyReady(long player, boolean hasVideo, boolean hasAudio, float duration) {
        try {
            READY.invokeExact(player, hasVideo ? 1 : 0, hasAudio ? 1 : 0, duration);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifyDurationChanged(long player, float duration) {
        try {
            DURATION_CHANGED.invokeExact(player, duration);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifySizeChanged(long player, int width, int height) {
        try {
            SIZE_CHANGED.invokeExact(player, width, height);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void notifyNewFrame(long player) {
        try {
            NEW_FRAME.invokeExact(player);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Reports the buffered time ranges. {@code count} is the number of floats, not the number of
     * pairs, which is what {@code GetArrayLength} gave the JNI form.
     *
     * @param player the player handle
     * @param ranges pairs of start and end times, may be {@code null}
     * @param bytesLoaded the number of bytes loaded so far
     */
    static void notifyBufferChanged(long player, float[] ranges, int bytesLoaded) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment values = MemorySegment.NULL;
            int count = 0;
            if (ranges != null) {
                count = ranges.length;
                values = arena.allocateFrom(JAVA_FLOAT, ranges);
            }
            try {
                BUFFER_CHANGED.invokeExact(player, values, count, bytesLoaded);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }
}
