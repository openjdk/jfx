/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates.
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
import javafx.scene.layout.GridPane;


public class Board extends GridPane {
    private Tile[][] tiles;
    private double boardWidth;
    private double boardHeight;

    private Resources.ImageType valueToTileType(int value) {
        Resources.ImageType result = Resources.ImageType.Blank;

        switch (value) {
            case 0: {
                result = Resources.ImageType.ExposedTile;
                break;
            }
            case 1: {
                result = Resources.ImageType.Number1;
                break;
            }
            case 2: {
                result = Resources.ImageType.Number2;
                break;
            }
            case 3: {
                result = Resources.ImageType.Number3;
                break;
            }
            case 4: {
                result = Resources.ImageType.Number4;
                break;
            }
            case 5: {
                result = Resources.ImageType.Number5;
                break;
            }
            case 6: {
                result = Resources.ImageType.Number6;
                break;
            }
            case 7: {
                result = Resources.ImageType.Number7;
                break;
            }
            case 8: {
                result = Resources.ImageType.Number8;
                break;
            }
        }

        return result;
    }

    public Board(int xDimension, int yDimension, int mineCount) {
        double r = 0.15; // Random value that returns good results.

        // Border is used for boundary cases
        // with generated board before it is converted to tiles.
        boolean[][] bombs = new boolean[xDimension + 2][yDimension + 2];
        int[][] board = new int[xDimension + 2][yDimension + 2];
        tiles = new Tile[xDimension][yDimension];
        int lMineCount = mineCount;

        // Generate location of bombs.
        for (int j = 1; j <= yDimension; j++) {
            for (int i = 1; i <= xDimension; i++) {
                boolean hasBomb = false;

                if (Math.random() < r) {
                    if (lMineCount > 0) {
                        lMineCount--;
                        hasBomb = true;
                    }
                }

                bombs[i][j] = hasBomb;
            }
        }

        if (lMineCount > 0) {
            for (int j = 1; j <= yDimension; j++) {
                for (int i = 1; i <= xDimension; i++) {
                    boolean hasBomb = bombs[i][j];

                    if (hasBomb == false) {
                        if (Math.random() < r) {
                            if (lMineCount > 0) {
                                lMineCount--;
                                hasBomb = true;
                            }
                        }

                        bombs[i][j] = hasBomb;
                    }
                }
            }
        }

        // There is a small chance minCount is not 0. Implement the
        // dispersal of the bombs better.

        if (Globals.debug) {
            for (int j = 1; j <= yDimension; j++) {
                for (int i = 1; i <= xDimension; i++) {
                    if (bombs[i][j]) {
                        System.out.print("* ");
                    }
                    else {
                        System.out.print(". ");
                    }
                }

                System.out.println();
            }
        }

        if (Globals.debug) {
            System.out.println();
        }

        // Generate board.
        for (int j = 1; j <= yDimension; j++) {
            for (int i = 1; i <= xDimension; i++) {
                for (int iindex = i - 1; iindex <= i + 1; iindex++) {
                    for (int jindex = j - 1; jindex <= j + 1; jindex++) {
                        if (bombs[iindex][jindex]) board[i][j]++;
                    }
                }
            }
        }

        Image image = Resources.getInstance().getImage(Resources.ImageType.Blank);
        double width = image.getWidth();
        double height = image.getHeight();
        double ypos = 5; // 10 is boarder.
        boardWidth = (xDimension * width) + 20; // 20 is boarder.
        boardHeight = (yDimension * height) + 28;

        // Convert board into tiles.
        for (int j = 1; j <= yDimension; j++) {
            double xpos = 10;

            for (int i = 1; i <= xDimension; i++) {
                Tile tile;
                if (bombs[i][j]) {
                    if (Globals.debug) {
                        System.out.print("* ");
                    }

                    tile = new Tile(xpos, ypos, width, height, Resources.ImageType.Mine, new Location(i - 1, j - 1));
                }
                else {
                    if (Globals.debug) {
                        System.out.print(board[i][j] + " ");
                    }

                    Resources.ImageType type = valueToTileType(board[i][j]);
                    tile = new Tile(xpos, ypos, width, height, type, new Location(i - 1, j - 1));
                }

                tiles[i - 1][j - 1] = tile;
                xpos += width;
            }

            if (Globals.debug) {
                System.out.println();
            }

            ypos += height;
        }
    }

    void uncoverAllAdjacent(Tile tile) {
        Tile.TileUncover status = tile.uncover();

        if (status == Tile.TileUncover.Done) {
            return;
        }

        if (status != Tile.TileUncover.Stop) {
            Location position = tile.getPosition();

            //Left
            if (position.x > 0) {
                uncoverAllAdjacent(tiles[position.x - 1][position.y]);
            }
            //Up
            if (position.y > 0) {
                uncoverAllAdjacent(tiles[position.x][position.y - 1]);
            }
            //Down
            if (position.y < tiles[position.x].length - 1) {
                uncoverAllAdjacent(tiles[position.x][position.y + 1]);
            }
            //Right
            if (position.x < tiles.length - 1) {
                uncoverAllAdjacent(tiles[position.x + 1][position.y]);
            }
        }
    }

    private interface Loop {
        public void process(Tile value);
    }

    private void forEachTile(Loop function) {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                function.process(tiles[i][j]);
            }
        }
    }

    public void draw(GraphicsContext graphics, Point mouseLocation) {
        forEachTile((tile) -> tile.draw(graphics, mouseLocation));
    }

    public void draw(GraphicsContext graphics) {
        forEachTile((tile) -> tile.draw(graphics));
    }

    public void invalidate(GraphicsContext graphics) {
        graphics.clearRect(0, 0, boardWidth, boardHeight);
    }

    public boolean checkFlags() {
        boolean result = true;

        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                if (tiles[i][j].isFlaggedAndMine() == false) {
                    result = false;
                }
            }
        }

        return result;
    }

    public Tile getTile(Point mouseLocation) {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                if (tiles[i][j].hitTest(mouseLocation) == true) {
                    return tiles[i][j];
                }
            }
        }

        return null;
    }

    public double getBoardWidth() {
        return boardWidth;
    }

    public double getBoardHeight() {
        return boardHeight;
    }
}
