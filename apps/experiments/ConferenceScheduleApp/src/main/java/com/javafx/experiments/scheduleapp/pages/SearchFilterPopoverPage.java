/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp.pages;

import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.PopoverBox;
import com.javafx.experiments.scheduleapp.control.TreeBoxItem;
import com.javafx.experiments.scheduleapp.data.DataService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class SearchFilterPopoverPage implements Popover.Page, Runnable {
    private final PopoverBox<Popover.Page> treeBox;
    private FilterSessionsByTrackPage filterByTracks;
    private FilterSessionsByTypePage filterByTypes;
    private DataService dataService;
    private SessionFilterCriteria filter = SessionFilterCriteria.EMPTY;
    private final StackPane background;
    private Popover popover;
    private Runnable onApply;

    public SearchFilterPopoverPage(DataService dataService, Runnable onApply) {
        this.dataService = dataService;
        this.onApply = onApply;
        this.treeBox = new PopoverBox<Popover.Page>();
        BackgroundFill fill = new BackgroundFill(Color.rgb(224, 227, 230),  CornerRadii.EMPTY, Insets.EMPTY);
        background = new StackPane();
        background.setPadding(new Insets(10));
        background.getChildren().add(treeBox);
        background.setBackground(new Background(fill));
    }

    @Override public void setPopover(Popover popover) {
        if (this.popover != popover) {
            this.popover = popover;
            filterByTracks = new FilterSessionsByTrackPage(dataService.getTracks(), this);
            filterByTypes = new FilterSessionsByTypePage(dataService.getSessionTypes(), this);
            treeBox.getItems().setAll(
                    new TreeBoxItem("Tracks", popover, filterByTracks),
                    new TreeBoxItem("Session Types", popover, filterByTypes)
            );
        }
    }

    public void run() {
        // update my filter
        if (filterByTracks != null) {
            filter = SessionFilterCriteria.withTracks(filterByTracks.getFilteredTracks(), filter);
            filter = SessionFilterCriteria.withSessionTypes(filterByTypes.getFilteredTypes(), filter);
        }
        if (onApply != null) {
            onApply.run();
        }
    }

    public final SessionFilterCriteria getSessionFilterCriteria() {
        return filter;
    }

    @Override public Popover getPopover() {
        return popover;
    }

    @Override public Node getPageNode() {
        return background;
    }

    @Override public String getPageTitle() {
        return "Filter Sessions";
    }

    @Override public String leftButtonText() {
        return null;
    }

    @Override public void handleLeftButton() {

    }

    @Override public String rightButtonText() {
        return "Done";
    }

    @Override public void handleRightButton() {
        popover.hide();
    }

    @Override public void handleShown() { }
    @Override public void handleHidden() { }

    public void reset() {
        if (filterByTracks != null) filterByTracks.reset();
        if (filterByTypes != null) filterByTypes.reset();
        filter = SessionFilterCriteria.EMPTY;
    }
}
