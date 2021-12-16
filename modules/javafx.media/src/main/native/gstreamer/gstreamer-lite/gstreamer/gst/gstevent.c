/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wim.taymans@chello.be>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *
 * gstevent.c: GstEvent subsystem
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
 * SECTION:gstevent
 * @title: GstEvent
 * @short_description: Structure describing events that are passed up and down
 *                     a pipeline
 * @see_also: #GstPad, #GstElement
 *
 * The event class provides factory methods to construct events for sending
 * and functions to query (parse) received events.
 *
 * Events are usually created with gst_event_new_*() which takes event-type
 * specific parameters as arguments.
 * To send an event application will usually use gst_element_send_event() and
 * elements will use gst_pad_send_event() or gst_pad_push_event().
 * The event should be unreffed with gst_event_unref() if it has not been sent.
 *
 * Events that have been received can be parsed with their respective
 * gst_event_parse_*() functions. It is valid to pass %NULL for unwanted details.
 *
 * Events are passed between elements in parallel to the data stream. Some events
 * are serialized with buffers, others are not. Some events only travel downstream,
 * others only upstream. Some events can travel both upstream and downstream.
 *
 * The events are used to signal special conditions in the datastream such as
 * EOS (end of stream) or the start of a new stream-segment.
 * Events are also used to flush the pipeline of any pending data.
 *
 * Most of the event API is used inside plugins. Applications usually only
 * construct and use seek events.
 * To do that gst_event_new_seek() is used to create a seek event. It takes
 * the needed parameters to specify seeking time and mode.
 * |[<!-- language="C" -->
 *   GstEvent *event;
 *   gboolean result;
 *   ...
 *   // construct a seek event to play the media from second 2 to 5, flush
 *   // the pipeline to decrease latency.
 *   event = gst_event_new_seek (1.0,
 *      GST_FORMAT_TIME,
 *      GST_SEEK_FLAG_FLUSH,
 *      GST_SEEK_TYPE_SET, 2 * GST_SECOND,
 *      GST_SEEK_TYPE_SET, 5 * GST_SECOND);
 *   ...
 *   result = gst_element_send_event (pipeline, event);
 *   if (!result)
 *     g_warning ("seek failed");
 *   ...
 * ]|
 */

#define GST_DISABLE_MINIOBJECT_INLINE_FUNCTIONS
#include "gst_private.h"
#include <string.h>             /* memcpy */

#include "gstinfo.h"
#include "gstevent.h"
#include "gstenumtypes.h"
#include "gstutils.h"
#include "gstquark.h"
#include "gstvalue.h"

GType _gst_event_type = 0;

typedef struct
{
  GstEvent event;

  GstStructure *structure;
  gint64 running_time_offset;
} GstEventImpl;

#define GST_EVENT_STRUCTURE(e)  (((GstEventImpl *)(e))->structure)

typedef struct
{
  const gint type;
  const gchar *name;
  GQuark quark;
} GstEventQuarks;

static GstEventQuarks event_quarks[] = {
  {GST_EVENT_UNKNOWN, "unknown", 0},
  {GST_EVENT_FLUSH_START, "flush-start", 0},
  {GST_EVENT_FLUSH_STOP, "flush-stop", 0},
  {GST_EVENT_SELECT_STREAMS, "select-streams", 0},
  {GST_EVENT_STREAM_START, "stream-start", 0},
  {GST_EVENT_STREAM_COLLECTION, "stream-collection", 0},
  {GST_EVENT_CAPS, "caps", 0},
  {GST_EVENT_SEGMENT, "segment", 0},
  {GST_EVENT_TAG, "tag", 0},
  {GST_EVENT_TOC, "toc", 0},
  {GST_EVENT_PROTECTION, "protection", 0},
  {GST_EVENT_BUFFERSIZE, "buffersize", 0},
  {GST_EVENT_SINK_MESSAGE, "sink-message", 0},
  {GST_EVENT_EOS, "eos", 0},
  {GST_EVENT_SEGMENT_DONE, "segment-done", 0},
  {GST_EVENT_GAP, "gap", 0},
  {GST_EVENT_QOS, "qos", 0},
  {GST_EVENT_SEEK, "seek", 0},
  {GST_EVENT_NAVIGATION, "navigation", 0},
  {GST_EVENT_LATENCY, "latency", 0},
  {GST_EVENT_STEP, "step", 0},
  {GST_EVENT_RECONFIGURE, "reconfigure", 0},
  {GST_EVENT_TOC_SELECT, "toc-select", 0},
  {GST_EVENT_CUSTOM_UPSTREAM, "custom-upstream", 0},
  {GST_EVENT_CUSTOM_DOWNSTREAM, "custom-downstream", 0},
  {GST_EVENT_CUSTOM_DOWNSTREAM_OOB, "custom-downstream-oob", 0},
  {GST_EVENT_CUSTOM_DOWNSTREAM_STICKY, "custom-downstream-sticky", 0},
  {GST_EVENT_CUSTOM_BOTH, "custom-both", 0},
  {GST_EVENT_CUSTOM_BOTH_OOB, "custom-both-oob", 0},
  {GST_EVENT_STREAM_GROUP_DONE, "stream-group-done", 0},
  {GST_EVENT_INSTANT_RATE_CHANGE, "instant-rate-change", 0},
  {GST_EVENT_INSTANT_RATE_SYNC_TIME, "instant-rate-sync-time", 0},

  {0, NULL, 0}
};

GST_DEFINE_MINI_OBJECT_TYPE (GstEvent, gst_event);

void
_priv_gst_event_initialize (void)
{
  gint i;

  _gst_event_type = gst_event_get_type ();

  g_type_class_ref (gst_seek_flags_get_type ());
  g_type_class_ref (gst_seek_type_get_type ());

  for (i = 0; event_quarks[i].name; i++) {
    event_quarks[i].quark = g_quark_from_static_string (event_quarks[i].name);
  }
}

/**
 * gst_event_type_get_name:
 * @type: the event type
 *
 * Get a printable name for the given event type. Do not modify or free.
 *
 * Returns: a reference to the static name of the event.
 */
const gchar *
gst_event_type_get_name (GstEventType type)
{
  gint i;

  for (i = 0; event_quarks[i].name; i++) {
    if (type == event_quarks[i].type)
      return event_quarks[i].name;
  }
  return "unknown";
}

/**
 * gst_event_type_to_quark:
 * @type: the event type
 *
 * Get the unique quark for the given event type.
 *
 * Returns: the quark associated with the event type
 */
GQuark
gst_event_type_to_quark (GstEventType type)
{
  gint i;

  for (i = 0; event_quarks[i].name; i++) {
    if (type == event_quarks[i].type)
      return event_quarks[i].quark;
  }
  return 0;
}

/**
 * gst_event_type_get_flags:
 * @type: a #GstEventType
 *
 * Gets the #GstEventTypeFlags associated with @type.
 *
 * Returns: a #GstEventTypeFlags.
 */
GstEventTypeFlags
gst_event_type_get_flags (GstEventType type)
{
  GstEventTypeFlags ret;

  ret = type & ((1 << GST_EVENT_NUM_SHIFT) - 1);

  return ret;
}

static void
_gst_event_free (GstEvent * event)
{
  GstStructure *s;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_IS_EVENT (event));

  GST_CAT_LOG (GST_CAT_EVENT, "freeing event %p type %s", event,
      GST_EVENT_TYPE_NAME (event));

  s = GST_EVENT_STRUCTURE (event);

  if (s) {
    gst_structure_set_parent_refcount (s, NULL);
    gst_structure_free (s);
  }
#ifdef USE_POISONING
  memset (event, 0xff, sizeof (GstEventImpl));
#endif

  g_slice_free1 (sizeof (GstEventImpl), event);
}

static void gst_event_init (GstEventImpl * event, GstEventType type);

static GstEvent *
_gst_event_copy (GstEvent * event)
{
  GstEventImpl *copy;
  GstStructure *s;

  copy = g_slice_new0 (GstEventImpl);

  gst_event_init (copy, GST_EVENT_TYPE (event));

  GST_EVENT_TIMESTAMP (copy) = GST_EVENT_TIMESTAMP (event);
  GST_EVENT_SEQNUM (copy) = GST_EVENT_SEQNUM (event);

  s = GST_EVENT_STRUCTURE (event);
  if (s) {
    GST_EVENT_STRUCTURE (copy) = gst_structure_copy (s);
    gst_structure_set_parent_refcount (GST_EVENT_STRUCTURE (copy),
        &copy->event.mini_object.refcount);
  } else {
    GST_EVENT_STRUCTURE (copy) = NULL;
  }

  ((GstEventImpl *) copy)->running_time_offset =
      ((GstEventImpl *) event)->running_time_offset;

  return GST_EVENT_CAST (copy);
}

static void
gst_event_init (GstEventImpl * event, GstEventType type)
{
  gst_mini_object_init (GST_MINI_OBJECT_CAST (event), 0, _gst_event_type,
      (GstMiniObjectCopyFunction) _gst_event_copy, NULL,
      (GstMiniObjectFreeFunction) _gst_event_free);

  GST_EVENT_TYPE (event) = type;
  GST_EVENT_TIMESTAMP (event) = GST_CLOCK_TIME_NONE;
  GST_EVENT_SEQNUM (event) = gst_util_seqnum_next ();
  event->running_time_offset = 0;
}


/**
 * gst_event_new_custom:
 * @type: The type of the new event
 * @structure: (transfer full): the structure for the event. The event will
 *     take ownership of the structure.
 *
 * Create a new custom-typed event. This can be used for anything not
 * handled by other event-specific functions to pass an event to another
 * element.
 *
 * Make sure to allocate an event type with the #GST_EVENT_MAKE_TYPE macro,
 * assigning a free number and filling in the correct direction and
 * serialization flags.
 *
 * New custom events can also be created by subclassing the event type if
 * needed.
 *
 * Returns: (transfer full) (nullable): the new custom event.
 */
