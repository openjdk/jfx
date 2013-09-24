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
 * Color enumeration.
 *
 */
public enum Color {

    W {
        @Override
        public King getKing() {
            return Piece.WHITE_KING;
        }

        @Override
        public Queen getQueen() {
            return Piece.WHITE_QUEEN;
        }

        @Override
        public Bishop getBishop() {
            return Piece.WHITE_BISHOP;
        }

        @Override
        public Rook getRook() {
            return Piece.WHITE_ROOK;
        }

        @Override
        public Color getOpponentColor() {
            return B;
        }

        @Override
        public String toString() {
            return "W";
        }
    },
    B {
        @Override
        public King getKing() {
            return Piece.BLACK_KING;
        }
        
        @Override
        public Queen getQueen() {
            return Piece.BLACK_QUEEN;
        }

        @Override
        public Bishop getBishop() {
            return Piece.BLACK_BISHOP;
        }

        @Override
        public Rook getRook() {
            return Piece.BLACK_ROOK;
        }

        @Override
        public Color getOpponentColor() {
            return W;
        }

        @Override
        public String toString() {
            return "B";
        }        
    };

    public abstract King getKing();
    
    public abstract Queen getQueen();

    public abstract Bishop getBishop();

    public abstract Rook getRook();

    public abstract Color getOpponentColor();
};
