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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.sun.javafx.ext.device.ios.ipod.MediaFilter.MediaFilterType;

/**
 * The MediaQuery class is the central entity of the iPod library access API. Its purpose is to provide
 * access to the user's iPod library content. It allows you to retrieve lists of media items fulfilling 
 * certain criteria and group them by some shared properties. E.g. it is possible to filter out media items
 * of a particular genre, composer, title, etc. See the MediaFilter class description for the list of 
 * available filters. It is possible to use multiple filters at the same time.
 * <br/>
 * The MediaQuery class also supports so called grouping. Grouping allows you to retrieve sorted and arranged 
 * collections of media items. The arrangement you get depends on the value you set by calling the
 * MediaQuery.setGrouping() method.
 * <br/>
 * The possible arrangements you can achieve with grouping are as follows:
 * <ul>
 * <li><strong>Title</strong> - groups and sorts media item collections by title.</li>
 * <li><strong>Album</strong> - groups and sorts media item collections by album, 
 * and sorts songs within an album by track order.</li>
 * <li><strong>Artist</strong> - groups and sorts media item collections by performing artist.</li>
 * <li><strong>AlbumArtist</strong> - groups and sorts media item collections by album artist 
 * (the primary performing artist for an album as a whole).</li>
 * <li><strong>Composer</strong> - groups and sorts media item collections by composer.</li>
 * <li><strong>Genre</strong> - groups and sorts media item collections by musical or film genre.</li>
 * <li><strong>PlayList</strong> - groups and sorts media item collections by playlist.</li>
 * <li><strong>PodcastTitle</strong> - groups and sorts media item collections by podcast title.</li>
 * </ul>
 * The following code example creates an instance of the MediaQuery class to get a list of all items in the
 * user's iPod library: 
 * <pre>
 * final MediaQuery mQuery = new MediaQuery();
 * final List&lt;MediaItem&gt; list = mQuery.getItems();
 * </pre>
 * The following example creates a MediaQuery instance, adds two filters to the query and retrieves the items.
 * The filters will make sure that we get a list of all musical items of the artist "TheBigLooser"
 * <pre>
 * final MediaQuery mQuery = new MediaQuery();
 * 
 * final MediaFilter artistFilter = new MediaFilter(MediaFilter.MediaFilterType.Artist, "TheBigLooser");
 * mQuery.addFilter(artistFilter);
 * 
 * final MediaFilter typeFilter = new MediaFilter(MediaFilter.MediaFilterType.MediaType, MediaItem.MediaItemType.Music);
 * mQuery.addFilter(typeFilter);
 * 
 * final List&lt;MediaItem&gt; list = mQuery.getItems();
 * </pre>
 * If you add the following line, your results will be sorted and grouped by the album on which they appeared.
 * <pre>
 * mQuery.setGrouping(MediaQuery.MediaGroupingType.Album);
 * </pre>
 * However, remember that when you employ grouping, you are supposed to receive media item collections in a form
 * of a List of Lists (List&lt;List&lt;MediaItem&gt;&gt;) of media items.
 * <p>
 * The following example demonstrates how to use both filtering, grouping and how to display each media item's 
 * properties:
 * <pre>
 * final MediaQuery mediaQuery = new MediaQuery();
 * final MediaFilter filter = new MediaFilter(MediaFilter.MediaFilterType.Artist, "RingtoneFeeder.com");
 * mediaQuery.addFilter(filter);
 * mediaQuery.setGroupingType(MediaQuery.MediaGroupingType.AlbumArtist);
 * final List&lt;List&lt;MediaItem&gt;&gt; collections = mediaQuery.getCollections();
 * for (final List&lt;MediaItem&gt; collection : collections) {
 *     for (final MediaItem item : collection) {
 *         System.out.println("      media type :        " + item.getMediaType());
 *         System.out.println("      title:              " + item.getTitle());
 *         System.out.println("      album title:        " + item.getAlbumTitle());
 *         System.out.println("      artist:             " + item.getArtist());
 *         System.out.println("      album artist:       " + item.getAlbumArtist());
 *         System.out.println("      genre:              " + item.getGenre());
 *         System.out.println("      composer:           " + item.getComposer());
 *         System.out.println("      playback duration:  " + item.getPlaybackDuration());
 *         System.out.println("      album track number: " + item.getAlbumTrackNumber());
 *         System.out.println("      album track count:  " + item.getAlbumTrackCount());
 *         System.out.println("      disc number:        " + item.getDiscNumber());
 *         System.out.println("      disc count:         " + item.getDiscCount());
 *         System.out.println("      lyrics:             " + item.getLyrics());
 *         System.out.println("      is compilation ?    " + item.isCompilation());
 *
 *         final SimpleDateFormat df = new SimpleDateFormat();
 *         df.applyPattern("dd/MM/yyyy");
 *         final Calendar date = item.getReleaseDate();
 *
 *         System.out.println("      release date:       " + df.format(date.getTime()));
 *         System.out.println("      beats per minute:   " + item.getBeatsPerMinute());
 *         System.out.println("      comments:           " + item.getComments());
 *         System.out.println("      url:                " + item.getURL());
 *     }
 * }
 * </pre>
 *
 */
public class MediaQuery {

