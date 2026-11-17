/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MacOSSystemMenuImageGraphicTest extends Application {

    public static final int COUNTER = 10000;

    public static void main(String[] args) {
        System.out.println("MacOS only: Open the 'Menu1' and 'Menu2' menus in the system menu bar.");
        System.out.println("Check that every menu item icon renders correctly (not missing/pixelated/garbled)" +
                "and there is no crash.");
        launch(args);
    }

    private static final int size = 16;
    private static final List<String> menuColors = List.of(
            "#E53935", "#8E24AA", "#3949AB", "#1E88E5",
            "#00897B", "#43A047", "#FDD835", "#FB8C00", "#6D4C41");

    @Override
    public void start(Stage stage) {
        VBox menus = new VBox(10);
        menus.setPadding(new Insets(20));

        Menu menu1 = new Menu("Menu1");
        menus.getChildren().add(new Label("Expected Menu 1:"));
        for (int i = 0; i < 4; i++) {
            generateItems(menus, menu1, i);
        }
        Menu menu2 = new Menu("Menu2");
        menus.getChildren().add(new Label("Expected Menu 2:"));
        for (int i = 4; i < 9; i++) {
            generateItems(menus, menu2, i);
        }
        MenuBar menuBar = new MenuBar(menu1, menu2);
        menuBar.setUseSystemMenuBar(true);

        BorderPane root = new BorderPane(menus);
        root.setTop(menuBar);
        stage.setScene(new Scene(root, 600, 400));
        stage.show();

        startBackgroundWork();

    }

    private void generateItems(VBox menus, Menu menu, int i) {
        Color color = Color.web(menuColors.get(i));
        MenuItem menuItem = new MenuItem("Item " + (i + 1), generateIcon(color));
        menu.getItems().add(menuItem);

        HBox expectedItem = new HBox(10);
        expectedItem.setPadding(new Insets(0, 0, 0, 20));
        expectedItem.getChildren().addAll(generateIcon(color), new Label("Item " + (i + 1)));
        menus.getChildren().add(expectedItem);
    }

    private static ImageView generateIcon(Color color) {
        Rectangle background = new Rectangle(size, size, color);
        background.setArcWidth(6);
        background.setArcHeight(6);

        Circle dot = new Circle(size / 2.0, size / 2.0, size / 4.0,
                color.getBrightness() < 0.6 ? Color.WHITE : Color.BLACK);

        Group group = new Group(background, dot);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        return new ImageView(group.snapshot(params, new WritableImage(size, size)));
    }

    private static void startBackgroundWork() {
        final int iconBytes = size * size * 4;

        Thread worker = new Thread(() -> {
            final List<ByteBuffer> directBuffers = new ArrayList<>();
            final List<byte[]> heapArrays = new ArrayList<>();

            while (!Thread.currentThread().isInterrupted()) {
                // Allocate direct buffers the size of an icon pixel buffer
                directBuffers.clear();
                try {
                    for (int i = 0; i < COUNTER; i++) {
                        ByteBuffer buf = ByteBuffer.allocateDirect(iconBytes);
                        for (int b = 0; b < iconBytes; b++) {
                            buf.put(b, (byte) 0xFF);
                        }
                        directBuffers.add(buf);
                    }
                } catch (OutOfMemoryError oom) {
                    // ignore
                }
                directBuffers.clear();

                // Allocate arrays until the heap forces a collection, making the icon's direct buffer unreachable
                heapArrays.clear();
                try {
                    for (int i = 0; i < COUNTER; i++) {
                        heapArrays.add(new byte[64 * 1024]);
                    }
                } catch (OutOfMemoryError oom) {
                    // ignore
                }
                heapArrays.clear();

                // 3) Force reclamation to free the native pixel memory
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "background-work");
        worker.setDaemon(true);
        worker.start();
    }

}
