/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#import "GlassView.h"
#import "GlassOffscreen.h"

// 3D version of Glass providing OpenGL context through CAOpenGLLayer
@interface GlassView3D : NSOpenGLView <GlassView, NSTextInputClient>
{
    GlassViewDelegate   *_delegate;

    NSUInteger          _drawCounter; // draw counter, so that we only bind/unbind offscreen once

    NSTrackingArea      *_trackingArea;

    GLuint              _texture;
    GLuint              _textureWidth;
    GLuint              _textureHeight;

    CGFloat             _backgroundR;
    CGFloat             _backgroundG;
    CGFloat             _backgroundB;
    CGFloat             _backgroundA;

    NSAttributedString *nsAttrBuffer;
    BOOL imEnabled;
    BOOL handlingKeyEvent;
    BOOL didCommitText;
    BOOL isHiDPIAware;
}

- (id)initWithFrame:(NSRect)frame withJview:(jobject)jView withJproperties:(jobject)jproperties;
- (void)setFrameOrigin:(NSPoint)newOrigin;

@end
