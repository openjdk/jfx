/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef        GLASS_MACROS_H
#define        GLASS_MACROS_H

#import <jni.h>
#import <Cocoa/Cocoa.h>
#import <assert.h>
#import <execinfo.h>

#import "GlassStatics.h"
#import "common.h"

// thread specific data to provide multiple call stack mechanism for pools
struct
{
        NSAutoreleasePool   *pool;
    int                 counter;
}
typedef GlassThreadData;

#define MAIN_JVM jVM

// debugging utility

//#define GLASS_USE_FILE_LOG
#if defined (GLASS_USE_FILE_LOG)
extern FILE *log_file;
#endif

//#define GLASS_USE_WINDOW_LOG
#if defined (GLASS_USE_WINDOW_LOG)
@interface GlassLogWindow : NSWindow
{
    NSTextView *_textView;
}

-(void)update:(NSString*)string;

@end

extern NSWindow *window_log;
#endif

extern NSDate *date;
extern NSTimeInterval intervalLast;
extern pthread_mutex_t LOCK;
static __inline__ void GlassLog(char *message, ...)
{
    pthread_mutex_lock(&LOCK);
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        if (date == nil)
        {
            date = [[NSDate dateWithTimeIntervalSinceNow:0.0f] retain];
        }
        NSDate *now = [NSDate dateWithTimeIntervalSinceNow:0.0f];
        NSTimeInterval interval = [now timeIntervalSinceDate:date];

        va_list argList;
        static char buffer[4096];
        memset(buffer, 0x00, sizeof(buffer));

        va_start(argList, message);
        vsnprintf(buffer, sizeof(buffer)-1, message, argList);
        va_end(argList);

        static char line[4096];
        memset(line, 0x00, sizeof(line));
        snprintf(line, sizeof(line)-1, "[time:%.6f, thread:%15p, main:%d] %s\n", interval, (void*)pthread_self(), pthread_main_np(), buffer);

#if !defined (GLASS_USE_FILE_LOG) && !defined (GLASS_USE_WINDOW_LOG)
        NSLog(@"%s", line);
#endif

#if defined (GLASS_USE_FILE_LOG)
        if (log_file == NULL)
        {
            static char name[4096];
            snprintf(name, sizeof(name)-1, "~/Desktop/glass_log_%d.txt", getpid());
            strncpy(name, [[[NSString stringWithUTF8String:name] stringByExpandingTildeInPath] UTF8String], sizeof(name)-1);
            log_file = fopen(name, "a");
        }
        if (log_file != NULL)
        {
            fputs(line, log_file);
            fflush(log_file);
        }
#endif

#if defined (GLASS_USE_WINDOW_LOG)
        if (window_log == nil)
        {
            static CGFloat WIDTH = 1024.0f;
            static CGFloat HEIGHT = 768.0f;
            static CGFloat OFFSET = 64.0f;
            static CGFloat ALPHA = 0.75f;
            NSScreen *screen = [[NSScreen screens] objectAtIndex:0];
            NSRect screenRect = [screen visibleFrame];
            CGFloat x = screenRect.size.width - WIDTH - OFFSET;
            CGFloat y = screenRect.size.height - HEIGHT - OFFSET;
            window_log = [[GlassLogWindow alloc] initWithContentRect:NSMakeRect(x, y, WIDTH, HEIGHT) styleMask:NSBorderlessWindowMask backing:NSBackingStoreBuffered defer:NO];
            [window_log setAlphaValue:ALPHA];
            [window_log setLevel:NSScreenSaverWindowLevel];
            [window_log setAcceptsMouseMovedEvents:NO];
            [window_log setIgnoresMouseEvents:YES];
            [window_log orderFrontRegardless];
        }
        if (window_log != nil)
        {
            static CGFloat SKIP_LINE_TIME = 5.0f;
            if (interval-intervalLast > SKIP_LINE_TIME)
            {
                [window_log performSelectorOnMainThread:@selector(update:) withObject:@"\n" waitUntilDone:NO];
            }
            [window_log performSelectorOnMainThread:@selector(update:) withObject:[NSString stringWithUTF8String:line] waitUntilDone:NO];
        }
#endif
        intervalLast = interval;
    }
    [pool drain];
    pthread_mutex_unlock(&LOCK);
}

#define GLASS_LOG(MSG, ...) GlassLog(MSG, ## __VA_ARGS__ )

// assert there is no outstanding java exception pending
#define GLASS_CHECK_EXCEPTION(ENV)                                                 \
do {                                                                               \
    jthrowable t = (*ENV)->ExceptionOccurred(ENV);                                 \
    if (t) {                                                                       \
        (*ENV)->ExceptionClear(ENV);                                               \
        (*ENV)->CallStaticVoidMethod(                                              \
            ENV, jApplicationClass, javaIDs.Application.reportException, t);       \
        (*ENV)->ExceptionClear(ENV);                                               \
    };                                                                             \
} while (0)

// assert main Java thread is still attached
#define GLASS_ASSERT_MAIN_JAVA_THREAD(env) \
        if ((pthread_main_np() == 0) && (jEnv == NULL)) { \
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

#define GLASS_CALLSTACK(MSG) \
{ \
    void* callstack[128]; \
    fprintf(stderr, MSG "%s:%s:%d\n", \
            __FUNCTION__, __FILE__, __LINE__); \
    int i, frames = backtrace(callstack, 128); \
    char** strs = backtrace_symbols(callstack, frames); \
    for (i = 0; i < frames; ++i) { \
        printf("%s\n", strs[i]); \
    } \
    free(strs); \
}


// Retrieve Java env, asserting the call originated on main thread.
// Warn if the JVM has already been detached.
#define GET_MAIN_JENV \
    assert(pthread_main_np() == 1); \
    if (jEnv == NULL) \
        GLASS_CALLSTACK("Java has been detached already, but someone is still trying to use it at ") \
    JNIEnv *env = jEnv;

// Retrieve Java env, asserting the call originated on main thread.
// This variant is silent if the JVM has been detached, making it suitable
// for use by dealloc methods, which are called by the auto-release mechanism.
#define GET_MAIN_JENV_NOWARN \
    assert(pthread_main_np() == 1); \
    JNIEnv *env = jEnv;

#endif
