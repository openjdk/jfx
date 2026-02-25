/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "scancodes.h"
#include <map>

static gboolean key_initialized = FALSE;
static gboolean key_initialized_remote_desktop = FALSE;
extern gboolean isRemoteDesktop;

// Map from keyval to Java KeyCode
static GHashTable *keymap;

// There may be more than one mapping from a keyvalue to a Java KeyCode in the
// keymap. That can produce unpredictable results when a Robot tries to work
// backward from KeyCode to keyvalue. This map is consulted first to resolve
// the ambiguity.
static std::map<jint, guint32> robot_java_to_keyval;

// GDK_KEY_{A..Z} to scancode map for QWERTY layout
static std::map<gint, guint32> keyval_to_scancode;

// As the user types we build a map from character to Java KeyCode. We use
// this map in getKeyCodeForChar which ensures we only reference keys that
// are on the user's keyboard. GDK calls that query the GdkKeymap are slow
// (they scan all the maps each time) and can return keys not present on the
// keyboard.
static std::map<guint32, jint> char_to_java_code;

static void glass_g_hash_table_insert_int(GHashTable *table, gint key, gint value)
{
    g_hash_table_insert(table, GINT_TO_POINTER(key), GINT_TO_POINTER(value));
}

static void initialize_key()
{
    keymap = g_hash_table_new(g_direct_hash, g_direct_equal);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Return), com_sun_glass_events_KeyEvent_VK_ENTER);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(BackSpace), com_sun_glass_events_KeyEvent_VK_BACKSPACE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Tab), com_sun_glass_events_KeyEvent_VK_TAB);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Clear), com_sun_glass_events_KeyEvent_VK_CLEAR); //XXX what is this?
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Pause), com_sun_glass_events_KeyEvent_VK_PAUSE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Escape), com_sun_glass_events_KeyEvent_VK_ESCAPE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(space), com_sun_glass_events_KeyEvent_VK_SPACE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Delete), com_sun_glass_events_KeyEvent_VK_DELETE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Print), com_sun_glass_events_KeyEvent_VK_PRINTSCREEN); //XXX is correct?
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Insert), com_sun_glass_events_KeyEvent_VK_INSERT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Help), com_sun_glass_events_KeyEvent_VK_HELP); //XXX what is this?

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Shift_L), com_sun_glass_events_KeyEvent_VK_SHIFT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Shift_R), com_sun_glass_events_KeyEvent_VK_SHIFT); //XXX is this correct?
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Control_L), com_sun_glass_events_KeyEvent_VK_CONTROL);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Control_R), com_sun_glass_events_KeyEvent_VK_CONTROL);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Alt_L), com_sun_glass_events_KeyEvent_VK_ALT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Alt_R), com_sun_glass_events_KeyEvent_VK_ALT_GRAPH);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Super_L), com_sun_glass_events_KeyEvent_VK_WINDOWS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Super_R), com_sun_glass_events_KeyEvent_VK_WINDOWS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Menu), com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Meta_L), com_sun_glass_events_KeyEvent_VK_WINDOWS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Meta_R), com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU);//XXX is this correct?
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Caps_Lock), com_sun_glass_events_KeyEvent_VK_CAPS_LOCK);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Num_Lock), com_sun_glass_events_KeyEvent_VK_NUM_LOCK);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Scroll_Lock), com_sun_glass_events_KeyEvent_VK_SCROLL_LOCK);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Page_Up), com_sun_glass_events_KeyEvent_VK_PAGE_UP);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Prior), com_sun_glass_events_KeyEvent_VK_PAGE_UP);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Page_Down), com_sun_glass_events_KeyEvent_VK_PAGE_DOWN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Next), com_sun_glass_events_KeyEvent_VK_PAGE_DOWN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(End), com_sun_glass_events_KeyEvent_VK_END);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Home), com_sun_glass_events_KeyEvent_VK_HOME);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Left), com_sun_glass_events_KeyEvent_VK_LEFT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Right), com_sun_glass_events_KeyEvent_VK_RIGHT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Up), com_sun_glass_events_KeyEvent_VK_UP);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Down), com_sun_glass_events_KeyEvent_VK_DOWN);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(comma), com_sun_glass_events_KeyEvent_VK_COMMA);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(minus), com_sun_glass_events_KeyEvent_VK_MINUS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(period), com_sun_glass_events_KeyEvent_VK_PERIOD);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(slash), com_sun_glass_events_KeyEvent_VK_SLASH);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(semicolon), com_sun_glass_events_KeyEvent_VK_SEMICOLON);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(equal), com_sun_glass_events_KeyEvent_VK_EQUALS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(bracketleft), com_sun_glass_events_KeyEvent_VK_OPEN_BRACKET);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(bracketright), com_sun_glass_events_KeyEvent_VK_CLOSE_BRACKET);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(backslash), com_sun_glass_events_KeyEvent_VK_BACK_SLASH);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(bar), com_sun_glass_events_KeyEvent_VK_BACK_SLASH);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Multiply), com_sun_glass_events_KeyEvent_VK_MULTIPLY);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Add), com_sun_glass_events_KeyEvent_VK_ADD);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Separator), com_sun_glass_events_KeyEvent_VK_SEPARATOR);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Subtract), com_sun_glass_events_KeyEvent_VK_SUBTRACT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Decimal), com_sun_glass_events_KeyEvent_VK_DECIMAL);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(apostrophe), com_sun_glass_events_KeyEvent_VK_QUOTE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(grave), com_sun_glass_events_KeyEvent_VK_BACK_QUOTE);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(ampersand), com_sun_glass_events_KeyEvent_VK_AMPERSAND);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(asterisk), com_sun_glass_events_KeyEvent_VK_ASTERISK);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(quotedbl), com_sun_glass_events_KeyEvent_VK_DOUBLE_QUOTE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(less), com_sun_glass_events_KeyEvent_VK_LESS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(greater), com_sun_glass_events_KeyEvent_VK_GREATER);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(braceleft), com_sun_glass_events_KeyEvent_VK_BRACELEFT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(braceright), com_sun_glass_events_KeyEvent_VK_BRACERIGHT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(at), com_sun_glass_events_KeyEvent_VK_AT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(colon), com_sun_glass_events_KeyEvent_VK_COLON);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(asciicircum), com_sun_glass_events_KeyEvent_VK_CIRCUMFLEX);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(dollar), com_sun_glass_events_KeyEvent_VK_DOLLAR);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(EuroSign), com_sun_glass_events_KeyEvent_VK_EURO_SIGN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(exclam), com_sun_glass_events_KeyEvent_VK_EXCLAMATION);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(exclamdown), com_sun_glass_events_KeyEvent_VK_INV_EXCLAMATION);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(parenleft), com_sun_glass_events_KeyEvent_VK_LEFT_PARENTHESIS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(numbersign), com_sun_glass_events_KeyEvent_VK_NUMBER_SIGN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(plus), com_sun_glass_events_KeyEvent_VK_PLUS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(parenright), com_sun_glass_events_KeyEvent_VK_RIGHT_PARENTHESIS);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(underscore), com_sun_glass_events_KeyEvent_VK_UNDERSCORE);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(0), com_sun_glass_events_KeyEvent_VK_0);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(1), com_sun_glass_events_KeyEvent_VK_1);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(2), com_sun_glass_events_KeyEvent_VK_2);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(3), com_sun_glass_events_KeyEvent_VK_3);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(4), com_sun_glass_events_KeyEvent_VK_4);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(5), com_sun_glass_events_KeyEvent_VK_5);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(6), com_sun_glass_events_KeyEvent_VK_6);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(7), com_sun_glass_events_KeyEvent_VK_7);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(8), com_sun_glass_events_KeyEvent_VK_8);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(9), com_sun_glass_events_KeyEvent_VK_9);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(a), com_sun_glass_events_KeyEvent_VK_A);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(b), com_sun_glass_events_KeyEvent_VK_B);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(c), com_sun_glass_events_KeyEvent_VK_C);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(d), com_sun_glass_events_KeyEvent_VK_D);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(e), com_sun_glass_events_KeyEvent_VK_E);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(f), com_sun_glass_events_KeyEvent_VK_F);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(g), com_sun_glass_events_KeyEvent_VK_G);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(h), com_sun_glass_events_KeyEvent_VK_H);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(i), com_sun_glass_events_KeyEvent_VK_I);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(j), com_sun_glass_events_KeyEvent_VK_J);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(k), com_sun_glass_events_KeyEvent_VK_K);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(l), com_sun_glass_events_KeyEvent_VK_L);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(m), com_sun_glass_events_KeyEvent_VK_M);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(n), com_sun_glass_events_KeyEvent_VK_N);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(o), com_sun_glass_events_KeyEvent_VK_O);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(p), com_sun_glass_events_KeyEvent_VK_P);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(q), com_sun_glass_events_KeyEvent_VK_Q);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(r), com_sun_glass_events_KeyEvent_VK_R);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(s), com_sun_glass_events_KeyEvent_VK_S);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(t), com_sun_glass_events_KeyEvent_VK_T);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(u), com_sun_glass_events_KeyEvent_VK_U);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(v), com_sun_glass_events_KeyEvent_VK_V);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(w), com_sun_glass_events_KeyEvent_VK_W);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(x), com_sun_glass_events_KeyEvent_VK_X);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(y), com_sun_glass_events_KeyEvent_VK_Y);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(z), com_sun_glass_events_KeyEvent_VK_Z);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(A), com_sun_glass_events_KeyEvent_VK_A);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(B), com_sun_glass_events_KeyEvent_VK_B);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(C), com_sun_glass_events_KeyEvent_VK_C);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(D), com_sun_glass_events_KeyEvent_VK_D);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(E), com_sun_glass_events_KeyEvent_VK_E);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F), com_sun_glass_events_KeyEvent_VK_F);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(G), com_sun_glass_events_KeyEvent_VK_G);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(H), com_sun_glass_events_KeyEvent_VK_H);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(I), com_sun_glass_events_KeyEvent_VK_I);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(J), com_sun_glass_events_KeyEvent_VK_J);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(K), com_sun_glass_events_KeyEvent_VK_K);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(L), com_sun_glass_events_KeyEvent_VK_L);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(M), com_sun_glass_events_KeyEvent_VK_M);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(N), com_sun_glass_events_KeyEvent_VK_N);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(O), com_sun_glass_events_KeyEvent_VK_O);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(P), com_sun_glass_events_KeyEvent_VK_P);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Q), com_sun_glass_events_KeyEvent_VK_Q);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(R), com_sun_glass_events_KeyEvent_VK_R);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(S), com_sun_glass_events_KeyEvent_VK_S);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(T), com_sun_glass_events_KeyEvent_VK_T);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(U), com_sun_glass_events_KeyEvent_VK_U);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(V), com_sun_glass_events_KeyEvent_VK_V);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(W), com_sun_glass_events_KeyEvent_VK_W);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(X), com_sun_glass_events_KeyEvent_VK_X);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Y), com_sun_glass_events_KeyEvent_VK_Y);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(Z), com_sun_glass_events_KeyEvent_VK_Z);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_0), com_sun_glass_events_KeyEvent_VK_NUMPAD0);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_1), com_sun_glass_events_KeyEvent_VK_NUMPAD1);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_2), com_sun_glass_events_KeyEvent_VK_NUMPAD2);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_3), com_sun_glass_events_KeyEvent_VK_NUMPAD3);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_4), com_sun_glass_events_KeyEvent_VK_NUMPAD4);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_5), com_sun_glass_events_KeyEvent_VK_NUMPAD5);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_6), com_sun_glass_events_KeyEvent_VK_NUMPAD6);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_7), com_sun_glass_events_KeyEvent_VK_NUMPAD7);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_8), com_sun_glass_events_KeyEvent_VK_NUMPAD8);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_9), com_sun_glass_events_KeyEvent_VK_NUMPAD9);

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Enter), com_sun_glass_events_KeyEvent_VK_ENTER);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Home), com_sun_glass_events_KeyEvent_VK_HOME);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Left), com_sun_glass_events_KeyEvent_VK_LEFT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Up), com_sun_glass_events_KeyEvent_VK_UP);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Right), com_sun_glass_events_KeyEvent_VK_RIGHT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Down), com_sun_glass_events_KeyEvent_VK_DOWN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Prior), com_sun_glass_events_KeyEvent_VK_PAGE_UP);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Page_Up), com_sun_glass_events_KeyEvent_VK_PAGE_UP);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Next), com_sun_glass_events_KeyEvent_VK_PAGE_DOWN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Page_Down), com_sun_glass_events_KeyEvent_VK_PAGE_DOWN);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_End), com_sun_glass_events_KeyEvent_VK_END);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Insert), com_sun_glass_events_KeyEvent_VK_INSERT);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Delete), com_sun_glass_events_KeyEvent_VK_DELETE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Divide), com_sun_glass_events_KeyEvent_VK_DIVIDE);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(KP_Begin),
            com_sun_glass_events_KeyEvent_VK_CLEAR); // 5 key on keypad with Num Lock turned off

    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F1), com_sun_glass_events_KeyEvent_VK_F1);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F2), com_sun_glass_events_KeyEvent_VK_F2);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F3), com_sun_glass_events_KeyEvent_VK_F3);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F4), com_sun_glass_events_KeyEvent_VK_F4);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F5), com_sun_glass_events_KeyEvent_VK_F5);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F6), com_sun_glass_events_KeyEvent_VK_F6);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F7), com_sun_glass_events_KeyEvent_VK_F7);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F8), com_sun_glass_events_KeyEvent_VK_F8);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F9), com_sun_glass_events_KeyEvent_VK_F9);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F10), com_sun_glass_events_KeyEvent_VK_F10);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F11), com_sun_glass_events_KeyEvent_VK_F11);
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(F12), com_sun_glass_events_KeyEvent_VK_F12);

    // Used by ISO keyboards
    glass_g_hash_table_insert_int(keymap, GLASS_GDK_KEY_CONSTANT(ISO_Level3_Shift), com_sun_glass_events_KeyEvent_VK_ALT_GRAPH);

    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_ENTER]   = GLASS_GDK_KEY_CONSTANT(Return);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_CLEAR]   = GLASS_GDK_KEY_CONSTANT(Clear);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_PAGE_UP] = GLASS_GDK_KEY_CONSTANT(Page_Up);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_END]     = GLASS_GDK_KEY_CONSTANT(End);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_HOME]    = GLASS_GDK_KEY_CONSTANT(Home);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_LEFT]    = GLASS_GDK_KEY_CONSTANT(Left);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_UP]      = GLASS_GDK_KEY_CONSTANT(Up);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_RIGHT]   = GLASS_GDK_KEY_CONSTANT(Right);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_DOWN]    = GLASS_GDK_KEY_CONSTANT(Down);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_DELETE]  = GLASS_GDK_KEY_CONSTANT(Delete);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_BACK_SLASH] = GLASS_GDK_KEY_CONSTANT(backslash);
    // This works on all keyboards, both ISO and ANSI.
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_ALT_GRAPH]  = GLASS_GDK_KEY_CONSTANT(ISO_Level3_Shift);
}

