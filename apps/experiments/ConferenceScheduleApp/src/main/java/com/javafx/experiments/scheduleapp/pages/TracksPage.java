/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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

import static com.javafx.experiments.scheduleapp.ConferenceScheduleApp.*;
import com.javafx.experiments.scheduleapp.TouchClickedEventAvoider;
import com.javafx.experiments.scheduleapp.Page;
import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.ResizableWrappingText;
import com.javafx.experiments.scheduleapp.control.ScrollPaneSkin3;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import com.javafx.experiments.scheduleapp.model.Track;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Page describing all the Tracks.
 */
public class TracksPage extends Page{
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox box = new VBox(12);
    private final Popover popover;

    public TracksPage(final Popover popover, DataService ds) {
        super("Tracks", ds);
        this.popover = popover;
        box.setPadding(new Insets(20));
        scrollPane.getStyleClass().clear();
        // Using ScrollPaneSkin3 to disable all the isEmbedded stuff.
        scrollPane.setSkin(new ScrollPaneSkin3(scrollPane));
        scrollPane.setContent(box);
        scrollPane.setFitToWidth(true);
        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(scrollPane);
        }
        getChildren().setAll(scrollPane);
        // cache the tracks page when it becomes visible
        box.cacheProperty().bind(visibleProperty());
        // Major wicked hack for the sake of getting the page to layout right.
        Platform.runLater(new Runnable() {
            @Override public void run() {
                resize(getWidth() + 1, getHeight());
                layout();
            }
        });
        // update tracks and listen for changes
        updateTracks();
        dataService.getTracks().addListener(new ListChangeListener<Track>() {
            @Override public void onChanged(Change<? extends Track> c) {
                updateTracks();
            }
        });
    }
    
    private void updateTracks() {
        box.getChildren().clear();
        for (final Track track: dataService.getTracks()) {
            VBox trackBox = new VBox(12);
            trackBox.setPadding(new Insets(0,0,0,10));
            ResizableWrappingText trackTitle = new ResizableWrappingText(track.getTitle());
            trackTitle.setFill(BLUE);
            trackTitle.setFont(HUGE_FONT);
            ResizableWrappingText trackDesc = new ResizableWrappingText(track.getDescription());
            trackDesc.setFill(GRAY);
            trackDesc.setFont(BASE_FONT);
            Button viewSessionsBtn = new Button();
            viewSessionsBtn.getStyleClass().setAll("view-sessions-button");
            viewSessionsBtn.setPrefSize(259, 48);
            viewSessionsBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {
                    List<SessionTime> sessionTimes = new ArrayList<SessionTime>();
                    List<Session> sessions = dataService.getSessions();
                    for(int i=0; i<sessions.size(); i++) {
                        final Session session = sessions.get(i);
                        if (session.getTrack() == track) {
                            final SessionTime[] st = session.getSessionTimes();
                            for (int j=0; j<st.length; j++) {
                                sessionTimes.add(st[j]);
                            }
                        }
                    }
                    System.out.println("Clicked on view sessions for track ["+track.getTitle()+"], found "+sessionTimes.size()+" session times");
                    popover.clearPages();
                    popover.pushPage(new SessionListPage(sessionTimes, dataService));
                    popover.show();
                }
            });
            trackBox.getChildren().addAll(trackTitle, trackDesc, viewSessionsBtn);
            trackBox.setStyle("-fx-border-color: transparent transparent transparent "+track.getColor()+"; -fx-border-width: 0 0 0 20;");
            box.getChildren().add(trackBox);
        }
    }
    
    @Override public void reset() {
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        scrollPane.resizeRelocate(0, 0, w, h);
    }
}
