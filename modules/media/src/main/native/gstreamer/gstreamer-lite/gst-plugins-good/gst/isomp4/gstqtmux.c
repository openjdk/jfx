/* Quicktime muxer plugin for GStreamer
 * Copyright (C) 2008-2010 Thiago Santos <thiagoss@embedded.ufcg.edu.br>
 * Copyright (C) 2008 Mark Nauwelaerts <mnauw@users.sf.net>
 * Copyright (C) 2010 Nokia Corporation. All rights reserved.
 * Contact: Stefan Kost <stefan.kost@nokia.com>

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
/*
 * Unless otherwise indicated, Source Code is licensed under MIT license.
 * See further explanation attached in License Statement (distributed in the file
 * LICENSE).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


/**
 * SECTION:element-qtmux
 * @short_description: Muxer for quicktime(.mov) files
 *
 * This element merges streams (audio and video) into QuickTime(.mov) files.
 *
 * The following background intends to explain why various similar muxers
 * are present in this plugin.
 *
 * The <ulink url="http://www.apple.com/quicktime/resources/qtfileformat.pdf">
 * QuickTime file format specification</ulink> served as basis for the MP4 file
 * format specification (mp4mux), and as such the QuickTime file structure is
 * nearly identical to the so-called ISO Base Media file format defined in
 * ISO 14496-12 (except for some media specific parts).
 * In turn, the latter ISO Base Media format was further specialized as a
 * Motion JPEG-2000 file format in ISO 15444-3 (mj2mux)
 * and in various 3GPP(2) specs (gppmux).
 * The fragmented file features defined (only) in ISO Base Media are used by
 * ISMV files making up (a.o.) Smooth Streaming (ismlmux).
 *
 * A few properties (<link linkend="GstQTMux--movie-timescale">movie-timescale</link>,
 * <link linkend="GstQTMux--trak-timescale">trak-timescale</link>) allow adjusting
 * some technical parameters, which might be useful in (rare) cases to resolve
 * compatibility issues in some situations.
 *
 * Some other properties influence the result more fundamentally.
 * A typical mov/mp4 file's metadata (aka moov) is located at the end of the file,
 * somewhat contrary to this usually being called "the header".
 * However, a <link linkend="GstQTMux--faststart">faststart</link> file will
 * (with some effort) arrange this to be located near start of the file,
 * which then allows it e.g. to be played while downloading.
 * Alternatively, rather than having one chunk of metadata at start (or end),
 * there can be some metadata at start and most of the other data can be spread
 * out into fragments of <link linkend="GstQTMux--fragment-duration">fragment-duration</link>.
 * If such fragmented layout is intended for streaming purposes, then
 * <link linkend="GstQTMux--streamable">streamable</link> allows foregoing to add
 * index metadata (at the end of file).
 *
 * <link linkend="GstQTMux--dts-method">dts-method</link> allows selecting a
 * method for managing input timestamps (stay tuned for 0.11 to have this
 * automagically settled).  The default delta/duration method should handle nice
 * (aka perfect streams) just fine, but may experience problems otherwise
 * (e.g. input stream with re-ordered B-frames and/or with frame dropping).
 * The re-ordering approach re-assigns incoming timestamps in ascending order
 * to incoming buffers and offers an alternative in such cases.  In cases where
 * that might fail, the remaining method can be tried, which is exact and
 * according to specs, but might experience playback on not so spec-wise players.
 * Note that this latter approach also requires one to enable
 * <link linkend="GstQTMux--presentation-timestamp">presentation-timestamp</link>.
 *
 * <refsect2>
 * <title>Example pipelines</title>
 * |[
 * gst-launch v4l2src num-buffers=500 ! video/x-raw-yuv,width=320,height=240 ! ffmpegcolorspace ! qtmux ! filesink location=video.mov
 * ]|
 * Records a video stream captured from a v4l2 device and muxes it into a qt file.
 * </refsect2>
 *
 * Last reviewed on 2010-12-03
 */

/*
 * Based on avimux
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <glib/gstdio.h>

#include <gst/gst.h>
#include <gst/base/gstcollectpads.h>
#include <gst/tag/xmpwriter.h>

#include <sys/types.h>
#ifdef G_OS_WIN32
#include <io.h>                 /* lseek, open, close, read */
#undef lseek
#define lseek _lseeki64
#undef off_t
#define off_t guint64
#endif

#ifdef _MSC_VER
#define ftruncate g_win32_ftruncate
#endif

#ifdef HAVE_UNISTD_H
#  include <unistd.h>
#endif

#include "gstqtmux.h"

GST_DEBUG_CATEGORY_STATIC (gst_qt_mux_debug);
#define GST_CAT_DEFAULT gst_qt_mux_debug

enum
{
  DTS_METHOD_DD,
  DTS_METHOD_REORDER,
  DTS_METHOD_ASC
};

static GType
gst_qt_mux_dts_method_get_type (void)
{
  static GType gst_qt_mux_dts_method = 0;

  if (!gst_qt_mux_dts_method) {
    static const GEnumValue dts_methods[] = {
      {DTS_METHOD_DD, "delta/duration", "dd"},
      {DTS_METHOD_REORDER, "reorder", "reorder"},
      {DTS_METHOD_ASC, "ascending", "asc"},
      {0, NULL, NULL},
    };

    gst_qt_mux_dts_method =
        g_enum_register_static ("GstQTMuxDtsMethods", dts_methods);
  }

  return gst_qt_mux_dts_method;
}

#define GST_TYPE_QT_MUX_DTS_METHOD \
  (gst_qt_mux_dts_method_get_type ())

/* QTMux signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_MOVIE_TIMESCALE,
  PROP_TRAK_TIMESCALE,
  PROP_FAST_START,
  PROP_FAST_START_TEMP_FILE,
  PROP_MOOV_RECOV_FILE,
  PROP_FRAGMENT_DURATION,
  PROP_STREAMABLE,
  PROP_DTS_METHOD,
  PROP_DO_CTTS,
};

/* some spare for header size as well */
#define MDAT_LARGE_FILE_LIMIT           ((guint64) 1024 * 1024 * 1024 * 2)
#define MAX_TOLERATED_LATENESS          (GST_SECOND / 10)

#define DEFAULT_MOVIE_TIMESCALE         1000
#define DEFAULT_TRAK_TIMESCALE          0
#define DEFAULT_DO_CTTS                 TRUE
#define DEFAULT_FAST_START              FALSE
#define DEFAULT_FAST_START_TEMP_FILE    NULL
#define DEFAULT_MOOV_RECOV_FILE         NULL
#define DEFAULT_FRAGMENT_DURATION       0
#define DEFAULT_STREAMABLE              FALSE
#define DEFAULT_DTS_METHOD              DTS_METHOD_REORDER


static void gst_qt_mux_finalize (GObject * object);

static GstStateChangeReturn gst_qt_mux_change_state (GstElement * element,
    GstStateChange transition);

/* property functions */
static void gst_qt_mux_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_qt_mux_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

/* pad functions */
static GstPad *gst_qt_mux_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name);
static void gst_qt_mux_release_pad (GstElement * element, GstPad * pad);

/* event */
static gboolean gst_qt_mux_sink_event (GstPad * pad, GstEvent * event);

static GstFlowReturn gst_qt_mux_collected (GstCollectPads * pads,
    gpointer user_data);
static GstFlowReturn gst_qt_mux_add_buffer (GstQTMux * qtmux, GstQTPad * pad,
    GstBuffer * buf);

static GstElementClass *parent_class = NULL;

static void
gst_qt_mux_base_init (gpointer g_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (g_class);
  GstQTMuxClass *klass = (GstQTMuxClass *) g_class;
  GstQTMuxClassParams *params;
  GstPadTemplate *videosinktempl, *audiosinktempl, *srctempl;
  gchar *longname, *description;

  params =
      (GstQTMuxClassParams *) g_type_get_qdata (G_OBJECT_CLASS_TYPE (g_class),
      GST_QT_MUX_PARAMS_QDATA);
  g_assert (params != NULL);

  /* construct the element details struct */
  longname = g_strdup_printf ("%s Muxer", params->prop->long_name);
  description = g_strdup_printf ("Multiplex audio and video into a %s file%s",
      params->prop->long_name,
      (params->prop->rank == GST_RANK_NONE) ? " (deprecated)" : "");
  gst_element_class_set_details_simple (element_class, longname,
      "Codec/Muxer", description,
      "Thiago Sousa Santos <thiagoss@embedded.ufcg.edu.br>");
  g_free (longname);
  g_free (description);

  /* pad templates */
  srctempl = gst_pad_template_new ("src", GST_PAD_SRC,
      GST_PAD_ALWAYS, params->src_caps);
  gst_element_class_add_pad_template (element_class, srctempl);

  if (params->audio_sink_caps) {
    audiosinktempl = gst_pad_template_new ("audio_%d",
        GST_PAD_SINK, GST_PAD_REQUEST, params->audio_sink_caps);
    gst_element_class_add_pad_template (element_class, audiosinktempl);
  }

  if (params->video_sink_caps) {
    videosinktempl = gst_pad_template_new ("video_%d",
        GST_PAD_SINK, GST_PAD_REQUEST, params->video_sink_caps);
    gst_element_class_add_pad_template (element_class, videosinktempl);
  }

  klass->format = params->prop->format;
}

