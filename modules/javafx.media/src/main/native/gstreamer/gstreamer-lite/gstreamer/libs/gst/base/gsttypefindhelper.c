/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) 2000,2005 Wim Taymans <wim@fluendo.com>
 * Copyright (C) 2006      Tim-Philipp Müller <tim centricular net>
 *
 * gsttypefindhelper.c:
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
 * SECTION:gsttypefindhelper
 * @title: GstTypeFindHelper
 * @short_description: Utility functions for typefinding
 *
 * Utility functions for elements doing typefinding:
 * gst_type_find_helper() does typefinding in pull mode, while
 * gst_type_find_helper_for_buffer() is useful for elements needing to do
 * typefinding in push mode from a chain function.
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <stdlib.h>
#include <string.h>

#include "gsttypefindhelper.h"

/* ********************** typefinding in pull mode ************************ */

static void
helper_find_suggest (gpointer data, guint probability, GstCaps * caps);

typedef struct
{
  GstBuffer *buffer;
  GstMapInfo map;
} GstMappedBuffer;

typedef struct
{
  GSList *buffers;              /* buffer cache */
  guint64 size;
  guint64 last_offset;
  GstTypeFindHelperGetRangeFunction func;
  GstTypeFindProbability best_probability;
  GstCaps *caps;
  GstTypeFindFactory *factory;  /* for logging */
  GstObject *obj;               /* for logging */
  GstObject *parent;
  GstFlowReturn flow_ret;
} GstTypeFindHelper;

/*
 * helper_find_peek:
 * @data: helper data struct
 * @off: stream offset
 * @size: block size
 *
 * Get data pointer within a stream. Keeps a cache of read buffers (partly
 * for performance reasons, but mostly because pointers returned by us need
 * to stay valid until typefinding has finished)
 *
 * Returns: (nullable): address of the data or %NULL if buffer does not cover
 * the requested range.
 */
static const guint8 *
helper_find_peek (gpointer data, gint64 offset, guint size)
{
  GstTypeFindHelper *helper;
  GstBuffer *buffer;
  GSList *insert_pos = NULL;
  gsize buf_size;
  guint64 buf_offset;
  GstMappedBuffer *bmap;
#if 0
  GstCaps *caps;
#endif

  helper = (GstTypeFindHelper *) data;

  GST_LOG_OBJECT (helper->obj, "Typefind factory called peek (%" G_GINT64_FORMAT
      ", %u)", offset, size);

  if (size == 0)
    return NULL;

  if (offset < 0) {
    if (helper->size == -1 || helper->size < -offset)
      return NULL;

    offset += helper->size;
  }

  /* see if we have a matching buffer already in our list */
  if (size > 0 && offset <= helper->last_offset) {
    GSList *walk;

    for (walk = helper->buffers; walk; walk = walk->next) {
      GstMappedBuffer *bmp = (GstMappedBuffer *) walk->data;
      GstBuffer *buf = GST_BUFFER_CAST (bmp->buffer);

      buf_offset = GST_BUFFER_OFFSET (buf);
      buf_size = bmp->map.size;

      /* buffers are kept sorted by end offset (highest first) in the list, so
       * at this point we save the current position and stop searching if
       * we're after the searched end offset */
      if (buf_offset <= offset) {
        if ((offset + size) < (buf_offset + buf_size)) {
          /* must already have been mapped before */
          return (guint8 *) bmp->map.data + (offset - buf_offset);
        }
      } else if (offset + size >= buf_offset + buf_size) {
        insert_pos = walk;
        break;
      }
    }
  }

  buffer = NULL;
  /* some typefinders go in 1 byte steps over 1k of data and request
   * small buffers. It is really inefficient to pull each time, and pulling
   * a larger chunk is almost free. Trying to pull a larger chunk at the end
   * of the file is also not a problem here, we'll just get a truncated buffer
   * in that case (and we'll have to double-check the size we actually get
   * anyway, see below) */
  helper->flow_ret =
      helper->func (helper->obj, helper->parent, offset, MAX (size, 4096),
      &buffer);

  if (helper->flow_ret != GST_FLOW_OK)
    goto error;

#if 0
  caps = GST_BUFFER_CAPS (buffer);

  if (caps && !gst_caps_is_empty (caps) && !gst_caps_is_any (caps)) {
    GST_DEBUG ("buffer has caps %" GST_PTR_FORMAT ", suggest max probability",
        caps);

    gst_caps_replace (&helper->caps, caps);
    helper->best_probability = GST_TYPE_FIND_MAXIMUM;

    gst_buffer_unref (buffer);
    return NULL;
  }
#endif

  /* getrange might silently return shortened buffers at the end of a file,
   * we must, however, always return either the full requested data or %NULL */
  buf_offset = GST_BUFFER_OFFSET (buffer);
  buf_size = gst_buffer_get_size (buffer);

  if (buf_size < size) {
    GST_DEBUG ("dropping short buffer of size %" G_GSIZE_FORMAT ","
        "requested size was %u", buf_size, size);
    gst_buffer_unref (buffer);
    return NULL;
  }

  if (buf_offset != -1 && buf_offset != offset) {
    GST_DEBUG ("dropping buffer with unexpected offset %" G_GUINT64_FORMAT ", "
        "expected offset was %" G_GUINT64_FORMAT, buf_offset, offset);
    gst_buffer_unref (buffer);
    return NULL;
  }

  bmap = g_new0 (GstMappedBuffer, 1);

  if (!gst_buffer_map (buffer, &bmap->map, GST_MAP_READ))
    goto map_failed;

  bmap->buffer = buffer;

  if (insert_pos) {
    helper->buffers = g_slist_insert_before (helper->buffers, insert_pos, bmap);
  } else {
    /* if insert_pos is not set, our offset is bigger than the largest offset
     * we have so far; since we keep the list sorted with highest offsets
     * first, we need to prepend the buffer to the list */
    helper->last_offset = GST_BUFFER_OFFSET (buffer) + buf_size;
    helper->buffers = g_slist_prepend (helper->buffers, bmap);
  }

  return bmap->map.data;

error:
  {
    GST_INFO ("typefind function returned: %s",
        gst_flow_get_name (helper->flow_ret));
    return NULL;
  }
map_failed:
  {
    GST_ERROR ("map failed");
    gst_buffer_unref (buffer);
    g_free (bmap);
    return NULL;
  }
}

