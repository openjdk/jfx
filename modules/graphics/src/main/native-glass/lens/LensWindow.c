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
#include "com_sun_glass_ui_lens_LensWindow.h"
#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_events_MouseEvent.h"

#include <pthread.h>

#define LENS_STATIC_WINDOWS_LIST_SIZE 4

LensResult glass_window_NativeWindow_release(JNIEnv *env,
                                             NativeWindow window) {    

    GLASS_LOG_FINE("NativeWindow_release on window %d[%p]",
                   window ? (signed int)window->id : -1,
                   window);

    //Check to see if this window is an owner of other windows, if so close them
    glass_window_list_lock();
    NativeWindow w = glass_window_list_getHead();
    int windowsListSize = glass_window_list_getSize();
    int notifyListIndex = -1;
    NativeWindow tmpStaticWindowsList[LENS_STATIC_WINDOWS_LIST_SIZE];
    NativeWindow *notifList = tmpStaticWindowsList;

    if (windowsListSize > LENS_STATIC_WINDOWS_LIST_SIZE) {
        notifList = (NativeWindow*)malloc(windowsListSize * sizeof(NativeWindow));
    }
   
    while (w) {
        GLASS_LOG_FINER("checking if w(%i)->owner(%i[%p]) == window %i[%p]", 
                        w->id,
                        w->owner?w->owner->id:-1,
                        w->owner,
                        window->id,window);

        if (w->owner == window) {
            GLASS_LOG_FINE("Closing window %i[%p] - owned by closing window %i[%p]", 
                           w->id,w,
                           window->id,window);

            notifList[++notifyListIndex] = w;
        }
        w = w->nextWindow;
    }

    glass_window_list_unlock();

    {   
        int i;
        for (i = 0; i <= notifyListIndex; ++i) {
            GLASS_LOG_FINER("Sending CLOSE event to window %i[%p]", notifList[i]->id, notifList[i]);
            glass_application_notifyWindowEvent(env, notifList[i], com_sun_glass_events_WindowEvent_CLOSE);
        }
    }

    if (notifList != tmpStaticWindowsList) {
        free(notifList);
        notifList = NULL;
    }

    GLASS_LOG_FINE("Removing window from window's list");
    glass_window_list_remove(window);

    GLASS_LOG_FINE("setting processEvents to false");
    window->processEvents = JNI_FALSE;

    GLASS_LOG_FINE("Notifying wm that window is released");
    lens_wm_notifyPlatformWindowRelease(env, window);

    GLASS_LOG_FINE("Releasing native platform data");
    glass_window_PlatformWindowRelease(env, window);

    if (window->view != NULL) {
        //this should never happen
        GLASS_LOG_SEVERE("Window's view (%p) is not closed", window->view);
        return JNI_FALSE;
    }

    glass_application_notifyWindowEvent(env,
                                        window,
                                        com_sun_glass_events_WindowEvent_DESTROY);

    if (window->lensWindow) {
        GLASS_LOG_FINE("Releasing LensWindow global reference for window %d[%p]",
                       window ? (signed int)window->id : -1,
                       window);
        (*env)->DeleteGlobalRef(env, window->lensWindow);
    }

    GLASS_LOG_FINE("freeing window (%p)", window);
    free(window);

    return LENS_OK;
}

