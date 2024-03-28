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
package com.oracle.tools.fx.monkey.util;

import java.util.Objects;

/**
 * Named value.
 * @param <V> the type of the value
 */
public class NamedValue<V> {
    private final String display;
    private final V value;

    /**
     * Constructor.
     * @param display the display name
     * @param value the value
     */
    public NamedValue(String display, V value) {
        Objects.nonNull(display);
        this.display = display;
        this.value = value;
    }

    @Override
    public String toString() {
        return display;
    }

    public String getDisplay() {
        return display;
    }

    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int h = NamedValue.class.hashCode();
        h = 31 * h + display.hashCode();
        h = 31 * h + (value != null ? value.hashCode() : 0);
        return h;
    }

     @Override
     public boolean equals(Object x) {
         if(x == this) {
             return true;
         } else if(x instanceof NamedValue v) {
             return
                 Objects.equals(display, v.display) &&
                 Objects.equals(value, v.value);
         }
         return false;
     }
}
