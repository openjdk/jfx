/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include <metal_stdlib>
#include <simd/simd.h>
#include "PhongPSDecl.h"
#include "PhongVS2PS.h"
using namespace metal;

#define SPEC_NONE 0
#define SPEC_TEX 1
#define SPEC_CLR 2
#define SPEC_MIX 3

constexpr sampler mipmapSampler(filter::linear,
                            mip_filter::linear,
                               address::repeat);

constexpr sampler nonMipmapSampler(filter::linear,
                                  address::repeat);

float NTSC_Gray(float3 color) {
    return dot(color, float3(0.299f, 0.587f, 0.114f));
}

float computeSpotlightFactor3(float3 l, float3 lightDir, float cosOuter, float denom, float falloff) {
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    if (falloff != 0.0f) {
        return pow(saturate(cutoff / denom), falloff);
    }
    return cutoff >= 0.0f ? 1.0f : 0.0f;
}

[[fragment]] float4 PhongPS0(VS_PHONG_INOUT0 vert [[stage_in]],
                        constant PS_PHONG_UNIFORMS & psUniforms [[ buffer(0) ]],
                        texture2d<float> mapDiffuse [[ texture(0) ]],
                        texture2d<float> mapSpecular [[ texture(1) ]],
                        texture2d<float> mapBump [[ texture(2) ]],
                        texture2d<float> mapSelfIllum [[ texture(3) ]])
{
    float2 texD = vert.texCoord;

    float4 tDiff = mapDiffuse.sample(mipmapSampler, texD);
    if (tDiff.a == 0.0f) discard_fragment();
    tDiff = tDiff * psUniforms.diffuseColor;

    float3 normal = float3(0.0f, 0.0f, 1.0f);

    // bump
    if (psUniforms.isBumpMap) {
        float4 BumpSpec = mapBump.sample(nonMipmapSampler, texD);
        normal = normalize(BumpSpec.xyz * 2.0f - 1.0f);
    }

    float3 rez = psUniforms.ambientLightColor.rgb * tDiff.rgb;

    // self-illumination
    if (psUniforms.isIlluminated) {
        rez += mapSelfIllum.sample(mipmapSampler, texD).rgb;
    }

    return float4(saturate(rez), tDiff.a);
}

