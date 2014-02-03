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

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.AbstractResilientHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TreeTableViewDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.DebugMouseGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.transform.Transform;

/**
 *
 * 
 */

public class TreeTableColumnHandles extends AbstractResilientHandles<Object> {
    
    /*
     * Handles for TreeTableColumn need a special treatment.
     * 
     * A TreeTableColumn instance can be transiently disconnected from its parent TreeTableView:
     *  - TreeTableColumn.getTableView() returns null
     *  - TreeTableView.getColumns().contains() returns false
     * 
     * When the TreeTableColumn is disconnected, handles cannot be drawn.
     * This Handles class inherits from AbstractResilientHandles to take
     * care of this singularity.
     */
    
    private final TreeTableViewDesignInfoX tableViewDesignInfo
            = new TreeTableViewDesignInfoX();
    private TreeTableView<?> treeTableView;
    
    public TreeTableColumnHandles(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance) {
        super(contentPanelController, fxomInstance, Object.class);
        assert fxomInstance.getSceneGraphObject() instanceof TreeTableColumn;
        
        getTreeTableColumn().treeTableViewProperty().addListener(
                new ChangeListener<Object>() {
                    @Override
                    public void changed(ObservableValue<? extends Object> ov, Object v1, Object v2) {
                        treeTableViewOrVisibilityDidChange();
                    }
                });
        getTreeTableColumn().visibleProperty().addListener(
                new ChangeListener<Object>() {
                    @Override
                    public void changed(ObservableValue<? extends Object> ov, Object v1, Object v2) {
                        treeTableViewOrVisibilityDidChange();
                    }
                });
        
        treeTableViewOrVisibilityDidChange();
    }
    
    public FXOMInstance getFxomInstance() {
        return (FXOMInstance) getFxomObject();
    }

    /*
     * AbstractGenericHandles
     */
    @Override
    public Bounds getSceneGraphObjectBounds() {
        assert isReady();
        assert treeTableView != null;
        assert getTreeTableColumn().isVisible();
        return tableViewDesignInfo.getColumnBounds(getTreeTableColumn());
    }

    @Override
    public Transform getSceneGraphToSceneTransform() {
        assert isReady();
        assert treeTableView != null;
        return treeTableView.getLocalToSceneTransform();
    }

    @Override
    public Point2D sceneGraphObjectToScene(double x, double y) {
        assert isReady();
        assert treeTableView != null;
        return treeTableView.localToScene(x, y);
    }

    @Override
    public Point2D sceneToSceneGraphObject(double x, double y) {
        assert isReady();
        assert treeTableView != null;
        return treeTableView.sceneToLocal(x, y);
    }

    @Override
    protected void startListeningToSceneGraphObject() {
        assert isReady();
        assert treeTableView != null;
        startListeningToLayoutBounds(treeTableView);
        startListeningToLocalToSceneTransform(treeTableView);
    }

    @Override
    protected void stopListeningToSceneGraphObject() {
        assert isReady();
        assert treeTableView != null;
        stopListeningToLayoutBounds(treeTableView);
        stopListeningToLocalToSceneTransform(treeTableView);
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        return new DebugMouseGesture(getContentPanelController(), "Resize gesture for TreeTableColumn");
    }


    /*
     * Private
     */
    
    private TreeTableColumn<?,?> getTreeTableColumn() {
        assert getSceneGraphObject() instanceof TreeTableColumn;
        return (TreeTableColumn<?,?>) getSceneGraphObject();
    }
    
    
    private void treeTableViewOrVisibilityDidChange() {
        treeTableView = getTreeTableColumn().getTreeTableView();
        setReady((treeTableView != null) && getTreeTableColumn().isVisible());
    }
}
