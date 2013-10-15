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
#include "lensRFB.h"
#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_TouchEvent.h"

#ifdef USE_WRAPPED_VNC
#define PORTRFBAPI
#include "wrapped_rfb.h"
#else
#include <rfb/rfb.h>
#include <rfb/keysym.h>
#endif

static jboolean isActive = JNI_FALSE;

//no choice but to cache it, as we can't pass it to client data directly
//because newClientHook is called at arbitrary point by VNC
static JNIEnv *genv;

static rfbClientPtr client = NULL;
static rfbScreenInfoPtr rfbScreen;

typedef struct ClientData {
    rfbBool oldButton;
    int oldX;
    int oldY;
} ClientData;

typedef struct {
    int fbKey;
    int shiftedfbKey;
    int javaKey;
} rfbKeytoJavaKeyPair;

//As we don't have linux input code, we need to hold our own "key map"
// NOTE: RFB sends us "shifted keys" and we kind of need to shift them back...

static rfbKeytoJavaKeyPair keyMap[] = {
    {0, 0,  com_sun_glass_events_KeyEvent_VK_UNDEFINED},
    {XK_Escape, 0,   com_sun_glass_events_KeyEvent_VK_ESCAPE},
    {XK_a, XK_A, com_sun_glass_events_KeyEvent_VK_A},
    {XK_b, XK_B, com_sun_glass_events_KeyEvent_VK_B},
    {XK_c, XK_C, com_sun_glass_events_KeyEvent_VK_C},
    {XK_d, XK_D, com_sun_glass_events_KeyEvent_VK_D},
    {XK_e, XK_E, com_sun_glass_events_KeyEvent_VK_E},
    {XK_f, XK_F, com_sun_glass_events_KeyEvent_VK_F},
    {XK_g, XK_G, com_sun_glass_events_KeyEvent_VK_G},
    {XK_h, XK_H, com_sun_glass_events_KeyEvent_VK_H},
    {XK_i, XK_I, com_sun_glass_events_KeyEvent_VK_I},
    {XK_j, XK_J, com_sun_glass_events_KeyEvent_VK_J},
    {XK_k, XK_K, com_sun_glass_events_KeyEvent_VK_K},
    {XK_l, XK_L, com_sun_glass_events_KeyEvent_VK_L},
    {XK_m, XK_M, com_sun_glass_events_KeyEvent_VK_M},
    {XK_n, XK_N, com_sun_glass_events_KeyEvent_VK_N},
    {XK_o, XK_O, com_sun_glass_events_KeyEvent_VK_O},
    {XK_p, XK_P, com_sun_glass_events_KeyEvent_VK_P},
    {XK_q, XK_Q, com_sun_glass_events_KeyEvent_VK_Q},
    {XK_r, XK_R, com_sun_glass_events_KeyEvent_VK_R},
    {XK_s, XK_S, com_sun_glass_events_KeyEvent_VK_S},
    {XK_t, XK_T, com_sun_glass_events_KeyEvent_VK_T},
    {XK_u, XK_U, com_sun_glass_events_KeyEvent_VK_U},
    {XK_v, XK_V, com_sun_glass_events_KeyEvent_VK_V},
    {XK_w, XK_W, com_sun_glass_events_KeyEvent_VK_W},
    {XK_x, XK_X, com_sun_glass_events_KeyEvent_VK_X},
    {XK_y, XK_Y, com_sun_glass_events_KeyEvent_VK_Y},
    {XK_z, XK_Z, com_sun_glass_events_KeyEvent_VK_Z},

    {XK_1, XK_exclam, com_sun_glass_events_KeyEvent_VK_1},
    {XK_2, XK_at, com_sun_glass_events_KeyEvent_VK_2},
    {XK_3, XK_numbersign, com_sun_glass_events_KeyEvent_VK_3},
    {XK_4, XK_dollar, com_sun_glass_events_KeyEvent_VK_4},
    {XK_5, XK_percent, com_sun_glass_events_KeyEvent_VK_5},
    {XK_6, XK_asciicircum, com_sun_glass_events_KeyEvent_VK_6},
    {XK_7, XK_ampersand, com_sun_glass_events_KeyEvent_VK_7},
    {XK_8, XK_asterisk, com_sun_glass_events_KeyEvent_VK_8},
    {XK_9, XK_parenleft, com_sun_glass_events_KeyEvent_VK_9},
    {XK_0, XK_parenright, com_sun_glass_events_KeyEvent_VK_0},

    {XK_minus, XK_underscore, com_sun_glass_events_KeyEvent_VK_MINUS},
    {XK_equal, XK_plus, com_sun_glass_events_KeyEvent_VK_EQUALS},
    {XK_apostrophe, XK_quotedbl, com_sun_glass_events_KeyEvent_VK_QUOTE},
    {XK_backslash, XK_bar, com_sun_glass_events_KeyEvent_VK_BACK_SLASH},
    {XK_bracketleft, XK_braceleft, com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET},
    {XK_bracketright, XK_braceright, com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET},
    {XK_grave, XK_asciitilde, com_sun_glass_events_KeyEvent_VK_BACK_QUOTE},
    {XK_semicolon, XK_colon, com_sun_glass_events_KeyEvent_VK_SEMICOLON},
    {XK_comma, XK_less, com_sun_glass_events_KeyEvent_VK_COMMA},
    {XK_period, XK_greater, com_sun_glass_events_KeyEvent_VK_PERIOD},
    {XK_slash, XK_question, com_sun_glass_events_KeyEvent_VK_SLASH},

    {XK_Alt_L, 0, com_sun_glass_events_KeyEvent_VK_ALT},
    {XK_Caps_Lock, 0, com_sun_glass_events_KeyEvent_VK_CAPS_LOCK},
    {XK_space, 0, com_sun_glass_events_KeyEvent_VK_SPACE},
    {XK_Shift_R, 0, com_sun_glass_events_KeyEvent_VK_SHIFT},
    {XK_Shift_L, 0, com_sun_glass_events_KeyEvent_VK_SHIFT},
    {XK_BackSpace, 0, com_sun_glass_events_KeyEvent_VK_BACKSPACE},
    {XK_Tab, 0, com_sun_glass_events_KeyEvent_VK_TAB},
    {XK_Control_L, 0, com_sun_glass_events_KeyEvent_VK_CONTROL},
    {XK_Return, 0, com_sun_glass_events_KeyEvent_VK_ENTER},

    {XK_F1, 0, com_sun_glass_events_KeyEvent_VK_F1},
    {XK_F2, 0, com_sun_glass_events_KeyEvent_VK_F2},
    {XK_F3, 0, com_sun_glass_events_KeyEvent_VK_F3},
    {XK_F4, 0, com_sun_glass_events_KeyEvent_VK_F4},
    {XK_F5, 0, com_sun_glass_events_KeyEvent_VK_F5},
    {XK_F6, 0, com_sun_glass_events_KeyEvent_VK_F6},
    {XK_F7, 0, com_sun_glass_events_KeyEvent_VK_F7},
    {XK_F8, 0, com_sun_glass_events_KeyEvent_VK_F8},
    {XK_F9, 0, com_sun_glass_events_KeyEvent_VK_F9},
    {XK_F10, 0, com_sun_glass_events_KeyEvent_VK_F10},
    {XK_Num_Lock, 0, com_sun_glass_events_KeyEvent_VK_NUM_LOCK},
    {XK_Scroll_Lock, 0, com_sun_glass_events_KeyEvent_VK_SCROLL_LOCK},
    {XK_F11, 0, com_sun_glass_events_KeyEvent_VK_F11},
    {XK_F12, 0, com_sun_glass_events_KeyEvent_VK_F12},

    {XK_KP_0, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD7},
    {XK_KP_1, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD1},
    {XK_KP_2, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD2},
    {XK_KP_3, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD3},
    {XK_KP_4, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD4},
    {XK_KP_5, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD5},
    {XK_KP_6, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD6},
    {XK_KP_7, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD7},
    {XK_KP_8, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD8},
    {XK_KP_9, 0, com_sun_glass_events_KeyEvent_VK_NUMPAD9},
    {XK_KP_Add, 0, com_sun_glass_events_KeyEvent_VK_ADD},
    {XK_KP_Decimal, 0, com_sun_glass_events_KeyEvent_VK_DECIMAL},
    {XK_KP_Subtract, 0, com_sun_glass_events_KeyEvent_VK_SUBTRACT},

    {XK_KP_Multiply, 0, com_sun_glass_events_KeyEvent_VK_MULTIPLY},
    {XK_KP_Enter, 0, com_sun_glass_events_KeyEvent_VK_ENTER},
    {XK_Control_R, 0, com_sun_glass_events_KeyEvent_VK_CONTROL},
    {XK_KP_Divide, 0, com_sun_glass_events_KeyEvent_VK_DIVIDE},
    {XK_Print, 0, com_sun_glass_events_KeyEvent_VK_PRINTSCREEN},
    {XK_Alt_R, 0, com_sun_glass_events_KeyEvent_VK_ALT},
    {XK_KP_Home, XK_Home, com_sun_glass_events_KeyEvent_VK_HOME},
    {XK_KP_Up, XK_Up, com_sun_glass_events_KeyEvent_VK_UP},
    {XK_KP_Page_Up, XK_Page_Up, com_sun_glass_events_KeyEvent_VK_PAGE_UP},
    {XK_KP_Left, XK_Left, com_sun_glass_events_KeyEvent_VK_LEFT},
    {XK_KP_Right, XK_Right, com_sun_glass_events_KeyEvent_VK_RIGHT},
    {XK_KP_End, XK_End, com_sun_glass_events_KeyEvent_VK_END},
    {XK_KP_Down, XK_Down, com_sun_glass_events_KeyEvent_VK_DOWN},
    {XK_KP_Page_Down, XK_Page_Down, com_sun_glass_events_KeyEvent_VK_PAGE_DOWN},
    {XK_KP_Insert, XK_Insert, com_sun_glass_events_KeyEvent_VK_INSERT},
    {XK_KP_Delete, 0, com_sun_glass_events_KeyEvent_VK_DELETE},
    {XK_Meta_L, 0, com_sun_glass_events_KeyEvent_VK_WINDOWS},
    {XK_Meta_R, 0, com_sun_glass_events_KeyEvent_VK_WINDOWS},
    {XK_Menu, 0, com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU}
};

