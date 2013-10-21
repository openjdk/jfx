/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
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
 * SECTION:gstaudio
 * @short_description: Support library for audio elements
 *
 * This library contains some helper functions for audio elements.
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include "audio.h"
#include "audio-enumtypes.h"

#include <gst/gststructure.h>

/**
 * gst_audio_frame_byte_size:
 * @pad: the #GstPad to get the caps from
 *
 * Calculate byte size of an audio frame.
 *
 * Returns: the byte size, or 0 if there was an error
 */
int
gst_audio_frame_byte_size (GstPad * pad)
{
  /* FIXME: this should be moved closer to the gstreamer core
   * and be implemented for every mime type IMO
   */

  int width = 0;
  int channels = 0;
  const GstCaps *caps = NULL;
  GstStructure *structure;

  /* get caps of pad */
  caps = GST_PAD_CAPS (pad);

  if (caps == NULL) {
    /* ERROR: could not get caps of pad */
    g_warning ("gstaudio: could not get caps of pad %s:%s\n",
        GST_DEBUG_PAD_NAME (pad));
    return 0;
  }

  structure = gst_caps_get_structure (caps, 0);

  gst_structure_get_int (structure, "width", &width);
  gst_structure_get_int (structure, "channels", &channels);
  return (width / 8) * channels;
}

/**
 * gst_audio_frame_length:
 * @pad: the #GstPad to get the caps from
 * @buf: the #GstBuffer
 *
 * Calculate length of buffer in frames.
 *
 * Returns: 0 if there's an error, or the number of frames if everything's ok
 */
long
gst_audio_frame_length (GstPad * pad, GstBuffer * buf)
{
  /* FIXME: this should be moved closer to the gstreamer core
   * and be implemented for every mime type IMO
   */
  int frame_byte_size = 0;

  frame_byte_size = gst_audio_frame_byte_size (pad);
  if (frame_byte_size == 0)
    /* error */
    return 0;
  /* FIXME: this function assumes the buffer size to be a whole multiple
   *        of the frame byte size
   */
  return GST_BUFFER_SIZE (buf) / frame_byte_size;
}

/**
 * gst_audio_duration_from_pad_buffer:
 * @pad: the #GstPad to get the caps from
 * @buf: the #GstBuffer
 *
 * Calculate length in nanoseconds of audio buffer @buf based on capabilities of
 * @pad.
 *
 * Returns: the length.
 */
GstClockTime
gst_audio_duration_from_pad_buffer (GstPad * pad, GstBuffer * buf)
{
  long bytes = 0;
  int width = 0;
  int channels = 0;
  int rate = 0;

  GstClockTime length;

  const GstCaps *caps = NULL;
  GstStructure *structure;

  g_assert (GST_IS_BUFFER (buf));
  /* get caps of pad */
  caps = GST_PAD_CAPS (pad);
  if (caps == NULL) {
    /* ERROR: could not get caps of pad */
    g_warning ("gstaudio: could not get caps of pad %s:%s\n",
        GST_DEBUG_PAD_NAME (pad));
    length = GST_CLOCK_TIME_NONE;
  } else {
    structure = gst_caps_get_structure (caps, 0);
    bytes = GST_BUFFER_SIZE (buf);
    gst_structure_get_int (structure, "width", &width);
    gst_structure_get_int (structure, "channels", &channels);
    gst_structure_get_int (structure, "rate", &rate);

    g_assert (bytes != 0);
    g_assert (width != 0);
    g_assert (channels != 0);
    g_assert (rate != 0);
    length = (bytes * 8 * GST_SECOND) / (rate * channels * width);
  }
  return length;
}

/**
 * gst_audio_is_buffer_framed:
 * @pad: the #GstPad to get the caps from
 * @buf: the #GstBuffer
 *
 * Check if the buffer size is a whole multiple of the frame size.
 *
 * Returns: %TRUE if buffer size is multiple.
 */
gboolean
gst_audio_is_buffer_framed (GstPad * pad, GstBuffer * buf)
{
  if (GST_BUFFER_SIZE (buf) % gst_audio_frame_byte_size (pad) == 0)
    return TRUE;
  else
    return FALSE;
}

/* _getcaps helper functions
 * sets structure fields to default for audio type
 * flag determines which structure fields to set to default
 * keep these functions in sync with the templates in audio.h
 */

/* private helper function
 * sets a list on the structure
 * pass in structure, fieldname for the list, type of the list values,
 * number of list values, and each of the values, terminating with NULL
 */
