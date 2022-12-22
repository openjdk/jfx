/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;

/**
 * This is a helper class for handling weak references across all devices.
 * We tried to use WeakHashMap, but it isn't available on mobile. We tried to
 * add it to mobile, but it requires ReferenceQueue and it appears that
 * ReferenceQueue requires support from the VM which we don't know that we
 * have on mobile. So this class attempts to lesson the likelyhood of
 * memory leaks.
 *
 * As we abandoned mobile, we considered removal of this class. But replacement
 * by WeakHashMap is not always possible as we use mutable elements. At least
 * it was now possible to optimize this class using the ReferenceQueue.
 */
public class WeakReferenceQueue<E> {
    /**
     * Reference queue for cleared weak references
     */
    private final ReferenceQueue garbage = new ReferenceQueue();

    /**
     * Strongly referenced list head
     */
    private Object strongRef = new Object();
    private ListEntry head = new ListEntry(strongRef, garbage);

    /**
     * Size of the queue
     */
    int size = 0;

    public void add(E obj) {
        cleanup();
        size++;
        new ListEntry(obj, garbage).insert(head.prev);
    }

    public void remove(E obj) {
        cleanup();

        ListEntry entry = head.next;
        while (entry != head) {
            Object other = entry.get();
            if (other == obj) {
                size--;
                entry.remove();
                return;
            }
            entry = entry.next;
        }
    }

    public void cleanup() {
        ListEntry entry;
        while ((entry = (ListEntry) garbage.poll()) != null) {
            size--;
            entry.remove();
        }
    }

    public Iterator<? super E> iterator() {
        return new Iterator() {
            private ListEntry index = head;
            private Object next = null;

            @Override
            public boolean hasNext() {
                next = null;
                while (next == null) {
                    ListEntry nextIndex = index.prev;
                    if (nextIndex == head) {
                        break;
                    }
                    next = nextIndex.get();
                    if (next == null) {
                        size--;
                        nextIndex.remove();
                    }
                }

                return next != null;
            }

            @Override
            public Object next() {
                hasNext(); // forces us to clear out crap up to the next
                           // valid spot
                index = index.prev;
                return next;
            }

            @Override
            public void remove() {
                if (index != head) {
                    ListEntry nextIndex = index.next;
                    size--;
                    index.remove();
                    index = nextIndex;
                }
            }
        };
    }

    private static class ListEntry extends WeakReference {
        ListEntry prev, next;

        public ListEntry(Object o, ReferenceQueue queue) {
            super(o, queue);
            prev = this;
            next = this;
        }

        public void insert(ListEntry where) {
            prev = where;
            next = where.next;
            where.next = this;
            next.prev = this;
        }

        public void remove() {
            prev.next = next;
            next.prev = prev;
            next = this;
            prev = this;
        }
    }

}
