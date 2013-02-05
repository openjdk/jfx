/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
public class ReadOnlyJavaBeanDoubleProperty_prim_Test extends ReadOnlyJavaBeanPropertyTestBase<Number> {

    private static final double EPSILON = 1e-12;

    private final static Double[] VALUES = new Double[] {Math.PI, -Math.E};

    @Override
    protected BeanStub<Number> createBean(Number initialValue) {
        return new DoublePOJO(initialValue.doubleValue());
    }

    @Override
    protected void check(Number actual, Number expected) {
        assertEquals(actual.doubleValue(), expected.doubleValue(), EPSILON);
    }

    @Override
    protected Number getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected ReadOnlyJavaBeanProperty<Number> extractProperty(Object bean) throws NoSuchMethodException {
        return ReadOnlyJavaBeanDoublePropertyBuilder.create().bean(bean).name("x").build();
    }

    public class DoublePOJO extends BeanStub<Number> {
        private double x;
        private boolean failureMode;

        public DoublePOJO(double x) {
            this.x = x;
        }

        public double getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        @Override
        public Double getValue() {
            return getX();
        }

        @Override
        public void setValue(Number value) throws PropertyVetoException {
            this.x = value.doubleValue();
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
