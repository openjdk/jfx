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

package test.javafx.scene.layout;

import com.sun.javafx.scene.LayoutFlags;
import com.sun.javafx.scene.ParentHelper;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.ParentShim;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class RenderScaleTest {

    private static final double SIZE = 10.2;
    private final List<Stage> stages = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        for (Stage stage : stages) {
            stage.close();
        }
    }

    @Test
    public void sceneWindowLifecycleUpdatesRenderScale() {
        Stage firstStage = createStage(1.5);
        var region = new SnappingRegion();
        region.setPadding(new Insets(0.6));
        var scene = new Scene(region);

        firstStage.setScene(scene);
        assertEquals(snappedSize(1.5), region.snapSizeX(SIZE));
        assertEquals(snappedSpace(0.6, 1.5), region.snappedLeftInset());

        firstStage.setScene(null);
        assertNull(firstStage.getScene());
        assertEquals(snappedSize(1.0), region.snapSizeX(SIZE));
        assertEquals(snappedSpace(0.6, 1.0), region.snappedLeftInset());

        Stage secondStage = createStage(1.25);
        secondStage.setScene(scene);
        assertEquals(snappedSize(1.25), region.snapSizeX(SIZE));
        assertEquals(snappedSpace(0.6, 1.25), region.snappedLeftInset());
    }

    @Test
    public void sceneAndSubSceneRootChangesUpdateRenderScale() {
        var initialSubSceneRoot = new Pane();
        var subScene = new SubScene(initialSubSceneRoot, 100, 100);
        var initialSceneRoot = new Pane(subScene);
        var scene = new Scene(initialSceneRoot);
        Stage stage = createStage(1.5);
        stage.setScene(scene);

        SnappingRegion newSubSceneRoot = new SnappingRegion();
        subScene.setRoot(newSubSceneRoot);
        assertEquals(snappedSize(1.5), newSubSceneRoot.snapSizeX(SIZE));

        SnappingRegion newSceneRoot = new SnappingRegion();
        scene.setRoot(newSceneRoot);
        assertEquals(snappedSize(1.5), newSceneRoot.snapSizeX(SIZE));
    }

    @Test
    public void renderScaleChangeReachesClipsAndSubScenes() {
        var clip = new SnappingRegion();
        var subSceneRoot = new SnappingRegion();
        var subScene = new SubScene(subSceneRoot, 100, 100);
        var root = new Pane(subScene);
        root.setClip(clip);

        Stage stage = createStage(1.0);
        stage.setScene(new Scene(root));
        assertEquals(snappedSize(1.0), clip.prefWidth(-1));
        assertEquals(snappedSize(1.0), subSceneRoot.prefWidth(-1));

        stage.setRenderScaleX(1.25);
        assertEquals(snappedSize(1.25), clip.prefWidth(-1));
        assertEquals(snappedSize(1.25), subSceneRoot.prefWidth(-1));
    }

    @Test
    public void renderScaleChangeInvalidatesCacheWhenRequestLayoutIsSuppressed() {
        var region = new SuppressingRegion();
        Stage stage = createStage(1.0);
        stage.setScene(new Scene(region));
        assertEquals(snappedSize(1.0), region.prefWidth(-1));

        stage.setRenderScaleX(1.5);
        assertEquals(snappedSize(1.5), region.prefWidth(-1));
    }

    @Test
    public void snapToPixelChangeInvalidatesCacheWhenRequestLayoutIsSuppressed() {
        var region = new SuppressingRegion();
        assertEquals(snappedSize(1.0), region.prefWidth(-1));

        region.setSnapToPixel(false);
        assertEquals(SIZE, region.prefWidth(-1));
    }

    @Test
    public void layoutContextChangeUsesNormalRequestLayoutPropagation() {
        var root = new Pane();
        var parent = new RecordingPane();
        var child = new SnappingRegion();
        parent.getChildren().add(child);
        root.getChildren().add(parent);

        Stage stage = createStage(1.0);
        stage.setScene(new Scene(root));
        assertEquals(snappedSize(1.0), parent.prefWidth(-1));
        assertEquals(snappedSize(1.0), root.prefWidth(-1));
        root.layout();
        root.layout();
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(parent));
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(child));
        parent.requestLayoutCount = 0;

        child.setSnapToPixel(false);
        assertEquals(1, parent.requestLayoutCount);
        assertEquals(SIZE, parent.prefWidth(-1));
        assertEquals(SIZE, root.prefWidth(-1));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(root));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(parent));
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(child));
    }

    @ParameterizedTest
    @ValueSource(ints = {32, 128, 256})
    public void recursiveLayoutContextChangeRequestsLayoutOnlyAtRoot(int depth) {
        List<RecordingPane> nodes = createChain(depth);
        RecordingPane root = nodes.getFirst();
        Stage stage = createStage(1.0);
        stage.setScene(new Scene(root));
        root.layout();
        root.layout();

        for (RecordingPane node : nodes) {
            assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(node));
            node.resetCounts();
        }

        stage.setRenderScaleX(1.5);
        assertSingleLayoutRequest(nodes);

        // Already-dirty parents still need their measurement caches invalidated, but should
        // not cause additional ancestor walks compared with the initially clean tree.
        nodes.forEach(RecordingPane::resetCounts);
        root.setSnapToPixel(false);
        assertSingleLayoutRequest(nodes);
    }

    @Test
    public void maskedLocalSnappingPolicyChangesDoNotInvalidateSubtrees() {
        List<RecordingPane> nodes = createChain(128);
        RecordingPane root = nodes.getFirst();
        root.setSnapToPixel(false);
        nodes.forEach(RecordingPane::resetCounts);

        for (int i = 1; i < nodes.size(); i++) {
            // Exercise both the direct setter and the property's invalidation callback.
            if (i % 2 == 0) {
                nodes.get(i).snapToPixelProperty();
            }

            nodes.get(i).setSnapToPixel(false);
        }

        for (RecordingPane node : nodes) {
            assertEquals(0, node.layoutContextInvalidatedCount);
            assertEquals(0, node.requestLayoutCount);
            assertFalse(node.isSnappedToPixel());
        }
    }

    @Test
    public void snappingPolicyChangePrunesDisabledBranchesAndPreservesClipSnappingPolicy() {
        var disabledBranch = new RecordingPane();
        var descendant = new RecordingPane();
        disabledBranch.getChildren().add(descendant);
        disabledBranch.setSnapToPixel(false);
        var sibling = new RecordingPane();
        var root = new RecordingPane();
        root.getChildren().addAll(disabledBranch, sibling);
        var clip = new RecordingRegion();
        root.setClip(clip);
        List.of(root, disabledBranch, descendant, sibling).forEach(RecordingPane::resetCounts);

        root.setSnapToPixel(false);

        assertEquals(1, root.layoutContextInvalidatedCount);
        assertEquals(1, sibling.layoutContextInvalidatedCount);
        assertEquals(0, disabledBranch.layoutContextInvalidatedCount);
        assertEquals(0, descendant.layoutContextInvalidatedCount);
        assertEquals(0, clip.layoutContextInvalidatedCount);
        assertTrue(clip.isSnappedToPixel());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void fixedSizeDescendantIsLaidOutAfterLayoutContextChange(boolean managed) {
        var second = new Rectangle(10, 10);
        VBox box = createFixedSizeBox(second);
        box.setManaged(managed);
        var root = new Pane(new Pane(box));
        Stage stage = createStage(1.0);
        stage.setRenderScaleY(1.0);
        stage.setScene(new Scene(root, 200, 200));
        root.layout();
        root.layout();
        assertEquals(11.0, second.getLayoutY());
        assertEquals(LayoutFlags.CLEAN, ParentShim.getLayoutFlag(box));

        stage.setRenderScaleY(2.0);
        assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(box));
        root.layout();
        assertEquals(10.5, second.getLayoutY());
        assertEquals(100.0, box.getWidth());
        assertEquals(100.0, box.getHeight());

        root.setSnapToPixel(false);
        root.layout();
        assertEquals(10.6, second.getLayoutY(), 1e-10);
        assertEquals(100.0, box.getWidth());
        assertEquals(100.0, box.getHeight());
    }

    @Test
    public void layoutContextInvalidationSchedulesNestedSubSceneLayout() {
        var second = new Rectangle(10, 10);
        VBox box = createFixedSizeBox(second);
        var inner = new SubScene(new Pane(box), 100, 100);
        var outer = new SubScene(new Pane(inner), 100, 100);
        var root = new Pane(outer);
        Stage stage = createStage(1.0);
        stage.setRenderScaleY(1.0);
        stage.setScene(new Scene(root, 200, 200));

        // Complete the initial layout passes so each SubScene clears its separate dirty flag.
        root.layout();
        root.layout();
        assertEquals(11.0, second.getLayoutY());

        stage.setRenderScaleY(2.0);
        root.layout();
        assertEquals(10.5, second.getLayoutY());

        root.setSnapToPixel(false);
        root.layout();
        assertEquals(10.6, second.getLayoutY(), 1e-10);
    }

    @Test
    public void layoutContextInvalidatedIsCalledOnceForEveryAffectedNode() {
        var root = new RecordingRegion();
        var clip = new RecordingRegion();
        var subSceneRoot = new RecordingRegion();
        root.setClip(clip);
        ParentShim.getChildren(root).add(new SubScene(subSceneRoot, 100, 100));

        Stage stage = createStage(1.0);
        stage.setScene(new Scene(root));
        assertEquals(0, root.layoutContextInvalidatedCount);
        assertEquals(0, clip.layoutContextInvalidatedCount);
        assertEquals(0, subSceneRoot.layoutContextInvalidatedCount);

        stage.setRenderScaleX(1.5);
        assertEquals(1, root.layoutContextInvalidatedCount);
        assertEquals(1, clip.layoutContextInvalidatedCount);
        assertEquals(1, subSceneRoot.layoutContextInvalidatedCount);
    }

    @Test
    public void groupSnappingPolicyPreservesLocalBindingAndUsesCurrentScaleWhenReenabled() {
        var child = new SnappingRegion();
        child.snapToPixelProperty().bind(new SimpleBooleanProperty(true));
        var group = new Group(child);
        Stage stage = createStage(1.0);
        stage.setScene(new Scene(group));
        group.setSnapToPixel(false);
        stage.setRenderScaleX(1.5);
        assertTrue(child.isSnapToPixel());
        assertFalse(child.isSnappedToPixel());
        assertEquals(1.5, ParentHelper.getRenderScaleX(child));
        assertEquals(SIZE, child.prefWidth(-1));
        group.setSnapToPixel(true);
        assertEquals(snappedSize(1.5), child.prefWidth(-1));
        group.setSnapToPixel(false);
        group.getChildren().remove(child);
        assertTrue(child.isSnappedToPixel());
        assertEquals(snappedSize(1.0), child.prefWidth(-1));
    }

    @Test
    public void subSceneRootInheritsSnappingPolicyAndRestoresItWhenReplaced() {
        var root = new SnappingRegion();
        var subScene = new SubScene(root, 100, 100);
        var parent = new Group(subScene);
        parent.setSnapToPixel(false);
        assertFalse(root.isSnappedToPixel());
        var replacement = new SnappingRegion();
        subScene.setRoot(replacement);
        assertTrue(root.isSnappedToPixel());
        assertFalse(replacement.isSnappedToPixel());
        parent.getChildren().remove(subScene);
        assertTrue(replacement.isSnappedToPixel());
    }

    private static List<RecordingPane> createChain(int depth) {
        List<RecordingPane> nodes = new ArrayList<>();

        for (int i = 0; i < depth; i++) {
            RecordingPane node = new RecordingPane();
            if (i > 0) {
                nodes.getLast().getChildren().add(node);
            }

            nodes.add(node);
        }

        return nodes;
    }

    private static void assertSingleLayoutRequest(List<RecordingPane> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            RecordingPane node = nodes.get(i);
            assertEquals(1, node.layoutContextInvalidatedCount);
            assertEquals(i == 0 ? 1 : 0, node.requestLayoutCount);
            assertEquals(LayoutFlags.NEEDS_LAYOUT, ParentShim.getLayoutFlag(node));
        }
    }

    private static VBox createFixedSizeBox(Rectangle second) {
        var box = new VBox(0.6, new Rectangle(10, 10), second);
        box.setMinSize(100, 100);
        box.setPrefSize(100, 100);
        box.setMaxSize(100, 100);
        box.resize(100, 100);
        return box;
    }

    private Stage createStage(double renderScale) {
        var stage = new Stage();
        stage.setRenderScaleX(renderScale);
        stages.add(stage);
        return stage;
    }

    private static double snappedSize(double scale) {
        return Math.ceil(SIZE * scale) / scale;
    }

    private static double snappedSpace(double value, double scale) {
        return Math.round(value * scale) / scale;
    }

    private static class SnappingRegion extends Region {
        @Override
        protected double computePrefWidth(double height) {
            return snapSizeX(SIZE);
        }

        @Override
        protected double computePrefHeight(double width) {
            return snapSizeY(SIZE);
        }
    }

    private static final class SuppressingRegion extends SnappingRegion {
        @Override
        public void requestLayout() {}
    }

    private static final class RecordingRegion extends SnappingRegion {
        private int layoutContextInvalidatedCount;

        @Override
        protected void layoutContextInvalidated() {
            ++layoutContextInvalidatedCount;
            super.layoutContextInvalidated();
        }
    }

    private static final class RecordingPane extends Pane {
        private int requestLayoutCount;
        private int layoutContextInvalidatedCount;

        private void resetCounts() {
            requestLayoutCount = 0;
            layoutContextInvalidatedCount = 0;
        }

        @Override
        protected void layoutContextInvalidated() {
            ++layoutContextInvalidatedCount;
            super.layoutContextInvalidated();
        }

        @Override
        public void requestLayout() {
            ++requestLayoutCount;
            super.requestLayout();
        }
    }
}
