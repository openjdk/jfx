/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import static java.lang.String.format;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

public class DirectoryLockTest {

    private static final File FOO = new File("foo");
    private static final File BAR = new File("bar");
    private static final File PRE_LOCKED = new File("baz");
    private static final File[] DIRS = new File[] {FOO, BAR, PRE_LOCKED};


    private static RandomAccessFile preLockedRaf;
    private static FileLock preLockedLock;


    private final ArrayList<DirectoryLock> createdLocks = new ArrayList<>();


    @BeforeClass
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

    @AfterClass
    public static void afterClass() throws IOException {
        preLockedLock.release();
        preLockedRaf.close();
        for (File dir : DIRS) {
            deleteRecursively(dir);
        }
    }


    @After
    public void after() {
        for (DirectoryLock lock : createdLocks) {
            lock.close();
        }
    }


    @Test
    public void testConstructor() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
    }

    @Test
    public void testClose() throws Exception {
        DirectoryLock lock = createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLock.referenceCount(FOO));
        assertNotLocked(FOO);
    }

    @Test
    public void testLockDirectoryTwice() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        createLock(FOO);
        assertEquals(2, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
    }

    @Test
    public void testUnlockDirectoryTwice() throws Exception {
        DirectoryLock lock1 = createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        DirectoryLock lock2 = createLock(FOO);
        assertEquals(2, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        lock1.close();
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        lock2.close();
        assertEquals(0, DirectoryLock.referenceCount(FOO));
        assertNotLocked(FOO);
    }

    @Test
    public void testLockTwoDirectoriesTwice() throws Exception {
        createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        createLock(BAR);
        assertEquals(1, DirectoryLock.referenceCount(BAR));
        assertLocked(BAR);
        createLock(FOO);
        assertEquals(2, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        createLock(BAR);
        assertEquals(2, DirectoryLock.referenceCount(BAR));
        assertLocked(BAR);
    }

    @Test
    public void testUnlockTwoDirectoriesTwice() throws Exception {
        DirectoryLock lock1 = createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        DirectoryLock lock2 = createLock(BAR);
        assertEquals(1, DirectoryLock.referenceCount(BAR));
        assertLocked(BAR);
        DirectoryLock lock3 = createLock(FOO);
        assertEquals(2, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        DirectoryLock lock4 = createLock(BAR);
        assertEquals(2, DirectoryLock.referenceCount(BAR));
        assertLocked(BAR);
        lock1.close();
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        lock2.close();
        assertEquals(1, DirectoryLock.referenceCount(BAR));
        assertLocked(BAR);
        lock3.close();
        assertEquals(0, DirectoryLock.referenceCount(FOO));
        assertNotLocked(FOO);
        lock4.close();
        assertEquals(0, DirectoryLock.referenceCount(BAR));
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
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        createLock(new File(FOO.getPath() + File.separatorChar));
        assertEquals(2, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        createLock(FOO.getCanonicalFile());
        assertEquals(3, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
    }

    public void testLockLockedDirectory() throws IOException {
        try {
            createLock(PRE_LOCKED);
            fail("DirectoryAlreadyInUseException expected but not thrown");
        } catch (DirectoryLock.DirectoryAlreadyInUseException expected) {}
    }

    @Test
    public void testCloseClosedLock() throws Exception {
        DirectoryLock lock = createLock(FOO);
        assertEquals(1, DirectoryLock.referenceCount(FOO));
        assertLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLock.referenceCount(FOO));
        assertNotLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLock.referenceCount(FOO));
        assertNotLocked(FOO);
        lock.close();
        assertEquals(0, DirectoryLock.referenceCount(FOO));
        assertNotLocked(FOO);
    }


    private DirectoryLock createLock(File directory)
        throws IOException, DirectoryLock.DirectoryAlreadyInUseException
    {
        DirectoryLock lock = new DirectoryLock(directory);
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
