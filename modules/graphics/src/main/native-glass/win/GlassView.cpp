/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "FullScreenWindow.h"
#include "GlassApplication.h"
#include "GlassView.h"
#include "GlassWindow.h"
#include "Pixels.h"

#include "com_sun_glass_events_ViewEvent.h"
#include "com_sun_glass_ui_win_WinView.h"


// Helper LEAVE_MAIN_THREAD for GlassView
#define LEAVE_MAIN_THREAD_WITH_view  \
    GlassView * view;  \
    LEAVE_MAIN_THREAD;  \
    ARG(view) = (GlassView*)jlong_to_ptr(ptr);

GlassView::GlassView(jobject jrefThis) :
    m_fullScreenWindow(NULL),
    m_hostHwnd(NULL)
{
    m_grefThis = GetEnv()->NewGlobalRef(jrefThis);
}

GlassView::~GlassView()
{
    if (m_grefThis) {
        GetEnv()->DeleteGlobalRef(m_grefThis);
    }
}

BOOL GlassView::Close()
{
    if (m_fullScreenWindow) {
        FullScreenWindow * fs = dynamic_cast<FullScreenWindow*>(m_fullScreenWindow);
        if (fs) {
            fs->Close();
        }
        m_fullScreenWindow = NULL;
    }

    return JNI_TRUE;
}

BOOL GlassView::EnterFullScreen(BOOL animate, BOOL keepRatio)
{
    GlassWindow *pWindow = GlassWindow::FromHandle(GetHostHwnd());
    if (pWindow && !pWindow->IsChild()) {
        m_fullScreenWindow = pWindow;
    } else {
        // create new FullScreen window to handle "ownerless" views
        FullScreenWindow * w = new FullScreenWindow();
        w->Create();
        m_fullScreenWindow = w;
    }

    BOOL ret = m_fullScreenWindow->EnterFullScreenMode(this, animate, keepRatio);
    if (ret) {
        NotifyFullscreen(true);
    }

    return ret;
}

void GlassView::ExitFullScreen(BOOL animate)
{
    if (!m_fullScreenWindow) {
        return;
    }

    m_fullScreenWindow->ExitFullScreenMode(animate);
    m_fullScreenWindow = NULL;
    NotifyFullscreen(false);
}

void GlassView::NotifyFullscreen(bool entered)
{
    GetEnv()->CallVoidMethod(GetView(), javaIDs.View.notifyView,
            entered ? com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER :
            com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT);
}

void GlassView::SetHostHwnd(HWND m_hostHwnd)
{
    if (this->m_hostHwnd == m_hostHwnd) {
        return;
    }

    if (this->m_hostHwnd) {
        this->m_hostHwnd = NULL;

        GetEnv()->CallVoidMethod(GetView(), javaIDs.View.notifyView,
                com_sun_glass_events_ViewEvent_REMOVE);
        CheckAndClearException(GetEnv());
    }

    this->m_hostHwnd = m_hostHwnd;

    if (m_hostHwnd) {
        GetEnv()->CallVoidMethod(GetView(), javaIDs.View.notifyView,
                com_sun_glass_events_ViewEvent_ADD);
        CheckAndClearException(GetEnv());
    }
}

void GlassView::EnableInputMethodEvents(BOOL enable)
{
    m_InputMethodEventsEnabled = enable;
}

void GlassView::FinishInputMethodComposition()
{
    HWND hwnd = GetHostHwnd();
    if (hwnd)
    {
        HIMC hIMC = ::ImmGetContext(hwnd);
        if(hIMC)
        {
            ::ImmNotifyIME(hIMC, NI_COMPOSITIONSTR, CPS_COMPLETE, 0);
            ::ImmReleaseContext(hwnd, hIMC);
        }
    }
}

