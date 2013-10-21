/* GStreamer
 * Copyright (C) 2010 Collabora Multimedia
 *               2010 Nokia Corporation
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "pbutils.h"
#include "pbutils-private.h"

static GstDiscovererStreamInfo
    * gst_discoverer_info_copy_int (GstDiscovererStreamInfo * info,
    GHashTable * stream_map);

static GstDiscovererContainerInfo
    * gst_stream_container_info_copy_int (GstDiscovererContainerInfo * ptr,
    GHashTable * stream_map);

static GstDiscovererAudioInfo
    * gst_discoverer_audio_info_copy_int (GstDiscovererAudioInfo * ptr);

static GstDiscovererVideoInfo
    * gst_discoverer_video_info_copy_int (GstDiscovererVideoInfo * ptr);

/* Per-stream information */

G_DEFINE_TYPE (GstDiscovererStreamInfo, gst_discoverer_stream_info,
    GST_TYPE_MINI_OBJECT);

static void
gst_discoverer_stream_info_init (GstDiscovererStreamInfo * info)
{
  /* Nothing needs initialization */
}

static void
gst_discoverer_stream_info_finalize (GstDiscovererStreamInfo * info)
{
  if (info->next)
    gst_mini_object_unref ((GstMiniObject *) info->next);

  if (info->caps)
    gst_caps_unref (info->caps);

  if (info->tags)
    gst_tag_list_free (info->tags);

  if (info->misc)
    gst_structure_free (info->misc);
}

static GstDiscovererStreamInfo *
gst_discoverer_stream_info_copy (GstDiscovererStreamInfo * info)
{
  return gst_discoverer_info_copy_int (info, NULL);
}

static void
gst_discoverer_stream_info_class_init (GstMiniObjectClass * klass)
{
  klass->finalize =
      (GstMiniObjectFinalizeFunction) gst_discoverer_stream_info_finalize;
  klass->copy = (GstMiniObjectCopyFunction) gst_discoverer_stream_info_copy;
}

static GstDiscovererStreamInfo *
gst_discoverer_stream_info_new (void)
{
  return (GstDiscovererStreamInfo *)
      gst_mini_object_new (GST_TYPE_DISCOVERER_STREAM_INFO);
}

static GstDiscovererStreamInfo *
gst_discoverer_info_copy_int (GstDiscovererStreamInfo * info,
    GHashTable * stream_map)
{
  GstDiscovererStreamInfo *ret;
  GType ltyp;

  g_return_val_if_fail (info != NULL, NULL);

  ltyp = G_TYPE_FROM_INSTANCE (info);

  if (ltyp == GST_TYPE_DISCOVERER_CONTAINER_INFO) {
    ret = (GstDiscovererStreamInfo *)
        gst_stream_container_info_copy_int (
        (GstDiscovererContainerInfo *) info, stream_map);
  } else if (ltyp == GST_TYPE_DISCOVERER_AUDIO_INFO) {
    ret = (GstDiscovererStreamInfo *)
        gst_discoverer_audio_info_copy_int ((GstDiscovererAudioInfo *) info);

  } else if (ltyp == GST_TYPE_DISCOVERER_VIDEO_INFO) {
    ret = (GstDiscovererStreamInfo *)
        gst_discoverer_video_info_copy_int ((GstDiscovererVideoInfo *) info);

  } else
    ret = gst_discoverer_stream_info_new ();

  if (info->next) {
    ret->next = gst_discoverer_info_copy_int (info->next, stream_map);
    ret->next->previous = ret;
  }

  if (info->caps)
    ret->caps = gst_caps_copy (info->caps);

  if (info->tags)
    ret->tags = gst_tag_list_copy (info->tags);

  if (info->misc)
    ret->misc = gst_structure_copy (info->misc);

  if (stream_map)
    g_hash_table_insert (stream_map, info, ret);

  return ret;
}

/* Container information */
G_DEFINE_TYPE (GstDiscovererContainerInfo, gst_discoverer_container_info,
    GST_TYPE_DISCOVERER_STREAM_INFO);

