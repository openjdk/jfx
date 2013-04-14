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
 
#ifndef LENS_COMMON_H
#define LENS_COMMON_H

#ifdef ANDROID_NDK
#include <stdio.h>
#endif
#include "stdlib.h"
#include "string.h"
#include <unistd.h>

#include <jni.h>

// JNI handles ******************************************
extern jclass jLensApplicationClass;
extern jmethodID jLensApplication_waitEventLoopsToFinish;
extern jmethodID mat_jWindowNotifyExpose;
extern jmethodID mat_jWindowNotifyMove;
extern jmethodID mat_jWindowNotifyResize;
extern jmethodID mat_jWindowNotifyClose;
extern jmethodID mat_jWindowNotifyFocus;

/**
 * The bellow macros will convert pointers to long
 * representation, which is how java is keeping native pointers,
 * and vice versa.
 * This code will work in both 32 and 64 bit systems
 */
#if defined (_LP64) || defined(_WIN64)
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif


#define USE_GLASS_CHECK
#ifndef USE_GLASS_CHECK
#define GLASS_CHECK_EXCEPTION(ENV)
#define GLASS_CLEAR_EXCEPTION(ENV)
#else
#define GLASS_CHECK_EXCEPTION(ENV) \
    if ((*ENV)->ExceptionCheck(ENV) == JNI_TRUE) {                                 \
        fprintf(stderr, "Glass detected outstanding Java exception at %s:%s:%d\n", \
                __FUNCTION__, __FILE__, __LINE__);                                 \
        (*ENV)->ExceptionDescribe(ENV);                                            \
        (*ENV)->ExceptionClear(ENV);                                               \
    };
#define GLASS_CLEAR_EXCEPTION(ENV) (*ENV)->ExceptionClear(ENV);
#endif

#define DEBUG_PRINTF(format,...) { \
        printf(format "\n", ##__VA_ARGS__); \
        fflush(stdout); \
    } //end of DEBUG_PRINTF

#define DEBUG_FUNC_ENTRY() GLASS_LOG_FINEST("Enter")
#define DEBUG_FUNC_EXIT() GLASS_LOG_FINEST("Exit")

/**
 * Throw exception by name
 *
 * @param env
 * @param name name of the exception (full class path), for
 *             example "java/lang/RuntimeException"
 * @param msg the message of the exception
 */
void glass_throw_exception_by_name(JNIEnv *env, const char *name, const char *msg);

#define CHECK_AND_RET_VOID(ENV)                                             \
    {                                                                       \
        if ((*ENV)->ExceptionCheck(ENV) == JNI_TRUE) {                      \
            fprintf(stderr, "erk Java exception detected in at %s:%s:%d\n", \
                    __FUNCTION__, __FILE__, __LINE__);                      \
            (*ENV)->ExceptionDescribe(ENV); (*ENV)->ExceptionClear(ENV);    \
            glass_throw_exception_by_name(env, glass_RuntimeException,      \
                                          "Error in JNI code");             \
            return;                                                         \
        }                                                                   \
    }; //end of CHECK_AND_RET_VOID

#define CHECK_AND_RET(ENV,ret)                                           \
    {                                                                    \
        if ((*ENV)->ExceptionCheck(ENV) == JNI_TRUE) {                   \
            fprintf(stderr, "Java exception detected in at %s:%s:%d\n",  \
                    __FUNCTION__, __FILE__, __LINE__);                   \
            (*ENV)->ExceptionDescribe(ENV);                              \
            (*ENV)->ExceptionClear(ENV);                                 \
            glass_throw_exception_by_name(env, glass_RuntimeException,   \
                                          "Error in JNI code");          \
            return ret;                                                  \
        };                                                               \
    }; //end of CHECK_AND_RET

/**********************************************/


/**
 * JNI references (fieldID methodID etc)
 * Initialized in LensApplication.c::initIDs()
 */

extern jclass jScreenClass;

/****************************/


extern char *glass_RuntimeException;
extern char *glass_NullPointerException;
extern char *glass_UnsupportedOperationException;

void glass_throw_exception_by_name(JNIEnv *env,
                                   const char *name,
                                   const char *msg);

//////////// Generic

/**
 * Lens porting layer error codes
 */
typedef enum _LensResult{
    LENS_OK         = 0, 
    LENS_FAILED     = 1
}LensResult;

/**                                                   
 * Data structure that define NativeWindow and NativeView bounds
 * structures 
 */                                                   
typedef struct {                                      
    int x;                                            
    int y;                                            
    int width;                                        
    int height;                                       
}LensBounds;                                          

//forward declarations for native structures
typedef struct _PlatformWindowData *PlatformWindowData; //Window's platform specific data
typedef struct _PlatformViewData *PlatformViewData; //View's platform specific data
typedef struct _NativeWindow *NativeWindow; //Pointer for native window structure
typedef struct _NativeView *NativeView; //forward decleration for native view data
typedef struct _NativeScreen *NativeScreen;


/* 
 * Utility routine to convert a char * string into a jcharArray
 * call glass_jcharArray_release to release the created reference
 *
 * @param env
 * @param string to convert
 */
jcharArray glass_util_strToJcharArray(JNIEnv *env, char *str);

/* 
 * Utility routine to release the reference created by 
 * glass_str_to_jcharArray
 * @param env
 * @param jcharArray to release
 */
void glass_util_jcharArrayRelease(JNIEnv *env, jcharArray jcharsobj);

/////////////Screen

