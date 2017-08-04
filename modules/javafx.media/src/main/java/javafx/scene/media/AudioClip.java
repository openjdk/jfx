/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import javafx.beans.NamedArg;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;

/**
 * An <code>AudioClip</code> represents a segment of audio that can be played
 * with minimal latency. Clips are loaded similarly to <code>Media</code>
 * objects but have different behavior, for example, a <code>Media</code> cannot
 * play itself. <code>AudioClip</code>s are also usable immediately. Playback
 * behavior is fire and forget: once one of the play methods is called the only
 * operable control is {@link #stop()}. An <code>AudioClip</code> may also be
 * played multiple times simultaneously. To accomplish the same task using
 * <code>Media</code> one would have to create a new <code>MediaPlayer</code>
 * object for each sound played in parallel. <code>Media</code> objects are
 * however better suited for long-playing sounds. This is primarily because
 * <code>AudioClip</code> stores in memory the raw, uncompressed audio data for
 * the entire sound, which can be quite large for long audio clips. A
 * <code>MediaPlayer</code> will only have enough decompressed audio data
 * pre-rolled in memory to play for a short amount of time so it is much more
 * memory efficient for long clips, especially if they are compressed.
 * <br>
 * <p>Example usage:</p>
 * <pre>{@code
 *     AudioClip plonkSound = new AudioClip("http://somehost/path/plonk.aiff");
 *     plonkSound.play();
 * }</pre>
 *
 * @since JavaFX 2.0
 */

public final class AudioClip {
    private String sourceURL;
    private com.sun.media.jfxmedia.AudioClip audioClip;

    /**
     * Create an <code>AudioClip</code> loaded from the supplied source URL.
     *
     * @param source URL string from which to load the audio clip. This can be an
     * HTTP, HTTPS, FILE or JAR source.
     * @throws NullPointerException if the parameter is <code>null</code>.
     * @throws IllegalArgumentException if the parameter violates
     * <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>.
     * @throws MediaException if there is some other problem loading the media.
     */
    public AudioClip(@NamedArg("source") String source) {
        URI srcURI = URI.create(source);
        sourceURL = source;
        try {
            audioClip = com.sun.media.jfxmedia.AudioClip.load(srcURI);
        } catch(URISyntaxException use) {
            throw new IllegalArgumentException(use);
        } catch(FileNotFoundException fnfe) {
            throw new MediaException(MediaException.Type.MEDIA_UNAVAILABLE, fnfe.getMessage());
        } catch(IOException ioe) {
            throw new MediaException(MediaException.Type.MEDIA_INACCESSIBLE, ioe.getMessage());
        } catch(com.sun.media.jfxmedia.MediaException me) {
            throw new MediaException(MediaException.Type.MEDIA_UNSUPPORTED, me.getMessage());
        }
    }

    /**
     * Get the source URL used to create this <code>AudioClip</code>.
     * @return source URL as provided to the constructor
     */
    public String getSource() {
        return sourceURL;
    }

    /**
     * The relative volume level at which the clip is played. Valid range is 0.0
     * (muted) to 1.0 (full volume). Values are clamped to this range internally
     * so values outside this range will have no additional effect. Volume is
     * controlled by attenuation, so values below 1.0 will reduce the sound
     * level accordingly.
     */
    private DoubleProperty volume;

    /**
     * Set the default volume level. The new setting will only take effect on
     * subsequent plays.
     * @see #volumeProperty()
     * @param value new default volume level for this clip
     */
    public final void setVolume(double value) {
        volumeProperty().set(value);
    }

    /**
     * Get the default volume level.
     * @see #volumeProperty()
     * @return the default volume level for this clip
     */
    public final double getVolume() {
        return (null == volume) ? 1.0 : volume.get();
    }
    public DoubleProperty volumeProperty() {
        if (volume == null) {
            volume = new DoublePropertyBase(1.0) {
                @Override
                protected void invalidated() {
                    if (null != audioClip) {
                        audioClip.setVolume(volume.get());
                    }
                }

                @Override
                public Object getBean() {
                    return AudioClip.this;
                }

                @Override
                public String getName() {
                    return "volume";
                }
            };
        }
        return volume;
    }

    /**
     * The relative left and right volume levels of the clip.
     * Valid range is -1.0 to 1.0 where -1.0 gives full volume to the left
     * channel while muting the right channel, 0.0 gives full volume to both
     * channels and 1.0 gives full volume to right channel and mutes the left
     * channel. Values outside this range are clamped internally.
     */
    private DoubleProperty balance;

