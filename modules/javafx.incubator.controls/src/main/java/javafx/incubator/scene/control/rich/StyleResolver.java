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

package javafx.incubator.scene.control.rich;

import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;

/**
 * Enables conversion of CSS styles to {@link StyleAttribute}s.
 * <p>
 * Whenever the {@code StyledTextModel} contains logical class names instead of actual attributes,
 * a separate CSS style resolution step is required.  The resulting attributes might depend on the view that
 * originated an operation (example: export or copy).
 */
public interface StyleResolver {
    /**
     * Resolves CSS styles (when present) to the individual attributes declared in {@link StyleAttrs}.
     * @param attrs the style attributes
     * @return the resolved style attributes
     */
    // TODO this can take CssStyles argument instead
    public StyleAttrs resolveStyles(StyleAttrs attrs);

    /**
     * Creates a snapshot of the specified Node.
     * @param node the {@link Node} to make a snapshot of
     * @return snapshot the generated {@link WritableImage}
     */
    public WritableImage snapshot(Node node);
}
