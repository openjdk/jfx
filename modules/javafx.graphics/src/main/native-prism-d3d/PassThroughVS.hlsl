/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

float4x4 WorldViewProj;

struct VS_INPUT
{
    float3 Pos : POSITION;
    float4 Diff : COLOR0;
    float2 TX1 : TEXCOORD0;
    float2 TX2 : TEXCOORD1;
};

struct VS_OUTPUT
{
    float4 Pos : POSITION;
    float4 Diff : COLOR0;
    float2 TX1 : TEXCOORD0;
    float2 TX2 : TEXCOORD1;
};

VS_OUTPUT passThrough(in VS_INPUT In)
{
    VS_OUTPUT Out;
    Out.Pos = mul(float4(In.Pos, 1.0), WorldViewProj);
        Out.Diff = In.Diff;
    Out.TX1 = In.TX1;
    Out.TX2 = In.TX2;
    return Out;
}
