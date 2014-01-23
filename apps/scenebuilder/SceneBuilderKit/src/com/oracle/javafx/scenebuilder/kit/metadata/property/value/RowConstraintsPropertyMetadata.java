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
import javafx.geometry.VPos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 *
 */
public class RowConstraintsPropertyMetadata extends ComplexPropertyMetadata<RowConstraints> {
    
    private static final RowConstraints DEFAULT = new RowConstraints();

    private final BooleanPropertyMetadata fillHeightMetadata
            = new BooleanPropertyMetadata(new PropertyName("fillHeight"), 
                    true, DEFAULT.isFillHeight(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata maxHeightMetadata
            = new DoublePropertyMetadata(new PropertyName("maxHeight"), 
                    DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE, true,
                    DEFAULT.getMaxHeight(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata minHeightMetadata
            = new DoublePropertyMetadata(new PropertyName("minHeight"), 
            DoublePropertyMetadata.DoubleKind.USE_COMPUTED_SIZE, true, 
                    DEFAULT.getMinHeight(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata percentHeightMetadata
            = new DoublePropertyMetadata(new PropertyName("percentHeight"), 
            DoublePropertyMetadata.DoubleKind.PERCENTAGE, true, 
                    DEFAULT.getPercentHeight(), InspectorPath.UNUSED);
    private final DoublePropertyMetadata prefHeightMetadata
            = new DoublePropertyMetadata(new PropertyName("prefHeight"), 
            DoublePropertyMetadata.DoubleKind.USE_PREF_SIZE, true, 
                    DEFAULT.getPrefHeight(), InspectorPath.UNUSED);
    private final EnumerationPropertyMetadata valignmentMetadata
            = new EnumerationPropertyMetadata(new PropertyName("valignment"),
            VPos.class, EnumerationPropertyMetadata.EQUIV_INHERITED, true, InspectorPath.UNUSED);
    private final EnumerationPropertyMetadata vgrowMetadata
            = new EnumerationPropertyMetadata(new PropertyName("vgrow"),
            Priority.class, EnumerationPropertyMetadata.EQUIV_INHERITED, true, InspectorPath.UNUSED);
    
    public RowConstraintsPropertyMetadata(PropertyName name, boolean readWrite, 
            RowConstraints defaultValue, InspectorPath inspectorPath) {
        super(name, RowConstraints.class, readWrite, defaultValue, inspectorPath);
    }

    /*
     * Utility
     */
    
    public static boolean equals(RowConstraints r1, RowConstraints r2) {
        assert r1 != null;
        assert r2 != null;

        final boolean result;
        if (r1 == r2) {
            result = true;
        } else {
            result = Objects.equals(r1.getValignment(),r2.getValignment())
                    && Objects.equals(r1.getVgrow(), r2.getVgrow())
                    && MathUtils.equals(r1.getMaxHeight(), r2.getMaxHeight())
                    && MathUtils.equals(r1.getMinHeight(), r2.getMinHeight())
                    && MathUtils.equals(r1.getPercentHeight(), r2.getPercentHeight())
                    && MathUtils.equals(r1.getPrefHeight(), r2.getPrefHeight());
        }
        
        return result;
    }
    
    /*
     * ComplexPropertyMetadata
     */
    
    @Override
    public FXOMInstance makeFxomInstanceFromValue(RowConstraints value, FXOMDocument fxomDocument) {
        final FXOMInstance result = new FXOMInstance(fxomDocument, getValueClass());
        
        fillHeightMetadata.setValue(result, value.isFillHeight());
        maxHeightMetadata.setValue(result, value.getMaxHeight());
        minHeightMetadata.setValue(result, value.getMinHeight());
        percentHeightMetadata.setValue(result, value.getPercentHeight());
        prefHeightMetadata.setValue(result, value.getPrefHeight());
        
        final VPos valignment = value.getValignment();
        if (valignment == null) {
            valignmentMetadata.setValue(result, valignmentMetadata.getDefaultValue());
        } else {
            valignmentMetadata.setValue(result, valignment.toString());
        }
        final Priority vgrow = value.getVgrow();
        if (vgrow == null) {
            vgrowMetadata.setValue(result, vgrowMetadata.getDefaultValue());
        } else {
            vgrowMetadata.setValue(result, vgrow.toString());
        }

        return result;
    }
    
}
