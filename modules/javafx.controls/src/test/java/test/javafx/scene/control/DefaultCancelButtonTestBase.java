/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.sun.javafx.tk.Toolkit;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static org.junit.Assert.*;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;

/**
 * Abstract base test for interplay of ENTER/ESCAPE handlers on Controls with
 * default/cancel button actions.
 * <p>
 * Need to test all combinations of
 * <ul>
 * <li> default/cancel button
 * <li> not/consuming external handler
 * <li> handler registration before/after showing the stage: this is due to
 *   https://bugs.openjdk.org/browse/JDK-8231245 (Controls' behavior
 *   depends on sequence of handler registration). The errors mostly show up
 *   when the handlers are registered after the stage is shown.
 * <li> added filter/handler/singleton handler and no handler at all
 * </ul>
 *
 * The test parameterized on all combination of the first 3 bullets, handling
 * the last by 4 test methods.
 */
@RunWith(Parameterized.class)
public abstract class DefaultCancelButtonTestBase<C extends Control> {
    /**
     * State of default/cancel button.
     */
    public static enum ButtonState {

        DEFAULT(ENTER),
        CANCEL(ESCAPE);

        KeyCode key;

        ButtonState(KeyCode key) {
            this.key = key;
        }

        /**
         * KeyCode that external handlers/button type is interested in.
         * @return
         */
        public KeyCode getCode() {
            return key;
        }

        /**
         * Creates and returns a handler that consumes the event for
         * keyCode.
         *
         * @return handler that consumes if the keyCode of the
         * event is the same as this type's keyCode.
         */
        public EventHandler<KeyEvent> getConsumingHandler() {
            return e -> {
                if (getCode() == e.getCode()) e.consume();
            };
        }

        /**
         * Configures the given button as either default or
         * cancel, based on keyCode.
         *
         * @param button to configure.
         */
        public void configureButton(Button button) {
            if (getCode() == ENTER) {
                button.setDefaultButton(true);
            } else if (getCode() == ESCAPE) {
                button.setCancelButton(true);
            }

        }
    }

    public static class ButtonType {
        Button button;
        ButtonState type;

        public ButtonType(ButtonState type) {
            this.type = type;
            button = new Button();
            type.configureButton(button);
        }

        public Button getButton() {
            return button;
        }

        public KeyCode getCode() {
            return type.getCode();
        }

        /**
         * Returns a competing handler (for our keyCode) that not/consumes
         * a received keyEvent depending on the given consuming flag. The
         * handler can be registered on another control in the same scene.
         *
         * @param consuming
         * @return
         */
        public EventHandler<KeyEvent> getKeyHandler(boolean consuming) {
            return consuming ? type.getConsumingHandler() : e -> {};
        }

        @Override
        public String toString() {
            return "" + type;
        }


    }

    private Stage stage;
    private VBox root;
    private C control;
    private Button fallback;
    private Scene scene;

    private ButtonType buttonType;
    private boolean consume;
    private boolean registerAfterShowing;

    // TODO name doesn't compile with gradle :controls:test
    // because the junit version is 4.8.2 - name was introduced in 4.11
    // commenting for now until upgrade to newer junit
    @Parameterized.Parameters //( name = "{index}: Button {0}, consuming {1}, registerAfterShowing {2} " )
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
            // buttonType, consuming, registerAfterShowing
            {new ButtonType(ButtonState.DEFAULT), true, true},
            {new ButtonType(ButtonState.DEFAULT), true, false},
            {new ButtonType(ButtonState.DEFAULT), false, true},
            {new ButtonType(ButtonState.DEFAULT), false, false},
            {new ButtonType(ButtonState.CANCEL), true, true},
            {new ButtonType(ButtonState.CANCEL), true, false},
            {new ButtonType(ButtonState.CANCEL), false, true},
            {new ButtonType(ButtonState.CANCEL), false, false},
        };
        return Arrays.asList(data);
    }

    public DefaultCancelButtonTestBase(ButtonType buttonType, boolean consume,
            boolean registerAfterShowing) {
        this.buttonType = buttonType;
        this.consume = consume;
        this.registerAfterShowing = registerAfterShowing;
    }


    @Test
    public void testFallbackFilter() {
        registerHandlerAndAssertFallbackNotification(this::addEventFilter);
    }

    @Test
    public void testFallbackHandler() {
        registerHandlerAndAssertFallbackNotification(this::addEventHandler);

    }

    @Test
    public void testFallbackSingletonHandler() {
        registerHandlerAndAssertFallbackNotification(this::setOnKeyPressed);

    }

    @Test
    public void testFallbackNoHandler() {
        if (consume) return;
        show();
        assertTargetNotification(buttonType.getCode(), buttonType.getButton(), 1);
    }

    protected void registerHandlerAndAssertFallbackNotification(Consumer<EventHandler<KeyEvent>> consumer) {
        if (registerAfterShowing) {
            show();
        }
        consumer.accept(buttonType.getKeyHandler(consume));
        if (!registerAfterShowing) {
            show();
        }

        int expected = consume ? 0 : 1;
        assertTargetNotification(buttonType.getCode(), buttonType.getButton(), expected);

    }
    /**
     * Registers the given handler on the textfield by adding as handler for keyPressed.
     * @param handler the handler to register
     */
    protected void addEventHandler(EventHandler<KeyEvent> handler) {
        control.addEventHandler(KEY_PRESSED, handler);
    }

    /**
     * Registers the given handler on the textfield by setting as singleton
     * keyPressed handler.
     * @param handler the handler to register
     */
    protected void setOnKeyPressed(EventHandler<KeyEvent> handler) {
        control.setOnKeyPressed(handler);
    }

    /**
     * Registers the given handler on the textfield by adding as filter for keyPressed.
     * @param handler the handler to register
     */
    protected void addEventFilter(EventHandler<KeyEvent> filter) {
        control.addEventFilter(KEY_PRESSED, filter);
    }

// ------------ assert helpers
    /**
     * Fires the key onto the control and asserts that
     * the target button receives the expected number of notifications in
     * its action handler.
     *
     * @param key the key to fire on the control
     * @param target the target button to test for nori
     * @param expected number of notifications in target button's action handler
     */
    protected void assertTargetNotification(KeyCode key, Button target, int expected) {
        List<ActionEvent> actions = new ArrayList<>();
        target.setOnAction(actions::add);
        KeyEventFirer keyFirer = new KeyEventFirer(control);
        keyFirer.doKeyPress(key);
        String exp = expected > 0 ? " must " : " must not ";
        assertEquals(key + exp + " trigger ", expected, actions.size());
    }


    /**
     * sanity test of initial state and test assumptions
     */
    @Test
    public void testInitial() {
        show();
        assertTrue(control.isFocused());
        assertSame(root, control.getParent());
        assertSame(root, fallback.getParent());
    }


    protected boolean isEnter() {
        return buttonType.getCode() == ENTER;
    }

    protected abstract C createControl();
    protected C getControl() {
        return control;
    }

    protected void show() {
        stage.show();
        // PENDING JW: a bit weird - sometimes need to focus the stage before
        // the node, sometimes not
        stage.requestFocus();
        control.requestFocus();
    }

    private void initStage() {
        assertTrue(Toolkit.getToolkit() instanceof StubToolkit);  // Ensure StubToolkit is loaded

        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    @Before
    public void setup() {
        initStage();
        control = createControl();

        fallback = buttonType.getButton();
        root.getChildren().addAll(control, fallback);

    }

    @After
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
    }

}
