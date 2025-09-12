/* GStreamer base utils library codec-specific utility functions
 * Copyright (C) 2010 Arun Raghavan <arun.raghavan@collabora.co.uk>
 *               2013 Sreerenj Balachandran <sreerenj.balachandran@intel.com>
 *               2010 Collabora Multimedia
 *               2010 Nokia Corporation
 *               2013 Intel Corporation
 *               2015 Sebastian Dröge <sebastian@centricular.com>
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
 * SECTION:gstpbutilscodecutils
 * @title: Codec utilities
 * @short_description: Miscellaneous codec-specific utility functions
 *
 * Provides codec-specific ulility functions such as functions to provide the
 * codec profile and level in human-readable string form from header data.
 *
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "pbutils.h"
#include <gst/base/base.h>
#include <gst/base/gstbitreader.h>
#include <gst/tag/tag.h>

#include <stdio.h>
#include <string.h>

#ifndef GST_DISABLE_GST_DEBUG
#define GST_CAT_DEFAULT gst_pb_utils_codec_utils_ensure_debug_category()

static GstDebugCategory *
gst_pb_utils_codec_utils_ensure_debug_category (void)
{
  static gsize cat_gonce = 0;

  if (g_once_init_enter (&cat_gonce)) {
    GstDebugCategory *cat = NULL;

    GST_DEBUG_CATEGORY_INIT (cat, "codec-utils", 0,
        "GstPbUtils codec helper functions");

    g_once_init_leave (&cat_gonce, (gsize) cat);
  }

  return (GstDebugCategory *) cat_gonce;
}
#endif /* GST_DISABLE_GST_DEBUG */

#define GST_SIMPLE_CAPS_HAS_NAME(caps,name) \
    gst_structure_has_name(gst_caps_get_structure((caps),0),(name))

#define GST_SIMPLE_CAPS_HAS_FIELD(caps,field) \
    gst_structure_has_field(gst_caps_get_structure((caps),0),(field))

static const guint aac_sample_rates[] = { 96000, 88200, 64000, 48000, 44100,
  32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350
};

static const gchar *
digit_to_string (guint digit)
{
  static const char itoa[][2] = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
  };

  if (G_LIKELY (digit < 10))
    return itoa[digit];
  else
    return NULL;
}

/**
 * gst_codec_utils_aac_get_sample_rate_from_index:
 * @sr_idx: Sample rate index as from the AudioSpecificConfig (MPEG-4
 *          container) or ADTS frame header
 *
 * Translates the sample rate index found in AAC headers to the actual sample
 * rate.
 *
 * Returns: The sample rate if @sr_idx is valid, 0 otherwise.
 */
guint
gst_codec_utils_aac_get_sample_rate_from_index (guint sr_idx)
{
  if (G_LIKELY (sr_idx < G_N_ELEMENTS (aac_sample_rates)))
    return aac_sample_rates[sr_idx];

  GST_WARNING ("Invalid sample rate index %u", sr_idx);
  return 0;
}

/**
 * gst_codec_utils_aac_get_index_from_sample_rate:
 * @rate: Sample rate
 *
 * Translates the sample rate to the index corresponding to it in AAC spec.
 *
 * Returns: The AAC index for this sample rate, -1 if the rate is not a
 * valid AAC sample rate.
 */
gint
gst_codec_utils_aac_get_index_from_sample_rate (guint rate)
{
  guint n;

  for (n = 0; n < G_N_ELEMENTS (aac_sample_rates); n++)
    if (aac_sample_rates[n] == rate)
      return n;

  GST_WARNING ("Invalid sample rate %u", rate);
  return -1;
}

static gboolean
gst_codec_utils_aac_get_audio_object_type (GstBitReader * br,
    guint8 * audio_object_type)
{
  guint8 aot;

  if (!gst_bit_reader_get_bits_uint8 (br, &aot, 5))
    return FALSE;

  if (aot == 31) {
    if (!gst_bit_reader_get_bits_uint8 (br, &aot, 6))
      return FALSE;
    aot += 32;
  }

  *audio_object_type = aot;

  return TRUE;
}

static gboolean
gst_codec_utils_aac_get_audio_sample_rate (GstBitReader * br,
    guint * sample_rate)
{
  guint8 sampling_freq_index;
  guint32 sampling_rate;

  if (!gst_bit_reader_get_bits_uint8 (br, &sampling_freq_index, 4))
    return FALSE;

  if (sampling_freq_index == 0xf) {
    if (!gst_bit_reader_get_bits_uint32 (br, &sampling_rate, 24))
      return FALSE;
  } else {
    sampling_rate =
        gst_codec_utils_aac_get_sample_rate_from_index (sampling_freq_index);
    if (!sampling_rate)
      return FALSE;
  }

  *sample_rate = sampling_rate;

  return TRUE;
}

static gboolean
gst_codec_utils_aac_get_audio_object_type_full (GstBitReader * br,
    guint8 * audio_object_type, guint8 * channel_config, guint * sample_rate)
{
  guint8 aot, channels;
  guint rate;

  if (!gst_codec_utils_aac_get_audio_object_type (br, &aot))
    return FALSE;

  if (!gst_codec_utils_aac_get_audio_sample_rate (br, &rate))
    return FALSE;

  if (!gst_bit_reader_get_bits_uint8 (br, &channels, 4))
    return FALSE;

  /* 5 indicates SBR extension (i.e. HE-AAC) */
  /* 29 indicates PS extension */
  if (aot == 5 || aot == 29) {
    if (!gst_codec_utils_aac_get_audio_sample_rate (br, &rate))
      return FALSE;
    if (!gst_codec_utils_aac_get_audio_object_type (br, &aot))
      return FALSE;
  }

  *audio_object_type = aot;
  *sample_rate = rate;
  *channel_config = channels;

  return TRUE;
}

/**
 * gst_codec_utils_aac_get_sample_rate:
 * @audio_config: (array length=len): a pointer to the AudioSpecificConfig
 *                as specified in the Elementary Stream Descriptor (esds)
 *                in ISO/IEC 14496-1.
 * @len: Length of @audio_config
 *
 * Translates the sample rate index found in AAC headers to the actual sample
 * rate.
 *
 * Returns: The sample rate if sr_idx is valid, 0 otherwise.
 *
 * Since: 1.10
 */
guint
gst_codec_utils_aac_get_sample_rate (const guint8 * audio_config, guint len)
{
  guint sample_rate = 0;
  guint8 audio_object_type = 0, channel_config = 0;
  GstBitReader br = GST_BIT_READER_INIT (audio_config, len);

  if (len < 2)
    return 0;

  gst_codec_utils_aac_get_audio_object_type_full (&br, &audio_object_type,
      &channel_config, &sample_rate);

  return sample_rate;
}

/**
 * gst_codec_utils_aac_get_channels:
 * @audio_config: (array length=len): a pointer to the AudioSpecificConfig
 *                as specified in the Elementary Stream Descriptor (esds)
 *                in ISO/IEC 14496-1.
 * @len: Length of @audio_config in bytes
 *
 * Returns the channels of the given AAC stream.
 *
 * Returns: The channels or 0 if the channel could not be determined.
 *
 * Since: 1.10
 */
guint
gst_codec_utils_aac_get_channels (const guint8 * audio_config, guint len)
{
  guint channels;

  if (len < 2)
    return 0;

  channels = (audio_config[1] & 0x7f) >> 3;
  if (channels > 0 && channels < 7)
    return channels;
  else if (channels == 7)
    return 8;
  else
    return 0;
}

/**
 * gst_codec_utils_aac_get_profile:
 * @audio_config: (array length=len): a pointer to the AudioSpecificConfig
 *                as specified in the Elementary Stream Descriptor (esds)
 *                in ISO/IEC 14496-1.
 * @len: Length of @audio_config in bytes
 *
 * Returns the profile of the given AAC stream as a string. The profile is
 * normally determined using the AudioObjectType field which is in the first
 * 5 bits of @audio_config
 *
 * Returns: (nullable): The profile as a const string and %NULL if the profile could not be
 * determined.
 */
const gchar *
gst_codec_utils_aac_get_profile (const guint8 * audio_config, guint len)
{
  const gchar *profile = NULL;
  guint sample_rate;
  guint8 audio_object_type, channel_config;
  GstBitReader br = GST_BIT_READER_INIT (audio_config, len);

  if (len < 1)
    return NULL;

  GST_MEMDUMP ("audio config", audio_config, len);

  if (!gst_codec_utils_aac_get_audio_object_type_full (&br, &audio_object_type,
          &channel_config, &sample_rate)) {
    return NULL;
  }

  switch (audio_object_type) {
    case 1:
      profile = "main";
      break;
    case 2:
      profile = "lc";
      break;
    case 3:
      profile = "ssr";
      break;
    case 4:
      profile = "ltp";
      break;
    default:
      GST_DEBUG ("Invalid profile idx: %u", audio_object_type);
      break;
  }

  return profile;
}

/**
 * gst_codec_utils_aac_get_level:
 * @audio_config: (array length=len): a pointer to the AudioSpecificConfig
 *                as specified in the Elementary Stream Descriptor (esds)
 *                in ISO/IEC 14496-1.
 * @len: Length of @audio_config in bytes
 *
 * Determines the level of a stream as defined in ISO/IEC 14496-3. For AAC LC
 * streams, the constraints from the AAC audio profile are applied. For AAC
 * Main, LTP, SSR and others, the Main profile is used.
 *
 * The @audio_config parameter follows the following format, starting from the
 * most significant bit of the first byte:
 *
 *   * Bit 0:4 contains the AudioObjectType (if this is 0x5, then the
 *     real AudioObjectType is carried after the rate and channel data)
 *   * Bit 5:8 contains the sample frequency index (if this is 0xf, then the
 *     next 24 bits define the actual sample frequency, and subsequent
 *     fields are appropriately shifted).
 *   * Bit 9:12 contains the channel configuration
 *
 * Returns: (nullable): The level as a const string and %NULL if the level could not be
 * determined.
 */
