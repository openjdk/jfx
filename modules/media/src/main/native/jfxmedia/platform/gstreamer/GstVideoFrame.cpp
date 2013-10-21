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

#include "GstVideoFrame.h"
#include "GstPipelineFactory.h"
#include <cstring>
#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <Utils/LowLevelPerf.h>
#include <Utils/ColorConverter.h>

static inline guint32 swap_uint32(guint32 x)
{
    return
        ((x & 0x000000ffU) << 24) |
        ((x & 0x0000ff00U) <<  8) |
        ((x & 0x00ff0000U) >>  8) |
        ((x & 0xff000000U) >> 24);
}

static GstBuffer *alloc_aligned_buffer(guint size)
{
    // allocate a new GstBuffer of the given size plus some for padding and alignment
    GstBuffer *newBuffer = NULL;
    guint8 *newData;
    guint8 *alignedData;
    guint alignedSize;

    // allocate a buffer large enough to accommodate 16 byte alignment
    alignedSize = size;
    size += 16;
    newData = (guint8*)g_try_malloc(size);
    if (NULL == newData) {
        return NULL;
    }

    // create empty GstBuffer
    newBuffer = gst_buffer_new();
    if (NULL == newBuffer) {
        g_free(newData);
        return NULL;
    }

    // Now set data, size and mallocdata
    alignedData = (guint8*)(((intptr_t)newData + 15) & ~15);
    gst_buffer_set_data(newBuffer, alignedData, alignedSize);
    GST_BUFFER_MALLOCDATA(newBuffer) = newData;

    return newBuffer;
}

GstCaps *create_RGB_caps(CVideoFrame::FrameType type, gint width, gint height, gint encodedWidth, gint encodedHeight, gint stride)
{
    gint red_mask, green_mask, blue_mask, alpha_mask;
    GstCaps *newCaps;

    if (type == CVideoFrame::ARGB) {
        alpha_mask  = 0xFF000000;
        red_mask    = 0x00FF0000;
        green_mask  = 0x0000FF00;
        blue_mask   = 0x000000FF;
    } else if (type == CVideoFrame::BGRA_PRE) {
        alpha_mask  = 0x000000FF;
        red_mask    = 0x0000FF00;
        green_mask  = 0x00FF0000;
        blue_mask   = 0xFF000000;
    }

    newCaps = gst_caps_new_simple("video/x-raw-rgb",
                                  "bpp", G_TYPE_INT, 32,
                                  "depth", G_TYPE_INT, 32,
                                  "red_mask", G_TYPE_INT, red_mask,
                                  "green_mask", G_TYPE_INT, green_mask,
                                  "blue_mask", G_TYPE_INT, blue_mask,
                                  "alpha_mask", G_TYPE_INT, alpha_mask,
                                  "width", G_TYPE_INT, width,
                                  "height", G_TYPE_INT, height,
                                  "encoded-width", G_TYPE_INT, encodedWidth,
                                  "encoded-height", G_TYPE_INT, encodedHeight,
                                  "line_stride", G_TYPE_INT, stride,
                                  NULL);
    return newCaps;
}

CGstVideoFrame::CGstVideoFrame(guint size)
    : m_bIsValid(false), m_pBuffer(NULL)
{
    m_pBuffer = alloc_aligned_buffer(size);
    if (!m_pBuffer) {
        throw GST_FLOW_ERROR;
    }
}

CGstVideoFrame::CGstVideoFrame(GstBuffer* buffer)
    : m_bIsValid(true)
{
    LOWLEVELPERF_COUNTERINC("CGstVideoFrame", 1, 1);

    // Increment the ref count as this object will be created
    // by the video sink and pushed into the FrameQueue.
    m_pBuffer = gst_buffer_ref(buffer);

    GstCaps* caps = GST_BUFFER_CAPS(m_pBuffer);

    m_ulSize        = (unsigned long)GST_BUFFER_SIZE(m_pBuffer);
    m_pvData        = (void*)GST_BUFFER_DATA(m_pBuffer);
    m_ulFrameNumber = (unsigned long) GST_BUFFER_OFFSET(m_pBuffer);


    if (GST_BUFFER_TIMESTAMP_IS_VALID (m_pBuffer)) {
        m_dTime = (double)GST_BUFFER_TIMESTAMP(m_pBuffer) / GST_SECOND;
    } else {
        m_dTime = 0.0;
        m_bIsValid = false;
    }

    SetFrameCaps(caps);
}

