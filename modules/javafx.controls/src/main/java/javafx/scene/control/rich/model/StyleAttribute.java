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

/**
 * Style Attribute provides a way to specify style in the RichTextArea.
 */
public class StyleAttribute<T> {
    private final String name;
    private final Class<T> type;
    private final boolean isParagraph;

    /**
     * Constructs a style attribute.
     * @param name the attribute name
     * @param type the attribute type
     * @param isParagraph specifies a paragraph attribute (true), or a character attribute (false)
     */
    public StyleAttribute(String name, Class<T> type, boolean isParagraph) {
        this.name = name;
        this.type = type;
        this.isParagraph = isParagraph;
    }

    /**
     * Attribute name.
     * @return attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the class corresponding to the attribute value.
     * The value must be Serializable.
     * @return attribute type
     */
    public final Class<T> getType() {
        return type;
    }

    /**
     * Returns true for a paragraph attribute, false for a character attribute.
     * @return true for a paragraph attribute, false for a character attribute
     */
    public boolean isParagraphAttribute() {
        return isParagraph;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int h = StyleAttribute.class.hashCode();
        h = 31 * h + name.hashCode();
        h = 31 * h + type.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof StyleAttribute a) {
            return (type == a.type) && (name.equals(a.name));
        }
        return false;
    }
}