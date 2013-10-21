/* -*- Mode: C; tab-width: 2; indent-tabs-mode: t; c-basic-offset: 2 -*- */
/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) <2006> Nokia Corporation, Stefan Kost <stefan.kost@nokia.com>.
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
 * SECTION:element-wavparse
 *
 * Parse a .wav file into raw or compressed audio.
 *
 * Wavparse supports both push and pull mode operations, making it possible to
 * stream from a network source.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch filesrc location=sine.wav ! wavparse ! audioconvert ! alsasink
 * ]| Read a wav file and output to the soundcard using the ALSA element. The
 * wav file is assumed to contain raw uncompressed samples.
 * |[
 * gst-launch gnomevfssrc location=http://www.example.org/sine.wav ! queue ! wavparse ! audioconvert ! alsasink
 * ]| Stream data from a network url.
 * </refsect2>
 *
 * Last reviewed on 2007-02-14 (0.10.6)
 */

/*
 * TODO:
 * http://replaygain.hydrogenaudio.org/file_format_wav.html
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include <string.h>
#include <math.h>

#include "gstwavparse.h"
#include "gst/riff/riff-ids.h"
#include "gst/riff/riff-media.h"
#include <gst/base/gsttypefindhelper.h>
#include <gst/gst-i18n-plugin.h>

#ifdef GSTREAMER_LITE
#include <fxplugins_common.h>
#endif // GSTREAMER_LITE

GST_DEBUG_CATEGORY_STATIC (wavparse_debug);
#define GST_CAT_DEFAULT (wavparse_debug)

static void gst_wavparse_dispose (GObject * object);

static gboolean gst_wavparse_sink_activate (GstPad * sinkpad);
static gboolean gst_wavparse_sink_activate_pull (GstPad * sinkpad,
    gboolean active);
static gboolean gst_wavparse_send_event (GstElement * element,
    GstEvent * event);
static GstStateChangeReturn gst_wavparse_change_state (GstElement * element,
    GstStateChange transition);
#ifdef GSTREAMER_LITE
static const GstQueryType* gst_wavparse_sink_query_types (GstPad * pad);
static gboolean gst_wavparse_sink_query (GstPad * pad, GstQuery * query);
#endif // GSTREAMER_LITE

static const GstQueryType *gst_wavparse_get_query_types (GstPad * pad);
static gboolean gst_wavparse_pad_query (GstPad * pad, GstQuery * query);
static gboolean gst_wavparse_pad_convert (GstPad * pad,
    GstFormat src_format,
    gint64 src_value, GstFormat * dest_format, gint64 * dest_value);

static GstFlowReturn gst_wavparse_chain (GstPad * pad, GstBuffer * buf);
static gboolean gst_wavparse_sink_event (GstPad * pad, GstEvent * event);
static void gst_wavparse_loop (GstPad * pad);
static gboolean gst_wavparse_srcpad_event (GstPad * pad, GstEvent * event);

static GstStaticPadTemplate sink_template_factory =
GST_STATIC_PAD_TEMPLATE ("wavparse_sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("audio/x-wav")
    );

#define DEBUG_INIT(bla) \
  GST_DEBUG_CATEGORY_INIT (wavparse_debug, "wavparse", 0, "WAV parser");

GST_BOILERPLATE_FULL (GstWavParse, gst_wavparse, GstElement,
    GST_TYPE_ELEMENT, DEBUG_INIT);

static void
gst_wavparse_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);
  GstPadTemplate *src_template;

  /* register pads */
  gst_element_class_add_pad_template (element_class,
      gst_static_pad_template_get (&sink_template_factory));

  src_template = gst_pad_template_new ("wavparse_src", GST_PAD_SRC,
      GST_PAD_SOMETIMES, gst_riff_create_audio_template_caps ());
  gst_element_class_add_pad_template (element_class, src_template);
  gst_object_unref (src_template);

  gst_element_class_set_details_simple (element_class, "WAV audio demuxer",
      "Codec/Demuxer/Audio",
      "Parse a .wav file into raw audio",
      "Erik Walthinsen <omega@cse.ogi.edu>");
}

static void
gst_wavparse_class_init (GstWavParseClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *object_class;

  gstelement_class = (GstElementClass *) klass;
  object_class = (GObjectClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  object_class->dispose = gst_wavparse_dispose;

  gstelement_class->change_state = gst_wavparse_change_state;
  gstelement_class->send_event = gst_wavparse_send_event;
}

static void
gst_wavparse_reset (GstWavParse * wav)
{
  wav->state = GST_WAVPARSE_START;

  /* These will all be set correctly in the fmt chunk */
  wav->depth = 0;
  wav->rate = 0;
  wav->width = 0;
  wav->channels = 0;
  wav->blockalign = 0;
  wav->bps = 0;
  wav->fact = 0;
  wav->offset = 0;
  wav->end_offset = 0;
  wav->dataleft = 0;
  wav->datasize = 0;
  wav->datastart = 0;
  wav->duration = 0;
  wav->got_fmt = FALSE;
  wav->first = TRUE;

  if (wav->seek_event)
    gst_event_unref (wav->seek_event);
  wav->seek_event = NULL;
  if (wav->adapter) {
    gst_adapter_clear (wav->adapter);
    g_object_unref (wav->adapter);
    wav->adapter = NULL;
  }
  if (wav->tags)
    gst_tag_list_free (wav->tags);
  wav->tags = NULL;
  if (wav->caps)
    gst_caps_unref (wav->caps);
  wav->caps = NULL;
  if (wav->start_segment)
    gst_event_unref (wav->start_segment);
  wav->start_segment = NULL;
  if (wav->close_segment)
    gst_event_unref (wav->close_segment);
  wav->close_segment = NULL;
}

static void
gst_wavparse_dispose (GObject * object)
{
  GstWavParse *wav = GST_WAVPARSE (object);

  GST_DEBUG_OBJECT (wav, "WAV: Dispose");
  gst_wavparse_reset (wav);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_wavparse_init (GstWavParse * wavparse, GstWavParseClass * g_class)
{
#ifdef GSTREAMER_LITE
	GstElementClass *klass = GST_ELEMENT_GET_CLASS (wavparse);
	GstPadTemplate *src_template;
#endif // GSTREAMER_LITE

  gst_wavparse_reset (wavparse);

  /* sink */
  wavparse->sinkpad =
      gst_pad_new_from_static_template (&sink_template_factory, "sink");
  gst_pad_set_activate_function (wavparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_sink_activate));
  gst_pad_set_activatepull_function (wavparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_sink_activate_pull));
  gst_pad_set_chain_function (wavparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_chain));
  gst_pad_set_event_function (wavparse->sinkpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_sink_event));

#ifdef GSTREAMER_LITE
    gst_pad_set_query_function (wavparse->sinkpad,
            GST_DEBUG_FUNCPTR (gst_wavparse_sink_query));
    gst_pad_set_query_type_function (wavparse->sinkpad,
            GST_DEBUG_FUNCPTR (gst_wavparse_sink_query_types));
#endif // GSTREAMER_LITE

  gst_element_add_pad (GST_ELEMENT_CAST (wavparse), wavparse->sinkpad);

#ifdef GSTREAMER_LITE
  src_template = gst_element_class_get_pad_template (klass, "wavparse_src");
  wavparse->srcpad = gst_pad_new_from_template (src_template, "src");
  gst_pad_use_fixed_caps (wavparse->srcpad);
  gst_pad_set_query_type_function (wavparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_get_query_types));
  gst_pad_set_query_function (wavparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_pad_query));
  gst_pad_set_event_function (wavparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_srcpad_event));
	gst_element_add_pad (GST_ELEMENT_CAST (wavparse), wavparse->srcpad);
#else // GSTREAMER_LITE
  /* src, will be created later */
  wavparse->srcpad = NULL;
#endif // GSTREAMER_LITE
}

static void
gst_wavparse_destroy_sourcepad (GstWavParse * wavparse)
{
  if (wavparse->srcpad) {
    gst_element_remove_pad (GST_ELEMENT_CAST (wavparse), wavparse->srcpad);
    wavparse->srcpad = NULL;
  }
}

static void
gst_wavparse_create_sourcepad (GstWavParse * wavparse)
{
  GstElementClass *klass = GST_ELEMENT_GET_CLASS (wavparse);
  GstPadTemplate *src_template;

  /* destroy previous one */
  gst_wavparse_destroy_sourcepad (wavparse);

  /* source */
  src_template = gst_element_class_get_pad_template (klass, "wavparse_src");
  wavparse->srcpad = gst_pad_new_from_template (src_template, "src");
  gst_pad_use_fixed_caps (wavparse->srcpad);
  gst_pad_set_query_type_function (wavparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_get_query_types));
  gst_pad_set_query_function (wavparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_pad_query));
  gst_pad_set_event_function (wavparse->srcpad,
      GST_DEBUG_FUNCPTR (gst_wavparse_srcpad_event));

  GST_DEBUG_OBJECT (wavparse, "srcpad created");
}

/* Compute (value * nom) % denom, avoiding overflow.  This can be used
 * to perform ceiling or rounding division together with
 * gst_util_uint64_scale[_int]. */
#define uint64_scale_modulo(val, nom, denom) \
  ((val % denom) * (nom % denom) % denom)

/* Like gst_util_uint64_scale, but performs ceiling division. */
static guint64
uint64_ceiling_scale_int (guint64 val, gint num, gint denom)
{
  guint64 result = gst_util_uint64_scale_int (val, num, denom);

  if (uint64_scale_modulo (val, num, denom) == 0)
    return result;
  else
    return result + 1;
}

/* Like gst_util_uint64_scale, but performs ceiling division. */
static guint64
uint64_ceiling_scale (guint64 val, guint64 num, guint64 denom)
{
  guint64 result = gst_util_uint64_scale (val, num, denom);

  if (uint64_scale_modulo (val, num, denom) == 0)
    return result;
  else
    return result + 1;
}


