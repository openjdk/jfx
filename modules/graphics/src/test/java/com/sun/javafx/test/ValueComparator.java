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

import static org.junit.Assert.assertTrue;

public abstract class ValueComparator {
    public static final ValueComparator DEFAULT =
            new ValueComparator() {
                @Override
                public boolean equals(final Object expected,
                                      final Object actual) {
                    return (expected == actual)
                            || (expected != null) && expected.equals(actual);
                }
            };

    public abstract boolean equals(Object expected, Object actual);

    public final void assertEquals(final Object expected,
                                   final Object actual) {
        assertTrue("expected=" + expected + " actual=" + actual,
                   equals(expected, actual));

    }
}
