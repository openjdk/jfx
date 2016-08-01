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

// lets make colorizer happy

#ifdef __cplusplus
    struct float2 {
        float x,y;
        float2() {}; float2(float,float) {}
    };
    struct float3 {
        float x,y,z;
        float3() {}; float3(float,float,float) {}

        float3 operator - ();
        float3 operator += (float3);
        float3 operator *= (float);
    };

    float3 operator * (float, float3);
    float3 operator * (float3, float);
    float3 operator / (float3, float);
    float3 operator * (float3, float3);
    float3 operator + (float3, float3);
    float3 operator - (float3, float3);
    float3 operator + (float3, float);

    struct float4 {
        float4() {}; float4(float,float,float,float) {} float4(float3, float) {}
        float x,y,z,w; float3 xyz;
    };

    struct float3x3 {};
    struct float4x3 {};

    float4x3 operator * (float4x3, float);

    template<class t> t mul(t, float4x3);

    struct sampler {};

    template <class t> float dot(t, t);
    float3 reflect(float3, float3);
    template <class t> t normalize(t);
    template <class t> t saturate(t);
    template <class t> t pow(t, t);
    template <class t> float dot(t, t);
    template <class t> float dot(t, float);
    template <class t> float length(t);

    float4 lit(float, float, float);

    float4 tex2D(sampler, float2);

    #define in
    #define out

    struct int4 {
        int operator [] (int);
    };

    int4 D3DCOLORtoUBYTE4(float4);

#endif