GstEvent *
gst_event_new_custom (GstEventType type, GstStructure * structure)
{
  GstEventImpl *event;

  event = g_slice_new0 (GstEventImpl);

  GST_CAT_DEBUG (GST_CAT_EVENT, "creating new event %p %s %d", event,
      gst_event_type_get_name (type), type);

  if (structure) {
    /* structure must not have a parent */
    if (!gst_structure_set_parent_refcount (structure,
            &event->event.mini_object.refcount))
      goto had_parent;

  }
  gst_event_init (event, type);

  GST_EVENT_STRUCTURE (event) = structure;

  return GST_EVENT_CAST (event);

  /* ERRORS */
had_parent:
  {
    g_slice_free1 (sizeof (GstEventImpl), event);
    g_warning ("structure is already owned by another object");
    return NULL;
  }
}

/**
 * gst_event_get_structure:
 * @event: The #GstEvent.
 *
 * Access the structure of the event.
 *
 * Returns: (transfer none) (nullable): The structure of the event. The
 * structure is still owned by the event, which means that you should not free
 * it and that the pointer becomes invalid when you free the event.
 *
 * MT safe.
 */
const GstStructure *
gst_event_get_structure (GstEvent * event)
{
  g_return_val_if_fail (GST_IS_EVENT (event), NULL);

  return GST_EVENT_STRUCTURE (event);
}

/**
 * gst_event_writable_structure:
 * @event: The #GstEvent.
 *
 * Get a writable version of the structure.
 *
 * Returns: (transfer none): The structure of the event. The structure
 * is still owned by the event, which means that you should not free
 * it and that the pointer becomes invalid when you free the event.
 * This function checks if @event is writable and will never return
 * %NULL.
 *
 * MT safe.
 */
GstStructure *
gst_event_writable_structure (GstEvent * event)
{
  GstStructure *structure;

  g_return_val_if_fail (GST_IS_EVENT (event), NULL);
  g_return_val_if_fail (gst_event_is_writable (event), NULL);

  structure = GST_EVENT_STRUCTURE (event);

  if (structure == NULL) {
    structure =
        gst_structure_new_id_empty (gst_event_type_to_quark (GST_EVENT_TYPE
            (event)));
    gst_structure_set_parent_refcount (structure, &event->mini_object.refcount);
    GST_EVENT_STRUCTURE (event) = structure;
  }
  return structure;
}

/**
 * gst_event_has_name:
 * @event: The #GstEvent.
 * @name: name to check
 *
 * Checks if @event has the given @name. This function is usually used to
 * check the name of a custom event.
 *
 * Returns: %TRUE if @name matches the name of the event structure.
 */
gboolean
gst_event_has_name (GstEvent * event, const gchar * name)
{
  g_return_val_if_fail (GST_IS_EVENT (event), FALSE);

  if (GST_EVENT_STRUCTURE (event) == NULL)
    return FALSE;

  return gst_structure_has_name (GST_EVENT_STRUCTURE (event), name);
}

/**
 * gst_event_has_name_id:
 * @event: The #GstEvent.
 * @name: name to check as a GQuark
 *
 * Checks if @event has the given @name. This function is usually used to
 * check the name of a custom event.
 *
 * Returns: %TRUE if @name matches the name of the event structure.
 *
 * Since: 1.18
 */
gboolean
gst_event_has_name_id (GstEvent * event, GQuark name)
{
  g_return_val_if_fail (GST_IS_EVENT (event), FALSE);

  if (GST_EVENT_STRUCTURE (event) == NULL)
    return FALSE;

  return (GST_EVENT_STRUCTURE (event)->name == name);
}

/**
 * gst_event_get_seqnum:
 * @event: A #GstEvent.
 *
 * Retrieve the sequence number of a event.
 *
 * Events have ever-incrementing sequence numbers, which may also be set
 * explicitly via gst_event_set_seqnum(). Sequence numbers are typically used to
 * indicate that a event corresponds to some other set of events or messages,
 * for example an EOS event corresponding to a SEEK event. It is considered good
 * practice to make this correspondence when possible, though it is not
 * required.
 *
 * Note that events and messages share the same sequence number incrementor;
 * two events or messages will never have the same sequence number unless
 * that correspondence was made explicitly.
 *
 * Returns: The event's sequence number.
 *
 * MT safe.
 */
guint32
gst_event_get_seqnum (GstEvent * event)
{
  g_return_val_if_fail (GST_IS_EVENT (event), -1);

  return GST_EVENT_SEQNUM (event);
}

/**
 * gst_event_set_seqnum:
 * @event: A #GstEvent.
 * @seqnum: A sequence number.
 *
 * Set the sequence number of a event.
 *
 * This function might be called by the creator of a event to indicate that the
 * event relates to other events or messages. See gst_event_get_seqnum() for
 * more information.
 *
 * MT safe.
 */
void
gst_event_set_seqnum (GstEvent * event, guint32 seqnum)
{
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (seqnum != GST_SEQNUM_INVALID);
  g_return_if_fail (gst_event_is_writable (event));

  GST_EVENT_SEQNUM (event) = seqnum;
}

/**
 * gst_event_get_running_time_offset:
 * @event: A #GstEvent.
 *
 * Retrieve the accumulated running time offset of the event.
 *
 * Events passing through #GstPads that have a running time
 * offset set via gst_pad_set_offset() will get their offset
 * adjusted according to the pad's offset.
 *
 * If the event contains any information that related to the
 * running time, this information will need to be updated
 * before usage with this offset.
 *
 * Returns: The event's running time offset
 *
 * MT safe.
 *
 * Since: 1.4
 */
gint64
gst_event_get_running_time_offset (GstEvent * event)
{
  g_return_val_if_fail (GST_IS_EVENT (event), 0);

  return ((GstEventImpl *) event)->running_time_offset;
}

/**
 * gst_event_set_running_time_offset:
 * @event: A #GstEvent.
 * @offset: A the new running time offset
 *
 * Set the running time offset of a event. See
 * gst_event_get_running_time_offset() for more information.
 *
 * MT safe.
 *
 * Since: 1.4
 */
void
gst_event_set_running_time_offset (GstEvent * event, gint64 offset)
{
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (gst_event_is_writable (event));

  ((GstEventImpl *) event)->running_time_offset = offset;
}

/**
 * gst_event_new_flush_start:
 *
 * Allocate a new flush start event. The flush start event can be sent
 * upstream and downstream and travels out-of-bounds with the dataflow.
 *
 * It marks pads as being flushing and will make them return
 * #GST_FLOW_FLUSHING when used for data flow with gst_pad_push(),
 * gst_pad_chain(), gst_pad_get_range() and gst_pad_pull_range().
 * Any event (except a #GST_EVENT_FLUSH_STOP) received
 * on a flushing pad will return %FALSE immediately.
 *
 * Elements should unlock any blocking functions and exit their streaming
 * functions as fast as possible when this event is received.
 *
 * This event is typically generated after a seek to flush out all queued data
 * in the pipeline so that the new media is played as soon as possible.
 *
 * Returns: (transfer full): a new flush start event.
 */
GstEvent *
gst_event_new_flush_start (void)
{
  return gst_event_new_custom (GST_EVENT_FLUSH_START, NULL);
}

/**
 * gst_event_new_flush_stop:
 * @reset_time: if time should be reset
 *
 * Allocate a new flush stop event. The flush stop event can be sent
 * upstream and downstream and travels serialized with the dataflow.
 * It is typically sent after sending a FLUSH_START event to make the
 * pads accept data again.
 *
 * Elements can process this event synchronized with the dataflow since
 * the preceding FLUSH_START event stopped the dataflow.
 *
 * This event is typically generated to complete a seek and to resume
 * dataflow.
 *
 * Returns: (transfer full): a new flush stop event.
 */
GstEvent *
gst_event_new_flush_stop (gboolean reset_time)
{
  GstEvent *event;

  GST_CAT_INFO (GST_CAT_EVENT, "creating flush stop %d", reset_time);

  event = gst_event_new_custom (GST_EVENT_FLUSH_STOP,
      gst_structure_new_id (GST_QUARK (EVENT_FLUSH_STOP),
          GST_QUARK (RESET_TIME), G_TYPE_BOOLEAN, reset_time, NULL));

  return event;
}

/**
 * gst_event_parse_flush_stop:
 * @event: The event to parse
 * @reset_time: (out): if time should be reset
 *
 * Parse the FLUSH_STOP event and retrieve the @reset_time member.
 */
void
gst_event_parse_flush_stop (GstEvent * event, gboolean * reset_time)
{
  GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_FLUSH_STOP);

  structure = GST_EVENT_STRUCTURE (event);
  if (G_LIKELY (reset_time))
    *reset_time =
        g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (RESET_TIME)));
}

/**
 * gst_event_new_select_streams:
 * @streams: (element-type utf8) (transfer none): the list of streams to
 * activate
 *
 * Allocate a new select-streams event.
 *
 * The select-streams event requests the specified @streams to be activated.
 *
 * The list of @streams corresponds to the "Stream ID" of each stream to be
 * activated. Those ID can be obtained via the #GstStream objects present
 * in #GST_EVENT_STREAM_START, #GST_EVENT_STREAM_COLLECTION or
 * #GST_MESSAGE_STREAM_COLLECTION.
 *
 * Note: The list of @streams can not be empty.
 *
 * Returns: (transfer full): a new select-streams event or %NULL in case of
 * an error (like an empty streams list).
 *
 * Since: 1.10
 */
GstEvent *
gst_event_new_select_streams (GList * streams)
{
  GstEvent *event;
  GValue val = G_VALUE_INIT;
  GstStructure *struc;
  GList *tmpl;

  g_return_val_if_fail (streams != NULL, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "Creating new select-streams event");
  struc = gst_structure_new_id_empty (GST_QUARK (EVENT_SELECT_STREAMS));
  g_value_init (&val, GST_TYPE_LIST);
  /* Fill struc with streams */
  for (tmpl = streams; tmpl; tmpl = tmpl->next) {
    GValue strval = G_VALUE_INIT;
    const gchar *str = (const gchar *) tmpl->data;
    g_value_init (&strval, G_TYPE_STRING);
    g_value_set_string (&strval, str);
    gst_value_list_append_and_take_value (&val, &strval);
  }
  gst_structure_id_take_value (struc, GST_QUARK (STREAMS), &val);
  event = gst_event_new_custom (GST_EVENT_SELECT_STREAMS, struc);

  return event;
}

