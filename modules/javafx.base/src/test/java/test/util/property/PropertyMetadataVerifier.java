/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import test.javafx.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public final class PropertyMetadataVerifier {

    private PropertyMetadataVerifier() {}

    /**
     * Asserts that property implementations for all public or protected properties of the specified object
     * conform to the following rules:
     * <ul>
     *     <li>{@link ReadOnlyProperty#getBean()} returns the object instance of the enclosing class,
     *         or the target object instance if the property is an attached property
     *     <li>{@link ReadOnlyProperty#getName()} returns the name of the property, which must
     *         correspond to the name of the property getter (excluding the word "Property")
     *     <li>{@link ReadOnlyProperty#getDeclaringClass()} returns the enclosing class of the property getter
     *     <li>The declaring class of a {@code Simple<*>Property} or {@code ReadOnly<*>Wrapper} must be
     *         specified in the constructor, not resolved at runtime
     *     <li>{@code getBean()}, {@code getName()}, and {@code getDeclaringClass()} must not be overridden
     *         in subclasses of {@code Simple<*>Property} or {@code ReadOnly<*>Wrapper}
     *     <li>An instance property does not implement {@link AttachedProperty}
     *     <li>An instance property has a parameterless property getter
     *     <li>An attached property implements {@code AttachedProperty}
     *     <li>An attached property has a static single-argument property getter that accepts the target object
     *     <li>{@link AttachedProperty#getTargetClass()} returns the class of the single parameter of the
     *         static property getter
     *     <li>A property getter does not return an instance of {@code ReadOnly<*>Wrapper}, it returns
     *         the result of calling {@code ReadOnly<*>Wrapper.getReadOnlyProperty()}
     * </ul>
     *
     * @param bean an object of the specified class
     * @param declaringClass the class that declares the properties to be tested
     */
    public static <T> void assertPropertyMetadata(T bean, Class<T> declaringClass) {
        assertPropertyMetadata(bean, declaringClass, _ -> null);
    }

    /**
     * Asserts that property implementations for all public or protected properties of the specified object
     * conform to the rules as specified in {@link #assertPropertyMetadata(Object, Class)}.
     * <p>
     * This method must be used if the declaring class includes attached properties, and the specified
     * supplier must return target objects for all attached properties encountered in the class.
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

            Object actualBean = propertyInfo.targetClass() != null
                ? Objects.requireNonNull(
                    targetObjectSupplier.apply(propertyInfo.targetClass()),
                    propertyInfo.displayName() + " is an attached property, use "
                        + "PropertyMetadataVerifier.assertPropertyMetadata(T, Class<T>, Function<Class<?>, Object>) "
                        + "and specify a supplier that returns a non-null target object")
                : bean;

            ReadOnlyProperty<?> property = propertyInfo.targetClass() != null
                ? (ReadOnlyProperty<?>)method.invoke(null, actualBean)
                : (ReadOnlyProperty<?>)method.invoke(actualBean);

            assertPropertyBean(property, propertyInfo, actualBean);
            assertPropertyName(property, propertyInfo);
            assertPropertyReadOnlyWrapper(property, propertyInfo);
            assertPropertyDeclaringClass(property, propertyInfo);
            assertPropertyTargetClass(property, propertyInfo);
        }
    }

    private static void assertPropertyBean(ReadOnlyProperty<?> property, PropertyInfo propertyInfo, Object bean) {
        Object actualBean = property.getBean();
        if (bean != actualBean) {
            fail("%s#getBean() returns %s, expected %s".formatted(propertyInfo.displayName(), actualBean, bean));
        }

        assertSimplePropertyNoOverride(property, propertyInfo, "getBean");
    }

    private static void assertPropertyName(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        String actualPropertyName = property.getName();
        if (!propertyInfo.name().equals(property.getName())) {
            fail("%s#getName() returns %s, but expected \"%s\"".formatted(propertyInfo.displayName(),
                actualPropertyName != null ? "\"" + actualPropertyName + "\"" : "null", propertyInfo.name()));
        }

        assertSimplePropertyNoOverride(property, propertyInfo, "getName");
    }

    private static void assertPropertyReadOnlyWrapper(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        if (isReadOnlyWrapperOrDerivedClass(property.getClass())) {
            fail("%sProperty() must not return %s, expected %s#getReadOnlyProperty()".formatted(
                propertyInfo.displayName(), property.getClass().getName(), property.getClass().getName()));
        }
    }

    private static void assertPropertyDeclaringClass(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        Class<?> propertyClass = property.getClass();
        Class<?> originalPropertyClass = propertyClass;

        if (isReadOnlyWrapperImplClass(propertyClass)) {
            property = ReflectionUtils.getFieldValue(property, "this$0");
            propertyClass = property.getClass();
            originalPropertyClass = propertyClass;
        }

        do {
            try {
                propertyClass.getDeclaredMethod("getDeclaringClass");

                if (isSimplePropertyClass(propertyClass) || isReadOnlyWrapperClass(propertyClass)) {
                    if (ReflectionUtils.getFieldValue(property, "declaringClass") == null) {
                        fail("Declaring class of %s must be specified in the constructor of %s".formatted(
                            propertyInfo.displayName(),
                            getSimplePropertyOrReadOnlyWrapperClass(originalPropertyClass).getName()));
                    }
                } else if (isSimplePropertyOrDerivedClass(propertyClass)) {
                    fail(("%s#getDeclaringClass() must not be overridden in %s, pass %s "
                        + "to the constructor of %s").formatted(
                            propertyInfo.displayName(), propertyClass.getName(),
                            propertyInfo.declaringClass().getName(),
                            getSimplePropertyOrReadOnlyWrapperClass(propertyClass).getName()));
                }

                break;
            } catch (NoSuchMethodException ex) {
                propertyClass = propertyClass.getSuperclass();
            }
        } while (propertyClass != null);

        if (propertyClass == null) {
            fail("%s#getDeclaringClass() must be overridden and return %s".formatted(
                propertyInfo.displayName(), propertyInfo.declaringClass().getName()));
        }

        Class<?> actualDeclaringClass = property.getDeclaringClass();

        if (!propertyInfo.declaringClass().equals(actualDeclaringClass)) {
            fail("%s#getDeclaringClass() returns %s, but expected %s".formatted(
                propertyInfo.displayName(),
                actualDeclaringClass != null ? actualDeclaringClass.getName() : "null",
                propertyInfo.declaringClass().getName()));
        }
    }

    private static void assertPropertyTargetClass(ReadOnlyProperty<?> property, PropertyInfo propertyInfo) {
        if (propertyInfo.targetClass() != null) {
            if (!(property instanceof AttachedProperty)) {
                fail(propertyInfo.displayName() + " has a static property getter, but does not implement AttachedProperty");
            }

            Class<?> actual = ((AttachedProperty)property).getTargetClass();
            Class<?> expected = propertyInfo.targetClass();

            if (!expected.equals(actual)) {
                fail("%s#getTargetClass() returns %s, but expected %s".formatted(
                    propertyInfo.displayName(),
                    actual != null ? actual.getName() : "null",
                    expected.getName()));
            }
        } else if (property instanceof AttachedProperty) {
            fail(propertyInfo.displayName() + " implements AttachedProperty, but does not have a static property getter");
        }
    }

    private static void assertSimplePropertyNoOverride(
            ReadOnlyProperty<?> property, PropertyInfo propertyInfo, String methodName) {
        Class<?> propertyClass = property.getClass();

        if (isReadOnlyWrapperImplClass(propertyClass)) {
            property = ReflectionUtils.getFieldValue(property, "this$0");
            propertyClass = property.getClass();
        }

        if (isSimplePropertyOrDerivedClass(propertyClass)) {
            do {
                try {
                    propertyClass.getDeclaredMethod(methodName);

                    if (isSimplePropertyClass(propertyClass)) {
                        return;
                    }

                    break;
                } catch (NoSuchMethodException ex) {
                    propertyClass = propertyClass.getSuperclass();
                }
            } while (propertyClass != null);

            if (propertyClass != null) {
                fail("%s#%s() must not be overridden in %s, pass the %s to the constructor of %s".formatted(
                    propertyInfo.displayName(), methodName, propertyClass.getName(),
                    methodName.substring(3).toLowerCase(Locale.ROOT), getSimplePropertyOrReadOnlyWrapperClass(propertyClass).getName()));
            }
        }
    }

    private static boolean isSimplePropertyOrDerivedClass(Class<?> propertyClass) {
        do {
            if (isSimplePropertyClass(propertyClass)) {
                return true;
            }

            propertyClass = propertyClass.getSuperclass();
        } while (propertyClass != null);

        return false;
    }

    private static boolean isSimplePropertyClass(Class<?> propertyClass) {
        String name = propertyClass.getName();
        return name.startsWith("javafx.beans.property.Simple") && name.endsWith("Property");
    }

    private static boolean isReadOnlyWrapperOrDerivedClass(Class<?> propertyClass) {
        do {
            if (isReadOnlyWrapperClass(propertyClass)) {
                return true;
            }

            propertyClass = propertyClass.getSuperclass();
        } while (propertyClass != null);

        return false;
    }

    private static boolean isReadOnlyWrapperClass(Class<?> propertyClass) {
        String name = propertyClass.getName();
        return name.startsWith("javafx.beans.property.ReadOnly") && name.endsWith("Wrapper");
    }

    private static boolean isReadOnlyWrapperImplClass(Class<?> propertyClass) {
        return propertyClass.getEnclosingClass() instanceof Class<?> enclosingClass
            && isReadOnlyWrapperClass(enclosingClass);
    }

    private static Class<?> getSimplePropertyOrReadOnlyWrapperClass(Class<?> propertyClass) {
        do {
            if (isSimplePropertyClass(propertyClass) || isReadOnlyWrapperClass(propertyClass)) {
                return propertyClass;
            }

            propertyClass = propertyClass.getSuperclass();
        } while (propertyClass != null);

        throw new AssertionError();
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
