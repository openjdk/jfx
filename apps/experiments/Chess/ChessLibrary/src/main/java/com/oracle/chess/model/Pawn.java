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
import java.util.List;

/**
 * Pawn class.
 *
 */
public final class Pawn extends Piece {

    protected Pawn(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int x1, int y1, int x2, int y2) {
        if (!super.isValidMove(x1, y1, x2, y2)) {
            return false;
        }
        switch (color) {
            case W:
                return x1 == x2 && y2 - y1 == 1 ||                 // one square forward
                       x1 == x2 && y2 - y1 == 2 && y1 == 1 ||      // two squares forward at start
                       y2 - y1 == 1 && Math.abs(x2 - x1) == 1;     // captures
            case B:
                return x1 == x2 && y2 - y1 == -1 ||                // one square forward
                       x1 == x2 && y2 - y1 == -2 && y1 == 6 ||     // two squares forward at start
                       y2 - y1 == -1 && Math.abs(x2 - x1) == 1;    // captures
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean isLegalMove(Point from, Point to, Board board) {
        if (!super.isLegalMove(from, to, board)) {
            return false;
        }

        if (from.getX() == to.getX()) {
            return !board.hasPiece(to);
        } else if (board.hasPiece(to)) {
            return board.hasColoredPiece(to, color.getOpponentColor());        // a normal capture
        } else {
            // Perhaps an en passant move?
            return new Move(this, from, to).isEnPassantAllowed(board.getLastMove());
        }
    }

    @Override
    public String toNotation() {
        return "P";
    }

    @Override
    public List<Point> generatePath(Point from, Point to) throws GameException {
        if (!isValidMove(from, to)) {
            throw new GameException(this, from, to);
        }
        // Move is valid for pawn, so we use queen here
        return WHITE_QUEEN.generatePath(from, to);
    }

    @Override
    public List<Point> generateMoves(Point from, Board board) {
        Point to;
        final List<Point> moves = new ArrayList<>();

        switch (color) {
            case W:
                to = Point.fromXY(from.getX(), from.getY() + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                to = Point.fromXY(from.getX(), from.getY() + 2);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                to = Point.fromXY(from.getX() + 1, from.getY() + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                to = Point.fromXY(from.getX() - 1, from.getY() + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                break;
            case B:
                to = Point.fromXY(from.getX(), from.getY() - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                to = Point.fromXY(from.getX(), from.getY() - 2);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                to = Point.fromXY(from.getX() + 1, from.getY() - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                to = Point.fromXY(from.getX() - 1, from.getY() - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
                break;
        }
        return moves;
    }

    @Override
    public boolean isPromoted(Point to) {
        return color == Color.W && to.getY() == 7 || color == Color.B && to.getY() == 0;
    }
}
