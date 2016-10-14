/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "RunLoop.h"

namespace WTF {

// JDK-8146878
//XXX: implement these stubs?

RunLoop::RunLoop() {}
RunLoop::~RunLoop() {}

void RunLoop::TimerBase::fired() {}

void RunLoop::run() {}
void RunLoop::stop() {}
void RunLoop::wakeUp() {}

RunLoop::TimerBase::TimerBase(RunLoop& runLoop)
    : m_runLoop(runLoop)
{
}

RunLoop::TimerBase::~TimerBase()
{
    stop();
}

void RunLoop::TimerBase::start(double fireInterval, bool repeat)
{
}

void RunLoop::TimerBase::stop()
{
}

bool RunLoop::TimerBase::isActive() const
{
  return true;
}

} // namespace WTF