/* FIXME: why is that not in use? */
#if 0
static void
gst_wavparse_parse_adtl (GstWavParse * wavparse, int len)
{
  guint32 got_bytes;
  GstByteStream *bs = wavparse->bs;
  gst_riff_chunk *temp_chunk, chunk;
  guint8 *tempdata;
  struct _gst_riff_labl labl, *temp_labl;
  struct _gst_riff_ltxt ltxt, *temp_ltxt;
  struct _gst_riff_note note, *temp_note;
  char *label_name;
  GstProps *props;
  GstPropsEntry *entry;
  GstCaps *new_caps;
  GList *caps = NULL;

  props = wavparse->metadata->properties;

  while (len > 0) {
    got_bytes =
        gst_bytestream_peek_bytes (bs, &tempdata, sizeof (gst_riff_chunk));
    if (got_bytes != sizeof (gst_riff_chunk)) {
      return;
    }
    temp_chunk = (gst_riff_chunk *) tempdata;

    chunk.id = GUINT32_FROM_LE (temp_chunk->id);
    chunk.size = GUINT32_FROM_LE (temp_chunk->size);

    if (chunk.size == 0) {
      gst_bytestream_flush (bs, sizeof (gst_riff_chunk));
      len -= sizeof (gst_riff_chunk);
      continue;
    }

    switch (chunk.id) {
      case GST_RIFF_adtl_labl:
        got_bytes =
            gst_bytestream_peek_bytes (bs, &tempdata,
            sizeof (struct _gst_riff_labl));
        if (got_bytes != sizeof (struct _gst_riff_labl)) {
          return;
        }

        temp_labl = (struct _gst_riff_labl *) tempdata;
        labl.id = GUINT32_FROM_LE (temp_labl->id);
        labl.size = GUINT32_FROM_LE (temp_labl->size);
        labl.identifier = GUINT32_FROM_LE (temp_labl->identifier);

        gst_bytestream_flush (bs, sizeof (struct _gst_riff_labl));
        len -= sizeof (struct _gst_riff_labl);

        got_bytes = gst_bytestream_peek_bytes (bs, &tempdata, labl.size - 4);
        if (got_bytes != labl.size - 4) {
          return;
        }

        label_name = (char *) tempdata;

        gst_bytestream_flush (bs, ((labl.size - 4) + 1) & ~1);
        len -= (((labl.size - 4) + 1) & ~1);

        new_caps = gst_caps_new ("label",
            "application/x-gst-metadata",
            gst_props_new ("identifier", G_TYPE_INT (labl.identifier),
                "name", G_TYPE_STRING (label_name), NULL));

        if (gst_props_get (props, "labels", &caps, NULL)) {
          caps = g_list_append (caps, new_caps);
        } else {
          caps = g_list_append (NULL, new_caps);

          entry = gst_props_entry_new ("labels", GST_PROPS_GLIST (caps));
          gst_props_add_entry (props, entry);
        }

        break;

      case GST_RIFF_adtl_ltxt:
        got_bytes =
            gst_bytestream_peek_bytes (bs, &tempdata,
            sizeof (struct _gst_riff_ltxt));
        if (got_bytes != sizeof (struct _gst_riff_ltxt)) {
          return;
        }

        temp_ltxt = (struct _gst_riff_ltxt *) tempdata;
        ltxt.id = GUINT32_FROM_LE (temp_ltxt->id);
        ltxt.size = GUINT32_FROM_LE (temp_ltxt->size);
        ltxt.identifier = GUINT32_FROM_LE (temp_ltxt->identifier);
        ltxt.length = GUINT32_FROM_LE (temp_ltxt->length);
        ltxt.purpose = GUINT32_FROM_LE (temp_ltxt->purpose);
        ltxt.country = GUINT16_FROM_LE (temp_ltxt->country);
        ltxt.language = GUINT16_FROM_LE (temp_ltxt->language);
        ltxt.dialect = GUINT16_FROM_LE (temp_ltxt->dialect);
        ltxt.codepage = GUINT16_FROM_LE (temp_ltxt->codepage);

        gst_bytestream_flush (bs, sizeof (struct _gst_riff_ltxt));
        len -= sizeof (struct _gst_riff_ltxt);

        if (ltxt.size - 20 > 0) {
          got_bytes = gst_bytestream_peek_bytes (bs, &tempdata, ltxt.size - 20);
          if (got_bytes != ltxt.size - 20) {
            return;
          }

          gst_bytestream_flush (bs, ((ltxt.size - 20) + 1) & ~1);
          len -= (((ltxt.size - 20) + 1) & ~1);

          label_name = (char *) tempdata;
        } else {
          label_name = "";
        }

        new_caps = gst_caps_new ("ltxt",
            "application/x-gst-metadata",
            gst_props_new ("identifier", G_TYPE_INT (ltxt.identifier),
                "name", G_TYPE_STRING (label_name),
                "length", G_TYPE_INT (ltxt.length), NULL));

        if (gst_props_get (props, "ltxts", &caps, NULL)) {
          caps = g_list_append (caps, new_caps);
        } else {
          caps = g_list_append (NULL, new_caps);

          entry = gst_props_entry_new ("ltxts", GST_PROPS_GLIST (caps));
          gst_props_add_entry (props, entry);
        }

        break;

      case GST_RIFF_adtl_note:
        got_bytes =
            gst_bytestream_peek_bytes (bs, &tempdata,
            sizeof (struct _gst_riff_note));
        if (got_bytes != sizeof (struct _gst_riff_note)) {
          return;
        }

        temp_note = (struct _gst_riff_note *) tempdata;
        note.id = GUINT32_FROM_LE (temp_note->id);
        note.size = GUINT32_FROM_LE (temp_note->size);
        note.identifier = GUINT32_FROM_LE (temp_note->identifier);

        gst_bytestream_flush (bs, sizeof (struct _gst_riff_note));
        len -= sizeof (struct _gst_riff_note);

        got_bytes = gst_bytestream_peek_bytes (bs, &tempdata, note.size - 4);
        if (got_bytes != note.size - 4) {
          return;
        }

        gst_bytestream_flush (bs, ((note.size - 4) + 1) & ~1);
        len -= (((note.size - 4) + 1) & ~1);

        label_name = (char *) tempdata;

        new_caps = gst_caps_new ("note",
            "application/x-gst-metadata",
            gst_props_new ("identifier", G_TYPE_INT (note.identifier),
                "name", G_TYPE_STRING (label_name), NULL));

        if (gst_props_get (props, "notes", &caps, NULL)) {
          caps = g_list_append (caps, new_caps);
        } else {
          caps = g_list_append (NULL, new_caps);

          entry = gst_props_entry_new ("notes", GST_PROPS_GLIST (caps));
          gst_props_add_entry (props, entry);
        }

        break;

      default:
        g_print ("Unknown chunk: %" GST_FOURCC_FORMAT "\n",
            GST_FOURCC_ARGS (chunk.id));
        return;
    }
  }

  g_object_notify (G_OBJECT (wavparse), "metadata");
}

static void
gst_wavparse_parse_cues (GstWavParse * wavparse, int len)
{
  guint32 got_bytes;
  GstByteStream *bs = wavparse->bs;
  struct _gst_riff_cue *temp_cue, cue;
  struct _gst_riff_cuepoints *points;
  guint8 *tempdata;
  int i;
  GList *cues = NULL;
  GstPropsEntry *entry;

  while (len > 0) {
    int required;

    got_bytes =
        gst_bytestream_peek_bytes (bs, &tempdata,
        sizeof (struct _gst_riff_cue));
    temp_cue = (struct _gst_riff_cue *) tempdata;

    /* fixup for our big endian friends */
    cue.id = GUINT32_FROM_LE (temp_cue->id);
    cue.size = GUINT32_FROM_LE (temp_cue->size);
    cue.cuepoints = GUINT32_FROM_LE (temp_cue->cuepoints);

    gst_bytestream_flush (bs, sizeof (struct _gst_riff_cue));
    if (got_bytes != sizeof (struct _gst_riff_cue)) {
      return;
    }

    len -= sizeof (struct _gst_riff_cue);

    /* -4 because cue.size contains the cuepoints size
       and we've already flushed that out of the system */
    required = cue.size - 4;
    got_bytes = gst_bytestream_peek_bytes (bs, &tempdata, required);
    gst_bytestream_flush (bs, ((required) + 1) & ~1);
    if (got_bytes != required) {
      return;
    }

    len -= (((cue.size - 4) + 1) & ~1);

    /* now we have an array of struct _gst_riff_cuepoints in tempdata */
    points = (struct _gst_riff_cuepoints *) tempdata;

    for (i = 0; i < cue.cuepoints; i++) {
      GstCaps *caps;

      caps = gst_caps_new ("cues",
          "application/x-gst-metadata",
          gst_props_new ("identifier", G_TYPE_INT (points[i].identifier),
              "position", G_TYPE_INT (points[i].offset), NULL));
      cues = g_list_append (cues, caps);
    }

    entry = gst_props_entry_new ("cues", GST_PROPS_GLIST (cues));
    gst_props_add_entry (wavparse->metadata->properties, entry);
  }

  g_object_notify (G_OBJECT (wavparse), "metadata");
}

/* Read 'fmt ' header */
static gboolean
gst_wavparse_fmt (GstWavParse * wav)
{
  gst_riff_strf_auds *header = NULL;
  GstCaps *caps;

  if (!gst_riff_read_strf_auds (wav, &header))
    goto no_fmt;

  wav->format = header->format;
  wav->rate = header->rate;
  wav->channels = header->channels;
  if (wav->channels == 0)
    goto no_channels;

  wav->blockalign = header->blockalign;
  wav->width = (header->blockalign * 8) / header->channels;
  wav->depth = header->size;
  wav->bps = header->av_bps;
  if (wav->bps <= 0)
    goto no_bps;

  /* Note: gst_riff_create_audio_caps might need to fix values in
   * the header header depending on the format, so call it first */
  caps = gst_riff_create_audio_caps (header->format, NULL, header, NULL);
  g_free (header);

  if (caps == NULL)
    goto no_caps;

#ifndef GSTREAMER_LITE
  gst_wavparse_create_sourcepad (wav);
#endif // GSTREAMER_LITE
  gst_pad_use_fixed_caps (wav->srcpad);
  gst_pad_set_active (wav->srcpad, TRUE);
  gst_pad_set_caps (wav->srcpad, caps);
  gst_caps_free (caps);
#ifndef GSTREAMER_LITE
  gst_element_add_pad (GST_ELEMENT_CAST (wav), wav->srcpad);
  gst_element_no_more_pads (GST_ELEMENT_CAST (wav));
#endif // GSTREAMER_LITE

  GST_DEBUG ("frequency %d, channels %d", wav->rate, wav->channels);

  return TRUE;

  /* ERRORS */
no_fmt:
  {
    GST_ELEMENT_ERROR (wav, STREAM, TYPE_NOT_FOUND, (NULL),
        ("No FMT tag found"));
    return FALSE;
  }
no_channels:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Stream claims to contain zero channels - invalid data"));
    g_free (header);
    return FALSE;
  }
no_bps:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Stream claims to bitrate of <= zero - invalid data"));
    g_free (header);
    return FALSE;
  }
no_caps:
  {
    GST_ELEMENT_ERROR (wav, STREAM, TYPE_NOT_FOUND, (NULL), (NULL));
    return FALSE;
  }
}

static gboolean
gst_wavparse_other (GstWavParse * wav)
{
  guint32 tag, length;

  if (!gst_riff_peek_head (wav, &tag, &length, NULL)) {
    GST_WARNING_OBJECT (wav, "could not peek head");
    return FALSE;
  }
  GST_DEBUG_OBJECT (wav, "got tag (%08x) %4.4s, length %d", tag,
      (gchar *) & tag, length);

  switch (tag) {
    case GST_RIFF_TAG_LIST:
      if (!(tag = gst_riff_peek_list (wav))) {
        GST_WARNING_OBJECT (wav, "could not peek list");
        return FALSE;
      }

      switch (tag) {
        case GST_RIFF_LIST_INFO:
          if (!gst_riff_read_list (wav, &tag) || !gst_riff_read_info (wav)) {
            GST_WARNING_OBJECT (wav, "could not read list");
            return FALSE;
          }
          break;

        case GST_RIFF_LIST_adtl:
          if (!gst_riff_read_skip (wav)) {
            GST_WARNING_OBJECT (wav, "could not read skip");
            return FALSE;
          }
          break;

        default:
          GST_DEBUG_OBJECT (wav, "skipping tag (%08x) %4.4s", tag,
              (gchar *) & tag);
          if (!gst_riff_read_skip (wav)) {
            GST_WARNING_OBJECT (wav, "could not read skip");
            return FALSE;
          }
          break;
      }

      break;

    case GST_RIFF_TAG_data:
      if (!gst_bytestream_flush (wav->bs, 8)) {
        GST_WARNING_OBJECT (wav, "could not flush 8 bytes");
        return FALSE;
      }

      GST_DEBUG_OBJECT (wav, "switching to data mode");
      wav->state = GST_WAVPARSE_DATA;
      wav->datastart = gst_bytestream_tell (wav->bs);
      if (length == 0) {
        guint64 file_length;

        /* length is 0, data probably stretches to the end
         * of file */
        GST_DEBUG_OBJECT (wav, "length is 0 trying to find length");
        /* get length of file */
        file_length = gst_bytestream_length (wav->bs);
        if (file_length == -1) {
          GST_DEBUG_OBJECT (wav,
              "could not get file length, assuming data to eof");
          /* could not get length, assuming till eof */
          length = G_MAXUINT32;
        }
        if (file_length > G_MAXUINT32) {
          GST_DEBUG_OBJECT (wav, "file length %" G_GUINT64_FORMAT
              ", clipping to 32 bits", file_length);
          /* could not get length, assuming till eof */
          length = G_MAXUINT32;
        } else {
          GST_DEBUG_OBJECT (wav, "file length %" G_GUINT64_FORMAT
              ", datalength %u", file_length, length);
          /* substract offset of datastart from length */
          length = file_length - wav->datastart;
          GST_DEBUG_OBJECT (wav, "datalength %u", length);
        }
      }
      wav->datasize = (guint64) length;
      GST_DEBUG_OBJECT (wav, "datasize = %ld", length)
          break;

    case GST_RIFF_TAG_cue:
      if (!gst_riff_read_skip (wav)) {
        GST_WARNING_OBJECT (wav, "could not read skip");
        return FALSE;
      }
      break;

    default:
      GST_DEBUG_OBJECT (wav, "skipping tag (%08x) %4.4s", tag, (gchar *) & tag);
      if (!gst_riff_read_skip (wav))
        return FALSE;
      break;
  }

  return TRUE;
}
#endif


