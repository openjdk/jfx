/* GStreamer
 *
 * Copyright (C) 2002 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * v4l2src.h - system calls
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

#ifndef __V4L2SRC_CALLS_H__
#define __V4L2SRC_CALLS_H__

#include "gstv4l2src.h"
#include "v4l2_calls.h"

gboolean   gst_v4l2src_get_capture       (GstV4l2Src * v4l2src);
gboolean   gst_v4l2src_set_capture       (GstV4l2Src * v4l2src,
                                          guint32 pixelformat,
                                          guint32 width, guint32 height,
                                          gboolean interlaced,
                                          guint32 fps_n, guint32 fps_d);

gboolean   gst_v4l2src_capture_init      (GstV4l2Src * v4l2src, GstCaps *caps);
gboolean   gst_v4l2src_capture_start     (GstV4l2Src * v4l2src);

GstFlowReturn gst_v4l2src_grab_frame     (GstV4l2Src * v4l2src, GstBuffer **buf);

gboolean   gst_v4l2src_capture_stop      (GstV4l2Src * v4l2src);
gboolean   gst_v4l2src_capture_deinit    (GstV4l2Src * v4l2src);


#endif /* __V4L2SRC_CALLS_H__ */
