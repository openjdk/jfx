/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <UIAutomation.h>

#include "GlassApplication.h"
#include "ViewContainer.h"
#include "GlassView.h"
#include "KeyTable.h"
#include "Utils.h"
#include "GlassDnD.h"
#include "GlassInputTextInfo.h"
#include "ManipulationEvents.h"
#include "BaseWnd.h"

#include "com_sun_glass_events_ViewEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_DndEvent.h"
#include "com_sun_glass_events_TouchEvent.h"

static UINT LangToCodePage(LANGID idLang)
{
    WCHAR strCodePage[8];
    // use the LANGID to create a LCID
    LCID idLocale = MAKELCID(idLang, SORT_DEFAULT);
    // get the ANSI code page associated with this locale
    if (::GetLocaleInfo(idLocale, LOCALE_IDEFAULTANSICODEPAGE,
                        strCodePage, sizeof(strCodePage) / sizeof(WCHAR)) > 0)
    {
        return _wtoi(strCodePage);
    } else {
        return ::GetACP();
    }
}

namespace {

bool IsTouchEvent()
{
    // Read this link if you wonder why we need to hard code the mask and signature:
    // http://msdn.microsoft.com/en-us/library/windows/desktop/ms703320(v=vs.85).aspx
    //"The lower 8 bits returned from GetMessageExtraInfo are variable.
    // Of those bits, 7 (the lower 7, masked by 0x7F) are used to represent the cursor ID,
    // zero for the mouse or a variable value for the pen ID.
    // Additionally, in Windows Vista, the eighth bit, masked by 0x80, is used to
    // differentiate touch input from pen input (0 = pen, 1 = touch)."
    UINT SIGNATURE = 0xFF515780;
    UINT MASK = 0xFFFFFF80;

    UINT v = (UINT) GetMessageExtraInfo();

    return ((v & MASK) == SIGNATURE);
}

} // namespace

ViewContainer::ViewContainer() :
    m_view(NULL),
    m_bTrackingMouse(FALSE),
    m_manipProc(NULL),
    m_inertiaProc(NULL),
    m_manipEventSink(NULL),
    m_gestureSupportCls(NULL),
    m_lastMouseMovePosition(-1),
    m_mouseButtonDownCounter(0),
    m_deadKeyWParam(0)
{
    m_kbLayout = ::GetKeyboardLayout(0);
    m_idLang = LOWORD(m_kbLayout);
    m_codePage = LangToCodePage(m_idLang);
    m_lastTouchInputCount = 0;
}

jobject ViewContainer::GetView()
{
    return GetGlassView() != NULL ? GetGlassView()->GetView() : NULL;
}

void ViewContainer::InitDropTarget(HWND hwnd)
{
    if (!hwnd) {
        return;
    }

    m_spDropTarget =
        std::auto_ptr<IDropTarget>(new GlassDropTarget(this, hwnd));
}

void ViewContainer::ReleaseDropTarget()
{
    m_spDropTarget = std::auto_ptr<IDropTarget>();
}

void ViewContainer::InitManipProcessor(HWND hwnd)
{
    if (IS_WIN7) {
        ::RegisterTouchWindow(hwnd, TWF_WANTPALM);

        HRESULT hr = ::CoCreateInstance(CLSID_ManipulationProcessor,
                                        NULL,
                                        CLSCTX_INPROC_SERVER,
                                        IID_IUnknown,
                                        (VOID**)(&m_manipProc)
                                        );

        if (SUCCEEDED(hr)) {
            ::CoCreateInstance(CLSID_InertiaProcessor,
                               NULL,
                               CLSCTX_INPROC_SERVER,
                               IID_IUnknown,
                               (VOID**)(&m_inertiaProc)
                               );

            m_manipEventSink =
                new ManipulationEventSinkWithInertia(m_manipProc, m_inertiaProc, this, hwnd);
        }

        const DWORD_PTR dwHwndTabletProperty =
             TABLET_DISABLE_PENTAPFEEDBACK |
             TABLET_DISABLE_PENBARRELFEEDBACK |
             TABLET_DISABLE_FLICKS;
        ::SetProp(hwnd, MICROSOFT_TABLETPENSERVICE_PROPERTY, reinterpret_cast<HANDLE>(dwHwndTabletProperty));

        if (!m_gestureSupportCls) {
            JNIEnv *env = GetEnv();
            const jclass cls = GlassApplication::ClassForName(env,
                    "com.sun.glass.ui.win.WinGestureSupport");

            m_gestureSupportCls = (jclass)env->NewGlobalRef(cls);
            env->DeleteLocalRef(cls);
            ASSERT(m_gestureSupportCls);
        }
    }
}

void ViewContainer::ReleaseManipProcessor()
{
    if (IS_WIN7) {
        if (m_manipProc) {
            m_manipProc->Release();
            m_manipProc = NULL;
        }
        if (m_inertiaProc) {
            m_inertiaProc->Release();
            m_inertiaProc = NULL;
        }
        if (m_manipEventSink) {
            m_manipEventSink->Release();
            m_manipEventSink = NULL;
        }
    }

    if (m_gestureSupportCls) {
        JNIEnv *env = GetEnv();
        env->DeleteGlobalRef(m_gestureSupportCls);
        m_gestureSupportCls = 0;
    }
}

void ViewContainer::HandleViewInputLangChange(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return;
    }

    m_kbLayout = reinterpret_cast<HKL>(lParam);
    m_idLang = LOWORD(m_kbLayout);
    m_codePage = LangToCodePage(m_idLang);

    m_deadKeyWParam = 0;
}

void ViewContainer::NotifyViewMoved(HWND hwnd)
{
    if (!hwnd || !GetGlassView()) {
        return;
    }

    JNIEnv* env = GetEnv();
    env->CallVoidMethod(GetView(), javaIDs.View.notifyView,
                        com_sun_glass_events_ViewEvent_MOVE);
    CheckAndClearException(env);
}

void ViewContainer::NotifyViewSize(HWND hwnd)
{
    if (!hwnd || !GetGlassView()) {
        return;
    }

    RECT r;
    if (::GetClientRect(hwnd, &r)) {
        JNIEnv* env = GetEnv();
        env->CallVoidMethod(GetView(), javaIDs.View.notifyResize,
                            r.right-r.left, r.bottom - r.top);
        CheckAndClearException(env);
    }
}

void ViewContainer::HandleViewPaintEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return;
    }

    RECT r;
    if (!::GetUpdateRect(hwnd, &r, FALSE)) {
        return;
    }

    JNIEnv* env = GetEnv();
    env->CallVoidMethod(GetView(), javaIDs.View.notifyRepaint,
            r.left, r.top, r.right-r.left, r.bottom-r.top);
    CheckAndClearException(env);
}


