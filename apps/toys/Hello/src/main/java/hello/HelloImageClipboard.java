/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloImageClipboard extends Application {

    private Button  clearBtn, copyBtn, pasteBtn;
    final ImageView imageView = new ImageView();

    @Override public void start(Stage stage) {
        //stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Hello Image Clipboard");
        Scene scene = new Scene(new Group(), 1024, 768);
        scene.setFill(Color.LIGHTGREEN);

        Rectangle overlay = new Rectangle();
        overlay.setWidth(800);
        overlay.setHeight(600);
        overlay.setFill(Color.TRANSPARENT);
        EventHandler<DragEvent> drop =  new EventHandler<DragEvent>() {
                public void handle(DragEvent de) {
                    checkBoard(de.getDragboard(), de);
                }
            };
            
        EventHandler<DragEvent> enter =  new EventHandler<DragEvent>() {
                public void handle(DragEvent de) {
                    if (de != null && de.getDragboard() != null && de.getDragboard().hasImage()) {
                        de.acceptTransferModes(TransferMode.ANY);
                    }
                }
            };

        EventHandler<DragEvent> dragged =  new EventHandler<DragEvent>() {
                public void handle(DragEvent de) {
                    if (de != null && de.getDragboard() != null && de.getDragboard().hasImage()) {
                        de.acceptTransferModes(TransferMode.ANY);
                    }
                }
            };

        overlay.setOnDragDropped(drop);
        overlay.setOnDragEntered(enter);
        overlay.setOnDragOver(dragged);

        clearBtn = new Button("Clear");
        clearBtn.setTranslateX(50);
        clearBtn.setTranslateY(30);

        copyBtn = new Button("Copy");
        copyBtn.setTranslateX(125);
        copyBtn.setTranslateY(30);
        
        pasteBtn = new Button("Paste");
        pasteBtn.setTranslateX(200);
        pasteBtn.setTranslateY(30);

        clearBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) { clear(); }
        });
        copyBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                ClipboardContent content = new ClipboardContent();
                content.putImage(imageView.getImage());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        pasteBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) { checkBoard(Clipboard.getSystemClipboard(), null); }
        });
        
        Group root = (Group)scene.getRoot();
        root.getChildren().add(overlay);
        root.getChildren().add(imageView);
        root.getChildren().add(clearBtn);
        root.getChildren().add(copyBtn);
        root.getChildren().add(pasteBtn);
        
        stage.setScene(scene);
        stage.show();
    }

    private void clear() {
    }
    /*
     * Called either when the user clicks the 'paste' button, or when they drop
     * files onto the scene overlay. Supports animating multiple files, or just
     * accepting one file.
     */
    private void checkBoard(Clipboard board, DragEvent de) {
        // clean up from any previous runs
        clear();

        if (board == null) {
            System.out.println("HelloImageClipboard: sorry - null Clipboard");
        }

        if (board.hasImage()) {
            if (de != null) de.acceptTransferModes(TransferMode.ANY);
            imageView.setImage(board.getImage());
            if (de != null) de.setDropCompleted(true);
            System.out.println("HelloImageClipboard: single image");
        } else {
            if (de != null) de.setDropCompleted(false);
            System.out.println("HelloImageClipboard: sorry - no images on the Clipboard");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
