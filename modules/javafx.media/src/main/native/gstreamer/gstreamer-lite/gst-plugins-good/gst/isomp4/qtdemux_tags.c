/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
 * Copyright (C) <2003> David A. Schleef <ds@schleef.org>
 * Copyright (C) <2006> Wim Taymans <wim@fluendo.com>
 * Copyright (C) <2007> Julien Moutte <julien@fluendo.com>
 * Copyright (C) <2009> Tim-Philipp Müller <tim centricular net>
 * Copyright (C) <2009> STEricsson <benjamin.gaignard@stericsson.com>
 * Copyright (C) <2013> Sreerenj Balachandran <sreerenj.balachandran@intel.com>
 * Copyright (C) <2013> Intel Corporation
 * Copyright (C) <2014> Centricular Ltd
 * Copyright (C) <2015> YouView TV Ltd.
 * Copyright (C) <2016> British Broadcasting Corporation
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

/* Parsing functions for various MP4 standard extension atom groups */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <stdio.h>
#ifdef GSTREAMER_LITE
#include <string.h>
#endif // GSTREAMER_LITE

#include <gst/base/gstbytereader.h>
#include <gst/tag/tag.h>

#include "qtdemux_tags.h"
#include "qtdemux_tree.h"
#include "qtdemux_types.h"
#include "fourcc.h"

static GstBuffer *
_gst_buffer_new_wrapped (gpointer mem, gsize size, GFreeFunc free_func)
{
  return gst_buffer_new_wrapped_full (free_func ? 0 : GST_MEMORY_FLAG_READONLY,
      mem, size, 0, size, mem, free_func);
}

/* check if major or compatible brand is 3GP */
static inline gboolean
qtdemux_is_brand_3gp (GstQTDemux * qtdemux, gboolean major)
{
  if (major) {
    return ((qtdemux->major_brand & GST_MAKE_FOURCC (255, 255, 0, 0)) ==
        FOURCC_3g__);
  } else if (qtdemux->comp_brands != NULL) {
    GstMapInfo map;
    guint8 *data;
    gsize size;
    gboolean res = FALSE;

    gst_buffer_map (qtdemux->comp_brands, &map, GST_MAP_READ);
    data = map.data;
    size = map.size;
    while (size >= 4) {
      res = res || ((QT_FOURCC (data) & GST_MAKE_FOURCC (255, 255, 0, 0)) ==
          FOURCC_3g__);
      data += 4;
      size -= 4;
    }
    gst_buffer_unmap (qtdemux->comp_brands, &map);
    return res;
  } else {
    return FALSE;
  }
}

/* check if tag is a spec'ed 3GP tag keyword storing a string */
static inline gboolean
qtdemux_is_string_tag_3gp (GstQTDemux * qtdemux, guint32 fourcc)
{
  return fourcc == FOURCC_cprt || fourcc == FOURCC_gnre || fourcc == FOURCC_titl
      || fourcc == FOURCC_dscp || fourcc == FOURCC_perf || fourcc == FOURCC_auth
      || fourcc == FOURCC_albm;
}

static void
qtdemux_tag_add_location (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  const gchar *env_vars[] = { "GST_QT_TAG_ENCODING", "GST_TAG_ENCODING", NULL };
  int offset;
  char *name;
  gchar *data;
  gdouble longitude, latitude, altitude;
  gint len;

  len = QT_UINT32 (node->data);
  if (len <= 14)
    goto short_read;

  data = node->data;
  offset = 14;

  /* TODO: language code skipped */

  name = gst_tag_freeform_string_to_utf8 (data + offset, -1, env_vars);

  if (!name) {
    /* do not alarm in trivial case, but bail out otherwise */
    if (*(data + offset) != 0) {
      GST_DEBUG_OBJECT (qtdemux, "failed to convert %s tag to UTF-8, "
          "giving up", tag);
    }
  } else {
    gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE,
        GST_TAG_GEO_LOCATION_NAME, name, NULL);
    offset += strlen (name);
    g_free (name);
  }

  if (len < offset + 2 + 4 + 4 + 4)
    goto short_read;

  /* +1 +1 = skip null-terminator and location role byte */
  offset += 1 + 1;
  /* table in spec says unsigned, semantics say negative has meaning ... */
  longitude = QT_SFP32 (data + offset);

  offset += 4;
  latitude = QT_SFP32 (data + offset);

  offset += 4;
  altitude = QT_SFP32 (data + offset);

  /* one invalid means all are invalid */
  if (longitude >= -180.0 && longitude <= 180.0 &&
      latitude >= -90.0 && latitude <= 90.0) {
    gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE,
        GST_TAG_GEO_LOCATION_LATITUDE, latitude,
        GST_TAG_GEO_LOCATION_LONGITUDE, longitude,
        GST_TAG_GEO_LOCATION_ELEVATION, altitude, NULL);
  }

  /* TODO: no GST_TAG_, so astronomical body and additional notes skipped */

  return;

  /* ERRORS */