LRESULT ViewContainer::HandleViewGetAccessible(HWND hwnd, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return NULL;
    }

    /* WM_GETOBJECT  is sent to request different object types,
     * always test the type to avoid unnecessary work.
     */
    LRESULT lr = NULL;
    if (static_cast<long>(lParam) == static_cast<long>(UiaRootObjectId)) {

        /* The client is requesting UI Automation. */
        JNIEnv* env = GetEnv();
        if (!env) return NULL;
        jlong pProvider = env->CallLongMethod(GetView(), javaIDs.View.getAccessible);
        CheckAndClearException(env);

        /* It is possible WM_GETOBJECT is sent before the toolkit is ready to
         * create the accessible object (getAccessible returns NULL).
         * On Windows 7, calling UiaReturnRawElementProvider() with a NULL provider
         * returns an invalid LRESULT which stops further WM_GETOBJECT to be sent,
         * effectively disabling accessibility for the window.
         */
        if (pProvider) {
            lr = UiaReturnRawElementProvider(hwnd, wParam, lParam, reinterpret_cast<IRawElementProviderSimple*>(pProvider));
        }
    } else if (static_cast<long>(lParam) == static_cast<long>(OBJID_CLIENT)) {

        /* By default JAWS does not send WM_GETOBJECT with UiaRootObjectId till
         * a focus event is raised by UiaRaiseAutomationEvent().
         * In some systems (i.e. touch monitors), OBJID_CLIENT are sent when
         * no screen reader is active. Test for SPI_GETSCREENREADER and
         * UiaClientsAreListening() to avoid initializing accessibility
         * unnecessarily.
         */
        UINT screenReader = 0;
        ::SystemParametersInfo(SPI_GETSCREENREADER, 0, &screenReader, 0);
        if (screenReader && UiaClientsAreListening()) {
            JNIEnv* env = GetEnv();
            if (env) {

                /* Calling getAccessible() initializes accessibility which
                 * eventually raises the focus events required to indicate to
                 * JAWS to use UIA for this window.
                 *
                 * Note: do not return the accessible object for OBJID_CLIENT,
                 * that would create an UIA-MSAA bridge. That problem with the
                 * bridge is that it does not respect
                 * ProviderOptions_UseComThreading.
                 */
                env->CallLongMethod(GetView(), javaIDs.View.getAccessible);
                CheckAndClearException(env);
            }
        }
    }
    return lr;
}


void ViewContainer::HandleViewSizeEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (wParam == SIZE_MINIMIZED) {
        return;
    }
    NotifyViewSize(hwnd);
}

void ViewContainer::HandleViewMenuEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return;
    }
    if ((HWND)wParam != hwnd) {
        return;
    }
    jboolean isKeyboardTrigger = lParam == (LPARAM)-1;
    if (isKeyboardTrigger) {
        lParam = ::GetMessagePos ();
    }
    POINT pt;
    int absX = pt.x = GET_X_LPARAM(lParam);
    int absY = pt.y = GET_Y_LPARAM(lParam);
    ::ScreenToClient (hwnd, &pt);
    if (!isKeyboardTrigger) {
        RECT rect;
        ::GetClientRect(hwnd, &rect);
        if (!::PtInRect(&rect, pt)) {
            return;
        }
    }
    // unmirror the x coordinate
    LONG style = ::GetWindowLong(hwnd, GWL_EXSTYLE);
    if (style & WS_EX_LAYOUTRTL) {
        RECT rect = {0};
        ::GetClientRect(hwnd, &rect);
        pt.x = max(0, rect.right - rect.left) - pt.x;
    }
    JNIEnv* env = GetEnv();
    env->CallVoidMethod(GetView(), javaIDs.View.notifyMenu, pt.x, pt.y, absX, absY, isKeyboardTrigger);
    CheckAndClearException(env);
}

