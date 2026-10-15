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

#include "D3D12NativeTexture.hpp"


namespace D3D12 {

class NativePhongMaterial
{
    NIPtr<NativeDevice> mNativeDevice;
    Pixel_RGBA32_FLOAT mDiffuseColor;
    bool mSpecularColorSet;
    Pixel_RGBA32_FLOAT mSpecularColor;
    NIPtr<NativeTexture> mMaps[static_cast<size_t>(TextureMapType::MAX_ENUM)];

public:
    NativePhongMaterial(const NIPtr<NativeDevice>& nativeDevice);

    bool Init();
    void SetDiffuseColor(float r, float g, float b, float a);
    void SetSpecularColor(bool set, float r, float g, float b, float a);
    void ClearTextureMap(TextureMapType type);
    void SetTextureMap(const NIPtr<NativeTexture>& map, TextureMapType type);

    inline const Pixel_RGBA32_FLOAT GetDiffuseColor() const
    {
        return mDiffuseColor;
    }

    inline const Pixel_RGBA32_FLOAT GetSpecularColor() const
    {
        return mSpecularColor;
    }

    inline const NIPtr<NativeTexture>& GetMap(TextureMapType map) const
    {
        return mMaps[static_cast<size_t>(map)];
    }

    inline bool IsBumpMap() const
    {
        return static_cast<bool>(GetMap(TextureMapType::BUMP));
    }

    inline bool IsSpecularMap() const
    {
        return static_cast<bool>(GetMap(TextureMapType::SPECULAR));
    }

    inline bool IsSelfIllum() const
    {
        return static_cast<bool>(GetMap(TextureMapType::SELF_ILLUM));
    }

    inline PhongShaderSpecularVariant GetSpecularVariant() const
    {
        if (IsSpecularMap())
        {
            return mSpecularColorSet ? PhongShaderSpecularVariant::MIX : PhongShaderSpecularVariant::TEXTURE;
        }

        return mSpecularColorSet ? PhongShaderSpecularVariant::COLOR : PhongShaderSpecularVariant::NONE;
    }

    inline PhongShaderMappingVariant GetMappingVariant() const
    {
        return IsBumpMap() ? PhongShaderMappingVariant::BUMP : PhongShaderMappingVariant::SIMPLE;
    }
};

} // namespace D3D12