static void
gst_discoverer_container_info_init (GstDiscovererContainerInfo * info)
{
  /* Nothing to initialize */
}

static GstDiscovererContainerInfo *
gst_discoverer_container_info_new (void)
{
  return (GstDiscovererContainerInfo *)
      gst_mini_object_new (GST_TYPE_DISCOVERER_CONTAINER_INFO);
}

static void
gst_discoverer_container_info_finalize (GstDiscovererContainerInfo * info)
{
  GList *tmp;

  for (tmp = ((GstDiscovererContainerInfo *) info)->streams; tmp;
      tmp = tmp->next)
    gst_mini_object_unref ((GstMiniObject *) tmp->data);

  gst_discoverer_stream_info_list_free (info->streams);

  gst_discoverer_stream_info_finalize ((GstDiscovererStreamInfo *) info);
}

static void
gst_discoverer_container_info_class_init (GstMiniObjectClass * klass)
{
  klass->finalize =
      (GstMiniObjectFinalizeFunction) gst_discoverer_container_info_finalize;
}

static GstDiscovererContainerInfo *
gst_stream_container_info_copy_int (GstDiscovererContainerInfo * ptr,
    GHashTable * stream_map)
{
  GstDiscovererContainerInfo *ret;
  GList *tmp;

  g_return_val_if_fail (ptr != NULL, NULL);

  ret = gst_discoverer_container_info_new ();

  for (tmp = ((GstDiscovererContainerInfo *) ptr)->streams; tmp;
      tmp = tmp->next) {
    GstDiscovererStreamInfo *subtop = gst_discoverer_info_copy_int (tmp->data,
        stream_map);
    ret->streams = g_list_append (ret->streams, subtop);
    if (stream_map)
      g_hash_table_insert (stream_map, tmp->data, subtop);
  }

  return ret;
}

/* Audio information */
G_DEFINE_TYPE (GstDiscovererAudioInfo, gst_discoverer_audio_info,
    GST_TYPE_DISCOVERER_STREAM_INFO);

static void
gst_discoverer_audio_info_class_init (GstDiscovererAudioInfoClass * klass)
{
  /* Nothing to initialize */
}

static void
gst_discoverer_audio_info_init (GstDiscovererAudioInfo * info)
{
  /* Nothing to initialize */
}

static GstDiscovererAudioInfo *
gst_discoverer_audio_info_new (void)
{
  return (GstDiscovererAudioInfo *)
      gst_mini_object_new (GST_TYPE_DISCOVERER_AUDIO_INFO);
}

static GstDiscovererAudioInfo *
gst_discoverer_audio_info_copy_int (GstDiscovererAudioInfo * ptr)
{
  GstDiscovererAudioInfo *ret;

  ret = gst_discoverer_audio_info_new ();

  ret->channels = ptr->channels;
  ret->sample_rate = ptr->sample_rate;
  ret->depth = ptr->depth;
  ret->bitrate = ptr->bitrate;
  ret->max_bitrate = ptr->max_bitrate;

  return ret;
}

/* Video information */
G_DEFINE_TYPE (GstDiscovererVideoInfo, gst_discoverer_video_info,
    GST_TYPE_DISCOVERER_STREAM_INFO);

static void
gst_discoverer_video_info_class_init (GstMiniObjectClass * klass)
{
  /* Nothing to initialize */
}

static void
gst_discoverer_video_info_init (GstDiscovererVideoInfo * info)
{
  /* Nothing to initialize */
}

static GstDiscovererVideoInfo *
gst_discoverer_video_info_new (void)
{
  return (GstDiscovererVideoInfo *)
      gst_mini_object_new (GST_TYPE_DISCOVERER_VIDEO_INFO);
}

static GstDiscovererVideoInfo *
gst_discoverer_video_info_copy_int (GstDiscovererVideoInfo * ptr)
{
  GstDiscovererVideoInfo *ret;

  ret = gst_discoverer_video_info_new ();

  ret->width = ptr->width;
  ret->height = ptr->height;
  ret->depth = ptr->depth;
  ret->framerate_num = ptr->framerate_num;
  ret->framerate_denom = ptr->framerate_denom;
  ret->par_num = ptr->par_num;
  ret->par_denom = ptr->par_denom;
  ret->interlaced = ptr->interlaced;
  ret->bitrate = ptr->bitrate;
  ret->max_bitrate = ptr->max_bitrate;
  ret->is_image = ptr->is_image;

  return ret;
}

