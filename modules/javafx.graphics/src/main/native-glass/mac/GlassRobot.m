/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_ui_mac_MacRobot.h"

#import <CoreServices/CoreServices.h>
#import <ApplicationServices/ApplicationServices.h>

#import "GlassMacros.h"
#import "GlassKey.h"
#import "GlassHelper.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define kMouseButtonNone 0

static inline void DumpImage(CGImageRef image)
{
    fprintf(stderr, "CGImageRef: %p\n", image);
    if (image != NULL)
    {
        fprintf(stderr, "    CGImageGetWidth(): %d\n", (int)CGImageGetWidth(image));
        fprintf(stderr, "    CGImageGetHeight(): %d\n", (int)CGImageGetHeight(image));
        fprintf(stderr, "    CGImageGetBitsPerComponent(): %d\n", (int)CGImageGetBitsPerComponent(image));
        fprintf(stderr, "    CGImageGetBitsPerPixel(): %d\n", (int)CGImageGetBitsPerPixel(image));
        fprintf(stderr, "    CGImageGetBytesPerRow(): %d\n", (int)CGImageGetBytesPerRow(image));
        CGImageAlphaInfo alpha = CGImageGetAlphaInfo(image) & kCGBitmapAlphaInfoMask;
        switch (alpha)
        {
            case kCGImageAlphaNone: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaNone\n"); break;
            case kCGImageAlphaPremultipliedLast: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaPremultipliedLast\n"); break;
            case kCGImageAlphaPremultipliedFirst: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaPremultipliedFirst\n"); break;
            case kCGImageAlphaLast: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaLast\n"); break;
            case kCGImageAlphaFirst: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaFirst\n"); break;
            case kCGImageAlphaNoneSkipLast: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaNoneSkipLast\n"); break;
            case kCGImageAlphaNoneSkipFirst: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaNoneSkipFirst\n"); break;
            case kCGImageAlphaOnly: fprintf(stderr, "    CGImageGetAlphaInfo(): kCGImageAlphaOnly\n"); break;
            default: fprintf(stderr, "    CGImageGetAlphaInfo(): unknown\n");
        }
        CGBitmapInfo bitmap = CGImageGetBitmapInfo(image) & kCGBitmapByteOrderMask;
        switch (bitmap)
        {
            case kCGBitmapByteOrderDefault: fprintf(stderr, "    CGImageGetBitmapInfo(): kCGBitmapByteOrderDefault\n"); break;
            case kCGBitmapByteOrder16Little: fprintf(stderr, "    CGImageGetBitmapInfo(): kCGBitmapByteOrder16Little\n"); break;
            case kCGBitmapByteOrder32Little: fprintf(stderr, "    CGImageGetBitmapInfo(): kCGBitmapByteOrder32Little\n"); break;
            case kCGBitmapByteOrder16Big: fprintf(stderr, "    CGImageGetBitmapInfo(): kCGBitmapByteOrder16Big\n"); break;
            case kCGBitmapByteOrder32Big: fprintf(stderr, "    CGImageGetBitmapInfo(): kCGBitmapByteOrder32Big\n"); break;
            default: fprintf(stderr, "    CGImageGetBitmapInfo(): unknown\n");
        }
    }
}

static inline void PostGlassMouseEvent(CGPoint location, UInt32 buttons, BOOL buttonPressed)
{
    // for each one bit in buttons, post a new mouse {press/release} event
    if (buttons != 0) {
        for (UInt32 index = 0; buttons != 0; index++, buttons >>= 1) {
            if (buttons & 1) {
                CGEventType type;
                switch (index) {
                    case 0:
                        type = buttonPressed ? kCGEventLeftMouseDown : kCGEventLeftMouseUp;
                        break;
                    case 1:
                        type = buttonPressed ? kCGEventRightMouseDown : kCGEventRightMouseUp;
                        break;
                    default:
                        type = buttonPressed ? kCGEventOtherMouseDown : kCGEventOtherMouseUp;
                        break;
                }

                CGEventRef newEvent = CGEventCreateMouseEvent(NULL, type, location, (CGMouseButton)index);
                CGEventPost(kCGHIDEventTap, newEvent);
                CFRelease(newEvent);
            }
        }
    }
}

