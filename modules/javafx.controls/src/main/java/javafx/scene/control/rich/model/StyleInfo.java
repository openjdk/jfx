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

package javafx.scene.control.rich.model;

import javafx.scene.control.rich.StyleResolver;

/**
 * Style information of a text segment can be represented either by a combination of
 * direct style (e.g. "-fx-fill:red;") and CSS stylesheet names, or a set of {@link StyleAttribute}s.
 * 
 * Objects of this class are immutable.
 */
public abstract class StyleInfo {
    /**
     * Returns the actual style attributes for the given {@link StyleResolver}.
     * @param resolver
     * @return attributes instance, or null
     */
    public abstract StyleAttrs getStyleAttrs(StyleResolver resolver);

    public static final StyleInfo NONE = StyleInfo.of(null, null);

    private StyleInfo() {
    }

    /**
     * Creates a {@code StyleInfo} instance from direct style and an array of style names,
     * to be used when the actual style attributes are not maintained by the model.
     *
     * @param direct direct style
     * @param css stylesheet style names
     */
    public static StyleInfo of(String direct, String[] css) {
        return new StyleInfo() {
            @Override
            public StyleAttrs getStyleAttrs(StyleResolver resolver) {
                if ((direct == null) && (css == null)) {
                    return null;
                }
                return resolver.convert(direct, css);
            }
        };
    }

    /**
     * Creates a {@code StyleInfo} instance from a set of attributes.
     *
     * @param attrs
     */
    public static StyleInfo of(StyleAttrs attrs) {
        return new StyleInfo() {
            @Override
            public StyleAttrs getStyleAttrs(StyleResolver resolver) {
                return attrs;
            }
        };
    }
}