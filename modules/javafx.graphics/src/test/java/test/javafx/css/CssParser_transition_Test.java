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

package test.javafx.css;

import org.junit.jupiter.api.Test;
import javafx.animation.Interpolator;
import javafx.animation.StepPosition;
import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.Rule;
import javafx.css.Stylesheet;
import javafx.css.TransitionDefinition;
import javafx.css.TransitionPropertySelector;
import javafx.util.Duration;

import static test.javafx.animation.InterpolatorUtils.*;
import static javafx.util.Duration.*;
import static org.junit.jupiter.api.Assertions.*;

public class CssParser_transition_Test {

    private Stylesheet parse(String stylesheetText) {
        CssParser.errorsProperty().clear();
        return new CssParser().parse(stylesheetText);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] values(String property, Rule rule) {
        for (Declaration decl : rule.getDeclarations()) {
            if (decl.getProperty().equals(property)) {
                return (T[])decl.getParsedValue().convert(null);
            }
        }

        fail("Property not found");
        return (T[])new Object[0];
    }

    private void assertTransition(TransitionDefinition expected, TransitionDefinition actual) {
        assertEquals(expected.getProperty(), actual.getProperty());
        assertEquals(expected.getDuration(), actual.getDuration());
        assertEquals(expected.getDelay(), actual.getDelay());
        assertInterpolatorEquals(expected.getInterpolator(), actual.getInterpolator());
    }

    private void assertStartsWith(String expected, String actual) {
        assertTrue(actual.startsWith(expected), "Expected: " + expected + ", but was: " + actual);
    }

