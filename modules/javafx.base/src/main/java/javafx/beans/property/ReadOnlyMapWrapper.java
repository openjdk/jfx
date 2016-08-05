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

import javafx.collections.ObservableMap;
import static javafx.collections.MapChangeListener.Change;

/**
 * This class provides a convenient class to define read-only properties. It
 * creates two properties that are synchronized. One property is read-only
 * and can be passed to external users. The other property is read- and
 * writable and should be used internally only.
 *
 * @since JavaFX 2.1
 */
public class ReadOnlyMapWrapper<K, V> extends SimpleMapProperty<K, V> {

    private ReadOnlyPropertyImpl readOnlyProperty;

    /**
     * The constructor of {@code ReadOnlyMapWrapper}
     */
    public ReadOnlyMapWrapper() {
    }

    /**
     * The constructor of {@code ReadOnlyMapWrapper}
     *
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyMapWrapper(ObservableMap<K, V> initialValue) {
        super(initialValue);
    }

    /**
     * The constructor of {@code ReadOnlyMapWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlyMapWrapper}
     * @param name
     *            the name of this {@code ReadOnlyMapWrapper}
     */
    public ReadOnlyMapWrapper(Object bean, String name) {
        super(bean, name);
    }

    /**
     * The constructor of {@code ReadOnlyMapWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlyMapWrapper}
     * @param name
     *            the name of this {@code ReadOnlyMapWrapper}
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyMapWrapper(Object bean, String name,
                              ObservableMap<K, V> initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Returns the readonly property, that is synchronized with this
     * {@code ReadOnlyMapWrapper}.
     *
     * @return the readonly property
     */
    public ReadOnlyMapProperty<K, V> getReadOnlyProperty() {
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireValueChangedEvent(Change<? extends K, ? extends V> change) {
        super.fireValueChangedEvent(change);
        if (readOnlyProperty != null) {
            readOnlyProperty.fireValueChangedEvent(change);
        }
    }

    private class ReadOnlyPropertyImpl extends ReadOnlyMapPropertyBase<K, V> {

        @Override
        public ObservableMap<K, V> get() {
            return ReadOnlyMapWrapper.this.get();
        }

        @Override
        public Object getBean() {
            return ReadOnlyMapWrapper.this.getBean();
        }

        @Override
        public String getName() {
            return ReadOnlyMapWrapper.this.getName();
        }

        @Override
        public ReadOnlyIntegerProperty sizeProperty() {
            return ReadOnlyMapWrapper.this.sizeProperty();
        }

        @Override
        public ReadOnlyBooleanProperty emptyProperty() {
            return ReadOnlyMapWrapper.this.emptyProperty();
        }
    }
}
