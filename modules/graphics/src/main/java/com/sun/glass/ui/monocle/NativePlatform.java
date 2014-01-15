/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.input.InputDeviceRegistry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class NativePlatform {

    private static InputDeviceRegistry inputDeviceRegistry;
    protected final ExecutorService executor;
    private Thread executorThread;
    private NativeCursor cursor;
    private NativeScreen screen;

    protected NativePlatform() {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                executorThread = new Thread(r, "Monocle Application Thread");
                return executorThread;
            }
        });
        // Get the event thread started so that its thread exists when we ask
        // for it in getExecutorThread(). This causes newThread() to run
        // immediately, in the current thread. So once submit() returns,
        // executorThread has been initialized.
        executor.submit(new Runnable() {
            public void run() {
            }
        });
        assert(executorThread != null);
    }

    protected void shutdown() {
        executor.shutdown();
        if (cursor != null) {
            cursor.shutdown();
        }
        if (screen != null) {
            screen.shutdown();
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Thread getExecutorThread() {
        return executorThread;
    }

    public synchronized InputDeviceRegistry getInputDeviceRegistry() {
        if (inputDeviceRegistry == null) {
            inputDeviceRegistry = createInputDeviceRegistry();
        }
        return inputDeviceRegistry;
    }

    protected abstract InputDeviceRegistry createInputDeviceRegistry();

    protected abstract NativeCursor createCursor();

    public synchronized NativeCursor getCursor() {
        if (cursor == null) {
            cursor = createCursor();
        }
        return cursor;
    }

    protected abstract NativeScreen createScreen();

    public synchronized NativeScreen getScreen() {
        if (screen == null) {
            screen = createScreen();
        }
        return screen;
    }

}
