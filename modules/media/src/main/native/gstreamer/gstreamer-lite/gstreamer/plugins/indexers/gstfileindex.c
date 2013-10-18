/* GStreamer
 * Copyright (C) 2003 Erik Walthinsen <omega@cse.ogi.edu>
 *               2003 Joshua N Pritikin <jpritikin@pobox.com>
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

#include <gst/gst.h>

#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>

#ifdef GST_DISABLE_DEPRECATED
#include <libxml/parser.h>
#endif

#include "gstindexers.h"

#define GST_TYPE_FILE_INDEX             \
  (gst_file_index_get_type ())
#define GST_FILE_INDEX(obj)             \
  (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_FILE_INDEX, GstFileIndex))
#define GST_FILE_INDEX_CLASS(klass)     \
  (G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_FILE_INDEX, GstFileIndexClass))
#define GST_IS_FILE_INDEX(obj)          \
  (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_FILE_INDEX))
#define GST_IS_FILE_INDEX_CLASS(klass)    \
  (GST_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_FILE_INDEX))

/*
 * Object model:
 *
 * We build an index to each entry for each id.
 *
 *
 *  fileindex
 *    -----------------------------...
 *    !                  !
 *   id1                 id2
 *    !
 *   GArray
 *
 * The fileindex creates a FileIndexId object for each writer id, a
 * Hashtable is kept to map the id to the FileIndexId
 *
 * The FileIndexId also keeps all the values in a sorted GArray.
 *
 * Finding a value for an id/format requires locating the correct GArray,
 * then do a binary search to get the required value.
 *
 * Unlike gstmemindex:  All formats are assumed to sort to the
 * same order.  All formats are assumed to be available from
 * any entry.
 */

/*
 * Each array element is (32bits flags, nformats * 64bits)
 */
typedef struct
{
  gint id;
  gchar *id_desc;
  gint nformats;
  GstFormat *format;
  GArray *array;
}
GstFileIndexId;

typedef struct _GstFileIndex GstFileIndex;
typedef struct _GstFileIndexClass GstFileIndexClass;

#define ARRAY_ROW_SIZE(_ii) \
  (sizeof (gint32) + (_ii)->nformats * sizeof (gint64))
#define ARRAY_TOTAL_SIZE(_ii) \
  (_ii->array->len * ARRAY_ROW_SIZE(_ii))

/* don't forget to convert to/from BE byte-order */
#define ARRAY_ROW_FLAGS(_row) \
  (*((gint32*) (_row)))
#define ARRAY_ROW_VALUE(_row,_vx) \
  (*(gint64*) (((gchar*)(_row)) + sizeof (gint32) + (_vx) * sizeof (gint64)))

GST_DEBUG_CATEGORY_STATIC (DC);
#define GST_CAT_DEFAULT DC

struct _GstFileIndex
{
  GstIndex parent;

  gchar *location;
  gboolean is_loaded;
  GSList *unresolved;
  gint next_id;
  GHashTable *id_index;

  GstIndexEntry *ret_entry;     /* hack to avoid leaking memory */
};

struct _GstFileIndexClass
{
  GstIndexClass parent_class;
};

enum
{
  ARG_0,
  ARG_LOCATION,
};

static void gst_file_index_dispose (GObject * object);

static void
gst_file_index_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void
gst_file_index_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static gboolean
gst_file_index_get_writer_id (GstIndex * _index, gint * id,
    gchar * writer_string);

static void gst_file_index_commit (GstIndex * index, gint writer_id);
static void gst_file_index_add_entry (GstIndex * index, GstIndexEntry * entry);
static GstIndexEntry *gst_file_index_get_assoc_entry (GstIndex * index, gint id,
    GstIndexLookupMethod method,
    GstAssocFlags flags,
    GstFormat format, gint64 value, GCompareDataFunc func, gpointer user_data);

#define CLASS(file_index)  GST_FILE_INDEX_CLASS (G_OBJECT_GET_CLASS (file_index))

GType gst_file_index_get_type (void);

G_DEFINE_TYPE (GstFileIndex, gst_file_index, GST_TYPE_INDEX);

