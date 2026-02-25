/* GStreamer
 * Copyright (C) 2005 Wim Taymans <wim at fluendo dot com>
 *
 * gstaudioconvert.h: Convert audio to different audio formats automatically
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

#ifndef __GST_AUDIO_CONVERT_H__
#define __GST_AUDIO_CONVERT_H__

#include <gst/gst.h>
#include <gst/base/gstbasetransform.h>
#include <gst/audio/audio.h>

/**
 * GstAudioConvertInputChannelsReorder:
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST: reorder input channels
 *     according to the default ordering in GStreamer: FRONT_LEFT, FRONT_RIGHT,
 *     FRONT_CENTER, LFE1 and then the other channels. If there is only one
 *     input channel available, it will be positioned to MONO.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_SMPTE: reorder input channels
 *     according to the SMPTE standard: FRONT_LEFT, FRONT_RIGHT, FRONT_CENTER,
 *     LFE1 and then the other channels (the ordering is slightly different from
 *     the default GStreamer order). This audio channels ordering is the only
 *     one that is officially standardized and used by default in many audio
 *     softwares (see: https://www.sis.se/api/document/preview/919377/). If
 *     there is only one input channel available, it will be positioned to MONO.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE: reorder input channels as it
 *     is commonly used in the cinema industry: FRONT_LEFT, FRONT_RIGHT,
 *     FRONT_CENTER, the other channels and then LFE1. This configuration is not
 *     standardized but usually appears in the literature related to the cinema
 *     industry and as an alternate ordering in different audio softwares. On
 *     some web sites, this configuration and the
 *     @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3 ordering are switched. If
 *     there is only one input channel available, it will be positioned to
 *     MONO. If the number of available input channels is > 2, the last channel
 *     will always be positioned to LFE1.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3: reorder input channels in the
 *     same order as the default order of the AC3 format: FRONT_LEFT,
 *     FRONT_CENTER, FRONT_RIGHT, the other channels (same order as in the
 *     @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE policy) and then LFE1.
 *     This configuration is also commonly used in the cinema industry and in
 *     professional audio softwares (like ProTools under the name "FILM"
 *     ordering). The only difference with the
 *     @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE configuration is the
 *     order of the first 3 channels. If there is only one input channel
 *     available, it will be positioned to MONO. If the number of available
 *     input channels is > 2, the last channel will always be positioned to
 *     LFE1. If the number of available input channels is 2 or 3, the first two
 *     channels will be positioned to FRONT_LEFT and FRONT_RIGHT.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AAC: reorder input channels in the
 *     same order as the default order of the AAC format: FRONT_CENTER,
 *     FRONT_LEFT, FRONT_RIGHT, the other channels (same order as in the
 *     @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE configuration) and then
 *     LFE1. The only difference with the
 *     @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE configuration is the
 *     order of the first 3 channels. If there is only one input channel
 *     available, it will be positioned to MONO. If the number of available
 *     input channels is > 2, the last channel will always be positioned to
 *     LFE1. If the number of available input channels is 2 or 3, the first two
 *     channels will be positioned to FRONT_LEFT and FRONT_RIGHT.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO: reorder all input channels
 *     to MONO. All input channels are mixed together at the same level to a
 *     virtual single mono channel. For `n` input channels, the virtual output
 *     sample value is computed as:
 *     `output_sample[MONO] = (1/n) x ∑ input_sample_for_channel(i)` with
 *     `0 <= i < n`. A concrete usage for this configuration is, for example,
 *     when importing audio from an array of multiple mono microphones and you
 *     want to use them as a unique mono channel.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_ALTERNATE: reorder all input
 *     channels to FRONT_LEFT and FRONT_RIGHT channels alternately (or MONO if
 *     there is only one input channel available). All left input channels are
 *     mixed together, at the same level, to a single FRONT_LEFT virtual
 *     channel and all right input channels are mixed together to a single
 *     FRONT_RIGHT virtual channel. For `2n` input channels the FRONT_LEFT and
 *     FRONT_RIGHT virtual output samples are computed as:
 *     `output_sample[FRONT_LEFT] = (1/n) x ∑ input_sample_for_channel(2i)` and
 *     `output_sample[FRONT_RIGHT] = (1/n) x ∑ input_sample_for_channel(2i+1)`
 *     with `0 <= i < n` (in case of an odd number of input channels the
 *     principle is the same but with an extra input left channel). A concrete
 *     usage for this configuration is, for example, when importing audio from
 *     an array of multiple stereo microphones and you want to use them as a
 *     simple pair of stereo channels.
 *
 * Input audio channels reordering configurations.
 *
 * It defines different ways of reordering input audio channels when they are
 * not positioned by GStreamer. As a general matter, channels are always ordered
 * in the @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST order and the
 * `channel-mask` field in the audio caps allows specifying which channels are
 * active.
 *
 * Depending on the selected mode (see:
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_UNPOSITIONED), input channels
 * can be automatically positioned when the `channel-mask` is not specified or
 * equals 0. In this case, all input channels will be positioned according to
 * the selected reordering configuration and the index of each input channel.
 * This can be useful when importing audio from an array of independent
 * microphones for example.
 *
 * The reordering configuration can also be forced (see:
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_FORCE) to reposition all
 * input channels according to each channel index. In this case the
 * `channel-mask` will be totally ignored and input channels will be reordered
 * just like if they were unpositioned. This can be useful when importing
 * multi-channels audio with errors in the channels positioning.
 *
 * For any of the former configurations, when the reordering is applied
 * (input channels are unpositioned or the "force" mode is active):
 * - When there is only one input channel available, it is positioned to MONO
 *   always, independently of the selected configuration.
 * - When there are 2 input channels available, they are positioned to
 *   FRONT_LEFT and FRONT_RIGHT (except for the
 *   @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO configuration where all
 *   input channels are positioned to MONO).
 *
 * Since: 1.26
 */
