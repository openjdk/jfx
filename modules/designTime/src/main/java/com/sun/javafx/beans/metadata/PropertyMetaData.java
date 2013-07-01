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

package com.sun.javafx.beans.metadata;

import static com.sun.javafx.beans.metadata.Bean.COMPUTE;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Represents the meta-data for a JavaFX Bean property. A PropertyMetaData is
 * created by invoking one of the constructors. These constructors reflect on a
 * Class representing a JavaFX Bean and the Method that represents the public
 * instance getter and extracts and/or generates the meta-data for that
 * property. Some information, such as the actual name of the property, are
 * determined reflectively and cannot be customized. Most information however
 * can be customized either through the use of the {@link Property} annotation,
 * or through the use of one or more {@link java.util.ResourceBundle}s.
 * <p>
 * During the reflective process, this class will look first for a resource
 * bundle named after the bean, and then for a generic "resources"
 * ResourceBundle located in the same package as the bean, the same as with
 * the BeanMetaData. When looking up the displayName of a property in the
 * specific "[MyBean]Resources" bundle, we will look for
 * "[propertyName]-displayName", and when looking up a value in the package
 * level "resources" bundle, we will look for
 * "[MyBean].[propertyName]-displayName".
 * <p>
 * Likewise, for other attributes, we follow the same lookup scheme but instead
 * of MyBean-displayName we look for MyBean-shortDescription, and so on.
 * <p>
 * From the PropertyMetaData it is possible to get a reference to the getter,
 * setter, and property methods of the property. If a property is readonly,
 * the setter will be null. If a property is immutable, the setter and the
 * property method will both be null. The getter is never null, as only those
 * properties that have a public getter can have a PropertyMetaData created
 * for them.
 *
 * @author Richard
 */
public class PropertyMetaData extends MetaData {
    // TODO need to add the ability to reference a custom PropertyEditor!

    /**
     * Defines the mutability of a property, which can either be immutable,
     * read-only, or writable.
     */
    public enum Mutability {
        IMMUTABLE,
        READ_ONLY,
        WRITABLE
    }

    /**
     * A reference to the getter for this property. This must not be null.
     */
    private Method getter;
    
    /**
     * A reference to the setter for this property. If this is set, then there
     * is a public setter, otherwise it is null. If null, the property is
     * considered read-only.
     */
    private Method setter;

    /**
     * A reference to the property method for this property. This should never
     * be null when setter is not null, and vice versa. The property method will
     * return a property (either ReadOnlyProperty or Property) object when
     * invoked of the appropriate type.
     */
    private Method property;

    /**
     * The reflectively determined type of the property. This is essentially
     * the return type of the getter method.
     */
    private Class<?> type;

    /**
     * The mutability of the property.
     */
    private Mutability mutability;

    /**
     * Create a new PropertyMetaData. Both the beanClass and getter must be
     * specified or a NullPointerException will be thrown. The getter must
     * be a method on the specified beanClass, or an IllegalArgumentException
     * will be thrown.
     *
     * @param beanClass The bean class, cannot be null
     * @param getter The getter on the bean class of the property,
     *        cannot be null
     */
    public PropertyMetaData(Class<?> beanClass, Method getter) {
        // If either the bean class or getter are null, throw an exception
        if (beanClass == null || getter == null) {
            throw new NullPointerException(
                    "Both the beanClass and getter cannot be null");
        }

        // Step 0: Parameter verification. If the getter is not a method on the
        //         beanClass, throw an exception. If the getter is not a proper
        //         getter, then throw an IllegalArgumentException
        if (!getter.getDeclaringClass().isAssignableFrom(beanClass)) {
            throw new IllegalArgumentException(
                    "The getter must be a method on the specified beanClass");
        }

        // Step 0a: Look for and load the resource bundles associated with
        //          this bean. Look for a "resources" bundle in the same
        //          package as the class, and a "FooResources" bundle also
        //          in the same package as the class. The FooResources takes
        //          precedence over "resources" in the case of lookup
        Resources bundle = new Resources(beanClass);

        init(beanClass, getter, bundle);
    }

    /**
     * Package private constructor which reuses the bundle and omits some checks
     * for the sake of performance efficiency.
     * 
     * @param beanClass The bean class, cannot be null
     * @param getter The getter, cannot be null
     * @param bundle The bundle, cannot be null
     */
    PropertyMetaData(Class<?> beanClass, Method getter, Resources bundle) {
        // If either the bean class or getter are null, throw an exception
        if (beanClass == null || getter == null) {
            throw new NullPointerException(
                    "Both the beanClass and getter cannot be null");
        }

        init(beanClass, getter, bundle);
    }

    MetaDataAnnotation getMetaDataAnnotation(Method getter) {
        final Property propertyAnnotation = getter.getAnnotation(Property.class);
        if (propertyAnnotation == null) return null;
        return new MetaDataAnnotation() {
            @Override public String displayName() {
                return propertyAnnotation.displayName();
            }

            @Override public String shortDescription() {
                return propertyAnnotation.shortDescription();
            }

            @Override public String category() {
                return propertyAnnotation.category();
            }
        };
    }

