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

package javafx.collections;

import com.sun.javafx.collections.NonIterableChange;
import com.sun.javafx.collections.ObservableListWrapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;

public class WeakListChangeListenerTest {
	
    @Test(expected=NullPointerException.class)
    public void testConstructWithNull() {
        new WeakListChangeListener<Object>(null);
    }

    @Test
    public void testHandle() {
        MockListObserver<Object> listener = new MockListObserver<Object>();
        final WeakListChangeListener<Object> weakListener = new WeakListChangeListener<Object>(listener);
        final ObservableListWrapper<Object> list = new ObservableListWrapper<Object>(new ArrayList<Object>());
        final Object removedElement = new Object();
        final ListChangeListener.Change<Object> change = new NonIterableChange.SimpleRemovedChange<Object>(0, 1, removedElement, list);

        // regular call
        weakListener.onChanged(change);
        listener.check1AddRemove(list, Collections.singletonList(removedElement), 0, 1);
        assertFalse(weakListener.wasGarbageCollected());

        // GC-ed call
        listener = null;
        System.gc();
        assertTrue(weakListener.wasGarbageCollected());
        weakListener.onChanged(change);

    }

}
