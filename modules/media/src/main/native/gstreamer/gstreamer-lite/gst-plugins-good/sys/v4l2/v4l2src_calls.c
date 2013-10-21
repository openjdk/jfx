/* GStreamer
 *
 * Copyright (C) 2002 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * v4l2src.c - system calls
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

#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <string.h>
#include <errno.h>
#include "v4l2src_calls.h"
#include <sys/time.h>
#include <unistd.h>
#ifdef __sun
/* Needed on older Solaris Nevada builds (72 at least) */
#include <stropts.h>
#include <sys/ioccom.h>
#endif

#include "gstv4l2tuner.h"
#include "gstv4l2bufferpool.h"

#include "gst/gst-i18n-plugin.h"

#define GST_CAT_DEFAULT v4l2src_debug
GST_DEBUG_CATEGORY_EXTERN (GST_CAT_PERFORMANCE);

/* lalala... */
#define GST_V4L2_SET_ACTIVE(element) (element)->buffer = GINT_TO_POINTER (-1)
#define GST_V4L2_SET_INACTIVE(element) (element)->buffer = NULL

/* On some systems MAP_FAILED seems to be missing */
#ifndef MAP_FAILED
#define MAP_FAILED ((caddr_t) -1)
#endif


/* Local functions */

static gboolean
gst_v4l2src_buffer_pool_activate (GstV4l2BufferPool * pool,
    GstV4l2Src * v4l2src)
{
  GstV4l2Buffer *buf;

  while ((buf = gst_v4l2_buffer_pool_get (pool, FALSE)) != NULL)
    if (!gst_v4l2_buffer_pool_qbuf (pool, buf))
      goto queue_failed;

  return TRUE;

  /* ERRORS */
queue_failed:
  {
    GST_ELEMENT_ERROR (v4l2src, RESOURCE, READ,
        (_("Could not enqueue buffers in device '%s'."),
            v4l2src->v4l2object->videodev),
        ("enqueing buffer %d/%d failed: %s",
            buf->vbuffer.index, v4l2src->num_buffers, g_strerror (errno)));
    return FALSE;
  }
}

/******************************************************
 * gst_v4l2src_grab_frame ():
 *   grab a frame for capturing
 * return value: GST_FLOW_OK, GST_FLOW_WRONG_STATE or GST_FLOW_ERROR
 ******************************************************/
GstFlowReturn
gst_v4l2src_grab_frame (GstV4l2Src * v4l2src, GstBuffer ** buf)
{
#define NUM_TRIALS 50
  GstV4l2Object *v4l2object;
  GstV4l2BufferPool *pool;
  gint32 trials = NUM_TRIALS;
  GstBuffer *pool_buffer;
  gboolean need_copy;
  gint ret;

  v4l2object = v4l2src->v4l2object;
  pool = v4l2src->pool;
  if (!pool)
    goto no_buffer_pool;

  GST_DEBUG_OBJECT (v4l2src, "grab frame");

  for (;;) {
    if (v4l2object->can_poll_device) {
      ret = gst_poll_wait (v4l2object->poll, GST_CLOCK_TIME_NONE);
      if (G_UNLIKELY (ret < 0)) {
        if (errno == EBUSY)
          goto stopped;
        if (errno == ENXIO) {
          GST_DEBUG_OBJECT (v4l2src,
              "v4l2 device doesn't support polling. Disabling");
          v4l2object->can_poll_device = FALSE;
        } else {
          if (errno != EAGAIN && errno != EINTR)
            goto select_error;
        }
      }
    }

    pool_buffer = GST_BUFFER (gst_v4l2_buffer_pool_dqbuf (pool));
    if (pool_buffer)
      break;

    GST_WARNING_OBJECT (pool->v4l2elem, "trials=%d", trials);

    /* if the sync() got interrupted, we can retry */
    switch (errno) {
      case EINVAL:
      case ENOMEM:
        /* fatal */
        return GST_FLOW_ERROR;

      case EAGAIN:
      case EIO:
      case EINTR:
      default:
        /* try again, until too many trials */
        break;
    }

    /* check nr. of attempts to capture */
    if (--trials == -1) {
      goto too_many_trials;
    }
  }

  /* if we are handing out the last buffer in the pool, we need to make a
   * copy and bring the buffer back in the pool. */
  need_copy = v4l2src->always_copy
      || !gst_v4l2_buffer_pool_available_buffers (pool);

  if (G_UNLIKELY (need_copy)) {
    if (!v4l2src->always_copy) {
      GST_CAT_LOG_OBJECT (GST_CAT_PERFORMANCE, v4l2src,
          "running out of buffers, making a copy to reuse current one");
    }
    *buf = gst_buffer_copy (pool_buffer);
    GST_BUFFER_FLAG_UNSET (*buf, GST_BUFFER_FLAG_READONLY);
    /* this will requeue */
    gst_buffer_unref (pool_buffer);
  } else {
    *buf = pool_buffer;
  }
  /* we set the buffer metadata in gst_v4l2src_create() */

  return GST_FLOW_OK;

  /* ERRORS */
no_buffer_pool:
  {
    GST_DEBUG ("no buffer pool");
    return GST_FLOW_WRONG_STATE;
  }
select_error:
  {
    GST_ELEMENT_ERROR (pool->v4l2elem, RESOURCE, READ, (NULL),
        ("select error %d: %s (%d)", ret, g_strerror (errno), errno));
    return GST_FLOW_ERROR;
  }
stopped:
  {
    GST_DEBUG ("stop called");
    return GST_FLOW_WRONG_STATE;
  }
too_many_trials:
  {
    GST_ELEMENT_ERROR (pool->v4l2elem, RESOURCE, FAILED,
        (_("Failed trying to get video frames from device '%s'."),
            v4l2object->videodev),
        (_("Failed after %d tries. device %s. system error: %s"),
            NUM_TRIALS, v4l2object->videodev, g_strerror (errno)));
    return GST_FLOW_ERROR;
  }
}

