/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.app.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class implements a mutex using FileLock.
 * Two processes which want to be in mutual exclusion should:
 *      1) create an instance of FileMutex using the same file
 *      2) call FileMutex.lock() or FileMutex.tryLock()
 */
class FileMutex {
    
    private final Path lockFile;
    private FileLock lock;
    
    public FileMutex(Path lockFile) {
        assert lockFile != null;
        this.lockFile = lockFile;
    }

    public Path getLockFile() {
        return lockFile;
    }
    
    public void lock(long timeout) throws IOException {
        assert lock == null;
        
        final FileChannel channel = createFileChannel();
        final Timer timer = new Timer();
        timer.schedule(new InterruptTask(), timeout);
        lock = channel.lock();
        timer.cancel();
        assert lock != null;
    }
    
    public boolean tryLock() throws IOException {
        assert lock == null;

        final FileChannel channel = createFileChannel();
        lock = channel.tryLock();
        if (lock == null) {
            channel.close();
        }
        
        return lock != null;
    }
    
    public void unlock() throws IOException {
        assert lock != null;
        lock.release();
        lock.channel().close();
        lock = null;
    }
    
    public boolean isLocked() {
        return lock != null;
    }

    
    /*
     * Private
     */
    
    private FileChannel createFileChannel() throws IOException {
        try {
            Files.createFile(lockFile);
        } catch(FileAlreadyExistsException x) {
            // Someone else already created it
        }
        final RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw"); //NOI18N
        return raf.getChannel();
    }
    
    private static class InterruptTask extends TimerTask {
        @Override 
        public void run() {
            Thread.currentThread().interrupt();
        }
    }
    
//    public static void main(String[] args) throws IOException {
//        final Path mutexPath = Paths.get(System.getProperty("user.home"), "test.mtx");
//        final FileMutex fm = new FileMutex(mutexPath);
//        for (int i = 0; i < 100000; i++) {
//            fm.lock();
//            fm.unlock();
//        }
//    }
}