static void
_gst_audio_structure_set_list (GstStructure * structure,
    const gchar * fieldname, GType type, int number, ...)
{
  va_list varargs;
  GValue value = { 0 };
  GArray *array;
  int j;

  g_return_if_fail (structure != NULL);

  g_value_init (&value, GST_TYPE_LIST);
  array = g_value_peek_pointer (&value);

  va_start (varargs, number);

  for (j = 0; j < number; ++j) {
    int i;
    gboolean b;

    GValue list_value = { 0 };

    switch (type) {
      case G_TYPE_INT:
        i = va_arg (varargs, int);

        g_value_init (&list_value, G_TYPE_INT);
        g_value_set_int (&list_value, i);
        break;
      case G_TYPE_BOOLEAN:
        b = va_arg (varargs, gboolean);
        g_value_init (&list_value, G_TYPE_BOOLEAN);
        g_value_set_boolean (&list_value, b);
        break;
      default:
        g_warning
            ("_gst_audio_structure_set_list: LIST of given type not implemented.");
    }
    g_array_append_val (array, list_value);

  }
  gst_structure_set_value (structure, fieldname, &value);
  va_end (varargs);
}

/**
 * gst_audio_structure_set_int:
 * @structure: a #GstStructure
 * @flag: a set of #GstAudioFieldFlag
 *
 * Do not use anymore.
 *
 * Deprecated: use gst_structure_set()
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
typedef enum
{
  GST_AUDIO_FIELD_RATE = (1 << 0),
  GST_AUDIO_FIELD_CHANNELS = (1 << 1),
  GST_AUDIO_FIELD_ENDIANNESS = (1 << 2),
  GST_AUDIO_FIELD_WIDTH = (1 << 3),
  GST_AUDIO_FIELD_DEPTH = (1 << 4),
  GST_AUDIO_FIELD_SIGNED = (1 << 5),
} GstAudioFieldFlag;
void
gst_audio_structure_set_int (GstStructure * structure, GstAudioFieldFlag flag);
#endif /* GST_DISABLE_DEPRECATED */

void
gst_audio_structure_set_int (GstStructure * structure, GstAudioFieldFlag flag)
{
  /* was added here:
   * http://webcvs.freedesktop.org/gstreamer/gst-plugins-base/gst-libs/gst/audio/audio.c?r1=1.16&r2=1.17
   * but it is not used
   */
  if (flag & GST_AUDIO_FIELD_RATE)
    gst_structure_set (structure, "rate", GST_TYPE_INT_RANGE, 1, G_MAXINT,
        NULL);
  if (flag & GST_AUDIO_FIELD_CHANNELS)
    gst_structure_set (structure, "channels", GST_TYPE_INT_RANGE, 1, G_MAXINT,
        NULL);
  if (flag & GST_AUDIO_FIELD_ENDIANNESS)
    _gst_audio_structure_set_list (structure, "endianness", G_TYPE_INT, 2,
        G_LITTLE_ENDIAN, G_BIG_ENDIAN, NULL);
  if (flag & GST_AUDIO_FIELD_WIDTH)
    _gst_audio_structure_set_list (structure, "width", G_TYPE_INT, 3, 8, 16, 32,
        NULL);
  if (flag & GST_AUDIO_FIELD_DEPTH)
    gst_structure_set (structure, "depth", GST_TYPE_INT_RANGE, 1, 32, NULL);
  if (flag & GST_AUDIO_FIELD_SIGNED)
    _gst_audio_structure_set_list (structure, "signed", G_TYPE_BOOLEAN, 2, TRUE,
        FALSE, NULL);
}
#endif /* GST_REMOVE_DEPRECATED */

/**
 * gst_audio_buffer_clip:
 * @buffer: The buffer to clip.
 * @segment: Segment in %GST_FORMAT_TIME or %GST_FORMAT_DEFAULT to which the buffer should be clipped.
 * @rate: sample rate.
 * @frame_size: size of one audio frame in bytes.
 *
 * Clip the the buffer to the given %GstSegment.
 *
 * After calling this function the caller does not own a reference to 
 * @buffer anymore.
 *
 * Returns: %NULL if the buffer is completely outside the configured segment,
 * otherwise the clipped buffer is returned.
 *
 * If the buffer has no timestamp, it is assumed to be inside the segment and
 * is not clipped 
 *
 * Since: 0.10.14
 */