/**
 * This data structure is used to define a native screen
 *
 * @param depth the bit depth support of the pixel format. i.e
 *              16 - for 565, 24 -for 888,32- for RGBA
 * @param x the x location of the screen
 * @param y the y location of the screen
 * @param width the width of the screen
 * @param height the height of the screen
 * @param visibleX the actual visable x
 * @param visibleY the actual visable Y
 * @param visibleWidth the actual visible width
 * @param visibleHeight the actual visible height
 * @param resolutionX the DPI setting of the X axis, use the
 *                    value of 72 as default
 * @param resolutionY the DPI setting of the Y axis, use the
 *                    value of 72 as default
 * @param data native handler of the screen
 */
struct _NativeScreen {
    int depth;
    int x, y;
    int width, height;
    int visibleX, visibleY;
    int visibleWidth, visibleHeight;
    int resolutionX, resolutionY;
    void *data;
};

/**
 * Init and cache the main screen
 * 
 * @param env 
 * 
 * @return LensResult LENS_OK on success
 */
NativeScreen lens_screen_initialize(JNIEnv *env);

/**
 * This function is used to initialize and get the parameters of
 * the main screen of the system.
 *
 * @return NativeScreenHandle* the screen parameters
 */
NativeScreen glass_screen_getMainScreen();

/**
 * Clear the screen to background before a repaint.
 * (not needed on all platforms).
 *
 */
void glass_screen_clear();

/**
 * Get access to the screen frame buffer, if available 
 * 
 * @return char* pointer to the frame buffer, NULL if not available  
 */
char *lens_screen_getFrameBuffer();

/**
 * Get a screen snapshot at the given location and return a jint
 * array containing a copy of the frame buffer.
 *
 * The pixel format of the buffer should be the same as
 * LensPixels.getNativeFormat_impl() 
 *  
 * Note: 1x1 pixels are a valid value 
 *
 * @param x the X top left coordinate of the capture area
 * @param y the Y top left coordinate of the capture area
 * @param width the width of the capture area
 * @param height the height of the capture area
 * @param pixels [OUT] pre-allocated buffer of the size
 *               width*height*sizeof(jint)
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_screen_capture(jint x,
                              jint y,
                              jint width,
                              jint height,
                              jint *pixels);


/**
 * Get an array of available screens
 */
 jobjectArray createJavaScreens(JNIEnv *env);

/////////////Aplication

/**
 * Service functions that return JavaVM instance
 * Always valid
 *
 *
 * @return JavaVM
 */
JavaVM *glass_application_GetVM();

/**
 * This is the first function that been called by the java code
 * before using the native library
 *
 * This function should initialize any resources it required
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_application_initialize(JNIEnv *env);

/**
 * This function called to see if our native platform has a window manager or not.  Most
 * platforms will return true;
 *
 */
jboolean glass_application_hasApplicationManager(JNIEnv *env);


/**
 * This is a function pointer declaration for creating a native
 * event loop.
 * Required by glass_application_request_native_event_loop()
 */
typedef void(*nativeEventLoopCallback)(JNIEnv *env,
                                       void *handle);

/**
 * Native implementation can call this function to create a
 * dedicate thread in which the event loop can run. This
 * function can be called as many time as the implementation
 * needs. (for implementations that require a separate event
 * loop for each resource that create an event. for example each
 * window in DFB implementation)
 *
 * @param env
 * @param callback a pointer to a function that will be called
 *                 when the thread will be created
 * @param handle native data structure the implementation
 *               requires
 */
void glass_application_request_native_event_loop(JNIEnv *env,
                                                 nativeEventLoopCallback callback,
                                                 void *handle);


/**
 * Inform the LensApplication singleton event thread for key
 * event
 * 
 * @param env
 * @param window window the window that received the event
 * @param type event type (com_sun_glass_events_KeyEvent_*)
 * @param jfxKeyCode key code for the event 
 *                   (com_sun_glass_events_KeyEvent_VK_*)
 * @param isRepeatEvent JNI_TRUE when the event is a repeat 
 *                      event
 *                  
 */
void glass_application_notifyKeyEvent(JNIEnv *env, 
                                      NativeWindow window,
                                      int type,
                                      int jfxKeyCode,
                                      jboolean isRepeatEvent);

/**
 * Notifications for mouse events
 * 
 * @param env 
 * @param window the window that received the event
 * @param eventType one of com_sun_glass_events_MouseEvent_
 * @param x position relative to window
 * @param y position relative to window
 * @param absx absolute position on screen
 * @param absy absolute position on screen
 * @param button button code for button reltaed events
 * @param modifiers MANDATORY - mask of currently pressed 
 *                  special keys and mouse buttons.
 */
void glass_application_notifyMouseEvent(JNIEnv *env,
                                        NativeWindow window,
                                        int eventType,
                                        int x,
                                        int y, 
                                        int absx,
                                        int absy,
                                        int button);

/**
 * Notify LensApplication of scroll event
 * 
 * @param env
 * @param window A reference to LenApplication object
 * @param x mouse x position relative to window
 * @param y mouse y position relative to window
 * @param xabs mouse x absolute position on screen
 * @param yabs mouse y absolute position on screen
 * @param dx horizontal scroll delta
 * @param dy vertical scroll delta
 * @param modifiersMask mask of currently pressed pecial keys 
 *                      and mouse buttons.
 */
