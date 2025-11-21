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

#import "GlassMacros.h"
#import "GlassScreen.h"
#import "GlassLayerMTL.h"
#import "GlassMTLOffscreen.h"

@implementation GlassLayerMTL

- (id) init:(long)mtlCommandQueuePtr
     isVsyncEnabled:(BOOL)isVsyncEnabled
       withIsSwPipe:(BOOL)isSwPipe
{
    self = [super init];
    isHiDPIAware = true; // TODO : pass in this from view

    [self setAutoresizingMask:(kCALayerWidthSizable | kCALayerHeightSizable)];
    [self setContentsGravity:kCAGravityTopLeft];

    [self setMasksToBounds:YES];
    [self setNeedsDisplayOnBoundsChange:YES];
    [self setAnchorPoint:CGPointMake(0.0f, 0.0f)];

    self.device = MTLCreateSystemDefaultDevice();

    self.pixelFormat = MTLPixelFormatBGRA8Unorm;
    self.framebufferOnly = NO;
    self.displaySyncEnabled = isVsyncEnabled;
    self.opaque = NO; //to support shaped window

    if (!isSwPipe) {
        self->_blitCommandQueue = (id<MTLCommandQueue>)(jlong_to_ptr(mtlCommandQueuePtr));
    } else {
        self->_blitCommandQueue = [self.device newCommandQueue];
    }
    self->_painterOffscreen = (GlassOffscreen*)[[GlassMTLOffscreen alloc] initWithContext:self.device
                                                                             commandQueue:self->_blitCommandQueue
                                                                              andIsSwPipe:isSwPipe];
    [self->_painterOffscreen setLayer:self];

    self.colorspace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);

    return self;
}

- (void)dealloc
{
    [self->_painterOffscreen release];
    self->_painterOffscreen = nil;

    [super dealloc];
}

- (GlassOffscreen*)getPainterOffscreen
{
    return self->_painterOffscreen;
}

- (void)display {

    [self blitToScreen];

    [super display];
}

volatile static int nextDrawableCount = 0;

- (void) blitToScreen
{
    if ([((GlassMTLOffscreen*)self->_painterOffscreen) tryLockTexture])
    {
        @try {
            id<MTLTexture> backBufferTex = [(GlassMTLOffscreen*)self->_painterOffscreen getMTLTexture];

            if (backBufferTex == nil) {
                return;
            }

            int width = [self->_painterOffscreen width];
            int height = [self->_painterOffscreen height];

            if (width <= 0 || height <= 0) {
                // NSLog(@"Layer --------- backing texture not ready yet--- skipping blit.");
                return;
            }

            if (nextDrawableCount > 2) {
                // NSLog(@"Layer --------- previous drawing in progress.. skipping blit to screen.");
                return;
            }

            @autoreleasepool {
                id<CAMetalDrawable> mtlDrawable = [self nextDrawable];
                if (mtlDrawable == nil) {
                    return;
                }

                id<MTLTexture> dstTexture = mtlDrawable.texture;
                if (dstTexture.width != width || dstTexture.height != height) {
                    return;
                }

                id<MTLCommandBuffer> commandBuf = [self->_blitCommandQueue commandBuffer];
                if (commandBuf == nil) {
                    return;
                }

                nextDrawableCount++;

                id <MTLBlitCommandEncoder> blitEncoder = [commandBuf blitCommandEncoder];

                [blitEncoder copyFromTexture:backBufferTex
                                   toTexture:dstTexture];

                [blitEncoder endEncoding];
                [commandBuf presentDrawable:mtlDrawable];
                [commandBuf addCompletedHandler:^(id <MTLCommandBuffer> commandBuf) {
                    nextDrawableCount--;
                }];

                [commandBuf commit];
                // [commandBuf waitUntilCompleted];
            }
        }
        @finally {
            [((GlassMTLOffscreen*)self->_painterOffscreen) unlockTexture];
        }
    }
    // else
    // {
    //     The texture is locked, either being created/destroyed or being encoded into BlitEncoder
    //     hence the frame could be dropped.
    // }
}

@end
