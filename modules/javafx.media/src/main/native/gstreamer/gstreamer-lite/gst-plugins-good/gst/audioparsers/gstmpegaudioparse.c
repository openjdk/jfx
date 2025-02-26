/* GStreamer MPEG audio parser
 * Copyright (C) 2006-2007 Jan Schmidt <thaytan@mad.scientist.com>
 * Copyright (C) 2010 Mark Nauwelaerts <mnauw users sf net>
 * Copyright (C) 2010 Nokia Corporation. All rights reserved.
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
/**
 * SECTION:element-mpegaudioparse
 * @title: mpegaudioparse
 * @short_description: MPEG audio parser
 * @see_also: #GstAmrParse, #GstAACParse
 *
 * Parses and frames mpeg1 audio streams. Provides seeking.
 *
 * ## Example launch line
 * |[
 * gst-launch-1.0 filesrc location=test.mp3 ! mpegaudioparse ! mpg123audiodec
 *  ! audioconvert ! audioresample ! autoaudiosink
 * ]|
 *
 */

/* Notes about gapless playback, "Frankenstein" streams, and the Xing header frame:
 *
 * Gapless playback is based on the LAME tag, which is located in the Xing
 * header frame. The tag contains the encoder delay and encoder padding.
 * The encoder delay specifies how many padding nullsamples have been prepended
 * by the encoder at the start of the mp3 stream, while the encoder padding
 * specifies how many padding nullsamples got added at the end of the stream.
 *
 * In addition, there is also a "decoder delay". This affects all existing
 * mp3 decoders - they themselves introduce a delay into the signal due to
 * the way mp3 decoding works. This delay is 529 samples long in all known
 * decoders. Unlike the encoder delay, the decoder delay is not specified
 * anywhere in the mp3 stream. Players/decoders therefore hardcode the
 * decoder delay as 529 samples.
 *
 * (The LAME tech FAQ mentions 528 samples instead of 529, but LAME seems to
 * use 529 samples. Also, decoders like mpg123 use 529 samples instead of 528.
 * The situation is a little unclear, but 529 samples seems to be standard.)
 *
 * For proper gapless playback, both mpegaudioparse and a downstream MPEG
 * audio decoder must do their part. mpegaudioparse adjusts buffer PTS/DTS
 * and durations, and adds GstAudioClippingMeta to outgoing buffers if
 * clipping is necessary. MPEG decoders then clip decoded frames according
 * to that meta (if present).
 *
 * To detect when to add GstAudioClippingMeta and when to adjust PTS/DTS/
 * durations, the number of the current frame is retrieved. Based on that, the
 * current stream position in samples is calculated. With the sample position,
 * it is determined whether or not the current playback position is still
 * if the actual playback range (= in the actual playback range of the stream
 * that excludes padding samples), or if it is already outside, or partially
 * outside.
 *
 * start_of_actual_samples and end_of_actual_samples define the start/end
 * of this actual playback range, in samples. So:
 * If sample_pos >= start_of_actual_samples and sample_pos end_of_actual_samples
 * -> sample_pos is inside the actual playback range.
 *
 * (The decoder delay could in theory be left for the decoder to worry
 * about. But then, the decoder would also have to adjust PTS/DTS/durations
 * of decoded buffers, which is not something a GstAudioDecoder based element
 * should have to deal with. So, for convenience, mpegaudioparse also factors
 * that delay into its calculations.)
 *
 *
 * "Frankenstein" streams are MPEG streams which have streams beyond
 * what the Xing metadata indicates. Such streams typically are the
 * result of poorly stitching individual mp3s together, like this:
 *
 *   cat first.mp3 second.mp3 > joined.mp3
 *
 * The resulting mp3 is not guaranteed to be valid. In particular, this can
 * cause confusion when first.mp3 contains a Xing header frame. Its length
 * indicator then does not match the actual length (which is bigger). When
 * this is detected, a log line about this being a Frankenstein stream is
 * generated.
 *
 *
 * Xing header frames are empty dummy MPEG frames. They only exist for
 * supplying metadata. They are encoded as valid silent MPEG frames for
 * backwards compatibility with older hardware MP3 players, but can be safely
 * dropped.
 *
 * For more about Xing header frames, see:
 * https://www.codeproject.com/Articles/8295/MPEG-Audio-Frame-Header#XINGHeader
 * https://www.compuphase.com/mp3/mp3loops.htm#PADDING_DELAYS
 *
 * To facilitate gapless playback and ensure that MPEG audio decoders don't
 * actually decode this frame as an empty MPEG frame, it is marked here as
 * GST_BUFFER_FLAG_DECODE_ONLY / GST_BUFFER_FLAG_DROPPABLE in mpegaudioparse
 * after its metadata got extracted. It is also marked as such if it is
 * encountered again after the user for example seeked back to the beginning
 * of the mp3 stream. Its duration is also set to zero to make sure that the
 * frame does not cause baseparse to increment the timestamp of the frame that
 * follows this one.
 *
 */

/* FIXME: we should make the base class (GstBaseParse) aware of the
 * XING seek table somehow, so it can use it properly for things like
 * accurate seeks. Currently it can only do a lookup via the convert function,
 * but then doesn't know what the result represents exactly. One could either
 * add a vfunc for index lookup, or just make mpegaudioparse populate the
 * base class's index via the API provided.
 */
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>

#include "gstaudioparserselements.h"
#include "gstmpegaudioparse.h"
#include <gst/base/gstbytereader.h>
#include <gst/pbutils/pbutils.h>

GST_DEBUG_CATEGORY_STATIC (mpeg_audio_parse_debug);
#define GST_CAT_DEFAULT mpeg_audio_parse_debug

#define MPEG_AUDIO_CHANNEL_MODE_UNKNOWN -1
#define MPEG_AUDIO_CHANNEL_MODE_STEREO 0
#define MPEG_AUDIO_CHANNEL_MODE_JOINT_STEREO 1
#define MPEG_AUDIO_CHANNEL_MODE_DUAL_CHANNEL 2
#define MPEG_AUDIO_CHANNEL_MODE_MONO 3

#define CRC_UNKNOWN -1
#define CRC_PROTECTED 0
#define CRC_NOT_PROTECTED 1

#define XING_FRAMES_FLAG     0x0001
#define XING_BYTES_FLAG      0x0002
#define XING_TOC_FLAG        0x0004
#define XING_VBR_SCALE_FLAG  0x0008

#define MIN_FRAME_SIZE       6

static GstStaticPadTemplate src_template = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/mpeg, "
        "mpegversion = (int) 1, "
        "layer = (int) [ 1, 3 ], "
        "mpegaudioversion = (int) [ 1, 3], "
        "rate = (int) [ 8000, 48000 ], "
        "channels = (int) [ 1, 2 ], " "parsed=(boolean) true")
    );

static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/mpeg, mpegversion = (int) 1")
    );

static void gst_mpeg_audio_parse_finalize (GObject * object);

static gboolean gst_mpeg_audio_parse_start (GstBaseParse * parse);
static gboolean gst_mpeg_audio_parse_stop (GstBaseParse * parse);
static GstFlowReturn gst_mpeg_audio_parse_handle_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame, gint * skipsize);
static GstFlowReturn gst_mpeg_audio_parse_pre_push_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame);
static gboolean gst_mpeg_audio_parse_src_query (GstBaseParse * parse,
    GstQuery * query);
static gboolean gst_mpeg_audio_parse_sink_event (GstBaseParse * parse,
    GstEvent * event);
static gboolean gst_mpeg_audio_parse_convert (GstBaseParse * parse,
    GstFormat src_format, gint64 src_value,
    GstFormat dest_format, gint64 * dest_value);
static GstCaps *gst_mpeg_audio_parse_get_sink_caps (GstBaseParse * parse,
    GstCaps * filter);

static gboolean
gst_mpeg_audio_parse_check_if_is_xing_header_frame (GstMpegAudioParse *
    mp3parse, GstBuffer * buf);

static void gst_mpeg_audio_parse_handle_first_frame (GstMpegAudioParse *
    mp3parse, GstBuffer * buf);

#define gst_mpeg_audio_parse_parent_class parent_class
G_DEFINE_TYPE (GstMpegAudioParse, gst_mpeg_audio_parse, GST_TYPE_BASE_PARSE);
GST_ELEMENT_REGISTER_DEFINE (mpegaudioparse, "mpegaudioparse",
    GST_RANK_PRIMARY + 2, GST_TYPE_MPEG_AUDIO_PARSE);

#define GST_TYPE_MPEG_AUDIO_CHANNEL_MODE  \
    (gst_mpeg_audio_channel_mode_get_type())

static const GEnumValue mpeg_audio_channel_mode[] = {
  {MPEG_AUDIO_CHANNEL_MODE_UNKNOWN, "Unknown", "unknown"},
  {MPEG_AUDIO_CHANNEL_MODE_MONO, "Mono", "mono"},
  {MPEG_AUDIO_CHANNEL_MODE_DUAL_CHANNEL, "Dual Channel", "dual-channel"},
  {MPEG_AUDIO_CHANNEL_MODE_JOINT_STEREO, "Joint Stereo", "joint-stereo"},
  {MPEG_AUDIO_CHANNEL_MODE_STEREO, "Stereo", "stereo"},
  {0, NULL, NULL},
};

static GType
gst_mpeg_audio_channel_mode_get_type (void)
{
  static GType mpeg_audio_channel_mode_type = 0;

  if (!mpeg_audio_channel_mode_type) {
    mpeg_audio_channel_mode_type =
        g_enum_register_static ("GstMpegAudioChannelMode",
        mpeg_audio_channel_mode);
  }
  return mpeg_audio_channel_mode_type;
}

