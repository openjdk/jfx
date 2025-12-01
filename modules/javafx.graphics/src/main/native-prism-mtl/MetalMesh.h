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

#ifndef METAL_MESH_H
#define METAL_MESH_H

#import "MetalContext.h"

#define NUM_OF_FLOATS_PER_VERTEX 9
#define MESH_INDEX_LIMIT 9000000

@interface MetalMesh : NSObject
{
    MetalContext *context;
    id<MTLBuffer> indexBuffer[3];
    id<MTLBuffer> vertexBuffer[3];
    NSUInteger numVertices;
    NSUInteger numIndices;
    NSUInteger indexType;
}

- (id) createMesh:(MetalContext*)ctx;
- (bool) buildBuffersShort:(float*)vb
                     vSize:(unsigned int)vbSize
                   iBuffer:(unsigned short*)ib
                     iSize:(unsigned int)ibSize;
- (bool) buildBuffersInt:(float*)vb
                   vSize:(unsigned int)vbSize
                 iBuffer:(unsigned int*)ib
                   iSize:(unsigned int)ibSize;
- (void) release;
- (void) createVertexBuffer:(unsigned int)size;
- (void) releaseVertexBuffer;
- (void) createIndexBuffer:(unsigned int)size;
- (void) releaseIndexBuffer;
- (id<MTLBuffer>) getVertexBuffer;
- (id<MTLBuffer>) getIndexBuffer;
- (NSUInteger) getNumVertices;
- (NSUInteger) getNumIndices;
- (NSUInteger) getIndexType;
@end

#endif
