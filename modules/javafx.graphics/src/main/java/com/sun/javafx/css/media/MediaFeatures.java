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

import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.RangeExpression;
import javafx.application.ColorScheme;
import javafx.css.Size;
import javafx.css.SizeUnits;
import java.util.Locale;
import java.util.function.Function;

/**
 * Contains the implementations of all supported media feature queries.
 */
final class MediaFeatures {

    private MediaFeatures() {}

    /**
     * Returns a {@code MediaQuery} that evaluates the specified feature in a discrete context.
     *
     * @param featureName the name of the media feature
     * @param featureValue the value of the media feature, or {@code null} to indicate no value
     * @throws IllegalArgumentException if {@code featureName} or {@code featureValue} is invalid
     * @return the {@code MediaQuery}
     */
    public static MediaQuery discreteQueryExpression(String featureName, String featureValue) {
        String lowerCaseFeatureName = lowerCaseTextValue(featureName);
        String lowerCaseFeatureValue = lowerCaseTextValue(featureValue);

        return switch (lowerCaseFeatureName) {
            // Discrete min-/max-features are just features in a range context in disguise.
            case "min-width" -> rangeQueryExpression(SizeQueryType.WIDTH, lowerCaseFeatureValue,
                                                     ComparisonOp.GREATER_OR_EQUAL.getExpressionSupplier());

            case "max-width" -> rangeQueryExpression(SizeQueryType.WIDTH, lowerCaseFeatureValue,
                                                     ComparisonOp.LESS_OR_EQUAL.getExpressionSupplier());

            case "min-height" -> rangeQueryExpression(SizeQueryType.HEIGHT, lowerCaseFeatureValue,
                                                      ComparisonOp.GREATER_OR_EQUAL.getExpressionSupplier());

            case "max-height" -> rangeQueryExpression(SizeQueryType.HEIGHT, lowerCaseFeatureValue,
                                                      ComparisonOp.LESS_OR_EQUAL.getExpressionSupplier());

            case "min-aspect-ratio" -> rangeQueryExpression(SizeQueryType.ASPECT_RATIO, lowerCaseFeatureValue,
                                                            ComparisonOp.GREATER_OR_EQUAL.getExpressionSupplier());

            case "max-aspect-ratio" -> rangeQueryExpression(SizeQueryType.ASPECT_RATIO, lowerCaseFeatureValue,
                                                            ComparisonOp.LESS_OR_EQUAL.getExpressionSupplier());

            // We have to account for range-based features used in a discrete context (e.g. "width: 500px").
            // This is unusual because in most cases these features will be evaluated in a range context, which
            // is handled in rangeQueryExpression().
            case "width",
                 "height",
                 "aspect-ratio" -> rangeQueryExpression(SizeQueryType.of(lowerCaseFeatureName),
                                                        lowerCaseFeatureValue,
                                                        ComparisonOp.EQUAL.getExpressionSupplier());

            // Portrait if height >= width, landscape otherwise.
            case "orientation" -> {
                boolean portrait = switch (checkNotNullValue(lowerCaseFeatureName, lowerCaseFeatureValue)) {
                    case "landscape" -> false;
                    case "portrait" -> true;
                    default -> throw unknownValue("orientation", featureValue);
                };

                yield FunctionExpression.of(
                    lowerCaseFeatureName, lowerCaseFeatureValue,
                    context -> portrait
                        ? context.getWidth() <= context.getHeight()
                        : context.getWidth() > context.getHeight(),
                    true, ContextAwareness.VIEWPORT_SIZE);
            }

            // We only support "standalone" and "fullscreen" display modes, not "minimal-ui" and "browser".
            case "display-mode" -> {
                boolean fullscreen = switch (checkNotNullValue(lowerCaseFeatureName, lowerCaseFeatureValue)) {
                    case "standalone" -> false;
                    case "fullscreen" -> true;
                    default -> throw unknownValue("display-mode", featureValue);
                };

                yield FunctionExpression.of(
                    lowerCaseFeatureName, lowerCaseFeatureValue,
                    context -> context.isFullScreen() == fullscreen,
                    true, ContextAwareness.FULLSCREEN);
            }

            case "prefers-color-scheme" -> FunctionExpression.of(
                lowerCaseFeatureName,
                checkNotNullValue(lowerCaseFeatureName, lowerCaseFeatureValue),
                MediaQueryContext::getColorScheme,
                enumValue(ColorScheme::valueOf, lowerCaseFeatureName, featureValue));

            case "prefers-reduced-motion" -> booleanPreferenceExpression(
                lowerCaseFeatureName, lowerCaseFeatureValue, "reduce", MediaQueryContext::isReducedMotion);

            case "prefers-reduced-transparency" -> booleanPreferenceExpression(
                lowerCaseFeatureName, lowerCaseFeatureValue, "reduce", MediaQueryContext::isReducedTransparency);

            case "prefers-reduced-data" -> booleanPreferenceExpression(
                lowerCaseFeatureName, lowerCaseFeatureValue, "reduce", MediaQueryContext::isReducedData);

            case "-fx-prefers-persistent-scrollbars" -> booleanPreferenceExpression(
                lowerCaseFeatureName, lowerCaseFeatureValue, "persistent", MediaQueryContext::isPersistentScrollBars);

            default -> throw new IllegalArgumentException(
                String.format("Unknown media feature <%s>", featureName));
        };
    }

