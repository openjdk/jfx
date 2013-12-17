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
import com.oracle.javafx.scenebuilder.app.skeleton.SkeletonBuffer.FORMAT_TYPE;
import com.oracle.javafx.scenebuilder.app.skeleton.SkeletonBuffer.TEXT_TYPE;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 */
public class SkeletonWindowController extends AbstractFxmlWindowController {

    @FXML
    Button copyButton;
    @FXML
    CheckBox commentCheckBox;
    @FXML
    CheckBox formatCheckBox;
    @FXML
    TextArea textArea;

    @FXML
    void onCopyAction(ActionEvent event) {
        final Map<DataFormat, Object> content = new HashMap<>();

        if (textArea.getSelection().getLength() == 0) {
            content.put(DataFormat.PLAIN_TEXT, textArea.getText());
        } else {
            content.put(DataFormat.PLAIN_TEXT, textArea.getSelectedText());
        }

        Clipboard.getSystemClipboard().setContent(content);
    }

    private final EditorController editorController;

    public SkeletonWindowController(EditorController editorController, Window owner) {
        super(SkeletonWindowController.class.getResource("SkeletonWindow.fxml"), I18N.getBundle(), owner); //NOI18N
        this.editorController = editorController;

        this.editorController.fxomDocumentProperty().addListener(
                new ChangeListener<FXOMDocument>() {
                    @Override
                    public void changed(ObservableValue<? extends FXOMDocument> ov,
                            FXOMDocument od, FXOMDocument nd) {
                        assert editorController.getFxomDocument() == nd;
                        if (od != null) {
                            od.sceneGraphRevisionProperty().removeListener(fxomDocumentRevisionListener);
                        }
                        if (nd != null) {
                            nd.sceneGraphRevisionProperty().addListener(fxomDocumentRevisionListener);
                            update();
                        }
                    }
                });

        if (editorController.getFxomDocument() != null) {
            editorController.getFxomDocument().sceneGraphRevisionProperty().addListener(fxomDocumentRevisionListener);
        }
    }

    @Override
    public void onCloseRequest(WindowEvent event) {
        getStage().close();
    }

    /*
     * AbstractFxmlWindowController
     */
    @Override
    protected void controllerDidLoadFxml() {
        super.controllerDidLoadFxml();
        assert copyButton != null;
        assert commentCheckBox != null;
        assert formatCheckBox != null;
        assert textArea != null;

        commentCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                update();
            }
        });

        formatCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                update();
            }
        });

        update();
    }

    /*
     * Private
     */
    private final ChangeListener<Number> fxomDocumentRevisionListener
            = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    update();
                }
            };

    private void updateTitle() {
        String documentName = DocumentWindowController.makeTitle(editorController.getFxomDocument());
        final String title = I18N.getString("skeleton.window.title", documentName);
        getStage().setTitle(title);
    }

    private void update() {
        updateTitle();
        final SkeletonBuffer buf = new SkeletonBuffer(editorController.getFxomDocument());

        if (commentCheckBox.isSelected()) {
            buf.setTextType(TEXT_TYPE.WITH_COMMENTS);
        } else {
            buf.setTextType(TEXT_TYPE.WITHOUT_COMMENTS);
        }

        if (formatCheckBox.isSelected()) {
            buf.setFormat(FORMAT_TYPE.FULL);
        } else {
            buf.setFormat(FORMAT_TYPE.COMPACT);
        }
        
        textArea.setText(buf.toString());
    }

