/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio;

import java.util.Arrays;
import java.util.List;

/**
 * A description of image format attributes.
 */
public interface ImageFormatDescription {
    /**
     * Get the name of the format, for example "JPEG," "PNG," and so on,
     * preferably in upper case.
     *
     * @return the format name.
     */
    String getFormatName();

    /**
     * Get the extension(s) used for a file stored in this format, preferably in
     * lower case.
     *
     * @return the file extension(s) for this format.
     */
    List<String> getExtensions();

    /**
     * Get the possible signatures which may appear at the beginning of
     * the stream of an image stored in this format.
     *
     * @return the signatures of an image stream in this format.
     */
    List<Signature> getSignatures();

    /**
     * Get the MIME subtype(s) of the "image" type corresponding to this format,
     * for example, "jpeg" "png," etc.
     *
     * @return the MIME type(s) of this format.
     */
    List<String> getMIMESubtypes();

    /**
     * Represents a sequences of bytes which can appear at the beginning of
     * the stream of an image stored in this format.
     */
    public final class Signature {
        private final byte[] bytes;

        public Signature(final byte... bytes) {
            this.bytes = bytes;
        }

        public int getLength() {
            return bytes.length;
        }

        public boolean matches(final byte[] streamBytes) {
            if (streamBytes.length < bytes.length) {
                return false;
            }

            for (int i = 0; i < bytes.length; i++) {
                if (streamBytes[i] != bytes[i]) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof Signature)) {
                return false;
            }

            return Arrays.equals(bytes, ((Signature) other).bytes);
        }
    }
}
