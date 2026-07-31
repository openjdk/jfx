/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Orientation;

/**
 * Interface providing measurement metrics for layout calculations.
 * <p>
 * Implemented by elements that provide intrinsic or calculated size constraints,
 * baseline alignment, and content bias.
 *
 * <h2>Sizing Precedence</h2>
 * Layout algorithms should compute dimensions using the bounds returned by this interface.
 * When bounds conflict, the following precedence rules apply:
 * <ul>
 *   <li>{@code min} takes precedence over {@code max}: If {@code min} is greater
 *       than {@code max}, the {@code min} value must be used.</li>
 *   <li>{@code max} takes precedence over {@code pref}: If {@code max} is smaller
 *       than {@code pref}, the {@code max} value must be used.
 * </ul>
 * The preferred size should therefore be clamped within the range {@code [min, max]}.
 *
 * <h2>Content Bias</h2>
 * An element's measurements may depend on its target size along the opposite axis:
 * <ul>
 *   <li>{@link Orientation#HORIZONTAL}: Height depends on width. Callers must pass
 *       the target width into {@code *Height(width)} methods. Pass {@code -1} for width queries.</li>
 *   <li>{@link Orientation#VERTICAL}: Width depends on height. Callers must pass
 *       the target height into {@code *Width(height)} methods. Pass {@code -1} for height queries.</li>
 *   <li>{@code null}: Unbiased. Callers should pass {@code -1} to all sizing queries.</li>
 * </ul>
 *
 * Layout engines must always check {@link #getContentBias()} before invoking measurement methods.
 *
 * @see Layoutable
 */
public interface Measurable {

    /**
     * This is a special value that might be returned by {@link #getBaselineOffset()}.
     * Indicates that the height of this element should be used as its baseline.
     */
    static final double BASELINE_OFFSET_SAME_AS_HEIGHT = Double.NEGATIVE_INFINITY;

    /**
     * Returns the minimum width for use in layout calculations.
     * <p>
     * Layout code which calls this method should first check the content-bias.
     * For a vertical content-bias callers should pass in a height value that
     * the minimum width should be based on. For a horizontal or null content-bias
     * the caller should pass in -1.
     * <p>
     * Implementations that have a vertical content-bias should honor the height
     * parameter whether -1 or a positive value. All other implementations may ignore
     * the height parameter (which will likely be -1).
     * <p>
     * If {@link #maxWidth(double)} is lower than this number, {@code minWidth} takes
     * precedence.
     *
     * @see #getContentBias()
     * @param height the height that should be used if minimum width depends on it
     * @return the minimum width required by this element; the result will never be NaN,
     *         nor will it ever be negative.
     */
    double minWidth(double height);

    /**
     * Returns the minimum height for use in layout calculations.
     * <p>
     * Layout code which calls this method should first check the content-bias.
     * For a horizontal content-bias callers should pass in a width value that
     * the minimum height should be based on. For a vertical or null content-bias
     * the caller should pass in -1.
     * <p>
     * Implementations that have a horizontal content-bias should honor the width
     * parameter whether -1 or a positive value. All other implementations may ignore
     * the width parameter (which will likely be -1).
     * <p>
     * If {@link #maxHeight(double)} is lower than this number, {@code minHeight} takes
     * precedence.
     *
     * @see #getContentBias()
     * @param width the width that should be used if minimum height depends on it
     * @return the minimum height required by this element; the result will never be NaN,
     *         nor will it ever be negative.
     */
    double minHeight(double width);

    /**
     * Returns the preferred width for use in layout calculations.
     * <p>
     * Layout code which calls this method should first check the content-bias.
     * For a vertical content-bias callers should pass in a height value that
     * the preferred width should be based on. For a horizontal or null content-bias
     * the caller should pass in -1.
     * <p>
     * Implementations that have a vertical content-bias should honor the height
     * parameter whether -1 or a positive value. All other implementations may ignore
     * the height parameter (which will likely be -1).
     *
     * @see #getContentBias()
     * @param height the height that should be used if preferred width depends on it
     * @return the preferred width for this element; the result will never be NaN,
     *         nor will it ever be negative.
     */
    double prefWidth(double height);

