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
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.tree.BinaryExpr;
import com.sun.scenario.effect.compiler.tree.DoWhileStmt;
import com.sun.scenario.effect.compiler.tree.ExprStmt;
import com.sun.scenario.effect.compiler.tree.ForStmt;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import com.sun.scenario.effect.compiler.tree.Stmt;
import com.sun.scenario.effect.compiler.tree.WhileStmt;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IterationStatementTest extends ParserBase {

    @Test
    public void whileStmt() {
        Stmt tree = parseTreeFor("while (i >= 3) j += 4;");
        assertTrue(tree instanceof WhileStmt);
        WhileStmt s = (WhileStmt)tree;
        assertTrue(s.getCondition() instanceof BinaryExpr);
        assertTrue(s.getStmt() instanceof ExprStmt);
    }

    @Test
    public void doWhileStmt() {
        Stmt tree = parseTreeFor("do j += 4; while (i >= 3);");
        assertTrue(tree instanceof DoWhileStmt);
        DoWhileStmt s = (DoWhileStmt)tree;
        assertTrue(s.getStmt() instanceof ExprStmt);
        assertTrue(s.getExpr() instanceof BinaryExpr);
    }

    @Test
    public void forStmt() {
        Stmt tree = parseTreeFor("for (i = 0; i < 5; i += 2) j += 4;");
        assertTrue(tree instanceof ForStmt);
        ForStmt s = (ForStmt)tree;
        assertTrue(s.getInit() instanceof ExprStmt);
        assertTrue(s.getCondition() instanceof BinaryExpr);
        assertTrue(s.getExpr() instanceof BinaryExpr);
        assertTrue(s.getStmt() instanceof ExprStmt);
    }

    @Test
    public void forStmtNoCondition() {
        Stmt tree = parseTreeFor("for (i = 0; ; i += 2) j += 4;");
        assertTrue(tree instanceof ForStmt);
        ForStmt s = (ForStmt)tree;
        assertTrue(s.getInit() instanceof ExprStmt);
        assertNull(s.getCondition());
        assertTrue(s.getExpr() instanceof BinaryExpr);
        assertTrue(s.getStmt() instanceof ExprStmt);
    }

    @Test
    public void forStmtNoIncrement() {
        Stmt tree = parseTreeFor("for (i = 0; i < 5; ) j += 4;");
        assertTrue(tree instanceof ForStmt);
        ForStmt s = (ForStmt)tree;
        assertTrue(s.getInit() instanceof ExprStmt);
        assertTrue(s.getCondition() instanceof BinaryExpr);
        assertNull(s.getExpr());
        assertTrue(s.getStmt() instanceof ExprStmt);
    }

    @Test
    public void forStmtNoConditionOrIncrement() {
        Stmt tree = parseTreeFor("for (i = 0; ; ) j += 4;");
        assertTrue(tree instanceof ForStmt);
        ForStmt s = (ForStmt)tree;
        assertTrue(s.getInit() instanceof ExprStmt);
        assertNull(s.getCondition());
        assertNull(s.getExpr());
        assertTrue(s.getStmt() instanceof ExprStmt);
    }

    @Test
    public void notAnIterationStmt() {
        assertThrows(ParseCancellationException.class, () -> {
            parseTreeFor("return;");
        });
    }

    private Stmt parseTreeFor(String text) {
        JSLParser parser = parserOver(text);
        JSLVisitor visitor = new JSLVisitor();
        visitor.getSymbolTable().declareVariable("i", Type.INT, null);
        visitor.getSymbolTable().declareVariable("j", Type.INT, null);
        return visitor.visitIteration_statement(parser.iteration_statement());
    }
}