static void initialize_key_remote_desktop() {
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(a)] = SCANCODE_A;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(b)] = SCANCODE_B;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(c)] = SCANCODE_C;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(d)] = SCANCODE_D;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(e)] = SCANCODE_E;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(f)] = SCANCODE_F;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(g)] = SCANCODE_G;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(h)] = SCANCODE_H;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(i)] = SCANCODE_I;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(j)] = SCANCODE_J;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(k)] = SCANCODE_K;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(l)] = SCANCODE_L;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(m)] = SCANCODE_M;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(n)] = SCANCODE_N;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(o)] = SCANCODE_O;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(p)] = SCANCODE_P;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(q)] = SCANCODE_Q;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(r)] = SCANCODE_R;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(s)] = SCANCODE_S;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(t)] = SCANCODE_T;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(u)] = SCANCODE_U;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(v)] = SCANCODE_V;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(w)] = SCANCODE_W;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(x)] = SCANCODE_X;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(y)] = SCANCODE_Y;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(z)] = SCANCODE_Z;

    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(A)] = SCANCODE_A;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(B)] = SCANCODE_B;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(C)] = SCANCODE_C;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(D)] = SCANCODE_D;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(E)] = SCANCODE_E;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(F)] = SCANCODE_F;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(G)] = SCANCODE_G;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(H)] = SCANCODE_H;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(I)] = SCANCODE_I;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(J)] = SCANCODE_J;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(K)] = SCANCODE_K;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(L)] = SCANCODE_L;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(M)] = SCANCODE_M;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(N)] = SCANCODE_N;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(O)] = SCANCODE_O;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(P)] = SCANCODE_P;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(Q)] = SCANCODE_Q;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(R)] = SCANCODE_R;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(S)] = SCANCODE_S;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(T)] = SCANCODE_T;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(U)] = SCANCODE_U;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(V)] = SCANCODE_V;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(W)] = SCANCODE_W;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(X)] = SCANCODE_X;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(Y)] = SCANCODE_Y;
    keyval_to_scancode[GLASS_GDK_KEY_CONSTANT(Z)] = SCANCODE_Z;

    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD0] = GLASS_GDK_KEY_CONSTANT(KP_Insert);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD1] = GLASS_GDK_KEY_CONSTANT(KP_End);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD2] = GLASS_GDK_KEY_CONSTANT(KP_Down);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD3] = GLASS_GDK_KEY_CONSTANT(KP_Page_Down);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD4] = GLASS_GDK_KEY_CONSTANT(KP_Left);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD5] = GLASS_GDK_KEY_CONSTANT(KP_Begin);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD6] = GLASS_GDK_KEY_CONSTANT(KP_Right);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD7] = GLASS_GDK_KEY_CONSTANT(KP_Home);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD8] = GLASS_GDK_KEY_CONSTANT(KP_Up);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_NUMPAD9] = GLASS_GDK_KEY_CONSTANT(KP_Page_Up);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_DECIMAL] = GLASS_GDK_KEY_CONSTANT(KP_Delete);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_WINDOWS] = GLASS_GDK_KEY_CONSTANT(Super_L);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_CONTEXT_MENU] = GLASS_GDK_KEY_CONSTANT(Menu);
    robot_java_to_keyval[com_sun_glass_events_KeyEvent_VK_CLEAR] = GLASS_GDK_KEY_CONSTANT(KP_Begin);

    // javafx/scene/input/KeyCode.java
    robot_java_to_keyval[0xE0] = GLASS_GDK_KEY_CONSTANT(KP_Up);    // Numpad Up
    robot_java_to_keyval[0xE1] = GLASS_GDK_KEY_CONSTANT(KP_Down);  // Numpad Down
    robot_java_to_keyval[0xE2] = GLASS_GDK_KEY_CONSTANT(KP_Left);  // Numpad Left
    robot_java_to_keyval[0xE3] = GLASS_GDK_KEY_CONSTANT(KP_Right); // Numpad Right
}

