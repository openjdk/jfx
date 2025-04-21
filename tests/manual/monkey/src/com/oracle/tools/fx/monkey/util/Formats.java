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

import java.text.DecimalFormat;
import javafx.geometry.Insets;

/**
 * Various formatting methods.
 */
public class Formats {
    private static final DecimalFormat FORMAT_2DP = new DecimalFormat("#0.##");

    public static String formatDouble(Number value) {
        if (value == null) {
            return "null";
        }
        double v = value.doubleValue();
        if (v == Math.rint(v)) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(v);
    }

    public static String format2DP(double v) {
        return FORMAT_2DP.format(v);
    }

    public static String formatInsets(Insets v) {
        if(v == null) {
            return "null";
        }
        return "Insets {" +
            "T=" + format2DP(v.getTop()) +
            " R=" + format2DP(v.getRight()) +
            " B=" + format2DP(v.getBottom()) +
            " L=" + format2DP(v.getLeft()) +
            "}";
    }
}