/* Note about fraction simplification
 * n1/d1 == n2/d2  is also written as  n1 == ( n2 * d1 ) / d2
 */
#define fractions_are_equal(n1,d1,n2,d2) ((n1) == gst_util_uint64_scale_int((n2), (d1), (d2)))

/******************************************************
 * gst_v4l2src_set_capture():
 *   set capture parameters
 * return value: TRUE on success, FALSE on error
 ******************************************************/
gboolean
gst_v4l2src_set_capture (GstV4l2Src * v4l2src, guint32 pixelformat,
    guint32 width, guint32 height, gboolean interlaced,
    guint fps_n, guint fps_d)
{
  gint fd = v4l2src->v4l2object->video_fd;
  struct v4l2_streamparm stream;

  if (pixelformat == GST_MAKE_FOURCC ('M', 'P', 'E', 'G'))
    return TRUE;

  if (!gst_v4l2_object_set_format (v4l2src->v4l2object, pixelformat, width,
          height, interlaced)) {
    /* error already reported */
    return FALSE;
  }

  /* Is there a reason we require the caller to always specify a framerate? */
  GST_DEBUG_OBJECT (v4l2src, "Desired framerate: %u/%u", fps_n, fps_d);

  memset (&stream, 0x00, sizeof (struct v4l2_streamparm));
  stream.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
  if (v4l2_ioctl (fd, VIDIOC_G_PARM, &stream) < 0) {
    GST_ELEMENT_WARNING (v4l2src, RESOURCE, SETTINGS,
        (_("Could not get parameters on device '%s'"),
            v4l2src->v4l2object->videodev), GST_ERROR_SYSTEM);
    goto done;
  }

  /* Note: V4L2 provides the frame interval, we have the frame rate */
  if (fractions_are_equal (stream.parm.capture.timeperframe.numerator,
          stream.parm.capture.timeperframe.denominator, fps_d, fps_n)) {
    GST_DEBUG_OBJECT (v4l2src, "Desired framerate already set");
    goto already_set;
  }

  /* We want to change the frame rate, so check whether we can. Some cheap USB
   * cameras don't have the capability */
  if ((stream.parm.capture.capability & V4L2_CAP_TIMEPERFRAME) == 0) {
    GST_DEBUG_OBJECT (v4l2src, "Not setting framerate (not supported)");
    goto done;
  }

  GST_LOG_OBJECT (v4l2src, "Setting framerate to %u/%u", fps_n, fps_d);

  /* Note: V4L2 wants the frame interval, we have the frame rate */
  stream.parm.capture.timeperframe.numerator = fps_d;
  stream.parm.capture.timeperframe.denominator = fps_n;

  /* some cheap USB cam's won't accept any change */
  if (v4l2_ioctl (fd, VIDIOC_S_PARM, &stream) < 0) {
    GST_ELEMENT_WARNING (v4l2src, RESOURCE, SETTINGS,
        (_("Video input device did not accept new frame rate setting.")),
        GST_ERROR_SYSTEM);
    goto done;
  }

already_set:

  v4l2src->fps_n = fps_n;
  v4l2src->fps_d = fps_d;

  /* if we have a framerate pre-calculate duration */
  if (fps_n > 0 && fps_d > 0) {
    v4l2src->duration = gst_util_uint64_scale_int (GST_SECOND, fps_d, fps_n);
  } else {
    v4l2src->duration = GST_CLOCK_TIME_NONE;
  }

  GST_INFO_OBJECT (v4l2src,
      "Set framerate to %u/%u and duration to %" GST_TIME_FORMAT, fps_n, fps_d,
      GST_TIME_ARGS (v4l2src->duration));
done:

  return TRUE;
}

/******************************************************
 * gst_v4l2src_capture_init():
 *   initialize the capture system
 * return value: TRUE on success, FALSE on error
 ******************************************************/
