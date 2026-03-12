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

#import "GlassMTLOffscreen.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassMTLOffscreen

static NSArray *allModes = nil;

- (id)initWithContext:(id<MTLDevice>)device
         commandQueue:(id<MTLCommandQueue>)commandQueue
          andIsSwPipe:(BOOL)isSwPipe
{
    self = [super init];
    if (self != nil)
    {
        self->_fbo = [[GlassMTLFrameBufferObject alloc] init];
        // if (self->_fbo == nil)
        // {
        //     TODO: implement PBuffer if needed
        //     self->_fbo = [[GlassPBuffer alloc] init];
        // }
        [(GlassMTLFrameBufferObject*)self->_fbo setIsSwPipe:(BOOL)isSwPipe];
        if (allModes == nil) {
            allModes = [[NSArray arrayWithObjects:NSDefaultRunLoopMode,
                                              NSEventTrackingRunLoopMode,
                                              NSModalPanelRunLoopMode, nil] retain];
        }
        self->offScreenCommandQueue = commandQueue;
        self->mtlDevice = device;
    }
    return self;
}

- (void)dealloc
{
    if (self->_fbo != nil) {
        [(NSObject*)self->_fbo release];
        self->_fbo = nil;
        [super dealloc];
    }
}

- (unsigned int)width
{
    return [self->_fbo width];
}

- (unsigned int)height
{
    return [self->_fbo height];
}

- (void)unbind
{
    // no-op in case of MTL
}

- (jlong)fbo
{
    // NSLog(@"Glass fbo = %@", [self->_fbo texture]);
    return ptr_to_jlong((void *)[self->_fbo texture]);
}

- (void)bindForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
    // NSLog(@"GlassMTLOffscreen -------- w x h : %d x %d", width, height);
    [self->_fbo bindForWidth:width
                   andHeight:height];
    CGSize s = {width, height};
    [(CAMetalLayer*)[self getLayer] setDrawableSize:s];
}

- (id<MTLTexture>)getMTLTexture
{
    return [self->_fbo texture];
}

- (void)blitForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
    [self->_fbo blitForWidth:width
                   andHeight:height];
}

- (bool)tryLockTexture {
    return [self->_fbo tryLockTexture];
}

- (void)unlockTexture {
    return [self->_fbo unlockTexture];
}

- (void)flush:(GlassOffscreen*)glassOffScreen
{
    if ([NSThread isMainThread]) {
        [[self getLayer] setNeedsDisplay];
    } else {
        [[self getLayer] performSelectorOnMainThread:@selector(setNeedsDisplay)
                                          withObject:nil
                                       waitUntilDone:NO
                                               modes:allModes];
    }
}

- (void)pushPixels:(void*)pixels
         withWidth:(unsigned int)width
        withHeight:(unsigned int)height
        withScaleX:(float)scalex
        withScaleY:(float)scaley
            ofView:(NSView*)view
{
    id<MTLTexture> backBufferTex = [self->_fbo texture];

    if ((backBufferTex.width != width) ||
        (backBufferTex.height != height)) {
        return;
    }

    @autoreleasepool {
        id<MTLCommandBuffer> commandBuf = [self->offScreenCommandQueue commandBuffer];
        if (commandBuf == nil) {
            return;
        }

        id <MTLBlitCommandEncoder> blitEncoder = [commandBuf blitCommandEncoder];

        id<MTLBuffer> buff = [[self->mtlDevice newBufferWithBytes:pixels
                                                           length:(width * height * 4)
                                                          options:0] autorelease];
        [blitEncoder copyFromBuffer:buff
                       sourceOffset:(NSUInteger)0
                  sourceBytesPerRow:(NSUInteger)width * 4
                sourceBytesPerImage:(NSUInteger)width * height * 4
                         sourceSize:MTLSizeMake(width, height, 1)
                          toTexture:backBufferTex
                   destinationSlice:(NSUInteger)0
                   destinationLevel:(NSUInteger)0
                  destinationOrigin:MTLOriginMake(0, 0, 0)];

        if (backBufferTex.usage == MTLTextureUsageRenderTarget) {
            [blitEncoder synchronizeTexture:backBufferTex slice:0 level:0];
        }
        [blitEncoder endEncoding];
        [commandBuf commit];
        [commandBuf waitUntilCompleted];
    }
}

- (unsigned char)isDirty
{
    // no-op in case of MTL
    return 0;
}

- (void)blit
{
    [self blitForWidth:[self->_fbo width]
             andHeight:[self->_fbo height]];
}

// TODO: MTL: This just creates another texture and doesn't do any blit
- (void)blitFromOffscreen:(GlassMTLOffscreen*)other_offscreen
{
    [(GlassMTLFrameBufferObject*)self->_fbo blitFromFBO:(GlassMTLFrameBufferObject*)other_offscreen->_fbo];
}

@end
