/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.shape;

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.ShapeHelper;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Group;
import test.javafx.scene.NodeTest;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import static org.junit.Assert.*;

public class ShapeTest {

    @Test public void testBoundPropertySync_StrokeType() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                new StubShape(),
                "strokeType", "strokeType", StrokeType.CENTERED);
    }

    @Test public void testBoundPropertySync_StrokeLineCap() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                new StubShape(),
                "strokeLineCap", "strokeLineCap", StrokeLineCap.SQUARE);
    }

    @Test public void testBoundPropertySync_StrokeLineJoin() throws Exception {
        NodeTest.assertObjectProperty_AsStringSynced(
                new StubShape(),
                "strokeLineJoin", "strokeLineJoin", StrokeLineJoin.MITER);
    }

    @Test public void testBoundPropertySync_StrokeWidth() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubShape(),
                "strokeWidth", "strokeWidth", 2.0);
    }

    @Test public void testBoundPropertySync_StrokeMiterLimit() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubShape(),
                "strokeMiterLimit", "strokeMiterLimit", 3.0);
    }

    @Test public void testBoundPropertySync_DashOffset() throws Exception {
        NodeTest.assertDoublePropertySynced(
                new StubShape(),
                "strokeDashOffset", "strokeDashOffset", 15.0);
    }

    @Test public void testBoundPropertySync_Stroke() throws Exception {
        final Shape shape = new StubShape();
        shape.setStroke(Color.RED);
        NodeTest.assertObjectProperty_AsStringSynced(
                shape, "stroke", "stroke", Color.GREEN);
    }

    @Test public void testBoundPropertySync_Fill() throws Exception {
        final Shape shape = new StubShape();
        shape.setFill(Color.BLUE);
        NodeTest.assertObjectProperty_AsStringSynced(
                shape, "fill", "fill", Color.RED);
    }

    @Test public void testBoundPropertySync_Smooth() throws Exception {
        final Shape shape = new StubShape();
        shape.setSmooth(true);
        NodeTest.assertBooleanPropertySynced(
                shape, "smooth", "smooth", false);
    }

    boolean listChangeCalled = false;
    @Test public void testStrokeDashArray() {
        // there is no strokeDashArrayProperty or this test
        // would be in Shape_properties_Test
        final ObservableList<Double> expected =
                FXCollections.observableArrayList(Double.valueOf(1),
                                                  Double.valueOf(2),
                                                  Double.valueOf(3));
        final Shape shape = new StubShape();
        ObservableList<Double> actual = shape.getStrokeDashArray();
        assertNotNull(actual);
        assertTrue(actual.isEmpty());

        actual.addListener((ListChangeListener<Double>) c -> {
            listChangeCalled = true;
            assertTrue(c.next());
            Assert.assertEquals(expected, c.getAddedSubList());
        });

        shape.getStrokeDashArray().addAll(expected);
        actual = shape.getStrokeDashArray();
        assertEquals(expected, actual);

        assertTrue(listChangeCalled);
    }

    @Test public void testGetStrokeDashArrayViaCSSPropertyIsNotNull() {
        final Shape shape = new StubShape();
        Double[] actual = null;
        List<CssMetaData<? extends Styleable, ?>> styleables = shape.getCssMetaData();
        for (CssMetaData styleable : styleables) {
            if ("-fx-stroke-dash-array".equals(styleable.getProperty())) {
                WritableValue writable = styleable.getStyleableProperty(shape);
                actual = (Double[])writable.getValue();
                break;
            }
        }
        assertNotNull(actual);
    }

    @Test public void testGetStrokeDashArrayViaCSSPropertyIsSame() {
        final Shape shape = new StubShape();
        shape.getStrokeDashArray().addAll(5d, 7d, 1d, 3d);
        Double[] actuals = null;
        List<CssMetaData<? extends Styleable, ?>> styleables = shape.getCssMetaData();

        for (CssMetaData styleable : styleables) {
            if ("-fx-stroke-dash-array".equals(styleable.getProperty())) {
                WritableValue writable = styleable.getStyleableProperty(shape);
                actuals = (Double[])writable.getValue();
            }
        }

        final Double[] expecteds = new Double[] {5d, 7d, 1d, 3d};
        Assert.assertArrayEquals(expecteds, actuals);
    }

    @Test public void testSetStrokeDashArrayViaCSSPropertyIsSame() {
        final Shape shape = new StubShape();
        List<Double> actual = null;
        List<CssMetaData<? extends Styleable, ?>> styleables = shape.getCssMetaData();

        for (CssMetaData styleable : styleables) {
            if ("-fx-stroke-dash-array".equals(styleable.getProperty())) {
                StyleableProperty styleableProperty = styleable.getStyleableProperty(shape);
                styleableProperty.applyStyle(null, new Double[] {5d, 7d, 1d, 3d});
                actual = shape.getStrokeDashArray();
            }
        }

        final List<Double> expected = new ArrayList();
        Collections.addAll(expected, 5d, 7d, 1d, 3d);
        assertEquals(expected, actual);
    }

    // RT-18647: ClassCastException: [Ljava.lang.Double; cannot be cast to javafx.collections.ObservableList
    @Test public void testRT_18647() {
        final Scene scene = new Scene(new Group(), 500, 500);

        final Shape shape = new StubShape();
        shape.setStyle("-fx-stroke-dash-array: 5 7 1 3;");

        ((Group)scene.getRoot()).getChildren().add(shape);
        shape.applyCss();

        final List<Double> expected = new ArrayList();
        Collections.addAll(expected, 5d, 7d, 1d, 3d);

        List<Double> actual = shape.getStrokeDashArray();
        assertEquals(expected, actual);

    }

    boolean listenerCalled = false;
    // make sure shapeChangeListener doesn't hold reference to runnable.
    @Test public void testShapeChangeListenerLeakTest() {

        Shape shape = new StubShape();

        Runnable listener = () -> {
            listenerCalled = true;
        };

        ShapeHelper.setShapeChangeListener(shape, listener);

        // sync peer to clear out dirty bits
        NodeHelper.syncPeer(shape);

        // should trigger listener
        shape.setFill(Color.GREEN);

        assert(listenerCalled);

        listener = null;
        System.gc();

        // sync peer to clear out dirty bits
        NodeHelper.syncPeer(shape);

        // this flag should remain false (listener should not be called)
        listenerCalled = false;

        shape.setFill(Color.RED);

        assert(!listenerCalled);
    }

}
