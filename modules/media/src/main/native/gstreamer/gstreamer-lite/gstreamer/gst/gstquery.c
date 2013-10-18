/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wim.taymans@chello.be>
 *                    2005 Wim Taymans <wim@fluendo.com>
 *
 * gstquery.c: GstQueryType registration and Query parsing/creation
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
 * SECTION:gstquery
 * @short_description: Dynamically register new query types. Provide functions
 *                     to create queries, and to set and parse values in them.
 * @see_also: #GstPad, #GstElement
 *
 * GstQuery functions are used to register new query types to the gstreamer
 * core and use them.
 * Queries can be performed on pads (gst_pad_query()) and elements
 * (gst_element_query()). Please note that some queries might need a running
 * pipeline to work.
 *
 * Queries can be created using the gst_query_new_*() functions.
 * Query values can be set using gst_query_set_*(), and parsed using
 * gst_query_parse_*() helpers.
 *
 * The following example shows how to query the duration of a pipeline:
 *
 * <example>
 *  <title>Query duration on a pipeline</title>
 *  <programlisting>
 *  GstQuery *query;
 *  gboolean res;
 *  query = gst_query_new_duration (GST_FORMAT_TIME);
 *  res = gst_element_query (pipeline, query);
 *  if (res) {
 *    gint64 duration;
 *    gst_query_parse_duration (query, NULL, &amp;duration);
 *    g_print ("duration = %"GST_TIME_FORMAT, GST_TIME_ARGS (duration));
 *  }
 *  else {
 *    g_print ("duration query failed...");
 *  }
 *  gst_query_unref (query);
 *  </programlisting>
 * </example>
 *
 * Last reviewed on 2006-02-14 (0.10.4)
 */

#include "gst_private.h"
#include "gstinfo.h"
#include "gstquery.h"
#include "gstvalue.h"
#include "gstenumtypes.h"
#include "gstquark.h"
#include "gsturi.h"

GST_DEBUG_CATEGORY_STATIC (gst_query_debug);
#define GST_CAT_DEFAULT gst_query_debug

static void gst_query_finalize (GstQuery * query);
static GstQuery *_gst_query_copy (GstQuery * query);

static GStaticMutex mutex = G_STATIC_MUTEX_INIT;
static GList *_gst_queries = NULL;
static GHashTable *_nick_to_query = NULL;
static GHashTable *_query_type_to_nick = NULL;
static guint32 _n_values = 1;   /* we start from 1 because 0 reserved for NONE */

static GstMiniObjectClass *parent_class = NULL;

static GstQueryTypeDefinition standard_definitions[] = {
  {GST_QUERY_POSITION, "position", "Current position", 0},
  {GST_QUERY_DURATION, "duration", "Total duration", 0},
  {GST_QUERY_LATENCY, "latency", "Latency", 0},
  {GST_QUERY_JITTER, "jitter", "Jitter", 0},
  {GST_QUERY_RATE, "rate", "Configured rate 1000000 = 1", 0},
  {GST_QUERY_SEEKING, "seeking", "Seeking capabilities and parameters", 0},
  {GST_QUERY_SEGMENT, "segment", "currently configured segment", 0},
  {GST_QUERY_CONVERT, "convert", "Converting between formats", 0},
  {GST_QUERY_FORMATS, "formats", "Supported formats for conversion", 0},
  {GST_QUERY_BUFFERING, "buffering", "Buffering status", 0},
  {GST_QUERY_CUSTOM, "custom", "Custom query", 0},
  {GST_QUERY_URI, "uri", "URI of the source or sink", 0},
  {0, NULL, NULL, 0}
};

void
_gst_query_initialize (void)
{
  GstQueryTypeDefinition *standards = standard_definitions;

  GST_CAT_INFO (GST_CAT_GST_INIT, "init queries");

  GST_DEBUG_CATEGORY_INIT (gst_query_debug, "query", 0, "query system");

  g_static_mutex_lock (&mutex);
  if (_nick_to_query == NULL) {
    _nick_to_query = g_hash_table_new (g_str_hash, g_str_equal);
    _query_type_to_nick = g_hash_table_new (NULL, NULL);
  }

  while (standards->nick) {
    standards->quark = g_quark_from_static_string (standards->nick);
    g_hash_table_insert (_nick_to_query, (gpointer) standards->nick, standards);
    g_hash_table_insert (_query_type_to_nick,
        GINT_TO_POINTER (standards->value), standards);

    _gst_queries = g_list_append (_gst_queries, standards);
    standards++;
    _n_values++;
  }
  g_static_mutex_unlock (&mutex);

  g_type_class_ref (gst_query_get_type ());
}

/**
 * gst_query_type_get_name:
 * @query: the query type
 *
 * Get a printable name for the given query type. Do not modify or free.
 *
 * Returns: a reference to the static name of the query.
 */
const gchar *
gst_query_type_get_name (GstQueryType query)
{
  const GstQueryTypeDefinition *def;

  def = gst_query_type_get_details (query);

  return def->nick;
}

/**
 * gst_query_type_to_quark:
 * @query: the query type
 *
 * Get the unique quark for the given query type.
 *
 * Returns: the quark associated with the query type
 */
GQuark
gst_query_type_to_quark (GstQueryType query)
{
  const GstQueryTypeDefinition *def;

  def = gst_query_type_get_details (query);

  return def->quark;
}