static inline void PostGlassKeyEvent(jint code, BOOL keyPressed)
{
    unsigned short macCode;
    if (GetMacKey(code, &macCode)) {
        // Using CGEvent API proved to be problematic - events for some keys were missing sometimes.
        // So we use the A11Y API instead - just as we do in AWT. It works fine in all cases.
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        AXUIElementRef elem = AXUIElementCreateSystemWide();
        AXUIElementPostKeyboardEvent(elem, (CGCharCode)0, macCode, keyPressed);
#pragma clang diagnostic pop
        CFRelease(elem);
    }
}

@interface GlassRobot : NSObject
{
    UInt32 mouseButtons;
}

- (void)mouseMove:(NSPoint)p;
- (void)mousePress:(UInt32)buttons;
- (void)mouseRelease:(UInt32)buttons;
- (CGPoint)getMousePosFlipped;
@end

@implementation GlassRobot

- (id)init
{
    self = [super init];
    if (self != nil)
    {
        self->mouseButtons = kMouseButtonNone;
    }
    return self;
}


- (void)mouseMove:(NSPoint)p
{
    CGPoint location = NSPointToCGPoint(p);
    UInt32 buttons = self->mouseButtons;
    CGEventType type=kCGEventMouseMoved;
    UInt32 index=0;
    for (; buttons != 0; index++, buttons >>= 1)
    {
        if (buttons & 1)
        {
            switch (index)
            {
                case 0:
                    type = kCGEventLeftMouseDragged;
                    break;
                case 1:
                    type = kCGEventRightMouseDragged;
                    break;
                default:
                    type = kCGEventOtherMouseDragged;
                    break;
            }
        }
    }
    CGEventRef newEvent = CGEventCreateMouseEvent(NULL, type, location, (CGMouseButton)index);
    CGEventPost(kCGHIDEventTap, newEvent);
    CGWarpMouseCursorPosition(location);
    CFRelease(newEvent);

}

- (CGPoint)getMousePosFlipped
{
    CGPoint where = NSPointToCGPoint([NSEvent mouseLocation]);
    NSScreen * screen = [[NSScreen screens] objectAtIndex: 0];
    NSRect screenFrame = screen.frame;
    where.y = screenFrame.size.height - where.y;
    return where;
}

- (void)mousePress:(UInt32)buttons
{
    //Add new pressed buttons
    self->mouseButtons = self->mouseButtons | buttons;
    PostGlassMouseEvent([self getMousePosFlipped], buttons, YES);
}

- (void)mouseRelease:(UInt32)buttons
{
    PostGlassMouseEvent([self getMousePosFlipped], buttons, NO);
    //reset buttons
    self->mouseButtons = self->mouseButtons & (~buttons);
}

