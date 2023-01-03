/* GStreamer
 *
 * Copyright (C) 2015 Centricular Ltd
 *  @author: Edward Hervey <edward@centricular.com>
 *  @author: Jan Schmidt <jan@centricular.com>
 *
 * gststreams.c: GstStreamCollection object and methods
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
 * SECTION:gststreamcollection
 * @title: GstStreamCollection
 * @short_description: Base class for collection of streams
 *
 * Since: 1.10
 */

#include "gst_private.h"

#include "gstenumtypes.h"
#include "gstevent.h"
#include "gststreamcollection.h"

GST_DEBUG_CATEGORY_STATIC (stream_collection_debug);
#define GST_CAT_DEFAULT stream_collection_debug

struct _GstStreamCollectionPrivate
{
  /* Maybe switch this to a GArray if performance is
   * ever an issue? */
  GQueue streams;
};

/* stream signals and properties */
enum
{
  SIG_STREAM_NOTIFY,
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_UPSTREAM_ID,
  PROP_LAST
};

static guint gst_stream_collection_signals[LAST_SIGNAL] = { 0 };

static void gst_stream_collection_dispose (GObject * object);

static void gst_stream_collection_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_stream_collection_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static void
proxy_stream_notify_cb (GstStream * stream, GParamSpec * pspec,
    GstStreamCollection * collection);

#define _do_init        \
{ \
  GST_DEBUG_CATEGORY_INIT (stream_collection_debug, "streamcollection", GST_DEBUG_BOLD, \
      "debugging info for the stream collection objects"); \
  \
}

#define gst_stream_collection_parent_class parent_class
G_DEFINE_TYPE_WITH_CODE (GstStreamCollection, gst_stream_collection,
    GST_TYPE_OBJECT, G_ADD_PRIVATE (GstStreamCollection) _do_init);

