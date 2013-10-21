/* GStreamer
 * Copyright (C) 2005 Wim Taymans <wim@fluendo.com>
 *
 * gstsegment.c: GstSegment subsystem
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

#include "gst_private.h"

#include <math.h>

#include "gstutils.h"
#include "gstsegment.h"

/**
 * SECTION:gstsegment
 * @short_description: Structure describing the configured region of interest
 *                     in a media file.
 * @see_also: #GstEvent
 *
 * This helper structure holds the relevant values for tracking the region of
 * interest in a media file, called a segment. 
 *
 * The structure can be used for two purposes:
 * <itemizedlist>
 *   <listitem><para>performing seeks (handling seek events)</para></listitem>
 *   <listitem><para>tracking playback regions (handling newsegment events)</para></listitem>
 * </itemizedlist>
 *
 * The segment is usually configured by the application with a seek event which 
 * is propagated upstream and eventually handled by an element that performs the seek.
 *
 * The configured segment is then propagated back downstream with a newsegment event.
 * This information is then used to clip media to the segment boundaries.
 *
 * A segment structure is initialized with gst_segment_init(), which takes a #GstFormat
 * that will be used as the format of the segment values. The segment will be configured
 * with a start value of 0 and a stop/duration of -1, which is undefined. The default
 * rate and applied_rate is 1.0.
 *
 * If the segment is used for managing seeks, the segment duration should be set with
 * gst_segment_set_duration(). The public duration field contains the duration of the
 * segment. When using the segment for seeking, the start and time members should 
 * normally be left to their default 0 value. The stop position is left to -1 unless
 * explicitly configured to a different value after a seek event.
 *
 * The current position in the segment should be set with the gst_segment_set_last_stop().
 * The public last_stop field contains the last set stop position in the segment.
 *
 * For elements that perform seeks, the current segment should be updated with the
 * gst_segment_set_seek() and the values from the seek event. This method will update
 * all the segment fields. The last_stop field will contain the new playback position.
 * If the cur_type was different from GST_SEEK_TYPE_NONE, playback continues from
 * the last_stop position, possibly with updated flags or rate.
 *
 * For elements that want to use #GstSegment to track the playback region, use
 * gst_segment_set_newsegment() to update the segment fields with the information from
 * the newsegment event. The gst_segment_clip() method can be used to check and clip
 * the media data to the segment boundaries.
 *
 * For elements that want to synchronize to the pipeline clock, gst_segment_to_running_time()
 * can be used to convert a timestamp to a value that can be used to synchronize
 * to the clock. This function takes into account all accumulated segments as well as
 * any rate or applied_rate conversions.
 *
 * For elements that need to perform operations on media data in stream_time, 
 * gst_segment_to_stream_time() can be used to convert a timestamp and the segment
 * info to stream time (which is always between 0 and the duration of the stream).
 *
 * Last reviewed on 2007-05-17 (0.10.13)
 */

/**
 * gst_segment_copy:
 * @segment: (transfer none): a #GstSegment
 *
 * Create a copy of given @segment.
 *
 * Free-function: gst_segment_free
 *
 * Returns: (transfer full): a new #GstSegment, free with gst_segment_free().
 *
 * Since: 0.10.20
 */
GstSegment *
gst_segment_copy (GstSegment * segment)
{
  GstSegment *result = NULL;

  if (segment) {
    result = (GstSegment *) g_slice_copy (sizeof (GstSegment), segment);
  }
  return result;
}

GType
gst_segment_get_type (void)
{
  static GType gst_segment_type = 0;

  if (G_UNLIKELY (gst_segment_type == 0)) {
    gst_segment_type = g_boxed_type_register_static ("GstSegment",
        (GBoxedCopyFunc) gst_segment_copy, (GBoxedFreeFunc) gst_segment_free);
  }

  return gst_segment_type;
}

/**
 * gst_segment_new:
 *
 * Allocate a new #GstSegment structure and initialize it using 
 * gst_segment_init().
 *
 * Free-function: gst_segment_free
 *
 * Returns: (transfer full): a new #GstSegment, free with gst_segment_free().
 */
GstSegment *
gst_segment_new (void)
{
  GstSegment *result;

  result = g_slice_new0 (GstSegment);
  gst_segment_init (result, GST_FORMAT_UNDEFINED);

  return result;
}

