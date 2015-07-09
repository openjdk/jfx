/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *                    2003 Colin Walters <cwalters@gnome.org>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *
 * gstqueue.c:
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
 * SECTION:element-queue
 *
 * Data is queued until one of the limits specified by the
 * #GstQueue:max-size-buffers, #GstQueue:max-size-bytes and/or
 * #GstQueue:max-size-time properties has been reached. Any attempt to push
 * more buffers into the queue will block the pushing thread until more space
 * becomes available.
 *
 * The queue will create a new thread on the source pad to decouple the
 * processing on sink and source pad.
 *
 * You can query how many buffers are queued by reading the
 * #GstQueue:current-level-buffers property. You can track changes
 * by connecting to the notify::current-level-buffers signal (which
 * like all signals will be emitted from the streaming thread). The same
 * applies to the #GstQueue:current-level-time and
 * #GstQueue:current-level-bytes properties.
 *
 * The default queue size limits are 200 buffers, 10MB of data, or
 * one second worth of data, whichever is reached first.
 *
 * As said earlier, the queue blocks by default when one of the specified
 * maximums (bytes, time, buffers) has been reached. You can set the
 * #GstQueue:leaky property to specify that instead of blocking it should
 * leak (drop) new or old buffers.
 *
 * The #GstQueue::underrun signal is emitted when the queue has less data than
 * the specified minimum thresholds require (by default: when the queue is
 * empty). The #GstQueue::overrun signal is emitted when the queue is filled
 * up. Both signals are emitted from the context of the streaming thread.
 */

#include "gst/gst_private.h"

#include <gst/gst.h>
#include "gstqueue.h"

#include "../../gst/gst-i18n-lib.h"

static GstStaticPadTemplate sinktemplate = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

GST_DEBUG_CATEGORY_STATIC (queue_debug);
#define GST_CAT_DEFAULT (queue_debug)
GST_DEBUG_CATEGORY_STATIC (queue_dataflow);

#define STATUS(queue, pad, msg) \
  GST_CAT_LOG_OBJECT (queue_dataflow, queue, \
                      "(%s:%s) " msg ": %u of %u-%u buffers, %u of %u-%u " \
                      "bytes, %" G_GUINT64_FORMAT " of %" G_GUINT64_FORMAT \
                      "-%" G_GUINT64_FORMAT " ns, %u items", \
                      GST_DEBUG_PAD_NAME (pad), \
                      queue->cur_level.buffers, \
                      queue->min_threshold.buffers, \
                      queue->max_size.buffers, \
                      queue->cur_level.bytes, \
                      queue->min_threshold.bytes, \
                      queue->max_size.bytes, \
                      queue->cur_level.time, \
                      queue->min_threshold.time, \
                      queue->max_size.time, \
                      queue->queue->length)

/* Queue signals and args */
enum
{
  SIGNAL_UNDERRUN,
  SIGNAL_RUNNING,
  SIGNAL_OVERRUN,
  SIGNAL_PUSHING,
  LAST_SIGNAL
};

enum
{
  PROP_0,
  /* FIXME: don't we have another way of doing this
   * "Gstreamer format" (frame/byte/time) queries? */
  PROP_CUR_LEVEL_BUFFERS,
  PROP_CUR_LEVEL_BYTES,
  PROP_CUR_LEVEL_TIME,
  PROP_MAX_SIZE_BUFFERS,
  PROP_MAX_SIZE_BYTES,
  PROP_MAX_SIZE_TIME,
  PROP_MIN_THRESHOLD_BUFFERS,
  PROP_MIN_THRESHOLD_BYTES,
  PROP_MIN_THRESHOLD_TIME,
  PROP_LEAKY,
  PROP_SILENT
};

/* default property values */
#define DEFAULT_MAX_SIZE_BUFFERS  200   /* 200 buffers */
#define DEFAULT_MAX_SIZE_BYTES    (10 * 1024 * 1024)    /* 10 MB       */
#define DEFAULT_MAX_SIZE_TIME     GST_SECOND    /* 1 second    */

#define GST_QUEUE_MUTEX_LOCK(q) G_STMT_START {                          \
  g_mutex_lock (q->qlock);                                              \
} G_STMT_END

#define GST_QUEUE_MUTEX_LOCK_CHECK(q,label) G_STMT_START {              \
  GST_QUEUE_MUTEX_LOCK (q);                                             \
  if (q->srcresult != GST_FLOW_OK)                                      \
    goto label;                                                         \
} G_STMT_END

#define GST_QUEUE_MUTEX_UNLOCK(q) G_STMT_START {                        \
  g_mutex_unlock (q->qlock);                                            \
} G_STMT_END

#define GST_QUEUE_WAIT_DEL_CHECK(q, label) G_STMT_START {               \
  STATUS (q, q->sinkpad, "wait for DEL");                               \
  q->waiting_del = TRUE;                                                \
  g_cond_wait (q->item_del, q->qlock);                                  \
  q->waiting_del = FALSE;                                               \
  if (q->srcresult != GST_FLOW_OK) {                                    \
    STATUS (q, q->srcpad, "received DEL wakeup");                       \
    goto label;                                                         \
  }                                                                     \
  STATUS (q, q->sinkpad, "received DEL");                               \
} G_STMT_END

#define GST_QUEUE_WAIT_ADD_CHECK(q, label) G_STMT_START {               \
  STATUS (q, q->srcpad, "wait for ADD");                                \
  q->waiting_add = TRUE;                                                \
  g_cond_wait (q->item_add, q->qlock);                                  \
  q->waiting_add = FALSE;                                               \
  if (q->srcresult != GST_FLOW_OK) {                                    \
    STATUS (q, q->srcpad, "received ADD wakeup");                       \
    goto label;                                                         \
  }                                                                     \
  STATUS (q, q->srcpad, "received ADD");                                \
} G_STMT_END

#define GST_QUEUE_SIGNAL_DEL(q) G_STMT_START {                          \
  if (q->waiting_del) {                                                 \
    STATUS (q, q->srcpad, "signal DEL");                                \
    g_cond_signal (q->item_del);                                        \
  }                                                                     \
} G_STMT_END

#define GST_QUEUE_SIGNAL_ADD(q) G_STMT_START {                          \
  if (q->waiting_add) {                                                 \
    STATUS (q, q->sinkpad, "signal ADD");                               \
    g_cond_signal (q->item_add);                                        \
  }                                                                     \
} G_STMT_END

#define _do_init(bla) \
    GST_DEBUG_CATEGORY_INIT (queue_debug, "queue", 0, "queue element"); \
    GST_DEBUG_CATEGORY_INIT (queue_dataflow, "queue_dataflow", 0, \
        "dataflow inside the queue element");

GST_BOILERPLATE_FULL (GstQueue, gst_queue, GstElement,
    GST_TYPE_ELEMENT, _do_init);

static void gst_queue_finalize (GObject * object);

static void gst_queue_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_queue_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static GstFlowReturn gst_queue_chain (GstPad * pad, GstBuffer * buffer);
static GstFlowReturn gst_queue_bufferalloc (GstPad * pad, guint64 offset,
    guint size, GstCaps * caps, GstBuffer ** buf);
