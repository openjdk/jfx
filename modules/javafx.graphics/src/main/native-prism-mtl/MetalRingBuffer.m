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

#import "MetalRingBuffer.h"
#import "MetalContext.h"

@implementation MetalRingBuffer

static bool isBufferInUse[NUM_BUFFERS];
static unsigned int currentBufferIndex;

- (MetalRingBuffer*) init:(MetalContext*)ctx
                   ofSize:(unsigned int)size {

    self = [super init];
    if (self) {
        bufferSize = size;
        currentOffset = 0;
        numReservedBytes = 0;
        currentBufferIndex = 0;
        if (@available(macOS 13, *)) {
            bufferOffsetAlignment = 32;
        } else {
            // MacOS 12 requires the offset aligment to be 256 bytes for the method
            // MTLRenderCommandEncoder.setVertexBuffer.
            bufferOffsetAlignment = 256;
        }

        for (int i = 0; i < NUM_BUFFERS; i++) {
            isBufferInUse[i] = false;
            buffer[i] = [[ctx getDevice] newBufferWithLength:bufferSize
                                                     options:MTLResourceStorageModeShared];
            buffer[i].label = [NSString stringWithFormat:@"JFX Ring Buffer"];
        }
    }
    return self;
}

+ (bool) isBufferAvailable {
    for (int i = 0; i < NUM_BUFFERS; i++) {
        if (!isBufferInUse[i]) {
            return true;
        }
    }
    return false;
}

// This method assumes that caller has made sure that a buffer is available
// by calling the method isBufferAvailable().
// If there is no buffer available then the behavior is undefined and
// should cause visual artefacts or Metal validation may fail or crash.

+ (unsigned int) updateBufferInUse {
    unsigned int prevBufferIndex = currentBufferIndex;
    for (int i = currentBufferIndex + 1; i < NUM_BUFFERS; i++) {
        if (!isBufferInUse[i]) {
            currentBufferIndex = i;
        }
    }
    if (prevBufferIndex == currentBufferIndex) {
        for (int i = 0; i < currentBufferIndex; i++) {
            if (!isBufferInUse[i]) {
                currentBufferIndex = i;
            }
        }
    }
    isBufferInUse[currentBufferIndex] = true;
    return currentBufferIndex;
}

+ (unsigned int) getCurrentBufferIndex {
    return currentBufferIndex;
}

+ (void) resetBuffer:(unsigned int)index {
    isBufferInUse[index] = false;
}

- (void) resetOffsets
{
    currentOffset = 0;
    numReservedBytes = 0;
}

- (id<MTLBuffer>) getBuffer {
    return [self getCurrentBuffer];
}

- (id<MTLBuffer>) getCurrentBuffer {
    return buffer[currentBufferIndex];
}

- (int) reserveBytes:(unsigned int)length {
    int prevOffset = currentOffset;
    currentOffset = numReservedBytes;
    unsigned int remainder = currentOffset % bufferOffsetAlignment;
    if (remainder != 0) {
        currentOffset = currentOffset + bufferOffsetAlignment - remainder;
    }

    if (currentOffset > bufferSize || length > (bufferSize - currentOffset)) {
        // RingBuffer overflows with requested length.
        currentOffset = prevOffset;
        return -1;
    }
    numReservedBytes = currentOffset + length;
    return currentOffset;
}

- (unsigned int) getNumReservedBytes {
    return numReservedBytes;
}

- (void) dealloc {
    for (int i = 0; i < NUM_BUFFERS; i++) {
        [buffer[i] release];
        buffer[i] = nil;
    }
    [super dealloc];
}

@end
