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

#include "GlassScreen.h"
#include "GlassApplication.h"

#include "com_sun_glass_ui_Screen.h"

extern BOOL CALLBACK CountMonitorsCallback(HMONITOR hMon, HDC hDC, LPRECT rRect, LPARAM lP);
extern BOOL CALLBACK CollectMonitorsCallback(HMONITOR hMonitor, HDC hDC, LPRECT rRect, LPARAM lP);

enum AnchorType {
    ANCHOR_TO_LEFT,
    ANCHOR_TO_TOP,
    ANCHOR_TO_RIGHT,
    ANCHOR_TO_BOTTOM
};

struct MonitorInfoStruct {
    HMONITOR hMonitor;
    RECT rcMonitor;
    RECT rcWork;
    RECT fxMonitor;
    RECT fxWork;
    jboolean primaryScreen;
    jint colorDepth;
    jfloat uiScale;
    jfloat renderScale;
    jint dpiX;
    jint dpiY;
    jint anchoredInPass;
    jobject gScreen;
};
extern jobject CreateJavaMonitorFromMIS(JNIEnv *env, MonitorInfoStruct *pMIS);

struct {
    int numInfos;
    int maxInfos;
    MonitorInfoStruct *pMonitorInfos;
} g_MonitorInfos;

typedef enum _Monitor_DPI_Type { 
    MDT_Effective_DPI  = 0,
    MDT_Angular_DPI    = 1,
    MDT_Raw_DPI        = 2,
    MDT_Default        = MDT_Effective_DPI
} Monitor_DPI_Type;

typedef enum _Process_DPI_Awareness {
    Process_DPI_Unaware            = 0,
    Process_System_DPI_Aware       = 1,
    Process_Per_Monitor_DPI_Aware  = 2
} Process_DPI_Awareness;

#undef DEBUG_DPI

BOOL triedToFindDPIFuncs = FALSE;
typedef HRESULT WINAPI FnGetDPIForMonitor(HMONITOR hmonitor, Monitor_DPI_Type dpiType, UINT *dpiX, UINT *dpiY);
typedef HRESULT WINAPI FnGetProcessDPIAwareness(HANDLE hprocess, Process_DPI_Awareness *value);
typedef HRESULT WINAPI FnSetProcessDPIAwareness(Process_DPI_Awareness value);
FnGetDPIForMonitor * pGetDPIForMonitor = 0;
FnGetProcessDPIAwareness * pGetProcessDPIAwareness = 0;
FnSetProcessDPIAwareness * pSetProcessDPIAwareness = 0;