gboolean
gst_v4l2src_capture_init (GstV4l2Src * v4l2src, GstCaps * caps)
{
  GST_DEBUG_OBJECT (v4l2src, "initializing the capture system");

  GST_V4L2_CHECK_OPEN (v4l2src->v4l2object);
  GST_V4L2_CHECK_NOT_ACTIVE (v4l2src->v4l2object);

  if (v4l2src->v4l2object->vcap.capabilities & V4L2_CAP_STREAMING) {

    /* Map the buffers */
    GST_LOG_OBJECT (v4l2src, "initiating buffer pool");

    if (!(v4l2src->pool = gst_v4l2_buffer_pool_new (GST_ELEMENT (v4l2src),
                v4l2src->v4l2object->video_fd,
                v4l2src->num_buffers, caps, TRUE, V4L2_BUF_TYPE_VIDEO_CAPTURE)))
      goto buffer_pool_new_failed;

    GST_INFO_OBJECT (v4l2src, "capturing buffers via mmap()");
    v4l2src->use_mmap = TRUE;

    if (v4l2src->num_buffers != v4l2src->pool->buffer_count) {
      v4l2src->num_buffers = v4l2src->pool->buffer_count;
      g_object_notify (G_OBJECT (v4l2src), "queue-size");
    }

  } else if (v4l2src->v4l2object->vcap.capabilities & V4L2_CAP_READWRITE) {
    GST_INFO_OBJECT (v4l2src, "capturing buffers via read()");
    v4l2src->use_mmap = FALSE;
    v4l2src->pool = NULL;
  } else {
    goto no_supported_capture_method;
  }

  GST_V4L2_SET_ACTIVE (v4l2src->v4l2object);

  return TRUE;

  /* ERRORS */
buffer_pool_new_failed:
  {
    GST_ELEMENT_ERROR (v4l2src, RESOURCE, READ,
        (_("Could not map buffers from device '%s'"),
            v4l2src->v4l2object->videodev),
        ("Failed to create buffer pool: %s", g_strerror (errno)));
    return FALSE;
  }
no_supported_capture_method:
  {
    GST_ELEMENT_ERROR (v4l2src, RESOURCE, READ,
        (_("The driver of device '%s' does not support any known capture "
                "method."), v4l2src->v4l2object->videodev), (NULL));
    return FALSE;
  }
}


/******************************************************
 * gst_v4l2src_capture_start():
 *   start streaming capture
 * return value: TRUE on success, FALSE on error
 ******************************************************/
gboolean
gst_v4l2src_capture_start (GstV4l2Src * v4l2src)
{
  GST_DEBUG_OBJECT (v4l2src, "starting the capturing");
  //GST_V4L2_CHECK_OPEN (v4l2src->v4l2object);
  GST_V4L2_CHECK_ACTIVE (v4l2src->v4l2object);

  v4l2src->quit = FALSE;

  if (v4l2src->use_mmap) {
    if (!gst_v4l2src_buffer_pool_activate (v4l2src->pool, v4l2src)) {
      return FALSE;
    }

    if (!gst_v4l2_object_start_streaming (v4l2src->v4l2object)) {
      return FALSE;
    }
  }

  v4l2src->is_capturing = TRUE;

  return TRUE;
}

/******************************************************
 * gst_v4l2src_capture_stop():
 *   stop streaming capture
 * return value: TRUE on success, FALSE on error
 ******************************************************/
gboolean
gst_v4l2src_capture_stop (GstV4l2Src * v4l2src)
{
  GST_DEBUG_OBJECT (v4l2src, "stopping capturing");

  if (!GST_V4L2_IS_OPEN (v4l2src->v4l2object)) {
    goto done;
  }
  if (!GST_V4L2_IS_ACTIVE (v4l2src->v4l2object)) {
    goto done;
  }

  if (v4l2src->use_mmap) {
    /* we actually need to sync on all queued buffers but not
     * on the non-queued ones */
    if (!gst_v4l2_object_stop_streaming (v4l2src->v4l2object)) {
      return FALSE;
    }
  }

done:

  /* make an optional pending wait stop */
  v4l2src->quit = TRUE;
  v4l2src->is_capturing = FALSE;

  return TRUE;
}

/******************************************************
 * gst_v4l2src_capture_deinit():
 *   deinitialize the capture system
 * return value: TRUE on success, FALSE on error
 ******************************************************/
gboolean
gst_v4l2src_capture_deinit (GstV4l2Src * v4l2src)
{
  GST_DEBUG_OBJECT (v4l2src, "deinitting capture system");

  if (!GST_V4L2_IS_OPEN (v4l2src->v4l2object)) {
    return TRUE;
  }
  if (!GST_V4L2_IS_ACTIVE (v4l2src->v4l2object)) {
    return TRUE;
  }

  if (v4l2src->pool) {
    gst_v4l2_buffer_pool_destroy (v4l2src->pool);
    v4l2src->pool = NULL;
  }

  GST_V4L2_SET_INACTIVE (v4l2src->v4l2object);

  return TRUE;
}
