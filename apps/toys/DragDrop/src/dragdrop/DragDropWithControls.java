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

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;

public class DragDropWithControls extends Application {

    final static DataFormat customBytes = new DataFormat("dndwithcontrols.custom.bytes");
    final static DataFormat customString = new DataFormat("dndwithcontrols.custom.string");

    final static String html =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
        "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
        "<head></head>" +
        "<body>Hello!</body>" +
        "</html>";

    final static String rtf = "{\\rtf1\\ansi\\uc1{\\colortbl;\\red255\\green0\\blue0;}\\uc1\\b\\i FRED\\par rtf\\par text}";

    Pane sourceControlPane = new Pane();
    Pane targetControlPane = new Pane();
    Set<TransferMode> sourceModes = EnumSet.noneOf(TransferMode.class);
    Set<TransferMode> targetModes = EnumSet.noneOf(TransferMode.class);
    Set<DataFormat> sourceFormats = new HashSet<DataFormat>();
    Set<DataFormat> targetFormats = new HashSet<DataFormat>();
    Image image;
    Text log = new Text();
    List<String> messages = new LinkedList<String>();
    List<File> files = new LinkedList<File>();

    @Override public void start(final Stage stage) {
        final Group root = new Group();
        root.getChildren().add(createMainPane());
        final Scene scene = new Scene(root);

        stage.setTitle("Drag and Drop Controls");
        stage.setWidth(700);
        stage.setHeight(700);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();

        new Thread(new Runnable() {
            @Override public void run() {
                log("Loading image..");
                image = new Image("http://openjdk.java.net/images/duke-thinking.png");
                log("Ready.");
                log("");
            }
        }).start();
    }

    private Node createMainPane() {
        Pane main = new Pane();

        HBox lr = new HBox(10);

        VBox src = new VBox(10);
        src.getChildren().add(new Text("SOURCE CONTROL:"));
        src.getChildren().add(new Separator());
        src.getChildren().add(sourceControlPane);
        src.getChildren().add(new Separator());
        src.getChildren().add(createLeftPane());

        VBox trgt = new VBox(10);
        trgt.getChildren().add(new Text("TARGET CONTROL:"));
        trgt.getChildren().add(new Separator());
        trgt.getChildren().add(targetControlPane);
        trgt.getChildren().add(new Separator());
        trgt.getChildren().add(createRightPane());

        lr.getChildren().add(src);
        lr.getChildren().add(new Separator(Orientation.VERTICAL));
        lr.getChildren().add(trgt);

        VBox tb = new VBox(10);
        tb.getChildren().add(lr);
        tb.getChildren().add(new Separator());
        tb.getChildren().add(log);

        main.getChildren().add(tb);
        return main;
    }

