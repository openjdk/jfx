/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.EditAction;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchDocumentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.GridPane;

/**
 * Job used to support Modify > GridPane > Increase/Decrease Row/Column span.
 * This job is multi-selection proof: it will do something on all items of the
 * selection if and only if required conditions are met by all of them.
 */
public class SpanJob extends BatchDocumentJob {
    private final EditAction editAction;

    public SpanJob(EditorController editorController, EditAction editAction) {
        super(editorController);
        this.editAction = editAction;
    }

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> jobList = new ArrayList<>();
        final AbstractSelectionGroup selectionGroup = getEditorController().getSelection().getGroup();
        
        // Do we have an asset selected which is a standard one (not a grid) ?
        if (selectionGroup instanceof ObjectSelectionGroup) {
            // Is that asset enclosed in a grid ?
            if (selectionGroup.getAncestor() != null
                    && selectionGroup.getAncestor().getSceneGraphObject() instanceof GridPane) {
                DesignHierarchyMask gridDHM = new DesignHierarchyMask(selectionGroup.getAncestor());
                int columnCount = gridDHM.getColumnsSize();
                int rowCount = gridDHM.getRowsSize();
                List<FXOMObject> items = ((ObjectSelectionGroup)selectionGroup).getSortedItems();
                
                // Create a job for all items then check each is executable.
                // As soon as one is not executable the job list is made empty
                // so that no change will be performed by the job, eventually.
                for (FXOMObject fxomObject : items) {
                    if (fxomObject instanceof FXOMInstance) {
                        Job job = createJob((FXOMInstance)fxomObject, columnCount, rowCount);

                        if (job.isExecutable()) {
                            jobList.add(job);
                        } else {
                            jobList.clear();
                            break;
                        }
                    }
                }
            }
        }
        
        return jobList;
    }

    @Override
    protected String makeDescription() {
        String description = ""; //NOI18N
        
        switch (editAction) {
            default:
            case DECREASE_COLUMN_SPAN:
                description = I18N.getString("job.decrease.column.span");
                break;
            case INCREASE_COLUMN_SPAN:
                description = I18N.getString("job.increase.column.span");
                break;
            case DECREASE_ROW_SPAN:
                description = I18N.getString("job.decrease.row.span");
                break;
            case INCREASE_ROW_SPAN:
                description = I18N.getString("job.increase.row.span");
                break;
        }
        
        assert ! description.isEmpty();
        return description;
    }
    
    private Job createJob(FXOMInstance candidate, int columnCount, int rowCount) {
        PropertyName propName = null;
        int newSpan = 1;
        
        switch (editAction) {
            default:
            case DECREASE_COLUMN_SPAN:
                newSpan = getNewSpan(TREND.DECREASE,
                        getValue(PROPERTY.COLUMN_INDEX, candidate),
                        getValue(PROPERTY.COLUMN_SPAN, candidate), columnCount);
                propName = new PropertyName(getName(PROPERTY.COLUMN_SPAN), GridPane.class);
                break;
            case INCREASE_COLUMN_SPAN:
                newSpan = getNewSpan(TREND.INCREASE,
                        getValue(PROPERTY.COLUMN_INDEX, candidate),
                        getValue(PROPERTY.COLUMN_SPAN, candidate), columnCount);
                propName = new PropertyName(getName(PROPERTY.COLUMN_SPAN), GridPane.class);
                break;
            case DECREASE_ROW_SPAN:
                newSpan = getNewSpan(TREND.DECREASE,
                        getValue(PROPERTY.ROW_INDEX, candidate),
                        getValue(PROPERTY.ROW_SPAN, candidate), rowCount);
                propName = new PropertyName(getName(PROPERTY.ROW_SPAN), GridPane.class);
                break;
            case INCREASE_ROW_SPAN:
                newSpan = getNewSpan(TREND.INCREASE,
                        getValue(PROPERTY.ROW_INDEX, candidate),
                        getValue(PROPERTY.ROW_SPAN, candidate), rowCount);
                propName = new PropertyName(getName(PROPERTY.ROW_SPAN), GridPane.class);
                break;
        }

        final ValuePropertyMetadata vpm
                = Metadata.getMetadata().queryValueProperty(candidate, propName);
        final ModifyObjectJob columnSpanJob = new ModifyObjectJob(
                candidate, vpm, newSpan, getEditorController());

        return columnSpanJob;
    }
    
    // May return a value identical to given span one.
    private int getNewSpan(TREND trend, int index, int span, int count) {
        int newSpan = span;
        switch(trend) {
            case DECREASE:
                if (span > 1) {
                    newSpan = span - 1;
                }
                break;
            case INCREASE:
                if (index+1 < count && span < count) {
                    newSpan = span + 1;
                }
                break;
        }
        
        return newSpan;
    }

    private enum TREND {DECREASE, INCREASE};
    private enum PROPERTY {COLUMN_INDEX, COLUMN_SPAN, ROW_INDEX, ROW_SPAN};
    
    private int getValue(PROPERTY property, FXOMInstance candidate) {
        String propertyName = getName(property);
        final PropertyName propName = new PropertyName(propertyName, GridPane.class);
        final ValuePropertyMetadata vpm
                = Metadata.getMetadata().queryValueProperty(candidate, propName);
        Object value = vpm.getValueObject(candidate);
        
        // Span value can be null
        if (value == null && (property == PROPERTY.COLUMN_SPAN || property == PROPERTY.ROW_SPAN)) {
            value = Integer.valueOf(1);
        }
        assert value instanceof Integer;
        
        return (Integer)value;
    }
    
    private String getName(PROPERTY property) {
        String propertyName = ""; //NOI18N
        
        switch (property) {
            case COLUMN_INDEX:
                propertyName = "columnIndex"; //NOI18N
                break;
            case COLUMN_SPAN:
                propertyName = "columnSpan"; //NOI18N
                break;
            case ROW_INDEX:
                propertyName = "rowIndex"; //NOI18N
                break;
            case ROW_SPAN:
                propertyName = "rowSpan"; //NOI18N
                break;
        }
        
        assert ! propertyName.isEmpty();
        return propertyName;
    }
    
}
