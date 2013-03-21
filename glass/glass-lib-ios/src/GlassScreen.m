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
#import "GlassScreen.h"
#import "GlassStatics.h"
#import "GlassTimer.h"

static inline jobject createJavaScreen(JNIEnv *env, UIScreen* screen)
{
    jmethodID screenInit = (*env)->GetMethodID(env, mat_jScreenClass,
                                                   "<init>",
                                                   "(JIIIIIIIIIIIF)V");

    return (jobject)(*env)->NewObject(env, mat_jScreenClass, screenInit,
                                      ptr_to_jlong(screen),

                                      32,

                                      (jint)[screen bounds].origin.x,
                                      (jint)[screen bounds].origin.y,
                                      (jint)[screen bounds].size.width,
                                      (jint)[screen bounds].size.height,
                                      
                                      (jint)[screen applicationFrame].origin.x,
                                      (jint)[screen applicationFrame].origin.y,
                                      (jint)[screen applicationFrame].size.width,
                                      (jint)[screen applicationFrame].size.height,
                                      
                                      
                                      (jint)[screen currentMode].size.width,
                                      (jint)[screen currentMode].size.height,
                                      (jfloat)[screen scale]);
    
}

void GlassScreenDidChangeScreenParameters(JNIEnv *env)
{    
    jmethodID jScreenNotifySettingsChanged = (*env)->GetStaticMethodID(env, mat_jScreenClass, "notifySettingsChanged", "()V");

    (*env)->CallStaticVoidMethod(env, mat_jScreenClass, jScreenNotifySettingsChanged);
}

jobjectArray createJavaScreens(JNIEnv* env) {
    NSArray* screens = [UIScreen screens];

    if (mat_jScreenClass == NULL)
    {
        mat_jScreenClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/Screen"));
    }

    jobjectArray screenArray = (*env)->NewObjectArray(env,
                                                      [screens count],
                                                      mat_jScreenClass,
                                                      NULL);

    for (NSUInteger index = 0; index < [screens count]; index++) {
        jobject javaScreen = createJavaScreen(env, [screens objectAtIndex:index]);
        (*env)->SetObjectArrayElement(env, screenArray, index, javaScreen);
    }

    return screenArray;
}


