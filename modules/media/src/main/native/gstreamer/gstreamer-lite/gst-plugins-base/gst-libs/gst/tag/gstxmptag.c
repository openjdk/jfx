/* GStreamer
 * Copyright (C) 2010 Stefan Kost <stefan.kost@nokia.com>
 * Copyright (C) 2010 Thiago Santos <thiago.sousa.santos@collabora.co.uk>
 *
 * gstxmptag.c: library for reading / modifying xmp tags
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
 * SECTION:gsttagxmp
 * @short_description: tag mappings and support functions for plugins
 *                     dealing with xmp packets
 * @see_also: #GstTagList
 *
 * Contains various utility functions for plugins to parse or create
 * xmp packets and map them to and from #GstTagList<!-- -->s.
 *
 * Please note that the xmp parser is very lightweight and not strict at all.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include "tag.h"
#include <gst/gsttagsetter.h>
#include "gsttageditingprivate.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <ctype.h>

static const gchar *schema_list[] = {
  "dc",
  "xap",
  "tiff",
  "exif",
  "photoshop",
  "Iptc4xmpCore",
  NULL
};

/**
 * gst_tag_xmp_list_schemas:
 *
 * Gets the list of supported schemas in the xmp lib
 *
 * Returns: a %NULL terminated array of strings with the schema names
 *
 * Since: 0.10.33
 */
const gchar **
gst_tag_xmp_list_schemas (void)
{
  return schema_list;
}

typedef struct _XmpSerializationData XmpSerializationData;
typedef struct _XmpTag XmpTag;

/*
 * Serializes a GValue into a string.
 */
typedef gchar *(*XmpSerializationFunc) (const GValue * value);

/*
 * Deserializes @str that is the gstreamer tag @gst_tag represented in
 * XMP as the @xmp_tag_value and adds the result to the @taglist.
 *
 * @pending_tags is passed so that compound xmp tags can search for its
 * complements on the list and use them. Note that used complements should
 * be freed and removed from the list.
 * The list is of PendingXmpTag
 */
typedef void (*XmpDeserializationFunc) (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag_value,
    const gchar * str, GSList ** pending_tags);

struct _XmpSerializationData
{
  GString *data;
  const gchar **schemas;
};

static gboolean
xmp_serialization_data_use_schema (XmpSerializationData * serdata,
    const gchar * schemaname)
{
  gint i = 0;
  if (serdata->schemas == NULL)
    return TRUE;

  while (serdata->schemas[i] != NULL) {
    if (strcmp (serdata->schemas[i], schemaname) == 0)
      return TRUE;
    i++;
  }
  return FALSE;
}


#define GST_XMP_TAG_TYPE_SIMPLE 0
#define GST_XMP_TAG_TYPE_BAG    1
#define GST_XMP_TAG_TYPE_SEQ    2
struct _XmpTag
{
  const gchar *tag_name;
  gint type;

  XmpSerializationFunc serialize;
  XmpDeserializationFunc deserialize;
};

static GstTagMergeMode
xmp_tag_get_merge_mode (XmpTag * xmptag)
{
  switch (xmptag->type) {
    case GST_XMP_TAG_TYPE_BAG:
    case GST_XMP_TAG_TYPE_SEQ:
      return GST_TAG_MERGE_APPEND;
    case GST_XMP_TAG_TYPE_SIMPLE:
    default:
      return GST_TAG_MERGE_KEEP;
  }
}

static const gchar *
xmp_tag_get_type_name (XmpTag * xmptag)
{
  switch (xmptag->type) {
    case GST_XMP_TAG_TYPE_SEQ:
      return "rdf:Seq";
    default:
      g_assert_not_reached ();
    case GST_XMP_TAG_TYPE_BAG:
      return "rdf:Bag";
  }
}

struct _PendingXmpTag
{
  const gchar *gst_tag;
  XmpTag *xmp_tag;
  gchar *str;
};
typedef struct _PendingXmpTag PendingXmpTag;


/*
 * A schema is a mapping of strings (the tag name in gstreamer) to a list of
 * tags in xmp (XmpTag). We need a list because some tags are split into 2
 * when serialized into xmp.
 * e.g. GST_TAG_GEO_LOCATION_ELEVATION needs to be mapped into 2 complementary
 * tags in the exif's schema. One of them stores the absolute elevation,
 * and the other one stores if it is above of below sea level.
 */
typedef GHashTable GstXmpSchema;
#define gst_xmp_schema_lookup g_hash_table_lookup
#define gst_xmp_schema_insert g_hash_table_insert
static GstXmpSchema *
gst_xmp_schema_new ()
{
  return g_hash_table_new (g_direct_hash, g_direct_equal);
}

/*
 * Mappings from schema names into the schema group of tags (GstXmpSchema)
 */
static GHashTable *__xmp_schemas;

static void
_gst_xmp_add_schema (const gchar * name, GstXmpSchema * schema)
{
  GQuark key;

  key = g_quark_from_string (name);

  if (g_hash_table_lookup (__xmp_schemas, GUINT_TO_POINTER (key))) {
    GST_WARNING ("Schema %s already exists, ignoring", name);
    g_assert_not_reached ();
    return;
  }

  g_hash_table_insert (__xmp_schemas, GUINT_TO_POINTER (key), schema);
}

static void
_gst_xmp_schema_add_mapping (GstXmpSchema * schema, const gchar * gst_tag,
    GPtrArray * array)
{
  GQuark key;

  key = g_quark_from_string (gst_tag);

  if (gst_xmp_schema_lookup (schema, GUINT_TO_POINTER (key))) {
    GST_WARNING ("Tag %s already present for the schema", gst_tag);
    g_assert_not_reached ();
    return;
  }
  gst_xmp_schema_insert (schema, GUINT_TO_POINTER (key), array);
}

static void
_gst_xmp_schema_add_simple_mapping (GstXmpSchema * schema,
    const gchar * gst_tag, const gchar * xmp_tag, gint xmp_type,
    XmpSerializationFunc serialization_func,
    XmpDeserializationFunc deserialization_func)
{
  XmpTag *xmpinfo;
  GPtrArray *array;

  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = xmp_tag;
  xmpinfo->type = xmp_type;
  xmpinfo->serialize = serialization_func;
  xmpinfo->deserialize = deserialization_func;

  array = g_ptr_array_sized_new (1);
  g_ptr_array_add (array, xmpinfo);

  _gst_xmp_schema_add_mapping (schema, gst_tag, array);
}

/*
 * We do not return a copy here because elements are
 * appended, and the API is not public, so we shouldn't
 * have our lists modified during usage
 */
