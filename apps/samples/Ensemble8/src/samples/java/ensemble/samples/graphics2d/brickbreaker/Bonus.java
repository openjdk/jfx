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

import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Bonus extends Parent {

    public static final int TYPE_SLOW = 0;
    public static final int TYPE_FAST = 1;
    public static final int TYPE_CATCH = 2;
    public static final int TYPE_GROW_BAT = 3;
    public static final int TYPE_REDUCE_BAT = 4;
    public static final int TYPE_GROW_BALL = 5;
    public static final int TYPE_REDUCE_BALL = 6;
    public static final int TYPE_STRIKE = 7;
    public static final int TYPE_LIFE = 8;

    public static final int COUNT = 9;

    public static final String[] NAMES = new String[] {
        "SLOW",
        "FAST",
        "CATCH",
        "GROW BAT",
        "REDUCE BAT",
        "GROW BALL",
        "REDUCE BALL",
        "STRIKE",
        "LIFE",
    };

    private int type;
    private int width;
    private int height;
    private ImageView content;

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getType() {
        return type;
    }

    public Bonus(int type) {
        content = new ImageView();
        getChildren().add(content);
        this.type = type;
        Image image = Config.getBonusesImages().get(type);
        width = (int)image.getWidth() - Config.SHADOW_WIDTH;
        height = (int)image.getHeight() - Config.SHADOW_HEIGHT;
        content.setImage(image);
        setMouseTransparent(true);
    }

}


