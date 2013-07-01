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

#import "GlassStatics.h"
#import "GlassTimer.h"

static NSObject<GlassTimerDelegate> *gDelegate = nil;

@implementation GlassTimer

+ (void)setDelegate:(NSObject<GlassTimerDelegate>*)delegate
{
    gDelegate = delegate; // notice, there is no retain
}

+ (NSObject<GlassTimerDelegate>*)getDelegate
{
    return gDelegate;
}

- (void)displayLinkUpdate:(CADisplayLink *)sender
{
    if (self->_env == NULL)
    {
        (*jVM)->AttachCurrentThreadAsDaemon(jVM, (void **)&self->_env, NULL);
    }
    
    if (self->_runnable != NULL)
    {
        (*self->_env)->CallVoidMethod(self->_env, self->_runnable, jRunnableRun);
    }
    
}
@end

/*
 * Class:     com_sun_glass_ui_ios_IosTimer
 * Method:    _start
 * Signature: (Ljava/lang/Runnable;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosTimer__1start
(JNIEnv *env, jobject itimer, jobject runnable)
{
    ((GlassTimer *)gDelegate)->_runnable = (*env)->NewGlobalRef(env, runnable);
    return (jlong)gDelegate;
}

/*
 * Class:     com_sun_glass_ui_ios_IosTimer
 * Method:    _stopVsyncTimer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosTimer__1stopVsyncTimer
(JNIEnv *env, jobject itimer, jlong vtimer)
{
    [displayLink removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
}
