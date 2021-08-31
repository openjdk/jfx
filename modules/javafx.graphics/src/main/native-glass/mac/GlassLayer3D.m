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

#import "GlassLayer3D.h"

#import "GlassMacros.h"
#import "GlassScreen.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassLayer3D

static NSArray *allModes = nil;

- (id)initWithSharedContext:(CGLContextObj)ctx
           andClientContext:(CGLContextObj)clCtx
             withHiDPIAware:(BOOL)HiDPIAware
             withIsSwPipe:(BOOL)isSwPipe
{
    LOG("GlassLayer3D initWithSharedContext]");
    self = [super init];
    if (self != nil)
    {
        self->_painterOffscreen = [[GlassOffscreen alloc] initWithContext:clCtx andIsSwPipe:isSwPipe];
        self->_glassOffscreen = [[GlassOffscreen alloc] initWithContext:ctx andIsSwPipe:isSwPipe];
        [self->_glassOffscreen setLayer:self];
        LOG("   GlassLayer3D context: %p", ctx);

        self->isHiDPIAware = HiDPIAware;

        [self setAsynchronous:NO];
        [self setAutoresizingMask:(kCALayerWidthSizable|kCALayerHeightSizable)];
        [self setContentsGravity:kCAGravityTopLeft];

        // Initially the view is not in any window yet, so using the
        // screens[0]'s scale is a good starting point (this is most probably
        // the notebook's main LCD display which is HiDPI-capable).
        // Note that mainScreen is the screen with the current app bar focus
        // in Mavericks and later OS so it will likely not match the screen
        // we initially show windows on if an app is started from an external
        // monitor.
        [self notifyScaleFactorChanged:GetScreenScaleFactor([[NSScreen screens] objectAtIndex:0])];

        [self setMasksToBounds:YES];
        [self setNeedsDisplayOnBoundsChange:YES];
        [self setAnchorPoint:CGPointMake(0.0f, 0.0f)];

        if (allModes == nil) {
            allModes = [[NSArray arrayWithObjects:NSDefaultRunLoopMode,
                                                  NSEventTrackingRunLoopMode,
                                                  NSModalPanelRunLoopMode, nil] retain];
        }
    }
    return self;
}

- (void)dealloc
{
    [self->_glassOffscreen release];
    self->_glassOffscreen = nil;

    [self->_painterOffscreen release];
    self->_painterOffscreen = nil;

    [super dealloc];
}

- (void)notifyScaleFactorChanged:(CGFloat)scale
{
    if (self->isHiDPIAware) {
        if ([self respondsToSelector:@selector(setContentsScale:)]) {
            [self setContentsScale: scale];
        }
    }
}

//- (void)setBounds:(CGRect)bounds
//{
//    LOG("GlassLayer3D setBounds:%s", [NSStringFromRect(NSRectFromCGRect(bounds)) UTF8String]);
//    [super setBounds:bounds];
//}

- (BOOL)canDrawInCGLContext:(CGLContextObj)glContext pixelFormat:(CGLPixelFormatObj)pixelFormat forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    return [self->_glassOffscreen isDirty];
}

- (CGLContextObj)copyCGLContextForPixelFormat:(CGLPixelFormatObj)pixelFormat
{
    return CGLRetainContext([self->_glassOffscreen getContext]);
}

- (CGLPixelFormatObj)copyCGLPixelFormatForDisplayMask:(uint32_t)mask
{
    return CGLRetainPixelFormat(CGLGetPixelFormat([self->_glassOffscreen getContext]));
}

- (void)drawInCGLContext:(CGLContextObj)glContext pixelFormat:(CGLPixelFormatObj)pixelFormat forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    // glContext is already set as current by now and locked by Quartz internaly
    LOG("GlassLayer3D drawInCGLContext]");
    LOG("   current context: %p", CGLGetCurrentContext());
#ifdef VERBOSE
    {
        GLint fbo = 0; // default to screen
        glGetIntegerv(GL_FRAMEBUFFER_BINDING_EXT, (GLint*)&fbo);
        LOG("   fbo: %d", fbo);
    }
#endif
    // the viewport is already set for us here, so just blit

#if 0
    // this will stretch the offscreen to cover all the surface
    // ie., live resizing "appears" better, but the blit area is not at 1:1 scale
    [self->_glassOffscreen blit];
#else
    // we blit only in the area we rendered in
    GLint params[] = { 0, 0, 0, 0 };
    glGetIntegerv(GL_VIEWPORT, params);
    if ((params[2] > 0) && ((params[3] > 0)))
    {
        [self->_glassOffscreen blitForWidth:(GLuint)params[2] andHeight:(GLuint)params[3]];
    }
#endif

    // the default implementation of the method flushes the context.
    [super drawInCGLContext:glContext pixelFormat:pixelFormat forLayerTime:timeInterval displayTime:timeStamp];
    LOG("\n");
}

- (void)flush
{
    [(GlassOffscreen*)_glassOffscreen blitFromOffscreen:(GlassOffscreen*)_painterOffscreen];
    if ([NSThread isMainThread]) {
        [[self->_glassOffscreen getLayer] setNeedsDisplay];
    } else {
        [[self->_glassOffscreen getLayer] performSelectorOnMainThread:@selector(setNeedsDisplay)
                                                           withObject:nil
                                                        waitUntilDone:NO
                                                                modes:allModes];
    }
}

- (GlassOffscreen*)getPainterOffscreen
{
    return self->_painterOffscreen;
}

- (GlassOffscreen*)getGlassOffscreen
{
    return self->_glassOffscreen;
}

- (void)hostOffscreen:(GlassOffscreen*)offscreen
{
    [self->_glassOffscreen release];
    self->_glassOffscreen = [offscreen retain];
    [self->_glassOffscreen setLayer:self];
}

@end
