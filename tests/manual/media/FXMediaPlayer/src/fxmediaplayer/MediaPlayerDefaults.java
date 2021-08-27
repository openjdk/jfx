/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package fxmediaplayer;

import java.io.File;

public class MediaPlayerDefaults {

    private static final String SETTINGS_FOLDER
            = System.getProperty("user.home") + File.separator + ".FXMediaPlayer";
    public static final String PLAYLIST_FILE
            = SETTINGS_FOLDER + File.separator + "playlist.xml";

    static {
        new File(SETTINGS_FOLDER).mkdirs();
    }
    private static final String[] PLAYLIST_DEFAULT = {
        "https://download.oracle.com/otndocs/products/javafx/oow2010-2.mp4",
        "https://download.oracle.com/otndocs/javafx/"
        + "JavaRap_ProRes_H264_768kbit_Widescreen.mp4"
    };
    public static final String[] PLAYLIST = PLAYLIST_DEFAULT;
    public static final String DEFAULT_SOURCE = PLAYLIST_DEFAULT[0];
}
