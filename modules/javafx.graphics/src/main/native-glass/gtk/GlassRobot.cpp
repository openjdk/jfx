/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/XTest.h>
#include <assert.h>
#include <stdlib.h>
#include <math.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>

#include <com_sun_glass_ui_GlassRobot.h>
#include <com_sun_glass_ui_gtk_GtkRobot.h>
#include <com_sun_glass_events_MouseEvent.h>
#include "glass_general.h"
#include "glass_key.h"
#include "glass_screen.h"

#define MOUSE_BACK_BTN 8
#define MOUSE_FORWARD_BTN 9

static void checkXTest(JNIEnv* env) {
    int32_t major_opcode, first_event, first_error;
    int32_t  event_basep, error_basep, majorp, minorp;
    static int32_t isXTestAvailable;
    static gboolean checkDone = FALSE;
    if (!checkDone) {
        /* check if XTest is available */
        isXTestAvailable = XQueryExtension(gdk_x11_get_default_xdisplay(), XTestExtensionName, &major_opcode, &first_event, &first_error);
        if (isXTestAvailable) {
            /* check if XTest version is OK */
            XTestQueryExtension(gdk_x11_get_default_xdisplay(), &event_basep, &error_basep, &majorp, &minorp);
            if (majorp < 2 || (majorp == 2 && minorp < 2)) {
                    isXTestAvailable = False;
            } else {
                XTestGrabControl(gdk_x11_get_default_xdisplay(), True);
            }
        }
        checkDone = TRUE;
    }
    if (!isXTestAvailable) {
        jclass cls = env->FindClass("java/lang/UnsupportedOperationException");
        if (env->ExceptionCheck()) return;
        env->ThrowNew(cls, "Glass Robot needs XTest extension to work");
    }
}

static void keyButton(jint code, gboolean press)
{
    Display *xdisplay = gdk_x11_get_default_xdisplay();
    gint gdk_keyval = find_gdk_keyval_for_glass_keycode(code);
    GdkKeymapKey *keys;
    gint n_keys;
    if (gdk_keyval == -1) {
        return;
    }
    gdk_keymap_get_entries_for_keyval(gdk_keymap_get_default(),
            gdk_keyval, &keys, &n_keys);
    if (n_keys < 1) {
        return;
    }

    XTestFakeKeyEvent(xdisplay,
                      keys[0].keycode,
                      press ? True : False,
                      CurrentTime);
    g_free(keys);
    XSync(xdisplay, False);
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _keyPress
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1keyPress
  (JNIEnv *env, jobject obj, jint code)
{
    (void)obj;

    checkXTest(env);
    keyButton(code, TRUE);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _keyRelease
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1keyRelease
  (JNIEnv *env, jobject obj, jint code)
{
    (void)obj;

    checkXTest(env);
    keyButton(code, FALSE);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _mouseMove
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1mouseMove
  (JNIEnv *env, jobject obj, jint x, jint y)
{
    (void)obj;

    Display *xdisplay = gdk_x11_get_default_xdisplay();
    checkXTest(env);
    jfloat uiScale = getUIScale();
    x = rint(x * uiScale);
    y = rint(y * uiScale);
    XWarpPointer(xdisplay,
            None,
            XRootWindow(xdisplay,gdk_x11_get_default_screen()),
            0, 0, 0, 0, x, y);
    XSync(xdisplay, False);
}

static void mouseButtons(jint buttons, gboolean press)
{
    Display *xdisplay = gdk_x11_get_default_xdisplay();
    if (buttons & com_sun_glass_ui_GlassRobot_MOUSE_LEFT_BTN) {
        XTestFakeButtonEvent(xdisplay, 1, press, CurrentTime);
    }
    if (buttons & com_sun_glass_ui_GlassRobot_MOUSE_MIDDLE_BTN) {
        XTestFakeButtonEvent(xdisplay, 2, press, CurrentTime);
    }
    if (buttons & com_sun_glass_ui_GlassRobot_MOUSE_RIGHT_BTN) {
        XTestFakeButtonEvent(xdisplay, 3, press, CurrentTime);
    }
    if (buttons & com_sun_glass_ui_GlassRobot_MOUSE_BACK_BTN) {
        XTestFakeButtonEvent(xdisplay, MOUSE_BACK_BTN, press, CurrentTime);
    }
    if (buttons & com_sun_glass_ui_GlassRobot_MOUSE_FORWARD_BTN) {
        XTestFakeButtonEvent(xdisplay, MOUSE_FORWARD_BTN, press, CurrentTime);
    }

    XSync(xdisplay, False);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _mousePress
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1mousePress
  (JNIEnv *env, jobject obj, jint buttons)
{
    (void)obj;

    checkXTest(env);
    mouseButtons(buttons, TRUE);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _mouseRelease
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1mouseRelease
  (JNIEnv *env, jobject obj, jint buttons)
{
    (void)obj;

    checkXTest(env);
    mouseButtons(buttons, FALSE);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _mouseWheel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1mouseWheel
  (JNIEnv *env, jobject obj, jint amt)
{
    (void)obj;

    Display *xdisplay = gdk_x11_get_default_xdisplay();
    int repeat = abs(amt);
    int button = amt < 0 ? 4 : 5;
    int i;

    checkXTest(env);
    for (i = 0; i < repeat; i++) {
        XTestFakeButtonEvent(xdisplay, button, True, CurrentTime);
        XTestFakeButtonEvent(xdisplay, button, False, CurrentTime);
    }
    XSync(xdisplay, False);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _getMouseX
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1getMouseX
  (JNIEnv *env, jobject obj)
{
    (void)env;
    (void)obj;

    jint x;
    glass_gdk_display_get_pointer(gdk_display_get_default(), &x, NULL);
    x = rint(x / getUIScale());
    return x;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _getMouseY
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1getMouseY
  (JNIEnv *env, jobject obj)
{
    (void)env;
    (void)obj;

    jint y;
    glass_gdk_display_get_pointer(gdk_display_get_default(), NULL, &y);
    y = rint(y / getUIScale());
    return y;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkRobot
 * Method:    _getScreenCapture
 * Signature: (IIII[I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkRobot__1getScreenCapture
  (JNIEnv * env, jobject obj, jint x, jint y, jint width, jint height, jintArray data)
{
    (void)obj;

    GdkPixbuf *screenshot, *tmp;
    GdkWindow *root_window = gdk_get_default_root_window();

    tmp = glass_pixbuf_from_window(root_window, x, y, width, height);
    screenshot = gdk_pixbuf_add_alpha(tmp, FALSE, 0, 0, 0);
    g_object_unref(tmp);

    jint *pixels = (jint *)convert_BGRA_to_RGBA((int*)gdk_pixbuf_get_pixels(screenshot), width * 4, height);
    env->SetIntArrayRegion(data, 0, height * width, pixels);
    g_free(pixels);

    g_object_unref(screenshot);
}

} // extern "C"
