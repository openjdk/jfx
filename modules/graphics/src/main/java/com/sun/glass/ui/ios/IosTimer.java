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

package com.sun.glass.ui.ios;

import com.sun.glass.ui.Timer;

/**
 * iOS platform implementation class for Timer.
 */
final class IosTimer extends Timer implements Runnable {
    private Thread timerThread;
    private Runnable timerRunnable;
    private long timerPeriod;

    protected IosTimer(Runnable runnable) {
        super(runnable);
    }

    @Override native protected long _start(Runnable runnable);

    native protected void _stopVsyncTimer(long timer);

    @Override
    protected long _start(Runnable runnable, int period) {
        timerThread = new Thread(this);
        timerRunnable = runnable;
        timerPeriod = period;
        timerThread.start();
        return timerThread.hashCode();
    }

    @Override
    protected void _stop(long timer) {
        if (timerThread != null ) {
            Thread t = timerThread;
            timerThread = null;
            try {
                t.join();
            } catch (InterruptedException e) { }
        } else {
            _stopVsyncTimer(timer);
        }
    }

    static int getMinPeriod_impl() {
        return 0;
    }

    static int getMaxPeriod_impl() {
        return 1000000;
    }

    /**
     * inheritDoc
     */
    @Override
    public void run() {
        Thread t = Thread.currentThread();
        long start;
        long sleepTime;
        while (t == timerThread) {
            start = System.currentTimeMillis();
            timerRunnable.run();
            sleepTime = timerPeriod - (System.currentTimeMillis() - start);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) { }
            }
        }
    }
}