static GPtrArray *
_xmp_tag_get_mapping (const gchar * gst_tag, XmpSerializationData * serdata)
{
  GPtrArray *ret = NULL;
  GHashTableIter iter;
  GQuark key = g_quark_from_string (gst_tag);
  gpointer iterkey, value;
  const gchar *schemaname;

  g_hash_table_iter_init (&iter, __xmp_schemas);
  while (!ret && g_hash_table_iter_next (&iter, &iterkey, &value)) {
    GstXmpSchema *schema = (GstXmpSchema *) value;

    schemaname = g_quark_to_string (GPOINTER_TO_UINT (iterkey));
    if (xmp_serialization_data_use_schema (serdata, schemaname))
      ret =
          (GPtrArray *) gst_xmp_schema_lookup (schema, GUINT_TO_POINTER (key));
  }
  return ret;
}

/* finds the gst tag that maps to this xmp tag in this schema */
static const gchar *
_gst_xmp_schema_get_mapping_reverse (GstXmpSchema * schema,
    const gchar * xmp_tag, XmpTag ** _xmp_tag)
{
  GHashTableIter iter;
  gpointer key, value;
  const gchar *ret = NULL;
  gint index;

  /* Iterate over the hashtable */
  g_hash_table_iter_init (&iter, schema);
  while (!ret && g_hash_table_iter_next (&iter, &key, &value)) {
    GPtrArray *array = (GPtrArray *) value;

    /* each mapping might contain complementary tags */
    for (index = 0; index < array->len; index++) {
      XmpTag *xmpinfo = (XmpTag *) g_ptr_array_index (array, index);

      if (strcmp (xmpinfo->tag_name, xmp_tag) == 0) {
        *_xmp_tag = xmpinfo;
        ret = g_quark_to_string (GPOINTER_TO_UINT (key));
        goto out;
      }
    }
  }

out:
  return ret;
}

/* finds the gst tag that maps to this xmp tag (searches on all schemas) */
static const gchar *
_gst_xmp_tag_get_mapping_reverse (const gchar * xmp_tag, XmpTag ** _xmp_tag)
{
  GHashTableIter iter;
  gpointer key, value;
  const gchar *ret = NULL;

  /* Iterate over the hashtable */
  g_hash_table_iter_init (&iter, __xmp_schemas);
  while (!ret && g_hash_table_iter_next (&iter, &key, &value)) {
    ret = _gst_xmp_schema_get_mapping_reverse ((GstXmpSchema *) value, xmp_tag,
        _xmp_tag);
  }
  return ret;
}

/* utility functions/macros */

#define METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR (3.6)
#define KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND (1/3.6)
#define MILES_PER_HOUR_TO_METERS_PER_SECOND (0.44704)
#define KNOTS_TO_METERS_PER_SECOND (0.514444)

static gchar *
double_to_fraction_string (gdouble num)
{
  gint frac_n;
  gint frac_d;

  gst_util_double_to_fraction (num, &frac_n, &frac_d);
  return g_strdup_printf ("%d/%d", frac_n, frac_d);
}

/* (de)serialize functions */
static gchar *
serialize_exif_gps_coordinate (const GValue * value, gchar pos, gchar neg)
{
  gdouble num;
  gchar c;
  gint integer;
  gchar fraction[G_ASCII_DTOSTR_BUF_SIZE];

  g_return_val_if_fail (G_VALUE_TYPE (value) == G_TYPE_DOUBLE, NULL);

  num = g_value_get_double (value);
  if (num < 0) {
    c = neg;
    num *= -1;
  } else {
    c = pos;
  }
  integer = (gint) num;

  g_ascii_dtostr (fraction, sizeof (fraction), (num - integer) * 60);

  /* FIXME review GPSCoordinate serialization spec for the .mm or ,ss
   * decision. Couldn't understand it clearly */
  return g_strdup_printf ("%d,%s%c", integer, fraction, c);
}

static gchar *
serialize_exif_latitude (const GValue * value)
{
  return serialize_exif_gps_coordinate (value, 'N', 'S');
}

static gchar *
serialize_exif_longitude (const GValue * value)
{
  return serialize_exif_gps_coordinate (value, 'E', 'W');
}

static void
deserialize_exif_gps_coordinate (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * str, gchar pos, gchar neg)
{
  gdouble value = 0;
  gint d = 0, m = 0, s = 0;
  gdouble m2 = 0;
  gchar c = 0;
  const gchar *current;

  /* get the degrees */
  if (sscanf (str, "%d", &d) != 1)
    goto error;

  /* find the beginning of the minutes */
  current = strchr (str, ',');
  if (current == NULL)
    goto end;
  current += 1;

  /* check if it uses ,SS or .mm */
  if (strchr (current, ',') != NULL) {
    sscanf (current, "%d,%d%c", &m, &s, &c);
  } else {
    gchar *copy = g_strdup (current);
    gint len = strlen (copy);
    gint i;

    /* check the last letter */
    for (i = len - 1; len >= 0; len--) {
      if (g_ascii_isspace (copy[i]))
        continue;

      if (g_ascii_isalpha (copy[i])) {
        /* found it */
        c = copy[i];
        copy[i] = '\0';
        break;

      } else {
        /* something is wrong */
        g_free (copy);
        goto error;
      }
    }

    /* use a copy so we can change the last letter as E can cause
     * problems here */
    m2 = g_ascii_strtod (copy, NULL);
    g_free (copy);
  }

end:
  /* we can add them all as those that aren't parsed are 0 */
  value = d + (m / 60.0) + (s / (60.0 * 60.0)) + (m2 / 60.0);

  if (c == pos) {
    //NOP
  } else if (c == neg) {
    value *= -1;
  } else {
    goto error;
  }

  gst_tag_list_add (taglist, xmp_tag_get_merge_mode (xmptag), gst_tag, value,
      NULL);
  return;

error:
  GST_WARNING ("Failed to deserialize gps coordinate: %s", str);
}

static void
deserialize_exif_latitude (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  deserialize_exif_gps_coordinate (xmptag, taglist, gst_tag, str, 'N', 'S');
}

static void
deserialize_exif_longitude (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  deserialize_exif_gps_coordinate (xmptag, taglist, gst_tag, str, 'E', 'W');
}

static gchar *
serialize_exif_altitude (const GValue * value)
{
  gdouble num;

  num = g_value_get_double (value);

  if (num < 0)
    num *= -1;

  return double_to_fraction_string (num);
}

