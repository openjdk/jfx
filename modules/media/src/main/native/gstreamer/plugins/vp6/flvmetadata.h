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

#ifndef __FLV_METADATA_H__
#define __FLV_METADATA_H__

#include <gst/gst.h>


#define FLV_SCRIPT_DATA_TYPE_DOUBLE (0)
#define FLV_SCRIPT_DATA_TYPE_BOOL (1)
#define FLV_SCRIPT_DATA_TYPE_STRING (2)
#define FLV_SCRIPT_DATA_TYPE_OBJECT (3)
#define FLV_SCRIPT_DATA_TYPE_MOVIE_CLIP (4)
#define FLV_SCRIPT_DATA_TYPE_NULL (5)
#define FLV_SCRIPT_DATA_TYPE_UNDEFINED (6)
#define FLV_SCRIPT_DATA_TYPE_REFERENCE (7)
#define FLV_SCRIPT_DATA_TYPE_ECMA (8)
#define FLV_SCRIPT_DATA_TYPE_TERMINATOR (9)
#define FLV_SCRIPT_DATA_TYPE_STRICT (10)
#define FLV_SCRIPT_DATA_TYPE_DATE (11)
#define FLV_SCRIPT_DATA_TYPE_LONG_STRING (12)


typedef struct _FlvScriptDataReader FlvScriptDataReader;
typedef struct _FlvKeyframe FlvKeyframe;
typedef struct _FlvMetadata FlvMetadata;

struct _FlvScriptDataReader {
    guchar*         position;
    guchar*         end;
};

struct _FlvKeyframe {
    GstClockTime    time;
    guint64         fileposition;
};

struct _FlvMetadata {
    //Metadata
    GstClockTime duration;
    gint file_size;
    gboolean can_seek_to_end;
    gint video_codec_id;
    gdouble video_data_rate;
    gint width;
    gint height;
    gint par_x;
    gint par_y;
    gdouble framerate;
    gint audio_codec_id;
    gint audio_data_rate;
    gint audio_sample_size;
    gboolean is_stereo;

    //List of custom tags
    GstTagList* tag_list;

    // keyframe list, from onMetaData
    GArray *keyframes;   // GArray of FlvKeyframe
};

/*!
 * \brief Allocate a new metadata structure
 */
FlvMetadata *flv_metadata_new();

/*!
 * \brief Free a previously allocated metadata structure, including anything it contains that has been allocated
 */
void flv_metadata_free(FlvMetadata *metadata);

/*!
 * \brief Attempts to read onMetaData tag from FLV file.
 */
gboolean
flv_script_data_read(FlvScriptDataReader* reader,
        FlvMetadata* metadata);

#endif
