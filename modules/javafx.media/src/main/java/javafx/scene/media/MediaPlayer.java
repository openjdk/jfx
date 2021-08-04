/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.media;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.util.Duration;
import javafx.util.Pair;

import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.control.VideoDataBuffer;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.events.AudioSpectrumEvent;
import com.sun.media.jfxmedia.events.BufferListener;
import com.sun.media.jfxmedia.events.BufferProgressEvent;
import com.sun.media.jfxmedia.events.MarkerEvent;
import com.sun.media.jfxmedia.events.MarkerListener;
import com.sun.media.jfxmedia.events.NewFrameEvent;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import com.sun.media.jfxmedia.events.PlayerStateListener;
import com.sun.media.jfxmedia.events.PlayerTimeListener;
import com.sun.media.jfxmedia.events.VideoTrackSizeListener;
import com.sun.media.jfxmedia.locator.Locator;
import java.util.*;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.EventHandler;

/**
 * The <code>MediaPlayer</code> class provides the controls for playing media.
 * It is used in combination with the {@link Media} and {@link MediaView}
 * classes to display and control media playback. <code>MediaPlayer</code> does
 * not contain any visual elements so must be used with the {@link MediaView}
 * class to view any video track which may be present.
 *
 * <p><code>MediaPlayer</code> provides the {@link #pause()}, {@link #play()},
 * {@link #stop()} and {@link #seek(javafx.util.Duration) seek()} controls as
 * well as the {@link #rateProperty rate} and {@link #autoPlayProperty autoPlay}
 * properties which apply to all types of media. It also provides the
 * {@link #balanceProperty balance}, {@link #muteProperty mute}, and
 * {@link #volumeProperty volume} properties which control audio playback
 * characteristics. Further control over audio quality may be attained via the
 * {@link AudioEqualizer} associated with the player. Frequency descriptors of
 * audio playback may be observed by registering an {@link AudioSpectrumListener}.
 * Information about playback position, rate, and buffering may be obtained from
 * the {@link #currentTimeProperty currentTime},
 * {@link #currentRateProperty currentRate}, and
 * {@link #bufferProgressTimeProperty bufferProgressTime}
 * properties, respectively. Media marker notifications are received by an event
 * handler registered as the {@link #onMarkerProperty onMarker} property.</p>
 *
 * <p>For finite duration media, playback may be positioned at any point in time
 * between <code>0.0</code> and the duration of the media. <code>MediaPlayer</code>
 * refines this definition by adding the {@link #startTimeProperty startTime} and
 * {@link #stopTimeProperty stopTime}
 * properties which in effect define a virtual media source with time position
 * constrained to <code>[startTime,stopTime]</code>. Media playback
 * commences at <code>startTime</code> and continues to <code>stopTime</code>.
 * The interval defined by these two endpoints is termed a <i>cycle</i> with
 * duration being the difference of the stop and start times. This cycle
 * may be set to repeat a specific or indefinite number of times. The total
 * duration of media playback is then the product of the cycle duration and the
 * number of times the cycle is played. If the stop time of the cycle is reached
 * and the cycle is to be played again, the event handler registered with the
 * {@link #onRepeatProperty onRepeat} property is invoked. If the stop time is
 * reached, then the event handler registered with the {@link #onEndOfMediaProperty onEndOfMedia}
 * property is invoked regardless of whether the cycle is to be repeated or not.
 * A zero-relative index of which cycle is presently being played is maintained
 * by {@link #currentCountProperty currentCount}.
 * </p>
 *
 * <p>The operation of a <code>MediaPlayer</code> is inherently asynchronous.
 * A player is not prepared to respond to commands quasi-immediately until
 * its status has transitioned to {@link Status#READY}, which in
 * effect generally occurs when media pre-roll completes. Some requests made of
 * a player prior to its status being <code>READY</code> will however take
 * effect when that status is entered. These include invoking {@link #play()}
 * without an intervening invocation of {@link #pause()} or {@link #stop()}
 * before the <code>READY</code> transition, as well as setting any of the
 * {@link #autoPlayProperty autoPlay}, {@link #balanceProperty balance},
 * {@link #muteProperty mute}, {@link #rateProperty rate},
 * {@link #startTimeProperty startTime}, {@link #stopTimeProperty stopTime}, and
 * {@link #volumeProperty volume} properties.</p>
 *
 * <p>The {@link #statusProperty status}
 * property may be monitored to make the application aware of player status
 * changes, and callback functions may be registered via properties such as
 * {@link #onReadyProperty onReady} if an action should be taken when a particular status is
 * entered. There are also {@link #errorProperty error} and {@link #onErrorProperty onError} properties which
 * respectively enable monitoring when an error occurs and taking a specified
 * action in response thereto.</p>
 *
 * <p>The same <code>MediaPlayer</code> object may be shared among multiple
 * <code>MediaView</code>s. This will not affect the player itself. In
 * particular, the property settings of the view will not have any effect on
 * media playback.</p>
 * @see Media
 * @see MediaView
 * @since JavaFX 2.0
 */
public final class MediaPlayer {

    /**
     * Enumeration describing the different status values of a {@link MediaPlayer}.
     *
     * <p>
     * The principal <code>MediaPlayer</code> status transitions are given in the
     * following table:
     * </p>
     * <table border="1">
     * <caption>MediaPlayer Status Transition Table</caption>
     * <tr>
     * <th scope="col">Current \ Next</th><th scope="col">READY</th><th scope="col">PAUSED</th>
     * <th scope="col">PLAYING</th><th scope="col">STALLED</th><th scope="col">STOPPED</th>
     * <th scope="col">DISPOSED</th>
     * </tr>
     * <tr>
     * <th scope="row"><b>UNKNOWN</b></th><td>pre-roll</td><td></td><td></td><td></td><td></td><td>dispose()</td>
     * </tr>
     * <tr>
     * <th scope="row"><b>READY</b></th><td></td><td></td><td>autoplay; play()</td><td></td><td></td><td>dispose()</td>
     * </tr>
     * <tr>
     * <th scope="row"><b>PAUSED</b></th><td></td><td></td><td>play()</td><td></td><td>stop()</td><td>dispose()</td>
     * </tr>
     * <tr>
     * <th scope="row"><b>PLAYING</b></th><td></td><td>pause()</td><td></td><td>buffering data</td><td>stop()</td><td>dispose()</td>
     * </tr>
     * <tr>
     * <th scope="row"><b>STALLED</b></th><td></td><td>pause()</td><td>data buffered</td><td></td><td>stop()</td><td>dispose()</td>
     * </tr>
     * <tr>
     * <th scope="row"><b>STOPPED</b></th><td></td><td>pause()</td><td>play()</td><td></td><td></td><td>dispose()</td>
     * </tr>
     * <tr>
     * <th scope="row"><b>HALTED</b></th><td></td><td></td><td></td><td></td><td></td><td>dispose()</td>
     * </tr>
     * </table>
     * <p>The table rows represent the current state of the player and the columns
     * the next state of the player. The cell at the intersection of a given row
     * and column lists the events which can cause a transition from the row
     * state to the column state. An empty cell represents an impossible transition.
     * The transitions to <code>UNKNOWN</code> and <code>HALTED</code> and from
     * <code>DISPOSED</code> status are intentionally not tabulated. <code>UNKNOWN</code>
     * is the initial status of the player before the media source is pre-rolled
     * and cannot be entered once exited. <code>DISPOSED</code> is a terminal status
     * entered after dispose() method is invoked and cannot be exited. <code>HALTED</code>
     * status entered when a critical error occurs and may be transitioned into
     * from any other status except <code>DISPOSED</code>.
     * </p>
     * <p>
     * The principal <code>MediaPlayer</code> status values and transitions are
     * depicted in the following diagram:
     * <br><br>
     * <img src="doc-files/mediaplayerstatus.png" alt="MediaPlayer status diagram">
     * </p>
     * <p>
     * Reaching the end of the media (or the
     * {@link #stopTimeProperty stopTime} if this is defined) while playing does not cause the
     * status to change from <code>PLAYING</code>. Therefore, for example, if
     * the media is played to its end and then a manual seek to an earlier
     * time within the media is performed, playing will continue from the
     * new media time.
     * </p>
     * @since JavaFX 2.0
     */
    public enum Status {

        /**
         * State of the player immediately after creation. While in this state,
         * property values are not reliable and should not be considered.
         * Additionally, commands sent to the player while in this state will be
         * buffered until the media is fully loaded and ready to play.
         */
        UNKNOWN,
        /**
         * State of the player once it is prepared to play.
         * This state is entered only once when the movie is loaded and pre-rolled.
         */
        READY,
        /**
         * State of the player when playback is paused. Requesting the player
         * to play again will cause it to continue where it left off.
         */
        PAUSED,
        /**
         * State of the player when it is currently playing.
         */
        PLAYING,
        /**
         * State of the player when playback has stopped.  Requesting the player
         * to play again will cause it to start playback from the beginning.
         */
        STOPPED,
        /**
         * State of the player when data coming into the buffer has slowed or
         * stopped and the playback buffer does not have enough data to continue
         * playing. Playback will continue automatically when enough data are
         * buffered to resume playback. If paused or stopped in this state, then
         * buffering will continue but playback will not resume automatically
         * when sufficient data are buffered.
         */
        STALLED,
        /**
         * State of the player when a critical error has occurred.  This state
         * indicates playback can never continue again with this player.  The
         * player is no longer functional and a new player should be created.
         */
        HALTED,
        /**
         * State of the player after dispose() method is invoked. This state indicates
         * player is disposed, all resources are free and player SHOULD NOT be used again.
         * <code>Media</code> and <code>MediaView</code> objects associated with disposed player can be reused.
         * @since JavaFX 8.0
         */
        DISPOSED
    };

    /**
     * A value representing an effectively infinite number of playback cycles.
     * When {@link #cycleCountProperty cycleCount} is set to this value, the player
     * will replay the <code>Media</code> until stopped or paused.
     */
    public static final int INDEFINITE = -1; // Note: this is a count, not a Duration.

    private static final double RATE_MIN = 0.0;
    private static final double RATE_MAX = 8.0;

    private static final int AUDIOSPECTRUM_THRESHOLD_MAX = 0; // dB

