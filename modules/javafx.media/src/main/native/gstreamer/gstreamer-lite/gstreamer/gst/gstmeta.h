/* GStreamer
 * Copyright (C) 2009 Wim Taymans <wim.taymans@gmail.be>
 *
 * gstmeta.h: Header for Metadata structures
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


#ifndef __GST_META_H__
#define __GST_META_H__

#include <glib.h>

#include <gst/gstbytearrayinterface.h>


G_BEGIN_DECLS

typedef struct _GstMeta GstMeta;
typedef struct _GstMetaInfo GstMetaInfo;

#define GST_META_CAST(meta)   ((GstMeta *)(meta))

/**
 * GstMetaFlags:
 * @GST_META_FLAG_NONE: no flags
 * @GST_META_FLAG_READONLY: metadata should not be modified
 * @GST_META_FLAG_POOLED: metadata is managed by a bufferpool
 * @GST_META_FLAG_LOCKED: metadata should not be removed
 * @GST_META_FLAG_LAST: additional flags can be added starting from this flag.
 *
 * Extra metadata flags.
 */
typedef enum {
  GST_META_FLAG_NONE        = 0,
  GST_META_FLAG_READONLY    = (1 << 0),
  GST_META_FLAG_POOLED      = (1 << 1),
  GST_META_FLAG_LOCKED      = (1 << 2),

  GST_META_FLAG_LAST        = (1 << 16)
} GstMetaFlags;

/**
 * GST_META_FLAGS:
 * @meta: a #GstMeta.
 *
 * A flags word containing #GstMetaFlags flags set on @meta
 */
#define GST_META_FLAGS(meta)  (GST_META_CAST (meta)->flags)
/**
 * GST_META_FLAG_IS_SET:
 * @meta: a #GstMeta.
 * @flag: the #GstMetaFlags to check.
 *
 * Gives the status of a specific flag on a metadata.
 */
#define GST_META_FLAG_IS_SET(meta,flag)        !!(GST_META_FLAGS (meta) & (flag))
/**
 * GST_META_FLAG_SET:
 * @meta: a #GstMeta.
 * @flag: the #GstMetaFlags to set.
 *
 * Sets a metadata flag on a metadata.
 */
#define GST_META_FLAG_SET(meta,flag)           (GST_META_FLAGS (meta) |= (flag))
/**
 * GST_META_FLAG_UNSET:
 * @meta: a #GstMeta.
 * @flag: the #GstMetaFlags to clear.
 *
 * Clears a metadata flag.
 *
 */
#define GST_META_FLAG_UNSET(meta,flag)         (GST_META_FLAGS (meta) &= ~(flag))

/**
 * GST_META_TAG_MEMORY_STR:
 *
 * This metadata stays relevant as long as memory layout is unchanged.
 * In hindsight, this tag should have been called "memory-layout".
 *
 * Since: 1.2
 */
#define GST_META_TAG_MEMORY_STR "memory"

/**
 * GST_META_TAG_MEMORY_REFERENCE_STR:
 *
 * This metadata stays relevant until a deep copy is made.
 *
 * Since: 1.20.4
 */
#define GST_META_TAG_MEMORY_REFERENCE_STR "memory-reference"

/**
 * GstMeta:
 * @flags: extra flags for the metadata
 * @info: pointer to the #GstMetaInfo
 *
 * Base structure for metadata. Custom metadata will put this structure
 * as the first member of their structure.
 */
struct _GstMeta {
  GstMetaFlags       flags;
  const GstMetaInfo *info;
};

/**
 * GstCustomMeta.structure:
 *
 * #GstStructure containing custom metadata.
 *
 * Since: 1.24
 */

/**
 * GstCustomMeta:
 * @meta: parent #GstMeta
 * @structure: a #GstStructure containing custom metadata. (Since: 1.24)
 *
 * Extra custom metadata. The @structure field is the same as returned by
 * gst_custom_meta_get_structure().
 *
 * Since 1.24 it can be serialized using gst_meta_serialize() and
 * gst_meta_deserialize(), but only if the #GstStructure does not contain any
 * fields that cannot be serialized, see %GST_SERIALIZE_FLAG_STRICT.
 *
 * Since: 1.20
 */
typedef struct {
  GstMeta meta;
  GstStructure *structure;
} GstCustomMeta;

#include <gst/gstbuffer.h>

/**
 * GstMetaInitFunction:
 * @meta: a #GstMeta
 * @params: parameters passed to the init function
 * @buffer: a #GstBuffer
 *
 * Function called when @meta is initialized in @buffer.
 */
typedef gboolean (*GstMetaInitFunction) (GstMeta *meta, gpointer params, GstBuffer *buffer);

/**
 * GstMetaFreeFunction:
 * @meta: a #GstMeta
 * @buffer: a #GstBuffer
 *
 * Function called when @meta is freed in @buffer.
 */
