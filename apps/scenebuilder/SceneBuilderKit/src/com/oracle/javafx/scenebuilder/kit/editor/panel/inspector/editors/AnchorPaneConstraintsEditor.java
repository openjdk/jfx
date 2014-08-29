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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;

/**
 * Editor for AnchorPane constraints.
 *
 * 
 */
public class AnchorPaneConstraintsEditor extends PropertiesEditor {

    private static final String ANCHOR_ENABLED_COLOR = "-sb-line-art-accent";
    private static final String ANCHOR_DISABLED_COLOR = "-sb-line-art";

    @FXML
    private ToggleButton bottomTb;
    @FXML
    private TextField bottomTf;
    @FXML
    private Region innerR;
    @FXML
    private ToggleButton leftTb;
    @FXML
    private TextField leftTf;
    @FXML
    private Region outerR;
    @FXML
    private ToggleButton rightTb;
    @FXML
    private TextField rightTf;
    @FXML
    private ToggleButton topTb;
    @FXML
    private TextField topTf;

    private Parent root = null;
    private final ArrayList<ConstraintEditor> contraintEditors = new ArrayList<>();
    private ChangeListener<Object> constraintListener;

    public AnchorPaneConstraintsEditor(String name, ValuePropertyMetadata topPropMeta,
            ValuePropertyMetadata rightPropMeta,
            ValuePropertyMetadata bottomPropMeta,
            ValuePropertyMetadata leftPropMeta,
            Set<FXOMInstance> selectedInstances) {
        super(name);
        initialize(topPropMeta, rightPropMeta, bottomPropMeta, leftPropMeta, selectedInstances);
        propertyChanged();
        styleRegions();
    }

    // Method to please findBugs
    private void initialize(ValuePropertyMetadata topPropMeta, ValuePropertyMetadata rightPropMeta,
            ValuePropertyMetadata bottomPropMeta, ValuePropertyMetadata leftPropMeta, Set<FXOMInstance> selectedInstances) {
        root = EditorUtils.loadFxml("AnchorPaneConstraintsEditor.fxml", this);

        constraintListener = (ov, prevValue, newValue) -> {
            propertyChanged();
            styleRegions();
        };

        contraintEditors.add(
                new ConstraintEditor(topTf, topTb, selectedInstances, topPropMeta, constraintListener));
        contraintEditors.add(
                new ConstraintEditor(rightTf, rightTb, selectedInstances, rightPropMeta, constraintListener));
        contraintEditors.add(
                new ConstraintEditor(bottomTf, bottomTb, selectedInstances, bottomPropMeta, constraintListener));
        contraintEditors.add(
                new ConstraintEditor(leftTf, leftTb, selectedInstances, leftPropMeta, constraintListener));
    }

    @Override
    public List<PropertyEditor> getPropertyEditors() {
        List<PropertyEditor> propertyEditors = new ArrayList<>();
        for (ConstraintEditor constraintEditor : contraintEditors) {
            propertyEditors.add(constraintEditor);
        }
        return propertyEditors;
    }

    public void reset(ValuePropertyMetadata topPropMeta,
            ValuePropertyMetadata rightPropMeta,
            ValuePropertyMetadata bottomPropMeta,
            ValuePropertyMetadata leftPropMeta,
            Set<FXOMInstance> selectedInstances) {
        contraintEditors.get(0).reset(selectedInstances, topPropMeta);
        contraintEditors.get(1).reset(selectedInstances, rightPropMeta);
        contraintEditors.get(2).reset(selectedInstances, bottomPropMeta);
        contraintEditors.get(3).reset(selectedInstances, leftPropMeta);
        for (int ii = 0; ii < 4; ii++) {
            contraintEditors.get(ii).addValueListener(constraintListener);
        }
        styleRegions();
    }

    @Override
    public Node getValueEditor() {
        return root;
    }

    private void styleRegions() {
        StringBuilder styleString = new StringBuilder();
        for (int ii = 0; ii < 4; ii++) {
            if (contraintEditors.get(ii).isAnchorEnabled()) {
                styleString.append(ANCHOR_ENABLED_COLOR);
                styleString.append(" ");
            } else {
                styleString.append(ANCHOR_DISABLED_COLOR);
                styleString.append(" ");
            }
        }
        String style = "-fx-border-color: " + styleString;
        innerR.setStyle(style);
        outerR.setStyle(style);
    }

    /*
     * Editor for a single constraint (e.g. topAnchor)
     */
    private static class ConstraintEditor extends PropertyEditor {

        private ToggleButton toggleButton;
        private TextField textField;
        private Set<FXOMInstance> selectedInstances;
        private ValuePropertyMetadata propMeta;

        private boolean updateFromTextField = false;