void glass_application_notifyScrollEvent(JNIEnv *env,
                                         NativeWindow window,
                                         int x,
                                         int y,
                                         int xabs,
                                         int yabs,
                                         jdouble dx,
                                         jdouble dy);

 /**
 * Notify LensApplication of a touch event
 *
 * @param env
 * @param lensApplication A reference to LenApplication object
 * @param lensWindow A reference to LensWindow object
 * @param state the state of the finger location (e.g.
 * TouchEvent.TOUCH_PRESSED)
 * @param id The event ID 
 * @param x x position relative to window
 * @param y y position relative to window
 * @param xabs x absolute position
 * @param xabs y absolute position
 */
void glass_application_notifyTouchEvent(JNIEnv *env,
                                        NativeWindow window,
                                        jint state,
                                        jlong id,
                                        int x,
                                        int y,
                                        int xabs,
                                        int yabs);




/**
 * This notification handle resize/restore/minimize/maximize
 * events. Name was taken from the event handler in Window.java.
 *
 * @param env
 * @param LensApplication a reference for LenApplication object
 * @param NativeWindow for the event
 * @param eventType one of:
 *                  com_sun_glass_events_WindowEvent_RESTORE,
 *                  com_sun_glass_events_WindowEvent_MAXIMIZE,
 *                  com_sun_glass_events_WindowEvent_MINIMIZE,
 *                  com_sun_glass_events_WindowEvent_RESIZE
 * @param width new window width
 * @param height new window height
 */
void glass_application_notifyWindowEvent_resize(JNIEnv *env,
                                                NativeWindow window,
                                                int  eventType,
                                                int width,
                                                int height);
/**
 * Window have moved.
 * Inform the LensApplication singleton event thread for window
 * event
 *
 * @param env
 * @param LensApplication a reference for LenApplication object
 * @param NativeWindow for the event
 * @param x new x postion of the window
 * @param y new y postion of the window
 */
void glass_application_notifyWindowEvent_move(JNIEnv *env,
                                              NativeWindow window,
                                              int x,
                                              int y);



/**
 * This is a 'generic' window events handler that handle events
 * that doesn't require parameters for the notification. Those
 * include notifications such as focus gained/lose and window
 * close/destroy
 *
 * @param env
 * @param LensApplication a reference for LenApplication object
 * @param lensWindow      a reference for the window object that
 *                        own this event
 * @param focusEvent type of event
 *                   (com_sun_glass_events_WindowEvent_*).
 *
 */
void glass_application_notifyWindowEvent(JNIEnv *env,
                                         NativeWindow window,
                                         int windowEvent);

/**
 * Notify view for events
 *
 * Events are listed in com_sun_glass_events_ViewEvent.h
 *
 *
 * @param env
 * @param lensApplication a reference for LenApplication objec
 * @param lensWindow  a reference for the window object which
 *                    the view belongs to
 * @param viewEventType one of the events listed in
 *                      com_sun_glass_events_ViewEvent.h
 */
void glass_application_notifyViewEvent(JNIEnv *env,
                                       NativeView view,
                                       int viewEventType,
                                       int x,
                                       int y,
                                       int width,
                                       int height);

/** 
 * Notify the view for context menu hint, usually mouse right 
 * click or some keyboard sequence 
 * 
 * @param env
 * @param view the view to notify
 * @param x relative to the view
 * @param y relative to the view
 * @param xAbs relative to the screen
 * @param yAbs relative to the screen
 * @param isKeyboardTrigger true if generated by keyboard 
 *                          sequence
 */
void glass_application_notifyMenuEvent(JNIEnv *env,
                                       NativeView view,
                                       int x, int y, int xAbs, int yAbs,
                                       jboolean isKeyboardTrigger);


/**
 * Notify when input devices are attached and detached
 *
 * @param env
 * @param flags a bitmask of device flags
 * @param attach whether the device is attached or detached
 */
void glass_application_notifyDeviceEvent(JNIEnv *env,
                                         jint flags,
                                         jboolean attach);

/**
 * Call Window.java::add(Window window) private method that adds 
 * a window to the visible window list. 
 * Windows on that list are the actual windows rendered by 
 * Quantum/Prism 
 * 
 * @param env 
 * @param window the window to add
 */
void glass_application_addWindowToVisibleWindowList(JNIEnv *env,
                                                    NativeWindow window);

/**
 * Call Window.java::remove(Window window) private method that 
 * removes a window from the visible window list. 
 * Windows on that list are the actual windows rendered by 
 * Quantum/Prism 
 * 
 * @param env 
 * @param window the window to remove
 */
void glass_application_RemoveWindowFromVisibleWindowList(JNIEnv *env,
                                          NativeWindow window);

/////////////Window
/**
 * NWS stands for Native Window State
 * the enum holds all the possible state of a window.
 * Window can have only one state at a given time
 */
typedef enum _NativeWindowState{
    NWS_NORMAL,
    NWS_MINIMIZED,
    NWS_MAXIMIZED,
    NWS_FULLSCREEN
} NativeWindowState;

/**                                                                      
 * This data structure represent the native information
 * associated with the Glass window (FX stage). Window can have 
 * only one view attached to it in given time, or no view at all
 */                                                                      
struct _NativeWindow {                                           
    //reference for LensWindow object                                    
    jobject lensWindow;
    
    //if the window had an owner it means the window is a sub window, usualy a 
    //'pop-up'
    NativeWindow owner;

    //the screen which the window belong to (will be required 
    //for multi-screen support)
    NativeScreen screen;
                                                                         
