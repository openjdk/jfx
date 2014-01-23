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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.klass.ComponentClassMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ImagePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignImage;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.stage.Window;

/**
 *
 */
public class ExternalDragSource extends AbstractDragSource {

    private final Dragboard dragboard;
    private final FXOMDocument targetDocument;
    private List<FXOMObject> draggedObjects; // Initialized lazily
    private List<File> inputFiles; // Initialized lazily
    private boolean nodeOnly; // Iniitalized lazily
    private int errorCount;
    private Exception lastException;

    public ExternalDragSource(Dragboard clipboardContent, FXOMDocument targetDocument, Window ownerWindow) {
        super(ownerWindow);
        
        assert clipboardContent != null;
        assert targetDocument != null;
        
        this.dragboard = clipboardContent;
        this.targetDocument = targetDocument;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public Exception getLastException() {
        return lastException;
    }

    
    /*
     * AbstractDragSource
     */
    
    @Override
    public List<FXOMObject> getDraggedObjects() {
        if (draggedObjects == null) {
            draggedObjects = new ArrayList<>();
            inputFiles = new ArrayList<>();

            for (File file : dragboard.getFiles()) {
                try {
                    final FXOMObject newObject
                            = FXOMNodes.newObject(targetDocument, file);
                    // newObject is null when file is empty
                    if (newObject != null) {
                        draggedObjects.add(newObject);
                        inputFiles.add(file);
                    }
                } catch (IOException x) {
                    errorCount++;
                    lastException = x;
                }
            }

            // We put all the Node dragged objects in a Scene and layout them
            // so that ContainerXYDropTarget can measure them.
            // We stack and shift them a little so that they are all visible.
            final Group group = new Group();
            double dxy = 0.0;
            for (FXOMObject o : draggedObjects) {
                if (o.getSceneGraphObject() instanceof Node) {
                    final Node sceneGraphNode = (Node) o.getSceneGraphObject();
                    sceneGraphNode.setLayoutX(dxy);
                    sceneGraphNode.setLayoutY(dxy);
                    dxy += 20.0;
                    
                    group.getChildren().add(sceneGraphNode);
                }
            }
            final Scene scene = new Scene(group); // Unused but required
            scene.getClass(); // used to dummy thing to silence FindBugs
            group.applyCss();
            group.layout();
        }
        
        return draggedObjects;
    }

    @Override
    public FXOMObject getHitObject() {
        final FXOMObject result;
        
        if (getDraggedObjects().isEmpty()) {
            result = null;
        } else {
            result = getDraggedObjects().get(0);
        }
        
        return result;
    }
    
    @Override
    public double getHitX() {
        final double result;
        
        final FXOMObject hitObject = getHitObject();
        if (hitObject == null) {
            result = Double.NaN;
        } else if (hitObject.isNode()) {
            final Node hitNode = (Node) hitObject.getSceneGraphObject();
            final Bounds b = hitNode.getLayoutBounds();
            result = (b.getMinX() + b.getMaxX()) / 2.0;
        } else {
            result = 0.0;
        }
        
        return result;
    }

    @Override
    public double getHitY() {
        final double result;
        
        final FXOMObject hitObject = getHitObject();
        if (hitObject == null) {
            result = Double.NaN;
        } else if (hitObject.isNode()) {
            final Node hitNode = (Node) hitObject.getSceneGraphObject();
            final Bounds b = hitNode.getLayoutBounds();
            result = (b.getMinY() + b.getMaxY()) / 2.0;
        } else {
            result = 0.0;
        }
        
        return result;
    }

    @Override
    public ClipboardContent makeClipboardContent() {
        throw new UnsupportedOperationException("should not be called"); //NOI18N
    }

    @Override
    public Image makeDragView() {
        throw new UnsupportedOperationException("should not be called"); //NOI18N
    }

    @Override
    public Node makeShadow() {
        final Group result = new Group();

        result.getStylesheets().add(EditorController.getStylesheet().toString());

        for (FXOMObject draggedObject : getDraggedObjects()) {
            if (draggedObject.getSceneGraphObject() instanceof Node) {
                final Node sceneGraphNode = (Node) draggedObject.getSceneGraphObject();
                final DragSourceShadow shadowNode = new DragSourceShadow();
                shadowNode.setupForNode(sceneGraphNode);
                shadowNode.getTransforms().add(sceneGraphNode.getLocalToSceneTransform());
                result.getChildren().add(shadowNode);
            }
        }
        
        // Translate the group so that it is centered above (layoutX, layoutY)
        final Bounds b = result.getBoundsInParent();
        final double centerX = (b.getMinX() + b.getMaxX()) / 2.0;
        final double centerY = (b.getMinY() + b.getMaxY()) / 2.0;
        result.setTranslateX(-centerX);
        result.setTranslateY(-centerY);
        
        return result;
    }

    @Override
    public String makeDropJobDescription() {
        final String result;
        
        if (inputFiles.size() == 1) {
            final Path inputPath = Paths.get(inputFiles.get(0).toURI());
            result = I18N.getString("drop.job.insert.from.single.file",
                    inputPath.getFileName());
        } else {
            result = I18N.getString("drop.job.insert.from.multiple.files",
                    inputFiles.size());
        }
        
        return result;
    }

    @Override
    public boolean isNodeOnly() {
        if (draggedObjects == null) {
            int nonNodeCount = 0;
            for (FXOMObject draggedObject : getDraggedObjects()) {
                if (draggedObject.isNode() == false) {
                    nonNodeCount++;
                }
            }
            nodeOnly = nonNodeCount == 0;
        }
        
        return nodeOnly;
    }
    
    /*
     * Object
     */
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": dragboard=(" + dragboard + ")"; //NOI18N
    }
    
    
    /*
     * Private
     */
    
