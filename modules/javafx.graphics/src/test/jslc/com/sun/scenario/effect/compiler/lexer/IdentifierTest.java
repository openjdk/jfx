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

package com.sun.scenario.effect.compiler.lexer;

import com.sun.scenario.effect.compiler.JSLLexer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdentifierTest extends LexerBase {

    @Test
    public void basic() {
        assertRecognized("foo");
    }

    @Test
    public void mixedCase() {
        assertRecognized("aAbBCCdd");
    }

    @Test
    public void lettersAndDigits() {
        assertRecognized("aA29");
    }

    @Test
    public void lettersAndDigitsAndSymbols() {
        assertRecognized("$aA___29");
    }

    @Test
    public void notAnId1() {
        assertNotRecognized("6foo", "6");
    }

    @Test
    public void notAnId2() {
        assertThrows(ParseCancellationException.class, () -> {
            assertRecognized("###");
        });
    }

    @Override
    protected int expectedTokenType() {
        return JSLLexer.IDENTIFIER;
    }
}
