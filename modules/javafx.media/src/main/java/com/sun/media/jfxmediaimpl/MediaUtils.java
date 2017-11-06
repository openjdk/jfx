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

package com.sun.media.jfxmediaimpl;

import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.MediaException;
import com.sun.media.jfxmedia.events.MediaErrorListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import java.util.List;
import java.lang.ref.WeakReference;
import java.util.ListIterator;

/**
 * Utility functions.
 */
public class MediaUtils {

    /** Maximum number of bytes needed to scan the file signature. */
    public static final int MAX_FILE_SIGNATURE_LENGTH = 22;

    /**
     * Format of an error which occurred in the native porting layer. A single
     * int argument representing an error code is expected.
     */
    static final String NATIVE_MEDIA_ERROR_FORMAT = "Internal media error: %d";
    /**
     * Format of an error which occurred in the native porting layer. A single
     * int argument representing a warning code is expected.
     *
     * Note: FLV related code in this file is needed so we can provide error
     * message that FLV support is removed.
     */
    static final String NATIVE_MEDIA_WARNING_FORMAT = "Internal media warning: %d";
    public static final String CONTENT_TYPE_AIFF = "audio/x-aiff";
    public static final String CONTENT_TYPE_MP3 = "audio/mp3";
    public static final String CONTENT_TYPE_MPA = "audio/mpeg";
    public static final String CONTENT_TYPE_WAV = "audio/x-wav";
    public static final String CONTENT_TYPE_JFX = "video/x-javafx";
    public static final String CONTENT_TYPE_FLV = "video/x-flv";
    public static final String CONTENT_TYPE_MP4 = "video/mp4";
    public static final String CONTENT_TYPE_M4A = "audio/x-m4a";
    public static final String CONTENT_TYPE_M4V = "video/x-m4v";
    public static final String CONTENT_TYPE_M3U8 = "application/vnd.apple.mpegurl";
    public static final String CONTENT_TYPE_M3U  = "audio/mpegurl";
    private static final String FILE_TYPE_AIF = "aif";
    private static final String FILE_TYPE_AIFF = "aiff";
    private static final String FILE_TYPE_FLV = "flv";
    private static final String FILE_TYPE_FXM = "fxm";
    private static final String FILE_TYPE_MPA = "mp3";
    private static final String FILE_TYPE_WAV = "wav";
    private static final String FILE_TYPE_MP4 = "mp4";
    private static final String FILE_TYPE_M4A = "m4a";
    private static final String FILE_TYPE_M4V = "m4v";
    private static final String FILE_TYPE_M3U8 = "m3u8";
    private static final String FILE_TYPE_M3U  = "m3u";

