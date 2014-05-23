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

import com.oracle.javafx.scenebuilder.kit.util.control.paintpicker.PaintPicker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Pane;

/**
 * Effects editor control.
 */
public class EffectPicker extends Pane {

    private final EffectPickerController controller;

    private static List<Class<? extends Effect>> effectClasses;

    public synchronized static Collection<Class<? extends Effect>> getEffectClasses() {
        if (effectClasses == null) {
            effectClasses = new ArrayList<>();
            effectClasses.add(javafx.scene.effect.Blend.class);
            effectClasses.add(javafx.scene.effect.Bloom.class);
            effectClasses.add(javafx.scene.effect.BoxBlur.class);
            effectClasses.add(javafx.scene.effect.ColorAdjust.class);
            effectClasses.add(javafx.scene.effect.ColorInput.class);
            effectClasses.add(javafx.scene.effect.DisplacementMap.class);
            effectClasses.add(javafx.scene.effect.DropShadow.class);
            effectClasses.add(javafx.scene.effect.GaussianBlur.class);
            effectClasses.add(javafx.scene.effect.Glow.class);
            effectClasses.add(javafx.scene.effect.ImageInput.class);
            effectClasses.add(javafx.scene.effect.InnerShadow.class);
            effectClasses.add(javafx.scene.effect.Lighting.class);
            effectClasses.add(javafx.scene.effect.MotionBlur.class);
            effectClasses.add(javafx.scene.effect.PerspectiveTransform.class);
            effectClasses.add(javafx.scene.effect.Reflection.class);
            effectClasses.add(javafx.scene.effect.SepiaTone.class);
            effectClasses.add(javafx.scene.effect.Shadow.class);
            effectClasses = Collections.unmodifiableList(effectClasses);
        }

        return effectClasses;
    }

    public EffectPicker(EffectPicker.Delegate epd, PaintPicker.Delegate ppd) {
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
            this.controller.setEffectPickerDelegate(epd);
            this.controller.setPaintPickerDelegate(ppd);
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

    public ReadOnlyIntegerProperty revisionProperty() {
        return controller.revisionProperty();
    }
    
    public final ReadOnlyBooleanProperty liveUpdateProperty() {
        return controller.liveUpdateProperty();
    }

    public boolean isLiveUpdate() {
        return controller.isLiveUpdate();
    }
    
    public String getEffectPath() {
        return controller.getEffectPath();
    }

    public List<MenuItem> getMenuItems() {
        final List<MenuItem> menuItems = new ArrayList<>();
        for (final Class<? extends Effect> clazz : getEffectClasses()) {
            final MenuItem mi = new MenuItem(clazz.getSimpleName());
            mi.setOnAction(t -> {
                final Effect effect = Utils.newInstance(clazz);
                setRootEffectProperty(effect);
                controller.incrementRevision();
            });
            menuItems.add(mi);
        }
        return menuItems;
    }
    
    public static interface Delegate {
        public void handleError(String warningKey, Object... arguments);
    }
}