static gchar *
serialize_exif_altituderef (const GValue * value)
{
  gdouble num;

  num = g_value_get_double (value);

  /* 0 means above sea level, 1 means below */
  if (num >= 0)
    return g_strdup ("0");
  return g_strdup ("1");
}

static void
deserialize_exif_altitude (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  const gchar *altitude_str = NULL;
  const gchar *altituderef_str = NULL;
  gint frac_n;
  gint frac_d;
  gdouble value;

  GSList *entry;
  PendingXmpTag *ptag = NULL;

  /* find the other missing part */
  if (strcmp (xmp_tag, "exif:GPSAltitude") == 0) {
    altitude_str = str;

    for (entry = *pending_tags; entry; entry = g_slist_next (entry)) {
      ptag = (PendingXmpTag *) entry->data;

      if (strcmp (ptag->xmp_tag->tag_name, "exif:GPSAltitudeRef") == 0) {
        altituderef_str = ptag->str;
        break;
      }
    }

  } else if (strcmp (xmp_tag, "exif:GPSAltitudeRef") == 0) {
    altituderef_str = str;

    for (entry = *pending_tags; entry; entry = g_slist_next (entry)) {
      ptag = (PendingXmpTag *) entry->data;

      if (strcmp (ptag->xmp_tag->tag_name, "exif:GPSAltitude") == 0) {
        altitude_str = ptag->str;
        break;
      }
    }

  } else {
    GST_WARNING ("Unexpected xmp tag %s", xmp_tag);
    return;
  }

  if (!altitude_str) {
    GST_WARNING ("Missing exif:GPSAltitude tag");
    return;
  }
  if (!altituderef_str) {
    GST_WARNING ("Missing exif:GPSAltitudeRef tag");
    return;
  }

  if (sscanf (altitude_str, "%d/%d", &frac_n, &frac_d) != 2) {
    GST_WARNING ("Failed to parse fraction: %s", altitude_str);
    return;
  }

  gst_util_fraction_to_double (frac_n, frac_d, &value);

  if (altituderef_str[0] == '0') {
    /* nop */
  } else if (altituderef_str[0] == '1') {
    value *= -1;
  } else {
    GST_WARNING ("Unexpected exif:AltitudeRef value: %s", altituderef_str);
    return;
  }

  /* add to the taglist */
  gst_tag_list_add (taglist, xmp_tag_get_merge_mode (xmptag),
      GST_TAG_GEO_LOCATION_ELEVATION, value, NULL);

  /* clean up entry */
  g_free (ptag->str);
  g_slice_free (PendingXmpTag, ptag);
  *pending_tags = g_slist_delete_link (*pending_tags, entry);
}

static gchar *
serialize_exif_gps_speed (const GValue * value)
{
  return double_to_fraction_string (g_value_get_double (value) *
      METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR);
}

static gchar *
serialize_exif_gps_speedref (const GValue * value)
{
  /* we always use km/h */
  return g_strdup ("K");
}

static void
deserialize_exif_gps_speed (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  const gchar *speed_str = NULL;
  const gchar *speedref_str = NULL;
  gint frac_n;
  gint frac_d;
  gdouble value;

  GSList *entry;
  PendingXmpTag *ptag = NULL;

  /* find the other missing part */
  if (strcmp (xmp_tag, "exif:GPSSpeed") == 0) {
    speed_str = str;

    for (entry = *pending_tags; entry; entry = g_slist_next (entry)) {
      ptag = (PendingXmpTag *) entry->data;

      if (strcmp (ptag->xmp_tag->tag_name, "exif:GPSSpeedRef") == 0) {
        speedref_str = ptag->str;
        break;
      }
    }

  } else if (strcmp (xmp_tag, "exif:GPSSpeedRef") == 0) {
    speedref_str = str;

    for (entry = *pending_tags; entry; entry = g_slist_next (entry)) {
      ptag = (PendingXmpTag *) entry->data;

      if (strcmp (ptag->xmp_tag->tag_name, "exif:GPSSpeed") == 0) {
        speed_str = ptag->str;
        break;
      }
    }

  } else {
    GST_WARNING ("Unexpected xmp tag %s", xmp_tag);
    return;
  }

  if (!speed_str) {
    GST_WARNING ("Missing exif:GPSSpeed tag");
    return;
  }
  if (!speedref_str) {
    GST_WARNING ("Missing exif:GPSSpeedRef tag");
    return;
  }

  if (sscanf (speed_str, "%d/%d", &frac_n, &frac_d) != 2) {
    GST_WARNING ("Failed to parse fraction: %s", speed_str);
    return;
  }

  gst_util_fraction_to_double (frac_n, frac_d, &value);

  if (speedref_str[0] == 'K') {
    value *= KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND;
  } else if (speedref_str[0] == 'M') {
    value *= MILES_PER_HOUR_TO_METERS_PER_SECOND;
  } else if (speedref_str[0] == 'N') {
    value *= KNOTS_TO_METERS_PER_SECOND;
  } else {
    GST_WARNING ("Unexpected exif:SpeedRef value: %s", speedref_str);
    return;
  }

  /* add to the taglist */
  gst_tag_list_add (taglist, xmp_tag_get_merge_mode (xmptag),
      GST_TAG_GEO_LOCATION_MOVEMENT_SPEED, value, NULL);

  /* clean up entry */
  g_free (ptag->str);
  g_slice_free (PendingXmpTag, ptag);
  *pending_tags = g_slist_delete_link (*pending_tags, entry);
}

static gchar *
serialize_exif_gps_direction (const GValue * value)
{
  return double_to_fraction_string (g_value_get_double (value));
}

static gchar *
serialize_exif_gps_directionref (const GValue * value)
{
  /* T for true geographic direction (M would mean magnetic) */
  return g_strdup ("T");
}

