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
#include "../D3D12Constants.hpp"

#include <array>
#include <string>


namespace D3D12 {
namespace Internal {

struct SamplerDesc
{
    TextureWrapMode wrapMode = TextureWrapMode::CLAMP_NOT_NEEDED;
    bool isLinear = false;

    static constexpr uint32_t TOTAL_BITS = 3;

    std::string ToString() const
    {
        std::string wrapModeStr;
        switch (wrapMode)
        {
        case TextureWrapMode::CLAMP_NOT_NEEDED: wrapModeStr = "CLAMP_NOT_NEEDED"; break;
        case TextureWrapMode::CLAMP_TO_ZERO: wrapModeStr = "CLAMP_TO_ZERO"; break;
        case TextureWrapMode::CLAMP_TO_EDGE: wrapModeStr = "CLAMP_TO_EDGE"; break;
        case TextureWrapMode::REPEAT: wrapModeStr = "REPEAT"; break;
        default: wrapModeStr = "UNKNOWN"; break;
        }

        return "wrapMode = " + wrapModeStr + "; isLinear = " + std::to_string(isLinear);
    }

    inline bool operator==(const SamplerDesc& other) const
    {
        return wrapMode == other.wrapMode &&
               isLinear == other.isLinear;
    }

    inline bool operator!=(const SamplerDesc& other) const
    {
        return !operator==(other);
    }
};

using SamplerDescArray = std::array<SamplerDesc, Constants::MAX_TEXTURE_UNITS>;

} // namespace Internal
} // namespace D3D12

template<>
struct std::hash<D3D12::Internal::SamplerDesc>
{
    std::size_t operator()(const D3D12::Internal::SamplerDesc& k) const
    {
        // NOTE: When adding fields to SamplerDesc also check ResourceManager
        // We combine those there into SamplerBindingIdentifier which relies
        // on bit count assumed here.
        return
            (static_cast<uint32_t>(k.isLinear) << 2) |
            (static_cast<uint32_t>(k.wrapMode));
    }
};
