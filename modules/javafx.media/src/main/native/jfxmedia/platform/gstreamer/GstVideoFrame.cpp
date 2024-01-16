/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

static void free_aligned_buffer(gpointer ptr)
{
    if (ptr != NULL) {
        g_free(ptr);
    }
}

static GstBuffer *alloc_aligned_buffer(guint size)
{
    // allocate a new GstBuffer of the given size plus some for padding and alignment
    guint8 *newData;
    guint8 *alignedData;
    guint alignedSize;

    // allocate a buffer large enough to accommodate 16 byte alignment
    alignedSize = size;
    if (size <= (G_MAXUINT - 16)) {
        size += 16;
    } else {
        return NULL;
    }

    newData = (guint8*)g_try_malloc(size);
    if (NULL == newData) {
        return NULL;
    }

    alignedData = (guint8*)(((intptr_t)newData + 15) & ~15);

    return gst_buffer_new_wrapped_full((GstMemoryFlags)0, alignedData, alignedSize, 0, alignedSize, newData, free_aligned_buffer);
}

GstCaps *create_RGB_caps(CVideoFrame::FrameType type, guint width, guint height, guint encodedWidth, guint encodedHeight, guint stride)
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
    } else {
        return NULL; // unsupported format..
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

CGstVideoFrame::CGstVideoFrame()
{
    m_bIsValid = false;
    m_pSample = NULL;
    m_pBuffer = NULL;
    m_bIsI420 = false;
}

CGstVideoFrame::~CGstVideoFrame()
{
    LOWLEVELPERF_COUNTERDEC("CGstVideoFrame", 1, 1);

    if (NULL != m_pBuffer)
        Dispose();
}

bool CGstVideoFrame::Init(GstSample* sample)
{
    LOWLEVELPERF_COUNTERINC("CGstVideoFrame", 1, 1);

    // Increment the ref count as this object will be created
    // by the video sink and pushed into the FrameQueue.
    m_pSample = gst_sample_ref(sample);
    m_pBuffer = gst_sample_get_buffer(m_pSample);
    if (m_pBuffer == NULL) {
        return false;
    }

    if (!gst_buffer_map(m_pBuffer, &m_Info, GST_MAP_READ)) {
        m_pBuffer = NULL;
        return false;
    }

    m_ulBufferSize = (unsigned long)m_Info.size;
    m_pvBufferBaseAddress = (void*)m_Info.data;

    if (GST_BUFFER_TIMESTAMP_IS_VALID (m_pBuffer)) {
        m_dTime = (double)GST_BUFFER_TIMESTAMP(m_pBuffer) / GST_SECOND;
    } else {
        m_dTime = 0.0;
        m_bIsValid = false;
    }

    GstCaps* caps = gst_sample_get_caps(m_pSample);
    if (caps == NULL) {
        return false;
    }

    SetFrameCaps(caps);

    return true;
}

void CGstVideoFrame::SetFrameCaps(GstCaps *newCaps)
{
    const GstStructure* str = gst_caps_get_structure(newCaps, 0);
    const gchar* sFormatFourCC = gst_structure_get_string(str, "format");

    // We should always start with success. See CalcSize(), AddSize() and
    // CalcPlanePointer() on how this flag is being used.
    m_bIsValid = true;

    // FIXME: make format type strings conformant with constant types
    if (gst_structure_has_name(str, "video/x-raw-yvua420p")) {
        m_typeFrame = YCbCr_420p;
        m_bHasAlpha = true;
    } else if (gst_structure_has_name(str, "video/x-raw-ycbcr422")) {
        m_typeFrame = YCbCr_422;
        m_bHasAlpha = false;
    } else if (gst_structure_has_name(str, "video/x-raw-yuv")) {
        if (sFormatFourCC != NULL && g_ascii_strcasecmp(sFormatFourCC, FOURCC_UYVY) == 0) {
            m_typeFrame = YCbCr_422;
        } else {
            if (sFormatFourCC != NULL && g_ascii_strcasecmp(sFormatFourCC, FOURCC_I420) == 0) {
                m_bIsI420 = true;
            }
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

    if (!gst_structure_get_int(str, "width", (int*)&m_uiWidth))
    {
#if JFXMEDIA_DEBUG
        g_warning("width could not be retrieved from GstBuffer\n");
#endif
        m_uiWidth = 0;
        m_bIsValid = false;
    }
    if (!gst_structure_get_int(str, "height", (int*)&m_uiHeight))
    {
#if JFXMEDIA_DEBUG
        g_warning("height could not be retrieved from GstBuffer\n");
#endif
        m_uiHeight = 0;
        m_bIsValid = false;
    }

    if (!gst_structure_get_int(str, "encoded-width", (int*)&m_uiEncodedWidth)) {
        m_uiEncodedWidth = m_uiWidth;
    }
    if (!gst_structure_get_int(str, "encoded-height", (int*)&m_uiEncodedHeight)) {
        m_uiEncodedHeight = m_uiHeight;
    }

    Reset();

    switch (m_typeFrame) {
        case YCbCr_420p: {
            unsigned int offset = 0;
            SetPlaneCount(3);

            if (!gst_structure_get_int(str, "stride-y", (int*)&m_puiPlaneStrides[0])) {
                m_puiPlaneStrides[0] = m_uiEncodedWidth;
            }
            if (!gst_structure_get_int(str, "stride-v", (int*)&m_puiPlaneStrides[1])) {
                m_puiPlaneStrides[1] = m_uiEncodedWidth/2;
            }
            if (!gst_structure_get_int(str, "stride-u", (int*)&m_puiPlaneStrides[2])) {
                m_puiPlaneStrides[2] = m_puiPlaneStrides[1];
            }

            gst_structure_get_int(str, "offset-y", (int*)&offset);
            m_pulPlaneSize[0] = CalcSize(m_puiPlaneStrides[0], m_uiEncodedHeight, &m_bIsValid);
            m_pvPlaneData[0] = CalcPlanePointer((intptr_t)m_pvBufferBaseAddress, offset,
                                                m_pulPlaneSize[0], m_ulBufferSize, &m_bIsValid);

            //
            // Chroma offsets assume YV12 ordering
            //
            offset += m_pulPlaneSize[0];
            gst_structure_get_int(str, "offset-v", (int*)&offset);
            m_pulPlaneSize[1] = CalcSize(m_puiPlaneStrides[1], (m_uiEncodedHeight/2), &m_bIsValid);
            m_pvPlaneData[1] = CalcPlanePointer((intptr_t)m_pvBufferBaseAddress, offset,
                                                m_pulPlaneSize[1], m_ulBufferSize, &m_bIsValid);

            offset += m_pulPlaneSize[1];
            gst_structure_get_int(str, "offset-u", (int*)&offset);
            m_pulPlaneSize[2] = CalcSize(m_puiPlaneStrides[2], (m_uiEncodedHeight/2), &m_bIsValid);
            m_pvPlaneData[2] = CalcPlanePointer((intptr_t)m_pvBufferBaseAddress, offset,
                                                m_pulPlaneSize[2], m_ulBufferSize, &m_bIsValid);

            // process alpha channel (before we potentially swap Cb/Cr)
            if (m_bHasAlpha) {
                SetPlaneCount(GetPlaneCount() + 1);
                if (!gst_structure_get_int(str, "stride-a", (int*)&m_puiPlaneStrides[3])) {
                    m_puiPlaneStrides[3] = m_puiPlaneStrides[0];
                }

                offset += m_pulPlaneSize[2];
                gst_structure_get_int(str, "offset-a", (int*)&offset);
                m_pulPlaneSize[3] = CalcSize(m_puiPlaneStrides[3], m_uiEncodedHeight, &m_bIsValid);
                m_pvPlaneData[3] = CalcPlanePointer((intptr_t)m_pvBufferBaseAddress, offset,
                                                m_pulPlaneSize[3], m_ulBufferSize, &m_bIsValid);
            }

            //
            // Swap chroma planes for I420.
            //
            if (m_bIsI420) {
                // Swap Cb/Cr plane data
                SwapPlanes(1, 2);
            }
            break;
        }

        default:
            SetPlaneCount(1);
            if (!gst_structure_get_int(str, "line_stride", (int*)&m_puiPlaneStrides[0])) {
                if (m_typeFrame == YCbCr_422) {
                    m_puiPlaneStrides[0] = m_uiEncodedWidth * 2; // 16 bpp
                } else {
                    m_puiPlaneStrides[0] = m_uiEncodedWidth * 4; // 32 bpp
                }
            }
            m_pulPlaneSize[0] = CalcSize(m_puiPlaneStrides[0], m_uiEncodedHeight, &m_bIsValid);
            m_pvPlaneData[0] = CalcPlanePointer((intptr_t)m_pvBufferBaseAddress, 0,
                                                m_pulPlaneSize[0], m_ulBufferSize, &m_bIsValid);
            break;
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

    if (m_pBuffer != NULL) {
        gst_buffer_unmap(m_pBuffer, &m_Info);
        m_pBuffer = NULL;
    }

    if (m_pSample != NULL) {
        // INLINE - gst_sample_unref()
        gst_sample_unref(m_pSample);
        m_pSample = NULL;
    }
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
    GstSample *destSample = NULL;
    GstBuffer *destBuffer = NULL;
    GstCaps *destCaps = NULL;
    GstMapInfo info;
    guint stride = 0;
    guint alloc_size = 0;
    unsigned int u_index, v_index = 0;
    int status = 0;

    if (m_bIsI420) {
        u_index = 1;
        v_index = 2;
    } else {
        u_index = 2;
        v_index = 1;
    }

    // Make sure we do not have an integer overflow
    if (m_uiEncodedWidth <= (G_MAXUINT / 4)) {
        stride = m_uiEncodedWidth * 4;
    } else {
        return NULL;
    }

    if (stride <= (G_MAXUINT - 16)) {
        stride = ((stride + 15) & ~15); // round up to multiple of 16 bytes
    } else {
        return NULL;
    }

    if (m_uiEncodedHeight > 0 && stride <= (G_MAXUINT / m_uiEncodedHeight)) {
        alloc_size = stride * m_uiEncodedHeight;
    } else {
        return NULL;
    }

    destBuffer = alloc_aligned_buffer(alloc_size);
    if (!destBuffer) {
        return NULL;
    }

    // copy buffer info
    GST_BUFFER_TIMESTAMP(destBuffer) = GST_BUFFER_TIMESTAMP(m_pBuffer);
    GST_BUFFER_OFFSET(destBuffer) = GST_BUFFER_OFFSET(m_pBuffer);
    GST_BUFFER_DURATION(destBuffer) = GST_BUFFER_DURATION(m_pBuffer);

    if (!gst_buffer_map(destBuffer, &info, GST_MAP_WRITE)) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    // now do the conversion
    if (destType == ARGB) {
        if (m_bHasAlpha) {
            status = ColorConvert_YCbCr420p_to_ARGB32(
                        info.data, stride,
                        m_uiEncodedWidth, m_uiEncodedHeight,
                        (const uint8_t*)m_pvPlaneData[0],
                        (const uint8_t*)m_pvPlaneData[v_index],
                        (const uint8_t*)m_pvPlaneData[u_index],
                        (const uint8_t*)m_pvPlaneData[3],
                        m_puiPlaneStrides[0], m_puiPlaneStrides[v_index],
                        m_puiPlaneStrides[u_index], m_puiPlaneStrides[3]);
        } else {
            status = ColorConvert_YCbCr420p_to_ARGB32_no_alpha(
                        info.data, stride,
                        m_uiEncodedWidth, m_uiEncodedHeight,
                        (const uint8_t*)m_pvPlaneData[0],
                        (const uint8_t*)m_pvPlaneData[v_index],
                        (const uint8_t*)m_pvPlaneData[u_index],
                        m_puiPlaneStrides[0], m_puiPlaneStrides[v_index],
                        m_puiPlaneStrides[u_index]);
        }
    } else {
        if (m_bHasAlpha) {
            status = ColorConvert_YCbCr420p_to_BGRA32(
                        info.data, stride,
                        m_uiEncodedWidth, m_uiEncodedHeight,
                        (const uint8_t*)m_pvPlaneData[0],
                        (const uint8_t*)m_pvPlaneData[v_index],
                        (const uint8_t*)m_pvPlaneData[u_index],
                        (const uint8_t*)m_pvPlaneData[3],
                        m_puiPlaneStrides[0], m_puiPlaneStrides[v_index],
                        m_puiPlaneStrides[u_index], m_puiPlaneStrides[3]);
        } else {
            status = ColorConvert_YCbCr420p_to_BGRA32_no_alpha(
                        info.data, stride,
                        m_uiEncodedWidth, m_uiEncodedHeight,
                        (const uint8_t*)m_pvPlaneData[0],
                        (const uint8_t*)m_pvPlaneData[v_index],
                        (const uint8_t*)m_pvPlaneData[u_index],
                        m_puiPlaneStrides[0], m_puiPlaneStrides[v_index],
                        m_puiPlaneStrides[u_index]);
        }
    }

    gst_buffer_unmap(destBuffer, &info);

    destCaps = create_RGB_caps(destType, m_uiWidth, m_uiHeight, m_uiEncodedWidth, m_uiEncodedHeight, stride);
    if (!destCaps) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    destSample = gst_sample_new(destBuffer, destCaps, NULL, NULL);
    if (!destSample) {
        gst_caps_unref(destCaps);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    gst_caps_unref(destCaps);

    if (0 == status && destSample) {
        CGstVideoFrame *newFrame = new CGstVideoFrame();
        bool result = newFrame->Init(destSample) && newFrame->IsValid();
        // INLINE - gst_sample_unref()
        gst_buffer_unref(destBuffer); // else we'll have a massive memory leak!
        // INLINE - gst_sample_unref()
        gst_sample_unref(destSample); // else we'll have a massive memory leak!
        if (result) {
            return newFrame;
        } else {
            delete newFrame;
        }
    }

    return NULL;
}

CGstVideoFrame *CGstVideoFrame::ConvertFromYCbCr422(FrameType destType)
{
    GstSample *destSample;
    GstBuffer *destBuffer;
    GstCaps *destCaps;
    GstMapInfo info;
    guint stride = 0;
    guint alloc_size = 0;
    int status = 0;

    // Not handling alpha ...
    if (m_bHasAlpha) {
        return NULL;
    }

    // Make sure we do not have an integer overflow
    if (m_uiEncodedWidth <= (G_MAXUINT / 4)) {
        stride = m_uiEncodedWidth * 4;
    } else {
        return NULL;
    }

    if (stride <= (G_MAXUINT - 16)) {
        stride = ((stride + 15) & ~15); // round up to multiple of 16 bytes
    } else {
        return NULL;
    }

    if (m_uiEncodedHeight > 0 && stride <= (G_MAXUINT / m_uiEncodedHeight)) {
        alloc_size = stride * m_uiEncodedHeight;
    } else {
        return NULL;
    }

    destBuffer = alloc_aligned_buffer(alloc_size);
    if (!destBuffer) {
        return NULL;
    }

    // copy buffer info
    GST_BUFFER_TIMESTAMP(destBuffer) = GST_BUFFER_TIMESTAMP(m_pBuffer);
    GST_BUFFER_OFFSET(destBuffer) = GST_BUFFER_OFFSET(m_pBuffer);
    GST_BUFFER_DURATION(destBuffer) = GST_BUFFER_DURATION(m_pBuffer);

    if (!gst_buffer_map(destBuffer, &info, GST_MAP_WRITE)) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    // now do the conversion
    if (destType == ARGB) {
        status = ColorConvert_YCbCr422p_to_ARGB32_no_alpha(info.data, stride,
                                                           m_uiEncodedWidth, m_uiEncodedHeight,
                                                           (uint8_t*)m_pvPlaneData[0] + 1,
                                                           (uint8_t*)m_pvPlaneData[0] + 2,
                                                           (uint8_t*)m_pvPlaneData[0],
                                                           m_puiPlaneStrides[0], m_puiPlaneStrides[0]);
    } else {
        status = ColorConvert_YCbCr422p_to_BGRA32_no_alpha(info.data, stride,
                                                           m_uiEncodedWidth, m_uiEncodedHeight,
                                                           (uint8_t*)m_pvPlaneData[0] + 1,
                                                           (uint8_t*)m_pvPlaneData[0] + 2,
                                                           (uint8_t*)m_pvPlaneData[0],
                                                           m_puiPlaneStrides[0], m_puiPlaneStrides[0]);
    }

    gst_buffer_unmap(destBuffer, &info);

    destCaps = create_RGB_caps(destType, m_uiWidth, m_uiHeight, m_uiEncodedWidth, m_uiEncodedHeight, stride);
    if (!destCaps) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    destSample = gst_sample_new(destBuffer, destCaps, NULL, NULL);
    if (!destSample) {
        gst_caps_unref(destCaps);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    gst_caps_unref(destCaps);

    if (0 == status && destBuffer) {
        CGstVideoFrame *newFrame = new CGstVideoFrame();
        bool result = newFrame->Init(destSample) && newFrame->IsValid();
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer); // else we'll have a massive memory leak!
        // INLINE - gst_sample_unref()
        gst_sample_unref(destSample); // else we'll have a massive memory leak!
        if (result) {
            return newFrame;
        } else {
            delete newFrame;
        }
    }

    return NULL;
}

CGstVideoFrame *CGstVideoFrame::ConvertSwapRGB(FrameType destType)
{
    GstSample *destSample;
    GstBuffer *destBuffer;
    GstCaps *srcCaps, *dstCaps;
    GstMapInfo srcInfo, destInfo;
    GstStructure* str;
    guint xx, yy, size;
    guint32 *srcData, *dstData;

    size = gst_buffer_get_size(m_pBuffer);

    destBuffer = alloc_aligned_buffer(size);
    if (!destBuffer) {
        return NULL;
    }

    // Create and set buffer caps for the new format
    srcCaps = gst_sample_get_caps(m_pSample);
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

    destSample = gst_sample_new(destBuffer, dstCaps, NULL, NULL);
    if (!destSample) {
        gst_caps_unref(dstCaps);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        return NULL;
    }

    gst_caps_unref(dstCaps);

    if (!gst_buffer_map(m_pBuffer, &srcInfo, GST_MAP_READ)) {
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        // INLINE - gst_sample_unref()
        gst_sample_unref(destSample);
        return NULL;
    }

    if (!gst_buffer_map(destBuffer, &destInfo, GST_MAP_WRITE)) {
        gst_buffer_unmap(m_pBuffer, &srcInfo);
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer);
        // INLINE - gst_sample_unref()
        gst_sample_unref(destSample);
        return NULL;
    }

    // Now copy data from src to dest, byteswapping as we copy
    srcData = (guint32*)srcInfo.data;
    dstData = (guint32*)destInfo.data;
    if (!(m_puiPlaneStrides[0] & 3)) {
        // four byte alignment on the entire buffer, we can just loop once
        for (xx = 0; xx < size; xx += 4) {
            *dstData++ = swap_uint32(*srcData++); // NOTE: SSE could be used here instead
        }
    } else {
        for (yy = 0; yy < m_uiHeight; yy++) {
            for (xx = 0; xx < m_uiWidth; xx++) {
                dstData[xx] = swap_uint32(srcData[xx]); // NOTE: SSE could be used here instead
            }
            dstData += m_puiPlaneStrides[0];
            srcData += m_puiPlaneStrides[0];
        }
    }

    gst_buffer_unmap(m_pBuffer, &srcInfo);
    gst_buffer_unmap(destBuffer, &destInfo);

    if (destBuffer) {
        CGstVideoFrame *newFrame = new CGstVideoFrame();
        bool result = newFrame->Init(destSample) && newFrame->IsValid();
        // INLINE - gst_buffer_unref()
        gst_buffer_unref(destBuffer); // else we'll have a massive memory leak!
        // INLINE - gst_sample_unref()
        gst_sample_unref(destSample); // else we'll have a massive memory leak!
        if (result) {
            return newFrame;
        } else {
            delete newFrame;
        }
    }
    return NULL;
}
