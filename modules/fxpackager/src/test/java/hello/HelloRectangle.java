/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package hello;

import javafx.application.Application;
import javafx.application.Preloader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.Optional;

/**
 */
public class HelloRectangle extends Application {

    private static class MessagePreloaderNotification implements Preloader.PreloaderNotification {
        String message;

        private MessagePreloaderNotification(String message) {
            this.message = message;
        }
        
        public String toString() {
            return message;
        }
    }
    
    @Override
    public void init() throws Exception {
        if (System.getProperty("javafx.preloader") != null) {
            notifyPreloader(new MessagePreloaderNotification("5..."));
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("4..."));
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("3..."));
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("2..."));
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("1..."));
            Thread.sleep(1000);
            notifyPreloader(new MessagePreloaderNotification("GO!"));
        }
    }

    @Override public void start(Stage stage) {
        stage.setTitle(Optional.ofNullable(System.getProperty("app.preferences.id")).orElse("Hello Rectangle").replace("/", " " ));

        Group root = new Group();
        Scene scene = new Scene(root, 600, 450);

        Rectangle rect = new Rectangle();
        rect.setX(25);
        rect.setY(40);
        rect.setWidth(300);
        rect.setHeight(300);
        rect.setFill(Color.RED);

        root.getChildren().addAll(rect);

        Tooltip.install(rect, new Tooltip(
                "Arguments:\n" +
                String.join("\n", getParameters().getUnnamed())));


        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
