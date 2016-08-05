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

package com.sun.media.jfxmedia;

import com.sun.media.jfxmediaimpl.AudioClipProvider;
import java.io.IOException;
import java.net.URI;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

/**
 * <code>AudioClip</code>s are short segments of audio that can be
 * played quickly and on-demand. Though loading of compressed audio formats may
 * be supported, all AudioClip data is stored uncompressed internally to avoid
 * decoding latency. The system is currently capable of playing up to
 * 16 <code>AudioClip</code>s simultaneously.
 *
 * <p><b>Core AudioClip features:</b>
 * <list>
 *  <li>Near-zero latency playback, playback begins as soon as it's called, decoding is done at load time</li>
 *  <li>Play clips up to ten seconds, though clips of any length may be loaded</li>
 *  <li>Quick and simple "one shot" playback interface, just load the effect and play, cleanup is automatic</li>
 *  <li>Play/pause/stop playback of effect, each playback instance is unique and can be controlled individually</li>
 *  <li>Each effect can be played multiple times simultaneously</li>
 *  <li>Loop playback (to some maximum amount?)</li>
 *  <li>Vary rate/pitch</li>
 *  <li>Playback volume</li>
 *  <li>Effect balance</li>
 *  <li>Pan left/right</li>
 *  <li>Mix N audio effects for simultaneous playback (N TBD, start with 32)</li>
 *  <li>Audio effect priority, lower priority effects get dropped first when playback threshold is reached</li>
 * </list><br><hr><br>
 *
 * <b>Loading sound effects</b>
 * <pre><code>
 * SoundEffectBank soundBank = /&#42; TBD &#42;/ ;
 * AudioClip ding = soundBank.effectNamed("ding");
 * AudioClip dong = soundBank.effectNamed("dong");
 * </code></pre>
 *
 * <b>OR</b><br>
 *
 * <pre><code>
 * import javafx.scene.media.Media;
 * import javafx.scene.media.AudioClip;
 *
 * AudioClip ding = new AudioClip("http://somehost.com/sounds/ding.aiff");
 * AudioClip dong = new AudioClip("jar:http://host/path/some.jar!/resources/sounds/dong.aiff");
 *
 *      // Or you can use the ClassLoader to get a resource URI for resources bundled inside your application jar file
 * AudioClip bonk = new AudioClip(this.getClass().getClassLoader().getResource("sounds/bonk.wav"));
 *
 *      // You can also load from a Media object
 * Media whizMedia = new Media("http://somehost.com/sounds/whiz.aiff");
 * AudioClip whiz = new AudioClip(whizMedia);
 * </code></pre>
 *
 * <p><b>Playing sound effects</b>
 * <p>Single play (fire and forget method):
 * <pre>
 * ding.play();
 * dong.play(); // the two sounds play simultaneously, there is no further control over playback
 * </pre>
 *
 * <br>Chaining multiple effects:
 * <pre>
 * ding.append(dong).play(); // ding and dong play sequentially, when finished the player is automatically disposed of
 * ding.append(dong).append(ding).append(dong).append(ding).append(dong).play(); // really impatient person at the door :)
 * AudioClip dingDong = ding.append(dong).flatten(); // dingDong is completely independent of ding and dong
 * </pre>
 *
 */

public abstract class AudioClip {
    // default playback parameters
    protected int clipPriority = 0;
    protected int loopCount = 0;
    protected double clipVolume = 1.0;
    protected double clipBalance = 0.0;
    protected double clipRate = 1.0;
    protected double clipPan = 0.0;

