/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.SymbolTable;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.tree.BinaryExpr;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

public class AssignmentExprTest extends ParserBase {

    @Test
    public void userVar() throws Exception {
        BinaryExpr tree = parseTreeFor("foo = 32.0");
    }

    @Test(expected = RuntimeException.class)
    public void userROVar() throws Exception {
        BinaryExpr tree = parseTreeFor("readonly = 32.0");
    }

    @Test
    public void coreVar() throws Exception {
        BinaryExpr tree = parseTreeFor("color = float4(1.0)");
    }

    @Test
    public void coreVarField() throws Exception {
        BinaryExpr tree = parseTreeFor("color.r = 3.0");
    }

    @Test(expected = RuntimeException.class)
    public void coreROVar() throws Exception {
        BinaryExpr tree = parseTreeFor("pos0 = float2(1.0)");
    }
    
    @Test(expected = RuntimeException.class)
    public void coreROVarField() throws Exception {
        BinaryExpr tree = parseTreeFor("pos0.x = 1.0");
    }

    @Test(expected = RecognitionException.class)
    public void notAnAssignment() throws Exception {
        parseTreeFor("const foo");
    }

    private BinaryExpr parseTreeFor(String text) throws RecognitionException {
        JSLParser parser = parserOver(text);
        SymbolTable st = parser.getSymbolTable();
        st.declareVariable("foo", Type.FLOAT, null);
        st.declareVariable("readonly", Type.FLOAT, Qualifier.CONST);
        // trick test into thinking main() function is currently in
        // scope so that we can test core variables such as color and pos0
        st.enterFrame();
        st.declareFunction("main", Type.VOID, null);
        return (BinaryExpr)parser.assignment_expression();
    }
}