/*
 * helper_find_suggest:
 * @data: helper data struct
 * @probability: probability of the match
 * @caps: caps of the type
 *
 * If given @probability is higher, replace previously store caps.
 */
static void
helper_find_suggest (gpointer data, guint probability, GstCaps * caps)
{
  GstTypeFindHelper *helper = (GstTypeFindHelper *) data;

  GST_LOG_OBJECT (helper->obj,
      "Typefind factory called suggest (%u, %" GST_PTR_FORMAT ")",
      probability, caps);

  if (probability > helper->best_probability) {
    gst_caps_replace (&helper->caps, caps);
    helper->best_probability = probability;
  }
}

static guint64
helper_find_get_length (gpointer data)
{
  GstTypeFindHelper *helper = (GstTypeFindHelper *) data;

  GST_LOG_OBJECT (helper->obj, "Typefind factory called get_length, returning %"
      G_GUINT64_FORMAT, helper->size);

  return helper->size;
}

static GList *
prioritize_extension (GstObject * obj, GList * type_list,
    const gchar * extension)
{
  gint pos = 0;
  GList *next, *l;

  if (!extension)
    return type_list;

  /* move the typefinders for the extension first in the list. The idea is that
   * when one of them returns MAX we don't need to search further as there is a
   * very high chance we got the right type. */

  GST_LOG_OBJECT (obj, "sorting typefind for extension %s to head", extension);

  for (l = type_list; l; l = next) {
    const gchar *const *ext;
    GstTypeFindFactory *factory;

    next = l->next;

    factory = GST_TYPE_FIND_FACTORY (l->data);

    ext = gst_type_find_factory_get_extensions (factory);
    if (ext == NULL)
      continue;

    GST_LOG_OBJECT (obj, "testing factory %s for extension %s",
        GST_OBJECT_NAME (factory), extension);

    while (*ext != NULL) {
      if (strcmp (*ext, extension) == 0) {
        /* found extension, move in front */
        GST_LOG_OBJECT (obj, "moving typefind for extension %s to head",
            extension);
        /* remove entry from list */
        type_list = g_list_delete_link (type_list, l);
        /* insert at the position */
        type_list = g_list_insert (type_list, factory, pos);
        /* next element will be inserted after this one */
        pos++;
        break;
      }
      ++ext;
    }
  }

  return type_list;
}

