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

import com.javafx.experiments.scheduleapp.ConferenceScheduleApp;
import com.javafx.experiments.scheduleapp.control.VirtualKeyboard;
import com.javafx.experiments.scheduleapp.data.DataService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * The LoginScreen is displayed whenever the user needs to login.
 */
public class LoginScreen extends Region {
    private final boolean small;
    private final Image strapImage;
    private final Image titleImage;
    private final Badge badge;
    private final DataService dataService;
    
    public LoginScreen(DataService dataService, boolean small) {
        this.small = small;
        setStyle("-fx-background-image: url(\""+dataService.getLoginBackgroundImageUrl()+"\"); -fx-background-size: cover;");
        
        this.strapImage = new Image(ConferenceScheduleApp.class.getResource("images/login-badge-strap"+(small?"-SMALL":"")+".png").toExternalForm());
        this.titleImage = new Image(ConferenceScheduleApp.class.getResource("images/login-title"+(small?"-SMALL":"")+".png").toExternalForm());
        this.badge = new Badge();
        getChildren().addAll(badge);
        this.dataService = dataService;
    }

    @Override protected void layoutChildren() {
        final int w = (int)getWidth();
        final int h = (int)getHeight();
        if (small) {
            badge.resize(466, 361);
            badge.setLayoutX((int) ((w - 466) / 2d));
            badge.setLayoutY(22);
        } else {
            badge.resize(465, 646);
            badge.setLayoutX((int) ((w - 465) / 2d));
            badge.setLayoutY(100);
        }
    }

    public void reset() {
        badge.messageLabel.setVisible(false);
        badge.progressPane.setVisible(false);
        badge.progressBar.progressProperty().unbind();
        badge.progressBar.setProgress(0);
        badge.inputPane.setVisible(true);
        badge.userNameField.clear();
        badge.passwordField.clear();
        badge.loginButton.setDisable(false);
        badge.guestButton.setDisable(false);
    }

