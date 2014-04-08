/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.tk.Toolkit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParentTest {
    private StubToolkit toolkit;
    private Stage stage;

    @Before
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        stage = new Stage();
    }

    @After
    public void tearDown() {
        stage.close();
    }

    @Test
    public void testGroupLookupCorrectId() {
        Rectangle rectA = new Rectangle();
        Rectangle rectB = new Rectangle();
        Rectangle rectC = new Rectangle();
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();
        Rectangle rect4 = new Rectangle();
        Rectangle rect5 = new Rectangle();
        Rectangle rect6 = new Rectangle();
        Rectangle rectE = new Rectangle();
        Rectangle rectF = new Rectangle();

        rectA.setId("a");
        rectB.setId("b");
        rectC.setId("c");
        rect1.setId("1");
        rect2.setId("2");
        rect3.setId("3");
        rect4.setId("4");
        rect5.setId("5");
        rect6.setId("6");
        rectE.setId("e");
        rectF.setId("f");

        Group groupD = new Group(rect1, rect2, rect3, rect4, rect5, rect6);
        groupD.setId("d");
        Group group = new Group(rectA, rectB, rectC, groupD, rectE, rectF);

        assertEquals(rect1, group.lookup("#1"));
        assertEquals(rect2, group.lookup("#2"));
        assertEquals(rect3, group.lookup("#3"));
        assertEquals(rect4, group.lookup("#4"));
        assertEquals(rect5, group.lookup("#5"));
        assertEquals(rect6, group.lookup("#6"));
        assertEquals(rectA, group.lookup("#a"));
        assertEquals(rectB, group.lookup("#b"));
        assertEquals(rectC, group.lookup("#c"));
        assertEquals(groupD, group.lookup("#d"));
        assertEquals(rectE, group.lookup("#e"));
        assertEquals(rectF, group.lookup("#f"));
    }

    @Test
    public void testGroupLookupBadId() {
        Rectangle rectA = new Rectangle();
        Rectangle rectB = new Rectangle();
        Rectangle rectC = new Rectangle();
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();
        Rectangle rect4 = new Rectangle();
        Rectangle rect5 = new Rectangle();
        Rectangle rect6 = new Rectangle();
        Rectangle rectE = new Rectangle();
        Rectangle rectF = new Rectangle();

        rectA.setId("a");
        rectB.setId("b");
        rectC.setId("c");
        rect1.setId("1");
        rect2.setId("2");
        rect3.setId("3");
        rect4.setId("4");
        rect5.setId("5");
        rect6.setId("6");
        rectE.setId("e");
        rectF.setId("f");


        Group groupD = new Group(rect1, rect2, rect3, rect4, rect5, rect6);
        groupD.setId("d");
        Group group = new Group(rectA, rectB, rectC, groupD, rectE, rectF);

        assertNull(group.lookup("#4444"));
    }

    @Test
    public void testRemoveChild() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();
        Rectangle rect4 = new Rectangle();
        Rectangle rect5 = new Rectangle();
        Rectangle rect6 = new Rectangle();

        Group g = new Group();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        g.getChildren().addAll(rect1, rect2, rect3, rect4, rect5, rect6);
        toolkit.fireTestPulse();

        // try removing node from the end of the observableArrayList
        g.getChildren().remove(rect6);
        toolkit.fireTestPulse();
        final NGGroup peer = g.impl_getPeer();
        assertEquals(5, g.getChildren().size());
        assertEquals(5, peer.getChildren().size());

        // try removing node from the beginning of the observableArrayList
        g.getChildren().remove(rect1);
        toolkit.fireTestPulse();
        assertEquals(4, g.getChildren().size());
        assertEquals(4, peer.getChildren().size());

        // try removing node from the middle of the observableArrayList
        g.getChildren().remove(rect3);
        toolkit.fireTestPulse();
        assertEquals(3, g.getChildren().size());
        assertEquals(3, peer.getChildren().size());
    }

    @Test
    public void testSetChild() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();

        Group g = new Group();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        g.getChildren().addAll(rect1, rect2);
        toolkit.fireTestPulse();

        // try setting node at given index
        g.getChildren().set(1, rect3);
        toolkit.fireTestPulse();
        assertEquals(2, g.getChildren().size());
        assertEquals(2, ((NGGroup)g.impl_getPeer()).getChildren().size());
    }

    @Test
    public void testSetSameChild() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();

        Group g = new Group();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        g.getChildren().addAll(rect1, rect2);
        toolkit.fireTestPulse();

        // try setting the same node at given index
        g.getChildren().set(1, rect2);

        toolkit.fireTestPulse();
        assertEquals(2, g.getChildren().size());
        assertEquals(2, ((NGGroup)g.impl_getPeer()).getChildren().size());
    }

    @Test
    public void testRemoveAddSameChild() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();
        Rectangle rect4 = new Rectangle();
        Rectangle rect5 = new Rectangle();
        Rectangle rect6 = new Rectangle();

        Group g = new Group();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        g.getChildren().addAll(rect1, rect2, rect3, rect4, rect5, rect6);
        toolkit.fireTestPulse();

        // try removing node from the end of the observableArrayList
        // and add it afterwords
        g.getChildren().remove(rect6);
        g.getChildren().add(rect6);
        toolkit.fireTestPulse();
        assertEquals(6, g.getChildren().size());
        assertEquals(6, ((NGGroup)g.impl_getPeer()).getChildren().size());
    }

    @Test
    public void testRemoveAddDiferentChild() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();
        Rectangle rect4 = new Rectangle();
        Rectangle rect5 = new Rectangle();
        Rectangle rect6 = new Rectangle();

        Group g = new Group();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        g.getChildren().addAll(rect1, rect2, rect3, rect4, rect5);
        toolkit.fireTestPulse();

        // try removing node from the end of the observableArrayList
        // and add a different one
        g.getChildren().remove(rect5);
        g.getChildren().add(rect6);
        toolkit.fireTestPulse();
        assertEquals(5, g.getChildren().size());
        assertEquals(5, ((NGGroup)g.impl_getPeer()).getChildren().size());
    }

    @Test
    public void testGetChildrenUnmodifiable() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();

        Group g = new Group();
        g.getChildren().addAll(rect1,rect2,rect3);

        assertEquals(3, g.getChildrenUnmodifiable().size());
        assertSame(rect1, g.getChildrenUnmodifiable().get(0));
        assertSame(rect2, g.getChildrenUnmodifiable().get(1));
        assertSame(rect3, g.getChildrenUnmodifiable().get(2));
    }

    @Test
    public void testGetChildrenUnmodifiableCantBeModified() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();

        Group g = new Group();
        g.getChildren().addAll(rect1,rect2,rect3);

        try {
            g.getChildrenUnmodifiable().add(new Rectangle());
            fail("UnsupportedOperationException should have been thrown.");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
    }

    @Test
    public void testRequestLayoutWithoutParent() {
        Group g = new Group();
        g.layout();
        // this shouldn't fail even when the group doesn't have a parent
        g.requestLayout();
    }

    @Test
    public void testRequestLayoutWithoutScene() {
        Group g = new Group();
        g.setManaged(false);
        g.layout();
        // this shouldn't fail even when the scene is not set
        g.requestLayout();
    }

    @Test
    public void testRequestLayoutClearsCache() {
        Group g = new Group();
        Rectangle r = new Rectangle(100,200);
        g.getChildren().add(r);

        g.requestLayout();
        assertEquals(100, g.prefWidth(-1), 1e-100);
        assertEquals(200, g.prefHeight(-1), 1e-100);

        r.setWidth(150);
        r.setHeight(250);
        g.requestLayout();

        assertEquals(150, g.prefWidth(-1), 1e-100);
        assertEquals(250, g.prefHeight(-1), 1e-100);
    }

    @Test
    public void testPrefWidthIncludesChildLayoutX() {
        Rectangle r = new Rectangle(10,10,100,100);
        r.setLayoutX(10);
        MockParent p = new MockParent(r);

        assertEquals(120, p.prefWidth(-1), 0);
    }

    @Test
    public void testPrefHeightIncludesChildLayoutY() {
        Rectangle r = new Rectangle(10,10,100,100);
        r.setLayoutY(10);
        MockParent p = new MockParent(r);

        assertEquals(120, p.prefHeight(-1), 0);
    }

    @Test
    public void testDuplicates() {
        Group g = new Group();

        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();
        Rectangle r3 = new Rectangle();
        Rectangle r4 = new Rectangle();

        try {
        g.getChildren().addAll(r1, r2, r3, r4);
        g.getChildren().add(r2);
        fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test(expected=NullPointerException.class)
    public void testAddingNullChild() {
        Group g = new Group();
        g.getChildren().add(null);
    }

    @Test(expected=NullPointerException.class)
    public void testNullCheckIsDoneBeforeTestForDuplicates() {
        Group g = new Group();
        g.getChildren().addAll(null, new Rectangle(), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAddingClipNodeTwice() {
        Group g = new Group();

        Node clipParent = new Rectangle();
        Node clipNode = new Rectangle();

        clipParent.setClip(clipNode);
        try {
            // try to add node which is already set as a clip
            g.getChildren().add(clipNode);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // try again
        g.getChildren().add(clipNode);
    }

    @Test
    public void testAddingFixedClipNode() {
        Group g = new Group();

        Node clipParent = new Rectangle();
        Node clipNode = new Rectangle();

        clipParent.setClip(clipNode);
        try {
            // try to add node which is already set as a clip
            g.getChildren().add(clipNode);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // fix the problem and add again
        clipParent.setClip(null);
        g.getChildren().add(clipNode);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFalsePermutation() {
        Group g = new Group();

        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();
        Rectangle r3 = new Rectangle();
        Rectangle r4 = new Rectangle();

        g.getChildren().addAll(r1, r2, r3, r4);
        g.getChildren().setAll(r1, r2, r2, r4);
    }

    @Test
    public void testFalseDuplicates() {
        Group g = new Group();

        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();
        Rectangle r3 = new Rectangle();
        Rectangle r4 = new Rectangle();

        g.getChildren().addAll(r1, r2);
        try {
            g.getChildren().addAll(r3, r4, r2);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            g.getChildren().add(r3);
        } catch (IllegalArgumentException e) {
            fail();
        }

    }

    @Test
    public void nodeCanBeAddedDuringLayout() {
        final Rectangle rect = new Rectangle(100, 100);
        final Group g = new Group(rect);

        Group root = new Group() {
            @Override protected void layoutChildren() {
                getChildren().setAll(g);
            }
        };

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();

        // there are assertions tested down the stack (see RT-21746)
    }

    private static class LGroup extends Group {

        private boolean layoutCalled;

        @Override
        public void requestLayout() {
            super.requestLayout();
            layoutCalled = true;
        }

        public void assertAndClear(boolean b) {
            assertEquals(b, layoutCalled);
            layoutCalled = false;
        }

        public void clear() {
            layoutCalled = false;
        }

    }

    @Test
    public void requestLayoutAlwaysCalledUpToTheLayoutRoot() {
        final Group root = new Group() {

            @Override
            public void requestLayout() {
                fail();
            }

        };
        final LGroup lroot = new LGroup();
        lroot.setManaged(false);
        root.getChildren().add(lroot);
        final LGroup sub = new LGroup();
        lroot.getChildren().add(sub);

        lroot.clear();
        sub.clear();
        root.layout();

        lroot.assertAndClear(false);
        sub.assertAndClear(false);

        sub.requestLayout();

        lroot.assertAndClear(true);
        sub.assertAndClear(true);

        sub.requestLayout();
        lroot.assertAndClear(true);
        sub.assertAndClear(true);

    }

    @Test
    public void requestLayoutTriggersPulse() {
        final Group root = new Group();
        final LGroup lroot = new LGroup();
        lroot.setManaged(false);
        root.getChildren().add(lroot);
        final LGroup sub = new LGroup();
        lroot.getChildren().add(sub);

        toolkit.clearPulseRequested();
        sub.requestLayout();
        Scene scene = new Scene(root);
        assertTrue(toolkit.isPulseRequested());
        toolkit.clearPulseRequested();
        root.layout();

        sub.requestLayout();

        assertTrue(toolkit.isPulseRequested());
    }

    @Test
    public void requestLayoutNotPropagatingDuringLayout() {
        final LGroup lroot = new LGroup();
        lroot.setManaged(false);
        final LGroup sub = new LGroup() {

            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                requestLayout();
            }

        };
        lroot.getChildren().add(sub);
        lroot.clear();
        sub.clear();

        sub.requestLayout();
        lroot.assertAndClear(true);
        sub.assertAndClear(true);

        lroot.layout();

        lroot.assertAndClear(false);
        sub.assertAndClear(true);
    }

    @Test
    public void testChildrenPermutationInvalidatesManagedChildrenAndLayout() {
        LGroup root = new LGroup();
        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();

        root.getChildren().addAll(r1, r2);

        root.clear();

        root.getManagedChildren().equals(root.getChildren());

        root.getChildren().setAll(r2, r1);

        root.getManagedChildren().equals(root.getChildren());
        root.assertAndClear(true);

        r2.toFront();

        root.getManagedChildren().equals(root.getChildren());
        root.assertAndClear(true);
    }

    @Test
    public void newChildInvalidatesLayoutWhenLayoutBoundsAreValidatedImmediately() {
        Group root = new Group();
        final AtomicBoolean layoutCalled = new AtomicBoolean();
        final AtomicBoolean testReady = new AtomicBoolean();
        LGroup sub = new LGroup() {

            @Override
            protected void layoutChildren() {
                if (testReady.get()) {
                    assertAndClear(true);
                    layoutCalled.set(true);
                }
            }

        };
        root.getChildren().add(sub);
        root.getLayoutBounds(); // validate
        sub.getBoundsInParent(); // validate

        root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            // ChangeListener will immediately validate the bounds
        });
        sub.clear();

        testReady.set(true);
        sub.getChildren().add(new Rectangle());
        assertTrue(layoutCalled.get());
    }

    @Test
    public void sceneListenerCanAddChild() {
        final Group root = new Group();
        final Scene scene = new Scene(root, 600, 450);

        final Group child = new Group(new Group(), new Group(), new Group());

        child.getChildren().get(1).sceneProperty().addListener(o -> child.getChildren().add(2, new Group()));

        root.getChildren().add(child);

        assertSame(scene, child.getChildren().get(3).getScene());
    }

    public static class MockParent extends Parent {
        public MockParent(Node... children) {
            getChildren().addAll(children);
        }
    }
}
