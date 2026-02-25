/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.layout;

import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class HeaderButtonBehaviorTest {

    enum ButtonDisabledStateTest {
        RESIZABLE(true, false, false, false, false),
        UNRESIZABLE(false, false, false, false, true),
        MODAL_RESIZABLE(true, true, false, true, false),
        MODAL_UNRESIZABLE(false, true, false, true, true),
        OWNER_RESIZABLE(true, false, true, true, false),
        OWNER_UNRESIZABLE(false, false, true, true, true),
        OWNER_MODAL_RESIZABLE(true, true, true, true, false),
        OWNER_MODAL_UNRESIZABLE(false, true, true, true, true);

        ButtonDisabledStateTest(boolean resizable, boolean modal, boolean hasOwner,
                                boolean iconifyDisabled, boolean maximizeDisabled) {
            this.resizable = resizable;
            this.modal = modal;
            this.hasOwner = hasOwner;
            this.iconifyDisabled = iconifyDisabled;
            this.maximizeDisabled = maximizeDisabled;
        }

        final boolean resizable;
        final boolean modal;
        final boolean hasOwner;
        final boolean iconifyDisabled;
        final boolean maximizeDisabled;
    }

    /**
     * Tests the disabled states of the iconify and maximize buttons for all combinations
     * of resizable, modal, and owner window attributes.
     */
    @ParameterizedTest
    @EnumSource(ButtonDisabledStateTest.class)
    void buttonDisabledStateIsCorrect(ButtonDisabledStateTest test) {
        Node iconify = new Group(), maximize = new Group(), close = new Group();
        HeaderBar.setButtonType(iconify, HeaderButtonType.ICONIFY);
        HeaderBar.setButtonType(maximize, HeaderButtonType.MAXIMIZE);
        HeaderBar.setButtonType(close, HeaderButtonType.CLOSE);

        var stage = new Stage();
        stage.initModality(test.modal ? Modality.WINDOW_MODAL : Modality.NONE);
        stage.setResizable(test.resizable);
        stage.setScene(new Scene(new Group(iconify, maximize, close)));

        if (test.hasOwner) {
            stage.initOwner(new Stage());
        }

        stage.show();
        assertEquals(test.iconifyDisabled, iconify.isDisabled());
        assertEquals(test.maximizeDisabled, maximize.isDisabled());
        assertFalse(close.isDisabled());
        stage.close();
    }

    /**
     * Tests that clicking the close button fires the {@link WindowEvent#WINDOW_CLOSE_REQUEST} event.
     */
    @Test
    void closeButtonFiresWindowCloseRequestEvent() {
        var stage = new Stage();
        var button = new Region();
        button.resize(10, 10);
        HeaderBar.setButtonType(button, HeaderButtonType.CLOSE);
        stage.setScene(new Scene(button));
        stage.show();

        boolean[] flag = new boolean[1];
        stage.setOnCloseRequest(_ -> flag[0] = true);

        Event.fireEvent(button, new MouseEvent(
            MouseEvent.MOUSE_RELEASED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
            false, false, false, false, false, false, false, false, false, false, null));

        assertTrue(flag[0]);
        stage.close();
    }
}