void GlassScreen::LoadDPIFuncs(jint awareRequested)
{
    if (triedToFindDPIFuncs) {
        return;
    }
    triedToFindDPIFuncs = TRUE;
    wchar_t path[MAX_PATH];
    HMODULE hLibSHCore = 0;
    if (::GetSystemDirectory(path, sizeof(path) / sizeof(wchar_t)) != 0) {
        wcscat_s(path, MAX_PATH-1, L"\\SHCore.dll");
        hLibSHCore = ::LoadLibrary(path);
    }
    if (hLibSHCore) {
        pGetProcessDPIAwareness = (FnGetProcessDPIAwareness*)GetProcAddress(hLibSHCore, "GetProcessDpiAwareness");
        pSetProcessDPIAwareness = (FnSetProcessDPIAwareness*)GetProcAddress(hLibSHCore, "SetProcessDpiAwareness");
        pGetDPIForMonitor = (FnGetDPIForMonitor*)GetProcAddress(hLibSHCore, "GetDpiForMonitor");
        if (!pGetProcessDPIAwareness || !pSetProcessDPIAwareness || !pGetDPIForMonitor) {
            pGetProcessDPIAwareness = 0;
            pSetProcessDPIAwareness = 0;
            pGetDPIForMonitor = 0;
        }
    } else {
#ifdef DEBUG_DPI
        fprintf(stderr, "Could not find libSHCore.dll\n");
#endif /* DEBUG_DPI */
    }
    if (pSetProcessDPIAwareness) {
        HRESULT res = (*pSetProcessDPIAwareness)((Process_DPI_Awareness) awareRequested);
#ifdef DEBUG_DPI
        if (res != S_OK) {
            if (res == E_ACCESSDENIED) {
                fprintf(stderr, "Process DPI awareness already set! (by application manifest or prior call)\n");
            } else {
                fprintf(stderr, "SetProcessDpiAwareness(%d) returned (0x%08x)\n", awareRequested, res);
            }
        }
#endif /* DEBUG_DPI */
    } else {
        BOOL ok = ::SetProcessDPIAware();
#ifdef DEBUG_DPI
        fprintf(stderr, "Could not find SetProcessDpiAwareness function, SetProcessDPIAware returned %d\n", ok);
#endif /* DEBUG_DPI */
    }
    if (pGetProcessDPIAwareness) {
        Process_DPI_Awareness awareness;
        HRESULT res = (*pGetProcessDPIAwareness)(NULL, &awareness);
#ifdef DEBUG_DPI
        if (res != S_OK) {
            fprintf(stderr, "Unable to query process DPI Awareness (0x%08X)\n", res);
        } else {
            char *awareDescription;
            if (awareness == Process_DPI_Unaware) {
                awareDescription = "DPI Unaware";
            } else if (awareness == Process_System_DPI_Aware) {
                awareDescription = "System DPI aware (legacy)";
            } else if (awareness == Process_Per_Monitor_DPI_Aware) {
                awareDescription = "Per Monitor (dynamic) DPI aware (best)";
            } else {
                awareDescription = "Unknown awareness value";
            }
            fprintf(stderr, "ProcessDPIAwareness = %d [%s]\n", awareness, awareDescription);
        }
#endif /* DEBUG_DPI */
    } else {
#ifdef DEBUG_DPI
        fprintf(stderr, "Could not find GetProcessDpiAwareness function\n");
#endif /* DEBUG_DPI */
    }
}