static gboolean
gst_wavparse_parse_file_header (GstElement * element, GstBuffer * buf)
{
  guint32 doctype;

  if (!gst_riff_parse_file_header (element, buf, &doctype))
    return FALSE;

  if (doctype != GST_RIFF_RIFF_WAVE)
    goto not_wav;

  return TRUE;

  /* ERRORS */
not_wav:
  {
    GST_ELEMENT_ERROR (element, STREAM, WRONG_TYPE, (NULL),
        ("File is not a WAVE file: %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (doctype)));
    return FALSE;
  }
}

static GstFlowReturn
gst_wavparse_stream_init (GstWavParse * wav)
{
  GstFlowReturn res;
  GstBuffer *buf = NULL;

  if ((res = gst_pad_pull_range (wav->sinkpad,
              wav->offset, 12, &buf)) != GST_FLOW_OK)
    return res;
  else if (!gst_wavparse_parse_file_header (GST_ELEMENT_CAST (wav), buf))
    return GST_FLOW_ERROR;

  wav->offset += 12;

  return GST_FLOW_OK;
}

static gboolean
gst_wavparse_time_to_bytepos (GstWavParse * wav, gint64 ts, gint64 * bytepos)
{
  /* -1 always maps to -1 */
  if (ts == -1) {
    *bytepos = -1;
    return TRUE;
  }

  /* 0 always maps to 0 */
  if (ts == 0) {
    *bytepos = 0;
    return TRUE;
  }

  if (wav->bps > 0) {
    *bytepos = uint64_ceiling_scale (ts, (guint64) wav->bps, GST_SECOND);
    return TRUE;
  } else if (wav->fact) {
    guint64 bps =
        gst_util_uint64_scale_int (wav->datasize, wav->rate, wav->fact);
    *bytepos = uint64_ceiling_scale (ts, bps, GST_SECOND);
    return TRUE;
  }

  return FALSE;
}

/* This function is used to perform seeks on the element.
 *
 * It also works when event is NULL, in which case it will just
 * start from the last configured segment. This technique is
 * used when activating the element and to perform the seek in
 * READY.
 */
static gboolean
gst_wavparse_perform_seek (GstWavParse * wav, GstEvent * event)
{
  gboolean res;
  gdouble rate;
  GstFormat format, bformat;
  GstSeekFlags flags;
  GstSeekType cur_type = GST_SEEK_TYPE_NONE, stop_type;
  gint64 cur, stop, upstream_size;
  gboolean flush;
  gboolean update;
  GstSegment seeksegment = { 0, };
  gint64 last_stop;

  if (event) {
    GST_DEBUG_OBJECT (wav, "doing seek with event");

    gst_event_parse_seek (event, &rate, &format, &flags,
        &cur_type, &cur, &stop_type, &stop);

    /* no negative rates yet */
    if (rate < 0.0)
      goto negative_rate;

    if (format != wav->segment.format) {
      GST_INFO_OBJECT (wav, "converting seek-event from %s to %s",
          gst_format_get_name (format),
          gst_format_get_name (wav->segment.format));
      res = TRUE;
      if (cur_type != GST_SEEK_TYPE_NONE)
        res =
            gst_pad_query_convert (wav->srcpad, format, cur,
            &wav->segment.format, &cur);
      if (res && stop_type != GST_SEEK_TYPE_NONE)
        res =
            gst_pad_query_convert (wav->srcpad, format, stop,
            &wav->segment.format, &stop);
      if (!res)
        goto no_format;

      format = wav->segment.format;
    }
  } else {
    GST_DEBUG_OBJECT (wav, "doing seek without event");
    flags = 0;
    rate = 1.0;
    cur_type = GST_SEEK_TYPE_SET;
    stop_type = GST_SEEK_TYPE_SET;
  }

  /* in push mode, we must delegate to upstream */
  if (wav->streaming) {
    gboolean res = FALSE;

    /* if streaming not yet started; only prepare initial newsegment */
    if (!event || wav->state != GST_WAVPARSE_DATA) {
      if (wav->start_segment)
        gst_event_unref (wav->start_segment);
      wav->start_segment =
          gst_event_new_new_segment (FALSE, wav->segment.rate,
          wav->segment.format, wav->segment.last_stop, wav->segment.duration,
          wav->segment.last_stop);
      res = TRUE;
    } else {
      /* convert seek positions to byte positions in data sections */
      if (format == GST_FORMAT_TIME) {
        /* should not fail */
        if (!gst_wavparse_time_to_bytepos (wav, cur, &cur))
          goto no_position;
        if (!gst_wavparse_time_to_bytepos (wav, stop, &stop))
          goto no_position;
      }
      /* mind sample boundary and header */
      if (cur >= 0) {
        cur -= (cur % wav->bytes_per_sample);
        cur += wav->datastart;
      }
      if (stop >= 0) {
        stop -= (stop % wav->bytes_per_sample);
        stop += wav->datastart;
      }
      GST_DEBUG_OBJECT (wav, "Pushing BYTE seek rate %g, "
          "start %" G_GINT64_FORMAT ", stop %" G_GINT64_FORMAT, rate, cur,
          stop);
      /* BYTE seek event */
      event = gst_event_new_seek (rate, GST_FORMAT_BYTES, flags, cur_type, cur,
          stop_type, stop);
      res = gst_pad_push_event (wav->sinkpad, event);
    }
    return res;
  }

  /* get flush flag */
  flush = flags & GST_SEEK_FLAG_FLUSH;

  /* now we need to make sure the streaming thread is stopped. We do this by
   * either sending a FLUSH_START event downstream which will cause the
   * streaming thread to stop with a WRONG_STATE.
   * For a non-flushing seek we simply pause the task, which will happen as soon
   * as it completes one iteration (and thus might block when the sink is
   * blocking in preroll). */
  if (flush) {
    if (wav->srcpad) {
      GST_DEBUG_OBJECT (wav, "sending flush start");
      gst_pad_push_event (wav->srcpad, gst_event_new_flush_start ());
    }
  } else {
    gst_pad_pause_task (wav->sinkpad);
  }

  /* we should now be able to grab the streaming thread because we stopped it
   * with the above flush/pause code */
  GST_PAD_STREAM_LOCK (wav->sinkpad);

  /* save current position */
  last_stop = wav->segment.last_stop;

  GST_DEBUG_OBJECT (wav, "stopped streaming at %" G_GINT64_FORMAT, last_stop);

  /* copy segment, we need this because we still need the old
   * segment when we close the current segment. */
  memcpy (&seeksegment, &wav->segment, sizeof (GstSegment));

  /* configure the seek parameters in the seeksegment. We will then have the
   * right values in the segment to perform the seek */
  if (event) {
    GST_DEBUG_OBJECT (wav, "configuring seek");
    gst_segment_set_seek (&seeksegment, rate, format, flags,
        cur_type, cur, stop_type, stop, &update);
  }

  /* figure out the last position we need to play. If it's configured (stop !=
   * -1), use that, else we play until the total duration of the file */
  if ((stop = seeksegment.stop) == -1)
    stop = seeksegment.duration;

  GST_DEBUG_OBJECT (wav, "cur_type =%d", cur_type);
  if ((cur_type != GST_SEEK_TYPE_NONE)) {
    /* bring offset to bytes, if the bps is 0, we have the segment in BYTES and
     * we can just copy the last_stop. If not, we use the bps to convert TIME to
     * bytes. */
    if (!gst_wavparse_time_to_bytepos (wav, seeksegment.last_stop,
            (gint64 *) & wav->offset))
      wav->offset = seeksegment.last_stop;
    GST_LOG_OBJECT (wav, "offset=%" G_GUINT64_FORMAT, wav->offset);
    wav->offset -= (wav->offset % wav->bytes_per_sample);
    GST_LOG_OBJECT (wav, "offset=%" G_GUINT64_FORMAT, wav->offset);
    wav->offset += wav->datastart;
    GST_LOG_OBJECT (wav, "offset=%" G_GUINT64_FORMAT, wav->offset);
  } else {
    GST_LOG_OBJECT (wav, "continue from offset=%" G_GUINT64_FORMAT,
        wav->offset);
  }

  if (stop_type != GST_SEEK_TYPE_NONE) {
    if (!gst_wavparse_time_to_bytepos (wav, stop, (gint64 *) & wav->end_offset))
      wav->end_offset = stop;
    GST_LOG_OBJECT (wav, "end_offset=%" G_GUINT64_FORMAT, wav->end_offset);
    wav->end_offset -= (wav->end_offset % wav->bytes_per_sample);
    GST_LOG_OBJECT (wav, "end_offset=%" G_GUINT64_FORMAT, wav->end_offset);
    wav->end_offset += wav->datastart;
    GST_LOG_OBJECT (wav, "end_offset=%" G_GUINT64_FORMAT, wav->end_offset);
  } else {
    GST_LOG_OBJECT (wav, "continue to end_offset=%" G_GUINT64_FORMAT,
        wav->end_offset);
  }

  /* make sure filesize is not exceeded due to rounding errors or so,
   * same precaution as in _stream_headers */
  bformat = GST_FORMAT_BYTES;
  if (gst_pad_query_peer_duration (wav->sinkpad, &bformat, &upstream_size))
    wav->end_offset = MIN (wav->end_offset, upstream_size);

  /* this is the range of bytes we will use for playback */
  wav->offset = MIN (wav->offset, wav->end_offset);
  wav->dataleft = wav->end_offset - wav->offset;

  GST_DEBUG_OBJECT (wav,
      "seek: rate %lf, offset %" G_GUINT64_FORMAT ", end %" G_GUINT64_FORMAT
      ", segment %" GST_TIME_FORMAT " -- %" GST_TIME_FORMAT, rate, wav->offset,
      wav->end_offset, GST_TIME_ARGS (seeksegment.start), GST_TIME_ARGS (stop));

  /* prepare for streaming again */
  if (wav->srcpad) {
    if (flush) {
      /* if we sent a FLUSH_START, we now send a FLUSH_STOP */
      GST_DEBUG_OBJECT (wav, "sending flush stop");
      gst_pad_push_event (wav->srcpad, gst_event_new_flush_stop ());
    } else if (wav->segment_running) {
      /* we are running the current segment and doing a non-flushing seek,
       * close the segment first based on the previous last_stop. */
      GST_DEBUG_OBJECT (wav, "closing running segment %" G_GINT64_FORMAT
          " to %" G_GINT64_FORMAT, wav->segment.start, wav->segment.last_stop);

      /* queue the segment for sending in the stream thread */
      if (wav->close_segment)
        gst_event_unref (wav->close_segment);
      wav->close_segment = gst_event_new_new_segment (TRUE,
          wav->segment.rate, wav->segment.format,
          wav->segment.start, wav->segment.last_stop, wav->segment.start);
    }
  }

  /* now we did the seek and can activate the new segment values */
  memcpy (&wav->segment, &seeksegment, sizeof (GstSegment));

  /* if we're doing a segment seek, post a SEGMENT_START message */
  if (wav->segment.flags & GST_SEEK_FLAG_SEGMENT) {
    gst_element_post_message (GST_ELEMENT_CAST (wav),
        gst_message_new_segment_start (GST_OBJECT_CAST (wav),
            wav->segment.format, wav->segment.last_stop));
  }

  /* now create the newsegment */
  GST_DEBUG_OBJECT (wav, "Creating newsegment from %" G_GINT64_FORMAT
      " to %" G_GINT64_FORMAT, wav->segment.last_stop, stop);

  /* store the newsegment event so it can be sent from the streaming thread. */
  if (wav->start_segment)
    gst_event_unref (wav->start_segment);
  wav->start_segment =
      gst_event_new_new_segment (FALSE, wav->segment.rate,
      wav->segment.format, wav->segment.last_stop, stop,
      wav->segment.last_stop);

  /* mark discont if we are going to stream from another position. */
  if (last_stop != wav->segment.last_stop) {
    GST_DEBUG_OBJECT (wav, "mark DISCONT, we did a seek to another position");
    wav->discont = TRUE;
  }

  /* and start the streaming task again */
  wav->segment_running = TRUE;
  if (!wav->streaming) {
    gst_pad_start_task (wav->sinkpad, (GstTaskFunction) gst_wavparse_loop,
        wav->sinkpad);
  }

  GST_PAD_STREAM_UNLOCK (wav->sinkpad);

  return TRUE;

  /* ERRORS */
negative_rate:
  {
    GST_DEBUG_OBJECT (wav, "negative playback rates are not supported yet.");
    return FALSE;
  }
no_format:
  {
    GST_DEBUG_OBJECT (wav, "unsupported format given, seek aborted.");
    return FALSE;
  }
no_position:
  {
    GST_DEBUG_OBJECT (wav,
        "Could not determine byte position for desired time");
    return FALSE;
  }
}

/*
 * gst_wavparse_peek_chunk_info:
 * @wav Wavparse object
 * @tag holder for tag
 * @size holder for tag size
 *
 * Peek next chunk info (tag and size)
 *
 * Returns: %TRUE when the chunk info (header) is available
 */
static gboolean
gst_wavparse_peek_chunk_info (GstWavParse * wav, guint32 * tag, guint32 * size)
{
  const guint8 *data = NULL;

  if (gst_adapter_available (wav->adapter) < 8)
    return FALSE;

  data = gst_adapter_peek (wav->adapter, 8);
  *tag = GST_READ_UINT32_LE (data);
  *size = GST_READ_UINT32_LE (data + 4);

  GST_DEBUG ("Next chunk size is %d bytes, type %" GST_FOURCC_FORMAT, *size,
      GST_FOURCC_ARGS (*tag));

  return TRUE;
}

/*
 * gst_wavparse_peek_chunk:
 * @wav Wavparse object
 * @tag holder for tag
 * @size holder for tag size
 *
 * Peek enough data for one full chunk
 *
 * Returns: %TRUE when the full chunk is available
 */
static gboolean
gst_wavparse_peek_chunk (GstWavParse * wav, guint32 * tag, guint32 * size)
{
  guint32 peek_size = 0;
  guint available;

  if (!gst_wavparse_peek_chunk_info (wav, tag, size))
    return FALSE;

  /* size 0 -> empty data buffer would surprise most callers,
   * large size -> do not bother trying to squeeze that into adapter,
   * so we throw poor man's exception, which can be caught if caller really
   * wants to handle 0 size chunk */
  if (!(*size) || (*size) >= (1 << 30)) {
    GST_INFO ("Invalid/unexpected chunk size %d for tag %" GST_FOURCC_FORMAT,
        *size, GST_FOURCC_ARGS (*tag));
    /* chain should give up */
    wav->abort_buffering = TRUE;
    return FALSE;
  }
  peek_size = (*size + 1) & ~1;
  available = gst_adapter_available (wav->adapter);

  if (available >= (8 + peek_size)) {
    return TRUE;
  } else {
    GST_LOG ("but only %u bytes available now", available);
    return FALSE;
  }
}

/*
 * gst_wavparse_calculate_duration:
 * @wav: wavparse object
 *
 * Calculate duration on demand and store in @wav. Prefer bps, but use fact as a
 * fallback.
 *
 * Returns: %TRUE if duration is available.
 */
static gboolean
gst_wavparse_calculate_duration (GstWavParse * wav)
{
  if (wav->duration > 0)
    return TRUE;

  if (wav->bps > 0) {
    GST_INFO_OBJECT (wav, "Got datasize %" G_GUINT64_FORMAT, wav->datasize);
    wav->duration =
        uint64_ceiling_scale (wav->datasize, GST_SECOND, (guint64) wav->bps);
    GST_INFO_OBJECT (wav, "Got duration (bps) %" GST_TIME_FORMAT,
        GST_TIME_ARGS (wav->duration));
    return TRUE;
  } else if (wav->fact) {
    wav->duration = uint64_ceiling_scale_int (GST_SECOND, wav->fact, wav->rate);
    GST_INFO_OBJECT (wav, "Got duration (fact) %" GST_TIME_FORMAT,
        GST_TIME_ARGS (wav->duration));
    return TRUE;
  }
  return FALSE;
}

static gboolean
gst_waveparse_ignore_chunk (GstWavParse * wav, GstBuffer * buf, guint32 tag,
    guint32 size)
{
  guint flush;

  if (wav->streaming) {
    if (!gst_wavparse_peek_chunk (wav, &tag, &size))
      return FALSE;
  }
  GST_DEBUG_OBJECT (wav, "Ignoring tag %" GST_FOURCC_FORMAT,
      GST_FOURCC_ARGS (tag));
  flush = 8 + ((size + 1) & ~1);
  wav->offset += flush;
  if (wav->streaming) {
    gst_adapter_flush (wav->adapter, flush);
  } else {
    gst_buffer_unref (buf);
  }

  return TRUE;
}

#define MAX_BUFFER_SIZE 4096

static GstFlowReturn
gst_wavparse_stream_headers (GstWavParse * wav)
{
  GstFlowReturn res = GST_FLOW_OK;
  GstBuffer *buf = NULL;
  gst_riff_strf_auds *header = NULL;
  guint32 tag, size;
  gboolean gotdata = FALSE;
  GstCaps *caps = NULL;
  gchar *codec_name = NULL;
  GstEvent **event_p;
  GstFormat bformat;
  gint64 upstream_size = 0;

  /* search for "_fmt" chunk, which should be first */
  while (!wav->got_fmt) {
    GstBuffer *extra;

    /* The header starts with a 'fmt ' tag */
    if (wav->streaming) {
      if (!gst_wavparse_peek_chunk (wav, &tag, &size))
        return res;

      gst_adapter_flush (wav->adapter, 8);
      wav->offset += 8;

      if (size) {
        buf = gst_adapter_take_buffer (wav->adapter, size);
        if (size & 1)
          gst_adapter_flush (wav->adapter, 1);
        wav->offset += GST_ROUND_UP_2 (size);
      } else {
        buf = gst_buffer_new ();
      }
    } else {
      if ((res = gst_riff_read_chunk (GST_ELEMENT_CAST (wav), wav->sinkpad,
                  &wav->offset, &tag, &buf)) != GST_FLOW_OK)
        return res;
    }

    if (tag == GST_RIFF_TAG_JUNK || tag == GST_RIFF_TAG_JUNQ ||
        tag == GST_RIFF_TAG_bext || tag == GST_RIFF_TAG_BEXT ||
        tag == GST_RIFF_TAG_LIST) {
      GST_DEBUG_OBJECT (wav, "skipping %" GST_FOURCC_FORMAT " chunk",
          GST_FOURCC_ARGS (tag));
      gst_buffer_unref (buf);
      buf = NULL;
      continue;
    }

    if (tag != GST_RIFF_TAG_fmt)
      goto invalid_wav;

    if (!(gst_riff_parse_strf_auds (GST_ELEMENT_CAST (wav), buf, &header,
                &extra)))
      goto parse_header_error;

    buf = NULL;                 /* parse_strf_auds() took ownership of buffer */

    /* do sanity checks of header fields */
    if (header->channels == 0)
      goto no_channels;
    if (header->rate == 0)
      goto no_rate;

    GST_DEBUG_OBJECT (wav, "creating the caps");

    /* Note: gst_riff_create_audio_caps might need to fix values in
     * the header header depending on the format, so call it first */
    caps = gst_riff_create_audio_caps (header->format, NULL, header, extra,
        NULL, &codec_name);

    if (extra)
      gst_buffer_unref (extra);

    if (!caps)
      goto unknown_format;

    /* do more sanity checks of header fields
     * (these can be sanitized by gst_riff_create_audio_caps()
     */
    wav->format = header->format;
    wav->rate = header->rate;
    wav->channels = header->channels;
    wav->blockalign = header->blockalign;
    wav->depth = header->size;
    wav->av_bps = header->av_bps;
    wav->vbr = FALSE;

    g_free (header);
    header = NULL;

    /* do format specific handling */
    switch (wav->format) {
      case GST_RIFF_WAVE_FORMAT_MPEGL12:
      case GST_RIFF_WAVE_FORMAT_MPEGL3:
      {
        /* Note: workaround for mp2/mp3 embedded in wav, that relies on the
         * bitrate inside the mpeg stream */
        GST_INFO ("resetting bps from %d to 0 for mp2/3", wav->av_bps);
        wav->bps = 0;
        break;
      }
      case GST_RIFF_WAVE_FORMAT_PCM:
        if (wav->blockalign > wav->channels * (guint) ceil (wav->depth / 8.0))
          goto invalid_blockalign;
        /* fall through */
      default:
        if (wav->av_bps > wav->blockalign * wav->rate)
          goto invalid_bps;
        /* use the configured bps */
        wav->bps = wav->av_bps;
        break;
    }

    wav->width = (wav->blockalign * 8) / wav->channels;
    wav->bytes_per_sample = wav->channels * wav->width / 8;

    if (wav->bytes_per_sample <= 0)
      goto no_bytes_per_sample;

    GST_DEBUG_OBJECT (wav, "blockalign = %u", (guint) wav->blockalign);
    GST_DEBUG_OBJECT (wav, "width      = %u", (guint) wav->width);
    GST_DEBUG_OBJECT (wav, "depth      = %u", (guint) wav->depth);
    GST_DEBUG_OBJECT (wav, "av_bps     = %u", (guint) wav->av_bps);
    GST_DEBUG_OBJECT (wav, "frequency  = %u", (guint) wav->rate);
    GST_DEBUG_OBJECT (wav, "channels   = %u", (guint) wav->channels);
    GST_DEBUG_OBJECT (wav, "bytes_per_sample = %u", wav->bytes_per_sample);

    /* bps can be 0 when we don't have a valid bitrate (mostly for compressed
     * formats). This will make the element output a BYTE format segment and
     * will not timestamp the outgoing buffers.
     */
    GST_DEBUG_OBJECT (wav, "bps        = %u", (guint) wav->bps);

    GST_DEBUG_OBJECT (wav, "caps = %" GST_PTR_FORMAT, caps);

    /* create pad later so we can sniff the first few bytes
     * of the real data and correct our caps if necessary */
    gst_caps_replace (&wav->caps, caps);
    gst_caps_replace (&caps, NULL);

    wav->got_fmt = TRUE;

    if (codec_name) {
      wav->tags = gst_tag_list_new ();

      gst_tag_list_add (wav->tags, GST_TAG_MERGE_REPLACE,
          GST_TAG_AUDIO_CODEC, codec_name, NULL);

      g_free (codec_name);
      codec_name = NULL;
    }

  }

  bformat = GST_FORMAT_BYTES;
  gst_pad_query_peer_duration (wav->sinkpad, &bformat, &upstream_size);
  GST_DEBUG_OBJECT (wav, "upstream size %" G_GUINT64_FORMAT, upstream_size);

  /* loop headers until we get data */
  while (!gotdata) {
    if (wav->streaming) {
      if (!gst_wavparse_peek_chunk_info (wav, &tag, &size))
        goto exit;
    } else {
      if ((res =
              gst_pad_pull_range (wav->sinkpad, wav->offset, 8,
                  &buf)) != GST_FLOW_OK)
#ifdef GSTREAMER_LITE
        if (res == GST_FLOW_WRONG_STATE)
          goto exit;
        else
#endif // GSTREAMER_LITE
        goto header_read_error;
      tag = GST_READ_UINT32_LE (GST_BUFFER_DATA (buf));
      size = GST_READ_UINT32_LE (GST_BUFFER_DATA (buf) + 4);
    }

    GST_INFO_OBJECT (wav,
        "Got TAG: %" GST_FOURCC_FORMAT ", offset %" G_GUINT64_FORMAT,
        GST_FOURCC_ARGS (tag), wav->offset);

    /* wav is a st00pid format, we don't know for sure where data starts.
     * So we have to go bit by bit until we find the 'data' header
     */
    switch (tag) {
      case GST_RIFF_TAG_data:{
        GST_DEBUG_OBJECT (wav, "Got 'data' TAG, size : %d", size);
        if (wav->streaming) {
          gst_adapter_flush (wav->adapter, 8);
          gotdata = TRUE;
        } else {
          gst_buffer_unref (buf);
        }
        wav->offset += 8;
        wav->datastart = wav->offset;
        /* If size is zero, then the data chunk probably actually extends to
           the end of the file */
        if (size == 0 && upstream_size) {
          size = upstream_size - wav->datastart;
        }
        /* Or the file might be truncated */
        else if (upstream_size) {
          size = MIN (size, (upstream_size - wav->datastart));
        }
        wav->datasize = (guint64) size;
        wav->dataleft = (guint64) size;
        wav->end_offset = size + wav->datastart;
        if (!wav->streaming) {
          /* We will continue parsing tags 'till end */
          wav->offset += size;
        }
        GST_DEBUG_OBJECT (wav, "datasize = %d", size);
        break;
      }
      case GST_RIFF_TAG_fact:{
        if (wav->format != GST_RIFF_WAVE_FORMAT_MPEGL12 &&
            wav->format != GST_RIFF_WAVE_FORMAT_MPEGL3) {
          const guint data_size = 4;

          GST_INFO_OBJECT (wav, "Have fact chunk");
          if (size < data_size) {
            if (!gst_waveparse_ignore_chunk (wav, buf, tag, size)) {
              /* need more data */
              goto exit;
            }
            GST_DEBUG_OBJECT (wav, "need %d, available %d; ignoring chunk",
                data_size, size);
            break;
          }
          /* number of samples (for compressed formats) */
          if (wav->streaming) {
            const guint8 *data = NULL;

            if (!gst_wavparse_peek_chunk (wav, &tag, &size)) {
              goto exit;
            }
            gst_adapter_flush (wav->adapter, 8);
            data = gst_adapter_peek (wav->adapter, data_size);
            wav->fact = GST_READ_UINT32_LE (data);
            gst_adapter_flush (wav->adapter, GST_ROUND_UP_2 (size));
          } else {
            gst_buffer_unref (buf);
            if ((res =
                    gst_pad_pull_range (wav->sinkpad, wav->offset + 8,
                        data_size, &buf)) != GST_FLOW_OK)
#ifdef GSTREAMER_LITE
            if (res == GST_FLOW_WRONG_STATE)
              goto exit;
            else
#endif // GSTREAMER_LITE
              goto header_read_error;
            wav->fact = GST_READ_UINT32_LE (GST_BUFFER_DATA (buf));
            gst_buffer_unref (buf);
          }
          GST_DEBUG_OBJECT (wav, "have fact %u", wav->fact);
          wav->offset += 8 + GST_ROUND_UP_2 (size);
          break;
        } else {
          if (!gst_waveparse_ignore_chunk (wav, buf, tag, size)) {
            /* need more data */
            goto exit;
          }
        }
        break;
      }
      case GST_RIFF_TAG_acid:{
        const gst_riff_acid *acid = NULL;
        const guint data_size = sizeof (gst_riff_acid);

        GST_INFO_OBJECT (wav, "Have acid chunk");
        if (size < data_size) {
          if (!gst_waveparse_ignore_chunk (wav, buf, tag, size)) {
            /* need more data */
            goto exit;
          }
          GST_DEBUG_OBJECT (wav, "need %d, available %d; ignoring chunk",
              data_size, size);
          break;
        }
        if (wav->streaming) {
          if (!gst_wavparse_peek_chunk (wav, &tag, &size)) {
            goto exit;
          }
          gst_adapter_flush (wav->adapter, 8);
          acid = (const gst_riff_acid *) gst_adapter_peek (wav->adapter,
              data_size);
        } else {
          gst_buffer_unref (buf);
          if ((res =
                  gst_pad_pull_range (wav->sinkpad, wav->offset + 8,
                      size, &buf)) != GST_FLOW_OK)
#ifdef GSTREAMER_LITE
          if (res == GST_FLOW_WRONG_STATE)
            goto exit;
          else
#endif // GSTREAMER_LITE
            goto header_read_error;
          acid = (const gst_riff_acid *) GST_BUFFER_DATA (buf);
        }
        /* send data as tags */
        if (!wav->tags)
          wav->tags = gst_tag_list_new ();
        gst_tag_list_add (wav->tags, GST_TAG_MERGE_REPLACE,
            GST_TAG_BEATS_PER_MINUTE, acid->tempo, NULL);

        size = GST_ROUND_UP_2 (size);
        if (wav->streaming) {
          gst_adapter_flush (wav->adapter, size);
        } else {
          gst_buffer_unref (buf);
        }
        wav->offset += 8 + size;
        break;
      }
        /* FIXME: all list tags after data are ignored in streaming mode */
      case GST_RIFF_TAG_LIST:{
        guint32 ltag;

        if (wav->streaming) {
          const guint8 *data = NULL;

          if (gst_adapter_available (wav->adapter) < 12) {
            goto exit;
          }
          data = gst_adapter_peek (wav->adapter, 12);
          ltag = GST_READ_UINT32_LE (data + 8);
        } else {
          gst_buffer_unref (buf);
          if ((res =
                  gst_pad_pull_range (wav->sinkpad, wav->offset, 12,
                      &buf)) != GST_FLOW_OK)
#ifdef GSTREAMER_LITE
          if (res == GST_FLOW_WRONG_STATE)
            goto exit;
          else
#endif // GSTREAMER_LITE
            goto header_read_error;
          ltag = GST_READ_UINT32_LE (GST_BUFFER_DATA (buf) + 8);
        }
        switch (ltag) {
          case GST_RIFF_LIST_INFO:{
            const gint data_size = size - 4;
            GstTagList *new;

            GST_INFO_OBJECT (wav, "Have LIST chunk INFO size %u", data_size);
            if (wav->streaming) {
              if (!gst_wavparse_peek_chunk (wav, &tag, &size)) {
                goto exit;
              }
              gst_adapter_flush (wav->adapter, 12);
              wav->offset += 12;
              if (data_size > 0) {
                buf = gst_adapter_take_buffer (wav->adapter, data_size);
                if (data_size & 1)
                  gst_adapter_flush (wav->adapter, 1);
              }
            } else {
              wav->offset += 12;
              gst_buffer_unref (buf);
              if (data_size > 0) {
                if ((res =
                        gst_pad_pull_range (wav->sinkpad, wav->offset,
                            data_size, &buf)) != GST_FLOW_OK)
#ifdef GSTREAMER_LITE
                if (res == GST_FLOW_WRONG_STATE)
                  goto exit;
                else
#endif // GSTREAMER_LITE
                  goto header_read_error;
              }
            }
            if (data_size > 0) {
              /* parse tags */
              gst_riff_parse_info (GST_ELEMENT (wav), buf, &new);
              if (new) {
                GstTagList *old = wav->tags;
                wav->tags =
                    gst_tag_list_merge (old, new, GST_TAG_MERGE_REPLACE);
                if (old)
                  gst_tag_list_free (old);
                gst_tag_list_free (new);
              }
              gst_buffer_unref (buf);
              wav->offset += GST_ROUND_UP_2 (data_size);
            }
            break;
          }
          default:
            GST_INFO_OBJECT (wav, "Ignoring LIST chunk %" GST_FOURCC_FORMAT,
                GST_FOURCC_ARGS (ltag));
            if (!gst_waveparse_ignore_chunk (wav, buf, tag, size))
              /* need more data */
              goto exit;
            break;
        }
        break;
      }
      default:
        if (!gst_waveparse_ignore_chunk (wav, buf, tag, size))
          /* need more data */
          goto exit;
        break;
    }

    if (upstream_size && (wav->offset >= upstream_size)) {
      /* Now we are gone through the whole file */
      gotdata = TRUE;
    }
  }

  GST_DEBUG_OBJECT (wav, "Finished parsing headers");

  if (wav->bps <= 0 && wav->fact) {
#if 0
    /* not a good idea, as for embedded mp2/mp3 we set bps to 0 earlier */
    wav->bps =
        (guint32) gst_util_uint64_scale ((guint64) wav->rate, wav->datasize,
        (guint64) wav->fact);
    GST_INFO_OBJECT (wav, "calculated bps : %d, enabling VBR", wav->bps);
#endif
    wav->vbr = TRUE;
  }

  if (gst_wavparse_calculate_duration (wav)) {
    gst_segment_init (&wav->segment, GST_FORMAT_TIME);
    gst_segment_set_duration (&wav->segment, GST_FORMAT_TIME, wav->duration);
  } else {
    /* no bitrate, let downstream peer do the math, we'll feed it bytes. */
    gst_segment_init (&wav->segment, GST_FORMAT_BYTES);
    gst_segment_set_duration (&wav->segment, GST_FORMAT_BYTES, wav->datasize);
  }

  /* now we have all the info to perform a pending seek if any, if no
   * event, this will still do the right thing and it will also send
   * the right newsegment event downstream. */
  gst_wavparse_perform_seek (wav, wav->seek_event);
  /* remove pending event */
  event_p = &wav->seek_event;
  gst_event_replace (event_p, NULL);

  /* we just started, we are discont */
  wav->discont = TRUE;

  wav->state = GST_WAVPARSE_DATA;

  /* determine reasonable max buffer size,
   * that is, buffers not too small either size or time wise
   * so we do not end up with too many of them */
  /* var abuse */
  upstream_size = 0;
  gst_wavparse_time_to_bytepos (wav, 40 * GST_MSECOND, &upstream_size);
  wav->max_buf_size = upstream_size;
  wav->max_buf_size = MAX (wav->max_buf_size, MAX_BUFFER_SIZE);
  if (wav->blockalign > 0)
    wav->max_buf_size -= (wav->max_buf_size % wav->blockalign);

  GST_DEBUG_OBJECT (wav, "max buffer size %d", wav->max_buf_size);

  return GST_FLOW_OK;

  /* ERROR */
exit:
  {
    if (codec_name)
      g_free (codec_name);
    if (header)
      g_free (header);
    if (caps)
      gst_caps_unref (caps);
    return res;
  }
fail:
  {
    res = GST_FLOW_ERROR;
    goto exit;
  }
invalid_wav:
  {
    GST_ELEMENT_ERROR (wav, STREAM, TYPE_NOT_FOUND, (NULL),
        ("Invalid WAV header (no fmt at start): %"
            GST_FOURCC_FORMAT, GST_FOURCC_ARGS (tag)));
    goto fail;
  }
parse_header_error:
  {
    GST_ELEMENT_ERROR (wav, STREAM, DEMUX, (NULL),
        ("Couldn't parse audio header"));
    goto fail;
  }
no_channels:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Stream claims to contain no channels - invalid data"));
    goto fail;
  }
no_rate:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Stream with sample_rate == 0 - invalid data"));
    goto fail;
  }
invalid_blockalign:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Stream claims blockalign = %u, which is more than %u - invalid data",
            wav->blockalign, wav->channels * (guint) ceil (wav->depth / 8.0)));
    goto fail;
  }