static const gchar *
gst_mpeg_audio_channel_mode_get_nick (gint mode)
{
  guint i;
  for (i = 0; i < G_N_ELEMENTS (mpeg_audio_channel_mode); i++) {
    if (mpeg_audio_channel_mode[i].value == mode)
      return mpeg_audio_channel_mode[i].value_nick;
  }
  return NULL;
}

static void
gst_mpeg_audio_parse_class_init (GstMpegAudioParseClass * klass)
{
  GstBaseParseClass *parse_class = GST_BASE_PARSE_CLASS (klass);
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *object_class = G_OBJECT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (mpeg_audio_parse_debug, "mpegaudioparse", 0,
      "MPEG1 audio stream parser");

  object_class->finalize = gst_mpeg_audio_parse_finalize;

  parse_class->start = GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_start);
  parse_class->stop = GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_stop);
  parse_class->handle_frame =
      GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_handle_frame);
  parse_class->pre_push_frame =
      GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_pre_push_frame);
  parse_class->src_query = GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_src_query);
  parse_class->sink_event = GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_sink_event);
  parse_class->convert = GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_convert);
  parse_class->get_sink_caps =
      GST_DEBUG_FUNCPTR (gst_mpeg_audio_parse_get_sink_caps);

  /* register tags */
#define GST_TAG_CRC      "has-crc"
#define GST_TAG_MODE     "channel-mode"

  gst_tag_register (GST_TAG_CRC, GST_TAG_FLAG_META, G_TYPE_BOOLEAN,
      "has crc", "Using CRC", NULL);
  gst_tag_register (GST_TAG_MODE, GST_TAG_FLAG_ENCODED, G_TYPE_STRING,
      "channel mode", "MPEG audio channel mode", NULL);

  g_type_class_ref (GST_TYPE_MPEG_AUDIO_CHANNEL_MODE);

  gst_element_class_add_static_pad_template (element_class, &sink_template);
  gst_element_class_add_static_pad_template (element_class, &src_template);

  gst_element_class_set_static_metadata (element_class, "MPEG1 Audio Parser",
      "Codec/Parser/Audio",
      "Parses and frames mpeg1 audio streams (levels 1-3), provides seek",
      "Jan Schmidt <thaytan@mad.scientist.com>,"
      "Mark Nauwelaerts <mark.nauwelaerts@collabora.co.uk>");
}

static void
gst_mpeg_audio_parse_reset (GstMpegAudioParse * mp3parse)
{
  mp3parse->upstream_format = GST_FORMAT_UNDEFINED;
  mp3parse->channels = -1;
  mp3parse->rate = -1;
  mp3parse->sent_codec_tag = FALSE;
  mp3parse->last_posted_crc = CRC_UNKNOWN;
  mp3parse->last_posted_channel_mode = MPEG_AUDIO_CHANNEL_MODE_UNKNOWN;
  mp3parse->freerate = 0;
  mp3parse->spf = 0;

  mp3parse->outgoing_frame_is_xing_header = FALSE;

  mp3parse->hdr_bitrate = 0;
  mp3parse->bitrate_is_constant = TRUE;

  mp3parse->xing_flags = 0;
  mp3parse->xing_bitrate = 0;
  mp3parse->xing_frames = 0;
  mp3parse->xing_total_time = 0;
  mp3parse->xing_bytes = 0;
  mp3parse->xing_vbr_scale = 0;
  memset (mp3parse->xing_seek_table, 0, sizeof (mp3parse->xing_seek_table));
  memset (mp3parse->xing_seek_table_inverse, 0,
      sizeof (mp3parse->xing_seek_table_inverse));

  mp3parse->vbri_bitrate = 0;
  mp3parse->vbri_frames = 0;
  mp3parse->vbri_total_time = 0;
  mp3parse->vbri_bytes = 0;
  mp3parse->vbri_seek_points = 0;
  g_free (mp3parse->vbri_seek_table);
  mp3parse->vbri_seek_table = NULL;

  mp3parse->encoder_delay = 0;
  mp3parse->encoder_padding = 0;
  mp3parse->decoder_delay = 0;
  mp3parse->start_of_actual_samples = 0;
  mp3parse->end_of_actual_samples = 0;
  mp3parse->total_padding_time = GST_CLOCK_TIME_NONE;
  mp3parse->start_padding_time = GST_CLOCK_TIME_NONE;
  mp3parse->end_padding_time = GST_CLOCK_TIME_NONE;
}

static void
gst_mpeg_audio_parse_init (GstMpegAudioParse * mp3parse)
{
  gst_mpeg_audio_parse_reset (mp3parse);
  GST_PAD_SET_ACCEPT_INTERSECT (GST_BASE_PARSE_SINK_PAD (mp3parse));
  GST_PAD_SET_ACCEPT_TEMPLATE (GST_BASE_PARSE_SINK_PAD (mp3parse));
}

static void
gst_mpeg_audio_parse_finalize (GObject * object)
{
  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static gboolean
gst_mpeg_audio_parse_start (GstBaseParse * parse)
{
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);

  gst_base_parse_set_min_frame_size (GST_BASE_PARSE (mp3parse), MIN_FRAME_SIZE);
  GST_DEBUG_OBJECT (parse, "starting");

  gst_mpeg_audio_parse_reset (mp3parse);

  return TRUE;
}

static gboolean
gst_mpeg_audio_parse_stop (GstBaseParse * parse)
{
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);

  GST_DEBUG_OBJECT (parse, "stopping");

  gst_mpeg_audio_parse_reset (mp3parse);

  return TRUE;
}

static const guint mp3types_bitrates[2][3][16] = {
  {
        {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448,},
        {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384,},
        {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320,}
      },
  {
        {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256,},
        {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160,},
        {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160,}
      },
};

static const guint mp3types_freqs[3][3] = { {44100, 48000, 32000},
{22050, 24000, 16000},
{11025, 12000, 8000}
};

static inline guint
mp3_type_frame_length_from_header (GstMpegAudioParse * mp3parse, guint32 header,
    guint * put_version, guint * put_layer, guint * put_channels,
    guint * put_bitrate, guint * put_samplerate, guint * put_mode,
    guint * put_crc)
{
  guint length;
  gulong mode, samplerate, bitrate, layer, channels, padding, crc;
  gulong version;
  gint lsf, mpg25;

  if (header & (1 << 20)) {
    lsf = (header & (1 << 19)) ? 0 : 1;
    mpg25 = 0;
  } else {
    lsf = 1;
    mpg25 = 1;
  }

  version = 1 + lsf + mpg25;

  layer = 4 - ((header >> 17) & 0x3);

  crc = (header >> 16) & 0x1;

  bitrate = (header >> 12) & 0xF;
  bitrate = mp3types_bitrates[lsf][layer - 1][bitrate] * 1000;
  if (!bitrate) {
    GST_LOG_OBJECT (mp3parse, "using freeform bitrate");
    bitrate = mp3parse->freerate;
  }

  samplerate = (header >> 10) & 0x3;
  samplerate = mp3types_freqs[lsf + mpg25][samplerate];

  /* force 0 length if 0 bitrate */
  padding = (bitrate > 0) ? (header >> 9) & 0x1 : 0;

  mode = (header >> 6) & 0x3;
  channels = (mode == 3) ? 1 : 2;

  switch (layer) {
    case 1:
      length = 4 * ((bitrate * 12) / samplerate + padding);
      break;
    case 2:
      length = (bitrate * 144) / samplerate + padding;
      break;
    default:
    case 3:
      length = (bitrate * 144) / (samplerate << lsf) + padding;
      break;
  }

  GST_DEBUG_OBJECT (mp3parse, "Calculated mp3 frame length of %u bytes",
      length);
  GST_DEBUG_OBJECT (mp3parse, "samplerate = %lu, bitrate = %lu, version = %lu, "
      "layer = %lu, channels = %lu, mode = %s", samplerate, bitrate, version,
      layer, channels, gst_mpeg_audio_channel_mode_get_nick (mode));

  if (put_version)
    *put_version = version;
  if (put_layer)
    *put_layer = layer;
  if (put_channels)
    *put_channels = channels;
  if (put_bitrate)
    *put_bitrate = bitrate;
  if (put_samplerate)
    *put_samplerate = samplerate;
  if (put_mode)
    *put_mode = mode;
  if (put_crc)
    *put_crc = crc;

  return length;
}

/* Minimum number of consecutive, valid-looking frames to consider
 * for resyncing */
#define MIN_RESYNC_FRAMES 3

/* Perform extended validation to check that subsequent headers match
 * the first header given here in important characteristics, to avoid
 * false sync. We look for a minimum of MIN_RESYNC_FRAMES consecutive
 * frames to match their major characteristics.
 *
 * If at_eos is set to TRUE, we just check that we don't find any invalid
 * frames in whatever data is available, rather than requiring a full
 * MIN_RESYNC_FRAMES of data.
 *
 * Returns TRUE if we've seen enough data to validate or reject the frame.
 * If TRUE is returned, then *valid contains TRUE if it validated, or false
 * if we decided it was false sync.
 * If FALSE is returned, then *valid contains minimum needed data.
 */
