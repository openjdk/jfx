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

package com.javafx.experiments.dukepad.chess.client;

import com.javafx.experiments.dukepad.chess.ChessUI;
import com.javafx.experiments.dukepad.chess.client3d.ChessBoard;
import com.javafx.experiments.dukepad.chess.client3d.ChessBoard.CellClickedEventListener;
import com.oracle.chess.model.Color;
import com.oracle.chess.model.Move;
import com.oracle.chess.model.Point;
import com.oracle.chess.protocol.*;
import javafx.application.Platform;
import javafx.util.Callback;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Exchanger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChessClient implements Runnable {

    private ChessBoard chessBoard;

    private String url;

    private String gameId;

    private Color color = Color.W;

    private boolean debug = Boolean.getBoolean("ChessClient.debug");

    private Exchanger<String> exchanger = new Exchanger<>();
    private ClientManager client;
    private RandomChessClient opponentChessClient;
    private boolean runRandom = true;
    private ChessClientEndpoint chessClientEndpoint;

    public ChessClient(String url, Color color, boolean runRandom, ChessBoard chessBoard) {
        this.color = color;
        this.chessBoard = chessBoard;
        this.runRandom = runRandom;
        this.url = url;
    }

    public static void main(String[] args) {
        checkUsage(args);
        new ChessClient(args).run();
    }

    private static void checkUsage(String[] args) {
        if (args.length < 1 || args.length > 3 || args[0].equals("--debug") && args.length < 2) {
            System.err.println("java " + ChessClient.class.getName() + " [--debug] <url> [<gameId>]");
            System.exit(1);
        }
    }

    public ChessClient(String[] args) {
        int n = 2;
        if (args[0].equals("--debug")) {
            debug = true;
            url = args[1];
            n++;
        } else {
            url = args[0];
        }
        if (args.length == n) {
            gameId = args[n - 1];
            color = Color.B;
        }
    }

    public ChessClient(String url, String gameId, ChessBoard chessBoard) {
        this.url = url;
        this.gameId = gameId;
        this.chessBoard = chessBoard;
    }

    @Override
    public void run() {
        try {
            client = ClientManager.createClient();
            chessClientEndpoint = new ChessClientEndpoint();
            if (chessBoard != null) {
                chessBoard.setListener(chessClientEndpoint);
            }
            client.connectToServer(chessClientEndpoint, new URI(url));
            exchanger.exchange("Done?");
        } catch (URISyntaxException | InterruptedException | DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void runRandom() {
        opponentChessClient = new RandomChessClient(ChessUI.URL, gameId);
        opponentChessClient.startThread();
    }

    public void startThread() {
        Thread websocketThread = new Thread(this, "ChessClient Websocket communication");
        websocketThread.setContextClassLoader(this.getClass().getClassLoader());
        websocketThread.setDaemon(true);
        websocketThread.start();
    }

    public void close() {
        chessBoard.unhighlightMoves();
        client.getExecutorService().shutdown();
        if (opponentChessClient != null) {
            opponentChessClient.close();
        }
    }

    public void queryGames(final Callback<QueryGamesRsp, Void> callback) {
        client.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                chessClientEndpoint.queryGamesCallback = callback;
                QueryGames queryGames = new QueryGames();
                chessClientEndpoint.sendMessage(queryGames);
            }
        });
    }

    private static enum ClientState {
        START, ENTER_GAME, IN_GAME
    };

    private static enum ListenerState {
        NONE, SOURCE, TARGET
    };

    @ClientEndpoint(
            encoders = {MessageEncoder.class},
            decoders = {MessageDecoder.class})
    public class ChessClientEndpoint implements CellClickedEventListener, ClientMessageProcessor {

        private ClientState state = ClientState.START;
        private ListenerState listenerState = ListenerState.NONE;
        private Session session;
        private SendMove sendMove;
        private Point fromPoint, toPoint;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
            if (gameId != null) {
                joinGame(gameId);
            } else {
                createGame(color);
            }
        }

        public void joinGame(String gameId) {
            color = Color.B;
            System.out.println("Attempting to join game " + gameId + " ...");
            JoinGame jg = new JoinGame(gameId);
            jg.setColor(color);
            sendMessage(jg);
            state = ClientState.ENTER_GAME;
        }

        public void createGame(Color color) {
            System.out.println("Attempting to create new game ...");
            CreateGame cg = new CreateGame();
            cg.setColor(color);
            sendMessage(cg);
            state = ClientState.ENTER_GAME;
        }

        @OnMessage
        public void onMessage(Message message, Session session) {
            if (debug) {
                System.out.println("RECEIVED: " + message);
            }
            this.session = session;
            MessageRsp rsp = (MessageRsp) message;

            if (rsp.hasError()) {
                System.err.println(rsp.getError().getMessage());
            }
            if (rsp.hasAlert()) {
                processAlert(rsp);
            }
            rsp.processMe(this);
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) throws IOException {
            System.out.println("Connection closed " + reason);
            try {
                exchanger.exchange("I'm done");
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @OnError
        public void onError(Throwable t) {
            new Exception("Error received from server", t).printStackTrace(System.err);
        }

//        private void printBoard(BoardRep boardRep) {
//            Board board = new Board();
//            board.clear();
//            for (String wp : boardRep.getWhites()) {
//                final String notation = wp.substring(0, 1);
//                final Point point = Point.fromNotation(wp.substring(1));
//                board.setPiece(Piece.fromNotation(Color.W, notation), point);
//            }
//            for (String bp : boardRep.getBlacks()) {
//                final String notation = bp.substring(0, 1);
//                final Point point = Point.fromNotation(bp.substring(1));
//                board.setPiece(Piece.fromNotation(Color.B, notation), point);
//            }
//
//            System.out.println("\n  +----+----+----+----+----+----+----+----+");
//            for (int y = N_SQUARES - 1; y >= 0; y--) {
//                System.out.print((y + 1) + " |");
//                for (int x = 0; x < N_SQUARES; x++) {
//                    Square sq = board.getSquare(Point.fromXY(x, y));
//                    System.out.print(" ");
//                    System.out.print(sq.getPiece() != null ? sq.getPiece() : "  ");
//                    System.out.print(" |");
//                }
//                System.out.println("\n  +----+----+----+----+----+----+----+----+");
//            }
//            System.out.println("    a    b    c    d    e    f    g    h  \n");
//        }

        @Override
        public void cellClicked(int x, int y) {
            Point point = Point.fromXY(x, y);
            String nt = point.toNotation();
            System.out.println(listenerState + " nt = " + nt);
            switch (listenerState) {
                case SOURCE:
                    if (!chessBoard.isThereAPiece(x, y)) {
                        break;
                    }
                    fromPoint = point;
                    sendMove.setFrom(nt);
                    listenerState = ListenerState.TARGET;
                    chessBoard.unhighlightMoves();

                    QueryMoves qm = new QueryMoves();
                    qm.setColor(color);
                    qm.setFrom(fromPoint.toNotation());
                    qm.setGameId(gameId);
                    sendMessage(qm);
                    break;
                case TARGET:
                    if (x == fromPoint.getX() && y == fromPoint.getY()) {
                        startMove();
                        break;
                    }
                    toPoint = point;
                    sendMove.setTo(nt);
                    chessBoard.unhighlightMoves();
                    sendMessage(sendMove);
                    listenerState = ListenerState.NONE;
                    break;
            }
        }

        private void startMove(MessageRsp rsp) {
            if (rsp.getTurn() == color) {
                startMove();
            }
        }

        private void update3DBoard(final Color color, final BoardRep board) {
            Platform.runLater(new Runnable() {
                @Override public void run() {
                    chessBoard.rotate180(color == Color.B);
                    chessBoard.update(board);
                }
            });
        }

        private void animateMove(final Point from, final Point to) {
            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    chessBoard.animate(from, to);
                }
            });
        }

        private void animateMove(final Point from, final Point to, final Point capture) {
            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    chessBoard.animate(from, to, capture);
                }
            });
        }

        void sendMessage(Object msg) {
            if (debug) {
                System.out.println("SENT: " + msg);
            }
            try {
                session.getBasicRemote().sendObject(msg);
            } catch (IOException | EncodeException ex) {
                Logger.getLogger(ChessClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void process(CreateGameRsp rsp) {
            if (state == ClientState.ENTER_GAME && !rsp.hasError()) {
                gameId = rsp.getGameId();
                System.out.println("Game created with ID " + gameId);
                if (runRandom) {
                    runRandom();
                }
                state = ClientState.IN_GAME;
                update3DBoard(color, rsp.getBoard());
                startMove(rsp);
            } else {
                throw new InternalError("Attempting to process CreateGameRsp in state " + state);
            }

        }

        @Override
        public void process(JoinGameRsp rsp) {
            if (state == ClientState.ENTER_GAME && !rsp.hasError()) {
                System.out.println("Joined gamed with ID " + rsp.getGameId());
                color = rsp.getColor();
                state = ClientState.IN_GAME;
                update3DBoard(color, rsp.getBoard());
                startMove(rsp);
            } else {
                throw new InternalError("Attempting to process JoinGameRsp in state " + state);
            }
        }

        @Override
        public void process(SendMoveRsp message) {
            if (state == ClientState.IN_GAME) {
                if (!message.hasError()) {
                    playMove(fromPoint, toPoint, message.getMoveType());
                    startMove(message);
                } else {
                    // restart move
                    startMove();
                }
            } else {
                throw new InternalError("Attempting to process SendMoveRsp in state " + state);
            }
        }

        private void playMove(Point from, Point to, Move.Type moveType) {
            switch (moveType) {
                case LEFT_CASTLING:
                    animateMove(from, to);
                    animateMove(Point.fromXY(0, from.getY()), Point.fromXY(to.getX() + 1, from.getY()));
                    break;
                case RIGHT_CASTLING:
                    animateMove(from, to);
                    animateMove(Point.fromXY(7, from.getY()), Point.fromXY(to.getX() - 1, from.getY()));
                    break;
                case EN_PASSANT:
                    animateMove(from, to, Point.fromXY(to.getX(), from.getY()));
                    break;
                case PROMOTION:
                case NORMAL:
                    animateMove(from, to);
                    break;
            }
        }

        @Override
        public void process(final QueryMovesRsp queryMovesRsp) {
            if (!queryMovesRsp.hasError()) {
                queryMovesRsp.getMoves().add(fromPoint.toNotation());
                Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    chessBoard.highlightMoves(queryMovesRsp.getMoves());
                }
            });
            } else {
                startMove();
            }
        }

        @Override
        public void process(UpdateGame rsp) {
            if (state == ClientState.IN_GAME && !rsp.hasError()) {
                if (rsp.hasAlert()) {
                    processAlert(rsp);
                }
                playMove(Point.fromNotation(rsp.getFrom()), Point.fromNotation(rsp.getTo()), rsp.getMoveType());
                System.out.println("Next turn: " + rsp.getTurn());
//                update3DBoard(rsp.getBoard());
                state = ClientState.IN_GAME;
                startMove(rsp);
            } else {
                throw new InternalError("Attempting to process UpdateGame in state " + state);
            }
        }

        private void processAlert(MessageRsp message) {
            try {
                if (message.hasAlert() && !message.hasError()) {
                    System.err.println(message.getAlert().getType());
                    switch (message.getAlert().getType()) {
                        case CHECKMATE:
                            System.out.println(message.getAlert().getMessage());
                            session.close();    // game over!
                            break;
                        case CHECK:
                            break;      // TODO
                        case DRAW:
                            break;      // TODO
                        }
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }

        private Callback<QueryGamesRsp, Void> queryGamesCallback;

        @Override
        public void process(QueryGamesRsp message) {
            if (queryGamesCallback != null) {
                queryGamesCallback.call(message);
                queryGamesCallback = null;
            }
        }

        @Override
        public void process(QueryGameRsp message) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void process(SendAction message) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void process(SendActionRsp message) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void process(CheckCredentialsRsp message) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void startMove() {
            sendMove = new SendMove(gameId);
            sendMove.setColor(color);
            listenerState = ListenerState.SOURCE;
            Platform.runLater(new Runnable() {
                @Override public void run() {
                    chessBoard.highlightMoves(color);
                }
            });
        }
    }
}
