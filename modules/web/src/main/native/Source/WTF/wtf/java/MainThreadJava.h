/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef MainThreadJava_h
#define MainThreadJava_h

#include "MainThread.h"
#include "Timer.h"

namespace WTF {

/*
 * The MainThreadJavaScheduler class is used to hold and manage Timer along with
 * the dispatching function.
 *
 * Timer is needed to schedule function invocation not only on Main Thread, but
 * in a dedicated time slot via SharedTimer invocation mechanism managed by the
 * platform (overall, this serves the purpose of throttling JS execution). 
 *
 * Timer is tight to the thread it has been created on. By its contract it should
 * be operated on the same thread (including its destruction). So, all the
 * MainThreadJavaScheduler methods are to be executed on Main Thread.
 *
 * A drawback of using Timer is that it inroduces a dependency on WebCore (as it
 * resides in WebCore/platform). Otherwise we would need to create another
 * ShreadTimer-like mechanism with an invocation queue on the Java side, which is
 * currently less preferable.
 */
class MainThreadJavaScheduler {
public:
    static MainThreadJavaScheduler* instance()
    {
        static MainThreadJavaScheduler inst;
        return &inst;
    }

    static void scheduleDispatch()
    {
        if (timer()) timer()->startOneShot(0);
    }

private:
    MainThreadJavaScheduler()
    {
        ASSERT(isMainThread());
        m_timer = new WebCore::Timer<MainThreadJavaScheduler>(this, &MainThreadJavaScheduler::dispatchOnMainThreadFired);
    }

    WebCore::Timer<MainThreadJavaScheduler>* m_timer;

    static WebCore::Timer<MainThreadJavaScheduler>* timer() { return instance()->m_timer; }

    void dispatchOnMainThreadFired(WebCore::Timer<MainThreadJavaScheduler>*)
    {
        dispatchFunctionsFromMainThread();
    }
};

}

#endif // MainThreadJava_h
