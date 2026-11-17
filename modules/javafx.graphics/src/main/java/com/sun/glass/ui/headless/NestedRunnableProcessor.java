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

import com.sun.glass.ui.Application;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class NestedRunnableProcessor implements Runnable {

    private final LinkedList<RunLoopEntry> activeRunLoops = new LinkedList<>();

    private final BlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        newRunLoop();
    }

    void invokeLater(final Runnable r) {
        runnableQueue.add(r);
    }

    void invokeAndWait(final Runnable r) {
        final CountDownLatch latch = new CountDownLatch(1);
        runnableQueue.add(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Application.reportException(e);
        }
    }

    void stopProcessing() {
        for (RunLoopEntry entry : activeRunLoops) {
            runnableQueue.add(() -> entry.active = false);
        }
    }

    public Object newRunLoop() {
        RunLoopEntry entry = new RunLoopEntry();

        activeRunLoops.push(entry);

        entry.active = true;
        while (entry.active) {
            try {
                runnableQueue.take().run();
            } catch (Throwable e) {
                Application.reportException(e);
            }
        }
        return entry.returnValue;
    }

    public void leaveCurrentLoop(Object returnValue) {
        RunLoopEntry entry = activeRunLoops.pop();
        entry.active = false;
        entry.returnValue = returnValue;
    }

    private static class RunLoopEntry {

        // This is only accessed on the event thread
        boolean active;

        Object returnValue;
    }

}
