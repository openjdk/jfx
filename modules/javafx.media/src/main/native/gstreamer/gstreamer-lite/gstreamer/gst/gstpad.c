/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gstpad.c: Pads for linking elements together
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
 * SECTION:gstpad
 * @title: GstPad
 * @short_description: Object contained by elements that allows links to
 *                     other elements
 * @see_also: #GstPadTemplate, #GstElement, #GstEvent, #GstQuery, #GstBuffer
 *
 * A #GstElement is linked to other elements via "pads", which are extremely
 * light-weight generic link points.
 *
 * Pads have a #GstPadDirection, source pads produce data, sink pads consume
 * data.
 *
 * Pads are typically created from a #GstPadTemplate with
 * gst_pad_new_from_template() and are then added to a #GstElement. This usually
 * happens when the element is created but it can also happen dynamically based
 * on the data that the element is processing or based on the pads that the
 * application requests.
 *
 * Pads without pad templates can be created with gst_pad_new(),
 * which takes a direction and a name as an argument.  If the name is %NULL,
 * then a guaranteed unique name will be assigned to it.
 *
 * A #GstElement creating a pad will typically use the various
 * gst_pad_set_*_function\() calls to register callbacks for events, queries or
 * dataflow on the pads.
 *
 * gst_pad_get_parent() will retrieve the #GstElement that owns the pad.
 *
 * After two pads are retrieved from an element by gst_element_get_static_pad(),
 * the pads can be linked with gst_pad_link(). (For quick links,
 * you can also use gst_element_link(), which will make the obvious
 * link for you if it's straightforward.). Pads can be unlinked again with
 * gst_pad_unlink(). gst_pad_get_peer() can be used to check what the pad is
 * linked to.
 *
 * Before dataflow is possible on the pads, they need to be activated with
 * gst_pad_set_active().
 *
 * gst_pad_query() and gst_pad_peer_query() can be used to query various
 * properties of the pad and the stream.
 *
 * To send a #GstEvent on a pad, use gst_pad_send_event() and
 * gst_pad_push_event(). Some events will be sticky on the pad, meaning that
 * after they pass on the pad they can be queried later with
 * gst_pad_get_sticky_event() and gst_pad_sticky_events_foreach().
 * gst_pad_get_current_caps() and gst_pad_has_current_caps() are convenience
 * functions to query the current sticky CAPS event on a pad.
 *
 * GstElements will use gst_pad_push() and gst_pad_pull_range() to push out
 * or pull in a buffer.
 *
 * The dataflow, events and queries that happen on a pad can be monitored with
 * probes that can be installed with gst_pad_add_probe(). gst_pad_is_blocked()
 * can be used to check if a block probe is installed on the pad.
 * gst_pad_is_blocking() checks if the blocking probe is currently blocking the
 * pad. gst_pad_remove_probe() is used to remove a previously installed probe
 * and unblock blocking probes if any.
 *
 * Pad have an offset that can be retrieved with gst_pad_get_offset(). This
 * offset will be applied to the running_time of all data passing over the pad.
 * gst_pad_set_offset() can be used to change the offset.
 *
 * Convenience functions exist to start, pause and stop the task on a pad with
 * gst_pad_start_task(), gst_pad_pause_task() and gst_pad_stop_task()
 * respectively.
 */

#include "gst_private.h"

#include "gstpad.h"
#include "gstpadtemplate.h"
#include "gstenumtypes.h"
#include "gstutils.h"
#include "gstinfo.h"
#include "gsterror.h"
#include "gsttracerutils.h"
#include "gstvalue.h"
#include "glib-compat-private.h"

GST_DEBUG_CATEGORY_STATIC (debug_dataflow);
#define GST_CAT_DEFAULT GST_CAT_PADS

/* Pad signals and args */
enum
{
  PAD_LINKED,
  PAD_UNLINKED,
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PAD_PROP_0,
  PAD_PROP_CAPS,
  PAD_PROP_DIRECTION,
  PAD_PROP_TEMPLATE,
  PAD_PROP_OFFSET
      /* FILL ME */
};

#define _PAD_PROBE_TYPE_ALL_BOTH_AND_FLUSH (GST_PAD_PROBE_TYPE_ALL_BOTH | GST_PAD_PROBE_TYPE_EVENT_FLUSH)

/* we have a pending and an active event on the pad. On source pads only the
 * active event is used. On sinkpads, events are copied to the pending entry and
 * moved to the active event when the eventfunc returned %TRUE. */
typedef struct
{
  gboolean received;
  GstEvent *event;
} PadEvent;

struct _GstPadPrivate
{
  guint events_cookie;
  GArray *events;
  guint last_cookie;

  gint using;
  guint probe_list_cookie;

  /* counter of how many idle probes are running directly from the add_probe
   * call. Used to block any data flowing in the pad while the idle callback
   * Doesn't finish its work */
  gint idle_running;

  /* conditional and variable used to ensure pads only get (de)activated
   * by a single thread at a time. Protected by the object lock */
  GCond activation_cond;
  gboolean in_activation;
};

typedef struct
{
  GHook hook;
} GstProbe;

#define GST_PAD_IS_RUNNING_IDLE_PROBE(p) \
    (((GstPad *)(p))->priv->idle_running > 0)

typedef struct
{
  GstPad *pad;
  GstPadProbeInfo *info;
  gboolean dropped;
  gboolean pass;
  gboolean handled;
  gboolean marshalled;

  gulong *called_probes;
  guint n_called_probes;
  guint called_probes_size;
  gboolean retry;
} ProbeMarshall;

static void gst_pad_dispose (GObject * object);
static void gst_pad_finalize (GObject * object);
static void gst_pad_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_pad_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static void gst_pad_set_pad_template (GstPad * pad, GstPadTemplate * templ);
static gboolean gst_pad_activate_default (GstPad * pad, GstObject * parent);
static GstFlowReturn gst_pad_chain_list_default (GstPad * pad,
    GstObject * parent, GstBufferList * list);

static GstFlowReturn gst_pad_send_event_unchecked (GstPad * pad,
    GstEvent * event, GstPadProbeType type);
static GstFlowReturn gst_pad_push_event_unchecked (GstPad * pad,
    GstEvent * event, GstPadProbeType type);

static gboolean activate_mode_internal (GstPad * pad, GstObject * parent,
    GstPadMode mode, gboolean active);

static guint gst_pad_signals[LAST_SIGNAL] = { 0 };

static GParamSpec *pspec_caps = NULL;

/* quarks for probe signals */
static GQuark buffer_quark;
static GQuark buffer_list_quark;
static GQuark event_quark;

typedef struct
{
  const gint ret;
  const gchar *name;
  GQuark quark;
} GstFlowQuarks;

static GstFlowQuarks flow_quarks[] = {
  {GST_FLOW_CUSTOM_SUCCESS, "custom-success", 0},
  {GST_FLOW_OK, "ok", 0},
  {GST_FLOW_NOT_LINKED, "not-linked", 0},
  {GST_FLOW_FLUSHING, "flushing", 0},
  {GST_FLOW_EOS, "eos", 0},
  {GST_FLOW_NOT_NEGOTIATED, "not-negotiated", 0},
  {GST_FLOW_ERROR, "error", 0},
  {GST_FLOW_NOT_SUPPORTED, "not-supported", 0},
  {GST_FLOW_CUSTOM_ERROR, "custom-error", 0}
};

/**
 * gst_flow_get_name:
 * @ret: a #GstFlowReturn to get the name of.
 *
 * Gets a string representing the given flow return.
 *
 * Returns: a static string with the name of the flow return.
 */
const gchar *
gst_flow_get_name (GstFlowReturn ret)
{
  gint i;

  ret = CLAMP (ret, GST_FLOW_CUSTOM_ERROR, GST_FLOW_CUSTOM_SUCCESS);

  for (i = 0; i < G_N_ELEMENTS (flow_quarks); i++) {
    if (ret == flow_quarks[i].ret)
      return flow_quarks[i].name;
  }
  return "unknown";
}

/**
 * gst_flow_to_quark:
 * @ret: a #GstFlowReturn to get the quark of.
 *
 * Get the unique quark for the given GstFlowReturn.
 *
 * Returns: the quark associated with the flow return or 0 if an
 * invalid return was specified.
 */
GQuark
gst_flow_to_quark (GstFlowReturn ret)
{
  gint i;

  ret = CLAMP (ret, GST_FLOW_CUSTOM_ERROR, GST_FLOW_CUSTOM_SUCCESS);

  for (i = 0; i < G_N_ELEMENTS (flow_quarks); i++) {
    if (ret == flow_quarks[i].ret)
      return flow_quarks[i].quark;
  }
  return 0;
}

/**
 * gst_pad_link_get_name:
 * @ret: a #GstPadLinkReturn to get the name of.
 *
 * Gets a string representing the given pad-link return.
 *
 * Returns: a static string with the name of the pad-link return.
 *
 * Since: 1.4
 */
const gchar *
gst_pad_link_get_name (GstPadLinkReturn ret)
{
  switch (ret) {
    case GST_PAD_LINK_OK:
      return "ok";
    case GST_PAD_LINK_WRONG_HIERARCHY:
      return "wrong hierarchy";
    case GST_PAD_LINK_WAS_LINKED:
      return "was linked";
    case GST_PAD_LINK_WRONG_DIRECTION:
      return "wrong direction";
    case GST_PAD_LINK_NOFORMAT:
      return "no common format";
    case GST_PAD_LINK_NOSCHED:
      return "incompatible scheduling";
    case GST_PAD_LINK_REFUSED:
      return "refused";
  }
  g_return_val_if_reached ("unknown");
}

#define _do_init \
{ \
  gint i; \
  \
  buffer_quark = g_quark_from_static_string ("buffer"); \
  buffer_list_quark = g_quark_from_static_string ("bufferlist"); \
  event_quark = g_quark_from_static_string ("event"); \
  \
  for (i = 0; i < G_N_ELEMENTS (flow_quarks); i++) {      \
    flow_quarks[i].quark = g_quark_from_static_string (flow_quarks[i].name); \
  } \
  \
  GST_DEBUG_CATEGORY_INIT (debug_dataflow, "GST_DATAFLOW", \
      GST_DEBUG_BOLD | GST_DEBUG_FG_GREEN, "dataflow inside pads"); \
}

#define gst_pad_parent_class parent_class
G_DEFINE_TYPE_WITH_CODE (GstPad, gst_pad, GST_TYPE_OBJECT,
    G_ADD_PRIVATE (GstPad) _do_init);

static void
gst_pad_class_init (GstPadClass * klass)
{
  GObjectClass *gobject_class;
  GstObjectClass *gstobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstobject_class = GST_OBJECT_CLASS (klass);

  gobject_class->dispose = gst_pad_dispose;
  gobject_class->finalize = gst_pad_finalize;
  gobject_class->set_property = gst_pad_set_property;
  gobject_class->get_property = gst_pad_get_property;

  /**
   * GstPad::linked:
   * @pad: the pad that emitted the signal
   * @peer: the peer pad that has been connected
   *
   * Signals that a pad has been linked to the peer pad.
   */
  gst_pad_signals[PAD_LINKED] =
      g_signal_new ("linked", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstPadClass, linked), NULL, NULL,
      NULL, G_TYPE_NONE, 1, GST_TYPE_PAD);
  /**
   * GstPad::unlinked:
   * @pad: the pad that emitted the signal
   * @peer: the peer pad that has been disconnected
   *
   * Signals that a pad has been unlinked from the peer pad.
   */
  gst_pad_signals[PAD_UNLINKED] =
      g_signal_new ("unlinked", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (GstPadClass, unlinked), NULL, NULL,
      NULL, G_TYPE_NONE, 1, GST_TYPE_PAD);

  pspec_caps = g_param_spec_boxed ("caps", "Caps",
      "The capabilities of the pad", GST_TYPE_CAPS,
      G_PARAM_READABLE | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PAD_PROP_CAPS, pspec_caps);

  g_object_class_install_property (gobject_class, PAD_PROP_DIRECTION,
      g_param_spec_enum ("direction", "Direction", "The direction of the pad",
          GST_TYPE_PAD_DIRECTION, GST_PAD_UNKNOWN,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS));

  /* FIXME, Make G_PARAM_CONSTRUCT_ONLY when we fix ghostpads. */
  g_object_class_install_property (gobject_class, PAD_PROP_TEMPLATE,
      g_param_spec_object ("template", "Template",
          "The GstPadTemplate of this pad", GST_TYPE_PAD_TEMPLATE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstPad:offset:
   *
   * The offset that will be applied to the running time of the pad.
   *
   * Since: 1.6
   */
  g_object_class_install_property (gobject_class, PAD_PROP_OFFSET,
      g_param_spec_int64 ("offset", "Offset",
          "The running time offset of the pad", 0, G_MAXINT64, 0,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstobject_class->path_string_separator = ".";

  /* Register common function pointer descriptions */
  GST_DEBUG_REGISTER_FUNCPTR (gst_pad_activate_default);
  GST_DEBUG_REGISTER_FUNCPTR (gst_pad_event_default);
  GST_DEBUG_REGISTER_FUNCPTR (gst_pad_query_default);
  GST_DEBUG_REGISTER_FUNCPTR (gst_pad_iterate_internal_links_default);
  GST_DEBUG_REGISTER_FUNCPTR (gst_pad_chain_list_default);
}

static void
gst_pad_init (GstPad * pad)
{
  pad->priv = gst_pad_get_instance_private (pad);

  GST_PAD_DIRECTION (pad) = GST_PAD_UNKNOWN;

  GST_PAD_ACTIVATEFUNC (pad) = gst_pad_activate_default;
  GST_PAD_EVENTFUNC (pad) = gst_pad_event_default;
  GST_PAD_QUERYFUNC (pad) = gst_pad_query_default;
  GST_PAD_ITERINTLINKFUNC (pad) = gst_pad_iterate_internal_links_default;
  GST_PAD_CHAINLISTFUNC (pad) = gst_pad_chain_list_default;

  GST_PAD_SET_FLUSHING (pad);

  g_rec_mutex_init (&pad->stream_rec_lock);

  g_cond_init (&pad->block_cond);

  g_hook_list_init (&pad->probes, sizeof (GstProbe));

  pad->priv->events = g_array_sized_new (FALSE, TRUE, sizeof (PadEvent), 16);
  pad->priv->events_cookie = 0;
  pad->priv->last_cookie = -1;
  g_cond_init (&pad->priv->activation_cond);

  pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
}

/* called when setting the pad inactive. It removes all sticky events from
 * the pad. must be called with object lock */
static void
remove_events (GstPad * pad)
{
  guint i, len;
  GArray *events;
  gboolean notify = FALSE;

  events = pad->priv->events;

  len = events->len;
  for (i = 0; i < len; i++) {
    PadEvent *ev = &g_array_index (events, PadEvent, i);
    GstEvent *event = ev->event;

    ev->event = NULL;

    if (event && GST_EVENT_TYPE (event) == GST_EVENT_CAPS)
      notify = TRUE;

    gst_event_unref (event);
  }

  GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_PENDING_EVENTS);
  g_array_set_size (events, 0);
  pad->priv->events_cookie++;

  if (notify) {
    GST_OBJECT_UNLOCK (pad);

    GST_DEBUG_OBJECT (pad, "notify caps");
    g_object_notify_by_pspec ((GObject *) pad, pspec_caps);

    GST_OBJECT_LOCK (pad);
  }
}

/* should be called with object lock */
static PadEvent *
find_event_by_type (GstPad * pad, GstEventType type, guint idx)
{
  guint i, len;
  GArray *events;
  PadEvent *ev;

  events = pad->priv->events;
  len = events->len;

  for (i = 0; i < len; i++) {
    ev = &g_array_index (events, PadEvent, i);
    if (ev->event == NULL)
      continue;

    if (GST_EVENT_TYPE (ev->event) == type) {
      if (idx == 0)
        goto found;
      idx--;
    } else if (GST_EVENT_TYPE (ev->event) > type) {
      break;
    }
  }
  ev = NULL;
found:
  return ev;
}

/* should be called with OBJECT lock */
static PadEvent *
find_event (GstPad * pad, GstEvent * event)
{
  guint i, len;
  GArray *events;
  PadEvent *ev;

  events = pad->priv->events;
  len = events->len;

  for (i = 0; i < len; i++) {
    ev = &g_array_index (events, PadEvent, i);
    if (event == ev->event)
      goto found;
    else if (GST_EVENT_TYPE (ev->event) > GST_EVENT_TYPE (event))
      break;
  }
  ev = NULL;
found:
  return ev;
}

/* should be called with OBJECT lock */
static void
remove_event_by_type (GstPad * pad, GstEventType type)
{
  guint i, len;
  GArray *events;
  PadEvent *ev;

  events = pad->priv->events;
  len = events->len;

  i = 0;
  while (i < len) {
    ev = &g_array_index (events, PadEvent, i);
    if (ev->event == NULL)
      goto next;

    if (GST_EVENT_TYPE (ev->event) > type)
      break;
    else if (GST_EVENT_TYPE (ev->event) != type)
      goto next;

    gst_event_unref (ev->event);
    g_array_remove_index (events, i);
    len--;
    pad->priv->events_cookie++;
    continue;

  next:
    i++;
  }
}

/* check all events on srcpad against those on sinkpad. All events that are not
 * on sinkpad are marked as received=%FALSE and the PENDING_EVENTS is set on the
 * srcpad so that the events will be sent next time */
/* should be called with srcpad and sinkpad LOCKS */
static void
schedule_events (GstPad * srcpad, GstPad * sinkpad)
{
  gint i, len;
  GArray *events;
  PadEvent *ev;
  gboolean pending = FALSE;

  events = srcpad->priv->events;
  len = events->len;

  for (i = 0; i < len; i++) {
    ev = &g_array_index (events, PadEvent, i);
    if (ev->event == NULL)
      continue;

    if (sinkpad == NULL || !find_event (sinkpad, ev->event)) {
      ev->received = FALSE;
      pending = TRUE;
    }
  }
  if (pending)
    GST_OBJECT_FLAG_SET (srcpad, GST_PAD_FLAG_PENDING_EVENTS);
}

typedef gboolean (*PadEventFunction) (GstPad * pad, PadEvent * ev,
    gpointer user_data);

/* should be called with pad LOCK */
static void
events_foreach (GstPad * pad, PadEventFunction func, gpointer user_data)
{
  guint i, len;
  GArray *events;
  gboolean ret;
  guint cookie;

  events = pad->priv->events;

restart:
  cookie = pad->priv->events_cookie;
  i = 0;
  len = events->len;
  while (i < len) {
    PadEvent *ev, ev_ret;

    ev = &g_array_index (events, PadEvent, i);
    if (G_UNLIKELY (ev->event == NULL))
      goto next;

    /* take additional ref, func might release the lock */
    ev_ret.event = gst_event_ref (ev->event);
    ev_ret.received = ev->received;

    ret = func (pad, &ev_ret, user_data);

    /* recheck the cookie, lock might have been released and the list could have
     * changed */
    if (G_UNLIKELY (cookie != pad->priv->events_cookie)) {
      if (G_LIKELY (ev_ret.event))
        gst_event_unref (ev_ret.event);
      goto restart;
    }

    /* store the received state */
    ev->received = ev_ret.received;

    /* if the event changed, we need to do something */
    if (G_UNLIKELY (ev->event != ev_ret.event)) {
      if (G_UNLIKELY (ev_ret.event == NULL)) {
        /* function unreffed and set the event to NULL, remove it */
        gst_event_unref (ev->event);
        g_array_remove_index (events, i);
        len--;
        cookie = ++pad->priv->events_cookie;
        continue;
      } else {
        /* function gave a new event for us */
        gst_event_take (&ev->event, ev_ret.event);
      }
    } else {
      /* just unref, nothing changed */
      gst_event_unref (ev_ret.event);
    }
    if (!ret)
      break;
  next:
    i++;
  }
}

/* should be called with LOCK */
static GstEvent *
_apply_pad_offset (GstPad * pad, GstEvent * event, gboolean upstream,
    gint64 pad_offset)
{
  gint64 offset;

  GST_DEBUG_OBJECT (pad, "apply pad offset %" GST_STIME_FORMAT,
      GST_STIME_ARGS (pad_offset));

  if (GST_EVENT_TYPE (event) == GST_EVENT_SEGMENT) {
    GstSegment segment;

    g_assert (!upstream);

    /* copy segment values */
    gst_event_copy_segment (event, &segment);
    gst_event_unref (event);

    gst_segment_offset_running_time (&segment, segment.format, pad_offset);
    event = gst_event_new_segment (&segment);
  }

  event = gst_event_make_writable (event);
  offset = gst_event_get_running_time_offset (event);
  if (upstream)
    offset -= pad_offset;
  else
    offset += pad_offset;
  gst_event_set_running_time_offset (event, offset);

  return event;
}

static inline GstEvent *
apply_pad_offset (GstPad * pad, GstEvent * event, gboolean upstream)
{
  if (G_UNLIKELY (pad->offset != 0))
    return _apply_pad_offset (pad, event, upstream, pad->offset);
  return event;
}

/* should be called with the OBJECT_LOCK */
static GstCaps *
get_pad_caps (GstPad * pad)
{
  GstCaps *caps = NULL;
  PadEvent *ev;

  ev = find_event_by_type (pad, GST_EVENT_CAPS, 0);
  if (ev && ev->event)
    gst_event_parse_caps (ev->event, &caps);

  return caps;
}

static void
gst_pad_dispose (GObject * object)
{
  GstPad *pad = GST_PAD_CAST (object);
  GstPad *peer;

  GST_CAT_DEBUG_OBJECT (GST_CAT_REFCOUNTING, pad, "%p dispose", pad);

  /* unlink the peer pad */
  if ((peer = gst_pad_get_peer (pad))) {
    /* window for MT unsafeness, someone else could unlink here
     * and then we call unlink with wrong pads. The unlink
     * function would catch this and safely return failed. */
    if (GST_PAD_IS_SRC (pad))
      gst_pad_unlink (pad, peer);
    else
      gst_pad_unlink (peer, pad);

    gst_object_unref (peer);
  }

  gst_pad_set_pad_template (pad, NULL);

  GST_OBJECT_LOCK (pad);
  remove_events (pad);
  GST_OBJECT_UNLOCK (pad);

  g_hook_list_clear (&pad->probes);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
gst_pad_finalize (GObject * object)
{
  GstPad *pad = GST_PAD_CAST (object);
  GstTask *task;

  /* in case the task is still around, clean it up */
  if ((task = GST_PAD_TASK (pad))) {
    gst_task_join (task);
    GST_PAD_TASK (pad) = NULL;
    gst_object_unref (task);
  }

  if (pad->activatenotify)
    pad->activatenotify (pad->activatedata);
  if (pad->activatemodenotify)
    pad->activatemodenotify (pad->activatemodedata);
  if (pad->linknotify)
    pad->linknotify (pad->linkdata);
  if (pad->unlinknotify)
    pad->unlinknotify (pad->unlinkdata);
  if (pad->chainnotify)
    pad->chainnotify (pad->chaindata);
  if (pad->chainlistnotify)
    pad->chainlistnotify (pad->chainlistdata);
  if (pad->getrangenotify)
    pad->getrangenotify (pad->getrangedata);
  if (pad->eventnotify)
    pad->eventnotify (pad->eventdata);
  if (pad->querynotify)
    pad->querynotify (pad->querydata);
  if (pad->iterintlinknotify)
    pad->iterintlinknotify (pad->iterintlinkdata);

  g_rec_mutex_clear (&pad->stream_rec_lock);
  g_cond_clear (&pad->block_cond);
  g_cond_clear (&pad->priv->activation_cond);
  g_array_free (pad->priv->events, TRUE);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
gst_pad_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  g_return_if_fail (GST_IS_PAD (object));

  switch (prop_id) {
    case PAD_PROP_DIRECTION:
      GST_PAD_DIRECTION (object) = (GstPadDirection) g_value_get_enum (value);
      break;
    case PAD_PROP_TEMPLATE:
      gst_pad_set_pad_template (GST_PAD_CAST (object),
          (GstPadTemplate *) g_value_get_object (value));
      break;
    case PAD_PROP_OFFSET:
      gst_pad_set_offset (GST_PAD_CAST (object), g_value_get_int64 (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_pad_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  g_return_if_fail (GST_IS_PAD (object));

  switch (prop_id) {
    case PAD_PROP_CAPS:
      GST_OBJECT_LOCK (object);
      g_value_set_boxed (value, get_pad_caps (GST_PAD_CAST (object)));
      GST_OBJECT_UNLOCK (object);
      break;
    case PAD_PROP_DIRECTION:
      g_value_set_enum (value, GST_PAD_DIRECTION (object));
      break;
    case PAD_PROP_TEMPLATE:
      g_value_set_object (value, GST_PAD_PAD_TEMPLATE (object));
      break;
    case PAD_PROP_OFFSET:
      g_value_set_int64 (value, gst_pad_get_offset (GST_PAD_CAST (object)));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/**
 * gst_pad_new:
 * @name: (allow-none): the name of the new pad.
 * @direction: the #GstPadDirection of the pad.
 *
 * Creates a new pad with the given name in the given direction.
 * If name is %NULL, a guaranteed unique name (across all pads)
 * will be assigned.
 * This function makes a copy of the name so you can safely free the name.
 *
 * Returns: (transfer floating): a new #GstPad.
 *
 * MT safe.
 */
GstPad *
gst_pad_new (const gchar * name, GstPadDirection direction)
{
  return g_object_new (GST_TYPE_PAD,
      "name", name, "direction", direction, NULL);
}

/**
 * gst_pad_new_from_template:
 * @templ: the pad template to use
 * @name: (allow-none): the name of the pad
 *
 * Creates a new pad with the given name from the given template.
 * If name is %NULL, a guaranteed unique name (across all pads)
 * will be assigned.
 * This function makes a copy of the name so you can safely free the name.
 *
 * Returns: (transfer floating): a new #GstPad.
 */
GstPad *
gst_pad_new_from_template (GstPadTemplate * templ, const gchar * name)
{
  GType pad_type =
      GST_PAD_TEMPLATE_GTYPE (templ) ==
      G_TYPE_NONE ? GST_TYPE_PAD : GST_PAD_TEMPLATE_GTYPE (templ);

  g_return_val_if_fail (GST_IS_PAD_TEMPLATE (templ), NULL);

  return g_object_new (pad_type,
      "name", name, "direction", templ->direction, "template", templ, NULL);
}

/**
 * gst_pad_new_from_static_template:
 * @templ: the #GstStaticPadTemplate to use
 * @name: the name of the pad
 *
 * Creates a new pad with the given name from the given static template.
 * If name is %NULL, a guaranteed unique name (across all pads)
 * will be assigned.
 * This function makes a copy of the name so you can safely free the name.
 *
 * Returns: (transfer floating): a new #GstPad.
 */
GstPad *
gst_pad_new_from_static_template (GstStaticPadTemplate * templ,
    const gchar * name)
{
  GstPad *pad;
  GstPadTemplate *template;

  template = gst_static_pad_template_get (templ);
  pad = gst_pad_new_from_template (template, name);
  gst_object_unref (template);
  return pad;
}

#define ACQUIRE_PARENT(pad, parent, label)                      \
  G_STMT_START {                                                \
    if (G_LIKELY ((parent = GST_OBJECT_PARENT (pad))))          \
      gst_object_ref (parent);                                  \
    else if (G_LIKELY (GST_PAD_NEEDS_PARENT (pad)))             \
      goto label;                                               \
  } G_STMT_END

#define RELEASE_PARENT(parent)                                  \
  G_STMT_START {                                                \
    if (G_LIKELY (parent))                                      \
      gst_object_unref (parent);                                \
  } G_STMT_END

/**
 * gst_pad_get_direction:
 * @pad: a #GstPad to get the direction of.
 *
 * Gets the direction of the pad. The direction of the pad is
 * decided at construction time so this function does not take
 * the LOCK.
 *
 * Returns: the #GstPadDirection of the pad.
 *
 * MT safe.
 */
GstPadDirection
gst_pad_get_direction (GstPad * pad)
{
  GstPadDirection result;

  /* PAD_UNKNOWN is a little silly but we need some sort of
   * error return value */
  g_return_val_if_fail (GST_IS_PAD (pad), GST_PAD_UNKNOWN);

  result = GST_PAD_DIRECTION (pad);

  return result;
}

static gboolean
gst_pad_activate_default (GstPad * pad, GstObject * parent)
{
  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  return activate_mode_internal (pad, parent, GST_PAD_MODE_PUSH, TRUE);
}

/**
 * gst_pad_mode_get_name:
 * @mode: the pad mode
 *
 * Return the name of a pad mode, for use in debug messages mostly.
 *
 * Returns: short mnemonic for pad mode @mode
 */
const gchar *
gst_pad_mode_get_name (GstPadMode mode)
{
  switch (mode) {
    case GST_PAD_MODE_NONE:
      return "none";
    case GST_PAD_MODE_PUSH:
      return "push";
    case GST_PAD_MODE_PULL:
      return "pull";
    default:
      break;
  }
  return "unknown";
}

/* Returns TRUE if pad wasn't already in the new_mode */
static gboolean
pre_activate (GstPad * pad, GstPadMode new_mode)
{
  switch (new_mode) {
    case GST_PAD_MODE_NONE:
      GST_OBJECT_LOCK (pad);
      while (G_UNLIKELY (pad->priv->in_activation))
        g_cond_wait (&pad->priv->activation_cond, GST_OBJECT_GET_LOCK (pad));
      if (new_mode == GST_PAD_MODE (pad)) {
        GST_WARNING_OBJECT (pad,
            "Pad is already in the process of being deactivated");
        GST_OBJECT_UNLOCK (pad);
        return FALSE;
      }
      pad->priv->in_activation = TRUE;
      GST_DEBUG_OBJECT (pad, "setting PAD_MODE NONE, set flushing");
      GST_PAD_SET_FLUSHING (pad);
      pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
      GST_PAD_MODE (pad) = new_mode;
      /* unlock blocked pads so element can resume and stop */
      GST_PAD_BLOCK_BROADCAST (pad);
      GST_OBJECT_UNLOCK (pad);
      break;
    case GST_PAD_MODE_PUSH:
    case GST_PAD_MODE_PULL:
      GST_OBJECT_LOCK (pad);
      while (G_UNLIKELY (pad->priv->in_activation))
        g_cond_wait (&pad->priv->activation_cond, GST_OBJECT_GET_LOCK (pad));
      if (new_mode == GST_PAD_MODE (pad)) {
        GST_WARNING_OBJECT (pad,
            "Pad is already in the process of being activated");
        GST_OBJECT_UNLOCK (pad);
        return FALSE;
      }
      pad->priv->in_activation = TRUE;
      GST_DEBUG_OBJECT (pad, "setting pad into %s mode, unset flushing",
          gst_pad_mode_get_name (new_mode));
      GST_PAD_UNSET_FLUSHING (pad);
      pad->ABI.abi.last_flowret = GST_FLOW_OK;
      GST_PAD_MODE (pad) = new_mode;
      if (GST_PAD_IS_SINK (pad)) {
        GstPad *peer;
        /* make sure the peer src pad sends us all events */
        if ((peer = GST_PAD_PEER (pad))) {
          gst_object_ref (peer);
          GST_OBJECT_UNLOCK (pad);

          GST_DEBUG_OBJECT (pad, "reschedule events on peer %s:%s",
              GST_DEBUG_PAD_NAME (peer));

          GST_OBJECT_LOCK (peer);
          schedule_events (peer, NULL);
          GST_OBJECT_UNLOCK (peer);

          gst_object_unref (peer);
        } else {
          GST_OBJECT_UNLOCK (pad);
        }
      } else {
        GST_OBJECT_UNLOCK (pad);
      }
      break;
  }
  return TRUE;
}

static void
post_activate (GstPad * pad, GstPadMode new_mode)
{
  switch (new_mode) {
    case GST_PAD_MODE_NONE:
      GST_OBJECT_LOCK (pad);
      pad->priv->in_activation = FALSE;
      g_cond_broadcast (&pad->priv->activation_cond);
      GST_OBJECT_UNLOCK (pad);

      /* ensures that streaming stops */
      GST_PAD_STREAM_LOCK (pad);
      GST_DEBUG_OBJECT (pad, "stopped streaming");
      GST_OBJECT_LOCK (pad);
      remove_events (pad);
      GST_OBJECT_UNLOCK (pad);
      GST_PAD_STREAM_UNLOCK (pad);
      break;
    case GST_PAD_MODE_PUSH:
    case GST_PAD_MODE_PULL:
      GST_OBJECT_LOCK (pad);
      pad->priv->in_activation = FALSE;
      g_cond_broadcast (&pad->priv->activation_cond);
      GST_OBJECT_UNLOCK (pad);
      /* NOP */
      break;
  }
}

/**
 * gst_pad_set_active:
 * @pad: the #GstPad to activate or deactivate.
 * @active: whether or not the pad should be active.
 *
 * Activates or deactivates the given pad.
 * Normally called from within core state change functions.
 *
 * If @active, makes sure the pad is active. If it is already active, either in
 * push or pull mode, just return. Otherwise dispatches to the pad's activate
 * function to perform the actual activation.
 *
 * If not @active, calls gst_pad_activate_mode() with the pad's current mode
 * and a %FALSE argument.
 *
 * Returns: %TRUE if the operation was successful.
 *
 * MT safe.
 */
gboolean
gst_pad_set_active (GstPad * pad, gboolean active)
{
  GstObject *parent;
  GstPadMode old;
  gboolean ret = FALSE;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);
  old = GST_PAD_MODE (pad);
  ACQUIRE_PARENT (pad, parent, no_parent);
  GST_OBJECT_UNLOCK (pad);

  if (active) {
    if (old == GST_PAD_MODE_NONE) {
      GST_DEBUG_OBJECT (pad, "activating pad from none");
      ret = (GST_PAD_ACTIVATEFUNC (pad)) (pad, parent);
      if (ret)
        pad->ABI.abi.last_flowret = GST_FLOW_OK;
    } else {
      GST_DEBUG_OBJECT (pad, "pad was active in %s mode",
          gst_pad_mode_get_name (old));
      ret = TRUE;
    }
  } else {
    if (old == GST_PAD_MODE_NONE) {
      GST_DEBUG_OBJECT (pad, "pad was inactive");
      ret = TRUE;
    } else {
      GST_DEBUG_OBJECT (pad, "deactivating pad from %s mode",
          gst_pad_mode_get_name (old));
      ret = activate_mode_internal (pad, parent, old, FALSE);
      if (ret)
        pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
    }
  }

  RELEASE_PARENT (parent);

  if (G_UNLIKELY (!ret))
    goto failed;

  return ret;

  /* ERRORS */
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "no parent");
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
failed:
  {
    GST_OBJECT_LOCK (pad);
    if (!active) {
      g_critical ("Failed to deactivate pad %s:%s, very bad",
          GST_DEBUG_PAD_NAME (pad));
    } else {
      GST_WARNING_OBJECT (pad, "Failed to activate pad");
    }
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
}

static gboolean
activate_mode_internal (GstPad * pad, GstObject * parent, GstPadMode mode,
    gboolean active)
{
  gboolean res = FALSE;
  GstPadMode old, new;
  GstPadDirection dir;
  GstPad *peer;

  GST_OBJECT_LOCK (pad);
  old = GST_PAD_MODE (pad);
  dir = GST_PAD_DIRECTION (pad);
  GST_OBJECT_UNLOCK (pad);

  new = active ? mode : GST_PAD_MODE_NONE;

  if (old == new)
    goto was_ok;

  if (active && old != mode && old != GST_PAD_MODE_NONE) {
    /* pad was activate in the wrong direction, deactivate it
     * and reactivate it in the requested mode */
    GST_DEBUG_OBJECT (pad, "deactivating pad from %s mode",
        gst_pad_mode_get_name (old));

    if (G_UNLIKELY (!activate_mode_internal (pad, parent, old, FALSE)))
      goto deactivate_failed;
    old = GST_PAD_MODE_NONE;
  }

  switch (mode) {
    case GST_PAD_MODE_PULL:
    {
      if (dir == GST_PAD_SINK) {
        if ((peer = gst_pad_get_peer (pad))) {
          GST_DEBUG_OBJECT (pad, "calling peer");
          if (G_UNLIKELY (!gst_pad_activate_mode (peer, mode, active)))
            goto peer_failed;
          gst_object_unref (peer);
        } else {
          /* there is no peer, this is only fatal when we activate. When we
           * deactivate, we must assume the application has unlinked the peer and
           * will deactivate it eventually. */
          if (active)
            goto not_linked;
          else
            GST_DEBUG_OBJECT (pad, "deactivating unlinked pad");
        }
      } else {
        if (G_UNLIKELY (GST_PAD_GETRANGEFUNC (pad) == NULL))
          goto failure;         /* Can't activate pull on a src without a
                                   getrange function */
      }
      break;
    }
    default:
      break;
  }

  /* Mark pad as needing reconfiguration */
  if (active)
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_NEED_RECONFIGURE);

  /* pre_activate returns TRUE if we weren't already in the process of
   * switching to the 'new' mode */
  if (pre_activate (pad, new)) {

    if (GST_PAD_ACTIVATEMODEFUNC (pad)) {
      if (G_UNLIKELY (!GST_PAD_ACTIVATEMODEFUNC (pad) (pad, parent, mode,
                  active)))
        goto failure;
    } else {
      /* can happen for sinks of passthrough elements */
    }

    post_activate (pad, new);
  }

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "%s in %s mode",
      active ? "activated" : "deactivated", gst_pad_mode_get_name (mode));

exit_success:
  res = TRUE;

  /* Clear sticky flags on deactivation */
  if (!active) {
    GST_OBJECT_LOCK (pad);
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_NEED_RECONFIGURE);
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_EOS);
    GST_OBJECT_UNLOCK (pad);
  }

exit:
  return res;

was_ok:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "already %s in %s mode",
        active ? "activated" : "deactivated", gst_pad_mode_get_name (mode));
    goto exit_success;
  }
deactivate_failed:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad,
        "failed to %s in switch to %s mode from %s mode",
        (active ? "activate" : "deactivate"), gst_pad_mode_get_name (mode),
        gst_pad_mode_get_name (old));
    goto exit;
  }
peer_failed:
  {
    GST_OBJECT_LOCK (peer);
    GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad,
        "activate_mode on peer (%s:%s) failed", GST_DEBUG_PAD_NAME (peer));
    GST_OBJECT_UNLOCK (peer);
    gst_object_unref (peer);
    goto exit;
  }