short_read:
  {
    GST_DEBUG_OBJECT (qtdemux, "short read parsing 3GP location");
    return;
  }
}


static void
qtdemux_tag_add_year (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  guint16 y;
  GDate *date;
  gint len;

  len = QT_UINT32 (node->data);
  if (len < 14)
    return;

  y = QT_UINT16 ((guint8 *) node->data + 12);
  if (y == 0) {
    GST_DEBUG_OBJECT (qtdemux, "year: %u is not a valid year", y);
    return;
  }
  GST_DEBUG_OBJECT (qtdemux, "year: %u", y);

  date = g_date_new_dmy (1, 1, y);
  gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, date, NULL);
  g_date_free (date);
}

static void
qtdemux_tag_add_classification (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  int offset;
  char *tag_str = NULL;
  guint8 *entity;
  guint16 table;
  gint len;

  len = QT_UINT32 (node->data);
  if (len <= 20)
    goto short_read;

  offset = 12;
  entity = (guint8 *) node->data + offset;
  if (entity[0] == 0 || entity[1] == 0 || entity[2] == 0 || entity[3] == 0) {
    GST_DEBUG_OBJECT (qtdemux,
        "classification info: %c%c%c%c invalid classification entity",
        entity[0], entity[1], entity[2], entity[3]);
    return;
  }

  offset += 4;
  table = QT_UINT16 ((guint8 *) node->data + offset);

  /* Language code skipped */

  offset += 4;

  /* Tag format: "XXXX://Y[YYYY]/classification info string"
   * XXXX: classification entity, fixed length 4 chars.
   * Y[YYYY]: classification table, max 5 chars.
   */
  tag_str = g_strdup_printf ("----://%u/%s",
      table, (char *) node->data + offset);

  /* memcpy To be sure we're preserving byte order */
  memcpy (tag_str, entity, 4);
  GST_DEBUG_OBJECT (qtdemux, "classification info: %s", tag_str);

  gst_tag_list_add (taglist, GST_TAG_MERGE_APPEND, tag, tag_str, NULL);

  g_free (tag_str);

  return;

  /* ERRORS */
short_read:
  {
    GST_DEBUG_OBJECT (qtdemux, "short read parsing 3GP classification");
    return;
  }
}

static gboolean
qtdemux_tag_add_str_full (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  const gchar *env_vars[] = { "GST_QT_TAG_ENCODING", "GST_TAG_ENCODING", NULL };
  GNode *data;
  char *s;
  int len;
  guint32 type;
  int offset;
  gboolean ret = TRUE;
  const gchar *charset = NULL;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000001 && len > 16) {
      s = gst_tag_freeform_string_to_utf8 ((char *) data->data + 16, len - 16,
          env_vars);
      if (s) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag %s", GST_STR_NULL (s));
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, s, NULL);
        g_free (s);
      } else {
        GST_DEBUG_OBJECT (qtdemux, "failed to convert %s tag to UTF-8", tag);
      }
    }
  } else {
    len = QT_UINT32 (node->data);
    type = QT_UINT32 ((guint8 *) node->data + 4);
    if ((type >> 24) == 0xa9 && len > 8 + 4) {
      gint str_len;
      gint lang_code;

      /* Type starts with the (C) symbol, so the next data is a list
       * of (string size(16), language code(16), string) */

      str_len = QT_UINT16 ((guint8 *) node->data + 8);
      lang_code = QT_UINT16 ((guint8 *) node->data + 10);

      /* the string + fourcc + size + 2 16bit fields,
       * means that there are more tags in this atom */
      if (len > str_len + 8 + 4) {
        /* TODO how to represent the same tag in different languages? */
        GST_WARNING_OBJECT (qtdemux, "Ignoring metadata entry with multiple "
            "text alternatives, reading only first one");
      }

      offset = 12;
      len = MIN (len, str_len + 8 + 4); /* remove trailing strings that we don't use */
      GST_DEBUG_OBJECT (qtdemux, "found international text tag");

      if (lang_code < 0x800) {  /* MAC encoded string */
        charset = "mac";
      }
    } else if (len > 14 && qtdemux_is_string_tag_3gp (qtdemux,
            QT_FOURCC ((guint8 *) node->data + 4))) {
      guint32 type = QT_UINT32 ((guint8 *) node->data + 8);

      /* we go for 3GP style encoding if major brands claims so,
       * or if no hope for data be ok UTF-8, and compatible 3GP brand present */
      if (qtdemux_is_brand_3gp (qtdemux, TRUE) ||
          (qtdemux_is_brand_3gp (qtdemux, FALSE) &&
              ((type & 0x00FFFFFF) == 0x0) && (type >> 24 <= 0xF))) {
        offset = 14;
        /* 16-bit Language code is ignored here as well */
        GST_DEBUG_OBJECT (qtdemux, "found 3gpp text tag");
      } else {
        goto normal;
      }
    } else {
    normal:
      offset = 8;
      GST_DEBUG_OBJECT (qtdemux, "found normal text tag");
      ret = FALSE;              /* may have to fallback */
    }
    if (charset) {
      GError *err = NULL;

      s = g_convert ((gchar *) node->data + offset, len - offset, "utf8",
          charset, NULL, NULL, &err);
      if (err) {
        GST_DEBUG_OBJECT (qtdemux, "Failed to convert string from charset %s:"
            " %s(%d): %s", charset, g_quark_to_string (err->domain), err->code,
            err->message);
        g_error_free (err);
      }
    } else {
      s = gst_tag_freeform_string_to_utf8 ((char *) node->data + offset,
          len - offset, env_vars);
    }
    if (s) {
      GST_DEBUG_OBJECT (qtdemux, "adding tag %s", GST_STR_NULL (s));
      gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, s, NULL);
      g_free (s);
      ret = TRUE;
    } else {
      GST_DEBUG_OBJECT (qtdemux, "failed to convert %s tag to UTF-8", tag);
    }
  }
  return ret;
}

