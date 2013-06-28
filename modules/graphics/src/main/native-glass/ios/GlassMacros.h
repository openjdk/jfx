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

#pragma once

#ifndef GLASS_MACROS_H
#define GLASS_MACROS_H

#import <UIKit/UIKit.h>
#import <jni.h>
#import <assert.h>
#import <pthread.h>

#include "common.h"

// thread specific data to provide multiple call stack mechanism for pools
struct
{
    NSAutoreleasePool   *pool;
    int                 counter;
}
typedef GlassThreadData;

extern pthread_key_t GlassThreadDataKey;

// assert there is no outstanding java exception pending
#define GLASS_CHECK_EXCEPTION(ENV)                                                 \
do {                                                                               \
    jthrowable t = (*ENV)->ExceptionOccurred(ENV);                                 \
    if (t) {                                                                       \
        (*ENV)->ExceptionClear(ENV);                                               \
        (*ENV)->CallStaticVoidMethod(                                              \
            ENV, jApplicationClass, jApplicationReportException, t);               \
    };                                                                             \
} while (0)

// assert main Java thread is still attached
#define GLASS_ASSERT_MAIN_JAVA_THREAD(env) \
    if ((pthread_main_np() == 0) && (jEnv == NULL)) { \
        NSLog(@"GLASS_ASSERT_MAIN_JAVA_THREAD:  %s :: %d",__FILE__, __LINE__); \
        GLASS_CHECK_EXCEPTION(env); \
        jclass newExcCls = (*env)->FindClass(env, "java/lang/RuntimeException"); \
        if (newExcCls != 0) { \
            (*env)->ThrowNew(env, newExcCls, "Main Java thread is detached."); \
        } \
    }

// setup/release autorelase pools
#define GLASS_POOL_ENTER \
{ \
    GlassThreadData *_GlassThreadData = (GlassThreadData*)pthread_getspecific(GlassThreadDataKey); \
    if (_GlassThreadData == NULL) \
    { \
        _GlassThreadData = malloc(sizeof(GlassThreadData)); \
        memset(_GlassThreadData, 0x00, sizeof(GlassThreadData)); \
        pthread_setspecific(GlassThreadDataKey, _GlassThreadData); \
    } \
    assert(_GlassThreadData->counter >= 0); \
    if (_GlassThreadData->counter++ == 0) \
    { \
        _GlassThreadData->pool = [[NSAutoreleasePool alloc] init]; \
    }
#define GLASS_POOL_EXIT \
    if (--_GlassThreadData->counter == 0) \
    { \
        [_GlassThreadData->pool drain]; \
        _GlassThreadData->pool = nil; \
    } \
    assert(_GlassThreadData->counter >= 0); \
}

// variations of GLASS_POOL_ENTER/GLASS_POOL_EXIT allowing them to be used accross different calls
#define GLASS_POOL_PUSH \
    GLASS_POOL_ENTER \
}
#define GLASS_POOL_POP \
{ \
    GlassThreadData *_GlassThreadData = (GlassThreadData*)pthread_getspecific(GlassThreadDataKey); \
    GLASS_POOL_EXIT

#if MAT_IOS_DEBUG

#define GLASS_LOG(fmt, ...) NSLog((@"%s [line %d] " fmt), __PRETTY_FUNCTION__, __LINE__, ##__VA_ARGS__);

#else

#define GLASS_LOG(...) {}

#endif


// retrieve main thread Java env asserting the call originated on main thread
#define GET_MAIN_JENV \
GLASS_LOG("assert([[NSThread currentThread] isMainThread] == YES) %d", (int)([[NSThread currentThread] isMainThread] == YES)); \
if (jEnv == NULL) {NSLog(@"ERROR: Java has been detached already, but someone is still trying to use it at %s:%s:%d\n", __FUNCTION__, __FILE__, __LINE__); @throw [[NSException new] autorelease];} \
JNIEnv *env = jEnv;

#endif
