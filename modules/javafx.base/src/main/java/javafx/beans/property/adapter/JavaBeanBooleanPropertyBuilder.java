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

package javafx.beans.property.adapter;

import com.sun.javafx.property.adapter.JavaBeanPropertyBuilderHelper;
import com.sun.javafx.property.adapter.PropertyDescriptor;

import java.lang.reflect.Method;

/**
 * A {@code JavaBeanBooleanPropertyBuilder} can be used to create
 * {@link JavaBeanBooleanProperty JavaBeanBooleanProperties}. To create
 * a {@code JavaBeanBooleanProperty} one first has to call {@link #create()}
 * to generate a builder, set the required properties, and then one can
 * call {@link #build()} to generate the property.
 * <p>
 * Not all properties of a builder have to specified, there are several
 * combinations possible. As a minimum the {@link #name(java.lang.String)} of
 * the property and the {@link #bean(java.lang.Object)} have to be specified.
 * If the names of the getter and setter follow the conventions, this is sufficient.
 * Otherwise it is possible to specify an alternative name for the getter and setter
 * ({@link #getter(java.lang.String)} and {@link #setter(java.lang.String)}) or
 * the getter and setter {@code Methods} directly ({@link #getter(java.lang.reflect.Method)}
 * and {@link #setter(java.lang.reflect.Method)}).
 * <p>
 * All methods to change properties return a reference to this builder, to enable
 * method chaining.
 * <p>
 * If you have to generate adapters for the same property of several instances
 * of the same class, you can reuse a {@code JavaBeanBooleanPropertyBuilder}
 * by switching the Java Bean instance (with {@link #bean(java.lang.Object)} and
 * calling {@link #build()}.
 *
 * @see JavaBeanBooleanProperty
 * @since JavaFX 2.1
 */
public final class JavaBeanBooleanPropertyBuilder {

    private final JavaBeanPropertyBuilderHelper<Boolean> helper = new JavaBeanPropertyBuilderHelper<>();

    private JavaBeanBooleanPropertyBuilder() {}

    /**
     * Creates a new instance of {@code JavaBeanBooleanPropertyBuilder}.
     *
     * @return the new {@code JavaBeanBooleanPropertyBuilder}
     */
    public static JavaBeanBooleanPropertyBuilder create() {
        return new JavaBeanBooleanPropertyBuilder();
    }

    /**
     * Generates a new {@link JavaBeanBooleanProperty} with the current settings.
     *
     * @return the new {@code JavaBeanBooleanProperty}
     * @throws NoSuchMethodException if the settings were not sufficient to find
     * the getter and the setter of the Java Bean property
     * @throws IllegalArgumentException if the Java Bean property is not of type
     * {@code boolean} or {@code Boolean}
     */
    public JavaBeanBooleanProperty build() throws NoSuchMethodException {
        final PropertyDescriptor<Boolean> descriptor = helper.getDescriptor();
        if (!boolean.class.equals(descriptor.getType()) && !Boolean.class.equals(descriptor.getType())) {
            throw new IllegalArgumentException("Not a boolean property");
        }
        return new JavaBeanBooleanProperty(descriptor, helper.getBean());
    }

    /**
     * Sets the name of the property.
     *
     * @param name the name of the property
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder name(String name) {
        helper.name(name);
        return this;
    }

    /**
     * Sets the Java Bean instance the adapter should connect to.
     *
     * @param bean the Java Bean instance
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder bean(Object bean) {
        helper.bean(bean);
        return this;
    }

    /**
     * Sets the Java Bean class in which the getter and setter should be searched.
     * This can be useful if the builder should generate adapters for several
     * Java Beans of different types.
     *
     * @param beanClass the Java Bean class
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder beanClass(Class<?> beanClass) {
        helper.beanClass(beanClass);
        return this;
    }

    /**
     * Sets an alternative name for the getter. This can be omitted if the
     * name of the getter follows Java Bean naming conventions.
     *
     * @param getter the alternative name of the getter
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder getter(String getter) {
        helper.getterName(getter);
        return this;
    }

    /**
     * Sets an alternative name for the setter. This can be omitted if the
     * name of the setter follows Java Bean naming conventions.
     *
     * @param setter the alternative name of the setter
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder setter(String setter) {
        helper.setterName(setter);
        return this;
    }

    /**
     * Sets the getter method directly. This can be omitted if the
     * name of the getter follows Java Bean naming conventions.
     *
     * @param getter the getter
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder getter(Method getter) {
        helper.getter(getter);
        return this;
    }

    /**
     * Sets the setter method directly. This can be omitted if the
     * name of the setter follows Java Bean naming conventions.
     *
     * @param setter the setter
     * @return a reference to this builder to enable method chaining
     */
    public JavaBeanBooleanPropertyBuilder setter(Method setter) {
        helper.setter(setter);
        return this;
    }
}
