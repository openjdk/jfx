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
import javafx.scene.layout.Region;

/**
 * The TabStopPolicy determines the tab stop positions within the text layout.
 *
 * @since 25
 */
public final class TabStopPolicy {
    private final ObservableList<TabStop> tabStops = FXCollections.observableArrayList();
    private final SimpleDoubleProperty defaultInterval = new SimpleDoubleProperty(0.0);

    /**
     * Constructs a new {@code TabStopPolicy} instance, with an empty list of stops.
     */
    public TabStopPolicy() {
    }

    /**
     * The list of tab stops.
     *
     * @return the non-null list of tab stops
     */
    public final ObservableList<TabStop> tabStops() {
        return tabStops;
    }

    /**
     * Specifies the default tab stop interval for tabs beyond the last stop provided
     * by {@link #tabStops()}.  This is a fixed repeating distance (in pixels) to the
     * next tab stop computed at regular intervals relative to the leading edge
     * of the {@code TextFlow} node.
     * <p>
     * A value of less than or equal 0 disables the default interval.
     *
     * @return the default tab interval property
     * @defaultValue 0
     */
    public final DoubleProperty defaultIntervalProperty() {
        return defaultInterval;
    }

    public final double getDefaultInterval() {
        return defaultInterval.get();
    }

    public final void setDefaultInterval(double value) {
        defaultInterval.set(value);
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof TabStopPolicy p) {
            return
                (getDefaultInterval() == p.getDefaultInterval()) &&
                tabStops().equals(p.tabStops());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = TabStopPolicy.class.hashCode();
        h = 31 * h + tabStops().hashCode();
        h = 31 * h + Double.hashCode(getDefaultInterval());
        return h;
    }
}
