/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import javafx.scene.text.Font;

/**
 * A representation of a parsed CSS value. {@code V} is the type of the parsed
 * value, {@code T} is the {@code StyleableProperty} type of the converted value.
 * Instances of {@code ParsedValue} are created by the CSS parser. For example,
 * the parser creates a {@code ParsedValue<String,Color>} when it parses a
 * web Color.
 * <p>
 * A ParsedValue is meaningful to the code that calculates actual values from
 * parsed CSS values. Elsewhere the value returned by
 * {@link #getValue()} is likely to be obscure, abstruse and perplexing.
 * @since JavaFX 8.0
 */
public class ParsedValue<V, T> {

    /**
     * The CSS property value as created by the parser.
     */
    final protected V value;

    /**
     * Gets the CSS property value as created by the parser, which may be null
     * or otherwise incomprehensible.
     * @return the CSS property value
     */
    public final V getValue() { return value; }

    /**
     * The {@code StyleConverter} which converts the parsed value to
     * the type of the {@link StyleableProperty}. This may be null, in which
     * case {@link #convert(javafx.scene.text.Font) convert}
     * will return {@link #getValue() getValue()}
     */
    final protected StyleConverter<V, T> converter;

    /**
     * A {@code StyleConverter} converts the parsed value to
     * the type of the {@link StyleableProperty}. If the {@code StyleConverter}
     * is null, {@link #convert(javafx.scene.text.Font)}
     * will return {@link #getValue()}
     * @return The {@code StyleConverter} which converts the parsed value to
     * the type of the {@link StyleableProperty}. May return null.
     */
    public final StyleConverter<V, T> getConverter() { return converter; }

    /**
     * Convenience method for calling
     * {@link StyleConverter#convert(javafx.css.ParsedValue, javafx.scene.text.Font) convert}
     * on this {@code ParsedValue}.
     * @param font         The {@link Font} to use when converting a
     * <a href="http://www.w3.org/TR/css3-values/#relative-lengths">relative</a>
     * value.
     * @return The value converted to the type of the {@link StyleableProperty}
     * @see #getConverter()
     */
    @SuppressWarnings("unchecked")
    public T convert(Font font) {
        // unchecked!
        return (T)((converter != null) ? converter.convert(this, font) : value);
    }

    /**
     * If value is itself a ParsedValue or sequence of values, and should any of
     * those values need to be looked up, then this flag is set. This
     * does not mean that this particular value needs to be looked up, but
     * that this value contains a value that needs to be looked up.
     *
     * @return true if this value contains a value that needs to be looked up,
     * otherwise false
     * @since 9
     */
    public boolean isContainsLookups() { return false; }

     /**
     * If value references another property, then the real value needs to
     * be looked up.
     *
     * @return true if value references another property, otherwise false
     * @since 9
     */
    public boolean isLookup() { return false; }

    /**
     * Create an instance of ParsedValue where the value type V is converted to
     * the target type T using the given converter.
     * If {@code converter} is null, then it is assumed that the type of value
     * {@code V} and the type of target {@code T} are the same and
     * do not need converted.
     * @param value the value to be converted
     * @param converter the converter
     */
    protected ParsedValue(V value, StyleConverter<V, T> converter) {
        this.value = value;
        this.converter = converter;
    }

}
