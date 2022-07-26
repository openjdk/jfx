/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __VIDEODECODER_H__
#define __VIDEODECODER_H__

#include "decoder.h"

#include <dlfcn.h>
#include <libswscale/swscale.h>

G_BEGIN_DECLS

#define TYPE_VIDEODECODER \
(videodecoder_get_type())
#define VIDEODECODER(obj) \
(G_TYPE_CHECK_INSTANCE_CAST((obj),TYPE_VIDEODECODER,VideoDecoder))
#define VIDEODECODER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_CAST((klass),TYPE_VIDEODECODER,VideoDecoderClass))
#define IS_VIDEODECODER(obj) \
(G_TYPE_CHECK_INSTANCE_TYPE((obj),TYPE_VIDEODECODER))
#define IS_VIDEODECODER_CLASS(klass) \
(G_TYPE_CHECK_CLASS_TYPE((klass),TYPE_VIDEODECODER))

#define AV_VIDEO_DECODER_PLUGIN_NAME "avvideodecoder"

#if HEVC_SUPPORT
// libswscale APIs
typedef struct SwsContext *(*sws_getContext_ptr)(int srcW, int srcH,
                                                 enum AVPixelFormat srcFormat,
                                                 int dstW, int dstH,
                                                 enum AVPixelFormat dstFormat,
                                                 int flags, SwsFilter *srcFilter,
                                                 SwsFilter *dstFilter,
                                                 const double *param);
typedef void (*sws_freeContext_ptr)(struct SwsContext *swsContext);
typedef int (*sws_scale_ptr)(struct SwsContext *c, const uint8_t *const srcSlice[],
                      const int srcStride[], int srcSliceY, int srcSliceH,
                      uint8_t *const dst[], const int dstStride[]);
#endif // HEVC_SUPPORT

typedef struct _VideoDecoder      VideoDecoder;
typedef struct _VideoDecoderClass VideoDecoderClass;

struct _VideoDecoder {
    BaseDecoder parent;

    gint        width;
    gint        height;
    int         frame_finished;
    gboolean    discont;

    int         frame_size;     // in bytes
    int         u_offset;
    int         v_offset;
    int         uv_blocksize;

    AVPacket    packet;

    gint        codec_id;

#if HEVC_SUPPORT
    struct SwsContext *sws_context;
    AVFrame           *dest_frame;

    // Load and use libswscale dynamically
    void                *swscale_module;
    sws_getContext_ptr  sws_getContext_func;
    sws_freeContext_ptr sws_freeContext_func;
    sws_scale_ptr       sws_scale_func;
#endif // HEVC_SUPPORT
};

struct _VideoDecoderClass
{
    BaseDecoderClass parent_class;
};

GType videodecoder_get_type (void);

gboolean videodecoder_plugin_init (GstPlugin * videodecoder);

G_END_DECLS

#endif // __VIDEODECODER_H__
