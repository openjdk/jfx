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

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CssTransitionsTest extends Application {

    @Override
    public void start(Stage stage) {
        var tab1 = new Tab("transition", new ScrollPane(createTransitionTab()));
        var tab2 = new Tab("transition-delay", new ScrollPane(createTransitionDelayTab()));
        var tab3 = new Tab("transition-timing-function", new ScrollPane(createTransitionTimingFunctionTab()));
        var tab4 = new Tab("backgrounds", new ScrollPane(createBackgroundTransitionsTab()));
        var tab5 = new Tab("borders", new ScrollPane(createBorderTransitionsTab()));

        var tabPane = new TabPane(tab1, tab2, tab3, tab4, tab5);
        stage.setScene(new Scene(tabPane));
        stage.setTitle("CSS Transitions");
        stage.show();
    }

    private Region createTransitionTab() {
        return createContent("""
            .rect {
              -fx-min-width: 100;
              -fx-min-height: 100;
              -fx-background-color: red;
              transition: -fx-min-width 2s, -fx-min-height 4s;
            }

            .rect:hover {
              -fx-min-width: 300;
              -fx-min-height: 300;
            }
            """,
            new RectInfo(".rect", ".rect"));
    }

    private Region createTransitionDelayTab() {
        return createContent("""
                .rect {
                  -fx-min-width: 100;
                  -fx-min-height: 100;
                  -fx-background-color: red;
                  transition-property: -fx-min-width;
                  transition-duration: 3s;
                  transition-delay: 1s;
                }

                .rect:hover {
                  -fx-min-width: 300;
                }
                """,
                new RectInfo(".rect", ".rect"));
    }

    private Region createTransitionTimingFunctionTab() {
        return createContent("""
            .rect {
              -fx-min-width: 100;
              -fx-min-height: 50;
              -fx-background-color: red;
              transition-property: -fx-min-width;
              transition-duration: 2s;
            }

            .rect:hover {
              -fx-min-width: 300;
            }

            #rect1 { transition-timing-function: linear; }
            #rect2 { transition-timing-function: ease; }
            #rect3 { transition-timing-function: ease-in; }
            #rect4 { transition-timing-function: ease-out; }
            #rect5 { transition-timing-function: ease-in-out; }
            """,
            new RectInfo("#rect1", "rect1"),
            new RectInfo("#rect2", "rect2"),
            new RectInfo("#rect3", "rect3"),
            new RectInfo("#rect4", "rect4"),
            new RectInfo("#rect5", "rect5"));
    }

    private Region createBackgroundTransitionsTab() {
        return createContent("""
            .rect {
              -fx-min-width: 100;
              -fx-min-height: 50;
              transition: all 2s;
            }

            .rect:hover {
              -fx-min-width: 300;
            }

            // Color transition
            #rect1 { -fx-background-color: red; }
            #rect1:hover { -fx-background-color: blue; }

            // LinearGradient transition
            #rect2 { -fx-background-color: linear-gradient(to right, red, blue); }
            #rect2:hover { -fx-background-color: linear-gradient(to right, purple, yellow); }

            // LinearGradient transition with different number of stops
            #rect3 { -fx-background-color: linear-gradient(to right, red, green); }
            #rect3:hover { -fx-background-color: linear-gradient(to right, red, yellow, blue); }

            // RadialGradient transition with different number of stops
            #rect4 { -fx-background-color: radial-gradient(radius 100%, red, green); }
            #rect4:hover { -fx-background-color: radial-gradient(radius 100%, red, yellow, blue); }
            """,
            new RectInfo("#rect1", "rect1"),
            new RectInfo("#rect2", "rect2"),
            new RectInfo("#rect3", "rect3"),
            new RectInfo("#rect4", "rect4"));
    }

    private Region createBorderTransitionsTab() {
        return createContent("""
            .rect {
              -fx-min-width: 100;
              -fx-min-height: 50;
              -fx-border-width: 10;
              transition: all 2s;
            }

            .rect:hover {
              -fx-min-width: 300;
            }

            // Color transition
            #rect1 { -fx-border-color: red; }
            #rect1:hover { -fx-border-color: blue; }

            // LinearGradient transition
            #rect2 { -fx-border-color: linear-gradient(to right, red, blue); }
            #rect2:hover { -fx-border-color: linear-gradient(to right, purple, yellow); }

            // LinearGradient transition with different number of stops
            #rect3 { -fx-border-color: linear-gradient(to right, red, green); }
            #rect3:hover { -fx-border-color: linear-gradient(to right, red, yellow, blue); }

            // RadialGradient transition with different number of stops
            #rect4 { -fx-border-color: radial-gradient(radius 100%, red, green); }
            #rect4:hover { -fx-border-color: radial-gradient(radius 100%, red, yellow, blue); }
            """,
            new RectInfo("#rect1", "rect1"),
            new RectInfo("#rect2", "rect2"),
            new RectInfo("#rect3", "rect3"),
            new RectInfo("#rect4", "rect4"));
    }

    private Region createContent(String stylesheet, RectInfo... rects) {
        var text = new Label(stylesheet);
        text.setFont(Font.font("monospace", FontWeight.BOLD, 12));
        text.setTextFill(Color.rgb(50, 50, 50));

        var container = new VBox(10, text);
        var uri = "data:charset=utf-8;base64," + Base64.getEncoder().encodeToString(
            stylesheet.getBytes(StandardCharsets.UTF_8));;
        container.getStylesheets().add(uri);
        container.setPadding(new Insets(10));

        for (RectInfo rectInfo : rects) {
            var rect = new Label(rectInfo.caption());
            rect.setId(rectInfo.id());
            rect.setAlignment(Pos.TOP_LEFT);
            rect.getStyleClass().add("rect");
            rect.setPadding(new Insets(5));
            container.getChildren().add(rect);
        }

        return container;
    }

    private record RectInfo(String caption, String id) {}

    public static void main(String[] args) {
        Application.launch(args);
    }

}
