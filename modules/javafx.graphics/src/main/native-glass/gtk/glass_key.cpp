/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

static gboolean key_initialized = FALSE;
static GHashTable *keymap;

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
}

static void init_keymap() {
    if (!key_initialized) {
        initialize_key();
        key_initialized = TRUE;
    }
}

jint gdk_keyval_to_glass(guint keyval)
{
    init_keymap();
    return GPOINTER_TO_INT(g_hash_table_lookup(keymap, GINT_TO_POINTER(keyval)));
}

jint get_glass_key(GdkEventKey* e) {
    init_keymap();

    guint keyValue;
    guint state = e->state & GDK_MOD2_MASK; //NumLock test

    gdk_keymap_translate_keyboard_state(gdk_keymap_get_default(),
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

        keyValue = gdk_keymap_lookup_key(gdk_keymap_get_default(), &kk);

        key = GPOINTER_TO_INT(g_hash_table_lookup(keymap,
                GINT_TO_POINTER(keyValue)));
    }

    return key;
}

gint find_gdk_keyval_for_glass_keycode(jint code) {
    gint result = -1;
    GHashTableIter iter;
    gpointer key, value;
    init_keymap();
    g_hash_table_iter_init(&iter, keymap);
    while (g_hash_table_iter_next(&iter, &key, &value)) {
        if (code == GPOINTER_TO_INT(value)) {
            result = GPOINTER_TO_INT(key);
            break;
        }
    }
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
