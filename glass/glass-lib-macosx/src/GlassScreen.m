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
#import "com_sun_glass_ui_Screen.h"
#import "com_sun_glass_ui_mac_MacScreen.h"

#import "GlassMacros.h"
#import "GlassScreen.h"
#import "GlassTimer.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define MAX_DISPLAY_COUNT 1024

CGFloat GetScreenScaleFactor(NSScreen *screen)
{
    if ([screen respondsToSelector:@selector(backingScaleFactor)]) {
        return [screen backingScaleFactor];
    } else {
        return [screen userSpaceScaleFactor];
    }
}

void SetJavaScreen(NSScreen *screen, JNIEnv *env, jobject jscreen)
{
    if (screen != nil)
    {
        (*env)->SetLongField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "ptr", "J"), ptr_to_jlong([screen retain]));
        
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "depth", "I"), (jint)NSBitsPerPixelFromDepth([screen depth]));
        
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "x", "I"), (jint)[screen frame].origin.x);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "y", "I"), (jint)[screen frame].origin.y);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "width", "I"), (jint)[screen frame].size.width);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "height", "I"), (jint)[screen frame].size.height);
        
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "visibleX", "I"), (jint)[screen visibleFrame].origin.x);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "visibleY", "I"),
                (jint)([screen frame].size.height - [screen visibleFrame].size.height - [screen visibleFrame].origin.y));
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "visibleWidth", "I"), (jint)[screen visibleFrame].size.width);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "visibleHeight", "I"), (jint)[screen visibleFrame].size.height);
        
        CGFloat scale = GetScreenScaleFactor(screen);
        (*env)->SetFloatField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "scale", "F"), (jfloat)scale);
        
        NSValue *resolutionValue = [[screen deviceDescription] valueForKey:NSDeviceResolution];
        NSSize resolution = [resolutionValue sizeValue];
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "resolutionX", "I"), (jint)resolution.width);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, jScreenClass, "resolutionY", "I"), (jint)resolution.height);
    }
}

void GlassScreenDidChangeScreenParameters(JNIEnv *env)
{
    if (jScreenNotifySettingsChanged == NULL) 
    {
        jScreenNotifySettingsChanged = (*env)->GetStaticMethodID(env, jScreenClass, "notifySettingsChanged", "()V");
    }
    (*env)->CallStaticVoidMethod(env, jScreenClass, jScreenNotifySettingsChanged);
}

static inline jobject createJavaScreen(JNIEnv *env)
{
    jobject jscreen = NULL;
    
    jscreen = (*env)->NewObject(env, jScreenClass, (*env)->GetMethodID(env, jScreenClass, "<init>", "()V"));
    {
        SetJavaScreen([NSScreen deepestScreen], env, jscreen);
    }
    
    return jscreen;
}

@implementation NSScreen (FullscreenAdditions)

- (CGDirectDisplayID)enterFullscreenAndHideCursor:(BOOL)hide
{
    CGDirectDisplayID displayID = 0;
    
    CGDisplayCount displayCount = 0;
    CGDirectDisplayID activeDisplays[MAX_DISPLAY_COUNT];
    CGDisplayErr err = CGGetActiveDisplayList(MAX_DISPLAY_COUNT, activeDisplays, &displayCount);
    if (err != kCGErrorSuccess)
    {
        NSLog(@"CGGetActiveDisplayList returned error: %d", err);
    }
    else
    {
        NSRect nsrect = [self frame];
        
        for (CGDisplayCount i=0; i<displayCount; i++)
        {
            CGRect cgrect = CGDisplayBounds(activeDisplays[i]);
            if ((nsrect.origin.x == cgrect.origin.x) && (nsrect.origin.y == cgrect.origin.y)
                && (nsrect.size.width == cgrect.size.width) && (nsrect.size.height == cgrect.size.height))
            {
                displayID = activeDisplays[i];
                break;
            }
        }
        
#if 0
        err = CGDisplayCapture(displayID);
#endif
        if (displayID == kCGDirectMainDisplay)
        {
            [NSMenu setMenuBarVisible:NO];
        }
        
        if (err != kCGErrorSuccess)
        {
            NSLog(@"CGDisplayCapture returned error: %d", err);
            displayID = 0;
        }
        else
        {
            if (hide == YES)
            {
                CGDisplayHideCursor(displayID);
            }
        }
    }
    
    return displayID;
}