void ViewContainer::HandleViewKeyEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return;
    }

    static const BYTE KEY_STATE_DOWN = 0x80;

    UINT wKey = static_cast<UINT>(wParam);
    UINT flags = HIWORD(lParam);

    jint jKeyCode = WindowsKeyToJavaKey(wKey);
    if (flags & (1 << 8)) {
        // this is an extended key (e.g. Right ALT == AltGr)
        switch (jKeyCode) {
            case com_sun_glass_events_KeyEvent_VK_ALT:
                jKeyCode = com_sun_glass_events_KeyEvent_VK_ALT_GRAPH;
                break;
        }
    }

    BYTE kbState[256];
    if (!::GetKeyboardState(kbState)) {
        return;
    }

    jint jModifiers = GetModifiers();

    if (jModifiers & com_sun_glass_events_KeyEvent_MODIFIER_CONTROL) {
        kbState[VK_CONTROL] &= ~KEY_STATE_DOWN;
    }

    WORD mbChar;
    UINT scancode = ::MapVirtualKeyEx(wKey, 0, m_kbLayout);

    // Depress modifiers to map a Unicode char to a key code
    kbState[VK_CONTROL] &= ~0x80;
    kbState[VK_SHIFT]   &= ~0x80;
    kbState[VK_MENU]    &= ~0x80;

    int converted = ::ToAsciiEx(wKey, scancode, kbState,
                                &mbChar, 0, m_kbLayout);

    wchar_t wChar[4] = {0};
    int unicodeConverted = ::ToUnicodeEx(wKey, scancode, kbState,
                                wChar, 4, 0, m_kbLayout);

    // Some virtual codes require special handling
    switch (wKey) {
        case 0x00BA:// VK_OEM_1
        case 0x00BB:// VK_OEM_PLUS
        case 0x00BC:// VK_OEM_COMMA
        case 0x00BD:// VK_OEM_MINUS
        case 0x00BE:// VK_OEM_PERIOD
        case 0x00BF:// VK_OEM_2
        case 0x00C0:// VK_OEM_3
        case 0x00DB:// VK_OEM_4
        case 0x00DC:// VK_OEM_5
        case 0x00DD:// VK_OEM_6
        case 0x00DE:// VK_OEM_7
        case 0x00DF:// VK_OEM_8
        case 0x00E2:// VK_OEM_102
            if (unicodeConverted < 0) {
                // Dead key
                switch (wChar[0]) {
                    case L'`':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_GRAVE; break;
                    case L'\'':  jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ACUTE; break;
                    case 0x00B4: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ACUTE; break;
                    case L'^':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CIRCUMFLEX; break;
                    case L'~':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_TILDE; break;
                    case 0x02DC: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_TILDE; break;
                    case 0x00AF: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_MACRON; break;
                    case 0x02D8: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_BREVE; break;
                    case 0x02D9: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ABOVEDOT; break;
                    case L'"':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_DIAERESIS; break;
                    case 0x00A8: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_DIAERESIS; break;
                    case 0x02DA: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_ABOVERING; break;
                    case 0x02DD: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_DOUBLEACUTE; break;
                    case 0x02C7: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CARON; break;            // aka hacek
                    case L',':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CEDILLA; break;
                    case 0x00B8: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_CEDILLA; break;
                    case 0x02DB: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_OGONEK; break;
                    case 0x037A: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_IOTA; break;             // ASCII ???
                    case 0x309B: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_VOICED_SOUND; break;
                    case 0x309C: jKeyCode = com_sun_glass_events_KeyEvent_VK_DEAD_SEMIVOICED_SOUND; break;

                    default:     jKeyCode = com_sun_glass_events_KeyEvent_VK_UNDEFINED; break;
                };
            } else if (unicodeConverted == 1) {
                switch (wChar[0]) {
                    case L'!':   jKeyCode = com_sun_glass_events_KeyEvent_VK_EXCLAMATION; break;
                    case L'"':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DOUBLE_QUOTE; break;
                    case L'#':   jKeyCode = com_sun_glass_events_KeyEvent_VK_NUMBER_SIGN; break;
                    case L'$':   jKeyCode = com_sun_glass_events_KeyEvent_VK_DOLLAR; break;
                    case L'&':   jKeyCode = com_sun_glass_events_KeyEvent_VK_AMPERSAND; break;
                    case L'\'':  jKeyCode = com_sun_glass_events_KeyEvent_VK_QUOTE; break;
                    case L'(':   jKeyCode = com_sun_glass_events_KeyEvent_VK_LEFT_PARENTHESIS; break;
                    case L')':   jKeyCode = com_sun_glass_events_KeyEvent_VK_RIGHT_PARENTHESIS; break;
                    case L'*':   jKeyCode = com_sun_glass_events_KeyEvent_VK_ASTERISK; break;
                    case L'+':   jKeyCode = com_sun_glass_events_KeyEvent_VK_PLUS; break;
                    case L',':   jKeyCode = com_sun_glass_events_KeyEvent_VK_COMMA; break;
                    case L'-':   jKeyCode = com_sun_glass_events_KeyEvent_VK_MINUS; break;
                    case L'.':   jKeyCode = com_sun_glass_events_KeyEvent_VK_PERIOD; break;
                    case L'/':   jKeyCode = com_sun_glass_events_KeyEvent_VK_SLASH; break;
                    case L':':   jKeyCode = com_sun_glass_events_KeyEvent_VK_COLON; break;
                    case L';':   jKeyCode = com_sun_glass_events_KeyEvent_VK_SEMICOLON; break;
                    case L'<':   jKeyCode = com_sun_glass_events_KeyEvent_VK_LESS; break;
                    case L'=':   jKeyCode = com_sun_glass_events_KeyEvent_VK_EQUALS; break;
                    case L'>':   jKeyCode = com_sun_glass_events_KeyEvent_VK_GREATER; break;
                    case L'@':   jKeyCode = com_sun_glass_events_KeyEvent_VK_AT; break;
                    case L'[':   jKeyCode = com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET; break;
                    case L'\\':  jKeyCode = com_sun_glass_events_KeyEvent_VK_BACK_SLASH; break;
                    case L']':   jKeyCode = com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET; break;
                    case L'^':   jKeyCode = com_sun_glass_events_KeyEvent_VK_CIRCUMFLEX; break;
                    case L'_':   jKeyCode = com_sun_glass_events_KeyEvent_VK_UNDERSCORE; break;
                    case L'`':   jKeyCode = com_sun_glass_events_KeyEvent_VK_BACK_QUOTE; break;
                    case L'{':   jKeyCode = com_sun_glass_events_KeyEvent_VK_BRACELEFT; break;
                    case L'}':   jKeyCode = com_sun_glass_events_KeyEvent_VK_BRACERIGHT; break;
                    case 0x00A1: jKeyCode = com_sun_glass_events_KeyEvent_VK_INV_EXCLAMATION; break;
                    case 0x20A0: jKeyCode = com_sun_glass_events_KeyEvent_VK_EURO_SIGN; break;

                    default:     jKeyCode = com_sun_glass_events_KeyEvent_VK_UNDEFINED; break;
                }
            } else if (unicodeConverted == 0 || unicodeConverted > 1) {
                jKeyCode = com_sun_glass_events_KeyEvent_VK_UNDEFINED;
            }
            break;
    };


    int keyCharCount = 0;
    jchar keyChars[4];
    const bool isAutoRepeat = (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN)
        && (lParam & (1 << 30));

    if (converted < 0) {
        // Dead key
        return;
    } else if (converted == 0) {
        // No translation available
        keyCharCount = 0;
        // This includes SHIFT, CONTROL, ALT, etc.
        // RT-17062: suppress auto-repeated events for modifier keys
        if (isAutoRepeat) {
            switch (jKeyCode) {
                case com_sun_glass_events_KeyEvent_VK_SHIFT:
                case com_sun_glass_events_KeyEvent_VK_CONTROL:
                case com_sun_glass_events_KeyEvent_VK_ALT:
                case com_sun_glass_events_KeyEvent_VK_ALT_GRAPH:
                case com_sun_glass_events_KeyEvent_VK_WINDOWS:
                    return;
            }
        }
    } else {
        // Handle some special cases
        if ((wKey == VK_BACK) ||
            (wKey == VK_ESCAPE))
        {
            keyCharCount = 0;
        } else {
            keyCharCount = ::MultiByteToWideChar(m_codePage, MB_PRECOMPOSED,
                                                 (LPCSTR)&mbChar, 2, (LPWSTR)keyChars,
                                                 4 * sizeof(jchar)) - 1;
            if (keyCharCount <= 0) {
                return;
            }
        }
    }

    JNIEnv* env = GetEnv();

    jcharArray jKeyChars = env->NewCharArray(keyCharCount);
    if (jKeyChars) {
        if (keyCharCount) {
            env->SetCharArrayRegion(jKeyChars, 0, keyCharCount, keyChars);
            CheckAndClearException(env);
        }

        if (jKeyCode == com_sun_glass_events_KeyEvent_VK_PRINTSCREEN &&
                (msg == WM_KEYUP || msg == WM_SYSKEYUP))
        {
            // MS Windows doesn't send WM_KEYDOWN for the PrintScreen key,
            // so we synthesize one
            env->CallVoidMethod(GetView(), javaIDs.View.notifyKey,
                    com_sun_glass_events_KeyEvent_PRESS,
                    jKeyCode, jKeyChars, jModifiers);
            CheckAndClearException(env);
        }

        if (GetGlassView()) {
            env->CallVoidMethod(GetView(), javaIDs.View.notifyKey,
                    (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) ?
                    com_sun_glass_events_KeyEvent_PRESS : com_sun_glass_events_KeyEvent_RELEASE,
                    jKeyCode, jKeyChars, jModifiers);
            CheckAndClearException(env);
        }

        // MS Windows doesn't send WM_CHAR for the Delete key,
        // so we synthesize one
        if (jKeyCode == com_sun_glass_events_KeyEvent_VK_DELETE &&
                (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN) &&
                GetGlassView())
        {
            // 0x7F == U+007F - a Unicode character for DELETE
            SendViewTypedEvent(1, (jchar)0x7F);
        }

        env->DeleteLocalRef(jKeyChars);
    }
}

void ViewContainer::SendViewTypedEvent(int repCount, jchar wChar)
{
    if (!GetGlassView()) {
        return;
    }

    JNIEnv* env = GetEnv();
    jcharArray jKeyChars = env->NewCharArray(repCount);
    if (jKeyChars) {
        jchar* nKeyChars = env->GetCharArrayElements(jKeyChars, NULL);
        if (nKeyChars) {
            for (int i = 0; i < repCount; i++) {
                nKeyChars[i] = wChar;
            }
            env->ReleaseCharArrayElements(jKeyChars, nKeyChars, 0);

            env->CallVoidMethod(GetView(), javaIDs.View.notifyKey,
                                com_sun_glass_events_KeyEvent_TYPED,
                                com_sun_glass_events_KeyEvent_VK_UNDEFINED, jKeyChars,
                                GetModifiers());
            CheckAndClearException(env);
        }
        env->DeleteLocalRef(jKeyChars);
    }
}

void ViewContainer::HandleViewDeadKeyEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return;
    }

    if (!m_deadKeyWParam) {
        // HandleViewKeyEvent() calls ::ToAsciiEx and ::ToUnicodeEx which clear
        // the dead key status from the keyboard layout. We store the current dead
        // key here to use it when processing WM_CHAR in order to get the
        // actual character typed.

        m_deadKeyWParam = wParam;
    } else {
        // There already was another dead key pressed previously. Clear it
        // and send two separate TYPED events instead to emulate native behavior.

        SendViewTypedEvent(1, (jchar)m_deadKeyWParam);
        SendViewTypedEvent(1, (jchar)wParam);

        m_deadKeyWParam = 0;
    }

    // Since we handle dead keys ourselves, reset the keyboard dead key status (if any)
    static BYTE kbState[256];
    ::GetKeyboardState(kbState);
    WORD ignored;
    ::ToAsciiEx(VK_SPACE, ::MapVirtualKey(VK_SPACE, 0),
            kbState, &ignored, 0, m_kbLayout);
}

