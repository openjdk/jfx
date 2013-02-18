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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 */
@RunWith(Parameterized.class)
public class IndexRangeValueOfTest {
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters public static Collection implementations() {
        // Valid strings are ones where the string contains only 2 numbers separated by a comma and
        // have only whitespace surrounding the numbers / comma. Any other characters will lead to
        // an exception.

        // We will use a couple numbers to test:
            // 10, 20, 1, -10, -20, -1, and 0.
        // We will construct the cross product of all valid combinations
        // We will then take this cross product and cross it with the following rules:
            // Just a number
            // A number may have only padding
            // A number may have an errant "a" character before or after it
            // A comma may be represented twice
        // We'll combine those into a big set of data, and then parametrize over it.

        int[] numbers = new int[] { 10, 20, 1, -10, -20, -1, 0};
        int[] rules = new int[] {0, 1, 2, 3};

        List params = new LinkedList();
        for (int i=0; i<numbers.length; i++) {
            for (int j=0; j<numbers.length; j++) {
                for (int k=0; k<rules.length; k++) {
                    final int start = numbers[i];
                    final int end = numbers[j];
                    final int rule = rules[k];

                    TestParameters param = new TestParameters();
                    switch(rule) {
                        case 0:
                            param.string = start + "," + end;
                            param.expected = IndexRange.normalize(start, end);
                            break;
                        case 1:
                            param.string = " " + start + " , " + end;
                            param.expected = IndexRange.normalize(start, end);
                            break;
                        case 2:
                            param.string = "a" + start + "," + end + "a";
                            break;
                        case 3:
                            param.string = start + ",," + end;
                            break;
                    }
                    params.add(new Object[] {param});
                }
            }
        }

        return params;
    }

    private TestParameters params;

    public IndexRangeValueOfTest(TestParameters params) {
        this.params = params;
    }

    @Test public void testValueOf() {
        if (params.expected == null) {
            try {
                IndexRange.valueOf(params.string);
                assertTrue(false);
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        } else {
            IndexRange range = IndexRange.valueOf(params.string);
            assertEquals(params.expected, range);
        }
    }

    private static final class TestParameters {
        private String string;
        private IndexRange expected; // if null, then we don't expect this to be a valid string
    }
}
