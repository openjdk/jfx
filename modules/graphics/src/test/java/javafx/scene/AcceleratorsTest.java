/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.test.MouseEventGenerator;
import javafx.collections.ObservableMap;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AcceleratorsTest {

    private Scene scene;
    private ObservableMap<KeyCombination, Runnable> accelerators;

    @Before
    public void setUp() {
        scene = new Scene(new Group());
        accelerators = scene.getAccelerators();
    }


    @Test
    public void testAcceleratorExecuted() {
        AtomicBoolean executed = new AtomicBoolean();
        accelerators.put(KeyCombination.keyCombination("Alt + A"), () -> executed.set(true));
        scene.impl_processKeyEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "A", "A", KeyCode.A, false, false, true, false));
        assertTrue(executed.get());
    }

    @Test
    public void testAcceleratorRemovedWhenExecuted() {
        AtomicBoolean executed = new AtomicBoolean();
        final KeyCombination altA = KeyCombination.keyCombination("Alt + A");
        final KeyCombination altB = KeyCombination.keyCombination("Alt + B");
        accelerators.put(altA, () -> accelerators.remove(altA));
        accelerators.put(altB, () -> executed.set(true));
        assertEquals(2, accelerators.size());
        scene.impl_processKeyEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "A", "A", KeyCode.A, false, false, true, false));
        assertEquals(1, accelerators.size());
        assertFalse(executed.get());
        scene.impl_processKeyEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "B", "B", KeyCode.B, false, false, true, false));
        assertTrue(executed.get());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testAcceleratorComodification() {
        final KeyCombination altA = KeyCombination.keyCombination("Alt + A");
        final KeyCombination altB = KeyCombination.keyCombination("Alt + B");
        accelerators.put(altA, () -> {
        });
        accelerators.put(altB, () -> {
        });

        final Iterator<Map.Entry<KeyCombination, Runnable>> iterator = accelerators.entrySet().iterator();
        iterator.next();

        final Iterator<Map.Entry<KeyCombination, Runnable>> iterator1 = accelerators.entrySet().iterator();
        iterator1.next();
        iterator1.remove();

        iterator.next();
    }
}