    /**
     * Set the default balance level. The new value will only affect subsequent
     * plays.
     * @see #balanceProperty()
     * @param balance new default balance
     */
    public void setBalance(double balance) {
        balanceProperty().set(balance);
    }

    /**
     * Get the default balance level for this clip.
     * @see #balanceProperty()
     * @return the default balance for this clip
     */
    public double getBalance() {
        return (null != balance) ? balance.get() : 0.0;
    }
    public DoubleProperty balanceProperty() {
        if (null == balance) {
            balance = new DoublePropertyBase(0.0) {
                @Override
                protected void invalidated() {
                    if (null != audioClip) {
                        audioClip.setBalance(balance.get());
                    }
                }

                @Override
                public Object getBean() {
                    return AudioClip.this;
                }

                @Override
                public String getName() {
                    return "balance";
                }
            };
        }
        return balance;
    }

    /**
     * The relative rate at which the clip is played. Valid range is 0.125
     * (1/8 speed) to 8.0 (8x speed); values outside this range are clamped
     * internally. Normal playback for a clip is 1.0; any other rate will affect
     * pitch and duration accordingly.
     */
    private DoubleProperty rate;

    /**
     * Set the default playback rate. The new value will only affect subsequent
     * plays.
     * @see #rateProperty()
     * @param rate the new default playback rate
     */
    public void setRate(double rate) {
        rateProperty().set(rate);
    }

    /**
     * Get the default playback rate.
     * @see #rateProperty()
     * @return default playback rate for this clip
     */
    public double getRate() {
        return (null != rate) ? rate.get() : 1.0;
    }
    public DoubleProperty rateProperty() {
        if (null == rate) {
            rate = new DoublePropertyBase(1.0) {
                @Override
                protected void invalidated() {
                    if (null != audioClip) {
                        audioClip.setPlaybackRate(rate.get());
                    }
                }

                @Override
                public Object getBean() {
                    return AudioClip.this;
                }

                @Override
                public String getName() {
                    return "rate";
                }
            };
        }
        return rate;
    }

    /**
     * The relative "center" of the clip. A pan value of 0.0 plays
     * the clip normally where a -1.0 pan shifts the clip entirely to the left
     * channel and 1.0 shifts entirely to the right channel. Unlike balance this
     * setting mixes both channels so neither channel loses data. Setting
     * pan on a mono clip has the same effect as setting balance, but with a
     * much higher cost in CPU overhead so this is not recommended for mono
     * clips.
     */
    private DoubleProperty pan;

    /**
     * Set the default pan value. The new value will only affect subsequent
     * plays.
     * @see #panProperty()
     * @param pan the new default pan value
     */
    public void setPan(double pan) {
        panProperty().set(pan);
    }

    /**
     * Get the default pan value.
     * @see #panProperty()
     * @return the default pan value for this clip
     */
    public double getPan() {
        return (null != pan) ? pan.get() : 0.0;
    }
    public DoubleProperty panProperty() {
        if (null == pan) {
            pan = new DoublePropertyBase(0.0) {
                @Override
                protected void invalidated() {
                    if (null != audioClip) {
                        audioClip.setPan(pan.get());
                    }
                }

                @Override
                public Object getBean() {
                    return AudioClip.this;
                }

                @Override
                public String getName() {
                    return "pan";
                }
            };
        }
        return pan;
    }

    /**
     * The relative priority of the clip with respect to other clips. This value
     * is used to determine which clips to remove when the maximum allowed number
     * of clips is exceeded. The lower the priority, the more likely the
     * clip is to be stopped and removed from the mixer channel it is occupying.
     * Valid range is any integer; there are no constraints. The default priority
     * is zero for all clips until changed. The number of simultaneous sounds
     * that can be played is implementation- and possibly system-dependent.
     */
    private IntegerProperty priority;

    /**
     * Set the default playback priority. The new value will only affect
     * subsequent plays.
     * @see #priorityProperty()
     * @param priority the new default playback priority
     */
    public void setPriority(int priority) {
        priorityProperty().set(priority);
    }

