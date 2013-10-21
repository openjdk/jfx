/* GStreamer
 *
 * Copyright (C) 2001-2002 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * gstv4l2src.h: BT8x8/V4L2 source element
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

#ifndef __GST_V4L2SRC_H__
#define __GST_V4L2SRC_H__

#include <gstv4l2object.h>
#include <gstv4l2bufferpool.h>

GST_DEBUG_CATEGORY_EXTERN (v4l2src_debug);

G_BEGIN_DECLS

#define GST_TYPE_V4L2SRC \
  (gst_v4l2src_get_type())
#define GST_V4L2SRC(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_V4L2SRC,GstV4l2Src))
#define GST_V4L2SRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_V4L2SRC,GstV4l2SrcClass))
#define GST_IS_V4L2SRC(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_V4L2SRC))
#define GST_IS_V4L2SRC_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_V4L2SRC))

typedef struct _GstV4l2Src GstV4l2Src;
typedef struct _GstV4l2SrcClass GstV4l2SrcClass;

typedef GstFlowReturn (*GstV4l2SrcGetFunc)(GstV4l2Src * v4l2src, GstBuffer ** buf);

/**
 * GstV4l2Src:
 *
 * Opaque object.
 */
struct _GstV4l2Src
{
  GstPushSrc pushsrc;

  /*< private >*/
  GstV4l2Object * v4l2object;

  /* pads */
  GstCaps *probed_caps;

  /* buffer handling */
  GstV4l2BufferPool *pool;

  guint32 num_buffers;
  gboolean use_mmap;
  guint32 frame_byte_size;

  /* if the buffer will be or not used from directly mmap */
  gboolean always_copy;

  int decimate;

  /* True if we want to stop */
  gboolean quit;
  gboolean is_capturing;

  guint64 offset;

  gint     fps_d, fps_n;       /* framerate if device is open */
  GstClockTime duration;       /* duration of one frame */

  GstClockTime ctrl_time;

  GstV4l2SrcGetFunc get_frame;
};

struct _GstV4l2SrcClass
{
  GstPushSrcClass parent_class;

  GList *v4l2_class_devices;
};

GType gst_v4l2src_get_type (void);

G_END_DECLS

#endif /* __GST_V4L2SRC_H__ */
