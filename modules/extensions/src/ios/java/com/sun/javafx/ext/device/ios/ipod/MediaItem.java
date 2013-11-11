/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.ext.device.ios.ipod;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javafx.util.Duration;
import javafx.scene.image.Image;

/**
 * The MediaItem class represents a single media item from the iPod library. It encapsulates each media item's
 * properties, such as media type, genre, title, artist, lyrics, release date and others. For a full list of 
 * media item properties see the Methods section.
 * <br/>
 * In order to use a particular media item with the JavaFX Media API, use <code>MediaItem.getURL()</code> method 
 * to obtain a String representation of the media item's location that can be used to create an instance of the 
 * <code>javafx.scene.media.Media</code> class. 
 */
public class MediaItem {

    // enum values correspond directly to MPMediaItemPropertyMediaType values on iOS
    
    /**
     * The MediaItemType enum defines all possible media item types. Use this enum's values to create a MediaFilter
     * filter that filters out media items according to their media type.
     * <br/>
     * See <code>MediaFilter</code> and <code>MediaFilter.MediaFilterType.MediaType</code>
     */
    public static enum MediaItemType {
        /**
         * The media item contains music.
         */
        Music       (1 << 0),
        /**
         * The media item contains a podcast.
         */
        Podcast     (1 << 1),
        /**
         * The media item contains an audio book.
         */
        AudioBook   (1 << 2),
        /**
         * The media item contains an unspecified type of audio content.
         */
        AnyAudio    (0x00ff),
        /**
         * The media item contains a movie.
         */
        Movie       (1 << 8),
        /**
         * The media item contains a TV show.
         */
        TVShow      (1 << 9),
        /**
         * The media item contains a video podcast.
         */
        VideoPodcast(1 << 10),
        /**
         * The media item contains a music video.
         */
        MusicVideo  (1 << 11),
        /**
         * The media item contains an iTunes U video.
         */
        VideoITunesU(1 << 12),
        /**
         * The media item contains an unspecified type of video content.
         */
        AnyVideo    (0xff00),
        /**
         * The media item contains an unspecified type of audio.
         */
        Any         (~0);
        
        // we need this mapping to be able to translate int values received from native code in an efficient manner
        // calling values() would create a new array each time and lookup would be O(n)
        private static final Map<Integer, MediaItemType> iosEnumCodes;
        
        static {
            iosEnumCodes = new HashMap<Integer, MediaItemType>();
            iosEnumCodes.put(Music.getValue(), Music);
            iosEnumCodes.put(Podcast.getValue(), Podcast);
            iosEnumCodes.put(AudioBook.getValue(), AudioBook);
            iosEnumCodes.put(AnyAudio.getValue(), AnyAudio);
            iosEnumCodes.put(Movie.getValue(), Movie);
            iosEnumCodes.put(TVShow.getValue(), TVShow);
            iosEnumCodes.put(VideoPodcast.getValue(), VideoPodcast);
            iosEnumCodes.put(MusicVideo.getValue(), MusicVideo);
            iosEnumCodes.put(VideoITunesU.getValue(), VideoITunesU);
            iosEnumCodes.put(AnyVideo.getValue(), AnyVideo);
            iosEnumCodes.put(Any.getValue(), Any);
        }
        
        private final int value;
        
        private MediaItemType(int value) {
            this.value = value;
        }
        
        /**
         * Returns the integer value associated with this enum value.
         * @return the integer value associated with this enum value.
         */
        public int getValue() {
            return value;
        }
        
        private static MediaItemType fromIntValue(int intValue) {
            MediaItemType mediaItemType = null;
            if (iosEnumCodes.containsKey(intValue)) {
                mediaItemType = iosEnumCodes.get(intValue);
            }
            return mediaItemType;
        }
    }

    private MediaItemType mediaType;
    private String title;
    private String albumTitle;
    private String artist;
    private String albumArtist;
    private String genre;
    private String composer;
    private Duration playbackDuration;
    private int albumTrackNumber;
    private int albumTrackCount;
    private int discNumber;
    private int discCount;
    private Image artwork;
    private String lyrics;
    private boolean isCompilation;
    private Calendar releaseDate;
    private int beatsPerMinute;
    private String comments;
    private String url;

    MediaItem() {
        
    }
    
    /**
     * Returns the media type of this media item. See the MediaItemType enum for possible values.
     * @return the media type of this media item
     */
    public MediaItemType getMediaType() {
        return mediaType;
    }
    
    /**
     * Returns the title (or name) of this media item.
     * @return the title (or name) of this media item.
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Returns the title of an album, such as "Thriller", as opposed to the title of an individual 
     * song on the album, such as "Beat It"
     * @return the title of an album
     */
    public String getAlbumTitle() {
        return albumTitle;
    }
    
    /**
     * Returns the performing artist(s) for this media item, which may vary from the primary artist for the album 
     * that a media item belongs to. For example, if the album artist is "Joseph Fable", the artist for one of 
     * the songs in the album may be "Joseph Fable featuring Thomas Smithson".
     * @return the performing artist(s) for this media item
     */
    public String getArtist() {
        return artist;
    }
    
