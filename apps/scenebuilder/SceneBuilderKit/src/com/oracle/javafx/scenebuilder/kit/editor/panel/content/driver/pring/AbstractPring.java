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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.AbstractDecoration;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import javafx.scene.Node;
import javafx.scene.paint.Paint;

/**
 *
 * 
 */
public abstract class AbstractPring<T> extends AbstractDecoration<T> {
    
    public static final String PARENT_RING_CLASS = "parent-ring"; //NOI18N
    
    public AbstractPring(ContentPanelController contentPanelController,
            FXOMObject fxomObject, Class<T> sceneGraphClass) {
        super(contentPanelController, fxomObject, sceneGraphClass);
    }
    
    public abstract void changeStroke(Paint stroke);
    public abstract AbstractGesture findGesture(Node node);
    
    private static final String PRING = "PRING"; //NOI18N
    
    public static AbstractPring<?> lookupPring(Node node) {
        assert node != null;
        assert node.isMouseTransparent() == false;
        
        final AbstractPring<?> result;
        final Object value = node.getProperties().get(PRING);
        if (value instanceof AbstractPring) {
            result = (AbstractPring<?>) value;
        } else {
            assert value == null;
            result = null;
        }
        
        return result;
    }
    
    public static void attachPring(Node node, AbstractPring<?> pring) {
        assert node != null;
        assert node.isMouseTransparent() == false;
        assert lookupPring(node) == null;
        
        if (pring == null) {
            node.getProperties().remove(PRING);
        } else {
            node.getProperties().put(PRING, pring);
        }
    }
}
