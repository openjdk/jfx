/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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
public class ReadOnlyObjectWrapper<T> extends SimpleObjectProperty<T> {

    private ReadOnlyPropertyImpl readOnlyProperty;

    /**
     * The constructor of {@code ReadOnlyObjectWrapper}
     */
    public ReadOnlyObjectWrapper() {
    }

    /**
     * The constructor of {@code ReadOnlyObjectWrapper}
     *
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyObjectWrapper(T initialValue) {
        super(initialValue);
    }

    /**
     * The constructor of {@code ReadOnlyObjectWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlyObjectProperty}
     * @param name
     *            the name of this {@code ReadOnlyObjectProperty}
     */
    public ReadOnlyObjectWrapper(Object bean, String name) {
        super(bean, name);
    }

    /**
     * The constructor of {@code ReadOnlyObjectWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlyObjectProperty}
     * @param name
     *            the name of this {@code ReadOnlyObjectProperty}
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyObjectWrapper(Object bean, String name, T initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Returns the readonly property, that is synchronized with this
     * {@code ReadOnlyObjectWrapper}.
     *
     * @return the readonly property
     */
    public ReadOnlyObjectProperty<T> getReadOnlyProperty() {
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

    private class ReadOnlyPropertyImpl extends ReadOnlyObjectPropertyBase<T> {

        @Override
        public T get() {
            return ReadOnlyObjectWrapper.this.get();
        }

        @Override
        public Object getBean() {
            return ReadOnlyObjectWrapper.this.getBean();
        }

        @Override
        public String getName() {
            return ReadOnlyObjectWrapper.this.getName();
        }
    }
}
