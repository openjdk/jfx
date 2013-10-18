/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) <2003> David Schleef <ds@schleef.org>
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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

 /**
 * SECTION:gstvideofilter
 * @short_description: Base class for video filters
 * 
 * <refsect2>
 * <para>
 * Provides useful functions and a base class for video filters.
 * </para>
 * <para>
 * The videofilter will by default enable QoS on the parent GstBaseTransform
 * to implement frame dropping.
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstvideofilter.h"

#include <gst/video/video.h>

GST_DEBUG_CATEGORY_STATIC (gst_video_filter_debug);
#define GST_CAT_DEFAULT gst_video_filter_debug

static void gst_video_filter_class_init (gpointer g_class, gpointer class_data);
static void gst_video_filter_init (GTypeInstance * instance, gpointer g_class);

static GstBaseTransformClass *parent_class = NULL;

GType
gst_video_filter_get_type (void)
{
  static GType video_filter_type = 0;

  if (!video_filter_type) {
    static const GTypeInfo video_filter_info = {
      sizeof (GstVideoFilterClass),
      NULL,
      NULL,
      gst_video_filter_class_init,
      NULL,
      NULL,
      sizeof (GstVideoFilter),
      0,
      gst_video_filter_init,
    };

    video_filter_type = g_type_register_static (GST_TYPE_BASE_TRANSFORM,
        "GstVideoFilter", &video_filter_info, G_TYPE_FLAG_ABSTRACT);
  }
  return video_filter_type;
}

static gboolean
gst_video_filter_get_unit_size (GstBaseTransform * btrans, GstCaps * caps,
    guint * size)
{
  GstVideoFormat fmt;
  gint width, height;

  if (!gst_video_format_parse_caps (caps, &fmt, &width, &height)) {
    GST_WARNING_OBJECT (btrans, "Failed to parse caps %" GST_PTR_FORMAT, caps);
    return FALSE;
  }

  *size = gst_video_format_get_size (fmt, width, height);

  GST_DEBUG_OBJECT (btrans, "Returning size %u bytes for caps %"
      GST_PTR_FORMAT, *size, caps);

  return TRUE;
}

static void
gst_video_filter_class_init (gpointer g_class, gpointer class_data)
{
  GstBaseTransformClass *trans_class;
  GstVideoFilterClass *klass;

  klass = (GstVideoFilterClass *) g_class;
  trans_class = (GstBaseTransformClass *) klass;

  trans_class->get_unit_size =
      GST_DEBUG_FUNCPTR (gst_video_filter_get_unit_size);

  parent_class = g_type_class_peek_parent (klass);

  GST_DEBUG_CATEGORY_INIT (gst_video_filter_debug, "videofilter", 0,
      "videofilter");
}

static void
gst_video_filter_init (GTypeInstance * instance, gpointer g_class)
{
  GstVideoFilter *videofilter = GST_VIDEO_FILTER (instance);

  GST_DEBUG_OBJECT (videofilter, "gst_video_filter_init");

  videofilter->inited = FALSE;
  /* enable QoS */
  gst_base_transform_set_qos_enabled (GST_BASE_TRANSFORM (videofilter), TRUE);
}
