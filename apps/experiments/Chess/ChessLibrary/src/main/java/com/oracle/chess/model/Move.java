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

/**
 * Move class.
 *
 */
public class Move {

    public enum Type {
        NORMAL, PROMOTION, EN_PASSANT, LEFT_CASTLING, RIGHT_CASTLING
    };

    private Point from;
    
    private Point to;
    
    private Piece piece;
    
    private Piece captured;

    private boolean promoted;

    private boolean enPassant;

    public Move() {
    }

    public Move(Piece piece, Point from, Point to) {
        this(piece, from, to, null);
    }

    public Move(Piece piece, Point from, Point to, Piece captured) {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.captured = captured;
    }

    public Point getFrom() {
        return from;
    }

    public void setFrom(Point from) {
        this.from = from;
    }

    public Point getTo() {
        return to;
    }

    public void setTo(Point to) {
        this.to = to;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Color getColor() {
        return piece.getColor();
    }
    
    public Piece getCaptured() {
        return captured;
    }

    public void setCaptured(Piece captured) {
        this.captured = captured;
    }

    public boolean hasCaptured() {
        return captured != null;
    }

    public boolean isPromoted() {
        return promoted;
    }

    public void setPromoted(boolean promoted) {
        this.promoted = promoted;
    }

    public boolean isEnPassant() {
        return enPassant;
    }

    public void setEnPassant(boolean enPassant) {
        this.enPassant = enPassant;
    }

    public boolean isEnPassantAllowed(Move lastMove) {
        if (lastMove != null && piece instanceof Pawn) {
            if (piece.getColor() == Color.W) {
                return lastMove.getPiece() == Piece.BLACK_PAWN
                        && from.getY() == lastMove.getTo().getY()
                        && lastMove.getFrom().getY() == 6
                        && to.getX() == lastMove.getTo().getX();
            } else {
                return lastMove.getPiece() == Piece.WHITE_PAWN
                        && from.getY() == lastMove.getTo().getY()
                        && lastMove.getFrom().getY() == 1
                        && to.getX() == lastMove.getTo().getX();
            }
        }
        return false;
    }

    public boolean isLeftCastling() {
        return (piece == Piece.WHITE_KING
                && from.equals(King.W_START_CASTLING)
                && to.equals(King.W_START_CASTLING.decrementX(2))) ||
                (piece == Piece.BLACK_KING
                && from.equals(King.B_START_CASTLING)
                && to.equals(King.B_START_CASTLING.decrementX(2)));
    }

    public boolean isRightCastling() {
        return (piece == Piece.WHITE_KING 
                && from.equals(King.W_START_CASTLING) 
                && to.equals(King.W_START_CASTLING.incrementX(2))) ||
                (piece == Piece.BLACK_KING 
                && from.equals(King.B_START_CASTLING) 
                && to.equals(King.B_START_CASTLING.incrementX(2)));
    }

    public Type getType() {
        return enPassant ? Type.EN_PASSANT
                : promoted ? Type.PROMOTION
                : isLeftCastling() ? Type.LEFT_CASTLING
                : isRightCastling() ? Type.RIGHT_CASTLING
                : Type.NORMAL;
    }

    public String toNotation() {
        return from.toNotation() + to.toNotation();     // TODO: capture/promotion?
    }
}
