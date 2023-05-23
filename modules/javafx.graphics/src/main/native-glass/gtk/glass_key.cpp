/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "glass_key.h"
#include <com_sun_glass_events_KeyEvent.h>
#include <com_sun_glass_ui_gtk_GtkApplication.h>

#include <glib.h>
#include "glass_general.h"
#include <gdk/gdkkeysyms.h>
#include <X11/XKBlib.h>

// NOTE: This must be ordered by gdk_key
static const struct {
  unsigned short gdk_key;
  unsigned short glass_key;
} gdk_to_glass_keys[] = {
    { GDK_KEY_space, com_sun_glass_events_KeyEvent_VK_SPACE },
    { GDK_KEY_exclam, com_sun_glass_events_KeyEvent_VK_EXCLAMATION },
    { GDK_KEY_quotedbl, com_sun_glass_events_KeyEvent_VK_DOUBLE_QUOTE },
    { GDK_KEY_numbersign, com_sun_glass_events_KeyEvent_VK_NUMBER_SIGN },
    { GDK_KEY_dollar, com_sun_glass_events_KeyEvent_VK_DOLLAR },
    { GDK_KEY_ampersand, com_sun_glass_events_KeyEvent_VK_AMPERSAND },
    { GDK_KEY_apostrophe, com_sun_glass_events_KeyEvent_VK_QUOTE },
    { GDK_KEY_parenleft, com_sun_glass_events_KeyEvent_VK_LEFT_PARENTHESIS },
    { GDK_KEY_parenright, com_sun_glass_events_KeyEvent_VK_RIGHT_PARENTHESIS },
    { GDK_KEY_asterisk, com_sun_glass_events_KeyEvent_VK_ASTERISK },
    { GDK_KEY_plus, com_sun_glass_events_KeyEvent_VK_PLUS },
    { GDK_KEY_comma, com_sun_glass_events_KeyEvent_VK_COMMA },
    { GDK_KEY_minus, com_sun_glass_events_KeyEvent_VK_MINUS },
    { GDK_KEY_period, com_sun_glass_events_KeyEvent_VK_PERIOD },
    { GDK_KEY_slash, com_sun_glass_events_KeyEvent_VK_SLASH },
    { GDK_KEY_0, com_sun_glass_events_KeyEvent_VK_0 },
    { GDK_KEY_1, com_sun_glass_events_KeyEvent_VK_1 },
    { GDK_KEY_2, com_sun_glass_events_KeyEvent_VK_2 },
    { GDK_KEY_3, com_sun_glass_events_KeyEvent_VK_3 },
    { GDK_KEY_4, com_sun_glass_events_KeyEvent_VK_4 },
    { GDK_KEY_5, com_sun_glass_events_KeyEvent_VK_5 },
    { GDK_KEY_6, com_sun_glass_events_KeyEvent_VK_6 },
    { GDK_KEY_7, com_sun_glass_events_KeyEvent_VK_7 },
    { GDK_KEY_8, com_sun_glass_events_KeyEvent_VK_8 },
    { GDK_KEY_9, com_sun_glass_events_KeyEvent_VK_9 },
    { GDK_KEY_colon, com_sun_glass_events_KeyEvent_VK_COLON },
    { GDK_KEY_semicolon, com_sun_glass_events_KeyEvent_VK_SEMICOLON },
    { GDK_KEY_less, com_sun_glass_events_KeyEvent_VK_LESS },
    { GDK_KEY_equal, com_sun_glass_events_KeyEvent_VK_EQUALS },
    { GDK_KEY_greater, com_sun_glass_events_KeyEvent_VK_GREATER },
    { GDK_KEY_at, com_sun_glass_events_KeyEvent_VK_AT },
    { GDK_KEY_A, com_sun_glass_events_KeyEvent_VK_A },
    { GDK_KEY_B, com_sun_glass_events_KeyEvent_VK_B },
    { GDK_KEY_C, com_sun_glass_events_KeyEvent_VK_C },
    { GDK_KEY_D, com_sun_glass_events_KeyEvent_VK_D },
    { GDK_KEY_E, com_sun_glass_events_KeyEvent_VK_E },
    { GDK_KEY_F, com_sun_glass_events_KeyEvent_VK_F },
    { GDK_KEY_G, com_sun_glass_events_KeyEvent_VK_G },
    { GDK_KEY_H, com_sun_glass_events_KeyEvent_VK_H },
    { GDK_KEY_I, com_sun_glass_events_KeyEvent_VK_I },
    { GDK_KEY_J, com_sun_glass_events_KeyEvent_VK_J },
    { GDK_KEY_K, com_sun_glass_events_KeyEvent_VK_K },
    { GDK_KEY_L, com_sun_glass_events_KeyEvent_VK_L },
    { GDK_KEY_M, com_sun_glass_events_KeyEvent_VK_M },
    { GDK_KEY_N, com_sun_glass_events_KeyEvent_VK_N },
    { GDK_KEY_O, com_sun_glass_events_KeyEvent_VK_O },
    { GDK_KEY_P, com_sun_glass_events_KeyEvent_VK_P },
    { GDK_KEY_Q, com_sun_glass_events_KeyEvent_VK_Q },
    { GDK_KEY_R, com_sun_glass_events_KeyEvent_VK_R },
    { GDK_KEY_S, com_sun_glass_events_KeyEvent_VK_S },
    { GDK_KEY_T, com_sun_glass_events_KeyEvent_VK_T },
    { GDK_KEY_U, com_sun_glass_events_KeyEvent_VK_U },
    { GDK_KEY_V, com_sun_glass_events_KeyEvent_VK_V },
    { GDK_KEY_W, com_sun_glass_events_KeyEvent_VK_W },
    { GDK_KEY_X, com_sun_glass_events_KeyEvent_VK_X },
    { GDK_KEY_Y, com_sun_glass_events_KeyEvent_VK_Y },
    { GDK_KEY_Z, com_sun_glass_events_KeyEvent_VK_Z },
    { GDK_KEY_bracketleft, com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET },
    { GDK_KEY_backslash, com_sun_glass_events_KeyEvent_VK_BACK_SLASH },
    { GDK_KEY_bracketright, com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET },
    { GDK_KEY_asciicircum, com_sun_glass_events_KeyEvent_VK_CIRCUMFLEX },
    { GDK_KEY_underscore, com_sun_glass_events_KeyEvent_VK_UNDERSCORE },
    { GDK_KEY_grave, com_sun_glass_events_KeyEvent_VK_BACK_QUOTE },
    { GDK_KEY_a, com_sun_glass_events_KeyEvent_VK_A },
    { GDK_KEY_b, com_sun_glass_events_KeyEvent_VK_B },
    { GDK_KEY_c, com_sun_glass_events_KeyEvent_VK_C },
    { GDK_KEY_d, com_sun_glass_events_KeyEvent_VK_D },
    { GDK_KEY_e, com_sun_glass_events_KeyEvent_VK_E },
    { GDK_KEY_f, com_sun_glass_events_KeyEvent_VK_F },
    { GDK_KEY_g, com_sun_glass_events_KeyEvent_VK_G },
    { GDK_KEY_h, com_sun_glass_events_KeyEvent_VK_H },
    { GDK_KEY_i, com_sun_glass_events_KeyEvent_VK_I },
    { GDK_KEY_j, com_sun_glass_events_KeyEvent_VK_J },
    { GDK_KEY_k, com_sun_glass_events_KeyEvent_VK_K },
    { GDK_KEY_l, com_sun_glass_events_KeyEvent_VK_L },
    { GDK_KEY_m, com_sun_glass_events_KeyEvent_VK_M },
    { GDK_KEY_n, com_sun_glass_events_KeyEvent_VK_N },
    { GDK_KEY_o, com_sun_glass_events_KeyEvent_VK_O },
    { GDK_KEY_p, com_sun_glass_events_KeyEvent_VK_P },
    { GDK_KEY_q, com_sun_glass_events_KeyEvent_VK_Q },
    { GDK_KEY_r, com_sun_glass_events_KeyEvent_VK_R },
    { GDK_KEY_s, com_sun_glass_events_KeyEvent_VK_S },
    { GDK_KEY_t, com_sun_glass_events_KeyEvent_VK_T },
    { GDK_KEY_u, com_sun_glass_events_KeyEvent_VK_U },
    { GDK_KEY_v, com_sun_glass_events_KeyEvent_VK_V },
    { GDK_KEY_w, com_sun_glass_events_KeyEvent_VK_W },
    { GDK_KEY_x, com_sun_glass_events_KeyEvent_VK_X },
    { GDK_KEY_y, com_sun_glass_events_KeyEvent_VK_Y },
    { GDK_KEY_z, com_sun_glass_events_KeyEvent_VK_Z },
    { GDK_KEY_braceleft, com_sun_glass_events_KeyEvent_VK_BRACELEFT },
    { GDK_KEY_bar, com_sun_glass_events_KeyEvent_VK_BACK_SLASH },
    { GDK_KEY_braceright, com_sun_glass_events_KeyEvent_VK_BRACERIGHT },
    { GDK_KEY_exclamdown, com_sun_glass_events_KeyEvent_VK_INV_EXCLAMATION },
    { GDK_KEY_EuroSign, com_sun_glass_events_KeyEvent_VK_EURO_SIGN },
    { GDK_KEY_ISO_Level3_Shift, com_sun_glass_events_KeyEvent_VK_ALT_GRAPH },
    { GDK_KEY_dead_grave, com_sun_glass_events_KeyEvent_VK_DEAD_GRAVE },
    { GDK_KEY_dead_acute, com_sun_glass_events_KeyEvent_VK_DEAD_ACUTE },
    { GDK_KEY_dead_circumflex, com_sun_glass_events_KeyEvent_VK_DEAD_CIRCUMFLEX },
    { GDK_KEY_dead_tilde, com_sun_glass_events_KeyEvent_VK_DEAD_TILDE },
    { GDK_KEY_dead_macron, com_sun_glass_events_KeyEvent_VK_DEAD_MACRON },
    { GDK_KEY_dead_breve, com_sun_glass_events_KeyEvent_VK_DEAD_BREVE },
    { GDK_KEY_dead_abovedot, com_sun_glass_events_KeyEvent_VK_DEAD_ABOVEDOT },
    { GDK_KEY_dead_diaeresis, com_sun_glass_events_KeyEvent_VK_DEAD_DIAERESIS },
    { GDK_KEY_dead_abovering, com_sun_glass_events_KeyEvent_VK_DEAD_ABOVERING },
    { GDK_KEY_dead_doubleacute, com_sun_glass_events_KeyEvent_VK_DEAD_DOUBLEACUTE },
    { GDK_KEY_dead_caron, com_sun_glass_events_KeyEvent_VK_DEAD_CARON },
    { GDK_KEY_dead_cedilla, com_sun_glass_events_KeyEvent_VK_DEAD_CEDILLA },
    { GDK_KEY_dead_ogonek, com_sun_glass_events_KeyEvent_VK_DEAD_OGONEK },
    { GDK_KEY_dead_iota, com_sun_glass_events_KeyEvent_VK_DEAD_IOTA },
    { GDK_KEY_dead_voiced_sound, com_sun_glass_events_KeyEvent_VK_DEAD_VOICED_SOUND },
    { GDK_KEY_dead_semivoiced_sound, com_sun_glass_events_KeyEvent_VK_DEAD_SEMIVOICED_SOUND },
    { GDK_KEY_BackSpace, com_sun_glass_events_KeyEvent_VK_BACKSPACE },
    { GDK_KEY_Tab, com_sun_glass_events_KeyEvent_VK_TAB },
    { GDK_KEY_Clear, com_sun_glass_events_KeyEvent_VK_CLEAR },
    { GDK_KEY_Return, com_sun_glass_events_KeyEvent_VK_ENTER },
    { GDK_KEY_Pause, com_sun_glass_events_KeyEvent_VK_PAUSE },
    { GDK_KEY_Scroll_Lock, com_sun_glass_events_KeyEvent_VK_SCROLL_LOCK },
    { GDK_KEY_Escape, com_sun_glass_events_KeyEvent_VK_ESCAPE },
    { GDK_KEY_Home, com_sun_glass_events_KeyEvent_VK_HOME },
    { GDK_KEY_Left, com_sun_glass_events_KeyEvent_VK_LEFT },
    { GDK_KEY_Up, com_sun_glass_events_KeyEvent_VK_UP },
    { GDK_KEY_Right, com_sun_glass_events_KeyEvent_VK_RIGHT },
    { GDK_KEY_Down, com_sun_glass_events_KeyEvent_VK_DOWN },
    { GDK_KEY_Page_Up, com_sun_glass_events_KeyEvent_VK_PAGE_UP },
    { GDK_KEY_Prior, com_sun_glass_events_KeyEvent_VK_PAGE_UP },
    { GDK_KEY_Page_Down, com_sun_glass_events_KeyEvent_VK_PAGE_DOWN },
    { GDK_KEY_Next, com_sun_glass_events_KeyEvent_VK_PAGE_DOWN },
    { GDK_KEY_End, com_sun_glass_events_KeyEvent_VK_END },
    { GDK_KEY_Print, com_sun_glass_events_KeyEvent_VK_PRINTSCREEN },
    { GDK_KEY_Insert, com_sun_glass_events_KeyEvent_VK_INSERT },
    { GDK_KEY_Menu, com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU },
    { GDK_KEY_Help, com_sun_glass_events_KeyEvent_VK_HELP },
    { GDK_KEY_Num_Lock, com_sun_glass_events_KeyEvent_VK_NUM_LOCK },
    { GDK_KEY_KP_Enter, com_sun_glass_events_KeyEvent_VK_ENTER },
    { GDK_KEY_KP_Home, com_sun_glass_events_KeyEvent_VK_HOME },
    { GDK_KEY_KP_Left, com_sun_glass_events_KeyEvent_VK_LEFT },
    { GDK_KEY_KP_Up, com_sun_glass_events_KeyEvent_VK_UP },
    { GDK_KEY_KP_Right, com_sun_glass_events_KeyEvent_VK_RIGHT },
    { GDK_KEY_KP_Down, com_sun_glass_events_KeyEvent_VK_DOWN },
    { GDK_KEY_KP_Prior, com_sun_glass_events_KeyEvent_VK_PAGE_UP },
    { GDK_KEY_KP_Page_Up, com_sun_glass_events_KeyEvent_VK_PAGE_UP },
    { GDK_KEY_KP_Next, com_sun_glass_events_KeyEvent_VK_PAGE_DOWN },
    { GDK_KEY_KP_Page_Down, com_sun_glass_events_KeyEvent_VK_PAGE_DOWN },
    { GDK_KEY_KP_End, com_sun_glass_events_KeyEvent_VK_END },
    { GDK_KEY_KP_Begin, com_sun_glass_events_KeyEvent_VK_CLEAR },
    { GDK_KEY_KP_Insert, com_sun_glass_events_KeyEvent_VK_INSERT },
    { GDK_KEY_KP_Delete, com_sun_glass_events_KeyEvent_VK_DELETE },
    { GDK_KEY_KP_Multiply, com_sun_glass_events_KeyEvent_VK_MULTIPLY },
    { GDK_KEY_KP_Add, com_sun_glass_events_KeyEvent_VK_ADD },
    { GDK_KEY_KP_Separator, com_sun_glass_events_KeyEvent_VK_SEPARATOR },
    { GDK_KEY_KP_Subtract, com_sun_glass_events_KeyEvent_VK_SUBTRACT },
    { GDK_KEY_KP_Decimal, com_sun_glass_events_KeyEvent_VK_DECIMAL },
    { GDK_KEY_KP_Divide, com_sun_glass_events_KeyEvent_VK_DIVIDE },
    { GDK_KEY_KP_0, com_sun_glass_events_KeyEvent_VK_NUMPAD0 },
    { GDK_KEY_KP_1, com_sun_glass_events_KeyEvent_VK_NUMPAD1 },
    { GDK_KEY_KP_2, com_sun_glass_events_KeyEvent_VK_NUMPAD2 },
    { GDK_KEY_KP_3, com_sun_glass_events_KeyEvent_VK_NUMPAD3 },
    { GDK_KEY_KP_4, com_sun_glass_events_KeyEvent_VK_NUMPAD4 },
    { GDK_KEY_KP_5, com_sun_glass_events_KeyEvent_VK_NUMPAD5 },
    { GDK_KEY_KP_6, com_sun_glass_events_KeyEvent_VK_NUMPAD6 },
    { GDK_KEY_KP_7, com_sun_glass_events_KeyEvent_VK_NUMPAD7 },
    { GDK_KEY_KP_8, com_sun_glass_events_KeyEvent_VK_NUMPAD8 },
    { GDK_KEY_KP_9, com_sun_glass_events_KeyEvent_VK_NUMPAD9 },
    { GDK_KEY_F1, com_sun_glass_events_KeyEvent_VK_F1 },
    { GDK_KEY_F2, com_sun_glass_events_KeyEvent_VK_F2 },
    { GDK_KEY_F3, com_sun_glass_events_KeyEvent_VK_F3 },
    { GDK_KEY_F4, com_sun_glass_events_KeyEvent_VK_F4 },
    { GDK_KEY_F5, com_sun_glass_events_KeyEvent_VK_F5 },
    { GDK_KEY_F6, com_sun_glass_events_KeyEvent_VK_F6 },
    { GDK_KEY_F7, com_sun_glass_events_KeyEvent_VK_F7 },
    { GDK_KEY_F8, com_sun_glass_events_KeyEvent_VK_F8 },
    { GDK_KEY_F9, com_sun_glass_events_KeyEvent_VK_F9 },
    { GDK_KEY_F10, com_sun_glass_events_KeyEvent_VK_F10 },
    { GDK_KEY_F11, com_sun_glass_events_KeyEvent_VK_F11 },
    { GDK_KEY_F12, com_sun_glass_events_KeyEvent_VK_F12 },
    { GDK_KEY_Shift_L, com_sun_glass_events_KeyEvent_VK_SHIFT },
    { GDK_KEY_Shift_R, com_sun_glass_events_KeyEvent_VK_SHIFT },
    { GDK_KEY_Control_L, com_sun_glass_events_KeyEvent_VK_CONTROL },
    { GDK_KEY_Control_R, com_sun_glass_events_KeyEvent_VK_CONTROL },
    { GDK_KEY_Caps_Lock, com_sun_glass_events_KeyEvent_VK_CAPS_LOCK },
    { GDK_KEY_Meta_L, com_sun_glass_events_KeyEvent_VK_WINDOWS },
    { GDK_KEY_Meta_R, com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU },
    { GDK_KEY_Alt_L, com_sun_glass_events_KeyEvent_VK_ALT },
    { GDK_KEY_Alt_R, com_sun_glass_events_KeyEvent_VK_ALT_GRAPH },
    { GDK_KEY_Super_L, com_sun_glass_events_KeyEvent_VK_WINDOWS },
    { GDK_KEY_Super_R, com_sun_glass_events_KeyEvent_VK_WINDOWS },
    { GDK_KEY_Delete, com_sun_glass_events_KeyEvent_VK_DELETE }
};

