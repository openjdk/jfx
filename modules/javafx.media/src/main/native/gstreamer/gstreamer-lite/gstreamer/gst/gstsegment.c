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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#include "gst_private.h"

#include <math.h>

#include "gstutils.h"
#include "gstsegment.h"

/**
 * SECTION:gstsegment
 * @title: GstSegment
 * @short_description: Structure describing the configured region of interest
 *                     in a media file.
 * @see_also: #GstEvent
 *
 * This helper structure holds the relevant values for tracking the region of
 * interest in a media file, called a segment.
 *
 * The structure can be used for two purposes:
 *
 *   * performing seeks (handling seek events)
 *   * tracking playback regions (handling newsegment events)
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
 * The public duration field contains the duration of the segment. When using
 * the segment for seeking, the start and time members should normally be left
 * to their default 0 value. The stop position is left to -1 unless explicitly
 * configured to a different value after a seek event.
 *
 * The current position in the segment should be set by changing the position
 * member in the structure.
 *
 * For elements that perform seeks, the current segment should be updated with the
 * gst_segment_do_seek() and the values from the seek event. This method will update
 * all the segment fields. The position field will contain the new playback position.
 * If the start_type was different from GST_SEEK_TYPE_NONE, playback continues from
 * the position position, possibly with updated flags or rate.
 *
 * For elements that want to use #GstSegment to track the playback region,
 * update the segment fields with the information from the newsegment event.
 * The gst_segment_clip() method can be used to check and clip
 * the media data to the segment boundaries.
 *
 * For elements that want to synchronize to the pipeline clock, gst_segment_to_running_time()
 * can be used to convert a timestamp to a value that can be used to synchronize
 * to the clock. This function takes into account the base as well as
 * any rate or applied_rate conversions.
 *
 * For elements that need to perform operations on media data in stream_time,
 * gst_segment_to_stream_time() can be used to convert a timestamp and the segment
 * info to stream time (which is always between 0 and the duration of the stream).
 */

/* FIXME 2.0: remove unused format parameter.
 * Most of the methods in gstsegment.c take and extra GstFormat format, just to
 * verify segment->format == format.
 * See https://bugzilla.gnome.org/show_bug.cgi?id=788979
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
 */
GstSegment *
gst_segment_copy (const GstSegment * segment)
{
  GstSegment *result = NULL;

  if (segment) {
    result = (GstSegment *) g_slice_copy (sizeof (GstSegment), segment);
  }
  return result;
}

/**
 * gst_segment_copy_into:
 * @src: (transfer none): a #GstSegment
 * @dest: (transfer none): a #GstSegment
 *
 * Copy the contents of @src into @dest.
 */
void
gst_segment_copy_into (const GstSegment * src, GstSegment * dest)
{
  memcpy (dest, src, sizeof (GstSegment));
}

G_DEFINE_BOXED_TYPE (GstSegment, gst_segment,
    (GBoxedCopyFunc) gst_segment_copy, (GBoxedFreeFunc) gst_segment_free);

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
 * The start/position fields are set to 0 and the stop/duration
 * fields are set to -1 (unknown). The default rate of 1.0 and no
 * flags are set.
 *
 * Initialize @segment to its default values.
 */
void
gst_segment_init (GstSegment * segment, GstFormat format)
{
  g_return_if_fail (segment != NULL);

  segment->flags = GST_SEGMENT_FLAG_NONE;
  segment->rate = 1.0;
  segment->applied_rate = 1.0;
  segment->format = format;
  segment->base = 0;
  segment->offset = 0;
  segment->start = 0;
  segment->stop = -1;
  segment->time = 0;
  segment->position = 0;
  segment->duration = -1;
}