/* Global stream information */
G_DEFINE_TYPE (GstDiscovererInfo, gst_discoverer_info, GST_TYPE_MINI_OBJECT);

static void
gst_discoverer_info_init (GstDiscovererInfo * info)
{
  /* Nothing needs initialization */
}

static void
gst_discoverer_info_finalize (GstDiscovererInfo * info)
{
  g_free (info->uri);

  if (info->stream_info)
    gst_mini_object_unref ((GstMiniObject *) info->stream_info);

  if (info->misc)
    gst_structure_free (info->misc);

  g_list_free (info->stream_list);

  if (info->tags)
    gst_tag_list_free (info->tags);
}

static GstDiscovererInfo *
gst_discoverer_info_new (void)
{
  return (GstDiscovererInfo *) gst_mini_object_new (GST_TYPE_DISCOVERER_INFO);
}

GstDiscovererInfo *
gst_discoverer_info_copy (GstDiscovererInfo * ptr)
{
  GstDiscovererInfo *ret;
  GHashTable *stream_map = g_hash_table_new (g_direct_hash, NULL);
  GList *tmp;

  g_return_val_if_fail (ptr != NULL, NULL);

  ret = gst_discoverer_info_new ();

  ret->uri = g_strdup (ptr->uri);
  if (ptr->stream_info) {
    ret->stream_info = gst_discoverer_info_copy_int (ptr->stream_info,
        stream_map);
  }
  ret->duration = ptr->duration;
  if (ptr->misc)
    ret->misc = gst_structure_copy (ptr->misc);

  /* We need to set up the new list of streams to correspond to old one. The
   * list just contains a set of pointers to streams in the stream_info tree,
   * so we keep a map of old stream info objects to the corresponding new
   * ones and use that to figure out correspondence in stream_list. */
  for (tmp = ptr->stream_list; tmp; tmp = tmp->next) {
    GstDiscovererStreamInfo *old_stream = (GstDiscovererStreamInfo *) tmp->data;
    GstDiscovererStreamInfo *new_stream = g_hash_table_lookup (stream_map,
        old_stream);
    g_assert (new_stream != NULL);
    ret->stream_list = g_list_append (ret->stream_list, new_stream);
  }

  if (ptr->tags)
    ret->tags = gst_tag_list_copy (ptr->tags);

  g_hash_table_destroy (stream_map);
  return ret;
}

static void
gst_discoverer_info_class_init (GstMiniObjectClass * klass)
{
  klass->finalize =
      (GstMiniObjectFinalizeFunction) gst_discoverer_info_finalize;
  klass->copy = (GstMiniObjectCopyFunction) gst_discoverer_info_copy;
}

/**
 * gst_discoverer_stream_info_list_free:
 * @infos: a #GList of #GstDiscovererStreamInfo
 *
 * Decrements the reference count of all contained #GstDiscovererStreamInfo
 * and fress the #GList.
 */
void
gst_discoverer_stream_info_list_free (GList * infos)
{
  GList *tmp;

  for (tmp = infos; tmp; tmp = tmp->next)
    gst_discoverer_stream_info_unref ((GstDiscovererStreamInfo *) tmp->data);
  g_list_free (infos);
}

/**
 * gst_discoverer_info_get_streams:
 * @info: a #GstDiscovererInfo
 * @streamtype: a #GType derived from #GstDiscovererStreamInfo
 *
 * Finds the #GstDiscovererStreamInfo contained in @info that match the
 * given @streamtype.
 *
 * Returns: (transfer full) (element-type Gst.DiscovererStreamInfo): A #GList of
 * matching #GstDiscovererStreamInfo. The caller should free it with
 * gst_discoverer_stream_info_list_free().
 *
 * Since: 0.10.31
 */
