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
import javafx.scene.effect.Blend;
import javafx.scene.effect.Effect;

/**
 * Effect path item for the blend effect.
 */
public class BlendPathItem extends EffectPathItem {

    private final RadioMenuItem topMenuItem = new RadioMenuItem("TopInput"); //NOI18N
    private final RadioMenuItem bottomMenuItem = new RadioMenuItem("BottomInput"); //NOI18N
    private final ToggleGroup inputToggleGroup = new ToggleGroup();
    private EffectPathItem topInputPathItem;
    private EffectPathItem bottomInputPathItem;

    public BlendPathItem(EffectPickerController epc, Effect effect, EffectPathItem hostPathItem) {
        super(epc, effect, hostPathItem);
        assert effect instanceof javafx.scene.effect.Blend;
        initialize();
    }

    @Override
    EffectPathItem getSelectedInputPathItem() {
        if (topMenuItem.isSelected()) {
            return topInputPathItem;
        } else {
            assert bottomMenuItem.isSelected() == true;
            return bottomInputPathItem;
        }
    }

    @Override
    void setSelectedInputEffect(Effect input) {
        if (topMenuItem.isSelected()) {
            setTopInput(input);
        } else {
            assert bottomMenuItem.isSelected() == true;
            setBottomInput(input);
        }
    }

    void setTopInputPathItem(EffectPathItem epi) {
        topInputPathItem = epi;
    }

    void setBottomInputPathItem(EffectPathItem epi) {
        bottomInputPathItem = epi;
    }

    Effect getTopInput() {
        return ((Blend) effect).getTopInput();
    }

    void setTopInput(Effect input) {
        ((Blend) effect).setTopInput(input);
    }

    Effect getBottomInput() {
        return ((Blend) effect).getBottomInput();
    }

    void setBottomInput(Effect input) {
        ((Blend) effect).setBottomInput(input);
    }

    private void initialize() {
        // Add Select Input Menu
        final Menu inputMenu = new Menu("Select Input"); //NOI18N
        topMenuItem.setToggleGroup(inputToggleGroup);
        topMenuItem.setOnAction(event -> {
            toggle_button.setText(getSimpleName() + " (TopInput)"); //NOI18N
            effectPickerController.updateUI(BlendPathItem.this);
        });
        bottomMenuItem.setToggleGroup(inputToggleGroup);
        bottomMenuItem.setOnAction(event -> {
            toggle_button.setText(getSimpleName() + " (BottomInput)"); //NOI18N
            effectPickerController.updateUI(BlendPathItem.this);
        });

        inputMenu.getItems().addAll(topMenuItem, bottomMenuItem);
        menu_button.getItems().add(0, inputMenu);
        menu_button.getItems().add(1, new SeparatorMenuItem());

        // TopInput selected at init time
        toggle_button.setText(getSimpleName() + " (TopInput)"); //NOI18N
        topMenuItem.setSelected(true);
        bottomMenuItem.setSelected(false);
    }
}
