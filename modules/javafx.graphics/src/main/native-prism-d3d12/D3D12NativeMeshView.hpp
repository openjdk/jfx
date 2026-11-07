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

#include "D3D12Constants.hpp"
#include "D3D12NativeMesh.hpp"
#include "D3D12NativePhongMaterial.hpp"

#include <array>


namespace D3D12 {

class NativeMeshView
{
public:
    template <typename T>
    using LightDataArray = std::array<T, Constants::MAX_LIGHTS>;

private:
    struct SpotLightFactorsRaw
    {
        float innerAngle;
        float outerAngle;
        float falloff;
    };

    NIPtr<NativeDevice> mNativeDevice;
    NIPtr<NativeMesh> mMesh;
    D3D12_CULL_MODE mCullMode;
    D3D12_FILL_MODE mFillMode;
    NIPtr<NativePhongMaterial> mMaterial;
    LightDataArray<bool> mLightEnabled;
    LightDataArray<SpotLightFactorsRaw> mLightSpotFactors;

    // below are shader structs which will memcpy-ed on rendering
    // they have to match shader-side structures of the same name
    LightDataArray<VSLightSpec> mLightsVS;
    PSColorSpec mColorsPS;
    LightDataArray<PSLightSpec> mLightsPS;

    inline bool IsPointLight(int lightIndex)
    {
        return mLightSpotFactors[lightIndex].falloff == 0 &&
               mLightSpotFactors[lightIndex].outerAngle == 180 &&
               mLightsPS[lightIndex].attenuation.a > 0.5;
    }

    inline bool IsDirectionalLight(int lightIndex)
    {
        return mLightsPS[lightIndex].attenuation.a < 0.5f;
    }

public:
    NativeMeshView(const NIPtr<NativeDevice>& nativeDevice);
    ~NativeMeshView() = default;

    bool Init(const NIPtr<NativeMesh>& mesh);
    void SetCullingMode(CullFace mode);
    void SetMaterial(const NIPtr<NativePhongMaterial>& material);
    void SetWireframe(bool wireframe);
    void SetAmbientLight(float r, float g, float b);
    void SetLight(int index, float x, float y, float z, float r, float g, float b,
                  float enabled, float ca, float la, float qa, float isAttenuated, float maxRange,
                  float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff);

    inline const PSColorSpec& GetPSColorSpec()
    {
        // get diffuse and specular colors from the material
        mColorsPS.diffuse = mMaterial->GetDiffuseColor();
        mColorsPS.specular = mMaterial->GetSpecularColor();

        return mColorsPS;
    }

    inline bool IsLightEnabled(int index) const
    {
        return mLightEnabled[index];
    }

    inline D3D12_CULL_MODE GetCullMode() const
    {
        return mCullMode;
    }

    inline D3D12_FILL_MODE GetFillMode() const
    {
        return mFillMode;
    }

    inline const VSLightSpec* GetVSLightSpecPtr(uint32_t idx) const
    {
        return &mLightsVS[idx];
    }

    inline const PSLightSpec* GetPSLightSpecPtr(uint32_t idx) const
    {
        return &mLightsPS[idx];
    }

    inline uint32_t GetEnabledLightCount() const
    {
        uint32_t count = 0;
        for (uint32_t i = 0; i < Constants::MAX_LIGHTS; ++i)
            count += mLightEnabled[i] ? 1 : 0;
        return count;
    }

    inline const NIPtr<NativeMesh>& GetMesh() const
    {
        return mMesh;
    }

    inline const NIPtr<NativePhongMaterial>& GetMaterial() const
    {
        return mMaterial;
    }
};

} // namespace D3D12