invalid_bps:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Stream claims av_bsp = %u, which is more than %u - invalid data",
            wav->av_bps, wav->blockalign * wav->rate));
    goto fail;
  }
no_bytes_per_sample:
  {
    GST_ELEMENT_ERROR (wav, STREAM, FAILED, (NULL),
        ("Could not caluclate bytes per sample - invalid data"));
    goto fail;
  }
unknown_format:
  {
    GST_ELEMENT_ERROR (wav, STREAM, TYPE_NOT_FOUND, (NULL),
        ("No caps found for format 0x%x, %d channels, %d Hz",
            wav->format, wav->channels, wav->rate));
    goto fail;
  }
header_read_error:
  {
    GST_ELEMENT_ERROR (wav, STREAM, DEMUX, (NULL),
        ("Couldn't read in header %d (%s)", res, gst_flow_get_name (res)));
    goto fail;
  }
}

/*
 * Read WAV file tag when streaming
 */
static GstFlowReturn
gst_wavparse_parse_stream_init (GstWavParse * wav)
{
  if (gst_adapter_available (wav->adapter) >= 12) {
    GstBuffer *tmp;

    /* _take flushes the data */
    tmp = gst_adapter_take_buffer (wav->adapter, 12);

    GST_DEBUG ("Parsing wav header");
    if (!gst_wavparse_parse_file_header (GST_ELEMENT_CAST (wav), tmp))
      return GST_FLOW_ERROR;

    wav->offset += 12;
    /* Go to next state */
    wav->state = GST_WAVPARSE_HEADER;
  }
  return GST_FLOW_OK;
}

