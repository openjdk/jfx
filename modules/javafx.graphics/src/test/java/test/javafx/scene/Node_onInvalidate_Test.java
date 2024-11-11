/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import java.util.stream.Stream;
import java.util.List;

import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Shadow;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import javafx.css.CssMetaData;
import com.sun.javafx.scene.DirtyBits;
import javafx.css.Styleable;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;

import org.junit.jupiter.params.provider.Arguments;
import test.com.sun.javafx.test.OnInvalidateMethodsTestBase;

public class Node_onInvalidate_Test extends OnInvalidateMethodsTestBase {

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( new Configuration(Rectangle.class, "visible", false, new DirtyBits[] {DirtyBits.NODE_VISIBLE, DirtyBits.NODE_BOUNDS}) ),
            Arguments.of( new Configuration(Rectangle.class, "cursor", Cursor.WAIT, new CssMetaData[] {findCssCSSProperty("-fx-cursor")}) ),
            Arguments.of( new Configuration(Rectangle.class, "opacity", 0.5, new CssMetaData[] {findCssCSSProperty("-fx-opacity")}) ),
            Arguments.of( new Configuration(Rectangle.class, "opacity", 0.5, new DirtyBits[] {DirtyBits.NODE_OPACITY}) ),
            Arguments.of( new Configuration(Rectangle.class, "viewOrder", 0.5, new CssMetaData[] {findCssCSSProperty("-fx-view-order")}) ),
            Arguments.of( new Configuration(Rectangle.class, "viewOrder", 0.5, new DirtyBits[] {DirtyBits.NODE_VIEW_ORDER}) ),
            Arguments.of( new Configuration(Rectangle.class, "blendMode", BlendMode.DARKEN, new CssMetaData[] {findCssCSSProperty("-fx-blend-mode")}) ),
            Arguments.of( new Configuration(Rectangle.class, "blendMode", BlendMode.DARKEN, new DirtyBits[] {DirtyBits.NODE_BLENDMODE}) ),
            Arguments.of( new Configuration(Rectangle.class, "cache", true, new DirtyBits[] {DirtyBits.NODE_CACHE}) ),
            Arguments.of( new Configuration(Rectangle.class, "cacheHint", CacheHint.QUALITY, new DirtyBits[] {DirtyBits.NODE_CACHE}) ),
            Arguments.of( new Configuration(Rectangle.class, "effect", new Shadow(), new CssMetaData[] {findCssCSSProperty("-fx-effect")}) ),
            Arguments.of( new Configuration(Rectangle.class, "translateX", 1.5, new CssMetaData[] {findCssCSSProperty("-fx-translate-x")}) ),
            Arguments.of( new Configuration(Rectangle.class, "translateX", 1.5, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "translateY", 1.5, new CssMetaData[] {findCssCSSProperty("-fx-translate-y")}) ),
            Arguments.of( new Configuration(Rectangle.class, "translateY", 1.5, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "translateZ", 1.5, new CssMetaData[] {findCssCSSProperty("-fx-translate-z")}) ),
            Arguments.of( new Configuration(Rectangle.class, "translateZ", 1.5, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "scaleX", 5.5, new CssMetaData[] {findCssCSSProperty("-fx-scale-x")}) ),
            Arguments.of( new Configuration(Rectangle.class, "scaleX", 5.5, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "scaleY", 5.5, new CssMetaData[] {findCssCSSProperty("-fx-scale-y")}) ),
            Arguments.of( new Configuration(Rectangle.class, "scaleY", 5.5, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "scaleZ", 5.5, new CssMetaData[] {findCssCSSProperty("-fx-scale-z")}) ),
            Arguments.of( new Configuration(Rectangle.class, "scaleZ", 5.5, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "rotate", 55, new CssMetaData[] {findCssCSSProperty("-fx-rotate")}) ),
            Arguments.of( new Configuration(Rectangle.class, "rotate", 55, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "rotationAxis", Rotate.X_AXIS, new DirtyBits[] {DirtyBits.NODE_TRANSFORM}) ),
            Arguments.of( new Configuration(Rectangle.class, "clip", new Rectangle(10, 10), new DirtyBits[] {DirtyBits.NODE_CLIP}) ),
            Arguments.of( new Configuration(Rectangle.class, "focusTraversable", true, new CssMetaData[] {findCssCSSProperty("-fx-focus-traversable")}) )
        );
    }


    public static CssMetaData findCssCSSProperty(String propertyName) {
        final List<CssMetaData<? extends Styleable, ?>> keys = Node.getClassCssMetaData();
        for(CssMetaData styleable : keys) {
            if (styleable.getProperty().equals(propertyName)) return styleable;
        }
        return null;
    }

}
