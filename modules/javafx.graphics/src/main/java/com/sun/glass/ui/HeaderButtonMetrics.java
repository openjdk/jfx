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

package com.sun.glass.ui;

import javafx.geometry.Dimension2D;
import javafx.stage.StageStyle;
import java.util.Objects;

/**
 * Provides metrics about the header buttons of {@link StageStyle#EXTENDED} windows.
 *
 * @param leftInset the size of the left inset
 * @param rightInset the size of the right inset
 * @param minHeight the minimum height of the window buttons
 * @see HeaderButtonOverlay
 */
public record HeaderButtonMetrics(Dimension2D leftInset, Dimension2D rightInset, double minHeight) {

    public static HeaderButtonMetrics EMPTY = new HeaderButtonMetrics(new Dimension2D(0, 0), new Dimension2D(0, 0), 0);

    public HeaderButtonMetrics {
        Objects.requireNonNull(leftInset);
        Objects.requireNonNull(rightInset);

        if (minHeight < 0) {
            throw new IllegalArgumentException("minHeight cannot be negative");
        }
    }

    public double totalInsetWidth() {
        return leftInset.getWidth() + rightInset.getWidth();
    }

    public double maxInsetHeight() {
        return Math.max(leftInset.getHeight(), rightInset.getHeight());
    }
}
