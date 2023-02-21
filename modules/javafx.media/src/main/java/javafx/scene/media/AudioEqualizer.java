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

package javafx.scene.media;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;

import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

import com.sun.javafx.collections.VetoableListDecorator;
import com.sun.media.jfxmedia.logging.Logger;

/**
 * The <code>AudioEqualizer</code> class provides audio equalization control for
 * a media player. It contains an {@link ObservableList} of {@link EqualizerBand}
 * elements. Each <code> AudioEqualizer</code> instance is connected to a
 * {@link MediaPlayer} and may be obtained using the
 * {@link MediaPlayer#getAudioEqualizer MediaPlayer.getAudioEqualizer} method.
 *
 * @see MediaPlayer
 * @see EqualizerBand
 * @since JavaFX 2.0
 */
public final class AudioEqualizer {

    /**
     * Maximum number of bands an <code>AudioEqualizer</code> may contain.
     * In the current implementation this value is <code>64</code>.
     */
    public static final int MAX_NUM_BANDS = com.sun.media.jfxmedia.effects.AudioEqualizer.MAX_NUM_BANDS;
    private com.sun.media.jfxmedia.effects.AudioEqualizer jfxEqualizer = null;
    private final ObservableList<EqualizerBand> bands;
    private final Object disposeLock = new Object();

    /**
     * ObservableList containing {@link EqualizerBand} elements. The content of
     * the sequence may be changed by adding or removing {@link EqualizerBand}
     * elements. When adding elements, the user must be prepared to catch
     * {@link IllegalArgumentException}s because
     * any change to the internal list can be vetoed
     * if a newly added instance is not valid.
     *
     * <p>The constraints for a valid {@link EqualizerBand} instance are:</p>
     * <ul>
     * <li>{@link EqualizerBand#centerFrequencyProperty EqualizerBand.centerFrequency} &gt; 0</li>
     * <li>{@link EqualizerBand#bandwidthProperty EqualizerBand.bandwidth} &gt; 0</li>
     * <li>{@link EqualizerBand#MIN_GAIN EqualizerBand.MIN_GAIN} &le;
     * {@link EqualizerBand#gainProperty EqualizerBand.gain} &le;
     * {@link EqualizerBand#MAX_GAIN EqualizerBand.MAX_GAIN}</li>
     * </ul>
     *
     * <p>The default set of bands is as in the following table; all bands have
     * unity gain (0 dB).
     * <table border="1">
     * <caption>AudioEqualizer Band Table</caption>
     * <tr><th scope="col">Band Index</th><th scope="col">Center Frequency (Hz)</th><th scope="col">Bandwidth (Hz)</th></tr>
     * <tr><th scope="row">0</th><td>32</td><td>19</td></tr>
     * <tr><th scope="row">1</th><td>64</td><td>39</td></tr>
     * <tr><th scope="row">2</th><td>125</td><td>78</td></tr>
     * <tr><th scope="row">3</th><td>250</td><td>156</td></tr>
     * <tr><th scope="row">4</th><td>500</td><td>312</td></tr>
     * <tr><th scope="row">5</th><td>1000</td><td>625</td></tr>
     * <tr><th scope="row">6</th><td>2000</td><td>1250</td></tr>
     * <tr><th scope="row">7</th><td>4000</td><td>2500</td></tr>
     * <tr><th scope="row">8</th><td>8000</td><td>5000</td></tr>
     * <tr><th scope="row">9</th><td>16000</td><td>10000</td></tr>
     * </table>
     *
     * @return ObservableList containing {@link EqualizerBand} elements.
     */
    public final ObservableList<EqualizerBand> getBands() {
        return bands;
    }

    AudioEqualizer() {
        bands = new Bands();

        // Add reasonable bands
        bands.addAll(new EqualizerBand(32, 19, 0),
                new EqualizerBand(64, 39, 0),
                new EqualizerBand(125, 78, 0),
                new EqualizerBand(250, 156, 0),
                new EqualizerBand(500, 312, 0),
                new EqualizerBand(1000, 625, 0),
                new EqualizerBand(2000, 1250, 0),
                new EqualizerBand(4000, 2500, 0),
                new EqualizerBand(8000, 5000, 0),
                new EqualizerBand(16000, 10000, 0));
    }

