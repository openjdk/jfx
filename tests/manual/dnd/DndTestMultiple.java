/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 import javafx.scene.Group;
 import javafx.scene.Node;
 import javafx.scene.Scene;
 import javafx.scene.control.Label;
 import javafx.scene.input.Dragboard;
 import javafx.scene.input.ClipboardContent;
 import javafx.scene.input.TransferMode;
 import javafx.scene.layout.VBox;
 import javafx.scene.paint.Color;
 import javafx.scene.text.Text;
 import javafx.stage.Stage;

 import java.io.File;
 import java.io.FileReader;
 import java.io.FileWriter;
 import java.io.BufferedReader;
 import java.util.List;


 public class DndTestMultiple extends Application {
     private String TEST_STRING_1 = "test_string_1";
     private String TEST_STRING_2 = "test_string_2";


     @Override
     public void start(Stage stage) {
         stage.setTitle("Drag And Drop Multiple Test");

         final Text source = new Text(50, 100, "DRAG ME");
         source.setScaleX(2.0);
         source.setScaleY(2.0);

         final Text target = new Text(250, 100, "DROP HERE");
         target.setScaleX(2.0);
         target.setScaleY(2.0);

         Group group = new Group();

         VBox root = new VBox(3,
                 new Label("Drag and drop from DRAG ME onto DROP HERE."),
                 new Label("If DROP HERE changes to SUCCESS the test passed"),
                 new Label(""),
                 group);

         Scene scene = new Scene(root, 400, 200);

         source.setOnDragDetected(event -> {
             File f1, f2;

             try {
                 // create test files for dnd operation
                 f1 = File.createTempFile("dnd_test_file_1", ".txt");
                 f1.deleteOnExit();

                 f2 = File.createTempFile("dnd_test_file_2", ".txt");
                 f2.deleteOnExit();

                 FileWriter fw = new FileWriter(f1);
                 fw.write(TEST_STRING_1);
                 fw.close();

                 fw = new FileWriter(f2);
                 fw.write(TEST_STRING_2);
                 fw.close();
             } catch (Exception e) {
                 target.setText("Failed to start DND - Exception caught: " + e.getMessage());
                 return;
             }

             List<File> files = List.of(f1, f2);
             Dragboard dragBoard = ((Node)event.getSource()).startDragAndDrop(TransferMode.ANY);

             ClipboardContent content = new ClipboardContent();
             content.putFiles(files);
             dragBoard.setContent(content);

             event.consume();
         });

         target.setOnDragOver(event -> {
             if (event.getGestureSource() != target &&
                     event.getDragboard().hasFiles()) {
                 event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
             }

             event.consume();
         });

         target.setOnDragEntered(event -> {
             if (event.getGestureSource() != target &&
                     event.getDragboard().hasFiles()) {
                 target.setFill(Color.GREEN);
             }

             event.consume();
         });

         target.setOnDragExited(event -> {
             target.setFill(Color.BLACK);

             event.consume();
         });

         target.setOnDragDropped(event -> {
             Dragboard db = event.getDragboard();
             boolean success = true;
             if (db.hasFiles()) {
                 List<File> files = db.getFiles();

                 try {
                     BufferedReader reader = new BufferedReader(new FileReader(files.get(0)));
                     String result = reader.readLine();
                     reader.close();
                     if (!result.contains(TEST_STRING_1)) {
                         target.setText("FAILED - file 1 contents invalid");
                         success = false;
                     }

                     reader = new BufferedReader(new FileReader(files.get(1)));
                     result = reader.readLine();
                     reader.close();
                     if (!result.contains(TEST_STRING_2)) {
                         target.setText("FAILED - file 2 contents invalid");
                         success = false;
                     }
                 } catch (Exception e) {
                     target.setText("FAILED: " + e.getMessage());
                     success = false;
                 }
             }

             if (success) {
                 target.setText("SUCCESS");
             }

             event.setDropCompleted(success);
             event.consume();
         });

         source.setOnDragDone(event -> {
             if (event.getTransferMode() == TransferMode.MOVE) {
                 source.setText("");
             }

             event.consume();
         });

         group.getChildren().add(source);
         group.getChildren().add(target);

         stage.setScene(scene);
         stage.show();
     }

     public static void main(String[] args) {
         Application.launch(args);
     }
 }
