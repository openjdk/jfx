/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class ErrorLoggingUtiltity {

    private PrintStream defaultStream = System.err;
    private ByteArrayOutputStream haystack;

    public void start() {
        haystack = new ByteArrayOutputStream();
        System.setErr(new PrintStream(haystack, true));
    }

    public void stop() {
        System.setErr(defaultStream);
        haystack = null;
    }
    
    public void reset() {
        if (haystack != null) {
            haystack.reset();
        }
    }

    public void check(Object... expected) {
        final int n = expected.length / 2;
        final String[] lines = (haystack == null)? new String[0] : haystack.toString().split("\n");
        for (int i=0; i<n; i++) {
            final int lineNumber = (Integer)expected[2*i];
            final String needle = (String)expected[2*i+1];
            assertTrue(String.format("Could not find '%s' in line %d: %s", needle, lineNumber, lines[lineNumber]), lines[lineNumber].contains(needle));
        }
        reset();
    }

    public boolean isEmpty() {
        return (haystack == null) || (haystack.size() == 0);
    }
}
