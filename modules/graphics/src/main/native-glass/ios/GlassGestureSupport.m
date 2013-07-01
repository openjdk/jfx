/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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


#import "GlassMacros.h"
#import "GlassStatics.h"

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_ios_IosGestureSupport
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosGestureSupport__1initIDs
(JNIEnv *env, jclass jClass)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosGestureSupport__1initIDs");

    // Initialize  methods IDs of java IosGestureSupport callbacks.
    
    if (jGestureSupportClass == NULL)
    {
        jGestureSupportClass = (*env)->NewGlobalRef(env, jClass);
    }

    if (jGestureSupportRotateGesturePerformed == NULL)
    {
        jGestureSupportRotateGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "rotateGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIIF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportScrollGesturePerformed == NULL)
    {
        jGestureSupportScrollGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "scrollGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IZFFFFFF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportSwipeGesturePerformed == NULL)
    {
        jGestureSupportSwipeGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "swipeGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIII)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportMagnifyGesturePerformed == NULL)
    {
        jGestureSupportMagnifyGesturePerformed = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "magnifyGesturePerformed",
                                "(Lcom/sun/glass/ui/View;IIIIIF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportGestureFinished == NULL)
    {
        jGestureSupportGestureFinished = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "gestureFinished",
                                "(Lcom/sun/glass/ui/View;IIIII)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportNotifyBeginTouchEvent == NULL)
    {
        jGestureSupportNotifyBeginTouchEvent = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "notifyBeginTouchEvent",
                                "(Lcom/sun/glass/ui/View;II)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportNotifyNextTouchEvent == NULL)
    {
        jGestureSupportNotifyNextTouchEvent = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "notifyNextTouchEvent",
                                "(Lcom/sun/glass/ui/View;IJFF)V");
        GLASS_CHECK_EXCEPTION(env);
    }

    if (jGestureSupportNotifyEndTouchEvent == NULL)
    {
        jGestureSupportNotifyEndTouchEvent = 
                    (*env)->GetStaticMethodID(env, jGestureSupportClass, 
                                "notifyEndTouchEvent",
                                "(Lcom/sun/glass/ui/View;)V");
        GLASS_CHECK_EXCEPTION(env);
    }
}
