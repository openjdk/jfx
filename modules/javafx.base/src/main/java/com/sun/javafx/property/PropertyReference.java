/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.property;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javafx.beans.property.ReadOnlyProperty;

import com.sun.javafx.reflect.ReflectUtil;

/**
 * A handle to a specific property defined on some {@link Bean}.
 */
public final class PropertyReference<T> {
    private String name;
    private Method getter;
    private Method setter;
    private Method propertyGetter;
    private Class<?> clazz;
    private Class<?> type;
    private boolean reflected = false;

    // uses reflection to implement the get / set methods
    /**
     * Creates a new {@code PropertyReference} for a property of a bean.
     *
     * @param clazz
     *            The class of the {@link Bean} that contains the property
     * @param name
     *            The name of the property
     * @throws NullPointerException
     *             if {@code clazz} or {@code name} are null
     * @throws IllegalArgumentException
     *             if {@code name} is an empty {@code String}
     */
    public PropertyReference(Class<?> clazz, String name) {
        if (name == null)
            throw new NullPointerException("Name must be specified");
        if (name.trim().length() == 0)
            throw new IllegalArgumentException("Name must be specified");
        if (clazz == null)
            throw new NullPointerException("Class must be specified");
        ReflectUtil.checkPackageAccess(clazz);
        this.name = name;
        this.clazz = clazz;
    }

    /**
     * Can be used to determine if a property can be set.
     *
     * @return {@code true}, if the property can be set, {@code false} otherwise
     */
    public boolean isWritable() {
        reflect();
        return setter != null;
    }

    /**
     * Can be used to determine if a property can be get.
     *
     * @return {@code true}, if the property can be get, {@code false} otherwise
     */
    public boolean isReadable() {
        reflect();
        return getter != null;
    }

    /**
     * Can be used to determine if a property provides an implementation of
     * {@link javafx.beans.value.ObservableValue}.
     *
     * @return
     */
    public boolean hasProperty() {
        reflect();
        return propertyGetter != null;
    }

    /**
     * Returns the name of the property.
     *
     * @return name of the propery
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the class of the {@link Bean} that contains the property.
     *
     * @return the class of the {@link Bean}
     */
    public Class<?> getContainingClass() {
        return clazz;
    }

    /**
     * Returns the type of the property. This type is only evaluated correctly
     * if a getter or a setter was found.
     *
     * @return the type of the property
     */
    public Class<?> getType() {
        reflect();
        return type;
    }

    /**
     * Set the property to a new value.
     *
     * @param bean
     *            The {@link Bean} instance for which the property should be set
     * @param value
     *            The new value
     * @throws IllegalStateException
     *             if the property is not writable
     */
    public void set(Object bean, T value) {
        if (!isWritable())
            throw new IllegalStateException(
                    "Cannot write to readonly property " + name);
        assert setter != null;
        try {
            MethodHelper.invoke(setter, bean, new Object[] {value});
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the value of the property.
     *
     * @param bean
     *            The {@code Bean} instance for which the property should be
     *            read
     * @return The value of the property
     * @throws IllegalStateException
     *             if the property is not readable
     */
    @SuppressWarnings("unchecked")
    public T get(Object bean) {
        if (!isReadable())
            throw new IllegalStateException(
                    "Cannot read from unreadable property " + name);
        assert getter != null;
        try {
            return (T)MethodHelper.invoke(getter, bean, (Object[])null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the {@link javafx.beans.value.ObservableValue} implementation of the
     * property.
     *
     * @param bean
     *            The {@code Bean} instance for which the property should be
     *            read
     * @return The {@code ObservableValue} of the property
     * @throws IllegalStateException
     *             if the property does not provide an implementation
     */
    @SuppressWarnings("unchecked")
    public ReadOnlyProperty<T> getProperty(Object bean) {
        if (!hasProperty())
            throw new IllegalStateException("Cannot get property " + name);
        assert propertyGetter != null;
        try {
            return (ReadOnlyProperty<T>)MethodHelper.invoke(propertyGetter, bean, (Object[])null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }

    private void reflect() {
        // If both the getter and setter are null then we have not reflected
        // on this property before
        if (!reflected) {
            reflected = true;
            try {
                // Since we use it in several places, construct the
                // first-letter-capitalized version of name
                final String properName = name.length() == 1 ?
                        name.substring(0, 1).toUpperCase() :
                        Character.toUpperCase(name.charAt(0))
                        + name.substring(1);

                // Now look for the getter. It will be named either
                // "get" + name with the first letter of name
                // capitalized, or it will be named "is" + name with
                // the first letter of the name capitalized. However it
                // is only named with "is" as a prefix if the type is
                // boolean.
                type = null;
                // first we check for getXXX
                String getterName = "get" + properName;
                try {
                    final Method m = clazz.getMethod(getterName);
                    if (Modifier.isPublic(m.getModifiers())) {
                        getter = m;
                    }
                } catch (NoSuchMethodException ex) {
                    // This is a legitimate error
                }

                // Then if it wasn't found we look for isXXX
                if (getter == null) {
                    getterName = "is" + properName;
                    try {
                        final Method m = clazz.getMethod(getterName);
                        if (Modifier.isPublic(m.getModifiers())) {
                            getter = m;
                        }
                    } catch (NoSuchMethodException ex) {
                        // This is a legitimate error
                    }
                }

                // Now attempt to look for the setter. It is simply
                // "set" + name with the first letter of name
                // capitalized.
                final String setterName = "set" + properName;

                // If we found the getter, we can get the type
                // and the setter easily.
                if (getter != null) {
                    type = getter.getReturnType();
                    try {
                        final Method m = clazz.getMethod(setterName, type);
                        if (Modifier.isPublic(m.getModifiers())) {
                            setter = m;
                        }
                    } catch (NoSuchMethodException ex) {
                        // This is a legitimate error
                    }
                } else { // no getter found
                    final Method[] methods = clazz.getMethods();
                    for (final Method m : methods) {
                        final Class<?>[] parameters = m.getParameterTypes();
                        if (setterName.equals(m.getName())
                                && (parameters.length == 1)
                                && Modifier.isPublic(m.getModifiers()))
                        {
                            setter = m;
                            type = parameters[0];
                            break;
                        }
                    }
                }

                // Now attempt to look for the property-getter.
                final String propertyGetterName = name + "Property";
                try {
                    final Method m = clazz.getMethod(propertyGetterName);
                    if (Modifier.isPublic(m.getModifiers())) {
                        propertyGetter = m;
                    } else
                        propertyGetter = null;
                } catch (NoSuchMethodException ex) {
                    // This is a legitimate error
                }
            } catch (RuntimeException e) {
                System.err.println("Failed to introspect property " + name);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PropertyReference)) {
            return false;
        }
        final PropertyReference<?> other = (PropertyReference<?>) obj;
        if (this.name != other.name
                && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        if (this.clazz != other.clazz
                && (this.clazz == null || !this.clazz.equals(other.clazz))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 97 * hash + (this.clazz != null ? this.clazz.hashCode() : 0);
        return hash;
    }
}
