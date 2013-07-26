/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package stretchygrid;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A fabric made up of a whole bunch of lines. When using the application, you use the
 * mouse to click and drag on the fabric, and doing so will cause the fabric to be pulled.
 * The lines on this fabric are springy and will stretch and return to their original shape.
 *
 * This toy is a useful test to see what the overall cost of a large scene graph is, using
 * nothing but lines. The parameters passed to the grid constructor determine the number of
 * blocks horizontally and vertically, and the size of each block. By increasing the number of
 * blocks you put more load on the system, by decreasing the number of blocks, less load.
 */
public class StretchyGrid extends Application {
    @Override public void start(Stage primaryStage) throws Exception {
        final Grid grid = new Grid(320, 320, 4);
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                grid.update();
            }
        };
        timer.start();

        Scene scene = new Scene(grid);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Cell {
    Grid grid;
    int row, col;
    DoubleProperty x = new SimpleDoubleProperty();
    DoubleProperty y = new SimpleDoubleProperty();
    double xv, yv, cx, cy;
    Cell left, right, bottom, top;

    Cell(Grid grid, int row, int col) {
        this.row = row;
        this.col = col;
        this.grid = grid;
        x.set(col * grid.gSize);
        y.set(row * grid.gSize);
        cx = x.get();
        cy = y.get();
    }

    // called after the whole grid.cells sequence has been initialized/filled
    void initAdjacencies() {
        left   = (col > 0)             ? grid.cells[row * grid.width + col-1] : null;
        right  = (col < grid.width-1)  ? grid.cells[row * grid.width + col+1] : null;
        top    = (row > 0)             ? grid.cells[(row-1) * grid.width + col] : null;
        bottom = (row < grid.height-1) ? grid.cells[(row+1) * grid.width + col] : null;
    }
}

class Grid extends Group {
    double xmouse, ymouse;
    double k1 = 0.01;
    double k2 = 0.20000000000000001;
    double damp = 0.72999999999999998;
    int drag = -1;
    boolean dragging;
    final int width;
    final int height;
    final double gSize;
    final Cell[] cells;

    Grid(int numCellsWide, int numCellsHigh, double cellSize) {
        this.width = numCellsWide;
        this.height = numCellsHigh;
        this.gSize = cellSize;

        cells = new Cell[height * width];
        for (int row=0; row<height; row++) {
            for (int col=0; col<width; col++) {
                cells[row*height+col] = new Cell(this, row, col);
            }
        }

        for (Cell cell : cells) {
            cell.initAdjacencies();
        }

        getChildren().add(new Group() {{
            getChildren().addAll(new Group() {{
                Rectangle r = new Rectangle();
                r.setSmooth(false);
                r.setWidth(width * gSize);
                r.setHeight(height * gSize);
                r.setFill(Color.BLACK);
                r.setOnMousePressed((e) -> {
                    int row = (int)(e.getSceneY() / gSize);
                    int col = (int)(e.getSceneX() / gSize);
                    if (row >= 0 && row < height && col >=0 && col < width) {
                        xmouse = e.getSceneX();
                        ymouse = e.getSceneY();
                        drag = row * width + col;
                    }
                    dragging = true;
                });
                r.setOnMouseDragged((e) -> {
                    xmouse = e.getSceneX();
                    ymouse = e.getSceneY();
                });
                r.setOnMouseReleased((e) -> {
                    dragging = false;
                    drag = -1;
                });

                Group g = new Group();
                for (Cell cell : cells) {
                    Group gg = new Group();
                    if (cell.right != null) {
                        Line line = new Line();
                        line.setSmooth(false);
                        line.setStroke(Color.WHITE);
                        line.startXProperty().bind(cell.x);
                        line.startYProperty().bind(cell.y);
                        line.endXProperty().bind(cell.right.x);
                        line.endYProperty().bind(cell.right.y);
                        gg.getChildren().add(line);
                    }
                    if (cell.bottom != null) {
                        Line line = new Line();
                        line.setSmooth(false);
                        line.setStroke(Color.WHITE);
                        line.startXProperty().bind(cell.x);
                        line.startYProperty().bind(cell.y);
                        line.endXProperty().bind(cell.bottom.x);
                        line.endYProperty().bind(cell.bottom.y);
                        gg.getChildren().add(line);
                    }
                    g.getChildren().add(gg);
                }

                getChildren().addAll(r, g);
            }});
        }});

        /* // uncomment to use rectangles instead of lines
        [if (cell.right == null) null else Rectangle {
            smooth: false;
            stroke: null;//Color.GREEN;
            fill: Color.GREEN
            x: bind cell.x;
            y: bind cell.y;
            width: bind (cell.right.x-cell.x);
            height: 1//bind (cell.y - cell.right.y);
        },
        if (cell.bottom == null) null else Rectangle {
             smooth: false;
             fill: Color.RED;
             stroke: null
             x: bind cell.x;
             y: bind cell.y;
             width: 1//bind cell.bottom.x;
             height: bind (cell.bottom.y-cell.y);
        }]
        */
    }

    void update() {
        if (dragging) {
            cells[drag].x.set((int)xmouse);
            cells[drag].y.set((int)ymouse);
        }
        int i = 0;
        for (Cell cell : cells) {
            if (drag != i) {
                cell.xv += (cell.cx - cell.x.get())*k1;
                cell.yv += (cell.cy - cell.y.get())*k1;
                if (cell.left != null) {
                    cell.xv += (cell.left.x.get() + gSize - cell.x.get()) *k2;
                    cell.yv += (cell.left.y.get() - cell.y.get()) *k2;
                }
                if (cell.right != null) {
                    cell.xv += (cell.right.x.get() - gSize - cell.x.get()) *k2;
                    cell.yv += (cell.right.y.get() - cell.y.get()) *k2;
                }
                if (cell.top != null) {
                    cell.xv += (cell.top.x.get() - cell.x.get()) *k2;
                    cell.yv += (cell.top.y.get() + gSize - cell.y.get()) *k2;
                }
                if (cell.bottom != null) {
                    cell.xv += (cell.bottom.x.get() - cell.x.get()) *k2;
                    cell.yv += (cell.bottom.y.get() - gSize - cell.y.get()) *k2;
                }
                cell.xv *= damp;
                cell.yv *= damp;
                cell.x.set(cell.x.get() + (int)cell.xv);
                cell.y.set(cell.y.get() + (int)cell.yv);
            }
            i++;
        }
    }
}
