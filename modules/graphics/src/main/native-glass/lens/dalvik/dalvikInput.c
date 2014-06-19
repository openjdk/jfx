/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

#if (defined(ANDROID_NDK) && defined(DALVIK_VM))

#include <sys/types.h>
#include <dlfcn.h>
#include "dalvikInput.h"
#include "dalvikUtils.h"
#include "../wm/LensWindowManager.h"
#include "com_sun_glass_ui_lens_LensApplication.h"
#include "com_sun_glass_events_TouchEvent.h"    
#include "com_sun_glass_ui_android_SoftwareKeyboard.h"
#include "com_sun_glass_ui_android_Activity.h"
#include "javafxports_android_FXActivity_InternalSurfaceView.h"
#include "com_sun_glass_ui_android_DalvikInput.h"

#define THROW_RUNTIME_EXCEPTION(ENV, MESSAGE, ...)                          \
    char error_msg[256];                                                    \
    sprintf(error_msg, MESSAGE, __VA_ARGS__);                               \
    if (env) {                                                              \
      (*ENV)->ThrowNew(ENV,                                                 \
          (*ENV)->FindClass(ENV, "java/lang/RuntimeException"), error_msg); \
    }                                                                       

#ifdef DEBUG
    static void *get_check_symbol(JNIEnv *env, void *handle, const char *name) {
        void *ret = dlsym(handle, name);
        if (!ret) {
            THROW_RUNTIME_EXCEPTION(env, "Failed to load symbol %s", name);
        }
        return ret;
    }
    #define GET_SYMBOL(env, handle,name) get_check_symbol(env, handle,name)
#else // #ifdef DEBUG
    #define GET_SYMBOL(env, handle,name) dlsym(handle,name)
#endif

#define ANDROID_LIB   "libactivity.so"
    
static int bind = 0;

static ANativeWindow *(*_ANDROID_getNativeWindow)();
static char          *(*_ANDROID_getDataDir)();
static void          *(*_ANDROID_notifyGlassStarted)();
static void          *(*_ANDROID_notifyGlassShutdown)();
static void          *(*_ANDROID_notifyShowIME)();
static void          *(*_ANDROID_notifyHideIME)();

void bind_activity(JNIEnv *env) {
    GLASS_LOG_FINEST("Binding to %s", ANDROID_LIB);
    void *libandroid = dlopen(ANDROID_LIB, RTLD_LAZY | RTLD_GLOBAL);
    if (!libandroid) {
        THROW_RUNTIME_EXCEPTION(env, "dlopen failed with error: ", dlerror());
        return;
    }
    
    _ANDROID_getNativeWindow = GET_SYMBOL(env, libandroid, "android_getNativeWindow");
    _ANDROID_getDataDir = GET_SYMBOL(env, libandroid, "android_getDataDir");
    _ANDROID_notifyGlassStarted = GET_SYMBOL(env, libandroid, "android_notifyGlassStarted");
    _ANDROID_notifyGlassShutdown = GET_SYMBOL(env, libandroid, "android_notifyGlassShutdown");
    _ANDROID_notifyShowIME = GET_SYMBOL(env, libandroid, "android_notifyShowIME");
    _ANDROID_notifyHideIME = GET_SYMBOL(env, libandroid, "android_notifyHideIME");
    bind = JNI_TRUE;
}

ANativeWindow *android_getNativeWindow(JNIEnv *env) {
    if(!bind) bind_activity(env);    
    return (*_ANDROID_getNativeWindow)();
}

const char *android_getDataDir(JNIEnv *env) {
    if(!bind) bind_activity(env);    
    return (*_ANDROID_getDataDir)();    
}

jboolean lens_input_initialize(JNIEnv *env) {        
    uint32_t flags = 0;
    flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_MULTITOUCH;
    glass_application_notifyDeviceEvent(env, flags, 1);
    (*_ANDROID_notifyGlassStarted)();
    return JNI_TRUE;
}

