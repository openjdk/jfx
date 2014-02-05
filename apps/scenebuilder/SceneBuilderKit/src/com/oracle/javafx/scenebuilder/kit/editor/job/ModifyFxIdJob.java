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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.JavaLanguage;
import java.util.Objects;

/**
 * Job used to modify an fx:id.
 *
 */
public class ModifyFxIdJob extends Job {

    private final FXOMObject fxomObject;
    private final String newValue;
    private final String oldValue;

    public ModifyFxIdJob(FXOMObject fxomObject, String newValue, EditorController editorController) {
        super(editorController);

        assert fxomObject != null;
        assert fxomObject.getSceneGraphObject() != null;

        this.fxomObject = fxomObject;
        this.newValue = newValue;
        this.oldValue = fxomObject.getFxId();
    }

    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        return Objects.equals(oldValue, newValue) == false
                && ((newValue == null) || JavaLanguage.isIdentifier(newValue));
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void undo() {
        getEditorController().getFxomDocument().beginUpdate();
        this.fxomObject.setFxId(oldValue);
        getEditorController().getFxomDocument().endUpdate();
        assert Objects.equals(fxomObject.getFxId(), oldValue);
    }

    @Override
    public void redo() {
        getEditorController().getFxomDocument().beginUpdate();
        this.fxomObject.setFxId(newValue);
        getEditorController().getFxomDocument().endUpdate();
        assert Objects.equals(fxomObject.getFxId(), newValue);
    }

    @Override
    public String getDescription() {
        final String result;
        
        if (newValue == null) {
            assert oldValue != null;
            result = I18N.getString("job.remove.fxid", oldValue);
        } else {
            result = I18N.getString("job.set.fxid", newValue);
        }
        
        return result;
    }
}