const gchar *
gst_codec_utils_aac_get_level (const guint8 * audio_config, guint len)
{
  guint8 audio_object_type = 0xFF, channel_config = 0xFF;
  guint rate;
  /* Number of single channel elements, channel pair elements, low frequency
   * elements, independently switched coupling channel elements, and
   * dependently switched coupling channel elements.
   *
   * Note: The 2 CCE types are ignored for now as they require us to actually
   * parse the first frame, and they are rarely found in actual streams.
   */
  int num_sce = 0, num_cpe = 0, num_lfe = 0, num_cce_indep = 0, num_cce_dep = 0;
  int num_channels;
  /* Processor and RAM Complexity Units (calculated and "reference" for single
   * channel) */
  int pcu = -1, rcu = -1, pcu_ref, rcu_ref;
  int ret = -1;
  GstBitReader br = GST_BIT_READER_INIT (audio_config, len);

  g_return_val_if_fail (audio_config != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("audio config", audio_config, len);

  if (!gst_codec_utils_aac_get_audio_object_type_full (&br, &audio_object_type,
          &channel_config, &rate)) {
    return NULL;
  }

  switch (channel_config) {
    case 0:
      /* Channel config is defined in the AudioObjectType's SpecificConfig,
       * which requires some amount of digging through the headers. I only see
       * this done in the MPEG conformance streams - FIXME */
      GST_WARNING ("Found a stream with channel configuration in the "
          "AudioSpecificConfig. Please file a bug with a link to the media if "
          "possible.");
      return NULL;
    case 1:
      /* front center */
      num_sce = 1;
      break;
    case 2:
      /* front left and right */
      num_cpe = 1;
      break;
    case 3:
      /* front left, right, and center */
      num_sce = 1;
      num_cpe = 1;
      break;
    case 4:
      /* front left, right, and center; rear surround */
      num_sce = 2;
      num_cpe = 1;
      break;
    case 5:
      /* front left, right, and center; rear left and right surround */
      num_sce = 1;
      num_cpe = 2;
      break;
    case 6:
      /* front left, right, center and LFE; rear left and right surround */
      num_sce = 1;
      num_cpe = 2;
      break;
    case 7:
    case 12:
    case 14:
      /* front left, right, center and LFE; outside front left and right;
       * rear left and right surround */
      num_sce = 1;
      num_cpe = 3;
      num_lfe = 1;
      break;
    case 11:
      num_sce = 2;
      num_cpe = 2;
      num_lfe = 1;
      break;
    default:
      GST_WARNING ("Unknown channel config in header: %d", channel_config);
      return NULL;
  }

  switch (audio_object_type) {
    case 0:                    /* NULL */
      GST_WARNING ("profile 0 is not a valid profile");
      return NULL;
    case 2:                    /* LC */
      pcu_ref = 3;
      rcu_ref = 3;
      break;
    case 3:                    /* SSR */
      pcu_ref = 4;
      rcu_ref = 3;
      break;
    case 4:                    /* LTP */
      pcu_ref = 4;
      rcu_ref = 4;
      break;
    case 1:                    /* Main */
    default:
      /* Other than a couple of ER profiles, Main is the worst-case */
      pcu_ref = 5;
      rcu_ref = 5;
      break;
  }

  /* "fs_ref" is 48000 Hz for AAC Main/LC/SSR/LTP. SBR's fs_ref is defined as
   * 24000/48000 (in/out), for SBR streams. Actual support is a FIXME */

  pcu = ((float) rate / 48000) * pcu_ref *
      ((2 * num_cpe) + num_sce + num_lfe + num_cce_indep + (0.3 * num_cce_dep));

  rcu = ((float) rcu_ref) * (num_sce + (0.5 * num_lfe) + (0.5 * num_cce_indep) +
      (0.4 * num_cce_dep));

  if (num_cpe < 2)
    rcu += (rcu_ref + (rcu_ref - 1)) * num_cpe;
  else
    rcu += (rcu_ref + (rcu_ref - 1) * ((2 * num_cpe) - 1));

  num_channels = num_sce + (2 * num_cpe);

  if (audio_object_type == 2) {
    /* AAC LC => return the level as per the 'AAC Profile' */
    if (num_channels <= 2 && rate <= 24000 && pcu <= 3 && rcu <= 5)
      ret = 1;
    else if (num_channels <= 2 && rate <= 48000 && pcu <= 6 && rcu <= 5)
      ret = 2;
    /* There is no level 3 for the AAC Profile */
    else if (num_channels <= 5 && rate <= 48000 && pcu <= 19 && rcu <= 15)
      ret = 4;
    else if (num_channels <= 5 && rate <= 96000 && pcu <= 38 && rcu <= 15)
      ret = 5;
    else if (num_channels <= 7 && rate <= 48000 && pcu <= 25 && rcu <= 19)
      ret = 6;
    else if (num_channels <= 7 && rate <= 96000 && pcu <= 50 && rcu <= 19)
      ret = 7;
  } else {
    /* Return the level as per the 'Main Profile' */
    if (pcu < 40 && rcu < 20)
      ret = 1;
    else if (pcu < 80 && rcu < 64)
      ret = 2;
    else if (pcu < 160 && rcu < 128)
      ret = 3;
    else if (pcu < 320 && rcu < 256)
      ret = 4;
  }

  if (ret == -1) {
    GST_WARNING ("couldn't determine level: profile=%u, rate=%u, "
        "channel_config=%u, pcu=%d,rcu=%d", audio_object_type, rate,
        channel_config, pcu, rcu);
    return NULL;
  } else {
    return digit_to_string (ret);
  }
}

/**
 * gst_codec_utils_aac_caps_set_level_and_profile:
 * @caps: the #GstCaps to which level and profile fields are to be added
 * @audio_config: (array length=len): a pointer to the AudioSpecificConfig
 *                as specified in the Elementary Stream Descriptor (esds)
 *                in ISO/IEC 14496-1. (See below for more details)
 * @len: Length of @audio_config in bytes
 *
 * Sets the level and profile on @caps if it can be determined from
 * @audio_config. See gst_codec_utils_aac_get_level() and
 * gst_codec_utils_aac_get_profile() for more details on the parameters.
 * @caps must be audio/mpeg caps with an "mpegversion" field of either 2 or 4.
 * If mpegversion is 4, the "base-profile" field is also set in @caps.
 *
 * Returns: %TRUE if the level and profile could be set, %FALSE otherwise.
 */
gboolean
gst_codec_utils_aac_caps_set_level_and_profile (GstCaps * caps,
    const guint8 * audio_config, guint len)
{
  GstStructure *s;
  const gchar *level, *profile;
  int mpegversion = 0;

  g_return_val_if_fail (GST_IS_CAPS (caps), FALSE);
  g_return_val_if_fail (GST_CAPS_IS_SIMPLE (caps), FALSE);
  g_return_val_if_fail (GST_SIMPLE_CAPS_HAS_NAME (caps, "audio/mpeg"), FALSE);
  g_return_val_if_fail (GST_SIMPLE_CAPS_HAS_FIELD (caps, "mpegversion"), FALSE);
  g_return_val_if_fail (audio_config != NULL, FALSE);

  s = gst_caps_get_structure (caps, 0);

  gst_structure_get_int (s, "mpegversion", &mpegversion);
  g_return_val_if_fail (mpegversion == 2 || mpegversion == 4, FALSE);

  level = gst_codec_utils_aac_get_level (audio_config, len);

  if (level != NULL)
    gst_structure_set (s, "level", G_TYPE_STRING, level, NULL);

  profile = gst_codec_utils_aac_get_profile (audio_config, len);

  if (profile != NULL) {
    if (mpegversion == 4) {
      gst_structure_set (s, "base-profile", G_TYPE_STRING, profile,
          "profile", G_TYPE_STRING, profile, NULL);
    } else {
      gst_structure_set (s, "profile", G_TYPE_STRING, profile, NULL);
    }
  }

  GST_LOG ("profile : %s", (profile) ? profile : "---");
  GST_LOG ("level   : %s", (level) ? level : "---");

  return (level != NULL && profile != NULL);
}

/**
 * gst_codec_utils_h264_get_profile:
 * @sps: (array length=len): Pointer to the sequence parameter set for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the profile indication (profile_idc) in the stream's
 * sequence parameter set into a string. The SPS is expected to have the
 * following format, as defined in the H.264 specification. The SPS is viewed
 * as a bitstream here, with bit 0 being the most significant bit of the first
 * byte.
 *
 * * Bit 0:7   - Profile indication
 * * Bit 8     - constraint_set0_flag
 * * Bit 9     - constraint_set1_flag
 * * Bit 10    - constraint_set2_flag
 * * Bit 11    - constraint_set3_flag
 * * Bit 12    - constraint_set3_flag
 * * Bit 13:15 - Reserved
 * * Bit 16:24 - Level indication
 *
 * Returns: (nullable): The profile as a const string, or %NULL if there is an error.
 */
const gchar *
gst_codec_utils_h264_get_profile (const guint8 * sps, guint len)
{
  const gchar *profile = NULL;
  gint csf1, csf3, csf4, csf5;

  g_return_val_if_fail (sps != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("SPS", sps, len);

  csf1 = (sps[1] & 0x40) >> 6;
  csf3 = (sps[1] & 0x10) >> 4;
  csf4 = (sps[1] & 0x08) >> 3;
  csf5 = (sps[1] & 0x04) >> 2;

  switch (sps[0]) {
    case 66:
      if (csf1)
        profile = "constrained-baseline";
      else
        profile = "baseline";
      break;
    case 77:
      profile = "main";
      break;
    case 88:
      profile = "extended";
      break;
    case 100:
      if (csf4) {
        if (csf5)
          profile = "constrained-high";
        else
          profile = "progressive-high";
      } else
        profile = "high";
      break;
    case 110:
      if (csf3)
        profile = "high-10-intra";
      else if (csf4)
        profile = "progressive-high-10";
      else
        profile = "high-10";
      break;
    case 122:
      if (csf3)
        profile = "high-4:2:2-intra";
      else
        profile = "high-4:2:2";
      break;
    case 244:
      if (csf3)
        profile = "high-4:4:4-intra";
      else
        profile = "high-4:4:4";
      break;
    case 44:
      profile = "cavlc-4:4:4-intra";
      break;
    case 118:
      profile = "multiview-high";
      break;
    case 128:
      profile = "stereo-high";
      break;
    case 83:
      if (csf5)
        profile = "scalable-constrained-baseline";
      else
        profile = "scalable-baseline";
      break;
    case 86:
      if (csf3)
        profile = "scalable-high-intra";
      else if (csf5)
        profile = "scalable-constrained-high";
      else
        profile = "scalable-high";
      break;
    default:
      return NULL;
  }

  return profile;
}

/**
 * gst_codec_utils_h264_get_level:
 * @sps: (array length=len): Pointer to the sequence parameter set for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the level indication (level_idc) in the stream's
 * sequence parameter set into a string. The SPS is expected to have the
 * same format as for gst_codec_utils_h264_get_profile().
 *
 * Returns: (nullable): The level as a const string, or %NULL if there is an error.
 */
const gchar *
gst_codec_utils_h264_get_level (const guint8 * sps, guint len)
{
  gint csf3;

  g_return_val_if_fail (sps != NULL, NULL);

  if (len < 3)
    return NULL;

  GST_MEMDUMP ("SPS", sps, len);

  csf3 = (sps[1] & 0x10) >> 4;

  if (sps[2] == 0)
    return NULL;
  else if ((sps[2] == 11 && csf3) || sps[2] == 9)
    return "1b";
  else if (sps[2] % 10 == 0)
    return digit_to_string (sps[2] / 10);
  else {
    switch (sps[2]) {
      case 11:
        return "1.1";
      case 12:
        return "1.2";
      case 13:
        return "1.3";
      case 21:
        return "2.1";
      case 22:
        return "2.2";
      case 31:
        return "3.1";
      case 32:
        return "3.2";
      case 41:
        return "4.1";
      case 42:
        return "4.2";
      case 51:
        return "5.1";
      case 52:
        return "5.2";
      case 61:
        return "6.1";
      case 62:
        return "6.2";
      default:
        return NULL;
    }
  }
}

/**
 * gst_codec_utils_h264_get_level_idc:
 * @level: A level string from caps
 *
 * Transform a level string from the caps into the level_idc
 *
 * Returns: the level_idc or 0 if the level is unknown
 */
guint8
gst_codec_utils_h264_get_level_idc (const gchar * level)
{
  g_return_val_if_fail (level != NULL, 0);

  if (!strcmp (level, "1"))
    return 10;
  else if (!strcmp (level, "1b"))
    return 9;
  else if (!strcmp (level, "1.1"))
    return 11;
  else if (!strcmp (level, "1.2"))
    return 12;
  else if (!strcmp (level, "1.3"))
    return 13;
  else if (!strcmp (level, "2"))
    return 20;
  else if (!strcmp (level, "2.1"))
    return 21;
  else if (!strcmp (level, "2.2"))
    return 22;
  else if (!strcmp (level, "3"))
    return 30;
  else if (!strcmp (level, "3.1"))
    return 31;
  else if (!strcmp (level, "3.2"))
    return 32;
  else if (!strcmp (level, "4"))
    return 40;
  else if (!strcmp (level, "4.1"))
    return 41;
  else if (!strcmp (level, "4.2"))
    return 42;
  else if (!strcmp (level, "5"))
    return 50;
  else if (!strcmp (level, "5.1"))
    return 51;
  else if (!strcmp (level, "5.2"))
    return 52;
  else if (!strcmp (level, "6"))
    return 60;
  else if (!strcmp (level, "6.1"))
    return 61;
  else if (!strcmp (level, "6.2"))
    return 62;

  GST_WARNING ("Invalid level %s", level);
  return 0;
}

/**
 * gst_codec_utils_h264_caps_set_level_and_profile:
 * @caps: the #GstCaps to which the level and profile are to be added
 * @sps: (array length=len): Pointer to the sequence parameter set for the stream.
 * @len: Length of the data available in @sps.
 *
 * Sets the level and profile in @caps if it can be determined from @sps. See
 * gst_codec_utils_h264_get_level() and gst_codec_utils_h264_get_profile()
 * for more details on the parameters.
 *
 * Returns: %TRUE if the level and profile could be set, %FALSE otherwise.
 */
gboolean
gst_codec_utils_h264_caps_set_level_and_profile (GstCaps * caps,
    const guint8 * sps, guint len)
{
  const gchar *level, *profile;

  g_return_val_if_fail (GST_IS_CAPS (caps), FALSE);
  g_return_val_if_fail (GST_CAPS_IS_SIMPLE (caps), FALSE);
  g_return_val_if_fail (GST_SIMPLE_CAPS_HAS_NAME (caps, "video/x-h264"), FALSE);
  g_return_val_if_fail (sps != NULL, FALSE);

  level = gst_codec_utils_h264_get_level (sps, len);

  if (level != NULL)
    gst_caps_set_simple (caps, "level", G_TYPE_STRING, level, NULL);

  profile = gst_codec_utils_h264_get_profile (sps, len);

  if (profile != NULL)
    gst_caps_set_simple (caps, "profile", G_TYPE_STRING, profile, NULL);

  GST_LOG ("profile : %s", (profile) ? profile : "---");
  GST_LOG ("level   : %s", (level) ? level : "---");

  return (level != NULL && profile != NULL);
}

/**
 * gst_codec_utils_h264_get_profile_flags_level:
 * @codec_data: (array length=len): H264 AVCC extradata
 * @len: length of @codec_data
 * @profile: (optional) (out): return location for h264 profile_idc or %NULL
 * @flags: (optional) (out): return location for h264 constraint set flags or %NULL
 * @level: (optional) (out): return location h264 level_idc or %NULL
 *
 * Parses profile, flags, and level from a H264 AVCC extradata/sequence_header.
 * These are most commonly retrieved from a video/x-h264 caps with a codec_data
 * buffer.
 *
 * The format of H264 AVCC extradata/sequence_header is documented in the
 * ITU-T H.264 specification section 7.3.2.1.1 as well as in ISO/IEC 14496-15
 * section 5.3.3.1.2.
 *
 * Returns: %TRUE on success, %FALSE on failure
 *
 * Since: 1.20
 */
gboolean
gst_codec_utils_h264_get_profile_flags_level (const guint8 * codec_data,
    guint len, guint8 * profile, guint8 * flags, guint8 * level)
{
  gboolean ret = FALSE;

  g_return_val_if_fail (codec_data != NULL, FALSE);

  if (len < 7) {
    GST_WARNING ("avc codec data is too small");
    goto done;
  }
  if (codec_data[0] != 1) {
    GST_WARNING ("failed to parse avc codec version, must be 1");
    goto done;
  }

  if (profile) {
    *profile = codec_data[1];
  }
  if (flags) {
    *flags = codec_data[2];
  }
  if (level) {
    *level = codec_data[3];
  }

  ret = TRUE;

done:
  return ret;
}

/* forked from gsth265parse.c */
typedef struct
{
  const gchar *profile;

  guint8 max_14bit_constraint_flag;
  guint8 max_12bit_constraint_flag;
  guint8 max_10bit_constraint_flag;
  guint8 max_8bit_constraint_flag;
  guint8 max_422chroma_constraint_flag;
  guint8 max_420chroma_constraint_flag;
  guint8 max_monochrome_constraint_flag;
  guint8 intra_constraint_flag;
  guint8 one_picture_only_constraint_flag;
  guint8 lower_bit_rate_constraint_flag;

  /* Tie breaker if more than one profiles are matching */
  guint priority;
} GstH265ExtensionProfile;

typedef struct
{
  const GstH265ExtensionProfile *profile;
  guint extra_constraints;
} H265ExtensionProfileMatch;

static gint
sort_fre_profile_matches (H265ExtensionProfileMatch * a,
    H265ExtensionProfileMatch * b)
{
  gint d;

  d = a->extra_constraints - b->extra_constraints;
  if (d)
    return d;

  return b->profile->priority - a->profile->priority;
}

static const gchar *
utils_get_extension_profile (const GstH265ExtensionProfile * profiles,
    guint num, GstH265ExtensionProfile * ext_profile)
{
  guint i;
  const gchar *profile = NULL;
  GList *cand = NULL;

  for (i = 0; i < num; i++) {
    GstH265ExtensionProfile p = profiles[i];
    guint extra_constraints = 0;
    H265ExtensionProfileMatch *m;

    /* Filter out all the profiles having constraints not satisfied by
     * @ext_profile.
     * Then pick the one having the least extra constraints. This allow us
     * to match the closet profile if bitstream contains not standard
     * constraints. */
    if (p.max_14bit_constraint_flag != ext_profile->max_14bit_constraint_flag) {
      if (p.max_14bit_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.max_12bit_constraint_flag != ext_profile->max_12bit_constraint_flag) {
      if (p.max_12bit_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.max_10bit_constraint_flag != ext_profile->max_10bit_constraint_flag) {
      if (p.max_10bit_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.max_8bit_constraint_flag != ext_profile->max_8bit_constraint_flag) {
      if (p.max_8bit_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.max_422chroma_constraint_flag !=
        ext_profile->max_422chroma_constraint_flag) {
      if (p.max_422chroma_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.max_420chroma_constraint_flag !=
        ext_profile->max_420chroma_constraint_flag) {
      if (p.max_420chroma_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.max_monochrome_constraint_flag !=
        ext_profile->max_monochrome_constraint_flag) {
      if (p.max_monochrome_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.intra_constraint_flag != ext_profile->intra_constraint_flag) {
      if (p.intra_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.one_picture_only_constraint_flag !=
        ext_profile->one_picture_only_constraint_flag) {
      if (p.one_picture_only_constraint_flag)
        continue;
      extra_constraints++;
    }

    if (p.lower_bit_rate_constraint_flag
        && !ext_profile->lower_bit_rate_constraint_flag)
      continue;

    /* choose this one if all flags are matched */
    if (extra_constraints == 0) {
      profile = p.profile;
      break;
    }

    m = g_new0 (H265ExtensionProfileMatch, 1);
    m->profile = &profiles[i];
    m->extra_constraints = extra_constraints;
    cand = g_list_prepend (cand, m);
  }

  if (!profile && cand) {
    H265ExtensionProfileMatch *m;

    cand = g_list_sort (cand, (GCompareFunc) sort_fre_profile_matches);
    m = cand->data;
    profile = m->profile->profile;
  }

  if (cand)
    g_list_free_full (cand, g_free);

  return profile;
}

static const gchar *
utils_get_format_range_extension_profile (GstH265ExtensionProfile * ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* FIXME 2.0: Consider ':' separated subsampling notation for consistency
     * https://gitlab.freedesktop.org/gstreamer/gst-plugins-base/merge_requests/23
     */
    /* Rec. ITU-T H.265 Table A.2 format range extensions profiles */
    /* *INDENT-OFF* */
    {"monochrome",                    0, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0},
    {"monochrome-10",                 0, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1},
    {"monochrome-12",                 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 2},
    {"monochrome-16",                 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 3},
    {"main-12",                       0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 4},
    {"main-422-10",                   0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 5},
    {"main-422-12",                   0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 6},
    {"main-444",                      0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 7},
    {"main-444-10",                   0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 8},
    {"main-444-12",                   0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 9},
    {"main-intra",                    0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 10},
    {"main-10-intra",                 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 11},
    {"main-12-intra",                 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 12},
    {"main-422-10-intra",             0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 13},
    {"main-422-12-intra",             0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 14},
    {"main-444-intra",                0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 15},
    {"main-444-10-intra",             0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 16},
    {"main-444-12-intra",             0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 17},
    {"main-444-16-intra",             0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 18},
    {"main-444-still-picture",        0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 19},
    {"main-444-16-still-picture",     0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 20},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

static const gchar *
utils_get_3d_profile (GstH265ExtensionProfile * ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* Rec. ITU-T H.265 I.11.1 3D Main profile */
    /* *INDENT-OFF* */
    {"3d-main",                       0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

static const gchar *
utils_get_multiview_profile (GstH265ExtensionProfile * ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* Rec. ITU-T H.265 G.11.1 Multiview Main profile */
    /* *INDENT-OFF* */
    {"multiview-main",                0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

static const gchar *
utils_get_scalable_profile (GstH265ExtensionProfile * ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* Rec. ITU-T H.265 H.11.1 */
    /* *INDENT-OFF* */
    {"scalable-main",                 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0},
    {"scalable-main-10",              0, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

static const gchar *
utils_get_high_throughput_profile (GstH265ExtensionProfile * ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* Rec. ITU-T H.265 Table A.3 high throughput profiles */
    /* *INDENT-OFF* */
    {"high-throughput-444",           1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0},
    {"high-throughput-444-10",        1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1},
    {"high-throughput-444-14",        1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2},
    {"high-throughput-444-16-intra",  0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 3},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

static const gchar *
utils_get_screen_content_coding_extensions_profile (GstH265ExtensionProfile *
    ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* Rec. ITU-T H.265 Table A.5 screen content coding extensions profiles */
    /* *INDENT-OFF* */
    {"screen-extended-main",          1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0},
    {"screen-extended-main-10",       1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1},
    {"screen-extended-main-444",      1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 2},
    {"screen-extended-main-444-10",   1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 3},
    /* identical to screen-extended-main-444 */
    {"screen-extended-high-throughput-444",
                                      1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 4},
    /* identical to screen-extended-main-444-10 */
    {"screen-extended-high-throughput-444-10",
                                      1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 5},
    {"screen-extended-high-throughput-444-14",
                                      1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 6},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

static const gchar *
utils_get_scalable_format_range_extensions_profile (GstH265ExtensionProfile *
    ext_profile)
{
  static const GstH265ExtensionProfile profiles[] = {
    /* Rec. ITU-T H.265 Table H.4 scalable range extensions profiles */
    /* *INDENT-OFF* */
    {"scalable-monochrome",           1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0},
    {"scalable-monochrome-12",        1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1},
    {"scalable-monochrome-16",        0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 2},
    {"scalable-main-444",             1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 3},
    /* *INDENT-ON* */
  };

  return utils_get_extension_profile (profiles, G_N_ELEMENTS (profiles),
      ext_profile);
}

/**
 * gst_codec_utils_h265_get_profile:
 * @profile_tier_level: (array length=len): Pointer to the profile_tier_level
 *   structure for the stream.
 * @len: Length of the data available in @profile_tier_level
 *
 * Converts the profile indication (general_profile_idc) in the stream's
 * profile_level_tier structure into a string. The profile_tier_level is
 * expected to have the following format, as defined in the H.265
 * specification. The profile_tier_level is viewed as a bitstream here,
 * with bit 0 being the most significant bit of the first byte.
 *
 * * Bit 0:1   - general_profile_space
 * * Bit 2     - general_tier_flag
 * * Bit 3:7   - general_profile_idc
 * * Bit 8:39  - gernal_profile_compatibility_flags
 * * Bit 40    - general_progressive_source_flag
 * * Bit 41    - general_interlaced_source_flag
 * * Bit 42    - general_non_packed_constraint_flag
 * * Bit 43    - general_frame_only_constraint_flag
 * * Bit 44:87 - See below
 * * Bit 88:95 - general_level_idc
 *
 * Returns: (nullable): The profile as a const string, or %NULL if there is an error.
 *
 * Since: 1.4
 */
const gchar *
gst_codec_utils_h265_get_profile (const guint8 * profile_tier_level, guint len)
{
  const gchar *profile = NULL;
  gint profile_idc;
  guint i;
  guint8 profile_compatibility_flags[32] = { 0, };
  GstBitReader br = GST_BIT_READER_INIT (profile_tier_level, len);

  g_return_val_if_fail (profile_tier_level != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("ProfileTierLevel", profile_tier_level, len);

  profile_idc = (profile_tier_level[0] & 0x1f);

  if (profile_idc == 1)
    profile = "main";
  else if (profile_idc == 2)
    profile = "main-10";
  else if (profile_idc == 3)
    profile = "main-still-picture";

  if (len > 4) {
    if (!gst_bit_reader_skip (&br, 8))
      return NULL;

    for (i = 0; i < 32; i++) {
      if (!gst_bit_reader_get_bits_uint8 (&br, &profile_compatibility_flags[i],
              1))
        return NULL;
    }
  }

  if (!profile) {
    if (profile_compatibility_flags[1])
      profile = "main";
    else if (profile_compatibility_flags[2])
      profile = "main-10";
    else if (profile_compatibility_flags[3])
      profile = "main-still-picture";
  }

  if (profile)
    return profile;

  if (profile_idc >= 4 && profile_idc <= 11 && len >= 11) {
    GstH265ExtensionProfile ext_profile = { 0, };

    /*
     * Bit 40 - general_progressive_source_flag
     * Bit 41 - general_interlaced_source_flag
     * Bit 42 - general_non_packed_constraint_flag
     * Bit 43 - general_frame_only_constraint_flag
     */
    if (!gst_bit_reader_skip (&br, 4))
      return NULL;

    /* Range extensions
     * profile_idc
     *   4 : Format range extensions profiles
     *   5 : High throughput profiles
     *   6 : Multiview main profile
     *   7 : Scalable main profiles
     *   8 : 3D Main profile
     *   9 : Screen content coding extensions profiles
     *  10 : Scalable format range extensions profiles
     *
     * Bit 44 - general_max_12bit_constraint_flag
     * Bit 45 - general_max_10bit_constraint_flag
     * Bit 46 - general_max_8bit_constraint_flag
     * Bit 47 - general_max_422chroma_constraint_flag
     * Bit 48 - general_max_420chroma_constraint_flag
     * Bit 49 - general_max_monochrome_constraint_flag
     * Bit 50 - general_intra_constraint_flag
     * Bit 51 - general_one_picture_only_constraint_flag
     * Bit 52 - general_lower_bit_rate_constraint_flag
     */
    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.max_12bit_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.max_10bit_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.max_8bit_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.max_422chroma_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.max_420chroma_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.max_monochrome_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.intra_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.one_picture_only_constraint_flag, 1))
      return NULL;

    if (!gst_bit_reader_get_bits_uint8 (&br,
            &ext_profile.lower_bit_rate_constraint_flag, 1))
      return NULL;

    if (profile_idc == 5 || profile_idc == 9 ||
        profile_idc == 10 || profile_idc == 11 ||
        profile_compatibility_flags[5] || profile_compatibility_flags[9] ||
        profile_compatibility_flags[10] || profile_compatibility_flags[11]) {
      /* Bit 53 - general_max_14bit_constraint_flag */
      if (!gst_bit_reader_get_bits_uint8 (&br,
              &ext_profile.max_14bit_constraint_flag, 1))
        return NULL;
    }

    if (profile_idc == 4 || profile_compatibility_flags[4])
      return utils_get_format_range_extension_profile (&ext_profile);

    if (profile_idc == 5 || profile_compatibility_flags[5])
      return utils_get_high_throughput_profile (&ext_profile);

    if (profile_idc == 6 || profile_compatibility_flags[6])
      return utils_get_multiview_profile (&ext_profile);

    if (profile_idc == 7 || profile_compatibility_flags[7])
      return utils_get_scalable_profile (&ext_profile);

    if (profile_idc == 8 || profile_compatibility_flags[8])
      return utils_get_3d_profile (&ext_profile);

    if (profile_idc == 9 || profile_compatibility_flags[9] ||
        profile_idc == 11 || profile_compatibility_flags[11])
      return utils_get_screen_content_coding_extensions_profile (&ext_profile);

    if (profile_idc == 10 || profile_compatibility_flags[10])
      return utils_get_scalable_format_range_extensions_profile (&ext_profile);
  }

  return profile;
}

/**
 * gst_codec_utils_h265_get_tier:
 * @profile_tier_level: (array length=len): Pointer to the profile_tier_level
 *   for the stream.
 * @len: Length of the data available in @profile_tier_level.
 *
 * Converts the tier indication (general_tier_flag) in the stream's
 * profile_tier_level structure into a string. The profile_tier_level
 * is expected to have the same format as for gst_codec_utils_h264_get_profile().
 *
 * Returns: (nullable): The tier as a const string, or %NULL if there is an error.
 *
 * Since: 1.4
 */
const gchar *
gst_codec_utils_h265_get_tier (const guint8 * profile_tier_level, guint len)
{
  const gchar *tier = NULL;
  gint tier_flag = 0;

  g_return_val_if_fail (profile_tier_level != NULL, NULL);

  if (len < 1)
    return NULL;

  GST_MEMDUMP ("ProfileTierLevel", profile_tier_level, len);

  tier_flag = (profile_tier_level[0] & 0x20) >> 5;

  if (tier_flag)
    tier = "high";
  else
    tier = "main";

  return tier;
}

/**
 * gst_codec_utils_h265_get_level:
 * @profile_tier_level: (array length=len): Pointer to the profile_tier_level
 *   for the stream
 * @len: Length of the data available in @profile_tier_level.
 *
 * Converts the level indication (general_level_idc) in the stream's
 * profile_tier_level structure into a string. The profiel_tier_level is
 * expected to have the same format as for gst_codec_utils_h264_get_profile().
 *
 * Returns: (nullable): The level as a const string, or %NULL if there is an error.
 *
 * Since: 1.4
 */
const gchar *
gst_codec_utils_h265_get_level (const guint8 * profile_tier_level, guint len)
{
  g_return_val_if_fail (profile_tier_level != NULL, NULL);

  if (len < 12)
    return NULL;

  GST_MEMDUMP ("ProfileTierLevel", profile_tier_level, len);

  if (profile_tier_level[11] == 0)
    return NULL;
  else if (profile_tier_level[11] % 30 == 0)
    return digit_to_string (profile_tier_level[11] / 30);
  else {
    switch (profile_tier_level[11]) {
      case 63:
        return "2.1";
        break;
      case 93:
        return "3.1";
        break;
      case 123:
        return "4.1";
        break;
      case 153:
        return "5.1";
        break;
      case 156:
        return "5.2";
        break;
      case 183:
        return "6.1";
        break;
      case 186:
        return "6.2";
        break;
      default:
        return NULL;
    }
  }
}

/**
 * gst_codec_utils_h265_get_level_idc:
 * @level: A level string from caps
 *
 * Transform a level string from the caps into the level_idc
 *
 * Returns: the level_idc or 0 if the level is unknown
 *
 * Since: 1.4
 */
guint8
gst_codec_utils_h265_get_level_idc (const gchar * level)
{
  g_return_val_if_fail (level != NULL, 0);

  if (!strcmp (level, "1"))
    return 30;
  else if (!strcmp (level, "2"))
    return 60;
  else if (!strcmp (level, "2.1"))
    return 63;
  else if (!strcmp (level, "3"))
    return 90;
  else if (!strcmp (level, "3.1"))
    return 93;
  else if (!strcmp (level, "4"))
    return 120;
  else if (!strcmp (level, "4.1"))
    return 123;
  else if (!strcmp (level, "5"))
    return 150;
  else if (!strcmp (level, "5.1"))
    return 153;
  else if (!strcmp (level, "5.2"))
    return 156;
  else if (!strcmp (level, "6"))
    return 180;
  else if (!strcmp (level, "6.1"))
    return 183;
  else if (!strcmp (level, "6.2"))
    return 186;

  GST_WARNING ("Invalid level %s", level);
  return 0;
}

/**
 * gst_codec_utils_h265_caps_set_level_tier_and_profile:
 * @caps: the #GstCaps to which the level, tier and profile are to be added
 * @profile_tier_level: (array length=len): Pointer to the profile_tier_level
 *   struct
 * @len: Length of the data available in @profile_tier_level.
 *
 * Sets the level, tier and profile in @caps if it can be determined from
 * @profile_tier_level. See gst_codec_utils_h265_get_level(),
 * gst_codec_utils_h265_get_tier() and gst_codec_utils_h265_get_profile()
 * for more details on the parameters.
 *
 * Returns: %TRUE if the level, tier, profile could be set, %FALSE otherwise.
 *
 * Since: 1.4
 */
gboolean
gst_codec_utils_h265_caps_set_level_tier_and_profile (GstCaps * caps,
    const guint8 * profile_tier_level, guint len)
{
  const gchar *level, *tier, *profile;

  g_return_val_if_fail (GST_IS_CAPS (caps), FALSE);
  g_return_val_if_fail (GST_CAPS_IS_SIMPLE (caps), FALSE);
  g_return_val_if_fail (GST_SIMPLE_CAPS_HAS_NAME (caps, "video/x-h265"), FALSE);
  g_return_val_if_fail (profile_tier_level != NULL, FALSE);

  level = gst_codec_utils_h265_get_level (profile_tier_level, len);
  if (level != NULL)
    gst_caps_set_simple (caps, "level", G_TYPE_STRING, level, NULL);

  tier = gst_codec_utils_h265_get_tier (profile_tier_level, len);
  if (tier != NULL)
    gst_caps_set_simple (caps, "tier", G_TYPE_STRING, tier, NULL);

  profile = gst_codec_utils_h265_get_profile (profile_tier_level, len);
  if (profile != NULL)
    gst_caps_set_simple (caps, "profile", G_TYPE_STRING, profile, NULL);

  GST_LOG ("profile : %s", (profile) ? profile : "---");
  GST_LOG ("tier    : %s", (tier) ? tier : "---");
  GST_LOG ("level   : %s", (level) ? level : "---");

  return (level != NULL && tier != NULL && profile != NULL);
}

/**
 * gst_codec_utils_h266_get_profile:
 * @ptl_record: (array length=len): Pointer to the VvcPTLRecord structure as defined in ISO/IEC 14496-15.
 * @len: Length of the data available in @ptl_record
 *
 * Converts the profile indication (general_profile_idc) in the stream's
 * ptl_record structure into a string.
 *
 * Returns: (nullable): The profile as a const string, or %NULL if there is an error.
 *
 * Since: 1.26
 */
const gchar *
gst_codec_utils_h266_get_profile (const guint8 * ptl_record, guint len)
{
  gint profile_idc;

  g_return_val_if_fail (ptl_record != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("VvcPTLRecord", ptl_record, len);

  profile_idc = (ptl_record[1] & 0xFE) >> 1;

  if (!profile_idc)
    return NULL;

  switch (profile_idc) {
    case 1:
      return "main-10";
      break;
    case 2:
      return "main-12";
      break;
    case 10:
      return "main-12-intra";
      break;
    case 17:
      return "multilayer-main-10";
      break;
    case 33:
      return "main-444-10";
      break;
    case 34:
      return "main-444-12";
      break;
    case 35:
      return "main-444-16";
      break;
    case 42:
      return "main-444-12-intra";
      break;
    case 43:
      return "main-444-16-intra";
      break;
    case 49:
      return "multilayer-main-444-10";
      break;
    case 65:
      return "main-10-still-picture";
      break;
    case 66:
      return "main-12-still-picture";
      break;
    case 97:
      return "main-444-10-still-picture";
      break;
    case 98:
      return "main-444-12-still-picture";
      break;
    case 99:
      return "main-444-16-still-picture";
      break;
    default:
      return NULL;
  }
}

/**
 * gst_codec_utils_h266_get_tier:
 * @ptl_record: (array length=len): Pointer to the VvcPTLRecord structure as defined in ISO/IEC 14496-15.
 * @len: Length of the data available in @ptl_record.
 *
 * Converts the tier indication (general_tier_flag) in the stream's
 * ptl_record structure into a string.
 *
 * Returns: (nullable): The tier as a const string, or %NULL if there is an error.
 *
 * Since: 1.26
 */
const gchar *
gst_codec_utils_h266_get_tier (const guint8 * ptl_record, guint len)
{
  const gchar *tier = NULL;
  gint tier_flag = 0;

  g_return_val_if_fail (ptl_record != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("VvcPTLRecord", ptl_record, len);

  tier_flag = ptl_record[1] & 0x01;

  if (tier_flag)
    tier = "high";
  else
    tier = "main";

  return tier;
}

/**
 * gst_codec_utils_h266_get_level:
 * @ptl_record: (array length=len): Pointer to the VvcPTLRecord structure as defined in ISO/IEC 14496-15.
 * @len: Length of the data available in @ptl_record.
 *
 * Converts the level indication (general_level_idc) in the stream's
 * ptl_record structure into a string.
 *
 * Returns: (nullable): The level as a const string, or %NULL if there is an error.
 *
 * Since: 1.26
 */
const gchar *
gst_codec_utils_h266_get_level (const guint8 * ptl_record, guint len)
{
  guint8 level_idc;

  g_return_val_if_fail (ptl_record != NULL, NULL);

  if (len < 3)
    return NULL;

  GST_MEMDUMP ("VvcPTLRecord", ptl_record, len);

  level_idc = ptl_record[2];

  if (!level_idc)
    return NULL;

  switch (level_idc) {
    case 16:
      return "1";
      break;
    case 32:
      return "2";
      break;
    case 35:
      return "2.1";
      break;
    case 48:
      return "3";
      break;
    case 51:
      return "3.1";
      break;
    case 64:
      return "4";
      break;
    case 67:
      return "4.1";
      break;
    case 80:
      return "5";
      break;
    case 83:
      return "5.1";
      break;
    case 86:
      return "5.2";
      break;
    case 96:
      return "6";
      break;
    case 99:
      return "6.1";
      break;
    case 102:
      return "6.2";
      break;
    case 105:
      return "6.3";
      break;
    default:
      return NULL;
  }
}

/**
 * gst_codec_utils_h266_get_level_idc:
 * @level: A level string from caps
 *
 * Transform a level string from the caps into the level_idc
 *
 * Returns: the level_idc or 0 if the level is unknown
 *
 * Since: 1.26
 */
guint8
gst_codec_utils_h266_get_level_idc (const gchar * level)
{
  g_return_val_if_fail (level != NULL, 0);

  if (!strcmp (level, "1"))
    return 16;
  else if (!strcmp (level, "2"))
    return 32;
  else if (!strcmp (level, "2.1"))
    return 35;
  else if (!strcmp (level, "3"))
    return 48;
  else if (!strcmp (level, "3.1"))
    return 51;
  else if (!strcmp (level, "4"))
    return 64;
  else if (!strcmp (level, "4.1"))
    return 67;
  else if (!strcmp (level, "5"))
    return 80;
  else if (!strcmp (level, "5.1"))
    return 83;
  else if (!strcmp (level, "5.2"))
    return 86;
  else if (!strcmp (level, "6"))
    return 96;
  else if (!strcmp (level, "6.1"))
    return 99;
  else if (!strcmp (level, "6.2"))
    return 102;
  else if (!strcmp (level, "6.3"))
    return 105;


  GST_WARNING ("Invalid level %s", level);
  return 0;
}

/**
 * gst_codec_utils_h266_caps_set_level_tier_and_profile:
 * @caps: the #GstCaps to which the level, tier and profile are to be added
 * @decoder_configuration: (array length=len): Pointer to the VvcDecoderConfigurationRecord struct as defined in ISO/IEC 14496-15
 * @len: Length of the data available in @decoder_configuration.
 *
 * Sets the level, tier and profile in @caps if it can be determined from
 * @decoder_configuration. See gst_codec_utils_h266_get_level(),
 * gst_codec_utils_h266_get_tier() and gst_codec_utils_h266_get_profile()
 * for more details on the parameters.
 *
 * Returns: %TRUE if the level, tier, profile could be set, %FALSE otherwise.
 *
 * Since: 1.26
 */
gboolean
gst_codec_utils_h266_caps_set_level_tier_and_profile (GstCaps * caps,
    const guint8 * decoder_configuration, guint len)
{
  const gchar *level, *tier, *profile;
  gboolean ptl_present_flag;
  const guint8 *ptl_record;

  g_return_val_if_fail (GST_IS_CAPS (caps), FALSE);
  g_return_val_if_fail (GST_CAPS_IS_SIMPLE (caps), FALSE);
  g_return_val_if_fail (GST_SIMPLE_CAPS_HAS_NAME (caps, "video/x-h266"), FALSE);
  g_return_val_if_fail (decoder_configuration != NULL, FALSE);

  if (len < 5)
    return FALSE;

  ptl_present_flag = decoder_configuration[0] & 0x01;
  if (!ptl_present_flag)
    return FALSE;

  ptl_record = decoder_configuration + 4;
  len -= 4;

  level = gst_codec_utils_h266_get_level (ptl_record, len);
  if (level != NULL)
    gst_caps_set_simple (caps, "level", G_TYPE_STRING, level, NULL);

  tier = gst_codec_utils_h266_get_tier (ptl_record, len);
  if (tier != NULL)
    gst_caps_set_simple (caps, "tier", G_TYPE_STRING, tier, NULL);

  profile = gst_codec_utils_h266_get_profile (ptl_record, len);
  if (profile != NULL)
    gst_caps_set_simple (caps, "profile", G_TYPE_STRING, profile, NULL);

  GST_LOG ("profile : %s", (profile) ? profile : "---");
  GST_LOG ("tier    : %s", (tier) ? tier : "---");
  GST_LOG ("level   : %s", (level) ? level : "---");

  return (level != NULL && tier != NULL && profile != NULL);
}

/**
 * gst_codec_utils_av1_get_seq_level_idx:
 * @level: A level string from caps
 *
 * Transform a level string from the caps into the seq_level_idx
 *
 * Returns: the seq_level_idx or 31 (max-level) if the level is unknown
 *
 * Since: 1.26
 */
guint8
gst_codec_utils_av1_get_seq_level_idx (const gchar * level)
{
  g_return_val_if_fail (level != NULL, 0);

  if (!strcmp (level, "2.0"))
    return 0;
  else if (!strcmp (level, "2.1"))
    return 1;
  else if (!strcmp (level, "2.2"))
    return 2;
  else if (!strcmp (level, "2.3"))
    return 3;
  else if (!strcmp (level, "3.0"))
    return 4;
  else if (!strcmp (level, "3.1"))
    return 5;
  else if (!strcmp (level, "3.2"))
    return 6;
  else if (!strcmp (level, "3.3"))
    return 7;
  else if (!strcmp (level, "4.0"))
    return 8;
  else if (!strcmp (level, "4.1"))
    return 9;
  else if (!strcmp (level, "4.2"))
    return 10;
  else if (!strcmp (level, "4.3"))
    return 11;
  else if (!strcmp (level, "5.0"))
    return 12;
  else if (!strcmp (level, "5.1"))
    return 13;
  else if (!strcmp (level, "5.2"))
    return 14;
  else if (!strcmp (level, "5.3"))
    return 15;
  else if (!strcmp (level, "6.0"))
    return 16;
  else if (!strcmp (level, "6.1"))
    return 17;
  else if (!strcmp (level, "6.2"))
    return 18;
  else if (!strcmp (level, "6.3"))
    return 19;
  else if (!strcmp (level, "7.0"))
    return 20;
  else if (!strcmp (level, "7.1"))
    return 21;
  else if (!strcmp (level, "7.2"))
    return 22;
  else if (!strcmp (level, "7.3"))
    return 23;

  GST_WARNING ("Invalid level %s", level);
  return 31;
}

/**
 * gst_codec_utils_av1_get_level:
 * @seq_level_idx: A seq_level_idx
 *
 * Transform a seq_level_idx into the level string
 *
 * Returns: (nullable): the level string or %NULL if the seq_level_idx is unknown
 *
 * Since: 1.26
 */
const gchar *
gst_codec_utils_av1_get_level (guint8 seq_level_idx)
{
  switch (seq_level_idx) {
    case 0:
      return "2.0";
    case 1:
      return "2.1";
    case 2:
      return "2.2";
    case 3:
      return "2.3";
    case 4:
      return "3.0";
    case 5:
      return "3.1";
    case 6:
      return "3.2";
    case 7:
      return "3.3";
    case 8:
      return "4.0";
    case 9:
      return "4.1";
    case 10:
      return "4.2";
    case 11:
      return "4.3";
    case 12:
      return "5.0";
    case 13:
      return "5.1";
    case 14:
      return "5.2";
    case 15:
      return "5.3";
    case 16:
      return "6.0";
    case 17:
      return "6.1";
    case 18:
      return "6.2";
    case 19:
      return "6.3";
    case 20:
      return "7.0";
    case 21:
      return "7.1";
    case 22:
      return "7.2";
    case 23:
      return "7.3";
    default:
      return NULL;
  }
}


/**
 * gst_codec_utils_mpeg4video_get_profile:
 * @vis_obj_seq: (array length=len): Pointer to the visual object
 *   sequence for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the profile indication in the stream's visual object sequence into
 * a string. @vis_obj_seq is expected to be the data following the visual
 * object sequence start code. Only the first byte
 * (profile_and_level_indication) is used.
 *
 * Returns: (nullable): The profile as a const string, or NULL if there is an error.
 */
const gchar *
gst_codec_utils_mpeg4video_get_profile (const guint8 * vis_obj_seq, guint len)
{
  /* The profile/level codes are from 14496-2, table G-1, and the Wireshark
   * sources: epan/dissectors/packet-mp4ves.c */

  /* These are a direct mapping from the integer profile id -> string. Profiles
   * 0x6, 0xe and 0xf can correspond to more than one profile depending on the
   * second 4 bits of vis_obj_seq[0], so they are handled separately. */
  static const char *profiles[] = { "simple", "simple-scalable", "core",
    "main", "n-bit", "scalable", NULL, "basic-animated-texture", "hybrid",
    "advanced-real-time-simple", "core-scalable", "advanced-coding-efficiency",
    "advanced-core", "advanced-scalable-texture",
  };
  int profile_id, level_id;

  g_return_val_if_fail (vis_obj_seq != NULL, NULL);

  if (len < 1)
    return NULL;

  GST_MEMDUMP ("VOS", vis_obj_seq, len);

  profile_id = vis_obj_seq[0] >> 4;
  level_id = vis_obj_seq[0] & 0xf;

  GST_LOG ("profile_id = %d, level_id = %d", profile_id, level_id);

  if (profile_id != 6 && profile_id < 0xe)
    return profiles[profile_id];

  if (profile_id != 0xf && level_id == 0)
    return NULL;

  switch (profile_id) {
    case 0x6:
      if (level_id < 3)
        return "simple-face";
      else if (level_id < 5)
        return "simple-fba";
      break;

    case 0xe:
      if (level_id < 5)
        return "simple-studio";
      else if (level_id < 9)
        return "core-studio";
      break;

    case 0xf:
      if (level_id < 6)
        return "advanced-simple";
      else if (level_id > 7 && level_id < 0xe)
        return "fine-granularity-scalable";
      break;
  }

  return NULL;
}

/**
 * gst_codec_utils_mpeg4video_get_level:
 * @vis_obj_seq: (array length=len): Pointer to the visual object
 *   sequence for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the level indication in the stream's visual object sequence into
 * a string. @vis_obj_seq is expected to be the data following the visual
 * object sequence start code. Only the first byte
 * (profile_and_level_indication) is used.
 *
 * Returns: (nullable): The level as a const string, or NULL if there is an error.
 */
const gchar *
gst_codec_utils_mpeg4video_get_level (const guint8 * vis_obj_seq, guint len)
{
  /* The profile/level codes are from 14496-2, table G-1, the Wireshark
   * sources: epan/dissectors/packet-mp4ves.c and the Xvid Sources:
   * src/xvid.h.
   * Levels 4a and 5 for SP were added in Amendment 2, level 6 in Amendment 4
   * (see Xvid sources vfw/config.c)
   *
   * Each profile has a different maximum level it defines. Some of them still
   * need special case handling, because not all levels start from 1, and the
   * Simple profile defines an intermediate level as well. */
  static const int level_max[] = { 6, 2, 2, 4, 2, 1, 2, 2, 2, 4, 3, 4, 2, 3, 4,
    5
  };
  int profile_id, level_id;

  g_return_val_if_fail (vis_obj_seq != NULL, NULL);

  if (len < 1)
    return NULL;

  GST_MEMDUMP ("VOS", vis_obj_seq, len);

  profile_id = vis_obj_seq[0] >> 4;
  level_id = vis_obj_seq[0] & 0xf;

  GST_LOG ("profile_id = %d, level_id = %d", profile_id, level_id);

  if (profile_id != 0xf && level_id == 0)
    return NULL;

  /* Let's do some validation of the level */
  switch (profile_id) {
    case 0x3:
      if (level_id == 1)
        return NULL;
      break;

    case 0x4:
      if (level_id != 2)
        return NULL;
      break;

    case 0x6:
      if (level_id > 5)
        return NULL;
      break;

    case 0xe:
      if (level_id > 9)
        return NULL;
      break;

    case 0xf:
      if (level_id == 6 || level_id == 7 || level_id > 0xd)
        return NULL;
      break;
  }

  if (profile_id == 0 && level_id == 8)
    /* Simple Profile / Level 0 */
    return "0";
  else if (profile_id == 0 && level_id == 9)
    /* Simple Profile / Level 0b */
    return "0b";
  else if (profile_id == 0 && level_id == 4)
    /* Simple Profile / Level 4a */
    return "4a";
  else if (profile_id == 0xf && level_id > 7)
    /* Fine Granularity Scalable Profile */
    return digit_to_string (level_id - 8);
  else if (level_id <= level_max[profile_id])
    /* Levels for all other cases */
    return digit_to_string (level_id);

  return NULL;
}

/**
 * gst_codec_utils_mpeg4video_caps_set_level_and_profile:
 * @caps: the #GstCaps to which the level and profile are to be added
 * @vis_obj_seq: (array length=len): Pointer to the visual object
 *   sequence for the stream.
 * @len: Length of the data available in @sps.
 *
 * Sets the level and profile in @caps if it can be determined from
 * @vis_obj_seq. See gst_codec_utils_mpeg4video_get_level() and
 * gst_codec_utils_mpeg4video_get_profile() for more details on the
 * parameters.
 *
 * Returns: %TRUE if the level and profile could be set, %FALSE otherwise.
 */
gboolean
gst_codec_utils_mpeg4video_caps_set_level_and_profile (GstCaps * caps,
    const guint8 * vis_obj_seq, guint len)
{
  const gchar *profile, *level;

  g_return_val_if_fail (GST_IS_CAPS (caps), FALSE);
  g_return_val_if_fail (GST_CAPS_IS_SIMPLE (caps), FALSE);
  g_return_val_if_fail (vis_obj_seq != NULL, FALSE);

  profile = gst_codec_utils_mpeg4video_get_profile (vis_obj_seq, len);

  if (profile != NULL)
    gst_caps_set_simple (caps, "profile", G_TYPE_STRING, profile, NULL);

  level = gst_codec_utils_mpeg4video_get_level (vis_obj_seq, len);

  if (level != NULL)
    gst_caps_set_simple (caps, "level", G_TYPE_STRING, level, NULL);

  GST_LOG ("profile : %s", (profile) ? profile : "---");
  GST_LOG ("level   : %s", (level) ? level : "---");

  return (profile != NULL && level != NULL);
}

#ifndef GSTREAMER_LITE
/**
 * gst_codec_utils_opus_parse_caps:
 * @caps: the #GstCaps to parse the data from
 * @rate: (optional) (out): the sample rate
 * @channels: (optional) (out): the number of channels
 * @channel_mapping_family: (optional) (out): the channel mapping family
 * @stream_count: (optional) (out): the number of independent streams
 * @coupled_count: (optional) (out): the number of stereo streams
 * @channel_mapping: (optional) (out) (array fixed-size=256): the mapping between the streams
 *
 * Parses Opus caps and fills the different fields with defaults if possible.
 *
 * Returns: %TRUE if parsing was successful, %FALSE otherwise.
 *
 * Since: 1.8
 */
gboolean
gst_codec_utils_opus_parse_caps (GstCaps * caps,
    guint32 * rate,
    guint8 * channels,
    guint8 * channel_mapping_family,
    guint8 * stream_count, guint8 * coupled_count, guint8 channel_mapping[256])
{
  GstStructure *s;
  gint c, f, sc, cc;
  const GValue *va, *v;

  g_return_val_if_fail (caps != NULL, FALSE);
  g_return_val_if_fail (gst_caps_is_fixed (caps), FALSE);
  g_return_val_if_fail (!gst_caps_is_empty (caps), FALSE);

  s = gst_caps_get_structure (caps, 0);

  g_return_val_if_fail (gst_structure_has_name (s, "audio/x-opus"), FALSE);
  g_return_val_if_fail (gst_structure_has_field_typed (s,
          "channel-mapping-family", G_TYPE_INT), FALSE);

  if (rate) {
    gint r;

    if (gst_structure_get_int (s, "rate", &r))
      *rate = r;
    else
      *rate = 48000;
  }

  gst_structure_get_int (s, "channel-mapping-family", &f);
  if (channel_mapping_family)
    *channel_mapping_family = f;

  if (!gst_structure_get_int (s, "channels", &c) || c == 0) {
    if (f == 0)
      c = 2;
    else
      return FALSE;
  }

  if (channels)
    *channels = c;

  /* RTP mapping */
  if (f == 0) {
    if (c > 2)
      return FALSE;

    if (stream_count)
      *stream_count = 1;
    if (coupled_count)
      *coupled_count = c == 2 ? 1 : 0;

    if (channel_mapping) {
      channel_mapping[0] = 0;
      channel_mapping[1] = 1;
    }

    return TRUE;
  }

  if (!gst_structure_get_int (s, "stream-count", &sc))
    return FALSE;
  if (stream_count)
    *stream_count = sc;

  if (!gst_structure_get_int (s, "coupled-count", &cc))
    return FALSE;
  if (coupled_count)
    *coupled_count = cc;

  va = gst_structure_get_value (s, "channel-mapping");
  if (!va || !G_VALUE_HOLDS (va, GST_TYPE_ARRAY))
    return FALSE;

  if (gst_value_array_get_size (va) != c)
    return FALSE;

  if (channel_mapping) {
    gint i;

    for (i = 0; i < c; i++) {
      gint cm;

      v = gst_value_array_get_value (va, i);

      if (!G_VALUE_HOLDS (v, G_TYPE_INT))
        return FALSE;

      cm = g_value_get_int (v);
      if (cm < 0 || cm > 255)
        return FALSE;

      channel_mapping[i] = cm;
    }
  }

  return TRUE;
}

/**
 * gst_codec_utils_opus_create_caps:
 * @rate: the sample rate
 * @channels: the number of channels
 * @channel_mapping_family: the channel mapping family
 * @stream_count: the number of independent streams
 * @coupled_count: the number of stereo streams
 * @channel_mapping: (nullable) (array): the mapping between the streams
 *
 * Creates Opus caps from the given parameters.
 *
 * Returns: (transfer full) (nullable): The #GstCaps, or %NULL if the parameters would lead to
 * invalid Opus caps.
 *
 * Since: 1.8
 */
GstCaps *
gst_codec_utils_opus_create_caps (guint32 rate,
    guint8 channels,
    guint8 channel_mapping_family,
    guint8 stream_count, guint8 coupled_count, const guint8 * channel_mapping)
{
  GstCaps *caps = NULL;
  GValue va = G_VALUE_INIT;
  GValue v = G_VALUE_INIT;
  gint i;

  if (rate == 0)
    rate = 48000;

  if (channel_mapping_family == 0) {
    if (channels > 2) {
      GST_ERROR ("Invalid channels count for channel_mapping_family 0: %d",
          channels);
      goto done;
    }

    if (stream_count > 1) {
      GST_ERROR ("Invalid stream count for channel_mapping_family 0: %d",
          stream_count);
      goto done;
    }

    if (coupled_count > 1) {
      GST_ERROR ("Invalid coupled count for channel_mapping_family 0: %d",
          coupled_count);
      goto done;
    }

    if (channels == 0)
      channels = 2;

    if (stream_count == 0)
      stream_count = 1;

    if (coupled_count == 0)
      coupled_count = channels == 2 ? 1 : 0;

    return gst_caps_new_simple ("audio/x-opus",
        "rate", G_TYPE_INT, rate,
        "channels", G_TYPE_INT, channels,
        "channel-mapping-family", G_TYPE_INT, channel_mapping_family,
        "stream-count", G_TYPE_INT, stream_count,
        "coupled-count", G_TYPE_INT, coupled_count, NULL);
  }

  if (channels == 0) {
    GST_ERROR ("Invalid channels count: %d", channels);
    goto done;
  }

  if (stream_count == 0) {
    GST_ERROR ("Invalid stream count: %d", stream_count);
    goto done;
  }

  if (coupled_count > stream_count) {
    GST_ERROR ("Coupled count %d > stream count: %d", coupled_count,
        stream_count);
    goto done;
  }

  if (channel_mapping == NULL) {
    GST_ERROR
        ("A non NULL channel-mapping is needed for channel_mapping_family != 0");
    goto done;
  }

  caps = gst_caps_new_simple ("audio/x-opus",
      "rate", G_TYPE_INT, rate,
      "channels", G_TYPE_INT, channels,
      "channel-mapping-family", G_TYPE_INT, channel_mapping_family,
      "stream-count", G_TYPE_INT, stream_count,
      "coupled-count", G_TYPE_INT, coupled_count, NULL);

  g_value_init (&va, GST_TYPE_ARRAY);
  g_value_init (&v, G_TYPE_INT);
  for (i = 0; i < channels; i++) {
    g_value_set_int (&v, channel_mapping[i]);
    gst_value_array_append_value (&va, &v);
  }
  gst_structure_set_value (gst_caps_get_structure (caps, 0), "channel-mapping",
      &va);
  g_value_unset (&va);
  g_value_unset (&v);

done:
  return caps;
}

/*
 * (really really) FIXME: move into core (dixit tpm)
 */
/*
 * _gst_caps_set_buffer_array:
 * @caps: (transfer full): a #GstCaps
 * @field: field in caps to set
 * @buf: header buffers
 *
 * Adds given buffers to an array of buffers set as the given @field
 * on the given @caps.  List of buffer arguments must be NULL-terminated.
 *
 * Returns: (transfer full): input caps with a streamheader field added, or NULL
 *     if some error occurred
 */
static GstCaps *
_gst_caps_set_buffer_array (GstCaps * caps, const gchar * field,
    GstBuffer * buf, ...)
{
  GstStructure *structure = NULL;
  va_list va;
  GValue array = { 0 };
  GValue value = { 0 };

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (gst_caps_is_fixed (caps), NULL);
  g_return_val_if_fail (field != NULL, NULL);

  caps = gst_caps_make_writable (caps);
  structure = gst_caps_get_structure (caps, 0);

  g_value_init (&array, GST_TYPE_ARRAY);

  va_start (va, buf);
  /* put buffers in a fixed list */
  while (buf) {
    g_assert (gst_buffer_is_writable (buf));

    /* mark buffer */
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_HEADER);

    g_value_init (&value, GST_TYPE_BUFFER);
    buf = gst_buffer_copy (buf);
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_HEADER);
    gst_value_set_buffer (&value, buf);
    gst_buffer_unref (buf);
    gst_value_array_append_value (&array, &value);
    g_value_unset (&value);

    buf = va_arg (va, GstBuffer *);
  }
  va_end (va);

  gst_structure_set_value (structure, field, &array);
  g_value_unset (&array);

  return caps;
}

/**
 * gst_codec_utils_opus_create_caps_from_header:
 * @header: OpusHead header
 * @comments: (nullable): Comment header or NULL
 *
 * Creates Opus caps from the given OpusHead @header and comment header
 * @comments.
 *
 * Returns: (transfer full) (nullable): The #GstCaps.
 *
 * Since: 1.8
 */
GstCaps *
gst_codec_utils_opus_create_caps_from_header (GstBuffer * header,
    GstBuffer * comments)
{
  GstCaps *caps;
  guint32 rate;
  guint8 channels;
  guint8 channel_mapping_family;
  guint8 stream_count;
  guint8 coupled_count;
  guint8 channel_mapping[256];
  GstBuffer *dummy_comments = NULL;

  g_return_val_if_fail (GST_IS_BUFFER (header), NULL);
  g_return_val_if_fail (comments == NULL || GST_IS_BUFFER (comments), NULL);

  if (!gst_codec_utils_opus_parse_header (header, &rate, &channels,
          &channel_mapping_family, &stream_count, &coupled_count,
          channel_mapping, NULL, NULL))
    return NULL;

  if (!(caps =
          gst_codec_utils_opus_create_caps (rate, channels,
              channel_mapping_family, stream_count, coupled_count,
              channel_mapping)))
    return NULL;

  if (!comments) {
    GstTagList *tags = gst_tag_list_new_empty ();
    dummy_comments =
        gst_tag_list_to_vorbiscomment_buffer (tags, (const guint8 *) "OpusTags",
        8, NULL);
    gst_tag_list_unref (tags);
  }
  _gst_caps_set_buffer_array (caps, "streamheader", header,
      comments ? comments : dummy_comments, NULL);

  if (dummy_comments)
    gst_buffer_unref (dummy_comments);

  return caps;
}

/**
 * gst_codec_utils_opus_create_header:
 * @rate: the sample rate
 * @channels: the number of channels
 * @channel_mapping_family: the channel mapping family
 * @stream_count: the number of independent streams
 * @coupled_count: the number of stereo streams
 * @channel_mapping: (nullable) (array): the mapping between the streams
 * @pre_skip: Pre-skip in 48kHz samples or 0
 * @output_gain: Output gain or 0
 *
 * Creates OpusHead header from the given parameters.
 *
 * Returns: (transfer full) (nullable): The #GstBuffer containing the OpusHead.
 *
 * Since: 1.8
 */
GstBuffer *
gst_codec_utils_opus_create_header (guint32 rate,
    guint8 channels,
    guint8 channel_mapping_family,
    guint8 stream_count,
    guint8 coupled_count,
    const guint8 * channel_mapping, guint16 pre_skip, gint16 output_gain)
{
  GstBuffer *buffer;
  GstByteWriter bw;
  gboolean hdl = TRUE;

  if (rate == 0)
    rate = 48000;

  if (channel_mapping_family == 0) {
    g_return_val_if_fail (channels <= 2, NULL);
    if (channels == 0)
      channels = 2;

    g_return_val_if_fail (stream_count == 0 || stream_count == 1, NULL);
    if (stream_count == 0)
      stream_count = 1;

    g_return_val_if_fail (coupled_count == 0 || coupled_count == 1, NULL);
    if (coupled_count == 0)
      coupled_count = channels == 2 ? 1 : 0;

    channel_mapping = NULL;
  } else {
    g_return_val_if_fail (channels > 0, NULL);
    g_return_val_if_fail (stream_count > 0, NULL);
    g_return_val_if_fail (coupled_count <= stream_count, NULL);
    g_return_val_if_fail (channel_mapping != NULL, NULL);
  }

  gst_byte_writer_init (&bw);
  /* See http://wiki.xiph.org/OggOpus */
  hdl &= gst_byte_writer_put_data (&bw, (const guint8 *) "OpusHead", 8);
  hdl &= gst_byte_writer_put_uint8 (&bw, 0x01); /* version number */
  hdl &= gst_byte_writer_put_uint8 (&bw, channels);
  hdl &= gst_byte_writer_put_uint16_le (&bw, pre_skip);
  hdl &= gst_byte_writer_put_uint32_le (&bw, rate);
  hdl &= gst_byte_writer_put_uint16_le (&bw, output_gain);
  hdl &= gst_byte_writer_put_uint8 (&bw, channel_mapping_family);
  if (channel_mapping_family > 0) {
    hdl &= gst_byte_writer_put_uint8 (&bw, stream_count);
    hdl &= gst_byte_writer_put_uint8 (&bw, coupled_count);
    hdl &= gst_byte_writer_put_data (&bw, channel_mapping, channels);
  }

  if (!hdl) {
    GST_WARNING ("Error creating header");
    gst_byte_writer_reset (&bw);
    return NULL;
  }

  buffer = gst_byte_writer_reset_and_get_buffer (&bw);
  GST_BUFFER_OFFSET (buffer) = 0;
  GST_BUFFER_OFFSET_END (buffer) = 0;

  return buffer;
}

/**
 * gst_codec_utils_opus_parse_header:
 * @header: the OpusHead #GstBuffer
 * @rate: (optional) (out): the sample rate
 * @channels: (optional) (out): the number of channels
 * @channel_mapping_family: (optional) (out): the channel mapping family
 * @stream_count: (optional) (out): the number of independent streams
 * @coupled_count: (optional) (out): the number of stereo streams
 * @channel_mapping: (optional) (out) (array fixed-size=256): the mapping between the streams
 * @pre_skip: (optional) (out): Pre-skip in 48kHz samples or 0
 * @output_gain: (optional) (out): Output gain or 0
 *
 * Parses the OpusHead header.
 *
 * Returns: %TRUE if parsing was successful, %FALSE otherwise.
 *
 * Since: 1.8
 */
gboolean
gst_codec_utils_opus_parse_header (GstBuffer * header,
    guint32 * rate,
    guint8 * channels,
    guint8 * channel_mapping_family,
    guint8 * stream_count,
    guint8 * coupled_count,
    guint8 channel_mapping[256], guint16 * pre_skip, gint16 * output_gain)
{
  GstByteReader br;
  GstMapInfo map;
  gboolean ret = TRUE;
  guint8 c, f, version;

  g_return_val_if_fail (GST_IS_BUFFER (header), FALSE);
  g_return_val_if_fail (gst_buffer_get_size (header) >= 19, FALSE);

  if (!gst_buffer_map (header, &map, GST_MAP_READ))
    return FALSE;
  gst_byte_reader_init (&br, map.data, map.size);
  /* See http://wiki.xiph.org/OggOpus */
  if (memcmp (gst_byte_reader_get_data_unchecked (&br, 8), "OpusHead", 8) != 0) {
    ret = FALSE;
    goto done;
  }
  version = gst_byte_reader_get_uint8_unchecked (&br);
  if (version == 0x00)
    GST_ERROR ("Opus Header version is wrong, should be 0x01 and not 0x00");
  else if (version != 0x01) {
    ret = FALSE;
    goto done;
  }

  c = gst_byte_reader_get_uint8_unchecked (&br);
  if (channels)
    *channels = c;

  if (pre_skip)
    *pre_skip = gst_byte_reader_get_uint16_le_unchecked (&br);
  else
    gst_byte_reader_skip_unchecked (&br, 2);

  if (rate)
    *rate = gst_byte_reader_get_uint32_le_unchecked (&br);
  else
    gst_byte_reader_skip_unchecked (&br, 4);

  if (output_gain)
    *output_gain = gst_byte_reader_get_uint16_le_unchecked (&br);
  else
    gst_byte_reader_skip_unchecked (&br, 2);

  f = gst_byte_reader_get_uint8_unchecked (&br);
  if (channel_mapping_family)
    *channel_mapping_family = f;
  if (f == 0 && c <= 2) {
    if (stream_count)
      *stream_count = 1;
    if (coupled_count)
      *coupled_count = c == 2 ? 1 : 0;
    if (channel_mapping) {
      channel_mapping[0] = 0;
      channel_mapping[1] = 1;
    }

    goto done;
  }

  if (gst_byte_reader_get_remaining (&br) < 2 + c) {
    ret = FALSE;
    goto done;
  }

  if (stream_count)
    *stream_count = gst_byte_reader_get_uint8_unchecked (&br);
  else
    gst_byte_reader_skip_unchecked (&br, 1);

  if (coupled_count)
    *coupled_count = gst_byte_reader_get_uint8_unchecked (&br);
  else
    gst_byte_reader_skip_unchecked (&br, 1);

  if (channel_mapping)
    memcpy (channel_mapping, gst_byte_reader_get_data_unchecked (&br, c), c);

done:
  gst_buffer_unmap (header, &map);

  return ret;
}

/**
 * gst_codec_utils_av1_create_caps_from_av1c:
 * @av1c: (transfer none): a #GstBuffer containing a AV1CodecConfigurationRecord
 *
 * Parses the provided @av1c and returns the corresponding caps
 *
 * Since: 1.26
 *
 * Returns: (transfer full) (nullable): The parsed AV1 caps, or %NULL if there
 * is an error
 */
GstCaps *
gst_codec_utils_av1_create_caps_from_av1c (GstBuffer * av1c)
{
  GstMapInfo map;
  GstCaps *ret = NULL;
  const gchar *profile, *chroma_format;
  guint bit_depth_luma = 8;
  gint presentation_delay = -1;

  g_return_val_if_fail (av1c, NULL);

  if (!gst_buffer_map (av1c, &map, GST_MAP_READ))
    return NULL;

  if (map.size < 4) {
    GST_WARNING ("av1c too small");
    goto done;
  }

  /*
   *  unsigned int (1) marker = 1;
   *  unsigned int (7) version = 1;
   */
  if (map.data[0] != 0x81) {
    GST_WARNING ("Wrong av1c marker/version: 0x%02x", map.data[0]);
    goto done;
  }

  /*
   *  unsigned int (3) seq_profile;
   *  unsigned int (5) seq_level_idx_0;
   */
  switch (map.data[1] >> 5) {
    case 0:
      profile = "main";
      break;
    case 1:
      profile = "high";
      break;
    case 2:
      profile = "professional";
      break;
    default:
      GST_WARNING ("Invalid seq_profile %d", map.data[1] >> 5);
      goto done;
  }

  /* FIXME : Add level processing */

  /*
   *  unsigned int (1) seq_tier_0;
   *  unsigned int (1) high_bitdepth;
   *  unsigned int (1) twelve_bit;
   *  unsigned int (1) monochrome;
   *  unsigned int (1) chroma_subsampling_x;
   *  unsigned int (1) chroma_subsampling_y;
   *  unsigned int (2) chroma_sample_position;
   */
  if ((map.data[2] & 0x60) == 0x60) {
    bit_depth_luma = 12;
  } else if ((map.data[2] & 0x60) == 0x40) {
    bit_depth_luma = 10;
  }

  switch (map.data[2] & 0x1c) {
    case 0x1c:
      chroma_format = "4:0:0";
      break;
    case 0x0c:
      chroma_format = "4:2:0";
      break;
    case 0x08:
      chroma_format = "4:2:2";
      break;
    case 0x00:
      chroma_format = "4:4:4";
      break;
    default:
      GST_WARNING ("invalid chroma format values");
      goto done;
  }

  /*
   *  unsigned int (3) reserved = 0;
   *
   *  unsigned int (1) initial_presentation_delay_present;
   *  if (initial_presentation_delay_present) {
   *    unsigned int (4) initial_presentation_delay_minus_one;
   *  } else {
   *    unsigned int (4) reserved = 0;
   *  }
   */
  if (map.data[3] & 0x10)
    presentation_delay = map.data[3] & 0xf;

  ret = gst_caps_new_simple ("video/x-av1", "profile", G_TYPE_STRING, profile,
      "bit-depth-luma", G_TYPE_UINT, bit_depth_luma,
      "chroma-format", G_TYPE_STRING, chroma_format, NULL);

  if (presentation_delay != -1)
    gst_caps_set_simple (ret, "presentation-delay", G_TYPE_INT,
        presentation_delay, NULL);

  /* FIXME : Extract more information from optional configOBU */

done:
  gst_buffer_unmap (av1c, &map);

  return ret;
}

/**
 * gst_codec_utils_av1_create_av1c_from_caps:
 * @caps: a video/x-av1 #GstCaps
 *
 * Creates the corresponding AV1 Codec Configuration Record
 *
 * Since: 1.26
 *
 * Returns: (transfer full) (nullable): The AV1 Codec Configuration Record, or
 * %NULL if there was an error.
 */

GstBuffer *
gst_codec_utils_av1_create_av1c_from_caps (GstCaps * caps)
{
  gint presentation_delay = -1;
  GstBuffer *av1_codec_data = NULL;
  GstStructure *structure;
  GstMapInfo map;
  const gchar *tmp;
  guint tmp2;

  g_return_val_if_fail (caps, NULL);

  structure = gst_caps_get_structure (caps, 0);
  if (!structure || !gst_structure_has_name (structure, "video/x-av1")) {
    GST_WARNING ("Caps provided are not video/x-av1");
    return NULL;
  }

  gst_structure_get_int (structure, "presentation-delay", &presentation_delay);

  av1_codec_data = gst_buffer_new_allocate (NULL, 4, NULL);
  gst_buffer_map (av1_codec_data, &map, GST_MAP_WRITE);

  /*
   *  unsigned int (1) marker = 1;
   *  unsigned int (7) version = 1;
   *  unsigned int (3) seq_profile;
   *  unsigned int (5) seq_level_idx_0;
   *  unsigned int (1) seq_tier_0;
   *  unsigned int (1) high_bitdepth;
   *  unsigned int (1) twelve_bit;
   *  unsigned int (1) monochrome;
   *  unsigned int (1) chroma_subsampling_x;
   *  unsigned int (1) chroma_subsampling_y;
   *  unsigned int (2) chroma_sample_position;
   *  unsigned int (3) reserved = 0;
   *
   *  unsigned int (1) initial_presentation_delay_present;
   *  if (initial_presentation_delay_present) {
   *    unsigned int (4) initial_presentation_delay_minus_one;
   *  } else {
   *    unsigned int (4) reserved = 0;
   *  }
   */

  map.data[0] = 0x81;
  map.data[1] = 0x00;
  if ((tmp = gst_structure_get_string (structure, "profile"))) {
    if (strcmp (tmp, "main") == 0)
      map.data[1] |= (0 << 5);
    if (strcmp (tmp, "high") == 0)
      map.data[1] |= (1 << 5);
    if (strcmp (tmp, "professional") == 0)
      map.data[1] |= (2 << 5);
  }
  /* FIXME: level set to 1 */
  map.data[1] |= 0x01;
  /* FIXME: tier set to 0 */

  if (gst_structure_get_uint (structure, "bit-depth-luma", &tmp2)) {
    if (tmp2 == 10) {
      map.data[2] |= 0x40;
    } else if (tmp2 == 12) {
      map.data[2] |= 0x60;
    }
  }

  /* Assume 4:2:0 if nothing else is given */
  map.data[2] |= 0x0C;
  if ((tmp = gst_structure_get_string (structure, "chroma-format"))) {
    if (strcmp (tmp, "4:0:0") == 0)
      map.data[2] |= 0x1C;
    if (strcmp (tmp, "4:2:0") == 0)
      map.data[2] |= 0x0C;
    if (strcmp (tmp, "4:2:2") == 0)
      map.data[2] |= 0x08;
    if (strcmp (tmp, "4:4:4") == 0)
      map.data[2] |= 0x00;
  }

  /* FIXME: keep chroma-site unknown */

  if (presentation_delay != -1) {
    map.data[3] = 0x10 | (MAX (0xF, presentation_delay) & 0xF);
  }

  gst_buffer_unmap (av1_codec_data, &map);

  return av1_codec_data;
}


static gboolean
h264_caps_structure_get_profile_flags_level (GstStructure * caps_st,
    guint8 * profile, guint8 * flags, guint8 * level)
{
  const GValue *codec_data_value = NULL;
  GstBuffer *codec_data = NULL;
  GstMapInfo map;
  gboolean ret = FALSE;
  guint8 *data = NULL;
  gsize size;

  codec_data_value = gst_structure_get_value (caps_st, "codec_data");
  if (!codec_data_value) {
    GST_DEBUG
        ("video/x-h264 caps did not have codec_data set, cannot parse profile, flags and level");
    return FALSE;
  }

  codec_data = gst_value_get_buffer (codec_data_value);
  if (!gst_buffer_map (codec_data, &map, GST_MAP_READ)) {
    return FALSE;
  }
  data = map.data;
  size = map.size;

  if (!gst_codec_utils_h264_get_profile_flags_level (data, (guint) size,
          profile, flags, level)) {
    GST_WARNING
        ("Failed to parse profile, flags and level from h264 codec data");
    goto done;
  }

  ret = TRUE;

done:
  gst_buffer_unmap (codec_data, &map);

  return ret;
}

static gboolean
aac_caps_structure_get_audio_object_type (GstStructure * caps_st,
    guint8 * audio_object_type)
{
  gboolean ret = FALSE;
  const GValue *codec_data_value = NULL;
  GstBuffer *codec_data = NULL;
  GstMapInfo map;
  guint8 *data = NULL;
  gsize size;
  GstBitReader br;

  codec_data_value = gst_structure_get_value (caps_st, "codec_data");
  if (!codec_data_value) {
    GST_DEBUG
        ("audio/mpeg pad did not have codec_data set, cannot parse audio object type");
    return FALSE;
  }

  codec_data = gst_value_get_buffer (codec_data_value);
  if (!gst_buffer_map (codec_data, &map, GST_MAP_READ)) {
    return FALSE;
  }
  data = map.data;
  size = map.size;

  if (size < 2) {
    GST_WARNING ("aac codec data is too small");
    goto done;
  }

  gst_bit_reader_init (&br, data, size);
  ret = gst_codec_utils_aac_get_audio_object_type (&br, audio_object_type);

done:
  gst_buffer_unmap (codec_data, &map);

  return ret;
}

static gboolean
hevc_caps_get_mime_codec (GstCaps * caps, gchar ** mime_codec)
{
  GstStructure *caps_st = NULL;
  const GValue *codec_data_value = NULL;
  GstBuffer *codec_data = NULL;
  GstMapInfo map;
  gboolean ret = FALSE;
  const gchar *stream_format;
  guint8 *data = NULL;
  gsize size;
  guint16 profile_space;
  guint8 tier_flag;
  guint16 profile_idc;
  guint32 compat_flags;
  guchar constraint_indicator_flags[6];
  guint8 level_idc;
  guint32 compat_flag_parameter = 0;
  GString *codec_string;
  const guint8 *profile_tier_level;
  gint last_flag_index;

  caps_st = gst_caps_get_structure (caps, 0);
  codec_data_value = gst_structure_get_value (caps_st, "codec_data");
  stream_format = gst_structure_get_string (caps_st, "stream-format");
  if (!codec_data_value) {
    GST_DEBUG ("video/x-h265 caps did not have codec_data set, cannot parse");
    return FALSE;
  } else if (!stream_format) {
    GST_DEBUG
        ("video/x-h265 caps did not have stream-format set, cannot parse");
    return FALSE;
  }

  codec_data = gst_value_get_buffer (codec_data_value);
  if (!gst_buffer_map (codec_data, &map, GST_MAP_READ)) {
    return FALSE;
  }
  data = map.data;
  size = map.size;

  /* HEVCDecoderConfigurationRecord is at a minimum 23 bytes long */
  if (size < 23) {
    GST_DEBUG ("Incomplete HEVCDecoderConfigurationRecord");
    goto done;
  }

  if (!g_str_equal (stream_format, "hev1")
      && !g_str_equal (stream_format, "hvc1")) {
    GST_DEBUG ("Unknown stream-format %s", stream_format);
    goto done;
  }

  profile_tier_level = data + 1;
  profile_space = (profile_tier_level[0] & 0x11) >> 6;
  tier_flag = (profile_tier_level[0] & 0x001) >> 5;
  profile_idc = (profile_tier_level[0] & 0x1f);

  compat_flags = GST_READ_UINT32_BE (data + 2);
  for (unsigned i = 0; i < 6; ++i)
    constraint_indicator_flags[i] = GST_READ_UINT8 (data + 6 + i);

  level_idc = data[12];

  /* The 32 bits of the compat_flags, but in reverse bit order */
  compat_flags =
      ((compat_flags & 0xaaaaaaaa) >> 1) | ((compat_flags & 0x55555555) << 1);
  compat_flags =
      ((compat_flags & 0xcccccccc) >> 2) | ((compat_flags & 0x33333333) << 2);
  compat_flags =
      ((compat_flags & 0xf0f0f0f0) >> 4) | ((compat_flags & 0x0f0f0f0f) << 4);
  compat_flags =
      ((compat_flags & 0xff00ff00) >> 8) | ((compat_flags & 0x00ff00ff) << 8);
  compat_flag_parameter = (compat_flags >> 16) | (compat_flags << 16);

  codec_string = g_string_new (stream_format);
  codec_string = g_string_append_c (codec_string, '.');
  if (profile_space)
    codec_string = g_string_append_c (codec_string, 'A' + profile_space - 1);
  g_string_append_printf (codec_string, "%" G_GUINT16_FORMAT ".%X.%c%d",
      profile_idc, compat_flag_parameter, tier_flag ? 'H' : 'L', level_idc);

  /* Each of the 6 bytes of the constraint flags, starting from the byte containing the
   * progressive_source_flag, each encoded as a hexadecimal number, and the encoding
   * of each byte separated by a period; trailing bytes that are zero may be omitted.
   */
  last_flag_index = 5;
  while (last_flag_index >= 0
      && (int) (constraint_indicator_flags[last_flag_index]) == 0)
    --last_flag_index;
  for (gint i = 0; i <= last_flag_index; ++i) {
    g_string_append_printf (codec_string, ".%02X",
        constraint_indicator_flags[i]);
  }

  *mime_codec = g_string_free (codec_string, FALSE);

  ret = TRUE;

done:
  gst_buffer_unmap (codec_data, &map);
  return ret;
}

/* https://www.webmproject.org/vp9/mp4/#codecs-parameter-string */
static char *
vp9_caps_get_mime_codec (GstCaps * caps)
{
  GstStructure *caps_st;
  const char *profile_str, *chroma_format_str, *colorimetry_str;
  guint bitdepth_luma, bitdepth_chroma;
  guint8 profile = -1, chroma_format = -1, level = -1, color_primaries =
      -1, color_transfer = -1, color_matrix = -1;
  gboolean video_full_range;
  GstVideoColorimetry cinfo = { 0, };
  GString *codec_string;

  caps_st = gst_caps_get_structure (caps, 0);
  codec_string = g_string_new ("vp09");

  profile_str = gst_structure_get_string (caps_st, "profile");
  if (g_strcmp0 (profile_str, "0") == 0) {
    profile = 0;
  } else if (g_strcmp0 (profile_str, "1") == 0) {
    profile = 1;
  } else if (g_strcmp0 (profile_str, "2") == 0) {
    profile = 2;
  } else if (g_strcmp0 (profile_str, "3") == 0) {
    profile = 3;
  } else {
    goto done;
  }

  /* XXX: hardcoded level */
  level = 10;

  gst_structure_get (caps_st, "bit-depth-luma", G_TYPE_UINT,
      &bitdepth_luma, "bit-depth-chroma", G_TYPE_UINT, &bitdepth_chroma, NULL);

  if (bitdepth_luma == 0)
    goto done;
  if (bitdepth_luma != bitdepth_chroma)
    goto done;

  /* mandatory elements */
  g_string_append_printf (codec_string, ".%02u.%02u.%02u", profile, level,
      bitdepth_luma);

  colorimetry_str = gst_structure_get_string (caps_st, "colorimetry");
  if (!colorimetry_str)
    goto done;
  if (!gst_video_colorimetry_from_string (&cinfo, colorimetry_str))
    goto done;
  video_full_range = cinfo.range == GST_VIDEO_COLOR_RANGE_0_255;

  chroma_format_str = gst_structure_get_string (caps_st, "chroma-format");
  if (g_strcmp0 (chroma_format_str, "4:2:0") == 0) {
    const char *chroma_site_str;
    GstVideoChromaSite chroma_site;

    chroma_site_str = gst_structure_get_string (caps_st, "chroma-site");
    if (chroma_site_str)
      chroma_site = gst_video_chroma_site_from_string (chroma_site_str);
    else
      chroma_site = GST_VIDEO_CHROMA_SITE_UNKNOWN;
    if (chroma_site == GST_VIDEO_CHROMA_SITE_V_COSITED) {
      chroma_format = 0;
    } else if (chroma_site == GST_VIDEO_CHROMA_SITE_COSITED) {
      chroma_format = 1;
    } else {
      chroma_format = 1;
    }
  } else if (g_strcmp0 (chroma_format_str, "4:2:2") == 0) {
    chroma_format = 2;
  } else if (g_strcmp0 (chroma_format_str, "4:4:4") == 0) {
    chroma_format = 3;
  } else {
    goto done;
  }

  /* optional but all or nothing. Include them if any parameter differs from the default value */
  color_primaries = gst_video_color_primaries_to_iso (cinfo.primaries);
  color_transfer = gst_video_transfer_function_to_iso (cinfo.transfer);
  color_matrix = gst_video_color_matrix_to_iso (cinfo.matrix);
  if (chroma_format != 1 || color_primaries != 1 || color_transfer != 1
      || color_matrix != 1 || video_full_range) {
    g_string_append_printf (codec_string, ".%02u.%02u.%02u.%02u.%02u",
        chroma_format, color_primaries, color_transfer, color_matrix,
        video_full_range);
  }

done:
  return g_string_free (codec_string, FALSE);
}

static GstCaps *
av1_caps_from_mime_codec (gchar ** subcodec)
{
  GstCaps *caps = NULL;
  gchar tier;
  guint seq_level_idx_0;
  guint bit_depth, seq_profile, chroma_sample_position,
      monochrome, chroma_subsampling_x, chroma_subsampling_y, primaries,
      transfer, matrix, full_range, chroma_sampling;
  const gchar *level_str;
  const gchar *tier_str;
  const gchar *profile_str;
  const gchar *chroma_format_str;
  gchar *colorimetry_str;
  GstVideoColorimetry cinfo = { 0, };

  caps = gst_caps_new_empty_simple ("video/x-av1");

  if (!subcodec[1])
    goto done;

  seq_profile = g_ascii_strtoull (subcodec[1], NULL, 10);
  if (seq_profile == 0) {
    profile_str = "main";
  } else if (seq_profile == 1) {
    profile_str = "high";
  } else if (seq_profile == 2) {
    profile_str = "professional";
  } else {
    GST_WARNING ("Unknown AV1 profile %d", seq_profile);
    goto done;
  }
  gst_caps_set_simple (caps, "profile", G_TYPE_STRING, profile_str, NULL);

  if (subcodec[2]) {
    if (sscanf (subcodec[2], "%02u%c", &seq_level_idx_0, &tier) != 2) {
      GST_WARNING ("Failed to parse level and tier from %s", subcodec[2]);
      goto done;
    }
  } else {
    seq_level_idx_0 = 1;
    tier = 'M';
  }

  if (tier == 'H') {
    tier_str = "high";
  } else if (tier == 'M') {
    tier_str = "main";
  } else {
    GST_WARNING ("Unknown AV1 tier %c", tier);
    goto done;
  }
  gst_caps_set_simple (caps, "tier", G_TYPE_STRING, tier_str, NULL);

  level_str = gst_codec_utils_av1_get_level (seq_level_idx_0);
  if (level_str) {
    gst_caps_set_simple (caps, "level", G_TYPE_STRING, level_str, NULL);
  } else {
    GST_WARNING ("Unknown AV1 level %d", seq_level_idx_0);
    goto done;
  }

  if (subcodec[3]) {
    bit_depth = g_ascii_strtoull (subcodec[3], NULL, 10);
    gst_caps_set_simple (caps, "bit-depth-luma", G_TYPE_UINT, bit_depth,
        "bit-depth-chroma", G_TYPE_UINT, bit_depth, NULL);
  } else {
    GST_WARNING ("Failed to parse bit-depth from %s", subcodec[3]);
    goto done;
  }

  /* Verify if all values necessary to continue are present in the subcodec */
  if (subcodec[4] && subcodec[5] && subcodec[6]
      && subcodec[7] && subcodec[8] && subcodec[9]) {

    monochrome = g_ascii_strtoull (subcodec[4], NULL, 10);
    chroma_sampling = g_ascii_strtoull (subcodec[5], NULL, 10);
    chroma_subsampling_x = chroma_sampling / 100;
    chroma_subsampling_y = (chroma_sampling % 100) / 10;
    chroma_sample_position = chroma_sampling % 10;
    if (monochrome) {
      chroma_format_str = "4:0:0";
    } else if (chroma_subsampling_x == 1 && chroma_subsampling_y == 1) {
      chroma_format_str = "4:2:0";
    } else if (chroma_subsampling_x == 1 && chroma_subsampling_y == 0) {
      chroma_format_str = "4:2:2";
    } else if (chroma_subsampling_x == 0 && chroma_subsampling_y == 0) {
      chroma_format_str = "4:4:4";
    } else {
      GST_WARNING ("Unknown chroma subsampling %d:%d:%d", chroma_subsampling_x,
          chroma_subsampling_y, monochrome);
      goto done;
    }

    primaries = g_ascii_strtoull (subcodec[6], NULL, 10);
    transfer = g_ascii_strtoull (subcodec[7], NULL, 10);
    matrix = g_ascii_strtoull (subcodec[8], NULL, 10);
    full_range = g_ascii_strtoull (subcodec[9], NULL, 10);
  } else {
    GST_DEBUG
        ("Using default values for chroma_format, chroma_sample_position, "
        "primaries, transfer, matrix, and full_range");

    chroma_format_str = "4:2:0";
    chroma_sample_position = 0;
    primaries = 1;
    transfer = 1;
    matrix = 1;
    full_range = 0;
  }

  gst_caps_set_simple (caps, "chroma-format", G_TYPE_STRING, chroma_format_str,
      NULL);
  if (chroma_sample_position == 1) {
    gst_caps_set_simple (caps, "chroma-site", G_TYPE_STRING, "v-cosited", NULL);
  } else if (chroma_sample_position == 2) {
    gst_caps_set_simple (caps, "chroma-site", G_TYPE_STRING,
        "v-cosited+h-cosited", NULL);
  }

  cinfo.range =
      full_range ? GST_VIDEO_COLOR_RANGE_0_255 : GST_VIDEO_COLOR_RANGE_16_235;
  cinfo.primaries = gst_video_color_primaries_from_iso (primaries);
  cinfo.transfer = gst_video_transfer_function_from_iso (transfer);
  cinfo.matrix = gst_video_color_matrix_from_iso (matrix);
  colorimetry_str = gst_video_colorimetry_to_string (&cinfo);
  if (colorimetry_str) {
    gst_caps_set_simple (caps, "colorimetry", G_TYPE_STRING, colorimetry_str,
        NULL);
  } else {
    GST_WARNING ("Failed to parse colorimetry from %u %u %u %u", full_range,
        matrix, transfer, primaries);
  }
  g_free (colorimetry_str);

done:
  return caps;
}

/* https://aomediacodec.github.io/av1-isobmff/#codecsparam */
static char *
av1_caps_get_mime_codec (GstCaps * caps)
{
  gchar tier_mime;
  guint8 seq_level_idx_0;
  guint bit_depth, seq_profile, chroma_sample_position,
      monochrome, chroma_subsampling_x, chroma_subsampling_y, primaries,
      transfer, matrix, full_range;
  GstStructure *caps_st;
  GString *codec_string;
  const gchar *level_str;
  const gchar *tier_str;
  const gchar *profile_str;
  const gchar *chroma_format_str;
  const gchar *chroma_site_str;
  const gchar *colorimetry_str;
  GstVideoColorimetry cinfo = { 0, };

  caps_st = gst_caps_get_structure (caps, 0);
  codec_string = g_string_new ("av01");

  tier_str = gst_structure_get_string (caps_st, "tier");
  if (g_strcmp0 (tier_str, "main") == 0) {
    tier_mime = 'M';
  } else if (g_strcmp0 (tier_str, "high") == 0) {
    tier_mime = 'H';
  } else {
    GST_WARNING ("Unknown AV1 tier %s, using default 'M'", tier_str);
    tier_mime = 'M';
  }

  level_str = gst_structure_get_string (caps_st, "level");
  if (level_str) {
    seq_level_idx_0 = gst_codec_utils_av1_get_seq_level_idx (level_str);
  } else {
    seq_level_idx_0 = 1;
  }

  profile_str = gst_structure_get_string (caps_st, "profile");
  if (g_strcmp0 (profile_str, "main") == 0) {
    seq_profile = 0;
  } else if (g_strcmp0 (profile_str, "high") == 0) {
    seq_profile = 1;
  } else if (g_strcmp0 (profile_str, "professional") == 0) {
    seq_profile = 2;
  } else {
    goto done;
  }

  if (!gst_structure_get_uint (caps_st, "bit-depth-luma", &bit_depth))
    goto done;

  /* We have all information to compute a minimal mime */
  g_string_append_printf (codec_string, ".%d.%02u%c.%02u",
      seq_profile, seq_level_idx_0, tier_mime, bit_depth);

  chroma_format_str = gst_structure_get_string (caps_st, "chroma-format");
  if (g_strcmp0 (chroma_format_str, "4:0:0") == 0) {
    monochrome = 1;
    chroma_subsampling_x = 1;
    chroma_subsampling_y = 1;
  } else if (g_strcmp0 (chroma_format_str, "4:2:0") == 0) {
    monochrome = 0;
    chroma_subsampling_x = 1;
    chroma_subsampling_y = 1;
  } else if (g_strcmp0 (chroma_format_str, "4:2:2") == 0) {
    monochrome = 0;
    chroma_subsampling_x = 1;
    chroma_subsampling_y = 0;
  } else if (g_strcmp0 (chroma_format_str, "4:4:4") == 0) {
    monochrome = 0;
    chroma_subsampling_x = 0;
    chroma_subsampling_y = 0;
  } else {
    goto done;
  }

  chroma_sample_position = 0;
  chroma_site_str = gst_structure_get_string (caps_st, "chroma-site");
  if (g_strcmp0 (chroma_site_str, "v-cosited") == 0) {
    chroma_sample_position = 1;
  } else if (g_strcmp0 (chroma_site_str, "v-cosited+h-cosited") == 0) {
    chroma_sample_position = 2;
  }

  colorimetry_str = gst_structure_get_string (caps_st, "colorimetry");
  if (!colorimetry_str)
    goto done;
  if (!gst_video_colorimetry_from_string (&cinfo, colorimetry_str))
    goto done;
  full_range = cinfo.range == GST_VIDEO_COLOR_RANGE_0_255;

  primaries = gst_video_color_primaries_to_iso (cinfo.primaries);
  transfer = gst_video_transfer_function_to_iso (cinfo.transfer);
  matrix = gst_video_color_matrix_to_iso (cinfo.matrix);

  if (chroma_subsampling_x != 1 || chroma_subsampling_y != 1
      || chroma_sample_position != 0 || primaries != 1 || transfer != 1
      || matrix != 1 || full_range != 0) {
    g_string_append_printf (codec_string,
        ".%u.%u%u%u.%02u.%02u.%02u.%u", monochrome, chroma_subsampling_x,
        chroma_subsampling_y, chroma_sample_position, primaries, transfer,
        matrix, full_range);
  }

done:
  return g_string_free (codec_string, FALSE);
}


/**
 * gst_codec_utils_caps_get_mime_codec:
 * @caps: A #GstCaps to convert to mime codec
 *
 * Converts @caps to a RFC 6381 compatible codec string if possible.
 *
 * Useful for providing the 'codecs' field inside the 'Content-Type' HTTP
 * header for containerized formats, such as mp4 or matroska.
 *
 * Registered codecs can be found at http://mp4ra.org/#/codecs
 *
 * Returns: (transfer full) (nullable): a RFC 6381 compatible codec string or %NULL
 *
 * Since: 1.20
 */
gchar *
gst_codec_utils_caps_get_mime_codec (GstCaps * caps)
{
  gchar *mime_codec = NULL;
  GstStructure *caps_st = NULL;
  const gchar *media_type = NULL;

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (gst_caps_is_fixed (caps), NULL);

  caps_st = gst_caps_get_structure (caps, 0);
  if (caps_st == NULL) {
    GST_WARNING ("Failed to get structure from caps");
    goto done;
  }

  media_type = gst_structure_get_name (caps_st);

  if (g_strcmp0 (media_type, "video/x-h264") == 0) {
    /* avc1.AABBCC
     *   AA = profile
     *   BB = constraint set flags
     *   CC = level
     */
    guint8 profile = 0;
    guint8 flags = 0;
    guint8 level = 0;

    if (!h264_caps_structure_get_profile_flags_level (caps_st, &profile, &flags,
            &level)) {
      GST_DEBUG
          ("h264 caps did not contain 'codec_data', cannot determine detailed codecs info");
      mime_codec = g_strdup ("avc1");
    } else {
      mime_codec = g_strdup_printf ("avc1.%02X%02X%02X", profile, flags, level);
    }
  } else if (g_strcmp0 (media_type, "video/x-h265") == 0) {
    if (!hevc_caps_get_mime_codec (caps, &mime_codec)) {
      GST_DEBUG ("h265 caps parsing failed");
      mime_codec = g_strdup ("hev1");
    }
  } else if (g_strcmp0 (media_type, "video/x-h266") == 0) {
    mime_codec = g_strdup ("vvc1");
  } else if (g_strcmp0 (media_type, "video/x-av1") == 0) {
    mime_codec = av1_caps_get_mime_codec (caps);
  } else if (g_strcmp0 (media_type, "video/x-vp8") == 0) {
    /* TODO: most browsers won't play the video unless more codec information is
     * available in the mime codec for vp8. */
    mime_codec = g_strdup ("vp08");
  } else if (g_strcmp0 (media_type, "video/x-vp9") == 0) {
    mime_codec = vp9_caps_get_mime_codec (caps);
  } else if (g_strcmp0 (media_type, "image/jpeg") == 0) {
    mime_codec = g_strdup ("mjpg");
  } else if (g_strcmp0 (media_type, "audio/mpeg") == 0) {
    guint8 audio_object_type = 0;
    if (aac_caps_structure_get_audio_object_type (caps_st, &audio_object_type)) {
      mime_codec = g_strdup_printf ("mp4a.40.%u", audio_object_type);
    } else {
      mime_codec = g_strdup ("mp4a.40");
    }
  } else if (g_strcmp0 (media_type, "audio/x-opus") == 0) {
    mime_codec = g_strdup ("opus");
  } else if (g_strcmp0 (media_type, "audio/x-mulaw") == 0) {
    mime_codec = g_strdup ("ulaw");
  } else if (g_strcmp0 (media_type, "audio/x-adpcm") == 0) {
    if (g_strcmp0 (gst_structure_get_string (caps_st, "layout"), "g726") == 0) {
      mime_codec = g_strdup ("g726");
    }
  }

done:
  return mime_codec;
}

static GstCaps *
gst_codec_utils_caps_from_mime_codec_single (const gchar * codec)
{
  GstCaps *caps = NULL;
  gchar **subcodec = NULL;
  gchar *subcodec0;
  guint32 codec_fourcc;

  GST_DEBUG ("Analyzing codec '%s'", codec);

  /* rfc 6381 3.3
   *
   * For the ISO Base Media File Format, and the QuickTime movie file
   * format, the first element of a 'codecs' parameter value is a sample
   * description entry four-character code as registered by the MP4
   * Registration Authority [MP4RA].
   *
   * See Also : http://mp4ra.org/#/codecs
   */
  if (strlen (codec) < 4) {
    GST_WARNING ("Invalid codec (smaller than 4 characters) : '%s'", codec);
    goto beach;
  }

  subcodec = g_strsplit (codec, ".", 0);
  subcodec0 = subcodec[0];

  if (subcodec0 == NULL)
    goto beach;

  /* Skip any leading spaces */
  while (*subcodec0 == ' ')
    subcodec0++;

  if (strlen (subcodec0) < 4) {
    GST_WARNING ("Invalid codec (smaller than 4 characters) : '%s'", subcodec0);
    goto beach;
  }

  GST_LOG ("subcodec[0] '%s'", subcodec0);

  codec_fourcc = GST_READ_UINT32_LE (subcodec0);
  switch (codec_fourcc) {
    case GST_MAKE_FOURCC ('a', 'v', 'c', '1'):
    case GST_MAKE_FOURCC ('a', 'v', 'c', '2'):
    case GST_MAKE_FOURCC ('a', 'v', 'c', '3'):
    case GST_MAKE_FOURCC ('a', 'v', 'c', '4'):
    {
      guint8 sps[3];
      guint64 spsint64;

      /* ISO 14496-15 Annex E : Sub-parameters for the MIME type “codecs”
       * parameter */
      caps = gst_caps_new_empty_simple ("video/x-h264");

      if (subcodec[1]) {
        /* The second element is the hexadecimal representation of the following
         * three bytes in the (subset) sequence parameter set Network
         * Abstraction Layer (NAL) unit specified in [AVC]:
         * * profile_idc
         * * constraint_set flags
         * * level_idc
         * */
        spsint64 = g_ascii_strtoull (subcodec[1], NULL, 16);
        sps[0] = spsint64 >> 16;
        sps[1] = (spsint64 >> 8) & 0xff;
        sps[2] = spsint64 & 0xff;
        gst_codec_utils_h264_caps_set_level_and_profile (caps,
            (const guint8 *) &sps, 3);
      }
    }
      break;
    case GST_MAKE_FOURCC ('m', 'p', '4', 'a'):
    {
      guint64 oti;

      if (!subcodec[1])
        break;
      oti = g_ascii_strtoull (subcodec[1], NULL, 16);
      /* For mp4a, mp4v and mp4s, the second element is the hexadecimal
       * representation of the MP4 Registration Authority
       * ObjectTypeIndication */
      switch (oti) {
        case 0x40:
        {
          guint64 audio_oti;
          const gchar *profile = NULL;

          /* MPEG-4 Audio (ISO/IEC 14496-3 */
          caps =
              gst_caps_new_simple ("audio/mpeg", "mpegversion", G_TYPE_INT, 4,
              NULL);

          if (!subcodec[2])
            break;
          /* If present, last element is the audio object type */
          audio_oti = g_ascii_strtoull (subcodec[2], NULL, 16);

          switch (audio_oti) {
            case 1:
              profile = "main";
              break;
            case 2:
              profile = "lc";
              break;
            case 3:
              profile = "ssr";
              break;
            case 4:
              profile = "ltp";
              break;
            default:
              GST_WARNING ("Unhandled MPEG-4 Audio Object Type: 0x%"
                  G_GUINT64_FORMAT "x", audio_oti);
              break;
          }
          if (profile)
            gst_caps_set_simple (caps, "profile", G_TYPE_STRING, profile, NULL);
          break;
        }
        default:
          GST_WARNING ("Unknown ObjectTypeIndication 0x%" G_GUINT64_FORMAT "x",
              oti);
          break;
      }
    }
      break;
    case GST_MAKE_FOURCC ('h', 'e', 'v', '1'):
    case GST_MAKE_FOURCC ('h', 'v', 'c', '1'):
    {
      /* ISO 14496-15 Annex E : Sub-parameters for the MIME type “codecs”
       * parameter */
      caps = gst_caps_new_empty_simple ("video/x-h265");

      /* FIXME : Extract information from the following component */
      break;
    }
    case GST_MAKE_FOURCC ('v', 'v', 'c', '1'):
    case GST_MAKE_FOURCC ('v', 'v', 'i', '1'):
    {
      /* H.266 */
      caps = gst_caps_new_empty_simple ("video/x-h266");
      break;
    }
      /* Following are not defined in rfc 6831 but are registered MP4RA codecs */
    case GST_MAKE_FOURCC ('a', 'c', '-', '3'):
      /* ETSI TS 102 366 v1.4.1 - Digital Audio Compression (AC-3, Enhanced AC-3) Standard, Annex F */
      caps = gst_caps_new_empty_simple ("audio/x-ac3");
      break;
    case GST_MAKE_FOURCC ('e', 'c', '+', '3'):
      GST_FIXME
          ("Signalling of ATMOS ('ec+3') isn't defined yet. Falling back to EAC3 caps");
      /* withdrawn, unused, do not use (was enhanced AC-3 audio with JOC) */
      /* FALLTHROUGH */
    case GST_MAKE_FOURCC ('e', 'c', '-', '3'):
      /* ETSI TS 102 366 v1.4.1 - Digital Audio Compression (AC-3, Enhanced AC-3) Standard, Annex F */
      caps = gst_caps_new_empty_simple ("audio/x-eac3");
      break;
    case GST_MAKE_FOURCC ('s', 't', 'p', 'p'):
      /* IMSC1-conformant TTM XML */
      caps = gst_caps_new_empty_simple ("application/ttml+xml");
      break;
    case GST_MAKE_FOURCC ('w', 'v', 't', 't'):
      /* WebVTT subtitles */
      caps = gst_caps_new_empty_simple ("application/x-subtitle-vtt");
      break;
    case GST_MAKE_FOURCC ('v', 'p', '0', '8'):
      /* VP8 */
      caps = gst_caps_new_empty_simple ("video/x-vp8");
      break;
    case GST_MAKE_FOURCC ('v', 'p', '0', '9'):
      /* VP9 */
      caps = gst_caps_new_empty_simple ("video/x-vp9");
      break;
    case GST_MAKE_FOURCC ('a', 'v', '0', '1'):
    {
      /* AV1 */
      caps = av1_caps_from_mime_codec (subcodec);
      break;
    }
    case GST_MAKE_FOURCC ('o', 'p', 'u', 's'):
      /* Opus */
      caps = gst_caps_new_empty_simple ("audio/x-opus");
      break;
    case GST_MAKE_FOURCC ('u', 'l', 'a', 'w'):
      /* ulaw */
      caps = gst_caps_new_empty_simple ("audio/x-mulaw");
      break;
    case GST_MAKE_FOURCC ('g', '7', '2', '6'):
      /* ulaw */
      caps =
          gst_caps_new_simple ("audio/x-adpcm", "layout", G_TYPE_STRING, "g726",
          NULL);
      break;
    case GST_MAKE_FOURCC ('m', 'j', 'p', 'g'):
      caps = gst_caps_new_empty_simple ("image/jpeg");
      break;
    default:
      GST_WARNING ("Unknown codec '%s' please file a bug", codec);
      break;
  }

beach:
  if (subcodec != NULL)
    g_strfreev (subcodec);
  return caps;
}

/**
 * gst_codec_utils_caps_from_mime_codec:
 * @codecs_field: A mime codec string field
 *
 * Converts a RFC 6381 compatible codec string to #GstCaps. More than one codec
 * string can be present (separated by `,`).
 *
 * Registered codecs can be found at http://mp4ra.org/#/codecs
 *
 * Returns: (transfer full) (nullable): The corresponding #GstCaps or %NULL
 *
 * Since: 1.22
 */
GstCaps *
gst_codec_utils_caps_from_mime_codec (const gchar * codecs_field)
{
  gchar **codecs = NULL;
  GstCaps *caps = NULL;
  guint i;

  g_return_val_if_fail (codecs_field != NULL, NULL);

  GST_LOG ("codecs_field '%s'", codecs_field);

  codecs = g_strsplit (codecs_field, ",", 0);
  if (codecs == NULL) {
    GST_WARNING ("Invalid 'codecs' field : '%s'", codecs_field);
    goto beach;
  }

  for (i = 0; codecs[i]; i++) {
    const gchar *codec = codecs[i];
    if (caps == NULL)
      caps = gst_codec_utils_caps_from_mime_codec_single (codec);
    else
      gst_caps_append (caps,
          gst_codec_utils_caps_from_mime_codec_single (codec));
  }

beach:
  g_strfreev (codecs);
  GST_LOG ("caps %" GST_PTR_FORMAT, caps);
  return caps;
}
#endif // GSTREAMER_LITE