    private static ExecutorService LOGIN_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread th = new Thread(r);
            th.setDaemon(true);
            th.setName("Login Thread");
            return th;
        }
    });

    private class Badge extends Region {
        private ImageView strap = new ImageView(strapImage);
        private ImageView title = new ImageView(titleImage);
        private ProgressBar progressBar = new ProgressBar();
        private TextField userNameField = new TextField();
        private PasswordField passwordField = new PasswordField();
        private Label messageLabel = new Label();
        private Button guestButton = new Button();
        private Button loginButton = new Button();
        private Group progressPane = new Group();
        private Group inputPane = new Group();
        private Animation fadeAnimation = null;
        private Task<Void> loginTask = null;
        
        public Badge() {
            setId("LoginBadge");
            getChildren().addAll(strap, title, inputPane, progressPane, guestButton, loginButton);
            guestButton.getStyleClass().clear();
            loginButton.getStyleClass().clear();
            progressBar.getStyleClass().clear();
            progressBar.setId("LoginProgressBar");
            guestButton.setId("LoginGuestBtn");
            loginButton.setId("LoginLoginBtn");
            userNameField.setPromptText("Username or Email");
            userNameField.getProperties().put("vkType", VirtualKeyboard.Type.EMAIL);
            passwordField.setPromptText("Password");
//            userNameField.setText("steve@widgetfx.org");
//            passwordField.setText("Foobar123");

            passwordField.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent actionEvent) {
                    login(false);
                }
            });
            messageLabel.getStyleClass().add("LoginMessageBox");
            messageLabel.setVisible(false);
            loginButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    login(false);
                }
            });
            guestButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    login(true);
                }
            });

            inputPane.getChildren().addAll(userNameField, passwordField, messageLabel);
            inputPane.setAutoSizeChildren(false);
            progressPane.getChildren().add(progressBar);
            progressPane.setVisible(false);
            progressPane.setAutoSizeChildren(false);
        }

        private void login(boolean guest) {
            loginButton.setDisable(true);
            guestButton.setDisable(true);
            messageLabel.setVisible(false);

            if (loginTask != null) {
                loginTask.cancel();
                progressBar.progressProperty().unbind();
            }

            if (guest) {
                ConferenceScheduleApp.getInstance().getSessionManagement().loginGuest();
                ConferenceScheduleApp.getInstance().hideLoginScreen();
            } else {
                loginTask = dataService.login(userNameField.getText(), passwordField.getText());
                loginTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override public void handle(WorkerStateEvent workerStateEvent) {
                        crossFade(inputPane, progressPane);
                        ConferenceScheduleApp.getInstance().getSessionManagement().login();
                        ConferenceScheduleApp.getInstance().hideLoginScreen();
                    }
                });
                loginTask.setOnFailed( new EventHandler<WorkerStateEvent>() {
                    @Override public void handle(WorkerStateEvent workerStateEvent) {
                        crossFade(inputPane, progressPane);
                        progressBar.progressProperty().unbind();
                        progressBar.setProgress(0);
                        messageLabel.setVisible(true);
                        messageLabel.setText("User name or password are incorrect");
                        badge.loginButton.setDisable(false);
                        badge.guestButton.setDisable(false);
                        Throwable ex = loginTask.getException();
                        if (ex != null) ex.printStackTrace();
                    }
                });
                crossFade(progressPane, inputPane);
                progressBar.progressProperty().bind(loginTask.progressProperty());
                LOGIN_EXECUTOR.submit(loginTask);
            }
        }

        private void crossFade(final Node in, final Node out) {
            if (!in.isVisible()) {
                in.setOpacity(0);
                in.setVisible(true);
            }

            if (fadeAnimation != null) {
                fadeAnimation.stop();
            }

            Duration time = Duration.seconds(.7);
            FadeTransition fadeIn = new FadeTransition(time, in);
            fadeIn.setToValue(1);

            FadeTransition fadeOut = new FadeTransition(time, out);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    out.setVisible(false);
                }
            });

            ParallelTransition tx = new ParallelTransition(fadeIn, fadeOut);
            tx.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    fadeAnimation = null;
                }
            });
            fadeAnimation = tx;
            tx.play();
        }
        
        @Override protected void layoutChildren() {
            final int w = (int)getWidth();
            if (small) {
                strap.setLayoutX(232-17);
                strap.setLayoutY(-23);
                title.setLayoutX(6 + 29);
                title.setLayoutY(5 + 38);
                progressBar.setLayoutX((int) ((w - 385) / 2d));
                progressBar.setLayoutY(155);
                progressBar.resize(385, 30);
                userNameField.setLayoutX((int) ((w - 385) / 2d));
                userNameField.setLayoutY(95);
                userNameField.resize(385, 45);
                passwordField.setLayoutX((int) ((w - 385) / 2d));
                passwordField.setLayoutY(95 + 50);
                passwordField.resize(385, 45);
                messageLabel.setLayoutX((int) ((w - 385) / 2d));
                messageLabel.setLayoutY(200);
                messageLabel.resize(385, 32);
                guestButton.setLayoutX((int)(w/2d) - 180);
                guestButton.setLayoutY(240);
                guestButton.resize(170, 66);
                loginButton.setLayoutX((int)(w/2d) + 10);
                loginButton.setLayoutY(240);
                loginButton.resize(170, 66);
            } else {
                strap.setLayoutX(198);
                strap.setLayoutY(-100);
                title.setLayoutX(6 + 45);
                title.setLayoutY(5 + 64);
                progressBar.setLayoutX((int) ((w - 385) / 2d));
                progressBar.setLayoutY(280);
                progressBar.resize(385, 36);
                userNameField.setLayoutX((int) ((w - 385) / 2d));
                userNameField.setLayoutY(337);
                userNameField.resize(385, 45);
                passwordField.setLayoutX((int) ((w - 385) / 2d));
                passwordField.setLayoutY(337 + 75);
                passwordField.resize(385, 45);
                messageLabel.setLayoutX((int) ((w - 385) / 2d));
                messageLabel.setLayoutY(480);
                messageLabel.resize(385, 36);
                guestButton.setLayoutX((int)(w/2d) - 180);
                guestButton.setLayoutY(534);
                guestButton.resize(170, 66);
                loginButton.setLayoutX((int)(w/2d) + 10);
                loginButton.setLayoutY(534);
                loginButton.resize(170, 66);
            }
        }
    }
}