static GstFlowReturn gst_queue_push_one (GstQueue * queue);
static void gst_queue_loop (GstPad * pad);

static gboolean gst_queue_handle_sink_event (GstPad * pad, GstEvent * event);

static gboolean gst_queue_handle_src_event (GstPad * pad, GstEvent * event);
static gboolean gst_queue_handle_src_query (GstPad * pad, GstQuery * query);

static gboolean gst_queue_acceptcaps (GstPad * pad, GstCaps * caps);
static GstCaps *gst_queue_getcaps (GstPad * pad);
static GstPadLinkReturn gst_queue_link_sink (GstPad * pad, GstPad * peer);
static GstPadLinkReturn gst_queue_link_src (GstPad * pad, GstPad * peer);
static void gst_queue_locked_flush (GstQueue * queue);

static gboolean gst_queue_src_activate_push (GstPad * pad, gboolean active);
static gboolean gst_queue_sink_activate_push (GstPad * pad, gboolean active);

static gboolean gst_queue_is_empty (GstQueue * queue);
static gboolean gst_queue_is_filled (GstQueue * queue);

#ifdef GSTREAMER_LITE
static GstStateChangeReturn gst_queue_change_state (GstElement *element, GstStateChange transition);
#endif

#define GST_TYPE_QUEUE_LEAKY (queue_leaky_get_type ())

static GType
queue_leaky_get_type (void)
{
  static GType queue_leaky_type = 0;
  static const GEnumValue queue_leaky[] = {
    {GST_QUEUE_NO_LEAK, "Not Leaky", "no"},
    {GST_QUEUE_LEAK_UPSTREAM, "Leaky on upstream (new buffers)", "upstream"},
    {GST_QUEUE_LEAK_DOWNSTREAM, "Leaky on downstream (old buffers)",
        "downstream"},
    {0, NULL, NULL},
  };

  if (!queue_leaky_type) {
    queue_leaky_type = g_enum_register_static ("GstQueueLeaky", queue_leaky);
  }
  return queue_leaky_type;
}

static guint gst_queue_signals[LAST_SIGNAL] = { 0 };

static void
gst_queue_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (gstelement_class,
      "Queue",
      "Generic", "Simple data queue", "Erik Walthinsen <omega@cse.ogi.edu>");
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sinktemplate));
}