static void
gst_qt_mux_class_init (GstQTMuxClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->finalize = gst_qt_mux_finalize;
  gobject_class->get_property = gst_qt_mux_get_property;
  gobject_class->set_property = gst_qt_mux_set_property;

  g_object_class_install_property (gobject_class, PROP_MOVIE_TIMESCALE,
      g_param_spec_uint ("movie-timescale", "Movie timescale",
          "Timescale to use in the movie (units per second)",
          1, G_MAXUINT32, DEFAULT_MOVIE_TIMESCALE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_TRAK_TIMESCALE,
      g_param_spec_uint ("trak-timescale", "Track timescale",
          "Timescale to use for the tracks (units per second, 0 is automatic)",
          0, G_MAXUINT32, DEFAULT_TRAK_TIMESCALE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_DO_CTTS,
      g_param_spec_boolean ("presentation-time",
          "Include presentation-time info",
          "Calculate and include presentation/composition time "
          "(in addition to decoding time)", DEFAULT_DO_CTTS,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_DTS_METHOD,
      g_param_spec_enum ("dts-method", "dts-method",
          "Method to determine DTS time",
          GST_TYPE_QT_MUX_DTS_METHOD, DEFAULT_DTS_METHOD,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_FAST_START,
      g_param_spec_boolean ("faststart", "Format file to faststart",
          "If the file should be formatted for faststart (headers first)",
          DEFAULT_FAST_START, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_FAST_START_TEMP_FILE,
      g_param_spec_string ("faststart-file", "File to use for storing buffers",
          "File that will be used temporarily to store data from the stream "
          "when creating a faststart file. If null a filepath will be "
          "created automatically", DEFAULT_FAST_START_TEMP_FILE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MOOV_RECOV_FILE,
      g_param_spec_string ("moov-recovery-file",
          "File to store data for posterior moov atom recovery",
          "File to be used to store "
          "data for moov atom making movie file recovery possible in case "
          "of a crash during muxing. Null for disabled. (Experimental)",
          DEFAULT_MOOV_RECOV_FILE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_FRAGMENT_DURATION,
      g_param_spec_uint ("fragment-duration", "Fragment duration",
          "Fragment durations in ms (produce a fragmented file if > 0)",
          0, G_MAXUINT32, klass->format == GST_QT_MUX_FORMAT_ISML ?
          2000 : DEFAULT_FRAGMENT_DURATION,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_STREAMABLE,
      g_param_spec_boolean ("streamable", "Streamable",
          "If set to true, the output should be as if it is to be streamed "
          "and hence no indexes written or duration written.",
          DEFAULT_STREAMABLE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (gst_qt_mux_request_new_pad);
  gstelement_class->change_state = GST_DEBUG_FUNCPTR (gst_qt_mux_change_state);
  gstelement_class->release_pad = GST_DEBUG_FUNCPTR (gst_qt_mux_release_pad);
}

static void
gst_qt_mux_pad_reset (GstQTPad * qtpad)
{
  gint i;

  qtpad->fourcc = 0;
  qtpad->is_out_of_order = FALSE;
  qtpad->have_dts = FALSE;
  qtpad->sample_size = 0;
  qtpad->sync = FALSE;
  qtpad->last_dts = 0;
  qtpad->first_ts = GST_CLOCK_TIME_NONE;
  qtpad->prepare_buf_func = NULL;
  qtpad->avg_bitrate = 0;
  qtpad->max_bitrate = 0;
  qtpad->ts_n_entries = 0;

  qtpad->buf_head = 0;
  qtpad->buf_tail = 0;
  for (i = 0; i < G_N_ELEMENTS (qtpad->buf_entries); i++) {
    if (qtpad->buf_entries[i]) {
      gst_buffer_unref (qtpad->buf_entries[i]);
      qtpad->buf_entries[i] = NULL;
    }
  }

  if (qtpad->last_buf)
    gst_buffer_replace (&qtpad->last_buf, NULL);

  /* reference owned elsewhere */
  qtpad->trak = NULL;

  if (qtpad->traf) {
    atom_traf_free (qtpad->traf);
    qtpad->traf = NULL;
  }
  atom_array_clear (&qtpad->fragment_buffers);

  /* reference owned elsewhere */
  qtpad->tfra = NULL;
}

/*
 * Takes GstQTMux back to its initial state
 */
static void
gst_qt_mux_reset (GstQTMux * qtmux, gboolean alloc)
{
  GSList *walk;

  qtmux->state = GST_QT_MUX_STATE_NONE;
  qtmux->header_size = 0;
  qtmux->mdat_size = 0;
  qtmux->mdat_pos = 0;
  qtmux->longest_chunk = GST_CLOCK_TIME_NONE;
  qtmux->video_pads = 0;
  qtmux->audio_pads = 0;
  qtmux->fragment_sequence = 0;

  if (qtmux->ftyp) {
    atom_ftyp_free (qtmux->ftyp);
    qtmux->ftyp = NULL;
  }
  if (qtmux->moov) {
    atom_moov_free (qtmux->moov);
    qtmux->moov = NULL;
  }
  if (qtmux->mfra) {
    atom_mfra_free (qtmux->mfra);
    qtmux->mfra = NULL;
  }
  if (qtmux->fast_start_file) {
    fclose (qtmux->fast_start_file);
    g_remove (qtmux->fast_start_file_path);
    qtmux->fast_start_file = NULL;
  }
  if (qtmux->moov_recov_file) {
    fclose (qtmux->moov_recov_file);
    qtmux->moov_recov_file = NULL;
  }
  for (walk = qtmux->extra_atoms; walk; walk = g_slist_next (walk)) {
    AtomInfo *ainfo = (AtomInfo *) walk->data;
    ainfo->free_func (ainfo->atom);
    g_free (ainfo);
  }
  g_slist_free (qtmux->extra_atoms);
  qtmux->extra_atoms = NULL;

  GST_OBJECT_LOCK (qtmux);
  gst_tag_setter_reset_tags (GST_TAG_SETTER (qtmux));
  GST_OBJECT_UNLOCK (qtmux);

  /* reset pad data */
  for (walk = qtmux->sinkpads; walk; walk = g_slist_next (walk)) {
    GstQTPad *qtpad = (GstQTPad *) walk->data;
    gst_qt_mux_pad_reset (qtpad);

    /* hm, moov_free above yanked the traks away from us,
     * so do not free, but do clear */
    qtpad->trak = NULL;
  }

  if (alloc) {
    qtmux->moov = atom_moov_new (qtmux->context);
    /* ensure all is as nice and fresh as request_new_pad would provide it */
    for (walk = qtmux->sinkpads; walk; walk = g_slist_next (walk)) {
      GstQTPad *qtpad = (GstQTPad *) walk->data;

      qtpad->trak = atom_trak_new (qtmux->context);
      atom_moov_add_trak (qtmux->moov, qtpad->trak);
    }
  }
}

static void
gst_qt_mux_init (GstQTMux * qtmux, GstQTMuxClass * qtmux_klass)
{
  GstElementClass *klass = GST_ELEMENT_CLASS (qtmux_klass);
  GstPadTemplate *templ;

  templ = gst_element_class_get_pad_template (klass, "src");
  qtmux->srcpad = gst_pad_new_from_template (templ, "src");
  gst_pad_use_fixed_caps (qtmux->srcpad);
  gst_element_add_pad (GST_ELEMENT (qtmux), qtmux->srcpad);

  qtmux->sinkpads = NULL;
  qtmux->collect = gst_collect_pads_new ();
  gst_collect_pads_set_function (qtmux->collect,
      (GstCollectPadsFunction) GST_DEBUG_FUNCPTR (gst_qt_mux_collected), qtmux);

  /* properties set to default upon construction */

  /* always need this */
  qtmux->context =
      atoms_context_new (gst_qt_mux_map_format_to_flavor (qtmux_klass->format));

  /* internals to initial state */
  gst_qt_mux_reset (qtmux, TRUE);
}


static void
gst_qt_mux_finalize (GObject * object)
{
  GstQTMux *qtmux = GST_QT_MUX_CAST (object);

  gst_qt_mux_reset (qtmux, FALSE);

  g_free (qtmux->fast_start_file_path);
  g_free (qtmux->moov_recov_file_path);

  atoms_context_free (qtmux->context);
  gst_object_unref (qtmux->collect);

  g_slist_free (qtmux->sinkpads);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static GstBuffer *
gst_qt_mux_prepare_jpc_buffer (GstQTPad * qtpad, GstBuffer * buf,
    GstQTMux * qtmux)
{
  GstBuffer *newbuf;

  GST_LOG_OBJECT (qtmux, "Preparing jpc buffer");

  if (buf == NULL)
    return NULL;

  newbuf = gst_buffer_new_and_alloc (GST_BUFFER_SIZE (buf) + 8);
  gst_buffer_copy_metadata (newbuf, buf, GST_BUFFER_COPY_ALL);

  GST_WRITE_UINT32_BE (GST_BUFFER_DATA (newbuf), GST_BUFFER_SIZE (newbuf));
  GST_WRITE_UINT32_LE (GST_BUFFER_DATA (newbuf) + 4, FOURCC_jp2c);

  memcpy (GST_BUFFER_DATA (newbuf) + 8, GST_BUFFER_DATA (buf),
      GST_BUFFER_SIZE (buf));
  gst_buffer_unref (buf);

  return newbuf;
}

static void
gst_qt_mux_add_mp4_tag (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  switch (gst_tag_get_type (tag)) {
      /* strings */
    case G_TYPE_STRING:
    {
      gchar *str = NULL;

      if (!gst_tag_list_get_string (list, tag, &str) || !str)
        break;
      GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %s",
          GST_FOURCC_ARGS (fourcc), str);
      atom_moov_add_str_tag (qtmux->moov, fourcc, str);
      g_free (str);
      break;
    }
      /* double */
    case G_TYPE_DOUBLE:
    {
      gdouble value;

      if (!gst_tag_list_get_double (list, tag, &value))
        break;
      GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %u",
          GST_FOURCC_ARGS (fourcc), (gint) value);
      atom_moov_add_uint_tag (qtmux->moov, fourcc, 21, (gint) value);
      break;
    }
    case G_TYPE_UINT:
    {
      guint value = 0;
      if (tag2) {
        /* paired unsigned integers */
        guint count = 0;

        if (!(gst_tag_list_get_uint (list, tag, &value) ||
                gst_tag_list_get_uint (list, tag2, &count)))
          break;
        GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %u/%u",
            GST_FOURCC_ARGS (fourcc), value, count);
        atom_moov_add_uint_tag (qtmux->moov, fourcc, 0,
            value << 16 | (count & 0xFFFF));
      } else {
        /* unpaired unsigned integers */
        if (!gst_tag_list_get_uint (list, tag, &value))
          break;
        GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %u",
            GST_FOURCC_ARGS (fourcc), value);
        atom_moov_add_uint_tag (qtmux->moov, fourcc, 1, value);
      }
      break;
    }
    default:
      g_assert_not_reached ();
      break;
  }
}

static void
gst_qt_mux_add_mp4_date (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  GDate *date = NULL;
  GDateYear year;
  GDateMonth month;
  GDateDay day;
  gchar *str;

  g_return_if_fail (gst_tag_get_type (tag) == GST_TYPE_DATE);

  if (!gst_tag_list_get_date (list, tag, &date) || !date)
    return;

  year = g_date_get_year (date);
  month = g_date_get_month (date);
  day = g_date_get_day (date);

  if (year == G_DATE_BAD_YEAR && month == G_DATE_BAD_MONTH &&
      day == G_DATE_BAD_DAY) {
    GST_WARNING_OBJECT (qtmux, "invalid date in tag");
    return;
  }

  str = g_strdup_printf ("%u-%u-%u", year, month, day);
  GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %s",
      GST_FOURCC_ARGS (fourcc), str);
  atom_moov_add_str_tag (qtmux->moov, fourcc, str);
  g_free (str);
}

static void
gst_qt_mux_add_mp4_cover (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  GValue value = { 0, };
  GstBuffer *buf;
  GstCaps *caps;
  GstStructure *structure;
  gint flags = 0;

  g_return_if_fail (gst_tag_get_type (tag) == GST_TYPE_BUFFER);

  if (!gst_tag_list_copy_value (&value, list, tag))
    return;

  buf = gst_value_get_buffer (&value);
  if (!buf)
    goto done;

  caps = gst_buffer_get_caps (buf);
  if (!caps) {
    GST_WARNING_OBJECT (qtmux, "preview image without caps");
    goto done;
  }

  GST_DEBUG_OBJECT (qtmux, "preview image caps %" GST_PTR_FORMAT, caps);

  structure = gst_caps_get_structure (caps, 0);
  if (gst_structure_has_name (structure, "image/jpeg"))
    flags = 13;
  else if (gst_structure_has_name (structure, "image/png"))
    flags = 14;
  gst_caps_unref (caps);

  if (!flags) {
    GST_WARNING_OBJECT (qtmux, "preview image format not supported");
    goto done;
  }

  GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT
      " -> image size %d", GST_FOURCC_ARGS (fourcc), GST_BUFFER_SIZE (buf));
  atom_moov_add_tag (qtmux->moov, fourcc, flags, GST_BUFFER_DATA (buf),
      GST_BUFFER_SIZE (buf));
done:
  g_value_unset (&value);
}

static void
gst_qt_mux_add_3gp_str (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  gchar *str = NULL;
  guint number;

  g_return_if_fail (gst_tag_get_type (tag) == G_TYPE_STRING);
  g_return_if_fail (!tag2 || gst_tag_get_type (tag2) == G_TYPE_UINT);

  if (!gst_tag_list_get_string (list, tag, &str) || !str)
    return;

  if (tag2)
    if (!gst_tag_list_get_uint (list, tag2, &number))
      tag2 = NULL;

  if (!tag2) {
    GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %s",
        GST_FOURCC_ARGS (fourcc), str);
    atom_moov_add_3gp_str_tag (qtmux->moov, fourcc, str);
  } else {
    GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %s/%d",
        GST_FOURCC_ARGS (fourcc), str, number);
    atom_moov_add_3gp_str_int_tag (qtmux->moov, fourcc, str, number);
  }

  g_free (str);
}

static void
gst_qt_mux_add_3gp_date (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  GDate *date = NULL;
  GDateYear year;

  g_return_if_fail (gst_tag_get_type (tag) == GST_TYPE_DATE);

  if (!gst_tag_list_get_date (list, tag, &date) || !date)
    return;

  year = g_date_get_year (date);

  if (year == G_DATE_BAD_YEAR) {
    GST_WARNING_OBJECT (qtmux, "invalid date in tag");
    return;
  }

  GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %d",
      GST_FOURCC_ARGS (fourcc), year);
  atom_moov_add_3gp_uint_tag (qtmux->moov, fourcc, year);
}

static void
gst_qt_mux_add_3gp_location (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  gdouble latitude = -360, longitude = -360, altitude = 0;
  gchar *location = NULL;
  guint8 *data, *ddata;
  gint size = 0, len = 0;
  gboolean ret = FALSE;

  g_return_if_fail (strcmp (tag, GST_TAG_GEO_LOCATION_NAME) == 0);

  ret = gst_tag_list_get_string (list, tag, &location);
  ret |= gst_tag_list_get_double (list, GST_TAG_GEO_LOCATION_LONGITUDE,
      &longitude);
  ret |= gst_tag_list_get_double (list, GST_TAG_GEO_LOCATION_LATITUDE,
      &latitude);
  ret |= gst_tag_list_get_double (list, GST_TAG_GEO_LOCATION_ELEVATION,
      &altitude);

  if (!ret)
    return;

  if (location)
    len = strlen (location);
  size += len + 1 + 2;

  /* role + (long, lat, alt) + body + notes */
  size += 1 + 3 * 4 + 1 + 1;

  data = ddata = g_malloc (size);

  /* language tag */
  GST_WRITE_UINT16_BE (data, language_code (GST_QT_MUX_DEFAULT_TAG_LANGUAGE));
  /* location */
  if (location)
    memcpy (data + 2, location, len);
  GST_WRITE_UINT8 (data + 2 + len, 0);
  data += len + 1 + 2;
  /* role */
  GST_WRITE_UINT8 (data, 0);
  /* long, lat, alt */
#define QT_WRITE_SFP32(data, fp) GST_WRITE_UINT32_BE(data, (guint32) ((gint) (fp * 65536.0)))
  QT_WRITE_SFP32 (data + 1, longitude);
  QT_WRITE_SFP32 (data + 5, latitude);
  QT_WRITE_SFP32 (data + 9, altitude);
  /* neither astronomical body nor notes */
  GST_WRITE_UINT16_BE (data + 13, 0);

  GST_DEBUG_OBJECT (qtmux, "Adding tag 'loci'");
  atom_moov_add_3gp_tag (qtmux->moov, fourcc, ddata, size);
  g_free (ddata);
}

static void
gst_qt_mux_add_3gp_keywords (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  gchar *keywords = NULL;
  guint8 *data, *ddata;
  gint size = 0, i;
  gchar **kwds;

  g_return_if_fail (strcmp (tag, GST_TAG_KEYWORDS) == 0);

  if (!gst_tag_list_get_string (list, tag, &keywords) || !keywords)
    return;

  kwds = g_strsplit (keywords, ",", 0);
  g_free (keywords);

  size = 0;
  for (i = 0; kwds[i]; i++) {
    /* size byte + null-terminator */
    size += strlen (kwds[i]) + 1 + 1;
  }

  /* language tag + count + keywords */
  size += 2 + 1;

  data = ddata = g_malloc (size);

  /* language tag */
  GST_WRITE_UINT16_BE (data, language_code (GST_QT_MUX_DEFAULT_TAG_LANGUAGE));
  /* count */
  GST_WRITE_UINT8 (data + 2, i);
  data += 3;
  /* keywords */
  for (i = 0; kwds[i]; ++i) {
    gint len = strlen (kwds[i]);

    GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %s",
        GST_FOURCC_ARGS (fourcc), kwds[i]);
    /* size */
    GST_WRITE_UINT8 (data, len + 1);
    memcpy (data + 1, kwds[i], len + 1);
    data += len + 2;
  }

  g_strfreev (kwds);

  atom_moov_add_3gp_tag (qtmux->moov, fourcc, ddata, size);
  g_free (ddata);
}

static gboolean
gst_qt_mux_parse_classification_string (GstQTMux * qtmux, const gchar * input,
    guint32 * p_fourcc, guint16 * p_table, gchar ** p_content)
{
  guint32 fourcc;
  gint table;
  gint size;
  const gchar *data;

  data = input;
  size = strlen (input);

  if (size < 4 + 3 + 1 + 1 + 1) {
    /* at least the minimum xxxx://y/z */
    GST_WARNING_OBJECT (qtmux, "Classification tag input (%s) too short, "
        "ignoring", input);
    return FALSE;
  }

  /* read the fourcc */
  memcpy (&fourcc, data, 4);
  size -= 4;
  data += 4;

  if (strncmp (data, "://", 3) != 0) {
    goto mismatch;
  }
  data += 3;
  size -= 3;

  /* read the table number */
  if (sscanf (data, "%d", &table) != 1) {
    goto mismatch;
  }
  if (table < 0) {
    GST_WARNING_OBJECT (qtmux, "Invalid table number in classification tag (%d)"
        ", table numbers should be positive, ignoring tag", table);
    return FALSE;
  }

  /* find the next / */
  while (size > 0 && data[0] != '/') {
    data += 1;
    size -= 1;
  }
  if (size == 0) {
    goto mismatch;
  }
  g_assert (data[0] == '/');

  /* skip the '/' */
  data += 1;
  size -= 1;
  if (size == 0) {
    goto mismatch;
  }

  /* read up the rest of the string */
  *p_content = g_strdup (data);
  *p_table = (guint16) table;
  *p_fourcc = fourcc;
  return TRUE;

mismatch:
  {
    GST_WARNING_OBJECT (qtmux, "Ignoring classification tag as "
        "input (%s) didn't match the expected entitycode://table/content",
        input);
    return FALSE;
  }
}

static void
gst_qt_mux_add_3gp_classification (GstQTMux * qtmux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc)
{
  gchar *clsf_data = NULL;
  gint size = 0;
  guint32 entity = 0;
  guint16 table = 0;
  gchar *content = NULL;
  guint8 *data;

  g_return_if_fail (strcmp (tag, GST_TAG_3GP_CLASSIFICATION) == 0);

  if (!gst_tag_list_get_string (list, tag, &clsf_data) || !clsf_data)
    return;

  GST_DEBUG_OBJECT (qtmux, "Adding tag %" GST_FOURCC_FORMAT " -> %s",
      GST_FOURCC_ARGS (fourcc), clsf_data);

  /* parse the string, format is:
   * entityfourcc://table/content
   */
  gst_qt_mux_parse_classification_string (qtmux, clsf_data, &entity, &table,
      &content);
  g_free (clsf_data);
  /* +1 for the \0 */
  size = strlen (content) + 1;

  /* now we have everything, build the atom
   * atom description is at 3GPP TS 26.244 V8.2.0 (2009-09) */
  data = g_malloc (4 + 2 + 2 + size);
  GST_WRITE_UINT32_LE (data, entity);
  GST_WRITE_UINT16_BE (data + 4, (guint16) table);
  GST_WRITE_UINT16_BE (data + 6, 0);
  memcpy (data + 8, content, size);
  g_free (content);

  atom_moov_add_3gp_tag (qtmux->moov, fourcc, data, 4 + 2 + 2 + size);
  g_free (data);
}

typedef void (*GstQTMuxAddTagFunc) (GstQTMux * mux, const GstTagList * list,
    const char *tag, const char *tag2, guint32 fourcc);

/*
 * Struct to record mappings from gstreamer tags to fourcc codes
 */
typedef struct _GstTagToFourcc
{
  guint32 fourcc;
  const gchar *gsttag;
  const gchar *gsttag2;
  const GstQTMuxAddTagFunc func;
} GstTagToFourcc;

/* tag list tags to fourcc matching */
static const GstTagToFourcc tag_matches_mp4[] = {
  {FOURCC__alb, GST_TAG_ALBUM, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_soal, GST_TAG_ALBUM_SORTNAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__ART, GST_TAG_ARTIST, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_soar, GST_TAG_ARTIST_SORTNAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_aART, GST_TAG_ALBUM_ARTIST, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_soaa, GST_TAG_ALBUM_ARTIST_SORTNAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__cmt, GST_TAG_COMMENT, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__wrt, GST_TAG_COMPOSER, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_soco, GST_TAG_COMPOSER_SORTNAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_tvsh, GST_TAG_SHOW_NAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_sosn, GST_TAG_SHOW_SORTNAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_tvsn, GST_TAG_SHOW_SEASON_NUMBER, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_tves, GST_TAG_SHOW_EPISODE_NUMBER, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__gen, GST_TAG_GENRE, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__nam, GST_TAG_TITLE, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_sonm, GST_TAG_TITLE_SORTNAME, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_perf, GST_TAG_PERFORMER, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__grp, GST_TAG_GROUPING, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__des, GST_TAG_DESCRIPTION, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__lyr, GST_TAG_LYRICS, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__too, GST_TAG_ENCODER, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_cprt, GST_TAG_COPYRIGHT, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_keyw, GST_TAG_KEYWORDS, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC__day, GST_TAG_DATE, NULL, gst_qt_mux_add_mp4_date},
  {FOURCC_tmpo, GST_TAG_BEATS_PER_MINUTE, NULL, gst_qt_mux_add_mp4_tag},
  {FOURCC_trkn, GST_TAG_TRACK_NUMBER, GST_TAG_TRACK_COUNT,
      gst_qt_mux_add_mp4_tag},
  {FOURCC_disk, GST_TAG_ALBUM_VOLUME_NUMBER, GST_TAG_ALBUM_VOLUME_COUNT,
      gst_qt_mux_add_mp4_tag},
  {FOURCC_covr, GST_TAG_PREVIEW_IMAGE, NULL, gst_qt_mux_add_mp4_cover},
  {0, NULL,}
};

static const GstTagToFourcc tag_matches_3gp[] = {
  {FOURCC_titl, GST_TAG_TITLE, NULL, gst_qt_mux_add_3gp_str},
  {FOURCC_dscp, GST_TAG_DESCRIPTION, NULL, gst_qt_mux_add_3gp_str},
  {FOURCC_cprt, GST_TAG_COPYRIGHT, NULL, gst_qt_mux_add_3gp_str},
  {FOURCC_perf, GST_TAG_ARTIST, NULL, gst_qt_mux_add_3gp_str},
  {FOURCC_auth, GST_TAG_COMPOSER, NULL, gst_qt_mux_add_3gp_str},
  {FOURCC_gnre, GST_TAG_GENRE, NULL, gst_qt_mux_add_3gp_str},
  {FOURCC_kywd, GST_TAG_KEYWORDS, NULL, gst_qt_mux_add_3gp_keywords},
  {FOURCC_yrrc, GST_TAG_DATE, NULL, gst_qt_mux_add_3gp_date},
  {FOURCC_albm, GST_TAG_ALBUM, GST_TAG_TRACK_NUMBER, gst_qt_mux_add_3gp_str},
  {FOURCC_loci, GST_TAG_GEO_LOCATION_NAME, NULL, gst_qt_mux_add_3gp_location},
  {FOURCC_clsf, GST_TAG_3GP_CLASSIFICATION, NULL,
      gst_qt_mux_add_3gp_classification},
  {0, NULL,}
};

/* qtdemux produces these for atoms it cannot parse */
#define GST_QT_DEMUX_PRIVATE_TAG "private-qt-tag"

static void
gst_qt_mux_add_xmp_tags (GstQTMux * qtmux, const GstTagList * list)
{
  GstQTMuxClass *qtmux_klass = (GstQTMuxClass *) (G_OBJECT_GET_CLASS (qtmux));
  GstBuffer *xmp = NULL;

  /* adobe specs only have 'quicktime' and 'mp4',
   * but I guess we can extrapolate to gpp.
   * Keep mj2 out for now as we don't add any tags for it yet.
   * If you have further info about xmp on these formats, please share */
  if (qtmux_klass->format == GST_QT_MUX_FORMAT_MJ2)
    return;

  GST_DEBUG_OBJECT (qtmux, "Adding xmp tags");

  if (qtmux_klass->format == GST_QT_MUX_FORMAT_QT) {
    xmp = gst_tag_xmp_writer_tag_list_to_xmp_buffer (GST_TAG_XMP_WRITER (qtmux),
        list, TRUE);
    if (xmp)
      atom_moov_add_xmp_tags (qtmux->moov, xmp);
  } else {
    AtomInfo *ainfo;
    /* for isom/mp4, it is a top level uuid atom */
    xmp = gst_tag_xmp_writer_tag_list_to_xmp_buffer (GST_TAG_XMP_WRITER (qtmux),
        list, TRUE);
    if (xmp) {
      ainfo = build_uuid_xmp_atom (xmp);
      if (ainfo) {
        qtmux->extra_atoms = g_slist_prepend (qtmux->extra_atoms, ainfo);
      }
    }
  }
  if (xmp)
    gst_buffer_unref (xmp);
}

static void
gst_qt_mux_add_metadata_tags (GstQTMux * qtmux, const GstTagList * list)
{
  GstQTMuxClass *qtmux_klass = (GstQTMuxClass *) (G_OBJECT_GET_CLASS (qtmux));
  guint32 fourcc;
  gint i;
  const gchar *tag, *tag2;
  const GstTagToFourcc *tag_matches;

  switch (qtmux_klass->format) {
    case GST_QT_MUX_FORMAT_3GP:
      tag_matches = tag_matches_3gp;
      break;
    case GST_QT_MUX_FORMAT_MJ2:
      tag_matches = NULL;
      break;
    default:
      /* sort of iTunes style for mp4 and QT (?) */
      tag_matches = tag_matches_mp4;
      break;
  }

  if (!tag_matches)
    return;

  for (i = 0; tag_matches[i].fourcc; i++) {
    fourcc = tag_matches[i].fourcc;
    tag = tag_matches[i].gsttag;
    tag2 = tag_matches[i].gsttag2;

    g_assert (tag_matches[i].func);
    tag_matches[i].func (qtmux, list, tag, tag2, fourcc);
  }

  /* add unparsed blobs if present */
  if (gst_tag_exists (GST_QT_DEMUX_PRIVATE_TAG)) {
    guint num_tags;

    num_tags = gst_tag_list_get_tag_size (list, GST_QT_DEMUX_PRIVATE_TAG);
    for (i = 0; i < num_tags; ++i) {
      const GValue *val;
      GstBuffer *buf;
      GstCaps *caps = NULL;

      val = gst_tag_list_get_value_index (list, GST_QT_DEMUX_PRIVATE_TAG, i);
      buf = (GstBuffer *) gst_value_get_mini_object (val);

      if (buf && (caps = gst_buffer_get_caps (buf))) {
        GstStructure *s;
        const gchar *style = NULL;

        GST_DEBUG_OBJECT (qtmux, "Found private tag %d/%d; size %d, caps %"
            GST_PTR_FORMAT, i, num_tags, GST_BUFFER_SIZE (buf), caps);
        s = gst_caps_get_structure (caps, 0);
        if (s && (style = gst_structure_get_string (s, "style"))) {
          /* try to prevent some style tag ending up into another variant
           * (todo: make into a list if more cases) */
          if ((strcmp (style, "itunes") == 0 &&
                  qtmux_klass->format == GST_QT_MUX_FORMAT_MP4) ||
              (strcmp (style, "iso") == 0 &&
                  qtmux_klass->format == GST_QT_MUX_FORMAT_3GP)) {
            GST_DEBUG_OBJECT (qtmux, "Adding private tag");
            atom_moov_add_blob_tag (qtmux->moov, GST_BUFFER_DATA (buf),
                GST_BUFFER_SIZE (buf));
          }
        }
        gst_caps_unref (caps);
      }
    }
  }

  return;
}

/*
 * Gets the tagsetter iface taglist and puts the known tags
 * into the output stream
 */
static void
gst_qt_mux_setup_metadata (GstQTMux * qtmux)
{
  const GstTagList *tags;

  GST_OBJECT_LOCK (qtmux);
  tags = gst_tag_setter_get_tag_list (GST_TAG_SETTER (qtmux));
  GST_OBJECT_UNLOCK (qtmux);

  GST_LOG_OBJECT (qtmux, "tags: %" GST_PTR_FORMAT, tags);

  if (tags && !gst_tag_list_is_empty (tags)) {
    GstTagList *copy = gst_tag_list_copy (tags);

    GST_DEBUG_OBJECT (qtmux, "Removing bogus tags");
    gst_tag_list_remove_tag (copy, GST_TAG_VIDEO_CODEC);
    gst_tag_list_remove_tag (copy, GST_TAG_AUDIO_CODEC);
    gst_tag_list_remove_tag (copy, GST_TAG_CONTAINER_FORMAT);

    GST_DEBUG_OBJECT (qtmux, "Formatting tags");
    gst_qt_mux_add_metadata_tags (qtmux, copy);
    gst_qt_mux_add_xmp_tags (qtmux, copy);
    gst_tag_list_free (copy);
  } else {
    GST_DEBUG_OBJECT (qtmux, "No tags received");
  }
}

static inline GstBuffer *
_gst_buffer_new_take_data (guint8 * data, guint size)
{
  GstBuffer *buf;

  buf = gst_buffer_new ();
  GST_BUFFER_DATA (buf) = GST_BUFFER_MALLOCDATA (buf) = data;
  GST_BUFFER_SIZE (buf) = size;

  return buf;
}

static GstFlowReturn
gst_qt_mux_send_buffer (GstQTMux * qtmux, GstBuffer * buf, guint64 * offset,
    gboolean mind_fast)
{
  GstFlowReturn res;
  guint8 *data;
  guint size;

  g_return_val_if_fail (buf != NULL, GST_FLOW_ERROR);

  data = GST_BUFFER_DATA (buf);
  size = GST_BUFFER_SIZE (buf);

  GST_LOG_OBJECT (qtmux, "sending buffer size %d", size);

  if (mind_fast && qtmux->fast_start_file) {
    gint ret;

    GST_LOG_OBJECT (qtmux, "to temporary file");
    ret = fwrite (data, sizeof (guint8), size, qtmux->fast_start_file);
    gst_buffer_unref (buf);
    if (ret != size)
      goto write_error;
    else
      res = GST_FLOW_OK;
  } else {
    GST_LOG_OBJECT (qtmux, "downstream");

    buf = gst_buffer_make_metadata_writable (buf);
    gst_buffer_set_caps (buf, GST_PAD_CAPS (qtmux->srcpad));
    res = gst_pad_push (qtmux->srcpad, buf);
  }

  if (G_LIKELY (offset))
    *offset += size;

  return res;

  /* ERRORS */
write_error:
  {
    GST_ELEMENT_ERROR (qtmux, RESOURCE, WRITE,
        ("Failed to write to temporary file"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }
}

static gboolean
gst_qt_mux_seek_to_beginning (FILE * f)
{
#ifdef HAVE_FSEEKO
  if (fseeko (f, (off_t) 0, SEEK_SET) != 0)
    return FALSE;
#elif defined (G_OS_UNIX) || defined (G_OS_WIN32)
  if (lseek (fileno (f), (off_t) 0, SEEK_SET) == (off_t) - 1)
    return FALSE;
#else
  if (fseek (f, (long) 0, SEEK_SET) != 0)
    return FALSE;
#endif
  return TRUE;
}

static GstFlowReturn
gst_qt_mux_send_buffered_data (GstQTMux * qtmux, guint64 * offset)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstBuffer *buf = NULL;

  if (fflush (qtmux->fast_start_file))
    goto flush_failed;

  if (!gst_qt_mux_seek_to_beginning (qtmux->fast_start_file))
    goto seek_failed;

  /* hm, this could all take a really really long time,
   * but there may not be another way to get moov atom first
   * (somehow optimize copy?) */
  GST_DEBUG_OBJECT (qtmux, "Sending buffered data");
  while (ret == GST_FLOW_OK) {
    gint r;
    const int bufsize = 4096;

    buf = gst_buffer_new_and_alloc (bufsize);
    r = fread (GST_BUFFER_DATA (buf), sizeof (guint8), bufsize,
        qtmux->fast_start_file);
    if (r == 0)
      break;
    GST_BUFFER_SIZE (buf) = r;
    GST_LOG_OBJECT (qtmux, "Pushing buffered buffer of size %d", r);
    ret = gst_qt_mux_send_buffer (qtmux, buf, offset, FALSE);
    buf = NULL;
  }
  if (buf)
    gst_buffer_unref (buf);

  if (ftruncate (fileno (qtmux->fast_start_file), 0))
    goto seek_failed;
  if (!gst_qt_mux_seek_to_beginning (qtmux->fast_start_file))
    goto seek_failed;

  return ret;

  /* ERRORS */
flush_failed:
  {
    GST_ELEMENT_ERROR (qtmux, RESOURCE, WRITE,
        ("Failed to flush temporary file"), GST_ERROR_SYSTEM);
    ret = GST_FLOW_ERROR;
    goto fail;
  }
seek_failed:
  {
    GST_ELEMENT_ERROR (qtmux, RESOURCE, SEEK,
        ("Failed to seek temporary file"), GST_ERROR_SYSTEM);
    ret = GST_FLOW_ERROR;
    goto fail;
  }
fail:
  {
    /* clear descriptor so we don't remove temp file later on,
     * might be possible to recover */
    fclose (qtmux->fast_start_file);
    qtmux->fast_start_file = NULL;
    return ret;
  }
}

/*
 * Sends the initial mdat atom fields (size fields and fourcc type),
 * the subsequent buffers are considered part of it's data.
 * As we can't predict the amount of data that we are going to place in mdat
 * we need to record the position of the size field in the stream so we can
 * seek back to it later and update when the streams have finished.
 */
static GstFlowReturn
gst_qt_mux_send_mdat_header (GstQTMux * qtmux, guint64 * off, guint64 size,
    gboolean extended)
{
  Atom *node_header;
  GstBuffer *buf;
  guint8 *data = NULL;
  guint64 offset = 0;

  GST_DEBUG_OBJECT (qtmux, "Sending mdat's atom header, "
      "size %" G_GUINT64_FORMAT, size);

  node_header = g_malloc0 (sizeof (Atom));
  node_header->type = FOURCC_mdat;
  if (extended) {
    /* use extended size */
    node_header->size = 1;
    node_header->extended_size = 0;
    if (size)
      node_header->extended_size = size + 16;
  } else {
    node_header->size = size + 8;
  }

  size = offset = 0;
  if (atom_copy_data (node_header, &data, &size, &offset) == 0)
    goto serialize_error;

  buf = _gst_buffer_new_take_data (data, offset);
  g_free (node_header);

  GST_LOG_OBJECT (qtmux, "Pushing mdat start");
  return gst_qt_mux_send_buffer (qtmux, buf, off, FALSE);

  /* ERRORS */
serialize_error:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("Failed to serialize mdat"));
    return GST_FLOW_ERROR;
  }
}

