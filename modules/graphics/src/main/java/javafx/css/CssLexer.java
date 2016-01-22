/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.css.parser.LexerState;
import com.sun.javafx.css.parser.Recognizer;
import com.sun.javafx.css.parser.Token;


final class CssLexer {
    final static int STRING = 10;
    final static int IDENT = 11;
    final static int FUNCTION = 12;
    final static int NUMBER = 13;
    final static int CM = 14;
    final static int EMS = 15;
    final static int EXS = 16;
    final static int IN = 17;
    final static int MM = 18;
    final static int PC = 19;
    final static int PT = 20;
    final static int PX = 21;
    final static int PERCENTAGE = 22;
    final static int DEG = 23;
    final static int GRAD = 24;
    final static int RAD = 25;
    final static int TURN = 26;
    final static int GREATER = 27;
    final static int LBRACE = 28;
    final static int RBRACE = 29;
    final static int SEMI = 30;
    final static int COLON = 31;
    final static int SOLIDUS = 32;
    final static int STAR = 33;
    final static int LPAREN = 34;
    final static int RPAREN = 35;
    final static int COMMA = 36;
    final static int HASH = 37;
    final static int DOT = 38;
    final static int IMPORTANT_SYM = 39;
    final static int WS = 40;
    final static int NL = 41;
    final static int FONT_FACE = 42;
    final static int URL = 43;
    final static int IMPORT = 44;
    final static int SECONDS = 45;
    final static int MS = 46;
    final static int AT_KEYWORD = 47;

    private final Recognizer A = (c) -> c == 'a' || c == 'A';
    private final Recognizer B = (c) -> c == 'b' || c == 'B';
    private final Recognizer C = (c) -> c == 'c' || c == 'C';
    private final Recognizer D = (c) -> c == 'd' || c == 'D';
    private final Recognizer E = (c) -> c == 'e' || c == 'E';
    private final Recognizer F = (c) -> c == 'f' || c == 'F';
    private final Recognizer G = (c) -> c == 'g' || c == 'G';
    private final Recognizer H = (c) -> c == 'h' || c == 'H';
    private final Recognizer I = (c) -> c == 'i' || c == 'I';
    private final Recognizer J = (c) -> c == 'j' || c == 'J';
    private final Recognizer K = (c) -> c == 'k' || c == 'K';
    private final Recognizer L = (c) -> c == 'l' || c == 'L';
    private final Recognizer M = (c) -> c == 'm' || c == 'M';
    private final Recognizer N = (c) -> c == 'n' || c == 'N';
    private final Recognizer O = (c) -> c == 'o' || c == 'O';
    private final Recognizer P = (c) -> c == 'p' || c == 'P';
    private final Recognizer Q = (c) -> c == 'q' || c == 'Q';
    private final Recognizer R = (c) -> c == 'r' || c == 'R';
    private final Recognizer S = (c) -> c == 's' || c == 'S';
    private final Recognizer T = (c) -> c == 't' || c == 'T';
    private final Recognizer U = (c) -> c == 'u' || c == 'U';
    private final Recognizer V = (c) -> c == 'v' || c == 'V';
    private final Recognizer W = (c) -> c == 'w' || c == 'W';
    private final Recognizer X = (c) -> c == 'x' || c == 'X';
    private final Recognizer Y = (c) -> c == 'y' || c == 'Y';
    private final Recognizer Z = (c) -> c == 'z' || c == 'Z';
    private final Recognizer ALPHA =  (c) -> ('a' <= c && c <= 'z') ||
           ('A' <= c && c <= 'Z');

    private final Recognizer NON_ASCII = (c) -> '\u0080' <= c && c <= '\uFFFF';

    private final Recognizer DOT_CHAR =        (c) -> c == '.';
    private final Recognizer GREATER_CHAR =    (c) -> c == '>';
    private final Recognizer LBRACE_CHAR =     (c) -> c == '{';
    private final Recognizer RBRACE_CHAR =     (c) -> c == '}';
    private final Recognizer SEMI_CHAR  =      (c) -> c == ';';
    private final Recognizer COLON_CHAR =      (c) -> c == ':';
    private final Recognizer SOLIDUS_CHAR =    (c) -> c == '/';
    private final Recognizer MINUS_CHAR =      (c) -> c == '-';
    private final Recognizer PLUS_CHAR =       (c) -> c == '+';
    private final Recognizer STAR_CHAR =       (c) -> c == '*';
    private final Recognizer LPAREN_CHAR =     (c) -> c == '(';
    private final Recognizer RPAREN_CHAR =     (c) -> c == ')';
    private final Recognizer COMMA_CHAR =      (c) -> c == ',';
    private final Recognizer UNDERSCORE_CHAR = (c) -> c == '_';
    private final Recognizer HASH_CHAR =       (c) -> c == '#';

