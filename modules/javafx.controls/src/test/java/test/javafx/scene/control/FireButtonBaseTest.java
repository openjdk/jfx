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

package test.javafx.scene.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.util.Arrays;
import java.util.Collection;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToggleButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(Parameterized.class)
public class FireButtonBaseTest {
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                {Button.class},
                {CheckBox.class},
                {Hyperlink.class},
                {RadioButton.class},
                {MenuButton.class},
                {SplitMenuButton.class},
                {ToggleButton.class}
        });
    }

    private ButtonBase btn;
    private Class type;

    public FireButtonBaseTest(Class type) {
        this.type = type;
    }

    @Before public void setup() throws Exception {
        btn = (ButtonBase) type.getDeclaredConstructor().newInstance();
    }

    @Test public void onActionCalledWhenButtonIsFired() {
        final EventHandlerStub handler = new EventHandlerStub();
        btn.setOnAction(handler);
        btn.fire();
        assertTrue(handler.called);
    }

    @Test public void onActionCalledWhenNullWhenButtonIsFiredIsNoOp() {
        btn.fire(); // should throw no exceptions, if it does, the test fails
    }

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };
}
