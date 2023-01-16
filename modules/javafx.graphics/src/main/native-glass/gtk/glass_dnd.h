/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef GLASS_DND_H
#define        GLASS_DND_H

#include "glass_general.h"
#include "glass_window.h"
#include <jni.h>

#include <gtk/gtk.h>

void process_dnd_target(WindowContext *, GdkEventDND *);
jint dnd_target_get_supported_actions(JNIEnv *);
jobjectArray dnd_target_get_mimes(JNIEnv *);
jobject dnd_target_get_data(JNIEnv *, jstring);

jint execute_dnd(JNIEnv *, jobject, jint);

gboolean is_in_drag();

#define DRAG_IMAGE_MAX_WIDTH 320
#define DRAG_IMAGE_MAX_HEIGH 240

#define BSWAP_32(x) (((uint)(x) << 24)  | \
          (((uint)(x) << 8) & 0xff0000) | \
          (((uint)(x) >> 8) & 0xff00)   | \
          ((uint)(x)  >> 24))

class DragView {
    GdkWindow* window;
    GdkPixbuf* pixbuf;
    gint width, height;
    gboolean is_raw_image;
    gint offset_x, offset_y;
public:
    DragView(GdkPixbuf* pixbuf, gboolean is_raw_image, gint offset_x, gint offset_y);
    void expose();
    void move(gint x, gint y);
    GdkWindow * get_window();
    ~DragView();
    static gboolean get_drag_image_offset(jobject obj, int* x, int* y);
    static GdkPixbuf* get_drag_image(jobject obj, gboolean* is_raw_image, gint* width, gint* height);

private:
    DragView(DragView&);
    DragView& operator=(const DragView&);
};

struct DragSourceContext {
    GdkDragContext *dnd_ctx;
    GdkWindow *dnd_window;
    DragView *drag_view;
    GdkDragAction actions;
    jobject data;
    jint performed_action;

    ~DragSourceContext() {
        if (drag_view) {
            delete drag_view;
        }

        if (data) {
            mainEnv->DeleteGlobalRef(data);
        }
    };
};

#endif        /* GLASS_DND_H */
