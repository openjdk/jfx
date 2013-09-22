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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

import static com.oracle.chess.model.GameException.ErrorCode.*;

/**
 * Game class.
 *
 * @param <P> Type of a game player.
 * @param <O> Type of a game observer.
 */
public final class Game<P, O> {

    public enum State {
        PLAYING("Game being played"),
        DRAW("Game ended by draw"),
        WHITE_WINS("White player wins"),
        BLACK_WINS("Black player wins");
        
        private String msg;
        
        State(String msg) {
            this.msg = msg;
        }
        
        @Override
        public String toString() {
            return msg;
        }
    };

    private Board board;

    private Color turn;

    private Color startTurn;

    private String summary;

    private String gameId;

    private P whitePlayer;

    private P blackPlayer;

    private P drawRequester;

    private List<O> observers = new ArrayList<>();

    private State state = State.PLAYING;

    private GameWatcher<P, O> watcher;

    private long creationStamp;

    private long updateStamp;

    public Game() {
        this(new Board(), Color.W);
    }

    public Game(Color turn, String summary) {
        this(new Board(), turn, summary);
    }

    public Game(Board board, Color turn) {
        this(board, turn, null);
    }

    public Game(Board board, Color turn, String summary) {
        this.board = board != null ? board : new Board();
        this.turn = this.startTurn = turn;
        this.summary = summary;
        creationStamp = updateStamp = System.currentTimeMillis();
        generateGameId();
    }

    /**
     * Generates a unique ID for this game.
     */
    public void generateGameId() {
        gameId = UUID.randomUUID().toString();
    }

    /**
     * Gets the color that started the game. This is used when an initial
     * board is specified (mostly for testing).
     *
     * @return Start color.
     */
    public synchronized Color getStartTurn() {
        return startTurn;
    }

    /**
     * Sets the start color for the game. This is used when an initial board
     * is specified (mostly for testing).
     *
     * @param startTurn Start color.
     */
    public synchronized void setStartTurn(Color startTurn) {
        if (watcher != null) {
            watcher.setStartTurn(this, startTurn);
        }
        this.startTurn = startTurn;
    }

    /**
     * Sets a player for a color.
     *
     * @param color The color.
     * @param player The player.
     */
    public synchronized void setPlayer(Color color, P player) {
        if (watcher != null) {
            watcher.setPlayer(this, color, player);
        }
        if (color == Color.W) {
            whitePlayer = player;
        } else {
            blackPlayer = player;
        }
    }

    /**
     * Gets a player of a certain color.
     *
     * @param color The color.
     * @return The player of that color or <code>null</code> if no player exists.
     */
    public synchronized P getPlayer(Color color) {
        return color == Color.W ? whitePlayer : blackPlayer;
    }

    /**
     * Determines if there is a player of a certain color.
     *
     * @param color The color.
     * @return Outcome of test.
     */
    public synchronized boolean hasPlayer(Color color) {
        return color == Color.W ? whitePlayer != null : blackPlayer != null;
    }

    /**
     * Returns the color of a player.
     *
     * @param player The player.
     * @return Color of player or <code>null</code> if player is unknown.
     */
    public synchronized Color getPlayerColor(P player) {
        return player.equals(whitePlayer) ? Color.W : player.equals(blackPlayer) ? Color.B : null;
    }

    /**
     * Adds an observer to this game.
     *
     * @param observer The observer.
     */
    public synchronized void addObserver(O observer) {
        observers.add(observer);
    }

    /**
     * Determines if observer is in the game.
     *
     * @param observer The observer.
     */
    public synchronized void hasObserver(O observer) {
        observers.contains(observer);
    }

    /**
     * Removes an observer from a game.
     *
     * @param observer The observer.
     * @return Boolean indicating if observer was found and removed.
     */
    public synchronized boolean removeObserver(O observer) {
        return observers.remove(observer);
    }

    /**
     * Gets a list of current observers.
     *
     * @return List of observers.
     */
    public synchronized List<O> getObservers() {
        return observers;
    }

    /**
     * Returns the opponent of a given player.
     *
     * @param player The player.
     * @return Opponent or <code>null</code> if it doesn't exist.
     */
    public synchronized P getOpponent(P player) {
        if (player != null) {
            if (player.equals(whitePlayer)) {
                return blackPlayer;
            } else if (player.equals(blackPlayer)) {
                return whitePlayer;
            }
        }
        return null;
    }

    /**
     * Determines if player has an opponent.
     *
     * @param player The player.
     * @return Outcome of test.
     */
    public synchronized boolean hasOpponent(P player) {
        return getOpponent(player) != null;
    }

