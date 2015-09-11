/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.CharArrayReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class CssLexerTest {
    
    public CssLexerTest() {
    }
    
    private void checkTokens(List<Token> resultTokens, Token... expectedTokens) 
        throws org.junit.ComparisonFailure {
                
        if (expectedTokens.length != resultTokens.size()) {
            throw new org.junit.ComparisonFailure(
                "lengths do not match",
                    Arrays.toString(expectedTokens),
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
        final CssLexer lexer = new CssLexer();
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
        lexDigitsWithUnits("", CssLexer.NUMBER);
    }        

    @Test
    public void testLexValidDigitsWithCM() {        
        lexDigitsWithUnits("cm", CssLexer.CM);
        // case should be ignored
        lexDigitsWithUnits("cM", CssLexer.CM);
    }        
    @Test
    public void testLexValidDigitsWithDEG() {        
        lexDigitsWithUnits("deg", CssLexer.DEG);
        // case should be ignored
        lexDigitsWithUnits("dEg", CssLexer.DEG);
    }        
    @Test
    public void testLexValidDigitsWithEM() {        
        lexDigitsWithUnits("em", CssLexer.EMS);
        // case should be ignored
        lexDigitsWithUnits("Em", CssLexer.EMS);
    }        
    @Test
    public void testLexValidDigitsWithEX() {        
        lexDigitsWithUnits("ex", CssLexer.EXS);
        // case should be ignored
        lexDigitsWithUnits("Ex", CssLexer.EXS);
    }        
    @Test
    public void testLexValidDigitsWithGRAD() {        
        lexDigitsWithUnits("grad", CssLexer.GRAD);
        // case should be ignored
        lexDigitsWithUnits("gRad", CssLexer.GRAD);
    }        
    @Test
    public void testLexValidDigitsWithIN() {        
        lexDigitsWithUnits("in", CssLexer.IN);
        // case should be ignored
        lexDigitsWithUnits("In", CssLexer.IN);
    }        
    @Test
    public void testLexValidDigitsWithMM() {        
        lexDigitsWithUnits("mm", CssLexer.MM);
        // case should be ignored
        lexDigitsWithUnits("mM", CssLexer.MM);
    }        
    @Test
    public void testLexValidDigitsWithPC() {        
        lexDigitsWithUnits("pc", CssLexer.PC);
        // case should be ignored
        lexDigitsWithUnits("Pc", CssLexer.PC);
    }        
    @Test
    public void testLexValidDigitsWithPT() {        
        lexDigitsWithUnits("pt", CssLexer.PT);
        // case should be ignored
        lexDigitsWithUnits("PT", CssLexer.PT);
    }        
    @Test
    public void testLexValidDigitsWithPX() {        
        lexDigitsWithUnits("px", CssLexer.PX);
        // case should be ignored
        lexDigitsWithUnits("Px", CssLexer.PX);
    }        
    @Test
    public void testLexValidDigitsWithRAD() {        
        lexDigitsWithUnits("rad", CssLexer.RAD);
        // case should be ignored
        lexDigitsWithUnits("RaD", CssLexer.RAD);
    }
    @Test
    public void testLexValidDigitsWithTURN() {        
        lexDigitsWithUnits("turn", CssLexer.TURN);
        // case should be ignored
        lexDigitsWithUnits("TurN", CssLexer.TURN);
    }
    @Test
    public void testLexValidDigitsWithS() {
        lexDigitsWithUnits("s", CssLexer.SECONDS);
        // case should be ignored
        lexDigitsWithUnits("S", CssLexer.SECONDS);
    }
    @Test
    public void testLexValidDigitsWithMS() {
        lexDigitsWithUnits("ms", CssLexer.MS);
        // case should be ignored
        lexDigitsWithUnits("mS", CssLexer.MS);
    }
    @Test
    public void testLexValidDigitsWithPCT() {        
        lexDigitsWithUnits("%", CssLexer.PERCENTAGE);
    }        
    @Test
    public void testLexValidDigitsWithBadUnits() { 
        lexDigitsWithUnits("xyzzy", Token.INVALID);
    }
    @Test 
    public void textLexValidDigitsValidDigits() {
        checkTokens(
            getTokens("foo: 10pt; bar: 20%;"),
            new Token(CssLexer.IDENT, "foo"),  
            new Token(CssLexer.COLON, ":"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PT, "10pt"),  
            new Token(CssLexer.SEMI, ";"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.IDENT, "bar"),  
            new Token(CssLexer.COLON, ":"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PERCENTAGE, "20%"),  
            new Token(CssLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexInvalidDigitsValidDigits() {
        checkTokens(
            getTokens("foo: 10pz; bar: 20%;"),
            new Token(CssLexer.IDENT, "foo"),  
            new Token(CssLexer.COLON, ":"),  
            new Token(CssLexer.WS, " "),  
            new Token(Token.INVALID, "10pz"),  
            new Token(CssLexer.SEMI, ";"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.IDENT, "bar"),  
            new Token(CssLexer.COLON, ":"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PERCENTAGE, "20%"),  
            new Token(CssLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexValidDigitsBangImportant() {
        checkTokens(
            getTokens("foo: 10pt !important;"),
            new Token(CssLexer.IDENT, "foo"),  
            new Token(CssLexer.COLON, ":"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PT, "10pt"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.IMPORTANT_SYM, "!important"),  
            new Token(CssLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexInvalidDigitsBangImportant() {
        checkTokens(
            getTokens("foo: 10pz !important;"),
            new Token(CssLexer.IDENT, "foo"),  
            new Token(CssLexer.COLON, ":"),  
            new Token(CssLexer.WS, " "),  
            new Token(Token.INVALID, "10pz"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.IMPORTANT_SYM, "!important"),  
            new Token(CssLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexValidDigitsInSequence() {
        checkTokens(
            getTokens("-1 0px 1pt .5em;"),
            new Token(CssLexer.NUMBER, "-1"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PX, "0px"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PT, "1pt"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.EMS, ".5em"),  
            new Token(CssLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }
    @Test 
    public void textLexInvalidDigitsInSequence() {
        checkTokens(
            getTokens("-1 0px 1pz .5em;"),
            new Token(CssLexer.NUMBER, "-1"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.PX, "0px"),  
            new Token(CssLexer.WS, " "),  
            new Token(Token.INVALID, "1pz"),  
            new Token(CssLexer.WS, " "),  
            new Token(CssLexer.EMS, ".5em"),  
            new Token(CssLexer.SEMI, ";"),  
            Token.EOF_TOKEN
        );
    }

    @Test 
    public void testTokenOffset() {
        
        String str =  "a: b;";
        // [?][0] = line
        // [?][1] = offset
        Token[] expected = {
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3),
            new Token(CssLexer.SEMI,  ";", 1, 4),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3),
            new Token(CssLexer.SEMI,  ";", 1, 4),
            new Token(CssLexer.NL,  "\\r", 1, 5),
            new Token(CssLexer.IDENT, "c", 2, 0),
            new Token(CssLexer.COLON, ":", 2, 1),
            new Token(CssLexer.WS,    " ", 2, 2),
            new Token(CssLexer.IDENT, "d", 2, 3),
            new Token(CssLexer.SEMI,  ";", 2, 4),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3),
            new Token(CssLexer.SEMI,  ";", 1, 4),
            new Token(CssLexer.NL,  "\\n", 1, 5),
            new Token(CssLexer.IDENT, "c", 2, 0),
            new Token(CssLexer.COLON, ":", 2, 1),
            new Token(CssLexer.WS,    " ", 2, 2),
            new Token(CssLexer.IDENT, "d", 2, 3),
            new Token(CssLexer.SEMI,  ";", 2, 4),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3),
            new Token(CssLexer.SEMI,  ";", 1, 4),
            new Token(CssLexer.NL,  "\\r\\n", 1, 5),
            new Token(CssLexer.IDENT, "c", 2, 0),
            new Token(CssLexer.COLON, ":", 2, 1),
            new Token(CssLexer.WS,    " ", 2, 2),
            new Token(CssLexer.IDENT, "d", 2, 3),
            new Token(CssLexer.SEMI,  ";", 2, 4),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 14), 
            new Token(CssLexer.SEMI,  ";", 1, 15),
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
            new Token(CssLexer.NL, "\\n", 1, 11),
            new Token(CssLexer.IDENT, "a", 2, 0),
            new Token(CssLexer.COLON, ":", 2, 1),
            new Token(CssLexer.WS,    " ", 2, 2),
            new Token(CssLexer.IDENT, "b", 2, 3), 
            new Token(CssLexer.SEMI,  ";", 2, 4),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3), 
            new Token(CssLexer.LPAREN, "(", 1, 4), 
            new Token(CssLexer.IDENT, "arg", 1, 5), 
            new Token(CssLexer.RPAREN, ")", 1, 8), 
            new Token(CssLexer.SEMI,  ";", 1, 9),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.HASH, "#012345", 1, 3), 
            new Token(CssLexer.SEMI,  ";", 1, 10),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.NUMBER, "123.45", 1, 3), 
            new Token(CssLexer.SEMI,  ";", 1, 9),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3),
            new Token(CssLexer.WS,    " ", 1, 4),
            new Token(CssLexer.IMPORTANT_SYM, "!important", 1, 5), 
            new Token(CssLexer.SEMI,  ";", 1, 15),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(CssLexer.IDENT, "b", 1, 3),
            new Token(CssLexer.WS,    " ", 1, 4),
            new Token(Token.SKIP, "!imporz", 1, 5), 
            new Token(CssLexer.SEMI,  ";", 1, 15),
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
            new Token(CssLexer.IDENT, "a", 1, 0),
            new Token(CssLexer.COLON, ":", 1, 1),
            new Token(CssLexer.WS,    " ", 1, 2),
            new Token(Token.INVALID, "1pz", 1, 3),
            new Token(CssLexer.SEMI,  ";", 1, 6),
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
            new Token(CssLexer.NL,     "\\n",  1, 11),
            new Token(CssLexer.STAR,   "*",    2, 0),
            new Token(CssLexer.DOT,    ".",    2, 1),
            new Token(CssLexer.IDENT,  "foo",  2, 2),
            new Token(CssLexer.HASH,   "#bar", 2, 5),
            new Token(CssLexer.COLON,  ":",    2, 9),
            new Token(CssLexer.IDENT,  "baz",  2, 10),
            new Token(CssLexer.WS,     " ",    2, 13),
            new Token(CssLexer.LBRACE, "{",    2, 14),
            new Token(CssLexer.NL,     "\\n",  2, 15),
            new Token(CssLexer.WS,     "\t",   3, 0),
            new Token(CssLexer.IDENT,  "a",    3, 1),
            new Token(CssLexer.COLON,  ":",    3, 2),
            new Token(CssLexer.WS,     " ",    3, 3),
            new Token(CssLexer.EMS,    "1em",  3, 4), 
            new Token(CssLexer.SEMI,   ";",    3, 7),
            new Token(CssLexer.NL,     "\\n",  3, 8),
            new Token(CssLexer.RBRACE, "}",    4, 0),
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
            new Token(CssLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
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
                new Token(CssLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
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
                new Token(CssLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
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
                new Token(CssLexer.URL, "http://foo.bar.com/fonts/serif/fubar.ttf", 1, 0),
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
                new Token(CssLexer.URL, "http://foo.bar.com/fonts/true type/fubar.ttf", 1, 0),
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
                new Token(CssLexer.URL, "http://foo.bar.com/fonts/true type/fubar.ttf", 1, 0),
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
