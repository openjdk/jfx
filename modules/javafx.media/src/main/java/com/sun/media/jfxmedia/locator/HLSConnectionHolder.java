/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.media.jfxmedia.locator;

import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmediaimpl.MediaUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

final class HLSConnectionHolder extends ConnectionHolder {

    // If true HLSConnectionHolder represents additional audio renditions
    private boolean isAudioExtStream = false;
    private HLSConnectionHolder audioConnectionHolder = null;
    private URLConnection urlConnection = null;
    private URLConnection headerConnection = null;
    private ReadableByteChannel headerChannel = null;
    private final PlaylistLoader playlistLoader;
    private VariantPlaylist variantPlaylist = null;
    private Playlist currentPlaylist = null;
    // If set we need to switch to new current playlist.
    // Used by audio ext streams when switching bitrates.
    private Playlist newCurrentPlaylist = null;
    private final Object newPlaylistLock = new Object();
    private boolean isBitrateAdjustable = false;
    private boolean hasAudioExtStream = false;
    private long readStartTime = -1;
    private boolean sendHeader = false;
    private boolean isInitialized = false;
    private int duration = -1;
    // Will be set to adjusted start time of segment.
    // Seek will set this value and HLS_PROP_SEGMENT_START_TIME
    // should return it if set.
    private int segmentStartTimeAfterSeek = -1;
    static final long HLS_VALUE_FLOAT_MULTIPLIER = 1000;
    static final int HLS_PROP_GET_DURATION = 1;
    static final int HLS_PROP_GET_HLS_MODE = 2;
    static final int HLS_PROP_GET_MIMETYPE = 3;
    static final int HLS_PROP_LOAD_SEGMENT = 4;
    static final int HLS_PROP_SEGMENT_START_TIME = 5;
    static final int HLS_PROP_HAS_AUDIO_EXT_STREAM = 6;
    static final int HLS_VALUE_MIMETYPE_UNKNOWN = -1;
    static final int HLS_VALUE_MIMETYPE_MP2T = 1;
    static final int HLS_VALUE_MIMETYPE_MP3 = 2;
    static final int HLS_VALUE_MIMETYPE_FMP4 = 3;
    static final int HLS_VALUE_MIMETYPE_AAC = 4;
    static final String CHARSET_UTF_8 = "UTF-8";
    static final String CHARSET_US_ASCII = "US-ASCII";

    HLSConnectionHolder(URI uri) {
        playlistLoader = new PlaylistLoader();
        playlistLoader.setPlaylistURI(uri);
        init();
    }

    // if isAudioExtStream is true then this HLSConnectionHolder
    // represents additional audio renditions from EXT-X-MEDIA tag.
    HLSConnectionHolder(Playlist currentPlaylist, boolean isAudioExtStream) {
        playlistLoader = null;
        this.currentPlaylist = currentPlaylist;
        this.isAudioExtStream = isAudioExtStream;
    }

    private void init() {
        playlistLoader.putState(PlaylistLoader.STATE_INIT);
        playlistLoader.start();
    }

    @Override
    public int readNextBlock() throws IOException {
        if (isBitrateAdjustable && readStartTime == -1) {
            readStartTime = System.currentTimeMillis();
        }

        if (headerChannel != null) {
            buffer.rewind();
            if (buffer.limit() < buffer.capacity()) {
                buffer.limit(buffer.capacity());
            }
            int read = headerChannel.read(buffer);
            if (read == -1) {
                resetHeaderConnection();
            } else {
                return read;
            }
        }

        int read = super.readNextBlock();
        if (isBitrateAdjustable && read == -1) {
            long readTime = System.currentTimeMillis() - readStartTime;
            readStartTime = -1;
            adjustBitrate(readTime);
        } else if (isAudioExtStream && read == -1) {
            adjustBitrateAudioExt();
        }

        return read;
    }

    @Override
    int readBlock(long position, int size) throws IOException {
        throw new IOException();
    }

    @Override
    boolean needBuffer() {
        return true;
    }

    @Override
    boolean isSeekable() {
        return true;
    }

    @Override
    boolean isRandomAccess() {
        return false; // Only by segments
    }

    @Override
    public long seek(long position) {
        if (!isReady()) {
            return -1;
        }

        if (hasAudioExtStream && position != 0) {
            if (getAudioStream() == null || getAudioStream().getCurrentPlaylist() == null) {
                return -1; // Something wrong or EOS
            }

            // Video stream with audio extenstion.
            // Get start of audio segment for seek position. This is same start position for
            // audio stream when seek to "position".
            double audioPosition = getAudioStream().getCurrentPlaylist().seekGetStartTime(position);

            // Seek this video stream to audioPosition. Note: If we seek video stream to position,
            // we might get segment which will not be aligned with audio start time after seek, since
            // target duration (length of audio and video segments) is not same.
            if (currentPlaylist.seek((long)audioPosition) == -1) {
                return -1; // Something wrong or EOS
            }

            // Now video stream at correct segment, but its start time will not be aligned with
            // audio segment, so we need to return audio segment start time instead. In this
            // case GStreamer will drop all frames before start time and audio and video will
            // be in sync.
            return (long) (audioPosition * HLS_VALUE_FLOAT_MULTIPLIER);
        } else {
            return (long) (currentPlaylist.seek(position) * HLS_VALUE_FLOAT_MULTIPLIER);
        }
    }

    @Override
    public void closeConnection() {
        currentPlaylist.close();
        super.closeConnection();
        resetConnection();
        playlistLoader.putState(PlaylistLoader.STATE_EXIT);
    }