G_DEFINE_TYPE (GstQuery, gst_query, GST_TYPE_MINI_OBJECT);

static void
gst_query_class_init (GstQueryClass * klass)
{
  parent_class = g_type_class_peek_parent (klass);

  klass->mini_object_class.copy = (GstMiniObjectCopyFunction) _gst_query_copy;
  klass->mini_object_class.finalize =
      (GstMiniObjectFinalizeFunction) gst_query_finalize;

}

static void
gst_query_init (GstQuery * query)
{
}

static void
gst_query_finalize (GstQuery * query)
{
  g_return_if_fail (query != NULL);

  if (query->structure) {
    gst_structure_set_parent_refcount (query->structure, NULL);
    gst_structure_free (query->structure);
  }

/*   GST_MINI_OBJECT_CLASS (parent_class)->finalize (GST_MINI_OBJECT (query)); */
}

static GstQuery *
_gst_query_copy (GstQuery * query)
{
  GstQuery *copy;

  copy = (GstQuery *) gst_mini_object_new (GST_TYPE_QUERY);

  copy->type = query->type;

  if (query->structure) {
    copy->structure = gst_structure_copy (query->structure);
    gst_structure_set_parent_refcount (copy->structure,
        &query->mini_object.refcount);
  }

  return copy;
}



/**
 * gst_query_type_register:
 * @nick: The nick of the new query
 * @description: The description of the new query
 *
 * Create a new GstQueryType based on the nick or return an
 * already registered query with that nick
 *
 * Returns: A new GstQueryType or an already registered query
 * with the same nick.
 */
GstQueryType
gst_query_type_register (const gchar * nick, const gchar * description)
{
  GstQueryTypeDefinition *query;
  GstQueryType lookup;

  g_return_val_if_fail (nick != NULL, 0);
  g_return_val_if_fail (description != NULL, 0);

  lookup = gst_query_type_get_by_nick (nick);
  if (lookup != GST_QUERY_NONE)
    return lookup;

  query = g_slice_new (GstQueryTypeDefinition);
  query->value = _n_values;
  query->nick = g_strdup (nick);
  query->description = g_strdup (description);
  query->quark = g_quark_from_static_string (query->nick);

  g_static_mutex_lock (&mutex);
  g_hash_table_insert (_nick_to_query, (gpointer) query->nick, query);
  g_hash_table_insert (_query_type_to_nick, GINT_TO_POINTER (query->value),
      query);
  _gst_queries = g_list_append (_gst_queries, query);
  _n_values++;
  g_static_mutex_unlock (&mutex);

  return query->value;
}

/**
 * gst_query_type_get_by_nick:
 * @nick: The nick of the query
 *
 * Get the query type registered with @nick.
 *
 * Returns: The query registered with @nick or #GST_QUERY_NONE
 * if the query was not registered.
 */
GstQueryType
gst_query_type_get_by_nick (const gchar * nick)
{
  GstQueryTypeDefinition *query;

  g_return_val_if_fail (nick != NULL, 0);

  g_static_mutex_lock (&mutex);
  query = g_hash_table_lookup (_nick_to_query, nick);
  g_static_mutex_unlock (&mutex);

  if (query != NULL)
    return query->value;
  else
    return GST_QUERY_NONE;
}

/**
 * gst_query_types_contains:
 * @types: The query array to search
 * @type: the #GstQueryType to find
 *
 * See if the given #GstQueryType is inside the @types query types array.
 *
 * Returns: TRUE if the type is found inside the array
 */
gboolean
gst_query_types_contains (const GstQueryType * types, GstQueryType type)
{
  if (!types)
    return FALSE;

  while (*types) {
    if (*types == type)
      return TRUE;

    types++;
  }
  return FALSE;
}


/**
 * gst_query_type_get_details:
 * @type: a #GstQueryType
 *
 * Get details about the given #GstQueryType.
 *
 * Returns: The #GstQueryTypeDefinition for @type or NULL on failure.
 */
const GstQueryTypeDefinition *
gst_query_type_get_details (GstQueryType type)
{
  const GstQueryTypeDefinition *result;

  g_static_mutex_lock (&mutex);
  result = g_hash_table_lookup (_query_type_to_nick, GINT_TO_POINTER (type));
  g_static_mutex_unlock (&mutex);

  return result;
}

/**
 * gst_query_type_iterate_definitions:
 *
 * Get a #GstIterator of all the registered query types. The definitions
 * iterated over are read only.
 *
 * Free-function: gst_iterator_free
 *
 * Returns: (transfer full): a #GstIterator of #GstQueryTypeDefinition.
 */
GstIterator *
gst_query_type_iterate_definitions (void)
{
  GstIterator *result;

  g_static_mutex_lock (&mutex);
  /* FIXME: register a boxed type for GstQueryTypeDefinition */
  result = gst_iterator_new_list (G_TYPE_POINTER,
      g_static_mutex_get_mutex (&mutex), &_n_values, &_gst_queries,
      NULL, NULL, NULL);
  g_static_mutex_unlock (&mutex);

  return result;
}

static GstQuery *
gst_query_new (GstQueryType type, GstStructure * structure)
{
  GstQuery *query;

  query = (GstQuery *) gst_mini_object_new (GST_TYPE_QUERY);

  GST_DEBUG ("creating new query %p %d", query, type);

  query->type = type;

  if (structure) {
    query->structure = structure;
    gst_structure_set_parent_refcount (query->structure,
        &query->mini_object.refcount);
  } else {
    query->structure = NULL;
  }

  return query;
}

