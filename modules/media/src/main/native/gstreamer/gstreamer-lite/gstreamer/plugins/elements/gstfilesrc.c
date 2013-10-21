/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *               2000,2005 Wim Taymans <wim@fluendo.com>
 *
 * gstfilesrc.c:
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
 * SECTION:element-filesrc
 * @see_also: #GstFileSrc
 *
 * Read data from a file in the local file system.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch filesrc location=song.ogg ! decodebin2 ! autoaudiosink
 * ]| Play a song.ogg from local dir.
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <gst/gst.h>
#include "gstfilesrc.h"

#include <stdio.h>
#include <sys/types.h>
#ifdef G_OS_WIN32
#include <io.h>                 /* lseek, open, close, read */
/* On win32, stat* default to 32 bit; we need the 64-bit
 * variants, so explicitly define it that way. */
#define stat __stat64
#define fstat _fstat64
#undef lseek
#define lseek _lseeki64
#undef off_t
#define off_t guint64
/* Prevent stat.h from defining the stat* functions as
 * _stat*, since we're explicitly overriding that */
#undef _INC_STAT_INL
#endif
#include <sys/stat.h>
#include <fcntl.h>

#ifdef HAVE_UNISTD_H
#  include <unistd.h>
#endif

#ifdef HAVE_MMAP
# include <sys/mman.h>
#endif

#include <errno.h>
#include <string.h>

#include "../../gst/gst-i18n-lib.h"

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

/* FIXME we should be using glib for this */
#ifndef S_ISREG
#define S_ISREG(mode) ((mode)&_S_IFREG)
#endif
#ifndef S_ISDIR
#define S_ISDIR(mode) ((mode)&_S_IFDIR)
#endif
#ifndef S_ISSOCK
#define S_ISSOCK(x) (0)
#endif
#ifndef O_BINARY
#define O_BINARY (0)
#endif

/* Copy of glib's g_open due to win32 libc/cross-DLL brokenness: we can't
 * use the 'file descriptor' opened in glib (and returned from this function)
 * in this library, as they may have unrelated C runtimes. */
static int
gst_open (const gchar * filename, int flags, int mode)
{
#ifdef G_OS_WIN32
  wchar_t *wfilename = g_utf8_to_utf16 (filename, -1, NULL, NULL, NULL);
  int retval;
  int save_errno;

  if (wfilename == NULL) {
    errno = EINVAL;
    return -1;
  }

  retval = _wopen (wfilename, flags, mode);
  save_errno = errno;

  g_free (wfilename);

  errno = save_errno;
  return retval;
#else
  return open (filename, flags, mode);
#endif
}


/**********************************************************************
 * GStreamer Default File Source
 * Theory of Operation
 *
 * Update: see GstFileSrc:use-mmap property documentation below
 *         for why use of mmap() is disabled by default.
 *
 * This source uses mmap(2) to efficiently load data from a file.
 * To do this without seriously polluting the applications' memory
 * space, it must do so in smaller chunks, say 1-4MB at a time.
 * Buffers are then subdivided from these mmap'd chunks, to directly
 * make use of the mmap.
 *
 * To handle refcounting so that the mmap can be freed at the appropriate
 * time, a buffer will be created for each mmap'd region, and all new
 * buffers will be sub-buffers of this top-level buffer.  As they are
 * freed, the refcount goes down on the mmap'd buffer and its free()
 * function is called, which will call munmap(2) on itself.
 *
 * If a buffer happens to cross the boundaries of an mmap'd region, we
 * have to decide whether it's more efficient to copy the data into a
 * new buffer, or mmap() just that buffer.  There will have to be a
 * breakpoint size to determine which will be done.  The mmap() size
 * has a lot to do with this as well, because you end up in double-
 * jeopardy: the larger the outgoing buffer, the more data to copy when
 * it overlaps, *and* the more frequently you'll have buffers that *do*
 * overlap.
 *
 * Seeking is another tricky aspect to do efficiently.  The initial
 * implementation of this source won't make use of these features, however.
 * The issue is that if an application seeks backwards in a file, *and*
 * that region of the file is covered by an mmap that hasn't been fully
 * deallocated, we really should re-use it.  But keeping track of these
 * regions is tricky because we have to lock the structure that holds
 * them.  We need to settle on a locking primitive (GMutex seems to be
 * a really good option...), then we can do that.
 */


GST_DEBUG_CATEGORY_STATIC (gst_file_src_debug);
#define GST_CAT_DEFAULT gst_file_src_debug

