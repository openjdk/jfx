/* GStreamer
 * Copyright (C) 2011 Wim Taymans <wim.taymans@gmail.com>
 *
 * gstsample.c: media sample
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
 * SECTION:gstsample
 * @title: GstSample
 * @short_description: A media sample
 * @see_also: #GstBuffer, #GstCaps, #GstSegment
 *
 * A #GstSample is a small object containing data, a type, timing and
 * extra arbitrary information.
 */
#define GST_DISABLE_MINIOBJECT_INLINE_FUNCTIONS
#include "gst_private.h"

#include "gstsample.h"

GST_DEBUG_CATEGORY_STATIC (gst_sample_debug);
#define GST_CAT_DEFAULT gst_sample_debug

struct _GstSample
{
  GstMiniObject mini_object;

  GstBuffer *buffer;
  GstCaps *caps;
  GstSegment segment;
  GstStructure *info;
  GstBufferList *buffer_list;
};

GType _gst_sample_type = 0;

GST_DEFINE_MINI_OBJECT_TYPE (GstSample, gst_sample);

void
_priv_gst_sample_initialize (void)
{
  _gst_sample_type = gst_sample_get_type ();

  GST_DEBUG_CATEGORY_INIT (gst_sample_debug, "sample", 0, "GstSample debug");
}

static GstSample *
_gst_sample_copy (GstSample * sample)
{
  GstSample *copy;

  copy = gst_sample_new (sample->buffer, sample->caps, &sample->segment,
      (sample->info) ? gst_structure_copy (sample->info) : NULL);

  if (sample->buffer_list) {
    copy->buffer_list = gst_buffer_list_ref (sample->buffer_list);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (copy->buffer_list),
        GST_MINI_OBJECT_CAST (copy));
  }

  return copy;
}

static void
_gst_sample_free (GstSample * sample)
{
  GST_LOG ("free %p", sample);

  if (sample->buffer) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (sample->buffer),
        GST_MINI_OBJECT_CAST (sample));
    gst_buffer_unref (sample->buffer);
  }

  if (sample->caps) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (sample->caps),
        GST_MINI_OBJECT_CAST (sample));
    gst_caps_unref (sample->caps);
  }

  if (sample->info) {
    gst_structure_set_parent_refcount (sample->info, NULL);
    gst_structure_free (sample->info);
  }
  if (sample->buffer_list) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (sample->buffer_list),
        GST_MINI_OBJECT_CAST (sample));
    gst_buffer_list_unref (sample->buffer_list);
  }
#ifdef USE_POISONING
  memset (sample, 0xff, sizeof (GstSample));
#endif

  g_slice_free1 (sizeof (GstSample), sample);
}

/**
 * gst_sample_new:
 * @buffer: (transfer none) (allow-none): a #GstBuffer, or %NULL
 * @caps: (transfer none) (allow-none): a #GstCaps, or %NULL
 * @segment: (transfer none) (allow-none): a #GstSegment, or %NULL
 * @info: (transfer full) (allow-none): a #GstStructure, or %NULL
 *
 * Create a new #GstSample with the provided details.
 *
 * Free-function: gst_sample_unref
 *
 * Returns: (transfer full): the new #GstSample. gst_sample_unref()
 *     after usage.
 */
GstSample *
gst_sample_new (GstBuffer * buffer, GstCaps * caps, const GstSegment * segment,
    GstStructure * info)
{
  GstSample *sample;

  sample = g_slice_new0 (GstSample);

  GST_LOG ("new %p", sample);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (sample), 0, _gst_sample_type,
      (GstMiniObjectCopyFunction) _gst_sample_copy, NULL,
      (GstMiniObjectFreeFunction) _gst_sample_free);

  if (buffer) {
    sample->buffer = gst_buffer_ref (buffer);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (sample->buffer),
        GST_MINI_OBJECT_CAST (sample));
  }

  if (caps) {
    sample->caps = gst_caps_ref (caps);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (sample->caps),
        GST_MINI_OBJECT_CAST (sample));
  }

  /* FIXME 2.0: initialize with GST_FORMAT_UNDEFINED by default */
  if (segment)
    gst_segment_copy_into (segment, &sample->segment);
  else
    gst_segment_init (&sample->segment, GST_FORMAT_TIME);

  if (info) {
    if (!gst_structure_set_parent_refcount (info,
            &sample->mini_object.refcount))
      goto had_parent;

    sample->info = info;
  }
  return sample;

  /* ERRORS */
