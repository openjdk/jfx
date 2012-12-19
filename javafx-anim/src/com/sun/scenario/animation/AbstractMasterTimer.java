/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.animation;

import com.sun.javafx.animation.TickCalculation;
import javafx.animation.AnimationTimer;

import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.Settings;
import com.sun.scenario.animation.shared.CurrentTime;
import com.sun.scenario.animation.shared.PulseReceiver;
import java.util.Arrays;
import javafx.util.Callback;

public abstract class AbstractMasterTimer {

    // This PropertyChangeListener is added to Settings to listen for changes
    // to the nogap and fullspeed properties.
    private static Callback<String, Void> pcl = new Callback<String, Void>() {
        public Void call(String key) {
            if (key.equals(FULLSPEED_PROP)) {
                fullspeed = Settings.getBoolean(FULLSPEED_PROP);
            } else if (key.equals(ADAPTIVE_PULSE_PROP)) {
                useAdaptivePulse = Settings.getBoolean(ADAPTIVE_PULSE_PROP);
            } else if (key.equals(ANIMATION_MBEAN_ENABLED)) {
                AnimationPulse.getDefaultBean()
                  .setEnabled(Settings.getBoolean(ANIMATION_MBEAN_ENABLED));
            }
            return null;
        }
    };
    
    protected final static String FULLSPEED_PROP = "javafx.animation.fullspeed";
    private static boolean fullspeed = Settings.getBoolean(FULLSPEED_PROP);

    // enables the code path which estimates the next pulse time to be just
    // enough in advance of the vsync to complete rendering before it happens
    protected final static String ADAPTIVE_PULSE_PROP = "com.sun.scenario.animation.adaptivepulse";
    private static boolean useAdaptivePulse = Settings.getBoolean(ADAPTIVE_PULSE_PROP);

    // another property which is controlling whether vsync is enabled:
    // "com.sun.scenario.animation.vsync". if true, JSGPanel will enable vsync
    // for the toplevel it's in. See JSGPanel.

    // properties to override the default pulse rate (set in hz - number of
    // pulses per second)
    protected final static String PULSE_PROP = "javafx.animation.pulse";
    protected final static String FRAMERATE_PROP = "javafx.animation.framerate";
    protected final static String FIXED_PULSE_LENGHT_PROP = "com.sun.scenario.animation.fixed.pulse.length";

    // property to enable AnimationPulse data gathering
    // note: it can be enabled via the MBean itself too
    protected final static String ANIMATION_MBEAN_ENABLED = "com.sun.scenario.animation.AnimationMBean.enabled";
    protected final static boolean enableAnimationMBean = false;

    protected final int PULSE_DURATION = getPulseDuration(1000);
    protected final int PULSE_DURATION_NS = getPulseDuration(1000000000);
    protected final int PULSE_DURATION_TICKS = getPulseDuration((int)TickCalculation.fromMillis(1000));

    private boolean paused = false;
    private long totalPausedTime;
    private long startPauseTime;

    private PulseReceiver receivers[] = new PulseReceiver[2];
    private int receiversLength;
    private boolean receiversLocked;

    // synchronize to update frameJobList and frameJobs
    private AnimationTimer animationTimers[] = new AnimationTimer[2]; // frameJobList
                                                                     // snapshot
    private int animationTimersLength;
    private boolean animationTimersLocked;

    private Runnable activeAnimationHandler;
    
    // There is a memory leak possibility by having this be non-static, as
    // Clip has a ref to this and this (may) have a ref to the enclosing class.
    // But having it static has initialization issues (need to call
    // shouldUseNanoTime() in the subclass during static init of the
    // superclass).
    // Moving to a true singleton model might help...
    private final/* static */CurrentTime currentTime = createCurrentTime();

    private final MainLoop theMaster = new MainLoop();


    static {
        Settings.addPropertyChangeListener(pcl);
        int pulse = Settings.getInt(PULSE_PROP, -1);
        if (pulse != -1) {
            System.err.println("Setting PULSE_DURATION to " + pulse + " hz");
        }
    }

    // Used by Clip.create() method that doesn't take a resolution argument
    public int getDefaultResolution() {
        return PULSE_DURATION_TICKS;
    }
    
