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
 
#include "input/LensInput.h"
#include "wm/LensWindowManager.h"
#include "com_sun_glass_events_ViewEvent.h"
#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "lensRFB/lensRFB.h"

#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>

static pthread_mutex_t renderMutex = PTHREAD_MUTEX_INITIALIZER;

static int _mousePosX;
static int _mousePosY;

static jboolean _mousePressed = JNI_FALSE;
static jboolean _onDraggingAction = JNI_FALSE;
static NativeWindow _dragGrabbingWindow = NULL;




static inline void render_lock() {
    pthread_mutex_lock(&renderMutex);
}

static inline void render_unlock() {
    pthread_mutex_unlock(&renderMutex);
}


static NativeScreen mainScreen;

static void lens_wm_rfbNotifyClearScreen();
static void lens_wm_clearScreen();
static void lens_wm_initRFB(JNIEnv *env);
static void lens_wm_rfbNotifyWindowUpdate(NativeWindow window,
                                          int width, int height);
static void lens_wm_windowCacheBounds(NativeWindow window);

jboolean lens_wm_initialize(JNIEnv *env) {

    jboolean result;

    GLASS_LOG_FINE("Init device");
    result = glass_application_initialize(env);
    if (result) {
        GLASS_LOG_FINE("Init screen");
        mainScreen = lens_screen_initialize(env);
        if (mainScreen) {
            GLASS_LOG_FINE("Clearing screen");
            lens_wm_clearScreen();

            GLASS_LOG_FINE("Cursor init");
            fbCursorInitialize();

            lens_wm_initRFB(env);
            GLASS_LOG_FINE("Init input devices");
            result =  lens_input_initialize(env);
            if (!result) {
                GLASS_LOG_SEVERE("lens_input_initialize failed");
            }
        } else {
            GLASS_LOG_SEVERE("lens_screen_initialize() failed");
            result = JNI_FALSE;
        }
    } else {
        GLASS_LOG_SEVERE("glass_application_initialize() failed");
    }

    return result;
}

NativeScreen glass_screen_getMainScreen() {
    return mainScreen;
}

void lens_wm_getPointerPosition(int *pX, int *pY) {
    *pX = _mousePosX;
    *pY = _mousePosY;
}

void lens_wm_setPointerPosition(int x, int y) {
    _mousePosX = x;
    _mousePosY = y;
    fbCursorSetPosition(_mousePosX, _mousePosY);
}

LensResult lens_wm_notifyPlatformWindowRelease(JNIEnv *env, NativeWindow window) {

    GLASS_LOG_FINE("WM Window Relase window [%i]%p", window->id, window);

    if (window == lens_wm_getMouseWindow()) {
        // allow the next mouse motion to generate the ENTER
        lens_wm_setMouseWindow(NULL);
    }
    if (window == lens_wm_getGrabbedWindow()) {

        lens_wm_setGrabbedWindow(NULL); // don't bother with an event
    }
    if (window == glass_window_getFocusedWindow()) {
        glass_window_setFocusedWindow(NULL);
    }

    NativeWindow head = glass_window_list_getHead();
    if (head && head->view) {
        lens_wm_repaint(env, head);
    }

    return LENS_OK;
}

void lens_wm_repaint(JNIEnv *env, NativeWindow window) {
    render_lock();

    // remember clear could actually write pixels...
    lens_wm_clearScreen();

    if (window && window->view) {
        glass_application_notifyViewEvent(env,
                                          window->view,
                                          com_sun_glass_events_ViewEvent_REPAINT,
                                          window->currentBounds.x, window->currentBounds.y,
                                          window->currentBounds.width, window->currentBounds.height);
    }

    render_unlock();
}

