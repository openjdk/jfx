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

import java.util.Arrays;
import java.util.List;
import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;
import javafx.scene.text.TextFlow;
import com.sun.javafx.scene.text.TabAdvancePolicy;

/**
 * This TabAdvancePolicy converts the high level {@code TabStopPolicy}
 * to {@code TabAdvancePolicy} that the text layout can use.
 */
public class DefaultTabAdvancePolicy implements TabAdvancePolicy {
    private final TextFlow flow;
    private final float[] stops;
    private final float interval;

    private DefaultTabAdvancePolicy(TextFlow flow, float[] tabs, float interval) {
        this.flow = flow;
        this.stops = tabs;
        this.interval = interval;
    }

    public static DefaultTabAdvancePolicy of(TextFlow flow, TabStopPolicy p) {
        List<TabStop> tabs = p.tabStops();
        float[] stops = new float[tabs.size()];
        for (int i = 0; i < stops.length; i++) {
            TabStop stop = tabs.get(i);
            stops[i] = (float)stop.getPosition();
        }
        Arrays.sort(stops);
        float interval = (float)p.getDefaultInterval();
        return new DefaultTabAdvancePolicy(flow, stops, interval);
    }

    @Override
    public float nextTabStop(float offset, float position) {
        for (int i = 0; i < stops.length; i++) {
            double p = stops[i] + offset;
            if (position < p) {
                return (float)(p);
            }
        }
        if (interval <= 0.0f) {
            return -1.0f;
        }
        return FixedTabAdvancePolicy.nextPosition(position - offset, interval) + offset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("DefaultTabAdvancePolicy{");
        sb.append(", stops=[");
        for (int i = 0; i < stops.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(stops[i]);
        }
        sb.append("], interval=").append(interval);
        sb.append("}");
        return sb.toString();
    }
}
