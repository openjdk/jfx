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

package com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.GridBounds;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class GridSnapshot {
    
    private final Map<FXOMObject, GridSnapshotItem> items = new HashMap<>();
    
    public GridSnapshot(Collection<FXOMObject> fxomObjects) {
        assert fxomObjects != null;
        assert fxomObjects.isEmpty() == false; // (1)
        
        for (FXOMObject fxomObject : fxomObjects) {
            assert items.containsKey(fxomObject) == false;
            items.put(fxomObject, new GridSnapshotItem(fxomObject));
        }
    }
    
    public GridSnapshot(Collection<FXOMObject> fxomObjects, int columnCount) {
        assert fxomObjects != null;
        assert fxomObjects.isEmpty() == false;
        assert columnCount >= 1;
        
        int columnIndex = 0;
        int rowIndex = 0;
        for (FXOMObject fxomObject : fxomObjects) {
            items.put(fxomObject, new GridSnapshotItem(fxomObject, columnIndex, rowIndex));
            columnIndex++;
            if (columnIndex >= columnCount) {
                columnIndex = 0;
                rowIndex++;
            }
        }
    }
    
    public int getColumnIndex(FXOMObject fxomObject) {
        assert fxomObject != null;
        assert items.containsKey(fxomObject);
        return items.get(fxomObject).getColumnIndex();
    }
    
    public int getRowIndex(FXOMObject fxomObject) {
        assert fxomObject != null;
        assert items.containsKey(fxomObject);
        return items.get(fxomObject).getRowIndex();
    }
    
    public GridBounds getBounds() {
        GridBounds result = null;
        
        for (Map.Entry<FXOMObject, GridSnapshotItem> e : items.entrySet()) {
            if (result == null) {
                result = e.getValue().getBounds();
            } else {
                result = result.union(e.getValue().getBounds());
            }
        }
        
        assert result != null; // Because (1)
        
        return result;
    }
}
