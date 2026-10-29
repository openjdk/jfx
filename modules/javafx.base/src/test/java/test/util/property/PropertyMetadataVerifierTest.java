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
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class PropertyMetadataVerifierTest {

    @Test
    void readOnlyWrapperIsNotReturnedFromPropertyGetter() {
        class A {
            private final ReadOnlyStringWrapper prop = new ReadOnlyStringWrapper(this, A.class, "prop") {};
            public ReadOnlyStringProperty propProperty() { return prop; }
        }

        assertError(
            new A(), A.class,
            error -> matches(
                error.getMessage(),
                "?A.propProperty() must not return ?A$1, expected ?A$1#getReadOnlyProperty()"));
    }

    @Nested
    class BeanTest {

        @Test
        void getBeanReturnsExpectedBean() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(this, A.class, "prop");
                public StringProperty propProperty() { return prop; }
            }

            assertNoError(new A(), A.class);
        }

        @Test
        void getBeanReturnsUnexpectedBean() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(null, A.class, "prop");
                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getBean() returns null, expected ?A@?"));
        }

        @Test
        void getBeanOverriddenInSimplePropertyClass() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(this, A.class, "prop") {
                    @Override public Object getBean() { return A.this; }
                };

                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getBean() must not be overridden in ?A$1, "
                    + "pass the bean to the constructor of javafx.beans.property.SimpleStringProperty"));
        }

        @Test
        void getBeanOverriddenInReadOnlyPropertyWrapper() {
            class A {
                private final ReadOnlyStringWrapper prop = new ReadOnlyStringWrapper(this, A.class, "prop") {
                    @Override public Object getBean() { return A.this; }
                };

                public ReadOnlyStringProperty propProperty() { return prop.getReadOnlyProperty(); }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getBean() must not be overridden in ?A$1, "
                    + "pass the bean to the constructor of javafx.beans.property.ReadOnlyStringWrapper"));
        }
    }

    @Nested
    class NameTest {

        @Test
        void getNameReturnsUnexpectedName() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(this, A.class, "wrongName");
                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getName() returns \"wrongName\", but expected \"prop\""));
        }

        @Test
        void getNameOverriddenInSimplePropertyClass() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(this, A.class, "prop") {
                    @Override public String getName() { return "prop"; }
                };

                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getName() must not be overridden in ?A$1, "
                    + "pass the name to the constructor of javafx.beans.property.SimpleStringProperty"));
        }

        @Test
        void getNameOverriddenInReadOnlyPropertyWrapper() {
            class A {
                private final ReadOnlyStringWrapper prop = new ReadOnlyStringWrapper(this, A.class, "prop") {
                    @Override public String getName() { return "prop"; }
                };

                public ReadOnlyStringProperty propProperty() { return prop.getReadOnlyProperty(); }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getName() must not be overridden in ?A$1, "
                    + "pass the name to the constructor of javafx.beans.property.ReadOnlyStringWrapper"));
        }
    }

    @Nested
    class DeclaringClassTest {

        @Test
        void getDeclaringClassMustBeOverridden() {
            class A {
                private final StringProperty prop = new StringPropertyBase() {
                    @Override public Object getBean() { return A.this; }
                    @Override public String getName() { return "prop"; }
                };

                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getDeclaringClass() must be overridden and return ?A"));
        }

        @Test
        void getDeclaringClassReturnsUnexpectedClass() {
            class A {
                private final StringProperty prop = new StringPropertyBase() {
                    @Override public Object getBean() { return A.this; }
                    @Override public String getName() { return "prop"; }
                    @Override public Class<?> getDeclaringClass() { return String.class; }
                };

                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getDeclaringClass() returns java.lang.String, but expected ?A"));
        }

        @Test
        void getDeclaringClassOverriddenInSimplePropertyClass() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(this, "prop") {
                    @Override public Class<?> getDeclaringClass() { return A.class; }
                };

                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getDeclaringClass() must not be overridden in ?A$1, "
                    + "pass ?A to the constructor of javafx.beans.property.SimpleStringProperty"));
        }

        @Test
        void getDeclaringClassOverriddenInReadOnlyPropertyWrapper() {
            class A {
                private final ReadOnlyStringWrapper prop = new ReadOnlyStringWrapper(this, "prop") {
                    @Override public Class<?> getDeclaringClass() { return A.class; }
                };

                public ReadOnlyStringProperty propProperty() { return prop.getReadOnlyProperty(); }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "?A.prop#getDeclaringClass() must not be overridden in ?A$1, "
                    + "pass ?A to the constructor of javafx.beans.property.ReadOnlyStringWrapper"));
        }

        @Test
        void declaringClassMustBeSpecifiedInSimplePropertyClassConstructor() {
            class A {
                private final StringProperty prop = new SimpleStringProperty(this, "prop");
                public StringProperty propProperty() { return prop; }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "Declaring class of ?A.prop must be specified in the constructor of "
                    + "javafx.beans.property.SimpleStringProperty"));
        }

        @Test
        void declaringClassMustBeSpecifiedInReadOnlyWrapperClassConstructor() {
            class A {
                private final ReadOnlyStringWrapper prop = new ReadOnlyStringWrapper(this, "prop");
                public ReadOnlyStringProperty propProperty() { return prop.getReadOnlyProperty(); }
            }

            assertError(
                new A(), A.class,
                error -> matches(
                    error.getMessage(),
                    "Declaring class of ?A.prop must be specified in the constructor of "
                    + "javafx.beans.property.ReadOnlyStringWrapper"));
        }
    }

    @Nested
    class TargetClassTest {

        @Test
        void attachedPropertyDoesNotImplementInterface() {
            class A {
                public static StringProperty propProperty(Integer target) {
                    return new SimpleStringProperty(target, A.class, "prop");
                }
            }

            assertError(
                new A(), A.class,
                target -> 0,
                error -> matches(
                    error.getMessage(),
                    "?A.prop has a static property getter, but does not implement AttachedProperty"));
        }

        @Test
        void attachedPropertyDoesNotHaveStaticPropertyGetter() {
            class A {
                static class P extends SimpleStringProperty implements AttachedProperty {
                    P(Object bean) {
                        super(bean, A.class, "prop");
                    }

                    @Override
                    public Class<?> getTargetClass() {
                        return Integer.class;
                    }
                }

                public P propProperty() { return new P(this); }
            }

            assertError(
                new A(), A.class,
                target -> 0,
                error -> matches(
                    error.getMessage(),
                    "?A.prop implements AttachedProperty, but does not have a static property getter"));
        }

        @Test
        void attachedPropertyReturnsUnexpectedTargetClass() {
            class A {
                static class P extends SimpleStringProperty implements AttachedProperty {
                    P(Object bean) {
                        super(bean, A.class, "prop");
                    }

                    @Override
                    public Class<?> getTargetClass() {
                        return Double.class;
                    }
                }

                public static P propProperty(Integer target) {
                    return new P(target);
                }
            }

            assertError(
                new A(), A.class,
                target -> 0,
                error -> matches(
                    error.getMessage(),
                    "?#getTargetClass() returns java.lang.Double, but expected java.lang.Integer"));
        }
    }

    private static <T> void assertNoError(T bean, Class<T> beanClass) {
        assertDoesNotThrow(() -> PropertyMetadataVerifier.assertPropertyMetadata(bean, beanClass));
    }

    private static <T> void assertError(T bean, Class<T> beanClass, Predicate<AssertionError> predicate) {
        var error = assertThrows(
            AssertionError.class,
            () -> PropertyMetadataVerifier.assertPropertyMetadata(bean, beanClass));

        if (!predicate.test(error)) {
            fail(error.getMessage());
        }
    }

    private static <T> void assertError(T bean, Class<T> beanClass,
                                        Function<Class<?>, Object> targetObjectSupplier,
                                        Predicate<AssertionError> predicate) {
        var error = assertThrows(
            AssertionError.class,
            () -> PropertyMetadataVerifier.assertPropertyMetadata(bean, beanClass, targetObjectSupplier));

        assertTrue(predicate.test(error), error::getMessage);
    }

    private static boolean matches(String actual, String template) {
        String[] parts = template.split(Pattern.quote("?"), -1);
        StringBuilder regex = new StringBuilder("^");

        for (int i = 0; i < parts.length; i++) {
            regex.append(Pattern.quote(parts[i]));

            if (i < parts.length - 1) {
                regex.append("([^,]+)");
            }
        }

        return actual.matches(regex.append("$").toString());
    }
}
