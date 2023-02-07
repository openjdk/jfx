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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/*
 * @bug 8290092
 */
public class ClipboardExtImageTest extends Application {
    Label testStatus = new Label();
    ImageView clipboardImageView = new ImageView();

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    private class TempFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.toLowerCase().endsWith(".tmp");
        }
    }

    private void testTemporaryFilesLeftover() throws Exception {
        if (!Clipboard.getSystemClipboard().hasImage()) {
            testStatus.setText("No Image found in system clipboard - copy one before running the test");
            testStatus.setTextFill(Color.DARKORANGE);
            return;
        }

        // Filter for temporary files produced by Windows system clipboard implementation

        // List all files in java.io.tmpdir
        File tmpPath = new File(System.getProperty("java.io.tmpdir"));
        File[] preGetTmpFiles = tmpPath.listFiles(new TempFileFilter());

        Image i = Clipboard.getSystemClipboard().getImage();

        File[] postGetTmpFiles = tmpPath.listFiles(new TempFileFilter());

        if (i != null) {
            clipboardImageView.setImage(i);
        }

        for (File f: preGetTmpFiles) {
            System.err.println("tmpFile: " + f.getPath());
        }

        for (File f: postGetTmpFiles) {
            System.err.println("newTmpFile: " + f.getPath());
        }

        ArrayList<String> leftovers = new ArrayList<String>();
        for (File postf: postGetTmpFiles) {
            boolean existed = false;
            for (File pref: preGetTmpFiles) {
                if (postf.getName().equals(pref.getName())) {
                    existed = true;
                    break;
                }
            }

            // every file from temp file list post-getImage() must've also existed pre-getImage()
            // if that's not the case, then we most probably left some temp files after ourselves.
            if (!existed) {
                leftovers.add(postf.getName());
            }
        }

        if (!leftovers.isEmpty()) {
            testStatus.setText("FAILED (File " + leftovers.get(0) + " was left behind)");
            testStatus.setTextFill(Color.RED);
        } else {
            testStatus.setText("PASSED (No spare temporary files found)");
            testStatus.setTextFill(Color.GREEN);
        }
    }

    private Scene createScene() {
        Label warn = new Label();
        warn.setText("This test refers to a Windows-specific issue.");

        Label instructions = new Label();
        instructions.setText(
            "Follow these steps to test the scenario:\n" +
            " 1. Open your web browser, find a random picture and copy it to system's Clipboard\n" +
            " 2. Click on \"Test - Read Clipboard\" button\n" +
            "If image was fetched from Clipboard properly, it will show in the scroll pane below.\n" +
            "After the test is run there should be no extra temporary files left by JFX in the TMP dir."
        );
        instructions.setMinHeight(90.0);

        Label testStatusHeader = new Label();
        testStatusHeader.setText("Test status:");

        testStatus.setText("...");

        Button testButton = new Button("Test - Read Clipboard");
        testButton.setOnAction((ActionEvent) -> {
            try {
                testTemporaryFilesLeftover();
            } catch (Exception e) {
                testStatus.setText("FAILED (Caught exception: " + e.getMessage() + ")");
                testStatus.setTextFill(Color.RED);
            }
        });

        Button openTmpButton = new Button("Open TMP dir");
        openTmpButton.setOnAction((ActionEvent) -> {
            try {
                Runtime.getRuntime().exec(new String[]{"explorer.exe", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath()});
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Failed to open TMP dir");
                alert.setContentText("TMP dir failed to open: caught Exception " + e.getMessage());
                alert.show();
            }
        });

        clipboardImageView.setPickOnBounds(true);
        clipboardImageView.setPreserveRatio(true);

        ScrollPane pane = new ScrollPane(clipboardImageView);
        pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        pane.setFitToHeight(true);
        pane.setFitToWidth(true);

        VBox box = new VBox(warn, instructions, testStatusHeader, testStatus, testButton, openTmpButton, pane);
        box.setAlignment(Pos.TOP_LEFT);
        box.setSpacing(5.0);
        box.setFillWidth(true);
        box.setVgrow(pane, Priority.ALWAYS);
        box.setPadding(new Insets(15.0));

        return new Scene(box, 800, 600);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            if (!isWindows()) {
                System.out.println("This test refers to Windows-only issue and won't work on other platforms. Exiting.");
                Platform.exit();
            }

            primaryStage.setScene(createScene());
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
