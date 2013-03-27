/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_events_TouchEvent.h"

#import "GlassMacros.h"
#import "GlassTouches.h"
#import "GlassKey.h"
#import "GlassHelper.h"
#import "GlassStatics.h"


//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif


static GlassTouches* glassTouches = nil;


@interface GlassTouches (hidden)

- (void)releaseTouches;

- (void)terminateImpl;

- (void)enableTouchInputEventTap;

- (void)sendJavaTouchEvent:(NSEvent *)theEvent;
- (void)notifyTouch:(JNIEnv*)env    identity:(const id)identity 
                                    phase:(NSUInteger)phase
                                    pos:(const NSPoint*)pos;
@end


static jint getTouchStateFromPhase(NSUInteger phase)
{
    switch (phase) 
    {
        case NSTouchPhaseBegan:
            return com_sun_glass_events_TouchEvent_TOUCH_PRESSED;
        case NSTouchPhaseMoved:
            return com_sun_glass_events_TouchEvent_TOUCH_MOVED;
        case NSTouchPhaseStationary:
            return com_sun_glass_events_TouchEvent_TOUCH_STILL;
        case NSTouchPhaseEnded:
        case NSTouchPhaseCancelled:
            return com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
    }
    return 0;
}


static BOOL isTouchEnded(NSUInteger phase)
{
    return phase == NSTouchPhaseEnded || phase == NSTouchPhaseCancelled;
}


static BOOL hasTouchWithIdentity(const id identity, const NSSet* touchPoints)
{
    for (const NSTouch* touch in touchPoints)
    {
        if ([identity isEqual:touch.identity])
        {
            return YES;
        }
    }
    return NO;
}


typedef struct 
{
    jlong touchId;
    jfloat x;
    jfloat y;
} TouchPoint;


static CGEventRef listenTouchEvents(CGEventTapProxy proxy, CGEventType type, 
                             CGEventRef event, void* refcon)
{
    if (type == kCGEventTapDisabledByTimeout)
    {   // OS may disable event tap if it handles events too slowly. 
        // This is undesirable, so enable event tap after such a reset.
        [glassTouches enableTouchInputEventTap];
        LOG("TOUCHES: listenTouchEvents: recover after timeout\n");
        return event;
    }

    NSEvent* theEvent = [NSEvent eventWithCGEvent:event];
    if (theEvent)
    {
        if (glassTouches)
        {
            [glassTouches sendJavaTouchEvent:theEvent];
        }
    }
    
    return event;
}


@implementation GlassTouches

+ (void)startTracking:(GlassViewDelegate *)delegate
{
    if (!glassTouches) 
    {
        glassTouches = [[GlassTouches alloc] init];
    }

    if (glassTouches)
    {
        glassTouches->curConsumer = delegate;
    }
    
    LOG("TOUCHES: startTracking: delegate=%p\n", glassTouches->curConsumer);
}

+ (void)stopTracking:(GlassViewDelegate *)delegate
{
    if (!glassTouches || glassTouches->curConsumer != delegate)
    {
        return;
    }

    // Keep updating java touch point counter, just have no view to notify.
    glassTouches->curConsumer = nil;
    
    LOG("TOUCHES: stopTracking: delegate=%p\n", glassTouches->curConsumer);
}

+ (void)updateTracking:(GlassViewDelegate *)oldDelegate newDelegate:(GlassViewDelegate *)newDelegate
{
    if (!glassTouches || glassTouches->curConsumer != oldDelegate)
    {
        return;
    }

    glassTouches->curConsumer = newDelegate;
    
    LOG("TOUCHES: updateTracking: old=%p new=%p\n", oldDelegate, glassTouches->curConsumer);
}

+ (void)terminate
{
    // Should be called right after Application's run loop terminate
    [glassTouches terminateImpl];
    glassTouches = nil;
}

- (id)init
{
    self = [super init];
    if (self != nil)
    {
        self->curConsumer   = nil;
        self->eventTap      = nil;
        self->runLoopSource = nil;
        self->touches       = nil;
        self->lastTouchId   = 0;
        
        //
        // Notes after fixing RT-23199:
        // 
        //  Don't use NSMachPort and NSRunLoop to integrate CFMachPortRef 
        //  instance into run loop.
        //
        // Ignoring the above "don't"s results into performance degradation 
        // referenced in the bug.
        //

        self->eventTap = CGEventTapCreate(kCGHIDEventTap, 
                                          kCGHeadInsertEventTap, 
                                          kCGEventTapOptionListenOnly, 
                                          CGEventMaskBit(NSEventTypeGesture), 
                                          listenTouchEvents, nil);

        LOG("TOUCHES: eventTap=%p\n", self->eventTap);

        if (self->eventTap)
        {   // Create a run loop source.
            self->runLoopSource = CFMachPortCreateRunLoopSource(
                                                        kCFAllocatorDefault, 
                                                        self->eventTap, 0);

            LOG("TOUCHES: runLoopSource=%p\n", self->runLoopSource);

            // Add to the current run loop.
            CFRunLoopAddSource(CFRunLoopGetCurrent(), self->runLoopSource, 
                               kCFRunLoopCommonModes);
        }
    }
    return self;
}

@end


@implementation GlassTouches (hidden)
- (void)terminateImpl
{
    LOG("TOUCHES: terminateImpl eventTap=%p runLoopSource=%p\n", self->eventTap, 
        self->runLoopSource);
    
    if (self->runLoopSource)
    {
        CFRunLoopRemoveSource(CFRunLoopGetCurrent(), self->runLoopSource, 
                              kCFRunLoopCommonModes);
        CFRelease(self->runLoopSource);
        self->runLoopSource = nil;
    }

    if (self->eventTap) 
    {
        CFRelease(self->eventTap);
        self->eventTap = nil;
    }

    [self releaseTouches];
}