static void
gst_file_index_class_init (GstFileIndexClass * klass)
{
  GObjectClass *gobject_class;
  GstIndexClass *gstindex_class;

  gobject_class = (GObjectClass *) klass;
  gstindex_class = (GstIndexClass *) klass;

  gobject_class->dispose = gst_file_index_dispose;
  gobject_class->set_property = gst_file_index_set_property;
  gobject_class->get_property = gst_file_index_get_property;

  gstindex_class->add_entry = gst_file_index_add_entry;
  gstindex_class->get_assoc_entry = gst_file_index_get_assoc_entry;
  gstindex_class->commit = gst_file_index_commit;
  gstindex_class->get_writer_id = gst_file_index_get_writer_id;

  g_object_class_install_property (gobject_class, ARG_LOCATION,
      g_param_spec_string ("location", "File Location",
          "Location of the index file", NULL,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
}

static void
gst_file_index_init (GstFileIndex * index)
{
  GST_DEBUG ("created new file index");

  index->id_index = g_hash_table_new (g_int_hash, g_int_equal);
}

static void
_file_index_id_free (GstFileIndexId * index_id, gboolean is_mmapped)
{
  if (index_id->id_desc)
    g_free (index_id->id_desc);
  if (index_id->format)
    g_free (index_id->format);
  if (index_id->array) {
    if (is_mmapped)
      munmap (index_id->array->data, ARRAY_TOTAL_SIZE (index_id));
    g_array_free (index_id->array, !is_mmapped);
  }
  g_slice_free (GstFileIndexId, index_id);
}

static gboolean
_id_index_free_helper (gpointer _key, GstFileIndexId * index_id,
    GstFileIndex * index)
{
  _file_index_id_free (index_id, index->is_loaded);
  return TRUE;
}

static void
gst_file_index_dispose (GObject * object)
{
  GstFileIndex *index = GST_FILE_INDEX (object);

  if (index->location) {
    g_free (index->location);
    index->location = NULL;
  }

  {
    GSList *elem;

    for (elem = index->unresolved; elem; elem = g_slist_next (elem))
      _file_index_id_free (elem->data, index->is_loaded);
    g_slist_free (index->unresolved);
    index->unresolved = NULL;
  }

  g_hash_table_foreach_steal (index->id_index,
      (GHRFunc) _id_index_free_helper, index);
  g_hash_table_destroy (index->id_index);
  index->id_index = NULL;

  gst_index_entry_free (index->ret_entry);      /* hack */

  G_OBJECT_CLASS (gst_file_index_parent_class)->dispose (object);
}

struct fi_find_writer_context
{
  const gchar *writer_string;
  GstFileIndexId *ii;
};

static void
_fi_find_writer (gpointer key, gpointer val, gpointer data)
{
  struct fi_find_writer_context *cx = data;
  GstFileIndexId *ii = val;

  if (strcmp (ii->id_desc, cx->writer_string) == 0)
    cx->ii = ii;
}

static gboolean
gst_file_index_get_writer_id (GstIndex * _index,
    gint * id, gchar * writer_string)
{
  GstFileIndex *index = GST_FILE_INDEX (_index);
  GSList *pending = index->unresolved;
  gboolean match = FALSE;
  GSList *elem;

  if (!index->is_loaded)
    return FALSE;

  g_return_val_if_fail (id, FALSE);
  g_return_val_if_fail (writer_string, FALSE);

  index->unresolved = NULL;

  for (elem = pending; elem; elem = g_slist_next (elem)) {
    GstFileIndexId *ii = elem->data;

    if (strcmp (ii->id_desc, writer_string) != 0) {
      index->unresolved = g_slist_prepend (index->unresolved, ii);
      continue;
    }

    if (match) {
      GST_WARNING_OBJECT (index, "Duplicate matches for writer '%s'",
          writer_string);
      continue;
    }

    ii->id = *id = ++index->next_id;
    g_hash_table_insert (index->id_index, &ii->id, ii);
    match = TRUE;
  }

  g_slist_free (pending);

  if (!match) {
    struct fi_find_writer_context cx;

    cx.writer_string = writer_string;
    cx.ii = NULL;
    g_hash_table_foreach (index->id_index, _fi_find_writer, &cx);

    if (cx.ii) {
      match = TRUE;
      GST_DEBUG_OBJECT (index, "Resolved writer '%s' again", writer_string);
    } else
      GST_WARNING_OBJECT (index, "Can't resolve writer '%s'", writer_string);
  }

  return match;
}

static void
_fc_alloc_array (GstFileIndexId * id_index)
{
  g_assert (!id_index->array);
  id_index->array =
      g_array_sized_new (FALSE, FALSE, ARRAY_ROW_SIZE (id_index), 0);
}

static void
gst_file_index_load (GstFileIndex * index)
{
  xmlDocPtr doc;
  xmlNodePtr root, part;
  xmlChar *val;

  g_assert (index->location);
  g_return_if_fail (!index->is_loaded);

  {
    gchar *path = g_strdup_printf ("%s/gstindex.xml", index->location);
    GError *err = NULL;
    gchar *buf;
    gsize len;

    g_file_get_contents (path, &buf, &len, &err);
    g_free (path);
    if (err) {
      GST_ERROR_OBJECT (index, "%s", err->message);
      return;
    }

    doc = xmlParseMemory (buf, len);
    g_free (buf);
  }

  //xmlDocFormatDump (stderr, doc, TRUE);

  root = doc->xmlRootNode;
  if (strcmp ((char *) root->name, "gstfileindex") != 0) {
    GST_ERROR_OBJECT (index, "root node isn't a gstfileindex");
    return;
  }

  val = xmlGetProp (root, (xmlChar *) "version");
  if (!val || atoi ((char *) val) != 1) {
    GST_ERROR_OBJECT (index, "version != 1");
    return;
  }
  free (val);

  for (part = root->children; part; part = part->next) {
    if (strcmp ((char *) part->name, "writers") == 0) {
      xmlNodePtr writer;

      for (writer = part->children; writer; writer = writer->next) {
        xmlChar *datafile = xmlGetProp (writer, (xmlChar *) "datafile");
        gchar *path = g_strdup_printf ("%s/%s", index->location, datafile);
        int fd;
        GstFileIndexId *id_index;
        xmlNodePtr wpart;
        xmlChar *entries_str;
        gpointer array_data;

        free (datafile);

        fd = open (path, O_RDONLY);
        g_free (path);
        if (fd < 0) {
          GST_ERROR_OBJECT (index,
              "Can't open '%s': %s", path, g_strerror (errno));
          continue;
        }

        id_index = g_slice_new0 (GstFileIndexId);
        id_index->id_desc = (char *) xmlGetProp (writer, (xmlChar *) "id");

        for (wpart = writer->children; wpart; wpart = wpart->next) {
          if (strcmp ((char *) wpart->name, "formats") == 0) {
            xmlChar *count_str = xmlGetProp (wpart, (xmlChar *) "count");
            gint fx = 0;
            xmlNodePtr format;

            id_index->nformats = atoi ((char *) count_str);
            free (count_str);

            id_index->format = g_new (GstFormat, id_index->nformats);

            for (format = wpart->children; format; format = format->next) {
              xmlChar *nick = xmlGetProp (format, (xmlChar *) "nick");
              GstFormat fmt = gst_format_get_by_nick ((gchar *) nick);

              if (fmt == GST_FORMAT_UNDEFINED)
                GST_ERROR_OBJECT (index, "format '%s' undefined", nick);
              g_assert (fx < id_index->nformats);
              id_index->format[fx++] = fmt;
              free (nick);
            }
          } else
            GST_INFO_OBJECT (index, "unknown wpart '%s'", wpart->name);
        }

        g_assert (id_index->nformats > 0);
        _fc_alloc_array (id_index);
        g_assert (id_index->array->data == NULL);       /* little bit risky */

        entries_str = xmlGetProp (writer, (xmlChar *) "entries");
        id_index->array->len = atoi ((char *) entries_str);
        free (entries_str);

        array_data =
            mmap (NULL, ARRAY_TOTAL_SIZE (id_index), PROT_READ, MAP_SHARED, fd,
            0);
        close (fd);
        if (array_data == MAP_FAILED) {
          GST_ERROR_OBJECT (index,
              "mmap %s failed: %s", path, g_strerror (errno));
          continue;
        }

        id_index->array->data = array_data;

        index->unresolved = g_slist_prepend (index->unresolved, id_index);
      }
    } else
      GST_INFO_OBJECT (index, "unknown part '%s'", part->name);
  }

  xmlFreeDoc (doc);

  GST_OBJECT_FLAG_UNSET (index, GST_INDEX_WRITABLE);
  index->is_loaded = TRUE;
  GST_LOG_OBJECT (index, "index %s loaded OK", index->location);
}

static void
gst_file_index_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec)
{
  GstFileIndex *index = GST_FILE_INDEX (object);

  switch (prop_id) {
    case ARG_LOCATION:
      if (index->location)
        g_free (index->location);
      index->location = g_value_dup_string (value);

      if (index->location && !g_hash_table_size (index->id_index))
        gst_file_index_load (index);
      break;
  }
}

static void
gst_file_index_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  GstFileIndex *index = GST_FILE_INDEX (object);

  switch (prop_id) {
    case ARG_LOCATION:
      g_value_set_string (value, index->location);
      break;
  }
}