void ViewContainer::HandleViewTypedEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return;
    }

    int repCount = LOWORD(lParam);
    jchar wChar;

    if (!m_deadKeyWParam) {
        wChar = (jchar)wParam;
    } else {
        // The character is composed together with the dead key, which
        // may be translated into one or more combining characters.
        const size_t COMP_SIZE = 5;
        wchar_t comp[COMP_SIZE] = { (wchar_t)wParam };

        // Some dead keys need additional translation:
        // http://www.fileformat.info/info/unicode/block/combining_diacritical_marks/images.htm
        // Also see awt_Component.cpp for the original dead keys table
        if (LOBYTE(m_idLang) == LANG_GREEK) {
            switch (m_deadKeyWParam) {
                case L']':   comp[1] = 0x300; break; // varia
                case L';':   comp[1] = 0x301; break; // oxia (wrong? generates tonos, not oxia)
                case L'-':   comp[1] = 0x304; break; // macron
                case L'_':   comp[1] = 0x306; break; // vrachy
                case L':':   comp[1] = 0x308; break; // dialytika
                case L'"':   comp[1] = 0x314; break; // dasia
                case 0x0384: comp[1] = 0x341; break; // tonos
                case L'[':   comp[1] = 0x342; break; // perispomeni
                case L'\'':  comp[1] = 0x343; break; // psili
                case L'~':   comp[1] = 0x344; break; // dialytika oxia
                case L'{':   comp[1] = 0x345; break; // ypogegrammeni

                case L'`':   comp[1] = 0x308; comp[2] = 0x300; break; // dialytika varia
                case L'\\':  comp[1] = 0x313; comp[2] = 0x300; break; // psili varia
                case L'/':   comp[1] = 0x313; comp[2] = 0x301; break; // psili oxia
                case L'=':   comp[1] = 0x313; comp[2] = 0x342; break; // psili perispomeni
                case L'|':   comp[1] = 0x314; comp[2] = 0x300; break; // dasia varia
                case L'?':   comp[1] = 0x314; comp[2] = 0x301; break; // dasia oxia
                case L'+':   comp[1] = 0x314; comp[2] = 0x342; break; // dasia perispomeni

                // AltGr dead chars don't work. Maybe kbd isn't reset properly?
                // case 0x1fc1: comp[1] = 0x308; comp[2] = 0x342; break; // dialytika perispomeni
                // case 0x1fde: comp[1] = 0x314; comp[2] = 0x301; comp[3] = 0x345; break; // dasia oxia ypogegrammeni

                default:     comp[1] = static_cast<wchar_t>(m_deadKeyWParam); break;
            }
        } else if (HIWORD(m_kbLayout) == 0xF0B1 && LOBYTE(m_idLang) == LANG_LATVIAN) {
            // The Latvian (Standard) keyboard, available in Win 8.1 and later.
            switch (m_deadKeyWParam) {
                case L'\'':
                case L'"':
                    // Note: " is Shift-' and automatically capitalizes the typed
                    // character in native Win 8.1 apps. We don't do this, so the user
                    // needs to keep the Shift key down. This is probably the common use
                    // case anyway.
                    switch (wParam) {
                        case L'A': case L'a':
                        case L'E': case L'e':
                        case L'I': case L'i':
                        case L'U': case L'u':
                             comp[1] = 0x304; break; // macron
                        case L'C': case L'c':
                        case L'S': case L's':
                        case L'Z': case L'z':
                             comp[1] = 0x30c; break; // caron
                        case L'G': case L'g':
                        case L'K': case L'k':
                        case L'L': case L'l':
                        case L'N': case L'n':
                             comp[1] = 0x327; break; // cedilla
                        default:
                             comp[1] = static_cast<wchar_t>(m_deadKeyWParam); break;
                    } break;
                default:     comp[1] = static_cast<wchar_t>(m_deadKeyWParam); break;
            }
        } else {
            switch (m_deadKeyWParam) {
                case L'`':   comp[1] = 0x300; break;
                case L'\'':  comp[1] = 0x301; break;
                case 0x00B4: comp[1] = 0x301; break;
                case L'^':   comp[1] = 0x302; break;
                case L'~':   comp[1] = 0x303; break;
                case 0x02DC: comp[1] = 0x303; break;
                case 0x00AF: comp[1] = 0x304; break;
                case 0x02D8: comp[1] = 0x306; break;
                case 0x02D9: comp[1] = 0x307; break;
                case L'"':   comp[1] = 0x308; break;
                case 0x00A8: comp[1] = 0x308; break;
                case 0x00B0: comp[1] = 0x30A; break;
                case 0x02DA: comp[1] = 0x30A; break;
                case 0x02DD: comp[1] = 0x30B; break;
                case 0x02C7: comp[1] = 0x30C; break;
                case L',':   comp[1] = 0x327; break;
                case 0x00B8: comp[1] = 0x327; break;
                case 0x02DB: comp[1] = 0x328; break;
                default:     comp[1] = static_cast<wchar_t>(m_deadKeyWParam); break;
            }
        }

        int compSize = 3;
        for (int i = 1; i < COMP_SIZE; i++) {
            if (comp[i] == L'\0') {
                compSize = i + 1;
                break;
            }
        }
        wchar_t out[3];
        int res = ::FoldString(MAP_PRECOMPOSED, (LPWSTR)comp, compSize, (LPWSTR)out, 3);

        if (res > 0) {
            wChar = (jchar)out[0];

            if (res == 3) {
                // The character cannot be accented, so we send a TYPED event
                // for the dead key itself first.
                SendViewTypedEvent(1, (jchar)m_deadKeyWParam);
            }
        } else {
            // Folding failed. Use the untranslated original character then
            wChar = (jchar)wParam;
        }

        // Clear the dead key
        m_deadKeyWParam = 0;
    }

    SendViewTypedEvent(repCount, wChar);
}

