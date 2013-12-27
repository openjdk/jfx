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
package com.oracle.javafx.scenebuilder.kit.editor.panel.library;

import com.oracle.javafx.scenebuilder.kit.editor.panel.library.ImportWindowController.PrefSize;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReportEntry;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 */
class ImportRow {

    private final BooleanProperty importRequired;
    private final JarReportEntry jre;
    private PrefSize prefSize;
    private final String canonicalClassName;

    public ImportRow(boolean importRequired, JarReportEntry jre, PrefSize prefSize) {
        this.importRequired = new SimpleBooleanProperty(importRequired);
        this.jre = jre;
        this.canonicalClassName = jre.getKlass().getCanonicalName();

        if (prefSize == null) {
            this.prefSize = PrefSize.DEFAULT;
        } else {
            this.prefSize = prefSize;
        }
    }

    public final BooleanProperty importRequired() {
        return importRequired;
    }

    public boolean isImportRequired() {
        return importRequired.get();
    }

    public void setImportRequired(boolean v) {
        importRequired().set(v);
    }

    public JarReportEntry getJarReportEntry() {
        return this.jre;
    }

    public PrefSize getPrefSize() {
        return this.prefSize;
    }

    public void setPrefSize(PrefSize value) {
        this.prefSize = value;
    }
    
    public String getCanonicalClassName() {
        return this.canonicalClassName;
    }
    
    /**
     * Used by the CheckBoxListCell
     * @return 
     */
    @Override
    public String toString() {
        return this.jre.getKlass().getSimpleName();
    }

}
