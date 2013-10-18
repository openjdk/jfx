/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef __FLVDEMUX_H__
#define __FLVDEMUX_H__

#include <gst/gst.h>
#include <gst/base/gstadapter.h>

#include "flvparser.h"

G_BEGIN_DECLS
#define TYPE_FLV_DEMUX \
  (flv_demux_get_type())
#define FLV_DEMUX(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),TYPE_FLV_DEMUX,FlvDemux))
#define FLV_DEMUX_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),TYPE_FLV_DEMUX,FlvDemuxClass))
#define IS_FLV_DEMUX(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),TYPE_FLV_DEMUX))
#define IS_FLV_DEMUX_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),TYPE_FLV_DEMUX))

#define FLVDEMUX_AUDIO_FORMAT_MP3       2
#define FLVDEMUX_VIDEO_FORMAT_VP6       4
#define FLVDEMUX_VIDEO_FORMAT_VP6_ALPHA 5

typedef struct _FlvDemux FlvDemux;
typedef struct _FlvDemuxClass FlvDemuxClass;

struct _FlvDemux
{
    GstElement element;

    GstPad *sink_pad;

    GstAdapter *adapter;
    FlvParser parser;
    gboolean is_pulling;
    gboolean need_parser_flush;

    //Stream properties
    gboolean has_video;
    gboolean has_audio;

    //metadata
    FlvMetadata*    metadata;           /* Metadata read from onMetaData tag */
    GstTagList*     queued_tags;        /* Tags waiting push */

    //Indexing and seeking
    guint64         last_file_position; /* Position of tag prefix within file */
    GstClockTime    current_timestamp;  /* Timestamp of last frame */
    GList*          keyframes;          /* Sorted list of FlvKeyframe, only used if no list exists in the metadata */
    gboolean        copied_metadata_keyframes;  /* Set to TRUE once we've processed the keyframe list from metadata */
    GstSegment      segment;
    gboolean        is_flushing;

    //Source pads
    GstPad          *audio_src_pad;
    GstPad          *video_src_pad;

    //Audio runtime info
    guint           sound_format;
    gboolean        audio_linked;
    guint           audio_offset;
    gboolean        audio_discont;
    GstClockTime    audio_prev_timestamp;
    GstClockTime    audio_frame_duration;

    //Video runtime info
    guint           video_codec_id;
    gboolean        video_linked;
    guint           video_offset;
    gboolean        video_discont;
    GstClockTime    video_prev_timestamp;
    GstClockTime    video_frame_duration;

};

struct _FlvDemuxClass
{
  GstElementClass parent_class;
};

GType flv_demux_get_type (void);

G_END_DECLS

#endif

