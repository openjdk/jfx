/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.editors;

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.EffectPickerController;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

public class ImageControl extends GridPane {

    @FXML
    private Button editor_button;
    @FXML
    private Label editor_label;
    @FXML
    private TextField editor_textfield;

//    private final StringProperty value = new SimpleStringProperty();
    private final ObjectProperty<Image> value = new SimpleObjectProperty<>();
    private final EffectPickerController effectPickerController;

    public ImageControl(EffectPickerController effectPickerController,
            String labelString, Image initVal) {
        this.effectPickerController = effectPickerController;
        initialize(labelString, initVal);
        if (EditorPlatform.IS_MAC) {
            editor_button.setManaged(false);
            editor_button.setVisible(false);
        }
    }

    public ObjectProperty<Image> valueProperty() {
        return value;
    }

    public Image getValue() {
        return value.get();
    }

    public void setValue(Image image) {
        value.set(image);
    }

    public Button getButton() {
        return editor_button;
    }

    public TextField getTextField() {
        return editor_textfield;
    }

    @FXML
    void textfieldOnAction(ActionEvent e) {
        final String location = editor_textfield.getText();
        try {
            final URI uri = new URI(location);
            final File file = new File(uri);
            if (file.exists()) {
                final Image image = new Image(uri.toURL().toExternalForm());
                // First update the model
                setValue(image);
                // Then notify the controller a change occured
                effectPickerController.incrementRevision();
            } else {
                effectPickerController.getEffectPickerDelegate().handleError(
                        "log.warning.image.location.does.not.exist", location);
            }
            editor_textfield.selectAll();
        } catch (URISyntaxException | MalformedURLException ex) {
            effectPickerController.getEffectPickerDelegate().handleError(
                    "log.warning.image.location.does.not.exist", location);
        } finally {
            e.consume();
        }
    }

    @FXML
    void buttonOnAction(ActionEvent e) {
        try {
            final String[] extensions = {"*.jpg", "*.jpeg", "*.png", "*.gif"}; //NOI18N
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            I18N.getString("inspector.select.image"),
                            Arrays.asList(extensions)));
            final File file = fileChooser.showOpenDialog(getScene().getWindow());
            if ((file == null)) {
                return;
            }
            String url;
            url = file.toURI().toURL().toExternalForm();
            final Image image = new Image(url);
            // First update the model
            setValue(image);
            // Then notify the controller a change occured
            effectPickerController.incrementRevision();
        } catch (MalformedURLException ex) {
            Logger.getLogger(ImageControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            e.consume();
        }
    }

    private void initialize(String labelString, Image initVal) {

        final URL layoutURL = ImageControl.class.getResource("ImageControl.fxml"); //NOI18N
        try (InputStream is = layoutURL.openStream()) {
            FXMLLoader loader = new FXMLLoader();
            loader.setController(this);
            loader.setRoot(this);
            loader.setLocation(layoutURL);
            Parent p = (Parent) loader.load(is);
            assert p == this;
        } catch (IOException x) {
            throw new RuntimeException(x);
        }

        editor_label.setText(labelString);
        editor_textfield.setText(initVal == null ? "" : Deprecation.getUrl(initVal)); //NOI18N
        setValue(initVal);
    }
}
