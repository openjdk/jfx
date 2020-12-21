/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef Spec
    #define Spec 0
#endif

#ifndef Bump
    #define Bump 0
#endif

#ifndef IllumMap
    #define IllumMap 0
#endif


static const bool bump = Bump;
static const int nSpecular = Spec;
static const bool isIlluminated = IllumMap;


float NTSC_Gray(float3 color) {
    return dot(color, float3(0.299, 0.587, 0.114));
}

float3 getBumpNormal(float3 bumpMap, float3 N[3]) {
    return bumpMap.z*N[0]+bumpMap.x*N[1]+bumpMap.y*N[2];
}

float4 retNormal(float3 n) { return float4( n*0.5+0.5,1); }

float4 retr(float x) { return float4(x.xxx,1); }

void phong(
    float3 n, float3 e, float power, in float4 L[LocalBump::nLights],
    in out float3 d, in out float3 s, int _s, int _e)
{
    float3 refl = reflect(e, n);
    for (int i = _s; i < _e; i++) {
        float dist = length(L[i].xyz);
        if (dist <= gLightRange[i].x) {
            float ca = gLightAttenuation[i].x;
            float la = gLightAttenuation[i].y;
            float qa = gLightAttenuation[i].z;
            float3 attenuatedColor = gLightColor[i].xyz / (ca + la * dist + qa * dist * dist);

            float3 l = normalize(L[i].xyz);
            d += saturate(dot(n, l)) * attenuatedColor;
            s += pow(saturate(dot(-refl, l)), power) * attenuatedColor;
        }
    }
}
