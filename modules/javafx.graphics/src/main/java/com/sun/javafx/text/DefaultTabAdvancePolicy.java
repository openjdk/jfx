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

package com.sun.javafx.text;

import java.util.List;
import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;
import javafx.scene.text.TextFlow;
import com.sun.javafx.scene.text.TabAdvancePolicy;

/**
 * This TabAdvancePolicy converts the high level {@code TabStopPolicy}
 * to {@code TabAdvancePolicy} that the text layout can use.
 */
public class DefaultTabAdvancePolicy implements TabAdvancePolicy {
    private float offset;
    private final float[] stops;
    private final float defaultStops;
    TextFlow flow;
    Region ref;

    private DefaultTabAdvancePolicy(TextFlow flow, Region ref, float offset, float[] tabs, float defaultStops) {
        // FIX
        this.flow = flow;
        this.ref = ref;
        //
        this.offset = offset;
        this.stops = tabs;
        this.defaultStops = defaultStops;
    }

    public static DefaultTabAdvancePolicy of(TextFlow flow, TabStopPolicy p) {
        Region ref = p.getReference();
        float offset = computeOffset(flow, ref);
        List<TabStop> tabs = p.tabStops();
        float[] stops = new float[tabs.size()];
        for (int i = 0; i < stops.length; i++) {
            TabStop stop = tabs.get(i);
            stops[i] = (float)stop.getPosition();
        }
        float defaultStops = (float)p.getDefaultStops();
        return new DefaultTabAdvancePolicy(flow, ref, offset, stops, defaultStops);
    }

    @Override
    public void reset() {
        // FIX may not need if created upon change!
        float off = computeOffset(flow, ref);
        if(offset != off) {
            // remove reset() if this never happens
            throw new Error(" *** reset. offset=" + offset + " off=" + off);
        }
        //System.out.println("reset. offset=" + offset);
    }

    @Override
    public float nextTabStop(float position) {
        position = (float)Math.max(0, position + offset);
        for (int i = 0; i < stops.length; i++) {
            double p = stops[i];
            if (position < p) {
                return (float)(p - offset);
            }
        }
        return FixedTabAdvancePolicy.nextPosition(position, defaultStops) - offset;
    }

    // this could be a method in the base class (change interface to an abstract class)
    private static float computeOffset(TextFlow flow, Region reference) {
        if (reference != null) {
            Point2D p0 = reference.localToScreen(0, 0);
            if (p0 != null) {
                Point2D p1 = flow.localToScreen(flow.snappedLeftInset(), 0);
                if (p1 != null) {
                    // TODO rtl
                    float v = (float)(p1.getX() - p0.getX());
                    if (!Float.isNaN(v)) {
                        return v;
                    }
                }
            }
        }
        return 0.0f;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("DefaultTabAdvancePolicy{");
        sb.append("offset=").append(offset);
        sb.append(", stops=[");
        for (int i = 0; i < stops.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(stops[i]);
        }
        sb.append("], defaultStops=").append(defaultStops);
        sb.append("}");
        return sb.toString();
    }
}
