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
 
#include "LensCommon.h"
#include "wm/LensWindowManager.h"
#include "com_sun_glass_ui_lens_LensApplication.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_events_WindowEvent.h"

#include <signal.h>

//********************************************************

// JNI handles ******************************************

jclass jScreenClass;

//Application.java
static jclass jApplicationClass;

//Application class is a singleton, therefore there is no problem to cache it
static jobject pApplication = NULL;

//LensWindow
jclass jLensWindowClass;
//LensApplication
jclass jLensApplicationClass;
static jmethodID jLensApplication_createNativeEventThread;
jmethodID jLensApplication_waitEventLoopsToFinish;
static jmethodID jLensApplication_notifyKeyEvent;
static jmethodID jLensApplication_notifyMouseEvent;
static jmethodID jLensApplication_notifyScrollEvent;
static jmethodID jLensApplication_notifyTouchEvent;
static jmethodID jLensApplication_notifyWindowResize;
static jmethodID jLensApplication_notifyWindowMove;
static jmethodID jLensApplication_notifyWindowEvent;
static jmethodID jLensApplication_notifyViewEvent;
static jmethodID jLensApplication_notifyDeviceEvent;
static jmethodID jLensApplication_notifyMenuEvent;
static jmethodID jLensApplication_reportException;

static jclass jGlassWindowClass;
static jmethodID jGlassWindowClass_Add;
static jmethodID jGlassWindowClass_Remove;

char *glass_RuntimeException = "java/lang/RuntimeException";
char *glass_NullPointerException = "java/lang/NullPointerException";
char *glass_UnsupportedOperationException = "java/lang/UnsupportedOperationException";

static void initIDs(JNIEnv *env);

static int haveIDs = 0;
static JavaVM *pGlassVm;
static int trapCtrlC = 0;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    char *javafxDebug;
    pGlassVm = vm;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6)) {
        return JNI_ERR; /* JNI version not supported */
    }
    glass_logger_init(vm, env);
    javafxDebug = getenv("JAVAFX_DEBUG");
    if (javafxDebug) {
        trapCtrlC = atoi(javafxDebug);
    }
    return JNI_VERSION_1_6;
}

JavaVM *glass_application_GetVM() {
    //always correct as it was set on JNI_OnLoad
    return pGlassVm;
}

