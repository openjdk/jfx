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

import com.javafx.experiments.scheduleapp.ConferenceScheduleApp;
import static com.javafx.experiments.scheduleapp.ConferenceScheduleApp.*;
import com.javafx.experiments.scheduleapp.TouchClickedEventAvoider;
import com.javafx.experiments.scheduleapp.Page;
import com.javafx.experiments.scheduleapp.PlatformIntegration;
import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.ResizableWrappingText;
import com.javafx.experiments.scheduleapp.control.ScrollPaneSkin3;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Room;
import com.javafx.experiments.scheduleapp.model.Venue;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Page with all the info about the venues
 */
public class VenuesPage extends Page{
    
    private ScrollPane scrollPane = new ScrollPane();
    private Rectangle workaroundClip;
    private VenueRoomPage roomPage;

    public VenuesPage(Popover popover, final DataService ds) {
        super("Venues", ds);
        roomPage = new VenueRoomPage(this, popover, ds);
        final VBox box = new VBox(5);
        box.setPadding(new Insets(20));
        for (Venue venue: ds.getVenues()) {
            box.getChildren().add(new VenueInfo(venue));
        }
        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(scrollPane);
        }
        scrollPane.getStyleClass().clear();
        // Using ScrollPaneSkin3 to disable all the isEmbedded stuff.
        scrollPane.setSkin(new ScrollPaneSkin3(scrollPane));
        scrollPane.setContent(box);
        scrollPane.setFitToWidth(true);
        roomPage.setVisible(false);
        getChildren().setAll(scrollPane, roomPage);
        // cache the venues page when it becomes visible
        box.cacheProperty().bind(visibleProperty());
        
