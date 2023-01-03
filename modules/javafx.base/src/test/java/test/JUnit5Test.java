/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

public class JUnit5Test {

    @Test
    void junit5ShouldWork() {
        assumeTrue(this != null);

        assertNotNull(this);
        System.err.println("JUnit 5 test working!");
    }

    static int callCount;
    static int[] intValues = {1, 2, 3};

    @ValueSource(ints = {1, 2, 3})
    @ParameterizedTest
    void testParameterizedTest(int value) {
        boolean match = false;
        for (int i = 0; i < intValues.length; i++) {
            if (value == intValues[i]) {
                match = true;
                intValues[i] = 0;
                break;
            }
        }
        callCount++;

        assertTrue(match, "Received incorrect value as parameter");
        assertTrue(callCount <= intValues.length, "Test function called more than number of ValueSources");
        if (callCount == intValues.length) {
            for (int i : intValues) {
                assertEquals(0, i, "Test method not called for Value " + i);
            }
        }
    }
}
