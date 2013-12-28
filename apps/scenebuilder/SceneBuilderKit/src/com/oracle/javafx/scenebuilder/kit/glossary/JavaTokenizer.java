/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.glossary;

/**
 */
// Tokenize file f as follows. We read the entire text of the file into a string. We replace every occurrence of #
// in the string by its unicode equivalent. Then we construct a version of the file where every token is preceded
// by #, so for example
//     for (String s : strings) {
// would become
// #    #for# #(#String# #s# #:# #strings#)# #{
// The idea is that we can apply patterns to this tokenized version without fear that they will match inside
// strings or comments. Since a string or comment is a single token, and since we removed any # characters that
// might be inside strings or comments, we know that if we see for example #for#( then that is definitely an
// occurrence of "for(" that is not inside a string or a comment. //NOI18N
// The concept of a "token" here does not correspond exactly to Java's, both because sequences of whitespace are tokens //NOI18N
// and because the scanning is simplistic. We assume that the code is syntactically correct, and we also split some
// tokens more than Java does, for example #-#1 or #+#=. This extra splitting has no effect on the particular rules
// we will apply. We also do not scan keywords any differently from identifiers; since they are reserved words, if
// we see #for# then that is definitely the "for" keyword. //NOI18N
class JavaTokenizer {
    private JavaTokenizer() {
        assert false;
    }

    private static final String unicodeHash = "\\u" + String.format("%04x", (int) '#');//NOI18N

    public static String tokenize(String text) throws ParseException {
        if (text.contains(unicodeHash)) {
            throw new ParseException("Source file contains the sequence '" + unicodeHash + "'");//NOI18N
            // Why would we bother writing unicode when we can write #?
        }

        final String noHashText = text.replace("#", unicodeHash);//NOI18N
        final int len = noHashText.length();
        final StringBuilder tokens = new StringBuilder();
        int nexti;
        for (int i = 0; i < len; i = nexti) {
            char c = noHashText.charAt(i);
            switch (c) {
                case '/'://NOI18N
                    nexti = scanSlash(noHashText, i);
                    break;
                case '"'://NOI18N
                case '\''://NOI18N
                    try {
                        nexti = scanQuote(noHashText, i, c);
                    } catch (Error e) {
                        System.err.println(tokens.toString());
                        throw e;
                    }
                    break;
                case '\\'://NOI18N
                    throw new ParseException("Backslash outside quotes");//NOI18N
                    // It could be a unicode escape but why would we ever want that outside a string?
                default:
                    if (Character.isWhitespace(c)) {
                        nexti = scanSpace(noHashText, i);
                    } else if (c == '.') {
                        if (i + 1 >= len) {
                            throw new ParseException("Dot at end of file");//NOI18N
                        }
                        if (Character.isDigit(noHashText.charAt(i + 1))) {
                            nexti = scanNumber(noHashText, i + 1);
                        } else {
                            nexti = i + 1;
                        }
                    } else if (c == '.' || Character.isDigit(c)) {//NOI18N
                        nexti = scanNumber(noHashText, i);
                    } else if (Character.isJavaIdentifierStart(c)) {
                        nexti = scanIdentifier(noHashText, i);
                    } else {
                        nexti = i + 1;
                    }
            }
            tokens.append('#').append(noHashText.substring(i, nexti));//NOI18N
        }

        return tokens.toString();
    }

    private static int scanSpace(String s, int i) {
        int len = s.length();
        while (i < len && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int scanNumber(String s, int i) {
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == 'e' || c == 'E') {//NOI18N
                i++;  // skip possible sign
            }
            if (c == '.' || Character.isDigit(c) || Character.isLetter(c)) {//NOI18N
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    private static int scanIdentifier(String s, int i) {
        int len = s.length();
        while (i < len && Character.isJavaIdentifierPart(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int scanSlash(String s, int i) throws ParseException {
        int len = s.length();
        if (i + 1 >= len) {
            return i + 1;
        }
        switch (s.charAt(i + 1)) {
            case '/'://NOI18N
                int newline = s.indexOf('\n', i);//NOI18N
                if (newline < 0) {
                    throw new ParseException("Unterminated // comment");//NOI18N
                }
                return newline + 1;
            case '*'://NOI18N
                int starSlash = s.indexOf("*/", i + 2);//NOI18N
                if (starSlash < 0) {
                    throw new ParseException("Unterminated /* comment");//NOI18N
                }
                return starSlash + 2;
            default:
                return i + 1;
        }
    }

    private static int scanQuote(String s, int i, char quote) throws ParseException {
        assert s.charAt(i) == quote;
        int len = s.length();
        while (++i < len && s.charAt(i) != quote) {
            if (s.charAt(i) == '\\') {//NOI18N
                i++;
            }
        }
        if (i >= len) {
            System.err.println(s);
            throw new ParseException("Unterminated char or string constant");//NOI18N
        }
        return i + 1;
    }

    @SuppressWarnings("serial")//NOI18N
    public static class ParseException extends Exception {
        public ParseException(String msg) {
            super(msg);
        }
    }
}
