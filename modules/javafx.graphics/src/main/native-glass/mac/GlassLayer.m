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

#import "GlassLayer.h"
#import "GlassMacros.h"
#import "GlassScreen.h"
#import "GlassLayerCGL.h"
#import "GlassLayerMTL.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassLayer

static NSArray *allModes = nil;

- (id)initGlassLayer:(NSObject*)ctx
    andClientContext:(NSObject*)clCtx
         mtlQueuePtr:(long)mtlCommandQueuePtr
      withHiDPIAware:(BOOL)HiDPIAware
        withIsSwPipe:(BOOL)isSwPipe
       useMTLForBlit:(BOOL)useMTLInGlass
{
    LOG("GlassLayer initGlassLayer]");
    self = [super init];
    if (self != nil)
    {
        if (mtlCommandQueuePtr != 0l ||
            useMTLInGlass) { // MTL
            LOG("GlassLayer initGlassLayer using MTLLayer");
            GlassLayerMTL* mtlLayer = [[GlassLayerMTL alloc] init:mtlCommandQueuePtr
                                                     withIsSwPipe:isSwPipe];
            self->painterOffScreen = [mtlLayer getPainterOffscreen];
            self->glassOffScreen = nil;
            [self addSublayer:mtlLayer];
        } else {
            LOG("GlassLayer initGlassLayer using CGLLayer");
            GlassLayerCGL* cglLayer = [[GlassLayerCGL alloc] initWithSharedContext:(CGLContextObj)ctx
                                                                  andClientContext:(CGLContextObj)clCtx
                                                                    withHiDPIAware:HiDPIAware
                                                                      withIsSwPipe:isSwPipe];
            self->painterOffScreen = [cglLayer getPainterOffscreen];
            self->glassOffScreen = [cglLayer getGlassOffscreen];
            [self addSublayer:cglLayer];
        }
        self->isHiDPIAware = HiDPIAware;
        LOG("   GlassLayer context: %p", ctx);

        [self setAutoresizingMask:(kCALayerWidthSizable | kCALayerHeightSizable)];
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
    [super dealloc];
}

- (void)notifyScaleFactorChanged:(CGFloat)scale
{
    if (self->isHiDPIAware) {
        if ([self.sublayers[0] respondsToSelector:@selector(setContentsScale:)]) {
            [self.sublayers[0] setContentsScale: scale];
        }
    }
}

- (void)end
{
    [self->painterOffScreen flush:self->glassOffScreen];
}

- (void)bindForWidth:(unsigned int)width
           andHeight:(unsigned int)height
{
    [self->painterOffScreen bindForWidth:width
                               andHeight:height];
}

- (GlassOffscreen*)getPainterOffscreen
{
    return self->painterOffScreen;
}

- (void)pushPixels:(void*)pixels
         withWidth:(unsigned int)width
        withHeight:(unsigned int)height
        withScaleX:(float)scalex
        withScaleY:(float)scaley
            ofView:(NSView*)view
{
    [self->painterOffScreen pushPixels:pixels
                             withWidth:width
                            withHeight:height
                            withScaleX:scalex
                            withScaleY:scaley
                                ofView:view];
}

@end
