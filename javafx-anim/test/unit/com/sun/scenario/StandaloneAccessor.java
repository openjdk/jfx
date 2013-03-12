/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.scenario;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.scenario.animation.AbstractMasterTimer;

/**
 * This class is used in case Toolkit is not present. Usual use case is animaton
 * testing for openjfx-compiler.
 */
public class StandaloneAccessor extends ToolkitAccessor {
    private final Map<Object, Object> map = new HashMap<Object, Object>();
    private final ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactory() {
                private final ThreadFactory factory = Executors
                        .defaultThreadFactory();

                public Thread newThread(Runnable r) {
                    Thread thread = factory.newThread(r);
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private AtomicReference<Future<?>> refFuture = new AtomicReference<Future<?>>();
    private StandaloneMasterTimer standaloneMasterTimer = new StandaloneMasterTimer();

    @Override
    public Map<Object, Object> getContextMapImpl() {
        return map;
    }

    @Override
    public AbstractMasterTimer getMasterTimerImpl() {
        return standaloneMasterTimer;
    }

    private class StandaloneMasterTimer extends AbstractMasterTimer {
        protected StandaloneMasterTimer() {
            super(true);
        }

        @Override
        protected void postUpdateAnimationRunnable(
                final DelayedRunnable animationRunnable) {
            if (animationRunnable == null) {
                Future<?> future = refFuture.get();
                if (future != null) {
                    future.cancel(false);
                    refFuture.set(null);
                }
                return;
            }
            if (refFuture.get() != null) {
                return;
            }
            Future<?> future = executor.schedule(new Runnable() {
                public void run() {
                    refFuture.set(null);
                    animationRunnable.run();
                }
            }, animationRunnable.getDelay(), TimeUnit.MILLISECONDS);
            refFuture.set(future);
        }

        @Override
        protected int getPulseDuration(int precision) {
            int retVal = precision / 60;
            return retVal;
        }
    }
}
