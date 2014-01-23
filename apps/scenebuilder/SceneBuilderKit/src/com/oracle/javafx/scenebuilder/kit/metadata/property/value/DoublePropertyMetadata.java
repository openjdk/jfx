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

import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.layout.Region;

/**
 *
 * 
 */
public class DoublePropertyMetadata extends TextEncodablePropertyMetadata<java.lang.Double> {
    
    public enum DoubleKind {
        COORDINATE,         // any double
        NULLABLE_COORDINATE,// any double or null
        SIZE,               // x >= 0
        USE_COMPUTED_SIZE,      // x >= 0 or x == Region.USE_COMPUTED_SIZE
        USE_PREF_SIZE,          // x >= 0 or x == Region.USE_COMPUTED_SIZE or x == Region.USE_PREF_SIZE
        EFFECT_SIZE,        // 0 <= x <= 255.0
        ANGLE,              // 0 <= x < 360
        OPACITY,            // 0 <= x <= 1.0
        PROGRESS,           // 0 <= x <= 1.0
        PERCENTAGE          // -1 or 0 <= x <= 100.0
    };

    private final DoubleKind kind;

    public DoublePropertyMetadata(PropertyName name, DoubleKind kind,
            boolean readWrite, Double defaultValue, InspectorPath inspectorPath) {
        super(name, Double.class, readWrite, defaultValue, inspectorPath);
        assert (kind != DoubleKind.NULLABLE_COORDINATE) || (defaultValue == null);
        this.kind = kind;
    }
    
    public DoubleKind getKind() {
        return kind;
    }
    
    public boolean isValidValue(Double value) {
        final boolean result;
        
        if (kind == DoubleKind.NULLABLE_COORDINATE) {
            result = true;
        } else if (value == null) {
            result = false;
        } else {
            switch(kind) {
                case COORDINATE:
                    result = true;
                    break;
                case SIZE:
                    result = (0 <= value);
                    break;
                case USE_COMPUTED_SIZE:
                    result = ((0 <= value) || (value == Region.USE_COMPUTED_SIZE));
                    break;
                case USE_PREF_SIZE:
                    result = (0 <= value) 
                            || (value == Region.USE_COMPUTED_SIZE)
                            || (value == Region.USE_PREF_SIZE);
                    break;
                case PERCENTAGE:
                    result = (value == -1) || ((0 <= value) && (value <= 100.0));
                    break;
                case EFFECT_SIZE:
                case ANGLE:
                case OPACITY:
                case PROGRESS:
                    result = true;
                    break;

                default:
                    assert false;
                    result = false;
                    break;
            }
        }
        
        return result;
    }
    
    public Double getCanonicalValue(Double value) {
        final Double result;
        
        if (value == null) {
            result = null;
        } else {
            switch(kind) {
                case COORDINATE:
                case NULLABLE_COORDINATE:
                case SIZE:
                case USE_COMPUTED_SIZE:
                case USE_PREF_SIZE:
                    result = value;
                    break;
                case EFFECT_SIZE:
                    result = Math.min(255.0, Math.max(0, value));
                    break;
                case ANGLE:
                    result = Math.IEEEremainder(value, 360.0);
                    break;
                case OPACITY:
                case PROGRESS:
                    result = Math.min(1, Math.max(0, value));
                    break;
                default:
                    assert false;
                    result = value;
                    break;
            }
        }
        
        return result;
    }

    /*
     * SingleValuePropertyMetadata
     */
    
    @Override
    public Double makeValueFromString(String string) {
        return Double.valueOf(string);
    }
}
