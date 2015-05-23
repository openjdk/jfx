/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_events_DndEvent.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_events_MouseEvent.h"
#import "com_sun_glass_ui_mac_MacGestureSupport.h"

#import "GlassMacros.h"
#import "GlassView2D.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

@implementation GlassView2D

- (id)initWithFrame:(NSRect)frame withJview:(jobject)jView withJproperties:(jobject)jproperties
{
    self = [super initWithFrame:frame];
    if (self != nil)
    {
        self->delegate = [[GlassViewDelegate alloc] initWithView:self withJview:jView];

        self->trackingArea = [[NSTrackingArea alloc] initWithRect:frame
            options:(NSTrackingMouseMoved | NSTrackingActiveAlways | NSTrackingInVisibleRect)
            owner:self userInfo:nil];
        [self addTrackingArea: self->trackingArea];
    }
    return self;
}

- (void)dealloc
{
    [self->delegate release];
    self->delegate = nil;

    [self removeTrackingArea: self->trackingArea];
    [self->trackingArea release];
    self->trackingArea = nil;
    
    [super dealloc];
}

- (BOOL)becomeFirstResponder
{
    return YES;
}

- (BOOL)acceptsFirstResponder
{
    return YES;
}

- (BOOL)canBecomeKeyView
{
    return YES;
}

- (BOOL)postsBoundsChangedNotifications
{
    return NO;
}

- (BOOL)postsFrameChangedNotifications
{
    return NO;
}

- (BOOL)acceptsFirstMouse:(NSEvent *)theEvent
{
    return YES;
}

- (BOOL)isFlipped
{
    return YES;
}

- (BOOL)isOpaque
{
    return NO;
}

- (BOOL)mouseDownCanMoveWindow
{
    return NO;
}

// also called when closing window, when [self window] == nil
- (void)viewDidMoveToWindow
{
    [self->delegate viewDidMoveToWindow];
}

- (void)setFrameOrigin:(NSPoint)newOrigin
{
    [super setFrameOrigin:newOrigin];
    [self->delegate setFrameOrigin:newOrigin];
}

- (void)setFrameSize:(NSSize)newSize
{
    [super setFrameSize:newSize];
    [self->delegate setFrameSize:newSize];
}

- (void)setFrame:(NSRect)frameRect
{
    [super setFrame:frameRect];
    [self->delegate setFrame:frameRect];
}

- (void)updateTrackingAreas
{
    [super updateTrackingAreas];
    [self->delegate updateTrackingAreas];
}

- (NSMenu *)menuForEvent:(NSEvent *)theEvent
{
    [self->delegate sendJavaMenuEvent:theEvent];
    return [super menuForEvent: theEvent];
}

- (void)mouseEntered:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseMoved:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseExited:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseDown:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseDragged:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseUp:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)rightMouseDown:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
    // By default, calling rightMouseDown: generates menuForEvent: but none of the other glass mouse handlers call the super
    // To be consistent with the rest of glass, call the menu event handler directly rather than letting the operating system do it
    [self->delegate sendJavaMenuEvent:theEvent];
//    return [super rightMouseDown: theEvent];
}

- (void)rightMouseDragged:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)rightMouseUp:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)otherMouseDown:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)otherMouseDragged:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)otherMouseUp:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (void)rotateWithEvent:(NSEvent *)theEvent
{
    [self->delegate sendJavaGestureEvent:theEvent type:com_sun_glass_ui_mac_MacGestureSupport_GESTURE_ROTATE];
}

- (void)swipeWithEvent:(NSEvent *)theEvent
{
    [self->delegate sendJavaGestureEvent:theEvent type:com_sun_glass_ui_mac_MacGestureSupport_GESTURE_SWIPE];
}

- (void)magnifyWithEvent:(NSEvent *)theEvent
{
    [self->delegate sendJavaGestureEvent:theEvent type:com_sun_glass_ui_mac_MacGestureSupport_GESTURE_MAGNIFY];
}

- (void)endGestureWithEvent:(NSEvent *)theEvent
{
    [self->delegate sendJavaGestureEndEvent:theEvent];
}

- (void)beginGestureWithEvent:(NSEvent *)theEvent
{
    [self->delegate sendJavaGestureBeginEvent:theEvent];
}

- (void)scrollWheel:(NSEvent *)theEvent
{
    [self->delegate sendJavaMouseEvent:theEvent];
}

- (BOOL)performKeyEquivalent:(NSEvent *)theEvent
{
    [self->delegate sendJavaKeyEvent:theEvent isDown:YES];
    return NO; // return NO to allow system-default processing of Cmd+Q, etc.
}

- (void)keyDown:(NSEvent *)theEvent
{
    [self->delegate sendJavaKeyEvent:theEvent isDown:YES];
}

- (void)keyUp:(NSEvent *)theEvent
{
    [self->delegate sendJavaKeyEvent:theEvent isDown:NO];
}

- (void)flagsChanged:(NSEvent *)theEvent
{
    [self->delegate sendJavaModifierKeyEvent:theEvent];
}

- (BOOL)wantsPeriodicDraggingUpdates
{
    // we only want want updated drag operations when the mouse position changes
    return NO;
}

- (BOOL)prepareForDragOperation:(id <NSDraggingInfo>)sender
{
    return YES;
}

- (BOOL)performDragOperation:(id <NSDraggingInfo>)sender
{
    [self->delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_PERFORM];
    
    return YES;
}

- (void)concludeDragOperation:(id <NSDraggingInfo>)sender
{
    
}

