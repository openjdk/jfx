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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.oracle.chess.model.Piece.*;

/**
 * Board class.
 *
 */
public final class Board {

    public static final int N_SQUARES = 8;

    private static final Piece[] WHITE_ROW = {
        WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN,
        WHITE_KING, WHITE_BISHOP, WHITE_KNIGHT, WHITE_ROOK };

    private static final Piece[] BLACK_ROW = {
        BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN,
        BLACK_KING, BLACK_BISHOP, BLACK_KNIGHT, BLACK_ROOK };

    /**
     * Board is comprised of 8x8 squares.
     */
    private Square[][] squares = new Square[N_SQUARES][N_SQUARES];

    /**
     * Square of white king on the board.
     */
    private Square whiteKing;

    /**
     * Square of black king on the board.
     */
    private Square blackKing;

    /**
     * List of moves made so far.
     */
    private List<Move> moves = new ArrayList<>();

    public Board() {
        initialize();
    }

    public void initialize() {
        // Init white pieces on board
        for (int x = 0; x < N_SQUARES; x++) {
            squares[x][0] = new Square(x, 0, WHITE_ROW[x]);
            if (WHITE_ROW[x] == WHITE_KING) {
                whiteKing = squares[x][0];
            }
        }
        for (int x = 0; x < N_SQUARES; x++) {
            squares[x][1] = new Square(x, 1, WHITE_PAWN);
        }

        // Init black pieces on board
        for (int x = 0; x < N_SQUARES; x++) {
            squares[x][7] = new Square(x, 7, BLACK_ROW[x]);
            if (BLACK_ROW[x] == BLACK_KING) {
                blackKing = squares[x][7];
            }
        }
        for (int x = 0; x < N_SQUARES; x++) {
            squares[x][6] = new Square(x, 6, BLACK_PAWN);
        }

        // Init all other empty squares
        for (int y = 2; y <= 5; y++) {
            for (int x = 0; x < N_SQUARES; x++) {
                squares[x][y] = new Square(x, y);
            }
        }
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }

    public Move getLastMove() {
        return moves.isEmpty() ? null : moves.get(moves.size() - 1);
    }

    public void doMove(Move move) {
        final Point from = move.getFrom();
        final Point to = move.getTo();
        final Piece piece = move.getPiece();

        // Carry out the move on the board
        squares[from.getX()][from.getY()].setPiece(null);
        move.setCaptured(squares[to.getX()][to.getY()].getPiece());
        squares[to.getX()][to.getY()].setPiece(piece);

        // Keep track of the kings
        if (piece == WHITE_KING) {
            whiteKing = squares[to.getX()][to.getY()];
        } else if (piece == BLACK_KING) {
            blackKing = squares[to.getX()][to.getY()];
        }

        // Check for castling first
        if (move.isLeftCastling()) {
            if (piece.getColor() == Color.W) {
                setPiece(null, King.W_LEFT_ROOK);
                setPiece(Piece.WHITE_ROOK, to.incrementX(1));
            } else {
                setPiece(null, King.B_LEFT_ROOK);
                setPiece(Piece.BLACK_ROOK, to.incrementX(1));
            }
        } else if (move.isRightCastling()) {
            if (piece.getColor() == Color.W) {
                setPiece(null, King.W_RIGHT_ROOK);
                setPiece(Piece.WHITE_ROOK, to.decrementX(1));
            } else {
                setPiece(null, King.B_RIGHT_ROOK);
                setPiece(Piece.BLACK_ROOK, to.decrementX(1));
            }
        } else {
            // Check for pawn promotions
            if (piece.isPromoted(to)) {
                setPiece(piece.getColor().getQueen(), to);
                move.setPromoted(true);
            }

            // En passant?
            final Move lastMove = getLastMove();
            if (move.isEnPassantAllowed(lastMove)) {
                final Point lastTo = lastMove.getTo();
                move.setEnPassant(true);
                move.setCaptured(squares[lastTo.getX()][lastTo.getY()].getPiece());
                squares[lastTo.getX()][lastTo.getY()].setPiece(null);
            }
        }

        // Record last move
        moves.add(move);
    }

    public void undoLastMove() {
        // Check that we have a move to undo
        if (moves.isEmpty()) {
            throw new InternalError("No move available to undo");
        }

        final Move lastMove = getLastMove();
        final Point from = lastMove.getFrom();
        final Point to = lastMove.getTo();
        final Piece piece = lastMove.getPiece();

        squares[from.getX()][from.getY()].setPiece(piece);

        if (lastMove.isLeftCastling()) {
            squares[to.getX()][to.getY()].setPiece(null);
            if (piece.getColor() == Color.W) {
                setPiece(Piece.WHITE_ROOK, King.W_LEFT_ROOK);
                setPiece(null, to.incrementX(1));
            } else {
                setPiece(Piece.BLACK_ROOK, King.B_LEFT_ROOK);
                setPiece(null, to.incrementX(1));
            }
        } else if (lastMove.isRightCastling()) {
            squares[to.getX()][to.getY()].setPiece(null);
            if (piece.getColor() == Color.W) {
                setPiece(Piece.WHITE_ROOK, King.W_RIGHT_ROOK);
                setPiece(null, to.decrementX(1));
            } else {
                setPiece(Piece.BLACK_ROOK, King.B_RIGHT_ROOK);
                setPiece(null, to.decrementX(1));
            }
        } else {
            final Piece captured = lastMove.getCaptured();

            // Undoing an en passant move?
            if (lastMove.isEnPassant()) {
                if (captured.getColor() == Color.B) {
                    squares[to.getX()][to.getY() - 1].setPiece(captured);
                } else {
                    squares[to.getX()][to.getY() + 1].setPiece(captured);
                }
                squares[to.getX()][to.getY()].setPiece(null);
            } else {
                squares[to.getX()][to.getY()].setPiece(captured);
            }

            // Keep track of the kings
            if (piece == WHITE_KING) {
                whiteKing = squares[from.getX()][from.getY()];
            } else if (piece == BLACK_KING) {
                blackKing = squares[from.getX()][from.getY()];
            }
        }

        // Remove move from history
        moves.remove(moves.size() - 1);
    }

