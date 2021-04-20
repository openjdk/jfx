/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gstbuffer.c: Buffer operations
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
 * SECTION:gstbuffer
 * @title: GstBuffer
 * @short_description: Data-passing buffer type
 * @see_also: #GstPad, #GstMiniObject, #GstMemory, #GstMeta, #GstBufferPool
 *
 * Buffers are the basic unit of data transfer in GStreamer. They contain the
 * timing and offset along with other arbitrary metadata that is associated
 * with the #GstMemory blocks that the buffer contains.
 *
 * Buffers are usually created with gst_buffer_new(). After a buffer has been
 * created one will typically allocate memory for it and add it to the buffer.
 * The following example creates a buffer that can hold a given video frame
 * with a given width, height and bits per plane.
 * |[<!-- language="C" -->
 *   GstBuffer *buffer;
 *   GstMemory *memory;
 *   gint size, width, height, bpp;
 *   ...
 *   size = width * height * bpp;
 *   buffer = gst_buffer_new ();
 *   memory = gst_allocator_alloc (NULL, size, NULL);
 *   gst_buffer_insert_memory (buffer, -1, memory);
 *   ...
 * ]|
 *
 * Alternatively, use gst_buffer_new_allocate() to create a buffer with
 * preallocated data of a given size.
 *
 * Buffers can contain a list of #GstMemory objects. You can retrieve how many
 * memory objects with gst_buffer_n_memory() and you can get a pointer
 * to memory with gst_buffer_peek_memory()
 *
 * A buffer will usually have timestamps, and a duration, but neither of these
 * are guaranteed (they may be set to #GST_CLOCK_TIME_NONE). Whenever a
 * meaningful value can be given for these, they should be set. The timestamps
 * and duration are measured in nanoseconds (they are #GstClockTime values).
 *
 * The buffer DTS refers to the timestamp when the buffer should be decoded and
 * is usually monotonically increasing. The buffer PTS refers to the timestamp when
 * the buffer content should be presented to the user and is not always
 * monotonically increasing.
 *
 * A buffer can also have one or both of a start and an end offset. These are
 * media-type specific. For video buffers, the start offset will generally be
 * the frame number. For audio buffers, it will be the number of samples
 * produced so far. For compressed data, it could be the byte offset in a
 * source or destination file. Likewise, the end offset will be the offset of
 * the end of the buffer. These can only be meaningfully interpreted if you
 * know the media type of the buffer (the preceding CAPS event). Either or both
 * can be set to #GST_BUFFER_OFFSET_NONE.
 *
 * gst_buffer_ref() is used to increase the refcount of a buffer. This must be
 * done when you want to keep a handle to the buffer after pushing it to the
 * next element. The buffer refcount determines the writability of the buffer, a
 * buffer is only writable when the refcount is exactly 1, i.e. when the caller
 * has the only reference to the buffer.
 *
 * To efficiently create a smaller buffer out of an existing one, you can
 * use gst_buffer_copy_region(). This method tries to share the memory objects
 * between the two buffers.
 *
 * If a plug-in wants to modify the buffer data or metadata in-place, it should
 * first obtain a buffer that is safe to modify by using
 * gst_buffer_make_writable().  This function is optimized so that a copy will
 * only be made when it is necessary.
 *
 * Several flags of the buffer can be set and unset with the
 * GST_BUFFER_FLAG_SET() and GST_BUFFER_FLAG_UNSET() macros. Use
 * GST_BUFFER_FLAG_IS_SET() to test if a certain #GstBufferFlags flag is set.
 *
 * Buffers can be efficiently merged into a larger buffer with
 * gst_buffer_append(). Copying of memory will only be done when absolutely
 * needed.
 *
 * Arbitrary extra metadata can be set on a buffer with gst_buffer_add_meta().
 * Metadata can be retrieved with gst_buffer_get_meta(). See also #GstMeta
 *
 * An element should either unref the buffer or push it out on a src pad
 * using gst_pad_push() (see #GstPad).
 *
 * Buffers are usually freed by unreffing them with gst_buffer_unref(). When
 * the refcount drops to 0, any memory and metadata pointed to by the buffer is
 * unreffed as well. Buffers allocated from a #GstBufferPool will be returned to
 * the pool when the refcount drops to 0.
 *
 * The #GstParentBufferMeta is a meta which can be attached to a #GstBuffer
 * to hold a reference to another buffer that is only released when the child
 * #GstBuffer is released.
 *
 * Typically, #GstParentBufferMeta is used when the child buffer is directly
 * using the #GstMemory of the parent buffer, and wants to prevent the parent
 * buffer from being returned to a buffer pool until the #GstMemory is available
 * for re-use. (Since: 1.6)
 *
 */
#define GST_DISABLE_MINIOBJECT_INLINE_FUNCTIONS
#include "gst_private.h"

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif

#include "gstbuffer.h"
#include "gstbufferpool.h"
#include "gstinfo.h"
#include "gstutils.h"
#include "gstversion.h"

GType _gst_buffer_type = 0;

/* info->size will be sizeof(FooMeta) which contains a GstMeta at the beginning
 * too, and then there is again a GstMeta in GstMetaItem, so subtract one. */
#define ITEM_SIZE(info) ((info)->size + sizeof (GstMetaItem) - sizeof (GstMeta))

#define GST_BUFFER_MEM_MAX         16

#define GST_BUFFER_SLICE_SIZE(b)   (((GstBufferImpl *)(b))->slice_size)
#define GST_BUFFER_MEM_LEN(b)      (((GstBufferImpl *)(b))->len)
#define GST_BUFFER_MEM_ARRAY(b)    (((GstBufferImpl *)(b))->mem)
#define GST_BUFFER_MEM_PTR(b,i)    (((GstBufferImpl *)(b))->mem[i])
#define GST_BUFFER_BUFMEM(b)       (((GstBufferImpl *)(b))->bufmem)
#define GST_BUFFER_META(b)         (((GstBufferImpl *)(b))->item)
#define GST_BUFFER_TAIL_META(b)    (((GstBufferImpl *)(b))->tail_item)

typedef struct
{
  GstBuffer buffer;

  gsize slice_size;

  /* the memory blocks */
  guint len;
  GstMemory *mem[GST_BUFFER_MEM_MAX];

  /* memory of the buffer when allocated from 1 chunk */
  GstMemory *bufmem;

  /* FIXME, make metadata allocation more efficient by using part of the
   * GstBufferImpl */
  GstMetaItem *item;
  GstMetaItem *tail_item;
} GstBufferImpl;

static gint64 meta_seq;         /* 0 *//* ATOMIC */

/* TODO: use GLib's once https://gitlab.gnome.org/GNOME/glib/issues/1076 lands */
#if defined(__GCC_HAVE_SYNC_COMPARE_AND_SWAP_8)
static inline gint64
gst_atomic_int64_inc (volatile gint64 * atomic)
{
  return __sync_fetch_and_add (atomic, 1);
}
#elif defined (G_PLATFORM_WIN32)
#include <windows.h>
static inline gint64
gst_atomic_int64_inc (volatile gint64 * atomic)
{
  return InterlockedExchangeAdd64 (atomic, 1);
}
#else
#define STR_TOKEN(s) #s
#define STR(s) STR_TOKEN(s)
#pragma message "No 64-bit atomic int defined for this " STR(TARGET_CPU) " platform/toolchain!"

#define NO_64BIT_ATOMIC_INT_FOR_PLATFORM
G_LOCK_DEFINE_STATIC (meta_seq);
static inline gint64
gst_atomic_int64_inc (volatile gint64 * atomic)
{
  gint64 ret;

  G_LOCK (meta_seq);
  ret = (*atomic)++;
  G_UNLOCK (meta_seq);

  return ret;
}
#endif

static gboolean
_is_span (GstMemory ** mem, gsize len, gsize * poffset, GstMemory ** parent)
{
  GstMemory *mcur, *mprv;
  gboolean have_offset = FALSE;
  gsize i;

  mcur = mprv = NULL;

  for (i = 0; i < len; i++) {
    if (mcur)
      mprv = mcur;
    mcur = mem[i];

    if (mprv && mcur) {
      gsize poffs;

      /* check if memory is contiguous */
      if (!gst_memory_is_span (mprv, mcur, &poffs))
        return FALSE;

      if (!have_offset) {
        if (poffset)
          *poffset = poffs;
        if (parent)
          *parent = mprv->parent;

        have_offset = TRUE;
      }
    }
  }
  return have_offset;
}

static GstMemory *
_actual_merged_memory (GstBuffer * buffer, guint idx, guint length)
{
  GstMemory **mem, *result = NULL;
  GstMemory *parent = NULL;
  gsize size, poffset = 0;

  mem = GST_BUFFER_MEM_ARRAY (buffer);

  size = gst_buffer_get_sizes_range (buffer, idx, length, NULL, NULL);

  if (G_UNLIKELY (_is_span (mem + idx, length, &poffset, &parent))) {
    if (!GST_MEMORY_IS_NO_SHARE (parent))
      result = gst_memory_share (parent, poffset, size);
    if (!result) {
      GST_CAT_DEBUG (GST_CAT_PERFORMANCE, "copy for merge %p", parent);
      result = gst_memory_copy (parent, poffset, size);
    }
  } else {
    gsize i, tocopy, left;
    GstMapInfo sinfo, dinfo;
    guint8 *ptr;

    result = gst_allocator_alloc (NULL, size, NULL);
    if (result == NULL || !gst_memory_map (result, &dinfo, GST_MAP_WRITE)) {
      GST_CAT_ERROR (GST_CAT_BUFFER, "Failed to map memory writable");
      if (result)
        gst_memory_unref (result);
      return NULL;
    }

    ptr = dinfo.data;
    left = size;

    for (i = idx; i < (idx + length) && left > 0; i++) {
      if (!gst_memory_map (mem[i], &sinfo, GST_MAP_READ)) {
        GST_CAT_ERROR (GST_CAT_BUFFER,
            "buffer %p, idx %u, length %u failed to map readable", buffer,
            idx, length);
        gst_memory_unmap (result, &dinfo);
        gst_memory_unref (result);
        return NULL;
      }
      tocopy = MIN (sinfo.size, left);
      GST_CAT_DEBUG (GST_CAT_PERFORMANCE,
          "memcpy %" G_GSIZE_FORMAT " bytes for merge %p from memory %p",
          tocopy, result, mem[i]);
      memcpy (ptr, (guint8 *) sinfo.data, tocopy);
      left -= tocopy;
      ptr += tocopy;
      gst_memory_unmap (mem[i], &sinfo);
    }
    gst_memory_unmap (result, &dinfo);
  }

  return result;
}

static inline GstMemory *
_get_merged_memory (GstBuffer * buffer, guint idx, guint length)
{
  GST_CAT_LOG (GST_CAT_BUFFER, "buffer %p, idx %u, length %u", buffer, idx,
      length);

  if (G_UNLIKELY (length == 0))
    return NULL;

  if (G_LIKELY (length == 1))
    return gst_memory_ref (GST_BUFFER_MEM_PTR (buffer, idx));

  return _actual_merged_memory (buffer, idx, length);
}


static void
_replace_memory (GstBuffer * buffer, guint len, guint idx, guint length,
    GstMemory * mem)
{
  gsize end, i;

  end = idx + length;

  GST_CAT_LOG (GST_CAT_BUFFER,
      "buffer %p replace %u-%" G_GSIZE_FORMAT " with memory %p", buffer, idx,
      end, mem);

  /* unref old memory */
  for (i = idx; i < end; i++) {
    GstMemory *old = GST_BUFFER_MEM_PTR (buffer, i);

    gst_memory_unlock (old, GST_LOCK_FLAG_EXCLUSIVE);
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (old),
        GST_MINI_OBJECT_CAST (buffer));
    gst_memory_unref (old);
  }

  if (mem != NULL) {
    /* replace with single memory */
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (mem),
        GST_MINI_OBJECT_CAST (buffer));
    gst_memory_lock (mem, GST_LOCK_FLAG_EXCLUSIVE);
    GST_BUFFER_MEM_PTR (buffer, idx) = mem;
    idx++;
    length--;
  }

  if (end < len) {
    memmove (&GST_BUFFER_MEM_PTR (buffer, idx),
        &GST_BUFFER_MEM_PTR (buffer, end), (len - end) * sizeof (gpointer));
  }
  GST_BUFFER_MEM_LEN (buffer) = len - length;
  GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_TAG_MEMORY);
}