    @Override
    int property(int prop, int value) {
        if (!isReady()) {
            return -1;
        }

        switch (prop) {
            case HLS_PROP_GET_DURATION:
                return duration;
            case HLS_PROP_GET_HLS_MODE:
                return 1;
            case HLS_PROP_GET_MIMETYPE:
                return currentPlaylist.getMimeType();
            case HLS_PROP_LOAD_SEGMENT:
                return loadNextSegment();
            case HLS_PROP_SEGMENT_START_TIME:
                int segmentStart = -1;
                if (segmentStartTimeAfterSeek != -1) {
                    segmentStart = segmentStartTimeAfterSeek;
                    segmentStartTimeAfterSeek = -1;
                } else {
                    segmentStart = (int) (currentPlaylist.getMediaFileStartTime() * HLS_VALUE_FLOAT_MULTIPLIER);
                }
                return segmentStart;
            case HLS_PROP_HAS_AUDIO_EXT_STREAM:
                return hasAudioExtStream ? 1 : 0;
            default:
                return -1;
        }
    }

    @Override
    public HLSConnectionHolder getAudioStream() {
        if (!hasAudioExtStream) {
            return null;
        }

        if (audioConnectionHolder != null) {
            return audioConnectionHolder;
        }

        // currentPlaylist should be set by now
        if (variantPlaylist != null && currentPlaylist != null) {
            String audioGroupID = currentPlaylist.getAudioGroupID();
            Playlist playlist = variantPlaylist.getAudioExtPlaylist(audioGroupID);
            audioConnectionHolder = new HLSConnectionHolder(playlist, true);
            return audioConnectionHolder;
        }

        return null;
    }

    // Will block if we are not ready yet.
    // Once ready it will do additional initialization like getting
    // playlists from PlayListLoader
    private synchronized boolean isReady() {
        if (playlistLoader != null) {
            if (!playlistLoader.waitForReady()) {
                return false;
            }
        }

        if (isInitialized) {
            return true;
        }

        if (playlistLoader != null) {
            variantPlaylist = playlistLoader.getVariantPlaylist();
            currentPlaylist = playlistLoader.getCurrentPlaylist();
        }

        // Always start with first data playlist
        if (variantPlaylist != null) {
            currentPlaylist = variantPlaylist.getPlaylist(0);
            isBitrateAdjustable = true;
            hasAudioExtStream = !variantPlaylist.getAudioExtMedia().isEmpty();
        }

        // Figure out duration. Duration might be slightly different
        // for streams with separate audio and video stream, so we will
        // take the largest one. Audio stream does not need to do this and
        // video stream will set correct duration.
        if (currentPlaylist != null && !isAudioExtStream) {
            duration = (int) (currentPlaylist.getDuration() * HLS_VALUE_FLOAT_MULTIPLIER);
            if (hasAudioExtStream) {
                int audioDuration = (int) (variantPlaylist.getAudioExtMedia().get(0)
                        .getPlaylist().getDuration() * HLS_VALUE_FLOAT_MULTIPLIER);
                if (duration < audioDuration) {
                    duration = audioDuration;
                }

                // Tell audio stream which duration to report to GStreamer.
                HLSConnectionHolder audioStream = getAudioStream();
                if (audioStream != null) {
                    audioStream.setDuration(duration);
                }
            }
        }

        // If we have playlist with fMP4, set flag to add header
        // to first data segment and adjust index to 0
        if (currentPlaylist != null && currentPlaylist.isFragmentedMP4()) {
            sendHeader = true;
            currentPlaylist.setMediaFileIndex(0);
        }

        isInitialized = true;

        return true;
    }

    private void resetConnection() {
        super.closeConnection();

        resetHeaderConnection();

        Locator.closeConnection(urlConnection);
        urlConnection = null;
    }

    private void resetHeaderConnection() {
        try {
            if (headerChannel != null) {
                headerChannel.close();
            }
        } catch (IOException ioex) {
        } finally {
            headerChannel = null;
        }

        Locator.closeConnection(headerConnection);
        headerConnection = null;
    }

    void setNewCurrentPlaylist(Playlist value) {
        synchronized (newPlaylistLock) {
            if (currentPlaylist != value) {
                newCurrentPlaylist = value;
            }
        }
    }

    void setDuration(int value) {
        duration = value;
    }

    // Returns -1 EOS or critical error
    // Returns positive size of segment if no issues.
    // Returns negative size of segment if discontinuity.
    private int loadNextSegment() {
        resetConnection();

        String mediaFile;
        int headerLength = 0;

        if (sendHeader) {
            mediaFile = currentPlaylist.getHeaderFile();
            if (mediaFile == null) {
                return -1;
            }

            try {
                URI uri = new URI(mediaFile);
                headerConnection = uri.toURL().openConnection();
                headerChannel = openHeaderChannel();
                headerLength = headerConnection.getContentLength();
            } catch (IOException | URISyntaxException e) {
                return -1;
            }
            sendHeader = false;
        }

        mediaFile = currentPlaylist.getNextMediaFile();
        if (mediaFile == null) {
            if (currentPlaylist.isFragmentedMP4()) {
                sendHeader = true;
            }
            return -1;
        }

        try {
            URI uri = new URI(mediaFile);
            urlConnection = uri.toURL().openConnection();
            channel = openChannel();
        } catch (IOException | URISyntaxException e) {
            return -1;
        }

        if (currentPlaylist.isCurrentMediaFileDiscontinuity()) {
            return (-1 * (urlConnection.getContentLength() + headerLength));
        } else {
            return (urlConnection.getContentLength() + headerLength);
        }
    }

    private ReadableByteChannel openChannel() throws IOException {
        return Channels.newChannel(urlConnection.getInputStream());
    }

    private ReadableByteChannel openHeaderChannel() throws IOException {
        return Channels.newChannel(headerConnection.getInputStream());
    }

    private void adjustBitrate(long readTime) {
        int avgBitrate = (int) (((long) urlConnection.getContentLength() * 8 * 1000) / readTime);

        Playlist playlist = variantPlaylist.getPlaylistBasedOnBitrate(avgBitrate);
        if (playlist != null && playlist != currentPlaylist) {
            if (currentPlaylist.isLive()) {
                playlist.update(currentPlaylist.getNextMediaFile());
                playlistLoader.setReloadPlaylist(playlist);
            }

            playlist.setForceDiscontinuity(true);
            // Copy index when switching playlist, so we continue reading
            // from correct position.
            playlist.setMediaFileIndex(currentPlaylist.getMediaFileIndex());
            currentPlaylist = playlist;
            if (currentPlaylist.isFragmentedMP4()) {
                sendHeader = true;
            }

            // We switched playlist and we need to check if we need to
            // switch audio stream as well.
            if (getAudioStream() != null) {
                String audioGroupID = currentPlaylist.getAudioGroupID();
                Playlist audioPlaylist = variantPlaylist.getAudioExtPlaylist(audioGroupID);
                getAudioStream().setNewCurrentPlaylist(audioPlaylist);
            }
        }
    }