    /**
     * Attempt to determine the content type from the file signature.
     *
     * @param buf File signature of size <code>MAX_FILE_SIGNATURE_LENGTH</code>
     * @return The content type or {@link Locator#DEFAULT_CONTENT_TYPE} if not
     * able to be determined or unsupported.
     */
    public static String fileSignatureToContentType(byte[] buf, int size) throws MediaException {
        String contentType = Locator.DEFAULT_CONTENT_TYPE;

        if (size < MAX_FILE_SIGNATURE_LENGTH) {
            throw new MediaException("Empty signature!");
        } else if (buf.length < MAX_FILE_SIGNATURE_LENGTH) {
            return contentType;
        } else if ((buf[0] & 0xff) == 0x46
                && (buf[1] & 0xff) == 0x4c
                && (buf[2] & 0xff) == 0x56) { // "FLV"
            contentType = CONTENT_TYPE_JFX;
        } else if ((((buf[0] & 0xff) << 24)
                | ((buf[1] & 0xff) << 16)
                | ((buf[2] & 0xff) << 8)
                | (buf[3] & 0xff)) == 0x52494646 && // "RIFF"
                (((buf[8] & 0xff) << 24)
                | ((buf[9] & 0xff) << 16)
                | ((buf[10] & 0xff) << 8)
                | (buf[11] & 0xff)) == 0x57415645 && // "WAVE
                (((buf[12] & 0xff) << 24)
                | ((buf[13] & 0xff) << 16)
                | ((buf[14] & 0xff) << 8)
                | (buf[15] & 0xff)) == 0x666d7420) { // "fmt"
            if (((buf[20] & 0xff) == 0x01 && (buf[21] & 0xff) == 0x00) || ((buf[20] & 0xff) == 0x03 && (buf[21] & 0xff) == 0x00)) { // PCM or IEEE float
                contentType = CONTENT_TYPE_WAV;
            } else {
                throw new MediaException("Compressed WAVE is not supported!");
            }
        } else if ((((buf[0] & 0xff) << 24)
                | ((buf[1] & 0xff) << 16)
                | ((buf[2] & 0xff) << 8)
                | (buf[3] & 0xff)) == 0x52494646 && // "RIFF"
                (((buf[8] & 0xff) << 24)
                | ((buf[9] & 0xff) << 16)
                | ((buf[10] & 0xff) << 8)
                | (buf[11] & 0xff)) == 0x57415645) // "WAVE
        {
            contentType = CONTENT_TYPE_WAV; // It is WAV for sure, but we cannot detect format, so format detection will be left to native part
        } else if ((((buf[0] & 0xff) << 24)
                | ((buf[1] & 0xff) << 16)
                | ((buf[2] & 0xff) << 8)
                | (buf[3] & 0xff)) == 0x464f524d && // "FORM"
                (((buf[8] & 0xff) << 24)
                | ((buf[9] & 0xff) << 16)
                | ((buf[10] & 0xff) << 8)
                | (buf[11] & 0xff)) == 0x41494646 && // "AIFF
                (((buf[12] & 0xff) << 24)
                | ((buf[13] & 0xff) << 16)
                | ((buf[14] & 0xff) << 8)
                | (buf[15] & 0xff)) == 0x434f4d4d) { // "COMM"
            contentType = CONTENT_TYPE_AIFF;
        } else if ((buf[0] & 0xff) == 0x49
                && (buf[1] & 0xff) == 0x44
                && (buf[2] & 0xff) == 0x33) { // "ID3"
            contentType = CONTENT_TYPE_MPA;
        } else if ((buf[0] & 0xff) == 0xff && (buf[1] & 0xe0) == 0xe0 && // sync
                (buf[2] & 0x18) != 0x08 && // not reserved version
                (buf[3] & 0x06) != 0x00) { // not reserved layer
            contentType = CONTENT_TYPE_MPA;
        } else if ((((buf[4] & 0xff) << 24)
                | ((buf[5] & 0xff) << 16)
                | ((buf[6] & 0xff) << 8)
                | (buf[7] & 0xff)) == 0x66747970) { // "ftyp"
            if ((buf[8] & 0xff) == 0x4D && (buf[9] & 0xff) == 0x34 && (buf[10] & 0xff) == 0x41 && (buf[11] & 0xff) == 0x20) // 'M4A '
                contentType = CONTENT_TYPE_M4A;
            else if ((buf[8] & 0xff) == 0x4D && (buf[9] & 0xff) == 0x34 && (buf[10] & 0xff) == 0x56 && (buf[11] & 0xff) == 0x20) // 'M4V '
                contentType = CONTENT_TYPE_M4V;
            else if ((buf[8] & 0xff) == 0x6D && (buf[9] & 0xff) == 0x70 && (buf[10] & 0xff) == 0x34 && (buf[11] & 0xff) == 0x32) // 'mp42'
                contentType = CONTENT_TYPE_MP4;
            else if ((buf[8] & 0xff) == 0x69 && (buf[9] & 0xff) == 0x73 && (buf[10] & 0xff) == 0x6F && (buf[11] & 0xff) == 0x6D) // 'isom'
                contentType = CONTENT_TYPE_MP4;
            else if ((buf[8] & 0xff) == 0x4D && (buf[9] & 0xff) == 0x50 && (buf[10] & 0xff) == 0x34 && (buf[11] & 0xff) == 0x20) // 'MP4 '
                contentType = CONTENT_TYPE_MP4;
        } else {
            throw new MediaException("Unrecognized file signature!");
        }

        return contentType;
    }