void lens_input_shutdown(JNIEnv *env) {
    (*_ANDROID_notifyGlassShutdown)();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_SoftwareKeyboard__1show
(JNIEnv *env, jclass clazz) {
    if(!bind) bind_activity(env);
    GLASS_LOG_FINE("Show SoftwareKeyboard");
    (*_ANDROID_notifyShowIME)();
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_SoftwareKeyboard__1hide
(JNIEnv *env, jclass clazz) {
    if(!bind) bind_activity(env);
    GLASS_LOG_FINE("Hide SoftwareKeyboard");
    (*_ANDROID_notifyHideIME)();
}

/*
 * Class:     javafxports_android_FXActivity_InternalSurfaceView
 * Class: com_sun_glass_ui_android_DalvikInput
 * Method:    onMultiTouchEventNative
 * Signature: (I[I[I[I[I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_DalvikInput_onMultiTouchEventNative
  (JNIEnv *env, jobject that, jint jcount, jintArray jactions, jintArray jids, jintArray jxs, jintArray jys) {
    GLASS_LOG_FINE("Call InternalSurfaceView_onMultiTouchEventNative");
    
    jlong jlongids[jcount];

    int *actions = (*env)->GetIntArrayElements(env, jactions, 0);    
    int *ids = (*env)->GetIntArrayElements(env, jids, 0);
    int *xs = (*env)->GetIntArrayElements(env, jxs, 0);
    int *ys = (*env)->GetIntArrayElements(env, jys, 0);
    int primary = 0;
    for(int i=0;i<jcount;i++) {
        actions[i] = to_jfx_touch_action(actions[i]);
        jlongids[i] = (jlong)ids[i];
        if (actions[i] != com_sun_glass_events_TouchEvent_TOUCH_STILL) {
            primary = actions[i] == com_sun_glass_events_TouchEvent_TOUCH_RELEASED && jcount == 1 ? -1 : i;
        }
    }
    
    lens_wm_notifyMultiTouchEvent(env,
           jcount,
           actions,
           jlongids,
           xs,
           ys,
           primary);
    
    (*env)->ReleaseIntArrayElements(env, jactions, actions, 0);
    (*env)->ReleaseIntArrayElements(env, jids, ids, 0);
    (*env)->ReleaseIntArrayElements(env, jxs, xs, 0);
    (*env)->ReleaseIntArrayElements(env, jys, ys, 0);    
}

/*
 * Class: com_sun_glass_ui_android_DalvikInput
 * Method:    onKeyEventNative
 * Signature: (IILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_DalvikInput_onKeyEventNative
  (JNIEnv *env, jobject that, jint action, jint keycode, jstring s) {    
    GLASS_LOG_FINEST("Key event: [action: %s, keyCode: %i]",
            describe_key_action(action), keycode);
    int type = to_jfx_key_action(action);
    int linux_keycode = to_linux_keycode(keycode);
    GLASS_LOG_FINEST("Translated to linux keycode: [%i]", linux_keycode);
    if (linux_keycode > 0) {        
        NativeWindow window = glass_window_getFocusedWindow();
        if (!window) {
            GLASS_LOG_FINE("Haven't got focused window. Terminate notifying key event.");
            return;
        }
        int jfxKeyCode = glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(linux_keycode);
        glass_application_notifyKeyEvent(env,
                window,
                type,
                jfxKeyCode,
                JNI_FALSE);
    }
}

/*
 * Class: com_sun_glass_ui_android_DalvikInput
 * Method: onSurfaceChangedNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_DalvikInput_onSurfaceChangedNative__
 (JNIEnv *env, jobject that) {
 GLASS_LOG_FINEST("Notify JFX that surface has changed!");
    glass_application_notifyScreenSettingsChanged(env);
    lens_wm_repaint_all(env);
}

/*
 * Class: com_sun_glass_ui_android_DalvikInput
 * Method: onSurfaceChangedNative
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_DalvikInput_onSurfaceChangedNative__III
 (JNIEnv *env, jobject that, jint fmt, jint w, jint h) {
    GLASS_LOG_FINEST("Notify JFX that surface has changed."); 
    GLASS_LOG_FINEST("Surface format:%d width:%d height%d", fmt, w, h);     
    glass_application_notifyScreenSettingsChanged(env);
    lens_wm_repaint_all(env);
}

/*
 * Class: com_sun_glass_ui_android_DalvikInput
 * Method: onSurfaceRedrawNeededNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_DalvikInput_onSurfaceRedrawNeededNative
 (JNIEnv *env, jobject that) {
    GLASS_LOG_WARNING("Call surfaceRedrawNeeded");
    lens_wm_repaint_all(env);
}

/*
 * Class: com_sun_glass_ui_android_DalvikInput
 * Method: onConfigurationChangedNative
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_android_DalvikInput_onConfigurationChangedNative
  (JNIEnv *env, jobject that, jint flags) {    
    GLASS_LOG_FINEST("Call configuration changed.");
    glass_application_notifyScreenSettingsChanged(env);   
    lens_wm_repaint_all(env);
}

#endif
