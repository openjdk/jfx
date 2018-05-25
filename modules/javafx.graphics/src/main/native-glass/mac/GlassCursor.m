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
#import "com_sun_glass_ui_Cursor.h"
#import "com_sun_glass_ui_mac_MacCursor.h"

#import <Cocoa/Cocoa.h>

#import "GlassMacros.h"
#import "GlassHelper.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define CURSOR_BEST_SIZE 32

static NSArray *cursorCache;

#pragma mark --- Java NSCursor

@interface NSCursor (Java)

+ (NSCursor*)performJavaSelector:(SEL)aSelector;

@end

@implementation NSCursor (Java)

+ (NSCursor*)performJavaSelector:(SEL)aSelector
{
    NSCursor *cursor = nil;
    if ([GlassHelper InvokeSelectorIfAvailable:aSelector forClass:[NSCursor class] withArgument:NULL withReturnValue:(void**)&cursor] == NO)
    {
        cursor = [NSCursor arrowCursor];
    }
    return cursor;
}

@end

#pragma mark --- com_sun_glass_ui_mac_MacCursor

/*
 * Class:     com_sun_glass_ui_mac_MacCursor
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacCursor__1initIDs
(JNIEnv *env, jclass jCursorClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacCursor__1initIDs");

    if (jSizeInit == NULL)
    {
        jclass cls = [GlassHelper ClassForName:"com.sun.glass.ui.Size" withEnv:env];
        if (!cls) {
            return;
        }
        jSizeInit = (*env)->GetMethodID(env, cls, "<init>", "(II)V");
    }

    NSString *base = @"/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/HIServices.framework/Versions/A/Resources/cursors";
    NSString *nwse = @"resizenorthwestsoutheast";
    NSString *nesw = @"resizenortheastsouthwest";
    NSString *nwsePath = [base stringByAppendingPathComponent:nwse];
    NSString *neswPath = [base stringByAppendingPathComponent:nesw];
    NSString *nwseFile = [nwsePath stringByAppendingPathComponent:@"cursor.pdf"];
    NSString *neswFile = [neswPath stringByAppendingPathComponent:@"cursor.pdf"];
    NSString *nwseInfo = [nwsePath stringByAppendingPathComponent:@"info.plist"];
    NSString *neswInfo = [neswPath stringByAppendingPathComponent:@"info.plist"];
    NSImage  *nwseImage = [[NSImage alloc] initByReferencingFile:nwseFile];
    NSImage  *neswImage = [[NSImage alloc] initByReferencingFile:neswFile];
    NSDictionary *nwseDict = [NSDictionary dictionaryWithContentsOfFile:nwseInfo];
    NSDictionary *neswDict = [NSDictionary dictionaryWithContentsOfFile:neswInfo];
    NSPoint   nwsePoint = NSMakePoint([[nwseDict valueForKey:@"hotx"] doubleValue],
                                      [[nwseDict valueForKey:@"hoty"] doubleValue]);
    NSPoint   neswPoint = NSMakePoint([[neswDict valueForKey:@"hotx"] doubleValue],
                                      [[neswDict valueForKey:@"hoty"] doubleValue]);


    cursorCache = [NSArray arrayWithObjects:
                      /* CURSOR_CUSTOM */           [NSCursor arrowCursor],     // not handed out in set
                      /* CURSOR_DEFAULT */          [NSCursor arrowCursor],
                      /* CURSOR_TEXT */             [NSCursor IBeamCursor],
                      /* CURSOR_CROSSHAIR */        [NSCursor crosshairCursor],
                      /* CURSOR_CLOSED_HAND */      [NSCursor closedHandCursor],
                      /* CURSOR_OPEN_HAND */        [NSCursor openHandCursor],
                      /* CURSOR_POINTING_HAND */    [NSCursor pointingHandCursor],
                      /* CURSOR_RESIZE_LEFT: */     [NSCursor resizeLeftCursor],
                      /* CURSOR_RESIZE_RIGHT */     [NSCursor resizeRightCursor],
                      /* CURSOR_RESIZE_UP */        [NSCursor resizeUpCursor],
                      /* CURSOR_RESIZE_DOWN */      [NSCursor resizeDownCursor],
                      /* CURSOR_RESIZE_LEFTRIGHT */ [NSCursor resizeLeftRightCursor],
                      /* CURSOR_RESIZE_UPDOWN */    [NSCursor resizeUpDownCursor],
                      /* CURSOR_DISAPPEAR */        [NSCursor disappearingItemCursor],
                      /* CURSOR_WAIT */             [NSCursor performJavaSelector:@selector(javaBusyButClickableCursor)],
                      /* CURSOR_RESIZE_SOUTHWEST */ [[NSCursor alloc] initWithImage:neswImage hotSpot:neswPoint],
                      /* CURSOR_RESIZE_SOUTHEAST */ [[NSCursor alloc] initWithImage:nwseImage hotSpot:nwsePoint],
                      /* CURSOR_RESIZE_NORTHWEST */ [[NSCursor alloc] initWithImage:nwseImage hotSpot:nwsePoint],
                      /* CURSOR_RESIZE_NORTHEAST */ [[NSCursor alloc] initWithImage:neswImage hotSpot:neswPoint],
                      /* CURSOR_MOVE */             [NSCursor performJavaSelector:@selector(javaMoveCursor)],
                      /* NS_REQUIRES_NIL_TERM */    nil
                   ];
    [cursorCache retain];

    assert([cursorCache count] == (com_sun_glass_ui_Cursor_CURSOR_MAX + 1));
}