/* handle an event sent directly to the element.
 *
 * This event can be sent either in the READY state or the
 * >READY state. The only event of interest really is the seek
 * event.
 *
 * In the READY state we can only store the event and try to
 * respect it when going to PAUSED. We assume we are in the
 * READY state when our parsing state != GST_WAVPARSE_DATA.
 *
 * When we are steaming, we can simply perform the seek right
 * away.
 */
static gboolean
gst_wavparse_send_event (GstElement * element, GstEvent * event)
{
  GstWavParse *wav = GST_WAVPARSE (element);
  gboolean res = FALSE;
  GstEvent **event_p;

  GST_DEBUG_OBJECT (wav, "received event %s", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEEK:
      if (wav->state == GST_WAVPARSE_DATA) {
        /* we can handle the seek directly when streaming data */
        res = gst_wavparse_perform_seek (wav, event);
      } else {
        GST_DEBUG_OBJECT (wav, "queuing seek for later");

        event_p = &wav->seek_event;
        gst_event_replace (event_p, event);

        /* we always return true */
        res = TRUE;
      }
      break;
    default:
      break;
  }
  gst_event_unref (event);
  return res;
}

static gboolean
gst_wavparse_have_dts_caps (const GstCaps * caps, GstTypeFindProbability prob)
{
  GstStructure *s;

  s = gst_caps_get_structure (caps, 0);
  if (!gst_structure_has_name (s, "audio/x-dts"))
    return FALSE;
  if (prob >= GST_TYPE_FIND_LIKELY)
    return TRUE;
  /* DTS at non-0 offsets and without second sync may yield POSSIBLE .. */
  if (prob < GST_TYPE_FIND_POSSIBLE)
    return FALSE;
  /* .. in which case we want at least a valid-looking rate and channels */
  if (!gst_structure_has_field (s, "channels"))
    return FALSE;
  /* and for extra assurance we could also check the rate from the DTS frame
   * against the one in the wav header, but for now let's not do that */
  return gst_structure_has_field (s, "rate");
}

