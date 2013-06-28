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

#import "GlassFullscreenWindow.h"

@implementation GlassBackgroundWindow : NSWindow

- (id)initWithWindow:(NSWindow *)window
{
    self = [super initWithContentRect:[[window screen] frame] styleMask:NSBorderlessWindowMask backing:NSBackingStoreBuffered defer:NO screen:[window screen]];
    if (self != nil)
    {
        self->trackWindow = [window retain];
        //[self->trackWindow setDelegate:self];
        [self setAlphaValue:0.0f];
        
        CGFloat ratioWidth = ([self->trackWindow frame].size.width/[[self->trackWindow screen] frame].size.width);
        CGFloat ratioHeight = ([self->trackWindow frame].size.height/[[self->trackWindow screen] frame].size.height);
        if (ratioWidth > ratioHeight)
        {
            self->startingTrackingSize = [window frame].size.width;
        }
        else
        {
            self->startingTrackingSize = [window frame].size.height;
        }
        
        [self useOptimizedDrawing:NO];
    }
    return self;
}

- (void)dealloc
{
    [self->trackWindow release];
    
    [super dealloc];
}

- (void)windowDidResize:(NSNotification *)notification
{
    CGFloat alpha = 0.0f;
    
    CGFloat ratioWidth = ([self->trackWindow frame].size.width/[[self->trackWindow screen] frame].size.width);
    CGFloat ratioHeight = ([self->trackWindow frame].size.height/[[self->trackWindow screen] frame].size.height);
    if (ratioWidth > ratioHeight)
    {
        CGFloat currentTrackingWindowSize = [self->trackWindow frame].size.width - self->startingTrackingSize;
        alpha = (currentTrackingWindowSize / ([[self->trackWindow screen] frame].size.width - self->startingTrackingSize));
    }
    else
    {
        CGFloat currentTrackingWindowSize = [self->trackWindow frame].size.height - self->startingTrackingSize;
        alpha = (currentTrackingWindowSize / ([[self->trackWindow screen] frame].size.height - self->startingTrackingSize));
    }
    
    [self setAlphaValue:(alpha*alpha)]; // linear
}

- (BOOL)isReleasedWhenClosed
{
    return YES;
}

- (BOOL)isOpaque
{
    return NO;
}

- (BOOL)hasShadow
{
    return NO;
}

- (BOOL)canBecomeMainWindow
{
    return NO;
}

- (BOOL)canBecomeKeyWindow
{
    return NO;
}

- (BOOL)showsResizeIndicator
{
    return NO;
}

- (NSColor *)backgroundColor
{
    return [NSColor blackColor];
}

@end

@implementation GlassFullscreenWindow : NSWindow

- (id)initWithContentRect:(NSRect)contentRect withHostView:(NSView *)hostView withView:(NSView *)view withScreen:(NSScreen *)screen withPoint:(NSPoint)p
{
    self = [super initWithContentRect:contentRect styleMask:NSBorderlessWindowMask backing:NSBackingStoreBuffered defer:NO screen:screen];
    if (self != nil)
    {
        self->point = p;
        
        [self useOptimizedDrawing:NO];
        [self setContentView:hostView];
        [self setInitialFirstResponder:view];
        [self makeFirstResponder:view];
        [self setLevel:NSScreenSaverWindowLevel];
        
        [self setBackgroundColor:[NSColor colorWithCalibratedRed:0.0f green:0.0f blue:0.0f alpha:0.0f]];
        
        [hostView release];
    }
    return self;
}

- (void)dealloc
{
    [super dealloc];
}

- (BOOL)isReleasedWhenClosed
{
    return YES;
}

- (BOOL)isOpaque
{
    return NO;
}

- (BOOL)hasShadow
{
    return NO;
}

- (BOOL)canBecomeMainWindow
{
    return YES;
}

- (BOOL)canBecomeKeyWindow
{
    return YES;
}

- (BOOL)showsResizeIndicator
{
    return NO;
}

- (NSPoint)point
{
    return self->point;        
}

@end
