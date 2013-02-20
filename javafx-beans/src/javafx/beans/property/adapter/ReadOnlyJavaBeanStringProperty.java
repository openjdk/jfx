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

package javafx.beans.property.adapter;

import com.sun.javafx.property.adapter.ReadOnlyPropertyDescriptor;
import javafx.beans.property.ReadOnlyStringPropertyBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import sun.misc.Cleaner;

import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;

import sun.reflect.misc.MethodUtil;

/**
 * A {@code ReadOnlyJavaBeanStringProperty} provides an adapter between a regular
 * read only Java Bean property of type {@code String} and a JavaFX 
 * {@code ReadOnlyStringProperty}. It cannot be created directly, but a 
 * {@link ReadOnlyJavaBeanStringPropertyBuilder} has to be used.
 * <p>
 * As a minimum, the Java Bean must implement a getter for the
 * property. If the getter of an instance of this class is called, the property of 
 * the Java Bean is returned. If the Java Bean property is bound (i.e. it supports
 * PropertyChangeListeners), this {@code ReadOnlyJavaBeanStringProperty} will be 
 * aware of changes in the Java Bean. Otherwise it can be notified about
 * changes by calling {@link #fireValueChangedEvent()}.
 * 
 * @see javafx.beans.property.ReadOnlyStringProperty
 * @see ReadOnlyJavaBeanStringPropertyBuilder
 */
public final class ReadOnlyJavaBeanStringProperty extends ReadOnlyStringPropertyBase implements ReadOnlyJavaBeanProperty<String> {

    private final ReadOnlyPropertyDescriptor descriptor;
    private final ReadOnlyPropertyDescriptor.ReadOnlyListener<String> listener;

    private final AccessControlContext acc = AccessController.getContext();

    ReadOnlyJavaBeanStringProperty(ReadOnlyPropertyDescriptor descriptor, Object bean) {
        this.descriptor = descriptor;
        this.listener = descriptor.new ReadOnlyListener<String>(bean, this);
        descriptor.addListener(listener);
        Cleaner.create(this, new Runnable() {
            @Override
            public void run() {
                ReadOnlyJavaBeanStringProperty.this.descriptor.removeListener(listener);
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UndeclaredThrowableException if calling the getter of the Java Bean
     * property throws an {@code IllegalAccessException} or an 
     * {@code InvocationTargetException}.
     */
    @Override
    public String get() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                try {
                    return (String)MethodUtil.invoke(descriptor.getGetter(), getBean(), (Object[])null);
                } catch (IllegalAccessException e) {
                    throw new UndeclaredThrowableException(e);
                } catch (InvocationTargetException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        }, acc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getBean() {
        return listener.getBean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return descriptor.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireValueChangedEvent() {
        super.fireValueChangedEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        descriptor.removeListener(listener);
    }
}