GList *
gst_discoverer_info_get_streams (GstDiscovererInfo * info, GType streamtype)
{
  GList *tmp, *res = NULL;

  for (tmp = info->stream_list; tmp; tmp = tmp->next) {
    GstDiscovererStreamInfo *stmp = (GstDiscovererStreamInfo *) tmp->data;

    if (G_TYPE_CHECK_INSTANCE_TYPE (stmp, streamtype))
      res = g_list_append (res, gst_discoverer_stream_info_ref (stmp));
  }

  return res;
}

/**
 * gst_discoverer_info_get_audio_streams:
 * @info: a #GstDiscovererInfo
 *
 * Finds all the #GstDiscovererAudioInfo contained in @info
 *
 * Returns: (transfer full) (element-type Gst.DiscovererStreamInfo): A #GList of
 * matching #GstDiscovererStreamInfo. The caller should free it with
 * gst_discoverer_stream_info_list_free().
 *
 * Since: 0.10.31
 */
GList *
gst_discoverer_info_get_audio_streams (GstDiscovererInfo * info)
{
  return gst_discoverer_info_get_streams (info, GST_TYPE_DISCOVERER_AUDIO_INFO);
}

/**
 * gst_discoverer_info_get_video_streams:
 * @info: a #GstDiscovererInfo
 *
 * Finds all the #GstDiscovererVideoInfo contained in @info
 *
 * Returns: (transfer full) (element-type Gst.DiscovererStreamInfo): A #GList of
 * matching #GstDiscovererStreamInfo. The caller should free it with
 * gst_discoverer_stream_info_list_free().
 *
 * Since: 0.10.31
 */
GList *
gst_discoverer_info_get_video_streams (GstDiscovererInfo * info)
{
  return gst_discoverer_info_get_streams (info, GST_TYPE_DISCOVERER_VIDEO_INFO);
}

/**
 * gst_discoverer_info_get_container_streams:
 * @info: a #GstDiscovererInfo
 *
 * Finds all the #GstDiscovererContainerInfo contained in @info
 *
 * Returns: (transfer full) (element-type Gst.DiscovererStreamInfo): A #GList of
 * matching #GstDiscovererStreamInfo. The caller should free it with
 * gst_discoverer_stream_info_list_free().
 *
 * Since: 0.10.31
 */
GList *
gst_discoverer_info_get_container_streams (GstDiscovererInfo * info)
{
  return gst_discoverer_info_get_streams (info,
      GST_TYPE_DISCOVERER_CONTAINER_INFO);
}

/**
 * gst_discoverer_stream_info_get_stream_type_nick:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: a human readable name for the stream type of the given @info (ex : "audio",
 * "container",...).
 *
 * Since: 0.10.31
 */
const gchar *
gst_discoverer_stream_info_get_stream_type_nick (GstDiscovererStreamInfo * info)
{
  if (GST_IS_DISCOVERER_CONTAINER_INFO (info))
    return "container";
  if (GST_IS_DISCOVERER_AUDIO_INFO (info))
    return "audio";
  if (GST_IS_DISCOVERER_VIDEO_INFO (info)) {
    if (gst_discoverer_video_info_is_image ((GstDiscovererVideoInfo *)
            info))
      return "video(image)";
    else
      return "video";
  }
  return "unknown";
}

/* ACCESSORS */


#define GENERIC_ACCESSOR_CODE(parent, parenttype, parentgtype, fieldname, type, failval) \
  type parent##_get_##fieldname(const parenttype info) {			\
    g_return_val_if_fail(G_TYPE_CHECK_INSTANCE_TYPE((info), parentgtype), failval); \
    return (info)->fieldname;				\
  }

/**
 * gst_discoverer_stream_info_get_previous:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: (transfer full): the previous #GstDiscovererStreamInfo in a chain.
 * %NULL for starting points. Unref with #gst_discoverer_stream_info_unref
 * after usage.
 *
 * Since: 0.10.31
 */
