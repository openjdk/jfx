/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionDefinitionConverter;
import com.sun.javafx.css.TransitionDefinitionCssMetaData;
import com.sun.javafx.css.InterpolatorConverter;
import com.sun.javafx.css.ParsedValueImpl;
import javafx.scene.Node;
import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.converter.DurationConverter;
import javafx.util.Duration;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.sun.javafx.css.InterpolatorConverter.*;
import static com.sun.javafx.css.TransitionDefinitionConverter.SequenceConverter;
import static test.javafx.animation.InterpolatorUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class TransitionDefinitionConverterTest {

    private static void assertTransitionEquals(
            String property, Duration duration, Duration delay, Interpolator interpolator,
            TransitionDefinition transition) {
        assertEquals(property, transition.propertyName());
        assertEquals(duration, transition.duration());
        assertEquals(delay, transition.delay());
        assertInterpolatorEquals(interpolator, transition.interpolator());
    }

    @Test
    public void testConvertParsedValuesToImplicitTransitionWithDefaults() {
        var value = new ParsedValueImpl<ParsedValue[], TransitionDefinition>(new ParsedValue[] {
            new ParsedValueImpl<String, String>("test", null),
            new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(1, SizeUnits.S), null), DurationConverter.getInstance()),
            null,
            null
        }, null);

        var result = TransitionDefinitionConverter.getInstance().convert(value, null);
        assertTransitionEquals("test", Duration.seconds(1), Duration.ZERO, CSS_EASE, result);
    }

    @Test
    public void testConvertParsedValuesToImplicitTransition() {
        var value = new ParsedValueImpl<ParsedValue[], TransitionDefinition>(new ParsedValue[] {
            new ParsedValueImpl<String, String>("test", null),
            new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(1, SizeUnits.S), null), DurationConverter.getInstance()),
            new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(0.25, SizeUnits.S), null), DurationConverter.getInstance()),
            new ParsedValueImpl<>(new ParsedValueImpl<String, String>("ease-in", null), InterpolatorConverter.getInstance()),
        }, null);

        var result = TransitionDefinitionConverter.getInstance().convert(value, null);
        assertTransitionEquals("test", Duration.seconds(1), Duration.seconds(0.25), CSS_EASE_IN, result);
    }

    @Test
    public void testConvertParsedValuesWithNegativeDurationIsCoercedToZeroDuration() {
        var value = new ParsedValueImpl<ParsedValue[], TransitionDefinition>(new ParsedValue[] {
            new ParsedValueImpl<String, String>("test", null),
            new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(-1, SizeUnits.S), null), DurationConverter.getInstance()),
            null,
            null
        }, null);

        var transition = TransitionDefinitionConverter.getInstance().convert(value, null);
        assertEquals(Duration.ZERO, transition.duration());
    }

    @Nested
    class SequenceConverterTest {

        @Test
        public void testConvertParsedValuesToImplicitTransitions() {
            var value1 = new ParsedValueImpl<ParsedValue[], TransitionDefinition>(new ParsedValue[] {
                new ParsedValueImpl<String, String>("test1", null),
                new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(1, SizeUnits.S), null), DurationConverter.getInstance()),
                null,
                null
            }, null);

            var value2 = new ParsedValueImpl<ParsedValue[], TransitionDefinition>(new ParsedValue[] {
                new ParsedValueImpl<String, String>("test2", null),
                new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(0.5, SizeUnits.S), null), DurationConverter.getInstance()),
                new ParsedValueImpl<>(new ParsedValueImpl<>(new Size(-1.5, SizeUnits.S), null), DurationConverter.getInstance()),
                new ParsedValueImpl<>(new ParsedValueImpl<String, String>("ease-in", null), InterpolatorConverter.getInstance()),
            }, null);

            ParsedValue<ParsedValue<ParsedValue[], TransitionDefinition>[], TransitionDefinition[]> values =
                new ParsedValueImpl<>(new ParsedValue[] { value1, value2 }, null);

            var result = SequenceConverter.getInstance().convert(values, null);
            assertEquals(2, result.length);
            assertTransitionEquals("test1", Duration.seconds(1), Duration.ZERO, CSS_EASE, result[0]);
            assertTransitionEquals("test2", Duration.seconds(0.5), Duration.seconds(-1.5), CSS_EASE_IN, result[1]);
        }

        @Test
        public void testConvertSubPropertiesToImplicitTransitionsWithDefaults() {
            var metadata = new TestImplicitTransitionCssMetaData();
            Map<CssMetaData<? extends Styleable, ?>, Object> values = Map.of(
                metadata.transitionProperty(), new String[] { "test" },
                metadata.transitionDuration(), new Duration[] { Duration.seconds(1) }
            );

            var result = SequenceConverter.getInstance().convert(values);
            assertEquals(1, result.length);
            assertTransitionEquals("test", Duration.seconds(1), Duration.ZERO, CSS_EASE, result[0]);
        }

        @Test
        public void testConvertSubPropertiesToImplicitTransitions() {
            var metadata = new TestImplicitTransitionCssMetaData();
            Map<CssMetaData<? extends Styleable, ?>, Object> values = Map.of(
                metadata.transitionProperty(), new String[] { "test" },
                metadata.transitionDuration(), new Duration[] { Duration.seconds(1) },
                metadata.transitionDelay(), new Duration[] { Duration.seconds(0.25) },
                metadata.transitionTimingFunction(), new Interpolator[] { CSS_EASE_IN }
            );

            var result = SequenceConverter.getInstance().convert(values);
            assertEquals(1, result.length);
            assertTransitionEquals("test", Duration.seconds(1), Duration.seconds(0.25), CSS_EASE_IN, result[0]);
        }

        @Test
        public void testUnmatchedValuesAreRepeated() {
            var metadata = new TestImplicitTransitionCssMetaData();
            Map<CssMetaData<? extends Styleable, ?>, Object> values = Map.of(
                metadata.transitionProperty(), new String[] { "test1", "test2", "test3", "test4" },
                metadata.transitionDuration(), new Duration[] { Duration.seconds(1), Duration.seconds(2) },
                metadata.transitionDelay(), new Duration[] { Duration.seconds(0.25) },
                metadata.transitionTimingFunction(), new Interpolator[] { CSS_EASE_IN, CSS_EASE_OUT }
            );

            var result = SequenceConverter.getInstance().convert(values);
            assertEquals(4, result.length);
            assertTransitionEquals("test1", Duration.seconds(1), Duration.seconds(0.25), CSS_EASE_IN, result[0]);
            assertTransitionEquals("test2", Duration.seconds(2), Duration.seconds(0.25), CSS_EASE_OUT, result[1]);
            assertTransitionEquals("test3", Duration.seconds(1), Duration.seconds(0.25), CSS_EASE_IN, result[2]);
            assertTransitionEquals("test4", Duration.seconds(2), Duration.seconds(0.25), CSS_EASE_OUT, result[3]);
        }

        @Test
        public void testConvertSubPropertiesWithMissingPropertyNameYieldsNoResult() {
            var metadata = new TestImplicitTransitionCssMetaData();
            Map<CssMetaData<? extends Styleable, ?>, Object> values = Map.of(
                metadata.transitionDuration(), new Duration[] { Duration.seconds(1) }
            );

            var result = SequenceConverter.getInstance().convert(values);
            assertEquals(0, result.length);
        }

        @Test
        public void testConvertSubPropertiesWithMissingDurationYieldsNoResult() {
            var metadata = new TestImplicitTransitionCssMetaData();
            Map<CssMetaData<? extends Styleable, ?>, Object> values = Map.of(
                metadata.transitionProperty(), new String[] { "test" }
            );

            var result = SequenceConverter.getInstance().convert(values);
            assertEquals(0, result.length);
        }

        private static class TestImplicitTransitionCssMetaData extends TransitionDefinitionCssMetaData {
            @Override
            public boolean isSettable(Node styleable) {
                return false;
            }

            @Override
            public StyleableProperty<TransitionDefinition[]> getStyleableProperty(Node styleable) {
                return null;
            }

            CssMetaData<? extends Styleable, ?> transitionProperty() {
                return getSubProperties().stream()
                    .filter(p -> p.getProperty().equals("transition-property"))
                    .findFirst()
                    .orElseThrow();
            }

            CssMetaData<? extends Styleable, ?> transitionDuration() {
                return getSubProperties().stream()
                    .filter(p -> p.getProperty().equals("transition-duration"))
                    .findFirst()
                    .orElseThrow();
            }

            CssMetaData<? extends Styleable, ?> transitionDelay() {
                return getSubProperties().stream()
                    .filter(p -> p.getProperty().equals("transition-delay"))
                    .findFirst()
                    .orElseThrow();
            }

            CssMetaData<? extends Styleable, ?> transitionTimingFunction() {
                return getSubProperties().stream()
                    .filter(p -> p.getProperty().equals("transition-timing-function"))
                    .findFirst()
                    .orElseThrow();
            }
        }

    }

}