/**
 * gst_buffer_get_flags:
 * @buffer: a #GstBuffer
 *
 * Get the #GstBufferFlags flags set on this buffer.
 *
 * Returns: the flags set on this buffer.
 *
 * Since: 1.10
 */
GstBufferFlags
gst_buffer_get_flags (GstBuffer * buffer)
{
  return (GstBufferFlags) GST_BUFFER_FLAGS (buffer);
}

/**
 * gst_buffer_has_flags:
 * @buffer: a #GstBuffer
 * @flags: the #GstBufferFlags flag to check.
 *
 * Gives the status of a specific flag on a buffer.
 *
 * Returns: %TRUE if all flags in @flags are found on @buffer.
 *
 * Since: 1.10
 */
gboolean
gst_buffer_has_flags (GstBuffer * buffer, GstBufferFlags flags)
{
  return GST_BUFFER_FLAG_IS_SET (buffer, flags);
}

/**
 * gst_buffer_set_flags:
 * @buffer: a #GstBuffer
 * @flags: the #GstBufferFlags to set.
 *
 * Sets one or more buffer flags on a buffer.
 *
 * Returns: %TRUE if @flags were successfully set on buffer.
 *
 * Since: 1.10
 */
gboolean
gst_buffer_set_flags (GstBuffer * buffer, GstBufferFlags flags)
{
  GST_BUFFER_FLAG_SET (buffer, flags);
  return TRUE;
}

/**
 * gst_buffer_unset_flags:
 * @buffer: a #GstBuffer
 * @flags: the #GstBufferFlags to clear
 *
 * Clears one or more buffer flags.
 *
 * Returns: true if @flags is successfully cleared from buffer.
 *
 * Since: 1.10
 */
gboolean
gst_buffer_unset_flags (GstBuffer * buffer, GstBufferFlags flags)
{
  GST_BUFFER_FLAG_UNSET (buffer, flags);
  return TRUE;
}



/* transfer full for return and transfer none for @mem */
static inline GstMemory *
_memory_get_exclusive_reference (GstMemory * mem)
{
  GstMemory *ret = NULL;

  if (gst_memory_lock (mem, GST_LOCK_FLAG_EXCLUSIVE)) {
    ret = gst_memory_ref (mem);
  } else {
    /* we cannot take another exclusive lock as the memory is already
     * locked WRITE + EXCLUSIVE according to part-miniobject.txt */
    ret = gst_memory_copy (mem, 0, -1);

    if (ret) {
      if (!gst_memory_lock (ret, GST_LOCK_FLAG_EXCLUSIVE)) {
        gst_memory_unref (ret);
        ret = NULL;
      }
    }
  }

  if (!ret)
    GST_CAT_WARNING (GST_CAT_BUFFER, "Failed to acquire an exclusive lock for "
        "memory %p", mem);

  return ret;
}

static inline void
_memory_add (GstBuffer * buffer, gint idx, GstMemory * mem)
{
  guint i, len = GST_BUFFER_MEM_LEN (buffer);

  GST_CAT_LOG (GST_CAT_BUFFER, "buffer %p, idx %d, mem %p", buffer, idx, mem);

  if (G_UNLIKELY (len >= GST_BUFFER_MEM_MAX)) {
    /* too many buffer, span them. */
    /* FIXME, there is room for improvement here: We could only try to merge
     * 2 buffers to make some room. If we can't efficiently merge 2 buffers we
     * could try to only merge the two smallest buffers to avoid memcpy, etc. */
    GST_CAT_DEBUG (GST_CAT_PERFORMANCE, "memory array overflow in buffer %p",
        buffer);
    _replace_memory (buffer, len, 0, len, _get_merged_memory (buffer, 0, len));
    /* we now have 1 single spanned buffer */
    len = 1;
  }

  if (idx == -1)
    idx = len;

  for (i = len; i > idx; i--) {
    /* move buffers to insert, FIXME, we need to insert first and then merge */
    GST_BUFFER_MEM_PTR (buffer, i) = GST_BUFFER_MEM_PTR (buffer, i - 1);
  }
  /* and insert the new buffer */
  GST_BUFFER_MEM_PTR (buffer, idx) = mem;
  GST_BUFFER_MEM_LEN (buffer) = len + 1;
  gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (mem),
      GST_MINI_OBJECT_CAST (buffer));

  GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_TAG_MEMORY);
}

GST_DEFINE_MINI_OBJECT_TYPE (GstBuffer, gst_buffer);

void
_priv_gst_buffer_initialize (void)
{
  _gst_buffer_type = gst_buffer_get_type ();

#ifdef NO_64BIT_ATOMIC_INT_FOR_PLATFORM
  GST_CAT_WARNING (GST_CAT_PERFORMANCE,
      "No 64-bit atomic int defined for this platform/toolchain!");
#endif
}

/**
 * gst_buffer_get_max_memory:
 *
 * Get the maximum amount of memory blocks that a buffer can hold. This is a
 * compile time constant that can be queried with the function.
 *
 * When more memory blocks are added, existing memory blocks will be merged
 * together to make room for the new block.
 *
 * Returns: the maximum amount of memory blocks that a buffer can hold.
 *
 * Since: 1.2
 */
guint
gst_buffer_get_max_memory (void)
{
  return GST_BUFFER_MEM_MAX;
}

/**
 * gst_buffer_copy_into:
 * @dest: a destination #GstBuffer
 * @src: a source #GstBuffer
 * @flags: flags indicating what metadata fields should be copied.
 * @offset: offset to copy from
 * @size: total size to copy. If -1, all data is copied.
 *
 * Copies the information from @src into @dest.
 *
 * If @dest already contains memory and @flags contains GST_BUFFER_COPY_MEMORY,
 * the memory from @src will be appended to @dest.
 *
 * @flags indicate which fields will be copied.
 *
 * Returns: %TRUE if the copying succeeded, %FALSE otherwise.
 */
gboolean
gst_buffer_copy_into (GstBuffer * dest, GstBuffer * src,
    GstBufferCopyFlags flags, gsize offset, gsize size)
{
  GstMetaItem *walk;
  gsize bufsize;
  gboolean region = FALSE;

  g_return_val_if_fail (dest != NULL, FALSE);
  g_return_val_if_fail (src != NULL, FALSE);

  /* nothing to copy if the buffers are the same */
  if (G_UNLIKELY (dest == src))
    return TRUE;

  g_return_val_if_fail (gst_buffer_is_writable (dest), FALSE);

  bufsize = gst_buffer_get_size (src);
  g_return_val_if_fail (bufsize >= offset, FALSE);
  if (offset > 0)
    region = TRUE;
  if (size == -1)
    size = bufsize - offset;
  if (size < bufsize)
    region = TRUE;
  g_return_val_if_fail (bufsize >= offset + size, FALSE);

  GST_CAT_LOG (GST_CAT_BUFFER, "copy %p to %p, offset %" G_GSIZE_FORMAT
      "-%" G_GSIZE_FORMAT "/%" G_GSIZE_FORMAT, src, dest, offset, size,
      bufsize);

  if (flags & GST_BUFFER_COPY_FLAGS) {
    /* copy flags */
    guint flags_mask = ~GST_BUFFER_FLAG_TAG_MEMORY;

    GST_MINI_OBJECT_FLAGS (dest) =
        (GST_MINI_OBJECT_FLAGS (src) & flags_mask) |
        (GST_MINI_OBJECT_FLAGS (dest) & ~flags_mask);
  }

  if (flags & GST_BUFFER_COPY_TIMESTAMPS) {
    if (offset == 0) {
      GST_BUFFER_PTS (dest) = GST_BUFFER_PTS (src);
      GST_BUFFER_DTS (dest) = GST_BUFFER_DTS (src);
      GST_BUFFER_OFFSET (dest) = GST_BUFFER_OFFSET (src);
      if (size == bufsize) {
        GST_BUFFER_DURATION (dest) = GST_BUFFER_DURATION (src);
        GST_BUFFER_OFFSET_END (dest) = GST_BUFFER_OFFSET_END (src);
      }
    } else {
      GST_BUFFER_PTS (dest) = GST_CLOCK_TIME_NONE;
      GST_BUFFER_DTS (dest) = GST_CLOCK_TIME_NONE;
      GST_BUFFER_DURATION (dest) = GST_CLOCK_TIME_NONE;
      GST_BUFFER_OFFSET (dest) = GST_BUFFER_OFFSET_NONE;
      GST_BUFFER_OFFSET_END (dest) = GST_BUFFER_OFFSET_NONE;
    }
  }

  if (flags & GST_BUFFER_COPY_MEMORY) {
    gsize skip, left, len, dest_len, i, bsize;
    gboolean deep;

    deep = flags & GST_BUFFER_COPY_DEEP;

    len = GST_BUFFER_MEM_LEN (src);
    dest_len = GST_BUFFER_MEM_LEN (dest);
    left = size;
    skip = offset;

    /* copy and make regions of the memory */
    for (i = 0; i < len && left > 0; i++) {
      GstMemory *mem = GST_BUFFER_MEM_PTR (src, i);

      bsize = mem->size;

      if (bsize <= skip) {
        /* don't copy buffer */
        skip -= bsize;
      } else {
        GstMemory *newmem = NULL;
        gsize tocopy;

        tocopy = MIN (bsize - skip, left);

        if (tocopy < bsize && !deep && !GST_MEMORY_IS_NO_SHARE (mem)) {
          /* we need to clip something */
          newmem = gst_memory_share (mem, skip, tocopy);
          if (newmem) {
            gst_memory_lock (newmem, GST_LOCK_FLAG_EXCLUSIVE);
            skip = 0;
          }
        }

        if (deep || GST_MEMORY_IS_NO_SHARE (mem) || (!newmem && tocopy < bsize)) {
          /* deep copy or we're not allowed to share this memory
           * between buffers, always copy then */
          newmem = gst_memory_copy (mem, skip, tocopy);
          if (newmem) {
            gst_memory_lock (newmem, GST_LOCK_FLAG_EXCLUSIVE);
            skip = 0;
          }
        } else if (!newmem) {
          newmem = _memory_get_exclusive_reference (mem);
        }

        if (!newmem) {
          gst_buffer_remove_memory_range (dest, dest_len, -1);
          return FALSE;
        }

        _memory_add (dest, -1, newmem);
        left -= tocopy;
      }
    }
    if (flags & GST_BUFFER_COPY_MERGE) {
      GstMemory *mem;

      len = GST_BUFFER_MEM_LEN (dest);
      mem = _get_merged_memory (dest, 0, len);
      if (!mem) {
        gst_buffer_remove_memory_range (dest, dest_len, -1);
        return FALSE;
      }
      _replace_memory (dest, len, 0, len, mem);
    }
  }

  if (flags & GST_BUFFER_COPY_META) {
    /* NOTE: GstGLSyncMeta copying relies on the meta
     *       being copied now, after the buffer data,
     *       so this has to happen last */
    for (walk = GST_BUFFER_META (src); walk; walk = walk->next) {
      GstMeta *meta = &walk->meta;
      const GstMetaInfo *info = meta->info;

      /* Don't copy memory metas if we only copied part of the buffer, didn't
       * copy memories or merged memories. In all these cases the memory
       * structure has changed and the memory meta becomes meaningless.
       */
      if ((region || !(flags & GST_BUFFER_COPY_MEMORY)
              || (flags & GST_BUFFER_COPY_MERGE))
          && gst_meta_api_type_has_tag (info->api, _gst_meta_tag_memory)) {
        GST_CAT_DEBUG (GST_CAT_BUFFER,
            "don't copy memory meta %p of API type %s", meta,
            g_type_name (info->api));
      } else if (info->transform_func) {
        GstMetaTransformCopy copy_data;

        copy_data.region = region;
        copy_data.offset = offset;
        copy_data.size = size;

        if (!info->transform_func (dest, meta, src,
                _gst_meta_transform_copy, &copy_data)) {
          GST_CAT_ERROR (GST_CAT_BUFFER,
              "failed to copy meta %p of API type %s", meta,
              g_type_name (info->api));
        }
      }
    }
  }

  return TRUE;
}

