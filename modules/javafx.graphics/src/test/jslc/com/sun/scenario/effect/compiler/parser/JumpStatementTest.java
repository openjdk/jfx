/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.compiler.tree.BreakStmt;
import com.sun.scenario.effect.compiler.tree.ContinueStmt;
import com.sun.scenario.effect.compiler.tree.DiscardStmt;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import com.sun.scenario.effect.compiler.tree.LiteralExpr;
import com.sun.scenario.effect.compiler.tree.ReturnStmt;
import com.sun.scenario.effect.compiler.tree.Stmt;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JumpStatementTest extends ParserBase {

    @Test
    public void cont() throws Exception {
        Stmt tree = parseTreeFor("continue;");
        assertTrue(tree instanceof ContinueStmt);
    }

    @Test
    public void brk() throws Exception {
        Stmt tree = parseTreeFor(" break ; ");
        assertTrue(tree instanceof BreakStmt);
    }

    @Test
    public void discard() throws Exception {
        Stmt tree = parseTreeFor("discard;");
        assertTrue(tree instanceof DiscardStmt);
    }

    @Test
    public void returnEmpty() throws Exception {
        Stmt tree = parseTreeFor("return;");
        assertTrue(tree instanceof ReturnStmt);
        assertNull(((ReturnStmt)tree).getExpr());
    }

    @Test
    public void returnExpr() throws Exception {
        Stmt tree = parseTreeFor("return 3;");
        assertTrue(tree instanceof ReturnStmt);
        ReturnStmt ret = (ReturnStmt)tree;
        assertTrue(ret.getExpr() instanceof LiteralExpr);
        LiteralExpr lit = (LiteralExpr)ret.getExpr();
        assertEquals(lit.getValue(), Integer.valueOf(3));
    }

    @Test(expected = ParseCancellationException.class)
    public void notAJump() throws Exception {
        parseTreeFor("float;");
    }

    private Stmt parseTreeFor(String text) throws Exception {
        JSLParser parser = parserOver(text);
        JSLVisitor visitor = new JSLVisitor();
        return visitor.visitJump_statement(parser.jump_statement());
    }
}