typedef void (*GstMetaFreeFunction)     (GstMeta *meta, GstBuffer *buffer);

/**
 * gst_meta_transform_copy:
 *
 * GQuark for the "gst-copy" transform.
 */

#ifndef GSTREAMER_LITE
GST_API GQuark _gst_meta_transform_copy;
#else // GSTREAMER_LITE
GST_EXPORT GQuark _gst_meta_transform_copy;
#endif // GSTREAMER_LITE

/**
 * GST_META_TRANSFORM_IS_COPY:
 * @type: a transform type
 *
 * Check if the transform type is a copy transform
 */
#define GST_META_TRANSFORM_IS_COPY(type) ((type) == _gst_meta_transform_copy)

/**
 * GstMetaTransformCopy:
 * @region: %TRUE if only region is copied
 * @offset: the offset to copy, 0 if @region is %FALSE, otherwise > 0
 * @size: the size to copy, -1 or the buffer size when @region is %FALSE
 *
 * Extra data passed to a "gst-copy" transform #GstMetaTransformFunction.
 */
typedef struct {
  gboolean region;
  gsize offset;
  gsize size;
} GstMetaTransformCopy;

/**
 * GstMetaTransformFunction:
 * @transbuf: a #GstBuffer
 * @meta: a #GstMeta
 * @buffer: a #GstBuffer
 * @type: the transform type
 * @data: transform specific data.
 *
 * Function called for each @meta in @buffer as a result of performing a
 * transformation on @transbuf. Additional @type specific transform data
 * is passed to the function as @data.
 *
 * Implementations should check the @type of the transform and parse
 * additional type specific fields in @data that should be used to update
 * the metadata on @transbuf.
 *
 * Returns: %TRUE if the transform could be performed
 */
typedef gboolean (*GstMetaTransformFunction) (GstBuffer *transbuf,
                                              GstMeta *meta, GstBuffer *buffer,
                                              GQuark type, gpointer data);

/**
 * GstCustomMetaTransformFunction:
 * @transbuf: a #GstBuffer
 * @meta: a #GstCustomMeta
 * @buffer: a #GstBuffer
 * @type: the transform type
 * @data: transform specific data.
 * @user_data: user data passed when registering the meta
 *
 * Function called for each @meta in @buffer as a result of performing a
 * transformation that yields @transbuf. Additional @type specific transform
 * data is passed to the function as @data.
 *
 * Implementations should check the @type of the transform and parse
 * additional type specific fields in @data that should be used to update
 * the metadata on @transbuf.
 *
 * Returns: %TRUE if the transform could be performed
 * Since: 1.20
 */
typedef gboolean (*GstCustomMetaTransformFunction) (GstBuffer *transbuf,
                                                    GstCustomMeta *meta, GstBuffer *buffer,
                                                    GQuark type, gpointer data, gpointer user_data);

/**
 * GstMetaSerializeFunction:
 * @meta: a #GstMeta
 * @data: #GstByteArrayInterface to append serialization data
 * @version: (out): version of the serialization format
 *
 * Serialize @meta into a format that can be stored or transmitted and later
 * deserialized by #GstMetaDeserializeFunction.
 *
 * By default version is set to 0, it should be bumped if incompatible changes
 * are made to the format so %GstMetaDeserializeFunction can deserialize each
 * version.
 *
 * Returns: %TRUE on success, %FALSE otherwise.
 *
 * Since: 1.24
 */
typedef gboolean (*GstMetaSerializeFunction) (const GstMeta *meta,
    GstByteArrayInterface *data, guint8 *version);

/**
 * GstMetaDeserializeFunction:
 * @info: #GstMetaInfo of the meta
 * @buffer: a #GstBuffer
 * @data: data obtained from #GstMetaSerializeFunction
 * @size: size of data to avoid buffer overflow
 *
 * Recreate a #GstMeta from serialized data returned by
 * #GstMetaSerializeFunction and add it to @buffer.
 *
 * Returns: (transfer none) (nullable): the metadata owned by @buffer, or %NULL.
 *
 * Since: 1.24
 */
typedef GstMeta *(*GstMetaDeserializeFunction) (const GstMetaInfo *info,
    GstBuffer *buffer, const guint8 *data, gsize size, guint8 version);

/**
 * GstMetaClearFunction:
 * @buffer: a #GstBuffer
 * @meta: a #GstMeta
 *
 * Clears the content of the meta. This will be called by the GstBufferPool
 * when a pooled buffer is returned.
 *
 * Since: 1.24
 */
typedef void (*GstMetaClearFunction) (GstBuffer *buffer, GstMeta *meta);

