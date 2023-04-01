/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.media.jfxmedia.MediaError;

/**
 * A <code>MediaException</code> describes a runtime error condition in a {@link Media},
 * {@link MediaPlayer} or {@link MediaView}.
 *
 * @see Media#onErrorProperty
 * @see MediaView#onErrorProperty
 * @since JavaFX 2.0
 */
public final class MediaException extends RuntimeException {
    /**
     * Enumeration describing categories of errors. A number of different
     * actual {@link Exception}s may belong to the same category.
     * @since JavaFX 2.0
     */
    public enum Type {
        // FIXME: generate more descriptive messages
        /**
         * Indicates an error has occurred: the media appears to be
         * invalid or corrupted.
         */
        MEDIA_CORRUPTED,
        /**
         * Indicates an error has occurred: although the media
         * may exist, it is not accessible.
         */
        MEDIA_INACCESSIBLE,
        /**
         * Indicates an error has occurred: the media
         * does not exist or is otherwise unavailable. This error may
         * be the result of security settings preventing access when
         * running in a browser.
         */
        MEDIA_UNAVAILABLE,
        /**
         * Indicates that the media has not been specified.
         */
        MEDIA_UNSPECIFIED,
        /**
         * Indicates that this media type is not supported by this platform.
         */
        MEDIA_UNSUPPORTED,
        /**
         * Indicates that an operation performed on the media is not
         * supported by this platform.
         */
        OPERATION_UNSUPPORTED,
        /**
         * Indicates a playback error which does not fall into any of the other
         * pre-defined categories.
         */
        PLAYBACK_ERROR,
        /**
         * Indicates an unrecoverable error which has resulted in halting playback.
         */
        PLAYBACK_HALTED,
        /**
         * Indicates an error has occurred for an unknown reason.
         */
        UNKNOWN
    }

    /**
     * Map {@link MediaError} codes to {@link Type}s.
     * @param errorCode Error code from implementation layer.
     * @return API level error type.
     */
    static Type errorCodeToType(int errorCode) {
        Type errorType;

        if(errorCode == MediaError.ERROR_LOCATOR_CONNECTION_LOST.code()) {
            errorType = Type.MEDIA_INACCESSIBLE;
        } else if(errorCode == MediaError.ERROR_GSTREAMER_SOURCEFILE_NONEXISTENT.code() ||
                errorCode == MediaError.ERROR_GSTREAMER_SOURCEFILE_NONREGULAR.code()) {
            errorType = Type.MEDIA_UNAVAILABLE;
        } else if(errorCode == MediaError.ERROR_MEDIA_AUDIO_FORMAT_UNSUPPORTED.code() ||
                errorCode == MediaError.ERROR_MEDIA_UNKNOWN_PIXEL_FORMAT.code() ||
                errorCode == MediaError.ERROR_MEDIA_VIDEO_FORMAT_UNSUPPORTED.code() ||
                errorCode == MediaError.ERROR_LOCATOR_CONTENT_TYPE_NULL.code() ||
                errorCode == MediaError.ERROR_LOCATOR_UNSUPPORTED_MEDIA_FORMAT.code() ||
                errorCode == MediaError.ERROR_LOCATOR_UNSUPPORTED_TYPE.code() ||
                errorCode == MediaError.ERROR_GSTREAMER_UNSUPPORTED_PROTOCOL.code() ||
                errorCode == MediaError.ERROR_MEDIA_MP3_FORMAT_UNSUPPORTED.code() ||
                errorCode == MediaError.ERROR_MEDIA_AAC_FORMAT_UNSUPPORTED.code() ||
                errorCode == MediaError.ERROR_MEDIA_H264_FORMAT_UNSUPPORTED.code() ||
                errorCode == MediaError.ERROR_MEDIA_H265_FORMAT_UNSUPPORTED.code() ||
                errorCode == MediaError.ERROR_MEDIA_HLS_FORMAT_UNSUPPORTED.code()) {
            errorType = Type.MEDIA_UNSUPPORTED;
        } else if(errorCode == MediaError.ERROR_MEDIA_CORRUPTED.code()) {
            errorType = Type.MEDIA_CORRUPTED;
        } else if((errorCode & MediaError.ERROR_BASE_GSTREAMER.code()) == MediaError.ERROR_BASE_GSTREAMER.code() ||
                (errorCode & MediaError.ERROR_BASE_JNI.code()) == MediaError.ERROR_BASE_JNI.code()) {
            errorType = Type.PLAYBACK_ERROR;
        } else {
            errorType = Type.UNKNOWN;
        }

        return errorType;
    }

    /**
     * converts Java exceptions into mediaErrors
     */
    static MediaException exceptionToMediaException(Exception e) {
        Type errType = Type.UNKNOWN;
        // Set appropriate error code based on exception cause.
        if (e.getCause() instanceof java.net.UnknownHostException) {
            errType = Type.MEDIA_UNAVAILABLE;
        } else if (e.getCause() instanceof java.lang.IllegalArgumentException) {
            errType = Type.MEDIA_UNSUPPORTED;
        } else if (e instanceof com.sun.media.jfxmedia.MediaException) {
            com.sun.media.jfxmedia.MediaException me = (com.sun.media.jfxmedia.MediaException)e;
            MediaError error = me.getMediaError();
            if (error != null ) {
                errType = errorCodeToType(error.code());
            }
        }

        return new MediaException(errType, e);
    }

    static MediaException haltException(String message) {
        return new MediaException(Type.PLAYBACK_HALTED, message);
    }

    static MediaException getMediaException(Object source, int errorCode, String message) {
        // Get the error code description.
        String errorDescription = MediaError.getFromCode(errorCode).description();
        String exceptionMessage = "[" + source + "] " + message + ": " + errorDescription;

        // We need to map some jfxMedia error codes to FX Media error codes.
        Type errorType = errorCodeToType(errorCode);
        return new MediaException(errorType, exceptionMessage);
    }

    MediaException(Type type, Throwable t) {
        super(t);
        this.type = type;
    }

    MediaException(Type type, String message, Throwable t) {
        super(message, t);
        this.type = type;
    }

    MediaException(Type type, String message) {
        super(message);
        this.type = type;
    }
    /**
     * What caused this error.
     */
    private final Type type;
    /**
     * Retrieves the category into which this error falls.
     * @return The type of this error
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns a string representation of this <code>MediaException</code> object.
     * @return a string representation of this <code>MediaException</code> object.
     */
    @Override
    public String toString() {
        String errString = "MediaException: " + type;
        if (getMessage() != null) errString += " : " + getMessage();
        if (getCause() != null) errString += " : " + getCause();
        return errString;
    }
}
