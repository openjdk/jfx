/* GStreamer
 * Copyright (C) 2011 Wim Taymans <wim.taymans@gmail.com>
 *
 * gstmeta.c: metadata operations
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
 * SECTION:gstmeta
 * @title: GstMeta
 * @short_description: Buffer metadata
 *
 * The #GstMeta structure should be included as the first member of a #GstBuffer
 * metadata structure. The structure defines the API of the metadata and should
 * be accessible to all elements using the metadata.
 *
 * A metadata API is registered with gst_meta_api_type_register() which takes a
 * name for the metadata API and some tags associated with the metadata.
 * With gst_meta_api_type_has_tag() one can check if a certain metadata API
 * contains a given tag.
 *
 * Multiple implementations of a metadata API can be registered.
 * To implement a metadata API, gst_meta_register() should be used. This
 * function takes all parameters needed to create, free and transform metadata
 * along with the size of the metadata. The function returns a #GstMetaInfo
 * structure that contains the information for the implementation of the API.
 *
 * A specific implementation can be retrieved by name with gst_meta_get_info().
 *
 * See #GstBuffer for how the metadata can be added, retrieved and removed from
 * buffers.
 */
#include "gst_private.h"

#include "gstbuffer.h"
#include "gstmeta.h"
#include "gstinfo.h"
#include "gstutils.h"
#include "gstquark.h"

static GHashTable *metainfo = NULL;
static GRWLock lock;

GQuark _gst_meta_transform_copy;
GQuark _gst_meta_tag_memory;

typedef struct
{
  GstCustomMeta meta;

  GstStructure *structure;
} GstCustomMetaImpl;

typedef struct
{
  GstMetaInfo info;
  GstCustomMetaTransformFunction custom_transform_func;
  gpointer custom_transform_user_data;
  GDestroyNotify custom_transform_destroy_notify;
  gboolean is_custom;
} GstMetaInfoImpl;

static void
free_info (gpointer data)
{
  g_slice_free (GstMetaInfoImpl, data);
}

void
_priv_gst_meta_initialize (void)
{
  g_rw_lock_init (&lock);
  metainfo = g_hash_table_new_full (g_str_hash, g_str_equal, NULL, free_info);

  _gst_meta_transform_copy = g_quark_from_static_string ("gst-copy");
  _gst_meta_tag_memory = g_quark_from_static_string ("memory");
}

static gboolean
notify_custom (gchar * key, GstMetaInfo * info, gpointer unused)
{
  GstMetaInfoImpl *impl = (GstMetaInfoImpl *) info;

  if (impl->is_custom) {
    if (impl->custom_transform_destroy_notify)
      impl->custom_transform_destroy_notify (impl->custom_transform_user_data);
  }
  return TRUE;
}

void
_priv_gst_meta_cleanup (void)
{
  if (metainfo != NULL) {
    g_hash_table_foreach_remove (metainfo, (GHRFunc) notify_custom, NULL);
    g_hash_table_unref (metainfo);
    metainfo = NULL;
  }
}

/**
 * gst_meta_api_type_register:
 * @api: an API to register
 * @tags: (array zero-terminated=1): tags for @api
 *
 * Register and return a GType for the @api and associate it with
 * @tags.
 *
 * Returns: a unique GType for @api.
 */
GType
gst_meta_api_type_register (const gchar * api, const gchar ** tags)
{
  GType type;

  g_return_val_if_fail (api != NULL, 0);
  g_return_val_if_fail (tags != NULL, 0);

  GST_CAT_DEBUG (GST_CAT_META, "register API \"%s\"", api);
  type = g_pointer_type_register_static (api);

  if (type != 0) {
    gint i;

    for (i = 0; tags[i]; i++) {
      GST_CAT_DEBUG (GST_CAT_META, "  adding tag \"%s\"", tags[i]);
      g_type_set_qdata (type, g_quark_from_string (tags[i]),
          GINT_TO_POINTER (TRUE));
    }
  }

  g_type_set_qdata (type, GST_QUARK (TAGS), g_strdupv ((gchar **) tags));

  return type;
}

static gboolean
custom_init_func (GstMeta * meta, gpointer params, GstBuffer * buffer)
{
  GstCustomMetaImpl *cmeta = (GstCustomMetaImpl *) meta;

  cmeta->structure = gst_structure_new_empty (g_type_name (meta->info->type));

  gst_structure_set_parent_refcount (cmeta->structure,
      &GST_MINI_OBJECT_REFCOUNT (buffer));

  return TRUE;
}

static void
custom_free_func (GstMeta * meta, GstBuffer * buffer)
{
  GstCustomMetaImpl *cmeta = (GstCustomMetaImpl *) meta;

  gst_structure_set_parent_refcount (cmeta->structure, NULL);
  gst_structure_free (cmeta->structure);
}

