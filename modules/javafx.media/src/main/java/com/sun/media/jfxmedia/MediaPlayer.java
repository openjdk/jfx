/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmedia;

import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.events.PlayerStateListener;
import com.sun.media.jfxmedia.events.VideoTrackSizeListener;
import com.sun.media.jfxmedia.control.VideoRenderControl;
import com.sun.media.jfxmedia.control.MediaPlayerOverlay;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.events.AudioSpectrumListener;
import com.sun.media.jfxmedia.events.MarkerListener;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.events.BufferListener;
import com.sun.media.jfxmedia.events.PlayerStateEvent.PlayerState;
import com.sun.media.jfxmedia.events.PlayerTimeListener;

/**
 * MediaPlayer class provides control of media playback.  Get a MediaPlayer from
 * either MediaManager or MediaRecorder.
 *
 * @see MediaManager
 * @see MediaRecorder
 */
public interface MediaPlayer {
    //**************************************************************************
    //***** Public control functions
    //**************************************************************************

    /**
     * Adds a listener for warnings which occur within the lifespan of the player.
     *
     * @param listener The warning listener.
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void addMediaErrorListener(MediaErrorListener listener);

    /**
     * Removes a listener for warnings.
     *
     * @param listener The warning listener.
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void removeMediaErrorListener(MediaErrorListener listener);

    /**
     * Adds a listener for media state.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void addMediaPlayerListener(PlayerStateListener listener);

    /**
     * Removes a listener for media state.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void removeMediaPlayerListener(PlayerStateListener listener);

    /**
     * Adds a listener for player time events.
     *
     * @param listener
     */
    public void addMediaTimeListener(PlayerTimeListener listener);

    /**
     * Removes a listener for player time events.
     *
     * @param listener
     */
    public void removeMediaTimeListener(PlayerTimeListener listener);

    /**
     * Adds a listener for video track frame dimensions.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void addVideoTrackSizeListener(VideoTrackSizeListener listener);

    /**
     * Removes a listener for video track frame dimensions.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void removeVideoTrackSizeListener(VideoTrackSizeListener listener);

    /**
     * Adds a listener for marker events.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void addMarkerListener(MarkerListener listener);

    /**
     * Removes a listener for marker events.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void removeMarkerListener(MarkerListener listener);

    /**
     * Adds a listener for all buffer events.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void addBufferListener(BufferListener listener);

    /**
     * Removes a listener for buffer events.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void removeBufferListener(BufferListener listener);

     /**
     * Adds a listener for audio spectrum events.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void addAudioSpectrumListener(AudioSpectrumListener listener);

    /**
     * Removes a listener for audio spectrum events.
     *
     * @param listener
     * @throws IllegalArgumentException if <code>listener</code> is
     * <code>null</code>.
     */
    public void removeAudioSpectrumListener(AudioSpectrumListener listener);

    /**
     * Returns the video rendering support interface.
     *
     * @return A <code>VideoRenderControl</code> instance.
     */
    public VideoRenderControl getVideoRenderControl();

    /**
     * Returns the media player overlay support interface.
     *
     * @return A <code>MediaPlayerOverlay</code> instance.
     */
    public MediaPlayerOverlay getMediaPlayerOverlay();

    /**
     * Gets a Media object.
     *
     * @return Media object.
     */
    public Media getMedia();

    /**
     * Set the amount of time to delay the audio. A positive value makes audio
     * render later, and a negative value makes audio render earlier.
     *
     * @param delay time in milliseconds
     */
    public void setAudioSyncDelay(long delay);

    /**
     * Retrieve the audio rendering delay.
     */
    public long getAudioSyncDelay();

    /**
     * Begins playing of the media.  To ensure smooth playback, catch the
     * onReady event in the MediaPlayerListener before playing.
     */
    public void play();

    /**
     * Stops playing of the media and resets the play time to 0.
     */
    public void stop();

    /**
     * Pauses the media playing.  Calling play() after pause() will continue
     * playing the media from where it left off.
     */
    public void pause();

    /**
     * Get the rate of playback.
     */
    public float getRate();

    //**************************************************************************
    //***** Public properties
    //**************************************************************************
    /**
     * Sets the rate of playback. A positive value indicates forward play and
     * a negative value reverse play.
     *
     * @param rate
     */
    public void setRate(float rate);

    /**
     * Gets the current presentation time. If the time is unknown or cannot be
     * obtained when this method is invoked, a negative value will be returned.
     *
     * @return the current presentation time
     */
    public double getPresentationTime();

    /**
     * Gets the current volume.
     *
     * @return the current volume
     */
    public float getVolume();

    /**
     * Sets the volume. Values will be clamped to the range
     * <code>[0,&nbsp;1.0]</code>.
     *
     * @param volume A value in the range <code>[0,&nbsp;1.0]</code>.
     */
    public void setVolume(float volume);

    /**
     * Gets the muted state. While muted no audio will be heard.
     * @return true if audio is muted.
     */
    public boolean getMute();

    /**
     * Enables/disable mute.  If mute is enabled then disabled, the previous
     * volume goes into effect.
     */
    public void setMute(boolean enable);

    /**
     * Gets the current balance.
     *
     * @return the current balance
     */
    public float getBalance();

    /**
     * Sets the balance. A negative value indicates left of center and a positive
     * value right of center. Values will be clamped to the range
     * <code>[-1.0,&nbsp;1.0]</code>.
     *
     * @param balance A value in the range <code>[-1.0,&nbsp;1.0]</code>.
     */
    public void setBalance(float balance);

    /**
     * Gets the audio equalizer for the player.
     *
     * @return AudioEqualizer object
     */
    public AudioEqualizer getEqualizer();

    /**
     * Gets the audio spectrum controller for the player.
     *
     * @return AudioSpectrum object
     */
    public AudioSpectrum getAudioSpectrum();

    /**
     * Gets the duration in seconds. If the duration is unknown or cannot be
     * obtained when this method is invoked, a negative value will be returned.
     */
    public double getDuration();

    /**
     * Gets the time within the duration of the media to start playing.
     */
    public double getStartTime();

    /**
     * Sets the start time within the media to play.
     */
    public void setStartTime(double streamTime);

    /**
     * Gets the time within the duration of the media to stop playing.
     */
    public double getStopTime();

    /**
     * Sets the stop time within the media to stop playback.
     */
    public void setStopTime(double streamTime);

    /**
     * Seeks playback to the specified time. The state of the player
     * is unchanged. A negative value will be clamped to zero, and a positive
     * value to the duration, if known.
     *
     * @param streamTime The time in seconds to which to seek.
     */
    public void seek(double streamTime);

    /**
     * Retrieves the current {@link PlayerState state} of the player.
     * @return the current player state.
     */
    public PlayerState getState();
    /**
     * Release any resources held by this player. The player will be unusable
     * after this method is invoked.
     */
    public void dispose();
    /**
     * Returns true if we have cached error event.
     */
    public boolean isErrorEventCached();
}
