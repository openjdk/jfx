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
package com.oracle.javafx.scenebuilder.app.report;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import com.oracle.javafx.scenebuilder.kit.library.user.UserLibrary;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReport;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReportEntry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.WindowEvent;

/**
 *
 */
public class JarAnalysisReportController extends AbstractFxmlWindowController {
    
    @FXML
    TextFlow textFlow;
    @FXML
    Label timestampLabel;

    @FXML
    void onCopyAction(ActionEvent event) {
        final Map<DataFormat, Object> content = new HashMap<>();
        StringBuilder sb = new StringBuilder();

        for (Node item : textFlow.getChildrenUnmodifiable()) {
            if (item instanceof Text) {
                sb.append(((Text)item).getText());
            }
        }
        
        content.put(DataFormat.PLAIN_TEXT, sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private final EditorController editorController;
    private final String TIMESTAMP_PATTERN = "h:mm a EEEEEEEEE d MMM. yyyy"; //NOI18N
    private final SimpleDateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat(TIMESTAMP_PATTERN);
    private int prefixCounter = 0;
    private boolean dirty = false;

    public JarAnalysisReportController(EditorController editorController, Window owner) {
        super(JarAnalysisReportController.class.getResource("JarAnalysisReport.fxml"), I18N.getBundle(), owner); //NOI18N
        this.editorController = editorController;
    }

    @Override
    public void onCloseRequest(WindowEvent event) {
        getStage().close();
    }
    
    @Override
    public void openWindow() {
        super.openWindow();
        
        if (dirty) {
            update();
        }
    }
    
    @Override
    protected void controllerDidCreateStage() {
        // Setup window title
        getStage().setTitle(I18N.getString("jar.analysis.report.title"));
    }

    @Override
    protected void controllerDidLoadFxml() {
        assert textFlow != null;
        assert timestampLabel != null;
                
        UserLibrary lib = (UserLibrary)editorController.getLibrary();
        lib.getJarReports().addListener(new ListChangeListener<JarReport>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends JarReport> change) {
                update();
            }
        });
        
        update();
    }
    
    private void update() {
        // No need to eat CPU if the skeleton window isn't opened
        if (getStage().isShowing()) {
            textFlow.getChildren().clear();
            
            updateTimeStampLabel();
            
            UserLibrary lib = (UserLibrary)editorController.getLibrary();
            
            for (JarReport report : lib.getJarReports()) {
                for (JarReportEntry entry : report.getEntries()) {
                    if (entry.getStatus() != JarReportEntry.Status.OK) {
                        if (entry.getKlass() != null && entry.getException() != null) {
                            // We use a Text instance for header and another one
                            // for full stack in order to style them separately
                            StringBuilder sb = new StringBuilder();
                            sb.append(getSectionPrefix()).append(I18N.getString("jar.analysis.exception"));
                            sb.append(" ").append(entry.getName()); //NOI18N
                            Text text = new Text();
                            text.setText(sb.toString());
                            text.getStyleClass().add("header"); //NOI18N
                            textFlow.getChildren().add(text);
                            
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(getFullStack(entry.getException()));
                            Text text2 = new Text();
                            text2.setText(sb2.toString());
                            text2.getStyleClass().add("body"); //NOI18N
                            textFlow.getChildren().add(text2);
                        }
                    } else if (! entry.isNode()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(getSectionPrefix()).append(I18N.getString("jar.analysis.not.node"));
                        sb.append(" ").append(entry.getName()); //NOI18N
                        Text text = new Text();
                        text.setText(sb.toString());
                        text.getStyleClass().add("header"); //NOI18N
                        textFlow.getChildren().add(text);
                    }
                }
            }

            dirty = false;
        } else {
            dirty = true;
        }
    }
    
    // The very first section must start on top, it is only for the next one we
    // need a separator.
    private String getSectionPrefix() {
        if (prefixCounter == 0) {
            prefixCounter++;
            return ""; //NOI18N
        } else {
            return "\n\n"; //NOI18N
        }
    }
    
    private StringBuilder getFullStack(Throwable t) {
        StringBuilder res = new StringBuilder("\n"); //NOI18N
        StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer, true));
            res.append(writer.getBuffer().toString());
        return res;
    }
    
    private void updateTimeStampLabel() {
        UserLibrary lib = (UserLibrary)editorController.getLibrary();
        Date date = (Date)lib.getExplorationDate();
        String timestampValue = TIMESTAMP_DATE_FORMAT.format(date);
        timestampLabel.setText(I18N.getString("jar.analysis.report.timestamp", timestampValue));
    }
}
