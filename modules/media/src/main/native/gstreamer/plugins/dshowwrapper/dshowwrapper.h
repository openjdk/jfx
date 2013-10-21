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

#ifndef __DSHOW_WRAPPER_H__
#define __DSHOW_WRAPPER_H__

#include <gst/gst.h>

#include "Src.h"
#include "Sink.h"

#include <Bdaiface.h>

G_BEGIN_DECLS

#define MAX_OUTPUT_DS_STREAMS          3 // Should be 3 for now.
#define MP2T_VIDEO_INDEX               0
#define MP2T_AUDIO_INDEX               1
#define MP2T_DATA_INDEX                2
#define DEFAULT_OUTPUT_DS_STREAM_INDEX 0

#define ENABLE_CLOCK 0

enum MEDIA_FORMAT
{
    MEDIA_FORMAT_NONE = 0,
    MEDIA_FORMAT_UNKNOWN,   // Used if format is unknown
    MEDIA_FORMAT_VIDEO_AVC1,
    MEDIA_FORMAT_VIDEO_H264,
    MEDIA_FORMAT_AUDIO_MP3,
    MEDIA_FORMAT_AUDIO_AAC,
    MEDIA_FORMAT_STREAM_MP2T,
    MEDIA_FORMAT_VIDEO_I420,
    MEDIA_FORMAT_VIDEO_YV12
};

// Will be used as bitwise flags
enum DECODER_SETTINGS
{
    DECODER_SETTING_NONE = 0,
    DECODER_SETTING_FORCE_STEREO_OUTPUT = 1,
};

#define AACDECODER_ENDIANNESS G_LITTLE_ENDIAN

#define GST_TYPE_DSHOWWRAPPER \
    (gst_dshowwrapper_get_type())
#define GST_DSHOWWRAPPER(obj) \
    (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_DSHOWWRAPPER,GstDShowWrapper))
#define GST_DSHOWWRAPPER_CLASS(klass) \
    (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_DSHOWWRAPPER,GstDShowWrapperClass))
#define GST_IS_DSHOWWRAPPER(obj) \
    (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_DSHOWWRAPPER))
#define GST_IS_DSHOWWRAPPER_CLASS(klass) \
    (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_DSHOWWRAPPER))

typedef struct _GstDShowWrapper      GstDShowWrapper;
typedef struct _GstDShowWrapperClass GstDShowWrapperClass;

struct _GstDShowWrapper
{
    GstElement element;

    GstPad *sinkpad;         // input pad
    GstPad *srcpad[MAX_OUTPUT_DS_STREAMS];          // output pads

    CCritSec *pDSLock; // Used to lock DirectShow init/deinit
    IFilterGraph *pGraph;
    IMediaControl *pMediaControl;
    CSrc *pSrc;
    IBaseFilter *pISrc;
    CSink *pSink[MAX_OUTPUT_DS_STREAMS];
    gboolean is_sink_connected[MAX_OUTPUT_DS_STREAMS];
    IBaseFilter *pISink[MAX_OUTPUT_DS_STREAMS];
    IBaseFilter *pDecoder;
    IMPEG2PIDMap *pMPEG2PIDMap[MAX_OUTPUT_DS_STREAMS]; // Store interfaces to map PID for MP2T
    ULONG Pid[MAX_OUTPUT_DS_STREAMS];
#if ENABLE_CLOCK
    IReferenceClock *pDSClock;
#endif // ENABLE_CLOCK
    CCritSec *pPTSLock; // Used to lock PTS initialization for MP2T
    CCritSec *pPIDLock; // Used to lock PID initialization for MP2T

    MEDIA_FORMAT eInputFormat;
    MEDIA_FORMAT eOutputFormat[MAX_OUTPUT_DS_STREAMS];

    DECODER_SETTINGS eDecoderSettings;

    guint64 offset[MAX_OUTPUT_DS_STREAMS];

    gboolean is_flushing;
    gboolean is_eos[MAX_OUTPUT_DS_STREAMS];
    gboolean is_eos_received;

    GstBuffer *out_buffer[MAX_OUTPUT_DS_STREAMS];

    gboolean enable_pts;

    gboolean enable_mp3;
    gboolean acm_wrapper;
    gint64 mp3_duration;
    gint64 mp3_id3_size;

    gint codec_id;

    gboolean is_data_produced;
    guint32 input_buffers_count;

    gboolean enable_position;
    GstClockTime last_stop;

    gboolean force_discontinuity;

    // For PID mapping
    gboolean get_pid;
    gboolean map_pid;
    gboolean first_map_pid;
    gboolean skip_flush;
    gint64 seek_position;
    gdouble rate;

#if ENABLE_CLOCK
    GstClock *clock;
#endif // ENABLE_CLOCK

    gboolean set_base_pts;
    GstClockTime base_pts;
    GstClockTime last_pts[MAX_OUTPUT_DS_STREAMS];
    GstClockTime offset_pts[MAX_OUTPUT_DS_STREAMS];

    GstEvent *pending_event;
};

struct _GstDShowWrapperClass
{
    GstElementClass parent_class;
};

GType gst_dshowwrapper_get_type(void);

gboolean dshowwrapper_init(GstPlugin* aacdecoder);

G_END_DECLS

#endif // __DSHOW_WRAPPER_H__
