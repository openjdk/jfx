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

import com.sun.javafx.css.media.expression.RangeExpression;
import javafx.css.Size;
import javafx.css.SizeUnits;

public enum SizeQueryType {

    WIDTH("width", SizeQueryType::sizeExpression, MediaQueryContext::getWidth),
    HEIGHT("height", SizeQueryType::sizeExpression, MediaQueryContext::getHeight),
    ASPECT_RATIO("aspect-ratio", SizeQueryType::numberExpression, context -> context.getWidth() / context.getHeight());

    SizeQueryType(String featureName, QuerySupplier supplier, QueryEvaluator evaluator) {
        this.featureName = featureName;
        this.evaluator = evaluator;
        this.supplier = supplier;
    }

    public static SizeQueryType of(String name) {
        for (SizeQueryType value : VALUES) {
            if (value.featureName.equals(name)) {
                return value;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown media feature <%s>", name));
    }

    private static MediaQuery numberExpression(SizeQueryType queryType,
                                               String featureValue,
                                               RangeExpression.Supplier rangeExpressionSupplier) {
        try {
            return rangeExpressionSupplier.getNumberExpression(queryType, Double.parseDouble(featureValue));
        } catch (NumberFormatException ignored) {
            throw invalidValue(queryType.getFeatureName(), featureValue);
        }
    }

    private static MediaQuery sizeExpression(SizeQueryType queryType,
                                             String featureValue,
                                             RangeExpression.Supplier rangeExpressionSupplier) {
        try {
            return rangeExpressionSupplier.getSizeExpression(
                queryType, sizeValue(queryType.getFeatureName(), featureValue));
        } catch (NumberFormatException ignored) {
            throw invalidValue(queryType.getFeatureName(), featureValue);
        }
    }

    private final String featureName;
    private final QueryEvaluator evaluator;
    private final QuerySupplier supplier;

    public String getFeatureName() {
        return featureName;
    }

    public MediaQuery getQueryExpression(String featureValue, RangeExpression.Supplier rangeExpressionSupplier) {
        return supplier.get(this, featureValue, rangeExpressionSupplier);
    }

    public double evaluate(MediaQueryContext context) {
        return evaluator.get(context);
    }

    /*
     * As per CSS specification (https://www.w3.org/TR/css-values-4/#lengths), a <length> value (called <size>
     * in JavaFX) always requires a unit. The only exception is the zero value, which can be specified without
     * a unit. However, JavaFX's CssParser accepts sizes without a unit by implicitly treating them as pixels.
     */
    private static Size sizeValue(String featureName, String lowerCaseText) {
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
            default -> throw invalidValue(featureName, lowerCaseText);
        });
    }

    private static RuntimeException invalidValue(String featureName, String featureValue) {
        return new IllegalArgumentException(
            String.format("Invalid value <%s> for media feature <%s>", featureValue, featureName));
    }

    private interface QuerySupplier {
        MediaQuery get(SizeQueryType queryType, String featureValue, RangeExpression.Supplier supplier);
    }

    private interface QueryEvaluator {
        double get(MediaQueryContext context);
    }

    private static final SizeQueryType[] VALUES = values();
}