void GetMonitorSettings(HMONITOR hMonitor, MonitorInfoStruct *mis)
{
    if (!triedToFindDPIFuncs) {
#ifdef DEBUG_DPI
        fprintf(stderr, "Monitor settings queried before DPI functions initialized!\n");
#endif /* DEBUG_DPI */
        GlassScreen::LoadDPIFuncs(Process_Per_Monitor_DPI_Aware);
    }

    MONITORINFOEX mix;
    memset(&mix, 0, sizeof(MONITORINFOEX));
    mix.cbSize = sizeof(MONITORINFOEX);

    mis->hMonitor = hMonitor;

    ::GetMonitorInfo(hMonitor, &mix);

    ::CopyRect(&mis->rcMonitor, &mix.rcMonitor);
    ::CopyRect(&mis->rcWork, &mix.rcWork);
#ifdef DEBUG_DPI
    fprintf(stderr, "raw monitor bounds = (%d, %d, %d, %d)\n",
            mis->rcMonitor.left, mis->rcMonitor.top,
            mis->rcMonitor.right, mis->rcMonitor.bottom);
    fprintf(stderr, "raw monitor working bounds = (%d, %d, %d, %d)\n",
            mis->rcWork.left, mis->rcWork.top,
            mis->rcWork.right, mis->rcWork.bottom);
#endif /* DEBUG_DPI */

    HDC hDC = ::CreateDC(TEXT("DISPLAY"), mix.szDevice, NULL, NULL);
    ASSERT(hDC);

    mis->primaryScreen = ((mix.dwFlags & MONITORINFOF_PRIMARY) != 0) ? JNI_TRUE : JNI_FALSE;
    mis->colorDepth = ::GetDeviceCaps(hDC, BITSPIXEL) * ::GetDeviceCaps(hDC, PLANES);
    UINT resx, resy;
    UINT uires;
    if (pGetDPIForMonitor) {
        // If we can use the GetDPIForMonitor function, then its Effective
        // value will tell us how much we should scale ourselves based on
        // all system settings, and its Raw value will tell us exactly how
        // many pixels per inch there are.  The Effective value can be
        // affected by user preference, accessibility settings, monitor
        // size, and resolution all computed by the system into a single
        // value that all applications should scale themselves by.
#ifdef DEBUG_DPI
        fprintf(stderr, "logpixelsX,Y = %d, %d\n",
                ::GetDeviceCaps(hDC, LOGPIXELSX),
                ::GetDeviceCaps(hDC, LOGPIXELSY));
#endif /* DEBUG_DPI */
        HRESULT res = (*pGetDPIForMonitor)(hMonitor, MDT_Effective_DPI, &resx, &resy);
#ifdef DEBUG_DPI
        fprintf(stderr, "effective DPI X,Y = [0x%08x] %d, %d\n", res, resx, resy);
#endif /* DEBUG_DPI */
        if (res != S_OK) {
            resx = ::GetDeviceCaps(hDC, LOGPIXELSX);
            resy = ::GetDeviceCaps(hDC, LOGPIXELSY);
        }
        uires = resx;
        res = (*pGetDPIForMonitor)(hMonitor, MDT_Raw_DPI, &resx, &resy);
#ifdef DEBUG_DPI
        fprintf(stderr, "raw DPI X,Y = [0x%08x] %d, %d\n", res, resx, resy);
#endif /* DEBUG_DPI */
    } else {
        resx = ::GetDeviceCaps(hDC, LOGPIXELSX);
        resy = ::GetDeviceCaps(hDC, LOGPIXELSY);
#ifdef DEBUG_DPI
        fprintf(stderr, "logpixelsX,Y = %d, %d\n", resx, resy);
#endif /* DEBUG_DPI */
        uires = resx;
    }
    mis->dpiX = resx;
    mis->dpiY = resy;
    mis->uiScale = GlassApplication::GetUIScale(uires);
    mis->renderScale = GlassApplication::getRenderScale(mis->uiScale);

    ::DeleteDC(hDC);
}

jclass GetScreenCls(JNIEnv *env)
{
    static jclass screenCls = NULL;
    if (!screenCls) {
        jclass cls = GlassApplication::ClassForName(env, "com.sun.glass.ui.Screen");
        ASSERT(cls);
        screenCls = (jclass)env->NewGlobalRef(cls);
        env->DeleteLocalRef(cls);
    }
    return screenCls;
}

void anchor(MonitorInfoStruct *pMIS, int pass);

jobject GlassScreen::GetJavaMonitor(JNIEnv *env, HMONITOR monitor)
{
    for (int i = 0; i < g_MonitorInfos.numInfos; i++) {
        MonitorInfoStruct *pMIS = &g_MonitorInfos.pMonitorInfos[i];
        if (pMIS->hMonitor == monitor) {
            return pMIS->gScreen;
        }
    }

#ifdef DEBUG_DPI
    fprintf(stderr, "MONITOR NOT FOUND - making a new Java Screen object in isolation!\n");
#endif /* DEBUG_DPI */

    MonitorInfoStruct mis;
    memset(&mis, 0, sizeof(MonitorInfoStruct));
    GetMonitorSettings(monitor, &mis);
    anchor(&mis, 0);

    jobject gScreen = CreateJavaMonitorFromMIS(env, &mis);
    // The return value (gScreen) is a local ref in addition to the global
    // ref stored in the mis.gScreen field.  We should not leave the global
    // ref laying around in this case because we are not saving the "mis"
    // structure.
    env->DeleteGlobalRef(mis.gScreen);
    return gScreen;
}

