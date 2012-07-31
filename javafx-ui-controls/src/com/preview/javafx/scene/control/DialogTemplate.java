/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.preview.javafx.scene.control;

import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import static com.preview.javafx.scene.control.DialogResources.*;
import static com.preview.javafx.scene.control.Dialogs.DialogType.*;
import com.preview.javafx.scene.control.Dialogs.DialogResponse;
import com.preview.javafx.scene.control.Dialogs.DialogType;
import com.preview.javafx.scene.control.Dialogs.DialogOptions;

/**
 * 
 * @param <T> The type for user input
 */
class DialogTemplate<T> {
    private static enum DialogStyle {
        SIMPLE,
        ERROR,
        INPUT;
    }
    
    
    // Defines max dialog width.
    final static int DIALOG_WIDTH = 516;
    
    // According to the UI spec, the width of the main message text in the upper
    // panel should be 426 pixels.
    private static int MAIN_TEXT_WIDTH = 400;
    
    private FXDialog dialog;
    private VBox contentPane;
    
    private DialogType dialogType = INFORMATION;
    private final DialogOptions options;
    private DialogResponse userResponse = DialogResponse.CLOSED;
    
    private DialogStyle style;
    
    // for user input dialogs (textfield / choicebox / combobox)
    private T initialInputValue;
    private List<T> inputChoices;
    private T userInputResponse;
    
    
    // masthead
    private String mastheadString;
    private BorderPane mastheadPanel;
    private ImageView mastheadIcon;
    private UITextArea mastheadTextArea;
    
    // center
    private Pane centerPanel;
    private String contentString = null;
    
    // Buttons
    private ObservableList<Button> buttons;
    private static final String okBtnStr = "common.ok.btn";
    private static final String yesBtnStr = "common.yes.btn";
    private static final String noBtnStr = "common.no.btn";
    private static final String cancelBtnStr = "common.cancel.btn";
    private static final String detailBtnStr = "common.detail.button";
    
    // This is used in the exception dialog only.
    private Throwable throwable = null;    
    
    // Visual indication of security level alert - either high or medium.
    // Located in the lower left corner at the bottom of the dialog.
    private final String SECURITY_ALERT_HIGH = "security.alert.high.image";
    private final String SECURITY_ALERT_LOW  = "security.alert.low.image";
    private ImageView securityIcon;

    // These are for security dialog only.
    private String[] alertStrs;
    private String[] infoStrs;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/    

    DialogTemplate(Stage owner, String title, String masthead, DialogOptions options) {
        this.dialog = new FXDialog(title, owner, true);
        
        this.contentPane = new VBox();
        this.dialog.setContentPane(contentPane);

        this.mastheadString = masthead;
        this.options = options;
    }
    
    
    
    
    /***************************************************************************
     *                                                                         *
     * Dialog construction API                                                 *
     *                                                                         *
     **************************************************************************/
    
    void setSimpleContent(String contentString, DialogType dialogType) {
        setSimpleContent(contentString, dialogType, null, true);
    }
    
    void setSimpleContent(String contentString, DialogType dialogType,
                          String infoString, boolean useWarningIcon) {
        this.style = DialogStyle.SIMPLE;
        this.contentString = contentString;
        
        this.dialogType = dialogType == null ? WARNING : dialogType;
        if (infoString != null) {
            String[] strs = { infoString };
            if (useWarningIcon) {
                this.alertStrs = strs;
            } else {
                this.infoStrs = strs;
            }
        }

        contentPane.getChildren().add(createMasthead());
        contentPane.getChildren().add(createCenterPanel());

        Pane bottomPanel = createBottomPanel();
        if (bottomPanel != null) {
            contentPane.getChildren().add(bottomPanel);
        }

        dialog.setResizable(false);
    }
    
    void setErrorContent(String contentString, Throwable throwable) {
        this.style = DialogStyle.ERROR;
        this.contentString = contentString;
        this.throwable = throwable;

        this.dialogType = DialogType.ERROR;

        contentPane.getChildren().add(createMasthead());
        contentPane.getChildren().add(createCenterPanel());
        
        Pane bottomPanel = createBottomPanel();
        if (bottomPanel != null && bottomPanel.getChildren().size() > 0) {
            contentPane.getChildren().add(bottomPanel);
        }

        dialog.setResizable(false);
    }
    