/* FileSrc signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

#define DEFAULT_BLOCKSIZE       4*1024
#define DEFAULT_MMAPSIZE        4*1024*1024
#define DEFAULT_TOUCH           TRUE
#define DEFAULT_USEMMAP         FALSE
#define DEFAULT_SEQUENTIAL      FALSE

enum
{
  ARG_0,
  ARG_LOCATION,
  ARG_FD,
  ARG_MMAPSIZE,
  ARG_SEQUENTIAL,
  ARG_TOUCH,
  ARG_USEMMAP
};

static void gst_file_src_finalize (GObject * object);

static void gst_file_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_file_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

static gboolean gst_file_src_start (GstBaseSrc * basesrc);
static gboolean gst_file_src_stop (GstBaseSrc * basesrc);

static gboolean gst_file_src_is_seekable (GstBaseSrc * src);
static gboolean gst_file_src_get_size (GstBaseSrc * src, guint64 * size);
static GstFlowReturn gst_file_src_create (GstBaseSrc * src, guint64 offset,
    guint length, GstBuffer ** buffer);
static gboolean gst_file_src_query (GstBaseSrc * src, GstQuery * query);

static void gst_file_src_uri_handler_init (gpointer g_iface,
    gpointer iface_data);

static void
_do_init (GType filesrc_type)
{
  static const GInterfaceInfo urihandler_info = {
    gst_file_src_uri_handler_init,
    NULL,
    NULL
  };

  g_type_add_interface_static (filesrc_type, GST_TYPE_URI_HANDLER,
      &urihandler_info);
  GST_DEBUG_CATEGORY_INIT (gst_file_src_debug, "filesrc", 0, "filesrc element");
}

GST_BOILERPLATE_FULL (GstFileSrc, gst_file_src, GstBaseSrc, GST_TYPE_BASE_SRC,
    _do_init);

static void
gst_file_src_base_init (gpointer g_class)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (g_class);

  gst_element_class_set_details_simple (gstelement_class,
      "File Source",
      "Source/File",
      "Read from arbitrary point in a file",
      "Erik Walthinsen <omega@cse.ogi.edu>");
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));
}

static void
gst_file_src_class_init (GstFileSrcClass * klass)
{
  GObjectClass *gobject_class;
  GstBaseSrcClass *gstbasesrc_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstbasesrc_class = GST_BASE_SRC_CLASS (klass);

  gobject_class->set_property = gst_file_src_set_property;
  gobject_class->get_property = gst_file_src_get_property;

  g_object_class_install_property (gobject_class, ARG_FD,
      g_param_spec_int ("fd", "File-descriptor",
          "File-descriptor for the file being mmap()d", 0, G_MAXINT, 0,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, ARG_LOCATION,
      g_param_spec_string ("location", "File Location",
          "Location of the file to read", NULL,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_READY));
  g_object_class_install_property (gobject_class, ARG_MMAPSIZE,
      g_param_spec_ulong ("mmapsize", "mmap() Block Size",
          "Size in bytes of mmap()d regions", 0, G_MAXULONG, DEFAULT_MMAPSIZE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_PLAYING));
  g_object_class_install_property (gobject_class, ARG_TOUCH,
      g_param_spec_boolean ("touch", "Touch mapped region read data",
          "Touch mmapped data regions to force them to be read from disk",
          DEFAULT_TOUCH,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_PLAYING));
  /**
   * GstFileSrc:use-mmap
   *
   * Whether to use mmap(). Set to TRUE to force use of mmap() instead of
   * read() for reading data.
   *
   * Use of mmap() is disabled by default since with mmap() there are a
   * number of occasions where the process/application will be notified of
   * read errors via a SIGBUS signal from the kernel, which will lead to
   * the application being killed if not handled by the application. This
   * is something that is difficult to work around for a library like
   * GStreamer, hence use of mmap() is disabled by default. Said errors
   * can occur for example when an external device (e.g. an external hard
   * drive or a portable music player) are unplugged while in use, or when
   * a CD/DVD medium cannot be be read because the medium is scratched or
   * otherwise damaged.
   *
   **/
  g_object_class_install_property (gobject_class, ARG_USEMMAP,
      g_param_spec_boolean ("use-mmap", "Use mmap to read data",
          "Whether to use mmap() instead of read()",
          DEFAULT_USEMMAP, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_READY));
  g_object_class_install_property (gobject_class, ARG_SEQUENTIAL,
      g_param_spec_boolean ("sequential", "Optimise for sequential mmap access",
          "Whether to use madvise to hint to the kernel that access to "
          "mmap pages will be sequential",
          DEFAULT_SEQUENTIAL, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_PLAYING));

  gobject_class->finalize = gst_file_src_finalize;

  gstbasesrc_class->start = GST_DEBUG_FUNCPTR (gst_file_src_start);
  gstbasesrc_class->stop = GST_DEBUG_FUNCPTR (gst_file_src_stop);
  gstbasesrc_class->is_seekable = GST_DEBUG_FUNCPTR (gst_file_src_is_seekable);
  gstbasesrc_class->get_size = GST_DEBUG_FUNCPTR (gst_file_src_get_size);
  gstbasesrc_class->create = GST_DEBUG_FUNCPTR (gst_file_src_create);
  gstbasesrc_class->query = GST_DEBUG_FUNCPTR (gst_file_src_query);

  if (sizeof (off_t) < 8) {
    GST_LOG ("No large file support, sizeof (off_t) = %" G_GSIZE_FORMAT "!",
        sizeof (off_t));
  }
}