not_linked:
  {
    GST_CAT_INFO_OBJECT (GST_CAT_PADS, pad, "can't activate unlinked sink "
        "pad in pull mode");
    goto exit;
  }
failure:
  {
    GST_OBJECT_LOCK (pad);
    GST_CAT_INFO_OBJECT (GST_CAT_PADS, pad, "failed to %s in %s mode",
        active ? "activate" : "deactivate", gst_pad_mode_get_name (mode));
    GST_PAD_SET_FLUSHING (pad);
    GST_PAD_MODE (pad) = old;
    pad->priv->in_activation = FALSE;
    g_cond_broadcast (&pad->priv->activation_cond);
    GST_OBJECT_UNLOCK (pad);
    goto exit;
  }
}

/**
 * gst_pad_activate_mode:
 * @pad: the #GstPad to activate or deactivate.
 * @mode: the requested activation mode
 * @active: whether or not the pad should be active.
 *
 * Activates or deactivates the given pad in @mode via dispatching to the
 * pad's activatemodefunc. For use from within pad activation functions only.
 *
 * If you don't know what this is, you probably don't want to call it.
 *
 * Returns: %TRUE if the operation was successful.
 *
 * MT safe.
 */
gboolean
gst_pad_activate_mode (GstPad * pad, GstPadMode mode, gboolean active)
{
  GstObject *parent;
  gboolean res;
  GstPadMode old, new;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);

  old = GST_PAD_MODE (pad);
  new = active ? mode : GST_PAD_MODE_NONE;
  if (old == new)
    goto was_ok;

  ACQUIRE_PARENT (pad, parent, no_parent);

  GST_OBJECT_UNLOCK (pad);

  res = activate_mode_internal (pad, parent, mode, active);

  RELEASE_PARENT (parent);

  return res;

was_ok:
  {
    GST_OBJECT_UNLOCK (pad);
    GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "already %s in %s mode",
        active ? "activated" : "deactivated", gst_pad_mode_get_name (mode));
    return TRUE;
  }
no_parent:
  {
    GST_WARNING_OBJECT (pad, "no parent");
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
}

/**
 * gst_pad_is_active:
 * @pad: the #GstPad to query
 *
 * Query if a pad is active
 *
 * Returns: %TRUE if the pad is active.
 *
 * MT safe.
 */
gboolean
gst_pad_is_active (GstPad * pad)
{
  gboolean result = FALSE;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);
  result = GST_PAD_IS_ACTIVE (pad);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

static void
cleanup_hook (GstPad * pad, GHook * hook)
{
  GstPadProbeType type;

  GST_DEBUG_OBJECT (pad,
      "cleaning up hook %lu with flags %08x", hook->hook_id, hook->flags);

  if (!G_HOOK_IS_VALID (hook))
    return;

  type = (hook->flags) >> G_HOOK_FLAG_USER_SHIFT;

  if (type & GST_PAD_PROBE_TYPE_BLOCKING) {
    /* unblock when we remove the last blocking probe */
    pad->num_blocked--;
    GST_DEBUG_OBJECT (pad, "remove blocking probe, now %d left",
        pad->num_blocked);

    /* Might have new probes now that want to be called */
    GST_PAD_BLOCK_BROADCAST (pad);

    if (pad->num_blocked == 0) {
      GST_DEBUG_OBJECT (pad, "last blocking probe removed, unblocking");
      GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_BLOCKED);
    }
  }
  g_hook_destroy_link (&pad->probes, hook);
  pad->num_probes--;
}

/**
 * gst_pad_add_probe:
 * @pad: the #GstPad to add the probe to
 * @mask: the probe mask
 * @callback: #GstPadProbeCallback that will be called with notifications of
 *           the pad state
 * @user_data: (closure): user data passed to the callback
 * @destroy_data: #GDestroyNotify for user_data
 *
 * Be notified of different states of pads. The provided callback is called for
 * every state that matches @mask.
 *
 * Probes are called in groups: First GST_PAD_PROBE_TYPE_BLOCK probes are
 * called, then others, then finally GST_PAD_PROBE_TYPE_IDLE. The only
 * exception here are GST_PAD_PROBE_TYPE_IDLE probes that are called
 * immediately if the pad is already idle while calling gst_pad_add_probe().
 * In each of the groups, probes are called in the order in which they were
 * added.
 *
 * Returns: an id or 0 if no probe is pending. The id can be used to remove the
 * probe with gst_pad_remove_probe(). When using GST_PAD_PROBE_TYPE_IDLE it can
 * happen that the probe can be run immediately and if the probe returns
 * GST_PAD_PROBE_REMOVE this functions returns 0.
 *
 * MT safe.
 */
gulong
gst_pad_add_probe (GstPad * pad, GstPadProbeType mask,
    GstPadProbeCallback callback, gpointer user_data,
    GDestroyNotify destroy_data)
{
  GHook *hook;
  gulong res;

  g_return_val_if_fail (GST_IS_PAD (pad), 0);
  g_return_val_if_fail (mask != 0, 0);

  GST_OBJECT_LOCK (pad);

  /* make a new probe */
  hook = g_hook_alloc (&pad->probes);

  GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "adding probe for mask 0x%08x",
      mask);

  /* when no constraints are given for the types, assume all types are
   * acceptable */
  if ((mask & _PAD_PROBE_TYPE_ALL_BOTH_AND_FLUSH) == 0)
    mask |= GST_PAD_PROBE_TYPE_ALL_BOTH;
  if ((mask & GST_PAD_PROBE_TYPE_SCHEDULING) == 0)
    mask |= GST_PAD_PROBE_TYPE_SCHEDULING;

  /* store our flags and other fields */
  hook->flags |= (mask << G_HOOK_FLAG_USER_SHIFT);
  hook->func = callback;
  hook->data = user_data;
  hook->destroy = destroy_data;

  /* add the probe */
  g_hook_append (&pad->probes, hook);
  pad->num_probes++;
  /* incremenent cookie so that the new hook gets called */
  pad->priv->probe_list_cookie++;

  /* get the id of the hook, we return this and it can be used to remove the
   * probe later */
  res = hook->hook_id;

  GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "got probe id %lu", res);

  if (mask & GST_PAD_PROBE_TYPE_BLOCKING) {
    /* we have a block probe */
    pad->num_blocked++;
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_BLOCKED);
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "added blocking probe, "
        "now %d blocking probes", pad->num_blocked);

    /* Might have new probes now that want to be called */
    GST_PAD_BLOCK_BROADCAST (pad);
  }

  /* call the callback if we need to be called for idle callbacks */
  if ((mask & GST_PAD_PROBE_TYPE_IDLE) && (callback != NULL)) {
    if (pad->priv->using > 0) {
      /* the pad is in use, we can't signal the idle callback yet. Since we set the
       * flag above, the last thread to leave the push will do the callback. New
       * threads going into the push will block. */
      GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
          "pad is in use, delay idle callback");
      GST_OBJECT_UNLOCK (pad);
    } else {
      GstPadProbeInfo info = { GST_PAD_PROBE_TYPE_IDLE, res, };
      GstPadProbeReturn ret;

      /* Keep another ref, the callback could destroy the pad */
      gst_object_ref (pad);
      pad->priv->idle_running++;

      /* the pad is idle now, we can signal the idle callback now */
      GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
          "pad is idle, trigger idle callback");
      GST_OBJECT_UNLOCK (pad);

      ret = callback (pad, &info, user_data);

      GST_OBJECT_LOCK (pad);
      switch (ret) {
        case GST_PAD_PROBE_REMOVE:
          /* remove the probe */
          GST_DEBUG_OBJECT (pad, "asked to remove hook");
          cleanup_hook (pad, hook);
          res = 0;
          break;
        case GST_PAD_PROBE_DROP:
          GST_DEBUG_OBJECT (pad, "asked to drop item");
          break;
        case GST_PAD_PROBE_PASS:
          GST_DEBUG_OBJECT (pad, "asked to pass item");
          break;
        case GST_PAD_PROBE_OK:
          GST_DEBUG_OBJECT (pad, "probe returned OK");
          break;
        case GST_PAD_PROBE_HANDLED:
          GST_DEBUG_OBJECT (pad, "probe handled the data");
          break;
        default:
          GST_DEBUG_OBJECT (pad, "probe returned %d", ret);
          break;
      }
      pad->priv->idle_running--;
      if (pad->priv->idle_running == 0) {
        GST_PAD_BLOCK_BROADCAST (pad);
      }
      GST_OBJECT_UNLOCK (pad);

      gst_object_unref (pad);
    }
  } else {
    GST_OBJECT_UNLOCK (pad);
  }
  return res;
}

/**
 * gst_pad_remove_probe:
 * @pad: the #GstPad with the probe
 * @id: the probe id to remove
 *
 * Remove the probe with @id from @pad.
 *
 * MT safe.
 */
void
gst_pad_remove_probe (GstPad * pad, gulong id)
{
  GHook *hook;

  g_return_if_fail (GST_IS_PAD (pad));

  GST_OBJECT_LOCK (pad);

  hook = g_hook_get (&pad->probes, id);
  if (hook == NULL)
    goto not_found;

  GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "removing hook %ld",
      hook->hook_id);
  cleanup_hook (pad, hook);
  GST_OBJECT_UNLOCK (pad);

  return;

not_found:
  {
    GST_OBJECT_UNLOCK (pad);
    g_warning ("%s: pad `%p' has no probe with id `%lu'", G_STRLOC, pad, id);
    return;
  }
}

/**
 * gst_pad_is_blocked:
 * @pad: the #GstPad to query
 *
 * Checks if the pad is blocked or not. This function returns the
 * last requested state of the pad. It is not certain that the pad
 * is actually blocking at this point (see gst_pad_is_blocking()).
 *
 * Returns: %TRUE if the pad is blocked.
 *
 * MT safe.
 */
gboolean
gst_pad_is_blocked (GstPad * pad)
{
  gboolean result = FALSE;

  g_return_val_if_fail (GST_IS_PAD (pad), result);

  GST_OBJECT_LOCK (pad);
  result = GST_OBJECT_FLAG_IS_SET (pad, GST_PAD_FLAG_BLOCKED);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/**
 * gst_pad_is_blocking:
 * @pad: the #GstPad to query
 *
 * Checks if the pad is blocking or not. This is a guaranteed state
 * of whether the pad is actually blocking on a #GstBuffer or a #GstEvent.
 *
 * Returns: %TRUE if the pad is blocking.
 *
 * MT safe.
 */
gboolean
gst_pad_is_blocking (GstPad * pad)
{
  gboolean result = FALSE;

  g_return_val_if_fail (GST_IS_PAD (pad), result);

  GST_OBJECT_LOCK (pad);
  /* the blocking flag is only valid if the pad is not flushing */
  result = GST_PAD_IS_BLOCKING (pad) && !GST_PAD_IS_FLUSHING (pad);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/**
 * gst_pad_needs_reconfigure:
 * @pad: the #GstPad to check
 *
 * Check the #GST_PAD_FLAG_NEED_RECONFIGURE flag on @pad and return %TRUE
 * if the flag was set.
 *
 * Returns: %TRUE is the GST_PAD_FLAG_NEED_RECONFIGURE flag is set on @pad.
 */
gboolean
gst_pad_needs_reconfigure (GstPad * pad)
{
  gboolean reconfigure;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);
  reconfigure = GST_PAD_NEEDS_RECONFIGURE (pad);
  GST_DEBUG_OBJECT (pad, "peeking RECONFIGURE flag %d", reconfigure);
  GST_OBJECT_UNLOCK (pad);

  return reconfigure;
}

/**
 * gst_pad_check_reconfigure:
 * @pad: the #GstPad to check
 *
 * Check and clear the #GST_PAD_FLAG_NEED_RECONFIGURE flag on @pad and return %TRUE
 * if the flag was set.
 *
 * Returns: %TRUE is the GST_PAD_FLAG_NEED_RECONFIGURE flag was set on @pad.
 */
gboolean
gst_pad_check_reconfigure (GstPad * pad)
{
  gboolean reconfigure;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);
  reconfigure = GST_PAD_NEEDS_RECONFIGURE (pad);
  if (reconfigure) {
    GST_DEBUG_OBJECT (pad, "remove RECONFIGURE flag");
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_NEED_RECONFIGURE);
  }
  GST_OBJECT_UNLOCK (pad);

  return reconfigure;
}