static void
_file_index_id_save_xml (gpointer _key, GstFileIndexId * ii, xmlNodePtr writers)
{
  const gint bufsize = 16;
  gchar buf[16];
  xmlNodePtr writer;
  xmlNodePtr formats;
  gint xx;

  if (!ii->array) {
    GST_INFO ("Index for %s is empty", ii->id_desc);
    return;
  }

  writer = xmlNewChild (writers, NULL, (xmlChar *) "writer", NULL);
  xmlSetProp (writer, (xmlChar *) "id", (xmlChar *) ii->id_desc);
  g_snprintf (buf, bufsize, "%d", ii->array->len);
  xmlSetProp (writer, (xmlChar *) "entries", (xmlChar *) buf);
  g_snprintf (buf, bufsize, "%d", ii->id);      /* any unique number is OK */
  xmlSetProp (writer, (xmlChar *) "datafile", (xmlChar *) buf);

  formats = xmlNewChild (writer, NULL, (xmlChar *) "formats", NULL);
  g_snprintf (buf, bufsize, "%d", ii->nformats);
  xmlSetProp (formats, (xmlChar *) "count", (xmlChar *) buf);

  for (xx = 0; xx < ii->nformats; xx++) {
    xmlNodePtr format = xmlNewChild (formats, NULL, (xmlChar *) "format", NULL);
    const GstFormatDefinition *def = gst_format_get_details (ii->format[xx]);

    xmlSetProp (format, (xmlChar *) "nick", (xmlChar *) def->nick);
  }
}