static jlong glass_create_native_window
  (JNIEnv *env, jobject jWindow, NativeWindow owner,
 NativeScreen nativeScreen, jint creationMask) {

    int result = LENS_FAILED;
    NativeWindow window = (NativeWindow)calloc(1, sizeof(struct _NativeWindow));

    if (window != NULL) {
        window->currentBounds.width = window->currentBounds.height = 1;

        window->lensWindow = (*env)->NewGlobalRef(env, jWindow);

        GLASS_LOG_FINE("Allocated NativeWindow window = %p, owner = %p lensWindow=%p", 
                window,owner,window->lensWindow);

        if (window->lensWindow) {
            // set the default NativeWindow values
            window->owner = owner;
            window->screen = nativeScreen;
            window->creationMask = creationMask;
            window->isFocusable = JNI_TRUE;
            window->isVisible = JNI_FALSE;
            window->isEnabled = JNI_TRUE;
            window->state = NWS_NORMAL;
            window->view = NULL;
            window->alpha = 1.0;
            window->previousWindow = NULL;
            window->nextWindow = NULL;

            // set the root of the tree, which could be us.
            window->root = owner ? owner->root : window;

            if ((result = glass_window_PlatformWindowData_create(env, window)) 
                        == LENS_OK) {
                GLASS_LOG_FINE("NativeWindow created window %d[%p]->data(%p)",
                               window->id, window, window->data);
                glass_window_list_add(window);
            } else {
                GLASS_LOG_SEVERE("Failed to create PlatformWindowData");
            }
        } else {
            GLASS_LOG_SEVERE("NewGlobalRef failed");
        }
    } else {
        GLASS_LOG_SEVERE("malloc failed");
    }

    if (result) {
        GLASS_LOG_INFO("Can't create native window, releasing resources");
        glass_window_NativeWindow_release(env, window);
        if (window) {
            free(window);
        }
        return ptr_to_jlong(NULL);
    } else {
        return ptr_to_jlong(window);
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _createChildWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensWindow__1createChildWindow
  (JNIEnv *env, jobject jWindow, jlong ownerNativeWindowPtr) {

    NativeWindow owner = (NativeWindow)jlong_to_ptr(ownerNativeWindowPtr);

    if (!owner) {
        return 0; //can't have a child without an owner
    }

    return glass_create_native_window(env, jWindow, 
        owner, 
        owner->screen,
        0);
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _createWindow
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensWindow__1createWindow
  (JNIEnv *env, jobject jWindow, jlong ownerNativeWindowPtr,
   jlong nativeScreenPtr, jint creationMask) {

    NativeScreen nativeScreen =
        (NativeScreen)(jlong_to_ptr(nativeScreenPtr));

    NativeWindow owner = (NativeWindow)jlong_to_ptr(ownerNativeWindowPtr);

    return glass_create_native_window(env, jWindow, 
        owner, 
        nativeScreen,
        creationMask);
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    attachViewToWindow
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow_attachViewToWindow
(JNIEnv *env , jobject _this, jlong nativeWindowPtr, jlong nativeViewPtr) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    NativeView view = (NativeView)jlong_to_ptr(nativeViewPtr);
    jboolean result = JNI_FALSE;

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        // note view can be null

        GLASS_LOG_FINE("attach view %p to window %i[%p]", view, window->id, window);
        window->view = view;
        result = JNI_TRUE;
    }

    return result;
}



/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1close
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    jboolean result = JNI_FALSE;

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {
        GLASS_LOG_FINE("close window %i[%p]", window->id, window);

        LensResult res = glass_window_NativeWindow_release(env, window);
        if (res != LENS_OK) {
            GLASS_LOG_SEVERE("Failed to close native window (%p)",
                             window);

        }
        result =  (res ? JNI_FALSE : JNI_TRUE);
    }

    return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setMenubar
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setMenubar
(JNIEnv *env, jobject jWindow, jlong ptr, jlong menubarPtr) {
    GLASS_LOG_WARNING("Not implemented");
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _minimize
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1minimize
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jboolean minimize) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    jboolean result = JNI_FALSE;
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {
        GLASS_LOG_FINE("minimize window %i[%p]", window->id, window);
        result = glass_window_minimize(env,
                                       window,
                                       minimize);
    }

    return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _maximize
 * Signature: (JZZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1maximize
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jboolean maximize,
 jboolean wasMaximized) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    jboolean result = JNI_FALSE;
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {
        GLASS_LOG_FINE("maximize window %i[%p]", window->id, window);
        result =  glass_window_maximize(env,
                                        window,
                                        maximize,
                                        wasMaximized);
    }

    return result;
}

JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_lens_LensWindow__1getNativeWindowImpl
(JNIEnv *env, jobject this, jlong nativeWindowPtr) {
    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    return ptr_to_jlong(glass_window_getPlatformWindow(env, window));
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow_setBoundsImpl
(JNIEnv *env, jobject this, jlong nativeWindowPtr,
 jint x, jint y, jint width, jint height,
 jboolean needToUpdatePostion, jboolean needToUpdateSize, jboolean isContentSize) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {
        GLASS_LOG_FINER("setBoundsImpl called with x=%d, y=%d, width=%d, height= %d"
                        " needToUpdatePostion = %s, needToUpdateSize=%s, "
                        "isContentSize=%s", x, y, width, height,
                        needToUpdatePostion ? "true" : "false",
                        needToUpdateSize ? "true" : "false",
                        isContentSize ? "true" : "false");

        glass_window_setBoundsImpl(env, window,
                                   x, y, width, height,
                                   needToUpdatePostion , needToUpdateSize, isContentSize);
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setVisible
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setVisible
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jboolean visible) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    jboolean result = JNI_FALSE;

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set window %i[%p] to %svisible",
                       window->id, window, visible ? "" : "in");
        //until setVisible = true, window is expected not to be shown
        result =  glass_window_setVisible(env, window, visible);
    }

    return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setResizable
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setResizable
(JNIEnv *env, jobject jWindow, jlong ptr, jboolean resizeable) {
    //As we currently support only undecorated windows this options will
    // have no effect
    GLASS_LOG_WARNING("No effect on an undecorated window");
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _requestFocus
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1requestFocus
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jint focusEventType) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);

    jboolean result = JNI_FALSE;
    if (window == NULL) {

        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("request focus on window %p", window);
        result = glass_window_requestFocus(env, window, focusEventType);
    }

     return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setFocusable
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setFocusable
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jboolean isFocusable) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {
        GLASS_LOG_FINE("set focusable=%i on window %p", (int) isFocusable, window),
        (void) glass_window_setFocusable(env, window, isFocusable);
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setTitle
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setTitle
(JNIEnv *env, jobject jWindow, jlong ptr, jstring jtitle) {

    //As we currently support only undecorated windows this options will
    // have no effect
    GLASS_LOG_WARNING("No effect on an undecorated window");
    return JNI_TRUE;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setLevel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setLevel
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jint level) {
    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set level=%i on window %p", level, window);

        if (!glass_window_setLevel(window, level)) {
            GLASS_LOG_SEVERE("Failed to setLevel for window, handle %p",
                             jlong_to_ptr(nativeWindowPtr));
            glass_throw_exception_by_name(env, glass_RuntimeException, "setLevel failed");
        }
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setAlpha
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setAlpha
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jfloat alpha) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set alpha=%f on window %i[%p]", alpha, window->id, window);

        if (!glass_window_setAlpha(env, window, alpha)) {
            GLASS_LOG_WARNING("failed to set window alpha");
            //we might want to throw an exception
        }
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setBackground
 * Signature: (JFFF)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setBackground
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr,
 jfloat r, jfloat g, jfloat b) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);


    jboolean result = JNI_FALSE;
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set background=(%f,%f,%f) on window %p",
                       r, g, b, window);

        result = glass_window_setBackground(window, r, g, b);
    }

     return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setEnabled
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jboolean enabled) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set enabled=%s on window %d[%p]",
                       enabled?"true":"false",
                       window->id, window);

        window->isEnabled = enabled;

        GLASS_LOG_FINE("glass_window_setFocusable(%s)", enabled?"true":"false");
        glass_window_setFocusable(env, window, enabled);

        //syntheticlly notify view for mouse exit
        if (!enabled && window->view) {
            glass_application_notifyMouseEvent(env,
                                               window,
                                               com_sun_glass_events_MouseEvent_EXIT, 
                                               0,0,0,0,
                                               com_sun_glass_events_MouseEvent_BUTTON_NONE);

        }
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setMinimumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setMinimumSize
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jint width, jint height) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);

    jboolean result = JNI_FALSE;

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set window %d[%p] minimum size to %ix%i", 
                       window->id, window, width, height);
        result = glass_window_setMinimumSize(env, window, width, height);
    }

     return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setMaximumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setMaximumSize
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr, jint width, jint height) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    jboolean result = JNI_FALSE;

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("set window %p maximum size to %ix%i",
                       window, width, height);
        result = glass_window_setMaximumSize(env, window, width, height);
    }

     return result;
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _setIcon
 * Signature: (JLcom/sun/glass/ui/Pixels;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1setIcon
(JNIEnv *env, jobject jWindow, jlong ptr, jobject jPixels) {

    GLASS_LOG_WARNING("Iconization not implemented");
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _toFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1toFront
(JNIEnv *env, jobject _this, jlong nativeWindowPtr) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("bring window %d[%p] to front", window->id, window);
        glass_window_toFront(env, window);
    }
}

