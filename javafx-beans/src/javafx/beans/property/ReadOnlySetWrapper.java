/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.SetExpressionHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import static javafx.collections.SetChangeListener.Change;

/**
 * This class provides a convenient class to define read-only properties. It
 * creates two properties that are synchronized. One property is read-only
 * and can be passed to external users. The other property is read- and
 * writable and should be used internally only.
 *
 */
public class ReadOnlySetWrapper<E> extends SimpleSetProperty<E> {

    private ReadOnlyPropertyImpl readOnlyProperty;

    /**
     * The constructor of {@code ReadOnlySetWrapper}
     */
    public ReadOnlySetWrapper() {
    }

    /**
     * The constructor of {@code ReadOnlySetWrapper}
     *
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlySetWrapper(ObservableSet<E> initialValue) {
        super(initialValue);
    }

    /**
     * The constructor of {@code ReadOnlySetWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlySetWrapper}
     * @param name
     *            the name of this {@code ReadOnlySetWrapper}
     */
    public ReadOnlySetWrapper(Object bean, String name) {
        super(bean, name);
    }

    /**
     * The constructor of {@code ReadOnlySetWrapper}
     *
     * @param bean
     *            the bean of this {@code ReadOnlySetWrapper}
     * @param name
     *            the name of this {@code ReadOnlySetWrapper}
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlySetWrapper(Object bean, String name,
                              ObservableSet<E> initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Returns the readonly property, that is synchronized with this
     * {@code ReadOnlySetWrapper}.
     *
     * @return the readonly property
     */
    public ReadOnlySetProperty<E> getReadOnlyProperty() {
        if (readOnlyProperty == null) {
            readOnlyProperty = new ReadOnlyPropertyImpl();
        }
        return readOnlyProperty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(InvalidationListener listener) {
        getReadOnlyProperty().addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(InvalidationListener listener) {
        if (readOnlyProperty != null) {
            readOnlyProperty.removeListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(ChangeListener<? super ObservableSet<E>> listener) {
        getReadOnlyProperty().addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(ChangeListener<? super ObservableSet<E>> listener) {
        if (readOnlyProperty != null) {
            readOnlyProperty.removeListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(SetChangeListener<? super E> listener) {
        getReadOnlyProperty().addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(SetChangeListener<? super E> listener) {
        if (readOnlyProperty != null) {
            readOnlyProperty.removeListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireValueChangedEvent() {
        if (readOnlyProperty != null) {
            readOnlyProperty.fireValueChangedEvent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireValueChangedEvent(Change<? extends E> change) {
        if (readOnlyProperty != null) {
            readOnlyProperty.fireValueChangedEvent(change);
        }
    }

    private class ReadOnlyPropertyImpl extends ReadOnlySetProperty<E> {

        private SetExpressionHelper<E> helper = null;

        @Override
        public ObservableSet<E> get() {
            return ReadOnlySetWrapper.this.get();
        }

        @Override
        public void addListener(InvalidationListener listener) {
            helper = SetExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            helper = SetExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public void addListener(ChangeListener<? super ObservableSet<E>> listener) {
            helper = SetExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(ChangeListener<? super ObservableSet<E>> listener) {
            helper = SetExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public void addListener(SetChangeListener<? super E> listener) {
            helper = SetExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(SetChangeListener<? super E> listener) {
            helper = SetExpressionHelper.removeListener(helper, listener);
        }

        private void fireValueChangedEvent() {
            SetExpressionHelper.fireValueChangedEvent(helper);
        }

        private void fireValueChangedEvent(Change<? extends E> change) {
            SetExpressionHelper.fireValueChangedEvent(helper, change);
        }

        @Override
        public Object getBean() {
            return ReadOnlySetWrapper.this.getBean();
        }

        @Override
        public String getName() {
            return ReadOnlySetWrapper.this.getName();
        }

        @Override
        public ReadOnlyIntegerProperty sizeProperty() {
            return ReadOnlySetWrapper.this.sizeProperty();
        }

        @Override
        public ReadOnlyBooleanProperty emptyProperty() {
            return ReadOnlySetWrapper.this.emptyProperty();
        }
    }
}