static gboolean
gst_mp3parse_validate_extended (GstMpegAudioParse * mp3parse, GstBuffer * buf,
    guint32 header, int bpf, gboolean at_eos, gint * valid)
{
  guint32 next_header;
  GstMapInfo map;
  gboolean res = TRUE;
  int frames_found = 1;
  int offset = bpf;

  gst_buffer_map (buf, &map, GST_MAP_READ);

  while (frames_found < MIN_RESYNC_FRAMES) {
    /* Check if we have enough data for all these frames, plus the next
       frame header. */
    if (map.size < offset + 4) {
      if (at_eos) {
        /* Running out of data at EOS is fine; just accept it */
        *valid = TRUE;
        goto cleanup;
      } else {
        *valid = offset + 4;
        res = FALSE;
        goto cleanup;
      }
    }

    next_header = GST_READ_UINT32_BE (map.data + offset);
    GST_DEBUG_OBJECT (mp3parse, "At %d: header=%08X, header2=%08X, bpf=%d",
        offset, (unsigned int) header, (unsigned int) next_header, bpf);

/* mask the bits which are allowed to differ between frames */
#define HDRMASK ~((0xF << 12)  /* bitrate */ | \
                  (0x1 <<  9)  /* padding */ | \
                  (0xf <<  4)  /* mode|mode extension */ | \
                  (0xf))        /* copyright|emphasis */

    if ((next_header & HDRMASK) != (header & HDRMASK)) {
      /* If any of the unmasked bits don't match, then it's not valid */
      GST_DEBUG_OBJECT (mp3parse, "next header doesn't match "
          "(header=%08X (%08X), header2=%08X (%08X), bpf=%d)",
          (guint) header, (guint) header & HDRMASK, (guint) next_header,
          (guint) next_header & HDRMASK, bpf);
      *valid = FALSE;
      goto cleanup;
    } else if (((next_header >> 12) & 0xf) == 0xf) {
      /* The essential parts were the same, but the bitrate held an
         invalid value - also reject */
      GST_DEBUG_OBJECT (mp3parse, "next header invalid (bitrate)");
      *valid = FALSE;
      goto cleanup;
    }

    bpf = mp3_type_frame_length_from_header (mp3parse, next_header,
        NULL, NULL, NULL, NULL, NULL, NULL, NULL);

    /* if no bitrate, and no freeform rate known, then fail */
    if (G_UNLIKELY (!bpf)) {
      GST_DEBUG_OBJECT (mp3parse, "next header invalid (bitrate 0)");
      *valid = FALSE;
      goto cleanup;
    }

    offset += bpf;
    frames_found++;
  }

  *valid = TRUE;

cleanup:
  gst_buffer_unmap (buf, &map);
  return res;
}

static gboolean
gst_mpeg_audio_parse_head_check (GstMpegAudioParse * mp3parse,
    unsigned long head)
{
  GST_DEBUG_OBJECT (mp3parse, "checking mp3 header 0x%08lx", head);
  /* if it's not a valid sync */
  if ((head & 0xffe00000) != 0xffe00000) {
    GST_WARNING_OBJECT (mp3parse, "invalid sync");
    return FALSE;
  }
  /* if it's an invalid MPEG version */
  if (((head >> 19) & 3) == 0x1) {
    GST_WARNING_OBJECT (mp3parse, "invalid MPEG version: 0x%lx",
        (head >> 19) & 3);
    return FALSE;
  }
  /* if it's an invalid layer */
  if (!((head >> 17) & 3)) {
    GST_WARNING_OBJECT (mp3parse, "invalid layer: 0x%lx", (head >> 17) & 3);
    return FALSE;
  }
  /* if it's an invalid bitrate */
#ifdef GSTREAMER_LITE
  // Lets disable free format, since it is not supported by dshowwrapper.
  // It was enabled with JDK-8199527 (GStreamer 1.14), we disabling it in same
  // way as before 1.14. This required to fix issue with some MP3 files.
  // See JDK-8213510.
  if (((head >> 12) & 0xf) == 0x0) {
    GST_WARNING_OBJECT (mp3parse, "invalid bitrate: 0x%lx."
        "Free format files are not supported yet", (head >> 12) & 0xf);
    return FALSE;
  }
#endif // GSTREAMER_LITE
  if (((head >> 12) & 0xf) == 0xf) {
    GST_WARNING_OBJECT (mp3parse, "invalid bitrate: 0x%lx", (head >> 12) & 0xf);
    return FALSE;
  }
  /* if it's an invalid samplerate */
  if (((head >> 10) & 0x3) == 0x3) {
    GST_WARNING_OBJECT (mp3parse, "invalid samplerate: 0x%lx",
        (head >> 10) & 0x3);
    return FALSE;
  }

  if ((head & 0x3) == 0x2) {
    /* Ignore this as there are some files with emphasis 0x2 that can
     * be played fine. See BGO #537235 */
    GST_WARNING_OBJECT (mp3parse, "invalid emphasis: 0x%lx", head & 0x3);
  }

  return TRUE;
}

/* Determines possible freeform frame rate/size by looking for next
 * header with valid bitrate (0 or otherwise valid) (and sufficiently
 * matching current header).
 *
 * Returns TRUE if we've found such one, and *rate then contains rate
 * (or *rate contains 0 if decided no freeframe size could be determined).
 * If not enough data, returns FALSE.
 */
static gboolean
gst_mp3parse_find_freerate (GstMpegAudioParse * mp3parse, GstMapInfo * map,
    guint32 header, gboolean at_eos, gint * _rate)
{
  guint32 next_header;
  const guint8 *data;
  guint available;
  int offset = 4;
  gulong samplerate, rate, layer, padding;
  gboolean valid;
  gint lsf, mpg25;

  available = map->size;
  data = map->data;

  *_rate = 0;

  /* pick apart header again partially */
  if (header & (1 << 20)) {
    lsf = (header & (1 << 19)) ? 0 : 1;
    mpg25 = 0;
  } else {
    lsf = 1;
    mpg25 = 1;
  }
  layer = 4 - ((header >> 17) & 0x3);
  samplerate = (header >> 10) & 0x3;
  samplerate = mp3types_freqs[lsf + mpg25][samplerate];
  padding = (header >> 9) & 0x1;

  for (; offset < available; ++offset) {
    /* Check if we have enough data for all these frames, plus the next
       frame header. */
    if (available < offset + 4) {
      if (at_eos) {
        /* Running out of data; failed to determine size */
        return TRUE;
      } else {
        return FALSE;
      }
    }

    valid = FALSE;
    next_header = GST_READ_UINT32_BE (data + offset);
    if ((next_header & 0xFFE00000) != 0xFFE00000)
      goto next;

    GST_DEBUG_OBJECT (mp3parse, "At %d: header=%08X, header2=%08X",
        offset, (unsigned int) header, (unsigned int) next_header);

    if ((next_header & HDRMASK) != (header & HDRMASK)) {
      /* If any of the unmasked bits don't match, then it's not valid */
      GST_DEBUG_OBJECT (mp3parse, "next header doesn't match "
          "(header=%08X (%08X), header2=%08X (%08X))",
          (guint) header, (guint) header & HDRMASK, (guint) next_header,
          (guint) next_header & HDRMASK);
      goto next;
    } else if (((next_header >> 12) & 0xf) == 0xf) {
      /* The essential parts were the same, but the bitrate held an
         invalid value - also reject */
      GST_DEBUG_OBJECT (mp3parse, "next header invalid (bitrate)");
      goto next;
    }

    valid = TRUE;

  next:
    /* almost accept as free frame */
    if (layer == 1) {
      rate = samplerate * (offset - 4 * padding + 4) / 48000;
    } else {
      rate = samplerate * (offset - padding + 1) / (144 >> lsf) / 1000;
    }

    if (valid) {
      GST_LOG_OBJECT (mp3parse, "calculated rate %lu", rate * 1000);
      if (rate < 8 || (layer == 3 && rate > 640)) {
        GST_DEBUG_OBJECT (mp3parse, "rate invalid");
        if (rate < 8) {
          /* maybe some hope */
          continue;
        } else {
          GST_DEBUG_OBJECT (mp3parse, "aborting");
          /* give up */
          break;
        }
      }
      *_rate = rate * 1000;
      break;
    } else {
      /* avoid indefinite searching */
      if (rate > 1000) {
        GST_DEBUG_OBJECT (mp3parse, "exceeded sanity rate; aborting");
        break;
      }
    }
  }

  return TRUE;
}

