/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.css.parser.CssLexer;
import com.sun.javafx.css.parser.CssNumberParser;
import com.sun.javafx.css.parser.CssParserHelper;
import com.sun.javafx.css.parser.Token;
import javafx.css.Size;

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
                                               Token featureValue,
                                               RangeExpression.Supplier rangeExpressionSupplier) {
        try {
            if (featureValue.getType() != CssLexer.NUMBER) {
                throw invalidValue(queryType.getFeatureName(), featureValue.getText());
            }

            return rangeExpressionSupplier.getNumberExpression(
                queryType, CssNumberParser.parseDouble(featureValue.getText()));
        } catch (NumberFormatException ignored) {
            throw invalidValue(queryType.getFeatureName(), featureValue.getText());
        }
    }

    private static MediaQuery sizeExpression(SizeQueryType queryType,
                                             Token featureValue,
                                             RangeExpression.Supplier rangeExpressionSupplier) {
        try {
            Size size = CssParserHelper.parseSize(featureValue);
            if (size == null) {
                throw invalidValue(queryType.getFeatureName(), featureValue.getText());
            }

            return rangeExpressionSupplier.getSizeExpression(queryType, size);
        } catch (NumberFormatException ignored) {
            throw invalidValue(queryType.getFeatureName(), featureValue.getText());
        }
    }

    private final String featureName;
    private final QueryEvaluator evaluator;
    private final QuerySupplier supplier;

    public String getFeatureName() {
        return featureName;
    }

    public MediaQuery getQueryExpression(Token featureValue, RangeExpression.Supplier rangeExpressionSupplier) {
        return supplier.get(this, featureValue, rangeExpressionSupplier);
    }

    public double evaluate(MediaQueryContext context) {
        return evaluator.get(context);
    }

    private static RuntimeException invalidValue(String featureName, String featureValue) {
        return new IllegalArgumentException(
            String.format("Invalid value <%s> for media feature <%s>", featureValue, featureName));
    }

    private interface QuerySupplier {
        MediaQuery get(SizeQueryType queryType, Token featureValue, RangeExpression.Supplier supplier);
    }

    private interface QueryEvaluator {
        double get(MediaQueryContext context);
    }

    private static final SizeQueryType[] VALUES = values();
}