    protected CurrentTime createCurrentTime() {
        if (Boolean.getBoolean(FIXED_PULSE_LENGHT_PROP)) { // Fixed time pulse is for debugging purposes only
            FixedPulseTime time = new FixedPulseTime(PULSE_DURATION_NS);
            return time;
        }
        return shouldUseNanoTime()? new NanoCurrentTime() : new MilliCurrentTime();
    }
    
    public void pause() {
        if (!paused) {
            startPauseTime = nanos();
            paused = true;
        }
    }
    
    public void resume() {
        if (paused) {
            paused = false;
            totalPausedTime += nanos() - startPauseTime;
        }
    }
    
    public long nanos() {
        return paused? startPauseTime : currentTime.nanos() - totalPausedTime;
    }

    public boolean isFullspeed() {
        return fullspeed;
    }

    protected abstract boolean shouldUseNanoTime();
    
    /** Prevent external instantiation of MasterTimer */
    protected AbstractMasterTimer() {
        if (Boolean.getBoolean(FIXED_PULSE_LENGHT_PROP)) { // Fixed time pulse is for debugging purposes only
            addPulseReceiver((FixedPulseTime)currentTime);
        }
    }
    
    // new methods to interact with Toolkit, will replace current interaction later
    // for now they call the respecive methods of the old implementation
    
    // triggers an animation cycle, called by the platform
    public void animationTrigger() {
        theMaster.run();
    }
    
    // return the delay to when the next pulse should be triggered
    public long getNextPulseTime() {
        return (theMaster.getDelay());
    }
    
    public boolean isActive() {
        return !theMaster.inactive;
    }
    
    public void setActiveAnimationHandler(Runnable activeAnimationHandler) {
        this.activeAnimationHandler = activeAnimationHandler;
        
    }

    /**
     * Adds a PulseReceiver to the list of targets being tracked against the
     * global schedule. The target should already have an absolute start time
     * recorded in it and that time will be used to start the clip at the
     * appropriate wall clock time as defined by milliTime().
     * 
     * Note that pulseReceiver cannot be removed from the MasterTimer directly.
     * It is removed automatically in the timePulse-iteration if timePulse
     * returns true.
     * 
     * @param target
     *            the Clip to be added to the scheduling queue
     */
    public void addPulseReceiver(PulseReceiver target) {
        boolean needMoreSize = receiversLength == receivers.length;
        if (receiversLocked || needMoreSize) {
            receivers = Arrays.copyOf(receivers, needMoreSize ? receivers.length * 3 / 2 + 1 : receivers.length);
            receiversLocked = false;
        }
        receivers[receiversLength++] = target;
        if (receiversLength == 1) {
            theMaster.updateAnimationRunnable();
        }
    }

    public void removePulseReceiver(PulseReceiver target) {
        if (receiversLocked) { 
            receivers = receivers.clone();
            receiversLocked = false;
        }
        for (int i = 0; i < receiversLength; ++i) {
            if (target == receivers[i]) {
                if (i == receiversLength - 1) {
                    receivers[i] = null;
                } else {
                    System.arraycopy(receivers, i + 1, receivers, i, receiversLength - i - 1);
                }
                --receiversLength;
                break;
            }
        }
        if (receiversLength == 0) {
            theMaster.updateAnimationRunnable();
        }
    }

    public void addAnimationTimer(AnimationTimer timer) {
        boolean needMoreSize = animationTimersLength == animationTimers.length;
        if (animationTimersLocked || needMoreSize) {
            animationTimers = Arrays.copyOf(animationTimers, needMoreSize ? animationTimers.length * 3 / 2 + 1 : animationTimers.length);
            animationTimersLocked = false;
        }
        animationTimers[animationTimersLength++] = timer;
        if (animationTimersLength == 1) {
            theMaster.updateAnimationRunnable();
        }
    }

    public void removeAnimationTimer(AnimationTimer timer) {
        if (animationTimersLocked) { 
            animationTimers = animationTimers.clone();
            animationTimersLocked = false;
        }
        for (int i = 0; i < animationTimersLength; ++i) {
            if (timer == animationTimers[i]) {
                if (i == animationTimersLength - 1) {
                    animationTimers[i] = null;
                } else {
                    System.arraycopy(animationTimers, i + 1, animationTimers, i, animationTimersLength - i - 1);
                }
                --animationTimersLength;
                break;
            }
        }
        if (animationTimersLength == 0) {
            theMaster.updateAnimationRunnable();
        }
    }

