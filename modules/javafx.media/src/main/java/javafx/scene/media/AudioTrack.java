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

package javafx.scene.media;

import java.util.Locale;
import java.util.Map;

/**
 * A {@link Track} that describes an audio track. An audio track may for example
 * be the unique track in a piece of digital music or one of several tracks in
 * an audiovisual media resource.
 * @since JavaFX 2.0
 */
public final class AudioTrack extends Track {
    /**
     * Retrieves the language of the audio track. The IANA language code might
     * be for example "en" for English or "ru" for Russian. The list of all
     * registered IANA language codes is available in the
     * <a href="http://www.iana.org/assignments/language-subtag-registry">
     * IANA Language Subtag Registry</a>.
     * @return the IANA language code or <code>null</code>.
     * @deprecated Use {@link Track#getLocale()} instead.
     */
    @Deprecated
    public final String getLanguage() {
        Locale l = getLocale();
        return (null == l) ? null : l.getLanguage();
    }

    AudioTrack(long trackID, Map<String,Object> metadata) {
        super(trackID, metadata);
    }
}
