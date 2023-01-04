/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.media.jfxmedia.events;

/**
 * An event indicating a change in the state of a media player.
 */
public class PlayerStateEvent extends PlayerEvent {

    public enum PlayerState {

        UNKNOWN, READY, PLAYING, PAUSED, STOPPED, STALLED, FINISHED, HALTED
    }

    private PlayerState playerState;
    private double playerTime;
    private String message;

    /**
     * Constructor.
     *
     * @param state The state of the player.
     * @param time The time in seconds when this event occurred.
     * @throws IllegalArgumentException if <code>state</code> is <code>null</code>
     * or <code>time&lt;0.0</code>.
     */
    public PlayerStateEvent(PlayerState state, double time) {
        if (state == null) {
            throw new IllegalArgumentException("state == null!");
        } else if (time < 0.0) {
            throw new IllegalArgumentException("time < 0.0!");
        }

        this.playerState = state;
        this.playerTime = time;
    }

    /**
     * Constructor.
     *
     * @param state The state of the player.
     * @param time The time in seconds when this event occurred.
     * @param message Carries auxiliary message. HALTED state has additional information.
     * @throws IllegalArgumentException if <code>state</code> is <code>null</code>
     * or <code>time&lt;0.0</code>.
     */
    public PlayerStateEvent(PlayerState state, double time, String message) {
        this(state, time);
        this.message = message;
    }

    /**
     * Retrieves the state of the media player.
     *
     * @return The player's state.
     */
    public PlayerState getState() {
        return playerState;
    }

    /**
     * Presentation time when the event occurred.
     *
     * @return The time in seconds of the state transition.
     */
    public double getTime() {
        return playerTime;
    }

    /**
     * Auxiliary message information when available.
     *
     * @return The message or null.
     */
    public String getMessage() {
        return message;
    }
}
