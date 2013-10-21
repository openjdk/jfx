/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 *
 * gstid3tag.c: plugin for reading / modifying id3 tags
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
 * SECTION:gsttagid3
 * @short_description: tag mappings and support functions for plugins
 *                     dealing with ID3v1 and ID3v2 tags
 * @see_also: #GstTagList
 * 
 * <refsect2>
 * <para>
 * Contains various utility functions for plugins to parse or create
 * ID3 tags and map ID3v2 identifiers to and from GStreamer identifiers.
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gsttageditingprivate.h"
#include <stdlib.h>
#include <string.h>

static const gchar *genres[] = {
  "Blues",
  "Classic Rock",
  "Country",
  "Dance",
  "Disco",
  "Funk",
  "Grunge",
  "Hip-Hop",
  "Jazz",
  "Metal",
  "New Age",
  "Oldies",
  "Other",
  "Pop",
  "R&B",
  "Rap",
  "Reggae",
  "Rock",
  "Techno",
  "Industrial",
  "Alternative",
  "Ska",
  "Death Metal",
  "Pranks",
  "Soundtrack",
  "Euro-Techno",
  "Ambient",
  "Trip-Hop",
  "Vocal",
  "Jazz+Funk",
  "Fusion",
  "Trance",
  "Classical",
  "Instrumental",
  "Acid",
  "House",
  "Game",
  "Sound Clip",
  "Gospel",
  "Noise",
  "Alternative Rock",
  "Bass",
  "Soul",
  "Punk",
  "Space",
  "Meditative",
  "Instrumental Pop",
  "Instrumental Rock",
  "Ethnic",
  "Gothic",
  "Darkwave",
  "Techno-Industrial",
  "Electronic",
  "Pop-Folk",
  "Eurodance",
  "Dream",
  "Southern Rock",
  "Comedy",
  "Cult",
  "Gangsta",
  "Top 40",
  "Christian Rap",
  "Pop/Funk",
  "Jungle",
  "Native American",
  "Cabaret",
  "New Wave",
  "Psychedelic",
  "Rave",
  "Showtunes",
  "Trailer",
  "Lo-Fi",
  "Tribal",
  "Acid Punk",
  "Acid Jazz",
  "Polka",
  "Retro",
  "Musical",
  "Rock & Roll",
  "Hard Rock",
  "Folk",
  "Folk/Rock",
  "National Folk",
  "Swing",
  "Fusion",
  "Bebob",
  "Latin",
  "Revival",
  "Celtic",
  "Bluegrass",
  "Avantgarde",
  "Gothic Rock",
  "Progressive Rock",
  "Psychedelic Rock",
  "Symphonic Rock",
  "Slow Rock",
  "Big Band",
  "Chorus",
  "Easy Listening",
  "Acoustic",
  "Humour",
  "Speech",
  "Chanson",
  "Opera",
  "Chamber Music",
  "Sonata",
  "Symphony",
  "Booty Bass",
  "Primus",
  "Porn Groove",
  "Satire",
  "Slow Jam",
  "Club",
  "Tango",
  "Samba",
  "Folklore",
  "Ballad",
  "Power Ballad",
  "Rhythmic Soul",
  "Freestyle",
  "Duet",
  "Punk Rock",
  "Drum Solo",
  "A Capella",
  "Euro-House",
  "Dance Hall",
  "Goa",
  "Drum & Bass",
  "Club-House",
  "Hardcore",
  "Terror",
  "Indie",
  "BritPop",
  "Negerpunk",
  "Polsk Punk",
  "Beat",
  "Christian Gangsta Rap",
  "Heavy Metal",
  "Black Metal",
  "Crossover",
  "Contemporary Christian",
  "Christian Rock",
  "Merengue",
  "Salsa",
  "Thrash Metal",
  "Anime",
  "Jpop",
  "Synthpop"
};

