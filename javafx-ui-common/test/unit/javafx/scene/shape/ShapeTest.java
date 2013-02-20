/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import javafx.css.CssMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.NodeTest;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.junit.Assert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ShapeTest {

    @Test public void testBoundPropertySync_StrokeType() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "strokeType", "strokeType", StrokeType.CENTERED);
    }

    @Test public void testBoundPropertySync_StrokeLineCap() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "strokeLineCap", "strokeLineCap", StrokeLineCap.SQUARE);
    }

    @Test public void testBoundPropertySync_StrokeLineJoin() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "strokeLineJoin", "strokeLineJoin", StrokeLineJoin.MITER);
    }

    @Test public void testBoundPropertySync_StrokeWidth() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "strokeWidth", "strokeWidth", 2.0);
    }

    @Test public void testBoundPropertySync_StrokeMiterLimit() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "strokeMiterLimit", "strokeMiterLimit", 3.0);
    }

    @Test public void testBoundPropertySync_DashOffset() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new Line(0.0 ,0.0, 100.0, 100.0),
                "strokeDashOffset", "strokeDashOffset", 15.0);
    }

    @Test public void testBoundPropertySync_Stroke() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                LineBuilder.create().stroke(Color.RED).build(),
                "stroke", "stroke", Color.GREEN);
    }

    @Test public void testBoundPropertySync_Fill() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                RectangleBuilder.create().x(0).y(0).width(100).height(100).fill(Color.BLUE).build(),
                "fill", "fill", Color.RED);
    }

    @Test public void testBoundPropertySync_Smooth() throws Exception {
        NodeTest.assertBooleanPropertySynced(
                RectangleBuilder.create().smooth(true).build(),
                "smooth", "smooth", false);
    }
    
    boolean listChangeCalled = false;
    @Test public void testStrokeDashArray() {
        // there is no strokeDashArrayProperty or this test
        // would be in Shape_properties_Test
        final ObservableList<Double> expected = 
                FXCollections.observableArrayList(Double.valueOf(1),
                                                  Double.valueOf(2),
                                                  Double.valueOf(3));
        Rectangle rect = new Rectangle();
        ObservableList<Double> actual = rect.getStrokeDashArray();
        assertNotNull(actual);
        assertTrue(actual.isEmpty());

        actual.addListener(new ListChangeListener<Double>(){

            @Override
            public void onChanged(Change<? extends Double> c) {
                listChangeCalled = true;
                assertTrue(c.next());
                assertEquals(expected, c.getAddedSubList());
            }
        });
        
        rect.getStrokeDashArray().addAll(expected);
        actual = rect.getStrokeDashArray();
        assertEquals(expected, actual);
        
        assertTrue(listChangeCalled);
    }
    
    @Test public void testGetStrokeDashArrayViaCSSPropertyIsNotNull() {
        Rectangle rect = new Rectangle();
        Double[] actual = null;
        List<CssMetaData<? extends Styleable, ?>> styleables = rect.getCssMetaData();
        for (CssMetaData styleable : styleables) {
            if ("-fx-stroke-dash-array".equals(styleable.getProperty())) {
                WritableValue writable = styleable.getStyleableProperty(rect);
                actual = (Double[])writable.getValue();
                break;
            }
        }
        assertNotNull(actual);
    }
    
    @Test public void testGetStrokeDashArrayViaCSSPropertyIsSame() {
        
        Rectangle rect = new Rectangle();
        rect.getStrokeDashArray().addAll(5d, 7d, 1d, 3d);
        Double[] actuals = null;
        List<CssMetaData<? extends Styleable, ?>> styleables = rect.getCssMetaData();
        
        for (CssMetaData styleable : styleables) {
            if ("-fx-stroke-dash-array".equals(styleable.getProperty())) {
                WritableValue writable = styleable.getStyleableProperty(rect);
                actuals = (Double[])writable.getValue();
            }
        }
        
        final Double[] expecteds = new Double[] {5d, 7d, 1d, 3d};
        Assert.assertArrayEquals(expecteds, actuals);
    }

    @Test public void testSetStrokeDashArrayViaCSSPropertyIsSame() {
        
        Rectangle rect = new Rectangle();
        List<Double> actual = null;
        List<CssMetaData<? extends Styleable, ?>> styleables = rect.getCssMetaData();
        
        for (CssMetaData styleable : styleables) {
            if ("-fx-stroke-dash-array".equals(styleable.getProperty())) {
                styleable.set(rect, new Double[] {5d, 7d, 1d, 3d}, null);
                actual = rect.getStrokeDashArray();
            }
        }
        
        final List<Double> expected = new ArrayList();
        Collections.addAll(expected, 5d, 7d, 1d, 3d);
        assertEquals(expected, actual);
    }
    
    // RT-18647: ClassCastException: [Ljava.lang.Double; cannot be cast to javafx.collections.ObservableList
    @Test public void testRT_18647() {
        
        final Scene scene = new Scene(new Group(), 500, 500);
        
        Rectangle rect = new Rectangle(100,100);
        rect.setStyle("-fx-stroke-dash-array: 5 7 1 3;");

        ((Group)scene.getRoot()).getChildren().add(rect);
        rect.impl_processCSS(true);

        final List<Double> expected = new ArrayList();
        Collections.addAll(expected, 5d, 7d, 1d, 3d);

        List<Double> actual = rect.getStrokeDashArray();
        assertEquals(expected, actual);        
        
    }
    
}