/**
 * gst_type_find_helper_get_range:
 * @obj: A #GstObject that will be passed as first argument to @func
 * @parent: (nullable): the parent of @obj or %NULL
 * @func: (scope call): A generic #GstTypeFindHelperGetRangeFunction that will
 *        be used to access data at random offsets when doing the typefinding
 * @size: The length in bytes
 * @extension: (nullable): extension of the media, or %NULL
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Utility function to do pull-based typefinding. Unlike gst_type_find_helper()
 * however, this function will use the specified function @func to obtain the
 * data needed by the typefind functions, rather than operating on a given
 * source pad. This is useful mostly for elements like tag demuxers which
 * strip off data at the beginning and/or end of a file and want to typefind
 * the stripped data stream before adding their own source pad (the specified
 * callback can then call the upstream peer pad with offsets adjusted for the
 * tag size, for example).
 *
 * When @extension is not %NULL, this function will first try the typefind
 * functions for the given extension, which might speed up the typefinding
 * in many cases.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data
 *     stream.  Returns %NULL if no #GstCaps matches the data stream.
 */
GstCaps *
gst_type_find_helper_get_range (GstObject * obj, GstObject * parent,
    GstTypeFindHelperGetRangeFunction func, guint64 size,
    const gchar * extension, GstTypeFindProbability * prob)
{
  GstCaps *caps = NULL;

  gst_type_find_helper_get_range_full (obj, parent, func, size, extension,
      &caps, prob);

  return caps;
}

/**
 * gst_type_find_helper_get_range_full:
 * @obj: A #GstObject that will be passed as first argument to @func
 * @parent: (nullable): the parent of @obj or %NULL
 * @func: (scope call): A generic #GstTypeFindHelperGetRangeFunction that will
 *        be used to access data at random offsets when doing the typefinding
 * @size: The length in bytes
 * @extension: (nullable): extension of the media, or %NULL
 * @caps: (out) (transfer full): returned caps
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Utility function to do pull-based typefinding. Unlike gst_type_find_helper()
 * however, this function will use the specified function @func to obtain the
 * data needed by the typefind functions, rather than operating on a given
 * source pad. This is useful mostly for elements like tag demuxers which
 * strip off data at the beginning and/or end of a file and want to typefind
 * the stripped data stream before adding their own source pad (the specified
 * callback can then call the upstream peer pad with offsets adjusted for the
 * tag size, for example).
 *
 * When @extension is not %NULL, this function will first try the typefind
 * functions for the given extension, which might speed up the typefinding
 * in many cases.
 *
 * Returns: the last %GstFlowReturn from pulling a buffer or %GST_FLOW_OK if
 *          typefinding was successful.
 *
 * Since: 1.14.3
 */
GstFlowReturn
gst_type_find_helper_get_range_full (GstObject * obj, GstObject * parent,
    GstTypeFindHelperGetRangeFunction func, guint64 size,
    const gchar * extension, GstCaps ** caps, GstTypeFindProbability * prob)
{
  GstTypeFindHelper helper;
  GstTypeFind find;
  GSList *walk;
  GList *l, *type_list;
  GstCaps *result = NULL;

  g_return_val_if_fail (GST_IS_OBJECT (obj), GST_FLOW_ERROR);
  g_return_val_if_fail (func != NULL, GST_FLOW_ERROR);
  g_return_val_if_fail (caps != NULL, GST_FLOW_ERROR);

  *caps = NULL;

  helper.buffers = NULL;
  helper.size = size;
  helper.last_offset = 0;
  helper.func = func;
  helper.best_probability = GST_TYPE_FIND_NONE;
  helper.caps = NULL;
  helper.obj = obj;
  helper.parent = parent;
  helper.flow_ret = GST_FLOW_OK;

  find.data = &helper;
  find.peek = helper_find_peek;
  find.suggest = helper_find_suggest;

  if (size == 0 || size == (guint64) - 1) {
    find.get_length = NULL;
  } else {
    find.get_length = helper_find_get_length;
  }

  type_list = gst_type_find_factory_get_list ();
  type_list = prioritize_extension (obj, type_list, extension);

  for (l = type_list; l; l = l->next) {
    helper.factory = GST_TYPE_FIND_FACTORY (l->data);
    gst_type_find_factory_call_function (helper.factory, &find);
    if (helper.best_probability >= GST_TYPE_FIND_MAXIMUM) {
      /* Any other flow return can be ignored here, we found
       * something before any error with highest probability */
      helper.flow_ret = GST_FLOW_OK;
      break;
    } else if (helper.flow_ret != GST_FLOW_OK
        && helper.flow_ret != GST_FLOW_EOS) {
      /* We had less than maximum probability and an error, don't return
       * any caps as they might be with a lower probability than what
       * we would've gotten when continuing if there was no error */
      gst_caps_replace (&helper.caps, NULL);
      break;
    }
  }
  gst_plugin_feature_list_free (type_list);

  for (walk = helper.buffers; walk; walk = walk->next) {
    GstMappedBuffer *bmap = (GstMappedBuffer *) walk->data;

    gst_buffer_unmap (bmap->buffer, &bmap->map);
    gst_buffer_unref (bmap->buffer);
    g_free (bmap);
  }
  g_slist_free (helper.buffers);

  if (helper.best_probability > 0)
    result = helper.caps;

  if (prob)
    *prob = helper.best_probability;

  *caps = result;
  if (helper.flow_ret == GST_FLOW_EOS) {
    /* Some typefinder might've tried to read too much, if we
     * didn't get any meaningful caps because of that this is
     * just a normal error */
    helper.flow_ret = GST_FLOW_ERROR;
  }

  GST_LOG_OBJECT (obj, "Returning %" GST_PTR_FORMAT " (probability = %u)",
      result, (guint) helper.best_probability);

  return helper.flow_ret;
}

