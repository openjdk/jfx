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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import com.javafx.experiments.scheduleapp.TouchClickedEventAvoider;
import com.javafx.experiments.scheduleapp.Page;
import com.javafx.experiments.scheduleapp.control.EventPopoverPage;
import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.ResizableWrappingText;
import com.javafx.experiments.scheduleapp.control.SearchBox;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Session;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import com.javafx.experiments.scheduleapp.model.Speaker;
import com.sun.javafx.scene.control.skin.FXVK;

import static com.javafx.experiments.scheduleapp.ConferenceScheduleApp.*;
import static com.javafx.experiments.scheduleapp.Theme.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;

/**
 * Page showing searchable list of all speakers at the conference
 */
public class SpeakersPage extends Page implements ChangeListener<String> {
    private static DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("hh:mma EEE");
    private static final Pattern HYPERLINK_PATTERN = Pattern.compile("http(s)?://\\S+");
    private static final int PIC_SIZE = 48;
    private static final int GAP = 6;
    private static final int IMG_GAP = 12;
    private static final int MIN_HEIGHT = GAP + PIC_SIZE + 10 + GAP;
    private static final int TEXT_LEFT = GAP+PIC_SIZE+IMG_GAP;
    
    private final SpeakerList speakersList = new SpeakerList();
    private final SearchBox searchBox = new SearchBox();
    private final Map<Speaker,Image> speakerImageCache = new HashMap<>();
    
    /** 
     * index of currently expanded cell. We need to keep tack of the cell that 
     * is expanded so that when you scroll away from it and its cell is reused 
     * then scroll back we can put the cell back in the expanded state.
     */
    private int expandedCellIndex = -1;
    /**
     * the currently expanded cell or null if no cell is expanded. This is used 
     * to make sure only once cell is expanded at a time. So when we expand a 
     * new cell we can collapse this one.
     */
    private SpeakerListCell expandedCell = null;

    private Popover popover;

    public SpeakersPage(Popover popover, DataService dataService) {
        super("Speakers", dataService);
        this.popover = popover;
        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(speakersList);
        }
        speakersList.setItems(dataService.getSpeakers());
        getChildren().setAll(speakersList,searchBox);
        searchBox.textProperty().addListener(this);
        
