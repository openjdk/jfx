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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * This class implements an simple IPC.
 * 
 * If n processes start together and performs:
 *      1) MessageBox mb = new MessageBox()
 *      2) boolean grabbed = mb.grab(...)
 * only one of the n processed will get grabbed = true : it's message box owner.
 * 
 * Other processes (which got grabbed == false) can then perform:
 *      3) mb.sendMessage(myMessage)
 * 
 * The message box owner will then receive the messages through 
 * its Delegate instance.
 * 
 * @param <T> 
 */
public class MessageBox<T extends Serializable> {
    
    public static final long NAP_TIME = 100; // ms
    
    final private String folder;
    final private Class<T> messageClass;
    final int pollingTime; // milliseconds
    final Path messageFile;
    final FileMutex boxMutex;
    final FileMutex messageMutex;
    
    private PollingThread<T> pollingThread;
    private Delegate<T> delegate;
    
    public MessageBox(String folder, Class<T> messageClass, int pollingTime) {
        assert folder != null;
        assert messageClass != null;
        assert pollingTime > 0;
        
        this.folder = folder;
        this.messageClass = messageClass;
        this.pollingTime = pollingTime;
        this.messageFile = Paths.get(folder, "message.dat"); //NOI18N
        this.boxMutex = new FileMutex(Paths.get(folder, "box.mtx")); //NOI18N
        this.messageMutex = new FileMutex(Paths.get(folder,"message.mtx")); //NOI18N
        
        if (Files.exists(Paths.get(folder)) == false) {
            throw new IllegalArgumentException(folder + " does not exist"); //NOI18N
        }
    }

    public String getFolder() {
        return folder;
    }
    
    public boolean grab(Delegate<T> delegate) 
    throws IOException {
        assert boxMutex.isLocked() == false;
        assert pollingThread == null;
        assert delegate != null;
        
        if (boxMutex.tryLock()) {
            this.delegate = delegate;
            this.pollingThread = new PollingThread<>(this);
            this.pollingThread.setDaemon(true);
            this.pollingThread.start();
        }
        
        return boxMutex.isLocked();
    }
    
    
    public void release() {
        assert boxMutex.isLocked();
        assert pollingThread != null;
        assert pollingThread.isAlive();
        
        pollingThread.interrupt();
        pollingThread = null;
        
        try {
            boxMutex.unlock();
        } catch(IOException x) {
            // Strange
            x.printStackTrace();
        }
    }
    
    
    public void sendMessage(T message) throws IOException, InterruptedException {
        assert boxMutex.isLocked() == false;
        assert messageMutex.isLocked() == false;
        
        final Path transientFile = Files.createTempFile(Paths.get(folder), null, null);
        Files.write(transientFile, serializeMessage(message));
        
        messageMutex.lock(100L * pollingTime);
        boolean retry;
        int accessDeniedCount = 0;
        do {
            if (Files.exists(messageFile) == false) {
                try {
                    Files.move(transientFile, messageFile, StandardCopyOption.ATOMIC_MOVE);
                    retry = false;
                } catch(AccessDeniedException x) {
                    // Sometime on Windows, move is denied (?).
                    // So we retry a few times...
                    if (accessDeniedCount++ <= 10) {
                        retry = true;
                    } else {
                        throw x;
                    }
                }
            } else {
                retry = true;
            }
            if (retry) {
                Thread.sleep(NAP_TIME);
            }
        } while (retry);
        messageMutex.unlock();
    }
    
    public interface Delegate<T> {
        public void messageBoxDidGetMessage(T message);
        public void messageBoxDidCatchException(Exception x);
    }
    
    public Path getMessagePath() {
        return messageFile;
    }
    
    public Path getBoxMutexPath() {
        return boxMutex.getLockFile();
    }
    
    public Path getMessageMutexPath() {
        return messageMutex.getLockFile();
    }
    
    
    /*
     * Private
     */    
    
    private byte[] serializeMessage(T message) throws IOException {
        final byte[] result;
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(message);
                result = bos.toByteArray();
            }
        }
        
        return result;
    }
    
    
    private T unserializeMessage(byte[] bytes) throws IOException {
        final T result;
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream ois = new ObjectInputStream(bis)) {
                try {
                    result = messageClass.cast(ois.readObject());
                } catch(ClassNotFoundException x) {
                    // Strange
                    throw new IOException(x);
                }
            }
        }

        return result;
    }
    
    
    private static class PollingThread<T extends Serializable> extends Thread {
        
        private final MessageBox<T> messageBox;

        public PollingThread(MessageBox<T> messageBox) {
            super("MessageBox[" + messageBox.getFolder() + "]"); //NOI18N
            this.messageBox = messageBox;
        }
        
        @Override
        public void run() {
            
            try {
                do {
                    if (Files.exists(messageBox.messageFile)) {
                        try {
                            final byte[] messageBytes = Files.readAllBytes(messageBox.messageFile);
                            final T message = messageBox.unserializeMessage(messageBytes);
                            messageBox.delegate.messageBoxDidGetMessage(message);
                        } catch(IOException x) {
                            messageBox.delegate.messageBoxDidCatchException(x);
                        } finally {
                            try {
                                Files.delete(messageBox.messageFile);
                            } catch(IOException x) {
                                messageBox.delegate.messageBoxDidCatchException(x);
                            }
                        }
                        Thread.sleep(NAP_TIME);
                    } else {
                        Thread.sleep(messageBox.pollingTime);
                    }
                } while (true);
                
            } catch(InterruptedException x) {
            }
        }
    }
}