    //id of this window (should be set by the platform)                  
    int id;
    //the current opacity of the window
    unsigned char opacity;

    // creation mask
    int creationMask;
                                                                         
    LensBounds currentBounds;                                            
    //cached bound is used when window is changed to                     
    //minimized/maximized/fullscreen state. When restoring a window those
    //values are used                                                    
    LensBounds cachedBounds;
    
    //indicate whether the window should read and process events.
    //usually used when each window have its own event loop handler 
    //and may be used latter for enabling/disabling windows
    jboolean processEvents;                                           
                                                                         
    //window restrictions                                                
    int minWidth;                                                        
    int maxWidth;                                                        
    int minHeight;                                                       
    int maxHeight;                                                       
                                                                         
    NativeWindowState state;                                      
                                                                         
    //windows link list                                                  
    NativeWindow previousWindow;                                
    NativeWindow nextWindow;                                    
                                                                         
    //changed according to different platforms                           
    PlatformWindowData data;                                     
                                                                         
    //Windows view (will be set through attachViewToWindow)              
    NativeView view;                                             

    //root window of this tree - the window with no owner
    //will match this NativeWindow if is the root.
    NativeWindow root;
                                                                         
    float alpha;

    jboolean hideCursorInFulscreen;                                      

    jboolean isFocusable;
    jboolean isVisible;
    jboolean isEnabled;

                                                                         
};                                                           

/**
 * Service function to convert NativeWindowState to a string
 *
 * @param nativeWindowState the NativeWindowState to convert
 *
 * @return char* String representation of NativeWindowState
 */
char *lens_window_getNativeStateName(NativeWindowState state);

/**
 * Service function that release _NativeWindow resources and 
 * calling glass_window_PlatformWindowRelease to release 
 * platform resources 
 * 
 * @param env 
 * @param window 
 * 
 * @return LensResult LENS_OK on success
 */
LensResult glass_window_NativeWindow_release(JNIEnv *env, NativeWindow window);

/**
 * Create and initialize any platform specific window related 
 * resources 
 * 
 * @param env
 * @param window as created in
 *               Java_com_sun_glass_ui_lens_LensWindow__1createWindow
 * @return LensResult LENS_OK on success
 */
LensResult glass_window_PlatformWindowData_create(JNIEnv *env,
                                                  NativeWindow window);

/**
 * Release the platform window resources 
 *  
 * @param data As was created by
 *                            glass_window_PlatformWindowData_create()
 * 
 * @return LensResult LENS_OK on success
 */
LensResult glass_window_PlatformWindowRelease(JNIEnv *env, NativeWindow window);

/**
 * Set window visible \ hidden
 *
 * Basically window is expected to be hidden until this function
 * is called with isVisible == JNI_TRUE
 *
 *
 * @param env env
 * @param windowHandle As was created by createNativeWindow()
 * @param isVisible JNI_TRUE - window visible, JNI_FALSE -
 *                  window hidden
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setVisible(JNIEnv *env,
                                 NativeWindow window,
                                 jboolean isVisible);

/**
 * The window opacity relative to its background
 *
 * @param env env
 * @param window NativeWindow
 * @param alpha the alpha level 0 - full transparent 1- solid
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setAlpha(JNIEnv *env,
                               NativeWindow window,
                               float alpha);

/**
 * Window level means the 'priority' of the window. Such as
 * always on top, normal etc.
 * The possible values of jint level are defined in class
 * com_sun_glass_ui_Window_Level.h
 *
 * @param window NativeWindow
 * @param level window level as defined in
 *              com_sun_glass_ui_Window_Level.h
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setLevel(NativeWindow window,
                               int level);


/**
* Change the window size and/or position
* Change in size may be the window content size w/o decorations
* or the total window size
*
* @param env used for the notification
* @param window NativeWindow
* @param x the position of the window X
* @param y the position of the window Y
* @param width of window/content
* @param height of window/content
* @param toUpdatePostion is x&y are valid, if not resize only
* @param toUpdateSize is width&height are valid, if not move
*                     only
* @param isContentSize does width&height refer to the content
*                      size or the whole window including
*                      decorations
*/
void glass_window_setBoundsImpl(JNIEnv *env,
                                NativeWindow window,
                                jint x,
                                jint y,
                                jint width,
                                jint height,
                                jboolean needToUpdatePostion,
                                jboolean needToUpdateSize,
                                jboolean isContentSize);

/**
 * Request one of WindowEvent.FOCUS_LOST, FOCUS_GAINED,
 * FOCUS_GAINED_FORWARD, FOCUS_GAINED_BACKWARD on the given
 * window
 *
 *
 * @param env
 * @param window the NativeWindow
 * @param focusType one of FOCUS_LOST, FOCUS_GAINED,
 *                  FOCUS_GAINED_FORWARD, FOCUS_GAINED_BACKWARD
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_requestFocus(JNIEnv *env,
                                   NativeWindow window,
                                   jint focusType);

/**
 * Enable/Disable focus events on a window
 *
 *
 * @param env  env
 * @param window  NativeWindow
 *                            createNativeWindow()
 * @param isFocusable JNI_TRUE focus events enabled on the
 *                    window. JNI_FALSE disabled
 *
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setFocusable(JNIEnv *env,
                                   NativeWindow window,
                                   jboolean isFocusable);


/**
 *
 * Set the window background color (no alpha)
 *
 * @param window NativeWindow
 * @param red value from 0-1.0
 * @param green value from 0-1.0
 * @param blue value from 0-1.0
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setBackground(NativeWindow window,
                                    jfloat red,
                                    jfloat green,
                                    jfloat blue);

/**
 * Put window on top of other windows.
 *
 *
 * @param env env
 * @param window NativeWindow
 *                            createNativeWindow()
 */