static void
gst_queue_class_init (GstQueueClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->set_property = gst_queue_set_property;
  gobject_class->get_property = gst_queue_get_property;

  /* signals */
  /**
   * GstQueue::underrun:
   * @queue: the queue instance
   *
   * Reports that the buffer became empty (underrun).
   * A buffer is empty if the total amount of data inside it (num-buffers, time,
   * size) is lower than the boundary values which can be set through the
   * GObject properties.
   */
  gst_queue_signals[SIGNAL_UNDERRUN] =
      g_signal_new ("underrun", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_FIRST,
      G_STRUCT_OFFSET (GstQueueClass, underrun), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);
  /**
   * GstQueue::running:
   * @queue: the queue instance
   *
   * Reports that enough (min-threshold) data is in the queue. Use this signal
   * together with the underrun signal to pause the pipeline on underrun and
   * wait for the queue to fill-up before resume playback.
   */
  gst_queue_signals[SIGNAL_RUNNING] =
      g_signal_new ("running", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_FIRST,
      G_STRUCT_OFFSET (GstQueueClass, running), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);
  /**
   * GstQueue::overrun:
   * @queue: the queue instance
   *
   * Reports that the buffer became full (overrun).
   * A buffer is full if the total amount of data inside it (num-buffers, time,
   * size) is higher than the boundary values which can be set through the
   * GObject properties.
   */
  gst_queue_signals[SIGNAL_OVERRUN] =
      g_signal_new ("overrun", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_FIRST,
      G_STRUCT_OFFSET (GstQueueClass, overrun), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);
  /**
   * GstQueue::pushing:
   * @queue: the queue instance
   *
   * Reports when the queue has enough data to start pushing data again on the
   * source pad.
   */
  gst_queue_signals[SIGNAL_PUSHING] =
      g_signal_new ("pushing", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_FIRST,
      G_STRUCT_OFFSET (GstQueueClass, pushing), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  /* properties */
  g_object_class_install_property (gobject_class, PROP_CUR_LEVEL_BYTES,
      g_param_spec_uint ("current-level-bytes", "Current level (kB)",
          "Current amount of data in the queue (bytes)",
          0, G_MAXUINT, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_CUR_LEVEL_BUFFERS,
      g_param_spec_uint ("current-level-buffers", "Current level (buffers)",
          "Current number of buffers in the queue",
          0, G_MAXUINT, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_CUR_LEVEL_TIME,
      g_param_spec_uint64 ("current-level-time", "Current level (ns)",
          "Current amount of data in the queue (in ns)",
          0, G_MAXUINT64, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_MAX_SIZE_BYTES,
      g_param_spec_uint ("max-size-bytes", "Max. size (kB)",
          "Max. amount of data in the queue (bytes, 0=disable)",
          0, G_MAXUINT, DEFAULT_MAX_SIZE_BYTES,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MAX_SIZE_BUFFERS,
      g_param_spec_uint ("max-size-buffers", "Max. size (buffers)",
          "Max. number of buffers in the queue (0=disable)", 0, G_MAXUINT,
          DEFAULT_MAX_SIZE_BUFFERS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MAX_SIZE_TIME,
      g_param_spec_uint64 ("max-size-time", "Max. size (ns)",
          "Max. amount of data in the queue (in ns, 0=disable)", 0, G_MAXUINT64,
          DEFAULT_MAX_SIZE_TIME, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_MIN_THRESHOLD_BYTES,
      g_param_spec_uint ("min-threshold-bytes", "Min. threshold (kB)",
          "Min. amount of data in the queue to allow reading (bytes, 0=disable)",
          0, G_MAXUINT, 0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MIN_THRESHOLD_BUFFERS,
      g_param_spec_uint ("min-threshold-buffers", "Min. threshold (buffers)",
          "Min. number of buffers in the queue to allow reading (0=disable)",
          0, G_MAXUINT, 0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MIN_THRESHOLD_TIME,
      g_param_spec_uint64 ("min-threshold-time", "Min. threshold (ns)",
          "Min. amount of data in the queue to allow reading (in ns, 0=disable)",
          0, G_MAXUINT64, 0, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_LEAKY,
      g_param_spec_enum ("leaky", "Leaky",
          "Where the queue leaks, if at all",
          GST_TYPE_QUEUE_LEAKY, GST_QUEUE_NO_LEAK,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstQueue:silent
   *
   * Don't emit queue signals. Makes queues more lightweight if no signals are
   * needed.
   *
   * Since: 0.10.31
   */
  g_object_class_install_property (gobject_class, PROP_SILENT,
      g_param_spec_boolean ("silent", "Silent",
          "Don't emit queue signals", FALSE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gobject_class->finalize = gst_queue_finalize;

#ifdef GSTREAMER_LITE
  GST_ELEMENT_CLASS (klass)->change_state = GST_DEBUG_FUNCPTR(gst_queue_change_state);
#endif

  /* Registering debug symbols for function pointers */
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_chain);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_sink_activate_push);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_handle_sink_event);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_link_sink);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_getcaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_acceptcaps);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_bufferalloc);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_src_activate_push);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_link_src);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_handle_src_event);
  GST_DEBUG_REGISTER_FUNCPTR (gst_queue_handle_src_query);
}

static void
gst_queue_init (GstQueue * queue, GstQueueClass * g_class)
{
  queue->sinkpad = gst_pad_new_from_static_template (&sinktemplate, "sink");

  gst_pad_set_chain_function (queue->sinkpad, gst_queue_chain);
  gst_pad_set_activatepush_function (queue->sinkpad,
      gst_queue_sink_activate_push);
  gst_pad_set_event_function (queue->sinkpad, gst_queue_handle_sink_event);
  gst_pad_set_link_function (queue->sinkpad, gst_queue_link_sink);
  gst_pad_set_getcaps_function (queue->sinkpad, gst_queue_getcaps);
  gst_pad_set_acceptcaps_function (queue->sinkpad, gst_queue_acceptcaps);
  gst_pad_set_bufferalloc_function (queue->sinkpad, gst_queue_bufferalloc);
  gst_element_add_pad (GST_ELEMENT (queue), queue->sinkpad);

  queue->srcpad = gst_pad_new_from_static_template (&srctemplate, "src");

  gst_pad_set_activatepush_function (queue->srcpad,
      gst_queue_src_activate_push);
  gst_pad_set_link_function (queue->srcpad, gst_queue_link_src);
  gst_pad_set_acceptcaps_function (queue->srcpad, gst_queue_acceptcaps);
  gst_pad_set_getcaps_function (queue->srcpad, gst_queue_getcaps);
  gst_pad_set_event_function (queue->srcpad, gst_queue_handle_src_event);
  gst_pad_set_query_function (queue->srcpad, gst_queue_handle_src_query);
  gst_element_add_pad (GST_ELEMENT (queue), queue->srcpad);

  GST_QUEUE_CLEAR_LEVEL (queue->cur_level);
  queue->max_size.buffers = DEFAULT_MAX_SIZE_BUFFERS;
  queue->max_size.bytes = DEFAULT_MAX_SIZE_BYTES;
  queue->max_size.time = DEFAULT_MAX_SIZE_TIME;
  GST_QUEUE_CLEAR_LEVEL (queue->min_threshold);
  GST_QUEUE_CLEAR_LEVEL (queue->orig_min_threshold);
  gst_segment_init (&queue->sink_segment, GST_FORMAT_TIME);
  gst_segment_init (&queue->src_segment, GST_FORMAT_TIME);
  queue->head_needs_discont = queue->tail_needs_discont = FALSE;

  queue->leaky = GST_QUEUE_NO_LEAK;
  queue->srcresult = GST_FLOW_WRONG_STATE;

  queue->qlock = g_mutex_new ();
  queue->item_add = g_cond_new ();
  queue->item_del = g_cond_new ();
  queue->queue = g_queue_new ();

  queue->sinktime = GST_CLOCK_TIME_NONE;
  queue->srctime = GST_CLOCK_TIME_NONE;

  queue->sink_tainted = TRUE;
  queue->src_tainted = TRUE;

  queue->newseg_applied_to_src = FALSE;

  GST_DEBUG_OBJECT (queue,
      "initialized queue's not_empty & not_full conditions");
}

/* called only once, as opposed to dispose */
static void
gst_queue_finalize (GObject * object)
{
  GstQueue *queue = GST_QUEUE (object);

  GST_DEBUG_OBJECT (queue, "finalizing queue");

  while (!g_queue_is_empty (queue->queue)) {
    GstMiniObject *data = g_queue_pop_head (queue->queue);

    gst_mini_object_unref (data);
  }
  g_queue_free (queue->queue);
  g_mutex_free (queue->qlock);
  g_cond_free (queue->item_add);
  g_cond_free (queue->item_del);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static gboolean
gst_queue_acceptcaps (GstPad * pad, GstCaps * caps)
{
  gboolean result;
  GstQueue *queue;
  GstPad *otherpad;

  queue = GST_QUEUE (gst_pad_get_parent (pad));
  if (G_UNLIKELY (queue == NULL))
    return FALSE;

  otherpad = (pad == queue->srcpad ? queue->sinkpad : queue->srcpad);
  result = gst_pad_peer_accept_caps (otherpad, caps);

  gst_object_unref (queue);

  return result;
}

static GstCaps *
gst_queue_getcaps (GstPad * pad)
{
  GstQueue *queue;
  GstPad *otherpad;
  GstCaps *result;

  queue = GST_QUEUE (gst_pad_get_parent (pad));
  if (G_UNLIKELY (queue == NULL))
    return gst_caps_new_any ();

  otherpad = (pad == queue->srcpad ? queue->sinkpad : queue->srcpad);
  result = gst_pad_peer_get_caps (otherpad);
  if (result == NULL)
    result = gst_caps_new_any ();

  gst_object_unref (queue);

  return result;
}

static GstPadLinkReturn
gst_queue_link_sink (GstPad * pad, GstPad * peer)
{
  return GST_PAD_LINK_OK;
}

static GstPadLinkReturn
gst_queue_link_src (GstPad * pad, GstPad * peer)
{
  GstPadLinkReturn result = GST_PAD_LINK_OK;
  GstQueue *queue;

  queue = GST_QUEUE (gst_pad_get_parent (pad));

  GST_DEBUG_OBJECT (queue, "queue linking source pad");

  if (GST_PAD_LINKFUNC (peer)) {
    result = GST_PAD_LINKFUNC (peer) (peer, pad);
  }

  if (GST_PAD_LINK_SUCCESSFUL (result)) {
    GST_QUEUE_MUTEX_LOCK (queue);
    if (queue->srcresult == GST_FLOW_OK) {
      queue->push_newsegment = TRUE;
      gst_pad_start_task (pad, (GstTaskFunction) gst_queue_loop, pad);
      GST_DEBUG_OBJECT (queue, "starting task as pad is linked");
    } else {
      GST_DEBUG_OBJECT (queue, "not starting task reason %s",
          gst_flow_get_name (queue->srcresult));
    }
    GST_QUEUE_MUTEX_UNLOCK (queue);
  }
  gst_object_unref (queue);

  return result;
}

static GstFlowReturn
gst_queue_bufferalloc (GstPad * pad, guint64 offset, guint size, GstCaps * caps,
    GstBuffer ** buf)
{
  GstQueue *queue;
  GstFlowReturn result;

  queue = GST_QUEUE (gst_pad_get_parent (pad));
  if (G_UNLIKELY (queue == NULL))
    return GST_FLOW_WRONG_STATE;

  /* Forward to src pad, without setting caps on the src pad */
  result = gst_pad_alloc_buffer (queue->srcpad, offset, size, caps, buf);

  gst_object_unref (queue);
  return result;
}

/* calculate the diff between running time on the sink and src of the queue.
 * This is the total amount of time in the queue. */
static void
update_time_level (GstQueue * queue)
{
  gint64 sink_time, src_time;

  if (queue->sink_tainted) {
    queue->sinktime =
        gst_segment_to_running_time (&queue->sink_segment, GST_FORMAT_TIME,
        queue->sink_segment.last_stop);
    queue->sink_tainted = FALSE;
  }
  sink_time = queue->sinktime;

  if (queue->src_tainted) {
    queue->srctime =
        gst_segment_to_running_time (&queue->src_segment, GST_FORMAT_TIME,
        queue->src_segment.last_stop);
    queue->src_tainted = FALSE;
  }
  src_time = queue->srctime;

  GST_LOG_OBJECT (queue, "sink %" GST_TIME_FORMAT ", src %" GST_TIME_FORMAT,
      GST_TIME_ARGS (sink_time), GST_TIME_ARGS (src_time));

  if (sink_time >= src_time)
    queue->cur_level.time = sink_time - src_time;
  else
    queue->cur_level.time = 0;
}

/* take a NEWSEGMENT event and apply the values to segment, updating the time
 * level of queue. */
static void
apply_segment (GstQueue * queue, GstEvent * event, GstSegment * segment,
    gboolean sink)
{
  gboolean update;
  GstFormat format;
  gdouble rate, arate;
  gint64 start, stop, time;

  gst_event_parse_new_segment_full (event, &update, &rate, &arate,
      &format, &start, &stop, &time);

  /* now configure the values, we use these to track timestamps on the
   * sinkpad. */
  if (format != GST_FORMAT_TIME) {
    /* non-time format, pretent the current time segment is closed with a
     * 0 start and unknown stop time. */
    update = FALSE;
    format = GST_FORMAT_TIME;
    start = 0;
    stop = -1;
    time = 0;
  }
  gst_segment_set_newsegment_full (segment, update,
      rate, arate, format, start, stop, time);

  if (sink)
    queue->sink_tainted = TRUE;
  else
    queue->src_tainted = TRUE;

  GST_DEBUG_OBJECT (queue,
      "configured NEWSEGMENT %" GST_SEGMENT_FORMAT, segment);

  /* segment can update the time level of the queue */
  update_time_level (queue);
}

/* take a buffer and update segment, updating the time level of the queue. */
static void
apply_buffer (GstQueue * queue, GstBuffer * buffer, GstSegment * segment,
    gboolean with_duration, gboolean sink)
{
  GstClockTime duration, timestamp;

  timestamp = GST_BUFFER_TIMESTAMP (buffer);
  duration = GST_BUFFER_DURATION (buffer);

  /* if no timestamp is set, assume it's continuous with the previous
   * time */
  if (timestamp == GST_CLOCK_TIME_NONE)
    timestamp = segment->last_stop;

  /* add duration */
  if (with_duration && duration != GST_CLOCK_TIME_NONE)
    timestamp += duration;

  GST_LOG_OBJECT (queue, "last_stop updated to %" GST_TIME_FORMAT,
      GST_TIME_ARGS (timestamp));

  gst_segment_set_last_stop (segment, GST_FORMAT_TIME, timestamp);
  if (sink)
    queue->sink_tainted = TRUE;
  else
    queue->src_tainted = TRUE;


  /* calc diff with other end */
  update_time_level (queue);
}

static void
gst_queue_locked_flush (GstQueue * queue)
{
  while (!g_queue_is_empty (queue->queue)) {
    GstMiniObject *data = g_queue_pop_head (queue->queue);

    /* Then lose another reference because we are supposed to destroy that
       data when flushing */
    gst_mini_object_unref (data);
  }
  GST_QUEUE_CLEAR_LEVEL (queue->cur_level);
  queue->min_threshold.buffers = queue->orig_min_threshold.buffers;
  queue->min_threshold.bytes = queue->orig_min_threshold.bytes;
  queue->min_threshold.time = queue->orig_min_threshold.time;
  gst_segment_init (&queue->sink_segment, GST_FORMAT_TIME);
  gst_segment_init (&queue->src_segment, GST_FORMAT_TIME);
  queue->head_needs_discont = queue->tail_needs_discont = FALSE;

  queue->sinktime = queue->srctime = GST_CLOCK_TIME_NONE;
  queue->sink_tainted = queue->src_tainted = TRUE;

  /* we deleted a lot of something */
  GST_QUEUE_SIGNAL_DEL (queue);
}

/* enqueue an item an update the level stats, with QUEUE_LOCK */
static inline void
gst_queue_locked_enqueue_buffer (GstQueue * queue, gpointer item)
{
  GstBuffer *buffer = GST_BUFFER_CAST (item);

  /* add buffer to the statistics */
  queue->cur_level.buffers++;
  queue->cur_level.bytes += GST_BUFFER_SIZE (buffer);
  apply_buffer (queue, buffer, &queue->sink_segment, TRUE, TRUE);

  g_queue_push_tail (queue->queue, item);
  GST_QUEUE_SIGNAL_ADD (queue);
}

static inline void
gst_queue_locked_enqueue_event (GstQueue * queue, gpointer item)
{
  GstEvent *event = GST_EVENT_CAST (item);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_EOS:
      /* Zero the thresholds, this makes sure the queue is completely
       * filled and we can read all data from the queue. */
      GST_QUEUE_CLEAR_LEVEL (queue->min_threshold);
      /* mark the queue as EOS. This prevents us from accepting more data. */
      GST_CAT_LOG_OBJECT (queue_dataflow, queue, "got EOS from upstream");
      queue->eos = TRUE;
      break;
    case GST_EVENT_NEWSEGMENT:
      apply_segment (queue, event, &queue->sink_segment, TRUE);
      /* if the queue is empty, apply sink segment on the source */
      if (queue->queue->length == 0) {
        GST_CAT_LOG_OBJECT (queue_dataflow, queue, "Apply segment on srcpad");
        apply_segment (queue, event, &queue->src_segment, FALSE);
        queue->newseg_applied_to_src = TRUE;
      }
      /* a new segment allows us to accept more buffers if we got UNEXPECTED
       * from downstream */
      queue->unexpected = FALSE;
      break;
    default:
      break;
  }

  g_queue_push_tail (queue->queue, item);
  GST_QUEUE_SIGNAL_ADD (queue);
}

/* dequeue an item from the queue and update level stats, with QUEUE_LOCK */
static GstMiniObject *
gst_queue_locked_dequeue (GstQueue * queue, gboolean * is_buffer)
{
  GstMiniObject *item;

  item = g_queue_pop_head (queue->queue);
  if (item == NULL)
    goto no_item;

  if (GST_IS_BUFFER (item)) {
    GstBuffer *buffer = GST_BUFFER_CAST (item);

    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "retrieved buffer %p from queue", buffer);

    queue->cur_level.buffers--;
    queue->cur_level.bytes -= GST_BUFFER_SIZE (buffer);
    apply_buffer (queue, buffer, &queue->src_segment, TRUE, FALSE);

    /* if the queue is empty now, update the other side */
    if (queue->cur_level.buffers == 0)
      queue->cur_level.time = 0;

    *is_buffer = TRUE;
  } else if (GST_IS_EVENT (item)) {
    GstEvent *event = GST_EVENT_CAST (item);

    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "retrieved event %p from queue", event);

    switch (GST_EVENT_TYPE (event)) {
      case GST_EVENT_EOS:
        /* queue is empty now that we dequeued the EOS */
        GST_QUEUE_CLEAR_LEVEL (queue->cur_level);
        break;
      case GST_EVENT_NEWSEGMENT:
        /* apply newsegment if it has not already been applied */
        if (G_LIKELY (!queue->newseg_applied_to_src)) {
          apply_segment (queue, event, &queue->src_segment, FALSE);
        } else {
          queue->newseg_applied_to_src = FALSE;
        }
        break;
      default:
        break;
    }

    *is_buffer = FALSE;
  } else {
    g_warning
        ("Unexpected item %p dequeued from queue %s (refcounting problem?)",
        item, GST_OBJECT_NAME (queue));
    item = NULL;
  }
  GST_QUEUE_SIGNAL_DEL (queue);

  return item;

  /* ERRORS */
no_item:
  {
    GST_CAT_DEBUG_OBJECT (queue_dataflow, queue, "the queue is empty");
    return NULL;
  }
}

static gboolean
gst_queue_handle_sink_event (GstPad * pad, GstEvent * event)
{
  GstQueue *queue;

  queue = GST_QUEUE (gst_pad_get_parent (pad));
  if (G_UNLIKELY (queue == NULL)) {
    gst_event_unref (event);
    return FALSE;
  }

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_FLUSH_START:
    {
      STATUS (queue, pad, "received flush start event");
      /* forward event */
      gst_pad_push_event (queue->srcpad, event);

      /* now unblock the chain function */
      GST_QUEUE_MUTEX_LOCK (queue);
      queue->srcresult = GST_FLOW_WRONG_STATE;
      /* unblock the loop and chain functions */
      GST_QUEUE_SIGNAL_ADD (queue);
      GST_QUEUE_SIGNAL_DEL (queue);
      GST_QUEUE_MUTEX_UNLOCK (queue);

      /* make sure it pauses, this should happen since we sent
       * flush_start downstream. */
      gst_pad_pause_task (queue->srcpad);
      GST_CAT_LOG_OBJECT (queue_dataflow, queue, "loop stopped");
      goto done;
    }
    case GST_EVENT_FLUSH_STOP:
    {
      STATUS (queue, pad, "received flush stop event");
      /* forward event */
      gst_pad_push_event (queue->srcpad, event);

      GST_QUEUE_MUTEX_LOCK (queue);
      gst_queue_locked_flush (queue);
      queue->srcresult = GST_FLOW_OK;
      queue->eos = FALSE;
      queue->unexpected = FALSE;
      if (gst_pad_is_linked (queue->srcpad)) {
        gst_pad_start_task (queue->srcpad, (GstTaskFunction) gst_queue_loop,
            queue->srcpad);
      } else {
        GST_INFO_OBJECT (queue, "not re-starting task as pad is not linked");
      }
      GST_QUEUE_MUTEX_UNLOCK (queue);

      STATUS (queue, pad, "after flush");
      goto done;
    }
    default:
      if (GST_EVENT_IS_SERIALIZED (event)) {
        /* serialized events go in the queue */
        GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
        /* refuse more events on EOS */
        if (queue->eos)
          goto out_eos;
        gst_queue_locked_enqueue_event (queue, event);
        GST_QUEUE_MUTEX_UNLOCK (queue);
      } else {
        /* non-serialized events are passed upstream. */
        gst_pad_push_event (queue->srcpad, event);
      }
      break;
  }
done:
  gst_object_unref (queue);
  return TRUE;

  /* ERRORS */
out_flushing:
  {
    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "refusing event, we are flushing");
    GST_QUEUE_MUTEX_UNLOCK (queue);
    gst_object_unref (queue);
    gst_event_unref (event);
    return FALSE;
  }
out_eos:
  {
    GST_CAT_LOG_OBJECT (queue_dataflow, queue, "refusing event, we are EOS");
    GST_QUEUE_MUTEX_UNLOCK (queue);
    gst_object_unref (queue);
    gst_event_unref (event);
    return FALSE;
  }
}

static gboolean
gst_queue_is_empty (GstQueue * queue)
{
  if (queue->queue->length == 0)
    return TRUE;

  /* It is possible that a max size is reached before all min thresholds are.
   * Therefore, only consider it empty if it is not filled. */
  return ((queue->min_threshold.buffers > 0 &&
          queue->cur_level.buffers < queue->min_threshold.buffers) ||
      (queue->min_threshold.bytes > 0 &&
          queue->cur_level.bytes < queue->min_threshold.bytes) ||
      (queue->min_threshold.time > 0 &&
          queue->cur_level.time < queue->min_threshold.time)) &&
      !gst_queue_is_filled (queue);
}

static gboolean
gst_queue_is_filled (GstQueue * queue)
{
  return (((queue->max_size.buffers > 0 &&
              queue->cur_level.buffers >= queue->max_size.buffers) ||
          (queue->max_size.bytes > 0 &&
              queue->cur_level.bytes >= queue->max_size.bytes) ||
          (queue->max_size.time > 0 &&
              queue->cur_level.time >= queue->max_size.time)));
}

static void
gst_queue_leak_downstream (GstQueue * queue)
{
  /* for as long as the queue is filled, dequeue an item and discard it */
  while (gst_queue_is_filled (queue)) {
    GstMiniObject *leak;
    gboolean is_buffer;

    leak = gst_queue_locked_dequeue (queue, &is_buffer);
    /* there is nothing to dequeue and the queue is still filled.. This should
     * not happen */
    g_assert (leak != NULL);

    GST_CAT_DEBUG_OBJECT (queue_dataflow, queue,
        "queue is full, leaking item %p on downstream end", leak);
    gst_mini_object_unref (leak);

    /* last buffer needs to get a DISCONT flag */
    queue->head_needs_discont = TRUE;
  }
}

static GstFlowReturn
gst_queue_chain (GstPad * pad, GstBuffer * buffer)
{
  GstQueue *queue;
  GstClockTime duration, timestamp;

  queue = (GstQueue *) GST_OBJECT_PARENT (pad);

  /* we have to lock the queue since we span threads */
  GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
  /* when we received EOS, we refuse any more data */
  if (queue->eos)
    goto out_eos;
  if (queue->unexpected)
    goto out_unexpected;

  timestamp = GST_BUFFER_TIMESTAMP (buffer);
  duration = GST_BUFFER_DURATION (buffer);

  GST_CAT_LOG_OBJECT (queue_dataflow, queue,
      "received buffer %p of size %d, time %" GST_TIME_FORMAT ", duration %"
      GST_TIME_FORMAT, buffer, GST_BUFFER_SIZE (buffer),
      GST_TIME_ARGS (timestamp), GST_TIME_ARGS (duration));

  /* We make space available if we're "full" according to whatever
   * the user defined as "full". Note that this only applies to buffers.
   * We always handle events and they don't count in our statistics. */
  while (gst_queue_is_filled (queue)) {
    if (!queue->silent) {
      GST_QUEUE_MUTEX_UNLOCK (queue);
      g_signal_emit (queue, gst_queue_signals[SIGNAL_OVERRUN], 0);
      GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
      /* we recheck, the signal could have changed the thresholds */
      if (!gst_queue_is_filled (queue))
        break;
    }

    /* how are we going to make space for this buffer? */
    switch (queue->leaky) {
      case GST_QUEUE_LEAK_UPSTREAM:
        /* next buffer needs to get a DISCONT flag */
        queue->tail_needs_discont = TRUE;
        /* leak current buffer */
        GST_CAT_DEBUG_OBJECT (queue_dataflow, queue,
            "queue is full, leaking buffer on upstream end");
        /* now we can clean up and exit right away */
        goto out_unref;
      case GST_QUEUE_LEAK_DOWNSTREAM:
        gst_queue_leak_downstream (queue);
        break;
      default:
        g_warning ("Unknown leaky type, using default");
        /* fall-through */
      case GST_QUEUE_NO_LEAK:
      {
        GST_CAT_DEBUG_OBJECT (queue_dataflow, queue,
            "queue is full, waiting for free space");

        /* don't leak. Instead, wait for space to be available */
        do {
          /* for as long as the queue is filled, wait till an item was deleted. */
          GST_QUEUE_WAIT_DEL_CHECK (queue, out_flushing);
        } while (gst_queue_is_filled (queue));

        GST_CAT_DEBUG_OBJECT (queue_dataflow, queue, "queue is not full");

        if (!queue->silent) {
          GST_QUEUE_MUTEX_UNLOCK (queue);
          g_signal_emit (queue, gst_queue_signals[SIGNAL_RUNNING], 0);
          GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
        }
        break;
      }
    }
  }

  if (queue->tail_needs_discont) {
    GstBuffer *subbuffer = gst_buffer_make_metadata_writable (buffer);

    if (subbuffer) {
      buffer = subbuffer;
      GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);
    } else {
      GST_DEBUG_OBJECT (queue, "Could not mark buffer as DISCONT");
    }
    queue->tail_needs_discont = FALSE;
  }

  /* put buffer in queue now */
  gst_queue_locked_enqueue_buffer (queue, buffer);
  GST_QUEUE_MUTEX_UNLOCK (queue);

  return GST_FLOW_OK;

  /* special conditions */
out_unref:
  {
    GST_QUEUE_MUTEX_UNLOCK (queue);

    gst_buffer_unref (buffer);

    return GST_FLOW_OK;
  }
out_flushing:
  {
    GstFlowReturn ret = queue->srcresult;

    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "exit because task paused, reason: %s", gst_flow_get_name (ret));
    GST_QUEUE_MUTEX_UNLOCK (queue);
    gst_buffer_unref (buffer);

    return ret;
  }