static void
qtdemux_tag_add_str (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  qtdemux_tag_add_str_full (qtdemux, taglist, tag, dummy, node);
}

static void
qtdemux_tag_add_keywords (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  const gchar *env_vars[] = { "GST_QT_TAG_ENCODING", "GST_TAG_ENCODING", NULL };
  guint8 *data;
  char *s, *t, *k = NULL;
  int len;
  int offset;
  int count;

  /* first try normal string tag if major brand not 3GP */
  if (!qtdemux_is_brand_3gp (qtdemux, TRUE)) {
    if (!qtdemux_tag_add_str_full (qtdemux, taglist, tag, dummy, node)) {
      /* hm, that did not work, maybe 3gpp storage in non-3gpp major brand;
       * let's try it 3gpp way after minor safety check */
      data = node->data;
      if (QT_UINT32 (data) < 15 || !qtdemux_is_brand_3gp (qtdemux, FALSE))
        return;
    } else
      return;
  }

  GST_DEBUG_OBJECT (qtdemux, "found 3gpp keyword tag");

  data = node->data;

  len = QT_UINT32 (data);
  if (len < 15)
    goto short_read;

  count = QT_UINT8 (data + 14);
  offset = 15;
  for (; count; count--) {
    gint slen;

    if (offset + 1 > len)
      goto short_read;
    slen = QT_UINT8 (data + offset);
    offset += 1;
    if (offset + slen > len)
      goto short_read;
    s = gst_tag_freeform_string_to_utf8 ((char *) node->data + offset,
        slen, env_vars);
    if (s) {
      GST_DEBUG_OBJECT (qtdemux, "adding keyword %s", GST_STR_NULL (s));
      if (k) {
        t = g_strjoin (",", k, s, NULL);
        g_free (s);
        g_free (k);
        k = t;
      } else {
        k = s;
      }
    } else {
      GST_DEBUG_OBJECT (qtdemux, "failed to convert keyword to UTF-8");
    }
    offset += slen;
  }

done:
  if (k) {
    GST_DEBUG_OBJECT (qtdemux, "adding tag %s", GST_STR_NULL (k));
    gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, k, NULL);
  }
  g_free (k);

  return;

  /* ERRORS */
short_read:
  {
    GST_DEBUG_OBJECT (qtdemux, "short read parsing 3GP keywords");
    goto done;
  }
}

static void
qtdemux_tag_add_num (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag1, const char *tag2, GNode * node)
{
  GNode *data;
  int len;
  int type;
  int n1, n2;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000000 && len >= 22) {
      n1 = QT_UINT16 ((guint8 *) data->data + 18);
      n2 = QT_UINT16 ((guint8 *) data->data + 20);
      if (n1 > 0) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag %s=%d", tag1, n1);
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag1, n1, NULL);
      }
      if (n2 > 0) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag %s=%d", tag2, n2);
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag2, n2, NULL);
      }
    }
  }
}