void glass_window_toFront(JNIEnv *env, NativeWindow window);

/**
 * Put window bellow all other windows.
 *
 *
 * @param env env
 * @param window NativeWindow
 */
void glass_window_toBack(JNIEnv *env, NativeWindow window);


/**
 *  Grab means:
 *  - all keyboard events are delivered to the grabbed window,
 *    as it will be the window that own the focus
 *  - window will not lose focus on mouse leave
 *  - mouse events are delivered as usual to sibling windows
 *  - glass_window_setFocusable() need to be implemented to make
 *    this API work, as siblings window, mainly pop-ups, should
 *    not be focusable
 *  - when a mouse click is occur outside the grabbed window or
 *    outside of one of its sibling windows, including system
 *    area, the grab need to be reset and a
 *    com_sun_glass_events_WindowEvent_FOCUS_UNGRAB event need
 *    to be sent to the grabbed window
 *
 * @param env
 * @param window NativeWindow
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_grabFocus(JNIEnv *env, NativeWindow window);

/**
 * An explicit request to release the grab that was captured
 * using glass_window_grabFocus() and:
 * - send com_sun_glass_events_WindowEvent_FOCUS_UNGRAB event to
 *   the grabbed window
 * - unbind keyboard events to the grabbed window
 * - reset focus behavior if was changed
 *
 * @param env
 * @param window NativeWindow
 */
void glass_window_ungrabFocus(JNIEnv *env, NativeWindow window);

/**
 * Set the minimum size of the window. Window resize should take
 * into account this value
 *
 * @param env
 * @param window NativeWindow
 * @param width the minimum width of the window
 * @param height the minimum height of the window
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setMinimumSize(JNIEnv *env,
                                     NativeWindow window,
                                     jint width, jint height);

/**
 *
 * Set the maximum size of the window. Window resize should
 * take into account this value
 *
 * @param env
 * @param window NativeWindow
 * @param width the maximum width of the window
 * @param height the maximum height of the window
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_setMaximumSize(JNIEnv *env,
                                     NativeWindow window,
                                     jint width, jint height);


/**
 * This service function checks if given width and height can be
 * applied to the given window by comparing them to the window's 
 * max and min attributes that were set by 
 * glass_window_setMaximumSize() and
 * glass_window_setMinimumSize().
 * If given values are out of bounds they will be update with
 * the closest in bounds values to match the restrictions.
 * 
 * 
 * @param window the window to check
 * @param width [IN/OUT] the requested width of a window. Will
 *              be changed to value within bounds
 * @param height [IN/OUT] the requested height of a window. Will
 *              be changed to value within bounds
 * @return JNI_TRUE means values are valid, JNI_FALSE means 
 *         values are not valid and have been updated.
 * 
 */
jboolean glass_window_check_bounds(NativeWindow window,
                                   int *width,
                                   int *height);

/**
 * Maximize / Restore window
 *
 * state machine
 *
 *    toMaximize  : isMaximized : operation
 *    ======================================
 *      false    :    false    : NoOp
 *      false    :     true    : Restore
 *      true     :     false   : Maximize
 *      true     :     true    : NoOp
 *
 * @param env
 * @param nativeWindowHandle as was created by
 *                            createNativeWindow()
 * @param toMaximize maximize / restore
 * @param isMaximized JNI_TRUE if window already maximize
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_maximize(JNIEnv *env,
                               NativeWindow window,
                               jboolean toMaximize,
                               jboolean isMaximized);

/**
 * Minimize / Restore window
 *
 * @param env
 * @param nativeWindowHandle as was created by
 *                            createNativeWindow()
 * @param toMinimize minimize / restore
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_window_minimize(JNIEnv *env,
                               NativeWindow window,
                               jboolean toMinimize);

/**
 * Get the first (farthest) window in the window list.
 *
 * @return First window or NULL for empty list
 */
NativeWindow glass_window_list_getHead();

/**
 * Get the last (closest)  window in the window list.
 *
 * @return last window or NULL for empty list
 */
NativeWindow glass_window_list_getTail();

/**
 * Reorder window list putting Window in the Front
 *
 * @param window window
 */
jboolean glass_window_list_toFront(NativeWindow window);

/**
 * Reorder window list putting Window in the back
 *
 * @param window window
 */
jboolean glass_window_list_toBack(NativeWindow window);

/**
 * Add a window to the window list
 *
 * @param window window
 */
void glass_window_list_add(NativeWindow window);

/**
 * Remove a window from the window list
 *
 * @param window window
 */
void glass_window_list_remove(NativeWindow window);

/**
 * Print the content of the windows list to the console. Used 
 * for debugging. 
 */
void glass_window_listPrint();

/**
 * Get the current focused window (as was set by 
 * glass_window_setFocusedWindow()) 
 *
 * @return Focused window or NULL if no window is focused
 */
NativeWindow glass_window_getFocusedWindow();

/**
 * Set the current focused window
 *
 * @param window window or NULL 
 */
LensResult glass_window_setFocusedWindow(NativeWindow window);

/**
 * Service function that will reset the focused window, i.e 
 * glass_window_getFocusedWindow() will return NULL, if its 
 * equal to the provided window 
 * 
 * @param window if focused window is equal to this window the 
 *               focus will be reset
 * 
 * @return LensResult LENS_OK on success 
 */
