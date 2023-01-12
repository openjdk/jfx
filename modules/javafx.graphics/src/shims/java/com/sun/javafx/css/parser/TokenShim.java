/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css.parser;

public class TokenShim {

    public static final int EOF = Token.EOF;
    public static final int INVALID = Token.INVALID;
    public static final int SKIP = Token.SKIP;

    public final static TokenShim EOF_TOKEN = new TokenShim(Token.EOF_TOKEN);
    public final static TokenShim INVALID_TOKEN = new TokenShim(Token.INVALID_TOKEN);
    public final static TokenShim SKIP_TOKEN = new TokenShim(Token.SKIP_TOKEN);

    private final Token token;

    public TokenShim(int type, String text, int line, int offset) {
        token = new Token(type, text, line, offset);
    }

    public TokenShim(int type, String text) {
        token = new Token(type, text);
    }

    public TokenShim(Token t) {
        token = t;
    }

    public int getType() {
        return token.getType();
    }

    public int getLine() {
        return token.getLine();
    }

    public int getOffset() {
        return token.getOffset();
    }

    public String getText() {
        return token.getText();
    }


}
