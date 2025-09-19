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

#import "MetalMeshView.h"
#import "MetalPipelineManager.h"

@implementation MetalMeshView

- (MetalMeshView*) createMeshView:(MetalContext*)ctx
                             mesh:(MetalMesh*)mtlMesh
{
    self = [super init];
    if (self) {
        context = ctx;
        mesh = mtlMesh;
        material = nil;
        ambientLightColor.x = 0;
        ambientLightColor.y = 0;
        ambientLightColor.z = 0;
        ambientLightColor.w = 0;
        numLights = 0;
        lightsDirty = TRUE;
        cullMode = MTLCullModeBack;
        wireframe = FALSE;
    }
    return self;
}

- (void) setMaterial:(MetalPhongMaterial*)pMaterial
{
    material = pMaterial;
}

- (void) setCullingMode:(int)cMode
{
    cullMode = cMode;
}

- (void) setWireframe:(bool)isWireFrame
{
    wireframe = isWireFrame;
}

- (void) setAmbientLight:(float)r
                       g:(float)g
                       b:(float)b
{
    ambientLightColor.x = r;
    ambientLightColor.y = g;
    ambientLightColor.z = b;
    ambientLightColor.w = 1;
}

- (void) computeNumLights
{
    if (!lightsDirty)
        return;
    lightsDirty = false;

    int n = 0;
    for (int i = 0; i != MAX_NUM_LIGHTS; ++i) {
        n += lights[i]->lightOn ? 1 : 0;
    }

    numLights = n;
}

- (void) setLight:(int)index
        x:(float)x y:(float)y z:(float)z
        r:(float)r g:(float)g b:(float)b w:(float)w
        ca:(float)ca la:(float)la qa:(float)qa
        isA:(float)isAttenuated range:(float)range
        dirX:(float)dirX dirY:(float)dirY dirZ:(float)dirZ
        inA:(float)innerAngle outA:(float)outerAngle
        falloff:(float)fall_off
{
    // NOTE: We only support up to 3 point lights at the present
    if (index >= 0 && index <= MAX_NUM_LIGHTS - 1) {
        if (lights[index] == nil) {
            MetalLight* light = ([[MetalLight alloc] createLight:x y:y z:z
            r:r g:g b:b w:w
            ca:ca la:la qa:qa
            isA:isAttenuated range:range
            dirX:dirX dirY:dirY dirZ:dirZ
            inA:innerAngle outA:outerAngle
            falloff:fall_off]);
            lights[index] = light;
        } else {
            lights[index]->position[0] = x;
            lights[index]->position[1] = y;
            lights[index]->position[2] = z;
            lights[index]->color[0] = r;
            lights[index]->color[1] = g;
            lights[index]->color[2] = b;
            lights[index]->lightOn = w;
            lights[index]->attenuation[0] = ca;
            lights[index]->attenuation[1] = la;
            lights[index]->attenuation[2] = qa;
            lights[index]->attenuation[3] = isAttenuated;
            lights[index]->maxRange = range;
            lights[index]->direction[0] = dirX;
            lights[index]->direction[1] = dirY;
            lights[index]->direction[2] = dirZ;
            lights[index]->inAngle = innerAngle;
            lights[index]->outAngle = outerAngle;
            lights[index]->falloff = fall_off;
        }
        lightsDirty = TRUE;
    }
}

- (MetalMesh*) getMesh
{
    return mesh;
}

- (int) getCullingMode
{
    return cullMode;
}