/*
 * We get the position of the mdat size field, seek back to it
 * and overwrite with the real value
 */
static GstFlowReturn
gst_qt_mux_update_mdat_size (GstQTMux * qtmux, guint64 mdat_pos,
    guint64 mdat_size, guint64 * offset)
{
  GstEvent *event;
  GstBuffer *buf;
  gboolean large_file;

  large_file = (mdat_size > MDAT_LARGE_FILE_LIMIT);

  if (large_file)
    mdat_pos += 8;

  /* seek and rewrite the header */
  event = gst_event_new_new_segment (FALSE, 1.0, GST_FORMAT_BYTES,
      mdat_pos, GST_CLOCK_TIME_NONE, 0);
  gst_pad_push_event (qtmux->srcpad, event);

  if (large_file) {
    buf = gst_buffer_new_and_alloc (sizeof (guint64));
    GST_WRITE_UINT64_BE (GST_BUFFER_DATA (buf), mdat_size + 16);
  } else {
    guint8 *data;

    buf = gst_buffer_new_and_alloc (16);
    data = GST_BUFFER_DATA (buf);
    GST_WRITE_UINT32_BE (data, 8);
    GST_WRITE_UINT32_LE (data + 4, FOURCC_free);
    GST_WRITE_UINT32_BE (data + 8, mdat_size + 8);
    GST_WRITE_UINT32_LE (data + 12, FOURCC_mdat);
  }

  return gst_qt_mux_send_buffer (qtmux, buf, offset, FALSE);
}

static GstFlowReturn
gst_qt_mux_send_ftyp (GstQTMux * qtmux, guint64 * off)
{
  GstBuffer *buf;
  guint64 size = 0, offset = 0;
  guint8 *data = NULL;

  GST_DEBUG_OBJECT (qtmux, "Sending ftyp atom");

  if (!atom_ftyp_copy_data (qtmux->ftyp, &data, &size, &offset))
    goto serialize_error;

  buf = _gst_buffer_new_take_data (data, offset);

  GST_LOG_OBJECT (qtmux, "Pushing ftyp");
  return gst_qt_mux_send_buffer (qtmux, buf, off, FALSE);

  /* ERRORS */
serialize_error:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("Failed to serialize ftyp"));
    return GST_FLOW_ERROR;
  }
}

