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
package com.oracle.javafx.scenebuilder.kit.fxom;

import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.ColumnConstraintsListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.RowConstraintsListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.List;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;

/**
 *
 * 
 */
class FXOMNormalizer {
    
    /*
     * We look for properties with $null values and remove them.
     * Exemple with ScrollPane.content.
     *
     *      <ScrollPane content="$null" ... />
     *
     * is transformed in:
     *
     *      <ScrollPane ... />
     *
     */
    
    /*
     * 
     * We look for the following pattern:
     * 
     * <Accordion>
     *   <expandedPane>
     *     <TitledPane fx:id="x1" text="B">
     *       ...
     *     </TitledPane>
     *   </expandedPane>
     *   <panes>
     *     <TitledPane text="A">
     *       ...
     *     </TitledPane>
     *     <fx:reference source="x1" />
     *   </panes>
     * </Accordion>
     * 
     * 
     * and transform it as:
     * 
     * <Accordion>
     *   <panes>
     *     <TitledPane text="A">
     *       ...
     *     </TitledPane>
     *     <TitledPane text="B">
     *       ...
     *     </TitledPane>
     *   </panes>
     * </Accordion>
     * 
     * 
     */
    
    private static final PropertyName expandedPaneName
            = new PropertyName("expandedPane");
    
    private final FXOMDocument fxomDocument;
    private int changeCount;
    
    public FXOMNormalizer(FXOMDocument fxomDocument) {
        this.fxomDocument = fxomDocument;
    }
    
    public void normalize() {
        changeCount = 0;
        normalizeExpandedPaneProperties();
        normalizeGridPanes();
        if (changeCount >= 1) {
            fxomDocument.refreshSceneGraph();
        }
    }
    
    public void normalizeExpandedPaneProperties() {
        
        final List<FXOMProperty> expandedPaneProperties
                = fxomDocument.getFxomRoot().collectProperties(expandedPaneName);
        
        for (FXOMProperty p : expandedPaneProperties) {
            if (p instanceof FXOMPropertyC) {
                final FXOMPropertyC pc = (FXOMPropertyC) p;
                assert pc.getValues().isEmpty() == false;
                final FXOMObject v0 = pc.getValues().get(0);
                if (v0 instanceof FXOMInstance) {
                    normalizeExpandedPaneProperty(pc);
                } else {
                    assert v0 instanceof FXOMIntrinsic;
                    p.removeFromParentInstance();
                }
            } else {
                assert p instanceof FXOMPropertyT;
                final FXOMPropertyT pt = (FXOMPropertyT)p;
                assert pt.getValue().equals("$null");
                p.removeFromParentInstance();
            }
            
            changeCount++;
        }
    }
    
    private void normalizeExpandedPaneProperty(FXOMPropertyC p) {
        
        assert p != null;

        /*
         * 
         * <Accordion>                           // p.getParentInstance()
         *   <expandedPane>                      // p
         *     <TitledPane fx:id="x1" text="B">  // p.getValues().get(0)
         *       ...
         *     </TitledPane>
         *   </expandedPane>
         *   <panes>                             // reference.getParentProperty()         
         *     <TitledPane text="A">
         *       ...
         *     </TitledPane>
         *     <fx:reference source="x1" />      // reference
         *   </panes>
         * </Accordion>
         * 
         */
    
        final FXOMInstance parentInstance = p.getParentInstance();
        assert parentInstance != null;
        final FXOMObject titledPane = p.getValues().get(0);
        assert titledPane.getSceneGraphObject() instanceof TitledPane;
        assert titledPane.getFxId() != null;
        
        final FXOMObject fxomRoot = p.getFxomDocument().getFxomRoot();
        final List<FXOMIntrinsic> references 
                = fxomRoot.collectReferences(titledPane.getFxId());
        assert references.size() == 1;
        final FXOMIntrinsic reference = references.get(0);
        assert reference.getSource().equals(titledPane.getFxId());
        assert reference.getParentObject() == parentInstance;
        final int referenceIndex = reference.getIndexInParentProperty();
        
        p.removeFromParentInstance();
        titledPane.removeFromParentProperty();
        titledPane.addToParentProperty(referenceIndex, reference.getParentProperty());
        reference.removeFromParentProperty();
    }
    
    
    
    private void normalizeGridPanes() {
        final FXOMObject fxomRoot = fxomDocument.getFxomRoot();
        for (FXOMObject fxomGridPane : fxomRoot.collectObjectWithSceneGraphObjectClass(GridPane.class)) {
            normalizeGridPane(fxomGridPane);
            changeCount++;
        }
    }
    
    private final static ColumnConstraintsListPropertyMetadata columnConstraintsMeta
            = new ColumnConstraintsListPropertyMetadata();
    private final static RowConstraintsListPropertyMetadata rowConstraintsMeta
            = new RowConstraintsListPropertyMetadata();
    
    private void normalizeGridPane(FXOMObject fxomGridPane) {
        assert fxomGridPane instanceof FXOMInstance;
        assert fxomGridPane.getSceneGraphObject() instanceof GridPane;
        
        final GridPane gridPane = (GridPane) fxomGridPane.getSceneGraphObject();
        final int columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        final int rowCount = Deprecation.getGridPaneRowCount(gridPane);
        columnConstraintsMeta.unpack((FXOMInstance) fxomGridPane, columnCount);
        rowConstraintsMeta.unpack((FXOMInstance) fxomGridPane, rowCount);
    }
}
