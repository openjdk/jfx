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

#import "GlassViewDelegate.h"

#import "com_sun_glass_events_ViewEvent.h"
#import "com_sun_glass_events_MouseEvent.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_events_TouchEvent.h"

#import "GlassStatics.h"
#import "GlassHelper.h"
#import "GlassMacros.h"
#import "GlassWindow.h"

//#define VERBOSE_DND
#ifdef VERBOSE_DND

#define DNDLOG NSLog

#else

#define DNDLOG(...)

#endif



@implementation GlassGestureDelegate

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer{
    return YES;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer{
    return YES;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldReceiveTouch:(UITouch *)touch{
    return YES;
}
@end


// translate UITouchPhase to glass touch event
static jint getTouchStateFromPhase(int phase)
{
    switch (phase) {
        case UITouchPhaseBegan:
            return com_sun_glass_events_TouchEvent_TOUCH_PRESSED;
        case UITouchPhaseMoved:
            return com_sun_glass_events_TouchEvent_TOUCH_MOVED;
        case UITouchPhaseStationary:
            return com_sun_glass_events_TouchEvent_TOUCH_STILL;
        case UITouchPhaseEnded:
        case UITouchPhaseCancelled:
            return com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
    }
    return 0;
}


@implementation GlassViewDelegate

// see comments in header
@synthesize uiView;
@synthesize jView;
@synthesize touches;
@synthesize lastTouchId;
@synthesize delegate;
@synthesize lastScrollOffset;
@synthesize ignoreNextScroll;
@synthesize isInertia;
@synthesize isScrolling;
@synthesize mouseTouch;
@synthesize lastEventPoint;
@synthesize beginTouchEventPoint;


- (void)touchesBeganCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event
{
    DNDLOG(@"GlassViewDelegate touchesBeganCallback");
    if ([GlassDragDelegate isDragging] == YES) {
        if ([involvedTouches containsObject:self.mouseTouch] == YES) {
            // forward to GlassDragDelegate
            [GlassDragDelegate touchesBegan:involvedTouches withEvent:event withMouse:mouseTouch];
        }
        return;//During emulated dragging session we do not deliver any touches, gestures, etc.
    }

    [self sendJavaTouchEvent:event];

    //When we are starting first touch, let's associate the first one
    //touch with mouse emulation
    if (self.mouseTouch == nil) {
        UITouch *touch = [[event allTouches] anyObject];
        CGPoint viewPoint = [touch locationInView:self.uiView.superview];
        self.beginTouchEventPoint = viewPoint;

        self.mouseTouch = touch;

        //focus owning GlassWindow
        [self.uiView.superview.superview makeKeyWindow];

        [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_ENTER button:com_sun_glass_events_MouseEvent_BUTTON_NONE];
        [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_DOWN button:com_sun_glass_events_MouseEvent_BUTTON_LEFT];
    }
}


- (void)touchesMovedCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event
{
    DNDLOG(@"GlassViewDelegate touchesMovedCallback");

    if ([GlassDragDelegate isDragging] == YES) {
        if ([involvedTouches containsObject:self.mouseTouch] == YES) {
            [GlassDragDelegate touchesMoved:involvedTouches withEvent:event withMouse:mouseTouch];
        }
        return;//During emulated dragging session we do not deliver any touches, gestures, etc.
    }

    [self sendJavaTouchEvent:event];

    // emulate mouse
    if (self.mouseTouch != nil && [involvedTouches containsObject:self.mouseTouch] == YES) {
        CGPoint viewPoint = [self.mouseTouch locationInView:self.uiView.superview];
        // iOS might send one or more 'NSTouchPhaseMoved', even if the initial event location didn't change.
        // This check prevents emulating mouse drag events in such cases
        if (!CGPointEqualToPoint(viewPoint, self.beginTouchEventPoint)) {
            [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_DRAG button:com_sun_glass_events_MouseEvent_BUTTON_LEFT];
        }
    }
}


- (void)touchesEndedCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event
{
    DNDLOG(@"GlassViewDelegate touchesEndedCallback");

    if ([GlassDragDelegate isDragging] == YES) {
        //End dragging session
        if ([involvedTouches containsObject:self.mouseTouch] == YES) {
            [GlassDragDelegate touchesEnded:involvedTouches withEvent:event withMouse:self.mouseTouch];
        } else {
            return; //we do not deliver any other touches to java during drag/drop session, so we do not need to end them
        }
    }

    [self sendJavaTouchEvent:event];

    // emulate mouse
    if (self.mouseTouch != nil && [involvedTouches containsObject:self.mouseTouch] == YES) {
        CGPoint viewPoint = [self.mouseTouch locationInView:self.uiView.superview];
        self.mouseTouch = nil; // do this before we call into the Java layer, as this might call us back (enterNestedEventLoop) before returning

        [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_UP button:com_sun_glass_events_MouseEvent_BUTTON_LEFT];
        [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_EXIT button:com_sun_glass_events_MouseEvent_BUTTON_NONE];

    }
}

- (void)touchesCancelledCallback:(NSSet *)involvedTouches withEvent:(UIEvent *)event
{
    [self touchesEndedCallback:involvedTouches withEvent:event];
}


- (void)sendJavaGestureEndEvent:(CGPoint)point
{
    if ([GlassDragDelegate isDragging] == YES) { // no gestures during drag
        return;
    }
    jint modifiers = 0;

    GET_MAIN_JENV;
    (*env)->CallStaticVoidMethod(
        env,
        jGestureSupportClass,
        jGestureSupportGestureFinished,
        self.jView,
        modifiers,
        (jint)point.x, (jint)point.y,
        (jint)point.x, (jint)point.y
    );
    GLASS_CHECK_EXCEPTION(env);

    isScrolling = NO;
}


- (void)handlePinchGesture:(UIPinchGestureRecognizer*)sender {

    if ([GlassDragDelegate isDragging] == YES) { // no gestures during drag
        return;
    }

    CGPoint viewPoint = [sender locationInView:self.uiView.superview];
    CGPoint basePoint = [sender locationInView:self.uiView.superview];

    jint modifiers = 0;

    GET_MAIN_JENV;

    (*env)->CallStaticVoidMethod(
        env,
        jGestureSupportClass,
        jGestureSupportMagnifyGesturePerformed,
        self.jView,
        modifiers,
        (jint)viewPoint.x, (jint)viewPoint.y,
        (jint)basePoint.x, (jint)basePoint.y,
        (jfloat)([sender scale] - 1.0)
    );
    [sender setScale:1.0];

    GLASS_CHECK_EXCEPTION(env);
    if (sender.state == UIGestureRecognizerStateEnded) {
        [self sendJavaGestureEndEvent:viewPoint];
    }
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate {
    if ([GlassDragDelegate isDragging] == YES) {
        return;
    }

    CGPoint viewLoc = [self.uiView.panGestureRecognizer locationInView:self.uiView.superview];

    // This message is documented to be sent when the user lifts their finger.
    // We won't get a touch notification again, so send a mouse up/exit.



    // decelerate is true when inertia scrolling starts.
    if (decelerate) {
        isInertia = YES;
    }

    // end the gesture - inertial events are delivered after gesture is finished
    [self sendJavaGestureEndEvent:viewLoc];
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView {
    if ([GlassDragDelegate isDragging] == YES) {
        return;
    }
    // Sent when the scroll view has coasted to a stop. End the scroll gesture.
    isInertia = NO;
    isScrolling = NO;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    if ([GlassDragDelegate isDragging] == YES) { // no gestures while dragging
        return;
    }
    GET_MAIN_JENV;

    if (ignoreNextScroll) {
        ignoreNextScroll = NO;
        lastScrollOffset = self.uiView.contentOffset;
        return;
    }

    jint modifiers = 0;
    CGPoint viewLoc = [self.uiView.panGestureRecognizer locationInView:self.uiView.superview];
    CGPoint point;
    CGPoint currOffset = self.uiView.contentOffset;
    point.x = lastScrollOffset.x - currOffset.x;
    point.y = lastScrollOffset.y - currOffset.y;
    lastScrollOffset = currOffset;

    (*env)->CallStaticVoidMethod(env,
                                 jGestureSupportClass,
                                 jGestureSupportScrollGesturePerformed,
                                 self.jView,
                                 modifiers,
                                 isInertia,
                                 viewLoc.x, viewLoc.y,
                                 viewLoc.x, viewLoc.y,
                                 point.x, point.y);

    GLASS_CHECK_EXCEPTION(env);
}

- (void)contentWillRecenter {
    ignoreNextScroll = YES;
}


- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView {
    if ([GlassDragDelegate isDragging] == YES) { // no gestures during drag
        return;
    }

    GET_MAIN_JENV;

    CGPoint viewLoc = [self.uiView.panGestureRecognizer locationInView:self.uiView.superview];

    isInertia = NO;
    isScrolling = YES;

    CGPoint point;
    CGPoint currOffset = self.uiView.contentOffset;
    point.x = lastScrollOffset.x - currOffset.x;
    point.y = lastScrollOffset.y - currOffset.y;
    lastScrollOffset = currOffset;

    (*env)->CallStaticVoidMethod(env,
                                 jGestureSupportClass,
                                 jGestureSupportScrollGesturePerformed,
                                 self.jView,
                                 0,
                                 isInertia,
                                 viewLoc.x, viewLoc.y,
                                 viewLoc.x, viewLoc.y,
                                 point.x, point.y);

    GLASS_CHECK_EXCEPTION(env);
}


- (void)handleRotateGesture:(UIRotationGestureRecognizer*)sender {
    if ([GlassDragDelegate isDragging] == YES) { // no gestures while dragging
        return;
    }

    CGPoint viewPoint = [sender locationInView:self.uiView.superview];
    CGPoint basePoint = [sender locationInView:self.uiView.superview];

    jint modifiers = 0;
    jfloat rotation = [sender rotation];

    GET_MAIN_JENV;

    (*env)->CallStaticVoidMethod(
        env,
        jGestureSupportClass,
        jGestureSupportRotateGesturePerformed,
        self.jView,
        modifiers,
        (jint)viewPoint.x, (jint)viewPoint.y,
        (jint)basePoint.x, (jint)basePoint.y,
        rotation
    );

    [sender setRotation:0.0];

    if (sender.state == UIGestureRecognizerStateEnded) {
        [self sendJavaGestureEndEvent:viewPoint];
    }

    GLASS_CHECK_EXCEPTION(env);
}


- (void)handleLongPressGesture:(UILongPressGestureRecognizer*)sender {
    if (sender.state == UIGestureRecognizerStateBegan) {
        // Simulate right-click
        CGPoint viewPoint = [sender locationInView:self.uiView.superview];
        [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_ENTER button:com_sun_glass_events_MouseEvent_BUTTON_NONE];
        [self sendJavaMouseEvent:viewPoint type:com_sun_glass_events_MouseEvent_DOWN button:com_sun_glass_events_MouseEvent_BUTTON_RIGHT];
    } else if (sender.state == UIGestureRecognizerStateEnded) {
        // Prevent touch ended event
        self.mouseTouch = nil;
    }
}


- (id)initWithView:(UIScrollView*)view withJview:(jobject)jview
{
    self = [super init];
    if (self != nil)
    {
        GET_MAIN_JENV;

        // Owner View
        self.uiView = view; // native side
        self.uiView.delegate = self;
        self.jView = (*env)->NewGlobalRef(env, jview); // java side

        // Ensure JNI stuff related to gesture processing is ready
        if (NULL == jGestureSupportClass) {
            [GlassHelper ClassForName:"com.sun.glass.ui.ios.IosGestureSupport" withEnv:env];
        }
        self.touches = NULL;
        self.lastTouchId = 0;

        mouseTouch = nil;

        [view setMultipleTouchEnabled:YES];

        GlassGestureDelegate *ggDelegate = [[GlassGestureDelegate alloc] init];
        //Zoom
        UIPinchGestureRecognizer *pinchGesture =
            [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(handlePinchGesture:)];
        [pinchGesture setCancelsTouchesInView:NO];
        [pinchGesture setDelaysTouchesEnded:NO];
        [pinchGesture setDelaysTouchesBegan:NO];
        [self.uiView addGestureRecognizer:pinchGesture];
        [pinchGesture setDelegate:ggDelegate];
        [pinchGesture release];
        //Rotation
        UIRotationGestureRecognizer *rotateGesture =
            [[UIRotationGestureRecognizer alloc] initWithTarget:self action:@selector(handleRotateGesture:)];
        [rotateGesture setCancelsTouchesInView:NO];
        [rotateGesture setDelaysTouchesEnded:NO];
        [rotateGesture setDelaysTouchesBegan:NO];
        [self.uiView addGestureRecognizer:rotateGesture];
        [rotateGesture setDelegate:ggDelegate];
        [rotateGesture release];
        //Scrolling
        UIPanGestureRecognizer * panGestureRecognizer = self.uiView.panGestureRecognizer;
        [panGestureRecognizer setCancelsTouchesInView:NO];
        [panGestureRecognizer setDelaysTouchesBegan:NO];
        [panGestureRecognizer setDelaysTouchesEnded:NO];
        //LongPress
        UILongPressGestureRecognizer *longPressGesture =
            [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(handleLongPressGesture:)];
        [longPressGesture setCancelsTouchesInView:NO];
        [longPressGesture setDelaysTouchesEnded:NO];
        [longPressGesture setDelaysTouchesBegan:NO];
        [self.uiView addGestureRecognizer:longPressGesture];
        [longPressGesture setDelegate:ggDelegate];
        [longPressGesture release];
    }
    return self;
}


- (void)dealloc
{
    GET_MAIN_JENV;
    (*env)->DeleteGlobalRef(env, self.jView);
    self.jView = NULL;
    self.delegate = nil;
    self.touches = NULL;

    [super dealloc];
}


- (void)viewDidMoveToWindow
{
    GLASS_LOG("viewDidMoveToWindow; self: %@", self);

    GET_MAIN_JENV;
    if ([self.uiView window] != nil)
    {
        UIView *currView = self.uiView;
        while (currView) {
            GLASS_LOG("  view --> %@", currView);
            currView = currView.superview;
        }

        (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyView, com_sun_glass_events_ViewEvent_ADD);
    }
    else
    {
        (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyView, com_sun_glass_events_ViewEvent_REMOVE);
    }
}


-(void)setBounds:(CGRect)boundsRect;
{
    GLASS_LOG("GlassViewDelegate setFrame: %d,%d %dx%d", (int)boundsRect.origin.x, (int)boundsRect.origin.y, (int)boundsRect.size.width, (int)boundsRect.size.height);
    // also listen for resize view's notifications
    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyResize, (int)boundsRect.size.width, (int)boundsRect.size.height);
    GLASS_CHECK_EXCEPTION(env);

    [self.uiView setNeedsDisplay];
}


//drawRect is called by system. not very often. we simply redraw whole View
- (void)drawRect:(CGRect)dirtyRect
{
    GLASS_LOG("BEGIN View:drawRect %@: ", self);

    GLASS_LOG("[self bounds]: %f,%f %fx%f", [self.uiView bounds].origin.x, [self.uiView bounds].origin.y, [self.uiView bounds].size.width, [self.uiView bounds].size.height);
    GET_MAIN_JENV;
    jint x = (jint) [self.uiView bounds].origin.x;
    jint y = (jint) [self.uiView bounds].origin.y;
    jint w = (jint) [self.uiView bounds].size.width;
    jint h = (jint) [self.uiView bounds].size.height;
    (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyRepaint, x, y, w, h);

    GLASS_CHECK_EXCEPTION(env);

    GLASS_LOG("END drawRect");
}


- (void)sendJavaMouseEvent:(CGPoint)viewPoint type:(int)type button:(int)button
{
    jint modifiers = 0;
    if (type != com_sun_glass_events_MouseEvent_UP)
    {
        switch (button)
        {
            case com_sun_glass_events_MouseEvent_BUTTON_LEFT:
                modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_PRIMARY;
                break;
            case com_sun_glass_events_MouseEvent_BUTTON_RIGHT:
                modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_SECONDARY;
                break;
            case com_sun_glass_events_MouseEvent_BUTTON_OTHER:
                modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_MIDDLE;
                break;
            case com_sun_glass_events_MouseEvent_BUTTON_BACK:
                modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_BACK;
                break;
            case com_sun_glass_events_MouseEvent_BUTTON_FORWARD:
                modifiers |= com_sun_glass_events_KeyEvent_MODIFIER_BUTTON_FORWARD;
                break;
        }
    }

    jboolean isSynthesized = JNI_TRUE;
    jboolean isPopupTrigger = JNI_FALSE;
    if (type == com_sun_glass_events_MouseEvent_DOWN) {
        if (button == com_sun_glass_events_MouseEvent_BUTTON_RIGHT) {
            isPopupTrigger = JNI_TRUE;
        }
        if (button == com_sun_glass_events_MouseEvent_BUTTON_LEFT &&
            (modifiers & com_sun_glass_events_KeyEvent_MODIFIER_CONTROL))

        {
            isPopupTrigger = JNI_TRUE;
        }
    }

    switch (type) {
            // prepare GlassDragDelegate for possible drag,
        case com_sun_glass_events_MouseEvent_DOWN:
        case com_sun_glass_events_MouseEvent_DRAG:
            DNDLOG(@"mouse type ==  com_sun_glass_events_MouseEvent_DRAG  %d",type ==  com_sun_glass_events_MouseEvent_DRAG);
            [GlassDragDelegate setDelegate:self];
            // fall through to save the lastEvent
            // or for filtering out duplicate MOVE events
        case com_sun_glass_events_MouseEvent_MOVE:
            self.lastEventPoint = CGPointMake(viewPoint.x, viewPoint.y);
            break;

      }


    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyMouse, type, button,
                           (jint)viewPoint.x, (jint)viewPoint.y, (jint)viewPoint.x, (jint)viewPoint.y,
                           modifiers, isPopupTrigger, isSynthesized);
    GLASS_CHECK_EXCEPTION(env);

    if (isPopupTrigger) {
        jboolean isKeyboardTrigger = JNI_FALSE;
        (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyMenu,
                               (jint)viewPoint.x, (jint)viewPoint.y, (jint)viewPoint.x, (jint)viewPoint.y, isKeyboardTrigger);
        GLASS_CHECK_EXCEPTION(env);
    }
}


- (void)sendJavaKeyEventWithType:(int)type keyCode:(int)code unicode:(int)unicode modifiers:(int)modif
{
    GET_MAIN_JENV;

    (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyKey, type, code, unicode, modif);

    GLASS_CHECK_EXCEPTION(env);
}

-(void) sendJavaInputMethodEvent:(NSString *) text clauseBoundary:(int[])clsBndr attrBoundary:(int[])attrBndr attrValue:(Byte[])attrVal
            committedTextLength:(int)cmtdTxtLen caretPos:(int)crtPos visiblePos:(int)visPos
{
    GET_MAIN_JENV;

    jsize buflen = [text length];
    unichar buffer[buflen];
    [text getCharacters:buffer];
    jstring textStr = (*env)->NewString(env, (jchar *)buffer, buflen);

    jintArray clauseBoundaryArray = (*env)->NewIntArray(env, 0);

    jintArray attrBoundaryArray = (*env)->NewIntArray(env, 0);

    jbyteArray attrValueArray = (*env)->NewByteArray(env, 0);

    (*env)->CallVoidMethod(env, self.jView, mat_jViewNotifyInputMethod, textStr, clauseBoundaryArray,
                           attrBoundaryArray, attrValueArray, cmtdTxtLen, crtPos, visPos);

    GLASS_CHECK_EXCEPTION(env);
}

static BOOL isTouchEnded(int phase)
{
    return phase == UITouchPhaseEnded || phase == UITouchPhaseCancelled;
}


- (void)sendJavaTouchEvent:(UIEvent *)theEvent
{
    jint modifiers = 0;

    NSSet* touchPoints = [theEvent allTouches];
    jint touchPointCount = touchPoints.count;
    // Adjust 'touchPointCount'.
    for (UITouch* touch in touchPoints)
    {
        int phase = touch.phase;
        BOOL isPhaseEnded = isTouchEnded(phase);

        if (!isPhaseEnded)
        {
            continue;
        }

        if (self.touches == nil || CFDictionaryGetValue(self.touches, touch) == NULL)
        {
            --touchPointCount;
        }
    }

    if (!touchPointCount)
    {
        return;
    }

    GET_MAIN_JENV;

    (*env)->CallStaticVoidMethod(env, jGestureSupportClass,
                                jGestureSupportNotifyBeginTouchEvent,
                                self.jView, modifiers,
                                touchPointCount);
    GLASS_CHECK_EXCEPTION(env);

    BOOL endAllTouches = YES;

    for (UITouch* touch in touchPoints)
    {
        int phase = touch.phase;
        CGPoint pos = [touch locationInView:self.uiView.superview];

        BOOL isPhaseEnded = isTouchEnded(phase);

        if (!isPhaseEnded)
        {
            endAllTouches = NO;
        }

        if (self.touches == nil)
        {
            CFMutableDictionaryRef d = CFDictionaryCreateMutable(NULL, 0, NULL, NULL);
            self.touches = d;
            CFRelease(d); // because the property retained it.
        }

        id touchId = (id)CFDictionaryGetValue(self.touches, touch);
        if (touchId == nil)
        {
            if (isPhaseEnded)
            {
                continue;
            }

            touchId = [NSNumber numberWithInt:++(self.lastTouchId)];
            CFDictionaryAddValue(self.touches, touch, touchId);
        }
        else {
            if (phase == UITouchPhaseBegan)
            {   // Adjust 'phase'. This is needed as OS X sometimes sends
                // multiple 'NSTouchPhaseBegan' for the same touch point.
                phase = UITouchPhaseStationary;
            }
        }

        if (isPhaseEnded)
        {
            CFDictionaryRemoveValue(self.touches, touch);
        }

        (*env)->CallStaticVoidMethod(env, jGestureSupportClass,
                                    jGestureSupportNotifyNextTouchEvent,
                                    self.jView, getTouchStateFromPhase(phase),
                                    [touchId longLongValue],
                                    pos.x, pos.y);
        GLASS_CHECK_EXCEPTION(env);
    }

    (*env)->CallStaticVoidMethod(env, jGestureSupportClass,
                                jGestureSupportNotifyEndTouchEvent, self.jView);
    GLASS_CHECK_EXCEPTION(env);

    if (endAllTouches)
    {
        self.touches = NULL;
        self.lastTouchId = 0;
    }
}

// called from Java layer drag handler, triggered by DnD Pasteboard flush
- (void)startDrag:(int)operation
{
    DNDLOG(@"GlassViewDelegate startDrag with operation %d", operation);
    [GlassDragDelegate drag:self.lastEventPoint operation:operation glassView:(UIView*)self.uiView];
}


- (BOOL)suppressMouseEnterExitOnMouseDown
{
    return NO;
}

#pragma mark --- UITextFieldDelegate

-(BOOL)textFieldShouldReturn:(UITextField *)textField{
    [self sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_PRESS
                                          keyCode:com_sun_glass_events_KeyEvent_VK_ENTER
                                          unicode:(char)13
                                        modifiers:0];

    [[GlassWindow getMasterWindow] resignFocusOwner];

    return YES;
}


@end
