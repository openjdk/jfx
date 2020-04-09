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

package com.sun.media.jfxmediaimpl;

import java.lang.annotation.Native;
import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.control.VideoRenderControl;
import com.sun.media.jfxmedia.effects.AudioEqualizer;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.events.AudioSpectrumEvent;
import com.sun.media.jfxmedia.events.AudioSpectrumListener;
import com.sun.media.jfxmedia.events.BufferListener;
import com.sun.media.jfxmedia.events.BufferProgressEvent;
import com.sun.media.jfxmedia.events.MarkerEvent;
import com.sun.media.jfxmedia.events.MarkerListener;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.events.NewFrameEvent;
import com.sun.media.jfxmedia.events.PlayerEvent;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import com.sun.media.jfxmedia.events.PlayerStateEvent.PlayerState;
import com.sun.media.jfxmedia.events.PlayerStateListener;
import com.sun.media.jfxmedia.events.PlayerTimeListener;
import com.sun.media.jfxmedia.events.VideoFrameRateListener;
import com.sun.media.jfxmedia.events.VideoRendererListener;
import com.sun.media.jfxmedia.events.VideoTrackSizeListener;
import com.sun.media.jfxmedia.logging.Logger;
import com.sun.media.jfxmedia.track.AudioTrack;
import com.sun.media.jfxmedia.track.SubtitleTrack;
import com.sun.media.jfxmedia.track.Track;
import com.sun.media.jfxmedia.track.Track.Encoding;
import com.sun.media.jfxmedia.track.VideoResolution;
import com.sun.media.jfxmedia.track.VideoTrack;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base implementation of a
 * <code>MediaPlayer</code>.
 */
public abstract class NativeMediaPlayer implements MediaPlayer, MarkerStateListener {
    //***** Event IDs for PlayerStateEvent.  IDs sent from native JNI layer.

    @Native public final static int eventPlayerUnknown = 100;
    @Native public final static int eventPlayerReady = 101;
    @Native public final static int eventPlayerPlaying = 102;
    @Native public final static int eventPlayerPaused = 103;
    @Native public final static int eventPlayerStopped = 104;
    @Native public final static int eventPlayerStalled = 105;
    @Native public final static int eventPlayerFinished = 106;
    @Native public final static int eventPlayerError = 107;
    // Nominal video frames per second.
    @Native private static final int NOMINAL_VIDEO_FPS = 30;
    // Nanoseconds per second.
    @Native public static final long ONE_SECOND = 1000000000L;

    /**
     * The
     * <code>Media</code> corresponding to the media source.
     */
    private NativeMedia media;
    private VideoRenderControl videoRenderControl;
    private final List<WeakReference<MediaErrorListener>> errorListeners = new ArrayList<>();
    private final List<WeakReference<PlayerStateListener>> playerStateListeners = new ArrayList<>();
    private final List<WeakReference<PlayerTimeListener>> playerTimeListeners = new ArrayList<>();
    private final List<WeakReference<VideoTrackSizeListener>> videoTrackSizeListeners = new ArrayList<>();
    private final List<WeakReference<VideoRendererListener>> videoUpdateListeners = new ArrayList<>();
    private final List<WeakReference<VideoFrameRateListener>> videoFrameRateListeners = new ArrayList<>();
    private final List<WeakReference<MarkerListener>> markerListeners = new ArrayList<>();
    private final List<WeakReference<BufferListener>> bufferListeners = new ArrayList<>();
    private final List<WeakReference<AudioSpectrumListener>> audioSpectrumListeners = new ArrayList<>();
    private final List<PlayerStateEvent> cachedStateEvents = new ArrayList<>();
    private final List<PlayerTimeEvent> cachedTimeEvents = new ArrayList<>();
    private final List<BufferProgressEvent> cachedBufferEvents = new ArrayList<>();
    private final List<MediaErrorEvent> cachedErrorEvents = new ArrayList<>();
    private boolean isFirstFrame = true;
    private NewFrameEvent firstFrameEvent = null;
    private double firstFrameTime;
    private final Object firstFrameLock = new Object();
    private EventQueueThread eventLoop = new EventQueueThread();
    private int frameWidth = -1;
    private int frameHeight = -1;
    private final AtomicBoolean isMediaPulseEnabled = new AtomicBoolean(false);
    private final Lock mediaPulseLock = new ReentrantLock();
    private Timer mediaPulseTimer;
    private final Lock markerLock = new ReentrantLock();
    private boolean checkSeek = false;
    private double timeBeforeSeek = 0.0;
    private double timeAfterSeek = 0.0;
    private double previousTime = 0.0;
    private double firedMarkerTime = -1.0;
    private double startTime = 0.0;
    private double stopTime = Double.POSITIVE_INFINITY;
    private boolean isStartTimeUpdated = false;
    private boolean isStopTimeSet = false;

    // --- Begin decoded frame rate fields
    private double encodedFrameRate = 0.0;
    private boolean recomputeFrameRate = true;
    private double previousFrameTime;
    private long numFramesSincePlaying;
    private double meanFrameDuration;
    private double decodedFrameRate;
    // --- End decoded frame rate fields
    private PlayerState playerState = PlayerState.UNKNOWN;
    private final Lock disposeLock = new ReentrantLock();
    private boolean isDisposed = false;
    private Runnable onDispose;

    //**************************************************************************
    //***** Constructors
    //**************************************************************************
    /**
     * Construct a NativeMediaPlayer for the referenced clip.
     *
     * @param clip Media object
     * @throws IllegalArgumentException if
     * <code>clip</code> is
     * <code>null</code>.
     */
    protected NativeMediaPlayer(NativeMedia clip) {
        if (clip == null) {
            throw new IllegalArgumentException("clip == null!");
        }
        media = clip;
        videoRenderControl = new VideoRenderer();
    }

