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

package com.sun.javafx.css.media;

import com.sun.javafx.css.media.expression.ConjunctionExpression;
import com.sun.javafx.css.media.expression.ConstantExpression;
import com.sun.javafx.css.media.expression.FunctionExpression;
import com.sun.javafx.css.media.expression.NegationExpression;
import com.sun.javafx.css.media.expression.DisjunctionExpression;
import com.sun.javafx.css.media.expression.RangeExpression;

/**
 * {@code MediaQuery} is the runtime representation of a CSS media query expression.
 * <p>
 * It is evaluated against a context that provides the values that are referenced in the expression,
 * and evaluates to either {@code true} or {@code false}.
 */
public sealed interface MediaQuery
        permits ConstantExpression,
                ConjunctionExpression,
                DisjunctionExpression,
                FunctionExpression,
                NegationExpression,
                RangeExpression {

    /**
     * Gets the context awareness flags of this media query, indicating which aspects of the
     * media query context are probed by the query.
     *
     * @return the context awareness flags
     */
    int getContextAwareness();

    /**
     * Attempts to determine the result of this media query without a context, and returns {@link TriState#TRUE}
     * if the query always matches, {@link TriState#FALSE} if it never matches, or {@link TriState#UNKNOWN} if
     * the result depends on the context or cannot be determined.
     *
     * @return {@link TriState#TRUE} if the query is always true, {@link TriState#FALSE}
     *         if the query is always false, otherwise {@link TriState#UNKNOWN}
     */
    TriState evaluate();

    /**
     * Evaluates this media query against the provided context.
     *
     * @param context the evaluation context
     * @return {@code true} if the media query matches, {@code false} otherwise
     */
    boolean evaluate(MediaQueryContext context);
}
