/* GStreamer
 * Copyright (C) 2025 Netflix Inc.
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

#pragma once

#include <gst/gstconfig.h>
#include <gst/gstplugin.h>
#include <gst/gstmeta.h>

G_BEGIN_DECLS

/**
 * GstMetaFactory:
 *
 * The opaque #GstMetaFactory data structure.
 *
 * Since: 1.28
 */
typedef struct _GstMetaFactoryClass GstMetaFactoryClass;
typedef struct _GstMetaFactory GstMetaFactory;

#define GST_TYPE_META_FACTORY    (gst_meta_factory_get_type ())
#define GST_IS_META_FACTORY(obj) (G_TYPE_CHECK_INSTANCE_TYPE((obj), GST_TYPE_META_FACTORY))
#define GST_META_FACTORY(obj)    (G_TYPE_CHECK_INSTANCE_CAST((obj), GST_TYPE_META_FACTORY, GstMetaFactory))

GST_API
GType gst_meta_factory_get_type (void);

GST_API
const GstMetaInfo * gst_meta_factory_load (const gchar *factoryname);

GST_API
gboolean gst_meta_factory_register (GstPlugin *plugin, const GstMetaInfo *meta_info);

G_END_DECLS