    /**
     * The MediaGroupingType enum defines valid values for grouping of a MediaQuery. See the 
     * MediaQuery description for an explanation of the grouping functionality.
     *
     */
    public static enum MediaGroupingType {
        /**
         * Groups and sorts media item collections by title.
         */
        Title(0),
        /**
         * Groups and sorts media item collections by album, and sorts songs within an album by track order.
         */
        Album(1),
        /**
         * Groups and sorts media item collections by performing artist.
         */
        Artist(2),
        /**
         * Groups and sorts media item collections by album artist (the primary performing artist for an album as a whole).
         */
        AlbumArtist(3),
        /**
         * Groups and sorts media item collections by composer.
         */
        Composer(4),
        /**
         * Groups and sorts media item collections by musical or film genre.
         */
        Genre(5),
        /**
         * Groups and sorts media item collections by playlist.
         */
        PlayList(6),
        /**
         * Groups and sorts media item collections by podcast title.
         */
        PodcastTitle(7);
        
        private final int value;
        
        private MediaGroupingType(int value) {
            this.value = value;
        }
        
        /**
         * Returns the integer value associated with this enum value.
         * @return the integer value associated with this enum value.
         */
        public int getValue() {
            return value;
        }
    }
    
    private List<MediaFilter> filters;
    private MediaGroupingType groupingType;
    
    private List<MediaItem> items;
    private List<List<MediaItem>> collections;
    
    /* Native methods */
    private native void nCreateQuery();
    private native void nAddNumberPredicate(int key, int value);
    private native void nAddStringPredicate(int key, String value);
    private native void nSetGroupingType(int type);
    private native void nFillItemList();
    private native void nFillCollections();
    private native void nDisposeQuery();
    
    /* Methods called from native to fill up the lists */
    private void addMediaItem(final MediaItem item) {
        items.add(item);
    }
    
    private void addCollection(final List<MediaItem> collection) {
        collections.add(collection);
    }
    
    /**
     * Instantiates a new MediaQuery with no filtering and no grouping. Calling <code>getItems()</code>
     * right after this constructor would result in all media items from the iPod library being returned.
     */
    public MediaQuery() {
        filters = new ArrayList<MediaFilter>();
        items = new LinkedList<MediaItem>();
        collections = new LinkedList<List<MediaItem>>();
    }
    
    /**
     * Constructs a new MediaQuery instance and adds the given MediaFilter to the query.
     * @param filter An initial MediaFilter filter
     */
    public MediaQuery(final MediaFilter filter) {
        this();
        addFilter(filter);
    }
    
    /**
     * Adds another MediaFilter to this query.
     * @param filter the MediaFilter to be added to this query
     */
    public void addFilter(final MediaFilter filter) {
        filters.add(filter);
    }

    /**
     * Removes the specified filter from this query.
     * @param filter the MediaFilter to be removed from this query
     */
    public void removeFilter(final MediaFilter filter) {
        filters.remove(filter);
    }
    
    private void setupFilters() {
        for (final MediaFilter filter : filters) {
            final MediaFilterType type = filter.getFilterType(); 
            if (type == MediaFilterType.MediaType) {
                final MediaItem.MediaItemType mediaType = (MediaItem.MediaItemType) filter.getFilterValue();
                nAddNumberPredicate(type.getValue(), mediaType.getValue());
            }
            else if (type == MediaFilterType.IsCompilation) {
                final boolean isCompilation = (Boolean) filter.getFilterValue();
                nAddNumberPredicate(type.getValue(), isCompilation ? 1 : 0);
            }
            else {
                nAddStringPredicate(type.getValue(), (String) filter.getFilterValue());
            }
        }
    }

    /**
     * Returns a list of media items from the iPod library that fulfill all the criteria imposed
     * by all the filters that were added to this query prior to this call. This method neglects
     * grouping. In order to employ grouping, use the <code>getCollections()</code> method instead.
     * @return a list of media items being a result of this query evaluation
     */
    public List<MediaItem> getItems() {
        
        nCreateQuery();
        setupFilters();
        
        if (!items.isEmpty()) {
            items.clear();
        }
        
        nFillItemList();
        nDisposeQuery();
        
        return items;
    }

    /**
     * Sets up grouping. For the list of available grouping types, see the MediaGroupingType enum.
     * @param groupingType the grouping type to use for creating media items collections
     */
    public void setGroupingType(final MediaGroupingType groupingType) {
        this.groupingType = groupingType;
    }

    /**
     * Returns a list of media items from the iPod library that fulfill all the criteria imposed
     * by all the filters that were added to this query prior to this call, arranged and grouped
     * by the grouping type set by calling the <code>setGroupingType</code> method. Collections are
     * returned in a form of lists of media items. See the <code>MediaQuery</code> class description 
     * for an example on how to use grouping.
     * @return
     */
    public List<List<MediaItem>> getCollections() {
        
        nCreateQuery();
        setupFilters();
        
        if (!collections.isEmpty()) {
            for (final List<MediaItem> collection : collections) {
                collection.clear();
            }
            collections.clear();
        }
        
        nSetGroupingType(groupingType.getValue());
        nFillCollections();
        nDisposeQuery();
        
        return collections;
    }

}