out_eos:
  {
    GST_CAT_LOG_OBJECT (queue_dataflow, queue, "exit because we received EOS");
    GST_QUEUE_MUTEX_UNLOCK (queue);

    gst_buffer_unref (buffer);

    return GST_FLOW_UNEXPECTED;
  }
out_unexpected:
  {
    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "exit because we received UNEXPECTED");
    GST_QUEUE_MUTEX_UNLOCK (queue);

    gst_buffer_unref (buffer);

    return GST_FLOW_UNEXPECTED;
  }
}

static void
gst_queue_push_newsegment (GstQueue * queue)
{
  GstSegment *s;
  GstEvent *event;

  s = &queue->src_segment;

  if (s->accum != 0) {
    event = gst_event_new_new_segment_full (FALSE, 1.0, 1.0, s->format, 0,
        s->accum, 0);
    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "pushing accum newsegment event");
    gst_pad_push_event (queue->srcpad, event);
  }

  event = gst_event_new_new_segment_full (FALSE, s->rate, s->applied_rate,
      s->format, s->start, s->stop, s->time);
  GST_CAT_LOG_OBJECT (queue_dataflow, queue, "pushing real newsegment event");
  gst_pad_push_event (queue->srcpad, event);
}

/* dequeue an item from the queue an push it downstream. This functions returns
 * the result of the push. */