static void
gst_qt_mux_prepare_ftyp (GstQTMux * qtmux, AtomFTYP ** p_ftyp,
    GstBuffer ** p_prefix)
{
  GstQTMuxClass *qtmux_klass = (GstQTMuxClass *) (G_OBJECT_GET_CLASS (qtmux));
  guint32 major, version;
  GList *comp;
  GstBuffer *prefix = NULL;
  AtomFTYP *ftyp = NULL;

  GST_DEBUG_OBJECT (qtmux, "Preparing ftyp and possible prefix atom");

  /* init and send context and ftyp based on current property state */
  gst_qt_mux_map_format_to_header (qtmux_klass->format, &prefix, &major,
      &version, &comp, qtmux->moov, qtmux->longest_chunk,
      qtmux->fast_start_file != NULL);
  ftyp = atom_ftyp_new (qtmux->context, major, version, comp);
  if (comp)
    g_list_free (comp);
  if (prefix) {
    if (p_prefix)
      *p_prefix = prefix;
    else
      gst_buffer_unref (prefix);
  }
  *p_ftyp = ftyp;
}

static GstFlowReturn
gst_qt_mux_prepare_and_send_ftyp (GstQTMux * qtmux)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstBuffer *prefix = NULL;

  GST_DEBUG_OBJECT (qtmux, "Preparing to send ftyp atom");

  /* init and send context and ftyp based on current property state */
  if (qtmux->ftyp) {
    atom_ftyp_free (qtmux->ftyp);
    qtmux->ftyp = NULL;
  }
  gst_qt_mux_prepare_ftyp (qtmux, &qtmux->ftyp, &prefix);
  if (prefix) {
    ret = gst_qt_mux_send_buffer (qtmux, prefix, &qtmux->header_size, FALSE);
    if (ret != GST_FLOW_OK)
      return ret;
  }
  return gst_qt_mux_send_ftyp (qtmux, &qtmux->header_size);
}

static void
gst_qt_mux_set_header_on_caps (GstQTMux * mux, GstBuffer * buf)
{
  GstStructure *structure;
  GValue array = { 0 };
  GValue value = { 0 };
  GstCaps *caps = GST_PAD_CAPS (mux->srcpad);

  caps = gst_caps_copy (GST_PAD_CAPS (mux->srcpad));
  structure = gst_caps_get_structure (caps, 0);

  g_value_init (&array, GST_TYPE_ARRAY);

  GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_IN_CAPS);
  g_value_init (&value, GST_TYPE_BUFFER);
  gst_value_take_buffer (&value, gst_buffer_ref (buf));
  gst_value_array_append_value (&array, &value);
  g_value_unset (&value);

  gst_structure_set_value (structure, "streamheader", &array);
  g_value_unset (&array);
  gst_pad_set_caps (mux->srcpad, caps);
  gst_caps_unref (caps);
}

static void
gst_qt_mux_configure_moov (GstQTMux * qtmux, guint32 * _timescale)
{
  gboolean fragmented;
  guint32 timescale;

  GST_OBJECT_LOCK (qtmux);
  timescale = qtmux->timescale;
  fragmented = qtmux->fragment_sequence > 0;
  GST_OBJECT_UNLOCK (qtmux);

  /* inform lower layers of our property wishes, and determine duration.
   * Let moov take care of this using its list of traks;
   * so that released pads are also included */
  GST_DEBUG_OBJECT (qtmux, "Updating timescale to %" G_GUINT32_FORMAT,
      timescale);
  atom_moov_update_timescale (qtmux->moov, timescale);
  atom_moov_set_fragmented (qtmux->moov, fragmented);

  atom_moov_update_duration (qtmux->moov);

  if (_timescale)
    *_timescale = timescale;
}

static GstFlowReturn
gst_qt_mux_send_moov (GstQTMux * qtmux, guint64 * _offset, gboolean mind_fast)
{
  guint64 offset = 0, size = 0;
  guint8 *data;
  GstBuffer *buf;
  GstFlowReturn ret = GST_FLOW_OK;

  /* serialize moov */
  offset = size = 0;
  data = NULL;
  GST_LOG_OBJECT (qtmux, "Copying movie header into buffer");
  if (!atom_moov_copy_data (qtmux->moov, &data, &size, &offset))
    goto serialize_error;

  buf = _gst_buffer_new_take_data (data, offset);
  GST_DEBUG_OBJECT (qtmux, "Pushing moov atoms");
  gst_qt_mux_set_header_on_caps (qtmux, buf);
  ret = gst_qt_mux_send_buffer (qtmux, buf, _offset, mind_fast);

  return ret;

serialize_error:
  {
    g_free (data);
    return GST_FLOW_ERROR;
  }
}

/* either calculates size of extra atoms or pushes them */
static GstFlowReturn
gst_qt_mux_send_extra_atoms (GstQTMux * qtmux, gboolean send, guint64 * offset,
    gboolean mind_fast)
{
  GSList *walk;
  guint64 loffset = 0, size = 0;
  guint8 *data;
  GstFlowReturn ret = GST_FLOW_OK;

  for (walk = qtmux->extra_atoms; walk; walk = g_slist_next (walk)) {
    AtomInfo *ainfo = (AtomInfo *) walk->data;

    loffset = size = 0;
    data = NULL;
    if (!ainfo->copy_data_func (ainfo->atom,
            send ? &data : NULL, &size, &loffset))
      goto serialize_error;

    if (send) {
      GstBuffer *buf;

      GST_DEBUG_OBJECT (qtmux,
          "Pushing extra top-level atom %" GST_FOURCC_FORMAT,
          GST_FOURCC_ARGS (ainfo->atom->type));
      buf = _gst_buffer_new_take_data (data, loffset);
      ret = gst_qt_mux_send_buffer (qtmux, buf, offset, FALSE);
      if (ret != GST_FLOW_OK)
        break;
    } else {
      if (offset)
        *offset += loffset;
    }
  }

  return ret;

serialize_error:
  {
    g_free (data);
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_qt_mux_start_file (GstQTMux * qtmux)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstCaps *caps;

  GST_DEBUG_OBJECT (qtmux, "starting file");

  caps = gst_caps_copy (gst_pad_get_pad_template_caps (qtmux->srcpad));
  /* qtmux has structure with and without variant, remove all but the first */
  while (gst_caps_get_size (caps) > 1)
    gst_caps_remove_structure (caps, 1);
  gst_pad_set_caps (qtmux->srcpad, caps);
  gst_caps_unref (caps);

  /* let downstream know we think in BYTES and expect to do seeking later on */
  gst_pad_push_event (qtmux->srcpad,
      gst_event_new_new_segment (FALSE, 1.0, GST_FORMAT_BYTES, 0, -1, 0));

  /* initialize our moov recovery file */
  GST_OBJECT_LOCK (qtmux);
  if (qtmux->moov_recov_file_path) {
    GST_DEBUG_OBJECT (qtmux, "Openning moov recovery file: %s",
        qtmux->moov_recov_file_path);
    qtmux->moov_recov_file = g_fopen (qtmux->moov_recov_file_path, "wb+");
    if (qtmux->moov_recov_file == NULL) {
      GST_WARNING_OBJECT (qtmux, "Failed to open moov recovery file in %s",
          qtmux->moov_recov_file_path);
    } else {
      GSList *walk;
      gboolean fail = FALSE;
      AtomFTYP *ftyp = NULL;
      GstBuffer *prefix = NULL;

      gst_qt_mux_prepare_ftyp (qtmux, &ftyp, &prefix);

      if (!atoms_recov_write_headers (qtmux->moov_recov_file, ftyp, prefix,
              qtmux->moov, qtmux->timescale,
              g_slist_length (qtmux->sinkpads))) {
        GST_WARNING_OBJECT (qtmux, "Failed to write moov recovery file "
            "headers");
        fail = TRUE;
      }

      atom_ftyp_free (ftyp);
      if (prefix)
        gst_buffer_unref (prefix);

      for (walk = qtmux->sinkpads; walk && !fail; walk = g_slist_next (walk)) {
        GstCollectData *cdata = (GstCollectData *) walk->data;
        GstQTPad *qpad = (GstQTPad *) cdata;
        /* write info for each stream */
        fail = atoms_recov_write_trak_info (qtmux->moov_recov_file, qpad->trak);
        if (fail) {
          GST_WARNING_OBJECT (qtmux, "Failed to write trak info to recovery "
              "file");
        }
      }
      if (fail) {
        /* cleanup */
        fclose (qtmux->moov_recov_file);
        qtmux->moov_recov_file = NULL;
        GST_WARNING_OBJECT (qtmux, "An error was detected while writing to "
            "recover file, moov recovery won't work");
      }
    }
  }
  GST_OBJECT_UNLOCK (qtmux);

  /* 
   * send mdat header if already needed, and mark position for later update.
   * We don't send ftyp now if we are on fast start mode, because we can
   * better fine tune using the information we gather to create the whole moov
   * atom.
   */
  if (qtmux->fast_start) {
    GST_OBJECT_LOCK (qtmux);
    qtmux->fast_start_file = g_fopen (qtmux->fast_start_file_path, "wb+");
    if (!qtmux->fast_start_file)
      goto open_failed;
    GST_OBJECT_UNLOCK (qtmux);

    /* send a dummy buffer for preroll */
    ret = gst_qt_mux_send_buffer (qtmux, gst_buffer_new (), NULL, FALSE);
    if (ret != GST_FLOW_OK)
      goto exit;

  } else {
    ret = gst_qt_mux_prepare_and_send_ftyp (qtmux);
    if (ret != GST_FLOW_OK) {
      goto exit;
    }

    /* well, it's moov pos if fragmented ... */
    qtmux->mdat_pos = qtmux->header_size;

    if (qtmux->fragment_duration) {
      GST_DEBUG_OBJECT (qtmux, "fragment duration %d ms, writing headers",
          qtmux->fragment_duration);
      /* also used as snapshot marker to indicate fragmented file */
      qtmux->fragment_sequence = 1;
      /* prepare moov and/or tags */
      gst_qt_mux_configure_moov (qtmux, NULL);
      gst_qt_mux_setup_metadata (qtmux);
      ret = gst_qt_mux_send_moov (qtmux, &qtmux->header_size, FALSE);
      if (ret != GST_FLOW_OK)
        return ret;
      /* extra atoms */
      ret =
          gst_qt_mux_send_extra_atoms (qtmux, TRUE, &qtmux->header_size, FALSE);
      if (ret != GST_FLOW_OK)
        return ret;
      /* prepare index */
      if (!qtmux->streamable)
        qtmux->mfra = atom_mfra_new (qtmux->context);
    } else {
      /* extended to ensure some spare space */
      ret = gst_qt_mux_send_mdat_header (qtmux, &qtmux->header_size, 0, TRUE);
    }
  }

exit:
  return ret;

  /* ERRORS */
open_failed:
  {
    GST_ELEMENT_ERROR (qtmux, RESOURCE, OPEN_READ_WRITE,
        (("Could not open temporary file \"%s\""), qtmux->fast_start_file_path),
        GST_ERROR_SYSTEM);
    GST_OBJECT_UNLOCK (qtmux);
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_qt_mux_stop_file (GstQTMux * qtmux)
{
  gboolean ret = GST_FLOW_OK;
  guint64 offset = 0, size = 0;
  GSList *walk;
  gboolean large_file;
  guint32 timescale;
  GstClockTime first_ts = GST_CLOCK_TIME_NONE;

  GST_DEBUG_OBJECT (qtmux, "Updating remaining values and sending last data");

  /* pushing last buffers for each pad */
  for (walk = qtmux->collect->data; walk; walk = g_slist_next (walk)) {
    GstCollectData *cdata = (GstCollectData *) walk->data;
    GstQTPad *qtpad = (GstQTPad *) cdata;

    /* send last buffer */
    GST_DEBUG_OBJECT (qtmux, "Sending the last buffer for pad %s",
        GST_PAD_NAME (qtpad->collect.pad));
    ret = gst_qt_mux_add_buffer (qtmux, qtpad, NULL);
    if (ret != GST_FLOW_OK)
      GST_WARNING_OBJECT (qtmux, "Failed to send last buffer for %s, "
          "flow return: %s", GST_PAD_NAME (qtpad->collect.pad),
          gst_flow_get_name (ret));

    if (!GST_CLOCK_TIME_IS_VALID (qtpad->first_ts)) {
      GST_DEBUG_OBJECT (qtmux, "Pad %s has no buffers",
          GST_PAD_NAME (qtpad->collect.pad));
      continue;
    }

    /* determine max stream duration */
    if (!GST_CLOCK_TIME_IS_VALID (first_ts) ||
        (GST_CLOCK_TIME_IS_VALID (qtpad->first_ts) &&
            qtpad->last_dts > first_ts)) {
      first_ts = qtpad->last_dts;
    }
  }

  if (qtmux->fragment_sequence) {
    GstEvent *event;

    if (qtmux->mfra) {
      guint8 *data = NULL;
      GstBuffer *buf;

      size = offset = 0;
      GST_DEBUG_OBJECT (qtmux, "adding mfra");
      if (!atom_mfra_copy_data (qtmux->mfra, &data, &size, &offset))
        goto serialize_error;
      buf = _gst_buffer_new_take_data (data, offset);
      ret = gst_qt_mux_send_buffer (qtmux, buf, NULL, FALSE);
      if (ret != GST_FLOW_OK)
        return ret;
    } else {
      /* must have been streamable; no need to write duration */
      GST_DEBUG_OBJECT (qtmux, "streamable file; nothing to stop");
      return GST_FLOW_OK;
    }


    timescale = qtmux->timescale;
    /* only mvex duration is updated,
     * mvhd should be consistent with empty moov
     * (but TODO maybe some clients do not handle that well ?) */
    qtmux->moov->mvex.mehd.fragment_duration =
        gst_util_uint64_scale (first_ts, timescale, GST_SECOND);
    GST_DEBUG_OBJECT (qtmux, "rewriting moov with mvex duration %"
        GST_TIME_FORMAT, GST_TIME_ARGS (first_ts));
    /* seek and rewrite the header */
    event = gst_event_new_new_segment (FALSE, 1.0, GST_FORMAT_BYTES,
        qtmux->mdat_pos, GST_CLOCK_TIME_NONE, 0);
    gst_pad_push_event (qtmux->srcpad, event);
    /* no need to seek back */
    return gst_qt_mux_send_moov (qtmux, NULL, FALSE);
  }

  gst_qt_mux_configure_moov (qtmux, &timescale);

  /* check for late streams */
  first_ts = GST_CLOCK_TIME_NONE;
  for (walk = qtmux->collect->data; walk; walk = g_slist_next (walk)) {
    GstCollectData *cdata = (GstCollectData *) walk->data;
    GstQTPad *qtpad = (GstQTPad *) cdata;

    if (!GST_CLOCK_TIME_IS_VALID (first_ts) ||
        (GST_CLOCK_TIME_IS_VALID (qtpad->first_ts) &&
            qtpad->first_ts < first_ts)) {
      first_ts = qtpad->first_ts;
    }
  }
  GST_DEBUG_OBJECT (qtmux, "Media first ts selected: %" GST_TIME_FORMAT,
      GST_TIME_ARGS (first_ts));
  /* add EDTSs for late streams */
  for (walk = qtmux->collect->data; walk; walk = g_slist_next (walk)) {
    GstCollectData *cdata = (GstCollectData *) walk->data;
    GstQTPad *qtpad = (GstQTPad *) cdata;
    guint32 lateness;
    guint32 duration;

    if (GST_CLOCK_TIME_IS_VALID (qtpad->first_ts) &&
        qtpad->first_ts > first_ts + MAX_TOLERATED_LATENESS) {
      GST_DEBUG_OBJECT (qtmux, "Pad %s is a late stream by %" GST_TIME_FORMAT,
          GST_PAD_NAME (qtpad->collect.pad),
          GST_TIME_ARGS (qtpad->first_ts - first_ts));
      lateness = gst_util_uint64_scale_round (qtpad->first_ts - first_ts,
          timescale, GST_SECOND);
      duration = qtpad->trak->tkhd.duration;
      atom_trak_add_elst_entry (qtpad->trak, lateness, (guint32) - 1,
          (guint32) (1 * 65536.0));
      atom_trak_add_elst_entry (qtpad->trak, duration, 0,
          (guint32) (1 * 65536.0));

      /* need to add the empty time to the trak duration */
      qtpad->trak->tkhd.duration += lateness;
    }
  }

  /* tags into file metadata */
  gst_qt_mux_setup_metadata (qtmux);

  large_file = (qtmux->mdat_size > MDAT_LARGE_FILE_LIMIT);
  /* if faststart, update the offset of the atoms in the movie with the offset
   * that the movie headers before mdat will cause.
   * Also, send the ftyp */
  if (qtmux->fast_start_file) {
    GstFlowReturn flow_ret;
    offset = size = 0;

    flow_ret = gst_qt_mux_prepare_and_send_ftyp (qtmux);
    if (flow_ret != GST_FLOW_OK) {
      goto ftyp_error;
    }
    /* copy into NULL to obtain size */
    if (!atom_moov_copy_data (qtmux->moov, NULL, &size, &offset))
      goto serialize_error;
    GST_DEBUG_OBJECT (qtmux, "calculated moov atom size %" G_GUINT64_FORMAT,
        offset);
    offset += qtmux->header_size + (large_file ? 16 : 8);

    /* sum up with the extra atoms size */
    ret = gst_qt_mux_send_extra_atoms (qtmux, FALSE, &offset, FALSE);
    if (ret != GST_FLOW_OK)
      return ret;
  } else {
    offset = qtmux->header_size;
  }
  atom_moov_chunks_add_offset (qtmux->moov, offset);

  /* moov */
  /* note: as of this point, we no longer care about tracking written data size,
   * since there is no more use for it anyway */
  ret = gst_qt_mux_send_moov (qtmux, NULL, FALSE);
  if (ret != GST_FLOW_OK)
    return ret;

  /* extra atoms */
  ret = gst_qt_mux_send_extra_atoms (qtmux, TRUE, NULL, FALSE);
  if (ret != GST_FLOW_OK)
    return ret;

  /* if needed, send mdat atom and move buffered data into it */
  if (qtmux->fast_start_file) {
    /* mdat_size = accumulated (buffered data) */
    ret = gst_qt_mux_send_mdat_header (qtmux, NULL, qtmux->mdat_size,
        large_file);
    if (ret != GST_FLOW_OK)
      return ret;
    ret = gst_qt_mux_send_buffered_data (qtmux, NULL);
    if (ret != GST_FLOW_OK)
      return ret;
  } else {
    /* mdat needs update iff not using faststart */
    GST_DEBUG_OBJECT (qtmux, "updating mdat size");
    ret = gst_qt_mux_update_mdat_size (qtmux, qtmux->mdat_pos,
        qtmux->mdat_size, NULL);
    /* note; no seeking back to the end of file is done,
     * since we no longer write anything anyway */
  }

  return ret;

  /* ERRORS */
serialize_error:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("Failed to serialize moov"));
    return GST_FLOW_ERROR;
  }
ftyp_error:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL), ("Failed to send ftyp"));
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_qt_mux_pad_fragment_add_buffer (GstQTMux * qtmux, GstQTPad * pad,
    GstBuffer * buf, gboolean force, guint32 nsamples, gint64 dts,
    guint32 delta, guint32 size, gboolean sync, gint64 pts_offset)
{
  GstFlowReturn ret = GST_FLOW_OK;

  /* setup if needed */
  if (G_UNLIKELY (!pad->traf || force))
    goto init;

flush:
  /* flush pad fragment if threshold reached,
   * or at new keyframe if we should be minding those in the first place */
  if (G_UNLIKELY (force || (sync && pad->sync) ||
          pad->fragment_duration < (gint64) delta)) {
    AtomMOOF *moof;
    guint64 size = 0, offset = 0;
    guint8 *data = NULL;
    GstBuffer *buffer;
    guint i, total_size;

    /* now we know where moof ends up, update offset in tfra */
    if (pad->tfra)
      atom_tfra_update_offset (pad->tfra, qtmux->header_size);

    moof = atom_moof_new (qtmux->context, qtmux->fragment_sequence);
    /* takes ownership */
    atom_moof_add_traf (moof, pad->traf);
    pad->traf = NULL;
    atom_moof_copy_data (moof, &data, &size, &offset);
    buffer = _gst_buffer_new_take_data (data, offset);
    GST_LOG_OBJECT (qtmux, "writing moof size %d", GST_BUFFER_SIZE (buffer));
    ret = gst_qt_mux_send_buffer (qtmux, buffer, &qtmux->header_size, FALSE);

    /* and actual data */
    total_size = 0;
    for (i = 0; i < atom_array_get_len (&pad->fragment_buffers); i++) {
      total_size +=
          GST_BUFFER_SIZE (atom_array_index (&pad->fragment_buffers, i));
    }

    GST_LOG_OBJECT (qtmux, "writing %d buffers, total_size %d",
        atom_array_get_len (&pad->fragment_buffers), total_size);
    if (ret == GST_FLOW_OK)
      ret = gst_qt_mux_send_mdat_header (qtmux, &qtmux->header_size, total_size,
          FALSE);
    for (i = 0; i < atom_array_get_len (&pad->fragment_buffers); i++) {
      if (G_LIKELY (ret == GST_FLOW_OK))
        ret = gst_qt_mux_send_buffer (qtmux,
            atom_array_index (&pad->fragment_buffers, i), &qtmux->header_size,
            FALSE);
      else
        gst_buffer_unref (atom_array_index (&pad->fragment_buffers, i));
    }

    atom_array_clear (&pad->fragment_buffers);
    atom_moof_free (moof);
    qtmux->fragment_sequence++;
    force = FALSE;
  }

init:
  if (G_UNLIKELY (!pad->traf)) {
    GST_LOG_OBJECT (qtmux, "setting up new fragment");
    pad->traf = atom_traf_new (qtmux->context, atom_trak_get_id (pad->trak));
    atom_array_init (&pad->fragment_buffers, 512);
    pad->fragment_duration = gst_util_uint64_scale (qtmux->fragment_duration,
        atom_trak_get_timescale (pad->trak), 1000);

    if (G_UNLIKELY (qtmux->mfra && !pad->tfra)) {
      pad->tfra = atom_tfra_new (qtmux->context, atom_trak_get_id (pad->trak));
      atom_mfra_add_tfra (qtmux->mfra, pad->tfra);
    }
  }

  /* add buffer and metadata */
  atom_traf_add_samples (pad->traf, delta, size, sync, pts_offset,
      pad->sync && sync);
  atom_array_append (&pad->fragment_buffers, buf, 256);
  pad->fragment_duration -= delta;

  if (pad->tfra) {
    guint32 sn = atom_traf_get_sample_num (pad->traf);

    if ((sync && pad->sync) || (sn == 1 && !pad->sync))
      atom_tfra_add_entry (pad->tfra, dts, sn);
  }

  if (G_UNLIKELY (force))
    goto flush;

  return ret;
}