    /**
     * Determines if player has an opponent, using color.
     *
     * @param color Color of player.
     * @return Outcome of test.
     */
    public synchronized boolean hasOpponent(Color color) {
        return getPlayer(color.getOpponentColor()) != null;
    }

    /**
     * Returns opponent of a given player, using color.
     *
     * @param color Color of player.
     * @return Outcome of test.
     */
    public synchronized P getOpponent(Color color) {
        return getPlayer(color.getOpponentColor());
    }

    /**
     * Returns the next turn.
     *
     * @return Next turn.
     */
    public synchronized Color getTurn() {
        return turn;
    }

    /**
     * Sets the next turn.
     *
     * @param turn Next turn.
     */
    public synchronized void setTurn(Color turn) {
        this.turn = turn;
    }

    /**
     * Returns complete list of moves in game.
     *
     * @return List of moves.
     */
    public synchronized List<Move> getMoves() {
        return board.getMoves();
    }

    /**
     * Adds a move to the end of the list.
     *
     * @param move The move.
     */
    public synchronized void addMove(Move move) {
        if (watcher != null) {
            watcher.addMove(this, move);
        }
        board.getMoves().add(move);
    }

    /**
     * Gets summary for this game.
     *
     * @return The summary.
     */
    public synchronized String getSummary() {
        return summary;
    }

    /**
     * Sets a summary for this game.
     *
     * @param summary The summary.
     */
    public synchronized void setSummary(String summary) {
        if (watcher != null) {
            watcher.setSummary(this, summary);
        }
        this.summary = summary;
    }

    /**
     * Gets the underlying board.
     *
     * @return The board.
     */
    public synchronized Board getBoard() {
        return board;
    }

    /**
     * Sets the underlying board for this game.
     *
     * @param board The board.
     */
    public synchronized void setBoard(Board board) {
        this.board = board;
    }

    /**
     * Returns game ID for this game.
     *
     * @return Game ID.
     */
    public synchronized String getGameId() {
        return gameId;
    }

    /**
     * Sets game ID for this game.
     *
     * @param gameId Game ID.
     */
    public synchronized void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /**
     * Determines if the game is open. I.e., if there is less than
     * two players.
     *
     * @return Outcome of test.
     */
    public synchronized boolean isOpen() {
        return whitePlayer == null || blackPlayer == null;
    }

    /**
     * Returns the internal state of the game.
     *
     * @return The state.
     */
    public synchronized State getState() {
        return state;
    }

    /**
     * Sets the internal state for this game.
     *
     * @param state The state.
     */
    public synchronized void setState(State state) {
        if (watcher != null) {
            watcher.setState(this, state);
        }
        this.state = state;
    }

    /**
     * Sets the winner for the game.
     *
     * @param color The player's color.
     */
    public synchronized void setWinner(Color color) {
        setState(color == Color.W ? State.WHITE_WINS : State.BLACK_WINS);
    }

    /**
     * Returns this game's watcher.
     *
     * @return Watcher or <code>null</code> if no watcher set.
     */
    public GameWatcher<P, O> getWatcher() {
        return watcher;
    }

    /**
     * Get creation timestamp.
     *
     * @return Game creation timestamp.
     */
    public long getCreationStamp() {
        return creationStamp;
    }

    /**
     * Sets creation timestamp.
     *
     * @param creationStamp Creation timestamp.
     */
    public void setCreationStamp(long creationStamp) {
        this.creationStamp = creationStamp;
    }

    /**
     * Get update timestamp. This timestamp is updated every time a {@link #makeMove}
     * is called.
     *
     * @return Game update timestamp.
     */
    public long getUpdateStamp() {
        return updateStamp;
    }

    /**
     * Sets a watcher for this game.
     *
     * @param watcher A watcher for this game.
     */
    public void setWatcher(GameWatcher<P, O> watcher) {
        this.watcher = watcher;
    }

