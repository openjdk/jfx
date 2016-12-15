/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <linux/fb.h>
#include <fcntl.h>
#ifndef __USE_GNU       // required for dladdr() & Dl_info
#define __USE_GNU
#endif
#include <dlfcn.h>
#include <sys/ioctl.h>

#include <jni.h>
#include <gtk/gtk.h>
#include <gdk/gdk.h>

#include "wrapped.h"

extern jboolean gtk_verbose;

/*
 * cpp and dlsym don't play nicely together. Do all dynamic loading in C
 */

// Note added in Glib 2.36 which is >= our OEL 7.0 version of 2.36
// but does not seem to be in the headers properly
static GSettingsSchemaSource * (*_g_settings_schema_source_get_default) (void);

GSettingsSchemaSource * wrapped_g_settings_schema_source_get_default (void)
{
    if(_g_settings_schema_source_get_default == NULL) {
        _g_settings_schema_source_get_default = dlsym(RTLD_DEFAULT, "g_settings_schema_source_get_default");
        if (gtk_verbose && _g_settings_schema_source_get_default) {
            fprintf(stderr, "loaded g_settings_schema_source_get_default\n"); fflush(stderr);
        }
    }

    if(_g_settings_schema_source_get_default != NULL) {
        return (*_g_settings_schema_source_get_default)();
    }

    return NULL;
}


// Note added in Glib 2.36 which is >= our OEL 7.0 version of 2.36
// but does not seem to be in the headers properly
static GSettingsSchema *
  (*_g_settings_schema_source_lookup) (GSettingsSchemaSource *source,
                                 const gchar *schema_id,
                                 gboolean recursive);

GSettingsSchema *
wrapped_g_settings_schema_source_lookup (GSettingsSchemaSource *source,
                                 const gchar *schema_id,
                                 gboolean recursive)
{
    if(_g_settings_schema_source_lookup == NULL) {
        _g_settings_schema_source_lookup = dlsym(RTLD_DEFAULT, "g_settings_schema_source_lookup");
        if (gtk_verbose && _g_settings_schema_source_lookup) {
            fprintf(stderr, "loaded g_settings_schema_source_lookup\n"); fflush(stderr);
        }
    }

    if(_g_settings_schema_source_lookup != NULL) {
        return (*_g_settings_schema_source_lookup)(source, schema_id, recursive);
    }

    return NULL;
}

// Note added in Glib 2.40 which is > our OEL 7.0 version of 2.36
static gboolean (*_g_settings_schema_has_key) (GSettingsSchema *schema, const gchar *name);

gboolean wrapped_g_settings_schema_has_key (GSettingsSchema *schema,
                           const gchar *name)
{
    if(_g_settings_schema_has_key == NULL) {
        _g_settings_schema_has_key = dlsym(RTLD_DEFAULT, "g_settings_schema_has_key");
        if (gtk_verbose && _g_settings_schema_has_key) {
            fprintf(stderr, "loaded g_settings_schema_has_key\n"); fflush(stderr);
        }
    }

    if(_g_settings_schema_has_key != NULL) {
        return (*_g_settings_schema_has_key)(schema, name);
    }

    return 0;
}

static void (*_g_settings_schema_unref) (GSettingsSchema *schema);

void wrapped_g_settings_schema_unref (GSettingsSchema *schema)
{
    if(_g_settings_schema_unref == NULL) {
        _g_settings_schema_unref = dlsym(RTLD_DEFAULT, "g_settings_schema_unref");
        if (gtk_verbose && _g_settings_schema_unref) {
            fprintf(stderr, "loaded g_settings_schema_unref\n"); fflush(stderr);
        }
    }

    if(_g_settings_schema_unref != NULL) {
        (*_g_settings_schema_unref)(schema);
    }

}

static void (*_gdk_x11_display_set_window_scale) (GdkDisplay *display, gint scale);

// Note added in libgdk 3.10 which is > our OEL 7.0 version of 3.8
void wrapped_gdk_x11_display_set_window_scale (GdkDisplay *display,
                                  gint scale)
{
#if GTK_CHECK_VERSION(3, 0, 0)
    if(_gdk_x11_display_set_window_scale == NULL) {
        _gdk_x11_display_set_window_scale = dlsym(RTLD_DEFAULT, "gdk_x11_display_set_window_scale");
        if (gtk_verbose && _gdk_x11_display_set_window_scale) {
            fprintf(stderr, "loaded gdk_x11_display_set_window_scale\n"); fflush(stderr);
        }
    }
#endif

    if(_gdk_x11_display_set_window_scale != NULL) {
        (*_gdk_x11_display_set_window_scale)(display, scale);
    }
}