/**
 * gst_event_parse_select_streams:
 * @event: The event to parse
 * @streams: (out) (element-type utf8) (transfer full): the streams
 *
 * Parse the SELECT_STREAMS event and retrieve the contained streams.
 *
 * Since: 1.10
 */
void
gst_event_parse_select_streams (GstEvent * event, GList ** streams)
{
  GstStructure *structure;
  GList *res = NULL;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SELECT_STREAMS);

  structure = GST_EVENT_STRUCTURE (event);
  if (G_LIKELY (streams)) {
    const GValue *vlist =
        gst_structure_id_get_value (structure, GST_QUARK (STREAMS));
    guint i, sz = gst_value_list_get_size (vlist);
    for (i = 0; i < sz; i++) {
      const GValue *strv = gst_value_list_get_value (vlist, i);
      res = g_list_append (res, g_value_dup_string (strv));
    }
    *streams = res;
  }
}


/**
 * gst_event_new_stream_group_done:
 * @group_id: the group id of the stream group which is ending
 *
 * Create a new Stream Group Done event. The stream-group-done event can
 * only travel downstream synchronized with the buffer flow. Elements
 * that receive the event on a pad should handle it mostly like EOS,
 * and emit any data or pending buffers that would depend on more data
 * arriving and unblock, since there won't be any more data.
 *
 * This event is followed by EOS at some point in the future, and is
 * generally used when switching pads - to unblock downstream so that
 * new pads can be exposed before sending EOS on the existing pads.
 *
 * Returns: (transfer full): the new stream-group-done event.
 *
 * Since: 1.10
 */
GstEvent *
gst_event_new_stream_group_done (guint group_id)
{
  GstStructure *s;

  g_return_val_if_fail (group_id != GST_GROUP_ID_INVALID, NULL);

  s = gst_structure_new_id (GST_QUARK (EVENT_STREAM_GROUP_DONE),
      GST_QUARK (GROUP_ID), G_TYPE_UINT, group_id, NULL);

  return gst_event_new_custom (GST_EVENT_STREAM_GROUP_DONE, s);
}

/**
 * gst_event_parse_stream_group_done:
 * @event: a stream-group-done event.
 * @group_id: (out): address of variable to store the group id into
 *
 * Parse a stream-group-done @event and store the result in the given
 * @group_id location.
 *
 * Since: 1.10
 */
void
gst_event_parse_stream_group_done (GstEvent * event, guint * group_id)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_GROUP_DONE);

  if (group_id) {
    gst_structure_id_get (GST_EVENT_STRUCTURE (event),
        GST_QUARK (GROUP_ID), G_TYPE_UINT, group_id, NULL);
  }
}

/**
 * gst_event_new_eos:
 *
 * Create a new EOS event. The eos event can only travel downstream
 * synchronized with the buffer flow. Elements that receive the EOS
 * event on a pad can return #GST_FLOW_EOS as a #GstFlowReturn
 * when data after the EOS event arrives.
 *
 * The EOS event will travel down to the sink elements in the pipeline
 * which will then post the #GST_MESSAGE_EOS on the bus after they have
 * finished playing any buffered data.
 *
 * When all sinks have posted an EOS message, an EOS message is
 * forwarded to the application.
 *
 * The EOS event itself will not cause any state transitions of the pipeline.
 *
 * Returns: (transfer full): the new EOS event.
 */
GstEvent *
gst_event_new_eos (void)
{
  return gst_event_new_custom (GST_EVENT_EOS, NULL);
}

/**
 * gst_event_new_gap:
 * @timestamp: the start time (pts) of the gap
 * @duration: the duration of the gap
 *
 * Create a new GAP event. A gap event can be thought of as conceptually
 * equivalent to a buffer to signal that there is no data for a certain
 * amount of time. This is useful to signal a gap to downstream elements
 * which may wait for data, such as muxers or mixers or overlays, especially
 * for sparse streams such as subtitle streams.
 *
 * Returns: (transfer full): the new GAP event.
 */
GstEvent *
gst_event_new_gap (GstClockTime timestamp, GstClockTime duration)
{
  GstEvent *event;

  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (timestamp), NULL);

  GST_CAT_TRACE (GST_CAT_EVENT, "creating gap %" GST_TIME_FORMAT " - "
      "%" GST_TIME_FORMAT " (duration: %" GST_TIME_FORMAT ")",
      GST_TIME_ARGS (timestamp), GST_TIME_ARGS (timestamp + duration),
      GST_TIME_ARGS (duration));

  event = gst_event_new_custom (GST_EVENT_GAP,
      gst_structure_new_id (GST_QUARK (EVENT_GAP),
          GST_QUARK (TIMESTAMP), GST_TYPE_CLOCK_TIME, timestamp,
          GST_QUARK (DURATION), GST_TYPE_CLOCK_TIME, duration, NULL));

  return event;
}

/**
 * gst_event_parse_gap:
 * @event: a #GstEvent of type #GST_EVENT_GAP
 * @timestamp: (out) (allow-none): location where to store the
 *     start time (pts) of the gap, or %NULL
 * @duration: (out) (allow-none): location where to store the duration of
 *     the gap, or %NULL
 *
 * Extract timestamp and duration from a new GAP event.
 */
void
gst_event_parse_gap (GstEvent * event, GstClockTime * timestamp,
    GstClockTime * duration)
{
  GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_GAP);

  structure = GST_EVENT_STRUCTURE (event);
  gst_structure_id_get (structure,
      GST_QUARK (TIMESTAMP), GST_TYPE_CLOCK_TIME, timestamp,
      GST_QUARK (DURATION), GST_TYPE_CLOCK_TIME, duration, NULL);
}

/**
 * gst_event_new_caps:
 * @caps: (transfer none): a #GstCaps
 *
 * Create a new CAPS event for @caps. The caps event can only travel downstream
 * synchronized with the buffer flow and contains the format of the buffers
 * that will follow after the event.
 *
 * Returns: (transfer full) (nullable): the new CAPS event.
 */
GstEvent *
gst_event_new_caps (GstCaps * caps)
{
  GstEvent *event;

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (gst_caps_is_fixed (caps), NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating caps event %" GST_PTR_FORMAT, caps);

  event = gst_event_new_custom (GST_EVENT_CAPS,
      gst_structure_new_id (GST_QUARK (EVENT_CAPS),
          GST_QUARK (CAPS), GST_TYPE_CAPS, caps, NULL));

  return event;
}

/**
 * gst_event_parse_caps:
 * @event: The event to parse
 * @caps: (out) (transfer none): A pointer to the caps
 *
 * Get the caps from @event. The caps remains valid as long as @event remains
 * valid.
 */
void
gst_event_parse_caps (GstEvent * event, GstCaps ** caps)
{
  GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_CAPS);

  structure = GST_EVENT_STRUCTURE (event);
  if (G_LIKELY (caps))
    *caps =
        g_value_get_boxed (gst_structure_id_get_value (structure,
            GST_QUARK (CAPS)));
}

/**
 * gst_event_new_segment:
 * @segment: (transfer none): a #GstSegment
 *
 * Create a new SEGMENT event for @segment. The segment event can only travel
 * downstream synchronized with the buffer flow and contains timing information
 * and playback properties for the buffers that will follow.
 *
 * The segment event marks the range of buffers to be processed. All
 * data not within the segment range is not to be processed. This can be
 * used intelligently by plugins to apply more efficient methods of skipping
 * unneeded data. The valid range is expressed with the @start and @stop
 * values.
 *
 * The time value of the segment is used in conjunction with the start
 * value to convert the buffer timestamps into the stream time. This is
 * usually done in sinks to report the current stream_time.
 * @time represents the stream_time of a buffer carrying a timestamp of
 * @start. @time cannot be -1.
 *
 * @start cannot be -1, @stop can be -1. If there
 * is a valid @stop given, it must be greater or equal the @start, including
 * when the indicated playback @rate is < 0.
 *
 * The @applied_rate value provides information about any rate adjustment that
 * has already been made to the timestamps and content on the buffers of the
 * stream. (@rate * @applied_rate) should always equal the rate that has been
 * requested for playback. For example, if an element has an input segment
 * with intended playback @rate of 2.0 and applied_rate of 1.0, it can adjust
 * incoming timestamps and buffer content by half and output a segment event
 * with @rate of 1.0 and @applied_rate of 2.0
 *
 * After a segment event, the buffer stream time is calculated with:
 *
 *   time + (TIMESTAMP(buf) - start) * ABS (rate * applied_rate)
 *
 * Returns: (transfer full) (nullable): the new SEGMENT event.
 */
GstEvent *
gst_event_new_segment (const GstSegment * segment)
{
  GstEvent *event;

  g_return_val_if_fail (segment != NULL, NULL);
  g_return_val_if_fail (segment->rate != 0.0, NULL);
  g_return_val_if_fail (segment->applied_rate != 0.0, NULL);
  g_return_val_if_fail (segment->format != GST_FORMAT_UNDEFINED, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating segment event %" GST_SEGMENT_FORMAT,
      segment);

  event = gst_event_new_custom (GST_EVENT_SEGMENT,
      gst_structure_new_id (GST_QUARK (EVENT_SEGMENT),
          GST_QUARK (SEGMENT), GST_TYPE_SEGMENT, segment, NULL));

  return event;
}

/**
 * gst_event_parse_segment:
 * @event: The event to parse
 * @segment: (out) (transfer none): a pointer to a #GstSegment
 *
 * Parses a segment @event and stores the result in the given @segment location.
 * @segment remains valid only until the @event is freed. Don't modify the segment
 * and make a copy if you want to modify it or store it for later use.
 */
