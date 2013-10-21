/* GStreamer
 *
 * Copyright (C) 2001-2002 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *               2006 Edgard Lima <edgard.lima@indt.org.br>
 *
 * gstv4l2src.c: Video4Linux2 source element
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
 * SECTION:element-v4l2src
 *
 * v4l2src can be used to capture video from v4l2 devices, like webcams and tv
 * cards.
 *
 * <refsect2>
 * <title>Example launch lines</title>
 * |[
 * gst-launch v4l2src ! xvimagesink
 * ]| This pipeline shows the video captured from /dev/video0 tv card and for
 * webcams.
 * |[
 * gst-launch v4l2src ! jpegdec ! xvimagesink
 * ]| This pipeline shows the video captured from a webcam that delivers jpeg
 * images.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#undef HAVE_XVIDEO

#include <string.h>
#include <sys/time.h>
#include "v4l2src_calls.h"
#include <unistd.h>

#include "gstv4l2colorbalance.h"
#include "gstv4l2tuner.h"
#ifdef HAVE_XVIDEO
#include "gstv4l2xoverlay.h"
#endif
#include "gstv4l2vidorient.h"

#include "gst/gst-i18n-plugin.h"

GST_DEBUG_CATEGORY (v4l2src_debug);
#define GST_CAT_DEFAULT v4l2src_debug

#define PROP_DEF_QUEUE_SIZE         2
#define PROP_DEF_ALWAYS_COPY        TRUE
#define PROP_DEF_DECIMATE           1

#define DEFAULT_PROP_DEVICE   "/dev/video0"

enum
{
  PROP_0,
  V4L2_STD_OBJECT_PROPS,
  PROP_QUEUE_SIZE,
  PROP_ALWAYS_COPY,
  PROP_DECIMATE
};

GST_IMPLEMENT_V4L2_PROBE_METHODS (GstV4l2SrcClass, gst_v4l2src);
GST_IMPLEMENT_V4L2_COLOR_BALANCE_METHODS (GstV4l2Src, gst_v4l2src);
GST_IMPLEMENT_V4L2_TUNER_METHODS (GstV4l2Src, gst_v4l2src);
#ifdef HAVE_XVIDEO
GST_IMPLEMENT_V4L2_XOVERLAY_METHODS (GstV4l2Src, gst_v4l2src);
#endif
GST_IMPLEMENT_V4L2_VIDORIENT_METHODS (GstV4l2Src, gst_v4l2src);

static void gst_v4l2src_uri_handler_init (gpointer g_iface,
    gpointer iface_data);

static gboolean
gst_v4l2src_iface_supported (GstImplementsInterface * iface, GType iface_type)
{
  GstV4l2Object *v4l2object = GST_V4L2SRC (iface)->v4l2object;

#ifdef HAVE_XVIDEO
  g_assert (iface_type == GST_TYPE_TUNER ||
      iface_type == GST_TYPE_X_OVERLAY ||
      iface_type == GST_TYPE_COLOR_BALANCE ||
      iface_type == GST_TYPE_VIDEO_ORIENTATION);
#else
  g_assert (iface_type == GST_TYPE_TUNER ||
      iface_type == GST_TYPE_COLOR_BALANCE ||
      iface_type == GST_TYPE_VIDEO_ORIENTATION);
#endif

  if (v4l2object->video_fd == -1)
    return FALSE;

#ifdef HAVE_XVIDEO
  if (iface_type == GST_TYPE_X_OVERLAY && !GST_V4L2_IS_OVERLAY (v4l2object))
    return FALSE;
#endif

  return TRUE;
}

static void
gst_v4l2src_interface_init (GstImplementsInterfaceClass * klass)
{
  /*
   * default virtual functions 
   */
  klass->supported = gst_v4l2src_iface_supported;
}

