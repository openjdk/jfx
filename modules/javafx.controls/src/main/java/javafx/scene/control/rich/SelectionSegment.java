/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package javafx.scene.control.rich;

import java.util.Objects;
import javafx.scene.control.rich.util.Util;

/**
 * Text selection segment.
 */
public class SelectionSegment implements Cloneable {
    private final Marker min;
    private final Marker max;
    private final boolean caretAtMin;

    public SelectionSegment(Marker min, Marker max, boolean caretAtMin) {
        Objects.requireNonNull(min, "min cannot be null");
        Objects.requireNonNull(max, "max cannot be null");
        isLessThanOrEqual(min, max, "min", "max");

        this.min = min;
        this.max = max;
        this.caretAtMin = caretAtMin;
    }

    public SelectionSegment(Marker anchor, Marker caret) {
        Objects.requireNonNull(anchor, "anchor cannot be null");
        Objects.requireNonNull(caret, "caret cannot be null");

        if (anchor.compareTo(caret) <= 0) {
            this.min = anchor;
            this.max = caret;
            this.caretAtMin = false;
        } else {
            this.min = caret;
            this.max = anchor;
            this.caretAtMin = true;
        }
    }

    public Marker getAnchor() {
        return caretAtMin ? max : min;
    }

    public Marker getCaret() {
        return caretAtMin ? min : max;
    }

    public Marker getMin() {
        return min;
    }

    public Marker getMax() {
        return max;
    }

    public String toString() {
        return "SelectionSegment{" + min + ", " + max + ", caretAtMin=" + caretAtMin + "}";
    }

    private static <T extends Comparable<T>> void isLessThanOrEqual(T min, T max, String nameMin, String nameMax) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(nameMin + " must be less or equal to " + nameMax);
        }
    }
}