void
gst_event_parse_segment (GstEvent * event, const GstSegment ** segment)
{
  GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SEGMENT);

  if (segment) {
    structure = GST_EVENT_STRUCTURE (event);
    *segment = g_value_get_boxed (gst_structure_id_get_value (structure,
            GST_QUARK (SEGMENT)));
  }
}

/**
 * gst_event_copy_segment:
 * @event: The event to parse
 * @segment: a pointer to a #GstSegment
 *
 * Parses a segment @event and copies the #GstSegment into the location
 * given by @segment.
 */
void
gst_event_copy_segment (GstEvent * event, GstSegment * segment)
{
  const GstSegment *src;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SEGMENT);

  if (segment) {
    gst_event_parse_segment (event, &src);
    gst_segment_copy_into (src, segment);
  }
}

/**
 * gst_event_new_tag:
 * @taglist: (transfer full): metadata list. The event will take ownership
 *     of the taglist.
 *
 * Generates a metadata tag event from the given @taglist.
 *
 * The scope of the taglist specifies if the taglist applies to the
 * complete medium or only to this specific stream. As the tag event
 * is a sticky event, elements should merge tags received from
 * upstream with a given scope with their own tags with the same
 * scope and create a new tag event from it.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_tag (GstTagList * taglist)
{
  GstStructure *s;
  GValue val = G_VALUE_INIT;
  const gchar *names[] = { "GstTagList-stream", "GstTagList-global" };

  g_return_val_if_fail (taglist != NULL, NULL);

  s = gst_structure_new_empty (names[gst_tag_list_get_scope (taglist)]);
  g_value_init (&val, GST_TYPE_TAG_LIST);
  g_value_take_boxed (&val, taglist);
  gst_structure_id_take_value (s, GST_QUARK (TAGLIST), &val);
  return gst_event_new_custom (GST_EVENT_TAG, s);
}

/**
 * gst_event_parse_tag:
 * @event: a tag event
 * @taglist: (out) (transfer none): pointer to metadata list
 *
 * Parses a tag @event and stores the results in the given @taglist location.
 * No reference to the taglist will be returned, it remains valid only until
 * the @event is freed. Don't modify or free the taglist, make a copy if you
 * want to modify it or store it for later use.
 */
void
gst_event_parse_tag (GstEvent * event, GstTagList ** taglist)
{
  const GValue *val;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_TAG);

  val = gst_structure_id_get_value (GST_EVENT_STRUCTURE (event),
      GST_QUARK (TAGLIST));

  if (taglist)
    *taglist = (GstTagList *) g_value_get_boxed (val);
}

/* buffersize event */
/**
 * gst_event_new_buffer_size:
 * @format: buffer format
 * @minsize: minimum buffer size
 * @maxsize: maximum buffer size
 * @async: thread behavior
 *
 * Create a new buffersize event. The event is sent downstream and notifies
 * elements that they should provide a buffer of the specified dimensions.
 *
 * When the @async flag is set, a thread boundary is preferred.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_buffer_size (GstFormat format, gint64 minsize,
    gint64 maxsize, gboolean async)
{
  GstEvent *event;
  GstStructure *structure;

  GST_CAT_INFO (GST_CAT_EVENT,
      "creating buffersize format %s, minsize %" G_GINT64_FORMAT
      ", maxsize %" G_GINT64_FORMAT ", async %d", gst_format_get_name (format),
      minsize, maxsize, async);

  structure = gst_structure_new_id (GST_QUARK (EVENT_BUFFER_SIZE),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (MINSIZE), G_TYPE_INT64, minsize,
      GST_QUARK (MAXSIZE), G_TYPE_INT64, maxsize,
      GST_QUARK (ASYNC), G_TYPE_BOOLEAN, async, NULL);
  event = gst_event_new_custom (GST_EVENT_BUFFERSIZE, structure);

  return event;
}

/**
 * gst_event_parse_buffer_size:
 * @event: The event to query
 * @format: (out): A pointer to store the format in
 * @minsize: (out): A pointer to store the minsize in
 * @maxsize: (out): A pointer to store the maxsize in
 * @async: (out): A pointer to store the async-flag in
 *
 * Get the format, minsize, maxsize and async-flag in the buffersize event.
 */
void
gst_event_parse_buffer_size (GstEvent * event, GstFormat * format,
    gint64 * minsize, gint64 * maxsize, gboolean * async)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_BUFFERSIZE);

  structure = GST_EVENT_STRUCTURE (event);
  if (format)
    *format = (GstFormat)
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (minsize)
    *minsize =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (MINSIZE)));
  if (maxsize)
    *maxsize =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (MAXSIZE)));
  if (async)
    *async =
        g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (ASYNC)));
}

/**
 * gst_event_new_qos:
 * @type: the QoS type
 * @proportion: the proportion of the qos message
 * @diff: The time difference of the last Clock sync
 * @timestamp: The timestamp of the buffer
 *
 * Allocate a new qos event with the given values.
 * The QOS event is generated in an element that wants an upstream
 * element to either reduce or increase its rate because of
 * high/low CPU load or other resource usage such as network performance or
 * throttling. Typically sinks generate these events for each buffer
 * they receive.
 *
 * @type indicates the reason for the QoS event. #GST_QOS_TYPE_OVERFLOW is
 * used when a buffer arrived in time or when the sink cannot keep up with
 * the upstream datarate. #GST_QOS_TYPE_UNDERFLOW is when the sink is not
 * receiving buffers fast enough and thus has to drop late buffers.
 * #GST_QOS_TYPE_THROTTLE is used when the datarate is artificially limited
 * by the application, for example to reduce power consumption.
 *
 * @proportion indicates the real-time performance of the streaming in the
 * element that generated the QoS event (usually the sink). The value is
 * generally computed based on more long term statistics about the streams
 * timestamps compared to the clock.
 * A value < 1.0 indicates that the upstream element is producing data faster
 * than real-time. A value > 1.0 indicates that the upstream element is not
 * producing data fast enough. 1.0 is the ideal @proportion value. The
 * proportion value can safely be used to lower or increase the quality of
 * the element.
 *
 * @diff is the difference against the clock in running time of the last
 * buffer that caused the element to generate the QOS event. A negative value
 * means that the buffer with @timestamp arrived in time. A positive value
 * indicates how late the buffer with @timestamp was. When throttling is
 * enabled, @diff will be set to the requested throttling interval.
 *
 * @timestamp is the timestamp of the last buffer that cause the element
 * to generate the QOS event. It is expressed in running time and thus an ever
 * increasing value.
 *
 * The upstream element can use the @diff and @timestamp values to decide
 * whether to process more buffers. For positive @diff, all buffers with
 * timestamp <= @timestamp + @diff will certainly arrive late in the sink
 * as well. A (negative) @diff value so that @timestamp + @diff would yield a
 * result smaller than 0 is not allowed.
 *
 * The application can use general event probes to intercept the QoS
 * event and implement custom application specific QoS handling.
 *
 * Returns: (transfer full) (nullable): a new QOS event.
 */
GstEvent *
gst_event_new_qos (GstQOSType type, gdouble proportion,
    GstClockTimeDiff diff, GstClockTime timestamp)
{
  GstEvent *event;
  GstStructure *structure;

  /* diff must be positive or timestamp + diff must be positive */
  g_return_val_if_fail (diff >= 0 || -diff <= timestamp, NULL);

  GST_CAT_LOG (GST_CAT_EVENT,
      "creating qos type %d, proportion %lf, diff %" G_GINT64_FORMAT
      ", timestamp %" GST_TIME_FORMAT, type, proportion,
      diff, GST_TIME_ARGS (timestamp));

  structure = gst_structure_new_id (GST_QUARK (EVENT_QOS),
      GST_QUARK (TYPE), GST_TYPE_QOS_TYPE, type,
      GST_QUARK (PROPORTION), G_TYPE_DOUBLE, proportion,
      GST_QUARK (DIFF), G_TYPE_INT64, diff,
      GST_QUARK (TIMESTAMP), G_TYPE_UINT64, timestamp, NULL);
  event = gst_event_new_custom (GST_EVENT_QOS, structure);

  return event;
}

/**
 * gst_event_parse_qos:
 * @event: The event to query
 * @type: (out): A pointer to store the QoS type in
 * @proportion: (out): A pointer to store the proportion in
 * @diff: (out): A pointer to store the diff in
 * @timestamp: (out): A pointer to store the timestamp in
 *
 * Get the type, proportion, diff and timestamp in the qos event. See
 * gst_event_new_qos() for more information about the different QoS values.
 *
 * @timestamp will be adjusted for any pad offsets of pads it was passing through.
 */
void
gst_event_parse_qos (GstEvent * event, GstQOSType * type,
    gdouble * proportion, GstClockTimeDiff * diff, GstClockTime * timestamp)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_QOS);

  structure = GST_EVENT_STRUCTURE (event);
  if (type)
    *type = (GstQOSType)
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (TYPE)));
  if (proportion)
    *proportion =
        g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (PROPORTION)));
  if (diff)
    *diff =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (DIFF)));
  if (timestamp) {
    gint64 offset = gst_event_get_running_time_offset (event);
    GstClockTimeDiff diff_ =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (DIFF)));

    *timestamp =
        g_value_get_uint64 (gst_structure_id_get_value (structure,
            GST_QUARK (TIMESTAMP)));
    /* Catch underflows */
    if (*timestamp > -offset)
      *timestamp += offset;
    else
      *timestamp = 0;

    /* Make sure that timestamp + diff is always >= 0. Because
     * of the running time offset this might not be true */
    if (diff_ < 0 && *timestamp < -diff_)
      *timestamp = (GstClockTime) - diff_;
  }
}