static GstBuffer *
gst_buffer_copy_with_flags (const GstBuffer * buffer, GstBufferCopyFlags flags)
{
  GstBuffer *copy;

  g_return_val_if_fail (buffer != NULL, NULL);

  /* create a fresh new buffer */
  copy = gst_buffer_new ();

  /* copy what the 'flags' want from our parent */
  /* FIXME why we can't pass const to gst_buffer_copy_into() ? */
  if (!gst_buffer_copy_into (copy, (GstBuffer *) buffer, flags, 0, -1))
    gst_buffer_replace (&copy, NULL);

  if (copy)
    GST_BUFFER_FLAG_UNSET (copy, GST_BUFFER_FLAG_TAG_MEMORY);

  return copy;
}

static GstBuffer *
_gst_buffer_copy (const GstBuffer * buffer)
{
  return gst_buffer_copy_with_flags (buffer, GST_BUFFER_COPY_ALL);
}

/**
 * gst_buffer_copy_deep:
 * @buf: a #GstBuffer.
 *
 * Create a copy of the given buffer. This will make a newly allocated
 * copy of the data the source buffer contains.
 *
 * Returns: (transfer full): a new copy of @buf.
 *
 * Since: 1.6
 */
GstBuffer *
gst_buffer_copy_deep (const GstBuffer * buffer)
{
  return gst_buffer_copy_with_flags (buffer,
      GST_BUFFER_COPY_ALL | GST_BUFFER_COPY_DEEP);
}

/* the default dispose function revives the buffer and returns it to the
 * pool when there is a pool */
static gboolean
_gst_buffer_dispose (GstBuffer * buffer)
{
  GstBufferPool *pool;

  /* no pool, do free */
  if ((pool = buffer->pool) == NULL)
    return TRUE;

  /* keep the buffer alive */
  gst_buffer_ref (buffer);
  /* return the buffer to the pool */
  GST_CAT_LOG (GST_CAT_BUFFER, "release %p to pool %p", buffer, pool);
  gst_buffer_pool_release_buffer (pool, buffer);

  return FALSE;
}

static void
_gst_buffer_free (GstBuffer * buffer)
{
  GstMetaItem *walk, *next;
  guint i, len;
  gsize msize;

  g_return_if_fail (buffer != NULL);

  GST_CAT_LOG (GST_CAT_BUFFER, "finalize %p", buffer);

  /* free metadata */
  for (walk = GST_BUFFER_META (buffer); walk; walk = next) {
    GstMeta *meta = &walk->meta;
    const GstMetaInfo *info = meta->info;

    /* call free_func if any */
    if (info->free_func)
      info->free_func (meta, buffer);

    next = walk->next;
    /* and free the slice */
    g_slice_free1 (ITEM_SIZE (info), walk);
  }

  /* get the size, when unreffing the memory, we could also unref the buffer
   * itself */
  msize = GST_BUFFER_SLICE_SIZE (buffer);

  /* free our memory */
  len = GST_BUFFER_MEM_LEN (buffer);
  for (i = 0; i < len; i++) {
    gst_memory_unlock (GST_BUFFER_MEM_PTR (buffer, i), GST_LOCK_FLAG_EXCLUSIVE);
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (GST_BUFFER_MEM_PTR
            (buffer, i)), GST_MINI_OBJECT_CAST (buffer));
    gst_memory_unref (GST_BUFFER_MEM_PTR (buffer, i));
  }

  /* we set msize to 0 when the buffer is part of the memory block */
  if (msize) {
#ifdef USE_POISONING
    memset (buffer, 0xff, msize);
#endif
    g_slice_free1 (msize, buffer);
  } else {
    gst_memory_unref (GST_BUFFER_BUFMEM (buffer));
  }
}

static void
gst_buffer_init (GstBufferImpl * buffer, gsize size)
{
  gst_mini_object_init (GST_MINI_OBJECT_CAST (buffer), 0, _gst_buffer_type,
      (GstMiniObjectCopyFunction) _gst_buffer_copy,
      (GstMiniObjectDisposeFunction) _gst_buffer_dispose,
      (GstMiniObjectFreeFunction) _gst_buffer_free);

  GST_BUFFER_SLICE_SIZE (buffer) = size;

  GST_BUFFER (buffer)->pool = NULL;
  GST_BUFFER_PTS (buffer) = GST_CLOCK_TIME_NONE;
  GST_BUFFER_DTS (buffer) = GST_CLOCK_TIME_NONE;
  GST_BUFFER_DURATION (buffer) = GST_CLOCK_TIME_NONE;
  GST_BUFFER_OFFSET (buffer) = GST_BUFFER_OFFSET_NONE;
  GST_BUFFER_OFFSET_END (buffer) = GST_BUFFER_OFFSET_NONE;

  GST_BUFFER_MEM_LEN (buffer) = 0;
  GST_BUFFER_META (buffer) = NULL;
}

/**
 * gst_buffer_new:
 *
 * Creates a newly allocated buffer without any data.
 *
 * MT safe.
 *
 * Returns: (transfer full): the new #GstBuffer.
 */
GstBuffer *
gst_buffer_new (void)
{
  GstBufferImpl *newbuf;

  newbuf = g_slice_new (GstBufferImpl);
  GST_CAT_LOG (GST_CAT_BUFFER, "new %p", newbuf);

  gst_buffer_init (newbuf, sizeof (GstBufferImpl));

  return GST_BUFFER_CAST (newbuf);
}

/**
 * gst_buffer_new_allocate:
 * @allocator: (transfer none) (allow-none): the #GstAllocator to use, or %NULL to use the
 *     default allocator
 * @size: the size in bytes of the new buffer's data.
 * @params: (transfer none) (allow-none): optional parameters
 *
 * Tries to create a newly allocated buffer with data of the given size and
 * extra parameters from @allocator. If the requested amount of memory can't be
 * allocated, %NULL will be returned. The allocated buffer memory is not cleared.
 *
 * When @allocator is %NULL, the default memory allocator will be used.
 *
 * Note that when @size == 0, the buffer will not have memory associated with it.
 *
 * MT safe.
 *
 * Returns: (transfer full) (nullable): a new #GstBuffer, or %NULL if
 *     the memory couldn't be allocated.
 */
GstBuffer *
gst_buffer_new_allocate (GstAllocator * allocator, gsize size,
    GstAllocationParams * params)
{
  GstBuffer *newbuf;
  GstMemory *mem;
#if 0
  guint8 *data;
  gsize asize;
#endif

#if 1
  if (size > 0) {
    mem = gst_allocator_alloc (allocator, size, params);
    if (G_UNLIKELY (mem == NULL))
      goto no_memory;
  } else {
    mem = NULL;
  }

  newbuf = gst_buffer_new ();

  if (mem != NULL) {
    gst_memory_lock (mem, GST_LOCK_FLAG_EXCLUSIVE);
    _memory_add (newbuf, -1, mem);
  }

  GST_CAT_LOG (GST_CAT_BUFFER,
      "new buffer %p of size %" G_GSIZE_FORMAT " from allocator %p", newbuf,
      size, allocator);
#endif

#if 0
  asize = sizeof (GstBufferImpl) + size;
  data = g_slice_alloc (asize);
  if (G_UNLIKELY (data == NULL))
    goto no_memory;

  newbuf = GST_BUFFER_CAST (data);

  gst_buffer_init ((GstBufferImpl *) data, asize);
  if (size > 0) {
    mem = gst_memory_new_wrapped (0, data + sizeof (GstBufferImpl), NULL,
        size, 0, size);
    _memory_add (newbuf, -1, mem, TRUE);
  }
#endif

#if 0
  /* allocate memory and buffer, it might be interesting to do this but there
   * are many complications. We need to keep the memory mapped to access the
   * buffer fields and the memory for the buffer might be just very slow. We
   * also need to do some more magic to get the alignment right. */
  asize = sizeof (GstBufferImpl) + size;
  mem = gst_allocator_alloc (allocator, asize, align);
  if (G_UNLIKELY (mem == NULL))
    goto no_memory;

  /* map the data part and init the buffer in it, set the buffer size to 0 so
   * that a finalize won't free the buffer */
  data = gst_memory_map (mem, &asize, NULL, GST_MAP_WRITE);
  gst_buffer_init ((GstBufferImpl *) data, 0);
  gst_memory_unmap (mem);

  /* strip off the buffer */
  gst_memory_resize (mem, sizeof (GstBufferImpl), size);

  newbuf = GST_BUFFER_CAST (data);
  GST_BUFFER_BUFMEM (newbuf) = mem;

  if (size > 0)
    _memory_add (newbuf, -1, gst_memory_ref (mem), TRUE);
#endif
  GST_BUFFER_FLAG_UNSET (newbuf, GST_BUFFER_FLAG_TAG_MEMORY);

  return newbuf;

  /* ERRORS */
no_memory:
  {
    GST_CAT_WARNING (GST_CAT_BUFFER,
        "failed to allocate %" G_GSIZE_FORMAT " bytes", size);
    return NULL;
  }
}

/**
 * gst_buffer_new_wrapped_full:
 * @flags: #GstMemoryFlags
 * @data: (array length=size) (element-type guint8) (transfer none): data to wrap
 * @maxsize: allocated size of @data
 * @offset: offset in @data
 * @size: size of valid data
 * @user_data: (allow-none): user_data
 * @notify: (allow-none) (scope async) (closure user_data): called with @user_data when the memory is freed
 *
 * Allocate a new buffer that wraps the given memory. @data must point to
 * @maxsize of memory, the wrapped buffer will have the region from @offset and
 * @size visible.
 *
 * When the buffer is destroyed, @notify will be called with @user_data.
 *
 * The prefix/padding must be filled with 0 if @flags contains
 * #GST_MEMORY_FLAG_ZERO_PREFIXED and #GST_MEMORY_FLAG_ZERO_PADDED respectively.
 *
 * Returns: (transfer full): a new #GstBuffer
 */
GstBuffer *
gst_buffer_new_wrapped_full (GstMemoryFlags flags, gpointer data,
    gsize maxsize, gsize offset, gsize size, gpointer user_data,
    GDestroyNotify notify)
{
  GstMemory *mem;
  GstBuffer *newbuf;

  newbuf = gst_buffer_new ();
  mem =
      gst_memory_new_wrapped (flags, data, maxsize, offset, size, user_data,
      notify);
  gst_memory_lock (mem, GST_LOCK_FLAG_EXCLUSIVE);
  _memory_add (newbuf, -1, mem);
  GST_BUFFER_FLAG_UNSET (newbuf, GST_BUFFER_FLAG_TAG_MEMORY);

  return newbuf;
}

/**
 * gst_buffer_new_wrapped:
 * @data: (array length=size) (element-type guint8) (transfer full): data to wrap
 * @size: allocated size of @data
 *
 * Creates a new buffer that wraps the given @data. The memory will be freed
 * with g_free and will be marked writable.
 *
 * MT safe.
 *
 * Returns: (transfer full): a new #GstBuffer
 */
GstBuffer *
gst_buffer_new_wrapped (gpointer data, gsize size)
{
  return gst_buffer_new_wrapped_full (0, data, size, 0, size, data, g_free);
}

/**
 * gst_buffer_new_wrapped_bytes:
 * @bytes: (transfer none): a #GBytes to wrap
 *
 * Creates a new #GstBuffer that wraps the given @bytes. The data inside
 * @bytes cannot be %NULL and the resulting buffer will be marked as read only.
 *
 * MT safe.
 *
 * Returns: (transfer full): a new #GstBuffer wrapping @bytes
 *
 * Since: 1.16
 */
GstBuffer *
gst_buffer_new_wrapped_bytes (GBytes * bytes)
{
  guint8 *bytes_data;
  gsize size;

  g_return_val_if_fail (bytes != NULL, NULL);
  bytes_data = (guint8 *) g_bytes_get_data (bytes, &size);
  g_return_val_if_fail (bytes_data != NULL, NULL);

  return gst_buffer_new_wrapped_full (GST_MEMORY_FLAG_READONLY, bytes_data,
      size, 0, size, g_bytes_ref (bytes), (GDestroyNotify) g_bytes_unref);
}

