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

#import "MetalMesh.h"

typedef struct
{
    vector_float4 position;
    vector_float4 color;
} MBEVertex;

@implementation MetalMesh

- (id) createMesh:(MetalContext*)ctx
{
    self = [super init];
    if (self) {
        context = ctx;
        numVertices = 0;
        numIndices = 0;
        indexType = MTLIndexTypeUInt16;
    }
    return self;
}

- (bool) buildBuffersShort:(float*)vb
                     vSize:(unsigned int)vbSize
                   iBuffer:(unsigned short*)ib
                     iSize:(unsigned int)ibSize
{
    id<MTLDevice> device = [context getDevice];
    unsigned int size = vbSize * sizeof (float);
    unsigned int vbCount = vbSize / NUM_OF_FLOATS_PER_VERTEX;

    if (numVertices != vbCount) {
        [self releaseVertexBuffer];
        [self createVertexBuffer:size];
        numVertices = vbCount;
    }

    NSUInteger currentIndex = [context getCurrentBufferIndex];
    if (vertexBuffer[currentIndex] != nil) {
        memcpy(vertexBuffer[currentIndex].contents, vb, size);
    }

    size = ibSize * sizeof (unsigned short);
    if (numIndices != ibSize) {
        [self releaseIndexBuffer];
        [self createIndexBuffer:size];
        numIndices = ibSize;
    }

    if (indexBuffer[currentIndex] != nil) {
        memcpy(indexBuffer[currentIndex].contents, ib, size);
    }
    indexType = MTLIndexTypeUInt16;
    return true;
}

- (bool) buildBuffersInt:(float*)vb
                   vSize:(unsigned int)vbSize
                 iBuffer:(unsigned int*)ib
                   iSize:(unsigned int)ibSize
{
    id<MTLDevice> device = [context getDevice];
    unsigned int size = vbSize * sizeof (float);
    unsigned int vbCount = vbSize / NUM_OF_FLOATS_PER_VERTEX;

    if (numVertices != vbCount) {
        [self releaseVertexBuffer];
        [self createVertexBuffer:size];
        numVertices = vbCount;
    }

    NSUInteger currentIndex = [context getCurrentBufferIndex];
    if (vertexBuffer[currentIndex] != nil) {
        memcpy(vertexBuffer[currentIndex].contents, vb, size);
    }

    size = ibSize * sizeof (unsigned int);
    if (numIndices != ibSize) {
        [self releaseIndexBuffer];
        [self createIndexBuffer:size];
        numIndices = ibSize;
    }

    if (indexBuffer[currentIndex] != nil) {
        memcpy(indexBuffer[currentIndex].contents, ib, size);
    }

    indexType = MTLIndexTypeUInt32;
    return true;
}

- (void) release
{
    [self releaseVertexBuffer];
    [self releaseIndexBuffer];
    context = nil;
}

- (void) createVertexBuffer:(unsigned int)size;
{
    id<MTLDevice> device = [context getDevice];
    for (int i = 0; i < BUFFER_SIZE; i++) {
        vertexBuffer[i] = [device newBufferWithLength:size
            options:MTLResourceStorageModeShared];
    }
}

- (void) releaseVertexBuffer
{
    for (int i = 0; i < BUFFER_SIZE; i++) {
        vertexBuffer[i] = nil;
    }
    numVertices = 0;
}

- (void) createIndexBuffer:(unsigned int)size;
{
    id<MTLDevice> device = [context getDevice];
    for (int i = 0; i < BUFFER_SIZE; i++) {
        indexBuffer[i] = [device newBufferWithLength:size
            options:MTLResourceStorageModeShared];
    }
}

- (void) releaseIndexBuffer
{
    for (int i = 0; i < BUFFER_SIZE; i++) {
        indexBuffer[i] = nil;
    }
    numIndices = 0;
}

- (id<MTLBuffer>) getVertexBuffer
{
    return vertexBuffer[[context getCurrentBufferIndex]];
}

- (id<MTLBuffer>) getIndexBuffer
{
    return indexBuffer[[context getCurrentBufferIndex]];
}

- (NSUInteger) getNumVertices
{
    return numVertices;
}

- (NSUInteger) getNumIndices
{
    return numIndices;
}

- (NSUInteger) getIndexType
{
    return indexType;
}
@end // MetalMesh
