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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gstevent
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
 * the needed parameters to specity seeking time and mode.
 * <example>
 * <title>performing a seek on a pipeline</title>
 *   <programlisting>
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
 *   </programlisting>
 * </example>
 *
 * Last reviewed on 2006-09-6 (0.10.10)
 */


#include "gst_private.h"
#include <string.h>             /* memcpy */

#include "gstinfo.h"
#include "gstevent.h"
#include "gstenumtypes.h"
#include "gstutils.h"
#include "gstquark.h"

#define GST_EVENT_SEQNUM(e) ((GstEvent*)e)->abidata.seqnum

static void gst_event_finalize (GstEvent * event);
static GstEvent *_gst_event_copy (GstEvent * event);

static GstMiniObjectClass *parent_class = NULL;

void
_gst_event_initialize (void)
{
  g_type_class_ref (gst_event_get_type ());
  g_type_class_ref (gst_seek_flags_get_type ());
  g_type_class_ref (gst_seek_type_get_type ());
}

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
  {GST_EVENT_EOS, "eos", 0},
  {GST_EVENT_NEWSEGMENT, "newsegment", 0},
  {GST_EVENT_TAG, "tag", 0},
  {GST_EVENT_BUFFERSIZE, "buffersize", 0},
  {GST_EVENT_SINK_MESSAGE, "sink-message", 0},
  {GST_EVENT_QOS, "qos", 0},
  {GST_EVENT_SEEK, "seek", 0},
  {GST_EVENT_NAVIGATION, "navigation", 0},
  {GST_EVENT_LATENCY, "latency", 0},
  {GST_EVENT_STEP, "step", 0},
  {GST_EVENT_CUSTOM_UPSTREAM, "custom-upstream", 0},
  {GST_EVENT_CUSTOM_DOWNSTREAM, "custom-downstream", 0},
  {GST_EVENT_CUSTOM_DOWNSTREAM_OOB, "custom-downstream-oob", 0},
  {GST_EVENT_CUSTOM_BOTH, "custom-both", 0},
  {GST_EVENT_CUSTOM_BOTH_OOB, "custom-both-oob", 0},

  {0, NULL, 0}
};

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

  ret = type & ((1 << GST_EVENT_TYPE_SHIFT) - 1);

  return ret;
}

#define _do_init \
{ \
  gint i; \
  \
  for (i = 0; event_quarks[i].name; i++) { \
    event_quarks[i].quark = g_quark_from_static_string (event_quarks[i].name); \
  } \
}

G_DEFINE_TYPE_WITH_CODE (GstEvent, gst_event, GST_TYPE_MINI_OBJECT, _do_init);

static void
gst_event_class_init (GstEventClass * klass)
{
  parent_class = g_type_class_peek_parent (klass);

  klass->mini_object_class.copy = (GstMiniObjectCopyFunction) _gst_event_copy;
  klass->mini_object_class.finalize =
      (GstMiniObjectFinalizeFunction) gst_event_finalize;
}

static void
gst_event_init (GstEvent * event)
{
  GST_EVENT_TIMESTAMP (event) = GST_CLOCK_TIME_NONE;
}

static void
gst_event_finalize (GstEvent * event)
{
  g_return_if_fail (event != NULL);
  g_return_if_fail (GST_IS_EVENT (event));

  GST_CAT_LOG (GST_CAT_EVENT, "freeing event %p type %s", event,
      GST_EVENT_TYPE_NAME (event));

  if (GST_EVENT_SRC (event)) {
    gst_object_unref (GST_EVENT_SRC (event));
    GST_EVENT_SRC (event) = NULL;
  }
  if (event->structure) {
    gst_structure_set_parent_refcount (event->structure, NULL);
    gst_structure_free (event->structure);
  }

/*   GST_MINI_OBJECT_CLASS (parent_class)->finalize (GST_MINI_OBJECT (event)); */
}

