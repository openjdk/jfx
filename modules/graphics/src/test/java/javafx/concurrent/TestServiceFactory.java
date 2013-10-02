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

package javafx.concurrent;

/**
 * The Task and Service tests need to do a little trickery due to threading.
 * Although in practice they run on different threads (ie: the UI and
 * background threads), while testing I want to be able to run predictably
 * and quickly on just the test thread. This implementation of
 * Service is used as the base implementation of all Services which are
 * tested. It will simply run the given command immediately, and makes
 * sure the checkThread does nothing.
 */
public abstract class TestServiceFactory {
    public final Thread appThread = Thread.currentThread();
    public ServiceTestBase test;
    protected AbstractTask currentTask;

    protected abstract AbstractTask createTestTask();
    protected Service<String> createService() {
        return new Service<String>() {
            @Override protected Task<String> createTask() {
                currentTask = createTestTask();
                currentTask.test = test;
                currentTask.appThread = appThread;
                return currentTask;
            }

            @Override boolean isFxApplicationThread() {
                return Thread.currentThread() == appThread;
            }

            @Override void runLater(Runnable r) {
                if (test != null) {
                    test.eventQueue.add(r);
                } else {
                    r.run();
                }
            }
        };
    }

    public final AbstractTask getCurrentTask() { return currentTask; }
}
