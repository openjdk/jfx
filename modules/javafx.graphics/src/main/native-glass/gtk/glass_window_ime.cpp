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

void on_preedit_start(GtkIMContext *im_context, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->setOnPreEdit(true);
    g_print("on_preedit_start\n");
}

void on_preedit_changed(GtkIMContext *im_context, gpointer user_data) {
    gchar *preedit_text;
    WindowContext *ctx = (WindowContext *) user_data;

    int cursor_pos;
    gtk_im_context_get_preedit_string(im_context, &preedit_text, NULL, &cursor_pos);
    ctx->updateCaretPos();

    g_print("on_preedit_changed: %s - %d\n", preedit_text, cursor_pos);

    jstring jstr = mainEnv->NewStringUTF(preedit_text);
    EXCEPTION_OCCURED(mainEnv);
    g_free(preedit_text);

    jsize slen = mainEnv->GetStringLength(jstr);

    mainEnv->CallVoidMethod(ctx->get_jview(),
            jViewNotifyInputMethodLinux,
            jstr,
            0,
            slen,
            cursor_pos,
            0,
            0);
    LOG_EXCEPTION(mainEnv)
}

void on_preedit_end(GtkIMContext *im_context, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->setOnPreEdit(false);
    g_print("on_preedit_end\n");
}

void on_commit(GtkIMContext *im_context, gchar* str, gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;
    ctx->commitIME(str);
}

void WindowContextBase::commitIME(gchar *str) {
    g_print("commitIME: %s\n", str);
    jstring jstr = mainEnv->NewStringUTF(str);
    EXCEPTION_OCCURED(mainEnv);
    jsize slen = mainEnv->GetStringLength(jstr);

    if (!im_ctx.on_preedit) {
        g_print("not on preedit: %s\n", str);

        //FIXME: could be more?
        if (slen == 1) {
            jcharArray jChars = mainEnv->NewCharArray(1);
            mainEnv->SetCharArrayRegion(jChars, 0, 1, (const jchar*) str);
            CHECK_JNI_EXCEPTION(mainEnv)

            mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                    com_sun_glass_events_KeyEvent_PRESS,
                    gdk_keyval_to_glass(gdk_unicode_to_keyval(str[0])),
                    jChars,
                    0);
            CHECK_JNI_EXCEPTION(mainEnv)

            mainEnv->CallVoidMethod(jview, jViewNotifyKey,
                    com_sun_glass_events_KeyEvent_TYPED,
                    com_sun_glass_events_KeyEvent_VK_UNDEFINED,
                    jChars,
                    0);
            CHECK_JNI_EXCEPTION(mainEnv)
        }
    } else {
        g_print("jViewNotifyInputMethodLinux: %s\n", str);
        mainEnv->CallVoidMethod(jview,
                jViewNotifyInputMethodLinux,
                jstr,
                4,
                slen,
                slen,
                0,
                slen);
        LOG_EXCEPTION(mainEnv)
    }
}

bool WindowContextBase::hasIME() {
    return im_ctx.enabled;
}

bool WindowContextBase::filterIME(GdkEvent *event) {
    if (!hasIME()) {
        return false;
    }

    return gtk_im_context_filter_keypress(im_ctx.ctx, &event->key);
}

void WindowContextBase::setOnPreEdit(bool preedit) {
    im_ctx.on_preedit = preedit;
}

void WindowContextBase::updateCaretPos() {
    double *nativePos;

    jdoubleArray pos = (jdoubleArray)mainEnv->CallObjectMethod(get_jview(),
                                      jViewNotifyInputMethodCandidatePosRequest,
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