static GstFlowReturn
gst_mpeg_audio_parse_handle_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame, gint * skipsize)
{
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);
  GstBuffer *buf = frame->buffer;
  GstByteReader reader;
  gint off, bpf = 0;
  gboolean lost_sync, draining, valid, caps_change;
  guint32 header;
  guint bitrate, layer, rate, channels, version, mode, crc;
  GstMapInfo map;
  gboolean res = FALSE;

  gst_buffer_map (buf, &map, GST_MAP_READ);
  if (G_UNLIKELY (map.size < 6)) {
    *skipsize = 1;
    goto cleanup;
  }

  gst_byte_reader_init (&reader, map.data, map.size);

  off = gst_byte_reader_masked_scan_uint32 (&reader, 0xffe00000, 0xffe00000,
      0, map.size);

  GST_LOG_OBJECT (parse, "possible sync at buffer offset %d", off);

  /* didn't find anything that looks like a sync word, skip */
  if (off < 0) {
    *skipsize = map.size - 3;
    goto cleanup;
  }

  /* possible frame header, but not at offset 0? skip bytes before sync */
  if (off > 0) {
    *skipsize = off;
    goto cleanup;
  }

  /* make sure the values in the frame header look sane */
  header = GST_READ_UINT32_BE (map.data);
  if (!gst_mpeg_audio_parse_head_check (mp3parse, header)) {
    *skipsize = 1;
    goto cleanup;
  }

  GST_LOG_OBJECT (parse, "got frame");

  lost_sync = GST_BASE_PARSE_LOST_SYNC (parse);
  draining = GST_BASE_PARSE_DRAINING (parse);

  if (G_UNLIKELY (lost_sync))
    mp3parse->freerate = 0;

  bpf = mp3_type_frame_length_from_header (mp3parse, header,
      &version, &layer, &channels, &bitrate, &rate, &mode, &crc);

  if (channels != mp3parse->channels || rate != mp3parse->rate ||
      layer != mp3parse->layer || version != mp3parse->version)
    caps_change = TRUE;
  else
    caps_change = FALSE;

  /* maybe free format */
  if (bpf == 0) {
    GST_LOG_OBJECT (mp3parse, "possibly free format");
    if (lost_sync || mp3parse->freerate == 0) {
      GST_DEBUG_OBJECT (mp3parse, "finding free format rate");
      if (!gst_mp3parse_find_freerate (mp3parse, &map, header, draining,
              &valid)) {
        /* not enough data */
        gst_base_parse_set_min_frame_size (parse, valid);
        *skipsize = 0;
        goto cleanup;
      } else {
        GST_DEBUG_OBJECT (parse, "determined freeform size %d", valid);
        mp3parse->freerate = valid;
      }
    }
    /* try again */
    bpf = mp3_type_frame_length_from_header (mp3parse, header,
        &version, &layer, &channels, &bitrate, &rate, &mode, &crc);
    if (!bpf) {
      /* did not come up with valid freeform length, reject after all */
      *skipsize = 1;
      goto cleanup;
    }
  }

  if (!draining && (lost_sync || caps_change)) {
    if (!gst_mp3parse_validate_extended (mp3parse, buf, header, bpf, draining,
            &valid)) {
      /* not enough data */
      gst_base_parse_set_min_frame_size (parse, valid);
      *skipsize = 0;
      goto cleanup;
    } else {
      if (!valid) {
        *skipsize = off + 2;
        goto cleanup;
      }
    }
  } else if (draining && lost_sync && caps_change && mp3parse->rate > 0) {
    /* avoid caps jitter that we can't be sure of */
    *skipsize = off + 2;
    goto cleanup;
  }

  /* restore default minimum */
  gst_base_parse_set_min_frame_size (parse, MIN_FRAME_SIZE);

  res = TRUE;

  /* metadata handling */
  if (G_UNLIKELY (caps_change)) {
    GstCaps *caps = gst_caps_new_simple ("audio/mpeg",
        "mpegversion", G_TYPE_INT, 1,
        "mpegaudioversion", G_TYPE_INT, version,
        "layer", G_TYPE_INT, layer,
        "rate", G_TYPE_INT, rate,
        "channels", G_TYPE_INT, channels, "parsed", G_TYPE_BOOLEAN, TRUE, NULL);
    gst_pad_set_caps (GST_BASE_PARSE_SRC_PAD (parse), caps);
    gst_caps_unref (caps);

    mp3parse->rate = rate;
    mp3parse->channels = channels;
    mp3parse->layer = layer;
    mp3parse->version = version;

    /* see http://www.codeproject.com/audio/MPEGAudioInfo.asp */
    if (mp3parse->layer == 1)
      mp3parse->spf = 384;
    else if (mp3parse->layer == 2)
      mp3parse->spf = 1152;
    else if (mp3parse->version == 1) {
      mp3parse->spf = 1152;
    } else {
      /* MPEG-2 or "2.5" */
      mp3parse->spf = 576;
    }

    /* We need the frame duration for calculating the frame number later
     * in gst_mpeg_audio_parse_pre_push_frame (). */
    mp3parse->frame_duration = gst_util_uint64_scale (GST_SECOND,
        mp3parse->spf, mp3parse->rate);

    /* lead_in:
     * We start pushing 9 frames earlier (29 frames for MPEG2) than
     * segment start to be able to decode the first frame we want.
     * 9 (29) frames are the theoretical maximum of frames that contain
     * data for the current frame (bit reservoir).
     *
     * lead_out:
     * Some mp3 streams have an offset in the timestamps, for which we have to
     * push the frame *after* the end position in order for the decoder to be
     * able to decode everything up until the segment.stop position. */
    gst_base_parse_set_frame_rate (parse, mp3parse->rate, mp3parse->spf,
        (version == 1) ? 10 : 30, 2);
  }

  if (mp3parse->hdr_bitrate && mp3parse->hdr_bitrate != bitrate) {
    mp3parse->bitrate_is_constant = FALSE;
  }
  mp3parse->hdr_bitrate = bitrate;

  /* While during normal playback, the Xing header frame is seen only once
   * (right at the beginning), we may see it again if the user seeked back
   * to the beginning. To make sure it is dropped again and NOT pushed
   * downstream, we have to check every frame for Xing IDs.
   *
   * (sent_codec_tag is TRUE after this Xing frame got parsed.) */
  if (G_LIKELY (mp3parse->sent_codec_tag)) {
    if (G_UNLIKELY (gst_mpeg_audio_parse_check_if_is_xing_header_frame
            (mp3parse, buf))) {
      GST_DEBUG_OBJECT (mp3parse, "This is a Xing header frame, which "
          "contains no meaningful audio data, and can be safely dropped");
      mp3parse->outgoing_frame_is_xing_header = TRUE;
    }
  }

  /* For first frame; check for seek tables and output a codec tag */
  gst_mpeg_audio_parse_handle_first_frame (mp3parse, buf);

  /* store some frame info for later processing */
  mp3parse->last_crc = crc;
  mp3parse->last_mode = mode;

cleanup:
  gst_buffer_unmap (buf, &map);

  /* We don't actually drop the frame right here, but rather in
   * gst_mpeg_audio_parse_pre_push_frame (), since it is still important
   * to let other code bits do their work there even if we want to drop
   * the current frame. */
  if (G_UNLIKELY (mp3parse->outgoing_frame_is_xing_header)) {
    frame->flags |= GST_BASE_PARSE_FRAME_FLAG_NO_FRAME;
    /* Set duration to zero to prevent the baseparse class
     * from incrementing outgoing timestamps */
    GST_BUFFER_DURATION (frame->buffer) = 0;
  }

  if (res && bpf <= map.size) {
    return gst_base_parse_finish_frame (parse, frame, bpf);
  }

  return GST_FLOW_OK;
}

static gboolean
gst_mpeg_audio_parse_check_if_is_xing_header_frame (GstMpegAudioParse *
    mp3parse, GstBuffer * buf)
{
  /* TODO: get rid of code duplication
   * (see gst_mpeg_audio_parse_handle_first_frame ()) */

  const guint32 xing_id = 0x58696e67;   /* 'Xing' in hex */
  const guint32 info_id = 0x496e666f;   /* 'Info' in hex - found in LAME CBR files */

  gint offset_xing;
  GstMapInfo map;
  guint8 *data;
  guint64 avail;
  guint32 read_id_xing = 0;
  gboolean ret = FALSE;

  /* Check first frame for Xing info */
  if (mp3parse->version == 1) { /* MPEG-1 file */
    if (mp3parse->channels == 1)
      offset_xing = 0x11;
    else
      offset_xing = 0x20;
  } else {                      /* MPEG-2 header */
    if (mp3parse->channels == 1)
      offset_xing = 0x09;
    else
      offset_xing = 0x11;
  }

  /* Skip the 4 bytes of the MP3 header too */
  offset_xing += 4;

  /* Check if we have enough data to read the Xing header */
  gst_buffer_map (buf, &map, GST_MAP_READ);
  data = map.data;
  avail = map.size;

  if (avail >= offset_xing + 4) {
    read_id_xing = GST_READ_UINT32_BE (data + offset_xing);
    ret = (read_id_xing == xing_id || read_id_xing == info_id);
  }

  gst_buffer_unmap (buf, &map);

  return ret;
}

