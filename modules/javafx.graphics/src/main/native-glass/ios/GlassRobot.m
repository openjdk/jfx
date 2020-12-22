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

#import "common.h"
#import "com_sun_glass_ui_ios_IOSRobot.h"

#import <QuartzCore/QuartzCore.h>

#import "GlassMacros.h"
#import "GlassWindow.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) NSLog(MSG, ## __VA_ARGS__);
#endif

#ifdef GLASS_ROBOT_ENABLED

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

//
// UITouch (Synthesize)
//
// Category to allow creation and modification of UITouch objects.
//
@interface UITouch (Synthesize)

- (id)initInView:(UIView *)view:(CGPoint)location;
- (void)setPhase:(UITouchPhase)phase;
- (void)setLocationInWindow:(CGPoint)location;

@end

@implementation UITouch (Synthesize)

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 60000
    // ivars declarations removed in 6.0
    NSTimeInterval  _timestamp;
    UITouchPhase    _phase;
    UITouchPhase    _savedPhase;
    NSUInteger      _tapCount;

    UIWindow        *_window;
    UIView          *_view;
    UIView          *_gestureView;
    UIView          *_warpedIntoView;
    NSMutableArray  *_gestureRecognizers;
    NSMutableArray  *_forwardingRecord;

    CGPoint         _locationInWindow;
    CGPoint         _previousLocationInWindow;
    UInt8           _pathIndex;
    UInt8           _pathIdentity;
    float           _pathMajorRadius;
    struct {
        unsigned int _firstTouchForView:1;
        unsigned int _isTap:1;
        unsigned int _isDelayed:1;
        unsigned int _sentTouchesEnded:1;
        unsigned int _abandonForwardingRecord:1;
    } _touchFlags;
#endif //__IPHONE_OS_VERSION_MAX_ALLOWED >= 60000

//
// initInView:phase:
//
// Creats a UITouch, centered on the specified view, in the view's window.
// Sets the phase as specified.
//
- (id)initInView:(UIView *)view:(CGPoint)location
{
    self = [super init];
    if (self != nil)
    {
        CGRect frameInWindow;
        if ([view isKindOfClass:[UIWindow class]])
        {
            frameInWindow = view.frame;
        }
        else
        {
            frameInWindow =
            [view.window convertRect:view.frame fromView:view.superview];
        }

        _tapCount = 1;
        _locationInWindow = location;
        _previousLocationInWindow = _locationInWindow;

        UIView *target = [view.window hitTest:_locationInWindow withEvent:nil];

        _window = [view.window retain];
        _view = [target retain];
        _phase = UITouchPhaseBegan;
        _touchFlags._firstTouchForView = 1;
        _touchFlags._isTap = 1;
        _timestamp = [NSDate timeIntervalSinceReferenceDate];
    }
    return self;
}

//
// setPhase:
//
// Setter to allow access to the _phase member.
//
- (void)setPhase:(UITouchPhase)phase
{
    _phase = phase;
    _timestamp = [NSDate timeIntervalSinceReferenceDate];
}

//
// setLocationInWindow:
//
// Setter to allow access to the _locationInWindow member.
//
- (void)setLocationInWindow:(CGPoint)location
{
    _previousLocationInWindow = _locationInWindow;
    _locationInWindow = location;
    _timestamp = [NSDate timeIntervalSinceReferenceDate];
}

@end

//
// GSEvent is an undeclared object. We don't need to use it ourselves but some
// Apple APIs (UIScrollView in particular) require the x and y fields to be present.
//
@interface GSEventProxy : NSObject
{
@public
    unsigned int flags;
    unsigned int type;
    unsigned int ignored1;
    float x1;
    float y1;
    float x2;
    float y2;
    unsigned int ignored2[10];
    unsigned int ignored3[7];
    float sizeX;
    float sizeY;
    float x3;
    float y3;
    unsigned int ignored4[3];
}
@end
@implementation GSEventProxy
@end