    /**
     * Returns a {@code MediaQuery} that evaluates the specified feature in a range context.
     *
     * @param featureName the name of the media feature
     * @param featureValue the value of the media feature
     * @param comparison the comparison operator
     * @throws IllegalArgumentException if {@code featureName} or {@code featureValue} is invalid,
     *                                  or if {@code comparison} is {@code null}
     * @return the {@code MediaQuery}
     */
    public static MediaQuery rangeQueryExpression(String featureName,
                                                  String featureValue,
                                                  ComparisonOp comparison) {
        return rangeQueryExpression(SizeQueryType.of(lowerCaseTextValue(featureName)),
                                    lowerCaseTextValue(featureValue),
                                    comparison.getExpressionSupplier());
    }

    /**
     * Returns a {@code MediaQuery} that evaluates the specified feature as an interval in a range context.
     *
     * @param featureName the name of the media feature
     * @param featureValue1 the first value of the media feature
     * @param featureValue2 the second value of the media feature
     * @param comparison1 the first comparison operator
     * @param comparison2 the second comparison operator
     * @throws IllegalArgumentException if {@code featureName}, {@code featureValue1}, or {@code featureValue2} is invalid,
     *                                  or if {@code comparison1} or {@code comparison2} is {@code null}
     * @return the {@code MediaQuery}
     */
    public static MediaQuery rangeQueryExpression(String featureName,
                                                  String featureValue1,
                                                  String featureValue2,
                                                  ComparisonOp comparison1,
                                                  ComparisonOp comparison2) {
        return ConjunctionExpression.of(
            rangeQueryExpression(featureName, featureValue1, comparison1.flipped()),
            rangeQueryExpression(featureName, featureValue2, comparison2));
    }

    private static MediaQuery rangeQueryExpression(SizeQueryType featureType,
                                                   String lowerCaseFeatureValue,
                                                   RangeExpression.Supplier supplier) {
        Size size = lengthValue(
            featureType.getFeatureName(),
            checkNotNullValue(featureType.getFeatureName(), lowerCaseFeatureValue));

        return supplier.rangeExpression(featureType, size);
    }

    private static MediaQuery booleanPreferenceExpression(String featureName,
                                                          String featureValue,
                                                          String trueValue,
                                                          Function<MediaQueryContext, Boolean> argument) {
        if ("no-preference".equals(featureValue)) {
            return FunctionExpression.of(featureName, featureValue, argument, false);
        }

        if (featureValue == null || trueValue.equals(featureValue)) {
            return FunctionExpression.of(featureName, featureValue, argument, true);
        }

        throw unknownValue(featureName, featureValue);
    }

    private static <T> T checkNotNullValue(String featureName, T featureValue) {
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
            throw unknownValue(featureName, featureValue);
        }
    }

    private static String lowerCaseTextValue(String text) {
        return text != null ? text.toLowerCase(Locale.ROOT).intern() : null;
    }

    private static Size lengthValue(String featureName, String lowerCaseText) {
        int unitIndex = -1;

        for (int i = 0; i < lowerCaseText.length(); i++) {
            if (!Character.isDigit(lowerCaseText.charAt(i))) {
                unitIndex = i;
                break;
            }
        }

        if (unitIndex == -1) {
            return new Size(Double.parseDouble(lowerCaseText), SizeUnits.PX);
        }

        double value = Double.parseDouble(lowerCaseText.substring(0, unitIndex));

        return new Size(value, switch (lowerCaseText.substring(unitIndex)) {
            case "px" -> SizeUnits.PX;
            case "em" -> SizeUnits.EM;
            case "ex" -> SizeUnits.EX;
            case "cm" -> SizeUnits.CM;
            case "mm" -> SizeUnits.MM;
            case "in" -> SizeUnits.IN;
            case "pt" -> SizeUnits.PT;
            case "pc" -> SizeUnits.PC;
            default -> throw new IllegalArgumentException(
                String.format("Invalid value <%s> for media feature <%s>", lowerCaseText, featureName));
        });
    }

    private static RuntimeException unknownValue(String featureName, String featureValue) {
        return new IllegalArgumentException(
            String.format("Unknown value <%s> for media feature <%s>", featureValue, featureName));
    }
}
