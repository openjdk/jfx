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
import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.control.EventPopoverPage;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.PopoverTreeList;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Room;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

public class VenueRoomPage extends ScrollPane {
    private final VBox box;
    private final Text title = new Text("Title");
    private final Text subTitle = new Text("Title");
    private final Button backButton = new Button("Back");
    private final ImageView plan = new ImageView();
    private final Region roomRegion = new Region();
    private final ImagesRegion imagesRegion = new ImagesRegion(plan, roomRegion);
    private final Text sessions = new Text("Sessions");
    private final DataService dataService;
    private final Popover popover;
    private final PopoverTreeList<SessionTime> sessionList = new PopoverTreeList<SessionTime>() {
        {
            setId("venue-room-session-list");
        }
        @Override protected void itemClicked(SessionTime item) {
            if (item != null) {
                com.javafx.experiments.scheduleapp.model.Event event = item.getEvent();
                if (event == null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(item.getStart());
                    calendar.add(Calendar.MINUTE, item.getLength());
                    event = new Event(item.getSession(), item.getStart(), calendar.getTime(), item);
                }
                popover.clearPages();
                popover.pushPage(new EventPopoverPage(dataService, event, false));
                popover.show();
            }
        }
    };

    public VenueRoomPage(final VenuesPage venuesPage, Popover popover, DataService ds) {
        this.dataService = ds;
        this.popover = popover;
        box = new VBox(12) {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                // Special handling for back button
                final Insets insets = getInsets();

                double buttonPrefWidth = backButton.prefWidth(-1);
                double buttonPrefHeight = backButton.prefHeight(-1);
                backButton.resizeRelocate(insets.getLeft(), insets.getTop(), buttonPrefWidth, buttonPrefHeight);
            }
        };
        setContent(box);
        setFitToWidth(true);
        box.setPadding(new Insets(12));
        title.setFill(PINK);
        title.setFont(HUGE_FONT);
        subTitle.setFill(BLUE);
        subTitle.setFont(LARGE_FONT);
        sessions.setFill(BLUE);
        sessions.setFont(LARGE_FONT);
        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(sessionList);
        }
        VBox.setVgrow(sessionList, Priority.ALWAYS);
        box.setAlignment(Pos.TOP_CENTER);
        backButton.setGraphic(new ImageView(new Image(ConferenceScheduleApp.class.getResource("images/back-arrow.png").toExternalForm())));
        backButton.setGraphicTextGap(8);
        backButton.getStyleClass().setAll("large-gray-blue-button");
        backButton.setManaged(false);
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                venuesPage.pageTabClicked();
            }
        });
        box.getChildren().addAll(title, subTitle, backButton, imagesRegion, sessions, sessionList);
        
        roomRegion.setStyle("-fx-background-color: #00a8cc44;");
        roomRegion.setScaleShape(false);
        roomRegion.setCenterShape(false);
    }
    
    public void setRoom(Room room) {
        // Get room metadata
        RoomMetadata metadata = ROOM_METADATA.get(room.getName());
        
        title.setText(room.getName());
        if (metadata != null && metadata.floor != null) {
            subTitle.setText(room.getVenue().getName()+" "+metadata.floor+" Floor");
        } else {
            subTitle.setText(room.getVenue().getName());
        }
        ArrayList sessionTimes = new ArrayList();
        for (Session session: dataService.getSessions()) {
            for (SessionTime sessionTime: session.getSessionTimes()) {
                if (sessionTime.getRoom() == room) sessionTimes.add(sessionTime);
            }
        }
        Collections.sort(sessionTimes, new Comparator<SessionTime>() {
            @Override public int compare(SessionTime o1, SessionTime o2) {
                return o1.getStart().compareTo(o2.getStart());
            }
        });
        sessionList.getItems().setAll(sessionTimes);
        
        if (metadata != null) {
            imagesRegion.setVisible(true);
            imagesRegion.setManaged(true);
            plan.setImage(new Image(metadata.imageUrl));
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(metadata.roomPathSVG);
            roomRegion.setTranslateX(-metadata.svgOriginX);
            roomRegion.setTranslateY(-metadata.svgOriginY);
            roomRegion.setShape(svgPath);
        } else {
            imagesRegion.setVisible(false);
            imagesRegion.setManaged(false);
        }
    }
    
    private static class ImagesRegion extends Region {
        private final ImageView plan;
        private final Region roomRegion;

        public ImagesRegion(ImageView plan, Region roomRegion) {
            this.plan = plan;
            this.roomRegion = roomRegion;
            setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
            setMaxHeight(USE_PREF_SIZE);
            getChildren().addAll(plan, roomRegion);
        }

        @Override protected double computePrefWidth(double height) {
            return plan.getLayoutBounds().getWidth();
        }

        @Override protected double computePrefHeight(double width) {
            return plan.getLayoutBounds().getHeight();
        }

        @Override protected void layoutChildren() {
            final int w = (int)getWidth();
            final int left = (int)((w - plan.getLayoutBounds().getWidth())/2d);
            plan.setLayoutX(left);
            roomRegion.setLayoutX(left);
            roomRegion.resize(plan.getLayoutBounds().getWidth(), plan.getLayoutBounds().getHeight());
        }
    }
    
    private static class RoomMetadata {
        public final String floor;
        public final String imageUrl;
        public final String roomPathSVG;
        public final double svgOriginX;
        public final double svgOriginY;

        public RoomMetadata(String floor, String imageUrl, String roomPathSVG, double svgOriginX, double svgOriginY) {
            this.floor = floor;
            this.imageUrl = imageUrl;
            this.roomPathSVG = roomPathSVG;
            this.svgOriginX = svgOriginX;
            this.svgOriginY = svgOriginY;
        }
    }
    
    private static Map<String,RoomMetadata> ROOM_METADATA = new HashMap<String,RoomMetadata>();