static const GstTagEntryMatch tag_matches[] = {
  {GST_TAG_TITLE, "TIT2"},
  {GST_TAG_ALBUM, "TALB"},
  {GST_TAG_TRACK_NUMBER, "TRCK"},
  {GST_TAG_ARTIST, "TPE1"},
  {GST_TAG_ALBUM_ARTIST, "TPE2"},
  {GST_TAG_COMPOSER, "TCOM"},
  {GST_TAG_COPYRIGHT, "TCOP"},
  {GST_TAG_COPYRIGHT_URI, "WCOP"},
  {GST_TAG_ENCODED_BY, "TENC"},
  {GST_TAG_GENRE, "TCON"},
  {GST_TAG_DATE, "TDRC"},
  {GST_TAG_COMMENT, "COMM"},
  {GST_TAG_ALBUM_VOLUME_NUMBER, "TPOS"},
  {GST_TAG_DURATION, "TLEN"},
  {GST_TAG_ISRC, "TSRC"},
  {GST_TAG_IMAGE, "APIC"},
  {GST_TAG_ENCODER, "TSSE"},
  {GST_TAG_BEATS_PER_MINUTE, "TBPM"},
  {GST_TAG_ARTIST_SORTNAME, "TSOP"},
  {GST_TAG_ALBUM_SORTNAME, "TSOA"},
  {GST_TAG_TITLE_SORTNAME, "TSOT"},
  {NULL, NULL}
};

/**
 * gst_tag_from_id3_tag:
 * @id3_tag: ID3v2 tag to convert to GStreamer tag
 *
 * Looks up the GStreamer tag for a ID3v2 tag.
 *
 * Returns: The corresponding GStreamer tag or NULL if none exists.
 */
const gchar *
gst_tag_from_id3_tag (const gchar * id3_tag)
{
  int i = 0;

  g_return_val_if_fail (id3_tag != NULL, NULL);

  while (tag_matches[i].gstreamer_tag != NULL) {
    if (strncmp (id3_tag, tag_matches[i].original_tag, 5) == 0) {
      return tag_matches[i].gstreamer_tag;
    }
    i++;
  }

  GST_INFO ("Cannot map ID3v2 tag '%c%c%c%c' to GStreamer tag",
      id3_tag[0], id3_tag[1], id3_tag[2], id3_tag[3]);

  return NULL;
}

static const GstTagEntryMatch user_tag_matches[] = {
  /* musicbrainz identifiers being used in the real world (foobar2000) */
  {GST_TAG_MUSICBRAINZ_ARTISTID, "TXXX|musicbrainz_artistid"},
  {GST_TAG_MUSICBRAINZ_ALBUMID, "TXXX|musicbrainz_albumid"},
  {GST_TAG_MUSICBRAINZ_ALBUMARTISTID, "TXXX|musicbrainz_albumartistid"},
  {GST_TAG_MUSICBRAINZ_TRMID, "TXXX|musicbrainz_trmid"},
  {GST_TAG_CDDA_MUSICBRAINZ_DISCID, "TXXX|musicbrainz_discid"},
  /* musicbrainz identifiers according to spec no one pays
   * attention to (http://musicbrainz.org/docs/specs/metadata_tags.html) */
  {GST_TAG_MUSICBRAINZ_ARTISTID, "TXXX|MusicBrainz Artist Id"},
  {GST_TAG_MUSICBRAINZ_ALBUMID, "TXXX|MusicBrainz Album Id"},
  {GST_TAG_MUSICBRAINZ_ALBUMARTISTID, "TXXX|MusicBrainz Album Artist Id"},
  {GST_TAG_MUSICBRAINZ_TRMID, "TXXX|MusicBrainz TRM Id"},
  /* according to: http://wiki.musicbrainz.org/MusicBrainzTag (yes, no space
   * before 'ID' and not 'Id' either this time, yay for consistency) */
  {GST_TAG_CDDA_MUSICBRAINZ_DISCID, "TXXX|MusicBrainz DiscID"},
  /* foobar2000 uses these identifiers to store gain/peak information in
   * ID3v2 tags <= v2.3.0. In v2.4.0 there's the RVA2 frame for that */
  {GST_TAG_TRACK_GAIN, "TXXX|replaygain_track_gain"},
  {GST_TAG_TRACK_PEAK, "TXXX|replaygain_track_peak"},
  {GST_TAG_ALBUM_GAIN, "TXXX|replaygain_album_gain"},
  {GST_TAG_ALBUM_PEAK, "TXXX|replaygain_album_peak"},
  /* the following two are more or less made up, there seems to be little
   * evidence that any popular application is actually putting this info
   * into TXXX frames; the first one comes from a musicbrainz wiki 'proposed
   * tags' page, the second one is analogue to the vorbis/ape/flac tag. */
  {GST_TAG_CDDA_CDDB_DISCID, "TXXX|discid"},
  {GST_TAG_CDDA_CDDB_DISCID, "TXXX|CDDB DiscID"}
};