        // HORRIFIC!! The problem is that on Beagle right now, we're not seeing the
        // virtual keyboard layer getting hidden. This is likely a bug in the window
        // hardware layer support. This code will just move the keyboard out of the way.
        searchBox.focusedProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                Iterator<Window> itr = Window.impl_getWindows();
                while (itr.hasNext()) {
                    Window win = itr.next();
                    Object obj = win.getScene().getRoot().lookup(".fxvk");
                    if (obj instanceof FXVK) {
                        FXVK keyboard = (FXVK) obj;
                        System.err.println("Found virtual keyboard");
                        if (searchBox.isFocused()) {
                            keyboard.setVisible(true);
                            keyboard.setTranslateX(0);
                        } else {
                            keyboard.setVisible(false);
                            keyboard.setTranslateX(2000);
                        }
                    }
                }
            }
        });
    }
    
    @Override public void reset() {
        searchBox.setText("");
        speakersList.scrollTo(0);
    }

    @Override protected void layoutChildren() {
        final int w = (int)getWidth() - 24;
        final int h = (int)getHeight() - 24;
        searchBox.resize(w,30);
        searchBox.setLayoutX(12);
        searchBox.setLayoutY(12);
        speakersList.resize(w,h - 42);
        speakersList.setLayoutX(12);
        speakersList.setLayoutY(53);
    }
    
    /**
     * Handle text searching
     */
    @Override public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
        final ObservableList<Speaker> items = speakersList.getItems();
        long start = System.currentTimeMillis();
        if (newValue == null || newValue.length() == 0) {
            items.setAll(dataService.getSpeakers());
        } else {
            final List<Speaker> speakers = dataService.getSpeakers();
            final ArrayList<Speaker> results = new ArrayList(speakers.size());
            final char[] search = newValue.toLowerCase().toCharArray();
            for(int i=0; i < speakers.size(); i++) {
                final Speaker s = speakers.get(i);
                final String first = s.firstName;
                boolean match = true;
                final int max = first.length() < search.length ? first.length() : search.length;
                for (int c=0; c < max; c++) {
                    if(Character.toLowerCase(first.charAt(c)) != search[c]) {
                        match = false;
                        break;
                    }
                }
                if (match == false) {
                    match = true;
                    final String last = s.lastName;
                    final int maxl = last.length() < search.length ? last.length() : search.length;
                    for (int c=0; c < maxl; c++) {
                        if(Character.toLowerCase(last.charAt(c)) != search[c]) {
                            match = false;
                            break;
                        }
                    }
                }
                if (match) results.add(s);
            }
            System.out.println("Setting data..."); 
            long t0 = System.currentTimeMillis(); 
            items.setAll(results);
            //ObservableList<Speaker> empty = FXCollections.observableArrayList(); 
            //speakersList.setItems(empty); 
            //items.clear(); 
            //speakersList.setItems(FXCollections.observableArrayList(results)); 
            long t1 = System.currentTimeMillis(); 
            System.out.println("Done: " + (t1 - t0)); 

        }
        long end = System.currentTimeMillis();
        System.out.println("search took = "+(end-start));
        // clear expanded cells
        expandedCellIndex = -1;
        expandedCell = null;
    }
    
    
    /**
     * Helper method used to update a session row in speakers extended info to 
     * show or not show a star for when it added to sessions to attend.
     * 
     * @param sessionTitle The label to add/remove star from
     * @param show true if the star should be added to the label, false if it 
     *             should be removed.
     */
    private static void updateStar(Label sessionTitle, boolean show) {
        if(show) {
            ImageView star = new ImageView(STAR);
            sessionTitle.setGraphic(star);
            sessionTitle.setContentDisplay(ContentDisplay.RIGHT);
        } else {
            sessionTitle.setGraphic(null);
        }
    }
    
    /**
     * Custom list for speakers, will all standard CSS removed an using our 
     * custom SpeakerListCell.
     */
    private class SpeakerList extends ListView<Speaker> implements Callback<ListView<Speaker>, ListCell<Speaker>>{
        public SpeakerList(){
            getStyleClass().setAll("twitter-list-view");
//            skinClassNameProperty().set("com.sun.javafx.scene.control.skin.ListViewSkin");
            setSkin(new com.sun.javafx.scene.control.skin.ListViewSkin(this));
            setCellFactory(this);
            // hack workaround for cell sizing
            Node node = lookup(".clipped-container");
            if (node != null) node.setManaged(true);
        }

        @Override public ListCell<Speaker> call(ListView<Speaker> p) {
            return new SpeakerListCell();
        }
    }

    /**
     * The main body of the speaker list cell. This is separate from the cell 
     * so that it can be cached and not need to be updated while the cells clip 
     * is changing during expansion.
     */
    private final static class SpeakerListCellBody extends Region {
        private final Text name = new Text();
        private final Text company = new Text();
        private final ImageView image = new ImageView();
        private final ImageView shadow = new ImageView(SHADOW_PIC);
        private final Rectangle imageBorder = new Rectangle(PIC_SIZE+6, PIC_SIZE+6,Color.WHITE);
        private final Rectangle dividerLine = new Rectangle(1, 1);

        public SpeakerListCellBody() {
            name.setFont(LARGE_FONT);
            name.setFill(DARK_GREY);
            name.setTextOrigin(VPos.CENTER);
            company.setFont(LARGE_LIGHT_FONT);
            company.setFill(GRAY);
            company.setTextOrigin(VPos.CENTER);
            dividerLine.setFill(Color.web("#ddd"));
            dividerLine.setSmooth(false);
            shadow.setFitWidth(PIC_SIZE+6);
            image.setFitWidth(PIC_SIZE);
            image.setFitHeight(PIC_SIZE);
            getChildren().addAll(name,company,dividerLine,shadow,imageBorder,image);
        }
        
        @Override protected void layoutChildren() {
            final int w = (int)getWidth();
            final int h = (int)getHeight();
            final int centerY = (int)(MIN_HEIGHT/2d);
            dividerLine.setWidth(w);
            image.setLayoutX(GAP);
            image.setLayoutY(GAP+3);
            imageBorder.setLayoutX(GAP-3);
            imageBorder.setLayoutY(GAP);
            shadow.setLayoutX(GAP-3);
            shadow.setLayoutY(GAP+PIC_SIZE+6);
            name.setLayoutX(TEXT_LEFT);
            name.setLayoutY(centerY-13);
            company.setLayoutX(TEXT_LEFT);
            company.setLayoutY(centerY+13);
        }
    }
    
    /**
     * The extended info section of the speaker list cell. This is separate from
     * the cell so that it can be cached and not need to be updated while the 
     * cells clip is changing during expansion.
     */
    private final static class SpeakerListCellExtended extends GridPane {
        private final VBox sessionsList = new VBox();
        private final Text jobTitleText = new Text("TITLE");
        private final ResizableWrappingText jobTitle = new ResizableWrappingText();
        private final Text bioText = new Text("BIO");
        private final ResizableWrappingText bio = new ResizableWrappingText();

        public SpeakerListCellExtended() {
            // create extended content
            setVgap(12);
            getColumnConstraints().setAll(new ColumnConstraints(TEXT_LEFT-GAP));
            jobTitleText.setFont(SMALL_FONT);
            jobTitleText.setFill(BLUE);
            GridPane.setConstraints(jobTitleText, 0, 0,1,1, HPos.LEFT, VPos.TOP);
            jobTitle.setFont(LIGHT_FONT);
            jobTitle.setFill(DARK_GREY);
            GridPane.setConstraints(jobTitle, 1, 0);
            bioText.setFont(SMALL_FONT);
            bioText.setFill(BLUE);
            GridPane.setConstraints(bioText, 0, 1,1,1, HPos.LEFT, VPos.TOP);
            bio.setFont(LIGHT_FONT);
            bio.setFill(DARK_GREY);
            GridPane.setConstraints(bio, 1, 1);
            Text sessionsText = new Text("SESSIONS");
            sessionsText.setFont(SMALL_FONT);
            sessionsText.setFill(BLUE);
            GridPane.setConstraints(sessionsText, 0, 2,1,1, HPos.LEFT, VPos.TOP);
            sessionsList.getStyleClass().setAll("speaker-session-list");
            sessionsList.setFillWidth(true);
            GridPane.setConstraints(sessionsList, 1, 2,1,1, HPos.LEFT, VPos.TOP, Priority.ALWAYS, Priority.NEVER);
            
            getChildren().addAll(jobTitleText, jobTitle, bioText, bio, sessionsText,sessionsList);
        }
    }
    
    /**
     * Custom list cell for the speakers list. It uses a pattern to avoid 
     * standard list cell skin to have minimal overhead.
     */
    private class SpeakerListCell extends ListCell<Speaker> implements Skin<SpeakerListCell>, EventHandler {
        private final SpeakerListCellBody body = new SpeakerListCellBody();
        private final SpeakerListCellExtended expandedContent = new SpeakerListCellExtended();
        private final ImageView arrow = new ImageView(RIGHT_ARROW);
        private int cellIndex;
        private final Rectangle clip = new Rectangle();
        private final SimpleDoubleProperty expansion = new SimpleDoubleProperty(0) {
            @Override protected void invalidated() {
                super.invalidated();
                requestLayout();
            }
        };
        
        private SpeakerListCell() {
            super();
            // we don't need any of the labeled functionality of the default cell skin, so we replace skin with our own
            // in this case using this same class as it saves memory. This skin is very simple its just a HBox container
            setSkin(this);
            getStyleClass().clear();
            arrow.rotateProperty().bind(new DoubleBinding() {
                { bind(expansion); }
                @Override protected double computeValue() {
                    return 90 - (180*expansion.get());
                }
            });
            clip.setSmooth(false);
            setClip(clip);
            getChildren().addAll(arrow);
            setOnMouseClicked(this);
            setPickOnBounds(true);
        }

        @Override public Orientation getContentBias() {
            return Orientation.HORIZONTAL;
        }

        @Override public void resize(double width, double height) {
            super.resize(width, height);
            clip.setWidth(width);
            clip.setHeight(height);
        }
        
        private Timeline expansionTimeline = null;
        
        @Override public void handle(Event t) {
            final double e = this.expansion.get();
            expandCollapse(this.expansion.get() < 1);
        }
        
        private void expandCollapse(boolean expand) {
            if (expansionTimeline != null) expansionTimeline.stop();
            if (expand) {
                if (expandedCell != null) expandedCell.expandCollapse(false);
                expandedCellIndex = getIndex();
                expandedCell = this;
                expansionTimeline = new Timeline(
                    new KeyFrame(Duration.millis(200), new KeyValue(expansion, 1, Interpolator.EASE_BOTH))
                );
                expansionTimeline.play();
            } else {
                expansionTimeline = new Timeline(
                    new KeyFrame(Duration.millis(200), new KeyValue(expansion, 0, Interpolator.EASE_BOTH))
                );
                expansionTimeline.setOnFinished(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        if (getIndex() == expandedCellIndex) {
                            expandedCellIndex = -1;
                            expandedCell = null;
                        }
                    }
                });
                expansionTimeline.play();
            }
        }

        @Override protected double computePrefWidth(double height) {
            return 100;
        }

        @Override protected double computePrefHeight(double width) {
            width = speakersList.getWidth();
            double headerHeight = MIN_HEIGHT;
            final double e = this.expansion.get();
            if (e == 0) {
                return (int) (headerHeight + 0.5d);
            } else {
                final int textLeft = GAP+PIC_SIZE+IMG_GAP;
                final double expandedContentHeight = expandedContent.prefHeight(width - textLeft - GAP);
                return (int) (headerHeight + (expandedContentHeight * e) + GAP + 0.5d);
            }
        }

        @Override protected void layoutChildren() {
            final int w = (int)getWidth();
            final int h = (int)getHeight();
            final int centerY = (int)(MIN_HEIGHT/2d);
            body.resize(w, MIN_HEIGHT);
            arrow.setLayoutX(w-GAP-7);
            arrow.setLayoutY(centerY-4);
            if (this.expansion.get() > 0) {
                if (expandedContent.getParent() == null) {
                    getChildren().add(expandedContent);
                }
                expandedContent.setLayoutX(GAP);
                expandedContent.setLayoutY(MIN_HEIGHT);
                final int expandedContentWidth = w - GAP - GAP;
                final int expandedContentHeight = (int)expandedContent.prefHeight(expandedContentWidth);
                expandedContent.resize(expandedContentWidth, expandedContentHeight);
            } else {
                getChildren().remove(expandedContent);
            }
        }
        
        @Override protected Node impl_pickNodeLocal(double localX, double localY) {
            if (contains(localX, localY)) {
                if (this.expansion.get() > 0) {
                    final Node superPick = super.impl_pickNodeLocal(localX, localY);
                    if (superPick != null) return superPick;
                }
                return this;
            }
            return null;
        }

        // CELL METHODS
        @Override protected void updateItem(Speaker speaker, boolean empty) {
            super.updateItem(speaker,empty);
            final ObservableList<Node> children = getChildren();
            body.dividerLine.setVisible(getIndex() != 0);
            if (speaker == null) { // empty item
                for (Node child: children) child.setVisible(false);
            } else {
                body.name.setText(speaker.getFullName() + (speaker.isRockStar()?" (Rock Star)":""));
                body.company.setText(speaker.getCompany());
                final String bioStr = speaker.getBio();
                if (bioStr == null || bioStr.length() == 0) {
                    expandedContent.bioText.setVisible(false);
                    expandedContent.bioText.setManaged(false);
                    expandedContent.bio.setVisible(false);
                    expandedContent.bio.setManaged(false);
                } else {
                    expandedContent.bioText.setVisible(true);
                    expandedContent.bioText.setManaged(true);
                    expandedContent.bio.setVisible(true);
                    expandedContent.bio.setManaged(true);
                    expandedContent.bio.setText(speaker.getBio());
                }
                final String jobTitleStr = speaker.getJobTitle();
                if (jobTitleStr == null || jobTitleStr.length() == 0) {
                    expandedContent.jobTitleText.setVisible(false);
                    expandedContent.jobTitleText.setManaged(false);
                    expandedContent.jobTitle.setVisible(false);
                    expandedContent.jobTitle.setManaged(false);
                } else {
                    expandedContent.jobTitleText.setVisible(true);
                    expandedContent.jobTitleText.setManaged(true);
                    expandedContent.jobTitle.setVisible(true);
                    expandedContent.jobTitle.setManaged(true);
                    expandedContent.jobTitle.setText(speaker.getJobTitle());
                }
                if (speaker.getImageUrl() != null) {
                    Image img = speakerImageCache.get(speaker);
                    if (img == null) {
                        img = new Image(speaker.getImageUrl(),PIC_SIZE, PIC_SIZE,false,true, true);
                        speakerImageCache.put(speaker,img);
                    }
                    body.image.setImage(img);
                } else {
                    body.image.setImage(DUKE_48);
                }  
                if (getIndex() == expandedCellIndex) {
                    expansion.set(1);
                } else if (expansion.get() == 1) {
                    expansion.set(0);
                }
                arrow.setVisible(true);
                body.setVisible(true);
                expandedContent.sessionsList.getChildren().clear();
                boolean first = true;
                for(final Session session: speaker.getSessions()) {
                    for (final SessionTime sessionTime: session.getSessionTimes()) {
                        Label title = new Label(session.getTitle()+" @"+DATE_TIME_FORMAT.format(sessionTime.getStart()));
                        title.setFont(BASE_FONT);
                        title.setTextFill(DARK_GREY);
                        title.setPrefHeight(44);
                        title.setMaxWidth(Double.MAX_VALUE);
                        title.setOnMouseClicked(new SessionClickHandler(session, sessionTime, title));
                        updateStar(title, sessionTime.getEvent() != null);
                        expandedContent.sessionsList.getChildren().add(title);
                        if (!first) {
                            title.getStyleClass().add("session-list-item");
                        }
                        first = false;
                    }
                    
                }
            }
        }

        // SKIN METHODS
        @Override public SpeakerListCell getSkinnable() { return this; }
        @Override public Node getNode() { return body; }
        @Override public void dispose() {}
    }
    
    private class SessionClickHandler implements EventHandler<MouseEvent>, Runnable {
        private final Session session;
        private final SessionTime sessionTime;
        private final Label sessionTitle;

        public SessionClickHandler(Session session, SessionTime sessionTime, Label sessionTitle) {
            this.session = session;
            this.sessionTime = sessionTime;
            this.sessionTitle = sessionTitle;
        }

        @Override public void handle(MouseEvent t) {
            t.consume();
            // get/create event
            com.javafx.experiments.scheduleapp.model.Event event = sessionTime.getEvent();
            if (event == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(sessionTime.getStart());
                calendar.add(Calendar.MINUTE, sessionTime.getLength());
                event = new com.javafx.experiments.scheduleapp.model.Event(session, sessionTime.getStart(), calendar.getTime(), sessionTime);
            }
            // show popover
            EventPopoverPage page = new EventPopoverPage(dataService, event, false);
            popover.clearPages();
            popover.pushPage(page);
            popover.show();
        }

        @Override public void run() {
            updateStar(sessionTitle, sessionTime.getEvent() != null);
        }
        
    }
}
