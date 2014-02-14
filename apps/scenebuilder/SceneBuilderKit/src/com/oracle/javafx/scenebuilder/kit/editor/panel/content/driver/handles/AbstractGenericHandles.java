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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.DiscardGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.ResizeGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 *
 * 
 */
public abstract class AbstractGenericHandles<T> extends AbstractHandles<T> {
    
    /*
     *  
     *                      handleNN
     *    handleNW  o----------o----------o  handleNE
     *              |                     |
     *              |                     |
     *    handleWW  o                     o  handleEE
     *              |                     |
     *              |                     |
     *    handleSW  o----------o----------o  handleSE
     *                      handleSS
     * 
     */
    
    private final ImageView handleNW = new ImageView();
    private final ImageView handleNE = new ImageView();
    private final ImageView handleSE = new ImageView();
    private final ImageView handleSW = new ImageView();
    private final ImageView handleNN = new ImageView();
    private final ImageView handleEE = new ImageView();
    private final ImageView handleSS = new ImageView();
    private final ImageView handleWW = new ImageView();
    private final MoveTo moveTo0 = new MoveTo();
    private final LineTo lineTo1 = new LineTo();
    private final LineTo lineTo2 = new LineTo();
    private final LineTo lineTo3 = new LineTo();
    
    public AbstractGenericHandles(ContentPanelController contentPanelController,
            FXOMObject fxomObject, Class<T> sceneGraphObjectClass) {
        super(contentPanelController, fxomObject, sceneGraphObjectClass);
        
        final Path shadow = new Path();
        final List<PathElement> shadowElements = shadow.getElements();
        shadowElements.add(moveTo0);
        shadowElements.add(lineTo1);
        shadowElements.add(lineTo2);
        shadowElements.add(lineTo3);
        shadowElements.add(new ClosePath());
        shadow.getStyleClass().add("selection-rect");
        shadow.setMouseTransparent(true);
             
        setupHandleImages();
                
        handleNW.setPickOnBounds(true);
        handleNE.setPickOnBounds(true);
        handleSE.setPickOnBounds(true);
        handleSW.setPickOnBounds(true);
        
        handleNN.setPickOnBounds(true);
        handleEE.setPickOnBounds(true);
        handleSS.setPickOnBounds(true);
        handleWW.setPickOnBounds(true);
        
        attachHandles(handleNW);
        attachHandles(handleNE);
        attachHandles(handleSE);
        attachHandles(handleSW);
        
        attachHandles(handleNN);
        attachHandles(handleEE);
        attachHandles(handleSS);
        attachHandles(handleWW);
        
        final List<Node> rootNodeChildren = getRootNode().getChildren();
        rootNodeChildren.add(shadow);
        rootNodeChildren.add(handleNW);
        rootNodeChildren.add(handleNE);
        rootNodeChildren.add(handleSE);
        rootNodeChildren.add(handleSW);
        
        rootNodeChildren.add(handleNN);
        rootNodeChildren.add(handleEE);
        rootNodeChildren.add(handleSS);
        rootNodeChildren.add(handleWW);
    }
    
    public Node getHandleNode(CardinalPoint cp) {
        final Node result;
        
        switch(cp) {
            case N:
                result = handleNN;
                break;
            case S:
                result = handleSS;
                break;
            case E:
                result = handleEE;
                break;
            case W:
                result = handleWW;
                break;
            case NW:
                result = handleNW;
                break;
            case NE:
                result = handleNE;
                break;
            case SW:
                result = handleSW;
                break;
            case SE:
                result = handleSE;
                break;
            default:
                assert false;
                result = null;
                break;
        }
        
        return result;
    }
    
    /*
     * AbstractHandles
     */