/**
 * gst_segment_free:
 * @segment: (in) (transfer full): a #GstSegment
 *
 * Free the allocated segment @segment.
 */
void
gst_segment_free (GstSegment * segment)
{
  g_slice_free (GstSegment, segment);
}

/**
 * gst_segment_init:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 *
 * The start/last_stop positions are set to 0 and the stop/duration
 * fields are set to -1 (unknown). The default rate of 1.0 and no
 * flags are set.
 *
 * Initialize @segment to its default values.
 */
void
gst_segment_init (GstSegment * segment, GstFormat format)
{
  g_return_if_fail (segment != NULL);

  segment->rate = 1.0;
  segment->abs_rate = 1.0;
  segment->applied_rate = 1.0;
  segment->format = format;
  segment->flags = 0;
  segment->start = 0;
  segment->stop = -1;
  segment->time = 0;
  segment->accum = 0;
  segment->last_stop = 0;
  segment->duration = -1;
}

/**
 * gst_segment_set_duration:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @duration: the duration of the segment info or -1 if unknown.
 *
 * Set the duration of the segment to @duration. This function is mainly
 * used by elements that perform seeking and know the total duration of the
 * segment. 
 * 
 * This field should be set to allow seeking requests relative to the
 * duration.
 */
void
gst_segment_set_duration (GstSegment * segment, GstFormat format,
    gint64 duration)
{
  g_return_if_fail (segment != NULL);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;
  else
    g_return_if_fail (segment->format == format);

  segment->duration = duration;
}

/**
 * gst_segment_set_last_stop:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @position: the position 
 *
 * Set the last observed stop position in the segment to @position.
 *
 * This field should be set to allow seeking requests relative to the
 * current playing position.
 */
void
gst_segment_set_last_stop (GstSegment * segment, GstFormat format,
    gint64 position)
{
  g_return_if_fail (segment != NULL);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;
  else
    g_return_if_fail (segment->format == format);

  segment->last_stop = MAX (segment->start, position);
}

/**
 * gst_segment_set_seek:
 * @segment: a #GstSegment structure.
 * @rate: the rate of the segment.
 * @format: the format of the segment.
 * @flags: the seek flags for the segment
 * @start_type: the seek method
 * @start: the seek start value
 * @stop_type: the seek method
 * @stop: the seek stop value
 * @update: boolean holding whether last_stop was updated.
 *
 * Update the segment structure with the field values of a seek event (see
 * gst_event_new_seek()).
 *
 * After calling this method, the segment field last_stop and time will
 * contain the requested new position in the segment. The new requested
 * position in the segment depends on @rate and @start_type and @stop_type. 
 *
 * For positive @rate, the new position in the segment is the new @segment
 * start field when it was updated with a @start_type different from
 * #GST_SEEK_TYPE_NONE. If no update was performed on @segment start position
 * (#GST_SEEK_TYPE_NONE), @start is ignored and @segment last_stop is
 * unmodified.
 *
 * For negative @rate, the new position in the segment is the new @segment
 * stop field when it was updated with a @stop_type different from
 * #GST_SEEK_TYPE_NONE. If no stop was previously configured in the segment, the
 * duration of the segment will be used to update the stop position.
 * If no update was performed on @segment stop position (#GST_SEEK_TYPE_NONE),
 * @stop is ignored and @segment last_stop is unmodified.
 *
 * The applied rate of the segment will be set to 1.0 by default.
 * If the caller can apply a rate change, it should update @segment
 * rate and applied_rate after calling this function.
 *
 * @update will be set to TRUE if a seek should be performed to the segment 
 * last_stop field. This field can be FALSE if, for example, only the @rate
 * has been changed but not the playback position.
 */
