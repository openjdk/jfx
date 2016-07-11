/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.media.jfxmediaimpl.MediaUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

final class HLSConnectionHolder extends ConnectionHolder {

    private URLConnection urlConnection = null;
    private PlaylistThread playlistThread = new PlaylistThread();
    private VariantPlaylist variantPlaylist = null;
    private Playlist currentPlaylist = null;
    private int mediaFileIndex = -1;
    private CountDownLatch readySignal = new CountDownLatch(1);
    private Semaphore liveSemaphore = new Semaphore(0);
    private boolean isPlaylistClosed = false;
    private boolean isBitrateAdjustable = false;
    private long startTime = -1;
    private static final long HLS_VALUE_FLOAT_MULTIPLIER = 1000;
    private static final int HLS_PROP_GET_DURATION = 1;
    private static final int HLS_PROP_GET_HLS_MODE = 2;
    private static final int HLS_PROP_GET_MIMETYPE = 3;
    private static final int HLS_VALUE_MIMETYPE_MP2T = 1;
    private static final int HLS_VALUE_MIMETYPE_MP3 = 2;
    private static final String CHARSET_UTF_8 = "UTF-8";
    private static final String CHARSET_US_ASCII = "US-ASCII";

    HLSConnectionHolder(URI uri) throws IOException {
        playlistThread.setPlaylistURI(uri);
        init();
    }

    private void init() {
        playlistThread.putState(PlaylistThread.STATE_INIT);
        playlistThread.start();
    }

    @Override
    public int readNextBlock() throws IOException {
        if (isBitrateAdjustable && startTime == -1) {
            startTime = System.currentTimeMillis();
        }

        int read = super.readNextBlock();
        if (isBitrateAdjustable && read == -1) {
            long readTime = System.currentTimeMillis() - startTime;
            startTime = -1;
            adjustBitrate(readTime);
        }

        return read;
    }

    int readBlock(long position, int size) throws IOException {
        throw new IOException();
    }

    boolean needBuffer() {
        return true;
    }

    boolean isSeekable() {
        return true;
    }

    boolean isRandomAccess() {
        return false; // Only by segments
    }

    public long seek(long position) {
        try {
            readySignal.await();
        } catch (Exception e) {
            return -1;
        }

        return (long) (currentPlaylist.seek(position) * HLS_VALUE_FLOAT_MULTIPLIER);
    }

    @Override
    public void closeConnection() {
        currentPlaylist.close();
        super.closeConnection();
        resetConnection();
        playlistThread.putState(PlaylistThread.STATE_EXIT);
    }

    @Override
    int property(int prop, int value) {
        try {
            readySignal.await();
        } catch (Exception e) {
            return -1;
        }

        if (prop == HLS_PROP_GET_DURATION) {
            return (int) (currentPlaylist.getDuration() * HLS_VALUE_FLOAT_MULTIPLIER);
        } else if (prop == HLS_PROP_GET_HLS_MODE) {
            return 1;
        } else if (prop == HLS_PROP_GET_MIMETYPE) {
            return currentPlaylist.getMimeType();
        }

        return -1;
    }

    @Override
    int getStreamSize() {
        try {
            readySignal.await();
        } catch (Exception e) {
            return -1;
        }

        return loadNextSegment();
    }

    private void resetConnection() {
        super.closeConnection();

        Locator.closeConnection(urlConnection);
        urlConnection = null;
    }

    // Returns -1 EOS or critical error
    // Returns positive size of segment if no isssues.
    // Returns negative size of segment if discontinuity.
    private int loadNextSegment() {
        resetConnection();

        String mediaFile = currentPlaylist.getNextMediaFile();
        if (mediaFile == null) {
            return -1;
        }

        try {
            URI uri = new URI(mediaFile);
            urlConnection = uri.toURL().openConnection();
            channel = openChannel();
        } catch (Exception e) {
            return -1;
        }

        if (currentPlaylist.isCurrentMediaFileDiscontinuity()) {
            return (-1 * urlConnection.getContentLength());
        } else {
            return urlConnection.getContentLength();
        }
    }