static GstFlowReturn
gst_queue_push_one (GstQueue * queue)
{
  GstFlowReturn result = GST_FLOW_OK;
  GstMiniObject *data;
  gboolean is_buffer;

  data = gst_queue_locked_dequeue (queue, &is_buffer);
  if (data == NULL)
    goto no_item;

next:
  if (is_buffer) {
    GstBuffer *buffer;
    GstCaps *caps;

    buffer = GST_BUFFER_CAST (data);

    if (queue->head_needs_discont) {
      GstBuffer *subbuffer = gst_buffer_make_metadata_writable (buffer);

      if (subbuffer) {
        buffer = subbuffer;
        GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_DISCONT);
      } else {
        GST_DEBUG_OBJECT (queue, "Could not mark buffer as DISCONT");
      }
      queue->head_needs_discont = FALSE;
    }

    caps = GST_BUFFER_CAPS (buffer);

    GST_QUEUE_MUTEX_UNLOCK (queue);
    /* set the right caps on the pad now. We do this before pushing the buffer
     * because the pad_push call will check (using acceptcaps) if the buffer can
     * be set on the pad, which might fail because this will be propagated
     * upstream. Also note that if the buffer has NULL caps, it means that the
     * caps did not change, so we don't have to change caps on the pad. */
    if (caps && caps != GST_PAD_CAPS (queue->srcpad))
      gst_pad_set_caps (queue->srcpad, caps);

    if (queue->push_newsegment) {
      gst_queue_push_newsegment (queue);
    }
    result = gst_pad_push (queue->srcpad, buffer);

    /* need to check for srcresult here as well */
    GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);

    if (result == GST_FLOW_UNEXPECTED) {
      GST_CAT_LOG_OBJECT (queue_dataflow, queue,
          "got UNEXPECTED from downstream");
      /* stop pushing buffers, we dequeue all items until we see an item that we
       * can push again, which is EOS or NEWSEGMENT. If there is nothing in the
       * queue we can push, we set a flag to make the sinkpad refuse more
       * buffers with an UNEXPECTED return value. */
      while ((data = gst_queue_locked_dequeue (queue, &is_buffer))) {
        if (is_buffer) {
          GST_CAT_LOG_OBJECT (queue_dataflow, queue,
              "dropping UNEXPECTED buffer %p", data);
          gst_buffer_unref (GST_BUFFER_CAST (data));
        } else {
          GstEvent *event = GST_EVENT_CAST (data);
          GstEventType type = GST_EVENT_TYPE (event);

          if (type == GST_EVENT_EOS || type == GST_EVENT_NEWSEGMENT) {
            /* we found a pushable item in the queue, push it out */
            GST_CAT_LOG_OBJECT (queue_dataflow, queue,
                "pushing pushable event %s after UNEXPECTED",
                GST_EVENT_TYPE_NAME (event));
            goto next;
          }
          GST_CAT_LOG_OBJECT (queue_dataflow, queue,
              "dropping UNEXPECTED event %p", event);
          gst_event_unref (event);
        }
      }
      /* no more items in the queue. Set the unexpected flag so that upstream
       * make us refuse any more buffers on the sinkpad. Since we will still
       * accept EOS and NEWSEGMENT we return _FLOW_OK to the caller so that the
       * task function does not shut down. */
      queue->unexpected = TRUE;
      result = GST_FLOW_OK;
    }
  } else {
    GstEvent *event = GST_EVENT_CAST (data);
    GstEventType type = GST_EVENT_TYPE (event);

    GST_QUEUE_MUTEX_UNLOCK (queue);

    if (queue->push_newsegment && type != GST_EVENT_NEWSEGMENT) {
      gst_queue_push_newsegment (queue);
    }
    gst_pad_push_event (queue->srcpad, event);

    GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
    /* if we're EOS, return UNEXPECTED so that the task pauses. */
    if (type == GST_EVENT_EOS) {
      GST_CAT_LOG_OBJECT (queue_dataflow, queue,
          "pushed EOS event %p, return UNEXPECTED", event);
      result = GST_FLOW_UNEXPECTED;
    }
  }
  return result;

  /* ERRORS */