static void initIDs(JNIEnv *env) {
    if (haveIDs) {
        return;
    }
    GLASS_LOG_FINE("Setting up JNI references");
    haveIDs = 1;

    // screen specific
    jScreenClass =
        (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/Screen"));
    CHECK_AND_RET_VOID(env);

    //Aplication.java
    CHECK_AND_RET_VOID(env);

    //LensWindow
    jLensWindowClass = (*env)->NewGlobalRef(env,
                                            (*env)->FindClass(env, "com/sun/glass/ui/lens/LensWindow"));
    CHECK_AND_RET_VOID(env);


    //LensApplication
    jLensApplicationClass = (*env)->NewGlobalRef(env,
                                                 (*env)->FindClass(env, "com/sun/glass/ui/lens/LensApplication"));
    CHECK_AND_RET_VOID(env);
    jLensApplication_notifyKeyEvent =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyKeyEvent",
                            "(Lcom/sun/glass/ui/lens/LensView;III[C)V");
    jLensApplication_notifyMouseEvent = (*env)->GetMethodID(
                                            env, jLensApplicationClass, "notifyMouseEvent",
                                            "(Lcom/sun/glass/ui/lens/LensView;IIIIIIIZZ)V");
    jLensApplication_notifyScrollEvent = (*env)->GetMethodID(
                                             env, jLensApplicationClass, "notifyScrollEvent",
                                             "(Lcom/sun/glass/ui/lens/LensView;IIIIDDIIIIIDD)V");
    jLensApplication_notifyTouchEvent = (*env)->GetMethodID(
                                            env, jLensApplicationClass, "notifyTouchEvent",
                                            "(Lcom/sun/glass/ui/lens/LensView;IJIIII)V");
    jLensApplication_notifyWindowResize =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyWindowResize",
                            "(Lcom/sun/glass/ui/lens/LensWindow;III)V");
    jLensApplication_notifyWindowMove =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyWindowMove",
                            "(Lcom/sun/glass/ui/lens/LensWindow;II)V");
    jLensApplication_createNativeEventThread =
        (*env)->GetStaticMethodID(env, jLensApplicationClass,
                                  "createNativeEventThread", "(JJ)V");
    jLensApplication_waitEventLoopsToFinish =
        (*env)->GetStaticMethodID(env, jLensApplicationClass,
                                  "waitEventLoopsToFinish", "()V");

    jLensApplication_notifyWindowEvent =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyWindowEvent",
                            "(Lcom/sun/glass/ui/lens/LensWindow;I)V");

    jLensApplication_notifyViewEvent =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyViewEvent",
                            "(Lcom/sun/glass/ui/lens/LensView;IIIII)V");

    jLensApplication_notifyDeviceEvent =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyDeviceEvent",
                            "(IZ)V");
    jLensApplication_notifyMenuEvent =
        (*env)->GetMethodID(env, jLensApplicationClass, "notifyMenuEvent",
                            "(Lcom/sun/glass/ui/lens/LensView;IIIIZ)V");

    jLensApplication_reportException =
        (*env)->GetStaticMethodID(env, jLensApplicationClass, "reportException",
                            "(Ljava/lang/Throwable;)V");

    CHECK_AND_RET_VOID(env);

    jGlassWindowClass =
            (*env)->NewGlobalRef(env,
                                 (*env)->FindClass(env, "com/sun/glass/ui/Window"));
    CHECK_AND_RET_VOID(env);
    jGlassWindowClass_Add =  
        (*env)->GetStaticMethodID(env, jGlassWindowClass, "add",
                                  "(Lcom/sun/glass/ui/Window;)V");
    CHECK_AND_RET_VOID(env);
    jGlassWindowClass_Remove =  
        (*env)->GetStaticMethodID(env, jGlassWindowClass, "remove",
                                  "(Lcom/sun/glass/ui/Window;)V");

    GLASS_LOG_FINE("Set up JNI references");
}

/*
 * Class:     com_sun_glass_ui_lens_LensApplication
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensApplication__1initIDs
(JNIEnv *env, jclass _jApplicationClass) {

    jApplicationClass = _jApplicationClass;
    initIDs(env);
}

/*
 * Class:     com_sun_glass_ui_lens_LensApplication
 * Method:    _initialize
 * Signature: ()V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensApplication__1initialize
(JNIEnv *env, jclass jApplicationClass) {

    return lens_wm_initialize(env);
}

/*
 * Class:     com_sun_glass_ui_lens_LensApplication
 * Method:    staticScreen_getScreens
 * Signature: ()[Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_lens_LensApplication_staticScreen_1getScreens
(JNIEnv *env, jobject jApplication) {

    return createJavaScreens(env);
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensApplication_registerApplication
(JNIEnv *env , jobject this) {
    GLASS_LOG_FINE("Rgistering LenApplication object");
    pApplication = (*env)->NewGlobalRef(env, this);
}

void glass_throw_exception_by_name(JNIEnv *env, const char *name, const char *msg) {
    GLASS_LOG_WARNING("Throwing exception %s '%s'", name, msg);
    jclass cls = (*env)->FindClass(env, name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    } else {
        GLASS_LOG_SEVERE("Exception class %s not found", name);
    }
    /* free the local ref */
    (*env)->DeleteLocalRef(env, cls);
}

