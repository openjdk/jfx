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

package javafx.stage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.pgstub.StubStage;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.Toolkit;

public final class WindowTest {
    private StubToolkit toolkit;
    private Stage testWindow;

    @Before
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        testWindow = new Stage();
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

    private static StubStage getPeer(final Window window) {
        final TKStage unkPeer = window.impl_getPeer();
        assertTrue(unkPeer instanceof StubStage);
        return (StubStage) unkPeer;
    }
}