[[fragment]] float4 PhongPS1(VS_PHONG_INOUT1 vert [[stage_in]],
                        constant PS_PHONG_UNIFORMS & psUniforms [[ buffer(0) ]],
                        texture2d<float> mapDiffuse [[ texture(0) ]],
                        texture2d<float> mapSpecular [[ texture(1) ]],
                        texture2d<float> mapBump [[ texture(2) ]],
                        texture2d<float> mapSelfIllum [[ texture(3) ]])
{
    float2 texD = vert.texCoord;

    float4 tDiff = mapDiffuse.sample(mipmapSampler, texD);
    if (tDiff.a == 0.0f) discard_fragment();
    tDiff = tDiff * psUniforms.diffuseColor;

    float3 normal = float3(0.0f, 0.0f, 1.0f);
    // bump
    if (psUniforms.isBumpMap) {
        float4 BumpSpec = mapBump.sample(nonMipmapSampler, texD);
        normal = normalize(BumpSpec.xyz * 2.0f - 1.0f);
    }
    // specular
    float4 tSpec = 0.0f;
    float specPower = 32.0f;
    if (psUniforms.specType > 0) {
        specPower = psUniforms.specColor.a;
        if (psUniforms.specType != SPEC_CLR) { // Texture or Mix
            tSpec = mapSpecular.sample(nonMipmapSampler, texD);
            specPower *= NTSC_Gray(tSpec.rgb);
        } else { // Color
            tSpec.rgb = psUniforms.specColor.rgb;
        }
        if (psUniforms.specType == SPEC_MIX) {
            tSpec.rgb *= psUniforms.specColor.rgb;
        }
    }

    // lighting
    float3 worldNormVecToEye = normalize(vert.worldVecToEye);
    float3 refl = reflect(worldNormVecToEye, normal);
    float3 diffLightColor = 0.0f;
    float3 specLightColor = 0.0f;

    float3 lightColor = float3(psUniforms.lightsColor[0],
                               psUniforms.lightsColor[1],
                               psUniforms.lightsColor[2]);
    float4 lightAttenuation = float4(psUniforms.lightsAttenuation[0],
                                     psUniforms.lightsAttenuation[1],
                                     psUniforms.lightsAttenuation[2],
                                     psUniforms.lightsAttenuation[3]);
    float3 spotLightsFactor = float3(psUniforms.spotLightsFactors[0],
                                     psUniforms.spotLightsFactors[1],
                                     psUniforms.spotLightsFactors[2]);

    // Testing if w is 0 or 1 using < 0.5 since equality check
    // for floating points might not work well
    if (lightAttenuation.w < 0.5f) {
        diffLightColor += saturate(dot(normal, -vert.worldNormLightDirs1)) * lightColor;
        specLightColor += pow(saturate(dot(-refl, -vert.worldNormLightDirs1)), specPower) * lightColor;
    } else {
        float dist = length(vert.worldVecsToLights1);
        if (dist <= psUniforms.lightsRange[0]) {
            float3 l = normalize(vert.worldVecsToLights1);

            float cosOuter = spotLightsFactor.x;
            float denom = spotLightsFactor.y;
            float falloff = spotLightsFactor.z;
            float spotlightFactor = computeSpotlightFactor3(l, vert.worldNormLightDirs1, cosOuter, denom, falloff);

            float ca = lightAttenuation.x;
            float la = lightAttenuation.y;
            float qa = lightAttenuation.z;
            float invAttnFactor = ca + la * dist + qa * dist * dist;

            float3 attenuatedColor = lightColor * spotlightFactor / invAttnFactor;
            diffLightColor += saturate(dot(normal, l)) * attenuatedColor;
            specLightColor += pow(saturate(dot(-refl, l)), specPower) * attenuatedColor;
        }
    }

    float3 ambLightColor = psUniforms.ambientLightColor.rgb;

    float3 rez = (ambLightColor + diffLightColor) * tDiff.rgb + specLightColor * tSpec.rgb;

    // self-illumination
    if (psUniforms.isIlluminated) {
        rez += mapSelfIllum.sample(mipmapSampler, texD).rgb;
    }

    return float4(saturate(rez), tDiff.a);
}