BOOL ViewContainer::HandleViewMouseEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    if (!GetGlassView()) {
        return FALSE;
    }

    int type = 0;
    int button = com_sun_glass_events_MouseEvent_BUTTON_NONE;
    POINT pt;   // client coords
    jdouble wheelRotation = 0.0;

    if (msg == WM_MOUSELEAVE) {
        type = com_sun_glass_events_MouseEvent_EXIT;
        // get the coords (the message does not contain them)
        lParam = ::GetMessagePos();
        pt.x = GET_X_LPARAM(lParam);
        pt.y = GET_Y_LPARAM(lParam);
        // this is screen coords, convert to client
        ::ScreenToClient(hwnd, &pt);

        // Windows has finished tracking mouse pointer already
        m_bTrackingMouse = FALSE;

        m_lastMouseMovePosition = -1;
    } else {
        // for all other messages lParam contains cursor coords
        pt.x = GET_X_LPARAM(lParam);
        pt.y = GET_Y_LPARAM(lParam);

        switch (msg) {
            case WM_MOUSEMOVE:
                if (lParam == m_lastMouseMovePosition) {
                    // Avoid sending synthetic MOVE/DRAG events if
                    // the pointer hasn't moved actually.
                    // Just consume the messages.
                    return TRUE;
                } else {
                    m_lastMouseMovePosition = lParam;
                }
                // See RT-11305 regarding the GetCapture() check
                if ((wParam & (MK_LBUTTON | MK_RBUTTON | MK_MBUTTON | MK_XBUTTON1 | MK_XBUTTON2)) != 0 && ::GetCapture() == hwnd) {
                    type = com_sun_glass_events_MouseEvent_DRAG;
                } else {
                    type = com_sun_glass_events_MouseEvent_MOVE;
                }
                // Due to RT-11305 we should report the pressed button for both
                // MOVE and DRAG. This also enables one to filter out these
                // events in client code in case they're undesired.
                if (wParam & MK_RBUTTON) {
                    button = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
                } else if (wParam & MK_LBUTTON) {
                    button = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
                } else if (wParam & MK_MBUTTON) {
                    button = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
                } else if (wParam & MK_XBUTTON1) {
                    button = com_sun_glass_events_MouseEvent_BUTTON_BACK;
                } else if (wParam & MK_XBUTTON2) {
                    button = com_sun_glass_events_MouseEvent_BUTTON_FORWARD;
                }
                break;
            case WM_LBUTTONDOWN:
                type = com_sun_glass_events_MouseEvent_DOWN;
                button = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
                break;
            case WM_LBUTTONUP:
                type = com_sun_glass_events_MouseEvent_UP;
                button = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
                break;
            case WM_RBUTTONDOWN:
                type = com_sun_glass_events_MouseEvent_DOWN;
                button = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
                break;
            case WM_RBUTTONUP:
                type = com_sun_glass_events_MouseEvent_UP;
                button = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
                break;
            case WM_MBUTTONDOWN:
                type = com_sun_glass_events_MouseEvent_DOWN;
                button = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
                break;
            case WM_MBUTTONUP:
                type = com_sun_glass_events_MouseEvent_UP;
                button = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
                break;
            case WM_XBUTTONDOWN:
                type = com_sun_glass_events_MouseEvent_DOWN;
                button = GET_XBUTTON_WPARAM(wParam) == XBUTTON1 ? com_sun_glass_events_MouseEvent_BUTTON_BACK :
                            com_sun_glass_events_MouseEvent_BUTTON_FORWARD;
                break;
            case WM_XBUTTONUP:
                type = com_sun_glass_events_MouseEvent_UP;
                button = GET_XBUTTON_WPARAM(wParam) == XBUTTON1 ? com_sun_glass_events_MouseEvent_BUTTON_BACK :
                            com_sun_glass_events_MouseEvent_BUTTON_FORWARD;
                break;
            case WM_MOUSEWHEEL:
            case WM_MOUSEHWHEEL:
                {
                    // MS Windows always sends WHEEL events to the focused window.
                    // Redirect the message to a Glass window under the mouse
                    // cursor instead to match Mac behavior
                    HWND hwndUnderCursor = ::ChildWindowFromPointEx(
                            ::GetDesktopWindow(), pt,
                            CWP_SKIPDISABLED | CWP_SKIPINVISIBLE);

                    if (hwndUnderCursor && hwndUnderCursor != hwnd)
                    {
                        DWORD hWndUnderCursorProcess;
                        ::GetWindowThreadProcessId(hwndUnderCursor, &hWndUnderCursorProcess);
                        if (::GetCurrentProcessId() == hWndUnderCursorProcess) {
                            return (BOOL)::SendMessage(hwndUnderCursor, msg, wParam, lParam);
                        }
                    }

                    // if there's none, proceed as usual
                    type = com_sun_glass_events_MouseEvent_WHEEL;
                    wheelRotation = (jdouble)GET_WHEEL_DELTA_WPARAM(wParam) / WHEEL_DELTA;
                }
                break;
        }
    }

    switch (type) {
        case 0:
            // not handled
            return FALSE;
        case com_sun_glass_events_MouseEvent_DOWN:
            m_mouseButtonDownCounter++;
            if (::GetCapture() != hwnd) {
                ::SetCapture(hwnd);
            }
            break;
        case com_sun_glass_events_MouseEvent_UP:
            if (m_mouseButtonDownCounter) {
                m_mouseButtonDownCounter--;
            } //else { internal inconsistency; quite unimportant though }
            if (::GetCapture() == hwnd && !m_mouseButtonDownCounter) {
                ::ReleaseCapture();
            }
            break;
    }

    // get screen coords
    POINT ptAbs = pt;
    if (type == com_sun_glass_events_MouseEvent_WHEEL) {
        ::ScreenToClient(hwnd, &pt);
    } else {
        ::ClientToScreen(hwnd, &ptAbs);
    }

    // unmirror the x coordinate
    LONG style = ::GetWindowLong(hwnd, GWL_EXSTYLE);
    if (style & WS_EX_LAYOUTRTL) {
        RECT rect = {0};
        ::GetClientRect(hwnd, &rect);
        pt.x = max(0, rect.right - rect.left) - pt.x;
    }

    jint jModifiers = GetModifiers();

    const jboolean isSynthesized = jboolean(IsTouchEvent());

    JNIEnv *env = GetEnv();

    if (!m_bTrackingMouse && type != com_sun_glass_events_MouseEvent_EXIT) {
        TRACKMOUSEEVENT trackData;
        trackData.cbSize = sizeof(trackData);
        trackData.dwFlags = TME_LEAVE;
        trackData.hwndTrack = hwnd;
        trackData.dwHoverTime = HOVER_DEFAULT;
        if (::TrackMouseEvent(&trackData)) {
            // Mouse tracking will be canceled automatically upon receiving WM_MOUSELEAVE
            m_bTrackingMouse = TRUE;
        }

        // Note that (ViewContainer*)this != (BaseWnd*)this. We could use
        // dynamic_case<>() instead, but it would fail later if 'this' is
        // already deleted. So we use FromHandle() which is safe.
        const BaseWnd *origWnd = BaseWnd::FromHandle(hwnd);

        env->CallVoidMethod(GetView(), javaIDs.View.notifyMouse,
                com_sun_glass_events_MouseEvent_ENTER,
                com_sun_glass_events_MouseEvent_BUTTON_NONE,
                pt.x, pt.y, ptAbs.x, ptAbs.y,
                jModifiers, JNI_FALSE, isSynthesized);
        CheckAndClearException(env);

        // At this point 'this' might have already been deleted if the app
        // closed the window while processing the ENTER event. Hence the check:
        if (!::IsWindow(hwnd) || BaseWnd::FromHandle(hwnd) != origWnd ||
                !GetGlassView())
        {
            return TRUE;
        }
    }

    switch (type) {
        case com_sun_glass_events_MouseEvent_DOWN:
            GlassDropSource::SetDragButton(button);
            break;
        case com_sun_glass_events_MouseEvent_UP:
            GlassDropSource::SetDragButton(0);
            break;
    }

    if (type == com_sun_glass_events_MouseEvent_WHEEL) {
        jdouble dx, dy;
        if (msg == WM_MOUSEHWHEEL) { // native horizontal scroll
            // Negate the value to be more "natural"
            dx = -wheelRotation;
            dy = 0.0;
        } else if (msg == WM_MOUSEWHEEL && LOWORD(wParam) & MK_SHIFT) {
            // Do not negate the emulated horizontal scroll amount
            dx = wheelRotation;
            dy = 0.0;
        } else { // vertical scroll
            dx = 0.0;
            dy = wheelRotation;
        }

        jint ls, cs;

        UINT val = 0;
        ::SystemParametersInfo(SPI_GETWHEELSCROLLLINES, 0, &val, 0);
        ls = (jint)val;

        val = 0;
        ::SystemParametersInfo(SPI_GETWHEELSCROLLCHARS, 0, &val, 0);
        cs = (jint)val;

        env->CallVoidMethod(GetView(), javaIDs.View.notifyScroll,
                pt.x, pt.y, ptAbs.x, ptAbs.y,
                dx, dy, jModifiers, ls, cs, 3, 3, (jdouble)40.0, (jdouble)40.0);
    } else {
        env->CallVoidMethod(GetView(), javaIDs.View.notifyMouse,
                type, button, pt.x, pt.y, ptAbs.x, ptAbs.y,
                jModifiers,
                type == com_sun_glass_events_MouseEvent_UP && button == com_sun_glass_events_MouseEvent_BUTTON_RIGHT,
                isSynthesized);
    }
    CheckAndClearException(env);

    return TRUE;
}

void ViewContainer::NotifyCaptureChanged(HWND hwnd, HWND to)
{
    m_mouseButtonDownCounter = 0;
}