//
// PublicEvent
//
// A dummy class used to gain access to UIEvent's private member variables.
// If UIEvent changes at all, this will break.
//
@interface PublicEvent : NSObject
{
@public
    GSEventProxy           *_event;
    NSTimeInterval          _timestamp;
    NSMutableSet           *_touches;
    CFMutableDictionaryRef  _keyedTouches;
}
@end

@implementation PublicEvent
@end

@interface UIEvent (Creation)

- (id)_initWithEvent:(GSEventProxy *)fp8 touches:(id)fp12;

@end

//
// UIEvent (Synthesize)
//
// A category to allow creation of a touch event.
//
@interface UIEvent (Synthesize)

- (id)initWithTouch:(UITouch *)touch;

@end

@implementation UIEvent (Synthesize)

- (id)initWithTouch:(UITouch *)touch
{
    CGPoint location = [touch locationInView:touch.window];
    GSEventProxy *gsEventProxy = [[GSEventProxy alloc] init];
    gsEventProxy->x1 = location.x;
    gsEventProxy->y1 = location.y;
    gsEventProxy->x2 = location.x;
    gsEventProxy->y2 = location.y;
    gsEventProxy->x3 = location.x;
    gsEventProxy->y3 = location.y;
    gsEventProxy->sizeX = 1.0;
    gsEventProxy->sizeY = 1.0;
    gsEventProxy->flags = ([touch phase] == UITouchPhaseEnded) ? 0x1010180 : 0x3010180;
    gsEventProxy->type = 3001;

    Class touchesEventClass = NSClassFromString(@"UITouchesEvent");
    if (touchesEventClass && ![[self class] isEqual:touchesEventClass])
    {
        [self release];
        self = [touchesEventClass alloc];
    }

    self = [self _initWithEvent:gsEventProxy touches:[NSSet setWithObject:touch]];
    if (self != nil)
    {
    }
    return self;
}

@end


@interface GlassRobot : NSObject
{
    CGPoint touchLocation;
    UITouch *touch;
    CGRect screenshotBounds;
    jint *screenPixels;
}

- (void)mouseMove:(CGPoint)p;
- (void)mousePress;
- (void)mouseRelease;
- (CGPoint)getTouchLocation;
- (void)captureScreen;
- (void)setScreenshotBounds:(CGRect)bounds;
- (jint *)getScreenPixels;
- (void)keyPress:(NSString *)chr;

@end

@implementation GlassRobot

- (void)mouseMove:(CGPoint)p
{
    touchLocation = p;
    // optionaly also generate touchesMoved event
}

- (void)mousePress
{
     touch = [[UITouch alloc] initInView:[GlassWindow getMainWindowHost] :touchLocation];
    UIEvent *eventDown = [[UIEvent alloc] initWithTouch:touch];

    [touch.view touchesBegan:[eventDown allTouches] withEvent:eventDown];

    [eventDown release];
}

- (void)mouseRelease
{
    [touch setLocationInWindow:touchLocation];
    UIEvent *eventUp = [[UIEvent alloc] initWithTouch:touch];

    [touch setPhase:UITouchPhaseEnded];
    [touch.view touchesEnded:[eventUp allTouches] withEvent:eventUp];

    [eventUp release];
    [touch release];
}

- (CGPoint)getTouchLocation {
    return touchLocation;
}

- (void)setScreenshotBounds:(CGRect)bounds {
    screenshotBounds = bounds;
}