static gboolean
custom_transform_func (GstBuffer * transbuf, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  GstCustomMetaImpl *custom, *cmeta = (GstCustomMetaImpl *) meta;
  GstMetaInfoImpl *info = (GstMetaInfoImpl *) meta->info;

  if (info->custom_transform_func)
    return info->custom_transform_func (transbuf, (GstCustomMeta *) meta,
        buffer, type, data, info->custom_transform_user_data);

  if (GST_META_TRANSFORM_IS_COPY (type)) {
    custom =
        (GstCustomMetaImpl *) gst_buffer_add_meta (transbuf, meta->info, NULL);
    gst_structure_set_parent_refcount (custom->structure, NULL);
    gst_structure_take (&custom->structure,
        gst_structure_copy (cmeta->structure));
    gst_structure_set_parent_refcount (custom->structure,
        &GST_MINI_OBJECT_REFCOUNT (buffer));
  } else {
    return FALSE;
  }

  return TRUE;
}

/**
 * gst_custom_meta_get_structure:
 *
 * Retrieve the #GstStructure backing a custom meta, the structure's mutability
 * is conditioned to the writability of the #GstBuffer @meta is attached to.
 *
 * Returns: (transfer none): the #GstStructure backing @meta
 * Since: 1.20
 */
GstStructure *
gst_custom_meta_get_structure (GstCustomMeta * meta)
{
  g_return_val_if_fail (meta != NULL, NULL);
  g_return_val_if_fail (gst_meta_info_is_custom (((GstMeta *) meta)->info),
      NULL);

  return ((GstCustomMetaImpl *) meta)->structure;
}

/**
 * gst_custom_meta_has_name:
 *
 * Checks whether the name of the custom meta is @name
 *
 * Returns: Whether @name is the name of the custom meta
 * Since: 1.20
 */
gboolean
gst_custom_meta_has_name (GstCustomMeta * meta, const gchar * name)
{
  g_return_val_if_fail (meta != NULL, FALSE);
  g_return_val_if_fail (gst_meta_info_is_custom (((GstMeta *) meta)->info),
      FALSE);

  return gst_structure_has_name (((GstCustomMetaImpl *) meta)->structure, name);
}

/**
 * gst_meta_register_custom:
 * @name: the name of the #GstMeta implementation
 * @tags: (array zero-terminated=1): tags for @api
 * @transform_func: (scope notified) (nullable): a #GstMetaTransformFunction
 * @user_data: (closure): user data passed to @transform_func
 * @destroy_data: #GDestroyNotify for user_data
 *
 * Register a new custom #GstMeta implementation, backed by an opaque
 * structure holding a #GstStructure.
 *
 * The registered info can be retrieved later with gst_meta_get_info() by using
 * @name as the key.
 *
 * The backing #GstStructure can be retrieved with
 * gst_custom_meta_get_structure(), its mutability is conditioned by the
 * writability of the buffer the meta is attached to.
 *
 * When @transform_func is %NULL, the meta and its backing #GstStructure
 * will always be copied when the transform operation is copy, other operations
 * are discarded, copy regions are ignored.
 *
 * Returns: (transfer none): a #GstMetaInfo that can be used to
 * access metadata.
 * Since: 1.20
 */
const GstMetaInfo *
gst_meta_register_custom (const gchar * name, const gchar ** tags,
    GstCustomMetaTransformFunction transform_func,
    gpointer user_data, GDestroyNotify destroy_data)
{
  gchar *api_name = g_strdup_printf ("%s-api", name);
  GType api;
  GstMetaInfoImpl *info;
  GstMetaInfo *ret = NULL;

  g_return_val_if_fail (tags != NULL, NULL);
  g_return_val_if_fail (name != NULL, NULL);

  api = gst_meta_api_type_register (api_name, tags);
  g_free (api_name);
  if (api == G_TYPE_INVALID)
    goto done;

  info = (GstMetaInfoImpl *) gst_meta_register (api, name,
      sizeof (GstCustomMetaImpl),
      custom_init_func, custom_free_func, custom_transform_func);

  if (!info)
    goto done;

  info->is_custom = TRUE;
  info->custom_transform_func = transform_func;
  info->custom_transform_user_data = user_data;
  info->custom_transform_destroy_notify = destroy_data;

  ret = (GstMetaInfo *) info;

done:
  return ret;
}

/**
 * gst_meta_info_is_custom:
 *
 * Returns: whether @info was registered as a #GstCustomMeta with
 *   gst_meta_register_custom()
 * Since:1.20
 */
gboolean
gst_meta_info_is_custom (const GstMetaInfo * info)
{
  g_return_val_if_fail (info != NULL, FALSE);

  return ((GstMetaInfoImpl *) info)->is_custom;
}

/**
 * gst_meta_api_type_has_tag:
 * @api: an API
 * @tag: the tag to check
 *
 * Check if @api was registered with @tag.
 *
 * Returns: %TRUE if @api was registered with @tag.
 */
