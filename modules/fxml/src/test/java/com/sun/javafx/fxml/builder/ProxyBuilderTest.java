/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.fxml.builder;

import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.StageStyle;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

public class ProxyBuilderTest {

    @Test
    public void testMutable() {
        ProxyBuilder pb = new ProxyBuilder(MutableClass.class);
        pb.put("intValue", 123);
        pb.put("doubleValue", 1.23);
        pb.put("stringValue", "This is String");

        MutableClass result = (MutableClass) pb.build();
        assertEquals(123, result.intValue);
        assertEquals(1.23, result.doubleValue, 1e-5);
        assertEquals("This is String", result.stringValue);
    }

    @Test
    public void testImmutable() {
        ProxyBuilder pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        ImmutableClass result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertEquals(456, result.b, 1e-10);
    }

    @Test
    public void testImmutableWithList() {
        ProxyBuilder pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5);
        pb.put("list", inputList);

        ImmutableClass result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertEquals(456, result.b, 1e-10);
        assertArrayEquals(inputList.toArray(), result.list.toArray());

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        Integer[] inputArray = new Integer[]{1, 2, 3, 4, 5};
        pb.put("list", inputArray);

        result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertEquals(456, result.b, 1e-10);
        assertArrayEquals(inputArray, result.list.toArray(new Integer[0]));

        pb = new ProxyBuilder(ClassWithCollection.class);
        pb.put("a", 123);
        pb.put("b", 456);
        inputList = Arrays.asList(1, 2, 3, 4, 5);
        pb.put("propertyList", inputList);

        ClassWithCollection result2 = (ClassWithCollection) pb.build();
        assertEquals(123, result2.a, 1e-10);
        assertEquals(456, result2.b, 1e-10);
        assertArrayEquals(inputList.toArray(), result2.propertyList.toArray());   
    }

    @Test
    @Ignore
    public void testImmutableTwoConstructorsWithSameArgNames() {
        ProxyBuilder pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        ImmutableClass result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertEquals(456, result.b, 1e-10);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456.1f);
        result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertEquals(456.1, result.b, 1e-10);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123.1f);
        pb.put("b", 456);
        result = (ImmutableClass) pb.build();
        assertEquals(123.1, result.a, 1e-10);
        assertEquals(456, result.b, 1e-10);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123.1f);
        pb.put("b", 456.1f);
        result = (ImmutableClass) pb.build();
        assertEquals(123.1, result.a, 1e-10);
        assertEquals(456.1, result.b, 1e-10);
    }

    @Test
    public void testPartiallyMutable() {
        ProxyBuilder pb = new ProxyBuilder(PartiallyMutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        pb.put("intValue", 1);
        pb.put("doubleValue", 1.2);
        pb.put("stringValue", "This is String");

        PartiallyMutableClass result = (PartiallyMutableClass) pb.build();
        assertEquals(123, result.a);
        assertEquals(456, result.b);
        assertEquals(1, result.intValue);
        assertEquals(1.2, result.doubleValue, 1e-5);
        assertEquals("This is String", result.stringValue);
    }

    private void assertColorEquals(Color expected, Paint actualPaint) {
        assertTrue(actualPaint instanceof Color);
        final Color actual = (Color) actualPaint;
        assertEquals(expected.getRed(), actual.getRed(), 1e-10);
        assertEquals(expected.getGreen(), actual.getGreen(), 1e-10);
        assertEquals(expected.getBlue(), actual.getBlue(), 1e-10);
        assertEquals(expected.getOpacity(), actual.getOpacity(), 1e-10);
    }

    @Test
    public void testDefaultValues() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithDefaultValues.class);
        ClassWithDefaultValues result = (ClassWithDefaultValues) pb.build();
        assertEquals(1, result.a, 1e-10);
        assertEquals(2, result.b, 1e-10);
        assertColorEquals(Color.RED, result.color);
        assertColorEquals(Color.GREEN, result.fill);
        assertEquals(StageStyle.DECORATED, result.stageStyle);

        pb = new ProxyBuilder(ClassWithDefaultValues.class);
        pb.put("a", 123);
        pb.put("color", Color.BLUE);
        result = (ClassWithDefaultValues) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertEquals(2, result.b, 1e-10);
        assertColorEquals(Color.BLUE, result.color);
        assertColorEquals(Color.GREEN, result.fill);
        assertEquals(StageStyle.DECORATED, result.stageStyle);

        //Integer[] inputArray = new Integer[] {1, 2, 3, 4, 5};
        //assertArrayEquals(inputArray, result.list.toArray(new Integer[0]));
    }

    @Test
    public void testImmutableWithValuesNotSet() {
        ProxyBuilder pb = new ProxyBuilder(ImmutableClass.class);
        ImmutableClass result = (ImmutableClass) pb.build();
        assertEquals(0, result.a, 1e-10);
        assertEquals(0, result.b, 1e-10);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("b", 123);
        result = (ImmutableClass) pb.build();
        assertEquals(0, result.a, 1e-10);
        assertEquals(123, result.b, 1e-10);
    }

    @Test
    public void testNonExistentProperties() {
        ProxyBuilder pb = new ProxyBuilder(PartiallyMutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        pb.put("intValue", 1);
        pb.put("doubleValue", 1.2);
        pb.put("stringValue", "This is String");
        pb.put("nonExistentValue", "This is non-existent String");

        try {
            PartiallyMutableClass result = (PartiallyMutableClass) pb.build();
            fail("expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        pb = new ProxyBuilder(ClassWithMixedConstructors.class);
        pb.put("a", 123);
        pb.put("b", 456);
        pb.put("c", "This is String C");
        pb.put("d", "This is String D");
        try {
            ClassWithMixedConstructors result = (ClassWithMixedConstructors) pb.build();
            fail("expected RuntimeException");
        } catch (RuntimeException ex) {
        }
    }

    @Test
    public void testReadOnlyList() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyCollection.class);
        pb.put("a", 123);

        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5);
        pb.put("propertyList", inputList);

        ClassWithReadOnlyCollection result = (ClassWithReadOnlyCollection) pb.build();
        assertEquals(123, result.a, 1e-10);
        assertArrayEquals(inputList.toArray(), result.propertyList.toArray());


        pb = new ProxyBuilder(ClassWithReadOnlyCollection.class);

        pb.put("propertyList", inputList);

        result = (ClassWithReadOnlyCollection) pb.build();
        assertArrayEquals(inputList.toArray(), result.propertyList.toArray());
    }
}
