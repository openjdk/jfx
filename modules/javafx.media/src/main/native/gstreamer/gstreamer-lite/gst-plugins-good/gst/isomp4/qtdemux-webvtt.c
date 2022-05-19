/* GStreamer
 * Copyright (C) 2008 Thijs Vermeir <thijsvermeir@gmail.com>
 * Copyright (C) 2011 David Schleef <ds@schleef.org>
 * Copyright (C) 2021 Jan Schmidt <jan@centricular.com>
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

#include "qtdemux-webvtt.h"
#include <gst/base/gstbytereader.h>

#include "fourcc.h"
#include "qtdemux.h"
#include "qtatomparser.h"

#include <stdlib.h>
#include <string.h>

GST_DEBUG_CATEGORY_EXTERN (qtdemux_debug);
#define GST_CAT_DEFAULT qtdemux_debug

gboolean
qtdemux_webvtt_is_empty (GstQTDemux * demux, guint8 * data, gsize size)
{
  GstByteReader br;
  guint32 atom_size;
  guint32 atom_type;

  gst_byte_reader_init (&br, data, size);
  if (gst_byte_reader_get_remaining (&br) < 8)
    return FALSE;

  if (!gst_byte_reader_get_uint32_be (&br, &atom_size) ||
      !qt_atom_parser_get_fourcc (&br, &atom_type))
    return FALSE;

  if (atom_type == FOURCC_vtte)
    return TRUE;

  return FALSE;
}

struct WebvttCue
{
  const guint8 *cue_id;
  guint32 cue_id_len;

  const guint8 *cue_time;
  guint32 cue_time_len;

  const guint8 *settings;
  guint32 settings_len;

  const guint8 *cue_text;
  guint32 cue_text_len;
};

static void
webvtt_append_timestamp_to_string (GstClockTime timestamp, GString * str)
{
  guint h, m, s, ms;

  h = timestamp / (3600 * GST_SECOND);

  timestamp -= h * 3600 * GST_SECOND;
  m = timestamp / (60 * GST_SECOND);

  timestamp -= m * 60 * GST_SECOND;
  s = timestamp / GST_SECOND;

  timestamp -= s * GST_SECOND;
  ms = timestamp / GST_MSECOND;

  g_string_append_printf (str, "%02d:%02d:%02d.%03d", h, m, s, ms);
}

static gboolean
webvtt_decode_vttc (GstQTDemux * qtdemux, GstByteReader * br,
    GstClockTime start, GstClockTime duration, GString * s)
{
  struct WebvttCue cue = { 0, };
  gboolean have_data = FALSE;

  while (gst_byte_reader_get_remaining (br) >= 8) {
    guint32 atom_size;
    guint32 atom_type;
    guint next_pos;

    if (!gst_byte_reader_get_uint32_be (br, &atom_size) ||
        !qt_atom_parser_get_fourcc (br, &atom_type))
      break;

    if (gst_byte_reader_get_remaining (br) < atom_size - 8)
      break;
    next_pos = gst_byte_reader_get_pos (br) - 8 + atom_size;

    GST_LOG_OBJECT (qtdemux, "WebVTT cue atom %" GST_FOURCC_FORMAT " len %u",
        GST_FOURCC_ARGS (atom_type), atom_size);

    switch (atom_type) {
      case FOURCC_ctim:
        if (!gst_byte_reader_get_data (br, atom_size - 8, &cue.cue_time))
          return FALSE;
        cue.cue_time_len = atom_size - 8;
        break;
      case FOURCC_iden:
        if (!gst_byte_reader_get_data (br, atom_size - 8, &cue.cue_id))
          return FALSE;
        cue.cue_id_len = atom_size - 8;
        break;
      case FOURCC_sttg:
        if (!gst_byte_reader_get_data (br, atom_size - 8, &cue.settings))
          return FALSE;
        cue.settings_len = atom_size - 8;
        break;
      case FOURCC_payl:
        if (!gst_byte_reader_get_data (br, atom_size - 8, &cue.cue_text))
          return FALSE;
        cue.cue_text_len = atom_size - 8;
        have_data = TRUE;
        break;
    }

    if (!gst_byte_reader_set_pos (br, next_pos))
      break;
  }

  if (have_data) {
    if (cue.cue_id)
      g_string_append_printf (s, "%.*s\n", cue.cue_id_len, cue.cue_id);

    /* Write the cue time and optional settings */
    webvtt_append_timestamp_to_string (start, s);
    g_string_append_printf (s, " --> ");
    webvtt_append_timestamp_to_string (start + duration, s);

    if (cue.settings)
      g_string_append_printf (s, " %.*s\n", cue.settings_len, cue.settings);
    else
      g_string_append (s, "\n");

    g_string_append_printf (s, "%.*s\n\n", cue.cue_text_len, cue.cue_text);
  }

  return have_data;
}

GstBuffer *
qtdemux_webvtt_decode (GstQTDemux * qtdemux, GstClockTime start,
    GstClockTime duration, guint8 * data, gsize size)
{
  GstByteReader br;
  GString *str = NULL;
  GstBuffer *buf = NULL;

  gst_byte_reader_init (&br, data, size);
  while (gst_byte_reader_get_remaining (&br) >= 8) {
    guint32 atom_size;
    guint32 atom_type;
    guint next_pos;

    if (!gst_byte_reader_get_uint32_be (&br, &atom_size) ||
        !qt_atom_parser_get_fourcc (&br, &atom_type))
      break;

    if (gst_byte_reader_get_remaining (&br) < atom_size - 8)
      break;
    next_pos = gst_byte_reader_get_pos (&br) - 8 + atom_size;

    switch (atom_type) {
      case FOURCC_vttc:
        GST_LOG_OBJECT (qtdemux,
            "WebVTT cue atom %" GST_FOURCC_FORMAT " len %u",
            GST_FOURCC_ARGS (atom_type), atom_size);
        if (str == NULL)
          str = g_string_new (NULL);
        if (!webvtt_decode_vttc (qtdemux, &br, start, duration, str))
          break;
        break;
      case FOURCC_vtte:
        /* The empty segment case should be handled separately using qtdemux_webvtt_is_empty().
         * Ignore it during decode */
        break;
      case FOURCC_vtta:
        /* extra attributes */
        break;
      default:
        GST_DEBUG_OBJECT (qtdemux,
            "Unknown WebVTT sample atom %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (atom_type));
        break;
    }
    if (!gst_byte_reader_set_pos (&br, next_pos))
      break;
  }

  if (str) {
    gsize webvtt_len = str->len;
    gchar *webvtt_chunk = g_string_free (str, FALSE);
    buf = gst_buffer_new_wrapped (webvtt_chunk, webvtt_len);
  }

  return buf;
}
