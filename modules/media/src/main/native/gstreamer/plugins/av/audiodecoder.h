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

#ifndef __AUDIODECODER_H__
#define __AUDIODECODER_H__

#include "decoder.h"
#include <libavcodec/avcodec.h>

G_BEGIN_DECLS

#define TYPE_AUDIODECODER \
(audiodecoder_get_type())
#define AUDIODECODER(obj) \
(G_TYPE_CHECK_INSTANCE_CAST((obj),TYPE_AUDIODECODER,AudioDecoder))
#define AUDIODECODER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_CAST((klass),TYPE_AUDIODECODER,AudioDecoderClass))
#define IS_AUDIODECODER(obj) \
(G_TYPE_CHECK_INSTANCE_TYPE((obj),TYPE_AUDIODECODER))
#define IS_AUDIODECODER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_TYPE((klass),TYPE_AUDIODECODER))

#define AV_AUDIO_DECODER_PLUGIN_NAME "avaudiodecoder"

#define AUDIODECODER_BITS_PER_SAMPLE       16
#define AUDIODECODER_OUT_NUM_CHANNELS       2

typedef struct _AudioDecoder      AudioDecoder;
typedef struct _AudioDecoderClass AudioDecoderClass;

struct _AudioDecoder {
    BaseDecoder  parent;

    guint8       *samples;          // temporary output buffer

    gboolean     is_synced;         // whether the first audio frame has been found
    gboolean     is_discont;        // whether the next frame is a discontinuity

    enum CodecID codec_id;          // the libavcodec codec ID

    gint         num_channels;      // channels / stream
    guint        bytes_per_sample;  // bytes / sample
    gint         sample_rate;       // samples / second
    guint        samples_per_frame; // samples / frame
    gint         bit_rate;

    guint64      initial_offset;    // offset of first frame in stream (bytes)
    GstClockTime duration;          // duration of the MP3 stream (nsec.)
    guint        frame_duration;    // duration of a frame (nsec.)
    guint64      total_samples;     // sample offset from zero at current time
    gboolean     generate_pts;

#if LIBAVCODEC_NEW
    AVPacket       packet;
#else // ! LIBAVCODEC_NEW
    uint8_t        *packet;
    int            packet_size;
#endif // LIBAVCODEC_NEW
};

struct _AudioDecoderClass
{
    BaseDecoderClass parent_class;
};

GType audiodecoder_get_type (void);

gboolean audiodecoder_plugin_init (GstPlugin * audiodecoder);

G_END_DECLS

#endif // __AUDIODECODER_H__
