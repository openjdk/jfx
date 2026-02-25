/*
 * Copyright (C) 2007, 2008 Ryan Lortie
 * Copyright (C) 2010 Codethink Limited
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
 */

#ifndef __G_VARIANT_CORE_H__
#define __G_VARIANT_CORE_H__

#include <glib/gvarianttypeinfo.h>
#include <glib/gvariant.h>
#include <glib/gbytes.h>

#if GLIB_SIZEOF_VOID_P == 8
# define G_VARIANT_MAX_PREALLOCATED 64
#else
# define G_VARIANT_MAX_PREALLOCATED 32
#endif

/* gvariant-core.c */

GVariant *              g_variant_new_preallocated_trusted              (const GVariantType  *type,
                                                                         gconstpointer        data,
                                                                         gsize                size);
GVariant *              g_variant_new_take_bytes                        (const GVariantType  *type,
                                                                         GBytes              *bytes,
                                                                         gboolean             trusted);
GVariant *              g_variant_new_from_children                     (const GVariantType  *type,
                                                                         GVariant           **children,
                                                                         gsize                n_children,
                                                                         gboolean             trusted);

gboolean                g_variant_is_trusted                            (GVariant            *value);

GVariantTypeInfo *      g_variant_get_type_info                         (GVariant            *value);

gsize                   g_variant_get_depth                             (GVariant            *value);

GVariant *              g_variant_maybe_get_child_value                 (GVariant            *value,
                                                                         gsize                index_);

#endif /* __G_VARIANT_CORE_H__ */
