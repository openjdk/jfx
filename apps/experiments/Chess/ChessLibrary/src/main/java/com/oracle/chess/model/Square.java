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
 * Square class.
 *
 */
public class Square {

    /**
     * Point or coordinate of this square in the board.
     */
    private final Point point;

    /**
     * Piece sitting on this square or <code>null</code> if square
     * is empty.
     */
    private Piece piece;

    public Square(int x, int y) {
        this(x, y, null);
    }
    
    public Square(int x, int y, Piece piece) {
        point = new Point(x, y);
        this.piece = piece;
    }

    public Square(Point point) {
        this(point, null);
    }

    public Square(Point point, Piece piece) {
        this.point = point;
        this.piece = piece;
    }

    /**
     * Returns the point of this square on the board.
     *
     * @return Point or coordinate for this square.
     */
    public Point getPoint() {
        return point;
    }

    /**
     * Returns the piece sitting on this square or <code>null<code>
     * if the square is empty.
     *
     * @return Piece on square or <code>null</code>.
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Sets a new piece on this square.
     *
     * @param piece New piece.
     */
    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    /**
     * Determines if a square is empty or not.
     *
     * @return Value <code>true</code> if piece on square, <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return piece == null;
    }

    /**
     * Returns the color for this square on the board.
     *
     * @return Color for this square.
     */
    public Color getColor() {
        return (point.getX() + point.getY()) % 2 == 0 ? Color.B : Color.W;
    }

    /**
     * Returns representation in algebraic notation. Letter for piece followed
     * by coordinate. For example, Ra1 for rook on a1 (0, 0). If no piece in
     * square, returns a strings with spaces.
     *
     * @return Notation representation.
     */
    public String toNotation() {
        if (piece == null) {
            return "   ";
        }
        return piece.toNotation() + point.toNotation();
    }

    /**
     * String representation for this square.
     *
     * @return String representation.
     */
    @Override
    public String toString() {
        return toNotation();
    }
}