/**
 * gst_query_new_position:
 * @format: the default #GstFormat for the new query
 *
 * Constructs a new query stream position query object. Use gst_query_unref()
 * when done with it. A position query is used to query the current position
 * of playback in the streams, in some format.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 */
GstQuery *
gst_query_new_position (GstFormat format)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_POSITION),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (CURRENT), G_TYPE_INT64, G_GINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_POSITION, structure);

  return query;
}

/**
 * gst_query_set_position:
 * @query: a #GstQuery with query type GST_QUERY_POSITION
 * @format: the requested #GstFormat
 * @cur: the position to set
 *
 * Answer a position query by setting the requested value in the given format.
 */
void
gst_query_set_position (GstQuery * query, GstFormat format, gint64 cur)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_POSITION);

  gst_structure_id_set (query->structure,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (CURRENT), G_TYPE_INT64, cur, NULL);
}

/**
 * gst_query_parse_position:
 * @query: a #GstQuery
 * @format: (out) (allow-none): the storage for the #GstFormat of the
 *     position values (may be NULL)
 * @cur: (out) (allow-none): the storage for the current position (may be NULL)
 *
 * Parse a position query, writing the format into @format, and the position
 * into @cur, if the respective parameters are non-NULL.
 */
void
gst_query_parse_position (GstQuery * query, GstFormat * format, gint64 * cur)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_POSITION);

  structure = query->structure;
  if (format)
    *format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (cur)
    *cur = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (CURRENT)));
}


/**
 * gst_query_new_duration:
 * @format: the #GstFormat for this duration query
 *
 * Constructs a new stream duration query object to query in the given format.
 * Use gst_query_unref() when done with it. A duration query will give the
 * total length of the stream.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 */
GstQuery *
gst_query_new_duration (GstFormat format)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_DURATION),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (DURATION), G_TYPE_INT64, G_GINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_DURATION, structure);

  return query;
}

/**
 * gst_query_set_duration:
 * @query: a #GstQuery
 * @format: the #GstFormat for the duration
 * @duration: the duration of the stream
 *
 * Answer a duration query by setting the requested value in the given format.
 */
void
gst_query_set_duration (GstQuery * query, GstFormat format, gint64 duration)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_DURATION);

  gst_structure_id_set (query->structure,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (DURATION), G_TYPE_INT64, duration, NULL);
}

/**
 * gst_query_parse_duration:
 * @query: a #GstQuery
 * @format: (out) (allow-none): the storage for the #GstFormat of the duration
 *     value, or NULL.
 * @duration: (out) (allow-none): the storage for the total duration, or NULL.
 *
 * Parse a duration query answer. Write the format of the duration into @format,
 * and the value into @duration, if the respective variables are non-NULL.
 */
void
gst_query_parse_duration (GstQuery * query, GstFormat * format,
    gint64 * duration)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_DURATION);

  structure = query->structure;
  if (format)
    *format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (duration)
    *duration = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (DURATION)));
}

/**
 * gst_query_new_latency:
 *
 * Constructs a new latency query object.
 * Use gst_query_unref() when done with it. A latency query is usually performed
 * by sinks to compensate for additional latency introduced by elements in the
 * pipeline.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a #GstQuery
 *
 * Since: 0.10.12
 */
GstQuery *
gst_query_new_latency (void)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_LATENCY),
      GST_QUARK (LIVE), G_TYPE_BOOLEAN, FALSE,
      GST_QUARK (MIN_LATENCY), G_TYPE_UINT64, G_GUINT64_CONSTANT (0),
      GST_QUARK (MAX_LATENCY), G_TYPE_UINT64, G_GUINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_LATENCY, structure);

  return query;
}

/**
 * gst_query_set_latency:
 * @query: a #GstQuery
 * @live: if there is a live element upstream
 * @min_latency: the minimal latency of the live element
 * @max_latency: the maximal latency of the live element
 *
 * Answer a latency query by setting the requested values in the given format.
 *
 * Since: 0.10.12
 */
void
gst_query_set_latency (GstQuery * query, gboolean live,
    GstClockTime min_latency, GstClockTime max_latency)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_LATENCY);

  gst_structure_id_set (query->structure,
      GST_QUARK (LIVE), G_TYPE_BOOLEAN, live,
      GST_QUARK (MIN_LATENCY), G_TYPE_UINT64, min_latency,
      GST_QUARK (MAX_LATENCY), G_TYPE_UINT64, max_latency, NULL);
}

/**
 * gst_query_parse_latency:
 * @query: a #GstQuery
 * @live: (out) (allow-none): storage for live or NULL
 * @min_latency: (out) (allow-none): the storage for the min latency or NULL
 * @max_latency: (out) (allow-none): the storage for the max latency or NULL
 *
 * Parse a latency query answer.
 *
 * Since: 0.10.12
 */
void
gst_query_parse_latency (GstQuery * query, gboolean * live,
    GstClockTime * min_latency, GstClockTime * max_latency)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_LATENCY);

  structure = query->structure;
  if (live)
    *live =
        g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (LIVE)));
  if (min_latency)
    *min_latency = g_value_get_uint64 (gst_structure_id_get_value (structure,
            GST_QUARK (MIN_LATENCY)));
  if (max_latency)
    *max_latency = g_value_get_uint64 (gst_structure_id_get_value (structure,
            GST_QUARK (MAX_LATENCY)));
}