GstDiscovererStreamInfo *
gst_discoverer_stream_info_get_previous (GstDiscovererStreamInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_STREAM_INFO (info), NULL);

  if (info->previous)
    return gst_discoverer_stream_info_ref (info->previous);
  return NULL;
}

/**
 * gst_discoverer_stream_info_get_next:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: (transfer full): the next #GstDiscovererStreamInfo in a chain. %NULL
 * for final streams.
 * Unref with #gst_discoverer_stream_info_unref after usage.
 *
 * Since: 0.10.31
 */
GstDiscovererStreamInfo *
gst_discoverer_stream_info_get_next (GstDiscovererStreamInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_STREAM_INFO (info), NULL);

  if (info->next)
    return gst_discoverer_stream_info_ref (info->next);
  return NULL;
}


/**
 * gst_discoverer_stream_info_get_caps:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: (transfer full): the #GstCaps of the stream. Unref with
 * #gst_caps_unref after usage.
 *
 * Since: 0.10.31
 */
GstCaps *
gst_discoverer_stream_info_get_caps (GstDiscovererStreamInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_STREAM_INFO (info), NULL);

  if (info->caps)
    return gst_caps_ref (info->caps);
  return NULL;
}

/**
 * gst_discoverer_stream_info_get_tags:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: (transfer none): the tags contained in this stream. If you wish to
 * use the tags after the life-time of @info you will need to copy them.
 *
 * Since: 0.10.31
 */

const GstTagList *
gst_discoverer_stream_info_get_tags (GstDiscovererStreamInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_STREAM_INFO (info), NULL);

  return info->tags;
}

/**
 * gst_discoverer_stream_info_get_misc:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: (transfer none): additional information regarding the stream (for
 * example codec version, profile, etc..). If you wish to use the #GstStructure
 * after the life-time of @info you will need to copy it.
 *
 * Since: 0.10.31
 */
const GstStructure *
gst_discoverer_stream_info_get_misc (GstDiscovererStreamInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_STREAM_INFO (info), NULL);

  return info->misc;
}

/* GstDiscovererContainerInfo */

/**
 * gst_discoverer_container_info_get_streams:
 * @info: a #GstDiscovererStreamInfo
 *
 * Returns: (transfer full) (element-type Gst.DiscovererStreamInfo): the list of
 * #GstDiscovererStreamInfo this container stream offers.
 * Free with gst_discoverer_stream_info_list_free() after usage.
 *
 * Since: 0.10.31
 */

GList *
gst_discoverer_container_info_get_streams (GstDiscovererContainerInfo * info)
{
  GList *res = NULL, *tmp;

  g_return_val_if_fail (GST_IS_DISCOVERER_CONTAINER_INFO (info), NULL);

  for (tmp = info->streams; tmp; tmp = tmp->next)
    res =
        g_list_append (res,
        gst_discoverer_stream_info_ref ((GstDiscovererStreamInfo *) tmp->data));

  return res;
}

/* GstDiscovererAudioInfo */

#define AUDIO_INFO_ACCESSOR_CODE(fieldname, type, failval)		\
  GENERIC_ACCESSOR_CODE(gst_discoverer_audio_info, GstDiscovererAudioInfo*, \
			GST_TYPE_DISCOVERER_AUDIO_INFO,		\
			fieldname, type, failval)

/**
 * gst_discoverer_audio_info_get_channels:
 * @info: a #GstDiscovererAudioInfo
 *
 * Returns: the number of channels in the stream.
 *
 * Since: 0.10.31
 */

AUDIO_INFO_ACCESSOR_CODE (channels, guint, 0);

/**
 * gst_discoverer_audio_info_get_sample_rate:
 * @info: a #GstDiscovererAudioInfo
 *
 * Returns: the sample rate of the stream in Hertz.
 *
 * Since: 0.10.31
 */

AUDIO_INFO_ACCESSOR_CODE (sample_rate, guint, 0);

/**
 * gst_discoverer_audio_info_get_depth:
 * @info: a #GstDiscovererAudioInfo
 *
 * Returns: the number of bits used per sample in each channel.
 *
 * Since: 0.10.31
 */

AUDIO_INFO_ACCESSOR_CODE (depth, guint, 0);

