/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.app.message;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.messagelog.MessageLogEntry;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 *
 */
public class MessageBarController extends AbstractFxmlPanelController {

    private MessagePopupController messageWindowController;
    private int previousTotalNumOfMessages = 0;

    @FXML
    private HBox messageBox;
    @FXML
    private Button messageButton;
    @FXML
    private Label messageLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private StackPane selectionBarHost;

    private final ImageView fileDirtyImage;
    private Tooltip statusLabelTooltip = null;

    public MessageBarController(EditorController editorController) {
        super(MessageBarController.class.getResource("MessageBar.fxml"), I18N.getBundle(), editorController); //NOI18N

        // Initialize file dirty image
        final URL fileDirtyURL = MessageBarController.class.getResource("file-dirty.png"); //NOI18N
        assert fileDirtyURL != null;
        fileDirtyImage = new ImageView(new Image(fileDirtyURL.toExternalForm()));
    }

    public StackPane getSelectionBarHost() {
        return selectionBarHost;
    }

    /*
     * Action Handlers
     */
    @FXML
    void onOpenCloseAction(ActionEvent e) {
        if (messageWindowController == null) {
            messageWindowController = new MessagePopupController(getEditorController());
        }
        if (messageWindowController.isWindowOpened()) {
            messageWindowController.closeWindow();
        } else {
            messageWindowController.openWindow(messageBox);
        }
    }