static void reset_bounds_and_state(JNIEnv *env, NativeWindow window,
                                   jint newX, jint newY,
                                   jint newWidth, jint newHeight,
                                   NativeWindowState newState) {

    jboolean moved = JNI_FALSE;
    if ((newX != window->currentBounds.x) ||
            (newY != window->currentBounds.y)) {

        GLASS_LOG_FINE("Move window %p to %i,%i", window, newX, newY);
        window->currentBounds.x = newX;
        window->currentBounds.y = newY;
        moved = JNI_TRUE;
    }

    if (newState == NWS_NORMAL) {
        glass_window_check_bounds(window, &newWidth, &newHeight);
    }

    jboolean sized = JNI_FALSE;
    if ((newWidth != window->currentBounds.width) ||
            (newHeight != window->currentBounds.height)) {

        GLASS_LOG_FINE("Resize window %p to %ix%i", window, newWidth, newHeight);
        window->currentBounds.width = newWidth;
        window->currentBounds.height = newHeight;
        sized = JNI_TRUE;
    }

    jboolean state = JNI_FALSE;
    if (newState != window->state) {
        state = JNI_TRUE;
    }

    jboolean do_repaint = JNI_FALSE;

    //If the window is staying same size/possition and only the content have been changed,
    //resize event should be sent, in order to repaint the scene

    if (!moved && !sized && !state) {
        sized = JNI_TRUE;
    }

    if (moved || sized || state) {

        if (moved) {


            glass_application_notifyWindowEvent_move(env,
                                                     window,
                                                     newX, newY);

            do_repaint = JNI_TRUE;
        }

        if (sized) {

            glass_application_notifyWindowEvent_resize(env,
                                                       window,
                                                       com_sun_glass_events_WindowEvent_RESIZE,
                                                       newWidth, newHeight);

            do_repaint = JNI_TRUE;
        }

        if (state) {
            GLASS_LOG_FINE("State change window %p %i to %i", window, window->state, newState);

            if (window->state == NWS_NORMAL) {
                if (newState == NWS_FULLSCREEN) {
                    glass_application_notifyViewEvent(env,
                                                      window->view,
                                                      com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER,
                                                      window->currentBounds.x, window->currentBounds.y,
                                                      window->currentBounds.width, window->currentBounds.height);
                } else if (newState == NWS_MAXIMIZED) {
                    // no events generated
                } else if (newState == NWS_MINIMIZED) {
                    glass_application_notifyWindowEvent_resize(env,
                                                               window,
                                                               com_sun_glass_events_WindowEvent_MINIMIZE,
                                                               window->currentBounds.width, window->currentBounds.height);
                } else {
                    GLASS_LOG_WARNING("BAD State change on window %p %i to %i", window, window->state, newState);
                }
            } else if (window->state == NWS_FULLSCREEN && newState == NWS_NORMAL) {
                glass_application_notifyViewEvent(env,
                                                  window->view,
                                                  com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT,
                                                  window->currentBounds.x, window->currentBounds.y,
                                                  window->currentBounds.width, window->currentBounds.height);
            } else if (window->state == NWS_MAXIMIZED && newState == NWS_NORMAL) {
                // no events generated
            } else if (window->state == NWS_MINIMIZED && newState == NWS_NORMAL) {
                glass_application_notifyWindowEvent_resize(env,
                                                           window,
                                                           com_sun_glass_events_WindowEvent_RESTORE,
                                                           window->currentBounds.width, window->currentBounds.height);
            } else {
                GLASS_LOG_WARNING("BAD State change on window %p %i to %i", window, window->state, newState);
            }
            window->state = newState;
        }

        if (do_repaint) {
            GLASS_LOG_FINE("Repaint required");
            lens_wm_repaint(env, window);
        }


    }
}

void glass_window_setBoundsImpl(JNIEnv *env,
                                NativeWindow window,
                                jint newX, jint newY, jint newWidth, jint newHeight,
                                jboolean needToUpdatePostion,
                                jboolean needToUpdateSize,
                                jboolean isContentSize) {

    int x = window->currentBounds.x;
    int y = window->currentBounds.y;
    int width = window->currentBounds.width;
    int height = window->currentBounds.height;

    //handle resize
    if (needToUpdateSize && (width != newWidth || height != newHeight)) {
        width = newWidth;
        height = newHeight;
    }

    //handle move
    if (needToUpdatePostion && (x != newX || y != newY)) {
        x = newX;
        y = newY;
    }

    reset_bounds_and_state(env, window,
                           x, y, width, height,
                           window->state);
}


