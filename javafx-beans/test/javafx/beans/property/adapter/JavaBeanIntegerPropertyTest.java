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

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;

import java.beans.PropertyVetoException;

import static org.junit.Assert.assertEquals;

/**
 */
public class JavaBeanIntegerPropertyTest extends JavaBeanPropertyTestBase<Number> {

    private static final int[] VALUES = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};

    @Override
    protected BeanStub<Number> createBean(Number initialValue) {
        return new IntegerPOJO(initialValue.intValue());
    }

    @Override
    protected void check(Number actual, Number expected) {
        assertEquals(actual.intValue(), expected.intValue());
    }

    @Override
    protected Number getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected Property<Number> createObservable(Number value) {
        return new SimpleIntegerProperty(value.intValue());
    }

    @Override
    protected JavaBeanProperty<Number> extractProperty(Object bean) throws NoSuchMethodException {
        return JavaBeanIntegerPropertyBuilder.create().bean(bean).name("x").build();
    }

    public class IntegerPOJO extends BeanStub<Number> {
        private Integer x;
        private boolean failureMode;

        public IntegerPOJO(Integer x) {
            this.x = x;
        }

        public Integer getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        public void setX(Integer x) {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                this.x = x;
            }
        }

        @Override
        public Integer getValue() {
            return getX();
        }

        @Override
        public void setValue(Number value) throws PropertyVetoException {
            setX(value.intValue());
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
