/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.MapExpressionHelper;
import com.sun.javafx.collections.ObservableMapWrapper;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class WeakMapChangeListenerTest {
	
    @Test(expected=NullPointerException.class)
    public void testConstructWithNull() {
        new WeakMapChangeListener<Object, Object>(null);
    }

    @Test
    public void testHandle() {
        MockMapObserver<Object, Object> listener = new MockMapObserver<Object, Object>();
        final WeakMapChangeListener<Object, Object> weakListener = new WeakMapChangeListener<Object, Object>(listener);
        final ObservableMapMock map = new ObservableMapMock();
        final Object key = new Object();
        final Object value = new Object();
        final MapChangeListener.Change<Object, Object> change = new MapExpressionHelper.SimpleChange<Object, Object>(map).setRemoved(key, value);

        // regular call
        weakListener.onChanged(change);
        listener.assertRemoved(MockMapObserver.Tuple.tup(key, value));
        assertFalse(weakListener.wasGarbageCollected());

        // GC-ed call
        map.reset();
        listener = null;
        System.gc();
        assertTrue(weakListener.wasGarbageCollected());
        weakListener.onChanged(change);
        assertEquals(1, map.removeCounter);
    }

    private static class ObservableMapMock extends ObservableMapWrapper<Object, Object> {
        private int removeCounter;

        public ObservableMapMock() {
            super(new HashMap<Object, Object>());
        }

        private void reset() {
            removeCounter = 0;
        }

        @Override
		public void removeListener(MapChangeListener<? super Object, ? super Object> listener) {
            removeCounter++;
		}
    }

}