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

#include "common.h"

#include "Timer.h"

#include "com_sun_glass_ui_win_WinTimer.h"


UINT Timer::timersCount = 0;
UINT Timer::wTimerRes = 0;
TIMECAPS Timer::tc = {0, 0};

class RunnableTimer : public Timer {
    public:
        static jlong Start(jobject r, jint period)
        {
            try {
                return ptr_to_jlong(new RunnableTimer(r, period));
            } catch (...) {
                return 0;
            }
        }

        static void Stop(jlong timer)
        {
            delete (RunnableTimer*)jlong_to_ptr(timer);
        }

        RunnableTimer(jobject r, jint period) : env(NULL), runnable(r)
        {
            if (!start((UINT)period)) {
                throw Exception();
            }
        }

        virtual void TimerCallback()
        {
            GetEnv()->CallVoidMethod(runnable, javaIDs.Runnable.run);
            CheckAndClearException(GetEnv());
        }
    private:
        JNIEnv * env;
        JGlobalRef<jobject> runnable;

        JNIEnv * GetEnv()
        {
            if (!env) {
                // We never DetachCurrentThread() but that's OK. Even if a
                // thread is re-used, the Attach*() simply fills in the env.
                GetJVM()->AttachCurrentThreadAsDaemon((void**)&env, NULL);
            }
            return env;
        }
};

extern "C" {

/*
 * Class:     com_sun_glass_ui_win_WinTimer
 * Method:    _start
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_win_WinTimer__1start
  (JNIEnv * env, jobject jThis, jobject runnable, jint period)
{
    return RunnableTimer::Start(runnable, period);
}

/*
 * Class:     com_sun_glass_ui_win_WinTimer
 * Method:    _stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinTimer__1stop
  (JNIEnv * env, jobject jThis, jlong timer)
{
    RunnableTimer::Stop(timer);
}

/*
 * Class:     com_sun_glass_ui_win_WinTimer
 * Method:    _getMinPeriod
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinTimer__1getMinPeriod
  (JNIEnv * env, jclass cls)
{
    return Timer::GetMinPeriod();
}

/*
 * Class:     com_sun_glass_ui_win_WinTimer
 * Method:    _getMaxPeriod
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinTimer__1getMaxPeriod
  (JNIEnv * env, jclass cls)
{
    return Timer::GetMaxPeriod();
}

} //extern "C"