had_parent:
  {
    gst_sample_unref (sample);
    g_warning ("structure is already owned by another object");
    return NULL;
  }
}

/**
 * gst_sample_get_buffer:
 * @sample: a #GstSample
 *
 * Get the buffer associated with @sample
 *
 * Returns: (transfer none) (nullable): the buffer of @sample or %NULL
 *  when there is no buffer. The buffer remains valid as long as
 *  @sample is valid.  If you need to hold on to it for longer than
 *  that, take a ref to the buffer with gst_buffer_ref().
 */
GstBuffer *
gst_sample_get_buffer (GstSample * sample)
{
  g_return_val_if_fail (GST_IS_SAMPLE (sample), NULL);

  return sample->buffer;
}

/**
 * gst_sample_get_caps:
 * @sample: a #GstSample
 *
 * Get the caps associated with @sample
 *
 * Returns: (transfer none) (nullable): the caps of @sample or %NULL
 *  when there is no caps. The caps remain valid as long as @sample is
 *  valid.  If you need to hold on to the caps for longer than that,
 *  take a ref to the caps with gst_caps_ref().
 */
GstCaps *
gst_sample_get_caps (GstSample * sample)
{
  g_return_val_if_fail (GST_IS_SAMPLE (sample), NULL);

  return sample->caps;
}

/**
 * gst_sample_get_segment:
 * @sample: a #GstSample
 *
 * Get the segment associated with @sample
 *
 * Returns: (transfer none): the segment of @sample.
 *  The segment remains valid as long as @sample is valid.
 */
GstSegment *
gst_sample_get_segment (GstSample * sample)
{
  g_return_val_if_fail (GST_IS_SAMPLE (sample), NULL);

  return &sample->segment;
}

/**
 * gst_sample_get_info:
 * @sample: a #GstSample
 *
 * Get extra information associated with @sample.
 *
 * Returns: (transfer none) (nullable): the extra info of @sample.
 *  The info remains valid as long as @sample is valid.
 */
const GstStructure *
gst_sample_get_info (GstSample * sample)
{
  g_return_val_if_fail (GST_IS_SAMPLE (sample), NULL);

  return sample->info;
}

/**
 * gst_sample_get_buffer_list:
 * @sample: a #GstSample
 *
 * Get the buffer list associated with @sample
 *
 * Returns: (transfer none) (nullable): the buffer list of @sample or %NULL
 *  when there is no buffer list. The buffer list remains valid as long as
 *  @sample is valid.  If you need to hold on to it for longer than
 *  that, take a ref to the buffer list with gst_mini_object_ref ().
 *
 * Since: 1.6
 */
GstBufferList *
gst_sample_get_buffer_list (GstSample * sample)
{
  g_return_val_if_fail (GST_IS_SAMPLE (sample), NULL);

  return sample->buffer_list;
}

/**
 * gst_sample_set_buffer_list:
 * @sample: a #GstSample
 * @buffer_list: a #GstBufferList
 *
 * Set the buffer list associated with @sample. @sample must be writable.
 *
 * Since: 1.6
 */
void
gst_sample_set_buffer_list (GstSample * sample, GstBufferList * buffer_list)
{
  GstBufferList *old = NULL;
  g_return_if_fail (GST_IS_SAMPLE (sample));
  g_return_if_fail (gst_sample_is_writable (sample));

  old = sample->buffer_list;

  if (old == buffer_list)
    return;

  if (buffer_list) {
    sample->buffer_list = gst_buffer_list_ref (buffer_list);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (sample->buffer_list),
        GST_MINI_OBJECT_CAST (sample));
  } else {
    sample->buffer_list = NULL;
  }

  if (old) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (old),
        GST_MINI_OBJECT_CAST (sample));
    gst_buffer_list_unref (old);
  }
}

/**
 * gst_sample_set_buffer:
 * @sample: A #GstSample
 * @buffer: (transfer none): A #GstBuffer
 *
 * Set the buffer associated with @sample. @sample must be writable.
 *
 * Since: 1.16
 */