void
gst_segment_set_seek (GstSegment * segment, gdouble rate,
    GstFormat format, GstSeekFlags flags,
    GstSeekType start_type, gint64 start,
    GstSeekType stop_type, gint64 stop, gboolean * update)
{
  gboolean update_stop, update_start;
  gint64 last_stop;

  g_return_if_fail (rate != 0.0);
  g_return_if_fail (segment != NULL);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;

  update_start = update_stop = TRUE;

  /* segment->start is never invalid */
  switch (start_type) {
    case GST_SEEK_TYPE_NONE:
      /* no update to segment, take previous start */
      start = segment->start;
      update_start = FALSE;
      break;
    case GST_SEEK_TYPE_SET:
      /* start holds desired position, map -1 to the start */
      if (start == -1)
        start = 0;
      /* start must be 0 or the formats must match */
      g_return_if_fail (start == 0 || segment->format == format);
      break;
    case GST_SEEK_TYPE_CUR:
      g_return_if_fail (start == 0 || segment->format == format);
      /* add start to currently configured segment */
      start = segment->start + start;
      break;
    case GST_SEEK_TYPE_END:
      if (segment->duration != -1) {
        g_return_if_fail (start == 0 || segment->format == format);
        /* add start to total length */
        start = segment->duration + start;
      } else {
        /* no update if duration unknown */
        start = segment->start;
        update_start = FALSE;
      }
      break;
  }
  /* bring in sane range */
  if (segment->duration != -1)
    start = CLAMP (start, 0, segment->duration);
  else
    start = MAX (start, 0);

  /* stop can be -1 if we have not configured a stop. */
  switch (stop_type) {
    case GST_SEEK_TYPE_NONE:
      stop = segment->stop;
      update_stop = FALSE;
      break;
    case GST_SEEK_TYPE_SET:
      /* stop holds required value, if it's not -1, it must be of the same
       * format as the segment. */
      g_return_if_fail (stop == -1 || segment->format == format);
      break;
    case GST_SEEK_TYPE_CUR:
      if (segment->stop != -1) {
        /* only add compatible formats or 0 */
        g_return_if_fail (stop == 0 || segment->format == format);
        stop = segment->stop + stop;
      } else
        stop = -1;
      break;
    case GST_SEEK_TYPE_END:
      if (segment->duration != -1) {
        /* only add compatible formats or 0 */
        g_return_if_fail (stop == 0 || segment->format == format);
        stop = segment->duration + stop;
      } else {
        stop = segment->stop;
        update_stop = FALSE;
      }
      break;
  }

  /* if we have a valid stop time, make sure it is clipped */
  if (stop != -1) {
    if (segment->duration != -1)
      stop = CLAMP (stop, 0, segment->duration);
    else
      stop = MAX (stop, 0);
  }

  /* we can't have stop before start */
  if (stop != -1)
    g_return_if_fail (start <= stop);

  segment->rate = rate;
  segment->abs_rate = ABS (rate);
  segment->applied_rate = 1.0;
  segment->flags = flags;
  segment->start = start;
  segment->stop = stop;
  segment->time = start;

  last_stop = segment->last_stop;
  if (update_start && rate > 0.0) {
    last_stop = start;
  }
  if (update_stop && rate < 0.0) {
    if (stop != -1)
      last_stop = stop;
    else {
      if (segment->duration != -1)
        last_stop = segment->duration;
      else
        last_stop = 0;
    }
  }
  /* set update arg to reflect update of last_stop */
  if (update)
    *update = last_stop != segment->last_stop;

  /* update new position */
  segment->last_stop = last_stop;
}

/**
 * gst_segment_set_newsegment:
 * @segment: a #GstSegment structure.
 * @update: flag indicating a new segment is started or updated
 * @rate: the rate of the segment.
 * @format: the format of the segment.
 * @start: the new start value
 * @stop: the new stop value
 * @time: the new stream time
 *
 * Update the segment structure with the field values of a new segment event and
 * with a default applied_rate of 1.0.
 *
 * Since: 0.10.6
 */
void
gst_segment_set_newsegment (GstSegment * segment, gboolean update, gdouble rate,
    GstFormat format, gint64 start, gint64 stop, gint64 time)
{
  gst_segment_set_newsegment_full (segment, update, rate, 1.0, format, start,
      stop, time);
}

/**
 * gst_segment_set_newsegment_full:
 * @segment: a #GstSegment structure.
 * @update: flag indicating a new segment is started or updated
 * @rate: the rate of the segment.
 * @applied_rate: the applied rate of the segment.
 * @format: the format of the segment.
 * @start: the new start value
 * @stop: the new stop value
 * @time: the new stream time
 *
 * Update the segment structure with the field values of a new segment event.
 */
