/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import com.sun.webkit.Disposer;
import com.sun.webkit.DisposerRecord;
import org.w3c.dom.DOMException;
import org.w3c.dom.stylesheets.MediaList;

public class MediaListImpl implements MediaList {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            MediaListImpl.dispose(peer);
        }
    }

    MediaListImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static MediaList create(long peer) {
        if (peer == 0L) return null;
        return new MediaListImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof MediaListImpl) && (peer == ((MediaListImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(MediaList arg) {
        return (arg == null) ? 0L : ((MediaListImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static MediaList getImpl(long peer) {
        return (MediaList)create(peer);
    }


// Attributes
    public String getMediaText() {
        return getMediaTextImpl(getPeer());
    }
    native static String getMediaTextImpl(long peer);

    public void setMediaText(String value) throws DOMException {
        setMediaTextImpl(getPeer(), value);
    }
    native static void setMediaTextImpl(long peer, String value);

    public int getLength() {
        return getLengthImpl(getPeer());
    }
    native static int getLengthImpl(long peer);


// Functions
    public String item(int index)
    {
        return itemImpl(getPeer()
            , index);
    }
    native static String itemImpl(long peer
        , int index);


    public void deleteMedium(String oldMedium) throws DOMException
    {
        deleteMediumImpl(getPeer()
            , oldMedium);
    }
    native static void deleteMediumImpl(long peer
        , String oldMedium);


    public void appendMedium(String newMedium) throws DOMException
    {
        appendMediumImpl(getPeer()
            , newMedium);
    }
    native static void appendMediumImpl(long peer
        , String newMedium);


}