        public ConstraintEditor(TextField textField, ToggleButton toggleButton, Set<FXOMInstance> selectedInstances,
                ValuePropertyMetadata propMeta, ChangeListener<Object> listener) {
            super(propMeta, null);
            super.addValueListener(listener);
            this.textField = textField;
            this.toggleButton = toggleButton;
            this.selectedInstances = selectedInstances;
            this.propMeta = propMeta;
            
            initialize();
        }
        
        private void initialize() {
            //
            // Text field
            //
            // For SQE tests
            textField.setId(EditorUtils.toDisplayName(propMeta.getName().getName()) + " Value"); //NOI18N
            EventHandler<ActionEvent> valueListener = event -> {
                if (isHandlingError()) {
                    // Event received because of focus lost due to error dialog
                    return;
                }
                String valStr = textField.getText();
                if (valStr == null || valStr.isEmpty()) {
                    if (toggleButton.isSelected()) {
                        updateFromTextField = true;
                        toggleButton.setSelected(false);
                        updateFromTextField = false;
                    }
                    userUpdateValueProperty(null);
                    return;
                }
                textField.selectAll();
                double valDouble;
                try {
                    valDouble = Double.parseDouble(valStr);
                } catch (NumberFormatException e) {
                    handleInvalidValue(valStr, textField);
                    return;
                }
                if (!((DoublePropertyMetadata) getPropertyMeta()).isValidValue(valDouble)) {
                    handleInvalidValue(valDouble, textField);
                    return;
                }
                if (!toggleButton.isSelected()) {
                    updateFromTextField = true;
                    toggleButton.setSelected(true);
                    updateFromTextField = false;
                }
                userUpdateValueProperty(valDouble);
            };
            setNumericEditorBehavior(this, textField, valueListener, false);
            // Override default promptText
            textField.setPromptText(""); //NOI18N

            textField.setOnMouseClicked(t -> ConstraintEditor.this.toggleButton.setSelected(true));

            //
            // Toggle button
            //
            assert propMeta instanceof DoublePropertyMetadata;

            toggleButton.selectedProperty().addListener((ChangeListener<Boolean>) (ov, prevSel, newSel) -> {
//                System.out.println("toggleButton : selectedProperty changed!");
                if (isUpdateFromModel() || updateFromTextField) {
                    // nothing to do
                    return;
                }

                // Update comes from toggleButton.
                if (newSel) {
                    // Anchor selected : compute its value from the selected node
                    double anchor = 0;
                    String propName = ConstraintEditor.this.propMeta.getName().toString();
                    switch (propName) {
                        // For the moment, we don't support multi-selection with different anchors:
                        // the first instance anchor only is used.
                        case topAnchorPropName:
                            anchor = EditorUtils.computeTopAnchor(getFirstInstance());
                            break;
                        case rightAnchorPropName:
                            anchor = EditorUtils.computeRightAnchor(getFirstInstance());
                            break;
                        case bottomAnchorPropName:
                            anchor = EditorUtils.computeBottomAnchor(getFirstInstance());
                            break;
                        case leftAnchorPropName:
                            anchor = EditorUtils.computeLeftAnchor(getFirstInstance());
                            break;
                        default:
                            assert false;
                    }
                    textField.setText(EditorUtils.valAsStr(anchor));
                    userUpdateValueProperty(getValue());
                } else {
                    // Anchor unselected
                    textField.setText(null);
                    userUpdateValueProperty(null);
                }
            });
        }

        @Override
        public Node getValueEditor() {
            // Should not be called
            assert false;
            return null;
        }

        @Override
        public Object getValue() {
            String valStr = textField.getText();
            if (valStr == null || valStr.isEmpty()) {
                return null;
            }
            return Double.valueOf(valStr);
        }

        @Override
        public void setValue(Object value) {
            setValueGeneric(value);
            if (isSetValueDone()) {
                return;
            }

            if (value == null) {
                toggleButton.setSelected(false);
                textField.setText(null);
            } else {
                assert (value instanceof Double);
                toggleButton.setSelected(true);
                textField.setText(EditorUtils.valAsStr(value));
                if (textField.isFocused()) {
                    textField.positionCaret(textField.getLength());
                }
            }
        }

        public void reset(Set<FXOMInstance> selectedInstances, ValuePropertyMetadata propMeta) {
            super.reset(propMeta, null);
            this.selectedInstances = selectedInstances;
            this.propMeta = propMeta;
            textField.setPromptText(null);
        }

        @Override
        protected void valueIsIndeterminate() {
            handleIndeterminate(textField);
        }

        public boolean isAnchorEnabled() {
            return valueProperty().getValue() != null;
        }

        @Override
        public void requestFocus() {
            EditorUtils.doNextFrame(() -> textField.requestFocus());
        }
        
        private FXOMInstance getFirstInstance() {
            return (FXOMInstance) selectedInstances.toArray()[0];
        }
    }
}
