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
package com.oracle.javafx.scenebuilder.app.message;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractPopupController;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.stage.WindowEvent;

/**
 *
 */
public class MessagePopupController extends AbstractPopupController {
    
    private final MessagePanelController messagePanelController;
    
    public MessagePopupController(EditorController editorController) {
        this.messagePanelController = new MessagePanelController(editorController);
    }
    
    
    /*
     * AbstractPopupController
     */

    @Override
    protected void makeRoot() {
        setRoot(messagePanelController.getPanelRoot());
    }

    @Override
    protected void onHidden(WindowEvent event) {
    }

    @Override
    protected void anchorBoundsDidChange() {
        final Bounds lb = getAnchor().getLayoutBounds();
        messagePanelController.setPanelWidth(lb.getWidth());
        // This callback should not be needed for auto hiding popups
        // See RT-31292 : Popup does not auto hide when resizing the window
        updatePopupLocation();
    }
    
    @Override
    protected void anchorTransformDidChange() {
        // This callback should not be needed for auto hiding popups
        // See RT-31292 : Popup does not auto hide when resizing the window
        updatePopupLocation();
    }
    
    @Override
    protected void anchorXYDidChange() {
        // This callback should not be needed for auto hiding popups
        // See RT-31292 : Popup does not auto hide when resizing the window
        updatePopupLocation();
    }
    
    @Override
    protected void controllerDidCreatePopup() {
        getPopup().setAutoFix(false);
        getPopup().setAutoHide(true);
    }
    
    /**
     * Update the popup location below the anchor node.
     */
    @Override
    protected void updatePopupLocation() {
        final Bounds anchorBounds = getAnchor().getLayoutBounds();
        Point2D popupLocation;
        
        assert anchorBounds != null;
        
        // At exit time, closeRequestHandler() is not always called.
        // So this method can be invoked after the anchor has been removed the
        // scene. This looks like a bug in FX...
        // Anway we protect ourself by checking.
        if (getAnchor().getScene() != null) {
            popupLocation = getAnchor().localToScreen(anchorBounds.getMinX(), anchorBounds.getMaxY());
            getPopup().setX(popupLocation.getX());
            getPopup().setY(popupLocation.getY());
        }
    }
}