- (void)exitFullscreen:(CGDirectDisplayID)displayID
{
    if (displayID != 0)
    {
        if (displayID == kCGDirectMainDisplay)
        {
            [NSMenu setMenuBarVisible:YES];
        }
#if 0
        CGDisplayRelease(displayID);
#endif
        CGDisplayShowCursor(displayID);
    }
}

@end

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacScreen__1initIDs
(JNIEnv *env, jclass jClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1initIDs");
    
    if (jScreenClass == NULL)
    {
        jScreenClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/Screen"));
    }
    
    initJavaIDsList(env);
    
    // TODO: move from Java_com_sun_glass_ui_mac_MacScreen__1initIDs to Java_com_sun_glass_ui_mac_MacTimer__1initIDs
    if (GlassDisplayLink == NULL)
    {
        CVReturn err = CVDisplayLinkCreateWithActiveCGDisplays(&GlassDisplayLink);
        if (err != kCVReturnSuccess)
        {
            NSLog(@"CVDisplayLinkCreateWithActiveCGDisplays error: %d", err);
        }
        err = CVDisplayLinkSetCurrentCGDisplay(GlassDisplayLink, kCGDirectMainDisplay);
        if (err != kCVReturnSuccess)
        {
            NSLog(@"CVDisplayLinkSetCurrentCGDisplay error: %d", err);
        }
        /*
         * set a null callback and start the link to prep for GlassTimer initialization
         */
        err = CVDisplayLinkSetOutputCallback(GlassDisplayLink, &CVOutputCallback, NULL);
        if (err != kCVReturnSuccess)
        {
            NSLog(@"CVDisplayLinkSetOutputCallback error: %d", err);
        }
        err = CVDisplayLinkStart(GlassDisplayLink);
        if (err != kCVReturnSuccess)
        {
            NSLog(@"CVDisplayLinkStart error: %d", err);
        }
    }
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getDeepestScreen
 * Signature: (Lcom/sun/glass/ui/Screen;)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacScreen__1getDeepestScreen
(JNIEnv *env, jclass jscreenClass, jobject jscreen)
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1getDeepestScreen");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        SetJavaScreen([NSScreen deepestScreen], env, jscreen);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return jscreen;
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getMainScreen
 * Signature: (Lcom/sun/glass/ui/Screen;)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacScreen__1getMainScreen
(JNIEnv *env, jclass jscreenClass, jobject jscreen)
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1getMainScreen");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSScreen * screen = [[NSScreen screens] objectAtIndex: 0];
        SetJavaScreen(screen, env, jscreen);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return jscreen;
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getScreenForLocation
 * Signature: (Lcom/sun/glass/ui/Screen;II)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacScreen__1getScreenForLocation
(JNIEnv *env, jclass jscreenClass, jobject jscreen, jint x, jint y)
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1getScreenForLocation");
    
    // how do we do this?
    
    return jscreen;
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getScreenForPtr
 * Signature: (Lcom/sun/glass/ui/Screen;J)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacScreen__1getScreenForPtr
(JNIEnv *env, jclass jscreenClass, jobject jscreen, jlong screenPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1getScreenForPtr");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        SetJavaScreen((NSScreen *)jlong_to_ptr(screenPtr), env, jscreen);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return jscreen;
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getScreens
 * Signature: (Ljava/util/ArrayList;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacScreen__1getScreens
(JNIEnv *env, jclass jscreenClass, jobject jscreens)
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1getScreens");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSArray *screens = [NSScreen screens];
        for (NSUInteger i=0; i<[screens count]; i++)
        {
            jobject jscreen = createJavaScreen(env);
            {
                SetJavaScreen([screens objectAtIndex:i], env, jscreen);
                
                (*env)->CallBooleanMethod(env, jscreens, javaIDs.List.add, jscreen);
            }
            (*env)->DeleteLocalRef(env, jscreen);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return jscreens;
}

/*
 * Class:     com_sun_glass_ui_mac_MacScreen
 * Method:    _getVideoRefreshPeriod
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL 
Java_com_sun_glass_ui_mac_MacScreen__1getVideoRefreshPeriod(JNIEnv *env, jclass screenClass) 
{
    LOG("Java_com_sun_glass_ui_mac_MacScreen__1getVideoRefreshPeriod");
    
    if (GlassDisplayLink != NULL)
    {
        double outRefresh = CVDisplayLinkGetActualOutputVideoRefreshPeriod(GlassDisplayLink);
        LOG("CVDisplayLinkGetActualOutputVideoRefreshPeriod: %f", outRefresh);
        return (outRefresh * 1000.0); // to millis
    } 
    else
    {
        return 0.0;
    }
}

