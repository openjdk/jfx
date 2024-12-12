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

package jfx.incubator.scene.control.richtext.model;

import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Contains information about {@link StyledTextModel} content change.
 * <p>
 * This class represents two kinds of changes made to the model: one that modifies the textual content
 * and embedded Nodes, and one that only changes styling of the content.
 * <p>
 * A content change can be though of as a replacement of content between the two positions
 * {@code start} and {@code end} with a different styled content (or no content).
 * The change object does not include the actual content, but only the information about inserted symbols:
 * <ul>
 * <li>the number of characters inserted to the first affected paragraph
 * <li>the number of complete paragraphs inserted
 * <li>the number of characters inserted into the first paragraph following the inserted lines
 * </ul>
 * The following diagram illustrates the inserted content relative to the start position (S) and
 * the end position (E):
 * <pre>
 * .......STTTTTTT
 * AAAAAAAAAAAAAAA
 * AAAAAAAAAAAAAAA
 * BBBE...........
 * </pre>
 * Where T are the characters inserted following the start position, A are the inserted complete paragraphs,
 * and B are the characters inserted at the beginning of the paragraph that contains the end position.
 *
 * @since 24
 */
public abstract class ContentChange {
    private final TextPos start;
    private final TextPos end;

    private ContentChange(TextPos start, TextPos end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the start position.
     * @return the start position
     */
    public TextPos getStart() {
        return start;
    }

    /**
     * Returns the end position.
     * @return the end position
     */
    public TextPos getEnd() {
        return end;
    }

    /**
     * The number of characters added at the end of the paragraph which contains the start position.
     * @return the number of characters inserted
     */
    public int getCharsAddedTop() {
        return 0;
    }

    /**
     * The number of whole paragraphs inserted.
     * @return the number of paragraphs
     */
    public int getLinesAdded() {
        return 0;
    }

    /**
     * The number of characters added at the beginning of the existing paragraph which contains the end position.
     * @return the number of characters
     */
    public int getCharsAddedBottom() {
        return 0;
    }

    /**
     * Determines whether the change is an edit ({@code true}) or affects styling only ({@code false}).
     * @return true if change is an edit
     */
    public boolean isEdit() {
        return true;
    }

    /**
     * Creates the content change event which represents an edit.
     *
     * @param start the start position
     * @param end the end position
     * @param charsAddedTop the number of characters appended to the paragraph containing the start position
     * @param linesAdded the number of full paragraphs inserted
     * @param charsAddedBottom the number of characters inserted at the beginning of the paragraph containing the end position
     * @return the change instance
     */
    public static ContentChange ofEdit(TextPos start, TextPos end, int charsAddedTop, int linesAdded, int charsAddedBottom) {
        return new ContentChange(start, end) {
            @Override
            public int getCharsAddedTop() {
                return charsAddedTop;
            }

            @Override
            public int getLinesAdded() {
                return linesAdded;
            }

            @Override
            public int getCharsAddedBottom() {
                return charsAddedBottom;
            }
        };
    }

    /**
     * Creates the content change event which represents a styling update.
     *
     * @param start the start position
     * @param end the end position
     * @return the change instance
     */
    public static ContentChange ofStyleChange(TextPos start, TextPos end) {
        return new ContentChange(start, end) {
            @Override
            public boolean isEdit() {
                return false;
            }
        };
    }
}
