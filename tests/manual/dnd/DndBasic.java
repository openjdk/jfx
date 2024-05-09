/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.UUID;
import java.util.Set;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DndBasic extends Application {

    ArrayList<Text> sourceResults = new ArrayList<Text>();
    ArrayList<Text> targetResults = new ArrayList<Text>();
    Rectangle lastDropTarget = null;

    @Override
    public void start(Stage primaryStage) throws Exception {

        TransferMode[] copyOnly = new TransferMode[]{TransferMode.COPY};
        TransferMode[] moveOnly = new TransferMode[]{TransferMode.MOVE};
        TransferMode[] linkOnly = new TransferMode[]{TransferMode.LINK};

        Label sourcesLabel = new Label("Sources");
        Group sourceAll = createSource(TransferMode.ANY);
        Group sourceCopyMove = createSource(TransferMode.COPY_OR_MOVE);
        Group sourceCopyOnly = createSource(copyOnly);
        Group sourceMoveOnly = createSource(moveOnly);
        Group sourceLinkOnly = createSource(linkOnly);

        VBox sources = new VBox(sourcesLabel,
            sourceAll, sourceCopyMove, sourceCopyOnly,
            sourceMoveOnly, sourceLinkOnly);
        sources.setSpacing(10);

        Label targetLabel = new Label("Destinations");
        Group targetAll = createTarget(TransferMode.ANY);
        Group targetCopyMove = createTarget(TransferMode.COPY_OR_MOVE);
        Group targetCopyOnly = createTarget(copyOnly);
        Group targetMoveOnly = createTarget(moveOnly);
        Group targetLinkOnly = createTarget(linkOnly);

        VBox targets = new VBox(targetLabel,
            targetAll, targetCopyMove, targetCopyOnly,
            targetMoveOnly, targetLinkOnly);
        targets.setSpacing(10);

        HBox columns = new HBox(sources, targets);
        columns.setSpacing(10);

        Label instructions = new Label("Drag from a source to a destination (the desktop can be either)");
        VBox withInstructions = new VBox(instructions, columns);
        withInstructions.setSpacing(20);
        withInstructions.setPadding(new Insets(10, 10, 10, 10));
        Scene s = new Scene(withInstructions);
        primaryStage.setScene(s);
        primaryStage.show();
    }

    private Text modesToText(String l, TransferMode[] modes) {
        String label = "";
        for (TransferMode c : modes) {
            if (label != "") {
                label = label + " ";
            }
            label += c;
        }
        Text text = new Text(label);
        return text;
    }

    private String modesToString(Set<TransferMode> modes) {
        String label = "";
        if (modes.contains(TransferMode.COPY)) {
            label += " " + TransferMode.COPY;
        }
        if (modes.contains(TransferMode.MOVE)) {
            label += " " + TransferMode.MOVE;
        }
        if (modes.contains(TransferMode.LINK)) {
            label += " " + TransferMode.LINK;
        }
        return label;
    }

    private void clearSourceResults() {
        for (Text t : sourceResults) {
            t.setText("");
        }
    }

    private void clearTargetResults() {
        for (Text t : targetResults) {
            t.setText("");
        }
    }

    private Group createSource(TransferMode[] modes) {
        Text title = modesToText("Source", modes);
        Text result = new Text("");
        title.setMouseTransparent(true);
        result.setMouseTransparent(true);
        sourceResults.add(result);

        VBox labels = new VBox(title, result);
        labels.setMouseTransparent(true);
        labels.setSpacing(3);
        labels.setPadding(new Insets(5, 5, 5, 5));

        Rectangle source = new Rectangle(300, 75);
        source.setOnDragDetected( evt -> {
            Dragboard dragboard = source.startDragAndDrop(modes);
            ClipboardContent content = new ClipboardContent();
            content.putString(UUID.randomUUID().toString());
            dragboard.setContent(content);
            evt.consume();
            clearSourceResults();
            clearTargetResults();
            result.setText("Dragging");
        });
        source.setOnDragDone( evt -> {
            String message = "Drag done, ";
            message = message + (evt.isAccepted() ? "accepted" : "rejected");
            if (evt.getTransferMode() != null) {
                message = message + " " + evt.getTransferMode();
            }
            result.setText(message);
            evt.consume();
        });
        source.setFill(Color.LIGHTGREEN);

        return new Group(source, labels);
    }

    private Group createTarget(TransferMode[] modes) {
        Text title = modesToText("Destination", modes);
        Text available = new Text("");
        Text result = new Text("");
        targetResults.add(result);
        title.setMouseTransparent(true);
        available.setMouseTransparent(true);
        result.setMouseTransparent(true);

        VBox labels = new VBox(title, available, result);
        labels.setMouseTransparent(true);
        labels.setSpacing(3);
        labels.setPadding(new Insets(5, 5, 5, 5));

        Rectangle target = new Rectangle(300, 75);
        target.setOnDragOver(evt -> {
            evt.acceptTransferModes(modes);
            clearTargetResults();
            if (evt.getGestureSource() == null) {
                clearSourceResults();
            }
            if (evt.getTransferMode() != null) {
                result.setText("Proposed " + evt.getTransferMode());
            }
            else {
                result.setText("");
            }
            // Note: we always show the source modes to illustrate how the Mac filters the
            // available modes based on the modifier key states.
            available.setText("Source" + modesToString(evt.getDragboard().getTransferModes()));
            evt.consume();
        });
        target.setOnDragDropped(evt -> {
            lastDropTarget = target;
            String text = "Dropped here";
            if (evt.getTransferMode() != null) {
                text += ", proposed " + evt.getTransferMode();
            }
            result.setText(text);
            evt.setDropCompleted(true);
            evt.consume();
        });
        target.setOnDragExited(evt -> {
            available.setText("");
            if (target != lastDropTarget) {
                result.setText("");
            }
            evt.consume();
        });
        target.setFill(Color.LIGHTBLUE);

        return new Group(target, labels);
    }

    public static void main(String[] args) {
        launch(DndBasic.class, args);
    }
}
