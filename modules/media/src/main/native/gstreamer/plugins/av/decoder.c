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

#include "decoder.h"

/***********************************************************************************
 * Static AVCodec library lock. One for all instances. Necessary for avcodec_open
 ***********************************************************************************/
static GStaticMutex avlib_lock = G_STATIC_MUTEX_INIT;

/***********************************************************************************
 * Substitution for
 * GST_BOILERPLATE (BaseDecoder, basedecoder, AVElement, TYPE_AVELEMENT);
 ***********************************************************************************/
static void basedecoder_class_init(BaseDecoderClass *g_class);
//static void basedecoder_init(BaseDecoder *decoder, BaseDecoderClass *g_class);
static void basedecoder_init_context_default(BaseDecoder *decoder);

static GstElementClass *parent_class = NULL;

static void basedecoder_class_init_trampoline(gpointer g_class, gpointer data) {
    parent_class = (GstElementClass *) g_type_class_peek_parent(g_class);
    basedecoder_class_init(BASEDECODER_CLASS(g_class));
}

GType basedecoder_get_type(void) {
    static volatile gsize gonce_data = 0;
    // INLINE - g_once_init_enter()
    if (g_once_init_enter(&gonce_data)) {
        GType _type = gst_type_register_static_full(TYPE_AVELEMENT,
                g_intern_static_string("BaseDecoder"),
                sizeof (BaseDecoderClass),
                NULL, //basedecoder_base_init,
                NULL,
                basedecoder_class_init_trampoline,
                NULL,
                NULL,
                sizeof (BaseDecoder),
                0,
                NULL, //(GInstanceInitFunc) basedecoder_init,
                NULL,
                (GTypeFlags) 0);
        g_once_init_leave(&gonce_data, (gsize) _type);
    }
    return (GType) gonce_data;
}

static void basedecoder_class_init(BaseDecoderClass *g_class)
{
    avcodec_register_all();

    g_class->init_context = basedecoder_init_context_default;
}

void basedecoder_init_state(BaseDecoder *decoder)
{
    decoder->codec_data = NULL;
    decoder->codec_data_size = 0;

    decoder->context = NULL;
    decoder->codec = NULL;

    decoder->is_initialized = FALSE;
    decoder->is_flushing = FALSE;
    decoder->is_hls = FALSE;
}

gboolean basedecoder_open_decoder(BaseDecoder *decoder, enum CodecID id)
{
    gboolean result = TRUE;

    g_static_mutex_lock(&avlib_lock);

    decoder->codec = avcodec_find_decoder(id);
    result = (decoder->codec != NULL);
    if (result)
    {
#if LIBAVCODEC_NEW
        decoder->context = avcodec_alloc_context3(decoder->codec);
#else
        decoder->context = avcodec_alloc_context();
#endif
        result = (decoder->context != NULL);

        if (result)
        {
            basedecoder_init_context(decoder);

#if LIBAVCODEC_NEW
            int ret = avcodec_open2(decoder->context, decoder->codec, NULL);
#else
            int ret = avcodec_open(decoder->context, decoder->codec);
#endif
            if (ret < 0) // Can't open codec
            {
                av_free(decoder->context);

                decoder->context = NULL;
                decoder->codec = NULL;

                result = FALSE;
            }
        }
    }

    g_static_mutex_unlock(&avlib_lock);
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
        decoder->codec_data_size = GST_BUFFER_SIZE(codec_data_buf);
        decoder->codec_data = g_memdup(GST_BUFFER_DATA(codec_data_buf), GST_BUFFER_SIZE(codec_data_buf));
    }
}

void basedecoder_flush(BaseDecoder *decoder)
{
    if (decoder->context)
    {
        avcodec_flush_buffers(decoder->context);
        avcodec_default_free_buffers(decoder->context);
    }
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
}