/**
 * GstMetaInfo.serialize_func:
 *
 * Function for serializing the metadata, or %NULL if not supported by this
 * meta.
 *
 * Since: 1.24
 */

/**
 * GstMetaInfo.deserialize_func:
 *
 * Function for deserializing the metadata, or %NULL if not supported by this
 * meta.
 *
 * Since: 1.24
 */

/**
 * GstMetaInfo.clear_func:
 *
 * Function for clearing the metadata, or %NULL if not supported by this
 * meta. This is called by the buffer pool when a buffer is returned for
 * pooled metas.
 *
 * Since: 1.24
 */

/**
 * GstMetaInfo:
 * @api: tag identifying the metadata structure and api
 * @type: type identifying the implementor of the api
 * @size: size of the metadata
 * @init_func: function for initializing the metadata
 * @free_func: function for freeing the metadata
 * @transform_func: function for transforming the metadata
 * @serialize_func: function for serializing the metadata into a #GstStructure,
 *  or %NULL if not supported by this meta. (Since 1.24)
 * @deserialize_func: function for deserializing the metadata from a
 *  #GstStructure, or %NULL if not supported by this meta. (Since 1.24)
 *
 * The #GstMetaInfo provides information about a specific metadata
 * structure.
 */
struct _GstMetaInfo {
  GType                      api;
  GType                      type;
  gsize                      size;

  GstMetaInitFunction        init_func;
  GstMetaFreeFunction        free_func;
  GstMetaTransformFunction   transform_func;
  GstMetaSerializeFunction   serialize_func;
  GstMetaDeserializeFunction deserialize_func;
  GstMetaClearFunction       clear_func;

  /* No padding needed, GstMetaInfo is always allocated by GStreamer and is
   * not subclassable or stack-allocatable, so we can extend it as we please
   * just like interfaces */
};

GST_API
GType                gst_meta_api_type_register (const gchar *api,
                                                 const gchar **tags);
GST_API
gboolean             gst_meta_api_type_has_tag  (GType api, GQuark tag);

GST_API
const GstMetaInfo *  gst_meta_register          (GType api, const gchar *impl,
                                                 gsize size,
                                                 GstMetaInitFunction      init_func,
                                                 GstMetaFreeFunction      free_func,
                                                 GstMetaTransformFunction transform_func);

GST_API
GstMetaInfo *        gst_meta_info_new (GType api,
                                        const gchar *impl,
                                        gsize size);
GST_API
const GstMetaInfo *  gst_meta_info_register (GstMetaInfo *info);

GST_API
const GstMetaInfo *  gst_meta_register_custom   (const gchar *name, const gchar **tags,
                                                 GstCustomMetaTransformFunction transform_func,
                                                 gpointer user_data, GDestroyNotify destroy_data);

GST_API
const GstMetaInfo *  gst_meta_register_custom_simple (const gchar *name);

GST_API
gboolean             gst_meta_info_is_custom    (const GstMetaInfo *info);

GST_API
GstStructure *       gst_custom_meta_get_structure (GstCustomMeta *meta);

GST_API
gboolean             gst_custom_meta_has_name (GstCustomMeta *meta, const gchar * name);

GST_API
const GstMetaInfo *  gst_meta_get_info          (const gchar * impl);

GST_API
const gchar* const*  gst_meta_api_type_get_tags (GType api);

GST_API
guint64              gst_meta_get_seqnum        (const GstMeta * meta);

GST_API
gint                 gst_meta_compare_seqnum    (const GstMeta * meta1,
                                                 const GstMeta * meta2);

GST_API
gboolean             gst_meta_serialize         (const GstMeta *meta,
                                                 GstByteArrayInterface *data);
GST_API
gboolean             gst_meta_serialize_simple  (const GstMeta *meta,
                                                 GByteArray *data);
GST_API
GstMeta *            gst_meta_deserialize       (GstBuffer *buffer,
                                                 const guint8 *data,
                                                 gsize size,
                                                 guint32 *consumed);

/* some default tags */

#ifndef GSTREAMER_LITE
GST_API GQuark _gst_meta_tag_memory;
GST_API GQuark _gst_meta_tag_memory_reference;
#else // GSTREAMER_LITE
GST_EXPORT GQuark _gst_meta_tag_memory;
GST_EXPORT GQuark _gst_meta_tag_memory_reference;
#endif // GSTREAMER_LITE

/**
 * GST_META_TAG_MEMORY:
 *
 * Metadata tagged with this tag depends on the particular memory
 * or buffer that it is on.
 *
 * Deprecated: The GQuarks are not exported by any public API, use
 *   GST_META_TAG_MEMORY_STR instead.
 */
#ifndef GST_DISABLE_DEPRECATED
#define GST_META_TAG_MEMORY (_gst_meta_tag_memory)
#endif

G_END_DECLS

#endif /* __GST_META_H__ */
