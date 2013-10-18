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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gstbuffer
 * @short_description: Data-passing buffer type, supporting sub-buffers.
 * @see_also: #GstPad, #GstMiniObject
 *
 * Buffers are the basic unit of data transfer in GStreamer.  The #GstBuffer
 * type provides all the state necessary to define a region of memory as part
 * of a stream.  Sub-buffers are also supported, allowing a smaller region of a
 * buffer to become its own buffer, with mechanisms in place to ensure that
 * neither memory space goes away prematurely.
 *
 * Buffers are usually created with gst_buffer_new(). After a buffer has been
 * created one will typically allocate memory for it and set the size of the
 * buffer data.  The following example creates a buffer that can hold a given
 * video frame with a given width, height and bits per plane.
 * <example>
 * <title>Creating a buffer for a video frame</title>
 *   <programlisting>
 *   GstBuffer *buffer;
 *   gint size, width, height, bpp;
 *   ...
 *   size = width * height * bpp;
 *   buffer = gst_buffer_new ();
 *   GST_BUFFER_SIZE (buffer) = size;
 *   GST_BUFFER_MALLOCDATA (buffer) = g_malloc (size);
 *   GST_BUFFER_DATA (buffer) = GST_BUFFER_MALLOCDATA (buffer);
 *   ...
 *   </programlisting>
 * </example>
 *
 * Alternatively, use gst_buffer_new_and_alloc()
 * to create a buffer with preallocated data of a given size.
 *
 * The data pointed to by the buffer can be retrieved with the GST_BUFFER_DATA()
 * macro. The size of the data can be found with GST_BUFFER_SIZE(). For buffers
 * of size 0, the data pointer is undefined (usually NULL) and should never be used.
 *
 * If an element knows what pad you will push the buffer out on, it should use
 * gst_pad_alloc_buffer() instead to create a buffer.  This allows downstream
 * elements to provide special buffers to write in, like hardware buffers.
 *
 * A buffer has a pointer to a #GstCaps describing the media type of the data
 * in the buffer. Attach caps to the buffer with gst_buffer_set_caps(); this
 * is typically done before pushing out a buffer using gst_pad_push() so that
 * the downstream element knows the type of the buffer.
 *
 * A buffer will usually have a timestamp, and a duration, but neither of these
 * are guaranteed (they may be set to #GST_CLOCK_TIME_NONE). Whenever a
 * meaningful value can be given for these, they should be set. The timestamp
 * and duration are measured in nanoseconds (they are #GstClockTime values).
 *
 * A buffer can also have one or both of a start and an end offset. These are
 * media-type specific. For video buffers, the start offset will generally be
 * the frame number. For audio buffers, it will be the number of samples
 * produced so far. For compressed data, it could be the byte offset in a
 * source or destination file. Likewise, the end offset will be the offset of
 * the end of the buffer. These can only be meaningfully interpreted if you
 * know the media type of the buffer (the #GstCaps set on it). Either or both
 * can be set to #GST_BUFFER_OFFSET_NONE.
 *
 * gst_buffer_ref() is used to increase the refcount of a buffer. This must be
 * done when you want to keep a handle to the buffer after pushing it to the
 * next element.
 *
 * To efficiently create a smaller buffer out of an existing one, you can
 * use gst_buffer_create_sub().
 *
 * If a plug-in wants to modify the buffer data in-place, it should first obtain
 * a buffer that is safe to modify by using gst_buffer_make_writable().  This
 * function is optimized so that a copy will only be made when it is necessary.
 *
 * A plugin that only wishes to modify the metadata of a buffer, such as the
 * offset, timestamp or caps, should use gst_buffer_make_metadata_writable(),
 * which will create a subbuffer of the original buffer to ensure the caller
 * has sole ownership, and not copy the buffer data.
 *
 * Several flags of the buffer can be set and unset with the
 * GST_BUFFER_FLAG_SET() and GST_BUFFER_FLAG_UNSET() macros. Use
 * GST_BUFFER_FLAG_IS_SET() to test if a certain #GstBufferFlag is set.
 *
 * Buffers can be efficiently merged into a larger buffer with
 * gst_buffer_merge() and gst_buffer_span() if the gst_buffer_is_span_fast()
 * function returns TRUE.
 *
 * An element should either unref the buffer or push it out on a src pad
 * using gst_pad_push() (see #GstPad).
 *
 * Buffers are usually freed by unreffing them with gst_buffer_unref(). When
 * the refcount drops to 0, any data pointed to by GST_BUFFER_MALLOCDATA() will
 * also be freed.
 *
 * Last reviewed on August 11th, 2006 (0.10.10)
 */
#include "gst_private.h"

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif
#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif

#include "gstbuffer.h"
#include "gstinfo.h"
#include "gstutils.h"
#include "gstminiobject.h"
#include "gstversion.h"

static void gst_buffer_finalize (GstBuffer * buffer);
static GstBuffer *_gst_buffer_copy (GstBuffer * buffer);

static GType _gst_buffer_type = 0;

/* buffer alignment in bytes
 * an alignment of 8 would be the same as malloc() guarantees
 */
#ifdef HAVE_POSIX_MEMALIGN
#if defined(BUFFER_ALIGNMENT_MALLOC)
static size_t _gst_buffer_data_alignment = 8;
#elif defined(BUFFER_ALIGNMENT_PAGESIZE)
static size_t _gst_buffer_data_alignment = 0;
#elif defined(BUFFER_ALIGNMENT)
static size_t _gst_buffer_data_alignment = BUFFER_ALIGNMENT;
#else
#error "No buffer alignment configured"
#endif

static inline gboolean
aligned_malloc (gpointer * memptr, guint size)
{
  gint res;

  res = posix_memalign (memptr, _gst_buffer_data_alignment, size);
  return (res == 0);
}

#endif /* HAVE_POSIX_MEMALIGN */

void
_gst_buffer_initialize (void)
{
  /* the GstMiniObject types need to be class_ref'd once before it can be
   * done from multiple threads;
   * see http://bugzilla.gnome.org/show_bug.cgi?id=304551 */
  g_type_class_ref (gst_buffer_get_type ());
#ifdef HAVE_GETPAGESIZE
#ifdef BUFFER_ALIGNMENT_PAGESIZE
  _gst_buffer_data_alignment = getpagesize ();
#endif
#endif
}

#define _do_init \
{ \
  _gst_buffer_type = g_define_type_id; \
}

G_DEFINE_TYPE_WITH_CODE (GstBuffer, gst_buffer, GST_TYPE_MINI_OBJECT, _do_init);

static void
gst_buffer_class_init (GstBufferClass * klass)
{
  klass->mini_object_class.copy = (GstMiniObjectCopyFunction) _gst_buffer_copy;
  klass->mini_object_class.finalize =
      (GstMiniObjectFinalizeFunction) gst_buffer_finalize;
}

static void
gst_buffer_finalize (GstBuffer * buffer)
{
  g_return_if_fail (buffer != NULL);

  GST_CAT_LOG (GST_CAT_BUFFER, "finalize %p", buffer);

  /* free our data */
  if (G_LIKELY (buffer->malloc_data))
    buffer->free_func (buffer->malloc_data);

  gst_caps_replace (&GST_BUFFER_CAPS (buffer), NULL);

  if (buffer->parent)
    gst_buffer_unref (buffer->parent);

/*   ((GstMiniObjectClass *) */
/*       gst_buffer_parent_class)->finalize (GST_MINI_OBJECT_CAST (buffer)); */
}

/**
 * gst_buffer_copy_metadata:
 * @dest: a destination #GstBuffer
 * @src: a source #GstBuffer
 * @flags: flags indicating what metadata fields should be copied.
 *
 * Copies the metadata from @src into @dest. The data, size and mallocdata
 * fields are not copied.
 *
 * @flags indicate which fields will be copied. Use #GST_BUFFER_COPY_ALL to copy
 * all the metadata fields.
 *
 * This function is typically called from a custom buffer copy function after
 * creating @dest and setting the data, size, mallocdata.
 *
 * Since: 0.10.13
 */
void
gst_buffer_copy_metadata (GstBuffer * dest, const GstBuffer * src,
    GstBufferCopyFlags flags)
{
  g_return_if_fail (dest != NULL);
  g_return_if_fail (src != NULL);

  /* nothing to copy if the buffers are the same */
  if (G_UNLIKELY (dest == src))
    return;

#if GST_VERSION_NANO == 1
  /* we enable this extra debugging in git versions only for now */
  g_warn_if_fail (gst_buffer_is_metadata_writable (dest));
#endif

  GST_CAT_LOG (GST_CAT_BUFFER, "copy %p to %p", src, dest);

  if (flags & GST_BUFFER_COPY_FLAGS) {
    guint mask;

    /* copy relevant flags */
    mask = GST_BUFFER_FLAG_PREROLL | GST_BUFFER_FLAG_IN_CAPS |
        GST_BUFFER_FLAG_DELTA_UNIT | GST_BUFFER_FLAG_DISCONT |
        GST_BUFFER_FLAG_GAP | GST_BUFFER_FLAG_MEDIA1 |
        GST_BUFFER_FLAG_MEDIA2 | GST_BUFFER_FLAG_MEDIA3;
    GST_MINI_OBJECT_FLAGS (dest) |= GST_MINI_OBJECT_FLAGS (src) & mask;
  }

  if (flags & GST_BUFFER_COPY_TIMESTAMPS) {
    GST_BUFFER_TIMESTAMP (dest) = GST_BUFFER_TIMESTAMP (src);
    GST_BUFFER_DURATION (dest) = GST_BUFFER_DURATION (src);
    GST_BUFFER_OFFSET (dest) = GST_BUFFER_OFFSET (src);
    GST_BUFFER_OFFSET_END (dest) = GST_BUFFER_OFFSET_END (src);
  }

  if (flags & GST_BUFFER_COPY_CAPS) {
    gst_caps_replace (&GST_BUFFER_CAPS (dest), GST_BUFFER_CAPS (src));
  }
}

static GstBuffer *
_gst_buffer_copy (GstBuffer * buffer)
{
  GstBuffer *copy;

  g_return_val_if_fail (buffer != NULL, NULL);

  /* create a fresh new buffer */
  copy = gst_buffer_new ();

  /* we simply copy everything from our parent */
#ifdef HAVE_POSIX_MEMALIGN
  {
    gpointer memptr = NULL;

    if (G_LIKELY (buffer->size)) {
      if (G_UNLIKELY (!aligned_malloc (&memptr, buffer->size))) {
        /* terminate on error like g_memdup() would */
        g_error ("%s: failed to allocate %u bytes", G_STRLOC, buffer->size);
      } else {
        memcpy (memptr, buffer->data, buffer->size);
      }
    }
    copy->data = (guint8 *) memptr;
    GST_BUFFER_FREE_FUNC (copy) = free;
  }
#else
  copy->data = g_memdup (buffer->data, buffer->size);
#endif

  /* make sure it gets freed (even if the parent is subclassed, we return a
     normal buffer) */
  copy->malloc_data = copy->data;
  copy->size = buffer->size;

  gst_buffer_copy_metadata (copy, buffer, GST_BUFFER_COPY_ALL);

  return copy;
}

static void
gst_buffer_init (GstBuffer * buffer)
{
  GST_CAT_LOG (GST_CAT_BUFFER, "init %p", buffer);

  GST_BUFFER_TIMESTAMP (buffer) = GST_CLOCK_TIME_NONE;
  GST_BUFFER_DURATION (buffer) = GST_CLOCK_TIME_NONE;
  GST_BUFFER_OFFSET (buffer) = GST_BUFFER_OFFSET_NONE;
  GST_BUFFER_OFFSET_END (buffer) = GST_BUFFER_OFFSET_NONE;
  GST_BUFFER_FREE_FUNC (buffer) = g_free;
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
  GstBuffer *newbuf;

  newbuf = (GstBuffer *) gst_mini_object_new (_gst_buffer_type);

  GST_CAT_LOG (GST_CAT_BUFFER, "new %p", newbuf);

  return newbuf;
}

/**
 * gst_buffer_new_and_alloc:
 * @size: the size in bytes of the new buffer's data.
 *
 * Creates a newly allocated buffer with data of the given size.
 * The buffer memory is not cleared. If the requested amount of
 * memory can't be allocated, the program will abort. Use
 * gst_buffer_try_new_and_alloc() if you want to handle this case
 * gracefully or have gotten the size to allocate from an untrusted
 * source such as a media stream.
 * 
 *
 * Note that when @size == 0, the buffer data pointer will be NULL.
 *
 * MT safe.
 *
 * Returns: (transfer full): the new #GstBuffer.
 */
GstBuffer *
gst_buffer_new_and_alloc (guint size)
{
  GstBuffer *newbuf;

  newbuf = gst_buffer_new ();

#ifdef HAVE_POSIX_MEMALIGN
  {
    gpointer memptr = NULL;

    if (G_LIKELY (size)) {
      if (G_UNLIKELY (!aligned_malloc (&memptr, size))) {
        /* terminate on error like g_memdup() would */
        g_error ("%s: failed to allocate %u bytes", G_STRLOC, size);
      }
    }
    newbuf->malloc_data = (guint8 *) memptr;
    GST_BUFFER_FREE_FUNC (newbuf) = free;
  }
#else
  newbuf->malloc_data = g_malloc (size);
#endif
  GST_BUFFER_DATA (newbuf) = newbuf->malloc_data;
  GST_BUFFER_SIZE (newbuf) = size;

  GST_CAT_LOG (GST_CAT_BUFFER, "new %p of size %d", newbuf, size);

  return newbuf;
}

/**
 * gst_buffer_try_new_and_alloc:
 * @size: the size in bytes of the new buffer's data.
 *
 * Tries to create a newly allocated buffer with data of the given size. If
 * the requested amount of memory can't be allocated, NULL will be returned.
 * The buffer memory is not cleared.
 *
 * Note that when @size == 0, the buffer data pointer will be NULL.
 *
 * MT safe.
 *
 * Returns: (transfer full): a new #GstBuffer, or NULL if the memory couldn't
 *     be allocated.
 *
 * Since: 0.10.13
 */
GstBuffer *
gst_buffer_try_new_and_alloc (guint size)
{
  GstBuffer *newbuf;
  guint8 *malloc_data;
#ifdef HAVE_POSIX_MEMALIGN
  gpointer memptr = NULL;

  if (G_LIKELY (size)) {
    if (G_UNLIKELY (!aligned_malloc (&memptr, size))) {
      GST_CAT_WARNING (GST_CAT_BUFFER, "failed to allocate %d bytes", size);
      return NULL;
    }
  }
  malloc_data = (guint8 *) memptr;
#else
  malloc_data = g_try_malloc (size);

  if (G_UNLIKELY (malloc_data == NULL && size != 0)) {
    GST_CAT_WARNING (GST_CAT_BUFFER, "failed to allocate %d bytes", size);
    return NULL;
  }
#endif

  /* FIXME: there's no g_type_try_create_instance() in GObject yet, so this
   * will still abort if a new GstBuffer structure can't be allocated */
  newbuf = gst_buffer_new ();

  GST_BUFFER_MALLOCDATA (newbuf) = malloc_data;
  GST_BUFFER_DATA (newbuf) = malloc_data;
  GST_BUFFER_SIZE (newbuf) = size;
#ifdef HAVE_POSIX_MEMALIGN
  GST_BUFFER_FREE_FUNC (newbuf) = free;
#endif

  GST_CAT_LOG (GST_CAT_BUFFER, "new %p of size %d", newbuf, size);

  return newbuf;
}

/**
 * gst_buffer_get_caps:
 * @buffer: a #GstBuffer.
 *
 * Gets the media type of the buffer. This can be NULL if there
 * is no media type attached to this buffer.
 *
 * Returns: (transfer full): a reference to the #GstCaps. unref after usage.
 * Returns NULL if there were no caps on this buffer.
 */
/* this is not made atomic because if the buffer were reffed from multiple
 * threads, it would have a refcount > 2 and thus be immutable.
 */
GstCaps *
gst_buffer_get_caps (GstBuffer * buffer)
{
  GstCaps *ret;

  g_return_val_if_fail (buffer != NULL, NULL);

  ret = GST_BUFFER_CAPS (buffer);

  if (ret)
    gst_caps_ref (ret);

  return ret;
}

/**
 * gst_buffer_set_caps:
 * @buffer: a #GstBuffer.
 * @caps: (transfer none): a #GstCaps.
 *
 * Sets the media type on the buffer. The refcount of the caps will
 * be increased and any previous caps on the buffer will be
 * unreffed.
 */
/* this is not made atomic because if the buffer were reffed from multiple
 * threads, it would have a refcount > 2 and thus be immutable.
 */
void
gst_buffer_set_caps (GstBuffer * buffer, GstCaps * caps)
{
  g_return_if_fail (buffer != NULL);
  g_return_if_fail (caps == NULL || GST_CAPS_IS_SIMPLE (caps));

#if GST_VERSION_NANO == 1
  /* we enable this extra debugging in git versions only for now */
  g_warn_if_fail (gst_buffer_is_metadata_writable (buffer));
  /* FIXME: would be nice to also check if caps are fixed here, but expensive */
#endif

  gst_caps_replace (&GST_BUFFER_CAPS (buffer), caps);
}

/**
 * gst_buffer_is_metadata_writable:
 * @buf: a #GstBuffer
 *
 * Similar to gst_buffer_is_writable, but this only ensures that the
 * refcount of the buffer is 1, indicating that the caller is the sole
 * owner and can change the buffer metadata, such as caps and timestamps.
 *
 * Returns: TRUE if the metadata is writable.
 */
gboolean
gst_buffer_is_metadata_writable (GstBuffer * buf)
{
  return (GST_MINI_OBJECT_REFCOUNT_VALUE (GST_MINI_OBJECT_CAST (buf)) == 1);
}

/**
 * gst_buffer_make_metadata_writable:
 * @buf: (transfer full): a #GstBuffer
 *
 * Similar to gst_buffer_make_writable, but does not ensure that the buffer
 * data array is writable. Instead, this just ensures that the returned buffer
 * is solely owned by the caller, by creating a subbuffer of the original
 * buffer if necessary.
 * 
 * After calling this function, @buf should not be referenced anymore. The
 * result of this function has guaranteed writable metadata.
 *
 * Returns: (transfer full): a new #GstBuffer with writable metadata, which
 *     may or may not be the same as @buf.
 */
GstBuffer *
gst_buffer_make_metadata_writable (GstBuffer * buf)
{
  GstBuffer *ret;

  if (gst_buffer_is_metadata_writable (buf)) {
    ret = buf;
  } else {
    ret = gst_buffer_create_sub (buf, 0, GST_BUFFER_SIZE (buf));

    gst_buffer_unref (buf);
  }

  return ret;
}

#define GST_IS_SUBBUFFER(obj)   (GST_BUFFER_CAST(obj)->parent != NULL)

/**
 * gst_buffer_create_sub:
 * @parent: a #GstBuffer.
 * @offset: the offset into parent #GstBuffer at which the new sub-buffer 
 *          begins.
 * @size: the size of the new #GstBuffer sub-buffer, in bytes.
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
 * Returns: (transfer full): the new #GstBuffer or NULL if the arguments were
 *     invalid.
 */
GstBuffer *
gst_buffer_create_sub (GstBuffer * buffer, guint offset, guint size)
{
  GstBuffer *subbuffer;
  GstBuffer *parent;
  gboolean complete;

  g_return_val_if_fail (buffer != NULL, NULL);
  g_return_val_if_fail (buffer->mini_object.refcount > 0, NULL);
  g_return_val_if_fail (buffer->size >= offset + size, NULL);

  /* find real parent */
  if (GST_IS_SUBBUFFER (buffer)) {
    parent = buffer->parent;
  } else {
    parent = buffer;
  }
  gst_buffer_ref (parent);

  /* create the new buffer */
  subbuffer = gst_buffer_new ();
  subbuffer->parent = parent;
  GST_BUFFER_FLAG_SET (subbuffer, GST_BUFFER_FLAG_READONLY);

  GST_CAT_LOG (GST_CAT_BUFFER, "new subbuffer %p (parent %p)", subbuffer,
      parent);

  /* set the right values in the child */
  GST_BUFFER_DATA (subbuffer) = buffer->data + offset;
  GST_BUFFER_SIZE (subbuffer) = size;

  if ((offset == 0) && (size == GST_BUFFER_SIZE (buffer))) {
    /* copy all the flags except IN_CAPS */
    GST_BUFFER_FLAG_SET (subbuffer, GST_BUFFER_FLAGS (buffer));
    GST_BUFFER_FLAG_UNSET (subbuffer, GST_BUFFER_FLAG_IN_CAPS);
  } else {
    /* copy only PREROLL & GAP flags */
    GST_BUFFER_FLAG_SET (subbuffer, (GST_BUFFER_FLAGS (buffer) &
            (GST_BUFFER_FLAG_PREROLL | GST_BUFFER_FLAG_GAP)));
  }

  /* we can copy the timestamp and offset if the new buffer starts at
   * offset 0 */
  if (offset == 0) {
    GST_BUFFER_TIMESTAMP (subbuffer) = GST_BUFFER_TIMESTAMP (buffer);
    GST_BUFFER_OFFSET (subbuffer) = GST_BUFFER_OFFSET (buffer);
    complete = (buffer->size == size);
  } else {
    GST_BUFFER_TIMESTAMP (subbuffer) = GST_CLOCK_TIME_NONE;
    GST_BUFFER_OFFSET (subbuffer) = GST_BUFFER_OFFSET_NONE;
    complete = FALSE;
  }

  if (complete) {
    GstCaps *caps;

    /* if we copied the complete buffer we can copy the duration,
     * offset_end and caps as well */
    GST_BUFFER_DURATION (subbuffer) = GST_BUFFER_DURATION (buffer);
    GST_BUFFER_OFFSET_END (subbuffer) = GST_BUFFER_OFFSET_END (buffer);
    if ((caps = GST_BUFFER_CAPS (buffer)))
      gst_caps_ref (caps);
    GST_BUFFER_CAPS (subbuffer) = caps;
  } else {
    GST_BUFFER_DURATION (subbuffer) = GST_CLOCK_TIME_NONE;
    GST_BUFFER_OFFSET_END (subbuffer) = GST_BUFFER_OFFSET_NONE;
    GST_BUFFER_CAPS (subbuffer) = NULL;
  }
  return subbuffer;
}

/**
 * gst_buffer_is_span_fast:
 * @buf1: the first #GstBuffer.
 * @buf2: the second #GstBuffer.
 *
 * Determines whether a gst_buffer_span() can be done without copying
 * the contents, that is, whether the data areas are contiguous sub-buffers of 
 * the same buffer.
 *
 * MT safe.
 * Returns: TRUE if the buffers are contiguous,
 * FALSE if a copy would be required.
 */
gboolean
gst_buffer_is_span_fast (GstBuffer * buf1, GstBuffer * buf2)
{
  g_return_val_if_fail (buf1 != NULL && buf2 != NULL, FALSE);
  g_return_val_if_fail (buf1->mini_object.refcount > 0, FALSE);
  g_return_val_if_fail (buf2->mini_object.refcount > 0, FALSE);

  /* it's only fast if we have subbuffers of the same parent */
  return (GST_IS_SUBBUFFER (buf1) &&
      GST_IS_SUBBUFFER (buf2) && (buf1->parent == buf2->parent)
      && ((buf1->data + buf1->size) == buf2->data));
}

/**
 * gst_buffer_span:
 * @buf1: the first source #GstBuffer to merge.
 * @offset: the offset in the first buffer from where the new
 * buffer should start.
 * @buf2: the second source #GstBuffer to merge.
 * @len: the total length of the new buffer.
 *
 * Creates a new buffer that consists of part of buf1 and buf2.
 * Logically, buf1 and buf2 are concatenated into a single larger
 * buffer, and a new buffer is created at the given offset inside
 * this space, with a given length.
 *
 * If the two source buffers are children of the same larger buffer,
 * and are contiguous, the new buffer will be a child of the shared
 * parent, and thus no copying is necessary. you can use
 * gst_buffer_is_span_fast() to determine if a memcpy will be needed.
 *
 * MT safe.
 *
 * Returns: (transfer full): the new #GstBuffer that spans the two source
 *     buffers, or NULL if the arguments are invalid.
 */
GstBuffer *
gst_buffer_span (GstBuffer * buf1, guint32 offset, GstBuffer * buf2,
    guint32 len)
{
  GstBuffer *newbuf;

  g_return_val_if_fail (buf1 != NULL && buf2 != NULL, NULL);
  g_return_val_if_fail (buf1->mini_object.refcount > 0, NULL);
  g_return_val_if_fail (buf2->mini_object.refcount > 0, NULL);
  g_return_val_if_fail (len > 0, NULL);
  g_return_val_if_fail (len <= buf1->size + buf2->size - offset, NULL);

  /* if the two buffers have the same parent and are adjacent */
  if (gst_buffer_is_span_fast (buf1, buf2)) {
    GstBuffer *parent = buf1->parent;

    /* we simply create a subbuffer of the common parent */
    newbuf = gst_buffer_create_sub (parent,
        buf1->data - parent->data + offset, len);
  } else {
    GST_CAT_DEBUG (GST_CAT_BUFFER,
        "slow path taken while spanning buffers %p and %p", buf1, buf2);
    /* otherwise we simply have to brute-force copy the buffers */
    newbuf = gst_buffer_new_and_alloc (len);

    /* copy the first buffer's data across */
    memcpy (newbuf->data, buf1->data + offset, buf1->size - offset);
    /* copy the second buffer's data across */
    memcpy (newbuf->data + (buf1->size - offset), buf2->data,
        len - (buf1->size - offset));
  }
  /* if the offset is 0, the new buffer has the same timestamp as buf1 */
  if (offset == 0) {
    GST_BUFFER_OFFSET (newbuf) = GST_BUFFER_OFFSET (buf1);
    GST_BUFFER_TIMESTAMP (newbuf) = GST_BUFFER_TIMESTAMP (buf1);

    /* if we completely merged the two buffers (appended), we can
     * calculate the duration too. Also make sure we's not messing with
     * invalid DURATIONS */
    if (buf1->size + buf2->size == len) {
      if (GST_BUFFER_DURATION_IS_VALID (buf1) &&
          GST_BUFFER_DURATION_IS_VALID (buf2)) {
        /* add duration */
        GST_BUFFER_DURATION (newbuf) = GST_BUFFER_DURATION (buf1) +
            GST_BUFFER_DURATION (buf2);
      }
      if (GST_BUFFER_OFFSET_END_IS_VALID (buf2)) {
        /* add offset_end */
        GST_BUFFER_OFFSET_END (newbuf) = GST_BUFFER_OFFSET_END (buf2);
      }
    }
  }

  return newbuf;
}
