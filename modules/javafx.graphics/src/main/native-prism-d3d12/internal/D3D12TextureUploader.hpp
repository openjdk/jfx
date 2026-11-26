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

#pragma once

#include "D3D12Common.hpp"


namespace D3D12 {
namespace Internal {

class TextureUploader
{
    struct
    {
        const void* ptr;
        size_t size;
        PixelFormat format;
        UINT x;
        UINT y;
        UINT w;
        UINT h;
        UINT stride;
    } mSource;
    struct
    {
        void* ptr;
        size_t size;
        DXGI_FORMAT format;
        // below are calculated after Upload() succeeds
        UINT stride;
    } mTarget;

    void TransferDirect();
    void TransferA8ToB8G8R8A8();
    void TransferRGBToB8G8R8A8();

public:
    TextureUploader();
    ~TextureUploader();

    static size_t EstimateTargetSize(size_t srcw, size_t srch, DXGI_FORMAT dstFormat);

    void SetSource(const void* ptr, size_t size, PixelFormat format, UINT x, UINT y, UINT w, UINT h, UINT stride);
    void SetTarget(void* ptr, size_t size, DXGI_FORMAT format);
    bool Upload();

    inline UINT GetTargetStride() const
    {
        return mTarget.stride;
    }

    inline DXGI_FORMAT GetTargetFormat() const
    {
        return mTarget.format;
    }
};

} // namespace Internal
} // namespace D3D12
