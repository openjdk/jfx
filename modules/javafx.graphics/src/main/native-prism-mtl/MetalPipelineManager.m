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

#import "MetalPipelineManager.h"
#import "MetalRTTexture.h"
#include "com_sun_prism_mtl_MTLContext.h"

// ---------------------------- Debug helper for Developers -------------------------
//
// This implementation is to utilize "Metal Debuger" present in Xcode.
// See - https://developer.apple.com/documentation/xcode/capturing-a-metal-workload-programmatically
//
// Limitation - Attaching to a java process and debugging frame by frame is not possible.
//
// Currently, this debug helper can capture each frame related data to a file.
// It is configured to capture frame data from MTLDevice for entire lifetime of the application.
// This creates a trace file which is in GBs. So, please use this debug helper judiciously
// (i.e. with applications running for a short time and with less animation.)
// The generated trace file can be opened in Xcode and replayed.
//
// If needed, this can be changed in future to capture only "scope of interest" as explained at -
// https://developer.apple.com/documentation/xcode/creating-and-using-custom-capture-scopes
//
// Prerequisites:
// 1. MacOS 14 (Sonoma)
// 2. On terminal, set environment variable MTL_CAPTURE_ENABLED=1.
//
// Uncomment below line to capture Metal GPU Debug Trace to file GPUTraceFilename specified below.
//#define JFX_MTL_DEBUG_CAPTURE
#ifdef JFX_MTL_DEBUG_CAPTURE
NSString *GPUTraceFilename = @"file:///tmp/fx_metal.gputrace";
#endif
// ---------------------------- Debug helper for Developers -------------------------

@implementation MetalPipelineManager

- (void) init:(MetalContext*)ctx
      libData:(dispatch_data_t)libData
{
    context = ctx;
    NSError *error = nil;
    shaderLib = [[context getDevice] newLibraryWithData:libData error:&error];

    if (shaderLib != nil) {
        vertexFunction = [self getFunction:@"passThrough"];
    } else {
        NSLog(@"MetalPipelineManager.init: Failed to create shader library");
    }

    clearRttPipeStateNoDepthDict = [[NSMutableDictionary alloc] init];
    clearRttPipeStateDepthDict = [[NSMutableDictionary alloc] init];
    phongPipelineStateNonMSAANoDepthDict = [[NSMutableDictionary alloc] init];
    phongPipelineStateNonMSAADepthDict = [[NSMutableDictionary alloc] init];
    phongPipelineStateMSAANoDepthDict = [[NSMutableDictionary alloc] init];
    phongPipelineStateMSAADepthDict = [[NSMutableDictionary alloc] init];
    uyvy422ToRGBAState = nil;

    // Create and cache 2 possible depthStencilStates
    @autoreleasepool {
        MTLDepthStencilDescriptor *depthStencilDescriptor = [[MTLDepthStencilDescriptor new] autorelease];
        depthStencilDescriptor.depthCompareFunction = MTLCompareFunctionAlways;
        depthStencilDescriptor.depthWriteEnabled = NO;
        depthStencilState[0] = [[context getDevice] newDepthStencilStateWithDescriptor:depthStencilDescriptor];

        depthStencilDescriptor.depthCompareFunction = MTLCompareFunctionLessEqual;
        depthStencilDescriptor.depthWriteEnabled = YES;
        depthStencilState[1] = [[context getDevice] newDepthStencilStateWithDescriptor:depthStencilDescriptor];
    }
#ifdef JFX_MTL_DEBUG_CAPTURE

    if (@available(macOS 14, *)) {
        @autoreleasepool {
            NSLog(@"JFX_MTL_DEBUG_CAPTURE is enabled");

            MTLCaptureManager* captureManager = [MTLCaptureManager sharedCaptureManager];
            if (![captureManager supportsDestination:MTLCaptureDestinationGPUTraceDocument]) {
                NSLog(@"MTLCaptureDestinationGPUTraceDocument destination is not supported.");
            } else {
                NSLog(@"MTLCaptureDestinationGPUTraceDocument destination is supported.");
                MTLCaptureDescriptor* captureDescriptor = [MTLCaptureDescriptor new];
                [captureDescriptor setCaptureObject: [context getDevice]];
                [captureDescriptor setDestination:MTLCaptureDestinationGPUTraceDocument];

                NSURL* url = [NSURL URLWithString:GPUTraceFilename];
                [captureDescriptor setOutputURL:url];
                [captureManager startCaptureWithDescriptor:captureDescriptor error:nil];
            }
        }
    } else {
        NSLog(@"MTL_CAPTURE_ENABLED is available only in macOS 14 and later versions");
    }
#endif
}

- (id<MTLFunction>) getFunction:(NSString*)funcName
{
    return [shaderLib newFunctionWithName:funcName];
}

