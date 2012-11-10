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

import java.util.List;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.Function;
import com.sun.scenario.effect.compiler.model.Param;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.ExtDecl;
import com.sun.scenario.effect.compiler.tree.FuncDef;
import com.sun.scenario.effect.compiler.tree.VarDecl;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalDeclarationTest extends ParserBase {

    @Test
    public void declaration() throws Exception {
        ExtDecl tree = parseTreeFor("param float4 foo;").get(0);
        assertTrue(tree instanceof VarDecl);
        VarDecl d = (VarDecl)tree;
        Variable var = d.getVariable();
        assertNotNull(var);
        assertEquals(var.getQualifier(), Qualifier.PARAM);
        assertEquals(var.getType(), Type.FLOAT4);
        assertEquals(var.getName(), "foo");
        assertNull(d.getInit());
    }

    @Test
    public void multiDeclaration() throws Exception {
        List<ExtDecl> decls = parseTreeFor("param float4 foo, bar;");
        assertEquals(decls.size(), 2);
        ExtDecl tree;
        tree = decls.get(0);
        assertTrue(tree instanceof VarDecl);
        VarDecl d = (VarDecl)tree;
        Variable var = d.getVariable();
        assertNotNull(var);
        assertEquals(var.getQualifier(), Qualifier.PARAM);
        assertEquals(var.getType(), Type.FLOAT4);
        assertEquals(var.getName(), "foo");
        assertNull(d.getInit());
        tree = decls.get(1);
        assertTrue(tree instanceof VarDecl);
        d = (VarDecl)tree;
        var = d.getVariable();
        assertNotNull(var);
        assertEquals(var.getQualifier(), Qualifier.PARAM);
        assertEquals(var.getType(), Type.FLOAT4);
        assertEquals(var.getName(), "bar");
        assertNull(d.getInit());
    }
    
    @Test
    public void funcDefNoParam() throws Exception {
        ExtDecl tree = parseTreeFor("void test() { int i = 3; }").get(0);
        assertTrue(tree instanceof FuncDef);
        FuncDef d = (FuncDef)tree;
        Function func = d.getFunction();
        assertNotNull(func);
        assertEquals(func.getReturnType(), Type.VOID);
        assertEquals(func.getName(), "test");
        List<Param> params = func.getParams();
        assertNotNull(params);
        assertEquals(params.size(), 0);
        assertNotNull(d.getStmt());
    }

    @Test
    public void funcDefOneParam() throws Exception {
        ExtDecl tree = parseTreeFor("void test(float3 foo) { int i = 3; }").get(0);
        assertTrue(tree instanceof FuncDef);
        FuncDef d = (FuncDef)tree;
        Function func = d.getFunction();
        assertNotNull(func);
        assertEquals(func.getReturnType(), Type.VOID);
        assertEquals(func.getName(), "test");
        List<Param> params = func.getParams();
        assertNotNull(params);
        assertEquals(params.size(), 1);
        assertNotNull(d.getStmt());
    }
    
    @Test
    public void funcDefTwoParam() throws Exception {
        ExtDecl tree = parseTreeFor("void test(float3 foo, float3 bar) { int i = 3; }").get(0);
        assertTrue(tree instanceof FuncDef);
        FuncDef d = (FuncDef)tree;
        Function func = d.getFunction();
        assertNotNull(func);
        assertEquals(func.getReturnType(), Type.VOID);
        assertEquals(func.getName(), "test");
        List<Param> params = func.getParams();
        assertNotNull(params);
        assertEquals(params.size(), 2);
        assertNotNull(d.getStmt());
    }

    @Test(expected = RecognitionException.class)
    public void notAnExtDecl() throws Exception {
        parseTreeFor("foo = 4");
    }

    private List<ExtDecl> parseTreeFor(String text) throws RecognitionException {
        JSLParser parser = parserOver(text);
        return parser.external_declaration();
    }
}
