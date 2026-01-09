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

import com.sun.javafx.css.media.expression.EqualExpression;
import com.sun.javafx.css.media.expression.GreaterExpression;
import com.sun.javafx.css.media.expression.GreaterOrEqualExpression;
import com.sun.javafx.css.media.expression.LessExpression;
import com.sun.javafx.css.media.expression.LessOrEqualExpression;
import com.sun.javafx.css.media.expression.RangeExpression;
import javafx.css.Size;

enum ComparisonOp {

    LESS(new RangeExpression.Supplier() {
        @Override
        public RangeExpression getSizeExpression(SizeQueryType featureType, Size sizeValue) {
            return LessExpression.ofSize(featureType, sizeValue);
        }

        @Override
        public RangeExpression getNumberExpression(SizeQueryType featureType, double numberValue) {
            return LessExpression.ofNumber(featureType, numberValue);
        }
    }),

    LESS_OR_EQUAL(new RangeExpression.Supplier() {
        @Override
        public RangeExpression getSizeExpression(SizeQueryType featureType, Size sizeValue) {
            return LessOrEqualExpression.ofSize(featureType, sizeValue);
        }

        @Override
        public RangeExpression getNumberExpression(SizeQueryType featureType, double numberValue) {
            return LessOrEqualExpression.ofNumber(featureType, numberValue);
        }
    }),

    GREATER(new RangeExpression.Supplier() {
        @Override
        public RangeExpression getSizeExpression(SizeQueryType featureType, Size sizeValue) {
            return GreaterExpression.ofSize(featureType, sizeValue);
        }

        @Override
        public RangeExpression getNumberExpression(SizeQueryType featureType, double numberValue) {
            return GreaterExpression.ofNumber(featureType, numberValue);
        }
    }),

    GREATER_OR_EQUAL(new RangeExpression.Supplier() {
        @Override
        public RangeExpression getSizeExpression(SizeQueryType featureType, Size sizeValue) {
            return GreaterOrEqualExpression.ofSize(featureType, sizeValue);
        }

        @Override
        public RangeExpression getNumberExpression(SizeQueryType featureType, double numberValue) {
            return GreaterOrEqualExpression.ofNumber(featureType, numberValue);
        }
    }),

    EQUAL(new RangeExpression.Supplier() {
        @Override
        public RangeExpression getSizeExpression(SizeQueryType featureType, Size sizeValue) {
            return EqualExpression.ofSize(featureType, sizeValue);
        }

        @Override
        public RangeExpression getNumberExpression(SizeQueryType featureType, double numberValue) {
            return EqualExpression.ofNumber(featureType, numberValue);
        }
    });

    ComparisonOp(RangeExpression.Supplier supplier) {
        this.supplier = supplier;
    }

    private final RangeExpression.Supplier supplier;

    public RangeExpression.Supplier getExpressionSupplier() {
        return supplier;
    }

    public ComparisonOp flipped() {
        return switch (this) {
            case LESS -> GREATER;
            case LESS_OR_EQUAL -> GREATER_OR_EQUAL;
            case GREATER -> LESS;
            case GREATER_OR_EQUAL -> LESS_OR_EQUAL;
            case EQUAL -> EQUAL;
        };
    }

    public boolean isSameDirection(ComparisonOp other) {
        return switch (this) {
            case LESS, LESS_OR_EQUAL -> other == LESS || other == LESS_OR_EQUAL;
            case GREATER, GREATER_OR_EQUAL -> other == GREATER || other == GREATER_OR_EQUAL;
            default -> false;
        };
    }
}
