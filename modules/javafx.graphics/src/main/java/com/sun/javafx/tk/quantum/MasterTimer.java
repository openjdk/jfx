/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.util.Map;
import javafx.animation.Timeline;
import com.sun.javafx.tk.Toolkit;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.Settings;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.animation.AnimationPulse;

/**
 * This class encapsulates the global static methods that manage scheduling and
 * actual running of animations against real wall clock time. It only deals in
 * absolute time values - all relative times that are specified in the
 * {@link Timeline} class will need to be turned into absolute times when the
 * {@code Timeline} objects are started.
 *
 * For now it is hidden until we have some use to expose it.
 */
public final class MasterTimer extends AbstractMasterTimer {

    /** Prevent external instantiation of MasterTimer */
    private MasterTimer() {
    }

    private static final Object MASTER_TIMER_KEY = new StringBuilder(
            "MasterTimerKey");

    public static synchronized MasterTimer getInstance() {
        Map<Object, Object> contextMap = Toolkit.getToolkit().getContextMap();
        MasterTimer instance = (MasterTimer) contextMap.get(MASTER_TIMER_KEY);
        if (instance == null) {
            instance = new MasterTimer();
            contextMap.put(MASTER_TIMER_KEY, instance);
            if (Settings.getBoolean(ANIMATION_MBEAN_ENABLED,
                                    enableAnimationMBean)) {
                AnimationPulse.getDefaultBean().setEnabled(true);
            }
        }
        return instance;
    }

    /*
     * Called to set the value of PULSE_DURATION or PULSE_DURATION_NS based on
     * the refresh rate of the primary screen (unless overridden by the
     * FRAMERATE_PROP Setting). If the refresh rate can not be determined the
     * default of 60hz is used.
     *
     * @param precision - precision in (1000 for ms or 1000000000 for ns)
     *
     * @return pulse duration value, either in ms or ns depending on the
     * parameter.
     */
    protected int getPulseDuration(int precision) {
        int retVal = precision / 60;
        // Allow Setting to override monitor refresh
        if (Settings.get(FRAMERATE_PROP) != null) {
            int overrideHz = Settings.getInt(FRAMERATE_PROP, 60);
            if (overrideHz > 0) {
                retVal = precision / overrideHz;
            }
        } else if (Settings.get(PULSE_PROP) != null) {
            int overrideHz = Settings.getInt(PULSE_PROP, 60);
            if (overrideHz > 0) {
                retVal = precision / overrideHz;
            }
        } else {
            // If not explicitly set in Settings, try to set based on
            // refresh rate of display
            int rate = Toolkit.getToolkit().getRefreshRate();
            if (rate > 0) {
                retVal = precision / rate;
            }
            // if unknown, use default
        }
        return retVal;
    }

    protected void postUpdateAnimationRunnable(DelayedRunnable animationRunnable) {
        Toolkit.getToolkit().setAnimationRunnable(animationRunnable);
    }

    @Override
    protected void recordStart(long shiftMillis) {
        AnimationPulse.getDefaultBean().recordStart(shiftMillis);
    }

    @Override
    protected void recordEnd() {
        AnimationPulse.getDefaultBean().recordEnd();
    }

    @Override
    protected void recordAnimationEnd() {
        AnimationPulse.getDefaultBean().recordAnimationEnd();
    }
}
