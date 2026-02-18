/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

#import <jni.h>
#import <dlfcn.h>
#import <stdlib.h>
#import <assert.h>
#import <stdio.h>
#import <string.h>
#import <math.h>

#import "MetalContext.h"
#import "MetalRTTexture.h"
#import "MetalPipelineManager.h"
#import "MetalShader.h"
#import "MetalMesh.h"
#import "MetalMeshView.h"
#import "MetalPhongMaterial.h"
#import "com_sun_prism_mtl_MTLContext.h"
#import "com_sun_prism_mtl_MTLPipeline.h"

@implementation MetalContext

- (id) createContext:(dispatch_data_t)shaderLibData
{
    self = [super init];
    if (self) {
        device = MTLCreateSystemDefaultDevice();

        currentRingBufferIndex = 0;
        ringBufferSemaphore = dispatch_semaphore_create(0);
        ringBufferLock = [[NSLock alloc] init];
        isWaitingForBuffer = false;

        argsRingBuffer = [[MetalRingBuffer alloc] init:self
                                                ofSize:ARGS_BUFFER_SIZE];
        dataRingBuffer = [[MetalRingBuffer alloc] init:self
                                                ofSize:DATA_BUFFER_SIZE];
        transientBuffersForCB = [[NSMutableArray alloc] init];
        shadersUsedInCB = [[NSMutableSet alloc] init];
        isScissorEnabled = false;
        commitOnDraw = false;
        currentRenderEncoder = nil;
        meshIndexCount = 0;
        linearSamplerDict = [[NSMutableDictionary alloc] init];
        nonLinearSamplerDict = [[NSMutableDictionary alloc] init];
        compositeMode = com_sun_prism_mtl_MTLContext_MTL_COMPMODE_SRCOVER; //default

        currentBufferIndex = 0;
        commandQueue = [device newCommandQueue];
        commandQueue.label = @"The only MTLCommandQueue";
        pipelineManager = [MetalPipelineManager alloc];
        [pipelineManager init:self libData:shaderLibData];

        rttPassDesc = [MTLRenderPassDescriptor new];
        rttPassDesc.colorAttachments[0].clearColor  = MTLClearColorMake(1, 1, 1, 1); // make this programmable
        rttPassDesc.colorAttachments[0].storeAction = MTLStoreActionStore;
        rttPassDesc.colorAttachments[0].loadAction  = MTLLoadActionLoad;

        pixelBuffer = [device newBufferWithLength:4 options:MTLResourceStorageModeShared];

        // clearing rtt related initialization
        clearEntireRttVerticesBuf = [device newBufferWithLength:sizeof(CLEAR_VS_INPUT) * 4
                                                        options:MTLResourceStorageModePrivate];
        id<MTLBuffer> tclearVertBuf = [self getTransientBufferWithLength:sizeof(CLEAR_VS_INPUT) * 4];
        CLEAR_VS_INPUT* clearEntireRttVertices = (CLEAR_VS_INPUT*)tclearVertBuf.contents;

        clearEntireRttVertices[0].position.x = -1; clearEntireRttVertices[0].position.y = -1;
        clearEntireRttVertices[1].position.x = -1; clearEntireRttVertices[1].position.y =  1;
        clearEntireRttVertices[2].position.x =  1; clearEntireRttVertices[2].position.y = -1;
        clearEntireRttVertices[3].position.x =  1; clearEntireRttVertices[3].position.y =  1;

        // Create Index Buffer
        indexBuffer = [device newBufferWithLength:(INDICES_PER_IB * sizeof(unsigned short))
                                          options:MTLResourceStorageModePrivate];
        id<MTLBuffer> tIndexBuffer = [self getTransientBufferWithLength:
                                               (INDICES_PER_IB * sizeof(unsigned short))];
        unsigned short* indices = (unsigned short*)tIndexBuffer.contents;
        for (unsigned short i = 0, j = 0; i < INDICES_PER_IB; j += 4, i += 6) {
            indices[i + 0] = 0 + j; // 0, 4,  8, 12
            indices[i + 1] = 1 + j; // 1, 5,  9, 13
            indices[i + 2] = 2 + j; // 2, 6, 10, 14

            indices[i + 3] = 1 + j; // 1, 5,  9, 13
            indices[i + 4] = 2 + j; // 2, 6, 10, 14
            indices[i + 5] = 3 + j; // 3, 7, 11, 15
        }

        id<MTLCommandBuffer> commandBuffer = [self getCurrentCommandBuffer];
        @autoreleasepool {
            id<MTLBlitCommandEncoder> blitEncoder = [commandBuffer blitCommandEncoder];
            [blitEncoder copyFromBuffer:tIndexBuffer
                           sourceOffset:(NSUInteger)0
                               toBuffer:indexBuffer
                      destinationOffset:(NSUInteger)0
                                   size:tIndexBuffer.length];

            [blitEncoder copyFromBuffer:tclearVertBuf
                           sourceOffset:(NSUInteger)0
                               toBuffer:clearEntireRttVerticesBuf
                      destinationOffset:(NSUInteger)0
                                   size:tclearVertBuf.length];

            [blitEncoder endEncoding];
        }
        [self commitCurrentCommandBuffer:false];
    }
    return self;
}

- (int) setRTT:(MetalRTTexture*)rttPtr
{
    if (rtt != rttPtr) {
        [self endCurrentRenderEncoder];
    }
    // The method can possibly be optmized(with no significant gain in FPS)
    // to avoid updating RenderPassDescriptor if the render target
    // is not being changed, implement or change in future if necessary.
    rtt = rttPtr;
    id<MTLTexture> mtlTex = [rtt getTexture];
    [self validatePixelBuffer:(mtlTex.width * mtlTex.height * 4)];
    if ([rttPtr isMSAAEnabled]) {
        rttPassDesc.colorAttachments[0].storeAction = MTLStoreActionStoreAndMultisampleResolve;
        rttPassDesc.colorAttachments[0].texture = [rtt getMSAATexture];
        rttPassDesc.colorAttachments[0].resolveTexture = [rtt getTexture];
    } else {
        rttPassDesc.colorAttachments[0].storeAction = MTLStoreActionStore;
        rttPassDesc.colorAttachments[0].texture = [rtt getTexture];
        rttPassDesc.colorAttachments[0].resolveTexture = nil;
    }
    [self resetClipRect];
    return 1;
}

