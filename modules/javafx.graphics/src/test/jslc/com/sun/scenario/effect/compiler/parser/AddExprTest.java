/*
 * Copyright (c) 2008, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler.parser;

import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BinaryOpType;
import com.sun.scenario.effect.compiler.tree.BinaryExpr;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AddExprTest extends MultExprTest {

    private String mult;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        this.mult = multiplicative();
    }

    @Test
    public void oneAddition() {
        BinaryExpr tree = parseTreeFor(mult + " + " + mult);
        assertEquals(BinaryOpType.ADD, tree.getOp());
    }

    @Test
    public void oneSubtraction() {
        BinaryExpr tree = parseTreeFor(mult + "   - " + mult);
        assertEquals(BinaryOpType.SUB, tree.getOp());
    }

    @Test
    public void additiveCombination() {
        BinaryExpr tree = parseTreeFor(mult + " + " + mult + '-' + mult + '-' + mult + "   +" + mult);
        assertEquals(BinaryOpType.ADD, tree.getOp());
    }

    @Test
    public void notAnAdditiveExpression() {
        assertThrows(ClassCastException.class, () -> {
            parseTreeFor(mult + "!" + mult);
        });
    }

    private BinaryExpr parseTreeFor(String text) {
        JSLParser parser = parserOver(text);
        JSLVisitor visitor = new JSLVisitor();
        return (BinaryExpr) visitor.visit(parser.additive_expression());
    }

    protected String additive() {
        return "(" + multiplicative() + " + " + multiplicative() + ")";
    }
}
