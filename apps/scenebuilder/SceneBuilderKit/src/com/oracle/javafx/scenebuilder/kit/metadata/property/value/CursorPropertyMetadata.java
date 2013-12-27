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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;

/**
 *
 * 
 */
public class CursorPropertyMetadata extends ComplexPropertyMetadata<Cursor> {

    private static Map<Cursor, String> cursorMap;
    
    public CursorPropertyMetadata(PropertyName name, boolean readWrite, 
            Cursor defaultValue, InspectorPath inspectorPath) {
        super(name, Cursor.class, readWrite, defaultValue, inspectorPath);
    }

    public static synchronized Map<Cursor, String> getCursorMap() {
        if (cursorMap == null) {
            cursorMap = new HashMap<>();
            cursorMap.put(Cursor.CLOSED_HAND,   "CLOSED_HAND"   );
            cursorMap.put(Cursor.CROSSHAIR,     "CROSSHAIR"     );
            cursorMap.put(Cursor.DEFAULT,       "DEFAULT"       );
            cursorMap.put(Cursor.DISAPPEAR,     "DISAPPEAR"     );
            cursorMap.put(Cursor.E_RESIZE,      "E_RESIZE"      );
            cursorMap.put(Cursor.HAND,          "HAND"          );
            cursorMap.put(Cursor.H_RESIZE,      "H_RESIZE"      );
            cursorMap.put(Cursor.MOVE,          "MOVE"          );
            cursorMap.put(Cursor.NE_RESIZE,     "NE_RESIZE"     );
            cursorMap.put(Cursor.NONE,          "NONE"          );
            cursorMap.put(Cursor.NW_RESIZE,     "NW_RESIZE"     );
            cursorMap.put(Cursor.N_RESIZE,      "N_RESIZE"      );
            cursorMap.put(Cursor.OPEN_HAND,     "OPEN_HAND"     );
            cursorMap.put(Cursor.SE_RESIZE,     "SE_RESIZE"     );
            cursorMap.put(Cursor.SW_RESIZE,     "SW_RESIZE"     );
            cursorMap.put(Cursor.S_RESIZE,      "S_RESIZE"      );
            cursorMap.put(Cursor.TEXT,          "TEXT"          );
            cursorMap.put(Cursor.V_RESIZE,      "V_RESIZE"      );
            cursorMap.put(Cursor.WAIT,          "WAIT"          );
            cursorMap.put(Cursor.W_RESIZE,      "W_RESIZE"      );
            cursorMap = Collections.unmodifiableMap(cursorMap);
        }
        
        return cursorMap;
    }
    
    
    /*
     * ComplexPropertyMetadata
     */
    @Override
    public FXOMProperty makeFxomPropertyFromValue(FXOMInstance fxomInstance, Cursor value) {
        assert fxomInstance != null;
        assert value != null;
        
        final FXOMDocument fxomDocument = fxomInstance.getFxomDocument();
        final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, computeDeclaredClass(value));
        updateFxomInstanceWithValue(valueInstance, value);
        return new FXOMPropertyC(fxomDocument, getName(), valueInstance);
    }

    
    @Override
    protected void updateFxomPropertyWithValue(FXOMProperty fxomProperty, Cursor value) {
        assert value != null;
        assert fxomProperty instanceof FXOMPropertyC; // Because it's *Complex*PropertyMetadata
        
        final Class<?> cursorDeclaredClass = computeDeclaredClass(value);
        
        final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
        assert fxomPropertyC.getValues().size() == 1;
        
        FXOMObject valueObject = fxomPropertyC.getValues().get(0);
        if (valueObject instanceof FXOMInstance) {
            final FXOMInstance valueInstance = (FXOMInstance) valueObject;
            if (valueInstance.getDeclaredClass() == cursorDeclaredClass) {
                updateFxomInstanceWithValue((FXOMInstance) valueObject, value);
            } else {
                final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
                final FXOMInstance newValueInstance = new FXOMInstance(fxomDocument, cursorDeclaredClass);
                updateFxomInstanceWithValue(newValueInstance, value);
                newValueInstance.addToParentProperty(0, fxomPropertyC);
                valueObject.removeFromParentProperty();
            }
        } else {
            final FXOMDocument fxomDocument = fxomProperty.getFxomDocument();
            final FXOMInstance valueInstance = new FXOMInstance(fxomDocument, cursorDeclaredClass);
            updateFxomInstanceWithValue(valueInstance, value);
            valueInstance.addToParentProperty(0, fxomPropertyC);
            valueObject.removeFromParentProperty();
        }
    }
    
    @Override
    protected Cursor castValue(Object value) {
        final Cursor result;
        
        /*
         * To simplify other routines of CursorPropertyMetadata, 
         * we make sure that "value" is:
         * 0) either null
         * 1) either a standard cursor (eg Cursor.OPEN_HAND)
         * 2) either an ImageCursor instance
         * If not, we replace "value" by Cursor.DEFAULT (3).
         */
        if (value == null) {             // (0)
            result = null;
        } else if (getCursorMap().get((Cursor)value) != null) {
            result = (Cursor) value;     // (1)
        } else {
            if (value instanceof ImageCursor) {
                result = (Cursor) value; // (2)
            } else {                          
                result = Cursor.DEFAULT; // (3)
            }
        }

        return result;
    }
    
    @Override
    protected void updateFxomInstanceWithValue(FXOMInstance valueInstance, Cursor value) {
        /*
         *      <cursor> <Cursor fx:constant="CLOSED_HAND" /> </cursor>
         */
        
        final String cursorName = getCursorMap().get(value);
        if (cursorName != null) {
            // It's a standard cursor
            assert valueInstance.getDeclaredClass() == Cursor.class;
            valueInstance.setFxConstant(cursorName);
        } else {
            assert value instanceof ImageCursor;
            assert valueInstance.getDeclaredClass() == ImageCursor.class;
            updateFxomInstanceWithImageCursor(valueInstance, (ImageCursor) value);
        }
        
    }
    
    /*
     * Private
     */
    
    private Class<?> computeDeclaredClass(Cursor value) {
        final Class<?> result;
        
        final String cursorName = getCursorMap().get(value);
        if (cursorName != null) {
            result = Cursor.class;
        } else {
            assert value instanceof ImageCursor; // See castValue() comments
            result = ImageCursor.class;
        }
        
        return result;
    }
    
    private final DoublePropertyMetadata hotspotXMetadata
            = new DoublePropertyMetadata(new PropertyName("hotspotX"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final DoublePropertyMetadata hotspotYMetadata
            = new DoublePropertyMetadata(new PropertyName("hotspotY"), 
            DoublePropertyMetadata.DoubleKind.COORDINATE, true, 0.0, InspectorPath.UNUSED);
    private final ImagePropertyMetadata imageMetadata
            = new ImagePropertyMetadata(new PropertyName("image"), 
            true, null, InspectorPath.UNUSED);
    
    private void updateFxomInstanceWithImageCursor(FXOMInstance valueInstance, 
            ImageCursor imageCursor) {
        hotspotXMetadata.setValue(valueInstance, imageCursor.getHotspotX());
        hotspotYMetadata.setValue(valueInstance, imageCursor.getHotspotY());
        imageMetadata.setValue(valueInstance, imageCursor.getImage());
    }
}

