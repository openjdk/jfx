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

package javafx.scene.text;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * TabStopPolicy determines the tab stop positions within the text layout.
 *
 * @since 999 TODO
 */
public class TabStopPolicy {
    private final ObservableList<TabStop> tabStops = FXCollections.observableArrayList();
    private final ObservableList<TabStop> tabStopsRO = FXCollections.unmodifiableObservableList(tabStops);
    private final SimpleDoubleProperty defaultStops = new SimpleDoubleProperty(100); // FIX use 8x of space char advance of the default font
    private static final double EPS = 0.01;

    /**
     * Constructs a new {@code TabStopPolicy} instance.
     */
    public TabStopPolicy() {
    }

    /**
     * Specifies the unmodifiable list of tab stops, sorted by position from smallest to largest.
     * The list can be changed using
     * {@link #addTabStop(double)},
     * {@link #clearTabStops()}, or
     * {@link #removeTabStop(TabStop)}.
     *
     * @return the non-null, unmodifiable list of tab stops, sorted by position
     */
    public final ObservableList<TabStop> tabStops() {
        return tabStopsRO;
    }

    /**
     * Adds a new tab stop at the specified position.
     * This method does nothing if the position coincides with an already existing tab stop.
     *
     * @param position the tab stop position
     */
    public final void addTabStop(double position) {
        for (int i = 0; i < tabStops.size(); i++) {
            TabStop t = tabStops.get(i);
            double p = t.getPosition();
            if (Math.abs(position - p) < EPS) {
                return;
            } else if (p > position) {
                tabStops.add(i, new TabStop(position));
                return;
            }
        }
        tabStops.add(new TabStop(position));
    }

    /**
     * Removes the specified tab stop.
     *
     * @param stop the tab stop to remove
     */
    public final void removeTabStop(TabStop stop) {
        tabStops.remove(stop);
    }

    /**
     * Removes all tab stops.
     */
    public final void clearTabStops() {
        tabStops.clear();
    }

    /**
     * Provides default tab stops (beyond the last tab stop specified by {@code #tabStops()}, as a distance
     * in points from the last tab stop position.
     *
     * TODO
     * It is unclear how to specify NONE value (negative perhaps?).  MS Word does not allow for NONE, but allows 0.
     *
     * @return the default tab stops property, in pixels.
     * @defaultValue TODO
     */
    public final DoubleProperty defaultStops() {
        return defaultStops;
    }

    public final double getDefaultStops() {
        return defaultStops.get();
    }

    public final void setDefaultStops(double value) {
        defaultStops.set(value);
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof TabStopPolicy p) {
            return
                (getDefaultStops() == p.getDefaultStops()) &&
                tabStops().equals(p.tabStops());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = TabStopPolicy.class.hashCode();
        h = 31 * h + tabStops().hashCode();
        h = 31 * h + Double.hashCode(getDefaultStops());
        return h;
    }
}