static void
gst_v4l2src_init_interfaces (GType type)
{
  static const GInterfaceInfo urihandler_info = {
    gst_v4l2src_uri_handler_init,
    NULL,
    NULL
  };

  static const GInterfaceInfo v4l2iface_info = {
    (GInterfaceInitFunc) gst_v4l2src_interface_init,
    NULL,
    NULL,
  };
  static const GInterfaceInfo v4l2_tuner_info = {
    (GInterfaceInitFunc) gst_v4l2src_tuner_interface_init,
    NULL,
    NULL,
  };
#ifdef HAVE_XVIDEO
  /* FIXME: does GstXOverlay for v4l2src make sense in a GStreamer context? */
  static const GInterfaceInfo v4l2_xoverlay_info = {
    (GInterfaceInitFunc) gst_v4l2src_xoverlay_interface_init,
    NULL,
    NULL,
  };
#endif
  static const GInterfaceInfo v4l2_colorbalance_info = {
    (GInterfaceInitFunc) gst_v4l2src_color_balance_interface_init,
    NULL,
    NULL,
  };
  static const GInterfaceInfo v4l2_videoorientation_info = {
    (GInterfaceInitFunc) gst_v4l2src_video_orientation_interface_init,
    NULL,
    NULL,
  };
  static const GInterfaceInfo v4l2_propertyprobe_info = {
    (GInterfaceInitFunc) gst_v4l2src_property_probe_interface_init,
    NULL,
    NULL,
  };

  g_type_add_interface_static (type, GST_TYPE_URI_HANDLER, &urihandler_info);
  g_type_add_interface_static (type,
      GST_TYPE_IMPLEMENTS_INTERFACE, &v4l2iface_info);
  g_type_add_interface_static (type, GST_TYPE_TUNER, &v4l2_tuner_info);
#ifdef HAVE_XVIDEO
  g_type_add_interface_static (type, GST_TYPE_X_OVERLAY, &v4l2_xoverlay_info);
#endif
  g_type_add_interface_static (type,
      GST_TYPE_COLOR_BALANCE, &v4l2_colorbalance_info);
  g_type_add_interface_static (type,
      GST_TYPE_VIDEO_ORIENTATION, &v4l2_videoorientation_info);
  g_type_add_interface_static (type, GST_TYPE_PROPERTY_PROBE,
      &v4l2_propertyprobe_info);
}

GST_BOILERPLATE_FULL (GstV4l2Src, gst_v4l2src, GstPushSrc, GST_TYPE_PUSH_SRC,
    gst_v4l2src_init_interfaces);

static void gst_v4l2src_dispose (GObject * object);
static void gst_v4l2src_finalize (GstV4l2Src * v4l2src);

/* element methods */
static GstStateChangeReturn gst_v4l2src_change_state (GstElement * element,
    GstStateChange transition);

/* basesrc methods */
static gboolean gst_v4l2src_start (GstBaseSrc * src);
static gboolean gst_v4l2src_unlock (GstBaseSrc * src);
static gboolean gst_v4l2src_unlock_stop (GstBaseSrc * src);
static gboolean gst_v4l2src_stop (GstBaseSrc * src);
static gboolean gst_v4l2src_set_caps (GstBaseSrc * src, GstCaps * caps);
static GstCaps *gst_v4l2src_get_caps (GstBaseSrc * src);
static gboolean gst_v4l2src_query (GstBaseSrc * bsrc, GstQuery * query);
static GstFlowReturn gst_v4l2src_create (GstPushSrc * src, GstBuffer ** out);
static void gst_v4l2src_fixate (GstBaseSrc * basesrc, GstCaps * caps);
static gboolean gst_v4l2src_negotiate (GstBaseSrc * basesrc);

static void gst_v4l2src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_v4l2src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

/* get_frame io methods */
static GstFlowReturn
gst_v4l2src_get_read (GstV4l2Src * v4l2src, GstBuffer ** buf);
static GstFlowReturn
gst_v4l2src_get_mmap (GstV4l2Src * v4l2src, GstBuffer ** buf);

static void
gst_v4l2src_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);
  GstV4l2SrcClass *gstv4l2src_class = GST_V4L2SRC_CLASS (g_class);

  gstv4l2src_class->v4l2_class_devices = NULL;

  GST_DEBUG_CATEGORY_INIT (v4l2src_debug, "v4l2src", 0, "V4L2 source element");

  gst_element_class_set_details_simple (gstelement_class,
      "Video (video4linux2) Source", "Source/Video",
      "Reads frames from a Video4Linux2 device",
      "Edgard Lima <edgard.lima@indt.org.br>,"
      " Stefan Kost <ensonic@users.sf.net>");

  gst_element_class_add_pad_template
      (gstelement_class,
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_v4l2_object_get_all_caps ()));
}

