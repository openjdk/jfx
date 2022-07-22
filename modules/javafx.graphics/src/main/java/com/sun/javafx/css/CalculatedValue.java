/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css;

import javafx.css.StyleOrigin;

public final class CalculatedValue {

    public static final CalculatedValue SKIP = new CalculatedValue(new int[0], null, false);


    public CalculatedValue(Object value, StyleOrigin origin, boolean relative) {

        this.value = value;
        this.origin = origin;
        this.relative = relative;
    }

    public Object getValue() {
        return value;
    }

    public StyleOrigin getOrigin() {
        return origin;
    }

    public boolean isRelative() {
        return relative;
    }

    @Override public String toString() {
        return
            (new StringBuilder()
                .append('{')
                .append(String.valueOf(value))
                .append(", ").append(origin)
                .append(", ").append(relative)
                .append('}')
            ).toString();
    }

    @Override public boolean equals(Object obj) {

        if(obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        CalculatedValue other = (CalculatedValue)obj;

        if (this.relative != other.relative)  {
            return false;
        }

        if (this.origin != other.origin) {
            return false;
        }

        if (this.value == null ? other.value != null : !this.value.equals(other.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int h = CalculatedValue.class.hashCode();
        h = 31 * h + Boolean.hashCode(relative);
        h = 31 * h + (origin == null ? 0 : origin.hashCode());
        h = 31 * h + (value == null ? 0 : value.hashCode());
        return h;
    }

    private final Object value;
    private final StyleOrigin origin;
    private final boolean relative;
}
