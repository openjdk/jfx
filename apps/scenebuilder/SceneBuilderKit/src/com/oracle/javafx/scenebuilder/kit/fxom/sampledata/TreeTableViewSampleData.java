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

package com.oracle.javafx.scenebuilder.kit.fxom.sampledata;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 *
 */
class TreeTableViewSampleData extends AbstractSampleData {
    
    private final TreeItem<SampleDataItem> sampleRoot;

    public TreeTableViewSampleData() {
        int i = 0;
        sampleRoot = new TreeItem<>(new SampleDataItem(i++));
        sampleRoot.setExpanded(true);
        for (int j = 0; j<10; j++) {
            final Rectangle r = new Rectangle(10, 10);
            r.setFill(TreeTableViewSampleData.color(i));
            TreeItem<SampleDataItem> child = new TreeItem<>(new SampleDataItem(i++));
            child.setExpanded(true);
            child.setGraphic(r);
            for (int k=0; k<3; k++) {
                final TreeItem<SampleDataItem> child2 = new TreeItem<>(new SampleDataItem(i++));
                child2.setExpanded(true);
                final Circle c = new Circle(5);
                c.setFill(TreeTableViewSampleData.color(i));
                child2.setGraphic(c);
                child.getChildren().add(child2);
            }
            sampleRoot.getChildren().add(child);
        }
    }

    public static boolean canApplyTo(TreeTableView<?> treeTableView) {
        final boolean result;
        
        /*
         * We can insert sample data if:
         * 1) TreeTableView.items() is null
         * 2) TreeTableView columns have no cell factory set
         */
        
        if (treeTableView.getRoot() != null) {
            result = false;
        } else {
            final List<TreeTableColumn<?, ?>> columns = new ArrayList<>();
            columns.addAll(treeTableView.getColumns());
            while (columns.isEmpty() == false) {
                final TreeTableColumn<?,?> tc = columns.get(0);
                if (tc.getCellValueFactory() == null) {
                    columns.remove(0);
                    columns.addAll(tc.getColumns());
                } else {
                    break;
                }
            }
            
            result = columns.isEmpty();
        }
        
        return result;
    }
    
    
    /*
     * AbstractSampleData
     */
    
    
    @Override
    public void applyTo(Object sceneGraphObject) {
        assert sceneGraphObject instanceof TreeTableView;
        
        @SuppressWarnings("unchecked")        
        final TreeTableView<SampleDataItem> tableView = (TreeTableView<SampleDataItem>) sceneGraphObject;
        
        tableView.setRoot(sampleRoot);
        
        final List<TreeTableColumn<SampleDataItem, ?>> columns = new ArrayList<>(tableView.getColumns());
        while (columns.isEmpty() == false) {
            @SuppressWarnings("unchecked")        
            final TreeTableColumn<SampleDataItem,String> ttc 
                    = (TreeTableColumn<SampleDataItem,String>)columns.get(0);
            ttc.setCellValueFactory(SampleDataItem.FACTORY);
            columns.remove(0);
            columns.addAll(ttc.getColumns());
        }
    }

    @Override
    public void removeFrom(Object sceneGraphObject) {
        assert sceneGraphObject instanceof TreeTableView;
        
        @SuppressWarnings("unchecked")        
        final TreeTableView<SampleDataItem> tableView = TreeTableView.class.cast(sceneGraphObject);
        tableView.setRoot(null);
        
        final List<TreeTableColumn<SampleDataItem, ?>> columns = new ArrayList<>();
        columns.addAll(tableView.getColumns());
        while (columns.isEmpty() == false) {
            @SuppressWarnings("unchecked")        
            final TreeTableColumn<SampleDataItem,String> tc 
                    = (TreeTableColumn<SampleDataItem,String>)columns.get(0);
            tc.setCellValueFactory(null);
            columns.remove(0);
            columns.addAll(tc.getColumns());
        }
    }
  
    
    /*
     * Private
     */
    
    
    public static class SampleDataItem {
        int index;
        
        public final static TreeItemPropertyValueFactory<SampleDataItem, String> FACTORY
                = new TreeItemPropertyValueFactory<>("prop"); //NOI18N
        
        public SampleDataItem(int index) {
            this.index = index;
        }
        
        public String getProp() {
            return TreeTableViewSampleData.lorem(index);
        }
    }
}