/**
 * gst_event_new_seek:
 * @rate: The new playback rate
 * @format: The format of the seek values
 * @flags: The optional seek flags
 * @start_type: The type and flags for the new start position
 * @start: The value of the new start position
 * @stop_type: The type and flags for the new stop position
 * @stop: The value of the new stop position
 *
 * Allocate a new seek event with the given parameters.
 *
 * The seek event configures playback of the pipeline between @start to @stop
 * at the speed given in @rate, also called a playback segment.
 * The @start and @stop values are expressed in @format.
 *
 * A @rate of 1.0 means normal playback rate, 2.0 means double speed.
 * Negatives values means backwards playback. A value of 0.0 for the
 * rate is not allowed and should be accomplished instead by PAUSING the
 * pipeline.
 *
 * A pipeline has a default playback segment configured with a start
 * position of 0, a stop position of -1 and a rate of 1.0. The currently
 * configured playback segment can be queried with #GST_QUERY_SEGMENT.
 *
 * @start_type and @stop_type specify how to adjust the currently configured
 * start and stop fields in playback segment. Adjustments can be made relative
 * or absolute to the last configured values. A type of #GST_SEEK_TYPE_NONE
 * means that the position should not be updated.
 *
 * When the rate is positive and @start has been updated, playback will start
 * from the newly configured start position.
 *
 * For negative rates, playback will start from the newly configured stop
 * position (if any). If the stop position is updated, it must be different from
 * -1 (#GST_CLOCK_TIME_NONE) for negative rates.
 *
 * It is not possible to seek relative to the current playback position, to do
 * this, PAUSE the pipeline, query the current playback position with
 * #GST_QUERY_POSITION and update the playback segment current position with a
 * #GST_SEEK_TYPE_SET to the desired position.
 *
 * Returns: (transfer full) (nullable): a new seek event.
 */
GstEvent *
gst_event_new_seek (gdouble rate, GstFormat format, GstSeekFlags flags,
    GstSeekType start_type, gint64 start, GstSeekType stop_type, gint64 stop)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (rate != 0.0, NULL);
  g_return_val_if_fail ((flags & GST_SEEK_FLAG_INSTANT_RATE_CHANGE) == 0
      || (start_type == GST_SEEK_TYPE_NONE
          && stop_type == GST_SEEK_TYPE_NONE
          && (flags & GST_SEEK_FLAG_FLUSH) == 0), NULL);

  /* SNAP flags only make sense in combination with the KEYUNIT flag. Warn
   * and unset the SNAP flags if they're set without the KEYUNIT flag */
  if (!(flags & GST_SEEK_FLAG_KEY_UNIT) &&
      (flags & (GST_SEEK_FLAG_SNAP_BEFORE | GST_SEEK_FLAG_SNAP_AFTER |
              GST_SEEK_FLAG_SNAP_NEAREST))) {
    g_warning ("SNAP seeks only work in combination with the KEY_UNIT "
        "flag, ignoring SNAP flags");
    flags &=
        ~(GST_SEEK_FLAG_SNAP_BEFORE | GST_SEEK_FLAG_SNAP_AFTER |
        GST_SEEK_FLAG_SNAP_NEAREST);
  }

  if (format == GST_FORMAT_TIME) {
    GST_CAT_INFO (GST_CAT_EVENT,
        "creating seek rate %lf, format TIME, flags %d, "
        "start_type %d, start %" GST_TIME_FORMAT ", "
        "stop_type %d, stop %" GST_TIME_FORMAT,
        rate, flags, start_type, GST_TIME_ARGS (start),
        stop_type, GST_TIME_ARGS (stop));
  } else {
    GST_CAT_INFO (GST_CAT_EVENT,
        "creating seek rate %lf, format %s, flags %d, "
        "start_type %d, start %" G_GINT64_FORMAT ", "
        "stop_type %d, stop %" G_GINT64_FORMAT,
        rate, gst_format_get_name (format), flags, start_type, start, stop_type,
        stop);
  }

  structure = gst_structure_new_id (GST_QUARK (EVENT_SEEK),
      GST_QUARK (RATE), G_TYPE_DOUBLE, rate,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (FLAGS), GST_TYPE_SEEK_FLAGS, flags,
      GST_QUARK (CUR_TYPE), GST_TYPE_SEEK_TYPE, start_type,
      GST_QUARK (CUR), G_TYPE_INT64, start,
      GST_QUARK (STOP_TYPE), GST_TYPE_SEEK_TYPE, stop_type,
      GST_QUARK (STOP), G_TYPE_INT64, stop,
      GST_QUARK (TRICKMODE_INTERVAL), GST_TYPE_CLOCK_TIME, (GstClockTime) 0,
      NULL);
  event = gst_event_new_custom (GST_EVENT_SEEK, structure);

  return event;
}

/**
 * gst_event_parse_seek:
 * @event: a seek event
 * @rate: (out): result location for the rate
 * @format: (out): result location for the stream format
 * @flags:  (out): result location for the #GstSeekFlags
 * @start_type: (out): result location for the #GstSeekType of the start position
 * @start: (out): result location for the start position expressed in @format
 * @stop_type:  (out): result location for the #GstSeekType of the stop position
 * @stop: (out): result location for the stop position expressed in @format
 *
 * Parses a seek @event and stores the results in the given result locations.
 */
void
gst_event_parse_seek (GstEvent * event, gdouble * rate,
    GstFormat * format, GstSeekFlags * flags, GstSeekType * start_type,
    gint64 * start, GstSeekType * stop_type, gint64 * stop)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SEEK);

  structure = GST_EVENT_STRUCTURE (event);
  if (rate)
    *rate =
        g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (RATE)));
  if (format)
    *format = (GstFormat)
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (flags)
    *flags = (GstSeekFlags)
        g_value_get_flags (gst_structure_id_get_value (structure,
            GST_QUARK (FLAGS)));
  if (start_type)
    *start_type = (GstSeekType)
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (CUR_TYPE)));
  if (start)
    *start =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (CUR)));
  if (stop_type)
    *stop_type = (GstSeekType)
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (STOP_TYPE)));
  if (stop)
    *stop =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (STOP)));
}

/**
 * gst_event_set_seek_trickmode_interval:
 *
 * Sets a trickmode interval on a (writable) seek event. Elements
 * that support TRICKMODE_KEY_UNITS seeks SHOULD use this as the minimal
 * interval between each frame they may output.
 *
 * Since: 1.16
 */
void
gst_event_set_seek_trickmode_interval (GstEvent * event, GstClockTime interval)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SEEK);
  g_return_if_fail (gst_event_is_writable (event));
  g_return_if_fail (GST_CLOCK_TIME_IS_VALID (interval));

  gst_structure_id_set (GST_EVENT_STRUCTURE (event),
      GST_QUARK (TRICKMODE_INTERVAL), GST_TYPE_CLOCK_TIME, interval, NULL);
}

/**
 * gst_event_parse_seek_trickmode_interval:
 * @interval: (out)
 *
 * Retrieve the trickmode interval that may have been set on a
 * seek event with gst_event_set_seek_trickmode_interval().
 *
 * Since: 1.16
 */
void
gst_event_parse_seek_trickmode_interval (GstEvent * event,
    GstClockTime * interval)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (interval != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SEEK);

  gst_structure_id_get (GST_EVENT_STRUCTURE (event),
      GST_QUARK (TRICKMODE_INTERVAL), GST_TYPE_CLOCK_TIME, interval, NULL);
}

/**
 * gst_event_new_navigation:
 * @structure: (transfer full): description of the event. The event will take
 *     ownership of the structure.
 *
 * Create a new navigation event from the given description.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_navigation (GstStructure * structure)
{
  g_return_val_if_fail (structure != NULL, NULL);

  return gst_event_new_custom (GST_EVENT_NAVIGATION, structure);
}

/**
 * gst_event_new_latency:
 * @latency: the new latency value
 *
 * Create a new latency event. The event is sent upstream from the sinks and
 * notifies elements that they should add an additional @latency to the
 * running time before synchronising against the clock.
 *
 * The latency is mostly used in live sinks and is always expressed in
 * the time format.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_latency (GstClockTime latency)
{
  GstEvent *event;
  GstStructure *structure;

  GST_CAT_INFO (GST_CAT_EVENT,
      "creating latency event %" GST_TIME_FORMAT, GST_TIME_ARGS (latency));

  structure = gst_structure_new_id (GST_QUARK (EVENT_LATENCY),
      GST_QUARK (LATENCY), G_TYPE_UINT64, latency, NULL);
  event = gst_event_new_custom (GST_EVENT_LATENCY, structure);

  return event;
}

/**
 * gst_event_parse_latency:
 * @event: The event to query
 * @latency: (out): A pointer to store the latency in.
 *
 * Get the latency in the latency event.
 */
void
gst_event_parse_latency (GstEvent * event, GstClockTime * latency)
{
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_LATENCY);

  if (latency)
    *latency =
        g_value_get_uint64 (gst_structure_id_get_value (GST_EVENT_STRUCTURE
            (event), GST_QUARK (LATENCY)));
}

/**
 * gst_event_new_step:
 * @format: the format of @amount
 * @amount: the amount of data to step
 * @rate: the step rate
 * @flush: flushing steps
 * @intermediate: intermediate steps
 *
 * Create a new step event. The purpose of the step event is to instruct a sink
 * to skip @amount (expressed in @format) of media. It can be used to implement
 * stepping through the video frame by frame or for doing fast trick modes.
 *
 * A rate of <= 0.0 is not allowed. Pause the pipeline, for the effect of rate
 * = 0.0 or first reverse the direction of playback using a seek event to get
 * the same effect as rate < 0.0.
 *
 * The @flush flag will clear any pending data in the pipeline before starting
 * the step operation.
 *
 * The @intermediate flag instructs the pipeline that this step operation is
 * part of a larger step operation.
 *
 * Returns: (transfer full) (nullable): a new #GstEvent
 */
