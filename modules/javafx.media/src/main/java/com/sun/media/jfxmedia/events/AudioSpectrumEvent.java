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

package com.sun.media.jfxmedia.events;

import com.sun.media.jfxmedia.effects.AudioSpectrum;

public class AudioSpectrumEvent extends PlayerEvent {
    private AudioSpectrum source;
    private double        timestamp;
    private double        duration;
    private boolean       queryTimestamp;

    /*
     * Value of timestamp will be ignored if queryTimestamp is set true and
     * timestamp will be requested from EventQueueThread when spectrum event is
     * received instead. We do not use -1.0 (GST_CLOCK_TIME_NONE), since
     * GStreamer might send us such events in case if something fails, so we using
     * queryTimestamp to know for sure that we need to ask for timestamp from
     * event queue. Note: Only OSX platfrom sets it true. GStreamer platfrom
     * should not use it unless such usage is tested.
     */
    public AudioSpectrumEvent(AudioSpectrum source, double timestamp,
                              double duration, boolean queryTimestamp) {
        this.source = source;
        this.timestamp = timestamp;
        this.duration = duration;
        this.queryTimestamp = queryTimestamp;
    }

    public final AudioSpectrum getSource() {
        return source;
    }

    public final void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public final double getTimestamp() {
        return timestamp;
    }

    public final double getDuration() {
        return duration;
    }

    public final boolean queryTimestamp() {
        return queryTimestamp;
    }
}
