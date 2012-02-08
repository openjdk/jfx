/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

}
