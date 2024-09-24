/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.ref.WeakReference;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ProgressBarSkin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;

/**
 */
public class ProgressBarSkinTest {
    private ProgressBar progressbar;
    private ProgressBarSkinMock skin;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @BeforeEach
    public void setup() {
        progressbar = new ProgressBar();
        skin = new ProgressBarSkinMock(progressbar);
        progressbar.setSkin(skin);
    }

    /**
     * Helper method to init the stage only if really needed.
     */
    private void initStage() {
        assertTrue(Toolkit.getToolkit() instanceof StubToolkit);  // Ensure StubToolkit is loaded

        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
    }

    /**
     * Test that inner bar width is in sync with its progressbar's width.
     */
    @Test
    public void testWidthListener() {
        initStage();
        // set determinate
        double progress = .5;
        progressbar.setProgress(progress);
        // make it resizable
        progressbar.setMaxWidth(2000);
        root.getChildren().setAll(progressbar);
        double stageSize = 300;
        stage.setWidth(stageSize);
        stage.setHeight(stageSize);
        stage.show();
        // fire to force layout
        Toolkit.getToolkit().firePulse();

        assertEquals(root.getWidth(), progressbar.getWidth(), 0.5, "progressbar fills root");
        Region innerBar = (Region) progressbar.lookup(".bar");
        assertEquals(progressbar.getWidth() * progress, innerBar.getWidth(), 0.5, "inner bar width updated");
    }

    WeakReference<Skin<?>> weakSkinRef;

    @Test
    public void testWidthListenerGC() {
        ProgressBar progressbar = new ProgressBar();
        progressbar.setSkin(new ProgressBarSkin(progressbar));
        weakSkinRef = new WeakReference<>(progressbar.getSkin());
        progressbar.setSkin(null);
        attemptGC(10);
        assertNull(weakSkinRef.get(), "skin must be gc'ed");
    }

    private void attemptGC(int n) {
        // Attempt gc n times
        for (int i = 0; i < n; i++) {
            System.gc();

            if (weakSkinRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                fail(e);
            }
        }
    }

    @Test public void maxWidthTracksPreferred() {
        progressbar.setPrefWidth(500);
        assertEquals(500, progressbar.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        progressbar.setPrefHeight(500);
        assertEquals(500, progressbar.maxHeight(-1), 0);
    }

    public static final class ProgressBarSkinMock extends ProgressBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ProgressBarSkinMock(ProgressBar progressbar) {
            super(progressbar);
        }

        public void addWatchedProperty(ObservableValue<?> p) {
            p.addListener(o -> {
                propertyChanged = true;
                propertyChangeCount++;
            });
        }
    }
}