    /*
     * Supported LPCM sample formats, all formats here can be loaded, but the
     * underlying implementation may convert to an optimal format internally.
     */
    public static final int SAMPLE_FORMAT_S8 = 0;       /** Signed 8 bit LPCM */
    public static final int SAMPLE_FORMAT_U8 = 1;       /** Unsigned 8 bit LPCM */
    public static final int SAMPLE_FORMAT_S16BE = 2;    /** Signed 16 bit LPCM, big endian byte order */
    public static final int SAMPLE_FORMAT_U16BE = 3;    /** Unsigned 16 bit LPCM, big endian byte order */
    public static final int SAMPLE_FORMAT_S16LE = 4;    /** Signed 16 bit LPCM, little endian byte order */
    public static final int SAMPLE_FORMAT_U16LE = 5;    /** Unsigned 16 bit LPCM, little endian byte order */
    public static final int SAMPLE_FORMAT_S24BE = 6;    /** Signed 24 bit LPCM, big endian byte order */
    public static final int SAMPLE_FORMAT_U24BE = 7;    /** Unsigned 24 bit LPCM, big endian byte order */
    public static final int SAMPLE_FORMAT_S24LE = 8;    /** Signed 24 bit LPCM, little endian byte order */
    public static final int SAMPLE_FORMAT_U24LE = 9;    /** Unsigned 24 bit LPCM, little endian byte order */
    // FIXME: float formats (F32LE, F32BE)

    // FIXME: add properties; sample size, sample playbackRate, duration, etc...

    /**
     * Load an audio clip from the specified source URI. Currently supported formats
     * are AIFF and WAV files. Audio data in the file must be raw PCM, loading
     * compressed files is not supported at this time.
     *
     * @param source URI to the desired clip.
     * @return AudioClip ready to play.
     * @throws IOException If an error occurred while loading the clip.
     * @throws IllegalArgumentException If an invalid URI is provided.
     */
    public static AudioClip load(URI source) throws URISyntaxException, FileNotFoundException, IOException {
        return AudioClipProvider.getProvider().load(source);
    }

    /**
     * Generate a AudioClip from raw LPCM audio data. This can be used to
     * programmatically create sound effects, either generated mathematically
     * or loaded from some arbitrary source. Multiple channels must be
     * interleaved properly, for stereo audio left channel is always first.
     *
     * @param data Raw PCM samples stored in a byte array.
     * @param dataOffset Byte offset into data that the sample data starts.
     * @param sampleCount Number of LPCM samples stored in data.
     * @param sampleFormat Raw format that the LPCM data is being provided in.
     * This may not be the actual format stored internally.
     * @param channels The number of channels. Currently only two channel
     * audio is supported, channels beyond two are simply dropped.
     * @param sampleRate Audio sample playbackRate of the raw LPCM data.
     *
     * @return A new AudioClip that can play the given raw LPCM audio data.
     * @throws IllegalArgumentException If an AudioClip cannot be created with
     * the given arguments.
     */
    public static AudioClip create(byte [] data, int dataOffset, int sampleCount, int sampleFormat, int channels, int sampleRate)
            throws IllegalArgumentException
    {
        return AudioClipProvider.getProvider().create(data, dataOffset, sampleCount, sampleFormat, channels, sampleRate);
    }

    /**
     * Stop all AudioClips that are currently playing.
     */
    public static void stopAllClips() {
        AudioClipProvider.getProvider().stopAllClips();
    }

    /**
     * Create a new AudioClip from a segment of an existing AudioClip. The new
     * clip may copy or simply reference this existing clip, so be aware that
     * this operation may have a severe memory usage impact for long segments.
     * The default parameters are copied to the new clip. This variant specifies
     * the start and end times of the segment. Specifying an end time beyond the
     * duration of the clip will crop the new AudioClip to the end of the source
     * clip. Specifying a start time less than zero or greater than end time is
     * undefined and will return null.
     *
     * @param startTime The start time for the segment
     * @param stopTime The end time of the segment or -1.0 for the remainder of
     * the source clip.
     *
     * @return A new AudioClip instance containing only the specified segment of
     * the source clip
     * @throws IllegalArgumentException If startTime or stopTime are not valid
     * for this AudioClip.
     */
    public abstract AudioClip createSegment(double startTime, double stopTime) throws IllegalArgumentException;