//forward declarations
void rfbEventLoop(JNIEnv *env, void *data);
static int  rfbGetJavaKeycodeFromPlatformKeyCode(int fbKey);
static enum rfbNewClientAction newclient(rfbClientPtr cl);
static void clientgone(rfbClientPtr cl);
static void rfbHandlePointerEvent(int buttonMask, int x, int y, rfbClientPtr cl);
static void rfbHandleKeyEvent(rfbBool pressed, rfbKeySym key, rfbClientPtr cl);



////// Init
void lens_rfb_init(JNIEnv *env) {

    NativeScreen screen = glass_screen_getMainScreen();
    char *fb = lens_screen_getFrameBuffer();

    if (fb == NULL) {
        GLASS_LOG_CONFIG("Platform doesn't support access to frame buffer - no VNC support");
        return;
    }


#ifdef USE_WRAPPED_VNC
    void *rfbhandle = dlopen("libvncserver.so", RTLD_LAZY);

    if (rfbhandle) {
        _rfbGetScreen = dlsym(rfbhandle, "rfbGetScreen");
        _rfbInitServer = dlsym(rfbhandle, "rfbInitServerWithPthreadsAndZRLE");
        _rfbShutdownServer = dlsym(rfbhandle, "rfbShutdownServer");
        _rfbNewFramebuffer = dlsym(rfbhandle, "rfbNewFramebuffer");
        _rfbRunEventLoop = dlsym(rfbhandle, "fbRunEventLoop");
        _rfbMarkRectAsModified = dlsym(rfbhandle, "rfbMarkRectAsModified");
        _rfbProcessEvents = dlsym(rfbhandle, "rfbProcessEvents");
        _rfbIsActive = dlsym(rfbhandle, "rfbIsActive");
    }

    if (!(_rfbInitServer && _rfbGetScreen)) {
        GLASS_LOG_CONFIG("Failed to load symbols from libvncserver.so - - no VNC support");
        return;
    }
#endif
    rfbScreen = rfbGetScreen(0, NULL, screen->width, screen->height, 8, 3, 4);
    if (!rfbScreen) {
        GLASS_LOG_CONFIG("rfbGetScreen() failed -no VNC support");
        return;
    }

    rfbScreen->desktopName = "JFX";
    rfbScreen->frameBuffer = fb;
    rfbScreen->alwaysShared = FALSE;
    rfbScreen->ptrAddEvent = rfbHandlePointerEvent;
    rfbScreen->kbdAddEvent = rfbHandleKeyEvent;
    rfbScreen->newClientHook = newclient;
    rfbScreen->httpEnableProxyConnect = FALSE;


    rfbInitServer(rfbScreen);

    //start listen to events
    glass_application_request_native_event_loop(env, &rfbEventLoop, NULL);
    isActive = JNI_TRUE;

    GLASS_LOG_CONFIG("VNC is running");

}

