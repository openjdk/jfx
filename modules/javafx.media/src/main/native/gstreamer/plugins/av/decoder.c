/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include "decoder.h"
#include <libavutil/mem.h>

#if NEW_ALLOC_FRAME
#include <libavutil/frame.h>
#endif

/***********************************************************************************
 * Static AVCodec library lock. One for all instances. Necessary for avcodec_open
 ***********************************************************************************/
G_LOCK_DEFINE_STATIC(avlib_lock);

static void basedecoder_init_context_default(BaseDecoder *decoder);

/***********************************************************************************
 * Substitution for
 * G_DEFINE_TYPE(BaseDecoder, basedecoder, AVElement, TYPE_AVELEMENT);
 ***********************************************************************************/
#define basedecoder_parent_class parent_class
static void basedecoder_init          (BaseDecoder      *self);
static void basedecoder_class_init    (BaseDecoderClass *klass);
static gpointer basedecoder_parent_class = NULL;
static void     basedecoder_class_intern_init (gpointer klass)
{
    basedecoder_parent_class = g_type_class_peek_parent (klass);
    basedecoder_class_init ((BaseDecoderClass*) klass);
}

GType basedecoder_get_type (void)
{
    static volatile gsize gonce_data = 0;
// INLINE - g_once_init_enter()
    if (g_once_init_enter (&gonce_data))
    {
        GType _type;
        _type = g_type_register_static_simple (TYPE_AVELEMENT,
               g_intern_static_string ("BaseDecoder"),
               sizeof (BaseDecoderClass),
               (GClassInitFunc) basedecoder_class_intern_init,
               sizeof(BaseDecoder),
               (GInstanceInitFunc) basedecoder_init,
               (GTypeFlags) 0);
        g_once_init_leave (&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

static void basedecoder_init(BaseDecoder *self)
{
}

static void basedecoder_class_init(BaseDecoderClass *g_class)
{
#if !NO_REGISTER_ALL
    avcodec_register_all();
#endif

    g_class->init_context = basedecoder_init_context_default;
}

void basedecoder_init_state(BaseDecoder *decoder)
{
    decoder->codec_data = NULL;
    decoder->codec_data_size = 0;

    decoder->frame = NULL;
    decoder->context = NULL;
    decoder->codec = NULL;

    decoder->is_initialized = FALSE;
    decoder->is_flushing = FALSE;
    decoder->is_hls = FALSE;
}

gboolean basedecoder_open_decoder(BaseDecoder *decoder, CodecIDType id)
{
    gboolean result = TRUE;

#if NEW_ALLOC_FRAME
    decoder->frame = av_frame_alloc();
#else
    decoder->frame = avcodec_alloc_frame();
#endif
    if (!decoder->frame) {
        return FALSE; // Can't create frame
    }

    G_LOCK(avlib_lock);

    decoder->codec = avcodec_find_decoder(id);
    result = (decoder->codec != NULL);
    if (result)
    {
        decoder->context = avcodec_alloc_context3(decoder->codec);
        result = (decoder->context != NULL);

        if (result)
        {
            basedecoder_init_context(decoder);

            int ret = avcodec_open2(decoder->context, decoder->codec, NULL);
            if (ret < 0) // Can't open codec
            {
                av_free(decoder->context);

                decoder->context = NULL;
                decoder->codec = NULL;

                result = FALSE;
            }
        }
    }

    G_UNLOCK(avlib_lock);
    return result;
}

void basedecoder_init_context(BaseDecoder *decoder)
{
    BASEDECODER_GET_CLASS(decoder)->init_context(decoder);
}

static void basedecoder_init_context_default(BaseDecoder *decoder)
{
    if (decoder->codec_data)
    {
        decoder->context->extradata = decoder->codec_data;
        decoder->context->extradata_size = decoder->codec_data_size;
    }
}

void basedecoder_set_codec_data(BaseDecoder *decoder, GstStructure *s)
{
    if (!gst_structure_get_boolean(s, "hls", &decoder->is_hls))
        decoder->is_hls = FALSE;

    const GValue *value = gst_structure_get_value(s, "codec_data");
    if (value)
    {
        GstBuffer* codec_data_buf = gst_value_get_buffer(value);
        if (codec_data_buf)
        {
            GstMapInfo info;
            if (gst_buffer_map(codec_data_buf, &info, GST_MAP_READ))
            {
                decoder->codec_data_size = info.size;
                decoder->codec_data = g_memdup(info.data, info.size);
                gst_buffer_unmap(codec_data_buf, &info);
            }
        }
    }
}

void basedecoder_flush(BaseDecoder *decoder)
{
    if (decoder->context)
        avcodec_flush_buffers(decoder->context);
}

void basedecoder_close_decoder(BaseDecoder *decoder)
{
    if (decoder->context)
    {
        avcodec_close(decoder->context);
        av_free(decoder->context);
    }
    decoder->context = NULL;

    if(decoder->codec_data)
    {
        g_free(decoder->codec_data);
        decoder->codec_data = NULL;
    }

    if (decoder->frame)
    {
#if NEW_ALLOC_FRAME
        av_frame_free(&decoder->frame);
#else
        av_free(decoder->frame);
        decoder->frame = NULL;
#endif
    }
}
