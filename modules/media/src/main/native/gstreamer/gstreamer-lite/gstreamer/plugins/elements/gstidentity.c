/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *
 * gstidentity.c:
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
 * SECTION:element-identity
 *
 * Dummy element that passes incomming data through unmodified. It has some
 * useful diagnostic functions, such as offset and timestamp checking.
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <stdlib.h>

#include "../../gst/gst-i18n-lib.h"
#include "gstidentity.h"
#include <gst/gstmarshal.h>

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

GST_DEBUG_CATEGORY_STATIC (gst_identity_debug);
#define GST_CAT_DEFAULT gst_identity_debug

/* Identity signals and args */
enum
{
  SIGNAL_HANDOFF,
  /* FILL ME */
  LAST_SIGNAL
};

#define DEFAULT_SLEEP_TIME              0
#define DEFAULT_DUPLICATE               1
#define DEFAULT_ERROR_AFTER             -1
#define DEFAULT_DROP_PROBABILITY        0.0
#define DEFAULT_DATARATE                0
#define DEFAULT_SILENT                  FALSE
#define DEFAULT_SINGLE_SEGMENT          FALSE
#define DEFAULT_DUMP                    FALSE
#define DEFAULT_SYNC                    FALSE
#define DEFAULT_CHECK_PERFECT           FALSE
#define DEFAULT_CHECK_IMPERFECT_TIMESTAMP FALSE
#define DEFAULT_CHECK_IMPERFECT_OFFSET    FALSE
#define DEFAULT_SIGNAL_HANDOFFS           TRUE

enum
{
  PROP_0,
  PROP_SLEEP_TIME,
  PROP_ERROR_AFTER,
  PROP_DROP_PROBABILITY,
  PROP_DATARATE,
  PROP_SILENT,
  PROP_SINGLE_SEGMENT,
  PROP_LAST_MESSAGE,
  PROP_DUMP,
  PROP_SYNC,
  PROP_CHECK_PERFECT,
  PROP_CHECK_IMPERFECT_TIMESTAMP,
  PROP_CHECK_IMPERFECT_OFFSET,
  PROP_SIGNAL_HANDOFFS
};


#define _do_init(bla) \
    GST_DEBUG_CATEGORY_INIT (gst_identity_debug, "identity", 0, "identity element");

GST_BOILERPLATE_FULL (GstIdentity, gst_identity, GstBaseTransform,
    GST_TYPE_BASE_TRANSFORM, _do_init);

static void gst_identity_finalize (GObject * object);
static void gst_identity_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_identity_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static gboolean gst_identity_event (GstBaseTransform * trans, GstEvent * event);
static GstFlowReturn gst_identity_transform_ip (GstBaseTransform * trans,
    GstBuffer * buf);
static GstFlowReturn gst_identity_prepare_output_buffer (GstBaseTransform
    * trans, GstBuffer * in_buf, gint out_size, GstCaps * out_caps,
    GstBuffer ** out_buf);
static gboolean gst_identity_start (GstBaseTransform * trans);
static gboolean gst_identity_stop (GstBaseTransform * trans);

static guint gst_identity_signals[LAST_SIGNAL] = { 0 };

static GParamSpec *pspec_last_message = NULL;

static void
gst_identity_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (gstelement_class,
      "Identity",
      "Generic",
      "Pass data without modification", "Erik Walthinsen <omega@cse.ogi.edu>");
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));
}

