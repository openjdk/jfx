/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package hello.dialog.fxml;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FXMLSampleDialog extends Application {
    
    @FXML private DialogPane dialogPane;
    @FXML private ButtonType helpButtonType;
    
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override public void start(Stage primaryStage) {
        showAndWait();
    }
    
    public void showAndWait() {
        try {
            FXMLLoader.load(getClass().getResource("FXMLSampleDialog.fxml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML private void initialize() {
        Dialog<Person> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        
        dialog.setResultConverter(buttonType -> {
            return buttonType == ButtonType.OK ? 
                    new Person(firstNameField.getText(), 
                               lastNameField.getText(), 
                               emailField.getText()) : null;
        });
        
        // add custom event handler to the help button
        Node helpButton = dialogPane.lookupButton(helpButtonType);
        ((Button)helpButton).addEventFilter(ActionEvent.ACTION, event -> {
            System.out.println("It's ok to ask for help!");
            event.consume(); // this stops the hello.dialog from hiding
            // show stuff without dismissing hello.dialog
        });
        
        // show hello.dialog and wait for result
        dialog.showAndWait().ifPresent(result -> System.out.println("Result is " + result));
    }

    public static class Person {
        public String firstName;
        public String lastName;
        public String email;

        public Person(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }
        @Override public String toString() {
            return "Person [firstName: " + firstName + ", lastName: " + lastName + ", email: " + email + "]";
        }
    }
}