    /**
     * Called by NativePlayer when native part is ready
     *
     * @param jfxEqualizer - Instance of native equalizer
     */
    void setAudioEqualizer(com.sun.media.jfxmedia.effects.AudioEqualizer jfxEqualizer) {
        synchronized (disposeLock) {
            if (this.jfxEqualizer == jfxEqualizer) {
                return;
            }

            if (this.jfxEqualizer != null && jfxEqualizer == null) {
                this.jfxEqualizer.setEnabled(false);
                for (EqualizerBand band : bands) {
                    band.setJfxBand(null);
                }
                this.jfxEqualizer = null;
                return;
            }

            this.jfxEqualizer = jfxEqualizer;

            // Propogate enabled
            jfxEqualizer.setEnabled(isEnabled());
            // Propogate bands
            for (EqualizerBand band : bands) {
                if (band.getCenterFrequency() > 0 && band.getBandwidth() > 0) {
                    com.sun.media.jfxmedia.effects.EqualizerBand jfxBand =
                            jfxEqualizer.addBand(band.getCenterFrequency(),
                            band.getBandwidth(),
                            band.getGain());
                    // setJfxBand will throw an NPE if jfxBand is null which
                    // should never happen.
                    band.setJfxBand(jfxBand);
                } else {
                    Logger.logMsg(Logger.ERROR, "Center frequency [" + band.getCenterFrequency()
                            + "] and bandwidth [" + band.getBandwidth() + "] must be greater than 0.");
                }
            }
        }
    }

    private BooleanProperty enabled;

    public final void setEnabled(boolean value) {
        enabledProperty().set(value);
    }

    public final boolean isEnabled() {
        return enabled == null ? false : enabled.get();
    }

    /**
     * Enables or disables <code>AudioEqualizer</code>. If the enabled property
     * is set to {@code false}, {@code AudioEqualizer} settings are preserved but
     * not taken into account during playback, which is equivalent to setting all
     * {@link EqualizerBand#gainProperty EqualizerBand.gain} properties to zero.
     *
     * @defaultValue {@code true}
     * @return the enabled property
     */
    public BooleanProperty enabledProperty() {
        if (enabled == null) {
            enabled = new BooleanPropertyBase() {

                @Override
                protected void invalidated() {
                    synchronized (disposeLock) {
                        if (jfxEqualizer != null) {
                            jfxEqualizer.setEnabled(enabled.get());
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return AudioEqualizer.this;
                }

                @Override
                public String getName() {
                    return "enabled";
                }
            };
        }
        return enabled;
    }

    private class Bands extends VetoableListDecorator<EqualizerBand> {

        public Bands() {
            super(FXCollections.<EqualizerBand>observableArrayList());
        }

        @Override
        protected void onProposedChange(List<EqualizerBand> toBeAdded, int... toBeRemoved) {
            synchronized (disposeLock) {
                if (jfxEqualizer != null) {
                    for (int i = 0; i < toBeRemoved.length; i += 2) {
                        for (EqualizerBand band : subList(toBeRemoved[i], toBeRemoved[i + 1])) {
                            jfxEqualizer.removeBand(band.getCenterFrequency());
                        }
                    }

                    for (EqualizerBand band : toBeAdded) {
                        if (band.getCenterFrequency() > 0 && band.getBandwidth() > 0) {
                            com.sun.media.jfxmedia.effects.EqualizerBand jfxBand =
                                    jfxEqualizer.addBand(band.getCenterFrequency(),
                                    band.getBandwidth(),
                                    band.getGain());
                            band.setJfxBand(jfxBand);
                        } else {
                            Logger.logMsg(Logger.ERROR, "Center frequency [" + band.getCenterFrequency()
                                    + "] and bandwidth [" + band.getBandwidth() + "] must be greater than 0.");
                        }
                    }
                }
            }
        }
    }
}
