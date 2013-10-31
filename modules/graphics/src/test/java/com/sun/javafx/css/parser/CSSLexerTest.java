/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.CharArrayReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class CSSLexerTest {
    
    public CSSLexerTest() {
    }
    
    private void checkTokens(List<Token> resultTokens, Token... expectedTokens) 
        throws org.junit.ComparisonFailure {
                
        if (expectedTokens.length != resultTokens.size()) {
            throw new org.junit.ComparisonFailure(
                "lengths do not match", 
                expectedTokens.toString(),
                resultTokens.toString()
            );
        }
        
        for (int n = 0; n<expectedTokens.length; n++) {
            
            final Token result = resultTokens.get(n);
            final Token expected = expectedTokens[n];
            
            if (expected.getType() != result.getType()) {
                throw new org.junit.ComparisonFailure(
                    "token " + n + " types do not match", 
                    Arrays.toString(expectedTokens),
                    resultTokens.toString()
                );
            }
            
            final String expectedText = expected.getText();
            final String resultText = result.getText();
            
            if (expectedText == null ? resultText != null : !expectedText.equals(resultText)) {
                throw new org.junit.ComparisonFailure(
                    "token " + n + " text does not match", 
                    Arrays.toString(expectedTokens),
                    resultTokens.toString()
                );
            }
        }
    }
    
    List<Token> getTokens(String string) {
        
        Reader reader = new CharArrayReader(string.toCharArray());
        final CSSLexer lexer = CSSLexer.getInstance();
        lexer.setReader(reader);
        
        final List<Token> tokens = new ArrayList<Token>();
        
        Token token = null;
        do {
            token = lexer.nextToken();
            tokens.add(token);
        } while (token.getType() != Token.EOF);

        return Collections.unmodifiableList(tokens);
    }
    
    private void lexDigitsWithUnits(String units, int type) throws org.junit.ComparisonFailure {
        
        checkTokens(getTokens("123"+units), new Token(type, "123"+units), Token.EOF_TOKEN);
        checkTokens(getTokens("123.45"+units), new Token(type, "123.45"+units), Token.EOF_TOKEN);
        checkTokens(getTokens(".45"+units), new Token(type, ".45"+units), Token.EOF_TOKEN);
        checkTokens(getTokens("-123"+units), new Token(type, "-123"+units), Token.EOF_TOKEN);
        checkTokens(getTokens("-.45"+units), new Token(type, "-.45"+units), Token.EOF_TOKEN);
        checkTokens(getTokens("+123"+units), new Token(type, "+123"+units), Token.EOF_TOKEN);
        checkTokens(getTokens("+.45"+units), new Token(type, "+.45"+units), Token.EOF_TOKEN);
    }
    
    @Test
    public void testLexValidDigits() {        
        lexDigitsWithUnits("", CSSLexer.NUMBER);
    }        

    @Test
    public void testLexValidDigitsWithCM() {        
        lexDigitsWithUnits("cm", CSSLexer.CM);
        // case should be ignored
        lexDigitsWithUnits("cM", CSSLexer.CM);
    }        
    @Test
    public void testLexValidDigitsWithDEG() {        
        lexDigitsWithUnits("deg", CSSLexer.DEG);
        // case should be ignored
        lexDigitsWithUnits("dEg", CSSLexer.DEG);
    }        
    @Test
    public void testLexValidDigitsWithEM() {        
        lexDigitsWithUnits("em", CSSLexer.EMS);
        // case should be ignored
        lexDigitsWithUnits("Em", CSSLexer.EMS);
    }        
    @Test
    public void testLexValidDigitsWithEX() {        
        lexDigitsWithUnits("ex", CSSLexer.EXS);
        // case should be ignored
        lexDigitsWithUnits("Ex", CSSLexer.EXS);
    }        
    @Test
    public void testLexValidDigitsWithGRAD() {        
        lexDigitsWithUnits("grad", CSSLexer.GRAD);
        // case should be ignored
        lexDigitsWithUnits("gRad", CSSLexer.GRAD);
    }        
    @Test
    public void testLexValidDigitsWithIN() {        
        lexDigitsWithUnits("in", CSSLexer.IN);
        // case should be ignored
        lexDigitsWithUnits("In", CSSLexer.IN);
    }        
    @Test
    public void testLexValidDigitsWithMM() {        
        lexDigitsWithUnits("mm", CSSLexer.MM);
        // case should be ignored
        lexDigitsWithUnits("mM", CSSLexer.MM);
    }        
    @Test
    public void testLexValidDigitsWithPC() {        
        lexDigitsWithUnits("pc", CSSLexer.PC);
        // case should be ignored
        lexDigitsWithUnits("Pc", CSSLexer.PC);
    }        
    @Test
    public void testLexValidDigitsWithPT() {        
        lexDigitsWithUnits("pt", CSSLexer.PT);
        // case should be ignored
        lexDigitsWithUnits("PT", CSSLexer.PT);
    }        
    @Test
    public void testLexValidDigitsWithPX() {        
        lexDigitsWithUnits("px", CSSLexer.PX);
        // case should be ignored
        lexDigitsWithUnits("Px", CSSLexer.PX);
    }        
    @Test
    public void testLexValidDigitsWithRAD() {        
        lexDigitsWithUnits("rad", CSSLexer.RAD);
        // case should be ignored
        lexDigitsWithUnits("RaD", CSSLexer.RAD);
    }
    @Test
    public void testLexValidDigitsWithTURN() {        
        lexDigitsWithUnits("turn", CSSLexer.TURN);
        // case should be ignored
        lexDigitsWithUnits("TurN", CSSLexer.TURN);
    }        
    @Test
    public void testLexValidDigitsWithPCT() {        
        lexDigitsWithUnits("%", CSSLexer.PERCENTAGE);
    }        
    @Test
    public void testLexValidDigitsWithBadUnits() { 
        lexDigitsWithUnits("xyzzy", Token.INVALID);
    }
    @Test 
    public void textLexValidDigitsValidDigits() {
        checkTokens(
            getTokens("foo: 10pt; bar: 20%;"),
            new Token(CSSLexer.IDENT, "foo"),  
            new Token(CSSLexer.COLON, ":"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PT, "10pt"),  
            new Token(CSSLexer.SEMI, ";"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.IDENT, "bar"),  
            new Token(CSSLexer.COLON, ":"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PERCENTAGE, "20%"),  
            new Token(CSSLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexInvalidDigitsValidDigits() {
        checkTokens(
            getTokens("foo: 10pz; bar: 20%;"),
            new Token(CSSLexer.IDENT, "foo"),  
            new Token(CSSLexer.COLON, ":"),  
            new Token(CSSLexer.WS, " "),  
            new Token(Token.INVALID, "10pz"),  
            new Token(CSSLexer.SEMI, ";"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.IDENT, "bar"),  
            new Token(CSSLexer.COLON, ":"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PERCENTAGE, "20%"),  
            new Token(CSSLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexValidDigitsBangImportant() {
        checkTokens(
            getTokens("foo: 10pt !important;"),
            new Token(CSSLexer.IDENT, "foo"),  
            new Token(CSSLexer.COLON, ":"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PT, "10pt"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.IMPORTANT_SYM, "!important"),  
            new Token(CSSLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexInvalidDigitsBangImportant() {
        checkTokens(
            getTokens("foo: 10pz !important;"),
            new Token(CSSLexer.IDENT, "foo"),  
            new Token(CSSLexer.COLON, ":"),  
            new Token(CSSLexer.WS, " "),  
            new Token(Token.INVALID, "10pz"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.IMPORTANT_SYM, "!important"),  
            new Token(CSSLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexValidDigitsInSequence() {
        checkTokens(
            getTokens("-1 0px 1pt .5em;"),
            new Token(CSSLexer.NUMBER, "-1"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PX, "0px"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PT, "1pt"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.EMS, ".5em"),  
            new Token(CSSLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexInvalidDigitsInSequence() {
        checkTokens(
            getTokens("-1 0px 1pz .5em;"),
            new Token(CSSLexer.NUMBER, "-1"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.PX, "0px"),  
            new Token(CSSLexer.WS, " "),  
            new Token(Token.INVALID, "1pz"),  
            new Token(CSSLexer.WS, " "),  
            new Token(CSSLexer.EMS, ".5em"),  
            new Token(CSSLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }

    @Test 
    public void testTokenOffset() {
        
        String str =  "a: b;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3),
            new Token(CSSLexer.SEMI,  ";", 1, 4),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
                
    }
    
    @Test 
    public void testTokenLineAndOffsetWithCR() {
        
        String str =  "a: b;\rc: d;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3),
            new Token(CSSLexer.SEMI,  ";", 1, 4),
            new Token(CSSLexer.NL,  "\\r", 1, 5),
            new Token(CSSLexer.IDENT, "c", 2, 0),
            new Token(CSSLexer.COLON, ":", 2, 1),
            new Token(CSSLexer.WS,    " ", 2, 2),
            new Token(CSSLexer.IDENT, "d", 2, 3),
            new Token(CSSLexer.SEMI,  ";", 2, 4),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
                
    }

    @Test 
    public void testTokenLineAndOffsetWithLF() {
        
        String str =  "a: b;\nc: d;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3),
            new Token(CSSLexer.SEMI,  ";", 1, 4),
            new Token(CSSLexer.NL,  "\\n", 1, 5),
            new Token(CSSLexer.IDENT, "c", 2, 0),
            new Token(CSSLexer.COLON, ":", 2, 1),
            new Token(CSSLexer.WS,    " ", 2, 2),
            new Token(CSSLexer.IDENT, "d", 2, 3),
            new Token(CSSLexer.SEMI,  ";", 2, 4),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
                
    }

    @Test 
    public void testTokenLineAndOffsetWithCRLF() {
        //             012345   01234
        String str =  "a: b;\r\nc: d;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3),
            new Token(CSSLexer.SEMI,  ";", 1, 4),
            new Token(CSSLexer.NL,  "\\r\\n", 1, 5),
            new Token(CSSLexer.IDENT, "c", 2, 0),
            new Token(CSSLexer.COLON, ":", 2, 1),
            new Token(CSSLexer.WS,    " ", 2, 2),
            new Token(CSSLexer.IDENT, "d", 2, 3),
            new Token(CSSLexer.SEMI,  ";", 2, 4),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
                
    }
    
    @Test 
    public void testTokenOffsetWithEmbeddedComment() {
        //             0123456789012345
        String str =  "a: /*comment*/b;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 14), 
            new Token(CSSLexer.SEMI,  ";", 1, 15),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }

    @Test 
    public void testTokenLineAndOffsetWithLeadingComment() {
        //             012345678901 01234
        String str =  "/*comment*/\na: b;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.NL, "\\n", 1, 11),
            new Token(CSSLexer.IDENT, "a", 2, 0),
            new Token(CSSLexer.COLON, ":", 2, 1),
            new Token(CSSLexer.WS,    " ", 2, 2),
            new Token(CSSLexer.IDENT, "b", 2, 3), 
            new Token(CSSLexer.SEMI,  ";", 2, 4),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }
    
    @Test 
    public void testTokenOffsetWithFunction() {
        //             01234567890
        String str =  "a: b(arg);";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3), 
            new Token(CSSLexer.LPAREN, "(", 1, 4), 
            new Token(CSSLexer.IDENT, "arg", 1, 5), 
            new Token(CSSLexer.RPAREN, ")", 1, 8), 
            new Token(CSSLexer.SEMI,  ";", 1, 9),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }
    
    @Test 
    public void testTokenOffsetWithHash() {
        //             01234567890
        String str =  "a: #012345;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.HASH, "#012345", 1, 3), 
            new Token(CSSLexer.SEMI,  ";", 1, 10),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }
 
    @Test 
    public void testTokenOffsetWithDigits() {
        //             01234567890
        String str =  "a: 123.45;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.NUMBER, "123.45", 1, 3), 
            new Token(CSSLexer.SEMI,  ";", 1, 9),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }

    @Test 
    public void testTokenOffsetWithBangImportant() {
        //             0123456789012345
        String str =  "a: b !important;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3),
            new Token(CSSLexer.WS,    " ", 1, 4),
            new Token(CSSLexer.IMPORTANT_SYM, "!important", 1, 5), 
            new Token(CSSLexer.SEMI,  ";", 1, 15),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }

    @Test 
    public void testTokenOffsetWithSkip() {
        //             0123456789012345
        String str =  "a: b !imporzant;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(CSSLexer.IDENT, "b", 1, 3),
            new Token(CSSLexer.WS,    " ", 1, 4),
            new Token(Token.SKIP, "!imporz", 1, 5), 
            new Token(CSSLexer.SEMI,  ";", 1, 15),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }
    
    @Test 
    public void testTokenOffsetWithInvalid() {
        //             0123456789012345
        String str =  "a: 1pz;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.IDENT, "a", 1, 0),
            new Token(CSSLexer.COLON, ":", 1, 1),
            new Token(CSSLexer.WS,    " ", 1, 2),
            new Token(Token.INVALID, "1pz", 1, 3),
            new Token(CSSLexer.SEMI,  ";", 1, 6),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }

    @Test
    public void testTokenLineAndOffsetMoreFully() {
        //             1            2                 3         4
        //             012345678901 0123456789012345  012345678 0
        String str =  "/*comment*/\n*.foo#bar:baz {\n\ta: 1em;\n}";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CSSLexer.NL,     "\\n",  1, 11),
            new Token(CSSLexer.STAR,   "*",    2, 0),
            new Token(CSSLexer.DOT,    ".",    2, 1),
            new Token(CSSLexer.IDENT,  "foo",  2, 2),
            new Token(CSSLexer.HASH,   "#bar", 2, 5),
            new Token(CSSLexer.COLON,  ":",    2, 9),
            new Token(CSSLexer.IDENT,  "baz",  2, 10),
            new Token(CSSLexer.WS,     " ",    2, 13),
            new Token(CSSLexer.LBRACE, "{",    2, 14),
            new Token(CSSLexer.NL,     "\\n",  2, 15),
            new Token(CSSLexer.WS,     "\t",   3, 0),
            new Token(CSSLexer.IDENT,  "a",    3, 1),
            new Token(CSSLexer.COLON,  ":",    3, 2),
            new Token(CSSLexer.WS,     " ",    3, 3),
            new Token(CSSLexer.EMS,    "1em",  3, 4), 
            new Token(CSSLexer.SEMI,   ";",    3, 7),
            new Token(CSSLexer.NL,     "\\n",  3, 8),
            new Token(CSSLexer.RBRACE, "}",    4, 0),
            Token.EOF_TOKEN
        };
        
        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);
        
        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }
    }

    @Test
    public void testScanUrl() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(http://foo.bar.com/fonts/serif/fubar.ttf)";
        Token[] expected = new Token[]{
            new Token(CSSLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
            Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanUrlWithWhiteSpace() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(    http://foo.bar.com/fonts/serif/fubar.ttf\t)";
        Token[] expected = new Token[]{
                new Token(CSSLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanQuotedUrlWithWhiteSpace() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(    'http://foo.bar.com/fonts/serif/fubar.ttf'\t)";
        Token[] expected = new Token[]{
                new Token(CSSLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanQuotedUrl() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(\"http://foo.bar.com/fonts/serif/fubar.ttf\")";
        Token[] expected = new Token[]{
                new Token(CSSLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanUrlWithEscapes() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(http://foo.bar.com/fonts/true\\ type/fubar.ttf)";
        Token[] expected = new Token[]{
                new Token(CSSLexer.URL, "http://foo.bar.com/fonts/true type/fubar.ttf", 1, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanQuotedUrlWithEscapes() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(\"http://foo.bar.com/fonts/true\\ type/fubar.ttf\")";
        Token[] expected = new Token[]{
                new Token(CSSLexer.URL, "http://foo.bar.com/fonts/true type/fubar.ttf", 1, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanUrlWithSyntaxError() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url(http://foo.bar.com/fonts/true'type/fubar.ttf)";
        Token[] expected = new Token[]{
                new Token(Token.INVALID, "http://foo.bar.com/fonts/true", 1, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

    @Test
    public void testScanQuotedUrlWithSyntaxError() {

        //             1           2               3        4
        //             01234567890101234567890123450123456780123456789
        String str =  "url('http://foo.bar.com/fonts/true\rtype/fubar.ttf')";
        Token[] expected = new Token[]{
                new Token(Token.INVALID, "http://foo.bar.com/fonts/true", 2, 0),
                Token.EOF_TOKEN
        };

        List<Token> tlist = getTokens(str);
        checkTokens(tlist, expected);

        for(int n=0; n<tlist.size(); n++) {
            Token tok = tlist.get(n);
            assertEquals("bad line. tok="+tok, expected[n].getLine(), tok.getLine());
            assertEquals("bad offset. tok="+tok, expected[n].getOffset(), tok.getOffset());
        }

    }

}
