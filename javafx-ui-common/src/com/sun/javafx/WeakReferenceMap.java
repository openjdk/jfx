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

package com.sun.javafx;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This is a helper class for handling weak references across all devices.
 * We tried to use WeakHashMap, but it isn't available on mobile. We tried to
 * add it to mobile, but it requires ReferenceQueue and it appears that
 * ReferenceQueue requires support from the VM which we don't know that we
 * have on mobile. So this class attempts to lessen the likelihood of
 * memory leaks. It is similar to the WeakReferenceQueue class, but specifically
 * has a Map-like interface, even though it does not work based on hashes but
 * rather on iteration based lookup.
 * NOTE: THIS DOES NOT CONFORM TO MAP, BE CAREFUL WHAT YOU CALL!!
 */
public class WeakReferenceMap implements Map {
    ArrayList<Entry> queue = new ArrayList<Entry>();

    @SuppressWarnings("unchecked")
    public Object put(Object key, Object obj) {
        // remove the specific key rather than calling cleanup(), otherwise 
        // the entry won't get removed if key is still referenced. In other
        // words, in the remove function, the call to entry.weakKey.get() 
        // could return non-null for this key resulting in more than
        // one entry for this key in the queue.
        remove(key);
        Entry entry = new Entry();
        entry.weakKey = new WeakReference(key);
        entry.value = obj;
        queue.add(entry);
        return obj;
    }

    public int size() {
        cleanup();
        return queue.size();
    }

    public boolean isEmpty() {
        cleanup();
        return queue.isEmpty();
    }

    public Object remove(Object obj) {
        for (int i = queue.size() - 1; i >= 0; --i) {
            Entry entry = queue.get(i);
            Object other = entry.weakKey.get();
            if (other == null || other == obj) queue.remove(i);
        }
        return obj;
    }

    void cleanup() {
        remove(null);
    }

    public Object get(Object key) {
        for (int i = queue.size() - 1; i >= 0; --i) {
            Entry entry = queue.get(i);
            Object k = entry.weakKey.get();
            if (k == null) {
                queue.remove(i);
            } else {
                if (k.equals(key)) {
                    return entry.value;
                }
            }
        }
        return null;
    }

    public void clear() {
        queue.clear();
    }

    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Set keySet() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Collection values() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Set entrySet() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void putAll(Map m) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private static final class Entry {
        WeakReference weakKey;
        Object value;
    }
}
