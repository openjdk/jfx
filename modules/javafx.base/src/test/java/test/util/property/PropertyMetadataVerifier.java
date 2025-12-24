/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.util.property;

import javafx.beans.property.AttachedProperty;
import javafx.beans.property.ReadOnlyProperty;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public final class PropertyMetadataVerifier {

    private PropertyMetadataVerifier() {}

    /**
     * Asserts that the property metadata methods for all public or protected instance properties
     * declared on the specified class return their expected values. These are the methods tested:
     * <ul>
     *     <li>{@link ReadOnlyProperty#getBean()}
     *     <li>{@link ReadOnlyProperty#getName()}
     *     <li>{@link ReadOnlyProperty#getDeclaringClass()}
     *     <li>{@link AttachedProperty#getTargetClass()}
     * </ul>
     *
     * @param bean an object of the specified class
     * @param declaringClass the class that declares the properties to be tested
     */
    public static <T> void assertPropertyMetadata(T bean, Class<T> declaringClass) {
        assertPropertyMetadata(bean, declaringClass, _ -> null);
    }

    /**
     * Asserts that the property metadata methods for all public or protected instance properties
     * as well as attached properties declared on the specified class return their expected values.
     * These are the methods tested:
     * <ul>
     *     <li>{@link ReadOnlyProperty#getBean()}
     *     <li>{@link ReadOnlyProperty#getName()}
     *     <li>{@link ReadOnlyProperty#getDeclaringClass()}
     *     <li>{@link AttachedProperty#getTargetClass()}
     * </ul>
     *
     * @param bean an object of the specified class
     * @param declaringClass the class that declares the properties to be tested
     * @param targetObjectSupplier a function that supplies the target object for attached properties
     */
    public static <T> void assertPropertyMetadata(
            T bean, Class<T> declaringClass, Function<Class<?>, Object> targetObjectSupplier) {
        try {
            assertPropertyMetadataImpl(bean, declaringClass, targetObjectSupplier);
        } catch (ReflectiveOperationException e) {
            fail(e.getMessage(), e);
        }
    }

    private static <T> void assertPropertyMetadataImpl(
            Object bean, Class<T> declaringClass, Function<Class<?>, Object> targetObjectSupplier)
                throws ReflectiveOperationException {
        assertTrue(declaringClass.isInstance(bean), "Bean is not an instance of declaring class");

        for (var method : declaringClass.getDeclaredMethods()) {
            var propertyInfo = getPropertyInfo(method);
            if (propertyInfo == null) {
                continue;
            }

            assertPropertyStaticity(propertyInfo, method);

            Object actualBean = propertyInfo.targetClass() != null
                ? targetObjectSupplier.apply(propertyInfo.targetClass())
                : bean;

            ReadOnlyProperty<?> property = propertyInfo.targetClass() != null
                ? (ReadOnlyProperty<?>)method.invoke(null, actualBean)
                : (ReadOnlyProperty<?>)method.invoke(actualBean);

            assertPropertyBean(property, propertyInfo, actualBean);
            assertPropertyName(property, propertyInfo);
            assertPropertyDeclaringClass(property, propertyInfo);
            assertPropertyTargetClass(property, propertyInfo);
        }
    }

    private static void assertPropertyStaticity(PropertyInfo propertyInfo, Method method) {
        if (propertyInfo.targetClass() != null) {
            assertTrue((method.getModifiers() & Modifier.STATIC) != 0,
               propertyInfo.displayName() + " is declared like an attached property, but has a non-static property getter");
        } else {
            assertTrue((method.getModifiers() & Modifier.STATIC) == 0,
                propertyInfo.displayName() + " is declared like an instance property, but has a static property getter");
        }
    }

    private static void assertPropertyBean(ReadOnlyProperty<?> property, PropertyInfo propertyInfo, Object bean) {
        assertSame(
            bean, property.getBean(),
            propertyInfo.displayName() + "#getBean() returns unexpected bean");
    }

    private static void assertPropertyName(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        String actualPropertyName = property.getName();
        assertEquals(
            propertyInfo.name(), property.getName(),
            propertyInfo.displayName() + "#getName() returns "
                + (actualPropertyName != null ? "\"" + actualPropertyName + "\"" : "null")
                + ", but expected \"" + propertyInfo.name() + "\"");
    }

    private static void assertPropertyDeclaringClass(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        Class<?> actualDeclaringClass = property.getDeclaringClass();
        assertEquals(
            propertyInfo.declaringClass(), actualDeclaringClass,
            propertyInfo.displayName() + "#getDeclaringClass() returns "
                + (actualDeclaringClass != null ? actualDeclaringClass.getName() : "null") + ", but expected "
                + propertyInfo.declaringClass().getName());
    }

    private static void assertPropertyTargetClass(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        if (propertyInfo.targetClass() != null) {
            assertTrue(
                property instanceof AttachedProperty,
                propertyInfo.displayName() + " is declared like an attached property, but does not implement AttachedProperty");

            Class<?> actual = ((AttachedProperty)property).getTargetClass();
            Class<?> expected = propertyInfo.targetClass();
            assertEquals(expected, actual,
                propertyInfo.displayName() + "#getTargetClass() returns "
                + (actual != null ? actual.getName() : "null") + ", but expected " + expected.getName());
        } else {
            assertFalse(
                property instanceof AttachedProperty,
                propertyInfo.displayName() + " implements AttachedProperty, but is not declared like an attached property");
        }
    }

    private static PropertyInfo getPropertyInfo(Method method) {
        String methodName = method.getName();
        int modifiers = method.getModifiers();
        boolean isPublic = (modifiers & Modifier.PUBLIC) != 0 || (modifiers & Modifier.PROTECTED) != 0;

        if (!isPublic
                || !methodName.endsWith("Property")
                || !ReadOnlyProperty.class.isAssignableFrom(method.getReturnType())
                || method.getParameterCount() >= 2) {
            return null;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        String propertyName = methodName.substring(0, methodName.length() - "Property".length());
        String displayName = declaringClass.getName() + "." + propertyName;

        return method.getParameterCount() == 1
            ? new PropertyInfo(propertyName, displayName, declaringClass,
                               method.getReturnType(), method.getParameterTypes()[0])
            : new PropertyInfo(propertyName, displayName, declaringClass,
                               method.getReturnType(), null);
    }

    private record PropertyInfo(
        String name,
        String displayName,
        Class<?> declaringClass,
        Class<?> propertyClass,
        Class<?> targetClass) {}
}