    private final Recognizer WS_CHARS = (c) -> c == ' '  ||
           c == '\t' ||
           c == '\r' ||
           c == '\n' ||
           c == '\f';
    private final Recognizer NL_CHARS = (c) -> (c == '\r' || c == '\n');

    private final Recognizer DIGIT = (c) -> '0' <= c && c <= '9';

    private final Recognizer HEX_DIGIT = (c) -> ('0' <= c && c <= '9') ||
           ('a' <= c && c <= 'f') ||
           ('A' <= c && c <= 'F');

    // The initial accepts any character
    final LexerState initState = new LexerState("initState", null) {
        @Override public boolean accepts(int c) { return true; }
    };

    final LexerState hashState = new LexerState("hashState",
        HASH_CHAR
    );

    final LexerState minusState = new LexerState("minusState",
        MINUS_CHAR
    );

    final LexerState plusState = new LexerState("plusState",
        PLUS_CHAR
    );

    // The dot char is either just a dot or may be the start of a number
    final LexerState dotState = new LexerState(DOT, "dotState",
        DOT_CHAR
    );

    // [_a-z]|{nonascii}|{escape}
    final LexerState nmStartState = new LexerState(IDENT, "nmStartState",
        UNDERSCORE_CHAR, ALPHA
    );

    // nmchar       [_a-z0-9-]|{nonascii}|{escape}
    final LexerState nmCharState = new LexerState(IDENT, "nmCharState",
        UNDERSCORE_CHAR, ALPHA, DIGIT, MINUS_CHAR
    );

    // same as nmchar, but need to differentiate between nmchar in ident and
    // nmchar in
    final LexerState hashNameCharState = new LexerState(HASH, "hashNameCharState",
        UNDERSCORE_CHAR, ALPHA, DIGIT, MINUS_CHAR
    );

    // lparen after ident implies function
    final LexerState lparenState = new LexerState(FUNCTION, "lparenState",
        LPAREN_CHAR
    ) {
        @Override public int getType() {

            if (text.indexOf("url(") == 0) {
                try {
                    return consumeUrl();
                } catch (IOException ioe) {
                    return Token.INVALID;
                }
            }
            return super.getType();
        }
    };


    // initial digits in a number
    final LexerState leadingDigitsState = new LexerState(NUMBER,"leadingDigitsState",
        DIGIT
    );

    // If the dot char follows leading digits, a plus or a minus, then it is
    // a decimal mark
    final LexerState decimalMarkState = new LexerState("decimalMarkState",
        DOT_CHAR
    );

    // digits following decimal mark
    final LexerState trailingDigitsState = new LexerState(NUMBER,"trailingDigitsState",
        DIGIT
    );

    // http://www.w3.org/TR/css3-values/
    final LexerState unitsState = new UnitsState();

