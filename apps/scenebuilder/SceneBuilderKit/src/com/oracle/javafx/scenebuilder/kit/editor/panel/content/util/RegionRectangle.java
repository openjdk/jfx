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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.util;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.layout.Region;

/**
 * A RegionRectangle behaves like a Rectangle but enables all the
 * CSS Styling available on a Region.
 * 
 * It is the following construct:
 *
 *    Group
 *         Region
 *
 * Layout bounds of the group must be equal to layout bounds
 * of the scene graph node. We ensure this by setting layoutX/Y
 * on the region (1).
 */
public class RegionRectangle extends Group {
    
    private final Region region = new Region();
    
    
    public RegionRectangle() {
        this.getChildren().add(region);
    }
    
    public Region getRegion() {
        return region;
    }
    
    public void setLayoutBounds(Bounds layoutBounds) {
        
        // Setup layoutX/layoutY on the image view and the region (1)
        region.setLayoutX(layoutBounds.getMinX());
        region.setLayoutY(layoutBounds.getMinY());
        region.setPrefWidth(layoutBounds.getWidth());
        region.setPrefHeight(layoutBounds.getHeight());
    }
}
