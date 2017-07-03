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


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class Game extends Pane {

    public enum GameDifficulty { Easy, Medium, Hard, Crazy }

    private Board board;
    private GraphicsContext graphicsContext;
    private Stage stage;
    private double extraHeight = 0;
    private int flagCount;
    private int tileCount;
    private Text minesLabel;

    private static String easyMessage = "Beginner 10 mines 10 x 10 tile grid";
    private static String mediumMessage = "Intermediate 30 mines 15 x 15 tile grid";
    private static String hardMessage = "Expert 100 mines 20 x 20 tile grid";
    private static String crazyMessage = "Crazy random mines random x random tile grid";

    static int random(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public Game(Stage stage, double extraHeight, Text minesLabel) {
        super();
        this.extraHeight = extraHeight;
        this.minesLabel = minesLabel;

        this.stage = stage;
    }

    private void newGame(int xDimension, int yDimension, int mineCount) {
        board = new Board(xDimension, yDimension, mineCount);
        flagCount = mineCount;
        tileCount = (xDimension * yDimension) - flagCount;

        setWidth(board.getBoardWidth());
        setHeight(board.getBoardHeight());

        stage.setWidth(board.getBoardWidth());
        stage.setHeight(board.getBoardHeight() + extraHeight);

        minesLabel.setText(Integer.toString(flagCount));

        if (graphicsContext != null) {
            board.invalidate(graphicsContext);
            board.draw(graphicsContext, null);
        }
    }

    public void newGame(GameDifficulty difficulty) {
        switch (difficulty) {
            case Easy:
                newGame(10, 10, 10);
                break;
            case Medium:
                newGame(15, 15, 30);
                break;
            case Hard:
                newGame(20, 20, 100);
                break;
            case Crazy:
                int x = random(5, 21);
                int y = random(5, 21);
                newGame(x, y, random(10, random(10, x * y)));
                break;
            // fix drawing of larger than 20x20 grid
        }
    }

    public void newGame(String description) {
        List<String> choices = new ArrayList<>();
        choices.add(easyMessage);
        choices.add(mediumMessage);
        choices.add(hardMessage);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(easyMessage, choices);
        dialog.setTitle("Minsweeper");
        dialog.setHeaderText(description);
        dialog.setContentText("New Game?");
        dialog.getDialogPane().getButtonTypes().remove(ButtonType.CANCEL);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(letter -> newGame(stringToGameDifficulty(letter)));
    }

    private GameDifficulty stringToGameDifficulty(String value) {
        if (value.equals(mediumMessage)) return GameDifficulty.Medium;
        else if (value.equals(hardMessage)) return GameDifficulty.Hard;
        else if (value.equals(crazyMessage)) return GameDifficulty.Crazy;
        return GameDifficulty.Easy;
    }

    public void setGraphicsContext(GraphicsContext value) {
        graphicsContext = value;
    }

    public void draw(GraphicsContext graphics, Point mouseLocation) {
        board.draw(graphics, mouseLocation);
    }

    public void leftClick(Point mouseLocation) {
        Tile tile = board.getTile(mouseLocation);

        if (tile != null) {
            board.uncoverAllAdjacent(tile);

            if (tile.selected(mouseLocation) == true) {
                board.draw(graphicsContext);
                newGame("Game over!");
            }
            else {
                tileCount--;

                if (tileCount == 0) {
                    board.draw(graphicsContext);
                    newGame("You won!");
                }
            }
        }

        board.draw(graphicsContext, null);
    }

    public void rightClick(Point mouseLocation) {
        Tile tile = board.getTile(mouseLocation);

        if (tile != null) {
            Tile.FlagState state = tile.flag(mouseLocation);

            if (state == Tile.FlagState.Flag) {
                flagCount--;
            }
            else if (state == Tile.FlagState.Unflag) {
                flagCount++;
            }

            if (flagCount == 0) {
                if (board.checkFlags() == true) {
                    board.draw(graphicsContext);
                    newGame("You won!");
                }
            }
        }

        minesLabel.setText(Integer.toString(flagCount));

        board.draw(graphicsContext, null);
    }
}