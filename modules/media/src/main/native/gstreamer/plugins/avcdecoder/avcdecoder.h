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

#ifndef __AVCDECODER_H__
#define __AVCDECODER_H__

#include <gst/gst.h>
#include <CoreFoundation/CoreFoundation.h>
#include <VideoDecodeAcceleration/VDADecoder.h>

G_BEGIN_DECLS

#define TYPE_AVCDECODER \
(avcdecoder_get_type())
#define AVCDECODER(obj) \
(G_TYPE_CHECK_INSTANCE_CAST((obj),TYPE_AVCDECODER,AvcDecoder))
#define AVCDECODER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_CAST((klass),TYPE_AVCDECODER,AvcDecoderClass))
#define IS_AVCDECODER(obj) \
(G_TYPE_CHECK_INSTANCE_TYPE((obj),TYPE_AVCDECODER))
#define IS_AVCDECODER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_TYPE((klass),TYPE_AVCDECODER))

typedef struct _AvcDecoder      AvcDecoder;
typedef struct _AvcDecoderClass AvcDecoderClass;

struct _AvcDecoder
{
    GstElement element;

    GstPad *sinkpad;        // input port for MPEG-4 audio
    GstPad *srcpad;         // output port for PCM samples

    VDADecoder decoder;     // the Video Decode Acceleration framework decoder reference
    VDADecoderOutputCallback* outputCallback; // the callback which receives decoded frames

    gboolean is_initialized; // whether this structure is initialized
    gboolean is_newsegment; // whether a new segment has been received
    volatile gboolean is_flushing; // element is between flush start and stop
    gboolean is_stride_set; // whether the output buffer stride has been set

    gint64 segment_start; // the start time of the segment

    GstClockTime frame_duration; // the duration of a single video frame (nsec)

    GQueue* ordered_frames;      // decoded frames sorted into order of increasign time stamp

    GstClockTime previous_timestamp; // the timestamp of the most recent preceding frame
    GstClockTime timestamp_ceil;     // increment above previous timestamp in which frame should fall
};

struct _AvcDecoderClass
{
    GstElementClass parent_class;
};

GType avcdecoder_get_type (void);
gboolean avcdecoder_plugin_init (GstPlugin * avcdecoder);

G_END_DECLS

#endif /* __AVCDECODER_H__ */