jobject CreateJavaMonitorFromMIS(JNIEnv *env, MonitorInfoStruct *pMIS)
{
    jclass screenCls = GetScreenCls(env);

    if (javaIDs.Screen.init == NULL) {
        javaIDs.Screen.init = env->GetMethodID(screenCls, "<init>", "(JIIIIIIIIIIIFF)V");
        ASSERT(javaIDs.Screen.init);
        if (CheckAndClearException(env)) return NULL;
    }

    jobject gScn = env->NewObject(screenCls, javaIDs.Screen.init,
                          ptr_to_jlong(pMIS->hMonitor),

                          pMIS->colorDepth,
                          pMIS->fxMonitor.left,
                          pMIS->fxMonitor.top,
                          pMIS->fxMonitor.right  - pMIS->fxMonitor.left,
                          pMIS->fxMonitor.bottom - pMIS->fxMonitor.top,

                          pMIS->fxWork.left,
                          pMIS->fxWork.top,
                          pMIS->fxWork.right  - pMIS->fxWork.left,
                          pMIS->fxWork.bottom - pMIS->fxWork.top,
                              
                          pMIS->dpiX,
                          pMIS->dpiY,
                          
                          pMIS->uiScale,
                          pMIS->renderScale);
    if (CheckAndClearException(env)) return NULL;
    pMIS->gScreen = env->NewGlobalRef(gScn);
    return gScn;
}

void GlassScreen::HandleDisplayChange()
{
    JNIEnv *env = GetEnv();

    jclass screenCls = GetScreenCls(env);

    if (javaIDs.Screen.notifySettingsChanged == NULL) {
        javaIDs.Screen.notifySettingsChanged
             = env->GetStaticMethodID(screenCls, "notifySettingsChanged", "()V");
        ASSERT(javaIDs.Screen.notifySettingsChanged);
        if (CheckAndClearException(env)) return;
    }

    env->CallStaticVoidMethod(screenCls, javaIDs.Screen.notifySettingsChanged);
    CheckAndClearException(env);
}

jfloat distSqTo(RECT rect, jfloat x, jfloat y)
{
    jfloat relx = x - (rect.left + rect.right) * 0.5f;
    jfloat rely = y - (rect.top + rect.bottom) * 0.5f;
    return relx * relx + rely * rely;
}

void convert(RECT from, RECT to, jfloat *pX, jfloat *pY)
{
    jfloat t;
    t = (*pX - from.left) / (from.right - from.left);
    *pX = to.left + t * (to.right - to.left);
    t = (*pY - from.top) / (from.bottom - from.top);
    *pY = to.top + t * (to.bottom - to.top);
}

BOOL GlassScreen::FX2Win(jfloat* pX, jfloat* pY)
{
    if (g_MonitorInfos.numInfos == 0) return FALSE;
    int monIndex = 0;
    jfloat distSq = distSqTo(g_MonitorInfos.pMonitorInfos[0].fxMonitor, *pX, *pY);
    for (int i = 1; i < g_MonitorInfos.numInfos; i++) {
        jfloat d = distSqTo(g_MonitorInfos.pMonitorInfos[i].fxMonitor, *pX, *pY);
        if (d < distSq) {
            distSq = d;
            monIndex = i;
        }
    }
    convert(g_MonitorInfos.pMonitorInfos[monIndex].fxMonitor,
            g_MonitorInfos.pMonitorInfos[monIndex].rcMonitor,
            pX, pY);
    return TRUE;
}

BOOL GlassScreen::Win2FX(jfloat* pX, jfloat* pY)
{
    if (g_MonitorInfos.numInfos == 0) return FALSE;
    int monIndex = 0;
    jfloat distSq = distSqTo(g_MonitorInfos.pMonitorInfos[0].rcMonitor, *pX, *pY);
    for (int i = 1; i < g_MonitorInfos.numInfos; i++) {
        jfloat d = distSqTo(g_MonitorInfos.pMonitorInfos[i].rcMonitor, *pX, *pY);
        if (d < distSq) {
            distSq = d;
            monIndex = i;
        }
    }
    convert(g_MonitorInfos.pMonitorInfos[monIndex].rcMonitor,
            g_MonitorInfos.pMonitorInfos[monIndex].fxMonitor,
            pX, pY);
    return TRUE;
}