/**
 * gst_type_find_helper:
 * @src: A source #GstPad
 * @size: The length in bytes
 *
 * Tries to find what type of data is flowing from the given source #GstPad.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data
 *     stream.  Returns %NULL if no #GstCaps matches the data stream.
 */

GstCaps *
gst_type_find_helper (GstPad * src, guint64 size)
{
  GstTypeFindHelperGetRangeFunction func;

  g_return_val_if_fail (GST_IS_OBJECT (src), NULL);
  g_return_val_if_fail (GST_PAD_GETRANGEFUNC (src) != NULL, NULL);

  func = (GstTypeFindHelperGetRangeFunction) (GST_PAD_GETRANGEFUNC (src));

  return gst_type_find_helper_get_range (GST_OBJECT (src),
      GST_OBJECT_PARENT (src), func, size, NULL, NULL);
}

/* ********************** typefinding for buffers ************************* */

typedef struct
{
  const guint8 *data;           /* buffer data */
  gsize size;
  GstTypeFindProbability best_probability;
  GstCaps *caps;
  GstObject *obj;               /* for logging */
} GstTypeFindBufHelper;

/**
 * GstTypeFindData:
 *
 * The opaque #GstTypeFindData structure.
 *
 * Since: 1.22
 *
 */
struct _GstTypeFindData
{
  GstTypeFind find;
  GstTypeFindBufHelper helper;
};

/*
 * buf_helper_find_peek:
 * @data: helper data struct
 * @off: stream offset
 * @size: block size
 *
 * Get data pointer within a buffer.
 *
 * Returns: (nullable): address inside the buffer or %NULL if buffer does not
 * cover the requested range.
 */
static const guint8 *
buf_helper_find_peek (gpointer data, gint64 off, guint size)
{
  GstTypeFindBufHelper *helper;

  helper = (GstTypeFindBufHelper *) data;
  GST_LOG_OBJECT (helper->obj,
      "Typefind factory called peek (%" G_GINT64_FORMAT ", %u)", off, size);

  if (size == 0)
    return NULL;

  if (off < 0) {
    GST_LOG_OBJECT (helper->obj,
        "Typefind factory wanted to peek at end; not supported");
    return NULL;
  }

  /* If we request beyond the available size, we're sure we can't return
   * anything regardless of the requested offset */
  if (size > helper->size)
    return NULL;

  /* Only return data if there's enough room left for the given offset.
   * This is the same as "if (off + size <= helper->size)" except that
   * it doesn't exceed type limits */
  if (off <= helper->size - size)
    return helper->data + off;

  return NULL;
}

/*
 * buf_helper_find_suggest:
 * @data: helper data struct
 * @probability: probability of the match
 * @caps: caps of the type
 *
 * If given @probability is higher, replace previously store caps.
 */