- (void) render
{
    [self computeNumLights];

    for (int i = 0, d = 0, p = 0, c = 0, a = 0, r = 0, s = 0; i < numLights; i++) {
        MetalLight* light = lights[i];

        vsUniforms.lightsPosition[p++] = light->position[0];
        vsUniforms.lightsPosition[p++] = light->position[1];
        vsUniforms.lightsPosition[p++] = light->position[2];

        vsUniforms.lightsNormDirection[d++] = light->direction[0];
        vsUniforms.lightsNormDirection[d++] = light->direction[1];
        vsUniforms.lightsNormDirection[d++] = light->direction[2];

        psUniforms.lightsColor[c++] = light->color[0];
        psUniforms.lightsColor[c++] = light->color[1];
        psUniforms.lightsColor[c++] = light->color[2];
        psUniforms.lightsColor[c++] = 1;

        psUniforms.lightsAttenuation[a++] = light->attenuation[0];
        psUniforms.lightsAttenuation[a++] = light->attenuation[1];
        psUniforms.lightsAttenuation[a++] = light->attenuation[2];
        psUniforms.lightsAttenuation[a++] = light->attenuation[3];

        psUniforms.lightsRange[r++] = light->maxRange;
        psUniforms.lightsRange[r++] = 0;
        psUniforms.lightsRange[r++] = 0;
        psUniforms.lightsRange[r++] = 0;

        if ([light isPointLight] || [light isDirectionalLight]) {
            psUniforms.spotLightsFactors[s++] = -1; // cos(180)
            psUniforms.spotLightsFactors[s++] = 2;  // cos(0) - cos(180)
            psUniforms.spotLightsFactors[s++] = 0;
            psUniforms.spotLightsFactors[s++] = 0;
        } else {
            // preparing for: I = pow((cosAngle - cosOuter) / (cosInner - cosOuter), falloff)
            float cosInner = cos(light->inAngle * M_PI / 180);
            float cosOuter = cos(light->outAngle * M_PI / 180);
            psUniforms.spotLightsFactors[s++] = cosOuter;
            psUniforms.spotLightsFactors[s++] = cosInner - cosOuter;
            psUniforms.spotLightsFactors[s++] = light->falloff;
            psUniforms.spotLightsFactors[s++] = 0;
        }
    }

    id<MTLRenderCommandEncoder> phongEncoder = [context getCurrentRenderEncoder];
    [phongEncoder setRenderPipelineState:[context getPhongPipelineStateWithNumLights:numLights]];
    id<MTLDepthStencilState> depthStencilState =
        [[context getPipelineManager] getDepthStencilState];
    [phongEncoder setDepthStencilState:depthStencilState];
    // In Metal default winding order is Clockwise but the vertex data that
    // we are getting is in CounterClockWise order, so we need to set
    // MTLWindingCounterClockwise explicitly
    [phongEncoder setFrontFacingWinding:MTLWindingCounterClockwise];
    [phongEncoder setCullMode:cullMode];
    if (wireframe) {
        [phongEncoder setTriangleFillMode:MTLTriangleFillModeLines];
    } else {
        [phongEncoder setTriangleFillMode:MTLTriangleFillModeFill];
    }
    [phongEncoder setScissorRect:[context getScissorRect]];
    vsUniforms.mvp_matrix = [context getMVPMatrix];
    vsUniforms.world_matrix = [context getWorldMatrix];
    vsUniforms.cameraPos = [context getCameraPosition];
    vsUniforms.numLights = numLights;
    id<MTLBuffer> vBuffer = [mesh getVertexBuffer];
    [phongEncoder setVertexBuffer:vBuffer
                           offset:0
                            atIndex:0];
    [phongEncoder setVertexBytes:&vsUniforms
                               length:sizeof(vsUniforms)
                              atIndex:1];
    psUniforms.diffuseColor = [material getDiffuseColor];
    psUniforms.ambientLightColor = ambientLightColor;
    psUniforms.specColor = [material getSpecularColor];

    psUniforms.numLights = numLights;
    psUniforms.specType = [material getSpecType];
    psUniforms.isBumpMap = [material isBumpMap] ? true : false;
    psUniforms.isIlluminated = [material isSelfIllumMap] ? true : false;

    [phongEncoder setFragmentBytes:&psUniforms
                                length:sizeof(psUniforms)
                                atIndex:0];
    [phongEncoder setFragmentTexture:[material getMap:DIFFUSE]
                             atIndex:0];
    [phongEncoder setFragmentTexture:[material getMap:SPECULAR]
                             atIndex:1];
    [phongEncoder setFragmentTexture:[material getMap:BUMP]
                             atIndex:2];
    [phongEncoder setFragmentTexture:[material getMap:SELFILLUMINATION]
                             atIndex:3];

    [phongEncoder drawIndexedPrimitives:MTLPrimitiveTypeTriangle
        indexCount:[mesh getNumIndices]
        indexType:[mesh getIndexType]
        indexBuffer:[mesh getIndexBuffer]
        indexBufferOffset:0];
}

- (void) release
{
    for (int i = 0; i < MAX_NUM_LIGHTS; i++) {
        if (lights[i] != nil) {
            [lights[i] release];
            lights[i] = nil;
        }
    }
}

@end // MetalMeshView
