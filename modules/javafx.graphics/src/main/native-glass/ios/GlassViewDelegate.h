/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

#import <CoreFoundation/CoreFoundation.h>
#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>

#import "common.h"
#import "GlassDragDelegate.h"

@interface GlassGestureDelegate : NSObject<UIGestureRecognizerDelegate> {

}
@end

typedef __attribute__((NSObject)) CFMutableDictionaryRef GlassMutableDictionaryRef;

// helper class that implements the custom GlassView functionality
@interface GlassViewDelegate : NSObject<UIScrollViewDelegate, GlassDragSourceDelegate, UITextFieldDelegate>
{

}

// delegate owner glass view
@property (nonatomic, retain) UIScrollView *uiView; // native GlassViewGL; owner of this delegate
@property (nonatomic) jobject jView;
// scrolling
@property (nonatomic) CGPoint lastScrollOffset;
@property (nonatomic) BOOL ignoreNextScroll;
@property (nonatomic) BOOL isInertia; // are we scrolling with finger or is it momentum
@property (nonatomic) BOOL isScrolling; // are we yet scrolling
// mouse events emulation
@property (nonatomic, retain) UITouch * mouseTouch; // UITouch object associated with mouse emulation (i.e. first UITouch)
@property (nonatomic) CGPoint lastEventPoint; // coordinates of last 'mouse' event
// touches
@property (nonatomic, strong) GlassMutableDictionaryRef touches;
@property (nonatomic) jlong lastTouchId;
@property (nonatomic) CGPoint beginTouchEventPoint; // coordinates at the beginning of a 'touch' event
// gestures
@property (nonatomic, retain) GlassGestureDelegate *delegate;


- (id)initWithView:(UIScrollView*)view withJview:(jobject)jview;

- (void)viewDidMoveToWindow;
- (void)contentWillRecenter;
- (void)setBounds:(CGRect)boundsRect;

- (void)drawRect:(CGRect)dirtyRect;

// We are emulating mouse events
- (void)sendJavaMouseEvent:(CGPoint)viewPoint type:(int)type button:(int)button;

// Java events callbacks
- (void)sendJavaKeyEventWithType:(int)type keyCode:(int)code unicode:(int)unicode modifiers:(int)modif;

- (void)sendJavaTouchEvent:(UIEvent *)theEvent;

- (void) sendJavaInputMethodEvent:(NSString *) text clauseBoundary:(int[])clsBndr attrBoundary:(int[])attrBndr attrValue:(Byte[])attrVal
              committedTextLength:(int)cmtdTxtLen caretPos:(int)crtPos visiblePos:(int)visPos;

- (BOOL)suppressMouseEnterExitOnMouseDown;

// Touches callbacks
- (void)touchesBeganCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event;
- (void)touchesMovedCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event;
- (void)touchesEndedCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event;
- (void)touchesCancelledCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event;

// GlassDragSourceDelegate
- (void)startDrag:(int)operation;

@end
