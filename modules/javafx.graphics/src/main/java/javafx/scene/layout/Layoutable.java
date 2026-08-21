/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import javafx.geometry.Bounds;

/**
 * Interface for elements that can be resized and relocated.
 * <p>
 * Extends {@link Measurable} with the ability to report whether an element
 * supports being resized, to expose the bounds that should be used for
 * layout calculations, and to resize and relocate the element to specific
 * dimensions and coordinates.
 *
 * <h2>Resizability</h2>
 * Whether an element supports being resized is reported by
 * {@link #isResizable()}. Calling {@link #resize(double, double)} on a
 * resizable element sets its layout bounds to the given size, ideally
 * within the range established by {@link Measurable}'s sizing methods.
 * Calling it on a non-resizable element has no effect; such an element
 * establishes its own size some other way, but can still be relocated.
 *
 * <h2>Layout Bounds</h2>
 * {@link #getLayoutBounds()} returns the rectangular bounds that should be
 * used for layout calculations, which may differ from an element's visual
 * bounds.
 *
 * <h2>Resizing and Relocation</h2>
 * {@link #resize(double, double)}, {@link #relocate(double, double)}, and
 * {@link #resizeRelocate(double, double, double, double)} resize and/or
 * reposition an element to specific dimensions and coordinates.
 *
 * @see Measurable
 * @since 28
 */
public interface Layoutable extends Measurable {

    /**
     * Indicates whether this element supports being resized.
     * If this method returns true, calling {@link #resize(double, double)}
     * sets this element's layout bounds to the given width and height
     * (ideally within its size range).
     * <p>
     * If this method returns false, {@link #resize(double, double)} is a
     * no-op, and this element's layoutBounds should be used as its minimum,
     * preferred, and maximum sizes.  Non-resizable elements depend on the
     * application to establish their sizing by setting appropriate properties
     * (like setting width/height for a rectangle, or setting a text).  A
     * non-resizable element may still be relocated during layout.
     *
     * @see #resize(double, double)
     * @see #getLayoutBounds()
     * @return whether or not this element supports being resized
     */
    boolean isResizable();

    /**
     * The rectangular bounds that should be used for layout calculations for
     * this element. {@code layoutBounds} may differ from the visual bounds
     * of the element and is computed differently depending on the element type.
     * <p>
     * If the element type is resizable then the layoutBounds will always be {@code 0,0 width x height}.
     * If the element type is not resizable then the {@code layoutBounds}
     * are computed based on the element's geometric properties.
     * <p>
     * Because the computation of layoutBounds is often tied to an element's
     * geometric variables, it is an error to bind any such variables to an
     * expression that depends upon {@code layoutBounds}. For example, the
     * x or y variables of a shape should never be bound to {@code layoutBounds}
     * for the purpose of positioning the element.
     * <p>
     * Note that for 3D shapes, the layout bounds is actually a rectangular box
     * with X, Y, and Z values, although only X and Y are used in layout calculations.
     * <p>
     * The {@code layoutBounds} will never be null.
     *
     * @return the current layout bounds, never {@code null}
     */
    Bounds getLayoutBounds();

    /**
     * If this element is resizable, sets its layout bounds to the specified
     * width and height.  If this element is not resizable, the resize step
     * is skipped.
     * <p>
     * Once the element has been resized (if resizable) then relocates it to
     * x,y.
     * <p>
     * Callers are responsible for ensuring the width and height values fall
     * within this element's preferred range.
     *
     * @see #isResizable()
     * @see #resize(double, double)
     * @see #relocate(double, double)
     * @param x the target x coordinate location
     * @param y the target y coordinate location
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     */
    default void resizeRelocate(double x, double y, double width, double height) {
        resize(width, height);
        relocate(x,y);
    }

    /**
     * If this element is resizable, sets its layout bounds to the specified
     * width and height.  If this element is not resizable, this method is a
     * no-op.
     * <p>
     * Callers are responsible for ensuring the width and height values fall
     * within this element's preferred range.
     *
     * @see #isResizable()
     * @see #minWidth(double)
     * @see #minHeight(double)
     * @see #maxWidth(double)
     * @see #maxHeight(double)
     * @see #getLayoutBounds()
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     */
    void resize(double width, double height);

    /**
     * Relocates this element to the given x,y location.
     * <p>
     * This method only affects this element's position; its size (as
     * reported by {@link #getLayoutBounds()}) is unaffected.
     *
     * @see #resizeRelocate(double, double, double, double)
     * @param x the target x coordinate location
     * @param y the target y coordinate location
     */
    void relocate(double x, double y);
}
