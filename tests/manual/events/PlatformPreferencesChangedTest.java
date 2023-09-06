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
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class PlatformPreferencesChangedTest extends Application {

    private Map<String, Object> cachedPreferences;

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

        var box = new VBox();
        box.setSpacing(20);
        box.getChildren().add(new VBox(10,
            new VBox(
                new Label("1. On a supported platform, change any of the platform preferences."),
                new Label("    See javafx.application.Platform.Preferences for a list of supported platforms.")),
            new Label("2. Observe whether the changed preferences are reported in the log below."),
            new Label("3. Click \"Pass\" if the changes were correctly reported, otherwise click \"Fail\"."),
            new HBox(5, passButton, failButton, clearButton)
        ));

        var root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setTop(box);
        root.setCenter(textArea);
        BorderPane.setMargin(textArea, new Insets(20, 0, 0, 0));

        cachedPreferences = new HashMap<>(Platform.getPreferences());
        textArea.setText("preferences = " + formatPrefs(cachedPreferences.entrySet()));

        Platform.getPreferences().addListener(
            (InvalidationListener)observable -> {
                Set<Map.Entry<String, Object>> changed = Platform.getPreferences().entrySet().stream()
                        .filter(entry -> !Objects.equals(entry.getValue(), cachedPreferences.get(entry.getKey())))
                        .collect(Collectors.toSet());

                double scrollTop = textArea.getScrollTop();
                textArea.setText(textArea.getText() + "changed = " + formatPrefs(changed));
                textArea.setScrollTop(scrollTop);

                cachedPreferences = new HashMap<>(Platform.getPreferences());
            });

        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static String formatPrefs(Set<Map.Entry<String, Object>> prefs) {
        String entries = prefs.stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    if (entry.getValue() instanceof Object[] array) {
                        return entry.getKey() + "=" + Arrays.toString(array);
                    }
                    return entry.getKey() + "=" + entry.getValue();
                })
                .collect(Collectors.joining("\r\n\t"));

        return "{\r\n\t" + entries + "\r\n}\r\n";
    }
}
