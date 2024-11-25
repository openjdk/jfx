/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PlatformPreferencesChangedTest extends Application {

    @Override
    public void start(Stage stage) {
        var passButton = new Button("Pass");
        passButton.setOnAction(e -> Platform.exit());

        var failButton = new Button("Fail");
        failButton.setOnAction(e -> {
            Platform.exit();
            throw new AssertionError("Platform preferences were not correctly reported");
        });

        var textArea = new TextArea();
        textArea.setEditable(false);

        var clearButton = new Button("Clear Log");
        clearButton.setOnAction(e -> textArea.setText(""));

        var backgroundColorLabel = new Label();
        var foregroundColorLabel = new Label();
        var accentColorLabel = new Label();
        var colorSchemeLabel = new Label();
        var reducedMotionLabel = new Label();
        var reducedTransparencyLabel = new Label();

        Runnable updateColorProperties = () -> {
            var preferences = Platform.getPreferences();
            backgroundColorLabel.setText(preferences.getBackgroundColor().toString());
            foregroundColorLabel.setText(preferences.getForegroundColor().toString());
            accentColorLabel.setText(preferences.getAccentColor().toString());
            colorSchemeLabel.setText(preferences.getColorScheme().toString());
            reducedMotionLabel.setText(Boolean.toString(preferences.isReducedMotion()));
            reducedTransparencyLabel.setText(Boolean.toString(preferences.isReducedTransparency()));
        };

        var box = new VBox();
        box.setSpacing(20);
        box.getChildren().add(new VBox(10,
            new VBox(
                new Label("1. On a supported platform, change any of the platform preferences."),
                new Label("    See javafx.application.Platform.Preferences for a list of supported platforms.")),
            new VBox(
                new Label("2. Observe whether the changed preferences are reported in the log below."),
                new Label("    Added or removed preferences are marked with a plus or minus sign.")),
            new VBox(
                new Label("3. Check whether the following computed properties reflect the reported preferences:"),
                new HBox(new BoldLabel("    backgroundColor: "), backgroundColorLabel),
                new HBox(new BoldLabel("    foregroundColor: "), foregroundColorLabel),
                new HBox(new BoldLabel("    accentColor: "), accentColorLabel),
                new HBox(new BoldLabel("    colorScheme: "), colorSchemeLabel),
                new HBox(new BoldLabel("    reducedMotion: "), reducedMotionLabel),
                new HBox(new BoldLabel("    reducedTransparency: "), reducedTransparencyLabel)),
            new Label("4. Click \"Pass\" if the changes were correctly reported, otherwise click \"Fail\"."),
            new HBox(5, passButton, failButton, clearButton)
        ));

        var root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setTop(box);
        root.setCenter(textArea);
        BorderPane.setMargin(textArea, new Insets(20, 0, 0, 0));

        appendText(textArea, "preferences: " + formatPrefs(Platform.getPreferences().entrySet()));
        updateColorProperties.run();

        Platform.getPreferences().addListener((InvalidationListener)observable -> {
            appendText(textArea, "\r\nchanged:");
            updateColorProperties.run();
        });

        Platform.getPreferences().addListener((MapChangeListener<String, Object>)change -> {
            if (change.wasRemoved() && change.wasAdded()) {
                appendText(textArea, "\t" + formatEntry(change.getKey(), change.getValueAdded()));
            } else if (change.wasRemoved()) {
                appendText(textArea, "\t-" + change.getKey());
            } else {
                appendText(textArea, "\t+" + formatEntry(change.getKey(), change.getValueAdded()));
            }
        });

        stage.setScene(new Scene(root));
        stage.show();
    }

    private void appendText(TextArea textArea, String text) {
        double scrollTop = textArea.getScrollTop();
        textArea.setText(textArea.getText() + text + "\r\n");
        textArea.setScrollTop(scrollTop);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static String formatPrefs(Set<Map.Entry<String, Object>> prefs) {
        return "\r\n\t" + prefs.stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> formatEntry(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("\r\n\t"));
    }

    private static String formatEntry(String key, Object value) {
        if (value instanceof Object[] array) {
            return key + "=" + Arrays.toString(array);
        }

        return key + "=" + value;
    }

    private static class BoldLabel extends Label {
        BoldLabel(String text) {
            super(text);
            setStyle("-fx-font-weight: bold");
        }
    }
}
