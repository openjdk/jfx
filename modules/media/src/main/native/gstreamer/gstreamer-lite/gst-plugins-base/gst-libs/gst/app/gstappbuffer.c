/* GStreamer
 * Copyright (C) 2007 David Schleef <ds@schleef.org>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/base/gstpushsrc.h>

#include <string.h>

#include "gstappbuffer.h"

static void gst_app_buffer_init (GstAppBuffer * buffer, gpointer g_class);
static void gst_app_buffer_class_init (gpointer g_class, gpointer class_data);
static void gst_app_buffer_finalize (GstAppBuffer * buffer);

static GstBufferClass *parent_class;

GType
gst_app_buffer_get_type (void)
{
  static volatile gsize app_buffer_type = 0;

  if (g_once_init_enter (&app_buffer_type)) {
    static const GTypeInfo app_buffer_info = {
      sizeof (GstBufferClass),
      NULL,
      NULL,
      gst_app_buffer_class_init,
      NULL,
      NULL,
      sizeof (GstAppBuffer),
      0,
      (GInstanceInitFunc) gst_app_buffer_init,
      NULL
    };
    GType tmp = g_type_register_static (GST_TYPE_BUFFER, "GstAppBuffer",
        &app_buffer_info, 0);
    g_once_init_leave (&app_buffer_type, tmp);
  }

  return (GType) app_buffer_type;
}

static void
gst_app_buffer_init (GstAppBuffer * buffer, gpointer g_class)
{

}

static void
gst_app_buffer_class_init (gpointer g_class, gpointer class_data)
{
  GstMiniObjectClass *mini_object_class = GST_MINI_OBJECT_CLASS (g_class);

  mini_object_class->finalize =
      (GstMiniObjectFinalizeFunction) gst_app_buffer_finalize;

  parent_class = g_type_class_peek_parent (g_class);
}

static void
gst_app_buffer_finalize (GstAppBuffer * buffer)
{
  g_return_if_fail (buffer != NULL);
  g_return_if_fail (GST_IS_APP_BUFFER (buffer));

  if (buffer->finalize) {
    buffer->finalize (buffer->priv);
  }

  GST_MINI_OBJECT_CLASS (parent_class)->finalize (GST_MINI_OBJECT (buffer));
}

GstBuffer *
gst_app_buffer_new (void *data, int length,
    GstAppBufferFinalizeFunc finalize, void *priv)
{
  GstAppBuffer *buffer;

  buffer = (GstAppBuffer *) gst_mini_object_new (GST_TYPE_APP_BUFFER);

  GST_BUFFER_DATA (buffer) = data;
  GST_BUFFER_SIZE (buffer) = length;

  buffer->finalize = finalize;
  buffer->priv = priv;

  return GST_BUFFER (buffer);
}
