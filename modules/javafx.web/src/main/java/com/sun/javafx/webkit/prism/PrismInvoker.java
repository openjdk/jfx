/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.Invoker;

public final class PrismInvoker extends Invoker {

    private static final PlatformLogger log =
            PlatformLogger.getLogger(PrismInvoker.class.getName());

    public PrismInvoker() {
    }

    /*
     * No synchronization b/w Event (User) & Render threads is required
     * because FX synchronizes pulse and render operations itself.
     */
    @Override protected boolean lock(ReentrantLock lock) {
        return false;
    }

    @Override protected boolean unlock(ReentrantLock lock) {
        return false;
    }

    @Override protected boolean isEventThread() {
        return isEventThreadPrivate();
    }

    private static boolean isEventThreadPrivate() {
        return Toolkit.getToolkit().isFxUserThread();
    }

    @Override public void checkEventThread() {
        Toolkit.getToolkit().checkFxUserThread();
    }

    @Override public void invokeOnEventThread(final Runnable r) {
        if (isEventThread()) {
            r.run();
        } else {
            PlatformImpl.runLater(r);
        }
    }

    @Override public void postOnEventThread(final Runnable r) {
        PlatformImpl.runLater(r);
    }

    static void invokeOnRenderThread(final Runnable r) {
        Toolkit.getToolkit().addRenderJob(new RenderJob(r));
    }

    static void runOnRenderThread(final Runnable r) {
        if (Thread.currentThread().getName().startsWith("QuantumRenderer")) {
            r.run();
        } else {
            FutureTask<Void> f = new FutureTask<Void>(r, null);
            Toolkit.getToolkit().addRenderJob(new RenderJob(f));
            try {
                // block until job is complete
                f.get();
            } catch (ExecutionException | InterruptedException ex) {
                log.severe("RenderJob error", ex);
            }
        }
    }
}