/*
 * Class:     com_sun_glass_ui_lens_LensWindow
 * Method:    _toBack
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1toBack
(JNIEnv *env, jobject jWindow, jlong nativeWindowPtr) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("send window %p to back", window);
        glass_window_toBack(env, window);
    }
}

JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_lens_LensWindow__1grabFocus
(JNIEnv *env, jobject _this, jlong nativeWindowPtr) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    jboolean result = JNI_FALSE;

    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("grab focus on window %p", window);
        result = glass_window_grabFocus(env, window);
    }

     return result;
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_lens_LensWindow__1ungrabFocus
(JNIEnv *env, jobject _this, jlong nativeWindowPtr) {

    NativeWindow window = (NativeWindow)jlong_to_ptr(nativeWindowPtr);
    if (window == NULL) {
        glass_throw_exception_by_name(
            env, glass_NullPointerException, "Window handle is null");
    } else {

        GLASS_LOG_FINE("ungrab focus on window %p", window);
        glass_window_ungrabFocus(env, window);
    }
}

jboolean glass_window_check_bounds(NativeWindow window, int *pWidth, int *pHeight) {

    GLASS_LOG_FINE("check bounds for window %i[%p] with new dimentions %ix%i",
                   window->id, window, *pWidth,*pHeight);
    jboolean paramsAreValid = JNI_TRUE;

    //check width
    if (window->minWidth > 0 && *pWidth < window->minWidth) {
        GLASS_LOG_FINE("Width %i, is smaller then the minimum window width (%i)."
                       " Updating width to minimum", *pWidth, window->minWidth);
        *pWidth = window->minWidth;
        paramsAreValid = JNI_FALSE;
    } else if (window->maxWidth > 0 && *pWidth > window->maxWidth) {
        GLASS_LOG_FINE("Width %i, is bigger then the window's maximum width (%i)."
                       " Updating width to maximum possible", *pWidth, window->maxWidth);
        *pWidth = window->maxWidth;
         paramsAreValid = JNI_FALSE;
    }

    //check height

    if (window->minHeight > 0 && *pHeight < window->minHeight) {
        GLASS_LOG_FINE("Height %i, is smaller then the minimum window's height (%i)."
                       " Updating height to minimum possible", *pHeight, window->minHeight);
        *pHeight = window->minHeight;
        paramsAreValid = JNI_FALSE;
    } else if (window->maxHeight > 0 && *pHeight > window->maxHeight) {
        GLASS_LOG_FINE("Height %i, is bigger then the window's maximum height (%i)."
                       " Updating height to maximum possible", *pHeight, window->maxHeight);
        *pHeight = window->maxHeight;
        paramsAreValid = JNI_FALSE;
    }

    GLASS_LOG_FINE("Params %s. Returning width = %i, height = %i",
                   (paramsAreValid)? "are valid" : "updated (were out of bounds)",
                   *pWidth, *pHeight);

    return paramsAreValid;
}

char *lens_window_getNativeStateName(NativeWindowState state) {

    switch (state) {
        case NWS_FULLSCREEN:
            return "FULLSCREEN";
        case NWS_MAXIMIZED:
            return "MAXIMIZED";
        case NWS_MINIMIZED:
            return "MINIMIZED";
        case NWS_NORMAL:
            return "NORMAL";
        default:
            GLASS_LOG_FINE("unknown native window state (%d)", state);
            return "UNKNOWN";
    }
}

/* 
 * Link list of allocated windows
 * The list will be saved in Z order with head the deepest from the user, 
 * tail closest.
 * impls that need Z order should also call toFront, toBack so that Z order is maintained.
 */
