/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.ipack.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Similar to HashingOutputStream, but calculates and stores a hash for each
 * page of the input stream.
 */
public final class PageHashingOutputStream extends FilterOutputStream {
    private final MessageDigest messageDigest;
    private final List<byte[]> pageHashes;
    private final int pageSize;

    private int pageRemaining;

    public PageHashingOutputStream(final OutputStream out) {
        this(out, 4096);
    }

    public PageHashingOutputStream(final OutputStream out, final int pageSize) {
        super(out);
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can't create message digest", e);
        }
        pageHashes = new ArrayList<byte[]>();
        this.pageSize = pageSize;
        this.pageRemaining = pageSize;
    }

    public List<byte[]> getPageHashes() {
        return Collections.unmodifiableList(pageHashes);
    }

    @Override
    public void write(final int byteValue) throws IOException {
        out.write(byteValue);
        messageDigest.update((byte) byteValue);
        --pageRemaining;
        if (pageRemaining == 0) {
            commitPageHashImpl();
        }
    }

    @Override
    public void write(final byte[] buffer, final int offset, final int length)
            throws IOException {
        out.write(buffer, offset, length);

        int hashRemaining = length;
        int hashOffset = offset;
        while (hashRemaining > 0) {
            final int chunkLength =
                    (pageRemaining < hashRemaining) ? pageRemaining
                                                    : hashRemaining;

            messageDigest.update(buffer, hashOffset, chunkLength);

            hashOffset += chunkLength;
            hashRemaining -= chunkLength;
            pageRemaining -= chunkLength;

            if (pageRemaining == 0) {
                commitPageHashImpl();
            }
        }
    }

    /**
     * Creates a new page hash from the remaining data.
     *
     * If there are some data which haven't been included in the previous page
     * hash, this methods adds a new page hash for them even if they don't
     * fill up a complete page.
     */
    public void commitPageHash() {
        if (pageRemaining != pageSize) {
            // we do have some data for hashing
            commitPageHashImpl();
        }
    }

    private void commitPageHashImpl() {
        pageHashes.add(messageDigest.digest());
        pageRemaining = pageSize;
    }
}
