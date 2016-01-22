/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.samples.graphics2d.brickbreaker;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * The main purpose of the game is to break all the bricks and
 * not drop the ball.
 *
 * @sampleName Brick Breaker
 * @preview preview.png
 * @see javafx.scene.image.Image
 * @see javafx.scene.image.ImageView
 * @see javafx.util.Duration
 * @see javafx.animation.KeyFrame
 * @see javafx.animation.KeyValue
 * @see javafx.animation.Timeline
 * @see javafx.application.Application
 * @see javafx.application.Platform
 * @see javafx.collections.ObservableList
 * @see javafx.geometry.Rectangle2D
 * @see javafx.geometry.VPos
 */

public class BrickBreakerApp extends Application {

    private MainFrame mainFrame;

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public Parent createContent() {
        Config.initialize();
        Pane root = new Pane();
        root.setPrefSize(960, 720);
        root.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        root.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        mainFrame = new MainFrame(root);
        mainFrame.changeState(MainFrame.SPLASH);
        return root;
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    @Override public void stop() {
        MainFrame currentMainFrame = getMainFrame();
        currentMainFrame.endGame();
    }

    public void play() {
        MainFrame currentMainFrame = getMainFrame();
        currentMainFrame.restartGame();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    public class MainFrame {
        // Instance of scene root node
        private Pane root;

        // Instance of splash (if exists)
        private Splash splash;

        // Instance of level (if exists)
        private Level level;

        // Number of lifes
        private int lifeCount;

        // Current score
        private int score;

        private MainFrame(Pane root) {
            this.root = root;
        }

        public int getState() {
            return state;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getLifeCount() {
            return lifeCount;
        }

        public void setLifeCount(int count) {
            lifeCount = count;
        }

        public void increaseLives() {
            lifeCount = Math.min(lifeCount + 1, Config.MAX_LIVES);
        }

        public void decreaseLives() {
            lifeCount--;
        }

        // Initializes game (lifes, scores etc)
        public void startGame() {
            lifeCount = 3;
            score = 0;
            changeState(1);
        }

        public void endGame() {
            if (splash != null) {
                splash.stop();
            }
            if (level != null) {
                level.stop();
            }
        }

        public void restartGame() {
            if (splash != null) {
                splash.start();
            }
            if (level != null) {
                level.restart();
            }
        }

        // Current state of the game. The next values are available
        // 0 - Splash
        public static final int SPLASH = 0;
        // 1..Level.LEVEL_COUNT - Level
        private int state = SPLASH;

        public void changeState(int newState) {
            this.state = newState;
            if (splash != null) {
                splash.stop();
            }
            if (level != null) {
                level.stop();
            }
            if (state < 1 || state > LevelData.getLevelsCount()) {
                root.getChildren().remove(level);
                level = null;
                splash = new Splash(mainFrame);
                root.getChildren().add(splash);
                splash.start();
            } else {
                root.getChildren().remove(splash);
                splash = null;
                level = new Level(mainFrame, state);
                root.getChildren().add(level);
                level.start();
            }
        }
    }

}

