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

package test.robot.javafx.scene;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import test.robot.testharness.RobotTestBase;


public class TextFieldCaretVisibilityTest extends RobotTestBase {
    private static final double MARGIN = 2.0;

    private static final double[] TEST_WIDTHS = { 600, 50 };

    private static final String LONG_LTR_TEXT = "ABCDEFGHIJKLMNO 0123456789";
    private static final String LONG_RTL_TEXT =
            "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5\u05D6\u05D7\u05D8\u05D9\u05DB\u05DC"
            + "\u05DE\u05E0\u05E1\u05E2\u05E4\u05E6\u05E7\u05E8\u05E9\u05EA";
    private static final String LONG_MIXED_TEXT = "abc \u05D0\u05D1\u05D2 123 \u05D3\u05D4\u05D5 xyz";

    private static final List<String> TEXTS = List.of(LONG_LTR_TEXT, LONG_RTL_TEXT, LONG_MIXED_TEXT);
    private static final List<OrientationCase> ORIENTATIONS = List.of(
            new OrientationCase(NodeOrientation.LEFT_TO_RIGHT, NodeOrientation.LEFT_TO_RIGHT),
            new OrientationCase(NodeOrientation.RIGHT_TO_LEFT, NodeOrientation.LEFT_TO_RIGHT),
            new OrientationCase(NodeOrientation.INHERIT, NodeOrientation.RIGHT_TO_LEFT)
    );

    @Test
    public void testNoDeadSpaceAfterResize() {
        for (OrientationCase orientation : ORIENTATIONS) {
            runAndWait(() -> contentPane.setNodeOrientation(orientation.parentOrientation));
            for (String text : TEXTS) {
                TextField field = new TextField(text);
                runAndWait(() -> {
                    field.setFont(Font.font("System", 18));
                    field.setNodeOrientation(orientation.fieldOrientation);
                });
                setContent(new HBox(field));
                runAndWait(field::requestFocus);
                waitForIdle();

                for (double width : TEST_WIDTHS) {
                    runAndWait(() -> field.setPrefWidth(width));
                    waitForIdle();

                    runAndWait(() -> field.positionCaret(0));
                    waitForIdle();
                    runAndWait(() -> {
                        assertCaretVisible(field, "start, width=" + width);
                        assertNoDeadSpace(field, "start, width=" + width);
                    });

                    runAndWait(() -> field.positionCaret(text.length()));
                    waitForIdle();
                    runAndWait(() -> {
                        assertCaretVisible(field, "end, width=" + width);
                        assertNoDeadSpace(field, "end, width=" + width);
                    });
                }
            }
        }
    }

    private void assertCaretVisible(TextField field, String context) {
        Bounds caretBounds = getCaretBoundsInScene(field);
        Bounds clipBounds = getTextClipBoundsInScene(field);
        assertTrue(caretBounds.getMinX() >= clipBounds.getMinX() - MARGIN,
                "caret left edge outside clip (" + context + "):"
                + " caret.minX=" + caretBounds.getMinX()
                + " clip.minX=" + clipBounds.getMinX());
        assertTrue(caretBounds.getMaxX() <= clipBounds.getMaxX() + MARGIN,
                "caret right edge outside clip (" + context + "):"
                + " caret.maxX=" + caretBounds.getMaxX()
                + " clip.maxX=" + clipBounds.getMaxX());
    }


    private void assertNoDeadSpace(TextField field, String context) {
        Text textNode = getTextNode(field);
        Parent textGroup = textNode.getParent();
        Node clip = textGroup.getClip();

        double textWidth = textNode.getLayoutBounds().getWidth();
        double clipWidth = clip != null
                ? clip.getLayoutBounds().getWidth()
                : textGroup.getLayoutBounds().getWidth();

        if (textWidth <= clipWidth + MARGIN) {
            return;
        }

        Bounds textBoundsInScene = textNode.localToScene(textNode.getLayoutBounds());
        Bounds clipBoundsInScene = getTextClipBoundsInScene(field);

        assertTrue(textBoundsInScene.getMinX() <= clipBoundsInScene.getMinX() + MARGIN,
                "dead space at leading edge (" + context + "):"
                + " text.minX=" + textBoundsInScene.getMinX()
                + " clip.minX=" + clipBoundsInScene.getMinX());
        assertTrue(textBoundsInScene.getMaxX() >= clipBoundsInScene.getMaxX() - MARGIN,
                "dead space at trailing edge (" + context + "):"
                + " text.maxX=" + textBoundsInScene.getMaxX()
                + " clip.maxX=" + clipBoundsInScene.getMaxX());
    }

    private Bounds getCaretBoundsInScene(TextField field) {
        Path caretPath = getCaretPath(field);
        assertNotNull(caretPath, "caret path not found");
        return caretPath.localToScene(caretPath.getLayoutBounds());
    }

    private Bounds getTextClipBoundsInScene(TextField field) {
        Text textNode = getTextNode(field);
        Parent textGroup = textNode.getParent();
        Node clip = textGroup.getClip();
        if (clip != null) {
            return textGroup.localToScene(clip.getLayoutBounds());
        }
        return textGroup.localToScene(textGroup.getLayoutBounds());
    }

    private Text getTextNode(TextField field) {
        Set<Node> nodes = field.lookupAll(".text");
        assertTrue(nodes.size() == 1, "expected a single text node, found " + nodes.size());
        return (Text) nodes.iterator().next();
    }

    private Path getCaretPath(TextField field) {
        Parent textGroup = getTextNode(field).getParent();
        for (Node child : textGroup.getChildrenUnmodifiable()) {
            if (child instanceof Group group && group.getChildren().size() == 1
                    && group.getChildren().get(0) instanceof Path path) {
                return path;
            }
        }
        return null;
    }

    private static final class OrientationCase {
        final NodeOrientation fieldOrientation;
        final NodeOrientation parentOrientation;

        OrientationCase(NodeOrientation fieldOrientation, NodeOrientation parentOrientation) {
            this.fieldOrientation = fieldOrientation;
            this.parentOrientation = parentOrientation;
        }
    }
}
