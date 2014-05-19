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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.ListValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class DoubleArrayPropertyMetadata extends ListValuePropertyMetadata<Double> {
    
    private final static PropertyName unusedName
            = new PropertyName("unused"); //NOI18N
    private final static DoublePropertyMetadata doubleMetadata
            = new DoublePropertyMetadata(unusedName, 
                    DoublePropertyMetadata.DoubleKind.COORDINATE,
                    true, 0.0, InspectorPath.UNUSED);

    public DoubleArrayPropertyMetadata(PropertyName name, boolean readWrite, List<Double> defaultValue, InspectorPath inspectorPath) {
        super(name, Double.class, doubleMetadata, readWrite, defaultValue, inspectorPath);
    }

    
    public void synchronizeWithSceneGraphObject(FXOMInstance fxomInstance) {
        /*
         * This routine transfers property value from the scene graph object
         * to the matching FXOMProperty. This is primarily used for the
         * SplitPane.dividerPositions property.
         */
        
        final Object sceneGraphValue = getValueInSceneGraphObject(fxomInstance);
        final List<Double> value;
        if (sceneGraphValue == null) {
            value = getDefaultValue(); // Mmmh... not so sure
        } else {
            assert sceneGraphValue.getClass().getComponentType() == double.class;
            value = new ArrayList<>();
            for (double d : (double[]) sceneGraphValue) {
                value.add(Double.valueOf(d));
            }
        }
        
        setValue(fxomInstance, value);
    }
}