    private static final double AUDIOSPECTRUM_INTERVAL_MIN = 0.000000001; // seconds

    private static final int AUDIOSPECTRUM_NUMBANDS_MIN = 2;

    // The underlying player
    private com.sun.media.jfxmedia.MediaPlayer jfxPlayer;
    // Need package getter for MediaView
    com.sun.media.jfxmedia.MediaPlayer retrieveJfxPlayer() {
        synchronized (disposeLock) {
            return jfxPlayer;
        }
    }

    private MapChangeListener<String,Duration> markerMapListener = null;
    private MarkerListener markerEventListener = null;

    private PlayerStateListener stateListener = null;
    private PlayerTimeListener timeListener = null;
    private VideoTrackSizeListener sizeListener = null;
    private com.sun.media.jfxmedia.events.MediaErrorListener errorListener = null;
    private BufferListener bufferListener = null;
    private com.sun.media.jfxmedia.events.AudioSpectrumListener spectrumListener = null;
    private RendererListener rendererListener = null;

    // Store requested operations sent before we receive the onReady event
    private boolean rateChangeRequested = false;
    private boolean volumeChangeRequested = false;
    private boolean balanceChangeRequested = false;
    private boolean startTimeChangeRequested = false;
    private boolean stopTimeChangeRequested = false;
    private boolean muteChangeRequested = false;
    private boolean playRequested = false;
    private boolean audioSpectrumNumBandsChangeRequested = false;
    private boolean audioSpectrumIntervalChangeRequested = false;
    private boolean audioSpectrumThresholdChangeRequested = false;
    private boolean audioSpectrumEnabledChangeRequested = false;

    private MediaTimerTask mediaTimerTask = null;
    private double prevTimeMs = -1.0;
    private boolean isUpdateTimeEnabled = false;
    private BufferProgressEvent lastBufferEvent = null;
    private Duration startTimeAtStop = null;
    private boolean isEOS = false;

    private final Object disposeLock = new Object();

    private final static int DEFAULT_SPECTRUM_BAND_COUNT = 128;
    private final static double DEFAULT_SPECTRUM_INTERVAL = 0.1;
    private final static int DEFAULT_SPECTRUM_THRESHOLD = -60;

    // views to be notified on media change
    private final Set<WeakReference<MediaView>> viewRefs =
            new HashSet<WeakReference<MediaView>>();

    /**
     * The read-only {@link AudioEqualizer} associated with this player. The
     * equalizer is enabled by default.
     */
    private AudioEqualizer audioEqualizer;

    private static double clamp(double dvalue, double dmin, double dmax) {
        if (dmin != Double.MIN_VALUE && dvalue < dmin) {
            return dmin;
        } else if (dmax != Double.MAX_VALUE && dvalue > dmax) {
            return dmax;
        } else {
            return dvalue;
        }
    }

    private static int clamp(int ivalue, int imin, int imax) {
        if (imin != Integer.MIN_VALUE && ivalue < imin) {
            return imin;
        } else if (imax != Integer.MAX_VALUE && ivalue > imax) {
            return imax;
        } else {
            return ivalue;
        }
    }

    /**
     * Retrieve the {@link AudioEqualizer} associated with this player.
     * @return the <code>AudioEqualizer</code> or <code>null</code> if player is disposed.
     */
    public final AudioEqualizer getAudioEqualizer() {
        synchronized (disposeLock) {
            if (getStatus() == Status.DISPOSED) {
                return null;
            }

            if (audioEqualizer == null) {
                audioEqualizer = new AudioEqualizer();
                if (jfxPlayer != null) {
                    audioEqualizer.setAudioEqualizer(jfxPlayer.getEqualizer());
                }
                audioEqualizer.setEnabled(true);
            }
            return audioEqualizer;
        }
    }

    /**
     * Create a player for a specific media. This is the only way to associate
     * a <code>Media</code> object with a <code>MediaPlayer</code>: once the
     * player is created it cannot be changed. Errors which occur synchronously
     * within the constructor will cause exceptions to be thrown. Errors which
     * occur asynchronously will cause the {@link #errorProperty error} property to be set and
     * consequently any {@link #onErrorProperty onError} callback to be invoked.
     *
     * <p>When created, the {@link #statusProperty status} of the player will be {@link Status#UNKNOWN}.
     * Once the <code>status</code> has transitioned to {@link Status#READY} the
     * player will be in a usable condition. The amount of time between player
     * creation and its entering <code>READY</code> status may vary depending,
     * for example, on whether the media is being read over a network connection
     * or from a local file system.
     *
     * @param media The media to play.
     * @throws NullPointerException if media is <code>null</code>.
     * @throws MediaException if any synchronous errors occur within the
     * constructor.
     */
    public MediaPlayer(@NamedArg("media") Media media) {
        if (null == media) {
            throw new NullPointerException("media == null!");
        }

        this.media = media;

        // So we can get errors during initialization from other threads (Ex. HLS).
        errorListener = new _MediaErrorListener();
        MediaManager.addMediaErrorListener(errorListener);

        try {
            // Init MediaPlayer. Run on separate thread if locator can block.
            Locator locator = media.retrieveJfxLocator();
            if (locator.canBlock()) {
                InitMediaPlayer initMediaPlayer = new InitMediaPlayer();
                Thread t = new Thread(initMediaPlayer);
                t.setDaemon(true);
                t.start();
            } else {
                init();
            }
        } catch (com.sun.media.jfxmedia.MediaException e) {
            throw MediaException.exceptionToMediaException(e);
        } catch (MediaException e) {
            throw e;
        }
    }

    void registerListeners() {
        synchronized (disposeLock) {
            if (getStatus() == Status.DISPOSED) {
                return;
            }

            if (jfxPlayer != null) {
                // Register jfxPlayer for dispose. It will be disposed when FX MediaPlayer does not have
                // any strong references.
                MediaManager.registerMediaPlayerForDispose(this, jfxPlayer);

                jfxPlayer.addMediaErrorListener(errorListener);

                jfxPlayer.addMediaTimeListener(timeListener);
                jfxPlayer.addVideoTrackSizeListener(sizeListener);
                jfxPlayer.addBufferListener(bufferListener);
                jfxPlayer.addMarkerListener(markerEventListener);
                jfxPlayer.addAudioSpectrumListener(spectrumListener);
                jfxPlayer.getVideoRenderControl().addVideoRendererListener(rendererListener);
                jfxPlayer.addMediaPlayerListener(stateListener);
            }

            if (null != rendererListener) {
                // add a stage listener, this will be called before scene listeners
                // so we can make sure the dirty bits are set correctly before PG sync
                Toolkit.getToolkit().addStageTkPulseListener(rendererListener);
            }
        }
    }

    private void init() throws MediaException {
        try {
            // Create a new player
            Locator locator = media.retrieveJfxLocator();

            // This call will block until we connected or fail to connect.
            // Call it here, so we do not block while initializing and holding locks like disposeLock.
            locator.waitForReadySignal();

            synchronized (disposeLock) {
                if (getStatus() == Status.DISPOSED) {
                    return;
                }

                jfxPlayer = MediaManager.getPlayer(locator);

                if (jfxPlayer != null) {
                    // Register media player with shutdown hook.
                    MediaPlayerShutdownHook.addMediaPlayer(this);

                    // Make sure we start with a known state
                    jfxPlayer.setBalance((float) getBalance());
                    jfxPlayer.setMute(isMute());
                    jfxPlayer.setVolume((float) getVolume());

                    // Create listeners for the Player's event
                    sizeListener = new _VideoTrackSizeListener();
                    stateListener = new _PlayerStateListener();
                    timeListener = new _PlayerTimeListener();
                    bufferListener = new _BufferListener();
                    markerEventListener = new _MarkerListener();
                    spectrumListener = new _SpectrumListener();
                    rendererListener = new RendererListener();
                }

                // Listen to Media.getMarkers() so as to propagate updates of the
                // map to the implementation layer.
                markerMapListener = new MarkerMapChangeListener();
                ObservableMap<String, Duration> markers = media.getMarkers();
                markers.addListener(markerMapListener);

                // Propagate to the implementation layer any markers already in
                // Media.getMarkers().
                com.sun.media.jfxmedia.Media jfxMedia = jfxPlayer.getMedia();
                for (Map.Entry<String, Duration> entry : markers.entrySet()) {
                    String markerName = entry.getKey();
                    if (markerName != null) {
                        Duration markerTime = entry.getValue();
                        if (markerTime != null) {
                            double msec = markerTime.toMillis();
                            if (msec >= 0.0) {
                                jfxMedia.addMarker(markerName, msec / 1000.0);
                            }
                        }
                    }
                }
            }
        } catch (com.sun.media.jfxmedia.MediaException e) {
            throw MediaException.exceptionToMediaException(e);
        }

        // Register for the Player's event
        Platform.runLater(() -> {
            registerListeners();
        });
    }

    private class InitMediaPlayer implements Runnable {

        @Override
        public void run() {
            try {
                init();
            } catch (com.sun.media.jfxmedia.MediaException e) {
                handleError(MediaException.exceptionToMediaException(e));
            } catch (MediaException e) {
                // Check media object for error. If it is connection related, then Media object will have better error message
                if (media.getError() != null) {
                    handleError(media.getError());
                } else {
                    handleError(e);
                }
            } catch (Exception e) {
                handleError(new MediaException(MediaException.Type.UNKNOWN, e.getMessage()));
            }
        }
    }

    /**
     * Observable property set to a <code>MediaException</code> if an error occurs.
     */
    private ReadOnlyObjectWrapper<MediaException> error;

    private void setError(MediaException value) {
        if (getError() == null) {
            errorPropertyImpl().set(value);
        }
    }

    /**
     * Retrieve the value of the {@link #errorProperty error} property or <code>null</code>
     * if there is no error.
     * @return a <code>MediaException</code> or <code>null</code>.
     */
    public final MediaException getError() {
        return error == null ? null : error.get();
    }