void
gst_segment_set_newsegment_full (GstSegment * segment, gboolean update,
    gdouble rate, gdouble applied_rate, GstFormat format, gint64 start,
    gint64 stop, gint64 time)
{
  gint64 duration, last_stop;

  g_return_if_fail (rate != 0.0);
  g_return_if_fail (applied_rate != 0.0);
  g_return_if_fail (segment != NULL);

  GST_DEBUG ("configuring segment update %d, rate %lf, format %s, "
      "start %" G_GINT64_FORMAT ", stop %" G_GINT64_FORMAT ", position %"
      G_GINT64_FORMAT, update, rate, gst_format_get_name (format), start,
      stop, time);
  GST_DEBUG ("old segment was: %" GST_SEGMENT_FORMAT, segment);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;

  /* any other format with 0 also gives time 0, the other values are
   * invalid in the format though. */
  if (format != segment->format && start == 0) {
    format = segment->format;
    if (stop != 0)
      stop = -1;
    if (time != 0)
      time = -1;
  }

  g_return_if_fail (segment->format == format);

  if (update) {
    if (G_LIKELY (segment->rate > 0.0)) {
      /* an update to the current segment is done, elapsed time is
       * difference between the old start and new start. */
      if (start > segment->start)
        duration = start - segment->start;
      else
        duration = 0;
    } else {
      /* for negative rates, the elapsed duration is the diff between the stop
       * positions */
      if (stop != -1 && stop < segment->stop)
        duration = segment->stop - stop;
      else
        duration = 0;
    }
    /* update last_stop to be a valid value in the updated segment */
    if (start > segment->last_stop)
      last_stop = start;
    else if (stop != -1 && stop < segment->last_stop)
      last_stop = stop;
    else
      last_stop = segment->last_stop;
  } else {
    /* the new segment has to be aligned with the old segment.
     * We first update the accumulated time of the previous
     * segment. the accumulated time is used when syncing to the
     * clock. */
    if (segment->stop != -1) {
      duration = segment->stop - segment->start;
    } else if (segment->last_stop != -1) {
      /* else use last seen timestamp as segment stop */
      duration = segment->last_stop - segment->start;
    } else {
      /* else we don't know and throw a warning.. really, this should
       * be fixed in the element. */
      g_warning ("closing segment of unknown duration, assuming duration of 0");
      duration = 0;
    }
    /* position the last_stop to the next expected position in the new segment,
     * which is the start or the stop of the segment */
    if (rate > 0.0)
      last_stop = start;
    else
      last_stop = stop;
  }
  /* use previous rate to calculate duration */
  if (G_LIKELY (segment->abs_rate != 1.0))
    duration /= segment->abs_rate;

  /* accumulate duration */
  segment->accum += duration;

  /* then update the current segment */
  segment->rate = rate;
  segment->abs_rate = ABS (rate);
  segment->applied_rate = applied_rate;
  segment->start = start;
  segment->last_stop = last_stop;
  segment->stop = stop;
  segment->time = time;
}

/**
 * gst_segment_to_stream_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @position: the position in the segment
 *
 * Translate @position to stream time using the currently configured 
 * segment. The @position value must be between @segment start and
 * stop value. 
 *
 * This function is typically used by elements that need to operate on
 * the stream time of the buffers it receives, such as effect plugins.
 * In those use cases, @position is typically the buffer timestamp or 
 * clock time that one wants to convert to the stream time.
 * The stream time is always between 0 and the total duration of the 
 * media stream. 
 *
 * Returns: the position in stream_time or -1 when an invalid position
 * was given.
 */