void glass_application_request_native_event_loop(JNIEnv *env,
                                                 nativeEventLoopCallback callback,
                                                 void *handle) {
    GLASS_LOG_FINE("Creating native event thread");
    (*env)->CallStaticVoidMethod(env, jLensApplicationClass,
                                 jLensApplication_createNativeEventThread,
                                 ptr_to_jlong(callback), ptr_to_jlong(handle));
    (void)glass_application_checkReportException(env);
    GLASS_LOG_FINE("Created native event thread");
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensApplication_startNativeEventLoop
(JNIEnv *env, jobject len, jobject lensApplication,
 jlong callbackFuncPtr, jlong nativeHandle) {

    nativeEventLoopCallback callback = jlong_to_ptr(callbackFuncPtr);

    void *handle = jlong_to_ptr(nativeHandle);

    if (callback != NULL) {
        GLASS_LOG_FINE("Calling native event loop callback");
        callback(env, handle); //blocking call
        GLASS_LOG_FINE("Finished native event loop callback");
    } else {
        GLASS_LOG_WARNING(
            "Cannot start event loop with callback=%p, handle=%p",
            callback, handle);
    }
}

// convert an char * string to a jcharArray
// call glass_jcharArray_release when done with array
jcharArray glass_util_strToJcharArray(JNIEnv *env, char *str) {

    int len = strlen(str);

    jcharArray jchars = (*env)->NewCharArray(env, len);

    jchar *jc = (*env)->GetCharArrayElements(env, jchars, 0);

    int i;
    for (i = 0; i < len; i++) {
        jc[i] = (jchar) str[i];
    }

    (*env)->ReleaseCharArrayElements(env, jchars, jc, 0);

    return jchars;
}

void glass_util_jcharArrayRelease(JNIEnv *env, jcharArray jcharsobj) {
    if (jcharsobj) {
        (*env)->DeleteLocalRef(env, jcharsobj);
    }
}

jboolean glass_application_checkReportException(JNIEnv *env) {
    jthrowable t = (*env)->ExceptionOccurred(env);
    if (t) {
        (*env)->ExceptionClear(env);
        (*env)->CallStaticVoidMethod(env,
            jLensApplicationClass, jLensApplication_reportException, t);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

void glass_application_notifyKeyEvent(JNIEnv *env,
                                      NativeWindow window,
                                      int eventType,
                                      int jfxKeyCode,
                                      jboolean isRepeatEvent) {

    char *keyStr = "";
    static int lastKey = com_sun_glass_events_KeyEvent_VK_UNDEFINED;

    if (!pApplication) {
        return;
    }

    if (window == NULL) {
        GLASS_LOG_WARNING("skipping notifyKeyEvent with NULL window");
        return;
    }

    if (window->view == NULL || window->view->lensView == NULL) {
        GLASS_LOG_WARNING("skipping notifyKeyEvent with NULL view");
        return;
    }
    jobject jview = window->view->lensView;

    if (jfxKeyCode == com_sun_glass_events_KeyEvent_VK_UNDEFINED) {
        GLASS_LOG_WARNING("skipping undefined key");
        return;
    }

    if (glass_inputEvents_getKeyChar(jfxKeyCode, &keyStr) == LENS_FAILED) {
        GLASS_LOG_WARNING("Failed to retrive key char in glass_inputEvents_getKeyChar()"
                          " - skipping");
        return;
    }

    if (isRepeatEvent && glass_inputEvents_isKeyModifier(jfxKeyCode)) {
        //no need to send an event if the key is a modifier
        GLASS_LOG_FINE("skipping repeat event on modifier key");
        return;
    }

    jcharArray jchars = glass_util_strToJcharArray(env, keyStr);

    if (jchars == NULL) {
        GLASS_LOG_WARNING("skipping notifyKeyEvent with NULL charBuffer");
        return;
    }

    glass_inputEvents_updateKeyModifiers(jfxKeyCode, eventType);

    GLASS_LOG_FINER("modifiers mask = 0x%x", glass_inputEvents_getModifiers());

    //notify typed on either repeat or release when key code is printable
    if (keyStr[0] &&
            (eventType == com_sun_glass_events_KeyEvent_RELEASE ||
             isRepeatEvent)) {

        GLASS_LOG_FINER("Sending typed event for jfxKeyCode %d, keyStr=%d",
                        jfxKeyCode, keyStr);

        (*env)->CallVoidMethod(env, pApplication, jLensApplication_notifyKeyEvent,
                               jview,
                               com_sun_glass_events_KeyEvent_TYPED,
                               jfxKeyCode,
                               glass_inputEvents_getModifiers(),
                               jchars);
        (void)glass_application_checkReportException(env);
    }

    int modifiers = glass_inputEvents_getModifiers();

    if (trapCtrlC && jfxKeyCode == 'C'
            && modifiers == com_sun_glass_events_KeyEvent_MODIFIER_CONTROL) {
        GLASS_LOG_FINEST("raise(SIGINT)");
        raise(SIGINT);
    }

    GLASS_LOG_FINEST("JNI call notifyKeyEvent");
    (*env)->CallVoidMethod(env, pApplication, jLensApplication_notifyKeyEvent,
                           jview,
                           eventType, jfxKeyCode,
                           modifiers, jchars);
    (void)glass_application_checkReportException(env);

    glass_util_jcharArrayRelease(env, jchars);

}

void glass_application_notifyMouseEvent(JNIEnv *env,
                                        NativeWindow window,
                                        int eventType,
                                        int x,
                                        int y,
                                        int absx,
                                        int absy,
                                        int button) {

    jboolean isPopupTrigger = JNI_FALSE;

    if (!pApplication) {
        return;
    }

    if (window->isEnabled) {

        if (window->view == NULL || window->view->lensView == NULL) {
            GLASS_LOG_WARNING("skipping notifyMouseEvent with NULL view");
            return;
        }
        jobject jview = window->view->lensView;

        //check for context menu hint - triggered by right click
        //NOTE:if we want to support this in touch there was a suggestion to use
        //long tap
        if (eventType == com_sun_glass_events_MouseEvent_UP &&
                button == com_sun_glass_events_MouseEvent_BUTTON_RIGHT) {
            isPopupTrigger = JNI_TRUE;
            GLASS_LOG_FINER("Context menue hint detected");
        }

        glass_inputEvents_updateMouseButtonModifiers(button, eventType);

        (*env)->CallVoidMethod(env, pApplication, jLensApplication_notifyMouseEvent,
                               jview,
                               eventType,
                               x, y, absx, absy,
                               button,
                               glass_inputEvents_getModifiers(),
                               isPopupTrigger, JNI_FALSE);
        if (glass_application_checkReportException(env)) {
            //an exception happened, bail now.
            return;
        }

        if (isPopupTrigger && window->view) {
            //we need to explictly notify the view for menu event in order 
            //for the application's OnContextMenuRequested handler to be called
            glass_application_notifyMenuEvent(env, window->view,
                                              x, y, absx, absy,
                                              JNI_FALSE);
        }

    } else {
        GLASS_LOG_FINE("Window %d[%p] is disabled - sending FOCUS_DISABLED event",
                       window->id, window);
        glass_application_notifyWindowEvent(env,
                                            window,
                                            com_sun_glass_events_WindowEvent_FOCUS_DISABLED);
    }
}

void glass_application_notifyScrollEvent(JNIEnv *env,
                                         NativeWindow window,
                                         int x, int y, int xabs, int yabs,
                                         jdouble dx, jdouble dy) {

    if (!pApplication) {
        return;
    }

    if (window->view == NULL || window->view->lensView == NULL) {
        GLASS_LOG_WARNING("skipping notifyScrollEvent with NULL view");
        return;
    }
    jobject jview = window->view->lensView;

    GLASS_LOG_FINEST("JNI call notifyScrollEvent");
    (*env)->CallVoidMethod(env, pApplication,
                           jLensApplication_notifyScrollEvent,
                           jview,
                           x, y,
                           xabs, yabs,
                           dx, dy,
                           glass_inputEvents_getModifiers(),
                           (jint) 0 /*lines*/,
                           (jint) 0 /*chars*/,
                           (jint) 0 /*defaultLines*/,
                           (jint) 0 /*defaultChars*/,
                           (jdouble)13.0 /*X multiplier*/,
                           (jdouble)13.0 /*Y multiplier*/);
    (void)glass_application_checkReportException(env);
}

void glass_application_notifyTouchEvent(JNIEnv *env,
                                        NativeWindow window,
                                        jint state,
                                        jlong id,
                                        int x,
                                        int y,
                                        int xabs,
                                        int yabs) {

    int button = com_sun_glass_events_MouseEvent_BUTTON_NONE;
    int eventType = -1;

    if (!pApplication) {
        return;
    }

    GLASS_LOG_FINEST("JNI call notifyTouchEvent");

    if (window->isEnabled) {
        if (window->view == NULL || window->view->lensView == NULL) {
            GLASS_LOG_WARNING("skipping notifyTouchEvent with NULL view");
            return;
        }
        jobject jview = window->view->lensView;

        button = com_sun_glass_events_MouseEvent_BUTTON_LEFT;

        if (state == com_sun_glass_events_TouchEvent_TOUCH_PRESSED) {
            eventType = com_sun_glass_events_MouseEvent_DOWN;
        } else if (state == com_sun_glass_events_TouchEvent_TOUCH_RELEASED) {
            eventType = com_sun_glass_events_MouseEvent_UP;
        } else if (state == com_sun_glass_events_TouchEvent_TOUCH_MOVED) {
            eventType = com_sun_glass_events_MouseEvent_MOVE;
        } else {
            GLASS_LOG_SEVERE("Unexpected touch state : %d", state);
        }

        glass_inputEvents_updateMouseButtonModifiers(button, eventType);


        (*env)->CallVoidMethod(env, pApplication,
                               jLensApplication_notifyTouchEvent,
                               jview,
                               state, id, x, y, xabs, yabs);
        (void)glass_application_checkReportException(env);
    } else {
        GLASS_LOG_FINE("Window %d[%p] is disabled - sending FOCUS_DISABLED event",
                       window->id, window);
        glass_application_notifyWindowEvent(env,
                                            window,
                                            com_sun_glass_events_WindowEvent_FOCUS_DISABLED);
    }
}



void glass_application_notifyWindowEvent_resize(JNIEnv *env,
                                                NativeWindow window,
                                                int  eventType,
                                                int width, int height) {

    if (!pApplication) {
        return;
    }

    if (!window) {
        GLASS_LOG_WARNING("notifyWindowEvent_resize with NULL window");
        return;
    }

    if (eventType == com_sun_glass_events_WindowEvent_RESTORE ||
            eventType == com_sun_glass_events_WindowEvent_MAXIMIZE ||
            eventType == com_sun_glass_events_WindowEvent_MINIMIZE ||
            eventType == com_sun_glass_events_WindowEvent_RESIZE) {

        GLASS_LOG_FINEST("JNI call notifyWindowResize");
        (*env)->CallVoidMethod(env, pApplication,
                               jLensApplication_notifyWindowResize,
                               window->lensWindow, eventType,
                               width, height);
        (void)glass_application_checkReportException(env);
    } else {
        GLASS_LOG_WARNING("glass_application_notifyWindowEvent_resize "
                          "was called with unsupported event - event code %d",
                          eventType);
    }
}

void glass_application_notifyWindowEvent_move(JNIEnv *env,
                                              NativeWindow window,
                                              int x, int y) {

    if (!window) {
        GLASS_LOG_WARNING("notifyWindowEvent_move with NULL window");
        return;
    }

    GLASS_LOG_FINEST("JNI call notifyWindowMove");
    (*env)->CallVoidMethod(env, pApplication,
                           jLensApplication_notifyWindowMove,
                           window->lensWindow,
                           x, y);
    (void)glass_application_checkReportException(env);
}

void glass_application_notifyWindowEvent(JNIEnv *env,
                                         NativeWindow window,
                                         int windowEvent) {

    if (!pApplication) {
        return;
    }

    if (!window) {
        GLASS_LOG_WARNING("notifyWindowEvent with NULL window");
        return;
    }

    GLASS_LOG_FINEST("JNI call notifyWindowEvent");
    (*env)->CallVoidMethod(env, pApplication,
                           jLensApplication_notifyWindowEvent,
                           window->lensWindow,
                           windowEvent);
    (void)glass_application_checkReportException(env);
}

void glass_application_notifyViewEvent(JNIEnv *env,
                                       NativeView view,
                                       int viewEventType,
                                       int x, int y, int width, int height) {

    if (!pApplication) {
        return;
    }

    if (!view || !view->lensView) {
        GLASS_LOG_WARNING("notifyViewEvent with NULL view");
        return;
    }

    GLASS_LOG_FINEST("JNI call notifyViewEvent to lensView %p", view->lensView);
    (*env)->CallVoidMethod(env, pApplication,
                           jLensApplication_notifyViewEvent,
                           view->lensView,
                           viewEventType,
                           x, y, width, height);
    (void)glass_application_checkReportException(env);
}

void glass_application_notifyMenuEvent(JNIEnv *env,
                                       NativeView view,
                                       int x, int y, int xAbs, int yAbs,
                                       jboolean isKeyboardTrigger) {

    if (!pApplication) {
        return;
    }

    if (!view || !view->lensView) {
        GLASS_LOG_WARNING("notifyMenuEvent with NULL view");
        return;
    }

    GLASS_LOG_FINEST("JNI call notifyMenuEvent to lensView %p", view->lensView);
    (*env)->CallVoidMethod(env, pApplication,
                           jLensApplication_notifyMenuEvent,
                           view->lensView,
                           x, y, xAbs, yAbs, isKeyboardTrigger);
    (void)glass_application_checkReportException(env);

}

void glass_application_notifyDeviceEvent(JNIEnv *env,
                                         jint flags,
                                         jboolean attach) {

    if (!pApplication) {
        return;
    }

    GLASS_LOG_FINEST("JNI call notifyDeviceEvent flags=0x%x attach=%i",
                     flags, (int) attach);
    (*env)->CallVoidMethod(env, pApplication,
                           jLensApplication_notifyDeviceEvent,
                           flags, attach);
    (void)glass_application_checkReportException(env);

}

/*
 * Class:     com_sun_glass_events_KeyEvent
 * Method:    _getKeyCodeForChar
 * Signature: (C)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_events_KeyEvent__1getKeyCodeForChar
(JNIEnv *env, jclass keyeventClass, jchar c) {
    GLASS_LOG_FINE("Java key code requested for c='%c' (0x%04x)",
                   (char) c, (int) c);
    return glass_inputEvents_getJavaKeyCodeFromJChar(c);
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensApplication_shutdown
(JNIEnv *env, jobject _this) {

    GLASS_LOG_FINEST("Shutting down");
    if (pApplication) {
        //at this stage we don't need to worry about errors
        GLASS_LOG_FINE("DeleteGlobalRef(pApplication(%p))", pApplication);
        (*env)->DeleteGlobalRef(env, pApplication);
    }

    pApplication = NULL;

    lens_wm_shutdown(env);
    GLASS_LOG_FINEST("Shut down");
}

void glass_application_addWindowToVisibleWindowList (JNIEnv *env,
                                        NativeWindow window) {
    GLASS_LOG_FINE("Adding window %i[%p] to the visible window list", window->id, window);
    (*env)->CallStaticVoidMethod(env, jGlassWindowClass, jGlassWindowClass_Add, window->lensWindow);
    (void)glass_application_checkReportException(env);
}

void glass_application_RemoveWindowFromVisibleWindowList (JNIEnv *env,
                                        NativeWindow window) {
    GLASS_LOG_FINE("Removing window %i[%p] from the visible window list", window->id, window);
    (*env)->CallStaticVoidMethod(env, jGlassWindowClass, jGlassWindowClass_Remove, window->lensWindow);
    (void)glass_application_checkReportException(env);
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensApplication__1notfyPlatformDnDStarted
  (JNIEnv *env, jobject lensApplication) {

    notify_lens_wm_DnDStarted();

}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensApplication__1notfyPlatformDnDEnded
  (JNIEnv *env, jobject lensApplication) {

    notify_lens_wm_DnDEnded();

}