CGstVideoFrame::~CGstVideoFrame()
{
    LOWLEVELPERF_COUNTERDEC("CGstVideoFrame", 1, 1);

    if (NULL != m_pBuffer)
        Dispose();
}

void CGstVideoFrame::SetFrameCaps(GstCaps *newCaps)
{
    GstCaps *bufCaps = NULL;
    const GstStructure* str = gst_caps_get_structure(newCaps, 0);
    gst_structure_get_fourcc(str, "format", &m_uFormatFourCC); // ignore return

    // default to success
    m_bIsValid = true;

    // FIXME: make format type strings conformant with constant types
    if (gst_structure_has_name(str, "video/x-raw-yvua420p")) {
        m_typeFrame = YCbCr_420p;
        m_bHasAlpha = true;
    } else if (gst_structure_has_name(str, "video/x-raw-ycbcr422")) {
        m_typeFrame = YCbCr_422;
        m_bHasAlpha = false;
    } else if (gst_structure_has_name(str, "video/x-raw-yuv")) {
        if (FOURCC_UYVY == m_uFormatFourCC) {
            m_typeFrame = YCbCr_422;
        } else {
            m_typeFrame = YCbCr_420p;
        }
        m_bHasAlpha = false;
    } else if (gst_structure_has_name(str, "video/x-raw-rgb")) {
        gint red_mask, green_mask, blue_mask;

        // determine if it's ARGB or BGRA_PRE
        if (gst_structure_get_int(str, "red_mask", &red_mask) &&
            gst_structure_get_int(str, "green_mask", &green_mask) &&
            gst_structure_get_int(str, "blue_mask", &blue_mask))
        {
            if (0x00ff0000 == red_mask || 0x0000ff00 == green_mask || 0x000000ff == blue_mask) {
                m_typeFrame = ARGB;
            } else if (0x0000ff00 == red_mask || 0x00ff0000 == green_mask || 0xff000000 == blue_mask) {
                m_typeFrame = BGRA_PRE;
            } else {
                LOGGER_LOGMSG(LOGGER_DEBUG, "CGstVideoFrame::SetFrameCaps - Invalid RGB mask combination");
                m_bIsValid = false;
                return;
            }
            m_bHasAlpha = true;
        } else {
            // Missing required fields, mark invalid and punt
            m_bIsValid = false;
            return;
        }
    } else {
        m_typeFrame = UNKNOWN;
        m_bHasAlpha = false;
        m_bIsValid = false;
    }

    if(!gst_structure_get_int(str, "width", &m_iWidth))
    {
#if JFXMEDIA_DEBUG
        g_warning("width could not be retrieved from GstBuffer\n");
#endif
        m_iWidth = 0;
        m_bIsValid = false;
    }
    if(!gst_structure_get_int(str, "height", &m_iHeight))
    {
#if JFXMEDIA_DEBUG
        g_warning("height could not be retrieved from GstBuffer\n");
#endif
        m_iHeight = 0;
        m_bIsValid = false;
    }

    if (!gst_structure_get_int(str, "encoded-width", &m_iEncodedWidth)) {
        m_iEncodedWidth = m_iWidth;
    }
    if (!gst_structure_get_int(str, "encoded-height", &m_iEncodedHeight)) {
        m_iEncodedHeight = m_iHeight;
    }

    m_piPlaneOffsets[0] = m_piPlaneOffsets[1] = m_piPlaneOffsets[2] = m_piPlaneOffsets[3] = 0;
    m_piPlaneStrides[0] = m_piPlaneStrides[1] = m_piPlaneStrides[2] = m_piPlaneStrides[3] = 0;

    switch (m_typeFrame) {
        case YCbCr_420p: {
            m_iPlaneCount = 3;

            if (!gst_structure_get_int(str, "stride-y", &m_piPlaneStrides[0])) {
                m_piPlaneStrides[0] = m_iEncodedWidth;
            }
            if (!gst_structure_get_int(str, "stride-v", &m_piPlaneStrides[1])) {
                m_piPlaneStrides[1] = m_iEncodedWidth/2;
            }
            if (!gst_structure_get_int(str, "stride-u", &m_piPlaneStrides[2])) {
                m_piPlaneStrides[2] = m_piPlaneStrides[1];
            }

            if(!gst_structure_get_int(str, "offset-y", &m_piPlaneOffsets[0])) {
                m_piPlaneOffsets[0] = 0;
            }
            //
            // Chroma offsets assume YV12 ordering
            //
            if(!gst_structure_get_int(str, "offset-v", &m_piPlaneOffsets[1])) {
                m_piPlaneOffsets[1] = m_piPlaneOffsets[0] + m_iEncodedHeight * m_piPlaneStrides[0];
            }
            if(!gst_structure_get_int(str, "offset-u", &m_piPlaneOffsets[2])) {
                m_piPlaneOffsets[2] = m_piPlaneOffsets[1] + (m_iEncodedHeight/2) * m_piPlaneStrides[1];
            }

            //
            // Swap chroma planes for I420.
            //
            if(FOURCC_I420 == m_uFormatFourCC) {
                int tmp_index = m_piPlaneOffsets[1];
                m_piPlaneOffsets[1] = m_piPlaneOffsets[2];
                m_piPlaneOffsets[2] = tmp_index;
            }

            // process alpha channel
            if (m_bHasAlpha) {
                m_iPlaneCount++;
                if (!gst_structure_get_int(str, "stride-a", &m_piPlaneStrides[3])) {
                    m_piPlaneStrides[3] = m_piPlaneStrides[0];
                }
                if(!gst_structure_get_int(str, "offset-a", &m_piPlaneOffsets[3])) {
                    m_piPlaneOffsets[3] = m_piPlaneOffsets[2] + (m_iEncodedHeight/2) * m_piPlaneStrides[2];
                }
            }
            break;
        }

        default:
            m_iPlaneCount = 1;
            if (!gst_structure_get_int(str, "line_stride", &m_piPlaneStrides[0])) {
                if (m_typeFrame == YCbCr_422) {
                    m_piPlaneStrides[0] = m_iEncodedWidth * 2; // 16 bpp
                } else {
                    m_piPlaneStrides[0] = m_iEncodedWidth * 4; // 32 bpp
                }
            }
            break;
    }

    bufCaps = GST_BUFFER_CAPS(m_pBuffer);
    if (bufCaps != newCaps) {
        // Updating caps externally
        gst_buffer_set_caps(m_pBuffer, newCaps);
    }
}