static void
deserialize_exif_gps_direction (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags, const gchar * direction_tag,
    const gchar * directionref_tag)
{
  const gchar *dir_str = NULL;
  const gchar *dirref_str = NULL;
  gint frac_n;
  gint frac_d;
  gdouble value;

  GSList *entry;
  PendingXmpTag *ptag = NULL;

  /* find the other missing part */
  if (strcmp (xmp_tag, direction_tag) == 0) {
    dir_str = str;

    for (entry = *pending_tags; entry; entry = g_slist_next (entry)) {
      ptag = (PendingXmpTag *) entry->data;

      if (strcmp (ptag->xmp_tag->tag_name, directionref_tag) == 0) {
        dirref_str = ptag->str;
        break;
      }
    }

  } else if (strcmp (xmp_tag, directionref_tag) == 0) {
    dirref_str = str;

    for (entry = *pending_tags; entry; entry = g_slist_next (entry)) {
      ptag = (PendingXmpTag *) entry->data;

      if (strcmp (ptag->xmp_tag->tag_name, direction_tag) == 0) {
        dir_str = ptag->str;
        break;
      }
    }

  } else {
    GST_WARNING ("Unexpected xmp tag %s", xmp_tag);
    return;
  }

  if (!dir_str) {
    GST_WARNING ("Missing %s tag", dir_str);
    return;
  }
  if (!dirref_str) {
    GST_WARNING ("Missing %s tag", dirref_str);
    return;
  }

  if (sscanf (dir_str, "%d/%d", &frac_n, &frac_d) != 2) {
    GST_WARNING ("Failed to parse fraction: %s", dir_str);
    return;
  }

  gst_util_fraction_to_double (frac_n, frac_d, &value);

  if (dirref_str[0] == 'T') {
    /* nop */
  } else if (dirref_str[0] == 'M') {
    GST_WARNING ("Magnetic direction tags aren't supported yet");
    return;
  } else {
    GST_WARNING ("Unexpected %s value: %s", directionref_tag, dirref_str);
    return;
  }

  /* add to the taglist */
  gst_tag_list_add (taglist, xmp_tag_get_merge_mode (xmptag), gst_tag, value,
      NULL);

  /* clean up entry */
  g_free (ptag->str);
  g_slice_free (PendingXmpTag, ptag);
  *pending_tags = g_slist_delete_link (*pending_tags, entry);
}

static void
deserialize_exif_gps_track (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  deserialize_exif_gps_direction (xmptag, taglist, gst_tag, xmp_tag, str,
      pending_tags, "exif:GPSTrack", "exif:GPSTrackRef");
}

static void
deserialize_exif_gps_img_direction (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  deserialize_exif_gps_direction (xmptag, taglist, gst_tag, xmp_tag, str,
      pending_tags, "exif:GPSImgDirection", "exif:GPSImgDirectionRef");
}

static void
deserialize_xmp_rating (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  guint value;

  if (sscanf (str, "%u", &value) != 1) {
    GST_WARNING ("Failed to parse xmp:Rating %s", str);
    return;
  }

  if (value < 0 || value > 100) {
    GST_WARNING ("Unsupported Rating tag %u (should be from 0 to 100), "
        "ignoring", value);
    return;
  }

  gst_tag_list_add (taglist, xmp_tag_get_merge_mode (xmptag), gst_tag, value,
      NULL);
}

static gchar *
serialize_tiff_orientation (const GValue * value)
{
  const gchar *str;
  gint num;

  str = g_value_get_string (value);
  if (str == NULL) {
    GST_WARNING ("Failed to get image orientation tag value");
    return NULL;
  }

  num = __exif_tag_image_orientation_to_exif_value (str);
  if (num == -1)
    return NULL;

  return g_strdup_printf ("%d", num);
}

static void
deserialize_tiff_orientation (XmpTag * xmptag, GstTagList * taglist,
    const gchar * gst_tag, const gchar * xmp_tag, const gchar * str,
    GSList ** pending_tags)
{
  guint value;
  const gchar *orientation = NULL;

  if (sscanf (str, "%u", &value) != 1) {
    GST_WARNING ("Failed to parse tiff:Orientation %s", str);
    return;
  }

  if (value < 1 || value > 8) {
    GST_WARNING ("Invalid tiff:Orientation tag %u (should be from 1 to 8), "
        "ignoring", value);
    return;
  }

  orientation = __exif_tag_image_orientation_from_exif_value (value);
  if (orientation == NULL)
    return;
  gst_tag_list_add (taglist, xmp_tag_get_merge_mode (xmptag), gst_tag,
      orientation, NULL);
}


/* look at this page for addtional schemas
 * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/XMP.html
 */
