/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class KeyCharacterCombinationTest extends Application {
    private final TextArea typingArea = new TextArea("");
    private KeyEvent lastPressed = null;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        typingArea.setEditable(false);
        typingArea.appendText("Press keys that generate printable characters.\n");
        typingArea.appendText("Shifted punctuation keys are most likely to fail.\n\n");

        typingArea.addEventFilter(KeyEvent.KEY_PRESSED, this::pressedEvent);
        typingArea.addEventFilter(KeyEvent.KEY_RELEASED, this::releasedEvent);
        typingArea.addEventFilter(KeyEvent.KEY_TYPED, this::typedEvent);

        Scene scene = new Scene(typingArea, 640, 640);
        stage.setScene(scene);
        stage.setTitle("Key Character Combinations");
        stage.show();

        Platform.runLater(typingArea::requestFocus);
    }

    // Helper Methods for Event Handling
    private void passed(String str) {
        typingArea.appendText("Passed: " + str + "\n");
    }

    private void failed(String str) {
        typingArea.appendText("* Failed: " + str + "\n");
    }

    private void ignored(String str) {
        typingArea.appendText("Ignored: " + str + "\n");
    }

    private void pressedEvent(KeyEvent e) {
        lastPressed = e;
    }

    private void releasedEvent(KeyEvent e) {
        lastPressed = null;
    }

    private KeyCombination.ModifierValue toModifier(boolean down)
    {
        if (down)
            return KeyCombination.ModifierValue.DOWN;
        return KeyCombination.ModifierValue.UP;
    }

    private void typedEvent(KeyEvent e) {
        if (lastPressed == null)
            return;

        // KeyCharacterCombinations only deal with one char at a time.
        if (e.getCharacter().length() == 0) {
            ignored("no text");
            return;
        }
        if (e.getCharacter().length() > 1) {
            ignored("text too long");
            return;
        }

        // Keys that only generate characters with diacritics are not
        // assigned key codes and will not match any combination.
        if (lastPressed.getCode() == KeyCode.UNDEFINED) {
            ignored("undefined key code for " + e.getCharacter());
            return;
        }

        String keyCodeName = lastPressed.getCode().getName();

        // Keys that generate control codes (like Tab and Delete) don't
        // work on some platforms. There are existing bugs on this which
        // will probably never be fixed since these keys should be
        // handled using KeyCodeCombinations instead.
        if (Character.isISOControl(e.getCharacter().charAt(0))) {
            ignored("control key"); 
            return;
        }

        // Keys on the numeric keypad will not match on Windows or Linux.
        // There are existing bug reports for this.
        if (lastPressed.getCode().isKeypadKey() ||
            lastPressed.getCode() == KeyCode.DIVIDE ||
            lastPressed.getCode() == KeyCode.MULTIPLY ||
            lastPressed.getCode() == KeyCode.ADD ||
            lastPressed.getCode() == KeyCode.SUBTRACT ||
            lastPressed.getCode() == KeyCode.DECIMAL) {
            ignored("keypad code ");
            return;
        }

        // Construct a KeyCharacterCombination with the same modifiers and verify that it
        // matches the key press event. This tests the internal routine
        // Toolkit::getKeyCodeForChar.
        KeyCombination.ModifierValue shiftModifier = toModifier(lastPressed.isShiftDown());
        KeyCombination.ModifierValue controlModifier = toModifier(lastPressed.isControlDown());
        KeyCombination.ModifierValue altModifier = toModifier(lastPressed.isAltDown());
        KeyCombination.ModifierValue metaModifier = toModifier(lastPressed.isMetaDown());
        KeyCombination.ModifierValue shortcutModifier = toModifier(lastPressed.isShortcutDown());

        KeyCharacterCombination combination = new KeyCharacterCombination(e.getCharacter(),
            shiftModifier, controlModifier, altModifier, metaModifier, shortcutModifier);

        String combinationDescription = combination.getDisplayText();
        if (lastPressed.getCode().isWhitespaceKey())
        {
            // Replace 'invisible' characters with their names.
            if (!combinationDescription.isEmpty())
                combinationDescription = combinationDescription.substring(0, combinationDescription.length() - 1);
            combinationDescription += lastPressed.getCode().getName();
        }
        
        if (combination.match(lastPressed))
            passed("key code " + keyCodeName + " matched " + combinationDescription);
        else
            failed("key code " + keyCodeName + " did not match " + combinationDescription);
    }
}
