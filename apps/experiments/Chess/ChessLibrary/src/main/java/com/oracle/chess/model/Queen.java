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

import java.util.List;

/**
 * Queen class.
 *
 */
public final class Queen extends Piece {

    public Queen(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(int x1, int y1, int x2, int y2) {
        if (!super.isValidMove(x1, y1, x2, y2)) {
            return false;
        }
        return (x1 == x2 && y1 != y2)
                || (x1 != x2 && y1 == y2)
                || Math.abs(x2 - x1) == Math.abs(y2 - y1);
    }

    @Override
    public String toNotation() {
        return "Q";
    }

    @Override
    public List<Point> generatePath(Point from, Point to) throws GameException {
        if (!isValidMove(from, to)) {
            throw new GameException(this, from, to);
        }
        return (from.getX() != to.getX() && from.getY() != to.getY())
                ? color.getBishop().generatePath(from, to) : color.getRook().generatePath(from, to);
    }

    @Override
    public List<Point> generateMoves(Point from, Board board) {
        final List<Point> moves = color.getBishop().generateMoves(from, board);
        moves.addAll(color.getRook().generateMoves(from, board));
        return moves;
    }
}
