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
package com.oracle.javafx.scenebuilder.app.skeleton;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import javafx.fxml.FXML;

/**
 *
 */
class SkeletonBuffer {

    private final FXOMDocument document;
    private final String INDENT = "    "; //NOI18N
    private final Set<String> imports = new TreeSet<>();
    private final StringBuilder variables = new StringBuilder();
    private final StringBuilder asserts = new StringBuilder();
    private TEXT_TYPE textType = TEXT_TYPE.WITHOUT_COMMENTS;
    private FORMAT_TYPE textFormat = FORMAT_TYPE.COMPACT;
    private final StringBuilder packageLine = new StringBuilder();
    private final StringBuilder classLine = new StringBuilder();
    private final StringBuilder header = new StringBuilder();
    private final StringBuilder initialize = new StringBuilder();
    private final StringBuilder handlers = new StringBuilder();

    enum TEXT_TYPE {

        WITH_COMMENTS, WITHOUT_COMMENTS
    };

    enum FORMAT_TYPE {

        COMPACT, FULL
    }

    SkeletonBuffer(FXOMDocument document) {
        assert document != null;
        this.document = document;
    }

    public void setTextType(TEXT_TYPE type) {
        this.textType = type;
    }

    public void setFormat(FORMAT_TYPE format) {
        this.textFormat = format;
    }

    private void constructHeader() {
        if (textType == TEXT_TYPE.WITH_COMMENTS) {
            String documentName = DocumentWindowController.makeTitle(document);
            final String title = I18N.getString("skeleton.window.title", documentName);
            header.append("/**\n"); //NOI18N
            header.append(" * "); //NOI18N
            header.append(title);
            header.append("\n */\n\n"); //NOI18N
        }
    }

    private void constructPackageLine() {
        String controller = document.getFxomRoot().getFxController();

        if (controller != null && !controller.isEmpty()
                && controller.indexOf(".") != -1 && controller.indexOf("$") == -1) { //NOI18N
            packageLine.append("package "); //NOI18N
            packageLine.append(controller.substring(0, controller.indexOf("."))); //NOI18N
            packageLine.append(";\n\n"); //NOI18N
        }
    }

    private void constructClassLine() {
        String controller = document.getFxomRoot().getFxController();
        classLine.append("\npublic "); //NOI18N

        if (controller != null && controller.indexOf("$") != -1) { //NOI18N
            classLine.append("static "); //NOI18N
        }
        
        classLine.append("class "); //NOI18N

        if (controller != null && !controller.isEmpty()) {
            String simpleName = controller.replace("$", "."); //NOI18N
            int dot = simpleName.lastIndexOf('.');
            if (dot > -1) {
                simpleName = simpleName.substring(dot+1);
            }
            classLine.append(simpleName);
        } else {
            classLine.append("PleaseProvideControllerClassName"); //NOI18N
        }

        classLine.append(" {\n\n"); //NOI18N
    }

    private void constructInitialize() {
        if (textFormat == FORMAT_TYPE.FULL) {
            initialize.append(INDENT);
            initialize.append("@FXML"); //NOI18N

            if (textType == TEXT_TYPE.WITH_COMMENTS) {
                initialize.append(" // This method is called by the FXMLLoader when initialization is complete\n"); //NOI18N
            } else {
                initialize.append("\n"); //NOI18N
            }

            initialize.append(INDENT);
            initialize.append("void initialize() {\n"); //NOI18N
            initialize.append(asserts);
            initialize.append("\n"); //NOI18N
            initialize.append(INDENT);
            initialize.append("}\n"); //NOI18N
        }
    }

    private void construct() {
        constructHeader();
        constructPackageLine();
        constructClassLine();
        String documentName = DocumentWindowController.makeTitle(document);

        // All that depends on fx:id
        Map<String, FXOMObject> fxids = document.collectFxIds();

        for (FXOMObject value : fxids.values()) {
            String key = value.getFxId();
            final Object obj = value.getSceneGraphObject();
            final Class<?> type = obj.getClass();

            addImportsFor(imports, FXML.class, type);
            variables.append(INDENT).append("@FXML"); //NOI18N

            if (textType == TEXT_TYPE.WITH_COMMENTS) {
                variables.append(" // fx:id=\"").append(key).append("\""); //NOI18N
            }

            variables.append("\n"); //NOI18N
            variables.append(INDENT).append("private ").append(type.getSimpleName()); //NOI18N
            final TypeVariable<? extends Class<?>>[] parameters = type.getTypeParameters();

            if (parameters.length > 0) {
                variables.append("<"); //NOI18N
                String sep = ""; //NOI18N
                for (TypeVariable<?> t : parameters) {
                    variables.append(sep).append("?"); //NOI18N
                    sep = ", "; //NOI18N
                    t.getName(); // silly call to silence FindBugs
                }
                variables.append(">"); //NOI18N
            }

            if (textType == TEXT_TYPE.WITH_COMMENTS) {
                variables.append(" ").append(key).append("; // Value injected by FXMLLoader\n\n"); //NOI18N
            } else {
                variables.append(" ").append(key).append(";\n\n"); //NOI18N
            }

            asserts.append(INDENT).append(INDENT).append("assert ").append(key).append(" != null : ") //NOI18N
                    .append("\"fx:id=\\\"").append(key).append("\\\" was not injected: check your FXML file ") //NOI18N
                    .append("'").append(documentName) //NOI18N
                    .append("'.\";\n"); //NOI18N
        }

        if (textFormat == FORMAT_TYPE.FULL) {
            addImportsFor(imports, URL.class, ResourceBundle.class);
        }
        
        // Event handlers
        for (FXOMPropertyT property : document.getFxomRoot().collectEventHandlers()) {
            handlers.append(INDENT).append("@FXML\n").append(INDENT).append("void "); //NOI18N
            final String methodName = property.getValue().replace("#", ""); //NOI18N
            handlers.append(methodName);
            handlers.append("(ActionEvent event) {\n\n").append(INDENT).append("}\n\n"); //NOI18N
        }

        // This method must be called once asserts has been populated.
        constructInitialize();
    }

    private void addImportsFor(Set<String> imports, Class<?>... classes) {
        for (Class<?> c : classes) {
            final StringBuilder importb = new StringBuilder();
            importb.append("import ").append(c.getName().replace("$", ".")).append(";\n"); //NOI18N
            imports.add(importb.toString());
        }
    }

    @Override
    public String toString() {
        construct();

        StringBuilder code = new StringBuilder();
        code.append(header);
        code.append(packageLine);

        for (String importStatement : imports) {
            code.append(importStatement);
        }

        code.append(classLine);

        if (textType == TEXT_TYPE.WITH_COMMENTS && textFormat == FORMAT_TYPE.FULL) {
            code.append(INDENT).append("@FXML // ResourceBundle that was given to the FXMLLoader\n") //NOI18N
                    .append(INDENT).append("private ResourceBundle resources;\n\n") //NOI18N
                    .append(INDENT).append("@FXML // URL location of the FXML file that was given to the FXMLLoader\n") //NOI18N
                    .append(INDENT).append("private URL location;\n\n"); //NOI18N
        } else if (textFormat == FORMAT_TYPE.FULL) {
            code.append(INDENT).append("@FXML\n") //NOI18N
                    .append(INDENT).append("private ResourceBundle resources;\n\n") //NOI18N
                    .append(INDENT).append("@FXML\n") //NOI18N
                    .append(INDENT).append("private URL location;\n\n"); //NOI18N
        }

        code.append(variables);
        code.append(handlers);
        code.append(initialize);
        code.append("}\n"); //NOI18N

        return code.toString();
    }
}