no_item:
  {
    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "exit because we have no item in the queue");
    return GST_FLOW_ERROR;
  }
out_flushing:
  {
    GST_CAT_LOG_OBJECT (queue_dataflow, queue, "exit because we are flushing");
    return GST_FLOW_WRONG_STATE;
  }
}

static void
gst_queue_loop (GstPad * pad)
{
  GstQueue *queue;
  GstFlowReturn ret;

  queue = (GstQueue *) GST_PAD_PARENT (pad);

  /* have to lock for thread-safety */
  GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);

  while (gst_queue_is_empty (queue)) {
    GST_CAT_DEBUG_OBJECT (queue_dataflow, queue, "queue is empty");
    if (!queue->silent) {
      GST_QUEUE_MUTEX_UNLOCK (queue);
      g_signal_emit (queue, gst_queue_signals[SIGNAL_UNDERRUN], 0);
      GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
    }

    /* we recheck, the signal could have changed the thresholds */
    while (gst_queue_is_empty (queue)) {
      GST_QUEUE_WAIT_ADD_CHECK (queue, out_flushing);
    }

    GST_CAT_DEBUG_OBJECT (queue_dataflow, queue, "queue is not empty");
    if (!queue->silent) {
      GST_QUEUE_MUTEX_UNLOCK (queue);
      g_signal_emit (queue, gst_queue_signals[SIGNAL_RUNNING], 0);
      g_signal_emit (queue, gst_queue_signals[SIGNAL_PUSHING], 0);
      GST_QUEUE_MUTEX_LOCK_CHECK (queue, out_flushing);
    }
  }

  ret = gst_queue_push_one (queue);
  queue->push_newsegment = FALSE;
  queue->srcresult = ret;
  if (ret != GST_FLOW_OK)
    goto out_flushing;

  GST_QUEUE_MUTEX_UNLOCK (queue);

  return;

  /* ERRORS */
