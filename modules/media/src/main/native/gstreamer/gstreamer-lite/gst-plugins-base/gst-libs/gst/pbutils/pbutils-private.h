/* GStreamer
 * Copyright (C) 2010 Edward Hervey <edward.hervey@collabora.co.uk>
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

struct _GstDiscovererStreamInfo {
  GstMiniObject          parent;

  GstDiscovererStreamInfo *previous;  /* NULL for starting points */
  GstDiscovererStreamInfo *next; /* NULL for containers */

  GstCaps               *caps;
  GstTagList            *tags;
  GstStructure          *misc;
};

struct _GstDiscovererContainerInfo {
  GstDiscovererStreamInfo parent;

  GList               *streams;
};

struct _GstDiscovererAudioInfo {
  GstDiscovererStreamInfo parent;

  guint channels;
  guint sample_rate;
  guint depth;

  guint bitrate;
  guint max_bitrate;
};

struct _GstDiscovererVideoInfo {
  GstDiscovererStreamInfo parent;

  guint width;
  guint height;
  guint depth;
  guint framerate_num;
  guint framerate_denom;
  guint par_num;
  guint par_denom;
  gboolean interlaced;

  guint bitrate;
  guint max_bitrate;

  gboolean is_image;
};

struct _GstDiscovererInfo {
  GstMiniObject parent;

  gchar *uri;
  GstDiscovererResult result;

  /* Sub-streams */
  GstDiscovererStreamInfo *stream_info;
  GList *stream_list;

  /* Stream global information */
  GstClockTime duration;
  GstStructure *misc;
  GstTagList *tags;
  gboolean seekable;
};

/* missing-plugins.c */

GstCaps *copy_and_clean_caps (const GstCaps * caps);
