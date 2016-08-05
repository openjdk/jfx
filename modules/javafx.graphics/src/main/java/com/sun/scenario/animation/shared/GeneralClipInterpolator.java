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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.value.WritableValue;

/**
 * General implementation of ClipInterpolator, which covers all use-cases.
 */

// @@OPT:
// - A binary search in interpolate might make sense.
// - Prepare only first segment when starting timeline and do the rest later =>
// improves startup time?
// - Store 1 / (rightMillis - leftMillis) for each interval and multiply
class GeneralClipInterpolator extends ClipInterpolator {

    private KeyFrame[] keyFrames;
    private long[] keyFrameTicks;

    // List of interpolation-points associated with each target
    private InterpolationInterval[][] interval = new InterpolationInterval[0][];

    // List of indexes for targets with undefined start value
    private int[] undefinedStartValues = new int[0];
    // Is internal representation up-to-date?
    private boolean invalid = true;

    GeneralClipInterpolator(KeyFrame[] keyFrames, long[] keyFrameTicks) {
        this.keyFrames = keyFrames;
        this.keyFrameTicks = keyFrameTicks;
    }

    // See comment in ClipInterpolator
    @Override
    ClipInterpolator setKeyFrames(KeyFrame[] keyFrames, long[] keyFrameTicks) {
        if (ClipInterpolator.getRealKeyFrameCount(keyFrames) == 2) {
            return ClipInterpolator.create(keyFrames, keyFrameTicks);
        }
        this.keyFrames = keyFrames;
        this.keyFrameTicks = keyFrameTicks;
        invalid = true;
        return this;
    }

    @Override
    void validate(boolean forceSync) {
        if (invalid) {
            final Map<WritableValue<?>, KeyValue> lastKeyValues = new HashMap<>();
            final int n = keyFrames.length;
            int index;
            for (index = 0; index < n; index++) {
                final KeyFrame keyFrame = keyFrames[index];
                if (keyFrameTicks[index] == 0) {
                    for (final KeyValue keyValue : keyFrame.getValues()) {
                        lastKeyValues.put(keyValue.getTarget(), keyValue);
                    }
                } else {
                    break;
                }
            }

            final Map<WritableValue<?>, List<InterpolationInterval>> map = new HashMap<>();
            final Set<WritableValue<?>> undefinedValues = new HashSet<>();
            // iterate through all keyFrames
            for (; index < n; index++) {
                final KeyFrame keyFrame = keyFrames[index];
                final long ticks = keyFrameTicks[index];
                // iterate through all keyValues in this keyFrame
                for (final KeyValue rightKeyValue : keyFrame.getValues()) {
                    final WritableValue<?> target = rightKeyValue.getTarget();
                    List<InterpolationInterval> list = map.get(target);
                    final KeyValue leftKeyValue = lastKeyValues.get(target);
                    if (list == null) {
                        // first encounter of a particular target, generate a
                        // new interval list
                        list = new ArrayList<>();
                        map.put(target, list);
                        if (leftKeyValue == null) {
                            list.add(InterpolationInterval.create(
                                    rightKeyValue, ticks));
                            undefinedValues.add(target);
                        } else {
                            list.add(InterpolationInterval
                                    .create(rightKeyValue, ticks,
                                            leftKeyValue, ticks));
                        }
                    } else {
                        assert leftKeyValue != null;
                        list.add(InterpolationInterval.create(rightKeyValue,
                                ticks, leftKeyValue,
                                ticks - list.get(list.size() - 1).ticks));
                    }
                    lastKeyValues.put(target, rightKeyValue);
                }
            }

            // copy everything to arrays
            final int targetCount = map.size();
            if (interval.length != targetCount) {
                interval = new InterpolationInterval[targetCount][];
            }
            final int undefinedStartValuesCount = undefinedValues.size();
            if (undefinedStartValues.length != undefinedStartValuesCount) {
                undefinedStartValues = new int[undefinedStartValuesCount];
            }
            int undefinedStartValuesIndex = 0;
            final Iterator<Map.Entry<WritableValue<?>, List<InterpolationInterval>>> iterator = map
                    .entrySet().iterator();
            for (int i = 0; i < targetCount; i++) {
                final Map.Entry<WritableValue<?>, List<InterpolationInterval>> entry = iterator
                        .next();
                interval[i] = new InterpolationInterval[entry.getValue().size()];
                entry.getValue().toArray(interval[i]);
                if (undefinedValues.contains(entry.getKey())) {
                    undefinedStartValues[undefinedStartValuesIndex++] = i;
                }
            }
            invalid = false;
        } else if (forceSync) {
            final int n = undefinedStartValues.length;
            for (int i = 0; i < n; i++) {
                final int index = undefinedStartValues[i];
                interval[index][0].recalculateStartValue();
            }
        }
    }

    @Override
    void interpolate(long ticks) {
        final int targetCount = interval.length;
        // iterate through all targets
        targetLoop: for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
            InterpolationInterval[] intervalList = interval[targetIndex];
            final int intervalCount = intervalList.length;
            // leftMillis keeps the timestamp of the left side of the interval
            long leftTicks = 0;
            // iterate through all intervals except the last one
            for (int intervalIndex = 0; intervalIndex < intervalCount - 1; intervalIndex++) {
                final InterpolationInterval i = intervalList[intervalIndex];
                final long rightTicks = i.ticks;
                if (ticks <= rightTicks) {
                    // we found the current interval
                    final double frac = (double)(ticks - leftTicks)
                            / (rightTicks - leftTicks);
                    i.interpolate(frac);
                    continue targetLoop;
                }
                leftTicks = rightTicks;
            }
            // we did not find a current interval, use the last one
            final InterpolationInterval i = intervalList[intervalCount - 1];
            // the last interval may end before the timeline ends, make sure we
            // set the end value
            final double frac = Math.min(1.0, (double)(ticks - leftTicks)
                    / (i.ticks - leftTicks));
            i.interpolate(frac);
        }
    }
}