/**
 * gst_pad_mark_reconfigure:
 * @pad: the #GstPad to mark
 *
 * Mark a pad for needing reconfiguration. The next call to
 * gst_pad_check_reconfigure() will return %TRUE after this call.
 */
void
gst_pad_mark_reconfigure (GstPad * pad)
{
  g_return_if_fail (GST_IS_PAD (pad));

  GST_OBJECT_LOCK (pad);
  GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_NEED_RECONFIGURE);
  GST_OBJECT_UNLOCK (pad);
}

/**
 * gst_pad_set_activate_function:
 * @p: a #GstPad.
 * @f: the #GstPadActivateFunction to set.
 *
 * Calls gst_pad_set_activate_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_activate_function_full:
 * @pad: a #GstPad.
 * @activate: the #GstPadActivateFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @activate will not be used anymore.
 *
 * Sets the given activate function for @pad. The activate function will
 * dispatch to gst_pad_activate_mode() to perform the actual activation.
 * Only makes sense to set on sink pads.
 *
 * Call this function if your sink pad can start a pull-based task.
 */
void
gst_pad_set_activate_function_full (GstPad * pad,
    GstPadActivateFunction activate, gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->activatenotify)
    pad->activatenotify (pad->activatedata);
  GST_PAD_ACTIVATEFUNC (pad) = activate;
  pad->activatedata = user_data;
  pad->activatenotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "activatefunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (activate));
}

/**
 * gst_pad_set_activatemode_function:
 * @p: a #GstPad.
 * @f: the #GstPadActivateModeFunction to set.
 *
 * Calls gst_pad_set_activatemode_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_activatemode_function_full:
 * @pad: a #GstPad.
 * @activatemode: the #GstPadActivateModeFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @activatemode will not be used anymore.
 *
 * Sets the given activate_mode function for the pad. An activate_mode function
 * prepares the element for data passing.
 */
void
gst_pad_set_activatemode_function_full (GstPad * pad,
    GstPadActivateModeFunction activatemode, gpointer user_data,
    GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->activatemodenotify)
    pad->activatemodenotify (pad->activatemodedata);
  GST_PAD_ACTIVATEMODEFUNC (pad) = activatemode;
  pad->activatemodedata = user_data;
  pad->activatemodenotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "activatemodefunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (activatemode));
}

/**
 * gst_pad_set_chain_function:
 * @p: a sink #GstPad.
 * @f: the #GstPadChainFunction to set.
 *
 * Calls gst_pad_set_chain_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_chain_function_full:
 * @pad: a sink #GstPad.
 * @chain: the #GstPadChainFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @chain will not be used anymore.
 *
 * Sets the given chain function for the pad. The chain function is called to
 * process a #GstBuffer input buffer. see #GstPadChainFunction for more details.
 */
void
gst_pad_set_chain_function_full (GstPad * pad, GstPadChainFunction chain,
    gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (GST_PAD_IS_SINK (pad));

  if (pad->chainnotify)
    pad->chainnotify (pad->chaindata);
  GST_PAD_CHAINFUNC (pad) = chain;
  pad->chaindata = user_data;
  pad->chainnotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "chainfunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (chain));
}

/**
 * gst_pad_set_chain_list_function:
 * @p: a sink #GstPad.
 * @f: the #GstPadChainListFunction to set.
 *
 * Calls gst_pad_set_chain_list_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_chain_list_function_full:
 * @pad: a sink #GstPad.
 * @chainlist: the #GstPadChainListFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @chainlist will not be used anymore.
 *
 * Sets the given chain list function for the pad. The chainlist function is
 * called to process a #GstBufferList input buffer list. See
 * #GstPadChainListFunction for more details.
 */
void
gst_pad_set_chain_list_function_full (GstPad * pad,
    GstPadChainListFunction chainlist, gpointer user_data,
    GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (GST_PAD_IS_SINK (pad));

  if (pad->chainlistnotify)
    pad->chainlistnotify (pad->chainlistdata);
  GST_PAD_CHAINLISTFUNC (pad) = chainlist;
  pad->chainlistdata = user_data;
  pad->chainlistnotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "chainlistfunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (chainlist));
}

/**
 * gst_pad_set_getrange_function:
 * @p: a source #GstPad.
 * @f: the #GstPadGetRangeFunction to set.
 *
 * Calls gst_pad_set_getrange_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_getrange_function_full:
 * @pad: a source #GstPad.
 * @get: the #GstPadGetRangeFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @get will not be used anymore.
 *
 * Sets the given getrange function for the pad. The getrange function is
 * called to produce a new #GstBuffer to start the processing pipeline. see
 * #GstPadGetRangeFunction for a description of the getrange function.
 */
void
gst_pad_set_getrange_function_full (GstPad * pad, GstPadGetRangeFunction get,
    gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (GST_PAD_IS_SRC (pad));

  if (pad->getrangenotify)
    pad->getrangenotify (pad->getrangedata);
  GST_PAD_GETRANGEFUNC (pad) = get;
  pad->getrangedata = user_data;
  pad->getrangenotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "getrangefunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (get));
}

/**
 * gst_pad_set_event_function:
 * @p: a #GstPad of either direction.
 * @f: the #GstPadEventFunction to set.
 *
 * Calls gst_pad_set_event_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_event_function_full:
 * @pad: a #GstPad of either direction.
 * @event: the #GstPadEventFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @event will not be used anymore.
 *
 * Sets the given event handler for the pad.
 */
void
gst_pad_set_event_function_full (GstPad * pad, GstPadEventFunction event,
    gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->eventnotify)
    pad->eventnotify (pad->eventdata);
  GST_PAD_EVENTFUNC (pad) = event;
  pad->eventdata = user_data;
  pad->eventnotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "eventfunc for set to %s",
      GST_DEBUG_FUNCPTR_NAME (event));
}

static gboolean
event_wrap (GstPad * pad, GstObject * object, GstEvent * event)
{
  GstFlowReturn ret;

  ret = GST_PAD_EVENTFULLFUNC (pad) (pad, object, event);
  if (ret == GST_FLOW_OK)
    return TRUE;
  return FALSE;
}

/**
 * gst_pad_set_event_full_function:
 * @p: a #GstPad of either direction.
 * @f: the #GstPadEventFullFunction to set.
 *
 * Calls gst_pad_set_event_full_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_event_full_function_full:
 * @pad: a #GstPad of either direction.
 * @event: the #GstPadEventFullFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @event will not be used anymore.
 *
 * Sets the given event handler for the pad.
 *
 * Since: 1.8
 */
void
gst_pad_set_event_full_function_full (GstPad * pad,
    GstPadEventFullFunction event, gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->eventnotify)
    pad->eventnotify (pad->eventdata);
  GST_PAD_EVENTFULLFUNC (pad) = event;
  GST_PAD_EVENTFUNC (pad) = event_wrap;
  pad->eventdata = user_data;
  pad->eventnotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "eventfullfunc for set to %s",
      GST_DEBUG_FUNCPTR_NAME (event));
}

/**
 * gst_pad_set_query_function:
 * @p: a #GstPad of either direction.
 * @f: the #GstPadQueryFunction to set.
 *
 * Calls gst_pad_set_query_function_full() with %NULL for the user_data and
 * notify.
 */
/**
 * gst_pad_set_query_function_full:
 * @pad: a #GstPad of either direction.
 * @query: the #GstPadQueryFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @query will not be used anymore.
 *
 * Set the given query function for the pad.
 */
void
gst_pad_set_query_function_full (GstPad * pad, GstPadQueryFunction query,
    gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->querynotify)
    pad->querynotify (pad->querydata);
  GST_PAD_QUERYFUNC (pad) = query;
  pad->querydata = user_data;
  pad->querynotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "queryfunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (query));
}

/**
 * gst_pad_set_iterate_internal_links_function:
 * @p: a #GstPad of either direction.
 * @f: the #GstPadIterIntLinkFunction to set.
 *
 * Calls gst_pad_set_iterate_internal_links_function_full() with %NULL
 * for the user_data and notify.
 */
/**
 * gst_pad_set_iterate_internal_links_function_full:
 * @pad: a #GstPad of either direction.
 * @iterintlink: the #GstPadIterIntLinkFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @iterintlink will not be used anymore.
 *
 * Sets the given internal link iterator function for the pad.
 */
void
gst_pad_set_iterate_internal_links_function_full (GstPad * pad,
    GstPadIterIntLinkFunction iterintlink, gpointer user_data,
    GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->iterintlinknotify)
    pad->iterintlinknotify (pad->iterintlinkdata);
  GST_PAD_ITERINTLINKFUNC (pad) = iterintlink;
  pad->iterintlinkdata = user_data;
  pad->iterintlinknotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "internal link iterator set to %s",
      GST_DEBUG_FUNCPTR_NAME (iterintlink));
}

/**
 * gst_pad_set_link_function:
 * @p: a #GstPad.
 * @f: the #GstPadLinkFunction to set.
 *
 * Calls gst_pad_set_link_function_full() with %NULL
 * for the user_data and notify.
 */
/**
 * gst_pad_set_link_function_full:
 * @pad: a #GstPad.
 * @link: the #GstPadLinkFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @link will not be used anymore.
 *
 * Sets the given link function for the pad. It will be called when
 * the pad is linked with another pad.
 *
 * The return value #GST_PAD_LINK_OK should be used when the connection can be
 * made.
 *
 * The return value #GST_PAD_LINK_REFUSED should be used when the connection
 * cannot be made for some reason.
 *
 * If @link is installed on a source pad, it should call the #GstPadLinkFunction
 * of the peer sink pad, if present.
 */
void
gst_pad_set_link_function_full (GstPad * pad, GstPadLinkFunction link,
    gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->linknotify)
    pad->linknotify (pad->linkdata);
  GST_PAD_LINKFUNC (pad) = link;
  pad->linkdata = user_data;
  pad->linknotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "linkfunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (link));
}

/**
 * gst_pad_set_unlink_function:
 * @p: a #GstPad.
 * @f: the #GstPadUnlinkFunction to set.
 *
 * Calls gst_pad_set_unlink_function_full() with %NULL
 * for the user_data and notify.
 */
/**
 * gst_pad_set_unlink_function_full:
 * @pad: a #GstPad.
 * @unlink: the #GstPadUnlinkFunction to set.
 * @user_data: user_data passed to @notify
 * @notify: notify called when @unlink will not be used anymore.
 *
 * Sets the given unlink function for the pad. It will be called
 * when the pad is unlinked.
 *
 * Note that the pad's lock is already held when the unlink
 * function is called, so most pad functions cannot be called
 * from within the callback.
 */
void
gst_pad_set_unlink_function_full (GstPad * pad, GstPadUnlinkFunction unlink,
    gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (GST_IS_PAD (pad));

  if (pad->unlinknotify)
    pad->unlinknotify (pad->unlinkdata);
  GST_PAD_UNLINKFUNC (pad) = unlink;
  pad->unlinkdata = user_data;
  pad->unlinknotify = notify;

  GST_CAT_DEBUG_OBJECT (GST_CAT_PADS, pad, "unlinkfunc set to %s",
      GST_DEBUG_FUNCPTR_NAME (unlink));
}

/**
 * gst_pad_unlink:
 * @srcpad: the source #GstPad to unlink.
 * @sinkpad: the sink #GstPad to unlink.
 *
 * Unlinks the source pad from the sink pad. Will emit the #GstPad::unlinked
 * signal on both pads.
 *
 * Returns: %TRUE if the pads were unlinked. This function returns %FALSE if
 * the pads were not linked together.
 *
 * MT safe.
 */
gboolean
gst_pad_unlink (GstPad * srcpad, GstPad * sinkpad)
{
  gboolean result = FALSE;
  GstElement *parent = NULL;

  g_return_val_if_fail (GST_IS_PAD (srcpad), FALSE);
  g_return_val_if_fail (GST_PAD_IS_SRC (srcpad), FALSE);
  g_return_val_if_fail (GST_IS_PAD (sinkpad), FALSE);
  g_return_val_if_fail (GST_PAD_IS_SINK (sinkpad), FALSE);

  GST_TRACER_PAD_UNLINK_PRE (srcpad, sinkpad);

  GST_CAT_INFO (GST_CAT_ELEMENT_PADS, "unlinking %s:%s(%p) and %s:%s(%p)",
      GST_DEBUG_PAD_NAME (srcpad), srcpad,
      GST_DEBUG_PAD_NAME (sinkpad), sinkpad);

  /* We need to notify the parent before taking any pad locks as the bin in
   * question might be waiting for a lock on the pad while holding its lock
   * that our message will try to take. */
  if ((parent = GST_ELEMENT_CAST (gst_pad_get_parent (srcpad)))) {
    if (GST_IS_ELEMENT (parent)) {
      gst_element_post_message (parent,
          gst_message_new_structure_change (GST_OBJECT_CAST (sinkpad),
              GST_STRUCTURE_CHANGE_TYPE_PAD_UNLINK, parent, TRUE));
    } else {
      gst_object_unref (parent);
      parent = NULL;
    }
  }

  GST_OBJECT_LOCK (srcpad);
  GST_OBJECT_LOCK (sinkpad);

  if (G_UNLIKELY (GST_PAD_PEER (srcpad) != sinkpad))
    goto not_linked_together;

  if (GST_PAD_UNLINKFUNC (srcpad)) {
    GstObject *tmpparent;

    ACQUIRE_PARENT (srcpad, tmpparent, no_src_parent);

    GST_PAD_UNLINKFUNC (srcpad) (srcpad, tmpparent);
    RELEASE_PARENT (tmpparent);
  }
no_src_parent:
  if (GST_PAD_UNLINKFUNC (sinkpad)) {
    GstObject *tmpparent;

    ACQUIRE_PARENT (sinkpad, tmpparent, no_sink_parent);

    GST_PAD_UNLINKFUNC (sinkpad) (sinkpad, tmpparent);
    RELEASE_PARENT (tmpparent);
  }
no_sink_parent:

  /* first clear peers */
  GST_PAD_PEER (srcpad) = NULL;
  GST_PAD_PEER (sinkpad) = NULL;

  GST_OBJECT_UNLOCK (sinkpad);
  GST_OBJECT_UNLOCK (srcpad);

  /* fire off a signal to each of the pads telling them
   * that they've been unlinked */
  g_signal_emit (srcpad, gst_pad_signals[PAD_UNLINKED], 0, sinkpad);
  g_signal_emit (sinkpad, gst_pad_signals[PAD_UNLINKED], 0, srcpad);

  GST_CAT_INFO (GST_CAT_ELEMENT_PADS, "unlinked %s:%s and %s:%s",
      GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad));

  result = TRUE;

done:
  if (parent != NULL) {
    gst_element_post_message (parent,
        gst_message_new_structure_change (GST_OBJECT_CAST (sinkpad),
            GST_STRUCTURE_CHANGE_TYPE_PAD_UNLINK, parent, FALSE));
    gst_object_unref (parent);
  }
  GST_TRACER_PAD_UNLINK_POST (srcpad, sinkpad, result);
  return result;

  /* ERRORS */
not_linked_together:
  {
    /* we do not emit a warning in this case because unlinking cannot
     * be made MT safe.*/
    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);
    goto done;
  }
}

/**
 * gst_pad_is_linked:
 * @pad: pad to check
 *
 * Checks if a @pad is linked to another pad or not.
 *
 * Returns: %TRUE if the pad is linked, %FALSE otherwise.
 *
 * MT safe.
 */
gboolean
gst_pad_is_linked (GstPad * pad)
{
  gboolean result;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);
  result = (GST_PAD_PEER (pad) != NULL);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/* get the caps from both pads and see if the intersection
 * is not empty.
 *
 * This function should be called with the pad LOCK on both
 * pads
 */
static gboolean
gst_pad_link_check_compatible_unlocked (GstPad * src, GstPad * sink,
    GstPadLinkCheck flags)
{
  GstCaps *srccaps = NULL;
  GstCaps *sinkcaps = NULL;
  gboolean compatible = FALSE;

  if (!(flags & (GST_PAD_LINK_CHECK_CAPS | GST_PAD_LINK_CHECK_TEMPLATE_CAPS)))
    return TRUE;

  /* Doing the expensive caps checking takes priority over only checking the template caps */
  if (flags & GST_PAD_LINK_CHECK_CAPS) {
    GST_OBJECT_UNLOCK (sink);
    GST_OBJECT_UNLOCK (src);

    srccaps = gst_pad_query_caps (src, NULL);
    sinkcaps = gst_pad_query_caps (sink, NULL);

    GST_OBJECT_LOCK (src);
    GST_OBJECT_LOCK (sink);
  } else {
    /* If one of the two pads doesn't have a template, consider the intersection
     * as valid.*/
    if (G_UNLIKELY ((GST_PAD_PAD_TEMPLATE (src) == NULL)
            || (GST_PAD_PAD_TEMPLATE (sink) == NULL))) {
      compatible = TRUE;
      goto done;
    }
    srccaps = gst_caps_ref (GST_PAD_TEMPLATE_CAPS (GST_PAD_PAD_TEMPLATE (src)));
    sinkcaps =
        gst_caps_ref (GST_PAD_TEMPLATE_CAPS (GST_PAD_PAD_TEMPLATE (sink)));
  }

  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, src, "src caps %" GST_PTR_FORMAT,
      srccaps);
  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, sink, "sink caps %" GST_PTR_FORMAT,
      sinkcaps);

  /* if we have caps on both pads we can check the intersection. If one
   * of the caps is %NULL, we return %TRUE. */
  if (G_UNLIKELY (srccaps == NULL || sinkcaps == NULL)) {
    if (srccaps)
      gst_caps_unref (srccaps);
    if (sinkcaps)
      gst_caps_unref (sinkcaps);
    goto done;
  }

  compatible = gst_caps_can_intersect (srccaps, sinkcaps);
  gst_caps_unref (srccaps);
  gst_caps_unref (sinkcaps);

done:
  GST_CAT_DEBUG (GST_CAT_CAPS, "caps are %scompatible",
      (compatible ? "" : "not "));

  return compatible;
}

/* check if the grandparents of both pads are the same.
 * This check is required so that we don't try to link
 * pads from elements in different bins without ghostpads.
 *
 * The LOCK should be held on both pads
 */
static gboolean
gst_pad_link_check_hierarchy (GstPad * src, GstPad * sink)
{
  GstObject *psrc, *psink;

  psrc = GST_OBJECT_PARENT (src);
  psink = GST_OBJECT_PARENT (sink);

  /* if one of the pads has no parent, we allow the link */
  if (G_UNLIKELY (psrc == NULL || psink == NULL))
    goto no_parent;

  /* only care about parents that are elements */
  if (G_UNLIKELY (!GST_IS_ELEMENT (psrc) || !GST_IS_ELEMENT (psink)))
    goto no_element_parent;

  /* if the parents are the same, we have a loop */
  if (G_UNLIKELY (psrc == psink))
    goto same_parents;

  /* if they both have a parent, we check the grandparents. We can not lock
   * the parent because we hold on the child (pad) and the locking order is
   * parent >> child. */
  psrc = GST_OBJECT_PARENT (psrc);
  psink = GST_OBJECT_PARENT (psink);

  /* if they have grandparents but they are not the same */
  if (G_UNLIKELY (psrc != psink))
    goto wrong_grandparents;

  return TRUE;

  /* ERRORS */
no_parent:
  {
    GST_CAT_DEBUG (GST_CAT_CAPS,
        "one of the pads has no parent %" GST_PTR_FORMAT " and %"
        GST_PTR_FORMAT, psrc, psink);
    return TRUE;
  }
no_element_parent:
  {
    GST_CAT_DEBUG (GST_CAT_CAPS,
        "one of the pads has no element parent %" GST_PTR_FORMAT " and %"
        GST_PTR_FORMAT, psrc, psink);
    return TRUE;
  }
same_parents:
  {
    GST_CAT_DEBUG (GST_CAT_CAPS, "pads have same parent %" GST_PTR_FORMAT,
        psrc);
    return FALSE;
  }
wrong_grandparents:
  {
    GST_CAT_DEBUG (GST_CAT_CAPS,
        "pads have different grandparents %" GST_PTR_FORMAT " and %"
        GST_PTR_FORMAT, psrc, psink);
    return FALSE;
  }
}

/* FIXME leftover from an attempt at refactoring... */
/* call with the two pads unlocked, when this function returns GST_PAD_LINK_OK,
 * the two pads will be locked in the srcpad, sinkpad order. */
static GstPadLinkReturn
gst_pad_link_prepare (GstPad * srcpad, GstPad * sinkpad, GstPadLinkCheck flags)
{
  GST_CAT_INFO (GST_CAT_PADS, "trying to link %s:%s and %s:%s",
      GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad));

  GST_OBJECT_LOCK (srcpad);

  if (G_UNLIKELY (GST_PAD_PEER (srcpad) != NULL))
    goto src_was_linked;

  GST_OBJECT_LOCK (sinkpad);

  if (G_UNLIKELY (GST_PAD_PEER (sinkpad) != NULL))
    goto sink_was_linked;

  /* check hierarchy, pads can only be linked if the grandparents
   * are the same. */
  if ((flags & GST_PAD_LINK_CHECK_HIERARCHY)
      && !gst_pad_link_check_hierarchy (srcpad, sinkpad))
    goto wrong_hierarchy;

  /* check pad caps for non-empty intersection */
  if (!gst_pad_link_check_compatible_unlocked (srcpad, sinkpad, flags))
    goto no_format;

  /* FIXME check pad scheduling for non-empty intersection */

  return GST_PAD_LINK_OK;

src_was_linked:
  {
    GST_CAT_INFO (GST_CAT_PADS, "src %s:%s was already linked to %s:%s",
        GST_DEBUG_PAD_NAME (srcpad),
        GST_DEBUG_PAD_NAME (GST_PAD_PEER (srcpad)));
    /* we do not emit a warning in this case because unlinking cannot
     * be made MT safe.*/
    GST_OBJECT_UNLOCK (srcpad);
    return GST_PAD_LINK_WAS_LINKED;
  }
sink_was_linked:
  {
    GST_CAT_INFO (GST_CAT_PADS, "sink %s:%s was already linked to %s:%s",
        GST_DEBUG_PAD_NAME (sinkpad),
        GST_DEBUG_PAD_NAME (GST_PAD_PEER (sinkpad)));
    /* we do not emit a warning in this case because unlinking cannot
     * be made MT safe.*/
    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);
    return GST_PAD_LINK_WAS_LINKED;
  }
wrong_hierarchy:
  {
    GST_CAT_INFO (GST_CAT_PADS, "pads have wrong hierarchy");
    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);
    return GST_PAD_LINK_WRONG_HIERARCHY;
  }
no_format:
  {
    GST_CAT_INFO (GST_CAT_PADS, "caps are incompatible");
    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);
    return GST_PAD_LINK_NOFORMAT;
  }
}

/**
 * gst_pad_can_link:
 * @srcpad: the source #GstPad.
 * @sinkpad: the sink #GstPad.
 *
 * Checks if the source pad and the sink pad are compatible so they can be
 * linked.
 *
 * Returns: %TRUE if the pads can be linked.
 */
gboolean
gst_pad_can_link (GstPad * srcpad, GstPad * sinkpad)
{
  GstPadLinkReturn result;

  /* generic checks */
  g_return_val_if_fail (GST_IS_PAD (srcpad), FALSE);
  g_return_val_if_fail (GST_IS_PAD (sinkpad), FALSE);

  GST_CAT_INFO (GST_CAT_PADS, "check if %s:%s can link with %s:%s",
      GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad));

  /* gst_pad_link_prepare does everything for us, we only release the locks
   * on the pads that it gets us. If this function returns !OK the locks are not
   * taken anymore. */
  result = gst_pad_link_prepare (srcpad, sinkpad, GST_PAD_LINK_CHECK_DEFAULT);
  if (result != GST_PAD_LINK_OK)
    goto done;

  GST_OBJECT_UNLOCK (srcpad);
  GST_OBJECT_UNLOCK (sinkpad);

done:
  return result == GST_PAD_LINK_OK;
}

/**
 * gst_pad_link_full:
 * @srcpad: the source #GstPad to link.
 * @sinkpad: the sink #GstPad to link.
 * @flags: the checks to validate when linking
 *
 * Links the source pad and the sink pad.
 *
 * This variant of #gst_pad_link provides a more granular control on the
 * checks being done when linking. While providing some considerable speedups
 * the caller of this method must be aware that wrong usage of those flags
 * can cause severe issues. Refer to the documentation of #GstPadLinkCheck
 * for more information.
 *
 * MT Safe.
 *
 * Returns: A result code indicating if the connection worked or
 *          what went wrong.
 */
