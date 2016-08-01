/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Class of exceptions which might be thrown while processing media.
 */
public class MediaException extends RuntimeException {
    // Suppress compilation warnings; 14 <=> JavaFX 1.4.
    private static final long serialVersionUID = 14L;

    private MediaError error = null;

    /**
     * Constructor which merely passes its parameter to the corresponding
     * superclass constructor
     * {@link RuntimeException#RuntimeException(java.lang.String)}.
     *
     * @param message The detail message.
     */
    public MediaException(String message) {
        super(message);
    }

    /**
     * Constructor which merely passes its parameters to the corresponding
     * superclass constructor
     * {@link RuntimeException#RuntimeException(java.lang.String, java.lang.Throwable)}.
     *
     * @param message The detail message.
     * @param cause The cause.
     */
    public MediaException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor which merely passes its parameters to the corresponding
     * superclass constructor
     * {@link RuntimeException#RuntimeException(java.lang.String, java.lang.Throwable)}.
     *
     * @param message The detail message.
     * @param cause The cause.
     * @param error The media error.
     */
    public MediaException(String message, Throwable cause, MediaError error) {
        super(message, cause);
        this.error = error;
    }

    public MediaError getMediaError() {
        return error;
    }
}
