/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.model;

import java.util.Objects;

/**
 * Style Attribute provides a way to specify style in the RichTextArea.
 *
 * @param <T> the attribute value type
 * @see StyleAttributeMap
 * @since 24
 */
public abstract class StyleAttribute<T> {
    private final String name;
    private final Class<T> type;

    /**
     * Constructs the style attribute.
     *
     * @param name the attribute name (cannot be null)
     * @param type the attribute type (cannot be null)
     */
    private StyleAttribute(String name, Class<T> type) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        this.name = name;
        this.type = type;
    }

    /**
     * Creates a character attribute.
     * @param <P> the attribute value type
     * @param name the attribute name
     * @param type the attribute value type
     * @return the new character attribute instance
     * @since 27
     */
    public static <P> StyleAttribute<P> character(String name, Class<P> type) {
        return new StyleAttribute<P>(name, type) {
            @Override
            public boolean isCharacterAttribute() {
                return true;
            }
        };
    }

    /**
     * Creates a document attribute.
     * @param <P> the attribute value type
     * @param name the attribute name
     * @param type the attribute value type
     * @return the new document attribute instance
     * @since 27
     */
    public static <P> StyleAttribute<P> document(String name, Class<P> type) {
        return new StyleAttribute<P>(name, type) {
            @Override
            public boolean isDocumentAttribute() {
                return true;
            }
        };
    }

    /**
     * Creates a paragraph attribute.
     * @param <P> the attribute value type
     * @param name the attribute name
     * @param type the attribute value type
     * @return the new paragraph attribute instance
     * @since 27
     */
    public static <P> StyleAttribute<P> paragraph(String name, Class<P> type) {
        return new StyleAttribute<P>(name, type) {
            @Override
            public boolean isParagraphAttribute() {
                return true;
            }
        };
    }

    /**
     * Attribute name.
     *
     * @return attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the class corresponding to the attribute value.
     *
     * @return attribute type
     */
    public final Class<T> getType() {
        return type;
    }

    /**
     * Returns true if this instance is a character attribute.
     * @return true for a character attribute
     * @since 27
     */
    public boolean isCharacterAttribute() {
        return false;
    }

    /**
     * Returns true if this instance is a document attribute.
     * @return true for a document attribute
     * @since 27
     */
    public boolean isDocumentAttribute() {
        return false;
    }

    /**
     * Returns true if this instance is a paragraph attribute.
     * @return true for a paragraph attribute
     */
    public boolean isParagraphAttribute() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof StyleAttribute a) {
            return
                (isCharacterAttribute() == a.isCharacterAttribute()) &&
                (isDocumentAttribute() == a.isDocumentAttribute()) &&
                (isParagraphAttribute() == a.isParagraphAttribute()) &&
                (type == a.type) &&
                name.equals(a.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = getClass().hashCode();
        h = 31 * h + name.hashCode();
        h = 31 * h + type.hashCode();
        return h;
    }
}
