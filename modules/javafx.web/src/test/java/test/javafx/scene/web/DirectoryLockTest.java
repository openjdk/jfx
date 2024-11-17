/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import static java.lang.String.format;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import javafx.scene.web.DirectoryLockShim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DirectoryLockTest {

    private static final File FOO = new File("foo");
    private static final File BAR = new File("bar");
    private static final File PRE_LOCKED = new File("baz");
    private static final File[] DIRS = new File[] {FOO, BAR, PRE_LOCKED};


    private static RandomAccessFile preLockedRaf;
    private static FileLock preLockedLock;


    private final ArrayList<DirectoryLockShim> createdLocks = new ArrayList<>();


    @BeforeAll
    public static void beforeClass() throws IOException {
        for (File dir : DIRS) {
            dir.mkdirs();
        }
        File preLockedFile = new File(PRE_LOCKED, ".lock");
        preLockedRaf = new RandomAccessFile(preLockedFile, "rw");
        preLockedLock = preLockedRaf.getChannel().tryLock();
        if (preLockedLock == null) {
            fail(format("Directory [%s] is already locked "
                    + "externally", PRE_LOCKED));
        }
    }

    @AfterAll
    public static void afterClass() throws IOException {
        preLockedLock.release();
        preLockedRaf.close();
        for (File dir : DIRS) {
            deleteRecursively(dir);
        }
    }


    @AfterEach
    public void after() {
        for (DirectoryLockShim lock : createdLocks) {
            lock.close();
        }
    }


    @Test
    public void testConstructor() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
    }

    @Test
    public void testClose() throws Exception {
        DirectoryLockShim lock = createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLockShim.referenceCount(FOO));
        assertNotLocked(FOO);
    }

    @Test
    public void testLockDirectoryTwice() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        createLock(FOO);
        assertEquals(2, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
    }

    @Test
    public void testUnlockDirectoryTwice() throws Exception {
        DirectoryLockShim lock1 = createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        DirectoryLockShim lock2 = createLock(FOO);
        assertEquals(2, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        lock1.close();
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        lock2.close();
        assertEquals(0, DirectoryLockShim.referenceCount(FOO));
        assertNotLocked(FOO);
    }

    @Test
    public void testLockTwoDirectoriesTwice() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        createLock(BAR);
        assertEquals(1, DirectoryLockShim.referenceCount(BAR));
        assertLocked(BAR);
        createLock(FOO);
        assertEquals(2, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        createLock(BAR);
        assertEquals(2, DirectoryLockShim.referenceCount(BAR));
        assertLocked(BAR);
    }

    @Test
    public void testUnlockTwoDirectoriesTwice() throws Exception {
        DirectoryLockShim lock1 = createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        DirectoryLockShim lock2 = createLock(BAR);
        assertEquals(1, DirectoryLockShim.referenceCount(BAR));
        assertLocked(BAR);
        DirectoryLockShim lock3 = createLock(FOO);
        assertEquals(2, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        DirectoryLockShim lock4 = createLock(BAR);
        assertEquals(2, DirectoryLockShim.referenceCount(BAR));
        assertLocked(BAR);
        lock1.close();
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        lock2.close();
        assertEquals(1, DirectoryLockShim.referenceCount(BAR));
        assertLocked(BAR);
        lock3.close();
        assertEquals(0, DirectoryLockShim.referenceCount(FOO));
        assertNotLocked(FOO);
        lock4.close();
        assertEquals(0, DirectoryLockShim.referenceCount(BAR));
        assertNotLocked(BAR);
    }

    @Test
    public void testConstructorNullPointerException() throws Exception {
        try {
            createLock(null);
            fail("NullPointerException expected but not thrown");
        } catch (NullPointerException expected) {}
    }

    @Test
    public void testCanonicalization() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        createLock(new File(FOO.getPath() + File.separatorChar));
        assertEquals(2, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        createLock(FOO.getCanonicalFile());
        assertEquals(3, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
    }

    public void testLockLockedDirectory() throws IOException {
        try {
            createLock(PRE_LOCKED);
            fail("DirectoryAlreadyInUseException expected but not thrown");
        } catch (DirectoryLockShim.DirectoryAlreadyInUseException expected) {}
    }

    @Test
    public void testCloseClosedLock() throws Exception {
        DirectoryLockShim lock = createLock(FOO);
        assertEquals(1, DirectoryLockShim.referenceCount(FOO));
        assertLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLockShim.referenceCount(FOO));
        assertNotLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLockShim.referenceCount(FOO));
        assertNotLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLockShim.referenceCount(FOO));
        assertNotLocked(FOO);
    }


    private DirectoryLockShim createLock(File directory)
            throws IOException, DirectoryLockShim.DirectoryAlreadyInUseException
    {
        DirectoryLockShim lock = new DirectoryLockShim(directory);
        createdLocks.add(lock);
        return lock;
    }

    private static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteRecursively(f);
            }
        }
        if (!file.delete()) {
            throw new IOException(String.format("Error deleting [%s]", file));
        }
    }

    private void assertLocked(File directory) throws IOException {
        File file = new File(directory, ".lock");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileLock fileLock = null;
        try {
            fileLock = raf.getChannel().tryLock();
            if (fileLock == null) {
                fail(format("Directory [%s] is locked externally", directory));
            } else {
                fail(format("Directory [%s] is not locked", directory));
            }
        } catch (OverlappingFileLockException expected) {
        } finally {
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException ignore) {}
            }
            try {
                raf.close();
            } catch (IOException ignore) {}
        }
    }

    private void assertNotLocked(File directory) throws IOException {
        File file = new File(directory, ".lock");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileLock fileLock = null;
        try {
            fileLock = raf.getChannel().tryLock();
            if (fileLock == null) {
                fail(format("Directory [%s] is locked externally", directory));
            }
        } catch (OverlappingFileLockException ex) {
            fail(format("Directory [%s] is locked", directory));
        } finally {
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException ignore) {}
            }
            try {
                raf.close();
            } catch (IOException ignore) {}
        }
    }
}