    @Override
    protected void layoutDecoration() {
        final Bounds b = getSceneGraphObjectBounds();
        
        final double minX = b.getMinX();
        final double minY = b.getMinY();
        final double maxX = b.getMaxX();
        final double maxY = b.getMaxY();
        final double midX = (minX + maxX) / 2.0;
        final double midY = (minY + maxY) / 2.0;
        
        final boolean zeroWidth = MathUtils.equals(minX, maxX);
        final boolean zeroHeight = MathUtils.equals(minY, maxY);
        
        final boolean snapToPixel = true;
        final Point2D pNW, pNE, pSE, pSW;
        final Point2D pNN, pEE, pSS, pWW;
        
        if (zeroWidth && zeroHeight) {
            pNW = pNE = pSE = pSW = 
            pNN = pEE = pSS = pWW = 
                    sceneGraphObjectToDecoration(minX, minY, snapToPixel);
        } else if (zeroWidth) {
            pNW = pNN = pNE =
                    sceneGraphObjectToDecoration(minX, minY, snapToPixel);
            pSW = pSS = pSE =
                    sceneGraphObjectToDecoration(minX, maxY, snapToPixel);
            pEE = pWW =
                    sceneGraphObjectToDecoration(minX, midY, snapToPixel);
        } else if (b.getHeight() == 0) {
            pNW = pWW = pSW =
                    sceneGraphObjectToDecoration(minX, minY, snapToPixel);
            pNE = pEE = pSE =
                    sceneGraphObjectToDecoration(maxX, minY, snapToPixel);
            pNN = pSS =
                    sceneGraphObjectToDecoration(midX, minY, snapToPixel);
        } else {
            pNW = sceneGraphObjectToDecoration(minX, minY, snapToPixel);
            pNE = sceneGraphObjectToDecoration(maxX, minY, snapToPixel);
            pSE = sceneGraphObjectToDecoration(maxX, maxY, snapToPixel);
            pSW = sceneGraphObjectToDecoration(minX, maxY, snapToPixel);

            pNN = sceneGraphObjectToDecoration(midX, minY, snapToPixel);
            pEE = sceneGraphObjectToDecoration(maxX, midY, snapToPixel);
            pSS = sceneGraphObjectToDecoration(midX, maxY, snapToPixel);
            pWW = sceneGraphObjectToDecoration(minX, midY, snapToPixel);
        }
        
        moveTo0.setX(pNW.getX());
        moveTo0.setY(pNW.getY());
        lineTo1.setX(pNE.getX());
        lineTo1.setY(pNE.getY());
        lineTo2.setX(pSE.getX());
        lineTo2.setY(pSE.getY());
        lineTo3.setX(pSW.getX());
        lineTo3.setY(pSW.getY());
        
        handleNW.setLayoutX(pNW.getX());
        handleNW.setLayoutY(pNW.getY());
        handleNE.setLayoutX(pNE.getX());
        handleNE.setLayoutY(pNE.getY());
        handleSE.setLayoutX(pSE.getX());
        handleSE.setLayoutY(pSE.getY());
        handleSW.setLayoutX(pSW.getX());
        handleSW.setLayoutY(pSW.getY());
        
        handleNN.setLayoutX(pNN.getX());
        handleNN.setLayoutY(pNN.getY());
        handleEE.setLayoutX(pEE.getX());
        handleEE.setLayoutY(pEE.getY());
        handleSS.setLayoutX(pSS.getX());
        handleSS.setLayoutY(pSS.getY());
        handleWW.setLayoutX(pWW.getX());
        handleWW.setLayoutY(pWW.getY());
        
        final Bounds handlesBounds = computeBounds(pNW, pNE, pSE, pSW);
        final int rotation = computeNWHandleRotation(pNW, handlesBounds);
        
        setupCornerHandle(handleNW, rotation +   0);
        setupCornerHandle(handleNE, rotation +  90);
        setupCornerHandle(handleSE, rotation + 180);
        setupCornerHandle(handleSW, rotation + 270);
        
        setupSideHandle(handleNN, rotation +   0);
        setupSideHandle(handleEE, rotation +  90);
        setupSideHandle(handleSS, rotation + 180);
        setupSideHandle(handleWW, rotation + 270);
        
        showHideSideHandle(handleNN, pNW, pNE);
        showHideSideHandle(handleEE, pNE, pSE);
        showHideSideHandle(handleSS, pSW, pSE);
        showHideSideHandle(handleWW, pNW, pSW);
    }


