/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

class IWICImagingFactory extends IUnknown {
    IWICImagingFactory(long ptr) {
        super(ptr);
    }

    /**
     * {@code CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER,
     * IID_IWICImagingFactory, &factory)}. The apartment must already be entered on this thread -
     * {@code DWFactory.getWICFactory} does that first, as the C did.
     */
    static IWICImagingFactory create() {
        long ptr = DWNative.wicCreateImagingFactory();
        return ptr != 0 ? new IWICImagingFactory(ptr) : null;
    }

    IWICBitmap CreateBitmap(int uiWidth, int uiHeight, int pixelFormat, int options) {
        long result = DWNative.createBitmap(ptr, uiWidth, uiHeight, pixelFormat, options);
        return result != 0 ? new IWICBitmap(result) : null;
    }
}
