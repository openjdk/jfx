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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.junit.Test;

public abstract class ObjectMethodsTestBase {
    private final Configuration configuration;

    public ObjectMethodsTestBase(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Test
    public void testEquals() {
        configuration.equalsTest();
    }

    @Test
    public void testHashCode() {
        configuration.hashCodeTest();
    }

    @Test
    public void testToString() {
        configuration.toStringTest();
    }

    public static Object[] equalObjects(final Object... objects) {
        return config(new Configuration(EQUAL_OBJECTS_EQUALS_TEST,
                                        EQUAL_OBJECTS_HASHCODE_TEST,
                                        EQUAL_OBJECTS_TOSTRING_TEST,
                                        objects));
    }

    public static Object[] differentObjects(
            final Object... objects) {
        return config(new Configuration(DIFFERENT_OBJECTS_EQUALS_TEST,
                                        DIFFERENT_OBJECTS_HARD_HASHCODE_TEST,
                                        DIFFERENT_OBJECTS_TOSTRING_TEST,
                                        objects));
    }

    public static Object[] differentObjectsEasyHashcode(
            final Object... objects) {
        return config(new Configuration(DIFFERENT_OBJECTS_EQUALS_TEST,
                                        DIFFERENT_OBJECTS_EASY_HASHCODE_TEST,
                                        DIFFERENT_OBJECTS_TOSTRING_TEST,
                                        objects));
    }

    public static Object[] differentObjectsMediumHashcode(
            final Object... objects) {
        return config(new Configuration(DIFFERENT_OBJECTS_EQUALS_TEST,
                                        DIFFERENT_OBJECTS_MEDIUM_HASHCODE_TEST,
                                        DIFFERENT_OBJECTS_TOSTRING_TEST,
                                        objects));
    }

    public static Object[] config(final Configuration configuration) {
        return new Object[] { configuration };
    }

    public static final class Configuration {
        private final Object[] objects;

        private final TestInstance equalsTest;

        private final TestInstance hashCodeTest;

        private final TestInstance toStringTest;

        public Configuration(final TestInstance equalsTest,
                             final TestInstance hashCodeTest,
                             final TestInstance toStringTest,
                             final Object... objects) {
            this.equalsTest = equalsTest;
            this.hashCodeTest = hashCodeTest;
            this.toStringTest = toStringTest;
            this.objects = objects;
        }

        public void equalsTest() {
            if (equalsTest != null) {
                equalsTest.test(objects);
            }
        }

        public void hashCodeTest() {
            if (hashCodeTest != null) {
                hashCodeTest.test(objects);
            }
        }

        public void toStringTest() {
            if (toStringTest != null) {
                toStringTest.test(objects);
            }
        }
    }

    public interface TestInstance {
        void test(Object[] objects);
    }

    public static final TestInstance EQUAL_OBJECTS_EQUALS_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    for (int i = 0; i < objects.length; ++i) {
                        for (int j = 0; j < objects.length; ++j) {
                            assertEquals(objects[i], objects[j]);
                        }
                        assertFalse(objects[i].equals(null));
                    }
                }
            };

    public static final TestInstance EQUAL_OBJECTS_HASHCODE_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    for (int i = 0; i < objects.length; ++i) {
                        for (int j = 0; j < objects.length; ++j) {
                            assertEquals(objects[i].hashCode(),
                                         objects[j].hashCode());
                        }
                    }
                }
            };
            
    public static final TestInstance EQUAL_OBJECTS_TOSTRING_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    for (int i = 0; i < objects.length; ++i) {
                        for (int j = 0; j < objects.length; ++j) {
                            assertEquals(objects[i].toString(),
                                         objects[j].toString());
                        }
                    }
                }
            };

    public static final TestInstance DIFFERENT_OBJECTS_EQUALS_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    for (int i = 0; i < objects.length; ++i) {
                        for (int j = 0; j < objects.length; ++j) {
                            if (i != j) {
                                assertFalse(objects[i].equals(objects[j]));
                            }
                        }
                        assertFalse(objects[i].equals(null));
                    }
                }
            };

    public static final TestInstance DIFFERENT_OBJECTS_EASY_HASHCODE_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    // if objects are different, their hashcodes can return the
                    // same or different values
                }
            };

    public static final TestInstance DIFFERENT_OBJECTS_MEDIUM_HASHCODE_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    // we require that at least one tested object returns a
                    // different hash code value
                    final int firstHashCodeValue = objects[0].hashCode();
                    for (int i = 1; i < objects.length; ++i) {
                        if (objects[i].hashCode() != firstHashCodeValue) {
                            return;
                        }
                    }

                    // all the hash codes are same, this violates our criteria
                    fail();
                }
            };

    public static final TestInstance DIFFERENT_OBJECTS_HARD_HASHCODE_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    // we require that at all tested objects returns a different
                    // hash code value
                    for (int i = 0; i < objects.length; ++i) {
                        for (int j = 0; j < objects.length; ++j) {
                            if (i != j) {
                                assertNotSame(objects[i].hashCode(),
                                              objects[j].hashCode());
                            }
                        }
                    }
                }
            };

    public static final TestInstance DIFFERENT_OBJECTS_TOSTRING_TEST =
            new TestInstance() {
                @Override
                public void test(final Object[] objects) {
                    for (int i = 0; i < objects.length; ++i) {
                        for (int j = 0; j < objects.length; ++j) {
                            if (i != j) {
                                assertFalse(objects[i].toString().equals(
                                                objects[j].toString()));
                            }
                        }
                    }
                }
            };
}
