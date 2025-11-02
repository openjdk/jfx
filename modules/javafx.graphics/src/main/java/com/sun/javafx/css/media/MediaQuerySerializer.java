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
import com.sun.javafx.css.media.expression.ConstantExpression;
import com.sun.javafx.css.media.expression.EqualExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.GreaterExpression;
import com.sun.javafx.css.media.expression.GreaterOrEqualExpression;
import com.sun.javafx.css.media.expression.LessExpression;
import com.sun.javafx.css.media.expression.LessOrEqualExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.expression.RangeExpression;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Serializes and deserializes a {@link MediaQuery} expression into and from a binary representation.
 */
public final class MediaQuerySerializer {

    private MediaQuerySerializer() {}

    private enum QueryType {
        CONSTANT(1),
        FUNCTION(2),
        CONJUNCTION(3),
        DISJUNCTION(4),
        NEGATION(5),
        EQUAL(6),
        GREATER(7),
        GREATER_OR_EQUAL(8),
        LESS(9),
        LESS_OR_EQUAL(10);

        static QueryType of(MediaQuery expression) {
            return switch (expression) {
                case ConstantExpression _ -> CONSTANT;
                case FunctionExpression<?> _ -> FUNCTION;
                case ConjunctionExpression _ -> CONJUNCTION;
                case DisjunctionExpression _ -> DISJUNCTION;
                case NegationExpression _ -> NEGATION;
                case EqualExpression _ -> EQUAL;
                case GreaterExpression _ -> GREATER;
                case GreaterOrEqualExpression _ -> GREATER_OR_EQUAL;
                case LessExpression _ -> LESS;
                case LessOrEqualExpression _ -> LESS_OR_EQUAL;
            };
        }

        static QueryType of(int serializedId) {
            for (QueryType value : VALUES) {
                if (value.serializedId == serializedId) {
                    return value;
                }
            }

            throw new IllegalArgumentException("serializedId");
        }

        QueryType(int serializedId) {
            this.serializedId = serializedId;
        }

        private final int serializedId;

        private static final QueryType[] VALUES = values();
    }

    public static void writeBinary(MediaQuery mediaQuery,
                                   DataOutputStream os,
                                   StyleConverter.StringStore stringStore) throws IOException {
        os.writeByte(QueryType.of(mediaQuery).serializedId);

        switch (mediaQuery) {
            case ConstantExpression expr ->
                os.writeBoolean(expr.value());

            case FunctionExpression<?> expr -> {
                os.writeInt(stringStore.addString(expr.getFeatureName()));

                if (expr.getFeatureValue() != null) {
                    os.writeInt(stringStore.addString(expr.getFeatureValue()));
                } else {
                    os.writeInt(-1);
                }
            }

            case NegationExpression expr ->
                writeBinary(expr.getExpression(), os, stringStore);

            case ConjunctionExpression expr -> {
                writeBinary(expr.getLeft(), os, stringStore);
                writeBinary(expr.getRight(), os, stringStore);
            }

            case DisjunctionExpression expr -> {
                writeBinary(expr.getLeft(), os, stringStore);
                writeBinary(expr.getRight(), os, stringStore);
            }

            case RangeExpression expr -> {
                os.writeInt(stringStore.addString(expr.getFeatureName()));
                os.writeDouble(expr.getFeatureValue().getValue());
                os.writeByte(expr.getFeatureValue().getUnits().ordinal());
            }
        }
    }

    public static MediaQuery readBinary(DataInputStream is, String[] strings) throws IOException {
        return switch (QueryType.of(is.readByte())) {
            case FUNCTION -> {
                String featureName = strings[is.readInt()];
                int featureValueIdx = is.readInt();
                String featureValue = featureValueIdx >= 0 ? strings[featureValueIdx] : null;
                yield MediaFeatures.discreteQueryExpression(featureName, featureValue);
            }
            case CONSTANT -> ConstantExpression.of(is.readBoolean());
            case NEGATION -> NegationExpression.of(readBinary(is, strings));
            case CONJUNCTION -> ConjunctionExpression.of(readBinary(is, strings), readBinary(is, strings));
            case DISJUNCTION -> DisjunctionExpression.of(readBinary(is, strings), readBinary(is, strings));
            case EQUAL -> EqualExpression.of(SizeQueryType.of(strings[is.readInt()]), readSize(is));
            case GREATER -> GreaterExpression.of(SizeQueryType.of(strings[is.readInt()]), readSize(is));
            case GREATER_OR_EQUAL -> GreaterOrEqualExpression.of(SizeQueryType.of(strings[is.readInt()]), readSize(is));
            case LESS -> LessExpression.of(SizeQueryType.of(strings[is.readInt()]), readSize(is));
            case LESS_OR_EQUAL -> LessOrEqualExpression.of(SizeQueryType.of(strings[is.readInt()]), readSize(is));
        };
    }

    private static final SizeUnits[] SIZE_UNITS = SizeUnits.values();

    private static Size readSize(DataInputStream is) throws IOException {
        return new Size(is.readDouble(), SIZE_UNITS[is.readByte()]);
    }
}
