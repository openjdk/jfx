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

#ifndef __VP6_DECODER_H__
#define __VP6_DECODER_H__

#include <gst/gst.h>

#define ON2_CODEC_DISABLE_COMPAT 1

#include <on2_decoder_config.h>

// Define some compatibility macros to avoid having to pepper the source with conditionals
// Constants
#define ON2_CODEC_OK                    ON2_DEC_OK
// Types
#define on2_codec_iface_t               on2_dec_iface_t
#define on2_codec_ctx_t                 on2_dec_ctx_t
#define on2_codec_iter_t                on2_dec_iter_t
#define on2_codec_err_t                 on2_dec_err_t
#define on2_codec_stream_info_t         on2_dec_stream_info_t
// Globals
#define on2_codec_vp6_algo              on2_dec_vp6_algo
#define on2_codec_vp6f_algo             on2_dec_vp6f_algo
// Functions
#define on2_codec_dec_init(aa,bb,cc,dd) on2_dec_init(aa,bb)
#define on2_codec_destroy               on2_dec_destroy
#define on2_codec_decode                on2_dec_decode
#define on2_codec_get_frame             on2_dec_get_frame
#define on2_codec_error                 on2_dec_error
#define on2_codec_peek_stream_info      on2_dec_peek_stream_info

// These are available in all cases
#include <on2_decoder.h>
#include <on2_image.h>
#include <vp6.h>


#ifdef WIN32
#define ADDR_ALIGN __declspec(align(32))
#else // WIN32
#define ADDR_ALIGN __attribute__((aligned(32)))
#endif // WIN32


G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define TYPE_VP6_DECODER \
  (vp6decoder_get_type())
#define VP6_DECODER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),TYPE_VP6_DECODER,VP6Decoder))
#define VP6_DECODER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),TYPE_VP6_DECODER,VP6DecoderClass))
#define IS_VP6_DECODER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),TYPE_VP6_DECODER))
#define IS_VP6_DECODER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),TYPE_VP6_DECODER))

typedef struct _VP6Decoder      VP6Decoder;
typedef struct _VP6DecoderClass VP6DecoderClass;

struct _VP6Decoder
{
    //Base structure
    GstElement element;

    // Pads
    GstPad *sinkpad, *srcpad;

    //Decoding context
    on2_codec_ctx_t *decoder;
    on2_codec_ctx_t *alphaDecoder;

    //Caps
    gboolean            need_set_caps;
    gint                width, height;                  /* Display size of image */
    gint                encoded_width, encoded_height;  /* Actual size of image stored in buffer */
    gint                framerate_num;
    gint                framerate_den;
    gboolean            have_par;
    gint                par_num, par_den;
    gboolean            decodeAlpha;        /* vp6-alpha stream, bitstream needs to be parsed before splitting and sending to decoder contexts */

    // image plane sizes, use to detect changes in plane layout
    gint                plane_size[4];

    //Temporary buffer
    ADDR_ALIGN guint8  *tmp_input_buf;      /* Temp buffer used to send compressed data to decoder */
    gint                tmp_input_buf_size; /* Size in bytes of temp buffer */

    //Current segment
    GstSegment          segment;

    //Last QoS message data
    gdouble             qos_proportion;     /* proportion member of QoS event */
    GstClockTimeDiff    qos_diff;           /* diff member of QoS event */
    GstClockTime        qos_timestamp;      /* timestamp member of QoS event */

    //QoS runtime data
    gboolean            qos_dropping;       /* TRUE if filter is dropping frame */
    gboolean            qos_discont;        /* TRUE to set DISCONT flag for next buffer */
    int                 frames_received;    /* total number of frames received */
    int                 keyframes_received; /* total number of keyframes received */
    int                 delta_sequence;     /* count of delta frames from last keyframe */
};

struct _VP6DecoderClass
{
  GstElementClass parent_class;
};

GType vp6decoder_get_type (void);

G_END_DECLS

#endif