    public ReadOnlyObjectProperty<MediaException> errorProperty() {
        return errorPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<MediaException> errorPropertyImpl() {
        if (error == null) {
            error = new ReadOnlyObjectWrapper<MediaException>() {

                @Override
                protected void invalidated() {
                    if (getOnError() != null) {
                        Platform.runLater(getOnError());
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "error";
                }
            };
        }
        return error;
    }

    /**
     * Event handler invoked when an error occurs.
     */
    private ObjectProperty<Runnable> onError;

    /**
     * Sets the event handler to be called when an error occurs.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnError(Runnable value) {
        onErrorProperty().set(value);
    }

    /**
     * Retrieves the event handler for errors.
     * @return the event handler.
     */
    public final Runnable getOnError() {
        return onError == null ? null : onError.get();
    }

    public ObjectProperty<Runnable> onErrorProperty() {
        if (onError == null) {
            onError = new ObjectPropertyBase<Runnable>() {

                @Override
                protected void invalidated() {
                    /*
                     * if we have an existing error condition schedule the handler to be
                     * called immediately. This way the client app does not have to perform
                     * an explicit error check.
                     */
                    if (get() != null && getError() != null) {
                        Platform.runLater(get());
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "onError";
                }
            };
        }
        return onError;
    }

    /**
     * The parent {@link Media} object; read-only.
     *
     * @see Media
     */
    private Media media;

    /**
     * Retrieves the {@link Media} instance being played.
     * @return the <code>Media</code> object.
     */
    public final Media getMedia() {
        return media;
    }

    /**
     * Whether playing should start as soon as possible. For a new player this
     * will occur once the player has reached the READY state. The default
     * value is <code>false</code>.
     *
     * @see MediaPlayer.Status
     */
    private BooleanProperty autoPlay;

    /**
     * Sets the {@link #autoPlayProperty autoPlay} property value.
     * @param value whether to enable auto-playback
     */
    public final void setAutoPlay(boolean value) {
        autoPlayProperty().set(value);
    }

    /**
     * Retrieves the {@link #autoPlayProperty autoPlay} property value.
     * @return the value.
     */
    public final boolean isAutoPlay() {
        return autoPlay == null ? false : autoPlay.get();
    }

    public BooleanProperty autoPlayProperty() {
        if (autoPlay == null) {
            autoPlay = new BooleanPropertyBase() {

                @Override
                protected void invalidated() {
                    if (autoPlay.get()) {
                        play();
                    } else {
                        playRequested = false;
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "autoPlay";
                }
            };
        }
        return autoPlay;
    }

    private boolean playerReady;

    /**
     * Starts playing the media. If previously paused, then playback resumes
     * where it was paused. If playback was stopped, playback starts
     * from the {@link #startTimeProperty startTime}. When playing actually starts the
     * {@link #statusProperty status} will be set to {@link Status#PLAYING}.
     */
    public void play() {
        synchronized (disposeLock) {
            if (getStatus() != Status.DISPOSED) {
                if (playerReady) {
                    jfxPlayer.play();
                } else {
                    playRequested = true;
                }
            }
        }
    }

    /**
     * Pauses the player. Once the player is actually paused the {@link #statusProperty status}
     * will be set to {@link Status#PAUSED}.
     */
    public void pause() {
        synchronized (disposeLock) {
            if (getStatus() != Status.DISPOSED) {
                if (playerReady) {
                    jfxPlayer.pause();
                } else {
                    playRequested = false;
                }
            }
        }
    }

    /**
     * Stops playing the media. This operation resets playback to
     * {@link #startTimeProperty startTime}, and resets
     * {@link #currentCountProperty currentCount} to zero. Once the player is actually
     * stopped, the {@link #statusProperty status} will be set to {@link Status#STOPPED}. The
     * only transitions out of <code>STOPPED</code> status are to
     * {@link Status#PAUSED} and {@link Status#PLAYING} which occur after
     * invoking {@link #pause()} or {@link #play()}, respectively.
     * While stopped, the player will not respond to playback position changes
     * requested by {@link #seek(javafx.util.Duration)}.
     */
    public void stop() {
        synchronized (disposeLock) {
            if (getStatus() != Status.DISPOSED) {
                if (playerReady) {
                    jfxPlayer.stop();
                    setCurrentCount(0);
                    destroyMediaTimer(); // Stop media timer
                } else {
                    playRequested = false;
                }
            }
        }
    }

    /**
     * The rate at which the media should be played. For example, a rate of
     * <code>1.0</code> plays the media at its normal (encoded) playback rate,
     * <code>2.0</code> plays back at twice the normal rate, etc. The currently
     * supported range of rates is <code>[0.0,&nbsp;8.0]</code>. The default
     * value is <code>1.0</code>.
     */
    private DoubleProperty rate;

    /**
     * Sets the playback rate to the supplied value. Its effect will be clamped
     * to the range <code>[0.0,&nbsp;8.0]</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     * @param value the playback rate
     */
    public final void setRate(double value) {
        rateProperty().set(value);
    }

    /**
     * Retrieves the playback rate.
     * @return the playback rate
     */
    public final double getRate() {
        return rate == null ? 1.0 : rate.get();
    }

    public DoubleProperty rateProperty() {
        if (rate == null) {
            rate = new DoublePropertyBase(1.0) {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                if (jfxPlayer.getDuration() != Double.POSITIVE_INFINITY) {
                                    jfxPlayer.setRate((float) clamp(rate.get(), RATE_MIN, RATE_MAX));
                                }
                            } else {
                                rateChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "rate";
                }
            };
        }
        return rate;
    }

    /**
     * The current rate of playback regardless of settings. For example, if
     * <code>rate</code> is set to 1.0 and the player is paused or stalled,
     * then <code>currentRate</code> will be zero.
     */
    // FIXME: we should see if we can track rate in the native player instead
    private ReadOnlyDoubleWrapper currentRate;

    private void setCurrentRate(double value) {
        currentRatePropertyImpl().set(value);
    }

    /**
     * Retrieves the current playback rate.
     * @return the current rate
     */
    public final double getCurrentRate() {
        return currentRate == null ? 0.0 : currentRate.get();
    }

    public ReadOnlyDoubleProperty currentRateProperty() {
        return currentRatePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper currentRatePropertyImpl() {
        if (currentRate == null) {
            currentRate = new ReadOnlyDoubleWrapper(this, "currentRate");
        }
        return currentRate;
    }

    /**
     * The volume at which the media should be played. The range of effective
     * values is <code>[0.0&nbsp;1.0]</code> where <code>0.0</code> is inaudible
     * and <code>1.0</code> is full volume, which is the default.
     */
    private DoubleProperty volume;

    /**
     * Sets the audio playback volume. Its effect will be clamped to the range
     * <code>[0.0,&nbsp;1.0]</code>.
     *
     * @param value the volume
     */
    public final void setVolume(double value) {
        volumeProperty().set(value);
    }

    /**
     * Retrieves the audio playback volume. The default value is <code>1.0</code>.
     * @return the audio volume
     */
    public final double getVolume() {
        return volume == null ? 1.0 : volume.get();
    }

    public DoubleProperty volumeProperty() {
        if (volume == null) {
            volume = new DoublePropertyBase(1.0) {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                jfxPlayer.setVolume((float) clamp(volume.get(), 0.0, 1.0));
                            } else {
                                volumeChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "volume";
                }
            };
        }
        return volume;
    }

    /**
     * The balance, or left-right setting, of the audio output. The range of
     * effective values is <code>[-1.0,&nbsp;1.0]</code> with <code>-1.0</code>
     * being full left, <code>0.0</code> center, and <code>1.0</code> full right.
     * The default value is <code>0.0</code>.
     */
    private DoubleProperty balance;

    /**
     * Sets the audio balance. Its effect will be clamped to the range
     * <code>[-1.0,&nbsp;1.0]</code>.
     * @param value the balance
     */
    public final void setBalance(double value) {
        balanceProperty().set(value);
    }

    /**
     * Retrieves the audio balance.
     * @return the audio balance
     */
    public final double getBalance() {
        return balance == null ? 0.0F : balance.get();
    }

    public DoubleProperty balanceProperty() {
        if (balance == null) {
            balance = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                jfxPlayer.setBalance((float) clamp(balance.get(), -1.0, 1.0));
                            } else {
                                balanceChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "balance";
                }
            };
        }
        return balance;
    }

    /**
     * Behaviorally clamp the start and stop times. The parameters are clamped
     * to the range <code>[0.0,&nbsp;duration]</code>. If the duration is not
     * known, {@link Double#MAX_VALUE} is used instead. Furthermore, if the
     * separately clamped values satisfy
     * <code>startTime&nbsp;&gt;&nbsp;stopTime</code>
     * then <code>stopTime</code> is clamped as
     * <code>stopTime&nbsp;&ge;&nbsp;startTime</code>.
     *
     * @param startValue the new start time.
     * @param stopValue the new stop time.
     * @return the clamped times in seconds as <code>{actualStart,&nbsp;actualStop}</code>.
     */
    private double[] calculateStartStopTimes(Duration startValue, Duration stopValue) {
        // Derive start time in seconds.
        double newStart;
        if (startValue == null || startValue.lessThan(Duration.ZERO)
                || startValue.equals(Duration.UNKNOWN)) {
            newStart = 0.0;
        } else if (startValue.equals(Duration.INDEFINITE)) {
            newStart = Double.MAX_VALUE;
        } else {
            newStart = startValue.toMillis() / 1000.0;
        }

        // Derive stop time in seconds.
        double newStop;
        if (stopValue == null || stopValue.equals(Duration.UNKNOWN)
                || stopValue.equals(Duration.INDEFINITE)) {
            newStop = Double.MAX_VALUE;
        } else if (stopValue.lessThan(Duration.ZERO)) {
            newStop = 0.0;
        } else {
            newStop = stopValue.toMillis() / 1000.0;
        }

        // Derive the duration in seconds.
        Duration mediaDuration = media.getDuration();
        double duration = mediaDuration == Duration.UNKNOWN ?
            Double.MAX_VALUE : mediaDuration.toMillis()/1000.0;

        // Clamp the start and stop times to [0,duration].
        double actualStart = clamp(newStart, 0.0, duration);
        double actualStop = clamp(newStop, 0.0, duration);

        // Restrict actual stop time to [startTime,duration].
        if (actualStart > actualStop) {
            actualStop = actualStart;
        }

        return new double[] {actualStart, actualStop};
    }

    /**
     * Set the effective start and stop times on the underlying player,
     * clamping as needed.
     *
     * @param startValue the new start time.
     * @param stopValue the new stop time.
     */
    private void setStartStopTimes(Duration startValue, boolean isStartValueSet, Duration stopValue, boolean isStopValueSet) {
        if (jfxPlayer.getDuration() == Double.POSITIVE_INFINITY) {
            return;
        }

        // Clamp the start and stop times to values in seconds.
        double[] startStop = calculateStartStopTimes(startValue, stopValue);

        // Set the start and stop times on the underlying player.
        if (isStartValueSet) {
            jfxPlayer.setStartTime(startStop[0]);
            if (getStatus() == Status.READY || getStatus() == Status.PAUSED) {
                Platform.runLater(() -> {
                    setCurrentTime(getStartTime());
                });
            }
        }
        if (isStopValueSet) {
            jfxPlayer.setStopTime(startStop[1]);
        }
    }

    /**
     * The time offset where media should start playing, or restart from when
     * repeating. When playback is stopped, the current time is reset to this
     * value. If this value is positive, then the first time the media is
     * played there might be a delay before playing begins unless the play
     * position can be set to an arbitrary time within the media. This could
     * occur for example for a video which does not contain a lookup table
     * of the offsets of intra-frames in the video stream. In such a case the
     * video frames would need to be skipped over until the position of the
     * first intra-frame before the start time was reached. The default value is
     * <code>Duration.ZERO</code>.
     *
     * <p>Constraints: <code>0&nbsp;&le;&nbsp;startTime&nbsp;&lt;&nbsp;{@link #stopTimeProperty stopTime}</code>
     */
    private ObjectProperty<Duration> startTime;

    /**
     * Sets the start time. Its effect will be clamped to
     * the range <code>[{@link Duration#ZERO},&nbsp;{@link #stopTimeProperty stopTime})</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     *
     * @param value the start time
     */
    public final void setStartTime(Duration value) {
        startTimeProperty().set(value);
    }

    /**
     * Retrieves the start time. The default value is <code>Duration.ZERO</code>.
     * @return the start time
     */
    public final Duration getStartTime() {
        return startTime == null ? Duration.ZERO : startTime.get();
    }

    public ObjectProperty<Duration> startTimeProperty() {
        if (startTime == null) {
            startTime = new ObjectPropertyBase<Duration>() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                setStartStopTimes(startTime.get(), true, getStopTime(), false);
                            } else {
                                startTimeChangeRequested = true;
                            }
                            calculateCycleDuration();
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "startTime";
                }
            };
        }
        return startTime;
    }
    /**
     * The time offset where media should stop playing or restart when repeating.
     * The default value is <code>{@link #getMedia()}.getDuration()</code>.
     *
     * <p>Constraints: <code>{@link #startTimeProperty startTime}&nbsp;&lt;&nbsp;stopTime&nbsp;&le;&nbsp;{@link Media#durationProperty Media.duration}</code>
     */
    private ObjectProperty<Duration> stopTime;

    /**
     * Sets the stop time. Its effect will be clamped to
     * the range <code>({@link #startTimeProperty startTime},&nbsp;{@link Media#durationProperty Media.duration}]</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     *
     * @param value the stop time
     */
    public final void setStopTime (Duration value) {
        stopTimeProperty().set(value);
    }

    /**
     * Retrieves the stop time. The default value is
     * <code>{@link #getMedia()}.getDuration()</code>. Note that
     * <code>{@link Media#durationProperty Media.duration}</code> may have the value
     * <code>Duration.UNKNOWN</code> if media initialization is not complete.
     * @return the stop time
     */
    public final Duration getStopTime() {
        return stopTime == null ? media.getDuration() : stopTime.get();
    }

    public ObjectProperty<Duration> stopTimeProperty() {
        if (stopTime == null) {
            stopTime = new ObjectPropertyBase<Duration>() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                setStartStopTimes(getStartTime(), false, stopTime.get(), true);
                            } else {
                                stopTimeChangeRequested = true;
                            }
                            calculateCycleDuration();
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "stopTime";
                }
            };
        }
        return stopTime;
    }

    /**
     * The amount of time between the {@link #startTimeProperty startTime} and
     * {@link #stopTimeProperty stopTime}
     * of this player. For the total duration of the Media use the
     * {@link Media#durationProperty Media.duration} property.
     */
    private ReadOnlyObjectWrapper<Duration> cycleDuration;


    private void setCycleDuration(Duration value) {
        cycleDurationPropertyImpl().set(value);
    }

    /**
     * Retrieves the cycle duration in seconds.
     * @return the cycle duration
     */
    public final Duration getCycleDuration() {
        return cycleDuration == null ? Duration.UNKNOWN : cycleDuration.get();
    }

    public ReadOnlyObjectProperty<Duration> cycleDurationProperty() {
        return cycleDurationPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> cycleDurationPropertyImpl() {
        if (cycleDuration == null) {
            cycleDuration = new ReadOnlyObjectWrapper<Duration>(this, "cycleDuration");
        }
        return cycleDuration;
    }

    // recalculate cycleDuration based on startTime, stopTime and Media.duration
    // if any are UNKNOWN then this is UNKNOWN
    private void calculateCycleDuration() {
        Duration endTime;
        Duration mediaDuration = media.getDuration();

        if (!getStopTime().isUnknown()) {
            endTime = getStopTime();
        } else {
            endTime = mediaDuration;
        }
        if (endTime.greaterThan(mediaDuration)) {
            endTime = mediaDuration;
        }

        // filter bad values
        if (endTime.isUnknown() || getStartTime().isUnknown() || getStartTime().isIndefinite()) {
            if (!getCycleDuration().isUnknown())
                setCycleDuration(Duration.UNKNOWN);
        }

        setCycleDuration(endTime.subtract(getStartTime()));
        calculateTotalDuration(); // since it's dependent on cycle duration
    }
    /**
     * The total amount of play time if allowed to play until finished. If
     * <code>cycleCount</code> is set to <code>INDEFINITE</code> then this will
     * also be INDEFINITE. If the Media duration is UNKNOWN, then this will
     * likewise be UNKNOWN. Otherwise, total duration will be the product of
     * cycleDuration and cycleCount.
     */
    private ReadOnlyObjectWrapper<Duration> totalDuration;


    private void setTotalDuration(Duration value) {
        totalDurationPropertyImpl().set(value);
    }

    /**
     * Retrieves the total playback duration including all cycles (repetitions).
     * @return the total playback duration
     */
    public final Duration getTotalDuration() {
        return totalDuration == null ? Duration.UNKNOWN : totalDuration.get();
    }

    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDurationPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> totalDurationPropertyImpl() {
        if (totalDuration == null) {
            totalDuration = new ReadOnlyObjectWrapper<Duration>(this, "totalDuration");
        }
        return totalDuration;
    }
     private void calculateTotalDuration() {
         if (getCycleCount() == INDEFINITE) {
             setTotalDuration(Duration.INDEFINITE);
         } else if (getCycleDuration().isUnknown()) {
             setTotalDuration(Duration.UNKNOWN);
         } else {
             setTotalDuration(getCycleDuration().multiply((double)getCycleCount()));
         }
     }

    /**
     * The current media playback time. This property is read-only: use
     * {@link #seek(javafx.util.Duration)} to change playback to a different
     * stream position.
     *
     */
    private ReadOnlyObjectWrapper<Duration> currentTime;


    private void setCurrentTime(Duration value) {
        currentTimePropertyImpl().set(value);
    }

    /**
     * Retrieves the current media time.
     * @return the current media time
     */
    public final Duration getCurrentTime() {
        synchronized (disposeLock) {
            if (getStatus() == Status.DISPOSED) {
                return Duration.ZERO;
            }

            if (getStatus() == Status.STOPPED) {
                return Duration.millis(getStartTime().toMillis());
            }

            if (isEOS) {
                Duration duration = media.getDuration();
                Duration stopTime = getStopTime();
                if (stopTime != Duration.UNKNOWN && duration != Duration.UNKNOWN) {
                    if (stopTime.greaterThan(duration)) {
                        return Duration.millis(duration.toMillis());
                    } else {
                        return Duration.millis(stopTime.toMillis());
                    }
                }
            }

            // Query the property value. This is necessary even if the returned
            // value is not used below as setting the property value in
            // setCurrentTime() as is done in updateTime() which is called by the
            // MediaTimer will not trigger invalidation events unless the previous
            // value of the property has been retrieved via get().
            Duration theCurrentTime = currentTimeProperty().get();

            // Query the implementation layer for a more accurate value of the time.
            // The MediaTimer only updates the property at a fixed interval and
            // the present method might be called too far away from a timer update.
            if (playerReady) {
                double timeSeconds = jfxPlayer.getPresentationTime();
                if (timeSeconds >= 0.0) {
                    theCurrentTime = Duration.seconds(timeSeconds);
                    // We do not set the currentTime property value here as doing so
                    // could result in an infinite loop if getCurrentTime() is for
                    // example being invoked by an Invaludation listener of
                    // currentTime, for example in response to MediaTimer calling
                    // updateTime().
                }
            }

            return theCurrentTime;
        }
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTimePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> currentTimePropertyImpl() {
        if (currentTime == null) {
            currentTime = new ReadOnlyObjectWrapper<Duration>(this, "currentTime");
            currentTime.setValue(Duration.ZERO);
            updateTime();
        }
        return currentTime;
    }

    /**
     * Seeks the player to a new playback time. Invoking this method will have
     * no effect while the player status is {@link Status#STOPPED} or media duration is {@link Duration#INDEFINITE}.
     *
     * <p>The behavior of <code>seek()</code> is constrained as follows where
     * <i>start time</i> and <i>stop time</i> indicate the effective lower and
     * upper bounds, respectively, of media playback:
     * </p>
     * <table border="1">
     * <caption>MediaPlayer Seek Table</caption>
     * <tr><th scope="col">seekTime</th><th scope="col">seek position</th></tr>
     * <tr><th scope="row"><code>null</code></th><td>no change</td></tr>
     * <tr><th scope="row">{@link Duration#UNKNOWN}</th><td>no change</td></tr>
     * <tr><th scope="row">{@link Duration#INDEFINITE}</th><td>stop time</td></tr>
     * <tr><th scope="row">seekTime&nbsp;&lt;&nbsp;start time</th><td>start time</td></tr>
     * <tr><th scope="row">seekTime&nbsp;&gt;&nbsp;stop time</th><td>stop time</td></tr>
     * <tr><th scope="row">start time&nbsp;&le;&nbsp;seekTime&nbsp;&le;&nbsp;stop time</th><td>seekTime</td></tr>
     * </table>
     *
     * @param seekTime the requested playback time
     */
    public void seek(Duration seekTime) {
        synchronized (disposeLock) {
            if (getStatus() == Status.DISPOSED) {
                return;
            }

            // Seek only if the player is ready and the seekTime is valid.
            if (playerReady && seekTime != null && !seekTime.isUnknown()) {
                if (jfxPlayer.getDuration() == Double.POSITIVE_INFINITY) {
                    return;
                }

                // Determine the seek position in seconds.
                double seekSeconds;

                // Duration.INDEFINITE means seek to end.
                if (seekTime.isIndefinite()) {
                    // Determine the effective duration.
                    Duration duration = media.getDuration();
                    if (duration == null
                            || duration.isUnknown()
                            || duration.isIndefinite()) {
                        duration = Duration.millis(Double.MAX_VALUE);
                    }

                    // Convert the duration to seconds.
                    seekSeconds = duration.toMillis() / 1000.0;
                } else {
                    // Convert the parameter to seconds.
                    seekSeconds = seekTime.toMillis() / 1000.0;

                    // Clamp the seconds if needed.
                    double[] startStop = calculateStartStopTimes(getStartTime(), getStopTime());
                    if (seekSeconds < startStop[0]) {
                        seekSeconds = startStop[0];
                    } else if (seekSeconds > startStop[1]) {
                        seekSeconds = startStop[1];
                    }
                }

                if (!isUpdateTimeEnabled) {
                    // Change time update flag to true amd current rate to rate
                    // if status is PLAYING and current time is in range.
                    Status playerStatus = getStatus();
                    if ((playerStatus == MediaPlayer.Status.PLAYING
                            || playerStatus == MediaPlayer.Status.PAUSED)
                            && getStartTime().toSeconds() <= seekSeconds
                            && seekSeconds <= getStopTime().toSeconds()) {
                        isEOS = false;
                        isUpdateTimeEnabled = true;
                        setCurrentRate(getRate());
                    }
                }

                // Perform the seek.
                jfxPlayer.seek(seekSeconds);
            }
        }
    }
    /**
     * The current state of the MediaPlayer.
     */
    private ReadOnlyObjectWrapper<Status> status;

    private void setStatus(Status value) {
        statusPropertyImpl().set(value);
    }

    /**
     * Retrieves the current player status.
     * @return the playback status
     */
    public final Status getStatus() {
        return status == null ? Status.UNKNOWN : status.get();
    }

    public ReadOnlyObjectProperty<Status> statusProperty() {
        return statusPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Status> statusPropertyImpl() {
        if (status == null) {
            status = new ReadOnlyObjectWrapper<Status>() {

                @Override
                protected void invalidated() {
                    // use status changes to update currentRate
                    if (get() == Status.PLAYING) {
                        setCurrentRate(getRate());
                    } else {
                        setCurrentRate(0.0);
                    }

                    // Signal status updates
                    if (get() == Status.READY) {
                        if (getOnReady() != null) {
                            Platform.runLater(getOnReady());
                        }
                    } else if (get() == Status.PLAYING) {
                        if (getOnPlaying() != null) {
                            Platform.runLater(getOnPlaying());
                        }
                    } else if (get() == Status.PAUSED) {
                        if (getOnPaused() != null) {
                            Platform.runLater(getOnPaused());
                        }
                    } else if (get() == Status.STOPPED) {
                        if (getOnStopped() != null) {
                            Platform.runLater(getOnStopped());
                        }
                    } else if (get() == Status.STALLED) {
                        if (getOnStalled() != null) {
                            Platform.runLater(getOnStalled());
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "status";
                }
            };
        }
        return status;
    }
    /**
     * The current buffer position indicating how much media can be played
     * without stalling the <code>MediaPlayer</code>. This is applicable to
     * buffered streams such as those reading from network connections as
     * opposed for example to local files.
     *
     * <p>Seeking to a position beyond <code>bufferProgressTime</code> might
     * cause a slight pause in playback until an amount of data sufficient to
     * permit playback resumption has been buffered.
     */
    private ReadOnlyObjectWrapper<Duration> bufferProgressTime;

    private void setBufferProgressTime(Duration value) {
        bufferProgressTimePropertyImpl().set(value);
    }

    /**
     * Retrieves the {@link #bufferProgressTimeProperty bufferProgressTime} value.
     * @return the buffer progress time
     */
    public final Duration getBufferProgressTime() {
        return bufferProgressTime == null ? null : bufferProgressTime.get();
    }

    public ReadOnlyObjectProperty<Duration> bufferProgressTimeProperty() {
        return bufferProgressTimePropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Duration> bufferProgressTimePropertyImpl() {
        if (bufferProgressTime == null) {
            bufferProgressTime = new ReadOnlyObjectWrapper<Duration>(this, "bufferProgressTime");
        }
        return bufferProgressTime;
    }
    /**
     * The number of times the media will be played.  By default,
     * <code>cycleCount</code> is set to <code>1</code>
     * meaning the media will only be played once. Setting <code>cycleCount</code>
     * to a value greater than 1 will cause the media to play the given number
     * of times or until stopped. If set to {@link #INDEFINITE INDEFINITE},
     * playback will repeat until stop() or pause() is called.
     *
     * <p>constraints: <code>cycleCount&nbsp;&ge;&nbsp;1</code>
     */
    private IntegerProperty cycleCount;

    /**
     * Sets the cycle count. Its effect will be constrained to
     * <code>[1,{@link Integer#MAX_VALUE}]</code>.
     * Invoking this method will have no effect if media duration is {@link Duration#INDEFINITE}.
     * @param value the cycle count
     */
    public final void setCycleCount(int value) {
        cycleCountProperty().set(value);
    }

    /**
     * Retrieves the cycle count.
     * @return the cycle count.
     */
    public final int getCycleCount() {
        return cycleCount == null ? 1 : cycleCount.get();
    }

    public IntegerProperty cycleCountProperty() {
        if (cycleCount == null) {
            cycleCount = new IntegerPropertyBase(1) {

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "cycleCount";
                }
            };
        }
        return cycleCount;
    }
    /**
     * The number of completed playback cycles. On the first pass,
     * the value should be 0.  On the second pass, the value should be 1 and
     * so on.  It is incremented at the end of each cycle just prior to seeking
     * back to {@link #startTimeProperty startTime}, i.e., when {@link #stopTimeProperty stopTime} or the
     * end of media has been reached.
     */
    private ReadOnlyIntegerWrapper currentCount;


    private void setCurrentCount(int value) {
        currentCountPropertyImpl().set(value);
    }

    /**
     * Retrieves the index of the current cycle.
     * @return the current cycle index
     */
    public final int getCurrentCount() {
        return currentCount == null ? 0 : currentCount.get();
    }

    public ReadOnlyIntegerProperty currentCountProperty() {
        return currentCountPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyIntegerWrapper currentCountPropertyImpl() {
        if (currentCount == null) {
            currentCount = new ReadOnlyIntegerWrapper(this, "currentCount");
        }
        return currentCount;
    }
    /**
     * Whether the player audio is muted. A value of <code>true</code> indicates
     * that audio is <i>not</i> being produced. The value of this property has
     * no effect on {@link #volumeProperty volume}, i.e., if the audio is muted and then
     * un-muted, audio playback will resume at the same audible level provided
     * of course that the <code>volume</code> property has not been modified
     * meanwhile. The default value is <code>false</code>.
     * @see #volume
     */
    private BooleanProperty mute;

    /**
     * Sets the value of {@link #muteProperty}.
     * @param value the <code>mute</code> setting
     */
    public final void setMute (boolean value) {
        muteProperty().set(value);
    }

    /**
     * Retrieves the {@link #muteProperty} value.
     * @return the mute setting
     */
    public final boolean isMute() {
        return mute == null ? false : mute.get();
    }

    public BooleanProperty muteProperty() {
        if (mute == null) {
            mute = new BooleanPropertyBase() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                jfxPlayer.setMute(get());
                            } else {
                                muteChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "mute";
                }
            };
        }
        return mute;
    }

    /**
     * Event handler invoked when the player <code>currentTime</code> reaches a
     * media marker.
     */
    private ObjectProperty<EventHandler<MediaMarkerEvent>> onMarker;

    /**
     * Sets the marker event handler.
     * @param onMarker the marker event handler.
     */
    public final void setOnMarker(EventHandler<MediaMarkerEvent> onMarker) {
        onMarkerProperty().set(onMarker);
    }

    /**
     * Retrieves the marker event handler.
     * @return the marker event handler.
     */
    public final EventHandler<MediaMarkerEvent> getOnMarker() {
        return onMarker == null ? null : onMarker.get();
    }

    public ObjectProperty<EventHandler<MediaMarkerEvent>> onMarkerProperty() {
        if (onMarker == null) {
            onMarker = new SimpleObjectProperty<EventHandler<MediaMarkerEvent>>(this, "onMarker");
        }
        return onMarker;
    }

    void addView(MediaView view) {
        WeakReference<MediaView> vref = new WeakReference<MediaView>(view);
        synchronized (viewRefs) {
            viewRefs.add(vref);
        }
    }

    void removeView(MediaView view) {
        synchronized (viewRefs) {
            for (WeakReference<MediaView> vref : viewRefs) {
                MediaView v = vref.get();
                if (v != null && v.equals(view)) {
                    viewRefs.remove(vref);
                }
            }
        }
    }

    // This function sets the player's error property on the UI thread.
    void handleError(final MediaException error) {
        Platform.runLater(() -> {
            setError(error);

            // Propogate errors that related to media to media object
            if (error.getType() == MediaException.Type.MEDIA_CORRUPTED
                    || error.getType() == MediaException.Type.MEDIA_UNSUPPORTED
                    || error.getType() == MediaException.Type.MEDIA_INACCESSIBLE
                    || error.getType() == MediaException.Type.MEDIA_UNAVAILABLE) {
                media._setError(error.getType(), error.getMessage());
            }
        });
    }

    void createMediaTimer() {
        synchronized (MediaTimerTask.timerLock) {
            if (mediaTimerTask == null) {
                mediaTimerTask = new MediaTimerTask(this);
                mediaTimerTask.start();
            }
            isUpdateTimeEnabled = true;
        }
    }

    void destroyMediaTimer() {
        synchronized (MediaTimerTask.timerLock) {
            if (mediaTimerTask != null) {
                isUpdateTimeEnabled = false;
                mediaTimerTask.stop();
                mediaTimerTask = null;
            }
        }
    }

    // Called periodically to update the currentTime
    void updateTime() {
        if (playerReady && isUpdateTimeEnabled && jfxPlayer != null) {
            double timeSeconds = jfxPlayer.getPresentationTime();
            if (timeSeconds >= 0.0) {
                double newTimeMs = timeSeconds*1000.0;

                if (Double.compare(newTimeMs, prevTimeMs) != 0) {
                    setCurrentTime(Duration.millis(newTimeMs));
                    prevTimeMs = newTimeMs;
                }
            }
        }
    }

    void loopPlayback() {
        seek (getStartTime());
    }

    // handleRequestedChanges() is called to update jfxPlayer's properties once
    // MediaPlayer gets the onReady event from jfxPlayer.  Before onReady, calls to
    // update MediaPlayer's properties to not correspond to calls to update jfxPlayer's
    // properties. Once we get onReady(), we must then go and update all of jfxPlayer's
    // proprties.
    void handleRequestedChanges() {
        if (rateChangeRequested) {
            if (jfxPlayer.getDuration() != Double.POSITIVE_INFINITY) {
                jfxPlayer.setRate((float)clamp(getRate(), RATE_MIN, RATE_MAX));
            }
            rateChangeRequested = false;
        }

        if (volumeChangeRequested) {
            jfxPlayer.setVolume((float)clamp(getVolume(), 0.0, 1.0));
            volumeChangeRequested = false;
        }

        if (balanceChangeRequested) {
            jfxPlayer.setBalance((float)clamp(getBalance(), -1.0, 1.0));
            balanceChangeRequested = false;
        }

        if (startTimeChangeRequested || stopTimeChangeRequested) {
            setStartStopTimes(getStartTime(), startTimeChangeRequested, getStopTime(), stopTimeChangeRequested);
            startTimeChangeRequested = stopTimeChangeRequested = false;
        }

        if (muteChangeRequested) {
            jfxPlayer.setMute(isMute());
            muteChangeRequested = false;
        }

        if (audioSpectrumNumBandsChangeRequested) {
            jfxPlayer.getAudioSpectrum().setBandCount(clamp(getAudioSpectrumNumBands(), AUDIOSPECTRUM_NUMBANDS_MIN, Integer.MAX_VALUE));
            audioSpectrumNumBandsChangeRequested = false;
        }

        if (audioSpectrumIntervalChangeRequested) {
            jfxPlayer.getAudioSpectrum().setInterval(clamp(getAudioSpectrumInterval(), AUDIOSPECTRUM_INTERVAL_MIN, Double.MAX_VALUE));
            audioSpectrumIntervalChangeRequested = false;
        }

        if (audioSpectrumThresholdChangeRequested) {
            jfxPlayer.getAudioSpectrum().setSensitivityThreshold(clamp(getAudioSpectrumThreshold(), Integer.MIN_VALUE, AUDIOSPECTRUM_THRESHOLD_MAX));
            audioSpectrumThresholdChangeRequested = false;
        }

        if (audioSpectrumEnabledChangeRequested) {
            boolean enabled = (getAudioSpectrumListener() != null);
            jfxPlayer.getAudioSpectrum().setEnabled(enabled);
            audioSpectrumEnabledChangeRequested = false;
        }

        if (playRequested) {
            jfxPlayer.play();
            playRequested = false;
        }
    }

    // ************************************************************************************************
    // ********* Player event-handling
    // ************************************************************************************************

    void preReady() {
        // Notify MediaView that we ready
        synchronized (viewRefs) {
            for (WeakReference<MediaView> vref : viewRefs) {
                MediaView v = vref.get();
                if (v != null) {
                    v._mediaPlayerOnReady();
                }
            }
        }

        // Update AudioEqaualizer if needed
        if (audioEqualizer != null) {
            audioEqualizer.setAudioEqualizer(jfxPlayer.getEqualizer());
        }

        // Update duration
        double durationSeconds = jfxPlayer.getDuration();
        Duration duration;
        if (durationSeconds >= 0.0 && !Double.isNaN(durationSeconds)) {
            duration = Duration.millis(durationSeconds * 1000.0);
        } else {
            duration = Duration.UNKNOWN;
        }

        playerReady = true;

        media.setDuration(duration);
        media._updateMedia(jfxPlayer.getMedia());

        // **** Sync up the player with the desired properties if they were called
        //      before onReady()
        handleRequestedChanges();

        // update cycle/total durations
        calculateCycleDuration();

        // Set BufferProgressTime
        if (lastBufferEvent != null && duration.toMillis() > 0.0) {
            double position = lastBufferEvent.getBufferPosition();
            double stop = lastBufferEvent.getBufferStop();
            final double bufferedTime = position / stop * duration.toMillis();
            lastBufferEvent = null;
            setBufferProgressTime(Duration.millis(bufferedTime));
        }

        setStatus(Status.READY);
    }
    /**
     * Event handler invoked when the player <code>currentTime</code> reaches
     * <code>stopTime</code>.
     */
    private ObjectProperty<Runnable> onEndOfMedia;

    /**
     * Sets the end of media event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnEndOfMedia(Runnable value) {
        onEndOfMediaProperty().set(value);
    }

    /**
     * Retrieves the end of media event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnEndOfMedia() {
        return onEndOfMedia == null ? null : onEndOfMedia.get();
    }

    public ObjectProperty<Runnable> onEndOfMediaProperty() {
        if (onEndOfMedia == null) {
            onEndOfMedia = new SimpleObjectProperty<Runnable>(this, "onEndOfMedia");
        }
        return onEndOfMedia;
    }

    /**
     * Event handler invoked when the status changes to
     * <code>READY</code>.
     */
    private ObjectProperty<Runnable> onReady; // Player is ready and media has prerolled

    /**
     * Sets the {@link Status#READY} event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnReady(Runnable value) {
        onReadyProperty().set(value);
    }

    /**
     * Retrieves the {@link Status#READY} event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnReady() {
        return onReady == null ? null : onReady.get();
    }

    public ObjectProperty<Runnable> onReadyProperty() {
        if (onReady == null) {
            onReady = new SimpleObjectProperty<Runnable>(this, "onReady");
        }
        return onReady;
    }

    /**
     * Event handler invoked when the status changes to
     * <code>PLAYING</code>.
     */
    private ObjectProperty<Runnable> onPlaying; // Media has reached its end.

    /**
     * Sets the {@link Status#PLAYING} event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnPlaying(Runnable value) {
        onPlayingProperty().set(value);
    }

    /**
     * Retrieves the {@link Status#PLAYING} event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnPlaying() {
        return onPlaying == null ? null : onPlaying.get();
    }

    public ObjectProperty<Runnable> onPlayingProperty() {
        if (onPlaying == null) {
            onPlaying = new SimpleObjectProperty<Runnable>(this, "onPlaying");
        }
        return onPlaying;
    }

    /**
     * Event handler invoked when the status changes to <code>PAUSED</code>.
     */
    private ObjectProperty<Runnable> onPaused; // Media has reached its end.

    /**
     * Sets the {@link Status#PAUSED} event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnPaused(Runnable value) {
        onPausedProperty().set(value);
    }

    /**
     * Retrieves the {@link Status#PAUSED} event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnPaused() {
        return onPaused == null ? null : onPaused.get();
    }

    public ObjectProperty<Runnable> onPausedProperty() {
        if (onPaused == null) {
            onPaused = new SimpleObjectProperty<Runnable>(this, "onPaused");
        }
        return onPaused;
    }

    /**
     * Event handler invoked when the status changes to
     * <code>STOPPED</code>.
     */
    private ObjectProperty<Runnable> onStopped; // Media has reached its end.

    /**
     * Sets the {@link Status#STOPPED} event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnStopped(Runnable value) {
        onStoppedProperty().set(value);
    }

    /**
     * Retrieves the {@link Status#STOPPED} event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnStopped() {
        return onStopped == null ? null : onStopped.get();
    }

    public ObjectProperty<Runnable> onStoppedProperty() {
        if (onStopped == null) {
            onStopped = new SimpleObjectProperty<Runnable>(this, "onStopped");
        }
        return onStopped;
    }

    /**
     * Event handler invoked when the status changes to <code>HALTED</code>.
     */
    private ObjectProperty<Runnable> onHalted; // Media caught an irrecoverable error.

    /**
     * Sets the {@link Status#HALTED} event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnHalted(Runnable value) {
        onHaltedProperty().set(value);
    }

    /**
     * Retrieves the {@link Status#HALTED} event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnHalted() {
        return onHalted == null ? null : onHalted.get();
    }

    public ObjectProperty<Runnable> onHaltedProperty() {
        if (onHalted == null) {
            onHalted = new SimpleObjectProperty<Runnable>(this, "onHalted");
        }
        return onHalted;
    }
    /**
     * Event handler invoked when the player <code>currentTime</code> reaches
     * <code>stopTime</code> and <i>will be</i> repeating. This callback is made
     * prior to seeking back to <code>startTime</code>.
     *
     * @see cycleCount
     */
    private ObjectProperty<Runnable> onRepeat;

    /**
     * Sets the repeat event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnRepeat(Runnable value) {
        onRepeatProperty().set(value);
    }

    /**
     * Retrieves the repeat event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnRepeat() {
        return onRepeat == null ? null : onRepeat.get();
    }

    public ObjectProperty<Runnable> onRepeatProperty() {
        if (onRepeat == null) {
            onRepeat = new SimpleObjectProperty<Runnable>(this, "onRepeat");
        }
        return onRepeat;
    }

    /**
     * Event handler invoked when the status changes to
     * <code>STALLED</code>.
     */
    private ObjectProperty<Runnable> onStalled;

    /**
     * Sets the {@link Status#STALLED} event handler.
     * @param value the event handler or <code>null</code>.
     */
    public final void setOnStalled(Runnable value) {
        onStalledProperty().set(value);
    }

    /**
     * Retrieves the {@link Status#STALLED} event handler.
     * @return the event handler or <code>null</code>.
     */
    public final Runnable getOnStalled() {
        return onStalled == null ? null : onStalled.get();
    }

    public ObjectProperty<Runnable> onStalledProperty() {
        if (onStalled == null) {
            onStalled = new SimpleObjectProperty<Runnable>(this, "onStalled");
        }
        return onStalled;
    }

    /* **************************************************************************
     * AudioSpectrum API
     ***************************************************************************/

    /**
     * The number of bands in the audio spectrum. The default value is 128; minimum
     * is 2. The frequency range of the audio signal will be divided into the
     * specified number of frequency bins. For example, a typical digital music
     * signal has a frequency range of <code>[0.0,&nbsp;22050]</code> Hz. If the
     * number of spectral bands were in this case set to 10, the width of each
     * frequency bin in the spectrum would be <code>2205</code> Hz with the
     * lower bound of the lowest frequency bin equal to <code>0.0</code>.
     */
    private IntegerProperty audioSpectrumNumBands;

    /**
     * Sets the number of bands in the audio spectrum.
     * @param value the number of spectral bands; <code>value</code>must be &ge; 2
     */
    public final void setAudioSpectrumNumBands(int value) {
        audioSpectrumNumBandsProperty().setValue(value);
    }

    /**
     * Retrieves the number of bands in the audio spectrum.
     * @return the number of spectral bands.
     */
    public final int getAudioSpectrumNumBands() {
        return audioSpectrumNumBandsProperty().getValue();
    }

    public IntegerProperty audioSpectrumNumBandsProperty() {
        if (audioSpectrumNumBands == null) {
            audioSpectrumNumBands = new IntegerPropertyBase(DEFAULT_SPECTRUM_BAND_COUNT) {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                jfxPlayer.getAudioSpectrum().setBandCount(clamp(audioSpectrumNumBands.get(), AUDIOSPECTRUM_NUMBANDS_MIN, Integer.MAX_VALUE));
                            } else {
                                audioSpectrumNumBandsChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "audioSpectrumNumBands";
                }
            };
        }
        return audioSpectrumNumBands;
    }

    /**
     * The interval between spectrum updates in seconds. The default is
     * <code>0.1</code> seconds.
     */
    private DoubleProperty audioSpectrumInterval;

    /**
     * Sets the value of the audio spectrum notification interval in seconds.
     * @param value a positive value specifying the spectral update interval
     */
    public final void setAudioSpectrumInterval(double value) {
        audioSpectrumIntervalProperty().set(value);
    }

    /**
     * Retrieves the value of the audio spectrum notification interval in seconds.
     * @return the spectral update interval
     */
    public final double getAudioSpectrumInterval() {
        return audioSpectrumIntervalProperty().get();
    }

    public DoubleProperty audioSpectrumIntervalProperty() {
        if (audioSpectrumInterval == null) {
            audioSpectrumInterval = new DoublePropertyBase(DEFAULT_SPECTRUM_INTERVAL) {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                jfxPlayer.getAudioSpectrum().setInterval(clamp(audioSpectrumInterval.get(), AUDIOSPECTRUM_INTERVAL_MIN, Double.MAX_VALUE));
                            } else {
                                audioSpectrumIntervalChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "audioSpectrumInterval";
                }
            };
        }
        return audioSpectrumInterval;
    }

    /**
     * The sensitivity threshold in decibels; must be non-positive. Values below
     * this threshold with respect to the peak frequency in the given spectral
     * band will be set to the value of the threshold. The default value is
     * -60 dB.
     */
    private IntegerProperty audioSpectrumThreshold;

    /**
     * Sets the audio spectrum threshold in decibels.
     * @param value the spectral threshold in dB; must be &le; <code>0</code>.
     */
    public final void setAudioSpectrumThreshold(int value) {
        audioSpectrumThresholdProperty().set(value);
    }

    /**
     * Retrieves the audio spectrum threshold in decibels.
     * @return the spectral threshold in dB
     */
    public final int getAudioSpectrumThreshold() {
        return audioSpectrumThresholdProperty().get();
    }

    public IntegerProperty audioSpectrumThresholdProperty() {
        if (audioSpectrumThreshold == null) {
            audioSpectrumThreshold = new IntegerPropertyBase(DEFAULT_SPECTRUM_THRESHOLD) {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                jfxPlayer.getAudioSpectrum().setSensitivityThreshold(clamp(audioSpectrumThreshold.get(), Integer.MIN_VALUE, AUDIOSPECTRUM_THRESHOLD_MAX));
                            } else {
                                audioSpectrumThresholdChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "audioSpectrumThreshold";
                }
            };
        }
        return audioSpectrumThreshold;
    }

    /**
     * A listener for audio spectrum updates. When the listener is registered,
     * audio spectrum computation is enabled; upon removing the listener,
     * computation is disabled. Only a single listener may be registered, so if
     * multiple observers are required, events must be forwarded.
     *
     * <p>An <code>AudioSpectrumListener</code> may be useful for example to
     * plot the frequency spectrum of the audio being played or to generate
     * waveforms for a music visualizer.
     */
    private ObjectProperty<AudioSpectrumListener> audioSpectrumListener;

    /**
     * Sets the listener of the audio spectrum.
     * @param listener the spectral listener or <code>null</code>.
     */
    public final void setAudioSpectrumListener(AudioSpectrumListener listener) {
        audioSpectrumListenerProperty().set(listener);
    }

    /**
     * Retrieves the listener of the audio spectrum.
     * @return the spectral listener or <code>null</code>
     */
    public final AudioSpectrumListener getAudioSpectrumListener() {
        return audioSpectrumListenerProperty().get();
    }

    public ObjectProperty<AudioSpectrumListener> audioSpectrumListenerProperty() {
        if (audioSpectrumListener == null) {
            audioSpectrumListener = new ObjectPropertyBase<AudioSpectrumListener>() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (getStatus() != Status.DISPOSED) {
                            if (playerReady) {
                                boolean enabled = (audioSpectrumListener.get() != null);
                                jfxPlayer.getAudioSpectrum().setEnabled(enabled);
                            } else {
                                audioSpectrumEnabledChangeRequested = true;
                            }
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return MediaPlayer.this;
                }

                @Override
                public String getName() {
                    return "audioSpectrumListener";
                }
            };
        }
        return audioSpectrumListener;
    }

    /**
     * Free all resources associated with player. Player SHOULD NOT be used after this function is called.
     * Player will transition to {@link Status#DISPOSED} after this method is done. This method can be
     * called anytime regardless of current player status.
     * @since JavaFX 8.0
     */
    public synchronized void dispose() {
        synchronized (disposeLock) {
            setStatus(Status.DISPOSED);

            destroyMediaTimer();

            if (audioEqualizer != null) {
                audioEqualizer.setAudioEqualizer(null);
                audioEqualizer = null;
            }

            if (jfxPlayer != null) {
                jfxPlayer.dispose();
                synchronized (renderLock) {
                    if (rendererListener != null) {
                        Toolkit.getToolkit().removeStageTkPulseListener(rendererListener);
                        rendererListener = null;
                    }
                }
                jfxPlayer = null;
            }
        }
    }

    /* **************************************************************************
     * Listeners section
     ***************************************************************************
     * Listener of modifications to the marker map in the public Media API.
     * Changes to this map are propagated to the implementation layer.
     */
    private class MarkerMapChangeListener implements MapChangeListener<String, Duration> {
        @Override
        public void onChanged(Change<? extends String, ? extends Duration> change) {
            synchronized (disposeLock) {
                if (getStatus() != Status.DISPOSED) {
                    String key = change.getKey();
                    // Reject null-named markers.
                    if (key == null) {
                        return;
                    }
                    com.sun.media.jfxmedia.Media jfxMedia = jfxPlayer.getMedia();
                    if (change.wasAdded()) {
                        if (change.wasRemoved()) {
                            // The remove and add marker calls eventually go to native code
                            // so we can't depend on the Java Map behavior or replacing a
                            // key-value pair when the key is already in the Map. Instead we
                            // explicitly remove the old entry and add the new one.
                            jfxMedia.removeMarker(key);
                        }
                        Duration value = change.getValueAdded();
                        // Reject null- or negative-valued marker times.
                        if (value != null && value.greaterThanOrEqualTo(Duration.ZERO)) {
                            jfxMedia.addMarker(key, change.getValueAdded().toMillis() / 1000.0);
                        }
                    } else if (change.wasRemoved()) {
                        jfxMedia.removeMarker(key);
                    }
                }
            }
        }
    }

    /**
     * Listener of marker events emitted by the implementation layer. The
     * CURRENT_MARKER property is updated to the most recently received event.
     */
    private class _MarkerListener implements MarkerListener {

        @Override
        public void onMarker(final MarkerEvent evt) {
            Platform.runLater(() -> {
                Duration markerTime = Duration.millis(evt.getPresentationTime() * 1000.0);
                if (getOnMarker() != null) {
                    getOnMarker().handle(new MediaMarkerEvent(new Pair<String, Duration>(evt.getMarkerName(), markerTime)));
                }
            });
        }
    }

    private class _PlayerStateListener implements PlayerStateListener {
        @Override
        public void onReady(PlayerStateEvent evt) {
            //System.out.println("** MediaPlayerFX received onReady!");
            Platform.runLater(() -> {
                synchronized (disposeLock) {
                    if (getStatus() == Status.DISPOSED) {
                        return;
                    }

                    preReady();
                }
            });
        }

        @Override
        public void onPlaying(PlayerStateEvent evt) {
            //System.err.println("** MediaPlayerFX received onPlaying!");
            startTimeAtStop = null;

            Platform.runLater(() -> {
                createMediaTimer();
                setStatus(Status.PLAYING);
            });
        }

        @Override
        public void onPause(PlayerStateEvent evt) {
            //System.err.println("** MediaPlayerFX received onPause!");

            Platform.runLater(() -> {
                // Disable updating currentTime.
                isUpdateTimeEnabled = false;

                setStatus(Status.PAUSED);
            });

            if (startTimeAtStop != null && startTimeAtStop != getStartTime()) {
                startTimeAtStop = null;
                Platform.runLater(() -> {
                    setCurrentTime(getStartTime());
                });
            }
        }

        @Override
        public void onStop(PlayerStateEvent evt) {
            //System.err.println("** MediaPlayerFX received onStop!");
            Platform.runLater(() -> {
                // Destroy media time and update current time
                destroyMediaTimer();
                startTimeAtStop = getStartTime();
                setCurrentTime(getStartTime());
                setStatus(Status.STOPPED);
            });
        }

        @Override
        public void onStall(PlayerStateEvent evt) {
            //System.err.println("** MediaPlayerFX received onStall!");
            Platform.runLater(() -> {
                // Disable updating currentTime.
                isUpdateTimeEnabled = false;

                setStatus(Status.STALLED);
            });
        }

        void handleFinish() {
            //System.err.println("** MediaPlayerFX handleFinish");

            // Increment number of times media has played.
            setCurrentCount(getCurrentCount() + 1);

            // Rewind and play from the beginning if the number
            // of repeats has yet to be reached.
            if ((getCurrentCount() < getCycleCount()) || (getCycleCount() == INDEFINITE)) {
                if (getOnEndOfMedia() != null) {
                     Platform.runLater(getOnEndOfMedia());
                }

                loopPlayback();

                if (getOnRepeat() != null) {
                    Platform.runLater(getOnRepeat());
                }
            } else {
                // Player status remains PLAYING.

                // Disable updating currentTime.
                isUpdateTimeEnabled = false;

                // Set current rate to zero.
                setCurrentRate(0.0);

                // Set EOS flag
                isEOS = true;

                if (getOnEndOfMedia() != null) {
                    Platform.runLater(getOnEndOfMedia());
                }
            }
        }

        @Override
        public void onFinish(PlayerStateEvent evt) {
            //System.err.println("** MediaPlayerFX received onFinish!");
            startTimeAtStop = null;

            Platform.runLater(() -> {
                handleFinish();
            });
        }

        @Override
        public void onHalt(final PlayerStateEvent evt) {
            Platform.runLater(() -> {
                setStatus(Status.HALTED);
                handleError(MediaException.haltException(evt.getMessage()));

                // Disable updating currentTime.
                isUpdateTimeEnabled = false;
            });
        }
    }

    private class _PlayerTimeListener implements PlayerTimeListener {
        double theDuration;

        void handleDurationChanged() {
            media.setDuration(Duration.millis(theDuration * 1000.0));
        }

        @Override
        public void onDurationChanged(final double duration) {
            //System.err.println("** MediaPlayerFX received onDurationChanged!");
            Platform.runLater(() -> {
                theDuration = duration;
                handleDurationChanged();
            });
        }
    }

    private class _VideoTrackSizeListener implements VideoTrackSizeListener {
        int trackWidth;
        int trackHeight;

        @Override
        public void onSizeChanged(final int width, final int height) {
            Platform.runLater(() -> {
                if (media != null) {
                    trackWidth = width;
                    trackHeight = height;
                    setSize();
                }
            });
        }

        void setSize() {
            media.setWidth(trackWidth);
            media.setHeight(trackHeight);

            synchronized (viewRefs) {
                for (WeakReference<MediaView> vref : viewRefs) {
                    MediaView v = vref.get();
                    if (v != null) {
                        v.notifyMediaSizeChange();
                    }
                }
            }
        }
    }

    private class _MediaErrorListener implements com.sun.media.jfxmedia.events.MediaErrorListener {
        @Override
        public void onError(Object source, int errorCode, String message) {
            MediaException error = MediaException.getMediaException(source, errorCode, message);

            handleError(error);
        }
    }

    private class _BufferListener implements BufferListener {
        double bufferedTime; // time in ms

        @Override
        public void onBufferProgress(BufferProgressEvent evt) {
            if (media != null) {
                if (evt.getDuration() > 0.0) {
                    double position = evt.getBufferPosition();  //Must assign.  I don't know how to convert integer to number otherwise.
                    double stop = evt.getBufferStop();
                    bufferedTime = position/stop * evt.getDuration()*1000.0;
                    lastBufferEvent = null;

                    Platform.runLater(() -> {
                         setBufferProgressTime(Duration.millis(bufferedTime));
                    });
                } else {
                    lastBufferEvent = evt;
                }
            }
        }
    }

    private class _SpectrumListener implements com.sun.media.jfxmedia.events.AudioSpectrumListener {
        private float[] magnitudes;
        private float[] phases;

        @Override public void onAudioSpectrumEvent(final AudioSpectrumEvent evt) {
            Platform.runLater(() -> {
                AudioSpectrumListener listener = getAudioSpectrumListener();
                if (listener != null) {
                    listener.spectrumDataUpdate(evt.getTimestamp(),
                            evt.getDuration(),
                            magnitudes = evt.getSource().getMagnitudes(magnitudes),
                            phases = evt.getSource().getPhases(phases));
                }
            });
        }
    }

    private final Object renderLock = new Object();
    private VideoDataBuffer currentRenderFrame;
    private VideoDataBuffer nextRenderFrame;

    // NGMediaView will call this to get the frame to render
    /**
     * WARNING: You must call releaseFrame() on the returned frame when you are
     * finished with it or a massive memory leak will occur.
     *
     * @return the current frame to be used for rendering, or null if not in a render cycle
     */
    VideoDataBuffer getLatestFrame() {
        synchronized (renderLock) {
            if (null != currentRenderFrame) {
                currentRenderFrame.holdFrame();
            }
            return currentRenderFrame;
        }
    }

    private class RendererListener implements
            com.sun.media.jfxmedia.events.VideoRendererListener,
            TKPulseListener
    {
        boolean updateMediaViews;

        @Override
        public void videoFrameUpdated(NewFrameEvent nfe) {
            VideoDataBuffer vdb = nfe.getFrameData();
            if (null != vdb) {

                Duration frameTS = new Duration(vdb.getTimestamp() * 1000);
                Duration stopTime = getStopTime();
                if (frameTS.greaterThanOrEqualTo(getStartTime()) && (stopTime.isUnknown() || frameTS.lessThanOrEqualTo(stopTime))) {
                    updateMediaViews = true;

                    synchronized (renderLock) {
                        vdb.holdFrame();

                        // currentRenderFrame must not be touched, queue this one for later
                        if (null != nextRenderFrame) {
                            nextRenderFrame.releaseFrame();
                        }
                        nextRenderFrame = vdb;
                    }
                    // make sure we get the next pulse so we can update our textures
                    Toolkit.getToolkit().requestNextPulse();
                } else {
                    vdb.releaseFrame();
                }
            }
        }

        @Override
        public void releaseVideoFrames() {
            synchronized (renderLock) {
                if (null != currentRenderFrame) {
                    currentRenderFrame.releaseFrame();
                    currentRenderFrame = null;
                }

                if (null != nextRenderFrame) {
                    nextRenderFrame.releaseFrame();
                    nextRenderFrame = null;
                }
            }
        }

        @Override
        public void pulse() {
            if (updateMediaViews) {
                updateMediaViews = false;

                /* swap in the next frame if there is one
                 * this should be done exactly once per render cycle so that all
                 * views display the same image.
                 */
                synchronized (renderLock) {
                    if (null != nextRenderFrame) {
                        if (null != currentRenderFrame) {
                            currentRenderFrame.releaseFrame();
                        }
                        currentRenderFrame = nextRenderFrame;
                        nextRenderFrame = null;
                    }
                }

                // tell all media views that their content needs to be redrawn
                synchronized (viewRefs) {
                    Iterator<WeakReference<MediaView>> iter = viewRefs.iterator();
                    while (iter.hasNext()) {
                        MediaView view = iter.next().get();
                        if (null != view) {
                            view.notifyMediaFrameUpdated();
                        } else {
                            iter.remove();
                        }
                    }
                }
            }
        }
    }
}