bool CGstVideoFrame::IsValid()
{
    return m_bIsValid;
}

// FIXME: I don't think Dispose is necessary anymore, move to the dtor
void CGstVideoFrame::Dispose()
{
    // A reference to this object should be held by its Java
    // peer which should invoke this dispose() method in its
    // finalizer.
// INLINE - gst_buffer_unref()
    gst_buffer_unref(m_pBuffer);
    m_pBuffer = NULL;
}

CVideoFrame *CGstVideoFrame::ConvertToFormat(FrameType type)
{
    CGstVideoFrame *newFrame = NULL;

    // just return myself if the same format is requested
    if (type == m_typeFrame) {
        return this;
    }

    if ((type == YCbCr_422) || (type == YCbCr_420p)) {
        LOGGER_LOGMSG(LOGGER_DEBUG, "Conversion to YCbCr is not supported");
        return NULL;
    }

    switch (m_typeFrame) {
        case ARGB:
        case BGRA_PRE:
            newFrame = ConvertSwapRGB(type);
            break;

        case YCbCr_420p:
            newFrame = ConvertFromYCbCr420p(type);
            break;

        case YCbCr_422:
            newFrame = ConvertFromYCbCr422(type);
            break;

        default:
            break;
    }

    return newFrame;
}

CGstVideoFrame *CGstVideoFrame::ConvertFromYCbCr420p(FrameType destType)
{
    GstBuffer *destBuffer;
    GstCaps *destCaps;
    gint stride = m_iEncodedWidth * 4;
    int u_index, v_index;
    int status;

    if (FOURCC_I420 == m_uFormatFourCC) {
        u_index = 1;
        v_index = 2;
    } else {
        u_index = 2;
        v_index = 1;
    }

    stride = ((stride + 15) & ~15); // round up to multiple of 16 bytes
    destBuffer = alloc_aligned_buffer(stride * m_iEncodedHeight);
    if (!destBuffer) {
        return NULL;
    }

    destCaps = create_RGB_caps(destType, m_iWidth, m_iHeight, m_iEncodedWidth, m_iEncodedHeight, stride);
    if (!destCaps) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }
    gst_buffer_set_caps(destBuffer, destCaps);
    gst_caps_unref(destCaps);

    // copy buffer info
    GST_BUFFER_TIMESTAMP(destBuffer) = GST_BUFFER_TIMESTAMP(m_pBuffer);
    GST_BUFFER_OFFSET(destBuffer) = GST_BUFFER_OFFSET(m_pBuffer);
    GST_BUFFER_DURATION(destBuffer) = GST_BUFFER_DURATION(m_pBuffer);

    // now do the conversion
    if (destType == ARGB) {
        if (m_bHasAlpha) {
            status = ColorConvert_YCbCr420p_to_ARGB32(
                        GST_BUFFER_DATA(destBuffer), stride,
                        m_iEncodedWidth, m_iEncodedHeight,
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[v_index],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[u_index],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[3],
                        m_piPlaneStrides[0], m_piPlaneStrides[v_index],
                        m_piPlaneStrides[u_index], m_piPlaneStrides[3]);
        } else {
            status = ColorConvert_YCbCr420p_to_ARGB32_no_alpha(
                        GST_BUFFER_DATA(destBuffer), stride,
                        m_iEncodedWidth, m_iEncodedHeight,
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[v_index],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[u_index],
                        m_piPlaneStrides[0], m_piPlaneStrides[v_index],
                        m_piPlaneStrides[u_index]);
        }
    } else {
        if (m_bHasAlpha) {
            status = ColorConvert_YCbCr420p_to_BGRA32(
                        GST_BUFFER_DATA(destBuffer), stride,
                        m_iEncodedWidth, m_iEncodedHeight,
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[v_index],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[u_index],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[3],
                        m_piPlaneStrides[0], m_piPlaneStrides[v_index],
                        m_piPlaneStrides[u_index], m_piPlaneStrides[3]);
        } else {
            status = ColorConvert_YCbCr420p_to_BGRA32_no_alpha(
                        GST_BUFFER_DATA(destBuffer), stride,
                        m_iEncodedWidth, m_iEncodedHeight,
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[v_index],
                        GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[u_index],
                        m_piPlaneStrides[0], m_piPlaneStrides[v_index],
                        m_piPlaneStrides[u_index]);
        }
    }

    if (0 == status && destBuffer) {
        CGstVideoFrame *newFrame = new CGstVideoFrame(destBuffer);
// INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer); // else we'll have a massive memory leak!
        return newFrame;
    }

    return NULL;
}

