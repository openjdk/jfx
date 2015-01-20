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

package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.Library;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AddTooltipToSelectionJob extends BatchSelectionJob {
    
    private Map<FXOMObject, FXOMObject> tooltipMap; // Initialized lazily

    public AddTooltipToSelectionJob(EditorController editorController) {
        super(editorController);
    }
    
    public Collection<FXOMObject> getTooltips() {
        constructTooltipMap();
        return tooltipMap.values();
    }

    /*
     * BatchSelectionJob
     */

    @Override
    protected List<Job> makeSubJobs() {
        
        constructTooltipMap();
        
        final List<Job> result = new LinkedList<>();
        for (Map.Entry<FXOMObject, FXOMObject> e : tooltipMap.entrySet()) {
            final FXOMObject fxomObject = e.getKey();
            final FXOMObject tooltipObject = e.getValue();
            final Job insertJob = new InsertAsAccessoryJob(
                    tooltipObject, fxomObject, 
                    DesignHierarchyMask.Accessory.TOOLTIP, 
                    getEditorController());
            result.add(insertJob);
        }
        
        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        final Collection<FXOMObject> contextMenus = tooltipMap.values();
        assert contextMenus.isEmpty() == false;
        final FXOMObject hitMenu = contextMenus.iterator().next();
        
        return new ObjectSelectionGroup(contextMenus, hitMenu, null);
    }

    /*
     * CompositeJob
     */
    
    @Override
    protected String makeDescription() {
        return I18N.getString("label.action.edit.add.tooltip");
    }
    
    
    /*
     * Private
     */
    
    private void constructTooltipMap() {
        if (tooltipMap == null) {
            tooltipMap = new LinkedHashMap<>();
            
            // Build the ContextMenu item from the library builtin items
            final String tooltipFxmlPath = "builtin/Tooltip.fxml"; //NOI18N
            final URL tooltipFxmlURL 
                    = BuiltinLibrary.class.getResource(tooltipFxmlPath);
            assert tooltipFxmlURL != null;

            final AbstractSelectionGroup asg = getEditorController().getSelection().getGroup();
            assert asg instanceof ObjectSelectionGroup; // Because of (1)
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;

            try {
                final String contextMenuFxmlText
                        = FXOMDocument.readContentFromURL(tooltipFxmlURL);

                final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
                final Library library = getEditorController().getLibrary();
                for (FXOMObject fxomObject : osg.getItems()) {
                    final FXOMDocument contextMenuDocument = new FXOMDocument(
                            contextMenuFxmlText,
                            tooltipFxmlURL, library.getClassLoader(), null);

                    assert contextMenuDocument != null;
                    final FXOMObject contextMenuObject = contextMenuDocument.getFxomRoot();
                    assert contextMenuObject != null;
                    contextMenuObject.moveToFxomDocument(fxomDocument);
                    assert contextMenuDocument.getFxomRoot() == null;

                    tooltipMap.put(fxomObject, contextMenuObject);
                }
            } catch(IOException x) {
                throw new IllegalStateException("Bug in " + getClass().getSimpleName(), x); //NOI18N
            }
        }
    }
}
