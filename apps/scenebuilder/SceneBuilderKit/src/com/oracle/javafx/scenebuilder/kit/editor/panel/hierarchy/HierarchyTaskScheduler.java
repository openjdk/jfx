/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy;

import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.BorderSide;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.scene.control.Cell;
import javafx.scene.control.TreeItem;

/**
 * Used to schedule :
 *
 * - tree items expand tasks
 * - graphic tree item place holder
 *
 * when DND within hierarchy.
 *
 * p
 * @treatAsPrivate
 */
public class HierarchyTaskScheduler {

    private final AbstractHierarchyPanelController panelController;
    private final long timerDelay = 1000;
    private Timer timer;
    private TimerTask timerTask;
    private boolean isAddEmptyGraphicTaskScheduled = false;

    public HierarchyTaskScheduler(final AbstractHierarchyPanelController c) {
        super();
        this.panelController = c;
    }

    public void scheduleExpandTask(final TreeItem<HierarchyItem> treeItem) {
        timerTask = new HierarchyTimerTask(treeItem);
        assert timerTask != null;
        getTimer().schedule(timerTask, timerDelay);
    }

    public void scheduleAddEmptyGraphicTask(final TreeItem<HierarchyItem> treeItem) {
        final HierarchyItem item = treeItem.getValue();
        assert item != null;
        final DesignHierarchyMask owner = item.getMask();
        assert owner != null;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // JavaFX data should only be accessed on the JavaFX thread. 
                // => we must wrap the code into a Runnable object and call the Platform.runLater
                Platform.runLater(() -> {
                    final TreeItem<HierarchyItem> graphicTreeItem
                            = panelController.makeTreeItemGraphic(owner, null);
                    // Add Graphic at first position
                    treeItem.getChildren().add(0, graphicTreeItem);
                    treeItem.setExpanded(true);
                    final Cell<?> cell = panelController.getCell(treeItem);
                    assert cell != null;
                    panelController.setBorder(cell, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                    isAddEmptyGraphicTaskScheduled = false;
                });
            }
        };
        assert timerTask != null;
        getTimer().schedule(timerTask, timerDelay);
        isAddEmptyGraphicTaskScheduled = true;
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            isAddEmptyGraphicTaskScheduled = false;
            timer = null;
        }
    }

    public boolean isAddEmptyGraphicTaskScheduled() {
        return isAddEmptyGraphicTaskScheduled;
    }

    private Timer getTimer() {
        if (timer == null) {
            timer = new Timer(true);
        }
        return timer;
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class HierarchyTimerTask extends TimerTask {

        private final TreeItem<HierarchyItem> treeItem;

        HierarchyTimerTask(final TreeItem<HierarchyItem> treeItem) {
            super();
            this.treeItem = treeItem;
        }

        @Override
        public void run() {
            // JavaFX data should only be accessed on the JavaFX thread. 
            // => we must wrap the code into a Runnable object and call the Platform.runLater
            Platform.runLater(() -> treeItem.setExpanded(true));
        }
    }
}