CGstVideoFrame *CGstVideoFrame::ConvertFromYCbCr422(FrameType destType)
{
    GstBuffer *destBuffer;
    GstCaps *destCaps;
    gint stride = m_iEncodedWidth * 4;
    int status = 1;

    // Not handling alpha ...
    if (m_bHasAlpha) {
        return NULL;
    }

    stride = ((stride + 15) & ~15); // round up to multiple of 16 bytes
    destBuffer = alloc_aligned_buffer(stride * m_iEncodedHeight);
    if (!destBuffer) {
        return NULL;
    }

    destCaps = create_RGB_caps(destType, m_iWidth, m_iHeight, m_iEncodedWidth, m_iEncodedHeight, stride);
    if (!destCaps) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }
    gst_buffer_set_caps(destBuffer, destCaps);
    gst_caps_unref(destCaps);

    // copy buffer info
    GST_BUFFER_TIMESTAMP(destBuffer) = GST_BUFFER_TIMESTAMP(m_pBuffer);
    GST_BUFFER_OFFSET(destBuffer) = GST_BUFFER_OFFSET(m_pBuffer);
    GST_BUFFER_DURATION(destBuffer) = GST_BUFFER_DURATION(m_pBuffer);

    // now do the conversion
    if (destType == ARGB) {
        status = ColorConvert_YCbCr422p_to_ARGB32_no_alpha(GST_BUFFER_DATA(destBuffer), stride,
                                                           m_iEncodedWidth, m_iEncodedHeight,
                                                           GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0] + 1,
                                                           GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0] + 2,
                                                           GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0],
                                                           m_piPlaneStrides[0], m_piPlaneStrides[0]);
    } else {
        status = ColorConvert_YCbCr422p_to_BGRA32_no_alpha(GST_BUFFER_DATA(destBuffer), stride,
                                                           m_iEncodedWidth, m_iEncodedHeight,
                                                           GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0] + 1,
                                                           GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0] + 2,
                                                           GST_BUFFER_DATA(m_pBuffer) + m_piPlaneOffsets[0],
                                                           m_piPlaneStrides[0], m_piPlaneStrides[0]);
    }

    if (0 == status && destBuffer) {
        CGstVideoFrame *newFrame = new CGstVideoFrame(destBuffer);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer); // else we'll have a massive memory leak!
        return newFrame;
    }

    return NULL;
}

