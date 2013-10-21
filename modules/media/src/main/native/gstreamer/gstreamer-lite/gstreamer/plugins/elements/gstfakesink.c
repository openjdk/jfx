/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *
 * gstfakesink.c:
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
 * SECTION:element-fakesink
 * @see_also: #GstFakeSrc
 *
 * Dummy sink that swallows everything.
 * 
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch audiotestsrc num-buffers=1000 ! fakesink sync=false
 * ]| Render 1000 audio buffers (of default size) as fast as possible.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include "gstfakesink.h"
#include <gst/gstmarshal.h>
#include <string.h>

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

GST_DEBUG_CATEGORY_STATIC (gst_fake_sink_debug);
#define GST_CAT_DEFAULT gst_fake_sink_debug

/* FakeSink signals and args */
enum
{
  /* FILL ME */
  SIGNAL_HANDOFF,
  SIGNAL_PREROLL_HANDOFF,
  LAST_SIGNAL
};

#define DEFAULT_SYNC FALSE

#define DEFAULT_STATE_ERROR FAKE_SINK_STATE_ERROR_NONE
#define DEFAULT_SILENT FALSE
#define DEFAULT_DUMP FALSE
#define DEFAULT_SIGNAL_HANDOFFS FALSE
#define DEFAULT_LAST_MESSAGE NULL
#define DEFAULT_CAN_ACTIVATE_PUSH TRUE
#define DEFAULT_CAN_ACTIVATE_PULL FALSE
#define DEFAULT_NUM_BUFFERS -1

enum
{
  PROP_0,
  PROP_STATE_ERROR,
  PROP_SILENT,
  PROP_DUMP,
  PROP_SIGNAL_HANDOFFS,
  PROP_LAST_MESSAGE,
  PROP_CAN_ACTIVATE_PUSH,
  PROP_CAN_ACTIVATE_PULL,
  PROP_NUM_BUFFERS
};

#define GST_TYPE_FAKE_SINK_STATE_ERROR (gst_fake_sink_state_error_get_type())
static GType
gst_fake_sink_state_error_get_type (void)
{
  static GType fakesink_state_error_type = 0;
  static const GEnumValue fakesink_state_error[] = {
    {FAKE_SINK_STATE_ERROR_NONE, "No state change errors", "none"},
    {FAKE_SINK_STATE_ERROR_NULL_READY,
        "Fail state change from NULL to READY", "null-to-ready"},
    {FAKE_SINK_STATE_ERROR_READY_PAUSED,
        "Fail state change from READY to PAUSED", "ready-to-paused"},
    {FAKE_SINK_STATE_ERROR_PAUSED_PLAYING,
        "Fail state change from PAUSED to PLAYING", "paused-to-playing"},
    {FAKE_SINK_STATE_ERROR_PLAYING_PAUSED,
        "Fail state change from PLAYING to PAUSED", "playing-to-paused"},
    {FAKE_SINK_STATE_ERROR_PAUSED_READY,
        "Fail state change from PAUSED to READY", "paused-to-ready"},
    {FAKE_SINK_STATE_ERROR_READY_NULL,
        "Fail state change from READY to NULL", "ready-to-null"},
    {0, NULL, NULL},
  };

  if (!fakesink_state_error_type) {
    fakesink_state_error_type =
        g_enum_register_static ("GstFakeSinkStateError", fakesink_state_error);
  }
  return fakesink_state_error_type;
}

#define _do_init(bla) \
    GST_DEBUG_CATEGORY_INIT (gst_fake_sink_debug, "fakesink", 0, "fakesink element");

GST_BOILERPLATE_FULL (GstFakeSink, gst_fake_sink, GstBaseSink,
    GST_TYPE_BASE_SINK, _do_init);

static void gst_fake_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_fake_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);
static void gst_fake_sink_finalize (GObject * obj);

static GstStateChangeReturn gst_fake_sink_change_state (GstElement * element,
    GstStateChange transition);

static GstFlowReturn gst_fake_sink_preroll (GstBaseSink * bsink,
    GstBuffer * buffer);
static GstFlowReturn gst_fake_sink_render (GstBaseSink * bsink,
    GstBuffer * buffer);
