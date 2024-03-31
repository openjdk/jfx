/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;


abstract class LinuxGLPixelFormat extends GLPixelFormat {

    private static native long nCreatePixelFormat(long nativeScreen, int[] attrArr);

    static int[] getAttributesArray(Attributes attrs) {
        // holds the list of attributes to be translated for native call
        int[] attrArr = new int[Attributes.NUM_ITEMS];

        attrArr[Attributes.RED_SIZE] = attrs.getRedSize();
        attrArr[Attributes.GREEN_SIZE] = attrs.getGreenSize();
        attrArr[Attributes.BLUE_SIZE] = attrs.getBlueSize();
        attrArr[Attributes.ALPHA_SIZE] = attrs.getAlphaSize();
        attrArr[Attributes.DEPTH_SIZE] = attrs.getDepthSize();
        attrArr[Attributes.DOUBLEBUFFER] = attrs.isDoubleBuffer() ? 1 : 0;
        attrArr[Attributes.ONSCREEN] = attrs.isOnScreen() ? 1 : 0;

        return attrArr;
    }

    LinuxGLPixelFormat(long nativeScreen, Attributes attrs) {
        super(nativeScreen, attrs);

        long nativePF = nCreatePixelFormat(nativeScreen, getAttributesArray(attrs));
        setNativePFInfo(nativePF);
    }
}