- (void)captureScreen {
    GLint backingWidth, backingHeight;

    // Get the size of the backing CAEAGLLayer
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &backingWidth);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &backingHeight);

    NSInteger x = 0, y = 0, width = backingWidth, height = backingHeight;
    NSInteger dataLength = width * height * 4;
    GLubyte *data = (GLubyte*)malloc(dataLength * sizeof(GLubyte));

    // Read pixel data from the framebuffer
    glPixelStorei(GL_PACK_ALIGNMENT, 4);
    glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);

    // Create a CGImage with the pixel data
    // If your OpenGL ES content is opaque, use kCGImageAlphaNoneSkipLast to ignore the alpha channel
    // otherwise, use kCGImageAlphaPremultipliedLast
    CGDataProviderRef ref = CGDataProviderCreateWithData(NULL, data, dataLength, NULL);
    CGColorSpaceRef colorspace = CGColorSpaceCreateDeviceRGB();
    CGImageRef iref = CGImageCreate(width, height, 8, 32, width * 4, colorspace, kCGBitmapByteOrder32Big | kCGImageAlphaPremultipliedLast,
                                    ref, NULL, true, kCGRenderingIntentDefault);

    // OpenGL ES measures data in PIXELS
    // Create a graphics context with the target size measured in POINTS
    // On iOS 4 and later, use UIGraphicsBeginImageContextWithOptions to take the scale into consideration
    // Set the scale parameter to your OpenGL ES view's contentScaleFactor
    // so that you get a high-resolution snapshot when its value is greater than 1.0
    CGFloat scale = [GlassWindow getMainWindowHost].contentScaleFactor;
    NSInteger widthInPoints = width / scale;
    NSInteger heightInPoints = height / scale;
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(widthInPoints, heightInPoints), NO, scale);

    CGContextRef cgcontext = UIGraphicsGetCurrentContext();

    // UIKit coordinate system is upside down to GL/Quartz coordinate system
    // Flip the CGImage by rendering it to the flipped bitmap context
    // The size of the destination area is measured in POINTS
    CGContextSetBlendMode(cgcontext, kCGBlendModeCopy);
    CGContextDrawImage(cgcontext, CGRectMake(0.0, 0.0, widthInPoints, heightInPoints), iref);

    // Retrieve the UIImage from the current context
    UIImage *screenshot = UIGraphicsGetImageFromCurrentImageContext();

    UIGraphicsEndImageContext();

    // Clean up
    free(data);
    CFRelease(ref);
    CFRelease(colorspace);
    CGImageRelease(iref);

    if (screenshot != nil) {
        CGImageRef screenImage = CGImageCreateWithImageInRect([screenshot CGImage], screenshotBounds);
        if (screenImage != NULL)
        {
            //DumpImage(screenImage);
            CGDataProviderRef provider = CGImageGetDataProvider(screenImage);
            if (provider != NULL)
            {
                CFDataRef cfdata = CGDataProviderCopyData(provider);
                if (data != NULL)
                {
                    screenPixels = (jint*)CFDataGetBytePtr(cfdata);
                }
                //CFRelease(cfdata);
            }
            CGImageRelease(screenImage);
        }
    }
}

- (jint *)getScreenPixels {
    return screenPixels;
}

- (void)keyPress:(NSString *)chr {
    UIView *subview = [[[GlassWindow getMainWindowHost] subviews] objectAtIndex:0];
    if (subview != NULL) {
        subview = [[subview subviews] objectAtIndex:0];
        if (subview != NULL) {
            NSArray *views = [subview subviews];
            for (UIView *v in views) {
                if ([v isMemberOfClass:[UITextField class]]) {
                    UITextField *tf = (UITextField *) v;
                    NSString *text = tf.text;
                    tf.text = [text stringByAppendingString:chr];
                    [[NSNotificationCenter defaultCenter] postNotificationName:UITextFieldTextDidChangeNotification object:tf];
                } else if ([v isMemberOfClass:[UITextView class]]) {
                    UITextView *tv = (UITextView *) v;
                    NSString *text = tv.text;
                    tv.text = [text stringByAppendingString:chr];
                    [[NSNotificationCenter defaultCenter] postNotificationName:UITextViewTextDidChangeNotification object:tv];
                }
            }
        }
    }
}

