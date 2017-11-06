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

package com.sun.media.jfxmedia;

import com.sun.media.jfxmedia.events.MetadataListener;
import java.io.IOException;

public interface MetadataParser {
    // MP3
    static final String DURATION_TAG_NAME = "duration";
    static final String IMAGE_TAG_NAME = "image";
    static final String ALBUMARTIST_TAG_NAME = "album artist";
    static final String ALBUM_TAG_NAME = "album";
    static final String ARTIST_TAG_NAME = "artist";
    static final String COMMENT_TAG_NAME = "comment";
    static final String COMPOSER_TAG_NAME = "composer";
    static final String GENRE_TAG_NAME = "genre";
    static final String TITLE_TAG_NAME = "title";
    static final String TRACKNUMBER_TAG_NAME = "track number";
    static final String TRACKCOUNT_TAG_NAME = "track count";
    static final String DISCNUMBER_TAG_NAME = "disc number";
    static final String DISCCOUNT_TAG_NAME = "disc count";
    static final String YEAR_TAG_NAME = "year";
    static final String TEXT_TAG_NAME = "text";

    static final String RAW_METADATA_TAG_NAME = "raw metadata";
    static final String RAW_ID3_METADATA_NAME = "ID3";

    void addListener(MetadataListener listener);

    void removeListener(MetadataListener listener);

    void startParser() throws IOException;

    void stopParser();
}
