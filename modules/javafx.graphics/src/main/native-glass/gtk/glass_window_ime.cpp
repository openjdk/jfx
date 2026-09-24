/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <com_sun_glass_events_KeyEvent.h>

#include "com_sun_glass_ui_View.h"
#include "glass_window.h"
#include "glass_general.h"
#include "glass_key.h"

#include <cstring>

static void on_preedit_start(GtkIMContext *im_context, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->setOnPreEdit(true);
}

static void on_preedit_changed(GtkIMContext *im_context, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    gchar *preedit_text;
    PangoAttrList* attrList;
    int cursor_pos;

    gtk_im_context_get_preedit_string(im_context, &preedit_text, &attrList, &cursor_pos);
    ctx->updateCaretPos();
    ctx->setOnPreEdit(true);

    // preedit_text itself is handed to the slot (the bytes the JNI of commit 033187ad90 passed to NewStringUTF
    // here), so it is freed only after that call.
    auto notify_preedit = glass_view_cb.notify_input_method_preedit;

    PangoAttrIterator *iter = pango_attr_list_get_iterator(attrList);
    PangoAttribute *pangoAttr;

    int8_t attr = com_sun_glass_ui_View_IME_ATTR_INPUT;
    do {
        if (pangoAttr = pango_attr_iterator_get(iter, PANGO_ATTR_BACKGROUND)) {
             attr = com_sun_glass_ui_View_IME_ATTR_TARGET_NOTCONVERTED;
             break;
        } else if ((pangoAttr = pango_attr_iterator_get(iter, PANGO_ATTR_UNDERLINE))
                && (((PangoAttrInt *)pangoAttr)->value == PANGO_UNDERLINE_SINGLE)) {
            attr = com_sun_glass_ui_View_IME_ATTR_CONVERTED;
            break;
        }
    } while (pango_attr_iterator_next(iter));

    pango_attr_list_unref(attrList);
    pango_attr_iterator_destroy(iter);

    if (notify_preedit) {
        // LOG_EXCEPTION site: the status is ignored. jview is not checked here: without a view the id is 0
        // (see glass_gtk_api.h, IDENTITY).
        notify_preedit(ctx->get_view_id(), preedit_text,
                (preedit_text != NULL) ? (int32_t) strlen(preedit_text) : 0, cursor_pos, attr);
    }
    g_free(preedit_text);
}

static void on_preedit_end(GtkIMContext *im_context, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->setOnPreEdit(false);
}

static void on_commit(GtkIMContext *im_context, gchar* str, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->commitIME(str);
}

// Note: JavaFX did not have surround support at this time
static gboolean on_delete_surrounding(GtkIMContext* self, gint offset, gint n_chars, gpointer user_data) {
    return TRUE;
}

static gboolean on_retrieve_surrounding(GtkIMContext* self, gpointer user_data) {
    return TRUE;
}

void WindowContextBase::commitIME(gchar *str) {
    if (im_ctx.in_preedit_window || !im_ctx.on_key_event) {
        if (glass_view_cb.notify_input_method_commit) {
            // LOG_EXCEPTION site: the status is ignored. Java derives both lengths from the decoded string,
            // as GetStringLength did. jview is not checked here: without a view view_id is 0.
            glass_view_cb.notify_input_method_commit(view_id, str,
                    (str != NULL) ? (int32_t) strlen(str) : 0);
        }
    } else {
        im_ctx.send_keypress = true;
    }
}

bool WindowContextBase::hasIME() {
    return im_ctx.enabled;
}

bool WindowContextBase::filterIME(GdkEvent *event) {
    if (!hasIME()) {
        return false;
    }

    im_ctx.on_key_event = true;
    // If "commit" arrives while pre-editing is active we send out an
    // InputMethodEvent; otherwise we send out a KeyEvent. Normally the
    // "commit" signal arrives before "preedit-end" but sometimes when
    // processing dead keys on a system with no input method
    // framework these signals arrive in reverse order. This flag
    // ensures that if we're in the pre-edit window when we start
    // filtering the event we stay there until we're done.
    im_ctx.in_preedit_window = im_ctx.on_preedit;
    bool filtered = gtk_im_context_filter_keypress(im_ctx.ctx, &event->key);
    im_ctx.in_preedit_window = im_ctx.on_preedit;

    if (filtered && im_ctx.send_keypress) {
        im_ctx.send_keypress = false;
        return false;
    }

    im_ctx.on_key_event = false;
    return filtered;
}

void WindowContextBase::setOnPreEdit(bool preedit) {
    im_ctx.on_preedit = preedit;
}

void WindowContextBase::updateCaretPos() {
    if (glass_view_cb.notify_input_method_candidate_pos_request) {
        // Not checked in the JNI, whose GetDoubleArrayElements then crashed on a throw or a null array: the
        // status is ignored and without a position the cursor location is not set. jview is not checked
        // here: without a view view_id is 0 (see glass_gtk_api.h, IDENTITY).
        double xy[2] = { 0, 0 };
        int32_t valid = 0;
        glass_view_cb.notify_input_method_candidate_pos_request(view_id, 0, xy, &valid);

        GdkRectangle rect;
        if (valid) {
            rect.x = (int) xy[0];
            rect.y = (int) xy[1];
            rect.width = 0;
            rect.height = 0;

            gtk_im_context_set_cursor_location(im_ctx.ctx, &rect);
        }
    }
}

void WindowContextBase::enableOrResetIME() {
    if (im_ctx.on_preedit) {
        gtk_im_context_focus_out(im_ctx.ctx);
    }

    if (!im_ctx.enabled) {
        im_ctx.ctx = gtk_im_multicontext_new();
        gtk_im_context_set_client_window(GTK_IM_CONTEXT(im_ctx.ctx), gdk_window);
        gtk_im_context_set_use_preedit(GTK_IM_CONTEXT(im_ctx.ctx), true);
        g_signal_connect(im_ctx.ctx, "preedit-start", G_CALLBACK(on_preedit_start), this);
        g_signal_connect(im_ctx.ctx, "preedit-changed", G_CALLBACK(on_preedit_changed), this);
        g_signal_connect(im_ctx.ctx, "preedit-end", G_CALLBACK(on_preedit_end), this);
        g_signal_connect(im_ctx.ctx, "commit", G_CALLBACK(on_commit), this);
        g_signal_connect(im_ctx.ctx, "retrieve-surrounding", G_CALLBACK(on_retrieve_surrounding), this);
        g_signal_connect(im_ctx.ctx, "delete-surrounding", G_CALLBACK(on_delete_surrounding), this);
    }

    gtk_im_context_reset(im_ctx.ctx);
    gtk_im_context_focus_in(im_ctx.ctx);

    im_ctx.on_preedit = false;
    im_ctx.enabled = true;
    im_ctx.on_key_event = false;
    im_ctx.in_preedit_window = false;
}

void WindowContextBase::disableIME() {
    if (im_ctx.ctx != NULL) {
        g_object_unref(im_ctx.ctx);
        im_ctx.ctx = NULL;
    }

    im_ctx.enabled = false;
}