static void
gst_file_src_init (GstFileSrc * src, GstFileSrcClass * g_class)
{
#ifdef HAVE_MMAP
  src->pagesize = getpagesize ();
#endif

  src->filename = NULL;
  src->fd = 0;
  src->uri = NULL;

  src->touch = DEFAULT_TOUCH;

  src->mapbuf = NULL;
  src->mapsize = DEFAULT_MMAPSIZE;      /* default is 4MB */
  src->use_mmap = DEFAULT_USEMMAP;
  src->sequential = DEFAULT_SEQUENTIAL;

  src->is_regular = FALSE;
}

static void
gst_file_src_finalize (GObject * object)
{
  GstFileSrc *src;

  src = GST_FILE_SRC (object);

  g_free (src->filename);
  g_free (src->uri);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static gboolean
gst_file_src_set_location (GstFileSrc * src, const gchar * location)
{
  GstState state;

  /* the element must be stopped in order to do this */
  GST_OBJECT_LOCK (src);
  state = GST_STATE (src);
  if (state != GST_STATE_READY && state != GST_STATE_NULL)
    goto wrong_state;
  GST_OBJECT_UNLOCK (src);

  g_free (src->filename);
  g_free (src->uri);

  /* clear the filename if we get a NULL (is that possible?) */
  if (location == NULL) {
    src->filename = NULL;
    src->uri = NULL;
  } else {
    /* we store the filename as received by the application. On Windows this
     * should be UTF8 */
    src->filename = g_strdup (location);
    src->uri = gst_filename_to_uri (location, NULL);
    GST_INFO ("filename : %s", src->filename);
    GST_INFO ("uri      : %s", src->uri);
  }
  g_object_notify (G_OBJECT (src), "location");
  gst_uri_handler_new_uri (GST_URI_HANDLER (src), src->uri);

  return TRUE;

  /* ERROR */
wrong_state:
  {
    g_warning ("Changing the `location' property on filesrc when a file is "
        "open is not supported.");
    GST_OBJECT_UNLOCK (src);
    return FALSE;
  }
}

static void
gst_file_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstFileSrc *src;

  g_return_if_fail (GST_IS_FILE_SRC (object));

  src = GST_FILE_SRC (object);

  switch (prop_id) {
    case ARG_LOCATION:
      gst_file_src_set_location (src, g_value_get_string (value));
      break;
    case ARG_MMAPSIZE:
      if ((src->mapsize % src->pagesize) == 0) {
        src->mapsize = g_value_get_ulong (value);
      } else {
        GST_INFO_OBJECT (src,
            "invalid mapsize, must be a multiple of pagesize, which is %d",
            src->pagesize);
      }
      break;
    case ARG_TOUCH:
      src->touch = g_value_get_boolean (value);
      break;
    case ARG_SEQUENTIAL:
      src->sequential = g_value_get_boolean (value);
      break;
    case ARG_USEMMAP:
      src->use_mmap = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_file_src_get_property (GObject * object, guint prop_id, GValue * value,
    GParamSpec * pspec)
{
  GstFileSrc *src;

  g_return_if_fail (GST_IS_FILE_SRC (object));

  src = GST_FILE_SRC (object);

  switch (prop_id) {
    case ARG_LOCATION:
      g_value_set_string (value, src->filename);
      break;
    case ARG_FD:
      g_value_set_int (value, src->fd);
      break;
    case ARG_MMAPSIZE:
      g_value_set_ulong (value, src->mapsize);
      break;
    case ARG_TOUCH:
      g_value_set_boolean (value, src->touch);
      break;
    case ARG_SEQUENTIAL:
      g_value_set_boolean (value, src->sequential);
      break;
    case ARG_USEMMAP:
      g_value_set_boolean (value, src->use_mmap);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

/***
 * mmap code below
 */

#ifdef HAVE_MMAP

/* GstMmapBuffer */

typedef struct _GstMmapBuffer GstMmapBuffer;
typedef struct _GstMmapBufferClass GstMmapBufferClass;

#define GST_TYPE_MMAP_BUFFER                         (gst_mmap_buffer_get_type())

#define GST_IS_MMAP_BUFFER(obj)  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_MMAP_BUFFER))
#define GST_IS_MMAP_BUFFER_CLASS(klass)  (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_MMAP_BUFFER))
#define GST_MMAP_BUFFER_GET_CLASS(obj)   (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_MMAP_BUFFER, GstMmapBufferClass))
#define GST_MMAP_BUFFER(obj)     (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_MMAP_BUFFER, GstMmapBuffer))
#define GST_MMAP_BUFFER_CLASS(klass)  (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_MMAP_BUFFER, GstMmapBufferClass))



struct _GstMmapBuffer
{
  GstBuffer buffer;

  GstFileSrc *filesrc;
};

struct _GstMmapBufferClass
{
  GstBufferClass buffer_class;
};

static void gst_mmap_buffer_finalize (GstMmapBuffer * mmap_buffer);

GType gst_mmap_buffer_get_type (void);

G_DEFINE_TYPE (GstMmapBuffer, gst_mmap_buffer, GST_TYPE_BUFFER);

static void
gst_mmap_buffer_class_init (GstMmapBufferClass * g_class)
{
  GstMiniObjectClass *mini_object_class = GST_MINI_OBJECT_CLASS (g_class);

  mini_object_class->finalize =
      (GstMiniObjectFinalizeFunction) gst_mmap_buffer_finalize;
}

static void
gst_mmap_buffer_init (GstMmapBuffer * buf)
{
  GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_READONLY);
  /* before we re-enable this flag, we probably need to fix _copy()
   * _make_writable(), etc. in GstMiniObject/GstBuffer as well */
  /* GST_BUFFER_FLAG_SET (buf, GST_BUFFER_FLAG_ORIGINAL); */
}

static void
gst_mmap_buffer_finalize (GstMmapBuffer * mmap_buffer)
{
  guint size;
  gpointer data;
  guint64 offset;
  GstFileSrc *src;
  GstBuffer *buffer = GST_BUFFER (mmap_buffer);

  /* get info */
  size = GST_BUFFER_SIZE (buffer);
  offset = GST_BUFFER_OFFSET (buffer);
  data = GST_BUFFER_DATA (buffer);
  src = mmap_buffer->filesrc;

  GST_LOG ("freeing mmap()d buffer at %" G_GUINT64_FORMAT "+%u", offset, size);

#ifdef MADV_DONTNEED
  /* madvise to tell the kernel what to do with it */
  if (madvise (data, size, MADV_DONTNEED) < 0) {
    GST_WARNING_OBJECT (src, "warning: madvise failed: %s", g_strerror (errno));
  }
#endif

  /* now unmap the memory */
  if (munmap (data, size) < 0) {
    GST_WARNING_OBJECT (src, "warning: munmap failed: %s", g_strerror (errno));
  }

  /* cast to unsigned long, since there's no gportable way to print
   * guint64 as hex */
  GST_LOG ("unmapped region %08lx+%08lx at %p",
      (gulong) offset, (gulong) size, data);

  GST_MINI_OBJECT_CLASS (gst_mmap_buffer_parent_class)->finalize
      (GST_MINI_OBJECT (mmap_buffer));
}

static GstBuffer *
gst_file_src_map_region (GstFileSrc * src, off_t offset, gsize size,
    gboolean testonly)
{
  GstBuffer *buf;
  void *mmapregion;

  g_return_val_if_fail (offset >= 0, NULL);

  /* FIXME ? use goffset and friends if we require glib >= 2.20 */
  GST_LOG_OBJECT (src, "mapping region %08" G_GINT64_MODIFIER "x+%08lx "
      "from file into memory", (gint64) offset, (gulong) size);

  mmapregion = mmap (NULL, size, PROT_READ, MAP_SHARED, src->fd, offset);

  if (mmapregion == NULL || mmapregion == MAP_FAILED)
    goto mmap_failed;

  GST_LOG_OBJECT (src, "mapped region %08lx+%08lx from file into memory at %p",
      (gulong) offset, (gulong) size, mmapregion);

  /* time to allocate a new mapbuf */
  buf = (GstBuffer *) gst_mini_object_new (GST_TYPE_MMAP_BUFFER);
  /* mmap() the data into this new buffer */
  GST_BUFFER_DATA (buf) = mmapregion;
  GST_MMAP_BUFFER (buf)->filesrc = src;

#ifdef MADV_SEQUENTIAL
  if (src->sequential) {
    /* madvise to tell the kernel what to do with it */
    if (madvise (mmapregion, size, MADV_SEQUENTIAL) < 0) {
      GST_WARNING_OBJECT (src, "warning: madvise failed: %s",
          g_strerror (errno));
    }
  }
#endif

  /* fill in the rest of the fields */
  GST_BUFFER_SIZE (buf) = size;
  GST_BUFFER_OFFSET (buf) = offset;
  GST_BUFFER_OFFSET_END (buf) = offset + size;
  GST_BUFFER_TIMESTAMP (buf) = GST_CLOCK_TIME_NONE;

  return buf;

  /* ERROR */
mmap_failed:
  {
    if (!testonly) {
      GST_ELEMENT_ERROR (src, RESOURCE, READ, (NULL),
          ("mmap (0x%08lx, %d, 0x%" G_GINT64_MODIFIER "x) failed: %s",
              (gulong) size, src->fd, (guint64) offset, g_strerror (errno)));
    }
    return NULL;
  }
}

static GstBuffer *
gst_file_src_map_small_region (GstFileSrc * src, off_t offset, gsize size)
{
  GstBuffer *ret;
  off_t mod;
  guint pagesize;

  GST_LOG_OBJECT (src,
      "attempting to map a small buffer at %" G_GUINT64_FORMAT "+%d",
      (guint64) offset, (gint) size);

  pagesize = src->pagesize;

  mod = offset % pagesize;

  /* if the offset starts at a non-page boundary, we have to special case */
  if (mod != 0) {
    gsize mapsize;
    off_t mapbase;
    GstBuffer *map;

    mapbase = offset - mod;
    mapsize = ((size + mod + pagesize - 1) / pagesize) * pagesize;

    GST_LOG_OBJECT (src,
        "not on page boundaries, resizing to map to %" G_GUINT64_FORMAT "+%d",
        (guint64) mapbase, (gint) mapsize);

    map = gst_file_src_map_region (src, mapbase, mapsize, FALSE);
    if (map == NULL)
      return NULL;

    ret = gst_buffer_create_sub (map, offset - mapbase, size);
    GST_BUFFER_OFFSET (ret) = GST_BUFFER_OFFSET (map) + offset - mapbase;

    gst_buffer_unref (map);
  } else {
    ret = gst_file_src_map_region (src, offset, size, FALSE);
  }

  return ret;
}

static GstFlowReturn
gst_file_src_create_mmap (GstFileSrc * src, guint64 offset, guint length,
    GstBuffer ** buffer)
{
  GstBuffer *buf = NULL;
  gsize readsize, mapsize;
  off_t readend, mapstart, mapend;
  int i;

  /* calculate end pointers so we don't have to do so repeatedly later */
  readsize = length;
  readend = offset + readsize;  /* note this is the byte *after* the read */

  mapstart = GST_BUFFER_OFFSET (src->mapbuf);
  mapsize = GST_BUFFER_SIZE (src->mapbuf);
  mapend = mapstart + mapsize;  /* note this is the byte *after* the map */

  GST_LOG ("attempting to read %08lx, %08lx, %08lx, %08lx",
      (unsigned long) readsize, (unsigned long) readend,
      (unsigned long) mapstart, (unsigned long) mapend);

  /* if the start is past the mapstart */
  if (offset >= mapstart) {
    /* if the end is before the mapend, the buffer is in current mmap region... */
    /* ('cause by definition if readend is in the buffer, so's readstart) */
    if (readend <= mapend) {
      GST_LOG_OBJECT (src, "read buf %" G_GUINT64_FORMAT "+%u lives in "
          "current mapbuf %u+%u, creating subbuffer of mapbuf",
          offset, (guint) readsize, (guint) mapstart, (guint) mapsize);
      buf = gst_buffer_create_sub (src->mapbuf, offset - mapstart, readsize);
      GST_BUFFER_OFFSET (buf) = offset;

      /* if the start actually is within the current mmap region, map an overlap buffer */
    } else if (offset < mapend) {
      GST_LOG_OBJECT (src, "read buf %" G_GUINT64_FORMAT "+%u starts in "
          "mapbuf %u+%u but ends outside, creating new mmap",
          offset, (guint) readsize, (guint) mapstart, (guint) mapsize);
      buf = gst_file_src_map_small_region (src, offset, readsize);
      if (buf == NULL)
        goto could_not_mmap;
    }

    /* the only other option is that buffer is totally outside, which means we search for it */

    /* now we can assume that the start is *before* the current mmap region */
    /* if the readend is past mapstart, we have two options */
  } else if (readend >= mapstart) {
    /* either the read buffer overlaps the start of the mmap region */
    /* or the read buffer fully contains the current mmap region    */
    /* either way, it's really not relevant, we just create a new region anyway */
    GST_LOG_OBJECT (src, "read buf %" G_GUINT64_FORMAT "+%d starts before "
        "mapbuf %d+%d, but overlaps it", (guint64) offset, (gint) readsize,
        (gint) mapstart, (gint) mapsize);
    buf = gst_file_src_map_small_region (src, offset, readsize);
    if (buf == NULL)
      goto could_not_mmap;
  }

  /* then deal with the case where the read buffer is totally outside */
  if (buf == NULL) {
    /* first check to see if there's a map that covers the right region already */
    GST_LOG_OBJECT (src, "searching for mapbuf to cover %" G_GUINT64_FORMAT
        "+%d", offset, (int) readsize);

    /* if the read buffer crosses a mmap region boundary, create a one-off region */
    if ((offset / src->mapsize) != (readend / src->mapsize)) {
      GST_LOG_OBJECT (src, "read buf %" G_GUINT64_FORMAT "+%d crosses a "
          "%d-byte boundary, creating a one-off", offset, (int) readsize,
          (int) src->mapsize);
      buf = gst_file_src_map_small_region (src, offset, readsize);
      if (buf == NULL)
        goto could_not_mmap;

      /* otherwise we will create a new mmap region and set it to the default */
    } else {
      gsize mapsize;

      off_t nextmap = offset - (offset % src->mapsize);

      GST_LOG_OBJECT (src, "read buf %" G_GUINT64_FORMAT "+%d in new mapbuf "
          "at %" G_GUINT64_FORMAT "+%d, mapping and subbuffering",
          offset, (gint) readsize, (guint64) nextmap, (gint) src->mapsize);
      /* first, we're done with the old mapbuf */
      gst_buffer_unref (src->mapbuf);
      mapsize = src->mapsize;

      /* double the mapsize as long as the readsize is smaller */
      while (readsize + offset > nextmap + mapsize) {
        GST_LOG_OBJECT (src, "readsize smaller then mapsize %08x %d",
            (guint) readsize, (gint) mapsize);
        mapsize <<= 1;
      }
      /* create a new one */
      src->mapbuf = gst_file_src_map_region (src, nextmap, mapsize, FALSE);
      if (src->mapbuf == NULL)
        goto could_not_mmap;

      /* subbuffer it */
      buf = gst_buffer_create_sub (src->mapbuf, offset - nextmap, readsize);
      GST_BUFFER_OFFSET (buf) =
          GST_BUFFER_OFFSET (src->mapbuf) + offset - nextmap;
    }
  }

  /* if we need to touch the buffer (to bring it into memory), do so */
  if (src->touch) {
    volatile guchar *p = GST_BUFFER_DATA (buf);

    /* read first byte of each page */
    for (i = 0; i < GST_BUFFER_SIZE (buf); i += src->pagesize)
      (void) p[i];
  }

  /* we're done, return the buffer */
  *buffer = buf;

  return GST_FLOW_OK;

  /* ERROR */
could_not_mmap:
  {
    return GST_FLOW_ERROR;
  }
}
#endif

/***
 * read code below
 * that is to say, you shouldn't read the code below, but the code that reads
 * stuff is below.  Well, you shouldn't not read the code below, feel free
 * to read it of course.  It's just that "read code below" is a pretty crappy
 * documentation string because it sounds like we're expecting you to read
 * the code to understand what it does, which, while true, is really not
 * the sort of attitude we want to be advertising.  No sir.
 *
 */

static GstFlowReturn
gst_file_src_create_read (GstFileSrc * src, guint64 offset, guint length,
    GstBuffer ** buffer)
{
  int ret;
  GstBuffer *buf;

  if (G_UNLIKELY (src->read_position != offset)) {
    off_t res;

    res = lseek (src->fd, offset, SEEK_SET);
    if (G_UNLIKELY (res < 0 || res != offset))
      goto seek_failed;

    src->read_position = offset;
  }

  buf = gst_buffer_try_new_and_alloc (length);
  if (G_UNLIKELY (buf == NULL && length > 0)) {
    GST_ERROR_OBJECT (src, "Failed to allocate %u bytes", length);
    return GST_FLOW_ERROR;
  }

  /* No need to read anything if length is 0 */
  if (length > 0) {
    GST_LOG_OBJECT (src, "Reading %d bytes at offset 0x%" G_GINT64_MODIFIER "x",
        length, offset);
    ret = read (src->fd, GST_BUFFER_DATA (buf), length);
    if (G_UNLIKELY (ret < 0))
      goto could_not_read;

    /* seekable regular files should have given us what we expected */
    if (G_UNLIKELY ((guint) ret < length && src->seekable))
      goto unexpected_eos;

    /* other files should eos if they read 0 and more was requested */
    if (G_UNLIKELY (ret == 0 && length > 0))
      goto eos;

    length = ret;
    GST_BUFFER_SIZE (buf) = length;
    GST_BUFFER_OFFSET (buf) = offset;
    GST_BUFFER_OFFSET_END (buf) = offset + length;

    src->read_position += length;
  }

  *buffer = buf;

  return GST_FLOW_OK;

  /* ERROR */
seek_failed:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, READ, (NULL), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }
could_not_read:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, READ, (NULL), GST_ERROR_SYSTEM);
    gst_buffer_unref (buf);
    return GST_FLOW_ERROR;
  }
unexpected_eos:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, READ, (NULL),
        ("unexpected end of file."));
    gst_buffer_unref (buf);
    return GST_FLOW_ERROR;
  }