GstEvent *
gst_event_new_step (GstFormat format, guint64 amount, gdouble rate,
    gboolean flush, gboolean intermediate)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (rate > 0.0, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating step event");

  structure = gst_structure_new_id (GST_QUARK (EVENT_STEP),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (AMOUNT), G_TYPE_UINT64, amount,
      GST_QUARK (RATE), G_TYPE_DOUBLE, rate,
      GST_QUARK (FLUSH), G_TYPE_BOOLEAN, flush,
      GST_QUARK (INTERMEDIATE), G_TYPE_BOOLEAN, intermediate, NULL);
  event = gst_event_new_custom (GST_EVENT_STEP, structure);

  return event;
}

/**
 * gst_event_parse_step:
 * @event: The event to query
 * @format: (out) (allow-none): a pointer to store the format in
 * @amount: (out) (allow-none): a pointer to store the amount in
 * @rate: (out) (allow-none): a pointer to store the rate in
 * @flush: (out) (allow-none): a pointer to store the flush boolean in
 * @intermediate: (out) (allow-none): a pointer to store the intermediate
 *     boolean in
 *
 * Parse the step event.
 */
void
gst_event_parse_step (GstEvent * event, GstFormat * format, guint64 * amount,
    gdouble * rate, gboolean * flush, gboolean * intermediate)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STEP);

  structure = GST_EVENT_STRUCTURE (event);
  if (format)
    *format =
        (GstFormat) g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (amount)
    *amount = g_value_get_uint64 (gst_structure_id_get_value (structure,
            GST_QUARK (AMOUNT)));
  if (rate)
    *rate = g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (RATE)));
  if (flush)
    *flush = g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (FLUSH)));
  if (intermediate)
    *intermediate = g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (INTERMEDIATE)));
}

/**
 * gst_event_new_reconfigure:

 * Create a new reconfigure event. The purpose of the reconfigure event is
 * to travel upstream and make elements renegotiate their caps or reconfigure
 * their buffer pools. This is useful when changing properties on elements
 * or changing the topology of the pipeline.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_reconfigure (void)
{
  GstEvent *event;

  GST_CAT_INFO (GST_CAT_EVENT, "creating reconfigure event");

  event = gst_event_new_custom (GST_EVENT_RECONFIGURE, NULL);

  return event;
}

/**
 * gst_event_new_sink_message:
 * @name: a name for the event
 * @msg: (transfer none): the #GstMessage to be posted
 *
 * Create a new sink-message event. The purpose of the sink-message event is
 * to instruct a sink to post the message contained in the event synchronized
 * with the stream.
 *
 * @name is used to store multiple sticky events on one pad.
 *
 * Returns: (transfer full): a new #GstEvent
 */
/* FIXME 2.0: take ownership of msg for consistency? */
GstEvent *
gst_event_new_sink_message (const gchar * name, GstMessage * msg)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (msg != NULL, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating sink-message event");

  structure = gst_structure_new_id (g_quark_from_string (name),
      GST_QUARK (MESSAGE), GST_TYPE_MESSAGE, msg, NULL);
  event = gst_event_new_custom (GST_EVENT_SINK_MESSAGE, structure);

  return event;
}

/**
 * gst_event_parse_sink_message:
 * @event: The event to query
 * @msg: (out) (transfer full): a pointer to store the #GstMessage in.
 *
 * Parse the sink-message event. Unref @msg after usage.
 */
void
gst_event_parse_sink_message (GstEvent * event, GstMessage ** msg)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SINK_MESSAGE);

  structure = GST_EVENT_STRUCTURE (event);
  if (msg)
    *msg =
        GST_MESSAGE (g_value_dup_boxed (gst_structure_id_get_value
            (structure, GST_QUARK (MESSAGE))));
}

/**
 * gst_event_new_stream_start:
 * @stream_id: Identifier for this stream
 *
 * Create a new STREAM_START event. The stream start event can only
 * travel downstream synchronized with the buffer flow. It is expected
 * to be the first event that is sent for a new stream.
 *
 * Source elements, demuxers and other elements that create new streams
 * are supposed to send this event as the first event of a new stream. It
 * should not be sent after a flushing seek or in similar situations
 * and is used to mark the beginning of a new logical stream. Elements
 * combining multiple streams must ensure that this event is only forwarded
 * downstream once and not for every single input stream.
 *
 * The @stream_id should be a unique string that consists of the upstream
 * stream-id, / as separator and a unique stream-id for this specific
 * stream. A new stream-id should only be created for a stream if the upstream
 * stream is split into (potentially) multiple new streams, e.g. in a demuxer,
 * but not for every single element in the pipeline.
 * gst_pad_create_stream_id() or gst_pad_create_stream_id_printf() can be
 * used to create a stream-id.  There are no particular semantics for the
 * stream-id, though it should be deterministic (to support stream matching)
 * and it might be used to order streams (besides any information conveyed by
 * stream flags).
 *
 * Returns: (transfer full): the new STREAM_START event.
 */
GstEvent *
gst_event_new_stream_start (const gchar * stream_id)
{
  GstStructure *s;

  g_return_val_if_fail (stream_id != NULL, NULL);

  s = gst_structure_new_id (GST_QUARK (EVENT_STREAM_START),
      GST_QUARK (STREAM_ID), G_TYPE_STRING, stream_id,
      GST_QUARK (FLAGS), GST_TYPE_STREAM_FLAGS, GST_STREAM_FLAG_NONE, NULL);

  return gst_event_new_custom (GST_EVENT_STREAM_START, s);
}

/**
 * gst_event_parse_stream_start:
 * @event: a stream-start event.
 * @stream_id: (out) (transfer none): pointer to store the stream-id
 *
 * Parse a stream-id @event and store the result in the given @stream_id
 * location. The string stored in @stream_id must not be modified and will
 * remain valid only until @event gets freed. Make a copy if you want to
 * modify it or store it for later use.
 */
void
gst_event_parse_stream_start (GstEvent * event, const gchar ** stream_id)
{
  const GstStructure *structure;
  const GValue *val;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START);

  structure = gst_event_get_structure (event);
  val = gst_structure_id_get_value (structure, GST_QUARK (STREAM_ID));

  if (stream_id)
    *stream_id = g_value_get_string (val);
}

/**
 * gst_event_set_stream:
 * @event: a stream-start event
 * @stream: (transfer none): the stream object to set
 *
 * Set the @stream on the stream-start @event
 *
 * Since: 1.10
 */
void
gst_event_set_stream (GstEvent * event, GstStream * stream)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START);
  g_return_if_fail (gst_event_is_writable (event));

  gst_structure_id_set (GST_EVENT_STRUCTURE (event),
      GST_QUARK (STREAM), GST_TYPE_STREAM, stream, NULL);
}

/**
 * gst_event_parse_stream:
 * @event: a stream-start event
 * @stream: (out) (transfer full): address of variable to store the stream
 *
 * Parse a stream-start @event and extract the #GstStream from it.
 *
 * Since: 1.10
 */
void
gst_event_parse_stream (GstEvent * event, GstStream ** stream)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START);

  if (stream) {
    gst_structure_id_get (GST_EVENT_STRUCTURE (event),
        GST_QUARK (STREAM), GST_TYPE_STREAM, stream, NULL);
  }

}

/**
 * gst_event_set_stream_flags:
 * @event: a stream-start event
 * @flags: the stream flags to set
 *
 * Since: 1.2
 */
void
gst_event_set_stream_flags (GstEvent * event, GstStreamFlags flags)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START);
  g_return_if_fail (gst_event_is_writable (event));

  gst_structure_id_set (GST_EVENT_STRUCTURE (event),
      GST_QUARK (FLAGS), GST_TYPE_STREAM_FLAGS, flags, NULL);
}

/**
 * gst_event_parse_stream_flags:
 * @event: a stream-start event
 * @flags: (out): address of variable where to store the stream flags
 *
 * Since: 1.2
 */
void
gst_event_parse_stream_flags (GstEvent * event, GstStreamFlags * flags)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START);

  if (flags) {
    gst_structure_id_get (GST_EVENT_STRUCTURE (event),
        GST_QUARK (FLAGS), GST_TYPE_STREAM_FLAGS, flags, NULL);
  }
}

/**
 * gst_event_set_group_id:
 * @event: a stream-start event
 * @group_id: the group id to set
 *
 * All streams that have the same group id are supposed to be played
 * together, i.e. all streams inside a container file should have the
 * same group id but different stream ids. The group id should change
 * each time the stream is started, resulting in different group ids
 * each time a file is played for example.
 *
 * Use gst_util_group_id_next() to get a new group id.
 *
 * Since: 1.2
 */
void
gst_event_set_group_id (GstEvent * event, guint group_id)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START);
  g_return_if_fail (gst_event_is_writable (event));
  g_return_if_fail (group_id != GST_GROUP_ID_INVALID);

  gst_structure_id_set (GST_EVENT_STRUCTURE (event),
      GST_QUARK (GROUP_ID), G_TYPE_UINT, group_id, NULL);
}

/**
 * gst_event_parse_group_id:
 * @event: a stream-start event
 * @group_id: (out): address of variable where to store the group id
 *
 * Returns: %TRUE if a group id was set on the event and could be parsed,
 *   %FALSE otherwise.
 *
 * Since: 1.2
 */
gboolean
gst_event_parse_group_id (GstEvent * event, guint * group_id)
{
  g_return_val_if_fail (event != NULL, FALSE);
  g_return_val_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_START,
      FALSE);

  if (group_id) {
    return gst_structure_id_get (GST_EVENT_STRUCTURE (event),
        GST_QUARK (GROUP_ID), G_TYPE_UINT, group_id, NULL);
  }

  return TRUE;
}

/**
 * gst_event_new_stream_collection:
 * @collection: Active collection for this data flow
 *
 * Create a new STREAM_COLLECTION event. The stream collection event can only
 * travel downstream synchronized with the buffer flow.
 *
 * Source elements, demuxers and other elements that manage collections
 * of streams and post #GstStreamCollection messages on the bus also send
 * this event downstream on each pad involved in the collection, so that
 * activation of a new collection can be tracked through the downstream
 * data flow.
 *
 * Returns: (transfer full): the new STREAM_COLLECTION event.
 *
 * Since: 1.10
 */
