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

#import "GlassFrameBufferObject.h"
#import "GlassMacros.h"
#import "GlassApplication.h"

#import <OpenGL/glu.h>

#define TARGET GL_TEXTURE_RECTANGLE_EXT
#define FORMAT GL_BGRA
#define TYPE GL_UNSIGNED_INT_8_8_8_8_REV

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassFrameBufferObject

- (CGLContextObj)_assertContext
{
    CGLContextObj cgl_ctx = CGLGetCurrentContext();
    assert(cgl_ctx != NULL);
    return cgl_ctx;
}

- (BOOL)_supportsFbo
{
    return (gluCheckExtension((const GLubyte *)"GL_EXT_framebuffer_object", glGetString(GL_EXTENSIONS)) == GL_TRUE);
}

- (BOOL)_checkFbo
{
    BOOL ok = NO;
    {
        switch (glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT))
        {
            case GL_FRAMEBUFFER_COMPLETE_EXT:
                ok = YES;
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                NSLog(@"GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                NSLog(@"GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                NSLog(@"GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                NSLog(@"GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                NSLog(@"GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT");                          
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                NSLog(@"GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT");
                break;
            case GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                NSLog(@"GL_FRAMEBUFFER_UNSUPPORTED_EXT");
                break;
            default:
                NSLog(@"Unknown FBO Error");
                break;
        }
    }
    return ok;
}

- (void)_destroyFbo
{
    if (self->_texture != 0)
    {
        glDeleteTextures(1, &self->_texture);
        self->_texture = 0;
    }
    if (self->_fbo != 0)
    {
        glDeleteFramebuffersEXT(1, &self->_fbo);
        self->_fbo = 0;
    }
}

- (void)_createFboIfNeededForWidth:(GLuint)width andHeight:(GLuint)height
{
    if ((self->_width != width) || (self->_height != height))
    {
        // TODO optimization: is it possible to just resize an FBO's texture without destroying it first?
        [self _destroyFbo];
    }
    
    if (self->_fbo == 0)
    {
        glEnable(TARGET);
        {
            glActiveTextureARB(GL_TEXTURE0);
            glGenTextures(1, &self->_texture);
            LOG("           GlassFrameBufferObject created Texture: %d", self->_texture);
            glBindTexture(TARGET, self->_texture);
            glTexParameteri(TARGET, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(TARGET, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(TARGET, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(TARGET, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            {
                GLenum target = TARGET;
                GLint level = 0;
                GLint internalformat = GL_RGBA;
                GLint border = 0;
                GLenum format = FORMAT;
                GLenum type = TYPE;
                const GLvoid *pixels = NULL;
                glTexImage2D(target, level, internalformat, (GLint)width, (GLint)height, border, format, type, pixels);
            }
            
            glGenFramebuffersEXT(1, &self->_fbo);
            LOG("           GlassFrameBufferObject created FBO: %d", self->_fbo);
            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, self->_fbo);
            {
                GLenum target = GL_FRAMEBUFFER_EXT;
                GLenum attachment = GL_COLOR_ATTACHMENT0_EXT;
                GLenum textarget = TARGET;
                GLuint texture = self->_texture;
                GLint level = 0;
                glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
            }
            LOG("           glGetError(): %d", glGetError());
            
            if ([self _checkFbo] == NO)
            {
                [self _destroyFbo];
            }
        }
        glDisable(TARGET);
        
        glViewport(0, 0, (GLint)width, (GLint)height);
    }
//    DOES NOT WORK
//    else if ((self->_width != width) || (self->_height != height))
//    {
//        glEnable(TARGET);
//        glBindTexture(TARGET, self->_texture);
//        
//        GLenum target = TARGET;
//        GLint level = 0;
//        GLint xoffset = 0;
//        GLint yoffset = 0;
//        GLenum format = FORMAT;
//        GLenum type = TYPE;
//        const GLvoid *pixels = NULL;
//        glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
//
//        glDisable(TARGET);
//    }
    
    self->_width = width;
    self->_height = height;
}

- (id)init
{
    self = [super init];
    if (self != nil)
    {
        self->_width = 0;
        self->_height = 0;
        self->_texture = 0;
        self->_fbo = 0;
        self->_isSwPipe = NO;
        
        [self _assertContext];
        if ([self _supportsFbo] == NO)
        {
            [super dealloc];
            self = nil;
        }
    }
    return self;
}

- (void)dealloc
{
    [self _assertContext];
    {
        [self _destroyFbo];
    }
    
    [super dealloc];
}

- (GLuint)width
{
    return self->_width;
}

- (GLuint)height
{
    return self->_height;
}

- (void)bindForWidth:(GLuint)width andHeight:(GLuint)height
{
    LOG("           GlassFrameBufferObject bindForWidth:%d andHeight:%d", width, height);
    LOG("               context:%p", CGLGetCurrentContext());
    [self _assertContext];
    {
        if ((width > 0) && (height > 0))
        {
            if(self->_isSwPipe)
            {
                self->_fboToRestore = 0; // default to screen
                glGetIntegerv(GL_FRAMEBUFFER_BINDING_EXT, (GLint*)&self->_fboToRestore);
                LOG("               will need to restore to FBO: %d", self->_fboToRestore);
            }

            [self _createFboIfNeededForWidth:width andHeight:height];

            if (self->_isSwPipe && (self->_fbo != 0))
            {
                GLuint framebufferToBind = self->_fbo; // our own FBO
                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebufferToBind);
                LOG("               bounded to FBO: %d", self->_fbo);
            }
        }
    }
    LOG("               BOUND");
    LOG("               glGetError(): %d", glGetError());
}

- (void)unbind
{
    if (self->_isSwPipe)
    {
        LOG("           GlassFrameBufferObject unbind"); 
        [self _assertContext];
        {
            GLint framebufferCurrent = 0;
            glGetIntegerv(GL_FRAMEBUFFER_BINDING_EXT, &framebufferCurrent);

            if ((GLuint)framebufferCurrent != self->_fbo)
            {
                fprintf(stderr, "ERROR: unexpected fbo is bound! Expected %d, but found %d\n", self->_fbo, framebufferCurrent);
            }

            if (![GlassApplication syncRenderingDisabled]) {         
                glFinish();
            }
            GLuint framebufferToRevertTo = self->_fboToRestore;
            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, framebufferToRevertTo);
            LOG("               restored to FBO: %d", framebufferToRevertTo);
            LOG("               glGetError(): %d", glGetError());
        }
    }
}

- (void)blitForWidth:(GLuint)width andHeight:(GLuint)height
{
    LOG("           GlassFrameBufferObject blitForWidth:%d andHeight:%d [%p]", width, height, self);
    if (self->_texture != 0)
    {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
        
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        glEnable(TARGET);
        {
            glActiveTextureARB(GL_TEXTURE0);
            glBindTexture(TARGET, self->_texture);
            glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            {
                glBegin(GL_QUADS);
                {
                    glTexCoord2f(        0.0f, self->_height); glVertex2f(        0.0f,          0.0f);
                    glTexCoord2f(self->_width, self->_height); glVertex2f(self->_width,          0.0f);
                    glTexCoord2f(self->_width,          0.0f); glVertex2f(self->_width, self->_height);
                    glTexCoord2f(        0.0f,          0.0f); glVertex2f(        0.0f, self->_height);
                }
                glEnd();
            }
            glBindTexture(TARGET, 0);
        }
        glDisable(TARGET);
        LOG("               BLITED");
        LOG("               glGetError(): %d", glGetError());
    }
}

- (void)blitFromFBO:(GlassFrameBufferObject*)other_fbo
{
    self->_fboToRestore = 0; // default to screen
    glGetIntegerv(GL_FRAMEBUFFER_BINDING_EXT, (GLint*)&self->_fboToRestore);
    [self _createFboIfNeededForWidth:other_fbo->_width andHeight:other_fbo->_height];
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, self->_fbo);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, other_fbo->_fbo);
    glBlitFramebuffer(0,0, other_fbo->_width, other_fbo->_height,
                      0,0, self->_width, self->_height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, self->_fboToRestore);
}


- (GLuint)texture
{
    return self->_texture;
}

- (GLuint)fbo
{
    return self->_fbo;
}

- (void)setIsSwPipe:(BOOL)isSwPipe
{
    self->_isSwPipe = isSwPipe;
}

@end
