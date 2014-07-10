/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.AutoSuggestEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

/**
 * KeyCombination popup editor (for keyboard shortcuts).
 *
 */
public class KeyCombinationPopupEditor extends PopupEditor {

    @FXML
    StackPane mainKeySp;
    @FXML
    Button clearAllBt;

    private GridPane gridPane;
    private static final int NB_MODIFIERS_MAX = 5;
    private final ArrayList<ModifierRow> modifierRows = new ArrayList<>();
    private KeyCombination.ModifierValue alt;
    private KeyCombination.ModifierValue control;
    private KeyCombination.ModifierValue meta;
    private KeyCombination.ModifierValue shift;
    private KeyCombination.ModifierValue shortcut;
    private MainKey mainKey;
    private EditorController editorController;
    private final KeyCombination.Modifier[] keyCombinationModifiers = {
        KeyCombination.ALT_ANY, KeyCombination.ALT_DOWN,
        KeyCombination.CONTROL_ANY, KeyCombination.CONTROL_DOWN,
        KeyCombination.META_ANY, KeyCombination.META_DOWN,
        KeyCombination.SHIFT_ANY, KeyCombination.SHIFT_DOWN,
        KeyCombination.SHORTCUT_ANY, KeyCombination.SHORTCUT_DOWN};

    @SuppressWarnings("LeakingThisInConstructor")
    public KeyCombinationPopupEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses,
            EditorController editorController) {
        super(propMeta, selectedClasses);
        this.editorController = editorController;
    }

    //
    // Interface from PopupEditor.
    // Methods called by PopupEditor.
    //
    @Override
    public void initializePopupContent() {
        Parent root = EditorUtils.loadPopupFxml("KeyCombinationPopupEditor.fxml", this); //NOI18N
        assert root instanceof GridPane;
        gridPane = (GridPane) root;
        // Build suggested key code list
        List<Field> keyCodes = Arrays.asList(KeyCode.class.getFields());
        List<String> keyCodesStr = new ArrayList<>();
        for (Field keyCode : keyCodes) {
            keyCodesStr.add(keyCode.getName());
        }

        mainKey = new MainKey(keyCodesStr, editorController);
        mainKeySp.getChildren().add(mainKey.getNode());

        clearAllBt.setText(I18N.getString("inspector.keycombination.clear"));
        clearAllBt.setOnAction(t -> {
            resetState();
            buildUI();
            commit(null);
        });

        buildUI();
    }

    @Override
    public String getPreviewString(Object value) {
        if (value == null) {
            return I18N.getString("inspector.keycombination.null");
        }
        assert value instanceof KeyCombination;
        KeyCombination keyCombinationVal = (KeyCombination) value;
        String valueAsString;
        if (isIndeterminate()) {
            valueAsString = "-"; //NOI18N
        } else {
            valueAsString = keyCombinationVal.toString();
        }
        return valueAsString;
    }

    @Override
    public void setPopupContentValue(Object value) {

        if (value != null) {
            // Empty the editor
            resetState();
            resetUI();
            assert value instanceof KeyCombination;
            // Apply the new keyCombination
            buildContent((KeyCombination) value);
        } else {
            resetState();
            buildUI();
        }
    }

    @Override
    public Node getPopupContentNode() {
        return gridPane;
    }

    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, EditorController editorController) {
        super.reset(propMeta, selectedClasses);
        this.editorController = editorController;
    }

    private void resetState() {
        modifierRows.clear();
        mainKey.setKeyCode(null);
    }

    private void resetUI() {
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
    }

    private void buildContent(KeyCombination keyCombination) {
        assert keyCombination != null;

        // Build the modifiers rows
        modifierRows.clear();
        KeyCombination.Modifier modifier1 = null;
        alt = keyCombination.getAlt();
        if (alt != KeyCombination.ModifierValue.UP) {
            if (alt == KeyCombination.ModifierValue.DOWN) {
                modifier1 = KeyCombination.ALT_DOWN;
            } else if (alt == KeyCombination.ModifierValue.ANY) {
                modifier1 = KeyCombination.ALT_ANY;
            }
            createModifierRow(modifier1);
        }

        KeyCombination.Modifier modifier2 = null;
        control = keyCombination.getControl();
        if (control != KeyCombination.ModifierValue.UP) {
            if (control == KeyCombination.ModifierValue.DOWN) {
                modifier2 = KeyCombination.CONTROL_DOWN;
            } else if (control == KeyCombination.ModifierValue.ANY) {
                modifier2 = KeyCombination.CONTROL_ANY;
            }
            createModifierRow(modifier2);
        }

        KeyCombination.Modifier modifier3 = null;
        meta = keyCombination.getMeta();
        if (meta != KeyCombination.ModifierValue.UP) {
            if (meta == KeyCombination.ModifierValue.DOWN) {
                modifier3 = KeyCombination.META_DOWN;
            } else if (meta == KeyCombination.ModifierValue.ANY) {
                modifier3 = KeyCombination.META_ANY;
            }
            createModifierRow(modifier3);
        }

        KeyCombination.Modifier modifier4 = null;
        shift = keyCombination.getShift();
        if (shift != KeyCombination.ModifierValue.UP) {
            if (shift == KeyCombination.ModifierValue.DOWN) {
                modifier4 = KeyCombination.SHIFT_DOWN;
            } else if (shift == KeyCombination.ModifierValue.ANY) {
                modifier4 = KeyCombination.SHIFT_ANY;
            }
            createModifierRow(modifier4);
        }

        KeyCombination.Modifier modifier5 = null;
        shortcut = keyCombination.getShortcut();
        if (shortcut != KeyCombination.ModifierValue.UP) {
            if (shortcut == KeyCombination.ModifierValue.DOWN) {
                modifier5 = KeyCombination.SHORTCUT_DOWN;
            } else if (shortcut == KeyCombination.ModifierValue.ANY) {
                modifier5 = KeyCombination.SHORTCUT_ANY;
            }
            createModifierRow(modifier5);
        }

        // Handle the main key
        KeyCode keyCode = null;
        if (keyCombination instanceof KeyCodeCombination) {
            keyCode = ((KeyCodeCombination) keyCombination).getCode();
        } else if (keyCombination instanceof KeyCharacterCombination) {
            keyCode = KeyCode.getKeyCode(((KeyCharacterCombination) keyCombination).getCharacter());
        }
        mainKey.setKeyCode(keyCode);

        // Build the UI
        buildUI();

        commit(keyCombination);
    }

    private void commit(KeyCombination keyCombination) {
        commitValue(keyCombination);
    }

    private KeyCombination createKeyCombination() {
        if (mainKey.isEmpty()) {
            return null;
        }
        KeyCodeCombination keyComb = null;
        List<KeyCombination.Modifier> modifiers = new ArrayList<>();
        for (ModifierRow modifier : modifierRows) {
            if (!modifier.isEmpty()) {
                if (modifiers.contains(modifier.getModifier())) {
                    // doublon: invalid
                    return null;
                }
                modifiers.add(modifier.getModifier());
            }
        }
        if (modifiers.isEmpty()) {
            // no modifier: invalid
            return null;
        }
        try {
            keyComb = new KeyCodeCombination(mainKey.getKeyCode(), modifiers.toArray(new KeyCombination.Modifier[1]));
        } catch (IllegalArgumentException | NullPointerException ex) {
            System.out.println("Invalid key combination" + ex); //NOI18N
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage() + ex);
        }
        return keyComb;
    }

    private List<KeyCombination.Modifier> getModifierConstants() {
        ArrayList<KeyCombination.Modifier> mods = new ArrayList<>();
        for (KeyCombination.Modifier modifier : keyCombinationModifiers) {
            boolean alreadyUsed = false;
            for (ModifierRow row : modifierRows) {
                if (!row.isEmpty()) {
                    if (row.getModifier().getKey().equals(modifier.getKey())) {
                        // modifier already used
                        alreadyUsed = true;
                        break;
                    }
                }
            }
            if (!alreadyUsed) {
                mods.add(modifier);
            }
        }
        mods.add(null);
        Collections.sort(mods, new ModifierComparator());
        return mods;
    }

    private static class ModifierComparator implements Comparator<KeyCombination.Modifier> {

        @Override
        public int compare(KeyCombination.Modifier o1, KeyCombination.Modifier o2) {
            if (o1 == null || o2 == null) {
                return -1;
            }
            String str1 = o1.getKey().toString() + o1.getValue().toString();
            String str2 = o2.getKey().toString() + o2.getValue().toString();
            return str1.compareTo(str2);
        }
    }

    private ChoiceBox<KeyCombination.Modifier> createModifierChoiceBox(KeyCombination.Modifier modifier) {
        final ChoiceBox<KeyCombination.Modifier> modifierChoiceBox = new ChoiceBox<>();
        EditorUtils.makeWidthStretchable(modifierChoiceBox);
        modifierChoiceBox.setConverter(new ModifierConverter());
        modifierChoiceBox.getItems().setAll(getModifierConstants());
        if (modifier != null) {
            modifierChoiceBox.getSelectionModel().select(modifier);
        }

        modifierChoiceBox.getSelectionModel().selectedItemProperty().addListener((ChangeListener<Modifier>) (observable, oldValue, newValue) -> {
            if (!mainKey.isEmpty()) {
        KeyCombination kc = createKeyCombination();
        if (kc != null) {
            commit(kc);
        }
            }
            buildUI();
         });
        return modifierChoiceBox;
    }

    private void buildUI() {
        resetUI();

        // Cleanup: remove empty rows
        ArrayList<ModifierRow> emptyRows = new ArrayList<>();
        for (ModifierRow row : modifierRows) {
            if (row.isEmpty()) {
                emptyRows.add(row);
            }
        }
        modifierRows.removeAll(emptyRows);

        int lineIndex = 0;
        for (ModifierRow row : modifierRows) {
            addModifierRow(row, lineIndex);
            lineIndex++;
        }

        // add an empty row
        boolean added = false;
        if (modifierRows.size() < NB_MODIFIERS_MAX) {
            added = addEmptyModifierIfNeeded();
        }
        if (added) {
            lineIndex++;
        }

        // add mainKey
        Label mainKeyLabel = new Label(I18N.getString("inspector.keycombination.mainkey"));
        gridPane.add(mainKeyLabel, 0, lineIndex);
        gridPane.add(mainKey.getNode(), 1, lineIndex);
        lineIndex++;

        // add reset button
        gridPane.add(clearAllBt, 1, lineIndex);
    }

    private boolean addEmptyModifierIfNeeded() {
        for (ModifierRow row : modifierRows) {
            if (row.isEmpty()) {
                return false;
            }
        }
        addModifierRow(createModifierRow(null), modifierRows.size() - 1);
        return true;
    }

    private void addModifierRow(ModifierRow row, int lineIndex) {
        row.getLabel().setText(I18N.getString("inspector.keycombination.modifier")
                + " " + (lineIndex + 1)); //NOI18N
        gridPane.add(row.getLabel(), 0, lineIndex);
        gridPane.add(row.getChoiceBox(), 1, lineIndex);
    }

    private ModifierRow createModifierRow(KeyCombination.Modifier modifier) {
        ChoiceBox<KeyCombination.Modifier> choiceBox = createModifierChoiceBox(modifier);
        ModifierRow row = new ModifierRow(choiceBox);
        modifierRows.add(row);
        return row;
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private class MainKey extends AutoSuggestEditor {

        private final EditorController editorController;
        String mainKey = null;

        public MainKey(List<String> suggestedKeys, EditorController editorController) {
            super("", null, suggestedKeys); //NOI18N
            this.editorController = editorController;
            EventHandler<ActionEvent> onActionListener = t -> {
                if (Objects.equals(mainKey, getTextField().getText())) {
                    // no change
                    return;
                }
                mainKey = getTextField().getText();
                if (!mainKey.isEmpty()) {
                    KeyCombination kc = createKeyCombination();
                    if (kc != null) {
                        commit(kc);
                    }
                }
            };

            setTextEditorBehavior(this, getTextField(), onActionListener);
            commitOnFocusLost(this);
        }

        public Node getNode() {
            return getValueEditor();
        }

        public void setKeyCode(KeyCode keyCode) {
            setValue((keyCode != null) ? keyCode.toString() : "");//NOI18N
        }

        public KeyCode getKeyCode() {
            String valStr = getTextField().getText();
            if (valStr.isEmpty()) {
                return null;
            }
            // Put the string in uppercase for convenience (all kycode are uppercase)
            valStr = valStr.toUpperCase(Locale.ROOT);

            KeyCode keyCode = null;
            try {
                keyCode = KeyCode.valueOf(valStr);
            } catch (Exception ex) {
                editorController.getMessageLog().logWarningMessage(
                        "inspector.keycombination.invalidkeycode", valStr); //NOI18N
            }
            return keyCode;
        }

        public boolean isEmpty() {
            return getKeyCode() == null;
        }
    }

    private static void commitOnFocusLost(AutoSuggestEditor autoSuggestEditor) {
        autoSuggestEditor.getTextField().focusedProperty().addListener((ChangeListener<Boolean>) (ov, oldVal, newVal) -> {
            if (!newVal) {
                // Focus lost
                autoSuggestEditor.getCommitListener().handle(null);
            }
        });
    }

    private static class ModifierRow {

        private Label label;
        private ChoiceBox<KeyCombination.Modifier> choiceBox;

        public ModifierRow(ChoiceBox<KeyCombination.Modifier> choiceBox) {
            this.label = new Label();
            this.choiceBox = choiceBox;
        }

        public Label getLabel() {
            return label;
        }

        @SuppressWarnings("unused")
        public void setLabel(Label label) {
            this.label = label;
        }

        public ChoiceBox<KeyCombination.Modifier> getChoiceBox() {
            return choiceBox;
        }

        @SuppressWarnings("unused")
        public void setChoiceBox(ChoiceBox<KeyCombination.Modifier> choiceBox) {
            this.choiceBox = choiceBox;
        }

        public KeyCombination.Modifier getModifier() {
            return choiceBox.getSelectionModel().getSelectedItem();
        }

        public boolean isEmpty() {
            return getModifier() == null;
        }
    }

    private static class ModifierConverter extends StringConverter<KeyCombination.Modifier> {

        @Override
        public String toString(KeyCombination.Modifier object) {
            if (object == null) {
                return I18N.getString("inspector.keycombination.none");
            }
            return object.getKey() + "_" + object.getValue(); //NOI18N 
        }

        @Override
        public KeyCombination.Modifier fromString(String string) {
            if (string.equals(I18N.getString("inspector.keycombination.none"))) {
                return null;
            }
            if (string.startsWith(KeyCode.ALT.getName())) {
                if (string.endsWith(KeyCombination.ModifierValue.DOWN.name())) {
                    return KeyCombination.ALT_DOWN;
                } else if (string.endsWith(KeyCombination.ModifierValue.ANY.name())) {
                    return KeyCombination.ALT_ANY;
                }
            }
            if (string.startsWith(KeyCode.CONTROL.getName())) {
                if (string.endsWith(KeyCombination.ModifierValue.DOWN.name())) {
                    return KeyCombination.CONTROL_DOWN;
                } else if (string.endsWith(KeyCombination.ModifierValue.ANY.name())) {
                    return KeyCombination.CONTROL_ANY;
                }
            }
            if (string.startsWith(KeyCode.META.getName())) {
                if (string.endsWith(KeyCombination.ModifierValue.DOWN.name())) {
                    return KeyCombination.META_DOWN;
                } else if (string.endsWith(KeyCombination.ModifierValue.ANY.name())) {
                    return KeyCombination.META_ANY;
                }
            }
            if (string.startsWith(KeyCode.SHIFT.getName())) {
                if (string.endsWith(KeyCombination.ModifierValue.DOWN.name())) {
                    return KeyCombination.SHIFT_DOWN;
                } else if (string.endsWith(KeyCombination.ModifierValue.ANY.name())) {
                    return KeyCombination.SHIFT_ANY;
                }
            }
            if (string.startsWith(KeyCode.SHORTCUT.getName())) {
                if (string.endsWith(KeyCombination.ModifierValue.DOWN.name())) {
                    return KeyCombination.SHORTCUT_DOWN;
                } else if (string.endsWith(KeyCombination.ModifierValue.ANY.name())) {
                    return KeyCombination.SHORTCUT_ANY;
                }
            }
            return null;
        }
    }

}