    void setInputContent(String message, T initialValue, List<T> choices) {
        this.style = DialogStyle.INPUT;
        this.contentString = message;
        this.initialInputValue = initialValue;
        this.inputChoices = choices;
        
        contentPane.getChildren().add(createMasthead());
        contentPane.getChildren().add(createCenterPanel());

        Pane bottomPanel = createBottomPanel();
        if (bottomPanel != null) {
            contentPane.getChildren().add(bottomPanel);
        }

        dialog.setResizable(false);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * 'Public' API                                                            *
     *                                                                         *
     **************************************************************************/
    
    public FXDialog getDialog() {
        return dialog;
    }
    
    public void show() {
        dialog.showAndWait();
    }
    
    public void hide() {
        dialog.hide();
    }
    
    /**
     * gets the response from the user.
     * @return the response
     */
    public DialogResponse getResponse() {
        return userResponse;
    }
    
    public T getInputResponse() {
        return userInputResponse;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Implementation                                                          *
     *                                                                         *
     **************************************************************************/

    /*
     * top part of the dialog contains short informative message, and either
     * an icon, or the text is displayed over a watermark image
     */
    private Pane createMasthead() {
        mastheadPanel = new BorderPane();
        mastheadPanel.getStyleClass().add("top-panel");

        // Create panel with text area and icon or just a background image:
        // Create topPanel's components.  UITextArea determines
        // the size of the dialog by defining the number of columns
        // based on font size.
        mastheadTextArea = new UITextArea(MAIN_TEXT_WIDTH);
        mastheadTextArea.getStyleClass().add("masthead-label-1");

        VBox mastheadVBox = new VBox();
        mastheadVBox.setAlignment(Pos.CENTER_LEFT);
        mastheadTextArea.setText(mastheadString);
        mastheadTextArea.setAlignment(Pos.CENTER_LEFT);
        mastheadVBox.getChildren().add(mastheadTextArea);

        mastheadPanel.setLeft(mastheadVBox);
        BorderPane.setAlignment(mastheadVBox, Pos.CENTER_LEFT);
        mastheadIcon = dialogType == null ? getIcon("java48.image") : dialogType.getImage();
        mastheadPanel.setRight(mastheadIcon);

        return mastheadPanel;
    }

    private Pane createCenterPanel() {
        centerPanel = new VBox();
        centerPanel.getStyleClass().add("center-panel");

        BorderPane contentPanel = new BorderPane();
        contentPanel.getStyleClass().add("center-content-panel");
        VBox.setVgrow(contentPanel, Priority.ALWAYS);

        Node content = createCenterContent();
        if (content != null) {
            contentPanel.setCenter(content);
            contentPanel.setPadding(new Insets(0, 0, 12, 0));
        }

        FlowPane buttonsPanel = new FlowPane(6, 0) {
            @Override protected void layoutChildren() {
                resizeButtons();
                super.layoutChildren();
            }
        };
        buttonsPanel.getStyleClass().add("button-bar");

        // Create buttons from okBtnStr and cancelBtnStr strings.
        buttonsPanel.getChildren().addAll(createButtons());
            
        if (contentPanel.getChildren().size() > 0) {
            centerPanel.getChildren().add(contentPanel);
        }

        BorderPane bottomPanel = new BorderPane();
        bottomPanel.getStyleClass().add("center-bottom-panel");
        bottomPanel.setRight(buttonsPanel);
        centerPanel.getChildren().add(bottomPanel);

        return centerPanel;
    }
    
    private Node createCenterContent() {
        if (style == DialogStyle.SIMPLE) {
            if (contentString != null) {
                UITextArea ta = new UITextArea(contentString);
                ta.getStyleClass().add("center-content-area");
                ta.setAlignment(Pos.TOP_LEFT);
                return ta;
            }
        } else if (style == DialogStyle.INPUT) {
            Control inputControl = null;
            if (inputChoices == null || inputChoices.isEmpty()) {
                // no input constraints, so use a TextField
                final TextField textField = new TextField();
                textField.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        userInputResponse = (T) textField.getText();
                        hide();
                    }
                });
                if (initialInputValue != null) {
                    textField.setText(initialInputValue.toString());
                }
                inputControl = textField;
            } else {
                // input method will be constrained to the given choices
                ChangeListener<T> changeListener = new ChangeListener<T>() {
                    @Override public void changed(ObservableValue<? extends T> ov, T t, T t1) {
                        userInputResponse = t1;
                    }
                };
                
                if (inputChoices.size() > 10) {
                    // use ComboBox
                    ComboBox<T> comboBox = new ComboBox<T>();
                    comboBox.getItems().addAll(inputChoices);
                    comboBox.getSelectionModel().select(initialInputValue);
                    comboBox.getSelectionModel().selectedItemProperty().addListener(changeListener);
                    inputControl = comboBox;
                } else {
                    // use ChoiceBox
                    ChoiceBox<T> choiceBox = new ChoiceBox<T>();
                    choiceBox.getItems().addAll(inputChoices);
                    choiceBox.getSelectionModel().select(initialInputValue);
                    choiceBox.getSelectionModel().selectedItemProperty().addListener(changeListener);
                    inputControl = choiceBox;
                }
            }
            
            HBox hbox = new HBox(10);
            
            if (contentString != null && ! contentString.isEmpty()) {
                Label label = new Label(contentString);
                hbox.getChildren().add(label);
                
            }
            
            if (inputControl != null) {
                hbox.getChildren().add(inputControl);
            }
            
            return hbox;
        }
        