static void
gst_mpeg_audio_parse_handle_first_frame (GstMpegAudioParse * mp3parse,
    GstBuffer * buf)
{
  const guint32 xing_id = 0x58696e67;   /* 'Xing' in hex */
  const guint32 info_id = 0x496e666f;   /* 'Info' in hex - found in LAME CBR files */
  const guint32 vbri_id = 0x56425249;   /* 'VBRI' in hex */
  const guint32 lame_id = 0x4c414d45;   /* 'LAME' in hex */
  gint offset_xing, offset_vbri;
  guint64 avail;
  gint64 upstream_total_bytes = 0;
  guint32 read_id_xing = 0, read_id_vbri = 0;
  GstMapInfo map;
  guint8 *data;
  guint bitrate;

  if (mp3parse->sent_codec_tag)
    return;

  /* Check first frame for Xing info */
  if (mp3parse->version == 1) { /* MPEG-1 file */
    if (mp3parse->channels == 1)
      offset_xing = 0x11;
    else
      offset_xing = 0x20;
  } else {                      /* MPEG-2 header */
    if (mp3parse->channels == 1)
      offset_xing = 0x09;
    else
      offset_xing = 0x11;
  }

  /* The VBRI tag is always at offset 0x20 */
  offset_vbri = 0x20;

  /* Skip the 4 bytes of the MP3 header too */
  offset_xing += 4;
  offset_vbri += 4;

  /* Check if we have enough data to read the Xing header */
  gst_buffer_map (buf, &map, GST_MAP_READ);
  data = map.data;
  avail = map.size;

  if (avail >= offset_xing + 4) {
    read_id_xing = GST_READ_UINT32_BE (data + offset_xing);
  }
  if (avail >= offset_vbri + 4) {
    read_id_vbri = GST_READ_UINT32_BE (data + offset_vbri);
  }

  /* obtain real upstream total bytes */
  if (!gst_pad_peer_query_duration (GST_BASE_PARSE_SINK_PAD (mp3parse),
          GST_FORMAT_BYTES, &upstream_total_bytes))
    upstream_total_bytes = 0;

  if (read_id_xing == xing_id || read_id_xing == info_id) {
    guint32 xing_flags;
    guint bytes_needed = offset_xing + 8;
    gint64 total_bytes;
    guint64 num_xing_samples = 0;
    GstClockTime total_time;

    GST_DEBUG_OBJECT (mp3parse, "Found Xing header marker 0x%x", xing_id);

    GST_DEBUG_OBJECT (mp3parse, "This is a Xing header frame, which contains "
        "no meaningful audio data, and can be safely dropped");
    mp3parse->outgoing_frame_is_xing_header = TRUE;

    /* Move data after Xing header */
    data += offset_xing + 4;

    /* Read 4 base bytes of flags, big-endian */
    xing_flags = GST_READ_UINT32_BE (data);
    data += 4;
    if (xing_flags & XING_FRAMES_FLAG)
      bytes_needed += 4;
    if (xing_flags & XING_BYTES_FLAG)
      bytes_needed += 4;
    if (xing_flags & XING_TOC_FLAG)
      bytes_needed += 100;
    if (xing_flags & XING_VBR_SCALE_FLAG)
      bytes_needed += 4;
    if (avail < bytes_needed) {
      GST_DEBUG_OBJECT (mp3parse,
          "Not enough data to read Xing header (need %d)", bytes_needed);
      goto cleanup;
    }

    GST_DEBUG_OBJECT (mp3parse, "Reading Xing header");
    mp3parse->xing_flags = xing_flags;

    if (xing_flags & XING_FRAMES_FLAG) {
      mp3parse->xing_frames = GST_READ_UINT32_BE (data);
      if (mp3parse->xing_frames == 0) {
        GST_WARNING_OBJECT (mp3parse,
            "Invalid number of frames in Xing header");
        mp3parse->xing_flags &= ~XING_FRAMES_FLAG;
      } else {
        num_xing_samples = (guint64) (mp3parse->xing_frames) * (mp3parse->spf);
        mp3parse->xing_total_time = gst_util_uint64_scale (GST_SECOND,
            num_xing_samples, mp3parse->rate);
      }

      data += 4;
    } else {
      mp3parse->xing_frames = 0;
      mp3parse->xing_total_time = 0;
    }

    /* Store the entire time as actual total time for now. Should there be
     * any padding present, this value will get adjusted accordingly. */
    mp3parse->xing_actual_total_time = mp3parse->xing_total_time;

    if (xing_flags & XING_BYTES_FLAG) {
      mp3parse->xing_bytes = GST_READ_UINT32_BE (data);
      if (mp3parse->xing_bytes == 0) {
        GST_WARNING_OBJECT (mp3parse, "Invalid number of bytes in Xing header");
        mp3parse->xing_flags &= ~XING_BYTES_FLAG;
      }
      data += 4;
    } else {
      mp3parse->xing_bytes = 0;
    }

    /* If we know the upstream size and duration, compute the
     * total bitrate, rounded up to the nearest kbit/sec */
    if ((total_time = mp3parse->xing_total_time) &&
        (total_bytes = mp3parse->xing_bytes)) {
      mp3parse->xing_bitrate = gst_util_uint64_scale (total_bytes,
          8 * GST_SECOND, total_time);
      mp3parse->xing_bitrate += 500;
      mp3parse->xing_bitrate -= mp3parse->xing_bitrate % 1000;
    }

    if (xing_flags & XING_TOC_FLAG) {
      int i, percent = 0;
      guchar *table = mp3parse->xing_seek_table;
      guchar old = 0, new;
      guint first;

      first = data[0];
      GST_DEBUG_OBJECT (mp3parse,
          "Subtracting initial offset of %d bytes from Xing TOC", first);

      /* xing seek table: percent time -> 1/256 bytepos */
      for (i = 0; i < 100; i++) {
        new = data[i] - first;
        if (old > new) {
          GST_WARNING_OBJECT (mp3parse, "Skipping broken Xing TOC");
          mp3parse->xing_flags &= ~XING_TOC_FLAG;
          goto skip_toc;
        }
        mp3parse->xing_seek_table[i] = old = new;
      }

      /* build inverse table: 1/256 bytepos -> 1/100 percent time */
      for (i = 0; i < 256; i++) {
        while (percent < 99 && table[percent + 1] <= i)
          percent++;

        if (table[percent] == i) {
          mp3parse->xing_seek_table_inverse[i] = percent * 100;
        } else if (percent < 99 && table[percent]) {
          gdouble fa, fb, fx;
          gint a = percent, b = percent + 1;

          fa = table[a];
          fb = table[b];
          fx = (b - a) / (fb - fa) * (i - fa) + a;
          mp3parse->xing_seek_table_inverse[i] = (guint16) (fx * 100);
        } else if (percent == 99) {
          gdouble fa, fb, fx;
          gint a = percent, b = 100;

          fa = table[a];
          fb = 256.0;
          fx = (b - a) / (fb - fa) * (i - fa) + a;
          mp3parse->xing_seek_table_inverse[i] = (guint16) (fx * 100);
        }
      }
    skip_toc:
      data += 100;
    } else {
      memset (mp3parse->xing_seek_table, 0, sizeof (mp3parse->xing_seek_table));
      memset (mp3parse->xing_seek_table_inverse, 0,
          sizeof (mp3parse->xing_seek_table_inverse));
    }

    if (xing_flags & XING_VBR_SCALE_FLAG) {
      mp3parse->xing_vbr_scale = GST_READ_UINT32_BE (data);
      data += 4;
    } else
      mp3parse->xing_vbr_scale = 0;

    GST_DEBUG_OBJECT (mp3parse, "Xing header reported %u frames, %"
        G_GUINT64_FORMAT " samples, time %" GST_TIME_FORMAT
        " (this includes potentially present padding data), %u bytes,"
        " vbr scale %u", mp3parse->xing_frames, num_xing_samples,
        GST_TIME_ARGS (mp3parse->xing_total_time), mp3parse->xing_bytes,
        mp3parse->xing_vbr_scale);

    /* check for truncated file */
    if (upstream_total_bytes && mp3parse->xing_bytes &&
        mp3parse->xing_bytes * 0.8 > upstream_total_bytes) {
      GST_WARNING_OBJECT (mp3parse, "File appears to have been truncated; "
          "invalidating Xing header duration and size");
      mp3parse->xing_flags &= ~XING_BYTES_FLAG;
      mp3parse->xing_flags &= ~XING_FRAMES_FLAG;
    }

    /* Optional LAME tag? */
    if (avail - bytes_needed >= 36 && GST_READ_UINT32_BE (data) == lame_id) {
      gchar lame_version[10] = { 0, };
      guint tag_rev;
      guint32 encoder_delay, encoder_padding;
      guint64 total_padding_samples;
      guint64 actual_num_xing_samples;

      memcpy (lame_version, data, 9);
      data += 9;
      tag_rev = data[0] >> 4;
      GST_DEBUG_OBJECT (mp3parse, "Found LAME tag revision %d created by '%s'",
          tag_rev, lame_version);

      /* Skip all the information we're not interested in */
      data += 12;
      /* Encoder delay and end padding */
      encoder_delay = GST_READ_UINT24_BE (data);
      encoder_delay >>= 12;
      encoder_padding = GST_READ_UINT24_BE (data);
      encoder_padding &= 0x000fff;

      total_padding_samples = encoder_delay + encoder_padding;

      mp3parse->encoder_delay = encoder_delay;
      mp3parse->encoder_padding = encoder_padding;

      /* As mentioned in the overview at the beginning of this source
       * file, decoders exhibit a delay of 529 samples. */
      mp3parse->decoder_delay = 529;

      /* Where the actual, non-padding samples start & end, in sample offsets. */
      mp3parse->start_of_actual_samples = mp3parse->encoder_delay +
          mp3parse->decoder_delay;
      mp3parse->end_of_actual_samples = num_xing_samples +
          mp3parse->decoder_delay - mp3parse->encoder_padding;

      /* Length of padding at the start and at the end of the stream,
       * in nanoseconds. */
      mp3parse->start_padding_time = gst_util_uint64_scale_int (GST_SECOND,
          mp3parse->start_of_actual_samples, mp3parse->rate);
      mp3parse->end_padding_time = mp3parse->xing_total_time -
          gst_util_uint64_scale_int (mp3parse->end_of_actual_samples,
          GST_SECOND, mp3parse->rate);

      /* Total length of all combined padding samples, in nanoseconds. */
      mp3parse->total_padding_time = gst_util_uint64_scale_int (GST_SECOND,
          total_padding_samples, mp3parse->rate);

      /* Length of media, in samples, without the number of padding samples. */
      actual_num_xing_samples = (num_xing_samples >= total_padding_samples) ?
          (num_xing_samples - total_padding_samples) : 0;
      /* Length of media, converted to nanoseconds. This is used for setting
       * baseparse's duration. */
      mp3parse->xing_actual_total_time = gst_util_uint64_scale (GST_SECOND,
          actual_num_xing_samples, mp3parse->rate);

      GST_DEBUG_OBJECT (mp3parse, "Encoder delay: %u samples",
          mp3parse->encoder_delay);
      GST_DEBUG_OBJECT (mp3parse, "Encoder padding: %u samples",
          mp3parse->encoder_padding);
      GST_DEBUG_OBJECT (mp3parse, "Decoder delay: %u samples",
          mp3parse->decoder_delay);
      GST_DEBUG_OBJECT (mp3parse, "Start of actual samples: %"
          G_GUINT64_FORMAT, mp3parse->start_of_actual_samples);
      GST_DEBUG_OBJECT (mp3parse, "End of actual samples: %"
          G_GUINT64_FORMAT, mp3parse->end_of_actual_samples);
      GST_DEBUG_OBJECT (mp3parse, "Total padding samples: %" G_GUINT64_FORMAT,
          total_padding_samples);
      GST_DEBUG_OBJECT (mp3parse, "Start padding time: %" GST_TIME_FORMAT,
          GST_TIME_ARGS (mp3parse->start_padding_time));
      GST_DEBUG_OBJECT (mp3parse, "End padding time: %" GST_TIME_FORMAT,
          GST_TIME_ARGS (mp3parse->end_padding_time));
      GST_DEBUG_OBJECT (mp3parse, "Total padding time: %" GST_TIME_FORMAT,
          GST_TIME_ARGS (mp3parse->total_padding_time));
      GST_DEBUG_OBJECT (mp3parse, "Actual total media samples: %"
          G_GUINT64_FORMAT, actual_num_xing_samples);
      GST_DEBUG_OBJECT (mp3parse, "Actual total media length: %"
          GST_TIME_FORMAT, GST_TIME_ARGS (mp3parse->xing_actual_total_time));
    }
  } else if (read_id_vbri == vbri_id) {
    gint64 total_bytes, total_frames;
    GstClockTime total_time;
    guint16 nseek_points;

    GST_DEBUG_OBJECT (mp3parse, "Found VBRI header marker 0x%x", vbri_id);

    if (avail < offset_vbri + 26) {
      GST_DEBUG_OBJECT (mp3parse,
          "Not enough data to read VBRI header (need %d)", offset_vbri + 26);
      goto cleanup;
    }

    GST_DEBUG_OBJECT (mp3parse, "Reading VBRI header");

    /* Move data after VBRI header */
    data += offset_vbri + 4;

    if (GST_READ_UINT16_BE (data) != 0x0001) {
      GST_WARNING_OBJECT (mp3parse,
          "Unsupported VBRI version 0x%x", GST_READ_UINT16_BE (data));
      goto cleanup;
    }
    data += 2;

    /* Skip encoder delay */
    data += 2;

    /* Skip quality */
    data += 2;

    total_bytes = GST_READ_UINT32_BE (data);
    if (total_bytes != 0)
      mp3parse->vbri_bytes = total_bytes;
    data += 4;

    total_frames = GST_READ_UINT32_BE (data);
    if (total_frames != 0) {
      mp3parse->vbri_frames = total_frames;
      mp3parse->vbri_total_time = gst_util_uint64_scale (GST_SECOND,
          (guint64) (mp3parse->vbri_frames) * (mp3parse->spf), mp3parse->rate);
    }
    data += 4;

    /* If we know the upstream size and duration, compute the
     * total bitrate, rounded up to the nearest kbit/sec */
    if ((total_time = mp3parse->vbri_total_time) &&
        (total_bytes = mp3parse->vbri_bytes)) {
      mp3parse->vbri_bitrate = gst_util_uint64_scale (total_bytes,
          8 * GST_SECOND, total_time);
      mp3parse->vbri_bitrate += 500;
      mp3parse->vbri_bitrate -= mp3parse->vbri_bitrate % 1000;
    }

    nseek_points = GST_READ_UINT16_BE (data);
    data += 2;

    if (nseek_points > 0) {
      guint scale, seek_bytes, seek_frames;
      gint i;

      mp3parse->vbri_seek_points = nseek_points;

      scale = GST_READ_UINT16_BE (data);
      data += 2;

      seek_bytes = GST_READ_UINT16_BE (data);
      data += 2;

      seek_frames = GST_READ_UINT16_BE (data);

      if (scale == 0 || seek_bytes == 0 || seek_bytes > 4 || seek_frames == 0) {
        GST_WARNING_OBJECT (mp3parse, "Unsupported VBRI seek table");
        goto out_vbri;
      }

      if (avail < offset_vbri + 26 + nseek_points * seek_bytes) {
        GST_WARNING_OBJECT (mp3parse,
            "Not enough data to read VBRI seek table (need %d)",
            offset_vbri + 26 + nseek_points * seek_bytes);
        goto out_vbri;
      }

      if (seek_frames * nseek_points < total_frames - seek_frames ||
          seek_frames * nseek_points > total_frames + seek_frames) {
        GST_WARNING_OBJECT (mp3parse,
            "VBRI seek table doesn't cover the complete file");
        goto out_vbri;
      }

      data = map.data;
      data += offset_vbri + 26;

      /* VBRI seek table: frame/seek_frames -> byte */
      mp3parse->vbri_seek_table = g_new (guint32, nseek_points);
      if (seek_bytes == 4)
        for (i = 0; i < nseek_points; i++) {
          mp3parse->vbri_seek_table[i] = GST_READ_UINT32_BE (data) * scale;
          data += 4;
      } else if (seek_bytes == 3)
        for (i = 0; i < nseek_points; i++) {
          mp3parse->vbri_seek_table[i] = GST_READ_UINT24_BE (data) * scale;
          data += 3;
      } else if (seek_bytes == 2)
        for (i = 0; i < nseek_points; i++) {
          mp3parse->vbri_seek_table[i] = GST_READ_UINT16_BE (data) * scale;
          data += 2;
      } else                    /* seek_bytes == 1 */
        for (i = 0; i < nseek_points; i++) {
          mp3parse->vbri_seek_table[i] = GST_READ_UINT8 (data) * scale;
          data += 1;
        }
    }
  out_vbri:

    GST_DEBUG_OBJECT (mp3parse, "VBRI header reported %u frames, time %"
        GST_TIME_FORMAT ", bytes %u", mp3parse->vbri_frames,
        GST_TIME_ARGS (mp3parse->vbri_total_time), mp3parse->vbri_bytes);

    /* check for truncated file */
    if (upstream_total_bytes && mp3parse->vbri_bytes &&
        mp3parse->vbri_bytes * 0.8 > upstream_total_bytes) {
      GST_WARNING_OBJECT (mp3parse, "File appears to have been truncated; "
          "invalidating VBRI header duration and size");
      mp3parse->vbri_valid = FALSE;
    } else {
      mp3parse->vbri_valid = TRUE;
    }
  } else {
    GST_DEBUG_OBJECT (mp3parse,
        "Xing, LAME or VBRI header not found in first frame");
  }

  /* set duration if tables provided a valid one */
  if (mp3parse->xing_flags & XING_FRAMES_FLAG) {
    gst_base_parse_set_duration (GST_BASE_PARSE (mp3parse), GST_FORMAT_TIME,
        mp3parse->xing_actual_total_time, 0);
  }
  if (mp3parse->vbri_total_time != 0 && mp3parse->vbri_valid) {
    gst_base_parse_set_duration (GST_BASE_PARSE (mp3parse), GST_FORMAT_TIME,
        mp3parse->vbri_total_time, 0);
  }

  /* tell baseclass how nicely we can seek, and a bitrate if one found */
  /* FIXME: fill index with seek table */
#if 0
  seekable = GST_BASE_PARSE_SEEK_DEFAULT;
  if ((mp3parse->xing_flags & XING_TOC_FLAG) && mp3parse->xing_bytes &&
      mp3parse->xing_total_time)
    seekable = GST_BASE_PARSE_SEEK_TABLE;

  if (mp3parse->vbri_seek_table && mp3parse->vbri_bytes &&
      mp3parse->vbri_total_time)
    seekable = GST_BASE_PARSE_SEEK_TABLE;
#endif

  if (mp3parse->xing_bitrate)
    bitrate = mp3parse->xing_bitrate;
  else if (mp3parse->vbri_bitrate)
    bitrate = mp3parse->vbri_bitrate;
  else
    bitrate = 0;

  gst_base_parse_set_average_bitrate (GST_BASE_PARSE (mp3parse), bitrate);

cleanup:
  gst_buffer_unmap (buf, &map);
}

