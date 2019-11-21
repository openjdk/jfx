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

#include <string.h>

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
 * Returns: The profile as a const string and %NULL if the profile could not be
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
 * Returns: The level as a const string and %NULL if the level could not be
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

  num_channels = num_sce + (2 * num_cpe) + num_lfe;

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
 * Returns: The profile as a const string, or %NULL if there is an error.
 */
const gchar *
gst_codec_utils_h264_get_profile (const guint8 * sps, guint len)
{
  const gchar *profile = NULL;
  gint csf1, csf3, csf5;

  g_return_val_if_fail (sps != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("SPS", sps, len);

  csf1 = (sps[1] & 0x40) >> 6;
  csf3 = (sps[1] & 0x10) >> 4;
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
      profile = "high";
      break;
    case 110:
      if (csf3)
        profile = "high-10-intra";
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
 * Returns: The level as a const string, or %NULL if there is an error.
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
 * * Bit 44:87 - general_reserved_zero_44bits
 * * Bit 88:95 - general_level_idc
 *
 * Returns: The profile as a const string, or %NULL if there is an error.
 *
 * Since: 1.4
 */
const gchar *
gst_codec_utils_h265_get_profile (const guint8 * profile_tier_level, guint len)
{
  const gchar *profile = NULL;
  gint profile_idc;

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
  else
    profile = NULL;

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
 * Returns: The tier as a const string, or %NULL if there is an error.
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
 * Returns: The level as a const string, or %NULL if there is an error.
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
 * Returns: The profile as a const string, or NULL if there is an error.
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
 * Returns: The level as a const string, or NULL if there is an error.
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
 * @rate: (out): the sample rate
 * @channels: (out): the number of channels
 * @channel_mapping_family: (out): the channel mapping family
 * @stream_count: (out): the number of independent streams
 * @coupled_count: (out): the number of stereo streams
 * @channel_mapping: (out) (array fixed-size=256): the mapping between the streams
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

  if (!gst_structure_get_int (s, "channels", &c)) {
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
 * @channel_mapping: (allow-none) (array): the mapping between the streams
 *
 * Creates Opus caps from the given parameters.
 *
 * Returns: The #GstCaps, or %NULL if the parameters would lead to
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
 * @comments: (allow-none): Comment header or NULL
 *
 * Creates Opus caps from the given OpusHead @header and comment header
 * @comments.
 *
 * Returns: The #GstCaps.
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
 * @channel_mapping: (allow-none) (array): the mapping between the streams
 * @pre_skip: Pre-skip in 48kHz samples or 0
 * @output_gain: Output gain or 0
 *
 * Creates OpusHead header from the given parameters.
 *
 * Returns: The #GstBuffer containing the OpusHead.
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
 * @rate: (out): the sample rate
 * @channels: (out): the number of channels
 * @channel_mapping_family: (out): the channel mapping family
 * @stream_count: (out): the number of independent streams
 * @coupled_count: (out): the number of stereo streams
 * @channel_mapping: (out) (array fixed-size=256): the mapping between the streams
 * @pre_skip: (out): Pre-skip in 48kHz samples or 0
 * @output_gain: (out): Output gain or 0
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
#endif // GSTREAMER_LITE
