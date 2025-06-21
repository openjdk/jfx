/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef METAL_RING_BUFFER_H
#define METAL_RING_BUFFER_H

#define NUM_BUFFERS (3)

// The alignment varies for different platforms.
// The alignment value can/should be retrived from device capabilities and updated accordingly.
//
// 1. For fragment function buffer uniforms the offset must be:
//    - multiple of 8 on intel mac,
//    - multiple of 4 for Apple GPU family 2 to 9,
//    - multiple of 32 for Mac2
// 2. BlitEncoder offset: needs to be a multiple of the destination texture’s pixel size.
//
// For more details see metal feature set table and doc of BlitEncoder.copyFromBuffer
// #define BUFFER_OFFSET_ALIGNMENT (32)

@class MetalContext;

#import "MetalCommon.h"

@interface MetalRingBuffer : NSObject
{
    id<MTLBuffer> buffer[NUM_BUFFERS];
    unsigned int currentOffset;
    unsigned int numReservedBytes;
    unsigned int bufferSize;
    unsigned int bufferOffsetAlignment;
}

- (MetalRingBuffer*) init:(MetalContext*)ctx
                   ofSize:(unsigned int)size;
- (void) resetOffsets;
- (id<MTLBuffer>) getBuffer;
- (id<MTLBuffer>) getCurrentBuffer;
- (int)  reserveBytes:(unsigned int)length;
- (unsigned int) getNumReservedBytes;
- (void) dealloc;

+ (unsigned int)  getCurrentBufferIndex;
+ (void) resetBuffer :(unsigned int)index;
+ (bool) isBufferAvailable;
+ (unsigned int) updateBufferInUse;

@end

#endif
