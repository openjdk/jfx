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
package com.oracle.javafx.scenebuilder.kit.util.control.effectpicker;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Effect path item.
 */
public abstract class EffectPathItem extends HBox {

    @FXML
    protected ImageView image_view;
    @FXML
    protected MenuButton menu_button;
    @FXML
    protected ToggleButton toggle_button;
    @FXML
    protected Tooltip tool_tip;
    @FXML
    public MenuItem delete_input_menuitem;
    @FXML
    public Menu replace_input_menu;

    protected final Effect effect;
    protected final EffectPathItem hostPathItem;
    protected final EffectPickerController effectPickerController;

    public EffectPathItem(EffectPickerController epc, Effect effect, EffectPathItem hostPathItem) {
        assert epc != null;
        assert effect != null;
        this.effectPickerController = epc;
        this.hostPathItem = hostPathItem;
        this.effect = effect;
        initialize();
    }

    public Effect getValue() {
        return effect;
    }

    public ToggleButton getToggleButton() {
        return toggle_button;
    }

    public EffectPathItem getHostPathItem() {
        return hostPathItem;
    }

    public abstract EffectPathItem getSelectedInputPathItem();

    public abstract void setSelectedInput(Effect input);

    public String getSimpleName() {
        return effect.getClass().getSimpleName();
    }

    @FXML
    void deleteEffect(ActionEvent event) {
        // Update model
        //---------------------------------------------
        if (getHostPathItem() != null) {
            // Delete this effect from the chain but relink it's input to its parent effect
            if (getSelectedInputPathItem() != null) {
                final Effect input = getSelectedInputPathItem().getValue();
                getHostPathItem().setSelectedInput(input);
            } else {
                getHostPathItem().setSelectedInput(null);
            }

        } else {
            // This is the root effect
            effectPickerController.setRootEffectProperty(null);
        }

        // Update UI
        //---------------------------------------------
        effectPickerController.updateUI();
    }

    @FXML
    void deleteEffectInput(ActionEvent event) {
        // Update model
        //---------------------------------------------
        setSelectedInput(null);

        // Update UI
        //---------------------------------------------
        effectPickerController.updateUI();
    }

    @FXML
    void replaceEffect(ActionEvent event) {
        final MenuItem menuItem = (MenuItem) event.getSource();
        final String text = menuItem.getText();
        // Update model
        //---------------------------------------------
        final Effect new_effect = Utils.newInstance(text);
        // Update effect input if any
        if (getSelectedInputPathItem() != null) {
            final Effect input = getSelectedInputPathItem().getValue();
            Utils.setDefaultInput(new_effect, input);
        }
        // Update effect host if any
        if (getHostPathItem() != null) {
            getHostPathItem().setSelectedInput(new_effect);
        } else {
            // This is the root effect
            effectPickerController.setRootEffectProperty(new_effect);
        }

        // Update UI
        //---------------------------------------------
        effectPickerController.updateUI();
    }

    @FXML
    void replaceEffectInput(ActionEvent event) {
        final MenuItem menuItem = (MenuItem) event.getSource();
        final String text = menuItem.getText();
        // Update model
        //---------------------------------------------
        final Effect new_effect = Utils.newInstance(text);
        setSelectedInput(new_effect);

        // Update UI
        //---------------------------------------------
        effectPickerController.updateUI();
    }

    @FXML
    void selectEffect(ActionEvent event) {
        effectPickerController.selectEffectPathItem(this);
    }

    private void initialize() {
        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(EffectPathItem.class.getResource("EffectPathItem.fxml")); //NOI18N
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(EffectPathItem.class.getName()).log(Level.SEVERE, null, ex);
        }

        assert image_view != null;
        assert menu_button != null;
        assert toggle_button != null;
        assert tool_tip != null;
        assert delete_input_menuitem != null;
        assert replace_input_menu != null;

        // Update ToggleButton
        final ToggleGroup toggleGroup = effectPickerController.getEffectToggleGroup();
        toggle_button.setToggleGroup(toggleGroup);
        toggle_button.setText(getSimpleName());

        // Update ImageView
        final URL url = EffectPathItem.class.getResource("images/" + effect.getClass().getSimpleName() + ".png"); //NOI18N
        final Image img = new Image(url.toExternalForm());
        image_view.setImage(img);
    }
}
