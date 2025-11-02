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

package com.sun.javafx.css.media.expression;

import com.sun.javafx.css.media.ContextAwareness;
import com.sun.javafx.css.media.MediaQuery;
import com.sun.javafx.css.media.SizeQueryType;
import javafx.css.Size;
import java.util.Objects;

/**
 * Base class for expressions that are evaluated in a range context.
 */
public sealed abstract class RangeExpression implements MediaQuery
        permits EqualExpression,
                GreaterExpression,
                GreaterOrEqualExpression,
                LessExpression,
                LessOrEqualExpression {

    private final SizeQueryType featureType;
    private final Size featureValue;

    RangeExpression(SizeQueryType featureType, Size featureValue) {
        this.featureType = Objects.requireNonNull(featureType, "featureType cannot be null");
        this.featureValue = Objects.requireNonNull(featureValue, "featureValue cannot be null");
    }

    public final SizeQueryType getFeatureType() {
        return featureType;
    }

    public final String getFeatureName() {
        return featureType.getFeatureName();
    }

    public final Size getFeatureValue() {
        return featureValue;
    }

    @Override
    public final int getContextAwareness() {
        return ContextAwareness.VIEWPORT_SIZE.value();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        RangeExpression other = (RangeExpression)obj;
        return other.featureType == featureType
            && other.featureValue.equals(featureValue);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), featureType, featureValue);
    }

    public interface Supplier {
        RangeExpression rangeExpression(SizeQueryType featureType, Size featureValue);
    }
}
