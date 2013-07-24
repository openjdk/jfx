
/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
#ifdef ANDROID_NDK

#include "LensCommon.h"
#include "android.h"
#include "com_oracle_dalvik_FXActivity.h"
#include "com_oracle_dalvik_FXActivity_InternalSurfaceView.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_KeyEvent.h"

#define GET_WINDOW_FROM_SURFACE(p, s) \
	(!s) ? NULL : ANativeWindow_fromSurface(p, s)

#define RGBA_8888 1
#define RGBX_8888 2
#define RGB_888   3
#define RGB_565   4

#define TOUCH_ACTION_DOWN           0
#define TOUCH_ACTION_UP             1
#define TOUCH_ACTION_MOVE           2
#define TOUCH_ACTION_CANCEL         3
#define TOUCH_ACTION_OUTSIDE		4
#define TOUCH_ACTION_POINTER_DOWN	5
#define TOUCH_ACTION_POINTER_UP     6

#define KEY_ACTION_DOWN     0
#define KEY_ACTION_UP       1
#define KEY_ACTION_MULTIPLE 2

JavaVM *dalvikVM;
JNIEnv *glass_env;
JNIEnv *dalvik_env;

ANativeWindow *window;
int32_t width;
int32_t height;
int32_t format;

static JavaVM* (*_glass_application_getVM)();

static void (*_glass_application_notifyWindowEvent_resize)(
        JNIEnv *env,
        ANativeWindow *window,
        int eventType,
        int width,
        int height);

static void (*_lens_wm_notifyTouchEvent)(
        JNIEnv *env,
        jint state,
        int id,
        int xabs,
        int yabs);

static void (*_lens_wm_notifyMotionEvent)(
        JNIEnv *env,
        int mousePosX,
        int mousePosY,
        int isTouch,
        int touchId);

static void (*_lens_wm_notifyButtonEvent)(
        JNIEnv *env,
        jboolean pressed,
        int button,
        int xabs, int yabs);

static void (*_glass_application_notifyKeyEvent)(
        JNIEnv *env,
        NativeWindow window,
        int eventType,
        int jfxKeyCode,
        jboolean isRepeatEvent);

static NativeWindow(*_glass_window_getFocusedWindow)();

static int (*_glass_inputEvents_getJavaKeycodeFromPlatformKeyCode)(
        int platformKey);

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    dalvikVM = vm;
    (*vm)->GetEnv(vm, (void **) &dalvik_env, JNI_VERSION_1_6);
    init_ids(dalvik_env);
    init_functions(dalvik_env);
    return JNI_VERSION_1_6;
}

jobject jFXActivity;
jclass jFXActivityClass;
jmethodID jFXActivity_getInstance;
jmethodID jFXActivity_getLDPath;
jmethodID jFXActivity_showIME;
jmethodID jFXActivity_hideIME;

