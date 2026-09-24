/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "common.h"

#include "Pixels.h"

Bitmap::Bitmap(int width, int height, void **data, HDC hdc)
{
    ASSERT(width && height && data);

    BITMAPV5HEADER bmi = {0};
    bmi.bV5Width = width;
    bmi.bV5Height = -height;
    bmi.bV5Planes = 1;
    bmi.bV5BitCount = 32;

    bmi.bV5Size = sizeof(BITMAPV5HEADER);
    bmi.bV5Compression = BI_BITFIELDS;
    bmi.bV5XPelsPerMeter = 72;
    bmi.bV5YPelsPerMeter = 72;
    bmi.bV5RedMask   = 0x00FF0000;
    bmi.bV5GreenMask = 0x0000FF00;
    bmi.bV5BlueMask  = 0x000000FF;
    bmi.bV5AlphaMask = 0xFF000000;

    Attach(::CreateDIBSection(
        hdc,
        (BITMAPINFO*)&bmi,
        DIB_RGB_COLORS,
        data,
        NULL,
        0));

    ASSERT((HBITMAP)*this);
}

DIBitmap::DIBitmap(int width, int height, const void* bits)
{
    void *bitmapBits = NULL;
    int32_t imageSize = width * height * 4;

    BITMAPINFOHEADER bmi = {0};
    bmi.biSize = sizeof(bmi);
    bmi.biWidth = width;
    bmi.biHeight = -height;
    bmi.biPlanes = 1;
    bmi.biBitCount = 32;
    bmi.biCompression = BI_RGB;
    bmi.biSizeImage = imageSize;

    HBITMAP hBitmap = ::CreateDIBSection(NULL, (BITMAPINFO *)&bmi, DIB_RGB_COLORS, &bitmapBits, NULL, 0);

    if (bitmapBits) {
        memcpy(bitmapBits, bits, imageSize);
        Attach(hBitmap);
    }
    ASSERT((HBITMAP)*this);
}

HANDLE BaseBitmap::GetGlobalDIB()
{
    HBITMAP hBitmap = (HBITMAP)*this;
    BITMAP bm;
    ::GetObject(hBitmap, sizeof(bm), &bm);

    int32_t imageSize = bm.bmWidth*bm.bmHeight*4;

    //BITMAPV5HEADER converts to ordinal BITMAPINFOHEADER
    //as in/out parameter of GetDIBits call.
    //Negative height is not supported by MS Wordpad. Sorry.
    BITMAPINFOHEADER bmi = {0};
    bmi.biSize = sizeof(bmi);
    bmi.biWidth = bm.bmWidth;
    bmi.biHeight = bm.bmHeight;
    bmi.biPlanes = 1;
    bmi.biBitCount = 32;
    bmi.biCompression = BI_RGB;
    bmi.biSizeImage = imageSize;

    HANDLE hDIB = ::GlobalAlloc(GHND, bmi.biSize + imageSize);
    if (hDIB) {
        bool success = false;
        HDC hDC = ::GetDC(NULL);
        if (hDC) {
            BITMAPINFOHEADER *pbi = (BITMAPINFOHEADER *)::GlobalLock(hDIB);
            if (pbi) {
                *pbi = bmi;
                success = ::GetDIBits(hDC, hBitmap,
                    0, bm.bmHeight,
                    (LPSTR)pbi + bmi.biSize,
                    (LPBITMAPINFO)pbi,
                    DIB_RGB_COLORS) != 0;
                ::GlobalUnlock(hDIB);
            }
            ::ReleaseDC(NULL, hDC);
        }
        if (!success) {
            ::GlobalFree(hDIB);
            hDIB = NULL;
        }
    }
    return hDIB;
}
