/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import com.sun.javafx.scene.SceneHelper;
import javafx.collections.ObservableMap;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.Group;
import javafx.scene.Scene;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AcceleratorsTest {

    private Scene scene;
    private ObservableMap<KeyCombination, Runnable> accelerators;

    @BeforeEach
    public void setUp() {
        scene = new Scene(new Group());
        accelerators = scene.getAccelerators();
    }


    @Test
    public void testAcceleratorExecuted() {
        AtomicBoolean executed = new AtomicBoolean();
        accelerators.put(KeyCombination.keyCombination("Alt + A"), () -> executed.set(true));
        SceneHelper.processKeyEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "A", "A", KeyCode.A, false, false, true, false));
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
        SceneHelper.processKeyEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "A", "A", KeyCode.A, false, false, true, false));
        assertEquals(1, accelerators.size());
        assertFalse(executed.get());
        SceneHelper.processKeyEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED, "B", "B", KeyCode.B, false, false, true, false));
        assertTrue(executed.get());
    }

    @Test
    public void testAcceleratorComodification() {
        assertThrows(ConcurrentModificationException.class, () -> {
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
        });
    }
}
