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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value.keycombination;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ComplexPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 *
 */
public class KeyCombinationPropertyMetadata extends ComplexPropertyMetadata<KeyCombination> {

    private final KeyCodeCombinationPropertyMetadata keyCodeCombinationMetadata;
    private final KeyCharacterCombinationPropertyMetadata keyCharacterCombinationMetadata;

    public KeyCombinationPropertyMetadata(PropertyName name, boolean readWrite, 
            KeyCombination defaultValue, InspectorPath inspectorPath) {
        super(name, KeyCombination.class, readWrite, defaultValue, inspectorPath);
        keyCodeCombinationMetadata = new KeyCodeCombinationPropertyMetadata(name, readWrite, null, inspectorPath);
        keyCharacterCombinationMetadata = new KeyCharacterCombinationPropertyMetadata(name, readWrite, null, inspectorPath);
    }

    /*
     * ComplexPropertyMetadata
     */
    @Override
    protected KeyCombination castValue(Object value) {
        return (KeyCombination) value;
    }


    @Override
    protected void updateFxomPropertyWithValue(FXOMProperty fxomProperty, KeyCombination value) {
        assert fxomProperty instanceof FXOMPropertyC;
        assert value != null;

        final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
        assert fxomPropertyC.getValues().size() == 1;

        FXOMObject valueObject = fxomPropertyC.getValues().get(0);
        if (valueObject instanceof FXOMInstance) {
            final FXOMInstance currentValueInstance = (FXOMInstance) valueObject;
            final Class<?> currentValueClass = currentValueInstance.getDeclaredClass();

            if (currentValueClass != value.getClass()) {
                // Eg current value is a KeyCodeCombination, new value is a KeyCharacterCombination
                final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
                final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
                updateFxomInstanceWithValue(valueInstance, value);
                valueInstance.addToParentProperty(0, fxomPropertyC);
                valueObject.removeFromParentProperty();
            } else {
                updateFxomInstanceWithValue(currentValueInstance, value);
            }
        } else {
            assert valueObject instanceof FXOMIntrinsic;

            final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
            final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, value.getClass());
            updateFxomInstanceWithValue(valueInstance, value);
            valueInstance.addToParentProperty(0, fxomPropertyC);
            valueObject.removeFromParentProperty();
        }
    }

    @Override
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, KeyCombination value) {
        if (value instanceof KeyCodeCombination) {
            keyCodeCombinationMetadata.updateFxomInstanceWithValue(valueInstance, (KeyCodeCombination) value);
        } else {
            assert value instanceof KeyCharacterCombination;
            keyCharacterCombinationMetadata.updateFxomInstanceWithValue(valueInstance, (KeyCharacterCombination) value);
        }
    }

}