static GstEvent *
_gst_event_copy (GstEvent * event)
{
  GstEvent *copy;

  copy = (GstEvent *) gst_mini_object_new (GST_TYPE_EVENT);

  GST_EVENT_TYPE (copy) = GST_EVENT_TYPE (event);
  GST_EVENT_TIMESTAMP (copy) = GST_EVENT_TIMESTAMP (event);
  GST_EVENT_SEQNUM (copy) = GST_EVENT_SEQNUM (event);

  if (GST_EVENT_SRC (event)) {
    GST_EVENT_SRC (copy) = gst_object_ref (GST_EVENT_SRC (event));
  }
  if (event->structure) {
    copy->structure = gst_structure_copy (event->structure);
    gst_structure_set_parent_refcount (copy->structure,
        &copy->mini_object.refcount);
  }
  return copy;
}

static GstEvent *
gst_event_new (GstEventType type)
{
  GstEvent *event;

  event = (GstEvent *) gst_mini_object_new (GST_TYPE_EVENT);

  GST_CAT_DEBUG (GST_CAT_EVENT, "creating new event %p %s %d", event,
      gst_event_type_get_name (type), type);

  event->type = type;
  event->src = NULL;
  event->structure = NULL;
  GST_EVENT_SEQNUM (event) = gst_util_seqnum_next ();

  return event;
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
 * Returns: (transfer full): the new custom event.
 */
GstEvent *
gst_event_new_custom (GstEventType type, GstStructure * structure)
{
  GstEvent *event;

  /* structure must not have a parent */
  if (structure)
    g_return_val_if_fail (structure->parent_refcount == NULL, NULL);

  event = gst_event_new (type);
  if (structure) {
    gst_structure_set_parent_refcount (structure, &event->mini_object.refcount);
    event->structure = structure;
  }
  return event;
}

/**
 * gst_event_get_structure:
 * @event: The #GstEvent.
 *
 * Access the structure of the event.
 *
 * Returns: The structure of the event. The structure is still
 * owned by the event, which means that you should not free it and
 * that the pointer becomes invalid when you free the event.
 *
 * MT safe.
 */
const GstStructure *
gst_event_get_structure (GstEvent * event)
{
  g_return_val_if_fail (GST_IS_EVENT (event), NULL);

  return event->structure;
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
 *
 * Since: 0.10.20
 */
gboolean
gst_event_has_name (GstEvent * event, const gchar * name)
{
  g_return_val_if_fail (GST_IS_EVENT (event), FALSE);

  if (event->structure == NULL)
    return FALSE;

  return gst_structure_has_name (event->structure, name);
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
 * two events or messages will never not have the same sequence number unless
 * that correspondence was made explicitly.
 *
 * Returns: The event's sequence number.
 *
 * MT safe.
 *
 * Since: 0.10.22
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
 *
 * Since: 0.10.22
 */
void
gst_event_set_seqnum (GstEvent * event, guint32 seqnum)
{
  g_return_if_fail (GST_IS_EVENT (event));

  GST_EVENT_SEQNUM (event) = seqnum;
}

/* FIXME 0.11: It would be nice to have flush events
 * that don't reset the running time in the sinks
 */

/**
 * gst_event_new_flush_start:
 *
 * Allocate a new flush start event. The flush start event can be sent
 * upstream and downstream and travels out-of-bounds with the dataflow.
 *
 * It marks pads as being flushing and will make them return
 * #GST_FLOW_WRONG_STATE when used for data flow with gst_pad_push(),
 * gst_pad_chain(), gst_pad_alloc_buffer(), gst_pad_get_range() and
 * gst_pad_pull_range(). Any event (except a #GST_EVENT_FLUSH_STOP) received
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
  return gst_event_new (GST_EVENT_FLUSH_START);
}

/**
 * gst_event_new_flush_stop:
 *
 * Allocate a new flush stop event. The flush stop event can be sent
 * upstream and downstream and travels serialized with the dataflow.
 * It is typically sent after sending a FLUSH_START event to make the
 * pads accept data again.
 *
 * Elements can process this event synchronized with the dataflow since
 * the preceeding FLUSH_START event stopped the dataflow.
 *
 * This event is typically generated to complete a seek and to resume
 * dataflow.
 *
 * Returns: (transfer full): a new flush stop event.
 */
GstEvent *
gst_event_new_flush_stop (void)
{
  return gst_event_new (GST_EVENT_FLUSH_STOP);
}

/**
 * gst_event_new_eos:
 *
 * Create a new EOS event. The eos event can only travel downstream
 * synchronized with the buffer flow. Elements that receive the EOS
 * event on a pad can return #GST_FLOW_UNEXPECTED as a #GstFlowReturn
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
  return gst_event_new (GST_EVENT_EOS);
}

/**
 * gst_event_new_new_segment:
 * @update: is this segment an update to a previous one
 * @rate: a new rate for playback
 * @format: The format of the segment values
 * @start: the start value of the segment
 * @stop: the stop value of the segment
 * @position: stream position
 *
 * Allocate a new newsegment event with the given format/values tripplets
 *
 * This method calls gst_event_new_new_segment_full() passing a default
 * value of 1.0 for applied_rate
 *
 * Returns: (transfer full): a new newsegment event.
 */
GstEvent *
gst_event_new_new_segment (gboolean update, gdouble rate, GstFormat format,
    gint64 start, gint64 stop, gint64 position)
{
  return gst_event_new_new_segment_full (update, rate, 1.0, format, start,
      stop, position);
}

/**
 * gst_event_parse_new_segment:
 * @event: The event to query
 * @update: (out): A pointer to the update flag of the segment
 * @rate: (out): A pointer to the rate of the segment
 * @format: (out): A pointer to the format of the newsegment values
 * @start: (out): A pointer to store the start value in
 * @stop: (out): A pointer to store the stop value in
 * @position: (out): A pointer to store the stream time in
 *
 * Get the update flag, rate, format, start, stop and position in the 
 * newsegment event. In general, gst_event_parse_new_segment_full() should
 * be used instead of this, to also retrieve the applied_rate value of the
 * segment. See gst_event_new_new_segment_full() for a full description 
 * of the newsegment event.
 */
void
gst_event_parse_new_segment (GstEvent * event, gboolean * update,
    gdouble * rate, GstFormat * format, gint64 * start,
    gint64 * stop, gint64 * position)
{
  gst_event_parse_new_segment_full (event, update, rate, NULL, format, start,
      stop, position);
}

/**
 * gst_event_new_new_segment_full:
 * @update: Whether this segment is an update to a previous one
 * @rate: A new rate for playback
 * @applied_rate: The rate factor which has already been applied
 * @format: The format of the segment values
 * @start: The start value of the segment
 * @stop: The stop value of the segment
 * @position: stream position
 *
 * Allocate a new newsegment event with the given format/values triplets.
 *
 * The newsegment event marks the range of buffers to be processed. All
 * data not within the segment range is not to be processed. This can be
 * used intelligently by plugins to apply more efficient methods of skipping
 * unneeded data. The valid range is expressed with the @start and @stop
 * values.
 *
 * The position value of the segment is used in conjunction with the start
 * value to convert the buffer timestamps into the stream time. This is 
 * usually done in sinks to report the current stream_time. 
 * @position represents the stream_time of a buffer carrying a timestamp of 
 * @start. @position cannot be -1.
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
 * incoming timestamps and buffer content by half and output a newsegment event 
 * with @rate of 1.0 and @applied_rate of 2.0
 *
 * After a newsegment event, the buffer stream time is calculated with:
 *
 *   position + (TIMESTAMP(buf) - start) * ABS (rate * applied_rate)
 *
 * Returns: (transfer full): a new newsegment event.
 *
 * Since: 0.10.6
 */
GstEvent *
gst_event_new_new_segment_full (gboolean update, gdouble rate,
    gdouble applied_rate, GstFormat format, gint64 start, gint64 stop,
    gint64 position)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (rate != 0.0, NULL);
  g_return_val_if_fail (applied_rate != 0.0, NULL);

  if (format == GST_FORMAT_TIME) {
    GST_CAT_INFO (GST_CAT_EVENT,
        "creating newsegment update %d, rate %lf, format GST_FORMAT_TIME, "
        "start %" GST_TIME_FORMAT ", stop %" GST_TIME_FORMAT
        ", position %" GST_TIME_FORMAT,
        update, rate, GST_TIME_ARGS (start),
        GST_TIME_ARGS (stop), GST_TIME_ARGS (position));
  } else {
    GST_CAT_INFO (GST_CAT_EVENT,
        "creating newsegment update %d, rate %lf, format %s, "
        "start %" G_GINT64_FORMAT ", stop %" G_GINT64_FORMAT ", position %"
        G_GINT64_FORMAT, update, rate, gst_format_get_name (format), start,
        stop, position);
  }

  g_return_val_if_fail (position != -1, NULL);
  g_return_val_if_fail (start != -1, NULL);
  if (stop != -1)
    g_return_val_if_fail (start <= stop, NULL);

  structure = gst_structure_id_new (GST_QUARK (EVENT_NEWSEGMENT),
      GST_QUARK (UPDATE), G_TYPE_BOOLEAN, update,
      GST_QUARK (RATE), G_TYPE_DOUBLE, rate,
      GST_QUARK (APPLIED_RATE), G_TYPE_DOUBLE, applied_rate,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (START), G_TYPE_INT64, start,
      GST_QUARK (STOP), G_TYPE_INT64, stop,
      GST_QUARK (POSITION), G_TYPE_INT64, position, NULL);
  event = gst_event_new_custom (GST_EVENT_NEWSEGMENT, structure);

  return event;
}

/**
 * gst_event_parse_new_segment_full:
 * @event: The event to query
 * @update: (out): A pointer to the update flag of the segment
 * @rate: (out): A pointer to the rate of the segment
 * @applied_rate: (out): A pointer to the applied_rate of the segment
 * @format: (out): A pointer to the format of the newsegment values
 * @start: (out): A pointer to store the start value in
 * @stop: (out): A pointer to store the stop value in
 * @position: (out): A pointer to store the stream time in
 *
 * Get the update, rate, applied_rate, format, start, stop and 
 * position in the newsegment event. See gst_event_new_new_segment_full() 
 * for a full description of the newsegment event.
 *
 * Since: 0.10.6
 */
void
gst_event_parse_new_segment_full (GstEvent * event, gboolean * update,
    gdouble * rate, gdouble * applied_rate, GstFormat * format,
    gint64 * start, gint64 * stop, gint64 * position)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_NEWSEGMENT);

  structure = event->structure;
  if (G_LIKELY (update))
    *update =
        g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (UPDATE)));
  if (G_LIKELY (rate))
    *rate =
        g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (RATE)));
  if (G_LIKELY (applied_rate))
    *applied_rate =
        g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (APPLIED_RATE)));
  if (G_LIKELY (format))
    *format =
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (G_LIKELY (start))
    *start =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (START)));
  if (G_LIKELY (stop))
    *stop =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (STOP)));
  if (G_LIKELY (position))
    *position =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (POSITION)));
}

