/* GStreamer DCA parser
 * Copyright (C) 2010 Tim-Philipp Müller <tim centricular net>
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
 * SECTION:element-dcaparse
 * @short_description: DCA (DTS Coherent Acoustics) parser
 * @see_also: #GstAmrParse, #GstAACParse, #GstAc3Parse
 *
 * This is a DCA (DTS Coherent Acoustics) parser.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch filesrc location=abc.dts ! dcaparse ! dtsdec ! audioresample ! audioconvert ! autoaudiosink
 * ]|
 * </refsect2>
 */

/* TODO:
 *  - should accept framed and unframed input (needs decodebin fixes first)
 *  - seeking in raw .dts files doesn't seem to work, but duration estimate ok
 *
 *  - if frames have 'odd' durations, the frame durations (plus timestamps)
 *    aren't adjusted up occasionally to make up for rounding error gaps.
 *    (e.g. if 512 samples per frame @ 48kHz = 10.666666667 ms/frame)
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>

#include "gstdcaparse.h"
#include <gst/base/gstbytereader.h>
#include <gst/base/gstbitreader.h>

GST_DEBUG_CATEGORY_STATIC (dca_parse_debug);
#define GST_CAT_DEFAULT dca_parse_debug

static GstStaticPadTemplate src_template = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-dts,"
        " framed = (boolean) true,"
        " channels = (int) [ 1, 8 ],"
        " rate = (int) [ 8000, 192000 ],"
        " depth = (int) { 14, 16 },"
        " endianness = (int) { LITTLE_ENDIAN, BIG_ENDIAN }"));

static GstStaticPadTemplate sink_template = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-dts, framed = (boolean) false"));

static void gst_dca_parse_finalize (GObject * object);

static gboolean gst_dca_parse_start (GstBaseParse * parse);
static gboolean gst_dca_parse_stop (GstBaseParse * parse);
static gboolean gst_dca_parse_check_valid_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame, guint * size, gint * skipsize);
static GstFlowReturn gst_dca_parse_parse_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame);

GST_BOILERPLATE (GstDcaParse, gst_dca_parse, GstBaseParse, GST_TYPE_BASE_PARSE);

static void
gst_dca_parse_base_init (gpointer klass)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_template));
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&src_template));

  gst_element_class_set_details_simple (element_class,
      "DTS Coherent Acoustics audio stream parser", "Codec/Parser/Audio",
      "DCA parser", "Tim-Philipp Müller <tim centricular net>");
}

static void
gst_dca_parse_class_init (GstDcaParseClass * klass)
{
  GstBaseParseClass *parse_class = GST_BASE_PARSE_CLASS (klass);
  GObjectClass *object_class = G_OBJECT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (dca_parse_debug, "dcaparse", 0,
      "DCA audio stream parser");

  object_class->finalize = gst_dca_parse_finalize;

  parse_class->start = GST_DEBUG_FUNCPTR (gst_dca_parse_start);
  parse_class->stop = GST_DEBUG_FUNCPTR (gst_dca_parse_stop);
  parse_class->check_valid_frame =
      GST_DEBUG_FUNCPTR (gst_dca_parse_check_valid_frame);
  parse_class->parse_frame = GST_DEBUG_FUNCPTR (gst_dca_parse_parse_frame);
}

static void
gst_dca_parse_reset (GstDcaParse * dcaparse)
{
  dcaparse->channels = -1;
  dcaparse->rate = -1;
  dcaparse->depth = -1;
  dcaparse->endianness = -1;
  dcaparse->block_size = -1;
  dcaparse->frame_size = -1;
  dcaparse->last_sync = 0;
}

static void
gst_dca_parse_init (GstDcaParse * dcaparse, GstDcaParseClass * klass)
{
  gst_base_parse_set_min_frame_size (GST_BASE_PARSE (dcaparse),
      DCA_MIN_FRAMESIZE);
  gst_dca_parse_reset (dcaparse);
}

static void
gst_dca_parse_finalize (GObject * object)
{
  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static gboolean
gst_dca_parse_start (GstBaseParse * parse)
{
  GstDcaParse *dcaparse = GST_DCA_PARSE (parse);

  GST_DEBUG_OBJECT (parse, "starting");

  gst_dca_parse_reset (dcaparse);

  return TRUE;
}

static gboolean
gst_dca_parse_stop (GstBaseParse * parse)
{
  GST_DEBUG_OBJECT (parse, "stopping");

  return TRUE;
}

static gboolean
gst_dca_parse_parse_header (GstDcaParse * dcaparse,
    const GstByteReader * reader, guint * frame_size,
    guint * sample_rate, guint * channels, guint * depth,
    gint * endianness, guint * num_blocks, guint * samples_per_block,
    gboolean * terminator)
{
  static const int sample_rates[16] = { 0, 8000, 16000, 32000, 0, 0, 11025,
    22050, 44100, 0, 0, 12000, 24000, 48000, 96000, 192000
  };
  static const guint8 channels_table[16] = { 1, 2, 2, 2, 2, 3, 3, 4, 4, 5,
    6, 6, 6, 7, 8, 8
  };
  GstByteReader r = *reader;
  guint16 hdr[8];
  guint32 marker;
  guint chans, lfe, i;

  if (gst_byte_reader_get_remaining (&r) < (4 + sizeof (hdr)))
    return FALSE;

  marker = gst_byte_reader_peek_uint32_be_unchecked (&r);

  /* raw big endian or 14-bit big endian */
  if (marker == 0x7FFE8001 || marker == 0x1FFFE800) {
    for (i = 0; i < G_N_ELEMENTS (hdr); ++i)
      hdr[i] = gst_byte_reader_get_uint16_be_unchecked (&r);
  } else
    /* raw little endian or 14-bit little endian */
  if (marker == 0xFE7F0180 || marker == 0xFF1F00E8) {
    for (i = 0; i < G_N_ELEMENTS (hdr); ++i)
      hdr[i] = gst_byte_reader_get_uint16_le_unchecked (&r);
  } else {
    return FALSE;
  }

  GST_LOG_OBJECT (dcaparse, "dts sync marker 0x%08x at offset %u", marker,
      gst_byte_reader_get_pos (reader));

  /* 14-bit mode */
  if (marker == 0x1FFFE800 || marker == 0xFF1F00E8) {
    if ((hdr[2] & 0xFFF0) != 0x07F0)
      return FALSE;
    /* discard top 2 bits (2 void), shift in 2 */
    hdr[0] = (hdr[0] << 2) | ((hdr[1] >> 12) & 0x0003);
    /* discard top 4 bits (2 void, 2 shifted into hdr[0]), shift in 4 etc. */
    hdr[1] = (hdr[1] << 4) | ((hdr[2] >> 10) & 0x000F);
    hdr[2] = (hdr[2] << 6) | ((hdr[3] >> 8) & 0x003F);
    hdr[3] = (hdr[3] << 8) | ((hdr[4] >> 6) & 0x00FF);
    hdr[4] = (hdr[4] << 10) | ((hdr[5] >> 4) & 0x03FF);
    hdr[5] = (hdr[5] << 12) | ((hdr[6] >> 2) & 0x0FFF);
    hdr[6] = (hdr[6] << 14) | ((hdr[7] >> 0) & 0x3FFF);
    g_assert (hdr[0] == 0x7FFE && hdr[1] == 0x8001);
  }

  GST_LOG_OBJECT (dcaparse, "frame header: %04x%04x%04x%04x",
      hdr[2], hdr[3], hdr[4], hdr[5]);

  *terminator = (hdr[2] & 0x80) ? FALSE : TRUE;
  *samples_per_block = ((hdr[2] >> 10) & 0x1f) + 1;
  *num_blocks = ((hdr[2] >> 2) & 0x7F) + 1;
  *frame_size = (((hdr[2] & 0x03) << 12) | (hdr[3] >> 4)) + 1;
  chans = ((hdr[3] & 0x0F) << 2) | (hdr[4] >> 14);
  *sample_rate = sample_rates[(hdr[4] >> 10) & 0x0F];
  lfe = (hdr[5] >> 9) & 0x03;

  GST_TRACE_OBJECT (dcaparse, "frame size %u, num_blocks %u, rate %u, "
      "samples per block %u", *frame_size, *num_blocks, *sample_rate,
      *samples_per_block);

  if (*num_blocks < 6 || *frame_size < 96 || *sample_rate == 0)
    return FALSE;

  if (marker == 0x1FFFE800 || marker == 0xFF1F00E8)
    *frame_size = (*frame_size * 16) / 14;      /* FIXME: round up? */

  if (chans < G_N_ELEMENTS (channels_table))
    *channels = channels_table[chans] + ((lfe) ? 1 : 0);
  else
    *channels = 0;

  if (depth)
    *depth = (marker == 0x1FFFE800 || marker == 0xFF1F00E8) ? 14 : 16;
  if (endianness)
    *endianness = (marker == 0xFE7F0180 || marker == 0xFF1F00E8) ?
        G_LITTLE_ENDIAN : G_BIG_ENDIAN;

  GST_TRACE_OBJECT (dcaparse, "frame size %u, channels %u, rate %u, "
      "num_blocks %u, samples_per_block %u", *frame_size, *channels,
      *sample_rate, *num_blocks, *samples_per_block);

  return TRUE;
}