GstEvent *
gst_event_new_stream_collection (GstStreamCollection * collection)
{
  GstStructure *s;

  g_return_val_if_fail (collection != NULL, NULL);
  g_return_val_if_fail (GST_IS_STREAM_COLLECTION (collection), NULL);

  s = gst_structure_new_id (GST_QUARK (EVENT_STREAM_COLLECTION),
      GST_QUARK (COLLECTION), GST_TYPE_STREAM_COLLECTION, collection, NULL);

  return gst_event_new_custom (GST_EVENT_STREAM_COLLECTION, s);
}

/**
 * gst_event_parse_stream_collection:
 * @event: a stream-collection event
 * @collection: (out): pointer to store the collection
 *
 * Retrieve new #GstStreamCollection from STREAM_COLLECTION event @event.
 *
 * Since: 1.10
 */
void
gst_event_parse_stream_collection (GstEvent * event,
    GstStreamCollection ** collection)
{
  const GstStructure *structure;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STREAM_COLLECTION);

  structure = gst_event_get_structure (event);

  if (collection) {
    gst_structure_id_get (structure,
        GST_QUARK (COLLECTION), GST_TYPE_STREAM_COLLECTION, collection, NULL);
  }
}

/**
 * gst_event_new_toc:
 * @toc: (transfer none): #GstToc structure.
 * @updated: whether @toc was updated or not.
 *
 * Generate a TOC event from the given @toc. The purpose of the TOC event is to
 * inform elements that some kind of the TOC was found.
 *
 * Returns: (transfer full): a new #GstEvent.
 */
GstEvent *
gst_event_new_toc (GstToc * toc, gboolean updated)
{
  GstStructure *toc_struct;
  GQuark id;

  g_return_val_if_fail (toc != NULL, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating toc event");

  /* need different structure names so sticky_multi event stuff on pads
   * works, i.e. both TOC events are kept around */
  if (gst_toc_get_scope (toc) == GST_TOC_SCOPE_GLOBAL)
    id = GST_QUARK (EVENT_TOC_GLOBAL);
  else
    id = GST_QUARK (EVENT_TOC_CURRENT);

  toc_struct = gst_structure_new_id (id,
      GST_QUARK (TOC), GST_TYPE_TOC, toc,
      GST_QUARK (UPDATED), G_TYPE_BOOLEAN, updated, NULL);

  return gst_event_new_custom (GST_EVENT_TOC, toc_struct);
}

/**
 * gst_event_parse_toc:
 * @event: a TOC event.
 * @toc: (out) (transfer full): pointer to #GstToc structure.
 * @updated: (out): pointer to store TOC updated flag.
 *
 * Parse a TOC @event and store the results in the given @toc and @updated locations.
 */
void
gst_event_parse_toc (GstEvent * event, GstToc ** toc, gboolean * updated)
{
  const GstStructure *structure;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_TOC);
  g_return_if_fail (toc != NULL);

  structure = gst_event_get_structure (event);

  gst_structure_id_get (structure,
      GST_QUARK (TOC), GST_TYPE_TOC, toc,
      GST_QUARK (UPDATED), G_TYPE_BOOLEAN, updated, NULL);
}

/**
 * gst_event_new_toc_select:
 * @uid: UID in the TOC to start playback from.
 *
 * Generate a TOC select event with the given @uid. The purpose of the
 * TOC select event is to start playback based on the TOC's entry with the
 * given @uid.
 *
 * Returns: a new #GstEvent.
 */
GstEvent *
gst_event_new_toc_select (const gchar * uid)
{
  GstStructure *structure;

  g_return_val_if_fail (uid != NULL, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating toc select event for UID: %s", uid);

  structure = gst_structure_new_id (GST_QUARK (EVENT_TOC_SELECT),
      GST_QUARK (UID), G_TYPE_STRING, uid, NULL);

  return gst_event_new_custom (GST_EVENT_TOC_SELECT, structure);
}

/**
 * gst_event_parse_toc_select:
 * @event: a TOC select event.
 * @uid: (out) (transfer full) (allow-none): storage for the selection UID.
 *
 * Parse a TOC select @event and store the results in the given @uid location.
 */
void
gst_event_parse_toc_select (GstEvent * event, gchar ** uid)
{
  const GstStructure *structure;
  const GValue *val;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_TOC_SELECT);

  structure = gst_event_get_structure (event);
  val = gst_structure_id_get_value (structure, GST_QUARK (UID));

  if (uid != NULL)
    *uid = g_value_dup_string (val);

}

/**
 * gst_event_new_protection:
 * @system_id: (transfer none): a string holding a UUID that uniquely
 * identifies a protection system.
 * @data: (transfer none): a #GstBuffer holding protection system specific
 * information. The reference count of the buffer will be incremented by one.
 * @origin: a string indicating where the protection
 * information carried in the event was extracted from. The allowed values
 * of this string will depend upon the protection scheme.
 *
 * Creates a new event containing information specific to a particular
 * protection system (uniquely identified by @system_id), by which that
 * protection system can acquire key(s) to decrypt a protected stream.
 *
 * In order for a decryption element to decrypt media
 * protected using a specific system, it first needs all the
 * protection system specific information necessary to acquire the decryption
 * key(s) for that stream. The functions defined here enable this information
 * to be passed in events from elements that extract it
 * (e.g., ISOBMFF demuxers, MPEG DASH demuxers) to protection decrypter
 * elements that use it.
 *
 * Events containing protection system specific information are created using
 * #gst_event_new_protection, and they can be parsed by downstream elements
 * using #gst_event_parse_protection.
 *
 * In Common Encryption, protection system specific information may be located
 * within ISOBMFF files, both in movie (moov) boxes and movie fragment (moof)
 * boxes; it may also be contained in ContentProtection elements within MPEG
 * DASH MPDs. The events created by #gst_event_new_protection contain data
 * identifying from which of these locations the encapsulated protection system
 * specific information originated. This origin information is required as
 * some protection systems use different encodings depending upon where the
 * information originates.
 *
 * The events returned by gst_event_new_protection() are implemented
 * in such a way as to ensure that the most recently-pushed protection info
 * event of a particular @origin and @system_id will
 * be stuck to the output pad of the sending element.
 *
 * Returns: a #GST_EVENT_PROTECTION event, if successful; %NULL
 * if unsuccessful.
 *
 * Since: 1.6
 */
GstEvent *
gst_event_new_protection (const gchar * system_id,
    GstBuffer * data, const gchar * origin)
{
  gchar *event_name;
  GstEvent *event;
  GstStructure *s;

  g_return_val_if_fail (system_id != NULL, NULL);
  g_return_val_if_fail (data != NULL, NULL);

  event_name =
      g_strconcat ("GstProtectionEvent", origin ? "-" : "",
      origin ? origin : "", "-", system_id, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating protection event %s", event_name);

  s = gst_structure_new (event_name, "data", GST_TYPE_BUFFER, data,
      "system_id", G_TYPE_STRING, system_id, NULL);
  if (origin)
    gst_structure_set (s, "origin", G_TYPE_STRING, origin, NULL);
  event = gst_event_new_custom (GST_EVENT_PROTECTION, s);

  g_free (event_name);
  return event;
}

/**
 * gst_event_parse_protection:
 * @event: a #GST_EVENT_PROTECTION event.
 * @system_id: (out) (allow-none) (transfer none): pointer to store the UUID
 * string uniquely identifying a content protection system.
 * @data: (out) (allow-none) (transfer none): pointer to store a #GstBuffer
 * holding protection system specific information.
 * @origin: (out) (allow-none) (transfer none): pointer to store a value that
 * indicates where the protection information carried by @event was extracted
 * from.
 *
 * Parses an event containing protection system specific information and stores
 * the results in @system_id, @data and @origin. The data stored in @system_id,
 * @origin and @data are valid until @event is released.
 *
 * Since: 1.6
 */
void
gst_event_parse_protection (GstEvent * event, const gchar ** system_id,
    GstBuffer ** data, const gchar ** origin)
{
  const GstStructure *s;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_PROTECTION);

  s = gst_event_get_structure (event);

  if (origin)
    *origin = gst_structure_get_string (s, "origin");

  if (system_id)
    *system_id = gst_structure_get_string (s, "system_id");

  if (data) {
    const GValue *value = gst_structure_get_value (s, "data");
    *data = gst_value_get_buffer (value);
  }
}

/**
 * gst_event_new_segment_done:
 * @format: The format of the position being done
 * @position: The position of the segment being done
 *
 * Create a new segment-done event. This event is sent by elements that
 * finish playback of a segment as a result of a segment seek.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_segment_done (GstFormat format, gint64 position)
{
  GstEvent *event;
  GstStructure *structure;

  GST_CAT_INFO (GST_CAT_EVENT, "creating segment-done event");

  structure = gst_structure_new_id (GST_QUARK (EVENT_SEGMENT_DONE),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (POSITION), G_TYPE_INT64, position, NULL);

  event = gst_event_new_custom (GST_EVENT_SEGMENT_DONE, structure);

  return event;
}

/**
 * gst_event_parse_segment_done:
 * @event: A valid #GstEvent of type GST_EVENT_SEGMENT_DONE.
 * @format: (out) (allow-none): Result location for the format, or %NULL
 * @position: (out) (allow-none): Result location for the position, or %NULL
 *
 * Extracts the position and format from the segment done message.
 *
 */
void
gst_event_parse_segment_done (GstEvent * event, GstFormat * format,
    gint64 * position)
{
  const GstStructure *structure;
  const GValue *val;

  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SEGMENT_DONE);

  structure = gst_event_get_structure (event);

  val = gst_structure_id_get_value (structure, GST_QUARK (FORMAT));
  if (format != NULL)
    *format = g_value_get_enum (val);

  val = gst_structure_id_get_value (structure, GST_QUARK (POSITION));
  if (position != NULL)
    *position = g_value_get_int64 (val);
}

