/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler.tree;

import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.BinaryOpType;
import com.sun.scenario.effect.compiler.model.Type;

/**
 */
public class BinaryExpr extends Expr {

    private final BinaryOpType op;
    private final Expr left,  right;

    BinaryExpr(BinaryOpType op, Expr left, Expr right) {
        super(getType(op, left, right));
        this.op = op;
        this.left = left;
        this.right = right;
    }

    private static Type getType(BinaryOpType op, Expr left, Expr right) {
        if (op.isRelational()) {
            // TODO: what about bool vector ops?
            return Type.BOOL;
        } else {
            Type ltype = left.getResultType();
            Type rtype = right.getResultType();
            BaseType lbase = ltype.getBaseType();
            BaseType rbase = rtype.getBaseType();
            if (ltype == rtype) {
                return ltype;
            } else if (lbase == rbase &&
                       ((ltype.isVector() && !rtype.isVector()) ||
                        (!ltype.isVector() && rtype.isVector())))
            {
                if (ltype.isVector()) {
                    return ltype;
                } else {
                    return rtype;
                }
            } else if (lbase != rbase &&
                       !ltype.isVector() &&
                       !rtype.isVector() &&
                       ((lbase == BaseType.FLOAT && rbase == BaseType.INT) ||
                        (lbase == BaseType.INT && rbase == BaseType.FLOAT)))
            {
                // allow for some basic operations involving float and int
                // scalar values, where we assume the backend will
                // automatically promote the result to floating point
                return Type.FLOAT;
            } else {
                throw new RuntimeException("Expressions must have compatible result types" +
                                           " (lhs=" + ltype +
                                           " rhs=" + rtype +
                                           " op=" + op + ")");
            }
        }
    }
    
    public BinaryOpType getOp() {
        return op;
    }

    public Expr getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
    }

    public void accept(TreeVisitor tv) {
        tv.visitBinaryExpr(this);
    }
}