//    private String generateSkeleton(TEXT_TYPE textType) {
//        final String INDENT = "    "; //NOI18N
//        Map<String, FXOMObject> fxids = editorController.getFxomDocument().collectFxIds();
//        Set<String> imports = new TreeSet<>();
//        StringBuilder variables = new StringBuilder();
//        StringBuilder asserts = new StringBuilder();
//        
//        for (String key : fxids.keySet()) {
//            final FXOMObject value = fxids.get(key);
////            if (value.isEmpty()) {
////                continue;
////            }
////            if (value.size() > 1) {
////                variables.append(INDENT).append("// WARNING: fx:id=\"").append(key) //NOI18N
////                        .append("\" cannot be injected: several objects share the same fx:id;\n\n"); //NOI18N
////            } else if (Utils.isValidFxmlId(key)) {
//                final Object obj = value.getSceneGraphObject();
//                final Class<?> type = obj.getClass();
//
////                if (ComponentDictionary.lookupDefinition(type, false) == null && !type.equals(ToggleGroup.class)) {
////                    Utils.println("Class '" + type.getSimpleName() + "' has no definition: skipping fx:id=\"" + key + "\"");
////                    continue;
////                }
//
//                addImportsFor(imports, FXML.class, type);
//                variables.append(INDENT).append("@FXML"); //NOI18N
//                
//                if (textType == TEXT_TYPE.WITH_COMMENTS) {
//                    variables.append(" // fx:id=\"").append(key).append("\""); //NOI18N
//                }
//                
//                variables.append("\n"); //NOI18N
//                variables.append(INDENT).append("private ").append(type.getSimpleName()); //NOI18N
//                final TypeVariable<? extends Class<?>>[] parameters = type.getTypeParameters();
//                
//                if (parameters.length > 0) {
//                    variables.append("<"); //NOI18N
//                    String sep = ""; //NOI18N
//                    for (TypeVariable<?> t : parameters) {
//                        variables.append(sep).append("?"); //NOI18N
//                        sep = ", "; //NOI18N
//                    }
//                    variables.append(">"); //NOI18N
//                }
//                
//                if (textType == TEXT_TYPE.WITH_COMMENTS) {
//                    variables.append(" ").append(key).append("; // Value injected by FXMLLoader\n\n"); //NOI18N
//                } else {
//                    variables.append(" ").append(key).append(";\n\n"); //NOI18N
//                }
//                
//                asserts.append(INDENT).append(INDENT).append("assert ").append(key).append(" != null : ") //NOI18N
//                        .append("\"fx:id=\\\"").append(key).append("\\\" was not injected: check your FXML file ") //NOI18N
////                        .append("'").append(project.isUntitledProject() ? project.getProjectName() : project.getProjectFxmlFile().getName()) //NOI18N
//                        .append("'.\";\n"); //NOI18N
////            } else {
////                variables.append(INDENT).append("// WARNING: fx:id=\"").append(key) //NOI18N
////                        .append("\" cannot be injected: it is not a valid Java Identifier;\n\n"); //NOI18N
////            }
//        }
//
//        addImportsFor(imports, URL.class, ResourceBundle.class);
//        
//        // GLOP
//        StringBuilder glop = new StringBuilder();
//        for (String ouaf : imports) {
//            glop.append(ouaf);
//        }
//        return glop.toString();
//
////        final StringBuilder handlerCode = new StringBuilder();
////        final Map<String, Set<TargetPropertyValue>> handlers = Collections.unmodifiableMap(context.handlers);
////        final TreeSet<String> handlerNames = new TreeSet<>(handlers.keySet());
////        for (String name : handlerNames) {
////            if (!name.startsWith("#") || name.startsWith("##")) {
////                continue; //NOI18N
////            }
////            final String methodName = name.substring(1);
////            if (!Utils.isValidFxmlId(methodName)) {
////                handlerCode.append(INDENT)
////                        .append("// WARNING: cannot create handler for '") //NOI18N
////                        .append(name).append("': not a valid method name\n\n"); //NOI18N
////            } else {
////                TreeSet<String> comments = new TreeSet<>();
////                Class<?> eventClass = null;
////                for (TargetPropertyValue tpv : handlers.get(name)) {
////                    comments.add(describeHandler(tpv));
////                    if (!tpv.getProperty().isProp()) {
////                        eventClass = Event.class;
////                    } else {
////                        eventClass = commonEventClass(eventClass, tpv.getProperty().getProp().getType());
////                    }
////                }
////                if (textType == TEXT_TYPE.WITH_COMMENTS) {
////                    for (String s : comments) {
////                        handlerCode.append(INDENT).append(s);
////                    }
////                }
////                handlerCode.append(INDENT).append("@FXML\n"); //NOI18N
////                handlerCode.append(INDENT).append("void ").append(methodName).append("(") //NOI18N
////                        .append(eventClass.getSimpleName()).append(" event) {\n"); //NOI18N
////                if (textType == TEXT_TYPE.WITH_COMMENTS) {
////                    handlerCode.append(INDENT).append(INDENT).append("// handle the event here\n"); //NOI18N
////                }
////                handlerCode.append(INDENT).append("}\n\n"); //NOI18N
////                addImportsFor(imports, eventClass);
////            }
////        }
////
////        final StringBuilder controller = new StringBuilder();
////        String controllerName = project.getScreenData().getControllerClass();
////        if (controllerName == null || controllerName.isEmpty()) {
////            controllerName = "PleaseProvideControllerClassName"; //NOI18N
////        }
////        String simpleName = controllerName.replace("$", "."); //NOI18N
////        int dot = simpleName.lastIndexOf('.');
////        if (dot > -1) {
////            simpleName = simpleName.substring(dot + 1);
////        }
////        if (textType == TEXT_TYPE.WITH_COMMENTS) {
////            controller.append("/**\n").append(" * ") //NOI18N
////                    .append(format("menu.view.sample.controller.skeleton.header.line1", project.getProjectName()))
////                    .append("\n").append(" * ") //NOI18N
////                    .append(format("menu.view.sample.controller.skeleton.header.line2"))
////                    .append("\n").append(" **/\n\n"); //NOI18N
////        }
////        if (!controllerName.contains("$")) { //NOI18N
////            int lastdot = controllerName.lastIndexOf('.');
////            if (lastdot > 0) {
////                controller.append("package ").append(controllerName.substring(0, lastdot)).append(";\n\n"); //NOI18N
////            }
////        }
////        for (String imp : imports) {
////            controller.append(imp);
////        }
////        controller.append("\n\n"); //NOI18N
////        controller.append("public "); //NOI18N
////        if (controllerName.contains("$")) { //NOI18N
////            controller.append("static "); //NOI18N
////        }
////        controller.append("class ").append(simpleName).append(" {\n\n"); //NOI18N
////        if (textType == TEXT_TYPE.WITH_COMMENTS) {
////            controller.append(INDENT).append("@FXML // ResourceBundle that was given to the FXMLLoader\n") //NOI18N
////                    .append(INDENT).append("private ResourceBundle resources;\n\n") //NOI18N
////                    .append(INDENT).append("@FXML // URL location of the FXML file that was given to the FXMLLoader\n") //NOI18N
////                    .append(INDENT).append("private URL location;\n\n"); //NOI18N
////        } else {
////            controller.append(INDENT).append("@FXML\n") //NOI18N
////                    .append(INDENT).append("private ResourceBundle resources;\n\n") //NOI18N
////                    .append(INDENT).append("@FXML\n") //NOI18N
////                    .append(INDENT).append("private URL location;\n\n"); //NOI18N
////        }
////        controller.append(variables);
////        controller.append("\n"); //NOI18N
////        controller.append(handlerCode.toString());
////        if (textType == TEXT_TYPE.WITH_COMMENTS) {
////            controller.append(INDENT).append("@FXML // This method is called by the FXMLLoader when initialization is complete\n"); //NOI18N
////        } else {
////            controller.append(INDENT).append("@FXML\n"); //NOI18N
////        }
////        controller.append(INDENT).append("void initialize() {\n") //NOI18N
////                .append(asserts.toString()).append("\n"); //NOI18N
////        if (textType == TEXT_TYPE.WITH_COMMENTS) {
////            controller.append(INDENT).append(INDENT).append("// Initialize your logic here: all @FXML variables will have been injected\n"); //NOI18N
////        }
////        controller.append("\n") //NOI18N
////                .append(INDENT).append("}\n\n"); //NOI18N
////        controller.append("}\n"); //NOI18N
////        return controller.toString();
//    }
}