        return null;
    }
    
    private List<Button> createButtons() {
        buttons = FXCollections.observableArrayList();
        
        if (style == DialogStyle.INPUT) {
            buttons.addAll(createButton(okBtnStr, DialogResponse.OK, true, false),
                            createButton(cancelBtnStr, DialogResponse.CANCEL, false, true));
        } else {
            if (DialogType.ERROR == dialogType && throwable != null) {
                // we've got an error dialog, which has 'OK' and 'Details..' buttons
                buttons.addAll(createButton(okBtnStr, DialogResponse.OK, true, false));

                Button detailsBtn = new Button((detailBtnStr == null) ? "" : getMessage(detailBtnStr));
                detailsBtn.setOnAction(exceptionDetailsHandler);
                buttons.add(detailsBtn);
            } else if (options == DialogOptions.OK) {
                buttons.addAll(createButton(okBtnStr, DialogResponse.OK, true, false));
            } else if (options == DialogOptions.OK_CANCEL) {
                buttons.addAll(createButton(okBtnStr, DialogResponse.OK, true, false),
                            createButton(cancelBtnStr, DialogResponse.CANCEL, false, true));
            } else if (options == DialogOptions.YES_NO) {
                buttons.addAll(createButton(yesBtnStr, DialogResponse.YES, true, false),
                            createButton(noBtnStr, DialogResponse.NO, false, true));
            } else if (options == DialogOptions.YES_NO_CANCEL) {
                buttons.addAll(createButton(yesBtnStr, DialogResponse.YES, true, false),
                            createButton(noBtnStr, DialogResponse.NO, false, true),
                            createButton(cancelBtnStr, DialogResponse.CANCEL, false, false));
            }
        }
        
        return buttons;
    }
    
    private Button createButton(String extLabel, DialogResponse response, boolean isDefault, boolean isCancel) {
        Button btn = new Button((extLabel == null) ? "" : getMessage(extLabel));
        btn.setOnAction(createButtonHandler(response));
        btn.setDefaultButton(isDefault);
        btn.setCancelButton(isCancel);
        
        return btn;
    }

    /*
     * bottom panel contains icon indicating the security alert level,
     * two bullets with most significant security warnings,
     * link label - to view more details about security warnings.
     */
    private Pane createBottomPanel() {
        if (alertStrs == null && infoStrs == null) return null;
            
        HBox bottomPanel = new HBox();
        bottomPanel.getStyleClass().add("bottom-panel");
        
        // Icon 32x32 pixels with indication of secutiry alert - high/low

        // If there are no messages in securityAlerts, show
        // SECURITY_ALERT_LOW icon in the lower left corner of
        // security dialog.
        String imageFile = SECURITY_ALERT_HIGH;

        if (alertStrs == null || alertStrs.length == 0) {
            imageFile = SECURITY_ALERT_LOW;
        }
        securityIcon = getIcon(imageFile);

        // Add icon to the bottom panel.
        bottomPanel.getChildren().add(securityIcon);

        // If there are no alerts (alertStrs is null, or length is 0),
        // then we should show only first message from infoStrs.
        // this is how it will work for security dialog...
        int textAreaWidth = 333;
        UITextArea bulletText = new UITextArea(textAreaWidth);
        bulletText.getStyleClass().add("bottom-text");

        if ((alertStrs == null || alertStrs.length == 0)
            && infoStrs != null && infoStrs.length != 0) {
            // If there are no alerts, use first string from the infoStrs.
            bulletText.setText((infoStrs[0] != null) ? infoStrs[0] : " ");
        } else if (alertStrs != null && alertStrs.length != 0) {
            // If there are any alerts, use first string from alertStrs.
            bulletText.setText((alertStrs[0] != null) ? alertStrs[0] : " ");
        }

        bottomPanel.getChildren().add(bulletText);

//        if (moreInfoLbl != null) {
//            bottomPanel.getChildren().add(moreInfoLbl);
//        }

        return bottomPanel;
    }

    /*
     * According to UI guidelines, all buttons should have the same length.
     * This function is to define the longest button in the array of buttons
     * and set all buttons in array to be the length of the longest button.
     */
    private void resizeButtons() {
        // Find out the longest button...
        double widest = 50;
        for (int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            if (btn == null) continue;
            widest = Math.max(widest, btn.prefWidth(-1));
        }
        
        // ...and set all buttons to be this width
        for (int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            if (btn == null) continue;
            btn.setPrefWidth(btn.isVisible() ? widest : 0);
        }
    }
    
    private EventHandler<ActionEvent> createButtonHandler(final DialogResponse response) {
        return new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent ae) {
                userResponse = response;

                // hide the dialog.  We'll return from the dialog,
                // and who ever called it will retrieve user's answer
                // and will dispose of the dialog after that.
                hide();
            }
        };
    }
    
    private EventHandler<ActionEvent> exceptionDetailsHandler = new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent ae) {
            if (throwable != null) {
                new ExceptionDialog(dialog, throwable).show();
                return;
            }
        }
    };
}