gint64
gst_segment_to_stream_time (GstSegment * segment, GstFormat format,
    gint64 position)
{
  gint64 result, start, stop, time;
  gdouble abs_applied_rate;

  /* format does not matter for -1 */
  if (G_UNLIKELY (position == -1))
    return -1;

  g_return_val_if_fail (segment != NULL, -1);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;

  /* if we have the position for the same format as the segment, we can compare
   * the start and stop values, otherwise we assume 0 and -1 */
  if (G_LIKELY (segment->format == format)) {
    start = segment->start;
    stop = segment->stop;
    time = segment->time;
  } else {
    start = 0;
    stop = -1;
    time = 0;
  }

  /* outside of the segment boundary stop */
  if (G_UNLIKELY (stop != -1 && position > stop))
    return -1;

  /* before the segment boundary */
  if (G_UNLIKELY (position < start))
    return -1;

  /* time must be known */
  if (G_UNLIKELY (time == -1))
    return -1;

  /* bring to uncorrected position in segment */
  result = position - start;

  abs_applied_rate = ABS (segment->applied_rate);

  /* correct for applied rate if needed */
  if (G_UNLIKELY (abs_applied_rate != 1.0))
    result *= abs_applied_rate;

  /* add or subtract from segment time based on applied rate */
  if (G_LIKELY (segment->applied_rate > 0.0)) {
    /* correct for segment time */
    result += time;
  } else {
    /* correct for segment time, clamp at 0. Streams with a negative
     * applied_rate have timestamps between start and stop, as usual, but have
     * the time member starting high and going backwards.  */
    if (G_LIKELY (time > result))
      result = time - result;
    else
      result = 0;
  }

  return result;
}

/**
 * gst_segment_to_running_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @position: the position in the segment
 *
 * Translate @position to the total running time using the currently configured 
 * and previously accumulated segments. Position is a value between @segment
 * start and stop time.
 *
 * This function is typically used by elements that need to synchronize to the
 * global clock in a pipeline. The runnning time is a constantly increasing value
 * starting from 0. When gst_segment_init() is called, this value will reset to
 * 0.
 *
 * This function returns -1 if the position is outside of @segment start and stop.
 *
 * Returns: the position as the total running time or -1 when an invalid position
 * was given.
 */
gint64
gst_segment_to_running_time (GstSegment * segment, GstFormat format,
    gint64 position)
{
  gint64 result;
  gint64 start, stop, accum;

  if (G_UNLIKELY (position == -1))
    return -1;

  g_return_val_if_fail (segment != NULL, -1);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;

  /* if we have the position for the same format as the segment, we can compare
   * the start and stop values, otherwise we assume 0 and -1 */
  if (G_LIKELY (segment->format == format)) {
    start = segment->start;
    stop = segment->stop;
    accum = segment->accum;
  } else {
    start = 0;
    stop = -1;
    accum = 0;
  }

  /* before the segment boundary */
  if (G_UNLIKELY (position < start))
    return -1;

  if (G_LIKELY (segment->rate > 0.0)) {
    /* outside of the segment boundary stop */
    if (G_UNLIKELY (stop != -1 && position > stop))
      return -1;

    /* bring to uncorrected position in segment */
    result = position - start;
  } else {
    /* cannot continue if no stop position set or outside of
     * the segment. */
    if (G_UNLIKELY (stop == -1 || position > stop))
      return -1;

    /* bring to uncorrected position in segment */
    result = stop - position;
  }

  /* scale based on the rate, avoid division by and conversion to 
   * float when not needed */
  if (G_UNLIKELY (segment->abs_rate != 1.0))
    result /= segment->abs_rate;

  /* correct for accumulated segments */
  result += accum;

  return result;
}

/**
 * gst_segment_clip:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @start: the start position in the segment
 * @stop: the stop position in the segment
 * @clip_start: (out) (allow-none): the clipped start position in the segment
 * @clip_stop: (out) (allow-none): the clipped stop position in the segment
 *
 * Clip the given @start and @stop values to the segment boundaries given
 * in @segment. @start and @stop are compared and clipped to @segment 
 * start and stop values.
 *
 * If the function returns FALSE, @start and @stop are known to fall
 * outside of @segment and @clip_start and @clip_stop are not updated.
 *
 * When the function returns TRUE, @clip_start and @clip_stop will be
 * updated. If @clip_start or @clip_stop are different from @start or @stop
 * respectively, the region fell partially in the segment.
 *
 * Note that when @stop is -1, @clip_stop will be set to the end of the
 * segment. Depending on the use case, this may or may not be what you want.
 *
 * Returns: TRUE if the given @start and @stop times fall partially or 
 *     completely in @segment, FALSE if the values are completely outside 
 *     of the segment.
 */
