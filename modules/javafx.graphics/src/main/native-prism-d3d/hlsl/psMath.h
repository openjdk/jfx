/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

#include "devColor.h"

#include "psConstants.h"
#include "vs2ps.h"


// needed for pixel lighting
//float3 getLocalVector(float3 global, float3 N[3]) {
//    return float3(dot(global, N[1]), dot(global, N[2]), dot(global, N[0]));
//}

float NTSC_Gray(float3 color) {
    return dot(color, float3(0.299, 0.587, 0.114));
}

//float3 getBumpNormal(float3 bumpMap, float3 N[3]) {
//    return bumpMap.z * N[0] + bumpMap.x * N[1] + bumpMap.y * N[2];
//}

//float4 retNormal(float3 n) { return float4(n * 0.5 + 0.5, 1); }

//float4 retr(float x) { return float4(x.xxx, 1); }


// Because pow(0, 0) is undefined (https://docs.microsoft.com/en-us/windows/win32/direct3dhlsl/dx-graphics-hlsl-pow),
// we need special treatment for falloff == 0 cases

/*
 * The methods 'computeSpotlightFactor' and 'computeSpotlightFactor2' are alternatives to the 'computeSpotlightFactor3'
 * method that was chosen for its better performance. They are kept for the case that a future changes will make them
 * more performant.
 *
float computeSpotlightFactor(float3 l, float3 lightDir, float cosOuter, float denom, float falloff) {
    if (falloff == 0 && cosOuter == -1) { // point light optimization (cosOuter == -1 is outerAngle == 180)
        return 1;
    }
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    if (falloff != 0) {
        return pow(saturate(cutoff / denom), falloff);
    }
    return cutoff >= 0 ? 1 : 0;
}

float computeSpotlightFactor2(float3 l, float3 lightDir, float cosOuter, float denom, float falloff) {
    if (falloff != 0) {
        float cosAngle = dot(normalize(-lightDir), l);
        float cutoff = cosAngle - cosOuter;
        return pow(saturate(cutoff / denom), falloff);
    }
    if (cosOuter == -1) {  // point light optimization (cosOuter == -1 is outerAngle == 180)
        return 1;
    }
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    return cutoff >= 0 ? 1 : 0;
}
*/

float computeSpotlightFactor3(float3 l, float3 lightDir, float cosOuter, float denom, float falloff) {
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    if (falloff != 0) {
        return pow(saturate(cutoff / denom), falloff);
    }
    return cutoff >= 0 ? 1 : 0;
}

/*
 * Computes the light's contribution by using the Phong shading model. A contribution consists of a diffuse component and a
 * specular component. The computation is done in world space.
 */
void computeLight(float i, float3 normal, float3 refl, float specPower, float3 toLight, float3 lightDir, in out float3 diff, in out float3 spec) {
    // testing if w is 0 or 1 using <0.5 since equality check for floating points might not work well
    if (gLightAttenuation[i].w < 0.5) {
        diff += saturate(dot(normal, -lightDir)) * gLightColor[i].rgb;
        spec += pow(saturate(dot(-refl, -lightDir)), specPower) * gLightColor[i].rgb;
        return;
    }

    float dist = length(toLight);
    if (dist > gLightRange[i].x) {
        return;
    }
    float3 l = normalize(toLight);

    float cosOuter = gSpotLightFactors[i].x;
    float denom = gSpotLightFactors[i].y;
    float falloff = gSpotLightFactors[i].z;
    float spotlightFactor = computeSpotlightFactor3(l, lightDir, cosOuter, denom, falloff);

    float ca = gLightAttenuation[i].x;
    float la = gLightAttenuation[i].y;
    float qa = gLightAttenuation[i].z;
    float invAttnFactor = ca + la * dist + qa * dist * dist;

    float3 attenuatedColor = gLightColor[i].rgb * spotlightFactor / invAttnFactor;
    diff += saturate(dot(normal, l)) * attenuatedColor;
    spec += pow(saturate(dot(-refl, l)), specPower) * attenuatedColor;
}