CGstVideoFrame *CGstVideoFrame::ConvertSwapRGB(FrameType destType)
{
    GstBuffer *destBuffer;
    GstCaps *srcCaps, *dstCaps;
    GstStructure* str;
    gint xx, yy;
    guint32 *srcData, *dstData;

    destBuffer = alloc_aligned_buffer(GST_BUFFER_SIZE(m_pBuffer));
    if (!destBuffer) {
        return NULL;
    }

    // Create and set buffer caps for the new format
    srcCaps = gst_buffer_get_caps(m_pBuffer);
    dstCaps = gst_caps_copy(srcCaps); // Should make caps writable
    gst_caps_unref(srcCaps);
    str = gst_caps_get_structure(dstCaps, 0);

    // all we need to change is alpha_mask, red_mask, green_mask and blue_mask
    switch (destType) {
        case ARGB:
            gst_structure_set(str,
                    "red_mask",   G_TYPE_INT, 0x00FF0000,
                    "green_mask", G_TYPE_INT, 0x0000FF00,
                    "blue_mask",  G_TYPE_INT, 0x000000FF,
                    "alpha_mask", G_TYPE_INT, 0xFF000000,
                    NULL);
            break;
        case BGRA_PRE:
            gst_structure_set(str,
                    "red_mask",   G_TYPE_INT, 0x0000FF00,
                    "green_mask", G_TYPE_INT, 0x00FF0000,
                    "blue_mask",  G_TYPE_INT, 0xFF000000,
                    "alpha_mask", G_TYPE_INT, 0x000000FF,
                    NULL);
            break;
        default:
            // shouldn't have gotten this far...
// INLINE - gst_buffer_unref()
            gst_buffer_unref(destBuffer);
            gst_caps_unref(dstCaps);
            return NULL;
    }
    gst_buffer_set_caps(destBuffer, dstCaps);
    gst_caps_unref(dstCaps);

    // Now copy data from src to dest, byteswapping as we copy
    srcData = (guint32*)GST_BUFFER_DATA(m_pBuffer);
    dstData = (guint32*)GST_BUFFER_DATA(destBuffer);
    if (!(m_piPlaneStrides[0] & 3)) {
        // four byte alignment on the entire buffer, we can just loop once
        for (xx = 0; xx < GST_BUFFER_SIZE(m_pBuffer); xx += 4) {
            *dstData++ = swap_uint32(*srcData++); // NOTE: SSE could be used here instead
        }
    } else {
        for (yy = 0; yy < m_iHeight; yy++) {
            for (xx = 0; xx < m_iWidth; xx++) {
                dstData[xx] = swap_uint32(srcData[xx]); // NOTE: SSE could be used here instead
            }
            dstData += m_piPlaneStrides[0];
            srcData += m_piPlaneStrides[0];
        }
    }

    if (destBuffer) {
        CGstVideoFrame *newFrame = new CGstVideoFrame(destBuffer);
// INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer); // else we'll have a massive memory leak!
        return newFrame;
    }
    return NULL;
}
