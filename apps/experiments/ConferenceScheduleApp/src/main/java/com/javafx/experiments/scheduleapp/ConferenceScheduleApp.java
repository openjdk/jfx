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
package com.javafx.experiments.scheduleapp;

import com.javafx.experiments.scheduleapp.control.Popover;
import com.javafx.experiments.scheduleapp.control.VirtualKeyboard;
import com.javafx.experiments.scheduleapp.data.DataService;
import com.javafx.experiments.scheduleapp.data.SessionManagement;
import com.javafx.experiments.scheduleapp.data.devoxx.DevoxxDataService;
import com.javafx.experiments.scheduleapp.data.devoxx.TestDataService;
import com.javafx.experiments.scheduleapp.model.SessionTime;
import com.javafx.experiments.scheduleapp.pages.CatalogPage;
import com.javafx.experiments.scheduleapp.pages.LoginScreen;
import com.javafx.experiments.scheduleapp.pages.SocialPage;
import com.javafx.experiments.scheduleapp.pages.SpeakersPage;
import com.javafx.experiments.scheduleapp.pages.TimelinePage;
import com.javafx.experiments.scheduleapp.pages.TracksPage;
import com.javafx.experiments.scheduleapp.pages.VenuesPage;
import com.sun.glass.ui.Screen;
import com.sun.javafx.tk.TKSceneListener;
import com.sun.javafx.tk.quantum.GlassScene;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ConferenceScheduleApp extends Application {
    private static final String os = System.getProperty("os.name");
    public static final boolean IS_BEAGLE = "Linux".equals(os) && Boolean.getBoolean("com.sun.javafx.isEmbedded");
    public static final boolean IS_MAC = "Mac OS X".equals(os);
    public static final boolean IS_WINDOWS = os.startsWith("Windows");
    public static final boolean DISABLE_AUTO_LOGOUT = "true".equalsIgnoreCase(System.getProperty("disable.auto.logout"));
    public static final boolean IS_TESTING_MODE = "true".equalsIgnoreCase(System.getProperty("test.mode"));
    public static final boolean IS_VK_DISABLED = Boolean.getBoolean("no.vk");

    private static ConferenceScheduleApp INSTANCE;
    private Popover centralPopover;
    private PageContainer pageContainer;
    private StackPane root;
    private LoginScreen loginScreen;
    private TimelinePage TIMELINE_PAGE;
    private CatalogPage CATALOG_PAGE;
    private final SessionManagement SESSION_MANAGEMENT = new SessionManagement();
    private long startTime;
    private long lastFrame;
    private int frames =0;
    private DataService dataService;
    private Scene scene;
    private ToggleButton loginLogoutButton;
    private Animation loginLogoutAnimation;
    private Timeline automatedTimer;
    private Timer autoLogoutTimer;
    private volatile long countdownTimerStart;
    private AutoLogoutLightBox lightBox;
    private VirtualKeyboard keyboard;
    private Animation keyboardSlideAnimation;
    private TKSceneListener sceneListener;
    private Text clock = new Text();
    private DateFormat dateFormat = new SimpleDateFormat("E hh:mm a");

    @Override public void start(Stage stage) throws Exception {
        INSTANCE = this;
        if(IS_TESTING_MODE) System.out.println("==============================================\n    WARNING: IN TEST MODE\n==============================================");
        
        // create data service
        dataService = IS_TESTING_MODE ? new TestDataService() : new DevoxxDataService(7);
        
        centralPopover = new Popover();
        centralPopover.setPrefWidth(400);
        lightBox = new AutoLogoutLightBox(dataService);
        lightBox.setVisible(false);
        keyboard = new VirtualKeyboard();
        loginLogoutButton = new ToggleButton();
        
        // calculate window size
        final double width = IS_BEAGLE ? Screen.getMainScreen().getWidth() : 1024;
        final double height = IS_BEAGLE ? Screen.getMainScreen().getHeight(): 600;

        // create pages
        CATALOG_PAGE = new CatalogPage(centralPopover, dataService);
        TIMELINE_PAGE = new TimelinePage(centralPopover, dataService);
        pageContainer = new PageContainer(centralPopover, lightBox,
            TIMELINE_PAGE,
            CATALOG_PAGE,
            new SocialPage(dataService),
            new SpeakersPage(centralPopover, dataService),
            new VenuesPage(centralPopover, dataService),
            new TracksPage(centralPopover, dataService)
        );
        pageContainer.setVisible(false);

        clock.setId("Clock");
        clock.setText(dateFormat.format(new Date()));
        clock.setTextOrigin(VPos.TOP);
        Timer clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                         clock.setText(dateFormat.format(new Date()));
                    }
                });
            }
        }, 20000, 20000);

        // create login/logout button
        loginLogoutButton.setId("LoginLogout");
        loginLogoutButton.getStyleClass().clear();
        loginLogoutButton.resize(75, 31);
        loginLogoutButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                showLoginScreen();
            }
        });
        pageContainer.getChildren().addAll(clock, loginLogoutButton);

        pageContainer.getChildren().addAll(centralPopover, lightBox);

        // create login screen
        loginScreen = new LoginScreen(dataService, height < 1000);
        
        // create root
        root = new StackPane() {
            @Override protected void layoutChildren() {
                final double w = getWidth();
                final double h = getHeight();
                super.layoutChildren();
                keyboard.resizeRelocate(0, h, w, w * (3.0/11.0));
                clock.setX(w - 240);
                clock.setY(9);
                loginLogoutButton.setLayoutX(w-67-12);
                loginLogoutButton.setLayoutY(5);
            }
        };
        root.getChildren().addAll(pageContainer, loginScreen, keyboard);

        // create scene
        scene = new Scene(root, width, height);

        if (!IS_VK_DISABLED) {
            keyboard.setOnAction(new EventHandler<KeyEvent>() {
                @Override public void handle(KeyEvent event) {
                    if (sceneListener == null) {
                        try {
                            GlassScene peer = (GlassScene) scene.impl_getPeer();
                            Field f = GlassScene.class.getDeclaredField("sceneListener");
                            f.setAccessible(true);
                            sceneListener = (TKSceneListener) f.get(peer);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // TODO not sure how to implement
                    sceneListener.keyEvent(
                            (EventType<KeyEvent>)event.getEventType(),
                            event.getCode().impl_getCode(),
                            event.getCharacter().toCharArray(),
                            event.isShiftDown(), false, false, false);
                }
            });
            scene.focusOwnerProperty().addListener(new ChangeListener<Node>() {
                @Override public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
                    boolean isTextOwner = false;
                    Parent parent = newValue instanceof Parent ? (Parent) newValue : null;
                    while (parent != null) {
                        if (parent instanceof TextInputControl) {
                            isTextOwner = true;
                            break;
                        }
                        parent = parent.getParent();
                    }

                    if (isTextOwner && keyboard.getTranslateY() == 0) {
                        // The focus on a text input control and therefore we must show the keyboard
                        if (keyboardSlideAnimation != null) {
                            keyboardSlideAnimation.stop();
                        }
                        TranslateTransition tx = new TranslateTransition(Duration.seconds(.4), keyboard);
                        tx.setToY(-(scene.getWidth() * (3.0 / 11.0)));
                        keyboardSlideAnimation = tx;
                        keyboardSlideAnimation.play();
                    } else if (!isTextOwner && keyboard.getTranslateY() != 0) {
                        if (keyboardSlideAnimation != null) {
                            keyboardSlideAnimation.stop();
                        }
                        TranslateTransition tx = new TranslateTransition(Duration.seconds(.4), keyboard);
                        tx.setToY(0);
                        keyboardSlideAnimation = tx;
                        keyboardSlideAnimation.play();
                    }

                    if (newValue != null) {
                        VirtualKeyboard.Type type = (VirtualKeyboard.Type) newValue.getProperties().get("vkType");
                        keyboard.setType(type == null ? VirtualKeyboard.Type.TEXT : type);
                    }
                }
            });
        }

        if (IS_BEAGLE) {
            TouchScrollEventSynthesizer ises = new TouchScrollEventSynthesizer(scene);
        }
        // beagle is really slow with background textures so use color for beagle
        Paint background = IS_BEAGLE ? Color.web("#e8eae8") :
                new ImagePattern(
                    new Image(getClass().getResource("images/rough_diagonal.png").toExternalForm()),
                    0,0,255,255,false);
        scene.setFill(background);
        scene.getStylesheets().add(
                getClass().getResource("SchedulerStyleSheet.css").toExternalForm());
        if(!IS_BEAGLE) {
            scene.getStylesheets().add(
                    getClass().getResource("SchedulerStyleSheet-Desktop.css").toExternalForm());
        }

        EventHandler<Event> resetAutoLogoutTimerHandler = new EventHandler<Event>() {
            @Override public void handle(Event event) {
                countdownTimerStart = System.currentTimeMillis();
                lightBox.hide();
            }
        };
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, resetAutoLogoutTimerHandler);
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, resetAutoLogoutTimerHandler);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, resetAutoLogoutTimerHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, resetAutoLogoutTimerHandler);
        scene.addEventFilter(ScrollEvent.ANY, resetAutoLogoutTimerHandler);
        scene.addEventFilter(TouchEvent.ANY, resetAutoLogoutTimerHandler);

        stage.setScene(scene);
        // show stage
        stage.show();
    }

    public static ConferenceScheduleApp getInstance() { return INSTANCE; }

    public SessionManagement getSessionManagement() {
        return SESSION_MANAGEMENT;
    }

    public void showLoginScreen() {
        for (com.javafx.experiments.scheduleapp.model.Event event : dataService.getEvents()) {
            SessionTime time = event.getSessionTime();
            if (time != null) time.setEvent(null);
        }
        dataService.getEvents().clear();

        if (autoLogoutTimer != null) {
            autoLogoutTimer.cancel();
            autoLogoutTimer = null;
            if (lightBox.isVisible()) {
                lightBox.setVisible(false);
            }
        }

        centralPopover.hide();

        loginScreen.setOpacity(0);
        loginScreen.setVisible(true);
        loginScreen.reset();
        // logout
        SESSION_MANAGEMENT.logout();
        pageContainer.reset();
        if (loginLogoutAnimation != null) {
            loginLogoutAnimation.stop();
            pageContainer.setVisible(true);
        }

        loginLogoutAnimation = new Timeline(
                new KeyFrame(
                    Duration.millis(800), 
                    new EventHandler<ActionEvent>() {
                        @Override public void handle(ActionEvent event) {
                            pageContainer.setVisible(false);
                            pageContainer.setCache(false);
                            loginScreen.setCache(false);
                            loginLogoutAnimation = null;
                        }
                    },
                    new KeyValue(loginScreen.opacityProperty(), 1d)
                )
        );
        loginLogoutAnimation.play();
    }
    
    public void hideLoginScreen() {
        final boolean isGuest = SESSION_MANAGEMENT.isGuestProperty().get();
        pageContainer.gotoPage(isGuest ? CATALOG_PAGE : TIMELINE_PAGE, false);
        pageContainer.setVisible(true);
        // update login button state
        loginLogoutButton.setSelected(!isGuest);
        if (loginLogoutAnimation != null) {
            loginLogoutAnimation.stop();
            loginScreen.setVisible(true);
        }

        loginLogoutAnimation = new Timeline(
                new KeyFrame(
                    Duration.millis(800), 
                    new EventHandler<ActionEvent>() {
                        @Override public void handle(ActionEvent event) {
                            loginScreen.setVisible(false);
                            loginScreen.setCache(false);
                            pageContainer.setCache(false);
                            loginLogoutAnimation = null;
                        }
                    },
                    new KeyValue(loginScreen.opacityProperty(), 0d)
                )
        );
        loginLogoutAnimation.play();

        if (!DISABLE_AUTO_LOGOUT) {
            countdownTimerStart = System.currentTimeMillis();
            autoLogoutTimer = new Timer("Auto Logout Timer", true);
            autoLogoutTimer.scheduleAtFixedRate(new TimerTask() {
                @Override public void run() {
                    long currentTime = System.currentTimeMillis();
                    long diff = currentTime - countdownTimerStart;
                    if (diff > 60000) {
                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                showLoginScreen();
                            }
                        });
                    } else if (diff > 45000) {
                        long remaining = 60000 - diff;
                        final int seconds = (int) (remaining / 1000.0);
                        Platform.runLater(
                                new Runnable() {
                                    @Override public void run() {
                                        lightBox.setSecondsLeft(seconds);
                                        lightBox.show();
                                    }
                                });
                    }
                }
            }, 1000, 1000);
        }
    }


    public static void main(String[] args) throws  Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        launch(args);
    }
}