- (MetalRTTexture*) getRTT
{
    return rtt;
}

- (void) validatePixelBuffer:(NSUInteger)length
{
    if ([pixelBuffer length] < length) {
        [transientBuffersForCB addObject:pixelBuffer];
        pixelBuffer = [device newBufferWithLength:length options:MTLResourceStorageModeShared];
    }
}

- (id<MTLBuffer>) getPixelBuffer
{
    return pixelBuffer;
}

- (MetalRingBuffer*) getArgsRingBuffer
{
    return argsRingBuffer;
}

- (MetalRingBuffer*) getDataRingBuffer
{
    return dataRingBuffer;
}

- (id<MTLBuffer>) getTransientBufferWithBytes:(const void *)pointer length:(NSUInteger)length
{
    id<MTLBuffer> transientBuf = [device newBufferWithBytes:pointer
                                                     length:length
                                                    options:MTLResourceStorageModeShared];
    [transientBuffersForCB addObject:transientBuf];
    commitOnDraw = true;
    return transientBuf;
}

- (id<MTLBuffer>) getTransientBufferWithLength:(NSUInteger)length
{
    id<MTLBuffer> transientBuf = [device newBufferWithLength:length
                                                     options:MTLResourceStorageModeShared];
    [transientBuffersForCB addObject:transientBuf];
    commitOnDraw = true;
    return transientBuf;
}

- (id<MTLSamplerState>) getSampler:(bool)isLinear
                          wrapMode:(int)wrapMode
{
    NSMutableDictionary* samplerDict;
    if (isLinear) {
        samplerDict = linearSamplerDict;
    } else {
        samplerDict = nonLinearSamplerDict;
    }
    NSNumber *keyWrapMode = [NSNumber numberWithInt:wrapMode];
    id<MTLSamplerState> sampler = samplerDict[keyWrapMode];
    if (sampler == nil) {
        sampler = [self createSampler:isLinear wrapMode:wrapMode];
        [samplerDict setObject:sampler forKey:keyWrapMode];
    }
    return sampler;
}

- (id<MTLSamplerState>) createSampler:(bool)isLinear
                             wrapMode:(int)wrapMode
{
    MTLSamplerDescriptor *samplerDescriptor = [MTLSamplerDescriptor new];
    if (isLinear) {
        samplerDescriptor.minFilter = MTLSamplerMinMagFilterLinear;
        samplerDescriptor.magFilter = MTLSamplerMinMagFilterLinear;
    }
    if (wrapMode != -1) {
        samplerDescriptor.sAddressMode = wrapMode;
        samplerDescriptor.tAddressMode = wrapMode;
    }
    id<MTLSamplerState> sampler = [[self getDevice] newSamplerStateWithDescriptor:samplerDescriptor];
    [samplerDescriptor release];
    return sampler;
}

- (id<MTLDevice>) getDevice
{
    return device;
}

- (void) commitCurrentCommandBuffer
{
    [self commitCurrentCommandBuffer:false];
}

- (void) commitCurrentCommandBufferAndWait
{
    [self commitCurrentCommandBuffer:true];
}

- (void) commitCurrentCommandBuffer:(bool)waitUntilCompleted
{
    if (currentCommandBuffer == nil) {
        return;
    }
    [self endCurrentRenderEncoder];

    NSMutableArray* bufsForCB = transientBuffersForCB;
    transientBuffersForCB = [[NSMutableArray alloc] init];

    for (MetalShader* shader in shadersUsedInCB) {
        [shader setArgsUpdated:true];
    }
    [shadersUsedInCB removeAllObjects];

    int rbid = currentRingBufferIndex;
    if ([argsRingBuffer getNumReservedBytes] == 0 &&
            [dataRingBuffer getNumReservedBytes] == 0) {
        [MetalRingBuffer resetBuffer:rbid];
        rbid = -1;
    }

    [currentCommandBuffer addCompletedHandler:^(id<MTLCommandBuffer> cb) {
        // The CompletedHandler is invoked on different background threads.
        // CommandBuffer are executed sequentially. A CommandBuffer is
        // considered to be completed only when all its CompletedHandler are
        // executed. So two completed handlers would never execute concurrently.

        if (rbid > -1) {
            [MetalRingBuffer resetBuffer:rbid];
            if (isWaitingForBuffer) {
                [ringBufferLock lock];
                if (isWaitingForBuffer) {
                    isWaitingForBuffer = false;
                    dispatch_semaphore_signal(ringBufferSemaphore);
                }
                [ringBufferLock unlock];
            }
        }
        for (id buffer in bufsForCB) {
            [buffer release];
        }
        [bufsForCB removeAllObjects];
        [bufsForCB release];
    }];
    commitOnDraw = false;
    [currentCommandBuffer commit];

    if (waitUntilCompleted) {
        [currentCommandBuffer waitUntilCompleted];
    } else {
        if (![MetalRingBuffer isBufferAvailable]) {
            [ringBufferLock lock];
            isWaitingForBuffer = true;
            [ringBufferLock unlock];
            dispatch_semaphore_wait(ringBufferSemaphore, DISPATCH_TIME_FOREVER);
        }
    }

    currentRingBufferIndex = [MetalRingBuffer updateBufferInUse];
    [argsRingBuffer resetOffsets];
    [dataRingBuffer resetOffsets];
    [currentCommandBuffer release];
    currentCommandBuffer = nil;
}

- (id<MTLCommandBuffer>) getCurrentCommandBuffer
{
    if (currentCommandBuffer == nil
                || currentCommandBuffer.status != MTLCommandBufferStatusNotEnqueued) {

        @autoreleasepool {
            // The commandBuffer creation using commandQueue returns
            // an autoreleased object. We need this object at a class level as it
            // gets used in other class methods.
            // Take up the ownership of this commandBuffer object using retain.
            currentCommandBuffer = [[commandQueue commandBuffer] retain];
        }
        currentCommandBuffer.label = @"JFX Command Buffer";
    }
    return currentCommandBuffer;
}

- (id<MTLRenderCommandEncoder>) getCurrentRenderEncoder
{
    if (currentRenderEncoder == nil) {
        id<MTLCommandBuffer> cb = [self getCurrentCommandBuffer];

        @autoreleasepool {
            // The RenderEncoder creation using command buffer returns
            // an autoreleased object. We need this object at a class level as it
            // gets used in other class methods.
            // Take up the ownership of this RenderEncoder object using retain.
            currentRenderEncoder = [[cb renderCommandEncoderWithDescriptor:rttPassDesc] retain];
        }
    }
    return currentRenderEncoder;
}

