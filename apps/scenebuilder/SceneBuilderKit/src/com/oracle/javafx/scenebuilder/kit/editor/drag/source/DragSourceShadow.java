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
package com.oracle.javafx.scenebuilder.kit.editor.drag.source;

import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

/**
 * A shadow is the following construct:
 *
 *    Group
 *         ImageView        snapshot of a 'scene graph node'
 *         Region           glass area with css styling
 *
 * Layout bounds of the group must be equal to layout bounds
 * of the scene graph node. We ensure this by setting layoutX/Y
 * on the image view and the region (1).
 */
class DragSourceShadow extends Group {
    
    private final ImageView imageView = new ImageView();
    private final Region glass = new Region();
    private static final String NID_DRAG_SHADOW = "dragShadow"; //NOI18N

    
    public DragSourceShadow() {
        this.setId(NID_DRAG_SHADOW);
        this.getChildren().add(imageView);
        this.getChildren().add(glass);
        
        this.getStyleClass().add("drag-shadow"); //NOI18N
        this.glass.getStyleClass().add("drag-shadow-glass"); //NOI18N
    }
    
    public void setupForNode(Node node) {
        assert node != null;
        assert node.getScene() != null;
        
        // Snapshot node
        // Note : we setup snapshot view port with layout bounds.
        final SnapshotParameters sp = new SnapshotParameters();
        final Transform l2p = node.getLocalToParentTransform();
        try {
            sp.setTransform(l2p.createInverse());
        } catch(NonInvertibleTransformException x) {
            throw new RuntimeException(x);
        }
        final Bounds vp = node.getLayoutBounds();
        if ((vp.getWidth() >= 0) && (vp.getHeight() >= 0)) {
            sp.setViewport(new Rectangle2D(vp.getMinX(), vp.getMinY(), 
                    vp.getWidth(), vp.getHeight()));
        }
        imageView.setImage(node.snapshot(sp, null));
        
        // Setup layoutX/layoutY on the image view and the region (1)
        final Bounds inputBounds = vp;
        imageView.setLayoutX(inputBounds.getMinX());
        imageView.setLayoutY(inputBounds.getMinY());
        glass.setLayoutX(inputBounds.getMinX());
        glass.setLayoutY(inputBounds.getMinY());
        glass.setPrefWidth(inputBounds.getWidth());
        glass.setPrefHeight(inputBounds.getHeight());

        final Bounds outputBounds = this.getLayoutBounds();
        assert MathUtils.equals(inputBounds.getMinX(), outputBounds.getMinX())
                : "inputBounds=" + inputBounds + ", outputBounds=" + outputBounds; //NOI18N
        assert MathUtils.equals(inputBounds.getMinY(), outputBounds.getMinY())
                : "inputBounds=" + inputBounds + ", outputBounds=" + outputBounds; //NOI18N
        assert MathUtils.equals(inputBounds.getWidth(), outputBounds.getWidth(), 5.0)
                : "inputBounds=" + inputBounds + ", outputBounds=" + outputBounds; //NOI18N
        assert MathUtils.equals(inputBounds.getHeight(), outputBounds.getHeight(), 5.0)
                : "inputBounds=" + inputBounds + ", outputBounds=" + outputBounds; //NOI18N
    }
}
