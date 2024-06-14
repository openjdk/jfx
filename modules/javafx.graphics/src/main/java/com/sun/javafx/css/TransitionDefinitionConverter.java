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

package com.sun.javafx.css;

import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.scene.text.Font;
import javafx.util.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * Converts an array of parsed values to a {@link TransitionDefinition}.
 * The array must contain four elements, all of which may be {@code null}:
 * <ol>
 *     <li>property: {@code ParsedValue<?, String>}
 *     <li>duration: {@code ParsedValue<?, Duration>}
 *     <li>delay: {@code ParsedValue<?, Duration>}
 *     <li>timingFunction: {@code ParsedValue<?, Interpolator>}
 * </ol>
 */
@SuppressWarnings("rawtypes")
public final class TransitionDefinitionConverter extends StyleConverter<ParsedValue[], TransitionDefinition> {

    public static final String PROPERTY_ALL = "all";

    private static class Holder {
        static final TransitionDefinitionConverter INSTANCE = new TransitionDefinitionConverter();
        static final SequenceConverter SEQUENCE_INSTANCE = new SequenceConverter();
    }

    public static StyleConverter<ParsedValue[], TransitionDefinition> getInstance() {
        return Holder.INSTANCE;
    }

    private TransitionDefinitionConverter() {}

    @Override
    @SuppressWarnings("unchecked")
    public TransitionDefinition convert(ParsedValue<ParsedValue[], TransitionDefinition> value, Font font) {
        ParsedValue[] values = value.getValue();
        ParsedValue<?, String> parsedProperty = values[0];
        ParsedValue<ParsedValue<?, Size>, Duration> parsedDuration = values[1];
        ParsedValue<ParsedValue<?, Size>, Duration> parsedDelay = values[2];
        ParsedValue<?, Interpolator> parsedInterpolator = values[3];

        String propertyName = parsedProperty != null ? parsedProperty.convert(null) : PROPERTY_ALL;
        Duration duration = parsedDuration != null ? parsedDuration.convert(null) : Duration.ZERO;
        Duration delay = parsedDelay != null ? parsedDelay.convert(null) : Duration.ZERO;
        Interpolator interpolator = parsedInterpolator != null ?
            parsedInterpolator.convert(null) : InterpolatorConverter.CSS_EASE;

        return new TransitionDefinition(
            propertyName.intern(), duration.lessThan(Duration.ZERO) ? Duration.ZERO : duration, delay, interpolator);
    }

    /**
     * Converts a sequence of parsed values to an array of {@link TransitionDefinition} instances.
     */
    public static final class SequenceConverter
            extends StyleConverter<ParsedValue<ParsedValue[], TransitionDefinition>[], TransitionDefinition[]> {
        private static final TransitionDefinition[] EMPTY_TRANSITION = new TransitionDefinition[0];
        private static final String[] EMPTY_STRING = new String[0];
        private static final Duration[] EMPTY_DURATION = new Duration[0];
        private static final Interpolator[] EMPTY_INTERPOLATOR = new Interpolator[0];

        public static SequenceConverter getInstance() {
            return Holder.SEQUENCE_INSTANCE;
        }

        private SequenceConverter() {}

        @Override
        public TransitionDefinition[] convert(Map<CssMetaData<? extends Styleable, ?>, Object> convertedValues) {
            String[] properties = EMPTY_STRING;
            Duration[] durations = EMPTY_DURATION;
            Duration[] delays = EMPTY_DURATION;
            Interpolator[] timingFunctions = EMPTY_INTERPOLATOR;

            for (Map.Entry<CssMetaData<? extends Styleable, ?>, Object> entry : convertedValues.entrySet()) {
                switch (entry.getKey().getProperty()) {
                    case "transition-property" -> properties = (String[]) entry.getValue();
                    case "transition-duration" -> durations = (Duration[]) entry.getValue();
                    case "transition-delay" -> delays = (Duration[]) entry.getValue();
                    case "transition-timing-function" -> timingFunctions = (Interpolator[]) entry.getValue();
                }
            }

            if (properties.length == 0 || durations.length == 0) {
                return EMPTY_TRANSITION;
            }

            // The length of the 'transition-property' list determines the number of transitions in the sequence.
            TransitionDefinition[] transitions = new TransitionDefinition[properties.length];

            // If any of the remaining sub-properties doesn't have enough values, missing values are filled in
            // by repeating the list of values until we have enough values for the sequence.
            for (int i = 0; i < transitions.length; ++i) {
                Interpolator timingFunction = timingFunctions.length == 0 ?
                    InterpolatorConverter.CSS_EASE : timingFunctions[i % timingFunctions.length];
                Duration duration = durations[i % durations.length];
                Duration delay = delays.length == 0 ? Duration.ZERO : delays[i % delays.length];
                transitions[i] = new TransitionDefinition(properties[i], duration, delay, timingFunction);
            }

            return transitions;
        }

        @Override
        public TransitionDefinition[] convert(
                ParsedValue<ParsedValue<ParsedValue[], TransitionDefinition>[], TransitionDefinition[]> value,
                Font font) {
            ParsedValue<ParsedValue[], TransitionDefinition>[] layers = value.getValue();
            if (layers.length == 0) {
                return EMPTY_TRANSITION;
            }

            TransitionDefinition[] transitions = new TransitionDefinition[layers.length];
            int numTransitions = 0;

            for (ParsedValue<ParsedValue[], TransitionDefinition> layer : layers) {
                TransitionDefinition transition = TransitionDefinitionConverter.getInstance().convert(layer, font);
                if (transition != null) {
                    transitions[numTransitions++] = transition;
                }
            }

            return numTransitions == transitions.length ?
                   transitions : Arrays.copyOf(transitions, numTransitions);
        }
    }

}
