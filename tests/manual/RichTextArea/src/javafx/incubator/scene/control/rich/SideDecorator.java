/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.incubator.scene.control.rich;

import javafx.scene.Node;

/**
 * Provides a way to add side decorations to each paragraph
 * in a {@link RichTextArea}.
 * <p>
 * The side decorations Nodes are added to either left or right side, of each paragraph.  Each side node will be
 * resized to the height of the corresponding paragraph.  The width, in order to avoid complicated
 * layout process, would be determined by the following process:
 * <ul>
 * <li>if {@link #getPrefWidth} method returns a positive value, that will be the width of all the side nodes.
 * <li>otherwise, the {@link #getNode(int, boolean)} method is called with {@code forMeasurement} argument set to true.
 * Preferred width of this Node will be used to size all other Nodes.
 * </ul>
 */
public interface SideDecorator {
    /**
     * Returns the width for all the side Nodes, or 0 if a measurer Node needs to be obtained via
     * {@link #getNode(int, boolean)}.
     * @param viewWidth width of the view
     * @return preferred width
     */
    public double getPrefWidth(double viewWidth);

    /**
     * Creates a Node to be added to the layout to the right or to the left of the given paragraph.
     * <p>
     * When {@code forMeasurement} is true, this method is expected to create a special non-null
     * measurement Node, whose preferred width will be used to size all the side Nodes (and must, therefore,
     * to be wider than any side node in the view).  The {@code modelIndex} is this case is the index of
     * the first paragraph in the view.
     * <p>
     * The measurement node will not be displayed.
     *
     * @param modelIndex model index
     * @param forMeasurement when true, specifies that a measurement Node must be created
     * @return new instance of the Node, or null
     */
    public Node getNode(int modelIndex, boolean forMeasurement);
}