[[fragment]] float4 PhongPS2(VS_PHONG_INOUT2 vert [[stage_in]],
                        constant PS_PHONG_UNIFORMS & psUniforms [[ buffer(0) ]],
                        texture2d<float> mapDiffuse [[ texture(0) ]],
                        texture2d<float> mapSpecular [[ texture(1) ]],
                        texture2d<float> mapBump [[ texture(2) ]],
                        texture2d<float> mapSelfIllum [[ texture(3) ]])
{
   float2 texD = vert.texCoord;

    float4 tDiff = mapDiffuse.sample(mipmapSampler, texD);
    if (tDiff.a == 0.0f) discard_fragment();
    tDiff = tDiff * psUniforms.diffuseColor;

    float3 normal = float3(0.0f, 0.0f, 1.0f);

    // bump
    if (psUniforms.isBumpMap) {
        float4 BumpSpec = mapBump.sample(nonMipmapSampler, texD);
        normal = normalize(BumpSpec.xyz * 2.0f - 1.0f);
    }
    // specular
    float4 tSpec = 0.0f;
    float specPower = 32.0f;
    if (psUniforms.specType > 0) {
        specPower = psUniforms.specColor.a;
        if (psUniforms.specType != SPEC_CLR) { // Texture or Mix
            tSpec = mapSpecular.sample(nonMipmapSampler, texD);
            specPower *= NTSC_Gray(tSpec.rgb);
        } else { // Color
            tSpec.rgb = psUniforms.specColor.rgb;
        }
        if (psUniforms.specType == SPEC_MIX) {
            tSpec.rgb *= psUniforms.specColor.rgb;
        }
    }

    // lighting
    float3 worldNormVecToEye = normalize(vert.worldVecToEye);
    float3 refl = reflect(worldNormVecToEye, normal);
    float3 diffLightColor = 0.0f;
    float3 specLightColor = 0.0f;

    for (int i = 0; i < psUniforms.numLights; i++) {

        float3 light;
        float3 lightDir;
        switch (i) {
            case 0 :
                light = vert.worldVecsToLights1;
                lightDir = vert.worldNormLightDirs1;
                break;
            case 1 :
                light = vert.worldVecsToLights2;
                lightDir = vert.worldNormLightDirs2;
                break;
        }
        float3 lightColor = float3(psUniforms.lightsColor[(i * 4)],
                                   psUniforms.lightsColor[(i * 4) + 1],
                                   psUniforms.lightsColor[(i * 4) + 2]);
        float4 lightAttenuation = float4(psUniforms.lightsAttenuation[(i * 4)],
                                         psUniforms.lightsAttenuation[(i * 4) + 1],
                                         psUniforms.lightsAttenuation[(i * 4) + 2],
                                         psUniforms.lightsAttenuation[(i * 4) + 3]);
        float3 spotLightsFactor = float3(psUniforms.spotLightsFactors[(i * 4)],
                                         psUniforms.spotLightsFactors[(i * 4) + 1],
                                         psUniforms.spotLightsFactors[(i * 4) + 2]);

        // Testing if w is 0 or 1 using < 0.5 since equality check
        // for floating points might not work well
        if (lightAttenuation.w < 0.5f) {
            diffLightColor += saturate(dot(normal, -lightDir)) * lightColor;
            specLightColor += pow(saturate(dot(-refl, -lightDir)), specPower) * lightColor;
        } else {
            float dist = length(light);
            if (dist <= psUniforms.lightsRange[(i * 4)]) {
                float3 l = normalize(light);

                float cosOuter = spotLightsFactor.x;
                float denom = spotLightsFactor.y;
                float falloff = spotLightsFactor.z;
                float spotlightFactor = computeSpotlightFactor3(l, lightDir, cosOuter, denom, falloff);

                float ca = lightAttenuation.x;
                float la = lightAttenuation.y;
                float qa = lightAttenuation.z;
                float invAttnFactor = ca + la * dist + qa * dist * dist;

                float3 attenuatedColor = lightColor * spotlightFactor / invAttnFactor;
                diffLightColor += saturate(dot(normal, l)) * attenuatedColor;
                specLightColor += pow(saturate(dot(-refl, l)), specPower) * attenuatedColor;
            }
        }
    }

    float3 ambLightColor = psUniforms.ambientLightColor.rgb;

    float3 rez = (ambLightColor + diffLightColor) * tDiff.rgb + specLightColor * tSpec.rgb;

    // self-illumination
    if (psUniforms.isIlluminated) {
        rez += mapSelfIllum.sample(mipmapSampler, texD).rgb;
    }

    return float4(saturate(rez), tDiff.a);
}

