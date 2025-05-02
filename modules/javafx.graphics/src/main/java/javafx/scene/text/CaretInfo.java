/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Rectangle2D;
import com.sun.javafx.text.PrismCaretInfo;

/**
 * Provides the information associated with the caret.
 * <p>
 * Typically, the caret is represented by a single vertical line which visually indicates the
 * position within the text.  In some cases, where the caret is positioned between left-to-right and
 * right-to-left text, two line segments will be shown, indicating the insertion position for both left-to-right
 * and right-to-left character.
 *
 * @since 25
 */
public sealed abstract class CaretInfo permits PrismCaretInfo {
    /**
     * Constructor for subclasses to call.
     */
    protected CaretInfo() {
    }

    /**
     * Returns the number of segments representing the caret.
     *
     * @return the number of segments representing the caret
     */
    public abstract int getSegmentCount();

    /**
     * Returns the geometry of the segment at the specified index.
     *
     * @param index the line index
     * @return the bounds of the caret segment
     * @throws IndexOutOfBoundsException if the index is out of range
     *     {@code (index < 0 || index >= getSegmentCount())}
     */
    public abstract Rectangle2D getSegmentAt(int index);
}