/* sigh, tiny list helpers to re-order stuff */
static void
gst_qt_mux_push_ts (GstQTMux * qtmux, GstQTPad * pad, GstClockTime ts)
{
  gint i;

  for (i = 0; (i < QTMUX_NO_OF_TS) && (i < pad->ts_n_entries); i++) {
    if (ts > pad->ts_entries[i])
      break;
  }
  memmove (&pad->ts_entries[i + 1], &pad->ts_entries[i],
      sizeof (GstClockTime) * (pad->ts_n_entries - i));
  pad->ts_entries[i] = ts;
  pad->ts_n_entries++;
}

/* takes ownership of @buf */
static GstBuffer *
gst_qt_mux_get_asc_buffer_ts (GstQTMux * qtmux, GstQTPad * pad, GstBuffer * buf)
{
  const gint wrap = G_N_ELEMENTS (pad->buf_entries);
  GstClockTime ts;

  /* store buffer and ts, latter ordered */
  if (buf) {
    pad->buf_entries[pad->buf_tail++] = buf;
    pad->buf_tail %= wrap;
    gst_qt_mux_push_ts (qtmux, pad, GST_BUFFER_TIMESTAMP (buf));
  }

  if (pad->ts_n_entries && (!buf || pad->ts_n_entries >= QTMUX_NO_OF_TS)) {
    ts = pad->ts_entries[--pad->ts_n_entries];
    buf = pad->buf_entries[pad->buf_head];
    pad->buf_entries[pad->buf_head++] = NULL;
    pad->buf_head %= wrap;
    buf = gst_buffer_make_metadata_writable (buf);
    /* track original ts (= pts ?) for later */
    GST_BUFFER_OFFSET_END (buf) = GST_BUFFER_TIMESTAMP (buf);
    GST_BUFFER_TIMESTAMP (buf) = ts;
    GST_DEBUG_OBJECT (qtmux, "next buffer uses reordered ts %" GST_TIME_FORMAT,
        GST_TIME_ARGS (ts));
  } else {
    buf = NULL;
  }

  return buf;
}

/*
 * Here we push the buffer and update the tables in the track atoms
 */
static GstFlowReturn
gst_qt_mux_add_buffer (GstQTMux * qtmux, GstQTPad * pad, GstBuffer * buf)
{
  GstBuffer *last_buf = NULL;
  GstClockTime duration;
  guint nsamples, sample_size;
  guint64 chunk_offset;
  gint64 last_dts, scaled_duration;
  gint64 pts_offset = 0;
  gboolean sync = FALSE, do_pts = FALSE;
  gboolean drain = (buf == NULL);
  GstFlowReturn ret = GST_FLOW_OK;

  if (!pad->fourcc)
    goto not_negotiated;

  /* if this pad has a prepare function, call it */
  if (pad->prepare_buf_func != NULL) {
    buf = pad->prepare_buf_func (pad, buf, qtmux);
  }

again:
  last_buf = pad->last_buf;
  if (G_UNLIKELY (qtmux->dts_method == DTS_METHOD_REORDER)) {
    buf = gst_qt_mux_get_asc_buffer_ts (qtmux, pad, buf);
    if (!buf && !last_buf) {
      GST_DEBUG_OBJECT (qtmux, "no reordered buffer");
      return GST_FLOW_OK;
    }
  }

  if (last_buf == NULL) {
#ifndef GST_DISABLE_GST_DEBUG
    if (buf == NULL) {
      GST_DEBUG_OBJECT (qtmux, "Pad %s has no previous buffer stored and "
          "received NULL buffer, doing nothing",
          GST_PAD_NAME (pad->collect.pad));
    } else {
      GST_LOG_OBJECT (qtmux,
          "Pad %s has no previous buffer stored, storing now",
          GST_PAD_NAME (pad->collect.pad));
    }
#endif
    pad->last_buf = buf;
    goto exit;
  } else
    gst_buffer_ref (last_buf);

  /* nasty heuristic mess to guestimate dealing with DTS/PTS,
   * while also trying to stay close to input ts to preserve sync,
   * so in DTS_METHOD_DD:
   * - prefer using input ts where possible
   * - if those detected out-of-order (*), mark as out-of-order
   * - if in out-of-order, then
   *   - if duration available, use that as delta
   *     Also mind to preserve sync between streams, and adding
   *     durations might drift, so try to resync when we expect
   *     input ts == (sum of durations), which is at some keyframe input frame.
   *   - if no duration available, we are actually in serious trouble and need
   *     to hack around that, so we fail.
   * To remedy failure, alternatively, in DTS_METHOD_REORDER:
   * - collect some buffers and re-order timestamp,
   *   then process the oldest buffer with smallest timestamps.
   *   This should typically compensate for some codec's handywork with ts.
   * ... but in case this makes ts end up where not expected, in DTS_METHOD_ASC:
   * - keep each ts with its buffer and still keep a list of most recent X ts,
   *   use the (ascending) minimum of those as DTS (and the difference as ts delta),
   *   and use this DTS as a basis to obtain a (positive) CTS offset.
   *   This should yield exact PTS == buffer ts, but it seems not all players
   *   out there are aware of ctts pts ...
   *
   * 0.11 Phew, can we (pretty) please please sort out DTS/PTS on buffers ...
   */
  if (G_LIKELY (buf) && !pad->is_out_of_order) {
    if (G_LIKELY (GST_BUFFER_TIMESTAMP_IS_VALID (last_buf) &&
            GST_BUFFER_TIMESTAMP_IS_VALID (buf))) {
      if ((GST_BUFFER_TIMESTAMP (buf) < GST_BUFFER_TIMESTAMP (last_buf))) {
        GST_DEBUG_OBJECT (qtmux, "detected out-of-order input");
        pad->is_out_of_order = TRUE;
      }
    } else {
      /* this is pretty bad */
      GST_WARNING_OBJECT (qtmux, "missing input timestamp");
      /* fall back to durations */
      pad->is_out_of_order = TRUE;
    }
  }

  /* would have to be some unusual input, but not impossible */
  if (G_UNLIKELY (qtmux->dts_method == DTS_METHOD_REORDER &&
          pad->is_out_of_order)) {
    goto no_order;
  }

  /* fall back to duration if last buffer or
   * out-of-order (determined previously), otherwise use input ts */
  if (buf == NULL ||
      (pad->is_out_of_order && qtmux->dts_method == DTS_METHOD_DD)) {
    if (!GST_BUFFER_DURATION_IS_VALID (last_buf)) {
      /* be forgiving for some possibly last upstream flushed buffer */
      if (buf)
        goto no_time;
      GST_WARNING_OBJECT (qtmux, "no duration for last buffer");
      /* iso spec recommends some small value, try 0 */
      duration = 0;
    } else {
      duration = GST_BUFFER_DURATION (last_buf);
      /* avoid drift in sum timestamps,
       * so use input timestamp for suitable keyframe */
      if (buf && !GST_BUFFER_FLAG_IS_SET (buf, GST_BUFFER_FLAG_DELTA_UNIT) &&
          GST_BUFFER_TIMESTAMP (buf) >= pad->last_dts) {
        GST_DEBUG_OBJECT (qtmux, "resyncing out-of-order input to ts; "
            "replacing %" GST_TIME_FORMAT " by %" GST_TIME_FORMAT,
            GST_TIME_ARGS (pad->last_dts + duration),
            GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)));
        duration = GST_BUFFER_TIMESTAMP (buf) - pad->last_dts;
      }
    }
  } else if (qtmux->dts_method != DTS_METHOD_ASC) {
    duration = GST_BUFFER_TIMESTAMP (buf) - GST_BUFFER_TIMESTAMP (last_buf);
  } else {
    GstClockTime ts;

    g_assert (qtmux->dts_method == DTS_METHOD_ASC);
    if (!qtmux->guess_pts)
      goto need_pts;

    /* add timestamp to queue; keeps in descending order */
    gst_qt_mux_push_ts (qtmux, pad, GST_BUFFER_TIMESTAMP (last_buf));
    /* chuck out smallest/last one if we have enough */
    if (G_LIKELY (pad->ts_n_entries > QTMUX_NO_OF_TS))
      pad->ts_n_entries--;
    /* peek the now smallest timestamp */
    ts = pad->ts_entries[pad->ts_n_entries - 1];
    /* these tails are expected to be (strictly) ascending with
     * large enough history */
    GST_DEBUG_OBJECT (qtmux, "ASC method; base timestamp %" GST_TIME_FORMAT,
        GST_TIME_ARGS (ts));
    if (ts >= pad->last_dts) {
      duration = ts - pad->last_dts;
    } else {
      /* fallback to previous value, negative ct offset might handle */
      GST_WARNING_OBJECT (qtmux, "unexpected decrease in timestamp");
      duration = 0;
    }
    /* arrange for small non-zero duration/delta << expected frame time */
    ts = gst_util_uint64_scale (10, GST_SECOND,
        atom_trak_get_timescale (pad->trak));
    duration = MAX (duration, ts);
  }

  gst_buffer_replace (&pad->last_buf, buf);

  last_dts = gst_util_uint64_scale_round (pad->last_dts,
      atom_trak_get_timescale (pad->trak), GST_SECOND);

  /* fragments only deal with 1 buffer == 1 chunk (== 1 sample) */
  if (pad->sample_size && !qtmux->fragment_sequence) {
    /* Constant size packets: usually raw audio (with many samples per
       buffer (= chunk)), but can also be fixed-packet-size codecs like ADPCM
     */
    sample_size = pad->sample_size;
    if (GST_BUFFER_SIZE (last_buf) % sample_size != 0)
      goto fragmented_sample;
    /* note: qt raw audio storage warps it implicitly into a timewise
     * perfect stream, discarding buffer times */
    if (GST_BUFFER_DURATION (last_buf) != GST_CLOCK_TIME_NONE) {
      nsamples = gst_util_uint64_scale_round (GST_BUFFER_DURATION (last_buf),
          atom_trak_get_timescale (pad->trak), GST_SECOND);
    } else {
      nsamples = GST_BUFFER_SIZE (last_buf) / sample_size;
    }
    duration = GST_BUFFER_DURATION (last_buf) / nsamples;

    /* timescale = samplerate */
    scaled_duration = 1;
    pad->last_dts += duration * nsamples;
  } else {
    nsamples = 1;
    sample_size = GST_BUFFER_SIZE (last_buf);
    if (pad->have_dts) {
      gint64 scaled_dts;
      pad->last_dts = GST_BUFFER_OFFSET_END (last_buf);
      if ((gint64) (pad->last_dts) < 0) {
        scaled_dts = -gst_util_uint64_scale_round (-pad->last_dts,
            atom_trak_get_timescale (pad->trak), GST_SECOND);
      } else {
        scaled_dts = gst_util_uint64_scale_round (pad->last_dts,
            atom_trak_get_timescale (pad->trak), GST_SECOND);
      }
      scaled_duration = scaled_dts - last_dts;
      last_dts = scaled_dts;
    } else {
      /* first convert intended timestamp (in GstClockTime resolution) to
       * trak timescale, then derive delta;
       * this ensures sums of (scale)delta add up to converted timestamp,
       * which only deviates at most 1/scale from timestamp itself */
      scaled_duration = gst_util_uint64_scale_round (pad->last_dts + duration,
          atom_trak_get_timescale (pad->trak), GST_SECOND) - last_dts;
      pad->last_dts += duration;
    }
  }
  chunk_offset = qtmux->mdat_size;

  GST_LOG_OBJECT (qtmux,
      "Pad (%s) dts updated to %" GST_TIME_FORMAT,
      GST_PAD_NAME (pad->collect.pad), GST_TIME_ARGS (pad->last_dts));
  GST_LOG_OBJECT (qtmux,
      "Adding %d samples to track, duration: %" G_GUINT64_FORMAT
      " size: %" G_GUINT32_FORMAT " chunk offset: %" G_GUINT64_FORMAT,
      nsamples, scaled_duration, sample_size, chunk_offset);

  /* might be a sync sample */
  if (pad->sync &&
      !GST_BUFFER_FLAG_IS_SET (last_buf, GST_BUFFER_FLAG_DELTA_UNIT)) {
    GST_LOG_OBJECT (qtmux, "Adding new sync sample entry for track of pad %s",
        GST_PAD_NAME (pad->collect.pad));
    sync = TRUE;
  }

  /* optionally calculate ctts entry values
   * (if composition-time expected different from decoding-time) */
  /* really not recommended:
   * - decoder typically takes care of dts/pts issues
   * - in case of out-of-order, dts may only be determined as above
   *   (e.g. sum of duration), which may be totally different from
   *   buffer timestamps in case of multiple segment, non-perfect streams
   *  (and just perhaps maybe with some luck segment_to_running_time
   *   or segment_to_media_time might get near to it) */
  if ((pad->have_dts || qtmux->guess_pts)) {
    guint64 pts;

    pts = qtmux->dts_method == DTS_METHOD_REORDER ?
        GST_BUFFER_OFFSET_END (last_buf) : GST_BUFFER_TIMESTAMP (last_buf);
    pts = gst_util_uint64_scale_round (pts,
        atom_trak_get_timescale (pad->trak), GST_SECOND);
    pts_offset = (gint64) (pts - last_dts);
    do_pts = TRUE;
    GST_LOG_OBJECT (qtmux, "Adding ctts entry for pad %s: %" G_GINT64_FORMAT,
        GST_PAD_NAME (pad->collect.pad), pts_offset);
  }

  /*
   * Each buffer starts a new chunk, so we can assume the buffer
   * duration is the chunk duration
   */
  if (GST_CLOCK_TIME_IS_VALID (duration) && (duration > qtmux->longest_chunk ||
          !GST_CLOCK_TIME_IS_VALID (qtmux->longest_chunk))) {
    GST_DEBUG_OBJECT (qtmux, "New longest chunk found: %" GST_TIME_FORMAT
        ", pad %s", GST_TIME_ARGS (duration), GST_PAD_NAME (pad->collect.pad));
    qtmux->longest_chunk = duration;
  }

  /* if this is the first buffer, store the timestamp */
  if (G_UNLIKELY (pad->first_ts == GST_CLOCK_TIME_NONE) && last_buf) {
    if (GST_CLOCK_TIME_IS_VALID (GST_BUFFER_TIMESTAMP (last_buf))) {
      pad->first_ts = GST_BUFFER_TIMESTAMP (last_buf);
    } else {
      GST_DEBUG_OBJECT (qtmux, "First buffer for pad %s has no timestamp, "
          "using 0 as first timestamp", GST_PAD_NAME (pad->collect.pad));
      pad->first_ts = 0;
    }
    GST_DEBUG_OBJECT (qtmux, "Stored first timestamp for pad %s %"
        GST_TIME_FORMAT, GST_PAD_NAME (pad->collect.pad),
        GST_TIME_ARGS (pad->first_ts));
  }

  /* now we go and register this buffer/sample all over */
  /* note that a new chunk is started each time (not fancy but works) */
  if (qtmux->moov_recov_file) {
    if (!atoms_recov_write_trak_samples (qtmux->moov_recov_file, pad->trak,
            nsamples, (gint32) scaled_duration, sample_size, chunk_offset, sync,
            do_pts, pts_offset)) {
      GST_WARNING_OBJECT (qtmux, "Failed to write sample information to "
          "recovery file, disabling recovery");
      fclose (qtmux->moov_recov_file);
      qtmux->moov_recov_file = NULL;
    }
  }

  if (buf)
    gst_buffer_unref (buf);

  if (qtmux->fragment_sequence) {
    /* ensure that always sync samples are marked as such */
    ret = gst_qt_mux_pad_fragment_add_buffer (qtmux, pad, last_buf,
        buf == NULL, nsamples, last_dts, (gint32) scaled_duration, sample_size,
        !pad->sync || sync, pts_offset);
  } else {
    atom_trak_add_samples (pad->trak, nsamples, (gint32) scaled_duration,
        sample_size, chunk_offset, sync, pts_offset);
    ret = gst_qt_mux_send_buffer (qtmux, last_buf, &qtmux->mdat_size, TRUE);
  }