static void
gst_v4l2src_class_init (GstV4l2SrcClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *element_class;
  GstBaseSrcClass *basesrc_class;
  GstPushSrcClass *pushsrc_class;

  gobject_class = G_OBJECT_CLASS (klass);
  element_class = GST_ELEMENT_CLASS (klass);
  basesrc_class = GST_BASE_SRC_CLASS (klass);
  pushsrc_class = GST_PUSH_SRC_CLASS (klass);

  gobject_class->dispose = gst_v4l2src_dispose;
  gobject_class->finalize = (GObjectFinalizeFunc) gst_v4l2src_finalize;
  gobject_class->set_property = gst_v4l2src_set_property;
  gobject_class->get_property = gst_v4l2src_get_property;

  element_class->change_state = gst_v4l2src_change_state;

  gst_v4l2_object_install_properties_helper (gobject_class,
      DEFAULT_PROP_DEVICE);
  g_object_class_install_property (gobject_class, PROP_QUEUE_SIZE,
      g_param_spec_uint ("queue-size", "Queue size",
          "Number of buffers to be enqueud in the driver in streaming mode",
          GST_V4L2_MIN_BUFFERS, GST_V4L2_MAX_BUFFERS, PROP_DEF_QUEUE_SIZE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_ALWAYS_COPY,
      g_param_spec_boolean ("always-copy", "Always Copy",
          "If the buffer will or not be used directly from mmap",
          PROP_DEF_ALWAYS_COPY, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  /**
   * GstV4l2Src:decimate
   *
   * Only use every nth frame
   *
   * Since: 0.10.26
   */
  g_object_class_install_property (gobject_class, PROP_DECIMATE,
      g_param_spec_int ("decimate", "Decimate",
          "Only use every nth frame", 1, G_MAXINT,
          PROP_DEF_DECIMATE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  basesrc_class->get_caps = GST_DEBUG_FUNCPTR (gst_v4l2src_get_caps);
  basesrc_class->set_caps = GST_DEBUG_FUNCPTR (gst_v4l2src_set_caps);
  basesrc_class->start = GST_DEBUG_FUNCPTR (gst_v4l2src_start);
  basesrc_class->unlock = GST_DEBUG_FUNCPTR (gst_v4l2src_unlock);
  basesrc_class->unlock_stop = GST_DEBUG_FUNCPTR (gst_v4l2src_unlock_stop);
  basesrc_class->stop = GST_DEBUG_FUNCPTR (gst_v4l2src_stop);
  basesrc_class->query = GST_DEBUG_FUNCPTR (gst_v4l2src_query);
  basesrc_class->fixate = GST_DEBUG_FUNCPTR (gst_v4l2src_fixate);
  basesrc_class->negotiate = GST_DEBUG_FUNCPTR (gst_v4l2src_negotiate);

  pushsrc_class->create = GST_DEBUG_FUNCPTR (gst_v4l2src_create);
}

static void
gst_v4l2src_init (GstV4l2Src * v4l2src, GstV4l2SrcClass * klass)
{
  /* fixme: give an update_fps_function */
  v4l2src->v4l2object = gst_v4l2_object_new (GST_ELEMENT (v4l2src),
      V4L2_BUF_TYPE_VIDEO_CAPTURE, DEFAULT_PROP_DEVICE,
      gst_v4l2_get_input, gst_v4l2_set_input, NULL);

  /* number of buffers requested */
  v4l2src->num_buffers = PROP_DEF_QUEUE_SIZE;

  v4l2src->always_copy = PROP_DEF_ALWAYS_COPY;
  v4l2src->decimate = PROP_DEF_DECIMATE;

  v4l2src->is_capturing = FALSE;

  gst_base_src_set_format (GST_BASE_SRC (v4l2src), GST_FORMAT_TIME);
  gst_base_src_set_live (GST_BASE_SRC (v4l2src), TRUE);

  v4l2src->fps_d = 0;
  v4l2src->fps_n = 0;
}


static void
gst_v4l2src_dispose (GObject * object)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (object);

  if (v4l2src->probed_caps) {
    gst_caps_unref (v4l2src->probed_caps);
  }

  G_OBJECT_CLASS (parent_class)->dispose (object);
}


static void
gst_v4l2src_finalize (GstV4l2Src * v4l2src)
{
  gst_v4l2_object_destroy (v4l2src->v4l2object);

  G_OBJECT_CLASS (parent_class)->finalize ((GObject *) (v4l2src));
}


static void
gst_v4l2src_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (object);

  if (!gst_v4l2_object_set_property_helper (v4l2src->v4l2object,
          prop_id, value, pspec)) {
    switch (prop_id) {
      case PROP_QUEUE_SIZE:
        v4l2src->num_buffers = g_value_get_uint (value);
        break;
      case PROP_ALWAYS_COPY:
        v4l2src->always_copy = g_value_get_boolean (value);
        break;
      case PROP_DECIMATE:
        v4l2src->decimate = g_value_get_int (value);
        break;
      default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
        break;
    }
  }
}


static void
gst_v4l2src_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (object);

  if (!gst_v4l2_object_get_property_helper (v4l2src->v4l2object,
          prop_id, value, pspec)) {
    switch (prop_id) {
      case PROP_QUEUE_SIZE:
        g_value_set_uint (value, v4l2src->num_buffers);
        break;
      case PROP_ALWAYS_COPY:
        g_value_set_boolean (value, v4l2src->always_copy);
        break;
      case PROP_DECIMATE:
        g_value_set_int (value, v4l2src->decimate);
        break;
      default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
        break;
    }
  }
}


/* this function is a bit of a last resort */
static void
gst_v4l2src_fixate (GstBaseSrc * basesrc, GstCaps * caps)
{
  GstStructure *structure;
  gint i;

  GST_DEBUG_OBJECT (basesrc, "fixating caps %" GST_PTR_FORMAT, caps);

  for (i = 0; i < gst_caps_get_size (caps); ++i) {
    const GValue *v;

    structure = gst_caps_get_structure (caps, i);

    /* FIXME such sizes? we usually fixate to something in the 320x200
     * range... */
    /* We are fixating to greater possble size (limited to GST_V4L2_MAX_SIZE)
       and the maximum framerate resolution for that size */
    gst_structure_fixate_field_nearest_int (structure, "width",
        GST_V4L2_MAX_SIZE);
    gst_structure_fixate_field_nearest_int (structure, "height",
        GST_V4L2_MAX_SIZE);
    gst_structure_fixate_field_nearest_fraction (structure, "framerate",
        G_MAXINT, 1);

    v = gst_structure_get_value (structure, "format");
    if (v && G_VALUE_TYPE (v) != GST_TYPE_FOURCC) {
      guint32 fourcc;

      g_return_if_fail (G_VALUE_TYPE (v) == GST_TYPE_LIST);

      fourcc = gst_value_get_fourcc (gst_value_list_get_value (v, 0));
      gst_structure_set (structure, "format", GST_TYPE_FOURCC, fourcc, NULL);
    }
  }

  GST_DEBUG_OBJECT (basesrc, "fixated caps %" GST_PTR_FORMAT, caps);
}


static gboolean
gst_v4l2src_negotiate (GstBaseSrc * basesrc)
{
  GstCaps *thiscaps;
  GstCaps *caps = NULL;
  GstCaps *peercaps = NULL;
  gboolean result = FALSE;

  /* first see what is possible on our source pad */
  thiscaps = gst_pad_get_caps (GST_BASE_SRC_PAD (basesrc));
  GST_DEBUG_OBJECT (basesrc, "caps of src: %" GST_PTR_FORMAT, thiscaps);
  LOG_CAPS (basesrc, thiscaps);

  /* nothing or anything is allowed, we're done */
  if (thiscaps == NULL || gst_caps_is_any (thiscaps))
    goto no_nego_needed;

  /* get the peer caps */
  peercaps = gst_pad_peer_get_caps (GST_BASE_SRC_PAD (basesrc));
  GST_DEBUG_OBJECT (basesrc, "caps of peer: %" GST_PTR_FORMAT, peercaps);
  LOG_CAPS (basesrc, peercaps);
  if (peercaps && !gst_caps_is_any (peercaps)) {
    GstCaps *icaps = NULL;
    int i;

    /* Prefer the first caps we are compatible with that the peer proposed */
    for (i = 0; i < gst_caps_get_size (peercaps); i++) {
      /* get intersection */
      GstCaps *ipcaps = gst_caps_copy_nth (peercaps, i);

      GST_DEBUG_OBJECT (basesrc, "peer: %" GST_PTR_FORMAT, ipcaps);
      LOG_CAPS (basesrc, ipcaps);

      icaps = gst_caps_intersect (thiscaps, ipcaps);
      gst_caps_unref (ipcaps);

      if (!gst_caps_is_empty (icaps))
        break;

      gst_caps_unref (icaps);
      icaps = NULL;
    }

    GST_DEBUG_OBJECT (basesrc, "intersect: %" GST_PTR_FORMAT, icaps);
    LOG_CAPS (basesrc, icaps);
    if (icaps) {
      /* If there are multiple intersections pick the one with the smallest
       * resolution strictly bigger then the first peer caps */
      if (gst_caps_get_size (icaps) > 1) {
        GstStructure *s = gst_caps_get_structure (peercaps, 0);

        int best = 0;

        int twidth, theight;

        int width = G_MAXINT, height = G_MAXINT;

        if (gst_structure_get_int (s, "width", &twidth)
            && gst_structure_get_int (s, "height", &theight)) {

          /* Walk the structure backwards to get the first entry of the
           * smallest resolution bigger (or equal to) the preferred resolution)
           */
          for (i = gst_caps_get_size (icaps) - 1; i >= 0; i--) {
            GstStructure *is = gst_caps_get_structure (icaps, i);

            int w, h;

            if (gst_structure_get_int (is, "width", &w)
                && gst_structure_get_int (is, "height", &h)) {
              if (w >= twidth && w <= width && h >= theight && h <= height) {
                width = w;
                height = h;
                best = i;
              }
            }
          }
        }

        caps = gst_caps_copy_nth (icaps, best);
        gst_caps_unref (icaps);
      } else {
        caps = icaps;
      }
    }
    gst_caps_unref (thiscaps);
    gst_caps_unref (peercaps);
  } else {
    /* no peer or peer have ANY caps, work with our own caps then */
    caps = thiscaps;
  }
  if (caps) {
    caps = gst_caps_make_writable (caps);
    gst_caps_truncate (caps);

    /* now fixate */
    if (!gst_caps_is_empty (caps)) {
      gst_pad_fixate_caps (GST_BASE_SRC_PAD (basesrc), caps);
      GST_DEBUG_OBJECT (basesrc, "fixated to: %" GST_PTR_FORMAT, caps);
      LOG_CAPS (basesrc, caps);

      if (gst_caps_is_any (caps)) {
        /* hmm, still anything, so element can do anything and
         * nego is not needed */
        result = TRUE;
      } else if (gst_caps_is_fixed (caps)) {
        /* yay, fixed caps, use those then */
        if (gst_pad_set_caps (GST_BASE_SRC_PAD (basesrc), caps))
          result = TRUE;
      }
    }
    gst_caps_unref (caps);
  }
  return result;

no_nego_needed:
  {
    GST_DEBUG_OBJECT (basesrc, "no negotiation needed");
    if (thiscaps)
      gst_caps_unref (thiscaps);
    return TRUE;
  }
}

static GstCaps *
gst_v4l2src_get_caps (GstBaseSrc * src)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (src);
  GstCaps *ret;
  GSList *walk;
  GSList *formats;

  if (!GST_V4L2_IS_OPEN (v4l2src->v4l2object)) {
    /* FIXME: copy? */
    return
        gst_caps_copy (gst_pad_get_pad_template_caps (GST_BASE_SRC_PAD
            (v4l2src)));
  }

  if (v4l2src->probed_caps)
    return gst_caps_ref (v4l2src->probed_caps);

  formats = gst_v4l2_object_get_format_list (v4l2src->v4l2object);

  ret = gst_caps_new_empty ();

  for (walk = formats; walk; walk = walk->next) {
    struct v4l2_fmtdesc *format;

    GstStructure *template;

    format = (struct v4l2_fmtdesc *) walk->data;

    template = gst_v4l2_object_v4l2fourcc_to_structure (format->pixelformat);

    if (template) {
      GstCaps *tmp;

      tmp =
          gst_v4l2_object_probe_caps_for_format (v4l2src->v4l2object,
          format->pixelformat, template);
      if (tmp)
        gst_caps_append (ret, tmp);

      gst_structure_free (template);
    } else {
      GST_DEBUG_OBJECT (v4l2src, "unknown format %u", format->pixelformat);
    }
  }

  v4l2src->probed_caps = gst_caps_ref (ret);

  GST_INFO_OBJECT (v4l2src, "probed caps: %" GST_PTR_FORMAT, ret);

  return ret;
}

static gboolean
gst_v4l2src_set_caps (GstBaseSrc * src, GstCaps * caps)
{
  GstV4l2Src *v4l2src;
  gint w = 0, h = 0;
  gboolean interlaced;
  struct v4l2_fmtdesc *format;
  guint fps_n, fps_d;
  guint size;

  v4l2src = GST_V4L2SRC (src);

  /* if we're not open, punt -- we'll get setcaps'd later via negotiate */
  if (!GST_V4L2_IS_OPEN (v4l2src->v4l2object))
    return FALSE;

  /* make sure we stop capturing and dealloc buffers */
  if (GST_V4L2_IS_ACTIVE (v4l2src->v4l2object)) {
    /* both will throw an element-error on failure */
    if (!gst_v4l2src_capture_stop (v4l2src))
      return FALSE;
    if (!gst_v4l2src_capture_deinit (v4l2src))
      return FALSE;
  }

  /* we want our own v4l2 type of fourcc codes */
  if (!gst_v4l2_object_get_caps_info (v4l2src->v4l2object, caps, &format, &w,
          &h, &interlaced, &fps_n, &fps_d, &size)) {
    GST_INFO_OBJECT (v4l2src,
        "can't get capture format from caps %" GST_PTR_FORMAT, caps);
    return FALSE;
  }

  GST_DEBUG_OBJECT (v4l2src, "trying to set_capture %dx%d at %d/%d fps, "
      "format %s", w, h, fps_n, fps_d, format->description);

  if (!gst_v4l2src_set_capture (v4l2src, format->pixelformat, w, h,
          interlaced, fps_n, fps_d))
    /* error already posted */
    return FALSE;

  if (!gst_v4l2src_capture_init (v4l2src, caps))
    return FALSE;

  if (v4l2src->use_mmap) {
    v4l2src->get_frame = gst_v4l2src_get_mmap;
  } else {
    v4l2src->get_frame = gst_v4l2src_get_read;
  }

  if (!gst_v4l2src_capture_start (v4l2src))
    return FALSE;

  /* now store the expected output size */
  v4l2src->frame_byte_size = size;

  return TRUE;
}

static gboolean
gst_v4l2src_query (GstBaseSrc * bsrc, GstQuery * query)
{
  GstV4l2Src *src;

  gboolean res = FALSE;

  src = GST_V4L2SRC (bsrc);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_LATENCY:{
      GstClockTime min_latency, max_latency;

      /* device must be open */
      if (!GST_V4L2_IS_OPEN (src->v4l2object)) {
        GST_WARNING_OBJECT (src,
            "Can't give latency since device isn't open !");
        goto done;
      }

      /* we must have a framerate */
      if (src->fps_n <= 0 || src->fps_d <= 0) {
        GST_WARNING_OBJECT (src,
            "Can't give latency since framerate isn't fixated !");
        goto done;
      }

      /* min latency is the time to capture one frame */
      min_latency =
          gst_util_uint64_scale_int (GST_SECOND, src->fps_d, src->fps_n);

      /* max latency is total duration of the frame buffer */
      max_latency = src->num_buffers * min_latency;

      GST_DEBUG_OBJECT (bsrc,
          "report latency min %" GST_TIME_FORMAT " max %" GST_TIME_FORMAT,
          GST_TIME_ARGS (min_latency), GST_TIME_ARGS (max_latency));

      /* we are always live, the min latency is 1 frame and the max latency is
       * the complete buffer of frames. */
      gst_query_set_latency (query, TRUE, min_latency, max_latency);

      res = TRUE;
      break;
    }
    default:
      res = GST_BASE_SRC_CLASS (parent_class)->query (bsrc, query);
      break;
  }

done:

  return res;
}