/**
 * gst_buffer_n_memory:
 * @buffer: a #GstBuffer.
 *
 * Get the amount of memory blocks that this buffer has. This amount is never
 * larger than what gst_buffer_get_max_memory() returns.
 *
 * Returns: the number of memory blocks this buffer is made of.
 */
guint
gst_buffer_n_memory (GstBuffer * buffer)
{
  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);

  return GST_BUFFER_MEM_LEN (buffer);
}

/**
 * gst_buffer_prepend_memory:
 * @buffer: a #GstBuffer.
 * @mem: (transfer full): a #GstMemory.
 *
 * Prepend the memory block @mem to @buffer. This function takes
 * ownership of @mem and thus doesn't increase its refcount.
 *
 * This function is identical to gst_buffer_insert_memory() with an index of 0.
 * See gst_buffer_insert_memory() for more details.
 */
void
gst_buffer_prepend_memory (GstBuffer * buffer, GstMemory * mem)
{
  gst_buffer_insert_memory (buffer, 0, mem);
}

/**
 * gst_buffer_append_memory:
 * @buffer: a #GstBuffer.
 * @mem: (transfer full): a #GstMemory.
 *
 * Append the memory block @mem to @buffer. This function takes
 * ownership of @mem and thus doesn't increase its refcount.
 *
 * This function is identical to gst_buffer_insert_memory() with an index of -1.
 * See gst_buffer_insert_memory() for more details.
 */
void
gst_buffer_append_memory (GstBuffer * buffer, GstMemory * mem)
{
  gst_buffer_insert_memory (buffer, -1, mem);
}

/**
 * gst_buffer_insert_memory:
 * @buffer: a #GstBuffer.
 * @idx: the index to add the memory at, or -1 to append it to the end
 * @mem: (transfer full): a #GstMemory.
 *
 * Insert the memory block @mem to @buffer at @idx. This function takes ownership
 * of @mem and thus doesn't increase its refcount.
 *
 * Only gst_buffer_get_max_memory() can be added to a buffer. If more memory is
 * added, existing memory blocks will automatically be merged to make room for
 * the new memory.
 */
void
gst_buffer_insert_memory (GstBuffer * buffer, gint idx, GstMemory * mem)
{
  GstMemory *tmp;

  g_return_if_fail (GST_IS_BUFFER (buffer));
  g_return_if_fail (gst_buffer_is_writable (buffer));
  g_return_if_fail (mem != NULL);
  g_return_if_fail (idx == -1 ||
      (idx >= 0 && idx <= GST_BUFFER_MEM_LEN (buffer)));

  tmp = _memory_get_exclusive_reference (mem);
  g_return_if_fail (tmp != NULL);
  gst_memory_unref (mem);
  _memory_add (buffer, idx, tmp);
}

static GstMemory *
_get_mapped (GstBuffer * buffer, guint idx, GstMapInfo * info,
    GstMapFlags flags)
{
  GstMemory *mem, *mapped;

  mem = gst_memory_ref (GST_BUFFER_MEM_PTR (buffer, idx));

  mapped = gst_memory_make_mapped (mem, info, flags);

  if (mapped != mem) {
    /* memory changed, lock new memory */
    gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (mapped),
        GST_MINI_OBJECT_CAST (buffer));
    gst_memory_lock (mapped, GST_LOCK_FLAG_EXCLUSIVE);
    GST_BUFFER_MEM_PTR (buffer, idx) = mapped;
    /* unlock old memory */
    gst_memory_unlock (mem, GST_LOCK_FLAG_EXCLUSIVE);
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (mem),
        GST_MINI_OBJECT_CAST (buffer));
    GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_TAG_MEMORY);
  }
  gst_memory_unref (mem);

  return mapped;
}

/**
 * gst_buffer_peek_memory:
 * @buffer: a #GstBuffer.
 * @idx: an index
 *
 * Get the memory block at @idx in @buffer. The memory block stays valid until
 * the memory block in @buffer is removed, replaced or merged, typically with
 * any call that modifies the memory in @buffer.
 *
 * Returns: (transfer none) (nullable): the #GstMemory at @idx.
 */
GstMemory *
gst_buffer_peek_memory (GstBuffer * buffer, guint idx)
{
  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);
  g_return_val_if_fail (idx < GST_BUFFER_MEM_LEN (buffer), NULL);

  return GST_BUFFER_MEM_PTR (buffer, idx);
}

/**
 * gst_buffer_get_memory:
 * @buffer: a #GstBuffer.
 * @idx: an index
 *
 * Get the memory block at index @idx in @buffer.
 *
 * Returns: (transfer full) (nullable): a #GstMemory that contains the data of the
 * memory block at @idx. Use gst_memory_unref () after usage.
 */
GstMemory *
gst_buffer_get_memory (GstBuffer * buffer, guint idx)
{
  return gst_buffer_get_memory_range (buffer, idx, 1);
}

/**
 * gst_buffer_get_all_memory:
 * @buffer: a #GstBuffer.
 *
 * Get all the memory block in @buffer. The memory blocks will be merged
 * into one large #GstMemory.
 *
 * Returns: (transfer full) (nullable): a #GstMemory that contains the merged memory.
 * Use gst_memory_unref () after usage.
 */
GstMemory *
gst_buffer_get_all_memory (GstBuffer * buffer)
{
  return gst_buffer_get_memory_range (buffer, 0, -1);
}

/**
 * gst_buffer_get_memory_range:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length
 *
 * Get @length memory blocks in @buffer starting at @idx. The memory blocks will
 * be merged into one large #GstMemory.
 *
 * If @length is -1, all memory starting from @idx is merged.
 *
 * Returns: (transfer full) (nullable): a #GstMemory that contains the merged data of @length
 *    blocks starting at @idx. Use gst_memory_unref () after usage.
 */
GstMemory *
gst_buffer_get_memory_range (GstBuffer * buffer, guint idx, gint length)
{
  guint len;

  GST_CAT_DEBUG (GST_CAT_BUFFER, "idx %u, length %d", idx, length);

  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);
  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_val_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || (length > 0 && length + idx <= len), NULL);

  if (length == -1)
    length = len - idx;

  return _get_merged_memory (buffer, idx, length);
}

/**
 * gst_buffer_replace_memory:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @mem: (transfer full): a #GstMemory
 *
 * Replaces the memory block at index @idx in @buffer with @mem.
 */
void
gst_buffer_replace_memory (GstBuffer * buffer, guint idx, GstMemory * mem)
{
  gst_buffer_replace_memory_range (buffer, idx, 1, mem);
}

/**
 * gst_buffer_replace_all_memory:
 * @buffer: a #GstBuffer.
 * @mem: (transfer full): a #GstMemory
 *
 * Replaces all memory in @buffer with @mem.
 */
void
gst_buffer_replace_all_memory (GstBuffer * buffer, GstMemory * mem)
{
  gst_buffer_replace_memory_range (buffer, 0, -1, mem);
}

/**
 * gst_buffer_replace_memory_range:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length should not be 0
 * @mem: (transfer full): a #GstMemory
 *
 * Replaces @length memory blocks in @buffer starting at @idx with @mem.
 *
 * If @length is -1, all memory starting from @idx will be removed and
 * replaced with @mem.
 *
 * @buffer should be writable.
 */
void
gst_buffer_replace_memory_range (GstBuffer * buffer, guint idx, gint length,
    GstMemory * mem)
{
  guint len;

  g_return_if_fail (GST_IS_BUFFER (buffer));
  g_return_if_fail (gst_buffer_is_writable (buffer));

  GST_CAT_DEBUG (GST_CAT_BUFFER, "idx %u, length %d, %p", idx, length, mem);

  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || (length > 0 && length + idx <= len));

  if (length == -1)
    length = len - idx;

  _replace_memory (buffer, len, idx, length, mem);
}

/**
 * gst_buffer_remove_memory:
 * @buffer: a #GstBuffer.
 * @idx: an index
 *
 * Remove the memory block in @b at index @i.
 */
void
gst_buffer_remove_memory (GstBuffer * buffer, guint idx)
{
  gst_buffer_remove_memory_range (buffer, idx, 1);
}

/**
 * gst_buffer_remove_all_memory:
 * @buffer: a #GstBuffer.
 *
 * Remove all the memory blocks in @buffer.
 */
void
gst_buffer_remove_all_memory (GstBuffer * buffer)
{
  if (GST_BUFFER_MEM_LEN (buffer))
    gst_buffer_remove_memory_range (buffer, 0, -1);
}

/**
 * gst_buffer_remove_memory_range:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length
 *
 * Remove @length memory blocks in @buffer starting from @idx.
 *
 * @length can be -1, in which case all memory starting from @idx is removed.
 */
void
gst_buffer_remove_memory_range (GstBuffer * buffer, guint idx, gint length)
{
  guint len;

  g_return_if_fail (GST_IS_BUFFER (buffer));
  g_return_if_fail (gst_buffer_is_writable (buffer));

  GST_CAT_DEBUG (GST_CAT_BUFFER, "idx %u, length %d", idx, length);

  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || length + idx <= len);

  if (length == -1)
    length = len - idx;

  _replace_memory (buffer, len, idx, length, NULL);
}

/**
 * gst_buffer_find_memory:
 * @buffer: a #GstBuffer.
 * @offset: an offset
 * @size: a size
 * @idx: (out): pointer to index
 * @length: (out): pointer to length
 * @skip: (out): pointer to skip
 *
 * Find the memory blocks that span @size bytes starting from @offset
 * in @buffer.
 *
 * When this function returns %TRUE, @idx will contain the index of the first
 * memory block where the byte for @offset can be found and @length contains the
 * number of memory blocks containing the @size remaining bytes. @skip contains
 * the number of bytes to skip in the memory block at @idx to get to the byte
 * for @offset.
 *
 * @size can be -1 to get all the memory blocks after @idx.
 *
 * Returns: %TRUE when @size bytes starting from @offset could be found in
 * @buffer and @idx, @length and @skip will be filled.
 */
gboolean
gst_buffer_find_memory (GstBuffer * buffer, gsize offset, gsize size,
    guint * idx, guint * length, gsize * skip)
{
  guint i, len, found;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), FALSE);
  g_return_val_if_fail (idx != NULL, FALSE);
  g_return_val_if_fail (length != NULL, FALSE);
  g_return_val_if_fail (skip != NULL, FALSE);

  len = GST_BUFFER_MEM_LEN (buffer);

  found = 0;
  for (i = 0; i < len; i++) {
    GstMemory *mem;
    gsize s;

    mem = GST_BUFFER_MEM_PTR (buffer, i);
    s = mem->size;

    if (s <= offset) {
      /* block before offset, or empty block, skip */
      offset -= s;
    } else {
      /* block after offset */
      if (found == 0) {
        /* first block, remember index and offset */
        *idx = i;
        *skip = offset;
        if (size == -1) {
          /* return remaining blocks */
          *length = len - i;
          return TRUE;
        }
        s -= offset;
        offset = 0;
      }
      /* count the amount of found bytes */
      found += s;
      if (found >= size) {
        /* we have enough bytes */
        *length = i - *idx + 1;
        return TRUE;
      }
    }
  }
  return FALSE;
}

/**
 * gst_buffer_is_memory_range_writable:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length should not be 0
 *
 * Check if @length memory blocks in @buffer starting from @idx are writable.
 *
 * @length can be -1 to check all the memory blocks after @idx.
 *
 * Note that this function does not check if @buffer is writable, use
 * gst_buffer_is_writable() to check that if needed.
 *
 * Returns: %TRUE if the memory range is writable
 *
 * Since: 1.4
 */
gboolean
gst_buffer_is_memory_range_writable (GstBuffer * buffer, guint idx, gint length)
{
  guint i, len;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), FALSE);

  GST_CAT_DEBUG (GST_CAT_BUFFER, "idx %u, length %d", idx, length);

  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_val_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || (length > 0 && length + idx <= len),
      FALSE);
#ifdef GSTREAMER_LITE
  gboolean expr = (len == 0 && idx == 0 && length == -1) || (length == -1 && idx < len) || (length > 0 && length + idx <= len);
  if (!expr) {
    return FALSE;
  }
#endif // GSTREAMER_LITE

  if (length == -1)
    len -= idx;
  else
    len = length;

  for (i = 0; i < len; i++) {
    if (!gst_memory_is_writable (GST_BUFFER_MEM_PTR (buffer, i + idx)))
      return FALSE;
  }
  return TRUE;
}