eos:
  {
    GST_DEBUG ("non-regular file hits EOS");
    gst_buffer_unref (buf);
    return GST_FLOW_UNEXPECTED;
  }
}

static GstFlowReturn
gst_file_src_create (GstBaseSrc * basesrc, guint64 offset, guint length,
    GstBuffer ** buffer)
{
  GstFileSrc *src;
  GstFlowReturn ret;

  src = GST_FILE_SRC_CAST (basesrc);

#ifdef HAVE_MMAP
  if (src->using_mmap) {
    ret = gst_file_src_create_mmap (src, offset, length, buffer);
  } else {
    ret = gst_file_src_create_read (src, offset, length, buffer);
  }
#else
  ret = gst_file_src_create_read (src, offset, length, buffer);
#endif

  return ret;
}

static gboolean
gst_file_src_query (GstBaseSrc * basesrc, GstQuery * query)
{
  gboolean ret = FALSE;
  GstFileSrc *src = GST_FILE_SRC (basesrc);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_URI:
      gst_query_set_uri (query, src->uri);
      ret = TRUE;
      break;
    default:
      ret = FALSE;
      break;
  }

  if (!ret)
    ret = GST_BASE_SRC_CLASS (parent_class)->query (basesrc, query);

  return ret;
}

static gboolean
gst_file_src_is_seekable (GstBaseSrc * basesrc)
{
  GstFileSrc *src = GST_FILE_SRC (basesrc);

  return src->seekable;
}

