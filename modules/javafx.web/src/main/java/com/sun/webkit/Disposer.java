/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.webkit;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is used for registering and disposing the native
 * data associated with java objects.
 *
 * The object can register itself by calling the addRecord method and
 * providing a descendant of the DisposerRecord class with overridden
 * dispose() method.
 *
 * When the object becomes unreachable, the dispose() method
 * of the associated DisposerRecord object will be called.
 *
 * @see DisposerRecord
 */
public final class Disposer implements Runnable {
    private static final ReferenceQueue queue = new ReferenceQueue();
    private static final Disposer disposerInstance = new Disposer();
    private static final Set<WeakDisposerRecord> records =
            new HashSet<WeakDisposerRecord>();

    static {
        java.security.AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            /*
             * The thread must be a member of a thread group
             * which will not get GCed before VM exit.
             * Make its parent the top-level thread group.
             */
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            for (ThreadGroup tgn = tg;
                    tgn != null;
                    tg = tgn, tgn = tg.getParent());

            Thread t = new Thread(tg, disposerInstance, "Disposer");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
            return null;
        });
    }

    /**
     * Registers the object and the native data for later disposal.
     * @param target Object to be registered
     * @param rec the associated DisposerRecord object
     * @see DisposerRecord
     */
    public static void addRecord(Object target, DisposerRecord rec) {
        disposerInstance.add(target, rec);
    }

    /**
     * Performs the actual registration of the target object to be disposed.
     * @param target Object to be registered
     * @param rec the associated DisposerRecord object
     * @see DisposerRecord
     */
    private synchronized void add(Object target, DisposerRecord rec) {
        records.add(new WeakDisposerRecord(target, rec));
    }

    /**
     * Registers the WeakDisposerRecord for later disposal.
     * @param rec the associated DisposerRecord object
     * @see DisposerRecord
     */
    public static void addRecord(WeakDisposerRecord rec) {
        disposerInstance.add(rec);
    }

    /**
     * Performs the actual registration of the WeakDisposerRecord be disposed.
     * @param rec the WeakDisposerRecord object
     * @see DisposerRecord
     */
    private synchronized void add(WeakDisposerRecord rec) {
        records.add(rec);
    }

    public void run() {
        while (true) {
            try {
                WeakDisposerRecord obj = (WeakDisposerRecord) queue.remove();
                obj.clear();
                DisposerRunnable.getInstance().enqueue(obj);
            } catch (Exception e) {
                System.out.println("Exception while removing reference: " + e);
                e.printStackTrace();
            }
        }
    }

    private static final class DisposerRunnable implements Runnable {
        private static final DisposerRunnable theInstance = new DisposerRunnable();

        private static DisposerRunnable getInstance() {
            return theInstance;
        }

        private boolean isRunning = false;
        private final Object disposerLock = new Object();
        private final LinkedBlockingQueue<WeakDisposerRecord> disposerQueue
                = new LinkedBlockingQueue<WeakDisposerRecord>();

        private void enqueueAll(Collection<WeakDisposerRecord> objs) {
            synchronized (disposerLock) {
                disposerQueue.addAll(objs);
                if (!isRunning) {
                    Invoker.getInvoker().invokeOnEventThread(this);
                    isRunning = true;
                }
            }
        }

        private void enqueue(WeakDisposerRecord obj) {
            enqueueAll(Arrays.asList(obj));
        }

        @Override public void run() {
            while (true) {
                WeakDisposerRecord obj;
                synchronized (disposerLock) {
                    obj = disposerQueue.poll();
                    if (obj == null) {
                        isRunning = false;
                        break;
                    }
                }
                // Check if the object has not yet been removed & disposed.
                if (records.contains(obj)) {
                    records.remove(obj);
                    obj.dispose();
                }
            }
        }
    }

    public static class WeakDisposerRecord
        extends WeakReference
        implements DisposerRecord
    {
        protected WeakDisposerRecord(Object referent) {
            super(referent, Disposer.queue);
            this.record = null;
        }

        private WeakDisposerRecord(Object referent, DisposerRecord record) {
            super(referent, Disposer.queue);
            this.record = record;
        }

        private final DisposerRecord record;

        @Override
        public void dispose() {
            record.dispose();
        }
    }
}
