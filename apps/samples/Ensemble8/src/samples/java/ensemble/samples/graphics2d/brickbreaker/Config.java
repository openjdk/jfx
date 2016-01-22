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
package ensemble.samples.graphics2d.brickbreaker;

import javafx.collections.ObservableList;
import javafx.scene.image.Image;

public final class Config {

    public static final double ANIMATION_TIME_SCALE = .5;
    public static final int MAX_LIVES = 9;
    // Screen info
    public static final int FIELD_BRICK_IN_ROW = 15;
    public static final int WINDOW_BORDER = 3; // on desktop platform
    public static final int TITLE_BAR_HEIGHT = 19; // on desktop platform
    public static final int SCREEN_WIDTH = 960;
    public static final int SCREEN_HEIGHT = 720;

    public static final int INFO_TEXT_SPACE = 10;

    // Game field info
    public static final int BRICK_WIDTH = 48;
    public static final int BRICK_HEIGHT = 24;
    public static final int SHADOW_WIDTH = 10;
    public static final int SHADOW_HEIGHT = 16;

    public static final double BALL_MIN_SPEED = 3;
    public static final double BALL_MAX_SPEED = BRICK_HEIGHT;
    public static final double BALL_MIN_COORD_SPEED = 2;
    public static final double BALL_SPEED_INC = 0.1f;

    public static final int BAT_Y = SCREEN_HEIGHT - 40;
    public static final int BAT_SPEED = 8;

    public static final int BONUS_SPEED = 3;

    public static final int FIELD_WIDTH = FIELD_BRICK_IN_ROW * BRICK_WIDTH;
    public static final int FIELD_HEIGHT = FIELD_WIDTH;
    public static final int FIELD_Y = SCREEN_HEIGHT - FIELD_HEIGHT;

    private static ObservableList<Image> bricksImages = javafx.collections.FXCollections.<Image>observableArrayList();

    public static ObservableList<Image> getBricksImages() {
        return bricksImages;
    }

    private static ObservableList<Image> bonusesImages = javafx.collections.FXCollections.<Image>observableArrayList();

    public static ObservableList<Image> getBonusesImages() {
        return bonusesImages;
    }

    public static final int IMAGE_BACKGROUND = 0;
    public static final int IMAGE_BAT_LEFT = 1;
    public static final int IMAGE_BAT_CENTER = 2;
    public static final int IMAGE_BAT_RIGHT = 3;
    public static final int IMAGE_BALL_0 = 4;
    public static final int IMAGE_BALL_1 = 5;
    public static final int IMAGE_BALL_2 = 6;
    public static final int IMAGE_BALL_3 = 7;
    public static final int IMAGE_BALL_4 = 8;
    public static final int IMAGE_BALL_5 = 9;
    public static final int IMAGE_LOGO = 10;
    public static final int IMAGE_SPLASH_BRICK = 11;
    public static final int IMAGE_SPLASH_BRICKSHADOW = 12;
    public static final int IMAGE_SPLASH_BREAKER = 13;
    public static final int IMAGE_SPLASH_BREAKERSHADOW = 14;
    public static final int IMAGE_SPLASH_PRESSANYKEY = 15;
    public static final int IMAGE_SPLASH_PRESSANYKEYSHADOW = 16;
    public static final int IMAGE_SPLASH_STRIKE = 17;
    public static final int IMAGE_SPLASH_STRIKESHADOW = 18;
    public static final int IMAGE_SPLASH_SUN = 19;
    public static final int IMAGE_READY = 20;
    public static final int IMAGE_GAMEOVER = 21;

    private static ObservableList<Image> images = javafx.collections.FXCollections.<Image>observableArrayList();

    public static ObservableList<Image> getImages() {
        return images;
    }

    public static void initialize() {

        images.clear();
        bricksImages.clear();
        bonusesImages.clear();

        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/background.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bat/left.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bat/center.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bat/right.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ball/ball0.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ball/ball1.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ball/ball2.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ball/ball3.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ball/ball4.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ball/ball5.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/logo.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/brick.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/brickshadow.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/breaker.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/breakershadow.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/pressanykey.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/pressanykeyshadow.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/strike.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/strikeshadow.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/splash/sun.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/ready.png")));
        images.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/gameover.png")));

        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/blue.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/broken1.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/broken2.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/brown.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/cyan.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/green.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/grey.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/magenta.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/orange.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/red.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/violet.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/white.png")));
        bricksImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/brick/yellow.png")));

        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/ballslow.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/ballfast.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/catch.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/batgrow.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/batreduce.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/ballgrow.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/ballreduce.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/strike.png")));
        bonusesImages.add(new Image(Config.class.getResourceAsStream("/ensemble/samples/shared-resources/brickImages/bonus/extralife.png")));
    }

    private Config() {

    }

}