/**
 * gst_query_new_convert:
 * @src_format: the source #GstFormat for the new query
 * @value: the value to convert
 * @dest_format: the target #GstFormat
 *
 * Constructs a new convert query object. Use gst_query_unref()
 * when done with it. A convert query is used to ask for a conversion between
 * one format and another.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a #GstQuery
 */
GstQuery *
gst_query_new_convert (GstFormat src_format, gint64 value,
    GstFormat dest_format)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_CONVERT),
      GST_QUARK (SRC_FORMAT), GST_TYPE_FORMAT, src_format,
      GST_QUARK (SRC_VALUE), G_TYPE_INT64, value,
      GST_QUARK (DEST_FORMAT), GST_TYPE_FORMAT, dest_format,
      GST_QUARK (DEST_VALUE), G_TYPE_INT64, G_GINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_CONVERT, structure);

  return query;
}

/**
 * gst_query_set_convert:
 * @query: a #GstQuery
 * @src_format: the source #GstFormat
 * @src_value: the source value
 * @dest_format: the destination #GstFormat
 * @dest_value: the destination value
 *
 * Answer a convert query by setting the requested values.
 */
void
gst_query_set_convert (GstQuery * query, GstFormat src_format, gint64 src_value,
    GstFormat dest_format, gint64 dest_value)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_CONVERT);

  gst_structure_id_set (query->structure,
      GST_QUARK (SRC_FORMAT), GST_TYPE_FORMAT, src_format,
      GST_QUARK (SRC_VALUE), G_TYPE_INT64, src_value,
      GST_QUARK (DEST_FORMAT), GST_TYPE_FORMAT, dest_format,
      GST_QUARK (DEST_VALUE), G_TYPE_INT64, dest_value, NULL);
}

/**
 * gst_query_parse_convert:
 * @query: a #GstQuery
 * @src_format: (out) (allow-none): the storage for the #GstFormat of the
 *     source value, or NULL
 * @src_value: (out) (allow-none): the storage for the source value, or NULL
 * @dest_format: (out) (allow-none): the storage for the #GstFormat of the
 *     destination value, or NULL
 * @dest_value: (out) (allow-none): the storage for the destination value,
 *     or NULL
 *
 * Parse a convert query answer. Any of @src_format, @src_value, @dest_format,
 * and @dest_value may be NULL, in which case that value is omitted.
 */
void
gst_query_parse_convert (GstQuery * query, GstFormat * src_format,
    gint64 * src_value, GstFormat * dest_format, gint64 * dest_value)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_CONVERT);

  structure = query->structure;
  if (src_format)
    *src_format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (SRC_FORMAT)));
  if (src_value)
    *src_value = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (SRC_VALUE)));
  if (dest_format)
    *dest_format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (DEST_FORMAT)));
  if (dest_value)
    *dest_value = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (DEST_VALUE)));
}

/**
 * gst_query_new_segment:
 * @format: the #GstFormat for the new query
 *
 * Constructs a new segment query object. Use gst_query_unref()
 * when done with it. A segment query is used to discover information about the
 * currently configured segment for playback.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 */
GstQuery *
gst_query_new_segment (GstFormat format)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_SEGMENT),
      GST_QUARK (RATE), G_TYPE_DOUBLE, (gdouble) 0.0,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (START_VALUE), G_TYPE_INT64, G_GINT64_CONSTANT (-1),
      GST_QUARK (STOP_VALUE), G_TYPE_INT64, G_GINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_SEGMENT, structure);

  return query;
}

/**
 * gst_query_set_segment:
 * @query: a #GstQuery
 * @rate: the rate of the segment
 * @format: the #GstFormat of the segment values (@start_value and @stop_value)
 * @start_value: the start value
 * @stop_value: the stop value
 *
 * Answer a segment query by setting the requested values. The normal
 * playback segment of a pipeline is 0 to duration at the default rate of
 * 1.0. If a seek was performed on the pipeline to play a different
 * segment, this query will return the range specified in the last seek.
 *
 * @start_value and @stop_value will respectively contain the configured
 * playback range start and stop values expressed in @format.
 * The values are always between 0 and the duration of the media and
 * @start_value <= @stop_value. @rate will contain the playback rate. For
 * negative rates, playback will actually happen from @stop_value to
 * @start_value.
 */
void
gst_query_set_segment (GstQuery * query, gdouble rate, GstFormat format,
    gint64 start_value, gint64 stop_value)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_SEGMENT);

  gst_structure_id_set (query->structure,
      GST_QUARK (RATE), G_TYPE_DOUBLE, rate,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (START_VALUE), G_TYPE_INT64, start_value,
      GST_QUARK (STOP_VALUE), G_TYPE_INT64, stop_value, NULL);
}

/**
 * gst_query_parse_segment:
 * @query: a #GstQuery
 * @rate: (out) (allow-none): the storage for the rate of the segment, or NULL
 * @format: (out) (allow-none): the storage for the #GstFormat of the values,
 *     or NULL
 * @start_value: (out) (allow-none): the storage for the start value, or NULL
 * @stop_value: (out) (allow-none): the storage for the stop value, or NULL
 *
 * Parse a segment query answer. Any of @rate, @format, @start_value, and
 * @stop_value may be NULL, which will cause this value to be omitted.
 *
 * See gst_query_set_segment() for an explanation of the function arguments.
 */
