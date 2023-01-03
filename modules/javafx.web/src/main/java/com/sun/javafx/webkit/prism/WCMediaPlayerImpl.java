/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import java.net.URI;
import java.util.List;

import com.sun.javafx.media.PrismMediaFrameHandler;
import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.control.VideoDataBuffer;
import com.sun.media.jfxmedia.events.BufferListener;
import com.sun.media.jfxmedia.events.BufferProgressEvent;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.events.NewFrameEvent;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import com.sun.media.jfxmedia.events.PlayerStateListener;
import com.sun.media.jfxmedia.events.PlayerTimeListener;
import com.sun.media.jfxmedia.events.VideoRendererListener;
import com.sun.media.jfxmedia.events.VideoTrackSizeListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.track.AudioTrack;
import com.sun.media.jfxmedia.track.Track;
import com.sun.media.jfxmedia.track.VideoTrack;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCMediaPlayer;


final class WCMediaPlayerImpl extends WCMediaPlayer
        implements PlayerStateListener, MediaErrorListener,
        VideoTrackSizeListener, BufferListener, PlayerTimeListener
{

    // lock for fields access (player, createThread, frameHandler)
    private final Object lock = new Object();

    private volatile MediaPlayer player;
    private volatile CreateThread createThread;
    private volatile PrismMediaFrameHandler frameHandler;

    private final MediaFrameListener frameListener;

    // we need this flag to handle a case when 1st frame arrives before onReady
    private boolean gotFirstFrame = false;

    // 1: at the end (rate > 0); -1: at the begining (rate < 0)
    private int finished = 0;

    WCMediaPlayerImpl() {
        frameListener = new MediaFrameListener();
    }

    private MediaPlayer getPlayer() {
        synchronized(lock) {
            if (createThread != null) {
                return null;
            }
            return player;
        }
    }

    private void setPlayer(MediaPlayer p) {
        synchronized (lock) {
            player = p;
            installListeners();
            frameHandler = PrismMediaFrameHandler.getHandler(player);
        }

        finished = 0;
    }

    private final class CreateThread extends Thread {
        private boolean cancelled = false;
        private final String url;
        private final String userAgent;
        private CreateThread(String url, String userAgent) {
            this.url = url;
            this.userAgent = userAgent;
            gotFirstFrame = false;
        }

        @Override
        public void run() {
            log.fine("CreateThread: started, url={0}", url);

            notifyNetworkStateChanged(NETWORK_STATE_LOADING);
            notifyReadyStateChanged(READY_STATE_HAVE_NOTHING);

            MediaPlayer p = null;

            try {
                Locator locator = new Locator(new URI(url));
                if (userAgent != null) {
                    locator.setConnectionProperty("User-Agent", userAgent);
                }
                locator.init();
                    log.fine("CreateThread: locator created");

                p = MediaManager.getPlayer(locator);
            } catch (Exception ex) {
                log.warning("CreateThread ERROR: {0}", ex.toString());
                onError(this, 0, ex.getMessage());
                return;
            }

            synchronized (lock) {
                if (cancelled) {
                    log.fine("CreateThread: cancelled");
                    p.dispose();
                    return;
                }
                createThread = null;
                setPlayer(p);
            }
            log.fine("CreateThread: completed");
        }

        private void cancel() {
            synchronized (lock) {
                cancelled = true;
            }
        }
    }


    @Override
    protected void load(String url, String userAgent) {
        synchronized (lock) {
            if (createThread != null) {
                createThread.cancel();
            }
            disposePlayer();
            createThread = new CreateThread(url, userAgent);
        }
        // fx media player does not support loading only metadata,
        // so handle PRELOAD_METADATA as PRELOAD_AUTO (start loading)
        if (getPreload() != PRELOAD_NONE) {
            createThread.start();
        }
    }

    @Override
    protected void cancelLoad() {
        synchronized (lock) {
            if (createThread != null) {
                createThread.cancel();
            }
        }
        MediaPlayer p = getPlayer();
        if (p != null) {
            p.stop();
        }
        notifyNetworkStateChanged(NETWORK_STATE_EMPTY);
        notifyReadyStateChanged(READY_STATE_HAVE_NOTHING);
    }

    @Override
    protected void disposePlayer() {
        MediaPlayer old;
        synchronized (lock) {
            removeListeners();
            old = player;
            player = null;
            if (frameHandler != null) {
                frameHandler.releaseTextures();
                frameHandler = null;
            }
        }
        if (old != null) {
            old.stop();
            old.dispose();
            old = null;
            if (frameListener != null) {
                frameListener.releaseVideoFrames();
            }
        }
    }

    private void installListeners() {
        if (null != player) {
            player.addMediaPlayerListener(this);
            player.addMediaErrorListener(this);
            player.addVideoTrackSizeListener(this);
            player.addBufferListener(this);
            player.getVideoRenderControl().addVideoRendererListener(frameListener);
        }
    }

    private void removeListeners() {
        if (null != player) {
            player.removeMediaPlayerListener(this);
            player.removeMediaErrorListener(this);
            player.removeVideoTrackSizeListener(this);
            player.removeBufferListener(this);
            player.getVideoRenderControl().removeVideoRendererListener(frameListener);
        }
    }

    @Override
    protected void prepareToPlay() {
        synchronized (lock) {
            if (player == null) {
                // Only start the thread if it has been created but not yet started.
                Thread t = createThread;
                if (t != null && t.getState() == Thread.State.NEW) {
                    t.start();
                }
            }
        }
    }

    @Override
    protected void play() {
        MediaPlayer p = getPlayer();
        if (p != null) {
            p.play();
            // workaround: webkit doesn't like late notifications
            notifyPaused(false);
        }
    }

    @Override
    protected void pause() {
        MediaPlayer p = getPlayer();
        if (p != null) {
            p.pause();
            // workaround: webkit doesn't like late notifications
            notifyPaused(true);
        }
    }

    @Override
    protected float getCurrentTime() {
        MediaPlayer p = getPlayer();
        if (p == null) {
            return 0f;
        }
        return finished == 0 ? (float)p.getPresentationTime()
                : finished > 0 ? (float)p.getDuration()
                : 0f;
    }

    @Override
    protected void seek(float time) {
        MediaPlayer p = getPlayer();
        if (p != null) {
            finished = 0;
            if (getReadyState() >= READY_STATE_HAVE_METADATA) {
                notifySeeking(true, READY_STATE_HAVE_METADATA);
            } else {
                notifySeeking(true, READY_STATE_HAVE_NOTHING);
            }
            p.seek(time);

            // fx media doesn't have a notification about seek completeness
            // while seeking fx player returns 0 as current time
            final float seekTime = time;
            Thread seekCompletedThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isSeeking()) {
                        MediaPlayer p = getPlayer();
                        if (p == null) {
                            break;
                        }
                        double cur = p.getPresentationTime();
                        if (seekTime < 0.01 || Math.abs(cur) >= 0.01) {
                            notifySeeking(false, READY_STATE_HAVE_ENOUGH_DATA);
                            break;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            });
            seekCompletedThread.setDaemon(true);
            seekCompletedThread.start();
        }
    }

    @Override
    protected void setRate(float rate) {
        MediaPlayer p = getPlayer();
        if (p != null) {
            p.setRate(rate);
        }
    }

    @Override
    protected void setVolume(float volume) {
        MediaPlayer p = getPlayer();
        if (p != null) {
            p.setVolume(volume);
        }
    }

    @Override
    protected void setMute(boolean mute) {
        MediaPlayer p = getPlayer();
        if (p != null) {
            p.setMute(mute);
        }
    }

    @Override
    protected void setSize(int w, int h) {
        // nothing to do
    }

    @Override
    protected void setPreservesPitch(boolean preserve) {
        // nothing to do
    }

    @Override
    protected void renderCurrentFrame(WCGraphicsContext gc, int x, int y, int w, int h) {
        // TODO: need a render lock in MediaFrameHandler
        synchronized (lock) {
            renderImpl(gc, x, y, w, h);
        }
    }


    private void renderImpl(WCGraphicsContext gc, int x, int y, int w, int h) {
        log.finer(">>(Prism)renderImpl");
        Graphics g = (Graphics)gc.getPlatformGraphics();

        Texture texture = null;
        VideoDataBuffer currentFrame = frameListener.getLatestFrame();

        if (null != currentFrame) {
            if (null != frameHandler) {
                texture = frameHandler.getTexture(g, currentFrame);
            }
            currentFrame.releaseFrame();
        }

        if (texture != null) {
            g.drawTexture(texture,
                    x, y, x + w, y + h,
                    0f, 0f, texture.getContentWidth(), texture.getContentHeight());
            texture.unlock();
        } else {
            log.finest("  (Prism)renderImpl, texture is null, draw black rect");
            gc.fillRect(x, y, w, h, Color.BLACK);
        }
        log.finer("<<(Prism)renderImpl");
    }

    // PlayerStateListener
    @Override
    public void onReady(PlayerStateEvent pse) {
        MediaPlayer p = getPlayer();
        log.fine("onReady");
        Media media = p.getMedia();
        boolean hasVideo = false;
        boolean hasAudio = false;
        if (media != null) {
            List<Track> tracks = media.getTracks();
            if (tracks != null) {
                log.fine("{0} track(s) detected:", tracks.size());
                for (Track track : tracks) {
                    if (track instanceof VideoTrack) {
                        hasVideo = true;
                    } else if (track instanceof AudioTrack) {
                        hasAudio = true;
                    }
                    log.fine("track: {0}", track);
                }
            } else {
                log.warning("onReady, tracks IS NULL");
            }
        } else {
            log.warning("onReady, media IS NULL");
        }
        log.fine("onReady, hasVideo:{0}, hasAudio: {1}", new Object[]{hasVideo, hasAudio});
        notifyReady(hasVideo, hasAudio, (float)p.getDuration());

        // if we have no video, report READY_STATE_HAVE_ENOUGH_DATA right now
        if (!hasVideo) {
            notifyReadyStateChanged(READY_STATE_HAVE_ENOUGH_DATA);
        } else {
            if (getReadyState() < READY_STATE_HAVE_METADATA) {
                if (gotFirstFrame) {
                    notifyReadyStateChanged(READY_STATE_HAVE_ENOUGH_DATA);
                } else {
                    notifyReadyStateChanged(READY_STATE_HAVE_METADATA);
                }
            }
        }
    }

    @Override
    public void onPlaying(PlayerStateEvent pse) {
        log.fine("onPlaying");
        notifyPaused(false);
    }

    @Override
    public void onPause(PlayerStateEvent pse) {
        log.fine("onPause, time: {0}", pse.getTime());
        notifyPaused(true);
    }

    @Override
    public void onStop(PlayerStateEvent pse) {
        log.fine("onStop");
        notifyPaused(true);
    }

    @Override
    public void onStall(PlayerStateEvent pse) {
        log.fine("onStall");
    }

    @Override
    public void onFinish(PlayerStateEvent pse) {
        MediaPlayer p = getPlayer();
        if (p != null) {
            finished = p.getRate() > 0 ? 1 : -1;
            log.fine("onFinish, time: {0}", pse.getTime());
            notifyFinished();
        }
    }

    @Override
    public void onHalt(PlayerStateEvent pse) {
        log.fine("onHalt");
    }

    // MediaErrorListener
    @Override
    public void onError(Object source, int errCode, String message) {
        //MediaPlayer p = getPlayer();
        log.warning("onError, errCode={0}, msg={1}", new Object[]{errCode, message});
        // TODO: parse errCode to detect NETWORK_STATE_FORMAT_ERROR/
        // NETWORK_STATE_NETWORK_ERROR/NETWORK_STATE_DECODE_ERROR
        notifyNetworkStateChanged(NETWORK_STATE_NETWORK_ERROR);
        notifyReadyStateChanged(READY_STATE_HAVE_NOTHING);
    }

    //PlayerTimeListener
    @Override
    public void onDurationChanged(double duration) {
        log.fine("onDurationChanged, duration={0}", duration);
        notifyDurationChanged((float)duration);
    }

    // VideoTrackSizeListener
    @Override
    public void onSizeChanged(int width, int height) {
        //MediaPlayer p = getPlayer();
        log.fine("onSizeChanged, new size = {0} x {1}", new Object[]{width, height});
        notifySizeChanged(width, height);
    }

    private void notifyFrameArrived() {
        if (!gotFirstFrame) {
            // this is the first frame
            // don't set HAVE_ENOUGH_DATA state before onReady
            if (getReadyState() >= READY_STATE_HAVE_METADATA) {
                notifyReadyStateChanged(READY_STATE_HAVE_ENOUGH_DATA);
            }
            gotFirstFrame = true;
        }
        if (finished != 0) {
            log.fine("notifyFrameArrived (after finished) time: {0}", getPlayer().getPresentationTime());
        }
        notifyNewFrame();
    }

    private float bufferedStart = 0f;
    private float bufferedEnd   = 0f;
    private boolean buffering   = false;

    private void updateBufferingStatus() {
        int newNetworkState =
                buffering ? NETWORK_STATE_LOADING
                : bufferedStart > 0 ? NETWORK_STATE_IDLE : NETWORK_STATE_LOADED;
        log.fine("updateBufferingStatus, buffered: [{0} - {1}], buffering = {2}",
            new Object[]{bufferedStart, bufferedEnd, buffering});
        notifyNetworkStateChanged(newNetworkState);
    }

    // BufferListener
    @Override
    public void onBufferProgress(BufferProgressEvent event) {
        /* event (in the current API):
         * double getDuration(): duration of the movie (seconds);
         * long getBufferStart(): start of the buffered data (bytes)
         * long getBufferStop(): end of the movie (bytes)
         * long getBufferPosition(): end of the buffered data (bytes)
         */
        // if duration is not yet known, we cannot calculate buffered ranges
        if (event.getDuration() < 0) {
            return;
        }
        double bytes2seconds = event.getDuration() / event.getBufferStop();
        bufferedStart = (float)(bytes2seconds * event.getBufferStart());
        bufferedEnd = (float)(bytes2seconds * event.getBufferPosition());
        buffering = event.getBufferPosition() < event.getBufferStop();

        float ranges[] = new float[2];
        ranges[0] = bufferedStart;
        ranges[1] = bufferedEnd;
        int bytesLoaded = (int)(event.getBufferPosition() - event.getBufferStart());
        log.finer("onBufferProgress, "
                + "bufferStart={0}, bufferStop={1}, bufferPos={2}, duration={3}; "
                + "notify range [{4},[5]], bytesLoaded: {6}",
                new Object[]{event.getBufferStart(), event.getBufferStop(),
                             event.getBufferPosition(), event.getDuration(),
                             ranges[0], ranges[1], bytesLoaded});
        notifyBufferChanged(ranges, bytesLoaded);
        updateBufferingStatus();
    }

    /* Inner class that will listen for new frames from the jfxmedia player and
     * manage our own texture cache to remove the dependency on
     * PrismMediaFrameHandler
     */
    private final class MediaFrameListener implements VideoRendererListener {
        private final Object frameLock = new Object();
        private VideoDataBuffer currentFrame;
        private VideoDataBuffer nextFrame;

        @Override
        public void videoFrameUpdated(NewFrameEvent nfe) {
            synchronized (frameLock) {
                if (null != nextFrame) {
                    nextFrame.releaseFrame();
                }
                nextFrame = nfe.getFrameData();
                if (null != nextFrame) {
                    nextFrame.holdFrame();
                }
            }

            // and finally notify the base player that we have a new frame
            notifyFrameArrived();
        }

        @Override
        public void releaseVideoFrames() {
            synchronized (frameLock) {
                if (null != nextFrame) {
                    nextFrame.releaseFrame();
                    nextFrame = null;
                }

                if (null != currentFrame) {
                    currentFrame.releaseFrame();
                    currentFrame = null;
                }
            }
        }

        public VideoDataBuffer getLatestFrame() {
            synchronized (frameLock) {
                if (null != nextFrame) {
                    if (null != currentFrame) {
                        currentFrame.releaseFrame();
                    }
                    currentFrame = nextFrame;
                    nextFrame = null;
                }

                // avoid premature release
                if (null != currentFrame) {
                    currentFrame.holdFrame();
                }
                return currentFrame;
            }
        }
    }
}
