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

import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;

import java.beans.PropertyVetoException;

import static org.junit.Assert.assertEquals;

/**
 */
public class JavaBeanDoublePropertyTest extends JavaBeanPropertyTestBase<Number> {

    private static final double EPSILON = 1e-12;
    
    private static final double[] VALUES = new double[] {Math.PI, -Math.E};

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
    protected Property<Number> createObservable(Number value) {
        return new SimpleDoubleProperty(value.doubleValue());
    }

    @Override
    protected JavaBeanProperty<Number> extractProperty(Object bean) throws NoSuchMethodException {
        return JavaBeanDoublePropertyBuilder.create().bean(bean).name("x").build();
    }

    public class DoublePOJO extends BeanStub<Number> {
        private Double x;
        private boolean failureMode;

        public DoublePOJO(Double x) {
            this.x = x;
        }

        public Double getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        public void setX(Double x) {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                this.x = x;
            }
        }

        @Override
        public Double getValue() {
            return getX();
        }

        @Override
        public void setValue(Number value) throws PropertyVetoException {
            setX(value.doubleValue());
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