/**
 * gst_event_new_instant_rate_change:
 * @rate_multiplier: the multiplier to be applied to the playback rate
 * @new_flags: A new subset of segment flags to replace in segments
 *
 * Create a new instant-rate-change event. This event is sent by seek
 * handlers (e.g. demuxers) when receiving a seek with the
 * %GST_SEEK_FLAG_INSTANT_RATE_CHANGE and signals to downstream elements that
 * the playback rate in the existing segment should be immediately multiplied
 * by the @rate_multiplier factor.
 *
 * The flags provided replace any flags in the existing segment, for the
 * flags within the %GST_SEGMENT_INSTANT_FLAGS set. Other GstSegmentFlags
 * are ignored and not transferred in the event.
 *
 * Returns: (transfer full): the new instant-rate-change event.
 *
 * Since: 1.18
 */
GstEvent *
gst_event_new_instant_rate_change (gdouble rate_multiplier,
    GstSegmentFlags new_flags)
{
  GstEvent *event;

  g_return_val_if_fail (rate_multiplier != 0.0, NULL);

  new_flags &= GST_SEGMENT_INSTANT_FLAGS;

  GST_CAT_TRACE (GST_CAT_EVENT, "creating instant-rate-change event %lf %08x",
      rate_multiplier, new_flags);

  event = gst_event_new_custom (GST_EVENT_INSTANT_RATE_CHANGE,
      gst_structure_new_id (GST_QUARK (EVENT_INSTANT_RATE_CHANGE),
          GST_QUARK (RATE), G_TYPE_DOUBLE, rate_multiplier,
          GST_QUARK (FLAGS), GST_TYPE_SEGMENT_FLAGS, new_flags, NULL));

  return event;
}

/**
 * gst_event_parse_instant_rate_change:
 * @event: a #GstEvent of type #GST_EVENT_INSTANT_RATE_CHANGE
 * @rate_multiplier: (out) (allow-none): location in which to store the rate
 *     multiplier of the instant-rate-change event, or %NULL
 * @new_flags: (out) (allow-none): location in which to store the new
 *     segment flags of the instant-rate-change event, or %NULL
 *
 * Extract rate and flags from an instant-rate-change event.
 *
 * Since: 1.18
 */
void
gst_event_parse_instant_rate_change (GstEvent * event,
    gdouble * rate_multiplier, GstSegmentFlags * new_flags)
{
  GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_INSTANT_RATE_CHANGE);

  structure = GST_EVENT_STRUCTURE (event);
  gst_structure_id_get (structure, GST_QUARK (RATE), G_TYPE_DOUBLE,
      rate_multiplier, GST_QUARK (FLAGS), GST_TYPE_SEGMENT_FLAGS, new_flags,
      NULL);
}

/**
 * gst_event_new_instant_rate_sync_time:
 * @rate_multiplier: the new playback rate multiplier to be applied
 * @running_time: Running time when the rate change should be applied
 * @upstream_running_time: The upstream-centric running-time when the
 *    rate change should be applied.
 *
 * Create a new instant-rate-sync-time event. This event is sent by the
 * pipeline to notify elements handling the instant-rate-change event about
 * the running-time when the new rate should be applied. The running time
 * may be in the past when elements handle this event, which can lead to
 * switching artifacts. The magnitude of those depends on the exact timing
 * of event delivery to each element and the magnitude of the change in
 * playback rate being applied.
 *
 * The @running_time and @upstream_running_time are the same if this
 * is the first instant-rate adjustment, but will differ for later ones
 * to compensate for the accumulated offset due to playing at a rate
 * different to the one indicated in the playback segments.
 *
 * Returns: (transfer full): the new instant-rate-sync-time event.
 *
 * Since: 1.18
 */
GstEvent *
gst_event_new_instant_rate_sync_time (gdouble rate_multiplier,
    GstClockTime running_time, GstClockTime upstream_running_time)
{
  GstEvent *event;

  g_return_val_if_fail (rate_multiplier != 0.0, NULL);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (running_time), NULL);
  g_return_val_if_fail (GST_CLOCK_TIME_IS_VALID (upstream_running_time), NULL);

  GST_CAT_TRACE (GST_CAT_EVENT,
      "creating instant-rate-sync-time event %lf %" GST_TIME_FORMAT
      " %" GST_TIME_FORMAT, rate_multiplier,
      GST_TIME_ARGS (running_time), GST_TIME_ARGS (upstream_running_time));

  event = gst_event_new_custom (GST_EVENT_INSTANT_RATE_SYNC_TIME,
      gst_structure_new_id (GST_QUARK (EVENT_INSTANT_RATE_SYNC_TIME),
          GST_QUARK (RATE), G_TYPE_DOUBLE, rate_multiplier,
          GST_QUARK (RUNNING_TIME), GST_TYPE_CLOCK_TIME, running_time,
          GST_QUARK (UPSTREAM_RUNNING_TIME), GST_TYPE_CLOCK_TIME,
          upstream_running_time, NULL));

  return event;
}

/**
 * gst_event_parse_instant_rate_sync_time:
 * @event: a #GstEvent of type #GST_EVENT_INSTANT_RATE_CHANGE
 * @rate_multiplier: (out) (allow-none): location where to store the rate of
 *     the instant-rate-sync-time event, or %NULL
 * @running_time: (out) (allow-none): location in which to store the running time
 *     of the instant-rate-sync-time event, or %NULL
 * @upstream_running_time: (out) (allow-none): location in which to store the
 *     upstream running time of the instant-rate-sync-time event, or %NULL
 *
 * Extract the rate multiplier and running times from an instant-rate-sync-time event.
 *
 * Since: 1.18
 */
void
gst_event_parse_instant_rate_sync_time (GstEvent * event,
    gdouble * rate_multiplier, GstClockTime * running_time,
    GstClockTime * upstream_running_time)
{
  GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_INSTANT_RATE_SYNC_TIME);

  structure = GST_EVENT_STRUCTURE (event);
  gst_structure_id_get (structure, GST_QUARK (RATE), G_TYPE_DOUBLE,
      rate_multiplier, GST_QUARK (RUNNING_TIME), GST_TYPE_CLOCK_TIME,
      running_time, GST_QUARK (UPSTREAM_RUNNING_TIME), GST_TYPE_CLOCK_TIME,
      upstream_running_time, NULL);
}

/**
 * gst_event_replace: (skip)
 * @old_event: (inout) (transfer full) (nullable): pointer to a
 *     pointer to a #GstEvent to be replaced.
 * @new_event: (allow-none) (transfer none): pointer to a #GstEvent that will
 *     replace the event pointed to by @old_event.
 *
 * Modifies a pointer to a #GstEvent to point to a different #GstEvent. The
 * modification is done atomically (so this is useful for ensuring thread safety
 * in some cases), and the reference counts are updated appropriately (the old
 * event is unreffed, the new one is reffed).
 *
 * Either @new_event or the #GstEvent pointed to by @old_event may be %NULL.
 *
 * Returns: %TRUE if @new_event was different from @old_event
 */
gboolean
gst_event_replace (GstEvent ** old_event, GstEvent * new_event)
{
  return gst_mini_object_replace ((GstMiniObject **) old_event,
      (GstMiniObject *) new_event);
}

/**
 * gst_event_steal: (skip)
 * @old_event: (inout) (transfer full) (nullable): pointer to a
 *     pointer to a #GstEvent to be stolen.
 *
 * Atomically replace the #GstEvent pointed to by @old_event with %NULL and
 * return the original event.
 *
 * Returns: the #GstEvent that was in @old_event
 */
GstEvent *
gst_event_steal (GstEvent ** old_event)
{
  return GST_EVENT_CAST (gst_mini_object_steal ((GstMiniObject **) old_event));
}

/**
 * gst_event_take: (skip)
 * @old_event: (inout) (transfer full) (nullable): pointer to a
 *     pointer to a #GstEvent to be stolen.
 * @new_event: (allow-none) (transfer full): pointer to a #GstEvent that will
 *     replace the event pointed to by @old_event.
 *
 * Modifies a pointer to a #GstEvent to point to a different #GstEvent. This
 * function is similar to gst_event_replace() except that it takes ownership of
 * @new_event.
 *
 * Either @new_event or the #GstEvent pointed to by @old_event may be %NULL.
 *
 * Returns: %TRUE if @new_event was different from @old_event
 */
gboolean
gst_event_take (GstEvent ** old_event, GstEvent * new_event)
{
  return gst_mini_object_take ((GstMiniObject **) old_event,
      (GstMiniObject *) new_event);
}

/**
 * gst_event_ref: (skip)
 * @event: The event to refcount
 *
 * Increase the refcount of this event.
 *
 * Returns: (transfer full): @event (for convenience when doing assignments)
 */
GstEvent *
gst_event_ref (GstEvent * event)
{
  return (GstEvent *) gst_mini_object_ref (GST_MINI_OBJECT_CAST (event));
}

/**
 * gst_event_unref: (skip)
 * @event: (transfer full): the event to refcount
 *
 * Decrease the refcount of an event, freeing it if the refcount reaches 0.
 */
void
gst_event_unref (GstEvent * event)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (event));
}

/**
 * gst_clear_event: (skip)
 * @event_ptr: a pointer to a #GstEvent reference
 *
 * Clears a reference to a #GstEvent.
 *
 * @event_ptr must not be %NULL.
 *
 * If the reference is %NULL then this function does nothing. Otherwise, the
 * reference count of the event is decreased and the pointer is set to %NULL.
 *
 * Since: 1.16
 */
void
gst_clear_event (GstEvent ** event_ptr)
{
  gst_clear_mini_object ((GstMiniObject **) event_ptr);
}

/**
 * gst_event_copy: (skip)
 * @event: The event to copy
 *
 * Copy the event using the event specific copy function.
 *
 * Returns: (transfer full): the new event
 */
GstEvent *
gst_event_copy (const GstEvent * event)
{
  return
      GST_EVENT_CAST (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST
          (event)));
}