GstPadLinkReturn
gst_pad_link_full (GstPad * srcpad, GstPad * sinkpad, GstPadLinkCheck flags)
{
  GstPadLinkReturn result;
  GstElement *parent;
  GstPadLinkFunction srcfunc, sinkfunc;

  g_return_val_if_fail (GST_IS_PAD (srcpad), GST_PAD_LINK_REFUSED);
  g_return_val_if_fail (GST_PAD_IS_SRC (srcpad), GST_PAD_LINK_WRONG_DIRECTION);
  g_return_val_if_fail (GST_IS_PAD (sinkpad), GST_PAD_LINK_REFUSED);
  g_return_val_if_fail (GST_PAD_IS_SINK (sinkpad),
      GST_PAD_LINK_WRONG_DIRECTION);

  GST_TRACER_PAD_LINK_PRE (srcpad, sinkpad);

  /* Notify the parent early. See gst_pad_unlink for details. */
  if (G_LIKELY ((parent = GST_ELEMENT_CAST (gst_pad_get_parent (srcpad))))) {
    if (G_LIKELY (GST_IS_ELEMENT (parent))) {
      gst_element_post_message (parent,
          gst_message_new_structure_change (GST_OBJECT_CAST (sinkpad),
              GST_STRUCTURE_CHANGE_TYPE_PAD_LINK, parent, TRUE));
    } else {
      gst_object_unref (parent);
      parent = NULL;
    }
  }

  /* prepare will also lock the two pads */
  result = gst_pad_link_prepare (srcpad, sinkpad, flags);

  if (G_UNLIKELY (result != GST_PAD_LINK_OK)) {
    GST_CAT_INFO (GST_CAT_PADS, "link between %s:%s and %s:%s failed: %s",
        GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad),
        gst_pad_link_get_name (result));
    goto done;
  }

  /* must set peers before calling the link function */
  GST_PAD_PEER (srcpad) = sinkpad;
  GST_PAD_PEER (sinkpad) = srcpad;

  /* check events, when something is different, mark pending */
  schedule_events (srcpad, sinkpad);

  /* get the link functions */
  srcfunc = GST_PAD_LINKFUNC (srcpad);
  sinkfunc = GST_PAD_LINKFUNC (sinkpad);

  if (G_UNLIKELY (srcfunc || sinkfunc)) {
    /* custom link functions, execute them */
    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);

    if (srcfunc) {
      GstObject *tmpparent;

      ACQUIRE_PARENT (srcpad, tmpparent, no_parent);
      /* this one will call the peer link function */
      result = srcfunc (srcpad, tmpparent, sinkpad);
      RELEASE_PARENT (tmpparent);
    } else if (sinkfunc) {
      GstObject *tmpparent;

      ACQUIRE_PARENT (sinkpad, tmpparent, no_parent);
      /* if no source link function, we need to call the sink link
       * function ourselves. */
      result = sinkfunc (sinkpad, tmpparent, srcpad);
      RELEASE_PARENT (tmpparent);
    }
  no_parent:

    GST_OBJECT_LOCK (srcpad);
    GST_OBJECT_LOCK (sinkpad);

    /* we released the lock, check if the same pads are linked still */
    if (GST_PAD_PEER (srcpad) != sinkpad || GST_PAD_PEER (sinkpad) != srcpad)
      goto concurrent_link;

    if (G_UNLIKELY (result != GST_PAD_LINK_OK))
      goto link_failed;
  }
  GST_OBJECT_UNLOCK (sinkpad);
  GST_OBJECT_UNLOCK (srcpad);

  /* fire off a signal to each of the pads telling them
   * that they've been linked */
  g_signal_emit (srcpad, gst_pad_signals[PAD_LINKED], 0, sinkpad);
  g_signal_emit (sinkpad, gst_pad_signals[PAD_LINKED], 0, srcpad);

  GST_CAT_INFO (GST_CAT_PADS, "linked %s:%s and %s:%s, successful",
      GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad));

  if (!(flags & GST_PAD_LINK_CHECK_NO_RECONFIGURE))
    gst_pad_send_event (srcpad, gst_event_new_reconfigure ());

done:
  if (G_LIKELY (parent)) {
    gst_element_post_message (parent,
        gst_message_new_structure_change (GST_OBJECT_CAST (sinkpad),
            GST_STRUCTURE_CHANGE_TYPE_PAD_LINK, parent, FALSE));
    gst_object_unref (parent);
  }

  GST_TRACER_PAD_LINK_POST (srcpad, sinkpad, result);
  return result;

  /* ERRORS */
concurrent_link:
  {
    GST_CAT_INFO (GST_CAT_PADS, "concurrent link between %s:%s and %s:%s",
        GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad));
    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);

    /* The other link operation succeeded first */
    result = GST_PAD_LINK_WAS_LINKED;
    goto done;
  }
link_failed:
  {
    GST_CAT_INFO (GST_CAT_PADS, "link between %s:%s and %s:%s failed: %s",
        GST_DEBUG_PAD_NAME (srcpad), GST_DEBUG_PAD_NAME (sinkpad),
        gst_pad_link_get_name (result));

    GST_PAD_PEER (srcpad) = NULL;
    GST_PAD_PEER (sinkpad) = NULL;

    GST_OBJECT_UNLOCK (sinkpad);
    GST_OBJECT_UNLOCK (srcpad);

    goto done;
  }
}

/**
 * gst_pad_link:
 * @srcpad: the source #GstPad to link.
 * @sinkpad: the sink #GstPad to link.
 *
 * Links the source pad and the sink pad.
 *
 * Returns: A result code indicating if the connection worked or
 *          what went wrong.
 *
 * MT Safe.
 */
GstPadLinkReturn
gst_pad_link (GstPad * srcpad, GstPad * sinkpad)
{
  return gst_pad_link_full (srcpad, sinkpad, GST_PAD_LINK_CHECK_DEFAULT);
}

static void
gst_pad_set_pad_template (GstPad * pad, GstPadTemplate * templ)
{
  GstPadTemplate **template_p;

  /* this function would need checks if it weren't static */

  GST_OBJECT_LOCK (pad);
  template_p = &pad->padtemplate;
  gst_object_replace ((GstObject **) template_p, (GstObject *) templ);
  GST_OBJECT_UNLOCK (pad);

  if (templ)
    gst_pad_template_pad_created (templ, pad);
}

/**
 * gst_pad_get_pad_template:
 * @pad: a #GstPad.
 *
 * Gets the template for @pad.
 *
 * Returns: (transfer full) (nullable): the #GstPadTemplate from which
 *     this pad was instantiated, or %NULL if this pad has no
 *     template. Unref after usage.
 */
GstPadTemplate *
gst_pad_get_pad_template (GstPad * pad)
{
  GstPadTemplate *templ;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  templ = GST_PAD_PAD_TEMPLATE (pad);

  return (templ ? gst_object_ref (templ) : NULL);
}

/**
 * gst_pad_has_current_caps:
 * @pad: a  #GstPad to check
 *
 * Check if @pad has caps set on it with a #GST_EVENT_CAPS event.
 *
 * Returns: %TRUE when @pad has caps associated with it.
 */
gboolean
gst_pad_has_current_caps (GstPad * pad)
{
  gboolean result;
  GstCaps *caps;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_OBJECT_LOCK (pad);
  caps = get_pad_caps (pad);
  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad,
      "check current pad caps %" GST_PTR_FORMAT, caps);
  result = (caps != NULL);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/**
 * gst_pad_get_current_caps:
 * @pad: a  #GstPad to get the current capabilities of.
 *
 * Gets the capabilities currently configured on @pad with the last
 * #GST_EVENT_CAPS event.
 *
 * Returns: (transfer full) (nullable): the current caps of the pad with
 * incremented ref-count or %NULL when pad has no caps. Unref after usage.
 */
GstCaps *
gst_pad_get_current_caps (GstPad * pad)
{
  GstCaps *result;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_OBJECT_LOCK (pad);
  if ((result = get_pad_caps (pad)))
    gst_caps_ref (result);
  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad,
      "get current pad caps %" GST_PTR_FORMAT, result);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/**
 * gst_pad_get_pad_template_caps:
 * @pad: a #GstPad to get the template capabilities from.
 *
 * Gets the capabilities for @pad's template.
 *
 * Returns: (transfer full): the #GstCaps of this pad template.
 * Unref after usage.
 */
GstCaps *
gst_pad_get_pad_template_caps (GstPad * pad)
{
  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  if (GST_PAD_PAD_TEMPLATE (pad))
    return gst_pad_template_get_caps (GST_PAD_PAD_TEMPLATE (pad));

  return gst_caps_ref (GST_CAPS_ANY);
}

/**
 * gst_pad_get_peer:
 * @pad: a #GstPad to get the peer of.
 *
 * Gets the peer of @pad. This function refs the peer pad so
 * you need to unref it after use.
 *
 * Returns: (transfer full) (nullable): the peer #GstPad. Unref after usage.
 *
 * MT safe.
 */
GstPad *
gst_pad_get_peer (GstPad * pad)
{
  GstPad *result;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_OBJECT_LOCK (pad);
  result = GST_PAD_PEER (pad);
  if (result)
    gst_object_ref (result);
  GST_OBJECT_UNLOCK (pad);

  return result;
}

/**
 * gst_pad_get_allowed_caps:
 * @pad: a #GstPad.
 *
 * Gets the capabilities of the allowed media types that can flow through
 * @pad and its peer.
 *
 * The allowed capabilities is calculated as the intersection of the results of
 * calling gst_pad_query_caps() on @pad and its peer. The caller owns a reference
 * on the resulting caps.
 *
 * Returns: (transfer full) (nullable): the allowed #GstCaps of the
 *     pad link. Unref the caps when you no longer need it. This
 *     function returns %NULL when @pad has no peer.
 *
 * MT safe.
 */
GstCaps *
gst_pad_get_allowed_caps (GstPad * pad)
{
  GstCaps *mycaps;
  GstCaps *caps = NULL;
  GstQuery *query;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_OBJECT_LOCK (pad);
  if (G_UNLIKELY (GST_PAD_PEER (pad) == NULL))
    goto no_peer;
  GST_OBJECT_UNLOCK (pad);

  GST_CAT_DEBUG_OBJECT (GST_CAT_PROPERTIES, pad, "getting allowed caps");

  mycaps = gst_pad_query_caps (pad, NULL);

  /* Query peer caps */
  query = gst_query_new_caps (mycaps);
  if (!gst_pad_peer_query (pad, query)) {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "Caps query failed");
    goto end;
  }

  gst_query_parse_caps_result (query, &caps);
  if (caps == NULL) {
    g_warn_if_fail (caps != NULL);
    goto end;
  }
  gst_caps_ref (caps);

  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "allowed caps %" GST_PTR_FORMAT,
      caps);

end:
  gst_query_unref (query);
  gst_caps_unref (mycaps);

  return caps;

no_peer:
  {
    GST_CAT_DEBUG_OBJECT (GST_CAT_PROPERTIES, pad, "no peer");
    GST_OBJECT_UNLOCK (pad);

    return NULL;
  }
}

/**
 * gst_pad_get_single_internal_link:
 * @pad: the #GstPad to get the internal link of.
 *
 * If there is a single internal link of the given pad, this function will
 * return it. Otherwise, it will return NULL.
 *
 * Returns: (transfer full) (nullable): a #GstPad, or %NULL if @pad has none
 * or more than one internal links. Unref returned pad with
 * gst_object_unref().
 *
 * Since: 1.18
 */
GstPad *
gst_pad_get_single_internal_link (GstPad * pad)
{
  GstIterator *iter;
  gboolean done = FALSE;
  GValue item = { 0, };
  GstPad *ret = NULL;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  iter = gst_pad_iterate_internal_links (pad);

  if (!iter)
    return NULL;

  while (!done) {
    switch (gst_iterator_next (iter, &item)) {
      case GST_ITERATOR_OK:
      {
        if (ret == NULL) {
          ret = g_value_dup_object (&item);
        } else {
          /* More than one internal link found - don't bother reffing */
          gst_clear_object (&ret);
          GST_DEBUG_OBJECT (pad,
              "Requested single internally linked pad, multiple found");
          done = TRUE;
        }
        g_value_reset (&item);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_clear_object (&ret);
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR_OBJECT (pad, "Could not iterate over internally linked pads");
        return NULL;
      case GST_ITERATOR_DONE:
        if (ret == NULL) {
          GST_DEBUG_OBJECT (pad,
              "Requested single internally linked pad, none found");
        }
        done = TRUE;
        break;
    }
  }
  g_value_unset (&item);
  gst_iterator_free (iter);

  return ret;
}

/**
 * gst_pad_iterate_internal_links_default:
 * @pad: the #GstPad to get the internal links of.
 * @parent: (allow-none): the parent of @pad or %NULL
 *
 * Iterate the list of pads to which the given pad is linked to inside of
 * the parent element.
 * This is the default handler, and thus returns an iterator of all of the
 * pads inside the parent element with opposite direction.
 *
 * The caller must free this iterator after use with gst_iterator_free().
 *
 * Returns: (nullable): a #GstIterator of #GstPad, or %NULL if @pad
 * has no parent. Unref each returned pad with gst_object_unref().
 */
GstIterator *
gst_pad_iterate_internal_links_default (GstPad * pad, GstObject * parent)
{
  GstIterator *res;
  GList **padlist;
  guint32 *cookie;
  GMutex *lock;
  gpointer owner;
  GstElement *eparent;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  if (parent != NULL && GST_IS_ELEMENT (parent)) {
    eparent = GST_ELEMENT_CAST (gst_object_ref (parent));
  } else {
    GST_OBJECT_LOCK (pad);
    eparent = GST_PAD_PARENT (pad);
    if (!eparent || !GST_IS_ELEMENT (eparent))
      goto no_parent;

    gst_object_ref (eparent);
    GST_OBJECT_UNLOCK (pad);
  }

  if (pad->direction == GST_PAD_SRC)
    padlist = &eparent->sinkpads;
  else
    padlist = &eparent->srcpads;

  GST_DEBUG_OBJECT (pad, "Making iterator");

  cookie = &eparent->pads_cookie;
  owner = eparent;
  lock = GST_OBJECT_GET_LOCK (eparent);

  res = gst_iterator_new_list (GST_TYPE_PAD,
      lock, cookie, padlist, (GObject *) owner, NULL);

  gst_object_unref (owner);

  return res;

  /* ERRORS */
no_parent:
  {
    GST_OBJECT_UNLOCK (pad);
    GST_DEBUG_OBJECT (pad, "no parent element");
    return NULL;
  }
}

/**
 * gst_pad_iterate_internal_links:
 * @pad: the GstPad to get the internal links of.
 *
 * Gets an iterator for the pads to which the given pad is linked to inside
 * of the parent element.
 *
 * Each #GstPad element yielded by the iterator will have its refcount increased,
 * so unref after use.
 *
 * Free-function: gst_iterator_free
 *
 * Returns: (transfer full) (nullable): a new #GstIterator of #GstPad
 *     or %NULL when the pad does not have an iterator function
 *     configured. Use gst_iterator_free() after usage.
 */
GstIterator *
gst_pad_iterate_internal_links (GstPad * pad)
{
  GstIterator *res = NULL;
  GstObject *parent;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);

  GST_OBJECT_LOCK (pad);
  ACQUIRE_PARENT (pad, parent, no_parent);
  GST_OBJECT_UNLOCK (pad);

  if (GST_PAD_ITERINTLINKFUNC (pad))
    res = GST_PAD_ITERINTLINKFUNC (pad) (pad, parent);

  RELEASE_PARENT (parent);

  return res;

  /* ERRORS */
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "no parent");
    GST_OBJECT_UNLOCK (pad);
    return NULL;
  }
}

/**
 * gst_pad_forward:
 * @pad: a #GstPad
 * @forward: (scope call): a #GstPadForwardFunction
 * @user_data: user data passed to @forward
 *
 * Calls @forward for all internally linked pads of @pad. This function deals with
 * dynamically changing internal pads and will make sure that the @forward
 * function is only called once for each pad.
 *
 * When @forward returns %TRUE, no further pads will be processed.
 *
 * Returns: %TRUE if one of the dispatcher functions returned %TRUE.
 */
gboolean
gst_pad_forward (GstPad * pad, GstPadForwardFunction forward,
    gpointer user_data)
{
  gboolean result = FALSE;
  GstIterator *iter;
  gboolean done = FALSE;
  GValue item = { 0, };
  GList *pushed_pads = NULL;

  iter = gst_pad_iterate_internal_links (pad);

  if (!iter)
    goto no_iter;

  while (!done) {
    switch (gst_iterator_next (iter, &item)) {
      case GST_ITERATOR_OK:
      {
        GstPad *intpad;

        intpad = g_value_get_object (&item);

        /* if already pushed, skip. FIXME, find something faster to tag pads */
        if (intpad == NULL || g_list_find (pushed_pads, intpad)) {
          g_value_reset (&item);
          break;
        }

        GST_LOG_OBJECT (pad, "calling forward function on pad %s:%s",
            GST_DEBUG_PAD_NAME (intpad));
        done = result = forward (intpad, user_data);

        pushed_pads = g_list_prepend (pushed_pads, intpad);

        g_value_reset (&item);
        break;
      }
      case GST_ITERATOR_RESYNC:
        /* We don't reset the result here because we don't push the event
         * again on pads that got the event already and because we need
         * to consider the result of the previous pushes */
        gst_iterator_resync (iter);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR_OBJECT (pad, "Could not iterate over internally linked pads");
        done = TRUE;
        break;
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  }
  g_value_unset (&item);
  gst_iterator_free (iter);

  g_list_free (pushed_pads);

no_iter:
  return result;
}

typedef struct
{
  GstEvent *event;
  gboolean result;
  gboolean dispatched;
} EventData;

static gboolean
event_forward_func (GstPad * pad, EventData * data)
{
  /* for each pad we send to, we should ref the event; it's up
   * to downstream to unref again when handled. */
  GST_LOG_OBJECT (pad, "Reffing and pushing event %p (%s) to %s:%s",
      data->event, GST_EVENT_TYPE_NAME (data->event), GST_DEBUG_PAD_NAME (pad));

  data->result |= gst_pad_push_event (pad, gst_event_ref (data->event));

  data->dispatched = TRUE;

  /* don't stop */
  return FALSE;
}

/**
 * gst_pad_event_default:
 * @pad: a #GstPad to call the default event handler on.
 * @parent: (allow-none): the parent of @pad or %NULL
 * @event: (transfer full): the #GstEvent to handle.
 *
 * Invokes the default event handler for the given pad.
 *
 * The EOS event will pause the task associated with @pad before it is forwarded
 * to all internally linked pads,
 *
 * The event is sent to all pads internally linked to @pad. This function
 * takes ownership of @event.
 *
 * Returns: %TRUE if the event was sent successfully.
 */
gboolean
gst_pad_event_default (GstPad * pad, GstObject * parent, GstEvent * event)
{
  gboolean result, forward = TRUE;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (event != NULL, FALSE);

  GST_LOG_OBJECT (pad, "default event handler for event %" GST_PTR_FORMAT,
      event);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CAPS:
      forward = GST_PAD_IS_PROXY_CAPS (pad);
      result = TRUE;
      break;
    default:
      break;
  }

  if (forward) {
    EventData data;

    data.event = event;
    data.dispatched = FALSE;
    data.result = FALSE;

    gst_pad_forward (pad, (GstPadForwardFunction) event_forward_func, &data);

    /* for sinkpads without a parent element or without internal links, nothing
     * will be dispatched but we still want to return TRUE. */
    if (data.dispatched)
      result = data.result;
    else
      result = TRUE;
  }

  gst_event_unref (event);

  return result;
}

/* Default accept caps implementation just checks against
 * the allowed caps for the pad */
static gboolean
gst_pad_query_accept_caps_default (GstPad * pad, GstQuery * query)
{
  /* get the caps and see if it intersects to something not empty */
  GstCaps *caps, *allowed = NULL;
  gboolean result;

  GST_DEBUG_OBJECT (pad, "query accept-caps %" GST_PTR_FORMAT, query);

  /* first forward the query to internally linked pads when we are dealing with
   * a PROXY CAPS */
  if (GST_PAD_IS_PROXY_CAPS (pad)) {
    result = gst_pad_proxy_query_accept_caps (pad, query);
    if (result)
      allowed = gst_pad_get_pad_template_caps (pad);
    else
      goto done;
  }

  gst_query_parse_accept_caps (query, &caps);
  if (!allowed) {
    if (GST_PAD_IS_ACCEPT_TEMPLATE (pad)) {
      allowed = gst_pad_get_pad_template_caps (pad);
    } else {
      GST_CAT_DEBUG_OBJECT (GST_CAT_PERFORMANCE, pad,
          "fallback ACCEPT_CAPS query, consider implementing a specialized version");
      allowed = gst_pad_query_caps (pad, caps);
    }
  }

  if (allowed) {
    if (GST_PAD_IS_ACCEPT_INTERSECT (pad)) {
      GST_DEBUG_OBJECT (pad,
          "allowed caps intersect %" GST_PTR_FORMAT ", caps %" GST_PTR_FORMAT,
          allowed, caps);
      result = gst_caps_can_intersect (caps, allowed);
    } else {
      GST_DEBUG_OBJECT (pad, "allowed caps subset %" GST_PTR_FORMAT ", caps %"
          GST_PTR_FORMAT, allowed, caps);
      result = gst_caps_is_subset (caps, allowed);
    }
    if (!result) {
      GST_CAT_WARNING_OBJECT (GST_CAT_CAPS, pad, "caps: %" GST_PTR_FORMAT
          " were not compatible with: %" GST_PTR_FORMAT, caps, allowed);
    }
    gst_caps_unref (allowed);
  } else {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad,
        "no compatible caps allowed on the pad");
    result = FALSE;
  }

  gst_query_set_accept_caps_result (query, result);

done:
  return TRUE;
}

/* Default caps implementation */
static gboolean
gst_pad_query_caps_default (GstPad * pad, GstQuery * query)
{
  GstCaps *result = NULL, *filter;
  GstPadTemplate *templ;
  gboolean fixed_caps;

  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "query caps %" GST_PTR_FORMAT,
      query);

  /* first try to proxy if we must */
  if (GST_PAD_IS_PROXY_CAPS (pad)) {
    if ((gst_pad_proxy_query_caps (pad, query))) {
      goto done;
    }
  }

  gst_query_parse_caps (query, &filter);

  /* no proxy or it failed, do default handling */
  fixed_caps = GST_PAD_IS_FIXED_CAPS (pad);

  GST_OBJECT_LOCK (pad);
  if (fixed_caps) {
    /* fixed caps, try the negotiated caps first */
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "fixed pad caps: trying pad caps");
    if ((result = get_pad_caps (pad)))
      goto filter_done_unlock;
  }

  if ((templ = GST_PAD_PAD_TEMPLATE (pad))) {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "trying pad template caps");
    if ((result = GST_PAD_TEMPLATE_CAPS (templ)))
      goto filter_done_unlock;
  }

  if (!fixed_caps) {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad,
        "non-fixed pad caps: trying pad caps");
    /* non fixed caps, try the negotiated caps */
    if ((result = get_pad_caps (pad)))
      goto filter_done_unlock;
  }

  /* this almost never happens */
  GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "pad has no caps");
  result = GST_CAPS_ANY;

filter_done_unlock:
  GST_OBJECT_UNLOCK (pad);

  /* run the filter on the result */
  if (filter) {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad,
        "using caps %p %" GST_PTR_FORMAT " with filter %p %"
        GST_PTR_FORMAT, result, result, filter, filter);
    result = gst_caps_intersect_full (filter, result, GST_CAPS_INTERSECT_FIRST);
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad, "result %p %" GST_PTR_FORMAT,
        result, result);
  } else {
    GST_CAT_DEBUG_OBJECT (GST_CAT_CAPS, pad,
        "using caps %p %" GST_PTR_FORMAT, result, result);
    result = gst_caps_ref (result);
  }
  gst_query_set_caps_result (query, result);
  gst_caps_unref (result);

done:
  return TRUE;
}

/* Default latency implementation */
typedef struct
{
  gboolean live;
  GstClockTime min, max;
} LatencyFoldData;

static gboolean
query_latency_default_fold (const GValue * item, GValue * ret,
    gpointer user_data)
{
  GstPad *pad = g_value_get_object (item), *peer;
  LatencyFoldData *fold_data = user_data;
  GstQuery *query;
  gboolean res = FALSE;

  query = gst_query_new_latency ();

  peer = gst_pad_get_peer (pad);
  if (peer) {
    res = gst_pad_peer_query (pad, query);
  } else {
    GST_LOG_OBJECT (pad, "No peer pad found, ignoring this pad");
  }

  if (res) {
    gboolean live;
    GstClockTime min, max;

    gst_query_parse_latency (query, &live, &min, &max);

    GST_LOG_OBJECT (pad, "got latency live:%s min:%" G_GINT64_FORMAT
        " max:%" G_GINT64_FORMAT, live ? "true" : "false", min, max);

    if (live) {
      if (min > fold_data->min)
        fold_data->min = min;

      if (fold_data->max == GST_CLOCK_TIME_NONE)
        fold_data->max = max;
      else if (max < fold_data->max)
        fold_data->max = max;

      fold_data->live = TRUE;
    }
  } else if (peer) {
    GST_DEBUG_OBJECT (pad, "latency query failed");
    g_value_set_boolean (ret, FALSE);
  }

  gst_query_unref (query);
  if (peer)
    gst_object_unref (peer);

  return TRUE;
}