static void keys_changed_signal(GdkKeymap* k, gpointer data) {
    char_to_java_code.clear();
}

static void init_keymap() {
    if (!key_initialized) {
        initialize_key();
        key_initialized = TRUE;

        GdkKeymap* gdk_keymap = gdk_keymap_get_for_display(gdk_display_get_default());

        // The documented signal emitted when the keyboard layout changes
        g_signal_connect(G_OBJECT(gdk_keymap), "keys-changed",
                         G_CALLBACK(keys_changed_signal), nullptr);
        // On some versions of X11 this is the actual signal emitted
        g_signal_connect(G_OBJECT(gdk_keymap), "keys_changed",
                         G_CALLBACK(keys_changed_signal), nullptr);
    }

    if (isRemoteDesktop && !key_initialized_remote_desktop) {
        initialize_key_remote_desktop();
        key_initialized_remote_desktop = TRUE;
    }
}

jint gdk_keyval_to_glass(guint keyval)
{
    init_keymap();
    return GPOINTER_TO_INT(g_hash_table_lookup(keymap, GINT_TO_POINTER(keyval)));
}

// For a given keypress event we update the char => KeyCode map multiple times
// each with a different shift level encoded in the state argument.
static void record_character(GdkKeymap *keymap, GdkEventKey *e, guint state, jint javaKeyCode) {
    guint keyValue;

    if (gdk_keymap_translate_keyboard_state(keymap, e->hardware_keycode,
                                            static_cast<GdkModifierType>(state), e->group,
                                            &keyValue, NULL, NULL, NULL)) {
        guint32 ucs = gdk_keyval_to_unicode(keyValue);
        if (ucs) {
            char_to_java_code[ucs] = javaKeyCode;
        }
    };
}

