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

#ifndef METAL_SHADER_H
#define METAL_SHADER_H

#import "MetalContext.h"
#import "MetalTexture.h"
#import "MetalPipelineManager.h"

@interface MetalShader : NSObject
{
    bool argsUpdated;
    MetalContext *context;
    NSString* fragFuncName;
    id<MTLFunction> fragmentFunction;

    NSMutableDictionary *pipeStateNonMSAANoDepthDict;
    NSMutableDictionary *pipeStateNonMSAADepthDict;
    NSMutableDictionary *pipeStateMSAANoDepthDict;
    NSMutableDictionary *pipeStateMSAADepthDict;

    NSDictionary* fragArgIndicesDict;
    NSMutableDictionary* fragTexArgsDict;
    NSMutableDictionary* fragTexSamplerDict;

    id<MTLArgumentEncoder> argumentEncoder;
    id<MTLBuffer> argumentBuffer;
    NSUInteger argumentBufferLength;
    int currentRingBufferOffset; // offset in the MetalRingBuffer
    id<MTLBuffer> argumentBufferForCB;
}

- (void) setArgsUpdated:(bool)updated;
- (jobject) getUniformNameIdMap:(JNIEnv*)env;
- (id) initWithContext:(MetalContext*)ctx
          withFragFunc:(NSString*)fragName;
- (id<MTLRenderPipelineState>) getPipelineState:(bool)isMSAA
                                  compositeMode:(int)compositeMode;

- (void) copyArgBufferToRingBuffer;
- (NSUInteger) getArgumentBufferLength;
- (int) getRingBufferOffset;
- (id<MTLBuffer>) getRingBuffer;

- (NSMutableDictionary*) getTexutresDict;
- (NSMutableDictionary*) getSamplersDict;

- (NSUInteger) getArgumentID:(NSString*)name;
- (void) enable;
- (void) disable;

- (void) setTexture:(int)texID
          uniformID:(int)uniformID
            texture:(id<MTLTexture>)texture
           isLinear:(bool)isLinear
           wrapMode:(int)wrapMode;

- (void) setInt:(int)uniformID i0:(int)i0;

- (void) setFloat1:(int)uniformID f0:(float)f0;
- (void) setFloat2:(int)uniformID f0:(float)f0 f1:(float)f1;
- (void) setFloat3:(int)uniformID f0:(float)f0 f1:(float)f1 f2:(float)f2;
- (void) setFloat4:(int)uniformID f0:(float)f0 f1:(float)f1 f2:(float)f2  f3:(float)f3;

- (void) setConstants:(int)uniformID values:(float[])values size:(int)size;
- (void) dealloc;

@end

#endif
