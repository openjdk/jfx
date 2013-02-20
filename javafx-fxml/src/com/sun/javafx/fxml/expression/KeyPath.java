/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.expression;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.AbstractList;
import java.util.ArrayList;

/**
 * Class representing a key path, an immutable list of string keys.
 */
public class KeyPath extends AbstractList<String> {
    private ArrayList<String> elements;

    public KeyPath(ArrayList<String> elements) {
        if (elements == null) {
            throw new NullPointerException();
        }

        this.elements = elements;
    }

    @Override
    public String get(int index) {
        return elements.get(index);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0, n = elements.size(); i < n; i++) {
            String element = elements.get(i);

            if (Character.isDigit(element.charAt(0))) {
                stringBuilder.append("[");
                stringBuilder.append(element);
                stringBuilder.append("]");
            } else {
                if (i > 0) {
                    stringBuilder.append(".");
                }

                stringBuilder.append(element);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Parses a string value into a key path.
     *
     * @param value
     * The string value to parse.
     *
     * @return
     * The resulting key path.
     */
    public static KeyPath parse(String value) {
        KeyPath keyPath;
        try {
            PushbackReader reader = new PushbackReader(new StringReader(value));

            try {
                keyPath = parse(reader);
            } finally {
                reader.close();
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return keyPath;
    }

    /**
     * Parses character content from a reader into a key path. If the character
     * following the key path is not EOF, it is pushed back.
     *
     * @param reader
     * The reader containing the content to parse.
     *
     * @return
     * The resulting key path.
     */
    protected static KeyPath parse(PushbackReader reader) throws IOException {
        ArrayList<String> elements = new ArrayList<String>();

        int c = reader.read();

        while (c != -1 && (Character.isJavaIdentifierStart(c) || c == '[')) {
            StringBuilder keyBuilder = new StringBuilder();

            boolean bracketed = (c == '[');

            if (bracketed) {
                c = reader.read();
                boolean quoted = (c == '"' || c == '\'');

                char quote;
                if (quoted) {
                    quote = (char)c;
                    c = reader.read();
                } else {
                    quote = Character.UNASSIGNED;
                }

                while (c != -1
                    && bracketed) {
                    if (Character.isISOControl(c)) {
                        throw new IllegalArgumentException("Illegal identifier character.");
                    }

                    if (!quoted
                        && !Character.isDigit(c)) {
                        throw new IllegalArgumentException("Illegal character in index value.");
                    }

                    keyBuilder.append((char)c);
                    c = reader.read();

                    if (quoted) {
                        quoted = c != quote;

                        if (!quoted) {
                            c = reader.read();
                        }
                    }

                    bracketed = c != ']';
                }

                if (quoted) {
                    throw new IllegalArgumentException("Unterminated quoted identifier.");
                }

                if (bracketed) {
                    throw new IllegalArgumentException("Unterminated bracketed identifier.");
                }

                c = reader.read();
            } else {
                while(c != -1 && (c != '.' && c != '[' && Character.isJavaIdentifierPart(c))) {
                    keyBuilder.append((char)c);
                    c = reader.read();
                }
            }

            if (c == '.') {
                c = reader.read();

                if (c == -1) {
                    throw new IllegalArgumentException("Illegal terminator character.");
                }
            }

            if (keyBuilder.length() == 0) {
                throw new IllegalArgumentException("Missing identifier.");
            }

            elements.add(keyBuilder.toString());
        }

        if (elements.size() == 0) {
            throw new IllegalArgumentException("Invalid path.");
        }

        // Can't push back EOF; subsequent calls to read() should still return -1
        if (c != -1) {
            reader.unread(c);
        }

        return new KeyPath(elements);
    }
}
