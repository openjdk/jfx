/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey;

import javafx.beans.property.SimpleBooleanProperty;

/**
 * Various logs (write to stdout).
 */
public class Loggers {
    public static final Logger accessibility = new Logger("accessibility");

    private static String toJson(long time, String name, Object[] nameValuePairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        kv(sb, "time", time);
        sb.append(", ");
        kv(sb, "log", name);
        for(int i=0; i<nameValuePairs.length; ) {
            Object k = nameValuePairs[i++];
            String ks = escape(k.toString());
            Object v = nameValuePairs[i++];
            String vs = formatValue(v);
            sb.append(", ");
            kv(sb, ks, vs);
        }
        sb.append("}");
        return sb.toString();
    }

    private static void kv(StringBuilder sb, String name, Object value) {
        String v = formatValue(value);
        sb.append(name);
        sb.append(":");
        sb.append(v);
    }

    private static String formatValue(Object v) {
        if (v == null) {
            return "null";
        } else if(v instanceof Number) {
            return v.toString();
        } else {
            return escape(v.toString());
        }
    }

    private static String escape(String text) {
        int len = text.length();
        StringBuilder sb = new StringBuilder(len + 8);
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            String s;
            switch(c) {
            case '\b':
                s = "\\b";
                break;
            case '\t':
                s = "\\t";
                break;
            case '\r':
                s = "\\r";
                break;
            case '\n':
                s = "\\n";
                break;
            case '\f':
                s = "\\f";
                break;
            case '"':
                s = "\\\"";
                break;
            default:
                sb.append(c);
                continue;
            }

            sb.append(s);
        }
        sb.append('"');
        return sb.toString();
    }

    public static class Logger {
        private final String name;
        public final SimpleBooleanProperty enabled = new SimpleBooleanProperty();
        private volatile boolean isEnabled;

        public Logger(String name) {
            this.name = name;
            enabled.addListener((s, p, on) -> isEnabled = on);
        }

        public void log(Object... nameValuePairs) {
            if (isEnabled) {
                String json = toJson(System.currentTimeMillis(), name, nameValuePairs);
                System.out.println(json);
            }
        }
    }
}