/*
 * JNI methods section
 *
 */

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1initIDs
    (JNIEnv *env, jclass cls)
{
    javaIDs.View.notifyResize = env->GetMethodID(cls, "notifyResize", "(II)V");
    ASSERT(javaIDs.View.notifyResize);
    if (env->ExceptionCheck()) return;
 
    javaIDs.View.notifyRepaint = env->GetMethodID(cls, "notifyRepaint", "(IIII)V");
    ASSERT(javaIDs.View.notifyRepaint);
    if (env->ExceptionCheck()) return;

     javaIDs.View.notifyKey = env->GetMethodID(cls, "notifyKey", "(II[CI)V");
     ASSERT(javaIDs.View.notifyKey);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyMouse = env->GetMethodID(cls, "notifyMouse", "(IIIIIIIZZ)V");
     ASSERT(javaIDs.View.notifyMouse);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyMenu = env->GetMethodID(cls, "notifyMenu", "(IIIIZ)V");
     ASSERT(javaIDs.View.notifyMenu);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyScroll = env->GetMethodID(cls, "notifyScroll", "(IIIIDDIIIIIDD)V");
     ASSERT(javaIDs.View.notifyScroll);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyInputMethod = env->GetMethodID(cls, "notifyInputMethod", "(Ljava/lang/String;[I[I[BIII)V");
     ASSERT(javaIDs.View.notifyInputMethod);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyDragEnter = env->GetMethodID(cls, "notifyDragEnter", "(IIIII)I");
     ASSERT(javaIDs.View.notifyDragEnter);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyDragOver = env->GetMethodID(cls, "notifyDragOver", "(IIIII)I");
     ASSERT(javaIDs.View.notifyDragOver);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyDragLeave = env->GetMethodID(cls, "notifyDragLeave", "()V");
     ASSERT(javaIDs.View.notifyDragLeave);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyDragDrop = env->GetMethodID(cls, "notifyDragDrop", "(IIIII)I");
     ASSERT(javaIDs.View.notifyDragDrop);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyView = env->GetMethodID(cls, "notifyView", "(I)V");
     ASSERT(javaIDs.View.notifyView);
     if (env->ExceptionCheck()) return;

     javaIDs.View.getWidth = env->GetMethodID(cls, "getWidth", "()I");
     ASSERT(javaIDs.View.getWidth);
     if (env->ExceptionCheck()) return;

     javaIDs.View.getHeight = env->GetMethodID(cls, "getHeight", "()I");
     ASSERT(javaIDs.View.getHeight);
     if (env->ExceptionCheck()) return;

     javaIDs.View.getAccessible = env->GetMethodID(cls, "getAccessible", "()J");
     ASSERT(javaIDs.View.getAccessible);
     if (env->ExceptionCheck()) return;

     javaIDs.View.notifyInputMethodCandidatePosRequest = env->GetMethodID(cls, "notifyInputMethodCandidatePosRequest", "(I)[D");
     ASSERT(javaIDs.View.notifyInputMethodCandidatePosRequest);
     if (env->ExceptionCheck()) return;

     javaIDs.View.ptr = env->GetFieldID(cls, "ptr", "J");
     ASSERT(javaIDs.View.ptr);
     if (env->ExceptionCheck()) return;
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _getMultiClickTime_impl
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinView__1getMultiClickTime_1impl
    (JNIEnv *env, jclass cls)
{
    return (jlong)::GetDoubleClickTime();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _getMultiClickMaxX_impl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinView__1getMultiClickMaxX_1impl
    (JNIEnv *env, jclass cls)
{
    return (jint)::GetSystemMetrics(SM_CXDOUBLECLK);
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _getMultiClickMaxY_impl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinView__1getMultiClickMaxY_1impl
    (JNIEnv *env, jclass cls)
{
    return (jint)::GetSystemMetrics(SM_CYDOUBLECLK);
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _create
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinView__1create
    (JNIEnv *env, jobject jview, jobject caps)
{
    return (jlong)new GlassView(jview);
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinView__1close
    (JNIEnv *env, jobject jview, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        jboolean result = bool_to_jbool(view->Close());
        delete view;
        return result;
    }
    LEAVE_MAIN_THREAD_WITH_view;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _getNativeView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinView__1getNativeView
  (JNIEnv *env, jobject _this, jlong viewPtr)
{
    GlassView * view = (GlassView *)viewPtr;

    return (jlong)view->GetHostHwnd();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _setParent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1setParent
  (JNIEnv *env, jobject _this, jlong ptr, jlong parentPtr)
{
    ENTER_MAIN_THREAD()
    {
        // The action may send ADD/REMOVE events. Let them be on the main thread
        view->SetHostHwnd((HWND)parentPtr);
    }
    jlong parentPtr;
    LEAVE_MAIN_THREAD_WITH_view;

    ARG(parentPtr) = parentPtr;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _getX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinView__1getX
  (JNIEnv *env, jobject _this, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jint)
    {
        HWND hWnd = view->GetHostHwnd();

        if (!hWnd) {
            return 0;
        }

        RECT rect1, rect2;

        ::GetWindowRect(hWnd, &rect1);
        ::GetClientRect(hWnd, &rect2);
        ::MapWindowPoints(hWnd, (HWND)NULL, (LPPOINT)&rect2, (sizeof(RECT)/sizeof(POINT)));

        return rect2.left - rect1.left;
    }
    LEAVE_MAIN_THREAD_WITH_view;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _getY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinView__1getY
  (JNIEnv *env, jobject _this, jlong ptr)
{
    ENTER_MAIN_THREAD_AND_RETURN(jint)
    {
        HWND hWnd = view->GetHostHwnd();

        if (!hWnd) {
            return 0;
        }

        RECT rect1, rect2;
        POINT p = {0, 0};

        ::GetWindowRect(hWnd, &rect1);
        ::GetClientRect(hWnd, &rect2);
        ::MapWindowPoints(hWnd, (HWND)NULL, (LPPOINT)&rect2, (sizeof(RECT)/sizeof(POINT)));

        return rect2.top - rect1.top;
    }
    LEAVE_MAIN_THREAD_WITH_view;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _beginPaint
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1begin
  (JNIEnv *env, jobject jview, jlong ptr)
{
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _endPaint
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1end
  (JNIEnv *env, jobject jview, jlong ptr)
{
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _uploadPixels
 * Signature: (JLcom/sun/glass/ui/Pixels;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1uploadPixels
    (JNIEnv *env, jobject jThis, jlong ptr, jobject jPixels)
{
    ENTER_MAIN_THREAD()
    {
        HWND hWnd = view->GetHostHwnd();
        if (!::IsWindow(hWnd)) {
            //NOTE: uploadPixels() may be invoked from a thread other than the
            //      toolkit thread. Therefore, when SendMessage() is finally
            //      processed, the hWnd may be stale already, and hence
            //      the pWindow == NULL shouldn't be surprising.
            return;
        }

        GlassWindow *pWindow = GlassWindow::FromHandle(hWnd);
        Pixels pixels(GetEnv(), jPixels);

        if (!pWindow || !pWindow->IsTransparent()) {
            // Either a non-glass window (FullScreenWindow), or not transparent
            BITMAPINFOHEADER bmi;

            ZeroMemory(&bmi, sizeof(bmi));
            bmi.biSize = sizeof(bmi);
            bmi.biWidth = pixels.GetWidth();
            bmi.biHeight = -pixels.GetHeight();
            bmi.biPlanes = 1;
            bmi.biBitCount = 32;
            bmi.biCompression = BI_RGB;

            HDC hdcDst = ::GetDC(hWnd);
            ::SetDIBitsToDevice(
                    hdcDst,
                    0, 0, pixels.GetWidth(), pixels.GetHeight(),
                    0, 0,
                    0, pixels.GetHeight(),
                    pixels.GetBits(),
                    (BITMAPINFO*)&bmi, DIB_RGB_COLORS);
            ::ReleaseDC(hWnd, hdcDst);
        } else { // IsTransparent() == TRUE
            // http://msdn.microsoft.com/en-us/library/ms997507.aspx
            RECT rect;
            ::GetWindowRect(hWnd, &rect);
            SIZE size = { rect.right - rect.left, rect.bottom - rect.top };

            if (size.cx != pixels.GetWidth() || size.cy != pixels.GetHeight()) {
                //XXX: should report a error? OTOH, we could proceed, but
                //this will cause the window to resize to the size of the bitmap
                return;
            }

            POINT ptSrc = { 0, 0 };
            POINT ptDst = { rect.left, rect.top };

            BLENDFUNCTION bf;
            bf.SourceConstantAlpha = pWindow->GetAlpha();
            bf.AlphaFormat = AC_SRC_ALPHA;
            bf.BlendOp = AC_SRC_OVER;
            bf.BlendFlags = 0;

            DIBitmap bitmap(pixels);

            HDC hdcDst = ::GetDC(NULL);
            HDC hdcSrc = ::CreateCompatibleDC(NULL);
            HBITMAP oldBitmap = (HBITMAP)::SelectObject(hdcSrc, bitmap);

            ::UpdateLayeredWindow(hWnd, hdcDst, &ptDst, &size, hdcSrc, &ptSrc,
                    RGB(0, 0, 0), &bf, ULW_ALPHA);

            ::SelectObject(hdcSrc, oldBitmap);
            ::DeleteDC(hdcSrc);
            ::ReleaseDC(NULL, hdcDst);
        }
    }
    DECL_jobject(jPixels);
    LEAVE_MAIN_THREAD_WITH_view;

    ARG(jPixels) = jPixels;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _scheduleRepaint
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1scheduleRepaint
  (JNIEnv *env, jobject jview, jlong ptr)
{
    ENTER_MAIN_THREAD()
    {
        if (view->GetHostHwnd()) {
            ::InvalidateRect(view->GetHostHwnd(), NULL, FALSE);
        }
    }
    LEAVE_MAIN_THREAD_WITH_view;

    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _enterFullscreen
 * Signature: (JZZZ)V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinView__1enterFullscreen
(JNIEnv *env, jobject jview, jlong ptr, jboolean animate, jboolean keepRatio, jboolean hideCursor)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        return bool_to_jbool(view->EnterFullScreen(animate, keepRatio));
    }
    BOOL animate;
    BOOL keepRatio;
    LEAVE_MAIN_THREAD_WITH_view;

    ARG(animate) = (BOOL)animate;
    ARG(keepRatio) = (BOOL)keepRatio;
    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _exitFullscreen
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1exitFullscreen
(JNIEnv *env, jobject jview, jlong ptr, jboolean animate)
{
    ENTER_MAIN_THREAD()
    {
        view->ExitFullScreen(animate);
    }
    BOOL animate;
    LEAVE_MAIN_THREAD_WITH_view;

    ARG(animate) = (BOOL)animate;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _enableInputMethodEvents
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1enableInputMethodEvents
  (JNIEnv *env, jobject jview, jlong ptr, jboolean enable)
{
    GlassView* view = (GlassView*)jlong_to_ptr(ptr);
    view->EnableInputMethodEvents(jbool_to_bool(enable));
}

/*
 * Class:     com_sun_glass_ui_win_WinView
 * Method:    _finishInputMethodComposition
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinView__1finishInputMethodComposition
  (JNIEnv *env, jobject jview, jlong ptr)
{
    GlassView* view = (GlassView*)jlong_to_ptr(ptr);
    view->FinishInputMethodComposition();
}

}   // extern "C"
