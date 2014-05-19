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
package login;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import login.model.User;

/**
 * Profile Controller.
 */
public class ProfileController extends AnchorPane {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;
    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;
    @FXML
    private TextField user;
    @FXML
    private TextField phone;
    @FXML
    private TextField email;
    @FXML
    private TextArea address;
    @FXML
    private CheckBox subscribed;
    @FXML
    private Hyperlink logout;
    @FXML 
    private Button update;
    
    @FXML 
    private Label success;
    
    private Main application;
    
    public void setApp(Main application){
        this.application = application;
        User loggedUser = application.getLoggedUser();
        user.setText(loggedUser.getId());
        email.setText(loggedUser.getEmail());
        phone.setText(loggedUser.getPhone());
        if (loggedUser.getAddress() != null) {
            address.setText(loggedUser.getAddress());
        }
        subscribed.setSelected(loggedUser.isSubscribed());
        success.setOpacity(0);
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
    }
    
    public void processLogout(ActionEvent event) {
        if (application == null){
            return;
        }
        
        application.userLogout();
    }
    
    public void processUpdate(ActionEvent event) {
        if (application == null){
            animateMessage();
            return;
        }
        User loggedUser = application.getLoggedUser();
        loggedUser.setEmail(email.getText());
        loggedUser.setPhone(phone.getText());
        loggedUser.setSubscribed(subscribed.isSelected());
        loggedUser.setAddress(address.getText());
        animateMessage();
    }

    private void animateMessage() {
        FadeTransition ft = new FadeTransition(Duration.millis(3000), success);
        ft.setFromValue(0.0);
        ft.setToValue(1);
        ft.play();
    }
}