static void
gst_identity_finalize (GObject * object)
{
  GstIdentity *identity;

  identity = GST_IDENTITY (object);

  g_free (identity->last_message);

#if !GLIB_CHECK_VERSION(2,26,0)
  g_static_rec_mutex_free (&identity->notify_lock);
#endif

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

/* fixme: do something about this */
static void
marshal_VOID__MINIOBJECT (GClosure * closure, GValue * return_value,
    guint n_param_values, const GValue * param_values, gpointer invocation_hint,
    gpointer marshal_data)
{
  typedef void (*marshalfunc_VOID__MINIOBJECT) (gpointer obj, gpointer arg1,
      gpointer data2);
  register marshalfunc_VOID__MINIOBJECT callback;
  register GCClosure *cc = (GCClosure *) closure;
  register gpointer data1, data2;

  g_return_if_fail (n_param_values == 2);

  if (G_CCLOSURE_SWAP_DATA (closure)) {
    data1 = closure->data;
    data2 = g_value_peek_pointer (param_values + 0);
  } else {
    data1 = g_value_peek_pointer (param_values + 0);
    data2 = closure->data;
  }
  callback =
      (marshalfunc_VOID__MINIOBJECT) (marshal_data ? marshal_data :
      cc->callback);

  callback (data1, gst_value_get_mini_object (param_values + 1), data2);
}

static void
gst_identity_class_init (GstIdentityClass * klass)
{
  GObjectClass *gobject_class;
  GstBaseTransformClass *gstbasetrans_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstbasetrans_class = GST_BASE_TRANSFORM_CLASS (klass);

  gobject_class->set_property = gst_identity_set_property;
  gobject_class->get_property = gst_identity_get_property;

  g_object_class_install_property (gobject_class, PROP_SLEEP_TIME,
      g_param_spec_uint ("sleep-time", "Sleep time",
          "Microseconds to sleep between processing", 0, G_MAXUINT,
          DEFAULT_SLEEP_TIME, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_ERROR_AFTER,
      g_param_spec_int ("error-after", "Error After", "Error after N buffers",
          G_MININT, G_MAXINT, DEFAULT_ERROR_AFTER,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_DROP_PROBABILITY,
      g_param_spec_float ("drop-probability", "Drop Probability",
          "The Probability a buffer is dropped", 0.0, 1.0,
          DEFAULT_DROP_PROBABILITY,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_DATARATE,
      g_param_spec_int ("datarate", "Datarate",
          "(Re)timestamps buffers with number of bytes per second (0 = inactive)",
          0, G_MAXINT, DEFAULT_DATARATE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_SILENT,
      g_param_spec_boolean ("silent", "silent", "silent", DEFAULT_SILENT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_SINGLE_SEGMENT,
      g_param_spec_boolean ("single-segment", "Single Segment",
          "Timestamp buffers and eat newsegments so as to appear as one segment",
          DEFAULT_SINGLE_SEGMENT, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  pspec_last_message = g_param_spec_string ("last-message", "last-message",
      "last-message", NULL, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PROP_LAST_MESSAGE,
      pspec_last_message);
  g_object_class_install_property (gobject_class, PROP_DUMP,
      g_param_spec_boolean ("dump", "Dump", "Dump buffer contents to stdout",
          DEFAULT_DUMP, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_SYNC,
      g_param_spec_boolean ("sync", "Synchronize",
          "Synchronize to pipeline clock", DEFAULT_SYNC,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_CHECK_PERFECT,
      g_param_spec_boolean ("check-perfect", "Check For Perfect Stream",
          "Verify that the stream is time- and data-contiguous. "
          "This only logs in the debug log.  This will be deprecated in favor "
          "of the check-imperfect-timestamp/offset properties.",
          DEFAULT_CHECK_PERFECT, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class,
      PROP_CHECK_IMPERFECT_TIMESTAMP,
      g_param_spec_boolean ("check-imperfect-timestamp",
          "Check for discontiguous timestamps",
          "Send element messages if timestamps and durations do not match up",
          DEFAULT_CHECK_IMPERFECT_TIMESTAMP,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_CHECK_IMPERFECT_OFFSET,
      g_param_spec_boolean ("check-imperfect-offset",
          "Check for discontiguous offset",
          "Send element messages if offset and offset_end do not match up",
          DEFAULT_CHECK_IMPERFECT_OFFSET,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstIdentity:signal-handoffs
   *
   * If set to #TRUE, the identity will emit a handoff signal when handling a buffer.
   * When set to #FALSE, no signal will be emited, which might improve performance.
   *
   * Since: 0.10.16
   */
  g_object_class_install_property (gobject_class, PROP_SIGNAL_HANDOFFS,
      g_param_spec_boolean ("signal-handoffs",
          "Signal handoffs", "Send a signal before pushing the buffer",
          DEFAULT_SIGNAL_HANDOFFS, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstIdentity::handoff:
   * @identity: the identity instance
   * @buffer: the buffer that just has been received
   * @pad: the pad that received it
   *
   * This signal gets emitted before passing the buffer downstream.
   */
  gst_identity_signals[SIGNAL_HANDOFF] =
      g_signal_new ("handoff", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstIdentityClass, handoff), NULL, NULL,
      marshal_VOID__MINIOBJECT, G_TYPE_NONE, 1, GST_TYPE_BUFFER);

  gobject_class->finalize = gst_identity_finalize;

  gstbasetrans_class->event = GST_DEBUG_FUNCPTR (gst_identity_event);
  gstbasetrans_class->transform_ip =
      GST_DEBUG_FUNCPTR (gst_identity_transform_ip);
  gstbasetrans_class->prepare_output_buffer =
      GST_DEBUG_FUNCPTR (gst_identity_prepare_output_buffer);
  gstbasetrans_class->start = GST_DEBUG_FUNCPTR (gst_identity_start);
  gstbasetrans_class->stop = GST_DEBUG_FUNCPTR (gst_identity_stop);
}

static void
gst_identity_init (GstIdentity * identity, GstIdentityClass * g_class)
{
  identity->sleep_time = DEFAULT_SLEEP_TIME;
  identity->error_after = DEFAULT_ERROR_AFTER;
  identity->drop_probability = DEFAULT_DROP_PROBABILITY;
  identity->datarate = DEFAULT_DATARATE;
  identity->silent = DEFAULT_SILENT;
  identity->single_segment = DEFAULT_SINGLE_SEGMENT;
  identity->sync = DEFAULT_SYNC;
  identity->check_perfect = DEFAULT_CHECK_PERFECT;
  identity->check_imperfect_timestamp = DEFAULT_CHECK_IMPERFECT_TIMESTAMP;
  identity->check_imperfect_offset = DEFAULT_CHECK_IMPERFECT_OFFSET;
  identity->dump = DEFAULT_DUMP;
  identity->last_message = NULL;
  identity->signal_handoffs = DEFAULT_SIGNAL_HANDOFFS;

#if !GLIB_CHECK_VERSION(2,26,0)
  g_static_rec_mutex_init (&identity->notify_lock);
#endif

  gst_base_transform_set_gap_aware (GST_BASE_TRANSFORM_CAST (identity), TRUE);
}

static void
gst_identity_notify_last_message (GstIdentity * identity)
{
  /* FIXME: this hacks around a bug in GLib/GObject: doing concurrent
   * g_object_notify() on the same object might lead to crashes, see
   * http://bugzilla.gnome.org/show_bug.cgi?id=166020#c60 and follow-ups.
   * So we really don't want to do a g_object_notify() here for out-of-band
   * events with the streaming thread possibly also doing a g_object_notify()
   * for an in-band buffer or event. This is fixed in GLib >= 2.26 */
#if !GLIB_CHECK_VERSION(2,26,0)
  g_static_rec_mutex_lock (&identity->notify_lock);
  g_object_notify ((GObject *) identity, "last-message");
  g_static_rec_mutex_unlock (&identity->notify_lock);
#else
  g_object_notify_by_pspec ((GObject *) identity, pspec_last_message);
#endif
}

static gboolean
gst_identity_event (GstBaseTransform * trans, GstEvent * event)
{
  GstIdentity *identity;
  gboolean ret = TRUE;

  identity = GST_IDENTITY (trans);

  if (!identity->silent) {
    const GstStructure *s;
    gchar *sstr;

    GST_OBJECT_LOCK (identity);
    g_free (identity->last_message);

    if ((s = gst_event_get_structure (event)))
      sstr = gst_structure_to_string (s);
    else
      sstr = g_strdup ("");

    identity->last_message =
        g_strdup_printf ("event   ******* (%s:%s) E (type: %d, %s) %p",
        GST_DEBUG_PAD_NAME (trans->sinkpad), GST_EVENT_TYPE (event), sstr,
        event);
    g_free (sstr);
    GST_OBJECT_UNLOCK (identity);

    gst_identity_notify_last_message (identity);
  }

  if (identity->single_segment
      && (GST_EVENT_TYPE (event) == GST_EVENT_NEWSEGMENT)) {
    if (trans->have_newsegment == FALSE) {
      GstEvent *news;
      GstFormat format;

      gst_event_parse_new_segment (event, NULL, NULL, &format, NULL, NULL,
          NULL);

      /* This is the first newsegment, send out a (0, -1) newsegment */
      news = gst_event_new_new_segment (TRUE, 1.0, format, 0, -1, 0);

      gst_pad_event_default (trans->sinkpad, news);
    }
  }

  /* Reset previous timestamp, duration and offsets on NEWSEGMENT
   * to prevent false warnings when checking for perfect streams */
  if (GST_EVENT_TYPE (event) == GST_EVENT_NEWSEGMENT) {
    identity->prev_timestamp = identity->prev_duration = GST_CLOCK_TIME_NONE;
    identity->prev_offset = identity->prev_offset_end = GST_BUFFER_OFFSET_NONE;
  }

  ret = parent_class->event (trans, event);

  if (identity->single_segment
      && (GST_EVENT_TYPE (event) == GST_EVENT_NEWSEGMENT)) {
    /* eat up segments */
    ret = FALSE;
  }

  return ret;
}

static GstFlowReturn
gst_identity_prepare_output_buffer (GstBaseTransform * trans,
    GstBuffer * in_buf, gint out_size, GstCaps * out_caps, GstBuffer ** out_buf)
{
  GstIdentity *identity = GST_IDENTITY (trans);

  /* only bother if we may have to alter metadata */
  if (identity->datarate > 0 || identity->single_segment) {
    if (gst_buffer_is_metadata_writable (in_buf))
      *out_buf = gst_buffer_ref (in_buf);
    else {
      /* make even less writable */
      gst_buffer_ref (in_buf);
      /* extra ref is dropped going through the official process */
      *out_buf = gst_buffer_make_metadata_writable (in_buf);
    }
  } else
    *out_buf = gst_buffer_ref (in_buf);

  return GST_FLOW_OK;
}

static void
gst_identity_check_perfect (GstIdentity * identity, GstBuffer * buf)
{
  GstClockTime timestamp;

  timestamp = GST_BUFFER_TIMESTAMP (buf);

  /* see if we need to do perfect stream checking */
  /* invalid timestamp drops us out of check.  FIXME: maybe warn ? */
  if (timestamp != GST_CLOCK_TIME_NONE) {
    /* check if we had a previous buffer to compare to */
    if (identity->prev_timestamp != GST_CLOCK_TIME_NONE &&
        identity->prev_duration != GST_CLOCK_TIME_NONE) {
      guint64 offset, t_expected;
      gint64 dt;

      t_expected = identity->prev_timestamp + identity->prev_duration;
      dt = timestamp - t_expected;
      if (dt != 0) {
        GST_WARNING_OBJECT (identity,
            "Buffer not time-contiguous with previous one: " "prev ts %"
            GST_TIME_FORMAT ", prev dur %" GST_TIME_FORMAT ", new ts %"
            GST_TIME_FORMAT " (expected ts %" GST_TIME_FORMAT ", delta=%c%"
            GST_TIME_FORMAT ")", GST_TIME_ARGS (identity->prev_timestamp),
            GST_TIME_ARGS (identity->prev_duration), GST_TIME_ARGS (timestamp),
            GST_TIME_ARGS (t_expected), (dt < 0) ? '-' : '+',
            GST_TIME_ARGS ((dt < 0) ? (GstClockTime) (-dt) : dt));
      }

      offset = GST_BUFFER_OFFSET (buf);
      if (identity->prev_offset_end != offset &&
          identity->prev_offset_end != GST_BUFFER_OFFSET_NONE &&
          offset != GST_BUFFER_OFFSET_NONE) {
        GST_WARNING_OBJECT (identity,
            "Buffer not data-contiguous with previous one: "
            "prev offset_end %" G_GINT64_FORMAT ", new offset %"
            G_GINT64_FORMAT, identity->prev_offset_end, offset);
      }
    } else {
      GST_DEBUG_OBJECT (identity, "can't check time-contiguity, no timestamp "
          "and/or duration were set on previous buffer");
    }
  }
}

static void
gst_identity_check_imperfect_timestamp (GstIdentity * identity, GstBuffer * buf)
{
  GstClockTime timestamp = GST_BUFFER_TIMESTAMP (buf);

  /* invalid timestamp drops us out of check.  FIXME: maybe warn ? */
  if (timestamp != GST_CLOCK_TIME_NONE) {
    /* check if we had a previous buffer to compare to */
    if (identity->prev_timestamp != GST_CLOCK_TIME_NONE &&
        identity->prev_duration != GST_CLOCK_TIME_NONE) {
      GstClockTime t_expected;
      GstClockTimeDiff dt;

      t_expected = identity->prev_timestamp + identity->prev_duration;
      dt = GST_CLOCK_DIFF (t_expected, timestamp);
      if (dt != 0) {
        /*
         * "imperfect-timestamp" bus message:
         * @identity:        the identity instance
         * @prev-timestamp:  the previous buffer timestamp
         * @prev-duration:   the previous buffer duration
         * @prev-offset:     the previous buffer offset
         * @prev-offset-end: the previous buffer offset end
         * @cur-timestamp:   the current buffer timestamp
         * @cur-duration:    the current buffer duration
         * @cur-offset:      the current buffer offset
         * @cur-offset-end:  the current buffer offset end
         *
         * This bus message gets emitted if the check-imperfect-timestamp
         * property is set and there is a gap in time between the
         * last buffer and the newly received buffer.
         */
        gst_element_post_message (GST_ELEMENT (identity),
            gst_message_new_element (GST_OBJECT (identity),
                gst_structure_new ("imperfect-timestamp",
                    "prev-timestamp", G_TYPE_UINT64,
                    identity->prev_timestamp, "prev-duration", G_TYPE_UINT64,
                    identity->prev_duration, "prev-offset", G_TYPE_UINT64,
                    identity->prev_offset, "prev-offset-end", G_TYPE_UINT64,
                    identity->prev_offset_end, "cur-timestamp", G_TYPE_UINT64,
                    timestamp, "cur-duration", G_TYPE_UINT64,
                    GST_BUFFER_DURATION (buf), "cur-offset", G_TYPE_UINT64,
                    GST_BUFFER_OFFSET (buf), "cur-offset-end", G_TYPE_UINT64,
                    GST_BUFFER_OFFSET_END (buf), NULL)));
      }
    } else {
      GST_DEBUG_OBJECT (identity, "can't check data-contiguity, no "
          "offset_end was set on previous buffer");
    }
  }
}

static void
gst_identity_check_imperfect_offset (GstIdentity * identity, GstBuffer * buf)
{
  guint64 offset;

  offset = GST_BUFFER_OFFSET (buf);

  if (identity->prev_offset_end != offset &&
      identity->prev_offset_end != GST_BUFFER_OFFSET_NONE &&
      offset != GST_BUFFER_OFFSET_NONE) {
    /*
     * "imperfect-offset" bus message:
     * @identity:        the identity instance
     * @prev-timestamp:  the previous buffer timestamp
     * @prev-duration:   the previous buffer duration
     * @prev-offset:     the previous buffer offset
     * @prev-offset-end: the previous buffer offset end
     * @cur-timestamp:   the current buffer timestamp
     * @cur-duration:    the current buffer duration
     * @cur-offset:      the current buffer offset
     * @cur-offset-end:  the current buffer offset end
     *
     * This bus message gets emitted if the check-imperfect-offset
     * property is set and there is a gap in offsets between the
     * last buffer and the newly received buffer.
     */
    gst_element_post_message (GST_ELEMENT (identity),
        gst_message_new_element (GST_OBJECT (identity),
            gst_structure_new ("imperfect-offset", "prev-timestamp",
                G_TYPE_UINT64, identity->prev_timestamp, "prev-duration",
                G_TYPE_UINT64, identity->prev_duration, "prev-offset",
                G_TYPE_UINT64, identity->prev_offset, "prev-offset-end",
                G_TYPE_UINT64, identity->prev_offset_end, "cur-timestamp",
                G_TYPE_UINT64, GST_BUFFER_TIMESTAMP (buf), "cur-duration",
                G_TYPE_UINT64, GST_BUFFER_DURATION (buf), "cur-offset",
                G_TYPE_UINT64, GST_BUFFER_OFFSET (buf), "cur-offset-end",
                G_TYPE_UINT64, GST_BUFFER_OFFSET_END (buf), NULL)));
  } else {
    GST_DEBUG_OBJECT (identity, "can't check offset contiguity, no offset "
        "and/or offset_end were set on previous buffer");
  }
}

static const gchar *
print_pretty_time (gchar * ts_str, gsize ts_str_len, GstClockTime ts)
{
  if (ts == GST_CLOCK_TIME_NONE)
    return "none";

  g_snprintf (ts_str, ts_str_len, "%" GST_TIME_FORMAT, GST_TIME_ARGS (ts));
  return ts_str;
}

static void
gst_identity_update_last_message_for_buffer (GstIdentity * identity,
    const gchar * action, GstBuffer * buf)
{
  gchar ts_str[64], dur_str[64];

  GST_OBJECT_LOCK (identity);

  g_free (identity->last_message);
  identity->last_message = g_strdup_printf ("%s   ******* (%s:%s)i "
      "(%u bytes, timestamp: %s, duration: %s, offset: %" G_GINT64_FORMAT ", "
      "offset_end: % " G_GINT64_FORMAT ", flags: %d) %p", action,
      GST_DEBUG_PAD_NAME (GST_BASE_TRANSFORM_CAST (identity)->sinkpad),
      GST_BUFFER_SIZE (buf),
      print_pretty_time (ts_str, sizeof (ts_str), GST_BUFFER_TIMESTAMP (buf)),
      print_pretty_time (dur_str, sizeof (dur_str), GST_BUFFER_DURATION (buf)),
      GST_BUFFER_OFFSET (buf), GST_BUFFER_OFFSET_END (buf),
      GST_BUFFER_FLAGS (buf), buf);

  GST_OBJECT_UNLOCK (identity);

  gst_identity_notify_last_message (identity);
}

static GstFlowReturn
gst_identity_transform_ip (GstBaseTransform * trans, GstBuffer * buf)
{
  GstFlowReturn ret = GST_FLOW_OK;
  GstIdentity *identity = GST_IDENTITY (trans);
  GstClockTime runtimestamp = G_GINT64_CONSTANT (0);

  if (identity->check_perfect)
    gst_identity_check_perfect (identity, buf);
  if (identity->check_imperfect_timestamp)
    gst_identity_check_imperfect_timestamp (identity, buf);
  if (identity->check_imperfect_offset)
    gst_identity_check_imperfect_offset (identity, buf);

  /* update prev values */
  identity->prev_timestamp = GST_BUFFER_TIMESTAMP (buf);
  identity->prev_duration = GST_BUFFER_DURATION (buf);
  identity->prev_offset_end = GST_BUFFER_OFFSET_END (buf);
  identity->prev_offset = GST_BUFFER_OFFSET (buf);

  if (identity->error_after >= 0) {
    identity->error_after--;
    if (identity->error_after == 0) {
      GST_ELEMENT_ERROR (identity, CORE, FAILED,
          (_("Failed after iterations as requested.")), (NULL));
      return GST_FLOW_ERROR;
    }
  }

  if (identity->drop_probability > 0.0) {
    if ((gfloat) (1.0 * rand () / (RAND_MAX)) < identity->drop_probability) {
      if (!identity->silent) {
        gst_identity_update_last_message_for_buffer (identity, "dropping", buf);
      }
      /* return DROPPED to basetransform. */
      return GST_BASE_TRANSFORM_FLOW_DROPPED;
    }
  }

  if (identity->dump) {
    gst_util_dump_mem (GST_BUFFER_DATA (buf), GST_BUFFER_SIZE (buf));
  }

  if (!identity->silent) {
    gst_identity_update_last_message_for_buffer (identity, "chain", buf);
  }

  if (identity->datarate > 0) {
    GstClockTime time = gst_util_uint64_scale_int (identity->offset,
        GST_SECOND, identity->datarate);

    GST_BUFFER_TIMESTAMP (buf) = time;
    GST_BUFFER_DURATION (buf) =
        GST_BUFFER_SIZE (buf) * GST_SECOND / identity->datarate;
  }

  if (identity->signal_handoffs)
    g_signal_emit (identity, gst_identity_signals[SIGNAL_HANDOFF], 0, buf);

  if (trans->segment.format == GST_FORMAT_TIME)
    runtimestamp = gst_segment_to_running_time (&trans->segment,
        GST_FORMAT_TIME, GST_BUFFER_TIMESTAMP (buf));

  if ((identity->sync) && (trans->segment.format == GST_FORMAT_TIME)) {
    GstClock *clock;

    GST_OBJECT_LOCK (identity);
    if ((clock = GST_ELEMENT (identity)->clock)) {
      GstClockReturn cret;
      GstClockTime timestamp;

      timestamp = runtimestamp + GST_ELEMENT (identity)->base_time;

      /* save id if we need to unlock */
      /* FIXME: actually unlock this somewhere in the state changes */
      identity->clock_id = gst_clock_new_single_shot_id (clock, timestamp);
      GST_OBJECT_UNLOCK (identity);

      cret = gst_clock_id_wait (identity->clock_id, NULL);

      GST_OBJECT_LOCK (identity);
      if (identity->clock_id) {
        gst_clock_id_unref (identity->clock_id);
        identity->clock_id = NULL;
      }
      if (cret == GST_CLOCK_UNSCHEDULED)
        ret = GST_FLOW_UNEXPECTED;
    }
    GST_OBJECT_UNLOCK (identity);
  }

  identity->offset += GST_BUFFER_SIZE (buf);

  if (identity->sleep_time && ret == GST_FLOW_OK)
    g_usleep (identity->sleep_time);

  if (identity->single_segment && (trans->segment.format == GST_FORMAT_TIME)
      && (ret == GST_FLOW_OK)) {
    GST_BUFFER_TIMESTAMP (buf) = runtimestamp;
    GST_BUFFER_OFFSET (buf) = GST_CLOCK_TIME_NONE;
    GST_BUFFER_OFFSET_END (buf) = GST_CLOCK_TIME_NONE;
  }

  return ret;
}

static void
gst_identity_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstIdentity *identity;

  identity = GST_IDENTITY (object);

  switch (prop_id) {
    case PROP_SLEEP_TIME:
      identity->sleep_time = g_value_get_uint (value);
      break;
    case PROP_SILENT:
      identity->silent = g_value_get_boolean (value);
      break;
    case PROP_SINGLE_SEGMENT:
      identity->single_segment = g_value_get_boolean (value);
      break;
    case PROP_DUMP:
      identity->dump = g_value_get_boolean (value);
      break;
    case PROP_ERROR_AFTER:
      identity->error_after = g_value_get_int (value);
      break;
    case PROP_DROP_PROBABILITY:
      identity->drop_probability = g_value_get_float (value);
      break;
    case PROP_DATARATE:
      identity->datarate = g_value_get_int (value);
      break;
    case PROP_SYNC:
      identity->sync = g_value_get_boolean (value);
      break;
    case PROP_CHECK_PERFECT:
      identity->check_perfect = g_value_get_boolean (value);
      break;
    case PROP_CHECK_IMPERFECT_TIMESTAMP:
      identity->check_imperfect_timestamp = g_value_get_boolean (value);
      break;
    case PROP_CHECK_IMPERFECT_OFFSET:
      identity->check_imperfect_offset = g_value_get_boolean (value);
      break;
    case PROP_SIGNAL_HANDOFFS:
      identity->signal_handoffs = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_identity_get_property (GObject * object, guint prop_id, GValue * value,
    GParamSpec * pspec)
{
  GstIdentity *identity;

  identity = GST_IDENTITY (object);

  switch (prop_id) {
    case PROP_SLEEP_TIME:
      g_value_set_uint (value, identity->sleep_time);
      break;
    case PROP_ERROR_AFTER:
      g_value_set_int (value, identity->error_after);
      break;
    case PROP_DROP_PROBABILITY:
      g_value_set_float (value, identity->drop_probability);
      break;
    case PROP_DATARATE:
      g_value_set_int (value, identity->datarate);
      break;
    case PROP_SILENT:
      g_value_set_boolean (value, identity->silent);
      break;
    case PROP_SINGLE_SEGMENT:
      g_value_set_boolean (value, identity->single_segment);
      break;
    case PROP_DUMP:
      g_value_set_boolean (value, identity->dump);
      break;
    case PROP_LAST_MESSAGE:
      GST_OBJECT_LOCK (identity);
      g_value_set_string (value, identity->last_message);
      GST_OBJECT_UNLOCK (identity);
      break;
    case PROP_SYNC:
      g_value_set_boolean (value, identity->sync);
      break;
    case PROP_CHECK_PERFECT:
      g_value_set_boolean (value, identity->check_perfect);
      break;
    case PROP_CHECK_IMPERFECT_TIMESTAMP:
      g_value_set_boolean (value, identity->check_imperfect_timestamp);
      break;
    case PROP_CHECK_IMPERFECT_OFFSET:
      g_value_set_boolean (value, identity->check_imperfect_offset);
      break;
    case PROP_SIGNAL_HANDOFFS:
      g_value_set_boolean (value, identity->signal_handoffs);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static gboolean
gst_identity_start (GstBaseTransform * trans)
{
  GstIdentity *identity;

  identity = GST_IDENTITY (trans);

  identity->offset = 0;
  identity->prev_timestamp = GST_CLOCK_TIME_NONE;
  identity->prev_duration = GST_CLOCK_TIME_NONE;
  identity->prev_offset_end = GST_BUFFER_OFFSET_NONE;
  identity->prev_offset = GST_BUFFER_OFFSET_NONE;

  return TRUE;
}

static gboolean
gst_identity_stop (GstBaseTransform * trans)
{
  GstIdentity *identity;

  identity = GST_IDENTITY (trans);

  GST_OBJECT_LOCK (identity);
  g_free (identity->last_message);
  identity->last_message = NULL;
  GST_OBJECT_UNLOCK (identity);

  return TRUE;
}
