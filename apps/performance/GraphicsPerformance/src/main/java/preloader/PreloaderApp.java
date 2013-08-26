/*
 * Copyright (c) 2013 Oracle and/or its affiliates.
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

package preloader;

import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 *
 */
public class PreloaderApp extends Preloader {
    private ProgressBar progressBar;
    private Label label;
    private Stage stage;
    private VBox root;

    @Override
    public void init() throws Exception {
        System.out.println("Preloader init: " + Thread.currentThread());
        progressBar = new ProgressBar();
        label = new Label();

        root = new VBox(progressBar, label);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(7);
        root.setBackground(null);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(root, Color.BLACK);
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
        System.out.println("Preloader start: " + Thread.currentThread());
    }

    /**
     * Indicates progress.
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * does nothing.
     * </p>
     *
     * @param info the progress notification
     */
    public void handleProgressNotification(ProgressNotification info) {
//        progressBar.setProgress(info.getProgress());
    }

    /**
     * Indicates a change in application state.
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * does nothing.
     * </p>
     *
     * @param info the state change notification
     */
    public void handleStateChangeNotification(StateChangeNotification info) {
        switch(info.getType()) {
            case BEFORE_INIT:
                break;
            case BEFORE_LOAD:
                break;
            case BEFORE_START:
                ((SlowStartingApp)info.getApplication()).preloaderApp = stage;
                break;
        }
    }

    /**
     * Indicates an application-generated notification.
     * Application should not call this method directly, but should use
     * notifyCurrentPreloader() instead to avoid mixed code dialog issues.
     *
     * <p>
     * The implementation of this method provided by the Preloader class
     * does nothing.
     * </p>
     *
     * @param info the application-generated notification
     */
    public void handleApplicationNotification(PreloaderNotification info) {
        label.setText(info.toString());
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
