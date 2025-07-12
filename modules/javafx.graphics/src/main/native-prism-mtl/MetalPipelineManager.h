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

#ifndef METAL_PIPELINE_MANAGER_H
#define METAL_PIPELINE_MANAGER_H

#import "MetalContext.h"

/**
 * native interface for the Java class MTLPipelineManager
 */
@interface MetalPipelineManager : NSObject
{
    id<MTLLibrary> shaderLib;
    id<MTLFunction> vertexFunction;
    MetalContext *context;
    NSMutableDictionary *clearRttPipeStateNoDepthDict;
    NSMutableDictionary *clearRttPipeStateDepthDict;
    NSMutableDictionary *phongPipelineStateNonMSAANoDepthDict;
    NSMutableDictionary *phongPipelineStateNonMSAADepthDict;
    NSMutableDictionary *phongPipelineStateMSAANoDepthDict;
    NSMutableDictionary *phongPipelineStateMSAADepthDict;
    id<MTLDepthStencilState> depthStencilState[2]; // [0] - disabled, [1] - enabled
    id<MTLComputePipelineState> uyvy422ToRGBAState;
}

- (void) init:(MetalContext*)ctx
      libData:(dispatch_data_t)libData;
- (id<MTLFunction>) getFunction:(NSString*)funcName;
- (id<MTLRenderPipelineState>) getClearRttPipeState;
- (id<MTLRenderPipelineState>) getPipeStateWithFragFunc:(id<MTLFunction>)fragFunc
                                          compositeMode:(int)compositeMode;
- (id<MTLRenderPipelineState>) getPhongPipeStateWithNumLights:(int)numLights
                                                compositeMode:(int)compositeMode;
- (id<MTLComputePipelineState>) getComputePipelineStateWithFunc:(NSString*)funcName;
- (id<MTLDepthStencilState>) getDepthStencilState;
- (void) setPipelineCompositeBlendMode:(MTLRenderPipelineDescriptor*)pipeDesc
                         compositeMode:(int)compositeMode;
- (void) dealloc;
@end

#endif
