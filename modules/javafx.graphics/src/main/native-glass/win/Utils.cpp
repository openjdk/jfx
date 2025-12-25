/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_glass_events_KeyEvent.h"
#include <shellscalingapi.h> // for PROCESS_DPI_AWARENESS

/*
 * Initialize the Java VM instance variable when the library is
 * first loaded
 */
static JavaVM *jvm;

JavaIDs javaIDs;

JavaVM* GetJVM()
{
    return jvm;
}

JNIEnv* GetEnv()
{
    void* env;
    jvm->GetEnv(&env, JNI_VERSION_1_2);
    return (JNIEnv*)env;
}

jboolean CheckAndClearException(JNIEnv* env)
{
    jthrowable t = env->ExceptionOccurred();
    if (!t) {
        return JNI_FALSE;
    }
    env->ExceptionClear();

    jclass cls = env->FindClass("com/sun/glass/ui/Application");
    if (env->ExceptionOccurred()) {
        env->ExceptionClear();
        return JNI_TRUE;
    }
    env->CallStaticVoidMethod(cls, javaIDs.Application.reportExceptionMID, t);
    if (env->ExceptionOccurred()) {
        env->ExceptionClear();
        return JNI_TRUE;
    }
    env->DeleteLocalRef(cls);

    return JNI_TRUE;
}