- (id<MTLRenderPipelineState>) getClearRttPipeState
{
    int sampleCount = 1;
    if ([[context getRTT] isMSAAEnabled]) {
        sampleCount = 4;
    }
    NSNumber *keySampleCount = [NSNumber numberWithInt:sampleCount];
    id<MTLRenderPipelineState> clearRttPipeState;
    if ([context clearDepth]) {
        clearRttPipeState = clearRttPipeStateDepthDict[keySampleCount];
    } else {
        clearRttPipeState = clearRttPipeStateNoDepthDict[keySampleCount];
    }
    if (clearRttPipeState == nil) {
        MTLRenderPipelineDescriptor* pipeDesc = [[MTLRenderPipelineDescriptor alloc] init];
        pipeDesc.vertexFunction   = [self getFunction:@"clearVF"];
        pipeDesc.fragmentFunction = [self getFunction:@"clearFF"];
        pipeDesc.colorAttachments[0].pixelFormat = [[context getRTT] getPixelFormat];
        pipeDesc.sampleCount = sampleCount;
        if ([context clearDepth]) {
            pipeDesc.depthAttachmentPixelFormat = MTLPixelFormatDepth32Float;
        } else {
            pipeDesc.depthAttachmentPixelFormat = MTLPixelFormatInvalid;
        }

        NSError* error;
        clearRttPipeState = [[context getDevice] newRenderPipelineStateWithDescriptor:pipeDesc error:&error];
        [pipeDesc release];
        pipeDesc = nil;
        NSAssert(clearRttPipeState, @"Failed to create clear pipeline state: %@", error);
        if ([context clearDepth]) {
            [clearRttPipeStateDepthDict setObject:clearRttPipeState forKey:keySampleCount];
        } else {
            [clearRttPipeStateNoDepthDict setObject:clearRttPipeState forKey:keySampleCount];
        }
    }
    return clearRttPipeState;
}

- (id<MTLRenderPipelineState>) getPipeStateWithFragFunc:(id<MTLFunction>)func
                                          compositeMode:(int)compositeMode
{
    NSError* error;
    MTLRenderPipelineDescriptor* pipeDesc = [[MTLRenderPipelineDescriptor alloc] init];
    pipeDesc.vertexFunction = vertexFunction;
    pipeDesc.fragmentFunction = func;
    pipeDesc.colorAttachments[0].pixelFormat = [[context getRTT] getPixelFormat];

    if ([context isDepthEnabled]) {
        pipeDesc.depthAttachmentPixelFormat = MTLPixelFormatDepth32Float;
    } else {
        pipeDesc.depthAttachmentPixelFormat = MTLPixelFormatInvalid;
    }

    if ([[context getRTT] isMSAAEnabled]) {
        pipeDesc.sampleCount = 4;
    } else {
        pipeDesc.sampleCount = 1;
    }

    [self setPipelineCompositeBlendMode:pipeDesc
                          compositeMode:compositeMode];

    id<MTLRenderPipelineState> pipeState = [[context getDevice] newRenderPipelineStateWithDescriptor:pipeDesc error:&error];
    [pipeDesc release];
    pipeDesc = nil;
    NSAssert(pipeState, @"Failed to create pipeline state to render to texture: %@", error);

    return pipeState;
}

- (id<MTLComputePipelineState>) getComputePipelineStateWithFunc:(NSString*)funcName
{
    if (uyvy422ToRGBAState == nil) {
        NSError* error;
        id<MTLFunction> kernelFunction = [self getFunction:funcName];
        uyvy422ToRGBAState = [[context getDevice] newComputePipelineStateWithFunction:kernelFunction
                                                                       error:&error];
        NSAssert(uyvy422ToRGBAState, @"Failed to create compute pipeline state: %@", error);
    }

    return uyvy422ToRGBAState;
}

- (id<MTLRenderPipelineState>) getPhongPipeStateWithNumLights:(int)numLights
                                                compositeMode:(int)compositeMode;
{
    NSError* error;
    NSMutableDictionary *psDict;
    if ([[context getRTT] isMSAAEnabled]) {
        if ([context isDepthEnabled]) {
            psDict = phongPipelineStateMSAADepthDict;
        } else {
            psDict = phongPipelineStateMSAANoDepthDict;
        }
    } else {
        if ([context isDepthEnabled]) {
            psDict = phongPipelineStateNonMSAADepthDict;
        } else {
            psDict = phongPipelineStateNonMSAANoDepthDict;
        }
    }

    NSNumber *keyCompMode = [NSNumber numberWithInt:(compositeMode << 8) | numLights];
    id<MTLRenderPipelineState> pipeState = psDict[keyCompMode];
    if (pipeState == nil) {

        NSString *vertFuncName = [[NSString alloc] initWithFormat:@"PhongVS%d", numLights];
        NSString *fragFuncName = [[NSString alloc] initWithFormat:@"PhongPS%d", numLights];

        MTLRenderPipelineDescriptor* pipeDesc = [[MTLRenderPipelineDescriptor alloc] init];
        pipeDesc.vertexFunction = [self getFunction:vertFuncName];
        pipeDesc.fragmentFunction = [self getFunction:fragFuncName];
        pipeDesc.colorAttachments[0].pixelFormat = [[context getRTT] getPixelFormat];
        if ([context isDepthEnabled]) {
            pipeDesc.depthAttachmentPixelFormat = MTLPixelFormatDepth32Float;
        } else {
            pipeDesc.depthAttachmentPixelFormat = MTLPixelFormatInvalid;
        }

        if ([[context getRTT] isMSAAEnabled]) {
            pipeDesc.sampleCount = 4;
        } else {
            pipeDesc.sampleCount = 1;
        }
        [self setPipelineCompositeBlendMode:pipeDesc
                compositeMode:compositeMode];
        pipeState = [[context getDevice]
                newRenderPipelineStateWithDescriptor:pipeDesc error:&error];
        [pipeDesc release];
        pipeDesc = nil;
        [psDict setObject:pipeState forKey:keyCompMode];
        [vertFuncName release];
        [fragFuncName release];
        NSAssert(pipeState, @"Failed to create phong pipeline state: %@", error);
    }
    return pipeState;
}