LensResult glass_window_resetFocusedWindow(NativeWindow window);

/**
 * Gets the handle used by Prism to render to a window.
 *
 */
void *glass_window_getPlatformWindow(JNIEnv *env, NativeWindow window);

/**
 * Determines which window receives a pointer event at the given location. This
 * will usually be the top level window, but can also be another window that has
 * grabbed the focus.
 *
 * @return the window that should receive a mouse event at the given location.
 * If no window was found, returns NULL.
 * On succcess, translates absX and absY into cordinates relative to the top
 * left of the window and returns the relative values in pRelX and pRelY.
 */
NativeWindow glass_window_findWindowAtLocation(int absX,
                                          int absY,
                                          int *pRelX,
                                          int *pRelY);

/////////// View

struct _NativeView {
    jobject lensView;
    NativeWindow parent;
    LensBounds bounds;
    PlatformViewData data;
};



/**
 * Create platform specific data for view. 
 *  
 * The platform should allocate and initiliaze any resource that 
 * it requires and register it to the view->data field. 
 *  
 * 
 * @param view Allocated and initialized NativeView
 * 
 * @return LensResult LENS_OK on success
 * 
 */
LensResult glass_view_PlatformViewData_create(NativeView view);

/**
 * Release PlatformViewData resources
 * 
 * 
 * @param env env
 * @param data as created by
 *                         glass_view_PlatformViewData_create
 * 
 * @return LensResult LENS_OK on success
 */
LensResult glass_view_PlatformViewRelease(JNIEnv *env, NativeView view);

/**
 * Service funtion to close NativeView
 * 
 * @param env env
 * @param view as created in 
 *             Java_com_sun_glass_ui_lens_LensView__1createNativeView
 * 
 * @return LensResult LENS_OK on success
 */
LensResult glass_view_releaseNativeView(JNIEnv *env, NativeView view);
/**
 * This function tell the view (and its associated surface) to
 * enter draw stage.
 * This may result in locking the surface or some other
 * operation that draw requires
 *
 * @param view NativeView
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_view_drawBegin(NativeView view);

/**
 * This is the complementary function to viewBegin().
 * i.e unlocking the surface for other drawing or screen update
 *
 * @param view NativeView
 */
void glass_view_drawEnd(NativeView view);


/**
 *
 * As view class is created with no context to a window class,
 * this method is called to connect between the two and to
 * provide the native window with a native view context
 * In practice  - Attach or Detach the view from  owner window
 *
 *
 * @param nativeViewHandle the native view to update
 * @param nativeWindowHandle new window handle, can be NULL
 */
void glass_view_setParent(JNIEnv* env,
                          NativeWindow parent,
                          NativeView view);

/**
 * Enter full screen
 * need to notify view with
 * com_sun_glass_events_ViewEvent_FULLSCREEN_ENTER event on
 * completion
 * 
 * 
 * @param env
 * @param view as created by
 *                         Java_com_sun_glass_ui_lens_LensView__1createNativeView
 * @param animate should transformation will be animated
 * @param keepRatio keep the same ratio of the surface
 * @param hideCursor when JNI_TRUE
 * 
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_view_enterFullscreen(JNIEnv* env,
                                    NativeView view,
                                    jboolean animate,
                                    jboolean keepRatio,
                                    jboolean hideCursor);

/**
 * Exit Fullscreen mode
 * need to notify view with
 * com_sun_glass_events_ViewEvent_FULLSCREEN_EXIT event on
 * completion
 *
 * @param env
 * @param view NtiveView
 * @param animate should transformation will be animated
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_view_exitFullscreen(JNIEnv *env,
                                   NativeView view,
                                   jboolean animate);

/**
 * Calculate the biggest surface possible to fit full screen,
 * while keeping the original surface ratio required for 
 * glass_view_enterFullscreen when keepRatio is true 
 *  
 * The function will update the NativeView::bounds field to 
 * represent the new bounds 
 * 
 * 
 * @param screen  [IN] screen information
 * @param view [IN/OUT] current surface
 */
void glass_view_fitSurfaceToScreen(NativeScreen screen,
                                   NativeView view);


/////////// Pixel

/**
 * This function is used to attach/dump image buffer, i.e
 * rendered pixels, to the native window surface.
 *
 * For performance reasons, the actual retrieval of the buffer
 * data from the java layer will need to be done by the
 * implementation itself. The reason for it is because when the
 * native code asks for a read access to the actual buffer, the
 * entire VM is suspended. Therefore the implementation should
 * release the buffer as soon as possible so other java threads
 * will continue to run. Further more the implementation should
 * use a reference of the array and not an actual copy of it.
 * again for performance reasons. The JNI functions to be used
 * are GetPrimitiveArrayCritical, which lock the VM, and
 * ReleasePrimitiveArrayCritical that frees it. see JNI
 * documentation for more information and usage.
 *
 * @param env
 * @param array jint* of pixels 
 * @param window
 * @param width
 * @param height
 * @param offset
 */
void glass_pixel_attachIntBuffer(JNIEnv *env,
                                 jint* array,
                                 NativeWindow window,
                                 jint width,
                                 jint height,
                                 int offset);




/////////// LensRobot