/**
 * gst_buffer_is_all_memory_writable:
 * @buffer: a #GstBuffer.
 *
 * Check if all memory blocks in @buffer are writable.
 *
 * Note that this function does not check if @buffer is writable, use
 * gst_buffer_is_writable() to check that if needed.
 *
 * Returns: %TRUE if all memory blocks in @buffer are writable
 *
 * Since: 1.4
 */
gboolean
gst_buffer_is_all_memory_writable (GstBuffer * buffer)
{
  return gst_buffer_is_memory_range_writable (buffer, 0, -1);
}

/**
 * gst_buffer_get_sizes:
 * @buffer: a #GstBuffer.
 * @offset: (out) (allow-none): a pointer to the offset
 * @maxsize: (out) (allow-none): a pointer to the maxsize
 *
 * Get the total size of the memory blocks in @b.
 *
 * When not %NULL, @offset will contain the offset of the data in the
 * first memory block in @buffer and @maxsize will contain the sum of
 * the size and @offset and the amount of extra padding on the last
 * memory block.  @offset and @maxsize can be used to resize the
 * buffer memory blocks with gst_buffer_resize().
 *
 * Returns: total size of the memory blocks in @buffer.
 */
gsize
gst_buffer_get_sizes (GstBuffer * buffer, gsize * offset, gsize * maxsize)
{
  return gst_buffer_get_sizes_range (buffer, 0, -1, offset, maxsize);
}

/**
 * gst_buffer_get_size:
 * @buffer: a #GstBuffer.
 *
 * Get the total size of the memory blocks in @buffer.
 *
 * Returns: total size of the memory blocks in @buffer.
 */
gsize
gst_buffer_get_size (GstBuffer * buffer)
{
  guint i;
  gsize size, len;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);

  /* FAST PATH */
  len = GST_BUFFER_MEM_LEN (buffer);
  for (i = 0, size = 0; i < len; i++)
    size += GST_BUFFER_MEM_PTR (buffer, i)->size;
  return size;
}

/**
 * gst_buffer_get_sizes_range:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length
 * @offset: (out) (allow-none): a pointer to the offset
 * @maxsize: (out) (allow-none): a pointer to the maxsize
 *
 * Get the total size of @length memory blocks stating from @idx in @buffer.
 *
 * When not %NULL, @offset will contain the offset of the data in the
 * memory block in @buffer at @idx and @maxsize will contain the sum of the size
 * and @offset and the amount of extra padding on the memory block at @idx +
 * @length -1.
 * @offset and @maxsize can be used to resize the buffer memory blocks with
 * gst_buffer_resize_range().
 *
 * Returns: total size of @length memory blocks starting at @idx in @buffer.
 */
gsize
gst_buffer_get_sizes_range (GstBuffer * buffer, guint idx, gint length,
    gsize * offset, gsize * maxsize)
{
  guint len;
  gsize size;
  GstMemory *mem;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);
  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_val_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || (length + idx <= len), 0);
#ifdef GSTREAMER_LITE
  gboolean expr = (len == 0 && idx == 0 && length == -1) || (length == -1 && idx < len) || (length + idx <= len);
  if (!expr) {
    return 0;
  }
#endif // GSTREAMER_LITE

  if (length == -1)
    length = len - idx;

  if (G_LIKELY (length == 1)) {
    /* common case */
    mem = GST_BUFFER_MEM_PTR (buffer, idx);
    size = gst_memory_get_sizes (mem, offset, maxsize);
  } else if (offset == NULL && maxsize == NULL) {
    /* FAST PATH ! */
    guint i, end;

    size = 0;
    end = idx + length;
    for (i = idx; i < end; i++) {
      mem = GST_BUFFER_MEM_PTR (buffer, i);
      size += mem->size;
    }
  } else {
    guint i, end;
    gsize extra, offs;

    end = idx + length;
    size = offs = extra = 0;
    for (i = idx; i < end; i++) {
      gsize s, o, ms;

      mem = GST_BUFFER_MEM_PTR (buffer, i);
      s = gst_memory_get_sizes (mem, &o, &ms);

      if (s) {
        if (size == 0)
          /* first size, take accumulated data before as the offset */
          offs = extra + o;
        /* add sizes */
        size += s;
        /* save the amount of data after this block */
        extra = ms - (o + s);
      } else {
        /* empty block, add as extra */
        extra += ms;
      }
    }
    if (offset)
      *offset = offs;
    if (maxsize)
      *maxsize = offs + size + extra;
  }
  return size;
}

/**
 * gst_buffer_resize:
 * @buffer: a #GstBuffer.
 * @offset: the offset adjustment
 * @size: the new size or -1 to just adjust the offset
 *
 * Set the offset and total size of the memory blocks in @buffer.
 */
void
gst_buffer_resize (GstBuffer * buffer, gssize offset, gssize size)
{
  gst_buffer_resize_range (buffer, 0, -1, offset, size);
}

/**
 * gst_buffer_set_size:
 * @buffer: a #GstBuffer.
 * @size: the new size
 *
 * Set the total size of the memory blocks in @buffer.
 */
void
gst_buffer_set_size (GstBuffer * buffer, gssize size)
{
  gst_buffer_resize_range (buffer, 0, -1, 0, size);
}

/**
 * gst_buffer_resize_range:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length
 * @offset: the offset adjustment
 * @size: the new size or -1 to just adjust the offset
 *
 * Set the total size of the @length memory blocks starting at @idx in
 * @buffer
 *
 * Returns: %TRUE if resizing succeeded, %FALSE otherwise.
 */
gboolean
gst_buffer_resize_range (GstBuffer * buffer, guint idx, gint length,
    gssize offset, gssize size)
{
  guint i, len, end;
  gsize bsize, bufsize, bufoffs, bufmax;

  g_return_val_if_fail (gst_buffer_is_writable (buffer), FALSE);
  g_return_val_if_fail (size >= -1, FALSE);
#ifdef GSTREAMER_LITE
  if (!gst_buffer_is_writable (buffer)) {
    return FALSE;
  }
  if (size < -1) {
    return FALSE;
  }
#endif // GSTREAMER_LITE

  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_val_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || (length + idx <= len), FALSE);
#ifdef GSTREAMER_LITE
  gboolean expr = (len == 0 && idx == 0 && length == -1) || (length == -1 && idx < len) || (length + idx <= len);
  if (!expr) {
    return FALSE;
  }
#endif // GSTREAMER_LITE

  if (length == -1)
    length = len - idx;

  bufsize = gst_buffer_get_sizes_range (buffer, idx, length, &bufoffs, &bufmax);

  GST_CAT_LOG (GST_CAT_BUFFER, "trim %p %" G_GSSIZE_FORMAT "-%" G_GSSIZE_FORMAT
      " size:%" G_GSIZE_FORMAT " offs:%" G_GSIZE_FORMAT " max:%"
      G_GSIZE_FORMAT, buffer, offset, size, bufsize, bufoffs, bufmax);

  /* we can't go back further than the current offset or past the end of the
   * buffer */
  g_return_val_if_fail ((offset < 0 && bufoffs >= -offset) || (offset >= 0
          && bufoffs + offset <= bufmax), FALSE);
#ifdef GSTREAMER_LITE
  expr = (offset < 0 && bufoffs >= -offset) || (offset >= 0 && bufoffs + offset <= bufmax);
  if (!expr) {
    return FALSE;
  }
#endif // GSTREAMER_LITE
  if (size == -1) {
    g_return_val_if_fail (bufsize >= offset, FALSE);
#ifdef GSTREAMER_LITE
  expr = bufsize >= offset;
  if (!expr) {
    return FALSE;
  }
#endif // GSTREAMER_LITE
    size = bufsize - offset;
  }
  g_return_val_if_fail (bufmax >= bufoffs + offset + size, FALSE);
#ifdef GSTREAMER_LITE
  expr = bufmax >= bufoffs + offset + size;
  if (!expr) {
    return FALSE;
  }
#endif // GSTREAMER_LITE

  /* no change */
  if (offset == 0 && size == bufsize)
    return TRUE;

  end = idx + length;
  /* copy and trim */
  for (i = idx; i < end; i++) {
    GstMemory *mem;
    gsize left, noffs;

    mem = GST_BUFFER_MEM_PTR (buffer, i);
    bsize = mem->size;

    noffs = 0;
    /* last buffer always gets resized to the remaining size */
    if (i + 1 == end)
      left = size;
    /* shrink buffers before the offset */
    else if ((gssize) bsize <= offset) {
      left = 0;
      noffs = offset - bsize;
      offset = 0;
    }
    /* clip other buffers */
    else
      left = MIN (bsize - offset, size);

    if (offset != 0 || left != bsize) {
      if (gst_memory_is_writable (mem)) {
        gst_memory_resize (mem, offset, left);
      } else {
        GstMemory *newmem = NULL;

        if (!GST_MEMORY_IS_NO_SHARE (mem))
          newmem = gst_memory_share (mem, offset, left);

        if (!newmem)
          newmem = gst_memory_copy (mem, offset, left);

        if (newmem == NULL)
          return FALSE;

        gst_mini_object_add_parent (GST_MINI_OBJECT_CAST (newmem),
            GST_MINI_OBJECT_CAST (buffer));
        gst_memory_lock (newmem, GST_LOCK_FLAG_EXCLUSIVE);
        GST_BUFFER_MEM_PTR (buffer, i) = newmem;
        gst_memory_unlock (mem, GST_LOCK_FLAG_EXCLUSIVE);
        gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (mem),
            GST_MINI_OBJECT_CAST (buffer));
        gst_memory_unref (mem);

        GST_BUFFER_FLAG_SET (buffer, GST_BUFFER_FLAG_TAG_MEMORY);
      }
    }

    offset = noffs;
    size -= left;
  }

  return TRUE;
}

/**
 * gst_buffer_map:
 * @buffer: a #GstBuffer.
 * @info: (out): info about the mapping
 * @flags: flags for the mapping
 *
 * This function fills @info with the #GstMapInfo of all merged memory
 * blocks in @buffer.
 *
 * @flags describe the desired access of the memory. When @flags is
 * #GST_MAP_WRITE, @buffer should be writable (as returned from
 * gst_buffer_is_writable()).
 *
 * When @buffer is writable but the memory isn't, a writable copy will
 * automatically be created and returned. The readonly copy of the
 * buffer memory will then also be replaced with this writable copy.
 *
 * The memory in @info should be unmapped with gst_buffer_unmap() after
 * usage.
 *
 * Returns: %TRUE if the map succeeded and @info contains valid data.
 */
gboolean
gst_buffer_map (GstBuffer * buffer, GstMapInfo * info, GstMapFlags flags)
{
  return gst_buffer_map_range (buffer, 0, -1, info, flags);
}

/**
 * gst_buffer_map_range:
 * @buffer: a #GstBuffer.
 * @idx: an index
 * @length: a length
 * @info: (out): info about the mapping
 * @flags: flags for the mapping
 *
 * This function fills @info with the #GstMapInfo of @length merged memory blocks
 * starting at @idx in @buffer. When @length is -1, all memory blocks starting
 * from @idx are merged and mapped.
 *
 * @flags describe the desired access of the memory. When @flags is
 * #GST_MAP_WRITE, @buffer should be writable (as returned from
 * gst_buffer_is_writable()).
 *
 * When @buffer is writable but the memory isn't, a writable copy will
 * automatically be created and returned. The readonly copy of the buffer memory
 * will then also be replaced with this writable copy.
 *
 * The memory in @info should be unmapped with gst_buffer_unmap() after usage.
 *
 * Returns: %TRUE if the map succeeded and @info contains valid
 * data.
 */
