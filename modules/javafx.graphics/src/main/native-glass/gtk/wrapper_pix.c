/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <linux/fb.h>
#include <fcntl.h>
#ifndef __USE_GNU       // required for dladdr() & Dl_info
#define __USE_GNU
#endif
#include <dlfcn.h>
#include <sys/ioctl.h>

#include <string.h>
#include <strings.h>

#include <assert.h>

#include <gdk/gdk.h>
#include <gdk/gdkx.h>

#include "glass_wrapper.h"

static GdkPixbuf *(*_gdk_pixbuf_add_alpha) (const GdkPixbuf * pixbuf,
                        gboolean substitute_color,
                        guchar r, guchar g, guchar b);
static gboolean (*_gdk_pixbuf_get_has_alpha) (const GdkPixbuf * pixbuf);
static int (*_gdk_pixbuf_get_height) (const GdkPixbuf * pixbuf);
static guchar *(*_gdk_pixbuf_get_pixels) (const GdkPixbuf * pixbuf);
static int (*_gdk_pixbuf_get_rowstride) (const GdkPixbuf * pixbuf);
static GType (*_gdk_pixbuf_get_type) (void) G_GNUC_CONST;
static int (*_gdk_pixbuf_get_width) (const GdkPixbuf * pixbuf);
static GdkPixbuf *(*_gdk_pixbuf_new_from_data) (const guchar * data,
                        GdkColorspace colorspace,
                        gboolean has_alpha,
                        int bits_per_sample,
                        int width, int height,
                        int rowstride,
                        GdkPixbufDestroyNotify
                        destroy_fn,
                        gpointer destroy_fn_data);
static GdkPixbuf *(*_gdk_pixbuf_new_from_stream) (GInputStream * stream,
                          GCancellable * cancellable,
                          GError ** error);
static GdkPixbuf *(*_gdk_pixbuf_scale_simple) (const GdkPixbuf * src,
                           int dest_width,
                           int dest_height,
                           GdkInterpType interp_type);
static gboolean (*_gdk_pixbuf_save_to_buffer) (GdkPixbuf * pixbuf,
                           gchar ** buffer,
                           gsize * buffer_size,
                           const char *type,
                           GError ** error,
                           ...) G_GNUC_NULL_TERMINATED;

/***************************************************************************/

#define PRELOAD_SYMBOL_PIX(x) \
    _##x = dlsym(libpix,#x); \
    if (!_##x) { \
        symbol_load_errors++; \
        fprintf(stderr,"failed loading %s\n",#x); \
    }

int wrapper_load_symbols_pix (int version, void *libpix)
{
    int symbol_load_errors = 0;
    (void) version; // currently not needed

    PRELOAD_SYMBOL_PIX (gdk_pixbuf_add_alpha);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_get_has_alpha);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_get_height);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_get_pixels);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_get_rowstride);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_get_type);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_get_width);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_new_from_data);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_new_from_stream);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_scale_simple);
    PRELOAD_SYMBOL_PIX (gdk_pixbuf_save_to_buffer);

    if (symbol_load_errors && wrapper_debug)
    {
      fprintf (stderr, "failed to load %d pix symbols",
           symbol_load_errors);
    }

    return symbol_load_errors;
}

#define CHECK_LOAD_SYMBOL_PIX(x) \
    { \
        if (!_##x) { \
            if (wrapper_debug) fprintf(stderr,"missing %s\n", #x); \
            assert(_##x); \
        } else { \
            if (wrapper_debug) { \
               fprintf(stderr,"using %s\n",#x); \
               fflush(stderr); \
            } \
        } \
    }

GdkPixbuf *gdk_pixbuf_add_alpha (const GdkPixbuf * pixbuf,
                 gboolean substitute_color, guchar r,
                 guchar g, guchar b)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_add_alpha);
    return (*_gdk_pixbuf_add_alpha) (pixbuf, substitute_color, r, g, b);
}

gboolean gdk_pixbuf_get_has_alpha (const GdkPixbuf * pixbuf)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_get_has_alpha);
    return (*_gdk_pixbuf_get_has_alpha) (pixbuf);
}

int gdk_pixbuf_get_height (const GdkPixbuf * pixbuf)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_get_height);
    return (*_gdk_pixbuf_get_height) (pixbuf);
}

guchar *gdk_pixbuf_get_pixels (const GdkPixbuf * pixbuf)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_get_pixels);
    return (*_gdk_pixbuf_get_pixels) (pixbuf);
}

int gdk_pixbuf_get_rowstride (const GdkPixbuf * pixbuf)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_get_rowstride);
    return (*_gdk_pixbuf_get_rowstride) (pixbuf);
}

GType gdk_pixbuf_get_type (void)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_get_type);
    return (*_gdk_pixbuf_get_type) ();
}

int gdk_pixbuf_get_width (const GdkPixbuf * pixbuf)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_get_width);
    return (*_gdk_pixbuf_get_width) (pixbuf);
}

GdkPixbuf *gdk_pixbuf_new_from_data (const guchar * data,
                     GdkColorspace colorspace,
                     gboolean has_alpha,
                     int bits_per_sample,
                     int width, int height,
                     int rowstride,
                     GdkPixbufDestroyNotify destroy_fn,
                     gpointer destroy_fn_data)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_new_from_data);
    return (*_gdk_pixbuf_new_from_data) (data, colorspace, has_alpha,
                     bits_per_sample, width, height,
                     rowstride, destroy_fn,
                     destroy_fn_data);
}

GdkPixbuf *gdk_pixbuf_new_from_stream (GInputStream * stream,
                       GCancellable * cancellable,
                       GError ** error)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_new_from_stream);
    return (*_gdk_pixbuf_new_from_stream) (stream, cancellable, error);
}

GdkPixbuf *gdk_pixbuf_scale_simple (const GdkPixbuf * src,
                    int dest_width,
                    int dest_height,
                    GdkInterpType interp_type)
{
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_scale_simple);
    return (*_gdk_pixbuf_scale_simple) (src, dest_width, dest_height,
                    interp_type);
}


//--------------------------------------------------------------------------------------

gboolean glass_gdk_pixbuf_save_to_buffer (GdkPixbuf * pixbuf,
                    gchar ** buffer,
                    gsize * buffer_size,
                    const char *type, GError ** error)
{
    // Note: wrapped because G_GNUC_NULL_TERMINATED
    CHECK_LOAD_SYMBOL_PIX (gdk_pixbuf_save_to_buffer);
    return (*_gdk_pixbuf_save_to_buffer) (
            pixbuf, buffer, buffer_size, type,
                      error, NULL);
}
