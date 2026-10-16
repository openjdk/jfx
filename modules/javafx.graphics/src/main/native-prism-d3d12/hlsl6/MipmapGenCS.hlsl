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

/*
 * This shader is based on a similar shader from DirectX Graphics Samples MiniEngine
 * For more details see:
 *   https://github.com/microsoft/DirectX-Graphics-Samples/blob/master/MiniEngine/Core/Shaders/GenerateMipsCS.hlsli
 *
 * Performs up to 4 downsamples of a source texture and generates a (partial) mipmap chain.
 * To generate a full mipmap chain, this has to be dispatched in a loop from source tex
 * all the way to final 1x1 mip. See D3D12::NativeDevice::GenerateMipmaps() for dispatch details.
 */

#include "ShaderCommon.hlsl"

RWTexture2D<float4> mipmapTex1: register(u0);
RWTexture2D<float4> mipmapTex2: register(u1);
RWTexture2D<float4> mipmapTex3: register(u2);
RWTexture2D<float4> mipmapTex4: register(u3);
Texture2D<float4> sourceTex: register(t0);
SamplerState samplerTex: register(s0);

struct MipmapGenData
{
    uint sourceLevel;
    uint numLevels;
    float2 texelSizeMip1;
};

ConstantBuffer<MipmapGenData> gData: register(b0);

// temporary storage for calculated mip on each level
// we dispatch the shader with 8x8 threads, so cache should have 64 slots
// this is to reduce sampling sourceTex after we do one mipmap level
groupshared float4 tmpmip[64];

// helpers to make reading/writing to tmpmip* easier
uint GetTempMipIdx(uint x, uint y)
{
    return x + (y * 8);
}

void StoreTempMip(uint i, float4 color)
{
    tmpmip[i] = color;
}

float4 GetTempMip(uint i)
{
    return tmpmip[i];
}


[RootSignature(JFX_INTERNAL_COMPUTE_RS)]
[numthreads(8, 8, 1)]
void main(uint groupIdx: SV_GroupIndex, uint3 tid: SV_DispatchThreadID) {
    // Mip level 1
    // First level always assumes non-power-of-2 source texture
    // TODO D3D12: could be optimized for some dispatches in the chain
    //      we would have to check if first mip is power-of-2 or not (also depending
    //      on width/height) which would let us reduce SampleLevel() calls.

    // figure out UVs and calculate mip based on 4 samples
    float2 uv1 = gData.texelSizeMip1 * (tid.xy + float2(0.25, 0.25));
    float4 mip = 0.0f;
    bool inRange = ((gData.texelSizeMip1.x * tid.x) <= 1.0f) && ((gData.texelSizeMip1.y * tid.y) <= 1.0f);
    if (inRange)
    {
        float2 texelOffset = gData.texelSizeMip1 * 0.5;
        mip = sourceTex.SampleLevel(samplerTex, uv1, gData.sourceLevel);
        mip += sourceTex.SampleLevel(samplerTex, uv1 + float2(texelOffset.x, 0.0), gData.sourceLevel);
        mip += sourceTex.SampleLevel(samplerTex, uv1 + float2(0.0, texelOffset.y), gData.sourceLevel);
        mip += sourceTex.SampleLevel(samplerTex, uv1 + texelOffset, gData.sourceLevel);

        // average out the sample and store it as first mipmap
        mip *= 0.25f;
        mipmapTex1[tid.xy] = mip;
        StoreTempMip(groupIdx, mip);
    }

    // if we do only 1 mipmap on this dispatch leave
    // otherwise wait for other threads to get to this point
    if (gData.numLevels == 1) return;
    GroupMemoryBarrierWithGroupSync();

    // Mip level 2
    // Instead of re-sampling sourceTex we will refer to Store/GetTempMip() from now on
    // mip2 is half of mip1, only let even X/Y threads through (bitmask is 001001)
    if (inRange && (groupIdx & 0x9) == 0)
    {
        // we already have the first sample from above, it's mip1
        // other source samples were done by other threads
        float4 mip2 = GetTempMip(groupIdx + GetTempMipIdx(1, 0));
        float4 mip3 = GetTempMip(groupIdx + GetTempMipIdx(0, 1));
        float4 mip4 = GetTempMip(groupIdx + GetTempMipIdx(1, 1));

        // average out above 4 samples
        mip = 0.25f * (mip + mip2 + mip3 + mip4);
        mipmapTex2[tid.xy / 2] = mip;
        StoreTempMip(groupIdx, mip);
    }

    if (gData.numLevels == 2) return;
    GroupMemoryBarrierWithGroupSync();

    // Mip level 3
    // Like above, but now we check for X/Y being a multiple of 4 (bitmask 011011)
    if (inRange && (groupIdx & 0x1B) == 0)
    {
        float4 mip2 = GetTempMip(groupIdx + GetTempMipIdx(2, 0));
        float4 mip3 = GetTempMip(groupIdx + GetTempMipIdx(0, 2));
        float4 mip4 = GetTempMip(groupIdx + GetTempMipIdx(2, 2));

        mip = 0.25f * (mip + mip2 + mip3 + mip4);
        mipmapTex3[tid.xy / 4] = mip;
        StoreTempMip(groupIdx, mip);
    }

    if (gData.numLevels == 3) return;
    GroupMemoryBarrierWithGroupSync();

    // Mip level 4
    // Here only one (first) thread should have a group ID being multiple of 8 in both dimensions
    if (inRange && groupIdx == 0)
    {
        float4 mip2 = GetTempMip(groupIdx + GetTempMipIdx(4, 0));
        float4 mip3 = GetTempMip(groupIdx + GetTempMipIdx(0, 4));
        float4 mip4 = GetTempMip(groupIdx + GetTempMipIdx(4, 4));

        mip = 0.25f * (mip + mip2 + mip3 + mip4);
        mipmapTex4[tid.xy / 8] = mip;
    }
}
