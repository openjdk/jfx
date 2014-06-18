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

import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Lighting;

/**
 * Effect path item for the lighting effect.
 */
public class LightingPathItem extends EffectPathItem {

    private final RadioMenuItem bumpMenuItem = new RadioMenuItem("BumpInput"); //NOI18N
    private final RadioMenuItem contentMenuItem = new RadioMenuItem("ContentInput"); //NOI18N
    private final ToggleGroup inputToggleGroup = new ToggleGroup();
    private EffectPathItem bumpInputPathItem;
    private EffectPathItem contentInputPathItem;

    public LightingPathItem(EffectPickerController epc, Effect effect, EffectPathItem hostPathItem) {
        super(epc, effect, hostPathItem);
        assert effect instanceof javafx.scene.effect.Lighting;
        initialize();
    }

    @Override
    EffectPathItem getSelectedInputPathItem() {
        if (bumpMenuItem.isSelected()) {
            return bumpInputPathItem;
        } else {
            assert contentMenuItem.isSelected() == true;
            return contentInputPathItem;
        }
    }

    @Override
    void setSelectedInputEffect(Effect input) {
        if (bumpMenuItem.isSelected()) {
            setBumpInput(input);
        } else {
            assert contentMenuItem.isSelected() == true;
            setContentInput(input);
        }
    }

    void setBumpInputPathItem(EffectPathItem epi) {
        bumpInputPathItem = epi;
    }

    void setContentInputPathItem(EffectPathItem epi) {
        contentInputPathItem = epi;
    }

    Effect getBumpInput() {
        return ((Lighting) effect).getBumpInput();
    }

    void setBumpInput(Effect input) {
        ((Lighting) effect).setBumpInput(input);
    }

    Effect getContentInput() {
        return ((Lighting) effect).getContentInput();
    }

    void setContentInput(Effect input) {
        ((Lighting) effect).setContentInput(input);
    }

    private void initialize() {
        // Add Select Input Menu
        final Menu inputMenu = new Menu("Select Input"); //NOI18N
        bumpMenuItem.setToggleGroup(inputToggleGroup);
        bumpMenuItem.setOnAction(event -> {
            toggle_button.setText(getSimpleName() + " (BumpInput)"); //NOI18N
            effectPickerController.updateUI(LightingPathItem.this);
        });
        contentMenuItem.setToggleGroup(inputToggleGroup);
        contentMenuItem.setOnAction(event -> {
            toggle_button.setText(getSimpleName() + " (ContentInput)"); //NOI18N
            effectPickerController.updateUI(LightingPathItem.this);
        });

        inputMenu.getItems().addAll(bumpMenuItem, contentMenuItem);
        menu_button.getItems().add(0, inputMenu);
        menu_button.getItems().add(1, new SeparatorMenuItem());

        // BumpInput selected at init time
        toggle_button.setText(getSimpleName() + " (BumpInput)"); //NOI18N
        bumpMenuItem.setSelected(true);
        contentMenuItem.setSelected(false);
    }
}
