/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import static com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource.INTERNAL_DATA_FORMAT;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ClipboardEncoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Window;

/**
 *
 */
public class DocumentDragSource extends AbstractDragSource {
    
    private final List<FXOMObject> draggedObjects = new ArrayList<>();
    private final FXOMObject hitObject;
    private final double hitX;
    private final double hitY;

    public DocumentDragSource(
            List<FXOMObject> draggedObjects, 
            FXOMObject hitObject,
            double hitX,
            double hitY,
            Window ownerWindow) {
        super(ownerWindow);
        
        assert draggedObjects != null;
        assert hitObject != null;
        assert draggedObjects.contains(hitObject);
        
        this.draggedObjects.addAll(draggedObjects);
        this.hitObject = hitObject;
        this.hitX = hitX;
        this.hitY = hitY;
    }

    public DocumentDragSource(
            List<FXOMObject> draggedObjects, 
            FXOMObject hitObject,
            Window ownerWindow) {
        super(ownerWindow);
        
        assert draggedObjects != null;
        assert hitObject != null;
        assert draggedObjects.contains(hitObject);
        
        this.draggedObjects.addAll(draggedObjects);
        this.hitObject = hitObject;
        
        final Point2D hitPoint = computeDefaultHit(hitObject);
        this.hitX = hitPoint.getX();
        this.hitY = hitPoint.getY();
    }
    
    private static Point2D computeDefaultHit(FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() != null;
        
        final double hitX, hitY;
        if (fxomObject.getSceneGraphObject() instanceof Node) {
            final Node sceneGraphNode = (Node) fxomObject.getSceneGraphObject();
            final Bounds lb = sceneGraphNode.getLayoutBounds();
            hitX = (lb.getMinX() + lb.getMaxX()) / 2.0;
            hitY = (lb.getMinY() + lb.getMaxY()) / 2.0;
        } else {
            hitX = 0.0;
            hitY = 0.0;
        }
        
        return new Point2D(hitX,hitY);
    }

    public FXOMObject getHitObject() {
        return hitObject;
    }

    public double getHitX() {
        return hitX;
    }

    public double getHitY() {
        return hitY;
    }

    /*
     * AbstractDragSource
     */
    
    @Override
    public List<FXOMObject> getDraggedObjects() {
        return draggedObjects;
    }

    @Override
    public ClipboardContent makeClipboardContent() {
        
        // Encode the dragged objects in FXML
        final ClipboardEncoder encoder = new ClipboardEncoder(draggedObjects);
        assert encoder.isEncodable();
        final ClipboardContent result = encoder.makeEncoding();
        
        // Add an entry that indicates that this clipboard content has
        // been created by Scene Builder itself.
        result.put(INTERNAL_DATA_FORMAT, "" /* Unused */); //NOI18N
        
        return result;
    }

    @Override
    public Image makeDragView() {
        final Image image;
        final DesignHierarchyMask mask = new DesignHierarchyMask(hitObject);
        final URL resource = mask.getClassNameIconURL();
        // Resource may be null for unresolved classes
        if (resource == null) {
            image = ImageUtils.getNodeIcon("MissingIcon.png"); //NOI18N
        } else {
            image = new Image(resource.toExternalForm());
        }

        final Label visualNode = new Label();
        visualNode.setGraphic(new ImageView(image));
        visualNode.setText(mask.getClassNameInfo());
        visualNode.getStylesheets().add(EditorController.getStylesheet().toString());
        visualNode.getStyleClass().add("drag-preview"); //NOI18N

        return ImageUtils.getImageFromNode(visualNode);
    }

    @Override
    public Node makeShadow() {
        final Group result = new Group();
        
        result.getStylesheets().add(EditorController.getStylesheet().toString());

        for (FXOMObject draggedObject : draggedObjects) {
            if (draggedObject.getSceneGraphObject() instanceof Node) {
                final Node sceneGraphNode = (Node) draggedObject.getSceneGraphObject();
                final DragSourceShadow shadowNode = new DragSourceShadow();
                shadowNode.setupForNode(sceneGraphNode);
//                assert shadowNode.getLayoutBounds().equals(sceneGraphNode.getLayoutBounds());
                shadowNode.getTransforms().add(sceneGraphNode.getLocalToParentTransform());
                result.getChildren().add(shadowNode);
            }
        }
        
        // Translate the group so that it renders (hitX, hitY) above (layoutX, layoutY).
        final Point2D hitPoint;
        if (hitObject.getSceneGraphObject() instanceof Node) {
            final Node hitNode = (Node) hitObject.getSceneGraphObject();
            hitPoint = hitNode.localToParent(hitX, hitY);
        } else {
            hitPoint = Point2D.ZERO;
        }
        result.setTranslateX(-hitPoint.getX());
        result.setTranslateY(-hitPoint.getY());
        
        return result;
    }

    @Override
    public String makeDropJobDescription() {
        final String result;
        
        if (draggedObjects.size() == 1) {
            final FXOMObject draggedObject = draggedObjects.get(0);
            final Object sceneGraphObject = draggedObject.getSceneGraphObject();
            if (sceneGraphObject == null) {
                result = I18N.getString("drop.job.move.single.unresolved");
            } else {
                result = I18N.getString("drop.job.move.single.resolved",
                        sceneGraphObject.getClass().getSimpleName());
            }
        } else {
            final Set<Class<?>> classes = new HashSet<>();
            int unresolvedCount = 0;
            for (FXOMObject o : draggedObjects) {
                if (o.getSceneGraphObject() != null) {
                    classes.add(o.getSceneGraphObject().getClass());
                } else {
                    unresolvedCount++;
                }
            }
            final boolean homogeneous = (classes.size() == 1) && (unresolvedCount == 0);
            
            if (homogeneous) {
                final Class<?> singleClass = classes.iterator().next();
                result = I18N.getString("drop.job.move.multiple.homogeneous",
                        draggedObjects.size(),
                        singleClass.getSimpleName());
            } else {
                result = I18N.getString("drop.job.move.multiple.heterogeneous",
                        draggedObjects.size());
            }
        }
        
        return result;
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": hitObject=(" + hitObject + ")"; //NOI18N
    }
    
}
