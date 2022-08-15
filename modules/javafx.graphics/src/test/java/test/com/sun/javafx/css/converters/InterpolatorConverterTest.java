/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css.converters;

import com.sun.javafx.css.InterpolatorConverter;
import com.sun.javafx.css.ParsedValueImpl;
import org.junit.jupiter.api.Test;
import javafx.animation.Interpolator;
import javafx.animation.StepPosition;
import javafx.css.ParsedValue;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static test.javafx.animation.InterpolatorUtils.*;

public class InterpolatorConverterTest {

    @Test
    public void testConvertLinearInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("linear", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(LINEAR, result);
    }

    @Test
    public void testConvertEaseInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("ease", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(EASE, result);
    }

    @Test
    public void testConvertEaseInInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("ease-in", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(EASE_IN, result);
    }

    @Test
    public void testConvertEaseOutInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("ease-out", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(EASE_OUT, result);
    }

    @Test
    public void testConvertEaseInOutInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("ease-in-out", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(EASE_IN_OUT, result);
    }

    @Test
    public void testConvertCubicBezierInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValue[] {
            new ParsedValueImpl<>("cubic-bezier(", null),
            new ParsedValueImpl<>(new double[] {0.1, 0.2, 0.3, 0.4}, null) },
            null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(CUBIC_BEZIER(0.1, 0.2, 0.3, 0.4), result);
    }

    @Test
    public void testConvertStepStartInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("step-start", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(STEP_START, result);
    }

    @Test
    public void testConvertStepEndInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("step-end", null), null);
        var result = InterpolatorConverter.getInstance().convert(value, null);
        assertInterpolatorEquals(STEP_END, result);
    }

    @Test
    public void testConvertStepsInterpolator() {
        for (var stepPosition : StepPosition.values()) {
            var cssName = "jump-" + stepPosition.toString().toLowerCase(Locale.ROOT).replace('_', '-');
            var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValue[] {
                new ParsedValueImpl<>("steps(", null),
                new ParsedValueImpl<>(new Object[] {3, cssName}, null) },
                null);
            var result = InterpolatorConverter.getInstance().convert(value, null);
            assertInterpolatorEquals(STEPS(3, stepPosition), result);
        }
    }

    @Test
    public void testRepeatedConversionReturnsCachedInterpolator() {
        var value = new ParsedValueImpl<Object, Interpolator>(new ParsedValue[] {
            new ParsedValueImpl<>("cubic-bezier(", null),
            new ParsedValueImpl<>(new double[] {0.1, 0.2, 0.3, 0.4}, null) },
            null);
        var result1 = InterpolatorConverter.getInstance().convert(value, null);
        var result2 = InterpolatorConverter.getInstance().convert(value, null);
        assertSame(result1, result2);
    }

    @Test
    public void testConvertInterpolatorSequence() {
        var value1 = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("linear", null), null);
        var value2 = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("ease", null), null);
        var value3 = new ParsedValueImpl<Object, Interpolator>(new ParsedValueImpl<>("ease-in", null), null);
        ParsedValue<ParsedValue<?, Interpolator>[], Interpolator[]> values = new ParsedValueImpl<>(
            new ParsedValue[] { value1, value2, value3 }, null);

        var result = InterpolatorConverter.SequenceConverter.getInstance().convert(values, null);
        assertEquals(3, result.length);
        assertInterpolatorEquals(LINEAR, result[0]);
        assertInterpolatorEquals(EASE, result[1]);
        assertInterpolatorEquals(EASE_IN, result[2]);
    }

}