@end

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _init
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosRobot__1init
(JNIEnv *env, jobject jrobot)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1init");

    return ptr_to_jlong([GlassRobot new]);
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1destroy
(JNIEnv *env, jobject jThis, jlong ptr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1destroy");

    [(GlassRobot*)jlong_to_ptr(ptr) release];
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _keyPress
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1keyPress
(JNIEnv *env, jobject jrobot, jlong ptr, jint code)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1keyPress");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        const unichar c = (char) code;
        NSString *chr = [[NSString stringWithCharacters:&c length:1] lowercaseString];
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        [r performSelectorOnMainThread:@selector(keyPress:) withObject:chr waitUntilDone:YES];

    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _keyRelease
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1keyRelease
(JNIEnv *env, jobject jrobot, jlong ptr, jint code)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1keyRelease NOT IMPLEMENTED!");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    /*GLASS_POOL_ENTER
    {
        unsigned short macCode;
        if (GetMacKey(code, &macCode)) {
            CGEventRef newEvent = CGEventCreateKeyboardEvent(NULL, macCode, false);
            CGEventPost(kCGHIDEventTap, newEvent);
            CFRelease(newEvent);
        }
    }
    GLASS_POOL_EXIT;*/
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _mouseMove
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1mouseMove
(JNIEnv *env, jobject jrobot, jlong ptr, jint x, jint y)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1mouseMove");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        [r mouseMove:CGPointMake((float)x, (float)y)];
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _getMouseX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosRobot__1getMouseX
(JNIEnv *env, jobject jrobot, jlong ptr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1getMouseX");

    jint x = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        x = (jint) [r getTouchLocation].x;
    }
    GLASS_POOL_EXIT;

    return x;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _getMouseY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosRobot__1getMouseY
(JNIEnv *env, jobject jrobot, jlong ptr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1getMouseY");

    jint y = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        y = (jint) [r getTouchLocation].y;
    }
    GLASS_POOL_EXIT;

    return y;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _mousePress
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1mousePress
(JNIEnv *env, jobject jrobot, jlong ptr, jint buttons)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1mousePress");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        [r performSelectorOnMainThread:@selector(mousePress) withObject:nil waitUntilDone:YES];
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _mouseRelease
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1mouseRelease
(JNIEnv *env, jobject jrobot, jlong ptr, jint buttons)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1mouseRelease");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        [r performSelectorOnMainThread:@selector(mouseRelease) withObject:nil waitUntilDone:YES];
    }
    GLASS_POOL_EXIT;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _mouseWheel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1mouseWheel
(JNIEnv *env, jobject jrobot, jlong ptr, jint wheelAmt)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1mouseWheel N/A on iOS");
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _getPixelColor
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosRobot__1getPixelColor
(JNIEnv *env, jobject jrobot, jlong ptr, jint x, jint y)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1getPixelColor");

    jint *color;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        CGRect bounds = CGRectMake((CGFloat)x, (CGFloat)y, 1.0f, 1.0f);
        GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
        [r setScreenshotBounds:bounds];
        [r performSelectorOnMainThread:@selector(captureScreen) withObject:nil waitUntilDone:YES];
        color = [r getScreenPixels];
    }
    GLASS_POOL_EXIT;

    return *color;
}

/*
 * Class:     com_sun_glass_ui_ios_IosRobot
 * Method:    _getScreenCapture
 * Signature: (JIIII[I;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosRobot__1getScreenCapture
(JNIEnv *env, jobject jrobot, jlong ptr, jint x, jint y, jint width, jint height, jintArray pixelArray)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosRobot__1getScreenCapture");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER
    {
        jint *javaPixels = (jint*)(*env)->GetIntArrayElements(env, pixelArray, 0);
        if (javaPixels != NULL)
        {
            CGRect bounds = CGRectMake((CGFloat)x, (CGFloat)y, (CGFloat)width, (CGFloat)height);
            GlassRobot* r = (GlassRobot*)jlong_to_ptr(ptr);
            [r setScreenshotBounds:bounds];
            [r performSelectorOnMainThread:@selector(captureScreen) withObject:nil waitUntilDone:YES];
            memcpy(javaPixels, [r getScreenPixels], width*height);
        }
        (*env)->ReleaseIntArrayElements(env, pixelArray, javaPixels, 0);
    }
    GLASS_POOL_EXIT;
}

#endif //GLASS_ROBOT_ENABLED
