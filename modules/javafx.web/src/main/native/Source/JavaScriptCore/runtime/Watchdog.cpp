/*
 * Copyright (C) 2013-2023 Apple Inc. All rights reserved.
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
#include "Watchdog.h"

#include "VM.h"
#include <wtf/CPUTime.h>
#include <wtf/TZoneMallocInlines.h>

namespace JSC {

WTF_MAKE_TZONE_ALLOCATED_IMPL(Watchdog);

Watchdog::Watchdog(VM* vm)
    : m_vm(vm)
    , m_timeLimit(noTimeLimit)
    , m_cpuDeadline(noTimeLimit)
    , m_deadline(MonotonicTime::infinity())
    , m_callback(nullptr)
    , m_callbackData1(nullptr)
    , m_callbackData2(nullptr)
    , m_timerQueue(WorkQueue::create("jsc.watchdog.queue"_s, WorkQueue::QOS::Utility))
{
}

void Watchdog::setTimeLimit(Seconds limit,
    ShouldTerminateCallback callback, void* data1, void* data2)
{
    ASSERT(m_vm->currentThreadIsHoldingAPILock());

    m_timeLimit = limit;
    m_callback = callback;
    m_callbackData1 = data1;
    m_callbackData2 = data2;

    if (m_hasEnteredVM && hasTimeLimit())
        startTimer(m_timeLimit);
}

bool Watchdog::shouldTerminate(JSGlobalObject* globalObject)
{
    ASSERT(m_vm->currentThreadIsHoldingAPILock());

    Seconds epsilon;
#if OS(WINDOWS)
    // We can reach this point as much as 15ms before the deadline on Windows,
    // in which case the watchdog will never get to do its job.
    // Adding this leeway shouldn't cause a problem for other platforms
    // (since the "deadline is infinity" case should be the crucial one),
    // but it is a fact that only Windows is experiencing the issue.
    epsilon = Seconds::fromMilliseconds(20);
#endif
    if (MonotonicTime::timePointFromNow(epsilon) < m_deadline)
        return false; // Just a stale timer firing. Nothing to do.

    // Set m_deadline to MonotonicTime::infinity() here so that we can reject all future
    // spurious wakes.
    m_deadline = MonotonicTime::infinity();

    auto cpuTime = CPUTime::forCurrentThread();
    if (cpuTime < m_cpuDeadline) {
        auto remainingCPUTime = m_cpuDeadline - cpuTime;
        startTimer(remainingCPUTime);
        return false;
    }

    // Note: we should not be holding the lock while calling the callbacks. The callbacks may
    // call setTimeLimit() which will try to lock as well.

    // If m_callback is not set, then we terminate by default.
    // Else, we let m_callback decide if we should terminate or not.
    bool needsTermination = !m_callback
        || m_callback(globalObject, m_callbackData1, m_callbackData2);
    if (needsTermination)
        return true;

    // If we get here, then the callback above did not want to terminate execution. As a
    // result, the callback may have done one of the following:
    //   1. cleared the time limit (i.e. watchdog is disabled),
    //   2. set a new time limit via Watchdog::setTimeLimit(), or
    //   3. did nothing (i.e. allow another cycle of the current time limit).
    //
    // In the case of 1, we don't have to do anything.
    // In the case of 2, Watchdog::setTimeLimit() would already have started the timer.
    // In the case of 3, we need to re-start the timer here.

    ASSERT(m_hasEnteredVM);
    bool callbackAlreadyStartedTimer = (m_cpuDeadline != noTimeLimit);
    if (hasTimeLimit() && !callbackAlreadyStartedTimer)
        startTimer(m_timeLimit);

    return false;
}

bool Watchdog::hasTimeLimit()
{
    return (m_timeLimit != noTimeLimit);
}

void Watchdog::enteredVM()
{
    m_hasEnteredVM = true;
    if (hasTimeLimit())
        startTimer(m_timeLimit);
}

void Watchdog::exitedVM()
{
    ASSERT(m_hasEnteredVM);
    stopTimer();
    m_hasEnteredVM = false;
}

void Watchdog::startTimer(Seconds timeLimit)
{
    ASSERT(m_hasEnteredVM);
    ASSERT(m_vm->currentThreadIsHoldingAPILock());
    ASSERT(hasTimeLimit());
    ASSERT(timeLimit <= m_timeLimit);

    m_cpuDeadline = CPUTime::forCurrentThread() + timeLimit;
    auto now = MonotonicTime::now();
    auto deadline = now + timeLimit;

    if ((now < m_deadline) && (m_deadline <= deadline))
        return; // Wait for the current active timer to expire before starting a new one.

    // Else, the current active timer won't fire soon enough. So, start a new timer.
    m_deadline = deadline;

    // We need to ensure that the Watchdog outlives the timer.
    // For the same reason, the timer may also outlive the VM that the Watchdog operates on.
    // So, we always need to null check m_vm before using it. The VM will notify the Watchdog
    // via willDestroyVM() before it goes away.
    RefPtr<Watchdog> protectedThis = this;
    m_timerQueue->dispatchAfter(timeLimit, [this, protectedThis] {
        Locker locker { m_lock };
        if (m_vm)
            m_vm->notifyNeedWatchdogCheck();
    });
}

void Watchdog::stopTimer()
{
    ASSERT(m_hasEnteredVM);
    ASSERT(m_vm->currentThreadIsHoldingAPILock());
    m_cpuDeadline = noTimeLimit;
}

void Watchdog::willDestroyVM(VM* vm)
{
    Locker locker { m_lock };
    ASSERT_UNUSED(vm, m_vm == vm);
    m_vm = nullptr;
}

} // namespace JSC
