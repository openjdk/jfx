/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
