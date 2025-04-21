/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __MF_WRAPPER_H__
#define __MF_WRAPPER_H__

#include <gst/gst.h>

#include "mfgstbuffer.h"

#include <mfapi.h>
#include <mferror.h>
#include <mftransform.h>

G_BEGIN_DECLS

// Media Foundation Color Convert:
// NV12 -> IYUV
// P010 -> NV12 -> IYUV
// Maximum number of color converters
#define MAX_COLOR_CONVERT 2
// Index in array for color convert with IYUV output format
#define COLOR_CONVERT_IYUV 0
// Index in array for color convert with NV12 output format
#define COLOR_CONVERT_NV12 1

#define GST_TYPE_MFWRAPPER \
    (gst_mfwrapper_get_type())
#define GST_MFWRAPPER(obj) \
    (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_MFWRAPPER,GstMFWrapper))
#define GST_MFWRAPPER_CLASS(klass) \
    (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_MFWRAPPER,GstMFWrapperClass))
#define GST_IS_MFWRAPPER(obj) \
    (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_MFWRAPPER))
#define GST_IS_MFWRAPPER_CLASS(klass) \
    (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_MFWRAPPER))

typedef struct _GstMFWrapper      GstMFWrapper;
typedef struct _GstMFWrapperClass GstMFWrapperClass;

struct _GstMFWrapper
{
    GstElement element;

    GstPad *sinkpad;         // input pad
    GstPad *srcpad;          // output pads

    gint codec_id;

    gboolean is_flushing;
    gboolean is_eos_received;
    gboolean is_eos;
    gboolean is_decoder_initialized;
    // If set to true do not call decoder it might hang.
    // This flag should be set if decoder calls failed.
    gboolean is_decoder_error;

    gboolean is_force_discontinuity;
    gboolean is_force_output_discontinuity;

    HRESULT hr_mfstartup;

    IMFTransform *pDecoder;
    IMFSample *pDecoderOutput;
    CMFGSTBuffer *pDecoderBuffer;

    IMFTransform *pColorConvert[MAX_COLOR_CONVERT];
    IMFSample *pColorConvertOutput[MAX_COLOR_CONVERT];
    CMFGSTBuffer *pColorConvertBuffer[MAX_COLOR_CONVERT];

    GstBufferPool *pool;

    BYTE *header;
    gsize header_size;
    gboolean is_send_header;

    guint width;
    guint height;
    guint framerate_num;
    guint framerate_den;

    guint defaultStride;
    guint pixel_num;
    guint pixel_den;

    gboolean is_set_caps;
};

struct _GstMFWrapperClass
{
    GstElementClass parent_class;
};

GType gst_mfwrapper_get_type(void);

gboolean mfwrapper_init(GstPlugin* mfwrapper);

G_END_DECLS

#endif // __MF_WRAPPER_H__
