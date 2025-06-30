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
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
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
        NEGATION(5);

        static QueryType of(MediaQuery expression) {
            return switch (expression) {
                case ConstantExpression _ -> CONSTANT;
                case FunctionExpression<?> _ -> FUNCTION;
                case ConjunctionExpression _ -> CONJUNCTION;
                case DisjunctionExpression _ -> DISJUNCTION;
                case NegationExpression _ -> NEGATION;
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
                os.writeInt(stringStore.addString(expr.featureName()));

                if (expr.featureValue() != null) {
                    os.writeInt(stringStore.addString(expr.featureValue()));
                } else {
                    os.writeInt(-1);
                }
            }

            case NegationExpression expr ->
                writeBinary(expr.expression(), os, stringStore);

            case ConjunctionExpression expr -> {
                writeBinary(expr.left(), os, stringStore);
                writeBinary(expr.right(), os, stringStore);
            }

            case DisjunctionExpression expr -> {
                writeBinary(expr.left(), os, stringStore);
                writeBinary(expr.right(), os, stringStore);
            }
        }
    }

    public static MediaQuery readBinary(DataInputStream is, String[] strings) throws IOException {
        return switch (QueryType.of(is.readByte())) {
            case FUNCTION -> {
                String featureName = strings[is.readInt()];
                int featureValueIdx = is.readInt();
                String featureValue = featureValueIdx >= 0 ? strings[featureValueIdx] : null;
                yield MediaFeatures.featureQueryExpression(featureName, featureValue);
            }
            case CONSTANT -> new ConstantExpression(is.readBoolean());
            case NEGATION -> new NegationExpression(readBinary(is, strings));
            case CONJUNCTION -> new ConjunctionExpression(readBinary(is, strings), readBinary(is, strings));
            case DISJUNCTION -> new DisjunctionExpression(readBinary(is, strings), readBinary(is, strings));
        };
    }
}
