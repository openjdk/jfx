/* GStreamer
 * Copyright (C) <2021> Jan Schmidt <jan@centricular.com>
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
#include <gst/gst.h>
#include "qtdemux.h"

#ifndef __QTDEMUX_WEBVTT_H__
#define __QTDEMUX_WEBVTT_H__

G_BEGIN_DECLS

gboolean qtdemux_webvtt_is_empty(GstQTDemux *demux, guint8 *data, gsize size);
GstBuffer *qtdemux_webvtt_decode (GstQTDemux * qtdemux, GstClockTime start, GstClockTime duration, guint8 *data, gsize size);

G_END_DECLS

#endif
