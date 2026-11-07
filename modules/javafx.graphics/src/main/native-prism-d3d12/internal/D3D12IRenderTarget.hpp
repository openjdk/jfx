/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "../D3D12Common.hpp"

#include "D3D12BBox.hpp"
#include "D3D12DescriptorData.hpp"
#include "D3D12ITrackedResource.hpp"
#include "D3D12TextureBase.hpp"


namespace D3D12 {
namespace Internal {

// TODO: D3D12: Cleanup - this class should be renamed to "RenderTargetBase"
//              and many common parts of both NativeRenderTarget and NativeSwapChain
//              should be added here.
class IRenderTarget: public ITrackedResource
{
protected:
    enum class BBoxTrackingState
    {
        Disabled = 0,   // set when upper layers disable bbox tracking permanently
        Enabled,        // bbox tracking is enabled
        FrameDisabled,  // bbox tracking was disabled for current frame due to ex. bbox being more complex
    };

    BBox mDirtyBBox; // tracks how much of the RTT was "used" aka. rendered on
    BBoxTrackingState mBBoxState;

public:
    virtual const NIPtr<TextureBase>& GetTexture() const = 0;
    virtual const NIPtr<TextureBase>& GetDepthTexture() const = 0;
    virtual DXGI_FORMAT GetFormat() const = 0;
    virtual uint64_t GetWidth() const = 0;
    virtual uint64_t GetHeight() const = 0;
    virtual bool HasDepthTexture() const = 0; // separate from IsDepthTestEnabled() because of RenderingContext::Clear()
    virtual bool IsDepthTestEnabled() const = 0;
    virtual uint32_t GetMSAASamples() const = 0;
    virtual const Internal::DescriptorData& GetRTVDescriptorData() const = 0;
    virtual const Internal::DescriptorData& GetDSVDescriptorData() const = 0;

    void UpdateDirtyBBox(const BBox& box);

    inline void ResetDirtyBBox()
    {
        mDirtyBBox = BBox();
        if (mBBoxState != BBoxTrackingState::Disabled) mBBoxState = BBoxTrackingState::Enabled;
    }

    inline bool BBoxEnabled() const
    {
        return (mBBoxState == BBoxTrackingState::Enabled);
    }

    inline const BBox& GetDirtyBBox() const
    {
        return mDirtyBBox;
    }
};

} // namespace Internal
} // namespace D3D12