jint get_glass_key(GdkEventKey* e) {
    init_keymap();

    guint keyValue;
    guint state = e->state & GDK_MOD2_MASK; //NumLock test
    GdkKeymap* gdk_keymap = gdk_keymap_get_for_display(gdk_display_get_default());

    gdk_keymap_translate_keyboard_state(gdk_keymap,
            e->hardware_keycode, static_cast<GdkModifierType>(state), e->group,
            &keyValue, NULL, NULL, NULL);

    jint key = GPOINTER_TO_INT(g_hash_table_lookup(keymap,
            GINT_TO_POINTER(keyValue)));

    if (!key) {
        // We failed to find a keyval in our keymap, this may happen with
        // non-latin layouts(e.g. Cyrillic). So here we try to find a keyval
        // from a default layout(we assume that it is a US-like one).
        GdkKeymapKey kk;
        kk.keycode = e->hardware_keycode;
        kk.group = kk.level = 0;

        keyValue = gdk_keymap_lookup_key(gdk_keymap, &kk);

        key = GPOINTER_TO_INT(g_hash_table_lookup(keymap,
                GINT_TO_POINTER(keyValue)));
    }

    // If this mapped to a Java code record which characters are
    // generated at different shift levels.
    if (key) {
        // Unshifted and Shift
        record_character(gdk_keymap, e, state, key);
        record_character(gdk_keymap, e, (state | GDK_SHIFT_MASK), key);
        // AltGr and Shift+AltGr
        record_character(gdk_keymap, e, (state | GDK_MOD5_MASK), key);
        record_character(gdk_keymap, e, (state | GDK_MOD5_MASK | GDK_SHIFT_MASK), key);
    }

    return key;
}

