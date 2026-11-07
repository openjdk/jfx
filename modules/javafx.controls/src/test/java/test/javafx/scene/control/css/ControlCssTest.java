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

package test.javafx.scene.control.css;

import com.sun.javafx.tk.Toolkit;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests CSS together with Controls and their Skins.
 */
public class ControlCssTest {

    private StageLoader stageLoader;
    private ObservableList<CssParser.ParseError> errors;

    @BeforeEach
    void setUp() {
        errors = CssParser.errorsProperty();
        errors.clear();
    }

    @AfterEach
    void tearDown() {
        errors.clear();
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    /**
     * A relative property (em) must be correctly reset to the initial value when the style is removed.
     */
    @Test
    public void testRelativePropertyIsResetToInitialValue() {
        Label label = new Label("Test");
        label.getStyleClass().add("padded");

        Scene scene = new Scene(new StackPane(label));
        scene.getStylesheets().add(toDataURL("""
                .padded { -fx-padding: 1em; }
                .padded .text { -fx-font-size: 20px; }
                """));
        stageLoader = new StageLoader(scene);

        assertEquals(new Insets(20), label.getPadding());

        label.getStyleClass().remove("padded");
        Toolkit.getToolkit().firePulse();

        assertEquals(Insets.EMPTY, label.getPadding());
    }

    private static String toDataURL(String css) {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }
}
