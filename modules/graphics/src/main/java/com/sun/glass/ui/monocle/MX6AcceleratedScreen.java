/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

class MX6AcceleratedScreen extends AcceleratedScreen {

    private long fbGetDisplayByIndexHandle, fbCreateWindowHandle;
    private long cachedNativeDisplay = 0l;

    private native long _platformGetNativeWindow(long methodHandle,
                                                 long display);

    private native long _platformGetNativeDisplay(long methodHandle);

    MX6AcceleratedScreen(int[] attributes) throws GLException {
        super(attributes);
    }

    @Override
    protected long platformGetNativeWindow() {
        fbCreateWindowHandle = ls.dlsym(getEGLHandle(), "fbCreateWindow");
        if (fbCreateWindowHandle == 0l) {
            return -1l;
        } else {
            return _platformGetNativeWindow(fbCreateWindowHandle, cachedNativeDisplay);
        }
    }

    @Override
    protected long platformGetNativeDisplay() {
        fbGetDisplayByIndexHandle = ls.dlsym(getEGLHandle(), "fbGetDisplayByIndex");
        if (fbGetDisplayByIndexHandle == 0l) {
            return -1l;
        } else {
            cachedNativeDisplay = _platformGetNativeDisplay(fbGetDisplayByIndexHandle);
            return cachedNativeDisplay;
        }
    }
}