/**
 * gst_segment_do_seek:
 * @segment: a #GstSegment structure.
 * @rate: the rate of the segment.
 * @format: the format of the segment.
 * @flags: the segment flags for the segment
 * @start_type: the seek method
 * @start: the seek start value
 * @stop_type: the seek method
 * @stop: the seek stop value
 * @update: (out) (allow-none): boolean holding whether position was updated.
 *
 * Update the segment structure with the field values of a seek event (see
 * gst_event_new_seek()).
 *
 * After calling this method, the segment field position and time will
 * contain the requested new position in the segment. The new requested
 * position in the segment depends on @rate and @start_type and @stop_type.
 *
 * For positive @rate, the new position in the segment is the new @segment
 * start field when it was updated with a @start_type different from
 * #GST_SEEK_TYPE_NONE. If no update was performed on @segment start position
 * (#GST_SEEK_TYPE_NONE), @start is ignored and @segment position is
 * unmodified.
 *
 * For negative @rate, the new position in the segment is the new @segment
 * stop field when it was updated with a @stop_type different from
 * #GST_SEEK_TYPE_NONE. If no stop was previously configured in the segment, the
 * duration of the segment will be used to update the stop position.
 * If no update was performed on @segment stop position (#GST_SEEK_TYPE_NONE),
 * @stop is ignored and @segment position is unmodified.
 *
 * The applied rate of the segment will be set to 1.0 by default.
 * If the caller can apply a rate change, it should update @segment
 * rate and applied_rate after calling this function.
 *
 * @update will be set to %TRUE if a seek should be performed to the segment
 * position field. This field can be %FALSE if, for example, only the @rate
 * has been changed but not the playback position.
 *
 * Returns: %TRUE if the seek could be performed.
 */
