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

#include "GlassScreen.h"
#include "GlassApplication.h"

#include "com_sun_glass_ui_Screen.h"

static int g_nMonitorCounter = 0;
static int g_nMonitorLimit = 0;

static HMONITOR* g_hmpMonitors = NULL;

extern BOOL CALLBACK CountMonitorsCallback(HMONITOR hMon, HDC hDC, LPRECT rRect, LPARAM lP);
extern BOOL CALLBACK CollectMonitorsCallback(HMONITOR hMonitor, HDC hDC, LPRECT rRect, LPARAM lP);
extern BOOL CALLBACK FindDeepestMonitorCallback(HMONITOR hMonitor, HDC hDC, LPRECT rRect, LPARAM lP);

struct MonitorInfoStruct {
    jlong ptr;
    RECT rcMonitor;
    RECT rcWork;
    jint colorDepth;
    jfloat scale;
    jint dpiX;
    jint dpiY;
};

int CountMonitors()
{
    g_nMonitorCounter = 0;
    ::EnumDisplayMonitors(NULL, NULL, CountMonitorsCallback, 0L);
    return g_nMonitorCounter;
}

int CollectMonitors(int limit)
{
    g_nMonitorCounter = 0;
    g_nMonitorLimit = limit;
    ::EnumDisplayMonitors(NULL, NULL, CollectMonitorsCallback, 0L);
    return g_nMonitorCounter;
}


void GetMonitorSettings(HMONITOR hMonitor, MonitorInfoStruct *mis)
{
    MONITORINFOEX mix;
    memset(&mix, 0, sizeof(MONITORINFOEX));
    mix.cbSize = sizeof(MONITORINFOEX);

    mis->ptr = ptr_to_jlong(hMonitor);

    ::GetMonitorInfo(hMonitor, &mix);

    ::CopyRect(&mis->rcMonitor, &mix.rcMonitor);
    ::CopyRect(&mis->rcWork, &mix.rcWork);

    HDC hDC = ::CreateDC(TEXT("DISPLAY"), mix.szDevice, NULL, NULL);
    ASSERT(hDC);

    mis->colorDepth = ::GetDeviceCaps(hDC, BITSPIXEL) * ::GetDeviceCaps(hDC, PLANES);
    mis->dpiX = ::GetDeviceCaps(hDC, LOGPIXELSX); // pixels per inch
    mis->dpiY = ::GetDeviceCaps(hDC, LOGPIXELSY);
    mis->scale = (float)1.0; // On Windows we always render in physical pixels

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

jobject GlassScreen::CreateJavaMonitor(JNIEnv *env, HMONITOR monitor)
{
    jclass screenCls = GetScreenCls(env);

    if (javaIDs.Screen.init == NULL) {
        javaIDs.Screen.init = env->GetMethodID(screenCls, "<init>", "(JIIIIIIIIIIIF)V");
        ASSERT(javaIDs.Screen.init);
        if (CheckAndClearException(env)) return NULL;
    }

    MonitorInfoStruct mis;
    memset(&mis, 0, sizeof(MonitorInfoStruct));
    GetMonitorSettings(monitor, &mis);
    
    jobject gScn = env->NewObject(screenCls, javaIDs.Screen.init,
                          mis.ptr,

                          mis.colorDepth,
                          mis.rcMonitor.left,
                          mis.rcMonitor.top,
                          mis.rcMonitor.right - mis.rcMonitor.left,
                          mis.rcMonitor.bottom - mis.rcMonitor.top,
                          
                          mis.rcWork.left,
                          mis.rcWork.top,
                          mis.rcWork.right - mis.rcWork.left,
                          mis.rcWork.bottom - mis.rcWork.top,
                              
                          mis.dpiX,
                          mis.dpiY,
                          
                          mis.scale);
    if (CheckAndClearException(env)) return NULL;
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

jobjectArray GlassScreen::CreateJavaScreens(JNIEnv *env)
{
    int numMonitors = CountMonitors();
    g_hmpMonitors = (HMONITOR *)malloc(numMonitors * sizeof(HMONITOR));
    numMonitors = CollectMonitors(numMonitors);

    jclass screenCls = GetScreenCls(env);

    jobjectArray jScreens = env->NewObjectArray(numMonitors, screenCls, NULL);
    if (CheckAndClearException(env)) {
        free(g_hmpMonitors);
        return NULL;
    }

    int arrayIndex = 1;
    for (int i = 0; i < numMonitors; i++) {
        if (g_hmpMonitors[i] != NULL) {

            jobject jScreen = CreateJavaMonitor(env, g_hmpMonitors[i]);
            const POINT ptZero = { 0, 0 };

            //The primary monitor should be set to the 0 index
            if (g_hmpMonitors[i] == ::MonitorFromPoint(ptZero, MONITOR_DEFAULTTOPRIMARY)) {
                env->SetObjectArrayElement(jScreens, 0, jScreen);
            } else {
                env->SetObjectArrayElement(jScreens, arrayIndex, jScreen);
                arrayIndex++;
            }
            CheckAndClearException(env);
            env->DeleteLocalRef(jScreen);
        }
    }

    free(g_hmpMonitors);
    return jScreens;
}

////////////////////////////////////////////////////////////////////////////////////
//                               native callbacks
////////////////////////////////////////////////////////////////////////////////////

BOOL CALLBACK CountMonitorsCallback(HMONITOR hMon, HDC hDC, LPRECT rRect, LPARAM lP)
{
    g_nMonitorCounter++;
    return TRUE;
}

BOOL CALLBACK CollectMonitorsCallback(HMONITOR hMonitor, HDC hDC, LPRECT rRect, LPARAM lP)
{
    if ((g_nMonitorCounter < g_nMonitorLimit) && (g_hmpMonitors != NULL)) {
        g_hmpMonitors[g_nMonitorCounter] = hMonitor;
        g_nMonitorCounter++;
    }
    return TRUE;
}