static gboolean
gst_file_src_get_size (GstBaseSrc * basesrc, guint64 * size)
{
  struct stat stat_results;
  GstFileSrc *src;

  src = GST_FILE_SRC (basesrc);

  if (!src->seekable) {
    /* If it isn't seekable, we won't know the length (but fstat will still
     * succeed, and wrongly say our length is zero. */
    return FALSE;
  }

  if (fstat (src->fd, &stat_results) < 0)
    goto could_not_stat;

  *size = stat_results.st_size;

  return TRUE;

  /* ERROR */
could_not_stat:
  {
    return FALSE;
  }
}

/* open the file and mmap it, necessary to go to READY state */
static gboolean
gst_file_src_start (GstBaseSrc * basesrc)
{
  GstFileSrc *src = GST_FILE_SRC (basesrc);
  struct stat stat_results;

  if (src->filename == NULL || src->filename[0] == '\0')
    goto no_filename;

  GST_INFO_OBJECT (src, "opening file %s", src->filename);

  /* open the file */
  src->fd = gst_open (src->filename, O_RDONLY | O_BINARY, 0);

  if (src->fd < 0)
    goto open_failed;

  /* check if it is a regular file, otherwise bail out */
  if (fstat (src->fd, &stat_results) < 0)
    goto no_stat;

  if (S_ISDIR (stat_results.st_mode))
    goto was_directory;

  if (S_ISSOCK (stat_results.st_mode))
    goto was_socket;

  src->using_mmap = FALSE;
  src->read_position = 0;

  /* record if it's a regular (hence seekable and lengthable) file */
  if (S_ISREG (stat_results.st_mode))
    src->is_regular = TRUE;

#ifdef HAVE_MMAP
  if (src->use_mmap) {
    /* FIXME: maybe we should only try to mmap if it's a regular file */
    /* allocate the first mmap'd region if it's a regular file ? */
    src->mapbuf = gst_file_src_map_region (src, 0, src->mapsize, TRUE);
    if (src->mapbuf != NULL) {
      GST_DEBUG_OBJECT (src, "using mmap for file");
      src->using_mmap = TRUE;
      src->seekable = TRUE;
    }
  }
  if (src->mapbuf == NULL)
#endif
  {
    /* If not in mmap mode, we need to check if the underlying file is
     * seekable. */
    off_t res = lseek (src->fd, 0, SEEK_END);

    if (res < 0) {
      GST_LOG_OBJECT (src, "disabling seeking, not in mmap mode and lseek "
          "failed: %s", g_strerror (errno));
      src->seekable = FALSE;
    } else {
      src->seekable = TRUE;
    }
    lseek (src->fd, 0, SEEK_SET);
  }

  /* We can only really do seeking on regular files - for other file types, we
   * don't know their length, so seeking isn't useful/meaningful */
  src->seekable = src->seekable && src->is_regular;

  return TRUE;

  /* ERROR */
no_filename:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, NOT_FOUND,
        (_("No file name specified for reading.")), (NULL));
    return FALSE;
  }