/* start and stop are not symmetric -- start will open the device, but not start
 * capture. it's setcaps that will start capture, which is called via basesrc's
 * negotiate method. stop will both stop capture and close the device.
 */
static gboolean
gst_v4l2src_start (GstBaseSrc * src)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (src);

  v4l2src->offset = 0;

  /* activate settings for first frame */
  v4l2src->ctrl_time = 0;
  gst_object_sync_values (G_OBJECT (src), v4l2src->ctrl_time);

  return TRUE;
}

static gboolean
gst_v4l2src_unlock (GstBaseSrc * src)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (src);

  GST_LOG_OBJECT (src, "Flushing");
  gst_poll_set_flushing (v4l2src->v4l2object->poll, TRUE);

  return TRUE;
}

static gboolean
gst_v4l2src_unlock_stop (GstBaseSrc * src)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (src);

  GST_LOG_OBJECT (src, "No longer flushing");
  gst_poll_set_flushing (v4l2src->v4l2object->poll, FALSE);

  return TRUE;
}

static gboolean
gst_v4l2src_stop (GstBaseSrc * src)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (src);

  if (GST_V4L2_IS_ACTIVE (v4l2src->v4l2object)
      && !gst_v4l2src_capture_stop (v4l2src))
    return FALSE;

  if (v4l2src->v4l2object->buffer != NULL) {
    if (!gst_v4l2src_capture_deinit (v4l2src))
      return FALSE;
  }

  v4l2src->fps_d = 0;
  v4l2src->fps_n = 0;

  return TRUE;
}

