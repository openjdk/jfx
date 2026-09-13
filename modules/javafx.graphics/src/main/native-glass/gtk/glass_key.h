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
#ifndef GLASS_KEY_H
#define        GLASS_KEY_H

#include <jni.h>

#include <gtk/gtk.h>

#ifdef __cplusplus
extern "C" {
#endif
jint gdk_keyval_to_glass(guint keyval);
jint get_glass_key(GdkEventKey* e);
jint glass_key_to_modifier(jint glassKey);
jint gdk_modifier_mask_to_glass(guint mask);
gint find_gdk_keyval_for_glass_keycode(jint code);
gint find_gdk_keycode_for_keyval(gint keyval);
gint find_scancode_for_gdk_keyval(gint keyval);
#ifdef __cplusplus
}
#endif
#endif        /* GLASS_KEY_H */
