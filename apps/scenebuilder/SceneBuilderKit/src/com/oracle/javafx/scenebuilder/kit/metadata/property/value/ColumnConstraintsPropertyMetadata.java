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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.Objects;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.ColumnConstraints;

/**
 *
 */
public class ColumnConstraintsPropertyMetadata extends ComplexPropertyMetadata<ColumnConstraints> {
    
    private static final ColumnConstraints DEFAULT = new ColumnConstraints();

    private final BooleanPropertyMetadata fillWidthMetadata
            = new BooleanPropertyMetadata(new PropertyName("fillWidth"), 
                    true, DEFAULT.isFillWidth(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata maxWidthMetadata
            = new DoublePropertyMetadata(new PropertyName("maxWidth"), 
                    DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE, true,
                    DEFAULT.getMaxWidth(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata minWidthMetadata
            = new DoublePropertyMetadata(new PropertyName("minWidth"), 
            DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE, true, 
                    DEFAULT.getMinWidth(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata percentWidthMetadata
            = new DoublePropertyMetadata(new PropertyName("percentWidth"), 
            DoublePropertyMetadata.DoubleKind.PERCENTAGE, true, 
                    DEFAULT.getPercentWidth(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata prefWidthMetadata
            = new DoublePropertyMetadata(new PropertyName("prefWidth"), 
            DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE, true, 
                    DEFAULT.getPrefWidth(), InspectorPath.UNUSED);
    private final EnumerationPropertyMetadata halignmentMetadata
            = new EnumerationPropertyMetadata(new PropertyName("halignment"),
            VPos.class, EnumerationPropertyMetadata.EQUIV_INHERITED, true, InspectorPath.UNUSED);
    private final EnumerationPropertyMetadata hgrowMetadata
            = new EnumerationPropertyMetadata(new PropertyName("hgrow"),
            Priority.class, EnumerationPropertyMetadata.EQUIV_INHERITED, true, InspectorPath.UNUSED);
    
    public ColumnConstraintsPropertyMetadata(PropertyName name, boolean readWrite, 
            ColumnConstraints defaultValue, InspectorPath inspectorPath) {
        super(name, ColumnConstraints.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * Utility
     */
    
    public static boolean equals(ColumnConstraints c1, ColumnConstraints c2) {
        assert c1 != null;
        assert c2 != null;

        final boolean result;
        if (c1 == c2) {
            result = true;
        } else {
            result = Objects.equals(c1.getHalignment(),c2.getHalignment())
                    && Objects.equals(c1.getHgrow(), c2.getHgrow())
                    && MathUtils.equals(c1.getMaxWidth(), c2.getMaxWidth())
                    && MathUtils.equals(c1.getMinWidth(), c2.getMinWidth())
                    && MathUtils.equals(c1.getPercentWidth(), c2.getPercentWidth())
                    && MathUtils.equals(c1.getPrefWidth(), c2.getPrefWidth());
        }
        
        return result;
    }
    
    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(ColumnConstraints value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, value.getClass());
        
        fillWidthMetadata.setValue(result, value.isFillWidth());
        maxWidthMetadata.setValue(result, value.getMaxWidth());
        minWidthMetadata.setValue(result, value.getMinWidth());
        percentWidthMetadata.setValue(result, value.getPercentWidth());
        prefWidthMetadata.setValue(result, value.getPrefWidth());
        
        final HPos halignment = value.getHalignment();
        if (halignment == null) {
            halignmentMetadata.setValue(result, halignmentMetadata.getDefaultValue());
        } else {
            halignmentMetadata.setValue(result, halignment.toString());
        }
        
        final Priority hgrow = value.getHgrow();
        if (hgrow == null) {
            hgrowMetadata.setValue(result, hgrowMetadata.getDefaultValue());
        } else {
            hgrowMetadata.setValue(result, hgrow.toString());
        }

        return result;
    }
    
}
