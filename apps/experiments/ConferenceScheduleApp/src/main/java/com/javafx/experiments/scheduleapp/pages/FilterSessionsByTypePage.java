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
import com.javafx.experiments.scheduleapp.model.SessionType;
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

/**
 * The page which allows the user to filter by session type.
 */
public class FilterSessionsByTypePage implements Popover.Page {
    private List<CheckBoxItem<SessionType>> types;
    private boolean[] originalSelection;
    private final StackPane background;
    private final Runnable onApply;
    private Popover popover;

    public FilterSessionsByTypePage(List<SessionType> sessionTypes, Runnable onApply) {
        this.onApply = onApply;
        types = new ArrayList<CheckBoxItem<SessionType>>();
        SessionType[] tmp = sessionTypes.toArray(new SessionType[sessionTypes.size()]);
        Arrays.sort(tmp, new Comparator<SessionType>() {
            @Override public int compare(SessionType o1, SessionType o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (SessionType type : tmp) {
            types.add(new CheckBoxItem<SessionType>(type.getName(), type, true));
        }
        PopoverBox<SessionType> treeBox = new PopoverBox<SessionType>();
        treeBox.getItems().addAll(types);

        BackgroundFill fill = new BackgroundFill(Color.rgb(224, 227, 230),  CornerRadii.EMPTY, Insets.EMPTY);
        background = new StackPane();
        background.setPadding(new Insets(10));
        background.getChildren().add(treeBox);
        background.setBackground(new Background(fill));
    }

    final List<SessionType> getFilteredTypes() {
        List<SessionType> filtered = new ArrayList<SessionType>();
        for (CheckBoxItem<SessionType> item : types) {
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
        return "Session Types";
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
        for (int i=0; i<types.size(); i++) {
            types.get(i).setChecked(originalSelection[i]);
        }
    }

    private void updateOriginalSelection() {
        originalSelection = new boolean[types.size()];
        for (int i=0; i<types.size(); i++) {
            originalSelection[i] = types.get(i).isChecked();
        }
    }

    public void reset() {
        for (int i=0; i<types.size(); i++) {
            types.get(i).setChecked(true);
        }
    }
}