gboolean
gst_segment_clip (GstSegment * segment, GstFormat format, gint64 start,
    gint64 stop, gint64 * clip_start, gint64 * clip_stop)
{
  g_return_val_if_fail (segment != NULL, FALSE);

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;
  else
    g_return_val_if_fail (segment->format == format, FALSE);

  /* if we have a stop position and a valid start and start is bigger, 
   * we're outside of the segment */
  if (G_UNLIKELY (segment->stop != -1 && start != -1 && start >= segment->stop))
    return FALSE;

  /* if a stop position is given and is before the segment start,
   * we're outside of the segment. Special case is were start
   * and stop are equal to the segment start. In that case we
   * are inside the segment. */
  if (G_UNLIKELY (stop != -1 && (stop < segment->start || (start != stop
                  && stop == segment->start))))
    return FALSE;

  if (clip_start) {
    if (start == -1)
      *clip_start = -1;
    else
      *clip_start = MAX (start, segment->start);
  }

  if (clip_stop) {
    if (stop == -1)
      *clip_stop = segment->stop;
    else if (segment->stop == -1)
      *clip_stop = MAX (-1, stop);
    else
      *clip_stop = MIN (stop, segment->stop);

    if (segment->duration != -1)
      *clip_stop = MIN (*clip_stop, segment->duration);
  }

  return TRUE;
}

/**
 * gst_segment_to_position:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @running_time: the running_time in the segment
 *
 * Convert @running_time into a position in the segment so that
 * gst_segment_to_running_time() with that position returns @running_time.
 *
 * Returns: the position in the segment for @running_time. This function returns
 * -1 when @running_time is -1 or when it is not inside @segment.
 *
 * Since: 0.10.24
 */
gint64
gst_segment_to_position (GstSegment * segment, GstFormat format,
    gint64 running_time)
{
  gint64 result;
  gint64 start, stop, accum;

  g_return_val_if_fail (segment != NULL, -1);

  if (G_UNLIKELY (running_time == -1))
    return -1;

  if (G_UNLIKELY (segment->format == GST_FORMAT_UNDEFINED))
    segment->format = format;

  /* if we have the position for the same format as the segment, we can compare
   * the start and stop values, otherwise we assume 0 and -1 */
  if (G_LIKELY (segment->format == format)) {
    start = segment->start;
    stop = segment->stop;
    accum = segment->accum;
  } else {
    start = 0;
    stop = -1;
    accum = 0;
  }

  /* this running_time was for a previous segment */
  if (running_time < accum)
    return -1;

  /* start by subtracting the accumulated time */
  result = running_time - accum;

  /* move into the segment at the right rate */
  if (G_UNLIKELY (segment->abs_rate != 1.0))
    result = ceil (result * segment->abs_rate);

  if (G_LIKELY (segment->rate > 0.0)) {
    /* bring to corrected position in segment */
    result += start;

    /* outside of the segment boundary stop */
    if (G_UNLIKELY (stop != -1 && result > stop))
      return -1;
  } else {
    /* cannot continue if no stop position set or outside of
     * the segment. */
    if (G_UNLIKELY (stop == -1 || result + start > stop))
      return -1;

    /* bring to corrected position in segment */
    result = stop - result;
  }
  return result;
}


/**
 * gst_segment_set_running_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @running_time: the running_time in the segment
 *
 * Adjust the start/stop and accum values of @segment such that the next valid
 * buffer will be one with @running_time.
 *
 * Returns: %TRUE if the segment could be updated successfully. If %FALSE is
 * returned, @running_time is -1 or not in @segment.
 *
 * Since: 0.10.24
 */
gboolean
gst_segment_set_running_time (GstSegment * segment, GstFormat format,
    gint64 running_time)
{
  gint64 position;
  gint64 start, stop, last_stop;

  /* start by bringing the running_time into the segment position */
  position = gst_segment_to_position (segment, format, running_time);

  /* we must have a valid position now */
  if (G_UNLIKELY (position == -1))
    return FALSE;

  start = segment->start;
  stop = segment->stop;
  last_stop = segment->last_stop;

  if (G_LIKELY (segment->rate > 0.0)) {
    /* update the start/last_stop and time values */
    start = position;
    if (last_stop < start)
      last_stop = start;
  } else {
    /* reverse, update stop */
    stop = position;
    /* if we were past the position, go back */
    if (last_stop > stop)
      last_stop = stop;
  }
  /* and accumulated time is exactly the running time */
  segment->time = gst_segment_to_stream_time (segment, format, start);
  segment->start = start;
  segment->stop = stop;
  segment->last_stop = last_stop;
  segment->accum = running_time;

  return TRUE;
}
