/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.animation.shared;

import javafx.animation.KeyFrame;
import javafx.util.Duration;

public abstract class ClipInterpolator {

    static ClipInterpolator create(KeyFrame[] keyFrames, long[] keyFrameTicks) {
        return (ClipInterpolator.getRealKeyFrameCount(keyFrames) == 2) ? (keyFrames.length == 1) ? new SimpleClipInterpolator(
                keyFrames[0], keyFrameTicks[0]) : new SimpleClipInterpolator(keyFrames[0],
                keyFrames[1], keyFrameTicks[1])
                : new GeneralClipInterpolator(keyFrames, keyFrameTicks);
    }

    /**
     * Figures out the number of "real" key frames. The user may not have specified the "zero" key
     * frame, in which case we end up inferring an additional zero key frame on the array.
     *
     * @param keyFrames The key frames. Must not be null.
     * @return The "real" number of key frames
     */
    static int getRealKeyFrameCount(KeyFrame[] keyFrames) {
        final int length = keyFrames.length;
        return (length == 0) ? 0 : (keyFrames[0].getTime()
                .greaterThan(Duration.ZERO)) ? length + 1 : length;
    }

    /**
     * Changes the keyframes.
     *
     * The optimal implementation for the new keyFrames might be different. For
     * this reason, setKeyFrames returns a ClipInterpolator implementation with
     * the updated values. This can either be the same object or a newly created
     * one.
     *
     * @param keyFrames
     *            The new sorted array of keyframes of this clip
     * @param keyFrameTicks
     *            tick duration of corresponding keyFrames
     * @return The ClipInterpolator implementation to use after changing the
     *         keyframes.
     */
    abstract ClipInterpolator setKeyFrames(KeyFrame[] keyFrames, long[] keyFrameTicks);

    abstract void interpolate(long ticks);

    abstract void validate(boolean forceSync);
}