    /**
     * Initialization method which must be called after construction to
     * initialize the internal state of the player. This method should be
     * invoked directly after the player is constructed.
     */
    protected void init() {
        media.addMarkerStateListener(this);
        eventLoop.start();
    }

    /**
     * Set a callback to invoke when the player is disposed.
     *
     * @param onDispose object on which to invoke {@link Runnable#run()} in
     * {@link #dispose()}.
     */
    void setOnDispose(Runnable onDispose) {
        disposeLock.lock();
        try {
            if (!isDisposed) {
                this.onDispose = onDispose;
            }
        } finally {
            disposeLock.unlock();
        }
    }

    /**
     * Event to be posted to any registered {@link MediaErrorListener}s.
     */
    private static class WarningEvent extends PlayerEvent {

        private final Object source;
        private final String message;

        WarningEvent(Object source, String message) {
            this.source = source;
            this.message = message;
        }

        public Object getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Event to be posted to any registered (@link MediaErrorListener)s
     */
    public static class MediaErrorEvent extends PlayerEvent {

        private final Object source;
        private final MediaError error;

        public MediaErrorEvent(Object source, MediaError error) {
            this.source = source;
            this.error = error;
        }

        public Object getSource() {
            return source;
        }

        public String getMessage() {
            return error.description();
        }

        public int getErrorCode() {
            return error.code();
        }
    }

    private static class PlayerTimeEvent extends PlayerEvent {

        private final double time;

        public PlayerTimeEvent(double time) {
            this.time = time;
        }

        public double getTime() {
            return time;
        }
    }

    /**
     * Event to be posted to any registered {@link PlayerStateListener}s.
     */
    private static class TrackEvent extends PlayerEvent {

        private final Track track;

        TrackEvent(Track track) {
            this.track = track;
        }

        public Track getTrack() {
            return this.track;
        }
    }

    /**
     * Event to be posted to any registered {@link VideoTrackSizeListener}s.
     */
    private static class FrameSizeChangedEvent extends PlayerEvent {

        private final int width;
        private final int height;

        public FrameSizeChangedEvent(int width, int height) {
            if (width > 0) {
                this.width = width;
            } else {
                this.width = 0;
            }

            if (height > 0) {
                this.height = height;
            } else {
                this.height = 0;
            }
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    /**
     * Helper class which managers {@link VideoRendererListener}s. This allows
     * any registered listeners, specifically AWT and Prism, to receive video
     * frames.
     */
    private class VideoRenderer implements VideoRenderControl {

        /**
         * adds the listener to the player's videoUpdate. The listener will be
         * called whenever a new frame of video is ready to be painted or
         * fetched by getData()
         *
         * @param listener the object which provides the VideoUpdateListener
         * callback interface
         */
        @Override
        public void addVideoRendererListener(VideoRendererListener listener) {
            if (listener != null) {
                synchronized (firstFrameLock) {
                    // If the first frame is cached, post it to the listener
                    // directly. The lock is obtained first so the cached
                    // frame is not cleared between the non-null test and
                    // posting the event.
                    if (firstFrameEvent != null) {
                        listener.videoFrameUpdated(firstFrameEvent);
                    }
                }
                videoUpdateListeners.add(new WeakReference<>(listener));
            }
        }

        /**
         * removes the listener from the player.
         *
         * @param listener to be removed from the player
         */
        @Override
        public void removeVideoRendererListener(VideoRendererListener listener) {
            if (listener != null) {
                for (ListIterator<WeakReference<VideoRendererListener>> it = videoUpdateListeners.listIterator(); it.hasNext();) {
                    VideoRendererListener l = it.next().get();
                    if (l == null || l == listener) {
                        it.remove();
                    }
                }
            }
        }

        @Override
        public void addVideoFrameRateListener(VideoFrameRateListener listener) {
            if (listener != null) {
                videoFrameRateListeners.add(new WeakReference<>(listener));
            }
        }

        @Override
        public void removeVideoFrameRateListener(VideoFrameRateListener listener) {
            if (listener != null) {
                for (ListIterator<WeakReference<VideoFrameRateListener>> it = videoFrameRateListeners.listIterator(); it.hasNext();) {
                    VideoFrameRateListener l = it.next().get();
                    if (l == null || l == listener) {
                        it.remove();
                    }
                }
            }
        }

        @Override
        public int getFrameWidth() {
            return frameWidth;
        }

        @Override
        public int getFrameHeight() {
            return frameHeight;
        }
    }

    //***** EventQueueThread Helper Class -- Provides event handling.
    /**
     * Thread for media player event processing. The thread maintains an
     * internal queue of
     * <code>PlayerEvent</code>s to which callers post using
     * <code>postEvent()</code>. The thread blocks until an event becomes
     * available on the queue, and then removes the event from the queue and
     * posts it to any registered listeners appropriate to the type of event.
     */
    private class EventQueueThread extends Thread {

        private final BlockingQueue<PlayerEvent> eventQueue =
                new LinkedBlockingQueue<>();
        private volatile boolean stopped = false;

        EventQueueThread() {
            setName("JFXMedia Player EventQueueThread");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    // trying to take an event from the queue.
                    // this method will block until an event becomes available.
                    PlayerEvent evt = eventQueue.take();

                    if (!stopped) {
                        if (evt instanceof NewFrameEvent) {
                            try {
                                HandleRendererEvents((NewFrameEvent) evt);
                            } catch (Throwable t) {
                                if (Logger.canLog(Logger.ERROR)) {
                                    Logger.logMsg(Logger.ERROR, "Caught exception in HandleRendererEvents: " + t.toString());
                                }
                            }
                        } else if (evt instanceof PlayerStateEvent) {
                            HandleStateEvents((PlayerStateEvent) evt);
                        } else if (evt instanceof FrameSizeChangedEvent) {
                            HandleFrameSizeChangedEvents((FrameSizeChangedEvent) evt);
                        } else if (evt instanceof TrackEvent) {
                            HandleTrackEvents((TrackEvent) evt);
                        } else if (evt instanceof MarkerEvent) {
                            HandleMarkerEvents((MarkerEvent) evt);
                        } else if (evt instanceof WarningEvent) {
                            HandleWarningEvents((WarningEvent) evt);
                        } else if (evt instanceof PlayerTimeEvent) {
                            HandlePlayerTimeEvents((PlayerTimeEvent) evt);
                        } else if (evt instanceof BufferProgressEvent) {
                            HandleBufferEvents((BufferProgressEvent) evt);
                        } else if (evt instanceof AudioSpectrumEvent) {
                            HandleAudioSpectrumEvents((AudioSpectrumEvent) evt);
                        } else if (evt instanceof MediaErrorEvent) {
                            HandleErrorEvents((MediaErrorEvent) evt);
                        }
                    }
                } catch (Exception e) {
                    // eventQueue.take() can throw InterruptedException,
                    // also in rare case it can throw wrong
                    // IllegalMonitorStateException
                    // so we catch Exception
                    // nothing to do, restart the loop unless it was properly stopped.
                }
            }

            eventQueue.clear();
        }

