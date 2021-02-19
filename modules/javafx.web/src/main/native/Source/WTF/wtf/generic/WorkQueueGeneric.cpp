/*
 * Copyright (C) 2016 Konstantin Tokavev <annulen@yandex.ru>
 * Copyright (C) 2016 Yusuke Suzuki <utatane.tea@gmail.com>
 * Copyright (C) 2011 Igalia S.L.
 * Copyright (C) 2010 Apple Inc. All rights reserved.
 * Portions Copyright (c) 2010 Motorola Mobility, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include <wtf/WorkQueue.h>

#include <wtf/threads/BinarySemaphore.h>

#if PLATFORM(JAVA)
#include <wtf/java/JavaEnv.h>
#endif

void WorkQueue::platformInitialize(const char* name, Type, QOS)
{
    BinarySemaphore semaphore;
    Thread::create(name, [&] {
        m_runLoop = &RunLoop::current();
        semaphore.signal();
        m_runLoop->run();
    })->detach();
    semaphore.wait();
}

void WorkQueue::platformInvalidate()
{
    if (m_runLoop) {
        Ref<RunLoop> protector(*m_runLoop);
        protector->stop();
        protector->dispatch([] {
            RunLoop::current().stop();
        });
    }
}

void WorkQueue::dispatch(Function<void()>&& function)
{
    RefPtr<WorkQueue> protect(this);
    m_runLoop->dispatch([protect, function = WTFMove(function)] {
#if PLATFORM(JAVA)
        AttachThreadAsDaemonToJavaEnv autoAttach;
#endif
        function();
    });
}

void WorkQueue::dispatchAfter(Seconds delay, Function<void()>&& function)
{
    RefPtr<WorkQueue> protect(this);
#if OS(WINDOWS) && PLATFORM(JAVA)
    // From empirical testing, we've seen CreateTimerQueueTimer() sometimes fire up to 5+ ms early.
    // This causes havoc for clients of this code that expect to not be called back until the
    // specified duration has expired. Other folks online have also observed some slop in the
    // firing times of CreateTimerQuqueTimer(). From the data posted at
    // http://omeg.pl/blog/2011/11/on-winapi-timers-and-their-resolution, it appears that the slop
    // can be up to about 10 ms. To ensure that we don't fire the timer early, we'll tack on a
    // slop adjustment to the duration, and we'll use double the worst amount of slop observed
    // so far.
    const Seconds slopAdjustment { 20_ms };
    if (delay)
        delay += slopAdjustment;
#endif
    m_runLoop->dispatchAfter(delay, [protect, function = WTFMove(function)] {
#if PLATFORM(JAVA)
        AttachThreadAsDaemonToJavaEnv autoAttach;
#endif
        function();
    });
}
