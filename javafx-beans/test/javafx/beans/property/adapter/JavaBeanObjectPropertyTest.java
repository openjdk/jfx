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
import javafx.beans.property.SimpleObjectProperty;

import java.beans.PropertyVetoException;

import static org.junit.Assert.assertEquals;

/**
 */
public class JavaBeanObjectPropertyTest extends JavaBeanPropertyTestBase<Object> {

    private final static Object[] VALUES = new Object[] {new Object(), new Object()};

    @Override
    protected BeanStub<Object> createBean(Object initialValue) {
        return new ObjectPOJO(initialValue);
    }

    @Override
    protected void check(Object actual, Object expected) {
        assertEquals(actual, expected);
    }

    @Override
    protected Object getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected Property<Object> createObservable(Object value) {
        return new SimpleObjectProperty<Object>(value);
    }

    @Override
    protected JavaBeanProperty<Object> extractProperty(Object bean) throws NoSuchMethodException {
        return JavaBeanObjectPropertyBuilder.create().bean(bean).name("x").build();
    }

    public class ObjectPOJO extends BeanStub<Object> {
        private Object x;
        private boolean failureMode;

        public ObjectPOJO(Object x) {
            this.x = x;
        }

        public Object getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        public void setX(Object x) {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                this.x = x;
            }
        }

        @Override
        public Object getValue() {
            return getX();
        }

        @Override
        public void setValue(Object value) throws PropertyVetoException {
            setX(value);
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
