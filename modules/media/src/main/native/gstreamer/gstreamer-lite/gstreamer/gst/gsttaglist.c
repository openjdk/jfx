/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 *
 * gsttaglist.c: tag support (aka metadata)
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
 * SECTION:gsttaglist
 * @short_description: List of tags and values used to describe media metadata
 *
 * List of tags and values used to describe media metadata.
 *
 * Strings must be in ASCII or UTF-8 encoding. No other encodings are allowed.
 *
 * Last reviewed on 2009-06-09 (0.10.23)
 */

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include "gst_private.h"
#include "gst-i18n-lib.h"
#include "gsttaglist.h"
#include "gstinfo.h"
#include "gstvalue.h"
#include "gstbuffer.h"
#include "gstquark.h"

#include <gobject/gvaluecollector.h>
#include <string.h>

#define GST_TAG_IS_VALID(tag)           (gst_tag_get_info (tag) != NULL)

/* FIXME 0.11: use GParamSpecs or something similar for tag registrations,
 * possibly even gst_tag_register(). Especially value ranges might be
 * useful for some tags. */

typedef struct
{
  GType type;                   /* type the data is in */

  gchar *nick;                  /* translated name */
  gchar *blurb;                 /* translated description of type */

  GstTagMergeFunc merge_func;   /* functions to merge the values */
  GstTagFlag flag;              /* type of tag */
}
GstTagInfo;

static GMutex *__tag_mutex;

static GHashTable *__tags;

#define TAG_LOCK g_mutex_lock (__tag_mutex)
#define TAG_UNLOCK g_mutex_unlock (__tag_mutex)

GType
gst_tag_list_get_type (void)
{
  static GType _gst_tag_list_type = 0;

  if (G_UNLIKELY (_gst_tag_list_type == 0)) {
    _gst_tag_list_type = g_boxed_type_register_static ("GstTagList",
        (GBoxedCopyFunc) gst_tag_list_copy, (GBoxedFreeFunc) gst_tag_list_free);

#if 0
    g_value_register_transform_func (_gst_tag_list_type, G_TYPE_STRING,
        _gst_structure_transform_to_string);
#endif
  }

  return _gst_tag_list_type;
}

