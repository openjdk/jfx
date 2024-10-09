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

package test.javafx.animation;


import com.sun.javafx.animation.KeyValueHelper;
import com.sun.javafx.animation.KeyValueType;
import javafx.animation.Interpolator;
import javafx.animation.KeyValue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableFloatValue;
import javafx.beans.value.WritableIntegerValue;
import javafx.beans.value.WritableLongValue;
import javafx.beans.value.WritableValue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KeyValueTest {

    private static final double EPSILON_DOUBLE = 1e-12;
    private static final float EPSILON_FLOAT = 1e-6f;

    private void assertKeyValue(KeyValueType type, WritableValue<?> target, Object endValue, Interpolator interpolator, KeyValue kv) {
        assertEquals(type, KeyValueHelper.getType(kv));
        assertEquals(target, kv.getTarget());
        assertEquals(endValue, kv.getEndValue());
        assertEquals(interpolator, kv.getInterpolator());
    }

    private void assertKeyValue(KeyValueType type, WritableValue<?> target, Interpolator interpolator, KeyValue kv) {
        assertEquals(type, KeyValueHelper.getType(kv));
        assertEquals(target, kv.getTarget());
        assertEquals(interpolator, kv.getInterpolator());
    }



    @Test
    public void testBooleanFactory_Interpolator() {
        final BooleanProperty v = new SimpleBooleanProperty();
        final KeyValue kv = new KeyValue(v, true, Interpolator.EASE_BOTH);
        assertKeyValue(KeyValueType.BOOLEAN, v, Boolean.TRUE, Interpolator.EASE_BOTH, kv);
    }

    @Test
    public void testBooleanFactory_Interpolator_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue(null, true, Interpolator.EASE_BOTH);
        });
    }

    @Test
    public void testBooleanFactory_Interpolator_NullInterpolator() {
        assertThrows(NullPointerException.class, () -> {
            final BooleanProperty v = new SimpleBooleanProperty();
            new KeyValue(v, true, null);
        });
    }



    @Test
    public void testBooleanFactory() {
        final BooleanProperty v = new SimpleBooleanProperty();
        final KeyValue kv = new KeyValue(v, true);
        assertKeyValue(KeyValueType.BOOLEAN, v, Boolean.TRUE, Interpolator.LINEAR, kv);
    }

    @Test
    public void testBooleanFactory_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue(null, true);
        });
    }



    @Test
    public void testDoubleFactory_Interpolator() {
        final DoubleProperty v = new SimpleDoubleProperty();
        final KeyValue kv = new KeyValue(v, Math.PI, Interpolator.EASE_BOTH);
        assertKeyValue(KeyValueType.DOUBLE, v, Interpolator.EASE_BOTH, kv);
        assertEquals(Math.PI, ((Number)kv.getEndValue()).doubleValue(), EPSILON_DOUBLE);
    }

    @Test
    public void testDoubleFactory_Interpolator_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue(null, Math.PI, Interpolator.EASE_BOTH);
        });
    }

    @Test
    public void testDoubleFactory_Interpolator_NullInterpolator() {
        assertThrows(NullPointerException.class, () -> {
            final DoubleProperty v = new SimpleDoubleProperty();
            new KeyValue(v, Math.PI, null);
        });
    }



    @Test
    public void testDoubleFactory() {
        final DoubleProperty v = new SimpleDoubleProperty();
        final KeyValue kv = new KeyValue(v, Math.E);
        assertKeyValue(KeyValueType.DOUBLE, v, Interpolator.LINEAR, kv);
        assertEquals(Math.E, ((Number)kv.getEndValue()).doubleValue(), EPSILON_DOUBLE);
    }

    @Test
    public void testDoubleFactory_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue(null, Math.E);
        });
    }



    @Test
    public void testFloatFactory_Interpolator() {
        final FloatProperty v = new SimpleFloatProperty();
        final KeyValue kv = new KeyValue(v, (float)Math.E, Interpolator.EASE_BOTH);
        assertKeyValue(KeyValueType.FLOAT, v, Interpolator.EASE_BOTH, kv);
        assertEquals((float)Math.E, ((Number)kv.getEndValue()).floatValue(), EPSILON_FLOAT);
    }

    @Test
    public void testFloatFactory_Interpolator_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue((WritableFloatValue)null, (float)Math.E, Interpolator.EASE_BOTH);
        });
    }

    @Test
    public void testFloatFactory_Interpolator_NullInterpolator() {
        assertThrows(NullPointerException.class, () -> {
            final FloatProperty v = new SimpleFloatProperty();
            new KeyValue(v, (float)Math.E, null);
        });
    }



    @Test
    public void testFloatFactory() {
        final FloatProperty v = new SimpleFloatProperty();
        final KeyValue kv = new KeyValue(v, (float)Math.PI);
        assertKeyValue(KeyValueType.FLOAT, v, Interpolator.LINEAR, kv);
        assertEquals((float)Math.PI, ((Number)kv.getEndValue()).floatValue(), EPSILON_FLOAT);
    }

    @Test
    public void testFloatFactory_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue((WritableFloatValue)null, (float)Math.PI);
        });
    }



    @Test
    public void testIntegerFactory_Interpolator() {
        final IntegerProperty v = new SimpleIntegerProperty();
        final KeyValue kv = new KeyValue(v, Integer.MAX_VALUE, Interpolator.EASE_BOTH);
        assertKeyValue(KeyValueType.INTEGER, v, Integer.MAX_VALUE, Interpolator.EASE_BOTH, kv);
    }

    @Test
    public void testIntegerFactory_Interpolator_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue((WritableIntegerValue)null, 1, Interpolator.EASE_BOTH);
        });
    }

    @Test
    public void testIntegerFactory_Interpolator_NullInterpolator() {
        assertThrows(NullPointerException.class, () -> {
            final IntegerProperty v = new SimpleIntegerProperty();
            new KeyValue(v, 1, null);
        });
    }



    @Test
    public void testIntegerFactory() {
        final IntegerProperty v = new SimpleIntegerProperty();
        final KeyValue kv = new KeyValue(v, Integer.MIN_VALUE);
        assertKeyValue(KeyValueType.INTEGER, v, Integer.MIN_VALUE, Interpolator.LINEAR, kv);
    }

    @Test
    public void testIntegerFactory_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue((WritableIntegerValue)null, Integer.MIN_VALUE);
        });
    }



    @Test
    public void testLongFactory_Interpolator() {
        final LongProperty v = new SimpleLongProperty();
        final KeyValue kv = new KeyValue(v, Long.MAX_VALUE, Interpolator.EASE_BOTH);
        assertKeyValue(KeyValueType.LONG, v, Long.MAX_VALUE, Interpolator.EASE_BOTH, kv);
    }

    @Test
    public void testLongFactory_Interpolator_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue((WritableLongValue)null, 1L, Interpolator.EASE_BOTH);
        });
    }

    @Test
    public void testLongFactory_Interpolator_NullInterpolator() {
        assertThrows(NullPointerException.class, () -> {
            final LongProperty v = new SimpleLongProperty();
            new KeyValue(v, 1L, null);
        });
    }



    @Test
    public void testLongFactory() {
        final LongProperty v = new SimpleLongProperty();
        final KeyValue kv = new KeyValue(v, Long.MIN_VALUE);
        assertKeyValue(KeyValueType.LONG, v, Long.MIN_VALUE, Interpolator.LINEAR, kv);
    }

    @Test
    public void testLongFactory_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue((WritableLongValue)null, Long.MIN_VALUE);
        });
    }



    @Test
    public void testObjectFactory_Interpolator() {
        final StringProperty v = new SimpleStringProperty();
        final KeyValue kv = new KeyValue(v, "Hello World", Interpolator.EASE_BOTH);
        assertKeyValue(KeyValueType.OBJECT, v, "Hello World", Interpolator.EASE_BOTH, kv);
    }

    @Test
    public void testObjectFactory_Interpolator_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue(null, "Hello World", Interpolator.EASE_BOTH);
        });
    }

    @Test
    public void testObjectFactory_Interpolator_NullInterpolator() {
        assertThrows(NullPointerException.class, () -> {
            final StringProperty v = new SimpleStringProperty();
            new KeyValue(v, "Hello World", null);
        });
    }



    @Test
    public void testObjectFactory() {
        final StringProperty v = new SimpleStringProperty();
        final KeyValue kv = new KeyValue(v, "Goodbye World");
        assertKeyValue(KeyValueType.OBJECT, v, "Goodbye World", Interpolator.LINEAR, kv);
    }

    @Test
    public void testObjectFactory_NullTarget() {
        assertThrows(NullPointerException.class, () -> {
            new KeyValue(null, "Goodbye World");
        });
    }

}