/**
 * gst_tag_from_id3_user_tag:
 * @type: the type of ID3v2 user tag (e.g. "TXXX" or "UDIF")
 * @id3_user_tag: ID3v2 user tag to convert to GStreamer tag
 *
 * Looks up the GStreamer tag for an ID3v2 user tag (e.g. description in
 * TXXX frame or owner in UFID frame).
 *
 * Returns: The corresponding GStreamer tag or NULL if none exists.
 */
const gchar *
gst_tag_from_id3_user_tag (const gchar * type, const gchar * id3_user_tag)
{
  int i = 0;

  g_return_val_if_fail (type != NULL && strlen (type) == 4, NULL);
  g_return_val_if_fail (id3_user_tag != NULL, NULL);

  for (i = 0; i < G_N_ELEMENTS (user_tag_matches); ++i) {
    if (strncmp (type, user_tag_matches[i].original_tag, 4) == 0 &&
        g_ascii_strcasecmp (id3_user_tag,
            user_tag_matches[i].original_tag + 5) == 0) {
      GST_LOG ("Mapped ID3v2 user tag '%s' to GStreamer tag '%s'",
          user_tag_matches[i].original_tag, user_tag_matches[i].gstreamer_tag);
      return user_tag_matches[i].gstreamer_tag;
    }
  }

  GST_INFO ("Cannot map ID3v2 user tag '%s' of type '%s' to GStreamer tag",
      id3_user_tag, type);

  return NULL;
}

/**
 * gst_tag_to_id3_tag:
 * @gst_tag: GStreamer tag to convert to vorbiscomment tag
 *
 * Looks up the ID3v2 tag for a GStreamer tag.
 *
 * Returns: The corresponding ID3v2 tag or NULL if none exists.
 */
const gchar *
gst_tag_to_id3_tag (const gchar * gst_tag)
{
  int i = 0;

  g_return_val_if_fail (gst_tag != NULL, NULL);

  while (tag_matches[i].gstreamer_tag != NULL) {
    if (strcmp (gst_tag, tag_matches[i].gstreamer_tag) == 0) {
      return tag_matches[i].original_tag;
    }
    i++;
  }
  return NULL;
}

static void
gst_tag_extract_id3v1_string (GstTagList * list, const gchar * tag,
    const gchar * start, const guint size)
{
  const gchar *env_vars[] = { "GST_ID3V1_TAG_ENCODING",
    "GST_ID3_TAG_ENCODING", "GST_TAG_ENCODING", NULL
  };
  gchar *utf8;

  utf8 = gst_tag_freeform_string_to_utf8 (start, size, env_vars);

  if (utf8 && *utf8 != '\0') {
    gst_tag_list_add (list, GST_TAG_MERGE_REPLACE, tag, utf8, NULL);
  }

  g_free (utf8);
}

/**
 * gst_tag_list_new_from_id3v1:
 * @data: 128 bytes of data containing the ID3v1 tag
 *
 * Parses the data containing an ID3v1 tag and returns a #GstTagList from the
 * parsed data.
 *
 * Returns: A new tag list or NULL if the data was not an ID3v1 tag.
 */
