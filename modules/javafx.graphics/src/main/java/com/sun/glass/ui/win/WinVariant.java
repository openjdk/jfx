/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.win;

final class WinVariant {
    static final int VT_EMPTY = 0;
    static final int VT_NULL  = 1;
    static final int VT_I2    = 2;
    static final int VT_I4    = 3;
    static final int VT_R4    = 4;
    static final int VT_R8    = 5;
    static final int VT_BOOL  = 11;
    static final int VT_BSTR  = 8;
    static final int VT_ARRAY = 0x2000;
    static final int VT_UNKNOWN = 13;

    short vt;

    short iVal;
    int lVal;
    float fltVal;
    double dblVal;
    boolean boolVal;
    String bstrVal;

    double[] pDblVal;
    long punkVal;
}