static gboolean
gst_pad_query_latency_default (GstPad * pad, GstQuery * query)
{
  GstIterator *it;
  GstIteratorResult res;
  GValue ret = G_VALUE_INIT;
  gboolean query_ret;
  LatencyFoldData fold_data;

  it = gst_pad_iterate_internal_links (pad);
  if (!it) {
    GST_DEBUG_OBJECT (pad, "Can't iterate internal links");
    return FALSE;
  }

  g_value_init (&ret, G_TYPE_BOOLEAN);

retry:
  fold_data.live = FALSE;
  fold_data.min = 0;
  fold_data.max = GST_CLOCK_TIME_NONE;

  g_value_set_boolean (&ret, TRUE);
  res = gst_iterator_fold (it, query_latency_default_fold, &ret, &fold_data);
  switch (res) {
    case GST_ITERATOR_OK:
      g_assert_not_reached ();
      break;
    case GST_ITERATOR_DONE:
      break;
    case GST_ITERATOR_ERROR:
      g_value_set_boolean (&ret, FALSE);
      break;
    case GST_ITERATOR_RESYNC:
      gst_iterator_resync (it);
      goto retry;
    default:
      g_assert_not_reached ();
      break;
  }
  gst_iterator_free (it);

  query_ret = g_value_get_boolean (&ret);
  if (query_ret) {
    GST_LOG_OBJECT (pad, "got latency live:%s min:%" G_GINT64_FORMAT
        " max:%" G_GINT64_FORMAT, fold_data.live ? "true" : "false",
        fold_data.min, fold_data.max);

    if (fold_data.min > fold_data.max) {
      GST_ERROR_OBJECT (pad, "minimum latency bigger than maximum latency");
    }

    gst_query_set_latency (query, fold_data.live, fold_data.min, fold_data.max);
  } else {
    GST_LOG_OBJECT (pad, "latency query failed");
  }

  return query_ret;
}

typedef struct
{
  GstQuery *query;
  gboolean result;
  gboolean dispatched;
} QueryData;

static gboolean
query_forward_func (GstPad * pad, QueryData * data)
{
  GST_LOG_OBJECT (pad, "query peer %p (%s) of %s:%s",
      data->query, GST_QUERY_TYPE_NAME (data->query), GST_DEBUG_PAD_NAME (pad));

  data->result |= gst_pad_peer_query (pad, data->query);

  data->dispatched = TRUE;

  /* stop on first successful reply */
  return data->result;
}

/**
 * gst_pad_query_default:
 * @pad: a #GstPad to call the default query handler on.
 * @parent: (allow-none): the parent of @pad or %NULL
 * @query: (transfer none): the #GstQuery to handle.
 *
 * Invokes the default query handler for the given pad.
 * The query is sent to all pads internally linked to @pad. Note that
 * if there are many possible sink pads that are internally linked to
 * @pad, only one will be sent the query.
 * Multi-sinkpad elements should implement custom query handlers.
 *
 * Returns: %TRUE if the query was performed successfully.
 */
gboolean
gst_pad_query_default (GstPad * pad, GstObject * parent, GstQuery * query)
{
  gboolean forward, ret = FALSE;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_SCHEDULING:
      forward = GST_PAD_IS_PROXY_SCHEDULING (pad);
      break;
    case GST_QUERY_ALLOCATION:
      forward = GST_PAD_IS_PROXY_ALLOCATION (pad);
      break;
    case GST_QUERY_ACCEPT_CAPS:
      ret = gst_pad_query_accept_caps_default (pad, query);
      forward = FALSE;
      break;
    case GST_QUERY_CAPS:
      ret = gst_pad_query_caps_default (pad, query);
      forward = FALSE;
      break;
    case GST_QUERY_LATENCY:
      ret = gst_pad_query_latency_default (pad, query);
      forward = FALSE;
      break;
    case GST_QUERY_BITRATE:
      /* FIXME: better default handling */
      forward = TRUE;
      break;
    case GST_QUERY_POSITION:
    case GST_QUERY_SEEKING:
    case GST_QUERY_FORMATS:
    case GST_QUERY_JITTER:
    case GST_QUERY_RATE:
    case GST_QUERY_CONVERT:
    default:
      forward = TRUE;
      break;
  }

  GST_DEBUG_OBJECT (pad, "%sforwarding %p (%s) query", (forward ? "" : "not "),
      query, GST_QUERY_TYPE_NAME (query));

  if (forward) {
    QueryData data;

    data.query = query;
    data.dispatched = FALSE;
    data.result = FALSE;

    gst_pad_forward (pad, (GstPadForwardFunction) query_forward_func, &data);

    if (data.dispatched) {
      ret = data.result;
    } else {
      /* nothing dispatched, assume drained */
      if (GST_QUERY_TYPE (query) == GST_QUERY_DRAIN)
        ret = TRUE;
      else
        ret = FALSE;
    }
  }
  return ret;
}

#define N_STACK_ALLOCATE_PROBES (16)

static void
probe_hook_marshal (GHook * hook, ProbeMarshall * data)
{
  GstPad *pad = data->pad;
  GstPadProbeInfo *info = data->info;
  GstPadProbeType type, flags;
  GstPadProbeCallback callback;
  GstPadProbeReturn ret;
  gpointer original_data;
  guint i;

  /* if we have called this callback, do nothing. But only check
   * if we're actually calling probes a second time */
  if (data->retry) {
    for (i = 0; i < data->n_called_probes; i++) {
      if (data->called_probes[i] == hook->hook_id) {
        GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
            "hook %lu already called", hook->hook_id);
        return;
      }
    }
  }

  /* reallocate on the heap if we had more than 16 probes */
  if (data->n_called_probes == data->called_probes_size) {
    if (data->called_probes_size > N_STACK_ALLOCATE_PROBES) {
      data->called_probes_size *= 2;
      data->called_probes =
          g_renew (gulong, data->called_probes, data->called_probes_size);
    } else {
      gulong *tmp = data->called_probes;

      data->called_probes_size *= 2;
      data->called_probes = g_new (gulong, data->called_probes_size);
      memcpy (data->called_probes, tmp,
          N_STACK_ALLOCATE_PROBES * sizeof (gulong));
    }
  }
  data->called_probes[data->n_called_probes++] = hook->hook_id;

  flags = hook->flags >> G_HOOK_FLAG_USER_SHIFT;
  type = info->type;
  original_data = info->data;

  /* one of the scheduling types */
  if ((flags & GST_PAD_PROBE_TYPE_SCHEDULING & type) == 0)
    goto no_match;

  if (G_UNLIKELY (data->handled)) {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "probe previously returned HANDLED, not calling again");
    goto no_match;
  } else if (G_UNLIKELY (data->dropped)) {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "probe previously returned DROPPED, not calling again");
    goto no_match;
  }

  if (type & GST_PAD_PROBE_TYPE_PUSH) {
    /* one of the data types for non-idle probes */
    if ((type & GST_PAD_PROBE_TYPE_IDLE) == 0
        && (flags & _PAD_PROBE_TYPE_ALL_BOTH_AND_FLUSH & type) == 0)
      goto no_match;
  } else if (type & GST_PAD_PROBE_TYPE_PULL) {
    /* one of the data types for non-idle probes */
    if ((type & GST_PAD_PROBE_TYPE_BLOCKING) == 0
        && (flags & _PAD_PROBE_TYPE_ALL_BOTH_AND_FLUSH & type) == 0)
      goto no_match;
  } else {
    /* Type must have PULL or PUSH probe types */
    g_assert_not_reached ();
  }

  /* one of the blocking types must match */
  if ((type & GST_PAD_PROBE_TYPE_BLOCKING) &&
      (flags & GST_PAD_PROBE_TYPE_BLOCKING & type) == 0)
    goto no_match;
  if ((type & GST_PAD_PROBE_TYPE_BLOCKING) == 0 &&
      (flags & GST_PAD_PROBE_TYPE_BLOCKING))
    goto no_match;
  /* only probes that have GST_PAD_PROBE_TYPE_EVENT_FLUSH set */
  if ((type & GST_PAD_PROBE_TYPE_EVENT_FLUSH) &&
      (flags & GST_PAD_PROBE_TYPE_EVENT_FLUSH & type) == 0)
    goto no_match;

  GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
      "hook %lu with flags 0x%08x matches", hook->hook_id, flags);

  data->marshalled = TRUE;

  callback = (GstPadProbeCallback) hook->func;
  if (callback == NULL)
    return;

  info->id = hook->hook_id;

  if ((flags & GST_PAD_PROBE_TYPE_IDLE))
    pad->priv->idle_running++;

  GST_OBJECT_UNLOCK (pad);

  ret = callback (pad, info, hook->data);

  GST_OBJECT_LOCK (pad);

  if ((flags & GST_PAD_PROBE_TYPE_IDLE))
    pad->priv->idle_running--;

  if (ret != GST_PAD_PROBE_HANDLED && original_data != NULL
      && info->data == NULL) {
    GST_DEBUG_OBJECT (pad, "data item in pad probe info was dropped");
    info->type = GST_PAD_PROBE_TYPE_INVALID;
    data->dropped = TRUE;
  }

  switch (ret) {
    case GST_PAD_PROBE_REMOVE:
      /* remove the probe */
      GST_DEBUG_OBJECT (pad, "asked to remove hook");
      cleanup_hook (pad, hook);
      break;
    case GST_PAD_PROBE_DROP:
      /* need to drop the data, make sure other probes don't get called
       * anymore */
      GST_DEBUG_OBJECT (pad, "asked to drop item");
      info->type = GST_PAD_PROBE_TYPE_INVALID;
      data->dropped = TRUE;
      break;
    case GST_PAD_PROBE_HANDLED:
      GST_DEBUG_OBJECT (pad, "probe handled data");
      data->handled = TRUE;
      break;
    case GST_PAD_PROBE_PASS:
      /* inform the pad block to let things pass */
      GST_DEBUG_OBJECT (pad, "asked to pass item");
      data->pass = TRUE;
      break;
    case GST_PAD_PROBE_OK:
      GST_DEBUG_OBJECT (pad, "probe returned OK");
      break;
    default:
      GST_DEBUG_OBJECT (pad, "probe returned %d", ret);
      break;
  }
  return;

no_match:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "hook %lu with flags 0x%08x does not match %08x",
        hook->hook_id, flags, info->type);
    return;
  }
}

/* a probe that does not take or return any data */
#define PROBE_NO_DATA(pad,mask,label,defaultval)                \
  G_STMT_START {            \
    if (G_UNLIKELY (pad->num_probes)) {       \
      GstFlowReturn pval = defaultval;        \
      /* pass NULL as the data item */                          \
      GstPadProbeInfo info = { mask, 0, NULL, 0, 0 };   \
      info.ABI.abi.flow_ret = defaultval;     \
      ret = do_probe_callbacks (pad, &info, defaultval);  \
      if (G_UNLIKELY (ret != pval && ret != GST_FLOW_OK)) \
        goto label;           \
    }               \
  } G_STMT_END

#define PROBE_FULL(pad,mask,data,offs,size,label,handleable,handle_label) \
  G_STMT_START {              \
    if (G_UNLIKELY (pad->num_probes)) {         \
      /* pass the data item */            \
      GstPadProbeInfo info = { mask, 0, data, offs, size };   \
      info.ABI.abi.flow_ret = GST_FLOW_OK;        \
      ret = do_probe_callbacks (pad, &info, GST_FLOW_OK);   \
      /* store the possibly updated data item */      \
      data = GST_PAD_PROBE_INFO_DATA (&info);       \
      /* if something went wrong, exit */       \
      if (G_UNLIKELY (ret != GST_FLOW_OK)) {        \
  if (handleable && ret == GST_FLOW_CUSTOM_SUCCESS_1) {   \
    ret = info.ABI.abi.flow_ret;            \
    goto handle_label;            \
  }               \
  goto label;             \
      }                 \
    }                 \
  } G_STMT_END

#define PROBE_PUSH(pad,mask,data,label)   \
  PROBE_FULL(pad, mask, data, -1, -1, label, FALSE, label);
#define PROBE_HANDLE(pad,mask,data,label,handle_label)  \
  PROBE_FULL(pad, mask, data, -1, -1, label, TRUE, handle_label);
#define PROBE_PULL(pad,mask,data,offs,size,label)   \
  PROBE_FULL(pad, mask, data, offs, size, label, FALSE, label);

static GstFlowReturn
do_pad_idle_probe_wait (GstPad * pad)
{
  while (GST_PAD_IS_RUNNING_IDLE_PROBE (pad)) {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "waiting idle probe to be removed");
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_BLOCKING);
    GST_PAD_BLOCK_WAIT (pad);
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_BLOCKING);
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "We got unblocked");

    if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
      return GST_FLOW_FLUSHING;
  }
  return GST_FLOW_OK;
}

#define PROBE_TYPE_IS_SERIALIZED(i) \
    ( \
      ( \
        (((i)->type & (GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM | \
        GST_PAD_PROBE_TYPE_EVENT_FLUSH)) && \
        GST_EVENT_IS_SERIALIZED ((i)->data)) \
      ) || ( \
        (((i)->type & GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM) && \
        GST_QUERY_IS_SERIALIZED ((i)->data)) \
      ) || ( \
        ((i)->type & (GST_PAD_PROBE_TYPE_BUFFER | \
        GST_PAD_PROBE_TYPE_BUFFER_LIST))  \
      ) \
    )

static GstFlowReturn
do_probe_callbacks (GstPad * pad, GstPadProbeInfo * info,
    GstFlowReturn defaultval)
{
  ProbeMarshall data;
  guint cookie;
  gboolean is_block;
  gulong called_probes[N_STACK_ALLOCATE_PROBES];

  data.pad = pad;
  data.info = info;
  data.pass = FALSE;
  data.handled = FALSE;
  data.marshalled = FALSE;
  data.dropped = FALSE;

  /* We stack-allocate for N_STACK_ALLOCATE_PROBES hooks as a first step. If more are needed,
   * we will re-allocate with g_malloc(). This should usually never be needed
   */
  data.called_probes = called_probes;
  data.n_called_probes = 0;
  data.called_probes_size = N_STACK_ALLOCATE_PROBES;
  data.retry = FALSE;

  is_block =
      (info->type & GST_PAD_PROBE_TYPE_BLOCK) == GST_PAD_PROBE_TYPE_BLOCK;

  if (is_block && PROBE_TYPE_IS_SERIALIZED (info)) {
    if (do_pad_idle_probe_wait (pad) == GST_FLOW_FLUSHING)
      goto flushing;
  }

again:
  GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "do probes");
  cookie = pad->priv->probe_list_cookie;

  g_hook_list_marshal (&pad->probes, TRUE,
      (GHookMarshaller) probe_hook_marshal, &data);

  /* if the list changed, call the new callbacks (they will not be in
   * called_probes yet) */
  if (cookie != pad->priv->probe_list_cookie) {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "probe list changed, restarting");
    data.retry = TRUE;
    goto again;
  }

  /* the first item that dropped will stop the hooks and then we drop here */
  if (data.dropped)
    goto dropped;

  /* If one handler took care of it, let the the item pass */
  if (data.handled) {
    goto handled;
  }

  /* if no handler matched and we are blocking, let the item pass */
  if (!data.marshalled && is_block)
    goto passed;

  /* At this point, all handlers returned either OK or PASS. If one handler
   * returned PASS, let the item pass */
  if (data.pass)
    goto passed;

  if (is_block) {
    while (GST_PAD_IS_BLOCKED (pad)) {
      GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
          "we are blocked %d times", pad->num_blocked);

      /* we might have released the lock */
      if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
        goto flushing;

      /* now we block the streaming thread. It can be unlocked when we
       * deactivate the pad (which will also set the FLUSHING flag) or
       * when the pad is unblocked. A flushing event will also unblock
       * the pad after setting the FLUSHING flag. */
      GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
          "Waiting to be unblocked or set flushing");
      GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_BLOCKING);
      GST_PAD_BLOCK_WAIT (pad);
      GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_BLOCKING);
      GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "We got unblocked");

      /* if the list changed, call the new callbacks (they will not be in
       * called_probes yet) */
      if (cookie != pad->priv->probe_list_cookie) {
        GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
            "probe list changed, restarting");
        data.retry = TRUE;
        goto again;
      }

      if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
        goto flushing;
    }
  }

  if (data.called_probes_size > N_STACK_ALLOCATE_PROBES)
    g_free (data.called_probes);

  return defaultval;

  /* ERRORS */
flushing:
  {
    GST_DEBUG_OBJECT (pad, "pad is flushing");
    if (data.called_probes_size > N_STACK_ALLOCATE_PROBES)
      g_free (data.called_probes);
    return GST_FLOW_FLUSHING;
  }
dropped:
  {
    GST_DEBUG_OBJECT (pad, "data is dropped");
    if (data.called_probes_size > N_STACK_ALLOCATE_PROBES)
      g_free (data.called_probes);
    return GST_FLOW_CUSTOM_SUCCESS;
  }
passed:
  {
    /* FIXME : Should we return FLOW_OK or the defaultval ?? */
    GST_DEBUG_OBJECT (pad, "data is passed");
    if (data.called_probes_size > N_STACK_ALLOCATE_PROBES)
      g_free (data.called_probes);
    return GST_FLOW_OK;
  }
handled:
  {
    GST_DEBUG_OBJECT (pad, "data was handled");
    if (data.called_probes_size > N_STACK_ALLOCATE_PROBES)
      g_free (data.called_probes);
    return GST_FLOW_CUSTOM_SUCCESS_1;
  }
}

/* pad offsets */

/**
 * gst_pad_get_offset:
 * @pad: a #GstPad
 *
 * Get the offset applied to the running time of @pad. @pad has to be a source
 * pad.
 *
 * Returns: the offset.
 */
gint64
gst_pad_get_offset (GstPad * pad)
{
  gint64 result;

  g_return_val_if_fail (GST_IS_PAD (pad), 0);

  GST_OBJECT_LOCK (pad);
  result = pad->offset;
  GST_OBJECT_UNLOCK (pad);

  return result;
}

static gboolean
mark_event_not_received (GstPad * pad, PadEvent * ev, gpointer user_data)
{
  ev->received = FALSE;
  return TRUE;
}

/**
 * gst_pad_set_offset:
 * @pad: a #GstPad
 * @offset: the offset
 *
 * Set the offset that will be applied to the running time of @pad.
 */
void
gst_pad_set_offset (GstPad * pad, gint64 offset)
{
  g_return_if_fail (GST_IS_PAD (pad));

  GST_OBJECT_LOCK (pad);
  /* if nothing changed, do nothing */
  if (pad->offset == offset)
    goto done;

  pad->offset = offset;
  GST_DEBUG_OBJECT (pad, "changed offset to %" GST_STIME_FORMAT,
      GST_STIME_ARGS (offset));

  /* resend all sticky events with updated offset on next buffer push */
  events_foreach (pad, mark_event_not_received, NULL);
  GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);

done:
  GST_OBJECT_UNLOCK (pad);
}

typedef struct
{
  GstFlowReturn ret;

  /* If TRUE and ret is not OK this means
   * that pushing the EOS event failed
   */
  gboolean was_eos;

  /* If called for an event this is
   * the event that would be pushed
   * next. Don't forward sticky events
   * that would come after that */
  GstEvent *event;
} PushStickyData;

/* should be called with pad LOCK */
static gboolean
push_sticky (GstPad * pad, PadEvent * ev, gpointer user_data)
{
  PushStickyData *data = user_data;
  GstEvent *event = ev->event;

  if (ev->received) {
    GST_DEBUG_OBJECT (pad, "event %s was already received",
        GST_EVENT_TYPE_NAME (event));
    return TRUE;
  }

  /* If we're called because of an sticky event, only forward
   * events that would come before this new event and the
   * event itself */
  if (data->event && GST_EVENT_IS_STICKY (data->event) &&
      GST_EVENT_TYPE (data->event) <= GST_EVENT_SEGMENT &&
      GST_EVENT_TYPE (data->event) < GST_EVENT_TYPE (event)) {
    data->ret = GST_FLOW_CUSTOM_SUCCESS_1;
  } else {
    data->ret = gst_pad_push_event_unchecked (pad, gst_event_ref (event),
        GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM);
    if (data->ret == GST_FLOW_CUSTOM_SUCCESS_1)
      data->ret = GST_FLOW_OK;
  }

  switch (data->ret) {
    case GST_FLOW_OK:
      ev->received = TRUE;
      GST_DEBUG_OBJECT (pad, "event %s marked received",
          GST_EVENT_TYPE_NAME (event));
      break;
    case GST_FLOW_CUSTOM_SUCCESS:
      /* we can't assume the event is received when it was dropped */
      GST_DEBUG_OBJECT (pad, "event %s was dropped, mark pending",
          GST_EVENT_TYPE_NAME (event));
      GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);
      data->ret = GST_FLOW_OK;
      break;
    case GST_FLOW_CUSTOM_SUCCESS_1:
      /* event was ignored and should be sent later */
      GST_DEBUG_OBJECT (pad, "event %s was ignored, mark pending",
          GST_EVENT_TYPE_NAME (event));
      GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);
      data->ret = GST_FLOW_OK;
      break;
    case GST_FLOW_NOT_LINKED:
      /* not linked is not a problem, we are sticky so the event will be
       * rescheduled to be sent later on re-link, but only for non-EOS events */
      GST_DEBUG_OBJECT (pad, "pad was not linked, mark pending");
      if (GST_EVENT_TYPE (event) != GST_EVENT_EOS) {
        data->ret = GST_FLOW_OK;
        ev->received = TRUE;
      }
      break;
    default:
      GST_DEBUG_OBJECT (pad, "result %s, mark pending events",
          gst_flow_get_name (data->ret));
      GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);
      break;
  }

  if (data->ret != GST_FLOW_OK && GST_EVENT_TYPE (event) == GST_EVENT_EOS)
    data->was_eos = TRUE;

  return data->ret == GST_FLOW_OK;
}

/* check sticky events and push them when needed. should be called
 * with pad LOCK */
static inline GstFlowReturn
check_sticky (GstPad * pad, GstEvent * event)
{
  PushStickyData data = { GST_FLOW_OK, FALSE, event };

  if (G_UNLIKELY (GST_PAD_HAS_PENDING_EVENTS (pad))) {
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_PENDING_EVENTS);

    GST_DEBUG_OBJECT (pad, "pushing all sticky events");
    events_foreach (pad, push_sticky, &data);

    /* If there's an EOS event we must push it downstream
     * even if sending a previous sticky event failed.
     * Otherwise the pipeline might wait forever for EOS.
     *
     * Only do this if pushing another event than the EOS
     * event failed.
     */
    if (data.ret != GST_FLOW_OK && !data.was_eos) {
      PadEvent *ev = find_event_by_type (pad, GST_EVENT_EOS, 0);

      if (ev && !ev->received) {
        data.ret = gst_pad_push_event_unchecked (pad, gst_event_ref (ev->event),
            GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM);
        /* the event could have been dropped. Because this can only
         * happen if the user asked for it, it's not an error */
        if (data.ret == GST_FLOW_CUSTOM_SUCCESS)
          data.ret = GST_FLOW_OK;
      }
    }
  }
  return data.ret;
}


/**
 * gst_pad_query:
 * @pad: a #GstPad to invoke the default query on.
 * @query: (transfer none): the #GstQuery to perform.
 *
 * Dispatches a query to a pad. The query should have been allocated by the
 * caller via one of the type-specific allocation functions. The element that
 * the pad belongs to is responsible for filling the query with an appropriate
 * response, which should then be parsed with a type-specific query parsing
 * function.
 *
 * Again, the caller is responsible for both the allocation and deallocation of
 * the query structure.
 *
 * Please also note that some queries might need a running pipeline to work.
 *
 * Returns: %TRUE if the query could be performed.
 */
gboolean
gst_pad_query (GstPad * pad, GstQuery * query)
{
  GstObject *parent;
  gboolean res, serialized;
  GstPadQueryFunction func;
  GstPadProbeType type;
  GstFlowReturn ret;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_IS_QUERY (query), FALSE);

  if (GST_PAD_IS_SRC (pad)) {
    if (G_UNLIKELY (!GST_QUERY_IS_UPSTREAM (query)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_QUERY_UPSTREAM;
  } else if (GST_PAD_IS_SINK (pad)) {
    if (G_UNLIKELY (!GST_QUERY_IS_DOWNSTREAM (query)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM;
  } else
    goto unknown_direction;

  GST_DEBUG_OBJECT (pad, "doing query %p (%s)", query,
      GST_QUERY_TYPE_NAME (query));
  GST_TRACER_PAD_QUERY_PRE (pad, query);

  serialized = GST_QUERY_IS_SERIALIZED (query);
  if (G_UNLIKELY (serialized))
    GST_PAD_STREAM_LOCK (pad);

  GST_OBJECT_LOCK (pad);
  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH |
      GST_PAD_PROBE_TYPE_BLOCK, query, probe_stopped);
  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH, query, probe_stopped);

  ACQUIRE_PARENT (pad, parent, no_parent);
  GST_OBJECT_UNLOCK (pad);

  if ((func = GST_PAD_QUERYFUNC (pad)) == NULL)
    goto no_func;

  res = func (pad, parent, query);

  RELEASE_PARENT (parent);

  GST_DEBUG_OBJECT (pad, "sent query %p (%s), result %d", query,
      GST_QUERY_TYPE_NAME (query), res);
  GST_TRACER_PAD_QUERY_POST (pad, query, res);

  if (res != TRUE)
    goto query_failed;

  GST_OBJECT_LOCK (pad);
  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PULL, query, probe_stopped);
  GST_OBJECT_UNLOCK (pad);

  if (G_UNLIKELY (serialized))
    GST_PAD_STREAM_UNLOCK (pad);

  return res;

  /* ERRORS */
wrong_direction:
  {
    g_warning ("pad %s:%s query %s in wrong direction",
        GST_DEBUG_PAD_NAME (pad), GST_QUERY_TYPE_NAME (query));
    return FALSE;
  }
unknown_direction:
  {
    g_warning ("pad %s:%s has invalid direction", GST_DEBUG_PAD_NAME (pad));
    return FALSE;
  }
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "had no parent");
    GST_OBJECT_UNLOCK (pad);
    if (G_UNLIKELY (serialized))
      GST_PAD_STREAM_UNLOCK (pad);
    return FALSE;
  }