@end

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _init
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacRobot__1init
(JNIEnv *env, jobject jrobot)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1init");

    return ptr_to_jlong([[GlassRobot alloc] init]);
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1destroy
(JNIEnv *env, jobject jThis, jlong ptr)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1destroy");

    [(GlassRobot*)jlong_to_ptr(ptr) release];
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _keyPress
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1keyPress
(JNIEnv *env, jobject jrobot, jint code)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1keyPress");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        PostGlassKeyEvent(code, YES);
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _keyRelease
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1keyRelease
(JNIEnv *env, jobject jrobot, jint code)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1keyRelease");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        PostGlassKeyEvent(code, NO);
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _mouseMove
 * Signature: (JFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1mouseMove
(JNIEnv *env, jobject jrobot, jlong ptr, jfloat x, jfloat y)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1mouseMove");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot * robot = (GlassRobot*)jlong_to_ptr(ptr);
        [robot mouseMove:NSMakePoint(x, y)];
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _getMouseX
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_glass_ui_mac_MacRobot__1getMouseX
(JNIEnv *env, jobject jrobot, jlong ptr)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1getMouseX");

    jfloat x = 0.0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot * robot = (GlassRobot*)jlong_to_ptr(ptr);
        x = (jfloat)[robot getMousePosFlipped].x;
    }
    GLASS_POOL_EXIT;

    return x;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _getMouseY
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_glass_ui_mac_MacRobot__1getMouseY
(JNIEnv *env, jobject jrobot, jlong ptr)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1getMouseY");

    jfloat y = 0.0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot * robot = (GlassRobot*)jlong_to_ptr(ptr);
        y = (jfloat)[robot getMousePosFlipped].y;
    }
    GLASS_POOL_EXIT;

    return y;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _mousePress
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1mousePress
(JNIEnv *env, jobject jrobot, jlong ptr, jint buttons)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1mousePress");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot * robot = (GlassRobot*)jlong_to_ptr(ptr);
        [robot mousePress:(UInt32)buttons];
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _mouseRelease
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1mouseRelease
(JNIEnv *env, jobject jrobot, jlong ptr, jint buttons)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1mouseRelease");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot * robot = (GlassRobot*)jlong_to_ptr(ptr);
        [robot mouseRelease:(UInt32)buttons];
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _mouseWheel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacRobot__1mouseWheel
(JNIEnv *env, jobject jrobot, jint wheelAmt)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1mouseWheel");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        CGEventRef newEvent = CGEventCreateScrollWheelEvent(NULL, kCGScrollEventUnitLine, 1, (int32_t)wheelAmt);
        CGEventPost(kCGHIDEventTap, newEvent);
        CFRelease(newEvent);
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _getPixelColor
 * Signature: (DD)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacRobot__1getPixelColor
(JNIEnv *env, jobject jrobot, jdouble x, jdouble y)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1getPixelColor");

    jint color = 0;
    CGColorRef origColor = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        CGRect bounds = CGRectMake((CGFloat)x, (CGFloat)y, 1.0f, 1.0f);
        CGImageRef screenImage = CGWindowListCreateImage(bounds, kCGWindowListOptionOnScreenOnly, kCGNullWindowID, kCGWindowImageDefault);
        if (screenImage != NULL)
        {
            //DumpImage(screenImage);
            CGDataProviderRef provider = CGImageGetDataProvider(screenImage);
            if (provider != NULL)
            {
                CFDataRef data = CGDataProviderCopyData(provider);
                if (data != NULL)
                {
                    jint *pixels = (jint*)CFDataGetBytePtr(data);
                    if (pixels != NULL)
                    {
                        color = *pixels;
                        CGFloat components[4];
                        components[0] = (CGFloat)((color & 0x00FF0000) >> 16) / 255.0;
                        components[1] = (CGFloat)((color & 0x0000FF00) >> 8) / 255.0;
                        components[2] = (CGFloat)((color & 0x000000FF)) / 255.0;
                        components[3] = (CGFloat)((color & 0xFF000000) >> 24) / 255.0;
                        origColor = CGColorCreate(CGImageGetColorSpace(screenImage), components);
                    }
                }
                CFRelease(data);
            }
            CGImageRelease(screenImage);
        }
    }

    if (origColor != NULL) {
        CGColorSpaceRef sRGBSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
        CGColorRef correctedColor = CGColorCreateCopyByMatchingToColorSpace(sRGBSpace, kCGRenderingIntentAbsoluteColorimetric, origColor, NULL);
        const CGFloat* components = CGColorGetComponents(correctedColor);
        color  = ((jint)(round(components[3] * 255)) & 0xFF) << 24;
        color |= ((jint)(round(components[0] * 255)) & 0xFF) << 16;
        color |= ((jint)(round(components[1] * 255)) & 0xFF) << 8;
        color |= ((jint)(round(components[2] * 255)) & 0xFF);
        CGColorSpaceRelease(sRGBSpace);
        CGColorRelease(correctedColor);
        CGColorRelease(origColor);
    }
    GLASS_POOL_EXIT;

    return color;
}

