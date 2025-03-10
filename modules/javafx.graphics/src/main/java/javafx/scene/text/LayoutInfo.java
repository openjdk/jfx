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

import java.util.List;
import javafx.geometry.Rectangle2D;

/**
 * Provides a view into the text layout used in a {@code Text} or a {@code TextFlow} node,
 * with purpose of querying the details of the layout such as break up of the text into lines,
 * as well as geometry of other shapes derived from the layout (selection, underline, etc.).
 * <p>
 * The information obtained via this object may change after the next layout cycle, which may come as a result
 * of actions such as resizing of the container, or modification of certain properties.
 * For example updating the text or the font might change the layout, but a change of color would not.
 *
 * @since 25
 */
public abstract class LayoutInfo {
    /**
     * Constructor for subclasses to call.
     */
    protected LayoutInfo() {
    }

    /**
     * Returns the logical bounds of the layout.
     * Depending on {@code includeLineSpacing}, the return value may include the line spacing after the
     * last line of text.
     *
     * @param includeLineSpacing determines whether the line spacing after last text line should be included
     * @return the layout bounds
     */
    public abstract Rectangle2D getBounds(boolean includeLineSpacing);

    /**
     * Returns the number of text lines in the layout.
     * @return the number of text lines
     */
    public abstract int getTextLineCount();

    /**
     * Returns the immutable list of text lines in the layout.
     *
     * @param includeLineSpacing determines whether the result includes the line spacing
     * @return the immutable list of {@code TextLineInfo} objects
     */
    public abstract List<TextLineInfo> getTextLines(boolean includeLineSpacing);

    /**
     * Returns the {@code TextLineInfo} object which contains information about
     * the text line at index {@code index}.
     *
     * @param index the line index
     * @param includeLineSpacing determines whether the result includes the line spacing
     * @return the {@code TextLineInfo} object
     * @throws IndexOutOfBoundsException if the index is out of range
     *     {@code (index < 0 || index > getTextLineCount())}
     */
    public abstract TextLineInfo getTextLine(int index, boolean includeLineSpacing);

    /**
     * Returns the geometry of the text selection, as an immutable list of {@code Rectangle2D} objects,
     * for the given start and end offsets.
     *
     * @param start the start offset
     * @param end the end offset
     * @param includeLineSpacing determines whether the result includes the line spacing
     * @return the immutable list of {@code Rectangle2D} objects
     */
    public abstract List<Rectangle2D> selectionShape(int start, int end, boolean includeLineSpacing);

    /**
     * Returns the geometry of the strike-through shape, as an immutable list of {@code Rectangle2D} objects,
     * for the given start and end offsets.
     *
     * @param start the start offset
     * @param end the end offset
     * @return the immutable list of {@code Rectangle2D} objects
     */
    public abstract List<Rectangle2D> strikeThroughShape(int start, int end);

    /**
     * Returns the geometry of the underline shape, as an immutable list of {@code Rectangle2D} objects,
     * for the given start and end offsets.
     *
     * @param start the start offset
     * @param end the end offset
     * @return the immutable list of {@code Rectangle2D} objects
     */
    public abstract List<Rectangle2D> underlineShape(int start, int end);

    /**
     * Returns the information related to the caret at the specified character index and the character bias.
     *
     * @param charIndex the character index
     * @param leading whether the caret is biased on the leading edge of the character
     * @return the {@code CaretInfo} object
     */
    public abstract CaretInfo caretInfo(int charIndex, boolean leading);
}