/**
 * Post scroll event to current window
 *
 * @param wheelAmt - wheeling direction - 
 *                 com_sun_glass_ui_Robot_WHEEL_UP/WHEEL_DOWN
 *  
 * @param x - x coordinate required for scroll event 
 *  
 * @param y - y coordinate required for scroll event 
 *  
 * @return jboolean JNI_TRUE on success 
 */
jboolean glass_robot_postScrollEvent(JNIEnv *env,
                                     jint wheelAmt);

/**
 * Post the given key event to the native event queue
 *
 * @param keyEventType
 *                     com_sun_glass_events_KeyEvent_PRESS/RELEASE
 * @param jfxKeyCode The JFX kecode as described in 
 *                   com_sun_glass_events_KeyEvent.h
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_robot_postKeyEvent(JNIEnv *env,
                                  jint keyEventType,
                                  jint jfxKeyCode);

/**
 * Post mouse event on the native queue
 *
 * @param mouseEventType
 *                        com_sun_glass_events_MouseEvent_UP/DOWN/MOVE
 * @param x relevant com_sun_glass_events__MouseEvent_MOVE only
 * @param y relevant com_sun_glass_events__MouseEvent_MOVE only
 * @param buttons relevant
 *                 com_sun_glass_events__MouseEvent_UP/DOWN only
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean glass_robot_postMouseEvent(JNIEnv *env,
                                    jint mouseEventType,
                                    jint x,
                                    jint y,
                                    jint buttons);

/**
 * Get the current location of the mouse
 *
 *
 * @param x [OUT] the X coordinate
 * @param y [OUT] the Y coordinate
 *
 * @return jboolean JNI_TRUE on success
 */
jboolean  glass_robot_getMouseLocation(jint *x, jint *y);



/////////// Input events

/**
 * Currently all platforms shares the same key maps based on linux 
 * kernel key codes. 
 *  
 * The functions bellow are utility functions to retrieve information 
 * from that table. 
 *  
 * If a future platform will not be able to use this services, the service 
 * should be extended to support proprietary platform key codes and use the same 
 * functions provided bellow. 
 *  
 * Note: unicode is not supported 
 */

/**
 * Most of mouse and key events requires, and some times 
 * depends, on the modifiers mask defined in 
 * com_sun_glass_events_KeyEvent_MODIFIER_* 
 *  
 * The functions bellow provides and API to manage them 
 */

/**
 * Return the VK code represent the given character on the platform, 
 * if available. 
 * Return com_sun_glass_events_KeyEvent_VK_UNDEFINED otherwise. 
 *  
 * NOTE: currently unicode is not supported 
 * 
 * @param c the charcter requied
 * 
 * @return int 
 */
int glass_inputEvents_getJavaKeyCodeFromJChar(jchar c);

/**
 * Return the string represtation of a key. Modifiers are 
 * respected. 
 * 
 * @param jfxKeyCode one of 
 *                   com_sun_glass_events_KeyEvent_VK_*
 * @param keyStr [OUT] the string
 * 
 * @return LensResult JNI_TRUE on success
 */
LensResult glass_inputEvents_getKeyChar(int jfxKeyCode, char **keyStr);

/**
 * Check if either to use the shift version ('upper case') or 
 * normal version('lower case') of a key based on current 
 * modifiers mask including caps lock. 
 * 
 * @param keyCode one of 
 *                   com_sun_glass_events_KeyEvent_VK_*
 * 
 * @return jboolean JNI_TRUE if key should represented by the 
 *         'shifted' version
 */
jboolean glass_inputEvents_checkForShift(int keyCode);

/**
 * Translate platform key code to JFX key code. 
 *  
 * Note: Currently only linux kernel key codes are supported. 
 * This method and the data structures its depends on should be 
 * changed if more platform code should be supported 
 * 
 * @param platformKeyCode 
 * 
 * @return int one of 
 *                   com_sun_glass_events_KeyEvent_VK_*
 */
int glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(int platformKeyCode);

/**
 * Update modifiers related to key events. Unrelated key events
 * will be ignored, so its safe to call this for any key event.
 * 
 * @param keyCode com_sun_glass_events_KeyEvent_VK_* 
 * @param eventType one of com_sun_glass_events_KeyEvent_
 * 
 * @param keyCode 
 * @param eventType 
 */
void glass_inputEvents_updateKeyModifiers(int keyCode, int eventType);

/**
 * Update modifiers related to mouse events. 
 * Unrelated events will be ignored so its safe to call this for
 * any mouse event. 
 * 
 * 
 * @param button one of com_sun_glass_events_MouseEvent_BUTTON_
 * @param eventType one of com_sun_glass_events_MouseEvent_*
 */
void glass_inputEvents_updateMouseButtonModifiers(int button, int eventType);

/**
 * Get current modifiers mask which is a bit mask of 
 * com_sun_glass_events_KeyEvent_MODIFIER_* 
 * 
 * @return int current mask (can be 
 *         com_sun_glass_events_KeyEvent_MODIFIER_NONE)
 */
int glass_inputEvents_getModifiers();

/**
 * Check if current key is considered as a modifier
 * 
 * @param jfxKeyCode key to check. one of 
 *                   com_sun_glass_events_KeyEvent_VK_*
 * 
 * @return jboolean JNI_TRUE if its a modifier
 */
jboolean glass_inputEvents_isKeyModifier(int jfxKeyCode);






/////////// Cursor
/**
 * Set the cursor to the one specified by ptr.  
 * 
 * @param ptr a pointer/handle to a custom cursor returned by 
 *  glass_cursor_createNativeCursor().
 */
void glass_cursor_setNativeCursor(jlong ptr);