gboolean
gst_segment_do_seek (GstSegment * segment, gdouble rate,
    GstFormat format, GstSeekFlags flags,
    GstSeekType start_type, guint64 start,
    GstSeekType stop_type, guint64 stop, gboolean * update)
{
  gboolean update_stop, update_start;
  guint64 position, base;

  g_return_val_if_fail (rate != 0.0, FALSE);
  g_return_val_if_fail (segment != NULL, FALSE);
  g_return_val_if_fail (segment->format == format, FALSE);

  /* Elements should not pass instant-rate seeks to gst_segment_do_seek().
   * This helps catch elements that have not been updated yet */
  if (flags & GST_SEEK_FLAG_INSTANT_RATE_CHANGE)
    return FALSE;

  update_start = update_stop = TRUE;

  position = segment->position;

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
      break;
    case GST_SEEK_TYPE_END:
      if (segment->duration != -1) {
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
    start = MIN (start, segment->duration);
  else
    start = MAX ((gint64) start, 0);

  /* stop can be -1 if we have not configured a stop. */
  switch (stop_type) {
    case GST_SEEK_TYPE_NONE:
      stop = segment->stop;
      update_stop = FALSE;
      break;
    case GST_SEEK_TYPE_SET:
      /* stop holds required value */
      break;
    case GST_SEEK_TYPE_END:
      if (segment->duration != -1) {
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
      stop = CLAMP ((gint64) stop, 0, (gint64) segment->duration);
    else
      stop = MAX ((gint64) stop, 0);
  }

  /* we can't have stop before start */
  if (stop != -1) {
    if (start > stop) {
      GST_WARNING ("segment update failed: start(%" G_GUINT64_FORMAT
          ") > stop(%" G_GUINT64_FORMAT ")", start, stop);
      g_return_val_if_fail (start <= stop, FALSE);
      return FALSE;
    }
  }

  if (flags & GST_SEEK_FLAG_FLUSH) {
    /* flush resets the running_time */
    base = 0;
  } else {
    /* make sure the position is inside the segment start/stop */
    position = CLAMP (position, segment->start, segment->stop);

    /* remember the elapsed time */
    base = gst_segment_to_running_time (segment, format, position);
    GST_DEBUG ("updated segment.base: %" G_GUINT64_FORMAT, base);
  }

  if (update_start && rate > 0.0) {
    position = start;
  }
  if (update_stop && rate < 0.0) {
    if (stop != -1)
      position = stop;
    else {
      if (segment->duration != -1)
        position = segment->duration;
      else
        position = 0;
    }
  }

  /* set update arg to reflect update of position */
  if (update)
    *update = position != segment->position;

  /* update new values */
  /* be explicit about our GstSeekFlag -> GstSegmentFlag conversion */
  segment->flags = GST_SEGMENT_FLAG_NONE;
  if ((flags & GST_SEEK_FLAG_FLUSH) != 0)
    segment->flags |= GST_SEGMENT_FLAG_RESET;
  if ((flags & GST_SEEK_FLAG_TRICKMODE) != 0)
    segment->flags |= GST_SEGMENT_FLAG_TRICKMODE;
  if ((flags & GST_SEEK_FLAG_SEGMENT) != 0)
    segment->flags |= GST_SEGMENT_FLAG_SEGMENT;
  if ((flags & GST_SEEK_FLAG_TRICKMODE_KEY_UNITS) != 0)
    segment->flags |= GST_SEGMENT_FLAG_TRICKMODE_KEY_UNITS;
  if ((flags & GST_SEEK_FLAG_TRICKMODE_NO_AUDIO) != 0)
    segment->flags |= GST_SEGMENT_FLAG_TRICKMODE_NO_AUDIO;
  if ((flags & GST_SEEK_FLAG_TRICKMODE_FORWARD_PREDICTED) != 0)
    segment->flags |= GST_SEGMENT_FLAG_TRICKMODE_FORWARD_PREDICTED;

  segment->rate = rate;
  segment->applied_rate = 1.0;

  segment->base = base;
  if (rate > 0.0)
    segment->offset = position - start;
  else {
    if (stop != -1)
      segment->offset = stop - position;
    else if (segment->duration != -1)
      segment->offset = segment->duration - position;
    else
      segment->offset = 0;
  }

  segment->start = start;
  segment->stop = stop;
  segment->time = start;
  segment->position = position;

  GST_INFO ("segment updated: %" GST_SEGMENT_FORMAT, segment);

  return TRUE;
}

/**
 * gst_segment_to_stream_time_full:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @position: the position in the segment
 * @stream_time: (out): result stream-time
 *
 * Translate @position to the total stream time using the currently configured
 * segment. Compared to gst_segment_to_stream_time() this function can return
 * negative stream-time.
 *
 * This function is typically used by elements that need to synchronize buffers
 * against the clock or each other.
 *
 * @position can be any value and the result of this function for values outside
 * of the segment is extrapolated.
 *
 * When 1 is returned, @position resulted in a positive stream-time returned
 * in @stream_time.
 *
 * When this function returns -1, the returned @stream_time should be negated
 * to get the real negative stream time.
 *
 * Returns: a 1 or -1 on success, 0 on failure.
 *
 * Since: 1.8
 */
gint
gst_segment_to_stream_time_full (const GstSegment * segment, GstFormat format,
    guint64 position, guint64 * stream_time)
{
  guint64 start, stop, time;
  gdouble abs_applied_rate;
  gint res;

  /* format does not matter for -1 */
  if (G_UNLIKELY (position == -1)) {
    *stream_time = -1;
    return 0;
  }

  g_return_val_if_fail (segment != NULL, 0);
  g_return_val_if_fail (segment->format == format, 0);

  stop = segment->stop;

  start = segment->start;
  time = segment->time;

  /* time must be known */
  if (G_UNLIKELY (time == -1))
    return 0;

  abs_applied_rate = ABS (segment->applied_rate);

  /* add or subtract from segment time based on applied rate */
  if (G_LIKELY (segment->applied_rate > 0.0)) {
    if (G_LIKELY (position > start)) {
      /* bring to uncorrected position in segment */
      *stream_time = position - start;
      /* correct for applied rate if needed */
      if (G_UNLIKELY (abs_applied_rate != 1.0))
        *stream_time *= abs_applied_rate;
      /* correct for segment time */
      *stream_time += time;
      res = 1;
    } else {
      *stream_time = start - position;
      if (G_UNLIKELY (abs_applied_rate != 1.0))
        *stream_time *= abs_applied_rate;
      if (*stream_time > time) {
        *stream_time -= time;
        res = -1;
      } else {
        *stream_time = time - *stream_time;
        res = 1;
      }
    }
  } else {
    /* correct for segment time. Streams with a negative applied_rate
     * have timestamps between start and stop, as usual, but have the
     * time member starting high and going backwards.  */
    /* cannot continue without a known segment stop */
    if (G_UNLIKELY (stop == -1))
      return 0;
    if (G_UNLIKELY (position > stop)) {
      *stream_time = position - stop;
      if (G_UNLIKELY (abs_applied_rate != 1.0))
        *stream_time *= abs_applied_rate;
      if (*stream_time > time) {
        *stream_time -= time;
        res = -1;
      } else {
        *stream_time = time - *stream_time;
        res = 1;
      }
    } else {
      *stream_time = stop - position;
      if (G_UNLIKELY (abs_applied_rate != 1.0))
        *stream_time *= abs_applied_rate;
      *stream_time += time;
      res = 1;
    }
  }

  return res;
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
 *
 * Since: 1.8
 */
guint64
gst_segment_to_stream_time (const GstSegment * segment, GstFormat format,
    guint64 position)
{
  guint64 result;

  g_return_val_if_fail (segment != NULL, -1);
  g_return_val_if_fail (segment->format == format, -1);

  /* before the segment boundary */
  if (G_UNLIKELY (position < segment->start)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") < start(%" G_GUINT64_FORMAT
        ")", position, segment->start);
    return -1;
  }
  /* after the segment boundary */
  if (G_UNLIKELY (segment->stop != -1 && position > segment->stop)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") > stop(%" G_GUINT64_FORMAT
        ")", position, segment->stop);
    return -1;
  }

#ifdef GSTREAMER_LITE
  if (segment->format != format)
    return -1;
#endif // GSTREAMER_LITE

  if (gst_segment_to_stream_time_full (segment, format, position, &result) == 1)
    return result;

  return -1;
}

/**
 * gst_segment_position_from_stream_time_full:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @stream_time: the stream-time
 * @position: (out): the resulting position in the segment
 *
 * Translate @stream_time to the segment position using the currently configured
 * segment. Compared to gst_segment_position_from_stream_time() this function can
 * return negative segment position.
 *
 * This function is typically used by elements that need to synchronize buffers
 * against the clock or each other.
 *
 * @stream_time can be any value and the result of this function for values outside
 * of the segment is extrapolated.
 *
 * When 1 is returned, @stream_time resulted in a positive position returned
 * in @position.
 *
 * When this function returns -1, the returned @position should be negated
 * to get the real negative segment position.
 *
 * Returns: a 1 or -1 on success, 0 on failure.
 *
 * Since: 1.8
 */
gint
gst_segment_position_from_stream_time_full (const GstSegment * segment,
    GstFormat format, guint64 stream_time, guint64 * position)
{
  guint64 start, time;
  gdouble abs_applied_rate;
  gint res;

  /* format does not matter for -1 */
  if (G_UNLIKELY (stream_time == -1)) {
    *position = -1;
    return 0;
  }

  g_return_val_if_fail (segment != NULL, -1);
  g_return_val_if_fail (segment->format == format, -1);

  start = segment->start;
  time = segment->time;

  /* time must be known */
  if (G_UNLIKELY (time == -1))
    return 0;

  abs_applied_rate = ABS (segment->applied_rate);

  if (G_LIKELY (segment->applied_rate > 0.0)) {
    if (G_LIKELY (stream_time > time)) {
      res = 1;
      *position = stream_time - time;
    } else {
      res = -1;
      *position = time - stream_time;
    }
    /* correct for applied rate if needed */
    if (G_UNLIKELY (abs_applied_rate != 1.0))
      *position /= abs_applied_rate;

    if (G_UNLIKELY (res == -1)) {
      if (*position > start) {
        *position -= start;
      } else {
        *position = start - *position;
        res = 1;
      }
    } else {
      *position += start;
    }
  } else {
    GstClockTime stop = segment->stop;
    /* cannot continue without a known segment stop */
    if (G_UNLIKELY (stop == -1))
      return 0;
    if (G_UNLIKELY (time > stream_time)) {
      res = -1;
      *position = time - stream_time;
    } else {
      res = 1;
      *position = stream_time - time;
    }
    if (G_UNLIKELY (abs_applied_rate != 1.0))
      *position /= abs_applied_rate;
    if (G_UNLIKELY (stop < *position)) {
      if (G_LIKELY (res == 1)) {
        *position -= stop;
        res = -1;
      } else {
        *position += stop;
        res = 1;
      }
    } else {
      if (G_LIKELY (res == 1)) {
        *position = stop - *position;
        res = 1;
      } else {
        *position += stop;
        res = 1;
      }
    }
  }

  return res;
}

/**
 * gst_segment_position_from_stream_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @stream_time: the stream_time in the segment
 *
 * Convert @stream_time into a position in the segment so that
 * gst_segment_to_stream_time() with that position returns @stream_time.
 *
 * Returns: the position in the segment for @stream_time. This function returns
 * -1 when @stream_time is -1 or when it is not inside @segment.
 *
 * Since: 1.8
 */
guint64
gst_segment_position_from_stream_time (const GstSegment * segment,
    GstFormat format, guint64 stream_time)
{
  guint64 position;
  gint res;

  g_return_val_if_fail (segment != NULL, -1);
  g_return_val_if_fail (segment->format == format, -1);

  res =
      gst_segment_position_from_stream_time_full (segment, format, stream_time,
      &position);

  /* before the segment boundary */
  if (G_UNLIKELY (position < segment->start)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") < start(%" G_GUINT64_FORMAT
        ")", position, segment->start);
    return -1;
  }

  /* after the segment boundary */
  if (G_UNLIKELY (segment->stop != -1 && position > segment->stop)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") > stop(%" G_GUINT64_FORMAT
        ")", position, segment->stop);
    return -1;
  }

  if (res == 1)
    return position;

  return -1;
}