static GstStateChangeReturn
gst_v4l2src_change_state (GstElement * element, GstStateChange transition)
{
  GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;
  GstV4l2Src *v4l2src = GST_V4L2SRC (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      /* open the device */
      if (!gst_v4l2_object_start (v4l2src->v4l2object))
        return GST_STATE_CHANGE_FAILURE;
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_NULL:
      /* close the device */
      if (!gst_v4l2_object_stop (v4l2src->v4l2object))
        return GST_STATE_CHANGE_FAILURE;

      if (v4l2src->probed_caps) {
        gst_caps_unref (v4l2src->probed_caps);
        v4l2src->probed_caps = NULL;
      }
      break;
    default:
      break;
  }

  return ret;
}

static GstFlowReturn
gst_v4l2src_get_read (GstV4l2Src * v4l2src, GstBuffer ** buf)
{
  gint amount;
  gint ret;

  gint buffersize;

  buffersize = v4l2src->frame_byte_size;
  /* In case the size per frame is unknown assume it's a streaming format (e.g.
   * mpegts) and grab a reasonable default size instead */
  if (buffersize == 0)
    buffersize = GST_BASE_SRC (v4l2src)->blocksize;

  *buf = gst_buffer_new_and_alloc (buffersize);

  do {
    ret = gst_poll_wait (v4l2src->v4l2object->poll, GST_CLOCK_TIME_NONE);
    if (G_UNLIKELY (ret < 0)) {
      if (errno == EBUSY)
        goto stopped;
      if (errno == ENXIO) {
        GST_DEBUG_OBJECT (v4l2src,
            "v4l2 device doesn't support polling. Disabling");
        v4l2src->v4l2object->can_poll_device = FALSE;
      } else {
        if (errno != EAGAIN && errno != EINTR)
          goto select_error;
      }
    }
    amount =
        v4l2_read (v4l2src->v4l2object->video_fd, GST_BUFFER_DATA (*buf),
        buffersize);
    if (amount == buffersize) {
      break;
    } else if (amount == -1) {
      if (errno == EAGAIN || errno == EINTR) {
        continue;
      } else
        goto read_error;
    } else {
      /* short reads can happen if a signal interrupts the read */
      continue;
    }
  } while (TRUE);

  /* we set the buffer metadata in gst_v4l2src_create() */

  return GST_FLOW_OK;

  /* ERRORS */
select_error:
  {
    GST_ELEMENT_ERROR (v4l2src, RESOURCE, READ, (NULL),
        ("select error %d: %s (%d)", ret, g_strerror (errno), errno));
    return GST_FLOW_ERROR;
  }
stopped:
  {
    GST_DEBUG ("stop called");
    return GST_FLOW_WRONG_STATE;
  }
read_error:
  {
    GST_ELEMENT_ERROR (v4l2src, RESOURCE, READ,
        (_("Error reading %d bytes from device '%s'."),
            buffersize, v4l2src->v4l2object->videodev), GST_ERROR_SYSTEM);
    gst_buffer_unref (*buf);
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_v4l2src_get_mmap (GstV4l2Src * v4l2src, GstBuffer ** buf)
{
  GstBuffer *temp;
  GstFlowReturn ret;
  guint size;
  guint count = 0;

again:
  ret = gst_v4l2src_grab_frame (v4l2src, &temp);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto done;

  if (v4l2src->frame_byte_size > 0) {
    size = GST_BUFFER_SIZE (temp);

    /* if size does not match what we expected, try again */
    if (size != v4l2src->frame_byte_size) {
      GST_ELEMENT_WARNING (v4l2src, RESOURCE, READ,
          (_("Got unexpected frame size of %u instead of %u."),
              size, v4l2src->frame_byte_size), (NULL));
      gst_buffer_unref (temp);
      if (count++ > 50)
        goto size_error;

      goto again;
    }
  }

  *buf = temp;
done:
  return ret;

  /* ERRORS */
size_error:
  {
    GST_ELEMENT_ERROR (v4l2src, RESOURCE, READ,
        (_("Error reading %d bytes on device '%s'."),
            v4l2src->frame_byte_size, v4l2src->v4l2object->videodev), (NULL));
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_v4l2src_create (GstPushSrc * src, GstBuffer ** buf)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (src);
  int i;
  GstFlowReturn ret;

  for (i = 0; i < v4l2src->decimate - 1; i++) {
    ret = v4l2src->get_frame (v4l2src, buf);
    if (ret != GST_FLOW_OK) {
      return ret;
    }
    gst_buffer_unref (*buf);
  }

  ret = v4l2src->get_frame (v4l2src, buf);

  /* set buffer metadata */
  if (G_LIKELY (ret == GST_FLOW_OK && *buf)) {
    GstClock *clock;
    GstClockTime timestamp;

    GST_BUFFER_OFFSET (*buf) = v4l2src->offset++;
    GST_BUFFER_OFFSET_END (*buf) = v4l2src->offset;

    /* timestamps, LOCK to get clock and base time. */
    /* FIXME: element clock and base_time is rarely changing */
    GST_OBJECT_LOCK (v4l2src);
    if ((clock = GST_ELEMENT_CLOCK (v4l2src))) {
      /* we have a clock, get base time and ref clock */
      timestamp = GST_ELEMENT (v4l2src)->base_time;
      gst_object_ref (clock);
    } else {
      /* no clock, can't set timestamps */
      timestamp = GST_CLOCK_TIME_NONE;
    }
    GST_OBJECT_UNLOCK (v4l2src);

    if (G_LIKELY (clock)) {
      /* the time now is the time of the clock minus the base time */
      timestamp = gst_clock_get_time (clock) - timestamp;
      gst_object_unref (clock);

      /* if we have a framerate adjust timestamp for frame latency */
      if (GST_CLOCK_TIME_IS_VALID (v4l2src->duration)) {
        if (timestamp > v4l2src->duration)
          timestamp -= v4l2src->duration;
        else
          timestamp = 0;
      }
    }

    /* activate settings for next frame */
    if (GST_CLOCK_TIME_IS_VALID (v4l2src->duration)) {
      v4l2src->ctrl_time += v4l2src->duration;
    } else {
      /* this is not very good (as it should be the next timestamp),
       * still good enough for linear fades (as long as it is not -1) 
       */
      v4l2src->ctrl_time = timestamp;
    }
    gst_object_sync_values (G_OBJECT (src), v4l2src->ctrl_time);
    GST_INFO_OBJECT (src, "sync to %" GST_TIME_FORMAT,
        GST_TIME_ARGS (v4l2src->ctrl_time));

    /* FIXME: use the timestamp from the buffer itself! */
    GST_BUFFER_TIMESTAMP (*buf) = timestamp;
    GST_BUFFER_DURATION (*buf) = v4l2src->duration;
  }
  return ret;
}


/* GstURIHandler interface */
static GstURIType
gst_v4l2src_uri_get_type (void)
{
  return GST_URI_SRC;
}

static gchar **
gst_v4l2src_uri_get_protocols (void)
{
  static gchar *protocols[] = { (char *) "v4l2", NULL };

  return protocols;
}

static const gchar *
gst_v4l2src_uri_get_uri (GstURIHandler * handler)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (handler);

  if (v4l2src->v4l2object->videodev != NULL) {
    gchar uri[256];

    /* need to return a const string, but also don't want to leak the generated
     * string, so just intern it - there's a limited number of video devices
     * after all */
    g_snprintf (uri, sizeof (uri), "v4l2://%s", v4l2src->v4l2object->videodev);
    return g_intern_string (uri);
  }

  return "v4l2://";
}

static gboolean
gst_v4l2src_uri_set_uri (GstURIHandler * handler, const gchar * uri)
{
  GstV4l2Src *v4l2src = GST_V4L2SRC (handler);
  const gchar *device = DEFAULT_PROP_DEVICE;

  if (strcmp (uri, "v4l2://") != 0) {
    device = uri + 7;
  }
  g_object_set (v4l2src, "device", device, NULL);

  return TRUE;
}


static void
gst_v4l2src_uri_handler_init (gpointer g_iface, gpointer iface_data)
{
  GstURIHandlerInterface *iface = (GstURIHandlerInterface *) g_iface;

  iface->get_type = gst_v4l2src_uri_get_type;
  iface->get_protocols = gst_v4l2src_uri_get_protocols;
  iface->get_uri = gst_v4l2src_uri_get_uri;
  iface->set_uri = gst_v4l2src_uri_set_uri;
}
