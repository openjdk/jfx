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

#ifndef METAL_TEXTURE_H
#define METAL_TEXTURE_H

#import "MetalContext.h"

enum PFormat {
    PFORMAT_INT_ARGB_PRE  = 0,
    PFORMAT_BYTE_RGBA_PRE = 1,
    PFORMAT_BYTE_BGRA_PRE = 8,
    PFORMAT_BYTE_RGB      = 2,
    PFORMAT_BYTE_GRAY     = 3,
    PFORMAT_BYTE_ALPHA    = 4,
    PFORMAT_MULTI_YV_12   = 5,
    PFORMAT_BYTE_APPL_422 = 6,
    PFORMAT_FLOAT_XYZW    = 7,
};

@interface MetalTexture : NSObject
{
    MetalContext *context;

    id<MTLTexture> texture;
    MTLPixelFormat pixelFormat;

    NSUInteger width;
    NSUInteger height;

    BOOL mipmapped;
}
- (BOOL) isMipmapped;

- (MTLPixelFormat) getPixelFormat;
- (id<MTLBuffer>)  getPixelBuffer;
- (id<MTLTexture>) getTexture;
- (MetalTexture*) createTexture:(MetalContext*)ctx
                        ofWidth:(NSUInteger)w
                       ofHeight:(NSUInteger)h
                    pixelFormat:(NSUInteger)format
                      useMipMap:(BOOL)useMipMap;

- (void) updateTexture:(void*)pixels
                  dstX:(int)dstX
                  dstY:(int)dstY
                  srcX:(int)srcX
                  srcY:(int)srcY
                 width:(int)w
                height:(int)h
            scanStride:(int)scanStride;

- (void) updateTextureYUV422:(char*)pixels
                        dstX:(int)dstX
                        dstY:(int)dstY
                        srcX:(int)srcX
                        srcY:(int)srcY
                       width:(int)w
                      height:(int)h
                  scanStride:(int)scanStride;

- (void) dealloc;

@end

#endif