void
gst_query_parse_segment (GstQuery * query, gdouble * rate, GstFormat * format,
    gint64 * start_value, gint64 * stop_value)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_SEGMENT);

  structure = query->structure;
  if (rate)
    *rate = g_value_get_double (gst_structure_id_get_value (structure,
            GST_QUARK (RATE)));
  if (format)
    *format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (start_value)
    *start_value = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (START_VALUE)));
  if (stop_value)
    *stop_value = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (STOP_VALUE)));
}

/**
 * gst_query_new_application:
 * @type: the query type
 * @structure: a structure for the query
 *
 * Constructs a new custom application query object. Use gst_query_unref()
 * when done with it.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 */
GstQuery *
gst_query_new_application (GstQueryType type, GstStructure * structure)
{
  g_return_val_if_fail (gst_query_type_get_details (type) != NULL, NULL);
  g_return_val_if_fail (structure != NULL, NULL);

  return gst_query_new (type, structure);
}

/**
 * gst_query_get_structure:
 * @query: a #GstQuery
 *
 * Get the structure of a query.
 *
 * Returns: (transfer none): the #GstStructure of the query. The structure is
 *     still owned by the query and will therefore be freed when the query
 *     is unreffed.
 */
GstStructure *
gst_query_get_structure (GstQuery * query)
{
  g_return_val_if_fail (GST_IS_QUERY (query), NULL);

  return query->structure;
}

/**
 * gst_query_new_seeking:
 * @format: the default #GstFormat for the new query
 *
 * Constructs a new query object for querying seeking properties of
 * the stream.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 */
GstQuery *
gst_query_new_seeking (GstFormat format)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_SEEKING),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (SEEKABLE), G_TYPE_BOOLEAN, FALSE,
      GST_QUARK (SEGMENT_START), G_TYPE_INT64, G_GINT64_CONSTANT (-1),
      GST_QUARK (SEGMENT_END), G_TYPE_INT64, G_GINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_SEEKING, structure);

  return query;
}

/**
 * gst_query_set_seeking:
 * @query: a #GstQuery
 * @format: the format to set for the @segment_start and @segment_end values
 * @seekable: the seekable flag to set
 * @segment_start: the segment_start to set
 * @segment_end: the segment_end to set
 *
 * Set the seeking query result fields in @query.
 */
void
gst_query_set_seeking (GstQuery * query, GstFormat format,
    gboolean seekable, gint64 segment_start, gint64 segment_end)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_SEEKING);

  gst_structure_id_set (query->structure,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (SEEKABLE), G_TYPE_BOOLEAN, seekable,
      GST_QUARK (SEGMENT_START), G_TYPE_INT64, segment_start,
      GST_QUARK (SEGMENT_END), G_TYPE_INT64, segment_end, NULL);
}

/**
 * gst_query_parse_seeking:
 * @query: a GST_QUERY_SEEKING type query #GstQuery
 * @format: (out) (allow-none): the format to set for the @segment_start
 *     and @segment_end values, or NULL
 * @seekable: (out) (allow-none): the seekable flag to set, or NULL
 * @segment_start: (out) (allow-none): the segment_start to set, or NULL
 * @segment_end: (out) (allow-none): the segment_end to set, or NULL
 *
 * Parse a seeking query, writing the format into @format, and
 * other results into the passed parameters, if the respective parameters
 * are non-NULL
 */
void
gst_query_parse_seeking (GstQuery * query, GstFormat * format,
    gboolean * seekable, gint64 * segment_start, gint64 * segment_end)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_SEEKING);

  structure = query->structure;
  if (format)
    *format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (seekable)
    *seekable = g_value_get_boolean (gst_structure_id_get_value (structure,
            GST_QUARK (SEEKABLE)));
  if (segment_start)
    *segment_start = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (SEGMENT_START)));
  if (segment_end)
    *segment_end = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (SEGMENT_END)));
}

/**
 * gst_query_new_formats:
 *
 * Constructs a new query object for querying formats of
 * the stream.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 *
 * Since: 0.10.4
 */
GstQuery *
gst_query_new_formats (void)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_empty_new (GST_QUARK (QUERY_FORMATS));
  query = gst_query_new (GST_QUERY_FORMATS, structure);

  return query;
}

static void
gst_query_list_add_format (GValue * list, GstFormat format)
{
  GValue item = { 0, };

  g_value_init (&item, GST_TYPE_FORMAT);
  g_value_set_enum (&item, format);
  gst_value_list_append_value (list, &item);
  g_value_unset (&item);
}

/**
 * gst_query_set_formats:
 * @query: a #GstQuery
 * @n_formats: the number of formats to set.
 * @...: A number of @GstFormats equal to @n_formats.
 *
 * Set the formats query result fields in @query. The number of formats passed
 * must be equal to @n_formats.
 */
void
gst_query_set_formats (GstQuery * query, gint n_formats, ...)
{
  va_list ap;
  GValue list = { 0, };
  gint i;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_FORMATS);

  g_value_init (&list, GST_TYPE_LIST);

  va_start (ap, n_formats);
  for (i = 0; i < n_formats; i++) {
    gst_query_list_add_format (&list, va_arg (ap, GstFormat));
  }
  va_end (ap);

  gst_structure_set_value (query->structure, "formats", &list);

  g_value_unset (&list);

}

