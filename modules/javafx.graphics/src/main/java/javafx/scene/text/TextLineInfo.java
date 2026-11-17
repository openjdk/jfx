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

/**
 * Provides the information about a text line in a text layout.
 *
 * @param start the start offset for the line
 * @param end the end offset for the line (index of the last character + 1)
 * @param bounds the bounds of the text line, in local coordinates:
 * <ul>
 *   <li>
 *     {@code minX} - the x origin of the line (relative to the layout).
 *     The x origin is defined by TextAlignment of the text layout, always zero
 *     for left-aligned text.
 *   <li>
 *     {@code minY} - the ascent of the line (negative).
 *     The ascent of the line is the max ascent of all fonts in the line.
 *   <li>
 *     {@code width} - the width of the line.
 *     The width of the line is sum of all the run widths in the line, it is not
 *     affect by the wrapping width but it will include any changes caused by
 *     justification.
 *   <li>
 *     {@code height} - the height of the line.
 *     The height of the line is sum of the max ascent, max descent, and
 *     max line gap of all the fonts in the line.
 * </ul>
 *
 * @since 25
 */
public record TextLineInfo(int start, int end, Rectangle2D bounds) {
}
