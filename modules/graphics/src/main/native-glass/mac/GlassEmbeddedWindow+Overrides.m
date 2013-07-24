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

#import "common.h"
#import "com_sun_glass_events_WindowEvent.h"
#import "com_sun_glass_ui_Window.h"
#import "com_sun_glass_ui_Window_Level.h"
#import "com_sun_glass_ui_mac_MacWindow.h"

#import "GlassMacros.h"
#import "GlassWindow.h"
#import "GlassWindow+Java.h"
#import "GlassWindow+Overrides.h"
#import "GlassEmbeddedWindow+Overrides.h"
#import "GlassView.h"
#import "GlassView3D.h"
#import "GlassLayer3D.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define ALPHA 0.0f

// a special window which is transparent and without shadow so it does not show
@implementation GlassEmbeddedWindow (Overrides)

- (BOOL)_isParent
{
    return (self->parent == nil); // has no parent, so it's the parent itself
}

- (void)setContentView:(NSView *)aView
{
    [super setContentView:self->gWindow->view];
    
    // embed the child layer (offscreen) into the parent layer (offscreen)
    if (self->parent != nil)
    {
        CALayer *layer = [self->gWindow->view layer];
        if ([layer isKindOfClass:[CAOpenGLLayer class]] == YES)
        {
            [((CAOpenGLLayer*)layer) setAsynchronous:NO];
        }
        
        if ([layer isKindOfClass:[GlassLayer3D class]] == YES)
        {
            GlassOffscreen *offscreen = [((GlassLayer3D*)layer) getGlassOffscreen];
            
            layer = [self->parent->gWindow->view layer];
            if ([layer isKindOfClass:[GlassLayer3D class]] == YES)
            {
                [((GlassLayer3D*)layer) hostOffscreen:offscreen];
            }
        }
    }
}

// keep the window invisible
#pragma mark --- Visibility

- (void)setHasShadow:(BOOL)hasShadow
{
    [super setHasShadow:NO];
}

- (BOOL)hasShadow
{
    return NO;
}

- (void)invalidateShadow
{
    
}

- (void)setAlphaValue:(CGFloat)windowAlpha
{
    [super setAlphaValue:ALPHA];
}

- (CGFloat)alphaValue
{
    return ALPHA;
}

- (void)setOpaque:(BOOL)isOpaque
{
    [super setOpaque:NO];
}

- (BOOL)isOpaque
{
    return NO;
}

#pragma mark --- Mission Control (Expose)

- (NSWindowCollectionBehavior)collectionBehavior
{
    // make sure we do not show up in MissionControl (was Expose)
    return NSWindowCollectionBehaviorTransient;
}

// match the child frame with that of parent
#pragma mark --- Frame

- (void)setFrame:(NSRect)frameRect display:(BOOL)flag
{
    [super setFrame:frameRect display:flag];
    if ([self _isParent] == YES)
    {
        [self->child setFrame:frameRect display:flag];
    }
}

- (void)setFrame:(NSRect)frameRect display:(BOOL)displayFlag animate:(BOOL)animateFlag
{
    [super setFrame:frameRect display:displayFlag animate:animateFlag];
    if ([self _isParent] == YES)
    {
        [self->child setFrame:frameRect display:displayFlag animate:animateFlag];
    }
}

- (void)setContentSize:(NSSize)aSize
{
    [super setContentSize:aSize];
    if ([self _isParent] == YES)
    {
        [self->child setContentSize:aSize];
    }
}

- (void)setFrameOrigin:(NSPoint)aPoint
{
    [super setFrameOrigin:aPoint];
    if ([self _isParent] == YES)
    {
        [self->child setFrameOrigin:aPoint];
    }
}

- (void)setFrameTopLeftPoint:(NSPoint)aPoint
{
    [super setFrameTopLeftPoint:aPoint];
    if ([self _isParent] == YES)
    {
        [self->child setFrameTopLeftPoint:aPoint];
    }
}

// manually track the focus, since the real applet is inside another process window (Browser)
// our own process will not be active and we can't depend on regular native NSWindow behavior

#pragma mark --- Focus

- (BOOL)isKeyWindow
{
    return self->isKeyWindow;
}

- (void)makeKeyWindow
{
    [super makeKeyWindow];
    
    self->isKeyWindow = YES;
    [[NSNotificationCenter defaultCenter] postNotificationName:NSWindowDidBecomeKeyNotification object:nil userInfo:nil];
    [[self delegate] windowDidBecomeKey:nil];
}

- (void)resignKeyWindow
{
    [super resignKeyWindow];
    
    self->isKeyWindow = NO;
    [[NSNotificationCenter defaultCenter] postNotificationName:NSWindowDidResignKeyNotification object:nil userInfo:nil];
    [[self delegate] windowDidResignKey:nil];
}

//#pragma mark --- Debug
//
//- (void)setBackgroundColor:(NSColor *)color
//{
//    if ([self _isParent] == NO)
//    {
//        [super setBackgroundColor:[NSColor greenColor]];
//    }
//    else
//    {
//        [super setBackgroundColor:[NSColor redColor]];
//    }
//}
//
//- (NSColor *)backgroundColor
//{
//    if ([self _isParent] == NO)
//    {
//        return [NSColor greenColor];
//    }
//    else
//    {
//        return [NSColor redColor];
//    }
//}

@end
