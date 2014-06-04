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
package helloswingnode;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javax.swing.SwingUtilities;

/**
 * The controller for our 'HelloSwingNode' application, see
 * 'HelloSwingNode.fxml'. This class has all the logic to instantiate the 2
 * buttons (FX button and Swing button) as well as the logic to handle the click
 * action on the FX button.
 */
public class HelloSwingNodeController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML                           // fx:id="fxButton"
    private Button fxButton;        // Value injected by FXMLLoader

    @FXML                           // fx:id="swingNode"
    private SwingNode swingNode;    // Value injected by FXMLLoader

    private SwingButton swingButton;
    private boolean enabled;

    /**
     * Called by the FXMLLoader when initialization is complete
     * @param url
     * @param rb
     */
    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert swingNode != null : "fx:id=\"swingNode\" was not injected: check your FXML file 'HelloSwingNode.fxml'.";
        assert fxButton != null : "fx:id=\"fxButton\" was not injected: check your FXML file 'HelloSwingNode.fxml'.";

        createSwingContent(swingNode);
        enabled = true;
    }

    /**
     * Called when the FX Button is fired. The click action alternatively
     * disables/enables the Swing Button
     *
     * @param event the action event
     */
    @FXML
    void handleButtonAction(ActionEvent event) {
        if (enabled) {
            // if Swing Button is currently enabled, then disable it, 
            // and update text and tooltip of FX Button so it can be used now 
            // to re-enable the Swing button
            SwingUtilities.invokeLater(() -> {
                swingButton.setEnabled(false);
            });
            enabled = false;
            fxButton.setText("Enable Swing Button");
            fxButton.getTooltip().setText("Click this button to enable the Swing button");
        } else {
            // if Swing Button is currently disabled, then enable it, 
            // and update text and tooltip of FX Button so it can be used now 
            // to disable the Swing button
            SwingUtilities.invokeLater(() -> {
                swingButton.setEnabled(true);
            });
            enabled = true;
            fxButton.setText("Disable Swing Button");
            fxButton.getTooltip().setText("Click this button to disable the Swing button");
        }
    }

    // Instantiates our SwingButton and sets it as the content of the SwingNode.
    // The SwingButton will in turn be able to disable/enable the FX button (passed as an arg).
    // All this has to be done on the Swing event thread.
    private void createSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            swingButton = new SwingButton(fxButton);
            swingNode.setContent(swingButton);
        });
    }
}
