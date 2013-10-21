/* GStreamer
 * Copyright (C) 2008 Nokia Corporation. All rights reserved.
 *
 * Contact: Stefan Kost <stefan.kost@nokia.com>
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

#ifndef __GST_BASE_PARSE_H__
#define __GST_BASE_PARSE_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define GST_TYPE_BASE_PARSE            (gst_base_parse_get_type())
#define GST_BASE_PARSE(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_BASE_PARSE,GstBaseParse))
#define GST_BASE_PARSE_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_BASE_PARSE,GstBaseParseClass))
#define GST_BASE_PARSE_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj),GST_TYPE_BASE_PARSE,GstBaseParseClass))
#define GST_IS_BASE_PARSE(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_BASE_PARSE))
#define GST_IS_BASE_PARSE_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_BASE_PARSE))
#define GST_BASE_PARSE_CAST(obj)       ((GstBaseParse *)(obj))

/**
 * GST_BASE_PARSE_SRC_PAD:
 * @obj: base parse instance
 *
 * Gives the pointer to the source #GstPad object of the element.
 *
 * Since: 0.10.33
 */
#define GST_BASE_PARSE_SRC_PAD(obj)    (GST_BASE_PARSE_CAST (obj)->srcpad)

/**
 * GST_BASE_PARSE_SINK_PAD:
 * @obj: base parse instance
 *
 * Gives the pointer to the sink #GstPad object of the element.
 *
 * Since: 0.10.33
 */
#define GST_BASE_PARSE_SINK_PAD(obj)    (GST_BASE_PARSE_CAST (obj)->sinkpad)

/**
 * GST_BASE_PARSE_FLOW_DROPPED:
 *
 * A #GstFlowReturn that can be returned from parse_frame to
 * indicate that no output buffer was generated, or from pre_push_buffer to
 * to forego pushing buffer.
 *
 * Since: 0.10.33
 */
#define GST_BASE_PARSE_FLOW_DROPPED     GST_FLOW_CUSTOM_SUCCESS

/**
 * GST_BASE_PARSE_FLOW_QUEUED:
 *
 * A #GstFlowReturn that can be returned from parse frame to indicate that
 * the buffer will be queued to be pushed when the next OK
 *
 * Since: 0.10.33
 */
#define GST_BASE_PARSE_FLOW_QUEUED      GST_FLOW_CUSTOM_SUCCESS_1

/* not public API, use accessor macros below */
#define GST_BASE_PARSE_FLAG_LOST_SYNC (1 << 0)
#define GST_BASE_PARSE_FLAG_DRAINING  (1 << 1)

/**
 * GST_BASE_PARSE_LOST_SYNC:
 * @parse: base parse instance
 *
 * Obtains current sync status.
 *
 * Since: 0.10.33
 */
#define GST_BASE_PARSE_LOST_SYNC(parse) (!!(GST_BASE_PARSE_CAST(parse)->flags & GST_BASE_PARSE_FLAG_LOST_SYNC))

/**
 * GST_BASE_PARSE_DRAINING:
 * @parse: base parse instance
 *
 * Obtains current drain status (ie. whether EOS has been received and
 * the parser is now processing the frames at the end of the stream)
 *
 * Since: 0.10.33
 */
#define GST_BASE_PARSE_DRAINING(parse)  (!!(GST_BASE_PARSE_CAST(parse)->flags & GST_BASE_PARSE_FLAG_DRAINING))

/**
 * GstBaseParseFrameFlags:
 * @GST_BASE_PARSE_FRAME_FLAG_NONE: no flag
 * @GST_BASE_PARSE_FRAME_FLAG_NO_FRAME: set to indicate this buffer should not be
 *   counted as frame, e.g. if this frame is dependent on a previous one.
 *   As it is not counted as a frame, bitrate increases but frame to time
 *   conversions are maintained.
 * @GST_BASE_PARSE_FRAME_FLAG_CLIP: @pre_push_buffer can set this to indicate
 *    that regular segment clipping can still be performed (as opposed to
 *    any custom one having been done).
 *
 * Flags to be used in a #GstBaseParseFrame.
 *
 * Since: 0.10.33
 */
typedef enum {
  GST_BASE_PARSE_FRAME_FLAG_NONE         = 0,
  GST_BASE_PARSE_FRAME_FLAG_NO_FRAME     = (1 << 0),
  GST_BASE_PARSE_FRAME_FLAG_CLIP         = (1 << 1)
} GstBaseParseFrameFlags;

/**
 * GstBaseParseFrame:
 * @buffer: data to check for valid frame or parsed frame.
 *   Subclass is allowed to replace this buffer.
 * @overhead: subclass can set this to indicates the metadata overhead
 *   for the given frame, which is then used to enable more accurate bitrate
 *   computations. If this is -1, it is assumed that this frame should be
 *   skipped in bitrate calculation.
 * @flags: a combination of input and output #GstBaseParseFrameFlags that
 *  convey additional context to subclass or allow subclass to tune
 *  subsequent #GstBaseParse actions.
 *
 * Frame (context) data passed to each frame parsing virtual methods.  In
 * addition to providing the data to be checked for a valid frame or an already
 * identified frame, it conveys additional metadata or control information
 * from and to the subclass w.r.t. the particular frame in question (rather
 * than global parameters).  Some of these may apply to each parsing stage, others
 * only to some a particular one.  These parameters are effectively zeroed at start
 * of each frame's processing, i.e. parsing virtual method invocation sequence.
 *
 * Since: 0.10.33
 */
