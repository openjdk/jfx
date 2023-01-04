/*
 * glib-compat.c
 * Functions copied from glib 2.10
 *
 * Copyright 2005 David Schleef <ds@schleef.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#ifndef __GLIB_COMPAT_PRIVATE_H__
#define __GLIB_COMPAT_PRIVATE_H__

#include <glib.h>

G_BEGIN_DECLS

#ifdef GSTREAMER_LITE
#ifdef LINUX
// Redefine GLIB_CHECK_VERSION.
// On Linux we using GLIB_VERSION_MIN_REQUIRED=GLIB_VERSION_2_48 and
// GLIB_VERSION_MAX_ALLOWED=GLIB_VERSION_2_48, so we can build and run with
// glib starting with 2.48. These defines has no effect on GLIB_CHECK_VERSION
// and this macro is evaluated based on GLib version we using during build, so
// we need to change it to pretend that we building with 2.48
#undef GLIB_CHECK_VERSION
#define GLIB_CHECK_VERSION(major,minor,micro)    \
    (2 > (major) || \
     (2 == (major) && 48 > (minor)) || \
     (2 == (major) && 48 == (minor) && \
      0 >= (micro)))
#endif // LINUX
#endif // GSTREAMER_LITE

#ifdef GSTREAMER_LITE
#if !defined(g_abort)
#include <stdlib.h>
#define g_abort() abort()
#endif // g_abort
#endif // GSTREAMER_LITE

/* copies */

/* adaptations */
#if !GLIB_CHECK_VERSION(2, 67, 4)
#define g_memdup2(ptr,sz) ((G_LIKELY(((guint64)(sz)) < G_MAXUINT)) ? g_memdup(ptr,sz) : (g_abort(),NULL))
#endif

G_END_DECLS

#endif
