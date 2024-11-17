/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.settings;

import java.util.ArrayList;

/**
 * Represents a string property as a stream of objects.
 */
public abstract class SStream {

    public abstract Object[] toArray();

    private SStream() {
    }

    public static SStream writer() {
        return new SStream() {
            private ArrayList<Object> items = new ArrayList<>();

            @Override
            protected void addValue(Object x) {
                items.add(x);
            }

            @Override
            public Object[] toArray() {
                return items.toArray();
            }
        };
    }

    public static SStream reader(Object[] items) {
        return new SStream() {
            int index;

            @Override
            protected Object nextValue() {
                if (index >= items.length) {
                    return null;
                }
                return items[index++];
            }

            @Override
            public Object[] toArray() {
                return items;
            }
        };
    }

    public void add(int x) {
        addValue(x);
    }

    public void add(double x) {
        addValue(x);
    }

    public void add(String x) {
        addValue(x);
    }

    protected void addValue(Object x) {
        throw new UnsupportedOperationException();
    }

    protected Object nextValue() {
        throw new UnsupportedOperationException();
    }

    public final String nextString(String defaultValue) {
        Object v = nextValue();
        if (v instanceof String s) {
            return s;
        }
        return defaultValue;
    }

    public final double nextDouble(double defaultValue) {
        Object v = nextValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                // ignore
            }
        } else if (v instanceof Double d) {
            return d;
        }
        return defaultValue;
    }

    public final int nextInt(int defaultValue) {
        Object v = nextValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                // ignore
            }
        } else if (v instanceof Integer d) {
            return d;
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[");
        boolean sep = false;
        for (Object x: toArray()) {
            if (sep) {
                sb.append(",");
            } else {
                sep = true;
            }
            sb.append(x);
        }
        sb.append("]");
        return sb.toString();
    }
}
