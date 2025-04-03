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

package jfx.incubator.scene.control.richtext;

import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Enables conversion of CSS styles to {@code StyleAttribute}s.
 * <p>
 * Whenever the {@code StyledTextModel} contains logical class names instead of actual attributes,
 * a separate CSS style resolution step is required.  The resulting attributes might depend on the view that
 * originated an operation such as exporting or coping.
 * <p>
 * This interface is a part of API layer between the model and the view, and only comes to play when the
 * model refers to CSS styles.
 * Applications should not normally use this interface.
 *
 * @since 24
 */
public interface StyleResolver {
    /**
     * Resolves CSS styles (when present) to the individual attributes declared in {@link StyleAttributeMap}.
     *
     * @param attrs the style attributes
     * @return the resolved style attributes
     */
    public StyleAttributeMap resolveStyles(StyleAttributeMap attrs);

    /**
     * Creates a snapshot of the specified Node to be exported or copied as an image.
     *
     * @param node the {@link Node} to make a snapshot of
     * @return snapshot the generated {@link WritableImage}
     */
    public WritableImage snapshot(Node node);
}