/**
 * gst_segment_to_running_time_full:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @position: the position in the segment
 * @running_time: (out) (allow-none): result running-time
 *
 * Translate @position to the total running time using the currently configured
 * segment. Compared to gst_segment_to_running_time() this function can return
 * negative running-time.
 *
 * This function is typically used by elements that need to synchronize buffers
 * against the clock or each other.
 *
 * @position can be any value and the result of this function for values outside
 * of the segment is extrapolated.
 *
 * When 1 is returned, @position resulted in a positive running-time returned
 * in @running_time.
 *
 * When this function returns -1, the returned @running_time should be negated
 * to get the real negative running time.
 *
 * Returns: a 1 or -1 on success, 0 on failure.
 *
 * Since: 1.6
 */
gint
gst_segment_to_running_time_full (const GstSegment * segment, GstFormat format,
    guint64 position, guint64 * running_time)
{
  gint res = 0;
  guint64 result;
  guint64 start, stop, offset;
  gdouble abs_rate;

  if (G_UNLIKELY (position == -1)) {
    GST_DEBUG ("invalid position (-1)");
    goto done;
  }

  g_return_val_if_fail (segment != NULL, 0);
  g_return_val_if_fail (segment->format == format, 0);

  offset = segment->offset;

  if (G_LIKELY (segment->rate > 0.0)) {
    start = segment->start + offset;

    /* bring to uncorrected position in segment */
    if (position < start) {
      /* negative value */
      result = start - position;
      res = -1;
    } else {
      result = position - start;
      res = 1;
    }
  } else {
    stop = segment->stop;

    if (stop == -1 && segment->duration != -1)
      stop = segment->start + segment->duration;

    /* cannot continue if no stop position set or invalid offset */
    g_return_val_if_fail (stop != -1, 0);
    g_return_val_if_fail (stop >= offset, 0);

    stop -= offset;

    /* bring to uncorrected position in segment */
    if (position > stop) {
      /* negative value */
      result = position - stop;
      res = -1;
    } else {
      result = stop - position;
      res = 1;
    }
  }

  if (running_time) {
    /* scale based on the rate, avoid division by and conversion to
     * float when not needed */
    abs_rate = ABS (segment->rate);
    if (G_UNLIKELY (abs_rate != 1.0))
      result /= abs_rate;

    /* correct for base of the segment */
    if (res == 1)
      /* positive, add base */
      *running_time = result + segment->base;
    else if (segment->base >= result) {
      /* negative and base is bigger, subtract from base and we have a
       * positive value again */
      *running_time = segment->base - result;
      res = 1;
    } else {
      /* negative and base is smaller, subtract base and remainder is
       * negative */
      *running_time = result - segment->base;
    }
  }
  return res;

done:
  {
    if (running_time)
      *running_time = -1;
    return 0;
  }
}

