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

#ifndef METAL_MESHVIEW_H
#define METAL_MESHVIEW_H

#import "MetalContext.h"
#import "MetalMesh.h"
#import "MetalLight.h"
#import "MetalPhongMaterial.h"

#define MAX_NUM_LIGHTS 3

typedef struct VS_PHONG_UNIFORMS {
    simd_float4x4 mvp_matrix;
    simd_float4x4 world_matrix;
    packed_float4 cameraPos;
    float lightsPosition[MAX_NUM_LIGHTS * 3];
    float lightsNormDirection[MAX_NUM_LIGHTS * 3];
    float numLights;
} VS_PHONG_UNIFORMS;

typedef struct PS_PHONG_UNIFORMS {
    packed_float4 diffuseColor;
    packed_float4 ambientLightColor;
    packed_float4 specColor;
    float lightsColor[MAX_NUM_LIGHTS * 4];
    float lightsAttenuation[MAX_NUM_LIGHTS * 4];
    float lightsRange[MAX_NUM_LIGHTS * 4];
    float spotLightsFactors[MAX_NUM_LIGHTS * 4];
    int numLights;
    int specType;
    bool isBumpMap;
    bool isIlluminated;
} PS_PHONG_UNIFORMS;

@interface MetalMeshView : NSObject
{
    MetalContext* context;
    MetalMesh* mesh;
    MetalPhongMaterial *material;
    MetalLight* lights[MAX_NUM_LIGHTS];
    vector_float4 ambientLightColor;
    int numLights;
    bool lightsDirty;
    int cullMode;
    bool wireframe;

    VS_PHONG_UNIFORMS vsUniforms;
    PS_PHONG_UNIFORMS psUniforms;
}

- (MetalMeshView*) createMeshView:(MetalContext*)ctx
                             mesh:(MetalMesh*)mtlMesh;
- (void) setMaterial:(MetalPhongMaterial*)pMaterial;
- (void) setCullingMode:(int)cMode;
- (void) setWireframe:(bool)isWireFrame;
- (void) setAmbientLight:(float)r
                       g:(float)g
                       b:(float)b;
- (void) computeNumLights;
- (void) setLight:(int)index
        x:(float)x y:(float)y z:(float)z
        r:(float)r g:(float)g b:(float)b w:(float)w
        ca:(float)ca la:(float)la qa:(float)qa
        isA:(float)isAttenuated range:(float)range
        dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
        inA:(float)innerAngle outA:(float)outerAngle
        falloff:(float)fall_off;

- (MetalMesh*) getMesh;
- (int) getCullingMode;
- (void) render;
- (void) release;
@end

#endif