    private Map<LexerState, LexerState[]> createStateMap() {

        Map<LexerState, LexerState[]> map =
                new HashMap<LexerState, LexerState[]>();

        // initState -- [#] --> hashState
        // initState -- [-] --> minusState
        // initState -- [+] --> plusState
        // initState -- [_a-z] --> nmStartState
        // initState -- [0-9] --> leadingDigitsState
        // initState -- [.] --> dotState
        map.put(
                initState,
                new LexerState[] {
                    hashState,
                    minusState,
                    nmStartState,
                    plusState,
                    minusState,
                    leadingDigitsState,
                    dotState
                }
        );

        // minus could be the start of an ident or a number
        // minusState -- [_a-z] --> nmStartState
        // minusState -- [0-9] --> leadingDigitsState
        // minusState -- [.] --> decimalMarkState
        map.put(
                minusState,
                new LexerState[] {
                    nmStartState,
                    leadingDigitsState,
                    decimalMarkState,
                }
        );

        //
        // # {name}
        // hash {nmchar}+
        // hashState -- [_a-z0-9-] --> nmCharState
        // nmCharState -- [_a-z0-9-] --> nmCharState
        //
        map.put(
                hashState,
                new LexerState[] {
                    hashNameCharState
                }
        );

        map.put(
                hashNameCharState,
                new LexerState[] {
                    hashNameCharState,
                }
        );


        //
        // {ident}
        // ident '-'? {nmchar}+
        // nmStartState -- [_a-z0-9-] --> nmCharState
        // nmCharState -- [_a-z0-9-] --> nmCharState
        // nmCharState -- [(] --> lparenState
        //
        map.put(
                nmStartState,
                new LexerState[] {
                    nmCharState
                }
        );

        map.put(
                nmCharState,
                new LexerState[] {
                    nmCharState,
                    lparenState
                }
        );

        // from +/- state, next state must be a digit or a dot
        map.put(
                plusState,
                new LexerState[] {
                    leadingDigitsState,
                    decimalMarkState
                }
        );

        // from leadingDigitsState, next state must be
        // another digit, a decimal mark, or units
        map.put(
                leadingDigitsState,
                new LexerState[] {
                    leadingDigitsState,
                    decimalMarkState,
                    unitsState
                }
        );

        // from decimal mark, next state must be a digit.
        // Need to map both dotState and decimalMarkState
        // since dot might be the first character and would
        // not be seen as a decimal point.
        map.put(
                dotState,
                new LexerState[] {
                    trailingDigitsState
                }
        );

        map.put(
                decimalMarkState,
                new LexerState[] {
                    trailingDigitsState
                }
        );

        // from trailingDigitsState, next state must be another digit or units
        map.put(
                trailingDigitsState,
                new LexerState[] {
                    trailingDigitsState,
                    unitsState,
                }
        );

        // UnitsState stays in UnitsState
        map.put(
                unitsState,
                new LexerState[] {
                    unitsState
                }
        );

        return map;
    }

    CssLexer() {
        this.stateMap = createStateMap();
        this.text = new StringBuilder(64);
        this.currentState = initState;
    }

    void setReader(Reader reader) {
        this.reader = reader;
        lastc = -1;
        pos = offset = 0;
        line = 1;
        this.currentState = initState;
        this.token = null;
        try {
            this.ch = readChar();
        } catch (IOException ioe) {
            token = Token.EOF_TOKEN;
        }
    }

    private Token scanImportant()  throws IOException{
        // CSS 2.1 grammar for important_sym
        // "!"({w}|{comment})*{I}{M}{P}{O}{R}{T}{A}{N}{T}
        final Recognizer[] important_sym =
                new Recognizer[] { I, M, P, O, R, T, A, N, T };
        int current = 0;

        text.append((char)ch);

        // get past the '!'
        ch = readChar();

        while(true) {

            switch (ch) {

                case Token.EOF:
                    token = Token.EOF_TOKEN;
                    return token;

                case '/':
                    ch = readChar();
                    if (ch == '*') skipComment();
                    else if (ch == '/') skipEOL();
                    else {
                        text.append('/').append((char)ch);
                        int temp = offset;
                        offset = pos;
                        return new Token(Token.INVALID, text.toString(), line, temp);
                    }
                    break;

                case ' ':
                case '\t':
                case '\r':
                case '\n':
                case '\f':
                    ch = readChar();
                    break;

                default:
                    boolean accepted = true;
                    while(accepted && current < important_sym.length) {
                        accepted = important_sym[current++].recognize(ch);
                        text.append((char)ch);
                        ch = readChar();
                    }
                    if (accepted) {
                        final int temp = offset;
                        offset = pos-1; // will have read one char too many
                        return new Token(IMPORTANT_SYM, "!important", line, temp);
                    } else {
                        while (ch != ';' &&
                               ch != '}' &&
                               ch != Token.EOF) {
                            ch = readChar();
                        }
                        if (ch != Token.EOF) {
                            final int temp = offset;
                            offset = pos-1; // will have read one char too many
                            return new Token(Token.SKIP, text.toString(), line, temp);
                        } else {
                            return Token.EOF_TOKEN;
                        }
                    }
            }
        }
    }

