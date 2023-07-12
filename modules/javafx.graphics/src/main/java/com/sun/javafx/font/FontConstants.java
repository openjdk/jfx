/*
 * Copyright (c) 2012, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

public interface FontConstants {
    /*  useful tags */
    public static final int ttcfTag = 0x74746366; // 'ttcf' - TTC file
    public static final int v1ttTag = 0x00010000; // 'v1tt' - Version 1 TT font
    public static final int trueTag = 0x74727565; // 'true' - Version 2 TT font
    public static final int ottoTag = 0x4f54544f; // 'otto' - OpenType CFF font
    public static final int woffTag = 0x774F4646; // 'wOFF' - WOFF File Format
    public static final int cmapTag = 0x636D6170; // 'cmap'
    public static final int headTag = 0x68656164; // 'head'
    public static final int hheaTag = 0x68686561; // 'hhea'
    public static final int hmtxTag = 0x686D7478; // 'hmtx'
    public static final int maxpTag = 0x6D617870; // 'maxp'
    public static final int nameTag = 0x6E616D65; // 'name'
    public static final int os_2Tag = 0x4F532F32; // 'OS/2'
    public static final int postTag = 0x706F7374; // 'post'
    public static final int colrTag = 0x434F4C52; // 'COLR'
    public static final int sbixTag = 0x73626978; // 'sbix'

    /* sizes, in bytes, of TT/TTC header records */
    public static final int TTCHEADERSIZE = 12;
    public static final int DIRECTORYHEADERSIZE = 12;
    public static final int DIRECTORYENTRYSIZE = 16;

    /* WOFF headers recored */
    public static final int WOFFHEADERSIZE = 44;
    public static final int WOFFDIRECTORYENTRYSIZE = 20;
}