/**
 * gst_event_new_tag:
 * @taglist: (transfer full): metadata list. The event will take ownership
 *     of the taglist.
 *
 * Generates a metadata tag event from the given @taglist.
 *
 * Returns: (transfer full): a new #GstEvent
 */
GstEvent *
gst_event_new_tag (GstTagList * taglist)
{
  g_return_val_if_fail (taglist != NULL, NULL);

  return gst_event_new_custom (GST_EVENT_TAG, (GstStructure *) taglist);
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
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_TAG);

  if (taglist)
    *taglist = (GstTagList *) event->structure;
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
 * When the @async flag is set, a thread boundary is prefered.
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

  structure = gst_structure_id_new (GST_QUARK (EVENT_BUFFER_SIZE),
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

  structure = event->structure;
  if (format)
    *format =
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
 * @proportion: the proportion of the qos message
 * @diff: The time difference of the last Clock sync
 * @timestamp: The timestamp of the buffer
 *
 * Allocate a new qos event with the given values. This function calls
 * gst_event_new_qos_full() with the type set to #GST_QOS_TYPE_OVERFLOW
 * when diff is negative (buffers are in time) and #GST_QOS_TYPE_UNDERFLOW
 * when @diff is positive (buffers are late).
 *
 * Returns: (transfer full): a new QOS event.
 */
GstEvent *
gst_event_new_qos (gdouble proportion, GstClockTimeDiff diff,
    GstClockTime timestamp)
{
  GstQOSType type;

  if (diff <= 0)
    type = GST_QOS_TYPE_OVERFLOW;
  else
    type = GST_QOS_TYPE_UNDERFLOW;

  return gst_event_new_qos_full (type, proportion, diff, timestamp);
}

/**
 * gst_event_new_qos_full:
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
 * whether to process more buffers. For possitive @diff, all buffers with
 * timestamp <= @timestamp + @diff will certainly arrive late in the sink
 * as well. A (negative) @diff value so that @timestamp + @diff would yield a
 * result smaller than 0 is not allowed.
 *
 * The application can use general event probes to intercept the QoS
 * event and implement custom application specific QoS handling.
 *
 * Returns: (transfer full): a new QOS event.
 *
 * Since: 0.10.33
 */
GstEvent *
gst_event_new_qos_full (GstQOSType type, gdouble proportion,
    GstClockTimeDiff diff, GstClockTime timestamp)
{
  GstEvent *event;
  GstStructure *structure;

  /* diff must be positive or timestamp + diff must be positive */
  g_return_val_if_fail (diff >= 0 || -diff <= timestamp, NULL);

  GST_CAT_INFO (GST_CAT_EVENT,
      "creating qos type %d, proportion %lf, diff %" G_GINT64_FORMAT
      ", timestamp %" GST_TIME_FORMAT, type, proportion,
      diff, GST_TIME_ARGS (timestamp));

  structure = gst_structure_id_new (GST_QUARK (EVENT_QOS),
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
 * @proportion: (out): A pointer to store the proportion in
 * @diff: (out): A pointer to store the diff in
 * @timestamp: (out): A pointer to store the timestamp in
 *
 * Get the proportion, diff and timestamp in the qos event. See
 * gst_event_new_qos() for more information about the different QoS values.
 */
void
gst_event_parse_qos (GstEvent * event, gdouble * proportion,
    GstClockTimeDiff * diff, GstClockTime * timestamp)
{
  gst_event_parse_qos_full (event, NULL, proportion, diff, timestamp);
}

/**
 * gst_event_parse_qos_full:
 * @event: The event to query
 * @type: (out): A pointer to store the QoS type in
 * @proportion: (out): A pointer to store the proportion in
 * @diff: (out): A pointer to store the diff in
 * @timestamp: (out): A pointer to store the timestamp in
 *
 * Get the type, proportion, diff and timestamp in the qos event. See
 * gst_event_new_qos_full() for more information about the different QoS values.
 *
 * Since: 0.10.33
 */
void
gst_event_parse_qos_full (GstEvent * event, GstQOSType * type,
    gdouble * proportion, GstClockTimeDiff * diff, GstClockTime * timestamp)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_QOS);

  structure = event->structure;
  if (type)
    *type =
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
  if (timestamp)
    *timestamp =
        g_value_get_uint64 (gst_structure_id_get_value (structure,
            GST_QUARK (TIMESTAMP)));
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
 * position (if any). If the stop position if updated, it must be different from
 * -1 for negative rates.
 *
 * It is not possible to seek relative to the current playback position, to do
 * this, PAUSE the pipeline, query the current playback position with
 * #GST_QUERY_POSITION and update the playback segment current position with a
 * #GST_SEEK_TYPE_SET to the desired position. 
 *
 * Returns: (transfer full): a new seek event.
 */
GstEvent *
gst_event_new_seek (gdouble rate, GstFormat format, GstSeekFlags flags,
    GstSeekType start_type, gint64 start, GstSeekType stop_type, gint64 stop)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (rate != 0.0, NULL);

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

  structure = gst_structure_id_new (GST_QUARK (EVENT_SEEK),
      GST_QUARK (RATE), G_TYPE_DOUBLE, rate,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (FLAGS), GST_TYPE_SEEK_FLAGS, flags,
      GST_QUARK (CUR_TYPE), GST_TYPE_SEEK_TYPE, start_type,
      GST_QUARK (CUR), G_TYPE_INT64, start,
      GST_QUARK (STOP_TYPE), GST_TYPE_SEEK_TYPE, stop_type,
      GST_QUARK (STOP), G_TYPE_INT64, stop, NULL);
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
 * @start: (out): result location for the start postion expressed in @format
 * @stop_type:  (out): result location for the #GstSeekType of the stop position
 * @stop: (out): result location for the stop postion expressed in @format
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

  structure = event->structure;
  if (rate)
    *rate =
        g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (RATE)));
  if (format)
    *format =
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (flags)
    *flags =
        g_value_get_flags (gst_structure_id_get_value (structure,
            GST_QUARK (FLAGS)));
  if (start_type)
    *start_type =
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (CUR_TYPE)));
  if (start)
    *start =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (CUR)));
  if (stop_type)
    *stop_type =
        g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (STOP_TYPE)));
  if (stop)
    *stop =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (STOP)));
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
 *
 * Since: 0.10.12
 */
