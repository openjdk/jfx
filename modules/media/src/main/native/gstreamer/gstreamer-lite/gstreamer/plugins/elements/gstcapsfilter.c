/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *                    2005 David Schleef <ds@schleef.org>
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
 * SECTION:element-capsfilter
 *
 * The element does not modify data as such, but can enforce limitations on the
 * data format.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch videotestsrc ! video/x-raw-gray ! ffmpegcolorspace ! autovideosink
 * ]| Limits acceptable video from videotestsrc to be grayscale.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "../../gst/gst-i18n-lib.h"
#include "gstcapsfilter.h"

enum
{
  PROP_0,
  PROP_FILTER_CAPS
};


static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);


GST_DEBUG_CATEGORY_STATIC (gst_capsfilter_debug);
#define GST_CAT_DEFAULT gst_capsfilter_debug

#define _do_init(bla) \
    GST_DEBUG_CATEGORY_INIT (gst_capsfilter_debug, "capsfilter", 0, \
    "capsfilter element");

GST_BOILERPLATE_FULL (GstCapsFilter, gst_capsfilter, GstBaseTransform,
    GST_TYPE_BASE_TRANSFORM, _do_init);


static void gst_capsfilter_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_capsfilter_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);
static void gst_capsfilter_dispose (GObject * object);

static GstCaps *gst_capsfilter_transform_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps);
static gboolean gst_capsfilter_accept_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps);
static GstFlowReturn gst_capsfilter_transform_ip (GstBaseTransform * base,
    GstBuffer * buf);
static GstFlowReturn gst_capsfilter_prepare_buf (GstBaseTransform * trans,
    GstBuffer * input, gint size, GstCaps * caps, GstBuffer ** buf);

static void
gst_capsfilter_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (gstelement_class,
      "CapsFilter",
      "Generic",
      "Pass data without modification, limiting formats",
      "David Schleef <ds@schleef.org>");
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));
}

