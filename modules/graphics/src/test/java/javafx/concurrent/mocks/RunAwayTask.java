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

package javafx.concurrent.mocks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.concurrent.AbstractTask;

/**
 * A Task which will simply loop forever without end. This is used to simulate
 * what happens when a task doesn't head the "isCancelled" flag or
 * cancellation in any way. To make sure it finally does terminate (so the
 * tests will work) there is a <code>stopLooping</code> atomic boolean that
 * the test needs to set to true.
 *
 * <p>To make sure that a single loop has occurred before we quit, there is
 * a <code>loopHasHappened</code> boolean. This way we always know that
 * a single iteration has completed, at least, before the task was
 * terminated.</p>
 *
 * <p>Different tests want to use a RunAwayTask differently. Some want to
 * call updateValue in the body of the loop, while others do not. For this
 * reason, the RunAwayTask is abstract and an abstract <code>loop</code>
 * method is defined that a subclass implements to implement the body
 * of the loop.</p>
 */
public abstract class RunAwayTask extends AbstractTask {
    public AtomicBoolean stopLooping = new AtomicBoolean(false);
    private boolean loopHasHappened = false;

    @Override protected String call() throws Exception {
        int count = 0;
        while (!loopHasHappened || !stopLooping.get()) {
            count++;
            loop(count);
            loopHasHappened = true;
        }
        return "" + count;
    }

    protected abstract void loop(int count) throws Exception;
    
}
