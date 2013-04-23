/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.gif;

class GIFStreamMetadata {
    // package scope
//    static final String
//        nativeMetadataFormatName = "javax_imageio_gif_stream_1.0";

    static final String[] versionStrings = { "87a", "89a" };

    String version; // 87a or 89a
    int logicalScreenWidth;
    int logicalScreenHeight;
    int colorResolution; // 1 to 8
    int pixelAspectRatio;

    int backgroundColorIndex; // Valid if globalColorTable != null
    boolean sortFlag; // Valid if globalColorTable != null

    static final String[] colorTableSizes = {
        "2", "4", "8", "16", "32", "64", "128", "256"
    };

    // Set global color table flag in header to 0 if null, 1 otherwise
    byte[] globalColorTable = null;

    GIFStreamMetadata() {}
}
