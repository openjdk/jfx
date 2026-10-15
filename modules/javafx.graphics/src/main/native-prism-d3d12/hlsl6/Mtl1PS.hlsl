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

#include "psMath.hlsl"
#include "ShaderCommon.hlsl"

// lights number (not specular)
#ifndef Spec
    #define Spec 0
#endif

#ifndef SType
    #define SType 0
#endif

#ifndef Bump
    #define Bump 0
#endif

#ifndef IllumMap
    #define IllumMap 0
#endif

#define SpecNone 0
#define SpecTexture 1
#define SpecColor 2
#define SpecMix 3

static const int numShaderLights = Spec;
static const int specType = SType;
static const bool bump = Bump;
static const bool isIlluminated = IllumMap;


// sampler states and texture mapping
SamplerState samplerDiffuse    : register(s0);
SamplerState samplerSpecular   : register(s1);
SamplerState samplerBumpHeight : register(s2);
SamplerState samplerSelfIllum  : register(s3);
Texture2D mapDiffuse           : register(t0);
Texture2D mapSpecular          : register(t1);
Texture2D mapBumpHeight        : register(t2);
Texture2D mapSelfIllum         : register(t3);

float4 debug() {
    return float4(0,0,1,1);
}

/**
 * Below Root Signature has following entries and refers to following shader resources:
 *     ConstantBuffer<VSData> gData; idx = 0
 *     ConstantBuffer<PSColorSpec> gColorSpec idx = 1
 *     ConstantBuffer<VSLightSpec> gLightSpec[numLights]; idx = 2
 *     ConstantBuffer<PSLightSpec> gLightSpec[numLights]; idx = 3
 *     [ mapDiffuse, mapSpecular, mapBumpHeight, mapSelfIllum]; idx = 4
 *     [ samplerDiffuse, samplerSpecular, samplerBumpHeight, samplerSelfIllum]; idx = 5
 */
[RootSignature(JFX_INTERNAL_GRAPHICS_RS)]
float4 main(PsInput psInput) : SV_TARGET0 {

    if (0) return debug();
    // return retNormal(psInput.debug);

    float2 texD = psInput.texD;

    // diffuse
    float4 tDiff = mapDiffuse.Sample(samplerDiffuse, texD);
    if (tDiff.a == 0.0) discard;
    tDiff = tDiff * gColor.diffuse;

    // return gColor.diffuse.aaaa;

    float3 normal = float3(0, 0, 1);

    // bump
    if (bump) {
        float4 BumpSpec = mapBumpHeight.Sample(samplerBumpHeight, texD);
        normal = normalize(BumpSpec.xyz * 2 - 1);
    }

    // specular
    float4 tSpec = float4(0, 0, 0, 0);
    float specPower = 0;

    if (specType > 0) {
        specPower = gColor.specular.a;
        if (specType != SpecColor) { // Texture or Mix
            tSpec = mapSpecular.Sample(samplerSpecular, texD);
            specPower *= NTSC_Gray(tSpec.rgb);
        } else { // Color
            tSpec.rgb = gColor.specular.rgb;
        }
        if (specType == SpecMix) {
            tSpec.rgb *= gColor.specular.rgb;
        }
    }
    // return sPower.xxxx;

    // lighting
    float3 worldNormVecToEye = normalize(psInput.worldVecToEye);
    float3 refl = reflect(worldNormVecToEye, normal);
    float3 diffLightColor = 0;
    float3 specLightColor = 0;

    for (int i = 0; i < numShaderLights; i++) {
        //  needed for pixel lighting
        //  float3 worldVecToLight = gLightsPos[i].xyz - worldPixelPos;
        //  worldVecToLight = getLocalVector(worldVecToLight, worldNormals);
        //
        //  float3 worldNormLightDir = gLightsNormDirs[i].xyz;
        //  worldNormLightDir = getLocalVector(worldNormLightDir, worldNormals); // renormalize?
        computeLight(i, normal, refl, specPower, psInput.worldVecsToLights[i], psInput.worldNormLightDirs[i], diffLightColor, specLightColor);
    }

    float3 ambLightColor = gColor.ambientLight.rgb;

    float3 rez = (ambLightColor + diffLightColor) * tDiff.rgb + specLightColor * tSpec.rgb;

    // self-illumination
    if (isIlluminated) {
        rez += mapSelfIllum.Sample(samplerSelfIllum, texD).rgb;
    }

    return float4(saturate(rez), tDiff.a);
}
