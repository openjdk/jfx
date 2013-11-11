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

/**
 * The MediaFilter class represents a filter that can be used to filter out media items
 * from the iPod library. For instance, one can filter out media items of a specific type,
 * genre, album title, composer, etc.
 * <br/>
 * A MediaFilter is defined by its type a and value.
 * <br/>
 * Valid MediaFilter types are as follows:
 * <ul>
 * <li><strong>MediaType</strong> - specifies the type of media items, e.g. podcasts, videos, etc. For a list
 * of valid media types, see <code>MediaItem.MediaItemType</code> enum values</li>
 * <li><strong>Title</strong> - the title (or name) of the media item</li>
 * <li><strong>AlbumTitle</strong> - the title of an album, as opposed to the title of an individual song on the
 * album</li>
 * <li><strong>Artist</strong> - the performing artist(s) for a media item, which may vary from the primary artist
 * for the album that a media item belongs to</li>
 * <li><strong>AlbumArtist</strong> - the primary performing artist for an album as a whole</li>
 * <li><strong>Genre</strong> - the musical or film genre of the media item</li>
 * <li><strong>Composer</strong> - the musical composer for the media item</li>
 * <li><strong>IsCompilation</strong> - indicates whether the media item is part of a compilation or not. Corresponds to 
 * the &ldquo;Part of a compilation&rdquo; checkbox in the Info tab in the Get Info dialog in iTunes</li>
 * </ul>
 * 
 * Filter values are of the type <code>String</code>, except for the <code>IsCompilation</code> filter type, the value of which 
 * is a <code>Boolean</code> and except for the <code>MediaType</code> type, the value of which is of the type <code>enum MediaItemType</code>.
 * <br/>
 * To add a filter to a <code>MediaQuery</code> instance, use <code>MediaQuery.addFilter()</code>.
 * <br/>
 * To remove a filter from a <code>MediaQuery</code> instance, use <code>MediaQuery.removeFilter()</code>.
 */
public class MediaFilter {

    /**
     * This enum defines valid values for the MediaFilter's type.
     */
    public static enum MediaFilterType {
        /**
         * Specifies the type of media items, for example podcasts, videos, etc. For a list of valid media types, 
         * see <code>enum MediaItem.MediaItemType</code> values.
         */
        MediaType(0),
        /**
         * The title (or name) of the media item.
         */
        Title(1),
        /**
         * The title of an album, as opposed to the title of an individual song on the album.
         */
        AlbumTitle(2),
        /**
         * The performing artist(s) for a media item, which may vary from the primary artist for the album that a media item belongs to.
         */
        Artist(3),
        /**
         * The primary performing artist for an album as a whole.
         */
        AlbumArtist(4),
        /**
         * The musical or film genre of the media item.
         */
        Genre(5),
        /**
         * The musical composer for the media item.
         */
        Composer(6),
        /**
         * Indicates whether the media item is part of a compilation or not. Corresponds to the "Part of a compilation" 
         * checkbox in the Info tab in the Get Info dialog in iTunes.
         */
        IsCompilation(7);
        
        private final int value;

        private MediaFilterType(int value) {
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
    
    private MediaFilterType filterType;
    private Object filterValue;

    /**
     * Constructs an empty media filter that excludes no media items. Using this filter is equivalent to
     * using no filters at all. It is possible to change the filter's type and value later on by calling the
     * appropriate setter methods.
     */
    public MediaFilter() {
        this(MediaFilterType.MediaType, MediaItem.MediaItemType.Any);
    }

    /**
     * Constructs a new media filter of the specified type and value.
     * 
     * @param filterType the filter's type
     * @param filterValue the filter's value
     */
    public MediaFilter(final MediaFilterType filterType, final Object filterValue) {
        this.filterType = filterType;
        this.filterValue = filterValue;
    }

    /**
     * Returns this filter's type.
     * 
     * @return this filter's type
     */
    public MediaFilterType getFilterType() {
        return filterType;
    }

    /**
     * Returns this filter's value
     * 
     * @return this filter's value
     */
    public Object getFilterValue() {
        return filterValue;
    }
    
    /**
     * Sets this filter's type
     * 
     * @param filterType the new type of this filter
     */
    public void setFilterType(final MediaFilterType filterType) {
        this.filterType = filterType;
    }
    
    /**
     * Sets this filter's value
     * 
     * @param filterValue the new value of this filter
     */
    public void setFilterValue(final Object filterValue) {
        this.filterValue = filterValue;
    }
}