    /**
     * Get the default playback priority.
     * @see #priorityProperty()
     * @return the default playback priority of this clip
     */
    public int getPriority() {
        return (null != priority) ? priority.get() : 0;
    }
    public IntegerProperty priorityProperty() {
        if (null == priority) {
            priority = new IntegerPropertyBase(0) {
                @Override
                protected void invalidated() {
                    if (null != audioClip) {
                        audioClip.setPriority(priority.get());
                    }
                }

                @Override
                public Object getBean() {
                    return AudioClip.this;
                }

                @Override
                public String getName() {
                    return "priority";
                }
            };
        }
        return priority;
    }

    /**
     * When {@link #cycleCountProperty cycleCount} is set to this value, the
     * <code>AudioClip</code> will loop continuously until stopped. This value is
     * synonymous with {@link MediaPlayer#INDEFINITE} and
     * {@link javafx.animation.Animation#INDEFINITE}, these values may be used
     * interchangeably.
     */
    public static final int INDEFINITE = -1;

    /**
     * The number of times the clip will be played when {@link #play()}
     * is called. A cycleCount of 1 plays exactly once, a cycleCount of 2
     * plays twice and so on. Valid range is 1 or more, but setting this to
     * {@link #INDEFINITE INDEFINITE} will cause the clip to continue looping
     * until {@link #stop} is called.
     */
    private IntegerProperty cycleCount;

    /**
     * Set the default cycle count. The new value will only affect subsequent
     * plays.
     * @see #cycleCountProperty()
     * @param count the new default cycle count for this clip
     */
    public void setCycleCount(int count) {
        cycleCountProperty().set(count);
    }

    /**
     * Get the default cycle count.
     * @see #cycleCountProperty()
     * @return the default cycleCount for this audio clip
     */
    public int getCycleCount() {
        return (null != cycleCount) ? cycleCount.get() : 1;
    }
    public IntegerProperty cycleCountProperty() {
        if (null == cycleCount) {
            cycleCount = new IntegerPropertyBase(1) {
                @Override
                protected void invalidated() {
                    if (null != audioClip) {
                        int value = cycleCount.get();
                        if (INDEFINITE != value) {
                            value = Math.max(1, value);
                            audioClip.setLoopCount(value - 1);
                        } else {
                            audioClip.setLoopCount(value); // INDEFINITE is the same there
                        }
                    }
                }

                @Override
                public Object getBean() {
                    return AudioClip.this;
                }

                @Override
                public String getName() {
                    return "cycleCount";
                }
            };
        }
        return cycleCount;
    }
    /**
     * Play the <code>AudioClip</code> using all the default parameters.
     */
    public void play() {
        if (null != audioClip) {
            audioClip.play();
        }
    }

    /**
     * Play the <code>AudioClip</code> using all the default parameters except volume.
     * This method does not modify the clip's default parameters.
     * @param volume the volume level at which to play the clip
     */
    public void play(double volume) {
        if (null != audioClip) {
            audioClip.play(volume);
        }
    }

    /**
     * Play the <code>AudioClip</code> using the given parameters. Values outside
     * the ranges as specified by their associated properties are clamped.
     * This method does not modify the clip's default parameters.
     *
     * @param volume Volume level at which to play this clip. Valid volume range is
     * 0.0 to 1.0, where 0.0 is effectively muted and 1.0 is full volume.
     * @param balance Left/right balance or relative channel volumes for stereo
     * effects.
     * @param rate Playback rate multiplier. 1.0 will play at the normal
     * rate while 2.0 will double the rate.
     * @param pan Left/right shift to be applied to the clip. A pan value of
     * -1.0 means full left channel, 1.0 means full right channel, 0.0 has no
     * effect.
     * @param priority Audio effect priority. Lower priority effects will be
     * dropped first if too many effects are trying to play simultaneously.
     */
    public void play(double volume, double balance, double rate, double pan, int priority) {
        if (null != audioClip) {
            audioClip.play(volume, balance, rate, pan, audioClip.loopCount(), priority);
        }
    }

    /**
     * Indicate whether this <code>AudioClip</code> is playing. If this returns true
     * then <code>play()</code> has been called at least once and it is still playing.
     * @return true if any mixer channel has this clip queued, false otherwise
     */
    public boolean isPlaying() {
        return null != audioClip && audioClip.isPlaying();
    }

    /**
     * Immediately stop all playback of this <code>AudioClip</code>.
     */
    public void stop() {
        if (null != audioClip) {
            audioClip.stop();
        }
    }
}
