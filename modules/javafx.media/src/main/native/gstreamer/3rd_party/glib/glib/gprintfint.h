/* GLIB - Library of useful routines for C programming
 * Copyright (C) 1995-1997  Peter Mattis, Spencer Kimball and Josh MacDonald
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Modified by the GLib Team and others 2002.  See the AUTHORS
 * file for a list of people on the GLib Team.  See the ChangeLog
 * files for a list of changes.  These files are distributed with
 * GLib at ftp://ftp.gtk.org/pub/gtk/.
 */

#ifndef __G_PRINTFINT_H__
#define __G_PRINTFINT_H__

#ifdef USE_SYSTEM_PRINTF

#define _g_printf    printf
#define _g_fprintf   fprintf
#define _g_sprintf   sprintf
#define _g_snprintf  snprintf

#define _g_vprintf   vprintf
#define _g_vfprintf  vfprintf
#define _g_vsprintf  vsprintf
#define _g_vsnprintf vsnprintf

#else

#include "gnulib/printf.h"

#define _g_printf    _g_gnulib_printf
#define _g_fprintf   _g_gnulib_fprintf
#define _g_sprintf   _g_gnulib_sprintf
#define _g_snprintf  _g_gnulib_snprintf

#define _g_vprintf   _g_gnulib_vprintf
#define _g_vfprintf  _g_gnulib_vfprintf
#define _g_vsprintf  _g_gnulib_vsprintf
#define _g_vsnprintf _g_gnulib_vsnprintf

#endif

#endif /* __G_PRINTF_H__ */