    /*
     * Default values specified by https://www.w3.org/TR/css-transitions-1
     *
     *     transition-property: 'all'
     *     transition-duration: '0s'
     *     transition-timing-function: 'ease'
     *     transition-delay: '0s'
     */
    @Test
    public void testDefaultValues() {
        Stylesheet stylesheet = parse("""
            .rule1 { transition: foo; }
            .rule2 { transition: 1s; }
            .rule3 { transition: linear; }
        """);

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.CSS, "foo", seconds(0), seconds(0), EASE),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(0)))[0]);

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.ALL, "all", seconds(1), seconds(0), EASE),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(1)))[0]);

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.ALL, "all", seconds(0), seconds(0), LINEAR),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(2)))[0]);
    }

    @Test
    public void testTransitionDuration() {
        Stylesheet stylesheet = parse("""
            .rule1 { transition-duration: 1s; }
            .rule2 { transition-duration: 1s, 0.5s, 0.25ms, 0s; }
            .rule3 { transition-duration: indefinite; }
            .err1 { transition-duration: 10; }
            .err2 { transition-duration: -5s; }
        """);

        assertArrayEquals(new Duration[] { seconds(1) },
            values("transition-duration", stylesheet.getRules().get(0)));

        assertArrayEquals(new Duration[] { seconds(1), seconds(0.5), millis(0.25), ZERO },
            values("transition-duration", stylesheet.getRules().get(1)));

        assertArrayEquals(new Duration[] { INDEFINITE },
            values("transition-duration", stylesheet.getRules().get(2)));

        assertStartsWith("Expected '<duration>'", CssParser.errorsProperty().get(0).getMessage());
        assertStartsWith("Invalid '<duration>'", CssParser.errorsProperty().get(2).getMessage());
    }

    @Test
    public void testTransitionDelay() {
        Stylesheet stylesheet = parse("""
            .rule1 { transition-delay: 1s; }
            .rule2 { transition-delay: 1s, 0.5s, 0.25ms; }
            .rule3 { transition-delay: 10; }
        """);

        assertArrayEquals(new Duration[] { seconds(1) },
            values("transition-delay", stylesheet.getRules().get(0)));

        assertArrayEquals(new Duration[] { seconds(1), seconds(0.5), millis(0.25) },
            values("transition-delay", stylesheet.getRules().get(1)));

        assertStartsWith("Expected '<duration>'", CssParser.errorsProperty().get(0).getMessage());
    }

    @Test
    public void testTransitionProperty() {
        Stylesheet stylesheet = parse("""
            .rule1 { transition-property: foo; }
            .rule2 { transition-property: foo, bar, baz; }
            .rule3 { transition-property: 10; }
        """);

        assertArrayEquals(new String[] {"foo"},
            values("transition-property", stylesheet.getRules().get(0)));

        assertArrayEquals(new String[] {"foo", "bar", "baz"},
            values("transition-property", stylesheet.getRules().get(1)));

        assertStartsWith("Expected '<transition-property>'", CssParser.errorsProperty().get(0).getMessage());
    }

    @Test
    public void testTransitionTimingFunction() {
        Stylesheet stylesheet = parse("""
            .rule1 { transition-timing-function: linear; }
            .rule2 { transition-timing-function: ease, ease-in, ease-out, ease-in-out, cubic-bezier(0.1, 0.2, 0.3, 0.4); }
            .rule3 { transition-timing-function: step-start, step-end,
                                                 steps(3, jump-start), steps(3, jump-end),
                                                 steps(3, jump-none), steps(3, jump-both),
                                                 steps(3, start), steps(3, end); }
            .rule4 { transition-timing-function: steps(3); }
            .err1 { transition-timing-function: cubic-bezier(2, 0, 0, 0); }
            .err2 { transition-timing-function: steps(2, 3); }
            .err3 { transition-timing-function: steps(1, foo); }
            .err4 { transition-timing-function: steps(foo, start); }
        """);

        Interpolator[] values = values("transition-timing-function", stylesheet.getRules().get(0));
        assertInterpolatorEquals(LINEAR, values[0]);

        values = values("transition-timing-function", stylesheet.getRules().get(1));
        assertInterpolatorEquals(EASE, values[0]);
        assertInterpolatorEquals(EASE_IN, values[1]);
        assertInterpolatorEquals(EASE_OUT, values[2]);
        assertInterpolatorEquals(EASE_IN_OUT, values[3]);
        assertInterpolatorEquals(CUBIC_BEZIER(0.1, 0.2, 0.3, 0.4), values[4]);

        values = values("transition-timing-function", stylesheet.getRules().get(2));
        assertInterpolatorEquals(STEP_START, values[0]);
        assertInterpolatorEquals(STEP_END, values[1]);
        assertInterpolatorEquals(STEPS(3, StepPosition.START), values[2]);
        assertInterpolatorEquals(STEPS(3, StepPosition.END), values[3]);
        assertInterpolatorEquals(STEPS(3, StepPosition.NONE), values[4]);
        assertInterpolatorEquals(STEPS(3, StepPosition.BOTH), values[5]);
        assertInterpolatorEquals(STEPS(3, StepPosition.START), values[6]);
        assertInterpolatorEquals(STEPS(3, StepPosition.END), values[7]);

        values = values("transition-timing-function", stylesheet.getRules().get(3));
        assertInterpolatorEquals(STEPS(3, StepPosition.END), values[0]);

        assertStartsWith("Expected '<number [0,1]>'", CssParser.errorsProperty().get(0).getMessage());
        assertStartsWith("Expected '<step-position>'", CssParser.errorsProperty().get(2).getMessage());
        assertStartsWith("Expected '<step-position>'", CssParser.errorsProperty().get(4).getMessage());
        assertStartsWith("Expected '<integer>'", CssParser.errorsProperty().get(6).getMessage());
    }

    @Test
    public void testShorthandTransition() {
        Stylesheet stylesheet = parse("""
            .rule0 { transition: all 0.25s; }
            .rule1 { transition: none 0.25s; }
            .rule2 { transition: ease all 125ms; }
            .rule3 { transition: foo 0.3s 0.4s cubic-bezier(0.1, 0.2, 0.3, .4); }
            .rule4 { transition: 0.3s foo cubic-bezier(0.1, 0.2, 0.3, .4) 0.4s; }
            .rule5 { transition: linear linear 0.5s; }
            .rule6 { transition: foo bar; }
        """);

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.ALL, null, seconds(0.25), seconds(0), EASE),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(0)))[0]);

        assertEquals("null",
            stylesheet.getRules().get(1).getDeclarations().get(0).getParsedValue().convert(null));

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.ALL, null, seconds(0.125), seconds(0), EASE),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(2)))[0]);

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.BEAN, "foo", seconds(0.3), seconds(0.4),
                                     CUBIC_BEZIER(0.1, 0.2, 0.3, .4)),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(3)))[0]);

        assertTransition(
            new TransitionDefinition(TransitionPropertySelector.BEAN, "foo", seconds(0.3), seconds(0.4),
                                     CUBIC_BEZIER(0.1, 0.2, 0.3, .4)),
            ((TransitionDefinition[])values("transition", stylesheet.getRules().get(4)))[0]);

        assertStartsWith("Expected '<single-transition-property>'", CssParser.errorsProperty().get(0).getMessage());
        assertStartsWith("Expected '<easing-function>'", CssParser.errorsProperty().get(2).getMessage());
    }

}