    private ReadableByteChannel openChannel() throws IOException {
        return Channels.newChannel(urlConnection.getInputStream());
    }

    private void adjustBitrate(long readTime) {
        int avgBitrate = (int)(((long) urlConnection.getContentLength() * 8 * 1000) / readTime);

        Playlist playlist = variantPlaylist.getPlaylistBasedOnBitrate(avgBitrate);
        if (playlist != null && playlist != currentPlaylist) {
            if (currentPlaylist.isLive()) {
                playlist.update(currentPlaylist.getNextMediaFile());
                playlistThread.setReloadPlaylist(playlist);
            }

            playlist.setForceDiscontinuity(true);
            currentPlaylist = playlist;
        }
    }

    private static String stripParameters(String mediaFile) {
        int qp = mediaFile.indexOf('?');
        if (qp > 0) {
            mediaFile = mediaFile.substring(0, qp); // Strip all possible http parameters.
        }
        return mediaFile;
    }

    private class PlaylistThread extends Thread {

        public static final int STATE_INIT = 0;
        public static final int STATE_EXIT = 1;
        public static final int STATE_RELOAD_PLAYLIST = 2;
        private BlockingQueue<Integer> stateQueue = new LinkedBlockingQueue<Integer>();
        private URI playlistURI = null;
        private Playlist reloadPlaylist = null;
        private final Object reloadLock = new Object();
        private volatile boolean stopped = false;

        private PlaylistThread() {
            setName("JFXMedia HLS Playlist Thread");
            setDaemon(true);
        }

        private void setPlaylistURI(URI playlistURI) {
            this.playlistURI = playlistURI;
        }

        private void setReloadPlaylist(Playlist playlist) {
            synchronized(reloadLock) {
                reloadPlaylist = playlist;
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
                } catch (Exception e) {
                }
            }
        }

        private void putState(int state) {
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

            PlaylistParser parser = new PlaylistParser();
            parser.load(playlistURI);

            if (parser.isVariantPlaylist()) {
                variantPlaylist = new VariantPlaylist(playlistURI);

                while (parser.hasNext()) {
                    variantPlaylist.addPlaylistInfo(parser.getString(), parser.getInteger());
                }
            } else {
                if (currentPlaylist == null) {
                    currentPlaylist = new Playlist(parser.isLivePlaylist(), parser.getTargetDuration());
                    currentPlaylist.setPlaylistURI(playlistURI);
                }

                if (currentPlaylist.setSequenceNumber(parser.getSequenceNumber())) {
                    while (parser.hasNext()) {
                        currentPlaylist.addMediaFile(parser.getString(), parser.getDouble(), parser.getBoolean());
                    }
                }

                if (variantPlaylist != null) {
                    variantPlaylist.addPlaylist(currentPlaylist);
                }
            }

            // Update variant playlists
            if (variantPlaylist != null) {
                while (variantPlaylist.hasNext()) {
                    try {
                        currentPlaylist = new Playlist(variantPlaylist.getPlaylistURI());
                        currentPlaylist.update(null);
                        variantPlaylist.addPlaylist(currentPlaylist);
                    } catch (URISyntaxException e) {
                    } catch (MalformedURLException e) {
                    }
                }
            }

            // Always start with first data playlist
            if (variantPlaylist != null) {
                currentPlaylist = variantPlaylist.getPlaylist(0);
                isBitrateAdjustable = true;
            }

            // Start reloading live playlist
            if (currentPlaylist.isLive()) {
                setReloadPlaylist(currentPlaylist);
                putState(STATE_RELOAD_PLAYLIST);
            }

            readySignal.countDown();
        }

