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

package com.sun.media.jfxmediaimpl;

import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.events.PlayerStateEvent;
import com.sun.media.jfxmedia.events.PlayerStateListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a stop-gap solution to eliminate the JavaSound implementation. It's
 * not meant to be perfect, just work well enough to demonstrate functionality.
 */
final class NativeMediaAudioClipPlayer
        implements PlayerStateListener, MediaErrorListener
{
    private MediaPlayer mediaPlayer;
    private int playCount;  // tracks number of times we've played
    private int loopCount;
    private boolean playing;
    private boolean ready; // tracks ready state
    private NativeMediaAudioClip sourceClip;

    private double volume;
    private double balance;
    private double pan;
    private double rate;
    private int priority;

    private final ReentrantLock playerStateLock = new ReentrantLock();

    private static final int MAX_PLAYER_COUNT = 16;

    private static final List<NativeMediaAudioClipPlayer> activePlayers =
                new ArrayList<>(MAX_PLAYER_COUNT);
    private static final ReentrantLock playerListLock = new ReentrantLock();

    public static int getPlayerLimit() {
        return MAX_PLAYER_COUNT;
    }

    public static int getPlayerCount() {
        return activePlayers.size();
    }

    // Singleton scheduler thread
    private static class Enthreaderator {
        private static final Thread schedulerThread;

        static {
            schedulerThread = new Thread(() -> {
                clipScheduler();
            });
            schedulerThread.setDaemon(true);
            schedulerThread.start();
        }

        public static Thread getSchedulerThread() {
            return schedulerThread;
        }
    }

    private static final LinkedBlockingQueue<SchedulerEntry> schedule =
            new LinkedBlockingQueue<>();

    private static void clipScheduler() {
        while (true) {
            SchedulerEntry entry = null;
            try {
                entry = schedule.take();
            } catch (InterruptedException ie) {}

            if (null != entry) {
                if (entry.getCommand() == 0) {
                    NativeMediaAudioClipPlayer player = entry.getPlayer();
                    if (null != player) {
                        // play a clip
                        if (addPlayer(player)) {
                            player.play();
                        } else {
                            player.sourceClip.playFinished(); // couldn't schedule
                        }
                    }
                } else if (entry.getCommand() == 1) {
                    // stop all instances of a clip, or all clips
                    // drop from schedule too, synchronize as this is expensive anyways
                    URI sourceURI = entry.getClipURI();

                    playerListLock.lock();
                    try {
                        // Stop all active players
                        NativeMediaAudioClipPlayer[] players = new NativeMediaAudioClipPlayer[MAX_PLAYER_COUNT];
                        players = activePlayers.toArray(players);
                        if (null != players) {
                            for (int index = 0; index < players.length; index++) {
                                if (null != players[index] && (null == sourceURI ||
                                    players[index].source().getURI().equals(sourceURI)))
                                {
                                    players[index].invalidate();
                                }
                            }
                        }
                    } finally {
                        playerListLock.unlock();
                    }

                    // purge the schedule too
                    boolean clearSchedule = (null == sourceURI); // if no source given, kill all instances
                    for (SchedulerEntry killEntry : schedule) {
                        NativeMediaAudioClipPlayer player = killEntry.getPlayer();
                        if (clearSchedule ||
                            (null != player && player.sourceClip.getLocator().getURI().equals(sourceURI)))
                        {
                            // deschedule the entry
                            schedule.remove(killEntry);
                            player.sourceClip.playFinished();
                        }
                    }
                } else if (entry.getCommand() == 2) {
                    entry.getMediaPlayer().dispose();
                }

                // unblock any waiting threads
                entry.signal();
            }
        }
    }

    public static void playClip(NativeMediaAudioClip clip,
            double volume, double balance,
            double rate, double pan,
            int loopCount, int priority)
    {
        // Kickstart the scheduler thread if needed
        Enthreaderator.getSchedulerThread();

        // don't schedule if we're just going to add a duplicate
        // this will prevent the app from overloading the queue
        NativeMediaAudioClipPlayer newPlayer = new NativeMediaAudioClipPlayer(clip, volume, balance, rate, pan, loopCount, priority);
        SchedulerEntry entry = new SchedulerEntry(newPlayer);
        boolean scheduled = schedule.contains(entry);
        if (scheduled || !schedule.offer(entry)) {
            // didn't schedule, make sure we update playCount
                // don't spam the log if it's just a duplicate entry
            if (Logger.canLog(Logger.DEBUG) && !scheduled) {
                Logger.logMsg(Logger.DEBUG, "AudioClip could not be scheduled for playback!");
            }
            clip.playFinished();
        }
    }

    private static boolean addPlayer(NativeMediaAudioClipPlayer newPlayer) {
        // find an available slot, create new player, fill available slot
        // see if we have room first
        playerListLock.lock();
        try {
            int priority = newPlayer.priority();
            while (activePlayers.size() >= MAX_PLAYER_COUNT) {
                // no more room, find a lower priority player to kill
                NativeMediaAudioClipPlayer target = null; // target for removal
                for (NativeMediaAudioClipPlayer player : activePlayers) {
                    if (player.priority() <= priority &&
                            (target != null ? (target.isReady() && (player.priority() < target.priority())) : true))
                    {
                        // DO NOT MODIFY activePlayers here!!!
                        target = player;
                    }
                }
                if (null != target) {
                    // found a target, kill it
                    target.invalidate();
                } else {
                    // this clip has too low priority, punt
                    return false;
                }
            }
            activePlayers.add(newPlayer);
        } finally {
            playerListLock.unlock();
        }
        return true;
    }

    // Pass null to stop all players
    public static void stopPlayers(Locator source) {
        URI sourceURI = (source != null) ? source.getURI() : null;
        // Use the scheduler thread to handle stopping playback
        // that way we avoid inadvertently allowing already scheduled clips to
        // slip through
        if (null != Enthreaderator.getSchedulerThread()) {
            // drop from the schedule too, we post an entry and wait for the
            // scheduler to process it, otherwise we would have to write a lot of
            // ugly code to work around concurrency issues
            CountDownLatch stopSignal = new CountDownLatch(1);
            SchedulerEntry entry = new SchedulerEntry(sourceURI, stopSignal);
            if (schedule.offer(entry)) {
                // block until the command is processed
                try {
                    // if it doesn't happen in five seconds we got problems
                    stopSignal.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {}
            }
        }
    }

    private NativeMediaAudioClipPlayer(NativeMediaAudioClip clip, double volume,
            double balance, double rate, double pan, int loopCount, int priority)
    {
        sourceClip = clip;
        this.volume = volume;
        this.balance = balance;
        this.pan = pan;
        this.rate = rate;
        this.loopCount = loopCount;
        this.priority = priority;
        ready = false;
    }

    private Locator source() {
        return sourceClip.getLocator();
    }

    public double volume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double balance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double pan() {
        return pan;
    }

    public void setPan(double pan) {
        this.pan = pan;
    }

    public double playbackRate() {
        return rate;
    }

    public void setPlaybackRate(double rate) {
        this.rate = rate;
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int loopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    public boolean isPlaying() {
        return playing;
    }

    private boolean isReady() {
        return ready;
    }

    public synchronized void play() {
        playerStateLock.lock();
        try {
            playing = true;
            playCount = 0;

            if (null == mediaPlayer) {
                mediaPlayer = MediaManager.getPlayer(source());
                mediaPlayer.addMediaPlayerListener(this);
                mediaPlayer.addMediaErrorListener(this);
            } else {
                mediaPlayer.play();
            }
        } finally {
            playerStateLock.unlock();
        }
    }

    public void stop() {
        invalidate();
    }

    public synchronized void invalidate() {
        playerStateLock.lock();
        playerListLock.lock();

        try {
            playing = false;
            playCount = 0;
            ready = false;

            activePlayers.remove(this);
            sourceClip.playFinished();

            if (null != mediaPlayer) {
                mediaPlayer.removeMediaPlayerListener(this);
                mediaPlayer.setMute(true);
                SchedulerEntry entry = new SchedulerEntry(mediaPlayer);
                if (!schedule.offer(entry)) {
                    mediaPlayer.dispose();
                }
                mediaPlayer = null;

            }
        } catch (Throwable t) {
//            System.err.println("Caught exception trying to invalidate AudioClip player: "+t);
//            t.printStackTrace(System.err);
        } finally {
            playerListLock.unlock();
            playerStateLock.unlock();
        }
    }

    @Override
    public void onReady(PlayerStateEvent evt) {
        playerStateLock.lock();
        try {
            ready = true;
            if (playing) {
                mediaPlayer.setVolume((float)volume);
                mediaPlayer.setBalance((float)balance);
                mediaPlayer.setRate((float)rate);
                mediaPlayer.play();
            }
        } finally {
            playerStateLock.unlock();
        }
    }

    @Override
    public void onPlaying(PlayerStateEvent evt) {
    }

    @Override
    public void onPause(PlayerStateEvent evt) {
    }

    @Override
    public void onStop(PlayerStateEvent evt) {
        invalidate();
    }

    @Override
    public void onStall(PlayerStateEvent evt) {
    }

    @Override
    public void onFinish(PlayerStateEvent evt) {
        playerStateLock.lock();
        try {
            if (playing) {
                if (loopCount != -1) {
                    playCount++;
                    if (playCount <= loopCount) {
                        mediaPlayer.seek(0); // restart
                    } else {
                        invalidate();
                    }
                } else {
                    mediaPlayer.seek(0); // restart
                }
            }
        } finally {
            playerStateLock.unlock();
        }
    }

    @Override
    public void onHalt(PlayerStateEvent evt) {
        invalidate();
    }

    public void onWarning(Object source, String message) {
    }

    @Override
    public void onError(Object source, int errorCode, String message) {
        if (Logger.canLog(Logger.ERROR)) {
            Logger.logMsg(Logger.ERROR, "Error with AudioClip player: code "+errorCode+" : "+message);
        }
        invalidate();
    }

    /*
     * Override equals for using in a List of clips pended for playback.
     * Equals is used to avoid repetitions.
     */
    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        }

        if (that instanceof NativeMediaAudioClipPlayer) {
            NativeMediaAudioClipPlayer otherPlayer = (NativeMediaAudioClipPlayer)that;
            URI myURI = sourceClip.getLocator().getURI();
            URI otherURI = otherPlayer.sourceClip.getLocator().getURI();

            return myURI.equals(otherURI) &&
                   priority == otherPlayer.priority &&
                   loopCount == otherPlayer.loopCount &&
                   Double.compare(volume, otherPlayer.volume) == 0 &&
                   Double.compare(balance, otherPlayer.balance) == 0 &&
                   Double.compare(rate, otherPlayer.rate) == 0 &&
                   Double.compare(pan, otherPlayer.pan) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = NativeMediaAudioClipPlayer.class.hashCode();
        h = 31 * h + sourceClip.getLocator().getURI().hashCode();
        h = 31 * h + priority;
        h = 31 * h + loopCount;
        h = 31 * h + Double.hashCode(volume);
        h = 31 * h + Double.hashCode(balance);
        h = 31 * h + Double.hashCode(rate);
        h = 31 * h + Double.hashCode(pan);
        return h;
    }

    private static class SchedulerEntry {
        private final int command; // 0 = play, 1 = stop, 2 = dispose
        private final NativeMediaAudioClipPlayer player; // MAY BE NULL!
        private final URI clipURI; // MAY BE NULL!
        private final CountDownLatch commandSignal; // MAY BE NULL!
        private final MediaPlayer mediaPlayer; // MAY BE NULL!

        // Play command constructor
        public SchedulerEntry(NativeMediaAudioClipPlayer player) {
            command = 0;
            this.player = player;
            clipURI = null;
            commandSignal  = null;
            mediaPlayer = null;
        }

        // Stop command constructor
        public SchedulerEntry(URI sourceURI, CountDownLatch signal) {
            command = 1;
            player = null;
            clipURI = sourceURI;
            commandSignal = signal;
            mediaPlayer = null;
        }

        // Dispose command constructor
        public SchedulerEntry(MediaPlayer mediaPlayer) {
            command = 2;
            player = null;
            clipURI = null;
            commandSignal = null;
            this.mediaPlayer = mediaPlayer;
        }

        public int getCommand() {
            return command;
        }

        public NativeMediaAudioClipPlayer getPlayer() {
            return player;
        }

        public URI getClipURI() {
            return clipURI;
        }

        public MediaPlayer getMediaPlayer() {
            return mediaPlayer;
        }

        public void signal() {
            if (null != commandSignal) {
                commandSignal.countDown();
            }
        }

        // provided ONLY for play implementation, so we can check for duplicate
        // schedule entries
        @Override public boolean equals(Object other) {
            if (other instanceof SchedulerEntry) {
                if (null != player) {
                    return player.equals(((SchedulerEntry)other).getPlayer());
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return player == null ? 0 : player.hashCode();
        }
    }
}
