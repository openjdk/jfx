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

import com.oracle.chess.model.Color;

/**
 * CreateGame class.
 *
 */
public class CreateGame extends Message {

    protected String summary;

    private BoardRep board;

    private Color turn;

    private boolean persisted;

    public CreateGame() {
    }
    
    public CreateGame(String gameId, Color color) {
        this(gameId, color, null);
    }

    public CreateGame(String gameId, Color color, String summary) {
        super(gameId);
        this.color = color;
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BoardRep getBoard() {
        return board;
    }

    public void setBoard(BoardRep board) {
        this.board = board;
    }

    public boolean hasBoard() {
        return board != null;
    }

    public Color getTurn() {
        return turn;
    }

    public void setTurn(Color turn) {
        this.turn = turn;
    }

    public boolean hasTurn() {
        return turn != null;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public Message processMe(ServerMessageProcessor processor) {
        return processor.process(this);
    }

    @Override
    public CreateGameRsp newResponse() {
        return new CreateGameRsp(gameId, color);
    }
}
