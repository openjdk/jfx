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

import java.util.List;

class GIFImageMetadata {

    // package scope
    static final String
        nativeMetadataFormatName = "javax_imageio_gif_image_1.0";

    static final String[] disposalMethodNames = {
        "none",
        "doNotDispose",
        "restoreToBackgroundColor",
        "restoreToPrevious",
        "undefinedDisposalMethod4",
        "undefinedDisposalMethod5",
        "undefinedDisposalMethod6",
        "undefinedDisposalMethod7"
    };

    // Fields from Image Descriptor
    int imageLeftPosition;
    int imageTopPosition;
    int imageWidth;
    int imageHeight;
    boolean interlaceFlag = false;
    boolean sortFlag = false;
    byte[] localColorTable = null;

    // Fields from Graphic Control Extension
    int disposalMethod = 0;
    boolean userInputFlag = false;
    boolean transparentColorFlag = false;
    int delayTime = 0;
    int transparentColorIndex = 0;

    // Fields from Plain Text Extension
    boolean hasPlainTextExtension = false;
    int textGridLeft;
    int textGridTop;
    int textGridWidth;
    int textGridHeight;
    int characterCellWidth;
    int characterCellHeight;
    int textForegroundColor;
    int textBackgroundColor;
    byte[] text;

    // Fields from ApplicationExtension
    // List of byte[]
    List<byte[]> applicationIDs = null; // new ArrayList();

    // List of byte[]
    List<byte[]> authenticationCodes = null; // new ArrayList();

    // List of byte[]
    List<byte[]> applicationData = null; // new ArrayList();

    // Fields from CommentExtension
    // List of byte[]
    List<byte[]> comments = null; // new ArrayList();

    GIFImageMetadata() {}
}
