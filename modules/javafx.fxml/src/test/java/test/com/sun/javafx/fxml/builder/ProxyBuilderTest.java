/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.fxml.builder;

import com.sun.javafx.fxml.builder.ProxyBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.NamedArg;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.StageStyle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

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
    public void testImmutableTwoConstructorsWithSameArgNames() {
        ProxyBuilder pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456);
        ImmutableClass result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-5);
        assertEquals(456, result.b, 1e-5);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123);
        pb.put("b", 456.1f);
        result = (ImmutableClass) pb.build();
        assertEquals(123, result.a, 1e-5);
        assertEquals(456.1, result.b, 1e-5);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123.1f);
        pb.put("b", 456);
        result = (ImmutableClass) pb.build();
        assertEquals(123.1, result.a, 1e-5);
        assertEquals(456, result.b, 1e-5);

        pb = new ProxyBuilder(ImmutableClass.class);
        pb.put("a", 123.1f);
        pb.put("b", 456.1f);
        result = (ImmutableClass) pb.build();
        assertEquals(123.1, result.a, 1e-5);
        assertEquals(456.1, result.b, 1e-5);
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

    @Test
    public void testContainsKeyForListProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithCollection.class);
        assertTrue(pb.containsKey("list"),
                "containsKey should be true for a getter-backed List property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyForObservableListProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithCollection.class);
        assertTrue(pb.containsKey("propertyList"),
                "containsKey should be true for a getter-backed ObservableList property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyForReadOnlySetProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlySet.class);
        assertTrue(pb.containsKey("properties"),
                "containsKey should be true for a getter-backed Set property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyForReadOnlyObservableSetProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlySet.class);
        assertTrue(pb.containsKey("observableProperties"),
                "containsKey should be true for a getter-backed ObservableSet property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyForReadOnlyMapProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyMap.class);
        assertTrue(pb.containsKey("properties"),
                "containsKey should be true for a getter-backed Map property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyForReadOnlyObservableMapProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyMap.class);
        assertTrue(pb.containsKey("observableProperties"),
                "containsKey should be true for a getter-backed ObservableMap property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyFalseWhenNoMapGetter() {
        ProxyBuilder pb = new ProxyBuilder(ImmutableClass.class);
        assertFalse(pb.containsKey("properties"),
                "containsKey should be false when the class has no getProperties()");
    }

    @Test
    public void testReadOnlyMapWithNamedArg() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyMap.class);
        pb.put("label", "hello");

        // Simulate what FXMLLoader does: obtain the container and add entries.
        Map<String, Object> container = (Map<String, Object>) pb.get("properties");
        container.put("myKey", "myValue");
        container.put("otherKey", 42);

        ClassWithReadOnlyMap result = (ClassWithReadOnlyMap) pb.build();

        assertEquals("hello", result.label, "NamedArg value must be set");
        assertEquals("myValue", result.getProperties().get("myKey"),
                "Map entry 'myKey' must be transferred to the built object");
        assertEquals(42, result.getProperties().get("otherKey"),
                "Map entry 'otherKey' must be transferred to the built object");
    }

    @Test
    public void testArrayListUnwrapsFirstElementForScalarArg() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithPlainListAndScalarArg.class);

        List<String> container = (List<String>) pb.get("child");
        container.add("expectedValue");

        ClassWithPlainListAndScalarArg result =
                (ClassWithPlainListAndScalarArg) pb.build();

        assertEquals("expectedValue", result.child,
                "ProxyBuilder must unwrap the first temporary ArrayList element "
                + "and pass it as the scalar @NamedArg constructor argument");
    }

    @Test
    public void testHashSetUnwrapsFirstElementForScalarArg() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithPlainSetAndScalarArg.class);

        // Simulate what FXMLLoader does: containsKey triggers wrapper creation,
        // get() returns it, and then the child element is added to the container.
        assertTrue(pb.containsKey("child"),
                "containsKey must be true for the plain-List getter");

        Set<String> container = (Set<String>) pb.get("child");
        container.add("expectedValue");

        ClassWithPlainSetAndScalarArg result = (ClassWithPlainSetAndScalarArg) pb.build();

        assertEquals("expectedValue", result.child,
                "ProxyBuilder must unwrap first ArrayList item for @NamedArg scalar arg");
    }

    @Test
    public void testContainsKeyForReadOnlyObservableIntegerArrayProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyObservableArrays.class);
        assertTrue(pb.containsKey("intArray"),
                "containsKey should be true for a getter-backed ObservableIntegerArray property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testContainsKeyForReadOnlyObservableFloatArrayProperty() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyObservableArrays.class);
        assertTrue(pb.containsKey("floatArray"),
                "containsKey should be true for a getter-backed ObservableFloatArray property");
        assertFalse(pb.containsKey("nonExistent"),
                "containsKey should be false when no getter exists");
    }

    @Test
    public void testReadOnlyObservableIntegerArray() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyObservableArrays.class);

        List<Integer> container = (List<Integer>) pb.get("intArray");
        container.add(10);
        container.add(20);
        container.add(30);

        ClassWithReadOnlyObservableArrays result = (ClassWithReadOnlyObservableArrays) pb.build();

        ObservableIntegerArray intArray = result.getIntArray();
        assertEquals(3, intArray.size(), "ObservableIntegerArray should contain 3 elements");
        int[] actual = new int[intArray.size()];
        intArray.toArray(actual);
        assertArrayEquals(new int[]{10, 20, 30}, actual);
    }

    @Test
    public void testReadOnlyObservableFloatArray() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyObservableArrays.class);

        List<Float> container = (List<Float>) pb.get("floatArray");
        container.add(1.5f);
        container.add(2.5f);
        container.add(3.5f);

        ClassWithReadOnlyObservableArrays result = (ClassWithReadOnlyObservableArrays) pb.build();

        ObservableFloatArray floatArray = result.getFloatArray();
        assertEquals(3, floatArray.size(), "ObservableFloatArray should contain 3 elements");
        float[] actual = new float[floatArray.size()];
        floatArray.toArray(actual);
        assertArrayEquals(new float[]{1.5f, 2.5f, 3.5f}, actual, 1e-5f);
    }

    @Test
    public void testReadOnlyObservableIntegerArrayWithNamedArg() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyObservableArrays.class);
        pb.put("label", "testLabel");

        List<Integer> container = (List<Integer>) pb.get("intArray");
        container.add(7);
        container.add(8);

        ClassWithReadOnlyObservableArrays result = (ClassWithReadOnlyObservableArrays) pb.build();

        assertEquals("testLabel", result.label, "NamedArg value must be set");
        ObservableIntegerArray intArray = result.getIntArray();
        assertEquals(2, intArray.size(), "ObservableIntegerArray should contain 2 elements");
        int[] actual = new int[intArray.size()];
        intArray.toArray(actual);
        assertArrayEquals(new int[]{7, 8}, actual);
    }

    @Test
    public void testReadOnlyObservableFloatArrayWithNamedArg() {
        ProxyBuilder pb = new ProxyBuilder(ClassWithReadOnlyObservableArrays.class);
        pb.put("label", "testLabel");

        List<Float> container = (List<Float>) pb.get("floatArray");
        container.add(0.1f);
        container.add(0.2f);

        ClassWithReadOnlyObservableArrays result = (ClassWithReadOnlyObservableArrays) pb.build();

        assertEquals("testLabel", result.label, "NamedArg value must be set");
        ObservableFloatArray floatArray = result.getFloatArray();
        assertEquals(2, floatArray.size(), "ObservableFloatArray should contain 2 elements");
        float[] actual = new float[floatArray.size()];
        floatArray.toArray(actual);
        assertArrayEquals(new float[]{0.1f, 0.2f}, actual, 1e-5f);
    }

    @Test
    public void testShortMethodNames() {
        new ProxyBuilder<>(ShortMethodNames.class);
    }

    public static class ShortMethodNames {
        public void get() {}
        public void set() {}
    }

    public static class SameArgNames {
        public boolean intIntCalled;
        public boolean doubleDoubleCalled;

        public SameArgNames(@NamedArg("a") int a, @NamedArg("b") int b) {
            intIntCalled = true;
        }
        public SameArgNames(@NamedArg("a") double a, @NamedArg("b") double b) {
            doubleDoubleCalled = true;
        }
        public void setC(int c) {}
    }

    public static class SameArgNames2 {
        public boolean intIntCalled;
        public boolean doubleDoubleCalled;

        public SameArgNames2(@NamedArg("a") double a, @NamedArg("b") double b) {
            doubleDoubleCalled = true;
        }
        public SameArgNames2(@NamedArg("a") int a, @NamedArg("b") int b) {
            intIntCalled = true;
        }
        public void setC(int c) {}
    }

    private Object createObject(Class clazz, String a, String b, String c) {
        ProxyBuilder pb = new ProxyBuilder(clazz);
        pb.put("a", a);
        pb.put("b", b);
        if (c != null) {
            pb.put("c", c);
        }
        return pb.build();
    }

    private void check_JDK_8146325(String a, String b, String c, boolean resInt, boolean resDouble) {
        SameArgNames result = (SameArgNames) createObject(SameArgNames.class, a, b, c);
        assertEquals(resInt, result.intIntCalled);
        assertEquals(resDouble, result.doubleDoubleCalled);

        SameArgNames2 result2 = (SameArgNames2) createObject(SameArgNames2.class, a, b, c);
        assertEquals(resInt, result2.intIntCalled);
        assertEquals(resDouble, result2.doubleDoubleCalled);
    }

    @Test
    public void test_JDK_8146325_IntExact() {
        check_JDK_8146325("123", "456", null, true, false);
    }

    @Test
    public void test_JDK_8146325_IntSetter() {
        check_JDK_8146325("123", "456", "789", true, false);
    }

    @Test
    public void test_JDK_8146325_DoubleExact() {
        check_JDK_8146325("123", "456.1", null, false, true);
    }

    @Test
    public void test_JDK_8146325_DoubleSetter() {
        check_JDK_8146325("123", "456.1", "789", false, true);
    }
}
