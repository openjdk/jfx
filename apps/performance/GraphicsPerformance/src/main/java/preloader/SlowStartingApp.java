/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
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

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * This app is meant to be used with a preloader. Run with:
 * -Djavafx.preloader=preloader.PreloaderApp
 *
 * This will run with the preloader on desktop and iOS.
 */
public class SlowStartingApp extends Application {
    private ListView<Background> list;
    public Stage preloaderApp;

    @Override
    public void init() throws Exception {
        System.out.println("App init: " + Thread.currentThread());
        ObservableList<Background> data = FXCollections.observableArrayList();
        for (int i=0; i<100; i++) {
            data.add(new Background(new BackgroundFill(
                    new Color(Math.random(), Math.random(), Math.random(), 1), null, null)));
        }
        list = new ListView<>(data);
        list.setCellFactory(new Callback<ListView<Background>, ListCell<Background>>() {
            @Override
            public ListCell<Background> call(ListView<Background> param) {
                ListCell<Background> cell = new ListCell<Background>() {
                    @Override
                    protected void updateItem(Background item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            setBackground(item);
                        }
                    }
                };
                cell.setPrefHeight(100);
                return cell;
            }
        });
        System.out.println("Simulating a really slow startup");
        Thread.sleep(10000);
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("App start: " + Thread.currentThread());
        Scene scene = new Scene(list);
        stage.setScene(scene);
        stage.show();
        preloaderApp.close();
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