static gint
gst_dca_parse_find_sync (GstDcaParse * dcaparse, GstByteReader * reader,
    const GstBuffer * buf, guint32 * sync)
{
  guint32 best_sync = 0;
  guint best_offset = G_MAXUINT;
  gint off;

  /* FIXME: verify syncs via _parse_header() here already */

  /* Raw little endian */
  off = gst_byte_reader_masked_scan_uint32 (reader, 0xffffffff, 0xfe7f0180,
      0, GST_BUFFER_SIZE (buf));
  if (off >= 0 && off < best_offset) {
    best_offset = off;
    best_sync = 0xfe7f0180;
  }

  /* Raw big endian */
  off = gst_byte_reader_masked_scan_uint32 (reader, 0xffffffff, 0x7ffe8001,
      0, GST_BUFFER_SIZE (buf));
  if (off >= 0 && off < best_offset) {
    best_offset = off;
    best_sync = 0x7ffe8001;
  }

  /* FIXME: check next 2 bytes as well for 14-bit formats (but then don't
   * forget to adjust the *skipsize= in _check_valid_frame() */

  /* 14-bit little endian  */
  off = gst_byte_reader_masked_scan_uint32 (reader, 0xffffffff, 0xff1f00e8,
      0, GST_BUFFER_SIZE (buf));
  if (off >= 0 && off < best_offset) {
    best_offset = off;
    best_sync = 0xff1f00e8;
  }

  /* 14-bit big endian  */
  off = gst_byte_reader_masked_scan_uint32 (reader, 0xffffffff, 0x1fffe800,
      0, GST_BUFFER_SIZE (buf));
  if (off >= 0 && off < best_offset) {
    best_offset = off;
    best_sync = 0x1fffe800;
  }

  if (best_offset == G_MAXUINT)
    return -1;

  *sync = best_sync;
  return best_offset;
}

