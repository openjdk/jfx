/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package myapp2;

import java.net.URL;

public class Util {

    public static final URL getURL(Class clazz, String name) {
        final String theName = name + ".fxml";
        final URL fxmlFile = clazz.getResource(theName);
        if (fxmlFile == null) {
            throw new AssertionError("(getURL()) unable to open: " + theName);
        }
        return fxmlFile;
    }

    public static void assertNull(String message, Object o) {
        if (o != null) {
            throw new AssertionError("(assertNull) " + message + ", expected null object, but was non-null");
        }
    }

    public static void assertNotNull(Object o) {
        if (o == null) {
            throw new AssertionError("(assertNotNull) expected non-null object, but was null");
        }
    }

    public static void assertEndsWith(String expected, String observed) {
        if ((expected == null && observed != null) || !observed.endsWith(expected)) {
            throw new AssertionError("(assertEndsWith) " + "expected: <" + expected + "> but was: <" + observed + ">");
        }
    }

    public static void assertStartsWith(String expected, String observed) {
        if ((expected == null && observed != null) || !observed.startsWith(expected)) {
            throw new AssertionError("(assertStartsWith) " + "expected: <" + expected + "> but was: <" + observed + ">");
        }
    }


    public static void assertSame(String message, Object expected, Object observed) {
        if (expected != observed) {
            throw new AssertionError("(assertSame) "+ message + ", expected: <" + expected + "> but was: <" + observed + ">");
        }
    }

    public static void assertTrue(String message, boolean cond) {
        if (!cond) {
            throw new AssertionError("(assertTrue): " + message);
        }
    }

    public static void assertFalse(String message, boolean cond) {
        if (cond) {
            throw new AssertionError("(assertFalse): " + message);
        }
    }

    public static void assertExists(String message, boolean cond) {
        if (!cond) {
            throw new AssertionError("(assertExists): " + message);
        }
    }

    public static void assertNotExists(String message, boolean cond) {
        if (cond) {
            throw new AssertionError("(assertNotExists): " + message);
        }
    }

    public static void assertType(String message, Class clz, Object obj) {
        if (obj == null) {
            throw new AssertionError("(assertType): " + message+": \"obj\" is null");
        }
        else if (clz == null) {
            throw new AssertionError("(assertType): " + message+": \"clz\" is null");
        }

        if (! clz.isInstance(obj)) {
            throw new AssertionError("(assertType): " + message
                                      + ", object " + obj +
                                      " is not an instance of class " +
                                      clz + " -> " + clz.getName() + "]");
        }
    }

    private Util() {
    }
}
