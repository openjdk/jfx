/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