static gboolean
gst_mpeg_audio_parse_time_to_bytepos (GstMpegAudioParse * mp3parse,
    GstClockTime ts, gint64 * bytepos)
{
  gint64 total_bytes;
  GstClockTime total_time;

  /* If XING seek table exists use this for time->byte conversion */
  if ((mp3parse->xing_flags & XING_TOC_FLAG) &&
      (total_bytes = mp3parse->xing_bytes) &&
      (total_time = mp3parse->xing_total_time)) {
    gdouble fa, fb, fx;
    gdouble percent =
        CLAMP ((100.0 * gst_util_guint64_to_gdouble (ts)) /
        gst_util_guint64_to_gdouble (total_time), 0.0, 100.0);
    gint index = CLAMP (percent, 0, 99);

    fa = mp3parse->xing_seek_table[index];
    if (index < 99)
      fb = mp3parse->xing_seek_table[index + 1];
    else
      fb = 256.0;

    fx = fa + (fb - fa) * (percent - index);

    *bytepos = (1.0 / 256.0) * fx * total_bytes;

    return TRUE;
  }

  if (mp3parse->vbri_seek_table && (total_bytes = mp3parse->vbri_bytes) &&
      (total_time = mp3parse->vbri_total_time)) {
    gint i, j;
    gdouble a, b, fa, fb;

    i = gst_util_uint64_scale (ts, mp3parse->vbri_seek_points - 1, total_time);
    i = CLAMP (i, 0, mp3parse->vbri_seek_points - 1);

    a = gst_guint64_to_gdouble (gst_util_uint64_scale (i, total_time,
            mp3parse->vbri_seek_points));
    fa = 0.0;
    for (j = i; j >= 0; j--)
      fa += mp3parse->vbri_seek_table[j];

    if (i + 1 < mp3parse->vbri_seek_points) {
      b = gst_guint64_to_gdouble (gst_util_uint64_scale (i + 1, total_time,
              mp3parse->vbri_seek_points));
      fb = fa + mp3parse->vbri_seek_table[i + 1];
    } else {
      b = gst_guint64_to_gdouble (total_time);
      fb = total_bytes;
    }

    *bytepos = fa + ((fb - fa) / (b - a)) * (gst_guint64_to_gdouble (ts) - a);

    return TRUE;
  }

  /* If we have had a constant bit rate (so far), use it directly, as it
   * may give slightly more accurate results than the base class. */
  if (mp3parse->bitrate_is_constant && mp3parse->hdr_bitrate) {
    *bytepos = gst_util_uint64_scale (ts, mp3parse->hdr_bitrate,
        8 * GST_SECOND);
    return TRUE;
  }

  return FALSE;
}

