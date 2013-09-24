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
 * MessageRsp class.
 *
 */
public abstract class MessageRsp extends Message {

    public static class Error {

        private int code;
        private String message;

        public Error() {
        }

        public Error(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    };

    public static enum AlertType {
        CHECKMATE, CHECK, DRAW
    };

    public static class Alert {
        
        private AlertType type;
        private String message;

        public Alert() {
        }

        public Alert(AlertType type, String message) {
            this.type = type;
            this.message = message;
        }

        public AlertType getType() {
            return type;
        }

        public void setType(AlertType type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    private Error error;

    private Alert alert;

    private BoardRep board;

    private Color turn;

    public MessageRsp() {
    }

    public MessageRsp(String gameId) {
        super(gameId);
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null;
    }

    public BoardRep getBoard() {
        return board;
    }

    public void setBoard(BoardRep board) {
        this.board = board;
    }

    public Color getTurn() {
        return turn;
    }

    public void setTurn(Color turn) {
        this.turn = turn;
    }

    public boolean hasAlert() {
        return alert != null;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    @Override
    public Message processMe(ServerMessageProcessor processor) {
        throw new InternalError("Not implemented");
    }

    public abstract void processMe(ClientMessageProcessor processor);

}