jboolean glass_window_setVisible(JNIEnv *env, NativeWindow window, jboolean visible) {

    window->isVisible = visible;

    if (!visible &&
            window == glass_window_getFocusedWindow()) {

        glass_application_notifyWindowEvent(env,
                                            window,
                                            com_sun_glass_events_WindowEvent_FOCUS_LOST);

        if (lens_wm_getGrabbedWindow()) {
            glass_window_ungrabFocus(env, lens_wm_getGrabbedWindow());
        }

        // find our first root window
        NativeWindow w = glass_window_list_getTail();
        while (w) {
            if (!w->owner && w->isVisible) {
                lens_wm_setFocusedWindow(env, w);
                break;
            }
            w = w->previousWindow;
        }


        // need to repaint here because we have a hole on the screen
        lens_wm_repaint(env, window);
    }


    return JNI_TRUE;
}

jboolean glass_view_drawBegin(NativeView view) {
    GLASS_LOG_FINE("glass_view_drawBegin");
    render_lock();
    return JNI_TRUE;
}

void glass_view_drawEnd(NativeView view) {
    GLASS_LOG_FINE("glass_view_drawEnd");
    render_unlock();
}

jboolean glass_window_requestFocus(JNIEnv *env, NativeWindow window, jint focusType) {

    jboolean result;

    NativeWindow focusWindow;

    if (lens_wm_getGrabbedWindow()) {
        // no changing focus in a grab
        return JNI_FALSE;
    }

    focusWindow = glass_window_getFocusedWindow();

    if (!window) {
        GLASS_LOG_WARNING("null window passes the glass_window_requestFocus");
        return JNI_FALSE;
    }

    if (window == focusWindow) {
        // no change, no notification ?
        GLASS_LOG_WARNING("Focus requested on current focus window");
        return JNI_TRUE;
    }

    if (!window->isFocusable) {
        GLASS_LOG_WARNING("Focus requested on isFocusable=false");
        return JNI_FALSE;
    }

    if (!window->isEnabled) {
        GLASS_LOG_WARNING("Focus requested on isEnabled=false");
        return JNI_FALSE;
    }

    lens_wm_setFocusedWindow(env, window);

    return JNI_TRUE;
}

jboolean glass_window_setFocusable(JNIEnv *env,
                                   NativeWindow window,
                                   jboolean isFocusable) {

    NativeWindow focusWindow;

    if (window->isFocusable == isFocusable) {
        // no change, so we can punt
        return JNI_TRUE;
    }

    focusWindow = glass_window_getFocusedWindow();
    if (!isFocusable && focusWindow == window) {
        lens_wm_setFocusedWindow(env, NULL);
        GLASS_LOG_WARNING("isFocusable(false) on focus owner, cascade ?");
    }

    window->isFocusable = isFocusable;

    return JNI_TRUE;
}

jboolean glass_window_setBackground(NativeWindow window,
                                    jfloat red,
                                    jfloat green,
                                    jfloat blue) {
    GLASS_LOG_WARNING("unimplemented glass_window_setBackground\n");
    return JNI_TRUE;
}

void glass_window_toFront(JNIEnv *env, NativeWindow window) {
    if (glass_window_list_toFront(window)) {
        lens_wm_repaint(env, window);
    }
}

void glass_window_toBack(JNIEnv *env, NativeWindow window) {
    if (glass_window_list_toBack(window)) {
        lens_wm_repaint(env, window);
    }
}

jboolean glass_window_grabFocus(JNIEnv *env, NativeWindow window) {

    if (window == lens_wm_getGrabbedWindow()) {
        //this is OK per spec
        GLASS_LOG_FINE("RE-GRAB on %p root %p\n", window, window->root);
        return JNI_TRUE;
    }

    if (NULL == lens_wm_getGrabbedWindow() &&
            window == glass_window_getFocusedWindow()) {
        // we allow the grab, note: focus is also checked in Java.
        GLASS_LOG_FINE("GRAB on %p root %p\n", window, window->root);
        lens_wm_setGrabbedWindow(window);
        return JNI_TRUE;
    }

    // should not be able to happen
    GLASS_LOG_FINE("ERROR NO-GRAB on %p\n", window);
    return JNI_FALSE;
}

