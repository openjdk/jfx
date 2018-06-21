/* GStreamer
 * Copyright (C) <2011> Wim Taymans <wim.taymans@gmail.com>
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

/**
 * SECTION:gstaudiometa
 * @title: GstAudioDownmixMeta
 * @short_description: Buffer metadata for audio downmix matrix handling
 *
 * #GstAudioDownmixMeta defines an audio downmix matrix to be send along with
 * audio buffers. These functions in this module help to create and attach the
 * meta as well as extracting it.
 */

#include <string.h>

#include "gstaudiometa.h"

static gboolean
gst_audio_downmix_meta_init (GstMeta * meta, gpointer params,
    GstBuffer * buffer)
{
  GstAudioDownmixMeta *dmeta = (GstAudioDownmixMeta *) meta;

  dmeta->from_position = dmeta->to_position = NULL;
  dmeta->from_channels = dmeta->to_channels = 0;
  dmeta->matrix = NULL;

  return TRUE;
}

static void
gst_audio_downmix_meta_free (GstMeta * meta, GstBuffer * buffer)
{
  GstAudioDownmixMeta *dmeta = (GstAudioDownmixMeta *) meta;

  g_free (dmeta->from_position);
  if (dmeta->matrix) {
    g_free (*dmeta->matrix);
    g_free (dmeta->matrix);
  }
}

static gboolean
gst_audio_downmix_meta_transform (GstBuffer * dest, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  GstAudioDownmixMeta *smeta, *dmeta;

  smeta = (GstAudioDownmixMeta *) meta;

  if (GST_META_TRANSFORM_IS_COPY (type)) {
    dmeta = gst_buffer_add_audio_downmix_meta (dest, smeta->from_position,
      smeta->from_channels, smeta->to_position, smeta->to_channels,
      (const gfloat **) smeta->matrix);
    if (!dmeta)
      return FALSE;
  } else {
    /* return FALSE, if transform type is not supported */
    return FALSE;
  }

  return TRUE;
}

/**
 * gst_buffer_get_audio_downmix_meta_for_channels:
 * @buffer: a #GstBuffer
 * @to_position: (array length=to_channels): the channel positions of
 *   the destination
 * @to_channels: The number of channels of the destination
 *
 * Find the #GstAudioDownmixMeta on @buffer for the given destination
 * channel positions.
 *
 * Returns: (transfer none): the #GstAudioDownmixMeta on @buffer.
 */
GstAudioDownmixMeta *
gst_buffer_get_audio_downmix_meta_for_channels (GstBuffer * buffer,
    const GstAudioChannelPosition * to_position, gint to_channels)
{
  gpointer state = NULL;
  GstMeta *meta;
  const GstMetaInfo *info = GST_AUDIO_DOWNMIX_META_INFO;

  while ((meta = gst_buffer_iterate_meta (buffer, &state))) {
    if (meta->info->api == info->api) {
      GstAudioDownmixMeta *ameta = (GstAudioDownmixMeta *) meta;
      if (ameta->to_channels == to_channels &&
          memcmp (ameta->to_position, to_position,
              sizeof (GstAudioChannelPosition) * to_channels) == 0)
        return ameta;
    }
  }
  return NULL;
}

/**
 * gst_buffer_add_audio_downmix_meta:
 * @buffer: a #GstBuffer
 * @from_position: (array length=from_channels): the channel positions
 *   of the source
 * @from_channels: The number of channels of the source
 * @to_position: (array length=to_channels): the channel positions of
 *   the destination
 * @to_channels: The number of channels of the destination
 * @matrix: The matrix coefficients.
 *
 * Attaches #GstAudioDownmixMeta metadata to @buffer with the given parameters.
 *
 * @matrix is an two-dimensional array of @to_channels times @from_channels
 * coefficients, i.e. the i-th output channels is constructed by multiplicating
 * the input channels with the coefficients in @matrix[i] and taking the sum
 * of the results.
 *
 * Returns: (transfer none): the #GstAudioDownmixMeta on @buffer.
 */
GstAudioDownmixMeta *
gst_buffer_add_audio_downmix_meta (GstBuffer * buffer,
    const GstAudioChannelPosition * from_position, gint from_channels,
    const GstAudioChannelPosition * to_position, gint to_channels,
    const gfloat ** matrix)
{
  GstAudioDownmixMeta *meta;
  gint i;

  g_return_val_if_fail (from_position != NULL, NULL);
  g_return_val_if_fail (from_channels > 0, NULL);
  g_return_val_if_fail (to_position != NULL, NULL);
  g_return_val_if_fail (to_channels > 0, NULL);
  g_return_val_if_fail (matrix != NULL, NULL);

  meta =
      (GstAudioDownmixMeta *) gst_buffer_add_meta (buffer,
      GST_AUDIO_DOWNMIX_META_INFO, NULL);

  meta->from_channels = from_channels;
  meta->to_channels = to_channels;

  meta->from_position =
      g_new (GstAudioChannelPosition, meta->from_channels + meta->to_channels);
  meta->to_position = meta->from_position + meta->from_channels;
  memcpy (meta->from_position, from_position,
      sizeof (GstAudioChannelPosition) * meta->from_channels);
  memcpy (meta->to_position, to_position,
      sizeof (GstAudioChannelPosition) * meta->to_channels);

  meta->matrix = g_new (gfloat *, meta->to_channels);
  meta->matrix[0] = g_new (gfloat, meta->from_channels * meta->to_channels);
  memcpy (meta->matrix[0], matrix[0], sizeof (gfloat) * meta->from_channels);
  for (i = 1; i < meta->to_channels; i++) {
    meta->matrix[i] = meta->matrix[0] + i * meta->from_channels;
    memcpy (meta->matrix[i], matrix[i], sizeof (gfloat) * meta->from_channels);
  }

  return meta;
}

