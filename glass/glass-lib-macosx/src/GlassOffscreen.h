/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>

#import <OpenGL/gl.h>
#import <OpenGL/OpenGL.h>

@protocol GlassOffscreenProtocol

// as destination (to draw into)
- (void)bindForWidth:(GLuint)width andHeight:(GLuint)height;
- (void)unbind;

// as source (to show)
- (GLuint)texture;
- (void)blitForWidth:(GLuint)width andHeight:(GLuint)height;

- (GLuint)width;
- (GLuint)height;

@end

@interface GlassOffscreen : NSObject <GlassOffscreenProtocol>
{
    NSRecursiveLock             *_lock;
    CGLContextObj               _ctx;
    CGLContextObj               _ctxToRestore;
    
    id<GlassOffscreenProtocol>  _offscreen;
    
    GLboolean                   _dirty;
    
    GLfloat                     _backgroundR;
    GLfloat                     _backgroundG;
    GLfloat                     _backgroundB;
    GLfloat                     _backgroundA;
}

- (id)initWithContext:(CGLContextObj)ctx;
- (CGLContextObj)getContext;

- (void)setBackgroundColor:(NSColor*)color;

- (void)blit;
- (GLuint)texture;

// need locks to set contexts, which may differ for OffscreenProtocol bind/unbind and blit
- (void)lock;
- (void)unlock;

- (GLboolean)isDirty;

@end