no_func:
  {
    GST_DEBUG_OBJECT (pad, "had no query function");
    RELEASE_PARENT (parent);
    if (G_UNLIKELY (serialized))
      GST_PAD_STREAM_UNLOCK (pad);
    return FALSE;
  }
query_failed:
  {
    GST_DEBUG_OBJECT (pad, "query failed");
    if (G_UNLIKELY (serialized))
      GST_PAD_STREAM_UNLOCK (pad);
    return FALSE;
  }
probe_stopped:
  {
    GST_DEBUG_OBJECT (pad, "probe stopped: %s", gst_flow_get_name (ret));
    GST_OBJECT_UNLOCK (pad);
    if (G_UNLIKELY (serialized))
      GST_PAD_STREAM_UNLOCK (pad);

    /* if a probe dropped without handling, we don't sent it further but assume
     * that the probe did not answer the query and return FALSE */
    if (ret != GST_FLOW_CUSTOM_SUCCESS_1)
      res = FALSE;
    else
      res = TRUE;

    return res;
  }
}

/**
 * gst_pad_peer_query:
 * @pad: a #GstPad to invoke the peer query on.
 * @query: (transfer none): the #GstQuery to perform.
 *
 * Performs gst_pad_query() on the peer of @pad.
 *
 * The caller is responsible for both the allocation and deallocation of
 * the query structure.
 *
 * Returns: %TRUE if the query could be performed. This function returns %FALSE
 * if @pad has no peer.
 */
gboolean
gst_pad_peer_query (GstPad * pad, GstQuery * query)
{
  GstPad *peerpad;
  GstPadProbeType type;
  gboolean res, serialized;
  GstFlowReturn ret;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_IS_QUERY (query), FALSE);

  if (GST_PAD_IS_SRC (pad)) {
    if (G_UNLIKELY (!GST_QUERY_IS_DOWNSTREAM (query)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM;
  } else if (GST_PAD_IS_SINK (pad)) {
    if (G_UNLIKELY (!GST_QUERY_IS_UPSTREAM (query)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_QUERY_UPSTREAM;
  } else
    goto unknown_direction;

  GST_DEBUG_OBJECT (pad, "peer query %p (%s)", query,
      GST_QUERY_TYPE_NAME (query));

  serialized = GST_QUERY_IS_SERIALIZED (query);

  GST_OBJECT_LOCK (pad);
  if (GST_PAD_IS_SRC (pad) && serialized) {
    /* all serialized queries on the srcpad trigger push of
     * sticky events */
    if (check_sticky (pad, NULL) != GST_FLOW_OK)
      goto sticky_failed;
  }

  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH |
      GST_PAD_PROBE_TYPE_BLOCK, query, probe_stopped);
  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH, query, probe_stopped);

  peerpad = GST_PAD_PEER (pad);
  if (G_UNLIKELY (peerpad == NULL))
    goto no_peer;

  gst_object_ref (peerpad);
  GST_OBJECT_UNLOCK (pad);

  res = gst_pad_query (peerpad, query);

  gst_object_unref (peerpad);

  if (res != TRUE)
    goto query_failed;

  GST_OBJECT_LOCK (pad);
  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PULL, query, probe_stopped);
  GST_OBJECT_UNLOCK (pad);

  return res;

  /* ERRORS */
wrong_direction:
  {
    g_warning ("pad %s:%s query %s in wrong direction",
        GST_DEBUG_PAD_NAME (pad), GST_QUERY_TYPE_NAME (query));
    return FALSE;
  }
unknown_direction:
  {
    g_warning ("pad %s:%s has invalid direction", GST_DEBUG_PAD_NAME (pad));
    return FALSE;
  }
sticky_failed:
  {
    GST_WARNING_OBJECT (pad, "could not send sticky events");
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
no_peer:
  {
    GST_INFO_OBJECT (pad, "pad has no peer");
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
query_failed:
  {
    GST_DEBUG_OBJECT (pad, "query failed");
    return FALSE;
  }
probe_stopped:
  {
    GST_DEBUG_OBJECT (pad, "probe stopped: %s", gst_flow_get_name (ret));
    GST_OBJECT_UNLOCK (pad);

    /* if a probe dropped without handling, we don't sent it further but
     * assume that the probe did not answer the query and return FALSE */
    if (ret != GST_FLOW_CUSTOM_SUCCESS_1)
      res = FALSE;
    else
      res = TRUE;

    return res;
  }
}

/**********************************************************************
 * Data passing functions
 */

/* this is the chain function that does not perform the additional argument
 * checking for that little extra speed.
 */
static inline GstFlowReturn
gst_pad_chain_data_unchecked (GstPad * pad, GstPadProbeType type, void *data)
{
  GstFlowReturn ret;
  GstObject *parent;
  gboolean handled = FALSE;

  GST_PAD_STREAM_LOCK (pad);

  GST_OBJECT_LOCK (pad);
  if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
    goto flushing;

  if (G_UNLIKELY (GST_PAD_IS_EOS (pad)))
    goto eos;

  if (G_UNLIKELY (GST_PAD_MODE (pad) != GST_PAD_MODE_PUSH))
    goto wrong_mode;

#ifdef GST_ENABLE_EXTRA_CHECKS
  if (G_UNLIKELY (pad->priv->last_cookie != pad->priv->events_cookie)) {
    if (!find_event_by_type (pad, GST_EVENT_STREAM_START, 0)) {
      g_warning (G_STRLOC
          ":%s:<%s:%s> Got data flow before stream-start event",
          G_STRFUNC, GST_DEBUG_PAD_NAME (pad));
    }
    if (!find_event_by_type (pad, GST_EVENT_SEGMENT, 0)) {
      g_warning (G_STRLOC
          ":%s:<%s:%s> Got data flow before segment event",
          G_STRFUNC, GST_DEBUG_PAD_NAME (pad));
    }
    pad->priv->last_cookie = pad->priv->events_cookie;
  }
#endif

  PROBE_HANDLE (pad, type | GST_PAD_PROBE_TYPE_BLOCK, data, probe_stopped,
      probe_handled);

  PROBE_HANDLE (pad, type, data, probe_stopped, probe_handled);

  ACQUIRE_PARENT (pad, parent, no_parent);
  GST_OBJECT_UNLOCK (pad);

  /* NOTE: we read the chainfunc unlocked.
   * we cannot hold the lock for the pad so we might send
   * the data to the wrong function. This is not really a
   * problem since functions are assigned at creation time
   * and don't change that often... */
  if (G_LIKELY (type & GST_PAD_PROBE_TYPE_BUFFER)) {
    GstPadChainFunction chainfunc;

    if (G_UNLIKELY ((chainfunc = GST_PAD_CHAINFUNC (pad)) == NULL))
      goto no_function;

    GST_CAT_DEBUG_OBJECT (GST_CAT_SCHEDULING, pad,
        "calling chainfunction &%s with buffer %" GST_PTR_FORMAT,
        GST_DEBUG_FUNCPTR_NAME (chainfunc), GST_BUFFER (data));

    ret = chainfunc (pad, parent, GST_BUFFER_CAST (data));

    GST_CAT_DEBUG_OBJECT (GST_CAT_SCHEDULING, pad,
        "called chainfunction &%s with buffer %p, returned %s",
        GST_DEBUG_FUNCPTR_NAME (chainfunc), data, gst_flow_get_name (ret));
  } else {
    GstPadChainListFunction chainlistfunc;

    if (G_UNLIKELY ((chainlistfunc = GST_PAD_CHAINLISTFUNC (pad)) == NULL))
      goto no_function;

    GST_CAT_DEBUG_OBJECT (GST_CAT_SCHEDULING, pad,
        "calling chainlistfunction &%s",
        GST_DEBUG_FUNCPTR_NAME (chainlistfunc));

    ret = chainlistfunc (pad, parent, GST_BUFFER_LIST_CAST (data));

    GST_CAT_DEBUG_OBJECT (GST_CAT_SCHEDULING, pad,
        "called chainlistfunction &%s, returned %s",
        GST_DEBUG_FUNCPTR_NAME (chainlistfunc), gst_flow_get_name (ret));
  }

  RELEASE_PARENT (parent);

  GST_PAD_STREAM_UNLOCK (pad);

  return ret;

  /* ERRORS */
flushing:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "chaining, but pad was flushing");
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_FLUSHING;
  }
eos:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "chaining, but pad was EOS");
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_EOS;
  }
wrong_mode:
  {
    g_critical ("chain on pad %s:%s but it was not in push mode",
        GST_DEBUG_PAD_NAME (pad));
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_ERROR;
  }
probe_handled:
  handled = TRUE;
  /* PASSTHROUGH */
probe_stopped:
  {
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    /* We unref the buffer, except if the probe handled it (CUSTOM_SUCCESS_1) */
    if (!handled)
      gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));

    switch (ret) {
      case GST_FLOW_CUSTOM_SUCCESS:
      case GST_FLOW_CUSTOM_SUCCESS_1:
        GST_DEBUG_OBJECT (pad, "dropped or handled buffer");
        ret = GST_FLOW_OK;
        break;
      default:
        GST_DEBUG_OBJECT (pad, "an error occurred %s", gst_flow_get_name (ret));
        break;
    }
    return ret;
  }
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "No parent when chaining %" GST_PTR_FORMAT, data);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    return GST_FLOW_FLUSHING;
  }
no_function:
  {
    RELEASE_PARENT (parent);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    g_critical ("chain on pad %s:%s but it has no chainfunction",
        GST_DEBUG_PAD_NAME (pad));
    GST_PAD_STREAM_UNLOCK (pad);
    return GST_FLOW_NOT_SUPPORTED;
  }
}

/**
 * gst_pad_chain:
 * @pad: a sink #GstPad, returns GST_FLOW_ERROR if not.
 * @buffer: (transfer full): the #GstBuffer to send, return GST_FLOW_ERROR
 *     if not.
 *
 * Chain a buffer to @pad.
 *
 * The function returns #GST_FLOW_FLUSHING if the pad was flushing.
 *
 * If the buffer type is not acceptable for @pad (as negotiated with a
 * preceding GST_EVENT_CAPS event), this function returns
 * #GST_FLOW_NOT_NEGOTIATED.
 *
 * The function proceeds calling the chain function installed on @pad (see
 * gst_pad_set_chain_function()) and the return value of that function is
 * returned to the caller. #GST_FLOW_NOT_SUPPORTED is returned if @pad has no
 * chain function.
 *
 * In all cases, success or failure, the caller loses its reference to @buffer
 * after calling this function.
 *
 * Returns: a #GstFlowReturn from the pad.
 *
 * MT safe.
 */
GstFlowReturn
gst_pad_chain (GstPad * pad, GstBuffer * buffer)
{
  g_return_val_if_fail (GST_IS_PAD (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_IS_BUFFER (buffer), GST_FLOW_ERROR);

  return gst_pad_chain_data_unchecked (pad,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_PUSH, buffer);
}

static GstFlowReturn
gst_pad_chain_list_default (GstPad * pad, GstObject * parent,
    GstBufferList * list)
{
  guint i, len;
  GstBuffer *buffer;
  GstFlowReturn ret;

  GST_INFO_OBJECT (pad, "chaining each buffer in list individually");

  len = gst_buffer_list_length (list);

  ret = GST_FLOW_OK;
  for (i = 0; i < len; i++) {
    buffer = gst_buffer_list_get (list, i);
    ret =
        gst_pad_chain_data_unchecked (pad,
        GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_PUSH,
        gst_buffer_ref (buffer));
    if (ret != GST_FLOW_OK)
      break;
  }
  gst_buffer_list_unref (list);

  return ret;
}

/**
 * gst_pad_chain_list:
 * @pad: a sink #GstPad, returns GST_FLOW_ERROR if not.
 * @list: (transfer full): the #GstBufferList to send, return GST_FLOW_ERROR
 *     if not.
 *
 * Chain a bufferlist to @pad.
 *
 * The function returns #GST_FLOW_FLUSHING if the pad was flushing.
 *
 * If @pad was not negotiated properly with a CAPS event, this function
 * returns #GST_FLOW_NOT_NEGOTIATED.
 *
 * The function proceeds calling the chainlist function installed on @pad (see
 * gst_pad_set_chain_list_function()) and the return value of that function is
 * returned to the caller. #GST_FLOW_NOT_SUPPORTED is returned if @pad has no
 * chainlist function.
 *
 * In all cases, success or failure, the caller loses its reference to @list
 * after calling this function.
 *
 * MT safe.
 *
 * Returns: a #GstFlowReturn from the pad.
 */
GstFlowReturn
gst_pad_chain_list (GstPad * pad, GstBufferList * list)
{
  g_return_val_if_fail (GST_IS_PAD (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), GST_FLOW_ERROR);

  return gst_pad_chain_data_unchecked (pad,
      GST_PAD_PROBE_TYPE_BUFFER_LIST | GST_PAD_PROBE_TYPE_PUSH, list);
}

static GstFlowReturn
gst_pad_push_data (GstPad * pad, GstPadProbeType type, void *data)
{
  GstPad *peer;
  GstFlowReturn ret;
  gboolean handled = FALSE;

  GST_OBJECT_LOCK (pad);
  if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
    goto flushing;

  if (G_UNLIKELY (GST_PAD_IS_EOS (pad)))
    goto eos;

  if (G_UNLIKELY (GST_PAD_MODE (pad) != GST_PAD_MODE_PUSH))
    goto wrong_mode;

#ifdef GST_ENABLE_EXTRA_CHECKS
  if (G_UNLIKELY (pad->priv->last_cookie != pad->priv->events_cookie)) {
    if (!find_event_by_type (pad, GST_EVENT_STREAM_START, 0)) {
      g_warning (G_STRLOC
          ":%s:<%s:%s> Got data flow before stream-start event",
          G_STRFUNC, GST_DEBUG_PAD_NAME (pad));
    }
    if (!find_event_by_type (pad, GST_EVENT_SEGMENT, 0)) {
      g_warning (G_STRLOC
          ":%s:<%s:%s> Got data flow before segment event",
          G_STRFUNC, GST_DEBUG_PAD_NAME (pad));
    }
    pad->priv->last_cookie = pad->priv->events_cookie;
  }
#endif

  if (G_UNLIKELY ((ret = check_sticky (pad, NULL))) != GST_FLOW_OK)
    goto events_error;

  /* do block probes */
  PROBE_HANDLE (pad, type | GST_PAD_PROBE_TYPE_BLOCK, data, probe_stopped,
      probe_handled);

  /* recheck sticky events because the probe might have cause a relink */
  if (G_UNLIKELY ((ret = check_sticky (pad, NULL))) != GST_FLOW_OK)
    goto events_error;

  /* do post-blocking probes */
  PROBE_HANDLE (pad, type, data, probe_stopped, probe_handled);

  /* recheck sticky events because the probe might have cause a relink */
  if (G_UNLIKELY ((ret = check_sticky (pad, NULL))) != GST_FLOW_OK)
    goto events_error;

  if (G_UNLIKELY ((peer = GST_PAD_PEER (pad)) == NULL))
    goto not_linked;

  /* take ref to peer pad before releasing the lock */
  gst_object_ref (peer);
  pad->priv->using++;
  GST_OBJECT_UNLOCK (pad);

  ret = gst_pad_chain_data_unchecked (peer, type, data);
  data = NULL;

  gst_object_unref (peer);

  GST_OBJECT_LOCK (pad);
  pad->ABI.abi.last_flowret = ret;
  pad->priv->using--;
  if (pad->priv->using == 0) {
    /* pad is not active anymore, trigger idle callbacks */
    PROBE_NO_DATA (pad, GST_PAD_PROBE_TYPE_PUSH | GST_PAD_PROBE_TYPE_IDLE,
        probe_stopped, ret);
  }
  GST_OBJECT_UNLOCK (pad);

  return ret;

  /* ERROR recovery here */
  /* ERRORS */
flushing:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "pushing, but pad was flushing");
    pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
    GST_OBJECT_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_FLUSHING;
  }
eos:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "pushing, but pad was EOS");
    pad->ABI.abi.last_flowret = GST_FLOW_EOS;
    GST_OBJECT_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_EOS;
  }
wrong_mode:
  {
    g_critical ("pushing on pad %s:%s but it was not activated in push mode",
        GST_DEBUG_PAD_NAME (pad));
    pad->ABI.abi.last_flowret = GST_FLOW_ERROR;
    GST_OBJECT_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_ERROR;
  }
events_error:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "error pushing events, return %s", gst_flow_get_name (ret));
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return ret;
  }
probe_handled:
  handled = TRUE;
  /* PASSTHROUGH */
probe_stopped:
  {
    GST_OBJECT_UNLOCK (pad);
    if (data != NULL && !handled)
      gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));

    switch (ret) {
      case GST_FLOW_CUSTOM_SUCCESS:
      case GST_FLOW_CUSTOM_SUCCESS_1:
        GST_DEBUG_OBJECT (pad, "dropped or handled buffer");
        ret = GST_FLOW_OK;
        break;
      default:
        GST_DEBUG_OBJECT (pad, "an error occurred %s", gst_flow_get_name (ret));
        break;
    }
    pad->ABI.abi.last_flowret = ret;
    return ret;
  }
not_linked:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "pushing, but it was not linked");
    pad->ABI.abi.last_flowret = GST_FLOW_NOT_LINKED;
    GST_OBJECT_UNLOCK (pad);
    gst_mini_object_unref (GST_MINI_OBJECT_CAST (data));
    return GST_FLOW_NOT_LINKED;
  }
}

/**
 * gst_pad_push:
 * @pad: a source #GstPad, returns #GST_FLOW_ERROR if not.
 * @buffer: (transfer full): the #GstBuffer to push returns GST_FLOW_ERROR
 *     if not.
 *
 * Pushes a buffer to the peer of @pad.
 *
 * This function will call installed block probes before triggering any
 * installed data probes.
 *
 * The function proceeds calling gst_pad_chain() on the peer pad and returns
 * the value from that function. If @pad has no peer, #GST_FLOW_NOT_LINKED will
 * be returned.
 *
 * In all cases, success or failure, the caller loses its reference to @buffer
 * after calling this function.
 *
 * Returns: a #GstFlowReturn from the peer pad.
 *
 * MT safe.
 */
GstFlowReturn
gst_pad_push (GstPad * pad, GstBuffer * buffer)
{
  GstFlowReturn res;

  g_return_val_if_fail (GST_IS_PAD (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_PAD_IS_SRC (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_IS_BUFFER (buffer), GST_FLOW_ERROR);

  GST_TRACER_PAD_PUSH_PRE (pad, buffer);
  res = gst_pad_push_data (pad,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_PUSH, buffer);
  GST_TRACER_PAD_PUSH_POST (pad, res);
  return res;
}

/**
 * gst_pad_push_list:
 * @pad: a source #GstPad, returns #GST_FLOW_ERROR if not.
 * @list: (transfer full): the #GstBufferList to push returns GST_FLOW_ERROR
 *     if not.
 *
 * Pushes a buffer list to the peer of @pad.
 *
 * This function will call installed block probes before triggering any
 * installed data probes.
 *
 * The function proceeds calling the chain function on the peer pad and returns
 * the value from that function. If @pad has no peer, #GST_FLOW_NOT_LINKED will
 * be returned. If the peer pad does not have any installed chainlist function
 * every group buffer of the list will be merged into a normal #GstBuffer and
 * chained via gst_pad_chain().
 *
 * In all cases, success or failure, the caller loses its reference to @list
 * after calling this function.
 *
 * Returns: a #GstFlowReturn from the peer pad.
 *
 * MT safe.
 */
GstFlowReturn
gst_pad_push_list (GstPad * pad, GstBufferList * list)
{
  GstFlowReturn res;

  g_return_val_if_fail (GST_IS_PAD (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_PAD_IS_SRC (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_IS_BUFFER_LIST (list), GST_FLOW_ERROR);

  GST_TRACER_PAD_PUSH_LIST_PRE (pad, list);
  res = gst_pad_push_data (pad,
      GST_PAD_PROBE_TYPE_BUFFER_LIST | GST_PAD_PROBE_TYPE_PUSH, list);
  GST_TRACER_PAD_PUSH_LIST_POST (pad, res);
  return res;
}

static GstFlowReturn
gst_pad_get_range_unchecked (GstPad * pad, guint64 offset, guint size,
    GstBuffer ** buffer)
{
  GstFlowReturn ret;
  GstPadGetRangeFunction getrangefunc;
  GstObject *parent;
  GstBuffer *res_buf;

  GST_PAD_STREAM_LOCK (pad);

  GST_OBJECT_LOCK (pad);
  if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
    goto flushing;

  if (G_UNLIKELY (GST_PAD_MODE (pad) != GST_PAD_MODE_PULL))
    goto wrong_mode;

  if (G_UNLIKELY ((ret = check_sticky (pad, NULL))) != GST_FLOW_OK)
    goto events_error;

  res_buf = *buffer;

  /* when one of the probes returns DROPPED, probe_stopped will be called
   * and we skip calling the getrange function, res_buf should then contain a
   * valid result buffer */
  PROBE_PULL (pad, GST_PAD_PROBE_TYPE_PULL | GST_PAD_PROBE_TYPE_BLOCK,
      res_buf, offset, size, probe_stopped);

  /* recheck sticky events because the probe might have cause a relink */
  if (G_UNLIKELY ((ret = check_sticky (pad, NULL))) != GST_FLOW_OK)
    goto events_error;

  ACQUIRE_PARENT (pad, parent, no_parent);
  GST_OBJECT_UNLOCK (pad);

  if (G_UNLIKELY ((getrangefunc = GST_PAD_GETRANGEFUNC (pad)) == NULL))
    goto no_function;

  GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
      "calling getrangefunc %s, offset %"
      G_GUINT64_FORMAT ", size %u",
      GST_DEBUG_FUNCPTR_NAME (getrangefunc), offset, size);

  ret = getrangefunc (pad, parent, offset, size, &res_buf);

  RELEASE_PARENT (parent);

  GST_OBJECT_LOCK (pad);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto get_range_failed;

  /* can only fire the signal if we have a valid buffer */
probed_data:
  PROBE_PULL (pad, GST_PAD_PROBE_TYPE_PULL | GST_PAD_PROBE_TYPE_BUFFER,
      res_buf, offset, size, probe_stopped_unref);
  pad->ABI.abi.last_flowret = ret;
  GST_OBJECT_UNLOCK (pad);

  GST_PAD_STREAM_UNLOCK (pad);

  /* If the caller provided a buffer it must be filled by the getrange
   * function instead of it returning a new buffer */
  g_return_val_if_fail (!*buffer || res_buf == *buffer, GST_FLOW_ERROR);

  *buffer = res_buf;

  return ret;

  /* ERRORS */
flushing:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "getrange, but pad was flushing");
    pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    return GST_FLOW_FLUSHING;
  }
wrong_mode:
  {
    g_critical ("getrange on pad %s:%s but it was not activated in pull mode",
        GST_DEBUG_PAD_NAME (pad));
    pad->ABI.abi.last_flowret = GST_FLOW_ERROR;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    return GST_FLOW_ERROR;
  }
events_error:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "error pushing events");
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    return ret;
  }
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "no parent");
    pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    return GST_FLOW_FLUSHING;
  }
no_function:
  {
    g_critical ("getrange on pad %s:%s but it has no getrangefunction",
        GST_DEBUG_PAD_NAME (pad));
    RELEASE_PARENT (parent);
    GST_PAD_STREAM_UNLOCK (pad);
    return GST_FLOW_NOT_SUPPORTED;
  }
probe_stopped:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "probe returned %s", gst_flow_get_name (ret));
    if (ret == GST_FLOW_CUSTOM_SUCCESS) {
      if (res_buf) {
        /* the probe filled the buffer and asks us to not call the getrange
         * anymore, we continue with the post probes then. */
        GST_DEBUG_OBJECT (pad, "handled buffer");
        ret = GST_FLOW_OK;
        goto probed_data;
      } else {
        /* no buffer, we are EOS */
        GST_DEBUG_OBJECT (pad, "no buffer, return EOS");
        ret = GST_FLOW_EOS;
      }
    }
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);

    return ret;
  }
probe_stopped_unref:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "probe returned %s", gst_flow_get_name (ret));
    /* if we drop here, it signals EOS */
    if (ret == GST_FLOW_CUSTOM_SUCCESS)
      ret = GST_FLOW_EOS;
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    if (*buffer == NULL)
      gst_buffer_unref (res_buf);
    return ret;
  }