void
_gst_tag_initialize (void)
{
  __tag_mutex = g_mutex_new ();
  __tags = g_hash_table_new (g_direct_hash, g_direct_equal);
  gst_tag_register (GST_TAG_TITLE, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("title"), _("commonly used title"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_TITLE_SORTNAME, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("title sortname"), _("commonly used title for sorting purposes"), NULL);
  gst_tag_register (GST_TAG_ARTIST, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("artist"),
      _("person(s) responsible for the recording"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_ARTIST_SORTNAME, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("artist sortname"),
      _("person(s) responsible for the recording for sorting purposes"), NULL);
  gst_tag_register (GST_TAG_ALBUM, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("album"),
      _("album containing this data"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_ALBUM_SORTNAME, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("album sortname"),
      _("album containing this data for sorting purposes"), NULL);
  gst_tag_register (GST_TAG_ALBUM_ARTIST, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("album artist"),
      _("The artist of the entire album, as it should be displayed"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_ALBUM_ARTIST_SORTNAME, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("album artist sortname"),
      _("The artist of the entire album, as it should be sorted"), NULL);
  gst_tag_register (GST_TAG_DATE, GST_TAG_FLAG_META, GST_TYPE_DATE,
      _("date"), _("date the data was created (as a GDate structure)"), NULL);
  gst_tag_register (GST_TAG_DATE_TIME, GST_TAG_FLAG_META, GST_TYPE_DATE_TIME,
      _("datetime"),
      _("date and time the data was created (as a GstDateTime structure)"),
      NULL);
  gst_tag_register (GST_TAG_GENRE, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("genre"),
      _("genre this data belongs to"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_COMMENT, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("comment"),
      _("free text commenting the data"), gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_EXTENDED_COMMENT, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("extended comment"),
      _("free text commenting the data in key=value or key[en]=comment form"),
      gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_TRACK_NUMBER, GST_TAG_FLAG_META,
      G_TYPE_UINT,
      _("track number"),
      _("track number inside a collection"), gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_TRACK_COUNT, GST_TAG_FLAG_META,
      G_TYPE_UINT,
      _("track count"),
      _("count of tracks inside collection this track belongs to"),
      gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_ALBUM_VOLUME_NUMBER, GST_TAG_FLAG_META,
      G_TYPE_UINT,
      _("disc number"),
      _("disc number inside a collection"), gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_ALBUM_VOLUME_COUNT, GST_TAG_FLAG_META,
      G_TYPE_UINT,
      _("disc count"),
      _("count of discs inside collection this disc belongs to"),
      gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_LOCATION, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("location"), _("Origin of media as a URI (location, where the "
          "original of the file or stream is hosted)"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_HOMEPAGE, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("homepage"),
      _("Homepage for this media (i.e. artist or movie homepage)"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_DESCRIPTION, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("description"), _("short text describing the content of the data"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_VERSION, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("version"), _("version of this data"), NULL);
  gst_tag_register (GST_TAG_ISRC, GST_TAG_FLAG_META, G_TYPE_STRING, _("ISRC"),
      _
      ("International Standard Recording Code - see http://www.ifpi.org/isrc/"),
      NULL);
  /* FIXME: organization (fix what? tpm) */
  gst_tag_register (GST_TAG_ORGANIZATION, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("organization"), _("organization"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_COPYRIGHT, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("copyright"), _("copyright notice of the data"), NULL);
  gst_tag_register (GST_TAG_COPYRIGHT_URI, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("copyright uri"),
      _("URI to the copyright notice of the data"), NULL);
  gst_tag_register (GST_TAG_ENCODED_BY, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("encoded by"), _("name of the encoding person or organization"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_CONTACT, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("contact"), _("contact information"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_LICENSE, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("license"), _("license of data"), NULL);
  gst_tag_register (GST_TAG_LICENSE_URI, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("license uri"),
      _("URI to the license of the data"), NULL);
  gst_tag_register (GST_TAG_PERFORMER, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("performer"),
      _("person(s) performing"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_COMPOSER, GST_TAG_FLAG_META,
      G_TYPE_STRING,
      _("composer"),
      _("person(s) who composed the recording"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_DURATION, GST_TAG_FLAG_DECODED,
      G_TYPE_UINT64,
      _("duration"), _("length in GStreamer time units (nanoseconds)"), NULL);
  gst_tag_register (GST_TAG_CODEC, GST_TAG_FLAG_ENCODED,
      G_TYPE_STRING,
      _("codec"),
      _("codec the data is stored in"), gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_VIDEO_CODEC, GST_TAG_FLAG_ENCODED,
      G_TYPE_STRING,
      _("video codec"), _("codec the video data is stored in"), NULL);
  gst_tag_register (GST_TAG_AUDIO_CODEC, GST_TAG_FLAG_ENCODED,
      G_TYPE_STRING,
      _("audio codec"), _("codec the audio data is stored in"), NULL);
  gst_tag_register (GST_TAG_SUBTITLE_CODEC, GST_TAG_FLAG_ENCODED,
      G_TYPE_STRING,
      _("subtitle codec"), _("codec the subtitle data is stored in"), NULL);
  gst_tag_register (GST_TAG_CONTAINER_FORMAT, GST_TAG_FLAG_ENCODED,
      G_TYPE_STRING, _("container format"),
      _("container format the data is stored in"), NULL);
  gst_tag_register (GST_TAG_BITRATE, GST_TAG_FLAG_ENCODED,
      G_TYPE_UINT, _("bitrate"), _("exact or average bitrate in bits/s"), NULL);
  gst_tag_register (GST_TAG_NOMINAL_BITRATE, GST_TAG_FLAG_ENCODED,
      G_TYPE_UINT, _("nominal bitrate"), _("nominal bitrate in bits/s"), NULL);
  gst_tag_register (GST_TAG_MINIMUM_BITRATE, GST_TAG_FLAG_ENCODED,
      G_TYPE_UINT, _("minimum bitrate"), _("minimum bitrate in bits/s"), NULL);
  gst_tag_register (GST_TAG_MAXIMUM_BITRATE, GST_TAG_FLAG_ENCODED,
      G_TYPE_UINT, _("maximum bitrate"), _("maximum bitrate in bits/s"), NULL);
  gst_tag_register (GST_TAG_ENCODER, GST_TAG_FLAG_ENCODED,
      G_TYPE_STRING,
      _("encoder"), _("encoder used to encode this stream"), NULL);
  gst_tag_register (GST_TAG_ENCODER_VERSION, GST_TAG_FLAG_ENCODED,
      G_TYPE_UINT,
      _("encoder version"),
      _("version of the encoder used to encode this stream"), NULL);
  gst_tag_register (GST_TAG_SERIAL, GST_TAG_FLAG_ENCODED,
      G_TYPE_UINT, _("serial"), _("serial number of track"), NULL);
  gst_tag_register (GST_TAG_TRACK_GAIN, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("replaygain track gain"), _("track gain in db"), NULL);
  gst_tag_register (GST_TAG_TRACK_PEAK, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("replaygain track peak"), _("peak of the track"), NULL);
  gst_tag_register (GST_TAG_ALBUM_GAIN, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("replaygain album gain"), _("album gain in db"), NULL);
  gst_tag_register (GST_TAG_ALBUM_PEAK, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("replaygain album peak"), _("peak of the album"), NULL);
  gst_tag_register (GST_TAG_REFERENCE_LEVEL, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("replaygain reference level"),
      _("reference level of track and album gain values"), NULL);
  gst_tag_register (GST_TAG_LANGUAGE_CODE, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("language code"),
      _("language code for this stream, conforming to ISO-639-1"), NULL);
  gst_tag_register (GST_TAG_IMAGE, GST_TAG_FLAG_META, GST_TYPE_BUFFER,
      _("image"), _("image related to this stream"), gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_PREVIEW_IMAGE, GST_TAG_FLAG_META, GST_TYPE_BUFFER,
      /* TRANSLATORS: 'preview image' = image that shows a preview of the full image */
      _("preview image"), _("preview image related to this stream"), NULL);
  gst_tag_register (GST_TAG_ATTACHMENT, GST_TAG_FLAG_META, GST_TYPE_BUFFER,
      _("attachment"), _("file attached to this stream"),
      gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_BEATS_PER_MINUTE, GST_TAG_FLAG_META, G_TYPE_DOUBLE,
      _("beats per minute"), _("number of beats per minute in audio"), NULL);
  gst_tag_register (GST_TAG_KEYWORDS, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("keywords"), _("comma separated keywords describing the content"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_GEO_LOCATION_NAME, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("geo location name"), _("human readable descriptive location of where "
          "the media has been recorded or produced"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_LATITUDE, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location latitude"),
      _("geo latitude location of where the media has been recorded or "
          "produced in degrees according to WGS84 (zero at the equator, "
          "negative values for southern latitudes)"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_LONGITUDE, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location longitude"),
      _("geo longitude location of where the media has been recorded or "
          "produced in degrees according to WGS84 (zero at the prime meridian "
          "in Greenwich/UK,  negative values for western longitudes)"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_ELEVATION, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location elevation"),
      _("geo elevation of where the media has been recorded or produced in "
          "meters according to WGS84 (zero is average sea level)"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_COUNTRY, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("geo location country"),
      _("country (english name) where the media has been recorded "
          "or produced"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_CITY, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("geo location city"),
      _("city (english name) where the media has been recorded "
          "or produced"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_SUBLOCATION, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("geo location sublocation"),
      _("a location whithin a city where the media has been produced "
          "or created (e.g. the neighborhood)"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_HORIZONTAL_ERROR, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location horizontal error"),
      _("expected error of the horizontal positioning measures (in meters)"),
      NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_MOVEMENT_SPEED, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location movement speed"),
      _("movement speed of the capturing device while performing the capture "
          "in m/s"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_MOVEMENT_DIRECTION, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location movement direction"),
      _("indicates the movement direction of the device performing the capture"
          " of a media. It is represented as degrees in floating point "
          "representation, 0 means the geographic north, and increases "
          "clockwise"), NULL);
  gst_tag_register (GST_TAG_GEO_LOCATION_CAPTURE_DIRECTION, GST_TAG_FLAG_META,
      G_TYPE_DOUBLE, _("geo location capture direction"),
      _("indicates the direction the device is pointing to when capturing "
          " a media. It is represented as degrees in floating point "
          " representation, 0 means the geographic north, and increases "
          "clockwise"), NULL);
  gst_tag_register (GST_TAG_SHOW_NAME, GST_TAG_FLAG_META, G_TYPE_STRING,
      /* TRANSLATORS: 'show name' = 'TV/radio/podcast show name' here */
      _("show name"),
      _("Name of the tv/podcast/series show the media is from"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_SHOW_SORTNAME, GST_TAG_FLAG_META, G_TYPE_STRING,
      /* TRANSLATORS: 'show sortname' = 'TV/radio/podcast show name as used for sorting purposes' here */
      _("show sortname"),
      _("Name of the tv/podcast/series show the media is from, for sorting "
          "purposes"), NULL);
  gst_tag_register (GST_TAG_SHOW_EPISODE_NUMBER, GST_TAG_FLAG_META, G_TYPE_UINT,
      _("episode number"),
      _("The episode number in the season the media is part of"),
      gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_SHOW_SEASON_NUMBER, GST_TAG_FLAG_META, G_TYPE_UINT,
      _("season number"),
      _("The season number of the show the media is part of"),
      gst_tag_merge_use_first);
  gst_tag_register (GST_TAG_LYRICS, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("lyrics"), _("The lyrics of the media, commonly used for songs"),
      gst_tag_merge_strings_with_comma);
  gst_tag_register (GST_TAG_COMPOSER_SORTNAME, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("composer sortname"),
      _("person(s) who composed the recording, for sorting purposes"), NULL);
  gst_tag_register (GST_TAG_GROUPING, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("grouping"),
      _("Groups related media that spans multiple tracks, like the different "
          "pieces of a concerto. It is a higher level than a track, "
          "but lower than an album"), NULL);
  gst_tag_register (GST_TAG_USER_RATING, GST_TAG_FLAG_META, G_TYPE_UINT,
      _("user rating"),
      _("Rating attributed by a user. The higher the rank, "
          "the more the user likes this media"), NULL);
  gst_tag_register (GST_TAG_DEVICE_MANUFACTURER, GST_TAG_FLAG_META,
      G_TYPE_STRING, _("device manufacturer"),
      _("Manufacturer of the device used to create this media"), NULL);
  gst_tag_register (GST_TAG_DEVICE_MODEL, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("device model"),
      _("Model of the device used to create this media"), NULL);
  gst_tag_register (GST_TAG_APPLICATION_NAME, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("application name"), _("Application used to create the media"), NULL);
  gst_tag_register (GST_TAG_APPLICATION_DATA, GST_TAG_FLAG_META,
      GST_TYPE_BUFFER, _("application data"),
      _("Arbitrary application data to be serialized into the media"), NULL);
  gst_tag_register (GST_TAG_IMAGE_ORIENTATION, GST_TAG_FLAG_META, G_TYPE_STRING,
      _("image orientation"),
      _("How the image should be rotated or flipped before display"), NULL);
}

/**
 * gst_tag_merge_use_first:
 * @dest: (out caller-allocates): uninitialized GValue to store result in
 * @src: GValue to copy from
 *
 * This is a convenience function for the func argument of gst_tag_register().
 * It creates a copy of the first value from the list.
 */
void
gst_tag_merge_use_first (GValue * dest, const GValue * src)
{
  const GValue *ret = gst_value_list_get_value (src, 0);

  g_value_init (dest, G_VALUE_TYPE (ret));
  g_value_copy (ret, dest);
}

/**
 * gst_tag_merge_strings_with_comma:
 * @dest: (out caller-allocates): uninitialized GValue to store result in
 * @src: GValue to copy from
 *
 * This is a convenience function for the func argument of gst_tag_register().
 * It concatenates all given strings using a comma. The tag must be registered
 * as a G_TYPE_STRING or this function will fail.
 */
void
gst_tag_merge_strings_with_comma (GValue * dest, const GValue * src)
{
  GString *str;
  gint i, count;

  count = gst_value_list_get_size (src);
  str = g_string_new (g_value_get_string (gst_value_list_get_value (src, 0)));
  for (i = 1; i < count; i++) {
    /* separator between two strings */
    g_string_append (str, _(", "));
    g_string_append (str,
        g_value_get_string (gst_value_list_get_value (src, i)));
  }

  g_value_init (dest, G_TYPE_STRING);
  g_value_take_string (dest, str->str);
  g_string_free (str, FALSE);
}

static GstTagInfo *
gst_tag_lookup (GQuark entry)
{
  GstTagInfo *ret;

  TAG_LOCK;
  ret = g_hash_table_lookup (__tags, GUINT_TO_POINTER (entry));
  TAG_UNLOCK;

  return ret;
}

/**
 * gst_tag_register:
 * @name: the name or identifier string
 * @flag: a flag describing the type of tag info
 * @type: the type this data is in
 * @nick: human-readable name
 * @blurb: a human-readable description about this tag
 * @func: function for merging multiple values of this tag, or NULL
 *
 * Registers a new tag type for the use with GStreamer's type system. If a type
 * with that name is already registered, that one is used.
 * The old registration may have used a different type however. So don't rely
 * on your supplied values.
 *
 * Important: if you do not supply a merge function the implication will be
 * that there can only be one single value for this tag in a tag list and
 * any additional values will silenty be discarded when being added (unless
 * #GST_TAG_MERGE_REPLACE, #GST_TAG_MERGE_REPLACE_ALL, or
 * #GST_TAG_MERGE_PREPEND is used as merge mode, in which case the new
 * value will replace the old one in the list).
 *
 * The merge function will be called from gst_tag_list_copy_value() when
 * it is required that one or more values for a tag be condensed into
 * one single value. This may happen from gst_tag_list_get_string(),
 * gst_tag_list_get_int(), gst_tag_list_get_double() etc. What will happen
 * exactly in that case depends on how the tag was registered and if a
 * merge function was supplied and if so which one.
 *
 * Two default merge functions are provided: gst_tag_merge_use_first() and
 * gst_tag_merge_strings_with_comma().
 */
void
gst_tag_register (const gchar * name, GstTagFlag flag, GType type,
    const gchar * nick, const gchar * blurb, GstTagMergeFunc func)
{
  GQuark key;
  GstTagInfo *info;

  g_return_if_fail (name != NULL);
  g_return_if_fail (nick != NULL);
  g_return_if_fail (blurb != NULL);
  g_return_if_fail (type != 0 && type != GST_TYPE_LIST);

  key = g_quark_from_string (name);
  info = gst_tag_lookup (key);

  if (info) {
    g_return_if_fail (info->type == type);
    return;
  }

  info = g_slice_new (GstTagInfo);
  info->flag = flag;
  info->type = type;
  info->nick = g_strdup (nick);
  info->blurb = g_strdup (blurb);
  info->merge_func = func;

  TAG_LOCK;
  g_hash_table_insert (__tags, GUINT_TO_POINTER (key), info);
  TAG_UNLOCK;
}

/**
 * gst_tag_exists:
 * @tag: name of the tag
 *
 * Checks if the given type is already registered.
 *
 * Returns: TRUE if the type is already registered
 */
gboolean
gst_tag_exists (const gchar * tag)
{
  g_return_val_if_fail (tag != NULL, FALSE);

  return gst_tag_lookup (g_quark_from_string (tag)) != NULL;
}

/**
 * gst_tag_get_type:
 * @tag: the tag
 *
 * Gets the #GType used for this tag.
 *
 * Returns: the #GType of this tag
 */
GType
gst_tag_get_type (const gchar * tag)
{
  GstTagInfo *info;

  g_return_val_if_fail (tag != NULL, 0);
  info = gst_tag_lookup (g_quark_from_string (tag));
  g_return_val_if_fail (info != NULL, 0);

  return info->type;
}

/**
 * gst_tag_get_nick
 * @tag: the tag
 *
 * Returns the human-readable name of this tag, You must not change or free
 * this string.
 *
 * Returns: the human-readable name of this tag
 */
const gchar *
gst_tag_get_nick (const gchar * tag)
{
  GstTagInfo *info;

  g_return_val_if_fail (tag != NULL, NULL);
  info = gst_tag_lookup (g_quark_from_string (tag));
  g_return_val_if_fail (info != NULL, NULL);

  return info->nick;
}

/**
 * gst_tag_get_description:
 * @tag: the tag
 *
 * Returns the human-readable description of this tag, You must not change or
 * free this string.
 *
 * Returns: the human-readable description of this tag
 */
const gchar *
gst_tag_get_description (const gchar * tag)
{
  GstTagInfo *info;

  g_return_val_if_fail (tag != NULL, NULL);
  info = gst_tag_lookup (g_quark_from_string (tag));
  g_return_val_if_fail (info != NULL, NULL);

  return info->blurb;
}

/**
 * gst_tag_get_flag:
 * @tag: the tag
 *
 * Gets the flag of @tag.
 *
 * Returns: the flag of this tag.
 */
GstTagFlag
gst_tag_get_flag (const gchar * tag)
{
  GstTagInfo *info;

  g_return_val_if_fail (tag != NULL, GST_TAG_FLAG_UNDEFINED);
  info = gst_tag_lookup (g_quark_from_string (tag));
  g_return_val_if_fail (info != NULL, GST_TAG_FLAG_UNDEFINED);

  return info->flag;
}

/**
 * gst_tag_is_fixed:
 * @tag: tag to check
 *
 * Checks if the given tag is fixed. A fixed tag can only contain one value.
 * Unfixed tags can contain lists of values.
 *
 * Returns: TRUE, if the given tag is fixed.
 */
gboolean
gst_tag_is_fixed (const gchar * tag)
{
  GstTagInfo *info;

  g_return_val_if_fail (tag != NULL, FALSE);
  info = gst_tag_lookup (g_quark_from_string (tag));
  g_return_val_if_fail (info != NULL, FALSE);

  return info->merge_func == NULL;
}

/**
 * gst_tag_list_new:
 *
 * Creates a new empty GstTagList.
 *
 * Free-function: gst_tag_list_free
 *
 * Returns: (transfer full): An empty tag list
 */
GstTagList *
gst_tag_list_new (void)
{
  return GST_TAG_LIST (gst_structure_id_empty_new (GST_QUARK (TAGLIST)));
}

/**
 * gst_tag_list_new_full:
 * @tag: tag
 * @...: NULL-terminated list of values to set
 *
 * Creates a new taglist and appends the values for the given tags. It expects
 * tag-value pairs like gst_tag_list_add(), and a NULL terminator after the
 * last pair. The type of the values is implicit and is documented in the API
 * reference, but can also be queried at runtime with gst_tag_get_type(). It
 * is an error to pass a value of a type not matching the tag type into this
 * function. The tag list will make copies of any arguments passed
 * (e.g. strings, buffers).
 *
 * Free-function: gst_tag_list_free
 *
 * Returns: (transfer full): a new #GstTagList. Free with gst_tag_list_free()
 *     when no longer needed.
 *
 * Since: 0.10.24
 */
/* FIXME 0.11: rename gst_tag_list_new_full to _new and _new to _new_empty */
GstTagList *
gst_tag_list_new_full (const gchar * tag, ...)
{
  GstTagList *list;
  va_list args;

  g_return_val_if_fail (tag != NULL, NULL);

  list = gst_tag_list_new ();
  va_start (args, tag);
  gst_tag_list_add_valist (list, GST_TAG_MERGE_APPEND, tag, args);
  va_end (args);

  return list;
}

/**
 * gst_tag_list_new_full_valist:
 * @var_args: tag / value pairs to set
 *
 * Just like gst_tag_list_new_full(), only that it takes a va_list argument.
 * Useful mostly for language bindings.
 *
 * Free-function: gst_tag_list_free
 *
 * Returns: (transfer full): a new #GstTagList. Free with gst_tag_list_free()
 *     when no longer needed.
 *
 * Since: 0.10.24
 */
GstTagList *
gst_tag_list_new_full_valist (va_list var_args)
{
  GstTagList *list;
  const gchar *tag;

  list = gst_tag_list_new ();

  tag = va_arg (var_args, gchar *);
  gst_tag_list_add_valist (list, GST_TAG_MERGE_APPEND, tag, var_args);

  return list;
}

/**
 * gst_tag_list_is_empty:
 * @list: A #GstTagList.
 *
 * Checks if the given taglist is empty.
 *
 * Returns: TRUE if the taglist is empty, otherwise FALSE.
 *
 * Since: 0.10.11
 */
gboolean
gst_tag_list_is_empty (const GstTagList * list)
{
  g_return_val_if_fail (list != NULL, FALSE);
  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);

  return (gst_structure_n_fields ((GstStructure *) list) == 0);
}

/**
 * gst_is_tag_list:
 * @p: Object that might be a taglist
 *
 * Checks if the given pointer is a taglist.
 *
 * Returns: TRUE, if the given pointer is a taglist
 */
gboolean
gst_is_tag_list (gconstpointer p)
{
  GstStructure *s = (GstStructure *) p;

  g_return_val_if_fail (p != NULL, FALSE);

  return (GST_IS_STRUCTURE (s) && s->name == GST_QUARK (TAGLIST));
}

typedef struct
{
  GstStructure *list;
  GstTagMergeMode mode;
}
GstTagCopyData;

static void
gst_tag_list_add_value_internal (GstStructure * list, GstTagMergeMode mode,
    GQuark tag, const GValue * value, GstTagInfo * info)
{
  const GValue *value2;

  if (info == NULL) {
    info = gst_tag_lookup (tag);
    if (G_UNLIKELY (info == NULL)) {
      g_warning ("unknown tag '%s'", g_quark_to_string (tag));
      return;
    }
  }

  if (info->merge_func
      && (value2 = gst_structure_id_get_value (list, tag)) != NULL) {
    GValue dest = { 0, };

    switch (mode) {
      case GST_TAG_MERGE_REPLACE_ALL:
      case GST_TAG_MERGE_REPLACE:
        gst_structure_id_set_value (list, tag, value);
        break;
      case GST_TAG_MERGE_PREPEND:
        gst_value_list_merge (&dest, value, value2);
        gst_structure_id_set_value (list, tag, &dest);
        g_value_unset (&dest);
        break;
      case GST_TAG_MERGE_APPEND:
        gst_value_list_merge (&dest, value2, value);
        gst_structure_id_set_value (list, tag, &dest);
        g_value_unset (&dest);
        break;
      case GST_TAG_MERGE_KEEP:
      case GST_TAG_MERGE_KEEP_ALL:
        break;
      default:
        g_assert_not_reached ();
        break;
    }
  } else {
    switch (mode) {
      case GST_TAG_MERGE_APPEND:
      case GST_TAG_MERGE_KEEP:
        if (gst_structure_id_get_value (list, tag) != NULL)
          break;
        /* fall through */
      case GST_TAG_MERGE_REPLACE_ALL:
      case GST_TAG_MERGE_REPLACE:
      case GST_TAG_MERGE_PREPEND:
        gst_structure_id_set_value (list, tag, value);
        break;
      case GST_TAG_MERGE_KEEP_ALL:
        break;
      default:
        g_assert_not_reached ();
        break;
    }
  }
}

static gboolean
gst_tag_list_copy_foreach (GQuark tag, const GValue * value, gpointer user_data)
{
  GstTagCopyData *copy = (GstTagCopyData *) user_data;

  gst_tag_list_add_value_internal (copy->list, copy->mode, tag, value, NULL);

  return TRUE;
}

/**
 * gst_tag_list_insert:
 * @into: list to merge into
 * @from: list to merge from
 * @mode: the mode to use
 *
 * Inserts the tags of the @from list into the first list using the given mode.
 */
void
gst_tag_list_insert (GstTagList * into, const GstTagList * from,
    GstTagMergeMode mode)
{
  GstTagCopyData data;

  g_return_if_fail (GST_IS_TAG_LIST (into));
  g_return_if_fail (GST_IS_TAG_LIST (from));
  g_return_if_fail (GST_TAG_MODE_IS_VALID (mode));

  data.list = (GstStructure *) into;
  data.mode = mode;
  if (mode == GST_TAG_MERGE_REPLACE_ALL) {
    gst_structure_remove_all_fields (data.list);
  }
  gst_structure_foreach ((GstStructure *) from, gst_tag_list_copy_foreach,
      &data);
}

/**
 * gst_tag_list_copy:
 * @list: list to copy
 *
 * Copies a given #GstTagList.
 *
 * Free-function: gst_tag_list_free
 *
 * Returns: (transfer full): copy of the given list
 */
GstTagList *
gst_tag_list_copy (const GstTagList * list)
{
  g_return_val_if_fail (GST_IS_TAG_LIST (list), NULL);

  return GST_TAG_LIST (gst_structure_copy ((GstStructure *) list));
}

/**
 * gst_tag_list_merge:
 * @list1: first list to merge
 * @list2: second list to merge
 * @mode: the mode to use
 *
 * Merges the two given lists into a new list. If one of the lists is NULL, a
 * copy of the other is returned. If both lists are NULL, NULL is returned.
 *
 * Free-function: gst_tag_list_free
 *
 * Returns: (transfer full): the new list
 */
GstTagList *
gst_tag_list_merge (const GstTagList * list1, const GstTagList * list2,
    GstTagMergeMode mode)
{
  GstTagList *list1_cp;
  const GstTagList *list2_cp;

  g_return_val_if_fail (list1 == NULL || GST_IS_TAG_LIST (list1), NULL);
  g_return_val_if_fail (list2 == NULL || GST_IS_TAG_LIST (list2), NULL);
  g_return_val_if_fail (GST_TAG_MODE_IS_VALID (mode), NULL);

  /* nothing to merge */
  if (!list1 && !list2) {
    return NULL;
  }

  /* create empty list, we need to do this to correctly handling merge modes */
  list1_cp = (list1) ? gst_tag_list_copy (list1) : gst_tag_list_new ();
  list2_cp = (list2) ? list2 : gst_tag_list_new ();

  gst_tag_list_insert (list1_cp, list2_cp, mode);

  if (!list2)
    gst_tag_list_free ((GstTagList *) list2_cp);

  return list1_cp;
}

/**
 * gst_tag_list_free:
 * @list: (in) (transfer full): the list to free
 *
 * Frees the given list and all associated values.
 */
void
gst_tag_list_free (GstTagList * list)
{
  g_return_if_fail (GST_IS_TAG_LIST (list));
  gst_structure_free ((GstStructure *) list);
}

/**
 * gst_tag_list_get_tag_size:
 * @list: a taglist
 * @tag: the tag to query
 *
 * Checks how many value are stored in this tag list for the given tag.
 *
 * Returns: The number of tags stored
 */
guint
gst_tag_list_get_tag_size (const GstTagList * list, const gchar * tag)
{
  const GValue *value;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), 0);

  value = gst_structure_get_value ((GstStructure *) list, tag);
  if (value == NULL)
    return 0;
  if (G_VALUE_TYPE (value) != GST_TYPE_LIST)
    return 1;

  return gst_value_list_get_size (value);
}

/**
 * gst_tag_list_add:
 * @list: list to set tags in
 * @mode: the mode to use
 * @tag: tag
 * @...: NULL-terminated list of values to set
 *
 * Sets the values for the given tags using the specified mode.
 */
void
gst_tag_list_add (GstTagList * list, GstTagMergeMode mode, const gchar * tag,
    ...)
{
  va_list args;

  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (GST_TAG_MODE_IS_VALID (mode));
  g_return_if_fail (tag != NULL);

  va_start (args, tag);
  gst_tag_list_add_valist (list, mode, tag, args);
  va_end (args);
}

/**
 * gst_tag_list_add_values:
 * @list: list to set tags in
 * @mode: the mode to use
 * @tag: tag
 * @...: GValues to set
 *
 * Sets the GValues for the given tags using the specified mode.
 */
void
gst_tag_list_add_values (GstTagList * list, GstTagMergeMode mode,
    const gchar * tag, ...)
{
  va_list args;

  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (GST_TAG_MODE_IS_VALID (mode));
  g_return_if_fail (tag != NULL);

  va_start (args, tag);
  gst_tag_list_add_valist_values (list, mode, tag, args);
  va_end (args);
}

/**
 * gst_tag_list_add_valist:
 * @list: list to set tags in
 * @mode: the mode to use
 * @tag: tag
 * @var_args: tag / value pairs to set
 *
 * Sets the values for the given tags using the specified mode.
 */
void
gst_tag_list_add_valist (GstTagList * list, GstTagMergeMode mode,
    const gchar * tag, va_list var_args)
{
  GstTagInfo *info;
  GQuark quark;
  gchar *error = NULL;

  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (GST_TAG_MODE_IS_VALID (mode));
  g_return_if_fail (tag != NULL);

  if (mode == GST_TAG_MERGE_REPLACE_ALL) {
    gst_structure_remove_all_fields (list);
  }

  while (tag != NULL) {
    GValue value = { 0, };

    quark = g_quark_from_string (tag);
    info = gst_tag_lookup (quark);
    if (G_UNLIKELY (info == NULL)) {
      g_warning ("unknown tag '%s'", tag);
      return;
    }
#if GLIB_CHECK_VERSION(2,23,3)
    G_VALUE_COLLECT_INIT (&value, info->type, var_args, 0, &error);
#else
    g_value_init (&value, info->type);
    G_VALUE_COLLECT (&value, var_args, 0, &error);
#endif
    if (error) {
      g_warning ("%s: %s", G_STRLOC, error);
      g_free (error);
      /* we purposely leak the value here, it might not be
       * in a sane state if an error condition occoured
       */
      return;
    }
    gst_tag_list_add_value_internal (list, mode, quark, &value, info);
    g_value_unset (&value);
    tag = va_arg (var_args, gchar *);
  }
}

/**
 * gst_tag_list_add_valist_values:
 * @list: list to set tags in
 * @mode: the mode to use
 * @tag: tag
 * @var_args: tag / GValue pairs to set
 *
 * Sets the GValues for the given tags using the specified mode.
 */
void
gst_tag_list_add_valist_values (GstTagList * list, GstTagMergeMode mode,
    const gchar * tag, va_list var_args)
{
  GQuark quark;

  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (GST_TAG_MODE_IS_VALID (mode));
  g_return_if_fail (tag != NULL);

  if (mode == GST_TAG_MERGE_REPLACE_ALL) {
    gst_structure_remove_all_fields (list);
  }

  while (tag != NULL) {
    quark = g_quark_from_string (tag);
    g_return_if_fail (gst_tag_lookup (quark) != NULL);
    gst_tag_list_add_value_internal (list, mode, quark, va_arg (var_args,
            GValue *), NULL);
    tag = va_arg (var_args, gchar *);
  }
}

/**
 * gst_tag_list_add_value:
 * @list: list to set tags in
 * @mode: the mode to use
 * @tag: tag
 * @value: GValue for this tag
 *
 * Sets the GValue for a given tag using the specified mode.
 *
 * Since: 0.10.24
 */
void
gst_tag_list_add_value (GstTagList * list, GstTagMergeMode mode,
    const gchar * tag, const GValue * value)
{
  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (GST_TAG_MODE_IS_VALID (mode));
  g_return_if_fail (tag != NULL);

  gst_tag_list_add_value_internal (list, mode, g_quark_from_string (tag),
      value, NULL);
}

/**
 * gst_tag_list_remove_tag:
 * @list: list to remove tag from
 * @tag: tag to remove
 *
 * Removes the given tag from the taglist.
 */
void
gst_tag_list_remove_tag (GstTagList * list, const gchar * tag)
{
  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (tag != NULL);

  gst_structure_remove_field ((GstStructure *) list, tag);
}

typedef struct
{
  GstTagForeachFunc func;
  const GstTagList *tag_list;
  gpointer data;
}
TagForeachData;

static int
structure_foreach_wrapper (GQuark field_id, const GValue * value,
    gpointer user_data)
{
  TagForeachData *data = (TagForeachData *) user_data;

  data->func (data->tag_list, g_quark_to_string (field_id), data->data);
  return TRUE;
}

/**
 * gst_tag_list_foreach:
 * @list: list to iterate over
 * @func: (scope call): function to be called for each tag
 * @user_data: (closure): user specified data
 *
 * Calls the given function for each tag inside the tag list. Note that if there
 * is no tag, the function won't be called at all.
 */
void
gst_tag_list_foreach (const GstTagList * list, GstTagForeachFunc func,
    gpointer user_data)
{
  TagForeachData data;

  g_return_if_fail (GST_IS_TAG_LIST (list));
  g_return_if_fail (func != NULL);

  data.func = func;
  data.tag_list = list;
  data.data = user_data;
  gst_structure_foreach ((GstStructure *) list, structure_foreach_wrapper,
      &data);
}

/**
 * gst_tag_list_get_value_index:
 * @list: a #GstTagList
 * @tag: tag to read out
 * @index: number of entry to read out
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: (transfer none): The GValue for the specified entry or NULL if the
 *          tag wasn't available or the tag doesn't have as many entries
 */
const GValue *
gst_tag_list_get_value_index (const GstTagList * list, const gchar * tag,
    guint index)
{
  const GValue *value;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), NULL);
  g_return_val_if_fail (tag != NULL, NULL);

  value = gst_structure_get_value ((GstStructure *) list, tag);
  if (value == NULL)
    return NULL;

  if (GST_VALUE_HOLDS_LIST (value)) {
    if (index >= gst_value_list_get_size (value))
      return NULL;
    return gst_value_list_get_value (value, index);
  } else {
    if (index > 0)
      return NULL;
    return value;
  }
}

/**
 * gst_tag_list_copy_value:
 * @dest: (out caller-allocates): uninitialized #GValue to copy into
 * @list: list to get the tag from
 * @tag: tag to read out
 *
 * Copies the contents for the given tag into the value,
 * merging multiple values into one if multiple values are associated
 * with the tag.
 * You must g_value_unset() the value after use.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *          given list.
 */
gboolean
gst_tag_list_copy_value (GValue * dest, const GstTagList * list,
    const gchar * tag)
{
  const GValue *src;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (dest != NULL, FALSE);
  g_return_val_if_fail (G_VALUE_TYPE (dest) == 0, FALSE);

  src = gst_structure_get_value ((GstStructure *) list, tag);
  if (!src)
    return FALSE;

  if (G_VALUE_TYPE (src) == GST_TYPE_LIST) {
    GstTagInfo *info = gst_tag_lookup (g_quark_from_string (tag));

    if (!info)
      return FALSE;

    /* must be there or lists aren't allowed */
    g_assert (info->merge_func);
    info->merge_func (dest, src);
  } else {
    g_value_init (dest, G_VALUE_TYPE (src));
    g_value_copy (src, dest);
  }
  return TRUE;
}

/* FIXME 0.11: this whole merge function business is overdesigned, and the
 * _get_foo() API is misleading as well - how many application developers will
 * expect gst_tag_list_get_string (list, GST_TAG_ARTIST, &val) might return a
 * string with multiple comma-separated artists? _get_foo() should just be
 * a convenience wrapper around _get_foo_index (list, tag, 0, &val),
 * supplemented by a special _tag_list_get_string_merged() function if needed
 * (unless someone can actually think of real use cases where the merge
 * function is not 'use first' for non-strings and merge for strings) */

/***** evil macros to get all the gst_tag_list_get_*() functions right *****/

#define TAG_MERGE_FUNCS(name,type,ret)                                  \
gboolean                                                                \
gst_tag_list_get_ ## name (const GstTagList *list, const gchar *tag,    \
                           type *value)                                 \
{                                                                       \
  GValue v = { 0, };                                                    \
                                                                        \
  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);                 \
  g_return_val_if_fail (tag != NULL, FALSE);                            \
  g_return_val_if_fail (value != NULL, FALSE);                          \
                                                                        \
  if (!gst_tag_list_copy_value (&v, list, tag))                         \
      return FALSE;                                                     \
  *value = COPY_FUNC (g_value_get_ ## name (&v));                       \
  g_value_unset (&v);                                                   \
  return ret;                                                           \
}                                                                       \
                                                                        \
gboolean                                                                \
gst_tag_list_get_ ## name ## _index (const GstTagList *list,            \
                                     const gchar *tag,                  \
                                     guint index, type *value)          \
{                                                                       \
  const GValue *v;                                                      \
                                                                        \
  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);                 \
  g_return_val_if_fail (tag != NULL, FALSE);                            \
  g_return_val_if_fail (value != NULL, FALSE);                          \
                                                                        \
  if ((v = gst_tag_list_get_value_index (list, tag, index)) == NULL)    \
      return FALSE;                                                     \
  *value = COPY_FUNC (g_value_get_ ## name (v));                        \
  return ret;                                                           \
}

/* FIXME 0.11: maybe get rid of _get_char*(), _get_uchar*(), _get_long*(),
 * _get_ulong*() and _get_pointer*()? - they are not really useful/common
 * enough to warrant convenience accessor functions */

#define COPY_FUNC /**/
/**
 * gst_tag_list_get_char:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_char_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (char, gchar, TRUE);
/**
 * gst_tag_list_get_uchar:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_uchar_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (uchar, guchar, TRUE);
/**
 * gst_tag_list_get_boolean:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_boolean_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (boolean, gboolean, TRUE);
/**
 * gst_tag_list_get_int:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_int_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (int, gint, TRUE);
/**
 * gst_tag_list_get_uint:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_uint_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (uint, guint, TRUE);
/**
 * gst_tag_list_get_long:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_long_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (long, glong, TRUE);
/**
 * gst_tag_list_get_ulong:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_ulong_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (ulong, gulong, TRUE);
/**
 * gst_tag_list_get_int64:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_int64_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (int64, gint64, TRUE);
/**
 * gst_tag_list_get_uint64:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_uint64_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (uint64, guint64, TRUE);
/**
 * gst_tag_list_get_float:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_float_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (float, gfloat, TRUE);
/**
 * gst_tag_list_get_double:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_double_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (double, gdouble, TRUE);
/**
 * gst_tag_list_get_pointer:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out) (transfer none): location for the result
 *
 * Copies the contents for the given tag into the value, merging multiple values
 * into one if multiple values are associated with the tag.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_pointer_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out) (transfer none): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (pointer, gpointer, (*value != NULL));

static inline gchar *
_gst_strdup0 (const gchar * s)
{
  if (s == NULL || *s == '\0')
    return NULL;

  return g_strdup (s);
}

#undef COPY_FUNC
#define COPY_FUNC _gst_strdup0

/**
 * gst_tag_list_get_string:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out callee-allocates) (transfer full): location for the result
 *
 * Copies the contents for the given tag into the value, possibly merging
 * multiple values into one if multiple values are associated with the tag.
 *
 * Use gst_tag_list_get_string_index (list, tag, 0, value) if you want
 * to retrieve the first string associated with this tag unmodified.
 *
 * The resulting string in @value will be in UTF-8 encoding and should be
 * freed by the caller using g_free when no longer needed. Since 0.10.24 the
 * returned string is also guaranteed to be non-NULL and non-empty.
 *
 * Free-function: g_free
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
/**
 * gst_tag_list_get_string_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out callee-allocates) (transfer full): location for the result
 *
 * Gets the value that is at the given index for the given tag in the given
 * list.
 *
 * The resulting string in @value will be in UTF-8 encoding and should be
 * freed by the caller using g_free when no longer needed. Since 0.10.24 the
 * returned string is also guaranteed to be non-NULL and non-empty.
 *
 * Free-function: g_free
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list.
 */
TAG_MERGE_FUNCS (string, gchar *, (*value != NULL));

/*
 *FIXME 0.11: Instead of _peek (non-copy) and _get (copy), we could have
 *            _get (non-copy) and _dup (copy) for strings, seems more
 *            widely used
 */
/**
 * gst_tag_list_peek_string_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out) (transfer none): location for the result
 *
 * Peeks at the value that is at the given index for the given tag in the given
 * list.
 *
 * The resulting string in @value will be in UTF-8 encoding and doesn't need
 * to be freed by the caller. The returned string is also guaranteed to
 * be non-NULL and non-empty.
 *
 * Returns: TRUE, if a value was set, FALSE if the tag didn't exist in the
 *              given list.
 */
gboolean
gst_tag_list_peek_string_index (const GstTagList * list,
    const gchar * tag, guint index, const gchar ** value)
{
  const GValue *v;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if ((v = gst_tag_list_get_value_index (list, tag, index)) == NULL)
    return FALSE;
  *value = g_value_get_string (v);
  return *value != NULL && **value != '\0';
}

/**
 * gst_tag_list_get_date:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out callee-allocates) (transfer full): address of a GDate pointer
 *     variable to store the result into
 *
 * Copies the first date for the given tag in the taglist into the variable
 * pointed to by @value. Free the date with g_date_free() when it is no longer
 * needed.
 *
 * Free-function: g_date_free
 *
 * Returns: TRUE, if a date was copied, FALSE if the tag didn't exist in the
 *              given list or if it was #NULL.
 */
gboolean
gst_tag_list_get_date (const GstTagList * list, const gchar * tag,
    GDate ** value)
{
  GValue v = { 0, };

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if (!gst_tag_list_copy_value (&v, list, tag))
    return FALSE;
  *value = (GDate *) g_value_dup_boxed (&v);
  g_value_unset (&v);
  return (*value != NULL);
}

/**
 * gst_tag_list_get_date_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out callee-allocates) (transfer full): location for the result
 *
 * Gets the date that is at the given index for the given tag in the given
 * list and copies it into the variable pointed to by @value. Free the date
 * with g_date_free() when it is no longer needed.
 *
 * Free-function: g_date_free
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list or if it was #NULL.
 */
gboolean
gst_tag_list_get_date_index (const GstTagList * list,
    const gchar * tag, guint index, GDate ** value)
{
  const GValue *v;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if ((v = gst_tag_list_get_value_index (list, tag, index)) == NULL)
    return FALSE;
  *value = (GDate *) g_value_dup_boxed (v);
  return (*value != NULL);
}

/**
 * gst_tag_list_get_date_time:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out callee-allocates) (transfer full): address of a #GstDateTime
 *     pointer variable to store the result into
 *
 * Copies the first datetime for the given tag in the taglist into the variable
 * pointed to by @value. Unref the date with gst_date_time_unref() when
 * it is no longer needed.
 *
 * Free-function: gst_date_time_unref
 *
 * Returns: TRUE, if a datetime was copied, FALSE if the tag didn't exist in
 *              thegiven list or if it was #NULL.
 *
 * Since: 0.10.31
 */
gboolean
gst_tag_list_get_date_time (const GstTagList * list, const gchar * tag,
    GstDateTime ** value)
{
  GValue v = { 0, };

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if (!gst_tag_list_copy_value (&v, list, tag))
    return FALSE;

  g_return_val_if_fail (GST_VALUE_HOLDS_DATE_TIME (&v), FALSE);

  *value = (GstDateTime *) g_value_dup_boxed (&v);
  g_value_unset (&v);
  return (*value != NULL);
}

/**
 * gst_tag_list_get_date_time_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out callee-allocates) (transfer full): location for the result
 *
 * Gets the datetime that is at the given index for the given tag in the given
 * list and copies it into the variable pointed to by @value. Unref the datetime
 * with gst_date_time_unref() when it is no longer needed.
 *
 * Free-function: gst_date_time_unref
 *
 * Returns: TRUE, if a value was copied, FALSE if the tag didn't exist in the
 *              given list or if it was #NULL.
 *
 * Since: 0.10.31
 */
gboolean
gst_tag_list_get_date_time_index (const GstTagList * list,
    const gchar * tag, guint index, GstDateTime ** value)
{
  const GValue *v;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if ((v = gst_tag_list_get_value_index (list, tag, index)) == NULL)
    return FALSE;
  *value = (GstDateTime *) g_value_dup_boxed (v);
  return (*value != NULL);
}

/**
 * gst_tag_list_get_buffer:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @value: (out callee-allocates) (transfer full): address of a GstBuffer
 *     pointer variable to store the result into
 *
 * Copies the first buffer for the given tag in the taglist into the variable
 * pointed to by @value. Free the buffer with gst_buffer_unref() when it is
 * no longer needed.
 *
 * Free-function: gst_buffer_unref
 *
 * Returns: TRUE, if a buffer was copied, FALSE if the tag didn't exist in the
 *              given list or if it was #NULL.
 *
 * Since: 0.10.23
 */
gboolean
gst_tag_list_get_buffer (const GstTagList * list, const gchar * tag,
    GstBuffer ** value)
{
  GValue v = { 0, };

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if (!gst_tag_list_copy_value (&v, list, tag))
    return FALSE;
  *value = (GstBuffer *) gst_value_dup_mini_object (&v);
  g_value_unset (&v);
  return (*value != NULL);
}

/**
 * gst_tag_list_get_buffer_index:
 * @list: a #GstTagList to get the tag from
 * @tag: tag to read out
 * @index: number of entry to read out
 * @value: (out callee-allocates) (transfer full): address of a GstBuffer
 *     pointer variable to store the result into
 *
 * Gets the buffer that is at the given index for the given tag in the given
 * list and copies it into the variable pointed to by @value. Free the buffer
 * with gst_buffer_unref() when it is no longer needed.
 *
 * Free-function: gst_buffer_unref
 *
 * Returns: TRUE, if a buffer was copied, FALSE if the tag didn't exist in the
 *              given list or if it was #NULL.
 *
 * Since: 0.10.23
 */
gboolean
gst_tag_list_get_buffer_index (const GstTagList * list,
    const gchar * tag, guint index, GstBuffer ** value)
{
  const GValue *v;

  g_return_val_if_fail (GST_IS_TAG_LIST (list), FALSE);
  g_return_val_if_fail (tag != NULL, FALSE);
  g_return_val_if_fail (value != NULL, FALSE);

  if ((v = gst_tag_list_get_value_index (list, tag, index)) == NULL)
    return FALSE;
  *value = (GstBuffer *) gst_value_dup_mini_object (v);
  return (*value != NULL);
}
