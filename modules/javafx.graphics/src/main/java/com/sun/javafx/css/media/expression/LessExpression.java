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

package com.sun.javafx.css.media.expression;

import com.sun.javafx.css.media.MediaQueryCache;
import com.sun.javafx.css.media.SizeQueryType;
import com.sun.javafx.css.media.MediaQueryContext;
import javafx.css.Size;

/**
 * Evaluates whether a media feature is less than a specified value.
 */
public final class LessExpression extends RangeExpression {

    private LessExpression(SizeQueryType featureType, Size sizeValue) {
        super(featureType, sizeValue);
    }

    private LessExpression(SizeQueryType featureType, double numberValue) {
        super(featureType, numberValue);
    }

    public static LessExpression ofSize(SizeQueryType featureType, Size sizeValue) {
        return MediaQueryCache.getCachedMediaQuery(new LessExpression(featureType, sizeValue));
    }

    public static LessExpression ofNumber(SizeQueryType featureType, double numberValue) {
        return MediaQueryCache.getCachedMediaQuery(new LessExpression(featureType, numberValue));
    }

    @Override
    public boolean evaluate(MediaQueryContext context) {
        return getFeatureType().evaluate(context) < getValue();
    }

    @Override
    public String toString() {
        return "(" + getFeatureName() + " < " + getFormattedValue() + ")";
    }
}
