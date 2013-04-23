/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "psMath.h"

#ifndef SType
    #define SType 0
#endif

#define SpecNone 0
#define SpecAuto 1
#define SpecAlpha 2

static const int specType = SType;

// sampler mapping see
sampler mapDiffuse    : register(s0);
sampler mapSpecular   : register(s1);
sampler mapBumpHeight : register(s2);
sampler mapSelfIllum  : register(s3);

float autoSpecular (float3 diffuseRGB, float3 specRGB) {
    return NTSC_Gray(specRGB);
}

float4 debug() {
    return float4(0,0,1,1);
}

float4 main(ObjectPsIn objAttr, LocalBump  lSpace) : color {

    if (0) return debug();
    // return retNormal(lSpace.debug);

    float3 nEye = normalize(lSpace.eye);

    float3 n = float3(0,0,1);

    if (bump) {
        float4 BumpSpec = tex2D(mapBumpHeight, objAttr.texD);
        n = normalize(BumpSpec.xyz*2-1);
    }

    float4 ambColor = objAttr.ambient;

    float4 tDiff = tex2D(mapDiffuse, objAttr.texD);
    tDiff.xyz = lerp(tDiff.xyz, gConstantColor.xyz, gConstantColor.a);

    // return gConstantColor.aaaa;

    float4 tSpec = float4(0,0,0,0);
    float sLevel = 0;

    if ( specType > 0 ) {
        tSpec = tex2D(mapSpecular, objAttr.texD);
        sLevel = (specType==SpecAuto) ? autoSpecular(tDiff.xyz, tSpec.rgb) : tSpec.a;
    }
    // return sLevel.xxxx;

    float3 diff = 0;
    float3 spec = 0;

    phong(n, nEye, sLevel, lSpace.lights, diff, spec, 0, nSpecular);

    float3 rez = (ambColor.xyz+diff)*tDiff.xyz + spec*tSpec.rgb;

    if (isIlluminated)
        rez += tex2D(mapSelfIllum, objAttr.texD).xyz;

    return float4( saturate(rez), tDiff.a);
}

