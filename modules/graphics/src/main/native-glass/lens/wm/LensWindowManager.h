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
 
#ifndef __LENS_WINDOW_MGR_H__
#define __LENS_WINDOW_MGR_H__

#include "LensCommon.h"

/**
 * The main entry point for the lens system
 * 
 * @param env 
 * 
 * @return jboolean JNI_TRUE on success
 */
jboolean lens_wm_initialize(JNIEnv *env);

/**
 * This function will be called on JFX application shutsown,
 * which will release all all resources allocated with
 * lens_wm_initialize() and other resources allocated
 * afterwards during application runtime. This function is
 * called after all windows have been closed.
 *
 */
void lens_wm_shutdown(JNIEnv *env);

/**
 * get current pointer position
 * 
 * @param pX [out] X coordinates
 * @param pY [out] Y coordinates
 */
void lens_wm_getPointerPosition(int *pX, int *pY);

/**
 * update the pointer position. Usually called by an input 
 * provider or robot implementation 
 * 
 * @param X new X coordinate
 * @param Y new Y coordinate
 */
void lens_wm_setPointerPosition(int X, int Y);

/**
 * Update the window manager for a dirty region on a window. 
 * Used when rfb is enabled 
 * 
 * @param window the updated window
 * @param width region width
 * @param height region height
 */
void lens_wm_notifyWindowUpdate(NativeWindow window, 
                                           int width, 
                                           int height);

/**
 * Get the current grab window
 * glass_wm_setGrabbedWindow()) 
 *
 * @return Grabbed window or NULL if no window is Grabbed
 */
NativeWindow lens_wm_getGrabbedWindow();

/**
 * Set the current grab window
 *
 * @param window window or NULL 
 */
void lens_wm_setGrabbedWindow(NativeWindow window);

/**
 * Check a buttom clicked event before passing it to
 * glass_application_notifyButtonEvent
 * so that grabbed window checks can be made.
 * Note: a NULL window is appropriate as that 
 * will cause the grab window to be release.
 *
 * @param window window or NULL 
 */
void lens_wm_notifyButtonEvent(JNIEnv *env, 
                                         jboolean pressed, int button,
                                         int xabs, int yabs);

/**
 * Check a scroll event before passing it to
 * glass_application_notifyScrollEvent so that 
 * grabbed window checks can be made. 
 */
void lens_wm_notifyScrollEvent(JNIEnv *env, int xabs, 
                                          int yabs, int step);


/**
 * Process a touch or multitouch event and simulate the required
 * mouse events. Notification is done through 
 * glass_application_notifyTouchEvent or 
 * glass_application_notifyMultiTouchEvent Note: 
 *  
 * @param env 
 * 
 * @param count number of touch points
 * 
 * @param state array of states for each point
 * 
 * @param ids array of ids for each point
 * 
 * @param xabs array of X coordinates for each point
 * 
 * @param yabs array of Y coordinates for each point
 * 
 * @param primaryPointIndex the index of the point that mouse 
 *                   events will be synthesis from
 * 
 */
void lens_wm_notifyMultiTouchEvent(JNIEnv *env,
                                   int count,
                                   jint *state,
                                   jlong *ids,
                                   int *xabs, int *yabs,
                                   int primaryPointIndex);


/**
 * Check a motion event before passing it to
 * glass_application_notifyMotionEvent
 * so that enter/exit window checks can be made.
 *
 * @param window window or NULL 
 */
void lens_wm_notifyMotionEvent(JNIEnv *env,
                                          int mousePosX, 
                                          int mousePosY);

/**
 * Release any wm state related to the window
 *
 * @param window window
 */
LensResult lens_wm_notifyPlatformWindowRelease(JNIEnv *env,
                                                    NativeWindow window);

/*
 * set focus to the specified window, 
 * providing FOCUS_LOST as needed to previous
 */
void lens_wm_setFocusedWindow(JNIEnv *env, NativeWindow window);

/**
 * Get the current mouse window (as was set by 
 * glass_wm_setMouseWindow()) 
 *
 * @return Focused window or NULL if no window is focused
 */
NativeWindow lens_wm_getMouseWindow();

/**
 * Set the current mouse window
 *
 * @param window window or NULL 
 */
LensResult lens_wm_setMouseWindow(NativeWindow window);

/**
 * Cause a repaint
 *
 * @param env env
 * @param window window or NULL 
 */
void lens_wm_repaint(JNIEnv *env, NativeWindow window);

/// platform specific calls
void lens_platform_shutdown(JNIEnv *env);

/**
 * Ask the platform window manager to minimize the window.
 * NOTE: this may be NOOP in some configurations. 
 * 
 * @param env 
 *  
 * @param window the window to minimize 
 * 
 * @return LensResult LENS_OK on success
 */
LensResult lens_platform_windowMinimize(JNIEnv *env,
                                        NativeWindow window,
                                        jboolean toMinimize);

/**
 * Ask the platform window manager to set window's visibility.  
 * NOTE: this may be NOOP in some configurations.
 * 
 * @param env 
 * @param window the window to set visibility on
 * @param visible does the window need to be visible
 * 
 * @return LensResult LENS_OK on success
 */
LensResult lens_platform_windowSetVisible(JNIEnv *env,
                                        NativeWindow window,
                                        jboolean visible);

/**
 * Notification sent from 
 * LensDnDClipboard::pushToSystem()->LensApplication::notifyDragStart(), 
 * indicating Drag N' Drop has been started
 *  
 * Note: DnD events has a higher priority then mouse drag events
 * 
 */
void notify_lens_wm_DnDStarted();

/**
 * Notification sent from LensApplication::handleDragEvents() 
 * after a DROP event was generated, indicating DnD has ended.
 * 
 */
void notify_lens_wm_DnDEnded();

#endif

