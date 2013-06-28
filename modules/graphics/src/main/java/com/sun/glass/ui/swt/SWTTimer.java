/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;

import com.sun.glass.ui.Timer;

final class SWTTimer extends Timer implements Runnable {
    Runnable timerRunnable;
    int period = 16;
    static final boolean THREAD_TIMER = System.getProperty("glass.swt.threadtimer") != null;
    
    protected SWTTimer(Runnable runnable) {
        super(runnable);
    }

    @Override protected long _start(Runnable runnable) {
        return 1;
    };
    
    @Override
    protected long _start(final Runnable runnable, final int period) {
        //TODO - timing bug when start/stop timer (shared state)
        //TODO - stop old timer before starting a new one
        this.period = period;
        if (THREAD_TIMER) {
            timerRunnable = runnable;
            new Thread(this).start();
            return 1;
        }
        final Display display = Display.getDefault();
        timerRunnable = new Runnable() {
            public void run() {
                runnable.run();
                display.timerExec(period, this);
            };
        };
        display.asyncExec(new Runnable () {
            public void run() {
                display.timerExec(period, timerRunnable);
                display.addListener(SWT.Dispose, new Listener () {
                    public void handleEvent (Event e) {
                        if (timerRunnable == null) return;
                        display.timerExec(-1, timerRunnable);
                        timerRunnable = null;
                    }
                });
            }
        });
        return 1;
    }

    @Override
    protected void _stop(long timer) {
        //TODO - timing bug when start/stop timer (shared state)
        if (timerRunnable == null) return;
        if (THREAD_TIMER) {
            timerRunnable = null;
            return;
        }
        final Display display = Display.getDefault();
        display.asyncExec(new Runnable () {
            public void run() {
                if (timerRunnable == null) return;
                display.timerExec(-1, timerRunnable);
                timerRunnable = null;
            }
        });
    }

    public void run() {
        while (timerRunnable != null) {
            long startTime = System.currentTimeMillis();
            timerRunnable.run();
            long sleepTime = period - (System.currentTimeMillis() - startTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) { }
            }
        }
    }
}