static void
qtdemux_tag_add_tmpo (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag1, const char *dummy, GNode * node)
{
  GNode *data;
  int len;
  int type;
  int n1;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    GST_DEBUG_OBJECT (qtdemux, "have tempo tag, type=%d,len=%d", type, len);
    /* some files wrongly have a type 0x0f=15, but it should be 0x15 */
    if ((type == 0x00000015 || type == 0x0000000f) && len >= 18) {
      n1 = QT_UINT16 ((guint8 *) data->data + 16);
      if (n1) {
        /* do not add bpm=0 */
        GST_DEBUG_OBJECT (qtdemux, "adding tag %d", n1);
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag1, (gdouble) n1,
            NULL);
      }
    }
  }
}

static void
qtdemux_tag_add_uint32 (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag1, const char *dummy, GNode * node)
{
  GNode *data;
  int len;
  int type;
  guint32 num;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    GST_DEBUG_OBJECT (qtdemux, "have %s tag, type=%d,len=%d", tag1, type, len);
    /* some files wrongly have a type 0x0f=15, but it should be 0x15 */
    if ((type == 0x00000015 || type == 0x0000000f) && len >= 20) {
      num = QT_UINT32 ((guint8 *) data->data + 16);
      if (num) {
        /* do not add num=0 */
        GST_DEBUG_OBJECT (qtdemux, "adding tag %d", num);
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag1, num, NULL);
      }
    }
  }
}

static void
qtdemux_tag_add_covr (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag1, const char *dummy, GNode * node)
{
  GNode *data;
  int len;
  int type;
  GstSample *sample;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    GST_DEBUG_OBJECT (qtdemux, "have covr tag, type=%d,len=%d", type, len);
    if ((type == 0x0000000d || type == 0x0000000e) && len > 16) {
      GstTagImageType image_type;

      if (gst_tag_list_get_tag_size (taglist, GST_TAG_IMAGE) == 0)
        image_type = GST_TAG_IMAGE_TYPE_FRONT_COVER;
      else
        image_type = GST_TAG_IMAGE_TYPE_NONE;

      if ((sample =
              gst_tag_image_data_to_image_sample ((guint8 *) data->data + 16,
                  len - 16, image_type))) {
        GST_DEBUG_OBJECT (qtdemux, "adding tag size %d", len - 16);
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag1, sample, NULL);
        gst_sample_unref (sample);
      }
    }
  }
}

static void
qtdemux_tag_add_date (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  GNode *data;
  GstDateTime *datetime = NULL;
  char *s;
  int len;
  int type;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (data) {
    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000001 && len > 16) {
      guint y, m = 1, d = 1;
      gint ret;

      s = g_strndup ((char *) data->data + 16, len - 16);
      GST_DEBUG_OBJECT (qtdemux, "adding date '%s'", s);
      datetime = gst_date_time_new_from_iso8601_string (s);
      if (datetime != NULL) {
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, GST_TAG_DATE_TIME,
            datetime, NULL);
        gst_date_time_unref (datetime);
      }

      ret = sscanf (s, "%u-%u-%u", &y, &m, &d);
      if (ret >= 1 && y > 1500 && y < 3000) {
        GDate *date;

        date = g_date_new_dmy (d, m, y);
        gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, date, NULL);
        g_date_free (date);
      } else {
        GST_DEBUG_OBJECT (qtdemux, "could not parse date string '%s'", s);
      }
      g_free (s);
    }
  }
}

static void
qtdemux_tag_add_gnre (GstQTDemux * qtdemux, GstTagList * taglist,
    const char *tag, const char *dummy, GNode * node)
{
  GNode *data;

  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);

  /* re-route to normal string tag if major brand says so
   * or no data atom and compatible brand suggests so */
  if (qtdemux_is_brand_3gp (qtdemux, TRUE) ||
      (qtdemux_is_brand_3gp (qtdemux, FALSE) && !data)) {
    qtdemux_tag_add_str (qtdemux, taglist, tag, dummy, node);
    return;
  }

  if (data) {
    guint len, type, n;

    len = QT_UINT32 (data->data);
    type = QT_UINT32 ((guint8 *) data->data + 8);
    if (type == 0x00000000 && len >= 18) {
      n = QT_UINT16 ((guint8 *) data->data + 16);
      if (n > 0) {
        const gchar *genre;

        genre = gst_tag_id3_genre_get (n - 1);
        if (genre != NULL) {
          GST_DEBUG_OBJECT (qtdemux, "adding %d [%s]", n, genre);
          gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, genre, NULL);
        }
      }
    }
  }
}

