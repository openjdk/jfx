/*
 * Copyright (C) 2018 Endless Mobile, Inc.
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 *
 * Author: Philip Withnall <withnall@endlessm.com>
 */

#ifndef __G_UTILS_PRIVATE_H__
#define __G_UTILS_PRIVATE_H__

#include "glibconfig.h"
#include "gtypes.h"
#include "gtestutils.h"

#include <math.h>
#include <time.h>

G_BEGIN_DECLS

void g_set_user_dirs (const gchar *first_dir_type,
                      ...) G_GNUC_NULL_TERMINATED;

/* Returns the smallest power of 2 greater than or equal to n,
 * or 0 if such power does not fit in a gsize
 */
static inline gsize
g_nearest_pow (gsize num)
{
  gsize n = num - 1;

  g_assert (num > 0 && num <= G_MAXSIZE / 2);

  n |= n >> 1;
  n |= n >> 2;
  n |= n >> 4;
  n |= n >> 8;
  n |= n >> 16;
#if GLIB_SIZEOF_SIZE_T == 8
  n |= n >> 32;
#endif

  return n + 1;
}

void _g_unset_cached_tmp_dir (void);

gboolean _g_localtime (time_t timet, struct tm *tm);

gboolean g_set_prgname_once (const gchar *prgname);

/* Although isnan() is defined as a type-independent macro in C99, mingw32
 * doesn’t seem to support that (it defines `isnan (float d)` only). Older
 * MSVC toolchains don’t support C99 either. So we provide an internal
 * abstraction macro.
 *
 * This should not be made public; toolchains will soon enough catch up with
 * C99, so third party code should just use isnan(). */
static inline int
g_isnan (double d)
{
#if (defined (_MSC_VER) && (_MSC_VER < 1800)) || defined(__MINGW32__)
  return _isnan (d);
#else
  return isnan (d);
#endif
}

G_END_DECLS

#endif /* __G_UTILS_PRIVATE_H__ */
