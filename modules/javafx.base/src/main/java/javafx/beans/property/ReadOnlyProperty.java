/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javafx.beans.value.ObservableValue;

/**
 * Generic interface that defines the methods common to all readable properties
 * independent of their type.
 *
 *
 * @param <T>
 *            the type of the wrapped value
 * @since JavaFX 2.0
 */
public interface ReadOnlyProperty<T> extends ObservableValue<T> {

    /**
     * Returns the {@code Object} that is associated with this property.
     * <p>
     * For instance properties, this is the object that contains the property.
     * For {@linkplain AttachedProperty attached properties}, it is the object to which the property is attached.
     * Is this property is not associated with an object, {@code null} is returned.
     *
     * @return the associated {@code Object} or {@code null}
     */
    Object getBean();

    /**
     * Returns the name of this property. If the property does not have a name,
     * this method returns an empty {@code String}.
     *
     * @return the name or an empty {@code String}
     */
    String getName();

    /**
     * Returns the {@code Class} in which this property was declared.
     * <p>
     * If this property is not associated with an object, {@code null} is returned.
     *
     * @return   the declaring class of this property, or {@code null}
     * @implSpec Implementations of {@linkplain AttachedProperty attached properties} must override this method
     *           and return the declaring class of the attached property, as the default implementation will not
     *           be able to discover the declaring class at runtime.
     * @implNote For instance properties, the default implementation uses reflection to search for a method
     *           with a signature compatible with {@code ReadOnlyProperty<?> <name>Property()}, where
     *           {@code <name>} is the name of the property as returned by the {@link #getName()} method.
     *           The return type must be a subtype of {@code ReadOnlyProperty<?>}. The class that declares
     *           such a method is the declaring class of the property.
     *           However, implementations are advised to override this method and return the declaring class
     *           directly, instead of relying on the reflective auto-discovery mechanism. Attached properties
     *           must always override this method.
     * @since 27
     */
    default Class<?> getDeclaringClass() {
        Object bean = getBean();
        if (bean == null) {
            return null;
        }

        String name = getName();
        if (name == null || name.isEmpty()) {
            return null;
        }

        Class<?> beanClass = bean.getClass();
        String propertyName = name + "Property";

        do {
            try {
                Method method = beanClass.getDeclaredMethod(propertyName);

                if ((method.getModifiers() & Modifier.STATIC) == 0
                        && ReadOnlyProperty.class.isAssignableFrom(method.getReturnType())) {
                    return beanClass;
                }
            } catch (NoSuchMethodException ignored) {
                // fall through
            }

            beanClass = beanClass.getSuperclass();
        } while (beanClass != null);

        return null;
    }
}
