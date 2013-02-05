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
package javafx.beans.value;

import javafx.beans.InvalidationListener;
import org.junit.Test;

import static org.junit.Assert.*;

public class WeakChangeListenerTest {
	
    @Test(expected=NullPointerException.class)
    public void testConstructWithNull() {
        new WeakChangeListener<Object>(null);
    }

    @Test
    public void testHandle() {
        ChangeListenerMock<Object> listener = new ChangeListenerMock<Object>(new Object());
        final WeakChangeListener<Object> weakListener = new WeakChangeListener<Object>(listener);
        final ObservableMock o = new ObservableMock();
        final Object obj1 = new Object();
        final Object obj2 = new Object();

        // regular call
        weakListener.changed(o, obj1, obj2);
        listener.check(o, obj1, obj2, 1);
        assertFalse(weakListener.wasGarbageCollected());

        // GC-ed call
        o.reset();
        listener = null;
        System.gc();
        assertTrue(weakListener.wasGarbageCollected());
        weakListener.changed(o, obj2, obj1);
        assertEquals(1, o.removeCounter);
    }

    private static class ObservableMock implements ObservableValue<Object> {
        private int removeCounter;

        private void reset() {
            removeCounter = 0;
        }

        @Override
        public Object getValue() {
            return null;
        }

		@Override
		public void addListener(InvalidationListener listener) {
			// not used
		}

		@Override
		public void addListener(ChangeListener<? super Object> listener) {
			// not used
		}

		@Override
		public void removeListener(InvalidationListener listener) {
			// not used
		}

		@Override
		public void removeListener(ChangeListener<? super Object> listener) {
            removeCounter++;
		}

    }

}