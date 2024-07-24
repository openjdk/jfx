/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.sun.javafx.css.StyleManager;
import javafx.stage.Stage;
import com.sun.javafx.tk.Toolkit;
import javafx.css.CssParser;
import javafx.css.PseudoClass;
import javafx.css.Stylesheet;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class CssStyleHelperTest {

    private Scene scene;
    private Stage stage;
    private StackPane root;

    private static void resetStyleManager() {
        StyleManager sm = StyleManager.getInstance();
        sm.userAgentStylesheetContainers.clear();
        sm.platformUserAgentStylesheetContainers.clear();
        sm.stylesheetContainerMap.clear();
        sm.cacheContainerMap.clear();
        sm.hasDefaultUserAgentStylesheet = false;
    }

    @BeforeEach
    public void setup() {
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        resetStyleManager();
    }

    @AfterAll
    public static void cleanupOnce() {
        resetStyleManager();
    }

    @Test
    public void movingNodeToDifferentBranchGetsNewFontStyleTest() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //Where C and D are Texts. Set the font style on A and a different font style on B.
        //C and D should pick up the font style of A. Then move D to B and see if it still has A's
        //font style.
        stylesheet = new CssParser().parse(
                "movingNodeToDifferentBranchGetsNewFontStyleTest",
                ".root {}\n"
                + ".a { -fx-font-style: italic; }\n"
                + ".b { -fx-font-family: normal; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane B = new Pane();
        B.getStyleClass().add("b");
        Text C = new Text("C");
        Text D = new Text("D");
        Text E = new Text("E");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());
        assertEquals("Italic", D.getFont().getStyle());
        assertEquals("Regular", E.getFont().getStyle());

        B.getChildren().add(D); //move D
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());
        assertEquals("Regular", D.getFont().getStyle());
        assertEquals("Regular", E.getFont().getStyle());
    }

    @Test
    public void testMovedNodeGetsCorrectPseudoClassState() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        stylesheet = new CssParser().parse(
                "testMovedNodeGetsCorrectPseudoClassState",
                ".root {-fx-background-color: green; }\n"
                + ":ps1 :ps2 { -fx-background-color: yellow; }\n"
                + ":ps2 { -fx-background-color: red; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.GREEN, root.backgroundProperty().getValue().getFills().get(0).getFill());

        Pane redPane = new Pane();
        redPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("ps2"), true);
        root.getChildren().add(redPane);
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, redPane.backgroundProperty().getValue().getFills().get(0).getFill());

        Pane parentPane = new Pane();
        parentPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("ps1"), true);
        root.getChildren().add(parentPane);
        Toolkit.getToolkit().firePulse();

        parentPane.getChildren().add(redPane);
        Toolkit.getToolkit().firePulse();
        //changes to yellow after being moved to a different part of the scene graph
        assertEquals(Color.YELLOW, redPane.backgroundProperty().getValue().getFills().get(0).getFill());

        Pane yellowPane = new Pane();
        yellowPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("ps2"), true);
        parentPane.getChildren().add(yellowPane);
        Toolkit.getToolkit().firePulse();
        //when first inserted, should be yellow
        assertEquals(Color.YELLOW, yellowPane.backgroundProperty().getValue().getFills().get(0).getFill());

        root.getChildren().add(yellowPane);
        Toolkit.getToolkit().firePulse();
        //changes to red after being moved to a different part of the scene graph
        assertEquals(Color.RED, yellowPane.backgroundProperty().getValue().getFills().get(0).getFill());

    }

    @Test
    public void removingThenAddingNodeToDifferentBranchGetsNewFontStyleTest() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //Where C and D are Labels. Then I'd set a font style on A and a different font style on B.
        //C and D should pick up the font style of A. Then remove D and readd it to B and see if it still has A's
        //font style.
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToDifferentBranchGetsNewFontStyleTest",
                ".root {}\n"
                + ".a { -fx-font-style: italic; }\n"
                + ".b { -fx-font-style: normal; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane B = new Pane();
        B.getStyleClass().add("b");
        Text C = new Text("C");
        Text D = new Text("D");
        Text E = new Text("E");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());
        assertEquals("Italic", D.getFont().getStyle());
        assertEquals("Regular", E.getFont().getStyle());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertEquals("Italic", C.getFont().getStyle());
        assertEquals("Regular", D.getFont().getStyle());
        assertEquals("Regular", E.getFont().getStyle());
    }

    @Test
    public void removingThenAddingNodeToDifferentBranchGetsIneritableStyle() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //Where C, D and E are Panes. Define a color variable on A and a different color variable on B.
        //C and D should pick up the color variable. Then remove D and readd it to B and see if it still has A's
        //font style.
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToDifferentBranchGetsIneritableStyle",
                ".root {}\n"
                + ".a { col: red; }\n"
                + ".b { col: blue; }\n"
                + ".leaf { -fx-background-color: col}"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane B = new Pane();
        B.getStyleClass().add("b");
        Pane C = new Pane();
        C.getStyleClass().add("leaf");
        Pane D = new Pane();
        D.getStyleClass().add("leaf");
        Pane E = new Pane();
        E.getStyleClass().add("leaf");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.RED, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());
    }

    @Test
    public void removingThenAddingNodeToDifferentBranchGetsPseudoClassStyles() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //Where C,D and E are Panes. Then set a pseudoclass on A and a different pseudoclass on B and add styles
        //that affect their children. C and D should pick up the style of A. Then remove D and readd it to B and
        //see if it still has A's
        //style.
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToDifferentBranchGetsPseudoClassStyles",
                ".root {}\n"
                + ":ps1 .leaf { -fx-background-color: red; }\n"
                + ":ps2 .leaf { -fx-background-color: blue; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.pseudoClassStateChanged(PseudoClass.getPseudoClass("ps1"), true);
        Pane B = new Pane();
        B.pseudoClassStateChanged(PseudoClass.getPseudoClass("ps2"), true);
        Pane C = new Pane();
        C.getStyleClass().add("leaf");
        Pane D = new Pane();
        D.getStyleClass().add("leaf");
        Pane E = new Pane();
        E.getStyleClass().add("leaf");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.RED, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());
    }

    @Test
    public void removingThenAddingNodeToDifferentBranchGetsCorrectClassStyles() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //Where C, D and E are Panes. Add a style on A and a different style on B which affect their descendents.
        //C and D should pick up the style of A. Then remove D and readd it to B and see if it still has A's
        //style.
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToDifferentBranchGetsCorrectClassStyles",
                ".root {}\n"
                + ".a .leaf { -fx-background-color: red; }\n"
                + ".b .leaf { -fx-background-color: blue; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane B = new Pane();
        B.getStyleClass().add("b");
        Pane C = new Pane();
        C.getStyleClass().add("leaf");
        Pane D = new Pane();
        D.getStyleClass().add("leaf");
        Pane E = new Pane();
        E.getStyleClass().add("leaf");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.RED, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());

        E.getChildren().add(A);
        Toolkit.getToolkit().firePulse();

        assertEquals(Color.BLUE, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());
    }

    @Test
    public void removingThenAddingNodeToDifferentBranchGetsCorrectInheritedValue() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //Where C and D and E are panes. Set the visibility on A and a different visibility on B.
        //C and D should pick up the visibility style of A by inheriting it. Then remove D and readd
        //it to B and see if it still has A's visibility.
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToDifferentBranchGetsCorrectInheritedValue",
                ".root {}\n"
                + ".a { visibility: collapse; }\n"
                + ".b { visibility: visible; }\n"
                + ".leaf { visibility: inherit;}"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane B = new Pane();
        B.getStyleClass().add("b");
        Pane C = new Pane();
        C.getStyleClass().add("leaf");
        Pane D = new Pane();
        D.getStyleClass().add("leaf");
        Pane E = new Pane();
        E.getStyleClass().add("leaf");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertFalse(A.isVisible());
        assertFalse(C.isVisible());
        assertFalse(D.isVisible());
        assertTrue(E.isVisible());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertFalse(A.isVisible());
        assertFalse(C.isVisible());
        assertTrue(D.isVisible());
        assertTrue(E.isVisible());
    }

    @Test
    public void removingThenAddingNodeToSameBranchAfterClassChangesGetsInheritedValue() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //R
        //.
        //A
        //.
        //C
        //Remove C from the scene graph, change the class of a, add C back and see if it has the correct state
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToSameBranchAfterClassChangesGetsInheritedValue",
                ".root {}\n"
                + ".a { visibility: collapse; }\n"
                + ".b { visibility: visible; }\n"
                + ".c { visibility: inherit;}"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane C = new Pane();
        C.getStyleClass().add("c");
        root.getChildren().addAll(A);
        A.getChildren().addAll(C);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertFalse(A.isVisible());
        assertFalse(C.isVisible());

        A.getChildren().remove(C);
        Toolkit.getToolkit().firePulse();
        A.getStyleClass().setAll("b");
        Toolkit.getToolkit().firePulse();

        A.getChildren().add(C);
        assertTrue(A.isVisible());
        assertTrue(C.isVisible());
    }

    @Test
    public void removingThenAddingNodeToSameBranchAfterClassChangesGetsCorrectFont() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //R
        //.
        //A
        //.
        //C
        //Remove C from the scene graph, change the class of A, add C back and see if it has the correct state
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToSameBranchAfterClassChangesGetsCorrectFont",
                ".root {}\n"
                + ".a { -fx-font-style: italic; }\n"
                + ".b { -fx-font-style: normal; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Text C = new Text("C");
        root.getChildren().addAll(A);
        A.getChildren().addAll(C);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());

        A.getChildren().remove(C);
        Toolkit.getToolkit().firePulse();

        A.getStyleClass().setAll("b");
        A.getChildren().add(C);
        Toolkit.getToolkit().firePulse();

        assertEquals("Regular", C.getFont().getStyle());
    }

    @Test
    public void removingThenAddingNodeToSameBranchAfterPseudoClassChangesGetsCorrectFont() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //R
        //.
        //A
        //.
        //C
        //Remove C from the scene graph, change the class of A, add C back and see if it has the correct state
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToSameBranchAfterPseudoClassChangesGetsCorrectFont",
                ".root {}\n"
                + ".a { -fx-font-style: italic; }\n"
                + ".a:normal { -fx-font-style: normal; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Text C = new Text("C");
        root.getChildren().addAll(A);
        A.getChildren().addAll(C);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());

        A.getChildren().remove(C);
        Toolkit.getToolkit().firePulse();

        A.pseudoClassStateChanged(PseudoClass.getPseudoClass("normal"), true);
        A.getChildren().add(C);
        Toolkit.getToolkit().firePulse();

        assertEquals("Regular", C.getFont().getStyle());
    }

    @Test
    public void removingThenAddingNodeToSameBranchAfterInlineStyleChangesGetsCorrectFont() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //R
        //.
        //A
        //.
        //C
        //Remove C from the scene graph, change the inline style of A, add C back and see if it has the correct state
        stylesheet = new CssParser().parse(
                "removingThenAddingNodeToSameBranchAfterPseudoClassChangesGetsCorrectFont",
                ".root {}\n"
                + ".a { -fx-font-style: italic; }\n"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Text C = new Text("C");
        root.getChildren().addAll(A);
        A.getChildren().addAll(C);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());

        A.getChildren().remove(C);
        Toolkit.getToolkit().firePulse();

        A.setStyle("-fx-font-style: normal");
        A.getChildren().add(C);
        Toolkit.getToolkit().firePulse();

        assertEquals("Regular", C.getFont().getStyle());
    }

    @Test
    public void movingBranchToDifferentBranchGetsNewCssVariableTest() throws IOException {
        Stylesheet stylesheet = null;
        root.getStyleClass().add("root");
        //               R
        //         .-----+-----.
        //         A           B
        //    .----+----.      .
        //    C         D      E
        //              .
        //              F
        //move D and F together under B, both should get the new variable for background color
        stylesheet = new CssParser().parse(
                "movingBranchToDifferentBranchGetsNewCssVariableTest",
                ".root {}\n"
                + ".a { col: red; }\n"
                + ".b { col: blue; }\n"
                + ".child { -fx-background-color: col}"
        );
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane A = new Pane();
        A.getStyleClass().add("a");
        Pane B = new Pane();
        B.getStyleClass().add("b");
        Pane C = new Pane();
        C.getStyleClass().add("child");
        Pane D = new Pane();
        D.getStyleClass().add("child");
        Pane E = new Pane();
        E.getStyleClass().add("child");
        Pane F = new Pane();
        F.getStyleClass().add("child");
        root.getChildren().addAll(A, B);
        A.getChildren().addAll(C, D);
        B.getChildren().add(E);
        D.getChildren().add(F);
        stage.show();
        Toolkit.getToolkit().firePulse();
        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.RED, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.RED, F.backgroundProperty().getValue().getFills().get(0).getFill());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertEquals(Color.RED, C.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, D.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, E.backgroundProperty().getValue().getFills().get(0).getFill());
        assertEquals(Color.BLUE, F.backgroundProperty().getValue().getFills().get(0).getFill());
    }

    @Test
    public void initialNodeWithUserSetValueShouldNotResetValuesOnOtherNodesWithoutOverriddenValue() throws IOException {
        Stylesheet stylesheet = new CssParser().parse(
            "initialNodeWithUserSetValueShouldNotResetValuesOnOtherNodesWithoutOverridenValue",
            """
                .pane {
                    -fx-padding: 4;
                }
            """
        );

        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Pane a = new Pane();
        Pane b = new Pane();

        a.getStyleClass().add("pane");
        a.setPadding(new Insets(10));

        b.getStyleClass().add("pane");

        root.getChildren().addAll(a, b);

        stage.show();
        Toolkit.getToolkit().firePulse();

        assertEquals(new Insets(10), a.getPadding());
        assertEquals(new Insets(4), b.getPadding());

        // When changing a to focused, this will be the first time that the padding
        // property is seen in the state focused. A new cache entry is created, which
        // despite the padding value being overridden, should still add the padding value
        // that comes from the USER_AGENT stylesheet as this entry is shared with
        // all other nodes with the same property, with the same state at the same nesting level.
        a.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
        Toolkit.getToolkit().firePulse();

        assertEquals(new Insets(10), a.getPadding());
        assertEquals(new Insets(4), b.getPadding());

        // When changing b to focused, it should not have its padding changed. If b
        // padding does change, then the cache entry was incorrect.
        a.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), false);
        b.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
        Toolkit.getToolkit().firePulse();

        assertEquals(new Insets(10), a.getPadding());
        assertEquals(new Insets(4), b.getPadding());
    }

    private static final String GRAY_STYLESHEET = ".my-pane {-fx-background-color: #808080}";
    private static final String GRAY_INDIRECT_STYLESHEET = ".root {-fx-base: #808080} .my-pane {-fx-background-color: -fx-base}";
    private static final String RED_STYLESHEET = ".my-pane {-fx-background-color: red}";
    private static final String RED_INDIRECT_STYLESHEET = ".root {-fx-base: red} .my-pane {-fx-background-color: -fx-base}";
    private static final String FX_BASE_GREEN_STYLESHEET = ".root {-fx-base: green}";
    private static final String FX_BASE_GRAY_STYLESHEET = ".root {-fx-base: #808080}";

    /**
     * All cases will lead to a neutral gray color #808080.
     *
     * UA = USER_AGENT
     */
    enum OverrideCases {
        // User agent styles win when not overridden directly or indirectly:
        UA(GRAY_STYLESHEET, null, null, null),
        INDIRECT_UA(GRAY_INDIRECT_STYLESHEET, null, null, null),

        // Property wins when not directly overridden by author or inline style:
        PROPERTY(null, Color.web("#808080"), null, null),
        PROPERTY_OVERRIDES_UA(RED_STYLESHEET, Color.web("#808080"), null, null),
        PROPERTY_OVERRIDES_INDIRECT_UA(RED_INDIRECT_STYLESHEET, Color.web("#808080"), null, null),

        // Property wins even if indirectly overridden by author or inline style (resolving of a lookup does not change priority of the user agent style):
        PROPERTY_OVERRIDES_UA_VARIABLE_SET_IN_AUTHOR(RED_INDIRECT_STYLESHEET, Color.web("#808080"), FX_BASE_GREEN_STYLESHEET, null),
        PROPERTY_OVERRIDES_UA_VARIABLE_SET_INLINE(RED_INDIRECT_STYLESHEET, Color.web("#808080"), null, "-fx-base: yellow"),

        // Author style wins when not directly overridden by inline style:
        AUTHOR_OVERRIDES_UA(RED_STYLESHEET, null, GRAY_STYLESHEET, null),
        AUTHOR_OVERRIDES_INDIRECT_UA(RED_INDIRECT_STYLESHEET, null, GRAY_STYLESHEET, null),
        AUTHOR_OVERRIDES_PROPERTY(null, Color.BLUE, GRAY_STYLESHEET, null),

        // Author style wins even if indirectly overridden by inline style (resolving of a lookup does not change priority of the user agent style):
        AUTHOR_OVERRIDES_UA_VARIABLE_SET_INLINE(RED_INDIRECT_STYLESHEET, null, GRAY_STYLESHEET, "-fx-base: yellow"),

        // Indirect author styles win when property is not set directly, and there is no direct or indirect override in an inline style:
        AUTHOR_VARIABLE_OVERRIDES_UA_VARIABLE(RED_INDIRECT_STYLESHEET, null, FX_BASE_GRAY_STYLESHEET, null),

        // Direct inline styles always win over anything:
        INLINE_OVERRIDES_UA(RED_STYLESHEET, null, null, "-fx-background-color: #808080"),
        INLINE_OVERRIDES_PROPERTY(null, Color.BLUE, null, "-fx-background-color: #808080"),
        INLINE_OVERRIDES_AUTHOR(null, null, RED_STYLESHEET, "-fx-background-color: #808080"),
        INLINE_OVERRIDES_ALL(RED_STYLESHEET, Color.BLUE, RED_STYLESHEET, "-fx-background-color: #808080"),

        // Indirect inline styles win when not directly overridden by author stylesheet or property:
        INLINE_VARIABLE_OVERRIDES_UA_VARIABLE(RED_INDIRECT_STYLESHEET, null, null, "-fx-base: #808080"),
        INLINE_VARIABLE_OVERRIDES_AUTHOR_VARIABLE(null, null, RED_INDIRECT_STYLESHEET, "-fx-base: #808080"),
        INLINE_VARIABLE_OVERRIDES_ALL(RED_INDIRECT_STYLESHEET, null, FX_BASE_GREEN_STYLESHEET, "-fx-base: #808080");

        private final String userAgentStylesheet;
        private final Color property;
        private final String authorStylesheet;
        private final String inlineStyles;

        OverrideCases(String userAgentStylesheet, Color property, String authorStylesheet, String inlineStyles) {
            this.userAgentStylesheet = userAgentStylesheet;
            this.property = property;
            this.authorStylesheet = authorStylesheet;
            this.inlineStyles = inlineStyles;
        }
    }

    /*
     * Tests various override cases, with direct or indirect (via lookup) overrides. All cases should lead
     * to a neutral gray color.
     */
    @ParameterizedTest
    @EnumSource(OverrideCases.class)
    public void whenAllStylesAndOverridesAreAppliedShouldBeNeutralGray(OverrideCases c) throws IOException {
        StyleManager.getInstance().setDefaultUserAgentStylesheet(new CssParser().parse("userAgentStylSheet", c.userAgentStylesheet));
        Pane pane = new Pane();

        pane.getStyleClass().addAll("root", "my-pane");

        if (c.property != null) {
            pane.setBackground(Background.fill(c.property));
        }
        if (c.authorStylesheet != null) {
            pane.getStylesheets().add(toDataURL(c.authorStylesheet));
        }
        if (c.inlineStyles != null) {
            pane.setStyle(c.inlineStyles);
        }

        root.getChildren().add(pane);

        stage.show();
        Toolkit.getToolkit().firePulse();

        assertEquals(Paint.valueOf("#808080"), pane.getBackground().getFills().get(0).getFill());
    }

    private static String toDataURL(String stylesheet) {
        return "data:text/plain;base64," + Base64.getEncoder().encodeToString(stylesheet.getBytes(StandardCharsets.UTF_8));
    }
}
