/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
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
package minesweeperfx;


import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class Tile {

    public enum TileState { Hidden, Showing, Flagged }
    public enum FlagState { None, Flag, Unflag }

    private TileState state = TileState.Hidden;
    private Rectangle rectangle;
    private Resources.ImageType imageType;
    private Location position;

    public Tile(double x, double y, double width, double height, Resources.ImageType imageType, Location position) {
        rectangle = new Rectangle(x, y, width, height);
        this.imageType = imageType;
        this.position = position;
    }

    public Location getPosition() {
        return position;
    }

    public void draw(GraphicsContext graphics, Point mouseLocation) {
        Image image = null;

        switch (state) {
            case Hidden: {
                if (rectangle.contains(mouseLocation) == true) {
                    image = Resources.getInstance().getImage(Resources.ImageType.Over);
                }
                else {
                    image = Resources.getInstance().getImage(Resources.ImageType.Blank);
                }
                break;
            }
            case Flagged: {
                if (rectangle.contains(mouseLocation) == true) {
                    image = Resources.getInstance().getImage(Resources.ImageType.FlagOver);
                }
                else {
                    image = Resources.getInstance().getImage(Resources.ImageType.Flag);
                }
                break;
            }
            case Showing: {
                image = Resources.getInstance().getImage(imageType);
                break;
            }
        }

        if (image != null) {
            graphics.drawImage(image, rectangle.x, rectangle.y);
        }
    }

    public void draw(GraphicsContext graphics) {
        Image image = null;

        if ((imageType == Resources.ImageType.Mine) &&
            (state == TileState.Showing)) {

            image = Resources.getInstance().getImage(Resources.ImageType.HitMine);
        }
        else {
            image = Resources.getInstance().getImage(imageType);
        }

        if (image != null) {
            graphics.drawImage(image, rectangle.x, rectangle.y);
        }
    }

    public boolean hitTest(Point mouseLocation) {
        boolean result = false;

        if (rectangle.contains(mouseLocation) == true) {
            result = true;
        }

        return result;
    }

    public boolean selected(Point mouseLocation) {
        boolean result = false;

        if (rectangle.contains(mouseLocation) == true) {
            state = TileState.Showing;

            if (imageType == Resources.ImageType.Mine) {
                result = true;
            }
        }

        return result;
    }

    public FlagState flag(Point mouseLocation) {
        FlagState result = FlagState.None;

        if (rectangle.contains(mouseLocation) == true) {
            if (state == TileState.Flagged) {
                state = TileState.Hidden;
                result = FlagState.Unflag;
            }
            else {
                state = TileState.Flagged;
                result = FlagState.Flag;
            }
        }

        return result;
    }

    // True is success, False is failure.
    public boolean isFlaggedAndMine() {
        boolean result = false;

        if ((state == TileState.Flagged && imageType == Resources.ImageType.Mine) ||
            (state != TileState.Flagged && imageType != Resources.ImageType.Mine)) {
            result = true;
        }

        return result;
    }

    public enum TileUncover {Stop, Continue, Done}

    public TileUncover uncover() {
        TileUncover result = TileUncover.Stop;

        if (state == TileState.Hidden && imageType != Resources.ImageType.Mine) {
            if (imageType == Resources.ImageType.ExposedTile) {
                state = TileState.Showing;
                result = TileUncover.Continue;
            }
            else if (imageType != Resources.ImageType.ExposedTile) {
                state = TileState.Showing;
                result = TileUncover.Done;
            }
        }

        return result;
    }
}