// note, may also be called from mouse handling
void glass_window_ungrabFocus(JNIEnv *env, NativeWindow window) {

    if (window != lens_wm_getGrabbedWindow()) {
        //GLASS_LOG_WARNING("Grab release on the wrong window");
        GLASS_LOG_SEVERE("Grab release on the wrong window");
        return;
    }

    GLASS_LOG_FINE("UNGRAB on %p \n", window);
    lens_wm_setGrabbedWindow(NULL);

    //notify the UNGRAB
    glass_application_notifyWindowEvent(env,
                                        window,
                                        com_sun_glass_events_WindowEvent_FOCUS_UNGRAB);
}

void glass_view_setParent(JNIEnv *env,
                          NativeWindow parent,
                          NativeView view) {
    NativeWindow oldParent = view->parent;

    if (oldParent && oldParent->view) {
        GLASS_LOG_FINE("Notifying old view removed");
        glass_application_notifyViewEvent(env,
                                          oldParent->view,
                                          com_sun_glass_events_ViewEvent_REMOVE,
                                          0, 0, 0, 0);
        view->parent = NULL;
    }

    GLASS_LOG_FINE("Setting new owner, window %d [%p], for view %p",
                   parent ? (signed int)parent->id : - 1,
                   parent,
                   view);
    view->parent = parent; //may be null

    if (parent && parent->view) {
        GLASS_LOG_FINE("Notifying view it has been added %p", parent->view);
        glass_application_notifyViewEvent(env,
                                          parent->view,
                                          com_sun_glass_events_ViewEvent_ADD,
                                          0, 0, 0, 0);
    }
}

void lens_wm_shutdown(JNIEnv *env) {
    lens_platform_shutdown(env);
}

jboolean glass_window_setLevel(NativeWindow window, int level) {
    GLASS_LOG_WARNING("unimplemented glass_window_setLevel\n");
    return JNI_TRUE;
}

jboolean glass_window_setMinimumSize(JNIEnv *env,
                                     NativeWindow window,
                                     jint width, jint height) {
    window->minWidth  = width;
    window->minHeight = height;

    width = window->currentBounds.width;
    height = window->currentBounds.height;

    glass_window_check_bounds(window, &width, &height);

    if (width != window->currentBounds.width ||
            height != window->currentBounds.height) {
        glass_window_setBoundsImpl(env,
                                   window,
                                   0, 0, width, height,
                                   JNI_FALSE,  // position
                                   JNI_TRUE,   // size
                                   JNI_FALSE); // contentSize
    }

    return JNI_TRUE;
}

jboolean glass_window_setMaximumSize(JNIEnv *env,
                                     NativeWindow window,
                                     jint width, jint height) {
    window->maxWidth  = width;
    window->maxHeight = height;

    width = window->currentBounds.width;
    height = window->currentBounds.height;

    glass_window_check_bounds(window, &width, &height);

    if (width != window->currentBounds.width ||
            height != window->currentBounds.height) {
        glass_window_setBoundsImpl(env,
                                   window,
                                   0, 0, width, height,
                                   JNI_FALSE, // position
                                   JNI_TRUE,  // size
                                   JNI_FALSE);// contentSize
    }

    return JNI_TRUE;
}

jboolean glass_view_enterFullscreen(JNIEnv *env,
                                    NativeView view,
                                    jboolean animate,
                                    jboolean keepRatio,
                                    jboolean hideCursor) {

    NativeWindow window = view->parent;

    if (window->state == NWS_FULLSCREEN) {
        return JNI_FALSE;
    }


    NativeScreen screen = glass_screen_getMainScreen();

    //save current window bounds
    lens_wm_windowCacheBounds(window);

    reset_bounds_and_state(env, window,
                           0, 0,
                           screen->width, screen->height,
                           NWS_FULLSCREEN);

    return JNI_TRUE;

}