    @Deprecated
    public void notifyJobsReady() {
        postUpdateAnimationRunnable(theMaster);
    }

    /*
     * methods to record times for different stages of a pulse overriden in
     * MasterTimer to collect data for AnimationPulse Mbean
     */
    protected void recordStart(long shiftMillis) {
    }

    protected void recordEnd() {
    }

    protected void recordAnimationEnd() {
    }

    /**
     * Hidden inner class to run the main timing loop. This is the
     * "AnimationRunnable" for Desktop and TV
     */
    private final class MainLoop implements DelayedRunnable {

        private boolean inactive = true;
        
        private long nextPulseTime = nanos();
        private long lastPulseDuration = Integer.MIN_VALUE;

        @Override
        public void run() {
            if (paused) {
                return;
            }
            final long now = nanos();
            recordStart((nextPulseTime - now) / 1000000);
            timePulseImpl(now);
            recordEnd();
            updateNextPulseTime(now);
            // reschedule animation runnable if needed
            updateAnimationRunnable();
        }

        @Override
        public long getDelay() {
            final long now = nanos();
            final long timeUntilPulse = (nextPulseTime - now) / 1000000;
            return Math.max(0, timeUntilPulse);
        }

        private void updateNextPulseTime(long pulseStarted) {
            final long now = nanos();
            if (fullspeed) {
                nextPulseTime = now;
            } else {
                if (useAdaptivePulse) {
                    // Estimate the next pulse time such that we wake up just
                    // early enough to finish up the painting and call swap
                    // before vsync happens. We try to minimize the amount of
                    // time we wait for vsync blocking the EDT thread.
                    nextPulseTime += PULSE_DURATION_NS;
                    long pulseDuration = now - pulseStarted;
                    // if the new duration was smaller than the previous one
                    // we don't need to do anything (we have decreased the
                    // duration), but if it's longer to within 1/2ms then we
                    // try to halve the next anticipated duration (but not
                    // closer
                    // than 2ms within the next expected pulse)
                    if (pulseDuration - lastPulseDuration > 500000) {
                        pulseDuration /= 2;
                    }
                    if (pulseDuration < 2000000) {
                        pulseDuration = 2000000;
                    }
                    // if the pulse took longer than pulse_duration_ns we
                    // probably missed the vsync
                    if (pulseDuration >= PULSE_DURATION_NS) {
                        pulseDuration = 3 * PULSE_DURATION_NS / 4;
                    }
                    lastPulseDuration = pulseDuration;
                    nextPulseTime = nextPulseTime - pulseDuration;
                } else {
                    nextPulseTime = ((nextPulseTime + PULSE_DURATION_NS) / PULSE_DURATION_NS)
                            * PULSE_DURATION_NS;
                }
            }
        }

        private void updateAnimationRunnable() {
            final boolean newInactive = (animationTimersLength == 0 && receiversLength == 0);
            if (inactive != newInactive) {
                inactive = newInactive;
                final DelayedRunnable animationRunnable = inactive? null : this;
                if (activeAnimationHandler != null) {
                    activeAnimationHandler.run();
                }
                postUpdateAnimationRunnable(animationRunnable);
            }
        }
    }

    protected abstract void postUpdateAnimationRunnable(
            DelayedRunnable animationRunnable);

    protected abstract int getPulseDuration(int precision);

    protected void timePulseImpl(long now) {
        final PulseReceiver receiversSnapshot[] = receivers;
        final int rLength = receiversLength;
        try {
            receiversLocked = true;
            for (int i = 0; i < rLength; i++) {
                receiversSnapshot[i].timePulse(TickCalculation.fromNano(now));
            }
        } finally {
            receiversLocked = false;
        }
        recordAnimationEnd();

        final AnimationTimer animationTimersSnapshot[] = animationTimers;
        final int aTLength = animationTimersLength;
        try {
            animationTimersLocked = true;
            // After every frame, call any frame jobs
            for (int i = 0; i < aTLength; i++) {
                animationTimersSnapshot[i].handle(now);
            }
        } finally {
            animationTimersLocked = false;
        }
    }

}