    /*
     * AbstractPanelController
     */
    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
        // Nothing to do
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
        // Nothing to do
    }

    @Override
    protected void jobManagerRevisionDidChange() {
        // Nothing to do
    }

    @Override
    protected void editorSelectionDidChange() {
        // Nothing to do
    }

    /*
     * AbstractFxmlPanelController
     */
    @Override
    protected void controllerDidLoadFxml() {

        // Sanity checks
        assert messageBox != null;
        assert messageLabel != null;
        assert statusLabel != null;
        assert selectionBarHost != null;

        // Remove fake data
        messageLabel.setText(""); //NOI18N
        statusLabel.setText(""); //NOI18N
        messageButton.setVisible(false);
        
        // Listens to the message log 
        getEditorController().getMessageLog().revisionProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                        messageLogDidChange();
                    }
                });
        getEditorController().getMessageLog().numOfWarningMessagesProperty().addListener(
                new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                        String numberOfMessages = Integer.toString(t1.intValue());
                        if (t1.intValue() > 9) {
                            numberOfMessages = "*"; //NOI18N
                        }
                        messageButton.setText(numberOfMessages);
                    }
                });
        
        statusLabelTooltip = statusLabel.getTooltip();
        
        // Update output components
        messageLogDidChange();
    }

    /*
     * Private
     */
    public void setDocumentDirty(boolean isDirty) {
        if (statusLabel != null) {
            if (isDirty) {
                statusLabel.setGraphic(fileDirtyImage);
                statusLabel.setTooltip(statusLabelTooltip);
            } else {
                statusLabel.setGraphic(null);
                statusLabel.setTooltip(null);
            }
        }
    }
    
    private void messageLogDidChange() {
        assert messageLabel != null;
        
        final MessageLogEntry entry
                = getEditorController().getMessageLog().getYoungestEntry();
        int logSize = getEditorController().getMessageLog().getEntries().size();

        // When a old message is dismissed the message log changes but there's
        // no need to display anything in the message bar.
        if (entry != null && logSize > previousTotalNumOfMessages) {
            // We mask the host
            getSelectionBarHost().getChildren().get(0).setVisible(false);
            getSelectionBarHost().setManaged(false);
            messageLabel.setManaged(true);
            
            // Styling message area according severity
            setStyle(entry.getType());
                        
            // If no message panel is defined nor in use we compute message bar
            // button. But as soon as a message panel is opened it means one or
            // more message is being displayed so we do not alter the message button.
            if (messageWindowController == null || ! messageWindowController.isWindowOpened()) {
                if (getEditorController().getMessageLog().getWarningEntryCount() == 0) {
                    messageButton.setVisible(false);
                    messageButton.setManaged(false);
                } else {
                    messageButton.setVisible(true);
                    messageButton.setManaged(true);
                }
            }
            
            // Displaying the message
            messageLabel.setText(entry.getText());
            messageLabel.setVisible(true);
            
            // We go back to the host after a given time
            // The fading based code commented out below do not seem to be
            // reliable in the sense that when sending slowly 10 messages in a
            // row you get 1 or 2 where the display time isn't correct, fading
            // duration abrupt, ...
            // For now we use a timer to switch back abruptly. But fact is the
            // time the message remains visible do not seem more stable than
            // with the fading transition.
            final Timer timer = new Timer(true);
            final TimerTask timerTask = new TimerTask() {

                @Override
                public void run() {
                    Platform.runLater(new Runnable() {

                        @Override
                        public void run() {
                            messageLabel.setVisible(false);
                            messageLabel.setGraphic(null);
                            messageLabel.setManaged(false);
                            if (getEditorController().getMessageLog().getWarningEntryCount() == 0) {
                                messageButton.setVisible(false);
                                messageButton.setManaged(false);
                            }
                                resetStyle();
                            getSelectionBarHost().setManaged(true);
                            getSelectionBarHost().getChildren().get(0).setOpacity(1.0);
                            getSelectionBarHost().getChildren().get(0).setVisible(true);
                        }
                    });
                    // I don't need to use the timer later on so by cancelling
                    // it here I free resources that otherwise would prevent
                    // the JVM from exiting.
                    timer.cancel();
                }
            };
            timer.schedule(timerTask, 2000); // milliseconds
            
//            FadeTransition showHost = new FadeTransition(Duration.seconds(1), messageLabel);
//            showHost.setFromValue(1.0);
//            showHost.setToValue(0.0);
//            showHost.setDelay(Duration.seconds(3));
//            showHost.setOnFinished(new EventHandler<ActionEvent>() {
//
//                @Override
//                public void handle(ActionEvent t) {
//                    messageLabel.setVisible(false);
//                    messageButton.setVisible(false);
//                    if (entry.getType() == MessageLogEntry.Type.INFO) {
//                        messageLabel.getStyleClass().remove("message-label-info");
//                    } else {
//                        messageLabel.getStyleClass().remove("message-label-warning-and-error");
//                    }
//                    getSelectionBarHost().getChildren().get(0).setOpacity(1.0);
//                    getSelectionBarHost().getChildren().get(0).setVisible(true);
//                }
//            });
//            showHost.play();
        } else if (getEditorController().getMessageLog().getEntryCount() == 0) {
            if (messageWindowController != null && messageWindowController.isWindowOpened()) {
                messageWindowController.closeWindow();
                messageButton.setVisible(false);
                messageButton.setManaged(false);
            }
        }

        previousTotalNumOfMessages = logSize;
    }
    
    private void resetStyle() {
        // We clear all previous use, the sole way to control what's going on.
        messageLabel.getStyleClass().removeAll("message-info"); //NOI18N
        messageLabel.getStyleClass().removeAll("message-warning"); //NOI18N
        statusLabel.getStyleClass().removeAll("message-info"); //NOI18N
        statusLabel.getStyleClass().removeAll("message-warning"); //NOI18N
        messageButton.getStyleClass().removeAll("message-info"); //NOI18N
        messageButton.getStyleClass().removeAll("message-warning"); //NOI18N
    }
    
    private void setStyle(MessageLogEntry.Type type) {
        resetStyle();
        
        switch (type) {
            case INFO:
                messageLabel.getStyleClass().add("message-info"); //NOI18N
                statusLabel.getStyleClass().add("message-info"); //NOI18N
                messageButton.getStyleClass().add("message-info"); //NOI18N
                break;
            case WARNING:
                messageLabel.getStyleClass().add("message-warning"); //NOI18N
                statusLabel.getStyleClass().add("message-warning"); //NOI18N
                messageButton.getStyleClass().add("message-warning"); //NOI18N
                break;
            default:
                break;
        }
    }
}
