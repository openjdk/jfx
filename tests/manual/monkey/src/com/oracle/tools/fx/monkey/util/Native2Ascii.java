/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

/**
 * Native to ASCII Conversion Utility.
 */
public class Native2Ascii {
    public static String native2ascii(String text) {
        if (text == null) {
            return null;
        }

        int sz = text.length();
        StringBuilder sb = new StringBuilder(sz + 256);
        for (int i = 0; i < sz; i++) {
            char c = text.charAt(i);
            switch (c) {
            case ' ':
            case '\n':
                sb.append(c);
                break;
            default:
                if ((c > ' ') && (c < 0x7f)) {
                    sb.append(c);
                } else {
                    escape(sb, c);
                }
            }
        }
        return sb.toString();
    }

    public static void escape(StringBuilder sb, char c) {
        sb.append("\\u");
        sb.append(hex(c >> 12));
        sb.append(hex(c >> 8));
        sb.append(hex(c >> 4));
        sb.append(hex(c));
    }

    private static char hex(int n) {
        return "0123456789abcdef".charAt(n & 0x0f);
    }

    private static int toUnicode(String text, int ix) {
        if (text.length() < (ix + 5)) {
            return -1;
        }

        char c = text.charAt(ix++);
        switch (c) {
        case 'u':
        case 'U':
            break;
        default:
            return -1;
        }

        try {
            String s = text.substring(ix, ix + 4);
            int v = Integer.parseInt(s, 16);
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String ascii2native(String text) {
        if (text == null) {
            return null;
        }

        int sz = text.length();
        StringBuilder sb = new StringBuilder(sz);
        for (int i = 0; i < sz; i++) {
            char c = text.charAt(i);
            switch (c) {
            case '\\':
                int u = toUnicode(text, i + 1);
                if (u < 0) {
                    sb.append(c);
                } else {
                    sb.append((char)u);
                    i += 5;
                }
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }
}
