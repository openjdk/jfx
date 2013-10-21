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

#ifndef __BASE_DECODER_H__
#define __BASE_DECODER_H__

#include <gst/gst.h>
#include <libavcodec/avcodec.h>
#include "avelement.h"

G_BEGIN_DECLS

#define TYPE_BASEDECODER            (basedecoder_get_type())
#define BASEDECODER(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), TYPE_BASEDECODER, BaseDecoder))
#define BASEDECODER_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass), TYPE_BASEDECODER, BaseDecoderClass))
#define BASEDECODER_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), TYPE_BASEDECODER, BaseDecoderClass))
#define IS_BASEDECODER(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), TYPE_BASEDECODER))
#define IS_BASEDECODER_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass), TYPE_BASEDECODER))

#define NO_DATA_USED -1

typedef struct _BaseDecoder       BaseDecoder;
typedef struct _BaseDecoderClass  BaseDecoderClass;

struct _BaseDecoder
{
    AVElement parent;

    GstPad *sinkpad;
    GstPad *srcpad;

    gboolean      is_initialized;    // decoder is initialized at _chain() with concrete caps.
    volatile gboolean is_flushing;   // element is between flush_start and flush_stop

    gboolean      is_hls;

    guint8        *codec_data;       // codec-specific data
    gint          codec_data_size;   // number of bytes of codec-specific data

    AVCodec        *codec;           // the libavcodec decoder reference
    AVCodecContext *context;         // the libavcodec context
};

struct _BaseDecoderClass
{
    AVElementClass parent_class;

    void (*init_context)(BaseDecoder* decoder);
};

GType     basedecoder_get_type (void);

void      basedecoder_init_state(BaseDecoder *decoder);

gboolean  basedecoder_open_decoder(BaseDecoder *decoder, enum CodecID id);

void      basedecoder_set_codec_data(BaseDecoder *decoder, GstStructure *s);

void      basedecoder_init_context(BaseDecoder *decoder);

void      basedecoder_flush(BaseDecoder *decoder);

void      basedecoder_close_decoder(BaseDecoder *decoder);

G_END_DECLS

#endif // __BASE_DECODER_H__
