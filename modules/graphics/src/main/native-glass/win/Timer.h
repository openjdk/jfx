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

#ifndef _TIMER_H
#define _TIMER_H


class Timer {
    public:
        static UINT GetMinPeriod()
        {
            InitTC();
            return tc.wPeriodMin;
        }

        static UINT GetMaxPeriod()
        {
            InitTC();
            return tc.wPeriodMax;
        }

        class Exception {};
        
        Timer() : id(0)
        {
            if (++timersCount == 1) {
                if (!InitTC()) {
                    throw Exception();
                }

                ::timeBeginPeriod(wTimerRes);
            }
        }

        virtual ~Timer()
        {
            if (id) {
                ::timeKillEvent(id);
            }
            if (--timersCount == 0 && wTimerRes != 0) {
                ::timeEndPeriod(wTimerRes);
            }
        }

        bool start(UINT period) {
            // MSDN suggests to use CreateTimerQueueTimer instead, but people
            // on the internets say is provides less accurate timers, so
            // let's use timeSetEvent.
            id = ::timeSetEvent(period, wTimerRes, StaticTimeCallback,
                    (DWORD_PTR)this, TIME_PERIODIC);

            return id != 0;
        }

        virtual void TimerCallback() = 0;

    private:
        static bool InitTC()
        {
            // Not quite thread-safe, but shouldn't hurt since we just retrieve
            // some constant values
            if (!tc.wPeriodMin && !tc.wPeriodMax) {
                if (::timeGetDevCaps(&tc, sizeof(tc)) != TIMERR_NOERROR) {
                    tc.wPeriodMin = tc.wPeriodMax = 0;
                    return false;
                } else {
                    // We want 1 ms accuracy
                    wTimerRes = min(max(tc.wPeriodMin, 1), tc.wPeriodMax);
                }
            }
            return true;
        }


        static void CALLBACK StaticTimeCallback(UINT uTimerID, UINT uMsg, DWORD_PTR dwUser, DWORD_PTR dw1, DWORD_PTR dw2)
        {
            ((Timer*)dwUser)->TimerCallback();
        }

        static UINT timersCount;
        static UINT wTimerRes;
        static TIMECAPS tc;

        UINT id;
};

#endif //_TIMER_H

