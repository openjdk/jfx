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
import javafx.beans.property.SimpleStringProperty;

import java.beans.PropertyVetoException;

import static org.junit.Assert.assertEquals;

/**
 */
public class JavaBeanStringPropertyTest extends JavaBeanPropertyTestBase<String> {

    private final static String[] VALUES = new String[] {"Hello World", "JavaFX is cool"};

    @Override
    protected BeanStub<String> createBean(String initialValue) {
        return new StringPOJO(initialValue);
    }

    @Override
    protected void check(String actual, String expected) {
        assertEquals(actual, expected);
    }

    @Override
    protected String getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected Property<String> createObservable(String value) {
        return new SimpleStringProperty(value);
    }

    @Override
    protected JavaBeanProperty<String> extractProperty(Object bean) throws NoSuchMethodException {
        return JavaBeanStringPropertyBuilder.create().bean(bean).name("x").build();
    }

    public class StringPOJO extends BeanStub<String> {
        private String x;
        private boolean failureMode;

        public StringPOJO(String x) {
            this.x = x;
        }

        public String getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        public void setX(String x) {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                this.x = x;
            }
        }

        @Override
        public String getValue() {
            return getX();
        }

        @Override
        public void setValue(String value) throws PropertyVetoException {
            setX(value);
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
