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

import java.util.ArrayList;
import java.util.Iterator;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import ensemble.samples.graphics2d.brickbreaker.BrickBreakerApp.MainFrame;
import javafx.animation.AnimationTimer;

public class Level extends Parent {

    private static final double MOB_SCALING = 1.5f;
    private final MainFrame mainFrame;

    private ArrayList<Brick> bricks;
    private int brickCount;
    private ArrayList<Brick> fadeBricks;
    private ArrayList<Bonus> bonuses;
    private Group group;
    private ArrayList<Bonus> lives;
    private int catchedBonus;

    // States
    // 0 - starting level
    // 1 - ball is catched
    // 2 - playing
    // 3 - game over
    private static final int STARTING_LEVEL = 0;
    private static final int BALL_CATCHED = 1;
    private static final int PLAYING = 2;
    private static final int GAME_OVER = 3;

    private int state;
    private int batDirection;
    private double ballDirX;
    private double ballDirY;
    private int levelNumber;
    private Bat bat;
    private Ball ball;
    private Text roundCaption;
    private Text round;
    private Text scoreCaption;
    private Text score;
    private Text livesCaption;
    private ImageView message;
    private Timeline startingTimeline;
    private AnimationTimer animationTimer;
    private Group infoPanel;

    public Level(MainFrame mainFrame, int levelNumber) {
        this.mainFrame = mainFrame;
        group = new Group();
        getChildren().add(group);
        initContent(levelNumber);
    }

    private void initStartingTimeline() {
        startingTimeline = new Timeline();
        KeyFrame kf1 = new KeyFrame(Duration.millis(500), (ActionEvent event) -> {
            message.setVisible(true);
            state = STARTING_LEVEL;
            bat.setVisible(false);
            ball.setVisible(false);
        }, new KeyValue(message.opacityProperty(), 0));
        KeyFrame kf2 = new KeyFrame(Duration.millis(1500), new KeyValue(message.opacityProperty(), 1));
        KeyFrame kf3 = new KeyFrame(Duration.millis(3000), new KeyValue(message.opacityProperty(), 1));
        KeyFrame kf4 = new KeyFrame(Duration.millis(4000), (ActionEvent event) -> {
            message.setVisible(false);

            bat.setTranslateX((Config.FIELD_WIDTH - bat.getWidth()) / 2.0);
            ball.setTranslateX((Config.FIELD_WIDTH - ball.getDiameter()) / 2.0);
            ball.setTranslateY(Config.BAT_Y - ball.getDiameter());
            ballDirX = (Utils.random(2) * 2 - 1) * Config.BALL_MIN_COORD_SPEED;
            ballDirY = -Config.BALL_MIN_SPEED;

            bat.setVisible(true);
            ball.setVisible(true);
            state = BALL_CATCHED;
        }, new KeyValue(message.opacityProperty(), 0));

        startingTimeline.getKeyFrames().addAll(kf1, kf2, kf3, kf4);
    }