class MediaPlayerShutdownHook implements Runnable {

    private final static List<WeakReference<MediaPlayer>> playerRefs = new ArrayList<WeakReference<MediaPlayer>>();
    private static boolean isShutdown = false;

    static {
        Toolkit.getToolkit().addShutdownHook(new MediaPlayerShutdownHook());
    }

    public static void addMediaPlayer(MediaPlayer player) {
        synchronized (playerRefs) {
            if (isShutdown) {
                com.sun.media.jfxmedia.MediaPlayer jfxPlayer = player.retrieveJfxPlayer();
                if (jfxPlayer != null) {
                    jfxPlayer.dispose();
                }
            } else {
                for (ListIterator<WeakReference<MediaPlayer>> it = playerRefs.listIterator(); it.hasNext();) {
                    MediaPlayer l = it.next().get();
                    if (l == null) {
                        it.remove();
                    }
                }

                playerRefs.add(new WeakReference<MediaPlayer>(player));
            }
        }
    }

    @Override
    public void run() {
        synchronized (playerRefs) {
            for (ListIterator<WeakReference<MediaPlayer>> it = playerRefs.listIterator(); it.hasNext();) {
                MediaPlayer player = it.next().get();
                if (player != null) {
                    player.destroyMediaTimer();
                    com.sun.media.jfxmedia.MediaPlayer jfxPlayer = player.retrieveJfxPlayer();
                    if (jfxPlayer != null) {
                        jfxPlayer.dispose();
                    }
                } else {
                    it.remove();
                }
            }

            isShutdown = true;
        }
    }
}

class MediaTimerTask extends TimerTask {

    private Timer mediaTimer = null;
    static final Object timerLock = new Object();
    private WeakReference<MediaPlayer> playerRef;

    MediaTimerTask(MediaPlayer player) {
        playerRef = new WeakReference<MediaPlayer>(player);
    }

    void start() {
        if (mediaTimer == null) {
            mediaTimer = new Timer(true);
            mediaTimer.scheduleAtFixedRate(this, 0, 100 /* period ms*/);
        }
    }

    void stop() {
        if (mediaTimer != null) {
            mediaTimer.cancel();
            mediaTimer = null;
        }
    }

    @Override
    public void run() {
        synchronized (timerLock) {
            final MediaPlayer player = playerRef.get();
            if (player != null) {

                Platform.runLater(() -> {
                    synchronized (timerLock) {
                        player.updateTime();
                    }
                });
            } else {
                cancel();
            }
        }
    }
}
