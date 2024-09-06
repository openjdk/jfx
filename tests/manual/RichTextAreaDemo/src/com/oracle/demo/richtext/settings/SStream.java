/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.settings;

import java.util.ArrayList;

/**
 * Represents a string property as a stream of objects.
 *
 * @author Andy Goryachev
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
