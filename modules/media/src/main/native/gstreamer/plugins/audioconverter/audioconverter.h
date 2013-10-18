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

#ifndef __AUDIOCONVERTER_H__
#define __AUDIOCONVERTER_H__

#include <gst/gst.h>

#if !defined(__COREAUDIO_USE_FLAT_INCLUDES__)
#include <AudioToolbox/AudioToolbox.h>
#include <CoreFoundation/CoreFoundation.h>
#else
#include "AudioToolbox.h"
#include "CoreFoundation.h"
#endif

G_BEGIN_DECLS

#define TYPE_AUDIOCONVERTER \
(audioconverter_get_type())
#define AUDIOCONVERTER(obj) \
(G_TYPE_CHECK_INSTANCE_CAST((obj),TYPE_AUDIOCONVERTER,AudioConverter))
#define AUDIOCONVERTER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_CAST((klass),TYPE_AUDIOCONVERTER,AudioConverterClass))
#define IS_AUDIOCONVERTER(obj) \
(G_TYPE_CHECK_INSTANCE_TYPE((obj),TYPE_AUDIOCONVERTER))
#define IS_AUDIOCONVERTER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_TYPE((klass),TYPE_AUDIOCONVERTER))

// Set to non-zero to enable a slew of print messages, zero to suppress.
#define ENABLE_PRINT_SPEW 0

#define AUDIOCONVERTER_DURATION_UNKNOWN      -1
#define AUDIOCONVERTER_STREAM_LENGTH_UNKNOWN -1

#define AUDIOCONVERTER_DATA_FORMAT_NONE 0
#define AUDIOCONVERTER_DATA_FORMAT_MPA  1
#define AUDIOCONVERTER_DATA_FORMAT_AAC  2

#define AUDIOCONVERTER_INITIAL_BUFFER_SIZE 8192
#define AUDIOCONVERTER_MPEG_MIN_PACKETS    3

#define AUDIOCONVERTER_AAC_ESDS_HEADER_SIZE 12

typedef struct _AudioConverter      AudioConverter;
typedef struct _AudioConverterClass AudioConverterClass;

struct _AudioConverter {
    GstElement element;

    GstPad *sinkpad;         // input compressed audio port
    GstPad *srcpad;          // output compressed audio port

    GQueue *packetDesc;      // queue of compressed audio packets
    GArray *inputData;       // buffer of encoded audio samples
    guint inputOffset;       // offset into input buffer

    gboolean is_initialized; // whether the struct has been set from a frame
    gboolean is_synced;      // whether the first audio frame has been found
    gboolean is_discont;     // whether the next frame is a discontinuity

    guint   data_format;     // the audio data format
    guint64 total_packets;   // number of compressed packets received; reset after seek
    guint64 total_samples;   // sample offset from zero at current time

    guint sampling_rate;     // samples / second
    guint samples_per_frame; // samples / frame
    guint num_channels;      // channel count

    guint64  initial_offset; // offset of first frame in stream (bytes)
    gint64   stream_length;  // length of MPEG audio stream (bytes)
    gint64   duration;       // duration of the MP3 stream (nsec.)

    guint frame_duration;    // duration of a frame (nsec.)

    gboolean is_priming;     // whether the decoder is being primed
    gboolean has_pad_caps;   // whether the pad caps have been set
    gboolean is_flushing;    // element is between flush start and stop

    gboolean enable_parser;  // whether stream parsing is enabled

    AudioFileStreamID audioStreamID;

    Boolean isFormatInitialized;
    AudioStreamBasicDescription audioInputFormat;
    AudioStreamBasicDescription audioOutputFormat;

    UInt64 audioDataPacketCount;
    Boolean hasAudioPacketTableInfo;
    AudioFilePacketTableInfo packetTableInfo;

    UInt32 cookieSize;
    void* cookieData;
    Boolean isAudioConverterReady;
    AudioConverterRef audioConverter;

    AudioStreamPacketDescription* outPacketDescription;

    void* previousDesc; // pointer to AudioStreamPacketDescription memory allocated during
                        // most recent to call to retrieveInputData
};

struct _AudioConverterClass
{
    GstElementClass parent_class;
};

GType audioconverter_get_type (void);

gboolean audioconverter_plugin_init (GstPlugin * audioconverter);

G_END_DECLS

#endif // __AUDIOCONVERTER_H__