void anchorTo(MonitorInfoStruct *pMIS,
              jint fxX, jboolean xBefore,
              jint fxY, jboolean yBefore,
              jint pass)
{
    jint monX = pMIS->rcMonitor.left;
    jint monY = pMIS->rcMonitor.top;
    jint monW = pMIS->rcMonitor.right  - monX;
    jint monH = pMIS->rcMonitor.bottom - monY;
    jint wrkL = pMIS->rcWork   .left   - monX;
    jint wrkT = pMIS->rcWork   .top    - monY;
    jint wrkR = pMIS->rcWork   .right  - monX;
    jint wrkB = pMIS->rcWork   .bottom - monY;
    jfloat scale = pMIS->uiScale;
    if (scale > 1.0f) {
        pMIS->dpiX = (jint) floorf((pMIS->dpiX / scale) + 0.5f);
        pMIS->dpiY = (jint) floorf((pMIS->dpiY / scale) + 0.5f);
        monW = (jint) floorf((monW / scale) + 0.5f);
        monH = (jint) floorf((monH / scale) + 0.5f);
        wrkL = (jint) floorf((wrkL / scale) + 0.5f);
        wrkT = (jint) floorf((wrkT / scale) + 0.5f);
        wrkR = (jint) floorf((wrkR / scale) + 0.5f);
        wrkB = (jint) floorf((wrkB / scale) + 0.5f);
    }

    if (xBefore) fxX -= monW;
    if (yBefore) fxY -= monH;    
    pMIS->fxMonitor.left   = fxX;
    pMIS->fxMonitor.top    = fxY;
    pMIS->fxMonitor.right  = fxX + monW;
    pMIS->fxMonitor.bottom = fxY + monH;
    pMIS->fxWork   .left   = fxX + wrkL;
    pMIS->fxWork   .top    = fxY + wrkT;
    pMIS->fxWork   .right  = fxX + wrkR;
    pMIS->fxWork   .bottom = fxY + wrkB;
    pMIS->anchoredInPass = pass;
}

void anchor(MonitorInfoStruct *pMIS, int pass) {
    anchorTo(pMIS,
             pMIS->rcMonitor.left, JNI_FALSE,
             pMIS->rcMonitor.top, JNI_FALSE,
             pass);
}

jint originOffsetFromRanges(jint aV0, jint aV1,
                            jint mV0, jint mV1,
                            jfloat aScale, jfloat mScale)
{
    jint v0 = (aV0 > mV0) ? aV0 : mV0;
    jint v1 = (aV1 < mV1) ? aV1 : mV1;
    jfloat mid = (v0 + v1) / 2.0f;
    jfloat rel = (mid - aV0) / aScale - (mid - mV0) / mScale;
    return (jint) floorf(rel + 0.5f);
}

void anchorH(MonitorInfoStruct *pAnchor, MonitorInfoStruct *pMon,
             jboolean before, jint pass)
{
    int x = before ? pAnchor->fxMonitor.left : pAnchor->fxMonitor.right;
    int yoff = originOffsetFromRanges(pAnchor->rcMonitor.top, pAnchor->rcMonitor.bottom,
                                      pMon->rcMonitor.top, pMon->rcMonitor.bottom,
                                      pAnchor->uiScale, pMon->uiScale);
    int y = pAnchor->fxMonitor.top + yoff;
    anchorTo(pMon, x, before, y, false, pass);
}

void anchorV(MonitorInfoStruct *pAnchor, MonitorInfoStruct *pMon,
             jboolean before, jint pass)
{
    int xoff = originOffsetFromRanges(pAnchor->rcMonitor.left, pAnchor->rcMonitor.right,
                                      pMon->rcMonitor.left, pMon->rcMonitor.right,
                                      pAnchor->uiScale, pMon->uiScale);
    int x = pAnchor->fxMonitor.left + xoff;
    int y = before ? pAnchor->fxMonitor.top : pAnchor->fxMonitor.bottom;
    anchorTo(pMon, x, false, y, before, pass);
}

