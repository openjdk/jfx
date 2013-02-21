/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "GlassHostView.h"

@implementation GlassHostView : NSView

- (void)dealloc
{
    [super dealloc];
}

- (BOOL)isOpaque
{
    return NO;
}

- (BOOL)becomeFirstResponder
{
    return NO;
}

- (BOOL)acceptsFirstResponder
{
    return NO;
}

- (BOOL)canBecomeKeyView
{
    return NO;
}

- (BOOL)postsBoundsChangedNotifications
{
    return YES;
}

- (BOOL)postsFrameChangedNotifications
{
    return NO;
}

- (BOOL)acceptsFirstMouse:(NSEvent *)theEvent
{
    return NO;
}

- (BOOL)mouseDownCanMoveWindow
{
    return NO;
}

- (void)addSubview:(NSView *)aView
{
    [super addSubview:aView];
    self->view = aView;
}

- (void)drawRect:(NSRect)rect
{
    //NSLog(@"Sparkle: drawRect: [%@] %.2f,%.2f %.2fx%.2f,", self, rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
    //NSLog(@"Sparkle: frame: [%@] %.2f,%.2f %.2fx%.2f,", self, [self frame].origin.x, [self frame].origin.y, [self frame].size.width, [self frame].size.height);
    //NSLog(@"Sparkle: bounds: [%@] %.2f,%.2f %.2fx%.2f,", self, [self bounds].origin.x, [self bounds].origin.y, [self bounds].size.width, [self bounds].size.height);
    //[[NSColor redColor] set];
    //NSRectFill([self bounds]);
}

- (BOOL) isFlipped
{
    return YES;
}

@end
