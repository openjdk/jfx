/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.io.RandomAccessFile;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The {@code WKJHostFileSystem} group: the ten upcalls of {@code wtf/java/FileSystemJava.cpp}
 * against {@link FileSystem}. All ten are filled - every one of them has a Java target that already
 * existed and did exactly this job for the JNI bindings.
 * <p>
 * The two array shaped returns lost their Java arrays on the way across.
 * {@code get_file_metadata} filled a {@code long[3]} the C++ allocated with {@code NewLongArray};
 * it now fills three {@code int64_t} the caller provided, in the same order.
 * {@code read_from_file} was handed a direct {@code ByteBuffer} over caller-owned memory, and still
 * is - the segment is wrapped rather than copied, exactly as {@code NewDirectByteBuffer} did, and
 * nothing retains it past the call.
 * <p>
 * {@code seek_file} returns nothing on purpose: the caller reports failure by asking
 * {@code core.check_and_clear_exception} straight afterwards, which is where it consulted the JNI
 * exception state and turned a pending one into -1. Routing this class's {@code catch} clauses
 * through {@link WebKitNative#upcallFailed} is what keeps that answer accurate.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 *
 * @see com.sun.webkit.FileSystem
 */
final class FileSystemUpcalls {

    private FileSystemUpcalls() {
    }

    /**
     * Fills the {@code filesystem} group of a {@code WKJHost} table under construction.
     *
     * @param host the table
     */
    static void install(MemorySegment host) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        WebKitNative.installHostSlot(host, "filesystem.file_exists", lookup, "fileExists",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "filesystem.get_file_size", lookup, "getFileSize",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "filesystem.get_file_metadata", lookup,
                "getFileMetadata", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        WebKitNative.installHostSlot(host, "filesystem.path_by_appending_component", lookup,
                "pathByAppendingComponent", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT,
                        ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        WebKitNative.installHostSlot(host, "filesystem.make_all_directories", lookup,
                "makeAllDirectories", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "filesystem.open_file", lookup, "openFile",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "filesystem.close_file", lookup, "closeFile",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "filesystem.read_from_file", lookup, "readFromFile",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "filesystem.path_get_file_name", lookup,
                "pathGetFileName",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        WebKitNative.installHostSlot(host, "filesystem.seek_file", lookup, "seekFile",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
    }

    /* fwkFileExists(String) -> boolean. Default when NULL: 0. */
    private static int fileExists(MemorySegment path, int pathLength) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            return name != null && FileSystem.fwkFileExists(name) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.file_exists", t);
            return 0;
        }
    }

    /*
     * fwkGetFileSize(String) -> long. A negative result means "no size available" and is what the
     * caller tests; it is not an error code to be normalised. Default when NULL: -1.
     */
    private static long getFileSize(MemorySegment path, int pathLength) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            return name == null ? -1L : FileSystem.fwkGetFileSize(name);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.get_file_size", t);
            return -1L;
        }
    }

    /*
     * fwkGetFileMetadata(String, long[3]) -> boolean. On 1 the three slots hold the modification
     * time in milliseconds, the length in bytes and the FileMetadata::Type, in that order - the
     * same three the long[] carried. Default when NULL: 0.
     */
    private static int getFileMetadata(MemorySegment path, int pathLength, MemorySegment out) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            if (name == null) {
                return 0;
            }
            long[] metadata = new long[3];
            if (!FileSystem.fwkGetFileMetadata(name, metadata)) {
                return 0;
            }
            return WebKitNative.writeLongs(out, metadata, 3) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.get_file_metadata", t);
            return 0;
        }
    }

    /* fwkPathByAppendingComponent(String, String) -> String. Default when NULL: WKJ_STR_NULL. */
    private static int pathByAppendingComponent(MemorySegment path, int pathLength,
                                                MemorySegment component, int componentLength,
                                                MemorySegment out, int capacity,
                                                MemorySegment length) {
        try {
            String base = WebKitNative.readString(path, pathLength);
            String leaf = WebKitNative.readString(component, componentLength);
            String joined = base == null || leaf == null
                    ? null
                    : FileSystem.fwkPathByAppendingComponent(base, leaf);
            return WebKitNative.emitString(joined, out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.path_by_appending_component", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /* fwkMakeAllDirectories(String) -> boolean. Default when NULL: 0. */
    private static int makeAllDirectories(MemorySegment path, int pathLength) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            return name != null && FileSystem.fwkMakeAllDirectories(name) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.make_all_directories", t);
            return 0;
        }
    }

    /*
     * fwkOpenFile(String path, String mode) -> RandomAccessFile. The returned id IS WebCore's
     * PlatformFileHandle for this port, so 0 is invalidPlatformFileHandle. Default when NULL: 0.
     */
    private static long openFile(MemorySegment path, int pathLength, MemorySegment mode,
                                 int modeLength) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            String how = WebKitNative.readString(mode, modeLength);
            if (name == null || how == null) {
                return 0L;
            }
            return WebKitNative.register(FileSystem.fwkOpenFile(name, how));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.open_file", t);
            return 0L;
        }
    }

    /* fwkCloseFile(RandomAccessFile). Default when NULL: no-op. */
    private static void closeFile(long file) {
        try {
            if (WebKitNative.lookup(file) instanceof RandomAccessFile target) {
                FileSystem.fwkCloseFile(target);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.close_file", t);
        }
    }

    /*
     * fwkReadFromFile(RandomAccessFile, ByteBuffer) -> int. The bytes belong to the caller and are
     * valid for the duration of the call; the segment is wrapped without copying, as
     * NewDirectByteBuffer did. A negative result is end of stream or an error, which the caller
     * normalises to -1 as it always did. Default when NULL: -1.
     */
    private static int readFromFile(long file, MemorySegment data, int length) {
        try {
            if (!(WebKitNative.lookup(file) instanceof RandomAccessFile target)
                    || data.address() == 0L || length <= 0) {
                return -1;
            }
            return FileSystem.fwkReadFromFile(target,
                    WebKitNative.resize(data, length).asByteBuffer());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.read_from_file", t);
            return -1;
        }
    }

    /* fwkPathGetFileName(String) -> String. Default when NULL: WKJ_STR_NULL. */
    private static int pathGetFileName(MemorySegment path, int pathLength, MemorySegment out,
                                       int capacity, MemorySegment length) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            return WebKitNative.emitString(name == null ? null : FileSystem.fwkPathGetFileName(name),
                    out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.path_get_file_name", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /*
     * fwkSeekFile(RandomAccessFile, long). It returns nothing: the caller asks
     * core.check_and_clear_exception straight afterwards, which is where it turned a pending JNI
     * exception into -1. Default when NULL: no-op.
     */
    private static void seekFile(long file, long offset) {
        try {
            if (WebKitNative.lookup(file) instanceof RandomAccessFile target) {
                FileSystem.fwkSeekFile(target, offset);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("filesystem.seek_file", t);
        }
    }
}
