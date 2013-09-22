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
 * King class.
 *
 */
public final class King extends Piece {

    final static Point W_START_CASTLING = Point.fromXY(4, 0);
    final static Point W_LEFT_CASTLING  = Point.fromXY(2, 0);
    final static Point W_RIGHT_CASTLING = Point.fromXY(6, 0);
    final static Point W_LEFT_ROOK      = Point.fromXY(0, 0);
    final static Point W_RIGHT_ROOK     = Point.fromXY(7, 0);

    final static Point B_START_CASTLING = Point.fromXY(4, 7);
    final static Point B_LEFT_CASTLING  = Point.fromXY(2, 7);
    final static Point B_RIGHT_CASTLING = Point.fromXY(6, 7);
    final static Point B_LEFT_ROOK      = Point.fromXY(0, 7);
    final static Point B_RIGHT_ROOK     = Point.fromXY(7, 7);

    protected King(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int x1, int y1, int x2, int y2) {
        if (!super.isValidMove(x1, y1, x2, y2)) {
            return false;
        }
        return Math.abs(x2 - x1) <= 1 && Math.abs(y2 - y1) <= 1;
    }

    @Override
    public boolean isLegalMove(Point from, Point to, Board board) {
        if (super.isLegalMove(from, to, board)) {
            return true;
        }

        // Check if this is a castling move
        if (color == Color.W) {
            if (from.equals(W_START_CASTLING)) {
                // Has king been moved?
                if (board.hasPiecedMoved(from)) {
                    return false;
                }
                // Check additional castling conditions depending on direction
                if (to.equals(W_LEFT_CASTLING)) {
                    return checkCastlingConditions(W_LEFT_ROOK, board);
                } else if (to.equals(W_RIGHT_CASTLING)) {
                    return checkCastlingConditions(W_RIGHT_ROOK, board);
                }
            }
        } else {
            if (from.equals(B_START_CASTLING)) {
                // Has king been moved?
                if (board.hasPiecedMoved(from)) {
                    return false;
                }
                // Check additional castling conditions depending on direction
                if (to.equals(B_LEFT_CASTLING)) {
                    return checkCastlingConditions(B_LEFT_ROOK, board);
                } else if (to.equals(B_RIGHT_CASTLING)) {
                    return checkCastlingConditions(B_RIGHT_ROOK, board);
                }
            }
        }
        return false;
    }

    @Override
    public String toNotation() {
        return "K";
    }

    @Override
    public List<Point> generatePath(Point from, Point to) throws GameException {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<Point> generateMoves(Point from, Board board) {
        final List<Point> moves = new ArrayList<>();
        final int x = from.getX();
        final int y = from.getY();

        Point to;
        if (x > 0) {
            to = Point.fromXY(x - 1, y);
            if (isLegalMove(from, to, board)) {
                moves.add(to);
            }
            if (y > 0) {
                to = Point.fromXY(x - 1, y - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
            if (y < Board.N_SQUARES - 1) {
                to = Point.fromXY(x - 1, y + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
        }
        if (y > 0) {
            to = Point.fromXY(x, y - 1);
            if (isLegalMove(from, to, board)) {
                moves.add(to);
            }
        }
        if (x < Board.N_SQUARES - 1) {
            to = Point.fromXY(x + 1, y);
            if (isLegalMove(from, to, board)) {
                moves.add(to);
            }
            if (y < Board.N_SQUARES - 1) {
                to = Point.fromXY(x + 1, y + 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
            if (y > 0) {
                to = Point.fromXY(x + 1, y - 1);
                if (isLegalMove(from, to, board)) {
                    moves.add(to);
                }
            }
        }
        if (y < Board.N_SQUARES - 1) {
            to = Point.fromXY(x, y + 1);
            if (isLegalMove(from, to, board)) {
                moves.add(to);
            }
        }

        if (color == Color.W && from.equals(W_START_CASTLING) ||
                color == Color.B && from.equals(B_START_CASTLING)) {
            to = Point.fromXY(x - 2, y);
            if (isLegalMove(from, to, board)) {
                moves.add(to);
            }
            to = Point.fromXY(x + 2, y);
            if (isLegalMove(from, to, board)) {
                moves.add(to);
            }
        }

        return moves;
    }

    /**
     * Checks that (i) the rook has not been moved (ii) that there are no pieces
     * between the rook and the king and (iii) that king is not in check when
     * during and at the end of the castling move.
     *
     * @param rook Rook involved in move.
     * @param board The board.
     * @return Outcome of test.
     */
    private boolean checkCastlingConditions(Point rook, Board board) {
        boolean isAllowed;
        final boolean left = (rook.getX() == 0);
        final Point start = color == Color.W ? W_START_CASTLING : B_START_CASTLING;

        if (left) {
            isAllowed = !board.hasPiecedMoved(rook)
                    && !board.hasPiece(rook.incrementX(1))
                    && !board.hasPiece(rook.incrementX(2))
                    && !board.hasPiece(rook.incrementX(3))
                    && !board.isKingAttacked(color);
            if (isAllowed) {
                board.doMove(new Move(this, start, start.decrementX(1)));
                isAllowed = !board.isKingAttacked(color);
                board.undoLastMove();
                if (isAllowed) {
                    board.doMove(new Move(this, start, start.decrementX(2)));
                    isAllowed = !board.isKingAttacked(color);
                    board.undoLastMove();
                }
            }
        } else {
            isAllowed = !board.hasPiecedMoved(rook)
                    && !board.hasPiece(rook.decrementX(1))
                    && !board.hasPiece(rook.decrementX(2))
                    && !board.isKingAttacked(color);
            if (isAllowed) {
                board.doMove(new Move(this, start, start.incrementX(1)));
                isAllowed = !board.isKingAttacked(color);
                board.undoLastMove();
                if (isAllowed) {
                    board.doMove(new Move(this, start, start.incrementX(2)));
                    isAllowed = !board.isKingAttacked(color);
                    board.undoLastMove();
                }
            }
        }
        return isAllowed;
    }
}