static void
buf_helper_find_suggest (gpointer data, guint probability, GstCaps * caps)
{
  GstTypeFindBufHelper *helper = (GstTypeFindBufHelper *) data;

  GST_LOG_OBJECT (helper->obj,
      "Typefind factory called suggest (%u, %" GST_PTR_FORMAT ")",
      probability, caps);

  /* Note: not >= as we call typefinders in order of rank, highest first */
  if (probability > helper->best_probability) {
    gst_caps_replace (&helper->caps, caps);
    helper->best_probability = probability;
  }
}

/**
 * gst_type_find_helper_for_data:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @data: (transfer none) (array length=size): * a pointer with data to typefind
 * @size: the size of @data
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Tries to find what type of data is contained in the given @data, the
 * assumption being that the data represents the beginning of the stream or
 * file.
 *
 * All available typefinders will be called on the data in order of rank. If
 * a typefinding function returns a probability of %GST_TYPE_FIND_MAXIMUM,
 * typefinding is stopped immediately and the found caps will be returned
 * right away. Otherwise, all available typefind functions will the tried,
 * and the caps with the highest probability will be returned, or %NULL if
 * the content of @data could not be identified.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data,
 *     or %NULL if no type could be found. The caller should free the caps
 *     returned with gst_caps_unref().
 */
GstCaps *
gst_type_find_helper_for_data (GstObject * obj, const guint8 * data, gsize size,
    GstTypeFindProbability * prob)
{
  return gst_type_find_helper_for_data_with_extension (obj, data, size, NULL,
      prob);
}

/**
 * gst_type_find_helper_for_data_with_extension:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @data: (transfer none) (array length=size): * a pointer with data to typefind
 * @size: the size of @data
 * @extension: (nullable): extension of the media, or %NULL
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Tries to find what type of data is contained in the given @data, the
 * assumption being that the data represents the beginning of the stream or
 * file.
 *
 * All available typefinders will be called on the data in order of rank. If
 * a typefinding function returns a probability of %GST_TYPE_FIND_MAXIMUM,
 * typefinding is stopped immediately and the found caps will be returned
 * right away. Otherwise, all available typefind functions will the tried,
 * and the caps with the highest probability will be returned, or %NULL if
 * the content of @data could not be identified.
 *
 * When @extension is not %NULL, this function will first try the typefind
 * functions for the given extension, which might speed up the typefinding
 * in many cases.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data,
 *     or %NULL if no type could be found. The caller should free the caps
 *     returned with gst_caps_unref().
 *
 * Since: 1.16
 *
 */
GstCaps *
gst_type_find_helper_for_data_with_extension (GstObject * obj,
    const guint8 * data, gsize size, const gchar * extension,
    GstTypeFindProbability * prob)
{
  GstTypeFindBufHelper helper;
  GstTypeFindFactory *factory;
  GstTypeFind find;
  GList *l, *type_list;
  GstCaps *result = NULL;

  g_return_val_if_fail (data != NULL, NULL);

  helper.data = data;
  helper.size = size;
  helper.best_probability = GST_TYPE_FIND_NONE;
  helper.caps = NULL;
  helper.obj = obj;

  if (helper.data == NULL || helper.size == 0)
    return NULL;

  find.data = &helper;
  find.peek = buf_helper_find_peek;
  find.suggest = buf_helper_find_suggest;
  find.get_length = NULL;

  type_list = gst_type_find_factory_get_list ();
  type_list = prioritize_extension (obj, type_list, extension);

  for (l = type_list; l; l = l->next) {
    factory = GST_TYPE_FIND_FACTORY (l->data);
    gst_type_find_factory_call_function (factory, &find);
    if (helper.best_probability >= GST_TYPE_FIND_MAXIMUM)
      break;
  }
  gst_plugin_feature_list_free (type_list);

  if (helper.best_probability > 0)
    result = helper.caps;

  if (prob)
    *prob = helper.best_probability;

  GST_LOG_OBJECT (obj, "Returning %" GST_PTR_FORMAT " (probability = %u)",
      result, (guint) helper.best_probability);

  return result;
}

/**
 * gst_type_find_helper_for_data_with_caps:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @data: (transfer none) (array length=size): a pointer with data to typefind
 * @size: the size of @data
 * @caps: caps of the media
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Tries to find if type of media contained in the given @data, matches the
 * @caps specified, assumption being that the data represents the beginning
 * of the stream or file.
 *
 * Only the typefinder matching the given caps will be called, if found. The
 * caps with the highest probability will be returned, or %NULL if the content
 * of the @data could not be identified.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data,
 *     or %NULL if no type could be found. The caller should free the caps
 *     returned with gst_caps_unref().
 *
 * Since: 1.22
 *
 */