- (void) endCurrentRenderEncoder
{
    if (currentRenderEncoder != nil) {
        meshIndexCount = 0;
        [currentRenderEncoder endEncoding];
        [currentRenderEncoder release];
        currentRenderEncoder = nil;
    }
}

- (NSUInteger) getCurrentBufferIndex
{
    return currentBufferIndex;
}

- (id<MTLRenderPipelineState>) getPhongPipelineStateWithNumLights:(int)numLights
{
    return [[self getPipelineManager] getPhongPipeStateWithNumLights:numLights
                compositeMode:[self getCompositeMode]];
}

- (NSInteger) drawIndexedQuads:(PrismSourceVertex const *)pSrcXYZUVs
                      ofColors:(char const *)pSrcColors
                   vertexCount:(NSUInteger)numVertices
{
    int vbLength   = numVertices * sizeof(PrismSourceVertex);
    int cbLength   = numVertices * 4;
    int numQuads   = numVertices / 4;
    int numIndices = numQuads * 6;

    id<MTLBuffer> vertexBuffer = [dataRingBuffer getBuffer];
    int vertexOffset = [dataRingBuffer reserveBytes:vbLength];
    if (vertexOffset < 0) {
        vertexBuffer = [self getTransientBufferWithLength:vbLength];
        vertexOffset = 0;
    }
    memcpy(vertexBuffer.contents + vertexOffset, pSrcXYZUVs, vbLength);

    id<MTLBuffer> colorBuffer = [dataRingBuffer getBuffer];
    int colorOffset = [dataRingBuffer reserveBytes:cbLength];
    if (colorOffset < 0) {
        colorBuffer = [self getTransientBufferWithLength:cbLength];
        colorOffset = 0;
    }
    memcpy(colorBuffer.contents + colorOffset, pSrcColors, cbLength);

    id<MTLRenderCommandEncoder> renderEncoder = [self getCurrentRenderEncoder];

    [renderEncoder setFrontFacingWinding:MTLWindingClockwise];
    [renderEncoder setCullMode:MTLCullModeNone];
    [renderEncoder setTriangleFillMode:MTLTriangleFillModeFill];

    [renderEncoder setVertexBytes:&mvpMatrix
                           length:sizeof(mvpMatrix)
                          atIndex:VertexInputMatrixMVP];

    MetalShader* shader = [self getCurrentShader];
    [shadersUsedInCB addObject:shader];

    [renderEncoder setRenderPipelineState:[shader getPipelineState:[rtt isMSAAEnabled]
                                                     compositeMode:compositeMode]];
    if (depthEnabled) {
        id<MTLDepthStencilState> depthStencilState =
            [[self getPipelineManager] getDepthStencilState];
        [renderEncoder setDepthStencilState:depthStencilState];
    }

    if ([shader getArgumentBufferLength] != 0) {
        [shader copyArgBufferToRingBuffer];
        [renderEncoder setFragmentBuffer:[shader getRingBuffer]
                                  offset:[shader getRingBufferOffset]
                                 atIndex:0];

        NSMutableDictionary* texturesDict = [shader getTexutresDict];
        if ([texturesDict count] > 0) {
            for (NSString *key in texturesDict) {
                id<MTLTexture> tex = texturesDict[key];
                [renderEncoder useResource:tex usage:MTLResourceUsageRead];
            }

            NSMutableDictionary* samplersDict = [shader getSamplersDict];
            for (NSNumber *key in samplersDict) {
                id<MTLSamplerState> sampler = samplersDict[key];
                [renderEncoder setFragmentSamplerState:sampler atIndex:[key integerValue]];
            }
        }
    }

    [renderEncoder setScissorRect:[self getScissorRect]];

    for (int i = 0; numIndices > 0; i++) {
        [renderEncoder setVertexBuffer:vertexBuffer
                                offset:(vertexOffset + (i * VERTICES_PER_IB * sizeof(PrismSourceVertex)))
                               atIndex:VertexInputIndexVertices];

        [renderEncoder setVertexBuffer:colorBuffer
                                offset:(colorOffset + (i * VERTICES_PER_IB * 4))
                               atIndex:VertexInputColors];

        [renderEncoder drawIndexedPrimitives:MTLPrimitiveTypeTriangle
                                  indexCount:((numIndices > INDICES_PER_IB) ? INDICES_PER_IB : numIndices)
                                   indexType:MTLIndexTypeUInt16
                                 indexBuffer:indexBuffer
                           indexBufferOffset:0];
        numIndices -= INDICES_PER_IB;
    }

    if (commitOnDraw) {
        [self commitCurrentCommandBuffer];
    }
    return 1;
}

- (void) setProjViewMatrix:(bool)depthTest
        m00:(float)m00 m01:(float)m01 m02:(float)m02 m03:(float)m03
        m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
        m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
        m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33
{
    mvpMatrix = simd_matrix(
        (simd_float4){ m00, m01, m02, m03 },
        (simd_float4){ m10, m11, m12, m13 },
        (simd_float4){ m20, m21, m22, m23 },
        (simd_float4){ m30, m31, m32, m33 }
    );
    if (depthTest &&
        ([rtt getDepthTexture] != nil)) {
        depthEnabled = true;
    } else {
        depthEnabled = false;
    }
    [self updateDepthDetails:depthTest];
}

- (void) setProjViewMatrix:(float)m00
        m01:(float)m01 m02:(float)m02 m03:(float)m03
        m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
        m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
        m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33
{
    mvpMatrix = simd_matrix(
        (simd_float4){ m00, m01, m02, m03 },
        (simd_float4){ m10, m11, m12, m13 },
        (simd_float4){ m20, m21, m22, m23 },
        (simd_float4){ m30, m31, m32, m33 }
    );
}

- (void) setWorldTransformMatrix:(float)m00
        m01:(float)m01 m02:(float)m02 m03:(float)m03
        m10:(float)m10 m11:(float)m11 m12:(float)m12 m13:(float)m13
        m20:(float)m20 m21:(float)m21 m22:(float)m22 m23:(float)m23
        m30:(float)m30 m31:(float)m31 m32:(float)m32 m33:(float)m33
{
    worldMatrix = simd_matrix(
        (simd_float4){ m00, m01, m02, m03 },
        (simd_float4){ m10, m11, m12, m13 },
        (simd_float4){ m20, m21, m22, m23 },
        (simd_float4){ m30, m31, m32, m33 }
    );
}

