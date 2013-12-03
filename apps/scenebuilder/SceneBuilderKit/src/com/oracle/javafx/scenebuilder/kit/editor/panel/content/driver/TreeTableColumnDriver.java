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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ContainerZDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.TreeTableColumnHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.TreeTableColumnPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.AbstractResizer;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.AbstractTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.TreeTableColumnTring;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

/**
 *
 */
public class TreeTableColumnDriver extends AbstractDriver {

    public TreeTableColumnDriver(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    /*
     * AbstractDriver
     */
    
    @Override
    public AbstractHandles<?> makeHandles(FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof TreeTableColumn;
        assert fxomObject instanceof FXOMInstance;
        return new TreeTableColumnHandles(contentPanelController, (FXOMInstance)fxomObject);
    }
    
    @Override
    public AbstractPring<?> makePring(FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof TreeTableColumn;
        assert fxomObject instanceof FXOMInstance;
        return new TreeTableColumnPring(contentPanelController, (FXOMInstance) fxomObject);
    }

    @Override
    public AbstractTring<?> makeTring(AbstractDropTarget dropTarget) {
        assert dropTarget != null;
        assert dropTarget.getTargetObject() instanceof FXOMInstance;
        assert dropTarget.getTargetObject().getSceneGraphObject() instanceof TreeTableColumn;
        
        return new TreeTableColumnTring(contentPanelController, (FXOMInstance) dropTarget.getTargetObject());
    }

    @Override
    public AbstractResizer<?> makeResizer(FXOMObject fxomObject) {
        assert false;
        return null;
    }
    
    @Override
    public FXOMObject refinePick(Node hitNode, double sceneX, double sceneY, FXOMObject fxomObject) {
        // TODO(elp) : implement TableColumnDriver.refinePick()
        return fxomObject;
    }

    @Override
    public AbstractDropTarget makeDropTarget(FXOMObject fxomObject, double sceneX, double sceneY) {
        // TODO(elp) : temporary implementation
        assert fxomObject instanceof FXOMInstance;
        return new ContainerZDropTarget((FXOMInstance)fxomObject, null);
    }

    @Override
    public Node getInlineEditorBounds(FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof TreeTableColumn;
        final TreeTableColumn<?,?> tc = (TreeTableColumn) fxomObject.getSceneGraphObject();
        final TreeTableViewDesignInfoX di = new TreeTableViewDesignInfoX();
        return di.getColumnNode(tc);
    }

    @Override
    public boolean intersectsBounds(FXOMObject fxomObject, Bounds bounds) {
        assert fxomObject.getSceneGraphObject() instanceof TreeTableColumn;
        
        final TreeTableColumn<?,?> tc 
                = (TreeTableColumn) fxomObject.getSceneGraphObject();
        final TreeTableView<?> tv 
                = tc.getTreeTableView();
        final TreeTableViewDesignInfoX di 
                = new TreeTableViewDesignInfoX();
        final Bounds tcBounds 
                = tv.localToScene(di.getColumnBounds(tc));
        
        return tcBounds.intersects(bounds);
    }
}