void rfbEventLoop(JNIEnv *env, void *data) {
    genv = env;
    rfbRunEventLoop(rfbScreen, 100000, FALSE);
}

static enum rfbNewClientAction newclient(rfbClientPtr cl) {
    GLASS_LOG_FINE("RFB new client");
    cl->clientData = (void *)calloc(sizeof(ClientData), 1);
    cl->clientGoneHook = clientgone;
    client = cl;
    return RFB_CLIENT_ACCEPT;
}

static void clientgone(rfbClientPtr cl) {
    GLASS_LOG_FINE("RFB client disconnect");
    free(cl->clientData);
    client = NULL;
}



void lens_rfb_notifyDirtyRegion(int topLeft_X,
                                int topLeft_Y,
                                int buttomRight_X,
                                int buttomRight_Y) {
    if (isActive) {
        rfbMarkRectAsModified(rfbScreen, topLeft_X, topLeft_Y,
                              buttomRight_X, buttomRight_Y);
    }
}

//event handling

/**
 * Handler function for pointer events. Called by VNC
 *
 * @param buttonMask event mask
 * @param x abs coordinate on screen
 * @param y abs coordinate on screen
 * @param cl client record
 */
static void rfbHandlePointerEvent(int buttonMask, int x, int y, rfbClientPtr cl) {
    GLASS_LOG_FINEST("RFB doptr %x %d,%d env=%p", buttonMask, x, y, genv);

    ClientData *cd = cl->clientData;

    if (x != cd->oldX || y != cd->oldY) {
        cd->oldX = x;
        cd->oldY = y;
        lens_wm_notifyMotionEvent(genv, x, y);
    }

    if (buttonMask != cd->oldButton) {
        int buttonsChanged = buttonMask ^ cd->oldButton;
        int pressed = 0;
        int glassMouseButton = 0;
        if (buttonsChanged & rfbButton1Mask) {
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_LEFT;
            pressed = (buttonMask & rfbButton1Mask) == rfbButton1Mask;
        }  else if (buttonsChanged & rfbButton2Mask) {
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_OTHER;
            pressed = (buttonMask & rfbButton2Mask) == rfbButton2Mask;
        } else if (buttonsChanged & rfbButton3Mask) {
            glassMouseButton = com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
            pressed = (buttonMask & rfbButton3Mask) == rfbButton3Mask;
        }

        if (glassMouseButton) {
            lens_wm_notifyButtonEvent(genv,
                                      pressed,
                                      glassMouseButton,
                                      x, y);
        }
        cd->oldButton = buttonMask;
    }

    rfbDefaultPtrAddEvent(buttonMask, x, y, cl);
}

