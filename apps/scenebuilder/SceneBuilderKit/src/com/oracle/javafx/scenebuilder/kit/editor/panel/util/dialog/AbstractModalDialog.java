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
package com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog;

import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 * 
 */
public abstract class AbstractModalDialog extends AbstractFxmlWindowController {
    
    public enum ButtonID { OK, CANCEL, ACTION }
    
    private final Window owner;
    private final URL contentFxmlURL;
    private final ResourceBundle contentResources;
    private Parent contentRoot;
    private ButtonID clickedButtonID;
    private boolean showDefaultButton;
    private ButtonID defaultButtonID = ButtonID.OK;
    private boolean focusTraversableButtons;
    
    /* 
     * The following members should be considered as 'private'.
     * They are 'protected' only to please the FXML loader.
     */
    @FXML protected StackPane contentPane;
    @FXML protected Button okButton;
    @FXML protected Button cancelButton;
    @FXML protected Button actionButton;
    @FXML protected Pane okParent;
    @FXML protected Pane actionParent;
    @FXML protected ImageView imageView;
    @FXML protected Pane imageViewParent;
    
    /*
     * Public
     */

    public AbstractModalDialog(URL contentFxmlURL, ResourceBundle contentResources, Window owner) {
        super(getContainerFxmlURL(), I18N.getBundle());
        this.owner = owner;
        this.contentFxmlURL = contentFxmlURL;
        this.contentResources = contentResources;
        assert contentFxmlURL != null;
    }
    
    public Parent getContentRoot() {
        
        if (contentRoot == null) {
            final FXMLLoader loader = new FXMLLoader();

            loader.setController(this);
            loader.setLocation(contentFxmlURL);
            loader.setResources(contentResources);
            try {
                contentRoot = (Parent)loader.load();
                controllerDidLoadContentFxml();
            } catch (IOException x) {
                contentRoot = null;
                throw new RuntimeException("Failed to load " + contentFxmlURL.getFile(), x); //NOI18N
            }
        }
        
        return contentRoot;
    }

    public final ButtonID showAndWait() {
//        center();
        clickedButtonID = ButtonID.CANCEL;
        getStage().showAndWait();
        return clickedButtonID;
    }
    
    public String getTitle() {
        return getStage().getTitle();
    }
    
    public void setTitle(String title) {
        getStage().setTitle(title);
    }

    public String getOKButtonTitle() {
        return getOKButton().getText();
    }
    
    public void setOKButtonTitle(String title) {
        getOKButton().setText(title);
    }
    
    public String getCancelButtonTitle() {
        return getCancelButton().getText();
    }
    
    public void setCancelButtonTitle(String title) {
        getCancelButton().setText(title);
    }
    
    public String getActionButtonTitle() {
        return getActionButton().getText();
    }
    
    public void setActionButtonTitle(String title) {
        getActionButton().setText(title);
    }
    
    public boolean isOKButtonVisible() {
        return getOKButton().getParent() != null;
    }
    
    public void setOKButtonVisible(boolean visible) {
        if (visible != isOKButtonVisible()) {
            if (visible) {
                assert getOKButton().getParent() == null;
                getOKParent().getChildren().add(getOKButton());
            } else {
                assert getOKButton().getParent() == getOKParent();
                getOKParent().getChildren().remove(getOKButton());
            }
        }
    }
    
    public boolean isActionButtonVisible() {
        return getActionButton().getParent() != null;
    }
    
    public void setActionButtonVisible(boolean visible) {
        if (visible != isActionButtonVisible()) {
            if (visible) {
                assert getActionButton().getParent() == null;
                getActionParent().getChildren().add(getActionButton());
            } else {
                assert getActionButton().getParent() == getActionParent();
                getActionParent().getChildren().remove(getActionButton());
            }
        }
    }
    
    public void setOKButtonDisable(boolean disable) {
        getOKButton().setDisable(disable);
    }
    
    public void setActionButtonDisable(boolean disable) {
        getActionButton().setDisable(disable);
    }

    public void setShowDefaultButton(boolean show) {
        showDefaultButton = show;
        updateButtonState();
    }
    
    public void setDefaultButtonID(ButtonID buttonID) {
        defaultButtonID = buttonID;
        updateButtonState();
    }
    
    public boolean isImageViewVisible() {
        return getImageView().getParent() != null;
    }
    
    public void setImageViewVisible(boolean visible) {
        if (visible != isImageViewVisible()) {
            if (visible) {
                assert getImageView().getParent() == null;
                imageViewParent.getChildren().add(getImageView());
            } else {
                assert getImageView().getParent() == imageViewParent;
                imageViewParent.getChildren().remove(getImageView());
            }
        }
    }
    
    public Image getImageViewImage() {
        return getImageView().getImage();
    }
    
    public void setImageViewImage(Image image) {
        getImageView().setImage(image);
    }
    
    // On Mac the FXML defines the 3 buttons as non focus traversable.
    // However for complex dialogs such a Preferences, Code Skeleton and
    // Preview Background Color we'd better have them focus traversable hence
    // this method.
    public void setButtonsFocusTraversable() {
        if (EditorPlatform.IS_MAC) {
            getOKButton().setFocusTraversable(true);
            getCancelButton().setFocusTraversable(true);
            getActionButton().setFocusTraversable(true);
            focusTraversableButtons = true;
        }
    }