jboolean glass_view_exitFullscreen(JNIEnv *env,
                                   NativeView view,
                                   jboolean animate) {

    NativeWindow window = view->parent;

    if (!window) {
        // note, this can happen to a view after disconnected from the window.
        GLASS_LOG_FINE("NULL window passed to exitFullScreen");
        glass_application_notifyViewEvent(env,
                                          view,
                                          com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT,
                                          0, 0, 0, 0);
        return JNI_FALSE;
    }

    if (window->state != NWS_FULLSCREEN) {
        return JNI_FALSE;
    }

    GLASS_LOG_FINE("EXITING FS, restoring %d,%d %dx%d",
                   window->cachedBounds.x, window->cachedBounds.y,
                   window->cachedBounds.width, window->cachedBounds.height);

    reset_bounds_and_state(env, window,
                           window->cachedBounds.x, window->cachedBounds.y,
                           window->cachedBounds.width, window->cachedBounds.height,
                           NWS_NORMAL);

    return JNI_TRUE;
}

jboolean glass_window_minimize(JNIEnv *env,
                               NativeWindow window,
                               jboolean toMinimize) {

    if (toMinimize && window->state != NWS_MINIMIZED) {
        // cache for a restore later
        window->cachedBounds.width = window->currentBounds.width;
        window->cachedBounds.height = window->currentBounds.height;

        NativeScreen screen = glass_screen_getMainScreen();

        reset_bounds_and_state(env, window,
                               window->currentBounds.x, window->currentBounds.y,
                               screen->width, screen->height,
                               NWS_MINIMIZED);

    } else if (!toMinimize && window->state == NWS_MINIMIZED)  {

        reset_bounds_and_state(env, window,
                               window->currentBounds.x, window->currentBounds.y,
                               window->cachedBounds.width, window->cachedBounds.height,
                               NWS_NORMAL);

    }
    return JNI_TRUE;

}

jboolean glass_window_maximize(JNIEnv *env,
                               NativeWindow window,
                               jboolean toMaximize,
                               jboolean isMaximized) {
    if (toMaximize && window->state != NWS_MAXIMIZED) {

        NativeScreen screen = glass_screen_getMainScreen();

        // cache for a restore later
        window->cachedBounds.x = window->currentBounds.x;
        window->cachedBounds.y = window->currentBounds.y;
        window->cachedBounds.width = window->currentBounds.width;
        window->cachedBounds.height = window->currentBounds.height;

        reset_bounds_and_state(env, window,
                               0, 0,
                               screen->width, screen->height,
                               NWS_MAXIMIZED);

    } else if (!toMaximize && window->state == NWS_MAXIMIZED)  {

        reset_bounds_and_state(env, window,
                               window->cachedBounds.x, window->cachedBounds.y,
                               window->cachedBounds.width, window->cachedBounds.height,
                               NWS_NORMAL);

    }
    return JNI_TRUE;
}

NativeWindow glass_window_findWindowAtLocation(int absX, int absY,
                                               int *pRelX, int *pRelY) {

    NativeWindow w = glass_window_list_getTail();
    while (w) {
        if (absX >= w->currentBounds.x &&
                absX < w->currentBounds.x + w->currentBounds.width &&
                absY >= w->currentBounds.y &&
                absY < w->currentBounds.y + w->currentBounds.height &&
                w->isEnabled) {

            *pRelX = absX - w->currentBounds.x;
            *pRelY = absY - w->currentBounds.y;
            GLASS_LOG_FINER(
                "Absolute coordinates %i,%i are on window %p "
                "as relative coordinates %i,%i",
                absX, absY, w, *pRelX, *pRelY);
            return w;
        }
        w = w->previousWindow;
    }
    GLASS_LOG_FINER("Absolute coordinates %i,%i are not on a window",
                    absX, absY);
    return NULL;
}

NativeWindow grabbedWindow = NULL;

NativeWindow lens_wm_getGrabbedWindow() {
    return grabbedWindow;
}

void lens_wm_setGrabbedWindow(NativeWindow window) {
    grabbedWindow = window;
}