/**
 * gst_segment_to_running_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @position: the position in the segment
 *
 * Translate @position to the total running time using the currently configured
 * segment. Position is a value between @segment start and stop time.
 *
 * This function is typically used by elements that need to synchronize to the
 * global clock in a pipeline. The running time is a constantly increasing value
 * starting from 0. When gst_segment_init() is called, this value will reset to
 * 0.
 *
 * This function returns -1 if the position is outside of @segment start and stop.
 *
 * Returns: the position as the total running time or -1 when an invalid position
 * was given.
 */
guint64
gst_segment_to_running_time (const GstSegment * segment, GstFormat format,
    guint64 position)
{
  guint64 result;

#ifdef GSTREAMER_LITE
  if (segment->format != format)
    return -1;
#endif // GSTREAMER_LITE

  g_return_val_if_fail (segment != NULL, -1);
  g_return_val_if_fail (segment->format == format, -1);

  /* before the segment boundary */
  if (G_UNLIKELY (position < segment->start)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") < start(%" G_GUINT64_FORMAT
        ")", position, segment->start);
    return -1;
  }
  /* after the segment boundary */
  if (G_UNLIKELY (segment->stop != -1 && position > segment->stop)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") > stop(%" G_GUINT64_FORMAT
        ")", position, segment->stop);
    return -1;
  }

  if (gst_segment_to_running_time_full (segment, format, position,
          &result) == 1)
    return result;

  return -1;
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
 * If the function returns %FALSE, @start and @stop are known to fall
 * outside of @segment and @clip_start and @clip_stop are not updated.
 *
 * When the function returns %TRUE, @clip_start and @clip_stop will be
 * updated. If @clip_start or @clip_stop are different from @start or @stop
 * respectively, the region fell partially in the segment.
 *
 * Note that when @stop is -1, @clip_stop will be set to the end of the
 * segment. Depending on the use case, this may or may not be what you want.
 *
 * Returns: %TRUE if the given @start and @stop times fall partially or
 *     completely in @segment, %FALSE if the values are completely outside
 *     of the segment.
 */