jint gdk_keyval_to_glass(guint keyval) {
    int min = 0;
    int max = G_N_ELEMENTS(gdk_to_glass_keys) - 1;
    int mid;

    // binary search
    while (max >= min) {
        mid = (min + max) / 2;
        if (gdk_to_glass_keys[mid].gdk_key < keyval) {
            min = mid + 1;
        } else if (gdk_to_glass_keys[mid].gdk_key > keyval) {
            max = mid - 1;
        } else {
            return gdk_to_glass_keys[mid].glass_key;
        }
    }

    return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
}

jint get_glass_key(GdkEventKey* e) {
    guint keyValue;
    guint state = e->state & GDK_MOD2_MASK; //NumLock test

    gdk_keymap_translate_keyboard_state(gdk_keymap_get_default(),
            e->hardware_keycode, static_cast<GdkModifierType>(state), e->group,
            &keyValue, NULL, NULL, NULL);

    jint key = gdk_keyval_to_glass(e->keyval);

    if (!key) {
        // We failed to find a keyval in our keymap, this may happen with
        // non-latin layouts(e.g. Cyrillic). So here we try to find a keyval
        // from a default layout(we assume that it is a US-like one).
        GdkKeymapKey kk;
        kk.keycode = e->hardware_keycode;
        kk.group = kk.level = 0;

        keyValue = gdk_keymap_lookup_key(gdk_keymap_get_default(), &kk);

        key = gdk_keyval_to_glass(keyValue);
    }

    return key;
}