GstBuffer *
gst_audio_buffer_clip (GstBuffer * buffer, GstSegment * segment, gint rate,
    gint frame_size)
{
  GstBuffer *ret;
  GstClockTime timestamp = GST_CLOCK_TIME_NONE, duration = GST_CLOCK_TIME_NONE;
  guint64 offset = GST_BUFFER_OFFSET_NONE, offset_end = GST_BUFFER_OFFSET_NONE;
  guint8 *data;
  guint size;

  gboolean change_duration = TRUE, change_offset = TRUE, change_offset_end =
      TRUE;

  g_return_val_if_fail (segment->format == GST_FORMAT_TIME ||
      segment->format == GST_FORMAT_DEFAULT, buffer);
  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);

  if (!GST_BUFFER_TIMESTAMP_IS_VALID (buffer))
    /* No timestamp - assume the buffer is completely in the segment */
    return buffer;

  /* Get copies of the buffer metadata to change later. 
   * Calculate the missing values for the calculations,
   * they won't be changed later though. */

  data = GST_BUFFER_DATA (buffer);
  size = GST_BUFFER_SIZE (buffer);

  timestamp = GST_BUFFER_TIMESTAMP (buffer);
  if (GST_BUFFER_DURATION_IS_VALID (buffer)) {
    duration = GST_BUFFER_DURATION (buffer);
  } else {
    change_duration = FALSE;
    duration = gst_util_uint64_scale (size / frame_size, GST_SECOND, rate);
  }

  if (GST_BUFFER_OFFSET_IS_VALID (buffer)) {
    offset = GST_BUFFER_OFFSET (buffer);
  } else {
    change_offset = FALSE;
    offset = 0;
  }

  if (GST_BUFFER_OFFSET_END_IS_VALID (buffer)) {
    offset_end = GST_BUFFER_OFFSET_END (buffer);
  } else {
    change_offset_end = FALSE;
    offset_end = offset + size / frame_size;
  }

  if (segment->format == GST_FORMAT_TIME) {
    /* Handle clipping for GST_FORMAT_TIME */

    gint64 start, stop, cstart, cstop, diff;

    start = timestamp;
    stop = timestamp + duration;

    if (gst_segment_clip (segment, GST_FORMAT_TIME,
            start, stop, &cstart, &cstop)) {

      diff = cstart - start;
      if (diff > 0) {
        timestamp = cstart;

        if (change_duration)
          duration -= diff;

        diff = gst_util_uint64_scale (diff, rate, GST_SECOND);
        if (change_offset)
          offset += diff;
        data += diff * frame_size;
        size -= diff * frame_size;
      }

      diff = stop - cstop;
      if (diff > 0) {
        /* duration is always valid if stop is valid */
        duration -= diff;

        diff = gst_util_uint64_scale (diff, rate, GST_SECOND);
        if (change_offset_end)
          offset_end -= diff;
        size -= diff * frame_size;
      }
    } else {
      gst_buffer_unref (buffer);
      return NULL;
    }
  } else {
    /* Handle clipping for GST_FORMAT_DEFAULT */
    gint64 start, stop, cstart, cstop, diff;

    g_return_val_if_fail (GST_BUFFER_OFFSET_IS_VALID (buffer), buffer);

    start = offset;
    stop = offset_end;

    if (gst_segment_clip (segment, GST_FORMAT_DEFAULT,
            start, stop, &cstart, &cstop)) {

      diff = cstart - start;
      if (diff > 0) {
        offset = cstart;

        timestamp = gst_util_uint64_scale (cstart, GST_SECOND, rate);

        if (change_duration)
          duration -= gst_util_uint64_scale (diff, GST_SECOND, rate);

        data += diff * frame_size;
        size -= diff * frame_size;
      }

      diff = stop - cstop;
      if (diff > 0) {
        offset_end = cstop;

        if (change_duration)
          duration -= gst_util_uint64_scale (diff, GST_SECOND, rate);

        size -= diff * frame_size;
      }
    } else {
      gst_buffer_unref (buffer);
      return NULL;
    }
  }

  /* Get a metadata writable buffer and apply all changes */
  ret = gst_buffer_make_metadata_writable (buffer);

  GST_BUFFER_TIMESTAMP (ret) = timestamp;
  GST_BUFFER_SIZE (ret) = size;
  GST_BUFFER_DATA (ret) = data;

  if (change_duration)
    GST_BUFFER_DURATION (ret) = duration;
  if (change_offset)
    GST_BUFFER_OFFSET (ret) = offset;
  if (change_offset_end)
    GST_BUFFER_OFFSET_END (ret) = offset_end;

  return ret;
}
