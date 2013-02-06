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
public class ReadOnlyJavaBeanBooleanProperty_prim_Test extends ReadOnlyJavaBeanPropertyTestBase<Boolean> {

    private final static Boolean[] VALUES = new Boolean[] {true, false};

    @Override
    protected BeanStub<Boolean> createBean(Boolean initialValue) {
        return new BooleanPOJO(initialValue);
    }

    @Override
    protected void check(Boolean actual, Boolean expected) {
        assertEquals(actual, expected);
    }

    @Override
    protected Boolean getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected ReadOnlyJavaBeanProperty<Boolean> extractProperty(Object bean) throws NoSuchMethodException {
        return ReadOnlyJavaBeanBooleanPropertyBuilder.create().bean(bean).name("x").build();
    }

    public class BooleanPOJO extends BeanStub<Boolean> {
        private boolean x;
        private boolean failureMode;

        public BooleanPOJO(boolean x) {
            this.x = x;
        }

        public boolean isX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        @Override
        public Boolean getValue() {
            return isX();
        }

        @Override
        public void setValue(Boolean value) throws PropertyVetoException {
            this.x = value;
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