BOOL touchesLeft(MonitorInfoStruct *pMISa, MonitorInfoStruct *pMISb) {
    return (pMISa->rcMonitor.left  == pMISb->rcMonitor.right &&
            pMISa->rcMonitor.top    < pMISb->rcMonitor.bottom &&
            pMISa->rcMonitor.bottom > pMISb->rcMonitor.top);
}

BOOL touchesAbove(MonitorInfoStruct *pMISa, MonitorInfoStruct *pMISb) {
    return (pMISa->rcMonitor.top  == pMISb->rcMonitor.bottom &&
            pMISa->rcMonitor.left  < pMISb->rcMonitor.right &&
            pMISa->rcMonitor.right > pMISb->rcMonitor.left);
}

void propagateAnchors(MonitorInfoStruct *pMIS, jint pass) {
    for (int i = 0; i < g_MonitorInfos.numInfos; i++) {
        MonitorInfoStruct *pMIS2 = &g_MonitorInfos.pMonitorInfos[i];
        if (pMIS2->anchoredInPass != 0) continue;
        if (touchesLeft(pMIS2, pMIS)) {
            anchorH(pMIS, pMIS2, JNI_FALSE, pass);
        } else if (touchesLeft(pMIS, pMIS2)) {
            anchorH(pMIS, pMIS2, JNI_TRUE, pass);
        } else if (touchesAbove(pMIS2, pMIS)) {
            anchorV(pMIS, pMIS2, JNI_FALSE, pass);
        } else if (touchesAbove(pMIS, pMIS2)) {
            anchorV(pMIS, pMIS2, JNI_TRUE, pass);
        }
    }
}

