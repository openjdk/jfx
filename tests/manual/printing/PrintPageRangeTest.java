/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;

public class PrintPageRangeTest extends Application {

    private static final double NAVI_BAR_MIN_DIMENSION = 32.0;
    private static final double PADDING_VALUE = 2.0;
    private static final String buttonStyle = "-fx-font-weight: bold; -fx-font-size: 16px;";

    private Scene scene;
    private WebView webView;
    private Label bottomMessageLabel;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
        stage.setWidth(640);
        stage.setHeight(480);
        stage.setTitle("Printing test with Page Range Option");
        stage.setScene(createScene());
        stage.show();
    }

    static final String instructions =
                    " 1. Press print button to open Print Dialog.\n" +
                    " 2. Select a Printer, Page Size A4 and PageRange 3 to 4 in page range selection.\n" +
                    " 3. Click Ok.\n" +
                    " 4. After this 2 pages must be printed.\n" +
                    " 5. When all pages are printed you will see <END OF PRINT JOB> on bottom.\n" +
                    " 6. Check whether printed pages are page no 3 and page no 4.\n" +
                    "     Note: In printed pages, first page should start with approx <HTML Line No. 50>.\n" +
                    " 7. if Yes then Test Passed else Test Failed";

    private String createHtmlPage() {
        StringBuilder htmlStringBuilder = new StringBuilder();
        htmlStringBuilder.append("<html><head></head><body>");
        for (int i = 0; i < 500; ++i) {
            htmlStringBuilder.append("<p> HTML Line No. ");
            htmlStringBuilder.append(i);
            htmlStringBuilder.append(" </p>");
        }
        htmlStringBuilder.append("</body></html>");
        return htmlStringBuilder.toString();
    }

    private Scene createScene() {
        webView = new WebView();
        final WebEngine webEngine = webView.getEngine();
        Text instructionsText = new Text(instructions);
        instructionsText.setWrappingWidth(550);
        Label htmlBeginLabel = new Label("Html Content below:");
        htmlBeginLabel.setMinHeight(NAVI_BAR_MIN_DIMENSION);
        htmlBeginLabel.setStyle(buttonStyle);
        final Button printButton = new Button("Print");
        printButton.setStyle(buttonStyle);
        printButton.setOnAction((e) -> {
            runTest();
        });
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        final HBox naviBar = new HBox();
        naviBar.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
        naviBar.getChildren().addAll(htmlBeginLabel, region, printButton);
        naviBar.setAlignment(Pos.CENTER);
        naviBar.setPadding(new Insets(PADDING_VALUE));
        bottomMessageLabel = new Label();
        final VBox root = new VBox();
        root.getChildren().addAll(instructionsText, naviBar, webView, bottomMessageLabel);
        VBox.setVgrow(webView, Priority.ALWAYS);
        String htmlContent = createHtmlPage();
        webEngine.loadContent(htmlContent);
        scene = new Scene(root);
        return scene;
    }

    private void setMessage(String msg) {
        bottomMessageLabel.setText(msg);
        System.out.println(msg);
    }
    private void runTest() {
        setMessage("START OF PRINT JOB");
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            Window window = webView.getScene() != null ? webView.getScene().getWindow() : null;
            if (job.showPageSetupDialog(window)) {
                if (job.showPrintDialog(window)) {
                    webView.getEngine().print(job);
                    job.endJob();
                }
            }
        }
        setMessage("END OF PRINT JOB");
    }
}
