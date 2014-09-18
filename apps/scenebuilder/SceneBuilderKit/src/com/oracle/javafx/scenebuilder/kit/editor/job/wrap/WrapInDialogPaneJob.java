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
package com.oracle.javafx.scenebuilder.kit.editor.job.wrap;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.DialogPane;

/**
 * Job used to wrap selection in a DialogPane. Will use the CONTENT property.
 */
public class WrapInDialogPaneJob extends AbstractWrapInJob {

    public WrapInDialogPaneJob(EditorController editorController) {
        super(editorController);
        newContainerClass = DialogPane.class;
    }

    @Override
    protected boolean canWrapIn() {
        final boolean result;
        if (super.canWrapIn()) { // (1)
            // Can wrap in CONTENT property single selection only
            final Selection selection = getEditorController().getSelection();
            assert selection.getGroup() instanceof ObjectSelectionGroup; // Because of (1)
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            result = osg.getItems().size() == 1;
        } else {
            result = false;
        }
        return result;
    }
    
    @Override
    protected List<Job> wrapChildrenJobs(final List<FXOMObject> children) {

        final List<Job> jobs = new ArrayList<>();

        final DesignHierarchyMask newContainerMask
                = new DesignHierarchyMask(newContainer);
        assert newContainerMask.isAcceptingAccessory(Accessory.DP_CONTENT);

        // Retrieve the new container property name to be used
        final PropertyName newContainerPropertyName
                = new PropertyName("content"); //NOI18N
        // Create the new container property
        final FXOMPropertyC newContainerProperty = new FXOMPropertyC(
                newContainer.getFxomDocument(), newContainerPropertyName);

        assert children.size() == 1;
        // Update children before adding them to the new container
        jobs.addAll(modifyChildrenJobs(children));

        // Add the children to the new container
        jobs.addAll(addChildrenJobs(newContainerProperty, children));

        // Add the new container property to the new container instance
        assert newContainerProperty.getParentInstance() == null;
        final Job addPropertyJob = new AddPropertyJob(
                newContainerProperty,
                newContainer,
                -1, getEditorController());
        jobs.add(addPropertyJob);

        return jobs;
    }
}