    /**
     * Returns the content type given the file name.
     *
     * @param filename
     * @return content type
     */
    public static String filenameToContentType(String filename) {
        String contentType = Locator.DEFAULT_CONTENT_TYPE;

        int dotIndex = filename.lastIndexOf(".");

        if (dotIndex != -1) {
            String extension = filename.toLowerCase().substring(dotIndex + 1);

            if (extension.equals(FILE_TYPE_AIF) || extension.equals(FILE_TYPE_AIFF)) {
                contentType = CONTENT_TYPE_AIFF;
            } else if (extension.equals(FILE_TYPE_FLV) || extension.equals(FILE_TYPE_FXM)) {
                contentType = CONTENT_TYPE_JFX;
            } else if (extension.equals(FILE_TYPE_MPA)) {
                contentType = CONTENT_TYPE_MPA;
            } else if (extension.equals(FILE_TYPE_WAV)) {
                contentType = CONTENT_TYPE_WAV;
            } else if (extension.equals(FILE_TYPE_MP4)) {
                contentType = CONTENT_TYPE_MP4;
            } else if (extension.equals(FILE_TYPE_M4A)) {
                contentType = CONTENT_TYPE_M4A;
            } else if (extension.equals(FILE_TYPE_M4V)) {
                contentType = CONTENT_TYPE_M4V;
            } else if (extension.equals(FILE_TYPE_M3U8)) {
                contentType = CONTENT_TYPE_M3U8;
            } else if (extension.equals(FILE_TYPE_M3U)) {
                contentType = CONTENT_TYPE_M3U;
            }
        }

        return contentType;
    }

    /**
     * Forward warnings to the global listeners registered with the
     * {@link NativeMediaManager}. This method may be invoked from either the
     * Java or the native layer.
     *
     * @param source the source of the warning, likely the object calling this
     * method.
     * @param message a <code>String</code> containing the warning.
     */
    public static void warning(Object source, String message) {
        // Log the warning.
        if (source != null & message != null) {
            Logger.logMsg(Logger.WARNING,
                    source.getClass().getName() + ": " + message);
        }
    }

    /**
     * Throw a <code>MediaException</code> with the indicated message and cause.
     * This method should be invoked only from the Java layer. The is logged
     * before the exception is thrown.
     *
     * @param message The detail message.
     * @param cause The cause.
     */
    public static void error(Object source, int errCode, String message, Throwable cause) {
        // Log the error.
        if (cause != null) {
            StackTraceElement[] stackTrace = cause.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement trace = stackTrace[0];
                Logger.logMsg(Logger.ERROR,
                        trace.getClassName(), trace.getMethodName(),
                        "( " + trace.getLineNumber() + ") " + message);
            }
        }

        // Forward warning to registered listeners.
        List<WeakReference<MediaErrorListener>> listeners =
                NativeMediaManager.getDefaultInstance().getMediaErrorListeners();
        if (!listeners.isEmpty()) {
            for (ListIterator<WeakReference<MediaErrorListener>> it = listeners.listIterator(); it.hasNext();) {
                MediaErrorListener l = it.next().get();
                if (l != null) {
                    l.onError(source, errCode, message);
                } else {
                    it.remove();
                }
            }
        } else {
            MediaException e = cause instanceof MediaException
                    ? (MediaException) cause : new MediaException(message, cause);
            throw e;
        }
    }

    /**
     * Send a message with the indicated native error code.
     *
     * @param warningCode The native warning code.
     */
    public static void nativeWarning(Object source, int warningCode, String warningMessage) {
        // Create a message per the defined format.
        String message = String.format(NATIVE_MEDIA_WARNING_FORMAT, warningCode);

        if (warningMessage != null) {
            message += ": " + warningMessage;
        }

        // Log the warning.
        Logger.logMsg(Logger.WARNING, message);
    }

    /**
     * Throw a <code>MediaException</code> with the indicated error code.
     * This method should be invoked only from the native layer.
     *
     * @param errorCode The native error code.
     */
    public static void nativeError(Object source, MediaError error) {
        // Log the error.
        Logger.logMsg(Logger.ERROR, error.description());

        // Forward warning to registered listeners.
        List<WeakReference<MediaErrorListener>> listeners =
                NativeMediaManager.getDefaultInstance().getMediaErrorListeners();
        if (!listeners.isEmpty()) {
            for (ListIterator<WeakReference<MediaErrorListener>> it = listeners.listIterator(); it.hasNext();) {
                MediaErrorListener l = it.next().get();
                if (l != null) {
                    l.onError(source, error.code(), error.description());
                } else {
                    it.remove();
                }
            }
        } else {
            throw new MediaException(error.description(), null, error);
        }
    }
}
