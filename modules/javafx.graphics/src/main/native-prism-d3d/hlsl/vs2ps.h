/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

static const int numLights = 3;

/*
 * The output of the vertex shader is the input of the pixel shader.
 */
typedef struct PsInput {

    // projection space = homogeneous clip space
    float4 projPos : position; // must be outputed even if unused

//  needed for pixel lighting
//  float3 worldPos           : texcoord1;

    float3 worldVecToEye                 : texcoord2;
    float3 worldVecsToLights[numLights]  : texcoord3; // 3, 4, 5
    float3 worldNormLightDirs[numLights] : texcoord6; // 6, 7, 8

//  needed for pixel lighting
//  float3 worldNormals[3] : texcoord3; // 3, 4, 5

    float2 texD : texcoord0;

//  float  oFog  : fog;
//  float3 debug : texcoord11;
} VsOutput;