static void
gst_stream_collection_class_init (GstStreamCollectionClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = (GObjectClass *) klass;

  gobject_class->set_property = gst_stream_collection_set_property;
  gobject_class->get_property = gst_stream_collection_get_property;

  /**
   * GstStream:upstream-id:
   *
   * stream-id
   */
  g_object_class_install_property (gobject_class, PROP_UPSTREAM_ID,
      g_param_spec_string ("upstream-id", "Upstream ID",
          "The stream ID of the parent stream",
          NULL,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  /**
   * GstStream::stream-notify:
   * @collection: a #GstStreamCollection
   * @prop_stream: the #GstStream that originated the signal
   * @prop: the property that changed
   *
   * The stream notify signal is used to be notified of property changes to
   * streams within the collection.
   */
  gst_stream_collection_signals[SIG_STREAM_NOTIFY] =
      g_signal_new ("stream-notify", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_FIRST | G_SIGNAL_NO_RECURSE | G_SIGNAL_DETAILED |
      G_SIGNAL_NO_HOOKS, G_STRUCT_OFFSET (GstStreamCollectionClass,
          stream_notify), NULL, NULL, NULL, G_TYPE_NONE,
      2, GST_TYPE_STREAM, G_TYPE_PARAM);

  gobject_class->dispose = gst_stream_collection_dispose;
}

static void
gst_stream_collection_init (GstStreamCollection * collection)
{
  collection->priv = gst_stream_collection_get_instance_private (collection);
  g_queue_init (&collection->priv->streams);
}

static void
release_gst_stream (GstStream * stream, GstStreamCollection * collection)
{
  g_signal_handlers_disconnect_by_func (stream,
      proxy_stream_notify_cb, collection);
  gst_object_unref (stream);
}

static void
gst_stream_collection_dispose (GObject * object)
{
  GstStreamCollection *collection = GST_STREAM_COLLECTION_CAST (object);

  if (collection->upstream_id) {
    g_free (collection->upstream_id);
    collection->upstream_id = NULL;
  }

  g_queue_foreach (&collection->priv->streams,
      (GFunc) release_gst_stream, collection);
  g_queue_clear (&collection->priv->streams);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

/**
 * gst_stream_collection_new:
 * @upstream_id: (allow-none): The stream id of the parent stream
 *
 * Create a new #GstStreamCollection.
 *
 * Returns: (transfer full): The new #GstStreamCollection.
 *
 * Since: 1.10
 */
GstStreamCollection *
gst_stream_collection_new (const gchar * upstream_id)
{
  GstStreamCollection *collection;

  collection =
      g_object_new (GST_TYPE_STREAM_COLLECTION, "upstream-id", upstream_id,
      NULL);

  /* Clear floating flag */
  g_object_ref_sink (collection);

  return collection;
}

static void
gst_stream_collection_set_upstream_id (GstStreamCollection * collection,
    const gchar * upstream_id)
{
  g_return_if_fail (collection->upstream_id == NULL);

  /* Upstream ID should only be set once on construction, but let's
   * not leak in case someone does something silly */
  if (collection->upstream_id)
    g_free (collection->upstream_id);

  if (upstream_id)
    collection->upstream_id = g_strdup (upstream_id);
}

/**
 * gst_stream_collection_get_upstream_id:
 * @collection: a #GstStreamCollection
 *
 * Returns the upstream id of the @collection.
 *
 * Returns: (transfer none) (nullable): The upstream id
 *
 * Since: 1.10
 */
const gchar *
gst_stream_collection_get_upstream_id (GstStreamCollection * collection)
{
  const gchar *res;

  res = collection->upstream_id;

  return res;
}

static void
gst_stream_collection_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstStreamCollection *collection;

  collection = GST_STREAM_COLLECTION_CAST (object);

  switch (prop_id) {
    case PROP_UPSTREAM_ID:
      gst_stream_collection_set_upstream_id (collection,
          g_value_get_string (value));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_stream_collection_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstStreamCollection *collection;

  collection = GST_STREAM_COLLECTION_CAST (object);

  switch (prop_id) {
    case PROP_UPSTREAM_ID:
      g_value_set_string (value,
          gst_stream_collection_get_upstream_id (collection));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
proxy_stream_notify_cb (GstStream * stream, GParamSpec * pspec,
    GstStreamCollection * collection)
{
  GST_DEBUG_OBJECT (collection, "Stream %" GST_PTR_FORMAT " updated %s",
      stream, pspec->name);
  g_signal_emit (collection, gst_stream_collection_signals[SIG_STREAM_NOTIFY],
      g_quark_from_string (pspec->name), stream, pspec);
}

/**
 * gst_stream_collection_add_stream:
 * @collection: a #GstStreamCollection
 * @stream: (transfer full): the #GstStream to add
 *
 * Add the given @stream to the @collection.
 *
 * Returns: %TRUE if the @stream was properly added, else %FALSE
 *
 * Since: 1.10
 */
gboolean
gst_stream_collection_add_stream (GstStreamCollection * collection,
    GstStream * stream)
{
  g_return_val_if_fail (GST_IS_STREAM_COLLECTION (collection), FALSE);
  g_return_val_if_fail (GST_IS_STREAM (stream), FALSE);

  GST_DEBUG_OBJECT (collection, "Adding stream %" GST_PTR_FORMAT, stream);

  g_queue_push_tail (&collection->priv->streams, stream);
  g_signal_connect (stream, "notify", (GCallback) proxy_stream_notify_cb,
      collection);

  return TRUE;
}

/**
 * gst_stream_collection_get_size:
 * @collection: a #GstStreamCollection
 *
 * Get the number of streams this collection contains
 *
 * Returns: The number of streams that @collection contains
 *
 * Since: 1.10
 */
guint
gst_stream_collection_get_size (GstStreamCollection * collection)
{
  g_return_val_if_fail (GST_IS_STREAM_COLLECTION (collection), 0);

  return g_queue_get_length (&collection->priv->streams);
}

/**
 * gst_stream_collection_get_stream:
 * @collection: a #GstStreamCollection
 * @index: Index of the stream to retrieve
 *
 * Retrieve the #GstStream with index @index from the collection.
 *
 * The caller should not modify the returned #GstStream
 *
 * Returns: (transfer none) (nullable): A #GstStream
 *
 * Since: 1.10
 */
GstStream *
gst_stream_collection_get_stream (GstStreamCollection * collection, guint index)
{
  g_return_val_if_fail (GST_IS_STREAM_COLLECTION (collection), NULL);

  return g_queue_peek_nth (&collection->priv->streams, index);
}