static gpointer
_init_xmp_tag_map (gpointer user_data)
{
  GPtrArray *array;
  XmpTag *xmpinfo;
  GstXmpSchema *schema;

  __xmp_schemas = g_hash_table_new (g_direct_hash, g_direct_equal);

  /* add the maps */
  /* dublic code metadata
   * http://dublincore.org/documents/dces/
   */
  schema = gst_xmp_schema_new ();
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_ARTIST,
      "dc:creator", GST_XMP_TAG_TYPE_SEQ, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_COPYRIGHT,
      "dc:rights", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_DATE, "dc:date",
      GST_XMP_TAG_TYPE_SEQ, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_DESCRIPTION,
      "dc:description", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_KEYWORDS,
      "dc:subject", GST_XMP_TAG_TYPE_BAG, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_TITLE, "dc:title",
      GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  /* FIXME: we probably want GST_TAG_{,AUDIO_,VIDEO_}MIME_TYPE */
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_VIDEO_CODEC,
      "dc:format", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_add_schema ("dc", schema);

  /* xap (xmp) schema */
  schema = gst_xmp_schema_new ();
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_USER_RATING,
      "xmp:Rating", GST_XMP_TAG_TYPE_SIMPLE, NULL, deserialize_xmp_rating);
  _gst_xmp_add_schema ("xap", schema);

  /* tiff */
  schema = gst_xmp_schema_new ();
  _gst_xmp_schema_add_simple_mapping (schema,
      GST_TAG_DEVICE_MANUFACTURER, "tiff:Make", GST_XMP_TAG_TYPE_SIMPLE, NULL,
      NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_DEVICE_MODEL,
      "tiff:Model", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_APPLICATION_NAME,
      "tiff:Software", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_IMAGE_ORIENTATION,
      "tiff:Orientation", GST_XMP_TAG_TYPE_SIMPLE, serialize_tiff_orientation,
      deserialize_tiff_orientation);
  _gst_xmp_add_schema ("tiff", schema);

  /* exif schema */
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_DATE_TIME,
      "exif:DateTimeOriginal", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema,
      GST_TAG_GEO_LOCATION_LATITUDE, "exif:GPSLatitude",
      GST_XMP_TAG_TYPE_SIMPLE, serialize_exif_latitude,
      deserialize_exif_latitude);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_GEO_LOCATION_LONGITUDE,
      "exif:GPSLongitude", GST_XMP_TAG_TYPE_SIMPLE, serialize_exif_longitude,
      deserialize_exif_longitude);
  _gst_xmp_schema_add_simple_mapping (schema,
      GST_TAG_CAPTURING_EXPOSURE_COMPENSATION, "exif:ExposureBiasValue",
      GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);

  /* compound exif tags */
  array = g_ptr_array_sized_new (2);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSAltitude";
  xmpinfo->serialize = serialize_exif_altitude;
  xmpinfo->deserialize = deserialize_exif_altitude;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSAltitudeRef";
  xmpinfo->serialize = serialize_exif_altituderef;
  xmpinfo->deserialize = deserialize_exif_altitude;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  _gst_xmp_schema_add_mapping (schema, GST_TAG_GEO_LOCATION_ELEVATION, array);

  array = g_ptr_array_sized_new (2);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSSpeed";
  xmpinfo->serialize = serialize_exif_gps_speed;
  xmpinfo->deserialize = deserialize_exif_gps_speed;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSSpeedRef";
  xmpinfo->serialize = serialize_exif_gps_speedref;
  xmpinfo->deserialize = deserialize_exif_gps_speed;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  _gst_xmp_schema_add_mapping (schema,
      GST_TAG_GEO_LOCATION_MOVEMENT_SPEED, array);

  array = g_ptr_array_sized_new (2);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSTrack";
  xmpinfo->serialize = serialize_exif_gps_direction;
  xmpinfo->deserialize = deserialize_exif_gps_track;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSTrackRef";
  xmpinfo->serialize = serialize_exif_gps_directionref;
  xmpinfo->deserialize = deserialize_exif_gps_track;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  _gst_xmp_schema_add_mapping (schema,
      GST_TAG_GEO_LOCATION_MOVEMENT_DIRECTION, array);

  array = g_ptr_array_sized_new (2);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSImgDirection";
  xmpinfo->serialize = serialize_exif_gps_direction;
  xmpinfo->deserialize = deserialize_exif_gps_img_direction;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  xmpinfo = g_slice_new (XmpTag);
  xmpinfo->tag_name = "exif:GPSImgDirectionRef";
  xmpinfo->serialize = serialize_exif_gps_directionref;
  xmpinfo->deserialize = deserialize_exif_gps_img_direction;
  xmpinfo->type = GST_XMP_TAG_TYPE_SIMPLE;
  g_ptr_array_add (array, xmpinfo);
  _gst_xmp_schema_add_mapping (schema,
      GST_TAG_GEO_LOCATION_CAPTURE_DIRECTION, array);
  _gst_xmp_add_schema ("exif", schema);

  /* photoshop schema */
  _gst_xmp_schema_add_simple_mapping (schema,
      GST_TAG_GEO_LOCATION_COUNTRY, "photoshop:Country",
      GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_schema_add_simple_mapping (schema, GST_TAG_GEO_LOCATION_CITY,
      "photoshop:City", GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_add_schema ("photoshop", schema);

  /* iptc4xmpcore schema */
  _gst_xmp_schema_add_simple_mapping (schema,
      GST_TAG_GEO_LOCATION_SUBLOCATION, "Iptc4xmpCore:Location",
      GST_XMP_TAG_TYPE_SIMPLE, NULL, NULL);
  _gst_xmp_add_schema ("Iptc4xmpCore", schema);

  return NULL;
}

static void
xmp_tags_initialize ()
{
  static GOnce my_once = G_ONCE_INIT;
  g_once (&my_once, (GThreadFunc) _init_xmp_tag_map, NULL);
}

typedef struct _GstXmpNamespaceMatch GstXmpNamespaceMatch;
struct _GstXmpNamespaceMatch
{
  const gchar *ns_prefix;
  const gchar *ns_uri;
};

static const GstXmpNamespaceMatch ns_match[] = {
  {"dc", "http://purl.org/dc/elements/1.1/"},
  {"exif", "http://ns.adobe.com/exif/1.0/"},
  {"tiff", "http://ns.adobe.com/tiff/1.0/"},
  {"xap", "http://ns.adobe.com/xap/1.0/"},
  {"photoshop", "http://ns.adobe.com/photoshop/1.0/"},
  {"Iptc4xmpCore", "http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/"},
  {NULL, NULL}
};

typedef struct _GstXmpNamespaceMap GstXmpNamespaceMap;
struct _GstXmpNamespaceMap
{
  const gchar *original_ns;
  gchar *gstreamer_ns;
};

/* parsing */

static void
read_one_tag (GstTagList * list, const gchar * tag, XmpTag * xmptag,
    const gchar * v, GSList ** pending_tags)
{
  GType tag_type;
  GstTagMergeMode merge_mode;

  if (xmptag && xmptag->deserialize) {
    xmptag->deserialize (xmptag, list, tag, xmptag->tag_name, v, pending_tags);
    return;
  }

  merge_mode = xmp_tag_get_merge_mode (xmptag);
  tag_type = gst_tag_get_type (tag);

  /* add gstreamer tag depending on type */
  switch (tag_type) {
    case G_TYPE_STRING:{
      gst_tag_list_add (list, merge_mode, tag, v, NULL);
      break;
    }
    case G_TYPE_DOUBLE:{
      gdouble value = 0;
      gint frac_n, frac_d;

      if (sscanf (v, "%d/%d", &frac_n, &frac_d) == 2) {
        gst_util_fraction_to_double (frac_n, frac_d, &value);
        gst_tag_list_add (list, merge_mode, tag, value, NULL);
      } else {
        GST_WARNING ("Failed to parse fraction: %s", v);
      }
      break;
    }
    default:
      if (tag_type == GST_TYPE_DATE_TIME) {
        GstDateTime *datetime = NULL;
        gint year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;
        gint usecs = 0;
        gint gmt_offset_hour = -1, gmt_offset_min = -1, gmt_offset = -1;
        gchar usec_str[16];
        gint ret;
        gint len;

        len = strlen (v);
        if (len == 0) {
          GST_WARNING ("Empty string for datetime parsing");
          return;
        }

        GST_DEBUG ("Parsing %s into a datetime", v);

        ret = sscanf (v, "%04d-%02d-%02dT%02d:%02d:%02d.%15s",
            &year, &month, &day, &hour, &minute, &second, usec_str);
        if (ret < 3) {
          /* FIXME theoretically, xmp can express datetimes with only year
           * or year and month, but gstdatetime doesn't support it */
          GST_WARNING ("Invalid datetime value: %s", v);
        }

        /* parse the usecs */
        if (ret >= 7) {
          gint num_digits = 0;

          /* find the number of digits */
          while (isdigit ((gint) usec_str[num_digits++]) && num_digits < 6);

          if (num_digits > 0) {
            /* fill up to 6 digits with 0 */
            while (num_digits < 6) {
              usec_str[num_digits++] = 0;
            }

            g_assert (num_digits == 6);

            usec_str[num_digits] = '\0';
            usecs = atoi (usec_str);
          }
        }

        /* parse the timezone info */
        if (v[len - 1] == 'Z') {
          GST_LOG ("UTC timezone");

          /* Having a Z at the end means UTC */
          datetime = gst_date_time_new (0, year, month, day, hour, minute,
              second + usecs / 1000000.0);
        } else {
          gchar *plus_pos = NULL;
          gchar *neg_pos = NULL;
          gchar *pos = NULL;

          GST_LOG ("Checking for timezone information");

          /* check if there is timezone info */
          plus_pos = strrchr (v, '+');
          neg_pos = strrchr (v, '-');
          if (plus_pos) {
            pos = plus_pos + 1;
          } else if (neg_pos) {
            pos = neg_pos + 1;
          }

          if (pos) {
            gint ret_tz = sscanf (pos, "%d:%d", &gmt_offset_hour,
                &gmt_offset_min);

            GST_DEBUG ("Parsing timezone: %s", pos);

            if (ret_tz == 2) {
              gmt_offset = gmt_offset_hour * 60 + gmt_offset_min;
              if (neg_pos != NULL && neg_pos + 1 == pos)
                gmt_offset *= -1;

              GST_LOG ("Timezone offset: %f (%d minutes)", gmt_offset / 60.0,
                  gmt_offset);

              /* no way to know if it is DST or not */
              datetime =
                  gst_date_time_new (gmt_offset / 60.0,
                  year, month, day, hour, minute,
                  second + usecs / ((gdouble) G_USEC_PER_SEC));
            } else {
              GST_WARNING ("Failed to parse timezone information");
            }
          } else {
            GST_WARNING ("No timezone signal found");
          }
        }

        if (datetime) {
          gst_tag_list_add (list, merge_mode, tag, datetime, NULL);
          gst_date_time_unref (datetime);
        }

      } else if (tag_type == GST_TYPE_DATE) {
        GDate *date;
        gint d, m, y;

        /* this is ISO 8601 Date and Time Format
         * %F     Equivalent to %Y-%m-%d (the ISO 8601 date format). (C99)
         * %T     The time in 24-hour notation (%H:%M:%S). (SU)
         * e.g. 2009-05-30T18:26:14+03:00 */

        /* FIXME: this would be the proper way, but needs
           #define _XOPEN_SOURCE before #include <time.h>

           date = g_date_new ();
           struct tm tm={0,};
           strptime (dts, "%FT%TZ", &tm);
           g_date_set_time_t (date, mktime(&tm));
         */
        /* FIXME: this cannot parse the date
           date = g_date_new ();
           g_date_set_parse (date, v);
           if (g_date_valid (date)) {
           gst_tag_list_add (list, merge_mode, tag,
           date, NULL);
           } else {
           GST_WARNING ("unparsable date: '%s'", v);
           }
         */
        /* poor mans straw */
        sscanf (v, "%04d-%02d-%02dT", &y, &m, &d);
        date = g_date_new_dmy (d, m, y);
        gst_tag_list_add (list, merge_mode, tag, date, NULL);
        g_date_free (date);
      } else {
        GST_WARNING ("unhandled type for %s from xmp", tag);
      }
      break;
  }
}

/**
 * gst_tag_list_from_xmp_buffer:
 * @buffer: buffer
 *
 * Parse a xmp packet into a taglist.
 *
 * Returns: new taglist or %NULL, free the list when done
 *
 * Since: 0.10.29
 */
GstTagList *
gst_tag_list_from_xmp_buffer (const GstBuffer * buffer)
{
  GstTagList *list = NULL;
  const gchar *xps, *xp1, *xp2, *xpe, *ns, *ne;
  guint len, max_ft_len;
  gboolean in_tag;
  gchar *part, *pp;
  guint i;
  const gchar *last_tag = NULL;
  XmpTag *last_xmp_tag = NULL;
  GSList *pending_tags = NULL;

  GstXmpNamespaceMap ns_map[] = {
    {"dc", NULL},
    {"exif", NULL},
    {"tiff", NULL},
    {"xap", NULL},
    {"photoshop", NULL},
    {"Iptc4xmpCore", NULL},
    {NULL, NULL}
  };

  xmp_tags_initialize ();

  g_return_val_if_fail (GST_IS_BUFFER (buffer), NULL);
  g_return_val_if_fail (GST_BUFFER_SIZE (buffer) > 0, NULL);

  xps = (const gchar *) GST_BUFFER_DATA (buffer);
  len = GST_BUFFER_SIZE (buffer);
  xpe = &xps[len + 1];

  /* check header and footer */
  xp1 = g_strstr_len (xps, len, "<?xpacket begin");
  if (!xp1)
    goto missing_header;
  xp1 = &xp1[strlen ("<?xpacket begin")];
  while (*xp1 != '>' && *xp1 != '<' && xp1 < xpe)
    xp1++;
  if (*xp1 != '>')
    goto missing_header;

  max_ft_len = 1 + strlen ("<?xpacket end=\".\"?>\n");
  if (len < max_ft_len)
    goto missing_footer;

  GST_DEBUG ("checking footer: [%s]", &xps[len - max_ft_len]);
  xp2 = g_strstr_len (&xps[len - max_ft_len], max_ft_len, "<?xpacket ");
  if (!xp2)
    goto missing_footer;

  GST_INFO ("xmp header okay");

  /* skip > and text until first xml-node */
  xp1++;
  while (*xp1 != '<' && xp1 < xpe)
    xp1++;

  /* no tag can be longer that the whole buffer */
  part = g_malloc (xp2 - xp1);
  list = gst_tag_list_new ();

  /* parse data into a list of nodes */
  /* data is between xp1..xp2 */
  in_tag = TRUE;
  ns = ne = xp1;
  pp = part;
  while (ne < xp2) {
    if (in_tag) {
      ne++;
      while (ne < xp2 && *ne != '>' && *ne != '<') {
        if (*ne == '\n' || *ne == '\t' || *ne == ' ') {
          while (ne < xp2 && (*ne == '\n' || *ne == '\t' || *ne == ' '))
            ne++;
          *pp++ = ' ';
        } else {
          *pp++ = *ne++;
        }
      }
      *pp = '\0';
      if (*ne != '>')
        goto broken_xml;
      /* create node */
      /* {XML, ns, ne-ns} */
      if (ns[0] != '/') {
        gchar *as = strchr (part, ' ');
        /* only log start nodes */
        GST_INFO ("xml: %s", part);

        if (as) {
          gchar *ae, *d;

          /* skip ' ' and scan the attributes */
          as++;
          d = ae = as;

          /* split attr=value pairs */
          while (*ae != '\0') {
            if (*ae == '=') {
              /* attr/value delimmiter */
              d = ae;
            } else if (*ae == '"') {
              /* scan values */
              gchar *v;

              ae++;
              while (*ae != '\0' && *ae != '"')
                ae++;

              *d = *ae = '\0';
              v = &d[2];
              GST_INFO ("   : [%s][%s]", as, v);
              if (!strncmp (as, "xmlns:", 6)) {
                i = 0;
                /* we need to rewrite known namespaces to what we use in
                 * tag_matches */
                while (ns_match[i].ns_prefix) {
                  if (!strcmp (ns_match[i].ns_uri, v))
                    break;
                  i++;
                }
                if (ns_match[i].ns_prefix) {
                  if (strcmp (ns_map[i].original_ns, &as[6])) {
                    ns_map[i].gstreamer_ns = g_strdup (&as[6]);
                  }
                }
              } else {
                const gchar *gst_tag;
                XmpTag *xmp_tag = NULL;
                /* FIXME: eventualy rewrite ns
                 * find ':'
                 * check if ns before ':' is in ns_map and ns_map[i].gstreamer_ns!=NULL
                 * do 2 stage filter in tag_matches
                 */
                gst_tag = _gst_xmp_tag_get_mapping_reverse (as, &xmp_tag);
                if (gst_tag) {
                  PendingXmpTag *ptag;

                  ptag = g_slice_new (PendingXmpTag);
                  ptag->gst_tag = gst_tag;
                  ptag->xmp_tag = xmp_tag;
                  ptag->str = g_strdup (v);

                  pending_tags = g_slist_append (pending_tags, ptag);
                }
              }
              /* restore chars overwritten by '\0' */
              *d = '=';
              *ae = '"';
            } else if (*ae == '\0' || *ae == ' ') {
              /* end of attr/value pair */
              as = &ae[1];
            }
            /* to next char if not eos */
            if (*ae != '\0')
              ae++;
          }
        } else {
          /*
             <dc:type><rdf:Bag><rdf:li>Image</rdf:li></rdf:Bag></dc:type>
             <dc:creator><rdf:Seq><rdf:li/></rdf:Seq></dc:creator>
           */
          /* FIXME: eventualy rewrite ns */

          /* skip rdf tags for now */
          if (strncmp (part, "rdf:", 4)) {
            const gchar *parttag;

            parttag = _gst_xmp_tag_get_mapping_reverse (part, &last_xmp_tag);
            if (parttag) {
              last_tag = parttag;
            }
          }
        }
      }
      /* next cycle */
      ne++;
      if (ne < xp2) {
        if (*ne != '<')
          in_tag = FALSE;
        ns = ne;
        pp = part;
      }
    } else {
      while (ne < xp2 && *ne != '<') {
        *pp++ = *ne;
        ne++;
      }
      *pp = '\0';
      /* create node */
      /* {TXT, ns, (ne-ns)-1} */
      if (ns[0] != '\n' && &ns[1] <= ne) {
        /* only log non-newline nodes, we still have to parse them */
        GST_INFO ("txt: %s", part);
        if (last_tag) {
          PendingXmpTag *ptag;

          ptag = g_slice_new (PendingXmpTag);
          ptag->gst_tag = last_tag;
          ptag->xmp_tag = last_xmp_tag;
          ptag->str = g_strdup (part);

          pending_tags = g_slist_append (pending_tags, ptag);
        }
      }
      /* next cycle */
      in_tag = TRUE;
      ns = ne;
      pp = part;
    }
  }

  while (pending_tags) {
    PendingXmpTag *ptag = (PendingXmpTag *) pending_tags->data;

    pending_tags = g_slist_delete_link (pending_tags, pending_tags);

    read_one_tag (list, ptag->gst_tag, ptag->xmp_tag, ptag->str, &pending_tags);

    g_free (ptag->str);
    g_slice_free (PendingXmpTag, ptag);
  }

  GST_INFO ("xmp packet parsed, %d entries",
      gst_structure_n_fields ((GstStructure *) list));

  /* free resources */
  i = 0;
  while (ns_map[i].original_ns) {
    g_free (ns_map[i].gstreamer_ns);
    i++;
  }
  g_free (part);

  return list;

  /* Errors */
missing_header:
  GST_WARNING ("malformed xmp packet header");
  return NULL;
missing_footer:
  GST_WARNING ("malformed xmp packet footer");
  return NULL;
broken_xml:
  GST_WARNING ("malformed xml tag: %s", part);
#ifdef GSTREAMER_LITE
  g_free (part);
#endif // GSTREAMER_LITE
  return NULL;
}


/* formatting */

static void
string_open_tag (GString * string, const char *tag)
{
  g_string_append_c (string, '<');
  g_string_append (string, tag);
  g_string_append_c (string, '>');
}

static void
string_close_tag (GString * string, const char *tag)
{
  g_string_append (string, "</");
  g_string_append (string, tag);
  g_string_append (string, ">\n");
}

static char *
gst_value_serialize_xmp (const GValue * value)
{
  switch (G_VALUE_TYPE (value)) {
    case G_TYPE_STRING:
      return g_markup_escape_text (g_value_get_string (value), -1);
    case G_TYPE_INT:
      return g_strdup_printf ("%d", g_value_get_int (value));
    case G_TYPE_UINT:
      return g_strdup_printf ("%u", g_value_get_uint (value));
    case G_TYPE_DOUBLE:
      return double_to_fraction_string (g_value_get_double (value));
    default:
      break;
  }
  /* put non-switchable types here */
  if (G_VALUE_TYPE (value) == GST_TYPE_DATE) {
    const GDate *date = gst_value_get_date (value);

    return g_strdup_printf ("%04d-%02d-%02d",
        (gint) g_date_get_year (date), (gint) g_date_get_month (date),
        (gint) g_date_get_day (date));
  } else if (G_VALUE_TYPE (value) == GST_TYPE_DATE_TIME) {
    gint year, month, day, hour, min, sec, microsec;
    gfloat gmt_offset = 0;
    gint gmt_offset_hour, gmt_offset_min;
    GstDateTime *datetime = (GstDateTime *) g_value_get_boxed (value);

    year = gst_date_time_get_year (datetime);
    month = gst_date_time_get_month (datetime);
    day = gst_date_time_get_day (datetime);
    hour = gst_date_time_get_hour (datetime);
    min = gst_date_time_get_minute (datetime);
    sec = gst_date_time_get_second (datetime);
    microsec = gst_date_time_get_microsecond (datetime);
    gmt_offset = gst_date_time_get_time_zone_offset (datetime);
    if (gmt_offset == 0) {
      /* UTC */
      return g_strdup_printf ("%04d-%02d-%02dT%02d:%02d:%02d.%06dZ",
          year, month, day, hour, min, sec, microsec);
    } else {
      gmt_offset_hour = ABS (gmt_offset);
      gmt_offset_min = (ABS (gmt_offset) - gmt_offset_hour) * 60;

      return g_strdup_printf ("%04d-%02d-%02dT%02d:%02d:%02d.%06d%c%02d:%02d",
          year, month, day, hour, min, sec, microsec,
          gmt_offset >= 0 ? '+' : '-', gmt_offset_hour, gmt_offset_min);
    }
  } else {
    return NULL;
  }
}

static void
write_one_tag (const GstTagList * list, const gchar * tag, gpointer user_data)
{
  guint i = 0, ct = gst_tag_list_get_tag_size (list, tag), tag_index;
  XmpSerializationData *serialization_data = user_data;
  GString *data = serialization_data->data;
  GPtrArray *xmp_tag_array = NULL;
  char *s;

  /* map gst-tag to xmp tag */
  xmp_tag_array = _xmp_tag_get_mapping (tag, serialization_data);

  if (!xmp_tag_array) {
    GST_WARNING ("no mapping for %s to xmp", tag);
    return;
  }

  for (tag_index = 0; tag_index < xmp_tag_array->len; tag_index++) {
    XmpTag *xmp_tag;

    xmp_tag = g_ptr_array_index (xmp_tag_array, tag_index);
    string_open_tag (data, xmp_tag->tag_name);

    /* fast path for single valued tag */
    if (ct == 1 || xmp_tag->type == GST_XMP_TAG_TYPE_SIMPLE) {
      if (xmp_tag->serialize) {
        s = xmp_tag->serialize (gst_tag_list_get_value_index (list, tag, 0));
      } else {
        s = gst_value_serialize_xmp (gst_tag_list_get_value_index (list, tag,
                0));
      }
      if (s) {
        g_string_append (data, s);
        g_free (s);
      } else {
        GST_WARNING ("unhandled type for %s to xmp", tag);
      }
    } else {
      const gchar *typename;

      typename = xmp_tag_get_type_name (xmp_tag);

      string_open_tag (data, typename);
      for (i = 0; i < ct; i++) {
        GST_DEBUG ("mapping %s[%u/%u] to xmp", tag, i, ct);
        if (xmp_tag->serialize) {
          s = xmp_tag->serialize (gst_tag_list_get_value_index (list, tag, i));
        } else {
          s = gst_value_serialize_xmp (gst_tag_list_get_value_index (list, tag,
                  i));
        }
        if (s) {
          string_open_tag (data, "rdf:li");
          g_string_append (data, s);
          string_close_tag (data, "rdf:li");
          g_free (s);
        } else {
          GST_WARNING ("unhandled type for %s to xmp", tag);
        }
      }
      string_close_tag (data, typename);
    }

    string_close_tag (data, xmp_tag->tag_name);
  }
}

/**
 * gst_tag_list_to_xmp_buffer_full:
 * @list: tags
 * @read_only: does the container forbid inplace editing
 * @schemas: %NULL terminated array of schemas to be used on serialization
 *
 * Formats a taglist as a xmp packet using only the selected
 * schemas. An empty list (%NULL) means that all schemas should
 * be used
 *
 * Returns: new buffer or %NULL, unref the buffer when done
 *
 * Since: 0.10.33
 */
GstBuffer *
gst_tag_list_to_xmp_buffer_full (const GstTagList * list, gboolean read_only,
    const gchar ** schemas)
{
  GstBuffer *buffer = NULL;
  XmpSerializationData serialization_data;
  GString *data;
  guint i;

  serialization_data.data = g_string_sized_new (4096);
  serialization_data.schemas = schemas;
  data = serialization_data.data;

  xmp_tags_initialize ();

  g_return_val_if_fail (GST_IS_TAG_LIST (list), NULL);

  /* xmp header */
  g_string_append (data,
      "<?xpacket begin=\"\xEF\xBB\xBF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n");
  g_string_append (data,
      "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"GStreamer\">\n");
  g_string_append (data,
      "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
  i = 0;
  while (ns_match[i].ns_prefix) {
    if (xmp_serialization_data_use_schema (&serialization_data,
            ns_match[i].ns_prefix))
      g_string_append_printf (data, " xmlns:%s=\"%s\"",
          ns_match[i].ns_prefix, ns_match[i].ns_uri);
    i++;
  }
  g_string_append (data, ">\n");
  g_string_append (data, "<rdf:Description rdf:about=\"\">\n");

  /* iterate the taglist */
  gst_tag_list_foreach (list, write_one_tag, &serialization_data);

  /* xmp footer */
  g_string_append (data, "</rdf:Description>\n");
  g_string_append (data, "</rdf:RDF>\n");
  g_string_append (data, "</x:xmpmeta>\n");

  if (!read_only) {
    /* the xmp spec recommand to add 2-4KB padding for in-place editable xmp */
    guint i;

    for (i = 0; i < 32; i++) {
      g_string_append (data, "                " "                "
          "                " "                " "\n");
    }
  }
  g_string_append_printf (data, "<?xpacket end=\"%c\"?>\n",
      (read_only ? 'r' : 'w'));

  buffer = gst_buffer_new ();
  GST_BUFFER_SIZE (buffer) = data->len + 1;
  GST_BUFFER_DATA (buffer) = (guint8 *) g_string_free (data, FALSE);
  GST_BUFFER_MALLOCDATA (buffer) = GST_BUFFER_DATA (buffer);

  return buffer;
}

/**
 * gst_tag_list_to_xmp_buffer:
 * @list: tags
 * @read_only: does the container forbid inplace editing
 *
 * Formats a taglist as a xmp packet.
 *
 * Returns: new buffer or %NULL, unref the buffer when done
 *
 * Since: 0.10.29
 */
GstBuffer *
gst_tag_list_to_xmp_buffer (const GstTagList * list, gboolean read_only)
{
  return gst_tag_list_to_xmp_buffer_full (list, read_only, NULL);
}

#undef gst_xmp_schema_lookup
#undef gst_xmp_schema_insert
