/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import com.sun.javafx.tk.Toolkit;
import javafx.application.ColorScheme;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class Scene_preferences_Test {

    private Stage stage;
    private Scene scene;
    private Rectangle rect;

    @BeforeEach
    void setup() {
        rect = new Rectangle();
        rect.setId("rect");
        scene = new Scene(new Group(rect));
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    void teardown() {
        stage.close();
    }

    record TestRun(String mediaQuery,
                   Consumer<Scene.Preferences> state1,
                   Consumer<Scene.Preferences> state2) {}

    static Stream<TestRun> changedScenePreferenceReappliesCSS_testRuns() {
        return Stream.of(
            new TestRun(
                "prefers-color-scheme: dark",
                prefs -> prefs.setColorScheme(ColorScheme.LIGHT),
                prefs -> prefs.setColorScheme(ColorScheme.DARK)),
            new TestRun(
                "prefers-reduced-motion",
                prefs -> prefs.setReducedMotion(false),
                prefs -> prefs.setReducedMotion(true)),
            new TestRun(
                "prefers-reduced-transparency",
                prefs -> prefs.setReducedTransparency(false),
                prefs -> prefs.setReducedTransparency(true)),
            new TestRun(
                "prefers-reduced-data",
                prefs -> prefs.setReducedData(false),
                prefs -> prefs.setReducedData(true)),
            new TestRun(
                "-fx-prefers-persistent-scrollbars",
                prefs -> prefs.setPersistentScrollBars(false),
                prefs -> prefs.setPersistentScrollBars(true))
        );
    }

    @ParameterizedTest
    @MethodSource("changedScenePreferenceReappliesCSS_testRuns")
    void changedScenePreferenceReappliesCSS(TestRun testRun) {
        scene.getStylesheets().add("data:base64," + Base64.getEncoder().encodeToString("""
            #rect { -fx-fill: red; }
            @media (%s) {
                #rect { -fx-fill: green; }
            }
            """.formatted(testRun.mediaQuery).getBytes(StandardCharsets.UTF_8)));

        testRun.state1.accept(scene.getPreferences());
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, rect.getFill());

        testRun.state2.accept(scene.getPreferences());
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, rect.getFill());
    }
}
