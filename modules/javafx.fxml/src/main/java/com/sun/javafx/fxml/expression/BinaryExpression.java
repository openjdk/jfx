/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.expression;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Abstract base class for binary expressions.
 */
public final class BinaryExpression<U, T> extends Expression<T> {
    private final BiFunction<U, U, T> evaluator;
    private final Expression<U> left;
    private final Expression<U> right;

    public BinaryExpression(Expression<U> left, Expression<U> right, BiFunction<U, U, T> evaluator) {
        if (left == null) {
            throw new NullPointerException();
        }

        if (right == null) {
            throw new NullPointerException();
        }

        this.left = left;
        this.right = right;
        this.evaluator = evaluator;
    }

    @Override
    public T evaluate(Object namespace) {
        return evaluator.apply(left.evaluate(namespace), right.evaluate(namespace));
    }

    @Override
    public void update(Object namespace, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefined(Object namespace) {
        return left.isDefined(namespace) && right.isDefined(namespace);
    }

    @Override
    public boolean isLValue() {
        return false;
    }

    @Override
    protected void getArguments(List<KeyPath> arguments) {
        left.getArguments(arguments);
        right.getArguments(arguments);
    }

}
