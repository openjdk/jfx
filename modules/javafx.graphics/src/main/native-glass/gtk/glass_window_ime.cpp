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

#include <com_sun_glass_events_KeyEvent.h>

#include "com_sun_glass_ui_View.h"
#include "glass_window.h"
#include "glass_general.h"
#include "glass_key.h"

static void on_preedit_start(GtkIMContext *im_context, gpointer user_data) {
    g_print("preedit_start\n");
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

    jstring jstr = mainEnv->NewStringUTF(preedit_text);
    EXCEPTION_OCCURED(mainEnv);

    jsize slen = mainEnv->GetStringLength(jstr);

    g_print("CursorPos: %d\n", cursor_pos);

    PangoAttrIterator* iter = pango_attr_list_get_iterator(attrList);
    PangoAttribute  *attr;

    int boundaryCount = 0;
    do {
        int start, end;
        pango_attr_iterator_range(iter, &start, &end);
        g_print("attr_range: %d, %d\n", start, end);

        if (attr = pango_attr_iterator_get(iter, PANGO_ATTR_BACKGROUND)) {
            const PangoColor *color = &((PangoAttrColor *)attr)->color;

            g_print("attr_color: %d, %d, %d\n", color->red, color->green, color->blue);

            if ((color->red | color->green | color->blue) == 0) {
//                v[i] = com_sun_glass_ui_View_IME_ATTR_TARGET_NOTCONVERTED;
            } else {
//                v[i] = com_sun_glass_ui_View_IME_ATTR_TARGET_CONVERTED;
            }
        } else if ((attr = pango_attr_iterator_get(iter, PANGO_ATTR_UNDERLINE))
                && (((PangoAttrInt *)attr)->value != PANGO_UNDERLINE_NONE)) {
            g_print("underline\n");
//            v[i] = com_sun_glass_ui_View_IME_ATTR_CONVERTED;
        } else {
//            v[i] = com_sun_glass_ui_View_IME_ATTR_INPUT;
        }

        boundaryCount++;
    } while (pango_attr_iterator_next(iter));

    pango_attr_iterator_destroy (iter);
    g_free(preedit_text);

//    boundary = mainEnv->newIntArray(slen);
//    CHECK_JNI_EXCEPTION(mainEnv)
//
//    attr = mainEnv->NewByteArray(slen);
//    CHECK_JNI_EXCEPTION(mainEnv)
//
//    mainEnv->SetByteArrayRegion(attr, 0, slen, v);
//    CHECK_JNI_EXCEPTION(mainEnv)

    mainEnv->CallVoidMethod(ctx->get_jview(),
            jViewNotifyInputMethod,
            jstr,
            NULL,
            NULL,
            NULL,
            0,
            cursor_pos,
            0);
    LOG_EXCEPTION(mainEnv)
}

static void on_preedit_end(GtkIMContext *im_context, gpointer user_data) {
    g_print("on_preedit_end\n");

    WindowContext *ctx = (WindowContext *) user_data;
    ctx->setOnPreEdit(false);
}

static void on_commit(GtkIMContext *im_context, gchar* str, gpointer user_data) {
    g_print("on_commit\n");
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->commitIME(str);
}

void WindowContextBase::commitIME(gchar *str) {
    if (im_ctx.on_preedit) {
        jstring jstr = mainEnv->NewStringUTF(str);
        EXCEPTION_OCCURED(mainEnv);
        jsize slen = mainEnv->GetStringLength(jstr);

        mainEnv->CallVoidMethod(jview,
                jViewNotifyInputMethod,
                jstr,
                NULL,
                NULL,
                NULL,
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

    g_print("will filter?\n");
    bool filtered = gtk_im_context_filter_keypress(im_ctx.ctx, &event->key);

    if (im_ctx.send_keypress) {
        process_key(&event->key);
        im_ctx.send_keypress = false;
    }

    g_print("filterIME -> keyval: %d, press: %d, filtered: %d\n", event->key.keyval, (event->key.type == GDK_KEY_PRESS), filtered);

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
    if (!im_ctx.enabled) {
        im_ctx.ctx = gtk_im_multicontext_new();
        gtk_im_context_set_client_window(GTK_IM_CONTEXT(im_ctx.ctx), gdk_window);
        g_signal_connect(im_ctx.ctx, "preedit-start", G_CALLBACK(on_preedit_start), this);
        g_signal_connect(im_ctx.ctx, "preedit-changed", G_CALLBACK(on_preedit_changed), this);
        g_signal_connect(im_ctx.ctx, "preedit-end", G_CALLBACK(on_preedit_end), this);
        g_signal_connect(im_ctx.ctx, "commit", G_CALLBACK(on_commit), this);
    }

    gtk_im_context_reset(im_ctx.ctx);
    gtk_im_context_focus_in(im_ctx.ctx);

    im_ctx.on_preedit = false;
    im_ctx.enabled = true;
}

void WindowContextBase::disableIME() {
    if (im_ctx.ctx != NULL) {
        g_signal_handlers_disconnect_matched(im_ctx.ctx, G_SIGNAL_MATCH_DATA, 0, 0, NULL, NULL, NULL);
        g_object_unref(im_ctx.ctx);
    }

    im_ctx.enabled = false;
}
