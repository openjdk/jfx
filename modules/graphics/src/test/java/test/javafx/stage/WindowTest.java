/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertNotNull;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.collections.ObservableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.pgstub.StubStage;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.atomic.AtomicInteger;

public final class WindowTest {
    private StubToolkit toolkit;
    private Stage testWindow;

    @Before
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        testWindow = new Stage();
    }

    @After
    public void afterTest() {
        testWindow.hide();
        testWindow = null;
    }

    @Test
    public void testOpacityBind() {
        final DoubleProperty variable = new SimpleDoubleProperty(0.5);

        testWindow.show();
        final StubStage peer = getPeer(testWindow);

        testWindow.opacityProperty().bind(variable);
        toolkit.fireTestPulse();

        assertEquals(0.5f, peer.opacity);

        variable.set(1.0f);
        toolkit.fireTestPulse();

        assertEquals(1.0f, peer.opacity);
    }

    @Test public void testProperties() {
        javafx.collections.ObservableMap<Object, Object> properties = testWindow.getProperties();

        /* If we ask for it, we should get it.
         */
        assertNotNull(properties);

        /* What we put in, we should get out.
         */
        properties.put("MyKey", "MyValue");
        assertEquals("MyValue", properties.get("MyKey"));

        /* If we ask for it again, we should get the same thing.
         */
        javafx.collections.ObservableMap<Object, Object> properties2 = testWindow.getProperties();
        assertEquals(properties2, properties);

        /* What we put in to the other one, we should get out of this one because
         * they should be the same thing.
         */
        assertEquals("MyValue", properties2.get("MyKey"));
    }

    private static StubStage getPeer(final Window window) {
        final TKStage unkPeer = window.impl_getPeer();
        assertTrue(unkPeer instanceof StubStage);
        return (StubStage) unkPeer;
    }

    @Test public void testGetWindowsIsObservable() {
        ObservableList<Window> windows = Window.getWindows();

        final int initialWindowCount = windows.size();
        AtomicInteger windowCount = new AtomicInteger(initialWindowCount);

        InvalidationListener listener = o -> windowCount.set(windows.size());
        windows.addListener(listener);

        assertEquals(initialWindowCount + 0, windowCount.get());

        testWindow.show();
        assertEquals(initialWindowCount + 1, windowCount.get());

        Stage anotherTestWindow = new Stage();
        anotherTestWindow.show();
        assertEquals(initialWindowCount + 2, windowCount.get());

        testWindow.hide();
        assertEquals(initialWindowCount + 1, windowCount.get());

        anotherTestWindow.hide();
        assertEquals(initialWindowCount + 0, windowCount.get());

        windows.removeListener(listener);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetWindowsIsUnmodifiable_add() {
        Stage anotherTestWindow = new Stage();
        Window.getWindows().add(anotherTestWindow);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetWindowsIsUnmodifiable_removeShowingWindow() {
        testWindow.show();
        Window.getWindows().remove(testWindow);
    }

    // There is no UOE here because the window being removed is not in the list of windows,
    // so no modification of the windows list occurs.
    @Test public void testGetWindowsIsUnmodifiable_removeNonShowingWindow_emptyList() {
        Stage anotherTestWindow = new Stage();
        Window.getWindows().remove(anotherTestWindow);
    }

    // There is no UOE here because the window being removed is not in the list of windows,
    // so no modification of the windows list occurs.
    @Test public void testGetWindowsIsUnmodifiable_removeNonShowingWindow_nonEmptyList() {
        ObservableList<Window> windows = Window.getWindows();

        final int initialWindowCount = windows.size();

        testWindow.show();
        assertEquals(initialWindowCount + 1, windows.size());

        Stage anotherTestWindow = new Stage();
        assertEquals(initialWindowCount + 1, windows.size());

        windows.remove(anotherTestWindow);
        assertEquals(initialWindowCount + 1, windows.size());
    }
}
