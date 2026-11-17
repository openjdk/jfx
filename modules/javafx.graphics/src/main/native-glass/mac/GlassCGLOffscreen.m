/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

#import "GlassCGLOffscreen.h"

@implementation GlassCGLOffscreen

static NSArray *allModes = nil;

- (id)initWithContext:(CGLContextObj)ctx
          andIsSwPipe:(BOOL)isSwPipe
{
    self = [super init];
    if (self != nil)
    {
        self->_drawCounter = 0;
        self->_texture = 0;
        self->_ctx = CGLRetainContext(ctx);

        [self setContext];
        {
            self->_fbo = [[GlassCGLFrameBufferObject alloc] init];
            if (self->_fbo == nil)
            {
                // TODO: implement PBuffer if needed
                // self->_fbo = [[GlassPBuffer alloc] init];
            }
            [(GlassCGLFrameBufferObject*)self->_fbo setIsSwPipe:(BOOL)isSwPipe];
        }
        [self unsetContext];
        if (allModes == nil) {
            allModes = [[NSArray arrayWithObjects:NSDefaultRunLoopMode,
                                                  NSEventTrackingRunLoopMode,
                                                  NSModalPanelRunLoopMode, nil] retain];
        }
    }
    return self;
}

- (CGLContextObj)getContext
{
    return self->_ctx;
}

- (void)dealloc
{
    if (self->_texture != 0)
    {
        [self bindForWidth:(GLuint)[self->glassView bounds].size.width
                 andHeight:(GLuint)[self->glassView bounds].size.height];
        glDeleteTextures(1, &self->_texture);
        [self unbind];
    }
    [self setContext];
    [(NSObject*)self->_fbo release];
    self->_fbo = NULL;
    [self unsetContext];

    CGLReleaseContext(self->_ctx);
    self->_ctx = NULL;

    [super dealloc];
}

- (unsigned int)width
{
    return [self->_fbo width];
}

- (unsigned int)height
{
    return [self->_fbo height];
}

- (jlong)fbo
{
    return (jlong)[self->_fbo fbo];
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

- (void)bindForWidth:(GLuint)width
           andHeight:(GLuint)height
{
    assert(self->_drawCounter >= 0);
    if (self->_drawCounter == 0)
    {
        self->_width = width;
        self->_height = height;
        [self setContext];
        [self->_fbo bindForWidth:width andHeight:height];
    }
    self->_drawCounter++;
}

- (void)flush:(GlassOffscreen*)glassOffScreen
{
    assert(self->_drawCounter > 0);
    self->_drawCounter--;
    if (self->_drawCounter == 0)
    {
        [self unbind];
        [(GlassCGLOffscreen*)glassOffScreen blitFromOffscreen:(GlassCGLOffscreen*)self];
        if ([NSThread isMainThread]) {
            [[(GlassCGLOffscreen*)glassOffScreen getLayer] setNeedsDisplay];
        } else {
            [[(GlassCGLOffscreen*)glassOffScreen getLayer]
                performSelectorOnMainThread:@selector(setNeedsDisplay)
                                 withObject:nil
                              waitUntilDone:NO
                                      modes:allModes];
        }
    }
}

- (void)pushPixels:(void*)pixels
         withWidth:(unsigned int)width
        withHeight:(unsigned int)height
        withScaleX:(float)scalex
        withScaleY:(float)scaley
            ofView:(NSView*)view
{
    assert(self->_drawCounter > 0);

    if (self->_texture == 0)
    {
        glGenTextures(1, &self->_texture);
    }
    self->glassView = view;
    BOOL uploaded = NO;
    if ((self->_textureWidth != width) || (self->_textureHeight != height))
    {
        uploaded = YES;

        self->_textureWidth = width;
        self->_textureHeight = height;

        // GL_EXT_texture_rectangle is defined in OS X 10.6 GL headers, so we can depend on GL_TEXTURE_RECTANGLE_EXT being available
        glBindTexture(GL_TEXTURE_RECTANGLE_EXT, self->_texture);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexImage2D(GL_TEXTURE_RECTANGLE_EXT, 0, GL_RGBA8, (GLsizei)self->_textureWidth, (GLsizei)self->_textureHeight,
                        0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
    }

    glEnable(GL_TEXTURE_RECTANGLE_EXT);
    glBindTexture(GL_TEXTURE_RECTANGLE_EXT, self->_texture);
    {
        if (uploaded == NO)
        {
            glTexSubImage2D(GL_TEXTURE_RECTANGLE_EXT, 0, 0, 0, (GLsizei)self->_textureWidth, (GLsizei)self->_textureHeight,
                                GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
        }

        GLfloat w = self->_textureWidth;
        GLfloat h = self->_textureHeight;

        NSSize size = [self->glassView bounds].size;
        size.width *= scalex;
        size.height *= scaley;
        if ((size.width != w) || (size.height != h))
        {
            // This could happen on live resize, clear the FBO to avoid rendering garbage
            glClear(GL_COLOR_BUFFER_BIT);
        }

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0f, size.width, size.height, 0.0f, -1.0f, 1.0f);
        {
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            {
                glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE); // copy

                glBegin(GL_QUADS);
                {
                    glTexCoord2f(0.0f, 0.0f); glVertex2f(0.0f, 0.0f);
                    glTexCoord2f(   w, 0.0f); glVertex2f(   w, 0.0f);
                    glTexCoord2f(   w,    h); glVertex2f(   w,    h);
                    glTexCoord2f(0.0f,    h); glVertex2f(0.0f,    h);
                }
                glEnd();
            }
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
        }
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }
    glBindTexture(GL_TEXTURE_RECTANGLE_EXT, 0);
    glDisable(GL_TEXTURE_RECTANGLE_EXT);

    glFinish();
}

- (void)unbind
{
    [self->_fbo unbind];
    [self unsetContext];
}

- (GLuint)texture
{
    return [self->_fbo texture];
}

- (void)blitForWidth:(GLuint)width
           andHeight:(GLuint)height
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
        [self->_fbo blitForWidth:width
                       andHeight:height];

        self->_dirty = GL_FALSE;
    }
}

- (GLboolean)isDirty
{
    return self->_dirty;
}

- (void)blit
{
    [self blitForWidth:[self->_fbo width]
             andHeight:[self->_fbo height]];
}

- (void)blitFromOffscreen:(GlassCGLOffscreen*)other_offscreen
{
    [self setContext];
    {
        [(GlassCGLFrameBufferObject*)self->_fbo blitFromFBO:(GlassCGLFrameBufferObject*)other_offscreen->_fbo];
        self->_dirty = GL_TRUE;
    }
    [self unsetContext];
}

@end