jint GetModifiers()
{
    jint modifiers = 0;
    if (HIBYTE(::GetKeyState(VK_CONTROL)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_CONTROL;
    }
    if (HIBYTE(::GetKeyState(VK_SHIFT)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_SHIFT;
    }
    if (HIBYTE(::GetKeyState(VK_MENU)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_ALT;
    }
    if (HIBYTE(::GetKeyState(VK_LWIN)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_WINDOWS;
    }
    if (HIBYTE(::GetKeyState(VK_RWIN)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_WINDOWS;
    }
    if (HIBYTE(::GetKeyState(VK_MBUTTON)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE;
    }
    if (HIBYTE(::GetKeyState(VK_RBUTTON)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY;
    }
    if (HIBYTE(::GetKeyState(VK_LBUTTON)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY;
    }
    if (HIBYTE(::GetKeyState(VK_XBUTTON1)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK;
    }
    if (HIBYTE(::GetKeyState(VK_XBUTTON2)) != 0) {
        modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD;
    }

    return modifiers;
}

/*
 * Returns the visible window bounds (excluding the DWM shadow / invisible resize borders) in the same
 * coordinate space that the rest of Glass expects: logical coordinates in the caller's DPI-awareness space.
 *
 * While DwmGetWindowAttribute(DWMWA_EXTENDED_FRAME_BOUNDS) reports the visible frame bounds in physical
 * coordinates (it is not DPI-virtualized for the caller), GetWindowRect() returns the window rectangle
 * in the caller's DPI-awareness space. If the caller is DPI-unaware or system-DPI-aware, Windows may
 * DPI-virtualize the returned coordinates.
 *
 * It is possible (though uncommon) for the JVM to run in a DPI-unaware or system-DPI-aware context.
 * For example, the JVM may be hosted in a DPI-unaware process, or the launcher (java.exe) may be forced
 * into DPI-unaware mode via compatibility settings (as of JDK9+, java.exe has an embedded manifest that
 * declares DPI awareness that can not be downgraded by code).
 *
 * If we cannot reliably map the visible window bounds into the caller's coordinate space, we fall back to
 * GetWindowRect() rather than risk returning incorrectly mapped bounds.
 */
BOOL GetExtendedFrameBounds(HWND hwnd, RECT* r) {

    struct impl_t {
        typedef HRESULT WINAPI FnGetProcessDpiAwareness(HANDLE, PROCESS_DPI_AWARENESS*);
        typedef DPI_AWARENESS_CONTEXT WINAPI FnSetThreadDpiAwarenessContext(DPI_AWARENESS_CONTEXT);
        typedef DPI_AWARENESS_CONTEXT WINAPI FnGetThreadDpiAwarenessContext(VOID);

        impl_t() {
            HMODULE hModule = GetModuleHandleW(L"user32.dll"); // user32 is already loaded

            pGetThreadDpiAwarenessContext = reinterpret_cast<FnGetThreadDpiAwarenessContext*>(
                GetProcAddress(hModule, "GetThreadDpiAwarenessContext"));

            pSetThreadDpiAwarenessContext = reinterpret_cast<FnSetThreadDpiAwarenessContext*>(
                GetProcAddress(hModule, "SetThreadDpiAwarenessContext"));

            // Only load GetProcessDpiAwareness if GetThreadDpiAwarenessContext is not available (pre-Win10).
            if (!pGetThreadDpiAwarenessContext) {
                wchar_t path[MAX_PATH];
                wchar_t file[MAX_PATH];

                UINT pathSize = sizeof(path) / sizeof(wchar_t);
                UINT rval = GetSystemDirectoryW(path, pathSize);
                if (rval == 0 || rval >= pathSize) {
                    fprintf(stderr, "BaseWnd: Failed to get system directory");
                    return;
                }

                HMODULE hModule;
                memcpy_s(file, sizeof(file), path, sizeof(path));
                if (wcscat_s(file, MAX_PATH-1, L"\\shcore.dll") != 0 || !(hModule = LoadLibraryW(file))) {
                    fprintf(stderr, "BaseWnd: Failed to load shcore.dll");
                    return;
                }

                pGetProcessDpiAwareness = reinterpret_cast<FnGetProcessDpiAwareness*>(
                    GetProcAddress(hModule, "GetProcessDpiAwareness"));
            }
        }

        FnGetProcessDpiAwareness* pGetProcessDpiAwareness = NULL;
        FnGetThreadDpiAwarenessContext* pGetThreadDpiAwarenessContext = NULL;
        FnSetThreadDpiAwarenessContext* pSetThreadDpiAwarenessContext = NULL;

        /*
         * We can only safely skip mapping when we know the current process is per-monitor DPI aware.
         * In that case, caller-space coordinates are already physical pixels, matching DWMWA_EXTENDED_FRAME_BOUNDS.
         * If we can't prove that we're PM-aware, we don't skip mapping.
         */
        BOOL canSkipMapping() const {
            // Supported on Windows 10+
            if (pGetThreadDpiAwarenessContext) {
                DPI_AWARENESS_CONTEXT currentAwareness = pGetThreadDpiAwarenessContext();

                return currentAwareness == DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE ||
                       currentAwareness == DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2;
            }

            // Supported on Windows 8.1+
            PROCESS_DPI_AWARENESS awareness;
            if (pGetProcessDpiAwareness && SUCCEEDED(pGetProcessDpiAwareness(NULL, &awareness))) {
                return awareness == PROCESS_PER_MONITOR_DPI_AWARE;
            }

            return FALSE;
        }

        /*
         * We sample GetWindowRect() in the current caller context ("callerBounds") and again while temporarily
         * switching the thread to a per-monitor aware context ("physBounds"). From these two rectangles we
         * derive an affine transform that maps physical pixels into the caller's virtualized coordinate space.
         */
        BOOL mapToCallerSpace(HWND hwnd, const RECT& extBounds, RECT* r) const {
            if (!pSetThreadDpiAwarenessContext) {
                return FALSE;
            }

            // callerBounds are in caller-space, and may be DPI-virtualized
            RECT callerBounds = {};
            if (!GetWindowRect(hwnd, &callerBounds)) {
                return FALSE;
            }

            // Switch thread to per-monitor DPI awareness to obtain non-virtualized ("physical") bounds.
            DPI_AWARENESS_CONTEXT oldAwareness =
                pSetThreadDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);

            if (!oldAwareness) {
                oldAwareness = pSetThreadDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE);
            }

            RECT physBounds = {};
            BOOL res = GetWindowRect(hwnd, &physBounds);

            // If SetThreadDpiAwarenessContext failed, oldAwareness will be NULL and physBounds is not a
            // reliable physical baseline; return to the fallback path.
            if (!oldAwareness) {
                return FALSE;
            }

            // Switch thread back to the original DPI awareness context.
            pSetThreadDpiAwarenessContext(oldAwareness);

            if (!res) {
                return FALSE;
            }

            // Derive an affine mapping from physical space to caller space:
            int physW = physBounds.right - physBounds.left;
            int physH = physBounds.bottom - physBounds.top;
            int callerW = callerBounds.right - callerBounds.left;
            int callerH = callerBounds.bottom - callerBounds.top;

            if (physW == 0 || physH == 0) {
                *r = callerBounds;
                return TRUE;
            }

            double sx = double(callerW) / double(physW);
            double sy = double(callerH) / double(physH);
            double ox = double(callerBounds.left) - double(physBounds.left) * sx;
            double oy = double(callerBounds.top) - double(physBounds.top) * sy;

            // Apply the mapping to the extended frame bounds to produce caller-space coordinates.
            r->left = (LONG)llround(double(extBounds.left) * sx + ox);
            r->top = (LONG)llround(double(extBounds.top) * sy + oy);
            r->right = (LONG)llround(double(extBounds.right) * sx + ox);
            r->bottom = (LONG)llround(double(extBounds.bottom) * sy + oy);
            return TRUE;
        }
    } static const impl;

    if (r == NULL) {
        return FALSE;
    }

    RECT extBounds = {};
    if (FAILED(DwmGetWindowAttribute(hwnd, DWMWA_EXTENDED_FRAME_BOUNDS, &extBounds, sizeof(extBounds)))) {
        return GetWindowRect(hwnd, r);
    }

    if (impl.canSkipMapping()) {
        *r = extBounds;
        return TRUE;
    }

    if (impl.mapToCallerSpace(hwnd, extBounds, r)) {
        return TRUE;
    }

    return GetWindowRect(hwnd, r);
}

extern "C" {

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL JNI_OnLoad_glass(JavaVM *vm, void *reserved)
#else
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
#endif
{
    memset(&javaIDs, 0, sizeof(javaIDs));
    jvm = vm;
    return JNI_VERSION_1_2;
}

} // extern "C"