    /**
     * Initializes the class, called only from one of the above constructors.
     *
     * @param beanClass The bean class, cannot be null
     * @param getter The getter, cannot be null
     * @param bundle The bundle, cannot be null
     */
    private void init(Class<?> beanClass, Method getter, Resources bundle) {

        // Step 1: Discover the name of the property. Assign this name to the
        //         name property. Create a proper display name based on it.
        //         Let the short description be an empty string. Default the
        //         category to an empty string.

        final String getterName = getter.getName();
        final String capitalizedName = getterName.startsWith("get") ?
                getterName.substring(3) :
                getterName.substring(2);
        final String _name = decapitalize(capitalizedName);
        String _displayName = bundle.get("displayName", toDisplayName(_name));
        String _shortDescription = bundle.get("shortDescription", "");
        String _category = bundle.get("category", "");

        // Step 2: Discover the type of the property. This is simply the return
        //         type of the getter method.
        type = getter.getReturnType();

        // Step 3: Look for the @Bean annotation on the getter. If it exists,
        //         then use the information defined there to override the
        //         information previously computed.
        MetaDataAnnotation propertyAnnotation = getMetaDataAnnotation(getter);
        if (propertyAnnotation != null) {
            String s = propertyAnnotation.displayName();
            if (s != null && !COMPUTE.equals(s)) _displayName = s;
            s = propertyAnnotation.shortDescription();
            if (s != null && !COMPUTE.equals(s)) _shortDescription = s;
            s = propertyAnnotation.category();
            if (s != null && !COMPUTE.equals(s)) _category = s;
        }

        // Step 2a: If the _displayName, _shortDescription, or _category is
        //          prefixed by % then lookup the new value from bundle
        if (_displayName.startsWith("%")) _displayName = bundle.get(_displayName, _displayName);
        if (_shortDescription.startsWith("%")) _shortDescription = bundle.get(_shortDescription, _shortDescription);
        if (_category.startsWith("%")) _category = bundle.get(_category, _category);

        // Step 2b: Make sure the category is Hidden by default for the
        //          "class" property
        if ("".equals(_category) && "class".equals(_name)) {
            _category = "Hidden";
        }

        // Step 2c: Update the BeanMetaData with these final values
        configure(_name, _displayName, _shortDescription, _category);

        // Step 4: Discover the setter and property methods.
        this.getter = getter;
        final String setterName = "set" + capitalizedName;
        try {
            this.setter = beanClass.getMethod(setterName, type);
            int mods = this.setter.getModifiers();
            // TODO I couldn't figure out how to use Void.class to ensure
            // that the setter has a void return type. So right now I will
            // accept as the setter methods that don't have a void return type.
            if (Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
                this.setter = null;
            }
        } catch (NoSuchMethodException ex) { }

        final String propertyName = _name + "Property";
        try {
            this.property = beanClass.getMethod(propertyName);
            int mods = this.property.getModifiers();
            if (Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
                this.setter = null;
            } else {
                // Check the return type to make sure it is assignable from
                // the ReadOnlyPropertyClass, or Property class, as
                // appropriate.
                Class<?> returnType = this.property.getReturnType();
                Class<javafx.beans.property.ReadOnlyProperty> readOnlyProperty =
                        javafx.beans.property.ReadOnlyProperty.class;
                Class<javafx.beans.property.Property> writableProperty =
                        javafx.beans.property.Property.class;

                if ((!readOnlyProperty.isAssignableFrom(returnType) || this.setter != null) &&
                        (!writableProperty.isAssignableFrom(returnType) || this.setter == null)) {
                    this.property = null;
                }
            }
        } catch (NoSuchMethodException ex) { }

        // Step 5: Establish mutability
        if (property == null) {
            mutability = Mutability.IMMUTABLE;
        } else if (setter == null) {
            mutability = Mutability.READ_ONLY;
        } else {
            mutability = Mutability.WRITABLE;
        }
    }

    /**
     * Gets the data type for this property. This is simply the return type
     * of the getter method for the property, and should match the setter type
     * and the type housed within the property returned by the property method.
     *
     * @return the data type for this property
     */
    public final Class<?> getType() { return type; }

    /**
     * Gets the getter method for this property. This will never be null.
     *
     * @return The getter
     */
    public final Method getGetterMethod() { return getter; }

    /**
     * Gets the setter method for this property. If there is not a publicly
     * visible instance setter, then this method will return null.
     *
     * @return The setter method, or null
     */
    public final Method getSetterMethod() { return setter; }

    /**
     * Gets the property method for this property. This is the method which
     * returns the Property object for the property. If the property is
     * immutable, this method will return null. Otherwise, this will return
     * a method.
     *
     * @return The property method for this property, or null if the property
     *         is immutable.
     */
    public final Method getPropertyMethod() { return property; }

    /**
     * Gets the mutability of this property, which can either be IMMUTABLE,
     * READ_ONLY, or WRITABLE.
     *
     * @return The mutability of this property. This is never null.
     */
    public final Mutability getMutability() {
        return mutability;
    }
}