get_range_failed:
  {
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);
    GST_CAT_LEVEL_LOG (GST_CAT_SCHEDULING,
        (ret >= GST_FLOW_EOS) ? GST_LEVEL_INFO : GST_LEVEL_WARNING,
        pad, "getrange failed, flow: %s", gst_flow_get_name (ret));
    return ret;
  }
}

/**
 * gst_pad_get_range:
 * @pad: a src #GstPad, returns #GST_FLOW_ERROR if not.
 * @offset: The start offset of the buffer
 * @size: The length of the buffer
 * @buffer: (out callee-allocates): a pointer to hold the #GstBuffer,
 *     returns #GST_FLOW_ERROR if %NULL.
 *
 * When @pad is flushing this function returns #GST_FLOW_FLUSHING
 * immediately and @buffer is %NULL.
 *
 * Calls the getrange function of @pad, see #GstPadGetRangeFunction for a
 * description of a getrange function. If @pad has no getrange function
 * installed (see gst_pad_set_getrange_function()) this function returns
 * #GST_FLOW_NOT_SUPPORTED.
 *
 * If @buffer points to a variable holding %NULL, a valid new #GstBuffer will be
 * placed in @buffer when this function returns #GST_FLOW_OK. The new buffer
 * must be freed with gst_buffer_unref() after usage.
 *
 * When @buffer points to a variable that points to a valid #GstBuffer, the
 * buffer will be filled with the result data when this function returns
 * #GST_FLOW_OK. If the provided buffer is larger than @size, only
 * @size bytes will be filled in the result buffer and its size will be updated
 * accordingly.
 *
 * Note that less than @size bytes can be returned in @buffer when, for example,
 * an EOS condition is near or when @buffer is not large enough to hold @size
 * bytes. The caller should check the result buffer size to get the result size.
 *
 * When this function returns any other result value than #GST_FLOW_OK, @buffer
 * will be unchanged.
 *
 * This is a lowlevel function. Usually gst_pad_pull_range() is used.
 *
 * Returns: a #GstFlowReturn from the pad.
 *
 * MT safe.
 */
GstFlowReturn
gst_pad_get_range (GstPad * pad, guint64 offset, guint size,
    GstBuffer ** buffer)
{
  g_return_val_if_fail (GST_IS_PAD (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_PAD_IS_SRC (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (buffer != NULL, GST_FLOW_ERROR);
  g_return_val_if_fail (*buffer == NULL || (GST_IS_BUFFER (*buffer)
          && gst_buffer_get_size (*buffer) >= size), GST_FLOW_ERROR);

  return gst_pad_get_range_unchecked (pad, offset, size, buffer);
}

/**
 * gst_pad_pull_range:
 * @pad: a sink #GstPad, returns GST_FLOW_ERROR if not.
 * @offset: The start offset of the buffer
 * @size: The length of the buffer
 * @buffer: (out callee-allocates): a pointer to hold the #GstBuffer, returns
 *     GST_FLOW_ERROR if %NULL.
 *
 * Pulls a @buffer from the peer pad or fills up a provided buffer.
 *
 * This function will first trigger the pad block signal if it was
 * installed.
 *
 * When @pad is not linked #GST_FLOW_NOT_LINKED is returned else this
 * function returns the result of gst_pad_get_range() on the peer pad.
 * See gst_pad_get_range() for a list of return values and for the
 * semantics of the arguments of this function.
 *
 * If @buffer points to a variable holding %NULL, a valid new #GstBuffer will be
 * placed in @buffer when this function returns #GST_FLOW_OK. The new buffer
 * must be freed with gst_buffer_unref() after usage. When this function
 * returns any other result value, @buffer will still point to %NULL.
 *
 * When @buffer points to a variable that points to a valid #GstBuffer, the
 * buffer will be filled with the result data when this function returns
 * #GST_FLOW_OK. When this function returns any other result value,
 * @buffer will be unchanged. If the provided buffer is larger than @size, only
 * @size bytes will be filled in the result buffer and its size will be updated
 * accordingly.
 *
 * Note that less than @size bytes can be returned in @buffer when, for example,
 * an EOS condition is near or when @buffer is not large enough to hold @size
 * bytes. The caller should check the result buffer size to get the result size.
 *
 * Returns: a #GstFlowReturn from the peer pad.
 *
 * MT safe.
 */
GstFlowReturn
gst_pad_pull_range (GstPad * pad, guint64 offset, guint size,
    GstBuffer ** buffer)
{
  GstPad *peer;
  GstFlowReturn ret;
  GstBuffer *res_buf;

  g_return_val_if_fail (GST_IS_PAD (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (GST_PAD_IS_SINK (pad), GST_FLOW_ERROR);
  g_return_val_if_fail (buffer != NULL, GST_FLOW_ERROR);
  g_return_val_if_fail (*buffer == NULL || (GST_IS_BUFFER (*buffer)
          && gst_buffer_get_size (*buffer) >= size), GST_FLOW_ERROR);

  GST_TRACER_PAD_PULL_RANGE_PRE (pad, offset, size);

  GST_OBJECT_LOCK (pad);
  if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
    goto flushing;

  if (G_UNLIKELY (GST_PAD_MODE (pad) != GST_PAD_MODE_PULL))
    goto wrong_mode;

  res_buf = *buffer;

  /* when one of the probes returns DROPPED, probe_stopped will be
   * called and we skip calling the peer getrange function. *buffer should then
   * contain a valid buffer */
  PROBE_PULL (pad, GST_PAD_PROBE_TYPE_PULL | GST_PAD_PROBE_TYPE_BLOCK,
      res_buf, offset, size, probe_stopped);

  if (G_UNLIKELY ((peer = GST_PAD_PEER (pad)) == NULL))
    goto not_linked;

  gst_object_ref (peer);
  pad->priv->using++;
  GST_OBJECT_UNLOCK (pad);

  ret = gst_pad_get_range_unchecked (peer, offset, size, &res_buf);

  gst_object_unref (peer);

  GST_OBJECT_LOCK (pad);
  pad->priv->using--;
  pad->ABI.abi.last_flowret = ret;
  if (pad->priv->using == 0) {
    /* pad is not active anymore, trigger idle callbacks */
    PROBE_NO_DATA (pad, GST_PAD_PROBE_TYPE_PULL | GST_PAD_PROBE_TYPE_IDLE,
        probe_stopped_unref, ret);
  }

  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto pull_range_failed;

probed_data:
  PROBE_PULL (pad, GST_PAD_PROBE_TYPE_PULL | GST_PAD_PROBE_TYPE_BUFFER,
      res_buf, offset, size, probe_stopped_unref);

  GST_OBJECT_UNLOCK (pad);

  *buffer = res_buf;

  GST_TRACER_PAD_PULL_RANGE_POST (pad, *buffer, ret);
  return ret;

  /* ERROR recovery here */
flushing:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "pullrange, but pad was flushing");
    pad->ABI.abi.last_flowret = GST_FLOW_FLUSHING;
    GST_OBJECT_UNLOCK (pad);
    ret = GST_FLOW_FLUSHING;
    goto done;
  }
wrong_mode:
  {
    g_critical ("pullrange on pad %s:%s but it was not activated in pull mode",
        GST_DEBUG_PAD_NAME (pad));
    pad->ABI.abi.last_flowret = GST_FLOW_ERROR;
    GST_OBJECT_UNLOCK (pad);
    ret = GST_FLOW_ERROR;
    goto done;
  }
probe_stopped:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad, "pre probe returned %s",
        gst_flow_get_name (ret));
    if (ret == GST_FLOW_CUSTOM_SUCCESS) {
      if (res_buf) {
        /* the probe filled the buffer and asks us to not forward to the peer
         * anymore, we continue with the post probes then */
        GST_DEBUG_OBJECT (pad, "handled buffer");
        ret = GST_FLOW_OK;
        goto probed_data;
      } else {
        /* no buffer, we are EOS then */
        GST_DEBUG_OBJECT (pad, "no buffer, return EOS");
        ret = GST_FLOW_EOS;
      }
    }
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    goto done;
  }
not_linked:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "pulling range, but it was not linked");
    pad->ABI.abi.last_flowret = GST_FLOW_NOT_LINKED;
    GST_OBJECT_UNLOCK (pad);
    ret = GST_FLOW_NOT_LINKED;
    goto done;
  }
pull_range_failed:
  {
    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);
    GST_CAT_LEVEL_LOG (GST_CAT_SCHEDULING,
        (ret >= GST_FLOW_EOS) ? GST_LEVEL_INFO : GST_LEVEL_WARNING,
        pad, "pullrange failed, flow: %s", gst_flow_get_name (ret));
    goto done;
  }
probe_stopped_unref:
  {
    GST_CAT_LOG_OBJECT (GST_CAT_SCHEDULING, pad,
        "post probe returned %s", gst_flow_get_name (ret));

    /* if we drop here, it signals EOS */
    if (ret == GST_FLOW_CUSTOM_SUCCESS)
      ret = GST_FLOW_EOS;

    pad->ABI.abi.last_flowret = ret;
    GST_OBJECT_UNLOCK (pad);

    if (*buffer == NULL)
      gst_buffer_unref (res_buf);
    goto done;
  }
done:
  GST_TRACER_PAD_PULL_RANGE_POST (pad, NULL, ret);
  return ret;
}

/* must be called with pad object lock */
static GstFlowReturn
store_sticky_event (GstPad * pad, GstEvent * event)
{
  guint i, len;
  GstEventType type;
  GArray *events;
  gboolean res = FALSE;
  GQuark name_id = 0;
  gboolean insert = TRUE;

  type = GST_EVENT_TYPE (event);

  /* Store all sticky events except SEGMENT/EOS when we're flushing,
   * otherwise they can be dropped and nothing would ever resend them.
   * Only do that for activated pads though, everything else is a bug!
   */
  if (G_UNLIKELY (GST_PAD_MODE (pad) == GST_PAD_MODE_NONE
          || (GST_PAD_IS_FLUSHING (pad) && (type == GST_EVENT_SEGMENT
                  || type == GST_EVENT_EOS))))
    goto flushed;

  /* Unset the EOS flag when received STREAM_START event, so pad can
   * store sticky event and then push it later */
  if (type == GST_EVENT_STREAM_START) {
    GST_LOG_OBJECT (pad, "Removing pending EOS, StreamGroupDone, TAG events");
    remove_event_by_type (pad, GST_EVENT_EOS);
    remove_event_by_type (pad, GST_EVENT_STREAM_GROUP_DONE);
    remove_event_by_type (pad, GST_EVENT_TAG);
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_EOS);
  }

  if (G_UNLIKELY (GST_PAD_IS_EOS (pad)))
    goto eos;

  if (type & GST_EVENT_TYPE_STICKY_MULTI)
    name_id = gst_structure_get_name_id (gst_event_get_structure (event));

  events = pad->priv->events;
  len = events->len;

  for (i = 0; i < len; i++) {
    PadEvent *ev = &g_array_index (events, PadEvent, i);

    if (ev->event == NULL)
      continue;

    if (type == GST_EVENT_TYPE (ev->event)) {
      /* matching types, check matching name if needed */
      if (name_id && !gst_event_has_name_id (ev->event, name_id))
        continue;

      /* overwrite */
      if ((res = gst_event_replace (&ev->event, event)))
        ev->received = FALSE;

      insert = FALSE;
      break;
    }

    if (type < GST_EVENT_TYPE (ev->event) || (type != GST_EVENT_TYPE (ev->event)
            && GST_EVENT_TYPE (ev->event) == GST_EVENT_EOS)) {
      /* STREAM_START, CAPS and SEGMENT must be delivered in this order. By
       * storing the sticky ordered we can check that this is respected. */
      if (G_UNLIKELY (GST_EVENT_TYPE (ev->event) <= GST_EVENT_SEGMENT
              || GST_EVENT_TYPE (ev->event) == GST_EVENT_EOS))
        g_warning (G_STRLOC
            ":%s:<%s:%s> Sticky event misordering, got '%s' before '%s'",
            G_STRFUNC, GST_DEBUG_PAD_NAME (pad),
            gst_event_type_get_name (GST_EVENT_TYPE (ev->event)),
            gst_event_type_get_name (type));
      break;
    }
  }
  if (insert) {
    PadEvent ev;
    ev.event = gst_event_ref (event);
    ev.received = FALSE;
    g_array_insert_val (events, i, ev);
    res = TRUE;
  }

  if (res) {
    pad->priv->events_cookie++;
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);

    GST_LOG_OBJECT (pad, "stored sticky event %s", GST_EVENT_TYPE_NAME (event));

    switch (GST_EVENT_TYPE (event)) {
      case GST_EVENT_CAPS:
        GST_OBJECT_UNLOCK (pad);

        GST_DEBUG_OBJECT (pad, "notify caps");
        g_object_notify_by_pspec ((GObject *) pad, pspec_caps);

        GST_OBJECT_LOCK (pad);
        break;
      default:
        break;
    }
  }
  if (type == GST_EVENT_EOS) {
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_EOS);
    pad->ABI.abi.last_flowret = GST_FLOW_EOS;
  }

  return GST_PAD_IS_FLUSHING (pad) ? GST_FLOW_FLUSHING : GST_FLOW_OK;

  /* ERRORS */
flushed:
  {
    GST_DEBUG_OBJECT (pad, "pad is flushing");
    return GST_FLOW_FLUSHING;
  }
eos:
  {
    GST_DEBUG_OBJECT (pad, "pad is EOS");
    return GST_FLOW_EOS;
  }
}

/**
 * gst_pad_store_sticky_event:
 * @pad: a #GstPad
 * @event: (transfer none): a #GstEvent
 *
 * Store the sticky @event on @pad
 *
 * Returns: #GST_FLOW_OK on success, #GST_FLOW_FLUSHING when the pad
 * was flushing or #GST_FLOW_EOS when the pad was EOS.
 *
 * Since: 1.2
 */
GstFlowReturn
gst_pad_store_sticky_event (GstPad * pad, GstEvent * event)
{
  GstFlowReturn ret;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_IS_EVENT (event), FALSE);

  GST_OBJECT_LOCK (pad);
  ret = store_sticky_event (pad, event);
  GST_OBJECT_UNLOCK (pad);

  return ret;
}

static gboolean
sticky_changed (GstPad * pad, PadEvent * ev, gpointer user_data)
{
  PushStickyData *data = user_data;

  /* Forward all sticky events before our current one that are pending */
  if (ev->event != data->event
      && GST_EVENT_TYPE (ev->event) < GST_EVENT_TYPE (data->event))
    return push_sticky (pad, ev, data);

  return TRUE;
}

/* should be called with pad LOCK */
static GstFlowReturn
gst_pad_push_event_unchecked (GstPad * pad, GstEvent * event,
    GstPadProbeType type)
{
  GstFlowReturn ret;
  GstPad *peerpad;
  GstEventType event_type;
  gint64 old_pad_offset = pad->offset;

  /* pass the adjusted event on. We need to do this even if
   * there is no peer pad because of the probes. */
  event = apply_pad_offset (pad, event, GST_PAD_IS_SINK (pad));

  /* Two checks to be made:
   * . (un)set the FLUSHING flag for flushing events,
   * . handle pad blocking */
  event_type = GST_EVENT_TYPE (event);
  switch (event_type) {
    case GST_EVENT_FLUSH_START:
      GST_PAD_SET_FLUSHING (pad);

      GST_PAD_BLOCK_BROADCAST (pad);
      type |= GST_PAD_PROBE_TYPE_EVENT_FLUSH;
      break;
    case GST_EVENT_FLUSH_STOP:
      if (G_UNLIKELY (!GST_PAD_IS_ACTIVE (pad)))
        goto inactive;

      GST_PAD_UNSET_FLUSHING (pad);

      /* Remove sticky EOS events */
      GST_LOG_OBJECT (pad, "Removing pending EOS events");
      remove_event_by_type (pad, GST_EVENT_EOS);
      remove_event_by_type (pad, GST_EVENT_STREAM_GROUP_DONE);
      remove_event_by_type (pad, GST_EVENT_SEGMENT);
      GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_EOS);
      pad->ABI.abi.last_flowret = GST_FLOW_OK;

      type |= GST_PAD_PROBE_TYPE_EVENT_FLUSH;
      break;
    default:
    {
      if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
        goto flushed;

      /* No need to check for EOS here as either the caller (gst_pad_push_event())
       * checked already or this is called as part of pushing sticky events,
       * in which case we still want to forward the EOS event downstream.
       */

      switch (GST_EVENT_TYPE (event)) {
        case GST_EVENT_RECONFIGURE:
          if (GST_PAD_IS_SINK (pad))
            GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_NEED_RECONFIGURE);
          break;
        default:
          break;
      }
      PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH |
          GST_PAD_PROBE_TYPE_BLOCK, event, probe_stopped);
      /* recheck sticky events because the probe might have cause a relink */
      if (GST_PAD_HAS_PENDING_EVENTS (pad) && GST_PAD_IS_SRC (pad)
          && (GST_EVENT_IS_SERIALIZED (event)
              || GST_EVENT_IS_STICKY (event))) {
        PushStickyData data = { GST_FLOW_OK, FALSE, event };
        GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_PENDING_EVENTS);

        /* Push all sticky events before our current one
         * that have changed */
        events_foreach (pad, sticky_changed, &data);
      }
      break;
    }
  }

  /* send probes after modifying the events above */
  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH, event, probe_stopped);

  /* recheck sticky events because the probe might have cause a relink */
  if (GST_PAD_HAS_PENDING_EVENTS (pad) && GST_PAD_IS_SRC (pad)
      && (GST_EVENT_IS_SERIALIZED (event)
          || GST_EVENT_IS_STICKY (event))) {
    PushStickyData data = { GST_FLOW_OK, FALSE, event };
    GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_PENDING_EVENTS);

    /* Push all sticky events before our current one
     * that have changed */
    events_foreach (pad, sticky_changed, &data);
  }

  /* the pad offset might've been changed by any of the probes above. It
   * would've been taken into account when repushing any of the sticky events
   * above but not for our current event here */
  if (G_UNLIKELY (old_pad_offset != pad->offset)) {
    event =
        _apply_pad_offset (pad, event, GST_PAD_IS_SINK (pad),
        pad->offset - old_pad_offset);
  }

  /* now check the peer pad */
  peerpad = GST_PAD_PEER (pad);
  if (peerpad == NULL)
    goto not_linked;

  gst_object_ref (peerpad);
  pad->priv->using++;
  GST_OBJECT_UNLOCK (pad);

  GST_LOG_OBJECT (pad, "sending event %p (%s) to peerpad %" GST_PTR_FORMAT,
      event, gst_event_type_get_name (event_type), peerpad);

  ret = gst_pad_send_event_unchecked (peerpad, event, type);

  /* Note: we gave away ownership of the event at this point but we can still
   * print the old pointer */
  GST_LOG_OBJECT (pad,
      "sent event %p (%s) to peerpad %" GST_PTR_FORMAT ", ret %s", event,
      gst_event_type_get_name (event_type), peerpad, gst_flow_get_name (ret));

  gst_object_unref (peerpad);

  GST_OBJECT_LOCK (pad);
  pad->priv->using--;
  if (pad->priv->using == 0) {
    /* pad is not active anymore, trigger idle callbacks */
    PROBE_NO_DATA (pad, GST_PAD_PROBE_TYPE_PUSH | GST_PAD_PROBE_TYPE_IDLE,
        idle_probe_stopped, ret);
  }
  return ret;

  /* ERROR handling */
flushed:
  {
    GST_DEBUG_OBJECT (pad, "We're flushing");
    gst_event_unref (event);
    return GST_FLOW_FLUSHING;
  }
inactive:
  {
    GST_DEBUG_OBJECT (pad, "flush-stop on inactive pad");
    gst_event_unref (event);
    return GST_FLOW_FLUSHING;
  }
probe_stopped:
  {
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);
    if (ret != GST_FLOW_CUSTOM_SUCCESS_1)
      gst_event_unref (event);

    switch (ret) {
      case GST_FLOW_CUSTOM_SUCCESS_1:
        GST_DEBUG_OBJECT (pad, "handled event");
        break;
      case GST_FLOW_CUSTOM_SUCCESS:
        GST_DEBUG_OBJECT (pad, "dropped event");
        break;
      default:
        GST_DEBUG_OBJECT (pad, "an error occurred %s", gst_flow_get_name (ret));
        break;
    }
    return ret;
  }
not_linked:
  {
    GST_DEBUG_OBJECT (pad, "Dropping event %s because pad is not linked",
        gst_event_type_get_name (GST_EVENT_TYPE (event)));
    GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_PENDING_EVENTS);
    gst_event_unref (event);

    /* unlinked pads should not influence latency configuration */
    if (event_type == GST_EVENT_LATENCY)
      return GST_FLOW_OK;

    return GST_FLOW_NOT_LINKED;
  }
idle_probe_stopped:
  {
    GST_DEBUG_OBJECT (pad, "Idle probe returned %s", gst_flow_get_name (ret));
    return ret;
  }
}

/**
 * gst_pad_push_event:
 * @pad: a #GstPad to push the event to.
 * @event: (transfer full): the #GstEvent to send to the pad.
 *
 * Sends the event to the peer of the given pad. This function is
 * mainly used by elements to send events to their peer
 * elements.
 *
 * This function takes ownership of the provided event so you should
 * gst_event_ref() it if you want to reuse the event after this call.
 *
 * Returns: %TRUE if the event was handled.
 *
 * MT safe.
 */
