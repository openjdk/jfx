/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.ParentShim;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests baseline offsets on various classes
 *
 */
public class BaselineTest {

    // test isResizable on key base classes

    @Test public void testShapeBaselineAtBottom() {
        Rectangle rect = new Rectangle(100,200);
        assertEquals(200, rect.getBaselineOffset(),1e-100);
    }

    @Test public void testTextBaseline() {
        Text text = new Text("Graphically");
        float size = (float) text.getFont().getSize();
        assertEquals(size, text.getBaselineOffset(),1e-100);
    }

    @Test public void testParentBaselineMatchesFirstChild() {
        Parent p = new MockParent();
        p.layout();
        assertEquals(180, p.getBaselineOffset(),1e-100);
    }

    @Test public void testParentBaselineIgnoresUnmanagedChild() {
        MockParent p = new MockParent();
        Rectangle r = new Rectangle(20,30);
        r.setManaged(false);
        ParentShim.getChildren(p).add(0, r);
        p.layout();
        assertEquals(180, p.getBaselineOffset(),1e-100);
    }

    /**
     * A Parent that contains multiple children will, by default, compute its baseline
     * based on the first child node that returns isTextBaseline()==true.
     */
    @Test public void testParentSelectsFirstTextNodeAsBaselineSource() {
        Pane p = new Pane();
        p.getChildren().add(new Rectangle());
        p.getChildren().add(new TextRectangle(100, 100, 1234));
        p.getChildren().add(new Rectangle());

        assertEquals(1234, p.getBaselineOffset(), 1e-10);
    }

    /**
     * A Parent that contains multiple children will select the first child that returns
     * isPrefBaseline()==true, even if other children return isTextBaseline()==true.
     */
    @Test public void testPrefBaselineOverridesTextBaselineSource() {
        Pane p = new Pane();
        p.getChildren().add(new Rectangle() {
            { setPrefBaseline(true); }
            @Override public double getBaselineOffset() { return 1234; }
        });
        p.getChildren().add(new TextRectangle(100, 100, 200));
        p.getChildren().add(new Rectangle());

        assertEquals(1234, p.getBaselineOffset(), 1e-10);
    }

    /**
     * A Parent that computes its baseline offset based on a text child will also
     * return isTextBaseline()==true.
     */
    @Test public void testIsTextBaselinePropagatesToParent() {
        Pane p = new Pane();
        p.getChildren().add(new HBox() {
            { getChildren().add(new TextRectangle(100, 100, 1234)); }
        });

        assertTrue(p.isTextBaseline());
        assertEquals(1234, p.getBaselineOffset(), 1e-10);
    }

    /**
     * A Parent returns isTextBaseline()==false if a non-text child has been manually
     * selected by setting prefBaseline on the child.
     */
    @Test public void testIsTextBaselineDoesNotPropagateToParentWhenOverriddenByPrefBaseline() {
        Pane p = new Pane();
        p.getChildren().add(new HBox() {
            {
                getChildren().add(new TextRectangle(100, 100, 1234));
                getChildren().add(new Rectangle(100, 100) {{ setPrefBaseline(true); }});
            }
        });

        assertFalse(p.isTextBaseline());
        assertEquals(100, p.getBaselineOffset(), 1e-10);
    }

    /**
     * A Parent that computes its baseline from a manually selected child node will automatically
     * update its isTextBaseline() and getBaselineOffset() when prefBaseline is set to 'false'
     * and an alternative text node is available.
     */
    @Test public void testPrefBaselineChangedUpdatesParentIsTextBaseline() {
        Pane p = new Pane();
        Rectangle[] r = new Rectangle[1];
        p.getChildren().add(new HBox() {
            {
                getChildren().add(new TextRectangle(100, 100, 1234));
                getChildren().add(r[0] = new Rectangle(100, 100));
            }
        });

        assertTrue(p.isTextBaseline());
        assertEquals(1234, p.getBaselineOffset(), 1e-10);

        r[0].setPrefBaseline(true);
        assertFalse(p.isTextBaseline());
        assertEquals(100, p.getBaselineOffset(), 1e-10);

        r[0].setPrefBaseline(false);
        assertTrue(p.isTextBaseline());
        assertEquals(1234, p.getBaselineOffset(), 1e-10);
    }

    /**
     * Tests that the baseline offset is calculated correctly from two text-node children
     * in different branches of the scene graph.
     *
     *            w=20            w=20
     *         ┌───────┐       ┌───────┐
     *    h=20 │   A   │       │   C   │ h=40
     *         ├───────┴───────┤       │
     *    ═════╪═══════════════╪═══════╪═════ baseline=30
     *        B└───────────────┼───────┤
     *                w=40     │   D   │ h=20
     *                         └───────┘
     */
    @Test public void testTextBaselineFromChildrenInDifferentBranches() {
        HBox p = new HBox();
        p.setAlignment(Pos.BASELINE_LEFT);
        p.getChildren().add(new VBox() {{
            getChildren().add(new Rectangle(20, 20)); // box A
            getChildren().add(new TextRectangle(40, 20, 10)); // box B
        }});
        p.getChildren().add(new VBox() {{
            getChildren().add(new TextRectangle(20, 40, 30)); // box C
            getChildren().add(new Rectangle(20, 20)); // box D
        }});
        p.layout();
        p.autosize();

        assertTrue(p.isTextBaseline());
        assertEquals(30, p.getBaselineOffset(), 1e-10);
        assertEquals(60, p.getHeight(), 1e-10);
        assertEquals(60, p.getWidth(), 1e-10);
    }

    /**
     * Similar test setup to {@link #testTextBaselineFromChildrenInDifferentBranches()}, with the
     * difference that there is only a single text-node child in the scene graph.
     *
     *                           w=20
     *            w=20         ┌───────┐
     *         ┌───────┐       │       │
     *    h=20 │   A   │       │   C   │ h=40
     *         ├───────┴───────┤       │
     *    ═════╪═══════════════╪─═─═─═─╪═════ baseline=40
     *        B└───────────────┤       │ h=20
     *                w=40     └───────┘
     *
     */
    @Test public void testTextBaselineFromChildInSingleBranch() {
        HBox p = new HBox();
        p.setAlignment(Pos.BASELINE_LEFT);
        p.getChildren().add(new VBox() {{
            getChildren().add(new Rectangle(20, 20)); // box A
            getChildren().add(new TextRectangle(40, 20, 10)); // box B
        }});
        p.getChildren().add(new VBox() {{
            getChildren().add(new Rectangle(20, 40)); // box C
            getChildren().add(new Rectangle(20, 20)); // box D
        }});
        p.layout();
        p.autosize();

        assertTrue(p.isTextBaseline());
        assertEquals(40, p.getBaselineOffset(), 1e-10);
        assertEquals(60, p.getHeight(), 1e-10);
        assertEquals(60, p.getWidth(), 1e-10);
    }

}
