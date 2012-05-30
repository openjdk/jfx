/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import com.sun.javafx.scene.control.behavior.ButtonBehavior;



/**
 * A Skin for command Buttons.
 */
public class ButtonSkin extends LabeledSkinBase<Button, ButtonBehavior<Button>> {

    public ButtonSkin(Button button) {
        super(button, new ButtonBehavior<Button>(button));

        // Register listeners
        registerChangeListener(button.defaultButtonProperty(), "DEFAULT_BUTTON");
        registerChangeListener(button.cancelButtonProperty(), "CANCEL_BUTTON");
        registerChangeListener(button.focusedProperty(), "FOCUSED");

        if (getSkinnable().isDefaultButton()) {
            /*
            ** were we already the defaultButton, before the listener was added?
            ** don't laugh, it can happen....
            */
            setDefaultButton(true);
        }

        if (getSkinnable().isCancelButton()) {
            /*
            ** were we already the defaultButton, before the listener was added?
            ** don't laugh, it can happen....
            */
            setCancelButton(true);
        }       

        button.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {

                ContextMenu cm = getSkinnable().getContextMenu();
                if (cm != null) {
                    if (!cm.isShowing()) {
                        cm.show(getSkinnable(), Side.RIGHT, 0, 0);
                        Utils.addMnemonics(cm, getSkinnable().getScene());
                    }
                    else {
                        cm.hide();
                        Utils.removeMnemonics(cm, getSkinnable().getScene());
                    }
                }
            }
        });
    }


    @Override protected void handleControlPropertyChanged(String p) {
        super.handleControlPropertyChanged(p);
        if (p == "DEFAULT_BUTTON") {
            setDefaultButton(getSkinnable().isDefaultButton());
        }
        else if (p == "CANCEL_BUTTON") {
            setCancelButton(getSkinnable().isCancelButton());
        }
        else if (p == "FOCUSED") {
           if (!getSkinnable().isFocused()) {
                ContextMenu cm = getSkinnable().getContextMenu();
                if (cm != null) {
                    if (cm.isShowing()) {
                        cm.hide();
                        Utils.removeMnemonics(cm, getSkinnable().getScene());
                    }
                }
           }
        }
    }

    Runnable defaultButtonRunnable = new Runnable() {
            public void run() {
                if (!getSkinnable().isDisabled()) {
                    getSkinnable().fire();
                }
            }
        };

    Runnable cancelButtonRunnable = new Runnable() {
            public void run() {                
                if (!getSkinnable().isDisabled()) {
                    getSkinnable().fire();
                }
            }
        };

    private void setDefaultButton(boolean value) {

        KeyCode acceleratorCode = KeyCode.ENTER;
        KeyCodeCombination acceleratorKeyCombo = 
                new KeyCodeCombination(acceleratorCode);

        if (value == false) {
            /*
            ** first check of there's a default button already
            */
            Runnable oldDefault = getSkinnable().getParent().getScene().getAccelerators().get(acceleratorKeyCombo);
            if (!defaultButtonRunnable.equals(oldDefault)) {
                /*
                ** is it us?
                */
                getSkinnable().getParent().getScene().getAccelerators().remove(acceleratorKeyCombo);
            }
        }
        else {
            /*
            ** first check of there's a default button already
            */
            Runnable oldDefault = getSkinnable().getParent().getScene().getAccelerators().get(acceleratorKeyCombo);
        }
        getSkinnable().getParent().getScene().getAccelerators().put(acceleratorKeyCombo, defaultButtonRunnable);
    }



    private void setCancelButton(boolean value) {

        KeyCode acceleratorCode = KeyCode.ESCAPE;
        KeyCodeCombination acceleratorKeyCombo =
                new KeyCodeCombination(acceleratorCode);

        if (value == false) {
            /*
            ** first check of there's a default button already
            */
            Runnable oldDefault = getSkinnable().getParent().getScene().getAccelerators().get(acceleratorKeyCombo);
            if (!defaultButtonRunnable.equals(oldDefault)) {
                /*
                ** is it us?
                */
                getSkinnable().getParent().getScene().getAccelerators().remove(acceleratorKeyCombo);
            }
        }
        else {
            /*
            ** first check of there's a default button already
            */
            Runnable oldDefault = getSkinnable().getParent().getScene().getAccelerators().get(acceleratorKeyCombo);
        }
        getSkinnable().getParent().getScene().getAccelerators().put(acceleratorKeyCombo, cancelButtonRunnable);
    }

}