gboolean
gst_pad_push_event (GstPad * pad, GstEvent * event)
{
  gboolean res = FALSE;
  GstPadProbeType type;
  gboolean sticky, serialized;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (GST_IS_EVENT (event), FALSE);

  GST_TRACER_PAD_PUSH_EVENT_PRE (pad, event);

  if (GST_PAD_IS_SRC (pad)) {
    if (G_UNLIKELY (!GST_EVENT_IS_DOWNSTREAM (event)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM;
  } else if (GST_PAD_IS_SINK (pad)) {
    if (G_UNLIKELY (!GST_EVENT_IS_UPSTREAM (event)))
      goto wrong_direction;
    /* events pushed on sinkpad never are sticky */
    type = GST_PAD_PROBE_TYPE_EVENT_UPSTREAM;
  } else
    goto unknown_direction;

  GST_OBJECT_LOCK (pad);
  sticky = GST_EVENT_IS_STICKY (event);
  serialized = GST_EVENT_IS_SERIALIZED (event);

  if (sticky) {
    /* srcpad sticky events are stored immediately, the received flag is set
     * to FALSE and will be set to TRUE when we can successfully push the
     * event to the peer pad */
    switch (store_sticky_event (pad, event)) {
      case GST_FLOW_FLUSHING:
        goto flushed;
      case GST_FLOW_EOS:
        goto eos;
      default:
        break;
    }
  }
  if (GST_PAD_IS_SRC (pad) && (serialized || sticky)) {
    /* all serialized or sticky events on the srcpad trigger push of
     * sticky events */
    res = (check_sticky (pad, event) == GST_FLOW_OK);
  }
  if (!sticky) {
    GstFlowReturn ret;

    /* other events are pushed right away */
    ret = gst_pad_push_event_unchecked (pad, event, type);
    /* dropped events by a probe are not an error */
    res = (ret == GST_FLOW_OK || ret == GST_FLOW_CUSTOM_SUCCESS
        || ret == GST_FLOW_CUSTOM_SUCCESS_1);
  } else {
    /* Errors in sticky event pushing are no problem and ignored here
     * as they will cause more meaningful errors during data flow.
     * For EOS events, that are not followed by data flow, we still
     * return FALSE here though.
     */
    if (GST_EVENT_TYPE (event) != GST_EVENT_EOS)
      res = TRUE;
    gst_event_unref (event);
  }
  GST_OBJECT_UNLOCK (pad);

  GST_TRACER_PAD_PUSH_EVENT_POST (pad, res);
  return res;

  /* ERROR handling */
wrong_direction:
  {
    g_warning ("pad %s:%s pushing %s event in wrong direction",
        GST_DEBUG_PAD_NAME (pad), GST_EVENT_TYPE_NAME (event));
    gst_event_unref (event);
    goto done;
  }
unknown_direction:
  {
    g_warning ("pad %s:%s has invalid direction", GST_DEBUG_PAD_NAME (pad));
    gst_event_unref (event);
    goto done;
  }
flushed:
  {
    GST_DEBUG_OBJECT (pad, "We're flushing");
    GST_OBJECT_UNLOCK (pad);
    gst_event_unref (event);
    goto done;
  }
eos:
  {
    GST_DEBUG_OBJECT (pad, "We're EOS");
    GST_OBJECT_UNLOCK (pad);
    gst_event_unref (event);
    goto done;
  }
done:
  GST_TRACER_PAD_PUSH_EVENT_POST (pad, FALSE);
  return FALSE;
}

/* Check if we can call the event function with the given event */
static GstFlowReturn
pre_eventfunc_check (GstPad * pad, GstEvent * event)
{
  GstCaps *caps;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CAPS:
    {
      /* backwards compatibility mode for caps */
      gst_event_parse_caps (event, &caps);

      if (!gst_pad_query_accept_caps (pad, caps))
        goto not_accepted;
      break;
    }
    default:
      break;
  }
  return GST_FLOW_OK;

  /* ERRORS */
not_accepted:
  {
    GST_CAT_WARNING_OBJECT (GST_CAT_CAPS, pad,
        "caps %" GST_PTR_FORMAT " not accepted", caps);
    return GST_FLOW_NOT_NEGOTIATED;
  }
}

static GstFlowReturn
gst_pad_send_event_unchecked (GstPad * pad, GstEvent * event,
    GstPadProbeType type)
{
  GstFlowReturn ret;
  GstEventType event_type;
  gboolean serialized, need_unlock = FALSE, sticky;
  GstPadEventFunction eventfunc;
  GstPadEventFullFunction eventfullfunc = NULL;
  GstObject *parent;
  gint64 old_pad_offset;

  GST_OBJECT_LOCK (pad);

  old_pad_offset = pad->offset;
  event = apply_pad_offset (pad, event, GST_PAD_IS_SRC (pad));

  if (GST_PAD_IS_SINK (pad))
    serialized = GST_EVENT_IS_SERIALIZED (event);
  else
    serialized = FALSE;
  sticky = GST_EVENT_IS_STICKY (event);
  event_type = GST_EVENT_TYPE (event);
  switch (event_type) {
    case GST_EVENT_FLUSH_START:
      GST_CAT_DEBUG_OBJECT (GST_CAT_EVENT, pad,
          "have event type %d (FLUSH_START)", GST_EVENT_TYPE (event));

      /* can't even accept a flush begin event when flushing */
      if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
        goto flushing;

      GST_PAD_SET_FLUSHING (pad);
      GST_CAT_DEBUG_OBJECT (GST_CAT_EVENT, pad, "set flush flag");
      GST_PAD_BLOCK_BROADCAST (pad);
      type |= GST_PAD_PROBE_TYPE_EVENT_FLUSH;
      break;
    case GST_EVENT_FLUSH_STOP:
      /* we can't accept flush-stop on inactive pads else the flushing flag
       * would be cleared and it would look like the pad can accept data.
       * Also, some elements restart a streaming thread in flush-stop which we
       * can't allow on inactive pads */
      if (G_UNLIKELY (!GST_PAD_IS_ACTIVE (pad)))
        goto inactive;

      GST_PAD_UNSET_FLUSHING (pad);
      GST_CAT_DEBUG_OBJECT (GST_CAT_EVENT, pad, "cleared flush flag");
      /* Remove pending EOS events */
      GST_LOG_OBJECT (pad, "Removing pending EOS and SEGMENT events");
      remove_event_by_type (pad, GST_EVENT_EOS);
      remove_event_by_type (pad, GST_EVENT_STREAM_GROUP_DONE);
      remove_event_by_type (pad, GST_EVENT_SEGMENT);
      GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_EOS);
      pad->ABI.abi.last_flowret = GST_FLOW_OK;

      GST_OBJECT_UNLOCK (pad);
      /* grab stream lock */
      GST_PAD_STREAM_LOCK (pad);
      need_unlock = TRUE;
      GST_OBJECT_LOCK (pad);
      if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
        goto flushing;
      break;
    case GST_EVENT_RECONFIGURE:
      if (GST_PAD_IS_SRC (pad))
        GST_OBJECT_FLAG_SET (pad, GST_PAD_FLAG_NEED_RECONFIGURE);
    default:
      GST_CAT_DEBUG_OBJECT (GST_CAT_EVENT, pad,
          "have event type %" GST_PTR_FORMAT, event);

      if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
        goto flushing;

      switch (event_type) {
        case GST_EVENT_STREAM_START:
          /* Remove sticky EOS events */
          GST_LOG_OBJECT (pad, "Removing pending EOS events");
          remove_event_by_type (pad, GST_EVENT_EOS);
          remove_event_by_type (pad, GST_EVENT_STREAM_GROUP_DONE);
          remove_event_by_type (pad, GST_EVENT_TAG);
          GST_OBJECT_FLAG_UNSET (pad, GST_PAD_FLAG_EOS);
          break;
        default:
          break;
      }

      if (serialized) {
        if (G_UNLIKELY (GST_PAD_IS_EOS (pad)))
          goto eos;

        /* lock order: STREAM_LOCK, LOCK, recheck flushing. */
        GST_OBJECT_UNLOCK (pad);
        GST_PAD_STREAM_LOCK (pad);
        need_unlock = TRUE;
        GST_OBJECT_LOCK (pad);
        if (G_UNLIKELY (GST_PAD_IS_FLUSHING (pad)))
          goto flushing;

        if (G_UNLIKELY (GST_PAD_IS_EOS (pad)))
          goto eos;
      }
      break;
  }

  /* now do the probe */
  PROBE_PUSH (pad,
      type | GST_PAD_PROBE_TYPE_PUSH |
      GST_PAD_PROBE_TYPE_BLOCK, event, probe_stopped);

  PROBE_PUSH (pad, type | GST_PAD_PROBE_TYPE_PUSH, event, probe_stopped);

  /* the pad offset might've been changed by any of the probes above. It
   * would've been taken into account when repushing any of the sticky events
   * above but not for our current event here */
  if (G_UNLIKELY (old_pad_offset != pad->offset)) {
    event =
        _apply_pad_offset (pad, event, GST_PAD_IS_SRC (pad),
        pad->offset - old_pad_offset);
  }

  eventfullfunc = GST_PAD_EVENTFULLFUNC (pad);
  eventfunc = GST_PAD_EVENTFUNC (pad);
  if (G_UNLIKELY (eventfunc == NULL && eventfullfunc == NULL))
    goto no_function;

  ACQUIRE_PARENT (pad, parent, no_parent);
  GST_OBJECT_UNLOCK (pad);

  ret = pre_eventfunc_check (pad, event);
  if (G_UNLIKELY (ret != GST_FLOW_OK))
    goto precheck_failed;

  if (sticky)
    gst_event_ref (event);

  if (eventfullfunc) {
    ret = eventfullfunc (pad, parent, event);
  } else if (eventfunc (pad, parent, event)) {
    ret = GST_FLOW_OK;
  } else {
    /* something went wrong */
    switch (event_type) {
      case GST_EVENT_CAPS:
        ret = GST_FLOW_NOT_NEGOTIATED;
        break;
      default:
        ret = GST_FLOW_ERROR;
        break;
    }
  }
  RELEASE_PARENT (parent);

  GST_DEBUG_OBJECT (pad, "sent event, ret %s", gst_flow_get_name (ret));

  if (sticky) {
    if (ret == GST_FLOW_OK) {
      GST_OBJECT_LOCK (pad);
      /* after the event function accepted the event, we can store the sticky
       * event on the pad */
      switch (store_sticky_event (pad, event)) {
        case GST_FLOW_FLUSHING:
          goto flushing;
        case GST_FLOW_EOS:
          goto eos;
        default:
          break;
      }
      GST_OBJECT_UNLOCK (pad);
    }
    gst_event_unref (event);
  }

  if (need_unlock)
    GST_PAD_STREAM_UNLOCK (pad);

  return ret;

  /* ERROR handling */
flushing:
  {
    GST_OBJECT_UNLOCK (pad);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    GST_CAT_INFO_OBJECT (GST_CAT_EVENT, pad,
        "Received event on flushing pad. Discarding");
    gst_event_unref (event);
    return GST_FLOW_FLUSHING;
  }
inactive:
  {
    GST_OBJECT_UNLOCK (pad);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    GST_CAT_INFO_OBJECT (GST_CAT_EVENT, pad,
        "Received flush-stop on inactive pad. Discarding");
    gst_event_unref (event);
    return GST_FLOW_FLUSHING;
  }
eos:
  {
    GST_OBJECT_UNLOCK (pad);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    GST_CAT_INFO_OBJECT (GST_CAT_EVENT, pad,
        "Received event on EOS pad. Discarding");
    gst_event_unref (event);
    return GST_FLOW_EOS;
  }
probe_stopped:
  {
    GST_OBJECT_UNLOCK (pad);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    /* Only unref if unhandled */
    if (ret != GST_FLOW_CUSTOM_SUCCESS_1)
      gst_event_unref (event);

    switch (ret) {
      case GST_FLOW_CUSTOM_SUCCESS_1:
      case GST_FLOW_CUSTOM_SUCCESS:
        GST_DEBUG_OBJECT (pad, "dropped or handled event");
        ret = GST_FLOW_OK;
        break;
      default:
        GST_DEBUG_OBJECT (pad, "an error occurred %s", gst_flow_get_name (ret));
        break;
    }
    return ret;
  }
no_function:
  {
    g_warning ("pad %s:%s has no event handler, file a bug.",
        GST_DEBUG_PAD_NAME (pad));
    GST_OBJECT_UNLOCK (pad);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    gst_event_unref (event);
    return GST_FLOW_NOT_SUPPORTED;
  }
no_parent:
  {
    GST_DEBUG_OBJECT (pad, "no parent");
    GST_OBJECT_UNLOCK (pad);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    gst_event_unref (event);
    return GST_FLOW_FLUSHING;
  }
precheck_failed:
  {
    GST_DEBUG_OBJECT (pad, "pre event check failed");
    RELEASE_PARENT (parent);
    if (need_unlock)
      GST_PAD_STREAM_UNLOCK (pad);
    gst_event_unref (event);
    return ret;
  }
}

/**
 * gst_pad_send_event:
 * @pad: a #GstPad to send the event to.
 * @event: (transfer full): the #GstEvent to send to the pad.
 *
 * Sends the event to the pad. This function can be used
 * by applications to send events in the pipeline.
 *
 * If @pad is a source pad, @event should be an upstream event. If @pad is a
 * sink pad, @event should be a downstream event. For example, you would not
 * send a #GST_EVENT_EOS on a src pad; EOS events only propagate downstream.
 * Furthermore, some downstream events have to be serialized with data flow,
 * like EOS, while some can travel out-of-band, like #GST_EVENT_FLUSH_START. If
 * the event needs to be serialized with data flow, this function will take the
 * pad's stream lock while calling its event function.
 *
 * To find out whether an event type is upstream, downstream, or downstream and
 * serialized, see #GstEventTypeFlags, gst_event_type_get_flags(),
 * #GST_EVENT_IS_UPSTREAM, #GST_EVENT_IS_DOWNSTREAM, and
 * #GST_EVENT_IS_SERIALIZED. Note that in practice that an application or
 * plugin doesn't need to bother itself with this information; the core handles
 * all necessary locks and checks.
 *
 * This function takes ownership of the provided event so you should
 * gst_event_ref() it if you want to reuse the event after this call.
 *
 * Returns: %TRUE if the event was handled.
 */
gboolean
gst_pad_send_event (GstPad * pad, GstEvent * event)
{
  gboolean result;
  GstPadProbeType type;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (event != NULL, FALSE);

  if (GST_PAD_IS_SINK (pad)) {
    if (G_UNLIKELY (!GST_EVENT_IS_DOWNSTREAM (event)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM;
  } else if (GST_PAD_IS_SRC (pad)) {
    if (G_UNLIKELY (!GST_EVENT_IS_UPSTREAM (event)))
      goto wrong_direction;
    type = GST_PAD_PROBE_TYPE_EVENT_UPSTREAM;
  } else
    goto unknown_direction;

  if (gst_pad_send_event_unchecked (pad, event, type) != GST_FLOW_OK)
    result = FALSE;
  else
    result = TRUE;

  return result;

  /* ERROR handling */
wrong_direction:
  {
    g_warning ("pad %s:%s sending %s event in wrong direction",
        GST_DEBUG_PAD_NAME (pad), GST_EVENT_TYPE_NAME (event));
    gst_event_unref (event);
    return FALSE;
  }
unknown_direction:
  {
    g_warning ("pad %s:%s has invalid direction", GST_DEBUG_PAD_NAME (pad));
    gst_event_unref (event);
    return FALSE;
  }
}

/**
 * gst_pad_set_element_private:
 * @pad: the #GstPad to set the private data of.
 * @priv: The private data to attach to the pad.
 *
 * Set the given private data gpointer on the pad.
 * This function can only be used by the element that owns the pad.
 * No locking is performed in this function.
 */
void
gst_pad_set_element_private (GstPad * pad, gpointer priv)
{
  pad->element_private = priv;
}

/**
 * gst_pad_get_element_private:
 * @pad: the #GstPad to get the private data of.
 *
 * Gets the private data of a pad.
 * No locking is performed in this function.
 *
 * Returns: (transfer none) (nullable): a #gpointer to the private data.
 */
gpointer
gst_pad_get_element_private (GstPad * pad)
{
  return pad->element_private;
}

/**
 * gst_pad_get_sticky_event:
 * @pad: the #GstPad to get the event from.
 * @event_type: the #GstEventType that should be retrieved.
 * @idx: the index of the event
 *
 * Returns a new reference of the sticky event of type @event_type
 * from the event.
 *
 * Returns: (transfer full) (nullable): a #GstEvent of type
 * @event_type or %NULL when no event of @event_type was on
 * @pad. Unref after usage.
 */
GstEvent *
gst_pad_get_sticky_event (GstPad * pad, GstEventType event_type, guint idx)
{
  GstEvent *event = NULL;
  PadEvent *ev;

  g_return_val_if_fail (GST_IS_PAD (pad), NULL);
  g_return_val_if_fail ((event_type & GST_EVENT_TYPE_STICKY) != 0, NULL);

  GST_OBJECT_LOCK (pad);
  ev = find_event_by_type (pad, event_type, idx);
  if (ev && (event = ev->event))
    gst_event_ref (event);
  GST_OBJECT_UNLOCK (pad);

  return event;
}

typedef struct
{
  GstPadStickyEventsForeachFunction func;
  gpointer user_data;
} ForeachDispatch;

static gboolean
foreach_dispatch_function (GstPad * pad, PadEvent * ev, gpointer user_data)
{
  ForeachDispatch *data = user_data;
  gboolean ret = TRUE;

  if (ev->event) {
    GST_OBJECT_UNLOCK (pad);

    ret = data->func (pad, &ev->event, data->user_data);

    GST_OBJECT_LOCK (pad);
  }

  return ret;
}

/**
 * gst_pad_sticky_events_foreach:
 * @pad: the #GstPad that should be used for iteration.
 * @foreach_func: (scope call): the #GstPadStickyEventsForeachFunction that
 *                should be called for every event.
 * @user_data: (closure): the optional user data.
 *
 * Iterates all sticky events on @pad and calls @foreach_func for every
 * event. If @foreach_func returns %FALSE the iteration is immediately stopped.
 */
void
gst_pad_sticky_events_foreach (GstPad * pad,
    GstPadStickyEventsForeachFunction foreach_func, gpointer user_data)
{
  ForeachDispatch data;

  g_return_if_fail (GST_IS_PAD (pad));
  g_return_if_fail (foreach_func != NULL);

  data.func = foreach_func;
  data.user_data = user_data;

  GST_OBJECT_LOCK (pad);
  events_foreach (pad, foreach_dispatch_function, &data);
  GST_OBJECT_UNLOCK (pad);
}

static void
do_stream_status (GstPad * pad, GstStreamStatusType type,
    GThread * thread, GstTask * task)
{
  GstElement *parent;

  GST_DEBUG_OBJECT (pad, "doing stream-status %d", type);

  if ((parent = GST_ELEMENT_CAST (gst_pad_get_parent (pad)))) {
    if (GST_IS_ELEMENT (parent)) {
      GstMessage *message;
      GValue value = { 0 };

      if (type == GST_STREAM_STATUS_TYPE_ENTER) {
        gchar *tname;

        /* create a good task name (we can directly grab the parent and pad
         * names since they both exist at this point, and changing the name of
         * parent and pad when a pad is activating is a big no-no). */
        tname = g_strdup_printf ("%s:%s", GST_DEBUG_PAD_NAME (pad));

        gst_object_set_name (GST_OBJECT_CAST (task), tname);
        g_free (tname);
      }

      message = gst_message_new_stream_status (GST_OBJECT_CAST (pad),
          type, parent);

      g_value_init (&value, GST_TYPE_TASK);
      g_value_set_object (&value, task);
      gst_message_set_stream_status_object (message, &value);
      g_value_unset (&value);

      GST_DEBUG_OBJECT (pad, "posting stream-status %d", type);
      gst_element_post_message (parent, message);
    }
    gst_object_unref (parent);
  }
}

static void
pad_enter_thread (GstTask * task, GThread * thread, gpointer user_data)
{
  do_stream_status (GST_PAD_CAST (user_data), GST_STREAM_STATUS_TYPE_ENTER,
      thread, task);
}

static void
pad_leave_thread (GstTask * task, GThread * thread, gpointer user_data)
{
  do_stream_status (GST_PAD_CAST (user_data), GST_STREAM_STATUS_TYPE_LEAVE,
      thread, task);
}

/**
 * gst_pad_start_task:
 * @pad: the #GstPad to start the task of
 * @func: the task function to call
 * @user_data: user data passed to the task function
 * @notify: called when @user_data is no longer referenced
 *
 * Starts a task that repeatedly calls @func with @user_data. This function
 * is mostly used in pad activation functions to start the dataflow.
 * The #GST_PAD_STREAM_LOCK of @pad will automatically be acquired
 * before @func is called.
 *
 * Returns: a %TRUE if the task could be started.
 */
gboolean
gst_pad_start_task (GstPad * pad, GstTaskFunction func, gpointer user_data,
    GDestroyNotify notify)
{
  GstTask *task;
  gboolean res;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);
  g_return_val_if_fail (func != NULL, FALSE);

  GST_DEBUG_OBJECT (pad, "start task");

  GST_OBJECT_LOCK (pad);
  task = GST_PAD_TASK (pad);
  if (task == NULL) {
    task = gst_task_new (func, user_data, notify);
    gst_task_set_lock (task, GST_PAD_GET_STREAM_LOCK (pad));
    gst_task_set_enter_callback (task, pad_enter_thread, pad, NULL);
    gst_task_set_leave_callback (task, pad_leave_thread, pad, NULL);
    GST_INFO_OBJECT (pad, "created task %p", task);
    GST_PAD_TASK (pad) = task;
    gst_object_ref (task);
    /* release lock to post the message */
    GST_OBJECT_UNLOCK (pad);

    do_stream_status (pad, GST_STREAM_STATUS_TYPE_CREATE, NULL, task);

    gst_object_unref (task);

    GST_OBJECT_LOCK (pad);
    /* nobody else is supposed to have changed the pad now */
    if (GST_PAD_TASK (pad) != task)
      goto concurrent_stop;
  }
  res = gst_task_set_state (task, GST_TASK_STARTED);
  GST_OBJECT_UNLOCK (pad);

  return res;

  /* ERRORS */
concurrent_stop:
  {
    GST_OBJECT_UNLOCK (pad);
    return TRUE;
  }
}

/**
 * gst_pad_pause_task:
 * @pad: the #GstPad to pause the task of
 *
 * Pause the task of @pad. This function will also wait until the
 * function executed by the task is finished if this function is not
 * called from the task function.
 *
 * Returns: a %TRUE if the task could be paused or %FALSE when the pad
 * has no task.
 */
gboolean
gst_pad_pause_task (GstPad * pad)
{
  GstTask *task;
  gboolean res;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_DEBUG_OBJECT (pad, "pause task");

  GST_OBJECT_LOCK (pad);
  task = GST_PAD_TASK (pad);
  if (task == NULL)
    goto no_task;
  res = gst_task_set_state (task, GST_TASK_PAUSED);
  /* unblock activation waits if any */
  pad->priv->in_activation = FALSE;
  g_cond_broadcast (&pad->priv->activation_cond);
  GST_OBJECT_UNLOCK (pad);

  /* wait for task function to finish, this lock is recursive so it does nothing
   * when the pause is called from the task itself */
  GST_PAD_STREAM_LOCK (pad);
  GST_PAD_STREAM_UNLOCK (pad);

  return res;

no_task:
  {
    GST_DEBUG_OBJECT (pad, "pad has no task");
    GST_OBJECT_UNLOCK (pad);
    return FALSE;
  }
}

/**
 * gst_pad_get_task_state:
 * @pad: the #GstPad to get task state from
 *
 * Get @pad task state. If no task is currently
 * set, #GST_TASK_STOPPED is returned.
 *
 * Returns: The current state of @pad's task.
 *
 * Since: 1.12
 */
GstTaskState
gst_pad_get_task_state (GstPad * pad)
{
  GstTask *task;
  GstTaskState res;

  g_return_val_if_fail (GST_IS_PAD (pad), GST_TASK_STOPPED);

  GST_OBJECT_LOCK (pad);
  task = GST_PAD_TASK (pad);
  if (task == NULL)
    goto no_task;
  res = gst_task_get_state (task);
  GST_OBJECT_UNLOCK (pad);

  return res;

no_task:
  {
    GST_DEBUG_OBJECT (pad, "pad has no task");
    GST_OBJECT_UNLOCK (pad);
    return GST_TASK_STOPPED;
  }
}

/**
 * gst_pad_stop_task:
 * @pad: the #GstPad to stop the task of
 *
 * Stop the task of @pad. This function will also make sure that the
 * function executed by the task will effectively stop if not called
 * from the GstTaskFunction.
 *
 * This function will deadlock if called from the GstTaskFunction of
 * the task. Use gst_task_pause() instead.
 *
 * Regardless of whether the pad has a task, the stream lock is acquired and
 * released so as to ensure that streaming through this pad has finished.
 *
 * Returns: a %TRUE if the task could be stopped or %FALSE on error.
 */
gboolean
gst_pad_stop_task (GstPad * pad)
{
  GstTask *task;
  gboolean res;

  g_return_val_if_fail (GST_IS_PAD (pad), FALSE);

  GST_DEBUG_OBJECT (pad, "stop task");

  GST_OBJECT_LOCK (pad);
  task = GST_PAD_TASK (pad);
  if (task == NULL)
    goto no_task;
  GST_PAD_TASK (pad) = NULL;
  res = gst_task_set_state (task, GST_TASK_STOPPED);
  /* unblock activation waits if any */
  pad->priv->in_activation = FALSE;
  g_cond_broadcast (&pad->priv->activation_cond);
  GST_OBJECT_UNLOCK (pad);

  GST_PAD_STREAM_LOCK (pad);
  GST_PAD_STREAM_UNLOCK (pad);

  if (!gst_task_join (task))
    goto join_failed;

  gst_object_unref (task);

  return res;

no_task:
  {
    GST_DEBUG_OBJECT (pad, "no task");
    GST_OBJECT_UNLOCK (pad);

    GST_PAD_STREAM_LOCK (pad);
    GST_PAD_STREAM_UNLOCK (pad);

    /* this is not an error */
    return TRUE;
  }
join_failed:
  {
    /* this is bad, possibly the application tried to join the task from
     * the task's thread. We install the task again so that it will be stopped
     * again from the right thread next time hopefully. */
    GST_OBJECT_LOCK (pad);
    GST_DEBUG_OBJECT (pad, "join failed");
    /* we can only install this task if there was no other task */
    if (GST_PAD_TASK (pad) == NULL)
      GST_PAD_TASK (pad) = task;
    GST_OBJECT_UNLOCK (pad);

    return FALSE;
  }
}

/**
 * gst_pad_probe_info_get_event:
 * @info: a #GstPadProbeInfo
 *
 * Returns: (transfer none) (nullable): The #GstEvent from the probe
 */

GstEvent *
gst_pad_probe_info_get_event (GstPadProbeInfo * info)
{
  g_return_val_if_fail (info->type & (GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM |
          GST_PAD_PROBE_TYPE_EVENT_UPSTREAM), NULL);

  return GST_PAD_PROBE_INFO_EVENT (info);
}


/**
 * gst_pad_probe_info_get_query:
 * @info: a #GstPadProbeInfo
 *
 * Returns: (transfer none) (nullable): The #GstQuery from the probe
 */

GstQuery *
gst_pad_probe_info_get_query (GstPadProbeInfo * info)
{
  g_return_val_if_fail (info->type & (GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM |
          GST_PAD_PROBE_TYPE_QUERY_UPSTREAM), NULL);

  return GST_PAD_PROBE_INFO_QUERY (info);
}

/**
 * gst_pad_probe_info_get_buffer:
 * @info: a #GstPadProbeInfo
 *
 * Returns: (transfer none) (nullable): The #GstBuffer from the probe
 */

GstBuffer *
gst_pad_probe_info_get_buffer (GstPadProbeInfo * info)
{
  g_return_val_if_fail (info->type & GST_PAD_PROBE_TYPE_BUFFER, NULL);

  return GST_PAD_PROBE_INFO_BUFFER (info);
}

/**
 * gst_pad_probe_info_get_buffer_list:
 * @info: a #GstPadProbeInfo
 *
 * Returns: (transfer none) (nullable): The #GstBufferList from the probe
 */

GstBufferList *
gst_pad_probe_info_get_buffer_list (GstPadProbeInfo * info)
{
  g_return_val_if_fail (info->type & GST_PAD_PROBE_TYPE_BUFFER_LIST, NULL);

  return GST_PAD_PROBE_INFO_BUFFER_LIST (info);
}

/**
 * gst_pad_get_last_flow_return:
 * @pad: the #GstPad
 *
 * Gets the #GstFlowReturn return from the last data passed by this pad.
 *
 * Since: 1.4
 */
GstFlowReturn
gst_pad_get_last_flow_return (GstPad * pad)
{
  GstFlowReturn ret;

  GST_OBJECT_LOCK (pad);
  ret = GST_PAD_LAST_FLOW_RETURN (pad);
  GST_OBJECT_UNLOCK (pad);

  return ret;
}
