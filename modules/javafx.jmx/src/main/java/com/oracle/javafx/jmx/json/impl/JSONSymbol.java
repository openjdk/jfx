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
import java.util.HashMap;
import java.util.Stack;


/**
 * JSON syntax. Collection of symbols linked with grammar processor. Grammar
 * tree trversal generate events fed into JSONParserHandler instance.
 */
enum JSONSymbol {

    X, O, O1, O3, OV, A, A1, A2, V, VA,
    CURLYOPEN, CURLYCLOSE, COLON, COMMA, SQUAREOPEN, SQUARECLOSE, KEYWORD, STRING, NUMBER, EOS,
    X_, O_, O1_, O3_, OV_, A_, A1_, A2_, V_, VA_;

    static final boolean DEBUG = false;

    static {
        final JSONSymbol[] epsilon = new JSONSymbol[0];
        X.transition(CURLYOPEN, new JSONSymbol[]{O});
        X.transition(SQUAREOPEN, new JSONSymbol[]{A});
        O.transition(CURLYOPEN, new JSONSymbol[]{CURLYOPEN, O1, CURLYCLOSE});
        O1.transition(CURLYCLOSE, epsilon);
        O1.transition(STRING, new JSONSymbol[]{OV, O3});
        O3.transition(CURLYCLOSE, epsilon);
        O3.transition(COMMA, new JSONSymbol[]{COMMA, OV, O3});
        OV.transition(STRING, new JSONSymbol[]{STRING, COLON, V});
        A.transition(SQUAREOPEN, new JSONSymbol[]{SQUAREOPEN, A1, SQUARECLOSE});
        A1.transition(CURLYOPEN, new JSONSymbol[]{VA, A2});
        A1.transition(SQUAREOPEN, new JSONSymbol[]{VA, A2});
        A1.transition(SQUARECLOSE, epsilon);
        A1.transition(KEYWORD, new JSONSymbol[]{VA, A2});
        A1.transition(STRING, new JSONSymbol[]{VA, A2});
        A1.transition(NUMBER, new JSONSymbol[]{VA, A2});
        A2.transition(COMMA, new JSONSymbol[]{COMMA, VA, A2});
        A2.transition(SQUARECLOSE, epsilon);
        VA.transition(CURLYOPEN, new JSONSymbol[]{V});
        VA.transition(SQUAREOPEN, new JSONSymbol[]{V});
        VA.transition(STRING, new JSONSymbol[]{V});
        VA.transition(NUMBER, new JSONSymbol[]{V});
        VA.transition(KEYWORD, new JSONSymbol[]{V});
        V.transition(CURLYOPEN, new JSONSymbol[]{O});
        V.transition(SQUAREOPEN, new JSONSymbol[]{A});
        V.transition(KEYWORD, new JSONSymbol[]{KEYWORD});
        V.transition(STRING, new JSONSymbol[]{STRING});
        V.transition(NUMBER, new JSONSymbol[]{NUMBER});
        X.marker(X_);
        O.marker(O_);
        O1.marker(O1_);
        O3.marker(O3_);
        OV.marker(OV_);
        A.marker(A_);
        A1.marker(A1_);
        A2.marker(A2_);
        VA.marker(VA_);
        V.marker(V_);
    }

    boolean isTerminal = true;
    boolean isMarker = false;
    HashMap<JSONSymbol, JSONSymbol[]> transitions;
    JSONSymbol markerSymbol;

    private void transition(JSONSymbol s, JSONSymbol[] sequence) {
        if (isTerminal) {
            isTerminal = false;
            transitions = new HashMap<JSONSymbol, JSONSymbol[]>();
        }
        transitions.put(s, sequence);
    }

    private void marker(JSONSymbol s) {
        this.markerSymbol = s;
        s.isMarker = true;
    }

    private static Stack<JSONSymbol> stack;
    private static JSONSymbol        terminal;
    private static JSONSymbol        current;
    private static JSONScanner       scanner;
    private static String            value;

    static void init(JSONScanner js) throws JSONException, IOException  {
        scanner = js;
        stack = new Stack<JSONSymbol>();

        stack.push(X);
        terminal = scanner.nextSymbol();
    }

    static JSONSymbol next() throws JSONException, IOException {
        current = stack.pop();
        if (DEBUG) {
            Object[] args = {current};
            System.out.println(JSONMessages.localize(args, "parser_current"));
        }

        if (current.isMarker) {
            if (current == X_) {
                // reached bottom of processing stack
                return current;
            }
        } else if (current.isTerminal) {
            if (current != terminal) {
                Object[] args = {current, terminal};
                throw new JSONException(JSONMessages.localize(args, "expected_but_found"), scanner.line(), scanner.column());
            }
            value = scanner.getValue();
            if (DEBUG) {
                Object[] args = {current, value};
                System.out.println(JSONMessages.localize(args, "parser_type"));
            }
            terminal = scanner.nextSymbol();
            if (DEBUG) {
                Object[] args = {terminal, scanner.line(), scanner.column()};
                System.out.println(JSONMessages.localize(args, "parser_next_terminal"));
            }
        } else {
            JSONSymbol[] target = current.transitions.get(terminal);
            if (target == null) {
                Object[] args = {terminal, current};
                throw new JSONException(JSONMessages.localize(args, "unexpected_terminal"), scanner.line(), scanner.column());
            }

            if (DEBUG) {
                Object[] args = {current.markerSymbol};
                System.out.print(JSONMessages.localize(args, "parser_target") + ", ");
            }
            stack.push(current.markerSymbol);

            for (int i = target.length; --i >= 0;) {
                final JSONSymbol s = target[i];

                if (DEBUG) {
                    System.out.print(s.toString() + (i > 0 ? ", " : "\n"));
                }

                stack.push(s);
            }
        }
        return current;
    }

    static String getValue() {
        return value;
    }
}
