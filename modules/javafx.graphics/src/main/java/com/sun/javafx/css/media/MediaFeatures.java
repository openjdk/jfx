/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.media;

import com.sun.javafx.css.media.expression.FunctionExpression;
import javafx.application.ColorScheme;
import java.util.Locale;
import java.util.function.Function;

/**
 * Contains the implementations of all supported media feature queries.
 */
final class MediaFeatures {

    private MediaFeatures() {}

    /**
     * Returns a {@code MediaQuery} that evaluates the specified feature.
     *
     * @param featureName the name of the media feature
     * @param featureValue the value of the media feature, or {@code null} to indicate no value
     * @throws IllegalArgumentException if {@code featureName} or {@code featureValue} is invalid
     * @return the {@code MediaQuery}
     */
    public static MediaQuery featureQueryExpression(String featureName, String featureValue) {
        featureName = featureName.toLowerCase(Locale.ROOT).intern();

        if (featureValue != null) {
            featureValue = featureValue.toLowerCase(Locale.ROOT).intern();
        }

        return switch (featureName) {
            case "prefers-color-scheme" -> new FunctionExpression<>(
                featureName,
                checkNotNullValue(featureName, featureValue),
                MediaQueryContext::getColorScheme,
                enumValue(ColorScheme::valueOf, featureName, featureValue));

            case "prefers-reduced-motion" -> booleanPreferenceExpression(
                featureName, featureValue, "reduce", MediaQueryContext::isReducedMotion);

            case "prefers-reduced-transparency" -> booleanPreferenceExpression(
                featureName, featureValue, "reduce", MediaQueryContext::isReducedTransparency);

            case "prefers-reduced-data" -> booleanPreferenceExpression(
                featureName, featureValue, "reduce", MediaQueryContext::isReducedData);

            case "-fx-prefers-persistent-scrollbars" -> booleanPreferenceExpression(
                featureName, featureValue, "persistent", MediaQueryContext::isPersistentScrollBars);

            default -> throw new IllegalArgumentException(
                String.format("Unknown media feature <%s>", featureName));
        };
    }

    private static MediaQuery booleanPreferenceExpression(String featureName,
                                                          String featureValue,
                                                          String trueValue,
                                                          Function<MediaQueryContext, Boolean> argument) {
        if ("no-preference".equals(featureValue)) {
            return new FunctionExpression<>(featureName, featureValue, argument, false);
        }

        if (featureValue == null || trueValue.equals(featureValue)) {
            return new FunctionExpression<>(featureName, featureValue, argument, true);
        }

        throw new IllegalArgumentException(
            String.format("Unknown value <%s> for media feature <%s>", featureValue, featureName));
    }

    private static String checkNotNullValue(String featureName, String featureValue) {
        if (featureValue == null) {
            throw new IllegalArgumentException(
                String.format("Media feature <%s> cannot be evaluated in a boolean context", featureName));
        }

        return featureValue;
    }

    private static <T extends Enum<T>> T enumValue(Function<String, T> func, String featureName, String featureValue) {
        try {
            return func.apply(featureValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("Unknown value <%s> for media feature <%s>", featureValue, featureName));
        }
    }
}
