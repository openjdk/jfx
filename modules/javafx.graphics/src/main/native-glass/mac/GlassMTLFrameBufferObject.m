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

#import "GlassMTLFrameBufferObject.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassMTLFrameBufferObject

- (void)_destroyFbo
{
    if (self->_texture != nil)
    {
        LOG("GlassMTLFrameBufferObject releasing FBO :%lu", self->_texture);
        [lock lock];
        [self->_texture release];
        self->_texture = nil;
        [lock unlock];
    }
}

- (void)_createFboIfNeededForWidth:(unsigned int)width
                         andHeight:(unsigned int)height
{
    if ((self->_width != width) || (self->_height != height))
    {
        // TODO optimization: is it possible to just resize an FBO's texture without destroying it first?
        [self _destroyFbo];
    }

    if (self->_texture == nil) {
        // Create a texture
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();

        MTLTextureDescriptor *texDescriptor =
                [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                                                   width:width
                                                                  height:height
                                                               mipmapped:false];
        texDescriptor.storageMode = MTLStorageModeManaged;
        texDescriptor.usage = MTLTextureUsageRenderTarget;

        [lock lock];
        self->_texture = [device newTextureWithDescriptor:texDescriptor];
        [lock unlock];
    }

    self->_width = width;
    self->_height = height;
}

- (id)init
{
    self = [super init];
    if (self != nil)
    {
        lock = [NSLock new];
        self->_width = 0;
        self->_height = 0;
        self->_texture = nil;
        self->_isSwPipe = NO;
    }
    return self;
}

- (void)dealloc
{
    [self _destroyFbo];
    [super dealloc];
}

- (unsigned int)width
{
    return self->_width;
}

- (unsigned int)height
{
    return self->_height;
}

- (void)bindForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
    LOG("           GlassMTLFrameBufferObject bindForWidth:%d andHeight:%d", width, height);
    {
        if ((width > 0) && (height > 0))
        {
            if(self->_isSwPipe)
            {
                // self->_fboToRestore = 0; // default to screen
                // glGetIntegerv(GL_FRAMEBUFFER_BINDING_EXT, (GLint*)&self->_fboToRestore);
                // LOG("               will need to restore to FBO: %d", self->_fboToRestore);
            }

            [self _createFboIfNeededForWidth:width andHeight:height];
        }
    }
}

- (void)blitForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
    // TODO: MTL: check if implementation required
}

- (bool)tryLockTexture {
    return [lock tryLock];
}

- (void)unlockTexture {
    return [lock unlock];
}

- (void)blitFromFBO:(GlassMTLFrameBufferObject*)other_fbo
{
    [self _createFboIfNeededForWidth:other_fbo->_width andHeight:other_fbo->_height];
}

- (id<MTLTexture>)texture
{
    return self->_texture;
}

- (void)setIsSwPipe:(BOOL)isSwPipe
{
    self->_isSwPipe = isSwPipe;
}

@end
