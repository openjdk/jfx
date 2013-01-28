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

import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.converters.StringConverter;
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
 * @param N The type of Node
 */
@com.sun.javafx.beans.annotations.NoBuilder
public abstract class FontCssMetaData<N extends Node> extends CssMetaData<N, Font> {

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

    private static <N extends Node> List<CssMetaData<? extends Node, ?>> createSubProperties(String property, Font initial) {
        
        final List<CssMetaData<N, ?>> subProperties = 
                new ArrayList<CssMetaData<N, ?>>();
        
        final Font defaultFont = initial != null ? initial : Font.getDefault();
        
        final CssMetaData<N, String> FAMILY = 
                new CssMetaData<N, String>(property.concat("-family"), 
                StringConverter.getInstance(), defaultFont.getFamily(), true) {
            @Override
            public boolean isSettable(N node) {
                return false;
            }

            @Override
            public StyleableProperty<String> getStyleableProperty(N node) {
                return null;
            }
        };
        subProperties.add(FAMILY);
        
        final CssMetaData<N, Number> SIZE = 
                new CssMetaData<N, Number>(property.concat("-size"), 
                SizeConverter.getInstance(), defaultFont.getSize(), true) {
            @Override
            public boolean isSettable(N node) {
                return false;
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(N node) {
                return null;
            }
        };
        subProperties.add(SIZE);
        
        final CssMetaData<N, FontPosture> STYLE = 
                new CssMetaData<N, FontPosture>(property.concat("-style"), 
                FontConverter.FontStyleConverter.getInstance(), FontPosture.REGULAR, true) {
            @Override
            public boolean isSettable(N node) {
                return false;
            }

            @Override
            public StyleableProperty<FontPosture> getStyleableProperty(N node) {
                return null;
            }
        };
        subProperties.add(STYLE);
        
        final CssMetaData<N, FontWeight> WEIGHT = 
                new CssMetaData<N, FontWeight>(property.concat("-weight"), 
                FontConverter.FontWeightConverter.getInstance(), FontWeight.NORMAL, true) {
            @Override
            public boolean isSettable(N node) {
                return false;
            }

            @Override
            public StyleableProperty<FontWeight> getStyleableProperty(N node) {
                return null;
            }
        };
        subProperties.add(WEIGHT);
        
        return Collections.<CssMetaData<? extends Node, ?>>unmodifiableList(subProperties);
    }
    
}
