/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FXCanvasMouseButtonEventsTest {

    static final String instructions =
            "This tests that SWT mouse button events (press, release, and click) are properly transferred to SWT. " +
                    "It passes if all mouse button events for each of the 5 buttons are recognized properly.";

    private static TextArea createInfo(String msg) {
        TextArea t = new TextArea(msg);
        t.setWrapText(true);
        t.setEditable(false);
        t.setMaxWidth(400);
        t.setMaxHeight(100);
        return t;
    }

    public static void main(String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("FXCanvasMouseButtonEventsTest");
        shell.setSize(400, 400);
        shell.setLayout(new FillLayout());
        final FXCanvas canvas = new FXCanvas(shell, SWT.NONE);
        shell.open();

        // create and hook scene
        Group root = new Group();

        TextArea info = createInfo(instructions);
        Label clickOutput = new Label("No click events yet...");
        Label pressOutput = new Label("No press events yet...");
        Label releaseOutput = new Label("No release events yet...");

        VBox vbox = new VBox();
        vbox.getChildren().addAll(info, clickOutput, pressOutput, releaseOutput);
        root.getChildren().add(vbox);

        final Scene scene = new Scene(root, 400, 400);

        final int[] clickEventCount = {0};
        root.setOnMouseClicked(clickEvent -> {
            clickOutput.setText("Mouse CLICK #" + clickEventCount[0]++ + ": button: " + clickEvent.getButton());
        });
        final int[] pressEventCount = {0};
        root.setOnMousePressed(pressEvent -> {
            pressOutput.setText("Mouse PRESS #" + pressEventCount[0]++ + ": button: " + pressEvent.getButton());
        });
        final int[] releaseEventCount = {0};
        root.setOnMouseReleased(releaseEvent -> {
            releaseOutput.setText("Mouse RELEASE #" + releaseEventCount[0]++ + ": button: " + releaseEvent.getButton());
        });

        canvas.setScene(scene);

        while (!shell.isDisposed()) {
            // run SWT event loop
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