static void
gst_wavparse_add_src_pad (GstWavParse * wav, GstBuffer * buf)
{
  GstStructure *s;

  GST_DEBUG_OBJECT (wav, "adding src pad");

  if (wav->caps) {
    s = gst_caps_get_structure (wav->caps, 0);
    if (s && gst_structure_has_name (s, "audio/x-raw-int") && buf != NULL) {
      GstTypeFindProbability prob;
      GstCaps *tf_caps;

      tf_caps = gst_type_find_helper_for_buffer (GST_OBJECT (wav), buf, &prob);
      if (tf_caps != NULL) {
        GST_LOG ("typefind caps = %" GST_PTR_FORMAT ", P=%d", tf_caps, prob);
        if (gst_wavparse_have_dts_caps (tf_caps, prob)) {
          GST_INFO_OBJECT (wav, "Found DTS marker in file marked as raw PCM");
          gst_caps_unref (wav->caps);
          wav->caps = tf_caps;

          gst_tag_list_add (wav->tags, GST_TAG_MERGE_REPLACE,
              GST_TAG_AUDIO_CODEC, "dts", NULL);
        } else {
          GST_DEBUG_OBJECT (wav, "found caps %" GST_PTR_FORMAT " for stream "
              "marked as raw PCM audio, but ignoring for now", tf_caps);
          gst_caps_unref (tf_caps);
        }
      }
    }
  }

#ifndef GSTREAMER_LITE
  gst_wavparse_create_sourcepad (wav);
#endif // GSTREAMER_LITE
  gst_pad_set_active (wav->srcpad, TRUE);
  gst_pad_set_caps (wav->srcpad, wav->caps);
  gst_caps_replace (&wav->caps, NULL);

#ifndef GSTREAMER_LITE
  gst_element_add_pad (GST_ELEMENT_CAST (wav), wav->srcpad);
  gst_element_no_more_pads (GST_ELEMENT_CAST (wav));
#endif // GSTREAMER_LITE

  if (wav->close_segment) {
    GST_DEBUG_OBJECT (wav, "Send close segment event on newpad");
    gst_pad_push_event (wav->srcpad, wav->close_segment);
    wav->close_segment = NULL;
  }
  if (wav->start_segment) {
    GST_DEBUG_OBJECT (wav, "Send start segment event on newpad");
    gst_pad_push_event (wav->srcpad, wav->start_segment);
    wav->start_segment = NULL;
  }

  if (wav->tags) {
    gst_element_found_tags_for_pad (GST_ELEMENT_CAST (wav), wav->srcpad,
        wav->tags);
    wav->tags = NULL;
  }
}

static GstFlowReturn
gst_wavparse_stream_data (GstWavParse * wav)
{
  GstBuffer *buf = NULL;
  GstFlowReturn res = GST_FLOW_OK;
  guint64 desired, obtained;
  GstClockTime timestamp, next_timestamp, duration;
  guint64 pos, nextpos;

iterate_adapter:
  GST_LOG_OBJECT (wav,
      "offset: %" G_GINT64_FORMAT " , end: %" G_GINT64_FORMAT " , dataleft: %"
      G_GINT64_FORMAT, wav->offset, wav->end_offset, wav->dataleft);

  /* Get the next n bytes and output them */
  if (wav->dataleft == 0 || wav->dataleft < wav->blockalign)
    goto found_eos;

  /* scale the amount of data by the segment rate so we get equal
   * amounts of data regardless of the playback rate */
  desired =
      MIN (gst_guint64_to_gdouble (wav->dataleft),
      wav->max_buf_size * wav->segment.abs_rate);

  if (desired >= wav->blockalign && wav->blockalign > 0)
    desired -= (desired % wav->blockalign);

  GST_LOG_OBJECT (wav, "Fetching %" G_GINT64_FORMAT " bytes of data "
      "from the sinkpad", desired);

  if (wav->streaming) {
    guint avail = gst_adapter_available (wav->adapter);
    guint extra;

    /* flush some bytes if evil upstream sends segment that starts
     * before data or does is not send sample aligned segment */
    if (G_LIKELY (wav->offset >= wav->datastart)) {
      extra = (wav->offset - wav->datastart) % wav->bytes_per_sample;
    } else {
      extra = wav->datastart - wav->offset;
    }

    if (G_UNLIKELY (extra)) {
      extra = wav->bytes_per_sample - extra;
      if (extra <= avail) {
        GST_DEBUG_OBJECT (wav, "flushing %d bytes to sample boundary", extra);
        gst_adapter_flush (wav->adapter, extra);
        wav->offset += extra;
        wav->dataleft -= extra;
        goto iterate_adapter;
      } else {
        GST_DEBUG_OBJECT (wav, "flushing %d bytes", avail);
        gst_adapter_clear (wav->adapter);
        wav->offset += avail;
        wav->dataleft -= avail;
        return GST_FLOW_OK;
      }
    }

    if (avail < desired) {
      GST_LOG_OBJECT (wav, "Got only %d bytes of data from the sinkpad", avail);
      return GST_FLOW_OK;
    }

    buf = gst_adapter_take_buffer (wav->adapter, desired);
  } else {
    if ((res = gst_pad_pull_range (wav->sinkpad, wav->offset,
                desired, &buf)) != GST_FLOW_OK)
      goto pull_error;

    /* we may get a short buffer at the end of the file */
    if (GST_BUFFER_SIZE (buf) < desired) {
      GST_LOG_OBJECT (wav, "Got only %u bytes of data", GST_BUFFER_SIZE (buf));
      if (GST_BUFFER_SIZE (buf) >= wav->blockalign) {
        buf = gst_buffer_make_metadata_writable (buf);
        GST_BUFFER_SIZE (buf) -= (GST_BUFFER_SIZE (buf) % wav->blockalign);
      } else {
        gst_buffer_unref (buf);
        goto found_eos;
      }
    }
  }

  obtained = GST_BUFFER_SIZE (buf);

  /* our positions in bytes */
  pos = wav->offset - wav->datastart;
  nextpos = pos + obtained;

  /* update offsets, does not overflow. */
  GST_BUFFER_OFFSET (buf) = pos / wav->bytes_per_sample;
  GST_BUFFER_OFFSET_END (buf) = nextpos / wav->bytes_per_sample;

  /* first chunk of data? create the source pad. We do this only here so
   * we can detect broken .wav files with dts disguised as raw PCM (sigh) */
  if (G_UNLIKELY (wav->first)) {
    wav->first = FALSE;
    /* this will also push the segment events */
    gst_wavparse_add_src_pad (wav, buf);
  } else {
    /* If we have a pending close/start segment, send it now. */
    if (G_UNLIKELY (wav->close_segment != NULL)) {
      gst_pad_push_event (wav->srcpad, wav->close_segment);
      wav->close_segment = NULL;
    }
    if (G_UNLIKELY (wav->start_segment != NULL)) {
      gst_pad_push_event (wav->srcpad, wav->start_segment);
      wav->start_segment = NULL;
    }
  }

  if (wav->bps > 0) {
    /* and timestamps if we have a bitrate, be careful for overflows */
    timestamp = uint64_ceiling_scale (pos, GST_SECOND, (guint64) wav->bps);
    next_timestamp =
        uint64_ceiling_scale (nextpos, GST_SECOND, (guint64) wav->bps);
    duration = next_timestamp - timestamp;

    /* update current running segment position */
    if (G_LIKELY (next_timestamp >= wav->segment.start))
      gst_segment_set_last_stop (&wav->segment, GST_FORMAT_TIME,
          next_timestamp);
  } else if (wav->fact) {
    guint64 bps =
        gst_util_uint64_scale_int (wav->datasize, wav->rate, wav->fact);
    /* and timestamps if we have a bitrate, be careful for overflows */
    timestamp = uint64_ceiling_scale (pos, GST_SECOND, bps);
    next_timestamp = uint64_ceiling_scale (nextpos, GST_SECOND, bps);
    duration = next_timestamp - timestamp;
  } else {
    /* no bitrate, all we know is that the first sample has timestamp 0, all
     * other positions and durations have unknown timestamp. */
    if (pos == 0)
      timestamp = 0;
    else
      timestamp = GST_CLOCK_TIME_NONE;
    duration = GST_CLOCK_TIME_NONE;
    /* update current running segment position with byte offset */
    if (G_LIKELY (nextpos >= wav->segment.start))
      gst_segment_set_last_stop (&wav->segment, GST_FORMAT_BYTES, nextpos);
  }
  if ((pos > 0) && wav->vbr) {
    /* don't set timestamps for VBR files if it's not the first buffer */
    timestamp = GST_CLOCK_TIME_NONE;
    duration = GST_CLOCK_TIME_NONE;
  }
  if (wav->discont) {
    GST_DEBUG_OBJECT (wav, "marking DISCONT");
    GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_DISCONT);
    wav->discont = FALSE;
  }

  GST_BUFFER_TIMESTAMP (buf) = timestamp;
  GST_BUFFER_DURATION (buf) = duration;

  /* don't forget to set the caps on the buffer */
  gst_buffer_set_caps (buf, GST_PAD_CAPS (wav->srcpad));

  GST_LOG_OBJECT (wav,
      "Got buffer. timestamp:%" GST_TIME_FORMAT " , duration:%" GST_TIME_FORMAT
      ", size:%u", GST_TIME_ARGS (timestamp), GST_TIME_ARGS (duration),
      GST_BUFFER_SIZE (buf));

  if ((res = gst_pad_push (wav->srcpad, buf)) != GST_FLOW_OK)
    goto push_error;

  if (obtained < wav->dataleft) {
    wav->offset += obtained;
    wav->dataleft -= obtained;
  } else {
    wav->offset += wav->dataleft;
    wav->dataleft = 0;
  }

  /* Iterate until need more data, so adapter size won't grow */
  if (wav->streaming) {
    GST_LOG_OBJECT (wav,
        "offset: %" G_GINT64_FORMAT " , end: %" G_GINT64_FORMAT, wav->offset,
        wav->end_offset);
    goto iterate_adapter;
  }
  return res;

  /* ERROR */
