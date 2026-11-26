/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12TextureUploader.hpp"

#include "D3D12Utils.hpp"


namespace D3D12 {
namespace Internal {

// assumes both source and target have the same BPP
void TextureUploader::TransferDirect()
{
    size_t bpp = GetPixelFormatBPP(mSource.format);

    for (size_t y = 0; y < mSource.h; ++y)
    {
        const uint8_t* srcPtr = reinterpret_cast<const uint8_t*>(mSource.ptr) + ((y + mSource.y) * mSource.stride) + (mSource.x * bpp);
        uint8_t* dstPtr = reinterpret_cast<uint8_t*>(mTarget.ptr) + (y * mTarget.stride);

        memcpy(dstPtr, srcPtr, mSource.w * bpp);
    }
}

void TextureUploader::TransferA8ToB8G8R8A8()
{
    size_t srcBpp = GetPixelFormatBPP(mSource.format);
    size_t srcStrideElems = mSource.stride / srcBpp;
    size_t dstStrideElems = mTarget.stride / GetDXGIFormatBPP(mTarget.format);

    for (size_t y = 0; y < mSource.h; ++y)
    {
        const uint8_t* srcPtr = reinterpret_cast<const uint8_t*>(mSource.ptr) + ((y + mSource.y) * srcStrideElems) + (mSource.x * srcBpp);
        Pixel_RGBA8_UNORM* dstPtr = reinterpret_cast<Pixel_RGBA8_UNORM*>(mTarget.ptr) + (y * dstStrideElems);
        for (size_t x = 0; x < mSource.w; ++x)
        {
            dstPtr[x].r = dstPtr[x].g = dstPtr[x].b = 0;
            dstPtr[x].a = srcPtr[x];
        }
    }
}

void TextureUploader::TransferRGBToB8G8R8A8()
{
    size_t srcBpp = GetPixelFormatBPP(mSource.format);
    size_t srcStrideElems = mSource.stride / srcBpp;
    size_t dstStrideElems = mTarget.stride / GetDXGIFormatBPP(mTarget.format);

    for (size_t y = 0; y < mSource.h; ++y)
    {
        const Pixel_RGB8_UNORM* srcPtr = reinterpret_cast<const Pixel_RGB8_UNORM*>(mSource.ptr) + ((y + mSource.y) * srcStrideElems) + (mSource.x);
        Pixel_BGRA8_UNORM* dstPtr = reinterpret_cast<Pixel_BGRA8_UNORM*>(mTarget.ptr) + (y * dstStrideElems);
        for (size_t x = 0; x < mSource.w; ++x)
        {
            dstPtr[x].r = srcPtr[x].r;
            dstPtr[x].g = srcPtr[x].g;
            dstPtr[x].b = srcPtr[x].b;
            dstPtr[x].a = 255;
        }
    }
}

TextureUploader::TextureUploader()
    : mSource{nullptr, 0, PixelFormat::INT_ARGB_PRE, 0, 0, 0, 0, 0}
    , mTarget{nullptr, 0, DXGI_FORMAT_UNKNOWN, 0}
{
}

TextureUploader::~TextureUploader()
{
}

size_t TextureUploader::EstimateTargetSize(size_t srcw, size_t srch, DXGI_FORMAT dstFormat)
{
    // D3D12 requires RowPitch aka. stride in PlacedFootprint structure to be a multiple of 256 bytes
    // If Stride is not a multiple of 256 we need to expand it
    // We assume srcStride has been calculated properly in upper layers (greater or equal to width * bpp)
    uint32_t bpp = GetDXGIFormatBPP(dstFormat);
    if (srcw == 0 || srch == 0 || bpp == 0) return 0;
    if (srcw > std::numeric_limits<uint32_t>::max() / bpp) return 0;

    size_t dstStride = srcw * GetDXGIFormatBPP(dstFormat);
    dstStride = Utils::Align<size_t>(dstStride, D3D12_TEXTURE_DATA_PITCH_ALIGNMENT);
    if (srch > std::numeric_limits<uint32_t>::max() / dstStride) return 0;

    return srch * dstStride;
}

void TextureUploader::SetSource(const void* ptr, size_t size, PixelFormat format, UINT x, UINT y, UINT w, UINT h, UINT stride)
{
    mSource.ptr = ptr;
    mSource.size = size;
    mSource.format = format;
    mSource.x = x;
    mSource.y = y;
    mSource.w = w;
    mSource.h = h;
    mSource.stride = stride;
}

void TextureUploader::SetTarget(void* ptr, size_t size, DXGI_FORMAT format)
{
    mTarget.ptr = ptr;
    mTarget.size = size;
    mTarget.format = format;

    // calculated on Upload() call
    mTarget.stride = 0;
}

bool TextureUploader::Upload()
{
    if (!mSource.ptr || !mTarget.ptr)
    {
        D3D12NI_LOG_ERROR("TextureUploader: Invalid source or target pointer");
        return false;
    }

    size_t srcBpp = GetPixelFormatBPP(mSource.format);
    if (srcBpp == 0)
    {
        D3D12NI_LOG_ERROR("TextureUploader: Unrecognized source pixel format");
        return false;
    }

    // validate source and target dimensions
    if (mSource.w == 0 || mSource.h == 0 || mSource.stride == 0 ||
        mSource.w > (std::numeric_limits<uint32_t>::max() / srcBpp) ||
        mSource.h > (std::numeric_limits<uint32_t>::max() / mSource.stride))
    {
        D3D12NI_LOG_ERROR("TextureUploader: Invalid source dimensions");
        return false;
    }

    if (mSource.x > (std::numeric_limits<uint32_t>::max() / srcBpp) ||
        mSource.x > (std::numeric_limits<uint32_t>::max() - mSource.w))
    {
        D3D12NI_LOG_ERROR("TextureUploader: Invalid source dimensions/stride");
        return false;
    }

    if ((mSource.y > 0 && mSource.stride > (std::numeric_limits<uint32_t>::max() / mSource.y)) ||
        (mSource.y > std::numeric_limits<uint32_t>::max() - mSource.h))
    {
        D3D12NI_LOG_ERROR("TextureUploader: Invalid source dimensions/stride");
        return false;
    }

    if ((mSource.w > (std::numeric_limits<uint32_t>::max() / srcBpp)) ||
        (mSource.h > (std::numeric_limits<uint32_t>::max() / mSource.stride)) ||
        (((mSource.h - 1) * mSource.stride) > std::numeric_limits<uint32_t>::max() - (mSource.x * srcBpp)) ||
        ((mSource.w * srcBpp) > std::numeric_limits<uint32_t>::max() - (mSource.x * srcBpp) - ((mSource.h - 1) * mSource.stride)) ||
        ((mSource.y * mSource.stride) > std::numeric_limits<uint32_t>::max() - (mSource.x * srcBpp) - ((mSource.h - 1) * mSource.stride) - (mSource.w * srcBpp)) ||
        ((mSource.y * mSource.stride) + (mSource.x * srcBpp) + ((mSource.h - 1) * mSource.stride) + (mSource.w * srcBpp)) > mSource.size)
    {
        D3D12NI_LOG_ERROR("TextureUploader: Source would read out of buffer bounds");
        return false;
    }

    size_t targetBytes = EstimateTargetSize(mSource.w, mSource.h, mTarget.format);
    if (targetBytes == 0 || targetBytes > mTarget.size)
    {
        D3D12NI_LOG_ERROR("TextureUploader: Target is invalid");
        return false;
    }

    mTarget.stride = Utils::Align<UINT>(mSource.w * GetDXGIFormatBPP(mTarget.format), D3D12_TEXTURE_DATA_PITCH_ALIGNMENT);

    // mostly mimics D3D's TextureUploader.cc
    switch (mSource.format)
    {
    case PixelFormat::BYTE_GRAY:
    case PixelFormat::BYTE_ALPHA:
    {
        switch (mTarget.format)
        {
        case DXGI_FORMAT_A8_UNORM:
        case DXGI_FORMAT_R8_UNORM:
            TransferDirect();
            break;
        case DXGI_FORMAT_B8G8R8A8_UNORM:
            TransferA8ToB8G8R8A8();
            break;
        default:
            D3D12NI_LOG_ERROR("TextureUploader: Transfer from BYTE_GRAY or BYTE_ALPHA to DXGI format %u not supported", mTarget.format);
            return false;
        }
        break;
    }
    case PixelFormat::BYTE_RGB:
    {
        switch (mTarget.format)
        {
        case DXGI_FORMAT_B8G8R8A8_UNORM:
        case DXGI_FORMAT_B8G8R8X8_UNORM:
            TransferRGBToB8G8R8A8();
            break;
        default:
            D3D12NI_LOG_ERROR("TextureUploader: Transfer from BYTE_RGB to DXGI format %u not supported", mTarget.format);
            return false;
        }
        break;
    }
    case PixelFormat::INT_ARGB_PRE:
    case PixelFormat::BYTE_BGRA_PRE:
    {
        switch (mTarget.format)
        {
        case DXGI_FORMAT_B8G8R8A8_UNORM:
        case DXGI_FORMAT_B8G8R8X8_UNORM:
            TransferDirect();
            break;
        default:
            D3D12NI_LOG_ERROR("TextureUploader: Transfer from INT_ARGB_PRE or BYTE_BGRA_PRE to DXGI format %u not supported", mTarget.format);
            return false;
        }
        break;
    }
    case PixelFormat::FLOAT_XYZW:
    {
        switch (mTarget.format)
        {
        case DXGI_FORMAT_R32G32B32A32_FLOAT:
            TransferDirect();
            break;
        default:
            D3D12NI_LOG_ERROR("TextureUploader: Transfer from FLOAT_XYZW to DXGI format %u not supported", mTarget.format);
            return false;
        }
        break;
    }
    default:
        D3D12NI_LOG_ERROR("TextureUploader: Unknown or not supported source format %u", mSource.format);
        return false;
    }

    return true;
}

} // namespace Internal
} // namespace D3D12