//    
//    static {
//        // PARK 55
//        ROOM_METADATA.put("Cyril Magnin I", new RoomMetadata("4th", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-4floor.png").toExternalForm(), 
//                "M-123.71,794.58l-115.25,0.096l-29.685-30.025V524.943l144.935-0.096V794.58z", -299.957, 426.0273));
//        ROOM_METADATA.put("Cyril Magnin II/III", new RoomMetadata("4th", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-4floor.png").toExternalForm(), 
//                "M-123.71,794.58l-115.25,0.096l-29.685-30.025V524.943l144.935-0.096V794.58z", -299.957, 426.0273));
//        ROOM_METADATA.put("Mission", new RoomMetadata("4th", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-4floor.png").toExternalForm(), 
//                "M200.088,493.849h187.511v60.847H200.088V493.849z", -299.957, 426.0273));
//        
//        ROOM_METADATA.put("Embarcadero", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-3floor.png").toExternalForm(), 
//                "M594.244-0.138h106.145v190.271H484.784V42.076h62.119v21.711h8.139l4.371-4.374v-9.799h26.389v9.952l3.618,3.618h5.127L594.244-0.138z", 0,0));
//        ROOM_METADATA.put("Market Street", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-3floor.png").toExternalForm(), 
//                "M62.425,337.516l17.248-1.943h178.753V436.29H62.425V337.516z", 0,0));
//        ROOM_METADATA.put("Powell I/II", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-3floor.png").toExternalForm(), 
//                "M201.715,292.917H78.02v-65.613h123.695V292.917z", 0,0));
//        
//        ROOM_METADATA.put("Divisidero", new RoomMetadata("2nd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Parc55-2floor.png").toExternalForm(), 
//                "M571.732,161.783l-20.072,0.231l-0.232,5.634h-86.805V75.67h107.109V161.783z", -1,-1));
//        
//        // Hotel Nikko
//        ROOM_METADATA.put("Carmel I/II", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Nikko-3floor.png").toExternalForm(), 
//                "M93.69,251.459H0.816V103.316H93.69V251.459z", -0.9561, -0.6035));
//        ROOM_METADATA.put("Monterey I/II", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Nikko-3floor.png").toExternalForm(), 
//                "M21.404,36.639c-0.001,0-0.189-13.599-0.189-13.599h15.488L36.515-0.604h212.303l-1.702,48.956v30.009H0.377V36.639H21.404z", -0.9561, -0.6035));
//        ROOM_METADATA.put("Nikko Ballroom I", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Nikko-3floor.png").toExternalForm(), 
//                "M417.605,346.83l-99.055,0.001V219.155h99.055", -0.9561, -0.6035));
//        ROOM_METADATA.put("Nikko Ballroom II/III", new RoomMetadata("3rd", 
//                ConferenceScheduleApp.class.getResource("images/plans/Nikko-3floor.png").toExternalForm(), 
//                "M417.605,219.155h197.41l0.723,18.033l80.789-0.72v100.264l-80.789,0.722l0.021,9.376H417.605", -0.9561, -0.6035));
//        // Hilton San Francisco
//        
//        
//        RoomMetadata continental = new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M180.282,270.154l15.968-4.386l133.037,39.236l-14.227,4.312l49.866,15.951l-68.548,17.677L64.145,275.257l71.388-18.793L180.282,270.154z", 0,0);
//        ROOM_METADATA.put("Continental Ballroom", continental);
//        ROOM_METADATA.put("Continental Ballroom 1/2/3", continental);
//        ROOM_METADATA.put("Continental Ballroom 4", continental);
//        ROOM_METADATA.put("Continental Ballroom 5", continental);
//        ROOM_METADATA.put("Continental Ballroom 6", continental);
//        ROOM_METADATA.put("Continental Ballroom 7/8/9", continental);
//        ROOM_METADATA.put("Continental Ballroom 7/8/9 *", continental);
//        
//        ROOM_METADATA.put("Franciscan A/B/C/D", new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M455.558,175.874l-0.997,4.564l36.227,13.692v17.972l-80.728-25.103l42.361-11.981L455.558,175.874z", 0,0));
//        ROOM_METADATA.put("Yosemite A/B/C", new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M616.874,259.456l-105.403,29.239l-60.62-17.401l-15.401,4.565l-36.8-11.127l11.125-3.279l20.823-4.423l23.82-6.56l-6.419-3.281l13.977-3.565l7.276,3.565l51.632-15.406L616.874,259.456z", 0,0));
//        RoomMetadata yosimite = new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M150.516,444.173l25.498,9.788L69.133,484.607l-31.936-11.59L150.516,444.173z M196.102,502.893l100.442-35.025l-32.238-9.356l-115.077,28.932L196.102,502.893z", 0,0);
//        ROOM_METADATA.put("Golden Gate 3/4/5", yosimite);
//        ROOM_METADATA.put("Golden Gate 6/7/8", yosimite);
//        
//        ROOM_METADATA.put("Imperial Ballroom A", new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M328.896,206.426l71.634,26.283l-44.946,12.082l-80.187-23.569L328.896,206.426z", 0,0));
//        ROOM_METADATA.put("Imperial Ballroom B", new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M449.142,219.642l-48.611,13.067l-71.634-26.283l38.475-10.64L449.142,219.642z", 0,0));
//        ROOM_METADATA.put("Plaza A/B", new RoomMetadata(null, 
//                ConferenceScheduleApp.class.getResource("images/plans/Hilton.png").toExternalForm(), 
//                "M349.957,406.56l70.419,20.981l-79.617,22.132l-75.304-22.132L349.957,406.56z", 0,0));
//        
//        
////        new Room(Venue.ALL_VENUES[3],"Golden Gate 3/4/5"),
////        new Room(Venue.ALL_VENUES[3],"Golden Gate 6/7/8"),
////        new Room(Venue.ALL_VENUES[3],"Imperial Ballroom A"),
////        new Room(Venue.ALL_VENUES[3],"Imperial Ballroom B"),
////        new Room(Venue.ALL_VENUES[3],"Plaza A/B"),
////
////
////// Masonic Center"
////        new Room(Venue.ALL_VENUES[0],"Auditorium"),
////
////// Moscone West
////        new Room(Venue.ALL_VENUES[4],"2002"),
////        new Room(Venue.ALL_VENUES[4],"2003"),
////        new Room(Venue.ALL_VENUES[4],"2004"),
////        new Room(Venue.ALL_VENUES[4],"2005"),
//    }
}