found_eos:
  {
    GST_DEBUG_OBJECT (wav, "found EOS");
    return GST_FLOW_UNEXPECTED;
  }
pull_error:
  {
    /* check if we got EOS */
    if (res == GST_FLOW_UNEXPECTED)
      goto found_eos;

    GST_WARNING_OBJECT (wav,
        "Error getting %" G_GINT64_FORMAT " bytes from the "
        "sinkpad (dataleft = %" G_GINT64_FORMAT ")", desired, wav->dataleft);
    return res;
  }
push_error:
  {
    GST_INFO_OBJECT (wav,
        "Error pushing on srcpad %s:%s, reason %s, is linked? = %d",
        GST_DEBUG_PAD_NAME (wav->srcpad), gst_flow_get_name (res),
        gst_pad_is_linked (wav->srcpad));
    return res;
  }
}

static void
gst_wavparse_loop (GstPad * pad)
{
  GstFlowReturn ret;
  GstWavParse *wav = GST_WAVPARSE (GST_PAD_PARENT (pad));

  GST_LOG_OBJECT (wav, "process data");

  switch (wav->state) {
    case GST_WAVPARSE_START:
      GST_INFO_OBJECT (wav, "GST_WAVPARSE_START");
      if ((ret = gst_wavparse_stream_init (wav)) != GST_FLOW_OK)
        goto pause;

      wav->state = GST_WAVPARSE_HEADER;
      /* fall-through */

    case GST_WAVPARSE_HEADER:
      GST_INFO_OBJECT (wav, "GST_WAVPARSE_HEADER");
      if ((ret = gst_wavparse_stream_headers (wav)) != GST_FLOW_OK)
        goto pause;

      wav->state = GST_WAVPARSE_DATA;
      GST_INFO_OBJECT (wav, "GST_WAVPARSE_DATA");
      /* fall-through */

    case GST_WAVPARSE_DATA:
      if ((ret = gst_wavparse_stream_data (wav)) != GST_FLOW_OK)
        goto pause;
      break;
    default:
      g_assert_not_reached ();
  }
  return;

  /* ERRORS */
pause:
  {
    const gchar *reason = gst_flow_get_name (ret);

    GST_DEBUG_OBJECT (wav, "pausing task, reason %s", reason);
    wav->segment_running = FALSE;
    gst_pad_pause_task (pad);

    if (ret == GST_FLOW_UNEXPECTED) {
      /* add pad before we perform EOS */
      if (G_UNLIKELY (wav->first)) {
        wav->first = FALSE;
        gst_wavparse_add_src_pad (wav, NULL);
      }

      if (wav->state == GST_WAVPARSE_START)
        GST_ELEMENT_ERROR (wav, STREAM, WRONG_TYPE,
            ("No valid input found before end of stream"), (NULL));

      /* perform EOS logic */
      if (wav->segment.flags & GST_SEEK_FLAG_SEGMENT) {
        GstClockTime stop;

        if ((stop = wav->segment.stop) == -1)
          stop = wav->segment.duration;

        gst_element_post_message (GST_ELEMENT_CAST (wav),
            gst_message_new_segment_done (GST_OBJECT_CAST (wav),
                wav->segment.format, stop));
      } else {
        if (wav->srcpad != NULL)
          gst_pad_push_event (wav->srcpad, gst_event_new_eos ());
      }
    } else if (ret == GST_FLOW_NOT_LINKED || ret < GST_FLOW_UNEXPECTED) {
      /* for fatal errors we post an error message, post the error
       * first so the app knows about the error first. */
      GST_ELEMENT_ERROR (wav, STREAM, FAILED,
          (_("Internal data flow error.")),
          ("streaming task paused, reason %s (%d)", reason, ret));
      if (wav->srcpad != NULL)
        gst_pad_push_event (wav->srcpad, gst_event_new_eos ());
    }
    return;
  }
}

static GstFlowReturn
gst_wavparse_chain (GstPad * pad, GstBuffer * buf)
{
  GstFlowReturn ret;
  GstWavParse *wav = GST_WAVPARSE (GST_PAD_PARENT (pad));

  GST_LOG_OBJECT (wav, "adapter_push %u bytes", GST_BUFFER_SIZE (buf));

  gst_adapter_push (wav->adapter, buf);

  switch (wav->state) {
    case GST_WAVPARSE_START:
      GST_INFO_OBJECT (wav, "GST_WAVPARSE_START");
      if ((ret = gst_wavparse_parse_stream_init (wav)) != GST_FLOW_OK)
        goto done;

      if (wav->state != GST_WAVPARSE_HEADER)
        break;

      /* otherwise fall-through */
    case GST_WAVPARSE_HEADER:
      GST_INFO_OBJECT (wav, "GST_WAVPARSE_HEADER");
      if ((ret = gst_wavparse_stream_headers (wav)) != GST_FLOW_OK)
        goto done;

      if (!wav->got_fmt || wav->datastart == 0)
        break;

      wav->state = GST_WAVPARSE_DATA;
      GST_INFO_OBJECT (wav, "GST_WAVPARSE_DATA");

      /* fall-through */
    case GST_WAVPARSE_DATA:
      if (buf && GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DISCONT))
        wav->discont = TRUE;
      if ((ret = gst_wavparse_stream_data (wav)) != GST_FLOW_OK)
        goto done;
      break;
    default:
      g_return_val_if_reached (GST_FLOW_ERROR);
  }
done:
  if (G_UNLIKELY (wav->abort_buffering)) {
    wav->abort_buffering = FALSE;
    ret = GST_FLOW_ERROR;
    /* sort of demux/parse error */
    GST_ELEMENT_ERROR (wav, STREAM, DEMUX, (NULL), ("unhandled buffer size"));
  }
#ifdef GSTREAMER_LITE
  else if (G_UNLIKELY(ret != GST_FLOW_OK && ret != GST_FLOW_WRONG_STATE))
  {
      GST_ELEMENT_ERROR (wav, STREAM, FAILED,
          (_("Internal data flow error.")),
          ("streaming task paused, reason %s (%d)",
              gst_flow_get_name (ret), ret));
  }
#endif // GSTREAMER_LITE

  return ret;
}

static GstFlowReturn
gst_wavparse_flush_data (GstWavParse * wav)
{
  GstFlowReturn ret = GST_FLOW_OK;
  guint av;

  if ((av = gst_adapter_available (wav->adapter)) > 0) {
    wav->dataleft = av;
    wav->end_offset = wav->offset + av;
    ret = gst_wavparse_stream_data (wav);
  }

  return ret;
}

static gboolean
gst_wavparse_sink_event (GstPad * pad, GstEvent * event)
{
  GstWavParse *wav = GST_WAVPARSE (GST_PAD_PARENT (pad));
  gboolean ret = TRUE;

  GST_LOG_OBJECT (wav, "handling %s event", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_NEWSEGMENT:
    {
      GstFormat format;
      gdouble rate, arate;
      gint64 start, stop, time, offset = 0, end_offset = -1;
      gboolean update;
      GstSegment segment;

      /* some debug output */
      gst_segment_init (&segment, GST_FORMAT_UNDEFINED);
      gst_event_parse_new_segment_full (event, &update, &rate, &arate, &format,
          &start, &stop, &time);
      gst_segment_set_newsegment_full (&segment, update, rate, arate, format,
          start, stop, time);
      GST_DEBUG_OBJECT (wav,
          "received format %d newsegment %" GST_SEGMENT_FORMAT, format,
          &segment);

      if (wav->state != GST_WAVPARSE_DATA) {
        GST_DEBUG_OBJECT (wav, "still starting, eating event");
        goto exit;
      }

      /* now we are either committed to TIME or BYTE format,
       * and we only expect a BYTE segment, e.g. following a seek */
      if (format == GST_FORMAT_BYTES) {
        if (start > 0) {
          offset = start;
          start -= wav->datastart;
          start = MAX (start, 0);
        }
        if (stop > 0) {
          end_offset = stop;
          stop -= wav->datastart;
          stop = MAX (stop, 0);
        }
        if (wav->segment.format == GST_FORMAT_TIME) {
          guint64 bps = wav->bps;

          /* operating in format TIME, so we can convert */
          if (!bps && wav->fact)
            bps =
                gst_util_uint64_scale_int (wav->datasize, wav->rate, wav->fact);
          if (bps) {
            if (start >= 0)
              start =
                  uint64_ceiling_scale (start, GST_SECOND, (guint64) wav->bps);
            if (stop >= 0)
              stop =
                  uint64_ceiling_scale (stop, GST_SECOND, (guint64) wav->bps);
          }
        }
      } else {
        GST_DEBUG_OBJECT (wav, "unsupported segment format, ignoring");
        goto exit;
      }

      /* accept upstream's notion of segment and distribute along */
      gst_segment_set_newsegment_full (&wav->segment, update, rate, arate,
          wav->segment.format, start, stop, start);
      /* also store the newsegment event for the streaming thread */
      if (wav->start_segment)
        gst_event_unref (wav->start_segment);
      wav->start_segment =
          gst_event_new_new_segment_full (update, rate, arate,
          wav->segment.format, start, stop, start);
      GST_DEBUG_OBJECT (wav, "Pushing newseg update %d, rate %g, "
          "applied rate %g, format %d, start %" G_GINT64_FORMAT ", "
          "stop %" G_GINT64_FORMAT, update, rate, arate, wav->segment.format,
          start, stop);

      /* stream leftover data in current segment */
      gst_wavparse_flush_data (wav);
      /* and set up streaming thread for next one */
      wav->offset = offset;
      wav->end_offset = end_offset;
      if (wav->end_offset > 0) {
        wav->dataleft = wav->end_offset - wav->offset;
      } else {
        /* infinity; upstream will EOS when done */
        wav->dataleft = G_MAXUINT64;
      }
    exit:
      gst_event_unref (event);
      break;
    }
#ifdef GSTREAMER_LITE
    case FX_EVENT_RANGE_READY: // This event appears only in pull mode during outrange seeking.
        ret = gst_pad_start_task (pad, (GstTaskFunction) gst_wavparse_loop, pad);
        gst_event_unref(event);
        break;
#endif // GSTREAMER_LITE
    case GST_EVENT_EOS:
      /* add pad if needed so EOS is seen downstream */
      if (G_UNLIKELY (wav->first)) {
        wav->first = FALSE;
        gst_wavparse_add_src_pad (wav, NULL);
      } else {
        /* stream leftover data in current segment */
        gst_wavparse_flush_data (wav);
      }

      if (wav->state == GST_WAVPARSE_START)
        GST_ELEMENT_ERROR (wav, STREAM, WRONG_TYPE,
            ("No valid input found before end of stream"), (NULL));

      /* fall-through */
    case GST_EVENT_FLUSH_STOP:
      gst_adapter_clear (wav->adapter);
      wav->discont = TRUE;
      /* fall-through */
    default:
      ret = gst_pad_event_default (wav->sinkpad, event);
      break;
  }

  return ret;
}