void ViewContainer::ResetMouseTracking(HWND hwnd)
{
    if (!m_bTrackingMouse) {
        return;
    }

    // We don't expect WM_MOUSELEAVE anymore, so we cancel mouse tracking manually
    TRACKMOUSEEVENT trackData;
    trackData.cbSize = sizeof(trackData);
    trackData.dwFlags = TME_LEAVE | TME_CANCEL;
    trackData.hwndTrack = hwnd;
    trackData.dwHoverTime = HOVER_DEFAULT;
    ::TrackMouseEvent(&trackData);

    m_bTrackingMouse = FALSE;

    if (!GetGlassView()) {
        return;
    }

    POINT ptAbs;
    ::GetCursorPos(&ptAbs);

    POINT pt = ptAbs;
    ::ScreenToClient(hwnd, &pt);

    // unmirror the x coordinate
    LONG style = ::GetWindowLong(hwnd, GWL_EXSTYLE);
    if (style & WS_EX_LAYOUTRTL) {
        RECT rect = {0};
        ::GetClientRect(hwnd, &rect);
        pt.x = max(0, rect.right - rect.left) - pt.x;
    }

    JNIEnv *env = GetEnv();
    env->CallVoidMethod(GetView(), javaIDs.View.notifyMouse,
            com_sun_glass_events_MouseEvent_EXIT, 0, pt.x, pt.y, ptAbs.x, ptAbs.y,
            GetModifiers(),
            JNI_FALSE,
            JNI_FALSE);
    CheckAndClearException(env);
}

BOOL ViewContainer::HandleViewInputMethodEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    GlassView* gv = GetGlassView();

    if (!gv) {
        return FALSE;
    }

    switch (msg) {
    case WM_IME_ENDCOMPOSITION:
        SendInputMethodEvent(NULL, 0, NULL, 0, NULL, NULL, 0, 0, 0);
    case WM_IME_STARTCOMPOSITION:
        return gv->IsInputMethodEventEnabled();

    case WM_IME_COMPOSITION:
        if (gv->IsInputMethodEventEnabled()) {
            WmImeComposition(hwnd, wParam, lParam);
            return TRUE;
        }
        break;

    case WM_IME_NOTIFY:
        if (gv->IsInputMethodEventEnabled()) {
            WmImeNotify(hwnd, wParam, lParam);
        }
        break;

    default:
        return FALSE;
    }

    return FALSE;
}

void ViewContainer::WmImeComposition(HWND hwnd, WPARAM wParam, LPARAM lParam)
{
    BOOL ret = FALSE;

    JNIEnv *env = GetEnv();

    int*      bndClauseW = NULL;
    int*      bndAttrW = NULL;
    BYTE*     valAttrW = NULL;
    int       cClauseW = 0;
    GlassInputTextInfo textInfo = GlassInputTextInfo(this);
    HIMC hIMC = ImmGetContext(hwnd);
    ASSERT(hIMC!=0);

    try {
        textInfo.GetContextData(hIMC, lParam);

        jstring jtextString = textInfo.GetText();
        if ((lParam & GCS_RESULTSTR && jtextString != NULL) ||
            (lParam & GCS_COMPSTR)) {
            int       cursorPosW = textInfo.GetCursorPosition();
            int       cAttrW = textInfo.GetAttributeInfo(bndAttrW, valAttrW);
            cClauseW = textInfo.GetClauseInfo(bndClauseW);

            SendInputMethodEvent(jtextString,
                                 cClauseW, bndClauseW,
                                 cAttrW, bndAttrW, valAttrW,
                                 textInfo.GetCommittedTextLength(),
                                 cursorPosW, cursorPosW);

        }
        ImmReleaseContext(hwnd, hIMC);
    } catch (...) {
        // since GetClauseInfo and GetAttributeInfo could throw exception, we have to release
        // the pointer here.
        delete [] bndClauseW;
        delete [] bndAttrW;
        delete [] valAttrW;
        ImmReleaseContext(hwnd, hIMC);
        throw;
    }

    /* Free the storage allocated. Since jtextString won't be passed from threads
     *  to threads, we just use the local ref and it will be deleted within the destructor
     *  of GlassInputTextInfo object.
     */

    delete [] bndClauseW;
    delete [] bndAttrW;
    delete [] valAttrW;
    CheckAndClearException(env);
}

void ViewContainer::WmImeNotify(HWND hwnd, WPARAM wParam, LPARAM lParam)
{
    if (wParam == IMN_OPENCANDIDATE || wParam == IMN_CHANGECANDIDATE) {
        JNIEnv *env = GetEnv();
        POINT curPos;
        UINT bits = 1;
        HIMC hIMC = ImmGetContext(hwnd);
        CANDIDATEFORM cf;

        GetCandidatePos(&curPos);
        ::ScreenToClient(hwnd, &curPos);

        for (int iCandType=0; iCandType<32; iCandType++, bits<<=1) {
            if (lParam & bits) {
                cf.dwIndex = iCandType;
                cf.dwStyle = CFS_CANDIDATEPOS;
                // The constant offset is needed because Windows is moving the IM window
                cf.ptCurrentPos.x = curPos.x - 6;
                cf.ptCurrentPos.y = curPos.y - 15;
                ::ImmSetCandidateWindow(hIMC, &cf);
            }
        }
        ImmReleaseContext(hwnd, hIMC);
    }
}

//
// generate and post InputMethodEvent
//
void ViewContainer::SendInputMethodEvent(jstring text,
    int cClause, int* rgClauseBoundary,
    int cAttrBlock, int* rgAttrBoundary, BYTE *rgAttrValue,
    int commitedTextLength, int caretPos, int visiblePos)
{
    JNIEnv *env = GetEnv();

    // assumption for array type casting
    ASSERT(sizeof(int)==sizeof(jint));
    ASSERT(sizeof(BYTE)==sizeof(jbyte));

    // caluse information
    jintArray clauseBoundary = NULL;
    if (cClause && rgClauseBoundary) {
        // convert clause boundary offset array to java array
        clauseBoundary = env->NewIntArray(cClause+1);
        if (clauseBoundary) {
            env->SetIntArrayRegion(clauseBoundary, 0, cClause+1, (jint *)rgClauseBoundary);
            CheckAndClearException(env);
        }
    }

    // attribute information
    jintArray attrBoundary = NULL;
    jbyteArray attrValue = NULL;
    if (cAttrBlock && rgAttrBoundary && rgAttrValue) {
        // convert attribute boundary offset array to java array
        attrBoundary = env->NewIntArray(cAttrBlock+1);
        if (attrBoundary) {
            env->SetIntArrayRegion(attrBoundary, 0, cAttrBlock+1, (jint *)rgAttrBoundary);
            CheckAndClearException(env);
        }
        // convert attribute value byte array to java array
        attrValue = env->NewByteArray(cAttrBlock);
        if (attrValue) {
            env->SetByteArrayRegion(attrValue, 0, cAttrBlock, (jbyte *)rgAttrValue);
            CheckAndClearException(env);
        }
    }

    env->CallBooleanMethod(GetView(), javaIDs.View.notifyInputMethod,
                        text, clauseBoundary, attrBoundary,
                        attrValue, commitedTextLength, caretPos, visiblePos);
    CheckAndClearException(env);

    if (clauseBoundary) {
        env->DeleteLocalRef(clauseBoundary);
    }
    if (attrBoundary) {
        env->DeleteLocalRef(attrBoundary);
    }
    if (attrValue) {
        env->DeleteLocalRef(attrValue);
    }
}

// Gets the candidate position
void ViewContainer::GetCandidatePos(LPPOINT curPos)
{
    JNIEnv *env = GetEnv();
    double* nativePos;

    jdoubleArray pos = (jdoubleArray)env->CallObjectMethod(GetView(),
                        javaIDs.View.notifyInputMethodCandidatePosRequest,
                        0);
    nativePos = env->GetDoubleArrayElements(pos, NULL);
    if (nativePos) {
        curPos->x = (int)nativePos[0];
        curPos->y  = (int)nativePos[1];

        env->ReleaseDoubleArrayElements(pos, nativePos, 0);
    }
}