static void
qtdemux_add_double_tag_from_str (GstQTDemux * demux, GstTagList * taglist,
    const gchar * tag, guint8 * data, guint32 datasize)
{
  gdouble value;
  gchar *datacopy;

  /* make a copy to have \0 at the end */
  datacopy = g_strndup ((gchar *) data, datasize);

  /* convert the str to double */
  if (sscanf (datacopy, "%lf", &value) == 1) {
    GST_DEBUG_OBJECT (demux, "adding tag: %s [%s]", tag, datacopy);
    gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, tag, value, NULL);
  } else {
    GST_WARNING_OBJECT (demux, "Failed to parse double from string: %s",
        datacopy);
  }
  g_free (datacopy);
}


static void
qtdemux_tag_add_revdns (GstQTDemux * demux, GstTagList * taglist,
    const char *tag, const char *tag_bis, GNode * node)
{
  GNode *mean;
  GNode *name;
  GNode *data;
  guint32 meansize;
  guint32 namesize;
  guint32 datatype;
  guint32 datasize;
  const gchar *meanstr;
  const gchar *namestr;

  /* checking the whole ---- atom size for consistency */
  if (QT_UINT32 (node->data) <= 4 + 12 + 12 + 16) {
    GST_WARNING_OBJECT (demux, "Tag ---- atom is too small, ignoring");
    return;
  }

  mean = qtdemux_tree_get_child_by_type (node, FOURCC_mean);
  if (!mean) {
    GST_WARNING_OBJECT (demux, "No 'mean' atom found");
    return;
  }

  meansize = QT_UINT32 (mean->data);
  if (meansize <= 12) {
    GST_WARNING_OBJECT (demux, "Small mean atom, ignoring the whole tag");
    return;
  }
  meanstr = ((gchar *) mean->data) + 12;
  meansize -= 12;

  name = qtdemux_tree_get_child_by_type (node, FOURCC_name);
  if (!name) {
    GST_WARNING_OBJECT (demux, "'name' atom not found, ignoring tag");
    return;
  }

  namesize = QT_UINT32 (name->data);
  if (namesize <= 12) {
    GST_WARNING_OBJECT (demux, "'name' atom is too small, ignoring tag");
    return;
  }
  namestr = ((gchar *) name->data) + 12;
  namesize -= 12;

  /*
   * Data atom is:
   * uint32 - size
   * uint32 - name
   * uint8  - version
   * uint24 - data type
   * uint32 - all 0
   * rest   - the data
   */
  data = qtdemux_tree_get_child_by_type (node, FOURCC_data);
  if (!data) {
    GST_WARNING_OBJECT (demux, "No data atom in this tag");
    return;
  }
  datasize = QT_UINT32 (data->data);
  if (datasize <= 16) {
    GST_WARNING_OBJECT (demux, "Data atom too small");
    return;
  }
  datatype = QT_UINT32 (((gchar *) data->data) + 8) & 0xFFFFFF;

  if ((strncmp (meanstr, "com.apple.iTunes", meansize) == 0) ||
      (strncmp (meanstr, "org.hydrogenaudio.replaygain", meansize) == 0)) {
    static const struct
    {
      const gchar name[28];
      const gchar tag[28];
    } tags[] = {
      {
      "replaygain_track_gain", GST_TAG_TRACK_GAIN}, {
      "replaygain_track_peak", GST_TAG_TRACK_PEAK}, {
      "replaygain_album_gain", GST_TAG_ALBUM_GAIN}, {
      "replaygain_album_peak", GST_TAG_ALBUM_PEAK}, {
      "MusicBrainz Track Id", GST_TAG_MUSICBRAINZ_TRACKID}, {
      "MusicBrainz Artist Id", GST_TAG_MUSICBRAINZ_ARTISTID}, {
      "MusicBrainz Album Id", GST_TAG_MUSICBRAINZ_ALBUMID}, {
      "MusicBrainz Album Artist Id", GST_TAG_MUSICBRAINZ_ALBUMARTISTID}
    };
    int i;

    for (i = 0; i < G_N_ELEMENTS (tags); ++i) {
      if (!g_ascii_strncasecmp (tags[i].name, namestr, namesize)) {
        switch (gst_tag_get_type (tags[i].tag)) {
          case G_TYPE_DOUBLE:
            qtdemux_add_double_tag_from_str (demux, taglist, tags[i].tag,
                ((guint8 *) data->data) + 16, datasize - 16);
            break;
          case G_TYPE_STRING:
            qtdemux_tag_add_str (demux, taglist, tags[i].tag, NULL, node);
            break;
          default:
            /* not reached */
            break;
        }
        break;
      }
    }
    if (i == G_N_ELEMENTS (tags))
      goto unknown_tag;
  } else {
    goto unknown_tag;
  }

  return;

/* errors */
unknown_tag:
#ifndef GST_DISABLE_GST_DEBUG
  {
    gchar *namestr_dbg;
    gchar *meanstr_dbg;

    meanstr_dbg = g_strndup (meanstr, meansize);
    namestr_dbg = g_strndup (namestr, namesize);

    GST_WARNING_OBJECT (demux, "This tag %s:%s type:%u is not mapped, "
        "file a bug at bugzilla.gnome.org", meanstr_dbg, namestr_dbg, datatype);

    g_free (namestr_dbg);
    g_free (meanstr_dbg);
  }
#endif
  return;
}