        // Major wicked hack for the sake of getting the page to layout right.
        Platform.runLater(new Runnable() {
            @Override public void run() {
                resize(getWidth() + 1, getHeight());
                layout();
            }
        });
        ds.getVenues().addListener(new ListChangeListener<Venue>() {
            @Override public void onChanged(Change<? extends Venue> c) {
                box.getChildren().clear();
                for (Venue venue: ds.getVenues()) {
                    box.getChildren().add(new VenueInfo(venue));
                }
            }
        });
    }

    @Override public void pageTabClicked() {
        scrollPane.setVisible(true);
        new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(scrollPane.translateXProperty(), scrollPane.getTranslateX()),
                new KeyValue(roomPage.translateXProperty(), roomPage.getTranslateX())
            ),
            new KeyFrame(Duration.millis(300),
                new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        roomPage.setCache(false);
                        roomPage.setVisible(false);
                    }
                },
                new KeyValue(scrollPane.translateXProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(roomPage.translateXProperty(), getWidth(), Interpolator.EASE_BOTH)
            )
        ).play();
    }
    
    private void showRoom(Room room) {
        roomPage.setRoom(room);
        roomPage.setTranslateX(getWidth());
        roomPage.setVisible(true);
        new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(scrollPane.translateXProperty(), scrollPane.getTranslateX()),
                new KeyValue(roomPage.translateXProperty(), roomPage.getTranslateX())
            ),
            new KeyFrame(Duration.millis(300),
                new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        scrollPane.setVisible(false);
                        roomPage.setCache(false);
                    }
                },
                new KeyValue(scrollPane.translateXProperty(), -getWidth(), Interpolator.EASE_BOTH),
                new KeyValue(roomPage.translateXProperty(), 0, Interpolator.EASE_BOTH)
            )
        ).play();
    }
    
    @Override public void reset() {
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        scrollPane.resizeRelocate(0, 0, w, h);
        roomPage.resizeRelocate(0, 0, w, h);
    }
    
    private class VenueInfo extends Region {
        private Venue venue;
        private Region venueImg;
        private ImageView shadowImg;
        private VBox box = new VBox(5);

        public VenueInfo(final Venue venue) {
            this.venue = venue;
            String venueImgUrl = venue.getImageUrl();
            venueImgUrl = venueImgUrl.indexOf(':') != -1 ? venueImgUrl : 
                    ConferenceScheduleApp.class.getResource(venue.getImageUrl()).toExternalForm();
            venueImg = new Region();
            venueImg.setStyle("-fx-background-image: url(\""+venueImgUrl+"\"); -fx-background-size: cover; -fx-border-color: white; -fx-border-width: 4px;");
            shadowImg = new ImageView(SHADOW_PIC);
            
            Text title = new Text(venue.getName());
            title.setFont(LARGE_FONT);
            title.setFill(BLUE);
            box.getChildren().addAll(title);
            
            if (PlatformIntegration.supportsMapRouting()) {
                Hyperlink address = new Hyperlink(venue.getAddress());
                address.setFont(BASE_FONT);
                address.setTextFill(DARK_GREY);
                address.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        PlatformIntegration.openMapRoute(venue.getAddress());
                    }
                });
                box.getChildren().add(address);
            } else {
                ResizableWrappingText address = new ResizableWrappingText(venue.getAddress());
                address.setFont(BASE_FONT);
                address.setFill(DARK_GREY);
                box.getChildren().add(address);
            }
            
            final List<Room> rooms =  venue.getRooms();
            if (rooms != null && rooms.size() > 0) {
                Text roomsText = new Text("Rooms:");
                roomsText.setFont(BASE_FONT);
                roomsText.setFill(GRAY);
                box.getChildren().add(roomsText);
                VBox listOfRooms = new VBox(0);
                listOfRooms.setPrefWidth(2000);
                listOfRooms.getStyleClass().add("list-of-links-transparent");
                box.getChildren().add(listOfRooms);
                for (int r=0; r<rooms.size(); r++) {
                    Room room = rooms.get(r);
                    listOfRooms.getChildren().addAll(new RoomRow(room, r!=0));
                }
            }
            
            box.setFillWidth(false);
            getChildren().addAll(venueImg, shadowImg,box);
            setMinHeight(USE_PREF_SIZE);
        }
       
        @Override protected double computePrefWidth(double height) {
            return 300;
        }

        @Override protected double computePrefHeight(double width) {
            final double boxHeight = box.prefHeight(width-180);
            return (boxHeight > 160? boxHeight : 160) + 10;
        }

        @Override protected void layoutChildren() {
            venueImg.setLayoutX(10);
            venueImg.setLayoutY(10);
            venueImg.resize(150, 150);
            shadowImg.setLayoutX(10);
            shadowImg.setLayoutY(160);
            box.setLayoutX(170);
            box.setLayoutY(10);
            box.resize(getWidth()-180, getHeight()-10);
        }
    }
    
    private class RoomRow extends Region implements EventHandler<MouseEvent>{
        private final Room room;
        private final Text roomText;
        private final Rectangle line;
        private final ImageView arrow;

        public RoomRow(Room room, boolean hasTopLine) {
            this.room = room;
            roomText = new Text(room.getName());
            roomText.setFont(BASE_FONT);
            roomText.setFill(DARK_GREY);
            roomText.setTextOrigin(VPos.CENTER);
            getChildren().add(roomText);
            arrow = new ImageView(RIGHT_ARROW);
            getChildren().add(arrow);
            if (hasTopLine) {
                line = new Rectangle(200,1,Color.web("#cacaca"));
                getChildren().add(line);
            } else {
                line = null;
            }
            setOnMouseClicked(this);
        }
        
        @Override public void handle(MouseEvent t) {
            showRoom(room);
        }

        @Override protected double computePrefWidth(double height) {
            return 150;
        }
        
        @Override protected double computePrefHeight(double width) {
            return 44;
        }

        @Override protected void layoutChildren() {
            final int width = (int)getWidth();
            final int height = (int)getHeight();
            roomText.setLayoutX(12);
            roomText.setLayoutY(height/2);
            arrow.setLayoutX(width - RIGHT_ARROW.getWidth() -12);
            arrow.setLayoutY((int)((height-RIGHT_ARROW.getHeight())/2));
            if(line!=null) line.setWidth(width);
        }
    } 
}
