/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleManager;
import javafx.stage.Stage;
import com.sun.javafx.tk.Toolkit;
import java.io.IOException;
import javafx.css.CssParser;
import javafx.css.PseudoClass;
import javafx.css.Stylesheet;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class CssStyleHelperTest {

    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before
    public void setup() {
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);

        StyleManager sm = StyleManager.getInstance();
        sm.userAgentStylesheetContainers.clear();
        sm.platformUserAgentStylesheetContainers.clear();
        sm.stylesheetContainerMap.clear();
        sm.cacheContainerMap.clear();
        sm.hasDefaultUserAgentStylesheet = false;
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
        assertNull(E.getFont().getStyle());

        B.getChildren().add(D); //move D
        Toolkit.getToolkit().firePulse();
        assertEquals("Italic", C.getFont().getStyle());
        assertNull(D.getFont().getStyle());
        assertNull(E.getFont().getStyle());
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
        assertNull(E.getFont().getStyle());

        A.getChildren().remove(D); //move D
        Toolkit.getToolkit().firePulse();
        B.getChildren().add(D);
        Toolkit.getToolkit().firePulse();

        assertEquals("Italic", C.getFont().getStyle());
        assertNull(D.getFont().getStyle());
        assertNull(E.getFont().getStyle());
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

        assertNull(C.getFont().getStyle());
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

        assertNull(C.getFont().getStyle());
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

        assertNull(C.getFont().getStyle());
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
}
