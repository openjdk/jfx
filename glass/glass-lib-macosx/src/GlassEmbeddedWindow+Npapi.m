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
#import "com_sun_glass_events_mac_NpapiEvent.h"
#import "com_sun_glass_ui_Window.h"
#import "com_sun_glass_ui_mac_MacWindow.h"

#import "GlassMacros.h"
#import "GlassEmbeddedWindow+Npapi.h"
#import "GlassNSEvent.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#pragma mark --- Internal utilities

inline GlassEmbeddedWindow *getGlassEmbeddedWindow(JNIEnv *env, jlong jPtr)
{
    if (jPtr != 0L)
    {
        return (GlassEmbeddedWindow*)jlong_to_ptr(jPtr);
    }
    else
    {
        return nil;
    }
}

static inline NSString* getNSString(JNIEnv* env, jstring jstring)
{
    NSString *string = @"";
    if (jstring != NULL)
    {
        const jchar* jstrChars = (*env)->GetStringChars(env, jstring, NULL);
        jsize size = (*env)->GetStringLength(env, jstring);
        if (size > 0)
        {
            string = [[[NSString alloc] initWithCharacters:jstrChars length:(NSUInteger)size] autorelease];
        }
        (*env)->ReleaseStringChars(env, jstring, jstrChars);
    }
    return string;
}

#pragma mark --- GlassEmbeddedWindow (Npapi)

@implementation GlassEmbeddedWindow (Npapi)

@end

#pragma mark --- Java APIs

