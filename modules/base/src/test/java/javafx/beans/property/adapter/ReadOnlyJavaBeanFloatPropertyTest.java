/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property.adapter;

import java.beans.PropertyVetoException;

import static org.junit.Assert.assertEquals;

/**
 */
public class ReadOnlyJavaBeanFloatPropertyTest extends ReadOnlyJavaBeanPropertyTestBase<Number> {

    private static final float EPSILON = 1e-6f;

    private final static Float[] VALUES = new Float[] {(float)Math.PI, (float)-Math.E};

    @Override
    protected BeanStub<Number> createBean(Number initialValue) {
        return new FloatPOJO(initialValue.floatValue());
    }

    @Override
    protected void check(Number actual, Number expected) {
        assertEquals(actual.floatValue(), expected.floatValue(), EPSILON);
    }

    @Override
    protected Number getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected ReadOnlyJavaBeanProperty<Number> extractProperty(Object bean) throws NoSuchMethodException {
        return ReadOnlyJavaBeanFloatPropertyBuilder.create().bean(bean).name("x").build();
    }

    public class FloatPOJO extends BeanStub<Number> {
        private Float x;
        private boolean failureMode;

        public FloatPOJO(Float x) {
            this.x = x;
        }

        public Float getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        @Override
        public Float getValue() {
            return getX();
        }

        @Override
        public void setValue(Number value) throws PropertyVetoException {
            this.x = value.floatValue();
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