static void
qtdemux_tag_add_id32 (GstQTDemux * demux, GstTagList * taglist, const char *tag,
    const char *tag_bis, GNode * node)
{
  guint8 *data;
  GstBuffer *buf;
  guint len;
  GstTagList *id32_taglist = NULL;

  GST_LOG_OBJECT (demux, "parsing ID32");

  data = node->data;
  len = GST_READ_UINT32_BE (data);

  /* need at least full box and language tag */
  if (len < 12 + 2)
    return;

  buf = gst_buffer_new_allocate (NULL, len - 14, NULL);
  gst_buffer_fill (buf, 0, data + 14, len - 14);

  id32_taglist = gst_tag_list_from_id3v2_tag (buf);
  if (id32_taglist) {
    GST_LOG_OBJECT (demux, "parsing ok");
    gst_tag_list_insert (taglist, id32_taglist, GST_TAG_MERGE_KEEP);
    gst_tag_list_unref (id32_taglist);
  } else {
    GST_LOG_OBJECT (demux, "parsing failed");
  }

  gst_buffer_unref (buf);
}

typedef void (*GstQTDemuxAddTagFunc) (GstQTDemux * demux, GstTagList * taglist,
    const char *tag, const char *tag_bis, GNode * node);

/* unmapped tags
FOURCC_pcst -> if media is a podcast -> bool
FOURCC_cpil -> if media is part of a compilation -> bool
FOURCC_pgap -> if media is part of a gapless context -> bool
FOURCC_tven -> the tv episode id e.g. S01E23 -> str
*/