open_failed:
  {
    switch (errno) {
      case ENOENT:
        GST_ELEMENT_ERROR (src, RESOURCE, NOT_FOUND, (NULL),
            ("No such file \"%s\"", src->filename));
        break;
      default:
        GST_ELEMENT_ERROR (src, RESOURCE, OPEN_READ,
            (_("Could not open file \"%s\" for reading."), src->filename),
            GST_ERROR_SYSTEM);
        break;
    }
    return FALSE;
  }
no_stat:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, OPEN_READ,
        (_("Could not get info on \"%s\"."), src->filename), (NULL));
    close (src->fd);
    return FALSE;
  }
was_directory:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, OPEN_READ,
        (_("\"%s\" is a directory."), src->filename), (NULL));
    close (src->fd);
    return FALSE;
  }
was_socket:
  {
    GST_ELEMENT_ERROR (src, RESOURCE, OPEN_READ,
        (_("File \"%s\" is a socket."), src->filename), (NULL));
    close (src->fd);
    return FALSE;
  }
}

/* unmap and close the file */
static gboolean
gst_file_src_stop (GstBaseSrc * basesrc)
{
  GstFileSrc *src = GST_FILE_SRC (basesrc);

  /* close the file */
  close (src->fd);

  /* zero out a lot of our state */
  src->fd = 0;
  src->is_regular = FALSE;

  if (src->mapbuf) {
    gst_buffer_unref (src->mapbuf);
    src->mapbuf = NULL;
  }

  return TRUE;
}