namespace {

class AutoTouchInputHandle {
    HTOUCHINPUT m_h;
private:
    AutoTouchInputHandle(const AutoTouchInputHandle&);
    AutoTouchInputHandle& operator=(const AutoTouchInputHandle&);
public:
    explicit AutoTouchInputHandle(LPARAM lParam): m_h((HTOUCHINPUT)lParam) {
    }
    ~AutoTouchInputHandle() {
        if (m_h) {
            ::CloseTouchInputHandle(m_h);
        }
    }
    operator HTOUCHINPUT() const {
        return m_h;
    }
};

static BOOL debugTouch = false;

static char * touchEventName(unsigned int dwFlags) {
        if (dwFlags & TOUCHEVENTF_MOVE) {
            return "MOVE";
        }
        if (dwFlags & TOUCHEVENTF_DOWN) {
            return "PRESS";
        }
        if (dwFlags & TOUCHEVENTF_UP) {
            return "RELEASE";
        }
        return "UNKOWN";
}

void NotifyTouchInput(
        HWND hWnd, jobject view, jclass gestureSupportCls,
        const TOUCHINPUT* ti, unsigned count)
{

    JNIEnv *env = GetEnv();

    // Sets to 'true' if source device is a touch screen
    // and to 'false' if source device is a touch pad/pen.
    const bool isDirect = IsTouchEvent();

    jint modifiers = GetModifiers();
    env->CallStaticObjectMethod(gestureSupportCls,
                                javaIDs.Gestures.notifyBeginTouchEventMID,
                                view, modifiers, jboolean(isDirect),
                                jint(count));
    CheckAndClearException(env);

    for (; count; --count, ++ti) {
        jlong touchID = jlong(ti->dwID);

        jint eventID = 0;
        if (ti->dwFlags & TOUCHEVENTF_MOVE) {
            eventID = com_sun_glass_events_TouchEvent_TOUCH_MOVED;
        }
        if (ti->dwFlags & TOUCHEVENTF_DOWN) {
            eventID = com_sun_glass_events_TouchEvent_TOUCH_PRESSED;
        }
        if (ti->dwFlags & TOUCHEVENTF_UP) {
            eventID = com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
        }

        POINT screen;
        POINT client;
        client.x = screen.x = LONG(ti->x / 100);
        client.y = screen.y = LONG(ti->y / 100);
        ScreenToClient(hWnd, &client);

        // unmirror the x coordinate
        LONG style = ::GetWindowLong(hWnd, GWL_EXSTYLE);
        if (style & WS_EX_LAYOUTRTL) {
            RECT rect = {0};
            ::GetClientRect(hWnd, &rect);
            client.x = max(0, rect.right - rect.left) - client.x;
        }

        env->CallStaticObjectMethod(gestureSupportCls,
                                    javaIDs.Gestures.notifyNextTouchEventMID,
                                    view, eventID, touchID,
                                    jint(client.x), jint(client.y),
                                    jint(screen.x), jint(screen.y));
        CheckAndClearException(env);
    }

    env->CallStaticObjectMethod(
            gestureSupportCls, javaIDs.Gestures.notifyEndTouchEventMID, view);
    CheckAndClearException(env);
}

void NotifyManipulationProcessor(
        IManipulationProcessor& manipProc,
        const TOUCHINPUT* ti, unsigned count)
{
    for (; count; --count, ++ti) {
        if (ti->dwFlags & TOUCHEVENTF_DOWN) {
            manipProc.ProcessDownWithTime(ti->dwID, FLOAT(ti->x), FLOAT(ti->y), ti->dwTime);
        }
        if (ti->dwFlags & TOUCHEVENTF_MOVE) {
            manipProc.ProcessMoveWithTime(ti->dwID, FLOAT(ti->x), FLOAT(ti->y), ti->dwTime);
        }
        if (ti->dwFlags & TOUCHEVENTF_UP) {
            manipProc.ProcessUpWithTime(ti->dwID, FLOAT(ti->x), FLOAT(ti->y), ti->dwTime);
        }
    }
}

} // namespace

unsigned int ViewContainer::HandleViewTouchEvent(
        HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    const UINT newCount = static_cast<UINT>(LOWORD(wParam));
    TOUCHINPUT * tempTouchInputBuf;

    unsigned int bufsz = newCount >  10 ? newCount : 10;
    if (m_thisTouchInputBuf.size() < bufsz) {
        m_thisTouchInputBuf.resize(bufsz);
    }

    if (newCount > 0) {
        tempTouchInputBuf = new TOUCHINPUT[newCount];
        do {
            AutoTouchInputHandle inputInfo(lParam);
            if (!::GetTouchInputInfo(inputInfo, newCount,
                                     tempTouchInputBuf, sizeof(TOUCHINPUT))) {
                delete [] tempTouchInputBuf;
                return 0;
            }
        } while(0); // scope for 'inputInfo'
    }

    // Fix up the touch point stream. Some drivers seem to lose touch events,
    // dropping PRESS, MOVE, UP, so we need to add them back in.

    unsigned int activeCount = 0;
    unsigned int pointsCount = 0;

    // check first for any "lost" touches
    // these need to get added to the send list of points
    for (unsigned int i = 0 ; i < m_lastTouchInputCount; i++) {
        if (!(m_lastTouchInputBuf[i].dwFlags & TOUCHEVENTF_UP)) {
            // looking for a dwID that is
            //   not present in the new batch
            //   was not UP in the old batch
            bool found = false;
            for (unsigned int j = 0; j < newCount; j++) {
                if (m_lastTouchInputBuf[i].dwID == tempTouchInputBuf[j].dwID) {
                    found = true;
                    //break;
                }
            }
            if (!found) {
                // We have a old event but not a new one, so release it
                m_thisTouchInputBuf[pointsCount].dwFlags = TOUCHEVENTF_UP;
                m_thisTouchInputBuf[pointsCount].dwID = m_lastTouchInputBuf[i].dwID;
                m_thisTouchInputBuf[pointsCount].x = m_lastTouchInputBuf[i].x;
                m_thisTouchInputBuf[pointsCount].y = m_lastTouchInputBuf[i].y;
                if (newCount > 0) {
                    //use the time of the first new element for our inserted event
                    m_thisTouchInputBuf[pointsCount].dwTime = tempTouchInputBuf[0].dwTime;
                } else {
                    m_thisTouchInputBuf[pointsCount].dwTime = m_lastTouchInputBuf[i].dwTime;
                }
                m_thisTouchInputBuf[pointsCount].dwMask = m_lastTouchInputBuf[i].dwMask;

                if (debugTouch) {
                        printf("TOUCH FIX UP  %d, %s\n", m_lastTouchInputBuf[i].dwID, touchEventName(m_lastTouchInputBuf[i].dwFlags));
                }

                pointsCount++;
            }
         }
    }

    if (pointsCount + newCount > m_thisTouchInputBuf.size()) {
        bufsz = pointsCount + newCount;
        m_thisTouchInputBuf.resize(bufsz);
    }

    // now fold in the current touch points
    for (unsigned int i = 0 ; i < newCount; i++) {
        bool found = false;
        for (unsigned int j = 0 ; j < m_lastTouchInputCount; j++) {
            if (m_lastTouchInputBuf[j].dwID == tempTouchInputBuf[i].dwID) {
                found = true;
                break;
            }
        }

        m_thisTouchInputBuf[pointsCount].dwFlags = tempTouchInputBuf[i].dwFlags;
        m_thisTouchInputBuf[pointsCount].dwID = tempTouchInputBuf[i].dwID;
        m_thisTouchInputBuf[pointsCount].dwTime = tempTouchInputBuf[i].dwTime;
        m_thisTouchInputBuf[pointsCount].dwMask = tempTouchInputBuf[i].dwMask;
        m_thisTouchInputBuf[pointsCount].x = tempTouchInputBuf[i].x;
        m_thisTouchInputBuf[pointsCount].y = tempTouchInputBuf[i].y;

        if (m_thisTouchInputBuf[pointsCount].dwFlags & TOUCHEVENTF_DOWN) {
            pointsCount++;
            activeCount ++;
        } else if (m_thisTouchInputBuf[pointsCount].dwFlags & TOUCHEVENTF_MOVE) {
                if (!found) {
                    if (debugTouch) {
                        printf("TOUCH FIX MV->DOWN  %d, %s\n", m_thisTouchInputBuf[pointsCount].dwID, touchEventName(m_thisTouchInputBuf[pointsCount].dwFlags));
            }
                        m_thisTouchInputBuf[pointsCount].dwFlags = TOUCHEVENTF_DOWN;
                    }
                pointsCount++;
                activeCount ++;
        } else if (m_thisTouchInputBuf[pointsCount].dwFlags & TOUCHEVENTF_UP) {
               if (found) {
                    pointsCount++;
               } else {
                   // UP without a previous DOWN, ignore it
               }
        }
     }

     if (debugTouch) {
        printf("Touch Sequence %d/%d win=%d view=%d %d,%d,%d\n",pointsCount,activeCount,
            hWnd, GetView(),
            m_lastTouchInputCount, newCount, pointsCount);
        for (unsigned int i = 0 ; i < m_lastTouchInputCount; i++) {
            printf("  old  %d, %s\n", m_lastTouchInputBuf[i].dwID, touchEventName(m_lastTouchInputBuf[i].dwFlags));
        }
        for (unsigned int i = 0 ; i < newCount; i++) {
            printf("  in   %d, %s\n", tempTouchInputBuf[i].dwID, touchEventName(tempTouchInputBuf[i].dwFlags));
        }
        for (unsigned int i = 0 ; i < pointsCount; i++) {
            printf("  this %d, %d\n", m_thisTouchInputBuf[i].dwID, m_thisTouchInputBuf[i].dwFlags & 0x07);
        }
        printf("  ---\n");
        fflush(stdout);
     }

    if (pointsCount > 0) {
        NotifyTouchInput(hWnd, GetView(), m_gestureSupportCls, &m_thisTouchInputBuf[0], pointsCount);

        if (m_manipProc) {
            NotifyManipulationProcessor(*m_manipProc, &m_thisTouchInputBuf[0], pointsCount);
        }

        std::swap(m_lastTouchInputBuf, m_thisTouchInputBuf);
        m_lastTouchInputCount = pointsCount;
    }

    if ( newCount > 0) {
        delete [] tempTouchInputBuf;
    }

    return activeCount;
}

