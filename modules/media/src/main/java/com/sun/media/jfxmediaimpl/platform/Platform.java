/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl.platform;

import com.sun.media.jfxmedia.Media;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.MetadataParser;
import com.sun.media.jfxmedia.locator.Locator;

/**
 * JFXMedia platform interface.
 */
public abstract class Platform {
    /*
     * Get an instance of the platform.
     */
    public static Platform getPlatformInstance() {
        throw new UnsupportedOperationException("Invalid platform class.");
    }

    /**
     * @return false if the platform cannot be loaded
     */
    public boolean loadPlatform() {
        return false;
    }

    public boolean canPlayContentType(String contentType) {
        String[] contentTypes = getSupportedContentTypes();
        if (contentTypes != null) {
            for (String type : contentTypes) {
                if (type.equalsIgnoreCase(contentType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canPlayProtocol(String protocol) {
        String[] protocols = getSupportedProtocols();
        if (protocols != null) {
            for (String p : protocols) {
                if (p.equalsIgnoreCase(protocol)) {
                    return true;
                }
            }
        }
        return false;
    }

    // NB: This method returns only content which can actually be PLAYED. It
    // does not account for metadata being able to be parsed without the media
    // being able to be played.
    public String[] getSupportedContentTypes() {
        return new String[0];
    }

    public String[] getSupportedProtocols() {
        return new String[0];
    }

    // XXX javadoc
    public MetadataParser createMetadataParser(Locator source) {
        return null;
    }

    public abstract Media createMedia(Locator source);

    /**
     * Prepare for playing the specified media. If the media stream is unsupported
     * return null so other platforms may be used.
     */
    public abstract MediaPlayer createMediaPlayer(Locator source);
}