jobjectArray GlassScreen::CreateJavaScreens(JNIEnv *env)
{
    if (g_MonitorInfos.maxInfos > 0) {
        int numMonitors = g_MonitorInfos.numInfos;
        for (int i = 0; i < numMonitors; i++) {
            MonitorInfoStruct *pMIS = &g_MonitorInfos.pMonitorInfos[i];
            if (pMIS->gScreen != NULL) {
                env->DeleteGlobalRef(pMIS->gScreen);
            }
        }
        free(g_MonitorInfos.pMonitorInfos);
        g_MonitorInfos.numInfos = g_MonitorInfos.maxInfos = 0;
        g_MonitorInfos.pMonitorInfos = NULL;
    }

    g_MonitorInfos.maxInfos = 0;
    ::EnumDisplayMonitors(NULL, NULL, CountMonitorsCallback, 0L);
    int numMonitors = g_MonitorInfos.maxInfos;

#ifdef DEBUG_DPI
    fprintf(stderr, "numMonitors = %d\n", numMonitors);
#endif /* DEBUG_DPI */

    g_MonitorInfos.numInfos = 0;
    g_MonitorInfos.pMonitorInfos =
            (MonitorInfoStruct *)malloc(numMonitors * sizeof(MonitorInfoStruct));
    memset(g_MonitorInfos.pMonitorInfos, 0, numMonitors * sizeof(MonitorInfoStruct));
    ::EnumDisplayMonitors(NULL, NULL, CollectMonitorsCallback, 0L);
    numMonitors = g_MonitorInfos.numInfos;
    if (numMonitors <= 0) {
        return NULL;
    }

    //The primary monitor should be set to the 0 index
    int primaryIndex = 0;
    for (int i = 0; i < numMonitors; i++) {
        MonitorInfoStruct *pMIS = &g_MonitorInfos.pMonitorInfos[i];
        if (pMIS->rcMonitor.left  <= 0 &&
            pMIS->rcMonitor.top   <= 0 &&
            pMIS->rcMonitor.right  > 0 &&
            pMIS->rcMonitor.bottom > 0)
        {
            primaryIndex = i;
            break;
        } else if (pMIS->primaryScreen) {
            primaryIndex = i;
        }
    }
    // Swap the primary monitor to the 0 index
    if (primaryIndex > 0) {
        MonitorInfoStruct tmpMIS = g_MonitorInfos.pMonitorInfos[primaryIndex];
        g_MonitorInfos.pMonitorInfos[primaryIndex] = g_MonitorInfos.pMonitorInfos[0];
        g_MonitorInfos.pMonitorInfos[0] = tmpMIS;
    }

    // Anchor the primary screen.
    // Then loop, propagating the geometry of the primary screen to its
    // neighbors first and then each screen in preference to how closely
    // it was anchored to the primary screen.
    // If all propagations are done and we still have unanchored screens,
    // choose the lowest such screen in the list and anchor it, repeating
    // the propagation process until all screens are anchored, preferably
    // to each other, but in isolated groups as well if necessary.
    int pass = 1;
    anchor(&g_MonitorInfos.pMonitorInfos[0], pass);
    do {
        jboolean foundUnpropagated = JNI_FALSE;
        for (int i = 0; i < numMonitors; i++) {
            MonitorInfoStruct *pMIS = &g_MonitorInfos.pMonitorInfos[i];
            if (pMIS->anchoredInPass == pass) {
                foundUnpropagated = JNI_TRUE;
                propagateAnchors(pMIS, pass+1);
            }
        }
        if (foundUnpropagated) {
            pass++;
        } else {
            jboolean foundUnanchored = JNI_FALSE;
            for (int i = 0; i < numMonitors; i++) {
                MonitorInfoStruct *pMIS = &g_MonitorInfos.pMonitorInfos[i];
                if (pMIS->anchoredInPass == 0) {
                    foundUnanchored = JNI_TRUE;
                    anchor(pMIS, pass);
                    break;
                }
            }
            if (!foundUnanchored) break;
            // Loop back without incrementing "pass" so that we propagate
            // the screen that we just anchored above.
        }
    } while (JNI_TRUE);

    jclass screenCls = GetScreenCls(env);

    jobjectArray jScreens = env->NewObjectArray(numMonitors, screenCls, NULL);
    if (CheckAndClearException(env)) {
        free(g_MonitorInfos.pMonitorInfos);
        g_MonitorInfos.numInfos = g_MonitorInfos.maxInfos = 0;
        g_MonitorInfos.pMonitorInfos = NULL;
        return NULL;
    }

    int arrayIndex = 1;
    for (int i = 0; i < numMonitors; i++) {
        MonitorInfoStruct *pMIS = &g_MonitorInfos.pMonitorInfos[i];
        HMONITOR hMonitor = pMIS->hMonitor;
        jobject jScreen = CreateJavaMonitorFromMIS(env, pMIS);
        env->SetObjectArrayElement(jScreens, i, jScreen);
        CheckAndClearException(env);
        env->DeleteLocalRef(jScreen);
    }

    return jScreens;
}

////////////////////////////////////////////////////////////////////////////////////
//                               native callbacks
////////////////////////////////////////////////////////////////////////////////////

BOOL CALLBACK CountMonitorsCallback(HMONITOR hMon, HDC hDC, LPRECT rRect, LPARAM lP)
{
    g_MonitorInfos.maxInfos++;
    return TRUE;
}

BOOL CALLBACK CollectMonitorsCallback(HMONITOR hMonitor, HDC hDC, LPRECT rRect, LPARAM lP)
{
    if ((hMonitor != NULL) &&
        (g_MonitorInfos.numInfos < g_MonitorInfos.maxInfos) &&
        (g_MonitorInfos.pMonitorInfos != NULL))
    {
        GetMonitorSettings(hMonitor, &(g_MonitorInfos.pMonitorInfos[g_MonitorInfos.numInfos]));
        g_MonitorInfos.numInfos++;
    }
    return TRUE;
}