/**
 * Release the cursor specified by ptr.  
 * 
 * @param ptr a pointer/handle to a custom cursor returned by 
 *  glass_cursor_createNativeCursor().
 */
void glass_cursor_releaseNativeCursor(jlong ptr);


/**
 * Create a cursor represented by srcArray, pointing to (x,y)
 * 
 * @param x the the cursor x hotspot. 
 * @param y the the cursor y hotspot.
 * @param srcArray the image of the cursor - ARGB. 
 * @param width the width of the cursor.
 * @param height the height of the cursor.
 */
jlong glass_cursor_createNativeCursor(JNIEnv *env,jint x, jint y,  
                                      jbyte* srcArray, jint width, jint height);

/**
 * Set the cursor to be visible or not.
 * 
 * @param isVisible true iff the cursor should be seen. 
 */
void glass_cursor_setVisible(jboolean isVisible);

/**
 * Return true iff the underlying platform supports translucency.
 */
jboolean glass_cursor_supportsTranslucency(void);

/**
 * Called when the application exit.
 */
void glass_cursor_terminate(void);



/////////// Logging

/**
 * Initialize the Glass logger
 *
 * Called from JNI_OnLoad
 */
void glass_logger_init();

/**
 * Log a message at the given logging level.
 * Not used directly. GLASS_LOG should be used instead.
 */
void glass_logf(int level,
                const char *func,
                const char *file,
                int line,
                const char *format, ...);


/**
 * Write a C and Java backtrace to stderr
 */
void glass_backtrace();

/**
 * The logging level.
 * Not used directly. GLASS_LOG and GLASS_IF_LOG should be used instead.
 */
extern jint glass_log_level;

/**
 * Begins a conditional statement that is only run if the current logging level
 * is less than or equal to "level".
 * For example, GLASS_IF_LOG(LOG_WARNING) { f(); } will call f() if and only if
 * the current logging settings include printing warning messages.
 * @param level The logging level to be tested against.
 */
#define GLASS_IF_LOG(level) if (level >= glass_log_level)

/**
 * Logs a message at the given logging level
 * @param level the logging level (e.g. LOG_WARNING)
 * @param ... a format string and parameters in printf format
 */
/** Logging levels, with same meanings as in java.util.logging.Level */
#define GLASS_LOG_LEVEL_SEVERE  1000
#define GLASS_LOG_LEVEL_WARNING 900
#define GLASS_LOG_LEVEL_INFO    800
#define GLASS_LOG_LEVEL_CONFIG  700
#define GLASS_LOG_LEVEL_FINE    500
#define GLASS_LOG_LEVEL_FINER   400
#define GLASS_LOG_LEVEL_FINEST  300

#ifdef ANDROID_NDK
// Can't use java logger in jvm8 on Android. Remove when this issue is fixed.
#include <android/log.h>
#define TAG "GLASS"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __VA_ARGS__))
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, __VA_ARGS__))
#define GLASS_LOG(level,...) \
        LOGI(TAG, __VA_ARGS__)
#else
#define GLASS_LOG(level,...) \
    GLASS_IF_LOG(level) \
    glass_logf(level, __func__, __FILE__, __LINE__, __VA_ARGS__)

#define GLASS_IF_LOG_SEVERE  GLASS_IF_LOG(GLASS_LOG_LEVEL_SEVERE)
#define GLASS_IF_LOG_WARNING GLASS_IF_LOG(GLASS_LOG_LEVEL_WARNING)
#define GLASS_IF_LOG_INFO    GLASS_IF_LOG(GLASS_LOG_LEVEL_INFO)
#define GLASS_IF_LOG_CONFIG  GLASS_IF_LOG(GLASS_LOG_LEVEL_CONFIG)
#define GLASS_IF_LOG_FINE    GLASS_IF_LOG(GLASS_LOG_LEVEL_FINE)
#define GLASS_IF_LOG_FINER   GLASS_IF_LOG(GLASS_LOG_LEVEL_FINER)
#define GLASS_IF_LOG_FINEST  GLASS_IF_LOG(GLASS_LOG_LEVEL_FINEST)
#endif

#ifdef NO_LOGGING
#define GLASS_LOG_SEVERE(...)  (void)0, ##__VA_ARGS__
#define GLASS_LOG_WARNING(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_INFO(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_CONFIG(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_FINE(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_FINER(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_FINEST(...) (void)0, ##__VA_ARGS__
#else
#define GLASS_LOG_SEVERE(...) GLASS_LOG(GLASS_LOG_LEVEL_SEVERE, __VA_ARGS__)
#define GLASS_LOG_WARNING(...) GLASS_LOG(GLASS_LOG_LEVEL_WARNING, __VA_ARGS__)
#define GLASS_LOG_INFO(...) GLASS_LOG(GLASS_LOG_LEVEL_INFO, __VA_ARGS__)
#define GLASS_LOG_CONFIG(...) GLASS_LOG(GLASS_LOG_LEVEL_CONFIG, __VA_ARGS__)
#define GLASS_LOG_FINE(...) GLASS_LOG(GLASS_LOG_LEVEL_FINE, __VA_ARGS__)
#define GLASS_LOG_FINER(...) GLASS_LOG(GLASS_LOG_LEVEL_FINER, __VA_ARGS__)
#define GLASS_LOG_FINEST(...) GLASS_LOG(GLASS_LOG_LEVEL_FINEST, __VA_ARGS__)
#endif

#endif
