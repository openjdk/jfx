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

#ifndef METAL_RTTEXTURE_H
#define METAL_RTTEXTURE_H

#import "MetalContext.h"
#import "MetalTexture.h"

@interface MetalRTTexture : MetalTexture
{
    NSUInteger contentWidth;
    NSUInteger contentHeight;
    NSUInteger physicalWidth;
    NSUInteger physicalHeight;

    id<MTLTexture> depthTexture;
    id<MTLTexture> depthMSAATexture;
    id<MTLTexture> msaaTexture;
    BOOL isMSAA;
    BOOL lastDepthMSAA;
}

- (id<MTLTexture>) getTexture;
- (id<MTLTexture>) getDepthTexture;
- (id<MTLTexture>) getDepthMSAATexture;
- (id<MTLTexture>) getMSAATexture;
- (BOOL) isMSAAEnabled;

- (MetalRTTexture*) createTexture:(MetalContext*)ctx
                  ofPhysicalWidth:(NSUInteger)pw
                 ofPhysicalHeight:(NSUInteger)ph
                   ofContentWidth:(NSUInteger)cw
                  ofContentHeight:(NSUInteger)ch
                           isMsaa:(BOOL)isMsaa;

- (MetalRTTexture*) createTexture:(MetalContext*)ctx
                  ofPhysicalWidth:(NSUInteger)pw
                 ofPhysicalHeight:(NSUInteger)ph
                           mtlTex:(long)pTex;

- (void) initRTT:(int*)arr;
- (void) readPixels:(int*)pDst;
- (void) readPixelsFromRTT:(int*)pDst;

- (void) createDepthTexture;
- (void) dealloc;

@end

#endif