GType
gst_audio_downmix_meta_api_get_type (void)
{
  static volatile GType type;
  static const gchar *tags[] =
      { GST_META_TAG_AUDIO_STR, GST_META_TAG_AUDIO_CHANNELS_STR, NULL };

  if (g_once_init_enter (&type)) {
    GType _type = gst_meta_api_type_register ("GstAudioDownmixMetaAPI", tags);
    g_once_init_leave (&type, _type);
  }
  return type;
}

const GstMetaInfo *
gst_audio_downmix_meta_get_info (void)
{
  static const GstMetaInfo *audio_downmix_meta_info = NULL;

  if (g_once_init_enter ((GstMetaInfo **) & audio_downmix_meta_info)) {
    const GstMetaInfo *meta =
        gst_meta_register (GST_AUDIO_DOWNMIX_META_API_TYPE,
        "GstAudioDownmixMeta", sizeof (GstAudioDownmixMeta),
        gst_audio_downmix_meta_init, gst_audio_downmix_meta_free,
        gst_audio_downmix_meta_transform);
    g_once_init_leave ((GstMetaInfo **) & audio_downmix_meta_info,
        (GstMetaInfo *) meta);
  }
  return audio_downmix_meta_info;
}

static gboolean
gst_audio_clipping_meta_init (GstMeta * meta, gpointer params,
    GstBuffer * buffer)
{
  GstAudioClippingMeta *cmeta = (GstAudioClippingMeta *) meta;

  cmeta->format = GST_FORMAT_UNDEFINED;
  cmeta->start = cmeta->end = 0;

  return TRUE;
}

static gboolean
gst_audio_clipping_meta_transform (GstBuffer * dest, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  GstAudioClippingMeta *smeta, *dmeta;

  smeta = (GstAudioClippingMeta *) meta;

  if (GST_META_TRANSFORM_IS_COPY (type)) {
    GstMetaTransformCopy *copy = data;

    if (copy->region)
      return FALSE;

    dmeta =
        gst_buffer_add_audio_clipping_meta (dest, smeta->format, smeta->start,
        smeta->end);
    if (!dmeta)
      return FALSE;
  } else {
    /* TODO: Could implement an automatic transform for resampling */
    /* return FALSE, if transform type is not supported */
    return FALSE;
  }

  return TRUE;
}

/**
 * gst_buffer_add_audio_clipping_meta:
 * @buffer: a #GstBuffer
 * @format: GstFormat of @start and @stop, GST_FORMAT_DEFAULT is samples
 * @start: Amount of audio to clip from start of buffer
 * @end: Amount of  to clip from end of buffer
 *
 * Attaches #GstAudioClippingMeta metadata to @buffer with the given parameters.
 *
 * Returns: (transfer none): the #GstAudioClippingMeta on @buffer.
 *
 * Since: 1.8
 */
GstAudioClippingMeta *
gst_buffer_add_audio_clipping_meta (GstBuffer * buffer,
    GstFormat format, guint64 start, guint64 end)
{
  GstAudioClippingMeta *meta;

  g_return_val_if_fail (format != GST_FORMAT_UNDEFINED, NULL);

  meta =
      (GstAudioClippingMeta *) gst_buffer_add_meta (buffer,
      GST_AUDIO_CLIPPING_META_INFO, NULL);

  meta->format = format;
  meta->start = start;
  meta->end = end;

  return meta;
}

GType
gst_audio_clipping_meta_api_get_type (void)
{
  static volatile GType type;
  static const gchar *tags[] =
      { GST_META_TAG_AUDIO_STR, GST_META_TAG_AUDIO_RATE_STR, NULL };

  if (g_once_init_enter (&type)) {
    GType _type = gst_meta_api_type_register ("GstAudioClippingMetaAPI", tags);
    g_once_init_leave (&type, _type);
  }
  return type;
}

const GstMetaInfo *
gst_audio_clipping_meta_get_info (void)
{
  static const GstMetaInfo *audio_clipping_meta_info = NULL;

  if (g_once_init_enter ((GstMetaInfo **) & audio_clipping_meta_info)) {
    const GstMetaInfo *meta =
        gst_meta_register (GST_AUDIO_CLIPPING_META_API_TYPE,
        "GstAudioClippingMeta", sizeof (GstAudioClippingMeta),
        gst_audio_clipping_meta_init, NULL,
        gst_audio_clipping_meta_transform);
    g_once_init_leave ((GstMetaInfo **) & audio_clipping_meta_info,
        (GstMetaInfo *) meta);
  }
  return audio_clipping_meta_info;
}
