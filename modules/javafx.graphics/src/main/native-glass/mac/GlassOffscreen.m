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

#import "GlassOffscreen.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassOffscreen

- (id)init
{
    self = [super init];
    if (self != nil)
    {
        self->_backgroundR = 1.0f;
        self->_backgroundG = 1.0f;
        self->_backgroundB = 1.0f;
        self->_backgroundA = 1.0f;
    }
    return self;
}

- (void)dealloc
{
    [super dealloc];
}

- (void)setBackgroundColor:(NSColor*)color
{
    self->_backgroundR = (float)[color redComponent];
    self->_backgroundG = (float)[color greenComponent];
    self->_backgroundB = (float)[color blueComponent];
    self->_backgroundA = (float)[color alphaComponent];
}

- (unsigned int)width
{
    return 0;
}

- (unsigned int)height
{
    return 0;
}

- (jlong)fbo
{
    return 0;
}

- (void)bindForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
}

- (void)unbind
{
}

- (void)flush:(GlassOffscreen*)glassOffScreen
{
}

- (void)pushPixels:(void*)pixels
         withWidth:(unsigned int)width
        withHeight:(unsigned int)height
        withScaleX:(float)scalex
        withScaleY:(float)scaley
            ofView:(NSView*)view
{
}

- (void)blit
{
}

- (void)blitForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
}

- (unsigned char)isDirty
{
    return 0;
}

- (void)blitFromOffscreen:(GlassOffscreen*)other_offscreen
{
}

- (CALayer*)getLayer
{
    return _layer;
}

- (void)setLayer:(CALayer*)new_layer
{
    //Set a weak reference as layer owns offscreen
    self->_layer = new_layer;
}

@end
