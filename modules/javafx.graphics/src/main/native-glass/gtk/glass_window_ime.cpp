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

#include <com_sun_glass_events_KeyEvent.h>

#include "com_sun_glass_ui_View.h"
#include "glass_window.h"
#include "glass_general.h"
#include "glass_key.h"

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

    jstring jstr = mainEnv->NewStringUTF(preedit_text);
    EXCEPTION_OCCURED(mainEnv);

    jsize slen = mainEnv->GetStringLength(jstr);

    PangoAttrIterator *iter = pango_attr_list_get_iterator(attrList);
    PangoAttribute *pangoAttr;

    jbyte attr = com_sun_glass_ui_View_IME_ATTR_INPUT;
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
    g_free(preedit_text);

    mainEnv->CallVoidMethod(ctx->get_jview(),
            jViewNotifyInputMethodLinux,
            jstr,
            0,
            cursor_pos,
            attr);
    LOG_EXCEPTION(mainEnv)
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
    if (im_ctx.on_preedit || !im_ctx.on_key_event) {
        jstring jstr = mainEnv->NewStringUTF(str);
        EXCEPTION_OCCURED(mainEnv);
        jsize slen = mainEnv->GetStringLength(jstr);

        mainEnv->CallVoidMethod(jview,
                jViewNotifyInputMethodLinux,
                jstr,
                slen,
                slen,
                0);
        LOG_EXCEPTION(mainEnv)
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
    bool filtered = gtk_im_context_filter_keypress(im_ctx.ctx, &event->key);

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
    double *nativePos;

    jdoubleArray pos = (jdoubleArray)mainEnv->CallObjectMethod(get_jview(),
                                      jViewNotifyInputMethodCandidateRelativePosRequest,
                                      0);

    nativePos = mainEnv->GetDoubleArrayElements(pos, NULL);

    GdkRectangle rect;
    if (nativePos) {
        rect.x = (int) nativePos[0];
        rect.y = (int) nativePos[1];
        rect.width = 0;
        rect.height = 0;

        mainEnv->ReleaseDoubleArrayElements(pos, nativePos, 0);
        gtk_im_context_set_cursor_location(im_ctx.ctx, &rect);
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
}

void WindowContextBase::disableIME() {
    if (im_ctx.ctx != NULL) {
        g_object_unref(im_ctx.ctx);
        im_ctx.ctx = NULL;
    }

    im_ctx.enabled = false;
}
