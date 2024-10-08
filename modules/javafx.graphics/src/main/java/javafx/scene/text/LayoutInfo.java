/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Represents an immutable snapshot of certain aspects of the text layout.
 *
 * @since 24
 */
public interface LayoutInfo {
    /**
     * Returns the logical bounds of the layout:
     * <ul>
     * <li>{@code minX} is always zero
     * <li>{@code minY} is the ascent of the first line (negative)
     * <li>{@code width} the width of the widest line
     * <li>{@code height} the sum of all lines height
     * </ul>
     *
     * @return the layout bounds
     */
    public Rectangle2D getBounds();

    /**
     * Returns the number of text lines in the layout.
     * @return the number of text lines
     */
    public int getTextLineCount();

    /**
     * Returns the start offset for the line at index {@code index}.
     *
     * @param index the line index
     * @return the start offset
     */
    public int getTextLineStart(int index);

    /**
     * Returns the end offset for the line at index {@code index}.
     *
     * @param index the line index
     * @return the end offset
     */
    public int getTextLineEnd(int index);

    /**
     * Returns the information about the line:
     * <ul>
     * <li>
     * {@code minX} - the x origin of the line (relative to the layout).
     * The x origin is defined by TextAlignment of the text layout, always zero
     * for left-aligned text.
     * <li>
     * {@code minY} - the ascent of the line (negative).
     * The ascent of the line is the max ascent of all fonts in the line.
     * <li>
     * {@code width} - the width of the line.
     * The width for the line is sum of all the run widths in the line, it is not
     * affect by the wrapping width but it will include any changes caused by
     * justification.
     * <li>
     * {@code height} - the height of the line.
     * The height of the line is sum of the max ascent, max descent, and
     * max line gap of all the fonts in the line.
     * </ul>
     *
     * @param index the line index
     * @return the line bounds
     */
    public Rectangle2D getLineBounds(int index);
}
