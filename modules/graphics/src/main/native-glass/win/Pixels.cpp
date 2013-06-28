/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_glass_ui_Pixels_Format.h"
#include "com_sun_glass_ui_win_WinPixels.h"

Bitmap::Bitmap(int width, int height)
{
    BYTE *mpixels = new BYTE[width * height];
    memset(mpixels, 0, width * height);
    Attach(::CreateBitmap(width, height, 1, 1, mpixels));
    delete[] mpixels;
    ASSERT((HBITMAP)*this);
}

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

Bitmap::Bitmap(Pixels & pixels)
{
    Attach(::CreateBitmap(pixels.GetWidth(), pixels.GetHeight(), 1, 32, pixels.GetBits()));
    ASSERT((HBITMAP)*this);
}

DIBitmap::DIBitmap(Pixels & pixels)
{
    int const width = pixels.GetWidth();
    int const height = pixels.GetHeight();
    void * const bits = pixels.GetBits();

    void *bitmapBits = NULL;
    jsize imageSize = width * height * 4;

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

HICON Pixels::CreateIcon(JNIEnv *env, jobject jPixels, BOOL fIcon, jint x, jint y)
{
    Pixels pixels(env, jPixels);

    Bitmap mask(pixels.GetWidth(), pixels.GetHeight());
    Bitmap bitmap(pixels);

    ICONINFO iconInfo;
    memset(&iconInfo, 0, sizeof(ICONINFO));
    iconInfo.hbmMask = mask;
    iconInfo.hbmColor = bitmap;
    iconInfo.fIcon = fIcon;
    iconInfo.xHotspot = x;
    iconInfo.yHotspot = y;
    HICON hIcon = ::CreateIconIndirect(&iconInfo);
    ASSERT(hIcon);

    ::GdiFlush();

    return hIcon;
}

Pixels::Pixels(JNIEnv *env, jobject jPixels)
{
    env->CallVoidMethod(jPixels, javaIDs.Pixels.attachData, ptr_to_jlong(this));
    CheckAndClearException(env);
}

void Pixels::AttachInt(JNIEnv *env, jint w, jint h, jobject buf, jintArray array, jint offset)
{
    width = w;
    height = h;
    ints.Attach(env, buf, array, offset);
}

void Pixels::AttachByte(JNIEnv *env, jint w, jint h, jobject buf, jbyteArray array, jint offset)
{
    width = w;
    height = h;
    bytes.Attach(env, buf, array, offset);
}

void * Pixels::GetBits()
{
    if (ints) {
        return ints.GetPtr();
    }
    if (bytes) {
        return bytes.GetPtr();
    }
    return NULL;
}

HANDLE BaseBitmap::GetGlobalDIB()
{
    HBITMAP hBitmap = (HBITMAP)*this;
    BITMAP bm;
    ::GetObject(hBitmap, sizeof(bm), &bm);

    jsize imageSize = bm.bmWidth*bm.bmHeight*4;

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

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinPixels
 * Method:    _initIDs
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinPixels__1initIDs
    (JNIEnv *env, jclass cls)
{
    javaIDs.Pixels.attachData = env->GetMethodID(cls, "attachData", "(J)V");
    ASSERT(javaIDs.Pixels.attachData);

    return com_sun_glass_ui_Pixels_Format_BYTE_BGRA_PRE;
}

/*
 * Class:     com_sun_glass_ui_win_WinPixels
 * Method:    _attachInt
 * Signature: (JIILjava/nio/IntBuffer;[II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinPixels__1attachInt
    (JNIEnv *env, jobject pixels, jlong ptr, jint w, jint h, jobject buf, jintArray array, jint offset)
{
    ((Pixels*)jlong_to_ptr(ptr))->AttachInt(env, w, h, buf, array, offset);
}

/*
 * Class:     com_sun_glass_ui_win_WinPixels
 * Method:    _attachByte
 * Signature: (JIILjava/nio/ByteBuffer;[BI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinPixels__1attachByte
    (JNIEnv *env, jobject pixels, jlong ptr, jint w, jint h, jobject buf, jbyteArray array, jint offset)
{
    ((Pixels*)jlong_to_ptr(ptr))->AttachByte(env, w, h, buf, array, offset);
}

/*
 * Class:     com_sun_glass_ui_win_WinPixels
 * Method:    _fillDirectByteBuffer
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinPixels__1fillDirectByteBuffer
    (JNIEnv *env, jobject jPixels, jobject bb)
{
    Pixels pixels(env, jPixels);

    memcpy(env->GetDirectBufferAddress(bb), pixels.GetBits(),
            pixels.GetWidth() * pixels.GetHeight() * 4);
}

} // extern "C"

