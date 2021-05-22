/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
package dragdrop;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {

    private static final int WIDTH = 380;
    private static final int HEIGHT = 500;

    VBox launcher = new VBox(15);
    Info info = new Info();


    //Use to register EventLogger
    private static final boolean DEBUG = false;

    @Override public void start(Stage stage) {
        stage.setTitle("Touch Suite");

        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.BEIGE);

        root.getChildren().add(launcher);
        root.getChildren().add(info);

        register("Color", DragDropColor.class, DragDropColor.info());
        register("Text", DragDropText.class, DragDropText.info());
        register("Controls", DragDropWithControls.class, DragDropWithControls.info());

        stage.setScene(scene);
        stage.show();
    }

    private Node createLauncher(final String name, final Class app) {
        Text t = new Text(name);
        t.setFont(new Font(25));
        t.setFill(Color.DARKBLUE);
        t.setUnderline(true);
        t.setTranslateX(40);
        t.setTextOrigin(VPos.TOP);

        t.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                Stage stage = new Stage();
                try {
                    ((Application) app.getDeclaredConstructor().newInstance()).start(stage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return t;
    }

    private void register(final String name, final Class app, final String description) {
        HBox g = new HBox();

        Circle bullet = new Circle(5);
        bullet.setFill(Color.BLACK);
        bullet.setTranslateX(20);
        bullet.setTranslateY(15);

        Text b1 = new Text("(");
        b1.setTranslateX(50);
        b1.setFont(new Font(25));

        Text b2 = new Text(")");
        b2.setTranslateX(50);
        b2.setFont(new Font(25));

        Text i = new Text("info");
        i.setTranslateX(50);
        i.setFont(new Font(25));
        i.setUnderline(true);
        i.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                info.show(name, app, description);
            }
        });

        g.getChildren().add(bullet);
        g.getChildren().add(createLauncher(name, app));
        g.getChildren().addAll(b1, i, b2);

        launcher.getChildren().add(g);
    }

    private class Info extends Group {
        Rectangle bg;
        Text n, t, run, close;

        public Info() {
            VBox vb = new VBox(10);

            bg = new Rectangle(WIDTH, HEIGHT);
            bg.setFill(Color.LIGHTBLUE);

            n = new Text();
            n.setFont(new Font(25));
            n.setTranslateX(10);
            n.setTextOrigin(VPos.TOP);

            t = new Text();
            t.setFont(new Font(15));
            t.setTextOrigin(VPos.TOP);
            t.setTranslateX(10);
            t.setWrappingWidth(WIDTH - 20);

            run = new Text("Start application");
            run.setFont(new Font(25));
            run.setTranslateX(10);
            run.setTextOrigin(VPos.TOP);

            close = new Text("Close info");
            close.setFont(new Font(25));
            close.setTranslateX(10);
            close.setTextOrigin(VPos.TOP);

            setVisible(false);

            close.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    Info.this.setVisible(false);
                }
            });

            vb.getChildren().addAll(n, t, run, close);
            getChildren().addAll(bg, vb);
        }

        public void show(final String name, final Class app, final String info) {
            n.setText(name);
            t.setText(info);
            run.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    Info.this.setVisible(false);
                    Stage stage = new Stage();
                    try {
                        ((Application) app.newInstance()).start(stage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            setVisible(true);
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