    // http://www.ietf.org/rfc/rfc3986
    // http://www.w3.org/TR/2011/REC-CSS2-20110607/syndata.html#uri
    // http://www.w3.org/TR/css3-syntax/#consume-a-url-token
    private int consumeUrl() throws IOException {

        text.delete(0, text.length());

        // skip initial white space
        while (WS_CHARS.recognize(ch)) {
            ch = readChar();
        }

        if (ch == Token.EOF) {
            return Token.EOF;
        }

        if (ch == '\'' || ch == '"') {

            int endQuote = ch;

            ch = readChar();

            // consume the string
            while (ch != endQuote) {

                if (ch == Token.EOF) {
                    break;
                }

                // un-escaped newline is an error
                if (NL_CHARS.recognize(ch)) {
                    break;
                }

                // handle escaped char
                // Note: this block does not handle the algorithm for consuming hex-digits
                if (ch == '\\') {

                    ch = readChar();

                    if (NL_CHARS.recognize(ch)) {

                        // consume newline
                        while(NL_CHARS.recognize(ch)) {
                            ch = readChar();
                        }

                    } else if (ch != Token.EOF) {
                        // if EOF, do nothing
                        text.append((char)ch);
                        ch = readChar();
                    }

                    continue;
                }

                text.append((char)ch);
                ch = readChar();

            }

            if (ch == endQuote) {

                ch = readChar();
                while(WS_CHARS.recognize(ch)) {
                    ch = readChar();
                }

                // After consuming white-space, the char has to be rparen or EOF. Error otherwise.
                if (ch == ')') {
                    // consume the rparen
                    ch = readChar();
                    return URL;
                }

                if(ch == Token.EOF) {
                    return URL;
                }
            }

        } else {

            // TODO: a lot of repeat code from above
            text.append((char)ch);
            ch = readChar();

            while (true) {

                while (WS_CHARS.recognize(ch)) {
                    ch = readChar();
                }

                if (ch == ')') {
                    // consume the rparen
                    ch = readChar();
                    return URL;
                }

                if (ch == Token.EOF) {
                    return URL;
                }

                // handle escaped char
                // Note: this block does not handle the algorithm for consuming hex-digits
                if (ch == '\\') {

                    ch = readChar();

                    if (NL_CHARS.recognize(ch)) {

                        // consume newline
                        while(NL_CHARS.recognize(ch)) {
                            ch = readChar();
                        }

                    } else if (ch != Token.EOF) {
                        // if EOF, do nothing
                        text.append((char)ch);
                        ch = readChar();
                    }

                    continue;
                }

                if (ch == '\'' || ch == '"' || ch == '(') {
                    break;
                }

                text.append((char)ch);
                ch = readChar();

            }
        }

        // if we get to here, then the token is bad
        // consume up to rparen or eof
        while(true) {
            int lastCh = ch;
            if (ch == Token.EOF) {
                return Token.EOF;
            } else if (ch == ')' && lastCh != '\\') {
                ch = readChar();
                return Token.INVALID;
            }

            lastCh = ch;
            ch = readChar();
        }

    }

    private class UnitsState extends LexerState {

        private final Recognizer[][] units = {

            // TODO: all units from http://www.w3.org/TR/css3-values/
            // If units are added, getType and unitsMask must be updated!
            { C, M },
            { D, E, G },
            { E, M },
            { E, X },
            { G, R, A, D },
            { I, N },
            { M, M },
            { M, S },
            { P, C },
            { P, T },
            { P, X },
            { R, A, D },
            { S },
            { T, U, R, N },
            { (c) -> c == '%'}
        };

        // One bit per unit
        private int unitsMask = 0x7FFF;

        // Offset into inner array of units
        private int index = -1;

        UnitsState() {
            super(-1, "UnitsState", null);
        }

