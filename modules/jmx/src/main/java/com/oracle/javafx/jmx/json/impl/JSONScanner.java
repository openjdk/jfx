/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.javafx.jmx.json.impl;

import com.oracle.javafx.jmx.json.JSONException;
import java.io.IOException;
import java.io.Reader;

/**
 * JSON scanner. Converts character data into stream of JSON symbols providing
 * their location in document and the respective string values.
 */
final class JSONScanner {

    private Reader reader;
    private String value;
    private int prevChar;
    private int x,  y = 1;

    @SuppressWarnings("unused")
    private int checkpointX;
    @SuppressWarnings("unused")
    private int checkpointY;
    private long offset;

    private boolean isInt;

    JSONScanner(Reader reader) throws IOException {
        this.reader = reader;
        offset = 0;
        prevChar = reader.read();
    }

    JSONSymbol nextSymbol() throws JSONException, IOException {
        int ch = prevChar;
        JSONSymbol s;

    /*
     * space character ' ' (0x20),
     * tab character (hex 0x09),
     * form feed character (hex 0x0c),
     * line separators characters newline (hex 0x0a)
     * carriage return (hex 0x0d)
     */

        // strip witespace
        while (ch != -1 && (ch == 0x20 ||
               ch == 0x09 || ch == 0x0c ||
               ch == 0x0a || ch == 0x0d)) {
            ch = readChar(true);
        }

        value = null;
        switch (ch) {
            case -1:
                s = JSONSymbol.EOS;
                prevChar = -1;
                break;
            case '{':
                s = JSONSymbol.CURLYOPEN;
                prevChar = readChar(true);
                break;
            case '}':
                s = JSONSymbol.CURLYCLOSE;
                prevChar = readChar(true);
                break;
            case '[':
                s = JSONSymbol.SQUAREOPEN;
                prevChar = readChar(true);
                break;
            case ']':
                s = JSONSymbol.SQUARECLOSE;
                prevChar = readChar(true);
                break;
            case ':':
                s = JSONSymbol.COLON;
                prevChar = readChar(true);
                break;
            case ',':
                s = JSONSymbol.COMMA;
                prevChar = readChar(true);
                break;
            case 't':
                readKeyword("true");
                s = JSONSymbol.KEYWORD;
                break;
            case 'f':
                readKeyword("false");
                s = JSONSymbol.KEYWORD;
                break;
            case 'n':
                readKeyword("null");
                s = JSONSymbol.KEYWORD;
                break;
            case '"':
                value = readString();
                s = JSONSymbol.STRING;
                break;
            case '-':
                value = readNumber(ch);
                s = JSONSymbol.NUMBER;
                break;
            default:
                if (ch >= '0' && ch <= '9') {
                    value = readNumber(ch);
                    s = JSONSymbol.NUMBER;
                } else {
                    Object[] args = {(char) ch};
                    throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
                }
        }

        return s;
    }

    String getValue() {
        return value;
    }

    private void readKeyword(final String text) throws JSONException, IOException {
        int i = 1;
        int ch = 0; /* to satisfy compiler. not needed actually */

        for (; i < text.length() && (ch = readChar(false)) == text.charAt(i); i++);

        if (i < text.length()) {
            Object[] args = {(char) ch};
            throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
        }

        value = text;

        prevChar = readChar(true);
    }

    private String readString() throws JSONException, IOException {
        StringBuilder val = new StringBuilder();
        int ch;

        do {
            ch = readChar(false);
            switch (ch) {
                case -1:
                    throw new JSONException(JSONMessages.localize(null, "unexpected_end_of_stream"), line(), column());
                case '\\':
                     {
                        int ch2 = readChar(false);
                        switch (ch2) {
                            case '"':
                            case '\\':
                            case '/':
                                val.append((char) ch2);
                                break;
                            case 'b':
                                val.append('\b');
                                break;
                            case 'f':
                                val.append('\f');
                                break;
                            case 'n':
                                val.append('\n');
                                break;
                            case 'r':
                                val.append('\r');
                                break;
                            case 't':
                                val.append('\t');
                                break;
                            case 'u': {
                                char unicode = 0;
                                for (int i = 4; --i >= 0;) {
                                    ch2 = readChar(false);
                                    unicode <<= 4;
                                    if (ch2 >= '0' && ch2 <= '9') {
                                        unicode |= ((char) ch2) - '0';
                                    } else if (ch2 >= 'a' && ch2 <= 'f') {
                                        unicode |= (((char) ch2) - 'a') + 0xA;
                                    } else if (ch2 >= 'A' && ch2 <= 'F') {
                                        unicode |= (((char) ch2) - 'A') + 0xA;
                                    } else {
                                        Object[] args = {(char) ch2};
                                        throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
                                    }
                                }
                                val.append((char) (unicode & 0xffff));
                                break;
                            }
                            default:
                                Object[] args = {(char) ch2};
                                throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
                        }
                    }
                    break;
                case '"':
                    break;
                default:
                    if ((ch >= 0x0000 && ch <= 0x001F) ||
                        (ch >= 0x007F && ch <= 0x009F)) {
                        throw new JSONException(JSONMessages.localize(null, "control_character_in_string"), line(), column());
                    }
                    val.append((char) ch);
            }
        } while (ch != '"');

        prevChar = readChar(true);

        return val.toString();
    }

    private String readNumber(final int prefetch) throws JSONException, IOException {
        int ch = prefetch;
        StringBuilder val = new StringBuilder();

        val.append((char) ch);

        // sign
        if (ch == '-') {
            ch = readChar(true);
            val.append((char) ch);
            if (ch < '0' || ch > '9') {
                Object[] args = {(char) ch};
                throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
            }
        }

        isInt = true;
        // int
        if (ch == '0') {
            ch = readChar(true);
            val.append((char) ch);
        } else {
            do {
                ch = readChar(true);
                val.append((char) ch);
            } while (ch >= '0' && ch <= '9');
        }

        // frac
        if (ch == '.') {
            isInt = false;
            int count = 0;
            do {
                ch = readChar(true);
                val.append((char) ch);
                count++;
            } while (ch >= '0' && ch <= '9');
            if (count == 1) {
                Object[] args = {(char) ch};
                throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
            }
        }

        // exp
        if (ch == 'e' || ch == 'E') {
            isInt = false;
            ch = readChar(false);
            val.append((char) ch);
            if (ch == '+' || ch == '-') {
                ch = readChar(false);
                val.append((char) ch);
            }
            int count;
            for (count = 0; ch >= '0' && ch <= '9'; count++) {
                ch = readChar(true);
                val.append((char) ch);
            }
            if (count == 0) {
                Object[] args = {(char) ch};
                throw new JSONException(JSONMessages.localize(args, "unexpected_char"), line(), column());
            }
        }

        prevChar = ch;

        return val.toString().substring(0, val.length() - 1);
    }

    private int readChar(boolean checkpoint) throws IOException {
        int ch = reader.read();

        if (checkpoint) {
            checkpointX = x;
            checkpointY = y;
        }

        if (ch == '\n') {
            y++;
            x = 0;
        } else {
            x++;
        }
        offset++;

        return ch;
    }

    boolean isInteger() {
        return isInt;
    }

    void close() throws IOException {
        reader.close();
    }

    int line() {
        return y;
    }

    int column() {
        return x;
    }

    long getCharacterOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return y + ":" + x +
            (offset >= 0 ? "@" + offset:"");
    }
}
