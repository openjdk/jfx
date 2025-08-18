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
#include "PhongVSDecl.h"
#include "PhongVS2PS.h"
using namespace metal;

void quatToMatrix(float4 q, float3 N[3]) {
    float3 t1 = q.xyz * q.yzx * 2;
    float3 t2 = q.zxy * q.www * 2;
    float3 t3 = q.xyz * q.xyz * 2;
    float3 t4 = 1 - (t3 + t3.yzx);

    float3 r1 = t1 + t2;
    float3 r2 = t1 - t2;

    N[0] = float3(t4.y, r1.x, r2.z);
    N[1] = float3(r2.x, t4.z, r1.y);
    N[2] = float3(r1.z, r2.y, t4.x);

    N[2] *= (q.w >= 0) ? 1.0f : -1.0f;   // ATI normal map generator compatibility
}

float3 getLocalVector(float3 global, float3 N[3]) {
    return float3(dot(global, N[1]), dot(global, N[2]), dot(global, N[0]));
}

[[vertex]] VS_PHONG_INOUT0 PhongVS0(const uint v_id [[ vertex_id ]],
                      constant VS_PHONG_INPUT * v_in [[ buffer(0) ]],
                      constant VS_PHONG_UNIFORMS & vsUniforms [[ buffer(1) ]])
{
    VS_PHONG_INOUT0 out;
    out.texCoord = v_in[v_id].texCoord;
    float4 worldVertexPos = vsUniforms.world_matrix * (float4(v_in[v_id].position, 1.0));

    out.position = vsUniforms.mvp_matrix * worldVertexPos;

    float3 n[3];
    quatToMatrix(v_in[v_id].normal, n);
    float3x3 sWorldMatrix = float3x3(vsUniforms.world_matrix[0].xyz,
                                     vsUniforms.world_matrix[1].xyz,
                                     vsUniforms.world_matrix[2].xyz);
    for (int i = 0; i != 3; ++i) {
        n[i] = sWorldMatrix * n[i];
    }

    float3 worldVecToEye = vsUniforms.cameraPos.xyz - worldVertexPos.xyz;
    out.worldVecToEye = getLocalVector(worldVecToEye, n);

    return out;
}

[[vertex]] VS_PHONG_INOUT1 PhongVS1(const uint v_id [[ vertex_id ]],
                      constant VS_PHONG_INPUT * v_in [[ buffer(0) ]],
                      constant VS_PHONG_UNIFORMS & vsUniforms [[ buffer(1) ]])
{
    VS_PHONG_INOUT1 out;
    out.texCoord = v_in[v_id].texCoord;
    float4 worldVertexPos = vsUniforms.world_matrix * (float4(v_in[v_id].position, 1.0));

    out.position = vsUniforms.mvp_matrix * worldVertexPos;

    float3 n[3];
    quatToMatrix(v_in[v_id].normal, n);
    float3x3 sWorldMatrix = float3x3(vsUniforms.world_matrix[0].xyz,
                                     vsUniforms.world_matrix[1].xyz,
                                     vsUniforms.world_matrix[2].xyz);
    for (int i = 0; i != 3; ++i) {
        n[i] = sWorldMatrix * n[i];
    }

    float3 worldVecToEye = vsUniforms.cameraPos.xyz - worldVertexPos.xyz;
    out.worldVecToEye = getLocalVector(worldVecToEye, n);

    float3 worldVecToLight = vsUniforms.lightsPosition[0] - worldVertexPos.xyz;
    out.worldVecsToLights1 = getLocalVector(worldVecToLight, n);
    out.worldNormLightDirs1 = getLocalVector(vsUniforms.lightsNormDirection[0], n);

    return out;
}

[[vertex]] VS_PHONG_INOUT2 PhongVS2(const uint v_id [[ vertex_id ]],
                      constant VS_PHONG_INPUT * v_in [[ buffer(0) ]],
                      constant VS_PHONG_UNIFORMS & vsUniforms [[ buffer(1) ]])
{
    VS_PHONG_INOUT2 out;
    out.texCoord = v_in[v_id].texCoord;
    float4 worldVertexPos = vsUniforms.world_matrix * (float4(v_in[v_id].position, 1.0));

    out.position = vsUniforms.mvp_matrix * worldVertexPos;

    float3 n[3];
    quatToMatrix(v_in[v_id].normal, n);
    float3x3 sWorldMatrix = float3x3(vsUniforms.world_matrix[0].xyz,
                                     vsUniforms.world_matrix[1].xyz,
                                     vsUniforms.world_matrix[2].xyz);
    for (int i = 0; i != 3; ++i) {
        n[i] = sWorldMatrix * n[i];
    }

    float3 worldVecToEye = vsUniforms.cameraPos.xyz - worldVertexPos.xyz;
    out.worldVecToEye = getLocalVector(worldVecToEye, n);

    float3 worldVecToLight = vsUniforms.lightsPosition[0] - worldVertexPos.xyz;
    out.worldVecsToLights1 = getLocalVector(worldVecToLight, n);
    out.worldNormLightDirs1 = getLocalVector(vsUniforms.lightsNormDirection[0], n);

    worldVecToLight = vsUniforms.lightsPosition[1] - worldVertexPos.xyz;
    out.worldVecsToLights2 = getLocalVector(worldVecToLight, n);
    out.worldNormLightDirs2 = getLocalVector(vsUniforms.lightsNormDirection[1], n);

    return out;
}

[[vertex]] VS_PHONG_INOUT3 PhongVS3(const uint v_id [[ vertex_id ]],
                      constant VS_PHONG_INPUT * v_in [[ buffer(0) ]],
                      constant VS_PHONG_UNIFORMS & vsUniforms [[ buffer(1) ]])
{
    VS_PHONG_INOUT3 out;
    out.texCoord = v_in[v_id].texCoord;
    float4 worldVertexPos = vsUniforms.world_matrix * (float4(v_in[v_id].position, 1.0));

    out.position = vsUniforms.mvp_matrix * worldVertexPos;

    float3 n[3];
    quatToMatrix(v_in[v_id].normal, n);
    float3x3 sWorldMatrix = float3x3(vsUniforms.world_matrix[0].xyz,
                                     vsUniforms.world_matrix[1].xyz,
                                     vsUniforms.world_matrix[2].xyz);
    for (int i = 0; i != 3; ++i) {
        n[i] = sWorldMatrix * n[i];
    }

    float3 worldVecToEye = vsUniforms.cameraPos.xyz - worldVertexPos.xyz;
    out.worldVecToEye = getLocalVector(worldVecToEye, n);

    float3 worldVecToLight = vsUniforms.lightsPosition[0] - worldVertexPos.xyz;
    out.worldVecsToLights1 = getLocalVector(worldVecToLight, n);
    out.worldNormLightDirs1 = getLocalVector(vsUniforms.lightsNormDirection[0], n);

    worldVecToLight = vsUniforms.lightsPosition[1] - worldVertexPos.xyz;
    out.worldVecsToLights2 = getLocalVector(worldVecToLight, n);
    out.worldNormLightDirs2 = getLocalVector(vsUniforms.lightsNormDirection[1], n);

    worldVecToLight = vsUniforms.lightsPosition[2] - worldVertexPos.xyz;
    out.worldVecsToLights3 = getLocalVector(worldVecToLight, n);
    out.worldNormLightDirs3 = getLocalVector(vsUniforms.lightsNormDirection[2], n);

    return out;
}
