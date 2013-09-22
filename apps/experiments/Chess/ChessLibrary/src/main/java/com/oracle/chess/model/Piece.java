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


import static com.oracle.chess.model.Board.N_SQUARES;
import java.util.List;

/**
 * Piece class.
 *
 */
public abstract class Piece {

    public static final Pawn WHITE_PAWN = new Pawn(Color.W);
    public static final Pawn BLACK_PAWN = new Pawn(Color.B);

    public static final Rook WHITE_ROOK = new Rook(Color.W);
    public static final Rook BLACK_ROOK = new Rook(Color.B);

    public static final Knight WHITE_KNIGHT = new Knight(Color.W);
    public static final Knight BLACK_KNIGHT = new Knight(Color.B);

    public static final Bishop WHITE_BISHOP = new Bishop(Color.W);
    public static final Bishop BLACK_BISHOP = new Bishop(Color.B);

    public static final King WHITE_KING = new King(Color.W);
    public static final King BLACK_KING = new King(Color.B);

    public static final Queen WHITE_QUEEN = new Queen(Color.W);
    public static final Queen BLACK_QUEEN = new Queen(Color.B);
    
    protected Color color;

    protected Piece(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    private boolean inRange(int z) {
        return z >= 0 && z < N_SQUARES;
    }

    /**
     * Determines if this piece can move from (x1,y1) to (x2,y2) based
     * on its kind, regardless of other pieces or the game's state.
     *
     * @param x1 Source x coordinate.
     * @param y1 Source y coordinate.
     * @param x2 Destination x coordinate.
     * @param y2 Destination y coordinate.
     * @return Validity of move based on kind.
     */
    public boolean isValidMove(int x1, int y1, int x2, int y2) {
        return (x1 != x2 || y1 != y2) && inRange(x1) && inRange(y1) && inRange(x2) && inRange(y2);
    }

    /**
     * Determines is this piece can move from one point to another based
     * on its kind, regardless of other pieces or the game's state.
     *
     * @param from From point.
     * @param to To point.
     * @return Validity of move based on kind.
     */
    public boolean isValidMove(Point from, Point to) {
        return isValidMove(from.getX(), from.getY(), to.getX(), to.getY());
    }

    /**
     * Generates a list of points between <code>from</code> and <code>to</code>
     * for this piece, excluding <code>from</code> and <code>to</code>, or
     * throws an exception if that isn't possible.
     *
     * @param from From point.
     * @param to To point.
     * @return List of points in path excluding <code>from</code> and <code>to</code>.
     * @throws GameException If not allowed for this kind of piece.
     */
    public abstract List<Point> generatePath(Point from, Point to) throws GameException;

    /**
     * Generates a list of all <b>legal</b> moves for this piece.
     *
     * @param from From point.
     * @param board The chessboard.
     * @return List of points that this piece can move to.
     */
    public abstract List<Point> generateMoves(Point from, Board board);

    /**
     * A move is legal if (i) it is valid for this piece and (ii) it can be completed
     * without being blocked by any other piece on the board. Note that this method
     * does not check if the king is in check after this move.
     *
     * @param from From point.
     * @param to To point.
     * @param board The chess board.
     * @return Legality of move based on kind and game state.
     */
    public boolean isLegalMove(Point from, Point to, Board board) {
        if (!isValidMove(from, to)) {
            return false;
        }
        try {
            List<Point> path = generatePath(from, to);
            for (Point p : path) {
                if (board.hasPiece(p)) {
                    return false;       // Another piece in the way
                }
            }
            // Check if trying to capture piece of same color
            Piece other = board.getPiece(to);
            if (other != null && other.getColor() == color) {
                return false;
            }
        } catch (GameException _) {
            return false;
        }
        return true;
    }

    /**
     * Determines if this piece can be promoted when moved to this point. This
     * method is overridden in {@link com.oracle.chess.model.Pawn}.
     *
     * @param to To point.
     * @return Outcome of promotion test.
     */
    public boolean isPromoted(Point to) {
        return false;
    }

    public abstract String toNotation();

    public static Piece fromNotation(Color color, String notation) {
        char ch = notation.charAt(0);
        switch (ch) {
            case 'P':
                return color == Color.W ? WHITE_PAWN : BLACK_PAWN;
            case 'R':
                return color == Color.W ? WHITE_ROOK : BLACK_ROOK;
            case 'N':
                return color == Color.W ? WHITE_KNIGHT : BLACK_KNIGHT;
            case 'B':
                return color == Color.W ? WHITE_BISHOP : BLACK_BISHOP;
            case 'K':
                return color == Color.W ? WHITE_KING : BLACK_KING;
            case 'Q':
                return color == Color.W ? WHITE_QUEEN : BLACK_QUEEN;
            default:
                throw new InternalError("Unknown piece notation " + notation);
        }
    }

    public static Piece fromString(String s) {
        return fromNotation(Color.valueOf(s.substring(0, 1)), s.substring(1));
    }

    @Override
    public String toString() {
        return color.toString() + toNotation();
    }
}
