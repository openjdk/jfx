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

package com.javafx.experiments.dukepad.mediaPlayer;

import javafx.application.Platform;
import javafx.beans.property.*;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java Wrapper around MPG321 media player app.
 */
public class MusicPlayer {
    private static final AtomicInteger numOfFrames = new AtomicInteger(-1);
    private static final SimpleDoubleProperty playPosition = new SimpleDoubleProperty() {
        @Override protected void invalidated() {
            final double frames = numOfFrames.get();
            if (frames > 0 && !isLocalChange) {
                sendCommand("JUMP "+((int)(playPosition.get()*frames)));
            }
        }
    };
    private static final SimpleIntegerProperty currentTime = new SimpleIntegerProperty();
    private static final SimpleIntegerProperty remainingTime = new SimpleIntegerProperty();
    private static final SimpleIntegerProperty totalTime = new SimpleIntegerProperty();
    private static final SimpleDoubleProperty volume = new SimpleDoubleProperty() {
        @Override protected void invalidated() {
            if (!isLocalChange) sendCommand("GAIN "+((int)(100*volume.get())));
        }
    };
    private static final SimpleBooleanProperty paused = new SimpleBooleanProperty(true);
    private static final SimpleObjectProperty<File> currentFile = new SimpleObjectProperty<>();
    private static boolean isLocalChange = false;
    private static Process mediaPlayerProcess;
    private static PrintWriter playerOut;
    private static BufferedReader playerErr;
    private static BufferedReader playerIn;

    /**
     * Play the given music file, starting music player app if needed
     *
     * @param musicFile The full path to music file
     */
    public static void play(File musicFile) {
        isLocalChange = true;
        currentFile.set(musicFile);
        numOfFrames.set(-1);
        volume.set(100);
        paused.set(false);
        isLocalChange = false;
        if (mediaPlayerProcess == null || !mediaPlayerProcess.isAlive()) {
            try {
                mediaPlayerProcess = new ProcessBuilder()
                        .command("mpg321", "-R", "/")
                        .start();
                playerIn = new BufferedReader(new InputStreamReader(mediaPlayerProcess.getInputStream()));
                playerErr = new BufferedReader(new InputStreamReader(mediaPlayerProcess.getErrorStream()));
                playerOut = new PrintWriter(mediaPlayerProcess.getOutputStream());
                new Thread(() -> {
                    String line;
                    try {
                        while((line = playerIn.readLine()) != null) {
                            processInputLine(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
                new Thread(() -> {
                    String line;
                    try {
                        while((line = playerErr.readLine()) != null) {
                            processInputLine(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendCommand("LOAD "+musicFile.getAbsolutePath());
    }

    /**
     * Process one line of output from the music player app
     *
     * @param line one line of output from app
     */
    private static void processInputLine(String line) {
        String[] parts = line.split("\\s+");
        switch (parts[0]) {
            case "@F" :
                final int currentFrame = Integer.parseInt(parts[1]);
                final int framesRemaining = Integer.parseInt(parts[2]);
                final double currentTime = Double.parseDouble(parts[3]);
                final double timeRemaining = Double.parseDouble(parts[4]);
                numOfFrames.set(currentFrame+framesRemaining);
                Platform.runLater(() -> {
                    isLocalChange = true;
                    MusicPlayer.playPosition.set(currentFrame/(double)(currentFrame+framesRemaining));
                    MusicPlayer.currentTime.set((int)currentTime);
                    MusicPlayer.remainingTime.set((int)timeRemaining);
                    MusicPlayer.totalTime.set((int)(currentTime+timeRemaining));
                    isLocalChange = false;
                });
                break;
            case "@P" :
                final int pauseStatus = Integer.parseInt(parts[1]);
                if (pauseStatus == 3) { // song has ended
                    Platform.runLater(() -> {
                        isLocalChange = true;
                        MusicPlayer.playPosition.set(1);
                        MusicPlayer.currentTime.set(MusicPlayer.totalTime.get());
                        currentFile.set(null);
                        numOfFrames.set(-1);
                        volume.set(100);
                        paused.set(true);
                        isLocalChange = false;
                    });
                }
                break;
            default:
                System.out.println("MEDIAPLAYER: "+line);
                break;
        }
    }

    /**
     * Pause the music player if its playing, if its not playing but has been loaded then start playing again
     */
    public static void pauseUnpause() {
        sendCommand("PAUSE");
        paused.set(!paused.get());
    }

    /**
     * Quit the music player app
     */
    public static void quit() {
        if (mediaPlayerProcess != null && mediaPlayerProcess.isAlive()) {
            currentFile.set(null);
            sendCommand("QUIT");
            mediaPlayerProcess.destroy();
        }
    }

    /**
     * Is the music player paused
     *
     * @return true if paused, false if playing
     */
    public static boolean getPaused() {
        return paused.get();
    }

    /**
     * Get paused property
     *
     * @return
     */
    public static ReadOnlyBooleanProperty pausedProperty() {
        return paused;
    }

    public static File getCurrentFile() {
        return currentFile.get();
    }

    public static SimpleObjectProperty<File> currentFileProperty() {
        return currentFile;
    }

    /**
     * Get current volume
     *
     * @return volume between 0 and 1
     */
    public static double getVolume() {
        return volume.get();
    }

    public static SimpleDoubleProperty volumeProperty() {
        return volume;
    }

    public static void setVolume(double volume) {
        MusicPlayer.volume.set(volume);
    }

    /**
     * Get current play head position
     *
     * @return play head position between 0 and 1
     */
    public static double getPlayPosition() {
        return playPosition.get();
    }

    /**
     * Get play head position property
     *
     * @return play head position property
     */
    public static SimpleDoubleProperty playPositionProperty() {
        return playPosition;
    }

    /**
     * Set the play head position
     *
     * @param playPosition new play head position between 0 and 1
     */
    public static void setPlayPosition(double playPosition) {
        MusicPlayer.playPosition.set(playPosition);
    }

    /**
     * Get current time
     *
     * @return current time in seconds
     */
    public static int getCurrentTime() {
        return currentTime.get();
    }

    public static SimpleIntegerProperty currentTimeProperty() {
        return currentTime;
    }

    /**
     * Get remaining time
     *
     * @return the remaining time in seconds
     */
    public static int getRemainingTime() {
        return remainingTime.get();
    }

    public static SimpleIntegerProperty remainingTimeProperty() {
        return remainingTime;
    }

    /**
     * Get total play time
     *
     * @return total play time in seconds
     */
    public static int getTotalTime() {
        return totalTime.get();
    }

    public static SimpleIntegerProperty totalTimeProperty() {
        return totalTime;
    }

    /**
     * Send a string command to media player app
     *
     * @param command the command to send
     */
    private static void sendCommand(String command) {
        if (mediaPlayerProcess != null && mediaPlayerProcess.isAlive()) {
            playerOut.println(command);
            playerOut.flush();
        }
    }
}