    /**
     * Returns the primary performing artist for an album as a whole.
     * @return the primary performing artist for an album as a whole
     */
    public String getAlbumArtist() {
        return albumArtist;
    }
    
    /**
     * Returns the musical or film genre of this media item.
     * @return the musical or film genre of this media item
     */
    public String getGenre() {
        return genre;
    }
    
    /**
     * Returns the musical composer for this media item.
     * @return the musical composer for this media item
     */
    public String getComposer() {
        return composer;
    }
    
    /**
     * Returns the playback duration of this media item.
     * @return the playback duration of this media item
     */
    public Duration getPlaybackDuration() {
        return playbackDuration;
    }
    
    /**
     * Returns the track number of this media item, if it is part of an album.
     * @return the track number of this media item
     */
    public int getAlbumTrackNumber() {
        return albumTrackNumber;
    }
    
    /**
     * Returns the number of tracks in the album that contains this media item.
     * @return the number of tracks in the album that contains this media item
     */
    public int getAlbumTrackCount() {
        return albumTrackCount;
    }
    
    /**
     * Returns the disc number of this media item, provided it is part of a multi-disc album.
     * @return the disc number of this media item
     */
    public int getDiscNumber() {
        return discNumber;
    }
    
    /**
     * Returns the number of discs in the album that contains this media item.
     * @return the number of discs in the album that contains this media item
     */
    public int getDiscCount() {
        return discCount;
    }
    
    /**
     * Returns the artwork image for this media item. <strong>Not implemented</strong>.
     * @return the artwork image for this media item
     */
    public Image getArtwork() {
        return artwork;
    }
    
    /**
     * Returns the lyrics for this media item.
     * @return the lyrics for this media item
     */
    public String getLyrics() {
        return lyrics;
    }
    
    /**
     * Indicates whether this media item is part of a compilation or not. Corresponds to the 
     * "Part of a compilation" checkbox in the Info tab in the Get Info dialog in iTunes.
     * @return a boolean value indicating whether this media item is part of a compilation
     */
    public boolean isCompilation() {
        return isCompilation;
    }
    
    /**
     * Returns the date on which the media item was first publicly released.
     * @return the date on which the media item was first publicly released
     */
    public Calendar getReleaseDate() {
        return releaseDate;
    }
    
    /**
     * Returns the number of musical beats per minute for the media item, corresponding to the "BPM" field 
     * in the Info tab in the Get Info dialog in iTunes.
     * @return the number of musical beats per minute (BPM)
     */
    public int getBeatsPerMinute() {
        return beatsPerMinute;
    }
    
    /**
     * Returns textual information about this media item, corresponding to the "Comments" field in the 
     * Info tab in the Get Info dialog in iTunes.
     * @return textual information about this media item
     */
    public String getComments() {
        return comments;
    }
    
    /**
     * Returns a URL pointing to the media item as a String. This value can be passed to the constructor of
     * the <code>javafx.scene.media.Media</code> class and thus used with the JavaFX media API. The URL has 
     * the custom scheme of <code>ipod-library</code>. For example, a URL might look like this:
     * <br/>
     * <code>ipod-library://item/item.m4a?id=12345</code> 
     * @return a URL pointing to the media item
     */
    public String getURL() {
        return url;
    }

    // setters have package access, will be set from native
    void setMediaType(final MediaItemType mediaType) {
        this.mediaType = mediaType;
    }
    
    // a convenience method to simplify the native code
    void setMediaType(final int mediaType) {
        this.mediaType = MediaItemType.fromIntValue(mediaType);
    }

    void setTitle(final String title) {
        this.title = title;
    }
    
    void setAlbumTitle(final String albumTitle) {
        this.albumTitle = albumTitle;
    }
    
    void setArtist(final String artist) {
        this.artist = artist;
    }
    
    void setAlbumArtist(final String albumArtist) {
        this.albumArtist = albumArtist;
    }
    
    void setGenre(final String genre) {
        this.genre = genre;
    }
    
    void setComposer(final String composer) {
        this.composer = composer;
    }
    
    void setPlaybackDuration(final Duration playbackDuration) {
        this.playbackDuration = playbackDuration;
    }
    
    void setAlbumTrackNumber(final int albumTrackNumber) {
        this.albumTrackNumber = albumTrackNumber;
    }
    
    void setAlbumTrackCount(final int albumTrackCount) {
        this.albumTrackNumber = albumTrackCount;
    }
    
    void setDiscNumber(final int discNumber) {
        this.discNumber = discNumber;
    }
    
    void setDiscCount(final int discCount) {
        this.discCount = discCount;
    }
    
    void setLyrics(final String lyrics) {
        this.lyrics = lyrics;
    }
    
    void setIsCompilation(final boolean isCompilation) {
        this.isCompilation = isCompilation;
    }
    
    void setReleaseDate(final Calendar releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    void setBeatsPerMinute(final int beatsPerMinute) {
        this.beatsPerMinute = beatsPerMinute;
    }
    
    void setComments(final String comments) {
        this.comments = comments;
    }
    
    void setURL(final String url) {
        this.url = url;
    }

}