static void handleClickOrTouchEvent(JNIEnv *env, int xabs, int yabs) {

    int relX, relY;
    NativeWindow window = glass_window_findWindowAtLocation(xabs, yabs,
                          &relX, &relY);

    // if we have a grabbed window, check to see if this breaks the grab
    if (grabbedWindow != NULL) {
        if ((window == NULL) ||
                (window->root != grabbedWindow->root)) {
            glass_window_ungrabFocus(env, grabbedWindow);
        }
    }

    if (window != NULL) {
        NativeWindow focusedWindow = glass_window_getFocusedWindow();
        // Will this cause a focus change ?
        if (focusedWindow && window->root != focusedWindow->root) {
            lens_wm_setFocusedWindow(env, window);
        }
    }
}

void lens_wm_notifyScrollEvent(JNIEnv *env, int xabs, int yabs, int step) {

    int relX, relY;
    NativeWindow window = glass_window_findWindowAtLocation(xabs, yabs,
                          &relX, &relY);
    if (window != NULL) {
        glass_application_notifyScrollEvent(env, window, relX, relY,
                                            xabs, yabs, 0.0, step);
    }

}

// check for window grab then forward event to application.
// check for focus changes and handle them.
void lens_wm_notifyButtonEvent(JNIEnv *env,
                               jboolean pressed,
                               int button,
                               int xabs, int yabs) {

    int relX, relY;
    NativeWindow window;

    //cache new coordinates
    _mousePosX = xabs;
    _mousePosY = yabs;

    window = glass_window_findWindowAtLocation(xabs, yabs,
                          &relX, &relY);

    _mousePressed = pressed;

    if (_onDraggingAction) {
        if (pressed) {
            GLASS_LOG_SEVERE("Press event while on drag !");
        }

        if (_dragGrabbingWindow != NULL) {

            relX = xabs - _dragGrabbingWindow->currentBounds.x;
            relY = yabs - _dragGrabbingWindow->currentBounds.y;
            glass_application_notifyMouseEvent(env,
                                               _dragGrabbingWindow,
                                               com_sun_glass_events_MouseEvent_UP,
                                               relX, relY, xabs, yabs,
                                               button);

        }

        _onDraggingAction = JNI_FALSE;
        _dragGrabbingWindow = NULL;

    } else {
        if (window != NULL) {
            GLASS_LOG_FINEST("glass_wm_notifyButtonEvent sending to  %p pressed=%d, button=%d  %d,%d, %d, %d ",
                             window,
                             pressed, button,
                             relX, relY, xabs, yabs);

            // pass on the event to Java.
            glass_application_notifyMouseEvent(env,
                                               window,
                                               pressed ? com_sun_glass_events_MouseEvent_DOWN :
                                               com_sun_glass_events_MouseEvent_UP,
                                               relX, relY, xabs, yabs,
                                               button);
        }
    }

    handleClickOrTouchEvent(env, xabs, yabs);

}


// check for window grab then forward event to application.
// check for focus changes and handle them.
void lens_wm_notifyTouchEvent(JNIEnv *env,
                              jint state,
                              int id,
                              int xabs, int yabs) {

    int relX, relY;
    NativeWindow window;

    //cache new coordinates
    _mousePosX = xabs;
    _mousePosY = yabs;

    window = glass_window_findWindowAtLocation(xabs, yabs,
                          &relX, &relY);

    lens_wm_setMouseWindow(window);

    if (state == com_sun_glass_events_TouchEvent_TOUCH_PRESSED) {
        _mousePressed = JNI_TRUE;
    } else if (state == com_sun_glass_events_TouchEvent_TOUCH_RELEASED) {
        _mousePressed = JNI_FALSE;
    } else {
        GLASS_LOG_SEVERE("Unexpected state %d", state);
    }

    if (_mousePressed && window) {
        // Pressed on window
        glass_application_notifyMouseEvent(env,
                                           window,
                                           com_sun_glass_events_MouseEvent_ENTER,
                                           relX, relY, xabs, yabs,
                                           com_sun_glass_events_MouseEvent_BUTTON_NONE);
        glass_application_notifyTouchEvent(env,
                                           window,
                                           com_sun_glass_events_TouchEvent_TOUCH_PRESSED,
                                           id,
                                           relX, relY, xabs, yabs);
    }


    if (!_mousePressed) {
        if (!_onDraggingAction && window) {
            //Press-release on a window without a move in between.
            glass_application_notifyTouchEvent(env,
                                               window,
                                               com_sun_glass_events_TouchEvent_TOUCH_RELEASED,
                                               id,
                                               relX, relY, xabs, yabs);

        } else if (_onDraggingAction && _dragGrabbingWindow != NULL) {
            //Finished drag that started on actual window.
            relX = xabs - _dragGrabbingWindow->currentBounds.x;
            relY = yabs - _dragGrabbingWindow->currentBounds.y;
            glass_application_notifyTouchEvent(env,
                                               _dragGrabbingWindow,
                                               com_sun_glass_events_TouchEvent_TOUCH_RELEASED,
                                               id,
                                               relX, relY, xabs, yabs);
        }

        _onDraggingAction = JNI_FALSE;
        _dragGrabbingWindow = NULL;

    }

    handleClickOrTouchEvent(env, xabs, yabs);

}


