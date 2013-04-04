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
#import "com_sun_glass_ui_mac_MacGestureSupport.h"

#import "GlassMacros.h"
#import "GlassStatics.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_mac_MacGestureSupport
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacGestureSupport__1initIDs
(JNIEnv *env, jclass jClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacGestureSupport__1initIDs");

    if (javaIDs.GestureSupport.rotateGesturePerformed == NULL)
    {
        javaIDs.GestureSupport.rotateGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "rotateGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIIF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.scrollGesturePerformed == NULL)
    {
        javaIDs.GestureSupport.scrollGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "scrollGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIIIFF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.swipeGesturePerformed == NULL)
    {
        javaIDs.GestureSupport.swipeGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "swipeGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIII)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.magnifyGesturePerformed == NULL)
    {
        javaIDs.GestureSupport.magnifyGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "magnifyGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIIF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.gestureFinished == NULL)
    {
        javaIDs.GestureSupport.gestureFinished = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "gestureFinished",
                                "(Lcom/sun/glass/ui/View;IIIII)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.notifyBeginTouchEvent == NULL)
    {
        javaIDs.GestureSupport.notifyBeginTouchEvent = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "notifyBeginTouchEvent",
                                "(Lcom/sun/glass/ui/View;II)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.notifyNextTouchEvent == NULL)
    {
        javaIDs.GestureSupport.notifyNextTouchEvent = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "notifyNextTouchEvent",
                                "(Lcom/sun/glass/ui/View;IJFF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (javaIDs.GestureSupport.notifyEndTouchEvent == NULL)
    {
        javaIDs.GestureSupport.notifyEndTouchEvent = 
                    (*env)->GetStaticMethodID(env, jClass,
                                "notifyEndTouchEvent",
                                "(Lcom/sun/glass/ui/View;)V");
        GLASS_CHECK_EXCEPTION(env);
    }
}
