/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

/**
 * Defines where to draw the stroke around the boundary of a Shape node.
 *
 * @see Shape
 * @since JavaFX 2.0
 */
public enum StrokeType {

    /**
     * The stroke is applied by extending the boundary of a closed {@link Shape}
     * node into its interior by a distance specified by the
     * {@link Shape#strokeWidthProperty strokeWidth}.
     * Since the definition of {@code INSIDE} depends on the filled interior
     * of the shape, this {@code StrokeType} is undefined for unclosed shapes.
     *
     * <p>
     * The image shows a shape without stroke and the same shape with a thick
     * inside stroke applied.
     * <p> <img src="doc-files/stroketype-inside.png" alt="A visual rendering
     * of StrokeType.INSIDE"> </p>
     */
    INSIDE,

    /**
     * The stroke is applied by extending the boundary of a closed {@link Shape}
     * node outside of its interior by a distance specified by the
     * {@link Shape#strokeWidthProperty strokeWidth}.
     * Since the definition of {@code OUTSIDE} depends on the filled interior
     * of the shape, this {@code StrokeType} is undefined for unclosed shapes.
     *
     * <p>
     * The image shows a shape without stroke and the same shape with a thick
     * outside stroke applied.
     * <p> <img src="doc-files/stroketype-outside.png" alt="A visual rendering
     * of StrokeType.OUTSIDE"> </p>
     */
    OUTSIDE,

    /**
     * The stroke is applied by thickening the boundary of the {@link Shape}
     * node by a distance of half of the {@link Shape#strokeWidthProperty strokeWidth}
     * on either side of the boundary.
     * Since the definition of {@code CENETERED} is symmetric and agnostic as
     * to the location of any interior of a shape, it can be used for either
     * closed or unclosed shapes.
     *
     * <p>
     * The image shows a shape without stroke and the same shape with a thick
     * centered stroke applied.
     * <p> <img src="doc-files/stroketype-centered.png" alt="A visual rendering
     * of StrokeType.CENTERED"> </p>
     */
    CENTERED
}