        @Override
        public int getType() {

            int type = Token.INVALID;

            // Must keep this in sync with units array.
            // Small switch will be faster than Math.log(oldMask)/Math.log(2)
            switch (unitsMask) {
                case 0x1: type = CM; break;
                case 0x2: type = DEG; break;
                case 0x4: type = EMS; break;
                case 0x8: type = EXS; break;
                case 0x10: type = GRAD; break;
                case 0x20: type = IN; break;
                case 0x40: type = MM; break;
                case 0x80: type = MS; break;
                case 0x100: type = PC; break;
                case 0x200: type = PT; break;
                case 0x400: type = PX; break;
                case 0x800: type = RAD; break;
                case 0x1000: type = SECONDS; break;
                case 0x2000: type = TURN; break;
                case 0x4000: type = PERCENTAGE; break;
                default: type = Token.INVALID;
            }

            // reset
            unitsMask = 0x7fff;
            index = -1;

            return type;
        }

        @Override
        public boolean accepts(int c) {

            // Ensure that something bogus like '10xyzzy' is
            // consumed as a token by only returning false
            // if the char is not alpha or %
            if (!ALPHA.recognize(c) && c != '%') {
                return false;
            }

            // If unitsMask is zero, then we've already figured out that
            // this is an invalid token, but we want to accept c so that
            // '10xyzzy' is consumed as a token, albeit an invalid one.
            if (unitsMask == 0) return true;

            index += 1;

            for (int n=0 ; n < units.length; n++) {

                final int u = 1 << n;

                // the unit at this index already failed. Move on.
                if ((unitsMask & u) == 0) continue;

                if ((index >= units[n].length) || !(units[n][index].recognize(c))) {
                    // not a match, turn off this bit
                    unitsMask &= ~u;
                }

            }


            return true;
        }

    }

    private  void skipComment() throws IOException {
        while(ch != -1) {
            if (ch == '*') {
                ch = readChar();
                if (ch == '/') {
                    offset = pos;
                    ch=readChar();
                    break;
                }
            } else {
                ch = readChar();
            }
        }
    }

    private void skipEOL() throws IOException {

        int lastc = ch;

        while (ch != -1) {

            ch = readChar();

            // EOL is cr, lf, or crlf
            if ((ch == '\n') || (lastc == '\r' && ch != '\n')) {
                    break;
            }
        }

    }

    private int pos = 0;
    private int offset = 0;
    private int line = 1;
    private int lastc = -1;

    private int readChar() throws IOException {

        int c = reader.read();

        // only reset line and pos counters after having read a NL since
        // a NL token is created after the readChar
        if (lastc == '\n' || (lastc == '\r' && c != '\n')) {
            // set pos to 1 since we've already read the first char of the new line
            pos = 1;
            offset = 0;
            line++;
        } else {
            pos++;
        }

        lastc = c;
        return c;
    }

    Token nextToken() {

        Token tok = null;
        if (token != null) {
            tok = token;
            if (token.getType() != Token.EOF) token = null;
        } else {
            do {
                tok = getToken();
            } while (tok != null &&
//                     tok.getType() != Token.EOF &&
                     Token.SKIP_TOKEN.equals(tok));
        }

        // reset text buffer and currentState
        text.delete(0,text.length());
        currentState = initState;

        return tok;
    }