/*** GSTURIHANDLER INTERFACE *************************************************/

static GstURIType
gst_file_src_uri_get_type (void)
{
  return GST_URI_SRC;
}

static gchar **
gst_file_src_uri_get_protocols (void)
{
  static gchar *protocols[] = { (char *) "file", NULL };

  return protocols;
}

static const gchar *
gst_file_src_uri_get_uri (GstURIHandler * handler)
{
  GstFileSrc *src = GST_FILE_SRC (handler);

  return src->uri;
}

static gboolean
gst_file_src_uri_set_uri (GstURIHandler * handler, const gchar * uri)
{
  gchar *location, *hostname = NULL;
  gboolean ret = FALSE;
  GstFileSrc *src = GST_FILE_SRC (handler);
  GError *error = NULL;

  if (strcmp (uri, "file://") == 0) {
    /* Special case for "file://" as this is used by some applications
     *  to test with gst_element_make_from_uri if there's an element
     *  that supports the URI protocol. */
    gst_file_src_set_location (src, NULL);
    return TRUE;
  }

  location = g_filename_from_uri (uri, &hostname, &error);

  if (!location || error) {
    if (error) {
      GST_WARNING_OBJECT (src, "Invalid URI '%s' for filesrc: %s", uri,
          error->message);
      g_error_free (error);
    } else {
      GST_WARNING_OBJECT (src, "Invalid URI '%s' for filesrc", uri);
    }
    goto beach;
  }

  if ((hostname) && (strcmp (hostname, "localhost"))) {
    /* Only 'localhost' is permitted */
    GST_WARNING_OBJECT (src, "Invalid hostname '%s' for filesrc", hostname);
    goto beach;
  }
#ifdef G_OS_WIN32
  /* Unfortunately, g_filename_from_uri() doesn't handle some UNC paths
   * correctly on windows, it leaves them with an extra backslash
   * at the start if they're of the mozilla-style file://///host/path/file 
   * form. Correct this.
   */
  if (location[0] == '\\' && location[1] == '\\' && location[2] == '\\')
    g_memmove (location, location + 1, strlen (location + 1) + 1);
#endif

  ret = gst_file_src_set_location (src, location);

beach:
  if (location)
    g_free (location);
  if (hostname)
    g_free (hostname);

  return ret;
}

static void
gst_file_src_uri_handler_init (gpointer g_iface, gpointer iface_data)
{
  GstURIHandlerInterface *iface = (GstURIHandlerInterface *) g_iface;

  iface->get_type = gst_file_src_uri_get_type;
  iface->get_protocols = gst_file_src_uri_get_protocols;
  iface->get_uri = gst_file_src_uri_get_uri;
  iface->set_uri = gst_file_src_uri_set_uri;
}
