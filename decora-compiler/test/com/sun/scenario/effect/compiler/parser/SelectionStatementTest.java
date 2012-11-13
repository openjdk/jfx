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

package com.sun.scenario.effect.compiler.parser;

import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.tree.BinaryExpr;
import com.sun.scenario.effect.compiler.tree.ExprStmt;
import com.sun.scenario.effect.compiler.tree.LiteralExpr;
import com.sun.scenario.effect.compiler.tree.SelectStmt;
import com.sun.scenario.effect.compiler.tree.Stmt;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import static org.junit.Assert.*;

public class SelectionStatementTest extends ParserBase {

    @Test
    public void ifOnly() throws Exception {
        Stmt tree = parseTreeFor("if (foo >= 3) foo += 12;");
        assertTrue(tree instanceof SelectStmt);
        SelectStmt s = (SelectStmt)tree;
        assertTrue(s.getIfExpr() instanceof BinaryExpr);
        assertTrue(s.getThenStmt() instanceof ExprStmt);
        assertNull(s.getElseStmt());
    }

    @Test
    public void ifAndElse() throws Exception {
        Stmt tree = parseTreeFor("if (true) foo+=5; else --foo;");
        assertTrue(tree instanceof SelectStmt);
        SelectStmt s = (SelectStmt)tree;
        assertTrue(s.getIfExpr() instanceof LiteralExpr);
        assertTrue(s.getThenStmt() instanceof ExprStmt);
        assertTrue(s.getElseStmt() instanceof ExprStmt);
    }

    @Test(expected = RecognitionException.class)
    public void notASelect() throws Exception {
        parseTreeFor("then (so) { bobs yer uncle }");
    }

    private Stmt parseTreeFor(String text) throws RecognitionException {
        JSLParser parser = parserOver(text);
        parser.getSymbolTable().declareVariable("foo", Type.INT, null);
        return parser.selection_statement();
    }
}
