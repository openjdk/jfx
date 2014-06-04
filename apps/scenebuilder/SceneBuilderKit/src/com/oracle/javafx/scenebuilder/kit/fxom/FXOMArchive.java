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

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class FXOMArchive implements Serializable {
    
    private static final long serialVersionUID = 7777;

    private final List<Entry> entries = new ArrayList<>();
    
    public FXOMArchive(List<FXOMObject> fxomObjects) {
        assert fxomObjects != null;
        
        for (FXOMObject o : fxomObjects) {
            final URL location = o.getFxomDocument().getLocation();
            final String fxmlText = FXOMNodes.newDocument(o).getFxmlText();
            entries.add(new Entry(fxmlText, location));
        }
    }
    
    public List<Entry> getEntries() {
        return entries;
    }
    
    public List<FXOMObject> decode(FXOMDocument targetDocument)
    throws IOException {
        final List<FXOMObject> result = new ArrayList<>();
        
        assert targetDocument != null;
        
        for (Entry e : entries) {
            final URL location = e.getLocation();
            final String fxmlText = e.getFxmlText();
            final FXOMDocument d = new FXOMDocument(fxmlText, location, 
                    targetDocument.getClassLoader(), targetDocument.getResources());
            final FXOMObject fxomRoot = d.getFxomRoot();
            assert fxomRoot != null;
            fxomRoot.moveToFxomDocument(targetDocument);
            result.add(fxomRoot);
        }
        
        return result;
    }
    
    public static boolean isArchivable(Collection<FXOMObject> fxomObjects) {
        
        // Checks that fxom objects are all self contained
        int selfContainedCount = 0;
        FXOMFxIdIndex fxomIndex = null;
        for (FXOMObject o : fxomObjects) {
            if ((fxomIndex == null) || (fxomIndex.getFxomDocument() != o.getFxomDocument())) {
                fxomIndex = new FXOMFxIdIndex(o.getFxomDocument());
            }
            if (fxomIndex.isSelfContained(o)) {
                selfContainedCount++;
            }
        }
        
        return selfContainedCount == fxomObjects.size();
    }
    
    
    public static class Entry implements Serializable {
        
        private static final long serialVersionUID = 8888;
        
        private final String fxmlText;
        private final URL location;
        
        public Entry(String fxmlText, URL location) {
            this.fxmlText = fxmlText;
            this.location = location;
        }

        public String getFxmlText() {
            return fxmlText;
        }

        public URL getLocation() {
            return location;
        }
        
    }
}
