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

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog.ButtonID;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AlertDialog;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal.CssPropAuthorInfo;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * Base class for all property editors.
 *
 *
 */
public abstract class PropertyEditor extends Editor {

    // Layout format for editors. See DTL-5727 for details.
    public enum LayoutFormat {

        SIMPLE_LINE_CENTERED,
        SIMPLE_LINE_BOTTOM,
        SIMPLE_LINE_TOP,
        SIMPLE_LINE_NO_NAME,
        DOUBLE_LINE
    }
    public final static LayoutFormat DEFAULT_LAYOUT_FORMAT = LayoutFormat.SIMPLE_LINE_CENTERED;
    private static final Image cogIcon = new Image(
            InspectorPanelController.class.getResource("images/cog.png").toExternalForm()); //NOI18N
    private static final Image cssIcon = new Image(
            InspectorPanelController.class.getResource("images/css-icon.png").toExternalForm()); //NOI18N
    private Hyperlink propName;
    private HBox propNameNode;
    private Tooltip tooltip;
    private MenuButton menu;
    private ValuePropertyMetadata propMeta = null;
    private Set<Class<?>> selectedClasses;
    private Object defaultValue;
    private final Set<ChangeListener<Object>> valueListeners = new HashSet<>();
    private final Set<ChangeListener<Boolean>> editingListeners = new HashSet<>();
    private final Set<ChangeListener<Boolean>> indeterminateListeners = new HashSet<>();
    private ChangeListener<String> navigateRequestListener = null;
    private EventHandler<?> commitListener;
    // State properties
    private final BooleanProperty disableProperty = new SimpleBooleanProperty(false);
    private boolean binding = false;
    private final BooleanProperty indeterminateProperty = new SimpleBooleanProperty(false);
    private boolean ruledByCss = false;
    private CssPropAuthorInfo cssInfo;
    private MenuItem showCssMenuItem = null;
    private boolean updateFromModel = true; // Value update from the model
    private final ObjectProperty<Object> valueProperty = new SimpleObjectProperty<>();
    private final BooleanProperty editingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty invalidValueProperty = new SimpleBooleanProperty(false);
    private final StringProperty navigateRequestProperty = new SimpleStringProperty();
    private boolean handlingError = false;
    private LayoutFormat layoutFormat = DEFAULT_LAYOUT_FORMAT;

    private final MenuItem resetvalueMenuItem = new MenuItem(I18N.getString("inspector.editors.resetvalue"));
    private FadeTransition fadeTransition = null;
    private boolean genericModesHandled = false;

