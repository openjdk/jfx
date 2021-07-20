/* GStreamer
 *
 * Copyright (C) 2015 Centricular Ltd
 *  @author: Edward Hervey <edward@centricular.com>
 *  @author: Jan Schmidt <jan@centricular.com>
 *
 * gststreams.c: GstStream and GstStreamCollection object and methods
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
 *
 * MT safe.
 */

/**
 * SECTION:gststreams
 * @title: GstStreams
 * @short_description: Base class for stream objects
 *
 * A #GstStream is a high-level object defining a stream of data which is, or
 * can be, present in a #GstPipeline.
 *
 * It is defined by a unique identifier, a "Stream ID". A #GstStream does not
 * automatically imply the stream is present within a pipeline or element.
 *
 * Any element that can introduce new streams in a pipeline should create the
 * appropriate #GstStream object, and can convey that object via the
 * %GST_EVENT_STREAM_START event and/or the #GstStreamCollection.
 *
 * Elements that do not modify the nature of the stream can add extra information
 * on it (such as enrich the #GstCaps, or #GstTagList). This is typically done
 * by parsing elements.
 *
 * Since: 1.10
 */

#include "gst_private.h"

#include "gstenumtypes.h"
#include "gstevent.h"
#include "gststreams.h"

GST_DEBUG_CATEGORY_STATIC (streams_debug);
#define GST_CAT_DEFAULT streams_debug

struct _GstStreamPrivate
{
  GstStreamFlags flags;
  GstStreamType type;
  GstTagList *tags;
  GstCaps *caps;
};

/* stream signals and properties */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_STREAM_ID,
  PROP_STREAM_FLAGS,
  PROP_STREAM_TYPE,
  PROP_TAGS,
  PROP_CAPS,
  PROP_LAST
};

static GParamSpec *gst_stream_pspecs[PROP_LAST] = { 0 };

#if 0
static guint gst_stream_signals[LAST_SIGNAL] = { 0 };
#endif

static void gst_stream_finalize (GObject * object);

static void gst_stream_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_stream_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

#define _do_init        \
{ \
  GST_DEBUG_CATEGORY_INIT (streams_debug, "streams", GST_DEBUG_BOLD, \
      "debugging info for the stream and stream collection objects"); \
  \
}

#define gst_stream_parent_class parent_class
G_DEFINE_TYPE_WITH_CODE (GstStream, gst_stream, GST_TYPE_OBJECT,
    G_ADD_PRIVATE (GstStream) _do_init);

static void
gst_stream_class_init (GstStreamClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = (GObjectClass *) klass;

  gobject_class->set_property = gst_stream_set_property;
  gobject_class->get_property = gst_stream_get_property;

  /**
   * GstStream:stream-id:
   *
   * The unique identifier of the #GstStream. Can only be set at construction
   * time.
   */
  g_object_class_install_property (gobject_class, PROP_STREAM_ID,
      g_param_spec_string ("stream-id", "Stream ID",
          "The stream ID of the stream",
          NULL,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS));

  /**
   * GstStream:flags:
   *
   * The #GstStreamFlags of the #GstStream. Can only be set at construction time.
   **/
  gst_stream_pspecs[PROP_STREAM_FLAGS] =
      g_param_spec_flags ("stream-flags", "Stream Flags", "The stream flags",
      GST_TYPE_STREAM_FLAGS, GST_STREAM_FLAG_NONE,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PROP_STREAM_FLAGS,
      gst_stream_pspecs[PROP_STREAM_FLAGS]);

  /**
   * GstStream:stream-type:
   *
   * The #GstStreamType of the #GstStream. Can only be set at construction time.
   **/
#ifndef GSTREAMER_LITE
  gst_stream_pspecs[PROP_STREAM_TYPE] =
      g_param_spec_flags ("stream-type", "Stream Type", "The type of stream",
      GST_TYPE_STREAM_TYPE, GST_STREAM_TYPE_UNKNOWN,
      G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PROP_STREAM_TYPE,
      gst_stream_pspecs[PROP_STREAM_TYPE]);
#endif // GSTREAMER_LITE

  /**
   * GstStream:caps:
   *
   * The #GstCaps of the #GstStream.
   **/
  gst_stream_pspecs[PROP_CAPS] =
      g_param_spec_boxed ("caps", "Caps", "The caps of the stream",
      GST_TYPE_CAPS, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PROP_CAPS,
      gst_stream_pspecs[PROP_CAPS]);

  /**
   * GstStream:tags:
   *
   * The #GstTagList of the #GstStream.
   **/
  gst_stream_pspecs[PROP_TAGS] =
      g_param_spec_boxed ("tags", "Tags", "The tags of the stream",
      GST_TYPE_TAG_LIST, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS);
  g_object_class_install_property (gobject_class, PROP_TAGS,
      gst_stream_pspecs[PROP_TAGS]);

  gobject_class->finalize = gst_stream_finalize;
}

