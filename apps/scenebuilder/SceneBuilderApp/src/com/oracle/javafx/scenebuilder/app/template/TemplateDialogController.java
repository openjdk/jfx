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
package com.oracle.javafx.scenebuilder.app.template;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp.ApplicationControlAction;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class TemplateDialogController extends AbstractModalDialog {

    @FXML //  fx:id="chooseButton"
    private Button chooseButton; // Value injected by FXMLLoader
    @FXML //  fx:id="detailsLabel"
    private Label detailsLabel; // Value injected by FXMLLoader
    @FXML //  fx:id="messageLabel"
    private Label messageLabel; // Value injected by FXMLLoader
    @FXML //  fx:id="locationTextField"
    private TextField locationTextField; // Value injected by FXMLLoader
    @FXML //  fx:id="nameTextField"
    private TextField nameTextField; // Value injected by FXMLLoader
//    private final Template template;
    private final ApplicationControlAction template;

    public TemplateDialogController(ApplicationControlAction template) {
        super(TemplateDialogController.class.getResource("TemplateDialog.fxml"), //NOI18N
                I18N.getBundle(), null);
        this.template = template;
    }

    /*
     * AbstractModalDialog
     */
    @Override
    protected void controllerDidLoadFxml() {
        super.controllerDidLoadFxml();
        setActionButtonVisible(false);
        setDefaultButtonID(AbstractModalDialog.ButtonID.OK);
        setShowDefaultButton(true);
        // Update title
        final String title = MessageFormat.format(
                I18N.getString("template.title.new.project"),
                FxmlTemplates.getTemplateName(template));
        getStage().setTitle(title);
    }

    @Override
    protected void controllerDidLoadContentFxml() {

        nameTextField.textProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                // Update details section
                updateDetails();
                // Update OK button
                updateOkButtonState();

            }
        });
        locationTextField.textProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                // Update details section
                updateDetails();
                // Update OK button
                updateOkButtonState();
            }
        });

        // Update name text field
        nameTextField.setText(FxmlTemplates.getTemplateName(template));

        // Update location text field
        final File initialDirectory = SceneBuilderApp.getSingleton().getNextInitialDirectory();
        if (initialDirectory != null) {
            locationTextField.setText(initialDirectory.getAbsolutePath());
        } else {
            locationTextField.setText(System.getProperty("user.home")); //NOI18N
        }
    }

    public File getNewProjectDirectory() {
        final String location = locationTextField.getText().trim();
        final String name = nameTextField.getText().trim();
        return new File(location, name);
    }

    @FXML
    public void chooseButtonPressed(ActionEvent e) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        final File selectedDir = directoryChooser.showDialog(getStage().getOwner());
        // Directory is null when pressing cancel button 
        if (selectedDir != null) {
            locationTextField.setText(selectedDir.getAbsolutePath());
        }
    }

    public void locationTextFieldOnAction(ActionEvent e) {
        locationTextField.selectAll();
    }

    public void nameTextFieldOnAction(ActionEvent e) {
        nameTextField.selectAll();
    }

    @Override
    protected void okButtonPressed(ActionEvent e) {
        final File newProjectDirectory = getNewProjectDirectory();
        // OK button is enabled => directory creation should succeed
        assert newProjectDirectory.mkdir();

        // Create FXML and resource files
        if (createTemplateFiles(newProjectDirectory)) {
            final String fxmlFileName = FxmlTemplates.getTemplateFileName(template);
            final File fxmlFile = new File(newProjectDirectory, fxmlFileName);

            final DocumentWindowController newTemplateWindow
                    = SceneBuilderApp.getSingleton().makeNewWindow();
            try {
                newTemplateWindow.loadFromFile(fxmlFile);
            } catch (IOException ex) {
                final ErrorDialog errorDialog = new ErrorDialog(null);
                errorDialog.setMessage(I18N.getString("alert.open.failure1.message", getStage().getTitle()));
                errorDialog.setDetails(I18N.getString("alert.open.failure1.details"));
                errorDialog.setDebugInfoWithThrowable(ex);
                errorDialog.setTitle(I18N.getString("alert.title.open"));
                errorDialog.showAndWait();
                SceneBuilderApp.getSingleton().documentWindowRequestClose(newTemplateWindow);
            }
            newTemplateWindow.openWindow();
        }

        SceneBuilderApp.getSingleton().updateNextInitialDirectory(newProjectDirectory);
        closeWindow();
    }

    @Override
    protected void cancelButtonPressed(ActionEvent e) {
        closeWindow();
    }

    @Override
    protected void actionButtonPressed(ActionEvent e) {
        // Should not be called because button is hidden
        throw new IllegalStateException();
    }

    private boolean createTemplateFiles(final File newProjectDirectory) {

        assert newProjectDirectory.exists();
        final String fxmlFileName = FxmlTemplates.getTemplateFileName(template);

        // Copy FXML file
        final InputStream fromFxmlFile = TemplateDialogController.class.getResourceAsStream(fxmlFileName);
        final File toFxmlFile = new File(newProjectDirectory, fxmlFileName);
        if (!copyFile(fromFxmlFile, toFxmlFile)) {
            return false;
        }

        // Copy resource files
        for (String resourceFileName : FxmlTemplates.getResourceFileNames(template)) {
            final InputStream fromResourceFile = TemplateDialogController.class.getResourceAsStream(resourceFileName);
            final File toResourceFile = new File(newProjectDirectory, resourceFileName);
            if (!copyFile(fromResourceFile, toResourceFile)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The OK button should be enabled according the validity of the user
     * inputs.
     */
    private void updateOkButtonState() {
        final String location = locationTextField.getText().trim();
        final File newProjectDirectory = getNewProjectDirectory();
        boolean disabled = false;

        // User inputs are declared *valid* if:
        // 1) "Location" matches an existing folder file
        // 2) "Location" / "Name" does not match any existing file
        // 3) "Name" is a valid file name (i.e. it does not contain any '/' e.g.)
        if (!new File(location).exists()) {
            messageLabel.setText(MessageFormat.format(
                    I18N.getString("template.location.does.not.exist"),
                    location));
            disabled = true;
        } else if (newProjectDirectory.exists()) {
            messageLabel.setText(MessageFormat.format(
                    I18N.getString("template.name.already.exists"),
                    newProjectDirectory.getName()));
            disabled = true;
        } else if (!isValidFileName(newProjectDirectory)) {
            messageLabel.setText(MessageFormat.format(
                    I18N.getString("template.cannot.create"),
                    newProjectDirectory.getAbsolutePath()));
            disabled = true;
        }
        if (disabled) {
            messageLabel.setVisible(true);
        } else {
            messageLabel.setVisible(false);
        }
        setOKButtonDisable(disabled);
    }

    private boolean isValidFileName(final File file) {
        boolean isValid = true;
        try {
            if (file.mkdir()) {
                // Code below to please findbugs
                if (file.delete() == false) {
                    isValid = false;
                }
            } else {
                isValid = false;
            }
        } catch (RuntimeException e) {
            isValid = false;
        }
        return isValid;
    }

    private void updateDetails() {
        final String location = locationTextField.getText().trim();
        final String name = nameTextField.getText().trim();
        final String path = location + File.separator + name + File.separator;
        final String fxmlFileName = FxmlTemplates.getTemplateFileName(template);
        final StringBuilder sb = new StringBuilder();
        // List fxml file
        sb.append(path);
        sb.append(fxmlFileName);
        // List resource files
        for (String resourceFileName : FxmlTemplates.getResourceFileNames(template)) {
            sb.append("\n"); //NOI18N
            sb.append(path);
            sb.append(resourceFileName);
        }
        detailsLabel.setText(sb.toString());
    }

    private boolean copyFile(final InputStream in, final File toFile, final CopyOption... options) {
        try {
            final Path target = Paths.get(toFile.toURI());
            Files.copy(in, target, options);
        } catch (IOException ioe) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setMessage(I18N.getString("alert.copy.failure.message", getStage().getTitle()));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.setTitle(I18N.getString("alert.title.copy"));
            errorDialog.showAndWait();
            return false;
        }
        return true;
    }
}