        private void stateReloadPlaylist() {
            try {
                long timeout;
                synchronized(reloadLock) {
                    timeout = reloadPlaylist.getTargetDuration() / 2;
                }
                Thread.sleep(timeout);
            } catch (InterruptedException ex) {
                return;
            }

            synchronized(reloadLock) {
                reloadPlaylist.update(null);
            }

            putState(STATE_RELOAD_PLAYLIST);
        }
    }

    private static class PlaylistParser {

        private boolean isFirstLine = true;
        private boolean isLineMediaFileURI = false;
        private boolean isEndList = false;
        private boolean isLinePlaylistURI = false;
        private boolean isVariantPlaylist = false;
        private boolean isDiscontinuity = false;
        private int targetDuration = 0;
        private int sequenceNumber = 0;
        private int dataListIndex = -1;
        private List<String> dataListString = new ArrayList<String>();
        private List<Integer> dataListInteger = new ArrayList<Integer>();
        private List<Double> dataListDouble = new ArrayList<Double>();
        private List<Boolean> dataListBoolean = new ArrayList<Boolean>();

        private void load(URI uri) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    MediaUtils.error(this, MediaError.ERROR_LOCATOR_CONNECTION_LOST.code(), "HTTP responce code: " + connection.getResponseCode(), null);
                }

                Charset charset = getCharset(uri.toURL().toExternalForm(), connection.getContentType());
                if (charset != null) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
                }

                if (reader != null) {
                    boolean result;
                    do {
                        result = parseLine(reader.readLine());
                    } while (result);
                }
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {}

                    Locator.closeConnection(connection);
                }
            }
        }

        private boolean isVariantPlaylist() {
            return isVariantPlaylist;
        }

        private boolean isLivePlaylist() {
            return !isEndList;
        }

        private int getTargetDuration() {
            return targetDuration;
        }

        private int getSequenceNumber() {
            return sequenceNumber;
        }

        private boolean hasNext() {
            dataListIndex++;
            if (dataListString.size() > dataListIndex || dataListInteger.size() > dataListIndex || dataListDouble.size() > dataListIndex || dataListBoolean.size() > dataListIndex) {
                return true;
            } else {
                return false;
            }
        }

        private String getString() {
            return dataListString.get(dataListIndex);
        }

        private Integer getInteger() {
            return dataListInteger.get(dataListIndex);
        }

        private Double getDouble() {
            return dataListDouble.get(dataListIndex);
        }

        private Boolean getBoolean() {
            return dataListBoolean.get(dataListIndex);
        }

        private boolean parseLine(String line) {
            if (line == null) {
                return false;
            }

            // First line of playlist must be "#EXTM3U"
            if (isFirstLine) {
                if (line.compareTo("#EXTM3U") != 0) {
                    return false;
                }

                isFirstLine = false;
                return true;
            }

            // Ignore blank lines and comments
            if (line.isEmpty() || (line.startsWith("#") && !line.startsWith("#EXT"))) {
                return true;
            }

            if (line.startsWith("#EXTINF")) { // #EXTINF
                //#EXTINF:<duration>,<title>
                String[] s1 = line.split(":");
                if (s1.length == 2 && s1[1].length() > 0) {
                    String[] s2 = s1[1].split(",");
                    if (s2.length >= 1) { // We have duration
                        dataListDouble.add(Double.parseDouble(s2[0]));
                    }
                }

                isLineMediaFileURI = true;
            } else if (line.startsWith("#EXT-X-TARGETDURATION")) {
                // #EXT-X-TARGETDURATION:<s>
                String[] s1 = line.split(":");
                if (s1.length == 2 && s1[1].length() > 0) {
                    targetDuration = Integer.parseInt(s1[1]);
                }
            } else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")) {
                // #EXT-X-MEDIA-SEQUENCE:<number>
                String[] s1 = line.split(":");
                if (s1.length == 2 && s1[1].length() > 0) {
                    sequenceNumber = Integer.parseInt(s1[1]);
                }
            } else if (line.startsWith("#EXT-X-STREAM-INF")) {
                // #EXT-X-STREAM-INF:<attribute-list>
                isVariantPlaylist = true;

                int bitrate = 0;
                String[] s1 = line.split(":");
                if (s1.length == 2 && s1[1].length() > 0) {
                    String[] s2 = s1[1].split(",");
                    if (s2.length > 0) {
                        for (int i = 0; i < s2.length; i++) {
                            s2[i] = s2[i].trim();
                            if (s2[i].startsWith("BANDWIDTH")) {
                                String[] s3 = s2[i].split("=");
                                if (s3.length == 2 && s3[1].length() > 0) {
                                    bitrate = Integer.parseInt(s3[1]);
                                }
                            }
                        }
                    }
                }

                if (bitrate < 1) {
                    return false;
                }

                dataListInteger.add(bitrate);

                isLinePlaylistURI = true; // Next line will be URI to playlist
            } else if (line.startsWith("#EXT-X-ENDLIST")) { // #EXT-X-ENDLIST
                isEndList = true;
            } else if (line.startsWith("#EXT-X-DISCONTINUITY")) { // #EXT-X-DISCONTINUITY
                isDiscontinuity = true;
            } else if (isLinePlaylistURI) {
                isLinePlaylistURI = false;
                dataListString.add(line);
            } else if (isLineMediaFileURI) {
                isLineMediaFileURI = false;
                dataListString.add(line);
                dataListBoolean.add(isDiscontinuity);
                isDiscontinuity = false;
            }

            return true;
        }

        private Charset getCharset(String url, String mimeType) {
            if ((url != null && stripParameters(url).endsWith(".m3u8")) || (mimeType != null && mimeType.equals("application/vnd.apple.mpegurl"))) {
                if (Charset.isSupported(CHARSET_UTF_8)) {
                    return Charset.forName(CHARSET_UTF_8);
                }
            } else if ((url != null && stripParameters(url).endsWith(".m3u")) || (mimeType != null && mimeType.equals("audio/mpegurl"))) {
                if (Charset.isSupported(CHARSET_US_ASCII)) {
                    return Charset.forName(CHARSET_US_ASCII);
                }
            }

            return null;
        }
    }

    private static class VariantPlaylist {

        private URI playlistURI = null;
        private int infoIndex = -1;
        private List<String> playlistsLocations = new ArrayList<String>();
        private List<Integer> playlistsBitrates = new ArrayList<Integer>();
        private List<Playlist> playlists = new ArrayList<Playlist>();
        private String mediaFileExtension = null; // Will be set to media file extension of first playlist

        private VariantPlaylist(URI uri) {
            playlistURI = uri;
        }

        private void addPlaylistInfo(String location, int bitrate) {
            playlistsLocations.add(location);
            playlistsBitrates.add(bitrate);
        }

        private void addPlaylist(Playlist playlist) {
            if (mediaFileExtension == null) {
                mediaFileExtension = playlist.getMediaFileExtension();
            } else {
                if (!mediaFileExtension.equals(playlist.getMediaFileExtension())) {
                    playlistsLocations.remove(infoIndex);
                    playlistsBitrates.remove(infoIndex);
                    infoIndex--;
                    return; // Ignore playlist with different media type
                }
            }
            playlists.add(playlist);
        }

        private Playlist getPlaylist(int index) {
            if (playlists.size() > index) {
                return playlists.get(index);
            } else {
                return null;
            }
        }

        private boolean hasNext() {
            infoIndex++;
            if (playlistsLocations.size() > infoIndex && playlistsBitrates.size() > infoIndex) {
                return true;
            } else {
                return false;
            }
        }

        private URI getPlaylistURI() throws URISyntaxException, MalformedURLException {
            String location = playlistsLocations.get(infoIndex);
            if (location.startsWith("http://") || location.startsWith("https://")) {
                return new URI(location);
            } else {
                return new URI(playlistURI.toURL().toString().substring(0, playlistURI.toURL().toString().lastIndexOf("/") + 1) + location);
            }
        }

        private Playlist getPlaylistBasedOnBitrate(int bitrate) {
            int playlistIndex = -1;
            int playlistBitrate = 0;

            // Get bitrate that less then requested bitrate, but most closed to it
            for (int i = 0; i < playlistsBitrates.size(); i++) {
                int b = playlistsBitrates.get(i);
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
                for (int i = 0; i < playlistsBitrates.size(); i++) {
                    int b = playlistsBitrates.get(i);
                    if (b < playlistBitrate || playlistIndex == -1) {
                        playlistBitrate = b;
                        playlistIndex = i;
                    }
                }
            }

            // Just in case
            if (playlistIndex < 0 || playlistIndex >= playlists.size()) {
                return null;
             } else {
                return playlists.get(playlistIndex);
            }
        }
    }

    private class Playlist {

        private boolean isLive = false;
        private volatile boolean isLiveWaiting = false;
        private volatile boolean isLiveStop = false;
        private long targetDuration = 0;
        private URI playlistURI = null;
        private final Object lock = new Object();
        private List<String> mediaFiles = new ArrayList<String>();
        private List<Double> mediaFilesStartTimes = new ArrayList<Double>();
        private List<Boolean> mediaFilesDiscontinuities = new ArrayList<Boolean>();
        private boolean needBaseURI = true;
        private String baseURI = null;
        private double duration = 0.0;
        private int sequenceNumber = -1;
        private int sequenceNumberStart = -1;
        private boolean sequenceNumberUpdated = false;
        private boolean forceDiscontinuity = false;

        private Playlist(boolean isLive, int targetDuration) {
            this.isLive = isLive;
            this.targetDuration = targetDuration * 1000;

            if (isLive) {
                duration = -1.0;
            }
        }

        private Playlist(URI uri) {
            playlistURI = uri;
        }

        private void update(String nextMediaFile) {
            PlaylistParser parser = new PlaylistParser();
            parser.load(playlistURI);

            isLive = parser.isLivePlaylist();
            targetDuration = parser.getTargetDuration() * 1000;

            if (isLive) {
                duration = -1.0;
            }

            if (setSequenceNumber(parser.getSequenceNumber())) {
                while (parser.hasNext()) {
                    addMediaFile(parser.getString(), parser.getDouble(), parser.getBoolean());
                }
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

        private boolean isLive() {
            return isLive;
        }

        private long getTargetDuration() {
            return targetDuration;
        }

        private void setPlaylistURI(URI uri) {
            playlistURI = uri;
        }

        private void addMediaFile(String URI, double duration, boolean isDiscontinuity) {
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
                    mediaFilesStartTimes.add(this.duration);
                    this.duration += duration;
                }
            }
        }

        private String getNextMediaFile() {
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
                if ((mediaFileIndex) < mediaFiles.size()) {
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

        private double getDuration() {
            return duration;
        }

        private void setForceDiscontinuity(boolean value) {
            forceDiscontinuity = value;
        }

        private boolean isCurrentMediaFileDiscontinuity() {
            if (forceDiscontinuity) {
                forceDiscontinuity = false;
                return true;
            } else {
                return mediaFilesDiscontinuities.get(mediaFileIndex);
            }
        }

        private double seek(long time) {
            synchronized (lock) {
                if (isLive) {
                    if (time == 0) {
                        mediaFileIndex = -1;
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
                                    mediaFileIndex = index - 1; // Seek will load segment and increment mediaFileIndex
                                    return mediaFilesStartTimes.get(index);
                                }
                            } else {
                                if ((time - targetDuration / 2000) < duration) {
                                    mediaFileIndex = index - 1; // Seek will load segment and increment mediaFileIndex
                                    return mediaFilesStartTimes.get(index);
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

        private int getMimeType() {
            synchronized (lock) {
                if (mediaFiles.size() > 0) {
                    if (stripParameters(mediaFiles.get(0)).endsWith(".ts")) {
                        return HLS_VALUE_MIMETYPE_MP2T;
                    } else if (stripParameters(mediaFiles.get(0)).endsWith(".mp3")) {
                        return HLS_VALUE_MIMETYPE_MP3;
                    }
                }
            }

            return -1;
        }

        private String getMediaFileExtension() {
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

        private boolean setSequenceNumber(int value) {
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

        private void close() {
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
    }
}
