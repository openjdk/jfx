/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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


public class LexerState {

    /**
     * Determine whether or not this state accepts the character. If the
     * character is accepted, then this state will be entered.
     * @param c the character to test
     * @return true if this state should be entered
     */
    public boolean accepts(int c) {
        final int nRecognizers = recognizers != null ? recognizers.length : 0;
        for(int n=0; n<nRecognizers; n++) {
            if (this.recognizers[n].recognize(c)) return true;
        }
        return false;
    }

    public int getType() {
        return type;
    }

    public LexerState(int type, String name, Recognizer recognizer, Recognizer... others) {
        assert(name != null);
        this.type = type;
        this.name = name;
        if (recognizer != null) {
            final int nRecognizers = 1 + (others != null ? others.length : 0);
            this.recognizers = new Recognizer[nRecognizers];
            this.recognizers[0] = recognizer;
            for(int n=1; n<recognizers.length; n++) {
                this.recognizers[n] = others[n-1];
            }
        } else {
            this.recognizers = null;
        }
    }

    public LexerState(String name, Recognizer recognizer, Recognizer... others) {
        this(Token.INVALID, name, recognizer, others);
    }

    private LexerState() {
        this(Token.INVALID, "invalid", null);
    }

    private final int type;
    private final String name;
    private final Recognizer[] recognizers;

    @Override public String toString() {
        return name;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        return (other instanceof LexerState) ?
                this.name.equals(((LexerState)other).name) : false;
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

}