void lens_wm_notifyMotionEvent(JNIEnv *env, int mousePosX, int mousePosY, int isTouch, int touchId) {

    int relX, relY;
    int reportMove = 0;
    GLASS_LOG_FINEST("Motion event: x=%03d, y=%03d", mousePosX, mousePosY);
    //cache new coordinates
    _mousePosX = mousePosX;
    _mousePosY = mousePosY;

    //update cursor if event came from pointer device
    if (!isTouch) {
        fbCursorSetPosition(mousePosX, mousePosY);
    }


    if (_mousePressed && !_onDraggingAction) {
        _onDraggingAction = JNI_TRUE;
        _dragGrabbingWindow = lens_wm_getMouseWindow();
    }

    NativeWindow window = glass_window_findWindowAtLocation(
                              _mousePosX, _mousePosY, &relX, &relY);


    NativeWindow lastMouseWindow = lens_wm_getMouseWindow();

    //Send EXIT/ENTER events
    if (_onDraggingAction && _dragGrabbingWindow != NULL) {
        if (window != _dragGrabbingWindow &&
                _dragGrabbingWindow == lastMouseWindow) {
            relX = _mousePosX - _dragGrabbingWindow->currentBounds.x;
            relY = _mousePosY - _dragGrabbingWindow->currentBounds.y;

            glass_application_notifyMouseEvent(env,
                                               _dragGrabbingWindow,
                                               com_sun_glass_events_MouseEvent_EXIT,
                                               relX, relY, _mousePosX, _mousePosY,
                                               com_sun_glass_events_MouseEvent_BUTTON_NONE);
        }

        if (window == _dragGrabbingWindow &&
                window != lastMouseWindow) {
            glass_application_notifyMouseEvent(env,
                                               _dragGrabbingWindow,
                                               com_sun_glass_events_MouseEvent_ENTER,
                                               relX, relY, _mousePosX, _mousePosY,
                                               com_sun_glass_events_MouseEvent_BUTTON_NONE);
        }
    }

    if (!_onDraggingAction) {
        if (window != lastMouseWindow) {
            if (lastMouseWindow) {
                // Exited from lastMouseWindow
                relX = _mousePosX - lastMouseWindow->currentBounds.x;
                relY = _mousePosY - lastMouseWindow->currentBounds.y;

                glass_application_notifyMouseEvent(env,
                                                   lastMouseWindow,
                                                   com_sun_glass_events_MouseEvent_EXIT,
                                                   relX, relY, _mousePosX, _mousePosY,
                                                   com_sun_glass_events_MouseEvent_BUTTON_NONE);
            }
            if (window) {
                // Enter into window
                glass_application_notifyMouseEvent(env,
                                                   window,
                                                   com_sun_glass_events_MouseEvent_ENTER,
                                                   relX, relY, _mousePosX, _mousePosY,
                                                   com_sun_glass_events_MouseEvent_BUTTON_NONE);

            }

        }
    }


    lens_wm_setMouseWindow(window);

    //Send the move event
    if (_onDraggingAction && _dragGrabbingWindow != NULL) {

        relX = _mousePosX - _dragGrabbingWindow->currentBounds.x;
        relY = _mousePosY - _dragGrabbingWindow->currentBounds.y;

        if (isTouch) {
            glass_application_notifyTouchEvent(env,
                                               _dragGrabbingWindow,
                                               com_sun_glass_events_TouchEvent_TOUCH_MOVED,
                                               touchId , relX, relY, _mousePosX, _mousePosY);
        }

        glass_application_notifyMouseEvent(env,
                                           _dragGrabbingWindow,
                                           com_sun_glass_events_MouseEvent_MOVE,
                                           relX, relY, _mousePosX, _mousePosY,
                                           com_sun_glass_events_MouseEvent_BUTTON_NONE);


    } else if (!_onDraggingAction && window != NULL) {

        if (isTouch) {
            glass_application_notifyTouchEvent(env,
                                               window,
                                               com_sun_glass_events_TouchEvent_TOUCH_MOVED,
                                               touchId , relX, relY, _mousePosX, _mousePosY);
        }

        glass_application_notifyMouseEvent(env,
                                           window,
                                           com_sun_glass_events_MouseEvent_MOVE,
                                           relX, relY, _mousePosX, _mousePosY,
                                           com_sun_glass_events_MouseEvent_BUTTON_NONE);

    }

}




