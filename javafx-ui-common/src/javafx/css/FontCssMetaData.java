/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.Size;
import com.sun.javafx.css.SizeUnits;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.SizeConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * An partial implementation of CssMetaData for Font properties which
 * includes the font sub-properties: weight, style, family and size.
 * @param <T> The type of Node
 */
public abstract class FontCssMetaData<T extends Node> extends CssMetaData<T, Font> {

    /**
     * The property name is concatenated with &quot;-weight&quot;,
     * &quot;-style&quot;, &quot;-family&quot; and &quot;-size&quot; to
     * create the sub-properties. For example,
     * {@code new FontCssMetaData<Text>("-fx-font", Font.getDefault());}
     * will create a CssMetaData for &quot;-fx-font&quot; with
     * sub-properties: &quot;-fx-font-weight&quot;,
     * &quot;-fx-font-style&quot;, &quot;-fx-font-family&quot;
     * and &quot;-fx-font-size&quot;
     * @param property
     * @param initial
     */
    /**
     * The property name is concatenated with &quot;-weight&quot;,
     * &quot;-style&quot;, &quot;-family&quot; and &quot;-size&quot; to
     * create the sub-properties. For example,
     * {@code new FontCssMetaData<Text>("-fx-font", Font.getDefault());}
     * will create a CssMetaData for &quot;-fx-font&quot; with
     * sub-properties: &quot;-fx-font-weight&quot;,
     * &quot;-fx-font-style&quot;, &quot;-fx-font-family&quot;
     * and &quot;-fx-font-size&quot;
     * @param property
     * @param initial
     */
    public FontCssMetaData(String property, Font initial) {
        super(property, FontConverter.getInstance(), initial, true, createSubProperties(property, initial));
    }

    private static List<CssMetaData> createSubProperties(String property, Font initial) {
        Font defaultFont = initial != null ? initial : Font.getDefault();
        final CssMetaData<Node, Size> SIZE = 
                new CssMetaData<Node, Size>(property.concat("-size"), 
                SizeConverter.getInstance(), new Size(defaultFont.getSize(), SizeUnits.PT), true) {
            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public StyleableProperty<Size> getStyleableProperty(Node node) {
                return null;
            }
        };
        final CssMetaData<Node, FontWeight> WEIGHT = 
                new CssMetaData<Node, FontWeight>(property.concat("-weight"), 
                SizeConverter.getInstance(), FontWeight.NORMAL, true) {
            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public StyleableProperty<FontWeight> getStyleableProperty(Node node) {
                return null;
            }
        };
        final CssMetaData<Node, FontPosture> STYLE = 
                new CssMetaData<Node, FontPosture>(property.concat("-style"), 
                SizeConverter.getInstance(), FontPosture.REGULAR, true) {
            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public StyleableProperty<FontPosture> getStyleableProperty(Node node) {
                return null;
            }
        };
        final CssMetaData<Node, String> FAMILY = 
                new CssMetaData<Node, String>(property.concat("-family"), 
                SizeConverter.getInstance(), defaultFont.getFamily(), true) {
            @Override
            public boolean isSettable(Node node) {
                return false;
            }

            @Override
            public StyleableProperty<String> getStyleableProperty(Node node) {
                return null;
            }
        };
        final List<CssMetaData> subProperties = new ArrayList<CssMetaData>();
        Collections.addAll(subProperties, FAMILY, SIZE, STYLE, WEIGHT);
        return Collections.unmodifiableList(subProperties);
    }
    
}