- (void) setWorldTransformIdentityMatrix
{
    worldMatrix = matrix_identity_float4x4;
}

- (void) clearRTT:(float)red
            green:(float)green
             blue:(float)blue
            alpha:(float)alpha
       clearDepth:(bool)clearDepth
{
    clearDepthTexture = false;
    if (clearDepth &&
        [rtt getDepthTexture] != nil) {
        clearDepthTexture = true;
        rttPassDesc.depthAttachment.clearDepth = 1.0;
        rttPassDesc.depthAttachment.loadAction = MTLLoadActionClear;
        if ([[self getRTT] isMSAAEnabled]) {
            rttPassDesc.depthAttachment.storeAction = MTLStoreActionStoreAndMultisampleResolve;
            rttPassDesc.depthAttachment.texture = [rtt getDepthMSAATexture];
            rttPassDesc.depthAttachment.resolveTexture = [rtt getDepthTexture];
        } else {
            rttPassDesc.depthAttachment.storeAction = MTLStoreActionStore;
            rttPassDesc.depthAttachment.texture = [[self getRTT] getDepthTexture];
            rttPassDesc.depthAttachment.resolveTexture = nil;
        }
    } else {
        rttPassDesc.depthAttachment = nil;
    }
    clearColor[0] = red;
    clearColor[1] = green;
    clearColor[2] = blue;
    clearColor[3] = alpha;

    id<MTLRenderCommandEncoder> renderEncoder = [self getCurrentRenderEncoder];

    [renderEncoder setRenderPipelineState:[pipelineManager getClearRttPipeState]];
    if (clearDepthTexture) {
        id<MTLDepthStencilState> depthStencilState =
            [[self getPipelineManager] getDepthStencilState];
        [renderEncoder setDepthStencilState:depthStencilState];
    }
    [renderEncoder setFrontFacingWinding:MTLWindingClockwise];
    [renderEncoder setCullMode:MTLCullModeNone];
    [renderEncoder setTriangleFillMode:MTLTriangleFillModeFill];

    [renderEncoder setScissorRect:[self getScissorRect]];

    if (isScissorEnabled) {
        [renderEncoder setVertexBytes:clearScissorRectVertices
                               length:sizeof(clearScissorRectVertices)
                              atIndex:VertexInputIndexVertices];
    } else {
        [renderEncoder setVertexBuffer:clearEntireRttVerticesBuf
                                offset:0
                               atIndex:VertexInputIndexVertices];
    }

    [renderEncoder setFragmentBytes:clearColor
                             length:sizeof(clearColor)
                            atIndex:VertexInputClearColor];

    [renderEncoder drawIndexedPrimitives:MTLPrimitiveTypeTriangle
                              indexCount:6
                               indexType:MTLIndexTypeUInt16
                             indexBuffer:indexBuffer
                       indexBufferOffset:0];

    if (clearDepth && !depthEnabled) {
        [self endCurrentRenderEncoder];
    }
}

- (void) renderMeshView:(MetalMeshView*)meshView
{
    MetalMesh* mesh = [meshView getMesh];
    NSUInteger indexCount = [mesh getNumIndices];
    meshIndexCount += indexCount;
    if (meshIndexCount >= MESH_INDEX_LIMIT) {
        [self endCurrentRenderEncoder];
    }
    [meshView render];
}

- (void) setClipRect:(int)x y:(int)y width:(int)width height:(int)height
{
    id<MTLTexture> currRtt = [rtt getTexture];
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    int x1 = x + width;
    int y1 = y + height;
    if (x <= 0 && y <= 0 && x1 >= currRtt.width && y1 >= currRtt.height) {
        [self resetClipRect];
    } else {
        if (x1 > currRtt.width)  width  = currRtt.width - x;
        if (y1 > currRtt.height) height = currRtt.height - y;
        if (x > x1)              width  = x = 0;
        if (y > y1)              height = y = 0;
        scissorRect.x = x;
        scissorRect.y = y;
        scissorRect.width  = width;
        scissorRect.height = height;
        isScissorEnabled = true;

        // Create device space (-1, 1) coordinates of scissor rect.
        float halfWidth  = (float)currRtt.width  / 2.0f;
        float halfHeight = (float)currRtt.height / 2.0f;
        float x1 =   (scissorRect.x - halfWidth)  / halfWidth;
        float y1 = - (scissorRect.y - halfHeight) / halfHeight;
        float x2 =   ((scissorRect.x + scissorRect.width)  - halfWidth)  / halfWidth;
        float y2 = - ((scissorRect.y + scissorRect.height) - halfHeight) / halfHeight;

        clearScissorRectVertices[0].position.x = x1;
        clearScissorRectVertices[0].position.y = y1;
        clearScissorRectVertices[1].position.x = x1;
        clearScissorRectVertices[1].position.y = y2;
        clearScissorRectVertices[2].position.x = x2;
        clearScissorRectVertices[2].position.y = y1;
        clearScissorRectVertices[3].position.x = x2;
        clearScissorRectVertices[3].position.y = y2;
    }
}

- (void) resetClipRect
{
    isScissorEnabled = false;
    scissorRect.x = 0;
    scissorRect.y = 0;
    scissorRect.width  = 0;
    scissorRect.height = 0;
}

- (void) resetProjViewMatrix
{
    mvpMatrix = matrix_identity_float4x4;
}

- (MetalPipelineManager*) getPipelineManager
{
    return pipelineManager;
}

- (MetalShader*) getCurrentShader
{
    return currentShader;
}

- (void) setCurrentShader:(MetalShader*)shader
{
    currentShader = shader;
}

- (void) updateDepthDetails:(bool)depthTest
{
    if (depthTest) {
        if ([[self getRTT] isMSAAEnabled]) {
            rttPassDesc.depthAttachment.storeAction = MTLStoreActionStoreAndMultisampleResolve;
            rttPassDesc.depthAttachment.texture = [rtt getDepthMSAATexture];
            rttPassDesc.depthAttachment.resolveTexture = [rtt getDepthTexture];
        } else {
            rttPassDesc.depthAttachment.storeAction = MTLStoreActionStore;
            rttPassDesc.depthAttachment.texture = [[self getRTT] getDepthTexture];
            rttPassDesc.depthAttachment.resolveTexture = nil;
        }
    } else {
        rttPassDesc.depthAttachment = nil;
    }
}

