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

package jfx.incubator.scene.control.richtext;

import java.util.HashMap;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.skin.CellContext;

/**
 * Style Handler Registry keeps track of the {@code StyleAttributeHandler} for supported
 * {@code StyleAttribute}s.  The registry, once created using its {@code Builder}, is immutable.
 *
 * This class is needed when extending the RichTextArea with support for other style attributes.
 * Applications should not normally use this interface.
 *
 * @since 24
 */
public class StyleHandlerRegistry {
    private final HashMap<StyleAttribute,StyleAttributeHandler> parStyleHandlerMap;
    private final HashMap<StyleAttribute,StyleAttributeHandler> segStyleHandlerMap;

    private StyleHandlerRegistry(HashMap<StyleAttribute,StyleAttributeHandler> p, HashMap<StyleAttribute,StyleAttributeHandler> s) {
        parStyleHandlerMap = p;
        segStyleHandlerMap = s;
    }

    /**
     * Creates a builder initialized with the parent's registry content.
     *
     * @param parent the parent class' registry (can be null)
     * @return the builder instance
     */
    public static Builder builder(StyleHandlerRegistry parent) {
        Builder b = new Builder();
        if (parent != null) {
            b.parStyleHandlerMap.putAll(parent.parStyleHandlerMap);
            b.segStyleHandlerMap.putAll(parent.segStyleHandlerMap);
        }
        return b;
    }

    /**
     * Invokes the handler, if present, for the specified attribute in the context of the specified control.
     *
     * @param <C> the control type
     * @param <T> the attribute value type
     * @param control the control reference
     * @param forParagraph specifies which attribute to search for: paragraph ({@code true}) or text segment ({@code false})
     * @param cx the cell context
     * @param a the attribute
     * @param value the attribute value
     */
    public <C extends RichTextArea, T> void process(C control, boolean forParagraph, CellContext cx, StyleAttribute<T> a, T value) {
        StyleAttributeHandler h = (forParagraph ? parStyleHandlerMap : segStyleHandlerMap).get(a);
        if (h != null) {
            h.apply(control, cx, value);
        }
    }

    /**
     * The builder used to create an immutable instance of {@code StyleHandlerRegistry}.
     */
    public static class Builder {
        private HashMap<StyleAttribute, StyleAttributeHandler> parStyleHandlerMap = new HashMap<>();
        private HashMap<StyleAttribute, StyleAttributeHandler> segStyleHandlerMap = new HashMap<>();

        Builder() {
        }

        /**
         * Sets the paragraph handler for the given attribute.
         *
         * @param <C> the control type
         * @param <T> the attribute value type
         * @param a the attribute
         * @param h the handler
         */
        public <C extends RichTextArea, T> void setParHandler(StyleAttribute<T> a, StyleAttributeHandler<C, T> h) {
            parStyleHandlerMap.put(a, h);
        }

        /**
         * Sets the text segment handler for the given attribute.
         *
         * @param <C> the control type
         * @param <T> the attribute value type
         * @param a the attribute
         * @param h the handler
         */
        public <C extends RichTextArea, T> void setSegHandler(StyleAttribute<T> a, StyleAttributeHandler<C, T> h) {
            segStyleHandlerMap.put(a, h);
        }

        /**
         * Creates an immutable instance of {@code StyleHandlerRegistry}.
         * @return the {@code StyleHandlerRegistry} instance
         */
        public StyleHandlerRegistry build() {
            return new StyleHandlerRegistry(parStyleHandlerMap, segStyleHandlerMap);
        }
    }
}