        private void HandleRendererEvents(NewFrameEvent evt) {
            if (isFirstFrame) {
                // Cache first frame. Frames are delivered time-sequentially
                // so there should be no thread contention problem here.
                isFirstFrame = false;
                synchronized (firstFrameLock) {
                    firstFrameEvent = evt;
                    firstFrameTime = firstFrameEvent.getFrameData().getTimestamp();
                    firstFrameEvent.getFrameData().holdFrame(); // hold as long as we cache it, else we'll crash
                }
            } else if (firstFrameEvent != null
                    && firstFrameTime != evt.getFrameData().getTimestamp()) {
                // If this branch is entered then it cannot be the first frame.
                // This means that the player must be in the PLAYING state as
                // the first frame will arrive upon completion of prerolling.
                // When playing, listeners should receive the current frame,
                // not the first frame in the stream.

                // Clear the cached first frame. Obtain the lock first to avoid
                // a race condition with a listener newly being added.
                synchronized (firstFrameLock) {
                    firstFrameEvent.getFrameData().releaseFrame();
                    firstFrameEvent = null;
                }
            }

            // notify videoUpdateListeners
            for (ListIterator<WeakReference<VideoRendererListener>> it = videoUpdateListeners.listIterator(); it.hasNext();) {
                VideoRendererListener l = it.next().get();
                if (l != null) {
                    l.videoFrameUpdated(evt);
                } else {
                    it.remove();
                }
            }
            // done with the frame, we can release our hold now
            evt.getFrameData().releaseFrame();

            if (!videoFrameRateListeners.isEmpty()) {
                // Decoded frame rate calculations.
                double currentFrameTime = System.nanoTime() / (double) ONE_SECOND;

                if (recomputeFrameRate) {
                    // First frame in new computation sequence.
                    recomputeFrameRate = false;
                    previousFrameTime = currentFrameTime;
                    numFramesSincePlaying = 1;
                } else {
                    boolean fireFrameRateEvent = false;

                    if (numFramesSincePlaying == 1) {
                        // Second frame. Estimate the initial frame rate and
                        // set event flag.
                        meanFrameDuration = currentFrameTime - previousFrameTime;
                        if (meanFrameDuration > 0.0) {
                            decodedFrameRate = 1.0 / meanFrameDuration;
                            fireFrameRateEvent = true;
                        }
                    } else {
                        // Update decoded frame rate estimate using a moving
                        // average over encodedFrameRate frames.
                        double previousMeanFrameDuration = meanFrameDuration;

                        // Determine moving average length.
                        int movingAverageLength = encodedFrameRate != 0.0
                                ? ((int) (encodedFrameRate + 0.5)) : NOMINAL_VIDEO_FPS;

                        // Claculate number of frames in current average.
                        long numFrames = numFramesSincePlaying < movingAverageLength
                                ? numFramesSincePlaying : movingAverageLength;

                        // Update the mean frame duration.
                        meanFrameDuration = ((numFrames - 1) * previousMeanFrameDuration
                                + currentFrameTime - previousFrameTime) / numFrames;

                        // If mean frame duration changed by more than 0.5 set
                        // event flag.
                        if (meanFrameDuration > 0.0
                                && Math.abs(decodedFrameRate - 1.0 / meanFrameDuration) > 0.5) {
                            decodedFrameRate = 1.0 / meanFrameDuration;
                            fireFrameRateEvent = true;
                        }
                    }

                    if (fireFrameRateEvent) {
                        // Fire event.
                        for (ListIterator<WeakReference<VideoFrameRateListener>> it = videoFrameRateListeners.listIterator(); it.hasNext();) {
                            VideoFrameRateListener l = it.next().get();
                            if (l != null) {
                                l.onFrameRateChanged(decodedFrameRate);
                            } else {
                                it.remove();
                            }
                        }
                    }

                    // Update running values.
                    previousFrameTime = currentFrameTime;
                    numFramesSincePlaying++;
                }
            }
        }