    /**
     * Create a new AudioClip from a segment of an existing AudioClip. The new
     * clip may copy or simply reference this existing clip, so be aware that
     * this operation may have a severe memory usage impact for long segments.
     * The default parameters are copied to the new clip. This variant specifies
     * the exact sample offsets of the segment, starting at sample zero. If
     * endSample is greater than the number of samples in the source clip, it
     * will be cropped to the last sample. Specifying a start sample less than
     * zero or higher than endSample is undefined and will return null.
     *
     * @param startSample The starting audio sample for the segment
     * @param endSample The ending audio sample of the segment, or -1 for the
     * remainder of the source clip.
     *
     * @return A new AudioClip instance containing only the specified segment of
     * the source clip
     * @throws IllegalArgumentException If the given sample range is invalid for
     * this AudioClip.
     */
    public abstract AudioClip createSegment(int startSample, int endSample) throws IllegalArgumentException;

    /**
     * Create a new AudioClip from an existing clip by resampling a segment of
     * the source clip to the specified sample playbackRate.
     *
     * @param startSample starting sample to begin resampling at. The first
     * sample is always zero, negative values are not allowed.
     * @param endSample The last sample to resample or -1 for the remainder of
     * the source clip.
     * @param newSampleRate The sample playbackRate to create the new AudioClip at.
     *
     * @return A new AudioClip that contains a copy of the source AudioClip
     * resampled to the new sample playbackRate.
     * @throws IllegalArgumentException If the sample range is invalid for this
     * AudioClip, or the new sample rate is not supported.
     * @throws IOException If an error occurred during rate conversion.
     */
    public abstract AudioClip resample(int startSample, int endSample, int newSampleRate)
            throws IllegalArgumentException, IOException;

    /**
     * Create a new AudioClip by appending the given clip to the current clip.
     * The new clip is independent of the two source clips. If the sample rates
     * are mismatched, the new clip will contain at least one resampled copy of
     * a source clip. Which clip is resampled is implementation dependent.
     *
     * @param clip The clip to be appended to the current clip.
     *
     * @return A new AudioClip that contains the concatenation of the two source
     * clips.
     * @throws IOException If an error occurred during the concatenation,
     * generally during rate conversion if it's necessary.
     */
    public abstract AudioClip append(AudioClip clip)
            throws IOException;

    /**
     * Creates a completely independent AudioClip. Any references contained
     * will be copied and the references removed. The result will be a single
     * AudioEffect containing all the audio data required to produce the effect.
     * If the effect is already independent, then this will simply return the
     * same AudioClip.
     *
     * @return A new AudioClip that is independent of all other clips.
     */
    public abstract AudioClip flatten();

    public int priority() {
        return this.clipPriority;
    }
    public void setPriority(int prio) {
        this.clipPriority = prio;
    }

    /**
     * Get the number of times the associated AudioClip will repeat when
     * played. Note that if you start a clip looping indefinitely the only way
     * to stop is to call AudioClip.stop() to stop all playback of a clip.
     *
     * @return The number of times a AudioClip will be repeated when played.
     */
    public int loopCount() {
        return this.loopCount;
    }

    /**
     * Specify the number of times a AudioClip should be repeated when this
     * player is played. Note that if you start a clip looping indefinitely
     * the only way to stop is to call AudioClip.stop() to stop all playback of
     * a clip.
     *
     * @param loopCount How many times to repeat the AudioClip during normal
     * playback. If this is zero, then the AudioClip will play exactly once
     * and stop. Set this to -1 to repeat indefinitely, other negative values
     * are undefined.
     */
    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    /**
     * Returns playback volume.
     *
     * @return Volume level.
     */
    public double volume() {
        return this.clipVolume;
    }

    /**
     * Set the volume level for playback. Volume control is by attenuation,
     * a volume of 1.0 is full volume where 0.0 is effectively muted.
     *
     * @param volume
     */
    public void setVolume(double vol) {
        this.clipVolume = vol;
    }

