/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;

/**
 * This class is used for cleaning up weak listener stubs registered by the
 * various *PropertyBase classes.
 *
 * A Cleaner is not used here as calling {@link Observable#removeListener(InvalidationListener)}
 * cannot be guaranteed to be executed very quick and not to block (as per Cleaner requirements).
 */
public class ListenerRemover {

    private static final ReferenceQueue<?> QUEUE = new ReferenceQueue<>();

    /**
     * When ref becomes weakly reachable, remove weakListener from target.
     *
     * @param <T> the type of reference
     * @param <L> the listener type
     * @param ref an object to monitor for weak reachability, cannot be {@code null}
     * @param weakListener a listener implementing {@link WeakListener} and {@link InvalidationListener}, cannot be {@code null}
     * @param target an {@link Observable} from which to remove the listener, cannot be {@code null}
     * @return a {@link WeakReference}, never {@code null}
     */
    public static final <T, L extends InvalidationListener & WeakListener> WeakReference<T> whenWeaklyReachable(T ref, L weakListener, Observable target) {
        return new TrackingWeakReference<>(
            Objects.requireNonNull(ref, "ref"),
            Objects.requireNonNull(weakListener, "weakListener"),
            Objects.requireNonNull(target, "target")
        );
    }

    private static class TrackingWeakReference<T> extends WeakReference<T> {
        final InvalidationListener weakListener;
        final Observable target;

        public TrackingWeakReference(T referent, InvalidationListener weakListener, Observable target) {
            super(referent, queue());

            this.weakListener = weakListener;
            this.target = target;
        }
    }

    @SuppressWarnings("unchecked")
    private static final <T> ReferenceQueue<T> queue() {
        return (ReferenceQueue<T>)QUEUE;
    }

    static {
        AccessController.doPrivileged(
            new PrivilegedAction<Object>() {
                @Override
                public Object run() {

                    /*
                     * The thread must be a member of a thread group
                     * which will not get GCed before VM exit.
                     * Make its parent the top-level thread group.
                     */

                    ThreadGroup tg = Thread.currentThread().getThreadGroup();

                    for (ThreadGroup tgn = tg;
                         tgn != null;
                         tg = tgn, tgn = tg.getParent());

                    Thread t = new Thread(tg, ListenerRemover::run, "Listener Stub Disposer");

                    t.setContextClassLoader(null);
                    t.setDaemon(true);
                    t.setPriority(Thread.MAX_PRIORITY);
                    t.start();

                    return null;
                }
            }
        );
    }

    private static void run() {
        while (true) {
            try {
                TrackingWeakReference<?> ref = (TrackingWeakReference<?>) QUEUE.remove();

                ref.target.removeListener(ref.weakListener);
            } catch (Exception e) {
                System.out.println("Exception while removing weak listener stub: " + e);
                e.printStackTrace();
            }
        }
    }
}
