/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;
import javafx.scene.text.TextFlow;

/**
 * This TabAdvancePolicy converts the high level {@code TabStopPolicy}
 * to {@code TabAdvancePolicy} that the text layout can use.
 */
public class DefaultTabAdvancePolicy implements TabAdvancePolicy {
    private final TextFlow flow;
    private final Region reference;
    private final double[] tabs;
    private final double defaultStops;
    private float offset;

    private DefaultTabAdvancePolicy(Region ref, TextFlow flow, double[] tabs, double defaultStops) {
        this.flow = flow;
        this.reference = ref;
        this.tabs = tabs;
        this.defaultStops = defaultStops;
    }

    public static DefaultTabAdvancePolicy of(TextFlow flow, TabStopPolicy p) {
        Region ref = p.getReference();
        double[] tabs = p.tabStops().stream()
            .map(TabStop::getPosition)
            .mapToDouble(Double::doubleValue)
            .toArray();
        double defaultStops = p.getDefaultStops();
        return new DefaultTabAdvancePolicy(ref, flow, tabs, defaultStops);
    }

    @Override
    public void reset() {
        offset = computeOffset();
        System.out.println("offset=" + offset); // FIX
    }

    @Override
    public float nextTabStop(float position) {
        float f = nextTabStop2(position);
        System.out.println("pos=" + position + " next=" + f); // FIX
        return f;
    }
    float nextTabStop2(float position) {
        position = Math.max(0, position + offset);
        for (int i = 0; i < tabs.length; i++) {
            double p = tabs[i];
            if (position < p) {
                return (float)p - offset;
            }
        }
        return (float)(((int)(position / defaultStops) + 1) * defaultStops) - offset;
    }

    // this could be a method in the base class (change interface to an abstract class)
    private float computeOffset() {
        if (reference != null) {
            Point2D p0 = reference.localToScreen(0, 0);
            Point2D p1 = flow.localToScreen(flow.snappedLeftInset(), 0);
            // TODO rtl
            float v = (float)(p1.getX() - p0.getX());
            if (!Float.isNaN(v)) {
                return v;
            }
        }
        return 0.0f;
    }
}