static const struct
{
  guint32 fourcc;
  const gchar *gst_tag;
  const gchar *gst_tag_bis;
  const GstQTDemuxAddTagFunc func;
} add_funcs[] = {
  {
  FOURCC__nam, GST_TAG_TITLE, NULL, qtdemux_tag_add_str}, {
  FOURCC_titl, GST_TAG_TITLE, NULL, qtdemux_tag_add_str}, {
  FOURCC__grp, GST_TAG_GROUPING, NULL, qtdemux_tag_add_str}, {
  FOURCC__wrt, GST_TAG_COMPOSER, NULL, qtdemux_tag_add_str}, {
  FOURCC__ART, GST_TAG_ARTIST, NULL, qtdemux_tag_add_str}, {
  FOURCC_aART, GST_TAG_ALBUM_ARTIST, NULL, qtdemux_tag_add_str}, {
  FOURCC_perf, GST_TAG_ARTIST, NULL, qtdemux_tag_add_str}, {
  FOURCC_auth, GST_TAG_COMPOSER, NULL, qtdemux_tag_add_str}, {
  FOURCC__alb, GST_TAG_ALBUM, NULL, qtdemux_tag_add_str}, {
  FOURCC_albm, GST_TAG_ALBUM, NULL, qtdemux_tag_add_str}, {
  FOURCC_cprt, GST_TAG_COPYRIGHT, NULL, qtdemux_tag_add_str}, {
  FOURCC__cpy, GST_TAG_COPYRIGHT, NULL, qtdemux_tag_add_str}, {
  FOURCC__cmt, GST_TAG_COMMENT, NULL, qtdemux_tag_add_str}, {
  FOURCC__des, GST_TAG_DESCRIPTION, NULL, qtdemux_tag_add_str}, {
  FOURCC_desc, GST_TAG_DESCRIPTION, NULL, qtdemux_tag_add_str}, {
  FOURCC_dscp, GST_TAG_DESCRIPTION, NULL, qtdemux_tag_add_str}, {
  FOURCC__lyr, GST_TAG_LYRICS, NULL, qtdemux_tag_add_str}, {
  FOURCC__day, GST_TAG_DATE, NULL, qtdemux_tag_add_date}, {
  FOURCC_yrrc, GST_TAG_DATE, NULL, qtdemux_tag_add_year}, {
  FOURCC__too, GST_TAG_ENCODER, NULL, qtdemux_tag_add_str}, {
  FOURCC__inf, GST_TAG_COMMENT, NULL, qtdemux_tag_add_str}, {
  FOURCC_trkn, GST_TAG_TRACK_NUMBER, GST_TAG_TRACK_COUNT, qtdemux_tag_add_num}, {
  FOURCC_disk, GST_TAG_ALBUM_VOLUME_NUMBER, GST_TAG_ALBUM_VOLUME_COUNT,
        qtdemux_tag_add_num}, {
  FOURCC_disc, GST_TAG_ALBUM_VOLUME_NUMBER, GST_TAG_ALBUM_VOLUME_COUNT,
        qtdemux_tag_add_num}, {
  FOURCC__gen, GST_TAG_GENRE, NULL, qtdemux_tag_add_str}, {
  FOURCC_gnre, GST_TAG_GENRE, NULL, qtdemux_tag_add_gnre}, {
  FOURCC_tmpo, GST_TAG_BEATS_PER_MINUTE, NULL, qtdemux_tag_add_tmpo}, {
  FOURCC_covr, GST_TAG_IMAGE, NULL, qtdemux_tag_add_covr}, {
  FOURCC_sonm, GST_TAG_TITLE_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soal, GST_TAG_ALBUM_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soar, GST_TAG_ARTIST_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soaa, GST_TAG_ALBUM_ARTIST_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_soco, GST_TAG_COMPOSER_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_sosn, GST_TAG_SHOW_SORTNAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_tvsh, GST_TAG_SHOW_NAME, NULL, qtdemux_tag_add_str}, {
  FOURCC_tvsn, GST_TAG_SHOW_SEASON_NUMBER, NULL, qtdemux_tag_add_uint32}, {
  FOURCC_tves, GST_TAG_SHOW_EPISODE_NUMBER, NULL, qtdemux_tag_add_uint32}, {
  FOURCC_kywd, GST_TAG_KEYWORDS, NULL, qtdemux_tag_add_keywords}, {
  FOURCC_keyw, GST_TAG_KEYWORDS, NULL, qtdemux_tag_add_str}, {
  FOURCC__enc, GST_TAG_ENCODER, NULL, qtdemux_tag_add_str}, {
  FOURCC_loci, GST_TAG_GEO_LOCATION_NAME, NULL, qtdemux_tag_add_location}, {
  FOURCC_clsf, GST_QT_DEMUX_CLASSIFICATION_TAG, NULL,
        qtdemux_tag_add_classification}, {
  FOURCC__mak, GST_TAG_DEVICE_MANUFACTURER, NULL, qtdemux_tag_add_str}, {
  FOURCC__mod, GST_TAG_DEVICE_MODEL, NULL, qtdemux_tag_add_str}, {
  FOURCC__swr, GST_TAG_APPLICATION_NAME, NULL, qtdemux_tag_add_str}, {

    /* This is a special case, some tags are stored in this
     * 'reverse dns naming', according to:
     * http://atomicparsley.sourceforge.net/mpeg-4files.html and
     * bug #614471
     */
  FOURCC_____, "", NULL, qtdemux_tag_add_revdns}, {
    /* see http://www.mp4ra.org/specs.html for ID32 in meta box */
  FOURCC_ID32, "", NULL, qtdemux_tag_add_id32}
};

struct _GstQtDemuxTagList
{
  GstQTDemux *demux;
  GstTagList *taglist;
};
typedef struct _GstQtDemuxTagList GstQtDemuxTagList;

static void
qtdemux_tag_add_blob (GNode * node, GstQtDemuxTagList * qtdemuxtaglist)
{
  gint len;
  guint8 *data;
  GstBuffer *buf;
  gchar *media_type;
  const gchar *style;
  GstSample *sample;
  GstStructure *s;
  guint i;
  guint8 ndata[4];
  GstQTDemux *demux = qtdemuxtaglist->demux;
  GstTagList *taglist = qtdemuxtaglist->taglist;

  data = node->data;
  len = QT_UINT32 (data);
  buf = gst_buffer_new_and_alloc (len);
  gst_buffer_fill (buf, 0, data, len);

  /* heuristic to determine style of tag */
  if (QT_FOURCC (data + 4) == FOURCC_____ ||
      (len > 8 + 12 && QT_FOURCC (data + 12) == FOURCC_data))
    style = "itunes";
  else if (demux->major_brand == FOURCC_qt__)
    style = "quicktime";
  /* fall back to assuming iso/3gp tag style */
  else
    style = "iso";

  /* sanitize the name for the caps. */
  for (i = 0; i < 4; i++) {
    guint8 d = data[4 + i];
    if (g_ascii_isalnum (d))
      ndata[i] = g_ascii_tolower (d);
    else
      ndata[i] = '_';
  }

  media_type = g_strdup_printf ("application/x-gst-qt-%c%c%c%c-tag",
      ndata[0], ndata[1], ndata[2], ndata[3]);
  GST_DEBUG_OBJECT (demux, "media type %s", media_type);

  s = gst_structure_new (media_type, "style", G_TYPE_STRING, style, NULL);
  sample = gst_sample_new (buf, NULL, NULL, s);
  gst_buffer_unref (buf);
  g_free (media_type);

  GST_DEBUG_OBJECT (demux, "adding private tag; size %d, info %" GST_PTR_FORMAT,
      len, s);

  gst_tag_list_add (taglist, GST_TAG_MERGE_APPEND,
      GST_QT_DEMUX_PRIVATE_TAG, sample, NULL);

  gst_sample_unref (sample);
}

