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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;
import com.javafx.experiments.scheduleapp.TouchClickedEventAvoider;
import com.javafx.experiments.scheduleapp.ConferenceScheduleApp;
import com.javafx.experiments.scheduleapp.Page;
import com.javafx.experiments.scheduleapp.control.EventPopoverPage;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Event;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.Track;
import com.sun.javafx.scene.control.skin.ListViewSkin;

import static com.javafx.experiments.scheduleapp.ConferenceScheduleApp.*;
import static com.javafx.experiments.scheduleapp.Theme.*;

public class TimelinePage extends Page implements Callback<ListView<Event>, ListCell<Event>> {
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("hh:mma");
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("EEEE MMMM d");
    private static final Image DOT_IMAGE = new Image(ConferenceScheduleApp.class.getResource("images/timeline-dot.png").toExternalForm());
    private static final Image PRESENTATION_IMAGE = new Image(ConferenceScheduleApp.class.getResource("images/timeline-presentation.png").toExternalForm());
    private static final Image TOP_FADE_IMAGE = new Image(ConferenceScheduleApp.class.getResource("images/timeline-top-fade.png").toExternalForm());
    private static final Image BOTTOM_FADE_IMAGE = new Image(ConferenceScheduleApp.class.getResource("images/timeline-bottom-fade.png").toExternalForm());
    private static final Calendar TEMP_CALENDAR_1 = Calendar.getInstance();
    private static final Calendar TEMP_CALENDAR_2 = Calendar.getInstance();
    private static final int TOP = 20;
    private static final int BOTTOM = 20;
    private static final int LABEL_HEIGHT = 18;
    private static final int BUBBLE_PADDING = 5;
    private static final Font TIME_FONT = LARGE_FONT;
    private static final Font TITLE_FONT = BASE_FONT;
    private static final Color TITLE_COLOR = DARK_GREY;
    private static final Font SPEAKERS_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.BOLD, 12);
    private static final Color SPEAKERS_COLOR = Color.web("#5f5f5f");
    private static final Font SUMMARY_FONT = Font.font(DEFAULT_FONT_NAME, FontWeight.NORMAL, 11);
    private static final Color SUMMARY_COLOR = DARK_GREY;
    private static Image TOOTH_IMAGE = new Image(ConferenceScheduleApp.class.getResource("images/timeline-bubble-tooth.png").toExternalForm());
    
    static {
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("PST"));
    }
    
    private ListView<Event> timelineListView;
    private TimelineListViewSkin timelineListViewSkin;
    private DayLabel pastLabel = new DayLabel(BLUE, null);
    private DayLabel futureLabel = new DayLabel(BLUE, null);
    private Rectangle line = new Rectangle(2,10,BLUE);
    private Polyline topZigZag = new Polyline(0,0, 0,5, 10,10, -10,15, 0,20);
    private Polyline bottomZigZag = new Polyline(0,0, 0,-5, -10,-10, 10,-15, 0,-20);
    private ImageView topFade = new ImageView(TOP_FADE_IMAGE);
    private ImageView bottomFade = new ImageView(BOTTOM_FADE_IMAGE);
    private Button nowButton = new Button();
    private long currentTime = -1;
    private final VBox notLoggedInContent = new VBox(20);
    private Popover popover;

    public TimelinePage(final Popover popover, final DataService dataService) {
        super("My Timeline", dataService);
        this.popover = popover;
        timelineListView = new ListView<Event>() {
            {
                getStyleClass().clear();
                setId("timeline-list-view");
                setSkin(timelineListViewSkin = new TimelineListViewSkin(this));
                setCellFactory(TimelinePage.this);
                setItems(dataService.getEvents());
            }
        };
        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(timelineListView);
        }
        topZigZag.setStroke(BLUE);
        topZigZag.setStrokeWidth(2);
        bottomZigZag.setStroke(BLUE);
        bottomZigZag.setStrokeWidth(2);
        nowButton.setId("now-button");
        nowButton.getStyleClass().clear();
        nowButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent t) {  
                // update current time
                currentTime = dataService.getNow();
                // find first item starting after the current time
                final ObservableList<Event> items = timelineListView.getItems();
                int itemToScrollTo = -1;
                for(int i=0; i< items.size(); i++) {
                    if (items.get(i).getStart().getTime() > currentTime) {
                       itemToScrollTo = i;
                        break;
                    }
                }
                if (itemToScrollTo == -1) itemToScrollTo = items.size()-1; // time must be after all items
                timelineListView.scrollTo(itemToScrollTo);
            }
        });
        
        ImageView loggedInMessage = new ImageView(new Image(ConferenceScheduleApp.class.getResource("images/need-to-be-logged-in.png").toExternalForm()));
        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().setAll("large-light-blue-button");
        loginBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                ConferenceScheduleApp.getInstance().showLoginScreen();
            }
        });
        notLoggedInContent.setAlignment(Pos.CENTER);
        notLoggedInContent.getChildren().addAll(loggedInMessage, loginBtn);
        
        getChildren().setAll(line,topZigZag,bottomZigZag,pastLabel,futureLabel,timelineListView, topFade, bottomFade, nowButton, notLoggedInContent);
        
        ConferenceScheduleApp.getInstance().getSessionManagement().isGuestProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                setIsGuest(newValue);
            }
        });
        setIsGuest(true);
    }

    @Override public void reset() {
        timelineListView.scrollTo(0);
    }

    private void setIsGuest(boolean isGuest) {
        if (isGuest) {
            timelineListView.setVisible(false);
            line.setVisible(false);
            topZigZag.setVisible(false);
            bottomZigZag.setVisible(false);
            pastLabel.setVisible(false);
            futureLabel.setVisible(false);
            nowButton.setVisible(false);
            topFade.setVisible(false);
            bottomFade.setVisible(false);
            notLoggedInContent.setVisible(true);
        } else {
            timelineListView.setVisible(true);
            line.setVisible(true);
            topZigZag.setVisible(true);
            bottomZigZag.setVisible(true);
            pastLabel.setVisible(true);
            futureLabel.setVisible(true);
            nowButton.setVisible(true);
            topFade.setVisible(true);
            bottomFade.setVisible(true);
            notLoggedInContent.setVisible(false);
        }
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final int center = (int)(w/2d);
        final double h = getHeight();
        pastLabel.setLayoutX(center);
        pastLabel.setLayoutY(TOP);
        futureLabel.setLayoutX(center);
        futureLabel.setLayoutY(h - BOTTOM - LABEL_HEIGHT);
        line.setLayoutX(center-1);
        line.setLayoutY(TOP + LABEL_HEIGHT + 20);
        line.setHeight(h - TOP - BOTTOM - LABEL_HEIGHT - LABEL_HEIGHT - 40);
        line.setSmooth(false);
        topZigZag.setLayoutX(center);
        topZigZag.setLayoutY(TOP + LABEL_HEIGHT);
        bottomZigZag.setLayoutX(center);
        bottomZigZag.setLayoutY(h - BOTTOM - LABEL_HEIGHT);
        timelineListView.resizeRelocate(0, TOP+LABEL_HEIGHT+20, w, h - TOP - BOTTOM - LABEL_HEIGHT - LABEL_HEIGHT - 40);
        notLoggedInContent.resizeRelocate(0, TOP+LABEL_HEIGHT+20, w, h - TOP - BOTTOM - LABEL_HEIGHT - LABEL_HEIGHT - 40);
        topFade.setLayoutY(TOP+LABEL_HEIGHT+20);
        topFade.setFitWidth(w);
        bottomFade.setLayoutY(h - BOTTOM - LABEL_HEIGHT - 34);
        bottomFade.setFitWidth(w);
        
        nowButton.resize(138,48);
        nowButton.setLayoutX(w-5-138);
        nowButton.setLayoutY(h-5-48);
    }

    @Override public ListCell<Event> call(ListView<Event> listView) {
        return new TimelineListCell();
    }
    
    private class TimelineListViewSkin extends ListViewSkin<Event> implements Runnable {
        private TimelineListCell prevFirstCell = null, prevLastCell = null;
        public TimelineListViewSkin(ListView<Event> listView) {
            super(listView);
            flow.setCellChangeNotificationListener(this);
        }
        
        @Override public void run() {
            TimelineListCell firstCell = (TimelineListCell)flow.getFirstVisibleCell();
            if (firstCell != prevFirstCell && firstCell != null) {
                Event firstVisibleEvent = firstCell.getItem();
                pastLabel.setText(DAY_FORMAT.format(firstVisibleEvent.getStart()).toUpperCase());
            }
            TimelineListCell lastCell = (TimelineListCell)flow.getLastVisibleCell();
            if (lastCell != prevLastCell && lastCell != null) {
                Event lastVisibleEvent = lastCell.getItem();
                if (lastVisibleEvent == null) lastVisibleEvent = timelineListView.getItems().get(timelineListView.getItems().size()-1);
                futureLabel.setText(DAY_FORMAT.format(lastVisibleEvent.getStart()).toUpperCase());
            }
            prevFirstCell = firstCell;
            prevLastCell = lastCell;
        }
    }
    
    /**
     * Special Label node with a rounded rectangle background and a center-top origin.
     */
    private class DayLabel extends Group {
        private final Rectangle backgroundRect = new Rectangle();
        private final Text text = new Text();
        
        public DayLabel(Color color, String content) {
            text.setTextOrigin(VPos.TOP);
            text.setLayoutY(4);
            text.setFont(TITLE_FONT);
            text.setFill(Color.WHITE);
            backgroundRect.setFill(color);
            backgroundRect.setArcWidth(10);
            backgroundRect.setArcHeight(10);
            getChildren().addAll(backgroundRect, text);
            if (content != null) setText(content);
        }
        
        public final void setText(String newText) {
            text.setText(newText);
            final Bounds textBounds = text.getBoundsInParent();
            final int textWidth = (int)(textBounds.getWidth()+0.5);
            final int textHeight = (int)(textBounds.getHeight()+0.5);
            text.setLayoutX(-textWidth/2);
            backgroundRect.setLayoutX((-textWidth/2) - 5);
            backgroundRect.setWidth(textWidth + 10);
            backgroundRect.setHeight(textHeight + 6);
        }
    }
    
    private class TimelineListCell extends ListCell<Event> implements Skin<TimelineListCell> {
        private EventBubble bubble = new EventBubble();
        private Text timeText = new Text();
        private ImageView dot = new ImageView(DOT_IMAGE);
        private ImageView icon = new ImageView(PRESENTATION_IMAGE);
        private DayLabel dayLabel = new DayLabel(BLUE,null);
        private DayLabel nowLabel = new DayLabel(PINK,"NOW");
        private Line nowLine = new Line();
        private boolean isOnRight = false;
        private boolean isFirstOfDay = false;
        private boolean isJustAfterNow = false;
        private boolean isFirst = false;
        private boolean isLast = false;
        
        private TimelineListCell() {
            super();
            // we don't need any of the labeled functionality of the default cell skin, so we replace skin with our own
            // in this case using this same class as it saves memory. This skin is very simple its just a HBox container
            setSkin(this);
            getStyleClass().clear();
            timeText.setFont(TIME_FONT);
            timeText.setFill(BLUE);
            timeText.setTextOrigin(VPos.CENTER);
            nowLine.setStroke(PINK);
            nowLine.setStrokeWidth(2);
            nowLine.getStrokeDashArray().setAll(5d,8d);
            getChildren().addAll(timeText, dot, icon, dayLabel, nowLine, nowLabel);
        }

        @Override protected double computePrefWidth(double height) {
            return 100;
        }

        @Override protected double computePrefHeight(double width) {
            final Insets insets = getInsets();
            final double bubblePref = bubble.prefHeight((timelineListView.getWidth()/2)-30) + (isFirstOfDay?40:0) + (isJustAfterNow?40:0) + (isFirst||isLast?10:0);
            return insets.getTop() + bubblePref + insets.getBottom();
        }

        @Override protected void layoutChildren() {
            final int top = (isFirstOfDay?40:0) + (isJustAfterNow?40:0) + (isFirst?10:0);
            final double w = getWidth();
            final double h = getHeight() - top - (isLast?10:0);
            final int centerX = (int)(w/2d);
            final int centerY = (int)(h/2d) + top;
//            final int dayLabelWidth = (int)dayLabel.prefWidth(-1);
//            dayLabel.resizeRelocate(centerX - (int)(dayLabelWidth/2), 10, dayLabelWidth, LABEL_HEIGHT);
            if (isFirstOfDay) {
                dayLabel.setLayoutX(centerX);
                dayLabel.setLayoutY(10);
            }
            if (isJustAfterNow) {
                final int nowY = isFirstOfDay?50:10;
                nowLabel.setLayoutX(centerX);
                nowLabel.setLayoutY(nowY);
                final int nowCenterY = 1 + nowY + (int)(nowLabel.getLayoutBounds().getHeight()/2d);
                nowLine.setStartX(12);
                nowLine.setStartY(nowCenterY);
                nowLine.setEndX(w-18);
                nowLine.setEndY(nowCenterY);
            }
            dot.relocate(
                    (int)(centerX - (dot.getLayoutBounds().getWidth()/2)), 
                    (int)(centerY - (dot.getLayoutBounds().getHeight()/2)));
            final Bounds iconBounds = icon.getLayoutBounds();
            if (isOnRight) {
                bubble.resizeRelocate(centerX+20, top, w-centerX-30, h);
                icon.relocate(
                        centerX - iconBounds.getWidth() - 12, 
                        (int)(centerY - (iconBounds.getHeight()/2)));
                timeText.relocate(
                        (int)(centerX - iconBounds.getWidth() - 10 -timeText.getBoundsInParent().getWidth()-10), 
                        (int)(centerY-(timeText.getBoundsInParent().getHeight()/2)));
            } else {
                bubble.resizeRelocate(10, top, w-centerX-30, h);
                icon.relocate(
                        centerX + 12, 
                        (int)(centerY - (iconBounds.getHeight()/2)));
                timeText.relocate(
                        (int)(centerX + 10 + iconBounds.getWidth() + 10), 
                        (int)(centerY-(timeText.getBoundsInParent().getHeight()/2)));
            }
        }
        
        // CELL METHODS
        @Override protected void updateItem(Event item, boolean empty) {
            if (getItem() != item || empty) {
                // let super do its work
                super.updateItem(item, empty);
                // calculate if we are on right or left
                final int index = getIndex();
                isFirst = index == 0;
                isLast = index == (timelineListView.getItems().size()-1);
                isOnRight = (index % 2) == 0;
                bubble.setIsOnRight(isOnRight);
                // update text and bubble
    //            System.out.println("updating cell ["+cellIndex+"] to type "+(item==null?"null":item.rowType)+"  from "+oldRowType);
                if (empty) { // empty item
                    timeText.setText(null);
                    bubble.setVisible(false);
                    dayLabel.setVisible(false);
                    nowLabel.setVisible(false);
                    nowLine.setVisible(false);
                    dot.setVisible(false);
                    icon.setVisible(false);
                    isFirstOfDay = false;
                    isJustAfterNow = false;
                } else {
                    final Event prevItem = (index <= 0) ? null : timelineListView.getItems().get(index-1);
                    // see if we are first of day
                    if(index <= 0) {
                        isFirstOfDay = false;
                    } else {
                        TEMP_CALENDAR_1.setTime(prevItem.getStart());
                        TEMP_CALENDAR_2.setTime(item.getStart());
                        isFirstOfDay = TEMP_CALENDAR_1.get(Calendar.DAY_OF_YEAR) < TEMP_CALENDAR_2.get(Calendar.DAY_OF_YEAR);
                    }
                    if (isFirstOfDay) {
                        dayLabel.setText(DAY_FORMAT.format(item.getStart()).toUpperCase());
                    }
                    dayLabel.setVisible(isFirstOfDay);

                    // see if need to show now label
                    currentTime = dataService.getNow();

                    if(index <= 0) {
                        isJustAfterNow = currentTime < item.getStart().getTime();
                    } else {
                        isJustAfterNow = currentTime < item.getEnd().getTime() && currentTime > prevItem.getEnd().getTime();
                    }
                    nowLabel.setVisible(isJustAfterNow);
                    nowLine.setVisible(isJustAfterNow);

                    timeText.setText(TIME_FORMAT.format(item.getStart()));
                    bubble.setVisible(true);
                    bubble.setEvent(item);
                    dot.setVisible(true);
                    icon.setVisible(true);
                    requestLayout();
                }
            }
        }

        // SKIN METHODS
        @Override public TimelineListCell getSkinnable() { return this; }
        @Override public Node getNode() { return bubble; }
        @Override public void dispose() {}
    }
    
    private class EventBubble extends Region {
        private Text titleText = new Text();
        private Text speakersText = new Text();
        private Text summaryText = new Text();
        private ImageView tooth = new ImageView(TOOTH_IMAGE);
        private Rectangle trackColor = new Rectangle(5,5,10, 10);
        private boolean isOnRight = false;
        private Event event;

        public EventBubble() {
            getStyleClass().add("timelinev2-bubble");
            getChildren().addAll(titleText,speakersText,summaryText, trackColor, tooth);
            titleText.setFont(TITLE_FONT);
            titleText.setFill(TITLE_COLOR);
            titleText.setTextOrigin(VPos.TOP);
            speakersText.setFont(SPEAKERS_FONT);
            speakersText.setFill(SPEAKERS_COLOR);
            speakersText.setTextOrigin(VPos.TOP);
            summaryText.setFont(BASE_FONT);
            summaryText.setFill(DARK_GREY);
            summaryText.setTextOrigin(VPos.TOP);
            setPickOnBounds(true);
            setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {
                    if (event != null) {
                        EventPopoverPage page = new EventPopoverPage(dataService, event, false);
                        popover.clearPages();
                        popover.pushPage(page);
                        popover.show();
                    }
                }
            });
        }

        public void setIsOnRight(boolean isOnRight) {
            this.isOnRight = isOnRight;
        }

        @Override protected double computePrefWidth(double height) {
            return 100;
        }

        @Override protected double computePrefHeight(double width) {
            int textWrapWidth = (int) width - 20 - BUBBLE_PADDING;
            
            // TEMPORARY CODE - textWrapWidth should be the same number that is computed in layoutChildren()
            // but for some reason, it oscillates back and forth by 1 pixel causing wrapping to occur.
            // The fix is to bump it up by one pixel and avoid the work
            textWrapWidth++;
            
            titleText.setWrappingWidth(textWrapWidth);
            speakersText.setWrappingWidth(textWrapWidth);
            summaryText.setWrappingWidth(textWrapWidth);
            return (int)(BUBBLE_PADDING + titleText.getBoundsInParent().getHeight() + 
                         BUBBLE_PADDING + speakersText.getBoundsInParent().getHeight() + 
                         BUBBLE_PADDING + summaryText.getBoundsInParent().getHeight() + 
                         BUBBLE_PADDING + 0.5);
        }

        @Override protected void layoutChildren() {
            final int w = (int)getWidth();
            final int h = (int)getHeight();
            // layout tooth
            if (isOnRight) {
                tooth.setScaleX(1);
                tooth.relocate(
                        -tooth.getLayoutBounds().getWidth()+1, 
                        (h - tooth.getLayoutBounds().getHeight())/2);
            } else {
                tooth.setScaleX(-1);
                tooth.relocate(
                        w-1, 
                        (h - tooth.getLayoutBounds().getHeight())/2);
            }
            // layout color bar
            trackColor.setHeight(h-10);
            // layout text
            final int textWrapWidth = w - 20 - BUBBLE_PADDING;
            titleText.setWrappingWidth(textWrapWidth);
            speakersText.setWrappingWidth(textWrapWidth);
            summaryText.setWrappingWidth(textWrapWidth);
            titleText.relocate(20, BUBBLE_PADDING);
            final int titleHeight = (int)(titleText.getBoundsInParent().getHeight()+0.5);
            speakersText.relocate(20, BUBBLE_PADDING + titleHeight + BUBBLE_PADDING);
            final int speakersHeight = (int)(speakersText.getBoundsInParent().getHeight()+0.5);
            summaryText.relocate(20, BUBBLE_PADDING + titleHeight + BUBBLE_PADDING + speakersHeight + BUBBLE_PADDING);
        }
        
        public void setEvent(Event event) {
            this.event = event;
            Session session = event.getSession();
            if (session != null) {
                titleText.setText(session.getAbbreviation()+" :: "+session.getTitle());
                speakersText.setText(session.getSpeakersDisplay());
                summaryText.setText(event.getSessionTime().getRoom().toString());
                Track track = session.getTrack();
                trackColor.setFill(track==null? Color.gray(0.9) : Color.web(track.getColor()));
                speakersText.setVisible(true);
            } else if (event != null) {
                titleText.setText(event.getTitle());
                if (event.getAttendees() != null) {
                    String attendees = "";
                    for (String attendee: event.getAttendees()) {
                        if (attendees.length() != 0) attendees += ", ";
                        attendees += attendee;
                    }
                    speakersText.setText(attendees);
                    speakersText.setVisible(true);
                } else {
                    speakersText.setVisible(false);
                }
                summaryText.setText(event.getLocation()==null?"":event.getLocation());
                trackColor.setFill(Color.BLACK);
            } else {
                titleText.setText("");
                summaryText.setText("");
            }
            requestLayout();
        }
    }
}
