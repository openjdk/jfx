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

#import "GlassMacros.h"
#import "GlassHelper.h"

@implementation GlassHelper

#pragma mark --- ClassLoader

static volatile jobject glassClassLoader = NULL;
+ (void)SetGlassClassLoader:(jobject)classLoader withEnv:(JNIEnv*)env
{
    glassClassLoader = (*env)->NewGlobalRef(env, classLoader);
}

/*
 * Function to find a glass class using the glass class loader. All glass
 * classes must be looked up using this function rather than FindClass so that
 * the correct ClassLoader is used.
 *
 * Note that the className passed to this function must use "." rather than "/"
 * as a package separator.
 */
+ (jclass)ClassForName:(char*)className withEnv:(JNIEnv*)env
{
    static jclass classCls = NULL;
    if (classCls == NULL)
    {
        classCls = (*env)->FindClass(env, "java/lang/Class");
    }
    if (classCls == NULL)
    {
        NSLog(@"GlassHelper error: classCls == NULL");
        return NULL;
    }

    static jmethodID forNameMID = NULL;
    if (forNameMID == NULL)
    {
        forNameMID = (*env)->GetStaticMethodID(env, classCls, "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
    }
    if (forNameMID == NULL)
    {
        NSLog(@"GlassHelper error: forNameMID == NULL");
        return NULL;
    }

    jstring classNameStr = (*env)->NewStringUTF(env, className);
    if (classNameStr == NULL)
    {
        NSLog(@"GlassHelper error: classNameStrs == NULL");
        return NULL;
    }

    jclass foundClass = (*env)->CallStaticObjectMethod(env, classCls,
        forNameMID,classNameStr, JNI_TRUE, glassClassLoader);

    return foundClass;
}

+ (jclass)ApplicationClass
{
    static jclass _ApplicationClass = NULL;
    if (_ApplicationClass == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationClass = [GlassHelper ClassForName:"com.sun.glass.ui.Application" withEnv:env];
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationClass == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationClass == NULL");
    }
    return _ApplicationClass;
}

+ (jmethodID)ApplicationNotifyWillFinishLaunchingMethod
{
    static jmethodID _ApplicationNotifyWillFinishLaunchingMethod = NULL;
    if (_ApplicationNotifyWillFinishLaunchingMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyWillFinishLaunchingMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyWillFinishLaunching", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyWillFinishLaunchingMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyWillFinishLaunchingMethod == NULL");
    }
    return _ApplicationNotifyWillFinishLaunchingMethod;
}

+ (jmethodID)ApplicationNotifyDidFinishLaunchingMethod
{
    static jmethodID _ApplicationNotifyDidFinishLaunchingMethod = NULL;
    if (_ApplicationNotifyDidFinishLaunchingMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyDidFinishLaunchingMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyDidFinishLaunching", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyDidFinishLaunchingMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyDidFinishLaunchingMethod == NULL");
    }
    return _ApplicationNotifyDidFinishLaunchingMethod;
}

+ (jmethodID)ApplicationNotifyWillBecomeActiveMethod
{
    static jmethodID _ApplicationNotifyWillBecomeActiveMethod = NULL;
    if (_ApplicationNotifyWillBecomeActiveMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyWillBecomeActiveMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyWillBecomeActive", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyWillBecomeActiveMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyWillBecomeActiveMethod == NULL");
    }
    return _ApplicationNotifyWillBecomeActiveMethod;
}

+ (jmethodID)ApplicationNotifyDidBecomeActiveMethod
{
    static jmethodID _ApplicationNotifyDidBecomeActiveMethod = NULL;
    if (_ApplicationNotifyDidBecomeActiveMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyDidBecomeActiveMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyDidBecomeActive", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyDidBecomeActiveMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyDidBecomeActiveMethod == NULL");
    }
    return _ApplicationNotifyDidBecomeActiveMethod;
}