/*
  We must save the binary data in separate files because
  mmap wants getpagesize() alignment.  If we append all
  the data to one file then we don't know the appropriate
  padding since the page size isn't fixed.
*/
static void
_file_index_id_save_entries (gpointer * _key,
    GstFileIndexId * ii, gchar * prefix)
{
  GError *err;
  gchar *path;
  GIOChannel *chan;

  if (!ii->array)
    return;

  err = NULL;
  path = g_strdup_printf ("%s/%d", prefix, ii->id);
  chan = g_io_channel_new_file (path, "w", &err);
  g_free (path);
  if (err)
    goto fail;

  g_io_channel_set_encoding (chan, NULL, &err);
  if (err)
    goto fail;

  g_io_channel_write_chars (chan,
      ii->array->data, ARRAY_TOTAL_SIZE (ii), NULL, &err);
  if (err)
    goto fail;

  g_io_channel_shutdown (chan, TRUE, &err);
  if (err)
    goto fail;

  g_io_channel_unref (chan);
  return;

fail:
  GST_ERROR ("%s", err->message);
}

/*
  We have to save the whole set of indexes into a single file
  so it doesn't make sense to commit only a single writer.

  i suggest:

  gst_index_commit (index, -1);
*/
static void
gst_file_index_commit (GstIndex * _index, gint _writer_id)
{
  GstFileIndex *index = GST_FILE_INDEX (_index);
  xmlDocPtr doc;
  xmlNodePtr writers;
  GError *err = NULL;
  gchar *path;
  GIOChannel *tocfile;

  g_return_if_fail (index->location);
  g_return_if_fail (!index->is_loaded);

  GST_OBJECT_FLAG_UNSET (index, GST_INDEX_WRITABLE);

  doc = xmlNewDoc ((xmlChar *) "1.0");
  doc->xmlRootNode =
      xmlNewDocNode (doc, NULL, (xmlChar *) "gstfileindex", NULL);
  xmlSetProp (doc->xmlRootNode, (xmlChar *) "version", (xmlChar *) "1");

  writers = xmlNewChild (doc->xmlRootNode, NULL, (xmlChar *) "writers", NULL);
  g_hash_table_foreach (index->id_index,
      (GHFunc) _file_index_id_save_xml, writers);

  if (mkdir (index->location, 0777) && errno != EEXIST) {
    GST_ERROR_OBJECT (index, "mkdir %s: %s", index->location,
        g_strerror (errno));
    return;
  }

  path = g_strdup_printf ("%s/gstindex.xml", index->location);
  tocfile = g_io_channel_new_file (path, "w", &err);
  g_free (path);
  if (err) {
    GST_ERROR_OBJECT (index, "%s", err->message);
    return;
  }

  g_io_channel_set_encoding (tocfile, NULL, &err);
  if (err) {
    GST_ERROR_OBJECT (index, "%s", err->message);
    return;
  }

  {
    xmlChar *xmlmem;
    int xmlsize;

    xmlDocDumpMemory (doc, &xmlmem, &xmlsize);
    g_io_channel_write_chars (tocfile, (gchar *) xmlmem, xmlsize, NULL, &err);
    if (err) {
      GST_ERROR_OBJECT (index, "%s", err->message);
      return;
    }
    xmlFreeDoc (doc);
    free (xmlmem);
  }

  g_io_channel_shutdown (tocfile, TRUE, &err);
  if (err) {
    GST_ERROR_OBJECT (index, "%s", err->message);
    return;
  }

  g_io_channel_unref (tocfile);

  g_hash_table_foreach (index->id_index,
      (GHFunc) _file_index_id_save_entries, index->location);
}