void init_ids(JNIEnv *env) {
    jFXActivityClass =
            (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/oracle/dalvik/FXActivity"));
    CHECK_EXCEPTION(env);
 
    jFXActivity_showIME = (*env)->GetMethodID(env, jFXActivityClass,
            "showIME", "()V");
    CHECK_EXCEPTION(env);

    jFXActivity_hideIME = (*env)->GetMethodID(env, jFXActivityClass,
            "hideIME", "()V");
    CHECK_EXCEPTION(env);

    jFXActivity_getInstance = (*env)->GetStaticMethodID(env, jFXActivityClass,
            "getInstance", "()Lcom/oracle/dalvik/FXActivity;");
    CHECK_EXCEPTION(env);

    jFXActivity = (*env)->CallStaticObjectMethod(env, jFXActivityClass, jFXActivity_getInstance);
    CHECK_EXCEPTION(env);

    jFXActivity_getLDPath = (*env)->GetMethodID(env, jFXActivityClass,
            "getLDPath", "()Ljava/lang/String;");
    CHECK_EXCEPTION(env);
}

void init_functions(JNIEnv *env) {
    const char *libglass_name = "libglass_lens_eglfb.so";
    jstring jldpath = (*env)->CallObjectMethod(env, jFXActivity, jFXActivity_getLDPath);
    const char *cpath = (*env)->GetStringUTFChars(env, jldpath, 0);
    int cpath_len = (*env)->GetStringUTFLength(env, jldpath);

    char *fullpath = (char *) calloc(cpath_len + strlen(libglass_name) + 2, 1);
    strcpy(fullpath, cpath);
    strcat(fullpath, "/");
    strcat(fullpath, libglass_name);

    void *libglass = dlopen(fullpath, RTLD_LAZY | RTLD_GLOBAL);
    if (!libglass) {
        THROW_RUNTIME_EXCEPTION(env, "dlopen failed with error: %s", dlerror());
        return;
    }
    _glass_application_getVM = GET_SYMBOL(env, libglass, "glass_application_GetVM");
    _glass_application_notifyWindowEvent_resize = GET_SYMBOL(env, libglass, "glass_application_notifyWindowEvent_resize");
    _lens_wm_notifyTouchEvent = GET_SYMBOL(env, libglass, "lens_wm_notifyTouchEvent");
    _lens_wm_notifyMotionEvent = GET_SYMBOL(env, libglass, "lens_wm_notifyMotionEvent");
    _lens_wm_notifyButtonEvent = GET_SYMBOL(env, libglass, "lens_wm_notifyButtonEvent");
    _glass_application_notifyKeyEvent = GET_SYMBOL(env, libglass, "glass_application_notifyKeyEvent");
    _glass_window_getFocusedWindow = GET_SYMBOL(env, libglass, "glass_window_getFocusedWindow");
    _glass_inputEvents_getJavaKeycodeFromPlatformKeyCode = GET_SYMBOL(env, libglass,
            "glass_inputEvents_getJavaKeycodeFromPlatformKeyCode");

    free(fullpath);
}

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    saveSurface
 * Signature: (Landroid/view/Surface;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity__1surfaceChanged__Landroid_view_Surface_2
(JNIEnv *env, jobject activity, jobject surface) {

    window = GET_WINDOW_FROM_SURFACE(env, surface);
}

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    saveSurface
 * Signature: (Landroid/view/Surface;III)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity__1surfaceChanged__Landroid_view_Surface_2III
(JNIEnv *env, jobject activity, jobject surface, jint f, jint w, jint h) {

    char buf[50];
    LOGV("Surface changed format:%s dimension:[%i, %i]",
            describe_surface_format(f, buf), w, h);
    window = GET_WINDOW_FROM_SURFACE(env, surface);
    format = f;
    w = width;
    h = height;
}

/*
 * Class:     com_oracle_dalvik_FXActivity
 * Method:    _surfaceRedrawNeeded
 * Signature: (Landroid/view/Surface;)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity__1surfaceRedrawNeeded
(JNIEnv *env, jobject activity, jobject surface) {
    window = GET_WINDOW_FROM_SURFACE(env, surface);
}

/*
 * Class:     com_oracle_dalvik_FXActivity_InternalSurfaceView
 * Method:    onTouchEventNative
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity_00024InternalSurfaceView_onTouchEventNative
(JNIEnv *ignore, jobject view, jint action, jint absx, jint absy) {
    LOGV(TAG, "Touch event: [%s, x: %i, y: %i]\n", describe_touch_action(action), absx, absy);

    JNIEnv *genv = get_glass_JNIEnv();
    if (!genv) return;

    int fxstate = to_jfx_touch_action(action);
    if (!fxstate) {
        LOGE(TAG, "Can't handle this state yet. Ignoring. (Probably multitouch)");
        return;
    }
    if (fxstate == com_sun_glass_events_TouchEvent_TOUCH_MOVED) {
        (*_lens_wm_notifyMotionEvent)(genv, absx, absy, 1, 0);
    } else {
        (*_lens_wm_notifyTouchEvent)(genv, fxstate, 0, absx, absy);
        //send also mouse event
        (*_lens_wm_notifyButtonEvent)(genv,
                (fxstate == com_sun_glass_events_TouchEvent_TOUCH_PRESSED),
                com_sun_glass_events_MouseEvent_BUTTON_LEFT,
                absx, absy);
    }
}

/*
 * Class:     com_oracle_dalvik_FXActivity_InternalSurfaceView
 * Method:    onKeyEventNative
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_oracle_dalvik_FXActivity_00024InternalSurfaceView_onKeyEventNative
(JNIEnv *ignore, jobject view, jint action, jint keyCode) {
    JNIEnv *genv = get_glass_JNIEnv();
    if (!genv) return;

    int jfxaction = to_jfx_key_action(action);
    int jfxKeyCode = (*_glass_inputEvents_getJavaKeycodeFromPlatformKeyCode)(
            translate_to_linux_keycode(keyCode));
    NativeWindow window = (*_glass_window_getFocusedWindow)();
    (*_glass_application_notifyKeyEvent)(genv, window, jfxaction, jfxKeyCode, 0);
}

JNIEnv *get_glass_JNIEnv() {
    if (!glass_env) {
        JavaVM *glass_vm = (*_glass_application_getVM)();
        if (glass_vm == NULL) {
            LOGV(TAG, "Ignoring event!");
            return NULL;
        }
        (*glass_vm)->AttachCurrentThread(glass_vm, (JNIEnv **) &glass_env, NULL);
    }
    return glass_env;
}

ANativeWindow *ANDROID_getNativeWindow() {
    return window;
}

void ANDROID_showIME() {
    JNIEnv *env;
    (*dalvikVM)->AttachCurrentThread(dalvikVM, (JNIEnv **) &env, NULL);
    (*env)->CallVoidMethod(env, jFXActivity, jFXActivity_showIME);
}

void ANDROID_hideIME() {
    JNIEnv *env;
    (*dalvikVM)->AttachCurrentThread(dalvikVM, (JNIEnv **) &env, NULL);
    (*env)->CallVoidMethod(env, jFXActivity, jFXActivity_hideIME);
}

int32_t translate_to_linux_keycode(int32_t androidKeyCode) {
    for (int i = 0; i < sizeof (keyMap); ++i) {
        if (keyMap[i].androidKC == androidKeyCode) {
            return keyMap[i].linuxKC;
        }
    }
    return KEY_RESERVED;
}

char *describe_surface_format(int f, char *buf) {
    char s[10];
    if (!buf) {
        buf = s;
    }
    switch (f) {
        case RGBA_8888:
            strcpy(buf, "RGBA_8888");
            return buf;
        case RGBX_8888:
            strcpy(buf, "RGBX_8888");
            return buf;
        case RGB_888:
            strcpy(buf, "RGB_888");
            return buf;
        case RGB_565:
            strcpy(buf, "RGB_565");
            return buf;
        default:
            sprintf(buf, "%i", f);
            return buf;
    }
}

int to_jfx_touch_action(int state) {
    switch (state) {
        case TOUCH_ACTION_DOWN:
            return com_sun_glass_events_TouchEvent_TOUCH_PRESSED;
        case TOUCH_ACTION_UP:
            return com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
        case TOUCH_ACTION_MOVE:
            return com_sun_glass_events_TouchEvent_TOUCH_MOVED;
        case TOUCH_ACTION_CANCEL:
            return com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
        default:
            return 0;
    }
}

int to_jfx_key_action(int action) {
    switch (action) {
        case KEY_ACTION_DOWN:
            return com_sun_glass_events_KeyEvent_PRESS;
        case KEY_ACTION_UP:
            return com_sun_glass_events_KeyEvent_RELEASE;
        case KEY_ACTION_MULTIPLE:
            return com_sun_glass_events_KeyEvent_TYPED;
    }
}

char *describe_touch_action(int state) {
    switch (state) {
        case TOUCH_ACTION_DOWN:
            return "TOUCH_ACTION_DOWN";
        case TOUCH_ACTION_UP:
            return "TOUCH_ACTION_UP";
        case TOUCH_ACTION_MOVE:
            return "TOUCH_ACTION_MOVE";
        case TOUCH_ACTION_CANCEL:
            return "TOUCH_ACTION_CANCEL";
        case TOUCH_ACTION_OUTSIDE:
            return "TOUCH_ACTION_OUTSIDE";
        case TOUCH_ACTION_POINTER_DOWN:
            return "TOUCH_ACTION_POINTER_DOWN";
        case TOUCH_ACTION_POINTER_UP:
            return "TOUCH_ACTION_POINTER_UP";
        default:
            return "TOUCH_ACTION_UNKNOWN";

    }
}

#endif /* ANDROID_NDK */


