/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.tk.Toolkit;
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

    //( name = "{index}: Button {0}, consuming {1}, registerAfterShowing {2} " )
    static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(new ButtonType(ButtonState.DEFAULT), true, true),
                Arguments.of(new ButtonType(ButtonState.DEFAULT), true, false),
                Arguments.of(new ButtonType(ButtonState.DEFAULT), false, true),
                Arguments.of(new ButtonType(ButtonState.DEFAULT), false, false),
                Arguments.of(new ButtonType(ButtonState.CANCEL), true, true),
                Arguments.of(new ButtonType(ButtonState.CANCEL), true, false),
                Arguments.of(new ButtonType(ButtonState.CANCEL), false, true),
                Arguments.of(new ButtonType(ButtonState.CANCEL), false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFallbackFilter(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        setup(buttonType);
        registerHandlerAndAssertFallbackNotification(buttonType, consume, registerAfterShowing, this::addEventFilter);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFallbackHandler(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        setup(buttonType);
        registerHandlerAndAssertFallbackNotification(buttonType, consume, registerAfterShowing, this::addEventHandler);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFallbackSingletonHandler(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        setup(buttonType);
        registerHandlerAndAssertFallbackNotification(buttonType, consume, registerAfterShowing, this::setOnKeyPressed);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testFallbackNoHandler(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        if (consume) return;
        setup(buttonType);
        show();
        assertTargetNotification(buttonType.getCode(), buttonType.getButton(), 1);
    }

    protected void registerHandlerAndAssertFallbackNotification(
        ButtonType buttonType,
        boolean consume,
        boolean registerAfterShowing,
        Consumer<EventHandler<KeyEvent>> consumer
    ) {
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
        assertEquals(expected, actions.size(), key + exp + " trigger ");
    }


    /**
     * sanity test of initial state and test assumptions
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testInitial(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        setup(buttonType);
        show();
        assertTrue(control.isFocused());
        assertSame(root, control.getParent());
        assertSame(root, fallback.getParent());
    }

    protected boolean isEnter(ButtonType buttonType) {
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

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    protected void setup(ButtonType buttonType) {
        initStage();
        control = createControl();
        fallback = buttonType.getButton();
        root.getChildren().addAll(control, fallback);
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
    }
}
