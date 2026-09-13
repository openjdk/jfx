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
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FieldSelectTest extends ParserBase {

    @Test
    public void rgba() {
        String tree = parseTreeFor(".rgba");
        assertEquals(tree, ".rgba");
    }

    @Test
    public void rgb() {
        String tree = parseTreeFor(".rgb");
        assertEquals(tree, ".rgb");
    }

    @Test
    public void rg() {
        String tree = parseTreeFor(".rg");
        assertEquals(tree, ".rg");
    }

    @Test
    public void r() {
        String tree = parseTreeFor(".r");
        assertEquals(tree, ".r");
    }

    @Test
    public void aaaa() {
        String tree = parseTreeFor(".aaaa");
        assertEquals(tree, ".aaaa");
    }

    @Test
    public void abgr() {
        String tree = parseTreeFor(".abgr");
        assertEquals(tree, ".abgr");
    }

    @Test
    public void xyzw() {
        String tree = parseTreeFor(".xyzw");
        assertEquals(tree, ".xyzw");
    }

    @Test
    public void xyz() {
        String tree = parseTreeFor(".xyz");
        assertEquals(tree, ".xyz");
    }

    @Test
    public void xy() {
        String tree = parseTreeFor(".xy");
        assertEquals(tree, ".xy");
    }

    @Test
    public void x() {
        String tree = parseTreeFor(".x");
        assertEquals(tree, ".x");
    }

    @Test
    public void zzz() {
        String tree = parseTreeFor(".zzz");
        assertEquals(tree, ".zzz");
    }

    @Test
    public void wzyz() {
        String tree = parseTreeFor(".wzyx");
        assertEquals(tree, ".wzyx");
    }

    @Test
    public void notAFieldSelection1() {
        assertThrows(ParseCancellationException.class, () -> {
            parseTreeFor("qpz");
        });
    }

    @Test
    public void notAFieldSelection2() {
        assertThrows(AssertionFailedError.class, () -> {
            parseTreeFor(".xqpz", true);
        });
    }

    @Test
    public void tooManyVals() {
        assertThrows(AssertionFailedError.class, () -> {
            parseTreeFor(".xyzwx", true);
        });
    }

    @Test
    public void mixedVals() {
        assertThrows(AssertionFailedError.class, () -> {
            parseTreeFor(".xyba", true);
        });
    }

    private String parseTreeFor(String text) {
        return parseTreeFor(text, false);
    }

    private String parseTreeFor(String text, boolean expectEx) {
        JSLParser parser = parserOver(text);
        JSLVisitor visitor = new JSLVisitor();
        String ret = visitor.visitField_selection(parser.field_selection()).getString();
        // TODO: there's probably a better way to check for trailing (invalid) characters
        boolean sawException = false;
        try {
            visitor.visitField_selection(parser.field_selection());
        } catch (Exception e) {
            sawException = true;
        }
        if (sawException == expectEx) {
            Assert.fail(expectEx ? "Expecting EOF" : "Not expecting EOF");
        }
        return ret;
    }
}