    /*
     * Utilities that should probably go somewhere else.
     */
    
    static FXOMDocument makeFxomDocumentFromImageURL(Image image, 
            double fitSize) throws IOException {

        assert image != null;
        assert fitSize > 0.0;
        
        final double imageWidth = image.getWidth();
        final double imageHeight = image.getHeight();
        
        final double fitWidth, fitHeight;
        final double imageSize = Math.max(imageWidth, imageHeight);
        if (imageSize < fitSize) {
            fitWidth = 0;
            fitHeight = 0;
        } else {
            final double widthScale  = fitSize / imageSize;
            final double heightScale = fitSize / imageHeight;
            final double scale = Math.min(widthScale, heightScale);
            fitWidth = Math.floor(imageWidth * scale);
            fitHeight = Math.floor(imageHeight * scale);
        }
        
        return makeFxomDocumentFromImageURL(image, fitWidth, fitHeight);
    }
    
    static final PropertyName imageName = new PropertyName("image"); //NOI18N
    static final PropertyName fitWidthName = new PropertyName("fitWidth"); //NOI18N
    static final PropertyName fitHeightName = new PropertyName("fitHeight"); //NOI18N
    
    static FXOMDocument makeFxomDocumentFromImageURL(Image image, double fitWidth, double fitHeight) {
        final FXOMDocument result = new FXOMDocument();
        final FXOMInstance imageView = new FXOMInstance(result, ImageView.class);
        
        final ComponentClassMetadata imageViewMeta 
                = Metadata.getMetadata().queryComponentMetadata(ImageView.class);
        final PropertyMetadata imagePropMeta
                = imageViewMeta.lookupProperty(imageName);
        final PropertyMetadata fitWidthPropMeta
                = imageViewMeta.lookupProperty(fitWidthName);
        final PropertyMetadata fitHeightPropMeta
                = imageViewMeta.lookupProperty(fitHeightName);
        
        assert imagePropMeta instanceof ImagePropertyMetadata;
        assert fitWidthPropMeta instanceof DoublePropertyMetadata;
        assert fitHeightPropMeta instanceof DoublePropertyMetadata;
        
        final ImagePropertyMetadata imageMeta
                = (ImagePropertyMetadata) imagePropMeta;
        final DoublePropertyMetadata fitWidthMeta
                = (DoublePropertyMetadata) fitWidthPropMeta;
        final DoublePropertyMetadata fitHeightMeta
                = (DoublePropertyMetadata) fitHeightPropMeta;

        imageMeta.setValue(imageView, new DesignImage(image));
        fitWidthMeta.setValue(imageView, fitWidth);
        fitHeightMeta.setValue(imageView, fitHeight);
        
        result.setFxomRoot(imageView);
        
        return result;
    }
}