gboolean
gst_buffer_map_range (GstBuffer * buffer, guint idx, gint length,
    GstMapInfo * info, GstMapFlags flags)
{
  GstMemory *mem, *nmem;
  gboolean write, writable;
  gsize len;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), FALSE);
  g_return_val_if_fail (info != NULL, FALSE);
  len = GST_BUFFER_MEM_LEN (buffer);
  g_return_val_if_fail ((len == 0 && idx == 0 && length == -1) ||
      (length == -1 && idx < len) || (length > 0
          && length + idx <= len), FALSE);

  GST_CAT_LOG (GST_CAT_BUFFER, "buffer %p, idx %u, length %d, flags %04x",
      buffer, idx, length, flags);

  write = (flags & GST_MAP_WRITE) != 0;
  writable = gst_buffer_is_writable (buffer);

  /* check if we can write when asked for write access */
  if (G_UNLIKELY (write && !writable))
    goto not_writable;

  if (length == -1)
    length = len - idx;

  mem = _get_merged_memory (buffer, idx, length);
  if (G_UNLIKELY (mem == NULL))
    goto no_memory;

  /* now try to map */
  nmem = gst_memory_make_mapped (mem, info, flags);
  if (G_UNLIKELY (nmem == NULL))
    goto cannot_map;

  /* if we merged or when the map returned a different memory, we try to replace
   * the memory in the buffer */
  if (G_UNLIKELY (length > 1 || nmem != mem)) {
    /* if the buffer is writable, replace the memory */
    if (writable) {
      _replace_memory (buffer, len, idx, length, gst_memory_ref (nmem));
    } else {
      if (len > 1) {
        GST_CAT_DEBUG (GST_CAT_PERFORMANCE,
            "temporary mapping for memory %p in buffer %p", nmem, buffer);
      }
    }
  }
  return TRUE;

  /* ERROR */
not_writable:
  {
    GST_WARNING ("write map requested on non-writable buffer");
    g_critical ("write map requested on non-writable buffer");
    memset (info, 0, sizeof (GstMapInfo));
    return FALSE;
  }
no_memory:
  {
    /* empty buffer, we need to return NULL */
    GST_DEBUG ("can't get buffer memory");
    memset (info, 0, sizeof (GstMapInfo));
    return TRUE;
  }
cannot_map:
  {
    GST_DEBUG ("cannot map memory");
    memset (info, 0, sizeof (GstMapInfo));
    return FALSE;
  }
}

/**
 * gst_buffer_unmap:
 * @buffer: a #GstBuffer.
 * @info: a #GstMapInfo
 *
 * Release the memory previously mapped with gst_buffer_map().
 */
void
gst_buffer_unmap (GstBuffer * buffer, GstMapInfo * info)
{
  g_return_if_fail (GST_IS_BUFFER (buffer));
  g_return_if_fail (info != NULL);

  /* we need to check for NULL, it is possible that we tried to map a buffer
   * without memory and we should be able to unmap that fine */
  if (G_LIKELY (info->memory)) {
    gst_memory_unmap (info->memory, info);
    gst_memory_unref (info->memory);
  }
}

/**
 * gst_buffer_fill:
 * @buffer: a #GstBuffer.
 * @offset: the offset to fill
 * @src: (array length=size) (element-type guint8): the source address
 * @size: the size to fill
 *
 * Copy @size bytes from @src to @buffer at @offset.
 *
 * Returns: The amount of bytes copied. This value can be lower than @size
 *    when @buffer did not contain enough data.
 */
gsize
gst_buffer_fill (GstBuffer * buffer, gsize offset, gconstpointer src,
    gsize size)
{
  gsize i, len, left;
  const guint8 *ptr = src;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);
  g_return_val_if_fail (gst_buffer_is_writable (buffer), 0);
  g_return_val_if_fail (src != NULL || size == 0, 0);

  GST_CAT_LOG (GST_CAT_BUFFER,
      "buffer %p, offset %" G_GSIZE_FORMAT ", size %" G_GSIZE_FORMAT, buffer,
      offset, size);

  len = GST_BUFFER_MEM_LEN (buffer);
  left = size;

  for (i = 0; i < len && left > 0; i++) {
    GstMapInfo info;
    gsize tocopy;
    GstMemory *mem;

    mem = _get_mapped (buffer, i, &info, GST_MAP_WRITE);
    if (info.size > offset) {
      /* we have enough */
      tocopy = MIN (info.size - offset, left);
      memcpy ((guint8 *) info.data + offset, ptr, tocopy);
      left -= tocopy;
      ptr += tocopy;
      offset = 0;
    } else {
      /* offset past buffer, skip */
      offset -= info.size;
    }
    gst_memory_unmap (mem, &info);
  }
  return size - left;
}

/**
 * gst_buffer_extract:
 * @buffer: a #GstBuffer.
 * @offset: the offset to extract
 * @dest: (out caller-allocates) (array length=size) (element-type guint8):
 *     the destination address
 * @size: the size to extract
 *
 * Copy @size bytes starting from @offset in @buffer to @dest.
 *
 * Returns: The amount of bytes extracted. This value can be lower than @size
 *    when @buffer did not contain enough data.
 */
gsize
gst_buffer_extract (GstBuffer * buffer, gsize offset, gpointer dest, gsize size)
{
  gsize i, len, left;
  guint8 *ptr = dest;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);
  g_return_val_if_fail (dest != NULL, 0);

  GST_CAT_LOG (GST_CAT_BUFFER,
      "buffer %p, offset %" G_GSIZE_FORMAT ", size %" G_GSIZE_FORMAT, buffer,
      offset, size);

  len = GST_BUFFER_MEM_LEN (buffer);
  left = size;

  for (i = 0; i < len && left > 0; i++) {
    GstMapInfo info;
    gsize tocopy;
    GstMemory *mem;

    mem = _get_mapped (buffer, i, &info, GST_MAP_READ);
    if (info.size > offset) {
      /* we have enough */
      tocopy = MIN (info.size - offset, left);
      memcpy (ptr, (guint8 *) info.data + offset, tocopy);
      left -= tocopy;
      ptr += tocopy;
      offset = 0;
    } else {
      /* offset past buffer, skip */
      offset -= info.size;
    }
    gst_memory_unmap (mem, &info);
  }
  return size - left;
}

/**
 * gst_buffer_memcmp:
 * @buffer: a #GstBuffer.
 * @offset: the offset in @buffer
 * @mem: (array length=size) (element-type guint8): the memory to compare
 * @size: the size to compare
 *
 * Compare @size bytes starting from @offset in @buffer with the memory in @mem.
 *
 * Returns: 0 if the memory is equal.
 */
gint
gst_buffer_memcmp (GstBuffer * buffer, gsize offset, gconstpointer mem,
    gsize size)
{
  gsize i, len;
  const guint8 *ptr = mem;
  gint res = 0;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);
  g_return_val_if_fail (mem != NULL, 0);

  GST_CAT_LOG (GST_CAT_BUFFER,
      "buffer %p, offset %" G_GSIZE_FORMAT ", size %" G_GSIZE_FORMAT, buffer,
      offset, size);

  if (G_UNLIKELY (gst_buffer_get_size (buffer) < offset + size))
    return -1;

  len = GST_BUFFER_MEM_LEN (buffer);

  for (i = 0; i < len && size > 0 && res == 0; i++) {
    GstMapInfo info;
    gsize tocmp;
    GstMemory *mem;

    mem = _get_mapped (buffer, i, &info, GST_MAP_READ);
    if (info.size > offset) {
      /* we have enough */
      tocmp = MIN (info.size - offset, size);
      res = memcmp (ptr, (guint8 *) info.data + offset, tocmp);
      size -= tocmp;
      ptr += tocmp;
      offset = 0;
    } else {
      /* offset past buffer, skip */
      offset -= info.size;
    }
    gst_memory_unmap (mem, &info);
  }
  return res;
}

/**
 * gst_buffer_memset:
 * @buffer: a #GstBuffer.
 * @offset: the offset in @buffer
 * @val: the value to set
 * @size: the size to set
 *
 * Fill @buf with @size bytes with @val starting from @offset.
 *
 * Returns: The amount of bytes filled. This value can be lower than @size
 *    when @buffer did not contain enough data.
 */
gsize
gst_buffer_memset (GstBuffer * buffer, gsize offset, guint8 val, gsize size)
{
  gsize i, len, left;

  g_return_val_if_fail (GST_IS_BUFFER (buffer), 0);
  g_return_val_if_fail (gst_buffer_is_writable (buffer), 0);

  GST_CAT_LOG (GST_CAT_BUFFER,
      "buffer %p, offset %" G_GSIZE_FORMAT ", val %02x, size %" G_GSIZE_FORMAT,
      buffer, offset, val, size);

  len = GST_BUFFER_MEM_LEN (buffer);
  left = size;

  for (i = 0; i < len && left > 0; i++) {
    GstMapInfo info;
    gsize toset;
    GstMemory *mem;

    mem = _get_mapped (buffer, i, &info, GST_MAP_WRITE);
    if (info.size > offset) {
      /* we have enough */
      toset = MIN (info.size - offset, left);
      memset ((guint8 *) info.data + offset, val, toset);
      left -= toset;
      offset = 0;
    } else {
      /* offset past buffer, skip */
      offset -= info.size;
    }
    gst_memory_unmap (mem, &info);
  }
  return size - left;
}

/**
 * gst_buffer_copy_region:
 * @parent: a #GstBuffer.
 * @flags: the #GstBufferCopyFlags
 * @offset: the offset into parent #GstBuffer at which the new sub-buffer
 *          begins.
 * @size: the size of the new #GstBuffer sub-buffer, in bytes. If -1, all
 *        data is copied.
 *
 * Creates a sub-buffer from @parent at @offset and @size.
 * This sub-buffer uses the actual memory space of the parent buffer.
 * This function will copy the offset and timestamp fields when the
 * offset is 0. If not, they will be set to #GST_CLOCK_TIME_NONE and
 * #GST_BUFFER_OFFSET_NONE.
 * If @offset equals 0 and @size equals the total size of @buffer, the
 * duration and offset end fields are also copied. If not they will be set
 * to #GST_CLOCK_TIME_NONE and #GST_BUFFER_OFFSET_NONE.
 *
 * MT safe.
 *
 * Returns: (transfer full): the new #GstBuffer or %NULL if the arguments were
 *     invalid.
 */
GstBuffer *
gst_buffer_copy_region (GstBuffer * buffer, GstBufferCopyFlags flags,
    gsize offset, gsize size)
{
  GstBuffer *copy;

  g_return_val_if_fail (buffer != NULL, NULL);

  /* create the new buffer */
  copy = gst_buffer_new ();

  GST_CAT_LOG (GST_CAT_BUFFER, "new region copy %p of %p %" G_GSIZE_FORMAT
      "-%" G_GSIZE_FORMAT, copy, buffer, offset, size);

  if (!gst_buffer_copy_into (copy, buffer, flags, offset, size))
    gst_buffer_replace (&copy, NULL);

  return copy;
}

/**
 * gst_buffer_append:
 * @buf1: (transfer full): the first source #GstBuffer to append.
 * @buf2: (transfer full): the second source #GstBuffer to append.
 *
 * Append all the memory from @buf2 to @buf1. The result buffer will contain a
 * concatenation of the memory of @buf1 and @buf2.
 *
 * Returns: (transfer full): the new #GstBuffer that contains the memory
 *     of the two source buffers.
 */
GstBuffer *
gst_buffer_append (GstBuffer * buf1, GstBuffer * buf2)
{
  return gst_buffer_append_region (buf1, buf2, 0, -1);
}

/**
 * gst_buffer_append_region:
 * @buf1: (transfer full): the first source #GstBuffer to append.
 * @buf2: (transfer full): the second source #GstBuffer to append.
 * @offset: the offset in @buf2
 * @size: the size or -1 of @buf2
 *
 * Append @size bytes at @offset from @buf2 to @buf1. The result buffer will
 * contain a concatenation of the memory of @buf1 and the requested region of
 * @buf2.
 *
 * Returns: (transfer full): the new #GstBuffer that contains the memory
 *     of the two source buffers.
 */
GstBuffer *
gst_buffer_append_region (GstBuffer * buf1, GstBuffer * buf2, gssize offset,
    gssize size)
{
  gsize i, len;

  g_return_val_if_fail (GST_IS_BUFFER (buf1), NULL);
  g_return_val_if_fail (GST_IS_BUFFER (buf2), NULL);

  buf1 = gst_buffer_make_writable (buf1);
  buf2 = gst_buffer_make_writable (buf2);

  gst_buffer_resize (buf2, offset, size);

  len = GST_BUFFER_MEM_LEN (buf2);
  for (i = 0; i < len; i++) {
    GstMemory *mem;

    mem = GST_BUFFER_MEM_PTR (buf2, i);
    gst_mini_object_remove_parent (GST_MINI_OBJECT_CAST (mem),
        GST_MINI_OBJECT_CAST (buf2));
    GST_BUFFER_MEM_PTR (buf2, i) = NULL;
    _memory_add (buf1, -1, mem);
  }

  GST_BUFFER_MEM_LEN (buf2) = 0;
  GST_BUFFER_FLAG_SET (buf2, GST_BUFFER_FLAG_TAG_MEMORY);
  gst_buffer_unref (buf2);

  return buf1;
}