/*
 * Class:     com_sun_glass_events_mac_NpapiEvent
 * Method:    _dispatchCocoaNpapiDrawEvent
 * Signature: (JIJDDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiDrawEvent
(JNIEnv *env, jclass jNpapiClass, jlong jPtr, jint jType, jlong jContext, jdouble jX, jdouble jY, jdouble jWidth, jdouble jHeight)
{
    LOG("Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiDrawEvent");

    // NOP, we use layer based architecture, so we will never get draw event
    NSLog(@"WARNING: GlassEmbeddedWindow+Npapi received _dispatchCocoaNpapiDrawEvent");
}

/*
 * Class:     com_sun_glass_events_mac_NpapiEvent
 * Method:    _dispatchCocoaNpapiMouseEvent
 * Signature: (JIIDDIIDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiMouseEvent
(JNIEnv *env, jclass jNpapiClass, jlong jPtr, jint jType, jint jModifierFlags, jdouble jPluginX, jdouble jPluginY, jint jButtonNumber, jint jClickCount, jdouble jDeltaX, jdouble jDeltaY, jdouble jDeltaZ)
{
    LOG("Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiMouseEvent");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassEmbeddedWindow *window = getGlassEmbeddedWindow(env, jPtr);
        if ((window != nil) && (window->child != nil))
        {
            NSEventType type = 0;
            switch (jType)
            {
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventMouseEntered:
                    LOG("   NSMouseEntered");
                    type = NSMouseEntered;
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventMouseExited:
                    LOG("   NSMouseExited");
                    type = NSMouseExited;
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventMouseDown:
                    LOG("   NSLeftMouseDown");
                    if (jButtonNumber == 0) {
                        type = NSLeftMouseDown;
                    } else if (jButtonNumber == 1) {
                        type = NSRightMouseDown;
                    } else {
                        type = NSOtherMouseDown;
                    }
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventMouseUp:
                    LOG("   NSLeftMouseUp");
                    if (jButtonNumber == 0) {
                        type = NSLeftMouseUp;
                    } else if (jButtonNumber == 1) {
                        type = NSRightMouseUp;
                    } else {
                        type = NSOtherMouseUp;
                    }
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventMouseDragged:
                    LOG("   NSLeftMouseDragged");
                    if (jButtonNumber == 0) {
                        type = NSLeftMouseDragged;
                    } else if (jButtonNumber == 1) {
                        type = NSRightMouseDragged;
                    } else {
                        type = NSOtherMouseDragged;
                    }
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventMouseMoved:
                    LOG("   NSMouseMoved");
                    type = NSMouseMoved;
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventScrollWheel:
                    LOG("   NSScrollWheel");
                    type = NSScrollWheel;
                    break;
            }

            NSEvent *event = nil;
            jdouble windowY = [window->child frame].size.height - jPluginY;
            if ((type == NSMouseEntered) || (type == NSMouseExited))
            {
                event = [NSEvent enterExitEventWithType:type
                                               location:NSMakePoint((CGFloat)jPluginX, (CGFloat)windowY)
                                          modifierFlags:(NSUInteger)jModifierFlags
                                              timestamp:[NSDate timeIntervalSinceReferenceDate]
                                           windowNumber:[window->child windowNumber]
                                                context:nil
                                            eventNumber:0
                                         trackingNumber:0
                                               userData:nil];
                [event setValue:window->child forKey:@"window"];
                LOG("   NPAPI mouse event: %s", [[event description] UTF8String]);
            } else if (type != NSScrollWheel) {
                NSPoint eventPoint = NSMakePoint((CGFloat)jPluginX, (CGFloat)windowY);
                event = [NSEvent mouseEventWithType:type
                                           location:eventPoint
                                      modifierFlags:0
                                          timestamp:[NSDate timeIntervalSinceReferenceDate]
                                       windowNumber:[window->child windowNumber]
                                            context:nil
                                        eventNumber:0
                                         clickCount:jClickCount
                                           pressure:0.0f];
                [event setValue:window->child forKey:@"window"];
                LOG("   NPAPI mouse event: %s", [[event description] UTF8String]);
            }
            else
            {
                CGEventRef scrollEvent = CGEventCreateScrollWheelEvent
                    (NULL, kCGScrollEventUnitPixel, 3, (int)jDeltaY, (int)jDeltaX, (int)jDeltaZ);
                event = [NSEvent eventWithCGEvent:scrollEvent];
                NSValue *location = [NSValue valueWithPoint:NSMakePoint((CGFloat)jPluginX, (CGFloat)windowY)];
                [event setValue:location forKey:@"location"];
                [event setValue:[NSNumber numberWithInteger:[window->child windowNumber]] forKey:@"windowNumber"];
            }

            if (event != nil)
            {
                dispatch_async(dispatch_get_main_queue(),
                   ^{
                       [window->child sendEvent:event];
                   });
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_events_mac_NpapiEvent
 * Method:    _dispatchCocoaNpapiKeyEvent
 * Signature: (JIILjava/lang/String;Ljava/lang/String;ZI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiKeyEvent
(JNIEnv *env, jclass jNpapiClass, jlong jPtr, jint jType, jint jModifierFlags, jstring jCharacters,
    jstring jCharactersIgnoringModifiers, jboolean jIsrepeat, jint jKeyCode, jboolean jNeedsKeyTyped)
{
    LOG("Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiKeyEvent");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassEmbeddedWindow *window = getGlassEmbeddedWindow(env, jPtr);
        if ((window != nil) && (window->child != nil))
        {
            NSEventType type = 0;
            switch (jType)
            {
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventKeyDown:
                    type = NSKeyDown;
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventKeyUp:
                    type = NSKeyUp;
                    break;
                case com_sun_glass_events_mac_NpapiEvent_NPCocoaEventFlagsChanged:
                    type = NSFlagsChanged;
                    break;
            }

            GlassNSEvent *event = (GlassNSEvent *)[GlassNSEvent keyEventWithType:type
                                              location:NSMakePoint(0.0, 0.0)
                                         modifierFlags:(NSUInteger)jModifierFlags
                                             timestamp:[NSDate timeIntervalSinceReferenceDate]
                                          windowNumber:[window->child windowNumber]
                                               context:nil
                                            characters:getNSString(env, jCharacters)
                           charactersIgnoringModifiers:getNSString(env, jCharactersIgnoringModifiers)
                                             isARepeat:(jIsrepeat==JNI_TRUE)
                                               keyCode:(unsigned short)jKeyCode];
            LOG("   NPAPI key event: %s", [[event description] UTF8String]);
            if (event != nil)
            {
                [event setNeedsKeyTyped:(jNeedsKeyTyped==JNI_TRUE)];
                dispatch_async(dispatch_get_main_queue(),
                               ^{
                                   [window->child sendEvent:event];
                               });
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_events_mac_NpapiEvent
 * Method:    _dispatchCocoaNpapiFocusEvent
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiFocusEvent
(JNIEnv *env, jclass jNpapiClass, jlong jPtr, jint jType, jboolean jHasFocus)
{
    LOG("Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiFocusEvent");
    LOG("   jPtr: %p", jPtr);
    LOG("   jHasFocus: %d", jHasFocus);

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassEmbeddedWindow *window = getGlassEmbeddedWindow(env, jPtr);
        if ((window != nil) && (window->child != nil))
        {
            if (jHasFocus == JNI_TRUE)
            {
                [window->child performSelectorOnMainThread:@selector(makeKeyWindow) withObject:nil waitUntilDone:NO];
            }
            else
            {
                [window->child performSelectorOnMainThread:@selector(resignKeyWindow) withObject:nil waitUntilDone:NO];
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_events_mac_NpapiEvent
 * Method:    _dispatchCocoaNpapiTextInputEvent
 * Signature: (JILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiTextInputEvent
(JNIEnv *env, jclass jNpapiClass, jlong jPtr, jint jType, jstring jText)
{
    LOG("Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiTextInputEvent");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassEmbeddedWindow *window = getGlassEmbeddedWindow(env, jPtr);
        NSString *chars = getNSString(env, jText);
        if ((window != nil) && (window->child != nil))
        {
            unichar *unichars = malloc([chars length] * sizeof(unichar));
            [chars getCharacters:unichars range:NSMakeRange(0, [chars length])];

            // Create a key-typed event for each character in the text input event.
            // This is better than sending a text input event because in the NPAPI
            // case there is no in-progress text to display.
            for (NSUInteger i = 0; i < [chars length]; i++) {
                NSString *singleChar = [NSString stringWithCharacters:&unichars[i] length:1];
                GlassNSEvent *event =
                (GlassNSEvent *)[GlassNSEvent keyEventWithType:NSKeyDown
                                                      location:NSMakePoint(0.0, 0.0)
                                                 modifierFlags:0
                                                     timestamp:[NSDate timeIntervalSinceReferenceDate]
                                                  windowNumber:[window->child windowNumber]
                                                       context:nil
                                                    characters:singleChar
                                   charactersIgnoringModifiers:singleChar
                                                     isARepeat:NO
                                                       keyCode:0];
                [event setSyntheticKeyTyped:YES];
                dispatch_async(dispatch_get_main_queue(),
                               ^{
                                   [window->child sendEvent:event];
                               });
            }
        }

        if ([chars length] == 1) {
            Java_com_sun_glass_events_mac_NpapiEvent__1dispatchCocoaNpapiKeyEvent(
                env, jNpapiClass, jPtr, com_sun_glass_events_mac_NpapiEvent_NPCocoaEventKeyUp,
                0, jText, jText, JNI_FALSE, 0, JNI_FALSE);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}
