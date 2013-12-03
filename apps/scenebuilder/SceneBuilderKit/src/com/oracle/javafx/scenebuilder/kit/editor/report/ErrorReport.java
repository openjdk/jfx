/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.report;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNode;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * 
 */
public class ErrorReport {
    
    private final Map<FXOMNode, List<ErrorReportEntry>> entries = new HashMap<>();
    private FXOMDocument fxomDocument;
    private boolean dirty = true;
    
    public void setFxomDocument(FXOMDocument fxomDocument) {
        this.fxomDocument = fxomDocument;
        requestUpdate();
    }
    
    public void requestUpdate() {
        this.entries.clear();
        this.dirty = true;
    }
    
    public List<ErrorReportEntry> query(FXOMObject fxomObject, boolean recursive) {
        final List<ErrorReportEntry> result;
        
        updateReport();
        
        final List<ErrorReportEntry> collected = new ArrayList<>();
        if (recursive) {
            collectEntries(fxomObject, collected);
        } else {
            if (entries.get(fxomObject) != null) {
                collected.addAll(entries.get(fxomObject));
            }
            if (fxomObject instanceof FXOMInstance) {
                final FXOMInstance fxomInstance = (FXOMInstance) fxomObject;
                for (FXOMProperty fxomProperty : fxomInstance.getProperties().values()) {
                    if (entries.get(fxomProperty) != null) {
                        collected.addAll(entries.get(fxomProperty));
                    }
                }
            }
        }
        
        if (collected.isEmpty()) {
            result = null;
        } else {
            result = collected;
        }
        
        assert (result == null) || (result.size() >= 1);
        
        return result;
    }
    
    public Map<FXOMNode, List<ErrorReportEntry>> getEntries() {
        updateReport();
        return Collections.unmodifiableMap(entries);
    }
    
    /*
     * Private
     */
    
    
    private void updateReport() {
        if (dirty) {
            assert entries.isEmpty();
            if (fxomDocument != null) {
                buildReport(fxomDocument.getFxomRoot());
            }
            dirty = false;
        }
    }
    
    private void buildReport(FXOMNode fxomNode) {
        
        final List<ErrorReportEntry> nodeEntries = new ArrayList<>();
        
        if (fxomNode instanceof FXOMCollection) {
            final FXOMCollection fxomCollection = (FXOMCollection) fxomNode;
            for (FXOMObject item : fxomCollection.getItems()) {
                buildReport(item);
            }
            verifySceneGraphObject(fxomCollection);
        } else if (fxomNode instanceof FXOMInstance) {
            final FXOMInstance fxomInstance = (FXOMInstance) fxomNode;
            for (FXOMProperty fxomProperty : fxomInstance.getProperties().values()) {
                buildReport(fxomProperty);
            }
            verifySceneGraphObject(fxomInstance);
        } else if (fxomNode instanceof FXOMIntrinsic) {
            verifyFxomIntrinsic((FXOMIntrinsic) fxomNode);
            
        } else if (fxomNode instanceof FXOMPropertyC) {
            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomNode;
            for (FXOMObject value : fxomPropertyC.getValues()) {
                buildReport(value);
            }
        } else if (fxomNode instanceof FXOMPropertyT) {
            verifyFxomPropertyT((FXOMPropertyT) fxomNode);
        }
        
        if (nodeEntries.isEmpty() == false) {
            entries.put(fxomNode, nodeEntries);
        }
    }
    
    
    private void verifySceneGraphObject(FXOMObject fxomObject) {
        if (fxomObject.getSceneGraphObject() == null) {
            final ErrorReportEntry newEntry 
                    = new ErrorReportEntry(fxomObject, ErrorReportEntry.Type.UNRESOLVED_CLASS);
            addEntry(fxomObject, newEntry);
        }
    }
    
    private void verifyFxomIntrinsic(FXOMIntrinsic fxomIntrinsic) {
        assert fxomIntrinsic != null;
        assert fxomIntrinsic.getParentObject() != null;
        
        if (fxomIntrinsic.getType() == FXOMIntrinsic.Type.FX_INCLUDE) {
            final String source = fxomIntrinsic.getSource();
            final FXOMObject parentObject = fxomIntrinsic.getParentObject();
            
            if (source.startsWith("@")) { //NOI18N
                final URL location = parentObject.resolveLocation(source);
                final boolean ok;

                if (location == null) {
                    ok = false;
                } else {
                    final String path = location.getPath();
                    if (path == null) {
                        ok = false;
                    } else {
                        final File file = new File(path);
                        ok = file.canRead();
                    }
                }
                if (ok == false) {
                    final ErrorReportEntry newEntry 
                            = new ErrorReportEntry(fxomIntrinsic, ErrorReportEntry.Type.UNRESOLVED_LOCATION);
                    addEntry(fxomIntrinsic, newEntry);
                }
            }
        }
    }
    
    private void verifyFxomPropertyT(FXOMPropertyT fxomProperty) {
        
        assert fxomProperty != null;
        assert fxomProperty.getParentInstance() != null : "fxomProperty="+fxomProperty.getName(); //NOI18N
        
        final String value = fxomProperty.getValue();
        final FXOMInstance parentInstance = fxomProperty.getParentInstance();
        
        if (value.startsWith("@")) { //NOI18N
            final URL location = parentInstance.resolveLocation(value);
            final boolean ok;
            
            if (location == null) {
                ok = false;
            } else {
                final String path = location.getPath();
                if (path == null) {
                    ok = false;
                } else {
                    final File file = new File(path);
                    ok = file.canRead();
                }
            }
            if (ok == false) {
                final ErrorReportEntry newEntry 
                        = new ErrorReportEntry(fxomProperty, ErrorReportEntry.Type.UNRESOLVED_LOCATION);
                addEntry(fxomProperty, newEntry);
            }
        }/* else if (value.startsWith("%")) {
            // It's a "resource" expression
            // TODO(elp)
        }*/
    }
        
    private void addEntry(FXOMNode fxomNode, ErrorReportEntry newEntry) {
        List<ErrorReportEntry> nodeEntries = entries.get(fxomNode);
        if (nodeEntries == null) {
            nodeEntries = new ArrayList<> ();
            entries.put(fxomNode, nodeEntries);
        }
        nodeEntries.add(newEntry);
    }
    
    
    private void collectEntries(FXOMNode fxomNode, List<ErrorReportEntry> collected) {
        assert fxomNode != null;
        assert collected != null;
        
        final List<ErrorReportEntry> nodeEntries = entries.get(fxomNode);
        if (nodeEntries != null) {
            collected.addAll(nodeEntries);
        }
        
        if (fxomNode instanceof FXOMCollection) {
            final FXOMCollection fxomCollection = (FXOMCollection) fxomNode;
            for (FXOMObject item : fxomCollection.getItems()) {
                collectEntries(item, collected);
            }
        } else if (fxomNode instanceof FXOMInstance) {
            final FXOMInstance fxomInstance = (FXOMInstance) fxomNode;
            for (FXOMProperty fxomProperty : fxomInstance.getProperties().values()) {
                collectEntries(fxomProperty, collected);
            }
        } else if (fxomNode instanceof FXOMPropertyC) {
            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomNode;
            for (FXOMObject value : fxomPropertyC.getValues()) {
                collectEntries(value, collected);
            }
        }
    }
}