    private void adjustBitrateAudioExt() {
        // If video stream provided new playlist, then use it.
        synchronized (newPlaylistLock) {
            if (newCurrentPlaylist != null && newCurrentPlaylist != currentPlaylist) {
                if (currentPlaylist.isLive()) {
                    newCurrentPlaylist.update(currentPlaylist.getNextMediaFile());
                    playlistLoader.setReloadAudioExtPlaylist(newCurrentPlaylist);
                }

                newCurrentPlaylist.setForceDiscontinuity(true);
                // Copy index when switching playlist, so we continue reading
                // from correct position.
                newCurrentPlaylist.setMediaFileIndex(currentPlaylist.getMediaFileIndex());
                currentPlaylist = newCurrentPlaylist;
                if (currentPlaylist.isFragmentedMP4()) {
                    sendHeader = true;
                }

                newCurrentPlaylist = null;
            }
        }
    }

    static String stripParameters(String mediaFile) {
        int qp = mediaFile.indexOf('?');
        if (qp > 0) {
            mediaFile = mediaFile.substring(0, qp); // Strip all possible http parameters.
        }
        return mediaFile;
    }

    Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    private static class PlaylistLoader extends Thread {

        public static final int STATE_INIT = 0;
        public static final int STATE_EXIT = 1;
        public static final int STATE_RELOAD_PLAYLIST = 2;
        private final BlockingQueue<Integer> stateQueue = new LinkedBlockingQueue<>();
        private URI playlistURI = null;
        private Playlist reloadPlaylist = null;
        private Playlist reloadAudioExtPlaylist = null;
        private final Object reloadLock = new Object();
        private volatile boolean stopped = false;
        private final CountDownLatch readySignal = new CountDownLatch(1);
        private VariantPlaylist variantPlaylist = null;
        private Playlist currentPlaylist = null;

        PlaylistLoader() {
            setName("JFXMedia HLS Playlist Thread");
            setDaemon(true);
        }

        boolean waitForReady() {
            try {
                readySignal.await();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        VariantPlaylist getVariantPlaylist() {
            return variantPlaylist;
        }

        Playlist getCurrentPlaylist() {
            return currentPlaylist;
        }

        void setPlaylistURI(URI playlistURI) {
            this.playlistURI = playlistURI;
        }

        void setReloadPlaylist(Playlist playlist) {
            synchronized (reloadLock) {
                reloadPlaylist = playlist;
            }
        }

        void setReloadAudioExtPlaylist(Playlist playlist) {
            synchronized (reloadLock) {
                reloadAudioExtPlaylist = playlist;
            }
        }

        @Override
        public void run() {
            while (!stopped) {
                try {
                    int state = stateQueue.take();
                    switch (state) {
                        case STATE_INIT:
                            stateInit();
                            break;
                        case STATE_EXIT:
                            stopped = true;
                            break;
                        case STATE_RELOAD_PLAYLIST:
                            stateReloadPlaylist();
                            break;
                        default:
                            break;
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        void putState(int state) {
            if (stateQueue != null) {
                try {
                    stateQueue.put(state);
                } catch (InterruptedException ex) {
                }
            }
        }

        private void stateInit() {
            if (playlistURI == null) {
                return;
            }

            try {
                PlaylistParser parser = new PlaylistParser();
                parser.load(playlistURI);

                if (parser.getVariantPlaylistOrNull() != null) {
                    variantPlaylist = parser.getVariantPlaylistOrNull();
                } else {
                    if (currentPlaylist == null) {
                        currentPlaylist = parser.getPlaylistOrNull();
                    }
                }

                if (variantPlaylist != null) {
                    // Load playlists (EXT-X-STREAM-INF) inside variant playlist if needed
                    variantPlaylist.getExtStreamInf().forEach((ExtStreamInf ext) -> {
                        Playlist playlist = new Playlist(ext.getPlaylistURI());
                        playlist.update(null);
                        playlist.setAudioGroupID(ext.getAudioGroupID());
                        ext.setPlaylist(playlist);
                    });
                    variantPlaylist.validateExtStreamInf();

                    final boolean isVideoStreamFragmentedMP4;
                    final long videoStreamTargetDuration;
                    currentPlaylist = variantPlaylist.getPlaylist(0);
                    if (currentPlaylist != null) {
                        isVideoStreamFragmentedMP4 = currentPlaylist.isFragmentedMP4();
                        if (isVideoStreamFragmentedMP4) {
                            videoStreamTargetDuration = currentPlaylist.getTargetDuration();
                        } else {
                            videoStreamTargetDuration = 0;
                        }
                    } else {
                        isVideoStreamFragmentedMP4 = false;
                        videoStreamTargetDuration = 0;
                    }

                    // Load Audio Ext Media playlist if needed
                    variantPlaylist.getAudioExtMedia().forEach((AudioExtMedia ext) -> {
                        Playlist playlist = new Playlist(ext.getPlaylistURI());
                        playlist.setIsVideoStreamFragmentedMP4(isVideoStreamFragmentedMP4);
                        playlist.setVideoStreamTargetDuration(videoStreamTargetDuration);
                        playlist.update(null);
                        ext.setPlaylist(playlist);
                    });
                    variantPlaylist.validateAudioExtMedia();
                }

                if (variantPlaylist != null) {
                    currentPlaylist = variantPlaylist.getPlaylist(0);
                    // Start reloading live playlist
                    if (currentPlaylist != null && currentPlaylist.isLive()) {
                        setReloadPlaylist(currentPlaylist);

                        // Add audio ext playlist for reload if we have one
                        Playlist audioExtPlaylist = variantPlaylist
                                .getAudioExtPlaylist(currentPlaylist.getAudioGroupID());
                        if (audioExtPlaylist != null && audioExtPlaylist.isLive()) {
                            this.setReloadAudioExtPlaylist(audioExtPlaylist);
                        }

                        putState(STATE_RELOAD_PLAYLIST);
                    }
                }
            } finally {
                readySignal.countDown();
            }
        }

        private void stateReloadPlaylist() {
            try {
                long timeout;
                synchronized (reloadLock) {
                    timeout = reloadPlaylist.getTargetDuration() / 2;
                }
                Thread.sleep(timeout);
            } catch (InterruptedException ex) {
                return;
            }

            synchronized (reloadLock) {
                reloadPlaylist.update(null);
                if (reloadAudioExtPlaylist != null) {
                    reloadAudioExtPlaylist.update(null);
                }
            }

            putState(STATE_RELOAD_PLAYLIST);
        }
    }

    private static class PlaylistParser {

        private URI playlistURI = null;
        private boolean isDiscontinuity = false;
        private VariantPlaylist variantPlaylist = null;
        private Playlist playlist = null;
        private boolean isEndList = false;

        private final String TAG_PARAM_TYPE = "TYPE";
        private final String TAG_PARAM_TYPE_AUDIO = "AUDIO";
        private final String TAG_PARAM_GROUP_ID = "GROUP-ID";
        private final String TAG_PARAM_AUTOSELECT = "AUTOSELECT";
        private final String TAG_PARAM_DEFAULT = "DEFAULT";
        private final String TAG_PARAM_URI = "URI";
        private final String TAG_PARAM_BANDWIDTH = "BANDWIDTH";
        private final String TAG_PARAM_AUDIO = "AUDIO";
        private final String TAG_VALUE_YES = "YES";

        void load(URI uri) {
            playlistURI = uri;

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    MediaUtils.error(this, MediaError.ERROR_LOCATOR_CONNECTION_LOST.code(),
                            "HTTP responce code: " + connection.getResponseCode(), null);
                }

                Charset charset = getCharset(uri.toURL().toExternalForm(), connection.getContentType());
                if (charset != null) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
                }

                if (reader != null) {
                    // First line cannot be null and should be #EXTM3U
                    if ("#EXTM3U".equals(reader.readLine())) {
                        parse(reader);
                    }
                }
            } catch (IOException e) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }

                    Locator.closeConnection(connection);
                }
            }
        }

        private VariantPlaylist getVariantPlaylist() {
            if (variantPlaylist == null) {
                variantPlaylist = new VariantPlaylist(playlistURI);
            }

            return variantPlaylist;
        }

        VariantPlaylist getVariantPlaylistOrNull() {
            return variantPlaylist;
        }

        void setPlaylist(Playlist value) {
            playlist = value;
        }

        private Playlist getPlaylist() {
            if (playlist == null) {
                playlist = new Playlist(playlistURI);
            }

            return playlist;
        }

        Playlist getPlaylistOrNull() {
            return playlist;
        }

        boolean isLivePlaylist() {
            return !isEndList;
        }

        private void validateArray(String[] tagParams, int length) {
            if (tagParams.length < length) {
                throw new MediaException("Invalid HLS playlist");
            }
        }

        private String getNextLine(BufferedReader reader) throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignore blank lines, comments and tags
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                } else {
                    return line;
                }
            }

            throw new MediaException("Invalid HLS playlist");
        }

        private void parse(BufferedReader reader) throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignore blank lines and comments
                if (line.isEmpty() || (line.startsWith("#") && !line.startsWith("#EXT"))) {
                    continue;
                }

                // If line is not a blank or comment, then it can be tags or URI.
                // URI lines should be read when we're parsing tags, so here we
                // should only get tags. We can have tags with or without
                // parameters. Tag and parameters are separated by ":".
                String[] tagParams = line.split(":");
                validateArray(tagParams, 1);

                switch (tagParams[0]) {
                    case "#EXTINF": { // #EXTINF:<duration>
                        validateArray(tagParams, 2);
                        String[] params = tagParams[1].split(",");
                        validateArray(params, 1);
                        String URI = getNextLine(reader);
                        getPlaylist().addMediaFile(URI,
                                Double.parseDouble(params[0]), isDiscontinuity);
                        // Clear discontinue flag, until it is set again by parser.
                        isDiscontinuity = false;
                        break;
                    }
                    case "#EXT-X-TARGETDURATION": { // #EXT-X-TARGETDURATION:<s>
                        validateArray(tagParams, 2);
                        getPlaylist().setTargetDuration(
                                Integer.parseInt(tagParams[1]));
                        break;
                    }
                    case "#EXT-X-MEDIA-SEQUENCE": { // #EXT-X-MEDIA-SEQUENCE:<number>
                        validateArray(tagParams, 2);
                        getPlaylist().setSequenceNumber(
                                Integer.parseInt(tagParams[1]));
                        break;
                    }
                    case "#EXT-X-STREAM-INF": { // #EXT-X-STREAM-INF:<attribute-list>
                        validateArray(tagParams, 2);
                        String[] params = tagParams[1].split(",");
                        int bitrate = getIntegerParams(TAG_PARAM_BANDWIDTH, params);
                        String audioGroupID = getStringParams(TAG_PARAM_AUDIO, params);
                        String location = getNextLine(reader);
                        ExtStreamInf item = new ExtStreamInf(location, bitrate);
                        item.setAudioGroupID(audioGroupID);
                        getVariantPlaylist().addExtStreamInf(item);
                        break;
                    }
                    case "#EXT-X-ENDLIST": { // #EXT-X-ENDLIST
                        isEndList = true;
                        break;
                    }
                    case "#EXT-X-DISCONTINUITY": { // #EXT-X-DISCONTINUITY
                        isDiscontinuity = true;
                        break;
                    }
                    case "#EXT-X-MAP": { // #EXT-X-MAP
                        validateArray(tagParams, 2);
                        String[] params = tagParams[1].split(",");
                        String uri = getStringParams(TAG_PARAM_URI, params);
                        getPlaylist().addMediaFile(uri,
                                getPlaylist().getTargetDuration(), true);
                        break;
                    }
                    case "#EXT-X-MEDIA": { // #EXT-X-MEDIA
                        validateArray(tagParams, 2);
                        String[] params = tagParams[1].split(",");
                        String type = getStringParams(TAG_PARAM_TYPE, params);
                        if (type.equals(TAG_PARAM_TYPE_AUDIO)) {
                            // Required
                            String groupID = getStringParams(TAG_PARAM_GROUP_ID, params);
                            // Optional (YES or NO)
                            // We should assume value NO if DEFAULT or AUTOSELECT is absent.
                            // According to spec we should not auto select streams if both
                            // DEFAULT and AUTOSELECT are NO. Currently, we do not provide APIs
                            // to manually select additional streams, so we will drop it in case
                            // when both DEFAULT and AUTOSELECT are NO.
                            String isAutoSelect = getStringParams(TAG_PARAM_AUTOSELECT, params);
                            String isDefault = getStringParams(TAG_PARAM_DEFAULT, params);
                            // Optional
                            // Looks like URI can be null for closed caption and video. If it is null,
                            // then stream is included with EXT-X-STREAM-INF, but in case for audio
                            // it is not clear how it will work. Also, there is no example for such case
                            // in documentations. Examples are only for video. For now, we will ignore
                            // any audio streams without URI.
                            String location = getStringParams(TAG_PARAM_URI, params);
                            boolean autoSelectBool = TAG_VALUE_YES.equalsIgnoreCase(isAutoSelect);
                            boolean defaultBool = TAG_VALUE_YES.equalsIgnoreCase(isDefault);
                            if (location != null && (autoSelectBool || defaultBool)) {
                                AudioExtMedia item = new AudioExtMedia(groupID, location);
                                getVariantPlaylist().addAudioExtMedia(item);
                            }
                        }
                        break;
                    }
                    default: { // Unsupported tag. Ok to ignore.
                        break;
                    }
                }
            }
        }

        private String getStringParams(String name, String[] params) {
            Stream<String> stream = Arrays.stream(params);
            return stream.filter(x -> x.startsWith(name))
                    .flatMap(x -> {
                        String param = x.trim();
                        String[] paramValuePair = param.split("=");
                        if (paramValuePair.length == 2 &&
                                !paramValuePair[1].isEmpty()) {
                            return Stream.of(
                                    paramValuePair[1].replaceAll("^\"+|\"+$", ""));
                        }
                        return Stream.empty();
                    })
                    .findFirst()
                    .orElse(null);
        }

        private int getIntegerParams(String name, String[] params) {
            Stream<String> stream = Arrays.stream(params);
            return stream.filter(x -> x.startsWith(name))
                    .flatMap(x -> {
                        String param = x.trim();
                        String[] paramValuePair = param.split("=");
                        if (paramValuePair.length == 2 &&
                                !paramValuePair[1].isEmpty()) {
                            return Stream.of(Integer.parseInt(paramValuePair[1]));
                        }
                        return Stream.empty();
                    })
                    .findFirst()
                    .orElse(null);
        }

        private Charset getCharset(String url, String mimeType) {
            if ((url != null && stripParameters(url).endsWith(".m3u8"))
                    || (mimeType != null && mimeType.equals("application/vnd.apple.mpegurl"))) {
                if (Charset.isSupported(CHARSET_UTF_8)) {
                    return Charset.forName(CHARSET_UTF_8);
                }
            } else if ((url != null && stripParameters(url).endsWith(".m3u"))
                    || (mimeType != null && mimeType.equals("audio/mpegurl"))) {
                if (Charset.isSupported(CHARSET_US_ASCII)) {
                    return Charset.forName(CHARSET_US_ASCII);
                }
            }

            return null;
        }
    }

    // Contains information from EXT-X-STREAM-INF tag and corresponding playlist
    // with media segments.
    private static class ExtStreamInf {
        private String location = null;
        private int bitrate = 0;
        private String audioGroupID = null;
        private URI playlistURI = null;
        private Playlist playlist = null;

        ExtStreamInf(String location, int bitrate) {
            this.location = location;
            this.bitrate = bitrate;
        }

        int getBitrate() {
            return bitrate;
        }

        void setAudioGroupID(String value) {
            audioGroupID = value;
        }

        String getAudioGroupID() {
            return audioGroupID;
        }

        String getLocation() {
            return location;
        }

        void setPlaylistURI(URI uri) {
            playlistURI = uri;
        }

        URI getPlaylistURI() {
            return playlistURI;
        }

        void setPlaylist(Playlist value) {
            playlist = value;
        }

        Playlist getPlaylist() {
            return playlist;
        }
    }

    // Contains information from EXT-X-MEDIA tag and corresponding playlist with
    // media segments. This class supports only AUDIO type.
    private static class AudioExtMedia {

        private String groupID = null;
        private String location = null;
        private URI playlistURI = null;
        private Playlist playlist = null;

        AudioExtMedia(String groupID, String location) {
            this.groupID = groupID;
            this.location = location;
        }

        String getGroupID() {
            return groupID;
        }

        String getLocation() {
            return location;
        }

        void setPlaylistURI(URI uri) {
            playlistURI = uri;
        }

        URI getPlaylistURI() {
            return playlistURI;
        }

        void setPlaylist(Playlist value) {
            playlist = value;
        }

        Playlist getPlaylist() {
            return playlist;
        }
    }

    private static class VariantPlaylist {

        private URI playlistURI = null;
        private final List<ExtStreamInf> extStreamInf = new ArrayList<>();
        private final List<AudioExtMedia> audioExtMedia = new ArrayList<>();
        private final List<Integer> playlistBitrates = new ArrayList<>();

        VariantPlaylist(URI uri) {
            playlistURI = uri;
        }

        void addExtStreamInf(ExtStreamInf item) {
            // Before adding ext stream inf we need to resolve URI against variant playlist.
            try {
                item.setPlaylistURI(locationToURI(item.getLocation()));
            } catch (URISyntaxException | MalformedURLException e) {
                throw new MediaException("Invalid HLS playlist");
            }
            extStreamInf.add(item);
        }

        List<ExtStreamInf> getExtStreamInf() {
            return extStreamInf;
        }

        // Should be called after ExtStreamInf is fully loaded including playlist
        // itself.
        // It will do final validation and drop anything we do not want.
        void validateExtStreamInf() {
            String extension = null; // Will be set to media file extension of first playlist
            boolean hasAudioGroupID = false; // Will be set to true if first playlist has group ID.
            Iterator<ExtStreamInf> it = extStreamInf.iterator();
            while (it.hasNext()) {
                ExtStreamInf item = it.next();
                Playlist playlist = item.getPlaylist();
                if (playlist == null) {
                    it.remove();
                } else {
                    if (extension == null) {
                        extension = playlist.getMediaFileExtension();
                        hasAudioGroupID = (item.getAudioGroupID() != null);
                    } else {
                        if (!extension.equals(playlist.getMediaFileExtension())) {
                            it.remove();
                        }
                        // If at least one stream has audio group id (separate audio stream), we
                        // need to make sure that they all have it. Our pipeline does not support
                        // such cases and it is not clear if they can exist.
                        if (hasAudioGroupID && (item.getAudioGroupID() == null)) {
                            it.remove();
                        }
                    }
                }
            }

            if (extStreamInf.isEmpty()) {
                // We did not found any supported streams
                throw new MediaException("Invalid HLS playlist");
            }

            // Load bitrates for switching playlists
            extStreamInf.forEach((ext) -> {
                playlistBitrates.add(ext.getBitrate());
            });
        }

        void addAudioExtMedia(AudioExtMedia item) {
            // Before adding audio ext media we need to resolve URI against variant
            // playlist.
            try {
                item.setPlaylistURI(locationToURI(item.getLocation()));
            } catch (URISyntaxException | MalformedURLException e) {
                throw new MediaException("Invalid HLS playlist");
            }
            audioExtMedia.add(item);
        }

        List<AudioExtMedia> getAudioExtMedia() {
            return audioExtMedia;
        }

        // Should be called after AudioExtMedia is fully loaded including playlist
        // itself.
        // It will do final validation and drop anything we do not want.
        void validateAudioExtMedia() {
            String extension = null; // Will be set to media file extension of first playlist
            Iterator<AudioExtMedia> it = audioExtMedia.iterator();
            while (it.hasNext()) {
                AudioExtMedia item = it.next();
                Playlist playlist = item.getPlaylist();
                if (playlist == null) {
                    it.remove();
                } else {
                    if (extension == null) {
                        extension = playlist.getMediaFileExtension();
                    } else {
                        if (!extension.equals(playlist.getMediaFileExtension())) {
                            it.remove();
                        }
                    }
                }
            }
        }

        Playlist getPlaylist(int index) {
            if (index < 0 || index >= extStreamInf.size()) {
                return null;
            } else {
                return extStreamInf.get(index).getPlaylist();
            }
        }

        Playlist getAudioExtPlaylist(String audioGroupID) {
            if (audioGroupID == null || audioExtMedia.isEmpty()) {
                return null;
            }

            AudioExtMedia item = audioExtMedia.stream()
                    .filter(ext -> ext.getGroupID().equals(audioGroupID))
                    .findFirst().orElse(null);

            return item == null ? null : item.getPlaylist();
        }

        // Converts playlist location to URI. .m3u8 playlist can have absolute URI or
        // relatively to
        // variant playlist. This function takes string value from playlist and returns
        // resolved URI
        private URI locationToURI(String location) throws URISyntaxException, MalformedURLException {
            if (location.startsWith("http://") || location.startsWith("https://")) {
                return new URI(location);
            } else {
                return new URI(
                        playlistURI.toURL().toString().substring(0, playlistURI.toURL().toString().lastIndexOf("/") + 1)
                                + location);
            }
        }

        Playlist getPlaylistBasedOnBitrate(int bitrate) {
            int playlistIndex = -1;
            int playlistBitrate = 0;

            // Get bitrate that less than requested bitrate, but most closed to it
            for (int i = 0; i < playlistBitrates.size(); i++) {
                int b = playlistBitrates.get(i);
                if (b < bitrate) {
                    if (playlistIndex != -1) {
                        if (b > playlistBitrate) {
                            playlistBitrate = b;
                            playlistIndex = i;
                        }
                    } else {
                        playlistIndex = i;
                    }
                }
            }

            // If we did not find one (stall), then get the lowest bitrate possible
            if (playlistIndex == -1) {
                for (int i = 0; i < playlistBitrates.size(); i++) {
                    int b = playlistBitrates.get(i);
                    if (b < playlistBitrate || playlistIndex == -1) {
                        playlistBitrate = b;
                        playlistIndex = i;
                    }
                }
            }

            // Just in case
            return getPlaylist(playlistIndex);
        }
    }

    private static class Playlist {

        private boolean isLive = false;
        private volatile boolean isLiveWaiting = false;
        private volatile boolean isLiveStop = false;
        private long targetDuration = 0;
        private URI playlistURI = null;
        private final Object lock = new Object();
        private final List<String> mediaFiles = new ArrayList<>();
        final List<Double> mediaFilesStartTimes = new ArrayList<>();
        private final List<Boolean> mediaFilesDiscontinuities = new ArrayList<>();
        private boolean needBaseURI = true;
        private String baseURI = null;
        private double startTime = 0.0;
        private double duration = 0.0;
        private int sequenceNumber = -1;
        private int sequenceNumberStart = -1;
        private boolean sequenceNumberUpdated = false;
        private boolean forceDiscontinuity = false;
        private int mimeType = HLS_VALUE_MIMETYPE_UNKNOWN;
        private int mediaFileIndex = -1;
        private final Semaphore liveSemaphore = new Semaphore(0);
        private boolean isPlaylistClosed = false;
        // Valid only if this playlist represent audio extension
        private boolean isVideoStreamFragmentedMP4 = false;
        // Target duration of video stream. For fMP4 streams PTS
        // starts with target duration, but for raw audio it will
        // start with 0, so if we have video stream in fMP4, but
        // audio extension as raw audio we will need to adjust
        // start time by video stream target duration, so PTS
        // will align properly.
        private long videoStreamTargetDuration = 0;
        private String audioGroupID = null;

        Playlist(URI uri) {
            playlistURI = uri;
        }

        void update(String nextMediaFile) {
            PlaylistParser parser = new PlaylistParser();
            parser.setPlaylist(this);
            parser.load(playlistURI);

            isLive = parser.isLivePlaylist();
            if (isLive) {
                duration = -1.0;
            }

            if (nextMediaFile != null) {
                synchronized (lock) {
                    for (int i = 0; i < mediaFiles.size(); i++) {
                        String mediaFile = mediaFiles.get(i);
                        if (nextMediaFile.endsWith(mediaFile)) {
                            mediaFileIndex = i - 1;
                            break;
                        }
                    }
                }
            }
        }

        void setMediaFileIndex(int value) {
            mediaFileIndex = value;
        }

        int getMediaFileIndex() {
            return mediaFileIndex;
        }

        boolean isLive() {
            return isLive;
        }

        boolean isFragmentedMP4() {
            return (getMimeType() == HLS_VALUE_MIMETYPE_FMP4);
        }

        void setTargetDuration(long value) {
            targetDuration = value;
        }

        long getTargetDuration() {
            return targetDuration;
        }

        void setVideoStreamTargetDuration(long value) {
            videoStreamTargetDuration = value;
        }

        void setIsVideoStreamFragmentedMP4(boolean value) {
            isVideoStreamFragmentedMP4 = value;
        }

        void addMediaFile(String URI, double duration, boolean isDiscontinuity) {
            synchronized (lock) {

                if (needBaseURI) {
                    setBaseURI(playlistURI.toString(), URI);
                }

                if (isLive) {
                    if (sequenceNumberUpdated) {
                        int index = mediaFiles.indexOf(URI);
                        if (index != -1) {
                            for (int i = 0; i < index; i++) {
                                mediaFiles.remove(0);
                                mediaFilesDiscontinuities.remove(0);
                                if (mediaFileIndex == -1) {
                                    forceDiscontinuity = true;
                                }
                                if (mediaFileIndex >= 0) {
                                    mediaFileIndex--;
                                }
                            }
                        }
                        sequenceNumberUpdated = false;
                    }

                    if (mediaFiles.contains(URI)) {
                        return; // Nothing to add
                    }
                }

                mediaFiles.add(URI);
                mediaFilesDiscontinuities.add(isDiscontinuity);

                if (isLive) {
                    if (isLiveWaiting) {
                        liveSemaphore.release();
                    }
                } else {
                    mediaFilesStartTimes.add(this.startTime);
                    this.startTime += duration;

                    // For fragmented MP4 we should not add duration of first
                    // segment, since it is header without actually data.
                    if (mediaFiles.size() == 1) {
                        if (!isFragmentedMP4()) {
                            this.duration += duration;
                        }
                    } else {
                        this.duration += duration;
                    }
                }
            }
        }

        String getNextMediaFile() {
            if (isLive) {
                synchronized (lock) {
                    isLiveWaiting = ((mediaFileIndex + 1) >= mediaFiles.size());
                }
                if (isLiveWaiting) {
                    try {
                        liveSemaphore.acquire();
                        isLiveWaiting = false;
                        if (isLiveStop) {
                            isLiveStop = false;
                            return null;
                        }
                    } catch (InterruptedException e) {
                        isLiveWaiting = false;
                        return null;
                    }
                }
                if (isPlaylistClosed) {
                    return null;
                }
            }

            synchronized (lock) {
                mediaFileIndex++;
                if (mediaFileIndex < mediaFiles.size()) {
                    if (baseURI != null) {
                        return baseURI + mediaFiles.get(mediaFileIndex);
                    } else {
                        return mediaFiles.get(mediaFileIndex);
                    }
                } else {
                    return null;
                }
            }
        }

        String getHeaderFile() {
            synchronized (lock) {
                if (mediaFiles.size() > 0) {
                    if (baseURI != null) {
                        return baseURI + mediaFiles.get(0);
                    } else {
                        return mediaFiles.get(0);
                    }
                } else {
                    return null;
                }
            }
        }

        double getMediaFileStartTime() {
            return getMediaFileStartTime(mediaFileIndex);
        }

        double getMediaFileStartTime(int index) {
            if (index >= 0 && index < mediaFilesStartTimes.size()) {
                // Special case if video is fMP4 and audio ext is not
                if (isVideoStreamFragmentedMP4 && !isFragmentedMP4() && index != 0) {
                    return (mediaFilesStartTimes.get(index) + (double) videoStreamTargetDuration);
                } else {
                    return mediaFilesStartTimes.get(index);
                }
            }

            return -1.0;
        }

        double getDuration() {
            return duration;
        }

        void setForceDiscontinuity(boolean value) {
            forceDiscontinuity = value;
        }

        boolean isCurrentMediaFileDiscontinuity() {
            if (forceDiscontinuity) {
                forceDiscontinuity = false;
                return true;
            } else {
                return mediaFilesDiscontinuities.get(mediaFileIndex);
            }
        }

        double seekGetStartTime(long time) {
            synchronized (lock) {
                int newIndex = 0;
                if (isLive) {
                    if (time == 0) {
                        return 0.0;
                    }
                } else {
                    time += targetDuration / 2000;

                    int mediaFileStartTimeSize = mediaFilesStartTimes.size();

                    for (int index = 0; index < mediaFileStartTimeSize; index++) {
                        if (time >= mediaFilesStartTimes.get(index)) {
                            if (index + 1 < mediaFileStartTimeSize) {
                                if (time < mediaFilesStartTimes.get(index + 1)) {
                                    if (isFragmentedMP4()) {
                                        newIndex = index; // We need to skip header
                                    } else {
                                        newIndex = index - 1; // Load segment will increment mediaFileIndex
                                    }
                                    // Special case for seek to 0 and fragmented MP4.
                                    // We should return 0, instead of first segment, since
                                    // first segment starts with target duration, but
                                    // GStreamer expects 0 as start of stream time.
                                    // Start of segment will be set to target duration
                                    // when reported from HLS_PROP_SEGMENT_START_TIME.
                                    // Same should be for video fMP4 and raw audio.
                                    if (time == 0 && isFragmentedMP4()) {
                                        return 0.0;
                                    }
                                    // Return start time of segment we will load (mediaFileIndex + 1)
                                    return getMediaFileStartTime(newIndex + 1);
                                }
                            } else {
                                if ((time - targetDuration / 2000) < duration) {
                                    if (isFragmentedMP4()) {
                                        newIndex = index; // We need to skip header
                                    } else {
                                        newIndex = index - 1; // Load segment will increment mediaFileIndex
                                    }
                                    // Return start time of segment we will load (mediaFileIndex + 1)
                                    return getMediaFileStartTime(newIndex + 1);
                                } else if (Double.compare(time - targetDuration / 2000, duration) == 0) {
                                    return duration;
                                }
                            }
                        }
                    }
                }
            }

            return -1.0;
        }

        double seek(long time) {
            synchronized (lock) {
                if (isLive) {
                    if (time == 0) {
                        if (isFragmentedMP4()) {
                            mediaFileIndex = 0; // Skip header at 0 index
                            // we will send it with first segment if needed.
                        } else {
                            mediaFileIndex = -1;
                        }
                        if (isLiveWaiting) {
                            isLiveStop = true;
                            liveSemaphore.release();
                        }
                        return 0;
                    }
                } else {
                    time += targetDuration / 2000;

                    int mediaFileStartTimeSize = mediaFilesStartTimes.size();

                    for (int index = 0; index < mediaFileStartTimeSize; index++) {
                        if (time >= mediaFilesStartTimes.get(index)) {
                            if (index + 1 < mediaFileStartTimeSize) {
                                if (time < mediaFilesStartTimes.get(index + 1)) {
                                    if (isFragmentedMP4()) {
                                        mediaFileIndex = index; // We need to skip header
                                    } else {
                                        mediaFileIndex = index - 1; // Load segment will increment mediaFileIndex
                                    }
                                    // Special case for seek to 0 and fragmented MP4.
                                    // We should return 0, instead of first segment, since
                                    // first segment starts with target duration, but
                                    // GStreamer expects 0 as start of stream time.
                                    // Start of segment will be set to target duration
                                    // when reported from HLS_PROP_SEGMENT_START_TIME.
                                    // Same should be for video fMP4 and raw audio.
                                    if (time == 0 && isFragmentedMP4()) {
                                        return 0.0;
                                    }
                                    // Return start time of segment we will load (mediaFileIndex + 1)
                                    return getMediaFileStartTime(mediaFileIndex + 1);
                                }
                            } else {
                                if ((time - targetDuration / 2000) < duration) {
                                    if (isFragmentedMP4()) {
                                        mediaFileIndex = index; // We need to skip header
                                    } else {
                                        mediaFileIndex = index - 1; // Load segment will increment mediaFileIndex
                                    }
                                    // Return start time of segment we will load (mediaFileIndex + 1)
                                    return getMediaFileStartTime(mediaFileIndex + 1);
                                } else if (Double.compare(time - targetDuration / 2000, duration) == 0) {
                                    return duration;
                                }
                            }
                        }
                    }
                }
            }

            return -1;
        }

        int getMimeType() {
            synchronized (lock) {
                if (mimeType == HLS_VALUE_MIMETYPE_UNKNOWN) {
                    if (mediaFiles.size() > 0) {
                        if (stripParameters(mediaFiles.get(0)).endsWith(".ts")) {
                            mimeType = HLS_VALUE_MIMETYPE_MP2T;
                        } else if (stripParameters(mediaFiles.get(0)).endsWith(".mp3")) {
                            mimeType = HLS_VALUE_MIMETYPE_MP3;
                        } else if (stripParameters(mediaFiles.get(0)).endsWith(".mp4")
                                || stripParameters(mediaFiles.get(0)).endsWith(".m4s")) {
                            mimeType = HLS_VALUE_MIMETYPE_FMP4;
                        } else if (stripParameters(mediaFiles.get(0)).endsWith(".aac")) {
                            mimeType = HLS_VALUE_MIMETYPE_AAC;
                        }
                    }
                }
            }

            return mimeType;
        }

        String getMediaFileExtension() {
            synchronized (lock) {
                if (mediaFiles.size() > 0) {
                    String mediaFile = stripParameters(mediaFiles.get(0));
                    int index = mediaFile.lastIndexOf(".");
                    if (index != -1) {
                        return mediaFile.substring(index);
                    }
                }
            }

            return null;
        }

        boolean setSequenceNumber(int value) {
            if (sequenceNumberStart == -1) {
                sequenceNumberStart = value;
            } else if (sequenceNumber != value) {
                sequenceNumberUpdated = true;
                sequenceNumber = value;
            } else {
                return false;
            }

            return true;
        }

        void close() {
            if (isLive) {
                isPlaylistClosed = true;
                liveSemaphore.release();
            }
        }

        private void setBaseURI(String playlistURI, String URI) {
            if (!URI.startsWith("http://") && !URI.startsWith("https://")) {
                baseURI = playlistURI.substring(0, playlistURI.lastIndexOf("/") + 1);
            }
            needBaseURI = false;
        }

        void setAudioGroupID(String value) {
            audioGroupID = value;
        }

        String getAudioGroupID() {
            return audioGroupID;
        }
    }
}
