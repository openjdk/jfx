/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
