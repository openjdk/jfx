/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.skin;

import javafx.scene.Node;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * This interface provides a mechanism for the Skin to support custom {@code StyleAttribute}s.
 * During the layout pass, the cell context is passed to the Skin so the latter can add inline style(s)
 * to either the paragraph Node (typically {@code TextFlow}) or the text segment Node ({@code Text}).
 *
 * @see RichTextAreaSkin#applyStyles(CellContext, StyleAttributeMap, boolean)
 * @since 24
 */
public interface CellContext {
    /**
     * Adds an inline style.
     * <p>
     * The inline style must be a valid CSS style string, for example {@code "-fx-font-size:15px;"}.
     * This string might contain multiple CSS properties.
     *
     * @param fxStyle the inline style string
     */
    public void addStyle(String fxStyle);

    /**
     * Returns the node being styled.
     * <p>
     * This might be a TextFlow (for the paragraph cell context) or Text (for the text segment cell context).
     * @return the node being styled.
     */
    public Node getNode();

    /**
     * Returns the current attributes.
     * @return the current attributes.
     */
    public StyleAttributeMap getAttributes();
}
