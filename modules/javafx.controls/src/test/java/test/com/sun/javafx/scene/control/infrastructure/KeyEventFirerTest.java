/*
 * Created on 22.10.2019
 *
 */
package test.com.sun.javafx.scene.control.infrastructure;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static org.junit.Assert.*;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Test of enhanced KeyEventFirer.
 * <p>
 *
 * Most of these tests are meant to document how to use the KeyEventFirer and what
 * happens if used incorrectly. The latter are ignored, because the build should pass.
 *
 */
public class KeyEventFirerTest {

    private TextField textField;
    private Button button;
    private Pane root;
    private Stage stage;
    private Scene scene;
    private ComboBox<String> comboBox;

    /**
     * Verify failing test from bug report example.
     */
    @Ignore
    @Test
    public void testFireComboEditorFailing() {
        showAndFocus(comboBox);
        List<KeyEvent> keys = new ArrayList<>();
        comboBox.getEditor().addEventFilter(KEY_PRESSED, keys::add);
        KeyEventFirer keyboard = new KeyEventFirer(comboBox.getEditor(), scene);
        keyboard.doKeyPress(ENTER);
        assertEquals("pressed ENTER filter on editor", 1, keys.size());
    }

    /**
     * False-green from bug report example.
     */
    @Ignore
    @Test
    public void testFireComboEditorFalseGreen() {
        showAndFocus(comboBox);
        List<KeyEvent> keys = new ArrayList<>();
        comboBox.getEditor().addEventFilter(KEY_PRESSED, keys::add);
        KeyEventFirer keyboard = new KeyEventFirer(comboBox.getEditor());
        keyboard.doKeyPress(ENTER);
        assertEquals("pressed ENTER filter on editor", 1, keys.size());
        fail("false green by firing directly on target which is not focusOwner");
    }

    /**
     * Test that keyEvent is delivered to focused control and nowhere else.
     * Here we fire directly onto the scene - and see a different outcome from
     * using scene.process: the events are delivered to the scene only, not
     * the focused node.
     */
    @Ignore
    @Test
    public void testFireSceneAsTarget() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        KeyEventFirer firer = new KeyEventFirer(scene);
        firer.doKeyPress(A);
        assertEquals("button must have received the key", 1, buttonEvents.size());
        assertEquals("textField must not have received the key", 0, textFieldEvents.size());
    }

    /**
     * Test that keyEvent is delivered to focused control and nowhere else.
     */
    @Test
    public void testFireViaScene() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        KeyEventFirer firer = new KeyEventFirer(textField, scene);
        firer.doKeyPress(A);
        assertEquals("button must have received the key", 1, buttonEvents.size());
        assertEquals("textField must not have received the key", 0, textFieldEvents.size());
    }

    /**
     * Test that keyEvent is delivered to focused control and nowhere else.
     * Here we test that the target is not required.
     */
    @Test
    public void testFireViaSceneNullTarget() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        KeyEventFirer firer = new KeyEventFirer(null, scene);
        firer.doKeyPress(A);
        assertEquals("button must have received the key", 1, buttonEvents.size());
        assertEquals("textField must not have received the key", 0, textFieldEvents.size());
    }

    /**
     * This simulates a false positive: even though not focused, the textField handlers
     * are notified when firing directly. That's possible, but typically not what we want to test!
     */
    @Ignore
    @Test
    public void testFireTargetFalseGreen() {
        showAndFocus(button);
        List<KeyEvent> buttonEvents = new ArrayList<>();
        button.addEventHandler(KEY_PRESSED, buttonEvents::add);
        List<KeyEvent> textFieldEvents = new ArrayList<>();
        textField.addEventHandler(KEY_PRESSED, textFieldEvents::add);
        KeyEventFirer firer = new KeyEventFirer(textField);
        firer.doKeyPress(A);
        assertEquals("textField must have received the key", 1, textFieldEvents.size());
        assertEquals("button must have received the key", 0, buttonEvents.size());
        fail("false green by firing directly on target which is not focusOwner");
    }

    @Test (expected= NullPointerException.class)
    public void testTwoParamConstructorNPE() {
        new KeyEventFirer(null, null);
    }

    @Test (expected= NullPointerException.class)
    public void testSingleParamConstructorNPE() {
        new KeyEventFirer(null);
    }

    /**
     * Need all: stage.show, stage.requestFocus and control.requestFocus to
     * have consistent focused state on control (that is focusOwner and isFocused)
     */
    @Test
    public void testUIState() {
        assertEquals(List.of(button, textField, comboBox), root.getChildren());
        stage.show();
        stage.requestFocus();
        button.requestFocus();
        assertEquals(button, scene.getFocusOwner());
        assertTrue(button.isFocused());
    }

    private void showAndFocus(Node focused) {
        stage.show();
        stage.requestFocus();
        if (focused != null) {
            focused.requestFocus();
            assertTrue(focused.isFocused());
            assertSame(focused, scene.getFocusOwner());
        }
    }

    @Before
    public void setup() {
        // This step is not needed (Just to make sure StubToolkit is loaded into VM)
        // @SuppressWarnings("unused")
        // Toolkit tk = (StubToolkit)Toolkit.getToolkit();
        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        button = new Button("I'm a button");
        textField = new TextField("some text");
        // to test the false-green example in the bug report
        comboBox = new ComboBox<>();
        comboBox.getItems().addAll("Test", "hello", "world");
        comboBox.setEditable(true);
        root.getChildren().addAll(button, textField, comboBox);
    }

    @After
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
    }

}