gint find_gdk_keyval_for_glass_keycode(jint code) {
    gint result = -1;
    init_keymap();

    auto i = robot_java_to_keyval.find(code);
    if (i != robot_java_to_keyval.end()) {
        return i->second;
    }

    GHashTableIter iter;
    gpointer key, value;
    g_hash_table_iter_init(&iter, keymap);
    while (g_hash_table_iter_next(&iter, &key, &value)) {
        if (code == GPOINTER_TO_INT(value)) {
            result = GPOINTER_TO_INT(key);
            break;
        }
    }
    return result;
}

gint find_scancode_for_gdk_keyval(gint keyval) {
    init_keymap();
    auto i = keyval_to_scancode.find(keyval);
    if (i != keyval_to_scancode.end()) {
        return i->second;
    }
    return -1;
}

static bool keyval_requires_numlock(gint keyval) {
    switch (keyval) {
        case GDK_KEY_KP_Equal:
        case GDK_KEY_KP_Multiply:
        case GDK_KEY_KP_Add:
        case GDK_KEY_KP_Subtract:
        case GDK_KEY_KP_Decimal:
        case GDK_KEY_KP_Separator:
        case GDK_KEY_KP_Divide:
        case GDK_KEY_KP_0:
        case GDK_KEY_KP_1:
        case GDK_KEY_KP_2:
        case GDK_KEY_KP_3:
        case GDK_KEY_KP_4:
        case GDK_KEY_KP_5:
        case GDK_KEY_KP_6:
        case GDK_KEY_KP_7:
        case GDK_KEY_KP_8:
        case GDK_KEY_KP_9:
            return true;
        default:
            return false;
    }
}

