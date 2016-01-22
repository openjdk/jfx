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

package com.sun.javafx.tk;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/*
 * RenderJob for the Quantum toolkit.
 */
public class RenderJob extends FutureTask {

    private CompletionListener listener;
    private Object             futureReturn;

    public RenderJob(Runnable pen) {
        super(pen, null);
    }

    public RenderJob(Runnable pen, CompletionListener cl) {
        super(pen, null);
        setCompletionListener(cl);
    }

    public CompletionListener getCompletionListener() {
        return listener;
    }

    public void setCompletionListener(CompletionListener cl) {
        listener = cl;
    }

    @Override public void run() {
        if (super.runAndReset() == false) {
            // if (PrismSettings.verbose) {
                try {
                    Object value = super.get();
                    System.err.println("RenderJob.run: failed no exception: " + value);
                } catch (CancellationException ce) {
                    System.err.println("RenderJob.run: task cancelled");
                } catch (ExecutionException ee) {
                    System.err.println("RenderJob.run: internal exception");
                    ee.getCause().printStackTrace();
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            /* } else {
                throw new IllegalArgumentException("RenderJob run failed");
            } */
        } else {
            if (listener != null) {
                try {
                    listener.done(this);
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }
    }

    @Override public Object get() {
        return (futureReturn);
    }

    public void setFutureReturn(Object o) {
        futureReturn = o;
    }
}