    /**
     * Updates the internal state of the game by making a piece move. Allows
     * <code>from</code> to be either a column or null.
     *
     * @param piece Piece to move.
     * @param from Initial location in notation format.
     * @param to Final location in notation format.
     * @return The move.
     * @throws GameException If an error is found while trying to move the piece.
     */
    public synchronized Move makeMove(Piece piece, String from, String to) throws GameException {
        Point pointFrom = null;
        final Point pointTo = Point.fromNotation(to);

        // If not from or only column in from, compute from
        if (from == null || from.length() == 1) {
            int x = -1, y = -1;
            if (from != null && from.length() == 1) {
                final char ch = from.charAt(0);
                if (Character.isDigit(ch)) {
                    y = (ch - '1');
                } else if (Character.isLetter(ch)) {
                    x = (ch - 'a');
                } else {
                    throw new GameException(ILLEGAL_MOVE, "Not a valid chess move!");
                }
            }

            final Iterator<Square> it = board.getIterator(piece.getColor());
            while (it.hasNext()) {
                final Square square = it.next();
                if (square.getPiece() == piece && piece.isLegalMove(square.getPoint(), pointTo, board)
                        && (x == -1 || square.getPoint().getX() == x)
                        && (y == -1 || square.getPoint().getY() == y)) {
                    pointFrom = square.getPoint();
                    break;
                }
            }
        } else {
            pointFrom = Point.fromNotation(from);
        }
        if (pointFrom == null) {
            throw new GameException(ILLEGAL_MOVE, "Not a valid chess move!");
        }
        return makeMove(piece.getColor(), pointFrom, pointTo);
    }

    /**
     * Updates the internal state of the game by making a piece move.
     *
     * @param color Color of piece to move.
     * @param from Initial location in notation format.
     * @param to Final location in notation format.
     * @return The move.
     * @throws GameException If an error is found while trying to move the piece.
     */
    public synchronized Move makeMove(Color color, String from, String to) throws GameException {
        return makeMove(color, Point.fromNotation(from), Point.fromNotation(to));
    }

    /**
     * Updates the internal state of the game by making a piece move.
     *
     * @param color Color of piece to move.
     * @param from Initial location of piece.
     * @param to Final location of piece.
     * @return The move.
     * @throws GameException If an error is found while trying to move the piece.
     */
    public synchronized Move makeMove(Color color, Point from, Point to) throws GameException {
        if (state != State.PLAYING) {
            throw new GameException(GAME_OVER, state.toString());
        }
        if (color != turn) {
            throw new GameException(NOT_YOUR_TURN, "Slow down, it is not your turn to play");
        }
        if (!board.hasPiece(from)) {
            throw new GameException(NO_PIECE_AT_LOCATION, "Get some glasses, there's no piece there");
        }
        final Piece piece = board.getPiece(from);
        if (color != piece.getColor()) {
            throw new GameException(NOT_YOUR_PIECE, "Cheater! That's not your piece");
        }
        if (!piece.isLegalMove(from, to, board)) {
            throw new GameException(ILLEGAL_MOVE, "You need to learn Chess!");
        }

        // Apply move to board
        final Move move = new Move(piece, from, to);
        board.doMove(move);

        // Inform game observer
        if (watcher != null) {
            watcher.addMove(this, move);
        }

        // Is my king in check after this move?
        final Square kingSquare = board.getKingSquare(color);
        if (board.isKingAttacked(color, kingSquare.getPoint())) {
            board.undoLastMove();
            throw new GameException(ILLEGAL_MOVE_KING_CHECK, "Can't leave your king in check!");
        }

        // Switch turns
        turn = turn.getOpponentColor();

        // Update timestamp
        updateStamp = System.currentTimeMillis();
        
        return move;
    }

    /**
     * Returns the list of moves that are legal for a piece of <code>color</code>
     * located at position <code>from</code>.
     *
     * @param color Piece's color.
     * @param from Piece's location in notation format.
     * @return List of moves in algebraic notation format.
     * @throws GameException If no piece at location or of the wrong color.
     */
    public synchronized List<String> queryMoves(Color color, String from) throws GameException {
        return queryMoves(color, Point.fromNotation(from));
    }

    /**
     * Returns the list of moves that are legal for a piece of <code>color</code>
     * located at position <code>from</code>.
     *
     * @param color Piece's color.
     * @param from Piece's location.
     * @return List of moves in algebraic notation format.
     * @throws GameException If no piece at location or of the wrong color.
     */
    public synchronized List<String> queryMoves(Color color, Point from) throws GameException {
        if (!board.hasPiece(from)) {
            throw new GameException(NO_PIECE_AT_LOCATION, "Get some glasses, there's no piece there");
        }
        final Piece piece = board.getPiece(from);
        if (color != piece.getColor()) {
            throw new GameException(NOT_YOUR_PIECE, "Piece at that location of wrong color");
        }

        final List<Point> points = board.queryMoves(from);
        final List<String> result = new ArrayList<>(points.size());
        for (Point to : points) {
            // Filter out moves that leave king in check
            final Move move = new Move(piece, from, to);
            try {
                board.doMove(move);
                final Square kingSquare = board.getKingSquare(color);
                if (!board.isKingAttacked(color, kingSquare.getPoint())) {
                    result.add(to.toNotation());
                }
            } finally {
                board.undoLastMove();
            }
        }
        return result;
    }