gboolean
gst_meta_api_type_has_tag (GType api, GQuark tag)
{
  g_return_val_if_fail (api != 0, FALSE);
  g_return_val_if_fail (tag != 0, FALSE);

  return g_type_get_qdata (api, tag) != NULL;
}

/**
 * gst_meta_api_type_get_tags:
 * @api: an API
 *
 * Returns: (transfer none) (array zero-terminated=1) (element-type utf8): an array of tags as strings.
 *
 * Since: 1.2
 */
const gchar *const *
gst_meta_api_type_get_tags (GType api)
{
  const gchar **tags;
  g_return_val_if_fail (api != 0, FALSE);

  tags = g_type_get_qdata (api, GST_QUARK (TAGS));

  if (!tags[0])
    return NULL;

  return (const gchar * const *) tags;
}

/**
 * gst_meta_register:
 * @api: the type of the #GstMeta API
 * @impl: the name of the #GstMeta implementation
 * @size: the size of the #GstMeta structure
 * @init_func: (scope async): a #GstMetaInitFunction
 * @free_func: (scope async): a #GstMetaFreeFunction
 * @transform_func: (scope async): a #GstMetaTransformFunction
 *
 * Register a new #GstMeta implementation.
 *
 * The same @info can be retrieved later with gst_meta_get_info() by using
 * @impl as the key.
 *
 * Returns: (transfer none): a #GstMetaInfo that can be used to
 * access metadata.
 */

const GstMetaInfo *
gst_meta_register (GType api, const gchar * impl, gsize size,
    GstMetaInitFunction init_func, GstMetaFreeFunction free_func,
    GstMetaTransformFunction transform_func)
{
  GstMetaInfo *info;
  GType type;

  g_return_val_if_fail (api != 0, NULL);
  g_return_val_if_fail (impl != NULL, NULL);
  g_return_val_if_fail (size != 0, NULL);

  if (init_func == NULL)
    g_critical ("Registering meta implementation '%s' without init function",
        impl);

  /* first try to register the implementation name. It's possible
   * that this fails because it was already registered. Don't warn,
   * glib did this for us already. */
  type = g_pointer_type_register_static (impl);
  if (type == 0)
    return NULL;

  info = (GstMetaInfo *) g_slice_new (GstMetaInfoImpl);
  info->api = api;
  info->type = type;
  info->size = size;
  info->init_func = init_func;
  info->free_func = free_func;
  info->transform_func = transform_func;
  ((GstMetaInfoImpl *) info)->is_custom = FALSE;

  GST_CAT_DEBUG (GST_CAT_META,
      "register \"%s\" implementing \"%s\" of size %" G_GSIZE_FORMAT, impl,
      g_type_name (api), size);

  g_rw_lock_writer_lock (&lock);
  g_hash_table_insert (metainfo, (gpointer) g_intern_string (impl),
      (gpointer) info);
  g_rw_lock_writer_unlock (&lock);

  return info;
}

/**
 * gst_meta_get_info:
 * @impl: the name
 *
 * Lookup a previously registered meta info structure by its implementation name
 * @impl.
 *
 * Returns: (transfer none) (nullable): a #GstMetaInfo with @impl, or
 * %NULL when no such metainfo exists.
 */
const GstMetaInfo *
gst_meta_get_info (const gchar * impl)
{
  GstMetaInfo *info;

  g_return_val_if_fail (impl != NULL, NULL);

  g_rw_lock_reader_lock (&lock);
  info = g_hash_table_lookup (metainfo, impl);
  g_rw_lock_reader_unlock (&lock);

  return info;
}

/**
 * gst_meta_get_seqnum:
 * @meta: a #GstMeta
 *
 * Gets seqnum for this meta.
 *
 * Since: 1.16
 */
guint64
gst_meta_get_seqnum (const GstMeta * meta)
{
  GstMetaItem *meta_item;
  guint8 *p;

  g_return_val_if_fail (meta != NULL, 0);

  p = (guint8 *) meta;
  p -= G_STRUCT_OFFSET (GstMetaItem, meta);
  meta_item = (GstMetaItem *) p;
  return meta_item->seq_num;
}

/**
 * gst_meta_compare_seqnum:
 * @meta1: a #GstMeta
 * @meta2: a #GstMeta
 *
 * Meta sequence number compare function. Can be used as #GCompareFunc
 * or a #GCompareDataFunc.
 *
 * Returns: a negative number if @meta1 comes before @meta2, 0 if both metas
 *   have an equal sequence number, or a positive integer if @meta1 comes
 *   after @meta2.
 *
 * Since: 1.16
 */
gint
gst_meta_compare_seqnum (const GstMeta * meta1, const GstMeta * meta2)
{
  guint64 seqnum1 = gst_meta_get_seqnum (meta1);
  guint64 seqnum2 = gst_meta_get_seqnum (meta2);

  if (seqnum1 == seqnum2)
    return 0;

  return (seqnum1 < seqnum2) ? -1 : 1;
}
