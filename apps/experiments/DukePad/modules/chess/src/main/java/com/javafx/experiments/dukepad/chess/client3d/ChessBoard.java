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

package com.javafx.experiments.dukepad.chess.client3d;


import com.javafx.experiments.dukepad.chess.client3d.ChessPiece.Role;
import com.javafx.experiments.dukepad.chess.client3d.ChessPiece.Side;
import com.oracle.chess.model.Board;
import com.oracle.chess.model.Piece;
import com.oracle.chess.model.Point;
import com.oracle.chess.protocol.BoardRep;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.*;

import static com.javafx.experiments.dukepad.chess.client3d.Utils3D.ReparentableGroup;
import static com.javafx.experiments.dukepad.chess.client3d.Utils3D.centerOnTop;


/**
 *
 */
public class ChessBoard extends Group {

    public static final double IN = 20;
    private static final int NUM_CELLS = Board.N_SQUARES;
    private static final double BOARD_WIDTH = 19.685 * IN; //16 * IN;
    private static final double CELL_WIDTH = BOARD_WIDTH / NUM_CELLS;
    private static final double DUKE_SIZE = CELL_WIDTH * 0.7;
    private static final double CELL_HEIGHT = 0.1 * IN;
    private final Rotate rotate180;

    private Board board;

    private Cell[][] cells = new Cell[NUM_CELLS][NUM_CELLS];
    private Node[][] cellContent = new Node[NUM_CELLS][NUM_CELLS];
    private Map<Node, Point> node2point = new HashMap<>();
    private Group pieces = new Group();
    private List<Node> blacks = new ArrayList<>(16);
    private List<Node> whites = new ArrayList<>(16);
    private CellClickedEventListener listener;
    private Queue<Runnable> queue = new LinkedList<>();
    private boolean queueStopped = true;
    private ObjectProperty<Color> selectionColor = new SimpleObjectProperty<>(Color.RED);

    private void processQueue() {
        Runnable runnable = queue.poll();
        if (runnable != null) {
            queueStopped = false;
            runnable.run();
        } else {
            queueStopped = true;
        }
    }

    public void animate(final Point from, final Point to) {
        animate(from, to, to);
    }