    private Node createLeftPane() {
        VBox box = new VBox(10);
        HBox hbox = new HBox(10);

        VBox lbox = new VBox(10);
        lbox.getChildren().add(new Text("Source control type:"));
        lbox.getChildren().add(createControlCombo(sourceControlPane, true));
        lbox.getChildren().add(new Text("Source transfer modes:"));
        lbox.getChildren().add(createTMSelect(sourceModes));

        VBox rbox = new VBox(10);
        rbox.getChildren().add(new Text("Data formats:"));
        rbox.getChildren().add(createFormatSelect(sourceFormats));

        hbox.getChildren().add(lbox);
        hbox.getChildren().add(new Separator(Orientation.VERTICAL));
        hbox.getChildren().add(rbox);

        final Text fileHdr = new Text("Files to drag (0):");
        final TextField tb = new TextField("Put full path here");
        final Button add = new Button("Add");
        add.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                File f = new File(tb.getText());
                if (f.exists()) {
                    files.add(f);
                    tb.setText("");
                    fileHdr.setText("Files to drag (" + files.size() + ")");
                    log("Added file " + f.getPath());
                } else {
                    log("File doesn't exist: " + f.getPath());
                }
            }
        });
        final Button clear = new Button("Clear");
        clear.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                files.clear();
                fileHdr.setText("Files to drag (0)");
                log("File list cleared");
            }
        });

        HBox btns = new HBox();
        btns.getChildren().add(add);
        btns.getChildren().add(clear);

        box.getChildren().add(hbox);
        box.getChildren().add(new Separator());
        box.getChildren().add(fileHdr);
        box.getChildren().add(tb);
        box.getChildren().add(btns);

        return box;
    }

    private Node createRightPane() {
        HBox hbox = new HBox(10);

        VBox lbox = new VBox(10);

        lbox.getChildren().add(new Text("Target control type:"));
        lbox.getChildren().add(createControlCombo(targetControlPane, false));
        lbox.getChildren().add(new Text("Target transfer modes:"));
        lbox.getChildren().add(createTMSelect(targetModes));

        VBox rbox = new VBox(10);
        rbox.getChildren().add(new Text("Data formats:"));
        rbox.getChildren().add(createFormatSelect(targetFormats));

        hbox.getChildren().add(lbox);
        hbox.getChildren().add(new Separator(Orientation.VERTICAL));
        hbox.getChildren().add(rbox);

        return hbox;
    }

    private Node createControlCombo(final Pane sourceControlPane, final boolean source) {
        ChoiceBox<String> cb = new ChoiceBox<String>();
        cb.getItems().addAll("Rectangle", "Button", "Checkbox", "ChoiceBox", "Hyperlink",
                "Label", "List", "RadioButton", "Tree");

        cb.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                Node ctrl = null;
                String text = source ? "This is source" : "This is target";
                if (t1.equals("Rectangle")) {
                    ctrl = new Rectangle(50, 25, Color.DARKGRAY);
                } else if (t1.equals("Button")) {
                    ctrl = new Button(text);
                } else if (t1.equals("List")) {
                    ListView<String> list = new ListView<String>(FXCollections.observableArrayList(
                            text, "Foo", "Bar", "More", "Enough"));
                    list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    list.setPrefSize(list.getPrefWidth(), 80);
                    ctrl = list;
                } else if (t1.equals("Tree")) {
                    TreeItem<String> ti = new TreeItem(text);
                    ti.getChildren().add(new TreeItem<String>("Foo"));
                    ti.getChildren().add(new TreeItem<String>("Bar"));
                    ti.getChildren().add(new TreeItem<String>("More"));
                    ti.getChildren().add(new TreeItem<String>("Enough"));
                    TreeView tree = new TreeView<String>(ti);
                    tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    tree.setPrefSize(tree.getPrefWidth(), 80);
                    ctrl = tree;
                } else if (t1.equals("Checkbox")) {
                    ctrl = new CheckBox(text);
                } else if (t1.equals("Hyperlink")) {
                    ctrl = new Hyperlink(text);
                } else if (t1.equals("Label")) {
                    ctrl = new Label(text);
                } else if (t1.equals("RadioButton")) {
                    ctrl = new RadioButton(text);
                } else if (t1.equals("ChoiceBox")) {
                    ChoiceBox c = new ChoiceBox();
                    c.getItems().addAll(text, "More");
                    c.getSelectionModel().select(0);
                    ctrl = c;
                }
                sourceControlPane.getChildren().clear();
                sourceControlPane.getChildren().add(ctrl);

                final Node control = ctrl;

                if (source) {
                    control.setOnDragDetected(new EventHandler<MouseEvent>() {
                        @Override public void handle(MouseEvent event) {
                            Dragboard db = control.startDragAndDrop(
                                    sourceModes.toArray(new TransferMode[sourceModes.size()]));
                            if (db == null) {
                                log("Cannot start drag and drop.");
                                return;
                            }
                            ClipboardContent content = new ClipboardContent();
                            if (sourceFormats.contains(DataFormat.PLAIN_TEXT)) {
                                log("Source is putting string on dragboard");
                                content.putString("Hello!");
                            }
                            if (sourceFormats.contains(DataFormat.URL)) {
                                log("Source is putting URL on dragboard");
                                content.putUrl("http://www.oracle.com");
                            }
                            if (sourceFormats.contains(DataFormat.IMAGE)) {
                                log("Source is putting image on dragboard");
                                content.putImage(image);
                            }
                            if (sourceFormats.contains(DataFormat.HTML)) {
                                log("Source is putting HTML on dragboard");
                                content.putHtml(html);
                            }
                            if (sourceFormats.contains(DataFormat.RTF)) {
                                log("Source is putting RTF on dragboard");
                                content.putRtf(rtf);
                            }
                            if (sourceFormats.contains(customBytes)) {
                                log("Source is putting custom four bytes on dragboard");
                                content.put(customBytes, new byte[] { 1, 2, 3, 4 });
                            }
                            if (sourceFormats.contains(customString)) {
                                log("Source is putting custom four bytes on dragboard");
                                content.put(customString, "Hello Custom String!");
                            }
                            if (sourceFormats.contains(DataFormat.FILES)) {
                                log("Source is putting two files on dragboard");
                                content.putFiles(files);
                            }
                            db.setContent(content);
                            event.consume();
                        }
                    });

                    control.setOnDragDone(new EventHandler<DragEvent>() {
                        @Override public void handle(DragEvent event) {
                            log("Transfer done: " + event.getTransferMode());
                            log("");
                        }
                    });
                } else {
                    control.setOnDragOver(new EventHandler<DragEvent>() {
                        @Override public void handle(DragEvent event) {
                            Dragboard db = event.getDragboard();
                            for (DataFormat df : targetFormats) {
                                if (db.hasContent(df)) {
                                    event.acceptTransferModes(
                                            targetModes.toArray(new TransferMode[targetModes.size()]));
                                    return;
                                }
                            }
                        }
                    });

                    control.setOnDragDropped(new EventHandler<DragEvent>() {
                        @Override public void handle(DragEvent event) {
                            Dragboard db = event.getDragboard();
                            boolean gotData = false;
                            if (targetFormats.contains(DataFormat.PLAIN_TEXT) && db.hasString()) {
                                log("Dropped string: " + db.getString());
                                gotData = true;
                            }
                            if (targetFormats.contains(DataFormat.HTML) && db.hasHtml()) {
                                log("Dropped HTML: " + db.getHtml());
                                gotData = true;
                            }
                            if (targetFormats.contains(DataFormat.RTF) && db.hasRtf()) {
                                log("Dropped RTF: " + db.getRtf());
                                gotData = true;
                            }
                            if (targetFormats.contains(DataFormat.URL) && db.hasUrl()) {
                                log("Dropped URL: " + db.getUrl());
                                gotData = true;
                            }
                            if (targetFormats.contains(DataFormat.IMAGE) && db.hasImage()) {
                                log("Dropped image: " + db.getImage());
                                gotData = true;
                            }
                            if (targetFormats.contains(DataFormat.FILES) && db.hasFiles()) {
                                log("Dropped files:");
                                for (File f : db.getFiles()) {
                                    log("   " + f.getPath());
                                }
                                gotData = true;
                            }
                            if (targetFormats.contains(customBytes) && db.hasContent(customBytes)) {
                                byte[] b = (byte[]) db.getContent(customBytes);
                                log("Dropped custom bytes: " + b[0] + ", " + b[1] + ", " + b[2] + ", " + b[3]);
                                gotData = true;
                            }
                            if (targetFormats.contains(customString) && db.hasContent(customString)) {
                                String s = (String) db.getContent(customString);
                                log("Dropped custom string: " + s);
                                gotData = true;
                            }
                            event.setDropCompleted(gotData);
                        }
                    });
                }

            }

        });

        cb.getSelectionModel().select(0);
        return cb;
    }

    private Node createTMSelect(final Set<TransferMode> tms) {
        VBox box = new VBox();

        for (final TransferMode tm : TransferMode.values()) {
            CheckBox cb = new CheckBox(tm.toString());
            cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                    if (t1) {
                        tms.add(tm);
                    } else {
                        tms.remove(tm);
                    }
                }
            });
            if (tm == TransferMode.COPY) {
                cb.selectedProperty().set(true);
            }
            box.getChildren().add(cb);
        }

        return box;
    }

    private Node createFormatSelect(final Set<DataFormat> dataFormats) {
        VBox box = new VBox();

        List<Pair<DataFormat, String>> list = new ArrayList<Pair<DataFormat, String>>(10);
        list.add(new Pair<DataFormat, String>(DataFormat.PLAIN_TEXT, "PLAIN_TEXT"));
        list.add(new Pair<DataFormat, String>(DataFormat.HTML, "HTML"));
        list.add(new Pair<DataFormat, String>(DataFormat.RTF, "RTF"));
        list.add(new Pair<DataFormat, String>(DataFormat.URL, "URL"));
        list.add(new Pair<DataFormat, String>(DataFormat.IMAGE, "IMAGE"));
        list.add(new Pair<DataFormat, String>(DataFormat.FILES, "FILES"));
        list.add(new Pair<DataFormat, String>(customBytes, "Custom (bytes)"));
        list.add(new Pair<DataFormat, String>(customString, "Custom (String)"));

        for (final Pair<DataFormat, String> df : list) {
            CheckBox cb = new CheckBox(df.getValue());
            cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                    if (t1) {
                        dataFormats.add(df.getKey());
                    } else {
                        dataFormats.remove(df.getKey());
                    }
                }
            });
            box.getChildren().add(cb);
        }

        ((CheckBox) box.getChildren().get(0)).selectedProperty().set(true);

        return box;
    }

    private void log(String text) {
        System.out.println(text);

        messages.add(text);
        if (messages.size() > 15) {
            messages.remove(0);
        }

        StringBuilder sb = new StringBuilder();
        for (String msg : messages) {
            sb.append(msg).append("\n");
        }
        Platform.runLater(() -> { log.setText(sb.toString()); });
    }

    public static String info() {
        return
                "This application provides for drag and drop"
                + "between controls with toggles for the drag"
                + "options and source types";
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
