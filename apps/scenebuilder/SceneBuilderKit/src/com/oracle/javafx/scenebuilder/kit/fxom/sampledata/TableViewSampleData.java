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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 *
 */
class TableViewSampleData extends AbstractSampleData {
    
    private final List<SampleDataItem> sampleItems = new ArrayList<>();

    public TableViewSampleData() {
        for (int i = 0; i < 20; i++) {
            sampleItems.add(new SampleDataItem(i));
        }
    }

    public static boolean canApplyTo(TableView<?> tableView) {
        final boolean result;
        
        /*
         * We can insert sample data if:
         * 1) TableView.items() is empty
         * 2) TableView columns have no cell factory set
         */
        
        if (tableView.getItems().isEmpty() == false) {
            result = false;
        } else {
            final List<TableColumn<?, ?>> columns = new ArrayList<>();
            columns.addAll(tableView.getColumns());
            while (columns.isEmpty() == false) {
                final TableColumn<?,?> tc = columns.get(0);
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
        assert sceneGraphObject instanceof TableView;
        
        @SuppressWarnings("unchecked")        
        final TableView<SampleDataItem> tableView = (TableView<SampleDataItem>) sceneGraphObject;
        
        tableView.getItems().clear();
        tableView.getItems().addAll(sampleItems);
        
        final List<TableColumn<SampleDataItem, ?>> columns = new ArrayList<>(tableView.getColumns());
        while (columns.isEmpty() == false) {
            @SuppressWarnings("unchecked")        
            final TableColumn<SampleDataItem,String> tc 
                    = (TableColumn<SampleDataItem,String>)columns.get(0);
            tc.setCellValueFactory(SampleDataItem.FACTORY);
            columns.remove(0);
            columns.addAll(tc.getColumns());
        }
    }

    @Override
    public void removeFrom(Object sceneGraphObject) {
        assert sceneGraphObject instanceof TableView;
        
        @SuppressWarnings("unchecked")        
        final TableView<SampleDataItem> tableView = TableView.class.cast(sceneGraphObject);
        tableView.getItems().clear();
        
        final List<TableColumn<SampleDataItem, ?>> columns = new ArrayList<>();
        columns.addAll(tableView.getColumns());
        while (columns.isEmpty() == false) {
            @SuppressWarnings("unchecked")        
            final TableColumn<SampleDataItem,String> tc 
                    = (TableColumn<SampleDataItem,String>)columns.get(0);
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
        
        public final static PropertyValueFactory<SampleDataItem, String> FACTORY
                = new PropertyValueFactory<>("prop"); //NOI18N
        
        public SampleDataItem(int index) {
            this.index = index;
        }
        
        public String getProp() {
            return TableViewSampleData.lorem(index);
        }
    }
}