/*
 * Class:     com_sun_glass_ui_mac_MacRobot
 * Method:    _getScreenCapture
 * Signature: (IIIIZ)Lcom/sun/glass/ui/Pixels;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacRobot__1getScreenCapture
(JNIEnv *env, jobject jrobot, jint x, jint y, jint width, jint height, jboolean scaleToFit)
{
    LOG("Java_com_sun_glass_ui_mac_MacRobot__1getScreenCapture");

    if (width <= 0 || height <= 0) {
        return NULL;
    }
    if (width >= INT_MAX / height) {
        return NULL;
    }

    jobject pixels = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        CGRect bounds = CGRectMake((CGFloat)x, (CGFloat)y, (CGFloat)width, (CGFloat)height);
        CGImageRef screenImage = CGWindowListCreateImage(bounds, kCGWindowListOptionOnScreenOnly, kCGNullWindowID, kCGWindowImageDefault);
        if (screenImage != NULL)
        {
            jint pixWidth, pixHeight;

            if (!scaleToFit) {
                pixWidth = (jint)CGImageGetWidth(screenImage);
                pixHeight = (jint)CGImageGetHeight(screenImage);
                if (pixWidth <= 0 || pixHeight <= 0) {
                    return NULL;
                }
                if (pixWidth >= INT_MAX / pixHeight) {
                    return NULL;
                }
            } else {
                pixWidth = width;
                pixHeight = height;
            }
            jintArray pixelArray = (*env)->NewIntArray(env, (jsize)pixWidth * pixHeight);
            if (pixelArray)
            {
                jint *javaPixels = (jint*)(*env)->GetIntArrayElements(env, pixelArray, 0);
                if (javaPixels != NULL)
                {
                    // create a graphics context around the Java int array
                    CGColorSpaceRef picColorSpace = CGColorSpaceCreateWithName(
                            kCGColorSpaceSRGB);
                    CGContextRef jPicContextRef = CGBitmapContextCreate(
                            javaPixels,
                            pixWidth, pixHeight,
                            8, pixWidth * sizeof(jint),
                            picColorSpace,
                            kCGBitmapByteOrder32Host |
                            kCGImageAlphaPremultipliedFirst);

                    CGColorSpaceRelease(picColorSpace);

                    // flip, scale, and color correct the screen image into the Java pixels
                    CGRect zeroBounds = { { 0, 0 }, { pixWidth, pixHeight } };
                    CGContextDrawImage(jPicContextRef, zeroBounds, screenImage);
                    CGContextFlush(jPicContextRef);

                    // cleanup
                    CGContextRelease(jPicContextRef);
                    (*env)->ReleaseIntArrayElements(env, pixelArray, javaPixels, 0);

                    jclass applicationClass =
                        [GlassHelper ClassForName:"com.sun.glass.ui.Application" withEnv:env];
                    if (!applicationClass) return NULL;

                    jfloat scale = (*env)->CallStaticFloatMethod(env,
                            applicationClass,
                            javaIDs.Application.getScaleFactor, x, y, width, height);
                    if ((*env)->ExceptionCheck(env)) return NULL;

                    // create Pixels
                    pixels = (*env)->CallStaticObjectMethod(env, applicationClass,
                                                            javaIDs.Application.createPixels,
                                                            pixWidth, pixHeight,
                                                            pixelArray, scale, scale);
                    if ((*env)->ExceptionCheck(env)) return NULL;
                }
            }

            CGImageRelease(screenImage);
        }
    }
    GLASS_POOL_EXIT;

    return pixels;
}

