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

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

import com.sun.javafx.binding.ExpressionHelper;

/**
 * This class provides a convenient class to define read-only properties. It
 * creates two properties that are synchronized. One property is read-only
 * and can be passed to external users. The other property is read- and
 * writable and should be used internally only.
 * 
 */
public class ReadOnlyFloatWrapper extends SimpleFloatProperty {

    private ReadOnlyPropertyImpl readOnlyProperty;

    /**
     * The constructor of {@code ReadOnlyFloatWrapper}
     */
    public ReadOnlyFloatWrapper() {
    }

    /**
     * The constructor of {@code ReadOnlyFloatWrapper}
     * 
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyFloatWrapper(float initialValue) {
        super(initialValue);
    }

    /**
     * The constructor of {@code ReadOnlyFloatWrapper}
     * 
     * @param bean
     *            the bean of this {@code ReadOnlyFloatProperty}
     * @param name
     *            the name of this {@code ReadOnlyFloatProperty}
     */
    public ReadOnlyFloatWrapper(Object bean, String name) {
        super(bean, name);
    }

    /**
     * The constructor of {@code ReadOnlyFloatWrapper}
     * 
     * @param bean
     *            the bean of this {@code ReadOnlyFloatProperty}
     * @param name
     *            the name of this {@code ReadOnlyFloatProperty}
     * @param initialValue
     *            the initial value of the wrapped value
     */
    public ReadOnlyFloatWrapper(Object bean, String name, float initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Returns the readonly property, that is synchronized with this
     * {@code ReadOnlyFloatWrapper}.
     * 
     * @return the readonly property
     */
    public ReadOnlyFloatProperty getReadOnlyProperty() {
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
    public void addListener(ChangeListener<? super Number> listener) {
        getReadOnlyProperty().addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(ChangeListener<? super Number> listener) {
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

    private class ReadOnlyPropertyImpl extends ReadOnlyFloatProperty {
        
        private ExpressionHelper<Number> helper = null;
        
        @Override
        public float get() {
            return ReadOnlyFloatWrapper.this.get();
        }

        @Override 
        public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override 
        public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }
        
        @Override
        public void addListener(ChangeListener<? super Number> listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override 
        public void removeListener(ChangeListener<? super Number> listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }
        
        protected void fireValueChangedEvent() {
            ExpressionHelper.fireValueChangedEvent(helper);
        }
        
        @Override
        public Object getBean() {
            return ReadOnlyFloatWrapper.this.getBean();
        }

        @Override
        public String getName() {
            return ReadOnlyFloatWrapper.this.getName();
        }
    };
}