    /**
     * Returns the preferred height for use in layout calculations.
     * <p>
     * Layout code which calls this method should first check the content-bias.
     * For a horizontal content-bias callers should pass in a width value that
     * the preferred height should be based on. For a vertical or null content-bias
     * the caller should pass in -1.
     * <p>
     * Implementations that have a horizontal content-bias should honor the height
     * parameter whether -1 or a positive value. All other implementations may ignore
     * the height parameter (which will likely be -1).
     *
     * @see #getContentBias()
     * @param width the width that should be used if preferred height depends on it
     * @return the preferred height for this element; the result will never be NaN,
     *         nor will it ever be negative.
     */
    double prefHeight(double width);

    /**
     * Returns the maximum width for use in layout calculations.
     * A value of {@code Double.MAX_VALUE} indicates that the width may be
     * expanded beyond its preferred width without limits.
     * <p>
     * Layout code which calls this method should first check the content-bias.
     * For a vertical content-bias callers should pass in a height value that
     * the maximum width should be based on. For a horizontal or null content-bias
     * the caller should pass in -1.
     * <p>
     * Implementations that have a vertical content-bias should honor the height
     * parameter whether -1 or a positive value. All other implementations may ignore
     * the height parameter (which will likely be -1).
     * <p>
     * If {@link #minWidth(double)} is greater, it should take precedence
     * over the {@code maxWidth}.
     *
     * @see #getContentBias()
     * @param height the height that should be used if maximum width depends on it
     * @return the maximum width for this element; the result will never be NaN,
     *         nor will it ever be negative.
     */
    double maxWidth(double height);

    /**
     * Returns the maximum height for use in layout calculations.
     * A value of {@code Double.MAX_VALUE} indicates that the height may be
     * expanded beyond its preferred height without limits.
     * <p>
     * Layout code which calls this method should first check the content-bias.
     * For a horizontal content-bias callers should pass in a width value that
     * the maximum height should be based on. For a vertical or null content-bias
     * the caller should pass in -1.
     * <p>
     * Implementations that have a horizontal content-bias should honor the width
     * parameter whether -1 or a positive value. All other implementations may ignore
     * the width parameter (which will likely be -1).
     * <p>
     * If {@link #minHeight(double)} is greater, it should take precedence
     * over the {@code maxHeight}.
     *
     * @see #getContentBias()
     * @param width the width that should be used if maximum height depends on it
     * @return the maximum height for this element; the result will never be NaN,
     *         nor will it ever be negative.
     */
    double maxHeight(double width);

    /**
     * The 'alphabetic' (or 'roman') baseline offset from the element's top boundary
     * that should be used when this element is being vertically aligned by baseline with
     * other elements. By default this returns {@link #BASELINE_OFFSET_SAME_AS_HEIGHT} for resizable elements
     * and height for non-resizable elements. Implementations which contain text
     * should override this method to return their actual text baseline offset.
     *
     * @return offset of text baseline from top boundary for non-resizable elements or {@link #BASELINE_OFFSET_SAME_AS_HEIGHT} otherwise
     */
    double getBaselineOffset();

    /**
     * Returns the orientation of the resizing bias for layout purposes.
     * If there is no bias, returns null.  If its height depends on its width,
     * returns {@link Orientation#HORIZONTAL}, else if its width depends on its height,
     * returns {@link Orientation#VERTICAL}.
     *
     * @see #minWidth(double)
     * @see #minHeight(double)
     * @see #prefWidth(double)
     * @see #prefHeight(double)
     * @see #maxWidth(double)
     * @see #maxHeight(double)
     * @return orientation of width/height dependency or null if there is none
     */
    Orientation getContentBias();
}