static gboolean
gst_mpeg_audio_parse_bytepos_to_time (GstMpegAudioParse * mp3parse,
    gint64 bytepos, GstClockTime * ts)
{
  gint64 total_bytes;
  GstClockTime total_time;

  /* If XING seek table exists use this for byte->time conversion */
  if ((mp3parse->xing_flags & XING_TOC_FLAG) &&
      (total_bytes = mp3parse->xing_bytes) &&
      (total_time = mp3parse->xing_total_time)) {
    gdouble fa, fb, fx;
    gdouble pos;
    gint index;

    pos = CLAMP ((bytepos * 256.0) / total_bytes, 0.0, 256.0);
    index = CLAMP (pos, 0, 255);
    fa = mp3parse->xing_seek_table_inverse[index];
    if (index < 255)
      fb = mp3parse->xing_seek_table_inverse[index + 1];
    else
      fb = 10000.0;

    fx = fa + (fb - fa) * (pos - index);

    *ts = (1.0 / 10000.0) * fx * gst_util_guint64_to_gdouble (total_time);

    return TRUE;
  }

  if (mp3parse->vbri_seek_table &&
      (total_bytes = mp3parse->vbri_bytes) &&
      (total_time = mp3parse->vbri_total_time)) {
    gint i = 0;
    guint64 sum = 0;
    gdouble a, b, fa, fb;

    do {
      sum += mp3parse->vbri_seek_table[i];
      i++;
    } while (i + 1 < mp3parse->vbri_seek_points
        && sum + mp3parse->vbri_seek_table[i] < bytepos);
    i--;

    a = gst_guint64_to_gdouble (sum);
    fa = gst_guint64_to_gdouble (gst_util_uint64_scale (i, total_time,
            mp3parse->vbri_seek_points));

    if (i + 1 < mp3parse->vbri_seek_points) {
      b = a + mp3parse->vbri_seek_table[i + 1];
      fb = gst_guint64_to_gdouble (gst_util_uint64_scale (i + 1, total_time,
              mp3parse->vbri_seek_points));
    } else {
      b = total_bytes;
      fb = gst_guint64_to_gdouble (total_time);
    }

    *ts = gst_gdouble_to_guint64 (fa + ((fb - fa) / (b - a)) * (bytepos - a));

    return TRUE;
  }

  /* If we have had a constant bit rate (so far), use it directly, as it
   * may give slightly more accurate results than the base class. */
  if (mp3parse->bitrate_is_constant && mp3parse->hdr_bitrate) {
    *ts = gst_util_uint64_scale (bytepos, 8 * GST_SECOND,
        mp3parse->hdr_bitrate);
    return TRUE;
  }

  return FALSE;
}

static gboolean
gst_mpeg_audio_parse_src_query (GstBaseParse * parse, GstQuery * query)
{
  gboolean res = FALSE;
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);

  res = GST_BASE_PARSE_CLASS (parent_class)->src_query (parse, query);
  if (!res)
    return FALSE;

  /* If upstream operates in BYTE format then consider any parsed Xing/LAME
   * header to remove encoder/decoder delay and padding samples from the
   * position query. */
  if (mp3parse->upstream_format == GST_FORMAT_BYTES
      || GST_PAD_MODE (GST_BASE_PARSE_SINK_PAD (parse)) == GST_PAD_MODE_PULL) {
    switch (GST_QUERY_TYPE (query)) {
      case GST_QUERY_POSITION:{
        GstFormat format;
        gint64 position, new_position;
        GstClockTime duration_to_skip;
        gst_query_parse_position (query, &format, &position);

        /* Adjust the position to exclude padding samples. */

        if ((position < 0) || (format != GST_FORMAT_TIME))
          break;

        duration_to_skip = mp3parse->frame_duration +
            mp3parse->start_padding_time;

        if (position < duration_to_skip)
          new_position = 0;
        else
          new_position = position - duration_to_skip;

        if (new_position > (mp3parse->xing_actual_total_time))
          new_position = mp3parse->xing_actual_total_time;

        GST_LOG_OBJECT (mp3parse, "applying gapless padding info to position "
            "query response: %" GST_TIME_FORMAT " -> %" GST_TIME_FORMAT,
            GST_TIME_ARGS (position), GST_TIME_ARGS (new_position));

        gst_query_set_position (query, GST_FORMAT_TIME, new_position);

        break;
      }

      default:
        break;
    }
  }

  return res;
}

static gboolean
gst_mpeg_audio_parse_sink_event (GstBaseParse * parse, GstEvent * event)
{
  gboolean res = FALSE;
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);

  res =
      GST_BASE_PARSE_CLASS (parent_class)->sink_event (parse,
      gst_event_ref (event));
  if (!res) {
    gst_event_unref (event);
    return FALSE;
  }

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEGMENT:{
      const GstSegment *segment;

      gst_event_parse_segment (event, &segment);
      mp3parse->upstream_format = segment->format;
    }
    default:
      break;
  }

  gst_event_unref (event);

  return res;
}

static gboolean
gst_mpeg_audio_parse_convert (GstBaseParse * parse, GstFormat src_format,
    gint64 src_value, GstFormat dest_format, gint64 * dest_value)
{
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);
  gboolean res = FALSE;

  if (src_format == GST_FORMAT_TIME && dest_format == GST_FORMAT_BYTES)
    res =
        gst_mpeg_audio_parse_time_to_bytepos (mp3parse, src_value, dest_value);
  else if (src_format == GST_FORMAT_BYTES && dest_format == GST_FORMAT_TIME)
    res = gst_mpeg_audio_parse_bytepos_to_time (mp3parse, src_value,
        (GstClockTime *) dest_value);

  /* if no tables, fall back to default estimated rate based conversion */
  if (!res)
    return gst_base_parse_convert_default (parse, src_format, src_value,
        dest_format, dest_value);

  return res;
}

