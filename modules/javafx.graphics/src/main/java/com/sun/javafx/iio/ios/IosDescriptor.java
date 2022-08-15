/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.ios;

import com.sun.javafx.iio.common.ImageDescriptor;

/**
 * A description of images on iOS platform.
 */
public class IosDescriptor extends ImageDescriptor {
    private static final String formatName = "PNGorJPEGorBMP";

    private static final String[] extensions = { "bmp", "png", "jpg", "jpeg", "gif" };

    private static final Signature[] signatures = {

        new Signature((byte) 0xff, (byte) 0xD8), // JPEG

        new Signature((byte) 137, (byte) 80, (byte) 78, (byte) 71, // PNG
        (byte) 13, (byte) 10, (byte) 26, (byte) 10),

        new Signature((byte)0x42, (byte)0x4D), // BMP

        new Signature(new byte[] {'G', 'I', 'F', '8', '7', 'a'}), // GIF87
        new Signature(new byte[] {'G', 'I', 'F', '8', '9', 'a'})  // GIF89
    };

    private static final String[] mimeSubtypes = { "bmp", "png", "x-png", "jpeg", "gif"};

    private static ImageDescriptor theInstance = null;

    private IosDescriptor() {
        super(formatName, extensions, signatures, mimeSubtypes);
    }

    /**
     * Returns an instance of IosDescriptor
     *
     * @return an instance of IosDescriptor
     */
    public static synchronized ImageDescriptor getInstance() {
        if (theInstance == null) {
            theInstance = new IosDescriptor();
        }
        return theInstance;
    }
}
