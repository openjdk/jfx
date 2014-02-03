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
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TableViewDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.DebugMouseGesture;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.transform.Transform;

/**
 *
 * 
 */

public class TableColumnHandles extends AbstractResilientHandles<Object> {
    
    /*
     * Handles for TableColumn need a special treatment.
     * 
     * A TableColumn instance can be transiently disconnected from its parent TableView:
     *  - TableColumn.getTableView() returns null
     *  - TableView.getColumns().contains() returns false
     * 
     * When the TableColumn is disconnected, handles cannot be drawn.
     * This Handles class inherits from AbstractResilientHandles to take
     * care of this singularity.
     */
    
    private final TableViewDesignInfoX tableViewDesignInfo
            = new TableViewDesignInfoX();
    private TableView<?> tableView;
    
    public TableColumnHandles(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance) {
        super(contentPanelController, fxomInstance, Object.class);
        assert fxomInstance.getSceneGraphObject() instanceof TableColumn;
        
        getTableColumn().tableViewProperty().addListener(
                new ChangeListener<Object>() {
                    @Override
                    public void changed(ObservableValue<? extends Object> ov, Object v1, Object v2) {
                        tableViewOrVisibilityDidChange();
                    }
                });
        getTableColumn().visibleProperty().addListener(
                new ChangeListener<Object>() {
                    @Override
                    public void changed(ObservableValue<? extends Object> ov, Object v1, Object v2) {
                        tableViewOrVisibilityDidChange();
                    }
                });
        
        tableViewOrVisibilityDidChange();
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
        assert tableView != null;
        assert getTableColumn().isVisible();
        return tableViewDesignInfo.getColumnBounds(getTableColumn());
    }

    @Override
    public Transform getSceneGraphToSceneTransform() {
        assert isReady();
        assert tableView != null;
        return tableView.getLocalToSceneTransform();
    }

    @Override
    public Point2D sceneGraphObjectToScene(double x, double y) {
        assert isReady();
        assert tableView != null;
        return tableView.localToScene(x, y);
    }

    @Override
    public Point2D sceneToSceneGraphObject(double x, double y) {
        assert isReady();
        assert tableView != null;
        return tableView.sceneToLocal(x, y);
    }

    @Override
    protected void startListeningToSceneGraphObject() {
        assert isReady();
        assert tableView != null;
        startListeningToLayoutBounds(tableView);
        startListeningToLocalToSceneTransform(tableView);
    }

    @Override
    protected void stopListeningToSceneGraphObject() {
        assert isReady();
        assert tableView != null;
        stopListeningToLayoutBounds(tableView);
        stopListeningToLocalToSceneTransform(tableView);
    }

    @Override
    public AbstractGesture findGesture(Node node) {
        return new DebugMouseGesture(getContentPanelController(), "Resize gesture for TableColumn");
    }


    /*
     * Private
     */
    
    private TableColumn<?,?> getTableColumn() {
        assert getSceneGraphObject() instanceof TableColumn;
        return (TableColumn<?,?>) getSceneGraphObject();
    }
    
    
    private void tableViewOrVisibilityDidChange() {
        tableView = getTableColumn().getTableView();
        setReady((tableView != null) && getTableColumn().isVisible());
    }
}
