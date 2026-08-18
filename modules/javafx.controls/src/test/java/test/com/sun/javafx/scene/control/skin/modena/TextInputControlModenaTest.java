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

package test.com.sun.javafx.scene.control.skin.modena;

import com.sun.javafx.tk.Toolkit;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import javafx.application.Application;
import javafx.scene.paint.Color;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Collection;
import java.util.List;

import static javafx.scene.control.skin.TextInputSkinShim.getPromptNode;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * Test class for verifying CSS styling behavior in the Modena stylesheet
 * for JavaFX {@link TextInputControl} subclasses, such as {@link TextField},
 * {@link PasswordField}, and {@link TextArea}.
 *
 */
public class TextInputControlModenaTest {
    private static String userAgentStylesheet;
    private Stage stage;

    private static Collection<Class> parameters() {
        return List.of(
                TextField.class,
                PasswordField.class,
                TextArea.class
        );
    }

    @BeforeAll
    public static void setup() {
        userAgentStylesheet = Application.getUserAgentStylesheet();
        Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
    }

    @AfterAll
    public static void cleanup() {
        Application.setUserAgentStylesheet(userAgentStylesheet);
    }

    @AfterEach
    public void cleanupTest() {
        if (stage != null) {
            stage.hide();
            stage = null;
        }
    }

    private TextInputControl createTextInput(Class<?> type) {
        try {
            return (TextInputControl) type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            fail(e);
            return null;
        }
    }

    /******************************************************
     * Test for highlight-text-fill                       *
     *****************************************************/

    /**
     * Verifies that the text selection color (highlight text fill) in different
     * {@link TextInputControl} types matches the expected color when a custom
     * <code>-fx-accent</code> color is applied.
     *
     * @param accentColor        the accent color applied via CSS
     * @param expectedTextColor  a hex color (no '#') that should be part of the computed selection fill
     */
    @ParameterizedTest
    @CsvSource({
            "#999999, 333333",
            "#222222, ffffff",
            "#777777, 000000"
    })
    public void testHighlightTextInput(String accentColor, String expectedTextColor) {
        for (Class<?> type : parameters()) {
            TextInputControl textInput = createTextInput(type);
            textInput.setText("This is a text");
            textInput.selectAll();

            StackPane root = new StackPane(textInput);
            Scene scene = new Scene(root, 400, 200);
            stage = new Stage();
            stage.setScene(scene);
            stage.show();

            scene.getRoot().setStyle("-fx-accent: " + accentColor + ";");
            textInput.requestFocus();
            Toolkit.getToolkit().firePulse();

            Text internalText = (Text) textInput.lookup(".text");
            Paint fill = internalText.getSelectionFill();

            String resolved = fill.toString().toLowerCase();
            assertTrue(resolved.contains(expectedTextColor));
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void promptTextIsVisibleWhenEmptyFocusedTextInput(Class<? extends TextInputControl> type)
            throws Exception {

        TextInputControl control = type.getDeclaredConstructor().newInstance();
        control.setPromptText("Prompt text");
        StackPane root = new StackPane(control);
        Scene scene = new Scene(root, 400, 200);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();

        control.requestFocus();
        Toolkit.getToolkit().firePulse();
        assertTrue(control.isFocused(), type.getSimpleName() + " should be focused");

        Text promptNode = getPromptNode(control);
        assertNotNull(promptNode, type.getSimpleName() + " should have a prompt node");
        assertTrue(promptNode.isVisible(), type.getSimpleName() + " prompt text should be visible");
        assertNotEquals(Color.TRANSPARENT, promptNode.getFill(),
                type.getSimpleName() + " prompt text fill should not be transparent");
    }
}