        private void HandleStateEvents(PlayerStateEvent evt) {
            playerState = evt.getState();

            recomputeFrameRate = PlayerState.PLAYING == evt.getState();

            switch (playerState) {
                case READY:
                    onNativeInit();
                    sendFakeBufferProgressEvent();
                    break;
                case PLAYING:
                    isMediaPulseEnabled.set(true);
                    break;
                case STOPPED:
                case FINISHED:
                    // Force a time update here to catch the time going to
                    // zero for STOPPED and any trailing markers for FINISHED.
                    doMediaPulseTask();
                case PAUSED:
                case STALLED:
                case HALTED:
                    isMediaPulseEnabled.set(false);
                    break;
                default:
                    break;
            }

            synchronized (cachedStateEvents) {
                if (playerStateListeners.isEmpty()) {
                    // Cache event for processing when first listener registers.
                    cachedStateEvents.add(evt);
                    return;
                }
            }

            for (ListIterator<WeakReference<PlayerStateListener>> it = playerStateListeners.listIterator(); it.hasNext();) {
                PlayerStateListener listener = it.next().get();
                if (listener != null) {
                    switch (playerState) {
                        case READY:
                            onNativeInit();
                            sendFakeBufferProgressEvent();
                            listener.onReady(evt);
                            break;

                        case PLAYING:
                            listener.onPlaying(evt);
                            break;

                        case PAUSED:
                            listener.onPause(evt);
                            break;

                        case STOPPED:
                            listener.onStop(evt);
                            break;

                        case STALLED:
                            listener.onStall(evt);
                            break;

                        case FINISHED:
                            listener.onFinish(evt);
                            break;

                        case HALTED:
                            listener.onHalt(evt);
                            break;

                        default:
                            break;
                    }
                } else {
                    it.remove();
                }
            }
        }

        private void HandlePlayerTimeEvents(PlayerTimeEvent evt) {
            synchronized (cachedTimeEvents) {
                if (playerTimeListeners.isEmpty()) {
                    // Cache event for processing when first listener registers.
                    cachedTimeEvents.add(evt);
                    return;
                }
            }

            for (ListIterator<WeakReference<PlayerTimeListener>> it = playerTimeListeners.listIterator(); it.hasNext();) {
                PlayerTimeListener listener = it.next().get();
                if (listener != null) {
                    listener.onDurationChanged(evt.getTime());
                } else {
                    it.remove();
                }
            }
        }

        private void HandleFrameSizeChangedEvents(FrameSizeChangedEvent evt) {
            frameWidth = evt.getWidth();
            frameHeight = evt.getHeight();
            Logger.logMsg(Logger.DEBUG, "** Frame size changed (" + frameWidth + ", " + frameHeight + ")");
            for (ListIterator<WeakReference<VideoTrackSizeListener>> it = videoTrackSizeListeners.listIterator(); it.hasNext();) {
                VideoTrackSizeListener listener = it.next().get();
                if (listener != null) {
                    listener.onSizeChanged(frameWidth, frameHeight);
                } else {
                    it.remove();
                }
            }
        }

        private void HandleTrackEvents(TrackEvent evt) {
            media.addTrack(evt.getTrack());

            if (evt.getTrack() instanceof VideoTrack) {
                encodedFrameRate = ((VideoTrack) evt.getTrack()).getEncodedFrameRate();
            }
        }

        private void HandleMarkerEvents(MarkerEvent evt) {
            for (ListIterator<WeakReference<MarkerListener>> it = markerListeners.listIterator(); it.hasNext();) {
                MarkerListener listener = it.next().get();
                if (listener != null) {
                    listener.onMarker(evt);
                } else {
                    it.remove();
                }
            }
        }

        private void HandleWarningEvents(WarningEvent evt) {
            Logger.logMsg(Logger.WARNING, evt.getSource() + evt.getMessage());
        }

        private void HandleErrorEvents(MediaErrorEvent evt) {
            Logger.logMsg(Logger.ERROR, evt.getMessage());

            synchronized (cachedErrorEvents) {
                if (errorListeners.isEmpty()) {
                    // cache error events until at least one listener is added
                    cachedErrorEvents.add(evt);
                    return;
                }
            }

            for (ListIterator<WeakReference<MediaErrorListener>> it = errorListeners.listIterator(); it.hasNext();) {
                MediaErrorListener l = it.next().get();
                if (l != null) {
                    l.onError(evt.getSource(), evt.getErrorCode(), evt.getMessage());
                } else {
                    it.remove();
                }
            }
        }

        private void HandleBufferEvents(BufferProgressEvent evt) {
            synchronized (cachedBufferEvents) {
                if (bufferListeners.isEmpty()) {
                    // Cache event for processing when first listener registers.
                    cachedBufferEvents.add(evt);
                    return;
                }
            }

            for (ListIterator<WeakReference<BufferListener>> it = bufferListeners.listIterator(); it.hasNext();) {
                BufferListener listener = it.next().get();
                if (listener != null) {
                    listener.onBufferProgress(evt);
                } else {
                    it.remove();
                }
            }
        }

        private void HandleAudioSpectrumEvents(AudioSpectrumEvent evt) {
            for (ListIterator<WeakReference<AudioSpectrumListener>> it = audioSpectrumListeners.listIterator(); it.hasNext();) {
                AudioSpectrumListener listener = it.next().get();
                if (listener != null) {
                    // OSXPlatfrom will set queryTimestamp to true, so we can request
                    // time here from EventQueueThread, since requesting time from
                    // audio processing thread might hang. See JDK-8240694.
                    if (evt.queryTimestamp()) {
                        double timestamp = playerGetPresentationTime();
                        evt.setTimestamp(timestamp);
                    }

                    listener.onAudioSpectrumEvent(evt);
                } else {
                    it.remove();
                }
            }
        }

        /**
         * Puts an event to the EventQuery.
         */
        public void postEvent(PlayerEvent event) {
            if (eventQueue != null) {
                eventQueue.offer(event);
            }
        }

        /**
         * Signals the thread to terminate.
         */
        public void terminateLoop() {
            stopped = true;
            // put an event to unblock eventQueue.take()
            try {
                eventQueue.put(new PlayerEvent());
            } catch(InterruptedException ex) {}
        }

        private void sendFakeBufferProgressEvent() {
            // Send fake 100% buffer progress event for HLS or !http protcol
            String contentType = media.getLocator().getContentType();
            String protocol = media.getLocator().getProtocol();
            if ((contentType != null && (contentType.equals(MediaUtils.CONTENT_TYPE_M3U) || contentType.equals(MediaUtils.CONTENT_TYPE_M3U8)))
                    || (protocol != null && !protocol.equals("http") && !protocol.equals("https"))) {
                HandleBufferEvents(new BufferProgressEvent(getDuration(), 0, 1, 1));
            }
        }
    }