static GstFlowReturn
gst_mpeg_audio_parse_pre_push_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame)
{
  GstMpegAudioParse *mp3parse = GST_MPEG_AUDIO_PARSE (parse);
  GstTagList *taglist = NULL;

  /* we will create a taglist (if any of the parameters has changed)
   * to add the tags that changed */
  if (mp3parse->last_posted_crc != mp3parse->last_crc) {
    gboolean using_crc;

    if (!taglist)
      taglist = gst_tag_list_new_empty ();

    mp3parse->last_posted_crc = mp3parse->last_crc;
    if (mp3parse->last_posted_crc == CRC_PROTECTED) {
      using_crc = TRUE;
    } else {
      using_crc = FALSE;
    }
    gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, GST_TAG_CRC,
        using_crc, NULL);
  }

  if (mp3parse->last_posted_channel_mode != mp3parse->last_mode) {
    if (!taglist)
      taglist = gst_tag_list_new_empty ();

    mp3parse->last_posted_channel_mode = mp3parse->last_mode;

    gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, GST_TAG_MODE,
        gst_mpeg_audio_channel_mode_get_nick (mp3parse->last_mode), NULL);
  }

  /* tag sending done late enough in hook to ensure pending events
   * have already been sent */
  if (taglist != NULL || !mp3parse->sent_codec_tag) {
    GstCaps *caps;

    if (taglist == NULL)
      taglist = gst_tag_list_new_empty ();

    /* codec tag */
    caps = gst_pad_get_current_caps (GST_BASE_PARSE_SRC_PAD (parse));
    if (G_UNLIKELY (caps == NULL)) {
      gst_tag_list_unref (taglist);

      if (GST_PAD_IS_FLUSHING (GST_BASE_PARSE_SRC_PAD (parse))) {
        GST_INFO_OBJECT (parse, "Src pad is flushing");
        return GST_FLOW_FLUSHING;
      } else {
        GST_INFO_OBJECT (parse, "Src pad is not negotiated!");
        return GST_FLOW_NOT_NEGOTIATED;
      }
    }
    gst_pb_utils_add_codec_description_to_tag_list (taglist,
        GST_TAG_AUDIO_CODEC, caps);
    gst_caps_unref (caps);

    if (mp3parse->hdr_bitrate > 0 && mp3parse->xing_bitrate == 0 &&
        mp3parse->vbri_bitrate == 0) {
      /* We don't have a VBR bitrate, so post the available bitrate as
       * nominal and let baseparse calculate the real bitrate */
      gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE,
          GST_TAG_NOMINAL_BITRATE, mp3parse->hdr_bitrate, NULL);
    }

    /* also signals the end of first-frame processing */
    mp3parse->sent_codec_tag = TRUE;
  }

  /* if the taglist exists, we need to update it so it gets sent out */
  if (taglist) {
    gst_base_parse_merge_tags (parse, taglist, GST_TAG_MERGE_REPLACE);
    gst_tag_list_unref (taglist);
  }

  /* adjust buffer PTS/DTS/durations according to gapless playback info */
  if ((mp3parse->upstream_format == GST_FORMAT_BYTES
          || GST_PAD_MODE (GST_BASE_PARSE_SINK_PAD (parse)) ==
          GST_PAD_MODE_PULL)
      && GST_CLOCK_TIME_IS_VALID (mp3parse->total_padding_time)) {
    guint64 frame_nr;
    GstClockTime pts, dts;
    gboolean add_clipping_meta = FALSE;
    guint32 start_clip = 0, end_clip = 0;
    GstClockTime timestamp_decrement;
    guint64 sample_pos;
    guint64 sample_pos_end;

    /* Get the number of the current frame so we can determine where we
     * currently are in the MPEG stream.
     *
     * Gapless playback is best done based on samples, not timestamps,
     * to avoid potential rounding errors that can otherwise cause a few
     * samples to be incorrectly clipped or not clipped.
     *
     * TODO: At the moment, there is no dedicated baseparse API for finding
     * out what frame we are currently in. The frame number is calculated
     * out of the PTS of the current frame. Each frame has the same duration,
     * and at this point, the buffer's PTS has not been adjusted to exclude
     * the padding samples, so the PTS will be an integer multiple of
     * frame_duration. However, this is not an ideal solution. Investigate
     * how to properly implement this. */
    frame_nr = GST_BUFFER_PTS (frame->buffer) / mp3parse->frame_duration;
    GST_LOG_OBJECT (mp3parse, "Handling MP3 frame #%" G_GUINT64_FORMAT,
        frame_nr);

    /* By default, we subtract the start_padding_time from the timestamps.
     * start_padding_time specifies the duration of the padding samples
     * at the beginning of the MPEG stream. To factor out these padding
     * samples, we have to shift the timestamps back, which is done with
     * this decrement. */
    timestamp_decrement = mp3parse->start_padding_time;

    pts = GST_BUFFER_PTS (frame->buffer);
    dts = GST_BUFFER_DTS (frame->buffer);

    /* sample_pos specifies the current position of the beginning of the
     * current frame, while sample_pos_end specifies the current position
     * of 1 samples past the end of the current frame. Both values are
     * in samples. */
    sample_pos = frame_nr * mp3parse->spf;
    sample_pos_end = sample_pos + mp3parse->spf;

    /* Check if the frame is not (fully) within the actual playback range. */
    if (G_UNLIKELY (sample_pos <= mp3parse->start_of_actual_samples ||
            (sample_pos_end >= mp3parse->end_of_actual_samples))) {

      if (G_UNLIKELY (frame_nr >= mp3parse->xing_frames)) {
        /* Test #1: Check if the current position lies past the length
         * that is specified by the Xing frame header. This normally does
         * not happen, but does occur with "Frankenstein" streams (see
         * the explanation at the beginning of this source file for more).
         * Do this first, since the other test may yield false positives
         * in this case. */
        GST_LOG_OBJECT (mp3parse, "There are frames beyond what the Xing "
            "metadata indicates; this is a Frankenstein stream!");

        /* The frames past the "officially" last one (= the last one according
         * to the Xing header frame) are located past the padding samples
         * that follow the actual playback range. The length of these
         * padding samples in nanoseconds is stored in end_padding_time.
         * We need to shift the PTS to compensate for these padding samples,
         * otherwise there would be a timestamp discontinuity between the
         * last "official" frame and the first "Frankenstein" frame. */
        timestamp_decrement += mp3parse->end_padding_time;
      } else if (sample_pos_end <= mp3parse->start_of_actual_samples) {
        /* Test #2: Check if the frame lies completely before the actual
         * playback range. This happens if the number of padding samples
         * at the start of the stream exceeds the size of a frame, meaning
         * that the entire frame will be filled with padding samples.
         * This has not been observed so far. However, it is in theory
         * possible, so handle it here. */

        /* We want to clip all samples in the frame. Since this is a frame
         * at the start of the stream, set start_clip to the frame size.
         * Also set the buffer duration to 0 to make sure baseparse does not
         * increment timestamps after this current frame is finished. */
        start_clip = mp3parse->spf;
        GST_BUFFER_DURATION (frame->buffer) = 0;

        add_clipping_meta = TRUE;
      } else if (sample_pos <= mp3parse->start_of_actual_samples) {
        /* Test #3: Check if a portion of the frame lies before the actual
         * playback range. Set the duration to the number of samples that
         * remain after clipping. */

        start_clip = mp3parse->start_of_actual_samples - sample_pos;
        GST_BUFFER_DURATION (frame->buffer) =
            gst_util_uint64_scale_int (sample_pos_end -
            mp3parse->start_of_actual_samples, GST_SECOND, mp3parse->rate);

        add_clipping_meta = TRUE;
      } else if (sample_pos >= mp3parse->end_of_actual_samples) {
        /* Test #4: Check if the frame lies completely after the actual
         * playback range. Similar to test #2, this happens if the number
         * of padding samples at the end of the stream exceeds the size of
         * a frame, meaning that the entire frame will be filled with padding
         * samples. Unlike test #2, this has been observed in mp3s several
         * times: The penultimate frame is partially clipped, the final
         * frame is fully clipped. */

        GstClockTime padding_ns;

        /* We want to clip all samples in the frame. Since this is a frame
         * at the end of the stream, set end_clip to the frame size.
         * Also set the buffer duration to 0 to make sure baseparse does not
         * increment timestamps after this current frame is finished. */
        end_clip = mp3parse->spf;
        GST_BUFFER_DURATION (frame->buffer) = 0;

        /* Even though this frame will be fully clipped, we still have to
         * make sure its timestamps are not discontinuous with the preceding
         * ones. To that end, it is necessary to subtract the time range
         * between the current position and the last valid playback range
         * position from the PTS and DTS. */
        padding_ns = gst_util_uint64_scale_int (sample_pos -
            mp3parse->end_of_actual_samples, GST_SECOND, mp3parse->rate);
        timestamp_decrement += padding_ns;

        add_clipping_meta = TRUE;
      } else if (sample_pos_end >= mp3parse->end_of_actual_samples) {
        /* Test #5: Check if a portion of the frame lies after the actual
         * playback range. Set the duration to the number of samples that
         * remain after clipping. */

        end_clip = sample_pos_end - mp3parse->end_of_actual_samples;
        GST_BUFFER_DURATION (frame->buffer) =
            gst_util_uint64_scale_int (mp3parse->end_of_actual_samples -
            sample_pos, GST_SECOND, mp3parse->rate);

        add_clipping_meta = TRUE;
      }
    }

    if (G_UNLIKELY (add_clipping_meta)) {
      GST_DEBUG_OBJECT (mp3parse, "Adding clipping meta: start %"
          G_GUINT32_FORMAT " end %" G_GUINT32_FORMAT, start_clip, end_clip);
      gst_buffer_add_audio_clipping_meta (frame->buffer, GST_FORMAT_DEFAULT,
          start_clip, end_clip);
    }

    /* Adjust the timestamps by subtracting from them. The decrement
     * is computed above. */
    GST_BUFFER_PTS (frame->buffer) = (pts >= timestamp_decrement) ? (pts -
        timestamp_decrement) : 0;
    GST_BUFFER_DTS (frame->buffer) = (dts >= timestamp_decrement) ? (dts -
        timestamp_decrement) : 0;

    /* NOTE: We do not adjust the size here, just the timestamps and duration.
     * We also do not drop fully clipped frames. This is because downstream
     * MPEG audio decoders still need the data of the frame, even if it gets
     * fully clipped later. They do need these frames for their decoding process.
     * If these frames were dropped, the decoders would not fully decode all
     * of the data from the MPEG stream. */

    /* TODO: Should offset/offset_end also be adjusted? */
  }

  /* Check if this frame can safely be dropped (for example, because it is an
   * empty Xing header frame). */
  if (G_UNLIKELY (mp3parse->outgoing_frame_is_xing_header)) {
    GST_DEBUG_OBJECT (mp3parse, "Marking frame as decode-only / droppable");
    mp3parse->outgoing_frame_is_xing_header = FALSE;
    GST_BUFFER_DURATION (frame->buffer) = 0;
    GST_BUFFER_FLAG_SET (frame->buffer, GST_BUFFER_FLAG_DECODE_ONLY);
    GST_BUFFER_FLAG_SET (frame->buffer, GST_BUFFER_FLAG_DROPPABLE);
  }

  /* usual clipping applies */
  frame->flags |= GST_BASE_PARSE_FRAME_FLAG_CLIP;

  return GST_FLOW_OK;
}

static void
remove_fields (GstCaps * caps)
{
  guint i, n;

  n = gst_caps_get_size (caps);
  for (i = 0; i < n; i++) {
    GstStructure *s = gst_caps_get_structure (caps, i);

    gst_structure_remove_field (s, "parsed");
  }
}

static GstCaps *
gst_mpeg_audio_parse_get_sink_caps (GstBaseParse * parse, GstCaps * filter)
{
  GstCaps *peercaps, *templ;
  GstCaps *res;

  templ = gst_pad_get_pad_template_caps (GST_BASE_PARSE_SINK_PAD (parse));
  if (filter) {
    GstCaps *fcopy = gst_caps_copy (filter);
    /* Remove the fields we convert */
    remove_fields (fcopy);
    peercaps = gst_pad_peer_query_caps (GST_BASE_PARSE_SRC_PAD (parse), fcopy);
    gst_caps_unref (fcopy);
  } else
    peercaps = gst_pad_peer_query_caps (GST_BASE_PARSE_SRC_PAD (parse), NULL);

  if (peercaps) {
    /* Remove the parsed field */
    peercaps = gst_caps_make_writable (peercaps);
    remove_fields (peercaps);

    res = gst_caps_intersect_full (peercaps, templ, GST_CAPS_INTERSECT_FIRST);
    gst_caps_unref (peercaps);
    gst_caps_unref (templ);
  } else {
    res = templ;
  }

  if (filter) {
    GstCaps *intersection;

    intersection =
        gst_caps_intersect_full (filter, res, GST_CAPS_INTERSECT_FIRST);
    gst_caps_unref (res);
    res = intersection;
  }

  return res;
}
