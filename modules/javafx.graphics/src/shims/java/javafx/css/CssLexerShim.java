/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package javafx.css;

import com.sun.javafx.css.parser.Token;
import com.sun.javafx.css.parser.TokenShim;
import java.io.Reader;

public class CssLexerShim {

    public final static int STRING = CssLexer.STRING;
    public final static int IDENT = CssLexer.IDENT;
    public final static int FUNCTION = CssLexer.FUNCTION;
    public final static int NUMBER = CssLexer.NUMBER;
    public final static int CM = CssLexer.CM;
    public final static int EMS = CssLexer.EMS;
    public final static int EXS = CssLexer.EXS;
    public final static int IN = CssLexer.IN;
    public final static int MM = CssLexer.MM;
    public final static int PC = CssLexer.PC;
    public final static int PT = CssLexer.PT;
    public final static int PX = CssLexer.PX;
    public final static int PERCENTAGE = CssLexer.PERCENTAGE;
    public final static int DEG = CssLexer.DEG;
    public final static int GRAD = CssLexer.GRAD;
    public final static int RAD = CssLexer.RAD;
    public final static int TURN = CssLexer.TURN;
    public final static int GREATER = CssLexer.GREATER;
    public final static int LBRACE = CssLexer.LBRACE;
    public final static int RBRACE = CssLexer.RBRACE;
    public final static int SEMI = CssLexer.SEMI;
    public final static int COLON = CssLexer.COLON;
    public final static int SOLIDUS = CssLexer.SOLIDUS;
    public final static int STAR = CssLexer.STAR;
    public final static int LPAREN = CssLexer.LPAREN;
    public final static int RPAREN = CssLexer.RPAREN;
    public final static int COMMA = CssLexer.COMMA;
    public final static int HASH = CssLexer.HASH;
    public final static int DOT = CssLexer.DOT;
    public final static int IMPORTANT_SYM = CssLexer.IMPORTANT_SYM;
    public final static int WS = CssLexer.WS;
    public final static int NL = CssLexer.NL;
    public final static int FONT_FACE = CssLexer.FONT_FACE;
    public final static int URL = CssLexer.URL;
    public final static int IMPORT = CssLexer.IMPORT;
    public final static int SECONDS = CssLexer.SECONDS;
    public final static int MS = CssLexer.MS;
    public final static int AT_KEYWORD = CssLexer.AT_KEYWORD;


    CssLexer lexer;

    public CssLexerShim(CssLexer lexer) {
        this.lexer = lexer;
    }

    public CssLexerShim() {
        this.lexer = new CssLexer();
    }

    public TokenShim nextToken() {
        Token t = this.lexer.nextToken();
        return new TokenShim(t);
    }

    public void setReader(Reader reader) {
        lexer.setReader(reader);
    }

    //------------

    public static TokenShim nextToken(CssLexerShim l) {
        Token t = l.lexer.nextToken();
        return new TokenShim(t);
    }


}
