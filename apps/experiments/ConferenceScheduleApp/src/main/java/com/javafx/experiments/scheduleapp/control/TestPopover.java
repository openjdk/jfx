/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp.control;

import com.javafx.experiments.scheduleapp.ConferenceScheduleApp;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class TestPopover extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private Region createRegion(double width, double height, String fill) {
        Region region = new Region();
        region.setStyle("-fx-background-color: " + fill + ";");
        region.setPrefSize(width, height);
        return region;
    }

    @Override public void start(Stage primaryStage) throws Exception {
        final Popover popover = new Popover();
        LinkedPage page1 = new LinkedPage("First", createRegion(400, 200, "red"));
        LinkedPage page2 = new LinkedPage("Second", createRegion(400, 300, "green"));
        LinkedPage page3 = new LinkedPage("Third", createRegion(400, 400, "blue"));
        LinkedPage page4 = new LinkedPage("Fourth", createRegion(400, 500, "yellow"));
        LinkedPage page5 = new LinkedPage("Last", createRegion(400, 300, "white"));
        page1.next = page2;
        page2.prev = page1;
        page2.next = page3;
        page3.prev = page2;
        page3.next = page4;
        page4.prev = page3;
        page4.next = page5;
        page5.prev = page4;
        popover.pushPage(page1);
        final Button button = new Button("Click Me");
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                popover.show();
            }
        });
        Pane group = new Pane();
        group.getChildren().addAll(button, popover);

        Scene scene = new Scene(group, 640, 480);
        scene.getStylesheets().addAll(
                ConferenceScheduleApp.class.getResource("SchedulerStyleSheet.css").toExternalForm(),
                ConferenceScheduleApp.class.getResource("SchedulerStyleSheet-Local-Fonts.css").toExternalForm(),
                ConferenceScheduleApp.class.getResource("SchedulerStyleSheet-Desktop.css").toExternalForm()
        );
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private class LinkedPage implements Popover.Page {
        private Region region;
        private Popover.Page next;
        private Popover.Page prev;
        private Popover popover;
        private String title;

        LinkedPage(String title, Region region) {
            this.region = region;
            this.title = title;
        }

        @Override public void setPopover(Popover popover) { this.popover = popover; }
        @Override public Popover getPopover() { return popover; }
        @Override public Node getPageNode() { return region; }
        @Override public String getPageTitle() { return title; }
        @Override public String leftButtonText() { return prev == null ? null : prev.getPageTitle(); }
        @Override public String rightButtonText() { return next == null ? null : next.getPageTitle(); }
        @Override public void handleLeftButton() { popover.popPage(); }
        @Override public void handleRightButton() { popover.pushPage(next); }
        @Override public String toString() { return title; }
        @Override public void handleShown() { }
        @Override public void handleHidden() { }
    }
}
