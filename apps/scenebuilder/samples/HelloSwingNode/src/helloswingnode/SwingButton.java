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

import java.awt.event.ActionEvent;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javax.swing.JButton;

/**
 * Our SwingButton is a JButton with some logic to alternatively disable/enable
 * the FX Button passed as an argument.
 */
public class SwingButton extends JButton {

    private final Button fxButton;

    public SwingButton(Button fxb) {

        // initially, this button is set up to disable the FX button when clicked
        super("Disable FX Button");
        setActionCommand("disable");
        fxButton = fxb;

        //Listen for actions on this button.
        addActionListener((ActionEvent e) -> {
            toggleFXButton(e.getActionCommand());
        });

        setToolTipText("Click this button to disable the FX button.");
    }

    private void toggleFXButton(String command) {

        if ("disable".equals(command)) {
            // this button is currently set to disable the FX button
            // access to the FX Scenegraph has to be done in the FX event thread
            Platform.runLater(() -> {
                fxButton.setDisable(true);
            });

            setText("Enable FX Button");
            setActionCommand("enable");
            setToolTipText("Click this button to enable the FX button.");

        } else {
            // this button is currently set to enable the FX button
            // access to the FX Scenegraph has to be done in the FX event thread
            Platform.runLater(() -> {
                fxButton.setDisable(false);
            });
            setText("Disable FX Button");
            setActionCommand("disable");
            setToolTipText("Click this button to disable the FX button.");

        }
    }
}
