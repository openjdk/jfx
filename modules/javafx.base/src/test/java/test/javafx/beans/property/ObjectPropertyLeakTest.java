/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.property;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectPropertyLeakTest {

    private static final int OBJ_COUNT = 10;

    private final ArrayList<Property<?>> origList = new ArrayList<>();
    private final ArrayList<Property<?>> wrappedList = new ArrayList<>();

    private final ArrayList<WeakReference<Property<?>>> origRefs = new ArrayList<>();
    private final ArrayList<WeakReference<Property<?>>> wrappedRefs = new ArrayList<>();

    private void checkRefs(String name, int numExpected,
            ArrayList<WeakReference<Property<?>>> refs) {

        int count = 0;
        for (var ref : refs) {
            if (ref.get() != null) count++;
        }
        final String msg = name + " properties should "
                + (numExpected > 0 ? "NOT be GCed" : "be GCed");
        assertEquals(msg, numExpected, count);
    }

    private void commonLeakTest(int origExpected, int wrappedExpected)
            throws Exception {

        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
        }

        checkRefs("Original", origExpected, origRefs);
        checkRefs("Wrapped", wrappedExpected, wrappedRefs);
    }

    private void commonLeakTest() throws Exception {
        // Verify that we hold references to both original and wrapped objects
        commonLeakTest(OBJ_COUNT, OBJ_COUNT);

        // Clear references to wrapped property objects and recheck
        wrappedList.clear();
        commonLeakTest(OBJ_COUNT, 0);

        // Clear references to original property objects and recheck
        origList.clear();
        commonLeakTest(0, 0);
    }

    private void saveRefs(Property<?> origProp, Property<?> wrappedProp) {
        // Save reference to original and wrapped objects
        origList.add(origProp);
        wrappedList.add(wrappedProp);

        // Save weak references for GC detection
        origRefs.add(new WeakReference<>(origProp));
        wrappedRefs.add(new WeakReference<>(wrappedProp));
    }

    @Test
    public void testBooleanPropertyAsObjectLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final BooleanProperty origProp = new SimpleBooleanProperty(true);
            final ObjectProperty<Boolean> wrappedProp = origProp.asObject();
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testObjectToBooleanLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final ObjectProperty<Boolean> origProp = new SimpleObjectProperty<>(true);
            final BooleanProperty wrappedProp = BooleanProperty.booleanProperty(origProp);
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testDoublePropertyAsObjectLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final DoubleProperty origProp = new SimpleDoubleProperty(1.0);
            final ObjectProperty<Double> wrappedProp = origProp.asObject();
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testObjectToDoubleLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final ObjectProperty<Double> origProp = new SimpleObjectProperty<>(1.0);
            final DoubleProperty wrappedProp = DoubleProperty.doubleProperty(origProp);
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testFloatPropertyAsObjectLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final FloatProperty origProp = new SimpleFloatProperty(1.0f);
            final ObjectProperty<Float> wrappedProp = origProp.asObject();
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testObjectToFloatLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final ObjectProperty<Float> origProp = new SimpleObjectProperty<>(1.0f);
            final FloatProperty wrappedProp = FloatProperty.floatProperty(origProp);
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testIntegerPropertyAsObjectLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final IntegerProperty origProp = new SimpleIntegerProperty(1);
            final ObjectProperty<Integer> wrappedProp = origProp.asObject();
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testObjectToIntegerLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final ObjectProperty<Integer> origProp = new SimpleObjectProperty<>(1);
            final IntegerProperty wrappedProp = IntegerProperty.integerProperty(origProp);
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testLongPropertyAsObjectLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final LongProperty origProp = new SimpleLongProperty(1L);
            final ObjectProperty<Long> wrappedProp = origProp.asObject();
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

    @Test
    public void testObjectToLongLeak() throws Exception {
        for (int i = 0; i < OBJ_COUNT; i++) {
            // Create original and wrapped property objects
            final ObjectProperty<Long> origProp = new SimpleObjectProperty<>(1L);
            final LongProperty wrappedProp = LongProperty.longProperty(origProp);
            saveRefs(origProp, wrappedProp);
        }
        commonLeakTest();
    }

}