    public boolean hasPiecedMoved(Point from) {
        for (Move move : moves) {
            if (move.getFrom().equals(from)) {
                return true;
            }
        }
        return false;
    }

    public Piece getPiece(Point p) {
        return squares[p.getX()][p.getY()].getPiece();
    }
    
    public boolean hasPiece(Point p) {
        return squares[p.getX()][p.getY()].getPiece() != null;
    }

    public Square getSquare(Point p) {
        return squares[p.getX()][p.getY()];
    }

    public boolean hasColoredPiece(Point p, Color color) {
        return hasPiece(p) && getPiece(p).getColor() == color;
    }

    public Square getKingSquare(Color color) {
        return color == Color.W ? whiteKing : blackKing;
    }

    public void clear() {
        for (int x = 0; x < N_SQUARES; x++) {
            for (int y = 0; y < N_SQUARES; y++) {
                squares[x][y] = new Square(x, y);
            }
        }

        // Need at least the kings on the board
        final Point wk = Point.fromXY(4, 0);
        whiteKing = squares[wk.getX()][wk.getY()] = new Square(wk, WHITE_KING);
        final Point bk = Point.fromXY(4, 7);
        blackKing = squares[bk.getX()][bk.getY()] = new Square(bk, BLACK_KING);
    }

    public void setPiece(Piece piece, Point p) {
        final Square sq = squares[p.getX()][p.getY()];
        sq.setPiece(piece);
        
        // Keep track of the kings
        if (piece == WHITE_KING && whiteKing != sq) {
            whiteKing.setPiece(null);
            whiteKing = sq;
        } else if (piece == BLACK_KING && blackKing != sq) {
            blackKing.setPiece(null);
            blackKing = sq;
        }
    }

    public Iterator<Square> getIterator(final Color filter) {
        return new Iterator<Square>() {
            private static final int TOTAL_SQUARES = N_SQUARES * N_SQUARES;

            private Color color = filter;

            private int lastK = 0;

            private Square next = null;

            private Square findNext() {
                int k;
                for (k = lastK; k < TOTAL_SQUARES; k++) {
                    final Point p = Point.fromXY(k % N_SQUARES, k / N_SQUARES);
                    if (hasColoredPiece(p, color)) {
                        lastK = k + 1;
                        return getSquare(p);
                    }
                }
                lastK = k;
                return null;
            }

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = findNext();
                }
                return next != null;
            }

            @Override
            public Square next() {
                if (next == null) {
                    findNext();
                    if (next == null) {
                        throw new NoSuchElementException("No more pieces on the board");
                    }
                }
                final Square result = next;
                next = null;
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    List<Point> queryMoves(Point from) {
        final Piece piece = getPiece(from);
        return piece.generateMoves(from, this);
    }

    /**
     * Determines if the king of <code>color</code> is in check or not.
     *
     * @param color King's color.
     * @return Result of in-check test.
     */
    public boolean isKingAttacked(Color color) {
        final Point toKing = getKingSquare(color).getPoint();
        return kingAttackers(color, toKing) != null;
    }

    /**
     * Determines if the king of <code>color</code> is in check or not
     * after moving to <code>toKing</code>.
     *
     * @param color King's color.
     * @param toKing King's location on the board.
     * @return Result of in-check test.
     */
    public boolean isKingAttacked(Color color, Point toKing) {
        return kingAttackers(color, toKing) != null;
    }

    /**
     * Returns the list of pieces that are attacking the king of <code>color</code> or
     * <code>null</null> if king is not in check.
     *
     * @param color King's color.
     * @param toKing King's location on the board.
     * @return The list of pieces attacking king or <code>null</code> if not in check.
     */
    public List<Square> kingAttackers(Color color, Point toKing) {
        List<Square> result = null;
        final Color opponent = color.getOpponentColor();
        Iterator<Square> iter = getIterator(opponent);
        while (iter.hasNext()) {
            final Square square = iter.next();
            if (square.getPiece().isLegalMove(square.getPoint(), toKing, this)) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(square);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("+---+---+---+---+---+---+---+---+\n");
        for (int y = N_SQUARES - 1; y >=0; y--) {
            sb.append("|");
            for (int x = 0; x < N_SQUARES; x++) {
                sb.append(squares[x][y]).append("|");
            }
            sb.append("\n");
            sb.append("+---+---+---+---+---+---+---+---+\n");
        }
        return sb.toString();
    }


}
