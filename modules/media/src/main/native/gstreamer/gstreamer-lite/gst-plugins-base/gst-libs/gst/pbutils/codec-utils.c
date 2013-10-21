/* GStreamer base utils library codec-specific utility functions
 * Copyright (C) 2010 Arun Raghavan <arun.raghavan@collabora.co.uk>
 *               2010 Collabora Multimedia
 *               2010 Nokia Corporation
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
 * SECTION:gstpbutilscodecutils
 * @short_description: Miscellaneous codec-specific utility functions
 *
 * <refsect2>
 * <para>
 * Provides codec-specific ulility functions such as functions to provide the
 * codec profile and level in human-readable string form from header data.
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "pbutils.h"

#define GST_SIMPLE_CAPS_HAS_NAME(caps,name) \
    gst_structure_has_name(gst_caps_get_structure((caps),0),(name))

#define GST_SIMPLE_CAPS_HAS_FIELD(caps,field) \
    gst_structure_has_field(gst_caps_get_structure((caps),0),(field))

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
 *
 * Since: 0.10.31
 */
guint
gst_codec_utils_aac_get_sample_rate_from_index (guint sr_idx)
{
  static const guint aac_sample_rates[] = { 96000, 88200, 64000, 48000, 44100,
    32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350
  };

  if (G_LIKELY (sr_idx < G_N_ELEMENTS (aac_sample_rates)))
    return aac_sample_rates[sr_idx];

  GST_WARNING ("Invalid sample rate index %u", sr_idx);
  return 0;
}

/**
 * gst_codec_utils_aac_get_profile:
 * @audio_config: a pointer to the AudioSpecificConfig as specified in the
 *                Elementary Stream Descriptor (esds) in ISO/IEC 14496-1 (see
 *                gst_codec_utils_aac_get_level() for a more details).
 * @len: Length of @audio_config in bytes
 *
 * Returns the profile of the given AAC stream as a string. The profile is
 * determined using the AudioObjectType field which is in the first 5 bits of
 * @audio_config.
 *
 * <note>
 * HE-AAC support has not yet been implemented.
 * </note>
 *
 * Returns: The profile as a const string and %NULL if the profile could not be
 * determined.
 *
 * Since: 0.10.31
 */
const gchar *
gst_codec_utils_aac_get_profile (const guint8 * audio_config, guint len)
{
  guint profile;

  if (len < 1)
    return NULL;

  GST_MEMDUMP ("audio config", audio_config, len);

  profile = audio_config[0] >> 3;
  switch (profile) {
    case 1:
      return "main";
    case 2:
      return "lc";
    case 3:
      return "ssr";
    case 4:
      return "ltp";
    default:
      break;
  }

  GST_DEBUG ("Invalid profile idx: %u", profile);
  return NULL;
}

/**
 * gst_codec_utils_aac_get_level:
 * @audio_config: a pointer to the AudioSpecificConfig as specified in the
 *                Elementary Stream Descriptor (esds) in ISO/IEC 14496-1.
 * @len: Length of @audio_config in bytes
 *
 * Determines the level of a stream as defined in ISO/IEC 14496-3. For AAC LC
 * streams, the constraints from the AAC audio profile are applied. For AAC
 * Main, LTP, SSR and others, the Main profile is used.
 *
 * The @audio_config parameter follows the following format, starting from the
 * most significant bit of the first byte:
 *
 * <itemizedlist>
 *   <listitem><para>
 *     Bit 0:4 contains the AudioObjectType
 *   </para></listitem>
 *   <listitem><para>
 *     Bit 5:8 contains the sample frequency index (if this is 0xf, then the
 *             next 24 bits define the actual sample frequency, and subsequent
 *             fields are appropriately shifted).
 *    </para></listitem>
 *   <listitem><para>
 *     Bit 9:12 contains the channel configuration
 *   </para></listitem>
 * </itemizedlist>
 *
 * <note>
 * HE-AAC support has not yet been implemented.
 * </note>
 *
 * Returns: The level as a const string and %NULL if the level could not be
 * determined.
 *
 * Since: 0.10.31
 */
const gchar *
gst_codec_utils_aac_get_level (const guint8 * audio_config, guint len)
{
  int profile, sr_idx, channel_config, rate;
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
  int pcu, rcu, pcu_ref, rcu_ref;
  int ret = -1;

  g_return_val_if_fail (audio_config != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("audio config", audio_config, len);

  profile = audio_config[0] >> 3;
  /* FIXME: add support for sr_idx = 0xf */
  sr_idx = ((audio_config[0] & 0x7) << 1) | ((audio_config[1] & 0x80) >> 7);
  rate = gst_codec_utils_aac_get_sample_rate_from_index (sr_idx);
  channel_config = (audio_config[1] & 0x7f) >> 3;

  if (rate == 0)
    return NULL;

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
      /* front left, right, center and LFE; outside front left and right;
       * rear left and right surround */
      num_sce = 1;
      num_cpe = 3;
      num_lfe = 1;
      break;
    default:
      GST_WARNING ("Unknown channel config in header: %d", channel_config);
      return NULL;
  }

  switch (profile) {
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

  if (profile == 2) {
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
        "channel_config=%u, pcu=%d,rcu=%d", profile, rate, channel_config, pcu,
        rcu);
    return NULL;
  } else {
    return digit_to_string (ret);
  }
}

