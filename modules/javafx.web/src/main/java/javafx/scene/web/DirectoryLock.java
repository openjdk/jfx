/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import com.sun.javafx.logging.PlatformLogger;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import static java.lang.String.format;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;

final class DirectoryLock {

    private static final PlatformLogger logger =
            PlatformLogger.getLogger(DirectoryLock.class.getName());
    private static final Map<File,Descriptor> descriptors = new HashMap<>();


    private Descriptor descriptor;


    DirectoryLock(File directory) throws IOException,
                                         DirectoryAlreadyInUseException
    {
        directory = canonicalize(directory);
        descriptor = descriptors.get(directory);
        if (descriptor == null) {
            File lockFile = lockFile(directory);
            RandomAccessFile lockRaf = new RandomAccessFile(lockFile, "rw");
            try {
                FileLock lock = lockRaf.getChannel().tryLock();
                if (lock == null) {
                    throw new DirectoryAlreadyInUseException(
                            directory.toString(), null);
                }
                descriptor = new Descriptor(directory, lockRaf, lock);
                descriptors.put(directory, descriptor);
            } catch (OverlappingFileLockException ex) {
                throw new DirectoryAlreadyInUseException(
                        directory.toString(), ex);
            } finally {
                if (descriptor == null) { // tryLock failed
                    try {
                        lockRaf.close();
                    } catch (IOException ex) {
                        logger.warning(format("Error closing [%s]",
                                lockFile), ex);
                    }
                }
            }
        }
        descriptor.referenceCount++;
    }


    void close() {
        if (descriptor == null) {
            return;
        }
        descriptor.referenceCount--;
        if (descriptor.referenceCount == 0) {
            try {
                descriptor.lock.release();
            } catch (IOException ex) {
                logger.warning(format("Error releasing lock on [%s]",
                        lockFile(descriptor.directory)), ex);
            }
            try {
                descriptor.lockRaf.close();
            } catch (IOException ex) {
                logger.warning(format("Error closing [%s]",
                        lockFile(descriptor.directory)), ex);
            }
            descriptors.remove(descriptor.directory);
        }
        descriptor = null;
    }


    static int referenceCount(File directory) throws IOException {
        Descriptor d = descriptors.get(canonicalize(directory));
        return d == null ? 0 : d.referenceCount;
    }

    static File canonicalize(File directory) throws IOException {
        String path = directory.getCanonicalPath();
        if (path.length() > 0
                && path.charAt(path.length() - 1) != File.separatorChar)
        {
            path += File.separatorChar;
        }
        return new File(path);
    }

    private static File lockFile(File directory) {
        return new File(directory, ".lock");
    }


    private static class Descriptor {
        private final File directory;
        private final RandomAccessFile lockRaf;
        private final FileLock lock;
        private int referenceCount;

        private Descriptor(File directory,
                           RandomAccessFile lockRaf,
                           FileLock lock)
        {
            this.directory = directory;
            this.lockRaf = lockRaf;
            this.lock = lock;
        }
    }

    final class DirectoryAlreadyInUseException extends Exception {
        DirectoryAlreadyInUseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