/**
 * gst_buffer_get_meta:
 * @buffer: a #GstBuffer
 * @api: the #GType of an API
 *
 * Get the metadata for @api on buffer. When there is no such metadata, %NULL is
 * returned. If multiple metadata with the given @api are attached to this
 * buffer only the first one is returned.  To handle multiple metadata with a
 * given API use gst_buffer_iterate_meta() or gst_buffer_foreach_meta() instead
 * and check the meta->info.api member for the API type.
 *
 * Returns: (transfer none) (nullable): the metadata for @api on
 * @buffer.
 */
GstMeta *
gst_buffer_get_meta (GstBuffer * buffer, GType api)
{
  GstMetaItem *item;
  GstMeta *result = NULL;

  g_return_val_if_fail (buffer != NULL, NULL);
  g_return_val_if_fail (api != 0, NULL);

  /* find GstMeta of the requested API */
  for (item = GST_BUFFER_META (buffer); item; item = item->next) {
    GstMeta *meta = &item->meta;
    if (meta->info->api == api) {
      result = meta;
      break;
    }
  }
  return result;
}

/**
 * gst_buffer_get_n_meta:
 * @buffer: a #GstBuffer
 * @api_type: the #GType of an API
 *
 * Returns: number of metas of type @api_type on @buffer.
 *
 * Since: 1.14
 */
guint
gst_buffer_get_n_meta (GstBuffer * buffer, GType api_type)
{
  gpointer state = NULL;
  GstMeta *meta;
  guint n = 0;

  while ((meta = gst_buffer_iterate_meta_filtered (buffer, &state, api_type)))
    ++n;

  return n;
}

/**
 * gst_buffer_add_meta:
 * @buffer: a #GstBuffer
 * @info: a #GstMetaInfo
 * @params: params for @info
 *
 * Add metadata for @info to @buffer using the parameters in @params.
 *
 * Returns: (transfer none) (nullable): the metadata for the api in @info on @buffer.
 */
GstMeta *
gst_buffer_add_meta (GstBuffer * buffer, const GstMetaInfo * info,
    gpointer params)
{
  GstMetaItem *item;
  GstMeta *result = NULL;
  gsize size;

  g_return_val_if_fail (buffer != NULL, NULL);
  g_return_val_if_fail (info != NULL, NULL);
  g_return_val_if_fail (gst_buffer_is_writable (buffer), NULL);

  /* create a new slice */
  size = ITEM_SIZE (info);
  /* We warn in gst_meta_register() about metas without
   * init function but let's play safe here and prevent
   * uninitialized memory
   */
  if (!info->init_func)
    item = g_slice_alloc0 (size);
  else
    item = g_slice_alloc (size);
  result = &item->meta;
  result->info = info;
  result->flags = GST_META_FLAG_NONE;
  GST_CAT_DEBUG (GST_CAT_BUFFER,
      "alloc metadata %p (%s) of size %" G_GSIZE_FORMAT, result,
      g_type_name (info->type), info->size);

  /* call the init_func when needed */
  if (info->init_func)
    if (!info->init_func (result, params, buffer))
      goto init_failed;

  item->seq_num = gst_atomic_int64_inc (&meta_seq);
  item->next = NULL;

  if (!GST_BUFFER_META (buffer)) {
    GST_BUFFER_META (buffer) = item;
    GST_BUFFER_TAIL_META (buffer) = item;
  } else {
    GST_BUFFER_TAIL_META (buffer)->next = item;
    GST_BUFFER_TAIL_META (buffer) = item;
  }

  return result;

init_failed:
  {
    g_slice_free1 (size, item);
    return NULL;
  }
}

/**
 * gst_buffer_remove_meta:
 * @buffer: a #GstBuffer
 * @meta: a #GstMeta
 *
 * Remove the metadata for @meta on @buffer.
 *
 * Returns: %TRUE if the metadata existed and was removed, %FALSE if no such
 * metadata was on @buffer.
 */
gboolean
gst_buffer_remove_meta (GstBuffer * buffer, GstMeta * meta)
{
  GstMetaItem *walk, *prev;

  g_return_val_if_fail (buffer != NULL, FALSE);
  g_return_val_if_fail (meta != NULL, FALSE);
  g_return_val_if_fail (gst_buffer_is_writable (buffer), FALSE);
  g_return_val_if_fail (!GST_META_FLAG_IS_SET (meta, GST_META_FLAG_LOCKED),
      FALSE);

  /* find the metadata and delete */
  prev = GST_BUFFER_META (buffer);
  for (walk = prev; walk; walk = walk->next) {
    GstMeta *m = &walk->meta;
    if (m == meta) {
      const GstMetaInfo *info = meta->info;

      /* remove from list */
      if (GST_BUFFER_TAIL_META (buffer) == walk) {
        if (prev != walk)
          GST_BUFFER_TAIL_META (buffer) = prev;
        else
          GST_BUFFER_TAIL_META (buffer) = NULL;
      }

      if (GST_BUFFER_META (buffer) == walk)
        GST_BUFFER_META (buffer) = walk->next;
      else
        prev->next = walk->next;

      /* call free_func if any */
      if (info->free_func)
        info->free_func (m, buffer);

      /* and free the slice */
      g_slice_free1 (ITEM_SIZE (info), walk);
      break;
    }
    prev = walk;
  }
  return walk != NULL;
}

/**
 * gst_buffer_iterate_meta: (skip)
 * @buffer: a #GstBuffer
 * @state: (out caller-allocates): an opaque state pointer
 *
 * Retrieve the next #GstMeta after @current. If @state points
 * to %NULL, the first metadata is returned.
 *
 * @state will be updated with an opaque state pointer
 *
 * Returns: (transfer none) (nullable): The next #GstMeta or %NULL
 * when there are no more items.
 */
GstMeta *
gst_buffer_iterate_meta (GstBuffer * buffer, gpointer * state)
{
  GstMetaItem **meta;

  g_return_val_if_fail (buffer != NULL, NULL);
  g_return_val_if_fail (state != NULL, NULL);

  meta = (GstMetaItem **) state;
  if (*meta == NULL)
    /* state NULL, move to first item */
    *meta = GST_BUFFER_META (buffer);
  else
    /* state !NULL, move to next item in list */
    *meta = (*meta)->next;

  if (*meta)
    return &(*meta)->meta;
  else
    return NULL;
}

/**
 * gst_buffer_iterate_meta_filtered: (skip)
 * @buffer: a #GstBuffer
 * @state: (out caller-allocates): an opaque state pointer
 * @meta_api_type: only return #GstMeta of this type
 *
 * Retrieve the next #GstMeta of type @meta_api_type after the current one
 * according to @state. If @state points to %NULL, the first metadata of
 * type @meta_api_type is returned.
 *
 * @state will be updated with an opaque state pointer
 *
 * Returns: (transfer none) (nullable): The next #GstMeta of type
 * @meta_api_type or %NULL when there are no more items.
 *
 * Since: 1.12
 */
GstMeta *
gst_buffer_iterate_meta_filtered (GstBuffer * buffer, gpointer * state,
    GType meta_api_type)
{
  GstMetaItem **meta;

  g_return_val_if_fail (buffer != NULL, NULL);
  g_return_val_if_fail (state != NULL, NULL);

  meta = (GstMetaItem **) state;
  if (*meta == NULL)
    /* state NULL, move to first item */
    *meta = GST_BUFFER_META (buffer);
  else
    /* state !NULL, move to next item in list */
    *meta = (*meta)->next;

  while (*meta != NULL && (*meta)->meta.info->api != meta_api_type)
    *meta = (*meta)->next;

  if (*meta)
    return &(*meta)->meta;
  else
    return NULL;
}

/**
 * gst_buffer_foreach_meta:
 * @buffer: a #GstBuffer
 * @func: (scope call): a #GstBufferForeachMetaFunc to call
 * @user_data: (closure): user data passed to @func
 *
 * Call @func with @user_data for each meta in @buffer.
 *
 * @func can modify the passed meta pointer or its contents. The return value
 * of @func define if this function returns or if the remaining metadata items
 * in the buffer should be skipped.
 *
 * Returns: %FALSE when @func returned %FALSE for one of the metadata.
 */
gboolean
gst_buffer_foreach_meta (GstBuffer * buffer, GstBufferForeachMetaFunc func,
    gpointer user_data)
{
  GstMetaItem *walk, *prev, *next;
  gboolean res = TRUE;

  g_return_val_if_fail (buffer != NULL, FALSE);
  g_return_val_if_fail (func != NULL, FALSE);

  /* find the metadata and delete */
  prev = GST_BUFFER_META (buffer);
  for (walk = prev; walk; walk = next) {
    GstMeta *m, *new;

    m = new = &walk->meta;
    next = walk->next;

    res = func (buffer, &new, user_data);

    if (new == NULL) {
      const GstMetaInfo *info = m->info;

      GST_CAT_DEBUG (GST_CAT_BUFFER, "remove metadata %p (%s)", m,
          g_type_name (info->type));

      g_return_val_if_fail (gst_buffer_is_writable (buffer), FALSE);
      g_return_val_if_fail (!GST_META_FLAG_IS_SET (m, GST_META_FLAG_LOCKED),
          FALSE);

      if (GST_BUFFER_TAIL_META (buffer) == walk) {
        if (prev != walk)
          GST_BUFFER_TAIL_META (buffer) = prev;
        else
          GST_BUFFER_TAIL_META (buffer) = NULL;
      }

      /* remove from list */
      if (GST_BUFFER_META (buffer) == walk)
        prev = GST_BUFFER_META (buffer) = next;
      else
        prev->next = next;

      /* call free_func if any */
      if (info->free_func)
        info->free_func (m, buffer);

      /* and free the slice */
      g_slice_free1 (ITEM_SIZE (info), walk);
    } else {
      prev = walk;
    }
    if (!res)
      break;
  }
  return res;
}

/**
 * gst_buffer_extract_dup:
 * @buffer: a #GstBuffer
 * @offset: the offset to extract
 * @size: the size to extract
 * @dest: (array length=dest_size) (element-type guint8) (out): A pointer where
 *  the destination array will be written. Might be %NULL if the size is 0.
 * @dest_size: (out): A location where the size of @dest can be written
 *
 * Extracts a copy of at most @size bytes the data at @offset into
 * newly-allocated memory. @dest must be freed using g_free() when done.
 *
 * Since: 1.0.10
 */

void
gst_buffer_extract_dup (GstBuffer * buffer, gsize offset, gsize size,
    gpointer * dest, gsize * dest_size)
{
  gsize real_size, alloc_size;

  real_size = gst_buffer_get_size (buffer);

  alloc_size = MIN (real_size - offset, size);
  if (alloc_size == 0) {
    *dest = NULL;
    *dest_size = 0;
  } else {
    *dest = g_malloc (alloc_size);
    *dest_size = gst_buffer_extract (buffer, offset, *dest, size);
  }
}

GST_DEBUG_CATEGORY_STATIC (gst_parent_buffer_meta_debug);

/**
 * gst_buffer_add_parent_buffer_meta:
 * @buffer: (transfer none): a #GstBuffer
 * @ref: (transfer none): a #GstBuffer to ref
 *
 * Add a #GstParentBufferMeta to @buffer that holds a reference on
 * @ref until the buffer is freed.
 *
 * Returns: (transfer none) (nullable): The #GstParentBufferMeta that was added to the buffer
 *
 * Since: 1.6
 */
GstParentBufferMeta *
gst_buffer_add_parent_buffer_meta (GstBuffer * buffer, GstBuffer * ref)
{
  GstParentBufferMeta *meta;

  g_return_val_if_fail (GST_IS_BUFFER (ref), NULL);

  meta =
      (GstParentBufferMeta *) gst_buffer_add_meta (buffer,
      GST_PARENT_BUFFER_META_INFO, NULL);

  if (!meta)
    return NULL;

  meta->buffer = gst_buffer_ref (ref);

  return meta;
}