/*
 * set focus to the specified window,
 * providing FOCUS_LOST as needed to previous
 */
void lens_wm_setFocusedWindow(JNIEnv *env, NativeWindow window) {

    NativeWindow focusWindow = glass_window_getFocusedWindow();

    if (focusWindow) {
        GLASS_LOG_FINER("Notifying focus lost on %p", focusWindow);
        glass_application_notifyWindowEvent(env,
                                            focusWindow,
                                            com_sun_glass_events_WindowEvent_FOCUS_LOST);
    }

    glass_window_setFocusedWindow(window);

    if (window != NULL) {
        GLASS_LOG_FINER("Notifying focus gained on %p", window);
        glass_application_notifyWindowEvent(env,
                                            window,
                                            com_sun_glass_events_WindowEvent_FOCUS_GAINED);
    }

}

/*
 * MouseWindow
 * The window that currently has the mouse in it.
 * Note, this may be NULL.
 */
static NativeWindow mouseWindow = NULL;

NativeWindow lens_wm_getMouseWindow() {
    return mouseWindow;
}

LensResult lens_wm_setMouseWindow(NativeWindow window) {
    mouseWindow = window;
    return LENS_OK;
}


static void lens_wm_clearScreen() {
    glass_screen_clear();
    lens_wm_rfbNotifyClearScreen();
}

void lens_wm_notifyWindowUpdate(NativeWindow window, int width, int height) {
    lens_wm_rfbNotifyWindowUpdate(window, width, height);
}


static void lens_wm_windowCacheBounds(NativeWindow window) {
    window->cachedBounds.x = window->currentBounds.x;
    window->cachedBounds.y = window->currentBounds.y;
    window->cachedBounds.width = window->currentBounds.width;
    window->cachedBounds.height = window->currentBounds.height;
}

static void lens_wm_windowUncacheBounds(NativeWindow window) {
    window->currentBounds.x = window->cachedBounds.x;
    window->currentBounds.y = window->cachedBounds.y;
    window->currentBounds.width = window->cachedBounds.width;
    window->currentBounds.height = window->cachedBounds.height;

}

//// RFB support
static void lens_wm_initRFB(JNIEnv *env) {
#ifdef USE_RFB
    lens_rfb_init(env);
#endif
}
static void lens_wm_rfbNotifyClearScreen() {
#ifdef USE_RFB
    NativeScreen screen = glass_screen_getMainScreen();
    lens_rfb_notifyDirtyRegion(0, 0, screen->width, screen->height);
#endif
}
static void lens_wm_rfbNotifyWindowUpdate(NativeWindow window,
                                          int width, int height) {
#ifdef USE_RFB
    NativeScreen screen = glass_screen_getMainScreen();
    int x = window->currentBounds.x;
    int y = window->currentBounds.y;

    width = x + width > screen->width ? screen->width - x : width;
    height = y + height > screen->height ? screen->height - y : height;

    lens_rfb_notifyDirtyRegion(x, y, width, height);
#endif
}