gboolean
gst_segment_clip (const GstSegment * segment, GstFormat format, guint64 start,
    guint64 stop, guint64 * clip_start, guint64 * clip_stop)
{
  g_return_val_if_fail (segment != NULL, FALSE);
  g_return_val_if_fail (segment->format == format, FALSE);

  /* if we have a stop position and a valid start and start is bigger,
   * we're outside of the segment. (Special case) segment start and
   * segment stop can be identical. In this case, if start is also identical,
   * it's inside of segment */
  if (G_UNLIKELY (segment->stop != -1 && start != -1 && (start > segment->stop
              || (segment->start != segment->stop && start == segment->stop))))
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
      *clip_stop = stop;
    else
      *clip_stop = MIN (stop, segment->stop);
  }

  return TRUE;
}

/**
 * gst_segment_position_from_running_time:
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
 * Since: 1.8
 */
guint64
gst_segment_position_from_running_time (const GstSegment * segment,
    GstFormat format, guint64 running_time)
{
  guint64 position;
  gint res;

  g_return_val_if_fail (segment != NULL, -1);
  g_return_val_if_fail (segment->format == format, -1);

  res =
      gst_segment_position_from_running_time_full (segment, format,
      running_time, &position);

  if (res != 1)
    return -1;

  /* before the segment boundary */
  if (G_UNLIKELY (position < segment->start)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") < start(%" G_GUINT64_FORMAT
        ")", position, segment->start);
    return -1;
  }

  /* after the segment boundary */
  if (G_UNLIKELY (segment->stop != -1 && position > segment->stop)) {
    GST_DEBUG ("position(%" G_GUINT64_FORMAT ") > stop(%" G_GUINT64_FORMAT
        ")", position, segment->stop);
    return -1;
  }

  return position;
}