static void
gst_file_index_add_id (GstIndex * index, GstIndexEntry * entry)
{
  GstFileIndex *fileindex = GST_FILE_INDEX (index);
  GstFileIndexId *id_index;

  id_index = g_hash_table_lookup (fileindex->id_index, &entry->id);

  if (!id_index) {
    id_index = g_slice_new0 (GstFileIndexId);

    id_index->id = entry->id;
    id_index->id_desc = g_strdup (entry->data.id.description);

    /* It would be useful to know the GType of the writer so
       we can try to cope with changes in the id_desc path. */

    g_hash_table_insert (fileindex->id_index, &id_index->id, id_index);
  }
}

/* This algorithm differs from libc bsearch in the handling
   of non-exact matches. */

static gboolean
_fc_bsearch (GArray * ary,
    gint stride,
    gint * ret,
    GCompareDataFunc compare, gconstpointer sample, gpointer user_data)
{
  gint first, last;
  gint mid;
  gint midsize;
  gint cmp;
  gint tx;

  g_return_val_if_fail (compare, FALSE);

  if (!ary->len) {
    if (ret)
      *ret = 0;
    return FALSE;
  }

  first = 0;
  last = ary->len - 1;

  midsize = last - first;

  while (midsize > 1) {
    mid = first + midsize / 2;

    cmp = (*compare) (sample, ary->data + mid * stride, user_data);

    if (cmp == 0) {
      /* if there are multiple matches then scan for the first match */
      while (mid > 0 &&
          (*compare) (sample, ary->data + (mid - 1) * stride, user_data) == 0)
        --mid;

      if (ret)
        *ret = mid;
      return TRUE;
    }

    if (cmp < 0)
      last = mid - 1;
    else
      first = mid + 1;

    midsize = last - first;
  }

  for (tx = first; tx <= last; tx++) {
    cmp = (*compare) (sample, ary->data + tx * stride, user_data);

    if (cmp < 0) {
      if (ret)
        *ret = tx;
      return FALSE;
    }
    if (cmp == 0) {
      if (ret)
        *ret = tx;
      return TRUE;
    }
  }

  if (ret)
    *ret = last + 1;
  return FALSE;
}

static gint
file_index_compare (gconstpointer sample, gconstpointer row, gpointer user_data)
{
  //GstFileIndexId *id_index = user_data;
  const GstIndexAssociation *ca = sample;
  gint64 val1 = ca->value;
  gint64 val2_be = ARRAY_ROW_VALUE (row, ca->format);
  gint64 val2 = GINT64_FROM_BE (val2_be);
  gint64 diff = val2 - val1;

  return (diff == 0 ? 0 : (diff < 0 ? 1 : -1));
}

