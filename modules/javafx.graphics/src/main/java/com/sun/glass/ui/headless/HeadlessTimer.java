/*
 * Copyright (c) 2025, Gluon. All rights reserved.
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
package com.sun.glass.ui.headless;

import com.sun.glass.ui.Timer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HeadlessTimer extends Timer {

    private static ScheduledThreadPoolExecutor scheduler;
    private ScheduledFuture<?> task;

    HeadlessTimer(final Runnable runnable) {
        super(runnable);
    }

    @Override
    protected long _start(Runnable runnable) {
        throw new RuntimeException("vsync timer not supported");
    }

    @Override
    protected long _start(Runnable runnable, int period) {
        if (scheduler == null) {
            scheduler = new ScheduledThreadPoolExecutor(1, target -> {
                Thread thread = new Thread(target, "Headless Timer");
                thread.setDaemon(true);
                return thread;
            });
        }
        task = scheduler.scheduleAtFixedRate(runnable, 0, period, TimeUnit.MILLISECONDS);
        return 1;

    }

    @Override
    protected void _stop(long timer) {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    @Override
    protected void _pause(long timer) {
    }

    @Override
    protected void _resume(long timer) {
    }

}