    public void animate(final Point from, final Point to, final Point capture) {
        System.out.println("Chessboard, animate from = " + from.toNotation() + ", to = " + to.toNotation() + " requested " + from + " - " + to);
        addToQueue(new Runnable() {

            @Override
            public void run() {
                System.out.println("Chessboard, animate from = " + from.toNotation() + ", to = " + to.toNotation() + " started " + from + " - "  + to);
                final Node duke = cellContent[from.getX()][from.getY()];
                cellContent[from.getX()][from.getY()] = null;
                final Node duke2 = cellContent[capture.getX()][capture.getY()];
                cellContent[to.getX()][to.getY()] = duke;
                node2point.put(duke, to);
//                System.out.println("duke.getTranslateX() = " + duke.getTranslateX());
//                System.out.println("duke.getTranslateZ() = " + duke.getTranslateZ());
                double tx = to.getX() * CELL_WIDTH;
                double tz = (NUM_CELLS - 1 - to.getY()) * CELL_WIDTH;
//                System.out.println("tx = " + tx);
//                System.out.println("tz = " + tz);
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), duke);
                tt.setToX(tx);
                tt.setToZ(tz);

                final ParallelTransition newAnimation = new ParallelTransition(tt);

                if (duke2 != null) {
                    node2point.remove(duke2);
                    blacks.remove(duke2);
                    whites.remove(duke2);
                    TranslateTransition tt2 = new TranslateTransition(Duration.millis(300), duke2);
                    tt2.setToY(500);
                    tt2.setOnFinished(new EventHandler<ActionEvent>() {

                        @Override
                        public void handle(ActionEvent t) {
                            pieces.getChildren().remove(duke2);
                        }
                    });

                    RotateTransition rt2 = new RotateTransition(Duration.millis(300), duke2);
                    rt2.setToAngle(360 * 10);

                    newAnimation.getChildren().addAll(tt2, rt2);
                }
                newAnimation.setOnFinished(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent t) {
                        processQueue();
                    }
                });
                newAnimation.play();
            }
        });
    }

    public void highlightMoves(final com.oracle.chess.model.Color color) {
        addToQueue(new Runnable() {

            @Override
            public void run() {
                List<Node> moves = (color == com.oracle.chess.model.Color.B) ? blacks : whites;
                clearHighlighted();
                for (Node p : moves) {
                    Point pos = node2point.get(p);
                    addToHighlight(pos);
                }
                processQueue();
            }
        });
    }

    private void clearHighlighted() {
        for (int x = 0; x < NUM_CELLS; x++) {
            for (int y = 0; y < NUM_CELLS; y++) {
                cells[x][y].highlighted.set(false);
            }
        }
    }

    private void addToHighlight(Point pos) {
        cells[pos.getX()][pos.getY()].highlighted.set(true);
    }

    private void addToQueue(Runnable runnable) {
        queue.add(runnable);
        if (queueStopped) {
            processQueue();
        }
    }

    public void highlightMoves(final List<String> moves) {
        addToQueue(new Runnable() {

            @Override
            public void run() {
                clearHighlighted();
                for (String p : moves) {
                    Point pos = Point.fromNotation(p);
                    addToHighlight(pos);
                }
                processQueue();
            }
        });
    }

    public void unhighlightMoves() {
        addToQueue(new Runnable() {
            @Override
            public void run() {
                clearHighlighted();
                processQueue();
            }
        });
    }

    public void rotate180(boolean black) {
        rotate180.setAngle(black ? 180 : 0);
    }

    public static interface CellClickedEventListener {
        void cellClicked(int x, int y);
    }

    public void setListener(CellClickedEventListener listener) {
        this.listener = listener;
    }

    private class Cell extends Box {

        private boolean black;
        private BooleanProperty pressed = new SimpleBooleanProperty(false);
        private BooleanProperty highlighted = new SimpleBooleanProperty(false);

        public Cell(final int x, final int y, com.oracle.chess.model.Color color) {
            super(CELL_WIDTH, CELL_HEIGHT, CELL_WIDTH);
            black = (color == com.oracle.chess.model.Color.B);
            materialProperty().bind(
                    Bindings.when(pressed)
                            .then(black ? PRESSED_BLACK_MATERIAL : PRESSED_WHITE_MATERIAL)
                            .otherwise(
                                    Bindings.when(highlighted)
                                            .then(black ? HIGHLIGHTED_BLACK_MATERIAL : HIGHLIGHTED_WHITE_MATERIAL)
                                            .otherwise(black ? BLACK_MATERIAL : WHITE_MATERIAL)));
            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    pressed.set(true);
                }
            });
            setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    pressed.set(false);
                }
            });
            setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {
                    if (t.isStillSincePress()) {
                        cellClicked(x, y);
                        t.consume();
                    }
                }
            });
        }

    }

    private final PhongMaterial WHITE_MATERIAL = new PhongMaterial();
    private final PhongMaterial BLACK_MATERIAL = new PhongMaterial();
    private final PhongMaterial HIGHLIGHTED_WHITE_MATERIAL = new PhongMaterial();
    private final PhongMaterial HIGHLIGHTED_BLACK_MATERIAL = new PhongMaterial();
    private final PhongMaterial PRESSED_WHITE_MATERIAL = new PhongMaterial();
    private final PhongMaterial PRESSED_BLACK_MATERIAL = new PhongMaterial();

    {
        WHITE_MATERIAL.setDiffuseColor(Color.WHITE);
        BLACK_MATERIAL.setDiffuseColor(Color.DARKGRAY);
        PRESSED_WHITE_MATERIAL.setDiffuseColor(Color.WHITE.interpolate(Color.RED, 0.8));
        PRESSED_BLACK_MATERIAL.setDiffuseColor(Color.DARKGRAY.interpolate(Color.RED, 0.8));
        HIGHLIGHTED_WHITE_MATERIAL.diffuseColorProperty().bind(new ObjectBinding<Color>() {
            {
                bind(selectionColor);
            }

            @Override
            protected Color computeValue() {
                return selectionColor.get().interpolate(WHITE_MATERIAL.getDiffuseColor(), 0.5);
            }
        });
        HIGHLIGHTED_BLACK_MATERIAL.diffuseColorProperty().bind(new ObjectBinding<Color>() {
            {
                bind(selectionColor);
            }
            @Override
            protected Color computeValue() {
                return selectionColor.get().interpolate(BLACK_MATERIAL.getDiffuseColor(), 0.5);
            }
        });
    }

    public ChessBoard(Board board) {
        this.board = board;
        for (int x = 0; x < NUM_CELLS; x++) {
            for (int y = 0; y < NUM_CELLS; y++) {
                cells[x][y] = new Cell(x, y, board.getSquare(Point.fromXY(x, y)).getColor());
                cells[x][y].setTranslateX(x * CELL_WIDTH);
                cells[x][y].setTranslateZ((NUM_CELLS - y - 1) * CELL_WIDTH);
                getChildren().add(cells[x][y]);
            }
        }
        getChildren().addAll(pieces);
        pieces.setMouseTransparent(true);

        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(selectionColor, Color.RED)),
                new KeyFrame(Duration.millis(1500), new KeyValue(selectionColor, Color.rgb(255, 200, 200))));
        t.setCycleCount(Timeline.INDEFINITE);
        t.setAutoReverse(true);
        t.play();

        double cx = 3.5 * CELL_WIDTH;
        double cy = 0;
        double cz = 3.5 * CELL_WIDTH;
        rotate180 = new Rotate(0, cx, cy, cz, Rotate.Y_AXIS);
        getTransforms().add(rotate180);
    }

    public void update(final BoardRep board) {
        System.out.println("Chessboard, update requested");
        addToQueue(new Runnable() {

            @Override
            public void run() {
                System.out.println("Chessboard, actual update");
                for (int x = 0; x < NUM_CELLS; x++) {
                    Arrays.fill(cellContent[x], null);
                }
                pieces.getChildren().clear();
                blacks.clear();
                whites.clear();
                for (String nt : board.getBlacks()) {
                    blacks.add(add(Piece.fromNotation(com.oracle.chess.model.Color.B, nt), Point.fromNotation(nt.substring(1))));
                }
                for (String nt : board.getWhites()) {
                    whites.add(add(Piece.fromNotation(com.oracle.chess.model.Color.W, nt), Point.fromNotation(nt.substring(1))));
                }
                processQueue();
            }
        });
    }

    private final static Map<String, Role> notation2role = new HashMap<>();

    static {
        notation2role.put(Piece.WHITE_PAWN.toNotation(), Role.PAWN);
        notation2role.put(Piece.WHITE_KNIGHT.toNotation(), Role.KNIGHT);
        notation2role.put(Piece.WHITE_BISHOP.toNotation(), Role.BISHOP);
        notation2role.put(Piece.WHITE_ROOK.toNotation(), Role.ROOK);
        notation2role.put(Piece.WHITE_QUEEN.toNotation(), Role.QUEEN);
        notation2role.put(Piece.WHITE_KING.toNotation(), Role.KING);
    }

    private Node add(Piece piece, Point point) {
        final int x = point.getX(), y = point.getY();
        Side side = piece.getColor() == com.oracle.chess.model.Color.B ? Side.BLACK : Side.WHITE;
        Role role = notation2role.get(piece.toNotation());
//        System.out.println("x = " + x + ", y = " + y + ", side = " + side + ", role = " + role);
        final Group duke = ChessPiece.getChessPieceFactory().createPiece(side, role);
        final ReparentableGroup dukeParent = new ReparentableGroup(duke);
        pieces.getChildren().add(dukeParent);
        centerOnTop(dukeParent, cells[x][y]);
//        putOnTop(dukeParent, allCells);
//        dukeParent.setTranslateX(x * CELL_WIDTH);
//        dukeParent.setTranslateZ((NUM_CELLS - 1 - y) * CELL_WIDTH);
        if (side == Side.WHITE) {
            duke.setRotate(180);
            duke.setRotationAxis(Rotate.Y_AXIS);
        }

        dukeParent.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
                if (t.isStillSincePress()) {
                    Point point = node2point.get(dukeParent);
                    cellClicked(point.getX(), point.getY());
                    t.consume();
                }
            }
        });
        cellContent[x][y] = dukeParent;
        node2point.put(dukeParent, point);
        return dukeParent;
    }

    private void cellClicked(int x, int y) {
        System.out.println("cellClicked x = " + x + ", y = " + y);
        if (listener != null) {
            listener.cellClicked(x, y);
        }
    }

    public boolean isThereAPiece(int x, int y) {
        return cellContent[x][y] != null;
    }
}
