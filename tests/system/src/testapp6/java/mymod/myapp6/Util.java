/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package myapp6;

import java.net.URL;

public class Util {

    public static final URL getURL(Class clazz, String name) {
        final String theName = name + ".fxml";
        final URL fxmlFile = clazz.getResource(theName);
        if (fxmlFile == null) {
            throw new AssertionError("Unable to open: " + theName);
        }
        return fxmlFile;
    }

    public static void assertNotNull(Object o) {
        if (o == null) {
            throw new AssertionError("Expected non-null object, but was null");
        }
    }

    public static void assertEquals(int expected, int observed) {
        if (expected != observed) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + observed + ">");
        }
    }

    public static void assertEquals(String expected, String observed) {
        if ((expected == null && observed != null) || !expected.equals(observed)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + observed + ">");
        }
    }

    public static void assertSame(Object expected, Object observed) {
        if (expected != observed) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + observed + ">");
        }
    }

    public static void assertTrue(String message, boolean cond) {
        if (!cond) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(String message, boolean cond) {
        if (cond) {
            throw new AssertionError(message);
        }
    }

    private Util() {
    }
}