/**
 * gst_codec_utils_aac_caps_set_level_and_profile:
 * @caps: the #GstCaps to which level and profile fields are to be added
 * @audio_config: a pointer to the AudioSpecificConfig as specified in the
 *                Elementary Stream Descriptor (esds) in ISO/IEC 14496-1 (see
 *                below for a more details).
 * @len: Length of @audio_config in bytes
 *
 * Sets the level and profile on @caps if it can be determined from
 * @audio_config. See gst_codec_utils_aac_get_level() and
 * gst_codec_utils_aac_get_profile() for more details on the parameters.
 * @caps must be audio/mpeg caps with an "mpegversion" field of either 2 or 4.
 * If mpegversion is 4, the "base-profile" field is also set in @caps.
 *
 * Returns: %TRUE if the level and profile could be set, %FALSE otherwise.
 *
 * Since: 0.10.31
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
 * @sps: Pointer to the sequence parameter set for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the profile indication (profile_idc) in the stream's
 * sequence parameter set into a string. The SPS is expected to have the
 * following format, as defined in the H.264 specification. The SPS is viewed
 * as a bitstream here, with bit 0 being the most significant bit of the first
 * byte.
 *
 * <itemizedlist>
 * <listitem><para>Bit 0:7   - Profile indication</para></listitem>
 * <listitem><para>Bit 8     - constraint_set0_flag</para></listitem>
 * <listitem><para>Bit 9     - constraint_set1_flag</para></listitem>
 * <listitem><para>Bit 10    - constraint_set2_flag</para></listitem>
 * <listitem><para>Bit 11    - constraint_set3_flag</para></listitem>
 * <listitem><para>Bit 12    - constraint_set3_flag</para></listitem>
 * <listitem><para>Bit 13:15 - Reserved</para></listitem>
 * <listitem><para>Bit 16:24 - Level indication</para></listitem>
 * </itemizedlist>
 *
 * Returns: The profile as a const string, or %NULL if there is an error.
 *
 * Since: 0.10.31
 */
const gchar *
gst_codec_utils_h264_get_profile (const guint8 * sps, guint len)
{
  const gchar *profile = NULL;
  gint csf1, csf3;

  g_return_val_if_fail (sps != NULL, NULL);

  if (len < 2)
    return NULL;

  GST_MEMDUMP ("SPS", sps, len);

  csf1 = (sps[1] & 0x40) >> 6;
  csf3 = (sps[1] & 0x10) >> 4;

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
    default:
      return NULL;
  }

  return profile;
}

/**
 * gst_codec_utils_h264_get_level:
 * @sps: Pointer to the sequence parameter set for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the level indication (level_idc) in the stream's
 * sequence parameter set into a string. The SPS is expected to have the
 * same format as for gst_codec_utils_h264_get_profile().
 *
 * Returns: The level as a const string, or %NULL if there is an error.
 *
 * Since: 0.10.31
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

  if (sps[2] == 11 && csf3)
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
      default:
        return NULL;
    }
  }
}

/**
 * gst_codec_utils_h264_caps_set_level_and_profile:
 * @caps: the #GstCaps to which the level and profile are to be added
 * @sps: Pointer to the sequence parameter set for the stream.
 * @len: Length of the data available in @sps.
 *
 * Sets the level and profile in @caps if it can be determined from @sps. See
 * gst_codec_utils_h264_get_level() and gst_codec_utils_h264_get_profile()
 * for more details on the parameters.
 *
 * Returns: %TRUE if the level and profile could be set, %FALSE otherwise.
 *
 * Since: 0.10.31
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
 * gst_codec_utils_mpeg4video_get_profile:
 * @vis_obj_seq: Pointer to the visual object sequence for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the profile indication in the stream's visual object sequence into
 * a string. @vis_obj_seq is expected to be the data following the visual
 * object sequence start code. Only the first byte
 * (profile_and_level_indication) is used.
 *
 * Returns: The profile as a const string, or NULL if there is an error.
 *
 * Since: 0.10.31
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
 * @vis_obj_seq: Pointer to the visual object sequence for the stream.
 * @len: Length of the data available in @sps.
 *
 * Converts the level indication in the stream's visual object sequence into
 * a string. @vis_obj_seq is expected to be the data following the visual
 * object sequence start code. Only the first byte
 * (profile_and_level_indication) is used.
 *
 * Returns: The level as a const string, or NULL if there is an error.
 *
 * Since: 0.10.31
 */
const gchar *
gst_codec_utils_mpeg4video_get_level (const guint8 * vis_obj_seq, guint len)
{
  /* The profile/level codes are from 14496-2, table G-1, and the Wireshark
   * sources: epan/dissectors/packet-mp4ves.c
   *
   * Each profile has a different maximum level it defines. Some of them still
   * need special case handling, because not all levels start from 1, and the
   * Simple profile defines an intermediate level as well. */
  static const int level_max[] = { 3, 2, 2, 4, 2, 1, 2, 2, 2, 4, 3, 4, 2, 3, 4,
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
      if (level_id == 7 && level_id > 0xd)
        return NULL;
      break;
  }

  if (profile_id == 0 && level_id == 8)
    /* Simple Profile / Level 0 */
    return "0";
  else if (profile_id == 0 && level_id == 9)
    /* Simple Profile / Level 0b */
    return "0b";
  else if (level_id <= level_max[profile_id])
    /* Levels for all other cases */
    return digit_to_string (level_id);

  return NULL;
}

/**
 * gst_codec_utils_mpeg4video_caps_set_level_and_profile:
 * @caps: the #GstCaps to which the level and profile are to be added
 * @vis_obj_seq: Pointer to the visual object sequence for the stream.
 * @len: Length of the data available in @sps.
 *
 * Sets the level and profile in @caps if it can be determined from
 * @vis_obj_seq. See gst_codec_utils_mpeg4video_get_level() and
 * gst_codec_utils_mpeg4video_get_profile() for more details on the
 * parameters.
 *
 * Returns: %TRUE if the level and profile could be set, %FALSE otherwise.
 *
 * Since: 0.10.31
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
