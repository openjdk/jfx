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

package javafx.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.junit.Test;

public class WeakInvalidationListenerTest {
	
    @Test(expected=NullPointerException.class)
    public void testConstructWithNull() {
        new WeakInvalidationListener(null);
    }

    @Test
    public void testHandle() {
        InvalidationListenerMock listener = new InvalidationListenerMock();
        final WeakInvalidationListener weakListener = new WeakInvalidationListener(listener);
        final ObservableMock o = new ObservableMock();

        // regular call
        weakListener.invalidated(o);
        listener.check(o, 1);
        assertFalse(weakListener.wasGarbageCollected());

        // GC-ed call
        o.reset();
        listener = null;
        System.gc();
        assertTrue(weakListener.wasGarbageCollected());
        weakListener.invalidated(o);
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
            removeCounter++;
		}

		@Override
		public void removeListener(ChangeListener<? super Object> listener) {
			// not used
		}

    }

}