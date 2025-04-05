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

import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;

public class SimpleTabAdvancePolicy implements TabAdvancePolicy {
    private final double[] tabs;
    private final double defaultStops;

    private SimpleTabAdvancePolicy(double[] tabs, double defaultStops) {
        this.tabs = tabs;
        this.defaultStops = defaultStops;
    }

    public static SimpleTabAdvancePolicy of(TabStopPolicy p) {
        double[] tabs = p.tabStops().stream()
            .map(TabStop::getPosition)
            .mapToDouble(Double::doubleValue)
            .toArray();
        double defaultStops = p.getDefaultStops();
        return new SimpleTabAdvancePolicy(tabs, defaultStops);
    }

    @Override
    public float nextTabStop(float position) {
        for (int i = 0; i < tabs.length; i++) {
            double p = tabs[i];
            if (position < p) {
                return (float)p;
            }
        }
        return (float)(((int)(position / defaultStops) + 1) * defaultStops);
    }
}
