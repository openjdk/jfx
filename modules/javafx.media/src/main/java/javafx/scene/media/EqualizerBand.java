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

package javafx.scene.media;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;

/**
 * The <code>EqualizerBand</code> class provides control for each band in the
 * {@link AudioEqualizer}.
 *
 * @see AudioEqualizer
 * @since JavaFX 2.0
 */
public final class EqualizerBand {
    /**
     * Minimum possible gain value.
     * In the current implementation this value is <code>-24.0</code> dB.
     */
    public static final double MIN_GAIN = com.sun.media.jfxmedia.effects.EqualizerBand.MIN_GAIN;

    /**
     * Maximum possible gain value.
     * In the current implementation this value is <code>12.0</code> dB.
     */
    public static final double MAX_GAIN = com.sun.media.jfxmedia.effects.EqualizerBand.MAX_GAIN;

    /**
     * <code>EqualizerBand</code> default constructor. It creates an instance with
     * <code>centerFrequency</code>, <code>bandwidth</code> and <code>gain</code> set to 0.
     */
    public EqualizerBand() {}

    /**
     * Custom <code>EqualizerBand</code> constructor. It creates an instance
     * from the <code>centerFrequency</code>, <code>bandwidth</code> and
     * <code>gain</code> parameters. The <code>gain</code> specifies the amount
     * of amplification (<code>gain&nbsp;&gt;&nbsp;0.0</code> dB) or attenuation
     * (<code>gain&nbsp;&lt;&nbsp;0.0</code> dB) to be applied to the center frequency of
     * the band. The bandwidth is the frequency spread between the upper and
     * lower edges of the equalizer transfer function which have half the dB gain
     * of the peak (center frequency).
     *
     * @param centerFrequency a positive value specifying the center
     * frequency of the band in Hertz.
     * @param bandwidth a positive value specifying the bandwidth of the band in Hertz.
     * @param gain the gain in decibels to be applied to the band in the range
     * [{@link #MIN_GAIN},&nbsp;{@link #MAX_GAIN}] dB.
     */
    public EqualizerBand(double centerFrequency, double bandwidth, double gain) {
        setCenterFrequency(centerFrequency);
        setBandwidth(bandwidth);
        setGain(gain);
    }
    /*
     * Package private write only property.
     *
     */
    private final Object disposeLock = new Object();
    private com.sun.media.jfxmedia.effects.EqualizerBand jfxBand;
    void setJfxBand(com.sun.media.jfxmedia.effects.EqualizerBand jfxBand) {
        synchronized (disposeLock) {
            this.jfxBand = jfxBand;
        }
    }

    /**
     * Center frequency of the band in Hertz. The default value is
     * <code>0.0</code> Hz.
     */
    private DoubleProperty centerFrequency;


    /**
     * Set the center frequency on the band in Hertz.
     * @param value the center frequency which must be a positive value in Hz.
     */
    public final void setCenterFrequency(double value) {
        centerFrequencyProperty().set(value);
    }

    /**
     * Retrieve the center frequency of the band.
     * @return the center frequency on the band in Hertz.
     */
    public final double getCenterFrequency() {
        return centerFrequency == null ? 0.0 : centerFrequency.get();
    }

    public DoubleProperty centerFrequencyProperty() {
        if (centerFrequency == null) {
            centerFrequency = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        double value = centerFrequency.get();
                        if (jfxBand != null && value > 0.0) {
                            jfxBand.setCenterFrequency(value);
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return EqualizerBand.this;
                }

                @Override
                public String getName() {
                    return "centerFrequency";
                }
            };
        }
        return centerFrequency;
    }

    /**
     * Bandwidth of the band in Hertz. The default value is
     * <code>0.0</code> Hz.
     */
    private DoubleProperty bandwidth;


    /**
     * Set the bandwidth of the band in Hertz.
     * @param value the bandwidth which must be a positive value in Hz.
     */
    public final void setBandwidth(double value) {
        bandwidthProperty().set(value);
    }

    /**
     * Retrieve the bandwidth of the band.
     * @return the bandwidth of the band in Hertz.
     */
    public final double getBandwidth() {
        return bandwidth == null ? 0.0 : bandwidth.get();
    }

    public DoubleProperty bandwidthProperty() {
        if (bandwidth == null) {
            bandwidth = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        double value = bandwidth.get();
                        if (jfxBand != null && value > 0.0) {
                            jfxBand.setBandwidth(value);
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return EqualizerBand.this;
                }

                @Override
                public String getName() {
                    return "bandwidth";
                }
            };
        }
        return bandwidth;
    }

    /**
     * The gain to be applied to the frequencies of this band. The default value
     * is <code>0.0</code> dB.
     */
    private DoubleProperty gain;

    /**
     * Set the gain of the band in dB. Gain property is limited to be
     * within the interval {@link #MIN_GAIN} to {@link #MAX_GAIN}.
     * @param value the gain in the range
     * [{@link #MIN_GAIN},&nbsp;{@link #MAX_GAIN}].
     */
    public final void setGain(double value) {
        gainProperty().set(value);
    }

    /**
     * Retrieve the gain to be applied to the band.
     * @return the gain of the band in dB.
     */
    public final double getGain() {
        return gain == null ? 0.0 : gain.get();
    }

    public DoubleProperty gainProperty() {
        if (gain == null) {
            gain = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (jfxBand != null) {
                            jfxBand.setGain(gain.get());
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return EqualizerBand.this;
                }

                @Override
                public String getName() {
                    return "gain";
                }
            };
        }
        return gain;
    }
}