/**
 * Handler function for key events. Called by VNC
 *
 *
 * @param pressed key state
 * @param key key code
 * @param cl client record
 */
static void rfbHandleKeyEvent(rfbBool pressed, rfbKeySym key, rfbClientPtr cl) {
    int jfxKeyCode;
    int eventType;
    NativeWindow window;

    window = glass_window_getFocusedWindow();

    if (window == NULL) {
        GLASS_LOG_FINE("Skipping event, no focused window");
        return;
    }

    jfxKeyCode = rfbGetJavaKeycodeFromPlatformKeyCode(key);

    GLASS_LOG_FINE("Sending KeyEvent: %s", pressed ? "PRESS" : "RELEASE");
    glass_application_notifyKeyEvent(genv, window,
                                     (pressed) ?
                                     com_sun_glass_events_KeyEvent_PRESS :
                                     com_sun_glass_events_KeyEvent_RELEASE,
                                     jfxKeyCode,
                                     JNI_FALSE/*rfb doesn't tell us when an event is a repeat*/);

}


static int  rfbGetJavaKeycodeFromPlatformKeyCode(int fbKey) {
    unsigned int i = 0;

    //currently just looping - performance later ..
    for (i = 0 ; i < (sizeof(keyMap) / sizeof(rfbKeytoJavaKeyPair)) ; ++i) {
        if (keyMap[i].fbKey == fbKey || keyMap[i].shiftedfbKey == fbKey) {
            return keyMap[i].javaKey;
        }
    }

    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;

}
