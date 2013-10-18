/* GStreamer
 *
 * Copyright (C) 2001-2002 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *               2009 Texas Instruments, Inc - http://www.ti.com/
 *
 * gstv4l2bufferpool.h V4L2 buffer pool class
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

#ifndef __GSTV4L2BUFFER_H__
#define __GSTV4L2BUFFER_H__

#include <gst/gst.h>
#include "v4l2_calls.h"

GST_DEBUG_CATEGORY_EXTERN (v4l2buffer_debug);

G_BEGIN_DECLS


GType gst_v4l2_buffer_get_type (void);
#define GST_TYPE_V4L2_BUFFER (gst_v4l2_buffer_get_type())
#define GST_IS_V4L2_BUFFER(obj) (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_V4L2_BUFFER))
#define GST_V4L2_BUFFER(obj) (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_V4L2_BUFFER, GstV4l2Buffer))

GType gst_v4l2_buffer_pool_get_type (void);
#define GST_TYPE_V4L2_BUFFER_POOL (gst_v4l2_buffer_pool_get_type())
#define GST_IS_V4L2_BUFFER_POOL(obj) (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_V4L2_BUFFER_POOL))
#define GST_V4L2_BUFFER_POOL(obj) (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_V4L2_BUFFER_POOL, GstV4l2BufferPool))



typedef struct _GstV4l2BufferPool GstV4l2BufferPool;
typedef struct _GstV4l2Buffer GstV4l2Buffer;


struct _GstV4l2BufferPool
{
  GstMiniObject parent;

  GstElement *v4l2elem;      /* the v4l2 src/sink that owns us.. maybe we should be owned by v4l2object? */
  gboolean requeuebuf;       /* if true, unusued buffers are automatically re-QBUF'd */
  enum v4l2_buf_type type;   /* V4L2_BUF_TYPE_VIDEO_CAPTURE, V4L2_BUF_TYPE_VIDEO_OUTPUT */

  GMutex *lock;
  gboolean running;          /* with lock */
  gint num_live_buffers;     /* number of buffers not with driver */
  GAsyncQueue* avail_buffers;/* pool of available buffers, not with the driver and which aren't held outside the bufferpool */
  gint video_fd;             /* a dup(2) of the v4l2object's video_fd */
  guint buffer_count;
  GstV4l2Buffer **buffers;
};

struct _GstV4l2Buffer {
  GstBuffer   buffer;

  struct v4l2_buffer vbuffer;

  /* FIXME: have GstV4l2Src* instead, as this has GstV4l2BufferPool* */
  /* FIXME: do we really want to fix this if GstV4l2Buffer/Pool is shared
   * between v4l2src and v4l2sink??
   */
  GstV4l2BufferPool *pool;
};

void gst_v4l2_buffer_pool_destroy (GstV4l2BufferPool * pool);
GstV4l2BufferPool *gst_v4l2_buffer_pool_new (GstElement *v4l2elem, gint fd, gint num_buffers, GstCaps * caps, gboolean requeuebuf, enum v4l2_buf_type type);


GstV4l2Buffer *gst_v4l2_buffer_pool_get (GstV4l2BufferPool *pool, gboolean blocking);
gboolean gst_v4l2_buffer_pool_qbuf (GstV4l2BufferPool *pool, GstV4l2Buffer *buf);
GstV4l2Buffer *gst_v4l2_buffer_pool_dqbuf (GstV4l2BufferPool *pool);

gint gst_v4l2_buffer_pool_available_buffers (GstV4l2BufferPool *pool);


#define GST_V4L2_BUFFER_POOL_LOCK(pool)     g_mutex_lock ((pool)->lock)
#define GST_V4L2_BUFFER_POOL_UNLOCK(pool)   g_mutex_unlock ((pool)->lock)

G_END_DECLS

#endif /* __GSTV4L2BUFFER_H__ */
