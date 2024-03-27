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
import javafx.animation.Interpolator.StepPosition;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a parsed value to an {@link Interpolator}.
 * <p>
 * If the value is a {@code ParsedValue}, its value represents one of the singleton instances:
 * <ol>
 *     <li>{@code linear}
 *     <li>{@code ease}, {@code ease-in}, {@code ease-out}, {@code ease-in-out}
 *     <li>{@code step-start}, {@code step-end}
 * </ol>
 * <p>
 * If the value is a {@code ParsedValue} array, the first element represents the name of the function
 * ({@code cubic-bezier} or {@code steps}), and the second element contains a list of arguments.
 */
public class InterpolatorConverter extends StyleConverter<Object, Interpolator> {

    private static class Holder {
        static final InterpolatorConverter INSTANCE = new InterpolatorConverter();
        static final SequenceConverter SEQUENCE_INSTANCE = new SequenceConverter();
    }

    public static StyleConverter<Object, Interpolator> getInstance() {
        return Holder.INSTANCE;
    }

    // We're using the CSS definitions of EASE_IN and EASE_OUT here, which are different from
    // SMIL 3.0's definitions that are used for Interpolator.EASE_IN and Interpolator.EASE_OUT.
    // https://www.w3.org/TR/css-easing-1/#cubic-bezier-easing-functions
    //
    public static final Interpolator CSS_EASE = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    public static final Interpolator CSS_EASE_IN = Interpolator.SPLINE(0.42, 0, 1, 1);
    public static final Interpolator CSS_EASE_OUT = Interpolator.SPLINE(0, 0, 0.58, 1);
    public static final Interpolator CSS_EASE_IN_OUT = Interpolator.SPLINE(0.42, 0, 0.58, 1);

    // We're using an LRU cache (least recently used) to limit the number of redundant instances.
    private static final Map<ParsedValue<?, ?>, Interpolator> CACHE = new LinkedHashMap<>(10, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ParsedValue<?, ?>, Interpolator> eldest) {
            return size() > 20;
        }
    };

    private InterpolatorConverter() {}

    @Override
    public Interpolator convert(ParsedValue<Object, Interpolator> value, Font font) {
        if (value.getValue() instanceof ParsedValue<?, ?> pv && pv.getValue() instanceof String name) {
            return switch (name) {
                case "ease" -> CSS_EASE;
                case "ease-in" -> CSS_EASE_IN;
                case "ease-out" -> CSS_EASE_OUT;
                case "ease-in-out" -> CSS_EASE_IN_OUT;
                case "step-start" -> Interpolator.STEP_START;
                case "step-end" -> Interpolator.STEP_END;
                case "linear" -> Interpolator.LINEAR;
                case "-fx-ease-in" -> Interpolator.EASE_IN;
                case "-fx-ease-out" -> Interpolator.EASE_OUT;
                case "-fx-ease-both" -> Interpolator.EASE_BOTH;
                default -> throw new AssertionError();
            };
        }

        if (value.getValue() instanceof ParsedValue<?, ?>[] pv && pv[0].getValue() instanceof String funcName) {
            return switch (funcName) {
                case "cubic-bezier(" -> CACHE.computeIfAbsent(value, key -> {
                    List<Double> args = arguments(key);
                    return Interpolator.SPLINE(args.get(0), args.get(1), args.get(2), args.get(3));
                });

                case "steps(" -> CACHE.computeIfAbsent(value, key -> {
                    List<Object> args = arguments(key);
                    String position = args.get(1) != null ? (String)args.get(1) : "end";
                    return Interpolator.STEPS((int)args.get(0), switch (position) {
                        case "jump-start", "start" -> StepPosition.START;
                        case "jump-both" -> StepPosition.BOTH;
                        case "jump-none" -> StepPosition.NONE;
                        default -> StepPosition.END;
                    });
                });

                default -> throw new AssertionError();
            };
        }

        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> arguments(ParsedValue<?, ?> value) {
        ParsedValue<?, ?>[] values = (ParsedValue<?, ?>[])value.getValue();
        return (List<T>)values[1].getValue();
    }

    /**
     * Converts a sequence of parsed values to an array of {@link Interpolator} instances.
     */
    public static final class SequenceConverter
            extends StyleConverter<ParsedValue<?, Interpolator>[], Interpolator[]> {
        public static SequenceConverter getInstance() {
            return Holder.SEQUENCE_INSTANCE;
        }

        private SequenceConverter() {}

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Interpolator[] convert(
                ParsedValue<ParsedValue<?, Interpolator>[], Interpolator[]> value,
                Font font) {
            ParsedValue[] layers = value.getValue();
            Interpolator[] interpolators = new Interpolator[layers.length];

            for (int layer = 0; layer < layers.length; layer++) {
                interpolators[layer] = InterpolatorConverter.getInstance().convert(layers[layer], font);
            }

            return interpolators;
        }
    }
}
