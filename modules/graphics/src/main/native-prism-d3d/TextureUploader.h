/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

// See com.sun.prism.PixelFormat enum

enum PFormat {
    PFORMAT_INT_ARGB_PRE  = 0,
    PFORMAT_BYTE_RGBA_PRE = 1,
    PFORMAT_BYTE_RGB      = 2,
    PFORMAT_BYTE_GRAY     = 3,
    PFORMAT_BYTE_ALPHA    = 4,
    PFORMAT_MULTI_YV_12   = 5, // used only at java level
    PFORMAT_BYTE_APPL_422 = 6, // unused in D3D
    PFORMAT_FLOAT_XYZW    = 7,
};

inline UINT getPixelSize(PFormat f) {
    switch (f) {
    case PFORMAT_BYTE_ALPHA:
    case PFORMAT_BYTE_GRAY:
        return 1;
    case PFORMAT_BYTE_RGB:
        return 3;
    case PFORMAT_INT_ARGB_PRE:
    case PFORMAT_BYTE_RGBA_PRE:
        return 4;
    case PFORMAT_FLOAT_XYZW:
        return 16;
    default:
        return 0;
    }
}


struct TextureUpdater {

    // source
    BYTE * data;
    PFormat format;
    UINT srcW, srcH, srcStride, srcSize;

    // destination
    IDirect3DTexture9 *pTexture;
    IDirect3DSurface9 *pSurface;
    D3DSURFACE_DESC *pDesc;
    UINT dstX, dstY;

    TextureUpdater() : pTexture(0), pSurface(0), data(0), pDesc(0) {}

    // return false if paramenters are incorrect
    // dstW and dstH are real texture size from its D3DSURFACE_DESC
    // unsigned types are used so that negative arguments will fail the tests
    static bool validateArguments(
        UINT dstX, UINT dstY, UINT dstW, UINT dstH,
        UINT srcX, UINT srcY, UINT srcW, UINT srcH,
        UINT srcSize, PFormat srcFormat, UINT srcStride)
    {
        UINT pixelSize = getPixelSize(srcFormat);
        bool dstOk = (dstX < dstW) && (dstY < dstH);
        bool srcOk =
            int(srcX) >= 0 && int(srcY) >= 0 && srcStride > 0 && pixelSize > 0 &&
            ((srcY+srcH) <= srcSize/srcStride) && ((srcX+srcW) <= srcStride/pixelSize) &&
            (srcW <= (dstW - dstX)) && (srcH <= (dstH - dstY));

        return dstOk && srcOk;
    }

    void setTarget(IDirect3DTexture9 *tex, IDirect3DSurface9 *surface, D3DSURFACE_DESC *desc, UINT x, UINT y) {
        pTexture = tex;
        pSurface = surface;
        pDesc = desc;
        dstX = x; dstY = y;
    }

    void setSource(BYTE *p, UINT size, PFormat f, UINT x, UINT y, UINT w, UINT h, UINT stride) {
        UINT pixelSize = getPixelSize(f);

        data = p + x * pixelSize + y * stride;
        format = f;
        srcW = w; srcH = h;
        srcStride = stride;
        srcSize = size;
    }

    /*
      it is required to call setSource and setTarget before to invoke any of update methods
      returns number of bytes transferred
    */
    int updateLockableTexture();
    int updateD3D9ExTexture(D3DContext *pCtx);

private:

    static void transferBytes( BYTE const *pSrcPixels, int srcStride, BYTE *pDstPixels, int dstStride, int w, int h);

    static void transferA8toA8R8G8B8( BYTE const *pSrcPixels, int srcStride, DWORD *pDstPixels, int dstStride, int w, int h);

    static void transferRGBtoA8R8G8B8( BYTE const *pSrcPixels, int srcStride, DWORD *pDstPixels, int dstStride, int w, int h);

    void unimplementedError();
};
