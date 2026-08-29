/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include "WKJDOMUtils.h"

#include <webkit_java_api.h>

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

    /*
     * The null-table test is the shape of the shutdown guard that used to sit here: it
     * returned early when the environment was gone, which during teardown it was. Detaching
     * the host table is how the Java side reaches the same state now, so the guard is a
     * substitution rather than a deletion.
     */
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->timer_set_fire_time)
        return;

    cb->timer_set_fire_time(fireTime);
    wkjCheckAndClearException();
}

void MainThreadSharedTimer::stop()
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->timer_stop)
        return;

    cb->timer_stop();
    wkjCheckAndClearException();
}

// JDK-8146958
void MainThreadSharedTimer::invalidate()
{
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_timer_fire(void)
{
    WebCore::WKJCallScope wkjScope;
    WebCore::MainThreadSharedTimer::singleton().fired();
}

}