    /**
     * Determines if the game is over due to a checkmate.
     *
     * @param color King's color to check.
     * @return Outcome of checkmate test.
     */
    public synchronized boolean isCheckmate(Color color) {
        final Square kingSquare = board.getKingSquare(color);
        final Point kingPoint = kingSquare.getPoint();

        // Is King in check?
        List<Square> attackers = kingAttackers(color, kingPoint);
        if (attackers == null) {
            return false;
        }

        // Is it in check no matter where it moves?
        List<Point> tos = kingSquare.getPiece().generateMoves(kingPoint, board);
        for (Point to : tos) {
            final Move move = new Move(kingSquare.getPiece(), kingPoint, to);
            board.doMove(move);
            if (!board.isKingAttacked(color, to)) {
                board.undoLastMove();
                return false;
            }
            board.undoLastMove();
        }

        try {
            // Is there any other piece that can stop all attackers?
            Iterator<Square> squares = board.getIterator(color);
            while (squares.hasNext()) {
                final Square square = squares.next();
                final Piece piece = square.getPiece();
                if (piece == color.getKing()) {
                    continue;               // skip king!
                }
                List<Point> pieceMoves = piece.generateMoves(square.getPoint(), board);
                int nAttackers = attackers.size();
                for (Square attacker : attackers) {
                    List<Point> path = attacker.getPiece().generatePath(attacker.getPoint(), kingPoint);
                    if (!Collections.disjoint(path, pieceMoves)) {
                        nAttackers--;       // piece that can block attacker
                        continue;
                    }
                    Set<Point> attackerPoint = Collections.singleton(attacker.getPoint());
                    if (!Collections.disjoint(attackerPoint, pieceMoves)) {
                        nAttackers--;       // piece that can capture attacker
                    }
                }
                if (nAttackers == 0) {
                    return false;       // piece can stop all the attackers
                }
            }
        } catch (GameException _) {
            throw new InternalError();
        }

        return true;        // checkmate!
    }

    /**
     * Determines if the king of <code>color</code> is in check or not.
     *
     * @param color King's color.
     * @return Result of in-check test.
     */
    public synchronized boolean isKingAttacked(Color color) {
        return board.isKingAttacked(color);
    }

    /**
     * Returns the list of pieces that are attacking the king of <code>color</code> or
     * <code>null</null> if king is not in check.
     *
     * @param color King's color.
     * @param toKing King's location on the board.
     * @return The list of pieces attacking king or <code>null</code> if not in check.
     */
    public synchronized List<Square> kingAttackers(Color color, Point toKing) {
        return board.kingAttackers(color, toKing);
    }

    /**
     * Determines if a stalemate situation is found for <code>color</code>. Game
     * should end as a draw.
     *
     * @param color Color to check for stalemate situation.
     * @return Outcome of test.
     */
    public synchronized boolean isStalemate(Color color) {
        try {
            Iterator<Square> squares = board.getIterator(color);
            while (squares.hasNext()) {
                final Square square = squares.next();
                if (queryMoves(color, square.getPoint()).size() > 0) {
                    return false;
                }
            }
            return true;
        } catch (GameException _) {
            throw new InternalError();
        }
    }

    /**
     * Determines if a player has requested a draw.
     *
     * @return Outcome of test.
     */
    public synchronized boolean hasDrawRequester() {
        return drawRequester != null;
    }

    /**
     * Gets player that requested a draw or no null if there is no such player.
     *
     * @return Player that requested draw.
     */
    public synchronized P getDrawRequester() {
        return drawRequester;
    }

    /**
     * Sets player that requested a draw.
     *
     * @param color Draw requester's color.
     */
    public synchronized void setDrawRequester(Color color) {
        this.drawRequester = getPlayer(color);
    }

    /**
     * Computes hash code based on game ID.
     *
     * @return Hash code.
     */
    @Override
    public int hashCode() {
        return gameId != null ? gameId.hashCode() : super.hashCode();
    }

    /**
     * Determines equality using game IDs.
     *
     * @param obj Other object.
     * @return Outcome of test.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Game<P, O> other = (Game<P, O>) obj;
        if (!Objects.equals(this.gameId, other.gameId)) {
            return false;
        }
        return true;
    }

    /**
     * Returns string representation for the game. Mostly for debugging purposes.
     *
     * @return String representation for game.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Next turn is ").append(turn).append("\n\n");
        sb.append(board);
        return sb.toString();
    }
}
