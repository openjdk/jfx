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

package test.javafx.scene.control;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collection;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 */
public class FireButtonBaseTest {
    private static Collection<Class<?>> parameters() {
        return List.of(
            Button.class,
            CheckBox.class,
            Hyperlink.class,
            RadioButton.class,
            MenuButton.class,
            SplitMenuButton.class,
            ToggleButton.class
        );
    }

    private ButtonBase btn;

    //@BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Class<?> type) {
        try {
            btn = (ButtonBase) type.getDeclaredConstructor().newInstance();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void onActionCalledWhenButtonIsFired(Class<?> type) {
        setup(type);
        final EventHandlerStub handler = new EventHandlerStub();
        btn.setOnAction(handler);
        btn.fire();
        assertTrue(handler.called);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void onActionCalledWhenNullWhenButtonIsFiredIsNoOp(Class<?> type) {
        setup(type);
        btn.fire(); // should throw no exceptions, if it does, the test fails
    }

    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    }
}
