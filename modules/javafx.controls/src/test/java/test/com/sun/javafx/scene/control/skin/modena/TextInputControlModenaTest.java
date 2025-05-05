/*
 * Copyright (c) 2010, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * Test class for verifying CSS styling behavior in the Modena stylesheet
 * for JavaFX {@link TextInputControl} subclasses, such as {@link TextField},
 * {@link PasswordField}, and {@link TextArea}.
 *
 */
public class TextInputControlModenaTest {

    private static Collection<Class> parameters() {
        return List.of(
                TextField.class,
                PasswordField.class,
                TextArea.class
        );
    }

    private TextInputControl textInput;

    //@BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Class<?> type) {
        setUncaughtExceptionHandler();
        try {
            textInput = (TextInputControl)type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            fail(e);
        }
    }

    @AfterEach
    public void cleanup() {
        removeUncaughtExceptionHandler();
    }

    private void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    private void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
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
            setup(type);
            textInput.setText("This is a text");
            textInput.selectAll();

            StackPane root = new StackPane(textInput);
            Scene scene = new Scene(root, 400, 200);
            Stage stage = new Stage();
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

}