out_flushing:
  {
    gboolean eos = queue->eos;
    GstFlowReturn ret = queue->srcresult;

    gst_pad_pause_task (queue->srcpad);
    GST_CAT_LOG_OBJECT (queue_dataflow, queue,
        "pause task, reason:  %s", gst_flow_get_name (ret));
    GST_QUEUE_SIGNAL_DEL (queue);
    GST_QUEUE_MUTEX_UNLOCK (queue);
    /* let app know about us giving up if upstream is not expected to do so */
    /* UNEXPECTED is already taken care of elsewhere */
    if (eos && (ret == GST_FLOW_NOT_LINKED || ret < GST_FLOW_UNEXPECTED)) {
      GST_ELEMENT_ERROR (queue, STREAM, FAILED,
          (_("Internal data flow error.")),
          ("streaming task paused, reason %s (%d)",
              gst_flow_get_name (ret), ret));
      gst_pad_push_event (queue->srcpad, gst_event_new_eos ());
    }
    return;
  }
}

static gboolean
gst_queue_handle_src_event (GstPad * pad, GstEvent * event)
{
  gboolean res = TRUE;
  GstQueue *queue = GST_QUEUE (gst_pad_get_parent (pad));

  if (G_UNLIKELY (queue == NULL)) {
    gst_event_unref (event);
    return FALSE;
  }
#ifndef GST_DISABLE_GST_DEBUG
  GST_CAT_DEBUG_OBJECT (queue_dataflow, queue, "got event %p (%d)",
      event, GST_EVENT_TYPE (event));
#endif

  res = gst_pad_push_event (queue->sinkpad, event);

  gst_object_unref (queue);
  return res;
}

static gboolean
gst_queue_handle_src_query (GstPad * pad, GstQuery * query)
{
  GstQueue *queue = GST_QUEUE (gst_pad_get_parent (pad));
  GstPad *peer;
  gboolean res;

  if (G_UNLIKELY (queue == NULL))
    return FALSE;

  if (!(peer = gst_pad_get_peer (queue->sinkpad))) {
    gst_object_unref (queue);
    return FALSE;
  }

  res = gst_pad_query (peer, query);
  gst_object_unref (peer);
  if (!res) {
    gst_object_unref (queue);
    return FALSE;
  }

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_POSITION:
    {
      gint64 peer_pos;
      GstFormat format;

      /* get peer position */
      gst_query_parse_position (query, &format, &peer_pos);

      /* FIXME: this code assumes that there's no discont in the queue */
      switch (format) {
        case GST_FORMAT_BYTES:
          peer_pos -= queue->cur_level.bytes;
          break;
        case GST_FORMAT_TIME:
          peer_pos -= queue->cur_level.time;
          break;
        default:
          GST_DEBUG_OBJECT (queue, "Can't adjust query in %s format, don't "
              "know how to adjust value", gst_format_get_name (format));
          return TRUE;
      }
      /* set updated position */
      gst_query_set_position (query, format, peer_pos);
      break;
    }
    case GST_QUERY_LATENCY:
    {
      gboolean live;
      GstClockTime min, max;

      gst_query_parse_latency (query, &live, &min, &max);

      /* we can delay up to the limit of the queue in time. If we have no time
       * limit, the best thing we can do is to return an infinite delay. In
       * reality a better estimate would be the byte/buffer rate but that is not
       * possible right now. */
      if (queue->max_size.time > 0 && max != -1)
        max += queue->max_size.time;
      else
        max = -1;

      /* adjust for min-threshold */
      if (queue->min_threshold.time > 0 && min != -1)
        min += queue->min_threshold.time;

      gst_query_set_latency (query, live, min, max);
      break;
    }
    default:
      /* peer handled other queries */
      break;
  }

  gst_object_unref (queue);
  return TRUE;
}

