/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package layout;

import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class GridPaneTab extends Tab {

    final GridPane gridPane = new GridPane();
    final Label descLbl = new Label("Description:");
    final TextArea descText = new TextArea();

    final Button okBtn = new Button("OK");
    final Button cancelBtn = new Button("Cancel");
//    final String imageStr = "resources/images/squares0.jpg";
    final String imageStr = "resources/images/duke_with_guitar.png";
    final Image image = new Image(imageStr, 80, 100, true, true);
    final ImageView imageView = new ImageView(image);
    ColumnConstraints cc3;

    public GridPaneTab(String text) {
        this.setText(text);
        init();
    }

    public void init() {

        // A Label and a TextField
        Label firstNameLbl = new Label("First Name:");
        TextField firstNameFld = new TextField("Duke");
        Label lastNameLbl = new Label("Last Name:");
        TextField lastNameFld = new TextField("Mascot");
        Label phoneLbl = new Label("Phone:");
        TextField phoneFld = new TextField("(415)-123-1234");

        descText.setText("Back in the early days of Java development, Sun Microsystems’ Green Project "
                + "team created its first working demo—an interactive handheld home entertainment "
                + "controller called the Star7. At the heart of the animated touch-screen user "
                + "interface was a cartoon character named Duke. The jumping, cartwheeling Duke "
                + "was created by one of the team's graphic artists, Joe Palrang. Joe went "
                + "on to work on popular animated movies such as Shrek, Over the Hedge, "
                + "and Flushed Away.\n\n"
                + "Duke was designed to represent a \"software agent\" that performed tasks "
                + "for the user. Duke was the interactive host that enabled a new type of "
                + "user interface that went beyond the buttons, mice, and pop-up menus "
                + "of the desktop computing world.\n\n"
                + "Duke was instantly embraced. In fact, at about the same time Java "
                + "was first introduced and the first Java cup logo was commissioned, "
                + "Duke became the official mascot of Java technology. In 2006, Duke "
                + "was officially \"open sourced” under a BSD license. Developers and "
                + "designers were encouraged to play around with Duke and for the first "
                + "time had access to Duke’s graphical specifications through a java.net "
                + "project at http://duke.kenai.com.\n\n"
                + "At Oracle, we celebrate Duke, too. A living, life-size Duke is a "
                + "popular feature at every JavaOne developer conference. And each year, "
                + "Oracle releases a new Duke personality. Last year it was Surfing Duke, "
                + "who tagged along for the ride at the Java Road Trip: Code to Coast. This "
                + "year it’s Future Tech Duke, moving Java forward into new technology and "
                + "platform arenas.\n\n"
                + "For a limited time, you can get a 3-D animated screensaver of Future Tech "
                + "Duke for your computer.\n\n"
                + "For more information about Java, visit Oracle.com/java.");
        descText.setPrefColumnCount(20);
        descText.setPrefRowCount(5);
        descText.setWrapText(true);

        // A Label used as a status bar
        Label statusBar = new Label("Status: Ready");
        statusBar.setStyle("-fx-background-color: lavender;"
                + "-fx-padding: 10 0 0 0;"
                + "-fx-text-fill: green");

        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.getStyleClass().add("layout");

        // Add children to the GridPane
        gridPane.add(firstNameLbl, 0, 0, 1, 1);
        gridPane.add(firstNameFld, 1, 0, 2, 1);
        gridPane.add(lastNameLbl, 0, 1, 1, 1);
        gridPane.add(lastNameFld, 1, 1, 2, 1);
        gridPane.add(phoneLbl, 0, 2, 1, 1);
        gridPane.add(phoneFld, 1, 2, 1, 1);

        gridPane.add(descLbl, 0, 4, 1, 1);
        gridPane.add(descText, 0, 5, 3, 1);

        gridPane.add(imageView, 4, 0, 2, 3);
        gridPane.add(okBtn, 4, 3, 1, 1);
        gridPane.add(cancelBtn, 4, 4, 1, 1);

        gridPane.add(statusBar, 0, 6, GridPane.REMAINING, 1);

        ColumnConstraints cc0 = new ColumnConstraints(20, 100, 100);
        ColumnConstraints cc1 = new ColumnConstraints(135);
        ColumnConstraints cc2 = new ColumnConstraints(150);
        cc3 = new ColumnConstraints(20, 200, Double.MAX_VALUE);
        cc3.setFillWidth(true);
        gridPane.getColumnConstraints().addAll(cc0, cc1, cc2, cc3);

        GridPane.setValignment(imageView, VPos.TOP);


        // The status bar in the last should fill its cell
        statusBar.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane(gridPane);

        final CheckBox hGrowCbx = new CheckBox("Hgrow");
        hGrowCbx.setSelected(false);
        hGrowCbx.setOnAction(e -> growHorizontal(firstNameFld, hGrowCbx.isSelected()));

        final CheckBox vGrowCbx = new CheckBox("Vgrow");
        vGrowCbx.setSelected(false);
        vGrowCbx.setOnAction(e -> growVertical(descText, vGrowCbx.isSelected()));

        CheckBox stretchButtonCbx = new CheckBox("Stretch Button");
        stretchButtonCbx.setOnAction(e -> stretchButton(stretchButtonCbx.isSelected()));

        CheckBox remainingCbx = new CheckBox("GridPane.REMAINING TextArea");
        remainingCbx.setOnAction(e -> remainingText(remainingCbx.isSelected()));

        CheckBox debugCbx = new CheckBox("Grid Lines Visible");
        debugCbx.setOnAction(e -> gridPane.setGridLinesVisible(debugCbx.isSelected()));

        HBox controlGrp = new HBox(hGrowCbx, vGrowCbx, stretchButtonCbx, remainingCbx, debugCbx);
        controlGrp.getStyleClass().add("control");
        controlGrp.setAlignment(Pos.CENTER_LEFT);
        root.setTop(controlGrp);

        this.setContent(root);
    }

    void stretchButton(boolean stretch) {
        if (stretch) {
            // Let the Cancel button expand vertically
            okBtn.setMaxWidth(Double.MAX_VALUE);
            cancelBtn.setMaxWidth(Double.MAX_VALUE);
        } else {
            okBtn.setMaxWidth(okBtn.getPrefWidth());
            cancelBtn.setMaxWidth(cancelBtn.getPrefWidth());
        }

    }

    void growHorizontal(TextField desc, boolean grow) {
        if (grow) {
            GridPane.setHgrow(desc, Priority.ALWAYS);
            cc3.setHgrow(Priority.ALWAYS);
        } else {
            GridPane.setHgrow(desc, Priority.NEVER);
            cc3.setHgrow(Priority.NEVER);
        }
    }

    void growVertical(TextArea desc, boolean grow) {
        if (grow) {
            GridPane.setVgrow(desc, Priority.ALWAYS);
        } else {
            GridPane.setVgrow(desc, Priority.NEVER);
        }
    }

    void remainingText(boolean remaining) {
        if (remaining) {
            GridPane.setColumnSpan(descText, GridPane.REMAINING);
        } else {
            GridPane.setColumnSpan(descText, 3);
        }
    }

}
