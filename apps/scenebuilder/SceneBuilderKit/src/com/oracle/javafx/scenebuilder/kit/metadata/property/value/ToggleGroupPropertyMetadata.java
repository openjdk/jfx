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

package com.oracle.javafx.scenebuilder.kit.metadata.property.value;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.control.ToggleGroup;

/**
 *
 */
public class ToggleGroupPropertyMetadata extends SingleValuePropertyMetadata<String> {

    public ToggleGroupPropertyMetadata(PropertyName name, boolean readWrite, String defaultValue, InspectorPath inspectorPath) {
        super(name, String.class, readWrite, defaultValue, inspectorPath);
    }

    
    /*
     * SingleValuePropertyMetadata
     */
    
    @Override
    public String makeValueFromString(String string) {
        final PrefixedValue pv = new PrefixedValue(string);
        final String result;
        
        if (pv.isExpression()) {
            result = pv.getSuffix();
        } else {
            assert false : "Unexpected prefixed value " + string;
            result = null;
        }
        
        return result;
    }

    @Override
    public String makeValueFromFxomInstance(FXOMInstance valueFxomInstance) {
        final String result;
        
        if (valueFxomInstance.getDeclaredClass() == ToggleGroup.class) {
            result = valueFxomInstance.getFxId();
        } else {
            assert false : "unexpected declared class "
                    + valueFxomInstance.getDeclaredClass().getSimpleName();
            result = null;
        }
        
        return result;
    }

    @Override
    public boolean canMakeStringFromValue(String value) {
        throw new UnsupportedOperationException("Should not be invoked"); //NOI18N
    }

    @Override
    public String makeStringFromValue(String value) {
        throw new UnsupportedOperationException("Should not be invoked"); //NOI18N
    }

    @Override
    public FXOMInstance makeFxomInstanceFromValue(String value, FXOMDocument fxomDocument) {
        throw new UnsupportedOperationException("Should not be invoked"); //NOI18N
    }
    
}