- (void) verifyDepthTexture
{
    id<MTLTexture> depthTexture = [rtt getDepthTexture];
    if (depthTexture == nil) {
        [rtt createDepthTexture];
        rttPassDesc.depthAttachment.clearDepth = 1.0;
        rttPassDesc.depthAttachment.loadAction = MTLLoadActionClear;
    } else {
        rttPassDesc.depthAttachment.loadAction = MTLLoadActionLoad;
    }
}

- (void) setCompositeMode:(int)mode
{
    compositeMode = mode;
}

- (int) getCompositeMode
{
    return compositeMode;
}

- (void) setCameraPosition:(float)x y:(float)y z:(float)z
{
    cPos.x = x;
    cPos.y = y;
    cPos.z = z;
    cPos.w = 0;
}

- (MTLRenderPassDescriptor*) getPhongRPD
{
    return phongRPD;
}

- (simd_float4x4) getMVPMatrix
{
    return mvpMatrix;
}

- (simd_float4x4) getWorldMatrix
{
    return worldMatrix;
}

- (vector_float4) getCameraPosition
{
    return cPos;
}

- (MTLScissorRect) getScissorRect
{
    if (!isScissorEnabled) {
        scissorRect.x = 0;
        scissorRect.y = 0;
        id<MTLTexture> currRtt = rttPassDesc.colorAttachments[0].texture;
        scissorRect.width  = currRtt.width;
        scissorRect.height = currRtt.height;
    }
    return scissorRect;
}

- (bool) clearDepth
{
    return clearDepthTexture;
}

- (bool) isDepthEnabled
{
    return depthEnabled;
}

- (bool) isScissorEnabled
{
    return isScissorEnabled;
}

- (bool) isCurrentRTT:(MetalRTTexture*)rttPtr
{
    if (rttPtr == rtt) {
        return true;
    } else {
        return false;
    }
}

- (void) dealloc
{
    if (currentCommandBuffer == nil) {
        [self getCurrentCommandBuffer];
    }
    [self commitCurrentCommandBuffer:true];

    if (commandQueue != nil) {
        [commandQueue release];
        commandQueue = nil;
    }

    if (pipelineManager != nil) {
        [pipelineManager release];
        pipelineManager = nil;
    }

    if (rttPassDesc != nil) {
        [rttPassDesc release];
        rttPassDesc = nil;
    }

    if (phongRPD != nil) {
        [phongRPD release];
        phongRPD = nil;
    }

    if (argsRingBuffer != nil) {
        [argsRingBuffer dealloc];
        argsRingBuffer = nil;
    }

    if (dataRingBuffer != nil) {
        [dataRingBuffer dealloc];
        dataRingBuffer = nil;
    }

    for (NSNumber *keyWrapMode in linearSamplerDict) {
        [linearSamplerDict[keyWrapMode] release];
    }
    for (NSNumber *keyWrapMode in nonLinearSamplerDict) {
        [nonLinearSamplerDict[keyWrapMode] release];
    }
    [linearSamplerDict release];
    [nonLinearSamplerDict release];

    if (transientBuffersForCB != nil) {
        for (id buffer in transientBuffersForCB) {
            [buffer release];
        }
        [transientBuffersForCB removeAllObjects];
        [transientBuffersForCB release];
        transientBuffersForCB = nil;
    }

    if (shadersUsedInCB != nil) {
        [shadersUsedInCB removeAllObjects];
        [shadersUsedInCB release];
        shadersUsedInCB = nil;
    }

    if (clearEntireRttVerticesBuf != nil) {
        [clearEntireRttVerticesBuf release];
        clearEntireRttVerticesBuf = nil;
    }

    if (pixelBuffer != nil) {
        [pixelBuffer release];
        pixelBuffer = nil;
    }

    device = nil;

    [super dealloc];
}

- (id<MTLCommandQueue>) getCommandQueue
{
    return commandQueue;
}

- (void) blit:(id<MTLTexture>)src srcX0:(int)srcX0 srcY0:(int)srcY0 srcX1:(int)srcX1 srcY1:(int)srcY1
       dstTex:(id<MTLTexture>)dst dstX0:(int)dstX0 dstY0:(int)dstY0 dstX1:(int)dstX1 dstY1:(int)dstY1
{
    [self endCurrentRenderEncoder];

    id<MTLCommandBuffer> commandBuffer = [self getCurrentCommandBuffer];
    @autoreleasepool {
        id<MTLBlitCommandEncoder> blitEncoder = [commandBuffer blitCommandEncoder];
        if (src.usage == MTLTextureUsageRenderTarget) {
            [blitEncoder synchronizeTexture:src slice:0 level:0];
        }
        if (dst.usage == MTLTextureUsageRenderTarget) {
            [blitEncoder synchronizeTexture:dst slice:0 level:0];
        }
        [blitEncoder copyFromTexture:src
                         sourceSlice:(NSUInteger)0
                         sourceLevel:(NSUInteger)0
                        sourceOrigin:MTLOriginMake(0, 0, 0)
                          sourceSize:MTLSizeMake(src.width, src.height, src.depth)
                           toTexture:dst
                    destinationSlice:(NSUInteger)0
                    destinationLevel:(NSUInteger)0
                   destinationOrigin:MTLOriginMake(0, 0, 0)];
        [blitEncoder endEncoding];
    }
}

@end // MetalContext