GstCaps *
gst_type_find_helper_for_data_with_caps (GstObject * obj,
    const guint8 * data, gsize size, GstCaps * caps,
    GstTypeFindProbability * prob)
{
  GstTypeFind *find;
  GstTypeFindData *find_data;
  GList *l, *factories = NULL;
  GstCaps *result = NULL;
  GstTypeFindProbability last_found_probability;

  g_return_val_if_fail (data != NULL, NULL);
  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (size != 0, NULL);

  find_data = gst_type_find_data_new (obj, data, size);
  find = gst_type_find_data_get_typefind (find_data);

  factories = gst_type_find_list_factories_for_caps (obj, caps);
  if (!factories) {
    GST_INFO_OBJECT (obj, "Failed to typefind for caps: %" GST_PTR_FORMAT,
        caps);
    goto out;
  }

  last_found_probability = GST_TYPE_FIND_NONE;

  for (l = factories; l; l = l->next) {
    GstTypeFindProbability found_probability;
    GstTypeFindFactory *factory = GST_TYPE_FIND_FACTORY (l->data);

    gst_type_find_factory_call_function (factory, find);

    found_probability = gst_type_find_data_get_probability (find_data);

    if (found_probability > last_found_probability) {
      last_found_probability = found_probability;
      result = gst_type_find_data_get_caps (find_data);

      GST_DEBUG_OBJECT (obj, "Found %" GST_PTR_FORMAT " (probability = %u)",
          result, (guint) last_found_probability);
      if (last_found_probability >= GST_TYPE_FIND_MAXIMUM)
        break;
    }
  }

  if (prob)
    *prob = last_found_probability;

  GST_LOG_OBJECT (obj, "Returning %" GST_PTR_FORMAT " (probability = %u)",
      result, (guint) last_found_probability);

out:
  g_list_free_full (factories, (GDestroyNotify) gst_object_unref);

  gst_type_find_data_free (find_data);

  return result;
}

/**
 * gst_type_find_helper_for_buffer:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @buf: (in) (transfer none): a #GstBuffer with data to typefind
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Tries to find what type of data is contained in the given #GstBuffer, the
 * assumption being that the buffer represents the beginning of the stream or
 * file.
 *
 * All available typefinders will be called on the data in order of rank. If
 * a typefinding function returns a probability of %GST_TYPE_FIND_MAXIMUM,
 * typefinding is stopped immediately and the found caps will be returned
 * right away. Otherwise, all available typefind functions will the tried,
 * and the caps with the highest probability will be returned, or %NULL if
 * the content of the buffer could not be identified.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data,
 *     or %NULL if no type could be found. The caller should free the caps
 *     returned with gst_caps_unref().
 */
GstCaps *
gst_type_find_helper_for_buffer (GstObject * obj, GstBuffer * buf,
    GstTypeFindProbability * prob)
{
  return gst_type_find_helper_for_buffer_with_extension (obj, buf, NULL, prob);
}

/**
 * gst_type_find_helper_for_buffer_with_extension:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @buf: (in) (transfer none): a #GstBuffer with data to typefind
 * @extension: (nullable): extension of the media, or %NULL
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Tries to find what type of data is contained in the given #GstBuffer, the
 * assumption being that the buffer represents the beginning of the stream or
 * file.
 *
 * All available typefinders will be called on the data in order of rank. If
 * a typefinding function returns a probability of %GST_TYPE_FIND_MAXIMUM,
 * typefinding is stopped immediately and the found caps will be returned
 * right away. Otherwise, all available typefind functions will the tried,
 * and the caps with the highest probability will be returned, or %NULL if
 * the content of the buffer could not be identified.
 *
 * When @extension is not %NULL, this function will first try the typefind
 * functions for the given extension, which might speed up the typefinding
 * in many cases.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data,
 *     or %NULL if no type could be found. The caller should free the caps
 *     returned with gst_caps_unref().
 *
 * Since: 1.16
 *
 */
