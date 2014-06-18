/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.text;

import java.text.BreakIterator;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * A unit test for the {@link TextBreakIterator} class.
 */
public class TextBreakIteratorTest {

    private static final int[] ITERATOR_TYPES = {
        TextBreakIterator.CHARACTER_ITERATOR,
        TextBreakIterator.WORD_ITERATOR,
        TextBreakIterator.LINE_ITERATOR,
        TextBreakIterator.SENTENCE_ITERATOR,
    };

    /**
     * For each iterator type, tests the {@code TEXT_BREAK_PRECEDING} method
     * with the {@code pos} argument greater than the string length.
     */
    @Test
    public void testBreakPrecedingFromGreaterThanStringLengthPosition() {
        int method = TextBreakIterator.TEXT_BREAK_PRECEDING;
        for (int type : ITERATOR_TYPES) {
            String[] strings = new String[] {
                "", "a", "aa", "a a", "a a. a a."
            };
            for (String string : strings) {
                int length = string.length();
                BreakIterator it =
                        TextBreakIterator.getIterator(type, "en-US", string, false);
                int[] positions = new int[] {
                    length + 1, length + 2, length + 10
                };
                for (int position : positions) {
                    int result = TextBreakIterator.invokeMethod(
                            it, method, position);
                    assertEquals("Unexpected result, type: " + type
                            + ", string: " + string + ", position: " + position,
                            length, result);
                }
            }
        }
    }
}
