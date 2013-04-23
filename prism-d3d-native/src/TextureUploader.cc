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

#include "D3DPipelineManager.h"
#include "TextureUploader.h"

void TextureUpdater::transferBytes( BYTE const *pSrcPixels, int srcStride, BYTE *pDstPixels, int dstStride, int w, int h) {
    for (int i = 0; i < h; ++i) {
        memcpy(pDstPixels, pSrcPixels, w);
        pSrcPixels += srcStride;
        pDstPixels += dstStride;
    }
}

void TextureUpdater::transferA8toA8R8G8B8( BYTE const *pSrcPixels, int srcStride, DWORD *pDstPixels, int dstStride, int w, int h) {
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            // only need to set the alpha channel
            pDstPixels[x] = DWORD(pSrcPixels[x]) << 24;
        }
        pSrcPixels += srcStride;
        pDstPixels = PDWORD(PBYTE(pDstPixels) + dstStride);
    }
}

void TextureUpdater::transferRGBtoA8R8G8B8( BYTE const *pSrcPixels, int srcStride, DWORD *pDstPixels, int dstStride, int w, int h) {
    for (int y = 0; y < h; y++) {
        for (int dx = 0, sx = 0; dx < w; dx++, sx+=3) {
            BYTE r = pSrcPixels[sx+0];
            BYTE g = pSrcPixels[sx+1];
            BYTE b = pSrcPixels[sx+2];
            pDstPixels[dx] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
        pSrcPixels += srcStride;
        pDstPixels = PDWORD(PBYTE(pDstPixels) + dstStride);
    }
}

bool TextureUpdater::isConversionNeeded() const {
    switch (format) {
    case PFORMAT_INT_ARGB_PRE:
    case PFORMAT_BYTE_RGBA_PRE:
        return srcStride != 4*srcW ||
            (pDesc->Format != D3DFMT_A8R8G8B8 && pDesc->Format != D3DFMT_X8R8G8B8);
    case PFORMAT_FLOAT_XYZW:
        return srcStride != 16*srcW || pDesc->Format != D3DFMT_A32B32G32R32F;
    default:
        return true;
    }
}

void TextureUpdater::unimplementedError() {
    RlsTrace(NWT_TRACE_ERROR, "Texture transfer is not implemented\n");
}

/* returns number of bytes transferred */
int TextureUpdater::updateLockableTexture() {
    D3DLOCKED_RECT lockedRect;
    RECT r = { dstX, dstY, dstX+srcW, dstY+srcH }, *pR = &r;
    DWORD dwLockFlags = D3DLOCK_NOSYSLOCK;

    if (pDesc->Usage == D3DUSAGE_DYNAMIC && dstX == 0 && dstY == 0) {
        // it is safe to lock with discard because we don't care about the
        // contents of dynamic textures and dstx,dsty for this case is
        // always 0,0 because we are uploading into a tile texture
        dwLockFlags |= D3DLOCK_DISCARD;
        pR = NULL;
    }

    HRESULT res = pTexture->LockRect(0, &lockedRect, pR, dwLockFlags);

    if (FAILED(res)) {
        DebugPrintD3DError(res, "IDirect3DTexture9.lock failed");
        return 0;
    }

    int  numTransferBytes = 0;

    switch (format) {
        // either a MaskFill tile, or a grayscale glyph
    case PFORMAT_BYTE_ALPHA:
    case PFORMAT_BYTE_GRAY:
        switch (pDesc->Format) {
        case D3DFMT_A8:
        case D3DFMT_L8:
            numTransferBytes = srcW * srcH;
            transferBytes(data, srcStride, PBYTE(lockedRect.pBits), lockedRect.Pitch, srcW, srcH);
            break;

        case D3DFMT_A8R8G8B8:
            numTransferBytes = srcW * srcH * 4;
            transferA8toA8R8G8B8(data, srcStride, PDWORD(lockedRect.pBits), lockedRect.Pitch, srcW, srcH);
            break;
        default:
            unimplementedError();
        }
        break;

    case PFORMAT_BYTE_RGB:
        switch (pDesc->Format) {
        case D3DFMT_A8R8G8B8:
        case D3DFMT_X8R8G8B8:
            numTransferBytes = srcW * srcH * 4;
            transferRGBtoA8R8G8B8(data, srcStride, PDWORD(lockedRect.pBits), lockedRect.Pitch, srcW, srcH);
            break;
        default:
            unimplementedError();
        }
        break;

    case PFORMAT_INT_ARGB_PRE:
    case PFORMAT_BYTE_RGBA_PRE:
        switch (pDesc->Format) {
        case D3DFMT_A8R8G8B8:
        case D3DFMT_X8R8G8B8:
            numTransferBytes = srcW * srcH * 4;
            transferBytes(data, srcStride, PBYTE(lockedRect.pBits), lockedRect.Pitch, srcW * 4, srcH);
            break;
        default:
            unimplementedError();
        }
        break;

    case PFORMAT_FLOAT_XYZW:
        switch (pDesc->Format) {
        case D3DFMT_A32B32G32R32F:
            numTransferBytes = srcW * srcH * 16;
            transferBytes(data, srcStride, PBYTE(lockedRect.pBits), lockedRect.Pitch, srcW * 16, srcH);
            break;
        default:
            unimplementedError();
        }
        break;

    default:
        unimplementedError();
    }

    res = pTexture->UnlockRect(0);
    return numTransferBytes;
}

int TextureUpdater::updateD3D9ExTexture(IDirect3DDevice9 *pDev) {
    IDirect3DTexture9 * temp = 0;
    HANDLE pData = isConversionNeeded() ? 0 : data;
    HRESULT hr = pDev->CreateTexture(srcW, srcH, 1, 0, pDesc->Format, D3DPOOL_SYSTEMMEM, &temp, pData ? &pData : 0);
    int size = 0;

    if (SUCCEEDED(hr)) {
        // need to upload data into the system texture
        if (pData == 0) {
            TextureUpdater updater;
            updater.setTarget(temp, pDesc, 0, 0);
            updater.setSource(data, srcSize, format, 0, 0, srcW, srcH, srcStride);
            size = updater.updateLockableTexture();
        } else {
            size = getPixelSize(format) * srcW * srcH;
        }

        if (srcW == pDesc->Width && srcH == pDesc->Height && dstX==0 && dstY==0) {
            hr = pDev->UpdateTexture(temp, pTexture);
            if (FAILED(hr)) {
                RlsTraceLn1(NWT_TRACE_ERROR, "Failed to update texture: %08X", hr);
                size = 0;
            }
        } else {
            IDirect3DSurface9 *src = 0, *dst = 0;
            HRESULT hr1 = temp->GetSurfaceLevel(0, &src);
            HRESULT hr2 = pTexture->GetSurfaceLevel(0, &dst);
            if (SUCCEEDED(hr1) && SUCCEEDED(hr2)) {
                RECT sRect = { 0, 0, srcW, srcH };
                POINT dPos = { dstX, dstY };
                hr = pDev->UpdateSurface( src, &sRect, dst, &dPos);
                if (FAILED(hr)) {
                    RlsTraceLn1(NWT_TRACE_ERROR, "Failed to update surface: %08X", hr);
                    size = 0;
                }
            } else {
                RlsTraceLn1(NWT_TRACE_ERROR, "Failed to get surfaceLevel 0x%08X", (hr1 | hr2));
            }
            SafeRelease(src); SafeRelease(dst);
        }
    } else {
        RlsTraceLn1(NWT_TRACE_ERROR, "Failed to create system memory texture for update operation: %08X", hr);
        return 0;
    }

    SafeRelease(temp);

    return size;
}