/**
 * gst_discoverer_audio_info_get_bitrate:
 * @info: a #GstDiscovererAudioInfo
 *
 * Returns: the average or nominal bitrate of the stream in bits/second.
 *
 * Since: 0.10.31
 */

AUDIO_INFO_ACCESSOR_CODE (bitrate, guint, 0);

/**
 * gst_discoverer_audio_info_get_max_bitrate:
 * @info: a #GstDiscovererAudioInfo
 *
 * Returns: the maximum bitrate of the stream in bits/second.
 *
 * Since: 0.10.31
 */

AUDIO_INFO_ACCESSOR_CODE (max_bitrate, guint, 0);

/* GstDiscovererVideoInfo */

#define VIDEO_INFO_ACCESSOR_CODE(fieldname, type, failval)		\
  GENERIC_ACCESSOR_CODE(gst_discoverer_video_info, GstDiscovererVideoInfo*, \
			GST_TYPE_DISCOVERER_VIDEO_INFO,			\
			fieldname, type, failval)

/**
 * gst_discoverer_video_info_get_width:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the width of the video stream in pixels.
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (width, guint, 0);

/**
 * gst_discoverer_video_info_get_height:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the height of the video stream in pixels.
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (height, guint, 0);

/**
 * gst_discoverer_video_info_get_depth:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the depth in bits of the video stream.
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (depth, guint, 0);

/**
 * gst_discoverer_video_info_get_framerate_num:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the framerate of the video stream (numerator).
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (framerate_num, guint, 0);

/**
 * gst_discoverer_video_info_get_framerate_denom:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the framerate of the video stream (denominator).
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (framerate_denom, guint, 0);

/**
 * gst_discoverer_video_info_get_par_num:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the Pixel Aspect Ratio (PAR) of the video stream (numerator).
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (par_num, guint, 0);

/**
 * gst_discoverer_video_info_get_par_denom:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the Pixel Aspect Ratio (PAR) of the video stream (denominator).
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (par_denom, guint, 0);

/**
 * gst_discoverer_video_info_is_interlaced:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: %TRUE if the stream is interlaced, else %FALSE.
 *
 * Since: 0.10.31
 */
gboolean
gst_discoverer_video_info_is_interlaced (const GstDiscovererVideoInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_VIDEO_INFO (info), FALSE);

  return info->interlaced;
}

/**
 * gst_discoverer_video_info_get_bitrate:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the average or nominal bitrate of the video stream in bits/second.
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (bitrate, guint, 0);

/**
 * gst_discoverer_video_info_get_max_bitrate:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: the maximum bitrate of the video stream in bits/second.
 *
 * Since: 0.10.31
 */

VIDEO_INFO_ACCESSOR_CODE (max_bitrate, guint, 0);

/**
 * gst_discoverer_video_info_is_image:
 * @info: a #GstDiscovererVideoInfo
 *
 * Returns: #TRUE if the video stream corresponds to an image (i.e. only contains
 * one frame).
 *
 * Since: 0.10.31
 */
gboolean
gst_discoverer_video_info_is_image (const GstDiscovererVideoInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_VIDEO_INFO (info), FALSE);

  return info->is_image;
}

/* GstDiscovererInfo */

#define DISCOVERER_INFO_ACCESSOR_CODE(fieldname, type, failval)		\
  GENERIC_ACCESSOR_CODE(gst_discoverer_info, GstDiscovererInfo*,	\
			GST_TYPE_DISCOVERER_INFO,			\
			fieldname, type, failval)

/**
 * gst_discoverer_info_get_uri:
 * @info: a #GstDiscovererInfo
 *
 * Returns: (transfer none): the URI to which this information corresponds to.
 * Copy it if you wish to use it after the life-time of @info.
 *
 * Since: 0.10.31
 */

DISCOVERER_INFO_ACCESSOR_CODE (uri, const gchar *, NULL);

/**
 * gst_discoverer_info_get_result:
 * @info: a #GstDiscovererInfo
 *
 * Returns: the result of the discovery as a #GstDiscovererResult.
 *
 * Since: 0.10.31
 */