/**
 * gst_query_set_formatsv:
 * @query: a #GstQuery
 * @n_formats: the number of formats to set.
 * @formats: (in) (array length=n_formats): an array containing @n_formats
 *     @GstFormat values.
 *
 * Set the formats query result fields in @query. The number of formats passed
 * in the @formats array must be equal to @n_formats.
 *
 * Since: 0.10.4
 */
void
gst_query_set_formatsv (GstQuery * query, gint n_formats,
    const GstFormat * formats)
{
  GValue list = { 0, };
  gint i;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_FORMATS);

  g_value_init (&list, GST_TYPE_LIST);
  for (i = 0; i < n_formats; i++) {
    gst_query_list_add_format (&list, formats[i]);
  }
  gst_structure_set_value (query->structure, "formats", &list);

  g_value_unset (&list);
}

/**
 * gst_query_parse_formats_length:
 * @query: a #GstQuery
 * @n_formats: (out): the number of formats in this query.
 *
 * Parse the number of formats in the formats @query.
 *
 * Since: 0.10.4
 */
void
gst_query_parse_formats_length (GstQuery * query, guint * n_formats)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_FORMATS);

  if (n_formats) {
    const GValue *list;

    list = gst_structure_get_value (query->structure, "formats");
    if (list == NULL)
      *n_formats = 0;
    else
      *n_formats = gst_value_list_get_size (list);
  }
}

/**
 * gst_query_parse_formats_nth:
 * @query: a #GstQuery
 * @nth: (out): the nth format to retrieve.
 * @format: (out): a pointer to store the nth format
 *
 * Parse the format query and retrieve the @nth format from it into
 * @format. If the list contains less elements than @nth, @format will be
 * set to GST_FORMAT_UNDEFINED.
 *
 * Since: 0.10.4
 */
void
gst_query_parse_formats_nth (GstQuery * query, guint nth, GstFormat * format)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_FORMATS);

  if (format) {
    const GValue *list;

    list = gst_structure_get_value (query->structure, "formats");
    if (list == NULL) {
      *format = GST_FORMAT_UNDEFINED;
    } else {
      if (nth < gst_value_list_get_size (list)) {
        *format = g_value_get_enum (gst_value_list_get_value (list, nth));
      } else
        *format = GST_FORMAT_UNDEFINED;
    }
  }
}

/**
 * gst_query_new_buffering
 * @format: the default #GstFormat for the new query
 *
 * Constructs a new query object for querying the buffering status of
 * a stream.
 *
 * Free-function: gst_query_new
 *
 * Returns: (transfer full): a new #GstQuery
 *
 * Since: 0.10.20
 */
GstQuery *
gst_query_new_buffering (GstFormat format)
{
  GstQuery *query;
  GstStructure *structure;

  /* by default, we configure the answer as no buffering with a 100% buffering
   * progress */
  structure = gst_structure_id_new (GST_QUARK (QUERY_BUFFERING),
      GST_QUARK (BUSY), G_TYPE_BOOLEAN, FALSE,
      GST_QUARK (BUFFER_PERCENT), G_TYPE_INT, 100,
      GST_QUARK (BUFFERING_MODE), GST_TYPE_BUFFERING_MODE, GST_BUFFERING_STREAM,
      GST_QUARK (AVG_IN_RATE), G_TYPE_INT, -1,
      GST_QUARK (AVG_OUT_RATE), G_TYPE_INT, -1,
      GST_QUARK (BUFFERING_LEFT), G_TYPE_INT64, G_GINT64_CONSTANT (0),
      GST_QUARK (ESTIMATED_TOTAL), G_TYPE_INT64, G_GINT64_CONSTANT (-1),
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (START_VALUE), G_TYPE_INT64, G_GINT64_CONSTANT (-1),
      GST_QUARK (STOP_VALUE), G_TYPE_INT64, G_GINT64_CONSTANT (-1), NULL);

  query = gst_query_new (GST_QUERY_BUFFERING, structure);

  return query;
}

/**
 * gst_query_set_buffering_percent
 * @query: A valid #GstQuery of type GST_QUERY_BUFFERING.
 * @busy: if buffering is busy
 * @percent: a buffering percent
 *
 * Set the percentage of buffered data. This is a value between 0 and 100.
 * The @busy indicator is %TRUE when the buffering is in progress.
 *
 * Since: 0.10.20
 */
void
gst_query_set_buffering_percent (GstQuery * query, gboolean busy, gint percent)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING);
  g_return_if_fail (percent >= 0 && percent <= 100);

  gst_structure_id_set (query->structure,
      GST_QUARK (BUSY), G_TYPE_BOOLEAN, busy,
      GST_QUARK (BUFFER_PERCENT), G_TYPE_INT, percent, NULL);
}

/**
 * gst_query_parse_buffering_percent
 * @query: A valid #GstQuery of type GST_QUERY_BUFFERING.
 * @busy: (out) (allow-none): if buffering is busy, or NULL
 * @percent: (out) (allow-none): a buffering percent, or NULL
 *
 * Get the percentage of buffered data. This is a value between 0 and 100.
 * The @busy indicator is %TRUE when the buffering is in progress.
 *
 * Since: 0.10.20
 */
