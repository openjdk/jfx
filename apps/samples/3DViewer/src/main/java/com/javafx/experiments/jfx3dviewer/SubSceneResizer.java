/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.jfx3dviewer;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;

/**
 * Resizable container for a SubScene
 */
public class SubSceneResizer extends Pane {
    private SubScene subScene;
    private final Node controlsPanel;

    public SubSceneResizer(SubScene subScene, Node controlsPanel) {
        this.subScene = subScene;
        this.controlsPanel = controlsPanel;
        setPrefSize(subScene.getWidth(),subScene.getHeight());
        setMinSize(50,50);
        setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        getChildren().addAll(subScene, controlsPanel);
    }

    public SubSceneResizer(ObjectProperty<SubScene> subScene, Node controlsPanel) {
        this.subScene = subScene.get();
        this.controlsPanel = controlsPanel;
        if (this.subScene != null) {
            setPrefSize(this.subScene.getWidth(),this.subScene.getHeight());
            getChildren().add(this.subScene);
        }
        subScene.addListener((o,old,newSubScene) -> {
            this.subScene = newSubScene;
            if (this.subScene != null) {
                setPrefSize(this.subScene.getWidth(),this.subScene.getHeight());
                if (getChildren().size() == 1) {
                    getChildren().add(0,this.subScene);
                } else {
                    getChildren().set(0,this.subScene);
                }
            }
        });
        setMinSize(50,50);
        setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        getChildren().add(controlsPanel);
    }

    @Override protected void layoutChildren() {
        final double width = getWidth();
        final double height = getHeight();
        if (subScene!=null) {
            subScene.setWidth(width);
            subScene.setHeight(height);
        }
        final int controlsWidth = (int)snapSize(controlsPanel.prefWidth(-1));
        final int controlsHeight = (int)snapSize(controlsPanel.prefHeight(-1));
        controlsPanel.resizeRelocate(width-controlsWidth,0,controlsWidth,controlsHeight);
    }
}
