/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

/**
 * This class provides a convenient class to define read-only properties. It
 * creates two properties that are synchronized. One property is read-only
 * and can be passed to external users. The other property is read- and
 * writable and should be used internally only.
 *
 * @since JavaFX 2.0
 */
public class ReadOnlyDoubleWrapper extends SimpleDoubleProperty {

    private ReadOnlyPropertyImpl readOnlyProperty;

    /**
     * The constructor of {@code ReadOnlyDoubleWrapper}
     */
    public ReadOnlyDoubleWrapper() {
    }

    /**
     * The constructor of {@code ReadOnlyDoubleWrapper}
     *
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyDoubleWrapper(double initialValue) {
        super(initialValue);
    }

    /**
     * The constructor of {@code ReadOnlyDoubleWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlyDoubleProperty}
     * @param name
     *            the name of this {@code ReadOnlyDoubleProperty}
     */
    public ReadOnlyDoubleWrapper(Object bean, String name) {
        super(bean, name);
    }

    /**
     * The constructor of {@code ReadOnlyDoubleWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlyDoubleProperty}
     * @param name
     *            the name of this {@code ReadOnlyDoubleProperty}
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyDoubleWrapper(Object bean, String name,
            double initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Returns the read-only property, that is synchronized with this
     * {@code ReadOnlyDoubleWrapper}.
     *
     * @return the read-only property
     */
    public ReadOnlyDoubleProperty getReadOnlyProperty() {
        if (readOnlyProperty == null) {
            readOnlyProperty = new ReadOnlyPropertyImpl();
        }
        return readOnlyProperty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireValueChangedEvent() {
        super.fireValueChangedEvent();
        if (readOnlyProperty != null) {
            readOnlyProperty.fireValueChangedEvent();
        }
    }

    private class ReadOnlyPropertyImpl extends ReadOnlyDoublePropertyBase {

        @Override
        public double get() {
            return ReadOnlyDoubleWrapper.this.get();
        }

        @Override
        public Object getBean() {
            return ReadOnlyDoubleWrapper.this.getBean();
        }

        @Override
        public String getName() {
            return ReadOnlyDoubleWrapper.this.getName();
        }
    };
}