static void
gst_capsfilter_class_init (GstCapsFilterClass * klass)
{
  GObjectClass *gobject_class;
  GstBaseTransformClass *trans_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_capsfilter_set_property;
  gobject_class->get_property = gst_capsfilter_get_property;
  gobject_class->dispose = gst_capsfilter_dispose;

  g_object_class_install_property (gobject_class, PROP_FILTER_CAPS,
      g_param_spec_boxed ("caps", _("Filter caps"),
          _("Restrict the possible allowed capabilities (NULL means ANY). "
              "Setting this property takes a reference to the supplied GstCaps "
              "object."), GST_TYPE_CAPS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  trans_class = GST_BASE_TRANSFORM_CLASS (klass);
  trans_class->transform_caps =
      GST_DEBUG_FUNCPTR (gst_capsfilter_transform_caps);
  trans_class->transform_ip = GST_DEBUG_FUNCPTR (gst_capsfilter_transform_ip);
  trans_class->accept_caps = GST_DEBUG_FUNCPTR (gst_capsfilter_accept_caps);
  trans_class->prepare_output_buffer =
      GST_DEBUG_FUNCPTR (gst_capsfilter_prepare_buf);
}

static void
gst_capsfilter_init (GstCapsFilter * filter, GstCapsFilterClass * g_class)
{
  GstBaseTransform *trans = GST_BASE_TRANSFORM (filter);
  gst_base_transform_set_gap_aware (trans, TRUE);
  filter->filter_caps = gst_caps_new_any ();
}

static gboolean
copy_func (GQuark field_id, const GValue * value, GstStructure * dest)
{
  gst_structure_id_set_value (dest, field_id, value);

  return TRUE;
}

static void
gst_capsfilter_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstCapsFilter *capsfilter = GST_CAPSFILTER (object);

  switch (prop_id) {
    case PROP_FILTER_CAPS:{
      GstCaps *new_caps;
      GstCaps *old_caps, *suggest, *nego;
      const GstCaps *new_caps_val = gst_value_get_caps (value);

      if (new_caps_val == NULL) {
        new_caps = gst_caps_new_any ();
      } else {
        new_caps = (GstCaps *) new_caps_val;
        gst_caps_ref (new_caps);
      }

      GST_OBJECT_LOCK (capsfilter);
      old_caps = capsfilter->filter_caps;
      capsfilter->filter_caps = new_caps;
      GST_OBJECT_UNLOCK (capsfilter);

      gst_caps_unref (old_caps);

      GST_DEBUG_OBJECT (capsfilter, "set new caps %" GST_PTR_FORMAT, new_caps);

      /* filter the currently negotiated format against the new caps */
      GST_OBJECT_LOCK (GST_BASE_TRANSFORM_SINK_PAD (object));
      nego = GST_PAD_CAPS (GST_BASE_TRANSFORM_SINK_PAD (object));
      if (nego) {
        GST_DEBUG_OBJECT (capsfilter, "we had negotiated caps %" GST_PTR_FORMAT,
            nego);

        if (G_UNLIKELY (gst_caps_is_any (new_caps))) {
          GST_DEBUG_OBJECT (capsfilter, "not settings any suggestion");

          suggest = NULL;
        } else {
          GstStructure *s1, *s2;

          /* first check if the name is the same */
          s1 = gst_caps_get_structure (nego, 0);
          s2 = gst_caps_get_structure (new_caps, 0);

          if (gst_structure_get_name_id (s1) == gst_structure_get_name_id (s2)) {
            /* same name, copy all fields from the new caps into the previously
             * negotiated caps */
            suggest = gst_caps_copy (nego);
            s1 = gst_caps_get_structure (suggest, 0);
            gst_structure_foreach (s2, (GstStructureForeachFunc) copy_func, s1);
            GST_DEBUG_OBJECT (capsfilter, "copied structure fields");
          } else {
            GST_DEBUG_OBJECT (capsfilter, "different structure names");
            /* different names, we can only suggest the complete caps */
            suggest = gst_caps_copy (new_caps);
          }
        }
      } else {
        GST_DEBUG_OBJECT (capsfilter, "no negotiated caps");
        /* no previous caps, the getcaps function will be used to find suitable
         * caps */
        suggest = NULL;
      }
      GST_OBJECT_UNLOCK (GST_BASE_TRANSFORM_SINK_PAD (object));

      GST_DEBUG_OBJECT (capsfilter, "suggesting new caps %" GST_PTR_FORMAT,
          suggest);
      gst_base_transform_suggest (GST_BASE_TRANSFORM (object), suggest, 0);
      if (suggest)
        gst_caps_unref (suggest);

      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_capsfilter_get_property (GObject * object, guint prop_id, GValue * value,
    GParamSpec * pspec)
{
  GstCapsFilter *capsfilter = GST_CAPSFILTER (object);

  switch (prop_id) {
    case PROP_FILTER_CAPS:
      GST_OBJECT_LOCK (capsfilter);
      gst_value_set_caps (value, capsfilter->filter_caps);
      GST_OBJECT_UNLOCK (capsfilter);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_capsfilter_dispose (GObject * object)
{
  GstCapsFilter *filter = GST_CAPSFILTER (object);

  gst_caps_replace (&filter->filter_caps, NULL);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static GstCaps *
gst_capsfilter_transform_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps)
{
  GstCapsFilter *capsfilter = GST_CAPSFILTER (base);
  GstCaps *ret, *filter_caps;

  GST_OBJECT_LOCK (capsfilter);
  filter_caps = gst_caps_ref (capsfilter->filter_caps);
  GST_OBJECT_UNLOCK (capsfilter);

  ret = gst_caps_intersect (caps, filter_caps);
  GST_DEBUG_OBJECT (capsfilter, "input:     %" GST_PTR_FORMAT, caps);
  GST_DEBUG_OBJECT (capsfilter, "filter:    %" GST_PTR_FORMAT, filter_caps);
  GST_DEBUG_OBJECT (capsfilter, "intersect: %" GST_PTR_FORMAT, ret);

  gst_caps_unref (filter_caps);

  return ret;
}

static gboolean
gst_capsfilter_accept_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps)
{
  GstCapsFilter *capsfilter = GST_CAPSFILTER (base);
  GstCaps *filter_caps;
  gboolean ret;

  GST_OBJECT_LOCK (capsfilter);
  filter_caps = gst_caps_ref (capsfilter->filter_caps);
  GST_OBJECT_UNLOCK (capsfilter);

  ret = gst_caps_can_intersect (caps, filter_caps);
  GST_DEBUG_OBJECT (capsfilter, "can intersect: %d", ret);
  if (ret) {
    /* if we can intersect, see if the other end also accepts */
    if (direction == GST_PAD_SRC)
      ret = gst_pad_peer_accept_caps (GST_BASE_TRANSFORM_SINK_PAD (base), caps);
    else
      ret = gst_pad_peer_accept_caps (GST_BASE_TRANSFORM_SRC_PAD (base), caps);
    GST_DEBUG_OBJECT (capsfilter, "peer accept: %d", ret);
  }

  gst_caps_unref (filter_caps);

  return ret;
}

static GstFlowReturn
gst_capsfilter_transform_ip (GstBaseTransform * base, GstBuffer * buf)
{
  /* No actual work here. It's all done in the prepare output buffer
   * func. */
  return GST_FLOW_OK;
}

/* Output buffer preparation... if the buffer has no caps, and
 * our allowed output caps is fixed, then give the caps to the
 * buffer.
 * This ensures that outgoing buffers have caps if we can, so
 * that pipelines like:
 *   gst-launch filesrc location=rawsamples.raw !
 *       audio/x-raw-int,width=16,depth=16,rate=48000,channels=2,
 *       endianness=4321,signed='(boolean)'true ! alsasink
 * will work.
 */
static GstFlowReturn
gst_capsfilter_prepare_buf (GstBaseTransform * trans, GstBuffer * input,
    gint size, GstCaps * caps, GstBuffer ** buf)
{
  GstFlowReturn ret = GST_FLOW_OK;

  if (GST_BUFFER_CAPS (input) != NULL) {
    /* Output buffer already has caps */
    GST_LOG_OBJECT (trans, "Input buffer already has caps (implicitely fixed)");
    /* FIXME : Move this behaviour to basetransform. The given caps are the ones
     * of the source pad, therefore our outgoing buffers should always have
     * those caps. */
    if (GST_BUFFER_CAPS (input) != caps) {
      /* caps are different, make a metadata writable output buffer to set
       * caps */
      if (gst_buffer_is_metadata_writable (input)) {
        /* input is writable, just set caps and use this as the output */
        *buf = input;
        gst_buffer_set_caps (*buf, caps);
        gst_buffer_ref (input);
      } else {
        GST_DEBUG_OBJECT (trans, "Creating sub-buffer and setting caps");
        *buf = gst_buffer_create_sub (input, 0, GST_BUFFER_SIZE (input));
        gst_buffer_set_caps (*buf, caps);
      }
    } else {
      /* caps are right, just use a ref of the input as the outbuf */
      *buf = input;
      gst_buffer_ref (input);
    }
  } else {
    /* Buffer has no caps. See if the output pad only supports fixed caps */
    GstCaps *out_caps;

    out_caps = GST_PAD_CAPS (trans->srcpad);

    if (out_caps != NULL) {
      gst_caps_ref (out_caps);
    } else {
      out_caps = gst_pad_get_allowed_caps (trans->srcpad);
      g_return_val_if_fail (out_caps != NULL, GST_FLOW_ERROR);
    }

    out_caps = gst_caps_make_writable (out_caps);
    gst_caps_do_simplify (out_caps);

    if (gst_caps_is_fixed (out_caps) && !gst_caps_is_empty (out_caps)) {
      GST_DEBUG_OBJECT (trans, "Have fixed output caps %"
          GST_PTR_FORMAT " to apply to buffer with no caps", out_caps);
      if (gst_buffer_is_metadata_writable (input)) {
        gst_buffer_ref (input);
        *buf = input;
      } else {
        GST_DEBUG_OBJECT (trans, "Creating sub-buffer and setting caps");
        *buf = gst_buffer_create_sub (input, 0, GST_BUFFER_SIZE (input));
      }
      GST_BUFFER_CAPS (*buf) = out_caps;

      if (GST_PAD_CAPS (trans->srcpad) == NULL)
        gst_pad_set_caps (trans->srcpad, out_caps);
    } else {
      gchar *caps_str = gst_caps_to_string (out_caps);

      GST_DEBUG_OBJECT (trans, "Cannot choose caps. Have unfixed output caps %"
          GST_PTR_FORMAT, out_caps);
      gst_caps_unref (out_caps);

      ret = GST_FLOW_ERROR;
      GST_ELEMENT_ERROR (trans, STREAM, FORMAT,
          ("Filter caps do not completely specify the output format"),
          ("Output caps are unfixed: %s", caps_str));
      g_free (caps_str);
    }
  }

  return ret;
}
