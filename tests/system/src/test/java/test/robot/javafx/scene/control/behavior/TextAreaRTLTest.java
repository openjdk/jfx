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
package test.robot.javafx.scene.control.behavior;

import java.util.Set;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.util.Util;

/**
 * Tests TextArea RTL/LTR behavior.
 */
@ExtendWith(ScreenshotFailedTestWatcher.class)
public class TextAreaRTLTest extends TextInputBehaviorRobotTest<TextArea> {
    private Color selectionColor;

    public TextAreaRTLTest() {
        super(new TextArea());
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        super.beforeEach();
        // set good initial conditions
        Util.runAndWait(() -> {
            control.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            control.setFont(Font.font("System", 24));
            control.setText(null);
            control.deselect();
            Util.parkCursor(robot);
        });
    }

    @Test
    public void testSelectionLTR() throws Exception {
        execute(
            setText("______\n______"),
            exe(() -> {
                mouseMove(5, 12);
                mousePress();
            }),
            exe(() -> {
                mouseMove(100, 100);
                mouseRelease();
            }),
            checkSelection(0, 13),
            exe(() -> {
                Rect r = charBounds(0);
                // check that caret position at index 0 is near 0,0 and is selected
                Assertions.assertTrue(r.x < 50);
                Assertions.assertTrue(r.y < 50);
                Assertions.assertTrue(isCellSelected(r));
            })
        );
    }

    // TODO the test does not work due to JDK-8189167
    @Disabled("JDK-8189167")
    @Test
    public void testSelectionRTL() throws Exception {
        // class for ease of accessing variables passed between individual Runnables in execute()
        new Runnable() {
            volatile double x;

            @Override
            public void run() {
                execute(
                    exe(() -> {
                        control.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                        control.setText("______\n______");
                    }),
                    PULSE,
                    exe(() -> {
                        int len = control.getText().length();
                        for(int i=0; i<=len; i++) {
                            Rect r = charBounds(i);
                            p("cell[" + i + "]=" + r);
                        }
                        Rect r = charBounds(0);
                        p("cell[0]=" + r);

                        // check that caret position at index 0 is in the right side and not selected
                        Assertions.assertTrue(r.x > (STAGE_WIDTH / 3));
                        Assertions.assertTrue(r.y < 50);
                        Assertions.assertFalse(isCellSelected(r));

                        // start with index 3
                        r = charBounds(3);
                        p("cell[3]=" + r);
                        x = r.x;
                    }),
                    exe(() -> {
                        mouseMove(x, 12);
                        mouseClick();
                        p("selection=" + control.getSelection()); // FIX
                    }),
                    PULSE,
                    "a",
                    exe(() -> {
                        mousePress();
                        mouseMove(x, 100);
                    }),
                    exe(() -> {
                        mouseRelease();
                    }),
                    PULSE,
                    exe(() -> {
                        IndexRange sel = control.getSelection();
                        Assertions.assertTrue(sel.getStart() > 0);
                        int ix = sel.getStart();

                        // check if the first selected cell is indeed highlighted
                        Rect r = charBounds(ix);
                        p(r);
                        Assertions.assertTrue(isCellSelected(r));
                        // but the previous one isn't
                        r = charBounds(ix - 1);
                        p(r);
                        Assertions.assertFalse(isCellSelected(r));
                    })
                );
            }
        }.run();
        //Thread.sleep(100_000); // FIX
    }

    private Rect charBounds(int index) {
        // there should be a method in TextArea to convert between screen coordinates and content indexes
        // similar to what was asked in JDK-8092278
        Set<Node> nodes = control.lookupAll(".text");
        if(nodes.size() != 1) {
            throw new AssertionError("expectng a single Text child with CSS class '.text'");
        }
        Text t = (Text)nodes.iterator().next();
        PathElement[] pe = t.rangeShape(index, index + 1);
        // translate to the control's coordinates
        Point2D p = t.localToScreen(0.0, 0.0);
        p = control.screenToLocal(p);
        return Rect.of(pe, p.getX(), p.getY());
    }

    // TODO maybe extract into a separate @Test, the first to get executed
    private boolean isCellSelected(Rect r) {
        if(selectionColor == null) {
            // initialize the selection color
            TextArea t = new TextArea();
            t.setText("____________________________");
            t.selectAll();
            t.setMinWidth(100);
            t.setMinHeight(100);
            t.setWrapText(true);
            BorderPane pp = new BorderPane(t);
            pp.setManaged(false);

            content.getChildren().add(pp);

            pp.layout();
            t.applyCss();
            pp.applyCss();
            t.requestFocus();

            try {
                WritableImage im = pp.snapshot(null, null);
                selectionColor = im.getPixelReader().getColor(30, 8);
                content.layout();
            } finally {
                content.getChildren().remove(pp);
            }
        }

        control.requestFocus();

        //System.out.println("insets=" + control.getInsets());
        Point2D p = control.localToScreen(r.midX(), r.midY());
        //System.out.println("r=" + r);
        //System.out.println("p=" + p);
        Color c = robot.getPixelColor(p);
        //System.out.println("color=" + c);
        double tolerance = 0.05;
        return
            (Math.abs(selectionColor.getRed() - c.getRed()) < tolerance) &&
            (Math.abs(selectionColor.getGreen() - c.getGreen()) < tolerance) &&
            (Math.abs(selectionColor.getBlue() - c.getBlue()) < tolerance);
    }

    /** Rectangle enveloping a single character */
    static record Rect(double x, double y, double w, double h) {
        public double midX() {
            return x + (w / 2.0);
        }

        public double midY() {
            return y + (h / 2.0);
        }

        public static Rect of(PathElement[] pe, double dx, double dy) {
            double xmin = Double.NaN;
            double xmax = Double.NaN;
            double ymin = Double.NaN;
            double ymax = Double.NaN;

            for (PathElement em: pe) {
                if (em instanceof MoveTo t) {
                    if (Double.isNaN(xmin)) {
                        xmin = t.getX();
                    } else {
                        if (xmin > t.getX()) {
                            xmin = t.getX();
                        }
                    }

                    if (Double.isNaN(xmax)) {
                        xmax = t.getX();
                    } else {
                        if (xmax < t.getX()) {
                            xmax = t.getX();
                        }
                    }

                    if (Double.isNaN(ymin)) {
                        ymin = t.getY();
                    } else {
                        if (ymin > t.getY()) {
                            ymin = t.getY();
                        }
                    }

                    if (Double.isNaN(ymax)) {
                        ymax = t.getY();
                    } else {
                        if (ymax < t.getY()) {
                            ymax = t.getY();
                        }
                    }
                } else if (em instanceof LineTo t) {
                    if (Double.isNaN(xmin)) {
                        xmin = t.getX();
                    } else {
                        if (xmin > t.getX()) {
                            xmin = t.getX();
                        }
                    }

                    if (Double.isNaN(xmax)) {
                        xmax = t.getX();
                    } else {
                        if (xmax < t.getX()) {
                            xmax = t.getX();
                        }
                    }

                    if (Double.isNaN(ymin)) {
                        ymin = t.getY();
                    } else {
                        if (ymin > t.getY()) {
                            ymin = t.getY();
                        }
                    }

                    if (Double.isNaN(ymax)) {
                        ymax = t.getY();
                    } else {
                        if (ymax < t.getY()) {
                            ymax = t.getY();
                        }
                    }
                }
            }

            if (Double.isNaN(xmin)) {
                return null;
            } else {
                return new Rect(xmin + dx, ymin + dy, xmax - xmin, ymax - ymin);
            }
        }
    }
}