static void
gst_stream_init (GstStream * stream)
{
  stream->priv = gst_stream_get_instance_private (stream);
  stream->priv->type = GST_STREAM_TYPE_UNKNOWN;
}

static void
gst_stream_finalize (GObject * object)
{
  GstStream *stream = GST_STREAM_CAST (object);

  gst_mini_object_replace ((GstMiniObject **) & stream->priv->tags,
      (GstMiniObject *) NULL);
  gst_caps_replace (&stream->priv->caps, NULL);
  g_free ((gchar *) stream->stream_id);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

/**
 * gst_stream_new:
 * @stream_id: (allow-none): the id for the new stream. If %NULL,
 * a new one will be automatically generated
 * @caps: (allow-none) (transfer none): the #GstCaps of the stream
 * @type: the #GstStreamType of the stream
 * @flags: the #GstStreamFlags of the stream
 *
 * Create a new #GstStream for the given @stream_id, @caps, @type
 * and @flags
 *
 * Returns: (transfer full): The new #GstStream
 *
 * Since: 1.10
 */
GstStream *
gst_stream_new (const gchar * stream_id, GstCaps * caps, GstStreamType type,
    GstStreamFlags flags)
{
  GstStream *stream;

  stream = g_object_new (GST_TYPE_STREAM, "stream-id", stream_id, "caps", caps,
      "stream-type", type, "stream-flags", flags, NULL);

  /* Clear floating flag */
  gst_object_ref_sink (stream);

  return stream;
}

static void
gst_stream_set_stream_id (GstStream * stream, const gchar * stream_id)
{
  g_return_if_fail (GST_IS_STREAM (stream));

  GST_OBJECT_LOCK (stream);
  g_assert (stream->stream_id == NULL);
  if (stream_id) {
    stream->stream_id = g_strdup (stream_id);
  } else {
    /* Create a random stream_id if NULL */
    stream->stream_id =
        g_strdup_printf ("%08x%08x%08x%08x", g_random_int (), g_random_int (),
        g_random_int (), g_random_int ());
  }

  GST_OBJECT_UNLOCK (stream);

  if (!stream_id)
    GST_FIXME_OBJECT (stream, "Created random stream-id, consider "
        "implementing a deterministic way of creating a stream-id");
}

/**
 * gst_stream_get_stream_id:
 * @stream: a #GstStream
 *
 * Returns the stream ID of @stream.
 *
 * Returns: (transfer none) (nullable): the stream ID of @stream. Only valid
 * during the lifetime of @stream.
 *
 * Since: 1.10
 */
const gchar *
gst_stream_get_stream_id (GstStream * stream)
{
  g_return_val_if_fail (GST_IS_STREAM (stream), NULL);

  return stream->stream_id;
}

/**
 * gst_stream_set_stream_flags:
 * @stream: a #GstStream
 * @flags: the flags to set on @stream
 *
 * Set the @flags for the @stream.
 *
 * Since: 1.10
 */
void
gst_stream_set_stream_flags (GstStream * stream, GstStreamFlags flags)
{
  g_return_if_fail (GST_IS_STREAM (stream));

  GST_OBJECT_LOCK (stream);
  stream->priv->flags = flags;
  GST_OBJECT_UNLOCK (stream);

  g_object_notify_by_pspec (G_OBJECT (stream),
      gst_stream_pspecs[PROP_STREAM_FLAGS]);
}

/**
 * gst_stream_get_stream_flags:
 * @stream: a #GstStream
 *
 * Retrieve the current stream flags for @stream
 *
 * Returns: The #GstStreamFlags for @stream
 *
 * Since: 1.10
 */
GstStreamFlags
gst_stream_get_stream_flags (GstStream * stream)
{
  GstStreamFlags res;

  g_return_val_if_fail (GST_IS_STREAM (stream), GST_STREAM_FLAG_NONE);

  GST_OBJECT_LOCK (stream);
  res = stream->priv->flags;
  GST_OBJECT_UNLOCK (stream);

  return res;
}

/**
 * gst_stream_set_stream_type:
 * @stream: a #GstStream
 * @stream_type: the type to set on @stream
 *
 * Set the stream type of @stream
 *
 * Since: 1.10
 */
void
gst_stream_set_stream_type (GstStream * stream, GstStreamType stream_type)
{
  g_return_if_fail (GST_IS_STREAM (stream));

  GST_OBJECT_LOCK (stream);
  stream->priv->type = stream_type;
  GST_OBJECT_UNLOCK (stream);

  g_object_notify_by_pspec (G_OBJECT (stream),
      gst_stream_pspecs[PROP_STREAM_TYPE]);
}

/**
 * gst_stream_get_stream_type:
 * @stream: a #GstStream
 *
 * Retrieve the stream type for @stream
 *
 * Returns: The #GstStreamType for @stream
 *
 * Since: 1.10
 */
GstStreamType
gst_stream_get_stream_type (GstStream * stream)
{
  GstStreamType res;

  g_return_val_if_fail (GST_IS_STREAM (stream), GST_STREAM_TYPE_UNKNOWN);

  GST_OBJECT_LOCK (stream);
  res = stream->priv->type;
  GST_OBJECT_UNLOCK (stream);

  return res;
}

/**
 * gst_stream_set_tags:
 * @stream: a #GstStream
 * @tags: (transfer none) (allow-none): a #GstTagList
 *
 * Set the tags for the #GstStream
 *
 * Since: 1.10
 */
void
gst_stream_set_tags (GstStream * stream, GstTagList * tags)
{
  gboolean notify = FALSE;

  g_return_if_fail (GST_IS_STREAM (stream));

  GST_OBJECT_LOCK (stream);
  if (stream->priv->tags == NULL || tags == NULL
      || !gst_tag_list_is_equal (stream->priv->tags, tags)) {
    gst_mini_object_replace ((GstMiniObject **) & stream->priv->tags,
        (GstMiniObject *) tags);
    notify = TRUE;
  }
  GST_OBJECT_UNLOCK (stream);

  if (notify)
    g_object_notify_by_pspec (G_OBJECT (stream), gst_stream_pspecs[PROP_TAGS]);
}

/**
 * gst_stream_get_tags:
 * @stream: a #GstStream
 *
 * Retrieve the tags for @stream, if any
 *
 * Returns: (transfer full) (nullable): The #GstTagList for @stream
 *
 * Since: 1.10
 */
GstTagList *
gst_stream_get_tags (GstStream * stream)
{
  GstTagList *res = NULL;

  g_return_val_if_fail (GST_IS_STREAM (stream), NULL);

  GST_OBJECT_LOCK (stream);
  if (stream->priv->tags)
    res = gst_tag_list_ref (stream->priv->tags);
  GST_OBJECT_UNLOCK (stream);

  return res;
}

/**
 * gst_stream_set_caps:
 * @stream: a #GstStream
 * @caps: (transfer none) (allow-none): a #GstCaps
 *
 * Set the caps for the #GstStream
 *
 * Since: 1.10
 */
void
gst_stream_set_caps (GstStream * stream, GstCaps * caps)
{
  gboolean notify = FALSE;

  g_return_if_fail (GST_IS_STREAM (stream));

  GST_OBJECT_LOCK (stream);
  if (stream->priv->caps == NULL || (caps
          && !gst_caps_is_equal (stream->priv->caps, caps))) {
    gst_caps_replace (&stream->priv->caps, caps);
    notify = TRUE;
  }
  GST_OBJECT_UNLOCK (stream);

  if (notify)
    g_object_notify_by_pspec (G_OBJECT (stream), gst_stream_pspecs[PROP_CAPS]);
}


/**
 * gst_stream_get_caps:
 * @stream: a #GstStream
 *
 * Retrieve the caps for @stream, if any
 *
 * Returns: (transfer full) (nullable): The #GstCaps for @stream
 *
 * Since: 1.10
 */
GstCaps *
gst_stream_get_caps (GstStream * stream)
{
  GstCaps *res = NULL;

  g_return_val_if_fail (GST_IS_STREAM (stream), NULL);

  GST_OBJECT_LOCK (stream);
  if (stream->priv->caps)
    res = gst_caps_ref (stream->priv->caps);
  GST_OBJECT_UNLOCK (stream);

  return res;
}

static void
gst_stream_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstStream *stream;

  stream = GST_STREAM_CAST (object);

  switch (prop_id) {
    case PROP_STREAM_ID:
      gst_stream_set_stream_id (stream, g_value_get_string (value));
      break;
    case PROP_STREAM_FLAGS:
      GST_OBJECT_LOCK (stream);
      stream->priv->flags = g_value_get_flags (value);
      GST_OBJECT_UNLOCK (stream);
      break;
    case PROP_STREAM_TYPE:
      GST_OBJECT_LOCK (stream);
      stream->priv->type = g_value_get_flags (value);
      GST_OBJECT_UNLOCK (stream);
      break;
    case PROP_TAGS:
      GST_OBJECT_LOCK (stream);
      gst_mini_object_replace ((GstMiniObject **) & stream->priv->tags,
          (GstMiniObject *) g_value_get_boxed (value));
      GST_OBJECT_UNLOCK (stream);
      break;
    case PROP_CAPS:
      GST_OBJECT_LOCK (stream);
      gst_mini_object_replace ((GstMiniObject **) & stream->priv->caps,
          (GstMiniObject *) g_value_get_boxed (value));
      GST_OBJECT_UNLOCK (stream);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_stream_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstStream *stream;

  stream = GST_STREAM_CAST (object);

  switch (prop_id) {
    case PROP_STREAM_ID:
      g_value_set_string (value, gst_stream_get_stream_id (stream));
      break;
    case PROP_STREAM_FLAGS:
      g_value_set_flags (value, gst_stream_get_stream_flags (stream));
      break;
    case PROP_STREAM_TYPE:
      g_value_set_flags (value, gst_stream_get_stream_type (stream));
      break;
    case PROP_TAGS:
      g_value_take_boxed (value, gst_stream_get_tags (stream));
      break;
    case PROP_CAPS:
      g_value_take_boxed (value, gst_stream_get_caps (stream));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/**
 * gst_stream_type_get_name:
 * @stype: a #GstStreamType
 *
 * Get a descriptive string for a given #GstStreamType
 *
 * Returns: A string describing the stream type
 *
 * Since: 1.10
 */
const gchar *
gst_stream_type_get_name (GstStreamType stype)
{
  /* FIXME : Make this more flexible */
  switch (stype) {
    case GST_STREAM_TYPE_UNKNOWN:
      return "unknown";
    case GST_STREAM_TYPE_AUDIO:
      return "audio";
    case GST_STREAM_TYPE_VIDEO:
      return "video";
    case GST_STREAM_TYPE_CONTAINER:
      return "container";
    case GST_STREAM_TYPE_TEXT:
      return "text";
    default:
      g_return_val_if_reached ("invalid");
      return "invalid";
  }
}