void
gst_query_parse_buffering_percent (GstQuery * query, gboolean * busy,
    gint * percent)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING);

  if (busy)
    *busy = g_value_get_boolean (gst_structure_id_get_value (query->structure,
            GST_QUARK (BUSY)));
  if (percent)
    *percent = g_value_get_int (gst_structure_id_get_value (query->structure,
            GST_QUARK (BUFFER_PERCENT)));
}

/**
 * gst_query_set_buffering_stats:
 * @query: A valid #GstQuery of type GST_QUERY_BUFFERING.
 * @mode: a buffering mode
 * @avg_in: the average input rate
 * @avg_out: the average output rate
 * @buffering_left: amount of buffering time left
 *
 * Configures the buffering stats values in @query.
 *
 * Since: 0.10.20
 */
void
gst_query_set_buffering_stats (GstQuery * query, GstBufferingMode mode,
    gint avg_in, gint avg_out, gint64 buffering_left)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING);

  gst_structure_id_set (query->structure,
      GST_QUARK (BUFFERING_MODE), GST_TYPE_BUFFERING_MODE, mode,
      GST_QUARK (AVG_IN_RATE), G_TYPE_INT, avg_in,
      GST_QUARK (AVG_OUT_RATE), G_TYPE_INT, avg_out,
      GST_QUARK (BUFFERING_LEFT), G_TYPE_INT64, buffering_left, NULL);
}

/**
 * gst_query_parse_buffering_stats:
 * @query: A valid #GstQuery of type GST_QUERY_BUFFERING.
 * @mode: (out) (allow-none): a buffering mode, or NULL
 * @avg_in: (out) (allow-none): the average input rate, or NULL
 * @avg_out: (out) (allow-none): the average output rat, or NULLe
 * @buffering_left: (out) (allow-none): amount of buffering time left, or NULL
 *
 * Extracts the buffering stats values from @query.
 *
 * Since: 0.10.20
 */
void
gst_query_parse_buffering_stats (GstQuery * query,
    GstBufferingMode * mode, gint * avg_in, gint * avg_out,
    gint64 * buffering_left)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING);

  if (mode)
    *mode = g_value_get_enum (gst_structure_id_get_value (query->structure,
            GST_QUARK (BUFFERING_MODE)));
  if (avg_in)
    *avg_in = g_value_get_int (gst_structure_id_get_value (query->structure,
            GST_QUARK (AVG_IN_RATE)));
  if (avg_out)
    *avg_out = g_value_get_int (gst_structure_id_get_value (query->structure,
            GST_QUARK (AVG_OUT_RATE)));
  if (buffering_left)
    *buffering_left =
        g_value_get_int64 (gst_structure_id_get_value (query->structure,
            GST_QUARK (BUFFERING_LEFT)));
}


/**
 * gst_query_set_buffering_range:
 * @query: a #GstQuery
 * @format: the format to set for the @start and @stop values
 * @start: the start to set
 * @stop: the stop to set
 * @estimated_total: estimated total amount of download time
 *
 * Set the available query result fields in @query.
 *
 * Since: 0.10.20
 */
void
gst_query_set_buffering_range (GstQuery * query, GstFormat format,
    gint64 start, gint64 stop, gint64 estimated_total)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING);

  gst_structure_id_set (query->structure,
      GST_QUARK (FORMAT), GST_TYPE_FORMAT, format,
      GST_QUARK (START_VALUE), G_TYPE_INT64, start,
      GST_QUARK (STOP_VALUE), G_TYPE_INT64, stop,
      GST_QUARK (ESTIMATED_TOTAL), G_TYPE_INT64, estimated_total, NULL);
}

/**
 * gst_query_parse_buffering_range:
 * @query: a GST_QUERY_BUFFERING type query #GstQuery
 * @format: (out) (allow-none): the format to set for the @segment_start
 *     and @segment_end values, or NULL
 * @start: (out) (allow-none): the start to set, or NULL
 * @stop: (out) (allow-none): the stop to set, or NULL
 * @estimated_total: (out) (allow-none): estimated total amount of download
 *     time, or NULL
 *
 * Parse an available query, writing the format into @format, and
 * other results into the passed parameters, if the respective parameters
 * are non-NULL
 *
 * Since: 0.10.20
 */
void
gst_query_parse_buffering_range (GstQuery * query, GstFormat * format,
    gint64 * start, gint64 * stop, gint64 * estimated_total)
{
  GstStructure *structure;

  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING);

  structure = query->structure;
  if (format)
    *format = g_value_get_enum (gst_structure_id_get_value (structure,
            GST_QUARK (FORMAT)));
  if (start)
    *start = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (START_VALUE)));
  if (stop)
    *stop = g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (STOP_VALUE)));
  if (estimated_total)
    *estimated_total =
        g_value_get_int64 (gst_structure_id_get_value (structure,
            GST_QUARK (ESTIMATED_TOTAL)));
}

/**
 * gst_query_add_buffering_range
 * @query: a GST_QUERY_BUFFERING type query #GstQuery
 * @start: start position of the range
 * @stop: stop position of the range
 *
 * Set the buffering-ranges array field in @query. The current last
 * start position of the array should be inferior to @start.
 *
 * Returns: a #gboolean indicating if the range was added or not.
 *
 * Since: 0.10.31
 */