static gboolean gst_fake_sink_event (GstBaseSink * bsink, GstEvent * event);

static guint gst_fake_sink_signals[LAST_SIGNAL] = { 0 };

static GParamSpec *pspec_last_message = NULL;

static void
marshal_VOID__MINIOBJECT_OBJECT (GClosure * closure, GValue * return_value,
    guint n_param_values, const GValue * param_values, gpointer invocation_hint,
    gpointer marshal_data)
{
  typedef void (*marshalfunc_VOID__MINIOBJECT_OBJECT) (gpointer obj,
      gpointer arg1, gpointer arg2, gpointer data2);
  register marshalfunc_VOID__MINIOBJECT_OBJECT callback;
  register GCClosure *cc = (GCClosure *) closure;
  register gpointer data1, data2;

  g_return_if_fail (n_param_values == 3);

  if (G_CCLOSURE_SWAP_DATA (closure)) {
    data1 = closure->data;
    data2 = g_value_peek_pointer (param_values + 0);
  } else {
    data1 = g_value_peek_pointer (param_values + 0);
    data2 = closure->data;
  }
  callback =
      (marshalfunc_VOID__MINIOBJECT_OBJECT) (marshal_data ? marshal_data :
      cc->callback);

  callback (data1, gst_value_get_mini_object (param_values + 1),
      g_value_get_object (param_values + 2), data2);
}

static void
gst_fake_sink_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (gstelement_class,
      "Fake Sink",
      "Sink",
      "Black hole for data",
      "Erik Walthinsen <omega@cse.ogi.edu>, "
      "Wim Taymans <wim@fluendo.com>, "
      "Mr. 'frag-me-more' Vanderwingo <wingo@fluendo.com>");
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));
}

