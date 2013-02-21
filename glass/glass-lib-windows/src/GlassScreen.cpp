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
#include "com_sun_glass_ui_win_WinScreen.h"


static jmethodID midNotifySettingsChanged = NULL;

static int g_nMonitorCounter = 0;
static int g_nMonitorLimit = 0;
static int g_nDeepestColorDepth = 0;

static HMONITOR* g_hmpMonitors = NULL;
static HMONITOR g_hDeepestMonitor;
static HMONITOR g_hPrimaryMonitor;

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

int FindDeepestMonitor(int limit)
{
    g_nMonitorCounter = 0;
    g_nMonitorLimit = limit;
    g_nDeepestColorDepth = 0;
    ::EnumDisplayMonitors(NULL, NULL, FindDeepestMonitorCallback, 0L);
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

void CopyMonitorSettingsToJava(JNIEnv *env, jobject jScreen, MonitorInfoStruct *mis)
{
    jclass cls = GlassApplication::ClassForName(env, "com.sun.glass.ui.Screen");
    ASSERT(cls);

    env->SetLongField(jScreen, env->GetFieldID(cls, "ptr", "J"), mis->ptr);

    env->SetIntField(jScreen, env->GetFieldID(cls, "x", "I"), mis->rcMonitor.left);
    env->SetIntField(jScreen, env->GetFieldID(cls, "y", "I"), mis->rcMonitor.top);
    env->SetIntField(jScreen, env->GetFieldID(cls, "width", "I"), mis->rcMonitor.right - mis->rcMonitor.left);
    env->SetIntField(jScreen, env->GetFieldID(cls, "height", "I"), mis->rcMonitor.bottom - mis->rcMonitor.top);

    env->SetIntField(jScreen, env->GetFieldID(cls, "visibleX", "I"), mis->rcWork.left);
    env->SetIntField(jScreen, env->GetFieldID(cls, "visibleY", "I"), mis->rcWork.top);
    env->SetIntField(jScreen, env->GetFieldID(cls, "visibleWidth", "I"), mis->rcWork.right - mis->rcWork.left);
    env->SetIntField(jScreen, env->GetFieldID(cls, "visibleHeight", "I"), mis->rcWork.bottom - mis->rcWork.top);

    env->SetIntField(jScreen, env->GetFieldID(cls, "depth", "I"), mis->colorDepth);
    env->SetIntField(jScreen, env->GetFieldID(cls, "resolutionX", "I"), mis->dpiX);
    env->SetIntField(jScreen, env->GetFieldID(cls, "resolutionY", "I"), mis->dpiY);
    env->SetFloatField(jScreen, env->GetFieldID(cls, "scale", "F"), mis->scale);
}

void GlassScreen::HandleDisplayChange()
{
    JNIEnv *env = GetEnv();

    jclass cls = GlassApplication::ClassForName(env, "com.sun.glass.ui.Screen");
    ASSERT(cls);
    env->CallStaticVoidMethod(cls, midNotifySettingsChanged);
    CheckAndClearException(env);
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

BOOL CALLBACK FindDeepestMonitorCallback(HMONITOR hMonitor, HDC hDC, LPRECT rRect, LPARAM lP)
{
    if (g_nMonitorCounter < g_nMonitorLimit) {
        MonitorInfoStruct mis;
        memset(&mis, 0, sizeof(MonitorInfoStruct));
        GetMonitorSettings(hMonitor, &mis);
        if (mis.colorDepth > g_nDeepestColorDepth) {
            g_nDeepestColorDepth = mis.colorDepth;
            g_hDeepestMonitor = hMonitor;
        }
        g_nMonitorCounter++;
    }
    return TRUE;
}

/*
 * Class:     com_sun_glass_ui_win_WinScreen
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinScreen__1initIDs
  (JNIEnv *env, jclass jScreenClass)
{
    jclass cls = env->FindClass("com/sun/glass/ui/Screen");
    ASSERT(cls);

    midNotifySettingsChanged = env->GetStaticMethodID(cls, "notifySettingsChanged", "()V");
    ASSERT(midNotifySettingsChanged);


    cls = env->FindClass("java/util/List");
    ASSERT(cls);
    javaIDs.List.add = env->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");
    ASSERT(javaIDs.List.add);
}

/*
 * Class:     com_sun_glass_ui_win_WinScreen
 * Method:    _getDeepestScreen
 * Signature: (Lcom/sun/glass/ui/Screen;)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinScreen__1getDeepestScreen
  (JNIEnv *env, jclass jScreenClass, jobject jScreen)
{
    int numMonitors = CountMonitors();
    FindDeepestMonitor(numMonitors);

    MonitorInfoStruct mis;
    memset(&mis, 0, sizeof(MonitorInfoStruct));
    GetMonitorSettings(g_hDeepestMonitor, &mis);
    CopyMonitorSettingsToJava(env, jScreen, &mis);

    return jScreen;
}

/*
 * Class:     com_sun_glass_ui_win_WinScreen
 * Method:    _getMainScreen
 * Signature: (Lcom/sun/glass/ui/Screen;)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinScreen__1getMainScreen
  (JNIEnv *env, jclass jScreenClass, jobject jScreen)
{
    const POINT ptZero = { 0, 0 }; // primary monitor has its upper left corner at (0, 0)
    HMONITOR hMonitor = ::MonitorFromPoint(ptZero, MONITOR_DEFAULTTOPRIMARY);

    MonitorInfoStruct mis;
    memset(&mis, 0, sizeof(MonitorInfoStruct));
    GetMonitorSettings(hMonitor, &mis);
    CopyMonitorSettingsToJava(env, jScreen, &mis);

    return jScreen;
}

/*
 * Class:     com_sun_glass_ui_win_WinScreen
 * Method:    _getScreenForLocation
 * Signature: (Lcom/sun/glass/ui/Screen;II)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinScreen__1getScreenForLocation
  (JNIEnv *env, jclass jScreenClass, jobject jScreen, jint x, jint y)
{
    POINT p = {x, y};
    HMONITOR hMonitor = ::MonitorFromPoint(p, MONITOR_DEFAULTTOPRIMARY);

    MonitorInfoStruct mis;
    memset(&mis, 0, sizeof(MonitorInfoStruct));
    GetMonitorSettings(hMonitor, &mis);
    CopyMonitorSettingsToJava(env, jScreen, &mis);

    return jScreen;
}

/*
 * Class:     com_sun_glass_ui_win_WinScreen
 * Method:    _getScreenForPtr
 * Signature: (Lcom/sun/glass/ui/Screen;J)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinScreen__1getScreenForPtr
  (JNIEnv *env, jclass jScreenClass, jobject jScreen, jlong screenPtr)
{
    HMONITOR hMonitor = (HMONITOR)jlong_to_ptr(screenPtr);

    MonitorInfoStruct mis;
    memset(&mis, 0, sizeof(MonitorInfoStruct));
    GetMonitorSettings(hMonitor, &mis);
    CopyMonitorSettingsToJava(env, jScreen, &mis);

    return jScreen;
}

/*
 * Class:     com_sun_glass_ui_win_WinScreen
 * Method:    _getScreens
 * Signature: (Ljava/util/List;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_win_WinScreen__1getScreens
  (JNIEnv *env, jclass jScreenClass, jobject jScreens)
{
    int numMonitors = CountMonitors();
    g_hmpMonitors = (HMONITOR *)malloc(numMonitors * sizeof(HMONITOR));
    numMonitors = CollectMonitors(numMonitors);

    jclass cls = GlassApplication::ClassForName(env, "com.sun.glass.ui.Screen");
    ASSERT(cls);

    for (int i = 0; i < numMonitors; i++) {
        if (g_hmpMonitors[i] != NULL) {
            jobject jScreen = env->NewObject(cls, env->GetMethodID(cls, "<init>", "()V"));
            if (!CheckAndClearException(env)) {
                MonitorInfoStruct mis;
                memset(&mis, 0, sizeof(MonitorInfoStruct));
                GetMonitorSettings(g_hmpMonitors[i], &mis);
                CopyMonitorSettingsToJava(env, jScreen, &mis);

                env->CallBooleanMethod(jScreens, javaIDs.List.add, jScreen);
                CheckAndClearException(env);
                env->DeleteLocalRef(jScreen);
            }
        }
    }

    free(g_hmpMonitors);
    return jScreens;
}
