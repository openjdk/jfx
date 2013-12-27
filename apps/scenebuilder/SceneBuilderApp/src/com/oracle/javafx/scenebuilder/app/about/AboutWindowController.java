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
package com.oracle.javafx.scenebuilder.app.about;

import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.GraphicsPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;

/**
 *
 */
public final class AboutWindowController extends AbstractFxmlWindowController {

    @FXML
    private VBox vbox;
    @FXML
    private TextArea textArea;
    
    private String sbBuildInfo = "PLACEHOLDER"; //NOI18N
    private String sbBuildDate = "PLACEHOLDER"; //NOI18N
    private String sbBuildJavaVersion = "PLACEHOLDER"; //NOI18N
    // The resource bundle contains two keys: about.copyright and about.copyright.open
    private String sbAboutCopyrightKeyName = "about.copyright.open"; //NOI18N

    public AboutWindowController() {
        super(AboutWindowController.class.getResource("About.fxml"), //NOI18N
                I18N.getBundle());
        try (InputStream in = getClass().getResourceAsStream("about.properties")) { //NOI18N

            if (in != null) {
                Properties sbProps = new Properties();
                sbProps.load(in);
                sbBuildInfo = sbProps.getProperty("build.info", "UNSET"); //NOI18N
                sbBuildDate = sbProps.getProperty("build.date", "UNSET"); //NOI18N
                sbBuildJavaVersion = sbProps.getProperty("build.java.version", "UNSET"); //NOI18N
                sbAboutCopyrightKeyName = sbProps.getProperty("copyright.key.name", "UNSET"); //NOI18N
            }
        } catch (IOException ex) {
            // We go with default values
        }
    }
    
    @FXML
    public void onMousePressed(MouseEvent event) {
        if ((event.getClickCount() == 2) && event.isAltDown()) {
            SceneBuilderApp.getSingleton().toggleDebugMenu();
        }
    }

    @Override
    public void onCloseRequest(WindowEvent event) {
        closeWindow();
    }

    /*
     * AbstractWindowController
     */
    @Override
    protected void controllerDidCreateStage() {
        assert getRoot() != null;
        assert getRoot().getScene() != null;
        assert getRoot().getScene().getWindow() != null;

        getStage().setTitle(I18N.getString("about.title"));
        getStage().initModality(Modality.APPLICATION_MODAL);
    }

    @Override
    protected void controllerDidLoadFxml() {
        super.controllerDidLoadFxml();
        assert vbox != null;
        assert textArea != null;
        textArea.setText(getAboutText());
    }

    private String getAboutText() {
        boolean hwAccelerated = false;

        String tk = Toolkit.getToolkit().getClass().getSimpleName();
        String fxtra = I18N.getString("about.fx.toolkit")
                + " = " + tk + "\n"; //NOI18N
        if ("GlassToolkit".equals(tk) || "PrismToolkit".equals(tk) //NOI18N
                || "QuantumToolkit".equals(tk)) { //NOI18N
            String ppl = GraphicsPipeline.getPipeline().getClass().getSimpleName();
            fxtra += I18N.getString("about.fx.pipeline")
                    + " = " + ppl + "\n"; //NOI18N
            if (ppl.trim().equals("D3DPipeline") //NOI18N
                    || ppl.trim().equals("ES1Pipeline") //NOI18N
                    || ppl.trim().equals("ES2Pipeline")) { //NOI18N
                hwAccelerated = true;
            }
        }
        fxtra += I18N.getString("about.fx.hardware.acceleration")
                + " " //NOI18N
                + (hwAccelerated ? I18N.getString("about.fx.hardware.acceleration.enabled")
                : I18N.getString("about.fx.hardware.acceleration.disabled"))
                + "\n\n"; //NOI18N

        String text = I18N.getString("about.product.version")
                + "\nJavaFX Scene Builder 2.0 (Developer Preview)\n\n" //NOI18N
                + I18N.getString("about.build.information")
                + "\n" + sbBuildInfo + "\n" //NOI18N
                + MessageFormat.format(I18N.getString("about.build.date"), sbBuildDate)
                + "\n\nJavaFX\n"; //NOI18N

        text += fxtra
                + "Java\n" //NOI18N
                + System.getProperty("java.runtime.version") //NOI18N
                + ", " //NOI18N
                + System.getProperty("java.vendor") //NOI18N
                + "\n\n" //NOI18N
                + I18N.getString("about.operating.system")
                + "\n" //NOI18N
                + System.getProperty("os.name") //NOI18N
                + ", " //NOI18N
                + System.getProperty("os.arch") //NOI18N
                + ", " //NOI18N
                + System.getProperty("os.version") //NOI18N
                + "\n\n"; //NOI18N

        text += I18N.getString(sbAboutCopyrightKeyName);
        return text;
    }
    
    /**
     *
     * @treatAsPrivate
     */
    public String getBuildJavaVersion() {
        return sbBuildJavaVersion;
    }
    
    /**
     *
     * @treatAsPrivate
     */
    public String getBuildInfo() {
        return sbBuildInfo;
    }
}