    /**
     * Internal function to get called when the native player is ready.
     */
    private synchronized void onNativeInit() {
        try {
            playerInit();
        } catch (MediaException me) {
            sendPlayerMediaErrorEvent(me.getMediaError().code());
        }
    }

    //**************************************************************************
    //***** MediaPlayer implementation
    //**************************************************************************
    //***** Listener (un)registration.
    @Override
    public void addMediaErrorListener(MediaErrorListener listener) {
        if (listener != null) {
            this.errorListeners.add(new WeakReference<>(listener));

            synchronized (cachedErrorEvents) {
                if (!cachedErrorEvents.isEmpty() && !errorListeners.isEmpty()) {
                    cachedErrorEvents.stream().forEach((evt) -> {
                        sendPlayerEvent(evt);
                    });
                    cachedErrorEvents.clear();
                }
            }
        }
    }

    @Override
    public void removeMediaErrorListener(MediaErrorListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<MediaErrorListener>> it = errorListeners.listIterator(); it.hasNext();) {
                MediaErrorListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addMediaPlayerListener(PlayerStateListener listener) {
        if (listener != null) {
            synchronized (cachedStateEvents) {
                if (!cachedStateEvents.isEmpty() && playerStateListeners.isEmpty()) {
                    // Forward all cached state events to first listener to register.
                    Iterator<PlayerStateEvent> events = cachedStateEvents.iterator();
                    while (events.hasNext()) {
                        PlayerStateEvent evt = events.next();
                        switch (evt.getState()) {
                            case READY:
                                listener.onReady(evt);
                                break;
                            case PLAYING:
                                listener.onPlaying(evt);
                                break;
                            case PAUSED:
                                listener.onPause(evt);
                                break;
                            case STOPPED:
                                listener.onStop(evt);
                                break;
                            case STALLED:
                                listener.onStall(evt);
                                break;
                            case FINISHED:
                                listener.onFinish(evt);
                                break;
                            case HALTED:
                                listener.onHalt(evt);
                                break;
                            default:
                                break;
                        }
                    }

                    // Clear state event cache.
                    cachedStateEvents.clear();
                }

                playerStateListeners.add(new WeakReference(listener));
            }
        }
    }

    @Override
    public void removeMediaPlayerListener(PlayerStateListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<PlayerStateListener>> it = playerStateListeners.listIterator(); it.hasNext();) {
                PlayerStateListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addMediaTimeListener(PlayerTimeListener listener) {
        if (listener != null) {
            synchronized (cachedTimeEvents) {
                if (!cachedTimeEvents.isEmpty() && playerTimeListeners.isEmpty()) {
                    // Forward all cached time events to first listener to register.
                    Iterator<PlayerTimeEvent> events = cachedTimeEvents.iterator();
                    while (events.hasNext()) {
                        PlayerTimeEvent evt = events.next();
                        listener.onDurationChanged(evt.getTime());
                    }

                    // Clear time event cache.
                    cachedTimeEvents.clear();
                } else {
                    // Let listener to know about duration
                    double duration = getDuration();
                    if (duration != Double.POSITIVE_INFINITY) {
                        listener.onDurationChanged(duration);
                    }
                }

                playerTimeListeners.add(new WeakReference(listener));
            }
        }
    }

    @Override
    public void removeMediaTimeListener(PlayerTimeListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<PlayerTimeListener>> it = playerTimeListeners.listIterator(); it.hasNext();) {
                PlayerTimeListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addVideoTrackSizeListener(VideoTrackSizeListener listener) {
        if (listener != null) {
            if (frameWidth != -1 && frameHeight != -1) {
                listener.onSizeChanged(frameWidth, frameHeight);
            }
            videoTrackSizeListeners.add(new WeakReference(listener));
        }
    }

    @Override
    public void removeVideoTrackSizeListener(VideoTrackSizeListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<VideoTrackSizeListener>> it = videoTrackSizeListeners.listIterator(); it.hasNext();) {
                VideoTrackSizeListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addMarkerListener(MarkerListener listener) {
        if (listener != null) {
            markerListeners.add(new WeakReference(listener));
        }
    }

    @Override
    public void removeMarkerListener(MarkerListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<MarkerListener>> it = markerListeners.listIterator(); it.hasNext();) {
                MarkerListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addBufferListener(BufferListener listener) {
        if (listener != null) {
            synchronized (cachedBufferEvents) {
                if (!cachedBufferEvents.isEmpty() && bufferListeners.isEmpty()) {
                    cachedBufferEvents.stream().forEach((evt) -> {
                        listener.onBufferProgress(evt);
                    });
                    // Clear buffer event cache.
                    cachedBufferEvents.clear();
                }

                bufferListeners.add(new WeakReference(listener));
            }
        }
    }

    @Override
    public void removeBufferListener(BufferListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<BufferListener>> it = bufferListeners.listIterator(); it.hasNext();) {
                BufferListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void addAudioSpectrumListener(AudioSpectrumListener listener) {
        if (listener != null) {
            audioSpectrumListeners.add(new WeakReference(listener));
        }
    }

    @Override
    public void removeAudioSpectrumListener(AudioSpectrumListener listener) {
        if (listener != null) {
            for (ListIterator<WeakReference<AudioSpectrumListener>> it = audioSpectrumListeners.listIterator(); it.hasNext();) {
                AudioSpectrumListener l = it.next().get();
                if (l == null || l == listener) {
                    it.remove();
                }
            }
        }
    }

    //***** Control functions
    @Override
    public VideoRenderControl getVideoRenderControl() {
        return videoRenderControl;
    }

    @Override
    public Media getMedia() {
        return media;
    }

    @Override
    public void setAudioSyncDelay(long delay) {
        try {
            playerSetAudioSyncDelay(delay);
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
    }

    @Override
    public long getAudioSyncDelay() {
        try {
            return playerGetAudioSyncDelay();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return 0;
    }

    @Override
    public void play() {
        try {
            if (isStartTimeUpdated) {
                playerSeek(startTime);
            }
            isMediaPulseEnabled.set(true);
            playerPlay();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
    }

    @Override
    public void stop() {
        try {
            playerStop();
            playerSeek(startTime);
        } catch (MediaException me) {
//            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
            MediaUtils.warning(this, "stop() failed!");
        }
    }

    @Override
    public void pause() {
        try {
            playerPause();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
    }

    @Override
    public float getRate() {
        try {
            return playerGetRate();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return 0;
    }

    //***** Public properties
    @Override
    public void setRate(float rate) {
        try {
            playerSetRate(rate);
        } catch (MediaException me) {
//            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
            MediaUtils.warning(this, "setRate(" + rate + ") failed!");
        }
    }

    @Override
    public double getPresentationTime() {
        try {
            return playerGetPresentationTime();
        } catch (MediaException me) {
//            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return -1.0;
    }

    @Override
    public float getVolume() {
        try {
            return playerGetVolume();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return 0;
    }

    @Override
    public void setVolume(float vol) {
        if (vol < 0.0F) {
            vol = 0.0F;
        } else if (vol > 1.0F) {
            vol = 1.0F;
        }

        try {
            playerSetVolume(vol);
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
    }

    @Override
    public boolean getMute() {
        try {
            return playerGetMute();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return false;
    }

    /**
     * Enables/disable mute. If mute is enabled then disabled, the previous
     * volume goes into effect.
     */
    @Override
    public void setMute(boolean enable) {
        try {
            playerSetMute(enable);
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
    }

    @Override
    public float getBalance() {
        try {
            return playerGetBalance();
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return 0;
    }

    @Override
    public void setBalance(float bal) {
        if (bal < -1.0F) {
            bal = -1.0F;
        } else if (bal > 1.0F) {
            bal = 1.0F;
        }

        try {
            playerSetBalance(bal);
        } catch (MediaException me) {
            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
    }

    @Override
    public abstract AudioEqualizer getEqualizer();

    @Override
    public abstract AudioSpectrum getAudioSpectrum();

    @Override
    public double getDuration() {
        try {
            return playerGetDuration();
        } catch (MediaException me) {
//            sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Gets the time within the duration of the media to start playing.
     */
    @Override
    public double getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time within the media to play.
     */
    @Override
    public void setStartTime(double startTime) {
        try {
            markerLock.lock();
            this.startTime = startTime;
            if (playerState != PlayerState.PLAYING && playerState != PlayerState.FINISHED && playerState != PlayerState.STOPPED) {
                playerSeek(startTime);
            } else if (playerState == PlayerState.STOPPED) {
                isStartTimeUpdated = true;
            }
        } finally {
            markerLock.unlock();
        }
    }

    /**
     * Gets the time within the duration of the media to stop playing.
     */
    @Override
    public double getStopTime() {
        return stopTime;
    }

    /**
     * Sets the stop time within the media to stop playback.
     */
    @Override
    public void setStopTime(double stopTime) {
        try {
            markerLock.lock();
            this.stopTime = stopTime;
            isStopTimeSet = true;
            createMediaPulse();
        } finally {
            markerLock.unlock();
        }
    }

    @Override
    public void seek(double streamTime) {
        if (playerState == PlayerState.STOPPED) {
            return; // No seek in stopped state
        }

        if (streamTime < 0.0) {
            streamTime = 0.0;
        } else {
            double duration = getDuration();
            if (duration >= 0.0 && streamTime > duration) {
                streamTime = duration;
            }
        }

        if (!isMediaPulseEnabled.get()) {
            if ((playerState == PlayerState.PLAYING
                    || playerState == PlayerState.PAUSED
                    || playerState == PlayerState.FINISHED)
                    && getStartTime() <= streamTime && streamTime <= getStopTime()) {
                isMediaPulseEnabled.set(true);
            }
        }

        markerLock.lock();
        try {
            timeBeforeSeek = getPresentationTime();
            timeAfterSeek = streamTime;
            checkSeek = timeBeforeSeek != timeAfterSeek;
            previousTime = streamTime;
            firedMarkerTime = -1.0;
//            System.out.println("seek @ "+System.currentTimeMillis());
//            System.out.println("seek to "+streamTime+" previousTime "+previousTime);

            try {
                playerSeek(streamTime);
            } catch (MediaException me) {
                //sendPlayerEvent(new MediaErrorEvent(this, me.getMediaError()));
                MediaUtils.warning(this, "seek(" + streamTime + ") failed!");
            }
        } finally {
            markerLock.unlock();
        }
    }

    protected abstract long playerGetAudioSyncDelay() throws MediaException;

    protected abstract void playerSetAudioSyncDelay(long delay) throws MediaException;

    protected abstract void playerPlay() throws MediaException;

    protected abstract void playerStop() throws MediaException;

    protected abstract void playerPause() throws MediaException;

    protected abstract void playerFinish() throws MediaException;

    protected abstract float playerGetRate() throws MediaException;

    protected abstract void playerSetRate(float rate) throws MediaException;

    protected abstract double playerGetPresentationTime() throws MediaException;

    protected abstract boolean playerGetMute() throws MediaException;

    protected abstract void playerSetMute(boolean state) throws MediaException;

    protected abstract float playerGetVolume() throws MediaException;

    protected abstract void playerSetVolume(float volume) throws MediaException;

    protected abstract float playerGetBalance() throws MediaException;

    protected abstract void playerSetBalance(float balance) throws MediaException;

    protected abstract double playerGetDuration() throws MediaException;

    protected abstract void playerSeek(double streamTime) throws MediaException;

    protected abstract void playerInit() throws MediaException;

    protected abstract void playerDispose();

    /**
     * Retrieves the current {@link PlayerState state} of the player.
     *
     * @return the current player state.
     */
    @Override
    public PlayerState getState() {
        return playerState;
    }

    @Override
    final public void dispose() {
        disposeLock.lock();
        try {
            if (!isDisposed) {
                // Terminate event firing
                destroyMediaPulse();

                if (eventLoop != null) {
                    eventLoop.terminateLoop();
                    eventLoop = null;
                }

                synchronized (firstFrameLock) {
                    if (firstFrameEvent != null) {
                        firstFrameEvent.getFrameData().releaseFrame();
                        firstFrameEvent = null;
                    }
                }

                // Terminate native layer
                playerDispose();

                // Dispose media object and clear reference
                if (media != null) {
                    media.dispose();
                    media = null;
                }

                if (videoUpdateListeners != null) {
                    for (ListIterator<WeakReference<VideoRendererListener>> it = videoUpdateListeners.listIterator(); it.hasNext();) {
                        VideoRendererListener l = it.next().get();
                        if (l != null) {
                            l.releaseVideoFrames();
                        } else {
                            it.remove();
                        }
                    }

                    videoUpdateListeners.clear();
                }

                if (playerStateListeners != null) {
                    playerStateListeners.clear();
                }

                if (videoTrackSizeListeners != null) {
                    videoTrackSizeListeners.clear();
                }

                if (videoFrameRateListeners != null) {
                    videoFrameRateListeners.clear();
                }

                if (cachedStateEvents != null) {
                    cachedStateEvents.clear();
                }

                if (cachedTimeEvents != null) {
                    cachedTimeEvents.clear();
                }

                if (cachedBufferEvents != null) {
                    cachedBufferEvents.clear();
                }

                if (errorListeners != null) {
                    errorListeners.clear();
                }

                if (playerTimeListeners != null) {
                    playerTimeListeners.clear();
                }

                if (markerListeners != null) {
                    markerListeners.clear();
                }

                if (bufferListeners != null) {
                    bufferListeners.clear();
                }

                if (audioSpectrumListeners != null) {
                    audioSpectrumListeners.clear();
                }

                if (videoRenderControl != null) {
                    videoRenderControl = null;
                }

                if (onDispose != null) {
                    onDispose.run();
                }

                isDisposed = true;
            }
        } finally {
            disposeLock.unlock();
        }
    }

    @Override
    public boolean isErrorEventCached() {
        synchronized (cachedErrorEvents) {
            if (cachedErrorEvents.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }
    }

    //**************************************************************************
    //***** Non-JNI methods called by the native layer. These methods are called
    //***** from the native layer via the invocation API. Their purpose is to
    //***** dispatch certain events to the Java layer. Each of these methods
    //***** posts an event on the <code>EventQueueThread</code> which in turn
    //***** forwards the event to any registered listeners.
    //**************************************************************************
    protected void sendWarning(int warningCode, String warningMessage) {
        if (eventLoop != null) {
            String message = String.format(MediaUtils.NATIVE_MEDIA_WARNING_FORMAT,
                    warningCode);
            if (warningMessage != null) {
                message += ": " + warningMessage;
            }
            eventLoop.postEvent(new WarningEvent(this, message));
        }
    }

    protected void sendPlayerEvent(PlayerEvent evt) {
        if (eventLoop != null) {
            eventLoop.postEvent(evt);
        }
    }

    protected void sendPlayerHaltEvent(String message, double time) {
        // Log the error.  Since these are most likely playback engine message (e.g. GStreamer or PacketVideo),
        // it makes no sense to propogate it above.
        Logger.logMsg(Logger.ERROR, message);

        if (eventLoop != null) {
            eventLoop.postEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.HALTED, time, message));
        }
    }

    protected void sendPlayerMediaErrorEvent(int errorCode) {
        sendPlayerEvent(new MediaErrorEvent(this, MediaError.getFromCode(errorCode)));
    }

    protected void sendPlayerStateEvent(int eventID, double time) {
        switch (eventID) {
            case eventPlayerReady:
                sendPlayerEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.READY, time));
                break;
            case eventPlayerPlaying:
                sendPlayerEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.PLAYING, time));
                break;
            case eventPlayerPaused:
                sendPlayerEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.PAUSED, time));
                break;
            case eventPlayerStopped:
                sendPlayerEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.STOPPED, time));
                break;
            case eventPlayerStalled:
                sendPlayerEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.STALLED, time));
                break;
            case eventPlayerFinished:
                sendPlayerEvent(new PlayerStateEvent(PlayerStateEvent.PlayerState.FINISHED, time));
                break;
            default:
                break;
        }
    }

    protected void sendNewFrameEvent(long nativeRef) {
        NativeVideoBuffer newFrameData = NativeVideoBuffer.createVideoBuffer(nativeRef);
        // createVideoBuffer puts a hold on the frame
        // we need to keep that hold until the event thread can process this event
        sendPlayerEvent(new NewFrameEvent(newFrameData));
    }

    protected void sendFrameSizeChangedEvent(int width, int height) {
        sendPlayerEvent(new FrameSizeChangedEvent(width, height));
    }

    protected void sendAudioTrack(boolean enabled, long trackID, String name, int encoding,
            String language, int numChannels,
            int channelMask, float sampleRate) {
        Locale locale = null;
        if (!language.equals("und")) {
            locale = new Locale(language);
        }

        Track track = new AudioTrack(enabled, trackID, name,
                locale, Encoding.toEncoding(encoding),
                numChannels, channelMask, sampleRate);

        TrackEvent evt = new TrackEvent(track);

        sendPlayerEvent(evt);
    }

    protected void sendVideoTrack(boolean enabled, long trackID, String name, int encoding,
            int width, int height, float frameRate,
            boolean hasAlphaChannel) {
        // No locale (currently) for video, so pass null
        Track track = new VideoTrack(enabled, trackID, name, null,
                Encoding.toEncoding(encoding),
                new VideoResolution(width, height), frameRate, hasAlphaChannel);

        TrackEvent evt = new TrackEvent(track);

        sendPlayerEvent(evt);
    }

    protected void sendSubtitleTrack(boolean enabled, long trackID, String name,
            int encoding, String language)
    {
        Locale locale = null;
        if (null != language) {
            locale = new Locale(language);
        }
        Track track = new SubtitleTrack(enabled, trackID, name, locale,
                Encoding.toEncoding(encoding));

        sendPlayerEvent(new TrackEvent(track));
    }

    protected void sendMarkerEvent(String name, double time) {
        sendPlayerEvent(new MarkerEvent(name, time));
    }

    protected void sendDurationUpdateEvent(double duration) {
        sendPlayerEvent(new PlayerTimeEvent(duration));
    }

    protected void sendBufferProgressEvent(double clipDuration, long bufferStart, long bufferStop, long bufferPosition) {
        sendPlayerEvent(new BufferProgressEvent(clipDuration, bufferStart, bufferStop, bufferPosition));
    }

    protected void sendAudioSpectrumEvent(double timestamp, double duration, boolean queryTimestamp) {
        sendPlayerEvent(new AudioSpectrumEvent(getAudioSpectrum(), timestamp, duration, queryTimestamp));
    }

    @Override
    public void markerStateChanged(boolean hasMarkers) {
        if (hasMarkers) {
            markerLock.lock();
            try {
                previousTime = getPresentationTime();
            } finally {
                markerLock.unlock();
            }
            createMediaPulse();
        } else {
            if (!isStopTimeSet) {
                destroyMediaPulse();
            }
        }
    }

    private void createMediaPulse() {
        mediaPulseLock.lock();
        try {
            if (mediaPulseTimer == null) {
                mediaPulseTimer = new Timer(true);
                mediaPulseTimer.scheduleAtFixedRate(new MediaPulseTask(this), 0, 40 /*
                         * period ms
                         */);
            }
        } finally {
            mediaPulseLock.unlock();
        }
    }

    private void destroyMediaPulse() {
        mediaPulseLock.lock();
        try {
            if (mediaPulseTimer != null) {
                mediaPulseTimer.cancel();
                mediaPulseTimer = null;
            }
        } finally {
            mediaPulseLock.unlock();
        }
    }

    boolean doMediaPulseTask() {
        if (this.isMediaPulseEnabled.get()) {
            disposeLock.lock();

            if (isDisposed) {
                disposeLock.unlock();
                return false;
            }

            double thisTime = getPresentationTime();

            markerLock.lock();

            try {
                //System.out.println("Media pulse @ pts "+thisTime+" previous "+previousTime);

                if (checkSeek) {
                    if (timeAfterSeek > timeBeforeSeek) {
                        // Forward seek
                        if (thisTime >= timeAfterSeek) {
//                        System.out.println("bail 1");
                            checkSeek = false;
                        } else {
                            return true;
                        }
                    } else if (timeAfterSeek < timeBeforeSeek) {
                        // Backward seek
                        if (thisTime >= timeBeforeSeek) {
//                        System.out.println("bail 2");
                            return true;
                        } else {
                            checkSeek = false;
                        }
                    }
                }

                Map.Entry<Double, String> marker = media.getNextMarker(previousTime, true);
//                System.out.println("marker "+marker);
//                System.out.println("Checking: " + previousTime + " " + thisTime + " "
//                        + getStartTime() + " " + getStopTime() + " "
//                        + marker.getKey());

                while (marker != null) {
                    double nextMarkerTime = marker.getKey();
                    if (nextMarkerTime > thisTime) {
                        break;
                    } else if (nextMarkerTime != firedMarkerTime
                            && nextMarkerTime >= previousTime
                            && nextMarkerTime >= getStartTime()
                            && nextMarkerTime <= getStopTime()) {
//                            System.out.println("Firing: "+previousTime+" "+thisTime+" "+
//                                    getStartTime()+" "+getStopTime()+" "+
//                                    nextMarkerTime);
                        MarkerEvent evt = new MarkerEvent(marker.getValue(), nextMarkerTime);
                        for (ListIterator<WeakReference<MarkerListener>> it = markerListeners.listIterator(); it.hasNext();) {
                            MarkerListener listener = it.next().get();
                            if (listener != null) {
                                listener.onMarker(evt);
                            } else {
                                it.remove();
                            }
                        }
                        firedMarkerTime = nextMarkerTime;
                    }
                    marker = media.getNextMarker(nextMarkerTime, false);
                }

                previousTime = thisTime;

                // Do stopTime
                if (isStopTimeSet && thisTime >= stopTime) {
                    playerFinish();
                }
            } finally {
                disposeLock.unlock();
                markerLock.unlock();
            }
        }

        return true;
    }

    /* Audio EQ and spectrum creation, used by sub-classes */
    protected AudioEqualizer createNativeAudioEqualizer(long nativeRef) {
        return new NativeAudioEqualizer(nativeRef);
    }

    protected AudioSpectrum createNativeAudioSpectrum(long nativeRef) {
        return new NativeAudioSpectrum(nativeRef);
    }
}

class MediaPulseTask extends TimerTask {

    WeakReference<NativeMediaPlayer> playerRef;

    MediaPulseTask(NativeMediaPlayer player) {
        playerRef = new WeakReference<>(player);
    }

    @Override
    public void run() {
        final NativeMediaPlayer player = playerRef.get();
        if (player != null) {
            if (!player.doMediaPulseTask()) {
                cancel(); // Stop if doMediaPulseTask() returns false. False means doMediaPulseTask() cannot continue (like after dispose).cy
            }
        } else {
            cancel();
        }
    }
}