- (NSDragOperation)draggingEntered:(id <NSDraggingInfo>)sender
{
    return [self->delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_ENTER];
}

- (NSDragOperation)draggingUpdated:(id <NSDraggingInfo>)sender
{
    return [self->delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_UPDATE];
}

- (void)draggingEnded:(id <NSDraggingInfo>)sender
{
    [self->delegate draggingEnded];
}

- (void)draggingExited:(id <NSDraggingInfo>)sender
{
    [self->delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_EXIT];
}

- (NSDragOperation)draggingSourceOperationMaskForLocal:(BOOL)isLocal
{
    return [self->delegate draggingSourceOperationMaskForLocal:isLocal];
}

- (void)drawRect:(NSRect)dirtyRect
{
    [self->delegate drawRect:dirtyRect];
}

- (void)enterFullscreenWithAnimate:(BOOL)animate withKeepRatio:(BOOL)keepRatio withHideCursor:(BOOL)hideCursor
{
    [self->delegate enterFullscreenWithAnimate:animate withKeepRatio:keepRatio withHideCursor:hideCursor];
}

- (void)exitFullscreenWithAnimate:(BOOL)animate
{
    [self->delegate exitFullscreenWithAnimate:animate];
}

// below are methods that are 2D specific
- (void)begin
{
    CGContextRef cgContext = [[NSGraphicsContext currentContext] graphicsPort];
    CGContextSaveGState(cgContext);
    {
#if 0
        NSRect bounds = [self bounds];
        fprintf(stderr, "bounds: %f,%f %fx%f\n", bounds.origin.x, bounds.origin.y, bounds.size.width, bounds.size.height);
        NSRect frame = [self frame];
        fprintf(stderr, "frame: %f,%f %fx%f\n", frame.origin.x, frame.origin.y, frame.size.width, frame.size.height);
        
        CGRect bbox = CGContextGetClipBoundingBox(cgContext);
        fprintf(stderr, "bbox: %f,%f %fx%f\n", bbox.origin.x, bbox.origin.y, bbox.size.width, bbox.size.height);
        CGAffineTransform ctm = CGContextGetCTM(cgContext);
        fprintf(stderr, "ctm: a:%f, b:%f, c:%f, d:%f, tx:%f, ty:%f\n", ctm.a, ctm.b, ctm.c, ctm.d, ctm.tx, ctm.ty);
#endif
        // gznote: we could clear the surface for the client, but the client should be responsible for drawing
        // and if garbage appears on the screen it's because the client is not drawing in response to system repaints
        //CGContextClearRect(cgContext, CGRectMake(0, 0, [self bounds].size.width, [self bounds].size.height));
    }
}

- (void)end
{
    CGContextRef cgContext = [[NSGraphicsContext currentContext] graphicsPort];
    {
#if 0
        [[NSColor blackColor] setStroke];
        NSBezierPath *path = [NSBezierPath bezierPath];
        [path moveToPoint:NSMakePoint(0.0f, 0.0f)];
        [path lineToPoint:NSMakePoint([self bounds].size.width, [self bounds].size.height)];
        [path moveToPoint:NSMakePoint(0.0f, [self bounds].size.height)];
        [path lineToPoint:NSMakePoint([self bounds].size.width, 0.0f)];
        [path stroke];
#endif
#if 0
        CGContextFillRect(cgContext, CGRectMake(0, 0, 128, 128));
#endif
        
        CGContextFlush(cgContext);
    }
    CGContextRestoreGState(cgContext);
}

- (void)pushPixels:(void*)pixels withWidth:(GLuint)width withHeight:(GLuint)height withScale:(GLfloat)scale withEnv:(JNIEnv *)env
{
    assert([NSGraphicsContext currentContext] != nil);
    
    CGContextRef cgContext = [[NSGraphicsContext currentContext] graphicsPort];
    {
        CGImageRef cgImage = NULL;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        {
            CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, pixels, width*height*4, NULL);
            {
                cgImage = CGImageCreate(width, height, 8, 32, 4*width, colorSpace, kCGImageAlphaPremultipliedFirst|kCGBitmapByteOrder32Little, provider, NULL, true, kCGRenderingIntentDefault);
                {
                    NSSize size = [self bounds].size;
                    if ((size.width != width) || (size.height != height))
                    {
                        //NSLog(@"Glass View2D size: %dx%d, but pixels size: %dx%d", (int)size.width, (int)size.height, width, height);
                        //CGContextClearRect(cgContext, CGRectMake(0, 0, size.width, size.height));
                    }
                    
                    CGContextSaveGState(cgContext);
                    {
                        CGContextTranslateCTM(cgContext, 0, size.height);
                        CGContextScaleCTM(cgContext, 1, -1);
                        CGContextSetBlendMode(cgContext, kCGBlendModeCopy);
                        CGContextDrawImage(cgContext, CGRectMake(0, 0, width/scale, height/scale), cgImage);
                    }
                    CGContextRestoreGState(cgContext);
                }
                CGImageRelease(cgImage);
            }
            CGDataProviderRelease(provider);
        }
        CGColorSpaceRelease(colorSpace);
    }
    CGContextFlush(cgContext); // implicit flush
}

- (GlassViewDelegate*)delegate
{
    return self->delegate;
}

- (void)setInputMethodEnabled:(BOOL)enabled
{
    // Just a no-op here, GlassView2D does not support the IM interface
}

- (void)notifyScaleFactorChanged:(CGFloat)scale
{
    // no-op
}

@end
