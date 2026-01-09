/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.media.MediaQuery;
import com.sun.javafx.css.media.MediaQueryCache;
import com.sun.javafx.css.media.MediaQueryContext;
import java.util.List;
import java.util.Objects;

/**
 * Logical disjunction of the specified expressions.
 */
public final class DisjunctionExpression implements MediaQuery {

    private final MediaQuery left;
    private final MediaQuery right;
    private final int contextAwareness;

    private DisjunctionExpression(MediaQuery left, MediaQuery right) {
        this.left = Objects.requireNonNull(left, "left cannot be null");
        this.right = Objects.requireNonNull(right, "right cannot be null");
        this.contextAwareness = left.getContextAwareness() | right.getContextAwareness();
    }

    /**
     * Returns the disjunction of the specified expressions.
     */
    public static DisjunctionExpression of(MediaQuery left, MediaQuery right) {
        return MediaQueryCache.getCachedMediaQuery(new DisjunctionExpression(left, right));
    }

    /**
     * Returns the disjunction of all specified expressions.
     */
    public static DisjunctionExpression of(List<MediaQuery> expressions) {
        if (expressions.size() < 2) {
            throw new IllegalArgumentException();
        }

        var result = of(expressions.get(0), expressions.get(1));

        for (int i = 2; i < expressions.size(); i++) {
            result = of(result, expressions.get(i));
        }

        return result;
    }

    public MediaQuery getLeft() {
        return left;
    }

    public MediaQuery getRight() {
        return right;
    }

    @Override
    public int getContextAwareness() {
        return contextAwareness;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DisjunctionExpression other
            && left.equals(other.left)
            && right.equals(other.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(DisjunctionExpression.class, left, right);
    }

    @Override
    public boolean evaluate(MediaQueryContext context) {
        return left.evaluate(context) || right.evaluate(context);
    }

    @Override
    public String toString() {
        return "(" + left + " or " + right + ")";
    }
}
