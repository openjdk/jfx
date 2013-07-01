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

package javafx.concurrent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import org.junit.Before;

/**
 * Base class for tests of the Service class. This class has built into it
 * the notion of an event queue, and executing a Service on a background
 * thread. It handles draining the EQ of events and so forth.
 */
public abstract class ServiceTestBase {
    protected final ConcurrentLinkedQueue<Runnable> eventQueue =
            new ConcurrentLinkedQueue<Runnable>();
    protected TestServiceFactory factory;
    protected Service<String> service;
    
    protected abstract TestServiceFactory setupServiceFactory();
    protected Executor createExecutor() {
        return new Executor() {
            @Override public void execute(final Runnable command) {
                if (command == null) Thread.dumpStack();
                Thread th = new Thread() {
                    @Override public void run() {
                        try {
                            command.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            eventQueue.add(new Sentinel());
                        }
                    }
                };
                th.setDaemon(true);
                th.start();
            }
        };
    }
    
    @Before public void setup() {
        factory = setupServiceFactory();
        factory.test = this;
        service = factory.createService();
        service.setExecutor(createExecutor());
    }

    public void handleEvents() {
        Runnable r;
        do {
            r = eventQueue.poll();
            if (r != null) r.run();
        } while (r == null || !(r instanceof Sentinel));
    }

    // This is a sentinel class, indicating that the test is done (no more events in the event queue)
    public static final class Sentinel implements Runnable {
        @Override public void run() { }
    }
}
