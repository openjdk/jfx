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

import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

// Draws text cascades using black-on-white, white-on-black, and white-on-blue.
//
// In each block the left column is LCD text, the right is grayscale.
//
// The first parameter (optional) is the name of the font to use. Defaults to System.
// The second parameter (optional) is a description added at the bottom. This is
// useful to ensure descriptive text is included in screenshots.
public class TextCascade extends Application {
    public static void main(String[] args) {
        launch(TextCascade.class, args);
    }

    private Text createSample(Font font, String sampleText, Color textColor, boolean lcdSmoothing) {
        var text = new Text(sampleText);
        text.setFont(font);
        text.setFill(textColor);
        if (lcdSmoothing) {
            text.setFontSmoothingType(FontSmoothingType.LCD);
        } else {
            text.setFontSmoothingType(FontSmoothingType.GRAY);
        }
        return text;
    }

    private Parent createCascade(String fontFamily, Color textColor, Color backgroundColor, boolean lcd) {
        var text = "Documents   \u59cb\u3081\u308b \u26AB";

        var vbox = new VBox();
        vbox.setSpacing(4);
        vbox.setBackground(new Background(new BackgroundFill(backgroundColor, null, null)));
        vbox.setPadding(new Insets(12, 12, 12, 12));
        for (double size = 6.0; size <= 14.0; size += 0.5) {
            var font = Font.font(fontFamily, FontWeight.NORMAL, size);
            var sample = createSample(font, text, textColor, lcd);
            vbox.getChildren().add(sample);
        }
        return vbox;
    }

    private Parent createSampleBlock(String fontFamily, Color textColor, Color backgroundColor) {
        var grid1 = createCascade(fontFamily, textColor, backgroundColor, true);
        var grid2 = createCascade(fontFamily, textColor, backgroundColor, false);
        return new HBox(grid1, grid2);
    }

    @Override
    public void start(Stage stage) throws Exception {

        String fontFamily = "System";
        String extraDescription = "";

        final Parameters params = getParameters();
        final List<String> unnamed = params.getUnnamed();
        if (!unnamed.isEmpty()) {
            fontFamily = unnamed.get(0);
            if (unnamed.size() > 1) {
                extraDescription = unnamed.get(1);
            }
        }

        var blackOnWhite = createSampleBlock(fontFamily, Color.BLACK, Color.WHITE);
        var whiteOnBlack = createSampleBlock(fontFamily, Color.WHITE, Color.BLACK);
        // The same blue used in popup menus
        var blue = new Color(0.0, 0.59, 0.79, 1.0);
        var whiteOnLightBlue = createSampleBlock(fontFamily, Color.WHITE, blue);

        var pane = new BorderPane();
        pane.setCenter(new HBox(blackOnWhite, whiteOnBlack, whiteOnLightBlue));

        var font = Font.font(fontFamily, FontWeight.NORMAL, 10);
        var fullDescription = font.getName() + " 6pt-14pt  " + System.getProperty("os.name");
        if (!extraDescription.isEmpty()) {
            fullDescription += ("  " + extraDescription);
        }

        var label = new Text(fullDescription);
        label.setFont(Font.getDefault());
        label.setFontSmoothingType(FontSmoothingType.LCD);
        label.setFill(Color.DARKRED);
        var labelBox = new HBox(label);
        labelBox.setPadding(new Insets(12, 12, 12, 12));
        pane.setBottom(labelBox);

        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
