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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.mode;

import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

/**
 *
 * 
 */


public class PickModeController extends AbstractModeController {

    private HitNodeChrome hitNodeChrome;
    
    public PickModeController(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }
    
    
    /*
     * AbstractModeController
     */
    
    @Override
    public void willResignActive(AbstractModeController nextModeController) {
        contentPanelController.getGlassLayer().setCursor(Cursor.DEFAULT);
        stopListeningToInputEvents();
        removeHitNodeChrome();
    }

    @Override
    public void didBecomeActive(AbstractModeController previousModeController) {
        assert contentPanelController.getGlassLayer() != null;
        
        updateHitNodeChrome();
        startListeningToInputEvents();
        contentPanelController.getGlassLayer().setCursor(ImageUtils.getCSSCursor());
    }
    
    @Override
    public void editorSelectionDidChange() {
        updateHitNodeChrome();
    }

    @Override
    public void fxomDocumentDidChange(FXOMDocument oldDocument) {
        // Same logic as when the scene graph is changed
        fxomDocumentDidRefreshSceneGraph();
    }

    @Override
    public void fxomDocumentDidRefreshSceneGraph() {
        updateHitNodeChrome();
    }

    @Override
    public void dropTargetDidChange() {
        // Should not be invoked : if drag gesture starts, editor controller
        // will switch to EditModeController.
        assert false;
    }
    
    
    /*
     * Private
     */
    

    private void startListeningToInputEvents() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        assert glassLayer.getOnMousePressed() == null;
        
        glassLayer.setOnMousePressed(mousePressedOnGlassLayerListener);
    }
    
    private void stopListeningToInputEvents() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnMousePressed(null);
    }
    
    private final EventHandler<MouseEvent> mousePressedOnGlassLayerListener
            = e -> mousePressedOnGlassLayer(e);
    
    
    private void mousePressedOnGlassLayer(MouseEvent e) {
        
        
        final Selection selection 
                = contentPanelController.getEditorController().getSelection();
        
        final FXOMDocument fxomDocument
                = contentPanelController.getEditorController().getFxomDocument();
        
        final FXOMObject hitObject;
        final Node hitNode;
        if ((fxomDocument == null) || (fxomDocument.getFxomRoot() == null)) {
            hitObject = null;
            hitNode = null;
        } else {
            final FXOMObject fxomRoot = fxomDocument.getFxomRoot();
            final Object sceneGraphRoot = fxomRoot.getSceneGraphObject();
            if (sceneGraphRoot instanceof Node) {
                hitNode = Deprecation.pick((Node)sceneGraphRoot, e.getSceneX(), e.getSceneY());
                FXOMObject fxomObject = null;
                Node node = hitNode;
                while ((fxomObject == null) && (node != null)) {
                    fxomObject = fxomRoot.searchWithSceneGraphObject(node);
                    node = node.getParent();
                }
                hitObject = fxomObject;
            } else {
                hitObject = null;
                hitNode = null;
            }
        }
        
        if (hitObject == null) {
            selection.clear();
        } else {
            if (selection.isSelected(hitObject)) {
                assert selection.getGroup() instanceof ObjectSelectionGroup;
                selection.updateHitObject(hitObject, hitNode);
            } else {
                selection.select(hitObject, hitNode);
            }
        }
    }
    
    
    private void updateHitNodeChrome() {
        final Selection selection = contentPanelController.getEditorController().getSelection();
        final HitNodeChrome newChrome;
        
        if ((hitNodeChrome == null) 
                || (hitNodeChrome.getFxomObject() != selection.getHitItem())
                || (hitNodeChrome.getHitNode() != selection.getCheckedHitNode())) {
            if ((selection.getHitItem() != null) && (selection.getCheckedHitNode() != null)){
                newChrome = makeHitNodeChrome(selection.getHitItem(), selection.getCheckedHitNode());
            } else {
                newChrome = null;
            }
        } else {
            switch(hitNodeChrome.getState()) {
                default:
                case CLEAN:
                    newChrome = hitNodeChrome;
                    break;
                case NEEDS_RECONCILE:
                    newChrome = hitNodeChrome;
                    hitNodeChrome.reconcile();
                    break;
                case NEEDS_REPLACE:
                    newChrome = makeHitNodeChrome(selection.getHitItem(), selection.getCheckedHitNode());
                    assert newChrome.getState() == HitNodeChrome.State.CLEAN;
                    break;
            }
        }
        
        if (newChrome != hitNodeChrome) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            if (hitNodeChrome != null) {
                rudderLayer.getChildren().remove(hitNodeChrome.getRootNode());
            }
            hitNodeChrome = newChrome;
            if (hitNodeChrome != null) {
                rudderLayer.getChildren().add(hitNodeChrome.getRootNode());
            }
        } else {
            assert (hitNodeChrome == null) || hitNodeChrome.getState() == AbstractPring.State.CLEAN;
        }
    }
    
    
    private HitNodeChrome makeHitNodeChrome(FXOMObject hitItem, Node hitNode) {
        final HitNodeChrome result;
        
        assert hitItem != null;
        
        /*
         * In some cases, we cannot make a chrome for some hitObject
         * 
         *  MenuButton          <= OK
         *      CustomMenuItem  <= KO because MenuItem are not displayable (case #1)
         *          CheckBox    <= KO because this CheckBox is in a separate scene (Case #2)
         */
        
        final AbstractDriver driver = contentPanelController.lookupDriver(hitItem);
        if (driver == null) {
            // Case #1 above
            result = null;
        } else {
            final FXOMObject closestNodeObject = hitItem.getClosestNode();
            if (closestNodeObject == null) {
                // Document content is not displayable in content panel
                result = null;
            } else {
                assert closestNodeObject.getSceneGraphObject() instanceof Node;
                final Node closestNode = (Node)closestNodeObject.getSceneGraphObject();
                if (closestNode.getScene() == contentPanelController.getPanelRoot().getScene()) {
                    result = new HitNodeChrome(contentPanelController, hitItem, hitNode);
                } else {
                    // Case #2 above
                    result = null;
                }
            }
        }
        
        return result;
    }
    
    
    private void removeHitNodeChrome() {
        if (hitNodeChrome != null) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            rudderLayer.getChildren().remove(hitNodeChrome.getRootNode());
            hitNodeChrome = null;
        }
    }
}