GstEvent *
gst_event_new_latency (GstClockTime latency)
{
  GstEvent *event;
  GstStructure *structure;

  GST_CAT_INFO (GST_CAT_EVENT,
      "creating latency event %" GST_TIME_FORMAT, GST_TIME_ARGS (latency));

  structure = gst_structure_id_new (GST_QUARK (EVENT_LATENCY),
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
 *
 * Since: 0.10.12
 */
void
gst_event_parse_latency (GstEvent * event, GstClockTime * latency)
{
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_LATENCY);

  if (latency)
    *latency =
        g_value_get_uint64 (gst_structure_id_get_value (event->structure,
            GST_QUARK (LATENCY)));
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
 * A rate of <= 0.0 is not allowed, pause the pipeline or reverse the playback
 * direction of the pipeline to get the same effect.
 *
 * The @flush flag will clear any pending data in the pipeline before starting
 * the step operation.
 *
 * The @intermediate flag instructs the pipeline that this step operation is
 * part of a larger step operation.
 *
 * Returns: (transfer full): a new #GstEvent
 *
 * Since: 0.10.24
 */
GstEvent *
gst_event_new_step (GstFormat format, guint64 amount, gdouble rate,
    gboolean flush, gboolean intermediate)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (rate > 0.0, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating step event");

  structure = gst_structure_id_new (GST_QUARK (EVENT_STEP),
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
 *
 * Since: 0.10.24
 */
void
gst_event_parse_step (GstEvent * event, GstFormat * format, guint64 * amount,
    gdouble * rate, gboolean * flush, gboolean * intermediate)
{
  const GstStructure *structure;

  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_STEP);

  structure = event->structure;
  if (format)
    *format = g_value_get_enum (gst_structure_id_get_value (structure,
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
 * gst_event_new_sink_message:
 * @msg: (transfer none): the #GstMessage to be posted
 *
 * Create a new sink-message event. The purpose of the sink-message event is
 * to instruct a sink to post the message contained in the event synchronized
 * with the stream.
 *
 * Returns: (transfer full): a new #GstEvent
 *
 * Since: 0.10.26
 */
/* FIXME 0.11: take ownership of msg for consistency? */
GstEvent *
gst_event_new_sink_message (GstMessage * msg)
{
  GstEvent *event;
  GstStructure *structure;

  g_return_val_if_fail (msg != NULL, NULL);

  GST_CAT_INFO (GST_CAT_EVENT, "creating sink-message event");

  structure = gst_structure_id_new (GST_QUARK (EVENT_SINK_MESSAGE),
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
 *
 * Since: 0.10.26
 */
void
gst_event_parse_sink_message (GstEvent * event, GstMessage ** msg)
{
  g_return_if_fail (GST_IS_EVENT (event));
  g_return_if_fail (GST_EVENT_TYPE (event) == GST_EVENT_SINK_MESSAGE);

  if (msg)
    *msg =
        GST_MESSAGE (gst_value_dup_mini_object (gst_structure_id_get_value
            (event->structure, GST_QUARK (MESSAGE))));
}
