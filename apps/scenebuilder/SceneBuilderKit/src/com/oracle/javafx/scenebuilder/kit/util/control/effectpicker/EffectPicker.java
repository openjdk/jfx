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
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;
import javafx.scene.layout.Pane;

/**
 * Effects editor control.
 */
public class EffectPicker extends Pane {

    private final EffectPickerController controller;

    private final Class<?>[] effectClasses = {
        Blend.class,
        Bloom.class,
        BoxBlur.class,
        ColorAdjust.class,
        ColorInput.class,
        DisplacementMap.class,
        DropShadow.class,
        GaussianBlur.class,
        Glow.class,
        ImageInput.class,
        InnerShadow.class,
        Lighting.class,
        MotionBlur.class,
        PerspectiveTransform.class,
        Reflection.class,
        SepiaTone.class,
        Shadow.class};

    public EffectPicker() {
        final FXMLLoader loader = new FXMLLoader();
        loader.setLocation(EffectPicker.class.getResource("EffectPicker.fxml")); //NOI18N

        try {
            // Loading
            final Object rootObject = loader.load();
            assert rootObject instanceof Node;
            final Node rootNode = (Node) rootObject;
            getChildren().add(rootNode);

            // Retrieving the controller
            final Object ctl = loader.getController();
            assert ctl instanceof EffectPickerController;
            this.controller = (EffectPickerController) ctl;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public final ObjectProperty<Effect> rootEffectProperty() {
        return controller.rootEffectProperty();
    }

    public final void setRootEffectProperty(Effect value) {
        // Update model
        controller.setRootEffectProperty(value);
        // Update UI
        controller.updateUI();
    }

    public final Effect getRootEffectProperty() {
        return controller.getRootEffectProperty();
    }

    public void reset() {
        controller.reset();
    }

    public String getEffectPath() {
        return controller.getEffectPath();
    }

    public List<MenuItem> getMenuItems() {
        final List<MenuItem> menuItems = new ArrayList<>();
        for (final Class<?> clazz : effectClasses) {
            final MenuItem mi = new MenuItem(clazz.getSimpleName());
            mi.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    final Effect effect = Utils.newInstance(clazz);
                    setRootEffectProperty(effect);
                }
            });
            menuItems.add(mi);
        }
        return menuItems;
    }
}
