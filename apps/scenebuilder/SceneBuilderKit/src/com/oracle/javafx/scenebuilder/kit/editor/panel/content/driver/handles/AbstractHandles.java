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

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.AbstractDecoration;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.net.URL;
import javafx.scene.Node;
import javafx.scene.image.Image;

/**
 *
 * 
 */
public abstract class AbstractHandles<T> extends AbstractDecoration<T> {
    
    public static final String SELECTION_RECT = "selection-rect"; //NOI18N
    public static final String SELECTION_WIRE = "selection-wire"; //NOI18N
    public static final String SELECTION_HANDLES = "selection-handles"; //NOI18N
    public static final double SELECTION_HANDLES_SIZE = 10.0; // pixels
    
    private static Image squareHandleImage = null;
    private static Image sideHandleImage = null;
    
    public AbstractHandles(ContentPanelController contentPanelController,
            FXOMObject fxomObject, Class<T> sceneGraphClass) {
        super(contentPanelController, fxomObject, sceneGraphClass);
    }
    
    public abstract AbstractGesture findGesture(Node node);
    
    private static final String HANDLES = "HANDLES";
    
    public static AbstractHandles<?> lookupHandles(Node node) {
        assert node != null;
        assert node.isMouseTransparent() == false;
        
        final AbstractHandles<?> result;
        final Object value = node.getProperties().get(HANDLES);
        if (value instanceof AbstractHandles) {
            result = (AbstractHandles) value;
        } else {
            assert value == null;
            result = null;
        }
        
        return result;
    }
    
    public static void attachHandles(Node node, AbstractHandles<?> handles) {
        assert node != null;
        assert node.isMouseTransparent() == false;
        assert lookupHandles(node) == null;
        
        if (handles == null) {
            node.getProperties().remove(HANDLES);
        } else {
            node.getProperties().put(HANDLES, handles);
        }
    }
    
    public synchronized static Image getCornerHandleImage() {
        if (squareHandleImage == null) {
            final URL url = AbstractHandles.class.getResource("corner-handle.png");
            squareHandleImage = new Image(url.toString());
        }
        return squareHandleImage;
    }
    
    public synchronized static Image getSideHandleImage() {
        if (sideHandleImage == null) {
            final URL url = AbstractHandles.class.getResource("side-handle.png");
            sideHandleImage = new Image(url.toString());
        }
        return sideHandleImage;
    }
    
}
