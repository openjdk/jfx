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
import com.sun.scenario.effect.compiler.JSLParser.Fully_specified_typeContext;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FullySpecifiedTypeTest extends ParserBase {

    @Test
    public void unqualified() throws Exception {
        JSLVisitor.FullySpecifiedTypeExpr ret = parseTreeFor("float");
        assertNull(ret.getQual());
        assertEquals(Type.FLOAT, ret.getType());
    }

    @Test
    public void qualified() throws Exception {
        JSLVisitor.FullySpecifiedTypeExpr ret = parseTreeFor("param bool3");
        assertEquals(Qualifier.PARAM, ret.getQual());
        assertEquals(Type.BOOL3, ret.getType());
    }

    @Test(expected = ParseCancellationException.class)
    public void notAFullySpecifiedType() throws Exception {
        parseTreeFor("double");
    }

    private fully_specified_type_return parseTreeFor(String text) throws Exception {
        JSLParser parser = parserOver(text);
        JSLVisitor visitor = new JSLVisitor();
        return (JSLVisitor.FullySpecifiedTypeExpr) visitor.visit(parser.fully_specified_type());
    }
}