GstTagList *
gst_tag_list_new_from_id3v1 (const guint8 * data)
{
  guint year;
  gchar *ystr;
  GstTagList *list;

  g_return_val_if_fail (data != NULL, NULL);

  if (data[0] != 'T' || data[1] != 'A' || data[2] != 'G')
    return NULL;
  list = gst_tag_list_new ();
  gst_tag_extract_id3v1_string (list, GST_TAG_TITLE, (gchar *) & data[3], 30);
  gst_tag_extract_id3v1_string (list, GST_TAG_ARTIST, (gchar *) & data[33], 30);
  gst_tag_extract_id3v1_string (list, GST_TAG_ALBUM, (gchar *) & data[63], 30);
  ystr = g_strndup ((gchar *) & data[93], 4);
  year = strtoul (ystr, NULL, 10);
  g_free (ystr);
  if (year > 0) {
    GDate *date = g_date_new_dmy (1, 1, year);

    gst_tag_list_add (list, GST_TAG_MERGE_REPLACE, GST_TAG_DATE, date, NULL);
    g_date_free (date);
  }
  if (data[125] == 0 && data[126] != 0) {
    gst_tag_extract_id3v1_string (list, GST_TAG_COMMENT, (gchar *) & data[97],
        28);
    gst_tag_list_add (list, GST_TAG_MERGE_REPLACE, GST_TAG_TRACK_NUMBER,
        (guint) data[126], NULL);
  } else {
    gst_tag_extract_id3v1_string (list, GST_TAG_COMMENT, (gchar *) & data[97],
        30);
  }
  if (data[127] < gst_tag_id3_genre_count () && !gst_tag_list_is_empty (list)) {
    gst_tag_list_add (list, GST_TAG_MERGE_REPLACE, GST_TAG_GENRE,
        gst_tag_id3_genre_get (data[127]), NULL);
  }

  return list;
}

/**
 * gst_tag_id3_genre_count:
 *
 * Gets the number of ID3v1 genres that can be identified. Winamp genres are 
 * included.
 *
 * Returns: the number of ID3v1 genres that can be identified
 */
guint
gst_tag_id3_genre_count (void)
{
  return G_N_ELEMENTS (genres);
}

/**
 * gst_tag_id3_genre_get:
 * @id: ID of genre to query
 *
 * Gets the ID3v1 genre name for a given ID.
 *
 * Returns: the genre or NULL if no genre is associated with that ID.
 */
const gchar *
gst_tag_id3_genre_get (const guint id)
{
  if (id >= G_N_ELEMENTS (genres))
    return NULL;
  return genres[id];
}

/**
 * gst_tag_list_add_id3_image:
 * @tag_list: a tag list
 * @image_data: the (encoded) image
 * @image_data_len: the length of the encoded image data at @image_data
 * @id3_picture_type: picture type as per the ID3 (v2.4.0) specification for
 *    the APIC frame (0 = unknown/other)
 *
 * Adds an image from an ID3 APIC frame (or similar, such as used in FLAC)
 * to the given tag list. Also see gst_tag_image_data_to_image_buffer() for
 * more information on image tags in GStreamer.
 *
 * Returns: %TRUE if the image was processed, otherwise %FALSE
 *
 * Since: 0.10.20
 */
gboolean
gst_tag_list_add_id3_image (GstTagList * tag_list, const guint8 * image_data,
    guint image_data_len, guint id3_picture_type)
{
  GstTagImageType tag_image_type;
  const gchar *tag_name;
  GstBuffer *image;

  g_return_val_if_fail (GST_IS_TAG_LIST (tag_list), FALSE);
  g_return_val_if_fail (image_data != NULL, FALSE);
  g_return_val_if_fail (image_data_len > 0, FALSE);

  if (id3_picture_type == 0x01 || id3_picture_type == 0x02) {
    /* file icon for preview. Don't add image-type to caps, since there
     * is only supposed to be one of these, and the type is already indicated
     * via the special tag */
    tag_name = GST_TAG_PREVIEW_IMAGE;
    tag_image_type = GST_TAG_IMAGE_TYPE_NONE;
  } else {
    tag_name = GST_TAG_IMAGE;

    /* Remap the ID3v2 APIC type our ImageType enum */
    if (id3_picture_type >= 0x3 && id3_picture_type <= 0x14)
      tag_image_type = (GstTagImageType) (id3_picture_type - 2);
    else
      tag_image_type = GST_TAG_IMAGE_TYPE_UNDEFINED;
  }

  image = gst_tag_image_data_to_image_buffer (image_data, image_data_len,
      tag_image_type);

  if (image == NULL)
    return FALSE;

  gst_tag_list_add (tag_list, GST_TAG_MERGE_APPEND, tag_name, image, NULL);
  gst_buffer_unref (image);
  return TRUE;
}