void
gst_sample_set_buffer (GstSample * sample, GstBuffer * buffer)
{
  GstBuffer *old = NULL;

  g_return_if_fail (GST_IS_SAMPLE (sample));
  g_return_if_fail (gst_sample_is_writable (sample));

  old = sample->buffer;

  if (old == buffer)
    return;

  if (buffer) {
    sample->buffer = gst_buffer_ref (buffer);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (sample->buffer),
        GST_MINI_OBJECT_CAST (sample));
  } else {
    sample->buffer = NULL;
  }

  if (old) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (old),
        GST_MINI_OBJECT_CAST (sample));
    gst_buffer_unref (old);
  }
}

/**
 * gst_sample_set_caps:
 * @sample: A #GstSample
 * @caps: (transfer none): A #GstCaps
 *
 * Set the caps associated with @sample. @sample must be writable.
 *
 * Since: 1.16
 */
void
gst_sample_set_caps (GstSample * sample, GstCaps * caps)
{
  GstCaps *old = NULL;

  g_return_if_fail (GST_IS_SAMPLE (sample));
  g_return_if_fail (gst_sample_is_writable (sample));

  old = sample->caps;

  if (old == caps)
    return;

  if (caps) {
    sample->caps = gst_caps_ref (caps);
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (sample->caps),
        GST_MINI_OBJECT_CAST (sample));
  } else {
    sample->caps = NULL;
  }

  if (old) {
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (old),
        GST_MINI_OBJECT_CAST (sample));
    gst_caps_unref (old);
  }
}

/**
 * gst_sample_set_segment:
 * @sample: A #GstSample
 * @segment: (transfer none): A #GstSegment
 *
 * Set the segment associated with @sample. @sample must be writable.
 *
 * Since: 1.16
 */
void
gst_sample_set_segment (GstSample * sample, const GstSegment * segment)
{
  g_return_if_fail (GST_IS_SAMPLE (sample));
  g_return_if_fail (gst_sample_is_writable (sample));

  /* FIXME 2.0: initialize with GST_FORMAT_UNDEFINED by default */
  if (segment)
    gst_segment_copy_into (segment, &sample->segment);
  else
    gst_segment_init (&sample->segment, GST_FORMAT_TIME);
}

/**
 * gst_sample_set_info:
 * @sample: A #GstSample
 * @info: (transfer full): A #GstStructure
 *
 * Set the info structure associated with @sample. @sample must be writable,
 * and @info must not have a parent set already.
 *
 * Since: 1.16
 */
gboolean
gst_sample_set_info (GstSample * sample, GstStructure * info)
{
  g_return_val_if_fail (GST_IS_SAMPLE (sample), FALSE);
  g_return_val_if_fail (gst_sample_is_writable (sample), FALSE);

  if (info) {
    if (!gst_structure_set_parent_refcount (info,
            &sample->mini_object.refcount))
      goto had_parent;
  }

  if (sample->info) {
    gst_structure_set_parent_refcount (sample->info, NULL);
    gst_structure_free (sample->info);
  }

  sample->info = info;

  return TRUE;

had_parent:
  g_warning ("structure is already owned by another object");
  return FALSE;
}

/**
 * gst_sample_ref: (skip)
 * @sample: a #GstSample
 *
 * Increases the refcount of the given sample by one.
 *
 * Returns: (transfer full): @sample
 */
GstSample *
gst_sample_ref (GstSample * sample)
{
  return GST_SAMPLE_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (sample)));
}

/**
 * gst_sample_unref: (skip)
 * @sample: (transfer full): a #GstSample
 *
 * Decreases the refcount of the sample. If the refcount reaches 0, the
 * sample will be freed.
 */
void
gst_sample_unref (GstSample * sample)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (sample));
}

/**
 * gst_sample_copy: (skip)
 * @buf: a #GstSample.
 *
 * Create a copy of the given sample. This will also make a newly allocated
 * copy of the data the source sample contains.
 *
 * Returns: (transfer full): a new copy of @buf.
 *
 * Since: 1.2
 */
GstSample *
gst_sample_copy (const GstSample * buf)
{
  return
      GST_SAMPLE_CAST (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST (buf)));
}
