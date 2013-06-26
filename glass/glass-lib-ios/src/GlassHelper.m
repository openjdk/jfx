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

#import "GlassHelper.h"
#import "GlassMacros.h"

@implementation GlassHelper

/*
 * Function to find a glass class using the context class loader. All glass
 * classes must be looked up using this function rather than FindClass so that
 * the correct ClassLoader is used.
 *
 * Note that the className passed to this function must use "." rather than "/"
 * as a package separator.
 */
+ (jclass)ClassForName:(char*)className withEnv:(JNIEnv*)env
{
    static jclass threadCls = NULL;
    if (threadCls == NULL)
    {
        threadCls = (*env)->FindClass(env, "java/lang/Thread");
    }
    if (threadCls == NULL)
    {
        NSLog(@"GlassHelper error: threadCls == NULL");
        return NULL;
    }
    
    static jmethodID currentThreadMID = NULL;
    if (currentThreadMID == NULL)
    {
        currentThreadMID = (*env)->GetStaticMethodID(env, threadCls, "currentThread", "()Ljava/lang/Thread;");
    }
    if (currentThreadMID == NULL)
    {
        NSLog(@"GlassHelper error: currentThreadMID == NULL");
        return NULL;
    }
    
    static jmethodID getContextClassLoaderMID = NULL;
    if (getContextClassLoaderMID == NULL)
    {
        getContextClassLoaderMID = (*env)->GetMethodID(env, threadCls, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
    }
    if (getContextClassLoaderMID == NULL)
    {
        NSLog(@"GlassHelper error: getContextClassLoaderMID == NULL");
        return NULL;
    }
    
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
    
    jobject jCurrentThread = (*env)->CallStaticObjectMethod(env, threadCls, currentThreadMID);
    if (jCurrentThread == NULL)
    {
        NSLog(@"GlassHelper error: jCurrentThread == NULL");
        return NULL;
    }
    
    jobject contextClassLoader = (*env)->CallObjectMethod(env, jCurrentThread, getContextClassLoaderMID);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE)
    {
        (*env)->ExceptionDescribe(env);
        return NULL;
    }
    
    jstring classNameStr = (*env)->NewStringUTF(env, className);
    if (classNameStr == NULL)
    {
        NSLog(@"GlassHelper error: classNameStrs == NULL");
        return NULL;
    }
    
    // possibly we can cache values in Dictionary
    return (*env)->CallStaticObjectMethod(env, classCls, forNameMID, classNameStr, JNI_TRUE, contextClassLoader);
}

// Returns java glass application class
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

/*
 * Notify java about iOS application state changes.
 */

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

+ (jmethodID)ApplicationNotifyDidReceiveMemoryWarningMethod
{
    static jmethodID _ApplicationNotifyDidReceiveMemoryWarningMethod = NULL;
    if (_ApplicationNotifyDidReceiveMemoryWarningMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyDidReceiveMemoryWarningMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyDidReceiveMemoryWarning", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyDidReceiveMemoryWarningMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyDidReceiveMemoryWarningMethod == NULL");
    }
    return _ApplicationNotifyDidReceiveMemoryWarningMethod;
}


+ (jmethodID)ApplicationNotifyQuitMethod
{
    static jmethodID _ApplicationNotifyQuitMethod = NULL;
    if (_ApplicationNotifyQuitMethod == NULL)
    {
        GET_MAIN_JENV;
        _ApplicationNotifyQuitMethod = (*env)->GetMethodID(env, [GlassHelper ApplicationClass], "notifyQuit", "()V");
        GLASS_CHECK_EXCEPTION(env);
    }
    if (_ApplicationNotifyQuitMethod == NULL)
    {
        NSLog(@"GlassHelper error: _ApplicationNotifyQuitMethod == NULL");
    }
    return _ApplicationNotifyQuitMethod;
}


@end