    public PropertyEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        this.propMeta = propMeta;
        initialize();
        setSelectedClasses(selectedClasses);
        setPropNamePrettyText();
        tooltip = new Tooltip();
        propName.setTooltip(tooltip);
        setTooltipText();
        this.defaultValue = propMeta.getDefaultValueObject();
    }

    // Special constructor for elements which are not JavaFX properties (e.g fx:id, controllerClass)
    // In this case, propMeta and selectedClasses are null.
    public PropertyEditor(String name, String defaultValue) {
        initialize();
        propName.setText(name);
        this.defaultValue = defaultValue;
    }

    private void initialize() {
        // Create a property link with a pretty name (e.g. layoutX ==> Layout X)
        propName = new Hyperlink();
        propName.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    if (propMeta != null && selectedClasses != null) {
                        if (selectedClasses.size() <= 1) {
                            EditorUtils.openUrl(selectedClasses, propMeta);
                        }
                    } else {
                        // Special case for non-properties (fx:id, ...)
                        EditorPlatform.open("http://docs.oracle.com/javafx/2/api/javafx/fxml/doc-files/introduction_to_fxml.html"); //NOI18N
                    }
                    // Selection of multiple different classes ==> no link
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        });
        propName.getStyleClass().add("property-link"); //NOI18N
        propName.setFocusTraversable(false);

        // The hyperlink is wrapped in an HBox so that the HBox grows in width, not the hyperlink
        propNameNode = new HBox();
        propNameNode.getChildren().add(propName);
        // default layout: simple line, centered vertically, propName aligned on right
        propNameNode.setAlignment(Pos.CENTER_RIGHT);

        EditorUtils.makeWidthStretchable(propNameNode);
    }

    public HBox getPropNameNode() {
        return propNameNode;
    }

    public PropertyName getPropertyName() {
        if (propMeta == null) {
            return null;
        }
        return propMeta.getName();
    }

    public String getPropertyNameText() {
        return propName.getText();
    }

    public void setPropertyText(String text) {
        propName.setText(text);
    }

    @Override
    public final MenuButton getMenu() {
        if (menu == null) {
            menu = new MenuButton();
            menu.disableProperty().bind(disableProperty);
            menu.setGraphic(new ImageView(cogIcon));
            menu.getStyleClass().add("cog-button"); //NOI18N
            menu.setOpacity(0);
            if (fadeTransition == null) {
                fadeTransition = new FadeTransition(Duration.millis(500), menu);
            }
            EditorUtils.handleFading(fadeTransition, menu, disableProperty);
            EditorUtils.handleFading(fadeTransition, propNameNode, disableProperty);
            menu.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        // focused
                        EditorUtils.fadeTo(fadeTransition, 1);
                    } else {
                        // focus lost
                        EditorUtils.fadeTo(fadeTransition, 0);
                    }
                }
            });
            menu.getItems().add(resetvalueMenuItem);
            resetvalueMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    setValue(defaultValue);
                    if (getCommitListener() != null) {
                        getCommitListener().handle(null);
                    }
                }
            });
        }
        return menu;
    }

    public void replaceMenuItem(MenuItem item, MenuItem newItem) {
        MenuButton cogMenu = getMenu();
        int index = cogMenu.getItems().indexOf(item);
        cogMenu.getItems().set(index, newItem);
    }

    public void setPropertyMetadata(ValuePropertyMetadata propMeta) {
        this.propMeta = propMeta;
    }

    public void addValueListener(ChangeListener<Object> listener) {
        if (!valueListeners.contains(listener)) {
            valueProperty().addListener(listener);
            valueListeners.add(listener);
        }
    }

    public void removeValueListener(ChangeListener<Object> listener) {
        valueProperty().removeListener(listener);
        valueListeners.remove(listener);
    }

    public void addEditingListener(ChangeListener<Boolean> listener) {
        if (!editingListeners.contains(listener)) {
            editingProperty().addListener(listener);
            editingListeners.add(listener);
        }
    }

    public void removeEditingListener(ChangeListener<Boolean> listener) {
        editingProperty().removeListener(listener);
        editingListeners.remove(listener);
    }

    public void addIndeterminateListener(ChangeListener<Boolean> listener) {
        if (!indeterminateListeners.contains(listener)) {
            indeterminateProperty().addListener(listener);
            indeterminateListeners.add(listener);
        }
    }

    public void removeIndeterminateListener(ChangeListener<Boolean> listener) {
        indeterminateProperty().removeListener(listener);
        indeterminateListeners.remove(listener);
    }

    public void addNavigateListener(ChangeListener<String> listener) {
        // We should have a single listener here
        if (navigateRequestListener == null) {
            navigateRequestProperty.addListener(listener);
            navigateRequestListener = listener;
        }
    }

    public void removeNavigateListener(ChangeListener<String> listener) {
        navigateRequestProperty.removeListener(listener);
        navigateRequestListener = null;
    }

    @Override
    public void removeAllListeners() {
        Set<ChangeListener<Object>> valListeners = new HashSet<>(valueListeners);
        for (ChangeListener<Object> listener : valListeners) {
            removeValueListener(listener);
        }
        Set<ChangeListener<Boolean>> editListeners = new HashSet<>(editingListeners);
        for (ChangeListener<Boolean> listener : editListeners) {
            removeEditingListener(listener);
        }
        Set<ChangeListener<Boolean>> indetermListeners = new HashSet<>(indeterminateListeners);
        for (ChangeListener<Boolean> listener : indetermListeners) {
            removeIndeterminateListener(listener);
        }
        removeNavigateListener(navigateRequestListener);
    }

    /*
     * Abstract methods
     */
    public abstract Object getValue();

    public abstract void setValue(Object value);

    protected abstract void valueIsIndeterminate();

    public abstract void requestFocus();

    public void setValueGeneric(Object value) {
        // Should be called (first line) from editors setValue()
//        System.out.println(getPropertyNameText() + " - setValue() to : " + value);
        if (!isUpdateFromModel()) {
            // User updated the value from this editor: nothing to do.
            return;
        }
        invalidValueProperty.setValue(false);
        valueProperty.setValue(value);
        resetMenuUpdate(value);
//        cssMenuUpdate();
        if (isRuledByCss()) {
            addCssVisual();
        } else {
            removeCssVisual();
        }
        if (!(value instanceof String)) {
            return;
        }
        String val = (String) value;

        // Handle generic binding case
        if (isBindingExpression(val)) {
            binding = true;
        }
    }

    private void resetMenuUpdate(Object value) {
        // "Reset value" menu item update
        if (value == null) {
            if (defaultValue == null) {
                resetvalueMenuItem.setDisable(true);
            }
        } else if (value.equals(defaultValue)) {
            resetvalueMenuItem.setDisable(true);
        } else {
            resetvalueMenuItem.setDisable(false);
        }
    }

    private void cssMenuUpdate() {
        // "Show css" menu item update
        if (!isRuledByCss()) {
            getMenu().getItems().remove(showCssMenuItem);
            showCssMenuItem = null;
        }
    }

    protected boolean isSetValueDone() {
        boolean done = !isHandlingError() && (isBinding() || isEditing());
        return done;
    }

    /*
     * State properties
     * 
     */
    public boolean isDisabled() {
        return disableProperty.getValue();
    }

    public void setDisable(boolean disabled) {
        disableProperty.setValue(disabled);
    }

    public ObservableBooleanValue disableProperty() {
        return disableProperty;
    }

    public boolean isBinding() {
        return binding;
    }

    public boolean isIndeterminate() {
        return indeterminateProperty.getValue();
    }

    public void setIndeterminate(boolean indeterminate) {
        if (!indeterminateProperty.getValue() && indeterminate) {
            valueIsIndeterminate();
        }
        indeterminateProperty.setValue(indeterminate);
    }

    public boolean isRuledByCss() {
        return ruledByCss;
    }

    public void setRuledByCss(boolean ruledByCss) {
        this.ruledByCss = ruledByCss;
    }

    public void setCssInfo(CssPropAuthorInfo cssInfo) {
        this.cssInfo = cssInfo;
    }

    public boolean isUpdateFromModel() {
        return updateFromModel;
    }

    public void setUpdateFromModel(boolean updateFromModel) {
        this.updateFromModel = updateFromModel;
    }

    public boolean isEditing() {
        return editingProperty.getValue();
    }

    public boolean isInvalidValue() {
        return invalidValueProperty.getValue();
    }

    public boolean isHandlingError() {
        return handlingError;
    }

    // Reset everything so that the editor can be re-used for another property
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selClasses) {
        resetStates();
        this.propMeta = propMeta;
        setSelectedClasses(selClasses);
        setPropNamePrettyText();
        setTooltipText();
        this.defaultValue = propMeta.getDefaultValueObject();
    }

    public void reset(String name, String defaultValue) {
        resetStates();
        propName.setText(name);
        this.defaultValue = defaultValue;
    }

    public ObjectProperty<Object> valueProperty() {
        return valueProperty;
    }

    public LayoutFormat getLayoutFormat() {
        return layoutFormat;
    }

    public void setLayoutFormat(LayoutFormat layoutFormat) {
        this.layoutFormat = layoutFormat;
    }

    public void userUpdateValueProperty(Object value) {
        if (!isValueChanged(value)) {
            return;
        }
        invalidValueProperty.setValue(false);
        indeterminateProperty.setValue(false);
        valueProperty.setValue(value);
        resetMenuUpdate(value);
    }

    @SuppressWarnings("unchecked")
    boolean isValueChanged(Object value) {
        if (value instanceof List) {
            List<Object> valueList = (List<Object>) value;
            List<Object> valuePropertyList = (List<Object>) valueProperty.getValue();
            if (valueList.size() != valuePropertyList.size()) {
                return true;
            }
            boolean changed = false;
            for (int ii = 0; ii < valueList.size(); ii++) {
                assert valueProperty.getValue() instanceof List;
                if (!valueList.get(ii).equals(valuePropertyList.get(ii))) {
                    changed = true;
                }
            }
            return changed;
        } else {
            return isSimpleValueChanged(value);
        }
    }

    boolean isSimpleValueChanged(Object newVal) {
        Object oldVal = valueProperty.getValue();
        if (oldVal == null || newVal == null) {
            return newVal != oldVal;
        }
        return isIndeterminate() || !newVal.equals(oldVal);
    }

    public BooleanProperty editingProperty() {
        return editingProperty;
    }

    public BooleanProperty indeterminateProperty() {
        return indeterminateProperty;
    }

    public BooleanProperty invalidValueProperty() {
        return invalidValueProperty;
    }

    public StringProperty navigateRequestProperty() {
        return navigateRequestProperty;
    }

    protected static Node getBindingValueEditor(Node valueEditor, String bindingExp) {
        TextField bindingTf = new TextField();
        bindingTf.setText(bindingExp);
        bindingTf.setEditable(false);
//                bindingTf.getStyleClass().add("read-only"); //NOI18N
        HBox hbox = new HBox(5);
        EditorUtils.replaceNode(valueEditor, hbox, null);
        hbox.getChildren().addAll(new Label("${"), bindingTf, new Label("}")); //NOI18N
        return hbox;
    }

    protected static boolean isBindingExpression(String str) {
        return str.startsWith("${") && str.endsWith("}"); //NOI18N
    }

    private void addCssVisual() {
        if (!propNameNode.getStyleClass().contains("css-override")) { //NOI18N
            ImageView iv = new ImageView(cssIcon);
            propName.setGraphic(iv);
            propName.getStyleClass().add("css-background"); //NOI18N
            propNameNode.getStyleClass().add("css-override"); //NOI18N
            setTooltipText();

            // menu
            if (showCssMenuItem == null) {
                showCssMenuItem = new MenuItem(I18N.getString("inspector.css.showcss")); //NOI18N
                showCssMenuItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        assert cssInfo != null;
                        if (cssInfo.isInline()) {
                            // Jump to the "style" property
                            navigateRequestProperty.setValue("style"); //NOI18N
                            navigateRequestProperty.setValue(null);
                        } else {
                            // Open the css file
                            if (cssInfo.getMainUrl() != null) {
                                try {
                                    EditorPlatform.open(cssInfo.getMainUrl().toString());
                                } catch (IOException ex) {
                                    System.out.println(ex.getMessage() + ex);
                                }
                            }
                        }
                    }
                });
            }
            getMenu().getItems().add(showCssMenuItem);
        }
    }

    private void removeCssVisual() {
        if (propNameNode.getStyleClass().contains("css-override")) { //NOI18N
            propName.setGraphic(null);
            propName.getStyleClass().remove("css-background"); //NOI18N
            propNameNode.getStyleClass().remove("css-override"); //NOI18N
            setTooltipText();
        }
        cssMenuUpdate();
    }

    protected Node handleGenericModes(Node valueEditor) {
        if (!genericModesHandled) {
            if (isBinding()) {
                assert getValue() instanceof String;
                return getBindingValueEditor(valueEditor, (String) getValue());
            }
            if (isRuledByCss()) {
                addCssVisual();
            } else {
                removeCssVisual();
            }
            if (fadeTransition == null) {
                fadeTransition = new FadeTransition(Duration.millis(500), getMenu());
            }
            EditorUtils.handleFading(fadeTransition, valueEditor, disableProperty);
            genericModesHandled = true;
        }
        return valueEditor;
    }

    public ValuePropertyMetadata getPropertyMeta() {
        return propMeta;
    }

    protected void handleInvalidValue(Object value) {
        handleInvalidValue(value, null);
    }

    protected void handleInvalidValue(Object value, Node source) {
        if (isHandlingError()) {
            return;
        }
        invalidValueProperty.setValue(true);
        handlingError = true;
        if (source == null) {
            source = propName;
        }
        final AlertDialog alertDialog = new AlertDialog(source.getScene().getWindow());
        // Messages
        alertDialog.setTitle(I18N.getString("inspector.error.title"));
        alertDialog.setMessage(I18N.getString("inspector.error.message"));
        alertDialog.setDetails(I18N.getString("inspector.error.details", value, getPropertyNameText()));
        // OK button is "Previous value"
        alertDialog.setOKButtonVisible(true);
        alertDialog.setOKButtonTitle(I18N.getString("inspector.error.previousvalue"));
        // Cancel button
        alertDialog.setDefaultButtonID(AlertDialog.ButtonID.CANCEL);
        alertDialog.setShowDefaultButton(true);
        alertDialog.setCancelButtonTitle(I18N.getString("inspector.error.cancel"));

//        // Temp for debug
//        Thread.dumpStack();
        ButtonID buttonClicked = alertDialog.showAndWait();
        if (buttonClicked == ButtonID.OK) {
            setValue(valueProperty().getValue());
        }
        alertDialog.getStage().close();
        // Get the focus back
        requestFocus();
        handlingError = false;
    }

    private void resetStates() {
        // State properties
        disableProperty.setValue(false);
        binding = false;
        indeterminateProperty.setValue(false);
        ruledByCss = false;
        updateFromModel = true;
        editingProperty.setValue(false);
        invalidValueProperty.setValue(false);

        genericModesHandled = false;
        layoutFormat = DEFAULT_LAYOUT_FORMAT;
        cssInfo = null;
        removeCssVisual();
    }

    private void setSelectedClasses(Set<Class<?>> selClasses) {
        this.selectedClasses = selClasses;
        if (selClasses == null) {
            return;
        }
        if (selClasses.size() > 1) {
            // multi-selection of different classes ==> no link
            propName.setMouseTransparent(true);
        } else {
            propName.setMouseTransparent(false);
        }
    }

    private void setPropNamePrettyText() {
        propName.setText(EditorUtils.toDisplayName(getPropertyName().getName()));
    }

    private void setTooltipText() {
        if (isRuledByCss()) {
            tooltip.setText(I18N.getString("inspector.css.tooltip"));
        } else {
            tooltip.setText(getPropertyName().getName()); //NOI18N
        }
    }

    protected static void handleIndeterminate(Node node) {
        if (node instanceof TextField) {
            ((TextField) node).setText(""); //NOI18N
            ((TextField) node).setPromptText(Editor.INDETERMINATE_STR);
        } else if (node instanceof ComboBox) {
            ((ComboBox) node).getEditor().setText("");//NOI18N
            ((ComboBox) node).setPromptText(Editor.INDETERMINATE_STR);
        } else if (node instanceof ChoiceBox) {
            ((ChoiceBox) node).getSelectionModel().clearSelection();
        } else if (node instanceof CheckBox) {
            ((CheckBox) node).setIndeterminate(true);
        } else if (node instanceof MenuButton) {
            ((MenuButton) node).setText(Editor.INDETERMINATE_STR);
        }
    }

    protected void setTextEditorBehavior(PropertyEditor editor, Control control, EventHandler<ActionEvent> onActionListener) {
        setTextEditorBehavior(editor, control, onActionListener, true);
    }

    protected void setTextEditorBehavior(Control control, EventHandler<ActionEvent> onActionListener) {
        setTextEditorBehavior(null, control, onActionListener, true, true);
    }

    protected void setTextEditorBehavior(PropertyEditor editor, Control control,
            EventHandler<ActionEvent> onActionListener, boolean stretchable) {
        setTextEditorBehavior(editor, control, onActionListener, stretchable, true);
    }

    protected void setTextEditorBehavior(Control control, EventHandler<ActionEvent> onActionListener, boolean addFocusListener) {
        setTextEditorBehavior(null, control, onActionListener, true, addFocusListener);
    }

    protected void setTextEditorBehavior(PropertyEditor editor, Control control,
            EventHandler<ActionEvent> onActionListener, boolean stretchable, boolean addFocusListener) {
        setCommitListener(onActionListener);
        if (stretchable) {
            EditorUtils.makeWidthStretchable(control);
        }
        if (editor != null) {
            control.disableProperty().bind(editor.disableProperty());
        }
//        setEmptyPromptText(control);
        if (control instanceof TextField) {
            ((TextField) control).setOnAction(onActionListener);
        } else if (control instanceof ComboBoxBase) {
            ((ComboBoxBase<?>) control).setOnAction(onActionListener);
        }
        if (addFocusListener && control instanceof TextInputControl) {
            addFocusListener((TextInputControl) control, onActionListener);
        }

    }

    public EventHandler<?> getCommitListener() {
        return commitListener;
    }

    protected void setCommitListener(EventHandler<?> listener) {
        this.commitListener = listener;
    }

    protected void setNumericEditorBehavior(PropertyEditor editor, Control control, EventHandler<ActionEvent> onActionListener) {
        setNumericEditorBehavior(editor, control, onActionListener, true);
    }

    protected void setNumericEditorBehavior(PropertyEditor editor, Control control,
            EventHandler<ActionEvent> onActionListener, boolean stretchable) {
        setCommitListener(onActionListener);
        setTextEditorBehavior(editor, control, onActionListener, stretchable);
        control.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() != KeyCode.UP && event.getCode() != KeyCode.DOWN) {
                    return;
                }
                int incDecVal = 1;
                boolean shiftDown = event.isShiftDown();
                if (shiftDown) {
                    incDecVal = 10;
                }
                Object val = getValue();
                assert val != null;
                if (event.getCode() == KeyCode.UP) {
                    if (val instanceof Double) {
                        setValue(((Double) val) + incDecVal);
                    } else if (val instanceof Integer) {
                        setValue(((Integer) val) + incDecVal);
                    }
                } else if (event.getCode() == KeyCode.DOWN) {
                    if (val instanceof Double) {
                        setValue(((Double) val) - incDecVal);
                    } else if (val instanceof Integer) {
                        setValue(((Integer) val) - incDecVal);
                    }
                }
                event.consume();
            }
        });
    }

    private void addFocusListener(TextInputControl tic, EventHandler<ActionEvent> onActionListener) {
        tic.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (!newValue && tic.isEditable()) {
                    // focus lost
//                    System.out.println("editingProperty() set to false.");
                    editingProperty().setValue(false);
                } else if (newValue && tic.isEditable()) {
                    // got focus
//                    System.out.println("editingProperty() set to true.");
                    editingProperty().setValue(true);
                }
            }
        });
    }

}