static gboolean
gst_dca_parse_check_valid_frame (GstBaseParse * parse,
    GstBaseParseFrame * frame, guint * framesize, gint * skipsize)
{
  GstDcaParse *dcaparse = GST_DCA_PARSE (parse);
  GstBuffer *buf = frame->buffer;
  GstByteReader r = GST_BYTE_READER_INIT_FROM_BUFFER (buf);
  gboolean parser_draining;
  gboolean parser_in_sync;
  gboolean terminator;
  guint32 sync = 0;
  guint size, rate, chans, num_blocks, samples_per_block;
  gint off = -1;

  if (G_UNLIKELY (GST_BUFFER_SIZE (buf) < 16))
    return FALSE;

  parser_in_sync = !GST_BASE_PARSE_LOST_SYNC (parse);

  if (G_LIKELY (parser_in_sync && dcaparse->last_sync != 0)) {
    off = gst_byte_reader_masked_scan_uint32 (&r, 0xffffffff,
        dcaparse->last_sync, 0, GST_BUFFER_SIZE (buf));
  }

  if (G_UNLIKELY (off < 0)) {
    off = gst_dca_parse_find_sync (dcaparse, &r, buf, &sync);
  }

  /* didn't find anything that looks like a sync word, skip */
  if (off < 0) {
    *skipsize = GST_BUFFER_SIZE (buf) - 3;
    GST_DEBUG_OBJECT (dcaparse, "no sync, skipping %d bytes", *skipsize);
    return FALSE;
  }

  GST_LOG_OBJECT (parse, "possible sync %08x at buffer offset %d", sync, off);

  /* possible frame header, but not at offset 0? skip bytes before sync */
  if (off > 0) {
    *skipsize = off;
    return FALSE;
  }

  /* make sure the values in the frame header look sane */
  if (!gst_dca_parse_parse_header (dcaparse, &r, &size, &rate, &chans, NULL,
          NULL, &num_blocks, &samples_per_block, &terminator)) {
    *skipsize = 4;
    return FALSE;
  }

  GST_LOG_OBJECT (parse, "got frame, sync %08x, size %u, rate %d, channels %d",
      sync, size, rate, chans);

  *framesize = size;

  dcaparse->last_sync = sync;

  parser_draining = GST_BASE_PARSE_DRAINING (parse);

  if (!parser_in_sync && !parser_draining) {
    /* check for second frame to be sure */
    GST_DEBUG_OBJECT (dcaparse, "resyncing; checking next frame syncword");
    if (GST_BUFFER_SIZE (buf) >= (size + 16)) {
      guint s2, r2, c2, n2, s3;
      gboolean t;

      GST_MEMDUMP ("buf", GST_BUFFER_DATA (buf), size + 16);
      gst_byte_reader_init_from_buffer (&r, buf);
      gst_byte_reader_skip_unchecked (&r, size);

      if (!gst_dca_parse_parse_header (dcaparse, &r, &s2, &r2, &c2, NULL, NULL,
              &n2, &s3, &t)) {
        GST_DEBUG_OBJECT (dcaparse, "didn't find second syncword");
        *skipsize = 4;
        return FALSE;
      }

      /* ok, got sync now, let's assume constant frame size */
      gst_base_parse_set_min_frame_size (parse, size);
    } else {
      /* FIXME: baseparse always seems to hand us buffers of min_frame_size
       * bytes, which is unhelpful here */
      GST_LOG_OBJECT (dcaparse, "next sync out of reach (%u < %u)",
          GST_BUFFER_SIZE (buf), size + 16);
      /* *skipsize = 0; */
      /* return FALSE; */
    }
  }

  return TRUE;
}