exit:
  if (G_UNLIKELY (drain && qtmux->dts_method == DTS_METHOD_REORDER &&
          ret == GST_FLOW_OK)) {
    buf = NULL;
    goto again;
  }

  return ret;

  /* ERRORS */
bail:
  {
    if (buf)
      gst_buffer_unref (buf);
    gst_buffer_unref (last_buf);
    return GST_FLOW_ERROR;
  }
no_time:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("Received buffer without timestamp/duration. "
            "Using e.g. dts-method=reorder might help."));
    goto bail;
  }
no_order:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("DTS method failed to re-order timestamps."));
    goto bail;
  }
need_pts:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("Selected DTS method also needs PTS enabled."));
    goto bail;
  }
fragmented_sample:
  {
    GST_ELEMENT_ERROR (qtmux, STREAM, MUX, (NULL),
        ("Audio buffer contains fragmented sample."));
    goto bail;
  }
not_negotiated:
  {
    GST_ELEMENT_ERROR (qtmux, CORE, NEGOTIATION, (NULL),
        ("format wasn't negotiated before buffer flow on pad %s",
            GST_PAD_NAME (pad->collect.pad)));
    if (buf)
      gst_buffer_unref (buf);
    return GST_FLOW_NOT_NEGOTIATED;
  }
}

static GstFlowReturn
gst_qt_mux_collected (GstCollectPads * pads, gpointer user_data)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstQTMux *qtmux = GST_QT_MUX_CAST (user_data);
  GSList *walk;
  GstQTPad *best_pad = NULL;
  GstClockTime time, best_time = GST_CLOCK_TIME_NONE;
  GstBuffer *buf;

  if (G_UNLIKELY (qtmux->state == GST_QT_MUX_STATE_STARTED)) {
    if ((ret = gst_qt_mux_start_file (qtmux)) != GST_FLOW_OK)
      return ret;
    else
      qtmux->state = GST_QT_MUX_STATE_DATA;
  }

  if (G_UNLIKELY (qtmux->state == GST_QT_MUX_STATE_EOS))
    return GST_FLOW_UNEXPECTED;

  /* select the best buffer */
  walk = qtmux->collect->data;
  while (walk) {
    GstQTPad *pad;
    GstCollectData *data;

    data = (GstCollectData *) walk->data;
    pad = (GstQTPad *) data;

    walk = g_slist_next (walk);

    buf = gst_collect_pads_peek (pads, data);
    if (buf == NULL) {
      GST_LOG_OBJECT (qtmux, "Pad %s has no buffers",
          GST_PAD_NAME (pad->collect.pad));
      continue;
    }
    time = GST_BUFFER_TIMESTAMP (buf);
    gst_buffer_unref (buf);

    /* invalid should pass */
    if (G_LIKELY (GST_CLOCK_TIME_IS_VALID (time))) {
      time =
          gst_segment_to_running_time (&data->segment, GST_FORMAT_TIME, time);
      if (G_UNLIKELY (!GST_CLOCK_TIME_IS_VALID (time))) {
        GST_DEBUG_OBJECT (qtmux, "clipping buffer on pad %s outside segment",
            GST_PAD_NAME (data->pad));
        buf = gst_collect_pads_pop (pads, data);
        gst_buffer_unref (buf);
        return GST_FLOW_OK;
      }
    }

    if (best_pad == NULL || !GST_CLOCK_TIME_IS_VALID (time) ||
        (GST_CLOCK_TIME_IS_VALID (best_time) && time < best_time)) {
      best_pad = pad;
      best_time = time;
    }
  }

  if (best_pad != NULL) {
    GST_LOG_OBJECT (qtmux, "selected pad %s with time %" GST_TIME_FORMAT,
        GST_PAD_NAME (best_pad->collect.pad), GST_TIME_ARGS (best_time));
    buf = gst_collect_pads_pop (pads, &best_pad->collect);
    buf = gst_buffer_make_metadata_writable (buf);
    GST_BUFFER_TIMESTAMP (buf) = best_time;
    ret = gst_qt_mux_add_buffer (qtmux, best_pad, buf);
  } else {
    ret = gst_qt_mux_stop_file (qtmux);
    if (ret == GST_FLOW_OK) {
      GST_DEBUG_OBJECT (qtmux, "Pushing eos");
      gst_pad_push_event (qtmux->srcpad, gst_event_new_eos ());
      ret = GST_FLOW_UNEXPECTED;
    } else {
      GST_WARNING_OBJECT (qtmux, "Failed to stop file: %s",
          gst_flow_get_name (ret));
    }
    qtmux->state = GST_QT_MUX_STATE_EOS;
  }

  return ret;
}

static gboolean
check_field (GQuark field_id, const GValue * value, gpointer user_data)
{
  GstStructure *structure = (GstStructure *) user_data;
  const GValue *other = gst_structure_id_get_value (structure, field_id);
  if (other == NULL)
    return FALSE;
  return gst_value_compare (value, other) == GST_VALUE_EQUAL;
}

static gboolean
gst_qtmux_caps_is_subset_full (GstQTMux * qtmux, GstCaps * subset,
    GstCaps * superset)
{
  GstStructure *sub_s = gst_caps_get_structure (subset, 0);
  GstStructure *sup_s = gst_caps_get_structure (superset, 0);

  return gst_structure_foreach (sub_s, check_field, sup_s);
}