typedef struct {
  GstBuffer * buffer;
  guint       flags;
  gint        overhead;
  /*< private >*/
  guint       _gst_reserved_i[2];
  gpointer    _gst_reserved_p[2];
  guint       _private_flags;
} GstBaseParseFrame;

typedef struct _GstBaseParse GstBaseParse;
typedef struct _GstBaseParseClass GstBaseParseClass;
typedef struct _GstBaseParsePrivate GstBaseParsePrivate;

/**
 * GstBaseParse:
 * @element: the parent element.
 *
 * The opaque #GstBaseParse data structure.
 */
struct _GstBaseParse {
  GstElement     element;

  /*< protected >*/
  /* source and sink pads */
  GstPad         *sinkpad;
  GstPad         *srcpad;

  guint           flags;

  /* MT-protected (with STREAM_LOCK) */
  GstSegment      segment;

  /*< private >*/
  gpointer       _gst_reserved[GST_PADDING_LARGE];
  GstBaseParsePrivate *priv;
};

/**
 * GstBaseParseClass:
 * @parent_class: the parent class
 * @start:          Optional.
 *                  Called when the element starts processing.
 *                  Allows opening external resources.
 * @stop:           Optional.
 *                  Called when the element stops processing.
 *                  Allows closing external resources.
 * @set_sink_caps:  allows the subclass to be notified of the actual caps set.
 * @check_valid_frame:  Check if the given piece of data contains a valid
 *                      frame.
 * @parse_frame:    Parse the already checked frame. Subclass need to
 *                  set the buffer timestamp, duration, caps and possibly
 *                  other necessary metadata. This is called with srcpad's
 *                  STREAM_LOCK held.
 * @convert:        Optional.
 *                  Convert between formats.
 * @event:          Optional.
 *                  Event handler on the sink pad. This function should return
 *                  TRUE if the event was handled and can be dropped.
 * @src_event:      Optional.
 *                  Event handler on the source pad. Should return TRUE
 *                  if the event was handled and can be dropped.
 * @pre_push_frame: Optional.
 *                   Called just prior to pushing a frame (after any pending
 *                   events have been sent) to give subclass a chance to perform
 *                   additional actions at this time (e.g. tag sending) or to
 *                   decide whether this buffer should be dropped or not
 *                   (e.g. custom segment clipping).
 *
 * Subclasses can override any of the available virtual methods or not, as
 * needed. At minimum @check_valid_frame and @parse_frame needs to be
 * overridden.
 */
struct _GstBaseParseClass {
  GstElementClass parent_class;

  /*< public >*/
  /* virtual methods for subclasses */

  gboolean      (*start)              (GstBaseParse * parse);

  gboolean      (*stop)               (GstBaseParse * parse);

  gboolean      (*set_sink_caps)      (GstBaseParse * parse,
                                       GstCaps      * caps);

  gboolean      (*check_valid_frame)  (GstBaseParse      * parse,
                                       GstBaseParseFrame * frame,
                                       guint             * framesize,
                                       gint              * skipsize);

  GstFlowReturn (*parse_frame)        (GstBaseParse      * parse,
                                       GstBaseParseFrame * frame);

  GstFlowReturn (*pre_push_frame)     (GstBaseParse      * parse,
                                       GstBaseParseFrame * frame);

  gboolean      (*convert)            (GstBaseParse * parse,
                                       GstFormat      src_format,
                                       gint64         src_value,
                                       GstFormat      dest_format,
                                       gint64       * dest_value);

  gboolean      (*event)              (GstBaseParse * parse,
                                       GstEvent     * event);

  gboolean      (*src_event)          (GstBaseParse * parse,
                                       GstEvent     * event);

  /*< private >*/
  gpointer       _gst_reserved[GST_PADDING_LARGE];
};

GType           gst_base_parse_get_type (void);

GType           gst_base_parse_frame_get_type (void);

GstBaseParseFrame * gst_base_parse_frame_new  (GstBuffer              * buffer,
                                               GstBaseParseFrameFlags   flags,
                                               gint                     overhead);

void            gst_base_parse_frame_init      (GstBaseParseFrame * frame);

void            gst_base_parse_frame_free      (GstBaseParseFrame * frame);

GstFlowReturn   gst_base_parse_push_frame      (GstBaseParse      * parse,
                                                GstBaseParseFrame * frame);

void            gst_base_parse_set_duration    (GstBaseParse      * parse,
                                                GstFormat           fmt,
                                                gint64              duration,
                                                gint                interval);

void            gst_base_parse_set_average_bitrate (GstBaseParse   * parse,
                                                    guint            bitrate);

void            gst_base_parse_set_min_frame_size (GstBaseParse    * parse,
                                                   guint             min_size);

void            gst_base_parse_set_has_timing_info (GstBaseParse   * parse,
                                                    gboolean         has_timing);

void            gst_base_parse_set_syncable    (GstBaseParse * parse,
                                                gboolean       syncable);

void            gst_base_parse_set_passthrough (GstBaseParse * parse,
                                                gboolean       passthrough);

void            gst_base_parse_set_frame_rate  (GstBaseParse * parse,
                                                guint          fps_num,
                                                guint          fps_den,
                                                guint          lead_in,
                                                guint          lead_out);

gboolean        gst_base_parse_convert_default (GstBaseParse * parse,
                                                GstFormat      src_format,
                                                gint64         src_value,
                                                GstFormat      dest_format,
                                                gint64       * dest_value);

gboolean        gst_base_parse_add_index_entry (GstBaseParse * parse,
                                                guint64        offset,
                                                GstClockTime   ts,
                                                gboolean       key,
                                                gboolean       force);

G_END_DECLS

#endif /* __GST_BASE_PARSE_H__ */