static GstFlowReturn
gst_dca_parse_parse_frame (GstBaseParse * parse, GstBaseParseFrame * frame)
{
  GstDcaParse *dcaparse = GST_DCA_PARSE (parse);
  GstBuffer *buf = frame->buffer;
  GstByteReader r = GST_BYTE_READER_INIT_FROM_BUFFER (buf);
  guint size, rate, chans, depth, block_size, num_blocks, samples_per_block;
  gint endianness;
  gboolean terminator;

  if (!gst_dca_parse_parse_header (dcaparse, &r, &size, &rate, &chans, &depth,
          &endianness, &num_blocks, &samples_per_block, &terminator))
    goto broken_header;

  block_size = num_blocks * samples_per_block;

  if (G_UNLIKELY (dcaparse->rate != rate || dcaparse->channels != chans
          || dcaparse->depth != depth || dcaparse->endianness != endianness
          || (!terminator && dcaparse->block_size != block_size)
          || (size != dcaparse->frame_size))) {
    GstCaps *caps;

    caps = gst_caps_new_simple ("audio/x-dts",
        "framed", G_TYPE_BOOLEAN, TRUE,
        "rate", G_TYPE_INT, rate, "channels", G_TYPE_INT, chans,
        "endianness", G_TYPE_INT, endianness, "depth", G_TYPE_INT, depth,
        "block-size", G_TYPE_INT, block_size, "frame-size", G_TYPE_INT, size,
        NULL);
    gst_buffer_set_caps (buf, caps);
    gst_pad_set_caps (GST_BASE_PARSE_SRC_PAD (parse), caps);
    gst_caps_unref (caps);

    dcaparse->rate = rate;
    dcaparse->channels = chans;
    dcaparse->depth = depth;
    dcaparse->endianness = endianness;
    dcaparse->block_size = block_size;
    dcaparse->frame_size = size;

    gst_base_parse_set_frame_rate (parse, rate, block_size, 0, 0);
  }

  return GST_FLOW_OK;

/* ERRORS */
broken_header:
  {
    /* this really shouldn't ever happen */
    GST_ELEMENT_ERROR (parse, STREAM, DECODE, (NULL), (NULL));
    return GST_FLOW_ERROR;
  }
}