- (void)enableTouchInputEventTap
{
    CGEventTapEnable(self->eventTap, true);
}

- (void)sendJavaTouchEvent:(NSEvent *)theEvent
{
    jint modifiers = GetJavaModifiers(theEvent);

    const NSSet* touchPoints = 
            [theEvent touchesMatchingPhase:NSTouchPhaseAny inView:nil];

    //
    // Known issues with OSX touch input:
    // - multiple 'NSTouchPhaseBegan' for the same touch point;
    // - missing 'NSTouchPhaseEnded' for released touch points 
    //  (RT-20139, RT-20375);
    //

    //
    // Find just released touch points that are not in the cache already. 
    // Don't send TouchEvent#TOUCH_RELEASED for these touch points.
    //
    jint noReleaseTouchPointCount = 0;
    for (NSTouch* touch in touchPoints)
    {
        NSUInteger phase = touch.phase;
        BOOL isPhaseEnded = isTouchEnded(phase);

        if (!isPhaseEnded) 
        {
            continue;
        }
        
        if (self->touches == nil || 
            [self->touches objectForKey:touch.identity] == nil)
        {
            ++noReleaseTouchPointCount;
        }
    }
    
    //
    // Find cached touch points that are not in the curent set of touch points.
    // Should send TouchEvent#TOUCH_RELEASED for these touch points.
    //
    NSMutableArray* releaseTouchIds = nil;
    if (self->touches != nil) 
    {
        for (id identity in self->touches)
        {
            if (!hasTouchWithIdentity(identity, touchPoints))
            {
                if (!releaseTouchIds)
                {
                    releaseTouchIds = [NSMutableArray array];
                }
                [releaseTouchIds addObject:identity];
            }
        }
    }
    
    const jint touchPointCount =
            (jint)touchPoints.count 
                - (jint)noReleaseTouchPointCount  + (jint)(releaseTouchIds == nil ? 0 : releaseTouchIds.count);
    if (!touchPointCount)
    {
        return;
    }

    GET_MAIN_JENV;
    const jclass jGestureSupportClass = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacGestureSupport"
                                                          withEnv:env];
    if (jGestureSupportClass)
    {
        (*env)->CallStaticVoidMethod(env, jGestureSupportClass,
                                     javaIDs.GestureSupport.notifyBeginTouchEvent,
                                     [self->curConsumer jView], modifiers,
                                     touchPointCount);
    }
    GLASS_CHECK_EXCEPTION(env);

    if (self->touches == nil && touchPointCount) 
    {
        self->touches = [[NSMutableDictionary alloc] init];
    }
    
    if (releaseTouchIds != nil)
    {
        for (id identity in releaseTouchIds)
        {
            [self notifyTouch:env 
                            identity:identity 
                            phase:NSTouchPhaseEnded 
                            pos:nil];
        }
    }

    for (NSTouch* touch in touchPoints)
    {
        const NSPoint pos = touch.normalizedPosition;
        [self notifyTouch:env 
                        identity:touch.identity 
                        phase:touch.phase 
                        pos:&pos];
    }

    if (jGestureSupportClass)
    {
        (*env)->CallStaticVoidMethod(env, jGestureSupportClass,
                                     javaIDs.GestureSupport.notifyEndTouchEvent,
                                     [self->curConsumer jView]);
    }
    GLASS_CHECK_EXCEPTION(env);

    if ([self->touches count] == 0)
    {
        [self releaseTouches];
        self->lastTouchId = 0;
    }
}

- (void)notifyTouch:(JNIEnv*)env identity:(const id)identity phase:(NSUInteger)phase
                    pos:(const NSPoint*)pos;
{
    const BOOL isPhaseEnded = isTouchEnded(phase);

    TouchPoint tp;
    NSValue* ctnr = [self->touches objectForKey:identity];
    if (ctnr == nil)
    {
        if (isPhaseEnded)
        {
            return;
        }
        tp.touchId = ++(self->lastTouchId);
        
        if (phase != NSTouchPhaseBegan) 
        {   // Adjust 'phase'. By some reason OS X sometimes doesn't send
            // 'NSTouchPhaseBegan' for the just appeared touch point.
            phase = NSTouchPhaseBegan;
        }
    }
    else
    {
        [ctnr getValue:&tp];
        
        if (phase == NSTouchPhaseBegan)
        {   // Adjust 'phase'. This is needed as OS X sometimes sends
            // multiple 'NSTouchPhaseBegan' for the same touch point.
            phase = NSTouchPhaseStationary;
        }
    }

    if (pos)
    {   // update stored position
        tp.x = (jfloat)pos->x;
        tp.y = (jfloat)pos->y;
    }
    
    if (isPhaseEnded)
    {
        [self->touches removeObjectForKey:identity];
    }
    else
    {
        ctnr = [NSValue valueWithBytes:&tp objCType:@encode(TouchPoint)];
        [self->touches setObject:ctnr forKey:identity];
    }

    const jclass jGestureSupportClass = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacGestureSupport"
                                                          withEnv:env];
    if (jGestureSupportClass)
    {
        (*env)->CallStaticVoidMethod(env, jGestureSupportClass,
                                     javaIDs.GestureSupport.notifyNextTouchEvent,
                                     [self->curConsumer jView],
                                     getTouchStateFromPhase(phase),
                                     tp.touchId, tp.x, tp.y);
    }
    GLASS_CHECK_EXCEPTION(env);
}

- (void)releaseTouches
{
    [self->touches release];
    self->touches = nil;
}

@end
