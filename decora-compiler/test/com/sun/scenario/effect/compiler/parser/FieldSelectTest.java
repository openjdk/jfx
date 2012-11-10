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
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import static org.junit.Assert.*;

public class FieldSelectTest extends ParserBase {

    @Test
    public void rgba() throws Exception {
        String tree = parseTreeFor(".rgba");
        assertEquals(tree, ".rgba");
    }

    @Test
    public void rgb() throws Exception {
        String tree = parseTreeFor(".rgb");
        assertEquals(tree, ".rgb");
    }

    @Test
    public void rg() throws Exception {
        String tree = parseTreeFor(".rg");
        assertEquals(tree, ".rg");
    }

    @Test
    public void r() throws Exception {
        String tree = parseTreeFor(".r");
        assertEquals(tree, ".r");
    }

    @Test
    public void aaaa() throws Exception {
        String tree = parseTreeFor(".aaaa");
        assertEquals(tree, ".aaaa");
    }

    @Test
    public void abgr() throws Exception {
        String tree = parseTreeFor(".abgr");
        assertEquals(tree, ".abgr");
    }
    
    @Test
    public void xyzw() throws Exception {
        String tree = parseTreeFor(".xyzw");
        assertEquals(tree, ".xyzw");
    }

    @Test
    public void xyz() throws Exception {
        String tree = parseTreeFor(".xyz");
        assertEquals(tree, ".xyz");
    }

    @Test
    public void xy() throws Exception {
        String tree = parseTreeFor(".xy");
        assertEquals(tree, ".xy");
    }

    @Test
    public void x() throws Exception {
        String tree = parseTreeFor(".x");
        assertEquals(tree, ".x");
    }

    @Test
    public void zzz() throws Exception {
        String tree = parseTreeFor(".zzz");
        assertEquals(tree, ".zzz");
    }

    @Test
    public void wzyz() throws Exception {
        String tree = parseTreeFor(".wzyx");
        assertEquals(tree, ".wzyx");
    }

    @Test(expected = RecognitionException.class)
    public void notAFieldSelection1() throws Exception {
        parseTreeFor("qpz");
    }

    @Test(expected = AssertionFailedError.class)
    public void notAFieldSelection2() throws Exception {
        parseTreeFor(".xqpz", true);
    }

    @Test(expected = AssertionFailedError.class)
    public void tooManyVals() throws Exception {
        parseTreeFor(".xyzwx", true);
    }

    @Test(expected = AssertionFailedError.class)
    public void mixedVals() throws Exception {
        parseTreeFor(".xyba", true);
    }

    private String parseTreeFor(String text) throws RecognitionException {
        return parseTreeFor(text, false);
    }

    private String parseTreeFor(String text, boolean expectEx) throws RecognitionException {
        JSLParser parser = parserOver(text);
        String ret = parser.field_selection();
        // TODO: there's probably a better way to check for trailing (invalid) characters
        boolean sawException = false;
        try {
            parser.field_selection();
        } catch (Exception e) {
            sawException = true;
        }
        if (sawException == expectEx) {
            Assert.fail(expectEx ? "Expecting EOF" : "Not expecting EOF");
        }
        return ret;
    }
}