static void
gst_file_index_add_association (GstIndex * index, GstIndexEntry * entry)
{
  GstFileIndex *fileindex = GST_FILE_INDEX (index);
  GstFileIndexId *id_index;
  gint mx;
  GstIndexAssociation sample;
  gboolean exact;

  id_index = g_hash_table_lookup (fileindex->id_index, &entry->id);
  if (!id_index)
    return;

  if (!id_index->nformats) {
    gint fx;

    id_index->nformats = GST_INDEX_NASSOCS (entry);
    GST_LOG_OBJECT (fileindex, "creating %d formats for %d",
        id_index->nformats, entry->id);
    id_index->format = g_new (GstFormat, id_index->nformats);
    for (fx = 0; fx < id_index->nformats; fx++)
      id_index->format[fx] = GST_INDEX_ASSOC_FORMAT (entry, fx);
    _fc_alloc_array (id_index);
  } else {
    /* only sanity checking */
    if (id_index->nformats != GST_INDEX_NASSOCS (entry))
      GST_WARNING_OBJECT (fileindex, "arity change %d -> %d",
          id_index->nformats, GST_INDEX_NASSOCS (entry));
    else {
      gint fx;

      for (fx = 0; fx < id_index->nformats; fx++)
        if (id_index->format[fx] != GST_INDEX_ASSOC_FORMAT (entry, fx))
          GST_WARNING_OBJECT (fileindex, "format[%d] changed %d -> %d",
              fx, id_index->format[fx], GST_INDEX_ASSOC_FORMAT (entry, fx));
    }
  }

  /* this is a hack, we should use a private structure instead */
  sample.format = 0;
  sample.value = GST_INDEX_ASSOC_VALUE (entry, 0);

  exact =
      _fc_bsearch (id_index->array, ARRAY_ROW_SIZE (id_index),
      &mx, file_index_compare, &sample, id_index);

  if (exact) {
    /* maybe overwrite instead? */
    GST_DEBUG_OBJECT (index,
        "Ignoring duplicate index association at %" G_GINT64_FORMAT,
        GST_INDEX_ASSOC_VALUE (entry, 0));
    return;
  }

  {
    gchar *row_data = (gchar *) g_malloc (ARRAY_ROW_SIZE (id_index));
    gint fx;

    gint32 flags_host = GST_INDEX_ASSOC_FLAGS (entry);

    ARRAY_ROW_FLAGS (row_data) = GINT32_TO_BE (flags_host);

    for (fx = 0; fx < id_index->nformats; fx++) {
      gint64 val_host = GST_INDEX_ASSOC_VALUE (entry, fx);

      ARRAY_ROW_VALUE (row_data, fx) = GINT64_TO_BE (val_host);
    }

    g_array_insert_vals (id_index->array, mx, row_data, 1);

    g_free (row_data);
  }
}

/*
static void
show_entry (GstIndexEntry *entry)
{
  switch (entry->type) {
    case GST_INDEX_ENTRY_ID:
      g_print ("id %d describes writer %s\n", entry->id,
                      GST_INDEX_ID_DESCRIPTION (entry));
      break;
    case GST_INDEX_ENTRY_FORMAT:
      g_print ("%d: registered format %d for %s\n", entry->id,
                      GST_INDEX_FORMAT_FORMAT (entry),
                      GST_INDEX_FORMAT_KEY (entry));
      break;
    case GST_INDEX_ENTRY_ASSOCIATION:
    {
      gint i;

      g_print ("%d: %08x ", entry->id, GST_INDEX_ASSOC_FLAGS (entry));
      for (i = 0; i < GST_INDEX_NASSOCS (entry); i++) {
        g_print ("%d %" G_GINT64_FORMAT, GST_INDEX_ASSOC_FORMAT (entry, i),
                             GST_INDEX_ASSOC_VALUE (entry, i));
      }
      g_print ("\n");
      break;
    }
    default:
      break;
  }
}
*/

static void
gst_file_index_add_entry (GstIndex * index, GstIndexEntry * entry)
{
  GST_LOG_OBJECT (index, "adding this entry");

  switch (entry->type) {
    case GST_INDEX_ENTRY_ID:
      gst_file_index_add_id (index, entry);
      break;
    case GST_INDEX_ENTRY_ASSOCIATION:
      gst_file_index_add_association (index, entry);
      break;
    case GST_INDEX_ENTRY_OBJECT:
      GST_ERROR_OBJECT (index, "gst_file_index_add_object not implemented");
      break;
    case GST_INDEX_ENTRY_FORMAT:
      /*
         We infer the formats from the entry itself so this type of
         GST_INDEX_ENTRY_* can probably go away.
       */
      GST_DEBUG_OBJECT (index, "gst_file_index_add_format not implemented");
      break;
    default:
      break;
  }
}

