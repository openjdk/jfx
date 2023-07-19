/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

/**
 * Debug logging facility.
 */
public class D {
    public static void p(Object... args) {
        StringBuilder sb = new StringBuilder();
        caller(sb);
        boolean sep = false;
        for (Object a : args) {
            if (sep) {
                sb.append(' ');
            } else {
                sep = true;
            }
            sb.append(a);
        }
        out(sb);
    }

    public static void f(String fmt, Object... args) {
        StringBuilder sb = new StringBuilder();
        caller(sb);
        sb.append(String.format(fmt, args));
        out(sb);
    }

    private static void caller(StringBuilder sb) {
        StackTraceElement s = new Throwable().getStackTrace()[2];
        String c = s.getClassName();
        int ix = c.lastIndexOf('.');
        if (ix >= 0) {
            c = c.substring(ix + 1);
        }
        sb.append(c);
        sb.append('.');
        sb.append(s.getMethodName());
        sb.append(':');
        sb.append(s.getLineNumber());
        sb.append(' ');
    }

    private static void out(Object x) {
        boolean enabled = true;
        if (enabled) {
            System.out.println(x);
        }
    }
}
