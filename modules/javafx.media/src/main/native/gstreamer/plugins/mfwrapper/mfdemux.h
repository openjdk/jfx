/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __MF_DEMUX_H__
#define __MF_DEMUX_H__

#include <gst/gst.h>

#include <mfapi.h>
#include <mfidl.h>
#include <mfreadwrite.h>

#include "mfgstbytestream.h"
#include "mftrace.h"

#include "fxplugins_common.h"

typedef struct {
    JFX_CODEC_ID codecID;
    UINT32 uiChannels;
    UINT32 uiRate;
    GstBuffer *codec_data;
} AudioFormat;

typedef struct {
    JFX_CODEC_ID codecID;
    UINT32 uiWidth;
    UINT32 uiHeight;
    UINT32 uiFrameRateNum;
    UINT32 uiFrameRateDen;
    UINT32 uiInterlaceMode;
    UINT32 uiPixelAspectRatioNum;
    UINT32 uiPixelAspectRatioDen;
    UINT32 uiMPEG2Profile;
    UINT32 uiMPEG2Level;
    GstBuffer *sequence_header;
} VideoFormat;

G_BEGIN_DECLS

#define GST_TYPE_MFDEMUX \
    (gst_mfdemux_get_type())
#define GST_MFDEMUX(obj) \
    (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_MFDEMUX,GstMFDemux))
#define GST_MFDEMUX_CLASS(klass) \
    (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_MFDEMUX,GstMFDemuxClass))
#define GST_IS_MFDEMUX(obj) \
    (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_MFDEMUX))
#define GST_IS_MFDEMUX_CLASS(klass) \
    (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_MFDEMUX))

typedef struct _GstMFDemux      GstMFDemux;
typedef struct _GstMFDemuxClass GstMFDemuxClass;

struct _GstMFDemux
{
    GstElement element;

    GMutex lock;

    GstFlowReturn src_result;

    GstPad *sink_pad;         // input pad

    HRESULT hr_mfstartup;

    gboolean is_flushing;
    gboolean is_eos;
    gboolean force_discontinuity;
    gboolean is_demux_initialized;
    gboolean send_new_segment;
    gboolean start_task_on_first_segment;
    gboolean is_hls;
#if TRACE_ENABLE
    gboolean log_first_audio_pts;
    gboolean log_first_video_pts;
    GstClockTime last_audio_pts;
    GstClockTime last_audio_dur;
    GstClockTime last_video_pts;
    GstClockTime last_video_dur;
#endif


    gdouble rate;
    gint64 seek_position;

    CMFGSTByteStream *pGSTMFByteStream;
    IMFByteStream *pIMFByteStream;
    IMFSourceReader *pSourceReader;

    LONGLONG llDuration;

    AudioFormat audioFormat;
    VideoFormat videoFormat;

    GstPad *audio_src_pad;
    GstPad *video_src_pad;

    DWORD audio_stream_index;
    DWORD video_stream_index;

    GstEvent *cached_segment_event;
};

struct _GstMFDemuxClass
{
    GstElementClass parent_class;
};

GType gst_mfdemux_get_type(void);

gboolean mfdemux_init(GstPlugin* mfdemux);

G_END_DECLS

#endif // __MF_DEMUX_H__