- (id<MTLDepthStencilState>) getDepthStencilState
{
    if ([context isDepthEnabled]) {
        return depthStencilState[1];
    } else {
        return depthStencilState[0];
    }
}

- (void) setPipelineCompositeBlendMode:(MTLRenderPipelineDescriptor*)pipeDesc
                         compositeMode:(int)compositeMode
{
    MTLBlendFactor srcFactor;
    MTLBlendFactor dstFactor;

    switch(compositeMode) {
        case com_sun_prism_mtl_MTLContext_MTL_COMPMODE_CLEAR:
            srcFactor = MTLBlendFactorZero;
            dstFactor = MTLBlendFactorZero;
            break;

        case com_sun_prism_mtl_MTLContext_MTL_COMPMODE_SRC:
            srcFactor = MTLBlendFactorOne;
            dstFactor = MTLBlendFactorZero;
            break;

        case com_sun_prism_mtl_MTLContext_MTL_COMPMODE_SRCOVER:
            srcFactor = MTLBlendFactorOne;
            dstFactor = MTLBlendFactorOneMinusSourceAlpha;
            break;

        case com_sun_prism_mtl_MTLContext_MTL_COMPMODE_DSTOUT:
            srcFactor = MTLBlendFactorZero;
            dstFactor = MTLBlendFactorOneMinusSourceAlpha;
            break;

        case com_sun_prism_mtl_MTLContext_MTL_COMPMODE_ADD:
            srcFactor = MTLBlendFactorOne;
            dstFactor = MTLBlendFactorOne;
            break;

        default:
            srcFactor = MTLBlendFactorOne;
            dstFactor = MTLBlendFactorOneMinusSourceAlpha;
            break;
    }

    pipeDesc.colorAttachments[0].blendingEnabled = YES;
    pipeDesc.colorAttachments[0].rgbBlendOperation = MTLBlendOperationAdd;
    pipeDesc.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;

    pipeDesc.colorAttachments[0].sourceAlphaBlendFactor = srcFactor;
    pipeDesc.colorAttachments[0].sourceRGBBlendFactor = srcFactor;
    pipeDesc.colorAttachments[0].destinationAlphaBlendFactor = dstFactor;
    pipeDesc.colorAttachments[0].destinationRGBBlendFactor = dstFactor;
}

- (void) dealloc
{
#ifdef JFX_MTL_DEBUG_CAPTURE
    if (@available(macOS 14, *)) {
        NSLog(@"stopping capture...");
        MTLCaptureManager* captureManager = [MTLCaptureManager sharedCaptureManager];
        [captureManager stopCapture];
    }
#endif

    if (shaderLib != nil) {
        [shaderLib release];
        shaderLib = nil;
    }

    if (uyvy422ToRGBAState != nil) {
        [uyvy422ToRGBAState release];
        uyvy422ToRGBAState = nil;
    }

    for (NSNumber *keyPipeState in clearRttPipeStateNoDepthDict) {
        [clearRttPipeStateNoDepthDict[keyPipeState] release];
    }
    for (NSNumber *keyPipeState in clearRttPipeStateDepthDict) {
        [clearRttPipeStateDepthDict[keyPipeState] release];
    }
    for (NSNumber *keyPipeState in phongPipelineStateNonMSAANoDepthDict) {
        [phongPipelineStateNonMSAANoDepthDict[keyPipeState] release];
    }
    for (NSNumber *keyPipeState in phongPipelineStateNonMSAADepthDict) {
        [phongPipelineStateNonMSAADepthDict[keyPipeState] release];
    }
    for (NSNumber *keyPipeState in phongPipelineStateMSAANoDepthDict) {
        [phongPipelineStateMSAANoDepthDict[keyPipeState] release];
    }
    for (NSNumber *keyPipeState in phongPipelineStateMSAADepthDict) {
        [phongPipelineStateMSAADepthDict[keyPipeState] release];
    }

    [clearRttPipeStateNoDepthDict release];
    [clearRttPipeStateDepthDict release];
    [phongPipelineStateNonMSAANoDepthDict release];
    [phongPipelineStateNonMSAADepthDict release];
    [phongPipelineStateMSAANoDepthDict release];
    [phongPipelineStateMSAADepthDict release];
    [super dealloc];
}

@end // MetalPipelineManager