GstCaps *
gst_type_find_helper_for_buffer_with_extension (GstObject * obj,
    GstBuffer * buf, const gchar * extension, GstTypeFindProbability * prob)
{
  GstCaps *result;
  GstMapInfo info;

  g_return_val_if_fail (buf != NULL, NULL);
  g_return_val_if_fail (GST_IS_BUFFER (buf), NULL);
  g_return_val_if_fail (GST_BUFFER_OFFSET (buf) == 0 ||
      GST_BUFFER_OFFSET (buf) == GST_BUFFER_OFFSET_NONE, NULL);

  if (!gst_buffer_map (buf, &info, GST_MAP_READ))
    return NULL;
  result =
      gst_type_find_helper_for_data_with_extension (obj, info.data, info.size,
      extension, prob);
  gst_buffer_unmap (buf, &info);

  return result;
}

/**
 * gst_type_find_helper_for_buffer_with_caps:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @buf: (transfer none): a #GstBuffer with data to typefind
 * @caps: caps of the media
 * @prob: (out) (optional): location to store the probability of the found
 *     caps, or %NULL
 *
 * Tries to find if type of media contained in the given #GstBuffer, matches
 * @caps specified, assumption being that the buffer represents the beginning
 * of the stream or file.
 *
 * Tries to find what type of data is contained in the given @data, the
 * assumption being that the data represents the beginning of the stream or
 * file.
 *
 * Only the typefinder matching the given caps will be called, if found. The
 * caps with the highest probability will be returned, or %NULL if the content
 * of the @data could not be identified.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to the data,
 *     or %NULL if no type could be found. The caller should free the caps
 *     returned with gst_caps_unref().
 *
 * Since: 1.22
 *
 */
GstCaps *
gst_type_find_helper_for_buffer_with_caps (GstObject * obj,
    GstBuffer * buf, GstCaps * caps, GstTypeFindProbability * prob)
{
  GstCaps *result;
  GstMapInfo info;

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (buf != NULL, NULL);
  g_return_val_if_fail (GST_IS_BUFFER (buf), NULL);
  g_return_val_if_fail (GST_BUFFER_OFFSET (buf) == 0 ||
      GST_BUFFER_OFFSET (buf) == GST_BUFFER_OFFSET_NONE, NULL);

  if (!gst_buffer_map (buf, &info, GST_MAP_READ))
    return NULL;

  result =
      gst_type_find_helper_for_data_with_caps (obj, info.data, info.size,
      caps, prob);

  gst_buffer_unmap (buf, &info);

  return result;
}

/**
 * gst_type_find_helper_for_extension:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @extension: an extension
 *
 * Tries to find the best #GstCaps associated with @extension.
 *
 * All available typefinders will be checked against the extension in order
 * of rank. The caps of the first typefinder that can handle @extension will be
 * returned.
 *
 * Free-function: gst_caps_unref
 *
 * Returns: (transfer full) (nullable): the #GstCaps corresponding to
 *     @extension, or %NULL if no type could be found. The caller should free
 *     the caps returned with gst_caps_unref().
 */
GstCaps *
gst_type_find_helper_for_extension (GstObject * obj, const gchar * extension)
{
  GList *l, *type_list;
  GstCaps *result = NULL;

  g_return_val_if_fail (extension != NULL, NULL);

  GST_LOG_OBJECT (obj, "finding caps for extension %s", extension);

  type_list = gst_type_find_factory_get_list ();

  for (l = type_list; l; l = g_list_next (l)) {
    GstTypeFindFactory *factory;
    const gchar *const *ext;

    factory = GST_TYPE_FIND_FACTORY (l->data);

    /* we only want to check those factories without a function */
    if (gst_type_find_factory_has_function (factory))
      continue;

    /* get the extension that this typefind factory can handle */
    ext = gst_type_find_factory_get_extensions (factory);
    if (ext == NULL)
      continue;

    /* there are extension, see if one of them matches the requested
     * extension */
    while (*ext != NULL) {
      if (strcmp (*ext, extension) == 0) {
        /* we found a matching extension, take the caps */
        if ((result = gst_type_find_factory_get_caps (factory))) {
          gst_caps_ref (result);
          goto done;
        }
      }
      ++ext;
    }
  }
done:
  gst_plugin_feature_list_free (type_list);

  GST_LOG_OBJECT (obj, "Returning %" GST_PTR_FORMAT, result);

  return result;
}

/**
 * gst_type_find_list_factories_for_caps:
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @caps: caps of the media
 *
 * Tries to find the best #GstTypeFindFactory associated with @caps.
 *
 * The typefinder that can handle @caps will be returned.
 *
 * Free-function: g_list_free
 *
 * Returns: (transfer full) (nullable) (element-type Gst.TypeFindFactory): the list of #GstTypeFindFactory
 *          corresponding to @caps, or %NULL if no typefinder could be
 *          found. Caller should free the returned list with g_list_free()
 *          and list elements with gst_object_unref().
 *
 * Since: 1.22
 *
 */
