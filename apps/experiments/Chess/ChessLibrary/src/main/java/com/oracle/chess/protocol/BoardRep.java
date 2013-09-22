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

package com.oracle.chess.protocol;

import java.util.List;

import com.oracle.chess.model.Board;
import com.oracle.chess.model.Color;
import com.oracle.chess.model.Piece;
import com.oracle.chess.model.Point;
import com.oracle.chess.model.Square;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * BoardRep class.
 *
 */
public class BoardRep {

    private List<String> whites;

    private List<String> blacks;

    public BoardRep() {
    }

    public BoardRep(Board board) {
        whites = new ArrayList<>();
        final Iterator<Square> wi = board.getIterator(Color.W);
        while (wi.hasNext()) {
            whites.add(wi.next().toNotation());
        }
        blacks = new ArrayList<>();
        final Iterator<Square> bi = board.getIterator(Color.B);
        while (bi.hasNext()) {
            blacks.add(bi.next().toNotation());
        }
    }

    public List<String> getWhites() {
        return whites;
    }

    public void setWhites(List<String> whites) {
        this.whites = whites;
    }

    public List<String> getBlacks() {
        return blacks;
    }

    public void setBlacks(List<String> blacks) {
        this.blacks = blacks;
    }

    public Board toBoard() {
        final Board board = new Board();
        board.clear();
        if (whites != null) {
            for (String w : whites) {
                board.setPiece(Piece.fromNotation(Color.W, w.substring(0, 1)),
                               Point.fromNotation(w.substring(1)));
            }
        }
        if (blacks != null) {
            for (String b : blacks) {
                board.setPiece(Piece.fromNotation(Color.B, b.substring(0, 1)),
                               Point.fromNotation(b.substring(1)));
            }
        }
        return board;
    }

}
