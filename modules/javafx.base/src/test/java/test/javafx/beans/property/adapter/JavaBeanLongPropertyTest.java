/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.property.adapter;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;

import java.beans.PropertyVetoException;
import javafx.beans.property.adapter.JavaBeanLongPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 */
public class JavaBeanLongPropertyTest extends JavaBeanPropertyTestBase<Number> {

    private static final long[] VALUES = new long[] {Long.MAX_VALUE, Long.MIN_VALUE};

    @Override
    protected BeanStub<Number> createBean(Number initialValue) {
        return new LongPOJO(initialValue.longValue());
    }

    @Override
    protected void check(Number actual, Number expected) {
        assertEquals(actual.longValue(), expected.longValue());
    }

    @Override
    protected Number getValue(int index) {
        return VALUES[index];
    }

    @Override
    protected Property<Number> createObservable(Number value) {
        return new SimpleLongProperty(value.longValue());
    }

    @Override
    protected JavaBeanProperty<Number> extractProperty(Object bean) throws NoSuchMethodException {
        return JavaBeanLongPropertyBuilder.create().bean(bean).name("x").build();
    }

    public class LongPOJO extends BeanStub<Number> {
        private Long x;
        private boolean failureMode;

        public LongPOJO(Long x) {
            this.x = x;
        }

        public Long getX() {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                return x;
            }
        }

        public void setX(Long x) {
            if (failureMode) {
                throw new RuntimeException("FailureMode activated");
            } else {
                this.x = x;
            }
        }

        @Override
        public Long getValue() {
            return getX();
        }

        @Override
        public void setValue(Number value) throws PropertyVetoException {
            setX(value.longValue());
        }

        @Override
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}
