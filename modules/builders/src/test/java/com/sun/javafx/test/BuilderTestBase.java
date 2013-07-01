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

package com.sun.javafx.test;

import com.sun.javafx.test.binding.ReflectionHelper;
import com.sun.javafx.tk.Toolkit;
import java.lang.reflect.Array;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;


public abstract class BuilderTestBase {
    private final Configuration configuration;

    public BuilderTestBase(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Test
    public void testBuilder() {
        configuration.buildTest();
    }

    @Test
    public void testEllipsis() {
        configuration.ellipsisTest();
    }

    public static class Configuration {
        private final List<BuilderProperty> properties;
        private final List<BuilderProperty> typedProperties;
        private final Class<?> bean;

        private static class BuilderProperty {
            public PropertyReference propertyReference;
            public Object expectedValue;
            public ValueComparator comparator = ValueComparator.DEFAULT;
            public Class<?> type;
            BuilderProperty(PropertyReference pr,
                    Object value,
                    ValueComparator comparator,
                    Class<?> type) {
                this.propertyReference = pr;
                this.expectedValue = value;
                this.comparator = comparator;
                this.type = type;
            }
        }

        public Configuration(Class<?> bean) {
            Toolkit.getToolkit();
            properties = new LinkedList<BuilderProperty> ();
            typedProperties = new LinkedList<BuilderProperty> ();
            this.bean = bean;
        }

        public void addProperty(String property, Object propertyValue) {
            PropertyReference pr = PropertyReference.createForBean(bean, property);
            properties.add(new BuilderProperty(pr, propertyValue, ValueComparator.DEFAULT, null));
        }

        public void addProperty(String property, Object propertyValue, ValueComparator comparator) {
            PropertyReference pr = PropertyReference.createForBean(bean, property);
            properties.add(new BuilderProperty(pr, propertyValue, comparator, null));
        }

        public void addProperty(String property, Class<?> elementType, Collection values) {
            PropertyReference pr = PropertyReference.createForBean(bean, property);
            properties.add(new BuilderProperty(pr, values, ValueComparator.DEFAULT, elementType));
            typedProperties.add(new BuilderProperty(pr, values, ValueComparator.DEFAULT, elementType));
        }

        public void buildTest() {
            // create builder
            final String builderClassName = bean.getName() + "Builder";

            Class<?> builderClass;
            try {
                builderClass = Class.forName(builderClassName);
            } catch (final ClassNotFoundException e) {
                // if there is no builder for the bean, we return
                return;
            }

            Method createMethod = ReflectionHelper.getMethod(builderClass, "create");
            Object builder = ReflectionHelper.invokeMethod(null, createMethod);

            for (BuilderProperty builderProperty : properties) {
                PropertyReference property = builderProperty.propertyReference;
                Object value = builderProperty.expectedValue;

                Method m = null;
                Class<?> valueType = property.getValueType();
                try {
                    m = ReflectionHelper.getMethod(builderClass, property.getPropertyName(), valueType);
                } catch (RuntimeException e) {
                    // in some cases Collections are handled separately in builders
                    // we need to use Collection class directly
                    if (Collection.class.isAssignableFrom(valueType)) {
                        valueType = Collection.class;
                        m = ReflectionHelper.getMethod(builderClass, property.getPropertyName(), valueType);
                    } else {
                        throw e;
                    }
                }
                ReflectionHelper.invokeMethod(builder, m, value);
            }

            // create new object via builder
            Method buildMethod = ReflectionHelper.getMethod(builderClass, "build");
            Object object = ReflectionHelper.invokeMethod(builder, buildMethod);


            // check whether the object was created correctly
            for (BuilderProperty builderProperty : properties) {
                PropertyReference property = builderProperty.propertyReference;
                Object expectedValue = builderProperty.expectedValue;

                builderProperty.comparator.assertEquals(expectedValue, property.getValue(object));
            }
        }

        public void ellipsisTest() {
            if (typedProperties.isEmpty()) {
                return; // nothing to test
            }

            // create builder
            final String builderClassName = bean.getName() + "Builder";

            Class<?> builderClass;
            try {
                builderClass = Class.forName(builderClassName);
            } catch (final ClassNotFoundException e) {
                // if there is no builder for the bean, we return
                return;
            }

            Method createMethod = ReflectionHelper.getMethod(builderClass, "create");
            Object builder = ReflectionHelper.invokeMethod(null, createMethod);

            for (BuilderProperty builderProperty : typedProperties) {
                PropertyReference property = builderProperty.propertyReference;
                Object value = builderProperty.expectedValue;


                Class<?> valueType = Array.newInstance(builderProperty.type, 0).getClass();
                Method m = ReflectionHelper.getMethod(builderClass, property.getPropertyName(), valueType);
                ReflectionHelper.invokeMethod(builder, m, (Object) ((Collection) value).toArray());
            }

            // create new object via builder
            Method buildMethod = ReflectionHelper.getMethod(builderClass, "build");
            Object object = ReflectionHelper.invokeMethod(builder, buildMethod);


            // check whether the object was created correctly
            for (BuilderProperty builderProperty : typedProperties) {
                PropertyReference property = builderProperty.propertyReference;
                Object expectedValue = builderProperty.expectedValue;

                builderProperty.comparator.assertEquals(expectedValue, property.getValue(object));
            }
        }
    }

    public static Object[] config(final Configuration configuration) {
        return new Object[] { configuration };
    }
}
