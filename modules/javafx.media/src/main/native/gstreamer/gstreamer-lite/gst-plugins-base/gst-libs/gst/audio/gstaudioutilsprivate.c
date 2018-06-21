/* GStreamer
 * Copyright (C) 2011 Mark Nauwelaerts <mark.nauwelaerts@collabora.co.uk>.
 * Copyright (C) 2011 Nokia Corporation. All rights reserved.
 *   Contact: Stefan Kost <stefan.kost@nokia.com>
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/audio/audio.h>
#include "gstaudioutilsprivate.h"

/*
 * Takes caps and copies its audio fields to tmpl_caps
 */
static GstCaps *
__gst_audio_element_proxy_caps (GstElement * element, GstCaps * templ_caps,
    GstCaps * caps)
{
  GstCaps *result = gst_caps_new_empty ();
  gint i, j;
  gint templ_caps_size = gst_caps_get_size (templ_caps);
  gint caps_size = gst_caps_get_size (caps);

  for (i = 0; i < templ_caps_size; i++) {
    GQuark q_name =
        gst_structure_get_name_id (gst_caps_get_structure (templ_caps, i));
    GstCapsFeatures *features = gst_caps_get_features (templ_caps, i);

    for (j = 0; j < caps_size; j++) {
      const GstStructure *caps_s = gst_caps_get_structure (caps, j);
      const GValue *val;
      GstStructure *s;
      GstCaps *tmp = gst_caps_new_empty ();

      s = gst_structure_new_id_empty (q_name);
      if ((val = gst_structure_get_value (caps_s, "rate")))
        gst_structure_set_value (s, "rate", val);
      if ((val = gst_structure_get_value (caps_s, "channels")))
        gst_structure_set_value (s, "channels", val);
      if ((val = gst_structure_get_value (caps_s, "channels-mask")))
        gst_structure_set_value (s, "channels-mask", val);

      gst_caps_append_structure_full (tmp, s,
          gst_caps_features_copy (features));
      result = gst_caps_merge (result, tmp);
    }
  }

  return result;
}

/**
 * __gst_audio_element_proxy_getcaps:
 * @element: a #GstElement
 * @sinkpad: the element's sink #GstPad
 * @srcpad: the element's source #GstPad
 * @initial_caps: initial caps
 * @filter: filter caps
 *
 * Returns caps that express @initial_caps (or sink template caps if
 * @initial_caps == NULL) restricted to rate/channels/...
 * combinations supported by downstream elements (e.g. muxers).
 *
 * Returns: a #GstCaps owned by caller
 */
GstCaps *
__gst_audio_element_proxy_getcaps (GstElement * element, GstPad * sinkpad,
    GstPad * srcpad, GstCaps * initial_caps, GstCaps * filter)
{
  GstCaps *templ_caps, *src_templ_caps;
  GstCaps *peer_caps;
  GstCaps *allowed;
  GstCaps *fcaps, *filter_caps;

  /* Allow downstream to specify rate/channels constraints
   * and forward them upstream for audio converters to handle
   */
  templ_caps = initial_caps ? gst_caps_ref (initial_caps) :
      gst_pad_get_pad_template_caps (sinkpad);
  src_templ_caps = gst_pad_get_pad_template_caps (srcpad);
  if (filter && !gst_caps_is_any (filter)) {
    GstCaps *proxy_filter =
        __gst_audio_element_proxy_caps (element, src_templ_caps, filter);

    peer_caps = gst_pad_peer_query_caps (srcpad, proxy_filter);
    gst_caps_unref (proxy_filter);
  } else {
    peer_caps = gst_pad_peer_query_caps (srcpad, NULL);
  }

  allowed = gst_caps_intersect_full (peer_caps, src_templ_caps,
      GST_CAPS_INTERSECT_FIRST);

  gst_caps_unref (src_templ_caps);
  gst_caps_unref (peer_caps);

  if (!allowed || gst_caps_is_any (allowed)) {
    fcaps = templ_caps;
    goto done;
  } else if (gst_caps_is_empty (allowed)) {
    fcaps = gst_caps_ref (allowed);
    goto done;
  }

  GST_LOG_OBJECT (element, "template caps %" GST_PTR_FORMAT, templ_caps);
  GST_LOG_OBJECT (element, "allowed caps %" GST_PTR_FORMAT, allowed);

  filter_caps = __gst_audio_element_proxy_caps (element, templ_caps, allowed);

  fcaps = gst_caps_intersect (filter_caps, templ_caps);
  gst_caps_unref (filter_caps);
  gst_caps_unref (templ_caps);

  if (filter) {
    GST_LOG_OBJECT (element, "intersecting with %" GST_PTR_FORMAT, filter);
    filter_caps = gst_caps_intersect (fcaps, filter);
    gst_caps_unref (fcaps);
    fcaps = filter_caps;
  }

done:
  gst_caps_replace (&allowed, NULL);

  GST_LOG_OBJECT (element, "proxy caps %" GST_PTR_FORMAT, fcaps);

  return fcaps;
}

/**
 * __gst_audio_encoded_audio_convert:
 * @fmt: audio format of the encoded audio
 * @bytes: number of encoded bytes
 * @samples: number of encoded samples
 * @src_format: source format
 * @src_value: source value
 * @dest_format: destination format
 * @dest_value: destination format
 *
 * Helper function to convert @src_value in @src_format to @dest_value in
 * @dest_format for encoded audio data.  Conversion is possible between
 * BYTE and TIME format by using estimated bitrate based on
 * @samples and @bytes (and @fmt).
 */
gboolean
__gst_audio_encoded_audio_convert (GstAudioInfo * fmt,
    gint64 bytes, gint64 samples, GstFormat src_format,
    gint64 src_value, GstFormat * dest_format, gint64 * dest_value)
{
  gboolean res = FALSE;

  g_return_val_if_fail (dest_format != NULL, FALSE);
  g_return_val_if_fail (dest_value != NULL, FALSE);

  if (G_UNLIKELY (src_format == *dest_format || src_value == 0 ||
          src_value == -1)) {
    if (dest_value)
      *dest_value = src_value;
    return TRUE;
  }

  if (samples == 0 || bytes == 0 || fmt->rate == 0) {
    GST_DEBUG ("not enough metadata yet to convert");
    goto exit;
  }

  bytes *= fmt->rate;

  switch (src_format) {
    case GST_FORMAT_BYTES:
      switch (*dest_format) {
        case GST_FORMAT_TIME:
          *dest_value = gst_util_uint64_scale (src_value,
              GST_SECOND * samples, bytes);
          res = TRUE;
          break;
        default:
          res = FALSE;
      }
      break;
    case GST_FORMAT_TIME:
      switch (*dest_format) {
        case GST_FORMAT_BYTES:
          *dest_value = gst_util_uint64_scale (src_value, bytes,
              samples * GST_SECOND);
          res = TRUE;
          break;
        default:
          res = FALSE;
      }
      break;
    default:
      res = FALSE;
  }

exit:
  return res;
}
