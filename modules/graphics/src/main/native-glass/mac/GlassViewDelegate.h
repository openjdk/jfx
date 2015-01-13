/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
#import <jni.h>

#import "GlassHostView.h"
#import "GlassFullscreenWindow.h"
#import "GlassDragSource.h"
#import "GlassAccessible.h"

// helper class that implements the custom GlassView functionality
@interface GlassViewDelegate : NSObject <GlassDragSourceDelegate>
{
    NSView                  *nsView;
    
    NSTrackingRectTag       trackingRect;
    
    GlassHostView           *parentHost;
    NSWindow                *parentWindow;
    CGFloat                 parentWindowAlpha;
    
    GlassHostView           *fullscreenHost;
    // not nil when the FS mode is initiated with the OS X 10.7 widget
    NSWindow*               nativeFullScreenModeWindow;
    
    BOOL                    mouseIsDown;
    BOOL                    mouseIsOver;
    int                     mouseDownMask; // bit 0 - left, 1 - right, 2 - other button
    
    BOOL                    gestureInProgress;
    
    NSEvent                 *lastEvent;
    NSDragOperation         dragOperation;
    NSInteger               lastTrackingNumber;
    
@public
    jobject                 jView;
    // not nil when we create a new FS window ourselves
    GlassFullscreenWindow   *fullscreenWindow;
}

- (id)initWithView:(NSView*)view withJview:(jobject)jview;

- (void)viewDidMoveToWindow;
- (void)setFrameSize:(NSSize)newSize;
- (void)setFrame:(NSRect)frameRect;
- (void)updateTrackingAreas;
- (void)drawRect:(NSRect)dirtyRect;

- (void)sendJavaMouseEvent:(NSEvent *)theEvent;
- (void)resetMouseTracking;
- (void)sendJavaMenuEvent:(NSEvent *)theEvent;
- (void)sendJavaKeyEvent:(NSEvent *)event isDown:(BOOL)isDown;
- (void)sendJavaModifierKeyEvent:(NSEvent *)theEvent;
- (void)sendJavaGestureEvent:(NSEvent *)theEvent type:(int)type;
- (void)sendJavaGestureBeginEvent:(NSEvent *)theEvent;
- (void)sendJavaGestureEndEvent:(NSEvent *)theEvent;

- (NSDragOperation)sendJavaDndEvent:(id <NSDraggingInfo>)info type:(jint)type;

- (NSDragOperation)draggingSourceOperationMaskForLocal:(BOOL)isLocal;
- (void)startDrag:(NSDragOperation)operation;

- (BOOL)suppressMouseEnterExitOnMouseDown;

- (void)enterFullscreenWithAnimate:(BOOL)animate withKeepRatio:(BOOL)keepRatio withHideCursor:(BOOL)hideCursor;
- (void)exitFullscreenWithAnimate:(BOOL)animate;
- (void)sendJavaFullScreenEvent:(BOOL)entered withNativeWidget:(BOOL)isNative;

- (void)notifyInputMethod:(id)aString attr:(int)attr length:(int)length cursor:(int)cursor selectedRange:(NSRange)selectionRange;
- (NSRect)getInputMethodCandidatePosRequest:(int)pos;

- (void)setFrameOrigin:(NSPoint)newOrigin;

- (jobject)jView;

- (GlassAccessible*)getAccessible;

@end
