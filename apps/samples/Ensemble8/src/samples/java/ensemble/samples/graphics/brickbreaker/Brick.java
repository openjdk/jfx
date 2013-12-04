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
package ensemble.samples.graphics.brickbreaker;

import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Brick extends Parent {

    public static final int TYPE_BLUE = 0;
    public static final int TYPE_BROKEN1 = 1;
    public static final int TYPE_BROKEN2 = 2;
    public static final int TYPE_BROWN = 3;
    public static final int TYPE_CYAN = 4;
    public static final int TYPE_GREEN = 5;
    public static final int TYPE_GREY = 6;
    public static final int TYPE_MAGENTA = 7;
    public static final int TYPE_ORANGE = 8;
    public static final int TYPE_RED = 9;
    public static final int TYPE_VIOLET = 10;
    public static final int TYPE_WHITE = 11;
    public static final int TYPE_YELLOW = 12;

    private int type;
    private ImageView content;

    public Brick(int type) {
        content = new ImageView();
        getChildren().add(content);
        changeType(type);
        setMouseTransparent(true);
    }

    public int getType() {
        return type;
    }

    public boolean kick() {
        if (type == TYPE_GREY) {
            return false;
        }
        if (type == TYPE_BROKEN1) {
            changeType(TYPE_BROKEN2);
            return false;
        }
        return true;
    }

    private void changeType(int newType) {
        this.type = newType;
        Image image = Config.getBricksImages().get(type);
        content.setImage(image);
        content.setFitWidth(Config.FIELD_WIDTH/15);
    }

    

    public static int getBrickType(String s) {
        switch (s) {
            case "L":
                return TYPE_BLUE;
            case "2":
                return TYPE_BROKEN1;
            case "B":
                return TYPE_BROWN;
            case "C":
                return TYPE_CYAN;
            case "G":
                return TYPE_GREEN;
            case "0":
                return TYPE_GREY;
            case "M":
                return TYPE_MAGENTA;
            case "O":
                return TYPE_ORANGE;
            case "R":
                return TYPE_RED;
            case "V":
                return TYPE_VIOLET;
            case "W":
                return TYPE_WHITE;
            case "Y":
                return TYPE_YELLOW;
            default:
                System.out.println("Unknown brick type '{s}'");
                return TYPE_WHITE;
        }
    }

}


