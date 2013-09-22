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
 * IllegalMoveException class.
 *
 */
public class GameException extends Exception {

    // Error codes must be between 0 and 999
    public static enum ErrorCode {
        NOT_YOUR_TURN(100),
        NO_PIECE_AT_LOCATION(200),
        NOT_YOUR_PIECE(300),
        ILLEGAL_MOVE(400),
        ILLEGAL_MOVE_KING_CHECK(500),
        GAME_OVER(600);
        
        int code;

        ErrorCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
     };

    private ErrorCode code;

    private Piece piece;

    private Point from;

    private Point to;

    public GameException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public GameException(Piece piece, Point from, Point to) {
        this.piece = piece;
        this.from = from;
        this.to = to;
    }

    public ErrorCode getErrorCode() {
        return code;
    }

    public Piece getPiece() {
        return piece;
    }

    public Point getFrom() {
        return from;
    }

    public Point getTo() {
        return to;
    }

}