+ (jmethodID)ApplicationNotifyWillResignActiveMethod
{
    static jmethodID _ApplicationNotifyWillResignActiveMethod = NULL;
    if (_ApplicationNotifyWillResignActiveMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyWillResignActiveMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyWillResignActive", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyWillResignActiveMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyWillResignActiveMethod == NULL");
    }
    return _ApplicationNotifyWillResignActiveMethod;
}

+ (jmethodID)ApplicationNotifyDidResignActiveMethod
{
    static jmethodID _ApplicationNotifyDidResignActiveMethod = NULL;
    if (_ApplicationNotifyDidResignActiveMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyDidResignActiveMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyDidResignActive", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyDidResignActiveMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyDidResignActiveMethod == NULL");
    }
    return _ApplicationNotifyDidResignActiveMethod;
}

+ (jmethodID)ApplicationNotifyWillHideMethod
{
    static jmethodID _ApplicationNotifyWillHideMethod = NULL;
    if (_ApplicationNotifyWillHideMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyWillHideMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyWillHide", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyWillHideMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyWillHideMethod == NULL");
    }
    return _ApplicationNotifyWillHideMethod;
}

+ (jmethodID)ApplicationNotifyDidHideMethod
{
    static jmethodID _ApplicationNotifyDidHideMethod = NULL;
    if (_ApplicationNotifyDidHideMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyDidHideMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyDidHide", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyDidHideMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyDidHideMethod == NULL");
    }
    return _ApplicationNotifyDidHideMethod;
}

+ (jmethodID)ApplicationNotifyWillUnhideMethod
{
    static jmethodID _ApplicationNotifyWillUnhideMethod = NULL;
    if (_ApplicationNotifyWillUnhideMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyWillUnhideMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyWillUnhide", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyWillUnhideMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyWillUnhideMethod == NULL");
    }
    return _ApplicationNotifyWillUnhideMethod;
}

+ (jmethodID)ApplicationNotifyDidUnhideMethod
{
    static jmethodID _ApplicationNotifyDidUnhideMethod = NULL;
    if (_ApplicationNotifyDidUnhideMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyDidUnhideMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyDidUnhide", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyDidUnhideMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyDidUnhideMethod == NULL");
    }
    return _ApplicationNotifyDidUnhideMethod;
}

+ (jmethodID)ApplicationNotifyOpenFilesMethod
{
    static jmethodID _ApplicationNotifyOpenFilesMethod = NULL;
    if (_ApplicationNotifyOpenFilesMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyOpenFilesMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyOpenFiles", "([Ljava/lang/String;)V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyOpenFilesMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyOpenFilesMethod == NULL");
    }
    return _ApplicationNotifyOpenFilesMethod;
}

+ (jmethodID)ApplicationNotifyWillQuitMethod
{
    static jmethodID _ApplicationNotifyWillQuitMethod = NULL;
    if (_ApplicationNotifyWillQuitMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyWillQuitMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyWillQuit", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyWillQuitMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyWillQuitMethod == NULL");
    }
    return _ApplicationNotifyWillQuitMethod;
}

#pragma mark --- Invocation

+ (BOOL)InvokeSelectorIfAvailable:(SEL)aSelector forClass:(Class)aClass withArgument:(void *)anArgument withReturnValue:(void **)aReturnValue
{
    if ([aClass respondsToSelector:aSelector] == YES)
    {
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[aClass methodSignatureForSelector:aSelector]];
        [invocation setSelector:aSelector];
        [invocation setTarget:aClass];
        
        if (anArgument != NULL)
        {
            [invocation setArgument:anArgument atIndex:2]; // arguments 0 and 1 are self and _cmd respectively, which are set automatically by NSInvocation
        }
        
        [invocation invoke];
       
        if (aReturnValue != NULL)
        {
            [invocation getReturnValue:aReturnValue];
        }
        
        return YES;
    }
    else
    {
        return NO;
    }
}

@end
