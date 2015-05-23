/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "math.h"

#include "KeyTable.h"

#include "com_sun_glass_ui_Robot.h"
#include "com_sun_glass_ui_win_WinRobot.h"
#include "GlassScreen.h"


static BOOL KeyEvent(JNIEnv *env, int code, bool isPress) {
    UINT vkey, modifiers;

    JavaKeyToWindowsKey(code, vkey, modifiers);

    if (!vkey) {
        return FALSE;
    } else {
        UINT scancode = ::MapVirtualKey(vkey, 0);

        INPUT keyInput = {0};
        keyInput.type = INPUT_KEYBOARD;
        keyInput.ki.wVk = vkey;
        keyInput.ki.wScan = scancode;
        keyInput.ki.time = 0;
        keyInput.ki.dwExtraInfo = 0;
        keyInput.ki.dwFlags = isPress ?  0 : KEYEVENTF_KEYUP;
        if (IsExtendedKey(vkey)) {
            keyInput.ki.dwFlags |= KEYEVENTF_EXTENDEDKEY;
        }

        ::SendInput(1, &keyInput, sizeof(keyInput));

        return TRUE;
    }
}

inline static jint WinToJavaPixel(USHORT r, USHORT g, USHORT b)
{
    jint value =
            0xFF << 24 | // alpha channel is always turned all the way up
            r << 16 |
            g << 8  |
            b << 0;
    return value;
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _keyPress
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1keyPress
    (JNIEnv *env, jobject jrobot, jint code)
{
    KeyEvent(env, code, true);
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _keyRelease
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1keyRelease
    (JNIEnv *env, jobject jrobot, jint code)
{
    KeyEvent(env, code, false);
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _mouseMove
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1mouseMove
    (JNIEnv *env, jobject jrobot, jint x, jint y)
{
    int oldAccel[3], newAccel[3];
    INT_PTR oldSpeed, newSpeed;
    BOOL bResult;

    jfloat fx = (jfloat) x + 0.5f;
    jfloat fy = (jfloat) y + 0.5f;
    GlassScreen::FX2Win(&fx, &fy);
    x = (jint) fx;
    y = (jint) fy;

    // The following values set mouse ballistics to 1 mickey/pixel.
    newAccel[0] = 0;
    newAccel[1] = 0;
    newAccel[2] = 0;
    newSpeed = 10;

    // Save the Current Mouse Acceleration Constants
    bResult = ::SystemParametersInfo(SPI_GETMOUSE, 0, oldAccel, 0);
    bResult = ::SystemParametersInfo(SPI_GETMOUSESPEED, 0, &oldSpeed, 0);
    // Set the new Mouse Acceleration Constants (Disabled).
    bResult = ::SystemParametersInfo(SPI_SETMOUSE, 0, newAccel, SPIF_SENDCHANGE);
    bResult = ::SystemParametersInfo(SPI_SETMOUSESPEED, 0,
            (PVOID)newSpeed,
            SPIF_SENDCHANGE);

    POINT curPos;
    ::GetCursorPos(&curPos);
    x -= curPos.x;
    y -= curPos.y;

    ::mouse_event(MOUSEEVENTF_MOVE, x, y, 0, 0);
    // Move the cursor to the desired coordinates.

    // Restore the old Mouse Acceleration Constants.
    bResult = ::SystemParametersInfo(SPI_SETMOUSE,0, oldAccel, SPIF_SENDCHANGE);
    bResult = ::SystemParametersInfo(SPI_SETMOUSESPEED, 0, (PVOID)oldSpeed,
            SPIF_SENDCHANGE);
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _getMouseX
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinRobot__1getMouseX
    (JNIEnv *env, jobject jrobot)
{
    POINT curPos;
    ::GetCursorPos(&curPos);
    jfloat fx = (jfloat) curPos.x + 0.5f;
    jfloat fy = (jfloat) curPos.y + 0.5f;
    GlassScreen::Win2FX(&fx, &fy);
    return (jint) fx;
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _getMouseY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinRobot__1getMouseY
    (JNIEnv *env, jobject jrobot)
{
    POINT curPos;
    ::GetCursorPos(&curPos);
    jfloat fx = (jfloat) curPos.x + 0.5f;
    jfloat fy = (jfloat) curPos.y + 0.5f;
    GlassScreen::Win2FX(&fx, &fy);
    return (jint) fy;
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _mousePress
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1mousePress
    (JNIEnv *env, jobject jrobot, jint buttons)
{
    DWORD dwFlags = 0L;
    // According to MSDN: Software Driving Software
    // application should consider SM_SWAPBUTTON to correctly emulate user with
    // left handed mouse setup
    BOOL bSwap = ::GetSystemMetrics(SM_SWAPBUTTON);

    if (buttons & (1 << 0)) {
        dwFlags |= !bSwap ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_RIGHTDOWN;
    }
    if (buttons & (1 << 1)) {
        dwFlags |= !bSwap ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_LEFTDOWN;
    }
    if (buttons & (1 << 2)) {
        dwFlags |= MOUSEEVENTF_MIDDLEDOWN;
    }

    INPUT mouseInput = {0};
    mouseInput.type = INPUT_MOUSE;
    mouseInput.mi.time = 0;
    mouseInput.mi.dwFlags = dwFlags;

    // Support for extra buttons
    if (buttons & (1 << 3)) {
        mouseInput.mi.dwFlags |= MOUSEEVENTF_XDOWN;
        mouseInput.mi.mouseData = XBUTTON1;
    }
    if (buttons & (1 << 4)) {
        mouseInput.mi.dwFlags |= MOUSEEVENTF_XDOWN;
        mouseInput.mi.mouseData = XBUTTON2;
    }

    ::SendInput(1, &mouseInput, sizeof(mouseInput));
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _mouseRelease
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1mouseRelease
    (JNIEnv *env, jobject jrobot, jint buttons)
{
    DWORD dwFlags = 0L;
    // According to MSDN: Software Driving Software
    // application should consider SM_SWAPBUTTON to correctly emulate user with
    // left handed mouse setup
    BOOL bSwap = ::GetSystemMetrics(SM_SWAPBUTTON);

    if (buttons & (1 << 0)) {
        dwFlags |= !bSwap ? MOUSEEVENTF_LEFTUP : MOUSEEVENTF_RIGHTUP;
    }
    if (buttons & (1 << 1)) {
        dwFlags |= !bSwap ? MOUSEEVENTF_RIGHTUP : MOUSEEVENTF_LEFTUP;
    }
    if (buttons & (1 << 2)) {
        dwFlags |= MOUSEEVENTF_MIDDLEUP;
    }

    INPUT mouseInput = {0};
    mouseInput.type = INPUT_MOUSE;
    mouseInput.mi.time = 0;
    mouseInput.mi.dwFlags = dwFlags;

    // Support for extra buttons
    if (buttons & (1 << 3)) {
        mouseInput.mi.dwFlags |= MOUSEEVENTF_XUP;
        mouseInput.mi.mouseData = XBUTTON1;
    }
    if (buttons & (1 << 4)) {
        mouseInput.mi.dwFlags |= MOUSEEVENTF_XUP;
        mouseInput.mi.mouseData = XBUTTON2;
    }

    ::SendInput(1, &mouseInput, sizeof(mouseInput));
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _mouseWheel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1mouseWheel
    (JNIEnv *env, jobject jrobot, jint wheelAmt)
{
    ::mouse_event(MOUSEEVENTF_WHEEL, 0, 0, wheelAmt * -1 * WHEEL_DELTA, 0);
}

void GetScreenCapture(jint x, jint y, jint devw, jint devh,
                      jint *pixelData, jint retw, jint reth);

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _getPixelColor
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinRobot__1getPixelColor
    (JNIEnv *env, jobject jrobot, jint x, jint y)
{
    jfloat fx = (jfloat) x + 0.5f;
    jfloat fy = (jfloat) y + 0.5f;
    GlassScreen::FX2Win(&fx, &fy);
    jint dx = (jint) fx;
    jint dy = (jint) fy;

    jint val = 0;
    //NOTE: we don't use the ::GetPixel() on the screen DC because it's not capable of
    //      getting the correct colors when non-opaque windows are present
    GetScreenCapture(dx, dy, 1, 1, &val, 1, 1);
    return val;
}

/*
 * Class:     com_sun_glass_ui_win_WinRobot
 * Method:    _getScreenCapture
 * Signature: (IIII[I;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinRobot__1getScreenCapture
    (JNIEnv *env, jobject jrobot, jint x, jint y, jint width, jint height, jintArray pixelArray)
{
    jfloat fx = (jfloat) x;
    jfloat fy = (jfloat) y;
    GlassScreen::FX2Win(&fx, &fy);
    jint dx = (jint) ceil(fx - 0.5f);
    jint dy = (jint) ceil(fy - 0.5f);
    fx = (jfloat) (x + width);
    fy = (jfloat) (y + height);
    GlassScreen::FX2Win(&fx, &fy);
    jint dw = ((jint) ceil(fx - 0.5f)) - dx;
    jint dh = ((jint) ceil(fy - 0.5f)) - dy;

    int numPixels = width * height;
    int pixelDataSize = sizeof(jint) * numPixels;
    ASSERT(pixelDataSize > 0 && pixelDataSize % 4 == 0);

    jint * pixelData = (jint *)(new BYTE[pixelDataSize]);

    if (pixelData) {
        GetScreenCapture(dx, dy, dw, dh, pixelData, width, height);

        // copy pixels into Java array
        env->SetIntArrayRegion(pixelArray, 0, numPixels, pixelData);
        delete pixelData;
    }
}

void GetScreenCapture(jint x, jint y, jint devw, jint devh,
                      jint *pixelData, jint retw, jint reth)
{
    HDC hdcScreen = ::CreateDC(TEXT("DISPLAY"), NULL, NULL, NULL);
    HDC hdcMem = ::CreateCompatibleDC(hdcScreen);
    HBITMAP hbitmap;
    HBITMAP hOldBitmap;
    HPALETTE hOldPalette = NULL;

    // create an offscreen bitmap
    hbitmap = ::CreateCompatibleBitmap(hdcScreen, retw, reth);
    if (hbitmap == NULL) {
        //TODO: OOM might be better?
        //throw std::bad_alloc();
    }
    hOldBitmap = (HBITMAP)::SelectObject(hdcMem, hbitmap);

    /* TODO: check this out
    // REMIND: not multimon-friendly...
    int primaryIndex = AwtWin32GraphicsDevice::GetDefaultDeviceIndex();
    hOldPalette =
        AwtWin32GraphicsDevice::SelectPalette(hdcMem, primaryIndex);
    AwtWin32GraphicsDevice::RealizePalette(hdcMem, primaryIndex);
    */

    // copy screen image to offscreen bitmap
    // CAPTUREBLT flag is required to capture WS_EX_LAYERED windows' contents
    // correctly on Win2K/XP
    static const DWORD dwRop = SRCCOPY|CAPTUREBLT;
    if (retw == devw && reth == devh) {
        ::BitBlt(hdcMem, 0, 0, retw, reth, hdcScreen, x, y, dwRop);
    } else {
        ::StretchBlt(hdcMem, 0, 0, retw, reth, hdcScreen, x, y, devw, devh, dwRop);
    }

    static const int BITS_PER_PIXEL = 32;

    struct {
        BITMAPINFOHEADER bmiHeader;
        RGBQUAD          bmiColors[3];
    } BitmapInfo;

    // prepare BITMAPINFO for a 32-bit RGB bitmap
    ::memset(&BitmapInfo, 0, sizeof(BitmapInfo));
    BitmapInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    BitmapInfo.bmiHeader.biWidth = retw;
    BitmapInfo.bmiHeader.biHeight = -reth; // negative height means a top-down DIB
    BitmapInfo.bmiHeader.biPlanes = 1;
    BitmapInfo.bmiHeader.biBitCount = BITS_PER_PIXEL;
    BitmapInfo.bmiHeader.biCompression = BI_BITFIELDS;

    // Setup up color masks
    static const RGBQUAD redMask =   {0, 0, 0xFF, 0};
    static const RGBQUAD greenMask = {0, 0xFF, 0, 0};
    static const RGBQUAD blueMask =  {0xFF, 0, 0, 0};

    BitmapInfo.bmiColors[0] = redMask;
    BitmapInfo.bmiColors[1] = greenMask;
    BitmapInfo.bmiColors[2] = blueMask;

    // Get the bitmap data in device-independent, 32-bit packed pixel format
    ::GetDIBits(hdcMem, hbitmap, 0, reth, pixelData, (BITMAPINFO *)&BitmapInfo, DIB_RGB_COLORS);

    // convert Win32 pixel format (BGRX) to Java format (ARGB)
    ASSERT(sizeof(jint) == sizeof(RGBQUAD));
    jint numPixels = retw * reth;
    jint *pPixel = pixelData;
    for(int nPixel = 0; nPixel < numPixels; nPixel++) {
        RGBQUAD * prgbq = (RGBQUAD *) pPixel;
        *pPixel++ = WinToJavaPixel(prgbq->rgbRed, prgbq->rgbGreen, prgbq->rgbBlue);
    }

    // free all the GDI objects we made
    ::SelectObject(hdcMem, hOldBitmap);
    if (hOldPalette != NULL) {
        ::SelectPalette(hdcMem, hOldPalette, FALSE);
    }
    ::DeleteObject(hbitmap);
    ::DeleteDC(hdcMem);
    ::DeleteDC(hdcScreen);
}

}