void
qtdemux_parse_udta (GstQTDemux * qtdemux, GstTagList * taglist, GNode * udta)
{
  GNode *meta;
  GNode *ilst;
  GNode *xmp_;
  GNode *node;
  gint i;
  GstQtDemuxTagList demuxtaglist;

  demuxtaglist.demux = qtdemux;
  demuxtaglist.taglist = taglist;

  meta = qtdemux_tree_get_child_by_type (udta, FOURCC_meta);
  if (meta != NULL) {
    ilst = qtdemux_tree_get_child_by_type (meta, FOURCC_ilst);
    if (ilst == NULL) {
      GST_LOG_OBJECT (qtdemux, "no ilst");
      return;
    }
  } else {
    ilst = udta;
    GST_LOG_OBJECT (qtdemux, "no meta so using udta itself");
  }

  i = 0;
  while (i < G_N_ELEMENTS (add_funcs)) {
    node = qtdemux_tree_get_child_by_type (ilst, add_funcs[i].fourcc);
    if (node) {
      gint len;

      len = QT_UINT32 (node->data);
      if (len < 12) {
        GST_DEBUG_OBJECT (qtdemux, "too small tag atom %" GST_FOURCC_FORMAT,
            GST_FOURCC_ARGS (add_funcs[i].fourcc));
      } else {
        add_funcs[i].func (qtdemux, taglist, add_funcs[i].gst_tag,
            add_funcs[i].gst_tag_bis, node);
      }
      g_node_destroy (node);
    } else {
      i++;
    }
  }

  /* parsed nodes have been removed, pass along remainder as blob */
  g_node_children_foreach (ilst, G_TRAVERSE_ALL,
      (GNodeForeachFunc) qtdemux_tag_add_blob, &demuxtaglist);

#ifndef GSTREAMER_LITE
  /* parse up XMP_ node if existing */
  xmp_ = qtdemux_tree_get_child_by_type (udta, FOURCC_XMP_);
  if (xmp_ != NULL) {
    GstBuffer *buf;
    GstTagList *xmptaglist;

    buf = _gst_buffer_new_wrapped (((guint8 *) xmp_->data) + 8,
        QT_UINT32 ((guint8 *) xmp_->data) - 8, NULL);
    xmptaglist = gst_tag_list_from_xmp_buffer (buf);
    gst_buffer_unref (buf);

    qtdemux_handle_xmp_taglist (qtdemux, taglist, xmptaglist);
  } else {
    GST_DEBUG_OBJECT (qtdemux, "No XMP_ node found");
  }
#endif // GSTREAMER_LITE
}

void
qtdemux_handle_xmp_taglist (GstQTDemux * qtdemux, GstTagList * taglist,
    GstTagList * xmptaglist)
{
  /* Strip out bogus fields */
  if (xmptaglist) {
    if (gst_tag_list_get_scope (taglist) == GST_TAG_SCOPE_GLOBAL) {
      gst_tag_list_remove_tag (xmptaglist, GST_TAG_VIDEO_CODEC);
      gst_tag_list_remove_tag (xmptaglist, GST_TAG_AUDIO_CODEC);
    } else {
      gst_tag_list_remove_tag (xmptaglist, GST_TAG_CONTAINER_FORMAT);
    }

    GST_DEBUG_OBJECT (qtdemux, "Found XMP tags %" GST_PTR_FORMAT, xmptaglist);

    /* prioritize native tags using _KEEP mode */
    gst_tag_list_insert (taglist, xmptaglist, GST_TAG_MERGE_KEEP);
    gst_tag_list_unref (xmptaglist);
  }
}
