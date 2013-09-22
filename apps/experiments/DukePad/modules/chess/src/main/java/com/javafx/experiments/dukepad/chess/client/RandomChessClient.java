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


import com.oracle.chess.model.Color;
import com.oracle.chess.protocol.*;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChessClient class.
 *
 */
public class RandomChessClient implements Runnable {

    private String url;

    private String gameId;

    private Color color = Color.W;

    private boolean debug = Boolean.getBoolean("ChessClient.debug");

    private Exchanger<String> exchanger = new Exchanger<>();
    private ClientManager client;

    public static void main(String[] args) {
        checkUsage(args);
        new RandomChessClient(args).run();
    }

    private static void checkUsage(String[] args) {
        if (args.length < 1 || args.length > 3 || args[0].equals("--debug") && args.length < 2) {
            System.err.println("java " + RandomChessClient.class.getName() + " [--debug] <url> [<gameId>]");
            System.exit(1);
        }
    }

    public RandomChessClient(String[] args) {
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

    public RandomChessClient(String url, String gameId) {
        this.url = url;
        this.gameId = gameId;
    }

    @Override
    public void run() {
        try {
            client = ClientManager.createClient();
            ChessClientEndpoint chessClientEndpoint = new ChessClientEndpoint();
            client.connectToServer(chessClientEndpoint, new URI(url));
            exchanger.exchange("Done?");
        } catch (URISyntaxException | InterruptedException | DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close() {
        client.getExecutorService().shutdown();
    }

    public void startThread() {
        Thread websocketThread = new Thread(this, "Opponent Websocket communication");
        websocketThread.setDaemon(true);
        websocketThread.start();
    }

    private static enum ClientState {
        START, ENTER_GAME, IN_GAME, FINISHED
    };

    @ClientEndpoint(
            encoders = {MessageEncoder.class},
            decoders = {MessageDecoder.class})
    public class ChessClientEndpoint {

        private ClientState state = ClientState.START;
        private Session session;
        private SendMove sendMove;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        private List<String> blacks;
        private List<String> whites;

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
            if (gameId != null) {
                System.out.println("OPPONENT Attempting to join game " + gameId + " ...");
                JoinGame jg = new JoinGame(gameId);
                sendMessage(jg);
            } else {
                System.out.println("OPPONENT Attempting to create new game ...");
                CreateGame cg = new CreateGame();
                cg.setColor(color);
                sendMessage(cg);
            }
            state = ClientState.ENTER_GAME;
        }

        @OnMessage
        public void onMessage(Message message, Session session) {
            if (debug) {
                System.out.println("OPPONENT RECEIVED: " + message);
            }
            try {
                MessageRsp rsp = (MessageRsp) message;

                if (rsp.hasAlert()) {
                    switch (rsp.getAlert().getType()) {
                        case CHECKMATE:
                        case DRAW:
                            session.close();
                            state = ClientState.FINISHED;
                            break;
                    }
                }
                if (rsp.hasError()) {
                    throw new RuntimeException(rsp.getError().getMessage());
                }

                switch (state) {
                    case START:
                        break;
                    case ENTER_GAME:
                        if (rsp instanceof CreateGameRsp) {
                            gameId = rsp.getGameId();
                            System.out.println("OPPONENT Game created with ID " + gameId);
                        } else if (message instanceof JoinGameRsp) {
                            System.out.println("OPPONENT Joined gamed with ID " + rsp.getGameId());
                            color = rsp.getColor();
                        } else {
                            throw new InternalError("OPPONENT Received unexpected message "
                                    + message.getClass().getSimpleName());
                        }
                        System.out.println("OPPONENT Next turn: " + rsp.getTurn());
                        updateMoves(rsp.getBoard());
                        state = ClientState.IN_GAME;
                        startMove(rsp);
                        break;
                    case IN_GAME:
                        if (rsp.hasError()) {
                            System.out.println(rsp.getError().getMessage());
                        } else if (rsp instanceof QueryMovesRsp) {
                            onQueryMovesRsp((QueryMovesRsp) rsp);
                            break;
                        } else {
                            updateMoves(rsp.getBoard());
                        }
                        System.out.println("OPPONENT Next turn: " + rsp.getTurn());
                        startMove(rsp);
                        break;
                }
            } catch (InternalError | IOException | EncodeException e) {
                e.printStackTrace(System.err);
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) throws IOException {
            System.out.println("OPPONENT Connection closed " + reason);
            try {
                exchanger.exchange("I'm done");
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @OnError
        public void onError(Throwable t) {
            new Exception("OPPONENT Error received from server", t).printStackTrace(System.err);
        }

        private void startMove(MessageRsp rsp) throws EncodeException, IOException {
            if (rsp.getTurn() == color) {
                startMove();
            }
        }

        private void onQueryMovesRsp(final QueryMovesRsp queryMovesRsp) throws EncodeException, IOException {
            List<String> moves = queryMovesRsp.getMoves();
            if (moves.isEmpty()) {
                startMove();
            } else {
                String randomMove = moves.get((int) (Math.random() * moves.size()));
                sendMove.setTo(randomMove);
                sendMessage(sendMove);
            }
        }

        private void sendMessage(Object msg) {
            if (debug) {
                System.out.println("OPPONENT SENT: " + msg);
            }
            try {
                session.getBasicRemote().sendObject(msg);
            } catch (IOException | EncodeException ex) {
                Logger.getLogger(RandomChessClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void updateMoves(BoardRep board) {
            blacks = board.getBlacks();
            whites = board.getWhites();
        }

        private void startMove() {
            List<String> moves = (color == Color.B) ? blacks : whites;
            String randomMove = moves.get((int) (Math.random() * moves.size()));
            String from = randomMove.substring(1);

            sendMove = new SendMove(gameId);
            sendMove.setColor(color);
            sendMove.setFrom(from);

            QueryMoves queryMoves = new QueryMoves();
            queryMoves.setGameId(gameId);
            queryMoves.setColor(color);
            queryMoves.setFrom(from);
            sendMessage(queryMoves);
        }
    }
}