    @Override
    public AbstractGesture findGesture(Node node) {
        final AbstractGesture result;
        
        if (isResizable() == false) {
            result = new DiscardGesture(getContentPanelController());
        } else {
            assert getFxomObject() instanceof FXOMInstance;
            
            final FXOMInstance fxomInstance = (FXOMInstance) getFxomObject();
            
            if (node == handleNW) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.NW);
            } else if (node == handleNE) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.NE);
            } else if (node == handleSE) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.SE);
            } else if (node == handleSW) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.SW);
            }  else if (node == handleNN) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.N);
            } else if (node == handleEE) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.E);
            } else if (node == handleSS) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.S);
            } else if (node == handleWW) {
                result = new ResizeGesture(getContentPanelController(), 
                        fxomInstance, CardinalPoint.W);
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    @Override
    public void enabledDidChange() {
        setupHandleImages();
    }

    /*
     * Private
     */
    
    private Bounds computeBounds(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        final double minX, minY, maxX, maxY;
        
        minX = Math.min(Math.min(p0.getX(), p1.getX()), Math.min(p2.getX(), p3.getX()));
        minY = Math.min(Math.min(p0.getY(), p1.getY()), Math.min(p2.getY(), p3.getY()));
        maxX = Math.max(Math.max(p0.getX(), p1.getX()), Math.max(p2.getX(), p3.getX()));
        maxY = Math.max(Math.max(p0.getY(), p1.getY()), Math.max(p2.getY(), p3.getY()));
        
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
    
    private void setupCornerHandle(ImageView handle, int rotation) {
        
        rotation = ((rotation % 360) + 360) % 360; // Clamp between 0 and 360
        
        final double dx, dy;
        final double handleWidth = handle.getLayoutBounds().getWidth();
        if (rotation == 0) {
            dx = +0.0;
            dy = +0.0;
        } else if (rotation == 90) {
            dx = -handleWidth;
            dy = +0.0;
        } else if (rotation == 180) {
            dx = -handleWidth;
            dy = -handleWidth;
        } else if (rotation == 270) {
            dx = +0.0;
            dy = -handleWidth;
        } else {
            assert false : "rotation=" + rotation;
            dx = +0.0;
            dy = +0.0;
        }
        
        handle.setRotate(rotation);
        handle.setTranslateX(dx);
        handle.setTranslateY(dy);
    }
    
    private void setupSideHandle(ImageView handle, int rotation) {
        
        rotation = ((rotation % 360) + 360) % 360; // Clamp between 0 and 360
        
        final double dx, dy;
        final double w = handle.getLayoutBounds().getWidth()  / 2.0;
        final double h = handle.getLayoutBounds().getHeight() / 2.0;
        final double k0 = 1.0; // Hugly trick to force pixel alignment :(
        
        if (rotation == 0) {
            dx = -w;
            dy = +0.0;
        } else if (rotation == 90) {
            dx = -w -h;
            dy = -h - k0;
        } else if (rotation == 180) {
            dx = -w + k0;
            dy = -h * 2;
        } else if (rotation == 270) {
            dx = -w + h;
            dy = -h;
        } else {
            assert false : "rotation=" + rotation;
            dx = +0.0;
            dy = +0.0;
        }
        
        handle.setRotate(rotation);
        handle.setTranslateX(dx);
        handle.setTranslateY(dy);
    }
    
    private int computeNWHandleRotation(Point2D handlePos, Bounds handlesBounds) {
        final int result;
        
        assert handlePos != null;
        assert handlesBounds != null;
        assert handlesBounds.contains(handlePos);
       
        
        if ((handlesBounds.getWidth() == 0) || (handlesBounds.getHeight() == 0)) {
            // scene graph object is zero sized
            result = +180;
        } else {
            /*
             *          x0        xm        x1
             *      y0  *---------*---------*
             *          |    tl   |   tr    |
             *          |         |         |
             *          |  +180°  |  +270°  |
             *          |         |         |
             *      ym  *---------*---------*
             *          |    bl   |   br    |
             *          |         |         |
             *          |   +90°  |    0°   |
             *          |         |         |
             *      y1  *---------*---------*
             */

            final double x0 = handlesBounds.getMinX();
            final double x1 = handlesBounds.getMaxX();
            final double xm = (x0 + x1) / 2.0;
            final double y0 = handlesBounds.getMinY();
            final double y1 = handlesBounds.getMaxY();
            final double ym = (y0 + y1) / 2.0;

            final double x = handlePos.getX();
            final double y = handlePos.getY();
            
            if (x <= xm) {
                if (y <= ym) {
                    // (x, y) is in the top left quadrant
                    result = +180;
                } else {
                    // (x, y) is in the bottom left quadrant
                    result = +90;
                }
            } else {
                if (y <= ym) {
                    // (x, y) is in the top right quadrant
                    result = +270;
                } else {
                    // (x, y) is in the bottom right quadrant
                    result = +0;
                }
            }
        }
        
        return result;
    }
    
    
    private void showHideSideHandle(ImageView handle, Point2D p0, Point2D p1) {
        
        final double dx = p1.getX() - p0.getX();
        final double dy = p1.getY() - p0.getY();
        final double d01 = Math.sqrt(dx * dx + dy * dy);
        
        final double sideHandleWidth = getSideHandleImage().getWidth();
        final double sideHandleHeight = getSideHandleImage().getHeight();
        final double sideHandleSize = Math.max(sideHandleWidth, sideHandleHeight);
        
        final boolean handleVisible = sideHandleSize < d01;
        handle.setVisible(handleVisible);
        handle.setMouseTransparent(! handleVisible);
    }

    
    private void setupHandleImages() {
        final Image handleImage, sideHandleImage;
        if (isEnabled() && isResizable()) {
            
            handleNW.setCursor(Cursor.NW_RESIZE);
            handleNE.setCursor(Cursor.NE_RESIZE);
            handleSE.setCursor(Cursor.SE_RESIZE);
            handleSW.setCursor(Cursor.SW_RESIZE);

            handleNN.setCursor(Cursor.N_RESIZE);
            handleEE.setCursor(Cursor.E_RESIZE);
            handleSS.setCursor(Cursor.S_RESIZE);
            handleWW.setCursor(Cursor.W_RESIZE);
            
            handleImage = getCornerHandleImage();
            sideHandleImage = getSideHandleImage();
            
        } else {
            
            handleNW.setCursor(Cursor.DEFAULT);
            handleNE.setCursor(Cursor.DEFAULT);
            handleSE.setCursor(Cursor.DEFAULT);
            handleSW.setCursor(Cursor.DEFAULT);

            handleNN.setCursor(Cursor.DEFAULT);
            handleEE.setCursor(Cursor.DEFAULT);
            handleSS.setCursor(Cursor.DEFAULT);
            handleWW.setCursor(Cursor.DEFAULT);
            
            handleImage = getCornerHandleDimImage();
            sideHandleImage = getSideHandleDimImage();
        }
        
        handleNW.setImage(handleImage);
        handleNE.setImage(handleImage);
        handleSE.setImage(handleImage);
        handleSW.setImage(handleImage);
        
        handleNN.setImage(sideHandleImage);
        handleEE.setImage(sideHandleImage);
        handleSS.setImage(sideHandleImage);
        handleWW.setImage(sideHandleImage);
        
    }
    
    
    private boolean isResizable() {
        final AbstractDriver driver 
                = getContentPanelController().lookupDriver(getFxomObject());
        return driver.makeResizer(getFxomObject()) != null;
    }
    
    /* 
     * Wraper to avoid the 'leaking this in constructor' warning emitted by NB.
     */
    private void attachHandles(Node node) {
        attachHandles(node, this);
    }
}
