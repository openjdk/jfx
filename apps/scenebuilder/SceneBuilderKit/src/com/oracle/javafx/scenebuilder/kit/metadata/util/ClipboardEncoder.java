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

package com.oracle.javafx.scenebuilder.kit.metadata.util;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMArchive;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.List;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

/**
 *
 */
public class ClipboardEncoder {
    
    // Internal SB2 data format
    static final DataFormat SB_DATA_FORMAT 
            = new DataFormat("com.oracle.javafx.scenebuilder2/internal"); //NOI18N

    // FXML Format
    static final DataFormat FXML_DATA_FORMAT 
            = new DataFormat("com.oracle.javafx/fxml"); //NOI18N
    
    private final List<FXOMObject> fxomObjects;

    public ClipboardEncoder(List<FXOMObject> fxomObjects) {
        assert fxomObjects != null;
        this.fxomObjects = fxomObjects;
    }
    
    public boolean isEncodable() {
        return fxomObjects.isEmpty() == false;
    }
    
    public ClipboardContent makeEncoding() {
        assert isEncodable();
        
        final ClipboardContent result = new ClipboardContent();
        final FXOMArchive fxomArchive = new FXOMArchive(fxomObjects);
        
        // SB_DATA_FORMAT
        result.put(SB_DATA_FORMAT, new FXOMArchive(fxomObjects));
            
        // FXML_DATA_FORMAT
        final FXOMArchive.Entry entry0 = fxomArchive.getEntries().get(0);
        result.put(FXML_DATA_FORMAT, entry0.getFxmlText());
        result.put(DataFormat.PLAIN_TEXT, entry0.getFxmlText());
        
        // DataFormat.IMAGE
        final FXOMObject fxomObject0 = fxomObjects.get(0);
        if (fxomObject0.getSceneGraphObject() instanceof ImageView) {
            final ImageView imageView = (ImageView) fxomObject0.getSceneGraphObject();
            if (imageView.getImage() != null) {
                result.put(DataFormat.IMAGE, imageView.getImage());
            }
        }
        
        return result;
    }
}