static GstIndexEntry *
gst_file_index_get_assoc_entry (GstIndex * index,
    gint id,
    GstIndexLookupMethod method,
    GstAssocFlags flags,
    GstFormat format,
    gint64 value, GCompareDataFunc _ignore_func, gpointer _ignore_user_data)
{
  GstFileIndex *fileindex = GST_FILE_INDEX (index);
  GstFileIndexId *id_index;
  gint formatx = -1;
  gint fx;
  GstIndexAssociation sample;
  gint mx;
  gboolean exact;
  gpointer row_data;
  GstIndexEntry *entry;
  gint xx;

  g_return_val_if_fail (id > 0, NULL);

  id_index = g_hash_table_lookup (fileindex->id_index, &id);
  if (!id_index) {
    GST_WARNING_OBJECT (fileindex, "writer %d unavailable", id);
    return NULL;
  }

  for (fx = 0; fx < id_index->nformats; fx++)
    if (id_index->format[fx] == format) {
      formatx = fx;
      break;
    }

  if (formatx == -1) {
    GST_WARNING_OBJECT (fileindex, "format %d not available", format);
    return NULL;
  }

  /* this is a hack, we should use a private structure instead */
  sample.format = formatx;
  sample.value = value;

  exact = _fc_bsearch (id_index->array, ARRAY_ROW_SIZE (id_index),
      &mx, file_index_compare, &sample, id_index);

  if (!exact) {
    if (method == GST_INDEX_LOOKUP_EXACT)
      return NULL;
    else if (method == GST_INDEX_LOOKUP_BEFORE) {
      if (mx == 0)
        return NULL;
      mx -= 1;
    } else if (method == GST_INDEX_LOOKUP_AFTER) {
      if (mx == id_index->array->len)
        return NULL;
    }
  }

  row_data = id_index->array->data + mx * ARRAY_ROW_SIZE (id_index);

  /* if exact then ignore flags (?) */
  if (method != GST_INDEX_LOOKUP_EXACT)
    while ((GINT32_FROM_BE (ARRAY_ROW_FLAGS (row_data)) & flags) != flags) {
      if (method == GST_INDEX_LOOKUP_BEFORE)
        mx -= 1;
      else if (method == GST_INDEX_LOOKUP_AFTER)
        mx += 1;
      if (mx < 0 || mx >= id_index->array->len)
        return NULL;
      row_data = id_index->array->data + mx * ARRAY_ROW_SIZE (id_index);
    }

  /* entry memory management needs improvement FIXME */
  if (!fileindex->ret_entry)
    fileindex->ret_entry = g_slice_new0 (GstIndexEntry);
  entry = fileindex->ret_entry;
  if (entry->data.assoc.assocs) {
    g_free (entry->data.assoc.assocs);
    entry->data.assoc.assocs = NULL;
  }

  entry->type = GST_INDEX_ENTRY_ASSOCIATION;

  GST_INDEX_NASSOCS (entry) = id_index->nformats;
  entry->data.assoc.assocs = g_new (GstIndexAssociation, id_index->nformats);

  {
    gint32 flags_be = ARRAY_ROW_FLAGS (row_data);

    GST_INDEX_ASSOC_FLAGS (entry) = GINT32_FROM_BE (flags_be);

    for (xx = 0; xx < id_index->nformats; xx++) {
      gint64 val_be = ARRAY_ROW_VALUE (row_data, xx);

      GST_INDEX_ASSOC_FORMAT (entry, xx) = id_index->format[xx];
      GST_INDEX_ASSOC_VALUE (entry, xx) = GINT64_FROM_BE (val_be);
    }
  }

  return entry;
}

gboolean
gst_file_index_plugin_init (GstPlugin * plugin)
{
  GstIndexFactory *factory;

  factory = gst_index_factory_new ("fileindex",
      "A index that stores entries in file", gst_file_index_get_type ());

  if (factory == NULL) {
    return FALSE;
  }

  GST_PLUGIN_FEATURE (factory)->plugin_name = plugin->desc.name;
  GST_PLUGIN_FEATURE (factory)->loaded = TRUE;

  gst_registry_add_feature (gst_registry_get_default (),
      GST_PLUGIN_FEATURE (factory));

  GST_DEBUG_CATEGORY_INIT (DC, "GST_FILEINDEX", 0, NULL);

  return TRUE;
}
