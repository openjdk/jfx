/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit;

import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.EventLoop;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

public final class EventLoopImpl extends EventLoop {

    private static final long DELAY = 20;

    private static final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();


    @Override
    protected void cycle() {
        // Here we need to execute one or a small number of event loop cycles.
        // In order to achive that, we enter and quickly exit a nested event
        // loop. The request to exit the nested event loop is submitted with
        // a short delay so as to throttle the outer
        // ScriptDebugServer::pauseIfNeeded loop.
        final Object key = new Object();
        executor.schedule(() -> {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Toolkit.getToolkit().exitNestedEventLoop(key, null);
                }
            });
        }, DELAY, TimeUnit.MILLISECONDS);
        Toolkit.getToolkit().enterNestedEventLoop(key);
    }
}