typedef enum {
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST = 0,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_SMPTE,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AAC,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_ALTERNATE
} GstAudioConvertInputChannelsReorder;

/**
 * GstAudioConvertInputChannelsReorderMode:
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_NONE: never reorder the input
 *     channels. If input channels are unpositioned and there are, at least, 3
 *     input channels, an error will be generated.
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_UNPOSITIONED: automatically
 *     reorder the input channels according to the selected
 *     #GstAudioConvertInputChannelsReorder configuration when, and only when,
 *     they are unpositioned (the `channel-mask` equals 0 or is not specified
 *     in the input caps).
 * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_FORCE: always reorder the
 *     input channels according to the selected
 *     #GstAudioConvertInputChannelsReorder configuration. The `channel-mask`
 *     value in the input caps is completely ignored. Input channels are always
 *     reordered as if they were unpositioned independently of the input caps.
 *
 * The different usage modes of the input channels reordering configuration.
 *
 * Independently of the selected mode, the explicit definition of a mix matrix
 * takes precedence over the reorder configuration. In this case, the provided
 * mix matrix will override the reorder configuration.
 *
 * Since: 1.26
 */
typedef enum {
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_NONE = 0,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_UNPOSITIONED,
  GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_FORCE
} GstAudioConvertInputChannelsReorderMode;

#define GST_TYPE_AUDIO_CONVERT (gst_audio_convert_get_type())
G_DECLARE_FINAL_TYPE (GstAudioConvert, gst_audio_convert,
    GST, AUDIO_CONVERT, GstBaseTransform);

/**
 * GstAudioConvert:
 *
 * The audioconvert object structure.
 */
struct _GstAudioConvert
{
  GstBaseTransform element;

  /* properties */
  GstAudioDitherMethod dither;
  guint dither_threshold;
  GstAudioNoiseShapingMethod ns;
  GValue mix_matrix;
  gboolean mix_matrix_is_set;
  GstAudioConvertInputChannelsReorder input_channels_reorder;
  GstAudioConvertInputChannelsReorderMode input_channels_reorder_mode;

  GstAudioInfo in_info;
  GstAudioInfo out_info;
  GstAudioConverter *convert;
};

GST_ELEMENT_REGISTER_DECLARE (audioconvert);

#endif /* __GST_AUDIO_CONVERT_H__ */
