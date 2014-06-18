/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samples.layout.gridpane;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * An example of a GridPane layout. There is more than one approach to using a
 * GridPane. The code can specify which rows and/or columns should
 * contain the content. Alternatively, the code can alter the constraints of the
 * rows and/or columns themselves, either by specifying the preferred minimum
 * or  maximum heights or widths, or by specifying the percentage of the
 * GridPane that belongs to certain rows or columns.  Note that grid lines can be
 * made visible to help in debugging.
 *
 * @sampleName GridPane
 * @preview preview.png
 * @see javafx.scene.layout.GridPane
 * @related /Controls/Text/Simple Label
 * @embedded
 */
public class GridPaneApp extends Application {

    private static final Image ICON_48 = new Image(GridPaneApp.class.getResourceAsStream("/ensemble/samples/shared-resources/icon-48x48.png"));

    public Parent createContent() {
        VBox vbox = new VBox();

        Label gridPerCaption = new Label("GridPane content placement by influencing row and column percentages.");
        gridPerCaption.setWrapText(true);
        GridPane gridPer = createGridPanePercentage();

        Label gridRCInfoCaption = new Label("GridPane content placement by influencing the rows and columns themselves:");
        gridRCInfoCaption.setWrapText(true);
        GridPane gridRCInfo = createGridPaneRCInfo();

        Label gridConstCaption = new Label("GridPane content placement by specifying rows and columns:");
        gridConstCaption.setWrapText(true);
        GridPane gridConst = createGridPaneConst();

        vbox.getChildren().addAll(gridPerCaption, gridPer, new Separator());
        vbox.getChildren().addAll(gridRCInfoCaption, gridRCInfo, new Separator());
        vbox.getChildren().addAll(gridConstCaption, gridConst);
        return vbox;
    }

    //The resulting GridPane places the child by influencing the rows and columns
    //via GridRowInfo and GridColumnInfo. This grid uses the percentages
    private GridPane createGridPanePercentage() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8, 8, 8, 8));
        RowConstraints rowinfo3 = new RowConstraints();
        rowinfo3.setPercentHeight(50);

        ColumnConstraints colInfo2 = new ColumnConstraints();
        colInfo2.setPercentWidth(25);

        ColumnConstraints colInfo3 = new ColumnConstraints();
        colInfo3.setPercentWidth(50);

        grid.getRowConstraints().add(rowinfo3);//2*50 percent
        grid.getRowConstraints().add(rowinfo3);

        grid.getColumnConstraints().add(colInfo2); //25 percent
        grid.getColumnConstraints().add(colInfo3); //50 percent
        grid.getColumnConstraints().add(colInfo2); //25 percent

        Label condLabel = new Label(" Member Name:");
        GridPane.setHalignment(condLabel, HPos.RIGHT);
        GridPane.setConstraints(condLabel, 0, 0);
        Label condValue = new Label("MyName");
        GridPane.setMargin(condValue, new Insets(0, 0, 0, 10));
        GridPane.setConstraints(condValue, 1, 0);

        Label acctLabel = new Label("Member Number:");
        GridPane.setHalignment(acctLabel, HPos.RIGHT);
        GridPane.setConstraints(acctLabel, 0, 1);
        TextField textBox = new TextField("Your number");
        GridPane.setMargin(textBox, new Insets(10, 10, 10, 10));
        GridPane.setConstraints(textBox, 1, 1);

        Button button = new Button("Help");
        GridPane.setConstraints(button, 2, 1);
        GridPane.setMargin(button, new Insets(10, 10, 10, 10));
        GridPane.setHalignment(button, HPos.CENTER);

        GridPane.setConstraints(condValue, 1, 0);
        grid.getChildren().addAll(condLabel, condValue, button, acctLabel, textBox);
        return grid;
    }

    //The resulting GridPane places the child by influencing the rows and columns themselves
    //via GridRowInfo and GridColumnInfo. This grid uses the preferred
    //width/height and max/min width/height.
    private GridPane createGridPaneRCInfo() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8, 8, 8, 8));

        RowConstraints rowinfo = new RowConstraints(40, 40, 40);
        ColumnConstraints colinfo = new ColumnConstraints(90, 90, 90);

        for (int i = 0; i <= 2; i++) {
            grid.getRowConstraints().add(rowinfo);
        }

        for (int j = 0; j <= 2; j++) {
            grid.getColumnConstraints().add(colinfo);
        }

        Label category = new Label("Category:");
        GridPane.setHalignment(category, HPos.RIGHT);
        Label categoryValue = new Label("Coffee");
        Label company = new Label("Type:");
        GridPane.setHalignment(company, HPos.RIGHT);
        Label companyValue = new Label("Kona");
        Label rating = new Label("Rating:");
        GridPane.setHalignment(rating, HPos.RIGHT);
        Label ratingValue = new Label("Excellent");

        ImageView imageView = new ImageView(ICON_48);
        GridPane.setHalignment(imageView, HPos.CENTER);

        //Place content
        GridPane.setConstraints(category, 0, 0);
        GridPane.setConstraints(categoryValue, 1, 0);
        GridPane.setConstraints(company, 0, 1);
        GridPane.setConstraints(companyValue, 1, 1);
        GridPane.setConstraints(imageView, 2, 1);
        GridPane.setConstraints(rating, 0, 2);
        GridPane.setConstraints(ratingValue, 1, 2);
        grid.getChildren().addAll(category, categoryValue, company, companyValue, imageView, rating, ratingValue);
        return grid;
    }

    //grid1 places the child by specifying the rows and columns in GridPane.setConstraints()
    private GridPane createGridPaneConst() {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 8, 8, 8));
        grid.setGridLinesVisible(true);

        ObservableList<Node> content = grid.getChildren();

        Label label = new Label("Name:");
        GridPane.setConstraints(label, 0, 0);
        GridPane.setHalignment(label, HPos.RIGHT);
        content.add(label);

        label = new Label("John Q. Public");
        GridPane.setConstraints(label, 1, 0, 2, 1);
        GridPane.setHalignment(label, HPos.LEFT);
        content.add(label);

        label = new Label("Address:");
        GridPane.setConstraints(label, 0, 1);
        GridPane.setHalignment(label, HPos.RIGHT);
        content.add(label);

        label = new Label("12345 Main Street, Some City, CA");
        GridPane.setConstraints(label, 1, 1, 5, 1);
        GridPane.setHalignment(label, HPos.LEFT);
        content.add(label);
        return grid;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