/**
 * gst_segment_position_from_running_time_full:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @running_time: the running-time
 * @position: (out): the resulting position in the segment
 *
 * Translate @running_time to the segment position using the currently configured
 * segment. Compared to gst_segment_position_from_running_time() this function can
 * return negative segment position.
 *
 * This function is typically used by elements that need to synchronize buffers
 * against the clock or each other.
 *
 * @running_time can be any value and the result of this function for values
 * outside of the segment is extrapolated.
 *
 * When 1 is returned, @running_time resulted in a positive position returned
 * in @position.
 *
 * When this function returns -1, the returned @position was < 0, and the value
 * in the position variable should be negated to get the real negative segment
 * position.
 *
 * Returns: a 1 or -1 on success, 0 on failure.
 *
 * Since: 1.8
 */
gint
gst_segment_position_from_running_time_full (const GstSegment * segment,
    GstFormat format, guint64 running_time, guint64 * position)
{
  gint res;
  guint64 start, stop, base;
  gdouble abs_rate;

  if (G_UNLIKELY (running_time == -1)) {
    *position = -1;
    return 0;
  }

  g_return_val_if_fail (segment != NULL, 0);
  g_return_val_if_fail (segment->format == format, 0);

  base = segment->base;

  abs_rate = ABS (segment->rate);

  start = segment->start;
  stop = segment->stop;

  if (G_LIKELY (segment->rate > 0.0)) {
    /* start by subtracting the base time */
    if (G_LIKELY (running_time >= base)) {
      *position = running_time - base;
      /* move into the segment at the right rate */
      if (G_UNLIKELY (abs_rate != 1.0))
        *position = ceil (*position * abs_rate);
      /* bring to corrected position in segment */
      *position += start + segment->offset;
      res = 1;
    } else {
      *position = base - running_time;
      if (G_UNLIKELY (abs_rate != 1.0))
        *position = ceil (*position * abs_rate);
      if (start + segment->offset >= *position) {
        /* The TS is before the segment, but the result is >= 0 */
        *position = start + segment->offset - *position;
        res = 1;
      } else {
        /* The TS is before the segment, and the result is < 0
         * so negate the return result */
        *position = *position - (start + segment->offset);
        res = -1;
      }
    }
  } else {
    if (G_LIKELY (running_time >= base)) {
      *position = running_time - base;
      if (G_UNLIKELY (abs_rate != 1.0))
        *position = ceil (*position * abs_rate);
      if (G_UNLIKELY (stop < *position + segment->offset)) {
        *position += segment->offset - stop;
        res = -1;
      } else {
        *position = stop - *position - segment->offset;
        res = 1;
      }
    } else {
      /* This case is tricky. Requested running time precedes the
       * segment base, so in a reversed segment where rate < 0, that
       * means it's before the alignment point of (stop - offset).
       * Before = always bigger than (stop-offset), which is usually +ve,
       * but could be -ve is offset is big enough. -ve position implies
       * that the offset has clipped away the entire segment anyway */
      *position = base - running_time;
      if (G_UNLIKELY (abs_rate != 1.0))
        *position = ceil (*position * abs_rate);

      if (G_LIKELY (stop + *position >= segment->offset)) {
        *position = stop + *position - segment->offset;
        res = 1;
      } else {
        /* Requested position is still negative because offset is big,
         * so negate the result */
        *position = segment->offset - *position - stop;
        res = -1;
      }
    }
  }
  return res;
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
 * Deprecated: Use gst_segment_position_from_running_time() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
guint64
gst_segment_to_position (const GstSegment * segment, GstFormat format,
    guint64 running_time)
{
  return gst_segment_position_from_running_time (segment, format, running_time);
}
#endif

/**
 * gst_segment_set_running_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @running_time: the running_time in the segment
 *
 * Adjust the start/stop and base values of @segment such that the next valid
 * buffer will be one with @running_time.
 *
 * Returns: %TRUE if the segment could be updated successfully. If %FALSE is
 * returned, @running_time is -1 or not in @segment.
 */
gboolean
gst_segment_set_running_time (GstSegment * segment, GstFormat format,
    guint64 running_time)
{
  guint64 position;
  guint64 start, stop;

  /* start by bringing the running_time into the segment position */
  position =
      gst_segment_position_from_running_time (segment, format, running_time);

  /* we must have a valid position now */
  if (G_UNLIKELY (position == -1))
    return FALSE;

  start = segment->start;
  stop = segment->stop;

  if (G_LIKELY (segment->rate > 0.0)) {
    /* update the start and time values */
    start = position;
  } else {
    /* reverse, update stop */
    stop = position;
  }
  /* and base time is exactly the running time */
  segment->time = gst_segment_to_stream_time (segment, format, start);
  segment->start = start;
  segment->stop = stop;
  segment->base = running_time;

  return TRUE;
}

/**
 * gst_segment_offset_running_time:
 * @segment: a #GstSegment structure.
 * @format: the format of the segment.
 * @offset: the offset to apply in the segment
 *
 * Adjust the values in @segment so that @offset is applied to all
 * future running-time calculations.
 *
 * Since: 1.2.3
 *
 * Returns: %TRUE if the segment could be updated successfully. If %FALSE is
 * returned, @offset is not in @segment.
 */
gboolean
gst_segment_offset_running_time (GstSegment * segment, GstFormat format,
    gint64 offset)
{
  g_return_val_if_fail (segment != NULL, FALSE);
  g_return_val_if_fail (segment->format == format, FALSE);

  if (offset == 0)
    return TRUE;

  if (offset > 0) {
    /* positive offset, we can simply apply to the base time */
    segment->base += offset;
  } else {
    offset = -offset;
    /* negative offset, first try to subtract from base */
    if (segment->base > offset) {
      segment->base -= offset;
    } else {
      guint64 position;

      /* subtract all from segment.base, remainder in offset */
      offset -= segment->base;
      segment->base = 0;
      position =
          gst_segment_position_from_running_time (segment, format, offset);
      if (position == -1)
        return FALSE;

      segment->offset = position - segment->start;
    }
  }
  return TRUE;
}

/**
 * gst_segment_is_equal:
 * @s0: a #GstSegment structure.
 * @s1: a #GstSegment structure.
 *
 * Checks for two segments being equal. Equality here is defined
 * as perfect equality, including floating point values.
 *
 * Since: 1.6
 *
 * Returns: %TRUE if the segments are equal, %FALSE otherwise.
 */
gboolean
gst_segment_is_equal (const GstSegment * s0, const GstSegment * s1)
{
  if (s0->flags != s1->flags)
    return FALSE;
  if (s0->rate != s1->rate)
    return FALSE;
  if (s0->applied_rate != s1->applied_rate)
    return FALSE;
  if (s0->format != s1->format)
    return FALSE;
  if (s0->base != s1->base)
    return FALSE;
  if (s0->offset != s1->offset)
    return FALSE;
  if (s0->start != s1->start)
    return FALSE;
  if (s0->stop != s1->stop)
    return FALSE;
  if (s0->time != s1->time)
    return FALSE;
  if (s0->position != s1->position)
    return FALSE;
  if (s0->duration != s1->duration)
    return FALSE;
  return TRUE;
}