    /*
     * To be subclassed
     */
    
    protected abstract void controllerDidLoadContentFxml();
    
    /*
     * To be subclassed #2
     * 
     * Those methods cannot be declared abstract else we fall in RT-34146.
     */
    @FXML
    protected void okButtonPressed(ActionEvent e) {
        
    }
    
    @FXML
    protected void cancelButtonPressed(ActionEvent e) {
        
    }
    
    @FXML
    protected void actionButtonPressed(ActionEvent e) {
        
    }
    
    
    /*
     * AbstractWindowController
     */

    @Override
    protected void controllerDidCreateStage() {
        if (this.owner == null) {
            // Dialog will be appliation modal
            getStage().initModality(Modality.APPLICATION_MODAL);
        } else {
            // Dialog will be window modal
            getStage().initOwner(this.owner);
            getStage().initModality(Modality.WINDOW_MODAL);
        }
    }
    
    /*
     * AbstractFxmlWindowController
     */

    @Override
    protected void controllerDidLoadFxml() {
        assert contentPane != null;
        assert okButton != null;
        assert cancelButton != null;
        assert actionButton != null;
        assert imageView != null;
        assert okParent != null;
        assert actionParent != null;
        assert imageViewParent != null;
        assert okButton.getParent() == okParent;
        assert actionButton.getParent() == actionParent;
        assert imageView.getParent() == imageViewParent;
        
        final EventHandler<ActionEvent> callUpdateButtonID = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                updateButtonID(e);
            }
        };
        okButton.addEventHandler(ActionEvent.ACTION, callUpdateButtonID);
        cancelButton.addEventHandler(ActionEvent.ACTION, callUpdateButtonID);
        actionButton.addEventHandler(ActionEvent.ACTION, callUpdateButtonID);
        
        contentPane.getChildren().add(getContentRoot());
        
        // By default, action button and image view are not visible
        setActionButtonVisible(false);
        setImageViewVisible(false);
        
        // Setup default state and focus
        updateButtonState();
        
        // Size everything
        getStage().sizeToScene();
    }

    @Override
    public void onCloseRequest(WindowEvent event) {
        // Closing the window is equivalent to clicking the Cancel button
        cancelButtonPressed(null);
    }

    
    /*
     * Private
     */
    private static URL getContainerFxmlURL() {
        final String fxmlName;
        
        if (EditorPlatform.IS_WINDOWS) {
            fxmlName = "AbstractModalDialogW.fxml"; //NOI18N
        } else {
            fxmlName = "AbstractModalDialogM.fxml"; //NOI18N
        }
        
        return AbstractModalDialog.class.getResource(fxmlName);
    }
    
    private Button getOKButton() {
        getRoot(); // Force fxml loading
        return okButton;
    }
    
    private Button getCancelButton() {
        getRoot(); // Force fxml loading
        return cancelButton;
    }
    
    private Button getActionButton() {
        getRoot(); // Force fxml loading
        return actionButton;
    }
    
    private Pane getOKParent() {
        getRoot(); // Force fxml loading
        return okParent;
    }
    
    private Pane getActionParent() {
        getRoot(); // Force fxml loading
        return actionParent;
    }
    
    private ImageView getImageView() {
        getRoot(); // Force fxml loading
        return imageView;
    }
    
    private void updateButtonID(ActionEvent t) {
        assert t != null;
        
        final Object source = t.getSource();
        if (source == getCancelButton()) {
            clickedButtonID = AbstractModalDialog.ButtonID.CANCEL;
        } else if (source == getOKButton()) {
            clickedButtonID = AbstractModalDialog.ButtonID.OK;
        } else if (source == getActionButton()) {
            clickedButtonID = AbstractModalDialog.ButtonID.ACTION;
        } else {
            throw new IllegalArgumentException("Bug"); //NOI18N
        }
    }

    private void updateButtonState() {
        getOKButton().setDefaultButton(false);
        getCancelButton().setDefaultButton(false);
        getActionButton().setDefaultButton(false);
        
        // To stick to OS specific "habits" we set a default button on Mac as on
        // Win and Linux we use focus to mark a button as the default one (then
        // you can tab navigate from a button to another, something which is
        // disabled on Mac.
        // However on Mac and for complex dialogs (Preferences, Code Skeleton,
        // Background Color) we apply the Win/Linux scheme: buttons are focus
        // traversable and there's no default one. The user needs to press Space
        // to take action with the focused button. We take this approach because
        // complex dialogs contain editable field that take focus and that
        // interferes with a button set as default one.
        // See DTL-5333.
        if (showDefaultButton) {
            switch(defaultButtonID) {
                case OK:
                    if (EditorPlatform.IS_MAC && ! focusTraversableButtons) {
                        getOKButton().setDefaultButton(true);
                    } else {
                        getOKButton().requestFocus();
                    }
                    break;
                case CANCEL:
                    if (EditorPlatform.IS_MAC && ! focusTraversableButtons) {
                        getCancelButton().setDefaultButton(true);
                    } else {
                        getCancelButton().requestFocus();
                    }
                    break;
                case ACTION:
                    if (EditorPlatform.IS_MAC && ! focusTraversableButtons) {
                        getActionButton().setDefaultButton(true);
                    } else {
                        getActionButton().requestFocus();
                    }
                    break;
            }
        }
    }

}