DISCOVERER_INFO_ACCESSOR_CODE (result, GstDiscovererResult, GST_DISCOVERER_OK);

/**
 * gst_discoverer_info_get_stream_info:
 * @info: a #GstDiscovererInfo
 *
 * Returns: (transfer full): the structure (or topology) of the URI as a
 * #GstDiscovererStreamInfo.
 * This structure can be traversed to see the original hierarchy. Unref with
 * gst_discoverer_stream_info_unref() after usage.
 *
 * Since: 0.10.31
 */

GstDiscovererStreamInfo *
gst_discoverer_info_get_stream_info (GstDiscovererInfo * info)
{
  g_return_val_if_fail (GST_IS_DISCOVERER_INFO (info), NULL);

  if (info->stream_info)
    return gst_discoverer_stream_info_ref (info->stream_info);
  return NULL;
}

/**
 * gst_discoverer_info_get_stream_list:
 * @info: a #GstDiscovererInfo
 *
 * Returns: (transfer full) (element-type Gst.DiscovererStreamInfo): the list of
 * all streams contained in the #info. Free after usage
 * with gst_discoverer_stream_info_list_free().
 *
 * Since: 0.10.31
 */
GList *
gst_discoverer_info_get_stream_list (GstDiscovererInfo * info)
{
  GList *res = NULL, *tmp;

  g_return_val_if_fail (GST_IS_DISCOVERER_INFO (info), NULL);

  for (tmp = info->stream_list; tmp; tmp = tmp->next)
    res =
        g_list_append (res,
        gst_discoverer_stream_info_ref ((GstDiscovererStreamInfo *) tmp->data));

  return res;
}

/**
 * gst_discoverer_info_get_duration:
 * @info: a #GstDiscovererInfo
 *
 * Returns: the duration of the URI in #GstClockTime (nanoseconds).
 *
 * Since: 0.10.31
 */

DISCOVERER_INFO_ACCESSOR_CODE (duration, GstClockTime, GST_CLOCK_TIME_NONE);

/**
 * gst_discoverer_info_get_seekable:
 * @info: a #GstDiscovererInfo
 *
 * Returns: the wheter the URI is seekable.
 *
 * Since: 0.10.32
 */

DISCOVERER_INFO_ACCESSOR_CODE (seekable, gboolean, FALSE);

/**
 * gst_discoverer_info_get_misc:
 * @info: a #GstDiscovererInfo
 *
 * Returns: (transfer none): Miscellaneous information stored as a #GstStructure
 * (for example: information about missing plugins). If you wish to use the
 * #GstStructure after the life-time of @info, you will need to copy it.
 *
 * Since: 0.10.31
 */

DISCOVERER_INFO_ACCESSOR_CODE (misc, const GstStructure *, NULL);

/**
 * gst_discoverer_info_get_tags:
 * @info: a #GstDiscovererInfo
 *
 * Returns: (transfer none): all tags contained in the %URI. If you wish to use
 * the tags after the life-time of @info, you will need to copy them.
 *
 * Since: 0.10.31
 */

DISCOVERER_INFO_ACCESSOR_CODE (tags, const GstTagList *, NULL);

/**
 * gst_discoverer_info_ref:
 * @info: a #GstDiscovererInfo
 *
 * Increments the reference count of @info.
 *
 * Returns: the same #GstDiscovererInfo object
 *
 * Since: 0.10.31
 */

/**
 * gst_discoverer_info_unref:
 * @info: a #GstDiscovererInfo
 *
 * Decrements the reference count of @info.
 *
 * Since: 0.10.31
 */

/**
 * gst_discoverer_stream_info_ref:
 * @info: a #GstDiscovererStreamInfo
 *
 * Increments the reference count of @info.
 *
 * Returns: the same #GstDiscovererStreamInfo object
 *
 * Since: 0.10.31
 */

/**
 * gst_discoverer_stream_info_unref:
 * @info: a #GstDiscovererStreamInfo
 *
 * Decrements the reference count of @info.
 *
 * Since: 0.10.31
 */