gint find_gdk_keyval_for_glass_keycode(jint code) {
    int max = G_N_ELEMENTS(gdk_to_glass_keys);

    // cant't do binary search because it's unorderd
    for (int i = 0; i < max; i++) {
        if (gdk_to_glass_keys[i].glass_key == code) {
            return gdk_to_glass_keys[i].gdk_key;
        }
    }

    return -1;
}

jint gdk_modifier_mask_to_glass(guint mask) {
    jint glass_mask = 0;
    glass_mask |= (mask & GDK_SHIFT_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_SHIFT : 0;
    glass_mask |= (mask & GDK_CONTROL_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_CONTROL : 0;
    glass_mask |= (mask & GDK_MOD1_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_ALT : 0;
    glass_mask |= (mask & GDK_META_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_ALT : 0; // XXX: is this OK?
    glass_mask |= (mask & GDK_BUTTON1_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY : 0;
    glass_mask |= (mask & GDK_BUTTON2_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE : 0;
    glass_mask |= (mask & GDK_BUTTON3_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY : 0;
    glass_mask |= (mask & GDK_BUTTON4_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK : 0;
    glass_mask |= (mask & GDK_BUTTON5_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD : 0;
    glass_mask |= (mask & GDK_SUPER_MASK) ? com_sun_glass_events_KeyEvent_MODIFIER_WINDOWS : 0; // XXX: is this OK?

    return glass_mask;
}

jint glass_key_to_modifier(jint glassKey) {
    switch (glassKey) {
        case com_sun_glass_events_KeyEvent_VK_SHIFT:
            return com_sun_glass_events_KeyEvent_MODIFIER_SHIFT;
        case com_sun_glass_events_KeyEvent_VK_ALT:
        case com_sun_glass_events_KeyEvent_VK_ALT_GRAPH:
            return com_sun_glass_events_KeyEvent_MODIFIER_ALT;
        case com_sun_glass_events_KeyEvent_VK_CONTROL:
            return com_sun_glass_events_KeyEvent_MODIFIER_CONTROL;
        case com_sun_glass_events_KeyEvent_VK_WINDOWS:
            return com_sun_glass_events_KeyEvent_MODIFIER_WINDOWS;
        default:
            return com_sun_glass_events_KeyEvent_MODIFIER_NONE;
    }
}
extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _getKeyCodeForChar
 * Signature: (C)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1getKeyCodeForChar
  (JNIEnv *env, jobject jApplication, jchar character)
{
    (void)env;
    (void)jApplication;

    gunichar *ucs_char = g_utf16_to_ucs4(&character, 1, NULL, NULL, NULL);
    if (ucs_char == NULL) {
        return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    }

    guint keyval = gdk_unicode_to_keyval(*ucs_char);

    if (keyval == (*ucs_char | 0x01000000)) {
        g_free(ucs_char);
        return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    }

    g_free(ucs_char);

    return gdk_keyval_to_glass(keyval);
}

/*
 * Function to determine whether the Xkb extention is available. This is a
 * precaution against X protocol errors, although it should be available on all
 * Linux systems.
 */

static Bool xkbInitialized = False;
static Bool xkbAvailable = False;

static Bool isXkbAvailable(Display *display) {
    if (!xkbInitialized) {
        int xkbMajor = XkbMajorVersion;
        int xkbMinor = XkbMinorVersion;
        xkbAvailable = XkbQueryExtension(display, NULL, NULL, NULL, &xkbMajor, &xkbMinor);
        xkbInitialized = True;
    }
    return xkbAvailable;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _isKeyLocked
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1isKeyLocked
  (JNIEnv * env, jobject obj, jint keyCode)
{
    GdkKeymap *keymap = gdk_keymap_get_default();

    switch (keyCode) {
        case com_sun_glass_events_KeyEvent_VK_CAPS_LOCK:
            return (gdk_keymap_get_caps_lock_state(keymap))
                ? com_sun_glass_events_KeyEvent_KEY_LOCK_ON
                : com_sun_glass_events_KeyEvent_KEY_LOCK_OFF;

        case com_sun_glass_events_KeyEvent_VK_NUM_LOCK:
            return (gdk_keymap_get_num_lock_state(keymap))
                ? com_sun_glass_events_KeyEvent_KEY_LOCK_ON
                : com_sun_glass_events_KeyEvent_KEY_LOCK_OFF;
    }

    return com_sun_glass_events_KeyEvent_KEY_LOCK_UNKNOWN;
}

} // extern "C"