    private void initTimeline() {
        mainFrame.setLifeCount(3);
        mainFrame.setScore(0);
        animationTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                final double speedFactor = Config.ANIMATION_TIME_SCALE;
                // Process fadeBricks
                Iterator<Brick> brickIterator = fadeBricks.iterator();
                while (brickIterator.hasNext()) {
                    Brick brick = brickIterator.next();
                    brick.setOpacity(brick.getOpacity() - 0.1 * speedFactor);
                    if (brick.getOpacity() <= 0) {
                        brick.setVisible(false);
                        brickIterator.remove();
                    }
                }
                // Move bat if needed
                if (batDirection != 0 && state != STARTING_LEVEL) {
                    moveBat(bat.getTranslateX() + batDirection);
                }
                // Process bonuses
                Iterator<Bonus> bonusIterator = bonuses.iterator();
                while (bonusIterator.hasNext()) {
                    Bonus bonus = bonusIterator.next();
                    if (bonus.getTranslateY() > Config.SCREEN_HEIGHT) {
                        bonus.setVisible(false);
                        bonusIterator.remove();
                        group.getChildren().remove(bonus);
                    } else {
                        bonus.setTranslateY(bonus.getTranslateY() + Config.BONUS_SPEED * speedFactor);
                        if (bonus.getTranslateX() + bonus.getWidth() > bat.getTranslateX() &&
                                bonus.getTranslateX() < bat.getTranslateX() + bat.getWidth() &&
                                bonus.getTranslateY() + bonus.getHeight() > bat.getTranslateY() &&
                                bonus.getTranslateY() < bat.getTranslateY() + bat.getHeight()) {
                            // Bonus is catched
                            updateScore(100);
                            catchedBonus = bonus.getType();
                            bonus.setVisible(false);
                            bonusIterator.remove();
                            group.getChildren().remove(bonus);
                            if (bonus.getType() == Bonus.TYPE_SLOW) {
                                ballDirX /= 1.5;
                                ballDirY /= 1.5;
                                correctBallSpeed();
                            } else if (bonus.getType() == Bonus.TYPE_FAST) {
                                ballDirX *= 1.5;
                                ballDirY *= 1.5;
                                correctBallSpeed();
                            } else if (bonus.getType() == Bonus.TYPE_GROW_BAT) {
                                if (bat.getSize() < Bat.MAX_SIZE) {
                                    bat.changeSize(bat.getSize() + 1);
                                    if (bat.getTranslateX() + bat.getWidth() > Config.FIELD_WIDTH) {
                                        bat.setTranslateX(Config.FIELD_WIDTH - bat.getWidth());
                                    }
                                }
                            } else if (bonus.getType() == Bonus.TYPE_REDUCE_BAT) {
                                if (bat.getSize() > 0) {
                                    int oldWidth = bat.getWidth();
                                    bat.changeSize(bat.getSize() - 1);
                                    bat.setTranslateX(bat.getTranslateX() + ((oldWidth - bat.getWidth()) / 2));
                                }
                            } else if (bonus.getType() == Bonus.TYPE_GROW_BALL) {
                                if (ball.getSize() < Ball.MAX_SIZE) {
                                    ball.changeSize(ball.getSize() + 1);
                                    if (state == BALL_CATCHED) {
                                        ball.setTranslateY(Config.BAT_Y - ball.getDiameter());
                                    }
                                }
                            } else if (bonus.getType() == Bonus.TYPE_REDUCE_BALL) {
                                if (ball.getSize() > 0) {
                                    ball.changeSize(ball.getSize() - 1);
                                    if (state == BALL_CATCHED) {
                                        ball.setTranslateY(Config.BAT_Y - ball.getDiameter());
                                    }
                                }
                            } else if (bonus.getType() == Bonus.TYPE_LIFE) {
                                mainFrame.increaseLives();
                                updateLives();
                            }
                        }
                    }
                }
                if (state != PLAYING) {
                    return;
                }
                double newX = ball.getTranslateX() + ballDirX;
                double newY = ball.getTranslateY() + ballDirY;
                boolean inverseX = false;
                boolean inverseY = false;
                if (newX < 0) {
                    newX = -newX;
                    inverseX = true;
                }
                int BALL_MAX_X = Config.FIELD_WIDTH - ball.getDiameter();
                if (newX > BALL_MAX_X) {
                    newX = BALL_MAX_X - (newX - BALL_MAX_X);
                    inverseX = true;
                }
                if (newY < Config.FIELD_Y) {
                    newY = 2 * Config.FIELD_Y - newY;
                    inverseY = true;
                }
                // Determine hit bat and ball
                if (ballDirY > 0 &&
                        ball.getTranslateY() + ball.getDiameter() < Config.BAT_Y &&
                        newY + ball.getDiameter() >= Config.BAT_Y &&
                        newX >= bat.getTranslateX() - ball.getDiameter() &&
                        newX < bat.getTranslateX() + bat.getWidth() + ball.getDiameter()) {
                    inverseY = true;
                    // Speed up ball
                    double speed = Math.sqrt(ballDirX * ballDirX + ballDirY * ballDirY);
                    ballDirX *= (speed + Config.BALL_SPEED_INC) / speed;
                    ballDirY *= (speed + Config.BALL_SPEED_INC) / speed;
                    // Correct ballDirX and ballDirY
                    double offsetX = newX + ball.getDiameter() / 2 - bat.getTranslateX() - bat.getWidth() / 2;
                    // Don't change direction if center of bat was used
                    if (Math.abs(offsetX) > bat.getWidth() / 4) {
                        ballDirX += offsetX / 5;
                        double MAX_COORD_SPEED = Math.sqrt(speed * speed -
                            Config.BALL_MIN_COORD_SPEED * Config.BALL_MIN_COORD_SPEED);
                        if (Math.abs(ballDirX) > MAX_COORD_SPEED) {
                            ballDirX = Utils.sign(ballDirX) * MAX_COORD_SPEED;
                        }
                        ballDirY = Utils.sign(ballDirY) *
                            Math.sqrt(speed * speed - ballDirX * ballDirX);
                    }
                    correctBallSpeed();
                    if (catchedBonus == Bonus.TYPE_CATCH) {
                        newY = Config.BAT_Y - ball.getDiameter();
                        state = BALL_CATCHED;
                    }
                }
                // Determine hit ball and brick
                int firstCol = (int)(newX / Config.BRICK_WIDTH);
                int secondCol = (int)((newX + ball.getDiameter()) / Config.BRICK_WIDTH);
                int firstRow = (int)((newY - Config.FIELD_Y) / Config.BRICK_HEIGHT);
                int secondRow = (int)((newY - Config.FIELD_Y + ball.getDiameter()) / Config.BRICK_HEIGHT);
                if (ballDirX > 0) {
                    int temp = secondCol;
                    secondCol = firstCol;
                    firstCol = temp;
                }
                if (ballDirY > 0) {
                    int temp = secondRow;
                    secondRow = firstRow;
                    firstRow = temp;
                }
                Brick vertBrick = getBrick(firstRow, secondCol);
                Brick horBrick = getBrick(secondRow, firstCol);
                if (vertBrick != null) {
                    kickBrick(firstRow, secondCol);
                    if (catchedBonus != Bonus.TYPE_STRIKE) {
                        inverseY = true;
                    }
                }
                if (horBrick != null &&
                        (firstCol != secondCol || firstRow != secondRow)) {
                    kickBrick(secondRow, firstCol);
                    if (catchedBonus != Bonus.TYPE_STRIKE) {
                        inverseX = true;
                    }
                }
                if (firstCol != secondCol || firstRow != secondRow) {
                    Brick diagBrick = getBrick(firstRow, firstCol);
                    if (diagBrick != null && diagBrick != vertBrick &&
                            diagBrick != horBrick) {
                        kickBrick(firstRow, firstCol);
                        if (vertBrick == null && horBrick == null &&
                                catchedBonus != Bonus.TYPE_STRIKE) {
                            inverseX = true;
                            inverseY = true;
                        }
                    }
                }
                ball.setTranslateX(newX);
                ball.setTranslateY(newY);
                if (inverseX) {
                    ballDirX = - ballDirX;
                }
                if (inverseY) {
                    ballDirY = - ballDirY;
                }
                if (ball.getTranslateY() > Config.SCREEN_HEIGHT) {
                    // Ball was lost
                    lostLife();
                }
            }
        };
    }

    public void start() {
        startingTimeline.play();
        animationTimer.start();
        group.getChildren().get(0).requestFocus();
        updateScore(0);
        updateLives();
    }

    public void stop() {
        startingTimeline.pause();
        animationTimer.stop();
    }

    public void restart() {
        if (message.isVisible()) {
            startingTimeline.play();
        }
        animationTimer.start();
    }

    private void initLevel() {
        String[] level = LevelData.getLevelData(levelNumber);
        for (int row = 0; row < level.length; row++) {
            for (int col = 0; col < Config.FIELD_BRICK_IN_ROW; col++) {
                String rowString = level[row];
                Brick brick = null;
                if (rowString != null && col < rowString.length()) {
                    String type = rowString.substring(col, col + 1);
                    if (!type.equals(" ")) {
                        brick = new Brick(Brick.getBrickType(type));
                        brick.setTranslateX(col * Config.BRICK_WIDTH);
                        brick.setTranslateY(Config.FIELD_Y + row * Config.BRICK_HEIGHT);
                        if (brick.getType() != Brick.TYPE_GREY) {
                            brickCount++;
                        }
                    }
                }
                bricks.add(brick);
            }
        }
    }

    private Brick getBrick(int row, int col) {
        int i = row * Config.FIELD_BRICK_IN_ROW + col;
        if (col < 0 || col >= Config.FIELD_BRICK_IN_ROW || row < 0 || i >= bricks.size()) {
            return null;
        } else {
            return bricks.get(i);
        }
    }

    private void updateScore(int inc) {
        mainFrame.setScore(mainFrame.getScore() + inc);
        score.setText(mainFrame.getScore() + "");
    }

    private void moveBat(double newX) {
        double x = newX;
        if (x < 0) {
            x = 0;
        }
        if (x + bat.getWidth() > Config.FIELD_WIDTH) {
            x = Config.FIELD_WIDTH - bat.getWidth();
        }
        if (state == BALL_CATCHED) {
            double ballX = ball.getTranslateX() + x - bat.getTranslateX();
            if (ballX < 0) {
                ballX = 0;
            }
            double BALL_MAX_X = Config.FIELD_WIDTH - ball.getDiameter();
            if (ballX > BALL_MAX_X) {
                ballX = BALL_MAX_X;
            }
            ball.setTranslateX(ballX);
        }
        bat.setTranslateX(x);
    }

    private void kickBrick(int row, int col) {
        Brick brick = getBrick(row, col);
        if (brick == null || (catchedBonus != Bonus.TYPE_STRIKE && !brick.kick())) {
            return;
        }
        updateScore(10);
        if (brick.getType() != Brick.TYPE_GREY) {
            brickCount--;
            if (brickCount == 0) {
                mainFrame.changeState(mainFrame.getState() + 1);
            }
        }
        bricks.set(row * Config.FIELD_BRICK_IN_ROW + col, null);
        fadeBricks.add(brick);
        if (Utils.random(8) == 0 && bonuses.size() < 5) {
            Bonus bonus = new Bonus(Utils.random(Bonus.COUNT));
            bonus.setTranslateY(brick.getTranslateY());
            bonus.setVisible(true);
            bonus.setTranslateX(brick.getTranslateX() + (Config.BRICK_WIDTH - bonus.getWidth()) / 2);
            group.getChildren().add(bonus);
            bonuses.add(bonus);
        }
    }

    private void updateLives() {
        while (lives.size() > mainFrame.getLifeCount()) {
            Bonus lifeBat = lives.get(lives.size() - 1);
            lives.remove(lifeBat);
            infoPanel.getChildren().remove(lifeBat);
        }
        // Add lifes (but no more than 9)
        int maxVisibleLifes = 9;
        double scale = 0.8;

        for (int life = lives.size(); life < Math.min(mainFrame.getLifeCount(), maxVisibleLifes); life++) {
            Bonus lifeBonus = new Bonus(Bonus.TYPE_LIFE);
            lifeBonus.setScaleX(scale);
            lifeBonus.setScaleY(scale);
            lifeBonus.setTranslateX(livesCaption.getTranslateX() +
                livesCaption.getBoundsInLocal().getWidth() + (life % 3) * lifeBonus.getWidth());
            lifeBonus.setTranslateY(livesCaption.getTranslateY() +
                (life / 3) * lifeBonus.getHeight() * MOB_SCALING);
            lives.add(lifeBonus);
            infoPanel.getChildren().add(lifeBonus);
        }
    }

    private void correctBallSpeed() {
        double speed = Math.sqrt(ballDirX * ballDirX + ballDirY * ballDirY);
        if (speed > Config.BALL_MAX_SPEED) {
            ballDirX *= Config.BALL_MAX_SPEED / speed;
            ballDirY *= Config.BALL_MAX_SPEED / speed;
            speed = Config.BALL_MAX_SPEED;
        }
        if (speed < Config.BALL_MIN_SPEED) {
            ballDirX *= Config.BALL_MIN_SPEED / speed;
            ballDirY *= Config.BALL_MIN_SPEED / speed;
            speed = Config.BALL_MIN_SPEED;
        }
        if (Math.abs(ballDirX) < Config.BALL_MIN_COORD_SPEED) {
            ballDirX = Utils.sign(ballDirX) * Config.BALL_MIN_COORD_SPEED;
            ballDirY = Utils.sign(ballDirY) * Math.sqrt(speed * speed - ballDirX * ballDirX);
        } else if (Math.abs(ballDirY) < Config.BALL_MIN_COORD_SPEED) {
            ballDirY = Utils.sign(ballDirY) * Config.BALL_MIN_COORD_SPEED;
            ballDirX = Utils.sign(ballDirX) * Math.sqrt(speed * speed - ballDirY * ballDirY);
        }
    }

    private void lostLife() {
        mainFrame.decreaseLives();
        if (mainFrame.getLifeCount() < 0) {
            state = GAME_OVER;
            ball.setVisible(false);
            bat.setVisible(false);
            message.setImage(Config.getImages().get(Config.IMAGE_GAMEOVER));
            message.setTranslateX((Config.FIELD_WIDTH - message.getImage().getWidth()) / 2);
            message.setTranslateY(Config.FIELD_Y +
                (Config.FIELD_HEIGHT - message.getImage().getHeight()) / 2);
            message.setVisible(true);
            message.setOpacity(1);
        } else {
            updateLives();
            bat.changeSize(Bat.DEFAULT_SIZE);
            ball.changeSize(Ball.DEFAULT_SIZE);
            bat.setTranslateX((Config.FIELD_WIDTH - bat.getWidth()) / 2);
            ball.setTranslateX(Config.FIELD_WIDTH / 2 - ball.getDiameter() / 2);
            ball.setTranslateY(Config.BAT_Y - ball.getDiameter());
            state = BALL_CATCHED;
            catchedBonus = 0;
            ballDirX = (Utils.random(2) * 2 - 1) * Config.BALL_MIN_COORD_SPEED;
            ballDirY = - Config.BALL_MIN_SPEED;
        }
    }

    private void initInfoPanel() {
        infoPanel = new Group();
        roundCaption = new Text();
        roundCaption.setText("ROUND");
        roundCaption.setTextOrigin(VPos.TOP);
        roundCaption.setFill(Color.rgb(51, 102, 51));
        Font f = new Font("Impact", 18);
        roundCaption.setFont(f);
        roundCaption.setTranslateX(30);
        roundCaption.setTranslateY(128);
        round = new Text();
        round.setTranslateX(roundCaption.getTranslateX() +
            roundCaption.getBoundsInLocal().getWidth() + Config.INFO_TEXT_SPACE);
        round.setTranslateY(roundCaption.getTranslateY());
        round.setText(levelNumber + "");
        round.setTextOrigin(VPos.TOP);
        round.setFont(f);
        round.setFill(Color.rgb(0, 204, 102));
        scoreCaption = new Text();
        scoreCaption.setText("SCORE");
        scoreCaption.setFill(Color.rgb(51, 102, 51));
        scoreCaption.setTranslateX(30);
        scoreCaption.setTranslateY(164);
        scoreCaption.setTextOrigin(VPos.TOP);
        scoreCaption.setFont(f);
        score = new Text();
        score.setTranslateX(scoreCaption.getTranslateX() +
            scoreCaption.getBoundsInLocal().getWidth() + Config.INFO_TEXT_SPACE);
        score.setTranslateY(scoreCaption.getTranslateY());
        score.setFill(Color.rgb(0, 204, 102));
        score.setTextOrigin(VPos.TOP);
        score.setFont(f);
        score.setText("");
        livesCaption = new Text();
        livesCaption.setText("LIFE");
        livesCaption.setTranslateX(30);
        livesCaption.setTranslateY(200);
        livesCaption.setFill(Color.rgb(51, 102, 51));
        livesCaption.setTextOrigin(VPos.TOP);
        livesCaption.setFont(f);
        Color INFO_LEGEND_COLOR = Color.rgb(0, 114, 188);
        int infoWidth = Config.SCREEN_WIDTH - Config.FIELD_WIDTH;
        Rectangle black = new Rectangle();
        black.setWidth(infoWidth);
        black.setHeight(Config.SCREEN_HEIGHT);
        black.setFill(Color.BLACK);
        ImageView verLine = new ImageView();
        verLine.setImage(new Image(Level.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/vline.png")));
        verLine.setTranslateX(3);
        ImageView logo = new ImageView();
        logo.setImage(Config.getImages().get(Config.IMAGE_LOGO));
        logo.setTranslateX(30);
        logo.setTranslateY(30);
        Text legend = new Text();
        legend.setTranslateX(30);
        legend.setTranslateY(310);
        legend.setText("LEGEND");
        legend.setFill(INFO_LEGEND_COLOR);
        legend.setTextOrigin(VPos.TOP);
        legend.setFont(new Font("Impact", 18));
        infoPanel.getChildren().addAll(black, verLine, logo, roundCaption,
                round, scoreCaption, score, livesCaption, legend);
        for (int i = 0; i < Bonus.COUNT; i++) {
            Bonus bonus = new Bonus(i);
            Text text = new Text();
            text.setTranslateX(100);
            text.setTranslateY(350 + i * 40);
            text.setText(Bonus.NAMES[i]);
            text.setFill(INFO_LEGEND_COLOR);
            text.setTextOrigin(VPos.TOP);
            text.setFont(new Font("Arial", 12));
            bonus.setTranslateX(30 + (820 - 750 - bonus.getWidth()) / 2);
            bonus.setTranslateY(text.getTranslateY() -
                (bonus.getHeight() - text.getBoundsInLocal().getHeight()) / 2);
            // Workaround JFXC-2379
            infoPanel.getChildren().addAll(bonus, text);
        }
        infoPanel.setTranslateX(Config.FIELD_WIDTH);
    }

    private void initContent(int level) {
        catchedBonus = 0;
        state = STARTING_LEVEL;
        batDirection = 0;
        levelNumber = level;
        lives = new ArrayList<Bonus>();
        bricks = new ArrayList<Brick>();
        fadeBricks = new ArrayList<Brick>();
        bonuses = new ArrayList<Bonus>();
        ball = new Ball();
        ball.setVisible(false);
        bat = new Bat();
        bat.setTranslateY(Config.BAT_Y);
        bat.setVisible(false);
        message = new ImageView();
        message.setImage(Config.getImages().get(Config.IMAGE_READY));
        message.setTranslateX((Config.FIELD_WIDTH - message.getImage().getWidth()) / 2);
        message.setTranslateY(Config.FIELD_Y +
            (Config.FIELD_HEIGHT - message.getImage().getHeight()) / 2);
        message.setVisible(false);
        initLevel();
        initStartingTimeline();
        initTimeline();
        initInfoPanel();
        final ImageView background = new ImageView();
        background.setFocusTraversable(true);
        background.setImage(Config.getImages().get(Config.IMAGE_BACKGROUND));
        background.setFitWidth(Config.SCREEN_WIDTH);
        background.setFitHeight(Config.SCREEN_HEIGHT);
        background.setOnMouseMoved((MouseEvent me) -> {
            moveBat(me.getX() - bat.getWidth() / 2);
            me.consume();
        });
        background.setOnMouseDragged((MouseEvent me) -> {
            // Support touch-only devices like some mobile phones
            moveBat(me.getX() - bat.getWidth() / 2);
            me.consume();
        });
        background.setOnMousePressed((MouseEvent me) -> {
            background.requestFocus();
            if (state == PLAYING) {
                // Support touch-only devices like some mobile phones
                moveBat(me.getX() - bat.getWidth() / 2);
            }
            if (state == BALL_CATCHED) {
                state = PLAYING;
            }
            if (state == GAME_OVER) {
                mainFrame.changeState(MainFrame.SPLASH);
            }
            me.consume();
        });
        background.setOnKeyPressed((KeyEvent ke) -> {
            if ((ke.getCode() == KeyCode.POWER) || (ke.getCode() == KeyCode.X)) {
                Platform.exit();
            }
            if (state == BALL_CATCHED && (ke.getCode() == KeyCode.SPACE ||
                    ke.getCode() == KeyCode.ENTER || ke.getCode() == KeyCode.PLAY)) {
                state = PLAYING;
            }
            if (state == GAME_OVER) {
                mainFrame.changeState(MainFrame.SPLASH);
            }
            if (state == PLAYING && ke.getCode() == KeyCode.Q) {
                // Lost life
                lostLife();
                return;
            }
            if ((ke.getCode() == KeyCode.LEFT || ke.getCode() == KeyCode.TRACK_PREV)) {
                batDirection = - Config.BAT_SPEED;
            }
            if ((ke.getCode() == KeyCode.RIGHT || ke.getCode() == KeyCode.TRACK_NEXT)) {
                batDirection = Config.BAT_SPEED;
            }
            if (ke.getCode() != KeyCode.TAB) {
                ke.consume();
            }
        });
        background.setOnKeyReleased((KeyEvent ke) -> {
            if (ke.getCode() == KeyCode.LEFT || ke.getCode() == KeyCode.RIGHT ||
                    ke.getCode() == KeyCode.TRACK_PREV || ke.getCode() == KeyCode.TRACK_NEXT) {
                batDirection = 0;
                ke.consume();
            }
        });
        group.getChildren().add(background);
        for (int row = 0; row < bricks.size()/Config.FIELD_BRICK_IN_ROW; row++) {
            for (int col = 0; col < Config.FIELD_BRICK_IN_ROW; col++) {
                Brick b = getBrick(row, col);
                if (b != null) { //tmp
                    group.getChildren().add(b);
                }
            }
        }

        group.getChildren().addAll(message, ball, bat, infoPanel);
    }

}