static void
gst_fake_sink_class_init (GstFakeSinkClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;
  GstBaseSinkClass *gstbase_sink_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstelement_class = GST_ELEMENT_CLASS (klass);
  gstbase_sink_class = GST_BASE_SINK_CLASS (klass);

  gobject_class->set_property = gst_fake_sink_set_property;
  gobject_class->get_property = gst_fake_sink_get_property;
  gobject_class->finalize = gst_fake_sink_finalize;

  g_object_class_install_property (gobject_class, PROP_STATE_ERROR,
      g_param_spec_enum ("state-error", "State Error",
          "Generate a state change error", GST_TYPE_FAKE_SINK_STATE_ERROR,
          DEFAULT_STATE_ERROR, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  pspec_last_message = g_param_spec_string ("last-message", "Last Message",
      "The message describing current status", DEFAULT_LAST_MESSAGE,
      G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PROP_LAST_MESSAGE,
      pspec_last_message);
  g_object_class_install_property (gobject_class, PROP_SIGNAL_HANDOFFS,
      g_param_spec_boolean ("signal-handoffs", "Signal handoffs",
          "Send a signal before unreffing the buffer", DEFAULT_SIGNAL_HANDOFFS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_SILENT,
      g_param_spec_boolean ("silent", "Silent",
          "Don't produce last_message events", DEFAULT_SILENT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_DUMP,
      g_param_spec_boolean ("dump", "Dump", "Dump buffer contents to stdout",
          DEFAULT_DUMP, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class,
      PROP_CAN_ACTIVATE_PUSH,
      g_param_spec_boolean ("can-activate-push", "Can activate push",
          "Can activate in push mode", DEFAULT_CAN_ACTIVATE_PUSH,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class,
      PROP_CAN_ACTIVATE_PULL,
      g_param_spec_boolean ("can-activate-pull", "Can activate pull",
          "Can activate in pull mode", DEFAULT_CAN_ACTIVATE_PULL,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_NUM_BUFFERS,
      g_param_spec_int ("num-buffers", "num-buffers",
          "Number of buffers to accept going EOS", -1, G_MAXINT,
          DEFAULT_NUM_BUFFERS, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstFakeSink::handoff:
   * @fakesink: the fakesink instance
   * @buffer: the buffer that just has been received
   * @pad: the pad that received it
   *
   * This signal gets emitted before unreffing the buffer.
   */
  gst_fake_sink_signals[SIGNAL_HANDOFF] =
      g_signal_new ("handoff", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstFakeSinkClass, handoff), NULL, NULL,
      marshal_VOID__MINIOBJECT_OBJECT, G_TYPE_NONE, 2,
      GST_TYPE_BUFFER, GST_TYPE_PAD);

  /**
   * GstFakeSink::preroll-handoff:
   * @fakesink: the fakesink instance
   * @buffer: the buffer that just has been received
   * @pad: the pad that received it
   *
   * This signal gets emitted before unreffing the buffer.
   *
   * Since: 0.10.7
   */
  gst_fake_sink_signals[SIGNAL_PREROLL_HANDOFF] =
      g_signal_new ("preroll-handoff", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstFakeSinkClass, preroll_handoff),
      NULL, NULL, marshal_VOID__MINIOBJECT_OBJECT, G_TYPE_NONE, 2,
      GST_TYPE_BUFFER, GST_TYPE_PAD);

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (gst_fake_sink_change_state);

  gstbase_sink_class->event = GST_DEBUG_FUNCPTR (gst_fake_sink_event);
  gstbase_sink_class->preroll = GST_DEBUG_FUNCPTR (gst_fake_sink_preroll);
  gstbase_sink_class->render = GST_DEBUG_FUNCPTR (gst_fake_sink_render);
}

static void
gst_fake_sink_init (GstFakeSink * fakesink, GstFakeSinkClass * g_class)
{
  fakesink->silent = DEFAULT_SILENT;
  fakesink->dump = DEFAULT_DUMP;
  fakesink->last_message = g_strdup (DEFAULT_LAST_MESSAGE);
  fakesink->state_error = DEFAULT_STATE_ERROR;
  fakesink->signal_handoffs = DEFAULT_SIGNAL_HANDOFFS;
  fakesink->num_buffers = DEFAULT_NUM_BUFFERS;
#if !GLIB_CHECK_VERSION(2,26,0)
  g_static_rec_mutex_init (&fakesink->notify_lock);
#endif

  gst_base_sink_set_sync (GST_BASE_SINK (fakesink), DEFAULT_SYNC);
}

static void
gst_fake_sink_finalize (GObject * obj)
{
#if !GLIB_CHECK_VERSION(2,26,0)
  GstFakeSink *sink = GST_FAKE_SINK (obj);

  g_static_rec_mutex_free (&sink->notify_lock);
#endif

  G_OBJECT_CLASS (parent_class)->finalize (obj);
}

static void
gst_fake_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstFakeSink *sink;

  sink = GST_FAKE_SINK (object);

  switch (prop_id) {
    case PROP_STATE_ERROR:
      sink->state_error = g_value_get_enum (value);
      break;
    case PROP_SILENT:
      sink->silent = g_value_get_boolean (value);
      break;
    case PROP_DUMP:
      sink->dump = g_value_get_boolean (value);
      break;
    case PROP_SIGNAL_HANDOFFS:
      sink->signal_handoffs = g_value_get_boolean (value);
      break;
    case PROP_CAN_ACTIVATE_PUSH:
      GST_BASE_SINK (sink)->can_activate_push = g_value_get_boolean (value);
      break;
    case PROP_CAN_ACTIVATE_PULL:
      GST_BASE_SINK (sink)->can_activate_pull = g_value_get_boolean (value);
      break;
    case PROP_NUM_BUFFERS:
      sink->num_buffers = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_fake_sink_get_property (GObject * object, guint prop_id, GValue * value,
    GParamSpec * pspec)
{
  GstFakeSink *sink;

  sink = GST_FAKE_SINK (object);

  switch (prop_id) {
    case PROP_STATE_ERROR:
      g_value_set_enum (value, sink->state_error);
      break;
    case PROP_SILENT:
      g_value_set_boolean (value, sink->silent);
      break;
    case PROP_DUMP:
      g_value_set_boolean (value, sink->dump);
      break;
    case PROP_SIGNAL_HANDOFFS:
      g_value_set_boolean (value, sink->signal_handoffs);
      break;
    case PROP_LAST_MESSAGE:
      GST_OBJECT_LOCK (sink);
      g_value_set_string (value, sink->last_message);
      GST_OBJECT_UNLOCK (sink);
      break;
    case PROP_CAN_ACTIVATE_PUSH:
      g_value_set_boolean (value, GST_BASE_SINK (sink)->can_activate_push);
      break;
    case PROP_CAN_ACTIVATE_PULL:
      g_value_set_boolean (value, GST_BASE_SINK (sink)->can_activate_pull);
      break;
    case PROP_NUM_BUFFERS:
      g_value_set_int (value, sink->num_buffers);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_fake_sink_notify_last_message (GstFakeSink * sink)
{
  /* FIXME: this hacks around a bug in GLib/GObject: doing concurrent
   * g_object_notify() on the same object might lead to crashes, see
   * http://bugzilla.gnome.org/show_bug.cgi?id=166020#c60 and follow-ups.
   * So we really don't want to do a g_object_notify() here for out-of-band
   * events with the streaming thread possibly also doing a g_object_notify()
   * for an in-band buffer or event. This is fixed in GLib >= 2.26 */
#if !GLIB_CHECK_VERSION(2,26,0)
  g_static_rec_mutex_lock (&sink->notify_lock);
  g_object_notify ((GObject *) sink, "last-message");
  g_static_rec_mutex_unlock (&sink->notify_lock);
#else
  g_object_notify_by_pspec ((GObject *) sink, pspec_last_message);
#endif
}

static gboolean
gst_fake_sink_event (GstBaseSink * bsink, GstEvent * event)
{
  GstFakeSink *sink = GST_FAKE_SINK (bsink);

  if (!sink->silent) {
    const GstStructure *s;
    gchar *sstr;

    GST_OBJECT_LOCK (sink);
    g_free (sink->last_message);

    if (GST_EVENT_TYPE (event) == GST_EVENT_SINK_MESSAGE) {
      GstMessage *msg;

      gst_event_parse_sink_message (event, &msg);
      sstr = gst_structure_to_string (msg->structure);
      sink->last_message =
          g_strdup_printf ("message ******* M (type: %d, %s) %p",
          GST_MESSAGE_TYPE (msg), sstr, msg);
      gst_message_unref (msg);
    } else {
      if ((s = gst_event_get_structure (event))) {
        sstr = gst_structure_to_string (s);
      } else {
        sstr = g_strdup ("");
      }

      sink->last_message =
          g_strdup_printf ("event   ******* E (type: %d, %s) %p",
          GST_EVENT_TYPE (event), sstr, event);
    }
    g_free (sstr);
    GST_OBJECT_UNLOCK (sink);

    gst_fake_sink_notify_last_message (sink);
  }

  if (GST_BASE_SINK_CLASS (parent_class)->event) {
    return GST_BASE_SINK_CLASS (parent_class)->event (bsink, event);
  } else {
    return TRUE;
  }
}

static GstFlowReturn
gst_fake_sink_preroll (GstBaseSink * bsink, GstBuffer * buffer)
{
  GstFakeSink *sink = GST_FAKE_SINK (bsink);

  if (sink->num_buffers_left == 0)
    goto eos;

  if (!sink->silent) {
    GST_OBJECT_LOCK (sink);
    g_free (sink->last_message);

    sink->last_message = g_strdup_printf ("preroll   ******* ");
    GST_OBJECT_UNLOCK (sink);

    gst_fake_sink_notify_last_message (sink);
  }
  if (sink->signal_handoffs) {
    g_signal_emit (sink,
        gst_fake_sink_signals[SIGNAL_PREROLL_HANDOFF], 0, buffer,
        bsink->sinkpad);
  }
  return GST_FLOW_OK;

  /* ERRORS */
eos:
  {
    GST_DEBUG_OBJECT (sink, "we are EOS");
    return GST_FLOW_UNEXPECTED;
  }
}

static GstFlowReturn
gst_fake_sink_render (GstBaseSink * bsink, GstBuffer * buf)
{
  GstFakeSink *sink = GST_FAKE_SINK_CAST (bsink);

  if (sink->num_buffers_left == 0)
    goto eos;

  if (sink->num_buffers_left != -1)
    sink->num_buffers_left--;

  if (!sink->silent) {
    gchar ts_str[64], dur_str[64];
    gchar flag_str[100];

    GST_OBJECT_LOCK (sink);
    g_free (sink->last_message);

    if (GST_BUFFER_TIMESTAMP (buf) != GST_CLOCK_TIME_NONE) {
      g_snprintf (ts_str, sizeof (ts_str), "%" GST_TIME_FORMAT,
          GST_TIME_ARGS (GST_BUFFER_TIMESTAMP (buf)));
    } else {
      g_strlcpy (ts_str, "none", sizeof (ts_str));
    }

    if (GST_BUFFER_DURATION (buf) != GST_CLOCK_TIME_NONE) {
      g_snprintf (dur_str, sizeof (dur_str), "%" GST_TIME_FORMAT,
          GST_TIME_ARGS (GST_BUFFER_DURATION (buf)));
    } else {
      g_strlcpy (dur_str, "none", sizeof (dur_str));
    }

    {
      const char *flag_list[12] = {
        "ro", "media4", "", "",
        "preroll", "discont", "incaps", "gap",
        "delta_unit", "media1", "media2", "media3"
      };
      int i;
      char *end = flag_str;
      end[0] = '\0';
      for (i = 0; i < 12; i++) {
        if (GST_MINI_OBJECT_CAST (buf)->flags & (1 << i)) {
          strcpy (end, flag_list[i]);
          end += strlen (end);
          end[0] = ' ';
          end[1] = '\0';
          end++;
        }
      }
    }

    sink->last_message =
        g_strdup_printf ("chain   ******* < (%5d bytes, timestamp: %s"
        ", duration: %s, offset: %" G_GINT64_FORMAT ", offset_end: %"
        G_GINT64_FORMAT ", flags: %d %s) %p", GST_BUFFER_SIZE (buf), ts_str,
        dur_str, GST_BUFFER_OFFSET (buf), GST_BUFFER_OFFSET_END (buf),
        GST_MINI_OBJECT_CAST (buf)->flags, flag_str, buf);
    GST_OBJECT_UNLOCK (sink);

    gst_fake_sink_notify_last_message (sink);
  }
  if (sink->signal_handoffs)
    g_signal_emit (sink, gst_fake_sink_signals[SIGNAL_HANDOFF], 0, buf,
        bsink->sinkpad);

  if (sink->dump) {
    gst_util_dump_mem (GST_BUFFER_DATA (buf), GST_BUFFER_SIZE (buf));
  }
  if (sink->num_buffers_left == 0)
    goto eos;

  return GST_FLOW_OK;

  /* ERRORS */
eos:
  {
    GST_DEBUG_OBJECT (sink, "we are EOS");
    return GST_FLOW_UNEXPECTED;
  }
}

static GstStateChangeReturn
gst_fake_sink_change_state (GstElement * element, GstStateChange transition)
{
  GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;
  GstFakeSink *fakesink = GST_FAKE_SINK (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      if (fakesink->state_error == FAKE_SINK_STATE_ERROR_NULL_READY)
        goto error;
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      if (fakesink->state_error == FAKE_SINK_STATE_ERROR_READY_PAUSED)
        goto error;
      fakesink->num_buffers_left = fakesink->num_buffers;
      break;
    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
      if (fakesink->state_error == FAKE_SINK_STATE_ERROR_PAUSED_PLAYING)
        goto error;
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
      if (fakesink->state_error == FAKE_SINK_STATE_ERROR_PLAYING_PAUSED)
        goto error;
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      if (fakesink->state_error == FAKE_SINK_STATE_ERROR_PAUSED_READY)
        goto error;
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      if (fakesink->state_error == FAKE_SINK_STATE_ERROR_READY_NULL)
        goto error;
      GST_OBJECT_LOCK (fakesink);
      g_free (fakesink->last_message);
      fakesink->last_message = NULL;
      GST_OBJECT_UNLOCK (fakesink);
      break;
    default:
      break;
  }

  return ret;

  /* ERROR */
error:
  GST_ELEMENT_ERROR (element, CORE, STATE_CHANGE, (NULL),
      ("Erroring out on state change as requested"));
  return GST_STATE_CHANGE_FAILURE;
}
