/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "PlatformJavaClasses.h"
#include "MainThreadSharedTimer.h"

#include <wtf/Assertions.h>
#include <wtf/MainThread.h>

namespace WebCore {

// The fire time is relative to the classic POSIX epoch of January 1, 1970,
// as the result of currentTime() is.
#define MINIMAL_INTERVAL 1e-9 //1ns
void MainThreadSharedTimer::setFireInterval(Seconds timeout)
{
    auto fireTime = timeout.value();
    if (fireTime < MINIMAL_INTERVAL) {
        fireTime = MINIMAL_INTERVAL;
    }
    WC_GETJAVAENV_CHKRET(env);

    static jmethodID mid = env->GetStaticMethodID(getTimerClass(env),
                                                  "fwkSetFireTime", "(D)V");
    ASSERT(mid);

    env->CallStaticVoidMethod(getTimerClass(env), mid, fireTime);
    WTF::CheckAndClearException(env);
}

void MainThreadSharedTimer::stop()
{
    WC_GETJAVAENV_CHKRET(env);

    static jmethodID mid = env->GetStaticMethodID(getTimerClass(env),
                                                  "fwkStopTimer", "()V");
    ASSERT(mid);

    env->CallStaticVoidMethod(getTimerClass(env), mid);
    WTF::CheckAndClearException(env);
}

// JDK-8146958
void MainThreadSharedTimer::invalidate()
{
}

} // namespace WebCore

extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_Timer_twkFireTimerEvent
    (JNIEnv*, jclass)
{
    WebCore::MainThreadSharedTimer::singleton().fired();
}

}