// This routine is given a set of GdkKeymap entries which can generate a specific keyval
// and finds the entry that generates that keyval on the correct layout (group) at shift
// level 0.
static gint search_keys(GdkKeymap *keymap, GdkKeymapKey *keys, gint n_keys, guint search_keyval, int search_group, bool requires_num_lock) {
    gint result = -1;

    GdkModifierType state = (GdkModifierType)0;
    if (requires_num_lock) {
        state = GDK_MOD2_MASK;
    }
    for (gint i = 0; i < n_keys; ++i)
    {
        guint keyval = 0;
        if (gdk_keymap_translate_keyboard_state(keymap, keys[i].keycode, state, search_group,
                                                &keyval, nullptr, nullptr, nullptr)) {
            if (keyval == search_keyval) {
                result = keys[i].keycode;
                break;
            }
        }
    }
    return result;
}

extern "C" {
    static gint get_current_keyboard_group();
}

gint find_gdk_keycode_for_keyval(gint keyval) {
    GdkKeymapKey *keys = nullptr;
    gint n_keys = 0;
    GdkKeymap* keymap = gdk_keymap_get_for_display(gdk_display_get_default());

    // The routine get_glass_key assigns a Java KeyCode to a key event. For
    // the Robot we need to reverse that process.
    //
    // GDK assigns different keyvals to upper and lower case letters.
    // get_glass_key turns off the Shift modifier and uses the lower-case
    // letter.
    keyval = gdk_keyval_to_lower(keyval);

    // When looking for a key code on the numeric keypad we have to manually
    // apply the correct modifier.
    bool requires_num_lock = keyval_requires_numlock(keyval);

    // Retrieve all the keymap entries that can generate this keyval. This
    // includes entries on all layouts (groups) and shift levels. It is up
    // to us to find an entry that's on the current group and at shift level
    // 0 (which is what get_glass_key uses).
    if (!gdk_keymap_get_entries_for_keyval(keymap, keyval, &keys, &n_keys)) {
        return -1;
    }

    gint group = get_current_keyboard_group();
    gint result = search_keys(keymap, keys, n_keys, keyval, group, requires_num_lock);
    if (result < 0 && group != 0) {
        // Accelerators involving the characters A-Z must work even on
        // non-Latin layouts. If get_glass_key can't map to a Java key code
        // on the current layout it switches to layout 0 seeking a Latin
        // mapping. This is wrong in two ways: layout 0 might not be Latin
        // and even if it is Latin it should only be used for finding
        // KeyCodes A-Z. For compatibility this routine continues to use
        // group 0 but does impose the A-Z restriction.
        if (keyval >= GDK_KEY_a && keyval <= GDK_KEY_z) {
            result = search_keys(keymap, keys, n_keys, keyval, 0, requires_num_lock);
        }
    }

    g_free(keys);
    return result;
}