    private Token getToken() {

        try {
            while (true) {
                charNotConsumed = false;

                final LexerState[] reachableStates =
                        currentState != null ? stateMap.get(currentState) : null;

                final int max = reachableStates != null ? reachableStates.length : 0;

                LexerState newState = null;
                for (int n=0; n<max && newState == null; n++) {
                    final LexerState reachableState = reachableStates[n];
                    if (reachableState.accepts(ch)) {
                        newState = reachableState;
                    }
                }

                if (newState != null) {

                    // Some reachable state was reached. Keep going until
                    // the char isn't accepted by any state
                    currentState = newState;
                    text.append((char)ch);
                    ch = readChar();
                    continue;

                } else {

                    // If none of the reachable states accepts the char,
                    // then see if there is a token.

                    final int type = currentState != null ? currentState.getType() : Token.INVALID;

                    //
                    // If the token is INVALID and
                    // the currentState is something other than initState, then
                    // there is an error, so return INVALID.
                     //
                    if (type != Token.INVALID ||
                        !currentState.equals(initState)) {

                        final String str = text.toString();
                        Token tok = new Token(type, str, line, offset);
                        // because the next char has already been read,
                        // the next token starts at pos-1
                        offset = pos-1;

                        // return here, but the next char has already been read.
                        return tok;

                    }
                }

                // The char wasn't accepted and there was no previous token.
                switch (ch) {

                    case -1:
                        token = Token.EOF_TOKEN;
                        return token;

                    case '"':
                    case '\'':

                        text.append((char)ch);
                        final int endq = ch;
                        while((ch=readChar()) != -1) {
                            text.append((char)ch);
                            if (ch == endq) break;
                        }

                        if (ch != -1) {
                            token = new Token(STRING, text.toString(), line, offset);
                            offset = pos;
                        } else {
                            token = new Token(Token.INVALID, text.toString(), line, offset);
                            offset = pos;
                        }
                        break;

                    case '/':
                        ch = readChar();
                        if (ch == '*') {
                            skipComment();
                             if (ch != -1) {
                                continue;
                            } else {
                                token = Token.EOF_TOKEN;
                                return token;
                            }
                        } else if (ch == '/') {
                            skipEOL();
                            if (ch != -1) {
                                continue;
                            } else {
                                token = Token.EOF_TOKEN;
                                return token;
                            }
                        } else {
                            // not a comment - a SOLIDUS
                            token = new Token(SOLIDUS,"/", line, offset);
                            offset = pos;
                            charNotConsumed = true;
                        }
                        break;

                    case '>':

                        token = new Token(GREATER,">", line, offset);
                        offset = pos;
                        break;

                    case '{':
                        token = new Token(LBRACE,"{", line, offset);
                        offset = pos;
                        break;

                    case '}':
                        token = new Token(RBRACE,"}", line, offset);
                        offset = pos;
                        break;

                    case ';':
                        token = new Token(SEMI,";", line, offset);
                        offset = pos;
                        break;

                    case ':':
                        token = new Token(COLON,":", line, offset);
                        offset = pos;
                        break;

                    case '*':
                        token = new Token(STAR,"*", line, offset);
                        offset = pos;
                        break;

                    case '(':
                        token = new Token(LPAREN,"(", line, offset);
                        offset = pos;
                        break;

                    case ')':
                        token = new Token(RPAREN,")", line, offset);
                        offset = pos;
                        break;

                    case ',':
                        token = new Token(COMMA,",", line, offset);
                        offset = pos;
                        break;

                    case '.':
                        token = new Token(DOT,".", line, offset);
                        offset = pos;
                        break;

                    case ' ':
                    case '\t':
                    case '\f':
                        token = new Token(WS, Character.toString((char)ch), line, offset);
                        offset = pos;
                        break;


                    case '\r':
                        token = new Token(NL, "\\r", line, offset);
                        // offset and pos are reset on next readChar

                        ch = readChar();
                        if (ch == '\n') {
                            token = new Token(NL, "\\r\\n", line, offset);
                            // offset and pos are reset on next readChar
                        } else {
                            // already read the next character, so return
                            // return the NL token here (avoid the readChar
                            // at the end of the loop below)
                            final Token tok = token;
                            token = (ch == -1) ? Token.EOF_TOKEN : null;
                            return tok;
                        }
                        break;

                    case '\n':
                        token = new Token(NL, "\\n", line, offset);
                        // offset and pos are reset on next readChar
                        break;

                    case '!':
                        Token tok = scanImportant();
                        return tok;

                    case '@':
                        token = new Token(AT_KEYWORD, "@", line, offset);
                        offset = pos;
                        break;

                    default:
//                      System.err.println("hit default case: ch = " + Character.toString((char)ch));
                        token = new Token(Token.INVALID, Character.toString((char)ch), line, offset);
                        offset = pos;
                        break;
                }

                if (token == null) {
//                    System.err.println("token is null! ch = " + Character.toString((char)ch));
                    token = new Token(Token.INVALID, null, line, offset);
                    offset = pos;
                } else if (token.getType() == Token.EOF) {
                    return token;
                }

                if (ch != -1 && !charNotConsumed) ch = readChar();

                final Token tok = token;
                token = null;
                return tok;
            }
        } catch (IOException ioe) {
            token = Token.EOF_TOKEN;
            return token;
        }
    }

    private int ch;
    private boolean charNotConsumed = false;
    private Reader reader;
    private Token token;
    private final Map<LexerState, LexerState[]> stateMap;
    private LexerState currentState;
    private final StringBuilder text;

}