// ** JNI METHODS **

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nInitialize
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLContext_nInitialize
    (JNIEnv *env, jclass jClass, jobject shaderLibBuffer)
{
    jlong jContextPtr = 0L;

    // Create data object from direct byte buffer
    const void* dataPtr = (*env)->GetDirectBufferAddress(env, shaderLibBuffer);
    if (dataPtr == NULL) {
        NSLog(@"MTLContext_nInitialize: shaderLibBuffer addr = NULL");
        return 0L;
    }

    const jlong numBytes = (*env)->GetDirectBufferCapacity(env, shaderLibBuffer);
    if (numBytes <= 0) {
        NSLog(@"MTLContext_nInitialize: shaderLibBuffer invalid capacity");
        return 0L;
    }


    // We use a no-op destructor because the direct ByteBuffer is managed on the
    // Java side. We must not free it here.
    dispatch_data_t shaderLibData = dispatch_data_create(dataPtr, numBytes,
            DISPATCH_QUEUE_SERIAL,
            ^(void) {});

    if (shaderLibData == nil) {
        NSLog(@"MTLContext_nInitialize: Unable to create a dispatch_data object");
        return 0L;
    }

    jContextPtr = ptr_to_jlong([[MetalContext alloc] createContext:shaderLibData]);
    return jContextPtr;
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nDisposeShader
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nDisposeShader
    (JNIEnv *env, jclass jClass, jlong shaderRef)
{
    MetalShader *shaderPtr = (MetalShader *)jlong_to_ptr(shaderRef);
    if (shaderPtr != nil) {
        [shaderPtr release];
        shaderPtr = nil;
    }
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nRelease
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nRelease
    (JNIEnv *env, jclass jClass, jlong context)
{
    MetalContext *contextPtr = (MetalContext *)jlong_to_ptr(context);

    if (contextPtr != nil) {
        [contextPtr dealloc];
    }
    contextPtr = nil;
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nCommitCurrentCommandBuffer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nCommitCurrentCommandBuffer
    (JNIEnv *env, jclass jClass, jlong context)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);
    [mtlContext commitCurrentCommandBuffer];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nDrawIndexedQuads
 * Signature: (J[F[BI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nDrawIndexedQuads
    (JNIEnv *env, jclass jClass, jlong context, jfloatArray vertices, jbyteArray colors, jint numVertices)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);

    PrismSourceVertex *pVertices =
                    (PrismSourceVertex *) (*env)->GetPrimitiveArrayCritical(env, vertices, 0);
    char *pColors = (char *) (*env)->GetPrimitiveArrayCritical(env, colors, 0);

    [mtlContext drawIndexedQuads:pVertices ofColors:pColors vertexCount:numVertices];

    if (pColors) (*env)->ReleasePrimitiveArrayCritical(env, colors, pColors, 0);
    if (pVertices) (*env)->ReleasePrimitiveArrayCritical(env, vertices, pVertices, 0);
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nUpdateRenderTarget
 * Signature: (JJZ)V
 */
JNIEXPORT int JNICALL Java_com_sun_prism_mtl_MTLContext_nUpdateRenderTarget
    (JNIEnv *env, jclass jClass, jlong context, jlong texPtr, jboolean depthTest)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);
    MetalRTTexture *rtt = (MetalRTTexture *)jlong_to_ptr(texPtr);
    int ret = [mtlContext setRTT:rtt];
    // If we create depth texture while creating RTT
    // then also current implementation works fine. So in future
    // if we see any performance/state impact we should move
    // depthTexture creation along with RTT creation,
    // implement or change in future if necessary.
    if (depthTest) {
        [mtlContext verifyDepthTexture];
    }
    return ret;
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetClipRect
 * Signature: (JJIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetClipRect
    (JNIEnv *env, jclass jClass, jlong ctx,
    jint x, jint y, jint width, jint height)
{
    MetalContext *pCtx = (MetalContext*)jlong_to_ptr(ctx);
    [pCtx setClipRect:x y:y width:width height:height];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nResetClipRect
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nResetClipRect
    (JNIEnv *env, jclass jClass, jlong ctx)
{
    MetalContext *pCtx = (MetalContext*)jlong_to_ptr(ctx);
    [pCtx resetClipRect];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetProjViewMatrix
 * Signature: (JZDDDDDDDDDDDDDDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetProjViewMatrix
    (JNIEnv *env, jclass jClass,
    jlong context, jboolean isOrtho,
    jdouble m00, jdouble m01, jdouble m02, jdouble m03,
    jdouble m10, jdouble m11, jdouble m12, jdouble m13,
    jdouble m20, jdouble m21, jdouble m22, jdouble m23,
    jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);
    [mtlContext setProjViewMatrix:isOrtho
        m00:m00 m01:m01 m02:m02 m03:m03
        m10:m10 m11:m11 m12:m12 m13:m13
        m20:m20 m21:m21 m22:m22 m23:m23
        m30:m30 m31:m31 m32:m32 m33:m33];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetTransform
 * Signature: (JDDDDDDDDDDDDDDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetTransform
    (JNIEnv *env, jclass jClass,
    jlong context,
    jdouble m00, jdouble m01, jdouble m02, jdouble m03,
    jdouble m10, jdouble m11, jdouble m12, jdouble m13,
    jdouble m20, jdouble m21, jdouble m22, jdouble m23,
    jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);

    // Added separate nSetTransform because previously
    // we used to use nSetProjViewMatrix only and enabled depth test
    // by default. Also check whether we need to do anything else
    // apart from just updating projection view matrix,
    // implement or change in future if necessary.

    [mtlContext setProjViewMatrix:m00
        m01:m01 m02:m02 m03:m03
        m10:m10 m11:m11 m12:m12 m13:m13
        m20:m20 m21:m21 m22:m22 m23:m23
        m30:m30 m31:m31 m32:m32 m33:m33];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetWorldTransform
 * Signature: (JDDDDDDDDDDDDDDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetWorldTransform
    (JNIEnv *env, jclass jClass,
    jlong context,
    jdouble m00, jdouble m01, jdouble m02, jdouble m03,
    jdouble m10, jdouble m11, jdouble m12, jdouble m13,
    jdouble m20, jdouble m21, jdouble m22, jdouble m23,
    jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);
    [mtlContext setWorldTransformMatrix:m00 m01:m01 m02:m02 m03:m03
        m10:m10 m11:m11 m12:m12 m13:m13
        m20:m20 m21:m21 m22:m22 m23:m23
        m30:m30 m31:m31 m32:m32 m33:m33];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetWorldTransformToIdentity
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetWorldTransformToIdentity
    (JNIEnv *env, jclass jClass, jlong context)
{
    MetalContext *mtlContext = (MetalContext *)jlong_to_ptr(context);
    [mtlContext setWorldTransformIdentityMatrix];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nCreateMTLMesh
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLContext_nCreateMTLMesh
    (JNIEnv *env, jclass jClass, jlong ctx)
{
    MetalContext *pCtx = (MetalContext*) jlong_to_ptr(ctx);

    MetalMesh* mesh = ([[MetalMesh alloc] createMesh:pCtx]);
    return ptr_to_jlong(mesh);
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nReleaseMTLMesh
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nReleaseMTLMesh
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMesh)
{
    MetalMesh *mesh = (MetalMesh *) jlong_to_ptr(nativeMesh);
    if (mesh != nil) {
        [mesh release];
        mesh = nil;
    }
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nBuildNativeGeometryShort
 * Signature: (JJ[FI[SI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_mtl_MTLContext_nBuildNativeGeometryShort
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMesh,
    jfloatArray vb, jint vbSize, jshortArray ib, jint ibSize)
{
    MetalMesh *mesh = (MetalMesh *) jlong_to_ptr(nativeMesh);

    if (vbSize < 0 || ibSize < 0) {
        return JNI_FALSE;
    }

    unsigned int uvbSize = (unsigned int) vbSize;
    unsigned int uibSize = (unsigned int) ibSize;
    unsigned int vertexBufferSize = (*env)->GetArrayLength(env, vb);
    unsigned int indexBufferSize = (*env)->GetArrayLength(env, ib);

    if (uvbSize > vertexBufferSize || uibSize > indexBufferSize) {
        return JNI_FALSE;
    }

    float *vertexBuffer = (float *) ((*env)->GetPrimitiveArrayCritical(env, vb, 0));
    if (vertexBuffer == NULL) {
        return JNI_FALSE;
    }

    unsigned short *indexBuffer = (unsigned short *) ((*env)->GetPrimitiveArrayCritical(env, ib, 0));
    if (indexBuffer == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, vb, vertexBuffer, 0);
        return JNI_FALSE;
    }

    bool result = [mesh buildBuffersShort:vertexBuffer
                                    vSize:uvbSize
                                  iBuffer:indexBuffer
                                    iSize:uibSize];
    (*env)->ReleasePrimitiveArrayCritical(env, ib, indexBuffer, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, vb, vertexBuffer, 0);

    return result;
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nBuildNativeGeometryInt
 * Signature: (JJ[FI[II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_mtl_MTLContext_nBuildNativeGeometryInt
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMesh,
    jfloatArray vb, jint vbSize, jintArray ib, jint ibSize)
{
    MetalMesh *mesh = (MetalMesh *) jlong_to_ptr(nativeMesh);

    if (vbSize < 0 || ibSize < 0) {
        return JNI_FALSE;
    }

    unsigned int uvbSize = (unsigned int) vbSize;
    unsigned int uibSize = (unsigned int) ibSize;
    unsigned int vertexBufferSize = (*env)->GetArrayLength(env, vb);
    unsigned int indexBufferSize = (*env)->GetArrayLength(env, ib);

    if (uvbSize > vertexBufferSize || uibSize > indexBufferSize) {
        return JNI_FALSE;
    }

    float *vertexBuffer = (float *) ((*env)->GetPrimitiveArrayCritical(env, vb, 0));
    if (vertexBuffer == NULL) {
        return JNI_FALSE;
    }

    unsigned int *indexBuffer = (unsigned int *) ((*env)->GetPrimitiveArrayCritical(env, ib, 0));
    if (indexBuffer == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, vb, vertexBuffer, 0);
        return JNI_FALSE;
    }

    bool result = [mesh buildBuffersInt:vertexBuffer
                                  vSize:uvbSize
                                iBuffer:indexBuffer
                                  iSize:uibSize];
    (*env)->ReleasePrimitiveArrayCritical(env, ib, indexBuffer, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, vb, vertexBuffer, 0);

    return result;
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nCreateMTLPhongMaterial
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLContext_nCreateMTLPhongMaterial
    (JNIEnv *env, jclass jClass, jlong ctx)
{
    MetalContext *pCtx = (MetalContext*) jlong_to_ptr(ctx);
    MetalPhongMaterial *phongMaterial = ([[MetalPhongMaterial alloc] createPhongMaterial:pCtx]);
    return ptr_to_jlong(phongMaterial);
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nReleaseMTLPhongMaterial
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nReleaseMTLPhongMaterial
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativePhongMaterial)
{
    MetalPhongMaterial *phongMaterial = (MetalPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    if (phongMaterial != nil) {
        [phongMaterial release];
        phongMaterial = nil;
    }
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetDiffuseColor
 * Signature: (JJFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetDiffuseColor
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativePhongMaterial,
    jfloat r, jfloat g, jfloat b, jfloat a)
{
    MetalPhongMaterial *phongMaterial = (MetalPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    [phongMaterial setDiffuseColor:r g:g b:b a:a];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetSpecularColor
 * Signature: (JJZFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetSpecularColor
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativePhongMaterial,
    jboolean set, jfloat r, jfloat g, jfloat b, jfloat a)
{
    MetalPhongMaterial *phongMaterial = (MetalPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    bool specularSet = set ? true : false;
    [phongMaterial setSpecularColor:specularSet r:r g:g b:b a:a];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetMap
 * Signature: (JJIJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetMap
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativePhongMaterial,
    jint mapType, jlong nativeTexture)
{
    MetalPhongMaterial *phongMaterial = (MetalPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    MetalTexture *texMap = (MetalTexture *)  jlong_to_ptr(nativeTexture);

    [phongMaterial setMap:mapType map:[texMap getTexture]];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nCreateMTLMeshView
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLContext_nCreateMTLMeshView
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMesh)
{
    MetalContext *pCtx = (MetalContext*) jlong_to_ptr(ctx);

    MetalMesh *pMesh = (MetalMesh *) jlong_to_ptr(nativeMesh);

    MetalMeshView* meshView = ([[MetalMeshView alloc] createMeshView:pCtx
                                                                mesh:pMesh]);
    return ptr_to_jlong(meshView);
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nReleaseMTLMeshView
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nReleaseMTLMeshView
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView)
{
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);
    if (meshView != nil) {
        [meshView release];
        meshView = nil;
    }
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetCullingMode
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetCullingMode
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView, jint cullMode)
{
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);

    switch (cullMode) {
        case com_sun_prism_mtl_MTLContext_CULL_BACK:
            cullMode = MTLCullModeBack;
            break;
        case com_sun_prism_mtl_MTLContext_CULL_FRONT:
            cullMode = MTLCullModeFront;
            break;
        case com_sun_prism_mtl_MTLContext_CULL_NONE:
            cullMode = MTLCullModeNone;
            break;
    }
    [meshView setCullingMode:cullMode];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetMaterial
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetMaterial
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView, jlong nativePhongMaterial)
{
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);

    MetalPhongMaterial *phongMaterial = (MetalPhongMaterial *) jlong_to_ptr(nativePhongMaterial);
    [meshView setMaterial:phongMaterial];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetWireframe
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetWireframe
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView, jboolean wireframe)
{
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);
    bool isWireFrame = wireframe ? true : false;
    [meshView setWireframe:isWireFrame];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetAmbientLight
 * Signature: (JJFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetAmbientLight
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView,
    jfloat r, jfloat g, jfloat b)
{
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);
    [meshView setAmbientLight:r g:g b:b];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetLight
 * Signature: (JJIFFFFFFFFFFFFFFFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetLight
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView, jint index,
    jfloat x, jfloat y, jfloat z, jfloat r, jfloat g, jfloat b, jfloat w,
    jfloat ca, jfloat la, jfloat qa, jfloat isAttenuated, jfloat range,
    jfloat dirX, jfloat dirY, jfloat dirZ, jfloat innerAngle, jfloat outerAngle, jfloat falloff)
{
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);
    [meshView setLight:index
        x:x y:y z:z
        r:r g:g b:b w:w
        ca:ca la:la qa:qa
        isA:isAttenuated range:range
        dirX:dirX dirY:dirY dirZ:dirZ
        inA:innerAngle outA:outerAngle
        falloff:falloff];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nRenderMeshView
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nRenderMeshView
    (JNIEnv *env, jclass jClass, jlong ctx, jlong nativeMeshView)
{
    MetalContext *pCtx = (MetalContext*) jlong_to_ptr(ctx);
    MetalMeshView *meshView = (MetalMeshView *) jlong_to_ptr(nativeMeshView);
    [pCtx renderMeshView:meshView];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nIsCurrentRTT
 * Signature: (JJ[[)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_mtl_MTLContext_nIsCurrentRTT
    (JNIEnv *env, jclass jClass, jlong ctx, jlong texPtr)
{
    MetalContext *pCtx = (MetalContext*)jlong_to_ptr(ctx);
    MetalRTTexture *rttPtr = (MetalRTTexture *)jlong_to_ptr(texPtr);
    return [pCtx isCurrentRTT:rttPtr];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nBlit
 * Signature: (JJJIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nBlit
    (JNIEnv *env, jclass jclass, jlong ctx, jlong nSrcRTT, jlong nDstRTT,
    jint srcX0, jint srcY0, jint srcX1, jint srcY1,
    jint dstX0, jint dstY0, jint dstX1, jint dstY1)
{
    MetalContext *pCtx = (MetalContext*)jlong_to_ptr(ctx);
    MetalRTTexture *srcRTT = (MetalRTTexture *)jlong_to_ptr(nSrcRTT);
    MetalRTTexture *dstRTT = (MetalRTTexture *)jlong_to_ptr(nDstRTT);

    id<MTLTexture> src = [srcRTT getTexture];
    id<MTLTexture> dst = [dstRTT getTexture];

    [pCtx blit:src srcX0:srcX0 srcY0:srcY0 srcX1:srcX1 srcY1:srcY1
        dstTex:dst dstX0:dstX0 dstY0:dstY0 dstX1:dstX1 dstY1:dstY1];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetCameraPosition
 * Signature: (JDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetCameraPosition
    (JNIEnv *env, jclass jClass, jlong ctx,
    jdouble x, jdouble y, jdouble z)
{
    MetalContext *pCtx = (MetalContext*)jlong_to_ptr(ctx);
    [pCtx setCameraPosition:x y:y z:z];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nSetCompositeMode
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLContext_nSetCompositeMode(
    JNIEnv *env, jclass jClass, jlong context, jint mode)
{
    MetalContext* pCtx = (MetalContext*)jlong_to_ptr(context);
    [pCtx setCompositeMode:mode];
}

/*
 * Class:     com_sun_prism_mtl_MTLContext
 * Method:    nGetCommandQueue
 * Signature: (J)J
 */
// This enables sharing of MTLCommandQueue between PRISM and GLASS, if needed.
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLContext_nGetCommandQueue
    (JNIEnv *env, jclass jClass, jlong context)
{
    MetalContext *contextPtr = (MetalContext *)jlong_to_ptr(context);
    jlong jPtr = ptr_to_jlong((void *)[contextPtr getCommandQueue]);
    return jPtr;
}

// MTLGraphics methods
/*
 * Class:     com_sun_prism_mtl_MTLGraphics
 * Method:    nClear
 * Signature: (JFFFFZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLGraphics_nClear
    (JNIEnv *env, jclass jClass, jlong ctx,
    jfloat red, jfloat green, jfloat blue, jfloat alpha, jboolean clearDepth)
{
    MetalContext* context = (MetalContext*)jlong_to_ptr(ctx);
    [context clearRTT:red
                green:green
                 blue:blue
                alpha:alpha
           clearDepth:clearDepth];
}


// MTLResourceFactory methods

/*
 * Class:     com_sun_prism_mtl_MTLResourceFactory
 * Method:    nCreateTexture
 * Signature: (JIIZIIIZ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLResourceFactory_nCreateTexture
    (JNIEnv *env, jclass class, jlong pContext, jint format, jint hint,
    jboolean isRTT, jint width, jint height, jint samples, jboolean useMipmap)
{
    MetalContext* context = (MetalContext*)jlong_to_ptr(pContext);
    jlong rtt = ptr_to_jlong([[MetalTexture alloc] createTexture:context
                                                         ofWidth:width
                                                        ofHeight:height
                                                     pixelFormat:format
                                                       useMipMap:useMipmap]);
    return rtt;
}

/*
 * Class:     com_sun_prism_mtl_MTLResourceFactory
 * Method:    nReleaseTexture
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLResourceFactory_nReleaseTexture
    (JNIEnv *env, jclass class, jlong pTexture)
{
    MetalTexture* pTex = (MetalTexture*)jlong_to_ptr(pTexture);
    [pTex release];
    pTex = nil;
}


// MTLPipeline methods

/*
 * Class:     com_sun_prism_mtl_MTLPipeline
 * Method:    nSupportsMTL
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_mtl_MTLPipeline_nSupportsMTL
    (JNIEnv *env, jclass jClass)
{
    id<MTLDevice> device = MTLCreateSystemDefaultDevice();
    // The Prism MTL pipeline requires MTLGPUFamilyMac2
    if ([device supportsFamily:MTLGPUFamilyMac2]) {
        return true;
    }
    return false;
}