static gboolean
gst_queue_sink_activate_push (GstPad * pad, gboolean active)
{
  gboolean result = TRUE;
  GstQueue *queue;

  queue = GST_QUEUE (gst_pad_get_parent (pad));

  if (active) {
    GST_QUEUE_MUTEX_LOCK (queue);
    queue->srcresult = GST_FLOW_OK;
    queue->eos = FALSE;
    queue->unexpected = FALSE;
    GST_QUEUE_MUTEX_UNLOCK (queue);
  } else {
    /* step 1, unblock chain function */
    GST_QUEUE_MUTEX_LOCK (queue);
    queue->srcresult = GST_FLOW_WRONG_STATE;
    gst_queue_locked_flush (queue);
    GST_QUEUE_MUTEX_UNLOCK (queue);
  }

  gst_object_unref (queue);

  return result;
}

static gboolean
gst_queue_src_activate_push (GstPad * pad, gboolean active)
{
  gboolean result = FALSE;
  GstQueue *queue;

  queue = GST_QUEUE (gst_pad_get_parent (pad));

  if (active) {
    GST_QUEUE_MUTEX_LOCK (queue);
    queue->srcresult = GST_FLOW_OK;
    queue->eos = FALSE;
    queue->unexpected = FALSE;
    /* we do not start the task yet if the pad is not connected */
    if (gst_pad_is_linked (pad))
      result = gst_pad_start_task (pad, (GstTaskFunction) gst_queue_loop, pad);
    else {
      GST_INFO_OBJECT (queue, "not starting task as pad is not linked");
      result = TRUE;
    }
    GST_QUEUE_MUTEX_UNLOCK (queue);
  } else {
    /* step 1, unblock loop function */
    GST_QUEUE_MUTEX_LOCK (queue);
    queue->srcresult = GST_FLOW_WRONG_STATE;
    /* the item add signal will unblock */
    g_cond_signal (queue->item_add);
    GST_QUEUE_MUTEX_UNLOCK (queue);

    /* step 2, make sure streaming finishes */
    result = gst_pad_stop_task (pad);
  }

  gst_object_unref (queue);

  return result;
}

static void
queue_capacity_change (GstQueue * queue)
{
  if (queue->leaky == GST_QUEUE_LEAK_DOWNSTREAM) {
    gst_queue_leak_downstream (queue);
  }

  /* changing the capacity of the queue must wake up
   * the _chain function, it might have more room now
   * to store the buffer/event in the queue */
  GST_QUEUE_SIGNAL_DEL (queue);
}

/* Changing the minimum required fill level must
 * wake up the _loop function as it might now
 * be able to preceed.
 */
#define QUEUE_THRESHOLD_CHANGE(q)\
  GST_QUEUE_SIGNAL_ADD (q);

static void
gst_queue_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec)
{
  GstQueue *queue = GST_QUEUE (object);

  /* someone could change levels here, and since this
   * affects the get/put funcs, we need to lock for safety. */
  GST_QUEUE_MUTEX_LOCK (queue);

  switch (prop_id) {
    case PROP_MAX_SIZE_BYTES:
      queue->max_size.bytes = g_value_get_uint (value);
      queue_capacity_change (queue);
      break;
    case PROP_MAX_SIZE_BUFFERS:
      queue->max_size.buffers = g_value_get_uint (value);
      queue_capacity_change (queue);
      break;
    case PROP_MAX_SIZE_TIME:
      queue->max_size.time = g_value_get_uint64 (value);
      queue_capacity_change (queue);
      break;
    case PROP_MIN_THRESHOLD_BYTES:
      queue->min_threshold.bytes = g_value_get_uint (value);
      queue->orig_min_threshold.bytes = queue->min_threshold.bytes;
      QUEUE_THRESHOLD_CHANGE (queue);
      break;
    case PROP_MIN_THRESHOLD_BUFFERS:
      queue->min_threshold.buffers = g_value_get_uint (value);
      queue->orig_min_threshold.buffers = queue->min_threshold.buffers;
      QUEUE_THRESHOLD_CHANGE (queue);
      break;
    case PROP_MIN_THRESHOLD_TIME:
      queue->min_threshold.time = g_value_get_uint64 (value);
      queue->orig_min_threshold.time = queue->min_threshold.time;
      QUEUE_THRESHOLD_CHANGE (queue);
      break;
    case PROP_LEAKY:
      queue->leaky = g_value_get_enum (value);
      break;
    case PROP_SILENT:
      queue->silent = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  GST_QUEUE_MUTEX_UNLOCK (queue);
}

static void
gst_queue_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  GstQueue *queue = GST_QUEUE (object);

  GST_QUEUE_MUTEX_LOCK (queue);

  switch (prop_id) {
    case PROP_CUR_LEVEL_BYTES:
      g_value_set_uint (value, queue->cur_level.bytes);
      break;
    case PROP_CUR_LEVEL_BUFFERS:
      g_value_set_uint (value, queue->cur_level.buffers);
      break;
    case PROP_CUR_LEVEL_TIME:
      g_value_set_uint64 (value, queue->cur_level.time);
      break;
    case PROP_MAX_SIZE_BYTES:
      g_value_set_uint (value, queue->max_size.bytes);
      break;
    case PROP_MAX_SIZE_BUFFERS:
      g_value_set_uint (value, queue->max_size.buffers);
      break;
    case PROP_MAX_SIZE_TIME:
      g_value_set_uint64 (value, queue->max_size.time);
      break;
    case PROP_MIN_THRESHOLD_BYTES:
      g_value_set_uint (value, queue->min_threshold.bytes);
      break;
    case PROP_MIN_THRESHOLD_BUFFERS:
      g_value_set_uint (value, queue->min_threshold.buffers);
      break;
    case PROP_MIN_THRESHOLD_TIME:
      g_value_set_uint64 (value, queue->min_threshold.time);
      break;
    case PROP_LEAKY:
      g_value_set_enum (value, queue->leaky);
      break;
    case PROP_SILENT:
      g_value_set_boolean (value, queue->silent);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

  GST_QUEUE_MUTEX_UNLOCK (queue);
}

#ifdef GSTREAMER_LITE
static GstStateChangeReturn gst_queue_change_state (GstElement *element, GstStateChange transition)
{
    GstQueue *queue = GST_QUEUE(element);
    GstStateChangeReturn ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

    if (ret == GST_STATE_CHANGE_FAILURE)
        return ret;

    switch (transition)
    {
        case GST_STATE_CHANGE_READY_TO_NULL:
            GST_QUEUE_MUTEX_LOCK (queue);
            queue->srcresult = GST_FLOW_WRONG_STATE; // make sure we stop _loop task
            g_cond_signal (queue->item_add);
            GST_QUEUE_MUTEX_UNLOCK (queue);
            break;
        default:
            break;
    }
    return ret;
}
#endif