static NativeWindow windowList_head = NULL;
static NativeWindow windowList_tail = NULL;
static int windowList_size = 0;
static pthread_mutex_t windowListMutex = PTHREAD_MUTEX_INITIALIZER;

void glass_window_list_lock(){
    pthread_mutex_lock(&windowListMutex);
}

void glass_window_list_unlock(){
    pthread_mutex_unlock(&windowListMutex);
}

int glass_window_list_getSize(){
    return windowList_size;
}

static jboolean glass_window_isExist(NativeWindow window){

    NativeWindow w = glass_window_list_getHead();
    while (w) {
         if (w == window) {
             return JNI_TRUE;
         }
         w = w->nextWindow;
    }

    return JNI_FALSE;
}


NativeWindow glass_window_list_getHead() {
    return windowList_head;
}

NativeWindow glass_window_list_getTail() {
    return windowList_tail;
}

jboolean glass_window_list_toFront(NativeWindow window) {

    glass_window_list_lock();
    // don't bother if we are already at the tail
    if (window == windowList_tail) {
        //already at head
        glass_window_list_unlock();
        return JNI_FALSE;
    }

    if (glass_window_isExist(window) == JNI_FALSE) {
        glass_window_list_unlock();
        GLASS_LOG_WARNING("window %p is not part of the windows list", window);
        return JNI_FALSE;
    }

    //disconnect first
    if (window->previousWindow) {
        window->previousWindow->nextWindow = window->nextWindow;
    }
    if (window->nextWindow) {
        window->nextWindow->previousWindow = window->previousWindow;
    }
    if (windowList_head == window) {
        windowList_head = window->nextWindow;
    }

    // inserting into tail spot
    window->previousWindow = windowList_tail;
    window->nextWindow = NULL;

    windowList_tail->nextWindow = window;
    windowList_tail = window;

    glass_window_list_unlock();
    return JNI_TRUE;
}

