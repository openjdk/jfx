/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

#import "GlassOffscreen.h"

#import "GlassFrameBufferObject.h"
//#import "GlassPBuffer.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@interface GlassOffscreen ()
- (void)setContext;
- (void)unsetContext;
@end

@implementation GlassOffscreen

- (id)initWithContext:(CGLContextObj)ctx
{
    self = [super init];
    if (self != nil)
    {
        self->_ctx = CGLRetainContext(ctx);
        
        self->_backgroundR = 1.0f;
        self->_backgroundG = 1.0f;
        self->_backgroundB = 1.0f;
        self->_backgroundA = 1.0f;
        
        [self setContext];
        {
            self->_offscreen = [[GlassFrameBufferObject alloc] init];
            if (self->_offscreen == nil)
            {
                // TODO: implement PBuffer if needed
                //self->_offscreen = [[GlassPBuffer alloc] init];
            }
        }
        [self unsetContext];
    }
    return self;
}

- (CGLContextObj)getContext;
{
    return self->_ctx;
}

- (void)dealloc
{
    [self setContext];
    {
        [(NSObject*)self->_offscreen release];
        self->_offscreen = NULL;
    }
    [self unsetContext];

    CGLReleaseContext(self->_ctx);
    self->_ctx = NULL;

    [super dealloc];
}

- (void)setBackgroundColor:(NSColor*)color
{
    self->_backgroundR = (GLfloat)[color redComponent];
    self->_backgroundG = (GLfloat)[color greenComponent];
    self->_backgroundB = (GLfloat)[color blueComponent];
    self->_backgroundA = (GLfloat)[color alphaComponent];
}

- (GLuint)width
{
    return [self->_offscreen width];
}

- (GLuint)height
{
    return [self->_offscreen height];
}

- (GLuint)fbo
{
    return [self->_offscreen fbo];
}

- (CAOpenGLLayer*)getLayer
{
    return _layer;
}

- (void)setLayer:(CAOpenGLLayer*)new_layer
{
    //Set a weak reference as layer owns offscreen
    self->_layer = new_layer;
}

- (void)setContext
{
    self->_ctxToRestore = CGLGetCurrentContext();
    CGLLockContext(self->_ctx);
    CGLSetCurrentContext(self->_ctx);
}

- (void)unsetContext
{
    CGLSetCurrentContext(self->_ctxToRestore);    
    CGLUnlockContext(self->_ctx);
}

- (void)bindForWidth:(GLuint)width andHeight:(GLuint)height
{
    [self setContext];
    [self->_offscreen bindForWidth:width andHeight:height];
    [self unsetContext];
}

- (void)blit
{
    [self blitForWidth:[self->_offscreen width] andHeight:[self->_offscreen height]];
}

- (GLuint)texture
{
    return [self->_offscreen texture];
}

- (void)blitForWidth:(GLuint)width andHeight:(GLuint)height
{
    {
#if 1
        glClearColor(self->_backgroundR, self->_backgroundG, self->_backgroundB, self->_backgroundA);
        glClear(GL_COLOR_BUFFER_BIT);
#else
        // for debugging, change clear color every 0.5 seconds
        static int counterFps = 0;
        static int counterColor = 0;
        counterFps++;
        if ((counterFps%(60/2)) == 0)
        {
            counterColor++;
        }
        switch (counterColor%3)
        {
            case 0:
                glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
                break;
            case 1:
                glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                break;
            case 2:
                glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
                break;
        }
        glClear(GL_COLOR_BUFFER_BIT);
#endif        
        [self->_offscreen blitForWidth:width andHeight:height];
        
        self->_dirty = GL_FALSE;
    }
}

- (GLboolean)isDirty
{
    return self->_dirty;
}

- (void)blitFromOffscreen:(GlassOffscreen*) other_offscreen
{
    [self setContext];
    {
        [(GlassFrameBufferObject*)self->_offscreen blitFromFBO:(GlassFrameBufferObject*)other_offscreen->_offscreen];
        self->_dirty = GL_TRUE;
    }
    [self unsetContext];
}

@end
