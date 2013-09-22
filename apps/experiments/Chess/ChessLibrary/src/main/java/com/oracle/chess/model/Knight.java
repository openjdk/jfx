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

package com.oracle.chess.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Knight class.
 *
 */
public final class Knight extends Piece {

    protected Knight(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int x1, int y1, int x2, int y2) {
        if (!super.isValidMove(x1, y1, x2, y2)) {
            return false;
        }
        return Math.abs(y2 - y1) == 2 && Math.abs(x2 - x1) == 1
                || Math.abs(y2 - y1) == 1 && Math.abs(x2 - x1) == 2;
    }

    @Override
    public String toNotation() {
        return "N";
    }

    @Override
    public List<Point> generatePath(Point from, Point to) throws GameException {
        if (!isValidMove(from, to)) {
            throw new GameException(this, from, to);
        }
        return Collections.EMPTY_LIST;      // horses can jump!
    }

    @Override
    public List<Point> generateMoves(Point from, Board board) {
        final List<Point> moves = new ArrayList<>();
        int x = from.getX();
        int y = from.getY();

        Point to;
        if (x + 1 < Board.N_SQUARES) {
            if (y + 2 < Board.N_SQUARES) {
                to = Point.fromXY(x + 1, y + 2);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
            if (y - 2 >= 0) {
                to = Point.fromXY(x + 1, y - 2);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
        }
        if (x - 1 >= 0) {
            if (y + 2 < Board.N_SQUARES) {
                to = Point.fromXY(x - 1, y + 2);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
            if (y - 2 >= 0) {
                to = Point.fromXY(x - 1, y - 2);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
        }
        if (x + 2 < Board.N_SQUARES) {
            if (y + 1 < Board.N_SQUARES) {
                to = Point.fromXY(x + 2, y + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
            if (y - 1 >= 0) {
                to = Point.fromXY(x + 2, y - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
        }
        if (x - 2 >= 0) {
            if (y + 1 < Board.N_SQUARES) {
                to = Point.fromXY(x - 2, y + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
            if (y - 1 >= 0) {
                to = Point.fromXY(x - 2, y - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
        }
        return moves;
    }
}
