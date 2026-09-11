/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.LayoutFlags;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.ParentHelper;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.scene.input.PickResultChooser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.GroupShim;
import javafx.scene.Node;
import javafx.scene.NodeShim;
import javafx.scene.Parent;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.Mnemonic;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParentTest {
    private StubToolkit toolkit;
    private Stage stage;

    @BeforeEach
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        stage = new Stage();
    }

    @AfterEach
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

        ParentShim.getChildren(g).addAll(rect1, rect2, rect3, rect4, rect5, rect6);
        toolkit.fireTestPulse();

        // try removing node from the end of the observableArrayList
        ParentShim.getChildren(g).remove(rect6);
        toolkit.fireTestPulse();
        final NGGroup peer = NodeHelper.getPeer(g);
        assertEquals(5, ParentShim.getChildren(g).size());
        assertEquals(5, peer.getChildren().size());

        // try removing node from the beginning of the observableArrayList
        ParentShim.getChildren(g).remove(rect1);
        toolkit.fireTestPulse();
        assertEquals(4, ParentShim.getChildren(g).size());
        assertEquals(4, peer.getChildren().size());

        // try removing node from the middle of the observableArrayList
        ParentShim.getChildren(g).remove(rect3);
        toolkit.fireTestPulse();
        assertEquals(3, ParentShim.getChildren(g).size());
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

        ParentShim.getChildren(g).addAll(rect1, rect2);
        toolkit.fireTestPulse();

        // try setting node at given index
        ParentShim.getChildren(g).set(1, rect3);
        toolkit.fireTestPulse();
        assertEquals(2, ParentShim.getChildren(g).size());
        assertEquals(2, ((NGGroup)NodeHelper.getPeer(g)).getChildren().size());
    }

    @Test
    public void testSetSameChild() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();

        Group g = new Group();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        ParentShim.getChildren(g).addAll(rect1, rect2);
        toolkit.fireTestPulse();

        // try setting the same node at given index
        ParentShim.getChildren(g).set(1, rect2);

        toolkit.fireTestPulse();
        assertEquals(2, ParentShim.getChildren(g).size());
        assertEquals(2, ((NGGroup)NodeHelper.getPeer(g)).getChildren().size());
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

        ParentShim.getChildren(g).addAll(rect1, rect2, rect3, rect4, rect5, rect6);
        toolkit.fireTestPulse();

        // try removing node from the end of the observableArrayList
        // and add it afterwords
        ParentShim.getChildren(g).remove(rect6);
        ParentShim.getChildren(g).add(rect6);
        toolkit.fireTestPulse();
        assertEquals(6, ParentShim.getChildren(g).size());
        assertEquals(6, ((NGGroup)NodeHelper.getPeer(g)).getChildren().size());
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

        ParentShim.getChildren(g).addAll(rect1, rect2, rect3, rect4, rect5);
        toolkit.fireTestPulse();

        // try removing node from the end of the observableArrayList
        // and add a different one
        ParentShim.getChildren(g).remove(rect5);
        ParentShim.getChildren(g).add(rect6);
        toolkit.fireTestPulse();
        assertEquals(5, ParentShim.getChildren(g).size());
        assertEquals(5, ((NGGroup)NodeHelper.getPeer(g)).getChildren().size());
    }

    @Test
    public void testSortChildren() {
        Rectangle rect1 = new Rectangle();
        rect1.setId("1");
        Rectangle rect2 = new Rectangle();
        rect2.setId("2");
        Rectangle rect3 = new Rectangle();
        rect3.setId("3");

        Group g = new Group();
        g.getChildren().addAll(rect3, rect1, rect2);
        g.getChildren().sort(Comparator.comparing(node -> node.getId()));

        assertEquals(List.of(rect1, rect2, rect3), g.getChildren());
    }

    @Test
    public void testGetChildrenUnmodifiable() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();
        Rectangle rect3 = new Rectangle();

        Group g = new Group();
        ParentShim.getChildren(g).addAll(rect1,rect2,rect3);

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
        ParentShim.getChildren(g).addAll(rect1,rect2,rect3);

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
        ParentShim.getChildren(g).add(r);

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
    public void testPrefWidthCacheInvalidatedWhenRenderScaleChanges() {
        Region region = new Region() {
            @Override
            protected double computePrefWidth(double height) {
                return snapSizeX(10.2);
            }
        };

        stage.setScene(new Scene(region));
        stage.setRenderScaleX(1.0);

        assertEquals(11.0, region.prefWidth(-1), 0.0);

        stage.setRenderScaleX(1.5);
        double expected = region.snapSizeX(10.2);

        assertEquals(10.666666666666666, expected, 0.0);
        assertEquals(expected, region.prefWidth(-1), 0.0);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void testLayoutContextInvalidationClearsSubclassCachesBeforeNeedsLayout(boolean horizontal) {
        var child = new TilePane(new Rectangle(10.2, 10.2));
        child.setPrefColumns(1);

        // The final requestLayout() is made on the root. Keeping the tested node below
        // it prevents that call from masking a stale size cache on the child.
        var root = new MockParent(child);
        stage.setScene(new Scene(root, 300, 300));
        root.layout();
        assertFalse(child.isNeedsLayout());
        assertEquals(11.0, child.prefWidth(-1), 0.0);
        assertEquals(11.0, child.prefHeight(-1), 0.0);

        var observedSizes = new ArrayList<Double>();

        child.needsLayoutProperty().addListener((observable, oldValue, needsLayout) -> {
            if (needsLayout) {
                observedSizes.add(child.prefWidth(-1));
                observedSizes.add(child.prefHeight(-1));
            }
        });

        if (horizontal) {
            stage.setRenderScaleX(2.0);
        } else {
            stage.setRenderScaleY(2.0);
        }

        double expectedWidth = horizontal ? 10.5 : 11.0;
        double expectedHeight = horizontal ? 11.0 : 10.5;
        assertEquals(List.of(expectedWidth, expectedHeight), observedSizes);
        assertEquals(expectedWidth, child.prefWidth(-1), 0.0);
        assertEquals(expectedHeight, child.prefHeight(-1), 0.0);
    }

    @Test
    public void testLayoutContextInvalidationClearsSizesCachedDuringNotification() {
        var child = new TilePane(new Rectangle(10.2, 10.2));
        child.setPrefColumns(1);
        var root = new MockParent(child);
        stage.setScene(new Scene(root, 300, 300));
        root.layout();
        assertEquals(11.0, child.prefWidth(-1), 0.0);
        assertEquals(11.0, child.prefHeight(-1), 0.0);

        var measuredDuringNotification = new AtomicBoolean();

        child.tileWidthProperty().addListener(observable -> {
            // The notification invalidates tile width before tile height. A listener can thus
            // refill the Parent's prefHeight cache before the tileHeight cache is cleared.
            child.prefHeight(-1);
            measuredDuringNotification.set(true);
        });

        stage.setRenderScaleY(2.0);

        assertTrue(measuredDuringNotification.get());
        assertEquals(10.5, child.getTileHeight(), 0.0);
        assertEquals(10.5, child.prefHeight(-1), 0.0);
    }

    @Test
    public void testSceneAttachmentInvalidatesMeasurementsAfterUpdatingDescendants() {
        var child = new MockParent() {
            @Override
            protected double computePrefWidth(double height) {
                return snapSizeX(10.2);
            }
        };

        var observedWidths = new ArrayList<Double>();

        var branch = new MockParent(child) {
            @Override
            protected void layoutContextInvalidated() {
                super.layoutContextInvalidated();

                // Invalidating an observable measurement can synchronously query children.
                observedWidths.add(child.prefWidth(-1));
            }
        };

        // Populate the child's size cache before attaching the subtree to a scaled scene.
        assertEquals(11.0, child.prefWidth(-1), 0.0);

        var root = new MockParent();
        stage.setRenderScaleX(1.5);
        stage.setScene(new Scene(root, 300, 300));
        ParentShim.getChildren(root).add(branch);

        double expectedWidth = Math.ceil(10.2 * 1.5) / 1.5;
        assertEquals(List.of(expectedWidth), observedWidths);
        assertEquals(expectedWidth, child.prefWidth(-1), 0.0);

        root.layout();
        assertEquals(expectedWidth, child.prefWidth(-1), 0.0);

        ParentShim.getChildren(root).remove(branch);
        assertEquals(List.of(expectedWidth, 11.0), observedWidths);
        assertEquals(11.0, child.prefWidth(-1), 0.0);
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, false", "true, true", "false, true"})
    public void testReentrantRenderScaleChange(boolean horizontal, boolean fromNeedsLayoutListener) {
        var trigger = new LayoutContextParent();
        var subSceneSibling = new LayoutContextParent();
        var subSceneRoot = new LayoutContextParent(trigger, subSceneSibling);
        var subSceneClip = new LayoutContextParent();
        var subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setClip(subSceneClip);

        var sibling = new LayoutContextParent();
        var root = new LayoutContextParent(subScene, sibling);
        var rootClip = new LayoutContextParent();
        root.setClip(rootClip);

        List<LayoutContextParent> parents = List.of(
            root, subSceneRoot, trigger, subSceneSibling,
            sibling, subSceneClip, rootClip);

        stage.setRenderScaleX(1.0);
        stage.setRenderScaleY(1.0);
        stage.setScene(new Scene(root, 300, 300));
        root.layout();
        assertFalse(trigger.isNeedsLayout());

        for (Parent parent : parents) {
            assertEquals(11.0, parent.prefWidth(-1), 0.0);
            assertEquals(11.0, parent.prefHeight(-1), 0.0);
        }

        DoubleProperty scale = horizontal ? stage.renderScaleXProperty() : stage.renderScaleYProperty();
        DoubleProperty input = fromNeedsLayoutListener ? new SimpleDoubleProperty(1.0) : scale;

        if (fromNeedsLayoutListener) {
            scale.bind(input);
            trigger.needsLayoutProperty().addListener((observable, oldValue, needsLayout) -> {
                if (needsLayout) {
                    input.set(2.0);
                }
            });
        } else {
            trigger.onInvalidation = () -> input.set(2.0);
        }

        // The older traversal must not restore 1.5 after the nested change to 2.0 returns.
        input.set(1.5);
        assertEquals(2.0, scale.get(), 0.0);

        for (Parent parent : parents) {
            assertEquals(horizontal ? 10.5 : 11.0, parent.snapSizeX(10.2), 0.0);
            assertEquals(horizontal ? 11.0 : 10.5, parent.snapSizeY(10.2), 0.0);
            assertEquals(horizontal ? 10.5 : 11.0, parent.prefWidth(-1), 0.0);
            assertEquals(horizontal ? 11.0 : 10.5, parent.prefHeight(-1), 0.0);
        }
    }

    @Test
    public void testReentrantSnapToPixelChange() {
        var trigger = new LayoutContextParent();
        var subSceneSibling = new LayoutContextParent();
        var subSceneRoot = new LayoutContextParent(trigger, subSceneSibling);
        var clip = new LayoutContextParent();
        var subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setClip(clip);

        var sibling = new LayoutContextParent();
        var root = new LayoutContextParent(subScene, sibling);
        List<LayoutContextParent> parents = List.of(root, subSceneRoot, trigger, subSceneSibling, sibling);
        root.setSnapToPixel(false);

        for (Parent parent : parents) {
            assertEquals(10.2, parent.prefWidth(-1), 0.0);
        }

        trigger.onInvalidation = () -> root.setSnapToPixel(false);
        root.setSnapToPixel(true);

        assertNull(trigger.onInvalidation);

        for (Parent parent : parents) {
            assertFalse(parent.isSnappedToPixel());
            assertEquals(10.2, parent.prefWidth(-1), 0.0);
        }

        assertTrue(trigger.isSnapToPixel());
        assertTrue(clip.isSnappedToPixel());
        assertEquals(11.0, clip.prefWidth(-1), 0.0);
    }

    @ParameterizedTest
    @CsvSource({
        "removeSelf, scale", "removeNext, scale", "insert, scale", "reorder, scale", "reparent, scale",
        "removeSelf, snapping", "removeNext, snapping", "insert, snapping", "reorder, snapping", "reparent, snapping",
        "removeSelf, attach", "removeNext, attach", "insert, attach", "reorder, attach", "reparent, attach"
    })
    public void testStructuralChangesDuringNotification(String mutation, String notification) {
        var trigger = new LayoutContextParent();
        var next = new LayoutContextParent();
        var last = new LayoutContextParent();
        var added = new LayoutContextParent();
        var branch = new LayoutContextParent(trigger, next, last);
        var destination = new LayoutContextParent();
        var root = new MockParent(destination);
        boolean attaching = notification.equals("attach");
        var scene = new Scene(root, 300, 300);
        stage.setRenderScaleX(attaching ? 2.0 : 1.0);
        stage.setScene(scene);

        if (!attaching) {
            ParentShim.getChildren(root).add(branch);
        }

        for (Parent parent : List.of(trigger, next, last, added)) {
            assertEquals(11.0, parent.prefWidth(-1), 0.0);
        }

        trigger.onInvalidation = () -> {
            switch (mutation) {
                case "removeSelf" -> ParentShim.getChildren(branch).remove(trigger);
                case "removeNext" -> ParentShim.getChildren(branch).remove(next);
                case "insert" -> ParentShim.getChildren(branch).add(0, added);
                case "reorder" -> last.toBack();
                case "reparent" -> ParentShim.getChildren(destination).add(next);
                default -> throw new AssertionError(mutation);
            }
        };

        switch (notification) {
            case "scale" -> stage.setRenderScaleX(2.0);
            case "snapping" -> branch.setSnapToPixel(false);
            case "attach" -> ParentShim.getChildren(root).add(branch);
            default -> throw new AssertionError(notification);
        }

        assertNull(trigger.onInvalidation);

        List<LayoutContextParent> expectedChildren = switch (mutation) {
            case "removeSelf" -> List.of(next, last);
            case "removeNext", "reparent" -> List.of(trigger, last);
            case "insert" -> List.of(added, trigger, next, last);
            case "reorder" -> List.of(last, trigger, next);
            default -> throw new AssertionError(mutation);
        };

        assertEquals(expectedChildren, branch.getChildrenUnmodifiable());

        for (Parent child : expectedChildren) {
            assertSame(branch, child.getParent());
            assertSame(scene, child.getScene());
            assertEquals(notification.equals("snapping") ? 10.2 : 10.5, child.prefWidth(-1), 0.0);
        }

        if (mutation.equals("removeSelf") || mutation.equals("removeNext")) {
            Parent removed = mutation.equals("removeSelf") ? trigger : next;
            assertNull(removed.getParent());
            assertNull(removed.getScene());
            assertEquals(11.0, removed.prefWidth(-1), 0.0);
        } else if (mutation.equals("reparent")) {
            assertSame(destination, next.getParent());
            assertSame(scene, next.getScene());
            assertEquals(notification.equals("snapping") ? 11.0 : 10.5, next.prefWidth(-1), 0.0);
        }
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void testStructuralMoveDuringSceneAttachment(boolean fromClip) {
        var trigger = new LayoutContextParent();
        var sibling = new LayoutContextParent();
        var branch = new LayoutContextParent(sibling);

        if (fromClip) {
            branch.setClip(trigger);
        } else {
            ParentShim.getChildren(branch).add(0, trigger);
        }

        var source = new MockParent();
        stage.setRenderScaleX(1.5);
        stage.setScene(new Scene(source, 300, 300));

        var destStage = new Stage();

        try {
            var destSubScene = new SubScene(new MockParent(), 100, 100);
            var destRoot = new MockParent(destSubScene);
            var destScene = new Scene(destRoot, 300, 300);

            destStage.setRenderScaleX(2.0);
            destStage.setScene(destScene);
            trigger.onInvalidation = () -> {
                ParentShim.getChildren(source).remove(branch);
                destSubScene.setRoot(branch);
            };

            ParentShim.getChildren(source).add(branch);

            assertSame(branch, destSubScene.getRoot());

            for (Parent parent : List.of(branch, trigger, sibling)) {
                assertSame(destScene, parent.getScene());
                assertSame(destSubScene, NodeShim.getSubScene(parent));
                assertEquals(10.5, parent.prefWidth(-1), 0.0);
            }

            // The resumed attachment must retain the branch's new role as a SubScene layout root.
            destRoot.layout();
            int layoutCount = branch.layoutCount;
            branch.requestLayout();
            destRoot.layout();
            assertEquals(layoutCount + 1, branch.layoutCount);
        } finally {
            destStage.close();
        }
    }

    @Test
    public void testStructuralMoveOfSubSceneDuringClipNotification() {
        var clip = new LayoutContextParent();
        var child = new LayoutContextParent();
        var subSceneRoot = new LayoutContextParent(child);
        var subScene = new SubScene(subSceneRoot, 100, 100);
        subScene.setClip(clip);

        var source = new MockParent();
        stage.setRenderScaleX(1.5);
        stage.setScene(new Scene(source, 300, 300));

        Stage destStage = new Stage();

        try {
            var dest = new MockParent();
            var destScene = new Scene(dest, 300, 300);
            destStage.setRenderScaleX(2.0);
            destStage.setScene(destScene);
            clip.onInvalidation = () -> ParentShim.getChildren(dest).add(subScene);

            ParentShim.getChildren(source).add(subScene);

            assertSame(dest, subScene.getParent());
            assertSame(destScene, subScene.getScene());

            for (Parent parent : List.of(clip, subSceneRoot, child)) {
                assertSame(destScene, parent.getScene());
                assertEquals(10.5, parent.prefWidth(-1), 0.0);
            }

            assertSame(subScene, NodeShim.getSubScene(subSceneRoot));
            assertSame(subScene, NodeShim.getSubScene(child));
        } finally {
            destStage.close();
        }
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void testStructuralReattachmentDuringDetach(boolean fromClip) {
        var trigger = new LayoutContextParent();
        var sibling = new LayoutContextParent();
        var branch = new LayoutContextParent(sibling);

        if (fromClip) {
            branch.setClip(trigger);
        } else {
            ParentShim.getChildren(branch).add(0, trigger);
        }

        var released = new AtomicBoolean();
        var peer = new NGGroup() {
            @Override
            public void release() {
                released.set(true);
                super.release();
            }
        };

        new ParentHelper() {
            { setHelper(branch, this); }

            @Override
            protected NGGroup createPeerImpl(Node node) {
                return peer;
            }
        };

        var dest = new MockParent();
        var source = new MockParent(branch, dest);
        stage.setRenderScaleX(1.5);
        Scene scene = new Scene(source, 300, 300);
        stage.setScene(scene);
        assertSame(peer, NodeHelper.getPeer(branch));

        var key = new KeyCodeCombination(KeyCode.A);
        var oldMnemonic = new Mnemonic(branch, key);
        var newMnemonic = new Mnemonic(branch, key);
        scene.addMnemonic(oldMnemonic);
        trigger.onInvalidation = () -> {
            ParentShim.getChildren(dest).add(branch);
            scene.addMnemonic(newMnemonic);
        };

        ParentShim.getChildren(source).remove(branch);

        assertSame(dest, branch.getParent());

        for (Parent parent : List.of(branch, trigger, sibling)) {
            assertSame(scene, parent.getScene());
            assertEquals(10.666666666666666, parent.prefWidth(-1), 0.0);
        }

        assertFalse(released.get(), "The outer detach must not release the reattached node's peer");
        assertEquals(List.of(newMnemonic), scene.getMnemonics().get(key));
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
        ParentShim.getChildren(g).addAll(r1, r2, r3, r4);
        ParentShim.getChildren(g).add(r2);
        fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testAddingNullChild() {
        assertThrows(NullPointerException.class, () -> {
            Group g = new Group();
            ParentShim.getChildren(g).add(null);
        });
    }

    @Test
    public void testNullCheckIsDoneBeforeTestForDuplicates() {
        assertThrows(NullPointerException.class, () -> {
            Group g = new Group();
            ParentShim.getChildren(g).addAll(null, new Rectangle(), null);
        });
    }

    @Test
    public void testAddingClipNodeTwice() {
        assertThrows(IllegalArgumentException.class, () -> {
            Group g = new Group();

            Node clipParent = new Rectangle();
            Node clipNode = new Rectangle();

            clipParent.setClip(clipNode);
            try {
                // try to add node which is already set as a clip
                ParentShim.getChildren(g).add(clipNode);
                fail();
            } catch (IllegalArgumentException e) {
            }

            // try again
            ParentShim.getChildren(g).add(clipNode);
        });
    }

    @Test
    public void testAddingFixedClipNode() {
        Group g = new Group();

        Node clipParent = new Rectangle();
        Node clipNode = new Rectangle();

        clipParent.setClip(clipNode);
        try {
            // try to add node which is already set as a clip
            ParentShim.getChildren(g).add(clipNode);
            fail();
        } catch (IllegalArgumentException e) {
        }

        // fix the problem and add again
        clipParent.setClip(null);
        ParentShim.getChildren(g).add(clipNode);
    }

    @Test
    public void testFalsePermutation() {
        assertThrows(IllegalArgumentException.class, () -> {
            Group g = new Group();

            Rectangle r1 = new Rectangle();
            Rectangle r2 = new Rectangle();
            Rectangle r3 = new Rectangle();
            Rectangle r4 = new Rectangle();

            ParentShim.getChildren(g).addAll(r1, r2, r3, r4);
            ParentShim.getChildren(g).setAll(r1, r2, r2, r4);
        });
    }

    @Test
    public void testFalseDuplicates() {
        Group g = new Group();

        Rectangle r1 = new Rectangle();
        Rectangle r2 = new Rectangle();
        Rectangle r3 = new Rectangle();
        Rectangle r4 = new Rectangle();

        ParentShim.getChildren(g).addAll(r1, r2);
        try {
            ParentShim.getChildren(g).addAll(r3, r4, r2);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            ParentShim.getChildren(g).add(r3);
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
                ParentShim.getChildren(this).setAll(g);
            }
        };

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();

        // there are assertions tested down the stack (see JDK-8115729)
    }

    @Test
    public void needsLayoutPropertyIsReadOnly() {
        assertThrows(
            ClassCastException.class,
            () -> { var _ = (Property<Boolean>)new Group().needsLayoutProperty(); });
    }

    @Test
    public void isNeedsLayoutReturnsCorrectValueInListener() {
         var g = new Group();
         g.layout();
         assertFalse(g.isNeedsLayout());

         boolean[] flags = new boolean[2];
         g.needsLayoutProperty().subscribe(value -> {
             flags[0] = value;
             flags[1] = g.isNeedsLayout();
         });

         ParentShim.setNeedsLayout(g, true);
         assertTrue(flags[0]);
         assertTrue(flags[1]);
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

    /**
     * Checks if layout flags are always consistent, even when a 2nd layout
     * pass is requested due to a modification while layout was running.
     *
     * This test needs at least a layout tree of 4 levels deep due to
     * how the layout flags are propagated:
     * - Node will force another layout on the PARENT of sibling
     * - Parent code will then ask for another layout on its parent
     * - If forceParentLayout flag is not propagated, then this does
     *   not continue up to the root, leaving the root clean.
     */
    @Test
    public void layoutPositionModificationDuringLayoutPassShouldNotLeaveLayoutFlagsInInconsistentState() {
        AtomicBoolean modifySiblingDuringLayout = new AtomicBoolean();
        HBox sibling = new HBox();
        HBox leaf = new HBox() {
            @Override
            protected void layoutChildren() {
                super.layoutChildren();

                /*
                 * Sometimes layout code modifies a sibling's position,
                 * in which case Node will force its parent to do another
                 * layout in a next pass (see layoutX and layoutY properties).
                 *
                 * The layout flags should not become inconsistent
                 * when it does so.
                 */

                if (modifySiblingDuringLayout.get()) {
                    sibling.setLayoutX(100);
                }
            }
        };
        VBox level2 = new VBox(leaf, sibling);
        HBox level1 = new HBox(level2);
        VBox root = new VBox(level1);

        // Assert default state after controls are created:
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(level1));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(level2));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(leaf));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(sibling));

        root.layout();

        // Assert that all is clean after a layout pass:
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(level1));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(level2));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(leaf));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(sibling));

        leaf.requestLayout();

        // Assert that all nodes between leaf and root are marked as needing layout:
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(level1));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(level2));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(leaf));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(sibling));

        // Trigger a layout that does a modification that needs a 2nd pass:
        modifySiblingDuringLayout.set(true);
        root.layout();

        // Assert that the parent of the sibling, all the way to the root are marked as needing another layout pass:
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(level1));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(level2));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(leaf));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(sibling));

        root.layout();

        // Assert that after another layout pass all are clean again:
        // Note: we still modify the sibling, but since its layoutX is unchanged now, no further pass is triggered
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(level1));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(level2));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(leaf));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(sibling));
    }

    @Test
    public void testRequestLayoutCall() {
        Rectangle rect1 = new Rectangle();
        Rectangle rect2 = new Rectangle();

        LGroup g = new LGroup();
        Scene s = new Scene(g);
        stage.setScene(s);
        stage.show();

        ParentShim.getChildren(g).addAll(rect1, rect2);
        toolkit.fireTestPulse();

        g.clear();
        // set rect1's layoutX to new value
        rect1.setLayoutX(10);
        g.assertAndClear(true);
        toolkit.fireTestPulse();

        // set rect1's layoutX to same value
        rect1.setLayoutX(10);
        g.assertAndClear(false);
        toolkit.fireTestPulse();

        // set rect2's layoutX to new value
        rect2.setLayoutX(10);
        g.assertAndClear(true);
        toolkit.fireTestPulse();

        // set rect2's layoutY to new value
        rect2.setLayoutY(10);
        g.assertAndClear(true);
        toolkit.fireTestPulse();

        // set rect1's layoutX to same value
        rect1.setLayoutX(10);
        g.assertAndClear(false);
        toolkit.fireTestPulse();

        // set rect2's layoutY to same value
        rect2.setLayoutY(10);
        g.assertAndClear(false);
        toolkit.fireTestPulse();

        // set rect1 to new values
        rect1.resizeRelocate(5, 5, 10, 10);
        g.assertAndClear(true);
        toolkit.fireTestPulse();

        // set rect2 to new values
        rect2.resizeRelocate(5, 5, 10, 10);
        g.assertAndClear(true);
        toolkit.fireTestPulse();

        // set rect1 to same values
        rect1.resizeRelocate(5, 5, 10, 10);
        g.assertAndClear(false);
        toolkit.fireTestPulse();
    }

    @Test
    public void requestLayoutAlwaysCalledUpToTheLayoutRoot() {
        final Group root = new Group();
        final LGroup lroot = new LGroup();
        lroot.setManaged(false);
        ParentShim.getChildren(root).add(lroot);
        final LGroup sub = new LGroup();
        ParentShim.getChildren(lroot).add(sub);

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
    public void unmanagedParentTest() {
        final LGroup innerGroup = new LGroup();
        innerGroup.setManaged(false);

        final LGroup outerGroup = new LGroup();
        outerGroup.setManaged(false);

        Scene s = new Scene(outerGroup);

        innerGroup.assertAndClear(false);
        outerGroup.assertAndClear(true);

        outerGroup.getChildren().add(innerGroup);
        innerGroup.requestLayout();

        innerGroup.assertAndClear(true);
        outerGroup.assertAndClear(true);

        outerGroup.getChildren().remove(innerGroup);

        innerGroup.assertAndClear(false);
        outerGroup.assertAndClear(false);

        final LGroup intermediate = new LGroup();
        intermediate.setManaged(false);
        intermediate.setAutoSizeChildren(false);
        outerGroup.getChildren().add(intermediate);

        innerGroup.assertAndClear(false);
        intermediate.assertAndClear(true);
        outerGroup.assertAndClear(true);

        innerGroup.requestLayout();
        intermediate.getChildren().add(innerGroup);

        innerGroup.assertAndClear(true);
        intermediate.assertAndClear(true);
        outerGroup.assertAndClear(false);

    }

    @Test
    public void requestLayoutTriggersPulse() {
        final Group root = new Group();
        final LGroup lroot = new LGroup();
        lroot.setManaged(false);
        ParentShim.getChildren(root).add(lroot);
        final LGroup sub = new LGroup();
        ParentShim.getChildren(lroot).add(sub);

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
                GroupShim.layoutChildren((Group)getParent());
                requestLayout();
            }

        };
        ParentShim.getChildren(lroot).add(sub);
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

        ParentShim.getChildren(root).addAll(r1, r2);

        root.clear();

        ParentShim.getManagedChildren(root).equals(ParentShim.getChildren(root));

        ParentShim.getChildren(root).setAll(r2, r1);

        ParentShim.getManagedChildren(root).equals(ParentShim.getChildren(root));
        root.assertAndClear(true);

        r2.toFront();

        ParentShim.getManagedChildren(root).equals(ParentShim.getChildren(root));
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
        ParentShim.getChildren(root).add(sub);
        root.getLayoutBounds(); // validate
        sub.getBoundsInParent(); // validate

        root.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            // ChangeListener will immediately validate the bounds
        });
        sub.clear();

        testReady.set(true);
        ParentShim.getChildren(sub).add(new Rectangle());
        assertTrue(layoutCalled.get());
    }

    @Test
    public void sceneListenerCanAddChild() {
        final Group root = new Group();
        final Scene scene = new Scene(root, 600, 450);

        final Group child = new Group(new Group(), new Group(), new Group());

        ParentShim.getChildren(child).get(1).sceneProperty().addListener(o -> ParentShim.getChildren(child).add(2, new Group()));

        ParentShim.getChildren(root).add(child);

        assertSame(scene, ParentShim.getChildren(child).get(3).getScene());
    }

    @Test
    public void testPickingChildNode() {
        Rectangle rect1 = new Rectangle();
        rect1.setX(10);
        rect1.setY(10);
        rect1.setWidth(100);
        rect1.setHeight(100);

        Rectangle rect2 = new Rectangle();
        rect2.setX(10);
        rect2.setY(10);
        rect2.setWidth(100);
        rect2.setHeight(100);
        Group g = new Group();

        // needed since picking doesn't work unless rooted in a scene and visible
        Scene scene = new Scene(g);
        stage.setScene(scene);
        stage.show();
        ParentShim.getChildren(g).addAll(rect1, rect2);
        toolkit.fireTestPulse();

        PickResultChooser res = new PickResultChooser();
        NodeHelper.pickNode(g, new PickRay(50, 50, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertSame(rect2, res.getIntersectedNode());
        res = new PickResultChooser();
        NodeHelper.pickNode(g, new PickRay(0, 0, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertNull(res.getIntersectedNode());
    }

    @Test
    public void testPickingChildNodeWithViewOrderSet() {
        Rectangle rect1 = new Rectangle();
        rect1.setX(10);
        rect1.setY(10);
        rect1.setWidth(100);
        rect1.setHeight(100);
        rect1.setViewOrder(-1);

        Rectangle rect2 = new Rectangle();
        rect2.setX(10);
        rect2.setY(10);
        rect2.setWidth(100);
        rect2.setHeight(100);
        Group g = new Group();

        // needed since picking doesn't work unless rooted in a scene and visible
        Scene scene = new Scene(g);
        stage.setScene(scene);
        stage.show();
        ParentShim.getChildren(g).addAll(rect1, rect2);
        toolkit.fireTestPulse();

        PickResultChooser res = new PickResultChooser();
        NodeHelper.pickNode(g, new PickRay(50, 50, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertSame(rect1, res.getIntersectedNode());
        res = new PickResultChooser();
        NodeHelper.pickNode(g, new PickRay(0, 0, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertNull(res.getIntersectedNode());
    }

    @Test
    public void testPickingChildNodeWithDirtyViewOrder() {
        //JDK-8205092
        Rectangle rect1 = new Rectangle();
        rect1.setX(10);
        rect1.setY(10);
        rect1.setWidth(100);
        rect1.setHeight(100);
        rect1.setViewOrder(-1);

        Rectangle rect2 = new Rectangle();
        rect2.setX(10);
        rect2.setY(10);
        rect2.setWidth(100);
        rect2.setHeight(100);
        Group g = new Group();

        // needed since picking doesn't work unless rooted in a scene and visible
        Scene scene = new Scene(g);
        stage.setScene(scene);
        stage.show();
        ParentShim.getChildren(g).addAll(rect1, rect2);
        toolkit.fireTestPulse();

        ParentShim.getChildren(g).remove(rect1);

        PickResultChooser res = new PickResultChooser();
        NodeHelper.pickNode(g, new PickRay(50, 50, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertSame(rect2, res.getIntersectedNode());
        res = new PickResultChooser();
        NodeHelper.pickNode(g, new PickRay(0, 0, 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), res);
        assertNull(res.getIntersectedNode());
    }

    @Test
    public void testNegativeIndex_Add() {
        Group g = new Group();
        g.getChildren().addAll(new Rectangle(), new Rectangle());

        int preTestSize = g.getChildren().size();

        // Adding an object at a negative or too big index should throw IndexOutOfBoundsException and not modify internal state
        assertThrows(IndexOutOfBoundsException.class, () -> g.getChildren().add(-1, new Rectangle()));
        assertEquals(preTestSize, g.getChildren().size());

        assertThrows(IndexOutOfBoundsException.class, () -> g.getChildren().add(g.getChildren().size() + 1, new Rectangle()));
        assertEquals(preTestSize, g.getChildren().size());

        // below call should throw no exception - if it does, internal state is corrupted
        g.getChildren().remove(0);
    }

    @Test
    public void testNegativeIndex_Set() {
        Group g = new Group();
        g.getChildren().addAll(new Rectangle(), new Rectangle());

        int preTestSize = g.getChildren().size();

        // Setting an object at a negative or too big index should throw IndexOutOfBoundsException and not modify internal state
        assertThrows(IndexOutOfBoundsException.class, () -> g.getChildren().set(-1, new Rectangle()));
        assertEquals(preTestSize, g.getChildren().size());

        assertThrows(IndexOutOfBoundsException.class, () -> g.getChildren().set(g.getChildren().size(), new Rectangle()));
        assertEquals(preTestSize, g.getChildren().size());

        // below call should throw no exception - if it does, internal state is corrupted
        g.getChildren().remove(0);
    }

    @Test
    public void testNegativeIndex_Remove() {
        Group g = new Group();
        g.getChildren().addAll(new Rectangle(), new Rectangle());

        int preTestSize = g.getChildren().size();

        // Removing an object at negative or too big index should throw IndexOutOfBoundsException and not modify internal state
        assertThrows(IndexOutOfBoundsException.class, () -> g.getChildren().remove(-1));
        assertEquals(preTestSize, g.getChildren().size());

        assertThrows(IndexOutOfBoundsException.class, () -> g.getChildren().remove(g.getChildren().size()));
        assertEquals(preTestSize, g.getChildren().size());

        // below call should throw no exception - if it does, internal state is corrupted
        g.getChildren().remove(0);
    }

    @Test
    public void testNullObject_AddAll() {
        Group g = new Group();
        g.getChildren().addAll(new Rectangle(), new Rectangle());

        int preTestSize = g.getChildren().size();

        // Adding a null object should throw NPE and not modify internal state
        assertThrows(NullPointerException.class, () -> g.getChildren().addAll((Collection<Node>)null));
        assertEquals(preTestSize, g.getChildren().size());

        assertThrows(NullPointerException.class, () -> g.getChildren().addAll(0, (Collection<Node>)null));
        assertEquals(preTestSize, g.getChildren().size());

        // below call should throw no exception - if it does, internal state is corrupted
        g.getChildren().remove(0);
    }

    @Test
    public void testNullObject_SetAll() {
        Group g = new Group();
        g.getChildren().addAll(new Rectangle(), new Rectangle());

        int preTestSize = g.getChildren().size();

        // Setting a null object should throw NPE and not modify internal state
        assertThrows(NullPointerException.class, () -> g.getChildren().setAll((Collection<Node>)null));
        assertEquals(preTestSize, g.getChildren().size());

        // below call should throw no exception - if it does, internal state is corrupted
        g.getChildren().remove(0);
    }

    private static final class LayoutContextParent extends MockParent {
        Runnable onInvalidation;
        int layoutCount;

        LayoutContextParent(Node... children) {
            super(children);
        }

        @Override
        protected double computePrefWidth(double height) {
            return snapSizeX(10.2);
        }

        @Override
        protected double computePrefHeight(double width) {
            return snapSizeY(10.2);
        }

        @Override
        protected void layoutChildren() {
            ++layoutCount;
            super.layoutChildren();
        }

        @Override
        protected void layoutContextInvalidated() {
            super.layoutContextInvalidated();
            Runnable callback = onInvalidation;
            onInvalidation = null;
            if (callback != null) {
                callback.run();
            }
        }
    }

    public static class MockParent extends Parent {
        public MockParent(Node... children) {
            ParentShim.getChildren(this).addAll(children);
        }
    }
}