    /**
     * Returns the left/right channel balance.
     *
     * @return
     */
    public double balance() {
        return this.clipBalance;
    }

    /**
     * Set left/right balance or relative channel volumes for stereo effects. A
     * value of 0.0 is equal levels (both channels at full volume), -1.0 is full
     * left channel with muted right, 1.0 is full right channel with muted left.
     * @param balance Balance value.
     */
    public void setBalance(double bal) {
        this.clipBalance = bal;
    }

    /**
     * Gets the audio sample rate multiplier that this player will use while
     * playing the associated AudioClip.
     *
     * @return Audio playback sample rate multiplier.
     *
     * @see #setPlaybackRate(double)
     */
    public double playbackRate() {
        return this.clipRate;
    }

    /**
     * Set the audio sample rate multiplier. The player will use this multiplier
     * when mixing audio to effectively modify the playback rate. A value of 1.0
     * plays the AudioClip at it's normal sample rate. For example, setting
     * the playback rate of a AudioClip that normally plays at 48 KHz to 0.5
     * will cause the player to effectively play the AudioClip at 24 KHz.
     *
     * @param rate The new audio rate multiplier. Only positive values above
     * zero are allowed. Note that implementations may cap this value at some
     * undefined amount.
     *
     * @see #playbackRate()
     */
    public void setPlaybackRate(double rate) {
        this.clipRate = rate;
    }

    /**
     * Pan (left/right spread) setting.
     *
     * @return Current pan value.
     */
    public double pan() {
        return this.clipPan;
    }

    /**
     * Sets the audio pan (or left/right channel spread) value. Valid range is
     * -1.0 to 1.0 inclusively. A zero setting means normal playback, left and
     * right channels will be sent to their respective output channels. A -1.0
     * value shifts the AudioClip so that all sound is output only on the left
     * output channel, likewise a 1.0 value shifts completely to the right
     * output channel.
     *
     * @param pan Audio pan setting
     */
    public void setPan(double pan) {
        this.clipPan = pan;
    }

    /**
     * Test if any AudioClipPlayer has this AudioClip in its effect chain.
     *
     * @return true if at least one AudioClipPlayer is playing or
     * has this AudioClip enqueued.
     */
    public abstract boolean isPlaying();

    /**
     * Play this AudioClip with the default parameters. This is a fire and
     * forget method, the clip will play exactly once using the default
     * parameters.
     */
    public abstract void play();

    /**
     * Play this AudioClip at the given volume level. This is a fire and
     * forget method, the clip will play exactly once using the given volume and
     * default pitch and pan.
     *
     * @param volume Volume level to play this effect at. Valid volume range is
     * 0.0 to 1.0, where 0.0 is effectively muted and 1.0 is full volume.
     */
    public abstract void play(double volume);

    /**
     * Play this AudioClip at the given volume level and relative pitch. This is
     * a fire and forget method, the clip will play exactly once using the given
     * parameters.
     *
     * @param volume Volume level to play this effect at. Valid volume range is
     * 0.0 to 1.0, where 0.0 is effectively muted and 1.0 is full volume.
     * @param balance Left/right balance or relative channel volumes for stereo
     * effects.
     * @param rate Playback rate multiplier. 1.0 will play back at the normal
     * rate while 2.0 will double the rate.
     * @param pan Left/right shift to be applied to the clip. A pan value of
     * -1.0 means full left channel, 1.0 means full right channel, 0.0 has no
     * effect.
     * @param loopCount The number of times to play this clip, specify -1 to
     * loop indefinitely
     * @param priority Audio effect priority. Lower priority effects will be
     * dropped first if too many effects are trying to play simultaneously.
     */
    public abstract void play(double volume, double balance, double rate, double pan, int loopCount, int priority);

    /**
     * Stops all playback of this AudioClip.
     */
    public abstract void stop();
}
