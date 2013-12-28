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
import com.javafx.experiments.scheduleapp.PlatformIntegration;
import static com.javafx.experiments.scheduleapp.Theme.*;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.model.Tweet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RegionBuilder;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextBuilder;
import javafx.util.Callback;

public class SocialPage extends Page {
    private final HBox box = new HBox(12);
    private final TwitterList recentTweetsList = new TwitterList();
    private final ListPopulator recentListPopulator;
    private final TwitterList popularTweetsList = new TwitterList();
    private final ListPopulator popularListPopulator;;
    private final Button refreshButton1;
    private final Button refreshButton2;
    private final Region spacer1;
    private final Region spacer2;
    private Button tweetButton;

    public SocialPage(DataService dataService) {
        super("Social", null);
        
        recentListPopulator = new ListPopulator(recentTweetsList, 
                "http://search.twitter.com/search.json?q=%23"+dataService.getTwitterSearch()+"&rpp=100&result_type=recent");
        popularListPopulator = new ListPopulator(popularTweetsList, 
                IS_TESTING_MODE ? 
                "http://search.twitter.com/search.json?q="+dataService.getTwitterSearch()+"&rpp=100&geocode="+dataService.getTwitterLocalLatLon()+",1000mi" :
                "http://search.twitter.com/search.json?q="+dataService.getTwitterSearch()+"&rpp=100&geocode="+dataService.getTwitterLocalLatLon()+",1mi");
        
        if (IS_BEAGLE) {
            new TouchClickedEventAvoider(recentTweetsList);
            new TouchClickedEventAvoider(popularTweetsList);
        }
        getChildren().setAll(box);
        box.setPadding(new Insets(12));
        
        box.setAlignment(Pos.CENTER);

        VBox recentBox = new VBox(12);
        recentBox.getChildren().addAll(
            HBoxBuilder.create()
                .spacing(12)
                .alignment(Pos.CENTER)
                .children(
                    TextBuilder.create()
                        .text(dataService.getTwitterSearch())
                        .font(HUGE_FONT)
                        .fill(BLUE)
                        .build(),
                    TextBuilder.create()
                        .text("Recent")
                        .font(HUGE_FONT)
                        .fill(VLIGHT_GRAY)
                        .build(),
                    spacer1 = RegionBuilder.create()
                        .build(),
                    refreshButton1 = ButtonBuilder.create()
                        .text(null)
                        .id("refresh-button")
                        .prefWidth(162)
                        .prefHeight(48)
                        .minWidth(162)
                        .minHeight(48)
                        .onMouseClicked(recentListPopulator)
                        .build()
                )
                .build(),
            recentTweetsList
        );
        VBox.setVgrow(recentTweetsList, Priority.ALWAYS);

        VBox localBox = new VBox(12);
        localBox.getChildren().addAll(
            HBoxBuilder.create()
                .spacing(12)
                .alignment(Pos.CENTER)
                .children(
                    TextBuilder.create()
                        .text(dataService.getTwitterSearch())
                        .font(HUGE_FONT)
                        .fill(PINK)
                        .build(),
                    TextBuilder.create()
                        .text("Local")
                        .font(HUGE_FONT)
                        .fill(VLIGHT_GRAY)
                        .build(),
                    spacer2 = RegionBuilder.create()
                        .build(),
                    refreshButton2 = ButtonBuilder.create()
                        .text(null)
                        .id("refresh-button")
                        .prefWidth(162)
                        .prefHeight(48)
                        .minWidth(162)
                        .minHeight(48)
                        .onMouseClicked(popularListPopulator)
                        .build()
                )
                .build(),
            popularTweetsList
        );
        VBox.setVgrow(popularTweetsList, Priority.ALWAYS);

        box.getChildren().addAll(recentBox, localBox);
        if (PlatformIntegration.supportsSendingTweets()) {
            tweetButton = ButtonBuilder.create()
                .text(null)
                .id("tweet-button")
                .prefWidth(162)
                .prefHeight(48)
                .minHeight(48)
                .onMouseClicked(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent t) {
                        PlatformIntegration.tweet("#javaone ");
                    }
                })
                .build();
            tweetButton.getStyleClass().clear();
            box.getChildren().add(tweetButton);
        }
        refreshButton1.getStyleClass().clear();
        refreshButton2.getStyleClass().clear();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        // refresh when switchimg to tab
        visibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean newvalue) {
                if (newvalue) {
                    recentListPopulator.handle(null);
                    popularListPopulator.handle(null);
                }
            }
        });
    }
    
    @Override public void reset() {
        recentTweetsList.scrollTo(0);
        popularTweetsList.scrollTo(0);
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        box.resizeRelocate(0, 0, w, h);
    }
    
    private static class TwitterList extends ListView<Tweet> implements Callback<ListView<Tweet>, ListCell<Tweet>>{
        public TwitterList(){
            getStyleClass().setAll("twitter-list-view");
            skinClassNameProperty().set("com.sun.javafx.scene.control.skin.ListViewSkin");
            setCellFactory(this);
            setMinHeight(100);
        }

        @Override public ListCell<Tweet> call(ListView<Tweet> p) {
            return new TweetListCell();
        }
    }
    
    private static class ListPopulator implements EventHandler, Callback<Tweet[], Void>, Runnable {
        private final ListView<Tweet> list;
        private final String searchUrl;
        private volatile Tweet[] tweets;

        public ListPopulator(ListView<Tweet> list, String searchUrl) {
            this.list = list;
            this.searchUrl = searchUrl;
        }
        

        @Override public void handle(Event t) {
            PlatformIntegration.getTweetsForQuery(searchUrl, this);
        }

        @Override public Void call(Tweet[] tweets) {
            System.out.println("GOT TWEETS "+tweets.length);
            this.tweets = tweets;
            Platform.runLater(this);
            return null;
        }

        @Override public void run() {
            System.out.println("SET LIST ITEMS TO "+tweets.length);
            list.getItems().setAll(tweets);
            System.out.println("LIST ITEMS "+list.getItems().size());
        }
        
    }

    private static class TweetListCell extends ListCell<Tweet> implements Skin<TweetListCell>, EventHandler {
        private static final Pattern HTYPERLINK_PATTER = Pattern.compile("http(s)?://\\S+");
        private static final int PIC_SIZE = 48;
        private static final int GAP = 6;
        private static final int IMG_GAP = 12;
        private static final int MIN_HEIGHT = GAP + PIC_SIZE + 10 + GAP;
        private Text user = new Text();
        private Text message = new Text();
        private Text time = new Text();
        private ImageView image = new ImageView();
        private ImageView shadow = new ImageView(SHADOW_PIC);
        private Hyperlink hyperlink = null;
        private Rectangle imageBorder = new Rectangle(PIC_SIZE+6, PIC_SIZE+6,Color.WHITE);
        private Rectangle dividerLine = new Rectangle(1, 1);
        private int cellIndex;
        
        private TweetListCell() {
            super();
            //System.out.println("CREATED TimeSlot CELL " + (cellIndex));
            // we don't need any of the labeled functionality of the default cell skin, so we replace skin with our own
            // in this case using this same class as it saves memory. This skin is very simple its just a HBox container
            setSkin(this);
            getStyleClass().clear();
            user.setFont(BASE_FONT);
            user.setFill(DARK_GREY);
            user.setTextOrigin(VPos.TOP);
            message.setFont(LIGHT_FONT);
            message.setFill(DARK_GREY);
            message.setTextOrigin(VPos.TOP);
            time.setFont(BASE_FONT);
            time.setFill(BLUE);
            time.setTextOrigin(VPos.TOP);
            dividerLine.setFill(Color.web("#ddd"));
            dividerLine.setSmooth(false);
            shadow.setFitWidth(PIC_SIZE+6);
            getChildren().addAll(shadow, imageBorder, image,message,time,dividerLine);
            if (PlatformIntegration.supportsOpeningUrls()) {
                hyperlink = new Hyperlink();
                hyperlink.setOnMouseClicked(this);
                getChildren().add(hyperlink);
            }
        }

        @Override public void resize(double width, double height) {
            super.resize(width, height);
            int textWith = (int)width - GAP - PIC_SIZE - GAP - GAP;
            message.setWrappingWidth(textWith);
        }

        @Override protected double computePrefWidth(double height) {
            return 100;
        }

        @Override protected double computePrefHeight(double width) {
//            final int messageWidth = width 
            double calculatedHeight = GAP + user.getLayoutBounds().getHeight() + GAP + message.getLayoutBounds().getHeight() + GAP;
            if (hyperlink != null && hyperlink.getText() != null) {
                calculatedHeight += 2 + hyperlink.prefHeight(-1);
            }
            return (int)((calculatedHeight<MIN_HEIGHT?MIN_HEIGHT:calculatedHeight)+ 0.5d);
        }

        @Override protected void layoutChildren() {
            final double w = getWidth();
            final double h = getHeight();
            final int textLeft = GAP+PIC_SIZE+IMG_GAP;
            dividerLine.setVisible(getIndex() != 0);
            dividerLine.setWidth(w);
            image.setLayoutX(GAP);
            image.setLayoutY(GAP+3);
            imageBorder.setLayoutX(GAP-3);
            imageBorder.setLayoutY(GAP);
            shadow.setLayoutX(GAP-3);
            shadow.setLayoutY(GAP+PIC_SIZE+6);
            user.setLayoutX(textLeft);
            user.setLayoutY(GAP);
            final int userHeight = (int)user.getLayoutBounds().getHeight();
            message.setLayoutX(textLeft);
            message.setLayoutY(GAP+userHeight+GAP);
            final int messageHeight = (int)message.getLayoutBounds().getHeight();
            final int timeWidth = (int)time.getLayoutBounds().getWidth();
            time.setLayoutX(w-GAP-timeWidth);
            time.setLayoutY(GAP);
            if (hyperlink != null) {
                hyperlink.setLayoutX(textLeft);
                hyperlink.setLayoutY(GAP+userHeight+GAP+messageHeight+2);
                hyperlink.resize((int)(hyperlink.prefWidth(-1)+.5), (int)(hyperlink.prefHeight(-1)+.5));
            }
        }
        

        // CELL METHODS
        @Override protected void updateItem(Tweet tweet, boolean empty) {
            super.updateItem(tweet,empty);
            final ObservableList<Node> children = getChildren();
            if (tweet == null) { // empty item
                for (Node child: children) child.setVisible(false);
            } else {
                user.setText(tweet.getFromUserName());
                user.setVisible(true);
                String text = tweet.getText();
                Matcher matcher = HTYPERLINK_PATTER.matcher(text);
                String link = null;
                if (matcher.find()) {
                    link = matcher.group();
                }
                text = text.replaceAll("http(s)?://\\S+", "");
                message.setText(text);
                message.setVisible(true);
                time.setText("6m");
                time.setVisible(true);
                if (hyperlink != null) {
                    hyperlink.setText(link);
                    hyperlink.setVisible(true);
                }
                image.setVisible(true);
                image.setImage(new Image(tweet.getProfileImageUrl(),PIC_SIZE,PIC_SIZE,true,true,true));
                imageBorder.setVisible(true);
                shadow.setVisible(true);
                dividerLine.setVisible(true);
            }
        }

        // SKIN METHODS
        @Override public TweetListCell getSkinnable() { return this; }
        @Override public Node getNode() { return user; }
        @Override public void dispose() {}

        @Override public void handle(Event t) {
            PlatformIntegration.openUrl(hyperlink.getText());
        }
    }
    
}