static gboolean
_gst_parent_buffer_meta_transform (GstBuffer * dest, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  GstParentBufferMeta *dmeta, *smeta;

  smeta = (GstParentBufferMeta *) meta;

  if (GST_META_TRANSFORM_IS_COPY (type)) {
    /* copy over the reference to the parent buffer.
     * Usually, this meta means we need to keep the parent buffer
     * alive because one of the child memories is in use, which
     * might not be the case if memory is deep copied or sub-regioned,
     * but we can't tell, so keep the meta */
    dmeta = gst_buffer_add_parent_buffer_meta (dest, smeta->buffer);
    if (!dmeta)
      return FALSE;

    GST_CAT_DEBUG (gst_parent_buffer_meta_debug,
        "copy buffer reference metadata");
  } else {
    /* return FALSE, if transform type is not supported */
    return FALSE;
  }
  return TRUE;
}

static void
_gst_parent_buffer_meta_free (GstParentBufferMeta * parent_meta,
    GstBuffer * buffer)
{
  GST_CAT_DEBUG (gst_parent_buffer_meta_debug,
      "Dropping reference on buffer %p", parent_meta->buffer);
  gst_buffer_unref (parent_meta->buffer);
}

static gboolean
_gst_parent_buffer_meta_init (GstParentBufferMeta * parent_meta,
    gpointer params, GstBuffer * buffer)
{
  static volatile gsize _init;

  if (g_once_init_enter (&_init)) {
    GST_DEBUG_CATEGORY_INIT (gst_parent_buffer_meta_debug, "parentbuffermeta",
        0, "parentbuffermeta");
    g_once_init_leave (&_init, 1);
  }

  parent_meta->buffer = NULL;

  return TRUE;
}

/**
 * gst_parent_buffer_meta_api_get_type: (attributes doc.skip=true)
 */
GType
gst_parent_buffer_meta_api_get_type (void)
{
  static volatile GType type = 0;
  static const gchar *tags[] = { NULL };

  if (g_once_init_enter (&type)) {
    GType _type = gst_meta_api_type_register ("GstParentBufferMetaAPI", tags);
    g_once_init_leave (&type, _type);
  }

  return type;
}

/**
 * gst_parent_buffer_meta_get_info:
 *
 * Get the global #GstMetaInfo describing  the #GstParentBufferMeta meta.
 *
 * Returns: (transfer none): The #GstMetaInfo
 *
 * Since: 1.6
 */
const GstMetaInfo *
gst_parent_buffer_meta_get_info (void)
{
  static const GstMetaInfo *meta_info = NULL;

  if (g_once_init_enter ((GstMetaInfo **) & meta_info)) {
    const GstMetaInfo *meta =
        gst_meta_register (gst_parent_buffer_meta_api_get_type (),
        "GstParentBufferMeta",
        sizeof (GstParentBufferMeta),
        (GstMetaInitFunction) _gst_parent_buffer_meta_init,
        (GstMetaFreeFunction) _gst_parent_buffer_meta_free,
        _gst_parent_buffer_meta_transform);
    g_once_init_leave ((GstMetaInfo **) & meta_info, (GstMetaInfo *) meta);
  }

  return meta_info;
}

GST_DEBUG_CATEGORY_STATIC (gst_reference_timestamp_meta_debug);

/**
 * gst_buffer_add_reference_timestamp_meta:
 * @buffer: (transfer none): a #GstBuffer
 * @reference: (transfer none): identifier for the timestamp reference.
 * @timestamp: timestamp
 * @duration: duration, or %GST_CLOCK_TIME_NONE
 *
 * Add a #GstReferenceTimestampMeta to @buffer that holds a @timestamp and
 * optionally @duration based on a specific timestamp @reference. See the
 * documentation of #GstReferenceTimestampMeta for details.
 *
 * Returns: (transfer none) (nullable): The #GstReferenceTimestampMeta that was added to the buffer
 *
 * Since: 1.14
 */
GstReferenceTimestampMeta *
gst_buffer_add_reference_timestamp_meta (GstBuffer * buffer,
    GstCaps * reference, GstClockTime timestamp, GstClockTime duration)
{
  GstReferenceTimestampMeta *meta;

  g_return_val_if_fail (GST_IS_CAPS (reference), NULL);
  g_return_val_if_fail (timestamp != GST_CLOCK_TIME_NONE, NULL);

  meta =
      (GstReferenceTimestampMeta *) gst_buffer_add_meta (buffer,
      GST_REFERENCE_TIMESTAMP_META_INFO, NULL);

  if (!meta)
    return NULL;

  meta->reference = gst_caps_ref (reference);
  meta->timestamp = timestamp;
  meta->duration = duration;

  return meta;
}

/**
 * gst_buffer_get_reference_timestamp_meta:
 * @buffer: a #GstBuffer
 * @reference: (allow-none): a reference #GstCaps
 *
 * Find the first #GstReferenceTimestampMeta on @buffer that conforms to
 * @reference. Conformance is tested by checking if the meta's reference is a
 * subset of @reference.
 *
 * Buffers can contain multiple #GstReferenceTimestampMeta metadata items.
 *
 * Returns: (transfer none) (nullable): the #GstReferenceTimestampMeta or %NULL when there
 * is no such metadata on @buffer.
 *
 * Since: 1.14
 */
GstReferenceTimestampMeta *
gst_buffer_get_reference_timestamp_meta (GstBuffer * buffer,
    GstCaps * reference)
{
  gpointer state = NULL;
  GstMeta *meta;
  const GstMetaInfo *info = GST_REFERENCE_TIMESTAMP_META_INFO;

  while ((meta = gst_buffer_iterate_meta (buffer, &state))) {
    if (meta->info->api == info->api) {
      GstReferenceTimestampMeta *rmeta = (GstReferenceTimestampMeta *) meta;

      if (!reference)
        return rmeta;
      if (gst_caps_is_subset (rmeta->reference, reference))
        return rmeta;
    }
  }
  return NULL;
}

static gboolean
_gst_reference_timestamp_meta_transform (GstBuffer * dest, GstMeta * meta,
    GstBuffer * buffer, GQuark type, gpointer data)
{
  GstReferenceTimestampMeta *dmeta, *smeta;

  /* we copy over the reference timestamp meta, independent of transformation
   * that happens. If it applied to the original buffer, it still applies to
   * the new buffer as it refers to the time when the media was captured */
  smeta = (GstReferenceTimestampMeta *) meta;
  dmeta =
      gst_buffer_add_reference_timestamp_meta (dest, smeta->reference,
      smeta->timestamp, smeta->duration);
  if (!dmeta)
    return FALSE;

  GST_CAT_DEBUG (gst_reference_timestamp_meta_debug,
      "copy reference timestamp metadata from buffer %p to %p", buffer, dest);

  return TRUE;
}

static void
_gst_reference_timestamp_meta_free (GstReferenceTimestampMeta * meta,
    GstBuffer * buffer)
{
  if (meta->reference)
    gst_caps_unref (meta->reference);
}

static gboolean
_gst_reference_timestamp_meta_init (GstReferenceTimestampMeta * meta,
    gpointer params, GstBuffer * buffer)
{
  static volatile gsize _init;

  if (g_once_init_enter (&_init)) {
    GST_DEBUG_CATEGORY_INIT (gst_reference_timestamp_meta_debug,
        "referencetimestampmeta", 0, "referencetimestampmeta");
    g_once_init_leave (&_init, 1);
  }

  meta->reference = NULL;
  meta->timestamp = GST_CLOCK_TIME_NONE;
  meta->duration = GST_CLOCK_TIME_NONE;

  return TRUE;
}

/**
 * gst_reference_timestamp_meta_api_get_type: (attributes doc.skip=true)
 */
GType
gst_reference_timestamp_meta_api_get_type (void)
{
  static volatile GType type = 0;
  static const gchar *tags[] = { NULL };

  if (g_once_init_enter (&type)) {
    GType _type =
        gst_meta_api_type_register ("GstReferenceTimestampMetaAPI", tags);
    g_once_init_leave (&type, _type);
  }

  return type;
}

/**
 * gst_reference_timestamp_meta_get_info:
 *
 * Get the global #GstMetaInfo describing  the #GstReferenceTimestampMeta meta.
 *
 * Returns: (transfer none): The #GstMetaInfo
 *
 * Since: 1.14
 */
const GstMetaInfo *
gst_reference_timestamp_meta_get_info (void)
{
  static const GstMetaInfo *meta_info = NULL;

  if (g_once_init_enter ((GstMetaInfo **) & meta_info)) {
    const GstMetaInfo *meta =
        gst_meta_register (gst_reference_timestamp_meta_api_get_type (),
        "GstReferenceTimestampMeta",
        sizeof (GstReferenceTimestampMeta),
        (GstMetaInitFunction) _gst_reference_timestamp_meta_init,
        (GstMetaFreeFunction) _gst_reference_timestamp_meta_free,
        _gst_reference_timestamp_meta_transform);
    g_once_init_leave ((GstMetaInfo **) & meta_info, (GstMetaInfo *) meta);
  }

  return meta_info;
}

/**
 * gst_buffer_ref: (skip)
 * @buf: a #GstBuffer.
 *
 * Increases the refcount of the given buffer by one.
 *
 * Note that the refcount affects the writability
 * of @buf and its metadata, see gst_buffer_is_writable().
 * It is important to note that keeping additional references to
 * GstBuffer instances can potentially increase the number
 * of memcpy operations in a pipeline.
 *
 * Returns: (transfer full): @buf
 */
GstBuffer *
gst_buffer_ref (GstBuffer * buf)
{
  return (GstBuffer *) gst_mini_object_ref (GST_MINI_OBJECT_CAST (buf));
}

/**
 * gst_buffer_unref: (skip)
 * @buf: (transfer full): a #GstBuffer.
 *
 * Decreases the refcount of the buffer. If the refcount reaches 0, the buffer
 * with the associated metadata and memory will be freed.
 */
void
gst_buffer_unref (GstBuffer * buf)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (buf));
}

/**
 * gst_clear_buffer: (skip)
 * @buf_ptr: a pointer to a #GstBuffer reference
 *
 * Clears a reference to a #GstBuffer.
 *
 * @buf_ptr must not be %NULL.
 *
 * If the reference is %NULL then this function does nothing. Otherwise, the
 * reference count of the buffer is decreased and the pointer is set to %NULL.
 *
 * Since: 1.16
 */
void
gst_clear_buffer (GstBuffer ** buf_ptr)
{
  gst_clear_mini_object ((GstMiniObject **) buf_ptr);
}

/**
 * gst_buffer_copy: (skip)
 * @buf: a #GstBuffer.
 *
 * Create a copy of the given buffer. This will only copy the buffer's
 * data to a newly allocated memory if needed (if the type of memory
 * requires it), otherwise the underlying data is just referenced.
 * Check gst_buffer_copy_deep() if you want to force the data
 * to be copied to newly allocated memory.
 *
 * Returns: (transfer full): a new copy of @buf.
 */
GstBuffer *
gst_buffer_copy (const GstBuffer * buf)
{
  return GST_BUFFER (gst_mini_object_copy (GST_MINI_OBJECT_CONST_CAST (buf)));
}

/**
 * gst_buffer_replace: (skip)
 * @obuf: (inout) (transfer full) (nullable): pointer to a pointer to
 *     a #GstBuffer to be replaced.
 * @nbuf: (transfer none) (allow-none): pointer to a #GstBuffer that will
 *     replace the buffer pointed to by @obuf.
 *
 * Modifies a pointer to a #GstBuffer to point to a different #GstBuffer. The
 * modification is done atomically (so this is useful for ensuring thread safety
 * in some cases), and the reference counts are updated appropriately (the old
 * buffer is unreffed, the new is reffed).
 *
 * Either @nbuf or the #GstBuffer pointed to by @obuf may be %NULL.
 *
 * Returns: %TRUE when @obuf was different from @nbuf.
 */
gboolean
gst_buffer_replace (GstBuffer ** obuf, GstBuffer * nbuf)
{
  return gst_mini_object_replace ((GstMiniObject **) obuf,
      (GstMiniObject *) nbuf);
}