gboolean
gst_query_add_buffering_range (GstQuery * query, gint64 start, gint64 stop)
{
  GValueArray *array;
  GValue *last_array_value;
  const GValue *value;
  GValue range_value = { 0 };

  g_return_val_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING, FALSE);

  if (G_UNLIKELY (start >= stop))
    return FALSE;

  value =
      gst_structure_id_get_value (query->structure,
      GST_QUARK (BUFFERING_RANGES));
  if (value) {
    array = (GValueArray *) g_value_get_boxed (value);
    last_array_value = g_value_array_get_nth (array, array->n_values - 1);
    if (G_UNLIKELY (start <= gst_value_get_int64_range_min (last_array_value)))
      return FALSE;
  } else {
    GValue new_array_val = { 0, };

    array = g_value_array_new (0);

    g_value_init (&new_array_val, G_TYPE_VALUE_ARRAY);
    g_value_take_boxed (&new_array_val, array);

    /* set the value array only once, so we then modify (append to) the
     * existing value array owned by the GstStructure / the field's GValue */
    gst_structure_id_take_value (query->structure, GST_QUARK (BUFFERING_RANGES),
        &new_array_val);
  }

  g_value_init (&range_value, GST_TYPE_INT64_RANGE);
  gst_value_set_int64_range (&range_value, start, stop);
  g_value_array_append (array, &range_value);
  /* skip the g_value_unset(&range_value) here, we know it's not needed */

  return TRUE;
}

/**
 * gst_query_get_n_buffering_ranges
 * @query: a GST_QUERY_BUFFERING type query #GstQuery
 *
 * Retrieve the number of values currently stored in the
 * buffered-ranges array of the query's structure.
 *
 * Returns: the range array size as a #guint.
 *
 * Since: 0.10.31
 */
guint
gst_query_get_n_buffering_ranges (GstQuery * query)
{
  GValueArray *array;
  const GValue *value;
  guint size = 0;

  g_return_val_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING, 0);

  value =
      gst_structure_id_get_value (query->structure,
      GST_QUARK (BUFFERING_RANGES));
  if (value) {
    array = (GValueArray *) g_value_get_boxed (value);
    size = array->n_values;
  }
  return size;
}


/**
 * gst_query_parse_nth_buffering_range
 * @query: a GST_QUERY_BUFFERING type query #GstQuery
 * @index: position in the buffered-ranges array to read
 * @start: (out) (allow-none): the start position to set, or NULL
 * @stop: (out) (allow-none): the stop position to set, or NULL
 *
 * Parse an available query and get the start and stop values stored
 * at the @index of the buffered ranges array.
 *
 * Returns: a #gboolean indicating if the parsing succeeded.
 *
 * Since: 0.10.31
 */
gboolean
gst_query_parse_nth_buffering_range (GstQuery * query, guint index,
    gint64 * start, gint64 * stop)
{
  const GValue *value;
  GValueArray *ranges;
  GValue *range_value;
  gboolean ret = FALSE;

  g_return_val_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_BUFFERING, ret);

  value =
      gst_structure_id_get_value (query->structure,
      GST_QUARK (BUFFERING_RANGES));
  ranges = (GValueArray *) g_value_get_boxed (value);
  range_value = g_value_array_get_nth (ranges, index);
  if (range_value) {
    if (start)
      *start = gst_value_get_int64_range_min (range_value);
    if (stop)
      *stop = gst_value_get_int64_range_max (range_value);
    ret = TRUE;
  }

  return ret;
}


/**
 * gst_query_new_uri:
 *
 * Constructs a new query URI query object. Use gst_query_unref()
 * when done with it. An URI query is used to query the current URI
 * that is used by the source or sink.
 *
 * Free-function: gst_query_unref
 *
 * Returns: (transfer full): a new #GstQuery
 *
 * Since: 0.10.22
 */
GstQuery *
gst_query_new_uri (void)
{
  GstQuery *query;
  GstStructure *structure;

  structure = gst_structure_id_new (GST_QUARK (QUERY_URI),
      GST_QUARK (URI), G_TYPE_STRING, NULL, NULL);

  query = gst_query_new (GST_QUERY_URI, structure);

  return query;
}

/**
 * gst_query_set_uri:
 * @query: a #GstQuery with query type GST_QUERY_URI
 * @uri: the URI to set
 *
 * Answer a URI query by setting the requested URI.
 *
 * Since: 0.10.22
 */
void
gst_query_set_uri (GstQuery * query, const gchar * uri)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_URI);
  g_return_if_fail (gst_uri_is_valid (uri));

  gst_structure_id_set (query->structure, GST_QUARK (URI), G_TYPE_STRING, uri,
      NULL);
}

/**
 * gst_query_parse_uri:
 * @query: a #GstQuery
 * @uri: (out callee-allocates) (allow-none): the storage for the current URI
 *     (may be NULL)
 *
 * Parse an URI query, writing the URI into @uri as a newly
 * allocated string, if the respective parameters are non-NULL.
 * Free the string with g_free() after usage.
 *
 * Since: 0.10.22
 */
void
gst_query_parse_uri (GstQuery * query, gchar ** uri)
{
  g_return_if_fail (GST_QUERY_TYPE (query) == GST_QUERY_URI);

  if (uri)
    *uri = g_value_dup_string (gst_structure_id_get_value (query->structure,
            GST_QUARK (URI)));
}