static gboolean
gst_qt_mux_audio_sink_set_caps (GstPad * pad, GstCaps * caps)
{
  GstQTMux *qtmux = GST_QT_MUX_CAST (gst_pad_get_parent (pad));
  GstQTMuxClass *qtmux_klass = (GstQTMuxClass *) (G_OBJECT_GET_CLASS (qtmux));
  GstQTPad *qtpad = NULL;
  GstStructure *structure;
  const gchar *mimetype;
  gint rate, channels;
  const GValue *value = NULL;
  const GstBuffer *codec_data = NULL;
  GstQTMuxFormat format;
  AudioSampleEntry entry = { 0, };
  AtomInfo *ext_atom = NULL;
  gint constant_size = 0;
  const gchar *stream_format;
  GstCaps *current_caps = NULL;

  /* find stream data */
  qtpad = (GstQTPad *) gst_pad_get_element_private (pad);
  g_assert (qtpad);

  qtpad->prepare_buf_func = NULL;

  /* does not go well to renegotiate stream mid-way, unless
   * the old caps are a subset of the new one (this means upstream
   * added more info to the caps, as both should be 'fixed' caps) */
  if (qtpad->fourcc) {
    g_object_get (pad, "caps", &current_caps, NULL);
    g_assert (caps != NULL);

    if (!gst_qtmux_caps_is_subset_full (qtmux, current_caps, caps)) {
      goto refuse_renegotiation;
    }
    GST_DEBUG_OBJECT (qtmux,
        "pad %s accepted renegotiation to %" GST_PTR_FORMAT " from %"
        GST_PTR_FORMAT, GST_PAD_NAME (pad), caps, GST_PAD_CAPS (pad));
  }

  GST_DEBUG_OBJECT (qtmux, "%s:%s, caps=%" GST_PTR_FORMAT,
      GST_DEBUG_PAD_NAME (pad), caps);

  format = qtmux_klass->format;
  structure = gst_caps_get_structure (caps, 0);
  mimetype = gst_structure_get_name (structure);

  /* common info */
  if (!gst_structure_get_int (structure, "channels", &channels) ||
      !gst_structure_get_int (structure, "rate", &rate)) {
    goto refuse_caps;
  }

  /* optional */
  value = gst_structure_get_value (structure, "codec_data");
  if (value != NULL)
    codec_data = gst_value_get_buffer (value);

  qtpad->is_out_of_order = FALSE;
  qtpad->have_dts = FALSE;

  /* set common properties */
  entry.sample_rate = rate;
  entry.channels = channels;
  /* default */
  entry.sample_size = 16;
  /* this is the typical compressed case */
  if (format == GST_QT_MUX_FORMAT_QT) {
    entry.version = 1;
    entry.compression_id = -2;
  }

  /* now map onto a fourcc, and some extra properties */
  if (strcmp (mimetype, "audio/mpeg") == 0) {
    gint mpegversion = 0;
    gint layer = -1;

    gst_structure_get_int (structure, "mpegversion", &mpegversion);
    switch (mpegversion) {
      case 1:
        gst_structure_get_int (structure, "layer", &layer);
        switch (layer) {
          case 3:
            /* mp3 */
            /* note: QuickTime player does not like mp3 either way in iso/mp4 */
            if (format == GST_QT_MUX_FORMAT_QT)
              entry.fourcc = FOURCC__mp3;
            else {
              entry.fourcc = FOURCC_mp4a;
              ext_atom =
                  build_esds_extension (qtpad->trak, ESDS_OBJECT_TYPE_MPEG1_P3,
                  ESDS_STREAM_TYPE_AUDIO, codec_data, qtpad->avg_bitrate,
                  qtpad->max_bitrate);
            }
            entry.samples_per_packet = 1152;
            entry.bytes_per_sample = 2;
            break;
        }
        break;
      case 4:

        /* check stream-format */
        stream_format = gst_structure_get_string (structure, "stream-format");
        if (stream_format) {
          if (strcmp (stream_format, "raw") != 0) {
            GST_WARNING_OBJECT (qtmux, "Unsupported AAC stream-format %s, "
                "please use 'raw'", stream_format);
            goto refuse_caps;
          }
        } else {
          GST_WARNING_OBJECT (qtmux, "No stream-format present in caps, "
              "assuming 'raw'");
        }

        if (!codec_data || GST_BUFFER_SIZE (codec_data) < 2)
          GST_WARNING_OBJECT (qtmux, "no (valid) codec_data for AAC audio");
        else {
          guint8 profile = GST_READ_UINT8 (GST_BUFFER_DATA (codec_data));

          /* warn if not Low Complexity profile */
          profile >>= 3;
          if (profile != 2)
            GST_WARNING_OBJECT (qtmux,
                "non-LC AAC may not run well on (Apple) QuickTime/iTunes");
        }

        /* AAC */
        entry.fourcc = FOURCC_mp4a;

        if (format == GST_QT_MUX_FORMAT_QT)
          ext_atom = build_mov_aac_extension (qtpad->trak, codec_data,
              qtpad->avg_bitrate, qtpad->max_bitrate);
        else
          ext_atom =
              build_esds_extension (qtpad->trak, ESDS_OBJECT_TYPE_MPEG4_P3,
              ESDS_STREAM_TYPE_AUDIO, codec_data, qtpad->avg_bitrate,
              qtpad->max_bitrate);
        break;
      default:
        break;
    }
  } else if (strcmp (mimetype, "audio/AMR") == 0) {
    entry.fourcc = FOURCC_samr;
    entry.sample_size = 16;
    entry.samples_per_packet = 160;
    entry.bytes_per_sample = 2;
    ext_atom = build_amr_extension ();
  } else if (strcmp (mimetype, "audio/AMR-WB") == 0) {
    entry.fourcc = FOURCC_sawb;
    entry.sample_size = 16;
    entry.samples_per_packet = 320;
    entry.bytes_per_sample = 2;
    ext_atom = build_amr_extension ();
  } else if (strcmp (mimetype, "audio/x-raw-int") == 0) {
    gint width;
    gint depth;
    gint endianness;
    gboolean sign;

    if (!gst_structure_get_int (structure, "width", &width) ||
        !gst_structure_get_int (structure, "depth", &depth) ||
        !gst_structure_get_boolean (structure, "signed", &sign)) {
      GST_DEBUG_OBJECT (qtmux, "broken caps, width/depth/signed field missing");
      goto refuse_caps;
    }

    if (depth <= 8) {
      endianness = G_BYTE_ORDER;
    } else if (!gst_structure_get_int (structure, "endianness", &endianness)) {
      GST_DEBUG_OBJECT (qtmux, "broken caps, endianness field missing");
      goto refuse_caps;
    }

    /* spec has no place for a distinction in these */
    if (width != depth) {
      GST_DEBUG_OBJECT (qtmux, "width must be same as depth!");
      goto refuse_caps;
    }

    if (sign) {
      if (endianness == G_LITTLE_ENDIAN)
        entry.fourcc = FOURCC_sowt;
      else if (endianness == G_BIG_ENDIAN)
        entry.fourcc = FOURCC_twos;
      /* maximum backward compatibility; only new version for > 16 bit */
      if (depth <= 16)
        entry.version = 0;
      /* not compressed in any case */
      entry.compression_id = 0;
      /* QT spec says: max at 16 bit even if sample size were actually larger,
       * however, most players (e.g. QuickTime!) seem to disagree, so ... */
      entry.sample_size = depth;
      entry.bytes_per_sample = depth / 8;
      entry.samples_per_packet = 1;
      entry.bytes_per_packet = depth / 8;
      entry.bytes_per_frame = entry.bytes_per_packet * channels;
    } else {
      if (width == 8 && depth == 8) {
        /* fall back to old 8-bit version */
        entry.fourcc = FOURCC_raw_;
        entry.version = 0;
        entry.compression_id = 0;
        entry.sample_size = 8;
      } else {
        GST_DEBUG_OBJECT (qtmux, "non 8-bit PCM must be signed");
        goto refuse_caps;
      }
    }
    constant_size = (depth / 8) * channels;
  } else if (strcmp (mimetype, "audio/x-alaw") == 0) {
    entry.fourcc = FOURCC_alaw;
    entry.samples_per_packet = 1023;
    entry.bytes_per_sample = 2;
  } else if (strcmp (mimetype, "audio/x-mulaw") == 0) {
    entry.fourcc = FOURCC_ulaw;
    entry.samples_per_packet = 1023;
    entry.bytes_per_sample = 2;
  } else if (strcmp (mimetype, "audio/x-adpcm") == 0) {
    gint blocksize;
    if (!gst_structure_get_int (structure, "block_align", &blocksize)) {
      GST_DEBUG_OBJECT (qtmux, "broken caps, block_align missing");
      goto refuse_caps;
    }
    /* Currently only supports WAV-style IMA ADPCM, for which the codec id is
       0x11 */
    entry.fourcc = MS_WAVE_FOURCC (0x11);
    /* 4 byte header per channel (including one sample). 2 samples per byte
       remaining. Simplifying gives the following (samples per block per
       channel) */
    entry.samples_per_packet = 2 * blocksize / channels - 7;
    entry.bytes_per_sample = 2;

    entry.bytes_per_frame = blocksize;
    entry.bytes_per_packet = blocksize / channels;
    /* ADPCM has constant size packets */
    constant_size = 1;
    /* TODO: I don't really understand why this helps, but it does! Constant
     * size and compression_id of -2 seem to be incompatible, and other files
     * in the wild use this too. */
    entry.compression_id = -1;

    ext_atom = build_ima_adpcm_extension (channels, rate, blocksize);
  } else if (strcmp (mimetype, "audio/x-alac") == 0) {
    GstBuffer *codec_config;
    gint len;

    entry.fourcc = FOURCC_alac;
    /* let's check if codec data already comes with 'alac' atom prefix */
    if (!codec_data || (len = GST_BUFFER_SIZE (codec_data)) < 28) {
      GST_DEBUG_OBJECT (qtmux, "broken caps, codec data missing");
      goto refuse_caps;
    }
    if (GST_READ_UINT32_LE (GST_BUFFER_DATA (codec_data) + 4) == FOURCC_alac) {
      len -= 8;
      codec_config = gst_buffer_create_sub ((GstBuffer *) codec_data, 8, len);
    } else {
      codec_config = gst_buffer_ref ((GstBuffer *) codec_data);
    }
    if (len != 28) {
      /* does not look good, but perhaps some trailing unneeded stuff */
      GST_WARNING_OBJECT (qtmux, "unexpected codec-data size, possibly broken");
    }
    if (format == GST_QT_MUX_FORMAT_QT)
      ext_atom = build_mov_alac_extension (qtpad->trak, codec_config);
    else
      ext_atom = build_codec_data_extension (FOURCC_alac, codec_config);
    /* set some more info */
    entry.bytes_per_sample = 2;
    entry.samples_per_packet =
        GST_READ_UINT32_BE (GST_BUFFER_DATA (codec_config) + 4);
    gst_buffer_unref (codec_config);
  }

  if (!entry.fourcc)
    goto refuse_caps;

  /* ok, set the pad info accordingly */
  qtpad->fourcc = entry.fourcc;
  qtpad->sample_size = constant_size;
  atom_trak_set_audio_type (qtpad->trak, qtmux->context, &entry,
      qtmux->trak_timescale ? qtmux->trak_timescale : entry.sample_rate,
      ext_atom, constant_size);

  gst_object_unref (qtmux);
  return TRUE;

  /* ERRORS */
refuse_caps:
  {
    GST_WARNING_OBJECT (qtmux, "pad %s refused caps %" GST_PTR_FORMAT,
        GST_PAD_NAME (pad), caps);
    gst_object_unref (qtmux);
    return FALSE;
  }
refuse_renegotiation:
  {
    GST_WARNING_OBJECT (qtmux,
        "pad %s refused renegotiation to %" GST_PTR_FORMAT,
        GST_PAD_NAME (pad), caps);
    gst_object_unref (qtmux);
    return FALSE;
  }
}

/* scale rate up or down by factor of 10 to fit into [1000,10000] interval */
static guint32
adjust_rate (guint64 rate)
{
  if (rate == 0)
    return 10000;

  while (rate >= 10000)
    rate /= 10;

  while (rate < 1000)
    rate *= 10;

  return (guint32) rate;
}

static gboolean
gst_qt_mux_video_sink_set_caps (GstPad * pad, GstCaps * caps)
{
  GstQTMux *qtmux = GST_QT_MUX_CAST (gst_pad_get_parent (pad));
  GstQTMuxClass *qtmux_klass = (GstQTMuxClass *) (G_OBJECT_GET_CLASS (qtmux));
  GstQTPad *qtpad = NULL;
  GstStructure *structure;
  const gchar *mimetype;
  gint width, height, depth = -1;
  gint framerate_num, framerate_den;
  guint32 rate;
  const GValue *value = NULL;
  const GstBuffer *codec_data = NULL;
  VisualSampleEntry entry = { 0, };
  GstQTMuxFormat format;
  AtomInfo *ext_atom = NULL;
  GList *ext_atom_list = NULL;
  gboolean sync = FALSE;
  int par_num, par_den;
  GstCaps *current_caps = NULL;

  /* find stream data */
  qtpad = (GstQTPad *) gst_pad_get_element_private (pad);
  g_assert (qtpad);

  qtpad->prepare_buf_func = NULL;

  /* does not go well to renegotiate stream mid-way, unless
   * the old caps are a subset of the new one (this means upstream
   * added more info to the caps, as both should be 'fixed' caps) */
  if (qtpad->fourcc) {
    g_object_get (pad, "caps", &current_caps, NULL);
    g_assert (caps != NULL);

    if (!gst_qtmux_caps_is_subset_full (qtmux, current_caps, caps)) {
      goto refuse_renegotiation;
    }
    GST_DEBUG_OBJECT (qtmux,
        "pad %s accepted renegotiation to %" GST_PTR_FORMAT " from %"
        GST_PTR_FORMAT, GST_PAD_NAME (pad), caps, GST_PAD_CAPS (pad));
  }

  GST_DEBUG_OBJECT (qtmux, "%s:%s, caps=%" GST_PTR_FORMAT,
      GST_DEBUG_PAD_NAME (pad), caps);

  format = qtmux_klass->format;
  structure = gst_caps_get_structure (caps, 0);
  mimetype = gst_structure_get_name (structure);

  /* required parts */
  if (!gst_structure_get_int (structure, "width", &width) ||
      !gst_structure_get_int (structure, "height", &height))
    goto refuse_caps;

  /* optional */
  depth = -1;
  /* works as a default timebase */
  framerate_num = 10000;
  framerate_den = 1;
  gst_structure_get_fraction (structure, "framerate", &framerate_num,
      &framerate_den);
  gst_structure_get_int (structure, "depth", &depth);
  value = gst_structure_get_value (structure, "codec_data");
  if (value != NULL)
    codec_data = gst_value_get_buffer (value);

  par_num = 1;
  par_den = 1;
  gst_structure_get_fraction (structure, "pixel-aspect-ratio", &par_num,
      &par_den);

  qtpad->is_out_of_order = FALSE;

  /* bring frame numerator into a range that ensures both reasonable resolution
   * as well as a fair duration */
  rate = qtmux->trak_timescale ?
      qtmux->trak_timescale : adjust_rate (framerate_num);
  GST_DEBUG_OBJECT (qtmux, "Rate of video track selected: %" G_GUINT32_FORMAT,
      rate);

  /* set common properties */
  entry.width = width;
  entry.height = height;
  entry.par_n = par_num;
  entry.par_d = par_den;
  /* should be OK according to qt and iso spec, override if really needed */
  entry.color_table_id = -1;
  entry.frame_count = 1;
  entry.depth = 24;

  /* sync entries by default */
  sync = TRUE;

  /* now map onto a fourcc, and some extra properties */
  if (strcmp (mimetype, "video/x-raw-rgb") == 0) {
    gint bpp;

    entry.fourcc = FOURCC_raw_;
    gst_structure_get_int (structure, "bpp", &bpp);
    entry.depth = bpp;
    sync = FALSE;
  } else if (strcmp (mimetype, "video/x-raw-yuv") == 0) {
    guint32 format = 0;

    sync = FALSE;
    gst_structure_get_fourcc (structure, "format", &format);
    switch (format) {
      case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
        if (depth == -1)
          depth = 24;
        entry.fourcc = FOURCC_2vuy;
        entry.depth = depth;
        break;
    }
  } else if (strcmp (mimetype, "video/x-h263") == 0) {
    ext_atom = NULL;
    if (format == GST_QT_MUX_FORMAT_QT)
      entry.fourcc = FOURCC_h263;
    else
      entry.fourcc = FOURCC_s263;
    ext_atom = build_h263_extension ();
    if (ext_atom != NULL)
      ext_atom_list = g_list_prepend (ext_atom_list, ext_atom);
  } else if (strcmp (mimetype, "video/x-divx") == 0 ||
      strcmp (mimetype, "video/mpeg") == 0) {
    gint version = 0;

    if (strcmp (mimetype, "video/x-divx") == 0) {
      gst_structure_get_int (structure, "divxversion", &version);
      version = version == 5 ? 1 : 0;
    } else {
      gst_structure_get_int (structure, "mpegversion", &version);
      version = version == 4 ? 1 : 0;
    }
    if (version) {
      entry.fourcc = FOURCC_mp4v;
      ext_atom =
          build_esds_extension (qtpad->trak, ESDS_OBJECT_TYPE_MPEG4_P2,
          ESDS_STREAM_TYPE_VISUAL, codec_data, qtpad->avg_bitrate,
          qtpad->max_bitrate);
      if (ext_atom != NULL)
        ext_atom_list = g_list_prepend (ext_atom_list, ext_atom);
      if (!codec_data)
        GST_WARNING_OBJECT (qtmux, "no codec_data for MPEG4 video; "
            "output might not play in Apple QuickTime (try global-headers?)");
    }
  } else if (strcmp (mimetype, "video/x-h264") == 0) {
    /* check if we accept these caps */
    if (gst_structure_has_field (structure, "stream-format")) {
      const gchar *format;
      const gchar *alignment;

      format = gst_structure_get_string (structure, "stream-format");
      alignment = gst_structure_get_string (structure, "alignment");

      if (strcmp (format, "avc") != 0 || alignment == NULL ||
          strcmp (alignment, "au") != 0) {
        GST_WARNING_OBJECT (qtmux, "Rejecting h264 caps, qtmux only accepts "
            "avc format with AU aligned samples");
        goto refuse_caps;
      }
    } else {
      GST_WARNING_OBJECT (qtmux, "no stream-format field in h264 caps");
      goto refuse_caps;
    }

    if (!codec_data) {
      GST_WARNING_OBJECT (qtmux, "no codec_data in h264 caps");
      goto refuse_caps;
    }

    entry.fourcc = FOURCC_avc1;
    if (qtpad->avg_bitrate == 0) {
      gint avg_bitrate = 0;
      gst_structure_get_int (structure, "bitrate", &avg_bitrate);
      qtpad->avg_bitrate = avg_bitrate;
    }
    ext_atom = build_btrt_extension (0, qtpad->avg_bitrate, qtpad->max_bitrate);
    if (ext_atom != NULL)
      ext_atom_list = g_list_prepend (ext_atom_list, ext_atom);
    ext_atom = build_codec_data_extension (FOURCC_avcC, codec_data);
    if (ext_atom != NULL)
      ext_atom_list = g_list_prepend (ext_atom_list, ext_atom);
  } else if (strcmp (mimetype, "video/x-svq") == 0) {
    gint version = 0;
    const GstBuffer *seqh = NULL;
    const GValue *seqh_value;
    gdouble gamma = 0;

    gst_structure_get_int (structure, "svqversion", &version);
    if (version == 3) {
      entry.fourcc = FOURCC_SVQ3;
      entry.version = 3;
      entry.depth = 32;

      seqh_value = gst_structure_get_value (structure, "seqh");
      if (seqh_value) {
        seqh = gst_value_get_buffer (seqh_value);
        ext_atom = build_SMI_atom (seqh);
        if (ext_atom)
          ext_atom_list = g_list_prepend (ext_atom_list, ext_atom);
      }

      /* we need to add the gamma anyway because quicktime might crash
       * when it doesn't find it */
      if (!gst_structure_get_double (structure, "applied-gamma", &gamma)) {
        /* it seems that using 0 here makes it ignored */
        gamma = 0.0;
      }
      ext_atom = build_gama_atom (gamma);
      if (ext_atom)
        ext_atom_list = g_list_prepend (ext_atom_list, ext_atom);
    } else {
      GST_WARNING_OBJECT (qtmux, "SVQ version %d not supported. Please file "
          "a bug at http://bugzilla.gnome.org", version);
    }
  } else if (strcmp (mimetype, "video/x-dv") == 0) {
    gint version = 0;
    gboolean pal = TRUE;

    sync = FALSE;
    if (framerate_num != 25 || framerate_den != 1)
      pal = FALSE;
    gst_structure_get_int (structure, "dvversion", &version);
    /* fall back to typical one */
    if (!version)
      version = 25;
    switch (version) {
      case 25:
        if (pal)
          entry.fourcc = GST_MAKE_FOURCC ('d', 'v', 'c', 'p');
        else
          entry.fourcc = GST_MAKE_FOURCC ('d', 'v', 'c', ' ');
        break;
      case 50:
        if (pal)
          entry.fourcc = GST_MAKE_FOURCC ('d', 'v', '5', 'p');
        else
          entry.fourcc = GST_MAKE_FOURCC ('d', 'v', '5', 'n');
        break;
      default:
        GST_WARNING_OBJECT (qtmux, "unrecognized dv version");
        break;
    }
  } else if (strcmp (mimetype, "image/jpeg") == 0) {
    entry.fourcc = FOURCC_jpeg;
    sync = FALSE;
  } else if (strcmp (mimetype, "image/x-j2c") == 0 ||
      strcmp (mimetype, "image/x-jpc") == 0) {
    guint32 fourcc;
    const GValue *cmap_array;
    const GValue *cdef_array;
    gint ncomp = 0;
    gint fields = 1;

    if (strcmp (mimetype, "image/x-jpc") == 0) {
      qtpad->prepare_buf_func = gst_qt_mux_prepare_jpc_buffer;
    }

    gst_structure_get_int (structure, "num-components", &ncomp);
    gst_structure_get_int (structure, "fields", &fields);
    cmap_array = gst_structure_get_value (structure, "component-map");
    cdef_array = gst_structure_get_value (structure, "channel-definitions");

    ext_atom = NULL;
    entry.fourcc = FOURCC_mjp2;
    sync = FALSE;
    if (gst_structure_get_fourcc (structure, "fourcc", &fourcc) &&
        (ext_atom =
            build_jp2h_extension (qtpad->trak, width, height, fourcc, ncomp,
                cmap_array, cdef_array)) != NULL) {
      ext_atom_list = g_list_append (ext_atom_list, ext_atom);

      ext_atom = build_fiel_extension (fields);
      if (ext_atom)
        ext_atom_list = g_list_append (ext_atom_list, ext_atom);

      ext_atom = build_jp2x_extension (codec_data);
      if (ext_atom)
        ext_atom_list = g_list_append (ext_atom_list, ext_atom);
    } else {
      GST_DEBUG_OBJECT (qtmux, "missing or invalid fourcc in jp2 caps");
      goto refuse_caps;
    }
  } else if (strcmp (mimetype, "video/x-vp8") == 0) {
    entry.fourcc = FOURCC_VP80;
    sync = FALSE;
  } else if (strcmp (mimetype, "video/x-qt-part") == 0) {
    guint32 fourcc;

    gst_structure_get_fourcc (structure, "format", &fourcc);
    entry.fourcc = fourcc;
    qtpad->have_dts = TRUE;
  } else if (strcmp (mimetype, "video/x-mp4-part") == 0) {
    guint32 fourcc;

    gst_structure_get_fourcc (structure, "format", &fourcc);
    entry.fourcc = fourcc;
    qtpad->have_dts = TRUE;
  }

  if (!entry.fourcc)
    goto refuse_caps;

  /* ok, set the pad info accordingly */
  qtpad->fourcc = entry.fourcc;
  qtpad->sync = sync;
  atom_trak_set_video_type (qtpad->trak, qtmux->context, &entry, rate,
      ext_atom_list);

  gst_object_unref (qtmux);
  return TRUE;

  /* ERRORS */
refuse_caps:
  {
    GST_WARNING_OBJECT (qtmux, "pad %s refused caps %" GST_PTR_FORMAT,
        GST_PAD_NAME (pad), caps);
    gst_object_unref (qtmux);
    return FALSE;
  }
refuse_renegotiation:
  {
    GST_WARNING_OBJECT (qtmux,
        "pad %s refused renegotiation to %" GST_PTR_FORMAT " from %"
        GST_PTR_FORMAT, GST_PAD_NAME (pad), caps, GST_PAD_CAPS (pad));
    gst_object_unref (qtmux);
    return FALSE;
  }
}