void ViewContainer::HandleViewTimerEvent(HWND hwnd, UINT_PTR timerID)
{
    if (IDT_GLASS_INERTIAPROCESSOR == timerID) {
        BOOL completed = FALSE;
        HRESULT hr = m_inertiaProc->Process(&completed);
        if (SUCCEEDED(hr) && completed) {
            StopTouchInputInertia(hwnd);

            JNIEnv *env = GetEnv();
            env->CallStaticVoidMethod(m_gestureSupportCls,
                    javaIDs.Gestures.inertiaGestureFinishedMID, GetView());
            CheckAndClearException(env);
        }
    }
}

void ViewContainer::NotifyGesturePerformed(HWND hWnd,
        bool isDirect, bool isInertia,
        FLOAT x, FLOAT y, FLOAT deltaX, FLOAT deltaY,
        FLOAT scaleDelta, FLOAT expansionDelta, FLOAT rotationDelta,
        FLOAT cumulativeDeltaX, FLOAT cumulativeDeltaY,
        FLOAT cumulativeScale, FLOAT cumulativeExpansion,
        FLOAT cumulativeRotation)
{
    JNIEnv *env = GetEnv();

    POINT screen;
    screen.x = LONG((x + 0.5) / 100);
    screen.y = LONG((y + 0.5) / 100);

    POINT client;
    client.x = screen.x;
    client.y = screen.y;
    ScreenToClient(hWnd, &client);

    // unmirror the x coordinate
    LONG style = ::GetWindowLong(hWnd, GWL_EXSTYLE);
    if (style & WS_EX_LAYOUTRTL) {
        RECT rect = {0};
        ::GetClientRect(hWnd, &rect);
        client.x = max(0, rect.right - rect.left) - client.x;
    }

    jint modifiers = GetModifiers();
    env->CallStaticVoidMethod(m_gestureSupportCls,
                              javaIDs.Gestures.gesturePerformedMID,
                              GetView(), modifiers,
                              jboolean(isDirect), jboolean(isInertia),
                              jint(client.x), jint(client.y),
                              jint(screen.x), jint(screen.y),
                              deltaX / 100, deltaY / 100,
                              cumulativeDeltaX / 100, cumulativeDeltaY / 100,
                              cumulativeScale, cumulativeExpansion / 100,
                              cumulativeRotation);
    CheckAndClearException(env);
}

void ViewContainer::StartTouchInputInertia(HWND hwnd)
{
    // TBD: check errors

    //
    // Collect initial inertia data
    //

    FLOAT vX, vY;
    m_manipProc->GetVelocityX(&vX);
    m_manipProc->GetVelocityY(&vY);

    const FLOAT VELOCITY_THRESHOLD = 10.0f;

    if (fabs(vX) < VELOCITY_THRESHOLD && fabs(vY) < VELOCITY_THRESHOLD) {
        return;
    }

    // TBD: check errors
    POINT origin;
    GetCursorPos(&origin);

    //
    // Setup inertia.
    //

    m_inertiaProc->Reset();

    m_inertiaProc->put_DesiredDeceleration(0.23f);

    // Set initial origins.
    m_inertiaProc->put_InitialOriginX(origin.x * 100.0f);
    m_inertiaProc->put_InitialOriginY(origin.y * 100.0f);

    // Set initial velocities.
    m_inertiaProc->put_InitialVelocityX(vX);
    m_inertiaProc->put_InitialVelocityY(vY);

    // TBD: check errors
    ::SetTimer(hwnd, IDT_GLASS_INERTIAPROCESSOR, 16, NULL);
}

void ViewContainer::StopTouchInputInertia(HWND hwnd)
{
    // TBD: check errors
    ::KillTimer(hwnd, IDT_GLASS_INERTIAPROCESSOR);
}


extern "C" {
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinGestureSupport__1initIDs(
        JNIEnv *env, jclass cls)
{
    javaIDs.Gestures.gesturePerformedMID =
        env->GetStaticMethodID(cls, "gesturePerformed",
                                "(Lcom/sun/glass/ui/View;IZZIIIIFFFFFFF)V");
    CheckAndClearException(env);

    javaIDs.Gestures.inertiaGestureFinishedMID =
        env->GetStaticMethodID(cls, "inertiaGestureFinished",
                                "(Lcom/sun/glass/ui/View;)V");
    CheckAndClearException(env);

    javaIDs.Gestures.notifyBeginTouchEventMID =
        env->GetStaticMethodID(cls, "notifyBeginTouchEvent",
                                "(Lcom/sun/glass/ui/View;IZI)V");
    CheckAndClearException(env);

    javaIDs.Gestures.notifyNextTouchEventMID =
        env->GetStaticMethodID(cls, "notifyNextTouchEvent",
                                "(Lcom/sun/glass/ui/View;IJIIII)V");
    CheckAndClearException(env);

    javaIDs.Gestures.notifyEndTouchEventMID =
        env->GetStaticMethodID(cls, "notifyEndTouchEvent",
                                "(Lcom/sun/glass/ui/View;)V");
    CheckAndClearException(env);
}

} // extern "C"
