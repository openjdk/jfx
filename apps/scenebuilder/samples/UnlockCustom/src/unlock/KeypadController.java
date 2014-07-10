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
package unlock;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.util.Callback;

/**
 * The controller for the custom Keypad component - see 'Keypad.fxml' 
 * and 'Keypad.java'.
 */
public final class KeypadController implements Initializable {
            
    @FXML //  fx:id="del"
    private Button del; // Value injected by FXMLLoader

    @FXML //  fx:id="ok"
    private Button ok; // Value injected by FXMLLoader

    @FXML //  fx:id="display"
    private PasswordField display; // Value injected by FXMLLoader

    private Callback<String, Boolean> validateCallback = null;
    
    // Handler for Button[Button[id=null, styleClass=button]] onAction
    // Handler for Button[fx:id="del"] onAction
    // Handler for Button[fx:id="ok"] onAction
    public void keyPressed(ActionEvent event) {
        // handle the event here
        if (event.getTarget() instanceof Button) {
            if (event.getTarget() == del && !display.getText().isEmpty()) {                
                delete();
            } else if (event.getTarget() == ok) {
                validateCallback.call(display.getText());
                display.setText("");
            } else if (event.getTarget() != del) {
                append(((Button)event.getTarget()).getText());
            }
            event.consume();
        }
    }
    
    private void delete() {
        display.setText(display.getText().substring(0, display.getText().length() -1));
    }
    
    private void append(String s) {
        String text = display.getText();
        if (text == null) text = "";
        display.setText(text+s);
    }

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert del != null : "fx:id=\"del\" was not injected: check your FXML file 'Keypad.fxml'.";
        assert ok != null : "fx:id=\"ok\" was not injected: check your FXML file 'Keypad.fxml'.";
        assert display != null : "fx:id=\"password\" was not injected: check your FXML file 'Keypad.fxml'.";
    }
    
    void setValidateCallback(Callback<String,Boolean> validateCB) {
        validateCallback = validateCB;
    }
    
}
