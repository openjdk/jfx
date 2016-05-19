/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.perf;

import javafx.scene.Scene;

import com.sun.javafx.tk.Toolkit;

public abstract class PerformanceTracker {
    /*
     * This class provides a way to track performance metrics such as first
     * paint, instant fps, average fps.<p>
     *
     * Typical use scenario is to obtain the tracker from a {@code Scene}, and use it
     * to get instant or average fps. It is also possible to execute a user
     * function every time the scene is repainted {@see #onPulse}.
     *
     */
    private static SceneAccessor sceneAccessor;

    /*
     * Use method instead of def to avoid explicit initialization which could
     * be circular (this class may be referenced before the toolkit is initialized).
     */
    public static boolean isLoggingEnabled() {
        return Toolkit.getToolkit().getPerformanceTracker().perfLoggingEnabled;
    }

    public static abstract class SceneAccessor {
        public abstract void setPerfTracker(Scene scene, PerformanceTracker tracker);
        public abstract PerformanceTracker getPerfTracker(Scene scene);
    }

    /*
     * Creates a {@code PerformanceTracker} for this scene. There could be only one
     * performance tracker per scene so once a tracker is created for a scene it
     * will be returned for each {@code getSceneTracker} call until the tracker
     * is released with {@link #releaseSceneTracker(Scene)}. <p>
     *
     * @return an instance of {@code PerformanceTracker} associated with the scene
     * or null if the tracker couldn't be created.
     */
    public static PerformanceTracker getSceneTracker(Scene scene) {
        PerformanceTracker tracker = null;
        if (sceneAccessor != null) {
            tracker = sceneAccessor.getPerfTracker(scene);
            if (tracker == null) {
                 tracker = Toolkit.getToolkit().createPerformanceTracker();
            }
            sceneAccessor.setPerfTracker(scene, tracker);
        }
        return tracker;
    }

    /*
     * Removes the tracker from the scene.
     */
    public static void releaseSceneTracker(Scene scene) {
        if (sceneAccessor != null) {
            sceneAccessor.setPerfTracker(scene, null);
        }
    }

    public static void setSceneAccessor(SceneAccessor accessor) {
        sceneAccessor = accessor;
    }

    // TODO: tdv implement media-specific tracker
    //public function getMediaTracker(player : MediaPlayer) : PerformanceTracker {
    //    null;
    //}

    /*
     * Log an event with given description.
     */
    public static void logEvent(String desc) {
        Toolkit.getToolkit().getPerformanceTracker().doLogEvent(desc);
    }

    /*
     * Output full log of events so far.
     */
    public static void outputLog() {
        Toolkit.getToolkit().getPerformanceTracker().doOutputLog();
    }

    private boolean perfLoggingEnabled;
    protected boolean isPerfLoggingEnabled() { return perfLoggingEnabled; }
    protected void setPerfLoggingEnabled(boolean value) { perfLoggingEnabled = value; }

    private boolean firstPulse = true;
    private float instantFPS;
    private int instantFPSFrames;
    private long instantFPSStartTime;
    private long avgStartTime;
    private int avgFramesTotal;
    private float instantPulses;
    private int instantPulsesFrames;
    private long instantPulsesStartTime;
    private long avgPulsesStartTime;
    private int avgPulsesTotal;

    protected abstract long nanoTime();

    public abstract void doOutputLog();

    public abstract void doLogEvent(String s);

    /*
     * Returns the number of frames rendered in the last second or so.
     */
    public synchronized float getInstantFPS() { return instantFPS; }

    /*
     * Returns the average FPS in the time period since the least call
     * to {@link #resetAverageFPS()}.
     */
    public synchronized float getAverageFPS() {
        long nsseconds = nanoTime() - avgStartTime;
        if (nsseconds > 0) {
            return ((avgFramesTotal * 1000000000f) / nsseconds);
        }
        return getInstantFPS();
    }

    public synchronized void resetAverageFPS() {
        avgStartTime = nanoTime();
        avgFramesTotal = 0;
    }

    /*
     * Returns the number of pulses received in the last second or so.
     */
    public float getInstantPulses() { return instantPulses; }

    /*
     * Returns the average pulses per second in the time period since the least call
     * to {@link #resetAveragePulses()}.
     */
    public float getAveragePulses() {
        long nsseconds = nanoTime() - avgPulsesStartTime;
        if (nsseconds > 0) {
            return ((avgPulsesTotal * 1000000000f) / nsseconds);
        }
        return getInstantPulses();
    }

    public void resetAveragePulses() {
        avgPulsesStartTime = nanoTime();
        avgPulsesTotal = 0;
    }

    public void pulse() {
        calcPulses();
        updateInstantFps();
        if (firstPulse) {
            doLogEvent("first repaint");
            firstPulse = false;
            resetAverageFPS();
            resetAveragePulses();
            if (onFirstPulse != null) {
                onFirstPulse.run();
            }
        }

        if (onPulse != null) onPulse.run();
    }

    public void frameRendered() {
        calcFPS();
        if (onRenderedFrameTask != null) {
            onRenderedFrameTask.run();
        }
    }

    private void calcPulses() {
        avgPulsesTotal++;
        instantPulsesFrames++;
        updateInstantPulses();
    }

    private synchronized void calcFPS() {
        avgFramesTotal++;
        instantFPSFrames++;
        updateInstantFps();
    }

    private synchronized void updateInstantFps() {
        long timeSince = nanoTime() - instantFPSStartTime;
        if (timeSince > 1000000000) {
            instantFPS = ((1000000000f * instantFPSFrames) / timeSince);
            instantFPSFrames = 0;
            instantFPSStartTime = nanoTime();
        }
    }

    private void updateInstantPulses() {
        long timeSince = nanoTime() - instantPulsesStartTime;
        if (timeSince > 1000000000) {
            instantPulses = ((1000000000f * instantPulsesFrames) / timeSince);
            instantPulsesFrames = 0;
            instantPulsesStartTime = nanoTime();
        }
    }

    /*
     * Called on every rendering pulse.
     */
    private Runnable onPulse;
    public void setOnPulse(Runnable value) { onPulse = value; }
    public Runnable getOnPulse() { return onPulse; }

    /*
     * Called on the first rendering pulse since this tracker has been created.
     */
    private Runnable onFirstPulse;
    public void setOnFirstPulse(Runnable value) { onFirstPulse = value; }
    public Runnable getOnFirstPulse() { return onFirstPulse; }

    private Runnable onRenderedFrameTask;
    public void setOnRenderedFrameTask(Runnable value) { onRenderedFrameTask = value; }
    public Runnable getOnRenderedFrameTask() { return onRenderedFrameTask; }
}

