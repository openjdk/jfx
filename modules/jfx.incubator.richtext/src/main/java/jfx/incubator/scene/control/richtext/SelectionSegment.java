/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext;

import java.util.Objects;

/**
 * Text selection segment, comprised of the selection anchor and the caret positions.
 * The main purpose of this class is to enable tracking of selection changes as a single entity.
 *
 * @since 24
 */
public final class SelectionSegment {
    private final TextPos min;
    private final TextPos max;
    private final boolean caretAtMin;

    /**
     * Constructs the selection segment.
     *
     * @param anchor the anchor position, must not be null
     * @param caret the caret position, must not be null
     */
    public SelectionSegment(TextPos anchor, TextPos caret) {
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

    /**
     * Returns the selection anchor position.
     * @return the anchor position
     */
    public final TextPos getAnchor() {
        return caretAtMin ? max : min;
    }

    /**
     * Returns the caret position.
     * @return the caret position
     */
    public final TextPos getCaret() {
        return caretAtMin ? min : max;
    }

    /**
     * Returns the position which is closer to the start of the document.
     * @return the text position
     */
    public final TextPos getMin() {
        return min;
    }

    /**
     * Returns the position which is closer to the end of the document.
     * @return the text position
     */
    public TextPos getMax() {
        return max;
    }

    /**
     * Returns true if the anchor and the caret are at the same position.
     * @return true if the anchor and the caret are at the same position
     */
    public boolean isCollapsed() {
        return min.equals(max);
    }

    @Override
    public String toString() {
        return "SelectionSegment{" + min + ", " + max + ", caretAtMin=" + caretAtMin + "}";
    }

    private static <T extends Comparable<T>> void isLessThanOrEqual(T min, T max, String nameMin, String nameMax) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(nameMin + " must be less or equal to " + nameMax);
        }
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof SelectionSegment s) {
            return
                (caretAtMin == s.caretAtMin) &&
                (min.equals(s.min)) &&
                (max.equals(s.max));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = SelectionSegment.class.hashCode();
        h = 31 * h + Boolean.hashCode(caretAtMin);
        h = 31 * h + min.hashCode();
        h = 31 * h + max.hashCode();
        return h;
    }
}