/*
 * Class:     com_sun_glass_ui_mac_MacCursor
 * Method:    _createCursor
 * Signature: (IILcom/sun/glass/ui/Pixels;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacCursor__1createCursor
(JNIEnv *env, jclass jCursorClass, jint x, jint y, jobject jPixels)
{
    LOG("Java_com_sun_glass_ui_mac_MacCursor__1createCursor");
    jlong jcursor = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSImage *image = NULL;
        (*env)->CallVoidMethod(env, jPixels, jPixelsAttachData, ptr_to_jlong(&image));
        if (image != NULL)
        {
            NSCursor *cursor = [[NSCursor alloc] initWithImage:image hotSpot:NSMakePoint(x, y)];
            jcursor = ptr_to_jlong(cursor);
            [image release];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return jcursor;
}

/*
 * Class:     com_sun_glass_ui_mac_MacCursor
 * Method:    _set
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacCursor__1set
(JNIEnv *env, jclass jCursorClass, jint jtype)
{
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        LOG("Java_com_sun_glass_ui_mac_MacCursor__1set: %d", jtype);
        assert(jtype > 0 && jtype <= com_sun_glass_ui_Cursor_CURSOR_MAX);

        NSCursor *cursor = [cursorCache objectAtIndex: jtype];

        [cursor set];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacCursor
 * Method:    _setCustom
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacCursor__1setCustom
(JNIEnv *env, jclass jCursorClass, jlong cursorPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacCursor__1setCustom");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSCursor *cursor = (NSCursor*)jlong_to_ptr(cursorPtr);
        [cursor set];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacCursor
 * Method:    _setVisible
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacCursor__1setVisible
(JNIEnv *env, jclass jCursorClass, jboolean visible)
{
    LOG("Java_com_sun_glass_ui_mac_MacCursor__1setVisible");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if (visible == JNI_TRUE)
        {
            [NSCursor unhide];
        }
        else
        {
            [NSCursor hide];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacCursor
 * Method:    _getBestSize
 * Signature: (II)Lcom.sun.glass.ui.Size;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCursor__1getBestSize
(JNIEnv *env, jclass jCursorClass, jint width, jint height)
{
    LOG("Java_com_sun_glass_ui_mac_MacCursor__1getBestSize");

    jobject jsize = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        jint widthBest = width;
        jint heightBest = height;

        NSImage *image = [[[NSCursor arrowCursor] image] retain];

        if (widthBest <= 0)
        {
            if (image != nil)
            {
                widthBest = (jint)[image size].width;
            }
            else
            {
                widthBest = CURSOR_BEST_SIZE;
            }
        }

        if (heightBest <= 0)
        {
            if (image != nil)
            {
                heightBest = (jint)[image size].height;
            }
            else
            {
                heightBest = CURSOR_BEST_SIZE;
            }
        }

        [image release];
        jclass sizeClass = [GlassHelper ClassForName:"com.sun.glass.ui.Size" withEnv:env];
        if (!sizeClass) {
            return NULL;
        }
        jsize = (*env)->NewObject(env, sizeClass, jSizeInit, widthBest, heightBest);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return jsize;
}
