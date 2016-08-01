/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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




class EGLX11GLContext extends GLContext {

    private static native long nInitialize(long nativeDInfo, long nativePFInfo,
                                           boolean vSyncRequest);
    private static native long nGetNativeHandle(long nativeCtxInfo);
    private static native void nMakeCurrent(long nativeCtxInfo, long nativeDInfo);

    EGLX11GLContext(long nativeCtxInfo) {
        this.nativeCtxInfo = nativeCtxInfo;
    }

    EGLX11GLContext(GLDrawable drawable, GLPixelFormat pixelFormat,
                           boolean vSyncRequest) {

        // holds the list of attributes to be translated for native call
        int attrArr[] = new int[GLPixelFormat.Attributes.NUM_ITEMS];

        GLPixelFormat.Attributes attrs = pixelFormat.getAttributes();

        attrArr[GLPixelFormat.Attributes.RED_SIZE] = attrs.getRedSize();
        attrArr[GLPixelFormat.Attributes.GREEN_SIZE] = attrs.getGreenSize();
        attrArr[GLPixelFormat.Attributes.BLUE_SIZE] = attrs.getBlueSize();
        attrArr[GLPixelFormat.Attributes.ALPHA_SIZE] = attrs.getAlphaSize();
        attrArr[GLPixelFormat.Attributes.DEPTH_SIZE] = attrs.getDepthSize();
        attrArr[GLPixelFormat.Attributes.DOUBLEBUFFER] = attrs.isDoubleBuffer() ? 1 : 0;
        attrArr[GLPixelFormat.Attributes.ONSCREEN] = attrs.isOnScreen() ? 1 : 0;

        Thread.dumpStack();
        // return the context info object created on the default screen
        nativeCtxInfo = nInitialize(drawable.getNativeDrawableInfo(),
                                    pixelFormat.getNativePFInfo(), vSyncRequest);
    }

    @Override
    long getNativeHandle() {
        return nGetNativeHandle(nativeCtxInfo);
    }

    @Override
    void makeCurrent(GLDrawable drawable) {
        nMakeCurrent(nativeCtxInfo, drawable.getNativeDrawableInfo());
    }
}