static gboolean
gst_qt_mux_sink_event (GstPad * pad, GstEvent * event)
{
  gboolean ret;
  GstQTMux *qtmux;
  guint32 avg_bitrate = 0, max_bitrate = 0;

  qtmux = GST_QT_MUX_CAST (gst_pad_get_parent (pad));
  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_TAG:{
      GstTagList *list;
      GstTagSetter *setter = GST_TAG_SETTER (qtmux);
      GstTagMergeMode mode;

      GST_OBJECT_LOCK (qtmux);
      mode = gst_tag_setter_get_tag_merge_mode (setter);

      GST_DEBUG_OBJECT (qtmux, "received tag event");
      gst_event_parse_tag (event, &list);

      gst_tag_setter_merge_tags (setter, list, mode);
      GST_OBJECT_UNLOCK (qtmux);

      if (gst_tag_list_get_uint (list, GST_TAG_BITRATE, &avg_bitrate) |
          gst_tag_list_get_uint (list, GST_TAG_MAXIMUM_BITRATE, &max_bitrate)) {
        GstQTPad *qtpad = gst_pad_get_element_private (pad);
        g_assert (qtpad);

        if (avg_bitrate > 0 && avg_bitrate < G_MAXUINT32)
          qtpad->avg_bitrate = avg_bitrate;
        if (max_bitrate > 0 && max_bitrate < G_MAXUINT32)
          qtpad->max_bitrate = max_bitrate;
      }

      break;
    }
    default:
      break;
  }

  ret = qtmux->collect_event (pad, event);
  gst_object_unref (qtmux);

  return ret;
}

static void
gst_qt_mux_release_pad (GstElement * element, GstPad * pad)
{
  GstQTMux *mux = GST_QT_MUX_CAST (element);
  GSList *walk;

  GST_DEBUG_OBJECT (element, "Releasing %s:%s", GST_DEBUG_PAD_NAME (pad));

  for (walk = mux->sinkpads; walk; walk = g_slist_next (walk)) {
    GstQTPad *qtpad = (GstQTPad *) walk->data;
    GST_DEBUG ("Checking %s:%s", GST_DEBUG_PAD_NAME (qtpad->collect.pad));
    if (qtpad->collect.pad == pad) {
      /* this is it, remove */
      mux->sinkpads = g_slist_delete_link (mux->sinkpads, walk);
      gst_element_remove_pad (element, pad);
      break;
    }
  }

  gst_collect_pads_remove_pad (mux->collect, pad);
}

static GstPad *
gst_qt_mux_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * req_name)
{
  GstElementClass *klass = GST_ELEMENT_GET_CLASS (element);
  GstQTMux *qtmux = GST_QT_MUX_CAST (element);
  GstQTPad *collect_pad;
  GstPad *newpad;
  gboolean audio;
  gchar *name;

  if (templ->direction != GST_PAD_SINK)
    goto wrong_direction;

  if (qtmux->state > GST_QT_MUX_STATE_STARTED)
    goto too_late;

  if (templ == gst_element_class_get_pad_template (klass, "audio_%d")) {
    audio = TRUE;
    name = g_strdup_printf ("audio_%02d", qtmux->audio_pads++);
  } else if (templ == gst_element_class_get_pad_template (klass, "video_%d")) {
    audio = FALSE;
    name = g_strdup_printf ("video_%02d", qtmux->video_pads++);
  } else
    goto wrong_template;

  GST_DEBUG_OBJECT (qtmux, "Requested pad: %s", name);

  /* create pad and add to collections */
  newpad = gst_pad_new_from_template (templ, name);
  g_free (name);
  collect_pad = (GstQTPad *)
      gst_collect_pads_add_pad_full (qtmux->collect, newpad, sizeof (GstQTPad),
      (GstCollectDataDestroyNotify) (gst_qt_mux_pad_reset));
  /* set up pad */
  gst_qt_mux_pad_reset (collect_pad);
  collect_pad->trak = atom_trak_new (qtmux->context);
  atom_moov_add_trak (qtmux->moov, collect_pad->trak);

  qtmux->sinkpads = g_slist_append (qtmux->sinkpads, collect_pad);

  /* set up pad functions */
  if (audio)
    gst_pad_set_setcaps_function (newpad,
        GST_DEBUG_FUNCPTR (gst_qt_mux_audio_sink_set_caps));
  else
    gst_pad_set_setcaps_function (newpad,
        GST_DEBUG_FUNCPTR (gst_qt_mux_video_sink_set_caps));

  /* FIXME: hacked way to override/extend the event function of
   * GstCollectPads; because it sets its own event function giving the
   * element no access to events.
   */
  qtmux->collect_event = (GstPadEventFunction) GST_PAD_EVENTFUNC (newpad);
  gst_pad_set_event_function (newpad,
      GST_DEBUG_FUNCPTR (gst_qt_mux_sink_event));

  gst_pad_set_active (newpad, TRUE);
  gst_element_add_pad (element, newpad);

  return newpad;

  /* ERRORS */
wrong_direction:
  {
    GST_WARNING_OBJECT (qtmux, "Request pad that is not a SINK pad.");
    return NULL;
  }
too_late:
  {
    GST_WARNING_OBJECT (qtmux, "Not providing request pad after stream start.");
    return NULL;
  }
wrong_template:
  {
    GST_WARNING_OBJECT (qtmux, "This is not our template!");
    return NULL;
  }
}

static void
gst_qt_mux_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  GstQTMux *qtmux = GST_QT_MUX_CAST (object);

  GST_OBJECT_LOCK (qtmux);
  switch (prop_id) {
    case PROP_MOVIE_TIMESCALE:
      g_value_set_uint (value, qtmux->timescale);
      break;
    case PROP_TRAK_TIMESCALE:
      g_value_set_uint (value, qtmux->trak_timescale);
      break;
    case PROP_DO_CTTS:
      g_value_set_boolean (value, qtmux->guess_pts);
      break;
    case PROP_DTS_METHOD:
      g_value_set_enum (value, qtmux->dts_method);
      break;
    case PROP_FAST_START:
      g_value_set_boolean (value, qtmux->fast_start);
      break;
    case PROP_FAST_START_TEMP_FILE:
      g_value_set_string (value, qtmux->fast_start_file_path);
      break;
    case PROP_MOOV_RECOV_FILE:
      g_value_set_string (value, qtmux->moov_recov_file_path);
      break;
    case PROP_FRAGMENT_DURATION:
      g_value_set_uint (value, qtmux->fragment_duration);
      break;
    case PROP_STREAMABLE:
      g_value_set_boolean (value, qtmux->streamable);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
  GST_OBJECT_UNLOCK (qtmux);
}

static void
gst_qt_mux_generate_fast_start_file_path (GstQTMux * qtmux)
{
  gchar *tmp;

  g_free (qtmux->fast_start_file_path);
  qtmux->fast_start_file_path = NULL;

  tmp = g_strdup_printf ("%s%d", "qtmux", g_random_int ());
  qtmux->fast_start_file_path = g_build_filename (g_get_tmp_dir (), tmp, NULL);
  g_free (tmp);
}

static void
gst_qt_mux_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec)
{
  GstQTMux *qtmux = GST_QT_MUX_CAST (object);

  GST_OBJECT_LOCK (qtmux);
  switch (prop_id) {
    case PROP_MOVIE_TIMESCALE:
      qtmux->timescale = g_value_get_uint (value);
      break;
    case PROP_TRAK_TIMESCALE:
      qtmux->trak_timescale = g_value_get_uint (value);
      break;
    case PROP_DO_CTTS:
      qtmux->guess_pts = g_value_get_boolean (value);
      break;
    case PROP_DTS_METHOD:
      qtmux->dts_method = g_value_get_enum (value);
      break;
    case PROP_FAST_START:
      qtmux->fast_start = g_value_get_boolean (value);
      break;
    case PROP_FAST_START_TEMP_FILE:
      g_free (qtmux->fast_start_file_path);
      qtmux->fast_start_file_path = g_value_dup_string (value);
      /* NULL means to generate a random one */
      if (!qtmux->fast_start_file_path) {
        gst_qt_mux_generate_fast_start_file_path (qtmux);
      }
      break;
    case PROP_MOOV_RECOV_FILE:
      g_free (qtmux->moov_recov_file_path);
      qtmux->moov_recov_file_path = g_value_dup_string (value);
      break;
    case PROP_FRAGMENT_DURATION:
      qtmux->fragment_duration = g_value_get_uint (value);
      break;
    case PROP_STREAMABLE:
      qtmux->streamable = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
  GST_OBJECT_UNLOCK (qtmux);
}

static GstStateChangeReturn
gst_qt_mux_change_state (GstElement * element, GstStateChange transition)
{
  GstStateChangeReturn ret;
  GstQTMux *qtmux = GST_QT_MUX_CAST (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      gst_collect_pads_start (qtmux->collect);
      qtmux->state = GST_QT_MUX_STATE_STARTED;
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      gst_collect_pads_stop (qtmux->collect);
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      gst_qt_mux_reset (qtmux, TRUE);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      break;
    default:
      break;
  }

  return ret;
}

gboolean
gst_qt_mux_register (GstPlugin * plugin)
{
  GTypeInfo typeinfo = {
    sizeof (GstQTMuxClass),
    (GBaseInitFunc) gst_qt_mux_base_init,
    NULL,
    (GClassInitFunc) gst_qt_mux_class_init,
    NULL,
    NULL,
    sizeof (GstQTMux),
    0,
    (GInstanceInitFunc) gst_qt_mux_init,
  };
  static const GInterfaceInfo tag_setter_info = {
    NULL, NULL, NULL
  };
  static const GInterfaceInfo tag_xmp_writer_info = {
    NULL, NULL, NULL
  };
  GType type;
  GstQTMuxFormat format;
  GstQTMuxClassParams *params;
  guint i = 0;

  GST_DEBUG_CATEGORY_INIT (gst_qt_mux_debug, "qtmux", 0, "QT Muxer");

  GST_LOG ("Registering muxers");

  while (TRUE) {
    GstQTMuxFormatProp *prop;

    prop = &gst_qt_mux_format_list[i];
    format = prop->format;
    if (format == GST_QT_MUX_FORMAT_NONE)
      break;

    /* create a cache for these properties */
    params = g_new0 (GstQTMuxClassParams, 1);
    params->prop = prop;
    params->src_caps = gst_static_caps_get (&prop->src_caps);
    params->video_sink_caps = gst_static_caps_get (&prop->video_sink_caps);
    params->audio_sink_caps = gst_static_caps_get (&prop->audio_sink_caps);

    /* create the type now */
    type = g_type_register_static (GST_TYPE_ELEMENT, prop->type_name, &typeinfo,
        0);
    g_type_set_qdata (type, GST_QT_MUX_PARAMS_QDATA, (gpointer) params);
    g_type_add_interface_static (type, GST_TYPE_TAG_SETTER, &tag_setter_info);
    g_type_add_interface_static (type, GST_TYPE_TAG_XMP_WRITER,
        &tag_xmp_writer_info);

    if (!gst_element_register (plugin, prop->name, prop->rank, type))
      return FALSE;

    i++;
  }

  GST_LOG ("Finished registering muxers");

  /* FIXME: ideally classification tag should be added and
     registered in gstreamer core gsttaglist
   */

  GST_LOG ("Registering tags");

  gst_tag_register (GST_TAG_3GP_CLASSIFICATION, GST_TAG_FLAG_META,
      G_TYPE_STRING, GST_TAG_3GP_CLASSIFICATION, "content classification",
      gst_tag_merge_use_first);

  GST_LOG ("Finished registering tags");

  return TRUE;
}