jint gdk_modifier_mask_to_glass(guint mask)
{
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
            return 0;
    }
}
extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _getKeyCodeForChar
 * Signature: (CI)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1getKeyCodeForChar
  (JNIEnv *env, jobject jApplication, jchar character, jint hint)
{
    (void)env;
    (void)jApplication;

    gunichar *ucs_char = g_utf16_to_ucs4(&character, 1, NULL, NULL, NULL);
    if (ucs_char == NULL) {
        return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    }

    auto i = char_to_java_code.find(*ucs_char);
    if (i != char_to_java_code.end()) {
        g_free(ucs_char);
        return i->second;
    }

    // If we don't find the character in the map fall back to the old logic
    // for compatibility. It is incorrect because it ignores the keyboard
    // layout but it can handle characters like space and A-Z on Latin
    // layouts.
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
  * Determine which keyboard layout is active. This is the group
  * number in the Xkb state. There is no direct way to query this
  * in Gdk.
  */
 static gint get_current_keyboard_group() {
     Display* display = gdk_x11_display_get_xdisplay(gdk_display_get_default());
     if (isXkbAvailable(display)) {
         XkbStateRec xkbState;
         XkbGetState(display, XkbUseCoreKbd, &xkbState);
         return xkbState.group;
     }
     return -1;
 }

/*
 * Class:     com_sun_glass_ui_gtk_GtkApplication
 * Method:    _isKeyLocked
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkApplication__1isKeyLocked
  (JNIEnv * env, jobject obj, jint keyCode)
{
    Display* display = gdk_x11_display_get_xdisplay(gdk_display_get_default());
    if (!isXkbAvailable(display)) {
        return com_sun_glass_events_KeyEvent_KEY_LOCK_UNKNOWN;
    }

    Atom keyCodeAtom = None;
    switch (keyCode) {
        case com_sun_glass_events_KeyEvent_VK_CAPS_LOCK:
            keyCodeAtom = XInternAtom(display, "Caps Lock", True);
            break;

        case com_sun_glass_events_KeyEvent_VK_NUM_LOCK:
            keyCodeAtom = XInternAtom(display, "Num Lock", True);
            break;
    }

    if (keyCodeAtom == None) {
        return com_sun_glass_events_KeyEvent_KEY_LOCK_UNKNOWN;
    }

    Bool isLocked = False;
    if (XkbGetNamedIndicator(display, keyCodeAtom, NULL, &isLocked, NULL, NULL)) {
        if (isLocked) {
            return com_sun_glass_events_KeyEvent_KEY_LOCK_ON;
        } else {
            return com_sun_glass_events_KeyEvent_KEY_LOCK_OFF;
        }
    }

    return com_sun_glass_events_KeyEvent_KEY_LOCK_UNKNOWN;
}

} // extern "C"
