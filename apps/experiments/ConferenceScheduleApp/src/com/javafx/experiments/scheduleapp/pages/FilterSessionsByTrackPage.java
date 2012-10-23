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

import com.javafx.experiments.scheduleapp.control.CheckBoxItem;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.PopoverBox;
import com.javafx.experiments.scheduleapp.model.Track;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * The page which shows a list of checkboxes for each track.
 */
public class FilterSessionsByTrackPage implements Popover.Page {
    private List<CheckBoxItem<Track>> tracks;
    private boolean[] originalSelection;
    private final StackPane background;
    private Popover popover;
    private Runnable onApply;

    public FilterSessionsByTrackPage(List<Track> tracks, Runnable onApply) {
        this.tracks = new ArrayList<CheckBoxItem<Track>>();
        this.onApply = onApply;
        Track[] tmp = tracks.toArray(new Track[tracks.size()]);
        Arrays.sort(tmp, new Comparator<Track>() {
            @Override public int compare(Track o1, Track o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
        for (Track track : tmp) {
            CheckBoxItem<Track> checkBoxItem = new CheckBoxItem<Track>(track.getTitle(), track, true);
            Rectangle rect = new Rectangle(16, 16, Color.web(track.getColor()));
            checkBoxItem.setGraphic(rect);
            this.tracks.add(checkBoxItem);
        }
        PopoverBox<Track> treeBox = new PopoverBox<Track>();
        treeBox.getItems().addAll(this.tracks);

        BackgroundFill fill = new BackgroundFill(Color.rgb(224, 227, 230),  CornerRadii.EMPTY, Insets.EMPTY);
        background = new StackPane();
        background.setPadding(new Insets(10));
        background.getChildren().add(treeBox);
        background.setBackground(new Background(fill));
    }

    final List<Track> getFilteredTracks() {
        List<Track> filtered = new ArrayList<Track>();
        for (CheckBoxItem<Track> item : tracks) {
            if (!item.isChecked()) {
                filtered.add(item.getItem());
            }
        }
        return filtered;
    }

    @Override public void setPopover(Popover popover) {
        this.popover = popover;
    }

    @Override public Popover getPopover() {
        return popover;
    }

    @Override public Node getPageNode() {
        return background;
    }

    @Override public String getPageTitle() {
        return "Tracks";
    }

    @Override public String leftButtonText() {
        return "Cancel";
    }

    @Override public void handleLeftButton() {
        popover.popPage();
    }

    @Override public String rightButtonText() {
        return "Apply";
    }

    @Override public void handleRightButton() {
        // Update the session filter with the results of
        // this operation.
        updateOriginalSelection();
        if (onApply != null) onApply.run();
        popover.popPage();
    }

    @Override public void handleShown() {
        updateOriginalSelection();
    }

    @Override public void handleHidden() {
        // Restore all of the guys to their previous selected state
        for (int i=0; i<tracks.size(); i++) {
            tracks.get(i).setChecked(originalSelection[i]);
        }
    }

    private void updateOriginalSelection() {
        originalSelection = new boolean[tracks.size()];
        for (int i=0; i<tracks.size(); i++) {
            originalSelection[i] = tracks.get(i).isChecked();
        }
    }

    public void reset() {
        for (int i=0; i<tracks.size(); i++) {
            tracks.get(i).setChecked(true);
        }
    }
}