#if 0
/* convert and query stuff */
static const GstFormat *
gst_wavparse_get_formats (GstPad * pad)
{
  static GstFormat formats[] = {
    GST_FORMAT_TIME,
    GST_FORMAT_BYTES,
    GST_FORMAT_DEFAULT,         /* a "frame", ie a set of samples per Hz */
    0
  };

  return formats;
}
#endif

static gboolean
gst_wavparse_pad_convert (GstPad * pad,
    GstFormat src_format, gint64 src_value,
    GstFormat * dest_format, gint64 * dest_value)
{
  GstWavParse *wavparse;
  gboolean res = TRUE;

  wavparse = GST_WAVPARSE (GST_PAD_PARENT (pad));

  if (*dest_format == src_format) {
    *dest_value = src_value;
    return TRUE;
  }

  if ((wavparse->bps == 0) && !wavparse->fact)
    goto no_bps_fact;

  GST_INFO_OBJECT (wavparse, "converting value from %s to %s",
      gst_format_get_name (src_format), gst_format_get_name (*dest_format));

  switch (src_format) {
    case GST_FORMAT_BYTES:
      switch (*dest_format) {
        case GST_FORMAT_DEFAULT:
          *dest_value = src_value / wavparse->bytes_per_sample;
          /* make sure we end up on a sample boundary */
          *dest_value -= *dest_value % wavparse->bytes_per_sample;
          break;
        case GST_FORMAT_TIME:
          /* src_value + datastart = offset */
          GST_INFO_OBJECT (wavparse,
              "src=%" G_GINT64_FORMAT ", offset=%" G_GINT64_FORMAT, src_value,
              wavparse->offset);
          if (wavparse->bps > 0)
            *dest_value = uint64_ceiling_scale (src_value, GST_SECOND,
                (guint64) wavparse->bps);
          else if (wavparse->fact) {
            guint64 bps = uint64_ceiling_scale_int (wavparse->datasize,
                wavparse->rate, wavparse->fact);

            *dest_value = uint64_ceiling_scale_int (src_value, GST_SECOND, bps);
          } else {
            res = FALSE;
          }
          break;
        default:
          res = FALSE;
          goto done;
      }
      break;

    case GST_FORMAT_DEFAULT:
      switch (*dest_format) {
        case GST_FORMAT_BYTES:
          *dest_value = src_value * wavparse->bytes_per_sample;
          break;
        case GST_FORMAT_TIME:
          *dest_value = gst_util_uint64_scale (src_value, GST_SECOND,
              (guint64) wavparse->rate);
          break;
        default:
          res = FALSE;
          goto done;
      }
      break;

    case GST_FORMAT_TIME:
      switch (*dest_format) {
        case GST_FORMAT_BYTES:
          if (wavparse->bps > 0)
            *dest_value = gst_util_uint64_scale (src_value,
                (guint64) wavparse->bps, GST_SECOND);
          else {
            guint64 bps = gst_util_uint64_scale_int (wavparse->datasize,
                wavparse->rate, wavparse->fact);

            *dest_value = gst_util_uint64_scale (src_value, bps, GST_SECOND);
          }
          /* make sure we end up on a sample boundary */
          *dest_value -= *dest_value % wavparse->blockalign;
          break;
        case GST_FORMAT_DEFAULT:
          *dest_value = gst_util_uint64_scale (src_value,
              (guint64) wavparse->rate, GST_SECOND);
          break;
        default:
          res = FALSE;
          goto done;
      }
      break;

    default:
      res = FALSE;
      goto done;
  }

done:
  return res;

  /* ERRORS */
no_bps_fact:
  {
    GST_DEBUG_OBJECT (wavparse, "bps 0 or no fact chunk, cannot convert");
    res = FALSE;
    goto done;
  }
}

static const GstQueryType *
gst_wavparse_get_query_types (GstPad * pad)
{
  static const GstQueryType types[] = {
    GST_QUERY_POSITION,
    GST_QUERY_DURATION,
    GST_QUERY_CONVERT,
    GST_QUERY_SEEKING,
    0
  };

  return types;
}

/* handle queries for location and length in requested format */
static gboolean
gst_wavparse_pad_query (GstPad * pad, GstQuery * query)
{
  gboolean res = TRUE;
  GstWavParse *wav = GST_WAVPARSE (gst_pad_get_parent (pad));

  /* only if we know */
  if (wav->state != GST_WAVPARSE_DATA) {
    gst_object_unref (wav);
    return FALSE;
  }

  GST_LOG_OBJECT (pad, "%s query", GST_QUERY_TYPE_NAME (query));

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_POSITION:
    {
      gint64 curb;
      gint64 cur;
      GstFormat format;

      /* this is not very precise, as we have pushed severla buffer upstream for prerolling */
      curb = wav->offset - wav->datastart;
      gst_query_parse_position (query, &format, NULL);
      GST_INFO_OBJECT (wav, "pos query at %" G_GINT64_FORMAT, curb);

      switch (format) {
        case GST_FORMAT_TIME:
          res = gst_wavparse_pad_convert (pad, GST_FORMAT_BYTES, curb,
              &format, &cur);
          break;
        default:
          format = GST_FORMAT_BYTES;
          cur = curb;
          break;
      }
      if (res)
        gst_query_set_position (query, format, cur);
      break;
    }
    case GST_QUERY_DURATION:
    {
      gint64 duration = 0;
      GstFormat format;

      gst_query_parse_duration (query, &format, NULL);

      switch (format) {
        case GST_FORMAT_TIME:{
          if ((res = gst_wavparse_calculate_duration (wav))) {
            duration = wav->duration;
          }
          break;
        }
        default:
          format = GST_FORMAT_BYTES;
          duration = wav->datasize;
          break;
      }
      gst_query_set_duration (query, format, duration);
      break;
    }
    case GST_QUERY_CONVERT:
    {
      gint64 srcvalue, dstvalue;
      GstFormat srcformat, dstformat;

      gst_query_parse_convert (query, &srcformat, &srcvalue,
          &dstformat, &dstvalue);
      res = gst_wavparse_pad_convert (pad, srcformat, srcvalue,
          &dstformat, &dstvalue);
      if (res)
        gst_query_set_convert (query, srcformat, srcvalue, dstformat, dstvalue);
      break;
    }
    case GST_QUERY_SEEKING:{
      GstFormat fmt;
      gboolean seekable = FALSE;

      gst_query_parse_seeking (query, &fmt, NULL, NULL, NULL);
      if (fmt == wav->segment.format) {
        if (wav->streaming) {
          GstQuery *q;

          q = gst_query_new_seeking (GST_FORMAT_BYTES);
          if ((res = gst_pad_peer_query (wav->sinkpad, q))) {
            gst_query_parse_seeking (q, &fmt, &seekable, NULL, NULL);
            GST_LOG_OBJECT (wav, "upstream BYTE seekable %d", seekable);
          }
          gst_query_unref (q);
        } else {
          GST_LOG_OBJECT (wav, "looping => seekable");
          seekable = TRUE;
          res = TRUE;
        }
      } else if (fmt == GST_FORMAT_TIME) {
        res = TRUE;
      }
      if (res) {
        gst_query_set_seeking (query, fmt, seekable, 0, wav->segment.duration);
      }
      break;
    }
    default:
      res = gst_pad_query_default (pad, query);
      break;
  }
  gst_object_unref (wav);
  return res;
}

#ifdef GSTREAMER_LITE
static const GstQueryType* 
gst_wavparse_sink_query_types (GstPad* pad)
{
  static const GstQueryType query_types[] = {
    GST_QUERY_CUSTOM,
    0
  };

  return query_types;
}

static gboolean 
gst_wavparse_sink_query (GstPad* pad, GstQuery* query)
{
    gboolean result = TRUE;
    switch (GST_QUERY_TYPE(query)) 
    {
        case GST_QUERY_CUSTOM:
        {
            GstStructure *s = gst_query_get_structure(query);
            if (gst_structure_has_name(s, GETRANGE_QUERY_NAME))
                gst_structure_set(s, GETRANGE_QUERY_SUPPORTS_FIELDNANE, 
                                     GETRANGE_QUERY_SUPPORTS_FIELDTYPE, 
                                     TRUE, 
                                     NULL);
            break;
        }
        default:
            result = gst_pad_query_default(pad, query);
            break;
    }
    return result;
}
#endif // GSTREAMER_LITE

static gboolean
gst_wavparse_srcpad_event (GstPad * pad, GstEvent * event)
{
  GstWavParse *wavparse = GST_WAVPARSE (gst_pad_get_parent (pad));
  gboolean res = FALSE;

  GST_DEBUG_OBJECT (wavparse, "%s event", GST_EVENT_TYPE_NAME (event));

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_SEEK:
      /* can only handle events when we are in the data state */
      if (wavparse->state == GST_WAVPARSE_DATA) {
        res = gst_wavparse_perform_seek (wavparse, event);
      }
      gst_event_unref (event);
      break;
    default:
      res = gst_pad_push_event (wavparse->sinkpad, event);
      break;
  }
  gst_object_unref (wavparse);
  return res;
}

static gboolean
gst_wavparse_sink_activate (GstPad * sinkpad)
{
  GstWavParse *wav = GST_WAVPARSE (gst_pad_get_parent (sinkpad));
  gboolean res;

  if (wav->adapter) {
    gst_adapter_clear (wav->adapter);
    g_object_unref (wav->adapter);
    wav->adapter = NULL;
  }

  if (gst_pad_check_pull_range (sinkpad)) {
    GST_DEBUG ("going to pull mode");
    wav->streaming = FALSE;
    res = gst_pad_activate_pull (sinkpad, TRUE);
  } else {
    GST_DEBUG ("going to push (streaming) mode");
    wav->streaming = TRUE;
    wav->adapter = gst_adapter_new ();
    res = gst_pad_activate_push (sinkpad, TRUE);
  }
  gst_object_unref (wav);
  return res;
}


static gboolean
gst_wavparse_sink_activate_pull (GstPad * sinkpad, gboolean active)
{
  GstWavParse *wav = GST_WAVPARSE (GST_OBJECT_PARENT (sinkpad));

  if (active) {
    /* if we have a scheduler we can start the task */
    wav->segment_running = TRUE;
    return gst_pad_start_task (sinkpad, (GstTaskFunction) gst_wavparse_loop,
        sinkpad);
  } else {
    wav->segment_running = FALSE;
    return gst_pad_stop_task (sinkpad);
  }
};

static GstStateChangeReturn
gst_wavparse_change_state (GstElement * element, GstStateChange transition)
{
  GstStateChangeReturn ret;
  GstWavParse *wav = GST_WAVPARSE (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      gst_wavparse_reset (wav);
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
#ifndef GSTREAMER_LITE
      gst_wavparse_destroy_sourcepad (wav);
#endif // GSTREAMER_LITE
      gst_wavparse_reset (wav);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      break;
    default:
      break;
  }
  return ret;
}

#ifdef GSTREAMER_LITE
gboolean
plugin_init_wavparse (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  gst_riff_init ();

  return gst_element_register (plugin, "wavparse", GST_RANK_PRIMARY,
      GST_TYPE_WAVPARSE);
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "wavparse",
    "Parse a .wav file into raw audio",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif // GSTREAMER_LITE