[[fragment]] float4 PhongPS3(VS_PHONG_INOUT3 vert [[stage_in]],
                        constant PS_PHONG_UNIFORMS & psUniforms [[ buffer(0) ]],
                        texture2d<float> mapDiffuse [[ texture(0) ]],
                        texture2d<float> mapSpecular [[ texture(1) ]],
                        texture2d<float> mapBump [[ texture(2) ]],
                        texture2d<float> mapSelfIllum [[ texture(3) ]])
{
    float2 texD = vert.texCoord;

    float4 tDiff = mapDiffuse.sample(mipmapSampler, texD);
    if (tDiff.a == 0.0f) discard_fragment();
    tDiff = tDiff * psUniforms.diffuseColor;

    float3 normal = float3(0.0f, 0.0f, 1.0f);

    // bump
    if (psUniforms.isBumpMap) {
        float4 BumpSpec = mapBump.sample(nonMipmapSampler, texD);
        normal = normalize(BumpSpec.xyz * 2.0f - 1.0f);
    }
    // specular
    float4 tSpec = 0.0f;
    float specPower = 32.0f;
    if (psUniforms.specType > 0) {
        specPower = psUniforms.specColor.a;
        if (psUniforms.specType != SPEC_CLR) { // Texture or Mix
            tSpec = mapSpecular.sample(nonMipmapSampler, texD);
            specPower *= NTSC_Gray(tSpec.rgb);
        } else { // Color
            tSpec.rgb = psUniforms.specColor.rgb;
        }
        if (psUniforms.specType == SPEC_MIX) {
            tSpec.rgb *= psUniforms.specColor.rgb;
        }
    }

    // lighting
    float3 worldNormVecToEye = normalize(vert.worldVecToEye);
    float3 refl = reflect(worldNormVecToEye, normal);
    float3 diffLightColor = 0.0f;
    float3 specLightColor = 0.0f;

    for (int i = 0; i < psUniforms.numLights; i++) {
        float3 light;
        float3 lightDir;
        switch (i) {
            case 0 :
                light = vert.worldVecsToLights1;
                lightDir = vert.worldNormLightDirs1;
                break;
            case 1 :
                light = vert.worldVecsToLights2;
                lightDir = vert.worldNormLightDirs2;
                break;
            case 2 :
                light = vert.worldVecsToLights3;
                lightDir = vert.worldNormLightDirs3;
                break;
        }
        float3 lightColor = float3(psUniforms.lightsColor[(i * 4)],
                                   psUniforms.lightsColor[(i * 4) + 1],
                                   psUniforms.lightsColor[(i * 4) + 2]);
        float4 lightAttenuation = float4(psUniforms.lightsAttenuation[(i * 4)],
                                         psUniforms.lightsAttenuation[(i * 4) + 1],
                                         psUniforms.lightsAttenuation[(i * 4) + 2],
                                         psUniforms.lightsAttenuation[(i * 4) + 3]);
        float3 spotLightsFactor = float3(psUniforms.spotLightsFactors[(i * 4)],
                                         psUniforms.spotLightsFactors[(i * 4) + 1],
                                         psUniforms.spotLightsFactors[(i * 4) + 2]);

        // Testing if w is 0 or 1 using < 0.5 since equality check
        // for floating points might not work well
        if (lightAttenuation.w < 0.5f) {
            diffLightColor += saturate(dot(normal, -lightDir)) * lightColor;
            specLightColor += pow(saturate(dot(-refl, -lightDir)), specPower) * lightColor;
        } else {
            float dist = length(light);
            if (dist <= psUniforms.lightsRange[(i * 4)]) {
                float3 l = normalize(light);

                float cosOuter = spotLightsFactor.x;
                float denom = spotLightsFactor.y;
                float falloff = spotLightsFactor.z;
                float spotlightFactor = computeSpotlightFactor3(l, lightDir, cosOuter, denom, falloff);

                float ca = lightAttenuation.x;
                float la = lightAttenuation.y;
                float qa = lightAttenuation.z;
                float invAttnFactor = ca + la * dist + qa * dist * dist;

                float3 attenuatedColor = lightColor * spotlightFactor / invAttnFactor;
                diffLightColor += saturate(dot(normal, l)) * attenuatedColor;
                specLightColor += pow(saturate(dot(-refl, l)), specPower) * attenuatedColor;
            }
        }
    }

    float3 ambLightColor = psUniforms.ambientLightColor.rgb;

    float3 rez = (ambLightColor + diffLightColor) * tDiff.rgb + specLightColor * tSpec.rgb;

    // self-illumination
    if (psUniforms.isIlluminated) {
        rez += mapSelfIllum.sample(mipmapSampler, texD).rgb;
    }

    return float4(saturate(rez), tDiff.a);
}
