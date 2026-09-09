/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests setting Tooltip on a Control
 */
public class ControlTooltipTest {
    private ControlStub control;
    private Tooltip tooltip;

    @BeforeEach
    public void setUp() {
        control = new ControlStub();
        tooltip = new Tooltip();
    }

    @Test public void controlHasNoTooltipByDefault() {
        assertNull(control.getTooltip());
    }

    @Test public void testAddingRemovingTooltipOnControl() {
        control.setTooltip(tooltip);
        assertSame(tooltip, control.getTooltip());

        control.setTooltip(null);
        assertNull(control.getTooltip());
    }

    @Test public void testAddingASecondTooltipOnControl() {
        control.setTooltip(tooltip);
        assertSame(tooltip, control.getTooltip());

        Tooltip tooltip1 = new Tooltip();
        control.setTooltip(tooltip1);
        assertSame(tooltip1, control.getTooltip());
    }

    @Test public void testTooltipInstallAndUninstallOnControl() {
        // Test Tooltip install
        Tooltip.install(control, tooltip);
        Node node = (Node) control;
        Tooltip temp = (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
        assertSame(tooltip, temp);

        // Test Tooltip uninstall
        Tooltip.uninstall(control, tooltip);
        temp = (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
        assertNull(temp);
    }

    @Test public void testTooltipInstallTwiceOnControl() {
        Tooltip.install(control, tooltip);
        Node node = (Node) control;
        Tooltip temp = (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
        assertSame(tooltip, temp);

        Tooltip tooltip1 = new Tooltip();
        Tooltip.install(control, tooltip1);

        temp = (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
        assertSame(tooltip1, temp);
    }
}
