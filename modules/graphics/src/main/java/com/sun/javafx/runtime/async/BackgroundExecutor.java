/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.runtime.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ExecutorSingleton
 *
 */
public class BackgroundExecutor {
    private static ExecutorService instance;
    private static ScheduledExecutorService timerInstance;

    private BackgroundExecutor() {
        // not instantiable
    }

    public static synchronized ExecutorService getExecutor() {
        if (instance == null) {
            instance = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
            ((ThreadPoolExecutor) instance).setKeepAliveTime(1, TimeUnit.SECONDS);
        }

        return instance;
    }

    public static synchronized ScheduledExecutorService getTimer() {
        if (timerInstance == null) {
            // @@@ Here's where we load the configuration and such
            timerInstance = new ScheduledThreadPoolExecutor(1,
                    r -> {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        return t;
                    }
            );
        }

        return timerInstance;
    }

    private static synchronized void shutdown() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
        if (timerInstance != null) {
            timerInstance.shutdown();
            timerInstance= null;
        }
    }
}
