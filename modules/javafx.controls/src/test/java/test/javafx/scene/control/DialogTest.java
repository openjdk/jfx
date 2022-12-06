/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tk.Toolkit;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.StackPane;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** Tests for the {@link Dialog} class. */
public class DialogTest {

    private Dialog<ButtonType> dialog;

    @Before
    public void setUp() {
        dialog = new Dialog<>();
    }

    @After
    public void cleanUp() {
        // Set a dummy result so the dialog can be closed.
        dialog.setResult(new ButtonType(""));
        dialog.hide();
    }

    @Test
    public void testDialogMaxHeight() {
        int maxHeight = 400;

        StackPane stackPane = new StackPane();
        stackPane.setPrefHeight(700);

        dialog.getDialogPane().setContent(stackPane);
        dialog.getDialogPane().setMaxHeight(maxHeight);
        dialog.show();

        assertDialogPaneHeightEquals(maxHeight);

        assertEquals(maxHeight, dialog.getDialogPane().getMaxHeight(), 0);
    }

    @Test
    public void testDialogMinHeight() {
        int minHeight = 400;

        dialog.getDialogPane().setContent(new StackPane());
        dialog.getDialogPane().setMinHeight(minHeight);
        dialog.show();

        assertDialogPaneHeightEquals(minHeight);

        assertEquals(minHeight, dialog.getDialogPane().getMinHeight(), 0);
    }

    @Test
    public void testAccessibleRole() {
        assertEquals(AccessibleRole.DIALOG, dialog.getDialogPane().getAccessibleRole());
    }

    @Test
    public void testDialogPrefHeight() {
        int prefHeight = 400;

        dialog.getDialogPane().setContent(new StackPane());
        dialog.getDialogPane().setPrefHeight(prefHeight);
        dialog.show();

        assertDialogPaneHeightEquals(prefHeight);

        assertEquals(prefHeight, dialog.getDialogPane().getPrefHeight(), 0);
    }

    private void assertDialogPaneHeightEquals(int height) {
        Toolkit.getToolkit().firePulse();

        assertEquals(height, dialog.getDialogPane().getHeight(), 0);

        // Test the height after another layout pass.
        Toolkit.getToolkit().firePulse();

        assertEquals(height, dialog.getDialogPane().getHeight(), 0);
    }

}