jboolean glass_window_list_toBack(NativeWindow window) {

    glass_window_list_lock();
    // don't bother if we are already at the head
    if (window == windowList_head) {
        //already at tail
        glass_window_list_unlock();
        return JNI_FALSE;
    }

    if (glass_window_isExist(window) == JNI_FALSE) {
        glass_window_list_unlock();
        GLASS_LOG_SEVERE("window %p is not part of the windows list", window);
        return JNI_FALSE;
    }


    //disconnect first
    if (window->previousWindow) {
        window->previousWindow->nextWindow = window->nextWindow;
    }
    if (window->nextWindow) {
        window->nextWindow->previousWindow = window->previousWindow;
    }
    if (windowList_tail == window) {
        windowList_tail = window->previousWindow;
    }

    // inserting into head spot
    window->previousWindow = NULL;
    window->nextWindow = windowList_head;

    windowList_head->previousWindow = window;
    windowList_head = window;

    glass_window_list_unlock();
    return JNI_TRUE;
}

/*
 * add a newly created window to the window list. 
 * Will be added either:
 *   closest Z to user
 *   on top of its parent
 */
void glass_window_list_add(NativeWindow window) {
    if (!window) {
        GLASS_LOG_WARNING("glass_window_list_add called with NULL window");
        return;
    }

    glass_window_list_lock();

    if (windowList_head == NULL) {
        //create the head
        windowList_head = window;
    }

    if (windowList_tail != NULL) {
        //append window to list
        windowList_tail->nextWindow = window;
    }

    window->previousWindow = windowList_tail; 
    window->nextWindow = NULL; //we are now the tail
 
    windowList_tail = window; //update the tail
    
    windowList_size++;

    glass_window_list_unlock();
}

/*
 * remove a window from the active list
 */
void glass_window_list_remove(NativeWindow window) {
    if (!window) {
        GLASS_LOG_WARNING("glass_window_list_remove called with NULL window");
        return;
    }

    glass_window_list_lock();

    if (glass_window_isExist(window) == JNI_FALSE) {
        glass_window_list_unlock();
        GLASS_LOG_SEVERE("window %p is not part of the windows list", window);
        return;
    }


    if (window->previousWindow != NULL) {
        //we have someone before us, attach it to the next window inline
        //(can be null)
        window->previousWindow->nextWindow = window->nextWindow;
    } else {
        //we are the head, replace with next window (can be null)
        windowList_head = window->nextWindow;
    }

    if (window->nextWindow != NULL) {
        //we have someone after us, attached it to the window before us
        //(can be null)
        window->nextWindow->previousWindow = window->previousWindow;
    } else {
        //we are the tail, replace with previous window (can be null)
        windowList_tail = window->previousWindow;
    }

    windowList_size--;

    glass_window_list_unlock();
}


void glass_window_listPrint() {

    glass_window_list_lock();

    NativeWindow w = windowList_head;
    GLASS_LOG_FINE("Window list head %i[%p] tail %i[%p]\n",
                   windowList_head?windowList_head->id : -1,
                   windowList_head,
                   windowList_tail? windowList_tail->id : -1,
                   windowList_tail);
    while (w) {
        GLASS_LOG_FINE(" window %i[%p] p=%i[%p] n=%i[%p]\n",
                       w? w->id :-1, w,
                       w->previousWindow?w->previousWindow->id :-1, w->previousWindow,
                       w->nextWindow?w->nextWindow->id : -1, w->nextWindow);
        w = w->nextWindow;
    }

    glass_window_list_unlock();
}

/* 
 * FocusWindow
 * The window that currently has focus. 
 * Note, this may be NULL.
 */
static NativeWindow focusedWindow = NULL;

NativeWindow glass_window_getFocusedWindow() {
    GLASS_LOG_FINE("Returning focused window %d[%p]",
                   focusedWindow?focusedWindow->id:-1,
                   focusedWindow);
    return focusedWindow;
}

LensResult glass_window_setFocusedWindow(NativeWindow window) {
    GLASS_LOG_FINE("Cached focused window was %d[%p], now its %d[%p]",
                   focusedWindow?focusedWindow->id:-1,
                   focusedWindow,
                   window?window->id:-1,
                   window);
    focusedWindow = window;
    return LENS_OK;
}

LensResult glass_window_resetFocusedWindow(NativeWindow window) {
    if (window == focusedWindow) {
        GLASS_LOG_FINE("Cached focused window have been reset");
        focusedWindow = NULL;
    }
    return LENS_OK;
}


