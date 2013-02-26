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

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>

#import "common.h"
#import "GlassMacros.h"
#import "GlassStatics.h"
#import "GlassTimer.h"

#include <com_sun_glass_ui_Screen.h>

// Initialize glass Screen (jscreen) object with screen info.
void SetJavaScreen(UIScreen *screen, JNIEnv *env, jobject jscreen)
{
    if (screen != nil)
    {
        GLASS_LOG("GlasScreen SetJavaScreen() (setting fields of com.sun.glass.ui.Screen)");
        (*env)->SetLongField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "ptr", "J"), ptr_to_jlong([screen retain]));
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "depth", "I"), 32);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "x", "I"), (jint) [screen bounds].origin.x);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "y", "I"), (jint) [screen bounds].origin.y);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "width", "I"), (jint) [screen bounds].size.width);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "height", "I"), (jint) [screen bounds].size.height);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "visibleX", "I"), (jint) [screen applicationFrame].origin.x);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "visibleY", "I"), (jint) [screen applicationFrame].origin.y);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "visibleWidth", "I"), (jint) [screen applicationFrame].size.width);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "visibleHeight", "I"), (jint) [screen applicationFrame].size.height);
        (*env)->SetFloatField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "scale", "F"), [screen scale]);

        CGSize resolution = [[screen currentMode] size];
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "resolutionX", "I"), (int)resolution.width);
        (*env)->SetIntField(env, jscreen, (*env)->GetFieldID(env, mat_jScreenClass, "resolutionY", "I"), (int)resolution.height);
    }
}


static inline jobject createJavaScreen(JNIEnv *env)
{
    jobject jscreen = (*env)->NewObject(env, mat_jScreenClass, (*env)->GetMethodID(env, mat_jScreenClass, "<init>", "()V"));
    {
        //On iOS we always return mainScreen
        SetJavaScreen([UIScreen mainScreen], env, jscreen);
    }
    
    return jscreen;
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getDeepestScreen
 * Signature: (Lcom/sun/glass/ui/Screen;)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_ios_IosScreen__1getDeepestScreen
(JNIEnv *env, jclass jscreenClass, jobject jscreen) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosScreen__1getDeepestScreen");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        //On iOS we don't have deepestScreen method, thus we always return mainScreen
        //SetJavaScreen([UIScreen deepestScreen], env, jscreen);
        SetJavaScreen([UIScreen mainScreen], env, jscreen);
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
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_ios_IosScreen__1getMainScreen
(JNIEnv *env, jclass jscreenClass, jobject jscreen) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosScreen__1getMainScreen");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        SetJavaScreen([UIScreen mainScreen], env, jscreen);
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
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_ios_IosScreen__1getScreenForLocation
(JNIEnv *env, jclass jscreenClass, jobject jscreen, jint x, jint y) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosScreen__1getScreenForLocation");
    
    return jscreen;
}

/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getScreenForPtr
 * Signature: (Lcom/sun/glass/ui/Screen;J)Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_ios_IosScreen__1getScreenForPtr
(JNIEnv *env, jclass jscreenClass, jobject jscreen, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosScreen__1getScreenForPtr");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        SetJavaScreen((UIScreen *)jlong_to_ptr(ptr), env, jscreen);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return jscreen;
}


/*
 * Class:     com_sun_glass_ui_Screen
 * Method:    _getScreens
 * Signature: (Ljava/util/Vector;)Ljava/util/Vector;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_ios_IosScreen__1getScreens
(JNIEnv *env, jclass jscreenClass, jobject vectorScreens) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosScreen__1getScreens");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSArray *screens = [UIScreen screens];
        for (int i=0; i<[screens count]; i++)
        {
            jobject jscreen = createJavaScreen(env);
            {
                  SetJavaScreen([screens objectAtIndex:i], env, jscreen);
                
                (*env)->CallVoidMethod(env, vectorScreens, mat_jVectorAddElement, jscreen);
            }
            (*env)->DeleteLocalRef(env, jscreen);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return vectorScreens;
}


/*
 * Class:     com_sun_glass_ui_ios_IosScreen
 * Method:    _getVideoRefreshPeriod
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL 
Java_com_sun_glass_ui_ios_IosScreen__1getVideoRefreshPeriod(JNIEnv *env, jclass screenClass) 
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosScreen__1getVideoRefreshPeriod");
    
    double outRefresh = 1.0 / 30.0;     // ability to set frame divider
    return (outRefresh * 1000.0);       // to millis
}
