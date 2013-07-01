/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;

import java.util.concurrent.Callable;

final class FxEventLoop {

    private final static class Impl extends AbstractEventLoop {

        private final Object NESTED_EVENT_LOOP_KEY = new Object();

        private Object nestedEventLoopToken;
        private int depth;

        @Override
        public void send(final Runnable r) {
            if (Platform.isFxApplicationThread()) {
                r.run();
                return;
            }
            super.send(r);
        }

        @Override
        public <V> V send(final Callable<V> c) {
            if (Platform.isFxApplicationThread()) {
                try {
                    return c.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return super.send(c);
        }

        @Override
        public void start() {
            if (Platform.isFxApplicationThread()) {
                // Don't allow nested nested loops
                assert (depth == 0);

                ++depth;
                try {
                    nestedEventLoopToken = Toolkit.getToolkit().
                            enterNestedEventLoop(NESTED_EVENT_LOOP_KEY);
                } finally {
                    --depth;
                }
            } else {
                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {
                        enterNestedLoop();
                    }
                });
            }
        }

        @Override
        public void stop() {
            if (Platform.isFxApplicationThread()) {
                Toolkit.getToolkit().exitNestedEventLoop(NESTED_EVENT_LOOP_KEY,
                                                         nestedEventLoopToken);
                nestedEventLoopToken = null;
            } else {
                send(new Runnable() {

                    @Override
                    public void run() {
                        leaveNestedLoop();
                    }
                });
            }
        }

        @Override
        protected void schedule(Runnable r) {
            Platform.runLater(r);
        }

        boolean isNestedLoopRunning() {
            assert Platform.isFxApplicationThread();

            return (depth != 0);
        }
    }
    private final static Impl impl = new Impl();

    public static void enterNestedLoop() {
        impl.start();
    }

    public static void leaveNestedLoop() {
        impl.stop();
    }

    public static void sendEvent(final Runnable e) {
        impl.send(e);
    }

    public static <V> V sendEvent(final Callable<V> e) {
        return impl.send(e);
    }

    public static boolean isNestedLoopRunning() {
        return impl.isNestedLoopRunning();
    }
}