GList *
gst_type_find_list_factories_for_caps (GstObject * obj, GstCaps * caps)
{
  GList *l, *type_list, *factories = NULL;

  g_return_val_if_fail (caps != NULL, NULL);

  GST_LOG_OBJECT (obj, "finding factory for caps %" GST_PTR_FORMAT, caps);

  type_list = gst_type_find_factory_get_list ();

  for (l = type_list; l; l = g_list_next (l)) {
    GstTypeFindFactory *factory;
    GstCaps *factory_caps;

    factory = GST_TYPE_FIND_FACTORY (l->data);

    /* We only want to check those factories without a function */
    if (gst_type_find_factory_has_function (factory))
      continue;

    /* Get the caps that this typefind factory can handle */
    factory_caps = gst_type_find_factory_get_caps (factory);
    if (!factory_caps)
      continue;

    if (gst_caps_can_intersect (factory_caps, caps)) {
      factory = gst_object_ref (factory);
      factories = g_list_prepend (factories, factory);
    }
  }

  gst_plugin_feature_list_free (type_list);

  return g_list_reverse (factories);
}

/**
 * gst_type_find_data_new: (skip)
 * @obj: (nullable): object doing the typefinding, or %NULL (used for logging)
 * @data: (transfer none) (array length=size): a pointer with data to typefind
 * @size: the size of @data
 *
 * Free-function: gst_type_find_data_free
 *
 * Returns: (transfer full): the #GstTypeFindData. The caller should free
 *          the returned #GstTypeFindData with gst_type_find_data_free().
 *
 * Since: 1.22
 *
 */
GstTypeFindData *
gst_type_find_data_new (GstObject * obj, const guint8 * data, gsize size)
{
  GstTypeFindData *find_data;

  g_return_val_if_fail (data != NULL, NULL);
  g_return_val_if_fail (size != 0, NULL);

  find_data = g_new0 (GstTypeFindData, 1);

  find_data->helper.data = data;
  find_data->helper.size = size;
  find_data->helper.best_probability = GST_TYPE_FIND_NONE;
  find_data->helper.caps = NULL;
  find_data->helper.obj = obj;

  find_data->find.data = (gpointer) (&find_data->helper);
  find_data->find.peek = buf_helper_find_peek;
  find_data->find.suggest = buf_helper_find_suggest;
  find_data->find.get_length = NULL;

  return find_data;
}

/**
 * gst_type_find_data_get_caps: (skip)
 * @data: GstTypeFindData *
 *
 * Returns #GstCaps associated with #GstTypeFindData
 *
 * Returns: (transfer full) (nullable): #GstCaps.
 *
 * Since: 1.22
 *
 */
GstCaps *
gst_type_find_data_get_caps (GstTypeFindData * data)
{
  g_return_val_if_fail (data != NULL, NULL);

  return gst_caps_ref (data->helper.caps);
}

/**
 * gst_type_find_data_get_probability: (skip)
 * @data: GstTypeFindData *
 *
 * Returns #GstTypeFindProbability associated with #GstTypeFindData
 *
 * Returns: #GstTypeFindProbability.
 *
 * Since: 1.22
 *
 */
GstTypeFindProbability
gst_type_find_data_get_probability (GstTypeFindData * data)
{
  g_return_val_if_fail (data != NULL, GST_TYPE_FIND_NONE);

  return data->helper.best_probability;
}

/**
 * gst_type_find_data_get_typefind: (skip)
 * @data: GstTypeFindData *
 *
 * Returns #GstTypeFind associated with #GstTypeFindData
 *
 * Returns: #GstTypeFind.
 *
 * Since: 1.22
 *
 */
GstTypeFind *
gst_type_find_data_get_typefind (GstTypeFindData * data)
{
  g_return_val_if_fail (data != NULL, NULL);

  return &data->find;
}

/**
 * gst_type_find_data_free: (skip)
 * @data: GstTypeFindData * to free
 *
 * Since: 1.22
 *
 */
void
gst_type_find_data_free (GstTypeFindData * data)
{
  if (data && data->helper.caps)
    gst_caps_unref (data->helper.caps);

  g_free (data);
}
