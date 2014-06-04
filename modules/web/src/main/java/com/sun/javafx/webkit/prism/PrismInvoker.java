/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.Invoker;

public final class PrismInvoker extends Invoker {

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

    @Override protected void checkEventThread() {
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
            } catch (ExecutionException ex) {
                throw new AssertionError(ex);
            } catch (InterruptedException ex) {
                // ignore; recovery is impossible
            }
        }
    }
}
