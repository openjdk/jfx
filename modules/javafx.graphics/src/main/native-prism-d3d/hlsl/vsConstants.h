/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

// see ShaderConstants.h cpp header

static const int MAX_BONES = 70;

#ifndef Skin
    #define Skin 0
#endif

static const int isSkinned = Skin;

struct Light {
    float4 pos;
    float4 color;
};

// See D3DPhongShader.h for register assignments

// camera
float4x4    mViewProj   : register(c0);
float4      gCameraPos  : register(c4);


float4      gReserved5[5] : register(c5);

// lighting
Light       sLights[5]        : register(c10);
float4      gLightsNormDir[5] : register(c20);
float4      gAmbinet          : register(c25);
float4      gAmbinetData[10]  : register(c25);

// world
float4x3    mWorld            : register(c35);
float4x3    mBones[MAX_BONES] : register(c35);

float4      gReserved245[11] : register(c245);
